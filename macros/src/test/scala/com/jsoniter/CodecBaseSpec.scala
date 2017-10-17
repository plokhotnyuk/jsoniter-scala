package com.jsoniter

import org.scalatest.{Matchers, WordSpec}

class CodecBaseSpec extends WordSpec with Matchers {
  "CodecBase.readObjectFieldAsHash" should {
    "compute the same hash value for escaped & non-escaped field names" in {
      hashCode("""Hello""") shouldBe hashCode("Hello")
      hashCode("""Hello""") shouldBe hashCode("\\u0048\\u0065\\u006C\\u006c\\u006f")
      hashCode("""\b\f\n\r\t\/\\""") shouldBe hashCode("\b\f\n\r\t/\\\\")
      hashCode("""\b\f\n\r\t\/Aиბ""") shouldBe hashCode("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1")
      hashCode("𝄞") shouldBe hashCode("\\ud834\\udd1e")
    }
    "throw parsing exception in case of invalid escape character sequence" in {
      assert(intercept[Exception](hashCode("\\x0008")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\u000Z")).getMessage.contains("90 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u000")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u00")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\u0")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](hashCode("\\")).getMessage.contains("invalid byte or escape sequence"))
      assert(intercept[Exception](hashCode("\\udd1e")).getMessage.contains("expect high surrogate character"))
      assert(intercept[Exception](hashCode("\\ud834")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\ud834\\")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\ud834\\x")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](hashCode("\\ud834\\ud834")).getMessage.contains("expect low surrogate character"))
    }
    "throw parsing exception in case of invalid byte sequence" in {
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte))).getMessage.contains("invalid byte or escape sequence"))
      assert(intercept[Exception](hashCode(Array[Byte](0x80.toByte))).getMessage.contains("malformed byte(s): 0x80"))
      assert(intercept[Exception](hashCode(Array[Byte](0xC0.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xC0, 0x80"))
      assert(intercept[Exception](hashCode(Array[Byte](0xC8.toByte, 0x08.toByte))).getMessage.contains("malformed byte(s): 0xC8, 0x08"))
      assert(intercept[Exception](hashCode(Array[Byte](0xC8.toByte, 0xFF.toByte))).getMessage.contains("malformed byte(s): 0xC8, 0xFF"))
      assert(intercept[Exception](hashCode(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xE0, 0x80, 0x80"))
      assert(intercept[Exception](hashCode(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xE0, 0xFF, 0x80"))
      assert(intercept[Exception](hashCode(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte))).getMessage.contains("malformed byte(s): 0xE8, 0x88, 0x08"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x80, 0x80, 0x80"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x04, 0x9E"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x84, 0xFF"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0xFF, 0x9E"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0xFF, 0x84, 0x9E"))
      assert(intercept[Exception](hashCode(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x84, 0x0E"))
    }
  }
  "CodecBase.readString" should {
    "parse null value" in {
      CodecBase.readString(JsonIterator.parse("null".getBytes)) shouldBe null
      assert(intercept[Exception] {
        CodecBase.readString(JsonIterator.parse("true".getBytes))
      }.getMessage.contains("expect string or null"))
    }
    "parse long string" in {
      val text =
        """
          |JavaScript Object Notation (JSON) is a lightweight, text-based,
          |language-independent data interchange format.  It was derived from
          |the ECMAScript Programming Language Standard.  JSON defines a small
          |set of formatting rules for the portable representation of structured
          |data.""".stripMargin
      readString(text) shouldBe text
    }
    "throw parsing exception for boolean values & numbers" in {
      assert(intercept[Exception] {
        CodecBase.readString(JsonIterator.parse("true".getBytes))
      }.getMessage.contains("expect string or null"))
      assert(intercept[Exception] {
        CodecBase.readString(JsonIterator.parse("false".getBytes))
      }.getMessage.contains("expect string or null"))
      assert(intercept[Exception] {
        CodecBase.readString(JsonIterator.parse("12345".getBytes))
      }.getMessage.contains("expect string or null"))
    }
    "get the same string value for escaped & non-escaped field names" in {
      readString("""Hello""") shouldBe readString("Hello")
      readString("""Hello""") shouldBe readString("\\u0048\\u0065\\u006C\\u006c\\u006f")
      readString("""\b\f\n\r\t\/\\""") shouldBe readString("\b\f\n\r\t/\\\\")
      readString("""\b\f\n\r\t\/Aиბ""") shouldBe readString("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1")
      readString("𝄞") shouldBe readString("\\ud834\\udd1e")
    }
    "throw parsing exception in case of invalid escape character sequence" in {
      assert(intercept[Exception](readString("\\x0008")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](readString("\\u000Z")).getMessage.contains("90 is not valid hex digit"))
      assert(intercept[Exception](readString("\\u000")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](readString("\\u00")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](readString("\\u0")).getMessage.contains("34 is not valid hex digit"))
      assert(intercept[Exception](readString("\\")).getMessage.contains("invalid byte or escape sequence"))
      assert(intercept[Exception](readString("\\udd1e")).getMessage.contains("expect high surrogate character"))
      assert(intercept[Exception](readString("\\ud834")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\x")).getMessage.contains("invalid escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\ud834")).getMessage.contains("expect low surrogate character"))
    }
    "throw parsing exception in case of invalid byte sequence" in {
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte))).getMessage.contains("invalid byte or escape sequence"))
      assert(intercept[Exception](readString(Array[Byte](0x80.toByte))).getMessage.contains("malformed byte(s): 0x80"))
      assert(intercept[Exception](readString(Array[Byte](0xC0.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xC0, 0x80"))
      assert(intercept[Exception](readString(Array[Byte](0xC8.toByte, 0x08.toByte))).getMessage.contains("malformed byte(s): 0xC8, 0x08"))
      assert(intercept[Exception](readString(Array[Byte](0xC8.toByte, 0xFF.toByte))).getMessage.contains("malformed byte(s): 0xC8, 0xFF"))
      assert(intercept[Exception](readString(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xE0, 0x80, 0x80"))
      assert(intercept[Exception](readString(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xE0, 0xFF, 0x80"))
      assert(intercept[Exception](readString(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte))).getMessage.contains("malformed byte(s): 0xE8, 0x88, 0x08"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x80, 0x80, 0x80"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x04, 0x9E"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x84, 0xFF"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0xFF, 0x9E"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0xFF, 0x84, 0x9E"))
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte))).getMessage.contains("malformed byte(s): 0xF0, 0x9D, 0x84, 0x0E"))
    }
  }

  def hashCode(s: String): Long = hashCode(s.getBytes("UTF-8"))

  def hashCode(buf: Array[Byte]): Long =
    CodecBase.readObjectFieldAsHash(JsonIterator.parse('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte))

  def readString(s: String): String = readString(s.getBytes("UTF-8"))

  def readString(buf: Array[Byte]): String =
    CodecBase.readString(JsonIterator.parse('"'.toByte +: buf :+ '"'.toByte))
}
