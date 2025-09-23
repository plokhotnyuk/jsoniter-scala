package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CompileTimeEval._
import scala.annotation._
import scala.annotation.meta.field
import scala.collection.{BitSet, immutable, mutable}
import scala.collection.mutable.Growable
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.quoted._
import scala.reflect.ClassTag

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
  *                               allow using `Option[Option[?]]` field values to distinguish `null` and missing field
  *                               cases
  * @param circeLikeObjectEncoding a flag that turns on serialization and parsing of Scala objects as JSON objects with
  *                               a key and empty object value: `{"EnumValue":{}}`
  * @param decodingOnly           a flag that turns generation of decoding implementation only (turned off by default)
  * @param encodingOnly           a flag that turns generation of encoding implementation only (turned off by default)
  * @param requireDefaultFields   a flag that turns on checking of presence of fields with default values and forces
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
  * @param transientNull          a flag that turns on skipping serialization of fields that have null values of
  *                               objects (turned off by default)
  */
class CodecMakerConfig private[macros] (
    val fieldNameMapper: NameMapper,
    val javaEnumValueNameMapper: NameMapper,
    val adtLeafClassNameMapper: NameMapper,
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
    val alwaysEmitDiscriminator: Boolean,
    val transientNull: Boolean) {
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

  def withTransientNull(transientNull: Boolean): CodecMakerConfig = copy(transientNull = transientNull)

  private def copy(fieldNameMapper: NameMapper = fieldNameMapper,
                   javaEnumValueNameMapper: NameMapper = javaEnumValueNameMapper,
                   adtLeafClassNameMapper: NameMapper = adtLeafClassNameMapper,
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
                   alwaysEmitDiscriminator: Boolean = alwaysEmitDiscriminator,
                   transientNull: Boolean = transientNull): CodecMakerConfig =
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
      alwaysEmitDiscriminator = alwaysEmitDiscriminator,
      transientNull = transientNull)
}

object CodecMakerConfig extends CodecMakerConfig(
  fieldNameMapper = JsonCodecMaker.partialIdentity,
  javaEnumValueNameMapper = JsonCodecMaker.partialIdentity,
  adtLeafClassNameMapper = JsonCodecMaker.simpleClassName,
  discriminatorFieldName = new Some("type"),
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
  alwaysEmitDiscriminator = false,
  transientNull = false) {

  /**
    * Use to enable printing of codec during compilation:
    * {{{
    * given CodecMakerConfig.PrintCodec with {}
    * val codec = JsonCodecMaker.make[MyClass]
    * }}}
    **/
  class PrintCodec

  private[macros] given FromExpr[CodecMakerConfig] with {
    def unapply(x: Expr[CodecMakerConfig])(using Quotes): Option[CodecMakerConfig] = {
      import quotes.reflect._

      x match
        case '{ CodecMakerConfig } => new Some(CodecMakerConfig)
        case '{ ($x: CodecMakerConfig).withDiscriminatorFieldName($v) } => new Some(x.valueOrAbort.withDiscriminatorFieldName(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withFieldNameMapper($v) } => new Some(x.valueOrAbort.copy(fieldNameMapper = ExprPartialFunctionWrapper(v)))
        case '{ ($x: CodecMakerConfig).withJavaEnumValueNameMapper($v) } => new Some(x.valueOrAbort.copy(javaEnumValueNameMapper = ExprPartialFunctionWrapper(v)))
        case '{ ($x: CodecMakerConfig).withAdtLeafClassNameMapper($v) } => new Some(x.valueOrAbort.copy(adtLeafClassNameMapper = ExprPartialFunctionWrapper('{ { case x => $v(x) } })))
        case '{ ($x: CodecMakerConfig).withAllowRecursiveTypes($v) } => new Some(x.valueOrAbort.withAllowRecursiveTypes(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withRequireDiscriminatorFirst($v) } => new Some(x.valueOrAbort.copy(requireDiscriminatorFirst = v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withCheckFieldDuplication($v) } => new Some(x.valueOrAbort.withCheckFieldDuplication(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withInlineOneValueClasses($v) } => new Some(x.valueOrAbort.withInlineOneValueClasses(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withUseScalaEnumValueId($v) } => new Some(x.valueOrAbort.withUseScalaEnumValueId(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withIsStringified($v) } => new Some(x.valueOrAbort.withIsStringified(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withDecodingOnly($v) } => new Some(x.valueOrAbort.withDecodingOnly(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withEncodingOnly($v) } => new Some(x.valueOrAbort.withEncodingOnly(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSkipUnexpectedFields($v) } => new Some(x.valueOrAbort.withSkipUnexpectedFields(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientDefault($v) } => new Some(x.valueOrAbort.withTransientDefault(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientEmpty($v) } => new Some(x.valueOrAbort.withTransientEmpty(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientNone($v) } => new Some(x.valueOrAbort.withTransientNone(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withRequireCollectionFields($v) } => new Some(x.valueOrAbort.withRequireCollectionFields(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withRequireDefaultFields($v) } => new Some(x.valueOrAbort.withRequireDefaultFields(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withScalaTransientSupport($v) } => new Some(x.valueOrAbort.withScalaTransientSupport(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withAlwaysEmitDiscriminator($v) } => new Some(x.valueOrAbort.withAlwaysEmitDiscriminator(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withMapAsArray($v) } => new Some(x.valueOrAbort.withMapAsArray(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalPrecision($v) } => new Some(x.valueOrAbort.withBigDecimalPrecision(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalScaleLimit($v) } => new Some(x.valueOrAbort.withBigDecimalScaleLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalDigitsLimit($v) } => new Some(x.valueOrAbort.withBigDecimalDigitsLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigIntDigitsLimit($v) } => new Some(x.valueOrAbort.copy(bigIntDigitsLimit = v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBitSetValueLimit($v) } => new Some(x.valueOrAbort.withBitSetValueLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withMapMaxInsertNumber($v) } => new Some(x.valueOrAbort.withMapMaxInsertNumber(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSetMaxInsertNumber($v) } => new Some(x.valueOrAbort.withSetMaxInsertNumber(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSkipNestedOptionValues($v) } => new Some(x.valueOrAbort.withSkipNestedOptionValues(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withCirceLikeObjectEncoding($v) } => new Some(x.valueOrAbort.withCirceLikeObjectEncoding(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientNull($v) } => new Some(x.valueOrAbort.withTransientNull(v.valueOrAbort))
        case other =>
          report.error(s"Can't interpret ${other.show} as a constant expression, tree=$other")
          None
    }
  }
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

  private def enforceCamelOrPascalCase(s: String, toPascal: Boolean): String =
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

  private def enforceSnakeOrKebabCaseWithSeparatedNonAlphabetic(s: String, separator: Char): String = {
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

  private def enforceSnakeOrKebabCaseWithJoinedNonAphabetic(s: String, separator: Char): String = {
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
    val lastComponent = fullClassName.substring(Math.max(fullClassName.lastIndexOf('.') + 1, 0))
    var localPrefixIndex = 0
    while (lastComponent.startsWith("_$", localPrefixIndex)) localPrefixIndex += 2
    lastComponent.substring(localPrefixIndex)

  /**
    * Derives a codec for JSON values for the specified type `A`.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def make[A]: JsonValueCodec[A] = ${Impl.makeWithDefaultConfig}

  /**
    * A replacement for the `make` call with the `CodecMakerConfig.withDiscriminatorFieldName(None)` configuration
    * parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithoutDiscriminator[A]: JsonValueCodec[A] = ${Impl.makeWithoutDiscriminator}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = ${Impl.makeWithRequiredCollectionFields}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name"))`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] =
    ${Impl.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName}

  /**
   * A replacement for the `make` call with the
   * `CodecMakerConfig.withTransientDefault(false).withRequireDefaultFields(true)`
   * configuration parameter.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  inline def makeWithRequiredDefaultFields[A]: JsonValueCodec[A] = ${Impl.makeWithRequiredDefaultFields}

  /**
   * A replacement for the `make` call with the
   * `CodecMakerConfig.withSkipNestedOptionValues(true)`
   * configuration parameter.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  inline def makeWithSkipNestedOptionValues[A]: JsonValueCodec[A] = ${Impl.makeWithSkipNestedOptionValues}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false).withDiscriminatorFieldName(None)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeCirceLike[A]: JsonValueCodec[A] = ${Impl.makeCirceLike}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withTransientNone(false).withDiscriminatorFieldName(None).withAdtLeafClassNameMapper(x => enforce_snake_case(simpleClassName(x))).withFieldNameMapper(enforce_snake_case).withJavaEnumValueNameMapper(enforce_snake_case)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeCirceLikeSnakeCased[A]: JsonValueCodec[A] = ${Impl.makeCirceLikeSnakeCased}

    /**
   * Replacements for the `make` call preconfigured to behave as expected by openapi specifications:
   * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withRequireCollectionFields(true).withAllowRecursiveTypes(true)`
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  inline def makeOpenapiLike[A]: JsonValueCodec[A] = ${Impl.makeOpenapiLike}

  /**
   * Replacements for the `make` call preconfigured to behave as expected by openapi specifications:
   * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withRequireCollectionFields(true).withAllowRecursiveTypes(true)`
   * with a privided discriminator field name.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @param discriminatorFieldName a name of discriminator field
   * @return an instance of the derived codec
   */
  inline def makeOpenapiLike[A](discriminatorFieldName: String): JsonValueCodec[A] =
    ${Impl.makeOpenapiLike('discriminatorFieldName)}

  /**
   * Replacements for the `make` call preconfigured to behave as expected by openapi specifications:
   * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withRequireCollectionFields(true).withAllowRecursiveTypes(true)`
   * with a privided discriminator field name and an ADT leaf-class name mapper with sequentionally applied function
   * that truncates to simple class name by default
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @param discriminatorFieldName a name of discriminator field
   * @param adtLeafClassNameMapper the function of mapping from string of case class/object full name to string value of
   *                               discriminator field
   * @return an instance of the derived codec
   */
  inline def makeOpenapiLike[A](discriminatorFieldName: String, inline adtLeafClassNameMapper: PartialFunction[String, String]): JsonValueCodec[A] =
    ${Impl.makeOpenapiLike('discriminatorFieldName, 'adtLeafClassNameMapper)}

  /**
   * Replacements for the `make` call preconfigured to behave as expected by openapi specifications:
   * `CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false).withRequireCollectionFields(true).withAllowRecursiveTypes(true).withDiscriminatorFieldName(scala.None))`
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  inline def makeOpenapiLikeWithoutDiscriminator[A]: JsonValueCodec[A] =
    ${Impl.makeOpenapiLikeWithoutDiscriminator}

  /**
    * Derives a codec for JSON values for the specified type `A` and a provided derivation configuration.
    *
    * @param config a derivation configuration
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def make[A](inline config: CodecMakerConfig): JsonValueCodec[A] = ${Impl.makeWithSpecifiedConfig('config)}

  private[macros] object Impl {
    def makeWithDefaultConfig[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig).make

    def makeWithoutDiscriminator[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withDiscriminatorFieldName(None)).make

    def makeWithRequiredCollectionFields[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)).make

    def makeWithRequiredDefaultFields[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientDefault(false).withRequireDefaultFields(true)).make

    def makeWithSkipNestedOptionValues[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withSkipNestedOptionValues(true)).make

    def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)
        .withDiscriminatorFieldName(Some("name"))).make

    def makeCirceLike[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false)
        .withTransientNone(false).withDiscriminatorFieldName(None).withCirceLikeObjectEncoding(true)).make

    def makeCirceLikeSnakeCased[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig(
        fieldNameMapper = enforce_snake_case,
        javaEnumValueNameMapper = enforce_snake_case,
        adtLeafClassNameMapper = (x: String) => enforce_snake_case(simpleClassName(x)),
        discriminatorFieldName = None,
        isStringified = false,
        mapAsArray = false,
        skipUnexpectedFields = true,
        transientDefault = false,
        transientEmpty = false,
        transientNone = false,
        transientNull = true,
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
        circeLikeObjectEncoding = true,
        decodingOnly = false,
        encodingOnly = false,
        requireDefaultFields = false,
        checkFieldDuplication = true,
        scalaTransientSupport = false,
        inlineOneValueClasses = false,
        alwaysEmitDiscriminator = false)).make

    def makeOpenapiLike[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false)
        .withRequireCollectionFields(true).withAllowRecursiveTypes(true)).make

    def makeOpenapiLike[A: Type](discriminatorFieldName: Expr[String])(using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false)
        .withRequireDiscriminatorFirst(false).withRequireCollectionFields(true).withAllowRecursiveTypes(true)
        .withDiscriminatorFieldName(Some(discriminatorFieldName.valueOrAbort))).make

    def makeOpenapiLike[A: Type](discriminatorFieldName: Expr[String],
                                 adtLeafClassNameMapper: Expr[PartialFunction[String, String]])(using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false)
        .withRequireCollectionFields(true).withAllowRecursiveTypes(true).withRequireDiscriminatorFirst(false)
        .withDiscriminatorFieldName(Some(discriminatorFieldName.valueOrAbort))
        .withAdtLeafClassNameMapper(ExprPartialFunctionWrapper(adtLeafClassNameMapper).apply.unlift
          .compose(PartialFunction.fromFunction(simpleClassName)))).make

    def makeOpenapiLikeWithoutDiscriminator[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      new JsonCodecMakerInstance(CodecMakerConfig.withTransientEmpty(false).withTransientDefault(false)
        .withRequireCollectionFields(true).withAllowRecursiveTypes(true)
        .withDiscriminatorFieldName(None)).make

    def makeWithSpecifiedConfig[A: Type](config: Expr[CodecMakerConfig])(using Quotes): Expr[JsonValueCodec[A]] = {
      import quotes.reflect._

      try new JsonCodecMakerInstance(summon[FromExpr[CodecMakerConfig]].unapply(config)
        .fold(report.errorAndAbort(s"Cannot evaluate a parameter of the 'make' macro call for type '${Type.show[A]}'. ")) {
          cfg =>
            if (cfg.requireCollectionFields && cfg.transientEmpty)
              report.errorAndAbort("'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously")
            if (cfg.requireDefaultFields && cfg.transientDefault)
              report.errorAndAbort("'requireDefaultFields' and 'transientDefault' cannot be 'true' simultaneously")
            if (cfg.circeLikeObjectEncoding && cfg.discriminatorFieldName.isDefined)
              report.errorAndAbort("'discriminatorFieldName' should be 'None' when 'circeLikeObjectEncoding' is 'true'")
            if (cfg.alwaysEmitDiscriminator && cfg.discriminatorFieldName.isEmpty)
              report.errorAndAbort("'discriminatorFieldName' should not be 'None' when 'alwaysEmitDiscriminator' is 'true'")
            if (cfg.decodingOnly && cfg.encodingOnly)
              report.errorAndAbort("'decodingOnly' and 'encodingOnly' cannot be 'true' simultaneously")
            cfg
        }).make[A] catch {
        case ex: CompileTimeEvalException => report.errorAndAbort(s"Can't evaluate compile-time expression: ${ex.message}", ex.expr)
      }
    }
  }
}

private class JsonCodecMakerInstance(cfg: CodecMakerConfig)(using Quotes) {
  import quotes.reflect._

  private case class JavaEnumValueInfo(value: Symbol, name: String, transformed: Boolean)

  private case class FieldInfo(symbol: Symbol, mappedName: String, getterOrField: Symbol, defaultValue: Option[Term],
                               resolvedTpe: TypeRepr, isTransient: Boolean, isStringified: Boolean, nonTransientFieldIndex: Int)

  private abstract class TypeInfo(val paramLists: List[List[FieldInfo]]) {
    val fields: List[FieldInfo] = paramLists.flatten.filter(!_.isTransient)

    def genNew(argss: List[List[Term]]): Term
  }

  private case class ClassInfo(tpe: TypeRepr, tpeTypeArgs: List[TypeRepr], primaryConstructor: Symbol,
                               override val paramLists: List[List[FieldInfo]]) extends TypeInfo(paramLists) {
    def genNew(argss: List[List[Term]]): Term =
      val constructorNoTypes = Select(New(Inferred(tpe)), primaryConstructor)
      val constructor =
        if (tpeTypeArgs eq Nil) constructorNoTypes
        else TypeApply(constructorNoTypes, tpeTypeArgs.map(Inferred(_)))
      argss.tail.foldLeft(Apply(constructor, argss.head))(Apply(_, _))
  }

  private case class NamedTupleInfo(tpe: TypeRepr, tupleTpe: TypeRepr, typeArgs: List[TypeRepr],
                                    override val paramLists: List[List[FieldInfo]]) extends TypeInfo(paramLists) {
    val isGeneric: Boolean = isGenericTuple(tupleTpe)

    def genNew(argss: List[List[Term]]): Term = {
      val args = argss.flatten
      if (isGeneric) {
        if (typeArgs eq Nil) enumOrModuleValueRef(TypeRepr.of[EmptyTuple])
        else {
          val arraySym = symbol("xs", arrayOfAnyTpe)
          val arrayRef = Ref(arraySym)
          val arrayValDef = ValDef(arraySym, new Some(Apply(newArrayOfAny, List(Literal(IntConstant(typeArgs.size))))))
          val assignments = args.map {
            val arrayUpdate = Select(arrayRef, defn.Array_update)
            var i = -1
            term =>
              i += 1
              Apply(arrayUpdate, List(Literal(IntConstant(i)), term))
          }
          val block = Block(arrayValDef :: assignments, arrayRef)
          val iArray = TypeApply(Select(block, asInstanceOfMethod), List(Inferred(iArrayOfAnyRefTpe)))
          TypeApply(Select(Apply(fromIArrayMethod, List(iArray)), asInstanceOfMethod), List(Inferred(tupleTpe)))
        }
      } else {
        val constructorNoTypes = Select(New(Inferred(tupleTpe)), tupleTpe.typeSymbol.primaryConstructor)
        Apply(TypeApply(constructorNoTypes, typeArgs.map(Inferred(_))), args)
      }
    }
  }

  private case class FieldAnnotations(partiallyMappedName: Option[String], transient: Boolean, stringified: Boolean)

  private case class DecoderMethodKey(tpe: TypeRepr, isStringified: Boolean, useDiscriminator: Boolean)

  private case class EncoderMethodKey(tpe: TypeRepr, isStringified: Boolean, discriminatorKeyValue: Option[(String, String)])

  private case class WriteDiscriminator(fieldName: String, fieldValue: String) {
    def write(out: Expr[JsonWriter]): Expr[Unit] = '{
      ${genWriteConstantKey(fieldName, out)}
      ${genWriteConstantVal(fieldValue, out)}
    }
  }

  private case class ReadDiscriminator(refOpt: Option[Ref]) {
    def skip(in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[Unit] = refOpt match
      case None => '{ $in.skip() }
      case Some(ref) => '{
        if (${ref.asExpr.asInstanceOf[Expr[Boolean]]}) {
          ${Assign(ref, Literal(BooleanConstant(false))).asExpr}
          $in.skip()
        } else $in.duplicatedKeyError($l)
      }
  }

  private val booleanTpe = defn.BooleanClass.typeRef
  private val byteTpe = defn.ByteClass.typeRef
  private val shortTpe = defn.ShortClass.typeRef
  private val intTpe = defn.IntClass.typeRef
  private val longTpe = defn.LongClass.typeRef
  private val floatTpe = defn.FloatClass.typeRef
  private val doubleTpe = defn.DoubleClass.typeRef
  private val charTpe = defn.CharClass.typeRef
  private val anyRefTpe = defn.AnyRefClass.typeRef
  private val anyValTpe = defn.AnyValClass.typeRef
  private val unitTpe = defn.UnitClass.typeRef
  private val anyTpe = defn.AnyClass.typeRef
  private val arrayOfAnyTpe = defn.ArrayClass.typeRef.appliedTo(anyTpe)
  private val iArrayOfAnyRefTpe = TypeRepr.of[IArray[AnyRef]]
  private val stringTpe = TypeRepr.of[String]
  private val tupleTpe = TypeRepr.of[Tuple]
  private val jsonKeyCodecTpe = TypeRepr.of[JsonKeyCodec]
  private val jsonValueCodecTpe = TypeRepr.of[JsonValueCodec]
  private val newArray = Select(New(TypeIdent(defn.ArrayClass)), defn.ArrayClass.primaryConstructor)
  private val newArrayOfAny = TypeApply(newArray, List(Inferred(anyTpe)))
  private val fromIArrayMethod = Select.unique(Ref(Symbol.requiredModule("scala.runtime.TupleXXL")), "fromIArray")
  private val asInstanceOfMethod = anyTpe.typeSymbol.methodMember("asInstanceOf").head
  private val inferredKeyCodecs = new mutable.HashMap[TypeRepr, Option[Expr[JsonKeyCodec[?]]]]
  private val inferredValueCodecs = new mutable.HashMap[TypeRepr, Option[Expr[JsonValueCodec[?]]]]
  private val inferredOrderings = new mutable.HashMap[TypeRepr, Term]
  private val decodeMethodRefs = new mutable.HashMap[DecoderMethodKey, Ref]
  private val encodeMethodRefs = new mutable.HashMap[EncoderMethodKey, Ref]
  private val decodeMethodDefs = new mutable.ArrayBuffer[DefDef]
  private val encodeMethodDefs = new mutable.ArrayBuffer[DefDef]
  private val classInfos = new mutable.LinkedHashMap[TypeRepr, ClassInfo]
  private val namedTupleInfos = new mutable.LinkedHashMap[TypeRepr, NamedTupleInfo]
  private val nullValues = new mutable.LinkedHashMap[TypeRepr, ValDef]
  private val fieldIndexAccessors = new mutable.LinkedHashMap[TypeRepr, DefDef]
  private val classTags = new mutable.LinkedHashMap[TypeRepr, ValDef]
  private val equalsMethods = new mutable.LinkedHashMap[TypeRepr, DefDef]
  private val scalaEnumCaches = new mutable.LinkedHashMap[TypeRepr, ValDef]
  private val javaEnumValueInfos = new mutable.LinkedHashMap[TypeRepr, List[JavaEnumValueInfo]]
  private val mathContexts = new mutable.LinkedHashMap[Int, ValDef]

  // used by Scala 3.7+ only
  private lazy val toTupleMethod = Select.unique(Ref(Symbol.requiredModule("scala.NamedTuple")), "toTuple")

  private def fail(msg: String): Nothing = report.errorAndAbort(msg, Position.ofMacroExpansion)

  private def warn(msg: String): Unit = report.warning(msg, Position.ofMacroExpansion)

  private def typeArgs(tpe: TypeRepr): List[TypeRepr] = tpe match
    case AppliedType(_, typeArgs) => typeArgs.map(_.dealias)
    case _ => Nil

  private def typeArg1(tpe: TypeRepr): TypeRepr = tpe match
    case AppliedType(_, typeArg1 :: _) => typeArg1.dealias
    case _ => fail(s"Cannot get 1st type argument in '${tpe.show}'")

  private def typeArg2(tpe: TypeRepr): TypeRepr = tpe match
    case AppliedType(_, _ :: typeArg2 :: _) => typeArg2.dealias
    case _ => fail(s"Cannot get 2nd type argument in '${tpe.show}'")

  private def isTuple(tpe: TypeRepr): Boolean = tpe <:< tupleTpe

  private def isGenericTuple(tpe: TypeRepr): Boolean = !defn.isTupleClass(tpe.typeSymbol)

  // Borrowed from an amazing work of Aleksander Rainko:
  // https://github.com/arainko/ducktape/blob/8d779f0303c23fd45815d3574467ffc321a8db2b/ducktape/src/main/scala/io/github/arainko/ducktape/internal/Structure.scala#L253-L270
  private def genericTupleTypeArgs(t: Type[?]): List[TypeRepr] = t match
    case '[head *: tail] => TypeRepr.of[head].dealias :: genericTupleTypeArgs(Type.of[tail])
    case _ => Nil

  // Borrowed from an amazing work of Aleksander Rainko:
  // https://github.com/arainko/ducktape/blob/8d779f0303c23fd45815d3574467ffc321a8db2b/ducktape/src/main/scala/io/github/arainko/ducktape/internal/Structure.scala#L277-L295
  private def normalizeGenericTuple(typeArgs: List[TypeRepr]): TypeRepr =
    val size = typeArgs.size
    if (size > 0 && size <= 22) defn.TupleClass(size).typeRef.appliedTo(typeArgs)
    else typeArgs.foldRight(TypeRepr.of[EmptyTuple]) {
      val tupleCons = TypeRepr.of[*:]
      (curr, acc) => tupleCons.appliedTo(curr :: acc :: Nil)
    }

  private def isNamedTuple(tpe: TypeRepr): Boolean = tpe match
    case AppliedType(ntTpe, _) => ntTpe.dealias.typeSymbol.fullName == "scala.NamedTuple$.NamedTuple"
    case _ => false

  private def valueClassValueSymbol(tpe: TypeRepr): Symbol = tpe.typeSymbol.fieldMembers.head

  private def valueClassValueType(tpe: TypeRepr): TypeRepr = tpe.memberType(valueClassValueSymbol(tpe)).dealias

  private def isNonAbstractScalaClass(tpe: TypeRepr): Boolean = tpe.classSymbol.fold(false) { sym =>
    val flags = sym.flags
    !(flags.is(Flags.Abstract) || flags.is(Flags.JavaDefined) || flags.is(Flags.Trait))
  }

  private def isOpaque(tpe: TypeRepr): Boolean = tpe.typeSymbol.flags.is(Flags.Opaque)

  @tailrec
  private def opaqueDealias(tpe: TypeRepr): TypeRepr = tpe match
    case trTpe: TypeRef if trTpe.isOpaqueAlias => opaqueDealias(trTpe.translucentSuperType.dealias)
    case _ => tpe

  private def isTypeRef(tpe: TypeRepr): Boolean = tpe match
    case trTpe: TypeRef =>
      val typeSymbol = trTpe.typeSymbol
      typeSymbol.isTypeDef && typeSymbol.isAliasType
    case _ => false

  private def typeRefDealias(tpe: TypeRepr): TypeRepr = tpe match
    case trTpe: TypeRef => trTpe.translucentSuperType.dealias
    case _ => tpe

  private def isSealedClass(tpe: TypeRepr): Boolean = tpe.typeSymbol.flags.is(Flags.Sealed)

  private def hasSealedParent(tpe: TypeRepr): Boolean =
    isSealedClass(tpe) || tpe.baseClasses.exists(_.flags.is(Flags.Sealed))

  private def isConstType(tpe: TypeRepr): Boolean = tpe match
    case _: ConstantType => true
    case _ => false

  private def isEnumValue(tpe: TypeRepr): Boolean = tpe.termSymbol.flags.is(Flags.Enum)

  private def isEnumOrModuleValue(tpe: TypeRepr): Boolean = isEnumValue(tpe) || tpe.typeSymbol.flags.is(Flags.Module)

  private def enumOrModuleValueRef(tpe: TypeRepr): Term = Ref {
    if (isEnumValue(tpe)) tpe.termSymbol
    else tpe.typeSymbol.companionModule
  }

  private def isOption(tpe: TypeRepr, types: List[TypeRepr]): Boolean = tpe <:< TypeRepr.of[Option[?]] &&
    (cfg.skipNestedOptionValues || !types.headOption.exists(_ <:< TypeRepr.of[Option[?]]))

  private def isNullable(tpe: TypeRepr): Boolean = tpe match
    case OrType(left, right) => isNullable(right) || isNullable(left)
    case _ => tpe =:= TypeRepr.of[Null]

  private def isIArray(tpe: TypeRepr): Boolean = tpe.typeSymbol.fullName == "scala.IArray$package$.IArray"

  private def isCollection(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[Iterable[?]] ||
    tpe <:< TypeRepr.of[Iterator[?]] || tpe <:< TypeRepr.of[Array[?]] || isIArray(tpe)

  private def isJavaEnum(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[java.lang.Enum[?]]

  private def scalaCollectionCompanion(tpe: TypeRepr): Term =
    val typeSymbol = tpe.typeSymbol
    if (typeSymbol.fullName.startsWith("scala.collection.")) Ref(typeSymbol.companionModule)
    else fail(s"Unsupported type '${tpe.show}'. Please consider using a custom implicitly accessible codec for it.")

  private def scalaCollectionBuilder(tpe: TypeRepr, eTpe: TypeRepr): Term =
    TypeApply(Select.unique(scalaCollectionCompanion(tpe), "newBuilder"), List(Inferred(eTpe)))

  private  def scalaMapBuilder(tpe: TypeRepr, kTpe: TypeRepr, vTpe: TypeRepr): Term =
    TypeApply(Select.unique(scalaCollectionCompanion(tpe), "newBuilder"), List(Inferred(kTpe), Inferred(vTpe)))

  private def scalaCollectionEmpty(tpe: TypeRepr, eTpe: TypeRepr): Term =
    TypeApply(Select.unique(scalaCollectionCompanion(tpe), "empty"), List(Inferred(eTpe)))

  private def scalaMapEmpty(tpe: TypeRepr, kTpe: TypeRepr, vTpe: TypeRepr): Term =
    TypeApply(Select.unique(scalaCollectionCompanion(tpe), "empty"), Inferred(kTpe) :: Inferred(vTpe) :: Nil)

  private def scala2EnumerationObject(tpe: TypeRepr): Expr[Enumeration] = tpe match
    case TypeRef(eTpe, _) => Ref(eTpe.termSymbol).asExpr.asInstanceOf[Expr[Enumeration]]

  private def symbol(name: String, tpe: TypeRepr, flags: Flags = Flags.EmptyFlags): Symbol =
    Symbol.newVal(Symbol.spliceOwner, name, tpe, flags, Symbol.noSymbol)

  private def checkRecursionInTypes(types: List[TypeRepr]): Unit = if (!cfg.allowRecursiveTypes) {
    val tpe = types.head
    val nestedTypes = types.tail
    val recursiveIdx = nestedTypes.indexOf(tpe)
    if (recursiveIdx >= 0) {
      val recTypes = nestedTypes.take(recursiveIdx + 1).map(_.show).reverse.mkString("'", "', '", "'")
      fail(s"Recursive type(s) detected: $recTypes. Please consider using a custom implicitly accessible codec for " +
        s"this type to control the level of recursion or turn on the '${Type.show[CodecMakerConfig]}.allowRecursiveTypes' " +
        "for the trusted input that will not exceed the thread stack size.")
    }
  }

  private def summonOrdering(tpe: TypeRepr): Term = inferredOrderings.getOrElseUpdate(tpe, {
    val orderingTpeApplied = TypeRepr.of[Ordering].appliedTo(tpe)
    Implicits.search(orderingTpeApplied) match
      case s: ImplicitSearchSuccess => s.tree
      case _ => fail(s"Can't summon '${orderingTpeApplied.show}'")
  })

  private def summonClassTag(tpe: TypeRepr): Term = Ref(classTags.getOrElseUpdate(tpe, {
    val classTagTpeApplied = TypeRepr.of[ClassTag].appliedTo(tpe)
    Implicits.search(classTagTpeApplied) match
      case s: ImplicitSearchSuccess => ValDef(symbol(s"ct${classTags.size}", classTagTpeApplied), new Some(s.tree))
      case _ => fail(s"Can't summon '${classTagTpeApplied.show}'")
  }).symbol)

  private def findImplicitKeyCodec(tpe: TypeRepr): Option[Expr[JsonKeyCodec[?]]] = inferredKeyCodecs.getOrElseUpdate(tpe, {
    Implicits.search(jsonKeyCodecTpe.appliedTo(tpe)) match
      case s: ImplicitSearchSuccess => new Some(s.tree.asExpr.asInstanceOf[Expr[JsonKeyCodec[?]]])
      case _ => None
  })

  private def findImplicitValueCodec(tpe: TypeRepr): Option[Expr[JsonValueCodec[?]]] = inferredValueCodecs.getOrElseUpdate(tpe, {
    Implicits.search(jsonValueCodecTpe.appliedTo(tpe)) match
      case s: ImplicitSearchSuccess => new Some(s.tree.asExpr.asInstanceOf[Expr[JsonValueCodec[?]]])
      case _ => None
  })

  private def withMathContextFor(precision: Int): Expr[MathContext] =
    if (precision == MathContext.DECIMAL128.getPrecision) '{ (MathContext.DECIMAL128: java.math.MathContext) }
    else if (precision == MathContext.DECIMAL64.getPrecision) '{ (MathContext.DECIMAL64: java.math.MathContext) }
    else if (precision == MathContext.DECIMAL32.getPrecision) '{ (MathContext.DECIMAL32: java.math.MathContext) }
    else if (precision == MathContext.UNLIMITED.getPrecision) '{ (MathContext.UNLIMITED: java.math.MathContext) }
    else Ref(mathContexts.getOrElseUpdate(precision, {
      val sym = symbol(s"mc${mathContexts.size}", TypeRepr.of[MathContext])
      ValDef(sym, new Some('{ new MathContext(${Expr(cfg.bigDecimalPrecision)}, java.math.RoundingMode.HALF_EVEN) }.asTerm))
    }).symbol).asExpr.asInstanceOf[Expr[MathContext]]

  private def findScala2EnumerationById[C <: AnyRef: Type](tpe: TypeRepr, i: Expr[Int])(using Quotes): Expr[Option[C]] =
    '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.id == $i) }.asInstanceOf[Expr[Option[C]]]

  private def findScala2EnumerationByName[C <: AnyRef: Type](tpe: TypeRepr, name: Expr[String])(using Quotes): Expr[Option[C]] =
    '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.toString == $name) }.asInstanceOf[Expr[Option[C]]]

  private def genNewArray[T: Type](size: Expr[Int])(using Quotes): Expr[Array[T]] =
    Apply(TypeApply(newArray, List(TypeTree.of[T])), List(size.asTerm)).asExpr.asInstanceOf[Expr[Array[T]]]

  private def withScalaEnumCacheFor[K: Type, T: Type](tpe: TypeRepr)(using Quotes): Expr[ConcurrentHashMap[K, T]] =
    Ref(scalaEnumCaches.getOrElseUpdate(tpe, {
      val sym = symbol(s"ec${scalaEnumCaches.size}", TypeRepr.of[ConcurrentHashMap[K, T]])
      ValDef(sym, new Some('{ new ConcurrentHashMap[K, T] }.asTerm))
    }).symbol).asExpr.asInstanceOf[Expr[ConcurrentHashMap[K, T]]]

  private def javaEnumValues(tpe: TypeRepr): List[JavaEnumValueInfo] = javaEnumValueInfos.getOrElseUpdate(tpe, {
    val classSym = tpe.classSymbol.getOrElse(fail(s"$tpe is not a class"))
    val values = classSym.children.map { sym =>
      val name = sym.name
      val transformed = cfg.javaEnumValueNameMapper(name).getOrElse(name)
      JavaEnumValueInfo(sym, transformed, name != transformed)
    }
    val nameCollisions = duplicated(values.map(_.name))
    if (nameCollisions.nonEmpty) {
      val formattedCollisions = nameCollisions.mkString("'", "', '", "'")
      fail(s"Duplicated JSON value(s) defined for '${tpe.show}': $formattedCollisions. Values are " +
        s"derived from value names of the enum that are mapped by the '${Type.show[CodecMakerConfig]}" +
        s".javaEnumValueNameMapper' function. Result values should be unique per enum class. All names: $values")
    }
    values
  })

  private def genReadJavaEnumValue[E: Type](enumValues: Seq[JavaEnumValueInfo], unexpectedEnumValueHandler: Expr[E],
                                            in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[E] = {
    def genReadCollisions(es: collection.Seq[JavaEnumValueInfo]): Expr[E] =
      es.foldRight(unexpectedEnumValueHandler)((e, acc) => '{
        if ($in.isCharBufEqualsTo($l, ${Expr(e.name)})) ${Ref(e.value).asExpr.asInstanceOf[Expr[E]]}
        else $acc
      })

    if (enumValues.size <= 8 && enumValues.foldLeft(0)(_ + _.name.length) <= 64) genReadCollisions(enumValues)
    else {
      val hashCode = (e: JavaEnumValueInfo) => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
      val cases = groupByOrdered(enumValues)(hashCode).map { case (hash, fs) =>
        val sym = Symbol.newBind(Symbol.spliceOwner, s"b$hash", Flags.EmptyFlags, intTpe)
        CaseDef(Bind(sym, Literal(IntConstant(hash))), None, genReadCollisions(fs).asTerm)
      } :+ CaseDef(Wildcard(), None, unexpectedEnumValueHandler.asTerm)
      Match('{ $in.charBufToHashCode($l) }.asTerm, cases.toList).asExpr.asInstanceOf[Expr[E]]
    }
  }

  private def getNamedTupleInfo(tpe: TypeRepr): NamedTupleInfo = namedTupleInfos.getOrElseUpdate(tpe, {
    tpe match
      case AppliedType(_, List(tpe1, tpe2)) =>
        val nTpe = tpe1.dealias
        var tTpe = tpe2.dealias
        // Borrowed from an amazing work of Aleksander Rainko: https://github.com/arainko/ducktape/blob/8d779f0303c23fd45815d3574467ffc321a8db2b/ducktape/src/main/scala/io/github/arainko/ducktape/internal/Structure.scala#L188-L199
        val nTypeArgs =
          if (isGenericTuple(nTpe)) genericTupleTypeArgs(nTpe.asType)
          else typeArgs(nTpe)
        var tTypeArgs =
          if (isGenericTuple(tTpe)) {
            val typeArgs = genericTupleTypeArgs(tTpe.asType)
            tTpe = normalizeGenericTuple(typeArgs)
            typeArgs
          } else typeArgs(tTpe)
        var i = - 1
        new NamedTupleInfo(tpe, tTpe, tTypeArgs, List(nTypeArgs.zip(tTypeArgs).map {
          case (ConstantType(StringConstant(name)), fTpe) =>
            i += 1
            val mappedName = cfg.fieldNameMapper(name).getOrElse(name)
            new FieldInfo(Symbol.noSymbol, mappedName, Symbol.noSymbol, None, fTpe, false, false, i)
          case _ => fail(s"Cannot extract names for '${tpe.show}'.")
        }))
  })

  private def getClassInfo(tpe: TypeRepr): ClassInfo = classInfos.getOrElseUpdate(tpe, {
    def hasSupportedAnnotation(m: Symbol): Boolean = m.annotations.exists { a =>
      val tpe = a.tpe
      tpe =:= TypeRepr.of[named] || tpe =:= TypeRepr.of[transient] || tpe =:= TypeRepr.of[stringified] ||
        (cfg.scalaTransientSupport && tpe =:= TypeRepr.of[scala.transient])
    }

    def supportedTransientTypeNames: String =
      if (cfg.scalaTransientSupport) s"'${Type.show[transient]}' (or '${Type.show[scala.transient]}')"
      else s"'${Type.show[transient]}')"

    val tpeTypeArgs = typeArgs(tpe)
    val tpeClassSym = tpe.classSymbol.getOrElse(fail(s"Expected that ${tpe.show} has classSymbol"))
    val primaryConstructor = tpeClassSym.primaryConstructor
    var annotations = Map.empty[String, FieldAnnotations]
    val caseFields = tpeClassSym.caseFields
    var companionRefAndMembers: (Ref, List[Symbol]) = null
    var fieldMembers: List[Symbol] = null
    var methodMembers: List[Symbol] = null

    tpeClassSym.fieldMembers.foreach {
      case m: Symbol if hasSupportedAnnotation(m) =>
        val name = m.name
        val named = m.annotations.count(_.tpe =:= TypeRepr.of[named])
        if (named > 1) fail(s"Duplicated '${TypeRepr.of[named].show}' defined for '$name' of '${tpe.show}'.")
        val trans = m.annotations.count(a => a.tpe =:= TypeRepr.of[transient] ||
          (cfg.scalaTransientSupport && a.tpe =:= TypeRepr.of[scala.transient]))
        if (trans > 1) warn(s"Duplicated $supportedTransientTypeNames defined for '$name' of '${tpe.show}'.")
        val strings = m.annotations.count(_.tpe =:= TypeRepr.of[stringified])
        if (strings > 1) warn(s"Duplicated '${TypeRepr.of[stringified].show}' defined for '$name' of '${tpe.show}'.")
        if ((named > 0 || strings > 0) && trans > 0)
          warn(s"Both $supportedTransientTypeNames and '${Type.show[named]}' or " +
            s"$supportedTransientTypeNames and '${Type.show[stringified]}' defined for '$name' of '${tpe.show}'.")
        val partiallyMappedName = namedValueOpt(m.annotations.find(_.tpe =:= TypeRepr.of[named]), tpe)
        annotations = annotations.updated(name, new FieldAnnotations(partiallyMappedName, trans > 0, strings > 0))
      case _ =>
    }

    def createFieldInfos(params: List[Symbol], typeParams: List[Symbol], fieldIndex: Boolean => Int): List[FieldInfo] = params.map {
      var i = 0
      symbol =>
        i += 1
        val name = symbol.name
        var fieldTpe = tpe.memberType(symbol).dealias
        if (tpeTypeArgs ne Nil) fieldTpe = fieldTpe.substituteTypes(typeParams, tpeTypeArgs)
        fieldTpe match
          case _: TypeLambda =>
            fail(s"Type lambdas are not supported for type '${tpe.show}' with field type for $name '${fieldTpe.show}'")
          case _: TypeBounds =>
            fail(s"Type bounds are not supported for type '${tpe.show}' with field type for $name '${fieldTpe.show}'")
          case _ =>
        val defaultValue = if (!cfg.requireDefaultFields && symbol.flags.is(Flags.HasDefault)) {
          val dvMemberName = "$lessinit$greater$default$" + i
          if (companionRefAndMembers eq null) {
            val typeSymbol = tpe.typeSymbol
            companionRefAndMembers = (Ref(typeSymbol.companionModule), typeSymbol.companionClass.methodMembers)
          }
          companionRefAndMembers._2.collectFirst { case methodSymbol if methodSymbol.name == dvMemberName =>
            val dvSelectNoTypes = Select(companionRefAndMembers._1, methodSymbol)
            methodSymbol.paramSymss match
              case Nil => dvSelectNoTypes
              case List(params) if params.exists(_.isTypeParam) => TypeApply(dvSelectNoTypes, tpeTypeArgs.map(Inferred(_)))
              case paramss => fail(s"Default method for $name of class ${tpe.show} have a complex parameter list: $paramss")
          }
        } else None
        val getterOrField = caseFields.find(_.name == name) match
          case Some(caseField) => caseField
          case _ =>
            if (fieldMembers eq null) fieldMembers = tpeClassSym.fieldMembers
            fieldMembers.find(_.name == name) match
              case Some(fieldMember) => fieldMember
              case _ =>
                if (methodMembers eq null) methodMembers = tpeClassSym.methodMembers
                methodMembers.find(x => x.flags.is(Flags.FieldAccessor) && x.name == name) match
                  case Some(methodMember) => methodMember
                  case _ => Symbol.noSymbol
        if (!getterOrField.exists || getterOrField.flags.is(Flags.PrivateLocal)) {
          fail(s"Getter or field '$name' of '${tpe.show}' is private. It should be defined as 'val' or 'var' in the primary constructor.")
        }
        val annotationOption = annotations.get(name)
        val mappedName = annotationOption.flatMap(_.partiallyMappedName).getOrElse(cfg.fieldNameMapper(name).getOrElse(name))
        val isStringified = annotationOption.exists(_.stringified)
        val isTransient = annotationOption.exists(_.transient)
        val index = fieldIndex(isTransient)
        new FieldInfo(symbol, mappedName, getterOrField, defaultValue, fieldTpe, isTransient, isStringified, index)
      }

    val fieldIndex: Boolean => Int = {
      var i = -1
      (isTransient: Boolean) =>
        if (!isTransient) i += 1
        i
    }
    new ClassInfo(tpe, tpeTypeArgs, primaryConstructor, primaryConstructor.paramSymss match {
      case tps :: pss if tps.exists(_.isTypeParam) => pss.map(ps => createFieldInfos(ps, tps, fieldIndex))
      case pss => pss.map(ps => createFieldInfos(ps, Nil, fieldIndex))
    })
  })

  private def isValueClass(tpe: TypeRepr): Boolean = !isConstType(tpe) && isNonAbstractScalaClass(tpe) &&
    (tpe <:< anyValTpe || cfg.inlineOneValueClasses && !isCollection(tpe) && getClassInfo(tpe).fields.size == 1)

  private def adtChildren(tpe: TypeRepr): Seq[TypeRepr] = {
    def resolveParentTypeArg(child: Symbol, fromNudeChildTarg: TypeRepr, parentTarg: TypeRepr,
                             binding: Map[String, TypeRepr]): Map[String, TypeRepr] = {
      val typeSymbol = fromNudeChildTarg.typeSymbol
      if (typeSymbol.isTypeParam) { // TODO: check for paramRef instead ?
        val paramName = typeSymbol.name
        binding.get(paramName) match
          case None => binding.updated(paramName, parentTarg)
          case Some(oldBinding) =>
            if (oldBinding =:= parentTarg) binding
            else fail(s"Type parameter $paramName in class ${child.name} appeared in the constructor of " +
              s"${tpe.show} two times differently, with ${oldBinding.show} and ${parentTarg.show}")
      } else if (fromNudeChildTarg <:< parentTarg) binding // TODO: assure parentTag is covariant, get covariance from type parameters
      else {
        (fromNudeChildTarg, parentTarg) match
          case (AppliedType(ctycon, ctargs), AppliedType(ptycon, ptargs)) =>
            ctargs.zip(ptargs).foldLeft(resolveParentTypeArg(child, ctycon, ptycon, binding)) { (b, e) =>
              resolveParentTypeArg(child, e._1, e._2, b)
            }
          case _ => fail(s"Failed unification of type parameters of ${tpe.show} from child $child - " +
            s"${fromNudeChildTarg.show} and ${parentTarg.show}")
      }
    }

    def resolveParentTypeArgs(child: Symbol, nudeChildParentTags: List[TypeRepr], parentTags: List[TypeRepr],
                              binding: Map[String, TypeRepr]): Map[String, TypeRepr] =
      nudeChildParentTags.zip(parentTags).foldLeft(binding)((b, e) => resolveParentTypeArg(child, e._1, e._2, b))

    val typeSymbol = tpe.typeSymbol
    typeSymbol.children.map { sym =>
      if (sym.isType) {
        if (sym.name == "<local child>") // problem - we have no other way to find this other return the name
          fail(s"Local child symbols are not supported, please consider change '${tpe.show}' or implement a " +
            "custom implicitly accessible codec")
        val nudeSubtype = sym.typeRef
        val tpeArgsFromChild = typeArgs(nudeSubtype.baseType(typeSymbol))
        nudeSubtype.memberType(sym.primaryConstructor) match
          case _: MethodType => nudeSubtype
          case PolyType(names, _, resPolyTp) =>
            val tpBinding = resolveParentTypeArgs(sym, tpeArgsFromChild, typeArgs(tpe), Map.empty)
            val ctArgs = names.map { name =>
              tpBinding.getOrElse(name, fail(s"Type parameter '$name' of '$sym' can't be deduced from " +
                s"type arguments of '${tpe.show}'. Please provide a custom implicitly accessible codec for it."))
            }
            val polyRes = resPolyTp match
              case MethodType(_, _, resTp) => resTp
              case other => other // hope we have no multiple typed param lists yet.
            if (ctArgs.isEmpty) polyRes
            else polyRes match
              case AppliedType(base, _) => base.appliedTo(ctArgs)
              case AnnotatedType(AppliedType(base, _), annot) => AnnotatedType(base.appliedTo(ctArgs), annot)
              case _ => polyRes.appliedTo(ctArgs)
          case other => fail(s"Primary constructor for '${tpe.show}' is not 'MethodType' or 'PolyType' but '$other''")
      } else if (sym.isTerm) Ref(sym).tpe
      else fail("Only concrete (no free type parameters) Scala classes & objects are supported for ADT leaf classes. " +
        s"Please consider using of them for ADT with base '${tpe.show}' or provide a custom implicitly accessible codec for the ADT base.")
    }
  }

  private def adtLeafClasses(adtBaseTpe: TypeRepr): Seq[TypeRepr] = {
    def collectRecursively(tpe: TypeRepr): Seq[TypeRepr] =
      val leafTpes = adtChildren(tpe).flatMap { subTpe =>
        if (isEnumOrModuleValue(subTpe)) Seq(subTpe)
        else if (isSealedClass(subTpe)) collectRecursively(subTpe)
        else if (isValueClass(subTpe)) {
          fail("'AnyVal' and one value classes with 'CodecMakerConfig.withInlineOneValueClasses(true)' are not " +
            s"supported as leaf classes for ADT with base '${adtBaseTpe.show}'.")
        } else if (isNonAbstractScalaClass(subTpe)) Seq(subTpe)
        else fail((if (subTpe.typeSymbol.flags.is(Flags.Abstract) || subTpe.typeSymbol.flags.is(Flags.Trait)) {
          "Only sealed intermediate traits or abstract classes are supported."
        } else {
          "Only concrete (no free type parameters) Scala classes & objects are supported for ADT leaf classes."
        }) + s" Please consider using of them for ADT with base '${adtBaseTpe.show}' or provide a custom implicitly accessible codec for the ADT base.")
      }
      if (isNonAbstractScalaClass(tpe)) leafTpes :+ tpe
      else leafTpes

    val classes = distinct(collectRecursively(adtBaseTpe))
    if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '${adtBaseTpe.show}'. " +
      "Please add them or provide a custom implicitly accessible codec for the ADT base.")
    classes
  }

  private def genReadKey[T: Type](types: List[TypeRepr], in: Expr[JsonReader])(using Quotes): Expr[T] = {
    val tpe = types.head
    val implKeyCodec = findImplicitKeyCodec(tpe)
    if (implKeyCodec.isDefined) '{ ${implKeyCodec.get}.decodeKey($in) }
    else if (tpe =:= stringTpe) '{ $in.readKeyAsString() }
    else if (tpe =:= booleanTpe) '{ $in.readKeyAsBoolean() }
    else if (tpe =:= byteTpe) '{ $in.readKeyAsByte() }
    else if (tpe =:= shortTpe) '{ $in.readKeyAsShort() }
    else if (tpe =:= intTpe) '{ $in.readKeyAsInt() }
    else if (tpe =:= longTpe) '{ $in.readKeyAsLong() }
    else if (tpe =:= floatTpe) '{ $in.readKeyAsFloat() }
    else if (tpe =:= doubleTpe) '{ $in.readKeyAsDouble() }
    else if (tpe =:= charTpe) '{ $in.readKeyAsChar() }
    else if (tpe =:= TypeRepr.of[java.lang.Boolean]) '{ java.lang.Boolean.valueOf($in.readKeyAsBoolean()) }
    else if (tpe =:= TypeRepr.of[java.lang.Byte]) '{ java.lang.Byte.valueOf($in.readKeyAsByte()) }
    else if (tpe =:= TypeRepr.of[java.lang.Short]) '{ java.lang.Short.valueOf($in.readKeyAsShort()) }
    else if (tpe =:= TypeRepr.of[java.lang.Integer]) '{ java.lang.Integer.valueOf($in.readKeyAsInt()) }
    else if (tpe =:= TypeRepr.of[java.lang.Long]) '{ java.lang.Long.valueOf($in.readKeyAsLong()) }
    else if (tpe =:= TypeRepr.of[java.lang.Float]) '{ java.lang.Float.valueOf($in.readKeyAsFloat()) }
    else if (tpe =:= TypeRepr.of[java.lang.Double]) '{ java.lang.Double.valueOf($in.readKeyAsDouble()) }
    else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ java.lang.Character.valueOf($in.readKeyAsChar()) }
    else if (tpe =:= TypeRepr.of[BigInt]) '{ $in.readKeyAsBigInt(${Expr(cfg.bigIntDigitsLimit)}) }
    else if (tpe =:= TypeRepr.of[BigDecimal]) {
      val mc = withMathContextFor(cfg.bigDecimalPrecision)
      '{ $in.readKeyAsBigDecimal($mc, ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)}) }
    } else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $in.readKeyAsUUID() }
    else if (tpe =:= TypeRepr.of[Duration]) '{ $in.readKeyAsDuration() }
    else if (tpe =:= TypeRepr.of[Instant]) '{ $in.readKeyAsInstant() }
    else if (tpe =:= TypeRepr.of[LocalDate]) '{ $in.readKeyAsLocalDate() }
    else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $in.readKeyAsLocalDateTime() }
    else if (tpe =:= TypeRepr.of[LocalTime]) '{ $in.readKeyAsLocalTime() }
    else if (tpe =:= TypeRepr.of[MonthDay]) '{ $in.readKeyAsMonthDay() }
    else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $in.readKeyAsOffsetDateTime() }
    else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $in.readKeyAsOffsetTime() }
    else if (tpe =:= TypeRepr.of[Period]) '{ $in.readKeyAsPeriod() }
    else if (tpe =:= TypeRepr.of[Year]) '{ $in.readKeyAsYear() }
    else if (tpe =:= TypeRepr.of[YearMonth]) '{ $in.readKeyAsYearMonth() }
    else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $in.readKeyAsZonedDateTime() }
    else if (tpe =:= TypeRepr.of[ZoneId]) '{ $in.readKeyAsZoneId() }
    else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $in.readKeyAsZoneOffset() }
    else if ({
      checkRecursionInTypes(types)
      isValueClass(tpe)
    }) {
      val vtpe = valueClassValueType(tpe)
      vtpe.asType match
        case '[vt] => getClassInfo(tpe).genNew(List(List(genReadKey[vt](vtpe :: types, in).asTerm))).asExpr
    } else if (tpe <:< TypeRepr.of[Enumeration#Value]) {
      if (cfg.useScalaEnumValueId) {
        val ec = withScalaEnumCacheFor[Int, T & Enumeration#Value](tpe)
        '{
          val i = $in.readKeyAsInt()
          var x = $ec.get(i)
          if (x eq null) {
            x = ${findScala2EnumerationById[T & Enumeration#Value](tpe, 'i)}.getOrElse($in.enumValueError(i.toString))
            $ec.put(i, x)
          }
          x
        }
      } else {
        val ec = withScalaEnumCacheFor[String, T & Enumeration#Value](tpe)
        '{
          val s = $in.readKeyAsString()
          var x = $ec.get(s)
          if (x eq null) {
            x = ${findScala2EnumerationByName[T & Enumeration#Value](tpe, 's)}.getOrElse($in.enumValueError(s.length))
            $ec.put(s, x)
          }
          x
        }
      }
    } else if (isJavaEnum(tpe)) {
      '{
        val l = $in.readKeyAsCharBuf()
        ${genReadJavaEnumValue(javaEnumValues(tpe), '{ $in.enumValueError(l) }, in, 'l)}
      }
    } else if (isConstType(tpe)) {
      tpe match
        case ConstantType(StringConstant(v)) =>
          '{ if ($in.readKeyAsString() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(BooleanConstant(v)) =>
          '{ if ($in.readKeyAsBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(ByteConstant(v)) =>
          '{ if ($in.readKeyAsByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(ShortConstant(v)) =>
          '{ if ($in.readKeyAsShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(IntConstant(v)) =>
          '{ if ($in.readKeyAsInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(LongConstant(v)) =>
          '{ if ($in.readKeyAsLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(FloatConstant(v)) =>
          '{ if ($in.readKeyAsFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(DoubleConstant(v)) =>
          '{ if ($in.readKeyAsDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case ConstantType(CharConstant(v)) =>
          '{ if ($in.readKeyAsChar() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }
        case _ => cannotFindKeyCodecError(tpe)
    } else if (isOpaque(tpe)) {
      val sTpe = opaqueDealias(tpe)
      sTpe.asType match { case '[s] => '{ ${genReadKey[s](sTpe :: types.tail, in)}.asInstanceOf[T] } }
    } else if (isTypeRef(tpe)) {
      val sTpe = typeRefDealias(tpe)
      sTpe.asType match { case '[s] => '{ ${genReadKey[s](sTpe :: types.tail, in)}.asInstanceOf[T] } }
    } else cannotFindKeyCodecError(tpe)
  }.asInstanceOf[Expr[T]]

  private def genReadArray[B: Type, C: Type](newBuilder: Quotes ?=> Expr[Int] => Expr[B],
                                             readVal: Quotes ?=> (Expr[B], Expr[Int], Expr[Int]) => Expr[Unit],
                                             default: Expr[C], result: Quotes ?=> (Expr[B], Expr[Int], Expr[Int]) => Expr[C],
                                             in: Expr[JsonReader])(using Quotes): Expr[C] = '{
    if ($in.isNextToken('[')) {
      if ($in.isNextToken(']')) $default
      else {
        $in.rollbackToken()
        var l = 8
        var x = ${newBuilder('l)}
        var i = 0
        while ({
          ${readVal('x, 'i, 'l)}
          i += 1
          $in.isNextToken(',')
        }) ()
        if ($in.isCurrentToken(']')) ${result('x, 'i, 'l)}
        else $in.arrayEndOrCommaError()
      }
    } else $in.readNullOrTokenError($default, '[')
  }

  private def genReadCollection[B: Type, C: Type](newBuilder: Expr[B], readVal: Quotes ?=> Expr[B] => Expr[Unit],
                                                  default: Expr[C], result: Quotes ?=> Expr[B] => Expr[C],
                                                  in: Expr[JsonReader])(using Quotes): Expr[C] = '{
    if ($in.isNextToken('[')) {
      if ($in.isNextToken(']')) $default
      else {
        $in.rollbackToken()
        val x = $newBuilder
        while ({
          ${readVal('x)}
          $in.isNextToken(',')
        }) ()
        if ($in.isCurrentToken(']')) ${result('x)}
        else $in.arrayEndOrCommaError()
      }
    } else $in.readNullOrTokenError($default, '[')
  }

  private def genReadSet[B: Type, C: Type](newBuilder: Expr[B], readVal: Quotes ?=> Expr[B] => Expr[Unit], default: Expr[C],
                                           result: Quotes ?=> Expr[B] => Expr[C], in: Expr[JsonReader])(using Quotes): Expr[C] =
    if (cfg.setMaxInsertNumber == Int.MaxValue) genReadCollection(newBuilder, readVal, default, result, in)
    else '{
      if ($in.isNextToken('[')) {
        if ($in.isNextToken(']')) $default
        else {
          $in.rollbackToken()
          var x = $newBuilder
          var i = 0
          while ({
            ${readVal('x)}
            i += 1
            if (i > ${Expr(cfg.setMaxInsertNumber)}) $in.decodeError("too many set inserts")
            $in.isNextToken(',')
          }) ()
          if ($in.isCurrentToken(']')) ${result('x)}
          else $in.arrayEndOrCommaError()
        }
      } else $in.readNullOrTokenError($default, '[')
    }

  private def genReadMap[B: Type, C: Type](newBuilder: Expr[B], readKV: Quotes ?=> Expr[B] => Expr[Unit],
                                           result: Quotes ?=> Expr[B] => Expr[C], in: Expr[JsonReader],
                                           default: Expr[C])(using Quotes): Expr[C] =
    if (cfg.setMaxInsertNumber == Int.MaxValue) '{
      if ($in.isNextToken('{')) {
        if ($in.isNextToken('}')) $default
        else {
          $in.rollbackToken()
          var x = $newBuilder
          while ({
            ${readKV('x)}
            $in.isNextToken(',')
          }) ()
          if ($in.isCurrentToken('}')) ${result('x)}
          else $in.objectEndOrCommaError()
        }
      } else $in.readNullOrTokenError($default, '{')
    } else '{
      if ($in.isNextToken('{')) {
        if ($in.isNextToken('}')) $default
        else {
          $in.rollbackToken()
          var x = $newBuilder
          var i = 0
          while ({
            ${readKV('x)}
            i += 1
            if (i > ${Expr(cfg.mapMaxInsertNumber)}) $in.decodeError("too many map inserts")
            $in.isNextToken(',')
          }) ()
          if ($in.isCurrentToken('}')) ${result('x)}
          else $in.objectEndOrCommaError()
        }
      } else $in.readNullOrTokenError($default, '{')
    }

  private def genReadMapAsArray[B: Type, C: Type](newBuilder: Expr[B], readKV: Quotes ?=> Expr[B] => Expr[Unit],
                                                  result: Quotes ?=> Expr[B] => Expr[C], in: Expr[JsonReader],
                                                  default: Expr[C])(using Quotes): Expr[C] =
    if (cfg.setMaxInsertNumber == Int.MaxValue) '{
      if ($in.isNextToken('[')) {
        if ($in.isNextToken(']')) $default
        else {
          $in.rollbackToken()
          var x = $newBuilder
          while ({
            if ($in.isNextToken('[')) {
              ${readKV('x)}
              if (!$in.isNextToken(']')) $in.arrayEndError()
            } else $in.decodeError("expected '['")
            $in.isNextToken(',')
          }) ()
          if ($in.isCurrentToken(']')) ${result('x)}
          else $in.arrayEndOrCommaError()
        }
      } else $in.readNullOrTokenError($default, '[')
    } else '{
      if ($in.isNextToken('[')) {
        if ($in.isNextToken(']')) $default
        else {
          $in.rollbackToken()
          var x = $newBuilder
          var i = 0
          while ({
            if ($in.isNextToken('[')) {
              ${readKV('x)}
              i += 1
              if (i > ${Expr(cfg.mapMaxInsertNumber)}) $in.decodeError("too many map inserts")
              if (!$in.isNextToken(']')) $in.arrayEndError()
            } else $in.decodeError("expected '['")
            $in.isNextToken(',')
          }) ()
          if ($in.isCurrentToken(']')) ${result('x)}
          else $in.arrayEndOrCommaError()
        }
      } else $in.readNullOrTokenError($default, '[')
    }

  @tailrec
  private def genWriteKey[T: Type](x: Expr[T], types: List[TypeRepr], out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
    val tpe = types.head
    val implKeyCodec = findImplicitKeyCodec(tpe)
    if (implKeyCodec.isDefined) '{ ${implKeyCodec.get.asInstanceOf[Expr[JsonKeyCodec[T]]]}.encodeKey($x, $out) }
    else if (tpe =:= stringTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[String]]}) }
    else if (tpe =:= booleanTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Boolean]]}) }
    else if (tpe =:= byteTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Byte]]}) }
    else if (tpe =:= shortTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Short]]}) }
    else if (tpe =:= intTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Int]]}) }
    else if (tpe =:= longTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Long]]}) }
    else if (tpe =:= floatTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Float]]}) }
    else if (tpe =:= doubleTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Double]]}) }
    else if (tpe =:= charTpe) '{ $out.writeKey(${x.asInstanceOf[Expr[Char]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Boolean]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Boolean]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Byte]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Byte]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Short]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Short]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Integer]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Integer]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Long]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Long]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Float]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Float]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Double]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Double]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.lang.Character]]}) }
    else if (tpe =:= TypeRepr.of[BigInt]) '{ $out.writeKey(${x.asInstanceOf[Expr[BigInt]]}) }
    else if (tpe =:= TypeRepr.of[BigDecimal]) '{ $out.writeKey(${x.asInstanceOf[Expr[BigDecimal]]}) }
    else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $out.writeKey(${x.asInstanceOf[Expr[java.util.UUID]]}) }
    else if (tpe =:= TypeRepr.of[Duration]) '{ $out.writeKey(${x.asInstanceOf[Expr[Duration]]}) }
    else if (tpe =:= TypeRepr.of[Instant]) '{ $out.writeKey(${x.asInstanceOf[Expr[Instant]]}) }
    else if (tpe =:= TypeRepr.of[LocalDate]) '{ $out.writeKey(${x.asInstanceOf[Expr[LocalDate]]}) }
    else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $out.writeKey(${x.asInstanceOf[Expr[LocalDateTime]]}) }
    else if (tpe =:= TypeRepr.of[LocalTime]) '{ $out.writeKey(${x.asInstanceOf[Expr[LocalTime]]}) }
    else if (tpe =:= TypeRepr.of[MonthDay]) '{ $out.writeKey(${x.asInstanceOf[Expr[MonthDay]]}) }
    else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $out.writeKey(${x.asInstanceOf[Expr[OffsetDateTime]]}) }
    else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $out.writeKey(${x.asInstanceOf[Expr[OffsetTime]]}) }
    else if (tpe =:= TypeRepr.of[Period]) '{ $out.writeKey(${x.asInstanceOf[Expr[Period]]}) }
    else if (tpe =:= TypeRepr.of[Year]) '{ $out.writeKey(${x.asInstanceOf[Expr[Year]]}) }
    else if (tpe =:= TypeRepr.of[YearMonth]) '{ $out.writeKey(${x.asInstanceOf[Expr[YearMonth]]}) }
    else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $out.writeKey(${x.asInstanceOf[Expr[ZonedDateTime]]}) }
    else if (tpe =:= TypeRepr.of[ZoneId]) '{ $out.writeKey(${x.asInstanceOf[Expr[ZoneId]]}) }
    else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $out.writeKey(${x.asInstanceOf[Expr[ZoneOffset]]}) }
    else if ({
      checkRecursionInTypes(types)
      isValueClass(tpe)
    }) {
      val vtpe = valueClassValueType(tpe)
      vtpe.asType match
        case '[vt] =>
          val valueExpr = Select(x.asTerm, valueClassValueSymbol(tpe)).asExpr.asInstanceOf[Expr[vt]]
          genWriteKey(valueExpr, vtpe :: types, out)
    } else if (tpe <:< TypeRepr.of[Enumeration#Value]) {
      if (cfg.useScalaEnumValueId) '{ $out.writeKey(${x.asInstanceOf[Expr[Enumeration#Value]]}.id) }
      else '{ $out.writeKey($x.toString) }
    } else if (isJavaEnum(tpe)) {
      val es = javaEnumValues(tpe)
      val encodingRequired = es.exists(e => isEncodingRequired(e.name))
      if (es.exists(_.transformed)) {
        val cases = es.map(e => CaseDef(Ref(e.value), None, Literal(StringConstant(e.name)))) :+
          CaseDef(Wildcard(), None, '{ $out.encodeError("illegal enum value: " + $x) }.asTerm)
        val matchExpr = Match(x.asTerm, cases).asExpr.asInstanceOf[Expr[String]]
        if (encodingRequired) '{ $out.writeKey($matchExpr) }
        else '{ $out.writeNonEscapedAsciiKey($matchExpr) }
      } else {
        val tx = x.asInstanceOf[Expr[java.lang.Enum[?]]]
        if (encodingRequired) '{ $out.writeKey($tx.name) }
        else '{ $out.writeNonEscapedAsciiKey($tx.name) }
      }
    } else if (isConstType(tpe)) {
      tpe match
        case ConstantType(StringConstant(v)) => genWriteConstantKey(v, out)
        case ConstantType(BooleanConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(ByteConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(ShortConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(IntConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(LongConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(FloatConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(DoubleConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case ConstantType(CharConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
        case _ => cannotFindKeyCodecError(tpe)
    } else if (isOpaque(tpe)) {
      val sTpe = opaqueDealias(tpe)
      sTpe.asType match { case '[s] => genWriteKey('{ $x.asInstanceOf[s] }, sTpe :: types.tail, out) }
    } else if (isTypeRef(tpe)) {
      val sTpe = typeRefDealias(tpe)
      sTpe.asType match { case '[s] => genWriteKey('{ $x.asInstanceOf[s] }, sTpe :: types.tail, out) }
    } else cannotFindKeyCodecError(tpe)

  private def genWriteConstantKey(name: String, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
    if (isEncodingRequired(name)) '{ $out.writeKey(${Expr(name)}) }
    else '{ $out.writeNonEscapedAsciiKey(${Expr(name)}) }

  private def genWriteConstantVal(value: String, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
    if (isEncodingRequired(value)) '{ $out.writeVal(${Expr(value)}) }
    else '{ $out.writeNonEscapedAsciiVal(${Expr(value)}) }

  private def genWriteArray[T: Type](x: Expr[Iterable[T]], writeVal: Quotes ?=> (Expr[JsonWriter], Expr[T]) => Expr[Unit],
                                     out: Expr[JsonWriter])(using Quotes): Expr[Unit] = '{
    $out.writeArrayStart()
    $x.foreach(x => ${writeVal(out, 'x)})
    $out.writeArrayEnd()
  }

  private def genWriteArray2[T: Type](x: Expr[Iterator[T]], writeVal: Quotes ?=> (Expr[JsonWriter], Expr[T]) => Expr[Unit],
                                      out: Expr[JsonWriter])(using Quotes): Expr[Unit] = '{
    $out.writeArrayStart()
    while ($x.hasNext) ${writeVal(out, '{$x.next()})}
    $out.writeArrayEnd()
  }

  private def genWriteMapScala[K: Type, V: Type](x: Expr[collection.Map[K, V]],
                                                 writeKey: Quotes ?=> (Expr[JsonWriter], Expr[K]) => Expr[Unit],
                                                 writeVal: Quotes ?=> (Expr[JsonWriter], Expr[V]) => Expr[Unit],
                                                 out: Expr[JsonWriter])(using Quotes): Expr[Unit] = '{
    $out.writeObjectStart()
    $x.foreachEntry { (k, v) =>
      ${writeKey(out, 'k)}
      ${writeVal(out, 'v)}
    }
    $out.writeObjectEnd()
  }

  private def genWriteMapAsArrayScala[K: Type, V: Type](x: Expr[collection.Map[K, V]],
                                                        writeKey: Quotes ?=> (Expr[JsonWriter], Expr[K]) => Expr[Unit],
                                                        writeVal: Quotes ?=> (Expr[JsonWriter], Expr[V]) => Expr[Unit],
                                                        out: Expr[JsonWriter])(using Quotes): Expr[Unit] = '{
    $out.writeArrayStart()
    $x.foreachEntry { (k, v) =>
      $out.writeArrayStart()
      ${writeKey(out, 'k)}
      ${writeVal(out, 'v)}
      $out.writeArrayEnd()
    }
    $out.writeArrayEnd()
  }

  private def cannotFindKeyCodecError(tpe: TypeRepr): Nothing =
    fail(s"No implicit '${TypeRepr.of[JsonKeyCodec[?]].show}' defined for '${tpe.show}'.")

  private def cannotFindValueCodecError(tpe: TypeRepr): Nothing =
    fail(if (tpe.typeSymbol.flags.is(Flags.Abstract) || tpe.typeSymbol.flags.is(Flags.Trait)) {
      "Only sealed traits or abstract classes are supported as an ADT base. " +
        s"Please consider sealing the '${tpe.show}' or provide a custom implicitly accessible codec for it."
    } else s"No implicit '${TypeRepr.of[JsonValueCodec[?]].show}' defined for '${tpe.show}'.")

  private def namedValueOpt(namedAnnotation: Option[Term], tpe: TypeRepr): Option[String] = namedAnnotation.map {
    case Apply(_, List(param)) => CompileTimeEval.evalExpr(param.asExpr.asInstanceOf[Expr[String]]).asTerm match
      case Literal(StringConstant(s)) => s
      case _ => fail(s"Cannot evaluate a parameter of the '@named' annotation in type '${tpe.show}': $param.")
  }

  private def unexpectedFieldHandler(in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[Unit] =
    if (cfg.skipUnexpectedFields) '{ $in.skip() }
    else '{ $in.unexpectedKeyError($l) }

  private def discriminatorValue(tpe: TypeRepr): String =
    val isEnumVal = isEnumValue(tpe)
    val symbol =
      if (isEnumVal) tpe.termSymbol
      else tpe.typeSymbol
    val named = symbol.annotations.filter(_.tpe =:= TypeRepr.of[named])
    if (named ne Nil) {
      if (named.size > 1) fail(s"Duplicated '${TypeRepr.of[named].show}' defined for '${tpe.show}'.")
      namedValueOpt(named.headOption, tpe).get
    } else cfg.adtLeafClassNameMapper({
      val name = symbol.fullName
      if (symbol.flags.is(Flags.Module)) name.substring(0, name.length - 1)
      else name
    }).getOrElse(fail(s"Discriminator is not defined for ${tpe.show}"))

  private def checkFieldNameCollisions(tpe: TypeRepr, names: Seq[String]): Unit =
    val collisions = duplicated(names)
    if (collisions.nonEmpty) {
      val formattedCollisions = collisions.mkString("'", "', '", "'")
      fail(s"Duplicated JSON key(s) defined for '${tpe.show}': $formattedCollisions. Keys are derived from " +
        s"field names of the class that are mapped by the '${TypeRepr.of[CodecMakerConfig].show}.fieldNameMapper' " +
        s"function or can be overridden by '${TypeRepr.of[named].show}' annotation(s). Result keys should be " +
        s"unique and should not match with a key for the discriminator field that is specified by the " +
        s"'${TypeRepr.of[CodecMakerConfig].show}.discriminatorFieldName' option.")
    }

  private def checkDiscriminatorValueCollisions(tpe: TypeRepr, names: Seq[String]): Unit =
    val collisions = duplicated(names)
    if (collisions.nonEmpty) {
      val formattedCollisions = collisions.mkString("'", "', '", "'")
      fail(s"Duplicated discriminator defined for ADT base '${tpe.show}': $formattedCollisions. Values for " +
        s"leaf classes of ADT that are returned by the '${Type.show[CodecMakerConfig]}.adtLeafClassNameMapper' " +
        s"function should be unique. Names: $names")
    }

  private def withNullValueFor[T: Type](tpe: TypeRepr)(f: => Expr[T]): Expr[T] =
    Ref(nullValues.getOrElseUpdate(tpe, ValDef(symbol(s"c${nullValues.size}", tpe), new Some(f.asTerm))).symbol)
      .asExpr.asInstanceOf[Expr[T]]

  private def withFieldsByIndexFor(tpe: TypeRepr)(f: => List[String]): Term =
    Ref(fieldIndexAccessors.getOrElseUpdate(tpe, { // [Int => String], we don't want eta-expand without reason, so let this will be just index
      val sym = Symbol.newMethod(Symbol.spliceOwner, s"f${fieldIndexAccessors.size}",
        MethodType(List("i"))(_ => intTpe :: Nil, _ => stringTpe))
      DefDef(sym, params => {
        val List(List(param)) = params
        val cases = f.map {
          var i = -1
          n =>
            i += 1
            CaseDef(Literal(IntConstant(i)), None, Literal(StringConstant(n)))
        }
        new Some(Match(param.asExpr.asTerm, cases))
      })
    }).symbol)

  private def withEqualsFor[T: Type](tpe: TypeRepr, arg1: Expr[T], arg2: Expr[T])
                                     (f: (Expr[T], Expr[T]) => Expr[Boolean]): Expr[Boolean] =
    Apply(Ref(equalsMethods.getOrElseUpdate(tpe, {
      val sym = Symbol.newMethod(Symbol.spliceOwner, s"q${equalsMethods.size}",
        MethodType("x1" :: "x2" :: Nil)(_ => tpe :: tpe :: Nil, _ => booleanTpe))
      DefDef(sym, params => {
        val List(List(x1, x2)) = params
        new Some(f(x1.asExpr.asInstanceOf[Expr[T]], x2.asExpr.asInstanceOf[Expr[T]]).asTerm.changeOwner(sym))
      })
    }).symbol), List(arg1.asTerm, arg2.asTerm)).asExpr.asInstanceOf[Expr[Boolean]]

  private def genArrayEquals[T: Type](tpe: TypeRepr, x1t: Expr[T], x2t: Expr[T]): Expr[Boolean] =
    val tpe1 = typeArg1(tpe)
    if (tpe1 <:< TypeRepr.of[Array[?]]) {
      tpe1.asType match
        case '[t1] =>
          val x1 = x1t.asInstanceOf[Expr[Array[t1]]]
          val x2 = x2t.asInstanceOf[Expr[Array[t1]]]

          def arrEquals(i: Expr[Int])(using Quotes): Expr[Boolean] =
            withEqualsFor(tpe1, '{ $x1($i) }, '{ $x2($i) })((x1, x2) => genArrayEquals(tpe1, x1, x2))

          '{
            ($x1 eq $x2) || (($x1 ne null) && ($x2 ne null) && {
              val l = $x1.length
              ($x2.length == l) && {
                var i = 0
                while (i < l && ${arrEquals('i)}) i += 1
                i == l
              }
            })
          }
    } else if (isIArray(tpe1)) {
      tpe1.asType match
        case '[t1] =>
          val x1 = x1t.asInstanceOf[Expr[IArray[t1]]]
          val x2 = x2t.asInstanceOf[Expr[IArray[t1]]]

          def arrEquals(i: Expr[Int])(using Quotes): Expr[Boolean] =
            withEqualsFor(tpe1, '{ $x1($i) }, '{ $x2($i) })((x1, x2) => genArrayEquals(tpe1, x1, x2))

          '{
            (($x1 ne null) && ($x2 ne null) && {
              val l = $x1.length
              ($x2.length == l) && {
                var i = 0
                while (i < l && ${arrEquals('i)}) i += 1
                i == l
              }
            })
          }
    } else if (isIArray(tpe)) {
      if (tpe1 =:= anyRefTpe) {
        '{ IArray.equals(${x1t.asInstanceOf[Expr[IArray[AnyRef]]]}, ${x2t.asInstanceOf[Expr[IArray[AnyRef]]]}) }
      } else {
        tpe1.asType match
          case '[t1] =>
            val x1 = x1t.asInstanceOf[Expr[IArray[t1]]]
            val x2 = x2t.asInstanceOf[Expr[IArray[t1]]]

            '{
              (($x1 ne null) && ($x2 ne null) && {
                val l = $x1.length
                ($x2.length == l) && {
                  var i = 0
                  while (i < l && $x1(i) == $x2(i)) i += 1
                  i == l
                }
              })
            }
      }
    } else if (tpe1 =:= booleanTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Boolean]]]}, ${x2t.asInstanceOf[Expr[Array[Boolean]]]}) }
    } else if (tpe1 =:= byteTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Byte]]]}, ${x2t.asInstanceOf[Expr[Array[Byte]]]}) }
    } else if (tpe1 =:= shortTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Short]]]}, ${x2t.asInstanceOf[Expr[Array[Short]]]}) }
    } else if (tpe1 =:= intTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Int]]]}, ${x2t.asInstanceOf[Expr[Array[Int]]]}) }
    } else if (tpe1 =:= longTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Long]]]}, ${x2t.asInstanceOf[Expr[Array[Long]]]}) }
    } else if (tpe1 =:= floatTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Float]]]}, ${x2t.asInstanceOf[Expr[Array[Float]]]}) }
    } else if (tpe1 =:= doubleTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Double]]]}, ${x2t.asInstanceOf[Expr[Array[Double]]]}) }
    } else if (tpe1 =:= charTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[Char]]]}, ${x2t.asInstanceOf[Expr[Array[Char]]]}) }
    } else if (tpe1 <:< anyRefTpe) {
      '{ java.util.Arrays.equals(${x1t.asInstanceOf[Expr[Array[AnyRef]]]}, ${x2t.asInstanceOf[Expr[Array[AnyRef]]]}) }
    } else fail(s"Can't compare arrays of type ${tpe1.show}")

  private def withDecoderFor[T: Type](methodKey: DecoderMethodKey, arg: Expr[T], in: Expr[JsonReader])
                             (f: (Expr[JsonReader], Expr[T]) => Expr[T]): Expr[T] =
    Apply(decodeMethodRefs.getOrElse(methodKey, {
      val sym = Symbol.newMethod(Symbol.spliceOwner, s"d${decodeMethodRefs.size}",
        MethodType("in" :: "default" :: Nil)(_ => TypeRepr.of[JsonReader] :: methodKey.tpe :: Nil, _ => TypeRepr.of[T]))
      val ref = Ref(sym)
      decodeMethodRefs.update(methodKey, ref)
      decodeMethodDefs.addOne(DefDef(sym, params => {
        val List(List(in, default)) = params
        new Some(f(in.asExpr.asInstanceOf[Expr[JsonReader]], default.asExpr.asInstanceOf[Expr[T]]).asTerm.changeOwner(sym))
      }))
      ref
    }), in.asTerm :: arg.asTerm :: Nil).asExpr.asInstanceOf[Expr[T]]

  private def withEncoderFor[T: Type](methodKey: EncoderMethodKey, arg: Expr[T], out: Expr[JsonWriter])
                                      (f: (Expr[JsonWriter], Expr[T]) => Expr[Unit]): Expr[Unit] =
    Apply(encodeMethodRefs.getOrElse(methodKey, {
      val sym = Symbol.newMethod(Symbol.spliceOwner, s"e${encodeMethodRefs.size}",
        MethodType("x" :: "out" :: Nil)(_ => TypeRepr.of[T] :: TypeRepr.of[JsonWriter] :: Nil, _ => unitTpe))
      val ref = Ref(sym)
      encodeMethodRefs.update(methodKey, ref)
      encodeMethodDefs.addOne(DefDef(sym, params => {
        val List(List(x, out)) = params
        new Some(f(out.asExpr.asInstanceOf[Expr[JsonWriter]], x.asExpr.asInstanceOf[Expr[T]]).asTerm.changeOwner(sym))
      }))
      ref
    }), List(arg.asTerm, out.asTerm)).asExpr.asInstanceOf[Expr[Unit]]

  private def genNullValue[T: Type](types: List[TypeRepr])(using Quotes): Expr[T] = {
    val tpe = types.head
    val implValueCodec = findImplicitValueCodec(tpe)
    if (implValueCodec.isDefined) '{ ${implValueCodec.get}.nullValue }
    else if (tpe =:= stringTpe) Literal(NullConstant()).asExpr
    else if (tpe =:= booleanTpe) Literal(BooleanConstant(false)).asExpr
    else if (tpe =:= byteTpe) Literal(ByteConstant(0)).asExpr
    else if (tpe =:= shortTpe) Literal(ShortConstant(0)).asExpr
    else if (tpe =:= intTpe) Literal(IntConstant(0)).asExpr
    else if (tpe =:= longTpe) Literal(LongConstant(0)).asExpr
    else if (tpe =:= floatTpe) Literal(FloatConstant(0f)).asExpr
    else if (tpe =:= doubleTpe) Literal(DoubleConstant(0d)).asExpr
    else if (tpe =:= charTpe) Literal(CharConstant('\u0000')).asExpr
    else if (tpe =:= TypeRepr.of[java.lang.Boolean]) '{ java.lang.Boolean.valueOf(false) }
    else if (tpe =:= TypeRepr.of[java.lang.Byte]) '{ java.lang.Byte.valueOf(0: Byte) }
    else if (tpe =:= TypeRepr.of[java.lang.Short]) '{ java.lang.Short.valueOf(0: Short) }
    else if (tpe =:= TypeRepr.of[java.lang.Integer]) '{ java.lang.Integer.valueOf(0) }
    else if (tpe =:= TypeRepr.of[java.lang.Long]) '{ java.lang.Long.valueOf(0L) }
    else if (tpe =:= TypeRepr.of[java.lang.Float]) '{ java.lang.Float.valueOf(0f) }
    else if (tpe =:= TypeRepr.of[java.lang.Double]) '{ java.lang.Double.valueOf(0d) }
    else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ java.lang.Character.valueOf('\u0000') }
    else if ({
      checkRecursionInTypes(types)
      isOption(tpe, types.tail)
    }) Ref(defn.NoneModule).asExpr
    else if (isValueClass(tpe)) {
      val tpe1 = valueClassValueType(tpe)
      tpe1.asType match { case'[t1] => getClassInfo(tpe).genNew(List(List(genNullValue[t1](tpe1 :: types).asTerm))).asExpr }
    } else if (isCollection(tpe)) {
      if (tpe <:< TypeRepr.of[mutable.BitSet]) '{ new mutable.BitSet }
      else if (tpe <:< TypeRepr.of[collection.BitSet]) withNullValueFor(tpe)('{ immutable.BitSet.empty })
      else if (tpe <:< TypeRepr.of[::[?]]) Literal(NullConstant()).asExpr
      else if (tpe <:< TypeRepr.of[List[?]] || tpe.typeSymbol == TypeRepr.of[Seq[?]].typeSymbol) '{ Nil }
      else if (tpe <:< TypeRepr.of[collection.SortedSet[?]] || tpe <:< TypeRepr.of[mutable.PriorityQueue[?]]) {
        val tpe1 = typeArg1(tpe)
        Apply(scalaCollectionEmpty(tpe, tpe1), List(summonOrdering(tpe1))).asExpr
      } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[?]] || tpe <:< TypeRepr.of[immutable.ArraySeq[?]] ||
          tpe <:< TypeRepr.of[mutable.UnrolledBuffer[?]]) {
        val tpe1 = typeArg1(tpe)
        Apply(scalaCollectionEmpty(tpe, tpe1), List(summonClassTag(tpe1))).asExpr
      } else if (tpe <:< TypeRepr.of[immutable.IntMap[?]] || tpe <:< TypeRepr.of[immutable.LongMap[?]] ||
          tpe <:< TypeRepr.of[immutable.Seq[?]] || tpe <:< TypeRepr.of[immutable.Set[?]]) withNullValueFor(tpe) {
        scalaCollectionEmpty(tpe, typeArg1(tpe)).asExpr
      } else if (tpe <:< TypeRepr.of[mutable.LongMap[?]]) scalaCollectionEmpty(tpe, typeArg1(tpe)).asExpr
      else if (tpe <:< TypeRepr.of[collection.SortedMap[?, ?]] || tpe <:< TypeRepr.of[mutable.CollisionProofHashMap[?, ?]]) {
        val tpe1 = typeArg1(tpe)
        Apply(scalaMapEmpty(tpe, tpe1, typeArg2(tpe)), List(summonOrdering(tpe1))).asExpr
      } else if (tpe <:< TypeRepr.of[immutable.TreeSeqMap[?, ?]]) withNullValueFor(tpe) {
        typeArg1(tpe).asType match { case '[t1] =>
          typeArg2(tpe).asType match { case '[t2] => '{ immutable.TreeSeqMap.empty[t1, t2] } }
        }
      } else if (tpe <:< TypeRepr.of[immutable.Map[?, ?]]) withNullValueFor(tpe) {
        scalaMapEmpty(tpe, typeArg1(tpe), typeArg2(tpe)).asExpr
      } else if (tpe <:< TypeRepr.of[collection.Map[?, ?]]) {
        scalaMapEmpty(tpe, typeArg1(tpe), typeArg2(tpe)).asExpr
      } else if (tpe <:< TypeRepr.of[Iterable[?]] || tpe <:< TypeRepr.of[Iterator[?]]) {
        scalaCollectionEmpty(tpe, typeArg1(tpe)).asExpr
      } else if (tpe <:< TypeRepr.of[Array[?]]) withNullValueFor(tpe) {
        typeArg1(tpe).asType match { case '[t1] => genNewArray[t1](Expr(0)) }
      } else if (isIArray(tpe)) withNullValueFor(tpe) {
        typeArg1(tpe).asType match { case '[t1] => '{ IArray.unsafeFromArray(${genNewArray[t1](Expr(0))}) } }
      } else '{ null.asInstanceOf[T] }
    } else if (isEnumOrModuleValue(tpe)) enumOrModuleValueRef(tpe).asExpr
    else if (TypeRepr.of[Null] <:< tpe) Literal(NullConstant()).asExpr
    else if (isOpaque(tpe) && !isNamedTuple(tpe)) {
      val sTpe = opaqueDealias(tpe)
      sTpe.asType match { case '[st] => '{ ${genNullValue[st](sTpe :: types.tail)}.asInstanceOf[T] } }
    } else if (isTypeRef(tpe)) {
      val sTpe = typeRefDealias(tpe)
      sTpe.asType match { case '[st] => '{ ${genNullValue[st](sTpe :: types.tail)}.asInstanceOf[T] } }
    } else if (isConstType(tpe)) tpe match { case ConstantType(c) => Literal(c).asExpr }
    else '{ null.asInstanceOf[T] }
  }.asInstanceOf[Expr[T]]

  private def genReadSealedClass[T: Type](types: List[TypeRepr], in: Expr[JsonReader], default: Expr[T],
                                          isStringified: Boolean)(using Quotes): Expr[T] = {
    val tpe = types.head
    val leafClasses = adtLeafClasses(tpe)
    val currentDiscriminator = cfg.discriminatorFieldName
    val discriminatorError =
      cfg.discriminatorFieldName.fold('{ $in.discriminatorError() })(n => '{ $in.discriminatorValueError(${Expr(n)}) })

    def genReadLeafClass[T: Type](subTpe: TypeRepr)(using Quotes): Expr[T] =
      val useDiscriminator = cfg.discriminatorFieldName.isDefined
      if (subTpe =:= tpe) {
        genReadNonAbstractScalaClass(getClassInfo(tpe), types, useDiscriminator, in, genNullValue[T](types))
      } else {
        val allTypes = subTpe :: types
        genReadVal(allTypes, genNullValue[T](allTypes), isStringified, useDiscriminator, in)
      }

    def genReadCollisions(subTpes: collection.Seq[TypeRepr], l: Expr[Int])(using Quotes): Expr[T] =
      subTpes.foldRight(discriminatorError.asInstanceOf[Expr[T]]) { (subTpe, acc) =>
        subTpe.asType match
          case '[st] => '{
            if ($in.isCharBufEqualsTo($l, ${Expr(discriminatorValue(subTpe))})) ${
              if (currentDiscriminator.isDefined) '{
                $in.rollbackToMark()
                ${genReadLeafClass[st](subTpe)}
              } else if (!cfg.circeLikeObjectEncoding && isEnumOrModuleValue(subTpe)) {
                enumOrModuleValueRef(subTpe).asExpr
              } else genReadLeafClass[st](subTpe)
            } else $acc
          }.asInstanceOf[Expr[T]]
      }

    def genReadSubclassesBlock(leafClasses: collection.Seq[TypeRepr], l: Expr[Int])(using Quotes): Expr[T] =
      if (leafClasses.size <= 8 && leafClasses.foldLeft(0)(_ + discriminatorValue(_).length) <= 64) {
        genReadCollisions(leafClasses, l)
      } else {
        val hashCode = (t: TypeRepr) => {
          val cs = discriminatorValue(t).toCharArray
          JsonReader.toHashCode(cs, cs.length)
        }
        val cases = groupByOrdered(leafClasses)(hashCode).map { case (hash, ts) =>
          CaseDef(Literal(IntConstant(hash)), None, genReadCollisions(ts, l).asTerm)
        }
        val lastCase = CaseDef(Wildcard(), None, discriminatorError.asTerm)
        val scrutinee = '{ $in.charBufToHashCode($l): @scala.annotation.switch }.asTerm
        Match(scrutinee, (cases :+ lastCase).toList).asExpr.asInstanceOf[Expr[T]]
      }

    checkDiscriminatorValueCollisions(tpe, leafClasses.map(discriminatorValue))

    def genReadJsObjClass(objClasses: Seq[TypeRepr], useCurrentToken: Boolean)(using Quotes): Expr[T] = {
      def checkToken(using Quotes): Expr[Boolean] =
        if (useCurrentToken) '{ $in.isCurrentToken('{') }
        else '{ $in.isNextToken('{') }

      def setMark(using Quotes): Expr[Unit] =
        if (useCurrentToken) '{
          $in.rollbackToken()
          $in.setMark()
        } else '{ $in.setMark() }

      currentDiscriminator match
        case None => '{
          if (${checkToken}) {
            val l = $in.readKeyAsCharBuf()
            val r = ${genReadSubclassesBlock(objClasses, 'l)}
            if ($in.isNextToken('}')) r
            else $in.objectEndOrCommaError()
          } else $in.readNullOrError($default, {
            if ($default == null) "expected '\"' or '{'"
            else "expected '\"' or '{' or null"
          })
        }
        case Some(discrFieldName) =>
          if (cfg.requireDiscriminatorFirst) '{
            ${setMark}
            if ($in.isNextToken('{')) {
              if ($in.isCharBufEqualsTo($in.readKeyAsCharBuf(), ${Expr(discrFieldName)})) {
                val l = $in.readStringAsCharBuf()
                ${genReadSubclassesBlock(objClasses, 'l)}
              } else $in.decodeError(${Expr("expected key: \"" + discrFieldName + '"')})
            } else $in.readNullOrTokenError($default, '{')
          } else '{
            ${setMark}
            if ($in.isNextToken('{')) {
              if ($in.skipToKey(${Expr(discrFieldName)})) {
                val l = $in.readStringAsCharBuf()
                ${genReadSubclassesBlock(objClasses, 'l)}
              } else $in.requiredFieldError(${Expr(discrFieldName)})
            } else $in.readNullOrTokenError($default, '{')
          }
    }

    if (currentDiscriminator eq None) {
      val (leafModuleClasses, leafCaseClasses) =
        leafClasses.partition(x => !cfg.circeLikeObjectEncoding && isEnumOrModuleValue(x))
      if (leafModuleClasses.nonEmpty && leafCaseClasses.nonEmpty) {
        '{
          if ($in.isNextToken('"')) {
            $in.rollbackToken()
            val l = $in.readStringAsCharBuf()
            ${genReadSubclassesBlock(leafModuleClasses, 'l)}
          } else ${genReadJsObjClass(leafCaseClasses, true)}
        }.asInstanceOf[Expr[T]]
      } else if (leafCaseClasses.nonEmpty) genReadJsObjClass(leafCaseClasses, false)
      else '{
        if ($in.isNextToken('"')) {
          $in.rollbackToken()
          val l = $in.readStringAsCharBuf()
          ${genReadSubclassesBlock(leafModuleClasses, 'l)}
        } else $in.readNullOrTokenError($default, '"')
      }.asInstanceOf[Expr[T]]
    } else genReadJsObjClass(leafClasses, false)
  }

  private def genReadNonAbstractScalaClass[T: Type](typeInfo: TypeInfo, types: List[TypeRepr], useDiscriminator: Boolean,
                                                    in: Expr[JsonReader], default: Expr[T])(using Quotes): Expr[T] = {
    val tpe = types.head
    val fields = typeInfo.fields
    val mappedNames = fields.map(_.mappedName)
    checkFieldNameCollisions(tpe, {
      if (useDiscriminator) cfg.discriminatorFieldName.fold(mappedNames)(mappedNames :+ _)
      else mappedNames
    })
    val required = fields.foldLeft(Set.newBuilder[String]) { (acc, fieldInfo) =>
      if (!((!cfg.requireDefaultFields && fieldInfo.symbol.flags.is(Flags.HasDefault)) ||
        isOption(fieldInfo.resolvedTpe, types) || isNullable(fieldInfo.resolvedTpe) ||
        (!cfg.requireCollectionFields && isCollection(fieldInfo.resolvedTpe)))) {
        acc.addOne(fieldInfo.mappedName)
      }
      acc
    }.result()
    val paramVarNum = fields.size
    val lastParamVarIndex = Math.max(0, (paramVarNum - 1) >> 5)
    val lastParamVarBits = -1 >>> -paramVarNum
    val paramVars =
      if (required.isEmpty && !cfg.checkFieldDuplication) Nil
      else (0 to lastParamVarIndex).foldLeft(new mutable.ArrayBuffer[ValDef]) { (acc, i) =>
        acc.addOne(ValDef(symbol(s"p$i", intTpe, Flags.Mutable), new Some(Literal(IntConstant {
          if (i == lastParamVarIndex) lastParamVarBits
          else -1
        }))))
      }
    val checkReqVars =
      if (required.isEmpty) Nil
      else paramVars.map {
        val nameByIndex = withFieldsByIndexFor(tpe)(mappedNames)
        val reqMasks = fields.grouped(32).toArray.map(_.foldLeft(0) {
          var i = -1
          (acc, fieldInfo) =>
            i += 1
            if (required.contains(fieldInfo.mappedName)) acc | 1 << i
            else acc
        })
        var i = -1
        nValDef =>
          i += 1
          val n = Ref(nValDef.symbol).asExpr.asInstanceOf[Expr[Int]]
          val reqMask = reqMasks(i)
          (if (reqMask == -1 || (i == lastParamVarIndex && reqMask == lastParamVarBits)) {
            val fieldName =
              if (i == 0) '{ java.lang.Integer.numberOfTrailingZeros($n) }
              else '{ java.lang.Integer.numberOfTrailingZeros($n) + ${Expr(i << 5)} }
            '{ if ($n != 0) $in.requiredFieldError(${Apply(nameByIndex, List(fieldName.asTerm)).asExpr.asInstanceOf[Expr[String]]}) }
          } else {
            val m = Expr(reqMask)
            val fieldName =
              if (i == 0) '{ java.lang.Integer.numberOfTrailingZeros($n & $m) }
              else '{ java.lang.Integer.numberOfTrailingZeros($n & $m) + ${Expr(i << 5)} }
            '{ if (($n & $m) != 0) $in.requiredFieldError(${Apply(nameByIndex, List(fieldName.asTerm)).asExpr.asInstanceOf[Expr[String]]}) }
          }).asTerm
      }
    val readVars = new mutable.ArrayBuffer[ValDef](paramVarNum)
    val readVarsMap = new mutable.HashMap[String, ValDef](paramVarNum << 1, 0.5)
    fields.foreach { fieldInfo =>
      val fTpe = fieldInfo.resolvedTpe
      fTpe.asType match
        case '[ft] =>
          val mappedName = fieldInfo.mappedName
          val sym = symbol(s"_$mappedName", fTpe, Flags.Mutable)
          val valDef = ValDef(sym, new Some(fieldInfo.defaultValue.getOrElse(genNullValue[ft](fTpe :: types).asTerm.changeOwner(sym))))
          readVars.addOne(valDef)
          readVarsMap.addOne((mappedName, valDef))
    }
    var nonTransientFieldIndex = -1
    val construct = typeInfo.genNew(typeInfo.paramLists.map(_.foldLeft(new mutable.ListBuffer[Term]) { (params, fieldInfo) =>
      params.addOne(if (fieldInfo.isTransient) {
        fieldInfo.defaultValue
          .getOrElse(fail(s"Transient field ${fieldInfo.symbol.name} in class ${tpe.show} have no default value"))
      } else {
        nonTransientFieldIndex += 1
        Ref(readVars(nonTransientFieldIndex).symbol)
      })
    }.toList))
    val readFields =
      if (useDiscriminator) cfg.discriminatorFieldName.fold(fields) { n =>
        fields :+ new FieldInfo(Symbol.noSymbol, n, Symbol.noSymbol, None, stringTpe, false, true, paramVarNum)
      } else fields
    val readBlock = new mutable.ListBuffer[Statement].addAll(readVars).addAll(paramVars)
    val discriminator =
      if (useDiscriminator && cfg.discriminatorFieldName.isDefined) {
        new Some(new ReadDiscriminator(
          if (cfg.checkFieldDuplication) {
            val valDef = ValDef(symbol("pd", booleanTpe, Flags.Mutable), new Some(Literal(BooleanConstant(true))))
            readBlock.addOne(valDef)
            new Some(Ref(valDef.symbol))
          } else None
        ))
      } else None

    def genReadCollisions(fieldInfos: collection.Seq[FieldInfo], l: Expr[Int])(using Quotes): Expr[Unit] =
      fieldInfos.foldRight(unexpectedFieldHandler(in, l)) { (fieldInfo, acc) =>
        val readValue =
          if (discriminator.isDefined && cfg.discriminatorFieldName.contains(fieldInfo.mappedName)) {
            discriminator.get.skip(in, l)
          } else {
            val fTpe = fieldInfo.resolvedTpe
            fTpe.asType match
              case '[ft] =>
                val tmpVar = Ref(readVarsMap(fieldInfo.mappedName).symbol)
                val readVal = genReadVal(fTpe :: types, tmpVar.asExpr.asInstanceOf[Expr[ft]], fieldInfo.isStringified, false, in)
                Block({
                  if (cfg.checkFieldDuplication || required.contains(fieldInfo.mappedName)) {
                    val nTerm = Ref(paramVars(fieldInfo.nonTransientFieldIndex >> 5).symbol)
                    val n = nTerm.asExpr.asInstanceOf[Expr[Int]]
                    if (cfg.checkFieldDuplication) {
                      val m = Expr(1 << fieldInfo.nonTransientFieldIndex)
                      List('{ if (($n & $m) == 0) $in.duplicatedKeyError($l) }.asTerm, Assign(nTerm, '{ $n ^ $m }.asTerm))
                    } else List(Assign(nTerm, '{ $n & ${Expr(~(1 << fieldInfo.nonTransientFieldIndex))} }.asTerm))
                  } else List(Literal(UnitConstant()))
                }, Assign(tmpVar, readVal.asTerm)).asExpr.asInstanceOf[Expr[Unit]]
          }
        '{
          if ($in.isCharBufEqualsTo($l, ${Expr(fieldInfo.mappedName)})) $readValue
          else $acc
        }
      }

    def readFieldsBlock(l: Expr[Int])(using Quotes): Expr[Unit] =
      if (readFields.size <= 8 && readFields.foldLeft(0)(_ + _.mappedName.length) <= 64) {
        genReadCollisions(readFields, l)
      } else {
        val hashCode = (fieldInfo: FieldInfo) =>
          JsonReader.toHashCode(fieldInfo.mappedName.toCharArray, fieldInfo.mappedName.length)
        val cases = groupByOrdered(readFields)(hashCode).map { case (hash, fieldInfos) =>
          CaseDef(Literal(IntConstant(hash)), None, genReadCollisions(fieldInfos, l).asTerm)
        } :+ CaseDef(Wildcard(), None, unexpectedFieldHandler(in, l).asTerm)
        Match('{ $in.charBufToHashCode($l): @scala.annotation.switch }.asTerm, cases.toList).asExpr.asInstanceOf[Expr[Unit]]
      }

    If('{ $in.isNextToken('{') }.asTerm,
      Block(readBlock.addOne('{
        if (!$in.isNextToken('}')) {
          $in.rollbackToken()
          var l = -1
          while (l < 0 || $in.isNextToken(',')) {
            l = $in.readKeyAsCharBuf()
            ${readFieldsBlock('l)}
          }
          if (!$in.isCurrentToken('}')) $in.objectEndOrCommaError()
        }
      }.asTerm.changeOwner(Symbol.spliceOwner)).addAll(checkReqVars).toList, construct),
      '{ $in.readNullOrTokenError($default, '{') }.asTerm).asExpr.asInstanceOf[Expr[T]]
  }

  private def genReadConstType[T: Type](tpe: TypeRepr, isStringified: Boolean, in: Expr[JsonReader])(using Quotes): Expr[T] = {
    tpe match
      case ConstantType(StringConstant(v)) =>
        '{ if ($in.readString(null) != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
      case ConstantType(BooleanConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(ByteConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(ShortConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(IntConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(LongConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(FloatConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(DoubleConstant(v)) =>
        if (isStringified)
          '{ if ($in.readStringAsDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
        else
          '{ if ($in.readDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }
      case ConstantType(CharConstant(v)) =>
        '{ if ($in.readChar() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }
      case _ => cannotFindValueCodecError(tpe)
  }.asInstanceOf[Expr[T]]

  private def genReadValForGrowable[G <: Growable[V]: Type, V: Type](types: List[TypeRepr], isStringified: Boolean,
                                                                     x: Expr[G], in: Expr[JsonReader])(using Quotes): Expr[Unit] =
      '{ $x.addOne(${genReadVal(types, genNullValue[V](types), isStringified, false, in)}) }

  private def genArraysCopyOf[T: Type](tpe: TypeRepr, x: Expr[Array[T]], newLen: Expr[Int])(using Quotes): Expr[Array[T]] = {
    if (tpe =:= booleanTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Boolean]]]}, $newLen) }
    else if (tpe =:= byteTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Byte]]]}, $newLen) }
    else if (tpe =:= shortTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Short]]]}, $newLen) }
    else if (tpe =:= intTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Int]]]}, $newLen) }
    else if (tpe =:= longTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Long]]]}, $newLen) }
    else if (tpe =:= floatTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Float]]]}, $newLen) }
    else if (tpe =:= doubleTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Double]]]}, $newLen) }
    else if (tpe =:= charTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[Char]]]}, $newLen) }
    else if (tpe <:< anyRefTpe) '{ java.util.Arrays.copyOf(${x.asInstanceOf[Expr[Array[AnyRef & T]]]}, $newLen) }
    else fail(s"Can't find Arrays.copyOf for ${tpe.show}")
  }.asInstanceOf[Expr[Array[T]]]

  private def genReadVal[T: Type](types: List[TypeRepr], default: Expr[T], isStringified: Boolean,
                                  useDiscriminator: Boolean, in: Expr[JsonReader])(using Quotes): Expr[T] = {
    val tpe = types.head
    val implValueCodec = findImplicitValueCodec(tpe)
    if (implValueCodec.isDefined) '{ ${implValueCodec.get.asInstanceOf[Expr[JsonValueCodec[T]]]}.decodeValue($in, $default) }
    else if (tpe =:= stringTpe) '{ $in.readString(${default.asInstanceOf[Expr[String]]}) }
    else if (tpe =:= booleanTpe) {
      if (isStringified) '{ $in.readStringAsBoolean() }
      else '{ $in.readBoolean() }
    } else if (tpe =:= byteTpe) {
      if (isStringified) '{ $in.readStringAsByte() }
      else '{ $in.readByte() }
    } else if (tpe =:= shortTpe) {
      if (isStringified) '{ $in.readStringAsShort() }
      else '{ $in.readShort() }
    } else if (tpe =:= intTpe) {
      if (isStringified) '{ $in.readStringAsInt() }
      else '{ $in.readInt() }
    } else if (tpe =:= longTpe) {
      if (isStringified) '{ $in.readStringAsLong() }
      else '{ $in.readLong() }
    } else if (tpe =:= floatTpe) {
      if (isStringified) '{ $in.readStringAsFloat() }
      else '{ $in.readFloat() }
    } else if (tpe =:= doubleTpe) {
      if (isStringified) '{ $in.readStringAsDouble() }
      else '{ $in.readDouble() }
    } else if (tpe =:= charTpe) '{ $in.readChar() }
    else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
      if (isStringified) '{ java.lang.Boolean.valueOf($in.readStringAsBoolean()) }
      else '{ java.lang.Boolean.valueOf($in.readBoolean()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Byte]) {
      if (isStringified) '{ java.lang.Byte.valueOf($in.readStringAsByte()) }
      else '{ java.lang.Byte.valueOf($in.readByte()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Short]) {
      if (isStringified) '{ java.lang.Short.valueOf($in.readStringAsShort()) }
      else '{ java.lang.Short.valueOf($in.readShort()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Integer]) {
      if (isStringified) '{ java.lang.Integer.valueOf($in.readStringAsInt()) }
      else '{ java.lang.Integer.valueOf($in.readInt()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Long]) {
      if (isStringified) '{ java.lang.Long.valueOf($in.readStringAsLong()) }
      else '{ java.lang.Long.valueOf($in.readLong()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Float]) {
      if (isStringified) '{ java.lang.Float.valueOf($in.readStringAsFloat()) }
      else '{ java.lang.Float.valueOf($in.readFloat()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Double]) {
      if (isStringified) '{ java.lang.Double.valueOf($in.readStringAsDouble()) }
      else '{ java.lang.Double.valueOf($in.readDouble()) }
    } else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ java.lang.Character.valueOf($in.readChar()) }
    else if (tpe =:= TypeRepr.of[BigInt]) {
      val tDefault = default.asInstanceOf[Expr[BigInt]]
      if (isStringified) '{ $in.readStringAsBigInt($tDefault, ${Expr(cfg.bigIntDigitsLimit)}) }
      else '{ $in.readBigInt($tDefault, ${Expr(cfg.bigIntDigitsLimit)}) }
    } else if (tpe =:= TypeRepr.of[BigDecimal]) {
      val mc = withMathContextFor(cfg.bigDecimalPrecision)
      val tDefault = default.asInstanceOf[Expr[BigDecimal]]
      val scaleLimit = Expr(cfg.bigDecimalScaleLimit)
      val digitsLimit = Expr(cfg.bigDecimalDigitsLimit)
      if (isStringified) '{ $in.readStringAsBigDecimal($tDefault, $mc, $scaleLimit, $digitsLimit) }
      else '{ $in.readBigDecimal($tDefault, $mc, $scaleLimit, $digitsLimit) }
    } else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $in.readUUID(${default.asInstanceOf[Expr[java.util.UUID]]}) }
    else if (tpe =:= TypeRepr.of[Duration]) '{ $in.readDuration(${default.asInstanceOf[Expr[Duration]]}) }
    else if (tpe =:= TypeRepr.of[Instant]) '{ $in.readInstant(${default.asInstanceOf[Expr[Instant]]}) }
    else if (tpe =:= TypeRepr.of[LocalDate]) '{ $in.readLocalDate(${default.asInstanceOf[Expr[LocalDate]]}) }
    else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $in.readLocalDateTime(${default.asInstanceOf[Expr[LocalDateTime]]}) }
    else if (tpe =:= TypeRepr.of[LocalTime]) '{ $in.readLocalTime(${default.asInstanceOf[Expr[LocalTime]]}) }
    else if (tpe =:= TypeRepr.of[MonthDay]) '{ $in.readMonthDay(${default.asInstanceOf[Expr[MonthDay]]}) }
    else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $in.readOffsetDateTime(${default.asInstanceOf[Expr[OffsetDateTime]]}) }
    else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $in.readOffsetTime(${default.asInstanceOf[Expr[OffsetTime]]}) }
    else if (tpe =:= TypeRepr.of[Period]) '{ $in.readPeriod(${default.asInstanceOf[Expr[Period]]}) }
    else if (tpe =:= TypeRepr.of[Year]) '{ $in.readYear(${default.asInstanceOf[Expr[Year]]}) }
    else if (tpe =:= TypeRepr.of[YearMonth]) '{ $in.readYearMonth(${default.asInstanceOf[Expr[YearMonth]]}) }
    else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $in.readZonedDateTime(${default.asInstanceOf[Expr[ZonedDateTime]]}) }
    else if (tpe =:= TypeRepr.of[ZoneId]) '{ $in.readZoneId(${default.asInstanceOf[Expr[ZoneId]]}) }
    else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $in.readZoneOffset(${default.asInstanceOf[Expr[ZoneOffset]]}) }
    else if ({
      checkRecursionInTypes(types)
      isOption(tpe, types.tail)
    }) {
      val tpe1 = typeArg1(tpe)
      val nullValue =
        if (cfg.skipNestedOptionValues && tpe <:< TypeRepr.of[Option[Option[?]]]) '{ new Some(None) }.asInstanceOf[Expr[T]]
        else default
      tpe1.asType match
        case '[t1] =>
          val types1 = tpe1 :: types
          val readVal1 = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
          '{
            if ($in.isNextToken('n')) $in.readNullOrError($nullValue, "expected value or null")
            else {
              $in.rollbackToken()
              new Some($readVal1)
            }
          }
    } else if (isValueClass(tpe)) {
      val tpe1 = valueClassValueType(tpe)
      tpe1.asType match
        case '[t1] =>
          val types1 = tpe1 :: types
          val readVal = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
          getClassInfo(tpe).genNew(List(List(readVal.asTerm))).asExpr
    } else {
      val isColl = isCollection(tpe)
      val methodKey = new DecoderMethodKey(tpe, isColl & isStringified, useDiscriminator)
      if (isColl) {
        if (tpe <:< TypeRepr.of[Array[?]] || tpe <:< TypeRepr.of[immutable.ArraySeq[?]] || isIArray(tpe) ||
          tpe <:< TypeRepr.of[mutable.ArraySeq[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val types1 = tpe1 :: types
          val newArrayOnChange = tpe1 match
            case _: AppliedType => true
            case _ => isValueClass(tpe1) || isOpaque(tpe1) || isTypeRef(tpe1)
          tpe1.asType match
            case '[t1] =>
              def growArray(x: Expr[Array[t1]], i: Expr[Int], l: Expr[Int])(using Quotes): Expr[Array[t1]] =
                if (newArrayOnChange) '{
                  val x1 = ${genNewArray[t1](l)}
                  java.lang.System.arraycopy($x, 0, x1, 0, $i)
                  x1
                } else genArraysCopyOf[t1](tpe1, x, l)

              def shrinkArray(x: Expr[Array[t1]], i: Expr[Int])(using Quotes): Expr[Array[t1]] =
                if (newArrayOnChange) '{
                  val x1 = ${genNewArray[t1](i)}
                  java.lang.System.arraycopy($x, 0, x1, 0, $i)
                  x1
                } else genArraysCopyOf[t1](tpe1, x, i)

              (if (tpe <:< TypeRepr.of[immutable.ArraySeq[?]]) {
                genReadArray(l => genNewArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExpr}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExpr}
                  }
                  $x($i) = ${genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)}
                }, default.asInstanceOf[Expr[immutable.ArraySeq[t1]]], (x, i, l) => '{
                  if ($i != $l) ${Assign(x.asTerm, shrinkArray(x, i).asTerm).asExpr}
                  immutable.ArraySeq.unsafeWrapArray[t1]($x)
                }.asInstanceOf[Expr[immutable.ArraySeq[t1]]], in)
              } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[?]]) {
                genReadArray(l => genNewArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExpr}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExpr}
                  }
                  $x($i) = ${genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)}
                }, default.asInstanceOf[Expr[mutable.ArraySeq[t1]]], (x, i, l) => '{
                  if ($i != $l) ${Assign(x.asTerm, shrinkArray(x, i).asTerm).asExpr}
                  mutable.ArraySeq.make[t1]($x)
                }.asInstanceOf[Expr[mutable.ArraySeq[t1]]], in)
              } else if (isIArray(tpe)) {
                genReadArray(l => genNewArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExpr}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExpr}
                  }
                  $x($i) = ${genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)}
                }, default.asInstanceOf[Expr[IArray[t1]]], (x, i, l) => '{
                  if ($i != $l) ${Assign(x.asTerm, shrinkArray(x, i).asTerm).asExpr}
                  IArray.unsafeFromArray[t1]($x)
                }.asInstanceOf[Expr[IArray[t1]]], in)
              } else {
                genReadArray(l => genNewArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExpr}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExpr}
                  }
                  $x($i) = ${genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)}
                }, default.asInstanceOf[Expr[Array[t1]]], (x, i, l) => '{
                  if ($i != $l) ${Assign(x.asTerm, shrinkArray(x, i).asTerm).asExpr}
                  $x
                }.asInstanceOf[Expr[Array[t1]]], in)
              }).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val types1 = tpe1 :: types
          tpe1.asType match
            case '[t1] =>
              val newBuilder = withNullValueFor(tpe)(scalaCollectionEmpty(tpe, tpe1).asExpr.asInstanceOf[Expr[immutable.IntMap[t1]]])
              val readVal = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
              (if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsInt() }
                  else '{ $in.readInt() }
                genReadMapAsArray(newBuilder, x => Assign(x.asTerm, '{
                  $x.updated($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() })
                }.asTerm).asExpr.asInstanceOf[Expr[Unit]], identity, in, default)
              } else {
                genReadMap(newBuilder,
                  x => Assign(x.asTerm, '{ $x.updated($in.readKeyAsInt(), $readVal) }.asTerm).asExpr.asInstanceOf[Expr[Unit]],
                  identity, in, default)
              }).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[mutable.LongMap[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val types1 = tpe1 :: types
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asInstanceOf[Expr[mutable.LongMap[t1]]]
              val newBuilder = '{
                if ($tDefault.isEmpty) $tDefault
                else ${scalaCollectionEmpty(tpe, tpe1).asExpr.asInstanceOf[Expr[mutable.LongMap[t1]]]}
              }
              val readVal = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
              (if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsLong() }
                  else '{ $in.readLong() }
                genReadMapAsArray(newBuilder,
                  x => '{ $x.update($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() }) },
                  identity, in, tDefault)
              } else {
                genReadMap(newBuilder, x => '{ $x.update($in.readKeyAsLong(), $readVal) }, identity, in, tDefault)
              }).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[immutable.LongMap[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val types1 = tpe1 :: types
          tpe1.asType match
            case '[t1] =>
              val newBuilder = withNullValueFor(tpe)(scalaCollectionEmpty(tpe, tpe1).asExpr.asInstanceOf[Expr[immutable.LongMap[t1]]])
              val readVal = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
              (if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsLong() }
                  else '{ $in.readLong() }
                genReadMapAsArray(newBuilder, x => Assign(x.asTerm, '{
                    $x.updated($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() })
                  }.asTerm).asExpr.asInstanceOf[Expr[Unit]],
                  identity, in, default.asInstanceOf[Expr[immutable.LongMap[t1]]])
              } else {
                genReadMap(newBuilder, x => Assign(x.asTerm, '{ $x.updated($in.readKeyAsLong(), $readVal) }.asTerm)
                  .asExpr.asInstanceOf[Expr[Unit]],
                  identity, in, default.asInstanceOf[Expr[immutable.LongMap[t1]]])
              }).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[mutable.Map[?, ?]] ||
            tpe <:< TypeRepr.of[mutable.CollisionProofHashMap[?, ?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          tpe1.asType match { case '[t1] =>
            tpe2.asType match { case '[t2] =>
              val types1 = tpe1 :: types
              val types2 = tpe2 :: types
              val tDefault = default.asInstanceOf[Expr[T & mutable.Map[t1, t2]]]
              val emptyMapNoArgs = scalaMapEmpty(tpe, tpe1, tpe2)
              val tEmpty =
                (if (tpe <:< TypeRepr.of[mutable.SortedMap[?, ?]] || tpe <:< TypeRepr.of[mutable.CollisionProofHashMap[?, ?]]) {
                  Apply(emptyMapNoArgs, List(summonOrdering(tpe1)))
                } else emptyMapNoArgs).asExpr.asInstanceOf[Expr[T & mutable.Map[t1, t2]]]
              val newBuilder = '{
                if ($tDefault.isEmpty) $tDefault
                else $tEmpty
              }.asInstanceOf[Expr[T & mutable.Map[t1, t2]]]
              val readVal2 = genReadVal(types2, genNullValue[t2](types2), isStringified, false, in)
              (if (cfg.mapAsArray) {
                val readVal1 = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
                genReadMapAsArray(newBuilder,
                  x => '{ $x.update($readVal1, { if ($in.isNextToken(',')) $readVal2 else $in.commaError() }) }, identity, in, tDefault)
              } else {
                genReadMap(newBuilder, x => '{ $x.update(${genReadKey[t1](types1, in)}, $readVal2) }, identity, in, tDefault)
              }).asInstanceOf[Expr[T]]
            }
          }
        } else if (tpe <:< TypeRepr.of[collection.Map[?, ?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          tpe1.asType match { case '[t1] =>
            tpe2.asType match { case '[t2] =>
              val types1 = tpe1 :: types
              val types2 = tpe2 :: types
              val readVal2 = genReadVal(types2, genNullValue[t2](types2), isStringified, false, in)
              val newBuilder =
                (if (tpe <:< TypeRepr.of[collection.SortedMap[?, ?]]) Apply(scalaMapBuilder(tpe, tpe1, tpe2), List(summonOrdering(tpe1))).asExpr
                else if (tpe <:< TypeRepr.of[immutable.TreeSeqMap[?, ?]]) '{ immutable.TreeSeqMap.newBuilder[t1, t2] }
                else scalaMapBuilder(tpe, tpe1, tpe2).asExpr).asInstanceOf[Expr[mutable.Builder[(t1, t2), T & collection.Map[t1, t2]]]]
              (if (cfg.mapAsArray) {
                val readVal1 = genReadVal(types1, genNullValue[t1](types1), isStringified, false, in)
                genReadMapAsArray(newBuilder,
                  x => '{ $x.addOne(new Tuple2($readVal1, { if ($in.isNextToken(',')) $readVal2 else $in.commaError() })): Unit},
                  x => '{ $x.result() }, in, default)
              } else {
                val readKey = genReadKey[t1](types1, in)
                genReadMap(newBuilder, x => '{ $x.addOne(new Tuple2($readKey, $readVal2)): Unit },
                  x => '{ $x.result() }, in, default)
              }).asInstanceOf[Expr[T]]
            }
          }
        } else if (tpe <:< TypeRepr.of[BitSet]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val readVal =
            if (isStringified) '{ $in.readStringAsInt() }
            else '{ $in.readInt() }
          '{
            if ($in.isNextToken('[')) {
              if ($in.isNextToken(']')) $default
              else {
                $in.rollbackToken()
                var x = new Array[Long](2)
                while ({
                  val v = $readVal
                  if (v < 0 || v >= ${Expr(cfg.bitSetValueLimit)}) $in.decodeError("illegal value for bit set")
                  val i = v >>> 6
                  if (i >= x.length) x = java.util.Arrays.copyOf(x, java.lang.Integer.highestOneBit(i) << 1)
                  x(i) = x(i) | 1L << v
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken(']')) ${
                  if (tpe <:< TypeRepr.of[mutable.BitSet]) '{ mutable.BitSet.fromBitMaskNoCopy(x) }
                  else '{ immutable.BitSet.fromBitMaskNoCopy(x) }
                } else $in.arrayEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '[')
          }.asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[mutable.Set[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asInstanceOf[Expr[T & mutable.Set[t1]]]
              val emptySetNoOrdering = scalaCollectionEmpty(tpe, tpe1)
              val emptySet =
                (if (tpe <:< TypeRepr.of[mutable.SortedSet[?]]) Apply(emptySetNoOrdering, List(summonOrdering(tpe1)))
                else emptySetNoOrdering).asExpr.asInstanceOf[Expr[T & mutable.Set[t1]]]
              genReadSet('{
                if ($tDefault.isEmpty) $tDefault
                else $emptySet
              }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), tDefault, identity, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[collection.Set[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val builderNoOrdering = scalaCollectionBuilder(tpe, tpe1)
              val builder =
                (if (tpe <:< TypeRepr.of[collection.SortedSet[?]]) Apply(builderNoOrdering, List(summonOrdering(tpe1)))
                else builderNoOrdering).asExpr.asInstanceOf[Expr[mutable.Builder[t1, T & collection.Set[t1]]]]
              genReadSet(builder, b => genReadValForGrowable(tpe1 :: types, isStringified, b, in),
                default.asInstanceOf[Expr[T & collection.Set[t1]]], b => '{ $b.result() }, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[::[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asInstanceOf[Expr[::[t1]]]
              '{
                if ($in.isNextToken('[')) {
                  if ($in.isNextToken(']')) {
                    if ($tDefault ne null) $tDefault
                    else $in.decodeError("expected non-empty JSON array")
                  } else {
                    $in.rollbackToken()
                    val x = new mutable.ListBuffer[t1]
                    while ({
                      ${genReadValForGrowable(tpe1 :: types, isStringified, 'x, in)}
                      $in.isNextToken(',')
                    }) ()
                    if ($in.isCurrentToken(']')) x.toList.asInstanceOf[::[t1]]
                    else $in.arrayEndOrCommaError()
                  }
                } else {
                  if ($tDefault ne null) $in.readNullOrTokenError($tDefault, '[')
                  else $in.decodeError("expected non-empty JSON array")
                }
              }.asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[List[?]] || tpe.typeSymbol == TypeRepr.of[Seq[?]].typeSymbol) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genReadCollection('{ new mutable.ListBuffer[t1] }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in),
                default, x => '{ $x.toList }, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[mutable.ListBuffer[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asInstanceOf[Expr[mutable.ListBuffer[t1]]]
              genReadCollection('{
                if ($tDefault.isEmpty) $tDefault
                else new mutable.ListBuffer[t1]
              }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), tDefault, identity, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[Vector[?]] || tpe.typeSymbol == TypeRepr.of[IndexedSeq[?]].typeSymbol) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genReadCollection('{ new immutable.VectorBuilder[t1] }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in),
                default, x => '{ $x.result() }, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[mutable.Iterable[?] & mutable.Growable[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val emptyCollectionNoArgs = scalaCollectionEmpty(tpe, tpe1)
              val emptyCollection =
                (if (tpe <:< TypeRepr.of[mutable.UnrolledBuffer[?]]) {
                  Apply(emptyCollectionNoArgs, List(summonClassTag(tpe1)))
                } else if (tpe <:< TypeRepr.of[mutable.PriorityQueue[?]]) {
                  Apply(emptyCollectionNoArgs, List(summonOrdering(tpe1)))
                } else emptyCollectionNoArgs).asExpr.asInstanceOf[Expr[T & mutable.Growable[t1]]]
              genReadCollection('{
                if (${default.asInstanceOf[Expr[Iterable[?]]]}.isEmpty) $default
                else $emptyCollection
              }.asInstanceOf[Expr[T & mutable.Growable[t1]]],
                x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), default, identity, in).asInstanceOf[Expr[T]]
        } else if (tpe <:< TypeRepr.of[Iterable[?]] || tpe <:< TypeRepr.of[Iterator[?]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genReadCollection(scalaCollectionBuilder(tpe, tpe1).asExpr.asInstanceOf[Expr[mutable.Builder[t1, T]]],
                x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), default, x => '{ $x.result() }, in)
        } else cannotFindValueCodecError(tpe)
      } else if (tpe <:< TypeRepr.of[Enumeration#Value]) withDecoderFor(methodKey, default, in) { (in, default) =>
        if (cfg.useScalaEnumValueId) {
          val ec = withScalaEnumCacheFor[Int, T & Enumeration#Value](tpe)
          if (isStringified) '{
            if ($in.isNextToken('"')) {
              $in.rollbackToken()
              val i = $in.readStringAsInt()
              var x = $ec.get(i)
              if (x eq null) {
                x = ${findScala2EnumerationById[T & Enumeration#Value](tpe, 'i)}.getOrElse($in.enumValueError(i.toString))
                $ec.put(i, x)
              }
              x
            } else $in.readNullOrTokenError($default, '"')
          } else '{
            val t = $in.nextToken()
            if (t >= '0' && t <= '9') {
              $in.rollbackToken()
              val i = $in.readInt()
              var x = $ec.get(i)
              if (x eq null) {
                x = ${findScala2EnumerationById[T & Enumeration#Value](tpe, 'i)}.getOrElse($in.decodeError("illegal enum value " + i))
                $ec.put(i, x)
              }
              x
            } else $in.readNullOrError($default, "expected digit")
          }
        } else {
          val ec = withScalaEnumCacheFor[String, T & Enumeration#Value](tpe)
          '{
            if ($in.isNextToken('"')) {
              $in.rollbackToken()
              val s = $in.readString(null)
              var x = $ec.get(s)
              if (${'x.asInstanceOf[Expr[AnyRef]]} eq null) {
                x = ${findScala2EnumerationByName[T & Enumeration#Value](tpe,'s)}.getOrElse($in.enumValueError(s.length))
                $ec.put(s, x)
              }
              x
            } else $in.readNullOrTokenError($default, '"')
          }
        }
      } else if (isJavaEnum(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        '{
          if ($in.isNextToken('"')) {
            $in.rollbackToken()
            val l = $in.readStringAsCharBuf()
            ${genReadJavaEnumValue(javaEnumValues(tpe), '{ $in.enumValueError(l) }, in, 'l) }
          } else $in.readNullOrTokenError($default, '"')
        }
      } else if (isNamedTuple(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        genReadNonAbstractScalaClass(getNamedTupleInfo(tpe), types, useDiscriminator, in, default)
      } else if (isTuple(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        var tTpe = tpe
        val tTypeArgs =
          if (isGenericTuple(tpe)) {
            val typeArgs = genericTupleTypeArgs(tpe.asType)
            tTpe = normalizeGenericTuple(typeArgs)
            typeArgs
          } else typeArgs(tpe)
        var i = 0
        val valDefs = tTypeArgs.map { fTpe =>
          fTpe.asType match
            case '[ft] =>
              i += 1
              val allTypes = fTpe :: types
              val readVal = genReadVal(allTypes, genNullValue[ft](allTypes), isStringified, false, in)
              val rhs =
                if (i == 1) readVal
                else '{
                  if ($in.isNextToken(',')) $readVal
                  else $in.commaError()
                }
              ValDef(symbol(s"_$i", fTpe), new Some(rhs.asTerm))
        }
        val size = i
        val readCreateBlock = Block(valDefs, '{
          if ($in.isNextToken(']')) ${
            if (size == 0) Expr(EmptyTuple)
            else if (size > 22) {
              val arraySym = symbol("xs", arrayOfAnyTpe)
              val arrayRef = Ref(arraySym)
              val arrayValDef = ValDef(arraySym, new Some(Apply(newArrayOfAny, List(Literal(IntConstant(size))))))
              val assignments = valDefs.map {
                val arrayUpdate = Select(arrayRef, defn.Array_update)
                var i = - 1
                valDef =>
                  i += 1
                  Apply(arrayUpdate, List(Literal(IntConstant(i)), Ref(valDef.symbol)))
              }
              val block = Block(arrayValDef :: assignments, arrayRef)
              val iArray = TypeApply(Select(block, asInstanceOfMethod), List(Inferred(iArrayOfAnyRefTpe)))
              TypeApply(Select(Apply(fromIArrayMethod, List(iArray)), asInstanceOfMethod), List(Inferred(tTpe))).asExpr
            } else {
              val constructorNoTypes = Select(New(Inferred(tTpe)), tTpe.typeSymbol.primaryConstructor)
              Apply(TypeApply(constructorNoTypes, tTypeArgs.map(Inferred(_))), valDefs.map(x => Ref(x.symbol))).asExpr
            }
          } else $in.arrayEndError()
        }.asTerm.changeOwner(Symbol.spliceOwner)).asExpr
        '{
          if ($in.isNextToken('[')) $readCreateBlock
          else $in.readNullOrTokenError($default, '[')
        }.asInstanceOf[Expr[T]]
      } else if (isEnumOrModuleValue(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        '{
          if ($in.isNextToken('{')) {
            $in.rollbackToken()
            $in.skip()
            ${enumOrModuleValueRef(tpe).asExpr}
          } else $in.readNullOrTokenError($default, '{')
        }.asInstanceOf[Expr[T]]
      } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        genReadSealedClass(types, in, default, isStringified)
      } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
        genReadNonAbstractScalaClass(getClassInfo(tpe), types, useDiscriminator, in, default)
      } else if (isOpaque(tpe)) {
        val sTpe = opaqueDealias(tpe)
        sTpe.asType match
          case '[s] =>
            val newDefault = '{ $default.asInstanceOf[s] }.asInstanceOf[Expr[s]]
            '{ ${genReadVal[s](sTpe :: types.tail, newDefault, isStringified, useDiscriminator, in)}.asInstanceOf[T] }
      } else if (isTypeRef(tpe)) {
        val sTpe = typeRefDealias(tpe)
        sTpe.asType match
          case '[s] =>
            val newDefault = '{ $default.asInstanceOf[s] }.asInstanceOf[Expr[s]]
            '{ ${genReadVal[s](sTpe :: types.tail, newDefault, isStringified, useDiscriminator, in)}.asInstanceOf[T] }
      } else if (isConstType(tpe)) genReadConstType(tpe, isStringified, in)
      else cannotFindValueCodecError(tpe)
    }
  }.asInstanceOf[Expr[T]]

  private def genWriteNonAbstractScalaClass[T: Type](x: Expr[T], typeInfo: TypeInfo, types: List[TypeRepr],
                                                     optDiscriminator: Option[WriteDiscriminator],
                                                     out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
    val tpe = types.head
    val (valDefs, valRef) =
      typeInfo match
        case namedTupleInfo: NamedTupleInfo =>
          val sym = symbol("t", namedTupleInfo.tupleTpe)
          val valDef = ValDef(sym, new Some(Apply(TypeApply(toTupleMethod, tpe.typeArgs.map(Inferred(_))), List(x.asTerm))))
          (List(valDef), Ref(sym))
        case _ => (Nil, x.asTerm)
    lazy val productElement = Select.unique(valRef, "productElement")
    var writeFields = typeInfo.fields.map { fieldInfo =>
      val fDefault =
        if (cfg.transientDefault) fieldInfo.defaultValue
        else None
      val fTpe = fieldInfo.resolvedTpe
      val allTypes = fTpe :: types
      val isStringified = fieldInfo.isStringified
      val getter = {
        typeInfo match
          case namedTupleInfo: NamedTupleInfo =>
            val i = fieldInfo.nonTransientFieldIndex
            if (namedTupleInfo.isGeneric) {
              TypeApply(Select(Apply(productElement, List(Literal(IntConstant(i)))), asInstanceOfMethod), List(Inferred(fTpe)))
            } else Select.unique(valRef, s"_${i + 1}")
          case _ => Select(valRef, fieldInfo.getterOrField)
      }.asExpr
      (fTpe.asType match { case '[ft] =>
        fDefault match {
          case Some(d) =>
            if (cfg.transientEmpty && fTpe <:< TypeRepr.of[Iterable[?]]) '{
              val v = ${getter.asInstanceOf[Expr[ft & Iterable[?]]]}
              if (!v.isEmpty && v != ${d.asExpr.asInstanceOf[Expr[ft]]}) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientEmpty && fTpe <:< TypeRepr.of[Iterator[?]]) '{
              val v = ${getter.asInstanceOf[Expr[ft & Iterator[?]]]}
              if (v.hasNext && v != ${d.asExpr.asInstanceOf[Expr[ft]]}) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientNone && isOption(fTpe, types)) {
              val tpe1 = typeArg1(fTpe)
              tpe1.asType match
                case '[t1] => '{
                  val v = ${getter.asInstanceOf[Expr[Option[t1]]]}
                  if ((v ne None) && v != ${d.asExpr.asInstanceOf[Expr[ft]]}) {
                    ${genWriteConstantKey(fieldInfo.mappedName, out)}
                    ${genWriteVal('{v.get}, tpe1 :: allTypes, fieldInfo.isStringified, None, out)}
                  }
                }
            } else if (cfg.transientNull && isNullable(fTpe)) '{
              val v = ${getter.asInstanceOf[Expr[ft]]}
              if ((v != null) && v != ${d.asExpr.asInstanceOf[Expr[ft]]}) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (fTpe <:< TypeRepr.of[Array[?]]) {
              def cond(v: Expr[Array[?]])(using Quotes): Expr[Boolean] =
                val da = d.asExpr.asInstanceOf[Expr[Array[?]]]
                if (cfg.transientEmpty) '{ $v.length != 0 && !${withEqualsFor(fTpe, v, da)((x1, x2) => genArrayEquals(fTpe, x1, x2))} }
                else '{ !${withEqualsFor(fTpe, v, da)((x1, x2) => genArrayEquals(fTpe, x1, x2))} }

              '{
                val v = ${getter.asInstanceOf[Expr[ft & Array[?]]]}
                if (${cond('v)}) {
                  ${genWriteConstantKey(fieldInfo.mappedName, out)}
                  ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
                }
              }
            } else if (isIArray(fTpe)) {
              typeArg1(fTpe).asType match
                case '[ft1] => {
                  def cond(v: Expr[IArray[ft1]])(using Quotes): Expr[Boolean] =
                    val da = d.asExpr.asInstanceOf[Expr[IArray[ft1]]]
                    if (cfg.transientEmpty) '{ $v.length != 0 && !${withEqualsFor(fTpe, v, da)((x1, x2) => genArrayEquals(fTpe, x1, x2))} }
                    else '{ !${withEqualsFor(fTpe, v, da)((x1, x2) => genArrayEquals(fTpe, x1, x2))} }

                  '{
                    val v = ${getter.asInstanceOf[Expr[IArray[ft1]]]}
                    if (${cond('v)}) {
                      ${genWriteConstantKey(fieldInfo.mappedName, out)}
                      ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
                    }
                  }
                }
            } else '{
              val v = ${getter.asInstanceOf[Expr[ft]]}
              if (v != ${d.asExpr.asInstanceOf[Expr[ft]]}) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            }
          case None =>
            if (cfg.transientEmpty && fTpe <:< TypeRepr.of[Iterable[?]]) '{
              val v = ${getter.asInstanceOf[Expr[ft & Iterable[?]]]}
              if (!v.isEmpty) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientEmpty && fTpe <:< TypeRepr.of[Iterator[?]]) '{
              val v = ${getter.asInstanceOf[Expr[ft & Iterator[?]]]}
              if (v.hasNext) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientNone && isOption(fTpe, types)) {
              val tpe1 = typeArg1(fTpe)
              tpe1.asType match
                case '[tf] => '{
                  val v = ${getter.asInstanceOf[Expr[Option[tf]]]}
                  if (v ne None) {
                    ${genWriteConstantKey(fieldInfo.mappedName, out)}
                    ${genWriteVal('{ v.get }, tpe1 :: allTypes, fieldInfo.isStringified, None, out)}
                  }
                }
            } else if (cfg.transientNull && isNullable(fTpe)) '{
              val v = ${getter.asInstanceOf[Expr[ft]]}
              if (v != null) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientEmpty && fTpe <:< TypeRepr.of[Array[?]]) '{
              val v = ${getter.asInstanceOf[Expr[ft & Array[?]]]}
              if (v.length != 0) {
                ${genWriteConstantKey(fieldInfo.mappedName, out)}
                ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
              }
            } else if (cfg.transientEmpty && isIArray(fTpe)) {
              typeArg1(fTpe).asType match
                case '[ft1] => '{
                  val v = ${getter.asInstanceOf[Expr[IArray[ft1]]]}
                  if (v.length != 0) {
                    ${genWriteConstantKey(fieldInfo.mappedName, out)}
                    ${genWriteVal('v, allTypes, fieldInfo.isStringified, None, out)}
                  }
                }
            } else '{
              ${genWriteConstantKey(fieldInfo.mappedName, out)}
              ${genWriteVal(getter.asInstanceOf[Expr[ft]], allTypes, fieldInfo.isStringified, None, out)}
            }
        }
      }).asTerm
    }
    if (optDiscriminator.isDefined) writeFields = optDiscriminator.get.write(out).asTerm :: writeFields
    val block = Block('{ $out.writeObjectStart() }.asTerm :: writeFields, '{ $out.writeObjectEnd() }.asTerm)
    (if (valDefs eq Nil) block
    else Block(valDefs, block.changeOwner(Symbol.spliceOwner))).asExpr.asInstanceOf[Expr[Unit]]

  private def getWriteConstType(tpe: TypeRepr, isStringified: Boolean, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
    tpe match
      case ConstantType(StringConstant(v)) => genWriteConstantVal(v, out)
      case ConstantType(BooleanConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(ByteConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(ShortConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(IntConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(LongConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(FloatConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(DoubleConstant(v)) =>
        if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
        else '{ $out.writeVal(${Expr(v)}) }
      case ConstantType(CharConstant(v)) => '{ $out.writeVal(${Expr(v)}) }
      case _ => cannotFindValueCodecError(tpe)

  private def genWriteVal[T: Type](m: Expr[T], types: List[TypeRepr], isStringified: Boolean,
                                   optWriteDiscriminator: Option[WriteDiscriminator],
                                   out: Expr[JsonWriter])(using Quotes): Expr[Unit] = {
    val tpe = types.head
    val implValueCodec = findImplicitValueCodec(tpe)
    if (implValueCodec.isDefined) '{ ${implValueCodec.get.asInstanceOf[Expr[JsonValueCodec[T]]]}.encodeValue($m, $out) }
    else if (tpe =:= stringTpe) '{ $out.writeVal(${m.asInstanceOf[Expr[String]]}) }
    else if (tpe =:= booleanTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Boolean]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Boolean]]}) }
    } else if (tpe =:= byteTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Byte]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Byte]]}) }
    } else if (tpe =:= shortTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Short]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Short]]}) }
    } else if (tpe =:= intTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Int]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Int]]}) }
    } else if (tpe =:= longTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Long]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Long]]}) }
    } else if (tpe =:= floatTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Float]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Float]]}) }
    } else if (tpe =:= doubleTpe) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[Double]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[Double]]}) }
    } else if (tpe =:= charTpe) '{ $out.writeVal(${m.asInstanceOf[Expr[Char]]}) }
    else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Boolean]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Boolean]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Byte]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Byte]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Byte]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Short]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Short]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Short]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Integer]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Integer]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Integer]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Long]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Long]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Long]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Float]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Float]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Float]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Double]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[java.lang.Double]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Double]]}) }
    } else if (tpe =:= TypeRepr.of[java.lang.Character]) {
      '{ $out.writeVal(${m.asInstanceOf[Expr[java.lang.Character]]}) }
    } else if (tpe =:= TypeRepr.of[BigInt]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[BigInt]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[BigInt]]}) }
    } else if (tpe =:= TypeRepr.of[BigDecimal]) {
      if (isStringified) '{ $out.writeValAsString(${m.asInstanceOf[Expr[BigDecimal]]}) }
      else '{ $out.writeVal(${m.asInstanceOf[Expr[BigDecimal]]}) }
    } else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $out.writeVal(${m.asInstanceOf[Expr[java.util.UUID]]}) }
    else if (tpe =:= TypeRepr.of[Duration]) '{ $out.writeVal(${m.asInstanceOf[Expr[Duration]]}) }
    else if (tpe =:= TypeRepr.of[Instant]) '{ $out.writeVal(${m.asInstanceOf[Expr[Instant]]}) }
    else if (tpe =:= TypeRepr.of[LocalDate]) '{ $out.writeVal(${m.asInstanceOf[Expr[LocalDate]]}) }
    else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $out.writeVal(${m.asInstanceOf[Expr[LocalDateTime]]}) }
    else if (tpe =:= TypeRepr.of[LocalTime]) '{ $out.writeVal(${m.asInstanceOf[Expr[LocalTime]]}) }
    else if (tpe =:= TypeRepr.of[MonthDay]) '{ $out.writeVal(${m.asInstanceOf[Expr[MonthDay]]}) }
    else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $out.writeVal(${m.asInstanceOf[Expr[OffsetDateTime]]}) }
    else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $out.writeVal(${m.asInstanceOf[Expr[OffsetTime]]}) }
    else if (tpe =:= TypeRepr.of[Period]) '{ $out.writeVal(${m.asInstanceOf[Expr[Period]]}) }
    else if (tpe =:= TypeRepr.of[Year]) '{ $out.writeVal(${m.asInstanceOf[Expr[Year]]}) }
    else if (tpe =:= TypeRepr.of[YearMonth]) '{ $out.writeVal(${m.asInstanceOf[Expr[YearMonth]]}) }
    else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $out.writeVal(${m.asInstanceOf[Expr[ZonedDateTime]]}) }
    else if (tpe =:= TypeRepr.of[ZoneId]) '{ $out.writeVal(${m.asInstanceOf[Expr[ZoneId]]}) }
    else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $out.writeVal(${m.asInstanceOf[Expr[ZoneOffset]]}) }
    else if ({
      checkRecursionInTypes(types)
      isOption(tpe, types.tail)
    }) {
      val tpe1 = typeArg1(tpe)
      tpe1.asType match
        case '[t1] =>
          val x = m.asInstanceOf[Expr[Option[t1]]]
          '{
            if ($x ne None) ${genWriteVal('{ $x.get }, tpe1 :: types, isStringified, None, out)}
            else $out.writeNull()
          }
    } else if (isValueClass(tpe)) {
      val vTpe = valueClassValueType(tpe)
      val vSym = valueClassValueSymbol(tpe)
      vTpe.asType match
        case '[vt] =>
          genWriteVal(Select(m.asTerm, vSym).asExpr.asInstanceOf[Expr[vt]], vTpe :: types, isStringified, None, out)
    } else {
      val isColl = isCollection(tpe)
      val methodKey = new EncoderMethodKey(tpe, isColl & isStringified, optWriteDiscriminator.map(x => (x.fieldName, x.fieldValue)))
      if (isColl) {
        if (tpe <:< TypeRepr.of[Array[?]] || tpe <:< TypeRepr.of[immutable.ArraySeq[?]] || isIArray(tpe) ||
          tpe <:< TypeRepr.of[mutable.ArraySeq[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val types1 = tpe1 :: types
              if (tpe <:< TypeRepr.of[immutable.ArraySeq[?]]) {
                val tx = x.asInstanceOf[Expr[immutable.ArraySeq[t1]]]
                '{
                  $out.writeArrayStart()
                  val xs = $tx.unsafeArray.asInstanceOf[Array[t1]]
                  val l = xs.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ xs(i) }, types1, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[?]]) {
                val tx = x.asInstanceOf[Expr[mutable.ArraySeq[t1]]]
                '{
                  $out.writeArrayStart()
                  val xs = $tx.array.asInstanceOf[Array[t1]]
                  val l = xs.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ xs(i) }, types1, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              } else if (isIArray(tpe)) {
                val tx = x.asInstanceOf[Expr[IArray[t1]]]
                '{
                  $out.writeArrayStart()
                  val l = $tx.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ $tx(i) }, types1, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              } else {
                val tx = x.asInstanceOf[Expr[Array[t1]]]
                '{
                  $out.writeArrayStart()
                  val l = $tx.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ $tx(i) }, types1, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              }
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[?]] || tpe <:< TypeRepr.of[mutable.LongMap[?]] ||
            tpe <:< TypeRepr.of[immutable.LongMap[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              def writeVal2(out: Expr[JsonWriter], v: Expr[t1])(using Quotes): Expr[Unit] =
                genWriteVal(v, tpe1 :: types, isStringified, None, out)

              if (tpe <:< TypeRepr.of[immutable.IntMap[?]]) {
                val tx = x.asInstanceOf[Expr[immutable.IntMap[t1]]]
                if (cfg.mapAsArray) {
                  def writeVal1(out: Expr[JsonWriter], k: Expr[Int])(using Quotes): Expr[Unit] =
                    if (isStringified) '{ $out.writeValAsString($k) }
                    else '{ $out.writeVal($k) }

                  genWriteMapAsArrayScala(tx, writeVal1, writeVal2, out)
                } else genWriteMapScala(tx, (out, k) => '{ $out.writeKey($k) }, writeVal2, out)
              } else {
                val tx = x.asInstanceOf[Expr[collection.Map[Long, t1]]]
                if (cfg.mapAsArray) {
                  def writeVal1(out: Expr[JsonWriter], k: Expr[Long])(using Quotes): Expr[Unit] =
                    if (isStringified) '{ $out.writeValAsString($k) }
                    else '{ $out.writeVal($k) }

                  genWriteMapAsArrayScala(tx, writeVal1, writeVal2, out)
                } else genWriteMapScala(tx, (out, k) => '{ $out.writeKey($k) }, writeVal2, out)
              }
        } else if (tpe <:< TypeRepr.of[collection.Map[?, ?]] ||
            tpe <:< TypeRepr.of[mutable.CollisionProofHashMap[?, ?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          tpe1.asType match { case '[t1] =>
            tpe2.asType match { case '[t2] =>
              def writeVal2(out: Expr[JsonWriter], v: Expr[t2])(using Quotes): Expr[Unit] =
                genWriteVal(v, tpe2 :: types, isStringified, None, out)

              val tx = x.asInstanceOf[Expr[collection.Map[t1, t2]]]
              val types1 = tpe1 :: types
              if (cfg.mapAsArray) {
                genWriteMapAsArrayScala(tx, (out, k) => genWriteVal(k, types1, isStringified, None, out), writeVal2, out)
              } else genWriteMapScala(tx, (out, k) => genWriteKey(k, types1, out), writeVal2, out)
            }
          }
        } else if (tpe <:< TypeRepr.of[BitSet]) withEncoderFor(methodKey, m, out) { (out, x) =>
          genWriteArray(x.asInstanceOf[Expr[BitSet]], (out, x1) => {
            if (isStringified) '{ $out.writeValAsString($x1) }
            else '{ $out.writeVal($x1) }
          }, out)
        } else if (tpe <:< TypeRepr.of[List[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tx = x.asInstanceOf[Expr[List[t1]]]
              '{
                $out.writeArrayStart()
                val n = Nil
                var l = $tx
                while (l ne n) {
                  ${genWriteVal('{ l.head }, tpe1 :: types, isStringified, None, out)}
                  l = l.tail
                }
                $out.writeArrayEnd()
              }
        } else if (tpe <:< TypeRepr.of[IndexedSeq[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tx = x.asInstanceOf[Expr[IndexedSeq[t1]]]
              val types1 = tpe1 :: types
              '{
                $out.writeArrayStart()
                val l = $tx.length
                if (l <= 32) {
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ $tx(i) }, types1, isStringified, None, out)}
                    i += 1
                  }
                } else $tx.foreach(x => ${genWriteVal('x, types1, isStringified, None, out)})
                $out.writeArrayEnd()
              }
        } else if (tpe <:< TypeRepr.of[Iterable[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genWriteArray(x.asInstanceOf[Expr[Iterable[t1]]],
                (out, x1) => genWriteVal(x1, tpe1 :: types, isStringified, None, out), out)
        } else if (tpe <:< TypeRepr.of[Iterator[?]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case'[t1] =>
              genWriteArray2(x.asInstanceOf[Expr[Iterator[t1]]],
                (out, x1) => genWriteVal(x1, tpe1 :: types, isStringified, None, out), out)
        } else cannotFindValueCodecError(tpe)
      } else if (tpe <:< TypeRepr.of[Enumeration#Value]) withEncoderFor(methodKey, m, out) { (out, x) =>
        val tx = x.asInstanceOf[Expr[Enumeration#Value]]
        if (cfg.useScalaEnumValueId) {
          if (isStringified) '{ $out.writeValAsString($tx.id) }
          else '{ $out.writeVal($tx.id) }
        } else '{ $out.writeVal($tx.toString) }
      } else if (isJavaEnum(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
        val es = javaEnumValues(tpe)
        val encodingRequired = es.exists(e => isEncodingRequired(e.name))
        if (es.exists(_.transformed)) {
          val cases = es.map(e => CaseDef(Ref(e.value), None, Expr(e.name).asTerm)) :+
            CaseDef(Wildcard(), None, '{ $out.encodeError("illegal enum value: null") }.asTerm)
          val matching = Match(x.asTerm, cases).asExpr.asInstanceOf[Expr[String]]
          if (encodingRequired) '{ $out.writeVal($matching) }
          else '{ $out.writeNonEscapedAsciiVal($matching) }
        } else {
          val tx = x.asInstanceOf[Expr[java.lang.Enum[?]]]
          if (encodingRequired) '{ $out.writeVal($tx.name) }
          else '{ $out.writeNonEscapedAsciiVal($tx.name) }
        }
      } else if (isNamedTuple(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
        genWriteNonAbstractScalaClass(x, getNamedTupleInfo(tpe), types, optWriteDiscriminator, out)
      } else if (isTuple(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
        var tTpe = tpe
        val tTypeArgs =
          if (isGenericTuple(tpe)) {
            val typeArgs = genericTupleTypeArgs(tpe.asType)
            tTpe = normalizeGenericTuple(typeArgs)
            typeArgs
          } else typeArgs(tpe)
        val writeFields = tTypeArgs.map {
          val xTerm = TypeApply(Select(x.asTerm, asInstanceOfMethod), List(Inferred(tTpe)))
          lazy val productElement = Select.unique(xTerm, "productElement")
          val isGeneric = tTypeArgs.size > 22
          var i = 0
          fTpe =>
            i += 1
            fTpe.asType match
              case '[ft] =>
                val getter =
                  (if (isGeneric) {
                    TypeApply(Select(Apply(productElement, List(Literal(IntConstant(i - 1)))), asInstanceOfMethod), List(Inferred(fTpe)))
                  } else Select.unique(xTerm, s"_$i")).asExpr.asInstanceOf[Expr[ft]]
                genWriteVal(getter, fTpe :: types, isStringified, None, out).asTerm
        }
        Block('{ $out.writeArrayStart() }.asTerm :: writeFields, '{ $out.writeArrayEnd() }.asTerm).asExpr.asInstanceOf[Expr[Unit]]
      } else if (isEnumOrModuleValue(tpe) && !(cfg.alwaysEmitDiscriminator && hasSealedParent(tpe))) withEncoderFor(methodKey, m, out) { (out, _) =>
        '{
          $out.writeObjectStart()
          ${optWriteDiscriminator.fold('{})(_.write(out))}
          $out.writeObjectEnd()
        }
      } else if (isSealedClass(tpe) || (cfg.alwaysEmitDiscriminator && hasSealedParent(tpe))) withEncoderFor(methodKey, m, out) { (out, x) =>
        def genWriteLeafClass(subTpe: TypeRepr, discriminator: Option[WriteDiscriminator], vx: Term): Expr[Unit] =
          subTpe.asType match
            case '[st] =>
              val vxExpr = vx.asExpr.asInstanceOf[Expr[st]]
              if (subTpe =:= tpe) {
                genWriteNonAbstractScalaClass(vxExpr, getClassInfo(tpe), types, discriminator, out)
              } else genWriteVal(vxExpr, subTpe :: types, isStringified, discriminator, out)

        val leafClasses = adtLeafClasses(tpe)
        val writeSubclasses = cfg.discriminatorFieldName.fold {
          leafClasses.map { subTpe =>
            if (!cfg.circeLikeObjectEncoding && isEnumOrModuleValue(subTpe)) {
              CaseDef(Typed(x.asTerm, Inferred(subTpe)), None,
                genWriteConstantVal(discriminatorValue(subTpe), out).asTerm)
            } else {
              val vxSym = Symbol.newBind(Symbol.spliceOwner, "vx", Flags.EmptyFlags, subTpe)
              CaseDef(Bind(vxSym, Typed(Wildcard(), Inferred(subTpe))), None, '{
                $out.writeObjectStart()
                ${genWriteConstantKey(discriminatorValue(subTpe), out)}
                ${genWriteLeafClass(subTpe, None, Ref(vxSym))}
                $out.writeObjectEnd()
              }.asTerm)
            }
          }
        } { discrFieldName =>
          leafClasses.map { subTpe =>
            val vxSym = Symbol.newBind(Symbol.spliceOwner, "vx", Flags.EmptyFlags, subTpe)
            val writeDiscriminator = WriteDiscriminator(discrFieldName, discriminatorValue(subTpe))
            CaseDef(Bind(vxSym, Typed(Wildcard(), Inferred(subTpe))), None,
              genWriteLeafClass(subTpe, new Some(writeDiscriminator), Ref(vxSym)).asTerm)
          }
        }
        Match('{$x: @scala.unchecked}.asTerm, writeSubclasses.toList).asExpr.asInstanceOf[Expr[Unit]]
      } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
        genWriteNonAbstractScalaClass(x, getClassInfo(tpe), types, optWriteDiscriminator, out)
      } else if (isConstType(tpe)) getWriteConstType(tpe, isStringified, out)
      else if (isOpaque(tpe)) {
        val sTpe = opaqueDealias(tpe)
        sTpe.asType match
          case '[s] =>
            genWriteVal[s]('{ $m.asInstanceOf[s] }, sTpe :: types.tail, isStringified, optWriteDiscriminator, out)
      } else if (isTypeRef(tpe)) {
        val sTpe = typeRefDealias(tpe)
        sTpe.asType match
          case '[s] =>
            genWriteVal[s]('{ $m.asInstanceOf[s] }, sTpe :: types.tail, isStringified, optWriteDiscriminator, out)
      } else cannotFindValueCodecError(tpe)
    }
  }.asInstanceOf[Expr[Unit]]

  private def isEncodingRequired(s: String): Boolean =
    val len = s.length
    var i = 0
    while (i < len && JsonWriter.isNonEscapedAscii(s.charAt(i))) i += 1
    i != len

  private def groupByOrdered[A, K](xs: collection.Seq[A])(f: A => K): collection.Seq[(K, collection.Seq[A])] =
    xs.foldLeft(new mutable.LinkedHashMap[K, ArrayBuffer[A]]) { (m, x) =>
      m.getOrElseUpdate(f(x), new ArrayBuffer[A]).addOne(x)
      m
    }.toArray

  private def duplicated[A](xs: collection.Seq[A]): collection.Seq[A] = xs.filter {
    val seen = new mutable.HashSet[A]
    x => !seen.add(x)
  }

  private def distinct[A](xs: Seq[A]): Seq[A] = xs.filter {
    val seen = new mutable.HashSet[A]
    x => seen.add(x)
  }

  def make[A: Type](using Quotes): Expr[JsonValueCodec[A]] = {
    val rootTpe = TypeRepr.of[A].dealias
    inferredKeyCodecs.put(rootTpe, None)
    inferredValueCodecs.put(rootTpe, None)
    val types = rootTpe :: Nil
    val codecDef = '{ // FIXME: generate a type class instance using `ClassDef.apply` and `Symbol.newClass` calls after graduating from experimental API: https://www.scala-lang.org/blog/2022/06/21/scala-3.1.3-released.html
      new JsonValueCodec[A] {
        @inline
        def nullValue: A = ${
          if (cfg.encodingOnly) '{ ??? }
          else genNullValue[A](types)
        }

        @inline
        def decodeValue(in: JsonReader, default: A): A = ${
          if (cfg.encodingOnly) '{ ??? }
          else genReadVal(types, 'default, cfg.isStringified, false, 'in)
        }

        @inline
        def encodeValue(x: A, out: JsonWriter): Unit = ${
          if (cfg.decodingOnly) '{ ??? }
          else genWriteVal('x, types, cfg.isStringified, None, 'out)
        }
      }
    }.asTerm
    val needDefs = (new mutable.ListBuffer).addAll(classTags.values).addAll(mathContexts.values)
      .addAll(nullValues.values).addAll(equalsMethods.values).addAll(scalaEnumCaches.values)
      .addAll(fieldIndexAccessors.values).addAll(decodeMethodDefs).addAll(encodeMethodDefs)
    val codec = Block(needDefs.toList, codecDef).asExpr.asInstanceOf[Expr[JsonValueCodec[A]]]
    if (// FIXME: uncomment after graduating from experimental API: CompilationInfo.XmacroSettings.contains("print-codecs") ||
    {
      Implicits.search(TypeRepr.of[CodecMakerConfig.PrintCodec]) match
        case s: ImplicitSearchSuccess => true
        case _ => false
    }) report.info(s"Generated JSON codec for type '${rootTpe.show}':\n${codec.show}", Position.ofMacroExpansion)
    codec
  }
}
