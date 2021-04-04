package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}

import scala.annotation.{StaticAnnotation, tailrec}
import scala.annotation.meta.field
import scala.collection.{BitSet, immutable, mutable}
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros
import scala.reflect.NameTransformer
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
  * Examples of `fieldNameMapper`, `javaEnumValueNameMapper`, and `adtLeafClassNameMapper` functions that have no
  * dependencies in the same compilation module are: `JsonCodecMaker.enforceCamelCase`, `JsonCodecMaker.enforce_snake_case`,
  * `JsonCodecMaker.enforce-kebab-case`, and `JsonCodecMaker.simpleClassName`. Or their composition like:
  * `s => JsonCodecMaker.enforce_snake_case(JsonCodecMaker.simpleClassName(s))`
  *
  * @param fieldNameMapper        the partial function of mapping from string of case class field name to JSON key
  *                               (an identity function by default)
  * @param javaEnumValueNameMapper the partial function of mapping from string of Java enum name to JSON key
  *                               (an identity function by default)
  * @param adtLeafClassNameMapper the function of mapping from string of case class/object full name to string value of
  *                               discriminator field (a function that truncate to simple class name by default)
  * @param discriminatorFieldName an optional name of discriminator field, where None can be used for alternative
  *                               representation of ADTs without the discriminator field (Some("type") by default)
  * @param isStringified          a flag that turns on stringification of number or boolean values of collections,
  *                               options and value classes (turned off by default)
  * @param mapAsArray             a flag that turns on serialization and parsing of maps as a JSON array (or sequences
  *                               of tuples) instead of a JSON object, that allow to use 'JsonValueCodec' for encoding
  *                               and decoding of keys (turned off by default)
  * @param skipUnexpectedFields   a flag that turns on skipping of unexpected fields or in other case a parse exception
  *                               will be thrown (turned on by default)
  * @param transientDefault       a flag that turns on skipping serialization of fields that have same values as
  *                               default values defined for them in the primary constructor (turned on by default)
  * @param transientEmpty         a flag that turns on skipping serialization of fields that have empty values of
  *                               arrays or collections (turned on by default)
  * @param transientNone          a flag that turns on skipping serialization of fields that have empty values of
  *                               options (turned on by default)
  * @param bigDecimalPrecision    a precision in 'BigDecimal' values (34 by default)
  * @param bigDecimalScaleLimit   an exclusive limit for accepted scale in 'BigDecimal' values (6178 by default)
  * @param bigDecimalDigitsLimit  an exclusive limit for accepted number of mantissa digits of to be parsed before
  *                               rounding with the precision specified for 'BigDecimal' values (308 by default)
  * @param bigIntDigitsLimit      an exclusive limit for accepted number of decimal digits in 'BigInt' values
  *                               (308 by default)
  * @param bitSetValueLimit       an exclusive limit for accepted numeric values in bit sets (1024 by default)
  * @param mapMaxInsertNumber     a max number of inserts into maps (1024 by default)
  * @param setMaxInsertNumber     a max number of inserts into sets excluding bit sets (1024 by default)
  * @param allowRecursiveTypes    a flag that turns on support of recursive types (turned off by default)
  * @param requireDiscriminatorFirst a flag that turns off limitation for a position of the discriminator field to be
  *                               the first field of the JSON object (turned on by default)
  * @param useScalaEnumValueId    a flag that turns on using of ids for parsing and serialization of Scala enumeration
 *                                values
  */
class CodecMakerConfig private (
    val fieldNameMapper: PartialFunction[String, String],
    val javaEnumValueNameMapper: PartialFunction[String, String],
    val adtLeafClassNameMapper: String => String,
    val discriminatorFieldName: Option[String],
    val isStringified: Boolean,
    val mapAsArray: Boolean,
    val skipUnexpectedFields: Boolean,
    val transientDefault: Boolean,
    val transientEmpty: Boolean,
    val transientNone: Boolean,
    val requireCollectionFields: Boolean,
    val bigDecimalPrecision: Int,
    val bigDecimalScaleLimit: Int,
    val bigDecimalDigitsLimit: Int,
    val bigIntDigitsLimit: Int,
    val bitSetValueLimit: Int,
    val mapMaxInsertNumber: Int,
    val setMaxInsertNumber: Int,
    val allowRecursiveTypes: Boolean,
    val requireDiscriminatorFirst: Boolean,
    val useScalaEnumValueId: Boolean) {
  def withFieldNameMapper(fieldNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(fieldNameMapper = fieldNameMapper)

  def withJavaEnumValueNameMapper(javaEnumValueNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(javaEnumValueNameMapper = javaEnumValueNameMapper)

  def withAdtLeafClassNameMapper(adtLeafClassNameMapper: String => String): CodecMakerConfig =
    copy(adtLeafClassNameMapper = adtLeafClassNameMapper)

  def withDiscriminatorFieldName(discriminatorFieldName: Option[String]): CodecMakerConfig =
    copy(discriminatorFieldName = discriminatorFieldName)

  def withIsStringified(isStringified: Boolean): CodecMakerConfig = copy(isStringified = isStringified)

  def withMapAsArray(mapAsArray: Boolean): CodecMakerConfig = copy(mapAsArray = mapAsArray)

  def withSkipUnexpectedFields(skipUnexpectedFields: Boolean): CodecMakerConfig =
    copy(skipUnexpectedFields = skipUnexpectedFields)

  def withTransientDefault(transientDefault: Boolean): CodecMakerConfig = copy(transientDefault = transientDefault)

  def withTransientEmpty(transientEmpty: Boolean): CodecMakerConfig = copy(transientEmpty = transientEmpty)

  def withTransientNone(transientNone: Boolean): CodecMakerConfig = copy(transientNone = transientNone)

  def withRequireCollectionFields(requireCollectionFields: Boolean): CodecMakerConfig =
    copy(requireCollectionFields = requireCollectionFields)

  def withBigDecimalPrecision(bigDecimalPrecision: Int): CodecMakerConfig =
    copy(bigDecimalPrecision = bigDecimalPrecision)

  def withBigDecimalScaleLimit(bigDecimalScaleLimit: Int): CodecMakerConfig =
    copy(bigDecimalScaleLimit = bigDecimalScaleLimit)

  def withBigDecimalDigitsLimit(bigDecimalDigitsLimit: Int): CodecMakerConfig =
    copy(bigDecimalDigitsLimit = bigDecimalDigitsLimit)

  def withBigIntDigitsLimit(bigIntDigitsLimit: Int): CodecMakerConfig = copy(bigIntDigitsLimit = bigIntDigitsLimit)

  def withBitSetValueLimit(bitSetValueLimit: Int): CodecMakerConfig = copy(bitSetValueLimit = bitSetValueLimit)

  def withMapMaxInsertNumber(mapMaxInsertNumber: Int): CodecMakerConfig = copy(mapMaxInsertNumber = mapMaxInsertNumber)

  def withSetMaxInsertNumber(setMaxInsertNumber: Int): CodecMakerConfig = copy(setMaxInsertNumber = setMaxInsertNumber)

  def withAllowRecursiveTypes(allowRecursiveTypes: Boolean): CodecMakerConfig =
    copy(allowRecursiveTypes = allowRecursiveTypes)

  def withRequireDiscriminatorFirst(requireDiscriminatorFirst: Boolean): CodecMakerConfig =
    copy(requireDiscriminatorFirst = requireDiscriminatorFirst)

  def withUseScalaEnumValueId(useScalaEnumValueId: Boolean): CodecMakerConfig =
    copy(useScalaEnumValueId = useScalaEnumValueId)

  private[this] def copy(fieldNameMapper: PartialFunction[String, String] = fieldNameMapper,
                         javaEnumValueNameMapper: PartialFunction[String, String] = javaEnumValueNameMapper,
                         adtLeafClassNameMapper: String => String = adtLeafClassNameMapper,
                         discriminatorFieldName: Option[String] = discriminatorFieldName,
                         isStringified: Boolean = isStringified,
                         mapAsArray: Boolean = mapAsArray,
                         skipUnexpectedFields: Boolean = skipUnexpectedFields,
                         transientDefault: Boolean = transientDefault,
                         transientEmpty: Boolean = transientEmpty,
                         transientNone: Boolean = transientNone,
                         requireCollectionFields: Boolean = requireCollectionFields,
                         bigDecimalPrecision: Int = bigDecimalPrecision,
                         bigDecimalScaleLimit: Int = bigDecimalScaleLimit,
                         bigDecimalDigitsLimit: Int = bigDecimalDigitsLimit,
                         bigIntDigitsLimit: Int = bigIntDigitsLimit,
                         bitSetValueLimit: Int = bitSetValueLimit,
                         mapMaxInsertNumber: Int = mapMaxInsertNumber,
                         setMaxInsertNumber: Int = setMaxInsertNumber,
                         allowRecursiveTypes: Boolean = allowRecursiveTypes,
                         requireDiscriminatorFirst: Boolean = requireDiscriminatorFirst,
                         useScalaEnumValueId: Boolean = useScalaEnumValueId): CodecMakerConfig =
    new CodecMakerConfig(
      fieldNameMapper = fieldNameMapper,
      javaEnumValueNameMapper = javaEnumValueNameMapper,
      adtLeafClassNameMapper = adtLeafClassNameMapper,
      discriminatorFieldName = discriminatorFieldName,
      isStringified = isStringified,
      mapAsArray= mapAsArray,
      skipUnexpectedFields = skipUnexpectedFields,
      transientDefault = transientDefault,
      transientEmpty = transientEmpty,
      transientNone = transientNone,
      requireCollectionFields = requireCollectionFields,
      bigDecimalPrecision = bigDecimalPrecision,
      bigDecimalScaleLimit = bigDecimalScaleLimit,
      bigDecimalDigitsLimit = bigDecimalDigitsLimit,
      bigIntDigitsLimit = bigIntDigitsLimit,
      bitSetValueLimit = bitSetValueLimit,
      mapMaxInsertNumber = mapMaxInsertNumber,
      setMaxInsertNumber = setMaxInsertNumber,
      allowRecursiveTypes = allowRecursiveTypes,
      requireDiscriminatorFirst = requireDiscriminatorFirst,
      useScalaEnumValueId = useScalaEnumValueId)
}

object CodecMakerConfig extends CodecMakerConfig(
  fieldNameMapper = JsonCodecMaker.partialIdentity,
  javaEnumValueNameMapper = JsonCodecMaker.partialIdentity,
  adtLeafClassNameMapper = JsonCodecMaker.simpleClassName,
  discriminatorFieldName = Some("type"),
  isStringified = false,
  mapAsArray = false,
  skipUnexpectedFields = true,
  transientDefault = true,
  transientEmpty = true,
  transientNone = true,
  requireCollectionFields = false,
  bigDecimalPrecision = 34, // precision for decimal128: java.math.MathContext.DECIMAL128.getPrecision
  bigDecimalScaleLimit = 6178, // limit for scale for decimal128: BigDecimal("0." + "0" * 33 + "1e-6143", java.math.MathContext.DECIMAL128).scale + 1
  bigDecimalDigitsLimit = 308, // 128 bytes: (BigDecimal(BigInt("9" * 307))).underlying.unscaledValue.toByteArray.length
  bigIntDigitsLimit = 308, // 128 bytes: (BigInt("9" * 307)).underlying.toByteArray.length
  bitSetValueLimit = 1024, // 128 bytes: collection.mutable.BitSet(1023).toBitMask.length * 8
  mapMaxInsertNumber = 1024, // to limit attacks from untrusted input that exploit worst complexity for inserts
  setMaxInsertNumber = 1024, // to limit attacks from untrusted input that exploit worst complexity for inserts
  allowRecursiveTypes = false, // to avoid stack overflow errors with untrusted input
  requireDiscriminatorFirst = true, // to avoid CPU overuse when the discriminator appears in the end of JSON objects, especially nested
  useScalaEnumValueId = false)

object JsonCodecMaker {
  /**
    * A partial function that is a total in fact and always returns a string passed to it.
    *
    * @return a provided value
    */
  val partialIdentity: PartialFunction[String, String] = { case s => s }

  /**
    * Mapping function for field or class names that should be in camelCase format.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val enforceCamelCase: PartialFunction[String, String] = { case s => enforceCamelOrPascalCase(s, toPascal = false) }

  /**
    * Mapping function for field or class names that should be in PascalCase format.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val EnforcePascalCase: PartialFunction[String, String] = { case s => enforceCamelOrPascalCase(s, toPascal = true) }

  /**
    * Mapping function for field or class names that should be in snake_case format.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val enforce_snake_case: PartialFunction[String, String] = { case s => enforceSnakeOrKebabCase(s, '_') }

  /**
    * Mapping function for field or class names that should be in kebab-case format.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val `enforce-kebab-case`: PartialFunction[String, String] = { case s => enforceSnakeOrKebabCase(s, '-') }

  private[this] def enforceCamelOrPascalCase(s: String, toPascal: Boolean): String =
    if (s.indexOf('_') == -1 && s.indexOf('-') == -1) {
      if (s.length == 0) s
      else {
        val ch = s.charAt(0)
        val fixedCh =
          if (toPascal) toUpperCase(ch)
          else toLowerCase(ch)
        s"$fixedCh${s.substring(1)}"
      }
    } else {
      val len = s.length
      val sb = new StringBuilder(len)
      var i = 0
      var isPrecedingDash = toPascal
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

  /**
    * Derives a codec for JSON values for the specified type `A`.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def make[A]: JsonValueCodec[A] = macro Impl.makeWithDefaultConfig[A]

  /**
   * A replacement for the `make` call with the `CodecMakerConfig.withDiscriminatorFieldName(None)` configuration
   * parameter.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  def makeWithoutDiscriminator[A]: JsonValueCodec[A] = macro Impl.makeWithoutDiscriminator[A]

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = macro Impl.makeWithRequiredCollectionFields[A]

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name"))`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] = macro Impl.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]

  /**
    * Derives a codec for JSON values for the specified type `A` and a provided derivation configuration.
    *
    * @param config a derivation configuration
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def make[A](config: CodecMakerConfig): JsonValueCodec[A] = macro Impl.makeWithSpecifiedConfig[A]

  private object Impl {
    def makeWithDefaultConfig[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig)

    def makeWithoutDiscriminator[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withDiscriminatorFieldName(None))

    def makeWithRequiredCollectionFields[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true))

    def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name")))

    def makeWithSpecifiedConfig[A: c.WeakTypeTag](c: blackbox.Context)(config: c.Expr[CodecMakerConfig]): c.Expr[JsonValueCodec[A]] =
      make(c) {
        import c.universe._

        def fail(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

        val cfg =
          try c.eval(c.Expr[CodecMakerConfig](c.untypecheck(config.tree.duplicate))) catch { case ex: Throwable =>
            fail(s"Cannot evaluate a parameter of the 'make' macro call for type '${weakTypeOf[A].dealias}'. " +
              "It should not depend on code from the same compilation module where the 'make' macro is called. " +
              "Use a separated submodule of the project to compile all such dependencies before their usage for " +
              s"generation of codecs. Cause:\n$ex")
          }
        if (cfg.requireCollectionFields && cfg.transientEmpty) {
          fail("'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously")
        }
        cfg
      }

    private[this] def make[A: c.WeakTypeTag](c: blackbox.Context)(cfg: CodecMakerConfig): c.Expr[JsonValueCodec[A]] = {
      import c.universe._
      import c.internal._

      def fail(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

      def warn(msg: String): Unit = c.warning(c.enclosingPosition, msg)

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs(1).dealias

      val tupleSymbols: Set[Symbol] = definitions.TupleClass.seq.toSet

      def isTuple(tpe: Type): Boolean = tupleSymbols(tpe.typeSymbol)

      def isValueClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueMethod(tpe: Type): MethodSymbol = tpe.decls.head.asMethod

      def decodeName(s: Symbol): String = NameTransformer.decode(s.name.toString)

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
        tpe.typeSymbol.isClass && !tpe.typeSymbol.isAbstract && !tpe.typeSymbol.isJava

      def isSealedClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isSealed

      def isConstType(tpe: Type): Boolean = tpe match {
        case ConstantType(Constant(_)) => true
        case _ => false
      }

      def adtLeafClasses(adtBaseTpe: Type): Seq[Type] = {
        def collectRecursively(tpe: Type): Seq[Type] =
          if (tpe.typeSymbol.isClass) {
            val leafTpes = tpe.typeSymbol.asClass.knownDirectSubclasses.toSeq.flatMap { s =>
              val classSymbol = s.asClass
              val subTpe =
                if (classSymbol.typeParams.isEmpty) classSymbol.toType
                else substituteTypes(classSymbol.toType, classSymbol.typeParams, tpe.typeArgs)
              if (isSealedClass(subTpe)) collectRecursively(subTpe)
              else if (isNonAbstractScalaClass(subTpe)) Seq(subTpe)
              else fail(if (s.isAbstract) {
                "Only sealed intermediate traits or abstract classes are supported. Please consider using of them " +
                  s"for ADT with base '$adtBaseTpe' or provide a custom implicitly accessible codec for the ADT base."
              } else {
                "Only Scala classes & objects are supported for ADT leaf classes. Please consider using of them " +
                  s"for ADT with base '$adtBaseTpe' or provide a custom implicitly accessible codec for the ADT base."
              })
            }
            if (isNonAbstractScalaClass(tpe)) leafTpes :+ tpe
            else leafTpes
          } else Seq.empty

        val classes = collectRecursively(adtBaseTpe).distinct
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$adtBaseTpe'. " +
          "Please add them or provide a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def companion(tpe: Type): Symbol = {
        val comp = tpe.typeSymbol.companion
        if (comp.isModule) comp
        else {
          // Borrowed from Magnolia: https://github.com/propensive/magnolia/blob/f21f2aabb49e43b372240e98ec77981662cc570c/core/shared/src/main/scala/magnolia.scala#L123-L155
          val ownerChainOf: Symbol => Iterator[Symbol] =
            s => Iterator.iterate(s)(_.owner).takeWhile(x => x != null && x != NoSymbol).toVector.reverseIterator
          val path = ownerChainOf(tpe.typeSymbol)
            .zipAll(ownerChainOf(enclosingOwner), NoSymbol, NoSymbol)
            .dropWhile { case (x, y) => x == y }
            .takeWhile(_._1 != NoSymbol)
            .map(_._1.name.toTermName)
          if (path.isEmpty) fail(s"Cannot find a companion for $tpe")
          else c.typecheck(path.foldLeft[Tree](Ident(path.next()))(Select(_, _)), silent = true).symbol
        }
      }

      def isOption(tpe: Type): Boolean = tpe <:< typeOf[Option[_]]

      def isCollection(tpe: Type): Boolean = tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Array[_]]

      def scalaCollectionCompanion(tpe: Type): Tree =
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ident(tpe.typeSymbol.companion)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")

      def enumSymbol(tpe: Type): Symbol = tpe match {
        case TypeRef(SingleType(_, enumSymbol), _, _) => enumSymbol
      }

      val isScala213: Boolean = util.Properties.versionNumberString.startsWith("2.13.")
      val rootTpe = weakTypeOf[A].dealias
      val inferredKeyCodecs: mutable.Map[Type, Tree] = mutable.Map.empty
      val inferredValueCodecs: mutable.Map[Type, Tree] = mutable.Map.empty

      def findImplicitCodec(types: List[Type], isValueCodec: Boolean): Tree = {
        def inferImplicitValue(typeTree: Tree): Tree = c.inferImplicitValue(c.typecheck(typeTree, c.TYPEmode).tpe)

        val tpe :: nestedTypes = types
        if (nestedTypes.isEmpty) EmptyTree
        else {
          val recursiveIdx =
            if (cfg.allowRecursiveTypes) -1
            else nestedTypes.indexOf(tpe)
          if (recursiveIdx < 0) {
            if (tpe == rootTpe) EmptyTree
            else if (isValueCodec) {
              inferredValueCodecs.getOrElseUpdate(tpe,
                inferImplicitValue(tq"_root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$tpe]"))
            } else {
              inferredKeyCodecs.getOrElseUpdate(tpe,
                inferImplicitValue(tq"_root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[$tpe]"))
            }
          } else {
            fail(s"Recursive type(s) detected: ${nestedTypes.take(recursiveIdx + 1).reverse.mkString("'", "', '", "'")}. " +
              "Please consider using a custom implicitly accessible codec for this type to control the level of " +
              s"recursion or turn on the '${typeOf[CodecMakerConfig]}.allowRecursiveTypes' for the trusted input that " +
              "will not exceed the thread stack size.")
          }
        }
      }

      val mathContextNames = mutable.LinkedHashMap.empty[Int, TermName]
      val mathContextTrees = mutable.LinkedHashMap.empty[Int, Tree]

      def withMathContextFor(precision: Int): Tree =
        if (precision == java.math.MathContext.DECIMAL128.getPrecision) q"_root_.java.math.MathContext.DECIMAL128"
        else if (precision == java.math.MathContext.DECIMAL64.getPrecision) q"_root_.java.math.MathContext.DECIMAL64"
        else if (precision == java.math.MathContext.DECIMAL32.getPrecision) q"_root_.java.math.MathContext.DECIMAL32"
        else if (precision == java.math.MathContext.UNLIMITED.getPrecision) q"_root_.java.math.MathContext.UNLIMITED"
        else {
          val mc = q"new _root_.java.math.MathContext(${cfg.bigDecimalPrecision}, _root_.java.math.RoundingMode.HALF_EVEN)"
          val mathContextName = mathContextNames.getOrElseUpdate(precision, TermName("mc" + mathContextNames.size))
          mathContextTrees.getOrElseUpdate(precision, q"private[this] val $mathContextName: _root_.java.math.MathContext = $mc")
          Ident(mathContextName)
        }

      val scalaEnumCacheNames = mutable.LinkedHashMap.empty[Type, TermName]
      val scalaEnumCacheTries = mutable.LinkedHashMap.empty[Type, Tree]

      def withScalaEnumCacheFor(tpe: Type): Tree = {
        val keyTpe = if (cfg.useScalaEnumValueId) tq"Int" else tq"String"
        val ec = q"new _root_.java.util.concurrent.ConcurrentHashMap[$keyTpe, $tpe]"
        val enumCacheName = scalaEnumCacheNames.getOrElseUpdate(tpe, TermName("ec" + scalaEnumCacheNames.size))
        scalaEnumCacheTries.getOrElseUpdate(tpe,
          q"private[this] val $enumCacheName: _root_.java.util.concurrent.ConcurrentHashMap[$keyTpe, $tpe] = $ec")
        Ident(enumCacheName)
      }

      case class EnumValueInfo(value: Tree, name: String, transformed: Boolean)

      val enumValueInfos = mutable.LinkedHashMap.empty[Type, Seq[EnumValueInfo]]

      def isJavaEnum(tpe: Type): Boolean = tpe <:< typeOf[java.lang.Enum[_]]

      def javaEnumValues(tpe: Type): Seq[EnumValueInfo] = enumValueInfos.getOrElseUpdate(tpe, {
        val javaEnumValueNameMapper: String => String = n => cfg.javaEnumValueNameMapper.lift(n).getOrElse(n)
        var values = tpe.typeSymbol.asClass.knownDirectSubclasses.toSeq.map { s: Symbol =>
          val name = s.name.toString
          val transformedName = javaEnumValueNameMapper(name)
          EnumValueInfo(q"$s", transformedName, name != transformedName)
        }
        if (values.isEmpty) {
          val comp = companion(tpe)
          values =
            comp.typeSignature.members.collect { case m: MethodSymbol if m.isGetter && m.returnType.dealias =:= tpe =>
              val name = decodeName(m)
              val transformedName = javaEnumValueNameMapper(name)
              EnumValueInfo(q"$comp.${TermName(name)}", transformedName, name != transformedName)
            }.toSeq
        }
        val nameCollisions = duplicated(values.map(_.name))
        if (nameCollisions.nonEmpty) {
          val formattedCollisions = nameCollisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON value(s) defined for '$tpe': $formattedCollisions. Values are derived from value " +
            s"names of the enum that are mapped by the '${typeOf[CodecMakerConfig]}.javaEnumValueNameMapper' function. " +
            s"Result values should be unique per enum class.")
        }
        values
      })

      def genReadEnumValue(enumValues: Seq[EnumValueInfo], unexpectedEnumValueHandler: Tree): Tree = {
        val hashCode: EnumValueInfo => Int = e => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
        val length: EnumValueInfo => Int = _.name.length

        def genReadCollisions(es: collection.Seq[EnumValueInfo]): Tree =
          es.foldRight(unexpectedEnumValueHandler) { case (e, acc) =>
            q"if (in.isCharBufEqualsTo(l, ${e.name})) ${e.value} else $acc"
          }

        if (enumValues.size <= 8 && enumValues.map(length).sum <= 64) genReadCollisions(enumValues)
        else {
          val cases = groupByOrdered(enumValues)(hashCode).map { case (hash, fs) =>
            cq"$hash => ${genReadCollisions(fs)}"
          } :+ cq"_ => $unexpectedEnumValueHandler"
          q"""(in.charBufToHashCode(l): @_root_.scala.annotation.switch) match {
                case ..$cases
              }"""
        }
      }

      def genReadKey(types: List[Type]): Tree = {
        val tpe = types.head
        val implKeyCodec = findImplicitCodec(types, isValueCodec = false)
        if (!implKeyCodec.isEmpty) q"$implKeyCodec.decodeKey(in)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readKeyAsBoolean()"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"in.readKeyAsByte()"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readKeyAsChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"in.readKeyAsShort()"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"in.readKeyAsInt()"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"in.readKeyAsLong()"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"in.readKeyAsFloat()"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"in.readKeyAsDouble()"
        else if (isValueClass(tpe)) q"new $tpe(${genReadKey(valueClassValueType(tpe) :: types)})"
        else if (tpe =:= typeOf[String]) q"in.readKeyAsString()"
        else if (tpe =:= typeOf[BigInt]) q"in.readKeyAsBigInt(${cfg.bigIntDigitsLimit})"
        else if (tpe =:= typeOf[BigDecimal]) {
          val mc = withMathContextFor(cfg.bigDecimalPrecision)
          q"in.readKeyAsBigDecimal($mc, ${cfg.bigDecimalScaleLimit}, ${cfg.bigDecimalDigitsLimit})"
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
          val ec = withScalaEnumCacheFor(tpe)
          if (cfg.useScalaEnumValueId) {
            q"""val i = in.readKeyAsInt()
                var x = $ec.get(i)
                if (x eq null) {
                  x = ${enumSymbol(tpe)}.values.iterator.find(_.id == i).getOrElse(in.enumValueError(i.toString))
                  $ec.put(i, x)
                }
                x"""
          } else {
            q"""val s = in.readKeyAsString()
                var x = $ec.get(s)
                if (x eq null) {
                  x = ${enumSymbol(tpe)}.values.iterator.find(_.toString == s).getOrElse(in.enumValueError(s.length))
                  $ec.put(s, x)
                }
                x"""
          }
        } else if (isJavaEnum(tpe)) {
          q"""val l = in.readKeyAsCharBuf()
              ${genReadEnumValue(javaEnumValues(tpe), q"in.enumValueError(l)")}"""
        } else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(Constant(v: String)) =>
              q"""if (in.readKeyAsString() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Boolean)) =>
              q"""if (in.readKeyAsBoolean() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Byte)) =>
              q"""if (in.readKeyAsByte() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Char)) =>
              q"""if (in.readKeyAsChar() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Short)) =>
              q"""if (in.readKeyAsShort() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Int)) =>
              q"""if (in.readKeyAsInt() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Long)) =>
              q"""if (in.readKeyAsLong() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Float)) =>
              q"""if (in.readKeyAsFloat() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case ConstantType(Constant(v: Double)) =>
              q"""if (in.readKeyAsDouble() != $v) in.decodeError(${"expected key: \"" + v + '"'}); $v"""
            case _ => cannotFindKeyCodecError(tpe)
          }
        } else cannotFindKeyCodecError(tpe)
      }

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"x"): Tree =
        q"""if (in.isNextToken('[')) {
              if (in.isNextToken(']')) default
              else {
                in.rollbackToken()
                ..$newBuilder
                while ({
                  ..$readVal
                  in.isNextToken(',')
                }) ()
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
                while ({
                  ..$readVal
                  i += 1
                  if (i > ${cfg.setMaxInsertNumber}) in.decodeError("too many set inserts")
                  in.isNextToken(',')
                }) ()
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
                while ({
                  ..$readKV
                  i += 1
                  if (i > ${cfg.mapMaxInsertNumber}) in.decodeError("too many map inserts")
                  in.isNextToken(',')
                }) ()
                if (in.isCurrentToken('}')) $result
                else in.objectEndOrCommaError()
              }
            } else in.readNullOrTokenError(default, '{')"""

      def genReadMapAsArray(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        q"""if (in.isNextToken('[')) {
              if (in.isNextToken(']')) default
              else {
                in.rollbackToken()
                ..$newBuilder
                var i = 0
                while ({
                  if (in.isNextToken('[')) {
                    ..$readKV
                    i += 1
                    if (i > ${cfg.mapMaxInsertNumber}) in.decodeError("too many map inserts")
                    if (!in.isNextToken(']')) in.arrayEndError()
                  } else in.readNullOrTokenError(default, '[')
                  in.isNextToken(',')
                }) ()
                if (in.isCurrentToken(']')) $result
                else in.objectEndOrCommaError()
              }
            } else in.readNullOrTokenError(default, '[')"""

      @tailrec
      def genWriteKey(x: Tree, types: List[Type]): Tree = {
        val tpe = types.head
        val implKeyCodec = findImplicitCodec(types, isValueCodec = false)
        if (!implKeyCodec.isEmpty) q"$implKeyCodec.encodeKey($x, out)"
        else if (isValueClass(tpe)) genWriteKey(q"$x.${valueClassValueMethod(tpe)}", valueClassValueType(tpe) :: types)
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
        else if (tpe <:< typeOf[Enumeration#Value]) {
          if (cfg.useScalaEnumValueId) q"out.writeKey($x.id)"
          else q"out.writeKey($x.toString)"
        } else if (isJavaEnum(tpe)) {
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => cq"${e.value} => ${e.name}") :+
              cq"""_ => out.encodeError("illegal enum value: " + $x)"""
            if (encodingRequired) q"out.writeKey($x match { case ..$cases})"
            else q"out.writeNonEscapedAsciiKey($x match { case ..$cases})"
          } else {
            if (encodingRequired) q"out.writeKey($x.name)"
            else q"out.writeNonEscapedAsciiKey($x.name)"
          }
        } else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(Constant(_: String)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Boolean)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Byte)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Char)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Short)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Int)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Long)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Float)) => q"out.writeKey($x)"
            case ConstantType(Constant(_: Double)) => q"out.writeKey($x)"
            case _ => cannotFindKeyCodecError(tpe)
          }
        } else cannotFindKeyCodecError(tpe)
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

      def genWriteMapAsArray(x: Tree, writeKey: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            $x.foreach { kv =>
              out.writeArrayStart()
              ..$writeKey
              ..$writeVal
              out.writeArrayEnd()
            }
            out.writeArrayEnd()"""

      def genWriteMapScala213(x: Tree, writeKey: Tree, writeVal: Tree): Tree =
        q"""out.writeObjectStart()
            $x.foreachEntry { (k, v) =>
              ..$writeKey
              ..$writeVal
            }
            out.writeObjectEnd()"""

      def genWriteMapAsArrayScala213(x: Tree, writeKey: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            $x.foreachEntry { (k, v) =>
              out.writeArrayStart()
              ..$writeKey
              ..$writeVal
              out.writeArrayEnd()
            }
            out.writeArrayEnd()"""

      def cannotFindKeyCodecError(tpe: Type): Nothing =
        fail(s"No implicit '${typeOf[JsonKeyCodec[_]]}' defined for '$tpe'.")

      def cannotFindValueCodecError(tpe: Type): Nothing =
        fail(if (tpe.typeSymbol.isAbstract) {
          "Only sealed traits or abstract classes are supported as an ADT base. " +
            s"Please consider sealing the '$tpe' or provide a custom implicitly accessible codec for it."
        } else s"No implicit '${typeOf[JsonValueCodec[_]]}' defined for '$tpe'.")

      case class FieldInfo(symbol: TermSymbol, mappedName: String, tmpName: TermName, getter: MethodSymbol,
                           defaultValue: Option[Tree], resolvedTpe: Type, isStringified: Boolean)

      case class ClassInfo(tpe: Type, fields: Seq[FieldInfo])

      val classInfos = mutable.LinkedHashMap.empty[Type, ClassInfo]

      def getClassInfo(tpe: Type): ClassInfo = classInfos.getOrElseUpdate(tpe, {
        case class FieldAnnotations(partiallyMappedName: String, transient: Boolean, stringified: Boolean)

        def getPrimaryConstructor(tpe: Type): MethodSymbol = tpe.decls.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m // FIXME: sometime it cannot be accessed from the place of the `make` call
        }.getOrElse(fail(s"Cannot find a primary constructor for '$tpe'"))

        def hasSupportedAnnotation(m: TermSymbol): Boolean = {
          m.info // to enforce the type information completeness and availability of annotations
          m.annotations.exists(a => a.tree.tpe =:= typeOf[named] || a.tree.tpe =:= typeOf[transient] ||
            a.tree.tpe =:= typeOf[stringified])
        }

        lazy val module = companion(tpe).asModule // don't lookup for the companion when there are no default values for constructor params
        val getters = tpe.members.collect { case m: MethodSymbol if m.isParamAccessor && m.isGetter => m }
        val annotations = tpe.members.collect {
          case m: TermSymbol if hasSupportedAnnotation(m) =>
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
            val partiallyMappedName = named.headOption.map { a =>
              a.tree.children.tail.collectFirst { case Literal(Constant(s: String)) => s }.getOrElse {
                try c.eval(c.Expr[named](c.untypecheck(a.tree.duplicate))).name catch { case ex: Throwable =>
                  fail(s"Cannot evaluate a parameter of the '@named' annotation in type '$tpe'. " +
                    "It should not depend on code from the same compilation module where the 'make' macro is called. " +
                    "Use a separated submodule of the project to compile all such dependencies before their usage " +
                    s"for generation of codecs. Cause:\n$ex")
                }
              }
            }.getOrElse(name)
            (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        ClassInfo(tpe, getPrimaryConstructor(tpe).paramLists match {
          case params :: Nil => params.zipWithIndex.flatMap { case (p, i) =>
            val symbol = p.asTerm
            val name = decodeName(symbol)
            val annotationOption = annotations.get(name)
            if (annotationOption.exists(_.transient)) None
            else {
              val fieldNameMapper: String => String = n => cfg.fieldNameMapper.lift(n).getOrElse(n)
              val mappedName = annotationOption.fold(fieldNameMapper(name))(_.partiallyMappedName)
              val tmpName = TermName("_" + symbol.name)
              val getter = getters.find(_.name == symbol.name).getOrElse {
                fail(s"'$name' parameter of '$tpe' should be defined as 'val' or 'var' in the primary constructor.")
              }
              val defaultValue =
                if (symbol.isParamWithDefault) Some(q"$module.${TermName("$lessinit$greater$default$" + (i + 1))}")
                else None
              val isStringified = annotationOption.exists(_.stringified)
              Some(FieldInfo(symbol, mappedName, tmpName, getter, defaultValue, paramType(tpe, symbol), isStringified))
            }
          }
          case _ => fail(s"'$tpe' has a primary constructor with multiple parameter lists. " +
            "Please consider using a custom implicitly accessible codec for this type.")
        })
      })

      val unexpectedFieldHandler =
        if (cfg.skipUnexpectedFields) q"in.skip()"
        else q"in.unexpectedKeyError(l)"
      val skipDiscriminatorField =
        q"""if (pd) {
              pd = false
              in.skip()
            } else in.duplicatedKeyError(l)"""

      def discriminatorValue(tpe: Type): String =
        cfg.adtLeafClassNameMapper(NameTransformer.decode(tpe.typeSymbol.fullName))

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
          q"""private[this] def $fieldName(i: Int): String =
                (i: @_root_.scala.annotation.switch @_root_.scala.unchecked) match {
                  case ..${f.zipWithIndex.map { case (n, i) => cq"$i => $n"}}
                }""")
        Ident(fieldName)
      }

      val equalsMethodNames = mutable.LinkedHashMap.empty[Type, TermName]
      val equalsMethodTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withEqualsFor(tpe: Type, arg1: Tree, arg2: Tree)(f: => Tree): Tree = {
        val equalsMethodName = equalsMethodNames.getOrElseUpdate(tpe, TermName("q" + equalsMethodNames.size))
        equalsMethodTrees.getOrElseUpdate(tpe,
          q"private[this] def $equalsMethodName(x1: $tpe, x2: $tpe): _root_.scala.Boolean = $f")
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
        } else q"_root_.java.util.Arrays.equals(x1, x2)"
      }

      case class MethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree)

      val decodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val decodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withDecoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val decodeMethodName = decodeMethodNames.getOrElseUpdate(methodKey, TermName("d" + decodeMethodNames.size))
        decodeMethodTrees.getOrElseUpdate(methodKey,
          q"private[this] def $decodeMethodName(in: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonReader, default: ${methodKey.tpe}): ${methodKey.tpe} = $f")
        q"$decodeMethodName(in, $arg)"
      }

      val encodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val encodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withEncoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val encodeMethodName = encodeMethodNames.getOrElseUpdate(methodKey, TermName("e" + encodeMethodNames.size))
        encodeMethodTrees.getOrElseUpdate(methodKey,
          q"private[this] def $encodeMethodName(x: ${methodKey.tpe}, out: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter): _root_.scala.Unit = $f")
        q"$encodeMethodName($arg, out)"
      }

      def nullValue(types: List[Type]): Tree = {
        val tpe = types.head
        val implCodec = findImplicitCodec(types, isValueCodec = true)
        if (!implCodec.isEmpty) q"$implCodec.nullValue"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"false"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"(0: _root_.scala.Byte)"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"'\u0000'"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"(0: _root_.scala.Short)"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"0"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"0L"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"0f"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"0.0"
        else if (isOption(tpe)) q"_root_.scala.None"
        else if (tpe <:< typeOf[mutable.BitSet]) q"${scalaCollectionCompanion(tpe)}.empty"
        else if (tpe <:< typeOf[BitSet]) withNullValueFor(tpe)(q"${scalaCollectionCompanion(tpe)}.empty")
        else if (tpe <:< typeOf[mutable.LongMap[_]]) q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[::[_]]) q"null"
        else if (tpe <:< typeOf[List[_]] || tpe =:= typeOf[Seq[_]]) q"_root_.scala.Nil"
        else if (tpe <:< typeOf[immutable.IntMap[_]] || tpe <:< typeOf[immutable.LongMap[_]] ||
          tpe <:< typeOf[immutable.Seq[_]] || tpe <:< typeOf[Set[_]]) withNullValueFor(tpe) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[immutable.Map[_, _]]) withNullValueFor(tpe) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[collection.Map[_, _]]) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[Iterable[_]]) q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[Array[_]]) withNullValueFor(tpe)(q"new _root_.scala.Array[${typeArg1(tpe)}](0)")
        else if (tpe.typeSymbol.isModuleClass) q"${tpe.typeSymbol.asClass.module}"
        else if (tpe <:< typeOf[AnyRef]) q"null"
        else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(Constant(v: String)) => q"$v"
            case ConstantType(Constant(v: Boolean)) => q"$v"
            case ConstantType(Constant(v: Byte)) => q"$v"
            case ConstantType(Constant(v: Char)) => q"$v"
            case ConstantType(Constant(v: Short)) => q"$v"
            case ConstantType(Constant(v: Int)) => q"$v"
            case ConstantType(Constant(v: Long)) => q"$v"
            case ConstantType(Constant(v: Float)) => q"$v"
            case ConstantType(Constant(v: Double)) => q"$v"
            case _ => cannotFindValueCodecError(tpe)
          }
        } else q"null.asInstanceOf[$tpe]"
      }

      def genReadNonAbstractScalaClass(types: List[Type], discriminator: Tree): Tree = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        checkFieldNameCollisions(tpe, cfg.discriminatorFieldName.fold(Seq.empty[String]) { n =>
          val names = classInfo.fields.map(_.mappedName)
          if (discriminator.isEmpty) names
          else names :+ n
        })
        val required: Set[String] = classInfo.fields.collect {
          case f if !(f.symbol.isParamWithDefault || isOption(f.resolvedTpe) ||
            (isCollection(f.resolvedTpe) && !cfg.requireCollectionFields)) => f.mappedName
        }.toSet
        val paramVarNum = classInfo.fields.size
        val lastParamVarIndex = Math.max(0, (paramVarNum - 1) >> 5)
        val lastParamVarBits = -1 >>> -paramVarNum
        val paramVarNames = (0 to lastParamVarIndex).map(i => TermName("p" + i))
        val checkAndResetFieldPresenceFlags = classInfo.fields.zipWithIndex.map { case (f, i) =>
          val n = paramVarNames(i >> 5)
          val m = 1 << i
          (f.mappedName, q"if (($n & $m) != 0) $n ^= $m else in.duplicatedKeyError(l)")
        }.toMap
        val paramVars =
          paramVarNames.init.map(n => q"var $n = -1") :+ q"var ${paramVarNames.last} = $lastParamVarBits"
        val checkReqVars =
          if (required.isEmpty) Nil
          else {
            val names = withFieldsFor(tpe)(classInfo.fields.map(_.mappedName))
            val reqMasks = classInfo.fields.grouped(32).toSeq.map(_.zipWithIndex.foldLeft(0) { case (acc, (f, i)) =>
              if (required(f.mappedName)) acc | (1 << i)
              else acc
            })
            paramVarNames.zipWithIndex.map { case (n, i) =>
              val m = reqMasks(i)
              val fieldName =
                if (i == 0) q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n & $m))"
                else q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n & $m) + ${i << 5})"
              q"if (($n & $m) != 0) in.requiredFieldError($fieldName)"
            }
          }
        val construct = q"new $tpe(..${classInfo.fields.map(f => q"${f.symbol.name} = ${f.tmpName}")})"
        val readVars = classInfo.fields.map { f =>
          q"var ${f.tmpName}: ${f.resolvedTpe} = ${f.defaultValue.getOrElse(nullValue(f.resolvedTpe :: types))}"
        }
        val hashCode: FieldInfo => Int = f => JsonReader.toHashCode(f.mappedName.toCharArray, f.mappedName.length)
        val length: FieldInfo => Int = _.mappedName.length
        val readFields = cfg.discriminatorFieldName.fold(classInfo.fields) { n =>
          if (discriminator.isEmpty) classInfo.fields
          else classInfo.fields :+ FieldInfo(null, n, null, null, null, null, isStringified = true)
        }

        def genReadCollisions(fs: collection.Seq[FieldInfo]): Tree =
          fs.foldRight(unexpectedFieldHandler) { case (f, acc) =>
            val readValue =
              if (discriminator.nonEmpty && cfg.discriminatorFieldName.contains(f.mappedName)) discriminator
              else {
                q"""${checkAndResetFieldPresenceFlags(f.mappedName)}
                    ${f.tmpName} = ${genReadVal(f.resolvedTpe :: types, q"${f.tmpName}", f.isStringified, EmptyTree)}"""
              }
            q"if (in.isCharBufEqualsTo(l, ${f.mappedName})) $readValue else $acc"
          }

        val readFieldsBlock =
          if (readFields.size <= 8 && readFields.map(length).sum <= 64) genReadCollisions(readFields)
          else {
            val cases = groupByOrdered(readFields)(hashCode).map { case (hash, fs) =>
              cq"$hash => ${genReadCollisions(fs)}"
            } :+ cq"_ => $unexpectedFieldHandler"
            q"""(in.charBufToHashCode(l): @_root_.scala.annotation.switch) match {
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
                var l = -1
                while (l < 0 || in.isNextToken(',')) {
                  l = in.readKeyAsCharBuf()
                  ..$readFieldsBlock
                }
                if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
              }
              ..$checkReqVars
              $construct
            } else in.readNullOrTokenError(default, '{')"""
      }

      def genReadConstType(tpe: c.universe.Type, isStringified: Boolean): Tree = tpe match {
        case ConstantType(Constant(v: String)) =>
          q"""if (in.readString(null) != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
        case ConstantType(Constant(v: Boolean)) =>
          if (isStringified) q"""if (in.readStringAsBoolean() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readBoolean() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Byte)) =>
          if (isStringified) q"""if (in.readStringAsByte() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readByte() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Char)) =>
          q"""if (in.readChar() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
        case ConstantType(Constant(v: Short)) =>
          if (isStringified) q"""if (in.readStringAsShort() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readShort() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Int)) =>
          if (isStringified) q"""if (in.readStringAsInt() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readInt() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Long)) =>
          if (isStringified) q"""if (in.readStringAsLong() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readLong() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Float)) =>
          if (isStringified) q"""if (in.readStringAsFloat() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readFloat() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case ConstantType(Constant(v: Double)) =>
          if (isStringified) q"""if (in.readStringAsDouble() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readDouble() != $v) in.decodeError(${"expected value: " + v}); $v"""
        case _ => cannotFindValueCodecError(tpe)
      }

      def genReadValForGrowable(types: List[Type], isStringified: Boolean): Tree =
          if (isScala213) q"x.addOne(${genReadVal(types, nullValue(types), isStringified, EmptyTree)})"
          else q"x += ${genReadVal(types, nullValue(types), isStringified, EmptyTree)}"

      def genReadVal(types: List[Type], default: Tree, isStringified: Boolean, discriminator: Tree): Tree = {
        val tpe = types.head
        val implCodec = findImplicitCodec(types, isValueCodec = true)
        val methodKey = MethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe)), discriminator)
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
          if (isStringified) q"in.readStringAsBigInt($default, ${cfg.bigIntDigitsLimit})"
          else q"in.readBigInt($default, ${cfg.bigIntDigitsLimit})"
        } else if (tpe =:= typeOf[BigDecimal]) {
          val mc = withMathContextFor(cfg.bigDecimalPrecision)
          if (isStringified) {
            q"in.readStringAsBigDecimal($default, $mc, ${cfg.bigDecimalScaleLimit}, ${cfg.bigDecimalDigitsLimit})"
          } else {
            q"in.readBigDecimal($default, $mc, ${cfg.bigDecimalScaleLimit}, ${cfg.bigDecimalDigitsLimit})"
          }
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          q"new $tpe(${genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)})"
        } else if (isOption(tpe)) {
          val tpe1 = typeArg1(tpe)
          q"""if (in.isNextToken('n')) in.readNullOrError($default, "expected value or null")
              else {
                in.rollbackToken()
                new _root_.scala.Some(${genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)})
              }"""
        } else if (tpe <:< typeOf[immutable.IntMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val newBuilder = q"var x = ${withNullValueFor(tpe)(q"${scalaCollectionCompanion(tpe)}.empty[$tpe1]")}"
          val readVal = genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readKey =
              if (cfg.isStringified) q"in.readStringAsInt()"
              else q"in.readInt()"
            genReadMapAsArray(newBuilder,
              q"x = x.updated($readKey, { if (in.isNextToken(',')) $readVal else in.commaError() })")
          } else genReadMap(newBuilder, q"x = x.updated(in.readKeyAsInt(), $readVal)")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val newBuilder = q"val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1]"
          val readVal = genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readKey =
              if (cfg.isStringified) q"in.readStringAsLong()"
              else q"in.readLong()"
            genReadMapAsArray(newBuilder,
              q"x.update($readKey, { if (in.isNextToken(',')) $readVal else in.commaError() })")
          } else genReadMap(newBuilder, q"x.update(in.readKeyAsLong(), $readVal)")
        } else if (tpe <:< typeOf[immutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val newBuilder = q"var x = ${withNullValueFor(tpe)(q"${scalaCollectionCompanion(tpe)}.empty[$tpe1]")}"
          val readVal = genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readKey =
              if (cfg.isStringified) q"in.readStringAsLong()"
              else q"in.readLong()"
            genReadMapAsArray(newBuilder,
              q"x = x.updated($readKey, { if (in.isNextToken(',')) $readVal else in.commaError() })")
          } else genReadMap(newBuilder, q"x = x.updated(in.readKeyAsLong(), $readVal)")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val newBuilder = q"val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1, $tpe2]"
          val readVal2 = genReadVal(tpe2 :: types, nullValue(tpe2 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readVal1 = genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)
            genReadMapAsArray(newBuilder, q"x.update($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() })")
          } else genReadMap(newBuilder, q"x.update(${genReadKey(tpe1 :: types)}, $readVal2)")
        } else if (tpe <:< typeOf[collection.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val newBuilder = q"val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1, $tpe2]"
          val readVal2 = genReadVal(tpe2 :: types, nullValue(tpe2 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readVal1 = genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)
            val readKV =
              if (isScala213) q"x.addOne(($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() }))"
              else q"x += (($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() }))"
            genReadMapAsArray(newBuilder, readKV,q"x.result()")
          } else {
            val readKey = genReadKey(tpe1 :: types)
            val readKV =
              if (isScala213) q"x.addOne(($readKey, $readVal2))"
              else q"x += (($readKey, $readVal2))"
            genReadMap(newBuilder, readKV,q"x.result()")
          }
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(methodKey, default) {
          val readVal =
            if (isStringified) q"in.readStringAsInt()"
            else q"in.readInt()"
          genReadArray(q"var x = new _root_.scala.Array[Long](2)",
            q"""val v = $readVal
                if (v < 0 || v >= ${cfg.bitSetValueLimit}) in.decodeError("illegal value for bit set")
                val i = v >>> 6
                if (i >= x.length) x = _root_.java.util.Arrays.copyOf(x, _root_.java.lang.Integer.highestOneBit(i) << 1)
                x(i) |= 1L << v""",
            if (tpe =:= typeOf[BitSet]) q"_root_.scala.collection.immutable.BitSet.fromBitMaskNoCopy(x)"
            else q"${scalaCollectionCompanion(tpe)}.fromBitMaskNoCopy(x)")
        } else if (tpe <:< typeOf[mutable.Set[_] with mutable.Builder[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1]",
            genReadValForGrowable(tpe1 :: types, isStringified))
        } else if (tpe <:< typeOf[collection.Set[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1]",
            genReadValForGrowable(tpe1 :: types, isStringified), q"x.result()")
        } else if (tpe <:< typeOf[::[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val readVal = genReadValForGrowable(tpe1 :: types, isStringified)
          q"""if (in.isNextToken('[')) {
                if (in.isNextToken(']')) {
                  if (default ne null) default
                  else in.decodeError("expected non-empty JSON array")
                } else {
                  in.rollbackToken()
                  val x = new _root_.scala.collection.mutable.ListBuffer[$tpe1]
                  while ({
                    ..$readVal
                    in.isNextToken(',')
                  }) ()
                  if (in.isCurrentToken(']')) x.toList.asInstanceOf[_root_.scala.collection.immutable.::[$tpe1]]
                  else in.arrayEndOrCommaError()
                }
              } else {
                if (default ne null) in.readNullOrTokenError(default, '[')
                else in.decodeError("expected non-empty JSON array")
              }"""
        } else if (tpe <:< typeOf[List[_]] || tpe =:= typeOf[Seq[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = new _root_.scala.collection.mutable.ListBuffer[$tpe1]",
            genReadValForGrowable(tpe1 :: types, isStringified), q"x.toList")
        } else if (tpe <:< typeOf[mutable.Iterable[_] with mutable.Builder[_, _]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { //ArrayStack uses 'push' for '+=' in Scala 2.11.x/2.12.x
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1]",
            genReadValForGrowable(tpe1 :: types, isStringified))
        } else if (tpe <:< typeOf[Iterable[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1]",
            genReadValForGrowable(tpe1 :: types, isStringified), q"x.result()")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val growArray =
            if (tpe1.typeArgs.nonEmpty) {
              q"""val x1 = new $tpe(i << 1)
                  _root_.java.lang.System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else q"_root_.java.util.Arrays.copyOf(x, i << 1)"
          val shrinkArray =
            if (tpe1.typeArgs.nonEmpty) {
              q"""val x1 = new $tpe(i)
                  _root_.java.lang.System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else q"_root_.java.util.Arrays.copyOf(x, i)"
          genReadArray(
            q"""var x = new $tpe(16)
                var i = 0""",
            q"""if (i == x.length) x = $growArray
                x(i) = ${genReadVal(tpe1 :: types, nullValue(tpe1 :: types), isStringified, EmptyTree)}
                i += 1""",
            q"if (i == x.length) x else $shrinkArray")
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(methodKey, default) {
          val ec = withScalaEnumCacheFor(tpe)
          if (cfg.useScalaEnumValueId) {
            if (isStringified) {
              q"""if (in.isNextToken('"')) {
                    in.rollbackToken()
                    val i = in.readStringAsInt()
                    var x = $ec.get(i)
                    if (x eq null) {
                      x = ${enumSymbol(tpe)}.values.iterator.find(_.id == i).getOrElse(in.enumValueError(i.toString))
                      $ec.put(i, x)
                    }
                    x
                  } else in.readNullOrTokenError(default, '"')"""
            } else {
              q"""val t = in.nextToken()
                  if (t >= '0' && t <= '9') {
                    in.rollbackToken()
                    val i = in.readInt()
                    var x = $ec.get(i)
                    if (x eq null) {
                      x = ${enumSymbol(tpe)}.values.iterator.find(_.id == i).getOrElse(in.decodeError("illegal enum value " + i))
                      $ec.put(i, x)
                    }
                    x
                  } else in.readNullOrError(default, "expected digit")"""
            }
          } else {
            q"""if (in.isNextToken('"')) {
                  in.rollbackToken()
                  val s = in.readString(null)
                  var x = $ec.get(s)
                  if (x eq null) {
                    x = ${enumSymbol(tpe)}.values.iterator.find(_.toString == s).getOrElse(in.enumValueError(s.length))
                    $ec.put(s, x)
                  }
                  x
                } else in.readNullOrTokenError(default, '"')"""
          }
        } else if (isJavaEnum(tpe)) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('"')) {
                in.rollbackToken()
                val l = in.readStringAsCharBuf()
                ${genReadEnumValue(javaEnumValues(tpe), q"in.enumValueError(l)")}
              } else in.readNullOrTokenError(default, '"')"""
        } else if (tpe.typeSymbol.isModuleClass) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('{')) {
                in.rollbackToken()
                in.skip()
                ${tpe.typeSymbol.asClass.module}
              } else in.readNullOrTokenError(default, '{')"""
        } else if (isTuple(tpe)) withDecoderFor(methodKey, default) {
          val indexedTypes = tpe.typeArgs.zipWithIndex
          val readFields = indexedTypes.tail.foldLeft[Tree] {
            val t = typeArg1(tpe)
            q"val _1: $t = ${genReadVal(t :: types, nullValue(t :: types), isStringified, EmptyTree)}"
          }{ case (acc, (ta, i)) =>
            val t = ta.dealias
            q"""..$acc
                val ${TermName("_" + (i + 1))}: $t =
                  if (in.isNextToken(',')) ${genReadVal(t :: types, nullValue(t :: types), isStringified, EmptyTree)}
                  else in.commaError()"""
          }
          val params = indexedTypes.map { case (_, i) => TermName("_" + (i + 1)) }
          q"""if (in.isNextToken('[')) {
                ..$readFields
                if (in.isNextToken(']')) new $tpe(..$params)
                else in.arrayEndError()
              } else in.readNullOrTokenError(default, '[')"""
        } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default) {
          val hashCode: Type => Int = t => {
            val cs = discriminatorValue(t).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }
          val length: Type => Int = t => discriminatorValue(t).length
          val leafClasses = adtLeafClasses(tpe)
          val discriminatorError = cfg.discriminatorFieldName
            .fold(q"in.discriminatorError()")(n => q"in.discriminatorValueError($n)")

          def genReadLeafClass(subTpe: Type): Tree =
            if (subTpe == tpe) genReadNonAbstractScalaClass(types, skipDiscriminatorField)
            else genReadVal(subTpe :: types, nullValue(subTpe :: types), isStringified, skipDiscriminatorField)

          def genReadCollisions(subTpes: collection.Seq[Type]): Tree =
            subTpes.foldRight(discriminatorError) { case (subTpe, acc) =>
              val readVal =
                if (cfg.discriminatorFieldName.isDefined) {
                  q"""in.rollbackToMark()
                      ..${genReadLeafClass(subTpe)}"""
                } else if (subTpe.typeSymbol.isModuleClass) q"${subTpe.typeSymbol.asClass.module}"
                else genReadLeafClass(subTpe)
              q"if (in.isCharBufEqualsTo(l, ${discriminatorValue(subTpe)})) $readVal else $acc"
            }

          def genReadSubclassesBlock(leafClasses: collection.Seq[Type]): Tree =
            if (leafClasses.size <= 8 && leafClasses.map(length).sum <= 64) genReadCollisions(leafClasses)
            else {
              val cases = groupByOrdered(leafClasses)(hashCode).map { case (hash, ts) =>
                val checkNameAndReadValue = genReadCollisions(ts)
                cq"$hash => $checkNameAndReadValue"
              }
              q"""(in.charBufToHashCode(l): @_root_.scala.annotation.switch) match {
                    case ..$cases
                    case _ => $discriminatorError
                  }"""
            }

          checkDiscriminatorValueCollisions(tpe, leafClasses.map(discriminatorValue))
          cfg.discriminatorFieldName match {
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
              if (cfg.requireDiscriminatorFirst) {
                q"""in.setMark()
                    if (in.isNextToken('{')) {
                      if ($discrFieldName.equals(in.readKeyAsString())) {
                        val l = in.readStringAsCharBuf()
                        ..${genReadSubclassesBlock(leafClasses)}
                      } else in.decodeError("expected key: \"" + $discrFieldName + '"')
                    } else in.readNullOrTokenError(default, '{')"""
              } else {
                q"""in.setMark()
                    if (in.isNextToken('{')) {
                      if (in.skipToKey($discrFieldName)) {
                        val l = in.readStringAsCharBuf()
                        ..${genReadSubclassesBlock(leafClasses)}
                      } else in.requiredFieldError($discrFieldName)
                    } else in.readNullOrTokenError(default, '{')"""
              }
          }
        } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default) {
          genReadNonAbstractScalaClass(types, discriminator)
        } else if (isConstType(tpe)) genReadConstType(tpe, isStringified)
        else cannotFindValueCodecError(tpe)
      }

      def genWriteNonAbstractScalaClass(types: List[Type], discriminator: Tree): Tree = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        val writeFields = classInfo.fields.map { f =>
          (if (cfg.transientDefault) f.defaultValue
          else None) match {
            case Some(d) =>
              if (f.resolvedTpe <:< typeOf[Iterable[_]] && cfg.transientEmpty) {
                q"""val v = x.${f.getter}
                    if (!v.isEmpty && v != $d) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v", f.resolvedTpe :: types, f.isStringified, EmptyTree)}
                    }"""
              } else if (isOption(f.resolvedTpe) && cfg.transientNone) {
                q"""val v = x.${f.getter}
                    if ((v ne _root_.scala.None) && v != $d) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v.get", typeArg1(f.resolvedTpe) :: types, f.isStringified, EmptyTree)}
                    }"""
              } else if (f.resolvedTpe <:< typeOf[Array[_]]) {
                val cond =
                  if (cfg.transientEmpty) {
                    q"v.length > 0 && !${withEqualsFor(f.resolvedTpe, q"v", d)(genArrayEquals(f.resolvedTpe))}"
                  } else q"!${withEqualsFor(f.resolvedTpe, q"v", d)(genArrayEquals(f.resolvedTpe))}"
                q"""val v = x.${f.getter}
                    if ($cond) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v", f.resolvedTpe :: types, f.isStringified, EmptyTree)}
                    }"""
              } else {
                q"""val v = x.${f.getter}
                    if (v != $d) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v", f.resolvedTpe :: types, f.isStringified, EmptyTree)}
                    }"""
              }
            case None =>
              if (f.resolvedTpe <:< typeOf[Iterable[_]] && cfg.transientEmpty) {
                q"""val v = x.${f.getter}
                    if (!v.isEmpty) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v", f.resolvedTpe :: types, f.isStringified, EmptyTree)}
                    }"""
              } else if (isOption(f.resolvedTpe) && cfg.transientNone) {
                q"""val v = x.${f.getter}
                    if (v ne _root_.scala.None) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v.get", typeArg1(f.resolvedTpe) :: types, f.isStringified, EmptyTree)}
                    }"""
              } else if (f.resolvedTpe <:< typeOf[Array[_]] && cfg.transientEmpty) {
                q"""val v = x.${f.getter}
                    if (v.length > 0) {
                      ..${genWriteConstantKey(f.mappedName)}
                      ..${genWriteVal(q"v", f.resolvedTpe :: types, f.isStringified, EmptyTree)}
                    }"""
              } else {
                q"""..${genWriteConstantKey(f.mappedName)}
                    ..${genWriteVal(q"x.${f.getter}", f.resolvedTpe :: types, f.isStringified, EmptyTree)}"""
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

      def getWriteConstType(tpe: c.universe.Type, m: c.universe.Tree, isStringified: Boolean): Tree = tpe match {
        case ConstantType(Constant(_: String)) => q"out.writeVal($m)"
        case ConstantType(Constant(_: Boolean)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Byte)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Char)) => q"out.writeVal($m)"
        case ConstantType(Constant(_: Short)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Int)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Long)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Float)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case ConstantType(Constant(_: Double)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case _ => cannotFindValueCodecError(tpe)
      }

      def genWriteVal(m: Tree, types: List[Type], isStringified: Boolean, discriminator: Tree): Tree = {
        val tpe = types.head
        val implCodec = findImplicitCodec(types, isValueCodec = true)
        val methodKey = MethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe)), discriminator)
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
          genWriteVal(q"$m.${valueClassValueMethod(tpe)}", valueClassValueType(tpe) :: types, isStringified, EmptyTree)
        } else if (isOption(tpe)) {
          q"""$m match {
                case _root_.scala.Some(x) => ${genWriteVal(q"x", typeArg1(tpe) :: types, isStringified, EmptyTree)}
                case _root_.scala.None => out.writeNull()
              }"""
        } else if (tpe <:< typeOf[immutable.IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] ||
            tpe <:< typeOf[immutable.LongMap[_]]) withEncoderFor(methodKey, m) {
          if (isScala213) {
            val writeVal2 = genWriteVal(q"v", typeArg1(tpe) :: types, isStringified, EmptyTree)
            if (cfg.mapAsArray) {
              val writeVal1 =
                if (isStringified) q"out.writeValAsString(k)"
                else q"out.writeVal(k)"
              genWriteMapAsArrayScala213(q"x", writeVal1, writeVal2)
            } else genWriteMapScala213(q"x", q"out.writeKey(k)", writeVal2)
          } else {
            val writeVal2 = genWriteVal(q"kv._2", typeArg1(tpe) :: types, isStringified, EmptyTree)
            if (cfg.mapAsArray) {
              val writeVal1 =
                if (isStringified) q"out.writeValAsString(kv._1)"
                else q"out.writeVal(kv._1)"
              genWriteMapAsArray(q"x", writeVal1, writeVal2)
            } else genWriteMap(q"x", q"out.writeKey(kv._1)", writeVal2)
          }
        } else if (tpe <:< typeOf[collection.Map[_, _]]) withEncoderFor(methodKey, m) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          if (isScala213) {
            val writeVal2 = genWriteVal(q"v", tpe2 :: types, isStringified, EmptyTree)
            if (cfg.mapAsArray) {
              genWriteMapAsArrayScala213(q"x", genWriteVal(q"k", tpe1 :: types, isStringified, EmptyTree), writeVal2)
            } else genWriteMapScala213(q"x", genWriteKey(q"k", tpe1 :: types), writeVal2)
          } else {
            val writeVal2 = genWriteVal(q"kv._2", tpe2 :: types, isStringified, EmptyTree)
            if (cfg.mapAsArray) {
              genWriteMapAsArray(q"x", genWriteVal(q"kv._1", tpe1 :: types, isStringified, EmptyTree), writeVal2)
            } else genWriteMap(q"x", genWriteKey(q"kv._1", tpe1 :: types), writeVal2)
          }
        } else if (tpe <:< typeOf[BitSet]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x",
            if (isStringified) q"out.writeValAsString(x)"
            else q"out.writeVal(x)")
        } else if (tpe <:< typeOf[List[_]]) withEncoderFor(methodKey, m) {
          val tpe1 = typeArg1(tpe)
          q"""out.writeArrayStart()
              var l: _root_.scala.collection.immutable.List[$tpe1] = x
              while (!l.isEmpty) {
                ..${genWriteVal(q"l.head", tpe1 :: types, isStringified, EmptyTree)}
                l = l.tail
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[IndexedSeq[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.size
              var i = 0
              while (i < l) {
                ..${genWriteVal(q"x(i)", typeArg1(tpe) :: types, isStringified, EmptyTree)}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Iterable[_]]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe) :: types, isStringified, EmptyTree))
        } else if (tpe <:< typeOf[Array[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.length
              var i = 0
              while (i < l) {
                ..${genWriteVal(q"x(i)", typeArg1(tpe) :: types, isStringified, EmptyTree)}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(methodKey, m) {
          if (cfg.useScalaEnumValueId) {
            if (isStringified) q"out.writeValAsString(x.id)"
            else q"out.writeVal(x.id)"
          } else q"out.writeVal(x.toString)"
        } else if (isJavaEnum(tpe)) withEncoderFor(methodKey, m) {
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => cq"${e.value} => ${e.name}") :+
              cq"""_ => out.encodeError("illegal enum value: " + x)"""
            if (encodingRequired) q"out.writeVal(x match { case ..$cases})"
            else q"out.writeNonEscapedAsciiVal(x match { case ..$cases})"
          } else {
            if (encodingRequired) q"out.writeVal(x.name)"
            else q"out.writeNonEscapedAsciiVal(x.name)"
          }
        } else if (tpe.typeSymbol.isModuleClass) withEncoderFor(methodKey, m) {
          q"""out.writeObjectStart()
              ..$discriminator
              out.writeObjectEnd()"""
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m) {
          val writeFields = tpe.typeArgs.zipWithIndex.map { case (ta, i) =>
            genWriteVal(q"x.${TermName("_" + (i + 1))}", ta.dealias :: types, isStringified, EmptyTree)
          }
          q"""out.writeArrayStart()
              ..$writeFields
              out.writeArrayEnd()"""
        } else if (isSealedClass(tpe)) withEncoderFor(methodKey, m) {
          def genWriteLeafClass(subTpe: Type, discriminator: Tree): Tree =
            if (subTpe != tpe) genWriteVal(q"x", subTpe :: types, isStringified, discriminator)
            else genWriteNonAbstractScalaClass(types, discriminator)

          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = (cfg.discriminatorFieldName match {
            case None =>
              val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(_.typeSymbol.isModuleClass)
              leafCaseClasses.map { subTpe =>
                cq"""x: $subTpe =>
                       out.writeObjectStart()
                       ${genWriteConstantKey(discriminatorValue(subTpe))}
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
          genWriteNonAbstractScalaClass(types, discriminator)
        } else if (isConstType(tpe)) getWriteConstType(tpe, m, isStringified)
        else cannotFindValueCodecError(tpe)
      }

      val codec =
        q"""new _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$rootTpe] {
              def nullValue: $rootTpe = ${nullValue(rootTpe :: Nil)}
              def decodeValue(in: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonReader, default: $rootTpe): $rootTpe =
                ${genReadVal(rootTpe :: Nil, q"default", cfg.isStringified, EmptyTree)}
              def encodeValue(x: $rootTpe, out: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter): _root_.scala.Unit =
                ${genWriteVal(q"x", rootTpe :: Nil, cfg.isStringified, EmptyTree)}
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
              ..${fieldTrees.values}
              ..${equalsMethodTrees.values}
              ..${nullValueTrees.values}
              ..${mathContextTrees.values}
              ..${scalaEnumCacheTries.values}
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

  private[this] def groupByOrdered[A, K](xs: collection.Seq[A])(f: A => K): collection.Seq[(K, collection.Seq[A])] =
    xs.foldLeft(mutable.LinkedHashMap.empty[K, ArrayBuffer[A]]) { (m, x) =>
      m.getOrElseUpdate(f(x), new ArrayBuffer[A]) += x
      m
    }.toSeq

  private[this] def duplicated[A](xs: collection.Seq[A]): collection.Seq[A] =
    xs.filter {
      val seen = new mutable.HashSet[A]
      x => !seen.add(x)
    }
}