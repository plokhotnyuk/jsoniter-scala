package com.github.plokhotnyuk.jsoniter_scala

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

class JsonReaderSpec extends WordSpec with Matchers {
  "JsonReader.this" should {
    "create an properly defined instances only" in {
      new JsonReader(new Array[Byte](2), 0, 2, new Array[Char](2), null)
      assert(intercept[Exception](new JsonReader(new Array[Byte](0), 0, 2, new Array[Char](2), null))
        .getMessage.contains("buf should be non empty"))
      assert(intercept[Exception](new JsonReader(null, 0, 2, new Array[Char](2), null))
        .getMessage.contains("buf should be non empty"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), -1, 2, new Array[Char](2), null))
        .getMessage.contains("head should be positive and not greater than tail"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), 3, 2, new Array[Char](2), null))
        .getMessage.contains("head should be positive and not greater than tail"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), 0, -1, new Array[Char](2), null))
        .getMessage.contains("tail should be positive and not greater than buf size"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), 0, 3, new Array[Char](2), null))
        .getMessage.contains("tail should be positive and not greater than buf size"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), 0, 2, new Array[Char](0), null))
        .getMessage.contains("reusableChars should be non empty"))
      assert(intercept[Exception](new JsonReader(new Array[Byte](2), 0, 2, null, null))
        .getMessage.contains("reusableChars should be non empty"))
    }
  }
  "JsonReader.skip" should {
    "skip string values" in {
      skip("\"\"")
      skip("\" \"")
      skip(" \n\t\r\" \"")
      skip("\"[\"")
      skip("\"{\"")
      skip("\"0\"")
      skip("\"9\"")
      skip("\"-\"")
    }
    "skip string values with escaped characters" in {
      skip(""""\\"""")
      skip(""""\\\"\\"""")
    }
    "skip number values" in {
      skip("0")
      skip("1.1")
      skip("-2.1")
      skip(" 3.1")
      skip("\n4.1")
      skip("\t5.1")
      skip("\r6.1")
      skip("7.1e+123456789")
      skip("-8.1E-123456789")
      skip("987654321.0E+10")
    }
    "skip boolean values" in {
      skip("true")
      skip(" \n\t\rfalse")
    }
    "skip null values" in {
      skip("null")
      skip(" \n\t\rnull")
    }
    "skip object values" in {
      skip("{}")
      skip(" \n\t\r{{{{{}}}}{{{}}}}")
      skip("{\"{\"}")
    }
    "skip array values" in {
      skip("[]")
      skip(" \n\t\r[[[[[]]]][[[]]]]")
      skip("[\"[\"]")
    }
    "skip mixed values" in {
      skip(
        """
          |{
          |  "x": {
          |    "xx": [
          |      -1.0,
          |      1,
          |      4.0E20
          |    ],
          |    "yy": {
          |      "xxx": true,
          |      "yyy": false,
          |      "zzz": null
          |    }
          |  },
          |  "y": [
          |    [1, 2, 3],
          |    [4, 5, 6],
          |    [7, 8, 9]
          |  ]
          |}""".stripMargin)
    }
    "throw parsing exception when skipping not from start of JSON value" in {
      assert(intercept[Exception](skip("]")).getMessage.contains("expected JSON value"))
      assert(intercept[Exception](skip("}")).getMessage.contains("expected JSON value"))
      assert(intercept[Exception](skip(",")).getMessage.contains("expected JSON value"))
      assert(intercept[Exception](skip(":")).getMessage.contains("expected JSON value"))
    }
  }
  "JsonReader.readBoolean" should {
    "parse valid true and false values" in {
      parse("true".getBytes).readBoolean() shouldBe true
      parse("false".getBytes).readBoolean() shouldBe false
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      assert(intercept[Exception](parse("".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](parse("tru".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](parse("tru".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input"))
    }
  }
  "JsonReader.readString" should {
    "parse null value" in {
      parse("null".getBytes).readString() shouldBe null
    }
    "return supplied default value instead of null value" in {
      parse("null".getBytes).readString("VVV") shouldBe "VVV"
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
    "throw parsing exception for empty input and illegal or broken string" in {
      assert(intercept[Exception](parse("".getBytes).readString())
        .getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](parse("\"".getBytes).readString())
        .getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](parse("\"\\".getBytes).readString())
        .getMessage.contains("unexpected end of input"))
    }
    "throw parsing exception for boolean values & numbers" in {
      assert(intercept[Exception](parse("true".getBytes).readString())
        .getMessage.contains("expected string value or `null`"))
      assert(intercept[Exception](parse("false".getBytes).readString())
        .getMessage.contains("expected string value or `null`"))
      assert(intercept[Exception](parse("12345".getBytes).readString())
        .getMessage.contains("expected string value or `null`"))
    }
    "get the same string value for escaped & non-escaped field names" in {
      readString("""Hello""") shouldBe readString("Hello")
      readString("""Hello""") shouldBe readString("\\u0048\\u0065\\u006C\\u006c\\u006f")
      readString("""\b\f\n\r\t\/\\""") shouldBe readString("\b\f\n\r\t/\\\\")
      readString("""\b\f\n\r\t\/Aиბ""") shouldBe readString("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1")
      readString("𝄞") shouldBe readString("\\ud834\\udd1e")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      assert(intercept[Exception](readString("\\x0008")).getMessage.contains("illegal escape sequence"))
      assert(intercept[Exception](readString("\\u000Z")).getMessage.contains("expected hex digit"))
      assert(intercept[Exception](readString("\\u000")).getMessage.contains("expected hex digit"))
      assert(intercept[Exception](readString("\\u00")).getMessage.contains("expected hex digit"))
      assert(intercept[Exception](readString("\\u0")).getMessage.contains("expected hex digit"))
      assert(intercept[Exception](readString("\\")).getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](readString("\\udd1e")).getMessage.contains("expected high surrogate character"))
      assert(intercept[Exception](readString("\\ud834")).getMessage.contains("illegal escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\")).getMessage.contains("illegal escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\x")).getMessage.contains("illegal escape sequence"))
      assert(intercept[Exception](readString("\\ud834\\ud834")).getMessage.contains("expected low surrogate character"))
    }
    "throw parsing exception in case of illegal byte sequence" in {
      assert(intercept[Exception](readString(Array[Byte](0xF0.toByte))).getMessage.contains("unexpected end of input"))
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
  "JsonReader.readInt" should {
    "parse valid int values" in {
      readInt("0") shouldBe 0
      readInt("-0") shouldBe -0
      readInt("123456789") shouldBe 123456789
      readInt("-123456789") shouldBe -123456789
      readInt("2147483647") shouldBe 2147483647
      readInt("-2147483648") shouldBe -2147483648
    }
    "parse valid int values with skiping JSON space characters" in {
      readInt(" \n\t\r123456789") shouldBe 123456789
      readInt(" \n\t\r-123456789") shouldBe -123456789
    }
    "parse valid int values and stops on not numeric chars" in {
      readInt("0$") shouldBe 0
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readInt("")).getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](readInt("-")).getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](readInt("x")).getMessage.contains("illegal number"))
    }
    "throw parsing exception on int overflow" in {
      assert(intercept[Exception](readInt("2147483648")).getMessage.contains("value is too large for int"))
      assert(intercept[Exception](readInt("-2147483649")).getMessage.contains("value is too large for int"))
      assert(intercept[Exception](readInt("12345678901")).getMessage.contains("value is too large for int"))
      assert(intercept[Exception](readInt("-12345678901")).getMessage.contains("value is too large for int"))
      assert(intercept[Exception](readInt("12345678901234567890")).getMessage.contains("value is too large for int"))
      assert(intercept[Exception](readInt("-12345678901234567890")).getMessage.contains("value is too large for int"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readInt("00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readInt("-00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readInt("0123456789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readInt("-0123456789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readInt("02147483647")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readInt("-02147483648")).getMessage.contains("illegal number"))
    }
  }
  "JsonReader.readLong" should {
    "parse valid long values" in {
      readLong("0") shouldBe 0L
      readLong("-0") shouldBe -0L
      readLong("1234567890123456789") shouldBe 1234567890123456789L
      readLong("-1234567890123456789") shouldBe -1234567890123456789L
      readLong("9223372036854775807") shouldBe 9223372036854775807L
      readLong("-9223372036854775808") shouldBe -9223372036854775808L
    }
    "parse valid long values with skiping JSON space characters" in {
      readLong(" \n\t\r1234567890123456789") shouldBe 1234567890123456789L
      readLong(" \n\t\r-1234567890123456789") shouldBe -1234567890123456789L
    }
    "parse valid long values and stops on not numeric chars" in {
      readLong("0$") shouldBe 0L
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readLong("")).getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](readLong("-")).getMessage.contains("unexpected end of input"))
      assert(intercept[Exception](readLong("x")).getMessage.contains("illegal number"))
    }
    "throw parsing exception on long overflow" in {
      assert(intercept[Exception](readLong("9223372036854775808")).getMessage.contains("value is too large for long"))
      assert(intercept[Exception](readLong("-9223372036854775809")).getMessage.contains("value is too large for long"))
      assert(intercept[Exception](readLong("12345678901234567890")).getMessage.contains("value is too large for long"))
      assert(intercept[Exception](readLong("-12345678901234567890")).getMessage.contains("value is too large for long"))
      assert(intercept[Exception](readLong("123456789012345678901234567890")).getMessage.contains("value is too large for long"))
      assert(intercept[Exception](readLong("-123456789012345678901234567890")).getMessage.contains("value is too large for long"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readLong("00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readLong("-00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readLong("01234567890123456789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readLong("-01234567890123456789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readLong("09223372036854775807")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readLong("-09223372036854775808")).getMessage.contains("illegal number"))
    }
  }
  "JsonReader.readFloat" should {
    "parse valid float values" in {
      readFloat("0") shouldBe 0.0f
      readFloat("0e0") shouldBe 0.0f
      readFloat("0.0") shouldBe 0.0f
      readFloat("-0.0") shouldBe -0.0f
      readFloat("12345.6789") shouldBe 12345.6789f
      readFloat("-12345.6789") shouldBe -12345.6789f
      readFloat("12345.6789e10") shouldBe 1.23456788e14f
      readFloat("12345.6789e+10") shouldBe 1.23456788e14f
      readFloat("-12345.6789E-10") shouldBe -1.2345679e-6f
    }
    "parse infinity on float overflow" in {
      readFloat("12345e6789") shouldBe Float.PositiveInfinity
      readFloat("-12345e6789") shouldBe Float.NegativeInfinity
      readFloat("12345678901234567890e12345678901234567890") shouldBe Float.PositiveInfinity
      readFloat("-12345678901234567890e12345678901234567890") shouldBe Float.NegativeInfinity
    }
    "parse zero on float underflow" in {
      readFloat("12345e-6789") shouldBe 0.0f
      readFloat("-12345e-6789") shouldBe -0.0f
      readFloat("12345678901234567890e-12345678901234567890") shouldBe 0.0f
      readFloat("-12345678901234567890e-12345678901234567890") shouldBe -0.0f
    }
    "parse valid float values with skiping JSON space characters" in {
      readFloat(" \n\t\r12345.6789") shouldBe 12345.6789f
      readFloat(" \n\t\r-12345.6789") shouldBe -12345.6789f
    }
    "parse valid float values and stops on not numeric chars" in {
      readFloat("0$") shouldBe 0.0f
      readFloat("12345$") shouldBe 12345f
      readFloat("12345.6789$") shouldBe 12345.6789f
      readFloat("12345.6789e10$") shouldBe 1.23456788e14f
      readFloat("12345678901234567890e12345678901234567890$") shouldBe Float.PositiveInfinity
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readFloat("")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat(" ")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("-")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat(" $")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("-$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("0e$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("0e-$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("0.E")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("0.+")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("0.-")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("NaN")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("Inf")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("Infinity")).getMessage.contains("illegal number"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readFloat("00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("-00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("012345.6789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readFloat("-012345.6789")).getMessage.contains("illegal number"))
    }
  }
  "JsonReader.readDouble" should {
    "parse valid double values" in {
      readDouble("0") shouldBe 0.0
      readDouble("0e0") shouldBe 0.0
      readDouble("0.0") shouldBe 0.0
      readDouble("-0.0") shouldBe -0.0
      readDouble("123456789.12345678") shouldBe 1.2345678912345678e8
      readDouble("-123456789.12345678") shouldBe -1.2345678912345678e8
      readDouble("123456789.123456789e10") shouldBe 1.23456789123456794e18
      readDouble("123456789.123456789e+10") shouldBe 1.23456789123456794e18
      readDouble("-123456789.123456789E-10") shouldBe -0.012345678912345679
    }
    "parse infinity on double overflow" in {
      readDouble("12345e6789") shouldBe Double.PositiveInfinity
      readDouble("-12345e6789") shouldBe Double.NegativeInfinity
      readDouble("12345678901234567890e12345678901234567890") shouldBe Double.PositiveInfinity
      readDouble("-12345678901234567890e12345678901234567890") shouldBe Double.NegativeInfinity
    }
    "parse zero on double underflow" in {
      readDouble("12345e-6789") shouldBe 0.0
      readDouble("-12345e-6789") shouldBe -0.0
      readDouble("12345678901234567890e-12345678901234567890") shouldBe 0.0
      readDouble("-1234567890123456789e-12345678901234567890") shouldBe -0.0
    }
    "parse valid double values with skiping JSON space characters" in {
      readDouble(" \n\t\r123456789.12345678") shouldBe 1.2345678912345678e8
      readDouble(" \n\t\r-123456789.12345678") shouldBe -1.2345678912345678e8
    }
    "parse valid double values and stops on not numeric chars" in {
      readDouble("0$") shouldBe 0.0
      readDouble("123456789$") shouldBe 123456789.0
      readDouble("123456789.12345678$") shouldBe 1.2345678912345678e8
      readDouble("123456789.123456789e10$") shouldBe 1.23456789123456794e18
      readDouble("12345678901234567890e12345678901234567890$") shouldBe Double.PositiveInfinity
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readDouble("")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble(" ")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("-")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble(" $")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("-$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("0e$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("0e-$")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("0.E")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("0.-")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("0.+")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("NaN")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("Inf")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("Infinity")).getMessage.contains("illegal number"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readDouble("00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("-00")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("012345.6789")).getMessage.contains("illegal number"))
      assert(intercept[Exception](readDouble("-012345.6789")).getMessage.contains("illegal number"))
    }
  }
  "JsonReader.readBigInt" should {
    "parse null value" in {
      readBigInt("null", null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigInt("null", BigInt("12345")) shouldBe BigInt("12345")
    }
    "parse valid number values" in {
      readBigInt("0", null) shouldBe BigInt("0")
      readBigInt("0e0", null) shouldBe BigInt("0")
      readBigInt("0.0", null) shouldBe BigInt("0")
      readBigInt("-0.0", null) shouldBe BigInt("0")
      readBigInt("12345678901234567890123456789", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt("-12345678901234567890123456789", null) shouldBe BigInt("-12345678901234567890123456789")
      readBigInt("1234567890123456789.0123456789e10", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt("1234567890123456789.0123456789e+10", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt("-1234567890123456789.0123456789E-10", null) shouldBe BigInt("-123456789")
    }
    "parse big number values without overflow" in {
      readBigInt("12345e6789", null) shouldBe BigInt("12345" + new String(Array.fill(6789)('0')))
      readBigInt("-12345e6789", null) shouldBe BigInt("-12345" + new String(Array.fill(6789)('0')))
    }
    "parse zero on underflow" in {
      readBigInt("12345e-6789", null) shouldBe BigInt("0")
      readBigInt("-12345e-6789", null) shouldBe BigInt("0")
    }
    "parse valid number values with skiping JSON space characters" in {
      readBigInt(" \n\t\r12345678901234567890123456789", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt(" \n\t\r-12345678901234567890123456789", null) shouldBe BigInt("-12345678901234567890123456789")
    }
    "parse valid number values and stops on not numeric chars" in {
      readBigInt("0$", null) shouldBe BigInt("0")
      readBigInt("1234567890123456789$", null) shouldBe BigInt("1234567890123456789")
      readBigInt("1234567890123456789.0123456789$", null) shouldBe BigInt("1234567890123456789")
      readBigInt("1234567890123456789.0123456789e10$", null) shouldBe BigInt("12345678901234567890123456789")
    }
    "throw number format exception for too big exponents" in {
      intercept[NumberFormatException](readBigInt("12345678901234567890e12345678901234567890", null))
      intercept[NumberFormatException](readBigInt("-12345678901234567890e12345678901234567890", null))
      intercept[NumberFormatException](readBigInt("12345678901234567890e-12345678901234567890", null))
      intercept[NumberFormatException](readBigInt("-12345678901234567890e-12345678901234567890", null))
      intercept[NumberFormatException](readBigInt("12345678901234567890e12345678901234567890$", null))
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readBigInt("", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt(" ", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("-", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt(" $", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("-$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("0e$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("0e-$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("0.E", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("0.-", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("0.+", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("NaN", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("Inf", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("Infinity", null)).getMessage.contains("illegal number"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readBigInt("00", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("-00", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("012345.6789", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigInt("-012345.6789", null)).getMessage.contains("illegal number"))
    }
  }
  "JsonReader.readBigDecimal" should {
    "parse null value" in {
      readBigDecimal("null", null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigDecimal("null", BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
    "parse valid number values" in {
      readBigDecimal("0", null) shouldBe BigDecimal("0")
      readBigDecimal("0e0", null) shouldBe BigDecimal("0")
      readBigDecimal("0.0", null) shouldBe BigDecimal("0")
      readBigDecimal("-0.0", null) shouldBe BigDecimal("0")
      readBigDecimal("1234567890123456789.0123456789", null) shouldBe BigDecimal("1234567890123456789.0123456789")
      readBigDecimal("-1234567890123456789.0123456789", null) shouldBe BigDecimal("-1234567890123456789.0123456789")
      readBigDecimal("1234567890123456789.0123456789e10", null) shouldBe BigDecimal("12345678901234567890123456789")
      readBigDecimal("1234567890123456789.0123456789e+10", null) shouldBe BigDecimal("12345678901234567890123456789")
      readBigDecimal("-1234567890123456789.0123456789E-10", null) shouldBe BigDecimal("-123456789.01234567890123456789")
    }
    "parse big number values without overflow" in {
      readBigDecimal("12345e6789", null) shouldBe BigDecimal("12345e6789")
      readBigDecimal("-12345e6789", null) shouldBe BigDecimal("-12345e6789")
    }
    "parse small number values without underflow" in {
      readBigDecimal("12345e-6789", null) shouldBe BigDecimal("12345e-6789")
      readBigDecimal("-12345e-6789", null) shouldBe BigDecimal("-12345e-6789")
    }
    "parse valid number values with skiping JSON space characters" in {
      readBigDecimal(" \n\t\r1234567890123456789.0123456789", null) shouldBe BigDecimal("1234567890123456789.0123456789")
      readBigDecimal(" \n\t\r-1234567890123456789.0123456789", null) shouldBe BigDecimal("-1234567890123456789.0123456789")
    }
    "parse valid number values and stops on not numeric chars" in {
      readBigDecimal("0$", null) shouldBe BigDecimal("0")
      readBigDecimal("1234567890123456789$", null) shouldBe BigDecimal("1234567890123456789")
      readBigDecimal("1234567890123456789.0123456789$", null) shouldBe BigDecimal("1234567890123456789.0123456789")
      readBigDecimal("1234567890123456789.0123456789e10$", null) shouldBe BigDecimal("12345678901234567890123456789")
    }
    "throw number format exception for too big exponents" in {
      intercept[NumberFormatException](readBigDecimal("12345678901234567890e12345678901234567890", null))
      intercept[NumberFormatException](readBigDecimal("-12345678901234567890e12345678901234567890", null))
      intercept[NumberFormatException](readBigDecimal("12345678901234567890e-12345678901234567890", null))
      intercept[NumberFormatException](readBigDecimal("-12345678901234567890e-12345678901234567890", null))
      intercept[NumberFormatException](readBigDecimal("12345678901234567890e12345678901234567890$", null))
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[Exception](readBigDecimal("", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal(" ", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("-", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal(" $", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("-$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("0e$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("0e-$", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("0.E", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("0.-", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("0.+", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("NaN", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("Inf", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("Infinity", null)).getMessage.contains("illegal number"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[Exception](readBigDecimal("00", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("-00", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("012345.6789", null)).getMessage.contains("illegal number"))
      assert(intercept[Exception](readBigDecimal("-012345.6789", null)).getMessage.contains("illegal number"))
    }
  }

  def parse(buf: Array[Byte]): JsonReader =
    new JsonReader(new Array[Byte](1), 0, 0, new Array[Char](1), new ByteArrayInputStream(buf))

  def skip(s: String): Unit = {
    val reader = parse((s + ",").getBytes)
    reader.skip()
    reader.nextToken().toChar shouldBe ','
  }

  def readString(s: String): String = readString(s.getBytes(StandardCharsets.UTF_8))

  def readString(buf: Array[Byte]): String = parse('"'.toByte +: buf :+ '"'.toByte).readString()

  def readInt(s: String): Int = readInt(s.getBytes(StandardCharsets.UTF_8))

  def readInt(buf: Array[Byte]): Int = parse(buf).readInt()

  def readLong(s: String): Long = readLong(s.getBytes(StandardCharsets.UTF_8))

  def readLong(buf: Array[Byte]): Long = parse(buf).readLong()

  def readFloat(s: String): Float = readFloat(s.getBytes(StandardCharsets.UTF_8))

  def readFloat(buf: Array[Byte]): Float = parse(buf).readFloat()

  def readDouble(s: String): Double = readDouble(s.getBytes(StandardCharsets.UTF_8))

  def readDouble(buf: Array[Byte]): Double = parse(buf).readDouble()

  def readBigInt(s: String, default: BigInt): BigInt = readBigInt(s.getBytes(StandardCharsets.UTF_8), default)

  def readBigInt(buf: Array[Byte], default: BigInt): BigInt = parse(buf).readBigInt(default)

  def readBigDecimal(s: String, default: BigDecimal): BigDecimal = readBigDecimal(s.getBytes(StandardCharsets.UTF_8), default)

  def readBigDecimal(buf: Array[Byte], default: BigDecimal): BigDecimal = parse(buf).readBigDecimal(default)
}
