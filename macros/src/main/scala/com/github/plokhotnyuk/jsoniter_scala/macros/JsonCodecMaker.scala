package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, JsonReader, JsonWriter}

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field
import scala.collection.generic.Growable
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.{breakOut, mutable}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class named(name: String) extends StaticAnnotation

@field
class transient extends StaticAnnotation

@field
class stringified extends StaticAnnotation

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
  */
case class CodecMakerConfig(
  fieldNameMapper: String => String = identity,
  adtLeafClassNameMapper: String => String = JsonCodecMaker.simpleClassName,
  discriminatorFieldName: String = "type",
  isStringified: Boolean = false,
  skipUnexpectedFields: Boolean = true) extends StaticAnnotation

object JsonCodecMaker {
  def enforceCamelCase(s: String): String =
    if (s.indexOf('_') == -1) s
    else {
      val len = s.length
      val sb = new StringBuilder(len)
      var i = 0
      var isPrecedingDash = false
      while (i < len) isPrecedingDash = {
        val ch = s.charAt(i)
        i += 1
        if (ch == '_') true
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
      if (ch == '_') {
        sb.append(ch)
        false
      } else if (isLowerCase(ch)) {
        sb.append(ch)
        true
      } else {
        if (isPrecedingLowerCased) sb.append('_')
        sb.append(toLowerCase(ch))
        false
      }
    }
    sb.toString
  }

  def simpleClassName(fullClassName: String): String =
    fullClassName.substring(Math.max(fullClassName.lastIndexOf('.') + 1, 0))

  def make[A](config: CodecMakerConfig): JsonCodec[A] = macro Impl.make[A]

  private object Impl {
    def make[A: c.WeakTypeTag](c: blackbox.Context)(config: c.Expr[CodecMakerConfig]): c.Expr[JsonCodec[A]] = {
      import c.universe._

      def fail(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

      def warn(msg: String): Unit = c.warning(c.enclosingPosition, msg)

      def info(msg: String): Unit = c.info(c.enclosingPosition, msg, force = true)

      def methodType(m: MethodSymbol): Type = m.returnType.dealias

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs.tail.head.dealias

      def isValueClass(tpe: Type): Boolean = tpe <:< typeOf[AnyVal] && tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueType(tpe: Type): Type = methodType(tpe.decls.head.asMethod)

      def isSealedAdtBase(tpe: Type): Boolean = {
        val classSymbol = tpe.typeSymbol.asClass
        (classSymbol.isAbstract || classSymbol.isTrait) &&
          (if (classSymbol.isSealed) true
          else fail("Only sealed traits & abstract classes are supported for an ADT base. Please consider adding " +
            s"of a sealed definition for '$tpe' or using a custom implicitly accessible codec for the ADT base."))
      }

      def adtLeafClasses(tpe: Type): Set[Type] = tpe.typeSymbol.asClass.knownDirectSubclasses.flatMap { s =>
        val subTpe = s.asClass.toType
        if (isSealedAdtBase(subTpe)) adtLeafClasses(subTpe)
        else if (s.asClass.isCaseClass) Set(subTpe)
        else fail("Only case classes & case objects are supported for ADT leaf classes. Please consider using " +
          s"of them for ADT with base '$tpe' or using a custom implicitly accessible codec for the ADT base.")
      }

      def isContainer(tpe: Type): Boolean =
        tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[Array[_]]

      def containerCompanion(tpe: Type): Tree = {
        val comp = tpe.typeSymbol.companion
        if (comp.isModule && (tpe <:< typeOf[Option[_]] || comp.fullName.startsWith("scala.collection."))) Ident(comp)
        else fail(s"Unsupported type '$tpe'. Please consider using a custom implicitly accessible codec for it.")
      }

      def enumSymbol(tpe: Type): Symbol = {
        val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
        enumSymbol
      }

      def genReadKey(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readKeyAsBoolean()"
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
        else if (tpe =:= typeOf[BigDecimal]) q"in.readKeyAsBigDecimal()"
        else if (tpe <:< typeOf[Enumeration#Value]) q"${enumSymbol(tpe)}.withName(in.readKeyAsString())"
        else fail(s"Unsupported type to be used as map key '$tpe'.")

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"x"): Tree =
        genReadCollection(newBuilder, readVal, result, q"'['", q"']'", q"in.arrayEndError()")

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        genReadCollection(newBuilder, readKV, result, q"'{'", q"'}'", q"in.objectEndError()")

      def genReadCollection(newBuilder: Tree, loopBody: Tree, result: Tree,
                            open: Tree, close: Tree, endError: Tree): Tree =
        q"""if (in.isNextToken($open)) {
              if (in.isNextToken($close)) default
              else {
                in.rollbackToken()
                ..$newBuilder
                do {
                  ..$loopBody
                } while (in.isNextToken(','))
                if (in.isCurrentToken($close)) $result
                else $endError
              }
            } else in.readNullOrTokenError(default, $open)"""

      def genWriteConstantKey(name: String): Tree =
        if (name.forall(JsonWriter.isNonEscapedAscii)) q"out.writeNonEscapedAsciiKey($name)"
        else q"out.writeKey($name)"

      def genWriteConstantVal(value: String): Tree =
        if (value.forall(JsonWriter.isNonEscapedAscii)) q"out.writeNonEscapedAsciiVal($value)"
        else q"out.writeVal($value)"

      def genWriteArray(m: Tree, writeVal: Tree): Tree =
        q"""if ($m ne null) {
              out.writeArrayStart()
              $m.foreach { x =>
                out.writeComma()
                ..$writeVal
              }
              out.writeArrayEnd()
            } else out.writeNull()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""if ($m ne null) {
              out.writeObjectStart()
              $m.foreach { kv =>
                out.writeKey(kv._1)
                ..$writeKV
              }
              out.writeObjectEnd()
            } else out.writeNull()"""

      def cannotFindCodecError(tpe: Type): Nothing = fail(s"No implicit '${typeOf[JsonCodec[_]]}' defined for '$tpe'.")

      def findImplicitCodec(tpe: Type): Tree = c.inferImplicitValue(c.typecheck(tq"JsonCodec[$tpe]", c.TYPEmode).tpe)

      case class FieldAnnotations(name: String, transient: Boolean, stringified: Boolean)

      def getFieldAnnotations(tpe: Type): Map[String, FieldAnnotations] = tpe.members.collect {
        case m: TermSymbol if m.annotations.exists(a => a.tree.tpe <:< c.weakTypeOf[named]
            || a.tree.tpe <:< c.weakTypeOf[transient] || a.tree.tpe <:< c.weakTypeOf[stringified]) =>
          val fieldName = m.name.toString.trim // FIXME: Why is there a space at the end of field name?!
          val named = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[named])
          if (named.size > 1) fail(s"Duplicated '${typeOf[named]}' defined for '$fieldName' of '$tpe'.")
          val trans = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[transient])
          if (trans.size > 1) warn(s"Duplicated '${typeOf[transient]}' defined for '$fieldName' of '$tpe'.")
          val strings = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[stringified])
          if (strings.size > 1) warn(s"Duplicated '${typeOf[stringified]}' defined for '$fieldName' of '$tpe'.")
          if ((named.nonEmpty || strings.nonEmpty) && trans.size == 1) {
            warn(s"Both '${typeOf[transient]}' and '${typeOf[named]}' or " +
              s"'${typeOf[transient]}' and '${typeOf[stringified]}' defined for '$fieldName' of '$tpe'.")
          }
          val name = named.headOption.flatMap(_.tree.children.tail.collectFirst {
            case Literal(Constant(name: String)) => Option(name).getOrElse(fieldName)
          }).getOrElse(fieldName)
          (fieldName, FieldAnnotations(name, trans.nonEmpty, strings.nonEmpty))
      }(breakOut)

      def getModule(tpe: Type): ModuleSymbol = {
        val comp = tpe.typeSymbol.companion
        if (!comp.isModule) {
          fail(s"Can't find companion object for '$tpe'. This can happen when it's nested too deeply. " +
              "Please consider defining it as a top-level object or directly inside of another class or object.")
        }
        comp.asModule //FIXME: module cannot be resolved properly for deeply nested inner case classes
      }

      def getParams(tpe: Type): Seq[TermSymbol] = tpe.decl(termNames.CONSTRUCTOR).asTerm.alternatives.flatMap {
        case m: MethodSymbol => m.paramLists.head.map(_.asTerm)
      } //FIXME: handling only default val params from the first list because subsequent might depend on previous params

      def getDefaults(tpe: Type): Map[String, Tree] = {
        val module = getModule(tpe)
        getParams(tpe).zipWithIndex.collect { case (p, i) if p.isParamWithDefault =>
          (p.name.toString, q"$module.${TermName("$lessinit$greater$default$" + (i + 1))}")
        }(breakOut)
      }

      def getMembers(annotations: Map[String, FieldAnnotations], tpe: c.universe.Type): Seq[MethodSymbol] = {
        def nonTransient(m: MethodSymbol): Boolean = annotations.get(m.name.toString).fold(true)(!_.transient)

        tpe.members.collect {
          case m: MethodSymbol if m.isCaseAccessor && nonTransient(m) => m
        }(breakOut).reverse
      }

      val rootTpe = weakTypeOf[A]
      val codecConfig = c.eval[CodecMakerConfig](c.Expr[CodecMakerConfig](c.untypecheck(config.tree)))
      val unexpectedFieldHandler =
        if (codecConfig.skipUnexpectedFields) q"in.skip()"
        else q"in.unexpectedKeyError(l)"
      val skipDiscriminatorField = {
        val cs = codecConfig.discriminatorFieldName.toCharArray
        cq"${JsonReader.toHashCode(cs, cs.length)} => in.skip()"
      }

      def discriminatorValue(tpe: Type): String = codecConfig.adtLeafClassNameMapper(tpe.typeSymbol.fullName)

      def getStringified(annotations: Map[String, FieldAnnotations], name: String): Boolean =
        annotations.get(name).fold(false)(_.stringified)

      def getMappedName(annotations: Map[String, FieldAnnotations], defaultName: String): String =
        annotations.get(defaultName).fold(codecConfig.fieldNameMapper(defaultName))(_.name)

      def getCollisions(names: Traversable[String]): Traversable[String] =
        names.groupBy(identity).collect { case (x, xs) if xs.size > 1 => x }

      def checkFieldNameCollisions(tpe: Type, names: Traversable[String]): Unit = {
        val collisions = getCollisions(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated JSON name(s) defined for '$tpe': $formattedCollisions. " +
              s"Names(s) defined by '${typeOf[named]}' annotation(s), " +
              "name of discriminator field specified by 'config.discriminatorFieldName' " +
              "and name(s) returned by 'config.nameMapper' for non-annotated fields should not match.")
        }
      }

      def checkDiscriminatorValueCollisions(discriminatorFieldName: String, names: Traversable[String]): Unit = {
        val collisions = getCollisions(names)
        if (collisions.nonEmpty) {
          val formattedCollisions = collisions.mkString("'", "', '", "'")
          fail(s"Duplicated value(s) defined for '$discriminatorFieldName': $formattedCollisions. " +
            s"Values(s) returned by 'config.classNameMapper' should not match.")
        }
      }

      val nullValueNames = mutable.LinkedHashMap.empty[Type, TermName]
      val nullValueTrees = mutable.LinkedHashMap.empty[Type, Tree]

      // use it only for immutable values which doesn't have public constants
      def withNullValueFor(tpe: Type)(f: => Tree): Tree = {
        val nullValueName = nullValueNames.getOrElseUpdate(tpe, TermName("v" + nullValueNames.size))
        nullValueTrees.getOrElseUpdate(tpe, {
          val impl = f
          q"private val $nullValueName: $tpe = $impl"
        })
        q"$nullValueName"
      }

      val reqFieldNames = mutable.LinkedHashMap.empty[Type, TermName]
      val reqFieldTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withReqFieldsFor(tpe: Type)(f: => Seq[String]): Tree = {
        val reqFieldName = reqFieldNames.getOrElseUpdate(tpe, TermName("r" + reqFieldNames.size))
        reqFieldTrees.getOrElseUpdate(tpe, {
          val impl = f
          q"private val $reqFieldName: Array[String] = Array(..$impl)"
        })
        q"$reqFieldName"
      }

      case class MethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree)

      def getMethodKey(tpe: Type, isStringified: Boolean, discriminator: Tree): MethodKey =
        MethodKey(tpe, isStringified && isContainer(tpe), discriminator)

      val decodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val decodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withDecoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val decodeMethodName = decodeMethodNames.getOrElseUpdate(methodKey, TermName("d" + decodeMethodNames.size))
        decodeMethodTrees.getOrElseUpdate(methodKey, {
          val impl = f
          q"private def $decodeMethodName(in: JsonReader, default: ${methodKey.tpe}): ${methodKey.tpe} = $impl"
        })
        q"$decodeMethodName(in, $arg)"
      }

      val encodeMethodNames = mutable.LinkedHashMap.empty[MethodKey, TermName]
      val encodeMethodTrees = mutable.LinkedHashMap.empty[MethodKey, Tree]

      def withEncoderFor(methodKey: MethodKey, arg: Tree)(f: => Tree): Tree = {
        val encodeMethodName = encodeMethodNames.getOrElseUpdate(methodKey, TermName("e" + encodeMethodNames.size))
        encodeMethodTrees.getOrElseUpdate(methodKey, {
          val impl = f
          q"private def $encodeMethodName(x: ${methodKey.tpe}, out: JsonWriter): Unit = $impl"
        })
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
          q"${containerCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"${containerCompanion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) q"${containerCompanion(tpe)}.empty"
        else if (tpe <:< typeOf[Traversable[_]]) q"${containerCompanion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[Array[_]]) withNullValueFor(tpe)(q"new Array[${typeArg1(tpe)}](0)")
        else if (tpe.typeSymbol.isModuleClass) q"${tpe.typeSymbol.asClass.module}"
        else q"null"

      def genReadVal(tpe: Type, default: Tree, isStringified: Boolean, discriminator: Tree = EmptyTree): Tree = {
        val implCodec = findImplicitCodec(tpe) // FIXME: add testing that implicit codecs should override any defaults
        val methodKey = getMethodKey(tpe, isStringified, discriminator)
        val decodeMethodName = decodeMethodNames.get(methodKey)
        if (!implCodec.isEmpty) q"$implCodec.decode(in, $default)"
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
        else if (tpe =:= typeOf[BigInt]) {
          if (isStringified) q"in.readStringAsBigInt($default)" else q"in.readBigInt($default)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          if (isStringified) q"in.readStringAsBigDecimal($default)" else q"in.readBigDecimal($default)"
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          q"new $tpe(${genReadVal(tpe1, nullValue(tpe1), isStringified)})"
        } else if (tpe <:< typeOf[Option[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          q"""val x = ${genReadVal(tpe1, nullValue(tpe1), isStringified)}
              if (x eq null) None else Some(x)"""
        } else if (tpe <:< typeOf[IntMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = containerCompanion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1]",
            q"x = x.updated(in.readKeyAsInt(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = containerCompanion(tpe)
          genReadMap(q"val x = if ((default ne null) && default.isEmpty) default else $comp.empty[$tpe1]",
            q"x.update(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = containerCompanion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1]",
            q"x = x.updated(in.readKeyAsLong(), ${genReadVal(tpe1, nullValue(tpe1), isStringified)})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = containerCompanion(tpe)
          genReadMap(q"val x = if ((default ne null) && default.isEmpty) default else $comp.empty[$tpe1, $tpe2]",
            q"x.update(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})")
        } else if (tpe <:< typeOf[Map[_, _]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = containerCompanion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1, $tpe2]",
            q"x = x.updated(${genReadKey(tpe1)}, ${genReadVal(tpe2, nullValue(tpe2), isStringified)})")
        } else if (tpe <:< typeOf[mutable.BitSet]) withDecoderFor(methodKey, default) {
          val comp = containerCompanion(tpe)
          val readVal = if (isStringified) q"x.add(in.readStringAsInt())" else q"x.add(in.readInt())"
          genReadArray(q"val x = if ((default ne null) && default.isEmpty) default else $comp.empty", readVal)
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(methodKey, default) {
          val comp = containerCompanion(tpe)
          val readVal = if (isStringified) q"x += in.readStringAsInt()" else q"x += in.readInt()"
          genReadArray(q"val x = $comp.newBuilder", readVal, q"x.result()")
        } else if (tpe <:< typeOf[mutable.Traversable[_] with Growable[_]] &&
            !(tpe <:< typeOf[mutable.ArrayStack[_]])) withDecoderFor(methodKey, default) { // ArrayStack uses 'push' for '+='
          val tpe1 = typeArg1(tpe)
          val comp = containerCompanion(tpe)
          genReadArray(q"val x = if ((default ne null) && default.isEmpty) default else $comp.empty[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}")
        } else if (tpe <:< typeOf[Traversable[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          val comp = containerCompanion(tpe)
          genReadArray(q"val x = $comp.newBuilder[$tpe1]",
            q"x += ${genReadVal(tpe1, nullValue(tpe1), isStringified)}", q"x.result()")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(methodKey, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(
            q"""var x = new Array[$tpe1](16)
                var i = 0""",
            q"""if (i == x.length) {
                  val y = new Array[$tpe1](i << 1)
                  System.arraycopy(x, 0, y, 0, i)
                  x = y
                }
                x(i) = ${genReadVal(tpe1, nullValue(tpe1), isStringified)}
                i += 1""",
            q"""if (i == x.length) x
                else {
                  val y = new Array[$tpe1](i)
                  System.arraycopy(x, 0, y, 0, i)
                  y
                }""")
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(methodKey, default) {
          q"""val v = in.readString()
              if (v eq null) default
              else try ${enumSymbol(tpe)}.withName(v) catch {
                case _: NoSuchElementException => in.enumValueError(v)
              }"""
        } else if (tpe.typeSymbol.isModuleClass) withDecoderFor(methodKey, default) {
          q"""if (in.isNextToken('{')) {
                in.rollbackToken()
                in.skip()
                ${tpe.typeSymbol.asClass.module}
              } else in.readNullOrTokenError(default, '{')"""
        } else if (tpe.typeSymbol.asClass.isCaseClass) withDecoderFor(methodKey, default) {
          val annotations = getFieldAnnotations(tpe)

          def name(m: MethodSymbol): String = getMappedName(annotations, m.name.toString)

          def hashCode(m: MethodSymbol): Int = {
            val cs = name(m).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }

          val members = getMembers(annotations, tpe)
          checkFieldNameCollisions(tpe,
            (if (discriminator.isEmpty) Seq.empty else Seq(codecConfig.discriminatorFieldName)) ++ members.map(name))
          val params = getParams(tpe)
          val required = params.collect {
            case p if !p.isParamWithDefault && !isContainer(p.typeSignature) => p.name.toString
          }
          val reqVarNum = required.size
          val lastReqVarIndex = reqVarNum >> 5
          val lastReqVarBits = (1 << reqVarNum) - 1
          val reqVarNames = (0 to lastReqVarIndex).map(i => TermName(s"req$i"))
          val bitmasks: Map[String, Tree] = required.zipWithIndex.map {
            case (r, i) => (r, q"${reqVarNames(i >> 5)} &= ${~(1 << i)}")
          }(breakOut)
          val reqVars =
            if (lastReqVarBits == 0) Nil
            else reqVarNames.init.map(n => q"var $n = -1") :+ q"var ${reqVarNames.last} = $lastReqVarBits"
          val checkReqVars = reqVarNames.map(n => q"$n == 0").reduce((e1, e2) => q"$e1 && $e2")
          val construct = q"new $tpe(..${members.map(m => q"${m.name} = ${TermName(s"_${m.name}")}")})"
          val checkReqVarsAndConstruct =
            if (lastReqVarBits == 0) construct
            else {
              val reqFieldNames = withReqFieldsFor(tpe) {
                required.map(r => getMappedName(annotations, r))
              }
              q"""if ($checkReqVars) $construct
                  else in.requiredKeyError($reqFieldNames, Array(..$reqVarNames))"""
            }
          val defaults = getDefaults(tpe)
          val readVars = members.map { m =>
            val tpe = methodType(m)
            q"var ${TermName(s"_${m.name}")}: $tpe = ${defaults.getOrElse(m.name.toString, nullValue(tpe))}"
          }
          val readFields = groupByOrdered(members)(hashCode).map { case (hashCode, ms) =>
            val checkNameAndReadValue = ms.foldRight(unexpectedFieldHandler) { case (m, acc) =>
              val varName = TermName(s"_${m.name}")
              val isStringified = getStringified(annotations, m.name.toString)
              val readValue = q"$varName = ${genReadVal(methodType(m), q"$varName", isStringified)}"
              val resetReqFieldFlag = bitmasks.getOrElse(m.name.toString, EmptyTree)
              q"""if (in.isCharBufEqualsTo(l, ${name(m)})) {
                    ..$readValue
                    ..$resetReqFieldFlag
                  } else $acc"""
            }
            cq"$hashCode => $checkNameAndReadValue"
          }(breakOut)
          val readFieldsBlock =
            (if (discriminator.isEmpty) readFields
            else readFields :+ discriminator) :+ cq"_ => $unexpectedFieldHandler"
          q"""if (in.isNextToken('{')) {
                ..$readVars
                ..$reqVars
                if (!in.isNextToken('}')) {
                  in.rollbackToken()
                  do {
                    val l = in.readKeyAsCharBuf()
                    (in.charBufToHashCode(l): @switch) match {
                      case ..$readFieldsBlock
                    }
                  } while (in.isNextToken(','))
                  if (!in.isCurrentToken('}')) in.objectEndError()
                }
                ..$checkReqVarsAndConstruct
              } else in.readNullOrTokenError(default, '{')"""
        } else if (isSealedAdtBase(tpe)) withDecoderFor(methodKey, default) {
          def hashCode(subTpe: Type): Int = {
            val cs = discriminatorValue(subTpe).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }

          val leafClasses = adtLeafClasses(tpe)
          checkDiscriminatorValueCollisions(codecConfig.discriminatorFieldName, leafClasses.map(discriminatorValue))
          val discriminatorValueError = q"in.discriminatorValueError(${codecConfig.discriminatorFieldName})"
          val readSubclasses = groupByOrdered(leafClasses)(hashCode).map { case (hashCode, subTpes) =>
            val checkNameAndReadValue = subTpes.foldRight(discriminatorValueError) { case (subTpe, acc) =>
              q"""if (in.isCharBufEqualsTo(l, ${discriminatorValue(subTpe)})) {
                    in.rollbackToMark()
                    ..${genReadVal(subTpe, nullValue(subTpe), isStringified, skipDiscriminatorField)}
                  } else $acc"""
            }
            cq"$hashCode => $checkNameAndReadValue"
          }(breakOut)
          q"""in.setMark()
              if (in.isNextToken('{')) {
                in.scanToKey(${codecConfig.discriminatorFieldName})
                val l = in.readStringAsCharBuf()
                (in.charBufToHashCode(l): @switch) match {
                  case ..$readSubclasses
                  case _ => $discriminatorValueError
                }
              } else in.readNullOrTokenError(default, '{')"""
        } else cannotFindCodecError(tpe)
      }

      def genWriteVal(m: Tree, tpe: Type, isStringified: Boolean, discriminator: Tree = EmptyTree): Tree = {
        val implCodec = findImplicitCodec(tpe) // FIXME: add testing that implicit codecs should override any defaults
        val methodKey = getMethodKey(tpe, isStringified, discriminator)
        val encodeMethodName = encodeMethodNames.get(methodKey)
        if (!implCodec.isEmpty) q"$implCodec.encode($m, out)"
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
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] || tpe =:= typeOf[String]) {
          q"out.writeVal($m)"
        } else if (isValueClass(tpe)) genWriteVal(q"$m.value", valueClassValueType(tpe), isStringified)
        else if (tpe <:< typeOf[Option[_]]) withEncoderFor(methodKey, m) {
          q"if ((x eq null) || x.isEmpty) out.writeNull() else ${genWriteVal(q"x.get", typeArg1(tpe), isStringified)}"
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] ||
            tpe <:< typeOf[LongMap[_]]) withEncoderFor(methodKey, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg1(tpe), isStringified))
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) withEncoderFor(methodKey, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg2(tpe), isStringified))
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", if (isStringified) q"out.writeValAsString(x)" else q"out.writeVal(x)")
        } else if (tpe <:< typeOf[Traversable[_]]) withEncoderFor(methodKey, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe), isStringified))
        } else if (tpe <:< typeOf[Array[_]]) withEncoderFor(methodKey, m) {
          q"""if (x ne null) {
                out.writeArrayStart()
                val l = x.length
                var i = 0
                while (i < l) {
                  out.writeComma()
                  ..${genWriteVal(q"x(i)", typeArg1(tpe), isStringified)}
                  i += 1
                }
                out.writeArrayEnd()
              } else out.writeNull()"""
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(methodKey, m) {
          q"if (x ne null) out.writeVal(x.toString) else out.writeNull()"
        } else if (tpe.typeSymbol.isModuleClass) withEncoderFor(methodKey, m) {
          q"""if (x ne null) {
                out.writeObjectStart()
                ..$discriminator
                out.writeObjectEnd()
              } else out.writeNull()"""
        } else if (tpe.typeSymbol.asClass.isCaseClass) withEncoderFor(methodKey, m) {
          val annotations = getFieldAnnotations(tpe)
          val members = getMembers(annotations, tpe)
          val defaults = getDefaults(tpe)
          val writeFields = members.map { m =>
            val tpe = methodType(m)
            val name = getMappedName(annotations, m.name.toString)
            val isStringified = getStringified(annotations, m.name.toString)
            defaults.get(m.name.toString) match {
              case Some(d) =>
                if (isContainer(tpe)) {
                  val nonEmptyAndDefaultMatchingCheck =
                    if (tpe <:< typeOf[Array[_]]) q"v.length > 0 && (v.length != $d.length || v.deep != $d.deep)"
                    else q"!v.isEmpty && v != $d"
                  q"""val v = x.$m
                      if ((v ne null) && $nonEmptyAndDefaultMatchingCheck) {
                        ..${genWriteConstantKey(name)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else {
                  q"""val v = x.$m
                      if (v != $d) {
                        ..${genWriteConstantKey(name)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                }
              case None =>
                if (isContainer(tpe)) {
                  val nonEmptyCheck = if (tpe <:< typeOf[Array[_]]) q"v.length > 0" else q"!v.isEmpty"
                  q"""val v = x.$m
                      if ((v ne null) && $nonEmptyCheck) {
                        ..${genWriteConstantKey(name)}
                        ..${genWriteVal(q"v", tpe, isStringified)}
                      }"""
                } else {
                  q"""..${genWriteConstantKey(name)}
                      ..${genWriteVal(q"x.$m", tpe, isStringified)}"""
                }
            }
          }
          val allWriteFields =
            if (discriminator.isEmpty) writeFields
            else discriminator +: writeFields
          q"""if (x ne null) {
                out.writeObjectStart()
                ..$allWriteFields
                out.writeObjectEnd()
              } else out.writeNull()"""
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
                case null => out.writeNull()
              }"""
        } else cannotFindCodecError(tpe)
      }

      val codec =
        q"""import com.github.plokhotnyuk.jsoniter_scala.core._
            import scala.annotation.switch
            new JsonCodec[$rootTpe] {
              def nullValue: $rootTpe = ${nullValue(rootTpe)}
              def decode(in: JsonReader, default: $rootTpe): $rootTpe =
                ${genReadVal(rootTpe, q"default", codecConfig.isStringified)}
              def encode(x: $rootTpe, out: JsonWriter): Unit =
                ${genWriteVal(q"x", rootTpe, codecConfig.isStringified)}
              ..${nullValueTrees.values}
              ..${reqFieldTrees.values}
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
            }"""
      if (c.settings.contains("print-codecs")) info(s"Generated JSON codec for type '$rootTpe':\n${showCode(codec)}")
      c.Expr[JsonCodec[A]](codec)
    }
  }

  private def groupByOrdered[A, K](xs: Traversable[A])(f: A => K): mutable.Map[K, mutable.Buffer[A]] = {
    val m = mutable.LinkedHashMap.empty[K, mutable.Buffer[A]].withDefault(_ => mutable.Buffer.empty[A])
    xs.foreach { x =>
      val k = f(x)
      m(k) = m(k) :+ x
    }
    m
  }
}