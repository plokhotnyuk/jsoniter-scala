package com.github.plokhotnyuk.jsoniter_scala

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import org.scalatest.{Matchers, WordSpec}

class JsonReaderSpec extends WordSpec with Matchers {
  case class Device(id: Int, model: String)
  implicit val deviceCodec: JsonCodec[Device] = JsonCodec.materialize[Device](CodecConfig())

  case class User(name: String, devices: Seq[Device])
  val userCodec: JsonCodec[User] = JsonCodec.materialize[User](CodecConfig())

  val user = User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X")))
  val json: Array[Byte] = """{"name":"John","devices":[{"id":2,"model":"iPhone X"}]}""".getBytes("UTF-8")
  val httpMessage: Array[Byte] =
    """HTTP/1.0 200 OK
      |Content-Type: application/json
      |Content-Length: 55
      |
      |{"name":"John","devices":[{"id":2,"model":"iPhone X"}]}""".stripMargin.getBytes("UTF-8")
  "JsonReader.read" should {
    "parse json from the provided input stream" in {
      JsonReader.read(userCodec, new ByteArrayInputStream(json)) shouldBe user
    }
    "parse json from the byte array" in {
      JsonReader.read(userCodec, json) shouldBe user
    }
    "parse json from the byte array within specified positions" in {
      JsonReader.read(userCodec, httpMessage, 66, httpMessage.length) shouldBe user
    }
    "throw json exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](JsonReader.read(userCodec, httpMessage)).getMessage ==
        """expected '{' or null, offset: 0x00000000, buf:
          |           +-------------------------------------------------+
          |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 48 54 54 50 2f 31 2e 30 20 32 30 30 20 4f 4b 0a | HTTP/1.0 200 OK. |
          || 00000010 | 43 6f 6e 74 65 6e 74 2d 54 79 70 65 3a 20 61 70 | Content-Type: ap |
          || 00000020 | 70 6c 69 63 61 74 69 6f 6e 2f 6a 73 6f 6e 0a 43 | plication/json.C |
          |+----------+-------------------------------------------------+------------------+""".stripMargin)
    }
    "throw json exception in case of the provided params are invalid" in {
      intercept[NullPointerException](JsonReader.read(null, json))
      intercept[NullPointerException](JsonReader.read(null, new ByteArrayInputStream(json)))
      intercept[NullPointerException](JsonReader.read(null, httpMessage, 66, httpMessage.length))
      intercept[NullPointerException](JsonReader.read(userCodec, null.asInstanceOf[Array[Byte]]))
      intercept[NullPointerException](JsonReader.read(userCodec, null.asInstanceOf[Array[Byte]], 0, 50))
      intercept[NullPointerException](JsonReader.read(userCodec, null.asInstanceOf[InputStream]))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonReader.read(userCodec, httpMessage, 50, 200))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonReader.read(userCodec, httpMessage, 50, 10))
        .getMessage.contains("`from` should be positive and not greater than `to`"))
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
    "throw parsing exception when skipping string that is not closed by parentheses" in {
      assert(intercept[JsonParseException](skip("\"")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](skip("\"abc")).getMessage.contains("unexpected end of input, offset: 0x00000005"))
    }
    "skip string values with escaped characters" in {
      skip(""""\\"""")
      skip(""""\\\"\\"""")
    }
    "skip number values" in {
      skip("0")
      skip("-0.0")
      skip("1.1")
      skip("2.1")
      skip(" 3.1")
      skip("\n4.1")
      skip("\t5.1")
      skip("\r6.1")
      skip("7.1e+123456789")
      skip("8.1E-123456789")
      skip("987654321.0E+10")
    }
    "skip boolean values" in {
      skip("true")
      skip(" \n\t\rfalse")
    }
    "throw parsing exception when skipping truncated boolean value" in {
      assert(intercept[JsonParseException](skip("t")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](skip("f")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
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
    "throw parsing exception when skipping not closed object" in {
      assert(intercept[JsonParseException](skip("{{}")).getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
    "skip array values" in {
      skip("[]")
      skip(" \n\t\r[[[[[]]]][[[]]]]")
      skip("[\"[\"]")
    }
    "throw parsing exception when skipping not closed array" in {
      assert(intercept[JsonParseException](skip("[[]")).getMessage.contains("unexpected end of input, offset: 0x00000004"))
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
      assert(intercept[JsonParseException](skip("]")).getMessage.contains("expected value, offset: 0x00000000"))
      assert(intercept[JsonParseException](skip("}")).getMessage.contains("expected value, offset: 0x00000000"))
      assert(intercept[JsonParseException](skip(",")).getMessage.contains("expected value, offset: 0x00000000"))
      assert(intercept[JsonParseException](skip(":")).getMessage.contains("expected value, offset: 0x00000000"))
    }
  }
  "JsonReader.readBoolean" should {
    "parse valid true and false values" in {
      parse("true".getBytes).readBoolean() shouldBe true
      parse("false".getBytes).readBoolean() shouldBe false
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      assert(intercept[JsonParseException](parse("".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](parse("tru".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](parse("fals".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000004"))
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
      assert(intercept[JsonParseException](parse("".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](parse("\"".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](parse("\"\\".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
    "throw parsing exception for boolean values & numbers" in {
      assert(intercept[JsonParseException](parse("true".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
      assert(intercept[JsonParseException](parse("false".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
      assert(intercept[JsonParseException](parse("12345".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
    }
    "get the same string value for escaped & non-escaped field names" in {
      readString("""Hello""") shouldBe readString("Hello")
      readString("""Hello""") shouldBe readString("\\u0048\\u0065\\u006C\\u006c\\u006f")
      readString("""\b\f\n\r\t\/\\""") shouldBe readString("\b\f\n\r\t/\\\\")
      readString("""\b\f\n\r\t\/Aиბ""") shouldBe
        readString("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1")
      readString("𝄞") shouldBe readString("\\ud834\\udd1e")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      assert(intercept[JsonParseException](readString("\\x0008"))
        .getMessage.contains("illegal escape sequence, offset: 0x00000002"))
      assert(intercept[JsonParseException](readString("\\u000Z"))
        .getMessage.contains("expected hex digit, offset: 0x00000006"))
      assert(intercept[JsonParseException](readString("\\u000"))
        .getMessage.contains("expected hex digit, offset: 0x00000006"))
      assert(intercept[JsonParseException](readString("\\u00"))
        .getMessage.contains("unexpected end of input, offset: 0x00000006"))
      assert(intercept[JsonParseException](readString("\\u0"))
        .getMessage.contains("unexpected end of input, offset: 0x00000005"))
      assert(intercept[JsonParseException](readString("\\"))
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](readString("\\udd1e"))
        .getMessage.contains("expected high surrogate character, offset: 0x00000006"))
      assert(intercept[JsonParseException](readString("\\ud834"))
        .getMessage.contains("unexpected end of input, offset: 0x00000008"))
      assert(intercept[JsonParseException](readString("\\ud834\\"))
        .getMessage.contains("unexpected end of input, offset: 0x00000009"))
      assert(intercept[JsonParseException](readString("\\ud834\\x"))
        .getMessage.contains("unexpected end of input, offset: 0x0000000a"))
      assert(intercept[JsonParseException](readString("\\ud834\\ud834"))
        .getMessage.contains("expected low surrogate character, offset: 0x0000000c"))
    }
    "throw parsing exception in case of illegal byte sequence" in {
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, offset: 0x00000001"))
      assert(intercept[JsonParseException](readString(Array[Byte](0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0x80, offset: 0x00000001"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xC0.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xc0, 0x80, offset: 0x00000002"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xC8.toByte, 0x08.toByte)))
        .getMessage.contains("malformed byte(s): 0xc8, 0x08, offset: 0x00000002"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xC8.toByte, 0xFF.toByte)))
        .getMessage.contains("malformed byte(s): 0xc8, 0xff, offset: 0x00000002"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte)))
        .getMessage.contains("malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0x80, 0x80, 0x80, offset: 0x00000004"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0x9d, 0x04, 0x9e, offset: 0x00000004"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0x9d, 0x84, 0xff, offset: 0x00000004"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0x9d, 0xff, 0x9e, offset: 0x00000004"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0xff, 0x84, 0x9e, offset: 0x00000004"))
      assert(intercept[JsonParseException](readString(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte)))
        .getMessage.contains("malformed byte(s): 0xf0, 0x9d, 0x84, 0x0e, offset: 0x00000004"))
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
      assert(intercept[JsonParseException](readInt("")).getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](readInt("-")).getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readInt("x")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on int overflow" in {
      assert(intercept[JsonParseException](readInt("2147483648"))
        .getMessage.contains("value is too large for int, offset: 0x00000009"))
      assert(intercept[JsonParseException](readInt("-2147483649"))
        .getMessage.contains("value is too large for int, offset: 0x0000000a"))
      assert(intercept[JsonParseException](readInt("12345678901"))
        .getMessage.contains("value is too large for int, offset: 0x0000000a"))
      assert(intercept[JsonParseException](readInt("-12345678901"))
        .getMessage.contains("value is too large for int, offset: 0x0000000b"))
      assert(intercept[JsonParseException](readInt("12345678901234567890"))
        .getMessage.contains("value is too large for int, offset: 0x0000000a"))
      assert(intercept[JsonParseException](readInt("-12345678901234567890"))
        .getMessage.contains("value is too large for int, offset: 0x0000000b"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readInt("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readInt("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readInt("0123456789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readInt("-0123456789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readInt("02147483647"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readInt("-02147483648"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
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
      assert(intercept[JsonParseException](readLong("")).getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](readLong("-")).getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readLong("x")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on long overflow" in {
      assert(intercept[JsonParseException](readLong("9223372036854775808"))
        .getMessage.contains("value is too large for long, offset: 0x00000012"))
      assert(intercept[JsonParseException](readLong("-9223372036854775809"))
        .getMessage.contains("value is too large for long, offset: 0x00000013"))
      assert(intercept[JsonParseException](readLong("12345678901234567890"))
        .getMessage.contains("value is too large for long, offset: 0x00000013"))
      assert(intercept[JsonParseException](readLong("-12345678901234567890"))
        .getMessage.contains("value is too large for long, offset: 0x00000014"))
      assert(intercept[JsonParseException](readLong("123456789012345678901234567890"))
        .getMessage.contains("value is too large for long, offset: 0x00000013"))
      assert(intercept[JsonParseException](readLong("-123456789012345678901234567890"))
        .getMessage.contains("value is too large for long, offset: 0x00000014"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readLong("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readLong("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readLong("01234567890123456789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readLong("-01234567890123456789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readLong("09223372036854775807"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readLong("-09223372036854775808"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
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
      assert(intercept[JsonParseException](readFloat("")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat(" ")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readFloat("-")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readFloat("$")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat(" $")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readFloat("-$")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readFloat("0e$")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readFloat("0e-$")).getMessage.contains("illegal number, offset: 0x00000003"))
      assert(intercept[JsonParseException](readFloat("0.E")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readFloat("0.+")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readFloat("0.-")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readFloat("NaN")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat("Inf")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat("Infinity")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readFloat("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readFloat("012345.6789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readFloat("-012345.6789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
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
      assert(intercept[JsonParseException](readDouble("")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble(" ")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readDouble("-")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readDouble("$")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble(" $")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readDouble("-$")).getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readDouble("0e$")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readDouble("0e-$")).getMessage.contains("illegal number, offset: 0x00000003"))
      assert(intercept[JsonParseException](readDouble("0.E")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readDouble("0.-")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readDouble("0.+")).getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readDouble("NaN")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble("Inf")).getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble("Infinity")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readDouble("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readDouble("012345.6789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readDouble("-012345.6789"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
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
      readBigInt("12345678901234567890123456789", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt("-12345678901234567890123456789", null) shouldBe BigInt("-12345678901234567890123456789")
    }
    "parse big number values without overflow" in {
      val bigNumber = "12345" + new String(Array.fill(6789)('0'))
      readBigInt(bigNumber, null) shouldBe BigInt(bigNumber)
      readBigInt("-" + bigNumber, null) shouldBe BigInt("-" + bigNumber)
    }
    "parse valid number values with skiping JSON space characters" in {
      readBigInt(" \n\t\r12345678901234567890123456789", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt(" \n\t\r-12345678901234567890123456789", null) shouldBe BigInt("-12345678901234567890123456789")
    }
    "parse valid number values and stops on not numeric or '.', 'e', 'E' chars" in {
      readBigInt("0$", null) shouldBe BigInt("0")
      readBigInt("1234567890123456789$", null) shouldBe BigInt("1234567890123456789")
      readBigInt("1234567890123456789.0123456789$", null) shouldBe BigInt("1234567890123456789")
      readBigInt("1234567890123456789e10$", null) shouldBe BigInt("1234567890123456789")
      readBigInt("1234567890123456789E10$", null) shouldBe BigInt("1234567890123456789")
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[JsonParseException](readBigInt("", null))
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt(" ", null))
        .getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigInt("-", null))
        .getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigInt("$", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt(" $", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigInt("-$", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigInt("NaN", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt("Inf", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt("Infinity", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readBigInt("00", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt("-00", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigInt("012345", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigInt("-012345", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
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
      readBigDecimal(" \n\t\r1234567890123456789.0123456789", null) shouldBe
        BigDecimal("1234567890123456789.0123456789")
      readBigDecimal(" \n\t\r-1234567890123456789.0123456789", null) shouldBe
        BigDecimal("-1234567890123456789.0123456789")
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
      assert(intercept[JsonParseException](readBigDecimal("", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal(" ", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigDecimal("-", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigDecimal("$", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal(" $", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigDecimal("-$", null))
        .getMessage.contains("illegal number, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigDecimal("0e$", null))
        .getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readBigDecimal("0e-$", null))
        .getMessage.contains("illegal number, offset: 0x00000003"))
      assert(intercept[JsonParseException](readBigDecimal("0.E", null))
        .getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readBigDecimal("0.-", null))
        .getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readBigDecimal("0.+", null))
        .getMessage.contains("illegal number, offset: 0x00000002"))
      assert(intercept[JsonParseException](readBigDecimal("NaN", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal("Inf", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal("Infinity", null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readBigDecimal("00", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal("-00", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readBigDecimal("012345.6789", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readBigDecimal("-012345.6789", null))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
    }
  }

  def parse(buf: Array[Byte]): JsonReader =
    new JsonReader(new Array[Byte](12), 0, 0, new Array[Char](1), new ByteArrayInputStream(buf))

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

  def readBigDecimal(s: String, default: BigDecimal): BigDecimal =
    readBigDecimal(s.getBytes(StandardCharsets.UTF_8), default)

  def readBigDecimal(buf: Array[Byte], default: BigDecimal): BigDecimal = parse(buf).readBigDecimal(default)
}