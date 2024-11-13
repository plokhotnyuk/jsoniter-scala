package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._
import com.github.plokhotnyuk.jsoniter_scala.core._
import scala.annotation.{StaticAnnotation, tailrec}
import scala.annotation.meta.field
import scala.collection.{BitSet, immutable, mutable}
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
  * @param requireCollectionFields a flag that turn on checking of presence of collection fields and forces
  *                               serialization when they are empty
  * @param bigDecimalPrecision    a precision in 'BigDecimal' values (34 by default that is a precision for decimal128,
  *                               see `java.math.MathContext.DECIMAL128.getPrecision`, don't set too big or infinite
  *                               precision to avoid attacks from untrusted input)
  * @param bigDecimalScaleLimit   an exclusive limit for accepted scale in 'BigDecimal' values (6178 by default that is
  *                               a range for decimal128, don't set too big scale limit to avoid attacks from untrusted
  *                               input)
  * @param bigDecimalDigitsLimit  an exclusive limit for accepted number of mantissa digits of to be parsed before
  *                               rounding with the precision specified for 'BigDecimal' values (308 by default, don't
  *                               set too big limit to avoid of OOM errors or attacks from untrusted input)
  * @param bigIntDigitsLimit      an exclusive limit for accepted number of decimal digits in 'BigInt' values
  *                               (308 by default, don't set too big limit to avoid of OOM errors or attacks from
  *                               untrusted input)
  * @param bitSetValueLimit       an exclusive limit for accepted numeric values in bit sets (1024 by default, don't set
  *                               too big limit to avoid of OOM errors or attacks from untrusted input)
  * @param mapMaxInsertNumber     a max number of inserts into maps (1024 by default to limit attacks from untrusted
  *                               input that exploit worst complexity for inserts, see https://github.com/scala/bug/issues/11203 )
  * @param setMaxInsertNumber     a max number of inserts into sets excluding bit sets (1024 by default to limit attacks
  *                               from untrusted input that exploit worst complexity for inserts, see https://github.com/scala/bug/issues/11203 )
  * @param allowRecursiveTypes    a flag that turns on support of recursive types (turned off by default to avoid
  *                               stack overflow errors with untrusted input)
  * @param requireDiscriminatorFirst a flag that turns off limitation for a position of the discriminator field to be
  *                               the first field of the JSON object (turned on by default to avoid CPU overuse when
  *                               the discriminator appears in the end of JSON objects, especially nested)
  * @param useScalaEnumValueId    a flag that turns on using of ids for parsing and serialization of Scala enumeration
  *                               values
  * @param skipNestedOptionValues a flag that turns on skipping of some values for nested more than 2-times options and
  *                               allow using `Option[Option[_]]` field values to distinguish `null` and missing field
  *                               cases
  * @param circeLikeObjectEncoding a flag that turns on serialization and parsing of Scala objects as JSON objects with
  *                               a key and empty object value: `{"EnumValue":{}}`
  * @param decodingOnly           a flag that turns generation of decoding implementation only (turned off by default)
  * @param encodingOnly           a flag that turns generation of encoding implementation only (turned off by default)
  * @param requireDefaultFields   a flag that turns on checking of presence of fields with default and forces
  *                               serialization of them
  * @param checkFieldDuplication  a flag that turns on checking of duplicated fields during parsing of classes (turned
  *                               on by default)
  * @param scalaTransientSupport  a flag that turns on support of `scala.transient` (turned off by default)
  * @param inlineOneValueClasses  a flag that turns on derivation of inlined codecs for non-values classes that have
  *                               the primary constructor with just one argument (turned off by default)
  * @param alwaysEmitDiscriminator a flag that causes the discriminator field and value to always be serialized, even
  *                               when the codec derived is for an ADT leaf class and not the ADT base class. Note that
  *                               this flag has no effect on generated decoders -- that is this flag does NOT cause
  *                               decoders to start requiring the discriminator field when they are not strictly necessary
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
    val useScalaEnumValueId: Boolean,
    val skipNestedOptionValues: Boolean,
    val circeLikeObjectEncoding: Boolean,
    val decodingOnly: Boolean,
    val encodingOnly: Boolean,
    val requireDefaultFields: Boolean,
    val checkFieldDuplication: Boolean,
    val scalaTransientSupport: Boolean,
    val inlineOneValueClasses: Boolean,
    val alwaysEmitDiscriminator: Boolean) {
  def withFieldNameMapper(fieldNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(fieldNameMapper = fieldNameMapper)

  def withJavaEnumValueNameMapper(javaEnumValueNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(javaEnumValueNameMapper = javaEnumValueNameMapper)

  def withAdtLeafClassNameMapper(adtLeafClassNameMapper: String => String): CodecMakerConfig =
    copy(adtLeafClassNameMapper = adtLeafClassNameMapper)

  def withDiscriminatorFieldName(discriminatorFieldName: Option[String]): CodecMakerConfig =
    copy(discriminatorFieldName = discriminatorFieldName)

  def withAlwaysEmitDiscriminator(alwaysEmitDiscriminator: Boolean): CodecMakerConfig =
    copy(alwaysEmitDiscriminator = alwaysEmitDiscriminator)

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

  def withSkipNestedOptionValues(skipNestedOptionValues: Boolean): CodecMakerConfig =
    copy(skipNestedOptionValues = skipNestedOptionValues)

  def withCirceLikeObjectEncoding(circeLikeObjectEncoding: Boolean): CodecMakerConfig =
    copy(circeLikeObjectEncoding = circeLikeObjectEncoding)

  def withDecodingOnly(decodingOnly: Boolean): CodecMakerConfig =
    copy(decodingOnly = decodingOnly)

  def withEncodingOnly(encodingOnly: Boolean): CodecMakerConfig =
    copy(encodingOnly = encodingOnly)

  def withRequireDefaultFields(requireDefaultFields: Boolean): CodecMakerConfig =
    copy(requireDefaultFields = requireDefaultFields)

  def withCheckFieldDuplication(checkFieldDuplication: Boolean): CodecMakerConfig =
    copy(checkFieldDuplication = checkFieldDuplication)

  def withScalaTransientSupport(scalaTransientSupport: Boolean): CodecMakerConfig =
    copy(scalaTransientSupport = scalaTransientSupport)

  def withInlineOneValueClasses(inlineOneValueClasses: Boolean): CodecMakerConfig =
    copy(inlineOneValueClasses = inlineOneValueClasses)

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
                         useScalaEnumValueId: Boolean = useScalaEnumValueId,
                         skipNestedOptionValues: Boolean = skipNestedOptionValues,
                         circeLikeObjectEncoding: Boolean = circeLikeObjectEncoding,
                         decodingOnly: Boolean = decodingOnly,
                         encodingOnly: Boolean = encodingOnly,
                         requireDefaultFields: Boolean = requireDefaultFields,
                         checkFieldDuplication: Boolean = checkFieldDuplication,
                         scalaTransientSupport: Boolean = scalaTransientSupport,
                         inlineOneValueClasses: Boolean = inlineOneValueClasses,
                         alwaysEmitDiscriminator: Boolean = alwaysEmitDiscriminator): CodecMakerConfig =
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
      useScalaEnumValueId = useScalaEnumValueId,
      skipNestedOptionValues = skipNestedOptionValues,
      circeLikeObjectEncoding = circeLikeObjectEncoding,
      decodingOnly = decodingOnly,
      encodingOnly = encodingOnly,
      requireDefaultFields = requireDefaultFields,
      checkFieldDuplication = checkFieldDuplication,
      scalaTransientSupport = scalaTransientSupport,
      inlineOneValueClasses = inlineOneValueClasses,
      alwaysEmitDiscriminator = alwaysEmitDiscriminator)
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
  bigDecimalPrecision = 34,
  bigDecimalScaleLimit = 6178,
  bigDecimalDigitsLimit = 308,
  bigIntDigitsLimit = 308,
  bitSetValueLimit = 1024,
  mapMaxInsertNumber = 1024,
  setMaxInsertNumber = 1024,
  allowRecursiveTypes = false,
  requireDiscriminatorFirst = true,
  useScalaEnumValueId = false,
  skipNestedOptionValues = false,
  circeLikeObjectEncoding = false,
  encodingOnly = false,
  decodingOnly = false,
  requireDefaultFields = false,
  checkFieldDuplication = true,
  scalaTransientSupport = false,
  inlineOneValueClasses = false,
  alwaysEmitDiscriminator = false) {

  /**
    * Use to enable printing of codec during compilation:
    *{{{
    *implicit val printCodec: CodecMakerConfig.PrintCodec = new CodecMakerConfig.PrintCodec {}
    *val codec = JsonCodecMaker.make[MyClass]
    *}}}
    **/
  class PrintCodec
}

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
    * Mapping function for field or class names that should be in snake_case format
    * with separated non-alphabetic characters.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val enforce_snake_case: PartialFunction[String, String] =
    { case s => enforceSnakeOrKebabCaseWithSeparatedNonAlphabetic(s, '_') }

  /**
    * Mapping function for field or class names that should be in snake_case format
    * with joined non-alphabetic characters.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val enforce_snake_case2: PartialFunction[String, String] =
    { case s => enforceSnakeOrKebabCaseWithJoinedNonAphabetic(s, '_') }

  /**
    * Mapping function for field or class names that should be in kebab-case format
    * with separated non-alphabetic characters.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val `enforce-kebab-case`: PartialFunction[String, String] =
    { case s => enforceSnakeOrKebabCaseWithSeparatedNonAlphabetic(s, '-') }

  /**
    * Mapping function for field or class names that should be in kebab-case format
    * with joined non-alphabetic characters.
    *
    * @return a transformed name or the same name if no transformation is required
    */
  val `enforce-kebab-case2`: PartialFunction[String, String] =
    { case s => enforceSnakeOrKebabCaseWithJoinedNonAphabetic(s, '-') }

  private[this] def enforceCamelOrPascalCase(s: String, toPascal: Boolean): String =
    if (s.indexOf('_') == -1 && s.indexOf('-') == -1) {
      if (s.isEmpty) s
      else {
        val ch = s.charAt(0)
        val fixedCh =
          if (toPascal) toUpperCase(ch)
          else toLowerCase(ch)
        s"$fixedCh${s.substring(1)}"
      }
    } else {
      val len = s.length
      val sb = new java.lang.StringBuilder(len)
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

  private[this] def enforceSnakeOrKebabCaseWithSeparatedNonAlphabetic(s: String, separator: Char): String = {
    val len = s.length
    val sb = new java.lang.StringBuilder(len << 1)
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

  private[this] def enforceSnakeOrKebabCaseWithJoinedNonAphabetic(s: String, separator: Char): String = {
    val len = s.length
    val sb = new java.lang.StringBuilder(len << 1)
    var i = 0
    var isPrecedingNotUpperCased = false
    while (i < len) isPrecedingNotUpperCased = {
      val ch = s.charAt(i)
      i += 1
      if (ch == '_' || ch == '-') {
        if (i > 1 && i < len && !isAlphabetic(s.charAt(i))) isPrecedingNotUpperCased
        else {
          sb.append(separator)
          false
        }
      } else if (!isUpperCase(ch)) {
        sb.append(ch)
        true
      } else {
        if (isPrecedingNotUpperCased || i > 1 && i < len && !isUpperCase(s.charAt(i))) sb.append(separator)
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
   * A replacement for the `make` call with the
   * `CodecMakerConfig.withTransientDefault(false).withRequireDefaultFields(true)`
   * configuration parameter.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  def makeWithRequiredDefaultFields[A]: JsonValueCodec[A] = macro Impl.makeWithRequiredDefaultFields[A]

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false).withDiscriminatorFieldName(None)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def makeCirceLike[A]: JsonValueCodec[A] = macro Impl.makeCirceLike[A]

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false).withDiscriminatorFieldName(None).withAdtLeafClassNameMapper(x => enforce_snake_case(simpleClassName(x))).withFieldNameMapper(enforce_snake_case).withJavaEnumValueNameMapper(enforce_snake_case)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  def makeCirceLikeSnakeCased[A]: JsonValueCodec[A] = macro Impl.makeCirceLikeSnakeCased[A]

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
      make(c)(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)
        .withDiscriminatorFieldName(Some("name")))

    def makeWithRequiredDefaultFields[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withTransientDefault(false).withRequireDefaultFields(true))

    def makeCirceLike[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false)
        .withDiscriminatorFieldName(None).withCirceLikeObjectEncoding(true))

    def makeCirceLikeSnakeCased[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonValueCodec[A]] =
      make(c)(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false)
        .withDiscriminatorFieldName(None).withAdtLeafClassNameMapper(x => enforce_snake_case(simpleClassName(x)))
        .withFieldNameMapper(enforce_snake_case).withJavaEnumValueNameMapper(enforce_snake_case)
        .withCirceLikeObjectEncoding(true))

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
        if (cfg.requireCollectionFields && cfg.transientEmpty)
          fail("'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously")
        if (cfg.requireDefaultFields && cfg.transientDefault)
          fail("'requireDefaultFields' and 'transientDefault' cannot be 'true' simultaneously")
        if (cfg.circeLikeObjectEncoding && cfg.discriminatorFieldName.nonEmpty)
          fail("'discriminatorFieldName' should be 'None' when 'circeLikeObjectEncoding' is 'true'")
        if (cfg.alwaysEmitDiscriminator && cfg.discriminatorFieldName.isEmpty)
          fail("'discriminatorFieldName' should not be 'None' when 'alwaysEmitDiscriminator' is 'true'")
        if (cfg.decodingOnly && cfg.encodingOnly)
          fail("'decodingOnly' and 'encodingOnly' cannot be 'true' simultaneously")
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

      def valueClassValueMethod(tpe: Type): MethodSymbol = tpe.decls.head.asMethod

      def decodeName(s: Symbol): String = NameTransformer.decode(s.name.toString)

      def resolveConcreteType(tpe: Type, mtpe: Type): Type = {
        val tpeTypeParams =
          if (tpe.typeSymbol.isClass) tpe.typeSymbol.asClass.typeParams
          else Nil
        if (tpeTypeParams.isEmpty) mtpe
        else mtpe.substituteTypes(tpeTypeParams, tpe.typeArgs)
      }

      def paramType(tpe: Type, p: TermSymbol): Type = resolveConcreteType(tpe, p.typeSignature.dealias)

      def valueClassValueType(tpe: Type): Type = resolveConcreteType(tpe, valueClassValueMethod(tpe).returnType.dealias)

      def isNonAbstractScalaClass(tpe: Type): Boolean =
        tpe.typeSymbol.isClass && !tpe.typeSymbol.isAbstract && !tpe.typeSymbol.isJava

      def isSealedClass(tpe: Type): Boolean = tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isSealed

      def hasSealedParent(tpe: Type): Boolean = tpe.typeSymbol.isClass && {
        val classSymbol = tpe.typeSymbol.asClass
        classSymbol.isSealed || classSymbol.baseClasses.exists(s => s.isClass && s.asClass.isSealed)
      }

      def isConstType(tpe: Type): Boolean = tpe match {
        case ConstantType(Constant(_)) => true
        case _ => false
      }

      def companion(tpe: Type): Symbol = {
        val comp = tpe.typeSymbol.companion
        if (comp.isModule) comp
        else { // Borrowed from Magnolia: https://github.com/softwaremill/magnolia/blob/f21f2aabb49e43b372240e98ec77981662cc570c/core/shared/src/main/scala/magnolia.scala#L123-L155
          val ownerChainOf = (s: Symbol) =>
            Iterator.iterate(s)(_.owner).takeWhile(x => x != NoSymbol).toVector.reverseIterator
          val path = ownerChainOf(tpe.typeSymbol)
            .zipAll(ownerChainOf(enclosingOwner), NoSymbol, NoSymbol)
            .dropWhile { case (x, y) => x == y }
            .takeWhile { case (x, _) => x != NoSymbol }
            .map { case (x, _) => x.name.toTermName }
          if (path.isEmpty) fail(s"Cannot find a companion for $tpe")
          else c.typecheck(path.foldLeft[Tree](Ident(path.next()))(Select(_, _)), silent = true).symbol
        }
      }

      def isOption(tpe: Type, types: List[Type]): Boolean =
        tpe <:< typeOf[Option[_]] && (cfg.skipNestedOptionValues || !types.headOption.exists(_ <:< typeOf[Option[_]]))

      def isCollection(tpe: Type): Boolean =
        tpe <:< typeOf[Iterable[_]] || tpe <:< typeOf[Iterator[_]] || tpe <:< typeOf[Array[_]]

      def scalaCollectionCompanion(tpe: Type): Tree =
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ident(tpe.typeSymbol.companion)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")

      def enumSymbol(tpe: Type): Symbol = tpe match {
        case TypeRef(SingleType(_, enumSymbol), _, _) => enumSymbol
      }

      val isScala213: Boolean = util.Properties.versionNumberString.startsWith("2.13.")
      val rootTpe = weakTypeOf[A].dealias
      val inferredKeyCodecs = mutable.Map.empty[Type, Tree]
      val inferredValueCodecs = mutable.Map.empty[Type, Tree]

      def isImmutableArraySeq(tpe: Type): Boolean =
        isScala213 && tpe.typeSymbol.fullName == "scala.collection.immutable.ArraySeq"

      def isMutableArraySeq(tpe: Type): Boolean =
        isScala213 && tpe.typeSymbol.fullName == "scala.collection.mutable.ArraySeq"

      def isCollisionProofHashMap(tpe: Type): Boolean =
        isScala213 && tpe.typeSymbol.fullName == "scala.collection.mutable.CollisionProofHashMap"

      def inferImplicitValue(typeTree: Tree): Tree = c.inferImplicitValue(c.typecheck(typeTree, c.TYPEmode).tpe)

      def checkRecursionInTypes(types: List[Type]): Unit =
        if (!cfg.allowRecursiveTypes) {
          val tpe = types.head
          val nestedTypes = types.tail
          val recursiveIdx = nestedTypes.indexOf(tpe)
          if (recursiveIdx >= 0) {
            val recTypes = nestedTypes.take(recursiveIdx + 1).reverse.mkString("'", "', '", "'")
            fail(s"Recursive type(s) detected: $recTypes. Please consider using a custom implicitly " +
              s"accessible codec for this type to control the level of recursion or turn on the " +
              s"'${typeOf[CodecMakerConfig]}.allowRecursiveTypes' for the trusted input that " +
              "will not exceed the thread stack size.")
          }
        }

      def findImplicitKeyCodec(types: List[Type]): Tree = {
        checkRecursionInTypes(types)
        val tpe = types.head
        if (tpe =:= rootTpe) EmptyTree
        else {
          inferredKeyCodecs.getOrElseUpdate(tpe,
            inferImplicitValue(tq"_root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonKeyCodec[$tpe]"))
        }
      }

      def findImplicitValueCodec(types: List[Type]): Tree = {
        checkRecursionInTypes(types)
        val tpe = types.head
        if (tpe =:= rootTpe) EmptyTree
        else {
          inferredValueCodecs.getOrElseUpdate(tpe,
            inferImplicitValue(tq"_root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$tpe]"))
        }
      }

      val mathContexts = new mutable.LinkedHashMap[Int, (TermName, Tree)]

      def withMathContextFor(precision: Int): Tree =
        if (precision == java.math.MathContext.DECIMAL128.getPrecision) q"_root_.java.math.MathContext.DECIMAL128"
        else if (precision == java.math.MathContext.DECIMAL64.getPrecision) q"_root_.java.math.MathContext.DECIMAL64"
        else if (precision == java.math.MathContext.DECIMAL32.getPrecision) q"_root_.java.math.MathContext.DECIMAL32"
        else if (precision == java.math.MathContext.UNLIMITED.getPrecision) q"_root_.java.math.MathContext.UNLIMITED"
        else Ident(mathContexts.getOrElseUpdate(precision, {
          val name = TermName("mc" + mathContexts.size)
          (name,
            q"private[this] val $name = new _root_.java.math.MathContext(${cfg.bigDecimalPrecision}, _root_.java.math.RoundingMode.HALF_EVEN)")
        })._1)

      val scalaEnumCaches = new mutable.LinkedHashMap[Type, (TermName, Tree)]

      def withScalaEnumCacheFor(tpe: Type): Tree = Ident(scalaEnumCaches.getOrElseUpdate(tpe, {
        val name = TermName("ec" + scalaEnumCaches.size)
        val keyTpe =
          if (cfg.useScalaEnumValueId) tq"Int"
          else tq"String"
        (name,
          q"private[this] val $name = new _root_.java.util.concurrent.ConcurrentHashMap[$keyTpe, $tpe]")
      })._1)

      case class JavaEnumValueInfo(value: Tree, name: String, transformed: Boolean)

      val enumValueInfos = new mutable.LinkedHashMap[Type, List[JavaEnumValueInfo]]

      def isJavaEnum(tpe: Type): Boolean = tpe <:< typeOf[java.lang.Enum[_]]

      def javaEnumValues(tpe: Type): List[JavaEnumValueInfo] = enumValueInfos.getOrElseUpdate(tpe, {
        val javaEnumValueNameMapper: String => String = n => cfg.javaEnumValueNameMapper.lift(n).getOrElse(n)
        var values = tpe.typeSymbol.asClass.knownDirectSubclasses.toList.map { s: Symbol =>
          val name = s.name.toString
          val transformedName = javaEnumValueNameMapper(name)
          JavaEnumValueInfo(q"$s", transformedName, name != transformedName)
        }
        if (values.isEmpty) {
          val comp = companion(tpe)
          values =
            comp.typeSignature.members.sorted.collect { case m: MethodSymbol if m.isGetter && m.returnType.dealias =:= tpe =>
              val name = decodeName(m)
              val transformedName = javaEnumValueNameMapper(name)
              JavaEnumValueInfo(q"$comp.${TermName(name)}", transformedName, name != transformedName)
            }
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

      def genReadEnumValue(enumValues: Seq[JavaEnumValueInfo], unexpectedEnumValueHandler: Tree): Tree = {
        def genReadCollisions(es: collection.Seq[JavaEnumValueInfo]): Tree =
          es.foldRight(unexpectedEnumValueHandler) { (e, acc) =>
            q"if (in.isCharBufEqualsTo(l, ${e.name})) ${e.value} else $acc"
          }

        if (enumValues.size <= 8 && enumValues.foldLeft(0)(_ + _.name.length) <= 64) genReadCollisions(enumValues)
        else {
          val hashCode = (e: JavaEnumValueInfo) => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
          val cases = groupByOrdered(enumValues)(hashCode).map { case (hash, fs) =>
            cq"$hash => ${genReadCollisions(fs)}"
          } :+ cq"_ => $unexpectedEnumValueHandler"
          q"""(in.charBufToHashCode(l): @_root_.scala.annotation.switch) match {
                case ..$cases
              }"""
        }
      }

      case class FieldInfo(symbol: TermSymbol, mappedName: String, tmpName: TermName, getter: MethodSymbol,
                           defaultValue: Option[Tree], resolvedTpe: Type, isStringified: Boolean)

      case class ClassInfo(tpe: Type, paramLists: List[List[FieldInfo]]) {
        val fields: List[FieldInfo] = paramLists.flatten
      }

      val classInfos = new mutable.LinkedHashMap[Type, ClassInfo]

      def getClassInfo(tpe: Type): ClassInfo = classInfos.getOrElseUpdate(tpe, {
        case class FieldAnnotations(partiallyMappedName: String, transient: Boolean, stringified: Boolean)

        def getPrimaryConstructor(tpe: Type): MethodSymbol = tpe.decls.collectFirst {
          case m: MethodSymbol if m.isPrimaryConstructor => m // FIXME: sometime it cannot be accessed from the place of the `make` call
        }.getOrElse(fail(s"Cannot find a primary constructor for '$tpe'"))

        def hasSupportedAnnotation(m: TermSymbol): Boolean = {
          m.info: Unit // to enforce the type information completeness and availability of annotations
          m.annotations.exists(a => a.tree.tpe =:= typeOf[named] || a.tree.tpe =:= typeOf[transient] ||
            a.tree.tpe =:= typeOf[stringified] || (cfg.scalaTransientSupport && a.tree.tpe =:= typeOf[scala.transient]))
        }

        def supportedTransientTypeNames: String =
          if (cfg.scalaTransientSupport) s"'${typeOf[transient]}' (or '${typeOf[scala.transient]}')"
          else s"'${typeOf[transient]}')"

        lazy val module = companion(tpe).asModule // don't lookup for the companion when there are no default values for constructor params
        val getters = tpe.members.collect { case m: MethodSymbol if m.isParamAccessor && m.isGetter => m }
        val annotations = tpe.members.collect {
          case m: TermSymbol if hasSupportedAnnotation(m) =>
            val name = decodeName(m).trim // FIXME: Why is there a space at the end of field name?!
            val named = m.annotations.filter(_.tree.tpe =:= typeOf[named])
            if (named.size > 1) fail(s"Duplicated '${typeOf[named]}' defined for '$name' of '$tpe'.")
            val trans = m.annotations.filter(a => a.tree.tpe =:= typeOf[transient]  ||
              (cfg.scalaTransientSupport && a.tree.tpe =:= typeOf[scala.transient]))
            if (trans.size > 1) warn(s"Duplicated $supportedTransientTypeNames defined for '$name' of '$tpe'.")
            val strings = m.annotations.filter(_.tree.tpe =:= typeOf[stringified])
            if (strings.size > 1) warn(s"Duplicated '${typeOf[stringified]}' defined for '$name' of '$tpe'.")
            if ((named.nonEmpty || strings.nonEmpty) && trans.nonEmpty) {
              warn(s"Both $supportedTransientTypeNames and '${typeOf[named]}' or " +
                s"$supportedTransientTypeNames and '${typeOf[stringified]}' defined for '$name' of '$tpe'.")
            }
            val partiallyMappedName = namedValueOpt(named.headOption, tpe).getOrElse(name)
            (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        ClassInfo(tpe, {
          var i = 0
          getPrimaryConstructor(tpe).paramLists.map(_.flatMap { p =>
            i += 1
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
                if (!cfg.requireDefaultFields && symbol.isParamWithDefault) {
                  Some(q"$module.${TermName("$lessinit$greater$default$" + i)}")
                } else None
              val isStringified = annotationOption.exists(_.stringified)
              Some(FieldInfo(symbol, mappedName, tmpName, getter, defaultValue, paramType(tpe, symbol), isStringified))
            }
          })
        })
      })

      def isValueClass(tpe: Type): Boolean = !isConstType(tpe) &&
        (cfg.inlineOneValueClasses && isNonAbstractScalaClass(tpe) && !isCollection(tpe) && getClassInfo(tpe).fields.size == 1 ||
          tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isDerivedValueClass)

      def adtLeafClasses(adtBaseTpe: Type): List[Type] = {
        def collectRecursively(tpe: Type): List[Type] = {
          val tpeClass = tpe.typeSymbol.asClass
          val leafTpes = tpeClass.knownDirectSubclasses.toList.flatMap { s =>
            val classSymbol = s.asClass
            val typeParams = classSymbol.typeParams
            val subTpe =
              if (typeParams.isEmpty) classSymbol.toType
              else {
                val typeParamsAndArgs = tpeClass.typeParams.map(_.toString).zip(tpe.typeArgs).toMap
                val typeArgs = typeParams.map(s => typeParamsAndArgs.getOrElse(s.toString, fail {
                  s"Cannot resolve generic type(s) for `${classSymbol.toType}`. Please provide a custom implicitly accessible codec for it."
                }))
                classSymbol.toType.substituteTypes(typeParams, typeArgs)
              }
            if (isSealedClass(subTpe)) collectRecursively(subTpe)
            else if (isValueClass(subTpe)) {
              fail("'AnyVal' and one value classes with 'CodecMakerConfig.withInlineOneValueClasses(true)' are not " +
                s"supported as leaf classes for ADT with base '$adtBaseTpe'.")
            } else if (isNonAbstractScalaClass(subTpe)) subTpe :: Nil
            else fail((if (s.isAbstract) {
              "Only sealed intermediate traits or abstract classes are supported."
            } else {
              "Only concrete (no free type parameters) Scala classes & objects are supported for ADT leaf classes."
            }) + s" Please consider using of them for ADT with base '$adtBaseTpe' or provide a custom implicitly accessible codec for the ADT base.")
          }
          if (isNonAbstractScalaClass(tpe)) leafTpes :+ tpe
          else leafTpes
        }

        val classes = collectRecursively(adtBaseTpe).distinct
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$adtBaseTpe'. " +
          "Please add them or provide a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def genReadKey(types: List[Type]): Tree = {
        val tpe = types.head
        val implKeyCodec = findImplicitKeyCodec(types)
        if (implKeyCodec.nonEmpty) q"$implKeyCodec.decodeKey(in)"
        else if (tpe =:= typeOf[String]) q"in.readKeyAsString()"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readKeyAsBoolean()"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"in.readKeyAsByte()"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readKeyAsChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"in.readKeyAsShort()"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"in.readKeyAsInt()"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"in.readKeyAsLong()"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"in.readKeyAsFloat()"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"in.readKeyAsDouble()"
        else if (isValueClass(tpe)) q"new $tpe(${genReadKey(valueClassValueType(tpe) :: types)})"
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
        if (cfg.setMaxInsertNumber == Int.MaxValue) genReadArray(newBuilder, readVal, result)
        else {
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
        }

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        if (cfg.setMaxInsertNumber == Int.MaxValue) {
          q"""if (in.isNextToken('{')) {
                if (in.isNextToken('}')) default
                else {
                  in.rollbackToken()
                  ..$newBuilder
                  while ({
                    ..$readKV
                    in.isNextToken(',')
                  }) ()
                  if (in.isCurrentToken('}')) $result
                  else in.objectEndOrCommaError()
                }
              } else in.readNullOrTokenError(default, '{')"""
        } else {
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
        }

      def genReadMapAsArray(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        if (cfg.setMaxInsertNumber == Int.MaxValue) {
          q"""if (in.isNextToken('[')) {
                if (in.isNextToken(']')) default
                else {
                  in.rollbackToken()
                  ..$newBuilder
                  while ({
                    if (in.isNextToken('[')) {
                      ..$readKV
                      if (!in.isNextToken(']')) in.arrayEndError()
                    } else in.decodeError("expected '['")
                    in.isNextToken(',')
                  }) ()
                  if (in.isCurrentToken(']')) $result
                  else in.arrayEndOrCommaError()
                }
              } else in.readNullOrTokenError(default, '[')"""
        } else {
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
                    } else in.decodeError("expected '['")
                    in.isNextToken(',')
                  }) ()
                  if (in.isCurrentToken(']')) $result
                  else in.arrayEndOrCommaError()
                }
              } else in.readNullOrTokenError(default, '[')"""
        }

      @tailrec
      def genWriteKey(x: Tree, types: List[Type]): Tree = {
        val tpe = types.head
        val implKeyCodec = findImplicitKeyCodec(types)
        if (implKeyCodec.nonEmpty) q"$implKeyCodec.encodeKey($x, out)"
        else if (tpe =:= typeOf[String] || tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] ||
          tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte] || tpe =:= definitions.CharTpe ||
          tpe =:= typeOf[java.lang.Character] || tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] || tpe =:= definitions.LongTpe ||
          tpe =:= typeOf[java.lang.Long] || tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double] || tpe =:= typeOf[BigInt] ||
          tpe =:= typeOf[BigDecimal] || tpe =:= typeOf[java.util.UUID] || tpe =:= typeOf[Duration] ||
          tpe =:= typeOf[Instant] || tpe =:= typeOf[LocalDate] || tpe =:= typeOf[LocalDateTime] ||
          tpe =:= typeOf[LocalTime] || tpe =:= typeOf[MonthDay] || tpe =:= typeOf[OffsetDateTime] ||
          tpe =:= typeOf[OffsetTime] || tpe =:= typeOf[Period] || tpe =:= typeOf[Year] || tpe =:= typeOf[YearMonth] ||
          tpe =:= typeOf[ZonedDateTime] || tpe =:= typeOf[ZoneId] || tpe =:= typeOf[ZoneOffset]) q"out.writeKey($x)"
        else if (isValueClass(tpe)) genWriteKey(q"$x.${valueClassValueMethod(tpe)}", valueClassValueType(tpe) :: types)
        else if (tpe <:< typeOf[Enumeration#Value]) {
          if (cfg.useScalaEnumValueId) q"out.writeKey($x.id)"
          else q"out.writeKey($x.toString)"
        } else if (isJavaEnum(tpe)) {
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => cq"${e.value} => ${e.name}") :+
              cq"""_ => out.encodeError("illegal enum value: " + $x)"""
            if (encodingRequired) q"out.writeKey($x match { case ..$cases })"
            else q"out.writeNonEscapedAsciiKey($x match { case ..$cases })"
          } else {
            if (encodingRequired) q"out.writeKey($x.name)"
            else q"out.writeNonEscapedAsciiKey($x.name)"
          }
        } else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(Constant(s: String)) => genWriteConstantKey(s)
            case ConstantType(Constant(_: Boolean)) | ConstantType(Constant(_: Byte)) | ConstantType(Constant(_: Char)) |
              ConstantType(Constant(_: Short)) | ConstantType(Constant(_: Int)) | ConstantType(Constant(_: Long)) |
              ConstantType(Constant(_: Float)) | ConstantType(Constant(_: Double)) => q"out.writeKey($x)"
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

      def genWriteArray2(x: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            while($x.hasNext) {
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

      def namedValueOpt(namedOpt: Option[Annotation], tpe: Type): Option[String] = namedOpt.map { a =>
        a.tree.children.tail.collectFirst { case Literal(Constant(s: String)) => s }.getOrElse {
          try c.eval(c.Expr[named](c.untypecheck(a.tree.duplicate))).name catch {
            case ex: Throwable =>
              fail(s"Cannot evaluate a parameter of the '@named' annotation in type '$tpe'. " +
                "It should not depend on code from the same compilation module where the 'make' macro is called. " +
                "Use a separated submodule of the project to compile all such dependencies before their usage " +
                s"for generation of codecs. Cause:\n$ex")
          }
        }
      }

      val unexpectedFieldHandler =
        if (cfg.skipUnexpectedFields) q"in.skip()"
        else q"in.unexpectedKeyError(l)"
      val skipDiscriminatorField =
        if (cfg.checkFieldDuplication) {
          q"""if (pd) {
                pd = false
                in.skip()
              } else in.duplicatedKeyError(l)"""
        } else q"in.skip()"

      def discriminatorValue(tpe: Type): String = {
        val named = tpe.typeSymbol.annotations.filter(_.tree.tpe =:= typeOf[named])
        if (named.size > 1) fail(s"Duplicated '${typeOf[named]}' defined for '$tpe'.")
        namedValueOpt(named.headOption, tpe)
          .getOrElse(cfg.adtLeafClassNameMapper(NameTransformer.decode(tpe.typeSymbol.fullName)))
      }

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

      val nullValues = new mutable.LinkedHashMap[Type, (TermName, Tree)]

      def withNullValueFor(tpe: Type)(f: => Tree): Tree = Ident(nullValues.getOrElseUpdate(tpe, {
        val name = TermName("c" + nullValues.size)
        (name, q"private[this] val $name: $tpe = $f")
      })._1)

      val fields = new mutable.LinkedHashMap[Type, (TermName, Tree)]

      def withFieldsFor(tpe: Type)(f: => Seq[String]): Tree = Ident(fields.getOrElseUpdate(tpe, {
        val name = TermName("f" + fields.size)
        val cases = f.map {
          var i = -1
          n =>
            i += 1
            cq"$i => $n"
        }
        (name,
          q"""private[this] def $name(i: Int): String =
                (i: @_root_.scala.annotation.switch @_root_.scala.unchecked) match {
                  case ..$cases
                }""")
      })._1)

      val equalsMethods = new mutable.LinkedHashMap[Type, (TermName, Tree)]

      def withEqualsFor(tpe: Type, arg1: Tree, arg2: Tree)(f: => Tree): Tree = {
        val equalsMethodName = equalsMethods.getOrElseUpdate(tpe, {
          val name = TermName("q" + equalsMethods.size)
          (name, q"private[this] def $name(x1: $tpe, x2: $tpe): _root_.scala.Boolean = $f")
        })._1
        q"$equalsMethodName($arg1, $arg2)"
      }

      def genArrayEquals(tpe: Type): Tree = {
        val tpe1 = typeArg1(tpe)
        if (tpe1 <:< typeOf[Array[_]]) {
          val equals = withEqualsFor(tpe1, q"x1(i)", q"x2(i)")(genArrayEquals(tpe1))
          q"""(x1 eq x2) || ((x1 ne null) && (x2 ne null) && {
                val l = x1.length
                (x2.length == l) && {
                  var i = 0
                  while (i < l && $equals) i += 1
                  i == l
                }
              })"""
        } else q"_root_.java.util.Arrays.equals(x1, x2)"
      }

      case class MethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree)

      val decodeMethodNames = new mutable.HashMap[MethodKey, TermName]
      val decodeMethodTrees = new mutable.ArrayBuffer[Tree]

      def withDecoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val decodeMethodName = decodeMethodNames.getOrElse(methodKey, {
          val name = TermName("d" + decodeMethodNames.size)
          val mtpe = methodKey.tpe
          decodeMethodNames.update(methodKey, name)
          decodeMethodTrees +=
            q"private[this] def $name(in: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonReader, default: $mtpe): $mtpe = $f"
          name
        })
        q"$decodeMethodName(in, $arg)"
      }

      val encodeMethodNames = new mutable.HashMap[MethodKey, TermName]
      val encodeMethodTrees = new mutable.ArrayBuffer[Tree]

      def withEncoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val encodeMethodName = encodeMethodNames.getOrElse(methodKey, {
          val name = TermName("e" + encodeMethodNames.size)
          encodeMethodNames.update(methodKey, name)
          encodeMethodTrees +=
            q"private[this] def $name(x: ${methodKey.tpe}, out: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter): _root_.scala.Unit = $f"
          name
        })
        q"$encodeMethodName($arg, out)"
      }

      def genNullValue(types: List[Type]): Tree = {
        val tpe = types.head
        val implValueCodec = findImplicitValueCodec(types)
        if (implValueCodec.nonEmpty) q"$implValueCodec.nullValue"
        else if (tpe =:= typeOf[String]) q"null"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"false"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"(0: _root_.scala.Byte)"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"'\u0000'"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"(0: _root_.scala.Short)"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"0"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"0L"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"0f"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"0.0"
        else if (isOption(tpe, types.tail)) q"_root_.scala.None"
        else if (tpe <:< typeOf[mutable.BitSet]) q"new _root_.scala.collection.mutable.BitSet"
        else if (tpe <:< typeOf[collection.BitSet]) withNullValueFor(tpe)(q"_root_.scala.collection.immutable.BitSet.empty")
        else if (tpe <:< typeOf[mutable.LongMap[_]]) q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[::[_]]) q"null"
        else if (tpe <:< typeOf[List[_]] || tpe.typeSymbol == typeOf[Seq[_]].typeSymbol) q"_root_.scala.Nil"
        else if (tpe <:< typeOf[immutable.IntMap[_]] || tpe <:< typeOf[immutable.LongMap[_]] ||
          tpe <:< typeOf[immutable.Seq[_]] || tpe <:< typeOf[immutable.Set[_]]) withNullValueFor(tpe) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[immutable.Map[_, _]]) withNullValueFor(tpe) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[collection.Map[_, _]]) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[Iterable[_]]) {
          q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[Iterator[_]]) {
          if (isScala213) q"${scalaCollectionCompanion(tpe)}.empty[${typeArg1(tpe)}]"
          else q"${scalaCollectionCompanion(tpe)}.empty"
        } else if (tpe <:< typeOf[Array[_]]) withNullValueFor(tpe)(q"new _root_.scala.Array[${typeArg1(tpe)}](0)")
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
        } else if (tpe.typeSymbol.isModuleClass) q"${tpe.typeSymbol.asClass.module}"
        else if (isValueClass(tpe)) q"new $tpe(${genNullValue(valueClassValueType(tpe) :: types)})"
        else if (tpe <:< typeOf[AnyRef]) q"null"
        else q"null.asInstanceOf[$tpe]"
      }

      def genReadNonAbstractScalaClass(types: List[Type], discriminator: Tree): Tree = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        val fields = classInfo.fields
        val mappedNames = fields.map(_.mappedName)
        checkFieldNameCollisions(tpe, {
          if (discriminator.isEmpty) mappedNames
          else cfg.discriminatorFieldName.fold(mappedNames)(mappedNames :+ _)
        })
        val required: Set[String] = fields.collect {
          case fieldInfo if !(!cfg.requireDefaultFields && fieldInfo.symbol.isParamWithDefault || isOption(fieldInfo.resolvedTpe, types) ||
            (!cfg.requireCollectionFields && isCollection(fieldInfo.resolvedTpe))) => fieldInfo.mappedName
        }.toSet
        val paramVarNum = fields.size
        val lastParamVarIndex = Math.max(0, (paramVarNum - 1) >> 5)
        val lastParamVarBits = -1 >>> -paramVarNum
        val paramVarNames = (0 to lastParamVarIndex).map(i => TermName("p" + i))
        val checkAndResetFieldPresenceFlags = fields.map {
          var i = -1
          fieldInfo =>
            i += 1
            val n = paramVarNames(i >> 5)
            val m = 1 << i
            val nm = ~m
            (fieldInfo.mappedName, {
              if (cfg.checkFieldDuplication) {
                q"""if (($n & $m) == 0) in.duplicatedKeyError(l)
                    $n ^= $m"""
              } else if (required(fieldInfo.mappedName)) q"$n &= $nm"
              else EmptyTree
            })
        }.toMap
        val paramVars =
          if (required.isEmpty && !cfg.checkFieldDuplication) Nil
          else paramVarNames.init.map(n => q"var $n = -1") :+ q"var ${paramVarNames.last} = $lastParamVarBits"
        val checkReqVars =
          if (required.isEmpty) Nil
          else {
            val names = withFieldsFor(tpe)(mappedNames)
            val reqMasks = fields.grouped(32).toArray.map(_.foldLeft(0) {
              var i = -1
              (acc, fieldInfo) =>
                i += 1
                if (required(fieldInfo.mappedName)) acc | 1 << i
                else acc
            })
            paramVarNames.map {
              var i = -1
              n =>
                i += 1
                val m = reqMasks(i)
                if (m == -1 || (i == lastParamVarIndex && m == lastParamVarBits)) {
                  val fieldName =
                    if (i == 0) q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n))"
                    else q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n) + ${i << 5})"
                  q"if ($n != 0) in.requiredFieldError($fieldName)"
                } else {
                  val fieldName =
                    if (i == 0) q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n & $m))"
                    else q"$names(_root_.java.lang.Integer.numberOfTrailingZeros($n & $m) + ${i << 5})"
                  q"if (($n & $m) != 0) in.requiredFieldError($fieldName)"
                }
            }
          }
        val construct = q"new $tpe(...${classInfo.paramLists.map(_.map(fieldInfo => q"${fieldInfo.symbol.name} = ${fieldInfo.tmpName}"))})"
        val readVars = fields.map { fieldInfo =>
          val fTpe = fieldInfo.resolvedTpe
          q"var ${fieldInfo.tmpName}: $fTpe = ${fieldInfo.defaultValue.getOrElse(genNullValue(fTpe :: types))}"
        }
        val readFields =
          if (discriminator.isEmpty) fields
          else cfg.discriminatorFieldName.fold(fields)(n => fields :+ FieldInfo(null, n, null, null, null, null, isStringified = true))

        def genReadCollisions(fs: collection.Seq[FieldInfo]): Tree =
          fs.foldRight(unexpectedFieldHandler) { (fieldInfo, acc) =>
            val readValue =
              if (discriminator.nonEmpty && cfg.discriminatorFieldName.contains(fieldInfo.mappedName)) discriminator
              else {
                val fTpe = fieldInfo.resolvedTpe
                q"""${checkAndResetFieldPresenceFlags(fieldInfo.mappedName)}
                    ${fieldInfo.tmpName} = ${genReadVal(fTpe :: types, q"${fieldInfo.tmpName}", fieldInfo.isStringified, EmptyTree)}"""
              }
            q"if (in.isCharBufEqualsTo(l, ${fieldInfo.mappedName})) $readValue else $acc"
          }

        val readFieldsBlock =
          if (readFields.size <= 8 && readFields.foldLeft(0)(_ + _.mappedName.length) <= 64) {
            genReadCollisions(readFields)
          } else {
            val hashCode = (fieldInfo: FieldInfo) => JsonReader.toHashCode(fieldInfo.mappedName.toCharArray, fieldInfo.mappedName.length)
            val cases = groupByOrdered(readFields)(hashCode).map { case (hash, fs) =>
              cq"$hash => ${genReadCollisions(fs)}"
            } :+ cq"_ => $unexpectedFieldHandler"
            q"""(in.charBufToHashCode(l): @_root_.scala.annotation.switch) match {
                  case ..$cases
                }"""
          }
        val discriminatorVar =
          if (discriminator.isEmpty || !cfg.checkFieldDuplication) EmptyTree
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
          else q"""if (in.readBoolean() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Byte)) =>
          if (isStringified) q"""if (in.readStringAsByte() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readByte() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Char)) =>
          q"""if (in.readChar() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
        case ConstantType(Constant(v: Short)) =>
          if (isStringified) q"""if (in.readStringAsShort() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readShort() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Int)) =>
          if (isStringified) q"""if (in.readStringAsInt() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readInt() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Long)) =>
          if (isStringified) q"""if (in.readStringAsLong() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readLong() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Float)) =>
          if (isStringified) q"""if (in.readStringAsFloat() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readFloat() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case ConstantType(Constant(v: Double)) =>
          if (isStringified) q"""if (in.readStringAsDouble() != $v) in.decodeError(${"expected value: \"" + v + '"'}); $v"""
          else q"""if (in.readDouble() != $v) in.decodeError(${s"expected value: $v"}); $v"""
        case _ => cannotFindValueCodecError(tpe)
      }

      def genReadValForGrowable(types: List[Type], isStringified: Boolean): Tree =
          if (isScala213) q"x.addOne(${genReadVal(types, genNullValue(types), isStringified, EmptyTree)})"
          else q"x += ${genReadVal(types, genNullValue(types), isStringified, EmptyTree)}"

      def genReadVal(types: List[Type], default: Tree, isStringified: Boolean, discriminator: Tree): Tree = {
        val tpe = types.head
        val implValueCodec = findImplicitValueCodec(types)
        val methodKey = MethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe, types.tail)), discriminator)
        val decodeMethodName = decodeMethodNames.get(methodKey)
        if (implValueCodec.nonEmpty) q"$implValueCodec.decodeValue(in, $default)"
        else if (decodeMethodName.isDefined) q"${decodeMethodName.get}(in, $default)"
        else if (tpe =:= typeOf[String]) q"in.readString($default)"
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
        } else if (tpe =:= typeOf[java.util.UUID]) q"in.readUUID($default)"
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
          q"new $tpe(${genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)})"
        } else if (isOption(tpe, types.tail)) {
          val tpe1 = typeArg1(tpe)
          val nullValue =
            if (cfg.skipNestedOptionValues && tpe <:< typeOf[Option[Option[_]]]) q"new _root_.scala.Some(_root_.scala.None)"
            else default
          q"""if (in.isNextToken('n')) in.readNullOrError($nullValue, "expected value or null")
              else {
                in.rollbackToken()
                new _root_.scala.Some(${genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)})
              }"""
        } else if (tpe <:< typeOf[Array[_]] || isImmutableArraySeq(tpe) ||
          isMutableArraySeq(tpe)) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val growArray =
            if (tpe1.typeArgs.nonEmpty || isValueClass(tpe1)) {
              q"""l <<= 1
                  val x1 = new Array[$tpe1](l)
                  _root_.java.lang.System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else {
              q"""l <<= 1
                  _root_.java.util.Arrays.copyOf(x, l)"""
            }
          val shrinkArray =
            if (tpe1.typeArgs.nonEmpty || isValueClass(tpe1)) {
              q"""val x1 = new Array[$tpe1](i)
                  _root_.java.lang.System.arraycopy(x, 0, x1, 0, i)
                  x1"""
            } else q"_root_.java.util.Arrays.copyOf(x, i)"
          genReadArray(
            q"""var l = 8
                var x = new Array[$tpe1](l)
                var i = 0""",
            q"""if (i == l) x = $growArray
                x(i) = ${genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)}
                i += 1""",
            {
              if (isImmutableArraySeq(tpe)) {
                q"""if (i != l) x = $shrinkArray
                    _root_.scala.collection.immutable.ArraySeq.unsafeWrapArray[$tpe1](x)"""
              } else if (isMutableArraySeq(tpe)) {
                q"""if (i != l) x = $shrinkArray
                    _root_.scala.collection.mutable.ArraySeq.make[$tpe1](x)"""
              } else {
                q"""if (i != l) x = $shrinkArray
                    x"""
              }
            })
        } else if (tpe <:< typeOf[immutable.IntMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val newBuilder = q"var x = ${withNullValueFor(tpe)(q"${scalaCollectionCompanion(tpe)}.empty[$tpe1]")}"
          val readVal = genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readKey =
              if (cfg.isStringified) q"in.readStringAsInt()"
              else q"in.readInt()"
            genReadMapAsArray(newBuilder,
              q"x = x.updated($readKey, { if (in.isNextToken(',')) $readVal else in.commaError() })")
          } else genReadMap(newBuilder, q"x = x.updated(in.readKeyAsInt(), $readVal)")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val newBuilder = q"{ val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1] }"
          val readVal = genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)
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
          val readVal = genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readKey =
              if (cfg.isStringified) q"in.readStringAsLong()"
              else q"in.readLong()"
            genReadMapAsArray(newBuilder,
              q"x = x.updated($readKey, { if (in.isNextToken(',')) $readVal else in.commaError() })")
          } else genReadMap(newBuilder, q"x = x.updated(in.readKeyAsLong(), $readVal)")
        } else if (tpe <:< typeOf[mutable.Map[_, _]] || isCollisionProofHashMap(tpe)) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val newBuilder = q"{ val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1, $tpe2] }"
          val readVal2 = genReadVal(tpe2 :: types, genNullValue(tpe2 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readVal1 = genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)
            genReadMapAsArray(newBuilder, q"x.update($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() })")
          } else genReadMap(newBuilder, q"x.update(${genReadKey(tpe1 :: types)}, $readVal2)")
        } else if (tpe <:< typeOf[collection.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val newBuilder = q"{ val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1, $tpe2] }"
          val readVal2 = genReadVal(tpe2 :: types, genNullValue(tpe2 :: types), isStringified, EmptyTree)
          if (cfg.mapAsArray) {
            val readVal1 = genReadVal(tpe1 :: types, genNullValue(tpe1 :: types), isStringified, EmptyTree)
            val readKV =
              if (isScala213) q"x.addOne(new _root_.scala.Tuple2($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() }))"
              else q"x += new _root_.scala.Tuple2($readVal1, { if (in.isNextToken(',')) $readVal2 else in.commaError() })"
            genReadMapAsArray(newBuilder, readKV, q"x.result()")
          } else {
            val readKey = genReadKey(tpe1 :: types)
            val readKV =
              if (isScala213) q"x.addOne(new _root_.scala.Tuple2($readKey, $readVal2))"
              else q"x += new _root_.scala.Tuple2($readKey, $readVal2)"
            genReadMap(newBuilder, readKV, q"x.result()")
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
                x(i) = x(i) | 1L << v""",
            if (tpe <:< typeOf[mutable.BitSet]) q"_root_.scala.collection.mutable.BitSet.fromBitMaskNoCopy(x)"
            else q"_root_.scala.collection.immutable.BitSet.fromBitMaskNoCopy(x)")
        } else if (tpe <:< typeOf[mutable.Set[_] with mutable.Builder[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"{ val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1] }",
            genReadValForGrowable(tpe1 :: types, isStringified))
        } else if (tpe <:< typeOf[collection.Set[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadSet(q"{ val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1] }",
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
        } else if (tpe <:< typeOf[List[_]] || tpe.typeSymbol == typeOf[Seq[_]].typeSymbol) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"{ val x = new _root_.scala.collection.mutable.ListBuffer[$tpe1] }",
            genReadValForGrowable(tpe1 :: types, isStringified), q"x.toList")
        } else if (tpe <:< typeOf[mutable.Iterable[_] with mutable.Builder[_, _]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { //ArrayStack uses 'push' for '+=' in Scala 2.12.x
          val tpe1 = typeArg1(tpe)
          genReadArray(q"{ val x = if (default.isEmpty) default else ${scalaCollectionCompanion(tpe)}.empty[$tpe1] }",
            genReadValForGrowable(tpe1 :: types, isStringified))
        } else if (tpe <:< typeOf[Iterable[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"{ val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1] }",
            genReadValForGrowable(tpe1 :: types, isStringified), q"x.result()")
        } else if (tpe <:< typeOf[Iterator[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray({
            if (isScala213) q"{ val x = ${scalaCollectionCompanion(tpe)}.newBuilder[$tpe1] }"
            else q"{ val x = Seq.newBuilder[$tpe1] }"
          }, genReadValForGrowable(tpe1 :: types, isStringified), {
            if (isScala213) q"x.result()"
            else q"x.result().iterator"
          })
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
        } else if (isTuple(tpe)) withDecoderFor(methodKey, default) {
          val indexedTypes = tpe.typeArgs
          val readFields = indexedTypes.tail.foldLeft[Tree] {
            val t = typeArg1(tpe)
            q"val _1: $t = ${genReadVal(t :: types, genNullValue(t :: types), isStringified, EmptyTree)}"
          }{
            var i = 1
            (acc, ta) =>
              i += 1
              val t = ta.dealias
              q"""..$acc
                  val ${TermName("_" + i)}: $t =
                    if (in.isNextToken(',')) ${genReadVal(t :: types, genNullValue(t :: types), isStringified, EmptyTree)}
                    else in.commaError()"""
          }
          val params = (1 to indexedTypes.length).map(i => TermName("_" + i))
          q"""if (in.isNextToken('[')) {
                ..$readFields
                if (in.isNextToken(']')) new $tpe(..$params)
                else in.arrayEndError()
              } else in.readNullOrTokenError(default, '[')"""
        } else if (tpe.typeSymbol.isModuleClass) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('{')) {
                in.rollbackToken()
                in.skip()
                ${tpe.typeSymbol.asClass.module}
              } else in.readNullOrTokenError(default, '{')"""
        } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default) {
          val leafClasses = adtLeafClasses(tpe)
          val discriminatorError = cfg.discriminatorFieldName
            .fold(q"in.discriminatorError()")(n => q"in.discriminatorValueError($n)")

          def genReadLeafClass(subTpe: Type): Tree =
            if (subTpe =:= tpe) genReadNonAbstractScalaClass(types, skipDiscriminatorField)
            else genReadVal(subTpe :: types, genNullValue(subTpe :: types), isStringified, skipDiscriminatorField)

          def genReadCollisions(subTpes: collection.Seq[Type]): Tree =
            subTpes.foldRight(discriminatorError) { (subTpe, acc) =>
              val readVal =
                if (cfg.discriminatorFieldName.isDefined) {
                  q"""in.rollbackToMark()
                      ..${genReadLeafClass(subTpe)}"""
                } else if (!cfg.circeLikeObjectEncoding && subTpe.typeSymbol.isModuleClass) {
                  q"${subTpe.typeSymbol.asClass.module}"
                } else genReadLeafClass(subTpe)
              q"if (in.isCharBufEqualsTo(l, ${discriminatorValue(subTpe)})) $readVal else $acc"
            }

          def genReadSubclassesBlock(leafClasses: collection.Seq[Type]): Tree =
            if (leafClasses.size <= 8 && leafClasses.foldLeft(0)(_ + discriminatorValue(_).length) <= 64) {
              genReadCollisions(leafClasses)
            } else {
              val hashCode = (t: Type) => {
                val cs = discriminatorValue(t).toCharArray
                JsonReader.toHashCode(cs, cs.length)
              }
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
              val (leafModuleClasses, leafCaseClasses) =
                leafClasses.partition(!cfg.circeLikeObjectEncoding && _.typeSymbol.isModuleClass)
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
                    } else {
                      val m =
                        if (default == null) "expected '\"' or '{'"
                        else "expected '\"' or '{' or null"
                      in.readNullOrError(default, m)
                    }"""
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
                      if (in.isCharBufEqualsTo(in.readKeyAsCharBuf(), $discrFieldName)) {
                        val l = in.readStringAsCharBuf()
                        ..${genReadSubclassesBlock(leafClasses)}
                      } else in.decodeError(${"expected key: \"" + discrFieldName + '"'})
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
        val writeFields = classInfo.fields.map { fieldInfo =>
          val fTpe = fieldInfo.resolvedTpe
          (if (cfg.transientDefault) fieldInfo.defaultValue
          else None) match {
            case Some(d) =>
              if (cfg.transientEmpty && fTpe <:< typeOf[Iterable[_]]) {
                q"""val v = x.${fieldInfo.getter}
                    if (!v.isEmpty && v != $d) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (cfg.transientEmpty && fTpe <:< typeOf[Iterator[_]]) {
                q"""val v = x.${fieldInfo.getter}
                    if (v.hasNext && v != $d) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (cfg.transientNone && isOption(fTpe, types)) {
                q"""val v = x.${fieldInfo.getter}
                    if ((v ne _root_.scala.None) && v != $d) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v.get", typeArg1(fTpe) :: fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (fTpe <:< typeOf[Array[_]]) {
                val cond =
                  if (cfg.transientEmpty) {
                    q"v.length > 0 && !${withEqualsFor(fTpe, q"v", d)(genArrayEquals(fTpe))}"
                  } else q"!${withEqualsFor(fTpe, q"v", d)(genArrayEquals(fTpe))}"
                q"""val v = x.${fieldInfo.getter}
                    if ($cond) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else {
                q"""val v = x.${fieldInfo.getter}
                    if (v != $d) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              }
            case None =>
              if (cfg.transientEmpty && fTpe <:< typeOf[Iterable[_]]) {
                q"""val v = x.${fieldInfo.getter}
                    if (!v.isEmpty) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (cfg.transientEmpty && fTpe <:< typeOf[Iterator[_]]) {
                q"""val v = x.${fieldInfo.getter}
                    if (v.hasNext) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (cfg.transientNone && isOption(fTpe, types)) {
                q"""val v = x.${fieldInfo.getter}
                    if (v ne _root_.scala.None) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v.get", typeArg1(fTpe) :: fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else if (cfg.transientEmpty && fTpe <:< typeOf[Array[_]]) {
                q"""val v = x.${fieldInfo.getter}
                    if (v.length > 0) {
                      ..${genWriteConstantKey(fieldInfo.mappedName)}
                      ..${genWriteVal(q"v", fTpe :: types, fieldInfo.isStringified, EmptyTree)}
                    }"""
              } else {
                q"""..${genWriteConstantKey(fieldInfo.mappedName)}
                    ..${genWriteVal(q"x.${fieldInfo.getter}", fTpe :: types, fieldInfo.isStringified, EmptyTree)}"""
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
        case ConstantType(Constant(s: String)) => genWriteConstantVal(s)
        case ConstantType(Constant(_: Char)) => q"out.writeVal($m)"
        case ConstantType(Constant(_: Boolean)) | ConstantType(Constant(_: Byte)) | ConstantType(Constant(_: Short)) |
          ConstantType(Constant(_: Int)) | ConstantType(Constant(_: Long)) | ConstantType(Constant(_: Float)) |
          ConstantType(Constant(_: Double)) =>
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        case _ => cannotFindValueCodecError(tpe)
      }

      def genWriteVal(m: Tree, types: List[Type], isStringified: Boolean, discriminator: Tree): Tree = {
        val tpe = types.head
        val implValueCodec = findImplicitValueCodec(types)
        val methodKey = MethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe, types.tail)), discriminator)
        val encodeMethodName = encodeMethodNames.get(methodKey)
        if (implValueCodec.nonEmpty) q"$implValueCodec.encodeValue($m, out)"
        else if (encodeMethodName.isDefined) q"${encodeMethodName.get}($m, out)"
        else if (tpe =:= typeOf[String]) q"out.writeVal($m)"
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] || tpe =:= definitions.ByteTpe ||
          tpe =:= typeOf[java.lang.Byte] || tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] || tpe =:= definitions.LongTpe ||
          tpe =:= typeOf[java.lang.Long] || tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double] || tpe =:= typeOf[BigInt] ||
          tpe =:= typeOf[BigDecimal]) {
          if (isStringified) q"out.writeValAsString($m)"
          else q"out.writeVal($m)"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] ||
          tpe =:= typeOf[java.util.UUID] || tpe =:= typeOf[Duration] || tpe =:= typeOf[Instant] ||
          tpe =:= typeOf[LocalDate] || tpe =:= typeOf[LocalDateTime] || tpe =:= typeOf[LocalTime] ||
          tpe =:= typeOf[MonthDay] || tpe =:= typeOf[OffsetDateTime] || tpe =:= typeOf[OffsetTime] ||
          tpe =:= typeOf[Period] || tpe =:= typeOf[Year] || tpe =:= typeOf[YearMonth] ||
          tpe =:= typeOf[ZonedDateTime] || tpe =:= typeOf[ZoneId] || tpe =:= typeOf[ZoneOffset]) q"out.writeVal($m)"
        else if (isValueClass(tpe)) {
          genWriteVal(q"$m.${valueClassValueMethod(tpe)}", valueClassValueType(tpe) :: types, isStringified, EmptyTree)
        } else if (isOption(tpe, types.tail)) {
          q"""if ($m ne _root_.scala.None) ${genWriteVal(q"$m.get", typeArg1(tpe) :: types, isStringified, EmptyTree)}
              else out.writeNull()"""
        } else if (tpe <:< typeOf[Array[_]] || isImmutableArraySeq(tpe) ||
          isMutableArraySeq(tpe)) withEncoderFor(methodKey, m) {
          val tpe1 = typeArg1(tpe)
          if (isImmutableArraySeq(tpe)) {
            q"""out.writeArrayStart()
                val xs = x.unsafeArray.asInstanceOf[Array[$tpe1]]
                val l = xs.length
                var i = 0
                while (i < l) {
                  ..${genWriteVal(q"xs(i)", tpe1 :: types, isStringified, EmptyTree)}
                  i += 1
                }
                out.writeArrayEnd()"""
          } else if (isMutableArraySeq(tpe)) {
            q"""out.writeArrayStart()
                val xs = x.array.asInstanceOf[Array[$tpe1]]
                val l = xs.length
                var i = 0
                while (i < l) {
                  ..${genWriteVal(q"xs(i)", tpe1 :: types, isStringified, EmptyTree)}
                  i += 1
                }
                out.writeArrayEnd()"""
          } else {
            q"""out.writeArrayStart()
                val l = x.length
                var i = 0
                while (i < l) {
                  ..${genWriteVal(q"x(i)", tpe1 :: types, isStringified, EmptyTree)}
                  i += 1
                }
                out.writeArrayEnd()"""
          }
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
        } else if (tpe <:< typeOf[collection.Map[_, _]] || isCollisionProofHashMap(tpe)) withEncoderFor(methodKey, m) {
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
              val n = _root_.scala.Nil
              var l: _root_.scala.collection.immutable.List[$tpe1] = x
              while (l ne n) {
                ..${genWriteVal(q"l.head", tpe1 :: types, isStringified, EmptyTree)}
                l = l.tail
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[IndexedSeq[_]]) withEncoderFor(methodKey, m) {
          q"""out.writeArrayStart()
              val l = x.length
              if (l <= 32) {
                var i = 0
                while (i < l) {
                  ..${genWriteVal(q"x(i)", typeArg1(tpe) :: types, isStringified, EmptyTree)}
                  i += 1
                }
              } else {
                x.foreach { x =>
                  ..${genWriteVal(q"x", typeArg1(tpe) :: types, isStringified, EmptyTree)}
                }
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Iterable[_]]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe) :: types, isStringified, EmptyTree))
        } else if (tpe <:< typeOf[Iterator[_]]) withEncoderFor(methodKey, m) {
          genWriteArray2(q"x", genWriteVal(q"x.next()", typeArg1(tpe) :: types, isStringified, EmptyTree))
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
            if (encodingRequired) q"out.writeVal(x match { case ..$cases })"
            else q"out.writeNonEscapedAsciiVal(x match { case ..$cases })"
          } else {
            if (encodingRequired) q"out.writeVal(x.name)"
            else q"out.writeNonEscapedAsciiVal(x.name)"
          }
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m) {
          val writeFields = tpe.typeArgs.map {
            var i = 0
            ta =>
              i += 1
              genWriteVal(q"x.${TermName("_" + i)}", ta.dealias :: types, isStringified, EmptyTree)
          }
          q"""out.writeArrayStart()
              ..$writeFields
              out.writeArrayEnd()"""
        } else if (tpe.typeSymbol.isModuleClass && !(cfg.alwaysEmitDiscriminator && hasSealedParent(tpe))) withEncoderFor(methodKey, m) {
          q"""out.writeObjectStart()
              ..$discriminator
              out.writeObjectEnd()"""
        } else if (isSealedClass(tpe) || (cfg.alwaysEmitDiscriminator && hasSealedParent(tpe))) withEncoderFor(methodKey, m) {
          def genWriteLeafClass(subTpe: Type, discriminator: Tree): Tree =
            if (subTpe != tpe) genWriteVal(q"x", subTpe :: types, isStringified, discriminator)
            else genWriteNonAbstractScalaClass(types, discriminator)

          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = cfg.discriminatorFieldName.fold {
            leafClasses.map { subTpe =>
              if (!cfg.circeLikeObjectEncoding && subTpe.typeSymbol.isModuleClass) {
                cq"x: $subTpe => ${genWriteConstantVal(discriminatorValue(subTpe))}"
              } else {
                cq"""x: $subTpe =>
                     out.writeObjectStart()
                     ${genWriteConstantKey(discriminatorValue(subTpe))}
                     ${genWriteLeafClass(subTpe, EmptyTree)}
                     out.writeObjectEnd()"""
              }
            }
          } { discrFieldName =>
              leafClasses.map { subTpe =>
                val writeDiscriminatorField =
                  q"""..${genWriteConstantKey(discrFieldName)}
                      ..${genWriteConstantVal(discriminatorValue(subTpe))}"""
                cq"x: $subTpe => ${genWriteLeafClass(subTpe, writeDiscriminatorField)}"
              }
          }
          q"""x match {
                case ..$writeSubclasses
              }"""
        } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m) {
          genWriteNonAbstractScalaClass(types, discriminator)
        } else if (isConstType(tpe)) getWriteConstType(tpe, m, isStringified)
        else cannotFindValueCodecError(tpe)
      }

      val codec =
        q"""{
              @_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.All"))
              val x = new _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec[$rootTpe] {
                @inline
                def nullValue: $rootTpe = ${genNullValue(rootTpe :: Nil)}

                @inline
                def decodeValue(in: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonReader, default: $rootTpe): $rootTpe = ${
                  if (cfg.encodingOnly) q"???"
                  else genReadVal(rootTpe :: Nil, q"default", cfg.isStringified, EmptyTree)
                }

                @inline
                def encodeValue(x: $rootTpe, out: _root_.com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter): _root_.scala.Unit = ${
                  if (cfg.decodingOnly) q"???"
                  else genWriteVal(q"x", rootTpe :: Nil, cfg.isStringified, EmptyTree)
                }
                ..$decodeMethodTrees
                ..$encodeMethodTrees
                ..${fields.values.map(_._2)}
                ..${equalsMethods.values.map(_._2)}
                ..${nullValues.values.map(_._2)}
                ..${mathContexts.values.map(_._2)}
                ..${scalaEnumCaches.values.map(_._2)}
              }
              x
            }"""
      if (c.settings.contains("print-codecs") ||
        inferImplicitValue(tq"_root_.com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig.PrintCodec") != EmptyTree) {
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
    xs.foldLeft(new mutable.LinkedHashMap[K, ArrayBuffer[A]]) { (m, x) =>
      m.getOrElseUpdate(f(x), new ArrayBuffer[A]) += x
      m
    }.toSeq

  private[this] def duplicated[A](xs: collection.Seq[A]): collection.Seq[A] = xs.filter {
    val seen = new mutable.HashSet[A]
    x => !seen.add(x)
  }
}
