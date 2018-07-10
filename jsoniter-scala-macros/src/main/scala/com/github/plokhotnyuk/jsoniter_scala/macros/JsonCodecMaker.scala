package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field
import scala.collection.{BitSet, mutable}
import scala.collection.immutable.{IntMap, LongMap}
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.reflect.NameTransformer
import scala.reflect.macros.contexts.Context
import scala.reflect.macros.blackbox

@field
final class named(val name: String) extends StaticAnnotation

@field
final class transient extends StaticAnnotation

@field
final class stringified extends StaticAnnotation

/**
  * Configuration parameter for `JsonCodecMaker.make()` call.
  *
  * @param fieldNameMapper the function of mapping from string of case class field name to JSON key (an identity
  *                        function by default)
  * @param adtLeafClassNameMapper the function of mapping from string of case class/object full name to string value of
  *                               discriminator field (a function that truncate to simple class name by default)
  * @param discriminatorFieldName a name of discriminator field ("type" value by default)
  * @param isStringified a flag that turn on stringification of number or boolean values of collections, options and
  *                      value classes (turned off by default)
  * @param skipUnexpectedFields a flag that turn on skipping of unexpected fields or in other case a parse exception
  *                             will be thrown (turned on by default)
  * @param bitSetValueLimit an exclusive limit for accepted numeric values in bit sets (1024 by default)
  * @param bigDecimalScaleLimit an exclusive limit for accepted scale of 'BigDecimal' values (300 by default)
  */
case class CodecMakerConfig(
  fieldNameMapper: String => String = identity,
  adtLeafClassNameMapper: String => String = JsonCodecMaker.simpleClassName,
  discriminatorFieldName: String = "type",
  isStringified: Boolean = false,
  skipUnexpectedFields: Boolean = true,
  bitSetValueLimit: Int = 1024, // ~128 bytes
  bigDecimalScaleLimit: Int = 300) // ~128 bytes, (BigDecimal("1e300") + 1).underlying.unscaledValue.toByteArray.length

object JsonCodecMaker {
  def enforceCamelCase(s: String): String =
    if (s.indexOf('_') == -1 && s.indexOf('-') == -1) s
    else {
      val len = s.length
      val sb = new StringBuilder(len)
      var i = 0
      var isPrecedingDash = false
      while (i < len) isPrecedingDash = {
        val ch = s.charAt(i)
        i += 1
        if (ch == '_' || ch == '-') true
        else {
          sb.append(if (isPrecedingDash) toUpperCase(ch) else toLowerCase(ch))
          false
        }
      }
      sb.toString
    }

  def enforce_snake_case(s: String): String = enforceSnakeOrKebabCase(s, '_')

  def `enforce-kebab-case`(s: String): String = enforceSnakeOrKebabCase(s, '-')

  private[this] def enforceSnakeOrKebabCase(s: String, separator: Char): String = {
    val len = s.length
    val sb = new StringBuilder(len << 1)
    var i = 0
    var isPrecedingLowerCased = false
    while (i < len) isPrecedingLowerCased = {
      val ch = s.charAt(i)
      i += 1
      if (ch == '_' || ch == '-') {
        sb.append(separator)
        false
      } else if (isLowerCase(ch)) {
        sb.append(ch)
        true
      } else {
        if (isPrecedingLowerCased || i < len && isLowerCase(s.charAt(i))) sb.append(separator)
        sb.append(toLowerCase(ch))
        false
      }
    }
    sb.toString
  }

  def simpleClassName(fullClassName: String): String =
    fullClassName.substring(Math.max(fullClassName.lastIndexOf('.') + 1, 0))

  def make[A](config: CodecMakerConfig): JsonValueCodec[A] = macro Impl.make[A]

  private object Impl {
    def make[A: c.WeakTypeTag](c: blackbox.Context)(config: c.Expr[CodecMakerConfig]): c.Expr[JsonValueCodec[A]] = {
      import c.universe._

      def fail(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

      def warn(msg: String): Unit = c.warning(c.enclosingPosition, msg)

      def info(msg: String): Unit = c.info(c.enclosingPosition, msg, force = true)

      def decodedName(s: Symbol): String = decodeName(s.name.toString)

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs(1).dealias

      val tupleSymbols: Set[Symbol] = definitions.TupleClass.seq.toSet

      def isTuple(tpe: Type): Boolean = tupleSymbols(tpe.typeSymbol)

      def isValueClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueMethod(tpe: Type): MethodSymbol = tpe.decls.head.asMethod

      def resolveConcreteType(tpe: Type, mtpe: Type): Type = {
        val tpeTypeParams =
          if (tpe.typeSymbol.isClass) tpe.typeSymbol.asClass.typeParams
          else Nil
        if (tpeTypeParams.isEmpty) mtpe
        else mtpe.substituteTypes(tpeTypeParams, tpe.typeArgs)
      }

      def methodType(tpe: Type, m: MethodSymbol): Type = resolveConcreteType(tpe, m.returnType.dealias)

      def paramType(tpe: Type, p: TermSymbol): Type = resolveConcreteType(tpe, p.typeSignature.dealias)

      def valueClassValueType(tpe: Type): Type = methodType(tpe, valueClassValueMethod(tpe))

      def isNonAbstractScalaClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && {
        val classSymbol = tpe.typeSymbol.asClass
        classSymbol.isCaseClass || (!classSymbol.isJava && !classSymbol.isAbstract)
      }

      def isSealedAdtBase(tpe: Type): Boolean = tpe.typeSymbol.isClass && {
        val classSymbol = tpe.typeSymbol.asClass
        (classSymbol.isAbstract || classSymbol.isTrait) &&
          (if (classSymbol.isSealed) true
          else fail("Only sealed traits & abstract classes are supported for an ADT base. Please consider adding " +
            s"of a sealed definition for '$tpe' or using a custom implicitly accessible codec for the ADT base."))
      }

      def adtLeafClasses(tpe: Type): Seq[Type] = {
        def collectRecursively(tpe: Type): Set[Type] = if (tpe.typeSymbol.isClass) {
          tpe.typeSymbol.asClass.knownDirectSubclasses.flatMap { s =>
            val classSymbol = s.asClass
            val subTpe =
              if (classSymbol.typeParams.isEmpty) classSymbol.toType
              else classSymbol.toType.substituteTypes(classSymbol.typeParams, tpe.typeArgs)
            if (isSealedAdtBase(subTpe)) collectRecursively(subTpe)
            else if (isNonAbstractScalaClass(subTpe)) Set(subTpe)
            else fail("Only Scala classes & objects are supported for ADT leaf classes. Please consider using of them " +
              s"for ADT with base '$tpe' or using a custom implicitly accessible codec for the ADT base.")
          }
        } else Set.empty

        val classes = collectRecursively(tpe).toSeq
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$tpe'. Please consider adding them or " +
          "using a custom implicitly accessible codec for the ADT base.")
        classes
      }

      // Borrowed and refactored from Chimney: https://github.com/scalalandio/chimney/blob/master/chimney/src/main/scala/io/scalaland/chimney/internal/CompanionUtils.scala#L10-L63
      // Copied from Magnolia: https://github.com/propensive/magnolia/blob/master/core/shared/src/main/scala/globalutil.scala
      // From Shapeless: https://github.com/milessabin/shapeless/blob/master/core/src/main/scala/shapeless/generic.scala#L698
      // Cut-n-pasted (with most original comments) and slightly adapted from https://github.com/scalamacros/paradise/blob/c14c634923313dd03f4f483be3d7782a9b56de0e/plugin/src/main/scala/org/scalamacros/paradise/typechecker/Namers.scala#L568-L613
      def patchedCompanionRef(tpe: Type): Tree = {
        val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
        val globalType = tpe.asInstanceOf[global.Type]
        val original = globalType.typeSymbol
        global.gen.mkAttributedRef(globalType.prefix, original.companion.orElse {
          import global._
          val name = original.name.companionName
          val expectedOwner = original.owner
          var ctx = c.asInstanceOf[Context].callsiteTyper.asInstanceOf[global.analyzer.Typer].context
          var res: Symbol = NoSymbol
          while (res == NoSymbol && ctx.outer != ctx) {
            // NOTE: original implementation says `val s = ctx.scope lookup name`
            // but we can't use it, because Scope.lookup returns wrong results when the lookup is ambiguous
            // and that triggers https://github.com/scalamacros/paradise/issues/64
            val s = ctx.scope.lookupAll(name)
              .filter(sym => (original.isTerm || sym.hasModuleFlag) && sym.isCoDefinedWith(original)).toList match {
              case Nil => NoSymbol
              case unique :: Nil => unique
              case _ => fail(s"Unexpected multiple results for a companion symbol lookup for $original")
            }
            if (s != NoSymbol && s.owner == expectedOwner) res = s
            else ctx = ctx.outer
          }
          res
        }).asInstanceOf[Tree]
      }

      def companion(tpe: Type): Symbol = {
        val comp = tpe.typeSymbol.companion
        if (comp.isModule) comp
        else patchedCompanionRef(tpe).symbol
      }

      def isContainer(tpe: Type): Boolean =
        tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Array[_]]

      def collectionCompanion(tpe: Type): Tree = {
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ident(tpe.typeSymbol.companion)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")
      }

      def enumSymbol(tpe: Type): Symbol = {
        val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
        enumSymbol
      }

      def getType(typeTree: Tree): Type = c.typecheck(typeTree, c.TYPEmode).tpe

      def eval[B](tree: Tree): B = c.eval[B](c.Expr[B](c.untypecheck(tree)))

      val codecConfig = eval[CodecMakerConfig](config.tree)
      val inferredCodecs: mutable.Map[Type, Tree] = mutable.Map.empty
      val inferredKeyCodecs: mutable.Map[Type, Tree] = mutable.Map.empty

      def findImplicitCodec(tpe: Type): Tree = inferredCodecs.getOrElseUpdate(tpe,
        c.inferImplicitValue(getType(tq"com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$tpe]")))

      def findImplicitKeyCodec(tpe: Type): Tree = inferredKeyCodecs.getOrElseUpdate(tpe,
        c.inferImplicitValue(getType(tq"com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[$tpe]")))

      def genReadKey(tpe: Type): Tree = {
        val implKeyCodec = findImplicitKeyCodec(tpe)
        if (!implKeyCodec.isEmpty) q"$implKeyCodec.decodeKey(in)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readKeyAsBoolean()"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"in.readKeyAsByte()"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readKeyAsChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"in.readKeyAsShort()"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"in.readKeyAsInt()"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"in.readKeyAsLong()"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"in.readKeyAsFloat()"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"in.readKeyAsDouble()"
        else if (isValueClass(tpe)) q"new $tpe(${genReadKey(valueClassValueType(tpe))})"
        else if (tpe =:= typeOf[String]) q"in.readKeyAsString()"
        else if (tpe =:= typeOf[BigInt]) q"in.readKeyAsBigInt()"
        else if (tpe =:= typeOf[BigDecimal]) q"in.readKeyAsBigDecimal(${codecConfig.bigDecimalScaleLimit})"
        else if (tpe =:= typeOf[java.util.UUID]) q"in.readKeyAsUUID()"
        else if (tpe =:= typeOf[Duration]) q"in.readKeyAsDuration()"
        else if (tpe =:= typeOf[Instant]) q"in.readKeyAsInstant()"
        else if (tpe =:= typeOf[LocalDate]) q"in.readKeyAsLocalDate()"
        else if (tpe =:= typeOf[LocalDateTime]) q"in.readKeyAsLocalDateTime()"
        else if (tpe =:= typeOf[LocalTime]) q"in.readKeyAsLocalTime()"
        else if (tpe =:= typeOf[MonthDay]) q"in.readKeyAsMonthDay()"
        else if (tpe =:= typeOf[OffsetDateTime]) q"in.readKeyAsOffsetDateTime()"
        else if (tpe =:= typeOf[OffsetTime]) q"in.readKeyAsOffsetTime()"
        else if (tpe =:= typeOf[Period]) q"in.readKeyAsPeriod()"
        else if (tpe =:= typeOf[Year]) q"in.readKeyAsYear()"
        else if (tpe =:= typeOf[YearMonth]) q"in.readKeyAsYearMonth()"
        else if (tpe =:= typeOf[ZonedDateTime]) q"in.readKeyAsZonedDateTime()"
        else if (tpe =:= typeOf[ZoneId]) q"in.readKeyAsZoneId()"
        else if (tpe =:= typeOf[ZoneOffset]) q"in.readKeyAsZoneOffset()"
        else if (tpe <:< typeOf[Enumeration#Value]) {
          q"""val len = in.readKeyAsCharBuf()
              ${enumSymbol(tpe)}.values.iterator.find(e => in.isCharBufEqualsTo(len, e.toString))
                .getOrElse(in.enumValueError(len))"""
        } else if (tpe <:< typeOf[java.lang.Enum[_]]) {
          q"""val v = in.readKeyAsString()
              try ${companion(tpe)}.valueOf(v) catch {
                case _: IllegalArgumentException => in.enumValueError(v)
              }"""
        } else fail(s"Unsupported type to be used as map key '$tpe'.")
      }

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"x"): Tree =
        q"""if (in.isNextToken('[')) {
              if (in.isNextToken(']')) default
              else {
                in.rollbackToken()
                ..$newBuilder
                do {
                  ..$readVal
                } while (in.isNextToken(','))
                if (in.isCurrentToken(']')) $result
                else in.arrayEndOrCommaError()
              }
            } else in.readNullOrTokenError(default, '[')"""

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        q"""if (in.isNextToken('{')) {
              if (in.isNextToken('}')) default
              else {
                in.rollbackToken()
                ..$newBuilder
                do {
                  ..$readKV
                } while (in.isNextToken(','))
                if (in.isCurrentToken('}')) $result
                else in.objectEndOrCommaError()
              }
            } else in.readNullOrTokenError(default, '{')"""

      def genWriteKey(x: Tree, tpe: Type): Tree = {
        val implKeyCodec = findImplicitKeyCodec(tpe)
        if (!implKeyCodec.isEmpty) q"$implKeyCodec.encodeKey($x, out)"
        else if (isValueClass(tpe)) genWriteKey(q"$x.${valueClassValueMethod(tpe)}", valueClassValueType(tpe))
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] ||
          tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte] ||
          tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] ||
          tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] ||
          tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long] ||
          tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double] ||
          tpe =:= typeOf[String] || tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal] ||
          tpe =:= typeOf[java.util.UUID] || tpe =:= typeOf[Duration] || tpe =:= typeOf[Instant] ||
          tpe =:= typeOf[LocalDate] || tpe =:= typeOf[LocalDateTime] || tpe =:= typeOf[LocalTime] ||
          tpe =:= typeOf[MonthDay] || tpe =:= typeOf[OffsetDateTime] || tpe =:= typeOf[OffsetTime] ||
          tpe =:= typeOf[Period] || tpe =:= typeOf[Year] || tpe =:= typeOf[YearMonth] ||
          tpe =:= typeOf[ZonedDateTime] || tpe =:= typeOf[ZoneId] || tpe =:= typeOf[ZoneOffset]) q"out.writeKey($x)"
        else if (tpe <:< typeOf[Enumeration#Value]) q"out.writeKey($x.toString)"
        else if (tpe <:< typeOf[java.lang.Enum[_]]) q"out.writeKey($x.name)"
        else fail(s"Unsupported type to be used as map key '$tpe'.")
      }

      def genWriteConstantKey(name: String): Tree =
        if (isEncodingRequired(name)) q"out.writeKey($name)"
        else q"out.writeNonEscapedAsciiKey($name)"

      def genWriteConstantVal(value: String): Tree =
        if (isEncodingRequired(value)) q"out.writeVal($value)"
        else q"out.writeNonEscapedAsciiVal($value)"

      def genWriteArray(x: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            $x.foreach { x =>
              out.writeComma()
              ..$writeVal
            }
            out.writeArrayEnd()"""

      def genWriteMap(x: Tree, writeKey: Tree, writeVal: Tree): Tree =
        q"""out.writeObjectStart()
            $x.foreach { kv =>
              ..$writeKey
              ..$writeVal
            }
            out.writeObjectEnd()"""

      def cannotFindCodecError(tpe: Type): Nothing =
        fail(s"No implicit '${typeOf[JsonValueCodec[_]]}' defined for '$tpe'.")

      case class FieldInfo(symbol: TermSymbol, mappedName: String, tmpName: TermName, getter: MethodSymbol,
                           defaultValue: Option[Tree], resolvedTpe: Type, isStringified: Boolean)

      case class ClassInfo(tpe: Type, fields: Seq[FieldInfo])

      val classInfos = mutable.LinkedHashMap.empty[Type, ClassInfo]

      def getClassInfo(tpe: Type): ClassInfo = classInfos.getOrElseUpdate(tpe, {
        case class FieldAnnotations(partiallyMappedName: String, transient: Boolean, stringified: Boolean)

        def getPrimaryConstructor(tpe: Type): MethodSymbol = tpe.decls.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m
        }.get // FIXME: while in Scala, every class has a primary constructor, but sometime it cannot be accessed

        lazy val module = companion(tpe).asModule // don't lookup for the companion when there are no default values for constructor params
        val getters = tpe.members.collect { case m: MethodSymbol if m.isParamAccessor && m.isGetter => m }
        val annotations = tpe.members.collect {
          case m: TermSymbol if {
            m.info // to enforce the type information completeness and availability of annotations
            m.annotations.exists(a => a.tree.tpe =:= typeOf[named] || a.tree.tpe =:= typeOf[transient] ||
              a.tree.tpe =:= typeOf[stringified])
          } =>
            val name = decodedName(m).trim // FIXME: Why is there a space at the end of field name?!
            val named = m.annotations.filter(_.tree.tpe =:= typeOf[named])
            if (named.size > 1) fail(s"Duplicated '${typeOf[named]}' defined for '$name' of '$tpe'.")
            val trans = m.annotations.filter(_.tree.tpe =:= typeOf[transient])
            if (trans.size > 1) warn(s"Duplicated '${typeOf[transient]}' defined for '$name' of '$tpe'.")
            val strings = m.annotations.filter(_.tree.tpe =:= typeOf[stringified])
            if (strings.size > 1) warn(s"Duplicated '${typeOf[stringified]}' defined for '$name' of '$tpe'.")
            if ((named.nonEmpty || strings.nonEmpty) && trans.size == 1) {
              warn(s"Both '${typeOf[transient]}' and '${typeOf[named]}' or " +
                s"'${typeOf[transient]}' and '${typeOf[stringified]}' defined for '$name' of '$tpe'.")
            }
            val partiallyMappedName = named.headOption.flatMap(a => Option(eval[named](a.tree).name)).getOrElse(name)
            (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        ClassInfo(tpe, getPrimaryConstructor(tpe).paramLists match {
          case params :: Nil => params.zipWithIndex.flatMap { case (p, i) =>
            val symbol = p.asTerm
            val name = decodedName(symbol)
            val annotationOption = annotations.get(name)
            if (annotationOption.fold(false)(_.transient)) None
            else {
              val mappedName = annotationOption.fold(codecConfig.fieldNameMapper(name))(_.partiallyMappedName)
              val tmpName = TermName("_" + symbol.name)
              val getter = getters.find(_.name == symbol.name).getOrElse {
                fail(s"'$name' parameter of '$tpe' should be defined as 'val' or 'var' in the primary constructor.")
              }
              val defaultValue =
                if (symbol.isParamWithDefault) Some(q"$module.${TermName("$lessinit$greater$default$" + (i + 1))}")
                else None
              val ptpe = paramType(tpe, symbol)
              val isStringified = annotationOption.fold(false)(_.stringified)
              Some(FieldInfo(symbol, mappedName, tmpName, getter, defaultValue, ptpe, isStringified))
            }
          }
          case _ => fail(s"'$tpe' has a primary constructor with multiple parameter lists. " +
            "Please consider using a custom implicitly accessible codec for this type.")
        })
      })

      val rootTpe = weakTypeOf[A].dealias
      val unexpectedFieldHandler =
        if (codecConfig.skipUnexpectedFields) q"in.skip()"
        else q"in.unexpectedKeyError(l)"
      val skipDiscriminatorField = {
        val cs = codecConfig.discriminatorFieldName.toCharArray
        cq"""${JsonReader.toHashCode(cs, cs.length)} =>
             if (pd) {
               pd = !pd
               in.skip()
             } else in.duplicatedKeyError(l)"""
      }

      def discriminatorValue(tpe: Type): String =
        codecConfig.adtLeafClassNameMapper(decodeName(tpe.typeSymbol.fullName))

      def checkFieldNameCollisions(tpe: Type, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON name(s) defined for '$tpe': $formattedCollisions. " +
              s"Names(s) defined by '${typeOf[named]}' annotation(s), " +
              "name of discriminator field specified by 'config.discriminatorFieldName' " +
              "and name(s) returned by 'config.fieldNameMapper' for non-annotated fields should not match.")
        }
      }

      def checkDiscriminatorValueCollisions(discriminatorFieldName: String, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated values defined for '$discriminatorFieldName': $formattedCollisions. " +
            "Values returned by 'config.adtLeafClassNameMapper' should not match.")
        }
      }

      val nullValueNames = mutable.LinkedHashMap.empty[Type, TermName]
      val nullValueTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withNullValueFor(tpe: Type)(f: => Tree): Tree = {
        val nullValueName = nullValueNames.getOrElseUpdate(tpe, TermName("c" + nullValueNames.size))
        nullValueTrees.getOrElseUpdate(tpe, q"private[this] val $nullValueName: $tpe = $f")
        Ident(nullValueName)
      }

      val fieldNames = mutable.LinkedHashMap.empty[Type, TermName]
      val fieldTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withFieldsFor(tpe: Type)(f: => Seq[String]): Tree = {
        val fieldName = fieldNames.getOrElseUpdate(tpe, TermName("f" + fieldNames.size))
        fieldTrees.getOrElseUpdate(tpe,
          q"""private[this] def $fieldName(i: Int): String = (i: @switch) match {
                case ..${f.zipWithIndex.map { case (n, i) => cq"$i => $n"}}
              }""")
        Ident(fieldName)
      }

      val equalsMethodNames = mutable.LinkedHashMap.empty[Type, TermName]
      val equalsMethodTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withEqualsFor(tpe: Type, arg1: Tree, arg2: Tree)(f: => Tree): Tree = {
        val equalsMethodName = equalsMethodNames.getOrElseUpdate(tpe, TermName("q" + equalsMethodNames.size))
        equalsMethodTrees.getOrElseUpdate(tpe,
          q"""private[this] def $equalsMethodName(x1: $tpe, x2: $tpe): Boolean = $f""")
        q"$equalsMethodName($arg1, $arg2)"
      }

      case class MethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree)

      def getMethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree): MethodKey =
        MethodKey(tpe, isStringified && isContainer(tpe), discriminator)

      val decodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val decodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withDecoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val decodeMethodName = decodeMethodNames.getOrElseUpdate(methodKey, TermName("d" + decodeMethodNames.size))
        decodeMethodTrees.getOrElseUpdate(methodKey,
          q"private[this] def $decodeMethodName(in: JsonReader, default: ${methodKey.tpe}): ${methodKey.tpe} = $f")
        q"$decodeMethodName(in, $arg)"
      }

      val encodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val encodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withEncoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val encodeMethodName = encodeMethodNames.getOrElseUpdate(methodKey, TermName("e" + encodeMethodNames.size))
        encodeMethodTrees.getOrElseUpdate(methodKey,
          q"private[this] def $encodeMethodName(x: ${methodKey.tpe}, out: JsonWriter): Unit = $f")
        q"$encodeMethodName($arg, out)"
      }

      def nullValue(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"false"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"(0: Byte)"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"'\u0000'"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"(0: Short)"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"0"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"0L"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"0f"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"0.0"
        else if (isValueClass(tpe)) q"null.asInstanceOf[$tpe]"
        else if (tpe <:< typeOf[Option[_]]) q"None"
        else if (tpe <:< typeOf[mutable.BitSet]) q"${collectionCompanion(tpe)}.empty"
        else if (tpe <:< typeOf[BitSet]) withNullValueFor(tpe)(q"${collectionCompanion(tpe)}.empty")
        else if (tpe <:< typeOf[mutable.LongMap[_]]) q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[LongMap[_]] ||
          tpe <:< typeOf[collection.immutable.Seq[_]] || tpe <:< typeOf[Set[_]]) withNullValueFor(tpe) {
          q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[collection.immutable.Map[_, _]]) withNullValueFor(tpe) {
          q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[collection.Map[_, _]]) {
          q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[Iterable[_]]) q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[Array[_]]) withNullValueFor(tpe)(q"new Array[${typeArg1(tpe)}](0)")
        else if (tpe.typeSymbol.isModuleClass) q"${tpe.typeSymbol.asClass.module}"
        else q"null"

      def genReadVal(tpe: Type, default: Tree, isStringified: Boolean, discriminator: Tree = EmptyTree,
                     isRoot: Boolean = false): Tree = {
        val implCodec = if (isRoot) EmptyTree else findImplicitCodec(tpe)
        val methodKey = getMethodKey(tpe, isStringified, discriminator)
        val decodeMethodName = decodeMethodNames.get(methodKey)
        if (!implCodec.isEmpty) q"$implCodec.decodeValue(in, $default)"
        else if (decodeMethodName.isDefined) q"${decodeMethodName.get}(in, $default)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) {
          if (isStringified) q"in.readStringAsBoolean()" else q"in.readBoolean()"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) {
          if (isStringified) q"in.readStringAsByte()" else q"in.readByte()"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) {
          if (isStringified) q"in.readStringAsShort()" else q"in.readShort()"
        } else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) {
          if (isStringified) q"in.readStringAsInt()" else q"in.readInt()"
        } else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) {
          if (isStringified) q"in.readStringAsLong()" else q"in.readLong()"
        } else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) {
          if (isStringified) q"in.readStringAsFloat()" else q"in.readFloat()"
        } else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          if (isStringified) q"in.readStringAsDouble()" else q"in.readDouble()"
        } else if (tpe =:= typeOf[String]) q"in.readString($default)"
        else if (tpe =:= typeOf[java.util.UUID]) q"in.readUUID($default)"
        else if (tpe =:= typeOf[Duration]) q"in.readDuration($default)"
        else if (tpe =:= typeOf[Instant]) q"in.readInstant($default)"
        else if (tpe =:= typeOf[LocalDate]) q"in.readLocalDate($default)"
        else if (tpe =:= typeOf[LocalDateTime]) q"in.readLocalDateTime($default)"
        else if (tpe =:= typeOf[LocalTime]) q"in.readLocalTime($default)"
        else if (tpe =:= typeOf[MonthDay]) q"in.readMonthDay($default)"
        else if (tpe =:= typeOf[OffsetDateTime]) q"in.readOffsetDateTime($default)"
        else if (tpe =:= typeOf[OffsetTime]) q"in.readOffsetTime($default)"
        else if (tpe =:= typeOf[Period]) q"in.readPeriod($default)"
        else if (tpe =:= typeOf[Year]) q"in.readYear($default)"
        else if (tpe =:= typeOf[YearMonth]) q"in.readYearMonth($default)"
        else if (tpe =:= typeOf[ZonedDateTime]) q"in.readZonedDateTime($default)"
        else if (tpe =:= typeOf[ZoneId]) q"in.readZoneId($default)"
        else if (tpe =:= typeOf[ZoneOffset]) q"in.readZoneOffset($default)"
        else if (tpe =:= typeOf[BigInt]) {
          if (isStringified) q"in.readStringAsBigInt($default)" else q"in.readBigInt($default)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          if (isStringified) q"in.readStringAsBigDecimal($default, ${codecConfig.bigDecimalScaleLimit})"
          else q"in.readBigDecimal($default, ${codecConfig.bigDecimalScaleLimit})"
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          q"new $tpe(${genReadVal(tpe1, nullValue(tpe1), isStringified)})"
        } else if (tpe <:< typeOf[Option[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          q"""if (in.isNextToken('n')) in.readNullOrError(default, "expected value or null")
              else {
                in.rollbackToken()
                Some(${genReadVal(tpe1, nullValue(tpe1), isStringified)})
              }"""
        } else if (tpe <:< typeOf[IntMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"$comp.empty[$tpe1]")}",
            q"x = x.updated(in.readKeyAsInt(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1]",
            q"x.update(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"$comp.empty[$tpe1]")}",
            q"x = x.updated(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1, $tpe2]",
            q"x.update(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})")
        } else if (tpe <:< typeOf[Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"$comp.empty[$tpe1]")}",
            q"x = x.updated(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})")
        } else if (tpe <:< typeOf[mutable.BitSet]) withDecoderFor(methodKey, default) {
          val comp = collectionCompanion(tpe)
          val readVal = if (isStringified) q"in.readStringAsInt()" else q"in.readInt()"
          genReadArray(q"var x = new Array[Long](2)",
            q"""val v = $readVal
                if (v < 0 || v >= ${codecConfig.bitSetValueLimit}) in.decodeError("illegal value for bit set")
                val i = v >>> 6
                if (i >= x.length) x = java.util.Arrays.copyOf(x, java.lang.Integer.highestOneBit(i) << 1)
                x(i) |= 1L << (v & 63)""",
            q"$comp.fromBitMaskNoCopy(x)")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(methodKey, default) {
          val comp = collectionCompanion(tpe)
          val readVal = if (isStringified) q"in.readStringAsInt()" else q"in.readInt()"
          genReadArray(q"var x = new Array[Long](2); var mi = 0",
            q"""val v = $readVal
                if (v < 0 || v >= ${codecConfig.bitSetValueLimit}) in.decodeError("illegal value for bit set")
                val i = v >>> 6
                if (i > mi) {
                  mi = i
                  if (i >= x.length) x = java.util.Arrays.copyOf(x, java.lang.Integer.highestOneBit(i) << 1)
                }
                x(i) |= 1L << (v & 63)""",
            q"""if (mi > 1 && mi + 1 != x.length) x = java.util.Arrays.copyOf(x, mi + 1)
                $comp.fromBitMaskNoCopy(x)""")
        } else if (tpe <:< typeOf[List[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = new scala.collection.mutable.ListBuffer[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.toList")
        } else if (tpe <:< typeOf[mutable.Iterable[_] with mutable.Builder[_, _]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { //ArrayStack uses 'push' for '+='
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = default; if (x.nonEmpty) x.clear(); ",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}")
        } else if (tpe <:< typeOf[Iterable[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadArray(q"val x = $comp.newBuilder[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.result()")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(
            q"""var x = new Array[$tpe1](16)
                var i = 0""",
            q"""if (i == x.length) x = java.util.Arrays.copyOf(x, i << 1)
                x(i) = ${genReadVal(tpe1, nullValue(tpe1), isStringified)}
                i += 1""",
            q"""if (i == x.length) x
                else java.util.Arrays.copyOf(x, i)""")
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('"')) {
                in.rollbackToken()
                val len = in.readStringAsCharBuf()
                ${enumSymbol(tpe)}.values.iterator.find(e => in.isCharBufEqualsTo(len, e.toString))
                  .getOrElse(in.enumValueError(len))
              } else in.readNullOrTokenError(default, '"')"""
        } else if (tpe <:< typeOf[java.lang.Enum[_]]) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('"')) {
                in.rollbackToken()
                val v = in.readString(null)
                try ${companion(tpe)}.valueOf(v) catch {
                  case _: IllegalArgumentException => in.enumValueError(v)
                }
              } else in.readNullOrTokenError(default, '"')"""
        } else if (tpe.typeSymbol.isModuleClass) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('{')) {
                in.rollbackToken()
                in.skip()
                ${tpe.typeSymbol.asClass.module}
              } else in.readNullOrTokenError(default, '{')"""
        } else if (isTuple(tpe)) withDecoderFor(methodKey, default) {
          val indexedTypes = tpe.typeArgs.zipWithIndex
          val readFields = indexedTypes.tail.foldLeft {
            val t = typeArg1(tpe)
            q"val _1: $t = ${genReadVal(t, nullValue(t), isStringified)}": Tree
          }{ case (acc, (ta, i)) =>
            val t = ta.dealias
            q"""..$acc
                val ${TermName("_" + (i + 1))}: $t =
                  if (in.isNextToken(',')) ${genReadVal(t, nullValue(t), isStringified)}
                  else in.commaError()"""
          }
          val vals = indexedTypes.map { case (t, i) => TermName("_" + (i + 1)) }
          q"""if (in.isNextToken('[')) {
                ..$readFields
                if (in.isNextToken(']')) new $tpe(..$vals)
                else in.arrayEndError()
              } else in.readNullOrTokenError(default, '[')"""
        } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default) {
          val classInfo = getClassInfo(tpe)
          checkFieldNameCollisions(tpe,
            (if (discriminator.isEmpty) Seq.empty
            else Seq(codecConfig.discriminatorFieldName)) ++ classInfo.fields.map(_.mappedName))
          val required = classInfo.fields.collect {
            case f if !f.symbol.isParamWithDefault && !isContainer(f.resolvedTpe) => f.mappedName
          }
          val paramVarNum = classInfo.fields.size
          val lastParamVarIndex = paramVarNum >> 5
          val lastParamVarBits = (1 << paramVarNum) - 1
          val paramVarNames = (0 to lastParamVarIndex).map(i => TermName("p" + i))
          val checkAndResetFieldPresenceFlags = classInfo.fields.zipWithIndex.map { case (f, i) =>
            val (n, m) = (paramVarNames(i >> 5), 1 << i)
            (f.mappedName, q"if (($n & $m) != 0) $n ^= $m else in.duplicatedKeyError(l)")
          }.toMap
          val paramVars =
            paramVarNames.init.map(n => q"var $n = -1") :+ q"var ${paramVarNames.last} = $lastParamVarBits"
          val checkReqVars = if (required.isEmpty) Nil else {
            val names = withFieldsFor(tpe)(classInfo.fields.map(_.mappedName))
            val reqSet = required.toSet
            val reqMasks = classInfo.fields.grouped(32).map(_.zipWithIndex.foldLeft(0) { case (acc, (f, i)) =>
              acc | (if (reqSet(f.mappedName)) 1 << i else 0)
            }).toSeq
            paramVarNames.zipWithIndex.map { case (n, i) =>
              val m = reqMasks(i)
              if (i == 0) q"if (($n & $m) != 0) in.requiredFieldError($names(Integer.numberOfTrailingZeros($n & $m)))"
              else q"if (($n & $m) != 0) in.requiredFieldError($names(Integer.numberOfTrailingZeros($n & $m) + ${i << 5}))"
            }
          }
          val construct = q"new $tpe(..${classInfo.fields.map(f => q"${f.symbol.name} = ${f.tmpName}")})"
          val readVars = classInfo.fields
            .map(f => q"var ${f.tmpName}: ${f.resolvedTpe} = ${f.defaultValue.getOrElse(nullValue(f.resolvedTpe))}")
          val hashCode: FieldInfo => Int = f => JsonReader.toHashCode(f.mappedName.toCharArray, f.mappedName.length)
          val readFields = groupByOrdered(classInfo.fields)(hashCode).map { case (hash, fs) =>
            val checkNameAndReadValue = fs.foldRight(unexpectedFieldHandler) { case (f, acc) =>
              val readValue = q"${f.tmpName} = ${genReadVal(f.resolvedTpe, q"${f.tmpName}", f.isStringified)}"
              q"""if (in.isCharBufEqualsTo(l, ${f.mappedName})) {
                    ..${checkAndResetFieldPresenceFlags(f.mappedName)}
                    ..$readValue
                  } else $acc"""
            }
            cq"$hash => $checkNameAndReadValue"
          }.toSeq
          val readFieldsBlock =
            (if (discriminator.isEmpty) readFields
            else readFields :+ discriminator) :+ cq"_ => $unexpectedFieldHandler"
          val discriminatorVar =
            if (discriminator.isEmpty) EmptyTree
            else q"var pd = true"
          q"""if (in.isNextToken('{')) {
                ..$readVars
                ..$paramVars
                ..$discriminatorVar
                if (!in.isNextToken('}')) {
                  in.rollbackToken()
                  do {
                    val l = in.readKeyAsCharBuf()
                    (in.charBufToHashCode(l): @switch) match {
                      case ..$readFieldsBlock
                    }
                  } while (in.isNextToken(','))
                  if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
                }
                ..$checkReqVars
                $construct
              } else in.readNullOrTokenError(default, '{')"""
        } else if (isSealedAdtBase(tpe)) withDecoderFor(methodKey, default) {
          def hashCode(subTpe: Type): Int = {
            val cs = discriminatorValue(subTpe).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }

          val leafClasses = adtLeafClasses(tpe)
          val discrName = codecConfig.discriminatorFieldName
          checkDiscriminatorValueCollisions(discrName, leafClasses.map(discriminatorValue))
          val discriminatorValueError = q"in.discriminatorValueError($discrName)"
          val readSubclasses = groupByOrdered(leafClasses)(hashCode).map { case (hashCode, subTpes) =>
            val checkNameAndReadValue = subTpes.foldRight(discriminatorValueError) { case (subTpe, acc) =>
              q"""if (in.isCharBufEqualsTo(l, ${discriminatorValue(subTpe)})) {
                    in.rollbackToMark()
                    ..${genReadVal(subTpe, nullValue(subTpe), isStringified, skipDiscriminatorField)}
                  } else $acc"""
            }
            cq"$hashCode => $checkNameAndReadValue"
          }.toSeq
          q"""in.setMark()
              if (in.isNextToken('{')) {
                if (in.skipToKey($discrName)) {
                  val l = in.readStringAsCharBuf()
                  (in.charBufToHashCode(l): @switch) match {
                    case ..$readSubclasses
                    case _ => $discriminatorValueError
                  }
                } else in.requiredFieldError($discrName)
              } else in.readNullOrTokenError(default, '{')"""
        } else cannotFindCodecError(tpe)
      }

      def genWriteVal(m: Tree, tpe: Type, isStringified: Boolean, discriminator: Tree = EmptyTree,
                      isRoot: Boolean = false): Tree = {
        val implCodec = if (isRoot) EmptyTree else findImplicitCodec(tpe)
        val methodKey = getMethodKey(tpe, isStringified, discriminator)
        val encodeMethodName = encodeMethodNames.get(methodKey)
        if (!implCodec.isEmpty) q"$implCodec.encodeValue($m, out)"
        else if (encodeMethodName.isDefined) q"${encodeMethodName.get}($m, out)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] ||
          tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte] ||
          tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] ||
          tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long] ||
          tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double] ||
          tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal]) {
          if (isStringified) q"out.writeValAsString($m)" else q"out.writeVal($m)"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] ||
          tpe =:= typeOf[String] || tpe =:= typeOf[java.util.UUID] ||
          tpe =:= typeOf[Duration] || tpe =:= typeOf[Instant] ||
          tpe =:= typeOf[LocalDate] || tpe =:= typeOf[LocalDateTime] ||
          tpe =:= typeOf[LocalTime] || tpe =:= typeOf[MonthDay] ||
          tpe =:= typeOf[OffsetDateTime] || tpe =:= typeOf[OffsetTime] ||
          tpe =:= typeOf[Period] || tpe =:= typeOf[Year] ||
          tpe =:= typeOf[YearMonth] || tpe =:= typeOf[ZonedDateTime] ||
          tpe =:= typeOf[ZoneId] || tpe =:= typeOf[ZoneOffset]) q"out.writeVal($m)"
        else if (isValueClass(tpe)) {
          genWriteVal(q"$m.${valueClassValueMethod(tpe)}", valueClassValueType(tpe), isStringified)
        } else if (tpe <:< typeOf[Option[_]]) withEncoderFor(methodKey, m) {
          q"if (x.isEmpty) out.writeNull() else ${genWriteVal(q"x.get", typeArg1(tpe), isStringified)}"
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] ||
            tpe <:< typeOf[LongMap[_]]) withEncoderFor(methodKey, m) {
          genWriteMap(q"x", q"out.writeKey(kv._1)", genWriteVal(q"kv._2", typeArg1(tpe), isStringified))
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) withEncoderFor(methodKey, m) {
          genWriteMap(q"x", genWriteKey(q"kv._1", typeArg1(tpe)), genWriteVal(q"kv._2", typeArg2(tpe), isStringified))
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", if (isStringified) q"out.writeValAsString(x)" else q"out.writeVal(x)")
        } else if (tpe <:< typeOf[List[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              var l = x
              while (!l.isEmpty) {
                out.writeComma()
                ..${genWriteVal(q"l.head", typeArg1(tpe), isStringified)}
                l = l.tail
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[IndexedSeq[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.size
              var i = 0
              while (i < l) {
                out.writeComma()
                ..${genWriteVal(q"x(i)", typeArg1(tpe), isStringified)}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Iterable[_]]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe), isStringified))
        } else if (tpe <:< typeOf[Array[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.length
              var i = 0
              while (i < l) {
                out.writeComma()
                ..${genWriteVal(q"x(i)", typeArg1(tpe), isStringified)}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(methodKey, m) {
          q"out.writeVal(x.toString)"
        } else if (tpe <:< typeOf[java.lang.Enum[_]]) withEncoderFor(methodKey, m) {
          q"out.writeVal(x.name)"
        } else if (tpe.typeSymbol.isModuleClass) withEncoderFor(methodKey, m) {
          q"""out.writeObjectStart()
              ..$discriminator
              out.writeObjectEnd()"""
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m) {
          val writeFields = tpe.typeArgs.zipWithIndex.map { case (ta, i) =>
            q"""out.writeComma()
                ${genWriteVal(q"x.${TermName("_" + (i + 1))}", ta.dealias, isStringified)}"""
          }
          q"""out.writeArrayStart()
              ..$writeFields
              out.writeArrayEnd()"""
        } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m) {
          val classInfo = getClassInfo(tpe)
          val writeFields = classInfo.fields.map { f =>
            f.defaultValue match {
              case Some(d) =>
                if (f.resolvedTpe <:< typeOf[Iterable[_]]) {
                  q"""val v = x.${f.getter}
                      if (!v.isEmpty && v != $d) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v", f.resolvedTpe, f.isStringified)}
                      }"""
                } else if (f.resolvedTpe <:< typeOf[Option[_]]) {
                  q"""val v = x.${f.getter}
                      if (!v.isEmpty && v != $d) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v.get", typeArg1(f.resolvedTpe), f.isStringified)}
                      }"""
                } else if (f.resolvedTpe <:< typeOf[Array[_]]) {
                  q"""val v = x.${f.getter}
                      if (v.length > 0 && !${withEqualsFor(f.resolvedTpe, q"v", d)(genArrayEquals(f.resolvedTpe))}) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v", f.resolvedTpe, f.isStringified)}
                      }"""
                } else {
                  q"""val v = x.${f.getter}
                      if (v != $d) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v", f.resolvedTpe, f.isStringified)}
                      }"""
                }
              case None =>
                if (f.resolvedTpe <:< typeOf[Iterable[_]]) {
                  q"""val v = x.${f.getter}
                      if (!v.isEmpty) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v", f.resolvedTpe, f.isStringified)}
                      }"""
                } else if (f.resolvedTpe <:< typeOf[Option[_]]) {
                  q"""val v = x.${f.getter}
                      if (!v.isEmpty) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v.get", typeArg1(f.resolvedTpe), f.isStringified)}
                      }"""
                } else if (f.resolvedTpe <:< typeOf[Array[_]]) {
                  q"""val v = x.${f.getter}
                      if (v.length > 0) {
                        ..${genWriteConstantKey(f.mappedName)}
                        ..${genWriteVal(q"v", f.resolvedTpe, f.isStringified)}
                      }"""
                } else {
                  q"""..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"x.${f.getter}", f.resolvedTpe, f.isStringified)}"""
                }
            }
          }
          val allWriteFields =
            if (discriminator.isEmpty) writeFields
            else discriminator +: writeFields
          q"""out.writeObjectStart()
              ..$allWriteFields
              out.writeObjectEnd()"""
        } else if (isSealedAdtBase(tpe)) withEncoderFor(methodKey, m) {
          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = leafClasses.map { subTpe =>
            val writeDiscriminatorField =
              q"""..${genWriteConstantKey(codecConfig.discriminatorFieldName)}
                  ..${genWriteConstantVal(discriminatorValue(subTpe))}"""
            cq"x: $subTpe => ${genWriteVal(q"x", subTpe, isStringified, writeDiscriminatorField)}"
          }
          q"""x match {
                case ..$writeSubclasses
              }"""
        } else cannotFindCodecError(tpe)
      }

      def genArrayEquals(tpe: Type): Tree = {
        val tpe1 = typeArg1(tpe)
        if (tpe1 <:< typeOf[Array[_]]) {
          val equals = withEqualsFor(tpe1, q"x1(i)", q"x2(i)")(genArrayEquals(tpe1))
          q"""if (x1 eq x2) true
              else if ((x1 eq null) || (x2 eq null)) false
              else {
                val l1 = x1.length
                val l2 = x2.length
                if (l1 != l2) false
                else {
                  var i = 0
                  while (i < l1 && $equals) i += 1
                  i == l1
                }
              }"""
        } else q"java.util.Arrays.equals(x1, x2)"
      }

      val codec =
        q"""import com.github.plokhotnyuk.jsoniter_scala.core._
            import scala.annotation.switch
            new JsonValueCodec[$rootTpe] {
              def nullValue: $rootTpe = ${nullValue(rootTpe)}
              def decodeValue(in: JsonReader, default: $rootTpe): $rootTpe =
                ${genReadVal(rootTpe, q"default", codecConfig.isStringified, isRoot = true)}
              def encodeValue(x: $rootTpe, out: JsonWriter): Unit =
                ${genWriteVal(q"x", rootTpe, codecConfig.isStringified, isRoot = true)}
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
              ..${fieldTrees.values}
              ..${equalsMethodTrees.values}
              ..${nullValueTrees.values}
            }"""
      if (c.settings.contains("print-codecs")) info(s"Generated JSON codec for type '$rootTpe':\n${showCode(codec)}")
      c.Expr[JsonValueCodec[A]](codec)
    }
  }

  private[this] def decodeName(s: String): String =
    if (s.indexOf('$') >= 0) NameTransformer.decode(s)
    else s

  private[this] def isEncodingRequired(s: String): Boolean = {
    val len = s.length
    var i = 0
    while (i < len && JsonWriter.isNonEscapedAscii(s.charAt(i))) i += 1
    i != len
  }

  private[this] def groupByOrdered[A, K](xs: collection.Seq[A])(f: A => K): mutable.Map[K, collection.Seq[A]] = {
    val m = mutable.LinkedHashMap.empty[K, collection.Seq[A]].withDefault(_ => new ArrayBuffer[A])
    xs.foreach { x =>
      val k = f(x)
      m(k) = m(k) :+ x
    }
    m
  }

  private[this] def duplicated[A](xs: collection.Seq[A]): collection.Seq[A] = {
    val seen = new mutable.HashSet[A]
    val dup = new ArrayBuffer[A]
    xs.foreach[Unit] { x =>
      if (seen(x)) dup += x
      else seen += x
    }
    dup
  }
}