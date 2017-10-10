package com.github.plokhotnyuk.jsoniter_scala

import com.jsoniter.{CodecBase, JsonIterator}

import scala.annotation.meta.field
import scala.collection.breakOut
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class key(key: String) extends scala.annotation.StaticAnnotation

abstract class Codec[A](implicit m: Manifest[A]) extends CodecBase[A]

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

      def genReadKey(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe) {
          q"readObjectFieldAsBoolean(in)"
        } else if (tpe =:= definitions.ByteTpe) {
          q"readObjectFieldAsInt(in).toByte"
        } else if (tpe =:= definitions.CharTpe) {
          q"readObjectFieldAsInt(in).toChar"
        } else if (tpe =:= definitions.ShortTpe) {
          q"readObjectFieldAsInt(in).toShort"
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
        } else if (tpe <:< typeOf[Option[_]]) {
          q"Option(${genReadKey(typeArg1(tpe))})"
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
          q"new $tpe(readObjectFieldAsString(in))"
        }

      def genReadArray(empty: Tree, newBuilder: Tree, readVal: Tree, result: Tree = q"buf.result()"): Tree =
        q"""nextToken(in) match {
              case '[' =>
                if (nextToken(in) == ']') $empty
                else {
                  unreadByte(in)
                  ..$newBuilder
                  do {
                    ..$readVal
                  } while (nextToken(in) == ',')
                  ..$result
                }
              case 'n' =>
                skipFixedBytes(in, 3)
                ..$empty
              case _ =>
                decodeError(in, "expect [ or n")
            }"""

      def genReadMap(empty: Tree, newBuilder: Tree, readKV: Tree, result: Tree = q"buf"): Tree =
        q"""nextToken(in) match {
              case '{' =>
                if (nextToken(in) == '}') $empty
                else {
                  unreadByte(in)
                  ..$newBuilder
                  do {
                    ..$readKV
                  } while (nextToken(in) == ',')
                  ..$result
                }
              case 'n' =>
                skipFixedBytes(in, 3)
                ..$empty
              case _ =>
                decodeError(in, "expect { or n")
            }"""

      val decoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withDecoderFor(tpe: Type)(f: => Tree): Tree =
        q"""${decoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"d${decoders.size}")
          (name, q"private def $name(in: JsonIterator): $tpe = $impl")})._1}(in)"""

      val encoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withEncoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree =
        q"""${encoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"e${encoders.size}")
          (name, q"private def $name(out: JsonStream, x: $tpe): Unit = $impl")})._1}(out, $arg)"""

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
        } else if (tpe <:< typeOf[IntMap[_]]) withDecoderFor(tpe) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"var buf = $comp.empty[$tpe1]",
            q"buf = buf.updated(readObjectFieldAsInt(in), ${genReadField(tpe1)})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(tpe) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"val buf = $comp.empty[$tpe1]",
            q"buf.update(readObjectFieldAsLong(in), ${genReadField(tpe1)})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(tpe) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1]", q"var buf = $comp.empty[$tpe1]",
            q"buf = buf.updated(readObjectFieldAsLong(in), ${genReadField(tpe1)})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(tpe) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1, $tpe2]", q"val buf = $comp.empty[$tpe1, $tpe2]",
            q"buf.update(${genReadKey(tpe1)}, ${genReadField(tpe2)})")
        } else if (tpe <:< typeOf[Map[_, _]]) withDecoderFor(tpe) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"$comp.empty[$tpe1, $tpe2]", q"var buf = $comp.empty[$tpe1, $tpe2]",
            q"buf = buf.updated(${genReadKey(tpe1)}, ${genReadField(tpe2)})")
        } else if (tpe <:< typeOf[mutable.BitSet]) withDecoderFor(tpe) {
          val comp = companion(tpe)
          genReadArray(q"$comp.empty", q"val buf = $comp.empty", q"buf.add(in.readInt())", q"buf")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(tpe) {
          val comp = companion(tpe)
          genReadArray(q"$comp.empty", q"val buf = $comp.newBuilder", q"buf += in.readInt()")
        } else if (tpe <:< typeOf[Iterable[_]]) withDecoderFor(tpe) {
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
            $m.foreach { x => first = writeSep(out, first); ..$writeVal }
            out.writeArrayEnd()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""out.writeObjectStart()
            var first = true
            $m.foreach { kv => first = writeSep(out, first); writeObjectField(out, kv._1); ..$writeKV }
            out.writeObjectEnd()"""

      def genWriteVal(m: Tree, tpe: Type): Tree =
        if (tpe <:< definitions.AnyValTpe) {
          q"out.writeVal($m)"
        } else if (tpe <:< typeOf[Option[_]]) {
          genWriteVal(q"$m.get", typeArg1(tpe))
        } else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[mutable.LongMap[_]] || tpe <:< typeOf[LongMap[_]]) withEncoderFor(tpe, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg1(tpe)))
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) withEncoderFor(tpe, m) {
          genWriteMap(q"x", genWriteVal(q"kv._2", typeArg2(tpe)))
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) withEncoderFor(tpe, m) {
          genWriteArray(q"x", q"out.writeVal(x)")
        } else if (tpe <:< typeOf[Iterable[_]]) withEncoderFor(tpe, m) {
          genWriteArray(q"x", genWriteVal(q"x", typeArg1(tpe)))
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
          q"if (($m ne null) && $m.isDefined) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"if (($m ne null) && $m.nonEmpty) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else if (tpe <:< typeOf[Iterable[_]]) {
          q"if (($m ne null) && $m.nonEmpty) { first = writeSep(out, first); out.writeObjectField($name); ${genWriteVal(m, tpe)} }"
        } else {
          q"if ($m ne null) { first = writeSep(out, first); out.writeObjectField($name); ..${genWriteVal(m, tpe)} }"
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
        CodecBase.readObjectFieldAsHash(JsonIterator.parse(s""""${keyName(m)}":""".getBytes("UTF-8")))

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
        q"var ${TermName(s"_${m.name}")}: ${methodType(m)} = ${defaults.getOrElse(m.name.toString, default(methodType(m)))}"
      }
      val readFields = members.map { m =>
        cq"""${hashCode(m)} =>
            ..${bitmasks.getOrElse(m.name.toString, EmptyTree)}
            ${TermName(s"_${m.name}")} = ${genReadField(methodType(m))}"""
      } :+ cq"_ => in.skip()"
      val writeFields = members.map { m =>
        val writeField = genWriteField(q"x.$m", methodType(m), keyName(m))
        defaults.get(m.name.toString) match {
          case Some(d) => q"if (x.$m != $d) $writeField"
          case None => writeField
        }
      }
      val tree =
        q"""import com.jsoniter.CodegenAccess._
            import com.jsoniter.CodecBase
            import com.jsoniter.JsonIterator
            import com.jsoniter.output.JsonStream
            new com.github.plokhotnyuk.jsoniter_scala.Codec[$tpe] {
              ..$reqFields
              override def decode(in: JsonIterator): AnyRef =
                nextToken(in) match {
                  case '{' =>
                    ..$reqVars
                    ..$readVars
                    if (nextToken(in).!=('}')) {
                      unreadByte(in)
                      do {
                        CodecBase.readObjectFieldAsHash(in) match {
                          case ..$readFields
                        }
                      } while (nextToken(in) == ',')
                    }
                    ..$checkReqVarsAndConstruct
                  case 'n' =>
                    skipFixedBytes(in, 3)
                    null
                  case _ =>
                    decodeError(in, "expect { or n")
                }
              override def encode(obj: AnyRef, out: JsonStream): Unit = {
                out.writeObjectStart()
                val x = obj.asInstanceOf[$tpe]
                var first = true
                ..$writeFields
                out.writeObjectEnd()
              }
              ..${decoders.map { case (_, d) => d._2 }}
              ..${encoders.map { case (_, e) => e._2 }}
            }"""
      if (c.settings.contains("print-codecs")) c.info(c.enclosingPosition, s"Generated codec for type '$tpe':\n${showCode(tree)}", force = true)
      c.Expr[Codec[A]](tree)
    }
  }
}
