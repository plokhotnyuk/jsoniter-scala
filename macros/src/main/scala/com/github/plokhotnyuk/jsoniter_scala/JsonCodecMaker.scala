package com.github.plokhotnyuk.jsoniter_scala

import java.lang.Character._

import scala.annotation.StaticAnnotation
import scala.annotation.meta.field
import scala.collection.immutable.{BitSet, IntMap, LongMap}
import scala.collection.{breakOut, mutable}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@field
class named(name: String) extends StaticAnnotation

@field
class transient extends StaticAnnotation

case class CodecMakerConfig(
    nameMapper: String => String = identity,
    descriptorFieldName: String = "type",
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

  def make[A](config: CodecMakerConfig): JsonCodec[A] = macro Impl.make[A]

  private object Impl {
    def make[A: c.WeakTypeTag](c: blackbox.Context)(config: c.Expr[CodecMakerConfig]): c.Expr[JsonCodec[A]] = {
      import c.universe._

      def methodType(m: MethodSymbol): Type = m.returnType.dealias

      def typeArg1(tpe: Type): Type = tpe.typeArgs.head.dealias

      def typeArg2(tpe: Type): Type = tpe.typeArgs.tail.head.dealias

      def companion(tpe: Type): Tree = Ident(tpe.typeSymbol.companion)

      def isValueClass(tpe: Type): Boolean = tpe <:< typeOf[AnyVal] && tpe.typeSymbol.asClass.isDerivedValueClass

      def valueClassValueType(tpe: Type): Type = methodType(tpe.decls.head.asMethod)

      def isContainer(tpe: Type): Boolean =
        tpe <:< typeOf[Option[_]] || tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[Array[_]]

      def enumSymbol(tpe: Type): Symbol = {
        val TypeRef(SingleType(_, enumSymbol), _, _) = tpe
        enumSymbol
      }

      def genReadKey(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readObjectFieldAsBoolean()"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"in.readObjectFieldAsByte()"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readObjectFieldAsChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"in.readObjectFieldAsShort()"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"in.readObjectFieldAsInt()"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"in.readObjectFieldAsLong()"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"in.readObjectFieldAsFloat()"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"in.readObjectFieldAsDouble()"
        else if (isValueClass(tpe)) q"new $tpe(${genReadKey(valueClassValueType(tpe))})"
        else if (tpe =:= typeOf[String]) q"in.readObjectFieldAsString()"
        else if (tpe =:= typeOf[BigInt]) q"in.readObjectFieldAsBigInt()"
        else if (tpe =:= typeOf[BigDecimal]) q"in.readObjectFieldAsBigDecimal()"
        else if (tpe <:< typeOf[Enumeration#Value]) q"${enumSymbol(tpe)}.withName(in.readObjectFieldAsString())"
        else c.abort(c.enclosingPosition, s"Unsupported type to be used as map key '$tpe'.")

      def genReadArray(newBuilder: Tree, readVal: Tree, result: Tree = q"x"): Tree =
        genReadCollection(newBuilder, readVal, result, q"'['", q"']'", q"in.arrayStartError()", q"in.arrayEndError()")

      def genReadMap(newBuilder: Tree, readKV: Tree, result: Tree = q"x"): Tree =
        genReadCollection(newBuilder, readKV, result, q"'{'", q"'}'", q"in.objectStartError()", q"in.objectEndError()")

      def genReadCollection(newBuilder: Tree, loopBody: Tree, result: Tree,
                            open: Tree, close: Tree, startError: Tree, endError: Tree): Tree =
        q"""(in.nextToken(): @switch) match {
              case $open =>
                if (in.nextToken() == $close) default
                else {
                  in.unreadByte()
                  ..$newBuilder
                  do {
                    ..$loopBody
                  } while (in.nextToken() == ',')
                  in.unreadByte()
                  if (in.nextToken() == $close) $result
                  else $endError
                }
              case 'n' =>
                in.parseNull(default)
              case _ =>
                ..$startError
            }"""

      def genWriteArray(m: Tree, writeVal: Tree): Tree =
        q"""out.writeArrayStart()
            var c = false
            $m.foreach { x =>
              c = out.writeComma(c)
              ..$writeVal
            }
            out.writeArrayEnd()"""

      def genWriteMap(m: Tree, writeKV: Tree): Tree =
        q"""out.writeObjectStart()
            var c = false
            $m.foreach { kv =>
              c = out.writeObjectField(c, kv._1)
              ..$writeKV
            }
            out.writeObjectEnd()"""

      def cannotFindCodecError(tpe: Type): Nothing =
        c.abort(c.enclosingPosition, s"No implicit '${typeOf[JsonCodec[_]]}' defined for '$tpe'.")

      def findImplicitCodec(tpe: Type): Tree = c.inferImplicitValue(c.typecheck(tq"JsonCodec[$tpe]", c.TYPEmode).tpe)

      case class FieldAnnotations(name: String, transient: Boolean)

      def getFieldAnnotations(tpe: Type): Map[String, FieldAnnotations] = tpe.members.collect {
        case m: TermSymbol if m.annotations.exists(a => a.tree.tpe <:< c.weakTypeOf[named] ||
                              a.tree.tpe <:< c.weakTypeOf[transient]) =>
          val fieldName = m.name.toString.trim // FIXME: Why is there a space at the end of field name?!
          val named = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[named])
          if (named.size > 1) {
            c.abort(c.enclosingPosition, s"Duplicated '${typeOf[named]}' defined for '$fieldName' of '$tpe'.")
          }
          val trans = m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[transient])
          if (trans.size > 1) {
            c.warning(c.enclosingPosition, s"Duplicated '${typeOf[transient]}' defined for '$fieldName' of '$tpe'.")
          } else if (named.size == 1 && trans.size == 1) {
            c.warning(c.enclosingPosition, s"Both '${typeOf[transient]}' and '${typeOf[named]}' defined for '$fieldName' of '$tpe'.")
          }
          val name = named.headOption.flatMap(_.tree.children.tail.collectFirst {
            case Literal(Constant(name: String)) => Option(name).getOrElse(fieldName)
          }).getOrElse(fieldName)
          (fieldName, FieldAnnotations(name, trans.nonEmpty))
      }(breakOut)

      def getModule(tpe: Type): ModuleSymbol = {
        val comp = tpe.typeSymbol.companion
        if (!comp.isModule) c.abort(c.enclosingPosition,
          s"Can't find companion object for '$tpe'. This can happen when it's nested too deeply. " +
            "Please consider defining it as a top-level object or directly inside of another class or object.")
        comp.asModule // FIXME: module cannot be resolved properly for deeply nested inner case classes
      }

      // FIXME: handling only default val params from the first list because subsequent might depend on previous params
      def getParams(module: ModuleSymbol): Seq[TermSymbol] =
        module.typeSignature.decl(TermName("apply")).asMethod.paramLists.head.map(_.asTerm)

      def getDefaults(tpe: Type): Map[String, Tree] = {
        val module = getModule(tpe)
        getParams(module).zipWithIndex.collect {
          case (p, i) if p.isParamWithDefault => (p.name.toString, q"$module.${TermName("apply$default$" + (i + 1))}")
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
        else q"in.unexpectedFieldError(l)"

      def getMappedName(annotations: Map[String, FieldAnnotations], defaultName: String): String =
        annotations.get(defaultName).fold(codecConfig.nameMapper(defaultName))(_.name)

      val defaultValueNames = mutable.LinkedHashMap.empty[Type, TermName]
      val defaultValueTrees = mutable.LinkedHashMap.empty[Type, Tree]

      // use it only for immutable values which doesn't have public constants
      def withDefaultValueFor(tpe: Type)(f: => Tree): Tree = {
        val defaultValueName = defaultValueNames.getOrElseUpdate(tpe, TermName("v" + defaultValueNames.size))
        defaultValueTrees.getOrElseUpdate(tpe, {
          val impl = f
          q"private val $defaultValueName: $tpe = $impl"
        })
        q"$defaultValueName"
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

      val decodeMethodNames = mutable.LinkedHashMap.empty[Type, TermName]
      val decodeMethodTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withDecoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree = {
        val decodeMethodName = decodeMethodNames.getOrElseUpdate(tpe, TermName("d" + decodeMethodNames.size))
        decodeMethodTrees.getOrElseUpdate(tpe, {
          val impl = f
          q"private def $decodeMethodName(in: JsonReader, default: $tpe): $tpe = $impl"
        })
        q"$decodeMethodName(in, $arg)"
      }

      val encodeMethodNames = mutable.LinkedHashMap.empty[Type, TermName]
      val encodeMethodTrees = mutable.LinkedHashMap.empty[Type, Tree]

      def withEncoderFor(tpe: Type, arg: Tree)(f: => Tree): Tree = {
        val encodeMethodName = encodeMethodNames.getOrElseUpdate(tpe, TermName("e" + encodeMethodNames.size))
        encodeMethodTrees.getOrElseUpdate(tpe, {
          val impl = f
          q"private def $encodeMethodName(x: $tpe, out: JsonWriter): Unit = $impl"
        })
        q"$encodeMethodName($arg, out)"
      }

      def defaultValue(tpe: Type): Tree =
        if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"false"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"0.toByte"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"0.toChar"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"0.toShort"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"0"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"0L"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"0f"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"0.0"
        else if (isValueClass(tpe)) q"null.asInstanceOf[$tpe]"
        else if (tpe <:< typeOf[Option[_]]) q"None"
        else if (tpe <:< typeOf[IntMap[_]] || tpe <:< typeOf[LongMap[_]] || tpe <:< typeOf[mutable.LongMap[_]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}]"
        } else if (tpe <:< typeOf[scala.collection.Map[_, _]]) {
          q"${companion(tpe)}.empty[${typeArg1(tpe)}, ${typeArg2(tpe)}]"
        } else if (tpe <:< typeOf[mutable.BitSet] || tpe <:< typeOf[BitSet]) q"${companion(tpe)}.empty"
        else if (tpe <:< typeOf[Traversable[_]]) q"${companion(tpe)}.empty[${typeArg1(tpe)}]"
        else if (tpe <:< typeOf[Array[_]]) withDefaultValueFor(tpe) {
          q"new Array[${typeArg1(tpe)}](0)"
        } else q"null"

      def genReadVal(tpe: Type, default: Tree, extraFields: Tree = EmptyTree): Tree = {
        val implCodec = findImplicitCodec(tpe) // FIXME: add testing that implicit codecs should override any defaults
        val decodeMethodName = decodeMethodNames.get(tpe)
        if (implCodec != EmptyTree) q"$implCodec.decode(in, $default)"
        else if (decodeMethodName.isDefined) q"${decodeMethodName.get}(in, $default)" // to avoid stack overflow during generation for recursive structures
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean]) q"in.readBoolean()"
        else if (tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte]) q"in.readByte()"
        else if (tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character]) q"in.readChar()"
        else if (tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short]) q"in.readShort()"
        else if (tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer]) q"in.readInt()"
        else if (tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long]) q"in.readLong()"
        else if (tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float]) q"in.readFloat()"
        else if (tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double]) q"in.readDouble()"
        else if (tpe =:= typeOf[String]) q"in.readString($default)"
        else if (tpe =:= typeOf[BigInt]) q"in.readBigInt($default)"
        else if (tpe =:= typeOf[BigDecimal]) q"in.readBigDecimal($default)"
        else if (isValueClass(tpe)) {
          val tpe1 = valueClassValueType(tpe)
          q"new $tpe(${genReadVal(tpe1, defaultValue(tpe1))})"
        } else if (tpe <:< typeOf[Option[_]]) {
          val tpe1 = typeArg1(tpe)
          q"Option(${genReadVal(tpe1, defaultValue(tpe1))})"
        } else if (tpe <:< typeOf[IntMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1]",
            q"x = x.updated(in.readObjectFieldAsInt(), ${genReadVal(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[mutable.LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1]",
            q"x.update(in.readObjectFieldAsLong(), ${genReadVal(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[LongMap[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1]",
            q"x = x.updated(in.readObjectFieldAsLong(), ${genReadVal(tpe1, defaultValue(tpe1))})")
        } else if (tpe <:< typeOf[mutable.Map[_, _]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"val x = if (default.isEmpty) default else $comp.empty[$tpe1, $tpe2]",
            q"x.update(${genReadKey(tpe1)}, ${genReadVal(tpe2, defaultValue(tpe2))})")
        } else if (tpe <:< typeOf[Map[_, _]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val tpe2 = typeArg2(tpe)
          val comp = companion(tpe)
          genReadMap(q"var x = $comp.empty[$tpe1, $tpe2]",
            q"x = x.updated(${genReadKey(tpe1)}, ${genReadVal(tpe2, defaultValue(tpe2))})")
        } else if (tpe <:< typeOf[mutable.BitSet]) withDecoderFor(tpe, default) {
          val comp = companion(tpe)
          genReadArray(q"val x = if (default.isEmpty) default else $comp.empty", q"x.add(in.readInt())")
        } else if (tpe <:< typeOf[BitSet]) withDecoderFor(tpe, default) {
          val comp = companion(tpe)
          genReadArray(q"val x = $comp.newBuilder", q"x += in.readInt()", q"x.result()")
        } else if (tpe <:< typeOf[Traversable[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          val comp = companion(tpe)
          genReadArray(q"val x = $comp.newBuilder[$tpe1]", q"x += ${genReadVal(tpe1, defaultValue(tpe1))}", q"x.result()")
        } else if (tpe <:< typeOf[Array[_]]) withDecoderFor(tpe, default) {
          val tpe1 = typeArg1(tpe)
          genReadArray(q"val x = collection.mutable.ArrayBuilder.make[$tpe1]",
            q"x += ${genReadVal(tpe1, defaultValue(tpe1))}", q"x.result()")
        } else if (tpe <:< typeOf[Enumeration#Value]) withDecoderFor(tpe, default) {
          q"""val v = in.readString()
              if (v ne null) {
                try ${enumSymbol(tpe)}.withName(v) catch {
                  case _: NoSuchElementException => in.decodeError("illegal enum value: \"" + v + "\"")
                }
              } else default"""
        } else if (tpe.typeSymbol.asClass.isCaseClass) withDecoderFor(tpe, default) {
          val annotations = getFieldAnnotations(tpe)

          def name(m: MethodSymbol): String = getMappedName(annotations, m.name.toString)

          def hashCode(m: MethodSymbol): Int = {
            val cs = name(m).toCharArray
            JsonReader.toHashCode(cs, cs.length)
          }

          val members = getMembers(annotations, tpe)
          val params = getParams(getModule(tpe))
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
            else reqVarNames.dropRight(1).map(n => q"var $n = -1") :+ q"var ${reqVarNames.last} = $lastReqVarBits"
          val checkReqVars = reqVarNames.map(n => q"$n == 0").reduce((e1, e2) => q"$e1 && $e2")
          val construct = q"new $tpe(..${members.map(m => q"${m.name} = ${TermName(s"_${m.name}")}")})"
          val checkReqVarsAndConstruct =
            if (lastReqVarBits == 0) construct
            else {
              val reqFieldNames = withReqFieldsFor(tpe) {
                required.map(r => getMappedName(annotations, r))
              }
              q"""if ($checkReqVars) $construct
                  else in.reqFieldError($reqFieldNames, ..$reqVarNames)"""
            }
          val defaults = getDefaults(tpe)
          val readVars = members.map { m =>
            val tpe = methodType(m)
            q"var ${TermName(s"_${m.name}")}: $tpe = ${defaults.getOrElse(m.name.toString, defaultValue(tpe))}"
          }
          val readFields = groupByOrdered(members)(hashCode).map { case (hashCode, ms) =>
            val checkNameAndReadValue = ms.foldRight(unexpectedFieldHandler) { case (m, acc) =>
              val varName = TermName(s"_${m.name}")
              val readValue = q"$varName = ${genReadVal(methodType(m), q"$varName")}"
              val resetReqFieldFlag = bitmasks.getOrElse(m.name.toString, EmptyTree)
              q"""if (in.isCharBufEqualsTo(l, ${name(m)})) {
                    ..$readValue
                    ..$resetReqFieldFlag
                  } else $acc"""
            }
            cq"$hashCode => $checkNameAndReadValue"
          }(breakOut)
          val readFieldsBlock =
            (if (extraFields == EmptyTree) readFields
            else readFields :+ extraFields) :+ cq"_ => $unexpectedFieldHandler"
          q"""(in.nextToken(): @switch) match {
                case '{' =>
                  ..$reqVars
                  ..$readVars
                  if (in.nextToken() != '}') {
                    in.unreadByte()
                    do {
                      val l = in.readObjectFieldAsCharBuf()
                      (in.charBufToHashCode(l): @switch) match {
                        case ..$readFieldsBlock
                      }
                    } while (in.nextToken() == ',')
                    in.unreadByte()
                    if (in.nextToken() != '}') in.objectEndError()
                  }
                  ..$checkReqVarsAndConstruct
                case 'n' =>
                  in.parseNull(default)
                case _ =>
                  in.objectStartError()
              }"""
        } else if (tpe.typeSymbol.asClass.isTrait) withDecoderFor(tpe, default) {
          val readSubclasses = tpe.typeSymbol.asClass.knownDirectSubclasses.map { s =>
            val tpe = s.asClass.toType
            val annotations = getFieldAnnotations(tpe)

            getMembers(annotations, tpe).foreach { m =>
              if (getMappedName(annotations, m.name.toString) == codecConfig.descriptorFieldName) {
                c.abort(c.enclosingPosition, s"'$tpe' define JSON name that matches with descriptor field name.")
              }
            }

            val typeName = tpe.toString
            val descriptorFieldNameHash = {
              val cs = codecConfig.descriptorFieldName.toCharArray
              JsonReader.toHashCode(cs, cs.length)
            }
            val extraFields = cq"$descriptorFieldNameHash => in.skip()"
            cq"""$typeName =>
                   in.rollbackToMark()
                   ${genReadVal(tpe, defaultValue(tpe), extraFields)}"""
          }
          val illegalDescriptorError = s"""illegal value of descriptor field "${codecConfig.descriptorFieldName}""""
          q"""in.setMark()
              (in.nextToken(): @switch) match {
                case '{' =>
                  in.scanToField(${codecConfig.descriptorFieldName})
                  in.readString(null) match {
                    case ..$readSubclasses
                    case _ => in.decodeError($illegalDescriptorError)
                  }
                case 'n' =>
                  in.resetMark()
                  in.parseNull(default)
                case _ =>
                  in.objectStartError()
              }"""
        } else cannotFindCodecError(tpe)
      }

      def genWriteVal(m: Tree, tpe: Type, extraFields: Tree = EmptyTree): Tree = {
        val implCodec = findImplicitCodec(tpe) // FIXME: add testing that implicit codecs should override any defaults
        val encodeMethodName = encodeMethodNames.get(tpe)
        if (implCodec != EmptyTree) q"$implCodec.encode($m, out)"
        else if (encodeMethodName.isDefined) q"${encodeMethodName.get}($m, out)" // to avoid stack overflow during generation for recursive structures
        else if (tpe =:= definitions.BooleanTpe || tpe =:= typeOf[java.lang.Boolean] ||
          tpe =:= definitions.ByteTpe || tpe =:= typeOf[java.lang.Byte] ||
          tpe =:= definitions.CharTpe || tpe =:= typeOf[java.lang.Character] ||
          tpe =:= definitions.ShortTpe || tpe =:= typeOf[java.lang.Short] ||
          tpe =:= definitions.IntTpe || tpe =:= typeOf[java.lang.Integer] ||
          tpe =:= definitions.LongTpe || tpe =:= typeOf[java.lang.Long] ||
          tpe =:= definitions.FloatTpe || tpe =:= typeOf[java.lang.Float] ||
          tpe =:= definitions.DoubleTpe || tpe =:= typeOf[java.lang.Double] ||
          tpe =:= typeOf[String] || tpe =:= typeOf[BigInt] || tpe =:= typeOf[BigDecimal]) q"out.writeVal($m)"
        else if (isValueClass(tpe)) genWriteVal(q"$m.value", valueClassValueType(tpe))
        else if (tpe <:< typeOf[Option[_]]) withEncoderFor(tpe, m) {
          q"if (x.isEmpty) out.writeNull() else ${genWriteVal(q"x.get", typeArg1(tpe))}"
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
              while (i < l) {
                out.writeComma(i != 0)
                ..${genWriteVal(q"x(i)", typeArg1(tpe))}
                i += 1
              }
              out.writeArrayEnd()"""
        } else if (tpe <:< typeOf[Enumeration#Value]) withEncoderFor(tpe, m) {
          q"if (x ne null) out.writeVal(x.toString) else out.writeNull()"
        } else if (tpe.typeSymbol.asClass.isCaseClass) withEncoderFor(tpe, m) {
          val annotations = getFieldAnnotations(tpe)
          val members = getMembers(annotations, tpe)
          val defaults = getDefaults(tpe)
          val writeFields = extraFields +: members.map { m =>
            val tpe = methodType(m)
            val name = getMappedName(annotations, m.name.toString)
            defaults.get(m.name.toString) match {
              case Some(d) =>
                if (tpe <:< typeOf[Array[_]]) {
                  q"""val v = x.$m
                      if ((v ne null) && v.length > 0 && {
                          val d = $d
                          v.length != d.length && v.deep != d.deep
                        }) {
                        c = out.writeObjectField(c, $name)
                        ..${genWriteVal(q"v", tpe)}
                      }"""
                } else if (isContainer(tpe)) {
                  q"""val v = x.$m
                      if ((v ne null) && !v.isEmpty && v != $d) {
                        c = out.writeObjectField(c, $name)
                        ..${genWriteVal(q"v", tpe)}
                      }"""
                } else {
                  q"""val v = x.$m
                      if (v != $d) {
                        c = out.writeObjectField(c, $name)
                        ..${genWriteVal(q"v", tpe)}
                      }"""
                }
              case None =>
                if (tpe <:< typeOf[Array[_]]) {
                  q"""val v = x.$m
                      if ((v ne null) && v.length > 0) {
                        c = out.writeObjectField(c, $name)
                        ..${genWriteVal(q"v", tpe)}
                      }"""
                } else if (isContainer(tpe)) {
                  q"""val v = x.$m
                      if ((v ne null) && !v.isEmpty) {
                        c = out.writeObjectField(c, $name)
                        ..${genWriteVal(q"v", tpe)}
                      }"""
                } else {
                  q"""c = out.writeObjectField(c, $name)
                      ..${genWriteVal(q"x.$m", tpe)}"""
                }
            }
          }
          val writeFieldsBlock =
            if (writeFields.isEmpty) EmptyTree
            else {
              q"""var c = false
                  ..$writeFields"""
            }
          q"""if (x != null) {
                out.writeObjectStart()
                ..$writeFieldsBlock
                out.writeObjectEnd()
              } else out.writeNull()"""
        } else if (tpe.typeSymbol.asClass.isTrait) withEncoderFor(tpe, m) {
          val writeSubclasses = tpe.typeSymbol.asClass.knownDirectSubclasses.map { s =>
            val tpe = s.asClass.toType
            val typeName = tpe.toString
            val extraFields =
              q"""c = out.writeObjectField(c, "type")
                  out.writeVal($typeName)"""
            cq"x: $tpe => ${genWriteVal(q"x", tpe, extraFields)}"
          }
          q"""x match {
                case ..$writeSubclasses
                case null => out.writeNull()
                case _ => ()
              }"""
        } else cannotFindCodecError(tpe)
      }

      val codec =
        q"""import com.github.plokhotnyuk.jsoniter_scala._
            import scala.annotation.switch
            new JsonCodec[$rootTpe] {
              def default: $rootTpe = ${defaultValue(rootTpe)}
              def decode(in: JsonReader, default: $rootTpe): $rootTpe = ${genReadVal(rootTpe, q"default")}
              def encode(x: $rootTpe, out: JsonWriter): Unit = ${genWriteVal(q"x", rootTpe)}
              ..${defaultValueTrees.values}
              ..${reqFieldTrees.values}
              ..${decodeMethodTrees.values}
              ..${encodeMethodTrees.values}
            }"""
      if (c.settings.contains("print-codecs")) {
        val msg = s"Generated JSON codec for type '$rootTpe':\n${showCode(codec)}"
        c.info(c.enclosingPosition, msg, force = true)
      }
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