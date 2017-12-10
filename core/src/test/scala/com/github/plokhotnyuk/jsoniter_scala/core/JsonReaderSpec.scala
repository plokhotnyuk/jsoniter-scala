package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import com.github.plokhotnyuk.jsoniter_scala.core.ReaderConfig
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class JsonReaderSpec extends WordSpec with Matchers with PropertyChecks {
  "JsonReader.read" should {
    "parse json from the provided input stream" in {
      JsonReader.read(codec, getClass.getResourceAsStream("user_api_response.json")) shouldBe user
    }
    "parse json from the byte array" in {
      JsonReader.read(codec, compactJson) shouldBe user
    }
    "parse json from the byte array within specified positions" in {
      JsonReader.read(codec, httpMessage, 66, httpMessage.length) shouldBe user
    }
    "throw json exception if cannot parse input with message containing input offset & hex dump of affected part" in {
      assert(intercept[JsonParseException](JsonReader.read(codec, httpMessage)).getMessage ==
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
      intercept[NullPointerException](JsonReader.read(null, compactJson))
      intercept[NullPointerException](JsonReader.read(null, new ByteArrayInputStream(compactJson)))
      intercept[NullPointerException](JsonReader.read(null, httpMessage, 66, httpMessage.length))
      intercept[NullPointerException](JsonReader.read(codec, null.asInstanceOf[Array[Byte]]))
      intercept[NullPointerException](JsonReader.read(codec, null.asInstanceOf[Array[Byte]], 0, 50))
      intercept[NullPointerException](JsonReader.read(codec, null.asInstanceOf[InputStream]))
      intercept[NullPointerException](JsonReader.read(codec, new ByteArrayInputStream(compactJson), null))
      intercept[NullPointerException](JsonReader.read(codec, httpMessage, 66, httpMessage.length, null))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonReader.read(codec, httpMessage, 50, 200))
        .getMessage.contains("`to` should be positive and not greater than `buf` length"))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonReader.read(codec, httpMessage, 50, 10))
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
  "JsonReader.nextToken" should {
    "find next non-whitespace byte of input" in {
      val r = reader("{}".getBytes)
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}".getBytes)
      r.skip()
      assert(intercept[JsonParseException](r.nextToken() == '{')
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.rollbackToken" should {
    "rollback of reading last byte of input" in {
      val r = reader("""{"x":1}""".getBytes)
      assert(r.nextToken() == '{')
      r.rollbackToken()
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '"')
      r.rollbackToken()
      assert(r.nextToken() == '"')
    }
    "throw array index out of bounds in case of missing preceding call of 'nextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("{}".getBytes).rollbackToken())
        .getMessage.contains("expected preceding call of 'nextToken()'"))
    }
  }
  "JsonReader.readBoolean" should {
    "parse valid true and false values" in {
      reader("true".getBytes).readBoolean() shouldBe true
      reader("false".getBytes).readBoolean() shouldBe false
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      assert(intercept[JsonParseException](reader("x".getBytes).readBoolean())
        .getMessage.contains("illegal boolean, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("trae".getBytes).readBoolean())
        .getMessage.contains("illegal boolean, offset: 0x00000002"))
      assert(intercept[JsonParseException](reader("folse".getBytes).readBoolean())
        .getMessage.contains("illegal boolean, offset: 0x00000001"))
      assert(intercept[JsonParseException](reader("".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("tru".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](reader("fals".getBytes).readBoolean())
        .getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
  }
  "JsonReader.readString" should {
    "parse null value" in {
      reader("null".getBytes).readString() shouldBe null
    }
    "return supplied default value instead of null value" in {
      reader("null".getBytes).readString("VVV") shouldBe "VVV"
    }
    "parse string with Unicode chars which are not escaped and are non-surrogate" in {
      forAll(minSuccessful(10000)) { (s: String) =>
        whenever(!s.exists(ch => ch == '"' || ch == '\\' || Character.isSurrogate(ch))) {
          readString(s) shouldBe s
        }
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      assert(intercept[JsonParseException](reader("".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("\"".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](reader("\"\\".getBytes).readString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
    "throw parsing exception for boolean values & numbers" in {
      assert(intercept[JsonParseException](reader("true".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("false".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("12345".getBytes).readString())
        .getMessage.contains("expected string value or null, offset: 0x00000000"))
    }
    "get the same string value for escaped strings as for non-escaped" in {
      readString("""\b\f\n\r\t\/\\""") shouldBe readString("\b\f\n\r\t/\\\\")
      readString("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1\\ud834\\udd1e") shouldBe
        readString("\b\f\n\r\t/Aиბ𝄞")
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
        .getMessage.contains("unexpected end of input, offset: 0x00000008"))
      assert(intercept[JsonParseException](readString("\\ud834"))
        .getMessage.contains("unexpected end of input, offset: 0x00000008"))
      assert(intercept[JsonParseException](readString("\\ud834\\"))
        .getMessage.contains("unexpected end of input, offset: 0x00000009"))
      assert(intercept[JsonParseException](readString("\\ud834\\x"))
        .getMessage.contains("unexpected end of input, offset: 0x0000000a"))
      assert(intercept[JsonParseException](readString("\\ud834\\ud834"))
        .getMessage.contains("illegal surrogate character pair, offset: 0x0000000c"))
    }
    "throw parsing exception in case of illegal byte sequence" in {
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
  "JsonReader.readChar" should {
    "parse Unicode char that is not escaped and is non-surrogate from string with length == 1" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          readChar(ch.toString) shouldBe ch
        }
      }
    }
    "throw parsing exception for string with length > 1" in {
      assert(intercept[JsonParseException](readChar("ZZZ"))
        .getMessage.contains("expected '\"', offset: 0x00000002"))
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      assert(intercept[JsonParseException](reader("".getBytes).readChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("\"".getBytes).readChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](reader("\"\\".getBytes).readChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
    "throw parsing exception for null, boolean values & numbers" in {
      assert(intercept[JsonParseException](reader("null".getBytes).readChar())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("true".getBytes).readChar())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("false".getBytes).readChar())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("12345".getBytes).readChar())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "get the same char value for escaped strings as for non-escaped" in {
      readChar("""\b""") shouldBe readChar("\b")
      readChar("""\f""") shouldBe readChar("\f")
      readChar("""\n""") shouldBe readChar("\n")
      readChar("""\r""") shouldBe readChar("\r")
      readChar("""\t""") shouldBe readChar("\t")
      readChar("""\/""") shouldBe readChar("/")
      readChar("""\\""") shouldBe readChar("\\\\")
      readChar("\\u0008") shouldBe readChar("\b")
      readChar("\\u000C") shouldBe readChar("\f")
      readChar("\\u000a") shouldBe readChar("\n")
      readChar("\\u000D") shouldBe readChar("\r")
      readChar("\\u0009") shouldBe readChar("\t")
      readChar("\\u002F") shouldBe readChar("/")
      readChar("\\u0041") shouldBe readChar("A")
      readChar("\\u0438") shouldBe readChar("и")
      readChar("\\u10d1") shouldBe readChar("ბ")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      assert(intercept[JsonParseException](readChar("\\x0008"))
        .getMessage.contains("illegal escape sequence, offset: 0x00000002"))
      assert(intercept[JsonParseException](readChar("\\u000Z"))
        .getMessage.contains("expected hex digit, offset: 0x00000006"))
      assert(intercept[JsonParseException](readChar("\\u000"))
        .getMessage.contains("expected hex digit, offset: 0x00000006"))
      assert(intercept[JsonParseException](readChar("\\u00"))
        .getMessage.contains("unexpected end of input, offset: 0x00000006"))
      assert(intercept[JsonParseException](readChar("\\u0"))
        .getMessage.contains("unexpected end of input, offset: 0x00000005"))
      assert(intercept[JsonParseException](readChar("\\"))
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](readChar("\\udd1e"))
        .getMessage.contains("illegal surrogate character, offset: 0x00000006"))
      assert(intercept[JsonParseException](readChar("\\ud834"))
        .getMessage.contains("illegal surrogate character, offset: 0x00000006"))
    }
    "throw parsing exception in case of illegal byte sequence" in {
      assert(intercept[JsonParseException](readChar(Array[Byte](0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0x80, offset: 0x00000001"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xC0.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xc0, 0x80, offset: 0x00000002"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xC8.toByte, 0x08.toByte)))
        .getMessage.contains("malformed byte(s): 0xc8, 0x08, offset: 0x00000002"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xC8.toByte, 0xFF.toByte)))
        .getMessage.contains("malformed byte(s): 0xc8, 0xff, offset: 0x00000002"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte)))
        .getMessage.contains("malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte)))
        .getMessage.contains("malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003"))
      assert(intercept[JsonParseException](readChar(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte)))
        .getMessage.contains("illegal surrogate character, offset: 0x00000004"))
    }
  }
  "JsonReader.readByte" should {
    "parse valid byte values" in {
      forAll(minSuccessful(10000)) { (n: Byte) =>
        val s = n.toString
        readByte(s) shouldBe java.lang.Byte.parseByte(s)
      }
    }
    "parse valid byte values with skiping of JSON space characters" in {
      readByte(" \n\t\r123") shouldBe 123.toByte
      readByte(" \n\t\r-123") shouldBe -123.toByte
    }
    "parse valid byte values and stops on not numeric chars" in {
      readByte("0$") shouldBe 0
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[JsonParseException](readByte("")).getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](readByte("-")).getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readByte("x")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on byte overflow" in {
      assert(intercept[JsonParseException](readByte("128"))
        .getMessage.contains("value is too large for byte, offset: 0x00000002"))
      assert(intercept[JsonParseException](readByte("-129"))
        .getMessage.contains("value is too large for byte, offset: 0x00000003"))
      assert(intercept[JsonParseException](readByte("12345"))
        .getMessage.contains("value is too large for byte, offset: 0x00000003"))
      assert(intercept[JsonParseException](readByte("-12345"))
        .getMessage.contains("value is too large for byte, offset: 0x00000004"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readByte("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readByte("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readByte("0123"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readByte("-0123"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readByte("0128"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readByte("-0128"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
    }
  }
  "JsonReader.readShort" should {
    "parse valid short values" in {
      forAll(minSuccessful(10000)) { (n: Short) =>
        val s = n.toString
        readShort(s) shouldBe java.lang.Short.parseShort(s)
      }
    }
    "parse valid short values with skiping of JSON space characters" in {
      readShort(" \n\t\r12345") shouldBe 12345.toShort
      readShort(" \n\t\r-12345") shouldBe -12345.toShort
    }
    "parse valid short values and stops on not numeric chars" in {
      readShort("0$") shouldBe 0
    }
    "throw parsing exception on illegal or empty input" in {
      assert(intercept[JsonParseException](readShort("")).getMessage.contains("unexpected end of input, offset: 0x00000000"))
      assert(intercept[JsonParseException](readShort("-")).getMessage.contains("unexpected end of input, offset: 0x00000001"))
      assert(intercept[JsonParseException](readShort("x")).getMessage.contains("illegal number, offset: 0x00000000"))
    }
    "throw parsing exception on short overflow" in {
      assert(intercept[JsonParseException](readShort("32768"))
        .getMessage.contains("value is too large for short, offset: 0x00000004"))
      assert(intercept[JsonParseException](readShort("-32769"))
        .getMessage.contains("value is too large for short, offset: 0x00000005"))
      assert(intercept[JsonParseException](readShort("12345678901"))
        .getMessage.contains("value is too large for short, offset: 0x00000005"))
      assert(intercept[JsonParseException](readShort("-12345678901"))
        .getMessage.contains("value is too large for short, offset: 0x00000006"))
    }
    "throw parsing exception on leading zero" in {
      assert(intercept[JsonParseException](readShort("00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readShort("-00"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readShort("012345"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readShort("-012345"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
      assert(intercept[JsonParseException](readShort("032767"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000000"))
      assert(intercept[JsonParseException](readShort("-032768"))
        .getMessage.contains("illegal number with leading zero, offset: 0x00000001"))
    }
  }
  "JsonReader.readInt" should {
    "parse valid int values" in {
      forAll(minSuccessful(10000)) { (n: Int) =>
        val s = n.toString
        readInt(s) shouldBe java.lang.Integer.parseInt(s)
      }
    }
    "parse valid int values with skiping of JSON space characters" in {
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
      forAll(minSuccessful(10000)) { (n: Long) =>
        val s = n.toString
        readLong(s) shouldBe java.lang.Long.parseLong(s)
      }
    }
    "parse valid long values with skiping of JSON space characters" in {
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
      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        val s = n.toString
        readFloat(s) shouldBe java.lang.Float.parseFloat(s)
      }
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
    "parse valid float values with skiping of JSON space characters" in {
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
      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        val s = n.toString
        readDouble(s) shouldBe java.lang.Double.parseDouble(s)
      }
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
    "parse valid double values with skiping of JSON space characters" in {
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
      forAll(minSuccessful(10000)) { (n: BigInt) =>
        val s = n.toString
        readBigInt(s, null) shouldBe BigInt(s)
      }
    }
    "parse big number values without overflow" in {
      val bigNumber = "12345" + new String(Array.fill(6789)('0'))
      readBigInt(bigNumber, null) shouldBe BigInt(bigNumber)
      readBigInt("-" + bigNumber, null) shouldBe BigInt("-" + bigNumber)
    }
    "parse valid number values with skiping of JSON space characters" in {
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
      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        val s = n.toString
        readBigDecimal(s, null) shouldBe BigDecimal(s)
      }
    }
    "parse big number values without overflow" in {
      readBigDecimal("12345e6789", null) shouldBe BigDecimal("12345e6789")
      readBigDecimal("-12345e6789", null) shouldBe BigDecimal("-12345e6789")
    }
    "parse small number values without underflow" in {
      readBigDecimal("12345e-6789", null) shouldBe BigDecimal("12345e-6789")
      readBigDecimal("-12345e-6789", null) shouldBe BigDecimal("-12345e-6789")
    }
    "parse valid number values with skiping of JSON space characters" in {
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

  def skip(s: String): Unit = {
    val r = reader((s + ",").getBytes)
    r.skip()
    r.nextToken().toChar shouldBe ','
  }

  def readString(s: String): String = readString(s.getBytes(UTF_8))

  def readString(buf: Array[Byte]): String = reader('"'.toByte +: buf :+ '"'.toByte).readString()

  def readChar(s: String): Char = readChar(s.getBytes(UTF_8))

  def readChar(buf: Array[Byte]): Char = reader('"'.toByte +: buf :+ '"'.toByte).readChar()

  def readByte(s: String): Byte = readByte(s.getBytes(UTF_8))

  def readByte(buf: Array[Byte]): Byte = reader(buf).readByte()

  def readShort(s: String): Short = readShort(s.getBytes(UTF_8))

  def readShort(buf: Array[Byte]): Short = reader(buf).readShort()

  def readInt(s: String): Int = readInt(s.getBytes(UTF_8))

  def readInt(buf: Array[Byte]): Int = reader(buf).readInt()

  def readLong(s: String): Long = readLong(s.getBytes(UTF_8))

  def readLong(buf: Array[Byte]): Long = reader(buf).readLong()

  def readFloat(s: String): Float = readFloat(s.getBytes(UTF_8))

  def readFloat(buf: Array[Byte]): Float = reader(buf).readFloat()

  def readDouble(s: String): Double = readDouble(s.getBytes(UTF_8))

  def readDouble(buf: Array[Byte]): Double = reader(buf).readDouble()

  def readBigInt(s: String, default: BigInt): BigInt = readBigInt(s.getBytes(UTF_8), default)

  def readBigInt(buf: Array[Byte], default: BigInt): BigInt = reader(buf).readBigInt(default)

  def readBigDecimal(s: String, default: BigDecimal): BigDecimal =
    readBigDecimal(s.getBytes(UTF_8), default)

  def readBigDecimal(buf: Array[Byte], default: BigDecimal): BigDecimal = reader(buf).readBigDecimal(default)

  def reader(buf: Array[Byte]): JsonReader = new JsonReader(new Array[Byte](12), // a minimal allowed length of `buf`
    0, 0, -1, new Array[Char](0), new ByteArrayInputStream(buf), 0, ReaderConfig())
}