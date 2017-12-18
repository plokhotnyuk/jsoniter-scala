package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
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
      validateSkip("\"\"")
      validateSkip("\" \"")
      validateSkip(" \n\t\r\" \"")
      validateSkip("\"[\"")
      validateSkip("\"{\"")
      validateSkip("\"0\"")
      validateSkip("\"9\"")
      validateSkip("\"-\"")
    }
    "throw parsing exception when skipping string that is not closed by parentheses" in {
      assert(intercept[JsonParseException](validateSkip("\"")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](validateSkip("\"abc")).getMessage.contains("unexpected end of input, offset: 0x00000005"))
    }
    "skip string values with escaped characters" in {
      validateSkip(""""\\"""")
      validateSkip(""""\\\"\\"""")
    }
    "skip number values" in {
      validateSkip("0")
      validateSkip("-0.0")
      validateSkip("1.1")
      validateSkip("2.1")
      validateSkip(" 3.1")
      validateSkip("\n4.1")
      validateSkip("\t5.1")
      validateSkip("\r6.1")
      validateSkip("7.1e+123456789")
      validateSkip("8.1E-123456789")
      validateSkip("987654321.0E+10")
    }
    "skip boolean values" in {
      validateSkip("true")
      validateSkip(" \n\t\rfalse")
    }
    "throw parsing exception when skipping truncated boolean value" in {
      assert(intercept[JsonParseException](validateSkip("t")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](validateSkip("f")).getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
    "skip null values" in {
      validateSkip("null")
      validateSkip(" \n\t\rnull")
    }
    "skip object values" in {
      validateSkip("{}")
      validateSkip(" \n\t\r{{{{{}}}}{{{}}}}")
      validateSkip("{\"{\"}")
    }
    "throw parsing exception when skipping not closed object" in {
      assert(intercept[JsonParseException](validateSkip("{{}")).getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
    "skip array values" in {
      validateSkip("[]")
      validateSkip(" \n\t\r[[[[[]]]][[[]]]]")
      validateSkip("[\"[\"]")
    }
    "throw parsing exception when skipping not closed array" in {
      assert(intercept[JsonParseException](validateSkip("[[]")).getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
    "skip mixed values" in {
      validateSkip(
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
      def check(invalidInput: String): Unit =
        assert(intercept[JsonParseException](validateSkip(invalidInput))
          .getMessage.contains("expected value, offset: 0x00000000"))

      check("]")
      check("}")
      check(",")
      check(":")
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
  "JsonReader.readBoolean and JsonReader.readStringAsBoolean and JsonReader.readKeyAsBoolean" should {
    "parse valid true and false values" in {
      def check(bytes: Array[Byte], value: Boolean): Unit = {
        reader(bytes).readBoolean() shouldBe value
        reader('\"'.toByte +: bytes :+ '\"'.toByte).readStringAsBoolean() shouldBe value
        reader('\"'.toByte +: bytes :+ '\"'.toByte :+ ':'.toByte).readKeyAsBoolean() shouldBe value
      }

      check("true".getBytes, value = true)
      check("false".getBytes, value = false)
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      def check(bytes: Array[Byte], error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readBoolean())
          .getMessage.contains(error1))
        assert(intercept[JsonParseException](reader('\"'.toByte +: bytes :+ '\"'.toByte).readStringAsBoolean())
          .getMessage.contains(error2))
        assert(intercept[JsonParseException](reader('\"'.toByte +: bytes :+ '\"'.toByte :+ ':'.toByte).readKeyAsBoolean())
          .getMessage.contains(error2))
      }

      check("x".getBytes, "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      check("trae".getBytes, "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      check("folse".getBytes, "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      check("".getBytes, "unexpected end of input, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      check("tru".getBytes, "unexpected end of input, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      check("fals".getBytes, "unexpected end of input, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
    }
  }
  "JsonReader.readString and JsonReader.readKeyAsString" should {
    "parse null value" in {
      reader("null".getBytes).readString() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsString())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null".getBytes).readString("VVV") shouldBe "VVV"
    }
    "parse string with Unicode chars which are not escaped and are non-surrogate" in {
      forAll(minSuccessful(10000)) { (s: String) =>
        whenever(!s.exists(ch => ch == '"' || ch == '\\' || Character.isSurrogate(ch))) {
          readString(s) shouldBe s
          readKeyAsString(s) shouldBe s
        }
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      def check(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readString()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error))
      }

      check("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      check("\"\\".getBytes, "unexpected end of input, offset: 0x00000002")
      assert(intercept[JsonParseException](reader("\"\"".getBytes).readKeyAsString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](reader("\"\"x".getBytes).readKeyAsString())
        .getMessage.contains("expected ':', offset: 0x00000002"))
    }
    "throw parsing exception for boolean values & numbers" in {
      def check(bytes: Array[Byte], error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readString()).getMessage.contains(error1))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error2))
      }

      check("true".getBytes, "expected string value or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
      check("false".getBytes, "expected string value or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
      check("12345".getBytes, "expected string value or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
    }
    "get the same string value for escaped strings as for non-escaped" in {
      def check(s1: String, s2: String): Unit = {
        readString(s1) shouldBe readString(s2)
        readKeyAsString(s1) shouldBe readKeyAsString(s2)
      }

      check("""\b\f\n\r\t\/\\""", "\b\f\n\r\t/\\\\")
      check("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1\\ud834\\udd1e", "\b\f\n\r\t/Aиბ𝄞")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def check(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readString(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsString(s)).getMessage.contains(error2))
      }

      check("\\x0008", "illegal escape sequence, offset: 0x00000002", "illegal escape sequence, offset: 0x00000002")
      check("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      check("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      check("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      check("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      check("\\", "unexpected end of input, offset: 0x00000003", "unexpected end of input, offset: 0x00000004")
      check("\\udd1e", "unexpected end of input, offset: 0x00000008", "unexpected end of input, offset: 0x00000009")
      check("\\ud834", "unexpected end of input, offset: 0x00000008", "unexpected end of input, offset: 0x00000009")
      check("\\ud834\\", "unexpected end of input, offset: 0x00000009", "unexpected end of input, offset: 0x0000000a")
      check("\\ud834\\x", "unexpected end of input, offset: 0x0000000a", "unexpected end of input, offset: 0x0000000b")
      check("\\ud834\\ud834", "illegal surrogate character pair, offset: 0x0000000c",
        "illegal surrogate character pair, offset: 0x0000000c")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def check(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](readString(bytes)).getMessage.contains(error))
        assert(intercept[JsonParseException](readKeyAsString(bytes)).getMessage.contains(error))
      }

      check(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      check(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      check(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      check(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      check(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte), "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      check(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte), "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      check(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte), "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      check(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xf0, 0x80, 0x80, 0x80, offset: 0x00000004")
      check(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x04, 0x9e, offset: 0x00000004")
      check(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0xff, offset: 0x00000004")
      check(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0xff, 0x9e, offset: 0x00000004")
      check(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0xff, 0x84, 0x9e, offset: 0x00000004")
      check(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0x0e, offset: 0x00000004")
    }
  }
  "JsonReader.readChar" should {
    "parse Unicode char that is not escaped and is non-surrogate from string with length == 1" in {
      def check(ch: Char): Unit = {
        readChar(ch.toString) shouldBe ch
        readKeyAsChar(ch.toString) shouldBe ch
      }

      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          check(ch)
        }
      }
    }
    "throw parsing exception for string with length > 1" in {
      def check(s: String, error: String): Unit = {
        assert(intercept[JsonParseException](readChar(s)).getMessage.contains(error))
        assert(intercept[JsonParseException](readKeyAsChar(s)).getMessage.contains(error))
      }

      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          check(ch.toString + ch.toString, "expected '\"'") // offset can differs for non-ASCII characters
        }
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      def check(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readChar()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsChar()).getMessage.contains(error))
      }

      check("".getBytes, "unexpected end of input, offset: 0x00000000")
      check("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      check("\"\\".getBytes, "unexpected end of input, offset: 0x00000002")
    }
    "throw parsing exception for null, boolean values & numbers" in {
      def check(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readChar()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsChar()).getMessage.contains(error))
      }

      check("null".getBytes, "expected '\"', offset: 0x00000000")
      check("true".getBytes, "expected '\"', offset: 0x00000000")
      check("false".getBytes, "expected '\"', offset: 0x00000000")
      check("12345".getBytes, "expected '\"', offset: 0x00000000")
    }
    "get the same char value for escaped strings as for non-escaped" in {
      def check(escaped: String, nonEscaped: String): Unit = {
        readChar(escaped) shouldBe readChar(nonEscaped)
        readKeyAsChar(escaped) shouldBe readKeyAsChar(nonEscaped)
      }

      check("""\b""", "\b")
      check("""\f""", "\f")
      check("""\n""", "\n")
      check("""\r""", "\r")
      check("""\t""", "\t")
      check("""\/""", "/")
      check("""\\""", "\\\\")
      check("\\u0008", "\b")
      check("\\u000C", "\f")
      check("\\u000a", "\n")
      check("\\u000D", "\r")
      check("\\u0009", "\t")
      check("\\u002F", "/")
      check("\\u0041", "A")
      check("\\u0438", "и")
      check("\\u10d1", "ბ")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def check(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readChar(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsChar(s)).getMessage.contains(error2))
      }

      check("\\x0008", "illegal escape sequence, offset: 0x00000002", "illegal escape sequence, offset: 0x00000002")
      check("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      check("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      check("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      check("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      check("\\", "unexpected end of input, offset: 0x00000003", "expected '\"', offset: 0x00000003")
      check("\\udd1e", "illegal surrogate character, offset: 0x00000006", "illegal surrogate character, offset: 0x00000006")
      check("\\ud834", "illegal surrogate character, offset: 0x00000006", "illegal surrogate character, offset: 0x00000006")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def check(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](readChar(bytes)).getMessage.contains(error))
        assert(intercept[JsonParseException](readKeyAsChar(bytes)).getMessage.contains(error))
      }

      check(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      check(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      check(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      check(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      check(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      check(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      check(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte),
        "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      check(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "illegal surrogate character, offset: 0x00000004")
    }
  }
  "JsonReader.readByte and JsonReader.readStringAsByte" should {
    "parse valid byte values" in {
      def check(n: Byte): Unit = {
        val s = n.toString
        readByte(s) shouldBe n
        readKeyAsByte(s) shouldBe n
        readStringAsByte(s) shouldBe n
      }

      forAll(minSuccessful(1000)) { (n: Byte) =>
        check(n)
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
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readByte(s)).getMessage.contains(error))

      check("", "unexpected end of input, offset: 0x00000000")
      check("-", "unexpected end of input, offset: 0x00000001")
      check("x", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on byte overflow" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readByte(s)).getMessage.contains(error))

      check("128", "value is too large for byte, offset: 0x00000002")
      check("-129", "value is too large for byte, offset: 0x00000003")
      check("12345", "value is too large for byte, offset: 0x00000003")
      check("-12345", "value is too large for byte, offset: 0x00000004")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readByte(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("0123", "illegal number with leading zero, offset: 0x00000000")
      check("-0123", "illegal number with leading zero, offset: 0x00000001")
      check("0128", "illegal number with leading zero, offset: 0x00000000")
      check("-0128", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readShort" should {
    "parse valid short values" in {
      def check(n: Short): Unit = {
        val s = n.toString
        readShort(s) shouldBe n
        readKeyAsShort(s) shouldBe n
        readStringAsShort(s) shouldBe n
      }

      forAll(minSuccessful(10000)) { (n: Short) =>
        check(n)
      }
    }
    "parse valid short values with skipping of JSON space characters" in {
      readShort(" \n\t\r12345") shouldBe 12345.toShort
      readShort(" \n\t\r-12345") shouldBe -12345.toShort
    }
    "parse valid short values and stops on not numeric chars" in {
      readShort("0$") shouldBe 0
    }
    "throw parsing exception on illegal or empty input" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readShort(s)).getMessage.contains(error))

      check("", "unexpected end of input, offset: 0x00000000")
      check("-", "unexpected end of input, offset: 0x00000001")
      check("x", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on short overflow" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readShort(s)).getMessage.contains(error))

      check("32768", "value is too large for short, offset: 0x00000004")
      check("-32769", "value is too large for short, offset: 0x00000005")
      check("12345678901", "value is too large for short, offset: 0x00000005")
      check("-12345678901", "value is too large for short, offset: 0x00000006")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readShort(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("012345", "illegal number with leading zero, offset: 0x00000000")
      check("-012345", "illegal number with leading zero, offset: 0x00000001")
      check("032767", "illegal number with leading zero, offset: 0x00000000")
      check("-032768", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readInt" should {
    "parse valid int values" in {
      def check(n: Int): Unit = {
        val s = n.toString
        readInt(s) shouldBe n
        readKeyAsInt(s) shouldBe n
        readStringAsInt(s) shouldBe n
      }

      forAll(minSuccessful(10000)) { (n: Int) =>
        check(n)
      }
    }
    "parse valid int values with skipping of JSON space characters" in {
      readInt(" \n\t\r123456789") shouldBe 123456789
      readInt(" \n\t\r-123456789") shouldBe -123456789
    }
    "parse valid int values and stops on not numeric chars" in {
      readInt("0$") shouldBe 0
    }
    "throw parsing exception on illegal or empty input" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readInt(s)).getMessage.contains(error))

      check("", "unexpected end of input, offset: 0x00000000")
      check("-", "unexpected end of input, offset: 0x00000001")
      check("x", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on int overflow" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readInt(s)).getMessage.contains(error))

      check("2147483648", "value is too large for int, offset: 0x00000009")
      check("-2147483649", "value is too large for int, offset: 0x0000000a")
      check("12345678901", "value is too large for int, offset: 0x0000000a")
      check("-12345678901", "value is too large for int, offset: 0x0000000b")
      check("12345678901234567890", "value is too large for int, offset: 0x0000000a")
      check("-12345678901234567890", "value is too large for int, offset: 0x0000000b")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readInt(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("0123456789", "illegal number with leading zero, offset: 0x00000000")
      check("-0123456789", "illegal number with leading zero, offset: 0x00000001")
      check("02147483647", "illegal number with leading zero, offset: 0x00000000")
      check("-02147483648", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readLong" should {
    "parse valid long values" in {
      def check(n: Long): Unit = {
        val s = n.toString
        readLong(s) shouldBe n
        readKeyAsLong(s) shouldBe n
        readStringAsLong(s) shouldBe n
      }

      forAll(minSuccessful(10000)) { (n: Long) =>
        check(n)
      }
    }
    "parse valid long values with skipping of JSON space characters" in {
      readLong(" \n\t\r1234567890123456789") shouldBe 1234567890123456789L
      readLong(" \n\t\r-1234567890123456789") shouldBe -1234567890123456789L
    }
    "parse valid long values and stops on not numeric chars" in {
      readLong("0$") shouldBe 0L
    }
    "throw parsing exception on illegal or empty input" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readLong(s)).getMessage.contains(error))

      check("", "unexpected end of input, offset: 0x00000000")
      check("-", "unexpected end of input, offset: 0x00000001")
      check("x", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on long overflow" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readLong(s)).getMessage.contains(error))

      check("9223372036854775808", "value is too large for long, offset: 0x00000012")
      check("-9223372036854775809", "value is too large for long, offset: 0x00000013")
      check("12345678901234567890", "value is too large for long, offset: 0x00000013")
      check("-12345678901234567890", "value is too large for long, offset: 0x00000014")
      check("123456789012345678901234567890", "value is too large for long, offset: 0x00000013")
      check("-123456789012345678901234567890", "value is too large for long, offset: 0x00000014")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readLong(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("01234567890123456789", "illegal number with leading zero, offset: 0x00000000")
      check("-01234567890123456789", "illegal number with leading zero, offset: 0x00000001")
      check("09223372036854775807", "illegal number with leading zero, offset: 0x00000000")
      check("-09223372036854775808", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readFloat" should {
    "parse valid float values" in {
      def check(n: BigDecimal): Unit = {
        val s = n.toString
        val f = java.lang.Float.parseFloat(s)
        readFloat(s) shouldBe f
        readKeyAsFloat(s) shouldBe f
        readStringAsFloat(s) shouldBe f
      }

      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        check(n)
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
    "parse valid float values with skipping of JSON space characters" in {
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
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readFloat(s)).getMessage.contains(error))

      check("", "illegal number, offset: 0x00000000")
      check(" ", "illegal number, offset: 0x00000001")
      check("-", "illegal number, offset: 0x00000001")
      check("$", "illegal number, offset: 0x00000000")
      check(" $", "illegal number, offset: 0x00000001")
      check("-$", "illegal number, offset: 0x00000001")
      check("0e$", "illegal number, offset: 0x00000002")
      check("0e-$", "illegal number, offset: 0x00000003")
      check("0.E", "illegal number, offset: 0x00000002")
      check("0.+", "illegal number, offset: 0x00000002")
      check("0.-", "illegal number, offset: 0x00000002")
      check("NaN", "illegal number, offset: 0x00000000")
      check("Inf", "illegal number, offset: 0x00000000")
      check("Infinity", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readFloat(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      check("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readDouble" should {
    "parse valid double values" in {
      def check(n: BigDecimal): Unit = {
        val s = n.toString
        val d = java.lang.Double.parseDouble(s)
        readDouble(s) shouldBe d
        readKeyAsDouble(s) shouldBe d
        readStringAsDouble(s) shouldBe d
      }

      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        check(n)
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
    "parse valid double values with skipping of JSON space characters" in {
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
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readDouble(s)).getMessage.contains(error))

      check("", "illegal number, offset: 0x00000000")
      check(" ", "illegal number, offset: 0x00000001")
      check("-", "illegal number, offset: 0x00000001")
      check("$", "illegal number, offset: 0x00000000")
      check(" $", "illegal number, offset: 0x00000001")
      check("-$", "illegal number, offset: 0x00000001")
      check("0e$", "illegal number, offset: 0x00000002")
      check("0e-$", "illegal number, offset: 0x00000003")
      check("0.E", "illegal number, offset: 0x00000002")
      check("0.-", "illegal number, offset: 0x00000002")
      check("0.+", "illegal number, offset: 0x00000002")
      check("NaN", "illegal number, offset: 0x00000000")
      check("Inf", "illegal number, offset: 0x00000000")
      check("Infinity", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readDouble(s)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      check("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigInt" should {
    "parse null value" in {
      readBigInt("null", null) shouldBe null
      reader("null".getBytes).readStringAsBigInt(null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigInt("null", BigInt("12345")) shouldBe BigInt("12345")
      reader("null".getBytes).readStringAsBigInt(BigInt("12345")) shouldBe BigInt("12345")
    }
    "parse valid number values" in {
      def check(n: BigInt): Unit = {
        val s = n.toString
        readBigInt(s, null) shouldBe n
        readKeyAsBigInt(s) shouldBe n
        readStringAsBigInt(s, null) shouldBe n
      }

      forAll(minSuccessful(10000)) { (n: BigInt) =>
        check(n)
      }
    }
    "parse big number values without overflow" in {
      val bigNumber = "12345" + new String(Array.fill(6789)('0'))
      readBigInt(bigNumber, null) shouldBe BigInt(bigNumber)
      readBigInt("-" + bigNumber, null) shouldBe BigInt("-" + bigNumber)
    }
    "parse valid number values with skipping of JSON space characters" in {
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
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigInt(s, null)).getMessage.contains(error))

      check("", "unexpected end of input, offset: 0x00000000")
      check(" ", "unexpected end of input, offset: 0x00000001")
      check("-", "unexpected end of input, offset: 0x00000001")
      check("$", "illegal number, offset: 0x00000000")
      check(" $", "illegal number, offset: 0x00000001")
      check("-$", "illegal number, offset: 0x00000001")
      check("NaN", "illegal number, offset: 0x00000000")
      check("Inf", "illegal number, offset: 0x00000000")
      check("Infinity", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigInt(s, null)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("012345", "illegal number with leading zero, offset: 0x00000000")
      check("-012345", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigDecimal" should {
    "parse null value" in {
      readBigDecimal("null", null) shouldBe null
      reader("null".getBytes).readStringAsBigDecimal(null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigDecimal("null", BigDecimal("12345")) shouldBe BigDecimal("12345")
      reader("null".getBytes).readStringAsBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
    "parse valid number values" in {
      def check(n: BigDecimal): Unit = {
        val s = n.toString
        readBigDecimal(s, null) shouldBe n
        readKeyAsBigDecimal(s) shouldBe n
        readStringAsBigDecimal(s, null) shouldBe n
      }

      forAll(minSuccessful(10000)) { (n: BigDecimal) =>
        check(n)
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
    "parse valid number values with skipping of JSON space characters" in {
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
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigDecimal(s, null)).getMessage.contains(error))

      check("", "illegal number, offset: 0x00000000")
      check(" ", "illegal number, offset: 0x00000001")
      check("-", "illegal number, offset: 0x00000001")
      check("$", "illegal number, offset: 0x00000000")
      check(" $", "illegal number, offset: 0x00000001")
      check("-$", "illegal number, offset: 0x00000001")
      check("0e$", "illegal number, offset: 0x00000002")
      check("0e-$", "illegal number, offset: 0x00000003")
      check("0.E", "illegal number, offset: 0x00000002")
      check("0.-", "illegal number, offset: 0x00000002")
      check("0.+", "illegal number, offset: 0x00000002")
      check("NaN", "illegal number, offset: 0x00000000")
      check("Inf", "illegal number, offset: 0x00000000")
      check("Infinity", "illegal number, offset: 0x00000000")
    }
    "throw parsing exception on leading zero" in {
      def check(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigDecimal(s, null)).getMessage.contains(error))

      check("00", "illegal number with leading zero, offset: 0x00000000")
      check("-00", "illegal number with leading zero, offset: 0x00000001")
      check("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      check("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.requiredKeyError" should {
    val jsonReader = reader("{}".getBytes)
    jsonReader.nextToken()
    "throw parsing exception with list of missing required fields that specified by bits" in {
      def check(bits: Int, error: String): Unit =
        assert(intercept[JsonParseException](jsonReader.requiredKeyError(Array("name", "device"), bits))
          .getMessage.contains(error))

      check(3, "missing required field(s) \"name\", \"device\", offset: 0x00000000")
      check(2, "missing required field(s) \"device\", offset: 0x00000000")
      check(1, "missing required field(s) \"name\", offset: 0x00000000")
    }
    "throw illegal argument exception in case of missing required fields cannot be selected" in {
      assert(intercept[IllegalArgumentException](jsonReader.requiredKeyError(Array("name", "device"), 0))
        .getMessage.contains("missing required field(s) cannot be reported for arguments: " +
          "reqFields = Array(name, device), reqBits = WrappedArray(0)"))
    }
  }
  "JsonReader.unexpectedKeyError" should {
    "throw parsing exception with name of unexpected key" in {
      val jsonReader = reader("\"xxx\"".getBytes)
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonParseException](jsonReader.unexpectedKeyError(len))
        .getMessage.contains("unexpected field: \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.discriminatorValueError" should {
    val jsonReader = reader("\"xxx\"".getBytes)
    val value = jsonReader.readString(null)
    "throw parsing exception with unexpected discriminator value" in {
      assert(intercept[JsonParseException](jsonReader.discriminatorValueError(value))
       .getMessage.contains("illegal value of discriminator field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.enumValueError" should {
    val jsonReader = reader("\"xxx\"".getBytes)
    val value = jsonReader.readString(null)
    "throw parsing exception with unexpected enum value" in {
      assert(intercept[JsonParseException](jsonReader.enumValueError(value))
        .getMessage.contains("illegal enum value: \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.arrayStartError" should {
    val jsonReader = reader("{}".getBytes)
    jsonReader.isNextToken('[')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayStartError())
        .getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndError" should {
    val jsonReader = reader("}".getBytes)
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayEndError())
        .getMessage.contains("expected ']' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader.objectStartError" should {
    val jsonReader = reader("[]".getBytes)
    jsonReader.isNextToken('{')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectStartError())
        .getMessage.contains("expected '{' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.objectEndError" should {
    val jsonReader = reader("]".getBytes)
    jsonReader.isNextToken('}')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectEndError())
        .getMessage.contains("expected '}' or ',', offset: 0x00000000"))
    }
  }

  def validateSkip(s: String): Unit = {
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

  def readKeyAsString(s: String): String = readKeyAsString(s.getBytes(UTF_8))

  def readKeyAsString(buf: Array[Byte]): String = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsString()

  def readKeyAsChar(s: String): Char = readKeyAsChar(s.getBytes(UTF_8))

  def readKeyAsChar(buf: Array[Byte]): Char = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsChar()

  def readKeyAsByte(s: String): Byte = readKeyAsByte(s.getBytes(UTF_8))

  def readKeyAsByte(buf: Array[Byte]): Byte = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsByte()

  def readKeyAsShort(s: String): Short = readKeyAsShort(s.getBytes(UTF_8))

  def readKeyAsShort(buf: Array[Byte]): Short = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsShort()

  def readKeyAsInt(s: String): Int = readKeyAsInt(s.getBytes(UTF_8))

  def readKeyAsInt(buf: Array[Byte]): Int = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsInt()

  def readKeyAsLong(s: String): Long = readKeyAsLong(s.getBytes(UTF_8))

  def readKeyAsLong(buf: Array[Byte]): Long = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsLong()

  def readKeyAsFloat(s: String): Float = readKeyAsFloat(s.getBytes(UTF_8))

  def readKeyAsFloat(buf: Array[Byte]): Float = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsFloat()

  def readKeyAsDouble(s: String): Double = readKeyAsDouble(s.getBytes(UTF_8))

  def readKeyAsDouble(buf: Array[Byte]): Double = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsDouble()

  def readKeyAsBigInt(s: String): BigInt = readKeyAsBigInt(s.getBytes(UTF_8))

  def readKeyAsBigInt(buf: Array[Byte]): BigInt = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsBigInt()

  def readKeyAsBigDecimal(s: String): BigDecimal = readKeyAsBigDecimal(s.getBytes(UTF_8))

  def readKeyAsBigDecimal(buf: Array[Byte]): BigDecimal = reader('"'.toByte +: buf :+ '"'.toByte :+ ':'.toByte).readKeyAsBigDecimal()

  def readStringAsByte(s: String): Byte = readStringAsByte(s.getBytes(UTF_8))

  def readStringAsByte(buf: Array[Byte]): Byte = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsByte()

  def readStringAsShort(s: String): Short = readStringAsShort(s.getBytes(UTF_8))

  def readStringAsShort(buf: Array[Byte]): Short = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsShort()

  def readStringAsInt(s: String): Int = readStringAsInt(s.getBytes(UTF_8))

  def readStringAsInt(buf: Array[Byte]): Int = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsInt()

  def readStringAsLong(s: String): Long = readStringAsLong(s.getBytes(UTF_8))

  def readStringAsLong(buf: Array[Byte]): Long = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsLong()

  def readStringAsFloat(s: String): Float = readStringAsFloat(s.getBytes(UTF_8))

  def readStringAsFloat(buf: Array[Byte]): Float = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsFloat()

  def readStringAsDouble(s: String): Double = readStringAsDouble(s.getBytes(UTF_8))

  def readStringAsDouble(buf: Array[Byte]): Double = reader('"'.toByte +: buf :+ '"'.toByte).readStringAsDouble()

  def readStringAsBigInt(s: String, default: BigInt): BigInt = readStringAsBigInt(s.getBytes(UTF_8), default)

  def readStringAsBigInt(buf: Array[Byte], default: BigInt): BigInt =
    reader('"'.toByte +: buf :+ '"'.toByte).readStringAsBigInt(default)

  def readStringAsBigDecimal(s: String, default: BigDecimal): BigDecimal =
    readStringAsBigDecimal(s.getBytes(UTF_8), default)

  def readStringAsBigDecimal(buf: Array[Byte], default: BigDecimal): BigDecimal =
    reader('"'.toByte +: buf :+ '"'.toByte).readStringAsBigDecimal(default)

  def reader(buf: Array[Byte]): JsonReader = new JsonReader(new Array[Byte](12), // a minimal allowed length of `buf`
    0, 0, -1, new Array[Char](0), new ByteArrayInputStream(buf), 0, ReaderConfig())
}