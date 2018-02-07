package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalacheck.Gen
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
      assert(intercept[JsonParseException](validateSkip("\""))
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](validateSkip("\"abc"))
        .getMessage.contains("unexpected end of input, offset: 0x00000005"))
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
      assert(intercept[JsonParseException](validateSkip("t"))
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](validateSkip("f"))
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
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
      assert(intercept[JsonParseException](validateSkip("{{}"))
        .getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
    "skip array values" in {
      validateSkip("[]")
      validateSkip(" \n\t\r[[[[[]]]][[[]]]]")
      validateSkip("[\"[\"]")
    }
    "throw parsing exception when skipping not closed array" in {
      assert(intercept[JsonParseException](validateSkip("[[]"))
        .getMessage.contains("unexpected end of input, offset: 0x00000004"))
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
  "JsonReader.readBoolean, JsonReader.readStringAsBoolean and JsonReader.readKeyAsBoolean" should {
    def check(s: String, value: Boolean): Unit = {
      reader(s.getBytes).readBoolean() shouldBe value
      reader(('\"' + s + '\"').getBytes).readStringAsBoolean() shouldBe value
      reader(('\"' + s + "\":").getBytes).readKeyAsBoolean() shouldBe value
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](reader(s.getBytes).readBoolean())
        .getMessage.contains(error1))
      assert(intercept[JsonParseException](reader(('\"' + s + '\"').getBytes).readStringAsBoolean())
        .getMessage.contains(error2))
      assert(intercept[JsonParseException](reader(('\"' + s + "\":").getBytes).readKeyAsBoolean())
        .getMessage.contains(error2))
    }

    "parse valid true and false values" in {
      check("true", value = true)
      check("false", value = false)
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      checkError("x", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("trae", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      checkError("folse", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("tru", "unexpected end of input, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("fals", "unexpected end of input, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
    }
  }
  "JsonReader.readKeyAsUUID" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"00000000-0000-0000-0000-000000000000\"".getBytes).readKeyAsUUID())
        .getMessage.contains("unexpected end of input, offset: 0x00000026"))
      assert(intercept[JsonParseException](reader("\"00000000-0000-0000-0000-000000000000\"x".getBytes).readKeyAsUUID())
        .getMessage.contains("expected ':', offset: 0x00000026"))
    }
  }
  "JsonReader.readUUID and JsonReader.readKeyAsUUID" should {
    "parse null value" in {
      reader("null".getBytes).readUUID() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsUUID())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = new UUID(0, 0)
      reader("null".getBytes).readUUID(default) shouldBe default
    }
    "parse UUID from a string representation according to format that defined in IETF RFC4122 (section 3)" in {
      def check(x: UUID): Unit = {
        val s = x.toString
        readUUID(s.toLowerCase) shouldBe x
        readUUID(s.toUpperCase) shouldBe x
        readKeyAsUUID(s.toLowerCase) shouldBe x
        readKeyAsUUID(s.toUpperCase) shouldBe x
      }

      forAll(Gen.uuid, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken UUID string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readUUID()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsUUID()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "unexpected end of input, offset: 0x00000002")
      checkError("\"00000000-0000-0000-0000-000000000000".getBytes, "unexpected end of input, offset: 0x00000025")
      checkError("\"Z0000000-0000-0000-0000-000000000000\"".getBytes, "expected hex digit, offset: 0x00000001")
      checkError("\"00000000=0000-0000-0000-000000000000\"".getBytes, "expected '-', offset: 0x00000009")
    }
  }
  "JsonReader.readKeyAsInstant" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"2008-01-20T07:24:33Z\"".getBytes).readKeyAsInstant())
        .getMessage.contains("unexpected end of input, offset: 0x00000016"))
      assert(intercept[JsonParseException](reader("\"2008-01-20T07:24:33Z\"x".getBytes).readKeyAsInstant())
        .getMessage.contains("expected ':', offset: 0x00000016"))
    }
  }
  "JsonReader.readDuration and JsonReader.readKeyAsDuration" should {
    "parse null value" in {
      reader("null".getBytes).readDuration() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsDuration())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Duration.parse("P2DT3H4M")
      reader("null".getBytes).readDuration(default) shouldBe default
    }
    "parse Duration from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(x: Duration, s: String): Unit = {
        readDuration(s) shouldBe x
        readKeyAsDuration(s) shouldBe x
      }

      check(Duration.ZERO, "PT0S")
      forAll(genDuration, minSuccessful(100000))(x => check(x, x.toString))
    }
    "throw parsing exception for empty input and illegal or broken Duration string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readDuration()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsDuration()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"".getBytes, "expected 'P', offset: 0x00000002")
      checkError("\"PXD\"".getBytes, "expected '-' or digit, offset: 0x00000002")
      checkError("\"PT0SX".getBytes, "expected '\"', offset: 0x00000005")
      checkError("\"P-XD\"".getBytes, "expected digit, offset: 0x00000003")
      checkError("\"P1XD\"".getBytes, "expected 'D' or digit, offset: 0x00000003")
      checkError("\"P106751991167301D\"".getBytes, "llegal duration, offset: 0x00000011")
      checkError("\"P-106751991167301D\"".getBytes, "llegal duration, offset: 0x00000012")
      checkError("\"P1DX1H\"".getBytes, "expected 'T' or '\"', offset: 0x00000004")
      checkError("\"P1DTXH\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"P1DT-XH\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"P1DT1XH\"".getBytes, "expected 'H' or 'M' or 'S or '.' or digit, offset: 0x00000006")
      checkError("\"P0DT2562047788015216H\"".getBytes, "illegal duration, offset: 0x00000015")
      checkError("\"P0DT-2562047788015216H\"".getBytes, "illegal duration, offset: 0x00000016")
      checkError("\"P0DT153722867280912931M\"".getBytes, "illegal duration, offset: 0x00000017")
      checkError("\"P0DT-153722867280912931M\"".getBytes, "illegal duration, offset: 0x00000018")
      checkError("\"P0DT9223372036854775808S\"".getBytes, "illegal duration, offset: 0x00000018")
      checkError("\"P0DT-9223372036854775809S\"".getBytes, "illegal duration, offset: 0x00000018")
      checkError("\"P1DT1HXM\"".getBytes, "expected '\"' or '-' or digit, offset: 0x00000007")
      checkError("\"P1DT1H-XM\"".getBytes, "expected digit, offset: 0x00000008")
      checkError("\"P1DT1H1XM\"".getBytes, "expected 'M' or 'S or '.' or digit, offset: 0x00000008")
      checkError("\"P0DT0H153722867280912931M\"".getBytes, "illegal duration, offset: 0x00000019")
      checkError("\"P0DT0H-153722867280912931M\"".getBytes, "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H9223372036854775808S\"".getBytes, "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H-9223372036854775809S\"".getBytes, "illegal duration, offset: 0x0000001a")
      checkError("\"P1DT1H1MXS\"".getBytes, "expected '\"' or '-' or digit, offset: 0x00000009")
      checkError("\"P1DT1H1M-XS\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0XS\"".getBytes, "expected 'S or '.' or digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0.XS\"".getBytes, "expected '\"' or digit, offset: 0x0000000b")
      checkError("\"P1DT1H1M0.012345678XS\"".getBytes, "expected 'S', offset: 0x00000014")
      checkError("\"P1DT1H1M0.0123456789S\"".getBytes, "expected 'S', offset: 0x00000014")
      checkError("\"P0DT0H0M9223372036854775808S\"".getBytes, "illegal duration, offset: 0x0000001c")
      checkError("\"P106751991167300DT24H\"".getBytes, "illegal duration, offset: 0x00000017")
      checkError("\"P0DT2562047788015215H60M\"".getBytes, "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H153722867280912930M60S\"".getBytes, "illegal duration, offset: 0x0000001e")
    }
  }
  "JsonReader.readInstant and JsonReader.readKeyAsInstant" should {
    "parse null value" in {
      reader("null".getBytes).readInstant() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsInstant())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Instant.parse("2008-01-20T07:24:33Z")
      reader("null".getBytes).readInstant(default) shouldBe default
    }
    "parse Instant from a string representation according to ISO-8601 format" in {
      def check(x: Instant): Unit = {
        val s = x.toString
        readInstant(s) shouldBe x
        readKeyAsInstant(s) shouldBe x
      }

      check(Instant.MAX)
      check(Instant.MIN)
      forAll(genInstant, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken Instant string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readInstant()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsInstant()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z".getBytes, "unexpected end of input, offset: 0x00000015")
      checkError("\"008-01-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X0-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"".getBytes, "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"".getBytes, "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"".getBytes, "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"".getBytes, "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"".getBytes, "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"".getBytes, "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"".getBytes, "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"".getBytes, "expected ':', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"".getBytes, "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes, "expected 'Z' or '.', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes, "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"".getBytes, "expected 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"".getBytes, "expected 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X\"".getBytes, "expected 'Z', offset: 0x0000001e")
      checkError("\"+1000000000=01-20T07:24:33Z\"".getBytes, "expected '-', offset: 0x0000000c")
      checkError("\"+1000000001-01-20T07:24:33Z\"".getBytes, "illegal year, offset: 0x0000001c")
      checkError("\"-1000000001-01-20T07:24:33Z\"".getBytes, "illegal year, offset: 0x0000001c")
      checkError("\"2008-00-20T07:24:33Z\"".getBytes, "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"".getBytes, "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-01-20T24:24:33Z\"".getBytes, "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"".getBytes, "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"".getBytes, "illegal second, offset: 0x00000015")
    }
  }
  "JsonReader.readLocalDate and JsonReader.readKeyAsLocalDate" should {
    "parse null value" in {
      reader("null".getBytes).readLocalDate() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsLocalDate())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDate.parse("2008-01-20")
      reader("null".getBytes).readLocalDate(default) shouldBe default
    }
    "parse LocalDate from a string representation according to ISO-8601 format" in {
      def check(x: LocalDate): Unit = {
        val s = x.toString
        readLocalDate(s) shouldBe x
        readKeyAsLocalDate(s) shouldBe x
      }

      check(LocalDate.MAX)
      check(LocalDate.MIN)
      forAll(genLocalDate, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDate string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readLocalDate()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalDate()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20".getBytes, "unexpected end of input, offset: 0x0000000b")
      checkError("\"008-01-20\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X1-20\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20\"".getBytes, "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2X\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X\"".getBytes, "expected '\"', offset: 0x0000000b")
      checkError("\"+1000000000-01-20\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"2008-00-20\"".getBytes, "illegal month, offset: 0x0000000b")
      checkError("\"2008-13-20\"".getBytes, "illegal month, offset: 0x0000000b")
      checkError("\"2008-01-00\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-01-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-02-30\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-03-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-04-31\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-05-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-06-31\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-07-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-08-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-09-31\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-10-32\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-11-31\"".getBytes, "illegal day, offset: 0x0000000b")
      checkError("\"2008-12-32\"".getBytes, "illegal day, offset: 0x0000000b")
    }
  }
  "JsonReader.readLocalDateTime and JsonReader.readKeyAsLocalDateTime" should {
    "parse null value" in {
      reader("null".getBytes).readLocalDateTime() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsLocalDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDateTime.parse("2008-01-20T07:24:33")
      reader("null".getBytes).readLocalDateTime(default) shouldBe default
    }
    "parse LocalDateTime from a string representation according to ISO-8601 format" in {
      def check(x: LocalDateTime): Unit = {
        val s = x.toString
        readLocalDateTime(s) shouldBe x
        readKeyAsLocalDateTime(s) shouldBe x
      }

      check(LocalDateTime.MAX)
      check(LocalDateTime.MIN)
      forAll(genLocalDateTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readLocalDateTime()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33".getBytes, "unexpected end of input, offset: 0x00000014")
      checkError("\"008-01-20T07:24:33\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X1-20T07:24:33\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33\"".getBytes, "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33\"".getBytes, "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33\"".getBytes, "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33\"".getBytes, "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33\"".getBytes, "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33\"".getBytes, "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33\"".getBytes, "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33\"".getBytes, "expected ':' or '\"', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3X\"".getBytes, "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes, "expected '.' or '\"', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33.X\"".getBytes, "expected '\"' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.123456789X\"".getBytes, "expected '\"', offset: 0x0000001e")
      checkError("\"+1000000000-01-20T07:24:33\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"2008-00-20T07:24:33\"".getBytes, "illegal month, offset: 0x00000014")
      checkError("\"2008-13-20T07:24:33\"".getBytes, "illegal month, offset: 0x00000014")
      checkError("\"2008-01-00T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-01-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-02-30T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-03-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-04-31T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-05-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-06-31T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-07-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-08-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-09-31T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-10-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-11-31T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-12-32T07:24:33\"".getBytes, "illegal day, offset: 0x00000014")
      checkError("\"2008-01-20T24:24:33\"".getBytes, "illegal hour, offset: 0x00000014")
      checkError("\"2008-01-20T07:60:33\"".getBytes, "illegal minute, offset: 0x00000014")
      checkError("\"2008-01-20T07:24:60\"".getBytes, "illegal second, offset: 0x00000014")
    }
  }
  "JsonReader.readLocalTime and JsonReader.readKeyAsLocalTime" should {
    "parse null value" in {
      reader("null".getBytes).readLocalTime() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsLocalTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalTime.parse("07:24:33")
      reader("null".getBytes).readLocalTime(default) shouldBe default
    }
    "parse LocalTime from a string representation according to ISO-8601 format" in {
      def check(x: LocalTime): Unit = {
        val s = x.toString
        readLocalTime(s) shouldBe x
        readKeyAsLocalTime(s) shouldBe x
      }

      check(LocalTime.MAX)
      check(LocalTime.MIN)
      forAll(genLocalTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readLocalTime()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected digit, offset: 0x00000001")
      checkError("\"07:24:33".getBytes, "unexpected end of input, offset: 0x00000009")
      checkError("\"7:24:33\"".getBytes, "expected digit, offset: 0x00000002")
      checkError("\"07=24:33\"".getBytes, "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33\"".getBytes, "expected digit, offset: 0x00000005")
      checkError("\"07:24=33\"".getBytes, "expected ':' or '\"', offset: 0x00000006")
      checkError("\"07:24:X3\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"07:24:3X\"".getBytes, "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"".getBytes, "expected '.' or '\"', offset: 0x00000009")
      checkError("\"07:24:33.X\"".getBytes, "expected '\"' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"".getBytes, "expected '\"', offset: 0x00000013")
      checkError("\"24:24:33\"".getBytes, "illegal hour, offset: 0x00000009")
      checkError("\"07:60:33\"".getBytes, "illegal minute, offset: 0x00000009")
      checkError("\"07:24:60\"".getBytes, "illegal second, offset: 0x00000009")
    }
  }
  "JsonReader.readMonthDay and JsonReader.readKeyAsMonthDay" should {
    "parse null value" in {
      reader("null".getBytes).readMonthDay() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsMonthDay())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = MonthDay.parse("--01-20")
      reader("null".getBytes).readMonthDay(default) shouldBe default
    }
    "parse MonthDay from a string representation according to ISO-8601 format" in {
      def check(x: MonthDay): Unit = {
        val s = x.toString
        readMonthDay(s) shouldBe x
        readKeyAsMonthDay(s) shouldBe x
      }

      check(MonthDay.of(12, 31))
      check(MonthDay.of(1, 1))
      forAll(genMonthDay, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readMonthDay()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsMonthDay()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-', offset: 0x00000001")
      checkError("\"--01-20".getBytes, "unexpected end of input, offset: 0x00000008")
      checkError("\"-01-20\"".getBytes, "expected '-', offset: 0x00000002")
      checkError("\"---01-20\"".getBytes, "expected digit, offset: 0x00000003")
      checkError("\"--0X-20\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"--01=20\"".getBytes, "expected '-', offset: 0x00000005")
      checkError("\"--01-X0\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"--01-2X\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"--01-20X\"".getBytes, "expected '\"', offset: 0x00000008")
      checkError("\"--00-20\"".getBytes, "illegal month, offset: 0x00000008")
      checkError("\"--13-20\"".getBytes, "illegal month, offset: 0x00000008")
      checkError("\"--01-00\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--01-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--02-30\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--03-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--04-31\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--05-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--06-31\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--07-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--08-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--09-31\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--10-32\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--11-31\"".getBytes, "illegal day, offset: 0x00000008")
      checkError("\"--12-32\"".getBytes, "illegal day, offset: 0x00000008")
    }
  }
  "JsonReader.readOffsetDateTime and JsonReader.readKeyAsOffsetDateTime" should {
    "parse null value" in {
      reader("null".getBytes).readOffsetDateTime() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsOffsetDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetDateTime.parse("2008-01-20T07:24Z")
      reader("null".getBytes).readOffsetDateTime(default) shouldBe default
    }
    "parse OffsetDateTime from a string representation according to ISO-8601 format" in {
      def check(x: OffsetDateTime): Unit = {
        val s = x.toString
        readOffsetDateTime(s) shouldBe x
        readKeyAsOffsetDateTime(s) shouldBe x
      }

      check(OffsetDateTime.MAX)
      check(OffsetDateTime.MIN)
      forAll(genOffsetDateTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken OffsetDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readOffsetDateTime()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsOffsetDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z".getBytes, "unexpected end of input, offset: 0x00000015")
      checkError("\"008-01-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X1-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"".getBytes, "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"".getBytes, "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"".getBytes, "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"".getBytes, "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"".getBytes, "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"".getBytes, "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"".getBytes, "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"".getBytes, "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"".getBytes, "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes, "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes, "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"".getBytes, "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"".getBytes, "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"+1000000000-01-20T07:24:33Z\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"2008-00-20T07:24:33Z\"".getBytes, "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"".getBytes, "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"".getBytes, "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33.123456789X\"".getBytes, "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"2008-01-20T24:24:33Z\"".getBytes, "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"".getBytes, "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"".getBytes, "illegal second, offset: 0x00000015")
      checkError("\"2008-01-20T07:24+\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24-\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:33+\"".getBytes, "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-\"".getBytes, "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+\"".getBytes, "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1\"".getBytes, "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=\"".getBytes, "expected ':' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:\"".getBytes, "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1\"".getBytes, "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10=10\"".getBytes, "expected ':' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0\"".getBytes, "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X\"".getBytes, "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+18:10\"".getBytes, "illegal zone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.-18:10\"".getBytes, "illegal zone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+20:10\"".getBytes, "illegal zone offset hour, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:90\"".getBytes, "illegal zone offset minute, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:60\"".getBytes, "illegal zone offset second, offset: 0x0000001e")
    }
  }
  "JsonReader.readOffsetTime and JsonReader.readKeyAsOffsetTime" should {
    "parse null value" in {
      reader("null".getBytes).readOffsetTime() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsOffsetTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetTime.parse("07:24:33+01:00")
      reader("null".getBytes).readOffsetTime(default) shouldBe default
    }
    "parse OffsetTime from a string representation according to ISO-8601 format" in {
      def check(x: OffsetTime): Unit = {
        val s = x.toString
        readOffsetTime(s) shouldBe x
        readKeyAsOffsetTime(s) shouldBe x
      }

      check(OffsetTime.MAX)
      check(OffsetTime.MIN)
      forAll(genOffsetTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken OffsetTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readOffsetTime()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsOffsetTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected digit, offset: 0x00000001")
      checkError("\"07:24:33Z".getBytes, "unexpected end of input, offset: 0x0000000a")
      checkError("\"7:24:33Z\"".getBytes, "expected digit, offset: 0x00000002")
      checkError("\"07=24:33Z\"".getBytes, "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33Z\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33Z\"".getBytes, "expected digit, offset: 0x00000005")
      checkError("\"07:24=33Z\"".getBytes, "expected ':' or '+' or '-' or 'Z', offset: 0x00000006")
      checkError("\"07:24:X3Z\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"07:24:3XZ\"".getBytes, "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"".getBytes, "expected '.' or '+' or '-' or 'Z', offset: 0x00000009")
      checkError("\"07:24:33.\"".getBytes, "expected '+' or '-' or 'Z' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"".getBytes, "expected '+' or '-' or 'Z', offset: 0x00000013")
      checkError("\"24:24:33Z\"".getBytes, "illegal hour, offset: 0x0000000a")
      checkError("\"07:60:33Z\"".getBytes, "illegal minute, offset: 0x0000000a")
      checkError("\"07:24:60Z\"".getBytes, "illegal second, offset: 0x0000000a")
      checkError("\"07:24+\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"07:24-\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"07:24:33+\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33-\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33.+\"".getBytes, "expected digit, offset: 0x0000000b")
      checkError("\"07:24:33.+1\"".getBytes, "expected digit, offset: 0x0000000c")
      checkError("\"07:24:33.+10=\"".getBytes, "expected ':' or '\"', offset: 0x0000000d")
      checkError("\"07:24:33.+10:\"".getBytes, "expected digit, offset: 0x0000000e")
      checkError("\"07:24:33.+10:1\"".getBytes, "expected digit, offset: 0x0000000f")
      checkError("\"07:24:33.+10:10=10\"".getBytes, "expected ':' or '\"', offset: 0x00000010")
      checkError("\"07:24:33.+10:10:X0\"".getBytes, "expected digit, offset: 0x00000011")
      checkError("\"07:24:33.+10:10:1X\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"07:24:33.+10:10:10X\"".getBytes, "expected '\"', offset: 0x00000013")
      checkError("\"07:24:33.+18:10\"".getBytes, "illegal zone offset, offset: 0x00000010")
      checkError("\"07:24:33.-18:10\"".getBytes, "illegal zone offset, offset: 0x00000010")
      checkError("\"07:24:33.+20:10\"".getBytes, "illegal zone offset hour, offset: 0x00000010")
      checkError("\"07:24:33.+10:90\"".getBytes, "illegal zone offset minute, offset: 0x00000010")
      checkError("\"07:24:33.+10:10:60\"".getBytes, "illegal zone offset second, offset: 0x00000013")
    }
  }
  "JsonReader.readPeriod and JsonReader.readKeyAsPeriod" should {
    "parse null value" in {
      reader("null".getBytes).readPeriod() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsPeriod())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Period.parse("P1Y2M3D")
      reader("null".getBytes).readPeriod(default) shouldBe default
    }
    "parse Period from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(x: Period, s: String): Unit = {
        val xx = Period.of(-x.getYears, -x.getMonths, -x.getDays)
        readPeriod(s) shouldBe x
        readKeyAsPeriod(s) shouldBe x
        readPeriod('-' + s) shouldBe xx
        readKeyAsPeriod('-' + s) shouldBe xx
      }

      check(Period.ZERO, "P0D")
      forAll(genPeriod, minSuccessful(100000))(x => check(x, x.toString))
      forAll(Gen.choose(Int.MinValue, Int.MaxValue), Gen.choose(Int.MinValue, Int.MaxValue), minSuccessful(100000)) {
        (x: Int, y: Int) =>
          check(Period.of(x, 0, 0), s"P${x}Y")
          check(Period.of(0, x, 0), s"P${x}M")
          check(Period.of(0, 0, x), s"P${x}D")
          check(Period.of(x, y, 0), s"P${x}Y${y}M")
          check(Period.of(0, x, y), s"P${x}M${y}D")
          check(Period.of(x, 0, y), s"P${x}Y${y}D")
      }
      forAll(Gen.choose(-1000000, 1000000), Gen.choose(-1000000, 1000000), minSuccessful(100000)) {
        (weeks: Int, days: Int) =>
          check(Period.of(0, 0, weeks * 7 + days), s"P${weeks}W${days}D")
          check(Period.of(1, 0, weeks * 7 + days), s"P1Y${weeks}W${days}D")
          check(Period.of(1, 1, weeks * 7 + days), s"P1Y1M${weeks}W${days}D")
      }
    }
    "throw parsing exception for empty input and illegal or broken Period string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readPeriod()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsPeriod()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"".getBytes, "expected 'P', offset: 0x00000002")
      checkError("\"PXY\"".getBytes, "expected '-' or digit, offset: 0x00000002")
      checkError("\"P-XY\"".getBytes, "expected digit, offset: 0x00000003")
      checkError("\"P1XY\"".getBytes, "expected 'Y' or 'M' or 'W' or 'D' or digit, offset: 0x00000003")
      checkError("\"P2147483648Y\"".getBytes, "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649Y\"".getBytes, "illegal period, offset: 0x0000000c")
      checkError("\"P1YXM\"".getBytes, "expected '\"' or '-' or digit, offset: 0x00000004")
      checkError("\"P1Y-XM\"".getBytes, "expected digit, offset: 0x00000005")
      checkError("\"P1Y1XM\"".getBytes, "expected 'M' or 'W' or 'D' or digit, offset: 0x00000005")
      checkError("\"P1Y2147483648M\"".getBytes, "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649M\"".getBytes, "illegal period, offset: 0x0000000e")
      checkError("\"P1Y1MXW\"".getBytes, "expected '\"' or '-' or digit, offset: 0x00000006")
      checkError("\"P1Y1M-XW\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"P1Y1M1XW\"".getBytes, "expected 'W' or 'D' or digit, offset: 0x00000007")
      checkError("\"P1Y1M306783379W\"".getBytes, "illegal period, offset: 0x0000000f")
      checkError("\"P1Y1M-306783379W\"".getBytes, "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M1WXD\"".getBytes, "expected '\"' or '-' or digit, offset: 0x00000008")
      checkError("\"P1Y1M1W-XD\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"P1Y1M1W1XD\"".getBytes, "expected 'D' or digit, offset: 0x00000009")
      checkError("\"P1Y1M306783378W8D\"".getBytes, "illegal period, offset: 0x00000011")
      checkError("\"P1Y1M-306783378W-8D\"".getBytes, "illegal period, offset: 0x00000013")
      checkError("\"P1Y1M0W2147483648D\"".getBytes, "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M0W-2147483649D\"".getBytes, "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M1W1DX".getBytes, "expected '\"', offset: 0x0000000a")
    }
  }
  "JsonReader.readYear" should {
    "parse valid number values with skipping of JSON space characters" in {
      readYear(" \n\t\r123456789", null) shouldBe Year.of(123456789)
      readYear(" \n\t\r-123456789", null) shouldBe Year.of(-123456789)
    }
    "parse valid number values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readYear("0$", null) shouldBe Year.of(0)
      readYear("123456789$", null) shouldBe Year.of(123456789)
    }
  }
  "JsonReader.readYear and JsonReader.readStringAsYear" should {
    "parse null value" in {
      readYear("null", null) shouldBe null
      reader("null".getBytes).readStringAsYear(null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readYear("null", Year.of(2008)) shouldBe Year.of(2008)
      reader("null".getBytes).readStringAsYear(Year.of(2008)) shouldBe Year.of(2008)
    }
  }
  "JsonReader.readYear, JsonReader.readStringAsYear and JsonReader.readKeyAsYear" should {
    def check(n: Year): Unit = {
      val s = n.toString
      readYear(s, null) shouldBe n
      readKeyAsYear(s) shouldBe n
      readStringAsYear(s, null) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readYear(s, null)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsYear(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsYear(s, null)).getMessage.contains(error2))
    }

    "parse valid number values" in {
      forAll(genYear, minSuccessful(100000))(check)
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("12345.0", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345e5", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345E5", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("nu", "unexpected end of input, offset: 0x00000002", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readYear(s, null)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readYearMonth and JsonReader.readKeyAsYearMonth" should {
    "parse null value" in {
      reader("null".getBytes).readYearMonth() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsYearMonth())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = YearMonth.parse("2008-01")
      reader("null".getBytes).readYearMonth(default) shouldBe default
    }
    "parse YearMonth from a string representation according to ISO-8601 format" in {
      def check(x: YearMonth): Unit = {
        val s = x.toString
        readYearMonth(s) shouldBe x
        readKeyAsYearMonth(s) shouldBe x
      }

      check(YearMonth.of(Year.MIN_VALUE, 12))
      check(YearMonth.of(Year.MIN_VALUE, 1))
      forAll(genYearMonth, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken YearMonth string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readYearMonth()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsYearMonth()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01".getBytes, "unexpected end of input, offset: 0x00000008")
      checkError("\"008-01\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X1\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01X\"".getBytes, "expected '\"', offset: 0x00000008")
      checkError("\"+1000000000-01\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"2008-00\"".getBytes, "illegal month, offset: 0x00000008")
      checkError("\"2008-13\"".getBytes, "illegal month, offset: 0x00000008")
    }
  }
  "JsonReader.readZonedDateTime and JsonReader.readKeyAsZonedDateTime" should {
    "parse null value" in {
      reader("null".getBytes).readZonedDateTime() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsZonedDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZonedDateTime.parse("2008-01-20T07:24Z[UTC]")
      reader("null".getBytes).readZonedDateTime(default) shouldBe default
    }
    "parse ZonedDateTime from a string representation according to ISO-8601 format with optional IANA time zone identifier in JDK 8+ format" in {
      def check(x: ZonedDateTime): Unit = {
        val s = x.toString
        readZonedDateTime(s) shouldBe x
        readKeyAsZonedDateTime(s) shouldBe x
      }

      check(ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MAX))
      check(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MIN))
      forAll(genZonedDateTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken ZonedDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readZonedDateTime()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZonedDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z[UTC]".getBytes, "unexpected end of input, offset: 0x0000001a")
      checkError("\"008-01-20T07:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z[UTC]\"".getBytes, "expected '-' or digit, offset: 0x00000005")
      checkError("\"2008-X1-20T07:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z[UTC]\"".getBytes, "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z[UTC]\"".getBytes, "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z[UTC]\"".getBytes, "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z[UTC]\"".getBytes, "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z[UTC]\"".getBytes, "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z[UTC]\"".getBytes, "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z[UTC]\"".getBytes, "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z[UTC]\"".getBytes, "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ[UTC]\"".getBytes, "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X[UTC]\"".getBytes, "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes, "expected '[', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.[UTC]\"".getBytes, "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000[UTC]\"".getBytes, "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X[UTC]\"".getBytes, "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"+1000000000-01-20T07:24:33Z[UTC]\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z[UTC]\"".getBytes, "expected '-', offset: 0x0000000b")
      checkError("\"2008-00-20T07:24:33Z[UTC]\"".getBytes, "illegal month, offset: 0x0000001a")
      checkError("\"2008-13-20T07:24:33Z[UTC]\"".getBytes, "illegal month, offset: 0x0000001a")
      checkError("\"2008-01-00T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-02-30T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-03-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-04-31T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-05-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-06-31T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-07-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-08-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-09-31T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-10-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-11-31T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-12-32T07:24:33Z[UTC]\"".getBytes, "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-20T24:24:33Z[UTC]\"".getBytes, "illegal hour, offset: 0x0000001a")
      checkError("\"2008-01-20T07:60:33Z[UTC]\"".getBytes, "illegal minute, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:60Z[UTC]\"".getBytes, "illegal second, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33+[UTC]\"".getBytes, "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-[UTC]\"".getBytes, "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+[UTC]\"".getBytes, "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1[UTC]\"".getBytes, "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=[UTC]\"".getBytes, "expected ':' or '[' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:[UTC]\"".getBytes, "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1[UTC]\"".getBytes, "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10[]\"".getBytes, "illegal date/time/zone, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10=10[UTC]\"".getBytes, "expected ':' or '[' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0[UTC]\"".getBytes, "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X[UTC]\"".getBytes, "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10:10[UTC]X\"".getBytes, "expected '\"', offset: 0x00000023")
      checkError("\"2008-01-20T07:24:33.+18:10[UTC]\"".getBytes, "illegal zone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.-18:10[UTC]\"".getBytes, "illegal zone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+20:10[UTC]\"".getBytes, "illegal zone offset hour, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:90[UTC]\"".getBytes, "illegal zone offset minute, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:10:60[UTC]\"".getBytes, "illegal zone offset second, offset: 0x00000023")
    }
  }
  "JsonReader.readZoneId and JsonReader.readKeyAsZoneId" should {
    "parse null value" in {
      reader("null".getBytes).readZoneId() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsZoneId())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneId.of("Europe/Warsaw")
      reader("null".getBytes).readZoneId(default) shouldBe default
    }
    "parse ZoneId from a string representation according to ISO-8601 format for zone offset or JDK 8+ format for IANA time zone identifier" in {
      def check(x: ZoneId): Unit = {
        val s = x.toString
        readZoneId(s) shouldBe x
        readKeyAsZoneId(s) shouldBe x
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneId, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken ZoneId string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readZoneId()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZoneId()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "illegal date/time/zone, offset: 0x00000001")
      checkError("\"+\"".getBytes, "illegal date/time/zone, offset: 0x00000002")
      //checkError("\"+1\"".getBytes, "expected digit, offset: 0x00000003") FIXME looks like bug in ZoneId.of() parser
      checkError("\"+10=\"".getBytes, "illegal date/time/zone, offset: 0x00000005")
      checkError("\"+10:\"".getBytes, "illegal date/time/zone, offset: 0x00000005")
      checkError("\"+10:1\"".getBytes, "illegal date/time/zone, offset: 0x00000006")
      checkError("\"+18:10\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"-18:10\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+20:10\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+10:90\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+10:10:60\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UT+\"".getBytes, "illegal date/time/zone, offset: 0x00000004")
      checkError("\"UT+10=\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"UT+10:\"".getBytes, "illegal date/time/zone, offset: 0x00000007")
      checkError("\"UT+10:1\"".getBytes, "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UT+18:10\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT-18:10\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+20:10\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+10:90\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+10:10:60\"".getBytes, "illegal date/time/zone, offset: 0x0000000c")
      checkError("\"UTC+\"".getBytes, "illegal date/time/zone, offset: 0x00000005")
      checkError("\"UTC+10=\"".getBytes, "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UTC+10:\"".getBytes, "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UTC+10:1\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UTC+18:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC-18:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+20:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+10:90\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+10:10:60\"".getBytes, "illegal date/time/zone, offset: 0x0000000d")
      checkError("\"GMT+\"".getBytes, "illegal date/time/zone, offset: 0x00000005")
      checkError("\"GMT+10=\"".getBytes, "illegal date/time/zone, offset: 0x00000008")
      checkError("\"GMT+10:\"".getBytes, "illegal date/time/zone, offset: 0x00000008")
      checkError("\"GMT+10:1\"".getBytes, "illegal date/time/zone, offset: 0x00000009")
      checkError("\"GMT+18:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT-18:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+20:10\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+10:90\"".getBytes, "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+10:10:60\"".getBytes, "illegal date/time/zone, offset: 0x0000000d")
    }
  }
  "JsonReader.readZoneOffset and JsonReader.readKeyAsZoneOffset" should {
    "parse null value" in {
      reader("null".getBytes).readZoneOffset() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsZoneOffset())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneOffset.of("+01:00")
      reader("null".getBytes).readZoneOffset(default) shouldBe default
    }
    "parse ZoneOffset from a string representation according to ISO-8601 format" in {
      def check(x: ZoneOffset): Unit = {
        val s = x.toString
        readZoneOffset(s) shouldBe x
        readKeyAsZoneOffset(s) shouldBe x
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneOffset, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken ZoneOffset string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readZoneOffset()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZoneOffset()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes, "expected '+' or '-' or 'Z', offset: 0x00000001")
      checkError("\"+\"".getBytes, "expected digit, offset: 0x00000002")
      checkError("\"+1\"".getBytes, "expected digit, offset: 0x00000003")
      checkError("\"+10=\"".getBytes, "expected ':' or '\"', offset: 0x00000004")
      checkError("\"+10:\"".getBytes, "expected digit, offset: 0x00000005")
      checkError("\"+10:1\"".getBytes, "expected digit, offset: 0x00000006")
      checkError("\"+10:10=10\"".getBytes, "expected ':' or '\"', offset: 0x00000007")
      checkError("\"+10:10:X0\"".getBytes, "expected digit, offset: 0x00000008")
      checkError("\"+10:10:1X\"".getBytes, "expected digit, offset: 0x00000009")
      checkError("\"+10:10:10X\"".getBytes, "expected '\"', offset: 0x0000000a")
      checkError("\"+18:10\"".getBytes, "illegal zone offset, offset: 0x00000007")
      checkError("\"-18:10\"".getBytes, "illegal zone offset, offset: 0x00000007")
      checkError("\"+20:10\"".getBytes, "illegal zone offset hour, offset: 0x00000007")
      checkError("\"+10:90\"".getBytes, "illegal zone offset minute, offset: 0x00000007")
      checkError("\"+10:10:60\"".getBytes, "illegal zone offset second, offset: 0x0000000a")
    }
  }
  "JsonReader.readKeyAsString" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"\"".getBytes).readKeyAsString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](reader("\"\"x".getBytes).readKeyAsString())
        .getMessage.contains("expected ':', offset: 0x00000002"))
    }
  }
  "JsonReader.readString and JsonReader.readKeyAsString" should {
    def check(s: String): Unit = {
      readString(s) shouldBe s
      readKeyAsString(s) shouldBe s
    }

    "parse null value" in {
      reader("null".getBytes).readString() shouldBe null
      assert(intercept[JsonParseException](reader("null".getBytes).readKeyAsString())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null".getBytes).readString("VVV") shouldBe "VVV"
    }
    "parse string with Unicode chars which are not escaped and are non-surrogate" in {
      forAll(minSuccessful(100000)) { (s: String) =>
        whenever(s.forall(ch => ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch))) {
          check(s)
        }
      }
    }
    "parse string with valid surrogate pairs" in {
      forAll(genHighSurrogateChar, genLowSurrogateChar, minSuccessful(100000)) { (hi: Char, lo: Char) =>
        whenever(Character.isSurrogatePair(hi, lo)) {
          check(new String(Array(hi, lo)))
        }
      }
    }
    "parse escaped chars of string value" in {
      def checkEncoded(s1: String, s2: String): Unit = {
        readString(s1) shouldBe s2
        readKeyAsString(s1) shouldBe s2
      }

      checkEncoded("""\b\f\n\r\t\/\\""", "\b\f\n\r\t/\\")
      checkEncoded("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1\\ud834\\udd1e", "\b\f\n\r\t/Aиბ𝄞")
    }
    "throw parsing exception for control chars that must be escaped" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readString()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error))
      }

      forAll(genControlChar, minSuccessful(1000)) { (ch: Char) =>
        checkError(Array('"', ch.toByte, '"'), "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readString()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error))
      }

      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\\".getBytes, "unexpected end of input, offset: 0x00000002")
    }
    "throw parsing exception for boolean values & numbers" in {
      def checkError(bytes: Array[Byte], error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readString()).getMessage.contains(error1))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error2))
      }

      checkError("true".getBytes, "expected '\"' or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
      checkError("false".getBytes, "expected '\"' or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
      checkError("12345".getBytes, "expected '\"' or null, offset: 0x00000000", "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readString(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsString(s)).getMessage.contains(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      checkError("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      checkError("\\", "unexpected end of input, offset: 0x00000003", "unexpected end of input, offset: 0x00000004")
      checkError("\\udd1e", "unexpected end of input, offset: 0x00000008",
        "unexpected end of input, offset: 0x00000009")
      checkError("\\ud834", "unexpected end of input, offset: 0x00000008",
        "unexpected end of input, offset: 0x00000009")
      checkError("\\ud834\\", "unexpected end of input, offset: 0x00000009",
        "unexpected end of input, offset: 0x0000000a")
      checkError("\\ud834\\x", "unexpected end of input, offset: 0x0000000a",
        "unexpected end of input, offset: 0x0000000b")
      checkError("\\ud834\\ud834", "illegal surrogate character pair, offset: 0x0000000c",
        "illegal surrogate character pair, offset: 0x0000000c")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](readString(bytes)).getMessage.contains(error))
        assert(intercept[JsonParseException](readKeyAsString(bytes)).getMessage.contains(error))
      }

      checkError(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      checkError(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      checkError(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte),
        "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      checkError(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xf0, 0x80, 0x80, 0x80, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x04.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x04, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0xFF.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0xff, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0xFF.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0xff, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0xFF.toByte, 0x84.toByte, 0x9E.toByte),
        "malformed byte(s): 0xf0, 0xff, 0x84, 0x9e, offset: 0x00000004")
      checkError(Array[Byte](0xF0.toByte, 0x9D.toByte, 0x84.toByte, 0x0E.toByte),
        "malformed byte(s): 0xf0, 0x9d, 0x84, 0x0e, offset: 0x00000004")
    }
  }
  "JsonReader.readKeyAsChar" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"x\"".getBytes).readKeyAsChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](reader("\"x\"x".getBytes).readKeyAsChar())
        .getMessage.contains("expected ':', offset: 0x00000003"))
    }
  }
  "JsonReader.readChar and JsonReader.readKeyAsChar" should {
    def check(ch: Char): Unit = {
      readChar(ch.toString) shouldBe ch
      readKeyAsChar(ch.toString) shouldBe ch
    }

    def checkEscaped(escaped: String, nonEscaped: Char): Unit = {
      readChar(escaped) shouldBe nonEscaped
      readKeyAsChar(escaped) shouldBe nonEscaped
    }

    def checkError(bytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonParseException](reader(bytes).readChar()).getMessage.contains(error))
      assert(intercept[JsonParseException](reader(bytes).readKeyAsChar()).getMessage.contains(error))
    }

    "parse Unicode char that is not escaped and is non-surrogate from string with length == 1" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          check(ch)
        }
      }
    }
    "parse escaped chars of string value" in {
      checkEscaped("""\b""", '\b')
      checkEscaped("""\f""", '\f')
      checkEscaped("""\n""", '\n')
      checkEscaped("""\r""", '\r')
      checkEscaped("""\t""", '\t')
      checkEscaped("""\/""", '/')
      checkEscaped("""\\""", '\\')
      checkEscaped("\\u0008", '\b')
      checkEscaped("\\u000C", '\f')
      checkEscaped("\\u000a", '\n')
      checkEscaped("\\u000D", '\r')
      checkEscaped("\\u0009", '\t')
      checkEscaped("\\u002F", '/')
      checkEscaped("\\u0041", 'A')
      checkEscaped("\\u0438", 'и')
      checkEscaped("\\u10d1", 'ბ')
    }
    "throw parsing exception for string with length > 1" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch)) {
          checkError(("\"" + ch + ch + "\"").getBytes(UTF_8), "expected '\"'") // offset can differs for non-ASCII characters
        }
      }
    }
    "throw parsing exception for control chars that must be escaped" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readChar()).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsChar()).getMessage.contains(error))
      }

      forAll(genControlChar, minSuccessful(1000)) { (ch: Char) =>
        checkError(Array('"', ch.toByte, '"'), "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError("".getBytes, "unexpected end of input, offset: 0x00000000")
      checkError("\"".getBytes, "unexpected end of input, offset: 0x00000001")
      checkError("\"\\".getBytes, "unexpected end of input, offset: 0x00000002")
    }
    "throw parsing exception for null, boolean values & numbers" in {
      checkError("null".getBytes, "expected '\"', offset: 0x00000000")
      checkError("true".getBytes, "expected '\"', offset: 0x00000000")
      checkError("false".getBytes, "expected '\"', offset: 0x00000000")
      checkError("12345".getBytes, "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readChar(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsChar(s)).getMessage.contains(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u000", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u00", "unexpected end of input, offset: 0x00000006", "expected hex digit, offset: 0x00000005")
      checkError("\\u0", "unexpected end of input, offset: 0x00000005", "unexpected end of input, offset: 0x00000006")
      checkError("\\", "unexpected end of input, offset: 0x00000003", "expected '\"', offset: 0x00000003")
      checkError("\\udd1e", "illegal surrogate character, offset: 0x00000006",
        "illegal surrogate character, offset: 0x00000006")
      checkError("\\ud834", "illegal surrogate character, offset: 0x00000006",
        "illegal surrogate character, offset: 0x00000006")
    }
    "throw parsing exception in case of illegal byte sequence" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](readChar(bytes)).getMessage.contains(error))
        assert(intercept[JsonParseException](readKeyAsChar(bytes)).getMessage.contains(error))
      }

      checkError(Array[Byte](0x80.toByte), "malformed byte(s): 0x80, offset: 0x00000001")
      checkError(Array[Byte](0xC0.toByte, 0x80.toByte), "malformed byte(s): 0xc0, 0x80, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0x08.toByte), "malformed byte(s): 0xc8, 0x08, offset: 0x00000002")
      checkError(Array[Byte](0xC8.toByte, 0xFF.toByte), "malformed byte(s): 0xc8, 0xff, offset: 0x00000002")
      checkError(Array[Byte](0xE0.toByte, 0x80.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0x80, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE0.toByte, 0xFF.toByte, 0x80.toByte),
        "malformed byte(s): 0xe0, 0xff, 0x80, offset: 0x00000003")
      checkError(Array[Byte](0xE8.toByte, 0x88.toByte, 0x08.toByte),
        "malformed byte(s): 0xe8, 0x88, 0x08, offset: 0x00000003")
      checkError(Array[Byte](0xF0.toByte, 0x80.toByte, 0x80.toByte, 0x80.toByte),
        "illegal surrogate character, offset: 0x00000004")
    }
  }
  "JsonReader.readByte" should {
    "parse valid byte values with skiping of JSON space characters" in {
      readByte(" \n\t\r123") shouldBe 123.toByte
      readByte(" \n\t\r-123") shouldBe (-123).toByte
    }
    "parse valid byte values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readByte("0$") shouldBe 0
      readByte("123$") shouldBe 123
    }
  }
  "JsonReader.readByte, JsonReader.readKeyAsByte and JsonReader.readStringAsByte" should {
    def check(n: Byte): Unit = {
      val s = n.toString
      readByte(s) shouldBe n
      readKeyAsByte(s) shouldBe n
      readStringAsByte(s) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readByte(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsByte(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsByte(s)).getMessage.contains(error2))
    }

    "parse valid byte values" in {
      forAll(minSuccessful(1000)) { (n: Byte) =>
        check(n)
      }
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("123.0", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("123e10", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("123E10", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on byte overflow" in {
      checkError("128", "value is too large for byte, offset: 0x00000002",
        "value is too large for byte, offset: 0x00000003")
      checkError("-129", "value is too large for byte, offset: 0x00000003",
        "value is too large for byte, offset: 0x00000004")
      checkError("12345", "value is too large for byte, offset: 0x00000003",
        "value is too large for byte, offset: 0x00000004")
      checkError("-12345", "value is too large for byte, offset: 0x00000004",
        "value is too large for byte, offset: 0x00000005")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readByte(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("0123", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0123", "illegal number with leading zero, offset: 0x00000001")
      checkError("0128", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0128", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readShort" should {
    "parse valid short values with skipping of JSON space characters" in {
      readShort(" \n\t\r12345") shouldBe 12345.toShort
      readShort(" \n\t\r-12345") shouldBe -12345.toShort
    }
    "parse valid short values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readShort("0$") shouldBe 0
      readShort("12345$") shouldBe 12345
    }
  }
  "JsonReader.readShort, JsonReader.readKeyAsShort and JsonReader.readStringAsShort" should {
    def check(n: Short): Unit = {
      val s = n.toString
      readShort(s) shouldBe n
      readKeyAsShort(s) shouldBe n
      readStringAsShort(s) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readShort(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsShort(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsShort(s)).getMessage.contains(error2))
    }

    "parse valid short values" in {
      forAll(minSuccessful(10000)) { (n: Short) =>
        check(n)
      }
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("12345.0", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345e10", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
      checkError("12345E10", "illegal number, offset: 0x00000005", "illegal number, offset: 0x00000006")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on short overflow" in {
      checkError("32768", "value is too large for short, offset: 0x00000004",
        "value is too large for short, offset: 0x00000005")
      checkError("-32769", "value is too large for short, offset: 0x00000005",
        "value is too large for short, offset: 0x00000006")
      checkError("12345678901", "value is too large for short, offset: 0x00000005",
        "value is too large for short, offset: 0x00000006")
      checkError("-12345678901", "value is too large for short, offset: 0x00000006",
        "value is too large for short, offset: 0x00000007")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readShort(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
      checkError("032767", "illegal number with leading zero, offset: 0x00000000")
      checkError("-032768", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readInt" should {
    "parse valid int values with skipping of JSON space characters" in {
      readInt(" \n\t\r123456789") shouldBe 123456789
      readInt(" \n\t\r-123456789") shouldBe -123456789
    }
    "parse valid int values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readInt("0$") shouldBe 0
      readInt("123456789$") shouldBe 123456789
    }
  }
  "JsonReader.readInt, JsonReader.readKeyAsInt and JsonReader.readStringAsInt" should {
    def check(n: Int): Unit = {
      val s = n.toString
      readInt(s) shouldBe n
      readKeyAsInt(s) shouldBe n
      readStringAsInt(s) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readInt(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsInt(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsInt(s)).getMessage.contains(error2))
    }

    "parse valid int values" in {
      forAll(minSuccessful(100000)) { (n: Int) =>
        check(n)
      }
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("123456789.0", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
      checkError("123456789e10", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
      checkError("123456789E10", "illegal number, offset: 0x00000009", "illegal number, offset: 0x0000000a")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on int overflow" in {
      checkError("2147483648", "value is too large for int, offset: 0x00000009",
        "value is too large for int, offset: 0x0000000a")
      checkError("-2147483649", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("12345678901", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("-12345678901", "value is too large for int, offset: 0x0000000b",
        "value is too large for int, offset: 0x0000000c")
      checkError("12345678901234567890", "value is too large for int, offset: 0x0000000a",
        "value is too large for int, offset: 0x0000000b")
      checkError("-12345678901234567890", "value is too large for int, offset: 0x0000000b",
        "value is too large for int, offset: 0x0000000c")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readInt(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("0123456789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-0123456789", "illegal number with leading zero, offset: 0x00000001")
      checkError("02147483647", "illegal number with leading zero, offset: 0x00000000")
      checkError("-02147483648", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readLong" should {
    "parse valid long values with skipping of JSON space characters" in {
      readLong(" \n\t\r1234567890123456789") shouldBe 1234567890123456789L
      readLong(" \n\t\r-1234567890123456789") shouldBe -1234567890123456789L
    }
    "parse valid long values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readLong("0$") shouldBe 0L
      readLong("1234567890123456789$") shouldBe 1234567890123456789L
    }
  }
  "JsonReader.readLong, JsonReader.readKeyAsLong and JsonReader.readStringAsLong" should {
    def check(n: Long): Unit = {
      val s = n.toString
      readLong(s) shouldBe n
      readKeyAsLong(s) shouldBe n
      readStringAsLong(s) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readLong(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsLong(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsLong(s)).getMessage.contains(error2))
    }

    "parse valid long values" in {
      forAll(minSuccessful(100000)) { (n: Long) =>
        check(n)
      }
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("1234567890123456789.0", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789e10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789E10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("x", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on long overflow" in {
      checkError("9223372036854775808", "value is too large for long, offset: 0x00000012",
        "value is too large for long, offset: 0x00000013")
      checkError("-9223372036854775809", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("12345678901234567890", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("-12345678901234567890", "value is too large for long, offset: 0x00000014",
        "value is too large for long, offset: 0x00000015")
      checkError("123456789012345678901234567890", "value is too large for long, offset: 0x00000013",
        "value is too large for long, offset: 0x00000014")
      checkError("-123456789012345678901234567890", "value is too large for long, offset: 0x00000014",
        "value is too large for long, offset: 0x00000015")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readLong(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("01234567890123456789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-01234567890123456789", "illegal number with leading zero, offset: 0x00000001")
      checkError("09223372036854775807", "illegal number with leading zero, offset: 0x00000000")
      checkError("-09223372036854775808", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readFloat" should {
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
  }
  "JsonReader.readFloat, JsonReader.readKeyAsFloat and JsonReader.readStringAsFloat" should {
    def check(s: String, n: Float): Unit = {
      readFloat(s) shouldBe n
      readKeyAsFloat(s) shouldBe n
      readStringAsFloat(s) shouldBe n
    }

    def checkFloat(s: String): Unit = check(s, java.lang.Float.parseFloat(s))

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readFloat(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsFloat(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsFloat(s)).getMessage.contains(error2))
    }

    "parse valid float values" in {
      forAll(minSuccessful(100000)) { (n: Float) =>
        checkFloat(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        checkFloat(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: BigDecimal) =>
        checkFloat(n.toString)
      }
    }
    "parse infinity on float overflow" in {
      check("12345e6789", Float.PositiveInfinity)
      check("-12345e6789", Float.NegativeInfinity)
      check("12345678901234567890e12345678901234567890", Float.PositiveInfinity)
      check("-12345678901234567890e12345678901234567890", Float.NegativeInfinity)
      readFloat("12345678901234567890e12345678901234567890$") shouldBe Float.PositiveInfinity
      readFloat("-12345678901234567890e12345678901234567890$") shouldBe Float.NegativeInfinity
    }
    "parse zero on float underflow" in {
      check("12345e-6789", 0.0f)
      check("-12345e-6789", -0.0f)
      check("12345678901234567890e-12345678901234567890", 0.0f)
      check("-12345678901234567890e-12345678901234567890", -0.0f)
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readFloat(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readDouble" should {
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
  }
  "JsonReader.readDouble, JsonReader.readKeyAsDouble and JsonReader.readStringAsDouble" should {
    def check(s: String, n: Double): Unit = {
      readDouble(s) shouldBe n
      readKeyAsDouble(s) shouldBe n
      readStringAsDouble(s) shouldBe n
    }

    def checkDouble(s: String): Unit = check(s, java.lang.Double.parseDouble(s))

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readDouble(s)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsDouble(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsDouble(s)).getMessage.contains(error2))
    }

    "parse valid double values" in {
      forAll(minSuccessful(100000)) { (n: Double) =>
        checkDouble(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        checkDouble(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: BigDecimal) =>
        checkDouble(n.toString)
      }
    }
    "parse infinity on double overflow" in {
      check("12345e6789", Double.PositiveInfinity)
      check("-12345e6789", Double.NegativeInfinity)
      check("12345678901234567890e12345678901234567890", Double.PositiveInfinity)
      check("-12345678901234567890e12345678901234567890", Double.NegativeInfinity)
      readDouble("12345678901234567890e12345678901234567890$") shouldBe Double.PositiveInfinity
      readDouble("-12345678901234567890e12345678901234567890$") shouldBe Double.NegativeInfinity
    }
    "parse zero on double underflow" in {
      check("12345e-6789", 0.0)
      check("-12345e-6789", -0.0)
      check("12345678901234567890e-12345678901234567890", 0.0)
      check("-1234567890123456789e-12345678901234567890", -0.0)
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readDouble(s)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigInt" should {
    "parse valid number values with skipping of JSON space characters" in {
      readBigInt(" \n\t\r12345678901234567890123456789", null) shouldBe BigInt("12345678901234567890123456789")
      readBigInt(" \n\t\r-12345678901234567890123456789", null) shouldBe BigInt("-12345678901234567890123456789")
    }
    "parse valid number values and stops on not numeric chars (except '.', 'e', 'E')" in {
      readBigInt("0$", null) shouldBe BigInt("0")
      readBigInt("1234567890123456789$", null) shouldBe BigInt("1234567890123456789")
    }
  }
  "JsonReader.readBigInt and JsonReader.readStringAsBigInt" should {
    "parse null value" in {
      readBigInt("null", null) shouldBe null
      reader("null".getBytes).readStringAsBigInt(null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigInt("null", BigInt("12345")) shouldBe BigInt("12345")
      reader("null".getBytes).readStringAsBigInt(BigInt("12345")) shouldBe BigInt("12345")
    }
  }
  "JsonReader.readBigInt, JsonReader.readStringAsBigInt and JsonReader.readKeyAsBigInt" should {
    def check(n: BigInt): Unit = {
      val s = n.toString
      readBigInt(s, null) shouldBe n
      readKeyAsBigInt(s) shouldBe n
      readStringAsBigInt(s, null) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readBigInt(s, null)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsBigInt(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsBigInt(s, null)).getMessage.contains(error2))
    }

    "parse valid number values" in {
      forAll(minSuccessful(100000)) { (n: BigInt) =>
        check(n)
      }
    }
    "parse big number values without overflow" in {
      val bigNumber = "12345" + new String(Array.fill(6789)('0'))
      check(BigInt(bigNumber))
      check(BigInt("-" + bigNumber))
    }
    "throw parsing exception on valid number values with '.', 'e', 'E' chars" in {
      checkError("1234567890123456789.0", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789e10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
      checkError("1234567890123456789E10", "illegal number, offset: 0x00000013", "illegal number, offset: 0x00000014")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("nu", "unexpected end of input, offset: 0x00000002", "illegal number, offset: 0x00000002")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigInt(s, null)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigDecimal" should {
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
  }
  "JsonReader.readBigDecimal and JsonReader.readStringAsBigDecimal" should {
    "parse null value" in {
      readBigDecimal("null", null) shouldBe null
      reader("null".getBytes).readStringAsBigDecimal(null) shouldBe null
    }
    "return supplied default value instead of null value" in {
      readBigDecimal("null", BigDecimal("12345")) shouldBe BigDecimal("12345")
      reader("null".getBytes).readStringAsBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
  }
  "JsonReader.readBigDecimal, JsonReader.readKeyAsBigDecimal and JsonReader.readStringAsBigDecimal" should {
    def check(n: BigDecimal): Unit = {
      val s = n.toString
      readBigDecimal(s, null) shouldBe n
      readKeyAsBigDecimal(s) shouldBe n
      readStringAsBigDecimal(s, null) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readBigDecimal(s, null)).getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsBigDecimal(s)).getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsBigDecimal(s, null)).getMessage.contains(error2))
    }

    "parse valid number values" in {
      forAll(minSuccessful(100000)) { (n: BigDecimal) =>
        check(n)
      }
    }
    "parse big number values without overflow" in {
      check(BigDecimal("12345e6789"))
      check(BigDecimal("-12345e6789"))
    }
    "parse small number values without underflow" in {
      check(BigDecimal("12345e-6789"))
      check(BigDecimal("-12345e-6789"))
    }
    "throw number format exception for too big exponents" in {
      checkError("12345678901234567890e12345678901234567890",
        "illegal number, offset: 0x00000028", "illegal number, offset: 0x00000029")
      checkError("-12345678901234567890e12345678901234567890",
        "illegal number, offset: 0x00000029", "illegal number, offset: 0x0000002a")
      checkError("12345678901234567890e-12345678901234567890",
        "illegal number, offset: 0x00000029", "illegal number, offset: 0x0000002a")
      checkError("-12345678901234567890e-12345678901234567890",
        "illegal number, offset: 0x0000002a", "illegal number, offset: 0x0000002b")
      checkError("12345678901234567890e12345678901234567890$",
        "illegal number, offset: 0x00000028", "illegal number, offset: 0x00000029")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("$", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" $", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-$", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000002")
      checkError("0e$", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0e-$", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000004")
      checkError("0.E", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.-", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("0.+", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000003")
      checkError("NaN", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Inf", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("Infinity", "illegal number, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError("nx", "illegal number, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("nux", "illegal number, offset: 0x00000002", "illegal number, offset: 0x00000001")
      checkError("nulx", "illegal number, offset: 0x00000003", "illegal number, offset: 0x00000001")
    }
    "throw parsing exception on leading zero" in {
      def checkError(s: String, error: String): Unit =
        assert(intercept[JsonParseException](readBigDecimal(s, null)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.setMark and JsonReader.rollbackToMark" should {
    "store current position of parsing and return back to it" in {
      val jsonReader = reader("{}".getBytes)
      jsonReader.setMark()
      jsonReader.skip()
      jsonReader.rollbackToMark()
      jsonReader.nextToken() shouldBe '{'
    }
    "throw exception in case of rollbackToMark was called before setMark" in {
      val jsonReader = reader("{}".getBytes)
      jsonReader.skip()
      assert(intercept[ArrayIndexOutOfBoundsException](jsonReader.rollbackToMark())
        .getMessage.contains("expected preceding call of 'setMark()'"))
    }
  }
  "JsonReader.scanToKey" should {
    "find key-value pair by provided key and set current position of parsing to its value" in {
      val jsonReader = reader("""{"key1":1,"key2":2}""".getBytes)
      jsonReader.isNextToken('{') // enter to JSON object
      jsonReader.scanToKey("key2")
      jsonReader.readInt() shouldBe 2
    }
    "throw parsing exception in case of key-value pair of provided key cannot be found" in {
      val jsonReader = reader("""{"key1":1}""".getBytes)
      jsonReader.isNextToken('{') // enter to JSON object
      assert(intercept[JsonParseException](jsonReader.scanToKey("key2"))
        .getMessage.contains("missing required field \"key2\", offset: 0x00000009"))
    }
  }
  "JsonReader.requiredKeyError" should {
    val jsonReader = reader("{}".getBytes)
    jsonReader.nextToken()
    "throw parsing exception with list of missing required fields that specified by bits" in {
      def check(bits: Int, error: String): Unit =
        assert(intercept[JsonParseException](jsonReader.requiredKeyError(Array("name", "device"), Array(bits)))
          .getMessage.contains(error))

      check(3, "missing required field(s) \"name\", \"device\", offset: 0x00000000")
      check(2, "missing required field(s) \"device\", offset: 0x00000000")
      check(1, "missing required field(s) \"name\", offset: 0x00000000")
    }
    "throw illegal argument exception in case of missing required fields cannot be selected" in {
      assert(intercept[IllegalArgumentException](jsonReader.requiredKeyError(Array("name", "device"), Array(0)))
        .getMessage.contains("missing required field(s) cannot be reported for arguments: " +
          "reqFields = Array(name, device), reqBits = Array(0)"))
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
  "JsonReader.commaError" should {
    val jsonReader = reader("{}".getBytes)
    jsonReader.isNextToken(',')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.commaError())
        .getMessage.contains("expected ',', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayStartOrNullError" should {
    val jsonReader = reader("{}".getBytes)
    jsonReader.isNextToken('[')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayStartOrNullError())
        .getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndError" should {
    val jsonReader = reader("}".getBytes)
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayEndError())
        .getMessage.contains("expected ']', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndOrCommaError" should {
    val jsonReader = reader("}".getBytes)
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayEndOrCommaError())
        .getMessage.contains("expected ']' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader.objectStartOrNullError" should {
    val jsonReader = reader("[]".getBytes)
    jsonReader.isNextToken('{')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectStartOrNullError())
        .getMessage.contains("expected '{' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.objectEndOrCommaError" should {
    val jsonReader = reader("]".getBytes)
    jsonReader.isNextToken('}')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectEndOrCommaError())
        .getMessage.contains("expected '}' or ',', offset: 0x00000000"))
    }
  }

  def validateSkip(s: String): Unit = {
    val r = reader((s + ",").getBytes)
    r.skip()
    r.nextToken().toChar shouldBe ','
  }

  def readInstant(s: String): Instant = readInstant(s.getBytes(UTF_8))

  def readInstant(buf: Array[Byte]): Instant = reader(stringify(buf)).readInstant()

  def readDuration(s: String): Duration = readDuration(s.getBytes(UTF_8))

  def readDuration(buf: Array[Byte]): Duration = reader(stringify(buf)).readDuration()

  def readLocalDate(s: String): LocalDate = readLocalDate(s.getBytes(UTF_8))

  def readLocalDate(buf: Array[Byte]): LocalDate = reader(stringify(buf)).readLocalDate()

  def readLocalDateTime(s: String): LocalDateTime = readLocalDateTime(s.getBytes(UTF_8))

  def readLocalDateTime(buf: Array[Byte]): LocalDateTime = reader(stringify(buf)).readLocalDateTime()

  def readLocalTime(s: String): LocalTime = readLocalTime(s.getBytes(UTF_8))

  def readLocalTime(buf: Array[Byte]): LocalTime = reader(stringify(buf)).readLocalTime()

  def readMonthDay(s: String): MonthDay = readMonthDay(s.getBytes(UTF_8))

  def readMonthDay(buf: Array[Byte]): MonthDay = reader(stringify(buf)).readMonthDay()

  def readOffsetDateTime(s: String): OffsetDateTime = readOffsetDateTime(s.getBytes(UTF_8))

  def readOffsetDateTime(buf: Array[Byte]): OffsetDateTime = reader(stringify(buf)).readOffsetDateTime()

  def readOffsetTime(s: String): OffsetTime = readOffsetTime(s.getBytes(UTF_8))

  def readOffsetTime(buf: Array[Byte]): OffsetTime = reader(stringify(buf)).readOffsetTime()

  def readPeriod(s: String): Period = readPeriod(s.getBytes(UTF_8))

  def readPeriod(buf: Array[Byte]): Period = reader(stringify(buf)).readPeriod()

  def readYear(s: String, default: Year): Year = readYear(s.getBytes(UTF_8), default)

  def readYear(buf: Array[Byte], default: Year): Year = reader(buf).readYear(default)

  def readYearMonth(s: String): YearMonth = readYearMonth(s.getBytes(UTF_8))

  def readYearMonth(buf: Array[Byte]): YearMonth = reader(stringify(buf)).readYearMonth()

  def readZonedDateTime(s: String): ZonedDateTime = readZonedDateTime(s.getBytes(UTF_8))

  def readZonedDateTime(buf: Array[Byte]): ZonedDateTime = reader(stringify(buf)).readZonedDateTime()

  def readZoneOffset(s: String): ZoneOffset = readZoneOffset(s.getBytes(UTF_8))

  def readZoneOffset(buf: Array[Byte]): ZoneOffset = reader(stringify(buf)).readZoneOffset()

  def readZoneId(s: String): ZoneId = readZoneId(s.getBytes(UTF_8))

  def readZoneId(buf: Array[Byte]): ZoneId = reader(stringify(buf)).readZoneId()

  def readUUID(s: String): UUID = readUUID(s.getBytes(UTF_8))

  def readUUID(buf: Array[Byte]): UUID = reader(stringify(buf)).readUUID()

  def readString(s: String): String = readString(s.getBytes(UTF_8))

  def readString(buf: Array[Byte]): String = reader(stringify(buf)).readString()

  def readChar(s: String): Char = readChar(s.getBytes(UTF_8))

  def readChar(buf: Array[Byte]): Char = reader(stringify(buf)).readChar()

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

  def readKeyAsInstant(s: String): Instant = readKeyAsInstant(s.getBytes(UTF_8))

  def readKeyAsInstant(buf: Array[Byte]): Instant = reader(stringify(buf) :+ ':'.toByte).readKeyAsInstant()

  def readKeyAsDuration(s: String): Duration = readKeyAsDuration(s.getBytes(UTF_8))

  def readKeyAsDuration(buf: Array[Byte]): Duration =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsDuration()

  def readKeyAsLocalDate(s: String): LocalDate = readKeyAsLocalDate(s.getBytes(UTF_8))

  def readKeyAsLocalDate(buf: Array[Byte]): LocalDate =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalDate()

  def readKeyAsLocalDateTime(s: String): LocalDateTime = readKeyAsLocalDateTime(s.getBytes(UTF_8))

  def readKeyAsLocalDateTime(buf: Array[Byte]): LocalDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalDateTime()

  def readKeyAsLocalTime(s: String): LocalTime = readKeyAsLocalTime(s.getBytes(UTF_8))

  def readKeyAsLocalTime(buf: Array[Byte]): LocalTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalTime()

  def readKeyAsMonthDay(s: String): MonthDay = readKeyAsMonthDay(s.getBytes(UTF_8))

  def readKeyAsMonthDay(buf: Array[Byte]): MonthDay =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsMonthDay()

  def readKeyAsOffsetDateTime(s: String): OffsetDateTime = readKeyAsOffsetDateTime(s.getBytes(UTF_8))

  def readKeyAsOffsetDateTime(buf: Array[Byte]): OffsetDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsOffsetDateTime()

  def readKeyAsOffsetTime(s: String): OffsetTime = readKeyAsOffsetTime(s.getBytes(UTF_8))

  def readKeyAsOffsetTime(buf: Array[Byte]): OffsetTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsOffsetTime()

  def readKeyAsPeriod(s: String): Period = readKeyAsPeriod(s.getBytes(UTF_8))

  def readKeyAsPeriod(buf: Array[Byte]): Period =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsPeriod()

  def readKeyAsYear(s: String): Year = readKeyAsYear(s.getBytes(UTF_8))

  def readKeyAsYear(buf: Array[Byte]): Year = reader(stringify(buf) :+ ':'.toByte).readKeyAsYear()

  def readKeyAsYearMonth(s: String): YearMonth = readKeyAsYearMonth(s.getBytes(UTF_8))

  def readKeyAsYearMonth(buf: Array[Byte]): YearMonth = reader(stringify(buf) :+ ':'.toByte).readKeyAsYearMonth()

  def readKeyAsZonedDateTime(s: String): ZonedDateTime = readKeyAsZonedDateTime(s.getBytes(UTF_8))

  def readKeyAsZonedDateTime(buf: Array[Byte]): ZonedDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsZonedDateTime()

  def readKeyAsZoneOffset(s: String): ZoneOffset = readKeyAsZoneOffset(s.getBytes(UTF_8))

  def readKeyAsZoneOffset(buf: Array[Byte]): ZoneOffset =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsZoneOffset()

  def readKeyAsZoneId(s: String): ZoneId = readKeyAsZoneId(s.getBytes(UTF_8))

  def readKeyAsZoneId(buf: Array[Byte]): ZoneId =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsZoneId()

  def readKeyAsUUID(s: String): UUID = readKeyAsUUID(s.getBytes(UTF_8))

  def readKeyAsUUID(buf: Array[Byte]): UUID = reader(stringify(buf) :+ ':'.toByte).readKeyAsUUID()

  def readKeyAsString(s: String): String = readKeyAsString(s.getBytes(UTF_8))

  def readKeyAsString(buf: Array[Byte]): String = reader(stringify(buf) :+ ':'.toByte).readKeyAsString()

  def readKeyAsChar(s: String): Char = readKeyAsChar(s.getBytes(UTF_8))

  def readKeyAsChar(buf: Array[Byte]): Char = reader(stringify(buf) :+ ':'.toByte).readKeyAsChar()

  def readKeyAsByte(s: String): Byte = readKeyAsByte(s.getBytes(UTF_8))

  def readKeyAsByte(buf: Array[Byte]): Byte = reader(stringify(buf) :+ ':'.toByte).readKeyAsByte()

  def readKeyAsShort(s: String): Short = readKeyAsShort(s.getBytes(UTF_8))

  def readKeyAsShort(buf: Array[Byte]): Short = reader(stringify(buf) :+ ':'.toByte).readKeyAsShort()

  def readKeyAsInt(s: String): Int = readKeyAsInt(s.getBytes(UTF_8))

  def readKeyAsInt(buf: Array[Byte]): Int = reader(stringify(buf) :+ ':'.toByte).readKeyAsInt()

  def readKeyAsLong(s: String): Long = readKeyAsLong(s.getBytes(UTF_8))

  def readKeyAsLong(buf: Array[Byte]): Long = reader(stringify(buf) :+ ':'.toByte).readKeyAsLong()

  def readKeyAsFloat(s: String): Float = readKeyAsFloat(s.getBytes(UTF_8))

  def readKeyAsFloat(buf: Array[Byte]): Float = reader(stringify(buf) :+ ':'.toByte).readKeyAsFloat()

  def readKeyAsDouble(s: String): Double = readKeyAsDouble(s.getBytes(UTF_8))

  def readKeyAsDouble(buf: Array[Byte]): Double = reader(stringify(buf) :+ ':'.toByte).readKeyAsDouble()

  def readKeyAsBigInt(s: String): BigInt = readKeyAsBigInt(s.getBytes(UTF_8))

  def readKeyAsBigInt(buf: Array[Byte]): BigInt = reader(stringify(buf) :+ ':'.toByte).readKeyAsBigInt()

  def readKeyAsBigDecimal(s: String): BigDecimal = readKeyAsBigDecimal(s.getBytes(UTF_8))

  def readKeyAsBigDecimal(buf: Array[Byte]): BigDecimal = reader(stringify(buf) :+ ':'.toByte).readKeyAsBigDecimal()

  def readStringAsYear(s: String, default: Year): Year =
    readStringAsYear(s.getBytes(UTF_8), default)

  def readStringAsYear(buf: Array[Byte], default: Year): Year =
    reader(stringify(buf)).readStringAsYear(default)

  def readStringAsByte(s: String): Byte = readStringAsByte(s.getBytes(UTF_8))

  def readStringAsByte(buf: Array[Byte]): Byte = reader(stringify(buf)).readStringAsByte()

  def readStringAsShort(s: String): Short = readStringAsShort(s.getBytes(UTF_8))

  def readStringAsShort(buf: Array[Byte]): Short = reader(stringify(buf)).readStringAsShort()

  def readStringAsInt(s: String): Int = readStringAsInt(s.getBytes(UTF_8))

  def readStringAsInt(buf: Array[Byte]): Int = reader(stringify(buf)).readStringAsInt()

  def readStringAsLong(s: String): Long = readStringAsLong(s.getBytes(UTF_8))

  def readStringAsLong(buf: Array[Byte]): Long = reader(stringify(buf)).readStringAsLong()

  def readStringAsFloat(s: String): Float = readStringAsFloat(s.getBytes(UTF_8))

  def readStringAsFloat(buf: Array[Byte]): Float = reader(stringify(buf)).readStringAsFloat()

  def readStringAsDouble(s: String): Double = readStringAsDouble(s.getBytes(UTF_8))

  def readStringAsDouble(buf: Array[Byte]): Double = reader(stringify(buf)).readStringAsDouble()

  def readStringAsBigInt(s: String, default: BigInt): BigInt = readStringAsBigInt(s.getBytes(UTF_8), default)

  def readStringAsBigInt(buf: Array[Byte], default: BigInt): BigInt =
    reader(stringify(buf)).readStringAsBigInt(default)

  def readStringAsBigDecimal(s: String, default: BigDecimal): BigDecimal =
    readStringAsBigDecimal(s.getBytes(UTF_8), default)

  def readStringAsBigDecimal(buf: Array[Byte], default: BigDecimal): BigDecimal =
    reader(stringify(buf)).readStringAsBigDecimal(default)

  def reader(buf: Array[Byte]): JsonReader = new JsonReader(new Array[Byte](12), // a minimal allowed length of `buf`
    0, 0, 2147483647, new Array[Char](0), new ByteArrayInputStream(buf), 0, ReaderConfig())

  def stringify(buf: Array[Byte]): Array[Byte] = '"'.toByte +: buf :+ '"'.toByte
}