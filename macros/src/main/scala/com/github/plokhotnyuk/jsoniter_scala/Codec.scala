package com.github.plokhotnyuk.jsoniter_scala

import java.io.{InputStream, OutputStream}

import com.jsoniter.output.{JsonStream, JsonStreamPool}
import com.jsoniter.spi.JsoniterSpi._
import com.jsoniter.spi.{Config, Decoder, Encoder, JsoniterSpi}
import com.jsoniter.{CodegenAccess, JsonIterator}

import scala.annotation.meta.field
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class key(key: String) extends scala.annotation.StaticAnnotation

abstract class Codec[A](val cls: Class[A]) extends Encoder with Decoder {
  JsoniterSpi.setDefaultConfig((new Config.Builder).escapeUnicode(false).build)
  addNewEncoder(getCurrentConfig.getEncoderCacheKey(cls), this)
  addNewDecoder(getCurrentConfig.getDecoderCacheKey(cls), this)

  def read(in: InputStream, bufSize: Int = 1024): A = JsonIterator.parse(in, bufSize).read(cls)

  def read(buf: Array[Byte]): A = JsonIterator.deserialize(buf, cls)

  def write(obj: A, out: OutputStream): Unit = JsonStream.serialize(obj, out)

  def write(obj: A): Array[Byte] = {
    val stream = JsonStreamPool.borrowJsonStream
    try {
      stream.reset(null)
      stream.writeVal(cls, obj)
      val buf = stream.buffer()
      val out = new Array[Byte](buf.len)
      System.arraycopy(buf.data, 0, out, 0, buf.len)
      out
    } finally JsonStreamPool.returnJsonStream(stream)
  }

  private[jsoniter_scala] def reqFieldError(in: JsonIterator, reqFields: Array[String], req: Long): Nothing = {
    val sb = new StringBuilder(64)
    val l = reqFields.length
    var i = 0
    while (i < l) {
      if ((req & (1L << i)) != 0) {
        sb.append(if (sb.isEmpty) "missing required field(s) " else ", ").append('"').append(reqFields(i)).append('"')
      }
      i += 1
    }
    decodeError(in, sb.toString())
  }

  private[jsoniter_scala] def decodeError(in: JsonIterator, msg: String): Nothing = throw in.reportError("decode", msg)

  private[jsoniter_scala] def writeSep(out: JsonStream, first: Boolean): Boolean = {
    if (first) out.writeIndention()
    else out.writeMore()
    false
  }
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

      def isContainer(tpe: Type): Boolean = tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Iterable[_]]

      def valueClassValueType(tpe: Type): Type = methodType(tpe.decls.head.asMethod)

      def default(tpe: Type): Tree =
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
        } else if (tpe <:< typeOf[Iterable[_]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}]"
        } else {
          q"null"
        }

      def genString2T(tpe: Type, s: Tree): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"$s.toBoolean"
        } else if (tpe =:= definitions.ByteTpe) {
          q"$s.toByte"
        } else if (tpe =:= definitions.CharTpe) {
          q"$s.toInt.toChar"
        } else if (tpe =:= definitions.ShortTpe) {
          q"$s.toShort"
        } else if (tpe =:= definitions.IntTpe) {
          q"$s.toInt"
        } else if (tpe =:= definitions.LongTpe) {
          q"$s.toLong"
        } else if (tpe =:= definitions.DoubleTpe) {
          q"$s.toDouble"
        } else if (tpe =:= definitions.FloatTpe) {
          q"$s.toFloat"
        } else if (isValueClass(tpe)) {
          q"new $tpe(${genString2T(valueClassValueType(tpe), s)})"
        } else if (tpe <:< typeOf[Option[_]]) {
          q"Option(${genString2T(typeArg1(tpe), s)})"
        } else if (tpe =:= typeOf[String]) {
          q"$s"
        } else if (tpe =:= typeOf[BigInt]) {
          q"BigInt($s)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"BigDecimal($s)"
        } else if (tpe <:< typeOf[Enumeration#Value]) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"$enumSymbol.apply($s.toInt)"
        } else {
          q"new $tpe($s)"
        }

      def genReadArray(empty: Tree, newBuilder: Tree, readVal: Tree, result: Tree = q"buf.result()"): Tree =
        q"""nextToken(in) match {
              case '[' =>
                if (nextToken(in) == ']') $empty
                else {
                  unreadByte(in)
                  $newBuilder
                  do {
                    $readVal
                  } while (nextToken(in) == ',')
                  $result
                }
              case 'n' =>
                skipFixedBytes(in, 3)
                $empty
              case _ =>
                decodeError(in, "expect [ or n")
            }"""

      def genReadMap(empty: Tree, newBuilder: Tree, readKV: Tree, result: Tree = q"buf"): Tree =
        q"""nextToken(in) match {
              case '{' =>
                if (nextToken(in) == '}') $empty
                else {
                  unreadByte(in)
                  $newBuilder
                  do {
                    $readKV
                  } while (nextToken(in) == ',')
                  $result
                }
              case 'n' =>
                skipFixedBytes(in, 3)
                $empty
              case _ =>
                decodeError(in, "expect { or n")
            }"""

      def genReadField(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"in.readBoolean()"
        } else if (tpe =:= definitions.ByteTpe) {
          q"in.readInt().toByte"
        } else if (tpe =:= definitions.CharTpe) {
          q"in.readInt().toChar"
        } else if (tpe =:= definitions.ShortTpe) {
          q"in.readInt().toShort"
        } else if (tpe =:= definitions.IntTpe) {
          q"in.readInt()"
        } else if (tpe.widen =:= definitions.LongTpe) {
          q"in.readLong()"
        } else if (tpe =:= definitions.DoubleTpe) {
          q"in.readDouble()"
        } else if (tpe =:= definitions.FloatTpe) {
          q"in.readFloat()"
        } else if (isValueClass(tpe)) {
          q"new $tpe(${genReadField(valueClassValueType(tpe))})"
        } else if (tpe <:< typeOf[Option[_]]) {
          q"Option(${genReadField(typeArg1(tpe))})"
        } else if (tpe <:< typeOf[IntMap[_]]) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"var buf = $comp.empty[$tpe1]",
            q"""buf = buf.updated(readObjectFieldAsString(in).toInt, ${genReadField(tpe1)})""")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"val buf = $comp.empty[$tpe1]",
            q"""buf.update(readObjectFieldAsString(in).toLong, ${genReadField(tpe1)})""")
        } else if (tpe <:< typeOf[LongMap[_]]) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"var buf = $comp.empty[$tpe1]",
            q"""buf = buf.updated(readObjectFieldAsString(in).toLong, ${genReadField(tpe1)})""")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1, $tpe2]", q"val buf = $comp.empty[$tpe1, $tpe2]",
            q"""buf.update(${genString2T(tpe1, q"readObjectFieldAsString(in)")}, ${genReadField(tpe2)})""")
        } else if (tpe <:< typeOf[Map[_, _]]) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1, $tpe2]", q"var buf = $comp.empty[$tpe1, $tpe2]",
            q"""buf = buf.updated(${genString2T(tpe1, q"readObjectFieldAsString(in)")}, ${genReadField(tpe2)})""")
        } else if (tpe <:< typeOf[mutable.BitSet]) {
          val comp = companion(tpe)
          genReadArray(q"$comp.empty", q"val buf = $comp.empty", q"buf.add(in.readInt())", q"buf")
        } else if (tpe <:< typeOf[BitSet]) {
          val comp = companion(tpe)
          genReadArray(q"$comp.empty", q"val buf = $comp.newBuilder", q"buf += in.readInt()")
        } else if (tpe <:< typeOf[Iterable[_]]) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadArray(q"$comp.empty[$tpe1]", q"val buf = $comp.newBuilder[$tpe1]",
            q"buf += ${genReadField(tpe1)}")
        } else if (tpe =:= typeOf[String]) {
          q"in.readString()"
        } else if (tpe =:= typeOf[BigInt]) {
          q"BigInt(in.readBigInteger())"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"BigDecimal(in.readBigDecimal())"
        } else if (tpe <:< typeOf[Enumeration#Value]) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"$enumSymbol.apply(in.readInt())"
        } else {
          q"in.read(classOf[$tpe])"
        }

      def genWriteArray(m: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            var first = true
            $m.foreach { x =>
              first = writeSep(out, first)
              $writeVal
            }
            out.writeArrayEnd()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""out.writeObjectStart()
            var first = true
            $m.foreach { kv =>
              first = writeSep(out, first)
              out.writeObjectField(kv._1.toString)
              $writeKV
            }
            out.writeObjectEnd()"""

      def genWriteVal(m: Tree, tpe: Type): Tree =
        if (tpe <:< definitions.AnyValTpe) {
          q"out.writeVal($m)"
        } else if (tpe <:< typeOf[Option[_]]) {
          genWriteVal(q"$m.get", typeArg1(tpe))
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] || tpe <:< typeOf[LongMap[_]]) {
          genWriteMap(m, genWriteVal(q"kv._2", typeArg1(tpe)))
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          genWriteMap(m, genWriteVal(q"kv._2", typeArg2(tpe)))
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) {
          genWriteArray(m, q"out.writeVal(x)")
        } else if (tpe <:< typeOf[Iterable[_]]) {
          genWriteArray(m, genWriteVal(q"x", typeArg1(tpe)))
        } else if (tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal]) {
          q"out.writeRaw($m.toString)"
        } else if (tpe <:< typeOf[Enumeration#Value]) {
          q"out.writeVal($m.id)"
        } else {
          q"out.writeVal($m)"
        }

      def genWriteField(m: Tree, tpe: Type, name: String): Tree =
        if (isValueClass(tpe)) {
          genWriteField(q"$m.value", valueClassValueType(tpe), name)
        } else if (tpe <:< definitions.AnyValTpe) {
          q"first = writeSep(out, first); out.writeObjectField($name); ..${genWriteVal(m, tpe)}"
        } else if (tpe <:< typeOf[Option[_]]) {
          q"if ($m != null && $m.isDefined) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"if ($m != null && $m.nonEmpty) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else if (tpe <:< typeOf[Iterable[_]]) {
          q"if ($m != null && $m.nonEmpty) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else {
          q"if ($m != null) { first = writeSep(out, first); out.writeObjectField($name); ..${genWriteVal(m, tpe)} }"
        }

      val tpe = weakTypeOf[A]
      if (!tpe.typeSymbol.asClass.isCaseClass) c.abort(c.enclosingPosition, s"'$tpe' must be a case class.")
      val annotations = tpe.members.collect {
        case m: TermSymbol if m.annotations.nonEmpty => (m.getter, m.annotations.toSet)
      }.toMap

      def nonTransient(m: MethodSymbol): Boolean =
        !annotations.get(m).exists(_.exists(_.tree.tpe <:< c.weakTypeOf[transient]))

      def keyName(m: MethodSymbol): String =
        annotations.get(m).flatMap(_.collectFirst { case a if a.tree.tpe <:< c.weakTypeOf[key] =>
          val Literal(Constant(str: String)) = a.tree.children.tail.head
          str
        }).getOrElse(m.name.toString)

      def hashCode(m: MethodSymbol): Int =
        CodegenAccess.readObjectFieldAsHash(JsonIterator.parse(s""""${keyName(m)}":""".getBytes("UTF-8")))

      // FIXME: module cannot be resolved properly for deeply nested inner case classes
      val comp = tpe.typeSymbol.companion
      if (!comp.isModule) c.abort(c.enclosingPosition,
        s"Can't find companion object for '$tpe'. This can happen when it's nested too deeply. " +
          "Please consider defining it as a top-level object or directly inside of another class or object.")
      val module = comp.asModule
      val apply = module.typeSignature.decl(TermName("apply")).asMethod
      // FIXME: handling only default val params from the first list because subsequent might depend on previous params
      val params = apply.paramLists.head.map(_.asTerm)
      val defaults = params.zipWithIndex.collect {
        case (p, i) if p.isParamWithDefault => p.name.toString -> q"$module.${TermName("apply$default$" + (i + 1))}"
      }.toMap
      val required = params.collect {
        case p if !p.isParamWithDefault && !isContainer(p.typeSignature) => p.name.toString
      }
      if (required.size > 64) c.abort(c.enclosingPosition, s"More than 64 required fields in: '$tpe'.")
      val members = tpe.members.collect {
        case m: MethodSymbol if m.isCaseAccessor && nonTransient(m) => m
      }.toSeq.reverse
      val readVars = members.map { m =>
        q"var ${TermName(s"_${m.name}")}: ${methodType(m)} = ${defaults.getOrElse(m.name.toString, default(methodType(m)))}"
      }
      val bitmasks = required.zipWithIndex.map { case (r, i) => (r, q"req &= ${~(1L << i)}")}.toMap
      val readFields = members.map { m =>
        val bitmask = bitmasks.getOrElse(m.name.toString, EmptyTree)
        cq"${hashCode(m)} => $bitmask; ${TermName(s"_${m.name}")} = ${genReadField(methodType(m))}"
      } :+ cq"_ => in.skip()"
      val readParams = members.map(m => q"${m.name} = ${TermName(s"_${m.name}")}")
      val writeFields = members.map { m =>
        val writeField = genWriteField(q"x.$m", methodType(m), keyName(m))
        defaults.get(m.name.toString) match {
          case Some(d) => q"if (x.$m != $d) $writeField"
          case None => writeField
        }
      }
      val tree =
        q"""import com.jsoniter.CodegenAccess._

            new com.github.plokhotnyuk.jsoniter_scala.Codec[$tpe](classOf[$tpe]) {
              private val reqFields: Array[String] = Array(..$required)

              override def decode(in: com.jsoniter.JsonIterator): AnyRef =
                nextToken(in) match {
                  case '{' =>
                    ..$readVars
                    var req = ${(1L << required.size) - 1}
                    if (nextToken(in).!=('}')) {
                      unreadByte(in)
                      do {
                        readObjectFieldAsHash(in) match {
                          case ..$readFields
                        }
                      } while (nextToken(in) == ',')
                    }
                    if (req == 0) new $tpe(..$readParams)
                    else reqFieldError(in, reqFields, req)
                  case 'n' =>
                    skipFixedBytes(in, 3)
                    null
                  case _ =>
                    decodeError(in, "expect { or n")
                }

              override def encode(obj: AnyRef, out: com.jsoniter.output.JsonStream): Unit = {
                out.writeObjectStart()
                val x = obj.asInstanceOf[$tpe]
                var first = true
                ..$writeFields
                out.writeObjectEnd()
              }
            }"""
      if (c.settings.contains("print-codecs")) c.info(c.enclosingPosition, s"Generated codec for type '$tpe':\n${showCode(tree)}", force = true)
      c.Expr[Codec[A]](tree)
    }
  }
}
