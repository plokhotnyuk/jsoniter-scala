package com.github.plokhotnyuk.jsoniter_scala.upickle

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import upickle.core.Visitor

import java.nio.charset.StandardCharsets

final class VisitorDecoder[J](maxDepth: Int,
                              numReader: VisitorNumberReader,
                              v: Visitor[?, J]) extends JsonValueCodec[J] {
  override def nullValue: J = null.asInstanceOf[J]

  override def encodeValue(x: J, out: JsonWriter): Unit =
    throw new UnsupportedOperationException("Codec only supports decoding")

  override def decodeValue(in: JsonReader, default: J): J =
    decode(in, maxDepth, v)

  private def decode[Z](in: JsonReader, depth: Int, v: Visitor[?, Z]): Z = {
    val b = in.nextToken()
    if (b == '"') {
      in.rollbackToken()
      v.visitString(in.readString(null), -1)
    } else if (b == 'f' || b == 't') {
      in.rollbackToken()
      if (in.readBoolean()) v.visitTrue(-1)
      else v.visitFalse(-1)
    } else if (b >= '0' && b <= '9' || b == '-') {
      in.rollbackToken()
      numReader.read(in, v)
    } else if (b == '[') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      val isEmpty = in.isNextToken(']')
      val arrV = v.visitArray(if (isEmpty) 0 else -1, -1).narrow
      if (!isEmpty) {
        in.rollbackToken()
        while ( {
          arrV.visitValue(decode(in, depthM1, arrV.subVisitor), -1)
          in.isNextToken(',')
        }) ()
        if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
      }
      arrV.visitEnd(-1)
    } else if (b == '{') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      val isEmpty = in.isNextToken('}')
      val objV = v.visitObject(if (isEmpty) 0 else -1, jsonableKeys = true, -1).narrow
      if (!isEmpty) {
        in.rollbackToken()
        var key = "?"
        while ( {
          key = in.readKeyAsString()
          objV.visitKeyValue(objV.visitKey(-1).visitString(key, -1))
          objV.visitValue(decode(in, depthM1, objV.subVisitor), -1)
          in.isNextToken(',')
        }) ()
        if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
      }
      objV.visitEnd(-1)
    } else in.readNullOrError(v.visitNull(-1), "expected JSON value")
  }
}

abstract class VisitorNumberReader {
  def read[N](in: JsonReader, v: Visitor[?, N]): N
}

object VisitorNumberReader {
  val Default: VisitorNumberReader = new VisitorNumberReader {
    override def read[N](in: JsonReader, v: Visitor[?, N]): N = {
      in.setMark()
      var isNeg = false
      var digits = 0
      var b = in.nextByte()
      if (b >= '0' && b <= '9') digits += 1
      else if (b == '-') {
        b = in.nextByte()
        isNeg = true
      }
      while ((b >= '0' && b <= '9') && in.hasRemaining()) {
        b = in.nextByte()
        digits += 1
      }
      in.rollbackToMark()
      if ((b | 0x20) != 'e' && b != '.') {
        if (digits < 19) {
          if (digits < 10) v.visitInt32(in.readInt(), -1)
          else v.visitInt64(in.readLong(), -1)
        } else {
          val x = in.readBigInt(null)
          if (x.isValidLong) v.visitInt64(x.longValue, -1)
          else v.visitString(x.toString(), -1) // see: ujson/JsVisitor.scala#L33
          // alt: v.visitFloat64StringParts(x.toString(), -1, -1, -1)
        }
      } else {
        in.setMark()
        val y = in.readDouble() // readDouble() returns Double.Infinity if too large
        if (java.lang.Double.isFinite(y)) { // https://github.com/openjdk/jdk/pull/9238
          val x = v.visitFloat64(y, -1)
          in.setMark()
          in.rollbackToMark()
          x
        } else {
          in.rollbackToMark()
          v.visitString(new String(in.readRawValAsBytes(), StandardCharsets.US_ASCII), -1) // see: ujson/JsVisitor.scala#L33
          // alt: v.visitFloat64StringParts(new String(in.readRaw...), -1, -1, -1)
        }
      }
    }
  }
}
