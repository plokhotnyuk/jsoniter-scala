package com.github.plokhotnyuk.jsoniter_scala.upickle

import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import upickle.core.{ArrVisitor, ObjVisitor, StringVisitor, Visitor}

class JsonWriterVisitor(writer: JsonWriter) extends Visitor[Any, JsonWriter] {

  override def visitNull(index: Int): JsonWriter = {
    writer.writeNull()
    writer
  }

  override def visitFalse(index: Int): JsonWriter = {
    writer.writeVal(false)
    writer
  }

  override def visitTrue(index: Int): JsonWriter = {
    writer.writeVal(true)
    writer
  }

  override def visitInt64(i: Long, index: Int): JsonWriter = {
    writer.writeVal(i)
    writer
  }

  override def visitFloat64(d: Double, index: Int): JsonWriter = {
    writer.writeVal(d)
    writer
  }

  override def visitFloat64String(s: String, index: Int): JsonWriter = {
    writer.writeNonEscapedAsciiVal(s)
    writer
  }

  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): JsonWriter = {
    writer.writeNonEscapedAsciiVal(s.toString)
    writer
  }

  override def visitString(s: CharSequence, index: Int): JsonWriter = {
    writer.writeVal(s.toString)
    writer
  }

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): JsonWriter = {
    val trimmed =
      if (offset == 0 && bytes.length <= len) bytes
      else bytes.slice(offset, offset + len)

    writer.writeBase64Val(trimmed, true)
    writer
  }

  override def visitArray(length: Int, index: Int): ArrVisitor[Any, JsonWriter] = {
    writer.writeArrayStart()
    new ArrVisitor[Any, JsonWriter] {
      override def subVisitor: Visitor[?, ?] = JsonWriterVisitor.this
      override def visitValue(v: Any, index: Int): Unit = ()
      override def visitEnd(index: Int): JsonWriter = {
        writer.writeArrayEnd()
        writer
      }
    }
  }

  override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[Any, JsonWriter] = {
    writer.writeObjectStart()
    new ObjVisitor[Any, JsonWriter] {
      override def visitKey(index: Int): Visitor[?, ?] = StringVisitor
      override def visitKeyValue(v: Any): Unit = writer.writeKey(v.toString)
      override def subVisitor: Visitor[?, ?] = JsonWriterVisitor.this
      override def visitValue(v: Any, index: Int): Unit = ()
      override def visitEnd(index: Int): JsonWriter = {
        writer.writeObjectEnd()
        writer
      }
    }
  }

  override def visitFloat32(d: Float, index: Int): JsonWriter = {
    writer.writeVal(d)
    writer
  }

  override def visitInt32(i: Int, index: Int): JsonWriter = {
    writer.writeVal(i)
    writer
  }

  override def visitUInt64(i: Long, index: Int): JsonWriter = {
    writer.writeVal(i)
    writer
  }

  override def visitChar(s: Char, index: Int): JsonWriter = {
    writer.writeVal(s)
    writer
  }

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): JsonWriter = 
    visitBinary(bytes, offset, len, index)
}
