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
import scala.util.control.NonFatal

@field
final class named(val name: String) extends StaticAnnotation

@field
final class transient extends StaticAnnotation

@field
final class stringified extends StaticAnnotation

/**
  * Configuration parameter for `JsonCodecMaker.make()` call.
  *
  * BEWARE: a parameter of the `make` macro should not depend on code from the same compilation module where it is called.
  * Use a separated submodule of the project to compile all such dependencies before their usage for generation of codecs.
  *
  * Examples of `fieldNameMapper` and `adtLeafClassNameMapper` functions that have no dependencies in the same
  * compilation module are: `JsonCodecMaker.enforceCamelCase`, `JsonCodecMaker.enforce_snake_case`,
  * `JsonCodecMaker.enforce-kebab-case`, and `JsonCodecMaker.simpleClassName`. Or their composition like:
  * `s => JsonCodecMaker.enforce_snake_case(JsonCodecMaker.simpleClassName(s))`
  *
  * @param fieldNameMapper        the function of mapping from string of case class field name to JSON key (an identity
  *                               function by default)
  * @param adtLeafClassNameMapper the function of mapping from string of case class/object full name to string value of
  *                               discriminator field (a function that truncate to simple class name by default)
  * @param discriminatorFieldName an optional name of discriminator field, where None can be used for alternative
  *                               representation of ADTs without the discriminator field (Some("type") value by default)
  * @param isStringified          a flag that turn on stringification of number or boolean values of collections,
  *                               options and value classes (turned off by default)
  * @param skipUnexpectedFields   a flag that turn on skipping of unexpected fields or in other case a parse exception
  *                               will be thrown (turned on by default)
  * @param transientDefault       a flag that turn on skipping serialization of fields that have same values as default
  *                               values defined for them in the primary constructor (turned on by default)
  * @param bigDecimalPrecision    a precision in 'BigDecimal' values (34 by default)
  * @param bigDecimalScaleLimit   an exclusive limit for accepted scale in 'BigDecimal' values (6178 by default)
  * @param bigDecimalDigitsLimit  an exclusive limit for accepted number of decimal digits in 'BigDecimal' values (308 by default)
  * @param bigIntDigitsLimit      an exclusive limit for accepted number of decimal digits in 'BigInt' values (308 by default)
  * @param bitSetValueLimit       an exclusive limit for accepted numeric values in bit sets (1024 by default)
  * @param mapMaxInsertNumber     a max number of inserts into maps (1024 by default)
  * @param setMaxInsertNumber     a max number of inserts into sets excluding bit sets (1024 by default)
  */
case class CodecMakerConfig(
  fieldNameMapper: String => String = identity,
  adtLeafClassNameMapper: String => String = JsonCodecMaker.simpleClassName,
  discriminatorFieldName: Option[String] = Some("type"),
  isStringified: Boolean = false,
  skipUnexpectedFields: Boolean = true,
  transientDefault: Boolean = true,
  bigDecimalPrecision: Int = 34, // precision for decimal128: java.math.MathContext.DECIMAL128.getPrecision
  bigDecimalScaleLimit: Int = 6178, // limit for scale for decimal128: BigDecimal("0." + "0" * 33 + "1e-6143", java.math.MathContext.DECIMAL128).scale + 1
  bigDecimalDigitsLimit: Int = 308, // 128 bytes: (BigDecimal(BigInt("9" * 307))).underlying.unscaledValue.toByteArray.length
  bigIntDigitsLimit: Int = 308, // 128 bytes: (BigInt("9" * 307)).underlying.toByteArray.length
  bitSetValueLimit: Int = 1024, // 128 bytes: collection.mutable.BitSet(1023).toBitMask.length * 8
  mapMaxInsertNumber: Int = 1024, // to limit attacks from untrusted input that exploit worst complexity for inserts
  setMaxInsertNumber: Int = 1024) // to limit attacks from untrusted input that exploit worst complexity for inserts

object JsonCodecMaker {
  /**
    * Mapping function for field or class names that should be in camelCase format.
    *
    * @param s the name to transform
    * @return a transformed name or the same name if no transformation is required
    */
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
        (ch == '_' || ch == '-') || {
          val fixedCh =
            if (isPrecedingDash) toUpperCase(ch)
            else toLowerCase(ch)
          sb.append(fixedCh)
          false
        }
      }
      sb.toString
    }

  /**
    * Mapping function for field or class names that should be in snake_case format.
    *
    * @param s the name to transform
    * @return a transformed name or the same name if no transformation is required
    */
  def enforce_snake_case(s: String): String = enforceSnakeOrKebabCase(s, '_')

  /**
    * Mapping function for field or class names that should be in kebab-case.
    *
    * @param s the name to transform
    * @return a transformed name or the same name if no transformation is required
    */
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
        if (isPrecedingLowerCased || i > 1 && i < len && isLowerCase(s.charAt(i))) sb.append(separator)
        sb.append(toLowerCase(ch))
        false
      }
    }
    sb.toString
  }

  /**
    * Mapping function for class names that should be trimmed to the simple class name without package prefix.
    *
    * @param fullClassName the name to transform
    * @return a transformed name or the same name if no transformation is required
    */
  def simpleClassName(fullClassName: String): String =
    fullClassName.substring(Math.max(fullClassName.lastIndexOf('.') + 1, 0))

  def make[A](config: CodecMakerConfig): JsonValueCodec[A] = macro Impl.make[A]

  private object Impl {
    def make[A: c.WeakTypeTag](c: blackbox.Context)(config: c.Expr[CodecMakerConfig]): c.Expr[JsonValueCodec[A]] = {
      import c.universe._

      def fail(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

      def warn(msg: String): Unit = c.warning(c.enclosingPosition, msg)

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs(1).dealias

      val tupleSymbols: Set[Symbol] = definitions.TupleClass.seq.toSet

      def isTuple(tpe: Type): Boolean = tupleSymbols(tpe.typeSymbol)

      def isValueClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueMethod(tpe: Type): MethodSymbol = tpe.decls.head.asMethod

      def substituteTypes(tpe: Type, from: List[Symbol], to: List[Type]): Type =
        try tpe.substituteTypes(from, to) catch { case NonFatal(_) =>
          fail(s"Cannot resolve generic type(s) for `$tpe`. Please provide a custom implicitly accessible codec for it.")
        }

      def resolveConcreteType(tpe: Type, mtpe: Type): Type = {
        val tpeTypeParams =
          if (tpe.typeSymbol.isClass) tpe.typeSymbol.asClass.typeParams
          else Nil
        if (tpeTypeParams.isEmpty) mtpe
        else substituteTypes(mtpe, tpeTypeParams, tpe.typeArgs)
      }

      def methodType(tpe: Type, m: MethodSymbol): Type = resolveConcreteType(tpe, m.returnType.dealias)

      def paramType(tpe: Type, p: TermSymbol): Type = resolveConcreteType(tpe, p.typeSignature.dealias)

      def valueClassValueType(tpe: Type): Type = methodType(tpe, valueClassValueMethod(tpe))

      def isNonAbstractScalaClass(tpe: Type): Boolean =
        tpe.typeSymbol.isClass && !tpe.typeSymbol.asClass.isAbstract && !tpe.typeSymbol.asClass.isJava

      def isSealedClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isSealed

      def adtLeafClasses(tpe: Type): Seq[Type] = {
        def collectRecursively(tpe: Type): Seq[Type] =
          if (tpe.typeSymbol.isClass) {
            val leafTpes = tpe.typeSymbol.asClass.knownDirectSubclasses.toSeq.flatMap { s =>
              val classSymbol = s.asClass
              val subTpe =
                if (classSymbol.typeParams.isEmpty) classSymbol.toType
                else substituteTypes(classSymbol.toType, classSymbol.typeParams, tpe.typeArgs)
              if (isSealedClass(subTpe)) collectRecursively(subTpe)
              else if (isNonAbstractScalaClass(subTpe)) Seq(subTpe)
              else fail("Only Scala classes & objects are supported for ADT leaf classes. Please consider using of " +
                s"them for ADT with base '$tpe' or provide a custom implicitly accessible codec for the ADT base.")
            }
            if (isNonAbstractScalaClass(tpe)) leafTpes :+ tpe
            else leafTpes
          } else Seq.empty

        val classes = collectRecursively(tpe)
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$tpe'. Please add them or " +
          "provide a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def companion(tpe: Type): Symbol = {
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

        val comp = tpe.typeSymbol.companion
        if (comp.isModule) comp
        else patchedCompanionRef(tpe).symbol
      }

      def isContainer(tpe: Type): Boolean =
        tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Array[_]]

      def collectionCompanion(tpe: Type): Tree =
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ident(tpe.typeSymbol.companion)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")

      def enumSymbol(tpe: Type): Symbol = {
        val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
        enumSymbol
      }

      def getType(typeTree: Tree): Type = c.typecheck(typeTree, c.TYPEmode).tpe

      def eval[B](tree: Tree): B = c.eval[B](c.Expr[B](c.untypecheck(tree)))

      val rootTpe = weakTypeOf[A].dealias
      val codecConfig = // FIXME: scalac can throw the stack overflow error here, see: https://github.com/scala/bug/issues/11157
        try eval[CodecMakerConfig](config.tree) catch { case _: Throwable =>
          fail(s"Cannot evaluate a parameter of the 'make' macro call for type '$rootTpe'. " +
            "It should not depend on code from the same compilation module where the 'make' macro is called. " +
            "Use a separated submodule of the project to compile all such dependencies before their usage for " +
            "generation of codecs.")
        }
      val inferredKeyCodecs: mutable.Map[Type, Tree] = mutable.Map.empty
      val inferredValueCodecs: mutable.Map[Type, Tree] = mutable.Map.empty

      def findImplicitKeyCodec(tpe: Type): Tree = inferredKeyCodecs.getOrElseUpdate(tpe,
        c.inferImplicitValue(getType(tq"com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[$tpe]")))

      def findImplicitValueCodec(tpe: Type): Tree = inferredValueCodecs.getOrElseUpdate(tpe,
        c.inferImplicitValue(getType(tq"com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$tpe]")))

      val mathContextNames = mutable.LinkedHashMap.empty[Int, TermName]
      val mathContextTrees = mutable.LinkedHashMap.empty[Int, Tree]

      def withMathContextFor(precision: Int): Tree =
        if (precision == java.math.MathContext.DECIMAL128.getPrecision) q"java.math.MathContext.DECIMAL128"
        else if (precision == java.math.MathContext.DECIMAL64.getPrecision) q"java.math.MathContext.DECIMAL64"
        else if (precision == java.math.MathContext.DECIMAL32.getPrecision) q"java.math.MathContext.DECIMAL32"
        else if (precision == java.math.MathContext.UNLIMITED.getPrecision) q"java.math.MathContext.UNLIMITED"
        else {
          val mc = q"new java.math.MathContext(${codecConfig.bigDecimalPrecision}, java.math.RoundingMode.HALF_EVEN)"
          val mathContextName = mathContextNames.getOrElseUpdate(precision, TermName("mc" + mathContextNames.size))
          mathContextTrees.getOrElseUpdate(precision, q"private[this] val $mathContextName: java.math.MathContext = $mc")
          Ident(mathContextName)
        }

      case class EnumValueInfo(value: Tree, name: String)

      def javaEnumValues(tpe: Type): Seq[EnumValueInfo] =
        tpe.typeSymbol.asClass.knownDirectSubclasses.toSeq.map { s: Symbol =>
          val n = s.fullName
          EnumValueInfo(q"$s", n.substring(n.lastIndexOf('.') + 1))
        } // FIXME: Scala 2.11.x returns empty set of subclasses for Java enums

      def genReadEnumValue(enumValues: Seq[EnumValueInfo], unexpectedEnumValueHandler: Tree): Tree = {
        val hashCode: EnumValueInfo => Int = e => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
        val length: EnumValueInfo => Int = _.name.length

        def genReadCollisions(es: collection.Seq[EnumValueInfo]): Tree =
          es.foldRight(unexpectedEnumValueHandler) { case (e, acc) =>
            q"if (in.isCharBufEqualsTo(l, ${e.name})) ${e.value} else $acc"
          }

        if (enumValues.size <= 4 && enumValues.size == enumValues.map(length).distinct.size) {
          genReadCollisions(enumValues)
        } else {
          val cases = groupByOrdered(enumValues)(hashCode).map { case (hash, fs) =>
            cq"$hash => ${genReadCollisions(fs)}"
          }.toSeq :+ cq"_ => $unexpectedEnumValueHandler"
          q"""(in.charBufToHashCode(l): @switch) match {
                case ..$cases
              }"""
        }
      }

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
        else if (tpe =:= typeOf[BigInt]) q"in.readKeyAsBigInt(${codecConfig.bigIntDigitsLimit})"
        else if (tpe =:= typeOf[BigDecimal]) {
          val mc = withMathContextFor(codecConfig.bigDecimalPrecision)
          q"in.readKeyAsBigDecimal($mc, ${codecConfig.bigDecimalScaleLimit}, ${codecConfig.bigDecimalDigitsLimit})"
        } else if (tpe =:= typeOf[java.util.UUID]) q"in.readKeyAsUUID()"
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
          val es = javaEnumValues(tpe)
          if (es.isEmpty) {
            q"""val v = in.readKeyAsString()
                try ${companion(tpe)}.valueOf(v) catch {
                  case _: IllegalArgumentException => in.enumValueError(v)
                }"""
          } else {
            q"""val l = in.readKeyAsCharBuf()
                ${genReadEnumValue(es, q"in.enumValueError(l)")}"""
          }
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

      def genReadSet(newBuilder: Tree, readVal: Tree, result: Tree = q"x"): Tree =
        q"""if (in.isNextToken('[')) {
              if (in.isNextToken(']')) default
              else {
                in.rollbackToken()
                ..$newBuilder
                var i = 0
                do {
                  ..$readVal
                  i += 1
                  if (i > ${codecConfig.setMaxInsertNumber}) in.decodeError("too many set inserts")
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
                var i = 0
                do {
                  ..$readKV
                  i += 1
                  if (i > ${codecConfig.mapMaxInsertNumber}) in.decodeError("too many map inserts")
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
        else if (tpe <:< typeOf[java.lang.Enum[_]]) {
          val es = javaEnumValues(tpe)
          if (es.isEmpty || es.exists(x => isEncodingRequired(x.name))) q"out.writeKey($x.name)"
          else q"out.writeNonEscapedAsciiKey($x.name)"
        } else fail(s"Unsupported type to be used as map key '$tpe'.")
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

        def decodeName(s: Symbol): String = NameTransformer.decode(s.name.toString)

        def getPrimaryConstructor(tpe: Type): MethodSymbol = tpe.decls.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m // FIXME: sometime it cannot be accessed from the place of the `make` call
        }.getOrElse(fail(s"Cannot find a primary constructor for '$tpe'"))

        lazy val module = companion(tpe).asModule // don't lookup for the companion when there are no default values for constructor params
        val getters = tpe.members.collect { case m: MethodSymbol if m.isParamAccessor && m.isGetter => m }
        val annotations = tpe.members.collect {
          case m: TermSymbol if {
            m.info // to enforce the type information completeness and availability of annotations
            m.annotations.exists(a => a.tree.tpe =:= typeOf[named] || a.tree.tpe =:= typeOf[transient] ||
              a.tree.tpe =:= typeOf[stringified])
          } =>
            val name = decodeName(m).trim // FIXME: Why is there a space at the end of field name?!
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
            val partiallyMappedName = named.headOption.flatMap(a => Option {
              try eval[named](a.tree).name catch { case _: Throwable => // FIXME: scalac can throw the stack overflow error here, see: https://github.com/scala/bug/issues/11157
                fail(s"Cannot evaluate a parameter of the '@named' annotation in type '$tpe'. " +
                  "It should not depend on code from the same compilation module where the 'make' macro is called. " +
                  "Use a separated submodule of the project to compile all such dependencies before their usage " +
                  "for generation of codecs.")
              }
            }).getOrElse(name)
            (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        ClassInfo(tpe, getPrimaryConstructor(tpe).paramLists match {
          case params :: Nil => params.zipWithIndex.flatMap { case (p, i) =>
            val symbol = p.asTerm
            val name = decodeName(symbol)
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

      val unexpectedFieldHandler =
        if (codecConfig.skipUnexpectedFields) q"in.skip()"
        else q"in.unexpectedKeyError(l)"
      val skipDiscriminatorField =
        q"""if (pd) {
              pd = false
              in.skip()
            } else in.duplicatedKeyError(l)"""

      def discriminatorValue(tpe: Type): String =
        codecConfig.adtLeafClassNameMapper(NameTransformer.decode(tpe.typeSymbol.fullName))

      def checkFieldNameCollisions(tpe: Type, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON key(s) defined for '$tpe': $formattedCollisions. Keys are derived from field names of " +
            s"the class that are mapped by the '${typeOf[CodecMakerConfig]}.fieldNameMapper' function or can be overridden " +
            s"by '${typeOf[named]}' annotation(s). Result keys should be unique and should not match with a key for the " +
            s"discriminator field that is specified by the '${typeOf[CodecMakerConfig]}.discriminatorFieldName' option.")
        }
      }

      def checkDiscriminatorValueCollisions(tpe: Type, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated discriminator defined for ADT base '$tpe': $formattedCollisions. Values for leaf classes of ADT " +
            s"that are returned by the '${typeOf[CodecMakerConfig]}.adtLeafClassNameMapper' function should be unique.")
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
        equalsMethodTrees.getOrElseUpdate(tpe, q"private[this] def $equalsMethodName(x1: $tpe, x2: $tpe): Boolean = $f")
        q"$equalsMethodName($arg1, $arg2)"
      }

      def genArrayEquals(tpe: Type): Tree = {
        val tpe1 = typeArg1(tpe)
        if (tpe1 <:< typeOf[Array[_]]) {
          val equals = withEqualsFor(tpe1, q"x1(i)", q"x2(i)")(genArrayEquals(tpe1))
          q"""(x1 eq x2) || ((x1 ne null) && (x2 ne null) && {
                val l1 = x1.length
                val l2 = x2.length
                (l1 == l2) && {
                  var i = 0
                  while (i < l1 && $equals) i += 1
                  i == l1
                }
              })"""
        } else q"java.util.Arrays.equals(x1, x2)"
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
        else if (tpe <:< typeOf[Option[_]]) q"None"
        else if (tpe <:< typeOf[mutable.BitSet]) q"${collectionCompanion(tpe)}.empty"
        else if (tpe <:< typeOf[BitSet]) withNullValueFor(tpe)(q"${collectionCompanion(tpe)}.empty")
        else if (tpe <:< typeOf[mutable.LongMap[_]]) q"${collectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[List[_]]) q"Nil"
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
        else if (tpe <:< typeOf[AnyRef]) q"null"
        else q"null.asInstanceOf[$tpe]"

      def genReadNonAbstractScalaClass(tpe: Type, default: Tree, isStringified: Boolean, discriminator: Tree): Tree = {
        val classInfo = getClassInfo(tpe)
        checkFieldNameCollisions(tpe, codecConfig.discriminatorFieldName.fold(Seq.empty[String]) { n =>
          val names = classInfo.fields.map(_.mappedName)
          if (discriminator.isEmpty) names
          else names :+ n
        })
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
        val checkReqVars =
          if (required.isEmpty) Nil
          else {
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
        val length: FieldInfo => Int = _.mappedName.length
        val readFields = codecConfig.discriminatorFieldName.fold(classInfo.fields) { n =>
          if (discriminator.isEmpty) classInfo.fields
          else classInfo.fields :+ FieldInfo(null, n, null, null, null, null, true)
        }

        def genReadCollisions(fs: collection.Seq[FieldInfo]): Tree =
          fs.foldRight(unexpectedFieldHandler) { case (f, acc) =>
            val readValue =
              if (discriminator.nonEmpty && codecConfig.discriminatorFieldName.contains(f.mappedName)) discriminator
              else {
                q"""${checkAndResetFieldPresenceFlags(f.mappedName)}
                    ${f.tmpName} = ${genReadVal(f.resolvedTpe, q"${f.tmpName}", f.isStringified)}"""
              }
            q"if (in.isCharBufEqualsTo(l, ${f.mappedName})) $readValue else $acc"
          }

        val readFieldsBlock =
          if (readFields.size <= 4 && readFields.size == readFields.map(length).distinct.size) {
            genReadCollisions(readFields)
          } else {
            val cases = groupByOrdered(readFields)(hashCode).map { case (hash, fs) =>
              cq"$hash => ${genReadCollisions(fs)}"
            }.toSeq :+ cq"_ => $unexpectedFieldHandler"
            q"""(in.charBufToHashCode(l): @switch) match {
                    case ..$cases
                }"""
          }
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
                  ..$readFieldsBlock
                } while (in.isNextToken(','))
                if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
              }
              ..$checkReqVars
              $construct
            } else in.readNullOrTokenError(default, '{')"""
      }

      def genReadVal(tpe: Type, default: Tree, isStringified: Boolean, discriminator: Tree = EmptyTree,
                     isRoot: Boolean = false): Tree = {
        val implCodec =
          if (isRoot) EmptyTree
          else findImplicitValueCodec(tpe)
        val methodKey = getMethodKey(tpe, isStringified, discriminator)
        val decodeMethodName = decodeMethodNames.get(methodKey)
        if (!implCodec.isEmpty) q"$implCodec.decodeValue(in, $default)"
        else if (decodeMethodName.isDefined) q"${decodeMethodName.get}(in, $default)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) {
          if (isStringified) q"in.readStringAsBoolean()"
          else q"in.readBoolean()"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) {
          if (isStringified) q"in.readStringAsByte()"
          else q"in.readByte()"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) {
          if (isStringified) q"in.readStringAsShort()"
          else q"in.readShort()"
        } else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) {
          if (isStringified) q"in.readStringAsInt()"
          else q"in.readInt()"
        } else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) {
          if (isStringified) q"in.readStringAsLong()"
          else q"in.readLong()"
        } else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) {
          if (isStringified) q"in.readStringAsFloat()"
          else q"in.readFloat()"
        } else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          if (isStringified) q"in.readStringAsDouble()"
          else q"in.readDouble()"
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
          if (isStringified) q"in.readStringAsBigInt($default, ${codecConfig.bigIntDigitsLimit})"
          else q"in.readBigInt($default, ${codecConfig.bigIntDigitsLimit})"
        } else if (tpe =:= typeOf[BigDecimal]) {
          val mc = withMathContextFor(codecConfig.bigDecimalPrecision)
          if (isStringified) {
            q"in.readStringAsBigDecimal($default, $mc, ${codecConfig.bigDecimalScaleLimit}, ${codecConfig.bigDecimalDigitsLimit})"
          } else {
            q"in.readBigDecimal($default, $mc, ${codecConfig.bigDecimalScaleLimit}, ${codecConfig.bigDecimalDigitsLimit})"
          }
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
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"${collectionCompanion(tpe)}.empty[$tpe1]")}",
            q"x = x.updated(in.readKeyAsInt(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else ${collectionCompanion(tpe)}.empty[$tpe1]",
            q"x.update(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"${collectionCompanion(tpe)}.empty[$tpe1]")}",
            q"x = x.updated(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else ${collectionCompanion(tpe)}.empty[$tpe1, $tpe2]",
            q"x.update(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})")
        } else if (tpe <:< typeOf[collection.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          genReadMap(q"var x = ${withNullValueFor(tpe)(q"${collectionCompanion(tpe)}.empty[$tpe1,$tpe2]")}",
            if (tpe <:< typeOf[Map[_, _]]) q"x = x.updated(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})"
            else q"x = x + ((${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)}))")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(methodKey, default) {
          val readVal =
            if (isStringified) q"in.readStringAsInt()"
            else q"in.readInt()"
          genReadArray(q"var x = new Array[Long](2)",
            q"""val v = $readVal
                if (v < 0 || v >= ${codecConfig.bitSetValueLimit}) in.decodeError("illegal value for bit set")
                val i = v >>> 6
                if (i >= x.length) x = java.util.Arrays.copyOf(x, java.lang.Integer.highestOneBit(i) << 1)
                x(i) |= 1L << v""",
            if (tpe =:= typeOf[BitSet]) q"collection.immutable.BitSet.fromBitMaskNoCopy(x)"
            else q"${collectionCompanion(tpe)}.fromBitMaskNoCopy(x)")
        } else if (tpe <:< typeOf[mutable.Set[_] with mutable.Builder[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"val x = if (default.isEmpty) default else ${collectionCompanion(tpe)}.empty[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}")
        } else if (tpe <:< typeOf[collection.Set[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"val x = ${collectionCompanion(tpe)}.newBuilder[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.result()")
        } else if (tpe <:< typeOf[List[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = new collection.mutable.ListBuffer[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.toList")
        } else if (tpe <:< typeOf[mutable.Iterable[_] with mutable.Builder[_, _]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { //ArrayStack uses 'push' for '+='
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = if (default.isEmpty) default else ${collectionCompanion(tpe)}.empty[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}")
        } else if (tpe <:< typeOf[Iterable[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = ${collectionCompanion(tpe)}.newBuilder[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.result()")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val growArray =
            if (tpe1.typeArgs.nonEmpty && tpe1 <:< typeOf[AnyRef]) {
              q"""val x1 = new $tpe(i << 1)
                  System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else q"java.util.Arrays.copyOf(x, i << 1)"
          val shrinkArray =
            if (tpe1.typeArgs.nonEmpty && tpe1 <:< typeOf[AnyRef]) {
              q"""val x1 = new $tpe(i)
                  System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else q"java.util.Arrays.copyOf(x, i)"
          genReadArray(
            q"""var x = new $tpe(16)
                var i = 0""",
            q"""if (i == x.length) x = $growArray
                x(i) = ${genReadVal(tpe1, nullValue(tpe1), isStringified)}
                i += 1""",
            q"if (i == x.length) x else $shrinkArray")
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('"')) {
                in.rollbackToken()
                val l = in.readStringAsCharBuf()
                ${enumSymbol(tpe)}.values.iterator.find(e => in.isCharBufEqualsTo(l, e.toString))
                  .getOrElse(in.enumValueError(l))
              } else in.readNullOrTokenError(default, '"')"""
        } else if (tpe <:< typeOf[java.lang.Enum[_]]) withDecoderFor(methodKey, default) {
          val es = javaEnumValues(tpe)
          if (es.isEmpty) {
            q"""if (in.isNextToken('"')) {
                  in.rollbackToken()
                  val v = in.readString(null)
                  try ${companion(tpe)}.valueOf(v) catch {
                    case _: IllegalArgumentException => in.enumValueError(v)
                  }
                } else in.readNullOrTokenError(default, '"')"""
          } else {
            q"""if (in.isNextToken('"')) {
                  in.rollbackToken()
                  val l = in.readStringAsCharBuf()
                  ${genReadEnumValue(es, q"in.enumValueError(l)")}
                } else in.readNullOrTokenError(default, '"')"""
          }
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
        } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default) {
          val hashCode: Type => Int = t => {
            val cs = discriminatorValue(t).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }
          val length: Type => Int = t => discriminatorValue(t).length
          val leafClasses = adtLeafClasses(tpe)
          val discriminatorError = codecConfig.discriminatorFieldName
            .fold(q"in.discriminatorError()")(n => q"in.discriminatorValueError($n)")

          def genReadLeafClass(subTpe: Type): Tree =
            if (subTpe != tpe) genReadVal(subTpe, nullValue(subTpe), isStringified, skipDiscriminatorField)
            else genReadNonAbstractScalaClass(tpe, default, isStringified, skipDiscriminatorField)

          def genReadCollisions(subTpes: collection.Seq[Type]): Tree =
            subTpes.foldRight(discriminatorError) { case (subTpe, acc) =>
              val readVal =
                if (codecConfig.discriminatorFieldName.isDefined) {
                  q"""in.rollbackToMark()
                      ..${genReadLeafClass(subTpe)}"""
                } else if (subTpe.typeSymbol.isModuleClass) q"${subTpe.typeSymbol.asClass.module}"
                else genReadLeafClass(subTpe)
              q"if (in.isCharBufEqualsTo(l, ${discriminatorValue(subTpe)})) $readVal else $acc"
            }

          def genReadSubclassesBlock(leafClasses: collection.Seq[Type]) =
            if (leafClasses.size <= 4 && leafClasses.size == leafClasses.map(length).distinct.size) {
              genReadCollisions(leafClasses)
            } else {
              val cases = groupByOrdered(leafClasses)(hashCode).map { case (hash, ts) =>
                val checkNameAndReadValue = genReadCollisions(ts)
                cq"$hash => $checkNameAndReadValue"
              }.toSeq
              q"""(in.charBufToHashCode(l): @switch) match {
                    case ..$cases
                    case _ => $discriminatorError
                  }"""
            }

          checkDiscriminatorValueCollisions(tpe, leafClasses.map(discriminatorValue))
          codecConfig.discriminatorFieldName match {
            case None =>
              val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(_.typeSymbol.isModuleClass)
              if (leafModuleClasses.nonEmpty && leafCaseClasses.nonEmpty) {
                q"""if (in.isNextToken('"')) {
                      in.rollbackToken()
                      val l = in.readStringAsCharBuf()
                      ${genReadSubclassesBlock(leafModuleClasses)}
                    } else if (in.isCurrentToken('{')) {
                      val l = in.readKeyAsCharBuf()
                      val r = ${genReadSubclassesBlock(leafCaseClasses)}
                      if (in.isNextToken('}')) r
                      else in.objectEndOrCommaError()
                    } else in.readNullOrError(default, "expected '\"' or '{' or null")"""
              } else if (leafCaseClasses.nonEmpty) {
                q"""if (in.isNextToken('{')) {
                      val l = in.readKeyAsCharBuf()
                      val r = ${genReadSubclassesBlock(leafCaseClasses)}
                      if (in.isNextToken('}')) r
                      else in.objectEndOrCommaError()
                    } else in.readNullOrTokenError(default, '{')"""
              } else {
                q"""if (in.isNextToken('"')) {
                      in.rollbackToken()
                      val l = in.readStringAsCharBuf()
                      ${genReadSubclassesBlock(leafModuleClasses)}
                    } else in.readNullOrTokenError(default, '"')"""
              }
            case Some(discrFieldName) =>
              q"""in.setMark()
                  if (in.isNextToken('{')) {
                    if (in.skipToKey($discrFieldName)) {
                      val l = in.readStringAsCharBuf()
                      ..${genReadSubclassesBlock(leafClasses)}
                    } else in.requiredFieldError($discrFieldName)
                  } else in.readNullOrTokenError(default, '{')"""
          }
        } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default) {
          genReadNonAbstractScalaClass(tpe, default, isStringified, discriminator)
        } else cannotFindCodecError(tpe)
      }

      def genWriteNonAbstractScalaClass(tpe: Type, isStringified: Boolean, discriminator: Tree): Tree = {
        val classInfo = getClassInfo(tpe)
        val writeFields = classInfo.fields.map { f =>
          (if (codecConfig.transientDefault) f.defaultValue
          else None) match {
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
      }

      def genWriteVal(m: Tree, tpe: Type, isStringified: Boolean, discriminator: Tree = EmptyTree,
                      isRoot: Boolean = false): Tree = {
        val implCodec =
          if (isRoot) EmptyTree
          else findImplicitValueCodec(tpe)
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
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
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
        } else if (tpe <:< typeOf[collection.Map[_, _]]) withEncoderFor(methodKey, m) {
          genWriteMap(q"x", genWriteKey(q"kv._1", typeArg1(tpe)), genWriteVal(q"kv._2", typeArg2(tpe), isStringified))
        } else if (tpe <:< typeOf[BitSet]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x",
            if (isStringified) q"out.writeValAsString(x)"
            else q"out.writeVal(x)")
        } else if (tpe <:< typeOf[List[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              var l = x
              while (!l.isEmpty) {
                ..${genWriteVal(q"l.head", typeArg1(tpe), isStringified)}
                l = l.tail
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[IndexedSeq[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.size
              var i = 0
              while (i < l) {
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
                ..${genWriteVal(q"x(i)", typeArg1(tpe), isStringified)}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(methodKey, m) {
          q"out.writeVal(x.toString)"
        } else if (tpe <:< typeOf[java.lang.Enum[_]]) withEncoderFor(methodKey, m) {
          val es = javaEnumValues(tpe)
          if (es.isEmpty || es.exists(x => isEncodingRequired(x.name))) q"out.writeVal(x.name)"
          else q"out.writeNonEscapedAsciiVal(x.name)"
        } else if (tpe.typeSymbol.isModuleClass) withEncoderFor(methodKey, m) {
          q"""out.writeObjectStart()
              ..$discriminator
              out.writeObjectEnd()"""
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m) {
          val writeFields = tpe.typeArgs.zipWithIndex.map { case (ta, i) =>
            genWriteVal(q"x.${TermName("_" + (i + 1))}", ta.dealias, isStringified)
          }
          q"""out.writeArrayStart()
              ..$writeFields
              out.writeArrayEnd()"""
        } else if (isSealedClass(tpe)) withEncoderFor(methodKey, m) {
          def genWriteLeafClass(subTpe: Type, discriminator: Tree): Tree =
            if (subTpe != tpe) genWriteVal(q"x", subTpe, isStringified, discriminator)
            else genWriteNonAbstractScalaClass(tpe, isStringified, discriminator)

          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = (codecConfig.discriminatorFieldName match {
            case None =>
              val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(_.typeSymbol.isModuleClass)
              leafCaseClasses.map { subTpe =>
                cq"""x: $subTpe =>
                       out.writeObjectStart()
                       out.writeKey(${discriminatorValue(subTpe)})
                       ${genWriteLeafClass(subTpe, EmptyTree)}
                       out.writeObjectEnd()"""
              } ++ leafModuleClasses.map(subTpe => cq"x: $subTpe => ${genWriteConstantVal(discriminatorValue(subTpe))}")
            case Some(discrFieldName) =>
              leafClasses.map { subTpe =>
                val writeDiscriminatorField =
                  q"""..${genWriteConstantKey(discrFieldName)}
                      ..${genWriteConstantVal(discriminatorValue(subTpe))}"""
                cq"x: $subTpe => ${genWriteLeafClass(subTpe, writeDiscriminatorField)}"
              }
          }) :+ cq"null => out.writeNull()"
          q"""x match {
                case ..$writeSubclasses
              }"""
        } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m) {
          genWriteNonAbstractScalaClass(tpe, isStringified, discriminator)
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
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
              ..${fieldTrees.values}
              ..${equalsMethodTrees.values}
              ..${nullValueTrees.values}
              ..${mathContextTrees.values}
            }"""
      if (c.settings.contains("print-codecs")) {
        c.info(c.enclosingPosition, s"Generated JSON codec for type '$rootTpe':\n${showCode(codec)}", force = true)
      }
      c.Expr[JsonValueCodec[A]](codec)
    }
  }

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