package com.github.plokhotnyuk.jsoniter_scala

import scala.annotation.meta.field
import scala.collection.breakOut
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class key(key: String) extends scala.annotation.StaticAnnotation

abstract class JsonCodec[A] {
  def decode(in: JsonReader): A

  def encode(obj: A, out: JsonWriter): Unit
}

object JsonCodec {
  def materialize[A]: JsonCodec[A] = macro Impl.materialize[A]

  private object Impl {
    def materialize[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[JsonCodec[A]] = {
      import c.universe._

      def methodType(m: MethodSymbol): Type = m.returnType.dealias

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs.tail.head.dealias

      def companion(tpe: Type): Tree = Ident(tpe.typeSymbol.companion)

      def isValueClass(tpe: Type): Boolean = tpe <:< typeOf[AnyVal] && tpe.typeSymbol.asClass.isDerivedValueClass

      def isContainer(tpe: Type): Boolean = tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Traversable[_]]

      def valueClassValueType(tpe: Type): Type = methodType(tpe.decls.head.asMethod)

      def defaultValue(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) {
          q"false"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) {
          q"0.toByte"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) {
          q"0.toChar"
        } else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) {
          q"0.toShort"
        } else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) {
          q"0"
        } else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) {
          q"0L"
        } else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) {
          q"0f"
        } else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          q"0.0"
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
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) {
          q"in.readObjectFieldAsBoolean()"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) {
          q"in.readObjectFieldAsByte()"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) {
          q"in.readObjectFieldAsChar()"
        } else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) {
          q"in.readObjectFieldAsShort()"
        } else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) {
          q"in.readObjectFieldAsInt()"
        } else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) {
          q"in.readObjectFieldAsLong()"
        } else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) {
          q"in.readObjectFieldAsFloat()"
        } else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          q"in.readObjectFieldAsDouble()"
        } else if (isValueClass(tpe)) {
          q"new $tpe(${genReadKey(valueClassValueType(tpe))})"
        } else if (tpe =:= typeOf[String]) {
          q"in.readObjectFieldAsString()"
        } else if (tpe =:= typeOf[BigInt]) {
          q"in.readObjectFieldAsBigInt()"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"in.readObjectFieldAsBigDecimal()"
        } else if (tpe <:< typeOf[Enumeration#Value]) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"$enumSymbol.withName(in.readObjectFieldAsString())"
        } else {
          c.abort(c.enclosingPosition, s"Unsupported type to be used as map key '$tpe'.")
        }

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"buf.result()"): Tree =
        q"""(in.nextToken(): @switch) match {
              case '[' =>
                if (in.nextToken() == ']') default
                else {
                  in.unreadByte()
                  ..$newBuilder
                  do {
                    ..$readVal
                  } while (in.nextToken() == ',')
                  ..$result
                }
              case 'n' =>
                in.parseNull(default)
              case _ =>
                in.arrayStartError()
            }"""

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"buf"): Tree =
        q"""(in.nextToken(): @switch) match {
              case '{' =>
                if (in.nextToken() == '}') default
                else {
                  in.unreadByte()
                  ..$newBuilder
                  do {
                    ..$readKV
                  } while (in.nextToken() == ',')
                  ..$result
                }
              case 'n' =>
                in.parseNull(default)
              case _ =>
                in.objectStartError()
            }"""

      val decoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withDecoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree =
        q"""${decoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"d${decoders.size}")
          (name, q"private def $name(in: JsonReader, default: $tpe): $tpe = $impl")})._1}(in, $arg)"""

      val encoders = mutable.LinkedHashMap.empty[Type, (TermName, Tree)]

      def withEncoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree =
        q"""${encoders.getOrElseUpdate(tpe, {
          val impl = f
          val name = TermName(s"e${encoders.size}")
          (name, q"private def $name(out: JsonWriter, x: $tpe): Unit = $impl")})._1}(out, $arg)"""

      def findImplicitCodec(tpe: Type): Option[Tree] = {
        val codecTpe = c.typecheck(tq"com.github.plokhotnyuk.jsoniter_scala.JsonCodec[$tpe]", mode = c.TYPEmode).tpe
        val implCodec = c.inferImplicitValue(codecTpe)
        if (implCodec != EmptyTree) Some(implCodec) else None
      }

      def genReadField(tpe: Type, default: Tree): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) {
          q"in.readBoolean()"
        } else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) {
          q"in.readByte()"
        } else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) {
          q"in.readChar()"
        } else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) {
          q"in.readShort()"
        } else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) {
          q"in.readInt()"
        } else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) {
          q"in.readLong()"
        } else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) {
          q"in.readFloat()"
        } else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          q"in.readDouble()"
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
            q"buf = buf.updated(in.readObjectFieldAsInt(), ${genReadField(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"val buf = $comp.empty[$tpe1]",
            q"buf.update(in.readObjectFieldAsLong(), ${genReadField(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"var buf = $comp.empty[$tpe1]",
            q"buf = buf.updated(in.readObjectFieldAsLong(), ${genReadField(tpe1, defaultValue(tpe1))})")
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
          genReadArray(q"val buf = $comp.empty", q"buf.add(in.readInt())", q"buf")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(tpe, default) {
          val comp = companion(tpe)
          genReadArray(q"val buf = $comp.newBuilder", q"buf += in.readInt()")
        } else if (tpe <:< typeOf[Traversable[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadArray(q"val buf = $comp.newBuilder[$tpe1]", q"buf += ${genReadField(tpe1, defaultValue(tpe1))}")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val buf = collection.mutable.ArrayBuilder.make[$tpe1]",
            q"buf += ${genReadField(tpe1, defaultValue(tpe1))}")
        } else if (tpe =:= typeOf[String]) {
          q"in.readString($default)"
        } else if (tpe =:= typeOf[BigInt]) {
          q"in.readBigInt($default)"
        } else if (tpe =:= typeOf[BigDecimal]) {
          q"in.readBigDecimal($default)"
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(tpe, default) {
          val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
          q"""if (in.nextToken() == 'n') in.parseNull(default)
              else {
                in.unreadByte()
                val v = in.readString()
                try $enumSymbol.withName(v) catch {
                  case _: java.util.NoSuchElementException => in.decodeError("illegal enum value: " + v)
                }
              }"""
        } else withDecoderFor(tpe, default) {
          q"""val x = ${findImplicitCodec(tpe).getOrElse(q"this")}.decode(in)
              if (x ne null) x else default"""
        }

      def genWriteArray(m: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            var c = false
            $m.foreach { x => c = out.writeComma(c); ..$writeVal }
            out.writeArrayEnd()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""out.writeObjectStart()
            var c = false
            $m.foreach { kv => c = out.writeObjectField(c, kv._1); ..$writeKV }
            out.writeObjectEnd()"""

      def genWriteVal(m: Tree, tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] ||
          tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte] ||
          tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] ||
          tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] ||
          tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long] ||
          tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) {
          q"out.writeVal($m)"
        } else if (tpe <:< typeOf[Option[_]]) {
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
              var c = false
              while (i < l) {
                c = out.writeComma(c)
                ..${genWriteVal(q"x(i)", typeArg1(tpe))}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe =:= typeOf[String] || tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal]) {
          q"out.writeVal($m)"
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(tpe, m) {
          q"if (x ne null) out.writeVal(x.toString) else out.writeNull()"
        } else {
          q"${findImplicitCodec(tpe).getOrElse(q"this")}.encode($m, out)"
        }

      def genWriteField(m: Tree, tpe: Type, name: String): Tree =
        if (isValueClass(tpe)) {
          genWriteField(q"$m.value", valueClassValueType(tpe), name)
        } else if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[scala.collection.Map[_, _]] || tpe <:< typeOf[Traversable[_]]) {
          q"if (($m ne null) && !$m.isEmpty) { c = out.writeObjectField(c, $name); ${genWriteVal(m, tpe)} }"
        } else {
          q"c = out.writeObjectField(c, $name); ..${genWriteVal(m, tpe)}"
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

      def hashCode(m: MethodSymbol): Int = {
        val cs = keyName(m).toCharArray
        JsonReader.toHashCode(cs, cs.length)
      }

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
      val required = params.collect { // FIXME should report overridden by annotation keys instead of param names
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
        else reqVarNames.dropRight(1).map(n => q"var $n = -1") :+ q"var ${reqVarNames.last} = $lastReqVarBits"
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
        else q"if ($checkReqVars) $construct else in.reqFieldError(reqFields, ..$reqVarNames)"
      val readVars = members.map { m =>
        q"var ${TermName(s"_${m.name}")}: ${methodType(m)} = ${defaults.getOrElse(m.name.toString, defaultValue(methodType(m)))}"
      }
      val readFields = members.map { m =>
        val varName = TermName(s"_${m.name}")
        cq"""${hashCode(m)} =>
            if (in.isReusableCharsEqualsTo(l, ${keyName(m)})) {
              ..${bitmasks.getOrElse(m.name.toString, EmptyTree)}
              $varName = ${genReadField(methodType(m), q"$varName")}
            } else in.skip()"""
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
        else q"val x = obj.asInstanceOf[$tpe]; var c = false; ..$writeFields"
      val tree =
        q"""import com.github.plokhotnyuk.jsoniter_scala.JsonReader
            import com.github.plokhotnyuk.jsoniter_scala.JsonWriter
            import scala.annotation.switch
            new com.github.plokhotnyuk.jsoniter_scala.JsonCodec[$tpe] {
              ..$reqFields
              override def decode(in: JsonReader): $tpe =
                (in.nextToken(): @switch) match {
                  case '{' =>
                    ..$reqVars
                    ..$readVars
                    if (in.nextToken() != '}') {
                      in.unreadByte()
                      do {
                        val l = in.readObjectFieldAsReusableChars()
                        (in.reusableCharsToHashCode(l): @switch) match {
                          case ..$readFields
                        }
                      } while (in.nextToken() == ',')
                    }
                    ..$checkReqVarsAndConstruct
                  case 'n' =>
                    in.parseNull(null)
                  case _ =>
                    in.objectStartError()
                }
              override def encode(obj: $tpe, out: JsonWriter): Unit =
                if (obj ne null) {
                  out.writeObjectStart()
                  ..$writeFieldsBlock
                  out.writeObjectEnd()
                } else out.writeNull()
              ..${decoders.map { case (_, d) => d._2 }}
              ..${encoders.map { case (_, e) => e._2 }}
            }"""
      if (c.settings.contains("print-codecs")) c.info(c.enclosingPosition, s"Generated JSON codec for type '$tpe':\n${showCode(tree)}", force = true)
      c.Expr[JsonCodec[A]](tree)
    }
  }
}
