package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.time._
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}

import scala.language.implicitConversions
import scala.annotation.{StaticAnnotation, tailrec}
import scala.annotation.meta.field
import scala.collection.{BitSet, immutable, mutable}
import scala.collection.mutable.Growable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

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
  * @param transientDefault       (always OFF in scala3)  a flag that turns on skipping serialization of fields that have same values as
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
class CodecMakerConfig(
    val fieldNameMapper: FieldNameMapper,
    val javaEnumValueNameMapper: FieldNameMapper,
    val adtLeafClassNameMapper: FieldNameMapper,
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
  inline def withFieldNameMapper(fieldNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(fieldNameMapper = FieldNameFunctionWrapper(fieldNameMapper))

  inline def withJavaEnumValueNameMapper(javaEnumValueNameMapper: PartialFunction[String, String]): CodecMakerConfig =
    copy(javaEnumValueNameMapper = FieldNameFunctionWrapper(javaEnumValueNameMapper))

  inline def withAdtLeafClassNameMapper(adtLeafClassNameMapper: String => String): CodecMakerConfig =
    copy(adtLeafClassNameMapper = FieldNameFunctionWrapper({ case x => adtLeafClassNameMapper(x) }))

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

  def copy(fieldNameMapper: FieldNameMapper = fieldNameMapper,
                         javaEnumValueNameMapper: FieldNameMapper = javaEnumValueNameMapper,
                         adtLeafClassNameMapper: FieldNameMapper = adtLeafClassNameMapper,
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
  transientDefault = false,  // scala3 -- off
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
  useScalaEnumValueId = false) {

     given FromExpr[CodecMakerConfig] with {

       def extract[X:FromExpr](name:String, x:Expr[X])(using Quotes): X =
        import quotes.reflect._
        summon[FromExpr[X]].unapply(x).getOrElse( throw FromExprException(s"can't parse ${name}: ${x.show}, tree: ${x.asTerm}", x) )  

       def unapply(x: Expr[CodecMakerConfig])(using Quotes):Option[CodecMakerConfig] =
        import quotes.reflect._
        x match 
          case '{ new CodecMakerConfig(
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
                        $exprUseScalaEnumValueId
                  ) 
                }  => 
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
                      extract("useScalaEnumValueId", exprUseScalaEnumValueId)
                    ))
                  } catch {
                    case FromExprException(message, expr) => 
                      report.warning(message, expr)
                      None
                  }
          case '{ (${x}:CodecMakerConfig).withAllowRecursiveTypes($v) } =>
            val vv = v.valueOrAbort
            val vx = x.valueOrAbort
            Some(vx.withAllowRecursiveTypes(vv))
          case '{ CodecMakerConfig } =>
            Some(CodecMakerConfig)
          case other =>
            report.error(s"Can't interpret ${other.show} as constant expression, tree=$other")
            None
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
  inline def make[A]: JsonValueCodec[A] = ${
     Impl.makeWithDefaultConfig[A]
  }

  /**
   * A replacement for the `make` call with the `CodecMakerConfig.withDiscriminatorFieldName(None)` configuration
   * parameter.
   *
   * @tparam A a type that should be encoded and decoded by the derived codec
   * @return an instance of the derived codec
   */
  inline def makeWithoutDiscriminator[A]: JsonValueCodec[A] = ${
     Impl.makeWithoutDiscriminator[A]
  }

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true)`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFields[A]: JsonValueCodec[A] = ${
    Impl.makeWithRequiredCollectionFields[A]
  } 

  /**
    * A replacement for the `make` call with the
    * `CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name"))`
    * configuration parameter.
    *
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]: JsonValueCodec[A] = ${
    Impl.makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A]
  } 

  /**
    * Derives a codec for JSON values for the specified type `A` and a provided derivation configuration.
    *
    * @param config a derivation configuration
    * @tparam A a type that should be encoded and decoded by the derived codec
    * @return an instance of the derived codec
    */
  inline def make[A](inline config: CodecMakerConfig): JsonValueCodec[A] = ${
    Impl.makeWithSpecifiedConfig[A]('config)
  }

  object Impl {
    def makeWithDefaultConfig[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig)

    def makeWithoutDiscriminator[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withDiscriminatorFieldName(None))

    def makeWithRequiredCollectionFields[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true))

    def makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[A: Type](using Quotes): Expr[JsonValueCodec[A]] =
      tryMake(CodecMakerConfig.withTransientEmpty(false).withRequireCollectionFields(true).withDiscriminatorFieldName(Some("name")))

    def makeWithSpecifiedConfig[A: Type](config: Expr[CodecMakerConfig])(using Quotes): Expr[JsonValueCodec[A]] =
      import quotes.reflect.*
      tryMake[A] {
        summon[FromExpr[CodecMakerConfig]].unapply(config) match
          case None =>
            report.errorAndAbort(s"Cannot evaluate a parameter of the 'make' macro call for type '${Type.show[A]}'. ")
          case Some(cfg) =>
            if (cfg.requireCollectionFields && cfg.transientEmpty) {
              report.throwError("'requireCollectionFields' and 'transientEmpty' cannot be 'true' simultaneously")
            }
            cfg
      }

    private[this] def tryMake[A: Type](cfg: CodecMakerConfig)(using Quotes): Expr[JsonValueCodec[A]] = {
      try {
        make[A](cfg)
      } catch {
        case ex: scala.quoted.runtime.StopMacroExpansion =>
          throw ex
        case NonFatal(ex) =>
          println(s"catched exception during macro expansion: $ex: msg=${ex.getMessage}")
          ex.printStackTrace()
          throw ex
      }
    }


    private[this] def make[A: Type](cfg: CodecMakerConfig)(using Quotes): Expr[JsonValueCodec[A]] = {
      import quotes.reflect._
    
      val traceFlag: Boolean = true  // TODO: add to confg 

      def fail(msg: String): Nothing = report.throwError(msg, Position.ofMacroExpansion )

      def warn(msg: String): Unit = report.warning(msg, Position.ofMacroExpansion )

      // TODO: test and use instead of typeArg
      def typeArg1Of[T[_]:Type](tpe: TypeRepr): TypeRepr = {
            tpe.baseType(TypeRepr.of[T].typeSymbol) match {
                case AppliedType(base, args) =>
                  args.head
                case _ =>
                  fail(s"type ${tpe.show} not having baseType of ${TypeRepr.of[T].show}")  
            }
      }

      def typeArg1(tpe: TypeRepr): TypeRepr = 
        tpe match
          case AppliedType(base, args) => args.head.dealias
          case _ =>
            fail(s"expected that ${tpe} is an AppliedType")

      def typeArg2(tpe: TypeRepr): TypeRepr =
        tpe match
          case AppliedType(base, args) =>
            if (args.length > 1) {
              args(1).dealias
            } else 
              fail(s"expected that ${tpe} have at least two arguments")
          case _ =>
            fail(s"expected that ${tpe} is an AppliedType")

      def areEqual(tpe1: TypeRepr, tpe2: TypeRepr): Boolean = tpe1 =:= tpe2

      //val tupleSymbols: Set[Symbol] = definitions.TupleClass.seq.toSet

      def isTuple(tpe: TypeRepr): Boolean = (tpe <:< TypeRepr.of[Tuple])

      def isValueClass(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[AnyVal]

      def valueClassValue(tpe: TypeRepr): Symbol = 
         tpe.typeSymbol.fieldMembers(0)

      def decodeName(s: Symbol): String = s.name

      
      /*
      not in 3.1.0
      def substituteTypes(tpe: TypeRepr, from: List[Symbol], to: List[TypeRepr]): TypeRepr =
        try tpe.substituteTypes(from, to) catch { case NonFatal(_) =>
          fail(s"Cannot resolve generic type(s) for `$tpe`. Please provide a custom implicitly accessible codec for it.")
        }
      */  
          

      /*
      def resolveConcreteType(tpe: TypeRepr, mtpe: TypeRepr): TypeRepr = {    
        val tpeTypeParams = tpe match
          case AppliedType(main, params) => params
          case _ => Nil
        if (tpeTypeParams.isEmpty) mtpe
        else substituteTypes(mtpe, tpeTypeParams, tpe.typeArgs)
      }
      */
      

      //def methodType(tpe: TypeRepr, m: Symbol): TypeRepr =
        //resolveConcreteType(tpe, m.returnType.dealias)

      //def paramType(tpe: TypeRepr, p: Symbol): TypeRepr = ??? // resolveConcreteType(tpe, p.typeSignature.dealias)

      def valueClassValueType(tpe: TypeRepr): TypeRepr = 
        try {
          val field = tpe.typeSymbol.fieldMembers(0)
          tpe.memberType(field)
        }catch{
          case NonFatal(ex) =>
            println(s"error getting valueType for ${tpe.show}")
            println(s"tpe.typeSymbol.fieldMembers = ${tpe.typeSymbol.fieldMembers}")
            throw ex
        }


      def isNonAbstractScalaClass(tpe: TypeRepr): Boolean =
        tpe.classSymbol match
          case None => false
          case Some(sym) =>
              val flags = sym.flags
              !flags.is(Flags.Abstract) && !flags.is(Flags.JavaDefined)

      def isSealedClass(tpe: TypeRepr): Boolean = 
        tpe.typeSymbol.flags.is(Flags.Sealed)
  
      def isConstType(tpe: TypeRepr): Boolean = tpe match {
        case ConstantType(_) => true
        case _ => false
      }


      def getEnclosingClass(sym: Symbol): Symbol =
        if (sym.isClassDef) sym else getEnclosingClass(sym.owner)

    
      // TODO: explorr collection adtLeafClasses via mirror.

      val enclosingClassTpe = getEnclosingClass(Symbol.spliceOwner).tree match
        case cl: ClassDef => cl.constructor.returnTpt.tpe
        case _ => fail("")

      def adtLeafClasses(adtBaseTpe: TypeRepr): Seq[TypeRepr] = {

        def collectRecursively(symbol: Symbol): Seq[TypeRepr] = {
          val leafTpes = if (symbol.flags.is(Flags.Sealed)) {
                               symbol.children.flatMap{ s =>
                                   collectRecursively(s)
                               }
                            }  else Seq()
          if (symbol.isType) {
            val flags = symbol.flags 
            if (!flags.is(Flags.Abstract) && !flags.is(Flags.JavaDefined) && symbol.isClassDef) {
                // tpe used for 'as seen from'
                //  mb - use class 'this'or package
                //
                // not work
                //val tpe = adtBaseTpe.memberType(symbol)
                // use antipattern
                val tpe = symbol.tree match
                  case tpt: TypeTree => tpt.tpe
                  case cl: ClassDef => 
                    val x = cl.self match
                      case Some(clSelf) => clSelf.tpt.tpe
                      case None => fail(s"Can't find self time for classdef ${symbol}")
                    x
                     
                  case _ => // todo: 
                    enclosingClassTpe.memberType(symbol)
                leafTpes :+ tpe
            } else {
                if (flags.is(Flags.Abstract)) {
                    fail(
                      "Only sealed intermediate traits or abstract classes are supported. Please consider using of them\n" +
                      s"for ADT with base '$adtBaseTpe' or provide a custom implicitly accessible codec for the ADT base.\n" +
                      s"failed symbol: $symbol\n"    
                    )
                } else {
                    fail(
                       "Only Scala classes & objects are supported for ADT leaf classes. Please consider using of them\n" +
                      s"for ADT with base '$adtBaseTpe' or provide a custom implicitly accessible codec for the ADT base.\n" +
                      s"failed symbol: $symbol\n" 
                    )
                }
              }
          } else {
            fail(s"expectd that ${symbol} is a type")
          }
        }  
               
        val classes = collectRecursively(adtBaseTpe.typeSymbol).distinct
        if (classes.isEmpty) fail(s"Cannot find leaf classes for ADT base '$adtBaseTpe'. " +
          "Please add them or provide a custom implicitly accessible codec for the ADT base.")
        classes
      }

      def companion(tpe: TypeRepr): Symbol = {
        tpe.typeSymbol.moduleClass
      }

      def isOption(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[Option[_]]

      def isCollection(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[Iterable[_]] || tpe <:< TypeRepr.of[Array[_]]

      def scalaCollectionCompanion(tpe: TypeRepr): Term =
        if (tpe.typeSymbol.fullName.startsWith("scala.collection.")) Ref(tpe.typeSymbol.companionModule)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")

 
      def scala2EnumerationObject(tpe: TypeRepr): Expr[scala.Enumeration] = tpe match {
        case TypeRef(ct, name) =>
          if (ct.isSingleton) {
            Ref(ct.termSymbol).asExprOf[scala.Enumeration]
          } else {
            fail(s"for scala2enum type reference to singleton term is expected, we have ${tpe}")
          }
        case _ =>
          fail(s"for scala2enum type reference is expected, we have ${tpe}")
      }

      def findScala2EnumerationById[C <: AnyRef: Type](tpe: TypeRepr, i: Expr[Int]): Expr[Option[C]] =
        '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.id == $i) }.asExprOf[Option[C]]

      def findScala2EnumerationByName[C <: AnyRef : Type](tpe: TypeRepr, name: Expr[String]): Expr[Option[C]] =
        '{ ${scala2EnumerationObject(tpe)}.values.iterator.find(_.toString == $name) }.asExprOf[Option[C]]
  

      val rootTpe = TypeRepr.of[A]
      // TODO: not sure that types are normalized. 
      val inferredKeyCodecs: mutable.Map[TypeRepr, Option[Expr[JsonKeyCodec[?]]] ] = mutable.Map.empty
      val inferredValueCodecs: mutable.Map[TypeRepr, Option[Expr[JsonValueCodec[?]]]] = mutable.Map.empty


      def inferImplicitValue(typeToSearch: TypeRepr): Option[Term] = 
        Implicits.search(typeToSearch) match
          case v: ImplicitSearchSuccess =>
            Some(v.tree)
          case f: ImplicitSearchFailure =>
            if (traceFlag) {
               report.info(s"failed implicit search for ${typeToSearch.show}: ${f.explanation}")
            }
            None

      def checkRecursionInTypes(types: List[TypeRepr]): Unit = {
        if (!cfg.allowRecursiveTypes) {
           val tpe::nested = types
           val recursiveIdx = nested.indexOf(tpe)
           if (recursiveIdx >=0 ) {
              fail(s"Recursive type(s) detected: ${nested.take(recursiveIdx + 1).reverse.mkString("'", "', '", "'")}. " +
              "Please consider using a custom implicitly accessible codec for this type to control the level of " +
              s"recursion or turn on the '${Type.show[CodecMakerConfig]}.allowRecursiveTypes' for the trusted input that " +
              "will not exceed the thread stack size.")
           }
        }
      }
    
      def findImplicitKeyCodec(types: List[TypeRepr]): Option[Expr[JsonKeyCodec[?]]] = {
        val tpe :: nestedTypes = types 
        if (nestedTypes.isEmpty) {
          // TODO: Unclear why, ask
          None
        } else if (areEqual(tpe, rootTpe)) {
          None
        } else {
          // Todo: unclear
          //if (nestedTypes.isEmpty) None
          checkRecursionInTypes(types)  
          inferredKeyCodecs.getOrElseUpdate(tpe,{
            val typeToSearch = TypeRepr.of[JsonKeyCodec].appliedTo(tpe)
            inferImplicitValue(typeToSearch).map(_.asExprOf[JsonKeyCodec[?]])
          })    
        }
      }
      
      def findImplicitValueCodec(types: List[TypeRepr]): Option[Expr[JsonValueCodec[?]]] = {
        val tpe :: nestedTypes = types 
        if (nestedTypes.isEmpty) {
          // TODO: Unclear why, ask
          None
        } else if (areEqual(tpe, rootTpe)) {
          // TODO:  Unclear why. ask 
          None
        } else {
          // Todo: unclear
          //if (nestedTypes.isEmpty) None
          checkRecursionInTypes(types)  
          inferredValueCodecs.getOrElseUpdate(tpe, {
            val typeToSearch = TypeRepr.of[JsonValueCodec].appliedTo(tpe)
            inferImplicitValue(typeToSearch).map(_.asExprOf[JsonValueCodec[?]])
          }) 
        }
      }

    
      val mathContexts = mutable.LinkedHashMap.empty[Int, ValDef]
      //val mathContextTrees = mutable.LinkedHashMap.empty[Int, (TermName,Expr[MathContext])]

      def withMathContextFor(precision: Int): Expr[MathContext] =
        if (precision == java.math.MathContext.DECIMAL128.getPrecision) '{ _root_.java.math.MathContext.DECIMAL128 }
        else if (precision == java.math.MathContext.DECIMAL64.getPrecision) '{ _root_.java.math.MathContext.DECIMAL64 }
        else if (precision == java.math.MathContext.DECIMAL32.getPrecision) '{ _root_.java.math.MathContext.DECIMAL32 }
        else if (precision == java.math.MathContext.UNLIMITED.getPrecision) '{ _root_.java.math.MathContext.UNLIMITED }
        else {
          val valDef = mathContexts.getOrElseUpdate( precision, {
            val mc = '{ new java.math.MathContext(${Expr(cfg.bigDecimalPrecision)}, _root_.java.math.RoundingMode.HALF_EVEN) }
            val name = "mc" + mathContexts.size
            val sym = Symbol.newVal(Symbol.spliceOwner, name, TypeRepr.of[MathContext], Flags.EmptyFlags, Symbol.noSymbol)
            ValDef(sym,Some(mc.asTerm.changeOwner(sym)))
          })
          Ref(valDef.symbol).asExprOf[MathContext]
        }
        

      
      //val scalaEnumCacheNames = mutable.LinkedHashMap.empty[TypeRepr, ValDef]
      //val scalaEnumCacheTries = mutable.LinkedHashMap.empty[TypeRepr, Expr[ConcurrentHashMap[Int|String,_]]]
      val scalaEnumCaches = mutable.LinkedHashMap.empty[TypeRepr, ValDef]

      def withScalaEnumCacheFor(tpe: TypeRepr): Term  = {
        val keyTpe = if (cfg.useScalaEnumValueId) TypeRepr.of[Int] else TypeRepr.of[String]
        tpe.asType match
          case '[t] =>
            keyTpe.asType match
              case '[kt] =>
                val valDef = scalaEnumCaches.getOrElseUpdate( tpe, {
                  val ec = '{ new _root_.java.util.concurrent.ConcurrentHashMap[kt, t]  }
                  val name = s"ec${scalaEnumCaches.size}"
                  val sym = Symbol.newVal(Symbol.spliceOwner, name, ec.asTerm.tpe.widen, Flags.EmptyFlags, Symbol.noSymbol)
                  ValDef(sym,Some(ec.asTerm.changeOwner(sym)))
                })
                Ref(valDef.symbol)
              case _ =>
                fail(s"Can't determinate type for ${keyTpe.show}")
          case _ =>
            fail(s"Can't determinate type for ${tpe.show}")
      }
      

      case class EnumValueInfo(value: Symbol, name: String, transformed: Boolean)

      val enumValueInfos = mutable.LinkedHashMap.empty[TypeRepr, Seq[EnumValueInfo]]

      def isJavaEnum(tpe: TypeRepr): Boolean = tpe <:< TypeRepr.of[java.lang.Enum[_]]

    
      def javaEnumValues(tpe: TypeRepr): Seq[EnumValueInfo] = enumValueInfos.getOrElseUpdate(tpe, {
        tpe.classSymbol match
          case Some(classSym) =>
            // known direct subclasses is not known at this type.
            //  TODO: check childs...
            println("javaEnum - loot at declared fields: : "+ classSym.declaredFields)
            var originValues = PlatformJavaEnumHelper.retrieveValues(classSym.fullName)
            // TODO: can be incorrect, run test.
            val values = originValues.map{ name =>
               val sym = classSym.typeMember(name)
               val transformed = cfg.javaEnumValueNameMapper.apply(name) match {
                 case Left((msg,expr)) => fail(msg)
                 case Right(optResult) => optResult.getOrElse(name)
               }
               EnumValueInfo(sym, transformed, name!=transformed)
            }
            if (values.isEmpty) {
              ???
              // TODO: get from companion object
            }
            val nameCollisions = duplicated(values.map(_.name))
            if (nameCollisions.nonEmpty) {
               val formattedCollisions = nameCollisions.mkString("'", "', '", "'")
                fail(s"Duplicated JSON value(s) defined for '$tpe': $formattedCollisions. Values are derived from value " +
                     s"names of the enum that are mapped by the '${Type.show[CodecMakerConfig]}.javaEnumValueNameMapper' function. " +
                     s"Result values should be unique per enum class.")
            }
            values
          case None =>
            fail(s"$tpe is not a class")
      })

      def genReadEnumValue[E:Type](enumValues: Seq[EnumValueInfo], unexpectedEnumValueHandler: Expr[E], in: Expr[JsonReader], l: Expr[Int]): Expr[E] = {
        val hashCode: EnumValueInfo => Int = e => JsonReader.toHashCode(e.name.toCharArray, e.name.length)
        val length: EnumValueInfo => Int = _.name.length

        def genReadCollisions(es: collection.Seq[EnumValueInfo]): Expr[E] = {
          es.foldRight(unexpectedEnumValueHandler) {  (e, acc) =>
            '{ 
              if ($in.isCharBufEqualsTo($l,${Expr(e.name)}))  {
                ${Ref(e.value).asExprOf[E]} 
              } else ${acc} 
            }
          }
        }

        if (enumValues.size <= 8 && enumValues.map(length).sum <= 64) 
          genReadCollisions(enumValues)
        else {
      
          val cases = groupByOrdered(enumValues)(hashCode).map{ case (hash, fs) =>
            val sym = Symbol.newBind(Symbol.spliceOwner, "b"+hash, Flags.EmptyFlags, TypeRepr.of[Int])
            CaseDef(Bind(sym, Expr(hash).asTerm),None, genReadCollisions(fs).asTerm )
          } :+ {
            //val sym = Symbol.newBind(Symbol.spliceOwner, "nofFound", Flags.EmptyFlags, TypeRepr.of[E])
            CaseDef(Wildcard(), None, unexpectedEnumValueHandler.asTerm)
          }
          
          Match('{ ${in}.charBufToHashCode($l) }.asTerm, cases.toList ).asExprOf[E]
        }
      }

      
      def genReadKey[T:Type](types: List[TypeRepr], in: Expr[JsonReader]): Expr[T] = {
        val tpe = types.head
        val implKeyCodec = findImplicitKeyCodec(types)
        if (!implKeyCodec.isEmpty)  '{ ${implKeyCodec.get}.decodeKey($in) }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Boolean] || tpe =:= TypeRepr.of[java.lang.Boolean])  '{ ${in}.readKeyAsBoolean() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Byte] || tpe =:= TypeRepr.of[java.lang.Byte])  '{ $in.readKeyAsByte() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Char] || tpe =:= TypeRepr.of[java.lang.Character]) '{ $in.readKeyAsChar() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Short] || tpe =:= TypeRepr.of[java.lang.Short]) '{ $in.readKeyAsShort() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Int] || tpe =:= TypeRepr.of[java.lang.Integer]) '{ $in.readKeyAsInt() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Long] || tpe =:= TypeRepr.of[java.lang.Long]) '{ $in.readKeyAsLong() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Float] || tpe =:= TypeRepr.of[java.lang.Float]) '{ $in.readKeyAsFloat() }.asExprOf[T]
        else if (tpe =:= TypeRepr.of[Double] || tpe =:= TypeRepr.of[java.lang.Double]) '{ $in.readKeyAsDouble() }.asExprOf[T]
        else if (isValueClass(tpe)) {
                val newObjInit = Select.unique(New(Inferred(tpe)),"<init>") 
                Apply(newObjInit,List( genReadKey(valueClassValueType(tpe)::types, in).asTerm)).asExprOf[T]
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
          val ec = withScalaEnumCacheFor(tpe)
          if (cfg.useScalaEnumValueId) {
            val ecTyped = {ec.asExprOf[ConcurrentHashMap[Int,Enumeration#Value]]} 
            '{
              val i = ${in}.readKeyAsInt()
              var x = ${ecTyped}.get(i)
              if (x eq null) {
                x = ${scala2EnumerationObject(tpe).asExprOf[Enumeration]}.values.iterator.find(_.id == i).getOrElse($in.enumValueError(i.toString))
                ${ecTyped}.put(i, x)
              }
              x
            }.asExprOf[T]
          } else {
            val ecTyped = {ec.asExprOf[ConcurrentHashMap[String,Enumeration#Value]]}
            '{
              val s = $in.readKeyAsString()
              var x = $ecTyped.get(s)
              if (x eq null) {
                  x = ${scala2EnumerationObject(tpe).asExprOf[Enumeration]}.values.iterator.find(_.toString == s).getOrElse($in.enumValueError(s.length))
                  $ecTyped.put(s, x)
              }
              x
            }.asExprOf[T]
          }
        } else if (isJavaEnum(tpe)) {
          '{
            val l = ${in}.readKeyAsCharBuf()
            ${genReadEnumValue(javaEnumValues(tpe), '{ $in.enumValueError(l) }, in, 'l)}
          }.asExprOf[T]
        } else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(StringConstant(v)) =>
              '{ if ($in.readKeyAsString() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(BooleanConstant(v)) =>
              '{ if ($in.readKeyAsBoolean() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(ByteConstant(v)) =>
              '{ if ($in.readKeyAsByte() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(CharConstant(v)) =>
              '{ if ($in.readKeyAsChar() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(ShortConstant(v)) =>
              '{ if ($in.readKeyAsShort() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(IntConstant(v)) =>
              '{ if ($in.readKeyAsInt() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(LongConstant(v)) =>
              '{ if ($in.readKeyAsLong() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(FloatConstant(v)) =>
              '{ if ($in.readKeyAsFloat() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case ConstantType(DoubleConstant(v)) =>
              '{ if ($in.readKeyAsDouble() != ${Expr(v)} ) $in.decodeError("expected key: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asExprOf[T]
            case _ => cannotFindKeyCodecError(tpe)
          }
        } else cannotFindKeyCodecError(tpe)
      }

      // type parameter here trigger dotty bug: see https://github.com/lampepfl/dotty/issues/14123
      sealed trait UpdateOp
      case class Assignment(value: Term) extends UpdateOp
      case class Update(operation: Expr[Unit]) extends UpdateOp
      case class ConditionalAssignmentAndUpdate(cond: Expr[Boolean], assignment: Term, update: Expr[Unit]) extends UpdateOp
      
      

      def genReadArray[B:Type,C:Type](newBuilder: Expr[B], readVal: (Expr[B], Expr[Int]) => UpdateOp, default: Expr[C], result: (Expr[B], Expr[Int]) => Expr[C], in: Expr[JsonReader]): Expr[C] =
          '{ if ($in.isNextToken('[')) {
              if ($in.isNextToken(']')) $default
              else {
                $in.rollbackToken()
                var x = $newBuilder
                var i = 0
                while ({
                  ${readVal('x,'i) match {
                    case Assignment(value) => '{ x = ${value.asExprOf[B]} }
                    case Update(operation) => operation
                    case ConditionalAssignmentAndUpdate(cond, value, operation) =>
                      '{
                        if ($cond) x=${value.asExprOf[B]}
                        $operation
                      }
                  }}
                  i += 1
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken(']')) ${result('x,'i)}
                else $in.arrayEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '[')
          }


      def genReadSet[B:Type,C:Type](newBuilder: Expr[B], readVal: Quotes ?=> Expr[B] => UpdateOp, default: Expr[C], result: Expr[B] => Expr[C],  in: Expr[JsonReader]): Expr[C] =
        '{  if ($in.isNextToken('[')) {
              if ($in.isNextToken(']')) $default
              else {
                $in.rollbackToken()
                var x = $newBuilder
                var i = 0
                while ({
                  ${readVal('x) match
                    case Assignment(value) =>
                      '{ x = ${value.asExprOf[B]} }
                    case Update(operation) =>
                      operation
                    case ConditionalAssignmentAndUpdate(cond, value, operation) =>
                      '{  if ($cond) x=${value.asExprOf[B]}; $operation }
                  }
                  i += 1
                  if (i > ${Expr(cfg.setMaxInsertNumber)}) $in.decodeError("too many set inserts")
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken(']')) ${result('x)}
                else $in.arrayEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '[')
        }.asExprOf[C]

      def genReadMap[B:Type,C:Type](newBuilder: Expr[B], readKV: Expr[B]=> UpdateOp, result: Expr[B]=>Expr[C], in: Expr[JsonReader], default: Expr[C]): Expr[C] =
        '{  if ($in.isNextToken('{')) {
              if ($in.isNextToken('}')) $default
              else {
                $in.rollbackToken()
                var x = $newBuilder
                var i = 0
                while ({
                  ${readKV('x) match
                      case Assignment(value) => '{ x = ${value.asExprOf[B]} }
                      case Update(op) => op
                      case ConditionalAssignmentAndUpdate(cond, value, update) =>
                        '{ 
                          if ($cond) {
                             x = ${value.asExprOf[B]}
                          }
                          $update
                         }
                  }
                  i += 1
                  if (i > ${Expr(cfg.mapMaxInsertNumber)}) $in.decodeError("too many map inserts")
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken('}')) ${result('x)}
                else $in.objectEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '{')
        }

      def genReadMapAsArray[B:Type,C:Type](newBuilder: Expr[B], 
                                readKV: Expr[B] => UpdateOp, 
                                result: Expr[B] => Expr[C], 
                                in: Expr[JsonReader],
                                default: Expr[C]): Expr[C] =
        '{ if ($in.isNextToken('[')) {
              if ($in.isNextToken(']')) $default
              else {
                $in.rollbackToken()
                var b = $newBuilder
                var i = 0
                while ({
                  if ($in.isNextToken('[')) {
                    ${readKV('b) match
                      case Assignment(value) => '{ b = ${value.asExprOf[B]} }
                      case Update(op) => op
                      case ConditionalAssignmentAndUpdate(cond, value, update) =>
                        '{
                          if ($cond) b=${value.asExprOf[B]}
                          $update
                        } 
                    }
                    i += 1
                    if (i > ${Expr(cfg.mapMaxInsertNumber)}) $in.decodeError("too many map inserts")
                    if (!$in.isNextToken(']')) $in.arrayEndError()
                  } else $in.readNullOrTokenError($default, '[')
                  $in.isNextToken(',')
                }) ()
                if ($in.isCurrentToken(']')) ${result('b)}
                else $in.objectEndOrCommaError()
              }
            } else $in.readNullOrTokenError($default, '[')
        }


      @tailrec
      def genWriteKey[A:Type](x: Expr[A], types: List[TypeRepr], out: Expr[JsonWriter]): Expr[Unit] = {
        val tpe = types.head.widen
        val implKeyCodec = findImplicitKeyCodec(types)
        if (!implKeyCodec.isEmpty) '{ ${implKeyCodec.get.asExprOf[JsonKeyCodec[A]]}.encodeKey($x, $out) }
        else if (isValueClass(tpe)) {
          genWriteKey(  Select.unique( x.asTerm, valueClassValue(tpe).name).asExprOf[A], 
                       valueClassValueType(tpe) :: types,
                       out)
        } else if (tpe =:= TypeRepr.of[Boolean]) {
          '{ $out.writeKey(${x.asExprOf[Boolean]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Boolean]}) }
        } else if (tpe =:= TypeRepr.of[Byte]) {
          '{ $out.writeKey(${x.asExprOf[Byte]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Byte]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Byte]}) }
        } else if (tpe =:= TypeRepr.of[Char]) {
          '{ $out.writeKey(${x.asExprOf[Char]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Character]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Character]}) }
        } else if (tpe =:= TypeRepr.of[Short]) {
          '{ $out.writeKey(${x.asExprOf[Short]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Short]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Short]}) }
        } else if (tpe =:= TypeRepr.of[Int]) {
          '{ $out.writeKey(${x.asExprOf[Int]}) }
        } else if (tpe =:= TypeRepr.of[Integer]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Integer]}) }
        } else if (tpe =:= TypeRepr.of[Long]) {
          '{ $out.writeKey(${x.asExprOf[Long]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Long] ) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Long]}) }
        } else if (tpe =:= TypeRepr.of[Float]) {
          '{ $out.writeKey(${x.asExprOf[Float]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Float]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Float]}) }
        } else if (tpe =:= TypeRepr.of[Double]) {
          '{ $out.writeKey(${x.asExprOf[Double]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Double]) {
          '{ $out.writeKey(${x.asExprOf[java.lang.Double]}) }
        } else if (tpe =:= TypeRepr.of[String]) {
          '{ $out.writeKey(${x.asExprOf[String]}) }
        } else if (tpe =:= TypeRepr.of[BigInt]) {
          '{ $out.writeKey(${x.asExprOf[BigInt]}) }
        } else if (tpe =:= TypeRepr.of[BigDecimal]) {
          '{ $out.writeKey(${x.asExprOf[BigDecimal]}) }
        } else if (tpe =:= TypeRepr.of[java.util.UUID]) {
          '{ $out.writeKey(${x.asExprOf[java.util.UUID]}) }
        } else if (tpe =:= TypeRepr.of[Duration]) {
          '{ $out.writeKey(${x.asExprOf[Duration]}) }
        } else if (tpe =:= TypeRepr.of[Instant]) {
          '{ $out.writeKey(${x.asExprOf[Instant]}) }
        } else if (tpe =:= TypeRepr.of[LocalDate]) {
          '{ $out.writeKey(${x.asExprOf[LocalDate]}) }
        } else if (tpe =:= TypeRepr.of[LocalDateTime]) {
          '{ $out.writeKey(${x.asExprOf[LocalDateTime]}) }
        } else if (tpe =:= TypeRepr.of[LocalTime]) {
          '{ $out.writeKey(${x.asExprOf[LocalTime]}) }
        } else if (tpe =:= TypeRepr.of[MonthDay]) {
          '{ $out.writeKey(${x.asExprOf[MonthDay]}) }
        } else if (tpe =:= TypeRepr.of[OffsetDateTime]) {
          '{ $out.writeKey(${x.asExprOf[OffsetDateTime]}) }
        } else if (tpe =:= TypeRepr.of[OffsetTime]) {
          '{ $out.writeKey(${x.asExprOf[OffsetTime]}) }
        } else if (tpe =:= TypeRepr.of[Period]) {
          '{ $out.writeKey(${x.asExprOf[Period]}) }
        } else if (tpe =:= TypeRepr.of[Year]) {
          '{ $out.writeKey(${x.asExprOf[Year]}) }
        } else if (tpe =:= TypeRepr.of[YearMonth]) {
          '{ $out.writeKey(${x.asExprOf[YearMonth]}) }
        } else if (tpe =:= TypeRepr.of[ZonedDateTime]) {
          '{ $out.writeKey(${x.asExprOf[ZonedDateTime]}) }
        } else if (tpe =:= TypeRepr.of[ZoneId]) {
          '{ $out.writeKey(${x.asExprOf[ZoneId]}) }
        } else if (tpe =:= TypeRepr.of[ZoneOffset]) {
           '{ $out.writeKey(${x.asExprOf[ZoneOffset]}) }
        } else if (tpe <:< TypeRepr.of[Enumeration#Value]) {
          if (cfg.useScalaEnumValueId) '{ $out.writeKey(${x.asExprOf[Enumeration#Value]}.id) }
          else '{ $out.writeKey($x.toString) }
        } else if (isJavaEnum(tpe)) {
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e => CaseDef(Ref(e.value),None,Literal(StringConstant(e.name))) ) :+
                        CaseDef(Wildcard(), None, '{ $out.encodeError("illegal enum value: " + $x) }.asTerm )
            val matchTerm = Match(x.asTerm, cases.toList )
            if (encodingRequired) '{ $out.writeKey(${matchTerm.asExprOf[String]}) }
            else '{ $out.writeNonEscapedAsciiKey(${matchTerm.asExprOf[String]}) }
          } else {
            val nameExpr = Select.unique(x.asTerm, "name").asExprOf[String]
            if (encodingRequired) '{ $out.writeKey(${nameExpr}) }
            else '{ $out.writeNonEscapedAsciiKey(${nameExpr})  }
          }
        } else if (isConstType(tpe)) {
          tpe match {
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
          }
        } else cannotFindKeyCodecError(tpe)
      }

      def genWriteConstantKey(name: String, out: Expr[JsonWriter]): Expr[Unit] =
        if (isEncodingRequired(name)) '{ $out.writeKey(${Expr(name)}) }
        else '{ $out.writeNonEscapedAsciiKey(${Expr(name)}) }

      def genWriteConstantVal(value: String, out: Expr[JsonWriter]): Expr[Unit] =
        if (isEncodingRequired(value)) '{ $out.writeVal(${Expr(value)}) }
        else '{ $out.writeNonEscapedAsciiVal(${Expr(value)}) }

      def genWriteArray(x: Expr[Iterable[?]], writeVal: (Expr[JsonWriter], Term) => Expr[Unit], out: Expr[JsonWriter]): Expr[Unit] =
        '{ $out.writeArrayStart()
            $x.foreach { x =>
              ${writeVal(out, 'x.asTerm)}
            }
            $out.writeArrayEnd()
        }

      /*  
      def genWriteMap(out: Expr[JsonWriter], x: Expr[collection.Map[?,?]], 
                      writeKey: (Expr[JsonWriter], Term) => Expr[Unit] , 
                      writeVal: (Expr[JsonWriter], Term) => Expr[Unit]): Expr[Unit] =
        '{  $out.writeObjectStart()
            $x.foreach { kv =>
              ${writeKey(out,'{ kv._1 }.asTerm)}
              ${writeVal(out, '{ kv._2 }.asTerm)}
            }
            $out.writeObjectEnd()
        }

      def genWriteMapAsArray(out:Expr[JsonWriter], x: Expr[Map[?,?]], 
                             writeKey: (Expr[JsonWriter], Term) => Expr[Unit], 
                             writeVal: (Expr[JsonWriter], Term) => Expr[Unit]): Expr[Unit] =
        '{  $out.writeArrayStart()
            $x.foreach { kv =>
              $out.writeArrayStart()
              ${writeKey(out, '{kv._1}.asTerm)}
              ${writeVal(out, '{kv._2}.asTerm)}
              $out.writeArrayEnd()
            }
            $out.writeArrayEnd()
        }
      */

      def genWriteMapScala213(x: Expr[collection.Map[?,?]], 
                              writeKey: (Expr[JsonWriter], Term) => Expr[Unit], 
                              writeVal: (Expr[JsonWriter], Term) => Expr[Unit],
                              out: Expr[JsonWriter]
                              ): Expr[Unit] =
        '{  $out.writeObjectStart()
            $x.foreachEntry { (k, v) =>
              ${writeKey(out, 'k.asTerm)}
              ${writeVal(out, 'v.asTerm)}
            }
            $out.writeObjectEnd()
        }

      def genWriteMapAsArrayScala213(x: Expr[collection.Map[?,?]], 
                              writeKey: (Expr[JsonWriter], Term) => Expr[Unit], 
                              writeVal: (Expr[JsonWriter], Term) => Expr[Unit],
                              out: Expr[JsonWriter] 
                              ): Expr[Unit] = 
        '{  $out.writeArrayStart()
            $x.foreachEntry { (k, v) =>
              $out.writeArrayStart()
              ${writeKey(out, 'k.asTerm)}
              ${writeVal(out, 'v.asTerm)}
              $out.writeArrayEnd()
            }
            $out.writeArrayEnd()
        }

      def cannotFindKeyCodecError(tpe: TypeRepr): Nothing =
        fail(s"No implicit '${TypeRepr.of[JsonKeyCodec[_]].show}' defined for '${tpe.show}'.")

      def cannotFindValueCodecError(tpe: TypeRepr): Nothing =
        fail(if (tpe.typeSymbol.flags.is(Flags.Abstract)) {
          "Only sealed traits or abstract classes are supported as an ADT base. " +
            s"Please consider sealing the '$tpe' or provide a custom implicitly accessible codec for it."
        } else s"No implicit '${TypeRepr.of[JsonValueCodec[_]].show}' defined for '${tpe.show}'.")

      def namedValueOpt(namedAnnotation: Option[Term], tpe: TypeRepr): Option[String] = namedAnnotation.map { a =>
        // TODO: write testcase
        a match {
          case Apply(_, List(param) ) =>
            param match
              case Literal(StringConstant(s)) => s
              case other =>
                // TODO: mini-eval
                fail(s"Cannot evaluate a parameter of the '@named' annotation in type '$tpe'.")
          case _ => 
            fail(s"Invalid named annotation ${a.show}")
        }
      }

      case class FieldInfo(symbol: Symbol, mappedName: String,  field: Symbol,
                           defaultValue: Option[Term], resolvedTpe: TypeRepr, isStringified: Boolean)

      case class ClassInfo(tpe: TypeRepr, fields: Seq[FieldInfo])

      val classInfos = mutable.LinkedHashMap.empty[TypeRepr, ClassInfo]

      def getClassInfo(tpe: TypeRepr): ClassInfo = classInfos.getOrElseUpdate(tpe, {
        case class FieldAnnotations(partiallyMappedName: String, transient: Boolean, stringified: Boolean)

        def getPrimaryConstructor(tpe: TypeRepr): Symbol = 
          tpe.classSymbol match 
            case None => 
              fail(s"Cannot find a primary constructor for '$tpe'")
            case Some(sym) =>
              val retval = sym.primaryConstructor
              if (!retval.exists) {
                fail(s"Cannot find a primary constructor for '$tpe'")
              }
              retval
     
        def hasSupportedAnnotation(m: Symbol): Boolean = {
          m.annotations.exists(a => a.tpe <:< TypeRepr.of[named] || a.tpe <:< TypeRepr.of[transient] ||
            a.tpe <:< TypeRepr.of[stringified])
        }

        val tpeClassSym = tpe.classSymbol.getOrElse(fail(s"expected that ${tpe.show} has classSymbol"))

        //lazy val module = companion(tpe).asModule // don't lookup for the companion when there are no default values for constructor params
        //val getters = tpeClassSym.methodMembers.collect{ case m: Symbol if m.flags.is(Flags.FieldAccessor) && m.paramSymss.isEmpty => m }
        val fields = tpeClassSym.fieldMembers
        if (tpeClassSym.name == "Nested") {
          println("!!tpeClassSym.name==Nested")
          println(s"members = ${tpeClassSym.methodMembers}")
          println(s"fields = ${fields}")
        }
        val annotations = tpeClassSym.fieldMembers.collect {
          case m: Symbol if hasSupportedAnnotation(m) =>
            val name = decodeName(m).trim // FIXME: Why is there a space at the end of field name?!
            val named = m.annotations.filter(_.tpe.widen =:= TypeRepr.of[named])
            if (named.size > 1) fail(s"Duplicated '${TypeRepr.of[named]}' defined for '$name' of '$tpe'.")
            val trans = m.annotations.filter(_.tpe.widen =:= TypeRepr.of[transient])
            if (trans.size > 1) warn(s"Duplicated '${TypeRepr.of[transient]}' defined for '$name' of '$tpe'.")
            val strings = m.annotations.filter(_.tpe.widen =:= TypeRepr.of[stringified])
            if (strings.size > 1) warn(s"Duplicated '${TypeRepr.of[stringified]}' defined for '$name' of '$tpe'.")
            if ((named.nonEmpty || strings.nonEmpty) && trans.size == 1) {
              warn(s"Both '${Type.show[transient]}' and '${Type.show[named]}' or " +
                s"'${Type.show[transient]}' and '${Type.show[stringified]}' defined for '$name' of '$tpe'.")
            }
            val partiallyMappedName = namedValueOpt(named.headOption, tpe).getOrElse(name)
            (name, FieldAnnotations(partiallyMappedName, trans.nonEmpty, strings.nonEmpty))
        }.toMap
        val primaryConstructor = getPrimaryConstructor(tpe)
        ClassInfo(tpe, primaryConstructor.paramSymss match {
          case params :: Nil => params.zipWithIndex.flatMap { case (symbol, i) =>
            //val symbol = p.asTerm
            //val name = decodeName(symbol)
            val name = symbol.name
            val annotationOption = annotations.get(name)
            if (annotationOption.exists(_.transient)) None
            else {
              val mappedName = annotationOption.fold{ 
                cfg.fieldNameMapper(name) match
                  case Left((message, expr)) => fail("Can't interpret fieldNameMapper:"+message)
                  case Right(optValue) => optValue.getOrElse(name)
              }(_.partiallyMappedName)
              //val getter = getters.find(_.name == symbol.name).getOrElse {
              //  println(s"!!!fail (name - parameters: ):getters = $getters, name=${symbol.name}")
              //  fail(s"'$name' parameter of '$tpe' should be defined as 'val' or 'var' in the primary constructor.")
              //}
              val field = fields.find(_.name == symbol.name).getOrElse{
                 fail(s"'$name' parameter of '$tpe' should be defined as 'val' or 'var' in the primary constructor.")
              }
              //???
              //val paramType = tpe.memberType(sym)
              val defaultValue = None  
                // TODO: find way to get default value of the parameter.  
                //  currently this is not possible in scala3: see https://github.com/lampepfl/dotty/discussions/14078
                //  
                //if (symbol.isParamWithDefault) Some(q"$module.${TermName("$lessinit$greater$default$" + (i + 1))}")
                //else None
              val isStringified = annotationOption.exists(_.stringified)
              val fieldType = tpe.memberType(symbol)       // paramType(tpe, symbol) -- ??? 
              Some(FieldInfo(symbol, mappedName, field, defaultValue, fieldType, isStringified))
            }
          }
          case _ => fail(s"'$tpe' has a primary constructor with multiple parameter lists. " +
            "Please consider using a custom implicitly accessible codec for this type.")
            // TODO:  check type-params,
        })
      })

      def unexpectedFieldHandler(in: Expr[JsonReader], l:Expr[Int]): Expr[Unit] =
        if (cfg.skipUnexpectedFields) '{ $in.skip() }
        else '{ $in.unexpectedKeyError($l) }

      def discriminatorValue(tpe: TypeRepr): String = {
        val named = tpe.typeSymbol.annotations.filter(_.tpe =:= TypeRepr.of[named])
        if (named.size > 1) fail(s"Duplicated '${TypeRepr.of[named].show}' defined for '$tpe'.")
        namedValueOpt(named.headOption, tpe)
          .getOrElse{
            cfg.adtLeafClassNameMapper(tpe.typeSymbol.fullName) match
              case Left((message,expr)) => fail("can't appy adtLefClasssNameMapper:"+message)
              case Right(v) => v.getOrElse(fail(s"discriminator is not defined for ${tpe.show}"))
          }
      }

      def checkFieldNameCollisions(tpe: TypeRepr, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON key(s) defined for '$tpe': $formattedCollisions. Keys are derived from field names of " +
            s"the class that are mapped by the '${TypeRepr.of[CodecMakerConfig]}.fieldNameMapper' function or can be overridden " +
            s"by '${TypeRepr.of[named]}' annotation(s). Result keys should be unique and should not match with a key for the " +
            s"discriminator field that is specified by the '${TypeRepr.of[CodecMakerConfig]}.discriminatorFieldName' option.")
        }
      }

      def checkDiscriminatorValueCollisions(tpe: TypeRepr, names: Seq[String]): Unit = {
        val collisions = duplicated(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated discriminator defined for ADT base '$tpe': $formattedCollisions. Values for leaf classes of ADT " +
            s"that are returned by the '${Type.show[CodecMakerConfig]}.adtLeafClassNameMapper' function should be unique.")
        }
      }

      val nullValues = mutable.LinkedHashMap.empty[TypeRepr, ValDef]
      
      def withNullValueFor[C:Type](tpe: TypeRepr)(f: => Expr[C]): Expr[C] = {
        val valDef = nullValues.getOrElseUpdate(tpe, {
            val name = "c" + nullValues.size
            val sym = Symbol.newVal(Symbol.spliceOwner, name, tpe, Flags.EmptyFlags, Symbol.noSymbol)
            ValDef(sym, Some(f.asTerm.changeOwner(sym)))
        })
        Ref(valDef.symbol).asExprOf[C]
      }

      val fieldIndexAccessors = mutable.LinkedHashMap.empty[TypeRepr, DefDef]
    

      def withFieldsByIndexFor(tpe: TypeRepr)(f: => Seq[String]): Term = { 
        // [Int => String], we don't want eta-expand without reason, so let this will be just index.
        val defDef = fieldIndexAccessors.getOrElseUpdate(tpe, {
          val name = "f" + fieldIndexAccessors.size
          val mt = MethodType(List("i"))(_ => List(TypeRepr.of[Int]), _ => TypeRepr.of[String] )
          val sym = Symbol.newMethod(Symbol.spliceOwner, name, mt)
          DefDef(sym, {
             case List(List(param)) =>
              val cases = f.zipWithIndex.map { case (n, i) => CaseDef( Literal(IntConstant(i)), None, Literal(StringConstant(n)) ) }
              val scrutinee = param match
                case paramTerm: Term => paramTerm
                case _ => fail("Exprected that param is term")
                // TODO: annotated iwth scala.annotation.switch and scala.unchecked
              val body = Match(scrutinee, cases.toList).changeOwner(sym)
              Some(body)
          })
        })
        Ref(defDef.symbol)// .asExprOf[Int => String]
      }

      val equalsMethods = mutable.LinkedHashMap.empty[TypeRepr, DefDef]

      def withEqualsFor[A:Type](tpe: TypeRepr, arg1: Expr[A], arg2: Expr[A])(f: (Expr[A], Expr[A]) => Expr[Boolean]): Expr[Boolean] = {
        val defDef = equalsMethods.getOrElseUpdate(tpe, {
          val name = "q" + equalsMethods.size
          val mt = MethodType(List("x1","x2"))(_ => List(tpe,tpe), _ => TypeRepr.of[Boolean])
          val sym = Symbol.newMethod(Symbol.spliceOwner, name, mt)
          DefDef(sym, {
            case List(List(x1,x2)) => Some(f(x1.asExprOf[A],x2.asExprOf[A]).asTerm.changeOwner(sym))
          })
        })
        val refDef = Ref(defDef.symbol)
        Apply(refDef,List(arg1.asTerm,arg2.asTerm)).asExprOf[Boolean]
      }

      def genArrayEquals[A:Type](tpe: TypeRepr, x1t:Expr[A], x2t: Expr[A]): Expr[Boolean] = {
        val tpe1 = tpe.simplified match
                     case AppliedType(tp, param) => param.head
                     case _ =>      
                      fail(s"expectd that ${tpe.show} is AppliedType")
        if (tpe1 <:< TypeRepr.of[Array[_]]) {
          tpe1.asType match
            case '[t1] =>
              val x1 = x1t.asExprOf[Array[t1]]
              val x2 = x2t.asExprOf[Array[t1]]
              def arrEquals(i:Expr[Int]):Expr[Boolean] = withEqualsFor[t1](tpe1, '{ $x1($i) }, '{ $x2($i) })(
                (x1, x2) => genArrayEquals(tpe1, x1, x2)
              )
              '{ ($x1 eq $x2) || (($x1 ne null) && ($x2 ne null) && {
                  val l1 = $x1.length
                  val l2 = $x2.length
                  (l1 == l2) && {
                    var i = 0
                    while (i < l1 && ${arrEquals('i)}) i += 1
                    i == l1
                  }
                })
              }
            case _ => fail(s"Can't determinate type for ${tpe1.show}")
        } else if (tpe1 <:< TypeRepr.of[Boolean]) {
          '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Boolean]]}, ${x2t.asExprOf[Array[Boolean]]} ) }
        } else if (tpe1 <:< TypeRepr.of[Byte]) {
            '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Byte]]}, ${x2t.asExprOf[Array[Byte]]}) }
        } else if (tpe1 <:< TypeRepr.of[AnyRef]){
          '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[AnyRef]]}, ${x2t.asExprOf[Array[AnyRef]]}) }
        } else if (tpe1 <:< TypeRepr.of[Short]){
          '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Short]]}, ${x2t.asExprOf[Array[Short]]}) }
        } else if (tpe1 <:< TypeRepr.of[Int]){
          '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Int]]}, ${x2t.asExprOf[Array[Int]]}) }
        } else if (tpe1 <:< TypeRepr.of[Float]){
          '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Float]]}, ${x2t.asExprOf[Array[Float]]}) }
        } else if (tpe1 <:< TypeRepr.of[Double]){
            '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Double]]}, ${x2t.asExprOf[Array[Double]]}) }
        } else if (tpe1 <:< TypeRepr.of[Char]){
              '{ _root_.java.util.Arrays.equals(${x1t.asExprOf[Array[Char]]}, ${x2t.asExprOf[Array[Char]]}) }
        } else {
          fail(s"Can't generate array of typr ${tpe1.show} -- unknon non-reference psrt")
        }
      }

      case class DecoderMethodKey(tpe: TypeRepr, isStringified: Boolean, useDiscriminator: Boolean)
      
      val decodeMethodDefs = mutable.LinkedHashMap.empty[DecoderMethodKey,  DefDef]
      
      def withDecoderFor[A:Type](methodKey: DecoderMethodKey, arg: Expr[A], in: Expr[JsonReader])(f: (Expr[JsonReader], Expr[A]) => Expr[A]): Expr[A] = {
        val defDef = decodeMethodDefs.getOrElseUpdate( methodKey, {
           val name = "d" + decodeMethodDefs.size
           val mt = MethodType(List("in","defaultValue"))(
             _ => List(TypeRepr.of[JsonReader], methodKey.tpe),
             _ => TypeRepr.of[A]
           )
           val sym = Symbol.newMethod(Symbol.spliceOwner, name, mt)
           DefDef(sym, {
             case List(List(in,default)) =>
                default match
                  case defaultTerm: Term =>
                    Some(f(in.asExprOf[JsonReader],defaultTerm.asExprOf[A]).asTerm.changeOwner(sym))
                  case _ =>
                    fail(s"expected that ${default} is term")
           })
        })
        val refDef = Ref(defDef.symbol)
        try {
          Apply(refDef,List(in.asTerm,arg.asTerm)).asExprOf[A]
        }catch{
          case NonFatal(ex) =>
            println(s"can't set decoder for ${TypeRepr.of[A].show}")
            throw ex;
        }
      }

      case class WriteDiscriminator(fieldName: String, fieldValue: String) {

          def write(out: Expr[JsonWriter]): Expr[Unit] = '{
             ${genWriteConstantKey(fieldName,out)}
             ${genWriteConstantVal(fieldValue,out)}
          }

      }
      
      case class EncoderMethodKey(tpe: TypeRepr, isStringified: Boolean, discriminatorKeyValue: Option[(String,String)])
      

      val encodeMethodDefs = mutable.LinkedHashMap.empty[EncoderMethodKey, DefDef]
      
      def withEncoderFor(methodKey: EncoderMethodKey,  arg: Term, out: Expr[JsonWriter])(f: (Expr[JsonWriter], Term)=> Expr[Unit]): Expr[Unit] = {
        val defDef = encodeMethodDefs.getOrElseUpdate(methodKey, {
          val name = "e" + encodeMethodDefs.size
          val mt = MethodType(List("x","out"))(
            _ => List(methodKey.tpe, TypeRepr.of[JsonWriter]),
            _ => TypeRepr.of[Unit]
          )
          val sym = Symbol.newMethod(Symbol.spliceOwner, name, mt)
          DefDef(sym, {
            case List(List(x,out)) =>
              x match
                case xTerm: Term => 
                  Some(f(out.asExprOf[JsonWriter],xTerm).asTerm.changeOwner(sym))
                case _ =>
                  fail(s"expected that ${x} is term")
          })
        })
        val refDef = Ref(defDef.symbol)
        Apply(refDef, List(arg, out.asTerm)).asExprOf[Unit]
      }

    

      def genNullValue[C:Type](types: List[TypeRepr]): Expr[C] = {
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        if (!implCodec.isEmpty) '{ ${implCodec.get}.nullValue }.asExprOf[C]  
        else if (tpe =:= TypeRepr.of[Boolean] || tpe =:= TypeRepr.of[java.lang.Boolean]) Literal(BooleanConstant(false)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Byte] || tpe =:= TypeRepr.of[java.lang.Byte]) Literal(ByteConstant(0)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Char] || tpe =:= TypeRepr.of[java.lang.Character]) '{ '\u0000' }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Short] || tpe =:= TypeRepr.of[java.lang.Short]) Literal(ShortConstant(0)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Int] || tpe =:= TypeRepr.of[java.lang.Integer]) Literal(IntConstant(0)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Long] || tpe =:= TypeRepr.of[java.lang.Long]) Literal(LongConstant(0)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Float] || tpe =:= TypeRepr.of[java.lang.Float]) Literal(FloatConstant(0f)).asExprOf[C]
        else if (tpe =:= TypeRepr.of[Double] || tpe =:= TypeRepr.of[java.lang.Double]) Literal(DoubleConstant(0d)).asExprOf[C]
        else if (isOption(tpe)) '{ None }.asExprOf[C]
        else if (tpe <:< TypeRepr.of[mutable.BitSet]) '{ mutable.BitSet.empty }.asExprOf[C]
        else if (tpe <:< TypeRepr.of[BitSet]) withNullValueFor(tpe)( '{ BitSet.empty }.asExprOf[C] )
        else if (tpe <:< TypeRepr.of[mutable.LongMap[_]]) {
            typeArg1(tpe).asType match
                case '[targ] =>
                  '{ mutable.LongMap.empty[targ] }.asExprOf[C]
                case _ =>
                  fail(s"Can't determinate type for ${tpe.show}")
        } else if (tpe <:< TypeRepr.of[::[_]]) '{ null }.asExprOf[C]
        else if (tpe <:<  TypeRepr.of[List[_]] || tpe =:=  TypeRepr.of[Seq[_]]) '{ Nil }.asExprOf[C]
        else if (tpe <:<  TypeRepr.of[immutable.IntMap[_]] || tpe <:<  TypeRepr.of[immutable.LongMap[_]] ||
          tpe <:<  TypeRepr.of[immutable.Seq[_]] || tpe <:<  TypeRepr.of[Set[_]]) withNullValueFor[C](tpe) {
            TypeApply(Select.unique(scalaCollectionCompanion(tpe),"empty"),List(Inferred(typeArg1(tpe)))).asExprOf[C]
        } else if (tpe <:< TypeRepr.of[immutable.Map[_, _]]) withNullValueFor(tpe) {
            TypeApply(Select.unique(scalaCollectionCompanion(tpe),"empty"),
                      List(Inferred(typeArg1(tpe)), Inferred(typeArg2(tpe)))).asExprOf[C]
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) {
            TypeApply(Select.unique(scalaCollectionCompanion(tpe),"empty"),
                      List(Inferred(typeArg1(tpe)),Inferred(typeArg2(tpe)))).asExprOf[C]
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) 
            TypeApply(Select.unique(scalaCollectionCompanion(tpe),"empty"),List(Inferred(typeArg1(tpe)))).asExprOf[C]
        else if (tpe <:< TypeRepr.of[Array[_]]) 
             typeArg1(tpe).asType match
               case '[t1] => 
                  Expr.summon[ClassTag[t1]] match
                    case Some(ct1) =>
                      withNullValueFor(tpe)('{ Array.empty[t1](using $ct1) }.asExprOf[C] )
                    case _ => fail(s"No classtag found from ${Type.show[t1]}")
               case other =>
                  fail(s"Can't determinate type for ${other}") 
        else if (tpe.isSingleton) 
             Ref(tpe.termSymbol).asExprOf[C]
        else if (tpe <:< TypeRepr.of[AnyRef]) '{ null }.asExprOf[C]
        else if (isConstType(tpe)) {
          tpe match {
            case ConstantType(StringConstant(v)) => Literal(StringConstant(v)).asExprOf[C] 
            case ConstantType(BooleanConstant(v)) => Literal(BooleanConstant(v)).asExprOf[C]
            case ConstantType(ByteConstant(v)) => Literal(ByteConstant(v)).asExprOf[C]
            case ConstantType(CharConstant(v)) => Literal(CharConstant(v)).asExprOf[C]
            case ConstantType(ShortConstant(v)) => Literal(ShortConstant(v)).asExprOf[C]
            case ConstantType(IntConstant(v)) => Literal(IntConstant(v)).asExprOf[C]
            case ConstantType(LongConstant(v)) => Literal(LongConstant(v)).asExprOf[C]
            case ConstantType(FloatConstant(v)) => Literal(FloatConstant(v)).asExprOf[C]
            case ConstantType(DoubleConstant(v)) => Literal(DoubleConstant(v)).asExprOf[C]
            case _ => cannotFindValueCodecError(tpe)
          }
        } else if (tpe =:= TypeRepr.of[Unit]) {
          '{ () }.asExprOf[C]
        } else '{ null }.asExprOf[C]
      }

      case class ReadDiscriminator(valDef: ValDef) {

        def skip(in: Expr[JsonReader], l: Expr[Int]): Expr[Unit] =
            val pd = Ref(valDef.symbol).asExprOf[Boolean]
            '{  if ($pd) {
                  ${Assign(Ref(valDef.symbol), Literal(BooleanConstant(false))).asExprOf[Unit]}
                  $in.skip()
                } else $in.duplicatedKeyError($l)
            }
    
      }

     
      def genReadNonAbstractScalaClass[A:Type](types: List[TypeRepr], 
                  discriminator: Option[ReadDiscriminator],
                  in: Expr[JsonReader],
                  default: Expr[A],
                  ): Expr[A] = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        checkFieldNameCollisions(tpe, cfg.discriminatorFieldName.fold(Seq.empty[String]) { n =>
          val names = classInfo.fields.map(_.mappedName)
          if (discriminator.isEmpty) names
          else names :+ n
        })
        val required: Set[String] = classInfo.fields.collect {
          case f if !(f.symbol.flags.is(Flags.HasDefault) || isOption(f.resolvedTpe) ||
            (isCollection(f.resolvedTpe) && !cfg.requireCollectionFields)) => f.mappedName
        }.toSet
        val paramVarNum = classInfo.fields.size
        val lastParamVarIndex = Math.max(0, (paramVarNum - 1) >> 5)
        val lastParamVarBits = -1 >>> -paramVarNum

       
        val paramVars = (0 to lastParamVarIndex).map{i => 
          val name = "p" + i;
          val sym = Symbol.newVal(Symbol.spliceOwner, name, TypeRepr.of[Int], Flags.Mutable, Symbol.noSymbol)
          val rhs = Literal(IntConstant(if (i == lastParamVarIndex) lastParamVarBits else -1))
          ValDef(sym, Some(rhs))
        } 
        val checkAndResetFieldPresenceFlags:Map[String, Expr[Int] => Expr[Unit]] = {
          classInfo.fields.zipWithIndex.map { case (f, i) =>
            val n = Ref(paramVars(i >> 5).symbol).asExprOf[Int]
            val m = Expr(1 << i)
            (f.mappedName, (l:Expr[Int]) => '{ 
              if (($n & $m) != 0) 
                ${Assign(n.asTerm , '{ $n ^ $m }.asTerm).asExprOf[Unit]} 
              else 
                $in.duplicatedKeyError($l) 
            } )
          }.toMap
        }
         
        val checkReqVars =
          if (required.isEmpty) Nil
          else {
            val nameByIndex = withFieldsByIndexFor(tpe)(classInfo.fields.map(_.mappedName))
            val reqMasks = classInfo.fields.grouped(32).toSeq.map(_.zipWithIndex.foldLeft(0) { case (acc, (f, i)) =>
              if (required(f.mappedName)) acc | (1 << i)
              else acc
            })
            paramVars.zipWithIndex.map { case (nValDef, i) =>
              val n = Ref(nValDef.symbol).asExprOf[Int]
              val m = Expr(reqMasks(i))
              val fieldName = 
                if (i == 0)  
                    Apply(nameByIndex,List( '{java.lang.Integer.numberOfTrailingZeros($n & $m) }.asTerm) ) 
                else 
                    Apply(nameByIndex,List( '{ java.lang.Integer.numberOfTrailingZeros($n & $m) + ${Expr(i << 5)} }.asTerm ))
              '{  if ( ($n & $m) != 0) $in.requiredFieldError(${fieldName.asExprOf[String]}) }
            }.toList
          }
        val readVars: Seq[ValDef] = classInfo.fields.map { f =>
            val name = "_"+f.symbol.name
            val sym = Symbol.newVal(Symbol.spliceOwner, name, f.resolvedTpe, Flags.Mutable, Symbol.noSymbol)
            f.resolvedTpe.asType match
              case '[ft] =>  
                ValDef(sym, Some(f.defaultValue.getOrElse(genNullValue[ft](f.resolvedTpe::types).asTerm)))
              case _ =>
                fail(s"Can't determinate type for ${f.resolvedTpe}")
        }
        val readVarsMap = (classInfo.fields zip readVars).map{ case (field, tmpVar) =>
            (field.symbol.name, tmpVar)
        }.toMap

        val construct = Apply(Select.unique(New(Inferred(tpe)),"<init>"),
                              classInfo.fields.zipWithIndex.map{ case (f,i) => Ref(readVars(i).symbol)}.toList
                        )
        //val construct = q"new $tpe(..${classInfo.fields.map(f => q"${f.symbol.name} = ${f.tmpName}")})"
        //val readVars = classInfo.fields.map { f =>
        //  q"var ${f.tmpName}: ${f.resolvedTpe} = ${f.defaultValue.getOrElse(nullValue(f.resolvedTpe :: types))}"
        //}
        val fiHashCode: (FieldInfo => Int) = f => JsonReader.toHashCode(f.mappedName.toCharArray, f.mappedName.length)
        val fiLength: (FieldInfo => Int) = _.mappedName.length
        val readFields = cfg.discriminatorFieldName.fold(classInfo.fields) { n =>
          if (discriminator.isEmpty) classInfo.fields
          else classInfo.fields :+ FieldInfo(Symbol.noSymbol, n, Symbol.noSymbol, None, TypeRepr.of[String], isStringified = true)
        }

        def genReadCollisions(fs: collection.Seq[FieldInfo], tmpVars: Map[String,ValDef], l:Expr[Int])(using Quotes): Expr[Unit] =
          val s0: Expr[Unit] = unexpectedFieldHandler(in,l) 
          fs.foldRight(s0){ (f, acc) =>
            val readValue: Expr[Unit] =
              if (discriminator.nonEmpty && cfg.discriminatorFieldName.contains(f.mappedName)) {
                discriminator.get.skip.apply(in, l)
              } else {
                f.resolvedTpe.asType match
                  case '[ft] =>
                    val tmpVar = Ref(tmpVars(f.symbol.name).symbol)
                    Block(
                      List(checkAndResetFieldPresenceFlags(f.mappedName)(l).asTerm),
                      Assign(
                        tmpVar, genReadVal[ft](f.resolvedTpe :: types, tmpVar.asExprOf[ft], f.isStringified,  None, in).asTerm
                      )
                    ).asExprOf[Unit]
                  case _ => fail(s"Can't get type for ${f.resolvedTpe}")
              }
             '{ if ($in.isCharBufEqualsTo($l, ${Expr(f.mappedName)})) $readValue else $acc }
          }

        def readFieldsBlock(l: Expr[Int])(using Quotes):Expr[Unit] =
          //  using Quotes for workarround agains https://github.com/lampepfl/dotty/issues/14137
          if (readFields.size <= 8 && readFields.map(fiLength).sum <= 64) 
            genReadCollisions(readFields, readVarsMap, l)
          else {
            val cases = groupByOrdered(readFields)(fiHashCode).map { case (hash, fs) =>
              //cq"$hash => ${genReadCollisions(fs)}"
              CaseDef(Literal(IntConstant(hash)),None, genReadCollisions(fs, readVarsMap, l).asTerm)
            } :+
              CaseDef(Wildcard(), None, unexpectedFieldHandler(in,l).asTerm)  
            val scrutinee = '{ $in.charBufToHashCode($l): @scala.annotation.switch }.asTerm 
            Match(scrutinee, cases.toList ).asExprOf[Unit]  
          }

        
        val optDiscriminatorVar = discriminator.map{ _.valDef }
                   

        def blockWithVars(next: Term): Term =
          Block(
            readVars.toList ++
            paramVars.toList ++
            optDiscriminatorVar.toList,
            next
          )

        '{  if ($in.isNextToken('{')) {
             ${blockWithVars(
              '{if (!$in.isNextToken('}')) {
                    $in.rollbackToken()
                    var l = -1
                    while (l < 0 || $in.isNextToken(',')) {
                        l = $in.readKeyAsCharBuf()
                        ${readFieldsBlock('l)}
                    }
                    if (!$in.isCurrentToken('}')) $in.objectEndOrCommaError()
                }
                ${Block(checkReqVars.map(_.asTerm), construct).asExprOf[A]}
              }.asTerm).asExprOf[A]
             }
            } else $in.readNullOrTokenError($default, '{')
        }

      }

      def genReadConstType(tpe: TypeRepr,  isStringified: Boolean, in: Expr[JsonReader]): Term = tpe match {
        case ConstantType(StringConstant(v)) =>
          '{ if ($in.readString(null) != ${Expr(v)}) $in.decodeError("expected value: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asTerm
        case ConstantType(BooleanConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsBoolean() != ${Expr(v)}) $in.decodeError("expected value: \"" + ${Expr(v)} + '"'); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readBoolean() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(ByteConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsByte() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readByte() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(CharConstant(v)) =>
          '{ if ($in.readChar() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
        case ConstantType(ShortConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsShort() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readShort() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(IntConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsInt() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readInt() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(LongConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsLong() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readLong() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(FloatConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsFloat() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readFloat() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case ConstantType(DoubleConstant(v)) =>
          if (isStringified) 
            '{ if ($in.readStringAsDouble() != ${Expr(v)}) $in.decodeError(${Expr("expected value: \"" + v + '"')}); ${Expr(v)} }.asTerm
          else 
            '{ if ($in.readDouble() != ${Expr(v)}) $in.decodeError(${Expr("expected value: " + v)}); ${Expr(v)} }.asTerm
        case _ => cannotFindValueCodecError(tpe)
      }

      def genReadValForGrowable[G<:Growable[V]:Type,V:Type](types: List[TypeRepr], isStringified: Boolean, x: Expr[G], in: Expr[JsonReader])(using Quotes): Expr[Unit] =
          '{ $x.addOne(${genReadVal[V](types, genNullValue[V](types), isStringified, None, in)}) }

      def genArraysCopyOf[T:Type](tpe: TypeRepr, x:Expr[Array[T]], newLen:Expr[Int]): Expr[Array[T]] = {
        if (tpe <:< TypeRepr.of[Boolean]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Boolean]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Byte]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Byte]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Short]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Short]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Int]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Int]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Long]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Long]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Char]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Char]]}, $newLen) }.asExprOf[Array[T]]
        } else if (tpe <:< TypeRepr.of[Float]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Float]]}, $newLen) }.asExprOf[Array[T]]
        }else if (tpe <:< TypeRepr.of[Double]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[Double]]}, $newLen) }.asExprOf[Array[T]]
        }else if (tpe <:< TypeRepr.of[AnyRef]) {
          '{ java.util.Arrays.copyOf(${x.asExprOf[Array[AnyRef & T]]}, $newLen) }.asExprOf[Array[T]]
        }else {
          fail(s"Can;t find Arrays.copyOf for ${tpe.show}")
        }
      }    

      def genReadVal[C:Type](types: List[TypeRepr], 
                     default: Expr[C], 
                     isStringified: Boolean, 
                     optDiscriminator: Option[ReadDiscriminator],
                     in: Expr[JsonReader]
                     ): Expr[C] = {
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        val methodKey = DecoderMethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe)), optDiscriminator.isDefined)
        val decodeMethodDef = decodeMethodDefs.get(methodKey)
        if (!implCodec.isEmpty) '{ ${implCodec.get.asExprOf[JsonValueCodec[C]]}.decodeValue($in, $default) }
        else if (decodeMethodDef.isDefined) 
          val decodeRef = Ref(decodeMethodDef.get.symbol) 
          Apply( decodeRef, List(in.asTerm, default.asTerm) ).asExprOf[C]   
        else if (tpe =:= TypeRepr.of[Boolean] || tpe =:= TypeRepr.of[java.lang.Boolean]) {
          if (isStringified) '{ $in.readStringAsBoolean() }.asExprOf[C]
          else '{ $in.readBoolean() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Byte] || tpe =:= TypeRepr.of[java.lang.Byte]) {
          if (isStringified) '{ $in.readStringAsByte() }.asExprOf[C]
          else '{ $in.readByte() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Char] || tpe =:= TypeRepr.of[java.lang.Character]) 
          '{ $in.readChar() }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Short] || tpe =:= TypeRepr.of[java.lang.Short]) {
          if (isStringified) '{ $in.readStringAsShort() }.asExprOf[C]
          else '{ $in.readShort() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Int] || tpe =:= TypeRepr.of[java.lang.Integer]) {
          if (isStringified) '{ $in.readStringAsInt() }.asExprOf[C]
          else '{ $in.readInt() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Long] || tpe =:= TypeRepr.of[java.lang.Long]) {
          if (isStringified) '{ $in.readStringAsLong() }.asExprOf[C]
          else '{ $in.readLong() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Float] || tpe =:= TypeRepr.of[java.lang.Float]) {
          if (isStringified) '{ $in.readStringAsFloat() }.asExprOf[C]
          else '{ $in.readFloat() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[Double]|| tpe =:= TypeRepr.of[java.lang.Double]) {
          if (isStringified) '{ $in.readStringAsDouble() }.asExprOf[C]
          else '{ $in.readDouble() }.asExprOf[C]
        } else if (tpe =:= TypeRepr.of[String]) '{ $in.readString(${default.asExprOf[String]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[java.util.UUID]) '{ $in.readUUID(${default.asExprOf[java.util.UUID]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Duration]) '{ $in.readDuration(${default.asExprOf[Duration]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Instant]) '{ $in.readInstant(${default.asExprOf[Instant]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[LocalDate]) '{ $in.readLocalDate(${default.asExprOf[LocalDate]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[LocalDateTime]) '{ $in.readLocalDateTime(${default.asExprOf[LocalDateTime]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[LocalTime]) '{ $in.readLocalTime(${default.asExprOf[LocalTime]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[MonthDay]) '{ $in.readMonthDay(${default.asExprOf[MonthDay]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[OffsetDateTime]) '{ $in.readOffsetDateTime(${default.asExprOf[OffsetDateTime]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[OffsetTime]) '{ $in.readOffsetTime(${default.asExprOf[OffsetTime]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Period]) '{ $in.readPeriod(${default.asExprOf[Period]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[Year]) '{ $in.readYear(${default.asExprOf[Year]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[YearMonth]) '{ $in.readYearMonth(${default.asExprOf[YearMonth]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[ZonedDateTime]) '{ $in.readZonedDateTime(${default.asExprOf[ZonedDateTime]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[ZoneId]) '{ $in.readZoneId(${default.asExprOf[ZoneId]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[ZoneOffset]) '{ $in.readZoneOffset(${default.asExprOf[ZoneOffset]}) }.asExprOf[C]
        else if (tpe =:= TypeRepr.of[BigInt]) {
          if (isStringified) 
            '{ $in.readStringAsBigInt(${default.asExprOf[BigInt]}, ${Expr(cfg.bigIntDigitsLimit)}) }.asExprOf[C]
          else 
            '{ $in.readBigInt(${default.asExprOf[BigInt]}, ${Expr(cfg.bigIntDigitsLimit)}) }.asExprOf[C]
        } else if (tpe =:=  TypeRepr.of[BigDecimal]) {
          val mc = withMathContextFor(cfg.bigDecimalPrecision)
          if (isStringified) {
              '{ $in.readStringAsBigDecimal(${default.asExprOf[BigDecimal]}, $mc, 
                    ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)}) 
              }.asExprOf[C]
          } else {
              '{ $in.readBigDecimal(${default.asExprOf[BigDecimal]}, $mc, 
                    ${Expr(cfg.bigDecimalScaleLimit)}, ${Expr(cfg.bigDecimalDigitsLimit)}) 
              }.asExprOf[C]
          }
        } else if (tpe =:= TypeRepr.of[Unit]) {
          fail("Unit can't be read")
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          tpe1.asType match
            case '[t1] =>
              val readVal = genReadVal[t1](tpe1::types, genNullValue[t1](tpe1::types), isStringified, None, in)
              Apply(Select.unique(New(Inferred(tpe)),"<init>"),List(readVal.asTerm)).asExprOf[C]
            case _ =>
              fail(s"can't determinate type for ${tpe1.show}.")
        } else if (isOption(tpe)) {
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              '{ if ($in.isNextToken('n')) $in.readNullOrError($default, "expected value or null")
                else {
                  $in.rollbackToken()
                  Some(${genReadVal[t1](tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, None, in)})
                }
              }.asExprOf[C]
            case _ => fail(s"can't determinate type for ${tpe1.show}.")
        } else if (tpe <:<  TypeRepr.of[immutable.IntMap[_]])  {
          withDecoderFor(methodKey, default, in) { (in, default) =>
            val tpe1 = typeArg1(tpe)
            tpe1.asType match
              case '[t1] =>
                val newBuilder = withNullValueFor[immutable.IntMap[t1]](tpe)( 
                  TypeApply(
                     Select.unique(scalaCollectionCompanion(tpe),"empty"),
                     List(Inferred(tpe1))
                  ).asExprOf[immutable.IntMap[t1]]
                )
                val readVal = genReadVal[t1](tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, None, in)
                if (cfg.mapAsArray) {
                    val readKey =
                      if (cfg.isStringified) '{ $in.readStringAsInt() }
                      else '{ $in.readInt() }
                    genReadMapAsArray[immutable.IntMap[t1],C](newBuilder, 
                          x => Assignment('{ 
                           $x.updated($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() }) 
                          }.asTerm),
                          x => x.asExprOf[C],
                          in,
                          default.asExprOf[C]
                    )
                } else genReadMap[immutable.IntMap[t1],immutable.IntMap[t1]](newBuilder.asExprOf[immutable.IntMap[t1]], 
                           x => Assignment('{ $x.updated($in.readKeyAsInt(), $readVal) }.asTerm),
                           identity,
                           in,
                           default.asExprOf[immutable.IntMap[t1]]
                        ).asExprOf[C]
              case _ => fail(s"can't determinate type for ${tpe1.show}.")
          }
        } else if (tpe <:< TypeRepr.of[mutable.LongMap[_]]) withDecoderFor[C](methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asExprOf[mutable.LongMap[t1]]
              val emptyMap = TypeApply(
                                   Select.unique(scalaCollectionCompanion(tpe),"empty"),
                                   List(Inferred(tpe1))
              ).asExprOf[mutable.LongMap[t1]]
              val newBuilder = '{ if ($tDefault.isEmpty) $tDefault else ${emptyMap} }
              val readVal = genReadVal(tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, None, in)
              if (cfg.mapAsArray) {
                val readKey =
                    if (cfg.isStringified) '{ $in.readStringAsLong() }
                    else '{ $in.readLong() }
                genReadMapAsArray[mutable.LongMap[t1],mutable.LongMap[t1]](newBuilder,
                  (x) => Update('{ $x.update($readKey, { if ($in.isNextToken(',')) $readVal else $in.commaError() }) }),
                  identity,
                  in,
                  default.asExprOf[mutable.LongMap[t1]]
                ).asExprOf[C]
              } else {
                genReadMap[mutable.LongMap[t1],mutable.LongMap[t1]](newBuilder, 
                    x => Update( '{ $x.update($in.readKeyAsLong(), $readVal) } ),
                    identity,
                    in,
                    default.asExprOf[mutable.LongMap[t1]]
                ).asExprOf[C]
              }
            case _ => fail(s"can't determinate type for ${tpe1.show}.")
        } else if (tpe <:< TypeRepr.of[immutable.LongMap[_]]) withDecoderFor[C](methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val newBuilder = withNullValueFor[immutable.LongMap[t1]](tpe)(
                 TypeApply(
                   Select.unique(scalaCollectionCompanion(tpe),"empty"),
                   List(Inferred(tpe1))
                 ).asExprOf[immutable.LongMap[t1]]
              )  
              val readVal = genReadVal[t1](tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, None, in)
              if (cfg.mapAsArray) {
                  val readKey =
                    if (cfg.isStringified) '{ $in.readStringAsLong() }
                    else '{ $in.readLong() }
                  genReadMapAsArray[immutable.LongMap[t1],immutable.LongMap[t1]](newBuilder,
                      x => Assignment('{ $x.updated($readKey, 
                                          { if ($in.isNextToken(',')) $readVal else $in.commaError() }) 
                          }.asTerm),
                      x => x,
                      in,
                      default.asExprOf[immutable.LongMap[t1]] 
                  ).asExprOf[C]
              } else {
                  genReadMap(newBuilder, 
                     x => Assignment('{ $x.updated($in.readKeyAsLong(), $readVal) }.asTerm ),
                     identity,
                     in,
                     default.asExprOf[immutable.LongMap[t1]]
                  ).asExprOf[C]
              }
        } else if (tpe <:< TypeRepr.of[mutable.Map[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          (tpe1.asType, tpe2.asType) match
            case ('[t1], '[t2]) =>
              val tDefault = default.asExprOf[mutable.Map[t1,t2]]
              val emptyMap = TypeApply(
                               Select.unique(scalaCollectionCompanion(tpe),"empty"),
                               List(TypeTree.of[t1], TypeTree.of[t2])
              ).asExprOf[mutable.Map[t1,t2]]  
              val newBuilder = '{ if ($tDefault.isEmpty) $tDefault else $emptyMap }.asExprOf[mutable.Map[t1,t2]]
              val readVal2 = genReadVal[t2](tpe2 :: types, genNullValue[t2](tpe2 :: types), isStringified, None, in)
              if (cfg.mapAsArray) {
                val readVal1 = genReadVal[t1](tpe1 :: types, genNullValue[t1](tpe1 :: types), isStringified, None, in)
                genReadMapAsArray[mutable.Map[t1,t2],mutable.Map[t1,t2]](newBuilder, 
                    x => Update('{ $x.update($readVal1, { if ($in.isNextToken(',')) $readVal2 else $in.commaError() }) }),
                    identity,
                    in,
                    tDefault
                ).asExprOf[C]
              } else 
                genReadMap[mutable.Map[t1,t2],mutable.Map[t1,t2]](newBuilder, 
                    x => Update('{ $x.update(${genReadKey[t1](tpe1 :: types, in)}, $readVal2) }),
                    identity,
                    in,
                    tDefault
                ).asExprOf[C]
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          tpe1.widen.asType match
            case '[t1] =>
              tpe2.widen.asType match 
                case '[t2] =>
                  val newBuilder = TypeApply(
                                      Select.unique(scalaCollectionCompanion(tpe), "newBuilder"),
                                      List(TypeTree.of[t1],TypeTree.of[t2])
                                   ).asExprOf[mutable.Builder[(t1,t2),collection.Map[t1,t2]]]
                  val readVal2 = genReadVal[t2](tpe2 :: types, genNullValue(tpe2 :: types).asExprOf[t2], isStringified, None, in)
                  if (cfg.mapAsArray) {
                      val readVal1 = genReadVal[t1](tpe1 :: types, genNullValue(tpe1 :: types).asExprOf[t1], isStringified, None, in)
                      val readKV = (x:Expr[mutable.Builder[(t1,t2),collection.Map[t1,t2]]]) => 
                                        Update('{ $x.addOne(($readVal1, 
                                                                { if ($in.isNextToken(',')) $readVal2 else $in.commaError() }))
                                        })
                      genReadMapAsArray(newBuilder, readKV, (b) => '{ $b.result() }, in, default).asExprOf[C]
                  } else {
                      val readKey = genReadKey[t1](tpe1 :: types, in)
                      val readKV = (x: Expr[mutable.Builder[(t1,t2),collection.Map[t1,t2]]]) => 
                               Update('{ $x.addOne(($readKey, $readVal2)) })
                      genReadMap[mutable.Builder[(t1,t2),collection.Map[t1,t2]],collection.Map[t1,t2]](newBuilder, 
                            readKV, (b) => '{ $b.result() }, 
                            in, default.asExprOf[collection.Map[t1,t2]]
                      ).asExprOf[C]                      
                  }
                case _ => fail(s"can't find type for ${tpe2.show}")
            case _ => fail(s"can't find type for ${tpe1.show}")
        } else if (tpe <:< TypeRepr.of[BitSet]) 
           withDecoderFor(methodKey, default, in) { (in, default) =>
              val readVal = if (isStringified) '{ $in.readStringAsInt() }  else '{ $in.readInt() }  
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
                        if (xi >= x.length) {
                          x = _root_.java.util.Arrays.copyOf(x, _root_.java.lang.Integer.highestOneBit(xi) << 1)
                        }
                        x(xi) |= 1L << v
                        $in.isNextToken(',')
                      }) ()
                      if ($in.isCurrentToken(']'))
                         ${
                          if (tpe =:= TypeRepr.of[BitSet] || tpe =:= TypeRepr.of[immutable.BitSet]) 
                            '{ immutable.BitSet.fromBitMaskNoCopy(x) }
                          else
                            '{ mutable.BitSet.fromBitMaskNoCopy(x) }
                         }
                      else $in.arrayEndOrCommaError()
                    }
                  } else $in.readNullOrTokenError($default, '[')
              }.asExprOf[C]
        } else if (tpe <:< TypeRepr.of[mutable.Set[_] with mutable.Builder[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
               val tDefault = default.asExprOf[mutable.Set[t1]]
               val emptySet = TypeApply(Select.unique(scalaCollectionCompanion(tpe),"empty"),List(TypeTree.of[t1])).asExprOf[mutable.Set[t1]]
               genReadSet[mutable.Set[t1], mutable.Set[t1]](
                   '{ if ($tDefault.isEmpty) $tDefault else $emptySet },
                    (x) => Update(genReadValForGrowable[mutable.Set[t1],t1](tpe1 :: types, isStringified, x, in)),
                    tDefault,
                    identity,
                    in
               ).asExprOf[C]
        } else if (tpe <:< TypeRepr.of[collection.Set[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val builder = TypeApply( Select.unique( scalaCollectionCompanion(tpe), "newBuilder" ),
                                       List(TypeTree.of[t1])
              ).asExprOf[mutable.Builder[t1,immutable.Set[t1]]]
              genReadSet[mutable.Builder[t1,immutable.Set[t1]],collection.Set[t1]]( builder,
                      (b) => Update(genReadValForGrowable[mutable.Builder[t1,immutable.Set[t1]], t1](
                                tpe1 :: types, isStringified, b, in)), 
                      default.asExprOf[collection.Set[t1]],
                      (b) => '{ $b.result() },     
                      in
                  ).asExprOf[C]
            case _ => fail(s"can't find type for ${tpe1.show}")
        } else if (tpe <:< TypeRepr.of[::[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match {
            case '[t1] =>
              val tDefault = default.asExprOf[::[t1]]
              val readVal = (x: Expr[mutable.ListBuffer[t1]]) => genReadValForGrowable[mutable.ListBuffer[t1],t1](tpe1 :: types, isStringified, x, in)
              '{  if ($in.isNextToken('[')) {
                    if ($in.isNextToken(']')) {
                      if ($tDefault ne null) $tDefault
                      else $in.decodeError("expected non-empty JSON array")
                    } else {
                      $in.rollbackToken()
                      val x = new _root_.scala.collection.mutable.ListBuffer[t1]
                      while ({
                        ${readVal('x)}
                        $in.isNextToken(',')
                      }) ()
                      if ($in.isCurrentToken(']')) x.toList.asInstanceOf[_root_.scala.collection.immutable.::[t1]]
                      else $in.arrayEndOrCommaError()
                    }
                  } else {
                    if ($tDefault ne null) $in.readNullOrTokenError($tDefault, '[')
                    else $in.decodeError("expected non-empty JSON array")
                  }
              }.asExprOf[C]
            case _ => fail(s"can't determinate type for ${tpe1.show}")
          }
        } else if (tpe <:< TypeRepr.of[List[_]] || tpe =:= TypeRepr.of[Seq[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>       
              genReadArray[mutable.ListBuffer[t1],List[t1]](
                '{ new _root_.scala.collection.mutable.ListBuffer[t1] },
                (x, i) => Update(genReadValForGrowable[mutable.ListBuffer[t1],t1](tpe1 :: types, isStringified, x, in)), 
                default.asExprOf[List[t1]],
                (x, i) => '{ $x.toList },
                in
              ).asExprOf[C]
            case _ => fail(s"can't determinate type for ${tpe1.show}")
        } else if (tpe <:< TypeRepr.of[mutable.Iterable[_] with mutable.Builder[_, _]]) withDecoderFor(methodKey, default, in) { (in, default) => 
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tDefault = default.asExprOf[ C & mutable.Iterable[t1]  ]
              val emptyCollection = TypeApply(
                Select.unique(scalaCollectionCompanion(tpe),"empty"),
                List(Inferred(tpe1))
              ).asExprOf[ C & mutable.Iterable[t1]]
              val builder = '{ if ($tDefault.isEmpty) $tDefault else ${emptyCollection} }.asExprOf[mutable.Iterable[t1] with mutable.Builder[t1,C]]
              genReadArray[mutable.Iterable[t1] with mutable.Builder[t1,C], mutable.Iterable[t1]](builder,
                  (x, i) => Update(genReadValForGrowable[mutable.Iterable[t1] with mutable.Builder[t1, C],t1](tpe1 :: types, isStringified, x, in)),
                  tDefault,
                  (x, i) => x,
                  in
              ).asExprOf[C]
            case _ => fail(s"can't determinate type for ${tpe1.show}")
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match 
            case '[t1] =>
              val builder = TypeApply(
                Select.unique(scalaCollectionCompanion(tpe),"newBuilder"),
                List(Inferred(tpe1))
              ).asExprOf[mutable.Builder[t1,C]]
              genReadArray[mutable.Builder[t1,C],C](builder,
                       (x,i) => Update(genReadValForGrowable(tpe1 :: types, isStringified, x, in)), 
                       default,
                       (x, i) => '{ $x.result() },
                       in
              )
            case _ => fail(s"can't determinate type for ${tpe1.show}")
        } else if (tpe <:< TypeRepr.of[Array[_]]) withDecoderFor(methodKey, default, in) { (in, default) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val newArrayOnChange = tpe1 match {
                case AppliedType(base, args) => true
                case _ => isValueClass(tpe1)
              }
              val t1ClassTag = Expr.summon[ClassTag[t1]] match
                case Some(tag) => tag
                case None => fail(s"Can't find ClassTag for ${tpe.show}")
              def growArray(x: Expr[Array[t1]], i: Expr[Int]): Expr[Array[t1]] =
                  if (newArrayOnChange) {
                    '{ val x1 = new Array[t1]($i << 1)(using $t1ClassTag)
                        _root_.java.lang.System.arraycopy($x, 0, x1, 0, $i)
                        x1
                      }
                  } else
                    genArraysCopyOf[t1](tpe1, x, '{ $i << 1 } )
              def shrinkArray(x: Expr[Array[t1]], i: Expr[Int]): Expr[Array[t1]] =
                  if (newArrayOnChange) {
                    '{  val x1 = new Array[t1]($i)(using $t1ClassTag)
                        _root_.java.lang.System.arraycopy($x, 0, x1, 0, $i)
                        x1 
                    }
                  } else
                    genArraysCopyOf[t1](tpe1, x, i)
              genReadArray( '{new Array[t1](16)(using $t1ClassTag) },
                        (x, i) => 
                            ConditionalAssignmentAndUpdate('{($i == $x.length)},
                                    growArray(x,i).asTerm,
                                    '{ $x($i) = 
                                            ${genReadVal(tpe1 :: types, genNullValue(tpe1 :: types).asExprOf[t1], isStringified, None, in)}
                                    }
                            ),
                        default,
                        (x,i) => '{
                          if ($i == $x.length) $x else ${shrinkArray(x, i)}
                        }.asExprOf[C],
                        in
              )
        } else if (tpe <:< TypeRepr.of[Enumeration#Value]) withDecoderFor(methodKey, default, in) { (in, default) =>
          if (cfg.useScalaEnumValueId) {
            val ec = withScalaEnumCacheFor(tpe).asExprOf[ConcurrentHashMap[Int,C & AnyRef]]
            if (isStringified) {
              '{ if ($in.isNextToken('"')) {
                    $in.rollbackToken()
                    val i = $in.readStringAsInt()
                    var x = $ec.get(i)
                    if (x eq null) {
                      x =  ${findScala2EnumerationById(tpe, 'i)}.getOrElse($in.enumValueError(i.toString))
                      $ec.put(i, x)
                    }
                    x
                  } else $in.readNullOrTokenError($default, '"')
              }.asExprOf[C]
            } else {
              '{  val t = $in.nextToken()
                  if (t >= '0' && t <= '9') {
                    $in.rollbackToken()
                    val i = $in.readInt()
                    var x = $ec.get(i)
                    if (x eq null) {
                      x = ${findScala2EnumerationById(tpe, 'i)}.getOrElse($in.decodeError("illegal enum value " + i))
                      $ec.put(i, x)
                    }
                    x
                  } else $in.readNullOrError($default, "expected digit")
              }
            }
          } else {
            val ec = withScalaEnumCacheFor(tpe).asExprOf[ConcurrentHashMap[String,C]]
            '{  if ($in.isNextToken('"')) {
                  $in.rollbackToken()
                  val s = $in.readString(null)
                  var x = $ec.get(s)
                  if ( ${ 'x.asExprOf[AnyRef] } eq null) {
                    x = ${findScala2EnumerationByName(tpe,'s)}.getOrElse($in.enumValueError(s.length))
                    $ec.put(s, x)
                  }
                  x
                } else $in.readNullOrTokenError($default, '"')
            }
          }
        } else if (isJavaEnum(tpe)) withDecoderFor(methodKey, default, in) { (in, default) => 
            '{  if ($in.isNextToken('"')) {
                   $in.rollbackToken()
                   val l = $in.readStringAsCharBuf()
                   ${genReadEnumValue(javaEnumValues(tpe), '{$in.enumValueError(l)}, in, 'l) }
                } else $in.readNullOrTokenError($default, '"')
            }
        } else if (tpe.typeSymbol.flags.is(Flags.Module) && tpe.isSingleton) withDecoderFor(methodKey, default, in) { (in, default) =>
            '{  if ($in.isNextToken('{')) {
                  $in.rollbackToken()
                  $in.skip()
                  ${Ref(tpe.termSymbol).asExprOf[C]}
                } else $in.readNullOrTokenError($default, '{')
            }
        } else if (isTuple(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          val indexedTypes = tpe match {
            case AppliedType(orig, typeArgs) =>
              typeArgs.zipWithIndex
            case _ =>
              List.empty[(TypeRepr,Int)]
          }
          val valDefs: ArrayBuffer[ValDef] = new ArrayBuffer()
          indexedTypes.foldLeft(valDefs){ (acc, e) =>
            val (te, i) = e
            val name = "_" + (i+1)
            val tp = te.dealias
            tp.asType match
              case '[t] =>
                val sym = Symbol.newVal(Symbol.spliceOwner, name, tp, Flags.EmptyFlags, Symbol.noSymbol)
                val rhs = if (i==0) {
                        genReadVal[t](tp::types, genNullValue[t](tp::types).asExprOf[t], isStringified, None, in).asTerm
                      } else {
                        '{
                          if ($in.isNextToken(',')) {
                            ${genReadVal(tp::types, genNullValue[t](tp::types).asExprOf[t], isStringified, None, in)}
                          } else {
                            $in.commaError()
                          }
                        }.asTerm
                      }
                val valDef = ValDef(sym, Some(rhs))
                valDefs.addOne(valDef)
              case _ =>
                fail(s"Can't match ${tp} as type")
          }
          val readCreateBlock = Block(
            valDefs.toList,
            '{ if ($in.isNextToken(']'))
                  ${Apply(Select.unique(New(Inferred(tpe)),"<init>"),valDefs.map(x=>Ref(x.symbol)).toList).asExpr}
               else
                  $in.arrayEndError()
            }.asTerm  
          ).asExprOf[C]  
          '{ if ($in.isNextToken('[')) {
                $readCreateBlock
              } else $in.readNullOrTokenError($default, '[')
          }.asExprOf[C]
        } else if (isSealedClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) => 
          val hashCode: TypeRepr => Int = t => {
            val cs = discriminatorValue(t).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }
          val length: TypeRepr => Int = t => discriminatorValue(t).length
          val leafClasses = adtLeafClasses(tpe)
          val discriminatorError = cfg.discriminatorFieldName.fold(
                                       '{ $in.discriminatorError() })(n => '{ $in.discriminatorValueError(${Expr(n)}) })

          def genReadLeafClass[S:Type](subTpe: TypeRepr): Expr[S] =
            //val discriminator = cfg.discriminatorFieldName.map( fieldName => Discriminator(fieldName, skipDiscriminatorField))
            val optLeafDiscriminator = cfg.discriminatorFieldName.map{ fieldName =>
              val sym = Symbol.newVal(Symbol.spliceOwner, "pd", TypeRepr.of[Boolean], Flags.Mutable, Symbol.noSymbol)
              val valDef = ValDef(sym,Some(Literal(BooleanConstant(true))))
              ReadDiscriminator(valDef)
            } 
            if (areEqual(subTpe, tpe)) 
                genReadNonAbstractScalaClass(types, optLeafDiscriminator, in, genNullValue[S](subTpe :: types))
            else 
                genReadVal[S](subTpe :: types, genNullValue[S](subTpe :: types), isStringified, optLeafDiscriminator, in)

          def genReadCollisions(subTpes: collection.Seq[TypeRepr], l:Expr[Int]): Term =
            val s0: Term = discriminatorError.asTerm
            subTpes.foldRight(s0) { (subTpe, acc) =>
                try {
                  subTpe.asType match
                    case '[st] =>
                      //println(s"resolved ${subTpe}")
                }catch{
                  case ex: Throwable =>
                    println(s"can't resolve tpe: ${subTpe} "+ Position.ofMacroExpansion)
                    val pos = Position.ofMacroExpansion
                    println(s"souceFile: ${pos.sourceFile}, line: ${pos.startLine}, code: ${pos.sourceCode}")
                    println(s"tpe=${tpe}, show: ${tpe.show}")
                    println(s"tpe.classSymbol=${tpe.classSymbol}")
                    println(s"tpe.typeSymbol.tree=${tpe.typeSymbol.tree.show}")
                    throw ex;
                }
                subTpe.asType match
                  case '[st] =>
                    val readVal: Expr[st] = {
                      if (cfg.discriminatorFieldName.isDefined) {
                        '{ $in.rollbackToMark()
                             ${genReadLeafClass(subTpe)}
                         }
                      } else if (subTpe.typeSymbol.flags.is(Flags.Module) && subTpe.isSingleton) {
                          Ref(subTpe.termSymbol).asExprOf[st]
                      } else genReadLeafClass[st](subTpe)
                    }
                    val r = '{ if ($in.isCharBufEqualsTo($l, ${Expr(discriminatorValue(subTpe))})) $readVal else ${acc.asExprOf[st]} }
                    r.asTerm 
                  case _ => fail(s"Can't extract type for ${subTpe}")
            }

          def genReadSubclassesBlock(leafClasses: collection.Seq[TypeRepr], l:Expr[Int]): Term =
            if (leafClasses.size <= 8 && leafClasses.map(length).sum <= 64) 
              genReadCollisions(leafClasses, l)
            else {
              val cases = groupByOrdered(leafClasses)(hashCode).map { case (hash, ts) =>
                val checkNameAndReadValue = genReadCollisions(ts, l)
                CaseDef(Literal(IntConstant(hash)),None, checkNameAndReadValue)
              }
              val lastCase = CaseDef(Wildcard(),None, discriminatorError.asTerm)
              val scrutinee = '{ $in.charBufToHashCode($l): @scala.annotation.switch }.asTerm
              Match(scrutinee, (cases :+ lastCase).toList)
            }

          checkDiscriminatorValueCollisions(tpe, leafClasses.map(discriminatorValue))
          cfg.discriminatorFieldName match {
            case None =>
              val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(_.typeSymbol.flags.is(Flags.Module))
              if (leafModuleClasses.nonEmpty && leafCaseClasses.nonEmpty) {
                '{
                    if ($in.isNextToken('"')) {
                      $in.rollbackToken()
                      val l = $in.readStringAsCharBuf()
                      ${genReadSubclassesBlock(leafModuleClasses, 'l).asExprOf[C]}
                    } else if ($in.isCurrentToken('{')) {
                      val l = $in.readKeyAsCharBuf()
                      val r = ${genReadSubclassesBlock(leafCaseClasses, 'l).asExprOf[C]}
                      if ($in.isNextToken('}')) r
                      else $in.objectEndOrCommaError()
                    } else $in.readNullOrError($default, "expected '\"' or '{' or null")
                }.asExprOf[C]
              } else if (leafCaseClasses.nonEmpty) {
                '{
                    if ($in.isNextToken('{')) {
                      val l = $in.readKeyAsCharBuf()
                      val r = ${genReadSubclassesBlock(leafCaseClasses, 'l).asExprOf[C]}
                      if ($in.isNextToken('}')) r
                      else $in.objectEndOrCommaError()
                    } else $in.readNullOrTokenError($default, '{')
                }.asExprOf[C]
              } else {
                '{
                    if ($in.isNextToken('"')) {
                      $in.rollbackToken()
                      val l = $in.readStringAsCharBuf()
                      ${genReadSubclassesBlock(leafModuleClasses, 'l).asExprOf[C]}
                    } else $in.readNullOrTokenError($default, '"')
                }
              }
            case Some(discrFieldName) =>
              if (cfg.requireDiscriminatorFirst) {
                '{
                    $in.setMark()
                    if ($in.isNextToken('{')) {
                      if (${Expr(discrFieldName)}.equals($in.readKeyAsString())) {
                        val l = $in.readStringAsCharBuf()
                        ${genReadSubclassesBlock(leafClasses, 'l).asExprOf[C]}
                      } else $in.decodeError("expected key: \"" + ${Expr(discrFieldName)} + '"')
                    } else $in.readNullOrTokenError($default, '{')
                }
              } else {
                '{
                    $in.setMark()
                    if ($in.isNextToken('{')) {
                      if ($in.skipToKey(${Expr(discrFieldName)})) {
                        val l = $in.readStringAsCharBuf()
                        ${genReadSubclassesBlock(leafClasses, 'l).asExprOf[C]}
                      } else $in.requiredFieldError(${Expr(discrFieldName)})
                    } else $in.readNullOrTokenError($default, '{')
                }
              }
          }
        } else if (isNonAbstractScalaClass(tpe)) withDecoderFor(methodKey, default, in) { (in, default) =>
          genReadNonAbstractScalaClass(types, optDiscriminator, in, default)
        } else if (isConstType(tpe)) genReadConstType(tpe, isStringified, in).asExprOf[C]
        else cannotFindValueCodecError(tpe)
      }

   
      def genWriteNonAbstractScalaClass[T](x: Expr[T], types: List[TypeRepr], optDiscriminator: Option[WriteDiscriminator], out: Expr[JsonWriter]): Expr[Unit] = {
        val tpe = types.head
        val classInfo = getClassInfo(tpe)
        val writeFields = classInfo.fields.map { f =>
          val fDefault = if (cfg.transientDefault) f.defaultValue else None
          f.resolvedTpe.asType match {
            case '[ft] =>
              fDefault match {
                case Some(d) =>
                  if (f.resolvedTpe <:< TypeRepr.of[Iterable[?]] && cfg.transientEmpty) {
                    '{  val v = ${ Select(x.asTerm, f.field).asExprOf[Iterable[?]] }
                        if (!v.isEmpty && v != ${d.asExprOf[ft]}) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                    }
                  } else if (isOption(f.resolvedTpe) && cfg.transientNone) {
                    typeArg1(f.resolvedTpe).asType match
                      case '[t1] =>
                        '{ 
                            val v = ${ Select(x.asTerm, f.field).asExprOf[Option[t1]]  }
                            if ((v ne None) && v != ${d.asExprOf[ft]}) {
                                ${genWriteConstantKey(f.mappedName, out)}
                                //val vg = v.get
                                ${genWriteVal( '{v.get} , typeArg1(f.resolvedTpe) :: types, f.isStringified, None, out)}
                            }
                        }
                      case _ =>
                        fail(s"Can't get type agument for ${f.resolvedTpe.show}")
                  } else if (f.resolvedTpe <:< TypeRepr.of[Array[?]]) {
                    def cond(v:Expr[Array[?]]): Expr[Boolean] =
                      val da = d.asExprOf[Array[?]]
                      if (cfg.transientEmpty) {
                        '{ $v.length > 0 && !${withEqualsFor(f.resolvedTpe, v, da)( (x1,x2) => genArrayEquals(f.resolvedTpe, x1, x2))} }
                      } else 
                        '{ !${withEqualsFor(f.resolvedTpe, v, da)( (x1, x2) => genArrayEquals(f.resolvedTpe, x1, x2))} }
                    '{  
                        val v = ${ Select(x.asTerm, f.field).asExprOf[ft & Array[?]] } 
                        if (${cond('v)}) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal( 'v, f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                    }
                  } else {
                    '{
                        val v = ${ Select(x.asTerm, f.field).asExprOf[ft] }
                        if (v != ${d.asExprOf[ft]}) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                    }
                  }
                case None =>
                  if (f.resolvedTpe <:< TypeRepr.of[Iterable[_]] && cfg.transientEmpty) {
                    '{
                        val v = ${ Select(x.asTerm, f.field).asExprOf[ft & Iterable[?]] }
                        if (!v.isEmpty) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                    }
                  } else if (isOption(f.resolvedTpe) && cfg.transientNone) {
                    typeArg1(f.resolvedTpe).asType match
                      case '[tf] =>
                        '{
                            val v = ${ Select(x.asTerm, f.field).asExprOf[Option[tf]] }
                            if (v ne None) {
                              ${genWriteConstantKey(f.mappedName, out)}
                              ${genWriteVal('{ v.get }, typeArg1(f.resolvedTpe) :: types, f.isStringified, None, out)}
                            }
                        }
                      case _ => fail(s"Can't resolve type of ${f.resolvedTpe.show}")
                  } else if (f.resolvedTpe <:< TypeRepr.of[Array[_]] && cfg.transientEmpty) {
                    '{  
                        val v = ${ Select(x.asTerm, f.field).asExprOf[ft & Array[?]] }
                        if (v.length > 0) {
                          ${genWriteConstantKey(f.mappedName, out)}
                          ${genWriteVal('v, f.resolvedTpe :: types, f.isStringified, None, out)}
                        }
                    }
                  } else {
                    val v = Select(x.asTerm, f.field).asExprOf[ft]
                    '{ 
                        ${genWriteConstantKey(f.mappedName, out)}
                        ${genWriteVal( v , f.resolvedTpe :: types, f.isStringified, None, out)}
                    }
                  }
              }
            case _ =>
              fail(s"Can't get typefor ${f.resolvedTpe}")
          }
        }
        val allWriteFields = optDiscriminator match 
          case None => writeFields
          case Some(writeDiscr) =>
            writeDiscr.write(out) +: writeFields

        Block(
          '{ $out.writeObjectStart() }.asTerm :: allWriteFields.toList.map(_.asTerm),
          '{ $out.writeObjectEnd() }.asTerm
        ).asExprOf[Unit]

      }

      def getWriteConstType(tpe: TypeRepr, m: Term, isStringified: Boolean, out: Expr[JsonWriter]): Expr[Unit] = tpe match {
        case ConstantType(StringConstant(_)) => '{ $out.writeVal(${m.asExprOf[String]}) }
        case ConstantType(BooleanConstant(_)) =>
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Boolean]}) }
          else '{ $out.writeVal(${m.asExprOf[Boolean]}) }
        case ConstantType(ByteConstant(_)) =>
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Byte]}) }
          else '{ $out.writeVal(${m.asExprOf[Boolean]})  }
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
      }

 
      def genWriteVal[T:Type](m: Expr[T], types: List[TypeRepr], isStringified: Boolean, optWriteDiscriminator: Option[WriteDiscriminator], out: Expr[JsonWriter]): Expr[Unit]= {
        val tpe = types.head
        val implCodec = findImplicitValueCodec(types)
        val methodKey = EncoderMethodKey(tpe, isStringified && (isCollection(tpe) || isOption(tpe)), optWriteDiscriminator.map(x => (x.fieldName, x.fieldValue)))
        val encodeMethodDef= encodeMethodDefs.get(methodKey)
        if (!implCodec.isEmpty) '{ ${implCodec.get.asExprOf[JsonValueCodec[T]]}.encodeValue($m, $out) }
        else if (encodeMethodDef.isDefined)  {
            val methodRef = Ref(encodeMethodDef.get.symbol)
            Apply(methodRef, List( m.asTerm, out.asTerm)).asExprOf[Unit]
        } else if (tpe <:< TypeRepr.of[Boolean]) {
            if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Boolean]}) }
            else '{ $out.writeVal(${m.asExprOf[Boolean]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Boolean]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[java.lang.Boolean]}) }
          else '{ $out.writeVal(${m.asExprOf[java.lang.Boolean]}) }
        } else if (tpe <:< TypeRepr.of[Byte]|| tpe =:= TypeRepr.of[java.lang.Byte] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Byte]}) }
          else '{ $out.writeVal(${m.asExprOf[Byte]}) }
        } else if ( tpe <:< TypeRepr.of[Short] || tpe =:= TypeRepr.of[java.lang.Short] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Short]}) }
          else '{ $out.writeVal(${m.asExprOf[Short]}) }
        } else if ( tpe <:< TypeRepr.of[Int] || tpe =:= TypeRepr.of[java.lang.Integer] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Int]}) }
          else '{ $out.writeVal(${m.asExprOf[Int]}) }
        } else if ( tpe <:< TypeRepr.of[Long] || tpe =:= TypeRepr.of[java.lang.Long] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Long]}) }
          else '{ $out.writeVal(${m.asExprOf[Long]}) }
        } else if (tpe =:= TypeRepr.of[Float] || tpe =:= TypeRepr.of[java.lang.Float] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Float]}) }
          else '{ $out.writeVal(${m.asExprOf[Float]}) }
        } else if (tpe =:= TypeRepr.of[Double] || tpe =:= TypeRepr.of[java.lang.Double] ) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[Double]}) }
          else '{ $out.writeVal(${m.asExprOf[Double]}) }
        } else if (tpe =:= TypeRepr.of[BigInt]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[BigInt]}) }
          else '{ $out.writeVal(${m.asExprOf[BigInt]})  }
        } else if (tpe =:= TypeRepr.of[BigDecimal]) {
          if (isStringified) '{ $out.writeValAsString(${m.asExprOf[BigDecimal]}) }
          else '{ $out.writeVal(${m.asExprOf[BigDecimal]})  }
        } else if (tpe =:= TypeRepr.of[Char]) {
          '{ $out.writeVal(${m.asExprOf[Char]}) }
        } else if (tpe =:= TypeRepr.of[java.lang.Character]) {
          '{ $out.writeVal(${m.asExprOf[java.lang.Character]}) }
        } else if (tpe =:= TypeRepr.of[String]) {
          '{ $out.writeVal(${m.asExprOf[String]}) }
        } else if (tpe =:= TypeRepr.of[java.util.UUID] ) {
          '{ $out.writeVal(${m.asExprOf[java.util.UUID]}) }
        } else if (tpe =:= TypeRepr.of[Duration]) {
          '{ $out.writeVal(${m.asExprOf[Duration]}) }
        } else if (tpe =:= TypeRepr.of[Instant]) {
          '{ $out.writeVal(${m.asExprOf[Instant]}) }
        } else if (tpe =:= TypeRepr.of[LocalDate] ) {
          '{ $out.writeVal(${m.asExprOf[LocalDate]}) }
        } else if (tpe =:= TypeRepr.of[LocalDateTime] ) {
          '{ $out.writeVal(${m.asExprOf[LocalDateTime]}) }
        } else if (tpe =:= TypeRepr.of[LocalTime] ) {
          '{ $out.writeVal(${m.asExprOf[LocalTime]}) }
        } else if (tpe =:= TypeRepr.of[MonthDay]) {
          '{ $out.writeVal(${m.asExprOf[MonthDay]}) }
        } else if (tpe =:= TypeRepr.of[OffsetDateTime]) {
          '{ $out.writeVal(${m.asExprOf[OffsetDateTime]}) }
        } else if (tpe =:= TypeRepr.of[OffsetTime] ) {
          '{ $out.writeVal(${m.asExprOf[OffsetTime]}) }         
        } else if (tpe =:= TypeRepr.of[Period] ) {
          '{ $out.writeVal(${m.asExprOf[Period]}) } 
        } else if (tpe =:= TypeRepr.of[Year]) {
          '{ $out.writeVal(${m.asExprOf[Year]}) }
        } else if (tpe =:= TypeRepr.of[YearMonth] ) {
          '{ $out.writeVal(${m.asExprOf[YearMonth]}) }
        } else if (tpe =:= TypeRepr.of[ZonedDateTime]) {
          '{ $out.writeVal(${m.asExprOf[ZonedDateTime]}) }
        } else if (tpe =:= TypeRepr.of[ZoneId]) {
          '{ $out.writeVal(${m.asExprOf[ZoneId]}) }
        } else if (tpe =:= TypeRepr.of[ZoneOffset]) {
          '{ $out.writeVal(${m.asExprOf[ZoneOffset]}) }
        } else if (isValueClass(tpe)) {
          genWriteVal( Select(m.asTerm, valueClassValue(tpe)).asExpr, valueClassValueType(tpe) :: types, isStringified, None, out)
        } else if (isOption(tpe)) {
          '{ $m match {
                case Some(x) => ${genWriteVal('x, typeArg1(tpe) :: types, isStringified, None, out)}
                case None => $out.writeNull()
              }
          }
        } else if (tpe <:< TypeRepr.of[immutable.IntMap[_]] || tpe <:< TypeRepr.of[mutable.LongMap[_]] ||
            tpe <:< TypeRepr.of[immutable.LongMap[_]]) withEncoderFor(methodKey, m.asTerm, out) { (out,x) =>
            val tpe1 = typeArg1(tpe); 
            tpe1.asType match
              case '[t1] =>  
                val writeVal2: ((Expr[JsonWriter], Term) => Expr[Unit]) = { (out, v) =>
                  genWriteVal[t1](v.asExprOf[t1], tpe1 :: types, isStringified, None, out)
                }
                if (tpe <:< TypeRepr.of[immutable.IntMap] ) {
                  if (cfg.mapAsArray) {
                    def writeVal1(out: Expr[JsonWriter], k: Term): Expr[Unit] = {
                      if (isStringified) {
                        '{ $out.writeValAsString(${k.asExprOf[Int]}) }
                      } else {
                        '{ $out.writeVal(${k.asExprOf[Int]}) }
                      }
                    }
                    genWriteMapAsArrayScala213(x.asExprOf[immutable.IntMap[t1]], writeVal1, writeVal2, out)
                  } else {
                    genWriteMapScala213(x.asExprOf[immutable.IntMap[t1]], (out, k) => '{ $out.writeKey(${k.asExprOf[Int]}) }, writeVal2, out)
                  }  
                } else { // Long key
                  if (cfg.mapAsArray) {
                    def writeVal1(out: Expr[JsonWriter], k: Term): Expr[Unit] = {
                      if (isStringified) {
                        '{ $out.writeValAsString(${k.asExprOf[Long]}) }
                      } else {
                        '{ $out.writeVal(${k.asExprOf[Long]}) }
                      }
                    }
                    genWriteMapAsArrayScala213(x.asExprOf[Map[Long,t1]], writeVal1, writeVal2, out)
                  } else {
                    genWriteMapScala213(x.asExprOf[Map[Long,t1]], (out, k) => '{ $out.writeKey(${k.asExprOf[Long]}) }, writeVal2, out)
                  }
                }
              case _ => ???
        } else if (tpe <:< TypeRepr.of[collection.Map[_, _]]) withEncoderFor(methodKey, m.asTerm, out) { (out,x) =>
          // TODO: this can be 
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          (tpe1.asType, tpe2.asType) match
            case ( '[t1], '[t2]) =>
              val writeVal2: ((Expr[JsonWriter], Term) => Expr[Unit]) = { (out, v) =>
                genWriteVal[t2](v.asExprOf[t2], tpe2 :: types, isStringified, None, out)
              }
              if (cfg.mapAsArray) {
                genWriteMapAsArrayScala213(x.asExprOf[collection.Map[t1,t2]], 
                    (out, k) => genWriteVal[t1](k.asExprOf[t1], tpe1 :: types, isStringified, None, out), 
                    writeVal2,
                    out)
              } else {
                genWriteMapScala213(x.asExprOf[collection.Map[t1,t2]], 
                    (out,k) => genWriteKey(k.asExprOf[t1], tpe1 :: types, out), 
                    writeVal2,
                    out)
              }
            case _ =>
              fail(s"Can't get types for ${tpe1}, ${tpe2}")
        } else if (tpe <:< TypeRepr.of[BitSet]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          genWriteArray(x.asExprOf[BitSet],
            (out, x) => {
               if (isStringified) '{ $out.writeValAsString(${x.asExprOf[Int]}) }
               else '{ $out.writeVal(${x.asExprOf[Int]}) }
            },
            out
          )
        } else if (tpe <:< TypeRepr.of[List[_]]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              '{ $out.writeArrayStart()
                  var l: _root_.scala.collection.immutable.List[t1] = ${x.asExprOf[List[t1]]}
                  while (!l.isEmpty) {
                    ${genWriteVal('{ l.head }, tpe1 :: types, isStringified, None, out)}
                    l = l.tail
                  }
                  $out.writeArrayEnd()
              }
            case _ => fail(s"Can't get types ${tpe1}")
        } else if (tpe <:< TypeRepr.of[IndexedSeq[_]]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)  // better typeArg
          tpe1.asType match
            case '[t1] =>
              val tx = x.asExprOf[IndexedSeq[t1]]
              '{  
                $out.writeArrayStart()
                val l = $tx.size
                var i = 0
                while (i < l) {
                  ${genWriteVal[t1]('{ $tx(i) }, typeArg1(tpe) :: types, isStringified, None, out)}
                  i += 1
                }
                $out.writeArrayEnd()
              }
            case _ => fail(s"Can't get types ${tpe1}")
        } else if (tpe <:< TypeRepr.of[Iterable[_]]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val tpe1 = typeArg1Of[Iterable](tpe)
          tpe1.asType match
            case '[t1] =>
              genWriteArray(x.asExprOf[Iterable[t1]], 
                  (out, x) => genWriteVal[t1](x.asExprOf[t1], typeArg1(tpe) :: types, isStringified, None, out),
                  out
              )
            case _ => fail(s"Can't get types ${tpe1}")
        } else if (tpe <:< TypeRepr.of[Array[_]]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val tpe1 = typeArg1(tpe)
          tpe1.asType match
            case '[t1] =>
              val tx = x.asExprOf[Array[t1]]
              '{
                $out.writeArrayStart()
                val l = $tx.length
                var i = 0
                while (i < l) {
                  ${genWriteVal[t1]( '{ $tx(i) }, typeArg1(tpe) :: types, isStringified, None, out)}
                  i += 1
                }
                $out.writeArrayEnd()
              }
            case _ => fail(s"Can't get types ${tpe1}")
        } else if (tpe <:< TypeRepr.of[Enumeration#Value]) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val tx = x.asExprOf[Enumeration#Value]
          if (cfg.useScalaEnumValueId) {
            if (isStringified) '{ $out.writeValAsString($tx.id) }
            else '{ $out.writeVal($tx.id) }
          } else '{ $out.writeVal($tx.toString) }
        } else if (isJavaEnum(tpe)) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          val es = javaEnumValues(tpe)
          val encodingRequired = es.exists(e => isEncodingRequired(e.name))
          if (es.exists(_.transformed)) {
            val cases = es.map(e =>  CaseDef( Ref(e.value), None, Expr(e.name).asTerm)) :+
              CaseDef(Wildcard(), None, '{ $out.encodeError("illegal enum value: " + ${x.asExpr}) }.asTerm )
              val matching = Match( x, cases.toList ).asExprOf[String]
            if (encodingRequired) 
                '{ $out.writeVal($matching) }
            else 
                '{ $out.writeNonEscapedAsciiVal($matching) }
          } else {
            val tx = x.asExprOf[java.lang.Enum[?]]
            if (encodingRequired) '{ $out.writeVal($tx.name) }
            else '{ $out.writeNonEscapedAsciiVal( $tx.name) }
          }
        } else if (tpe.typeSymbol.flags.is(Flags.Module)) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          '{
              $out.writeObjectStart()
              ${ 
                optWriteDiscriminator match
                  case Some(discriminator) =>
                    discriminator.write(out)
                  case None => '{} 
              }
              $out.writeObjectEnd()
          }
        } else if (isTuple(tpe)) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          tpe match
            case AppliedType(base, typeArgs) =>
              val writeFields = typeArgs.zipWithIndex.map { case (ta, i) =>
                val name = "_" + (i + 1)
                val t = Select.unique(x,name)
                genWriteVal(t.asExpr, ta.dealias :: types, isStringified, None, out)
              }
              val block = Block(
                '{ $out.writeArrayStart() }.asTerm :: writeFields.map(_.asTerm).toList,
                '{ $out.writeArrayEnd() }.asTerm,
              )
              block.asExprOf[Unit]
            case _ => fail(s"exprected that ${tpe.show} should be AppliedType")
        } else if (isSealedClass(tpe)) withEncoderFor(methodKey, m.asTerm, out) { (out, x) =>
          def genWriteLeafClass(subTpe: TypeRepr, discriminator: Option[WriteDiscriminator]): Expr[Unit] = {
            subTpe.asType match
              case '[st] =>
                if (subTpe != tpe) { // TODO: check chanr to =:=
                  genWriteVal[st](x.asExprOf[st], subTpe :: types, isStringified, discriminator, out)
                } else {
                  genWriteNonAbstractScalaClass(x.asExprOf[st], types, discriminator, out)
                }
              case _ => fail(s"Can't get type of ${subTpe.show}")
          } 
          val leafClasses = adtLeafClasses(tpe)
          val writeSubclasses = (cfg.discriminatorFieldName match {
            case None =>
              val (leafModuleClasses, leafCaseClasses) = leafClasses.partition(_.typeSymbol.flags.is(Flags.Module))
              leafCaseClasses.map { subTpe =>
                CaseDef(
                  Typed(x, Inferred(subTpe)), None,
                  '{
                    $out.writeObjectStart()
                    ${genWriteConstantKey(discriminatorValue(subTpe), out)}
                    ${genWriteLeafClass(subTpe, None)}
                    $out.writeObjectEnd()
                  }.asTerm
                )
              } ++ leafModuleClasses.map{ subTpe =>
                  CaseDef(Typed(x, Inferred(subTpe)), None, 
                    genWriteConstantVal(discriminatorValue(subTpe), out).asTerm
                  )
              }
            case Some(discrFieldName) =>
              leafClasses.map{ subTpe =>
                val writeDiscriminator = WriteDiscriminator(discrFieldName,  discriminatorValue(subTpe))
                CaseDef(Typed(x, Inferred(subTpe)), None, genWriteLeafClass(subTpe, Some(writeDiscriminator)).asTerm) 
              }
          }) :+ CaseDef(Literal(NullConstant()),  None, '{ $out.writeNull() }.asTerm )
          Match(x, writeSubclasses.toList).asExprOf[Unit]
        } else if (isNonAbstractScalaClass(tpe)) withEncoderFor(methodKey, m.asTerm, out) { (out,x) =>
          genWriteNonAbstractScalaClass[T](x.asExprOf[T], types, optWriteDiscriminator, out)
        } else if (isConstType(tpe)) getWriteConstType(tpe, m.asTerm, isStringified, out)
        else cannotFindValueCodecError(tpe)
      }

      val codec = rootTpe.asType match {
        case '[rootType] =>
          val codecExpr = '{
            new JsonValueCodec[rootType] {
               def nullValue: rootType = ${genNullValue[rootType](rootTpe :: Nil)}
               
               def decodeValue(in: JsonReader, default: rootType): rootType = {
                  ${genReadVal(rootTpe :: Nil, 'default, cfg.isStringified, None, 'in)}
               }

               def encodeValue(x: rootType, out: JsonWriter): Unit = {
                 ${genWriteVal('x, rootTpe::Nil, cfg.isStringified, None, 'out)}
               }
               
            }
          }

          val needDefs: List[Statement] = (decodeMethodDefs.values.toList: List[Statement]) ++
                                          (encodeMethodDefs.values.toList: List[Statement]) ++
                                          (fieldIndexAccessors.values.toList: List[Statement]) ++
                                          (equalsMethods.values.toList: List[Statement]) ++
                                          (nullValues.values.toList: List[Statement]) ++
                                          (mathContexts.values.toList: List[Statement]) ++
                                          (scalaEnumCaches.values.toList: List[Statement])
          val retBlock = Block(
              needDefs,
              codecExpr.asTerm
          )
          retBlock
      }
      
           
      if (traceFlag || true) {  // TODO: insert flag from macro context
        report.info(s"Generated JSON codec for type '$rootTpe':\n${codec.show}", Position.ofMacroExpansion)
      }
      codec.asExprOf[JsonValueCodec[A]]
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