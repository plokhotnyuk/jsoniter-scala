package com.github.plokhotnyuk.jsoniter_scala

import com.jsoniter.{JsonIterator, JsonIteratorUtil}
import com.jsoniter.output.JsonStream
import com.jsoniter.spi.{Config, Decoder, Encoder, JsoniterSpi}
import com.jsoniter.spi.JsoniterSpi.{addNewDecoder, addNewEncoder, getCurrentConfig}

import scala.annotation.meta.field
import scala.collection.breakOut
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class key(key: String) extends scala.annotation.StaticAnnotation

abstract class Codec[A](implicit m: Manifest[A]) extends Encoder with Decoder {
  private val cls = m.runtimeClass.asInstanceOf[Class[A]]

  JsoniterSpi.setDefaultConfig((new Config.Builder).escapeUnicode(false).build)
  addNewEncoder(getCurrentConfig.getEncoderCacheKey(cls), this)
  addNewDecoder(getCurrentConfig.getDecoderCacheKey(cls), this)

  def read(in: JsonIterator): A

  def write(obj: A, out: JsonStream): Unit

  override def decode(in: JsonIterator): AnyRef = read(in).asInstanceOf[AnyRef]

  override def encode(obj: AnyRef, out: JsonStream): Unit = write(obj.asInstanceOf[A], out)
}

object Codec {
  def materialize[A]: Codec[A] = macro Impl.materialize[A]

  private object Impl {
    def materialize[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[Codec[A]] = {
      import c.universe._

      def methodType(m: MethodSymbol): Type = m.returnType.dealias

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs.tail.head.dealias

      def companion(tpe: Type): Tree = Ident(tpe.typeSymbol.companion)

      def isValueClass(tpe: Type): Boolean = tpe <:< typeOf[AnyVal] && tpe.typeSymbol.asClass.isDerivedValueClass

      def isContainer(tpe: Type): Boolean = tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Traversable[_]]

      def valueClassValueType(tpe: Type): Type = methodType(tpe.decls.head.asMethod)

      def defaultValue(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"false"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= definitions.CharTpe || tpe =:= definitions.ShortTpe ||
          tpe =:= definitions.IntTpe || tpe =:= definitions.LongTpe || tpe =:= definitions.DoubleTpe ||
          tpe =:= definitions.FloatTpe) {
          q"0"
        } else if (isValueClass(tpe)) {
          q"null.asInstanceOf[$tpe]"
        } else if (tpe <:< typeOf[Option[_]]) {
          q"None"
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[LongMap[_]] || tpe <:< typeOf[mutable.LongMap[_]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) {
          q"${companion(tpe)}.empty"
        } else if (tpe <:< typeOf[Traversable[_]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[Array[_]]) {
          q"new Array[${typeArg1(tpe)}](0)"
        } else {
          q"null"
        }

      def genReadKey(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"readObjectFieldAsBoolean(in)"
        } else if (tpe =:= definitions.ByteTpe) {
          q"readObjectFieldAsByte(in)"
        } else if (tpe =:= definitions.CharTpe) {
          q"readObjectFieldAsChar(in)"
        } else if (tpe =:= definitions.ShortTpe) {
          q"readObjectFieldAsShort(in)"
        } else if (tpe =:= definitions.IntTpe) {
          q"readObjectFieldAsInt(in)"
        } else if (tpe =:= definitions.LongTpe) {
          q"readObjectFieldAsLong(in)"
        } else if (tpe =:= definitions.DoubleTpe) {
          q"readObjectFieldAsDouble(in)"
        } else if (tpe =:= definitions.FloatTpe) {
          q"readObjectFieldAsFloat(in)"
        } else if (isValueClass(tpe)) {
          q"new $tpe(${genReadKey(valueClassValueType(tpe))})"
        } else if (tpe =:= typeOf[String]) {
          q"readObjectFieldAsString(in)"
        } else if (tpe =:= typeOf[BigInt]) {
          q"readObjectFieldAsBigInt(in)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"readObjectFieldAsBigDecimal(in)"
        } else if (tpe <:< typeOf[Enumeration#Value]) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"$enumSymbol.apply(readObjectFieldAsInt(in))"
        } else {
          c.abort(c.enclosingPosition, s"Unsupported type to be used as map key '$tpe'.")
        }

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"buf.result()"): Tree =
        q"""nextToken(in) match {
              case '[' =>
                if (nextToken(in) == ']') default
                else {
                  unreadByte(in)
                  ..$newBuilder
                  do {
                    ..$readVal
                  } while (nextToken(in) == ',')
                  ..$result
                }
              case 'n' =>
                parseNull(in, default)
              case _ =>
                decodeError(in, "expect [ or n")
            }"""

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"buf"): Tree =
        q"""nextToken(in) match {
              case '{' =>
                if (nextToken(in) == '}') default
                else {
                  unreadByte(in)
                  ..$newBuilder
                  do {
                    ..$readKV
                  } while (nextToken(in) == ',')
                  ..$result
                }
              case 'n' =>
                parseNull(in, default)
              case _ =>
                decodeError(in, "expect { or n")
            }"""

      val decoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withDecoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree =
        q"""${decoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"d${decoders.size}")
          (name, q"private def $name(in: JsonIterator, default: $tpe): $tpe = $impl")})._1}(in, $arg)"""

      val encoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withEncoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree =
        q"""${encoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"e${encoders.size}")
          (name, q"private def $name(out: JsonStream, x: $tpe): Unit = $impl")})._1}(out, $arg)"""

      def genReadField(tpe: Type, default: Tree): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"in.readBoolean()"
        } else if (tpe =:= definitions.ByteTpe) {
          q"readByte(in)"
        } else if (tpe =:= definitions.CharTpe) {
          q"readChar(in)"
        } else if (tpe =:= definitions.ShortTpe) {
          q"readShort(in)"
        } else if (tpe =:= definitions.IntTpe) {
          q"readInt(in)"
        } else if (tpe.widen =:= definitions.LongTpe) {
          q"readLong(in)"
        } else if (tpe =:= definitions.DoubleTpe) {
          q"readDouble(in)"
        } else if (tpe =:= definitions.FloatTpe) {
          q"readFloat(in)"
        } else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          q"new $tpe(${genReadField(tpe1, defaultValue(tpe1))})"
        } else if (tpe <:< typeOf[Option[_]]) {
          val tpe1 = typeArg1(tpe)
          q"Option(${genReadField(tpe1, defaultValue(tpe1))})"
        } else if (tpe <:< typeOf[IntMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"var buf = $comp.empty[$tpe1]",
            q"buf = buf.updated(readObjectFieldAsInt(in), ${genReadField(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"val buf = $comp.empty[$tpe1]",
            q"buf.update(readObjectFieldAsLong(in), ${genReadField(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"var buf = $comp.empty[$tpe1]",
            q"buf = buf.updated(readObjectFieldAsLong(in), ${genReadField(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"val buf = $comp.empty[$tpe1, $tpe2]",
            q"buf.update(${genReadKey(tpe1)}, ${genReadField(tpe2, defaultValue(tpe2))})")
        } else if (tpe <:< typeOf[Map[_, _]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"var buf = $comp.empty[$tpe1, $tpe2]",
            q"buf = buf.updated(${genReadKey(tpe1)}, ${genReadField(tpe2, defaultValue(tpe2))})")
        } else if (tpe <:< typeOf[mutable.BitSet]) withDecoderFor(tpe, default) {
          val comp = companion(tpe)
          genReadArray(q"val buf = $comp.empty", q"buf.add(readInt(in))", q"buf")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(tpe, default) {
          val comp = companion(tpe)
          genReadArray(q"val buf = $comp.newBuilder", q"buf += readInt(in)")
        } else if (tpe <:< typeOf[Traversable[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadArray(q"val buf = $comp.newBuilder[$tpe1]", q"buf += ${genReadField(tpe1, defaultValue(tpe1))}")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val buf = collection.mutable.ArrayBuilder.make[$tpe1]",
            q"buf += ${genReadField(tpe1, defaultValue(tpe1))}")
        } else if (tpe =:= typeOf[String]) {
          q"readString(in, $default)"
        } else if (tpe =:= typeOf[BigInt]) {
          q"readBigInt(in, $default)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"readBigDecimal(in, $default)"
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(tpe, default) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"""nextToken(in) match {
                case 'n' =>
                  parseNull(in, default)
                case _ =>
                  unreadByte(in)
                  val v = readInt(in)
                  try $enumSymbol.apply(v) catch {
                    case _: java.util.NoSuchElementException => decodeError(in, "invalid enum value: " + v)
                  }
              }"""
        } else withDecoderFor(tpe, default) {
          q"""val x = in.read(classOf[$tpe])
              if (x ne null) x else default"""
        }

      def genWriteArray(m: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            var first = true
            $m.foreach { x => first = writeSep(out, first); ..$writeVal }
            out.writeArrayEnd()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""out.writeObjectStart()
            var first = true
            $m.foreach { kv => first = writeSep(out, first); writeObjectField(out, kv._1); ..$writeKV }
            out.writeObjectEnd()"""

      def genWriteVal(m: Tree, tpe: Type): Tree =
        if (tpe <:< typeOf[Option[_]]) {
          genWriteVal(q"$m.get", typeArg1(tpe))
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] || tpe <:< typeOf[LongMap[_]]) withEncoderFor(tpe, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg1(tpe)))
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) withEncoderFor(tpe, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg2(tpe)))
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) withEncoderFor(tpe, m) {
          genWriteArray(q"x", q"out.writeVal(x)")
        } else if (tpe <:< typeOf[Traversable[_]]) withEncoderFor(tpe, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe)))
        } else if (tpe <:< typeOf[Array[_]]) withEncoderFor(tpe, m) {
          q"""out.writeArrayStart()
              val l = x.length
              var i = 0
              var first = true
              while (i < l) {
                first = writeSep(out, first)
                ..${genWriteVal(q"x(i)", typeArg1(tpe))}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal]) withEncoderFor(tpe, m) {
          q"if (x ne null) out.writeRaw(x.toString) else out.writeNull()"
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(tpe, m) {
          q"if (x ne null) out.writeVal(x.id) else out.writeNull()"
        } else {
          q"out.writeVal($m)"
        }

      def genWriteField(m: Tree, tpe: Type, name: String): Tree =
        if (isValueClass(tpe)) {
          genWriteField(q"$m.value", valueClassValueType(tpe), name)
        } else if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[scala.collection.Map[_, _]] || tpe <:< typeOf[Traversable[_]]) {
          q"if (($m ne null) && !$m.isEmpty) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else {
          q"first = writeSep(out, first); out.writeObjectField($name); ..${genWriteVal(m, tpe)}"
        }

      val tpe = weakTypeOf[A]
      if (!tpe.typeSymbol.asClass.isCaseClass) c.abort(c.enclosingPosition, s"'$tpe' must be a case class.")
      val annotations: Map[Symbol, Set[Annotation]] = tpe.members.collect {
        case m: TermSymbol if m.annotations.nonEmpty => (m.getter, m.annotations.toSet)
      }(breakOut)

      def nonTransient(m: MethodSymbol): Boolean =
        !annotations.get(m).exists(_.exists(_.tree.tpe <:< c.weakTypeOf[transient]))

      def keyName(m: MethodSymbol): String =
        annotations.get(m).flatMap(_.collectFirst { case a if a.tree.tpe <:< c.weakTypeOf[key] =>
          val Literal(Constant(key: String)) = a.tree.children.tail.head
          key
        }).getOrElse(m.name.toString)

      def hashCode(m: MethodSymbol): Long =
        JsonIteratorUtil.readObjectFieldAsHash(JsonIterator.parse(s""""${keyName(m)}":""".getBytes("UTF-8")))

      // FIXME: module cannot be resolved properly for deeply nested inner case classes
      val comp = tpe.typeSymbol.companion
      if (!comp.isModule) c.abort(c.enclosingPosition,
        s"Can't find companion object for '$tpe'. This can happen when it's nested too deeply. " +
          "Please consider defining it as a top-level object or directly inside of another class or object.")
      val module = comp.asModule
      val apply = module.typeSignature.decl(TermName("apply")).asMethod
      // FIXME: handling only default val params from the first list because subsequent might depend on previous params
      val params = apply.paramLists.head.map(_.asTerm)
      val defaults: Map[String, Tree] = params.zipWithIndex.collect {
        case (p, i) if p.isParamWithDefault => (p.name.toString, q"$module.${TermName("apply$default$" + (i + 1))}")
      }(breakOut)
      val required = params.collect {
        case p if !p.isParamWithDefault && !isContainer(p.typeSignature) => p.name.toString
      }
      val reqVarNum = required.size
      val lastReqVarIndex = reqVarNum >> 6
      val lastReqVarBits = (1L << reqVarNum) - 1
      val reqVarNames = (0 to lastReqVarIndex).map(i => TermName(s"req$i"))
      val bitmasks: Map[String, Tree] = required.zipWithIndex.map {
        case (r, i) => (r, q"${reqVarNames(i >> 6)} &= ${~(1L << i)}")
      }(breakOut)
      val reqVars =
        if (lastReqVarBits == 0) Nil
        else reqVarNames.dropRight(1).map(n => q"var $n: Long = -1") :+ q"var ${reqVarNames.last}: Long = $lastReqVarBits"
      val reqFields =
        if (lastReqVarBits == 0) EmptyTree
        else q"private val reqFields: Array[String] = Array(..$required)"
      val checkReqVars = reqVarNames.map(n => q"$n == 0").reduce((e1, e2) => q"$e1 && $e2")
      val members: Seq[MethodSymbol] = tpe.members.collect {
        case m: MethodSymbol if m.isCaseAccessor && nonTransient(m) => m
      }(breakOut).reverse
      val construct = q"new $tpe(..${members.map(m => q"${m.name} = ${TermName(s"_${m.name}")}")})"
      val checkReqVarsAndConstruct =
        if (lastReqVarBits == 0) construct
        else q"if ($checkReqVars) $construct else reqFieldError(in, reqFields, ..$reqVarNames)"
      val readVars = members.map { m =>
        q"var ${TermName(s"_${m.name}")}: ${methodType(m)} = ${defaults.getOrElse(m.name.toString, defaultValue(methodType(m)))}"
      }
      val readFields = members.map { m =>
        val varName = TermName(s"_${m.name}")
        cq"""${hashCode(m)} =>
            ..${bitmasks.getOrElse(m.name.toString, EmptyTree)}
            $varName = ${genReadField(methodType(m), q"$varName")}"""
      } :+ cq"_ => in.skip()"
      val writeFields = members.map { m =>
        val tpe = methodType(m)
        val writeField = genWriteField(q"x.$m", tpe, keyName(m))
        defaults.get(m.name.toString) match {
          case Some(d) => // FIXME: more efficient equals for array required
            if (tpe <:< typeOf[Array[_]]) q"if (x.$m.deep != $d.deep) $writeField"
            else q"if (x.$m != $d) $writeField"
          case None => writeField
        }
      }
      val writeFieldsBlock =
        if (writeFields.isEmpty) EmptyTree
        else q"val x = obj.asInstanceOf[$tpe]; var first = true; ..$writeFields"
      val tree =
        q"""import com.jsoniter.JsonIterator
            import com.jsoniter.JsonIteratorUtil._
            import com.jsoniter.output.JsonStream
            import com.jsoniter.output.JsonStreamUtil._
            new com.github.plokhotnyuk.jsoniter_scala.Codec[$tpe] {
              ..$reqFields
              override def read(in: JsonIterator): $tpe =
                nextToken(in) match {
                  case '{' =>
                    ..$reqVars
                    ..$readVars
                    if (nextToken(in) != '}') {
                      unreadByte(in)
                      do {
                        readObjectFieldAsHash(in) match {
                          case ..$readFields
                        }
                      } while (nextToken(in) == ',')
                    }
                    ..$checkReqVarsAndConstruct
                  case 'n' =>
                    parseNull(in, null)
                  case _ =>
                    decodeError(in, "expect { or n")
                }
              override def write(obj: $tpe, out: JsonStream): Unit =
                if (obj ne null) {
                  out.writeObjectStart()
                  ..$writeFieldsBlock
                  out.writeObjectEnd()
                } else out.writeNull()
              ..${decoders.map { case (_, d) => d._2 }}
              ..${encoders.map { case (_, e) => e._2 }}
            }"""
      if (c.settings.contains("print-codecs")) c.info(c.enclosingPosition, s"Generated codec for type '$tpe':\n${showCode(tree)}", force = true)
      c.Expr[Codec[A]](tree)
    }
  }
}
