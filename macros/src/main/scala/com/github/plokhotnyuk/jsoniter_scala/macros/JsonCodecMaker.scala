package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field
import scala.collection.generic.Growable
import scala.collection.BitSet
import scala.collection.immutable.{IntMap, LongMap}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.reflect.NameTransformer
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

  def enforce_snake_case(s: String): String = {
    val len = s.length
    val sb = new StringBuilder(len << 1)
    var i = 0
    var isPrecedingLowerCased = false
    while (i < len) isPrecedingLowerCased = {
      val ch = s.charAt(i)
      i += 1
      if (ch == '_' || ch == '-') {
        sb.append('_')
        false
      } else if (isLowerCase(ch)) {
        sb.append(ch)
        true
      } else {
        if (isPrecedingLowerCased || i < len && isLowerCase(s.charAt(i))) sb.append('_')
        sb.append(toLowerCase(ch))
        false
      }
    }
    sb.toString
  }

  def `enforce-kebab-case`(s: String): String = {
    val len = s.length
    val sb = new StringBuilder(len << 1)
    var i = 0
    var isPrecedingLowerCased = false
    while (i < len) isPrecedingLowerCased = {
      val ch = s.charAt(i)
      i += 1
      if (ch == '_' || ch == '-') {
        sb.append('-')
        false
      } else if (isLowerCase(ch)) {
        sb.append(ch)
        true
      } else {
        if (isPrecedingLowerCased || i < len && isLowerCase(s.charAt(i))) sb.append('-')
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

      def methodType(m: MethodSymbol): Type = m.returnType.dealias

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs.tail.head.dealias

      def isCaseClass(tpe: Type): Boolean = tpe.typeSymbol.asClass.isCaseClass

      val tupleSymbols: Set[Symbol] = definitions.TupleClass.seq.toSet

      def isTuple(tpe: Type): Boolean = tupleSymbols(tpe.typeSymbol)

      def isValueClass(tpe: Type): Boolean = tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueMethod(tpe: Type): MethodSymbol = tpe.decls.head.asMethod

      def valueClassValueType(tpe: Type): Type = methodType(valueClassValueMethod(tpe))

      def isSealedAdtBase(tpe: Type): Boolean = {
        val classSymbol = tpe.typeSymbol.asClass
        (classSymbol.isAbstract || classSymbol.isTrait) &&
          (if (classSymbol.isSealed) true
          else fail("Only sealed traits & abstract classes are supported for an ADT base. Please consider adding " +
            s"of a sealed definition for '$tpe' or using a custom implicitly accessible codec for the ADT base."))
      }

      def adtLeafClasses(tpe: Type): Seq[Type] = {
        def collectRecursively(tpe: Type): Set[Type] =
          tpe.typeSymbol.asClass.knownDirectSubclasses.flatMap { s =>
            val subTpe = s.asClass.toType
            if (isSealedAdtBase(subTpe)) collectRecursively(subTpe)
            else if (isCaseClass(subTpe)) Set(subTpe)
            else fail("Only case classes & case objects are supported for ADT leaf classes. Please consider using " +
              s"of them for ADT with base '$tpe' or using a custom implicitly accessible codec for the ADT base.")
          }

        val classes = collectRecursively(tpe).toSeq
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$tpe'. Please consider adding them or " +
          "using a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def companion(tpe: Type): Symbol = tpe.typeSymbol.companion

      def isContainer(tpe: Type): Boolean =
        tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Array[_]]

      def collectionCompanion(tpe: Type): Tree = {
        val comp = companion(tpe)
        if (comp.isModule && comp.fullName.startsWith("scala.collection.")) Ident(comp)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")
      }

      def enumSymbol(tpe: Type): Symbol = {
        val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
        enumSymbol
      }

      def getType(typeTree: Tree): Type = c.typecheck(typeTree, c.TYPEmode).tpe

      def eval[B](tree: Tree): B = c.eval[B](c.Expr[B](c.untypecheck(tree)))

      val codecConfig = eval[CodecMakerConfig](config.tree)
      val inferredKeyCodecs: mutable.Map[Type, Tree] = mutable.Map.empty

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

      val inferredCodecs: mutable.Map[Type, Tree] = mutable.Map.empty

      def findImplicitCodec(tpe: Type): Tree =
        inferredCodecs.getOrElseUpdate(tpe, c.inferImplicitValue(getType(tq"JsonValueCodec[$tpe]")))

      case class FieldAnnotations(name: String, transient: Boolean, stringified: Boolean)

      def getFieldAnnotations(tpe: Type): Map[String, FieldAnnotations] = tpe.members.collect {
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
          val mappedName = named.headOption.flatMap(x => Option(eval[named](x.tree).name)).getOrElse(name)
          (name, FieldAnnotations(mappedName, trans.nonEmpty, strings.nonEmpty))
      }.toMap

      def getModule(tpe: Type): ModuleSymbol = {
        val comp = tpe.typeSymbol.companion
        if (!comp.isModule) {
          fail("Can't find companion object with synthetic methods for default values of the primary constructor of " +
            s"'$tpe'. This can happen when it's nested too deeply. Please consider defining it as a top-level object " +
            "or directly inside of another class or object.")
        }
        comp.asModule //FIXME: module cannot be resolved properly for deeply nested inner case classes
      }

      def getPrimaryConstructor(tpe: Type): MethodSymbol = tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.get // FIXME: while in Scala, every class has a primary constructor, but sometime it cannot be accessed

      def getParams(tpe: Type): Seq[TermSymbol] = getPrimaryConstructor(tpe).paramLists match {
        case paramList :: Nil => paramList.map(_.asTerm)
        case _ => fail(s"'$tpe' has a primary constructor with multiple parameter lists. " +
          "Please consider using a custom implicitly accessible codec for this type.")
      }

      def getDefaults(tpe: Type): Map[String, Tree] = {
        val params = getParams(tpe)
        lazy val module = getModule(tpe) // don't lookup for the companion when there are no default values for constructor params
        params.zipWithIndex.collect { case (p, i) if p.isParamWithDefault =>
          (decodedName(p), q"$module.${TermName("$lessinit$greater$default$" + (i + 1))}")
        }.toMap
      }

      def getMembers(annotations: Map[String, FieldAnnotations], tpe: c.universe.Type): Seq[MethodSymbol] = {
        def nonTransient(m: MethodSymbol): Boolean = annotations.get(decodedName(m)).fold(true)(!_.transient)

        tpe.members.collect {
          case m: MethodSymbol if m.isCaseAccessor && nonTransient(m) => m
        }.toSeq.reverse
      }

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
             } else in.duplicatedKeyError(l)
          """
      }

      def discriminatorValue(tpe: Type): String = codecConfig.adtLeafClassNameMapper(decodeName(tpe.typeSymbol.fullName))

      def getStringified(annotations: Map[String, FieldAnnotations], name: String): Boolean =
        annotations.get(name).fold(false)(_.stringified)

      def getMappedName(annotations: Map[String, FieldAnnotations], defaultName: String): String =
        annotations.get(defaultName).fold(codecConfig.fieldNameMapper(defaultName))(_.name)

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

      // use it only for immutable values which doesn't have public constants
      def withNullValueFor(tpe: Type)(f: => Tree): Tree = {
        val nullValueName = nullValueNames.getOrElseUpdate(tpe, TermName("v" + nullValueNames.size))
        nullValueTrees.getOrElseUpdate(tpe, q"private[this] val $nullValueName: $tpe = $f")
        Ident(nullValueName)
      }

      val fieldNames = mutable.LinkedHashMap.empty[Type, TermName]
      val fieldTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withFieldsFor(tpe: Type)(f: => Seq[String]): Tree = {
        val reqFieldName = fieldNames.getOrElseUpdate(tpe, TermName("f" + fieldNames.size))
        fieldTrees.getOrElseUpdate(tpe, q"private[this] val $reqFieldName: Array[String] = Array(..$f)")
        Ident(reqFieldName)
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
        else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[LongMap[_]] || tpe <:< typeOf[mutable.LongMap[_]]) {
          q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) q"${collectionCompanion(tpe)}.empty"
        else if (tpe <:< typeOf[Iterable[_]]) q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
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
          genReadMap(q"var x = $comp.empty[$tpe1]",
            q"x = x.updated(in.readKeyAsInt(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1]",
            q"x.update(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1]",
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
          genReadMap(q"var x = $comp.empty[$tpe1, $tpe2]",
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
        } else if (tpe <:< typeOf[mutable.Iterable[_] with Growable[_]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { // ArrayStack uses 'push' for '+='
          val tpe1 = typeArg1(tpe)
          val comp = collectionCompanion(tpe)
          genReadArray(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1]",
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
            val t = tpe.typeArgs.head
            q"val _1: $t = ${genReadVal(t, nullValue(t), isStringified)}": Tree
          }{ case (acc, (t, i)) =>
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
        } else if (isCaseClass(tpe)) withDecoderFor(methodKey, default) {
          val annotations = getFieldAnnotations(tpe)

          def name(m: Symbol): String = getMappedName(annotations, decodedName(m))

          def hashCode(m: Symbol): Int = {
            val cs = name(m).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }

          val members = getMembers(annotations, tpe)
          checkFieldNameCollisions(tpe,
            (if (discriminator.isEmpty) Seq.empty else Seq(codecConfig.discriminatorFieldName)) ++ members.map(name))
          val params = getParams(tpe).filterNot { p =>
            annotations.get(decodedName(p)).fold(false)(_.transient)
          }
          val required = params.collect {
            case p if !p.isParamWithDefault && !isContainer(p.typeSignature) => name(p)
          }
          val paramVarNum = params.size
          val lastParamVarIndex = paramVarNum >> 5
          val lastParamVarBits = (1 << paramVarNum) - 1
          val paramVarNames = (0 to lastParamVarIndex).map(i => TermName("p" + i))
          val bitmasks: Map[String, Tree] = params.zipWithIndex.map {
            case (r, i) =>
              val n = paramVarNames(i >> 5)
              val bit = 1 << i
              (decodedName(r), q"if (($n & $bit) != 0) $n ^= $bit else in.duplicatedKeyError(l)")
          }.toMap
          val paramVars =
            paramVarNames.init.map(n => q"var $n = -1") :+ q"var ${paramVarNames.last} = $lastParamVarBits"
          val checkReqVars = if (required.isEmpty) Nil else {
            val fields = withFieldsFor(tpe)(params.map(name))
            val reqSet = required.toSet
            val reqMasks = params.grouped(32).map(_.zipWithIndex.foldLeft(0) { case (acc, (n, i)) =>
              acc | (if (reqSet(name(n))) 1 << i else 0)
            }).toSeq
            paramVarNames.zipWithIndex.map { case (n, i) =>
              val m = reqMasks(i)
              if (i == 0) q"if (($n & $m) != 0) in.requiredFieldError($fields(Integer.numberOfTrailingZeros($n)))"
              else q"if (($n & $m) != 0) in.requiredFieldError($fields(Integer.numberOfTrailingZeros($n) + ${i << 5}))"
            }
          }
          val construct = q"new $tpe(..${members.map(m => q"${m.name} = ${TermName("_" + m.name)}")})"
          val defaults = getDefaults(tpe)
          val readVars = members.map { m =>
            val tpe = methodType(m)
            q"var ${TermName("_" + m.name)}: $tpe = ${defaults.getOrElse(decodedName(m), nullValue(tpe))}"
          }
          val readFields = groupByOrdered(members)(hashCode).map { case (hashCode, ms) =>
            val checkNameAndReadValue = ms.foldRight(unexpectedFieldHandler) { case (m, acc) =>
              val varName = TermName("_" + m.name)
              val isStringified = getStringified(annotations, decodedName(m))
              val readValue = q"$varName = ${genReadVal(methodType(m), q"$varName", isStringified)}"
              val resetFieldFlag = bitmasks(decodedName(m))
              q"""if (in.isCharBufEqualsTo(l, ${name(m)})) {
                    ..$resetFieldFlag
                    ..$readValue
                  } else $acc"""
            }
            cq"$hashCode => $checkNameAndReadValue"
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
          val writeFields = tpe.typeArgs.zipWithIndex.map { case (t, i) =>
            q"""out.writeComma()
                ${genWriteVal(q"x.${TermName("_" + (i + 1))}", t, isStringified)}"""
          }
          q"""out.writeArrayStart()
              ..$writeFields
              out.writeArrayEnd()"""
        } else if (isCaseClass(tpe)) withEncoderFor(methodKey, m) {
          val annotations = getFieldAnnotations(tpe)
          val members = getMembers(annotations, tpe)
          val defaults = getDefaults(tpe)
          val writeFields = members.map { m =>
            val tpe = methodType(m)
            val name = decodedName(m)
            val mappedName = getMappedName(annotations, name)
            val isStringified = getStringified(annotations, name)
            defaults.get(name) match {
              case Some(d) =>
                if (tpe <:< typeOf[Iterable[_]]) {
                  q"""val v = x.$m
                      if (!v.isEmpty && v != $d) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else if (tpe <:< typeOf[Option[_]]) {
                  q"""val v = x.$m
                      if (!v.isEmpty && v != $d) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v.get", typeArg1(tpe), isStringified)}
                      }"""
                } else if (tpe <:< typeOf[Array[_]]) {
                  q"""val v = x.$m
                      if (v.length > 0 && {
                            val d = $d
                            v.length != d.length || v.deep != d.deep
                          }) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else {
                  q"""val v = x.$m
                      if (v != $d) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                }
              case None =>
                if (tpe <:< typeOf[Iterable[_]]) {
                  q"""val v = x.$m
                      if (!v.isEmpty) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else if (tpe <:< typeOf[Option[_]]) {
                  q"""val v = x.$m
                      if (!v.isEmpty) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v.get", typeArg1(tpe), isStringified)}
                      }"""
                } else if (tpe <:< typeOf[Array[_]]) {
                  q"""val v = x.$m
                      if (v.length > 0) {
                        ..${genWriteConstantKey(mappedName)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else {
                  q"""..${genWriteConstantKey(mappedName)}
                      ..${genWriteVal(q"x.$m", tpe, isStringified)}"""
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

      val codec =
        q"""import com.github.plokhotnyuk.jsoniter_scala.core._
            import scala.annotation.switch
            new JsonValueCodec[$rootTpe] {
              def nullValue: $rootTpe = ${nullValue(rootTpe)}
              def decodeValue(in: JsonReader, default: $rootTpe): $rootTpe =
                ${genReadVal(rootTpe, q"default", codecConfig.isStringified, isRoot = true)}
              def encodeValue(x: $rootTpe, out: JsonWriter): Unit =
                ${genWriteVal(q"x", rootTpe, codecConfig.isStringified, isRoot = true)}
              ..${nullValueTrees.values}
              ..${fieldTrees.values}
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
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

  private[this] def groupByOrdered[A, K](xs: Seq[A])(f: A => K): mutable.Map[K, Seq[A]] = {
    val m = mutable.LinkedHashMap.empty[K, Seq[A]].withDefault(_ => new ArrayBuffer[A])
    xs.foreach { x =>
      val k = f(x)
      m(k) = m(k) :+ x
    }
    m
  }

  private[this] def duplicated[A](xs: Seq[A]): Seq[A] = {
    val seen = new mutable.HashSet[A]
    val dup = new ArrayBuffer[A]
    xs.foreach[Unit] { x =>
      if (seen(x)) dup += x
      else seen += x
    }
    dup
  }
}