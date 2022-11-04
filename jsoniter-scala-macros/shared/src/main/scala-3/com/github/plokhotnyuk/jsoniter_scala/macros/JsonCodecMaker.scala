package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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
  *                               allow using `Option[Option[_]]` field values to distinguish `null` and missing field
  *                               cases
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
    val skipNestedOptionValues: Boolean) {
  @compileTimeOnly("withFieldNameMapper should be used only inside JsonCodec.make functions")
  def withFieldNameMapper(fieldNameMapper: PartialFunction[String, String]): CodecMakerConfig = ???

  @compileTimeOnly("withJavaEnumValueNameMapper should be used only inside JsonCodec.make functions")
  def withJavaEnumValueNameMapper(javaEnumValueNameMapper: PartialFunction[String, String]): CodecMakerConfig = ???

  @compileTimeOnly("withJavaEnumValueNameMapper should be used only inside JsonCodec.make functions")
  def withAdtLeafClassNameMapper(adtLeafClassNameMapper: String => String): CodecMakerConfig = ???

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

  def withSkipNestedOptionValues(skipNestedOptionValues: Boolean): CodecMakerConfig =
    copy(skipNestedOptionValues = skipNestedOptionValues)

  private[this] def copy(fieldNameMapper: NameMapper = fieldNameMapper,
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
                         skipNestedOptionValues: Boolean = skipNestedOptionValues): CodecMakerConfig =
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
      skipNestedOptionValues = skipNestedOptionValues)
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
  skipNestedOptionValues = false) {

  /**
    * Use to enable printing of codec during compilation:
    *{{{
    *given CodecMakerConfig.PrintCodec with {}
    *val codec = JsonCodecMaker.make[MyClass]
    *}}}
    **/
  class PrintCodec

  private[macros] given FromExpr[CodecMakerConfig] with {
    def extract[X: FromExpr](name: String, x: Expr[X])(using Quotes): X = {
      import quotes.reflect._

      summon[FromExpr[X]].unapply(x).getOrElse(throw FromExprException(s"Can't parse $name: ${x.show}, tree: ${x.asTerm}", x))
    }

    def unapply(x: Expr[CodecMakerConfig])(using Quotes): Option[CodecMakerConfig] = {
      import quotes.reflect._

      x match
        case '{
          CodecMakerConfig(
            $exprFieldNameMapper,
            $exprJavaEnumValueNameMapper,
            $exprAdtLeafClassNameMapper,
            $exprDiscriminatorFieldName,
            $exprIsStringified,
            $exprMapAsArray,
            $exprSkipUnexpectedFields,
            $exprTransientDefault,
            $exprTransientEmpty,
            $exprTransientNone,
            $exprRequireCollectionFields,
            $exprBigDecimalPrecision,
            $exprBigDecimalScaleLimit,
            $exprBigDecimalDigitsLimit,
            $exprBigIntDigitsLimit,
            $exprBitSetValueLimit,
            $exprMapMaxInsertNumber,
            $exprSetMaxInsertNumber,
            $exprAllowRecursiveTypes,
            $exprRequireDiscriminatorFirst,
            $exprUseScalaEnumValueId,
            $skipNestedOptionValues)
        } =>
          try {
            Some(CodecMakerConfig(
              extract("fieldNameMapper", exprFieldNameMapper),
              extract("javaEnumValueNameMapper", exprJavaEnumValueNameMapper),
              extract("eadtLeafClassNameMapper", exprAdtLeafClassNameMapper),
              extract("discriminatorFieldName", exprDiscriminatorFieldName),
              extract("isStringified", exprIsStringified),
              extract("mapAsArray", exprMapAsArray),
              extract("skipUnexpectedFields", exprSkipUnexpectedFields),
              extract("transientDefault", exprTransientDefault),
              extract("transientEmpty", exprTransientEmpty),
              extract("transientNone", exprTransientNone),
              extract("requireCollectionFields", exprRequireCollectionFields),
              extract("bigDecimalPrecision", exprBigDecimalPrecision),
              extract("bigDecimalScaleLimit", exprBigDecimalScaleLimit),
              extract("bigDecimalDigitsLimit", exprBigDecimalDigitsLimit),
              extract("bigIntDigitsLimit", exprBigIntDigitsLimit),
              extract("bitSetValueLimit", exprBitSetValueLimit),
              extract("mapMaxInsertNumber", exprMapMaxInsertNumber),
              extract("setMaxInsertNumber", exprSetMaxInsertNumber),
              extract("allowRecursiveTypes", exprAllowRecursiveTypes),
              extract("requireDiscriminatorFirst", exprRequireDiscriminatorFirst),
              extract("useScalaEnumValueId", exprUseScalaEnumValueId),
              extract("skipNestedOptionValues", skipNestedOptionValues)))
          } catch {
            case FromExprException(message, expr) =>
              report.warning(message, expr)
              None
          }
        case '{ ($x: CodecMakerConfig).withAllowRecursiveTypes($v) } => Some(x.valueOrAbort.withAllowRecursiveTypes(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withDiscriminatorFieldName($v) } => Some(x.valueOrAbort.withDiscriminatorFieldName(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withUseScalaEnumValueId($v) } => Some(x.valueOrAbort.withUseScalaEnumValueId(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withIsStringified($v) } => Some(x.valueOrAbort.withIsStringified(v.valueOrAbort))
        case '{ CodecMakerConfig } => Some(CodecMakerConfig)
        case '{ ($x: CodecMakerConfig).withFieldNameMapper($v) } => Some(x.valueOrAbort.copy(fieldNameMapper = ExprPartialFunctionWrapper(v)))
        case '{ ($x: CodecMakerConfig).withJavaEnumValueNameMapper($v) } => Some(x.valueOrAbort.copy(javaEnumValueNameMapper = ExprPartialFunctionWrapper(v)))
        case '{ ($x: CodecMakerConfig).withAdtLeafClassNameMapper($v) } => Some(x.valueOrAbort.copy(adtLeafClassNameMapper = ExprPartialFunctionWrapper('{ { case x => $v(x) } })))
        case '{ ($x: CodecMakerConfig).withRequireDiscriminatorFirst($v) } => Some(x.valueOrAbort.copy(requireDiscriminatorFirst = v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withMapAsArray($v) } => Some(x.valueOrAbort.withMapAsArray(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSkipUnexpectedFields($v) } => Some(x.valueOrAbort.withSkipUnexpectedFields(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientDefault($v) } => Some(x.valueOrAbort.withTransientDefault(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientEmpty($v) } => Some(x.valueOrAbort.withTransientEmpty(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withTransientNone($v) } => Some(x.valueOrAbort.withTransientNone(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withRequireCollectionFields($v) } => Some(x.valueOrAbort.withRequireCollectionFields(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalPrecision($v) } => Some(x.valueOrAbort.withBigDecimalPrecision(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalScaleLimit($v) } => Some(x.valueOrAbort.withBigDecimalScaleLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigDecimalDigitsLimit($v) } => Some(x.valueOrAbort.withBigDecimalDigitsLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBigIntDigitsLimit($v) } => Some(x.valueOrAbort.copy(bigIntDigitsLimit = v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withBitSetValueLimit($v) } => Some(x.valueOrAbort.withBitSetValueLimit(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withMapMaxInsertNumber($v) } => Some(x.valueOrAbort.withMapMaxInsertNumber(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSetMaxInsertNumber($v) } => Some(x.valueOrAbort.withSetMaxInsertNumber(v.valueOrAbort))
        case '{ ($x: CodecMakerConfig).withSkipNestedOptionValues($v) } => Some(x.valueOrAbort.withSkipNestedOptionValues(v.valueOrAbort))
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

  private[this] def enforceSnakeOrKebabCase(s: String, separator: Char): String =
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
  inline def make[A]: JsonValueCodec[A] = ${Impl.makeWithDefaultConfig[A]}

  /**
    * A replacement for the `make` call with the `CodecMakerConfig.withDiscriminatorFieldName(None)` configuration
    * parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithoutDiscriminator[A]: JsonValueCodec[A] = ${Impl.makeWithoutDiscriminator[A]}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = ${Impl.makeWithRequiredCollectionFields[A]}

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name"))`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] =
    ${Impl.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]}

  /**
    * Derives a codec for JSON values for the specified type `A` and a provided derivation configuration.
    *
    * @param config a derivation configuration
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def make[A](inline config: CodecMakerConfig): JsonValueCodec[A] = ${Impl.makeWithSpecifiedConfig[A]('config)}

  private[macros] object Impl {
    def makeWithDefaultConfig[A: Type](using Quotes): Expr[JsonValueCodec[A]] = tryMake(CodecMakerConfig)

    def makeWithoutDiscriminator[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withDiscriminatorFieldName(None))

    def makeWithRequiredCollectionFields[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true))

    def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)
        .withDiscriminatorFieldName(Some("name")))

    def makeWithSpecifiedConfig[A: Type](config: Expr[CodecMakerConfig])(using Quotes): Expr[JsonValueCodec[A]] = {
      import quotes.reflect._

      tryMake[A](summon[FromExpr[CodecMakerConfig]].unapply(config)
        .fold(report.errorAndAbort(s"Cannot evaluate a parameter of the 'make' macro call for type '${Type.show[A]}'. ")) {
          cfg =>
            if (cfg.requireCollectionFields && cfg.transientEmpty)
              report.errorAndAbort("'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously")
            cfg
        })
    }

    private[this] def tryMake[A: Type](cfg: CodecMakerConfig)(using Quotes): Expr[JsonValueCodec[A]] = {
      import quotes.reflect._

      try make[A](cfg) catch {
        case ex: CompileTimeEvalException => report.errorAndAbort("Can't evaluate compile-time expression", ex.expr)
      }
    }

    private[this] def make[A: Type](cfg: CodecMakerConfig)(using Quotes): Expr[JsonValueCodec[A]] = {
      import quotes.reflect._

      def fail(msg: String): Nothing = report.errorAndAbort(msg, Position.ofMacroExpansion)

      def warn(msg: String): Unit = report.warning(msg, Position.ofMacroExpansion)

      def typeArgs(tpe: TypeRepr): List[TypeRepr] = tpe match
        case AppliedType(_, typeArgs) => typeArgs.map(_.dealias)
        case _ => Nil

      def typeArg1(tpe: TypeRepr): TypeRepr = typeArgs(tpe).head

      def typeArg2(tpe: TypeRepr): TypeRepr = typeArgs(tpe).tail.head

      def isTuple(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[Tuple]

      def isValueClass(tpe: TypeRepr): Boolean = !isConstType(tpe) && tpe <:< TypeRepr.of[AnyVal]

      def valueClassValue(tpe: TypeRepr): Symbol = tpe.typeSymbol.fieldMembers(0)

      def valueClassValueType(tpe: TypeRepr): TypeRepr = tpe.memberType(tpe.typeSymbol.fieldMembers(0)).dealias

      def isNonAbstractScalaClass(tpe: TypeRepr): Boolean = tpe.classSymbol.fold(false) { sym =>
        val flags = sym.flags
        !flags.is(Flags.Abstract) && !flags.is(Flags.JavaDefined) && !flags.is(Flags.Trait)
      }

      def isSealedClass(tpe: TypeRepr): Boolean = tpe.typeSymbol.flags.is(Flags.Sealed)

      def isConstType(tpe: TypeRepr): Boolean = tpe match
        case ConstantType(_) => true
        case _ => false

      def isEnumOrModuleValue(tpe: TypeRepr): Boolean = tpe.isSingleton &&
        (tpe.typeSymbol.flags.is(Flags.Enum) || tpe.typeSymbol.flags.is(Flags.Module) ||
          tpe.typeSymbol.flags.is(Flags.EmptyFlags) && !isConstType(tpe)) // FIXME: Remove after fix in Dotty: https://github.com/lampepfl/dotty/issues/16008

      def adtChildren(tpe: TypeRepr): Seq[TypeRepr] = { // TODO: explore yet one variant with mirrors
        def resolveParentTypeArg(child: Symbol, fromNudeChildTarg: TypeRepr, parentTarg: TypeRepr,
                                 binding: Map[String, TypeRepr]): Map[String, TypeRepr] =
          if (fromNudeChildTarg.typeSymbol.isTypeParam) { // todo: check for paramRef instead ?
            val paramName = fromNudeChildTarg.typeSymbol.name
            binding.get(paramName) match
              case None => binding.updated(paramName, parentTarg)
              case Some(oldBinding) =>
                if (oldBinding =:= parentTarg) binding
                else fail(s"Type parameter $paramName in class ${child.name} appeared in the constructor of " +
                  s"${tpe.show} two times differently, with ${oldBinding.show} and ${parentTarg.show}")
          } else if (fromNudeChildTarg <:< parentTarg) binding // TODO: assupe parentTag is covariant, get covariance from tycon type parameters.
          else {
            (fromNudeChildTarg, parentTarg) match
              case (AppliedType(ctycon, ctargs), AppliedType(ptycon, ptargs)) =>
                ctargs.zip(ptargs).foldLeft(resolveParentTypeArg(child, ctycon, ptycon, binding)) { (b, e) =>
                  resolveParentTypeArg(child, e._1, e._2, b)
                }
              case _ => fail(s"Failed unification of type parameters of ${tpe.show} from child $child - " +
                  s"${fromNudeChildTarg.show} and ${parentTarg.show}")
          }

        def resolveParentTypeArgs(child: Symbol, nudeChildParentTags: List[TypeRepr], parentTags: List[TypeRepr],
                                  binding: Map[String, TypeRepr]): Map[String, TypeRepr] =
          nudeChildParentTags.zip(parentTags).foldLeft(binding)((s, e) => resolveParentTypeArg(child, e._1, e._2, s))

        tpe.typeSymbol.children.map { sym =>
          if (sym.isType) {
            if (sym.name == "<local child>") // problem - we have no other way to find this other return the name
              fail(s"Local child symbols are not supported, please consider change '${tpe.show}' or implement a " +
                "custom implicitly accessible codec")
            val nudeSubtype = TypeIdent(sym).tpe
            val tpeArgsFromChild = typeArgs(nudeSubtype.baseType(tpe.typeSymbol))
            nudeSubtype.memberType(sym.primaryConstructor) match
              case MethodType(_, _, resTp) => resTp
              case PolyType(names, bounds, resPolyTp) =>
                val targs = typeArgs(tpe)
                val tpBinding = resolveParentTypeArgs(sym, tpeArgsFromChild, targs, Map.empty)
                val ctArgs = names.map { name =>
                  tpBinding.get(name).getOrElse(fail(s"Type parameter $name of $sym can't be deduced from " +
                    s"type arguments of ${tpe.show}. Please provide a custom implicitly accessible codec for it."))
                }
                val polyRes = resPolyTp match
                  case MethodType(_, _, resTp) => resTp
                  case other => other // hope we have no multiple typed param lists yet.
                if (ctArgs.isEmpty) polyRes
                else polyRes match
                  case AppliedType(base, _) => base.appliedTo(ctArgs)
                  case AnnotatedType(AppliedType(base, _), annot) => AnnotatedType(base.appliedTo(ctArgs), annot)
                  case _ => polyRes.appliedTo(ctArgs)
              case other => fail(s"Primary constructior for ${tpe.show} is not MethodType or PolyType but $other")
          } else if (sym.isTerm) Ref(sym).tpe
          else fail("Only Scala classes & objects are supported for ADT leaf classes. Please consider using of " +
            s"them for ADT with base '${tpe.show}' or provide a custom implicitly accessible codec for the ADT base. " +
            s"Failed symbol: $sym (fullName=${sym.fullName})\n")
        }
      }

      def adtLeafClasses(adtBaseTpe: TypeRepr): Seq[TypeRepr] = {
        def collectRecursively(tpe: TypeRepr): Seq[TypeRepr] =
          val leafTpes = adtChildren(tpe).flatMap { subTpe =>
            if (isEnumOrModuleValue(subTpe) || subTpe =:= TypeRepr.of[None.type]) subTpe :: Nil
            else if (isSealedClass(subTpe)) collectRecursively(subTpe)
            else if (isNonAbstractScalaClass(subTpe)) subTpe :: Nil
            else fail(if (subTpe.typeSymbol.flags.is(Flags.Abstract) || subTpe.typeSymbol.flags.is(Flags.Trait) ) {
              "Only sealed intermediate traits or abstract classes are supported. Please consider using of them " +
                s"for ADT with base '${adtBaseTpe.show}' or provide a custom implicitly accessible codec for the ADT base."
            } else {
              "Only Scala classes & objects are supported for ADT leaf classes. Please consider using of them " +
                s"for ADT with base '${adtBaseTpe.show}' or provide a custom implicitly accessible codec for the ADT base."
            })
          }
          if (isNonAbstractScalaClass(tpe)) leafTpes :+ tpe
          else leafTpes

        val classes = collectRecursively(adtBaseTpe).distinct
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '${adtBaseTpe.show}'. " +
          "Please add them or provide a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def isOption(tpe: TypeRepr, types: List[TypeRepr]): Boolean = tpe <:< TypeRepr.of[Option[_]] &&
        (cfg.skipNestedOptionValues || !types.headOption.exists(_ <:< TypeRepr.of[Option[_]]))

      def isCollection(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[Iterable[_]] || tpe <:< TypeRepr.of[Array[_]]

      def isJavaEnum(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[java.lang.Enum[_]]

      def scalaCollectionCompanion(tpe: TypeRepr): Term =
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ref(tpe.typeSymbol.companionModule)
        else fail(s"Unsupported type '${tpe.show}'. Please consider using a custom implicitly accessible codec for it.")

      def scalaCollectionEmptyNoArgs(cTpe: TypeRepr, eTpe: TypeRepr): Term =
        TypeApply(Select.unique(scalaCollectionCompanion(cTpe), "empty"), List(Inferred(eTpe)))

      def scalaMapEmptyNoArgs(cTpe: TypeRepr, kTpe: TypeRepr, vTpe: TypeRepr): Term =
        TypeApply(Select.unique(scalaCollectionCompanion(cTpe), "empty"), List(Inferred(kTpe), Inferred(vTpe)))

      def scala2EnumerationObject(tpe: TypeRepr): Expr[Enumeration] = tpe match
        case TypeRef(ct, _) => Ref(ct.termSymbol).asExprOf[Enumeration]

      def summonOrdering(tpe: TypeRepr): Term = tpe.asType match
        case '[t] => Expr.summon[Ordering[t]].fold(fail(s"Can't summon Ordering[${tpe.show}]"))(_.asTerm)

      def summonClassTag(tpe: TypeRepr): Term = tpe.asType match
        case '[t] => Expr.summon[ClassTag[t]].fold(fail(s"Can't summon ClassTag[${tpe.show}]"))(_.asTerm)

      def findScala2EnumerationById[C <: AnyRef: Type](tpe: TypeRepr, i: Expr[Int])(using Quotes): Expr[Option[C]] =
        '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.id == $i) }.asExprOf[Option[C]]

      def findScala2EnumerationByName[C <: AnyRef: Type](tpe: TypeRepr, name: Expr[String])(using Quotes): Expr[Option[C]] =
        '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.toString == $name) }.asExprOf[Option[C]]

      def newArray[T: Type](size: Expr[Int])(using Quotes): Expr[Array[T]] =
        val tpe = TypeRepr.of[T]
        tpe.asType match
          case '[Boolean] => '{ new Array[Boolean]($size) }.asExprOf[Array[T]]
          case '[Byte] => '{ new Array[Byte]($size) }.asExprOf[Array[T]]
          case '[Short] => '{ new Array[Short]($size) }.asExprOf[Array[T]]
          case '[Char] => '{ new Array[Char]($size) }.asExprOf[Array[T]]
          case '[Int] => '{ new Array[Int]($size) }.asExprOf[Array[T]]
          case '[Float] => '{ new Array[Float]($size) }.asExprOf[Array[T]]
          case '[Long] => '{ new Array[Long]($size) }.asExprOf[Array[T]]
          case '[Double] => '{ new Array[Double]($size) }.asExprOf[Array[T]]
          case _ => '{ ${summonClassTag(tpe).asExprOf[ClassTag[T]]}.newArray($size) }

      val rootTpe = TypeRepr.of[A].dealias
      val inferredKeyCodecs = mutable.Map.empty[TypeRepr, Option[Expr[JsonKeyCodec[_]]]]
      val inferredValueCodecs = mutable.Map.empty[TypeRepr, Option[Expr[JsonValueCodec[_]]]]

      def inferImplicitValue[T: Type](typeToSearch: TypeRepr): Option[Expr[T]] = Implicits.search(typeToSearch) match
        case v: ImplicitSearchSuccess => Some(v.tree.asExprOf[T])
        case _ => None

      def symbol(name: String, tpe: TypeRepr, flags: Flags = Flags.EmptyFlags): Symbol =
        Symbol.newVal(Symbol.spliceOwner, name, tpe, flags, Symbol.noSymbol)

      def checkRecursionInTypes(types: List[TypeRepr]): Unit =
        if (!cfg.allowRecursiveTypes) {
          val tpe = types.head
          val nestedTypes = types.tail
          if (!tpe.typeSymbol.flags.is(Flags.Enum)) {
            val recursiveIdx = nestedTypes.indexOf(tpe)
            if (recursiveIdx >= 0) {
              val recTypes = nestedTypes.take(recursiveIdx + 1).map(_.show).reverse.mkString("'", "', '", "'")
              fail(s"Recursive type(s) detected: $recTypes. Please consider using a custom implicitly " +
                s"accessible codec for this type to control the level of recursion or turn on the " +
                s"'${Type.show[CodecMakerConfig]}.allowRecursiveTypes' for the trusted input that " +
                s"will not exceed the thread stack size.\nall types: ${types.map(_.show)}")
            }
          }
        }

      def findImplicitKeyCodec(types: List[TypeRepr]): Option[Expr[JsonKeyCodec[_]]] =
        checkRecursionInTypes(types)
        val tpe = types.head
        if (tpe =:= rootTpe) None
        else inferredKeyCodecs.getOrElseUpdate(tpe, {
          inferImplicitValue[JsonKeyCodec[_]](TypeRepr.of[JsonKeyCodec].appliedTo(tpe))
        })

      def findImplicitValueCodec(types: List[TypeRepr]): Option[Expr[JsonValueCodec[_]]] =
        checkRecursionInTypes(types)
        val tpe = types.head
        if (tpe =:= rootTpe) None
        else inferredValueCodecs.getOrElseUpdate(tpe, {
          inferImplicitValue[JsonValueCodec[_]](TypeRepr.of[JsonValueCodec].appliedTo(tpe))
        })

      val mathContexts = new mutable.LinkedHashMap[Int, ValDef]

      def withMathContextFor(precision: Int): Expr[MathContext] =
        if (precision == MathContext.DECIMAL128.getPrecision) '{ MathContext.DECIMAL128 }
        else if (precision == MathContext.DECIMAL64.getPrecision) '{ MathContext.DECIMAL64 }
        else if (precision == MathContext.DECIMAL32.getPrecision) '{ MathContext.DECIMAL32 }
        else if (precision == MathContext.UNLIMITED.getPrecision) '{ MathContext.UNLIMITED }
        else Ref(mathContexts.getOrElseUpdate(precision, {
          val mc = '{ new MathContext(${Expr(cfg.bigDecimalPrecision)}, java.math.RoundingMode.HALF_EVEN) }
          val sym = symbol("mc" + mathContexts.size, TypeRepr.of[MathContext])
          ValDef(sym, Some(mc.asTerm.changeOwner(sym)))
        }).symbol).asExprOf[MathContext]

      val scalaEnumCaches = new mutable.LinkedHashMap[TypeRepr, ValDef]

      def withScalaEnumCacheFor[K: Type, T: Type](tpe: TypeRepr)(using Quotes): Expr[ConcurrentHashMap[K, T]] =
        Ref(scalaEnumCaches.getOrElseUpdate(tpe, {
          val sym = symbol("ec" + scalaEnumCaches.size, TypeRepr.of[ConcurrentHashMap[K, T]])
          ValDef(sym, Some('{ new ConcurrentHashMap[K, T] }.asTerm.changeOwner(sym)))
        }).symbol).asExprOf[ConcurrentHashMap[K, T]]

      case class JavaEnumValueInfo(value: Symbol, name: String, transformed: Boolean)

      val javaEnumValueInfos = new mutable.LinkedHashMap[TypeRepr, Seq[JavaEnumValueInfo]]

      def javaEnumValues(tpe: TypeRepr): Seq[JavaEnumValueInfo] = javaEnumValueInfos.getOrElseUpdate(tpe, {
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

      def genReadJavaEnumValue[E: Type](enumValues: Seq[JavaEnumValueInfo], unexpectedEnumValueHandler: Expr[E],
                                        in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[E] = {
        def genReadCollisions(es: collection.Seq[JavaEnumValueInfo]): Expr[E] =
          es.foldRight(unexpectedEnumValueHandler) { (e, acc) => '{
            if ($in.isCharBufEqualsTo($l, ${Expr(e.name)})) ${Ref(e.value).asExprOf[E]}
            else $acc
          } }

        if (enumValues.size <= 8 && enumValues.foldLeft(0)(_ + _.name.length) <= 64) genReadCollisions(enumValues)
        else {
          val hashCode = (e: JavaEnumValueInfo) => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
          val cases = groupByOrdered(enumValues)(hashCode).map { case (hash, fs) =>
            val sym = Symbol.newBind(Symbol.spliceOwner, "b" + hash, Flags.EmptyFlags, TypeRepr.of[Int])
            CaseDef(Bind(sym, Expr(hash).asTerm), None, genReadCollisions(fs).asTerm)
          } :+ CaseDef(Wildcard(), None, unexpectedEnumValueHandler.asTerm)
          Match('{ $in.charBufToHashCode($l) }.asTerm, cases.toList).asExprOf[E]
        }
      }

      case class FieldInfo(symbol: Symbol,
                           mappedName: String,
                           getterOrField: Symbol,
                           defaultValue: Option[Term],
                           resolvedTpe: TypeRepr,
                           isTransient: Boolean,
                           isStringified: Boolean,
                           nonTransientFieldIndex: Int)

      case class ClassInfo(tpe: TypeRepr, primaryConstructor: Symbol, paramLists: List[List[FieldInfo]]) {
        val fields: Seq[FieldInfo] = paramLists.flatten.filter(!_.isTransient)

        def genNew(arg: Term): Term = genNew(List(List(arg)))

        def genNew(argss: List[List[Term]]): Term =
          val constructorNoTypes = Select(New(Inferred(tpe)), primaryConstructor)
          val constructor = typeArgs(tpe) match
            case Nil => constructorNoTypes
            case typeArgs => TypeApply(constructorNoTypes, typeArgs.map(Inferred(_)))
          argss.tail.foldLeft(Apply(constructor, argss.head))((acc, args) => Apply(acc, args))
      }

      val classInfos = new mutable.LinkedHashMap[TypeRepr, ClassInfo]

      def getClassInfo(tpe: TypeRepr): ClassInfo = classInfos.getOrElseUpdate(tpe, {
        case class FieldAnnotations(partiallyMappedName: String, transient: Boolean, stringified: Boolean)

        def getPrimaryConstructor(tpe: TypeRepr): Symbol = tpe.classSymbol match
          case Some(sym) if sym.primaryConstructor.exists => sym.primaryConstructor
          case _ => fail(s"Cannot find a primary constructor for '$tpe'")

        def hasSupportedAnnotation(m: Symbol): Boolean =
          m.annotations.exists(a => a.tpe <:< TypeRepr.of[named] || a.tpe <:< TypeRepr.of[transient] ||
            a.tpe <:< TypeRepr.of[stringified])

        val tpeClassSym = tpe.classSymbol.getOrElse(fail(s"Expected that ${tpe.show} has classSymbol"))
        val fieldsWithDefaultValues = tpeClassSym.fieldMembers.filter(_.flags.is(Flags.HasDefault))
        val annotations = tpeClassSym.fieldMembers.collect { case m: Symbol if hasSupportedAnnotation(m) =>
          val name = m.name
          val named = m.annotations.filter(_.tpe =:= TypeRepr.of[named])
          if (named.size > 1) fail(s"Duplicated '${TypeRepr.of[named].show}' defined for '$name' of '${tpe.show}'.")
          val trans = m.annotations.filter(_.tpe =:= TypeRepr.of[transient])
          if (trans.size > 1) warn(s"Duplicated '${TypeRepr.of[transient].show}' defined for '$name' of '${tpe.show}'.")
          val strings = m.annotations.filter(_.tpe =:= TypeRepr.of[stringified])
          if (strings.size > 1) warn(s"Duplicated '${TypeRepr.of[stringified].show}' defined for '$name' of '${tpe.show}'.")
          if ((named.nonEmpty || strings.nonEmpty) && trans.size == 1)
            warn(s"Both '${Type.show[transient]}' and '${Type.show[named]}' or " +
              s"'${Type.show[transient]}' and '${Type.show[stringified]}' defined for '$name' of '${tpe.show}'.")
          val partiallyMappedName = namedValueOpt(named.headOption, tpe).getOrElse(name)
          (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        val primaryConstructor = getPrimaryConstructor(tpe)

        def createFieldInfos(params: List[Symbol], typeParams: List[Symbol],
                             nonTransientFieldIndex: AtomicInteger): List[FieldInfo] = {
          val fieldInfos = new mutable.ListBuffer[FieldInfo]
          params.zipWithIndex.foreach { case (symbol, i) =>
            val name = symbol.name
            val annotationOption = annotations.get(name)
            val mappedName = annotationOption.fold(cfg.fieldNameMapper(name).getOrElse(name))(_.partiallyMappedName)
            val field = tpeClassSym.fieldMember(name)
            val getterOrField =
              if (field.exists) {
                if (field.flags.is(Flags.PrivateLocal)) fail(s"Field '$name' in class '${tpe.show}' is private. " +
                  "It should be defined as 'val' or 'var' in the primary constructor.")
                field
              } else {
                val getters = tpeClassSym.methodMember(name)
                  .filter(_.flags.is(Flags.CaseAccessor | Flags.FieldAccessor | Flags.ParamAccessor))
                if (getters.isEmpty) { // Scala3 doesn't set FieldAccess flag for val parameters of constructor
                  val namedMembers = tpeClassSym.methodMember(name).filter(_.paramSymss == Nil)
                  if (namedMembers.isEmpty) fail(s"Field and getter not found: '$name' parameter of '${tpe.show}' " +
                    s"should be defined as 'val' or 'var' in the primary constructor.")
                  namedMembers.head.privateWithin match
                    case None => namedMembers.head
                    case _ => fail(s"Getter is private: '$name' paramter of '${tpe.show}' should be defined " +
                        "as 'val' or 'var' in the primary constructor.")
                } else getters.head // TODO: check length ?  when we have both reader and writer getters.filter(_.paramSymss == List(List()))
              }
            val defaultValue =
              if (symbol.flags.is(Flags.HasDefault)) {
                val dvMembers = tpe.typeSymbol.companionClass.methodMember("$lessinit$greater$default$" + (i + 1))
                if (dvMembers.isEmpty) fail(s"Can't find default value for $symbol in class ${tpe.show}")
                val methodSymbol = dvMembers.head
                val dvSelectNoTArgs = Ref(tpe.typeSymbol.companionModule).select(methodSymbol)
                val dvSelect = methodSymbol.paramSymss match
                  case Nil => dvSelectNoTArgs
                  case List(params) if (params.exists(_.isTypeParam)) => typeArgs(tpe) match
                    case Nil => fail(s"Expected that ${tpe.show} is an applied type")
                    case typeArgs => TypeApply(dvSelectNoTArgs, typeArgs.map(Inferred(_)))
                  case _ => fail(s"Default method for ${symbol.name} of class ${tpe.show} have a complex " +
                    s"parameter list: ${methodSymbol.paramSymss}")
                Some(dvSelect)
              } else None
            val isStringified = annotationOption.exists(_.stringified)
            val isTransient = annotationOption.exists(_.transient)
            val originFieldType = tpe.memberType(symbol).dealias
            val fieldType = typeArgs(tpe) match
              case Nil => originFieldType
              case typeArgs => originFieldType.substituteTypes(typeParams, typeArgs)
            fieldType match
              case tl@TypeLambda(_, _, _) => fail(s"Hight-kinded types are not supported for type ${tpe.show} " +
                  s"with field type for '$name' (symbol=$symbol) : ${fieldType.show}, originFieldType=" +
                  s"${originFieldType.show}, constructor typeParams=$typeParams, ")
              case TypeBounds(_, _) => fail(s"Type bounds are not supported for type '${tpe.show}' with field " +
                  s"type for $name '${fieldType.show}'")
              case _ =>
                if (defaultValue.isDefined && !(defaultValue.get.tpe <:< fieldType)) {
                  fail("Polymorphic expression cannot be instantiated to expected type: default value for " +
                    s"field $symbol of class ${tpe.show} have type ${defaultValue.get.tpe.show} but field type " +
                    s"is ${fieldType.show}")
                }
            fieldInfos.addOne(FieldInfo(symbol, mappedName, getterOrField, defaultValue, fieldType, isTransient,
              isStringified, nonTransientFieldIndex.get))
            if (!isTransient) nonTransientFieldIndex.getAndIncrement()
          }
          fieldInfos.result()
        }

        def isTypeParamsList(symbols: List[Symbol]): Boolean = symbols.exists(_.isTypeParam)

        val nonTransientFieldIndex = new AtomicInteger
        ClassInfo(tpe, primaryConstructor, primaryConstructor.paramSymss match {
          case tps :: ps :: Nil if isTypeParamsList(tps) => createFieldInfos(ps, tps, nonTransientFieldIndex) :: Nil
          case tps :: pss if isTypeParamsList(tps) => pss.map(ps => createFieldInfos(ps, tps, nonTransientFieldIndex))
          case pss => pss.map(ps => createFieldInfos(ps, Nil, nonTransientFieldIndex))
        })
      })

      def genReadKey[T: Type](types: List[TypeRepr], in: Expr[JsonReader])(using Quotes): Expr[T] =
        val tpe = types.head
        val implKeyCodec = findImplicitKeyCodec(types)
        if (!implKeyCodec.isEmpty) '{ ${implKeyCodec.get}.decodeKey($in) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Boolean] || tpe =:= TypeRepr.of[java.lang.Boolean]) '{ $in.readKeyAsBoolean() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Byte] || tpe =:= TypeRepr.of[java.lang.Byte]) '{ $in.readKeyAsByte() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Char] || tpe =:= TypeRepr.of[java.lang.Character]) '{ $in.readKeyAsChar() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Short] || tpe =:= TypeRepr.of[java.lang.Short]) '{ $in.readKeyAsShort() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Int] || tpe =:= TypeRepr.of[java.lang.Integer]) '{ $in.readKeyAsInt() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Long] || tpe =:= TypeRepr.of[java.lang.Long]) '{ $in.readKeyAsLong() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Float] || tpe =:= TypeRepr.of[java.lang.Float]) '{ $in.readKeyAsFloat() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Double] || tpe =:= TypeRepr.of[java.lang.Double]) '{ $in.readKeyAsDouble() }.asExprOf[T]
        else if (isValueClass(tpe)) {
          val vtpe = valueClassValueType(tpe)
          vtpe.asType match
            case '[vt] =>
              getClassInfo(tpe).genNew(genReadKey[vt](vtpe :: types, in).asTerm).asExprOf[T]
        } else if (tpe =:= TypeRepr.of[String]) '{ $in.readKeyAsString() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[BigInt]) '{ $in.readKeyAsBigInt(${Expr(cfg.bigIntDigitsLimit)}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[BigDecimal]) {
          val mc = withMathContextFor(cfg.bigDecimalPrecision)
          '{ $in.readKeyAsBigDecimal($mc, ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)}) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $in.readKeyAsUUID() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Duration]) '{ $in.readKeyAsDuration() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Instant]) '{ $in.readKeyAsInstant() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalDate]) '{ $in.readKeyAsLocalDate() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $in.readKeyAsLocalDateTime() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalTime]) '{ $in.readKeyAsLocalTime() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[MonthDay]) '{ $in.readKeyAsMonthDay() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $in.readKeyAsOffsetDateTime() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $in.readKeyAsOffsetTime() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Period]) '{ $in.readKeyAsPeriod() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Year]) '{ $in.readKeyAsYear() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[YearMonth]) '{ $in.readKeyAsYearMonth() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $in.readKeyAsZonedDateTime() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZoneId]) '{ $in.readKeyAsZoneId() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $in.readKeyAsZoneOffset() }.asExprOf[T]
        else if (tpe <:< TypeRepr.of[Enumeration#Value]) {
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
            }.asExprOf[T]
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
            }.asExprOf[T]
          }
        } else if (isJavaEnum(tpe)) {
          '{
            val l = $in.readKeyAsCharBuf()
            ${genReadJavaEnumValue(javaEnumValues(tpe), '{ $in.enumValueError(l) }, in, 'l)}
          }.asExprOf[T]
        } else if (isConstType(tpe)) {
          tpe match
            case ConstantType(StringConstant(v)) =>
              '{ if ($in.readKeyAsString() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(BooleanConstant(v)) =>
              '{ if ($in.readKeyAsBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(ByteConstant(v)) =>
              '{ if ($in.readKeyAsByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(CharConstant(v)) =>
              '{ if ($in.readKeyAsChar() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(ShortConstant(v)) =>
              '{ if ($in.readKeyAsShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(IntConstant(v)) =>
              '{ if ($in.readKeyAsInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(LongConstant(v)) =>
              '{ if ($in.readKeyAsLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(FloatConstant(v)) =>
              '{ if ($in.readKeyAsFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case ConstantType(DoubleConstant(v)) =>
              '{ if ($in.readKeyAsDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected key: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
            case _ => cannotFindKeyCodecError(tpe)
        } else cannotFindKeyCodecError(tpe)

      def genReadArray[B: Type, C: Type](newBuilder: Quotes ?=> Expr[Int] => Expr[B],
                                         readVal: Quotes ?=> (Expr[B], Expr[Int], Expr[Int]) => Expr[Unit],
                                         default: Expr[C], result: Quotes ?=> (Expr[B], Expr[Int], Expr[Int]) => Expr[C],
                                         in: Expr[JsonReader])(using Quotes): Expr[C] = '{
        if ($in.isNextToken('[')) {
          if ($in.isNextToken(']')) $default
          else {
            $in.rollbackToken()
            var l = 16
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

      def genReadCollection[B: Type, C: Type](newBuilder: Expr[B], readVal: Quotes ?=> Expr[B] => Expr[Unit],
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

      def genReadSet[B: Type, C: Type](newBuilder: Expr[B], readVal: Quotes ?=> Expr[B] => Expr[Unit], default: Expr[C],
                                       result: Quotes ?=> Expr[B] => Expr[C], in: Expr[JsonReader])(using Quotes): Expr[C] = '{
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

      def genReadMap[B: Type, C: Type](newBuilder: Expr[B], readKV: Quotes ?=> Expr[B] => Expr[Unit],
                                       result: Quotes ?=> Expr[B]=>Expr[C], in: Expr[JsonReader],
                                       default: Expr[C])(using Quotes): Expr[C] = '{
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

      def genReadMapAsArray[B: Type, C: Type](newBuilder: Expr[B], readKV: Quotes ?=> Expr[B] => Expr[Unit],
                                              result: Quotes ?=> Expr[B] => Expr[C], in: Expr[JsonReader],
                                              default: Expr[C])(using Quotes): Expr[C] = '{
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
              } else $in.readNullOrTokenError($default, '[')
              $in.isNextToken(',')
            }) ()
            if ($in.isCurrentToken(']')) ${result('x)}
            else $in.objectEndOrCommaError()
          }
        } else $in.readNullOrTokenError($default, '[')
      }

      @tailrec
      def genWriteKey[T: Type](x: Expr[T], types: List[TypeRepr], out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        val tpe = types.head
        val implKeyCodec = findImplicitKeyCodec(types)
        if (!implKeyCodec.isEmpty) '{ ${implKeyCodec.get.asExprOf[JsonKeyCodec[T]]}.encodeKey($x, $out) }
        else if (tpe =:= TypeRepr.of[Boolean]) '{ $out.writeKey(${x.asExprOf[Boolean]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Boolean]) '{ $out.writeKey(${x.asExprOf[java.lang.Boolean]}) }
        else if (tpe =:= TypeRepr.of[Byte]) '{ $out.writeKey(${x.asExprOf[Byte]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Byte]) '{ $out.writeKey(${x.asExprOf[java.lang.Byte]}) }
        else if (tpe =:= TypeRepr.of[Char]) '{ $out.writeKey(${x.asExprOf[Char]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ $out.writeKey(${x.asExprOf[java.lang.Character]}) }
        else if (tpe =:= TypeRepr.of[Short]) '{ $out.writeKey(${x.asExprOf[Short]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Short]) '{ $out.writeKey(${x.asExprOf[java.lang.Short]}) }
        else if (tpe =:= TypeRepr.of[Int]) '{ $out.writeKey(${x.asExprOf[Int]}) }
        else if (tpe =:= TypeRepr.of[Integer]) '{ $out.writeKey(${x.asExprOf[java.lang.Integer]}) }
        else if (tpe =:= TypeRepr.of[Long]) '{ $out.writeKey(${x.asExprOf[Long]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Long]) '{ $out.writeKey(${x.asExprOf[java.lang.Long]}) }
        else if (tpe =:= TypeRepr.of[Float]) '{ $out.writeKey(${x.asExprOf[Float]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Float]) '{ $out.writeKey(${x.asExprOf[java.lang.Float]}) }
        else if (tpe =:= TypeRepr.of[Double]) '{ $out.writeKey(${x.asExprOf[Double]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Double]) '{ $out.writeKey(${x.asExprOf[java.lang.Double]}) }
        else if (tpe =:= TypeRepr.of[String]) '{ $out.writeKey(${x.asExprOf[String]}) }
        else if (tpe =:= TypeRepr.of[BigInt]) '{ $out.writeKey(${x.asExprOf[BigInt]}) }
        else if (tpe =:= TypeRepr.of[BigDecimal]) '{ $out.writeKey(${x.asExprOf[BigDecimal]}) }
        else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $out.writeKey(${x.asExprOf[java.util.UUID]}) }
        else if (tpe =:= TypeRepr.of[Duration]) '{ $out.writeKey(${x.asExprOf[Duration]}) }
        else if (tpe =:= TypeRepr.of[Instant]) '{ $out.writeKey(${x.asExprOf[Instant]}) }
        else if (tpe =:= TypeRepr.of[LocalDate]) '{ $out.writeKey(${x.asExprOf[LocalDate]}) }
        else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $out.writeKey(${x.asExprOf[LocalDateTime]}) }
        else if (tpe =:= TypeRepr.of[LocalTime]) '{ $out.writeKey(${x.asExprOf[LocalTime]}) }
        else if (tpe =:= TypeRepr.of[MonthDay]) '{ $out.writeKey(${x.asExprOf[MonthDay]}) }
        else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $out.writeKey(${x.asExprOf[OffsetDateTime]}) }
        else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $out.writeKey(${x.asExprOf[OffsetTime]}) }
        else if (tpe =:= TypeRepr.of[Period]) '{ $out.writeKey(${x.asExprOf[Period]}) }
        else if (tpe =:= TypeRepr.of[Year]) '{ $out.writeKey(${x.asExprOf[Year]}) }
        else if (tpe =:= TypeRepr.of[YearMonth]) '{ $out.writeKey(${x.asExprOf[YearMonth]}) }
        else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $out.writeKey(${x.asExprOf[ZonedDateTime]}) }
        else if (tpe =:= TypeRepr.of[ZoneId]) '{ $out.writeKey(${x.asExprOf[ZoneId]}) }
        else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $out.writeKey(${x.asExprOf[ZoneOffset]}) }
        else if (isValueClass(tpe)) {
          val vtpe = valueClassValueType(tpe)
          vtpe.asType match
            case '[vt] => genWriteKey(Select.unique(x.asTerm, valueClassValue(tpe).name).asExprOf[vt], vtpe :: types, out)
        } else if (tpe <:< TypeRepr.of[Enumeration#Value]) {
          if (cfg.useScalaEnumValueId) '{ $out.writeKey(${x.asExprOf[Enumeration#Value]}.id) }
          else '{ $out.writeKey($x.toString) }
        } else if (isJavaEnum(tpe)) {
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => CaseDef(Ref(e.value), None, Literal(StringConstant(e.name)))) :+
              CaseDef(Wildcard(), None, '{ $out.encodeError("illegal enum value: " + $x) }.asTerm)
            val matchExpr = Match(x.asTerm, cases.toList).asExprOf[String]
            if (encodingRequired) '{ $out.writeKey($matchExpr) }
            else '{ $out.writeNonEscapedAsciiKey($matchExpr) }
          } else {
            val nameExpr = Apply(Select.unique(x.asTerm, "name"), List()).asExprOf[String]
            if (encodingRequired) '{ $out.writeKey($nameExpr) }
            else '{ $out.writeNonEscapedAsciiKey($nameExpr) }
          }
        } else if (isConstType(tpe)) {
          tpe match
            case ConstantType(StringConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(BooleanConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(ByteConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(CharConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(ShortConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(IntConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(LongConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(FloatConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case ConstantType(DoubleConstant(v)) => '{ $out.writeKey(${Expr(v)}) }
            case _ => cannotFindKeyCodecError(tpe)
        } else cannotFindKeyCodecError(tpe)

      def genWriteConstantKey(name: String, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        if (isEncodingRequired(name)) '{ $out.writeKey(${Expr(name)}) }
        else '{ $out.writeNonEscapedAsciiKey(${Expr(name)}) }

      def genWriteConstantVal(value: String, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        if (isEncodingRequired(value)) '{ $out.writeVal(${Expr(value)}) }
        else '{ $out.writeNonEscapedAsciiVal(${Expr(value)}) }

      def genWriteArray[T: Type](x: Expr[Iterable[T]], writeVal: Quotes ?=> (Expr[JsonWriter], Expr[T]) => Expr[Unit],
                                 out: Expr[JsonWriter])(using Quotes): Expr[Unit] = '{
        $out.writeArrayStart()
        $x.foreach(x => ${writeVal(out, 'x)})
        $out.writeArrayEnd()
      }

      def genWriteMapScala213[K: Type, V: Type](x: Expr[collection.Map[K, V]],
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

      def genWriteMapAsArrayScala213[K: Type, V: Type](x: Expr[collection.Map[K, V]],
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

      def cannotFindKeyCodecError(tpe: TypeRepr): Nothing =
        fail(s"No implicit '${TypeRepr.of[JsonKeyCodec[_]].show}' defined for '${tpe.show}'.")

      def cannotFindValueCodecError(tpe: TypeRepr): Nothing =
        fail(if (tpe.typeSymbol.flags.is(Flags.Abstract) || tpe.typeSymbol.flags.is(Flags.Trait)) {
          "Only sealed traits or abstract classes are supported as an ADT base. " +
            s"Please consider sealing the '${tpe.show}' or provide a custom implicitly accessible codec for it."
        } else s"No implicit '${TypeRepr.of[JsonValueCodec[_]].show}' defined for '${tpe.show}'.")

      def namedValueOpt(namedAnnotation: Option[Term], tpe: TypeRepr): Option[String] = namedAnnotation.map {
        case Apply(_, List(param)) => CompileTimeEval.evalExpr(param.asExprOf[String]).asTerm match // TODO: write testcase
          case Literal(StringConstant(s)) => s
          case _ => fail(s"Cannot evaluate a parameter of the '@named' annotation in type '${tpe.show}': $param.")
        case a => fail(s"Invalid named annotation ${a.show}")
      }

      def unexpectedFieldHandler(in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[Unit] =
        if (cfg.skipUnexpectedFields) '{ $in.skip() }
        else '{ $in.unexpectedKeyError($l) }

      def discriminatorValue(tpe: TypeRepr): String =
        val named = tpe.typeSymbol.annotations.filter(_.tpe =:= TypeRepr.of[named])
        if (named.size > 1) fail(s"Duplicated '${TypeRepr.of[named].show}' defined for '${tpe.show}'.")
        if (named.size > 0) namedValueOpt(named.headOption, tpe).get
        else cfg.adtLeafClassNameMapper({
          if (tpe =:= TypeRepr.of[None.type]) "None"
          else if (tpe.typeSymbol.flags.is(Flags.Enum)) {
            tpe match
              case TermRef(_, name) => name
              case TypeRef(_, name) => name // ADT
              case AppliedType(base, _) => base.typeSymbol.fullName
              case _ => fail(s"Unsupported enum type: '${tpe.show}', tree=$tpe")
          } else if (tpe.typeSymbol.flags.is(Flags.Module)) tpe.termSymbol.fullName
          else tpe.typeSymbol.fullName
        }).getOrElse(fail(s"Discriminator is not defined for ${tpe.show}"))

      def checkFieldNameCollisions(tpe: TypeRepr, names: Seq[String]): Unit =
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON key(s) defined for '${tpe.show}': $formattedCollisions. Keys are derived from " +
            s"field names of the class that are mapped by the '${TypeRepr.of[CodecMakerConfig].show}.fieldNameMapper' " +
            s"function or can be overridden by '${TypeRepr.of[named].show}' annotation(s). Result keys should be " +
            s"unique and should not match with a key for the discriminator field that is specified by the " +
            s"'${TypeRepr.of[CodecMakerConfig].show}.discriminatorFieldName' option.")
        }

      def checkDiscriminatorValueCollisions(tpe: TypeRepr, names: Seq[String]): Unit =
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated discriminator defined for ADT base '${tpe.show}': $formattedCollisions. Values for " +
            s"leaf classes of ADT that are returned by the '${Type.show[CodecMakerConfig]}.adtLeafClassNameMapper' " +
            s"function should be unique. Names: $names")
        }

      val nullValues = new mutable.LinkedHashMap[TypeRepr, ValDef]

      def withNullValueFor[T: Type](tpe: TypeRepr)(f: => Expr[T]): Expr[T] =
        Ref(nullValues.getOrElseUpdate(tpe, {
          val sym = symbol("c" + nullValues.size, tpe)
          ValDef(sym, Some(f.asTerm.changeOwner(sym)))
        }).symbol).asExprOf[T]

      val fieldIndexAccessors = new mutable.LinkedHashMap[TypeRepr, DefDef]

      def withFieldsByIndexFor(tpe: TypeRepr)(f: => Seq[String]): Term =
        Ref(fieldIndexAccessors.getOrElseUpdate(tpe, { // [Int => String], we don't want eta-expand without reason, so let this will be just index
          val mt = MethodType(List("i"))(_ => List(TypeRepr.of[Int]), _ => TypeRepr.of[String])
          val sym = Symbol.newMethod(Symbol.spliceOwner, "f" + fieldIndexAccessors.size, mt)
          DefDef(sym, params => {
            val List(List(param)) = params
            val cases = f.zipWithIndex
              .map { case (n, i) => CaseDef(Literal(IntConstant(i)), None, Literal(StringConstant(n))) }
            Some(Match(param.asExprOf[Int].asTerm, cases.toList).changeOwner(sym))
          })
        }).symbol)

      val equalsMethods = new mutable.LinkedHashMap[TypeRepr, DefDef]

      def withEqualsFor[T: Type](tpe: TypeRepr, arg1: Expr[T], arg2: Expr[T])
                                (f: (Expr[T], Expr[T]) => Expr[Boolean]): Expr[Boolean] =
        Apply(Ref(equalsMethods.getOrElseUpdate(tpe, {
          val mt = MethodType(List("x1", "x2"))(_ => List(tpe, tpe), _ => TypeRepr.of[Boolean])
          val sym = Symbol.newMethod(Symbol.spliceOwner, "q" + equalsMethods.size, mt)
          DefDef(sym, params => {
            val List(List(x1, x2)) = params
            Some(f(x1.asExprOf[T], x2.asExprOf[T]).asTerm.changeOwner(sym))
          })
        }).symbol), List(arg1.asTerm, arg2.asTerm)).asExprOf[Boolean]

      def genArrayEquals[T: Type](tpe: TypeRepr, x1t: Expr[T], x2t: Expr[T]): Expr[Boolean] =
        val tpe1 = typeArg1(tpe)
        if (tpe1 <:< TypeRepr.of[Array[_]]) {
          tpe1.asType match
            case '[t1] =>
              val x1 = x1t.asExprOf[Array[t1]]
              val x2 = x2t.asExprOf[Array[t1]]

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
        } else if (tpe1 =:= TypeRepr.of[Boolean]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Boolean]]}, ${x2t.asExprOf[Array[Boolean]]}) }
        } else if (tpe1 =:= TypeRepr.of[Byte]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Byte]]}, ${x2t.asExprOf[Array[Byte]]}) }
        } else if (tpe1 =:= TypeRepr.of[Short]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Short]]}, ${x2t.asExprOf[Array[Short]]}) }
        } else if (tpe1 =:= TypeRepr.of[Int]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Int]]}, ${x2t.asExprOf[Array[Int]]}) }
        } else if (tpe1 =:= TypeRepr.of[Long]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Long]]}, ${x2t.asExprOf[Array[Long]]}) }
        } else if (tpe1 =:= TypeRepr.of[Float]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Float]]}, ${x2t.asExprOf[Array[Float]]}) }
        } else if (tpe1 =:= TypeRepr.of[Double]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Double]]}, ${x2t.asExprOf[Array[Double]]}) }
        } else if (tpe1 =:= TypeRepr.of[Char]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[Char]]}, ${x2t.asExprOf[Array[Char]]}) }
        } else if (tpe1 <:< TypeRepr.of[AnyRef]) {
          '{ java.util.Arrays.equals(${x1t.asExprOf[Array[AnyRef]]}, ${x2t.asExprOf[Array[AnyRef]]}) }
        } else fail(s"Can't compare arrays of type ${tpe1.show}")

      case class DecoderMethodKey(tpe: TypeRepr, isStringified: Boolean, useDiscriminator: Boolean)

      val decodeMethodDefs = new mutable.LinkedHashMap[DecoderMethodKey, DefDef]
      val decodeMethodSyms = new mutable.LinkedHashMap[DecoderMethodKey, Symbol]

      def withDecoderFor[T: Type](methodKey: DecoderMethodKey, arg: Expr[T], in: Expr[JsonReader])
                                 (f: (Expr[JsonReader], Expr[T]) => Expr[T])(using Quotes): Expr[T] =
        Apply(Ref(decodeMethodSyms.get(methodKey).getOrElse {
          val mt = MethodType(List("in", "defaultValue"))(_ => List(TypeRepr.of[JsonReader], methodKey.tpe), _ => TypeRepr.of[T])
          val sym = Symbol.newMethod(Symbol.spliceOwner, "d" + decodeMethodSyms.size, mt)
          decodeMethodSyms.update(methodKey, sym)
          decodeMethodDefs.update(methodKey, DefDef(sym, params => {
            val List(List(in, default)) = params
            Some(f(in.asExprOf[JsonReader], default.asExprOf[T]).asTerm.changeOwner(sym))
          }))
          sym
        }), List(in.asTerm, arg.asTerm)).asExprOf[T]

      case class WriteDiscriminator(fieldName: String, fieldValue: String) {
        def write(out: Expr[JsonWriter]): Expr[Unit] = '{
          ${genWriteConstantKey(fieldName, out)}
          ${genWriteConstantVal(fieldValue, out)}
        }
      }

      case class EncoderMethodKey(tpe: TypeRepr, isStringified: Boolean, discriminatorKeyValue: Option[(String, String)])

      val encodeMethodDefs = new mutable.LinkedHashMap[EncoderMethodKey, DefDef]
      val encodeMethodSyms = new mutable.LinkedHashMap[EncoderMethodKey, Symbol]

      def withEncoderFor[T: Type](methodKey: EncoderMethodKey, arg: Expr[T], out: Expr[JsonWriter])
                                 (f: (Expr[JsonWriter], Expr[T])=> Expr[Unit]): Expr[Unit] =
        Apply(Ref(encodeMethodSyms.get(methodKey).getOrElse {
          val mt = MethodType(List("x", "out"))(_ => List(TypeRepr.of[T], TypeRepr.of[JsonWriter]), _ => TypeRepr.of[Unit])
          val sym = Symbol.newMethod(Symbol.spliceOwner, "e" + encodeMethodSyms.size, mt)
          encodeMethodSyms.update(methodKey, sym)
          encodeMethodDefs.update(methodKey, DefDef(sym, params => {
            val List(List(x, out)) = params
            Some(f(out.asExprOf[JsonWriter], x.asExprOf[T]).asTerm.changeOwner(sym))
          }))
          sym
        }), List(arg.asTerm, out.asTerm)).asExprOf[Unit]

      def genNullValue[T: Type](types: List[TypeRepr])(using Quotes): Expr[T] =
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        if (!implCodec.isEmpty) '{ ${implCodec.get}.nullValue }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Boolean]) Literal(BooleanConstant(false)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Boolean]) '{ java.lang.Boolean.valueOf(false) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Byte]) Literal(ByteConstant(0)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Byte]) '{ java.lang.Byte.valueOf(0: Byte) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Char]) '{ '\u0000' }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ java.lang.Character.valueOf('\u0000') }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Short]) Literal(ShortConstant(0)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Short]) '{ java.lang.Short.valueOf(0: Short) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Int]) Literal(IntConstant(0)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Integer]) '{ java.lang.Integer.valueOf(0) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Long]) Literal(LongConstant(0)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Long]) '{ java.lang.Long.valueOf(0L) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Float]) Literal(FloatConstant(0f)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Float]) '{ java.lang.Float.valueOf(0f) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Double]) Literal(DoubleConstant(0d)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.lang.Double]) '{ java.lang.Double.valueOf(0d) }.asExprOf[T]
        else if (isOption(tpe, types.tail)) '{ None }.asExprOf[T]
        else if (tpe <:< TypeRepr.of[mutable.BitSet]) '{ new mutable.BitSet }.asExprOf[T]
        else if (tpe <:< TypeRepr.of[immutable.BitSet]) withNullValueFor(tpe)('{ immutable.BitSet.empty }.asExprOf[T])
        else if (tpe <:< TypeRepr.of[collection.BitSet]) withNullValueFor(tpe)('{ collection.BitSet.empty }.asExprOf[T])
        else if (tpe <:< TypeRepr.of[::[_]]) '{ null }.asExprOf[T]
        else if (tpe <:< TypeRepr.of[List[_]] || tpe =:= TypeRepr.of[Seq[_]]) '{ Nil }.asExprOf[T]
        else if (tpe <:< TypeRepr.of[collection.SortedSet[_]]) withNullValueFor(tpe) {
          val tpe1 = typeArg1(tpe)
          Apply(scalaCollectionEmptyNoArgs(tpe, tpe1), List(summonOrdering(tpe1))).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[_]] || tpe <:< TypeRepr.of[mutable.UnrolledBuffer[_]]) {
          val tpe1 = typeArg1(tpe)
          Apply(scalaCollectionEmptyNoArgs(tpe, tpe1), List(summonClassTag(tpe1))).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[immutable.ArraySeq[_]]) withNullValueFor(tpe) {
          val tpe1 = typeArg1(tpe)
          Apply(scalaCollectionEmptyNoArgs(tpe, tpe1), List(summonClassTag(tpe1))).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[_]] || tpe <:< TypeRepr.of[immutable.LongMap[_]] ||
            tpe <:< TypeRepr.of[mutable.LongMap[_]] || tpe <:< TypeRepr.of[immutable.Seq[_]] ||
            tpe <:< TypeRepr.of[Set[_]]) withNullValueFor(tpe) {
          scalaCollectionEmptyNoArgs(tpe, typeArg1(tpe)).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[immutable.SortedMap[_, _]]) withNullValueFor(tpe) {
          val tpe1 = typeArg1(tpe)
          Apply(scalaMapEmptyNoArgs(tpe, tpe1, typeArg2(tpe)), List(summonOrdering(tpe1))).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[immutable.Map[_, _]]) withNullValueFor(tpe) {
          scalaMapEmptyNoArgs(tpe, typeArg1(tpe), typeArg2(tpe)).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) {
          scalaMapEmptyNoArgs(tpe, typeArg1(tpe), typeArg2(tpe)).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) scalaCollectionEmptyNoArgs(tpe, typeArg1(tpe)).asExprOf[T]
        else if (tpe <:< TypeRepr.of[Array[_]]) withNullValueFor(tpe) {
          typeArg1(tpe).asType match
            case '[t1] => newArray[t1]('{ 0 }).asExprOf[T]
        } else if (isConstType(tpe)) {
          tpe match
            case ConstantType(StringConstant(v)) => Literal(StringConstant(v)).asExprOf[T]
            case ConstantType(BooleanConstant(v)) => Literal(BooleanConstant(v)).asExprOf[T]
            case ConstantType(ByteConstant(v)) => Literal(ByteConstant(v)).asExprOf[T]
            case ConstantType(CharConstant(v)) => Literal(CharConstant(v)).asExprOf[T]
            case ConstantType(ShortConstant(v)) => Literal(ShortConstant(v)).asExprOf[T]
            case ConstantType(IntConstant(v)) => Literal(IntConstant(v)).asExprOf[T]
            case ConstantType(LongConstant(v)) => Literal(LongConstant(v)).asExprOf[T]
            case ConstantType(FloatConstant(v)) => Literal(FloatConstant(v)).asExprOf[T]
            case ConstantType(DoubleConstant(v)) => Literal(DoubleConstant(v)).asExprOf[T]
            case _ => cannotFindValueCodecError(tpe)
        } else if (isEnumOrModuleValue(tpe)) Ref(tpe.termSymbol).asExprOf[T]
        else if (tpe =:= TypeRepr.of[None.type]) '{ None }.asExprOf[T]
        else if (TypeRepr.of[Null] <:< tpe) '{ null }.asExprOf[T]
        else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          tpe1.asType match
            case '[t1] => getClassInfo(tpe).genNew(genNullValue[t1](tpe1 :: types).asTerm).asExprOf[T]
        } else '{ null.asInstanceOf[T] }.asExprOf[T]

      case class ReadDiscriminator(valDef: ValDef) {
        def skip(in: Expr[JsonReader], l: Expr[Int])(using Quotes): Expr[Unit] = '{
          if (${Ref(valDef.symbol).asExprOf[Boolean]}) {
            ${Assign(Ref(valDef.symbol), Literal(BooleanConstant(false))).asExprOf[Unit]}
            $in.skip()
          } else $in.duplicatedKeyError($l)
        }
      }

      def genReadSealedClass[T: Type](types: List[TypeRepr], in: Expr[JsonReader], default: Expr[T],
                                      isStringified: Boolean)(using Quotes): Expr[T] = {
        val tpe = types.head
        val leafClasses = adtLeafClasses(tpe)
        val currentDiscriminator = cfg.discriminatorFieldName
        val discriminatorError =
          cfg.discriminatorFieldName.fold('{ $in.discriminatorError() })(n => '{ $in.discriminatorValueError(${Expr(n)}) })

        def genReadLeafClass[T: Type](subTpe: TypeRepr)(using Quotes): Expr[T] =
          val useDiscriminator = cfg.discriminatorFieldName.isDefined
          if (subTpe =:= tpe) genReadNonAbstractScalaClass(types, useDiscriminator, in, genNullValue[T](types))
          else genReadVal(subTpe :: types, genNullValue[T](subTpe :: types), isStringified, useDiscriminator, in)

        def genReadCollisions[T: Type](subTpes: collection.Seq[TypeRepr], l: Expr[Int])(using Quotes): Expr[T] =
          subTpes.foldRight(discriminatorError.asExprOf[T]) { (subTpe, acc) =>
            subTpe.asType match
              case '[st] => '{
                if ($in.isCharBufEqualsTo($l, ${Expr(discriminatorValue(subTpe))})) ${
                  if (currentDiscriminator.isDefined) '{
                    $in.rollbackToMark()
                    ${genReadLeafClass[st](subTpe)}
                  } else if (isEnumOrModuleValue(subTpe)) Ref(subTpe.termSymbol).asExprOf[st]
                  else if (subTpe =:= TypeRepr.of[None.type]) '{ None }.asExprOf[st]
                  else genReadLeafClass[st](subTpe)
                } else $acc
              }.asExprOf[T]
          }

        def genReadSubclassesBlock[T: Type](leafClasses: collection.Seq[TypeRepr], l: Expr[Int])(using Quotes): Expr[T] =
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
            Match(scrutinee, (cases :+ lastCase).toList).asExprOf[T]
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
                val r = ${genReadSubclassesBlock(objClasses, 'l).asExprOf[T]}
                if ($in.isNextToken('}')) r
                else $in.objectEndOrCommaError()
              } else $in.readNullOrError($default, "expected '\"' or '{' or null")
            }
            case Some(discrFieldName) =>
              if (cfg.requireDiscriminatorFirst) '{
                ${setMark}
                if ($in.isNextToken('{')) {
                  if ($in.isCharBufEqualsTo($in.readKeyAsCharBuf(), ${Expr(discrFieldName)})) {
                    val l = $in.readStringAsCharBuf()
                    ${genReadSubclassesBlock(objClasses, 'l).asExprOf[T]}
                  } else $in.decodeError(${Expr("expected key: \"" + discrFieldName + '"')})
                } else $in.readNullOrTokenError($default, '{')
              } else '{
                ${setMark}
                if ($in.isNextToken('{')) {
                  if ($in.skipToKey(${Expr(discrFieldName)})) {
                    val l = $in.readStringAsCharBuf()
                    ${genReadSubclassesBlock(objClasses, 'l).asExprOf[T]}
                  } else $in.requiredFieldError(${Expr(discrFieldName)})
                } else $in.readNullOrTokenError($default, '{')
              }
        }

        if (currentDiscriminator == None) {
          val (leafModuleClasses, leafCaseClasses) =
            leafClasses.partition(tpe => isEnumOrModuleValue(tpe) || tpe =:= TypeRepr.of[None.type])
          if (leafModuleClasses.nonEmpty && leafCaseClasses.nonEmpty) {
            '{
              if ($in.isNextToken('"')) {
                $in.rollbackToken()
                val l = $in.readStringAsCharBuf()
                ${genReadSubclassesBlock(leafModuleClasses, 'l).asExprOf[T]}
              } else ${genReadJsObjClass(leafCaseClasses, true)}
            }.asExprOf[T]
          } else if (leafCaseClasses.nonEmpty) genReadJsObjClass(leafCaseClasses, false)
          else '{
            if ($in.isNextToken('"')) {
              $in.rollbackToken()
              val l = $in.readStringAsCharBuf()
              ${genReadSubclassesBlock(leafModuleClasses, 'l).asExprOf[T]}
            } else $in.readNullOrTokenError($default, '"')
          }.asExprOf[T]
        } else genReadJsObjClass(leafClasses, false)
      }

      def genReadNonAbstractScalaClass[T: Type](types: List[TypeRepr], useDiscriminator: Boolean, in: Expr[JsonReader],
                                                default: Expr[T])(using Quotes): Expr[T] = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        checkFieldNameCollisions(tpe, cfg.discriminatorFieldName.fold(Seq.empty[String]) { n =>
          val names = classInfo.fields.map(_.mappedName)
          if (!useDiscriminator) names
          else names :+ n
        })
        val required = classInfo.fields.collect {
          case f if !(f.symbol.flags.is(Flags.HasDefault) || isOption(f.resolvedTpe, types) ||
            (isCollection(f.resolvedTpe) && !cfg.requireCollectionFields)) => f.mappedName
        }.toSet
        val paramVarNum = classInfo.fields.size
        val lastParamVarIndex = Math.max(0, (paramVarNum - 1) >> 5)
        val lastParamVarBits = -1 >>> -paramVarNum
        val paramVars = (0 to lastParamVarIndex)
          .map(i => ValDef(symbol("p" + i, TypeRepr.of[Int], Flags.Mutable), Some(Literal(IntConstant {
            if (i == lastParamVarIndex) lastParamVarBits
            else -1
          }))))

        def checkAndResetFieldPresenceFlag(name: String, l: Expr[Int])(using Quotes): Expr[Unit] =
          val fi = classInfo.fields.find(_.mappedName == name)
            .getOrElse(fail(s"Field $name is not found in fields for $classInfo"))
          val n = Ref(paramVars(fi.nonTransientFieldIndex >> 5).symbol).asExprOf[Int]
          val m = Expr(1 << fi.nonTransientFieldIndex)
          '{
            if (($m & $n) != 0) ${Assign(n.asTerm, '{ $n ^ $m }.asTerm).asExprOf[Unit]}
            else $in.duplicatedKeyError($l)
          }

        val checkReqVars =
          if (required.isEmpty) Nil
          else {
            val nameByIndex = withFieldsByIndexFor(tpe)(classInfo.fields.map(_.mappedName))
            val reqMasks = classInfo.fields.grouped(32).toSeq.map(_.zipWithIndex.foldLeft(0) {
              case (acc, (f, i)) =>
                if (required(f.mappedName)) acc | 1 << i
                else acc
            })
            paramVars.zipWithIndex.map { case (nValDef, i) =>
              val n = Ref(nValDef.symbol).asExprOf[Int]
              val reqMask = reqMasks(i)
              if (reqMask == -1 || (i == lastParamVarIndex && reqMask == lastParamVarBits)) {
                val fieldName =
                  if (i == 0) '{ java.lang.Integer.numberOfTrailingZeros($n) }.asTerm
                  else '{ java.lang.Integer.numberOfTrailingZeros($n) + ${Expr(i << 5)} }.asTerm
                '{ if ($n != 0) $in.requiredFieldError(${Apply(nameByIndex, List(fieldName)).asExprOf[String]}) }.asTerm
              } else {
                val m = Expr(reqMask)
                val fieldName =
                  if (i == 0) '{ java.lang.Integer.numberOfTrailingZeros($n & $m) }.asTerm
                  else '{ java.lang.Integer.numberOfTrailingZeros($n & $m) + ${Expr(i << 5)} }.asTerm
                '{ if (($n & $m) != 0) $in.requiredFieldError(${Apply(nameByIndex, List(fieldName)).asExprOf[String]}) }.asTerm
              }
            }.toList
          }
        val readVars = classInfo.fields.map { f =>
          val sym = symbol("_" + f.symbol.name, f.resolvedTpe, Flags.Mutable)
          f.resolvedTpe.asType match
            case '[ft] =>
              ValDef(sym, Some(f.defaultValue.getOrElse(genNullValue[ft](f.resolvedTpe :: types).asTerm.changeOwner(sym))))
        }
        val readVarsMap = classInfo.fields.zip(readVars).map { case (field, tmpVar) =>
          (field.symbol.name, tmpVar)
        }.toMap
        var nonTransientFieldIndex = 0
        val construct = classInfo.genNew(classInfo.paramLists.map(_.foldLeft(List.newBuilder[Term]) {
          (params, fieldInfo) =>
            params.addOne(if (fieldInfo.isTransient) {
              fieldInfo.defaultValue
                .getOrElse(fail(s"Transient field ${fieldInfo.symbol.name} in class ${tpe.show} have no default value"))
            } else {
              val rv = readVars(nonTransientFieldIndex).symbol
              nonTransientFieldIndex += 1
              Ref(rv)
            })
        }.result))
        val readFields = cfg.discriminatorFieldName.fold(classInfo.fields) { n =>
          if (!useDiscriminator) classInfo.fields
          else classInfo.fields :+
            FieldInfo(Symbol.noSymbol, n, Symbol.noSymbol, None, TypeRepr.of[String], isTransient = false,
              isStringified = true, classInfo.fields.length)
        }

        def genReadCollisions(fs: collection.Seq[FieldInfo], tmpVars: Map[String, ValDef],
                              discriminator: Option[ReadDiscriminator], l: Expr[Int])(using Quotes): Expr[Unit] =
          fs.foldRight(unexpectedFieldHandler(in, l)) { (f, acc) =>
            val readValue =
              if (discriminator.nonEmpty && cfg.discriminatorFieldName.contains(f.mappedName)) {
                discriminator.get.skip(in, l)
              } else {
                f.resolvedTpe.asType match
                  case '[ft] =>
                    val tmpVar = Ref(tmpVars(f.symbol.name).symbol)
                    val readVal = genReadVal(f.resolvedTpe :: types, tmpVar.asExprOf[ft], f.isStringified, false, in).asTerm
                    Block(List(checkAndResetFieldPresenceFlag(f.mappedName, l).asTerm), Assign(tmpVar, readVal)).asExprOf[Unit]
              }
            '{
              if ($in.isCharBufEqualsTo($l, ${Expr(f.mappedName)})) $readValue
              else $acc
            }
          }

        val discriminator =
          if (useDiscriminator) {
            cfg.discriminatorFieldName.map { fieldName =>
              val sym = symbol("pd", TypeRepr.of[Boolean], Flags.Mutable)
              ReadDiscriminator(ValDef(sym, Some(Literal(BooleanConstant(true)).changeOwner(sym))))
            }
          } else None
        val optDiscriminatorVar = discriminator.map(_.valDef)

        def readFieldsBlock(l: Expr[Int])(using Quotes): Expr[Unit] = // Using Quotes for w/a, see: https://github.com/lampepfl/dotty/issues/14137
          if (readFields.size <= 8 && readFields.foldLeft(0)(_ + _.mappedName.length) <= 64) {
            genReadCollisions(readFields, readVarsMap, discriminator, l)
          } else {
            val hashCode = (f: FieldInfo) => JsonReader.toHashCode(f.mappedName.toCharArray, f.mappedName.length)
            val cases = groupByOrdered(readFields)(hashCode).map { case (hash, fs) =>
              CaseDef(Literal(IntConstant(hash)), None, genReadCollisions(fs, readVarsMap, discriminator, l).asTerm)
            } :+ CaseDef(Wildcard(), None, unexpectedFieldHandler(in, l).asTerm)
            Match('{ $in.charBufToHashCode($l): @scala.annotation.switch }.asTerm, cases.toList).asExprOf[Unit]
          }

        def blockWithVars(next: Term)(using Quotes): Term =
          Block(readVars.toList ++ paramVars.toList ++ optDiscriminatorVar.toList, next.changeOwner(Symbol.spliceOwner))
            .changeOwner(Symbol.spliceOwner) // All owners should be from top Symbol.spliceOwner because vals are created with this owner

        val readNonEmpty = blockWithVars('{
          if (!$in.isNextToken('}')) {
            $in.rollbackToken()
            var l = -1
            while (l < 0 || $in.isNextToken(',')) {
              l = $in.readKeyAsCharBuf()
              ${readFieldsBlock('l)}
            }
            if (!$in.isCurrentToken('}')) $in.objectEndOrCommaError()
          }
          ${Block(checkReqVars, construct).changeOwner(Symbol.spliceOwner).asExprOf[T]}
        }.asTerm)
        If('{ $in.isNextToken('{') }.asTerm.changeOwner(Symbol.spliceOwner), readNonEmpty,
          '{ $in.readNullOrTokenError($default, '{') }.asTerm.changeOwner(Symbol.spliceOwner)).asExprOf[T]
      }

      def genReadConstType[T: Type](tpe: TypeRepr, isStringified: Boolean, in: Expr[JsonReader])(using Quotes): Expr[T] = tpe match
        case ConstantType(StringConstant(v)) =>
          '{ if ($in.readString(null) != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(BooleanConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readBoolean() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(ByteConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readByte() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(CharConstant(v)) =>
          '{ if ($in.readChar() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(ShortConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readShort() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(IntConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readInt() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(LongConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readLong() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(FloatConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readFloat() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case ConstantType(DoubleConstant(v)) =>
          if (isStringified)
            '{ if ($in.readStringAsDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: \"$v\"")}); ${Expr(v)} }.asExprOf[T]
          else
            '{ if ($in.readDouble() != ${Expr(v)}) $in.decodeError(${Expr(s"expected value: $v")}); ${Expr(v)} }.asExprOf[T]
        case _ => cannotFindValueCodecError(tpe)

      def genReadValForGrowable[G <: Growable[V]: Type, V: Type](types: List[TypeRepr], isStringified: Boolean,
                                                                 x: Expr[G], in: Expr[JsonReader])(using Quotes): Expr[Unit] =
          '{ $x.addOne(${genReadVal(types, genNullValue[V](types), isStringified, false, in)}) }

      def genArraysCopyOf[T: Type](tpe: TypeRepr, x: Expr[Array[T]], newLen: Expr[Int])(using Quotes): Expr[Array[T]] =
        if (tpe =:= TypeRepr.of[Boolean]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Boolean]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Byte]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Byte]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Short]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Short]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Int]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Int]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Long]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Long]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Char]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Char]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Float]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Float]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe =:= TypeRepr.of[Double]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Double]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[AnyRef]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[AnyRef & T]]}, $newLen) }.asExprOf[Array[T]]
        } else fail(s"Can't find Arrays.copyOf for ${tpe.show}")

      def genReadVal[T: Type](types: List[TypeRepr], default: Expr[T], isStringified: Boolean,
                              useDiscriminator: Boolean, in: Expr[JsonReader])(using Quotes): Expr[T] =
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        val methodKey = DecoderMethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe, types.tail)), useDiscriminator)
        val decodeMethodSym = decodeMethodSyms.get(methodKey)
        if (!implCodec.isEmpty) '{ ${implCodec.get.asExprOf[JsonValueCodec[T]]}.decodeValue($in, $default) }
        else if (decodeMethodSym.isDefined) Apply(Ref(decodeMethodSym.get), List(in.asTerm, default.asTerm)).asExprOf[T]
        else if (tpe =:= TypeRepr.of[Boolean]) {
          if (isStringified) '{ $in.readStringAsBoolean() }.asExprOf[T]
          else '{ $in.readBoolean() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
          if (isStringified) '{ java.lang.Boolean.valueOf($in.readStringAsBoolean()) }.asExprOf[T]
          else '{ java.lang.Boolean.valueOf($in.readBoolean()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Byte]) {
          if (isStringified) '{ $in.readStringAsByte() }.asExprOf[T]
          else '{ $in.readByte() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Byte]) {
          if (isStringified) '{ java.lang.Byte.valueOf($in.readStringAsByte()) }.asExprOf[T]
          else '{ java.lang.Byte.valueOf($in.readByte()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Char]) {
          '{ $in.readChar() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Character])
          '{ java.lang.Character.valueOf($in.readChar()) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Short]) {
          if (isStringified) '{ $in.readStringAsShort() }.asExprOf[T]
          else '{ $in.readShort() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Short]) {
          if (isStringified) '{ java.lang.Short.valueOf($in.readStringAsShort()) }.asExprOf[T]
          else '{ java.lang.Short.valueOf($in.readShort()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Int]) {
          if (isStringified) '{ $in.readStringAsInt() }.asExprOf[T]
          else '{ $in.readInt() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Integer]) {
          if (isStringified) '{ java.lang.Integer.valueOf($in.readStringAsInt()) }.asExprOf[T]
          else '{ java.lang.Integer.valueOf($in.readInt()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Long]) {
          if (isStringified) '{ $in.readStringAsLong() }.asExprOf[T]
          else '{ $in.readLong() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Long]) {
          if (isStringified) '{ java.lang.Long.valueOf($in.readStringAsLong()) }.asExprOf[T]
          else '{ java.lang.Long.valueOf($in.readLong()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Float]) {
          if (isStringified) '{ $in.readStringAsFloat() }.asExprOf[T]
          else '{ $in.readFloat() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Float]) {
          if (isStringified) '{ java.lang.Float.valueOf($in.readStringAsFloat()) }.asExprOf[T]
          else '{ java.lang.Float.valueOf($in.readFloat()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[Double]) {
          if (isStringified) '{ $in.readStringAsDouble() }.asExprOf[T]
          else '{ $in.readDouble() }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[java.lang.Double]) {
          if (isStringified) '{ java.lang.Double.valueOf($in.readStringAsDouble()) }.asExprOf[T]
          else '{ java.lang.Double.valueOf($in.readDouble()) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[String]) '{ $in.readString(${default.asExprOf[String]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $in.readUUID(${default.asExprOf[java.util.UUID]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Duration]) '{ $in.readDuration(${default.asExprOf[Duration]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Instant]) '{ $in.readInstant(${default.asExprOf[Instant]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalDate]) '{ $in.readLocalDate(${default.asExprOf[LocalDate]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $in.readLocalDateTime(${default.asExprOf[LocalDateTime]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[LocalTime]) '{ $in.readLocalTime(${default.asExprOf[LocalTime]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[MonthDay]) '{ $in.readMonthDay(${default.asExprOf[MonthDay]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $in.readOffsetDateTime(${default.asExprOf[OffsetDateTime]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $in.readOffsetTime(${default.asExprOf[OffsetTime]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Period]) '{ $in.readPeriod(${default.asExprOf[Period]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Year]) '{ $in.readYear(${default.asExprOf[Year]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[YearMonth]) '{ $in.readYearMonth(${default.asExprOf[YearMonth]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $in.readZonedDateTime(${default.asExprOf[ZonedDateTime]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZoneId]) '{ $in.readZoneId(${default.asExprOf[ZoneId]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $in.readZoneOffset(${default.asExprOf[ZoneOffset]}) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[BigInt]) {
          if (isStringified) '{ $in.readStringAsBigInt(${default.asExprOf[BigInt]}, ${Expr(cfg.bigIntDigitsLimit)}) }.asExprOf[T]
          else '{ $in.readBigInt(${default.asExprOf[BigInt]}, ${Expr(cfg.bigIntDigitsLimit)}) }.asExprOf[T]
        } else if (tpe =:= TypeRepr.of[BigDecimal]) {
          val mc = withMathContextFor(cfg.bigDecimalPrecision)
          if (isStringified) {
            '{
              $in.readStringAsBigDecimal(${default.asExprOf[BigDecimal]}, $mc,
                ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)})
            }.asExprOf[T]
          } else {
            '{
              $in.readBigDecimal(${default.asExprOf[BigDecimal]}, $mc,
                ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)})
            }.asExprOf[T]
          }
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          tpe1.asType match
            case '[t1] =>
              val readVal = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
              getClassInfo(tpe).genNew(readVal.asTerm).asExprOf[T]
        } else if (isOption(tpe, types.tail)) {
          val tpe1 = typeArg1(tpe)
          val nullValue =
            if (cfg.skipNestedOptionValues && tpe <:< TypeRepr.of[Option[Option[_]]]) '{ new Some(None) }.asExprOf[T]
            else default
          tpe1.asType match
            case '[t1] =>
              val readVal1 = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
              '{
                if ($in.isNextToken('n')) $in.readNullOrError($nullValue, "expected value or null")
                else {
                  $in.rollbackToken()
                  new Some($readVal1)
                }
              }.asExprOf[T]
        } else if (tpe <:< TypeRepr.of[Array[_]] || tpe <:< TypeRepr.of[immutable.ArraySeq[_]] ||
          tpe <:< TypeRepr.of[mutable.ArraySeq[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val newArrayOnChange = tpe1 match
                case AppliedType(_, _) => true
                case _ => isValueClass(tpe1)

              def growArray(x: Expr[Array[t1]], i: Expr[Int], l: Expr[Int])(using Quotes): Expr[Array[t1]] =
                if (newArrayOnChange) '{
                  val x1 = ${newArray[t1](l)}
                  java.lang.System.arraycopy($x, 0, x1, 0, $i)
                  x1
                } else genArraysCopyOf[t1](tpe1, x, l)

              def shrinkArray(x: Expr[Array[t1]], i: Expr[Int])(using Quotes): Expr[Array[t1]] =
                if (newArrayOnChange) '{
                  val x1 = ${newArray[t1](i)}
                  java.lang.System.arraycopy($x, 0, x1, 0, $i)
                  x1
                } else genArraysCopyOf[t1](tpe1, x, i)

              if (tpe <:< TypeRepr.of[immutable.ArraySeq[_]]) {
                genReadArray(l => newArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExprOf[Unit]}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExprOf[Unit]}
                  }
                  $x($i) = ${genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)}
                }, default.asExprOf[immutable.ArraySeq[t1]], (x, i, l) => '{
                  immutable.ArraySeq.unsafeWrapArray[t1]({
                    if ($i == $l) $x
                    else ${shrinkArray(x, i)}
                  })
                }.asExprOf[immutable.ArraySeq[t1]], in).asExprOf[T]
              } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[_]]) {
                genReadArray(l => newArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExprOf[Unit]}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExprOf[Unit]}
                  }
                  $x($i) = ${genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)}
                }, default.asExprOf[mutable.ArraySeq[t1]], (x, i, l) => '{
                  mutable.ArraySeq.make[t1]({
                    if ($i == $l) $x
                    else ${shrinkArray(x, i)}
                  })
                }.asExprOf[mutable.ArraySeq[t1]], in).asExprOf[T]
              } else {
                genReadArray(l => newArray[t1](l), (x, i, l) => '{
                  if ($i == $l) {
                    ${Assign(l.asTerm, '{ $l << 1 }.asTerm).asExprOf[Unit]}
                    ${Assign(x.asTerm, growArray(x, i, l).asTerm).asExprOf[Unit]}
                  }
                  $x($i) = ${genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)}
                }, default.asExprOf[Array[t1]], (x, i, l) => '{
                  if ($i == $l) $x
                  else ${shrinkArray(x, i)}
                }.asExprOf[Array[t1]], in).asExprOf[T]
              }
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val newBuilder = withNullValueFor(tpe)(scalaCollectionEmptyNoArgs(tpe, tpe1).asExprOf[immutable.IntMap[t1]])
              val readVal = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
              if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsInt() }
                  else '{ $in.readInt() }
                genReadMapAsArray(newBuilder, x => Assign(x.asTerm, '{
                  $x.updated($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() })
                }.asTerm).asExprOf[Unit], _.asExprOf[T], in, default.asExprOf[T])
              } else genReadMap(newBuilder.asExprOf[immutable.IntMap[t1]],
                x => Assign(x.asTerm, '{ $x.updated($in.readKeyAsInt(), $readVal) }.asTerm).asExprOf[Unit], identity, in,
                default.asExprOf[immutable.IntMap[t1]]).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[mutable.LongMap[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asExprOf[mutable.LongMap[t1]]
              val newBuilder = '{
                if ($tDefault.isEmpty) $tDefault
                else ${scalaCollectionEmptyNoArgs(tpe, tpe1).asExprOf[mutable.LongMap[t1]]}
              }
              val readVal = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
              if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsLong() }
                  else '{ $in.readLong() }
                genReadMapAsArray(newBuilder,
                  x => '{ $x.update($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() }) },
                  identity, in, tDefault).asExprOf[T]
              } else {
                genReadMap(newBuilder,
                  x => '{ $x.update($in.readKeyAsLong(), $readVal) }, identity, in, tDefault).asExprOf[T]
              }
        } else if (tpe <:< TypeRepr.of[immutable.LongMap[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val newBuilder = withNullValueFor(tpe)(scalaCollectionEmptyNoArgs(tpe, tpe1).asExprOf[immutable.LongMap[t1]])
              val readVal = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
              if (cfg.mapAsArray) {
                val readKey =
                  if (cfg.isStringified) '{ $in.readStringAsLong() }
                  else '{ $in.readLong() }
                genReadMapAsArray(newBuilder,
                  x => Assign(x.asTerm, '{ $x.updated($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() })}.asTerm).asExprOf[Unit],
                  x => x, in, default.asExprOf[immutable.LongMap[t1]]).asExprOf[T]
              } else {
                genReadMap(newBuilder, x => Assign(x.asTerm, '{ $x.updated($in.readKeyAsLong(), $readVal) }.asTerm).asExprOf[Unit],
                  identity, in, default.asExprOf[immutable.LongMap[t1]]).asExprOf[T]
              }
        } else if (tpe <:< TypeRepr.of[mutable.Map[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          (tpe1.asType, tpe2.asType) match
            case ('[t1], '[t2]) =>
              val tDefault = default.asExprOf[T & mutable.Map[t1, t2]]
              val newBuilder = '{
                if ($tDefault.isEmpty) $tDefault
                else ${scalaMapEmptyNoArgs(tpe, tpe1, tpe2).asExprOf[T & mutable.Map[t1, t2]]}
              }.asExprOf[T & mutable.Map[t1, t2]]

              def readVal2(using Quotes) =
                genReadVal(tpe2 :: types, genNullValue[t2](tpe2 :: types), isStringified, false, in)

              if (cfg.mapAsArray) {
                val readVal1 = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)
                genReadMapAsArray(newBuilder,
                  x => '{ $x.update($readVal1, { if ($in.isNextToken(',')) $readVal2 else $in.commaError() }) },
                  identity, in, tDefault).asExprOf[T]
              } else {
                genReadMap(newBuilder, x => '{ $x.update(${genReadKey[t1](tpe1 :: types, in)}, $readVal2) },
                  identity, in, tDefault).asExprOf[T]
              }
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          (tpe1.asType, tpe2.asType) match
            case ('[t1], '[t2]) =>
              val builderNoApply =
                TypeApply(Select.unique(scalaCollectionCompanion(tpe), "newBuilder"), List(TypeTree.of[t1], TypeTree.of[t2]))
              val newBuilder =
                (if (tpe <:< TypeRepr.of[immutable.SortedMap[_, _]]) Apply(builderNoApply, List(summonOrdering(tpe1))) // TODO: add withOrderfingFoe
                else builderNoApply).asExprOf[mutable.Builder[(t1, t2), T & collection.Map[t1, t2]]]

              def readVal2(using Quotes) =
                genReadVal(tpe2 :: types, genNullValue[t2](tpe2 :: types), isStringified, false, in)

              if (cfg.mapAsArray) {
                def readVal1(using Quotes) =
                  genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, false, in)

                genReadMapAsArray(newBuilder, x => '{ $x.addOne(($readVal1, { if ($in.isNextToken(',')) $readVal2 else $in.commaError() })): Unit},
                  x => '{ $x.result() }, in, default).asExprOf[T]
              } else {
                def readKey(using Quotes) = genReadKey[t1](tpe1 :: types, in)

                genReadMap(newBuilder, x => '{ $x.addOne(($readKey, $readVal2)): Unit },
                  x => '{ $x.result() }, in, default).asExprOf[T]
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
                  val xi = v >>> 6
                  if (xi >= x.length) x = java.util.Arrays.copyOf(x, java.lang.Integer.highestOneBit(xi) << 1)
                  x(xi) |= 1L << v
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken(']')) ${
                  if (tpe =:= TypeRepr.of[BitSet] || tpe =:= TypeRepr.of[immutable.BitSet]) '{ immutable.BitSet.fromBitMaskNoCopy(x) }
                  else '{ mutable.BitSet.fromBitMaskNoCopy(x) }
                } else $in.arrayEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '[')
          }.asExprOf[T]
        } else if (tpe <:< TypeRepr.of[mutable.Set[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asExprOf[T & mutable.Set[t1]]
              val emptySetNoOrdering = scalaCollectionEmptyNoArgs(tpe, tpe1)
              val emptySet =
                (if (tpe <:< TypeRepr.of[mutable.SortedSet[_]]) Apply(emptySetNoOrdering, List(summonOrdering(tpe1)))
                else emptySetNoOrdering).asExprOf[T & mutable.Set[t1]]
              genReadSet('{
                if ($tDefault.isEmpty) $tDefault
                else $emptySet
              }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), tDefault, identity, in).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[collection.Set[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val builderNoOrdering =
                TypeApply(Select.unique(scalaCollectionCompanion(tpe), "newBuilder"), List(TypeTree.of[t1]))
              val builder =
                (if (tpe <:< TypeRepr.of[collection.SortedSet[_]]) Apply(builderNoOrdering, List(summonOrdering(tpe1)))
                else builderNoOrdering).asExprOf[mutable.Builder[t1, T & collection.Set[t1]]]
              genReadSet(builder, b => genReadValForGrowable(tpe1 :: types, isStringified, b, in),
                default.asExprOf[T & collection.Set[t1]], b => '{ $b.result() }, in).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[::[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match {
            case '[t1] =>
              val tDefault = default.asExprOf[::[t1]]
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
              }.asExprOf[T]
          }
        } else if (tpe <:< TypeRepr.of[List[_]] || tpe =:= TypeRepr.of[Seq[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genReadCollection('{ new mutable.ListBuffer[t1] },
                x => genReadValForGrowable(tpe1 :: types, isStringified, x, in),
                default.asExprOf[List[t1]], x => '{ $x.toList }, in).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[mutable.ListBuffer[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asExprOf[mutable.ListBuffer[t1]]
              genReadCollection('{
                if ($tDefault.isEmpty) $tDefault
                else new mutable.ListBuffer[t1]
              }, x => genReadValForGrowable(tpe1 :: types, isStringified, x, in), tDefault, x => x, in).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[mutable.Iterable[_] with mutable.Growable[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val emptyCollection = {
                if (tpe <:< TypeRepr.of[mutable.ArraySeq[_]] || tpe <:< TypeRepr.of[mutable.UnrolledBuffer[_]]) {
                  Apply(scalaCollectionEmptyNoArgs(tpe, tpe1), List(summonClassTag(tpe1)))
                } else scalaCollectionEmptyNoArgs(tpe, tpe1)
              }.asExprOf[T & mutable.Growable[t1]]
              genReadCollection('{
                if (${default.asExprOf[Iterable[_]]}.isEmpty) $default
                else $emptyCollection
              }.asExprOf[T & mutable.Growable[t1]], x => genReadValForGrowable(tpe1 :: types, isStringified, x, in),
                default, x => x, in).asExprOf[T]
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val builder = TypeApply(Select.unique(scalaCollectionCompanion(tpe), "newBuilder"), List(TypeTree.of[t1]))
              genReadCollection({
                if (tpe <:< TypeRepr.of[mutable.ArraySeq[_]]) Apply(builder, List(summonClassTag(tpe1)))
                else builder
              }.asExprOf[mutable.Builder[t1, T]], x => genReadValForGrowable(tpe1 :: types, isStringified, x, in),
                default, x => '{ $x.result() }, in)
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
                if (${'x.asExprOf[AnyRef]} eq null) {
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
        } else if (isEnumOrModuleValue(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          '{
            if ($in.isNextToken('{')) {
              $in.rollbackToken()
              $in.skip()
              ${Ref(tpe.termSymbol).asExprOf[T]}
            } else $in.readNullOrTokenError($default, '{')
          }
        } else if (tpe =:= TypeRepr.of[None.type]) withDecoderFor(methodKey, default, in) { (in, default) =>
          '{
            if ($in.isNextToken('{')) {
              $in.rollbackToken()
              $in.skip()
              ${'{ None }.asExprOf[T]}
            } else $in.readNullOrTokenError($default, '{')
          }
        } else if (isTuple(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          val valDefs = new ArrayBuffer[ValDef]
          val indexedTypes = typeArgs(tpe).zipWithIndex
          indexedTypes.foreach { case (te, i) =>
            te.asType match
              case '[t] =>
                val sym = symbol("_r" + (i + 1), te)
                val nullVal = genNullValue[t](te :: types)
                val rhs =
                  if (i == 0) genReadVal(te :: types, nullVal, isStringified, false, in)
                  else '{
                    if ($in.isNextToken(',')) {
                      ${genReadVal(te :: types, nullVal, isStringified, false, in)}
                    } else $in.commaError()
                  }
                valDefs.addOne(ValDef(sym, Some(rhs.asTerm.changeOwner(sym))))
          }
          val readCreateBlock = Block(valDefs.toList, '{
            if ($in.isNextToken(']')) {
              ${Apply(TypeApply(Select.unique(New(Inferred(tpe)), "<init>"),
                indexedTypes.map(x => Inferred(x._1)).toList), valDefs.map(x => Ref(x.symbol)).toList).asExpr}
            } else $in.arrayEndError()
          }.asTerm)
          '{
            if ($in.isNextToken('[')) ${readCreateBlock.asExprOf[T]}
            else $in.readNullOrTokenError($default, '[')
          }
        } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          genReadSealedClass(types, in, default, isStringified)
        } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          genReadNonAbstractScalaClass(types, useDiscriminator, in, default)
        } else if (isConstType(tpe)) genReadConstType(tpe, isStringified, in)
        else cannotFindValueCodecError(tpe)

      def genWriteNonAbstractScalaClass[T: Type](x: Expr[T], types: List[TypeRepr],
                                                 optDiscriminator: Option[WriteDiscriminator],
                                                 out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        val writeFields = classInfo.fields.map { f =>
          val fDefault =
            if (cfg.transientDefault) f.defaultValue
            else None
          f.resolvedTpe.asType match {
            case '[ft] =>
              fDefault match {
                case Some(d) =>
                  if (f.resolvedTpe <:< TypeRepr.of[Iterable[_]] && cfg.transientEmpty) {
                    val tpe1 = typeArg1(f.resolvedTpe.baseType(TypeRepr.of[Iterable[_]].typeSymbol))
                    tpe1.asType match
                      case '[t1] => '{
                        val v = ${Select(x.asTerm, f.getterOrField).asExprOf[Iterable[t1]]}
                        if (!v.isEmpty && v != ${d.asExprOf[ft]}) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('v, TypeRepr.of[Iterable[t1]] :: types, f.isStringified, None, out)}
                        }
                      }
                  } else if (isOption(f.resolvedTpe, types) && cfg.transientNone) {
                    val tpe1 = typeArg1(f.resolvedTpe)
                    tpe1.asType match
                      case '[t1] => '{
                        val v = ${Select(x.asTerm, f.getterOrField).asExprOf[Option[t1]]}
                        if ((v ne None) && v != ${d.asExprOf[ft]}) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('{v.get}, tpe1 :: f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                      }
                  } else if (f.resolvedTpe <:< TypeRepr.of[Array[_]]) {
                    def cond(v: Expr[Array[_]])(using Quotes): Expr[Boolean] =
                      val da = d.asExprOf[Array[_]]
                      if (cfg.transientEmpty)
                        '{ $v.length > 0 && !${withEqualsFor(f.resolvedTpe, v, da)((x1, x2) => genArrayEquals(f.resolvedTpe, x1, x2))} }
                      else
                        '{ !${withEqualsFor(f.resolvedTpe, v, da)((x1, x2) => genArrayEquals(f.resolvedTpe, x1, x2))} }

                    '{
                      val v = ${Select(x.asTerm, f.getterOrField).asExprOf[ft & Array[_]]}
                      if (${cond('v)}) {
                        ${genWriteConstantKey(f.mappedName, out)}
                        ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                      }
                    }
                  } else '{
                    val v = ${Select(x.asTerm, f.getterOrField).asExprOf[ft]}
                    if (v != ${d.asExprOf[ft]}) {
                      ${genWriteConstantKey(f.mappedName, out)}
                      ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                    }
                  }
                case None =>
                  if (f.resolvedTpe <:< TypeRepr.of[Iterable[_]] && cfg.transientEmpty) '{
                    val v = ${Select(x.asTerm, f.getterOrField).asExprOf[ft & Iterable[_]]}
                    if (!v.isEmpty) {
                      ${genWriteConstantKey(f.mappedName, out)}
                      ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                    }
                  } else if (isOption(f.resolvedTpe, types) && cfg.transientNone) {
                    val tpe1 = typeArg1(f.resolvedTpe)
                    tpe1.asType match
                      case '[tf] => '{
                        val v = ${Select(x.asTerm, f.getterOrField).asExprOf[Option[tf]]}
                        if (v ne None) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('{ v.get }, tpe1 :: f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                      }
                  } else if (f.resolvedTpe <:< TypeRepr.of[Array[_]] && cfg.transientEmpty) '{
                    val v = ${Select(x.asTerm, f.getterOrField).asExprOf[ft & Array[_]]}
                    if (v.length > 0) {
                      ${genWriteConstantKey(f.mappedName, out)}
                      ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                    }
                  } else '{
                    ${genWriteConstantKey(f.mappedName, out)}
                    ${genWriteVal(Select(x.asTerm, f.getterOrField).asExprOf[ft], f.resolvedTpe :: types, f.isStringified, None, out)}
                  }
              }
          }
        }
        val allWriteFields = optDiscriminator.fold(writeFields)(_.write(out) +: writeFields)
        Block('{ $out.writeObjectStart() }.asTerm :: allWriteFields.toList.map(_.asTerm.changeOwner(Symbol.spliceOwner)),
          '{ $out.writeObjectEnd() }.asTerm).asExprOf[Unit]

      def getWriteConstType(tpe: TypeRepr, isStringified: Boolean, out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        tpe match
          case ConstantType(StringConstant(v)) => '{ $out.writeVal(${Expr(v)}) }
          case ConstantType(BooleanConstant(v)) =>
            if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
            else '{ $out.writeVal(${Expr(v)}) }
          case ConstantType(ByteConstant(v)) =>
            if (isStringified) '{ $out.writeValAsString(${Expr(v)}) }
            else '{ $out.writeVal(${Expr(v)}) }
          case ConstantType(CharConstant(v)) => '{ $out.writeVal(${Expr(v)}) }
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
          case _ => cannotFindValueCodecError(tpe)

      def genWriteVal[T: Type](m: Expr[T], types: List[TypeRepr], isStringified: Boolean,
                               optWriteDiscriminator: Option[WriteDiscriminator],
                               out: Expr[JsonWriter])(using Quotes): Expr[Unit] =
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        val methodKey = EncoderMethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe, types.tail)),
            optWriteDiscriminator.map(x => (x.fieldName, x.fieldValue)))
        val encodeMethodSym = encodeMethodSyms.get(methodKey)
        if (!implCodec.isEmpty) '{ ${implCodec.get.asExprOf[JsonValueCodec[T]]}.encodeValue($m, $out) }
        else if (encodeMethodSym.isDefined) Apply(Ref(encodeMethodSym.get), List(m.asTerm, out.asTerm)).asExprOf[Unit]
        else if (tpe =:= TypeRepr.of[Boolean]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Boolean]}) }
          else '{ $out.writeVal(${m.asExprOf[Boolean]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Boolean]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Boolean]}) }
        } else if (tpe =:= TypeRepr.of[Byte]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Byte]}) }
          else '{ $out.writeVal(${m.asExprOf[Byte]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Byte]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Byte]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Byte]}) }
        } else if (tpe =:= TypeRepr.of[Short]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Short]}) }
          else '{ $out.writeVal(${m.asExprOf[Short]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Short]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Short]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Short]}) }
        } else if (tpe =:= TypeRepr.of[Int]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Int]}) }
          else '{ $out.writeVal(${m.asExprOf[Int]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Integer]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Integer]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Integer]}) }
        } else if (tpe =:= TypeRepr.of[Long]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Long]}) }
          else '{ $out.writeVal(${m.asExprOf[Long]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Long]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Long]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Long]}) }
        } else if (tpe =:= TypeRepr.of[Float]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Float]}) }
          else '{ $out.writeVal(${m.asExprOf[Float]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Float]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Float]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Float]}) }
        } else if (tpe =:= TypeRepr.of[Double]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Double]}) }
          else '{ $out.writeVal(${m.asExprOf[Double]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Double]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Double]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Double]}) }
        } else if (tpe =:= TypeRepr.of[BigInt]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[BigInt]}) }
          else '{ $out.writeVal(${m.asExprOf[BigInt]}) }
        } else if (tpe =:= TypeRepr.of[BigDecimal]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[BigDecimal]}) }
          else '{ $out.writeVal(${m.asExprOf[BigDecimal]}) }
        } else if (tpe =:= TypeRepr.of[Char]) '{ $out.writeVal(${m.asExprOf[Char]}) }
        else if (tpe =:= TypeRepr.of[java.lang.Character]) '{ $out.writeVal(${m.asExprOf[java.lang.Character]}) }
        else if (tpe =:= TypeRepr.of[String]) '{ $out.writeVal(${m.asExprOf[String]}) }
        else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $out.writeVal(${m.asExprOf[java.util.UUID]}) }
        else if (tpe =:= TypeRepr.of[Duration]) '{ $out.writeVal(${m.asExprOf[Duration]}) }
        else if (tpe =:= TypeRepr.of[Instant]) '{ $out.writeVal(${m.asExprOf[Instant]}) }
        else if (tpe =:= TypeRepr.of[LocalDate]) '{ $out.writeVal(${m.asExprOf[LocalDate]}) }
        else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $out.writeVal(${m.asExprOf[LocalDateTime]}) }
        else if (tpe =:= TypeRepr.of[LocalTime]) '{ $out.writeVal(${m.asExprOf[LocalTime]}) }
        else if (tpe =:= TypeRepr.of[MonthDay]) '{ $out.writeVal(${m.asExprOf[MonthDay]}) }
        else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $out.writeVal(${m.asExprOf[OffsetDateTime]}) }
        else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $out.writeVal(${m.asExprOf[OffsetTime]}) }
        else if (tpe =:= TypeRepr.of[Period]) '{ $out.writeVal(${m.asExprOf[Period]}) }
        else if (tpe =:= TypeRepr.of[Year]) '{ $out.writeVal(${m.asExprOf[Year]}) }
        else if (tpe =:= TypeRepr.of[YearMonth]) '{ $out.writeVal(${m.asExprOf[YearMonth]}) }
        else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $out.writeVal(${m.asExprOf[ZonedDateTime]}) }
        else if (tpe =:= TypeRepr.of[ZoneId]) '{ $out.writeVal(${m.asExprOf[ZoneId]}) }
        else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $out.writeVal(${m.asExprOf[ZoneOffset]}) }
        else if (isValueClass(tpe)) {
          val vtpe = valueClassValueType(tpe)
          genWriteVal(Select(m.asTerm, valueClassValue(tpe)).asExpr, vtpe :: types, isStringified, None, out)
        } else if (isOption(tpe, types.tail)) {
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] => '{
              ${m.asExprOf[Option[t1]]} match
                case Some(x) => ${genWriteVal('x, tpe1 :: types, isStringified, None, out)}
                case None => $out.writeNull()
            }
        } else if (tpe <:< TypeRepr.of[Array[_]] || tpe <:< TypeRepr.of[immutable.ArraySeq[_]] ||
          tpe <:< TypeRepr.of[mutable.ArraySeq[_]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              if (tpe <:< TypeRepr.of[immutable.ArraySeq[_]]) {
                val tx = x.asExprOf[immutable.ArraySeq[t1]]
                '{
                  $out.writeArrayStart()
                  val xs = $tx.unsafeArray.asInstanceOf[Array[t1]]
                  val l = xs.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ xs(i) }, tpe1 :: types, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              } else if (tpe <:< TypeRepr.of[mutable.ArraySeq[_]]) {
                val tx = x.asExprOf[mutable.ArraySeq[t1]]
                '{
                  $out.writeArrayStart()
                  val xs = $tx.array.asInstanceOf[Array[t1]]
                  val l = xs.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ xs(i) }, tpe1 :: types, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              } else {
                val tx = x.asExprOf[Array[t1]]
                '{
                  $out.writeArrayStart()
                  val l = $tx.length
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ $tx(i) }, tpe1 :: types, isStringified, None, out)}
                    i += 1
                  }
                  $out.writeArrayEnd()
                }
              }
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[_]] || tpe <:< TypeRepr.of[mutable.LongMap[_]] ||
            tpe <:< TypeRepr.of[immutable.LongMap[_]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              def writeVal2(out: Expr[JsonWriter], v: Expr[t1])(using Quotes) =
                genWriteVal(v, tpe1 :: types, isStringified, None, out)

              if (tpe <:< TypeRepr.of[immutable.IntMap[_]]) {
                val tx = x.asExprOf[immutable.IntMap[t1]]
                if (cfg.mapAsArray) {
                  def writeVal1(out: Expr[JsonWriter], k: Expr[Int])(using Quotes): Expr[Unit] =
                    if (isStringified) '{ $out.writeValAsString($k) }
                    else '{ $out.writeVal($k) }

                  genWriteMapAsArrayScala213(tx, writeVal1, writeVal2, out)
                } else genWriteMapScala213(tx, (out, k) => '{ $out.writeKey($k) }, writeVal2, out)
              } else {
                val tx = x.asExprOf[collection.Map[Long, t1]]
                if (cfg.mapAsArray) {
                  def writeVal1(out: Expr[JsonWriter], k: Expr[Long])(using Quotes): Expr[Unit] =
                    if (isStringified) '{ $out.writeValAsString($k) }
                    else '{ $out.writeVal($k) }

                  genWriteMapAsArrayScala213(tx, writeVal1, writeVal2, out)
                } else genWriteMapScala213(tx, (out, k) => '{ $out.writeKey($k) }, writeVal2, out)
              }
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          (tpe1.asType, tpe2.asType) match
            case ('[t1], '[t2]) =>
              def writeVal2(out: Expr[JsonWriter], v: Expr[t2])(using Quotes): Expr[Unit] =
                genWriteVal(v, tpe2 :: types, isStringified, None, out)

              val tx = x.asExprOf[collection.Map[t1, t2]]
              if (cfg.mapAsArray) {
                genWriteMapAsArrayScala213(tx, (out, k) => genWriteVal(k, tpe1 :: types, isStringified, None, out), writeVal2, out)
              } else genWriteMapScala213(tx, (out, k) => genWriteKey(k, tpe1 :: types, out), writeVal2, out)
        } else if (tpe <:< TypeRepr.of[BitSet]) withEncoderFor(methodKey, m, out) { (out, x) =>
          genWriteArray(x.asExprOf[BitSet], (out, x1) => {
            if (isStringified) '{ $out.writeValAsString($x1) }
            else '{ $out.writeVal($x1) }
          }, out)
        } else if (tpe <:< TypeRepr.of[List[_]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tx = x.asExprOf[List[t1]]
              '{
                $out.writeArrayStart()
                var l = $tx
                while (l ne Nil) {
                  ${genWriteVal('{ l.head }, tpe1 :: types, isStringified, None, out)}
                  l = l.tail
                }
                $out.writeArrayEnd()
              }
        } else if (tpe <:< TypeRepr.of[IndexedSeq[_]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tx = x.asExprOf[IndexedSeq[t1]]
              '{
                $out.writeArrayStart()
                val l = $tx.size
                if (l <= 32) {
                  var i = 0
                  while (i < l) {
                    ${genWriteVal('{ $tx(i) }, tpe1 :: types, isStringified, None, out)}
                    i += 1
                  }
                } else $tx.foreach(x => ${genWriteVal('x, tpe1 :: types, isStringified, None, out)})
                $out.writeArrayEnd()
              }
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              genWriteArray(x.asExprOf[Iterable[t1]],
                (out, x1) => genWriteVal(x1, tpe1 :: types, isStringified, None, out), out)
        } else if (tpe <:< TypeRepr.of[Enumeration#Value]) withEncoderFor(methodKey, m, out) { (out, x) =>
          val tx = x.asExprOf[Enumeration#Value]
          if (cfg.useScalaEnumValueId) {
            if (isStringified) '{ $out.writeValAsString($tx.id) }
            else '{ $out.writeVal($tx.id) }
          } else '{ $out.writeVal($tx.toString) }
        } else if (isJavaEnum(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => CaseDef(Ref(e.value), None, Expr(e.name).asTerm)) :+
              CaseDef('{ null }.asTerm, None, '{ $out.encodeError("illegal enum value: null") }.asTerm)
            val matching = Match(x.asTerm, cases.toList).asExprOf[String]
            if (encodingRequired) '{ $out.writeVal($matching) }
            else '{ $out.writeNonEscapedAsciiVal($matching) }
          } else {
            val tx = x.asExprOf[java.lang.Enum[_]]
            if (encodingRequired) '{ $out.writeVal($tx.name) }
            else '{ $out.writeNonEscapedAsciiVal($tx.name) }
          }
        } else if (isEnumOrModuleValue(tpe) || tpe =:= TypeRepr.of[None.type]) withEncoderFor(methodKey, m, out) { (out, x) =>
          '{
            $out.writeObjectStart()
            ${optWriteDiscriminator.fold('{})(_.write(out))}
            $out.writeObjectEnd()
          }
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
          val writeFields = typeArgs(tpe).zipWithIndex.map { case (te, i) =>
            te.asType match
              case '[t] =>
                genWriteVal(Select.unique(x.asTerm, "_" + (i + 1)).asExprOf[t], te :: types, isStringified, None, out).asTerm
          }.toList
          if (writeFields.isEmpty) fail(s"Expected that ${tpe.show} should be an applied type")
          Block('{ $out.writeArrayStart() }.asTerm :: writeFields, '{ $out.writeArrayEnd() }.asTerm).asExprOf[Unit]
        } else if (isSealedClass(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
          def genWriteLeafClass(subTpe: TypeRepr, discriminator: Option[WriteDiscriminator], vx: Term): Expr[Unit] =
            subTpe.asType match
              case '[st] =>
                if (subTpe =:= tpe) genWriteNonAbstractScalaClass(vx.asExprOf[st], types, discriminator, out)
                else genWriteVal(vx.asExprOf[st], subTpe :: types, isStringified, discriminator, out)

          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = cfg.discriminatorFieldName.fold {
            val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(x => isEnumOrModuleValue(x) || x =:= TypeRepr.of[None.type])
            leafCaseClasses.map { subTpe =>
              val vxSym = Symbol.newBind(Symbol.spliceOwner, "vx", Flags.EmptyFlags, subTpe)
              CaseDef(Bind(vxSym, Typed(Wildcard(), Inferred(subTpe))), None, '{
                $out.writeObjectStart()
                ${genWriteConstantKey(discriminatorValue(subTpe), out)}
                ${genWriteLeafClass(subTpe, None, Ref(vxSym))}
                $out.writeObjectEnd()
              }.asTerm)
            } ++ leafModuleClasses.map { subTpe =>
              CaseDef(Typed(x.asTerm, Inferred(subTpe)), None,
                genWriteConstantVal(discriminatorValue(subTpe), out).asTerm)
            }
          } { discrFieldName =>
            leafClasses.map { subTpe =>
              val vxSym = Symbol.newBind(Symbol.spliceOwner, "vx", Flags.EmptyFlags, subTpe)
              val writeDiscriminator = WriteDiscriminator(discrFieldName, discriminatorValue(subTpe))
              CaseDef(Bind(vxSym, Typed(Wildcard(), Inferred(subTpe))), None,
                genWriteLeafClass(subTpe, Some(writeDiscriminator), Ref(vxSym)).asTerm)
            }
          } :+ CaseDef(Literal(NullConstant()), None, '{ $out.writeNull() }.asTerm)
          Match(x.asTerm, writeSubclasses.toList).asExprOf[Unit]
        } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m, out) { (out, x) =>
          genWriteNonAbstractScalaClass(x.asExprOf[T], types, optWriteDiscriminator, out)
        } else if (isConstType(tpe)) getWriteConstType(tpe, isStringified, out)
        else cannotFindValueCodecError(tpe)

      val codecDef = '{ //FIXME: generate a type class instance using `ClassDef.apply` and `Symbol.newClass` calls after graduating from experimental API: https://www.scala-lang.org/blog/2022/06/21/scala-3.1.3-released.html
        new JsonValueCodec[A] {
          def nullValue: A = ${genNullValue[A](rootTpe :: Nil)}

          def decodeValue(in: JsonReader, default: A): A =
            ${genReadVal(rootTpe :: Nil, 'default, cfg.isStringified, false, 'in)}

          def encodeValue(x: A, out: JsonWriter): Unit =
            ${genWriteVal('x, rootTpe :: Nil, cfg.isStringified, None, 'out)}
        }
      }.asTerm
      val needDefs =
        mathContexts.values ++
          nullValues.values ++
          equalsMethods.values ++
          scalaEnumCaches.values ++
          fieldIndexAccessors.values ++
          decodeMethodDefs.values ++
          encodeMethodDefs.values
      val codec = Block(needDefs.toList, codecDef).asExprOf[JsonValueCodec[A]]
      if (//FIXME: uncomment after graduating from experimental API: CompilationInfo.XmacroSettings.contains("print-codecs") ||
        Expr.summon[CodecMakerConfig.PrintCodec].isDefined) {
        report.info(s"Generated JSON codec for type '${rootTpe.show}':\n${codec.show}", Position.ofMacroExpansion)
      }
      codec
    }
  }

  private[this] def isEncodingRequired(s: String): Boolean =
    val len = s.length
    var i = 0
    while (i < len && JsonWriter.isNonEscapedAscii(s.charAt(i))) i += 1
    i != len

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