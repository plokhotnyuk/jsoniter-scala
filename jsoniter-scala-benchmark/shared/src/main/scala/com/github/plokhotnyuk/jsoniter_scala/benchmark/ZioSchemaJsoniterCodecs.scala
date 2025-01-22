package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import zio.blocks.schema.binding.RegisterOffset.RegisterOffset
import zio.blocks.schema.binding._

import scala.annotation.switch

object ZioSchemaJsoniterCodecs {
  implicit val primitivesCodec: JsonValueCodec[Primitives] = {
    val constructor: Constructor[Primitives] =
      new Constructor[Primitives] {
        def size: RegisterOffset = RegisterOffset(
          bytes = 1,
          shorts = 1,
          ints = 1,
          longs = 1,
          booleans = 1,
          chars = 1,
          doubles = 1,
          floats = 1
        )

        def construct(rs: Registers, baseOffset: RegisterOffset): Primitives = Primitives(
          b = rs.getByte(baseOffset, 0),
          s = rs.getShort(baseOffset, 0),
          i = rs.getInt(baseOffset, 0),
          l = rs.getLong(baseOffset, 0),
          bl = rs.getBoolean(baseOffset, 0),
          ch = rs.getChar(baseOffset, 0),
          dbl = rs.getDouble(baseOffset, 0),
          f = rs.getFloat(baseOffset, 0)
        )
      }

    val deconstructor: Deconstructor[Primitives] =
      new Deconstructor[Primitives] {
        def size: RegisterOffset = RegisterOffset(ints = 1, objects = 3)

        def deconstruct(rs: Registers, baseOffset: RegisterOffset, in: Primitives): Unit = {
          rs.setByte(baseOffset, 0, in.b)
          rs.setShort(baseOffset, 0, in.s)
          rs.setInt(baseOffset, 0, in.i)
          rs.setLong(baseOffset, 0, in.l)
          rs.setBoolean(baseOffset, 0, in.bl)
          rs.setChar(baseOffset, 0, in.ch)
          rs.setDouble(baseOffset, 0, in.dbl)
          rs.setFloat(baseOffset, 0, in.f)
        }
      }


    new JsonValueCodec[Primitives] {
      private[this] val registers = Registers()

      @inline def nullValue: Primitives = null

      @inline def decodeValue(in: JsonReader, default: Primitives): Primitives = d0(in, default)

      @inline def encodeValue(x: Primitives, out: JsonWriter): _root_.scala.Unit = e0(x, out)

      private[this] def d0(in: JsonReader, default: Primitives): Primitives =
        if (in.isNextToken('{')) {
          val rs = registers
          rs.setByte(0, 0, 0: Byte)
          rs.setShort(0, 0, 0: Short)
          rs.setInt(0, 0, 0)
          rs.setLong(0, 0, 0L)
          rs.setBoolean(0, 0, false)
          rs.setChar(0, 0, 0: Char)
          rs.setDouble(0, 0, 0.0)
          rs.setFloat(0, 0, 0.0f)
          var p0 = 255
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            var l = -1
            while (l < 0  ||  in.isNextToken(',')) {
              l = in.readKeyAsCharBuf()
              if (in.isCharBufEqualsTo(l, "b")) {
                p0 &= -2
                rs.setByte(0, 0, in.readByte())
              } else if (in.isCharBufEqualsTo(l, "s")) {
                p0 &= -3
                rs.setShort(0, 0, in.readShort())
              } else if (in.isCharBufEqualsTo(l, "i")) {
                p0 &= -5
                rs.setInt(0, 0, in.readInt())
              } else if (in.isCharBufEqualsTo(l, "l")) {
                p0 &= -9
                rs.setLong(0, 0, in.readLong())
              } else if (in.isCharBufEqualsTo(l, "bl")) {
                p0 &= -17
                rs.setBoolean(0, 0, in.readBoolean())
              } else if (in.isCharBufEqualsTo(l, "ch")) {
                p0 &= -33
                rs.setChar(0, 0, in.readChar())
              } else if (in.isCharBufEqualsTo(l, "dbl")) {
                p0 &= -65
                rs.setDouble(0, 0, in.readDouble())
              } else if (in.isCharBufEqualsTo(l, "f")) {
                p0 &= -129
                rs.setFloat(0, 0, in.readFloat())
              } else in.skip()
            }
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
          if (p0 != 0) in.requiredFieldError(f0(_root_.java.lang.Integer.numberOfTrailingZeros(p0)))
          constructor.construct(rs, 0)
        }
        else
          in.readNullOrTokenError(default, '{')

      private[this] def e0(x: Primitives, out: JsonWriter): _root_.scala.Unit = {
        val rs = registers
        deconstructor.deconstruct(rs, 0, x)
        out.writeObjectStart()
        out.writeNonEscapedAsciiKey("b")
        out.writeVal(rs.getByte(0, 0))
        out.writeNonEscapedAsciiKey("s")
        out.writeVal(rs.getShort(0, 0))
        out.writeNonEscapedAsciiKey("i")
        out.writeVal(rs.getInt(0, 0))
        out.writeNonEscapedAsciiKey("l")
        out.writeVal(rs.getLong(0, 0))
        out.writeNonEscapedAsciiKey("bl")
        out.writeVal(rs.getBoolean(0, 0))
        out.writeNonEscapedAsciiKey("ch")
        out.writeVal(rs.getChar(0, 0))
        out.writeNonEscapedAsciiKey("dbl")
        out.writeVal(rs.getDouble(0, 0))
        out.writeNonEscapedAsciiKey("f")
        out.writeVal(rs.getFloat(0, 0))
        out.writeObjectEnd()
      }

      private[this] def f0(i: Int): String = ((i: @switch): @unchecked) match {
        case 0 => "b"
        case 1 => "s"
        case 2 => "i"
        case 3 => "l"
        case 4 => "bl"
        case 5 => "ch"
        case 6 => "dbl"
        case 7 => "f"
      }
    }
  }
}
