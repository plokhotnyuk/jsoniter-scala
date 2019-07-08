package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalatest.{Matchers, WordSpec}

import scala.language.higherKinds

class VerifyingSpec extends WordSpec with Matchers {
  def verifySerDeser[T](codec: JsonValueCodec[T], obj: T, json: String, cfg: WriterConfig = WriterConfig()): Unit = {
    verifySer(codec, obj, json, cfg)
    verifyDeser(codec, obj, json)
  }

  def verifySer[T](codec: JsonValueCodec[T], obj: T, json: String, cfg: WriterConfig = WriterConfig()): Unit = {
    val len = json.getBytes(UTF_8).length
    verifyDirectByteBufferSer(codec, obj, len, cfg, json)
    verifyHeapByteBufferSer(codec, obj, len, cfg, json)
    verifyOutputStreamSer(codec, obj, cfg, json)
    verifyArraySer(codec, obj, cfg, json)
  }

  def verifyDeser[T](codec: JsonValueCodec[T], obj: T, json: String): Unit =
    verifyDeserByCheck[T](codec, json, check = (_: T) shouldBe obj)

  def verifyDeserByCheck[T](codec: JsonValueCodec[T], json: String, check: T => Unit): Unit = {
    val jsonBytes = json.getBytes(UTF_8)
    verifyDirectByteBufferDeser(codec, jsonBytes, check)
    verifyHeapByteBufferDeser(codec, jsonBytes, check)
    verifyInputStreamDeser(codec, jsonBytes, check)
    verifyByteArrayDeser(codec, jsonBytes, check)
  }

  def verifyDeserError[T](codec: JsonValueCodec[T], json: String, msg: String): Unit =
    verifyDeserError(codec, json.getBytes(UTF_8), msg)

  def verifyDeserError[T](codec: JsonValueCodec[T], jsonBytes: Array[Byte], msg: String): Unit = {
    assert(intercept[JsonReaderException](verifyDirectByteBufferDeser(codec, jsonBytes, (_: T) => ()))
      .getMessage.contains(msg))
    assert(intercept[JsonReaderException](verifyHeapByteBufferDeser(codec, jsonBytes, (_: T) => ()))
      .getMessage.contains(msg))
    assert(intercept[JsonReaderException](verifyInputStreamDeser(codec, jsonBytes, (_: T) => ()))
      .getMessage.contains(msg))
    assert(intercept[JsonReaderException](verifyByteArrayDeser(codec, jsonBytes, (_: T) => ()))
      .getMessage.contains(msg))
  }

  def verifyDirectByteBufferSer[T](codec: JsonValueCodec[T], obj: T, len: Int, cfg: WriterConfig, expectedStr: String): Unit = {
    val directBuf = ByteBuffer.allocateDirect(len + 100)
    directBuf.position(0)
    writeToByteBuffer(obj, directBuf, cfg)(codec)
    directBuf.position(0)
    val buf = new Array[Byte](len)
    directBuf.get(buf)
    toString(buf) shouldBe expectedStr
  }

  def verifyHeapByteBufferSer[T](codec: JsonValueCodec[T], obj: T, len: Int, cfg: WriterConfig, expectedStr: String): Unit = {
    val heapBuf = ByteBuffer.wrap(new Array[Byte](len + 100))
    heapBuf.position(0)
    writeToByteBuffer(obj, heapBuf, cfg)(codec)
    heapBuf.position(0)
    val buf = new Array[Byte](len)
    heapBuf.get(buf)
    toString(buf) shouldBe expectedStr
  }

  def verifyOutputStreamSer[T](codec: JsonValueCodec[T], obj: T, cfg: WriterConfig, expectedStr: String): Unit = {
    val baos = new ByteArrayOutputStream
    writeToStream(obj, baos, cfg)(codec)
    toString(baos.toByteArray) shouldBe expectedStr
  }

  def verifyArraySer[T](codec: JsonValueCodec[T], obj: T, cfg: WriterConfig, expectedStr: String): Unit =
    toString(writeToArray(obj, cfg)(codec)) shouldBe expectedStr

  def verifyDirectByteBufferDeser[T](codec: JsonValueCodec[T], json: Array[Byte], check: T => Unit): Unit = {
    val directBuf = ByteBuffer.allocateDirect(json.length)
    directBuf.put(json)
    directBuf.position(0)
    check(readFromByteBuffer(directBuf)(codec))
  }

  def verifyHeapByteBufferDeser[T](codec: JsonValueCodec[T], json: Array[Byte], check: T => Unit): Unit =
    check(readFromByteBuffer(ByteBuffer.wrap(json))(codec))

  def verifyInputStreamDeser[T](codec: JsonValueCodec[T], json: Array[Byte], check: T => Unit): Unit =
    check(readFromStream(new ByteArrayInputStream(json))(codec))

  def verifyByteArrayDeser[T](codec: JsonValueCodec[T], json: Array[Byte], check: T => Unit): Unit =
    check(readFromArray(json)(codec))

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}
