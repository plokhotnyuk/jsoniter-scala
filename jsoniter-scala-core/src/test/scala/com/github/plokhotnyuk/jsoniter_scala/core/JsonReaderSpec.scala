package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.ByteArrayInputStream
import java.math.MathContext
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader.{defaultMathContext, defaultMaxScale}
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class JsonReaderSpec extends WordSpec with Matchers with PropertyChecks {
  "ReaderConfig.<init>" should {
    "have safe and handy defaults" in {
      ReaderConfig().throwParseExceptionWithStackTrace shouldBe false
      ReaderConfig().appendHexDumpToParseException shouldBe true
      ReaderConfig().preferredBufSize shouldBe 16384
      ReaderConfig().preferredCharBufSize shouldBe 2048
    }
    "throw exception in case for unsupported values of params" in {
      assert(intercept[IllegalArgumentException](ReaderConfig(preferredBufSize = 11))
        .getMessage.contains("'preferredBufSize' should be not less than 12"))
      assert(intercept[IllegalArgumentException](ReaderConfig(preferredCharBufSize = -1))
        .getMessage.contains("'preferredCharBufSize' should be not less than 0"))
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
      val r = reader("{}".getBytes("UTF-8"))
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}".getBytes("UTF-8"))
      r.skip()
      assert(intercept[JsonParseException](r.nextToken() == '{')
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.readNullOrError" should {
    "parse null value" in {
      val r = reader("null".getBytes("UTF-8"))
      r.isNextToken('n') shouldBe true
      r.readNullOrError("default", "error") shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      assert(intercept[JsonParseException] {
        val r = reader("nxll".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null") shouldBe "default"
      }.getMessage.contains("expected null, offset: 0x00000001"))
      assert(intercept[JsonParseException] {
        val r = reader("nuxl".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null") shouldBe "default"
      }.getMessage.contains("expected null, offset: 0x00000002"))
      assert(intercept[JsonParseException] {
        val r = reader("nulx".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null") shouldBe "default"
      }.getMessage.contains("expected null, offset: 0x00000003"))
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("null".getBytes("UTF-8")).readNullOrError("default", "error"))
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readNullOrTokenError" should {
    "parse null value" in {
      val r = reader("null".getBytes("UTF-8"))
      r.isNextToken('n') shouldBe true
      r.readNullOrTokenError("default", 'x') shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      assert(intercept[JsonParseException] {
        val r = reader("nxll".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x') shouldBe "default"
      }.getMessage.contains("expected 'x' or null, offset: 0x00000001"))
      assert(intercept[JsonParseException] {
        val r = reader("nuxl".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x') shouldBe "default"
      }.getMessage.contains("expected 'x' or null, offset: 0x00000002"))
      assert(intercept[JsonParseException] {
        val r = reader("nulx".getBytes("UTF-8"))
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x') shouldBe "default"
      }.getMessage.contains("expected 'x' or null, offset: 0x00000003"))
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("null".getBytes("UTF-8")).readNullOrError("default", "error"))
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.rollbackToken" should {
    "rollback of reading last byte of input" in {
      val r = reader("""{"x":1}""".getBytes("UTF-8"))
      assert(r.nextToken() == '{')
      r.rollbackToken()
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '"')
      r.rollbackToken()
      assert(r.nextToken() == '"')
    }
    "throw array index out of bounds exception in case of missing preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("{}".getBytes("UTF-8")).rollbackToken())
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readBoolean, JsonReader.readStringAsBoolean and JsonReader.readKeyAsBoolean" should {
    def check(s: String, value: Boolean): Unit = {
      reader(s.getBytes("UTF-8")).readBoolean() shouldBe value
      reader(('\"' + s + '\"').getBytes("UTF-8")).readStringAsBoolean() shouldBe value
      reader(('\"' + s + "\":").getBytes("UTF-8")).readKeyAsBoolean() shouldBe value
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](reader(s.getBytes("UTF-8")).readBoolean())
        .getMessage.contains(error1))
      assert(intercept[JsonParseException](reader(('\"' + s + '\"').getBytes("UTF-8")).readStringAsBoolean())
        .getMessage.contains(error2))
      assert(intercept[JsonParseException](reader(('\"' + s + "\":").getBytes("UTF-8")).readKeyAsBoolean())
        .getMessage.contains(error2))
    }

    "parse valid true and false values" in {
      check("true", value = true)
      check("false", value = false)
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      checkError("x", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("txue", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      checkError("trae", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      checkError("folse", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      checkError("faxse", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      checkError("falxe", "illegal boolean, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("falsu", "illegal boolean, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("tru", "unexpected end of input, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("fals", "unexpected end of input, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
    }
  }
  "JsonReader.readKeyAsUUID" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"00000000-0000-0000-0000-000000000000\"".getBytes("UTF-8")).readKeyAsUUID())
        .getMessage.contains("unexpected end of input, offset: 0x00000026"))
      assert(intercept[JsonParseException](reader("\"00000000-0000-0000-0000-000000000000\"x".getBytes("UTF-8")).readKeyAsUUID())
        .getMessage.contains("expected ':', offset: 0x00000026"))
    }
  }
  "JsonReader.readUUID and JsonReader.readKeyAsUUID" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readUUID(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsUUID())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = new UUID(0, 0)
      reader("null".getBytes("UTF-8")).readUUID(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readUUID(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsUUID()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000002")
      checkError("\"00000000-0000-0000-0000-000000000000".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000025")
      checkError("\"Z0000000-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000001")
      checkError("\"0Z000000-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000002")
      checkError("\"00Z00000-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000003")
      checkError("\"000Z0000-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000004")
      checkError("\"0000Z000-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000005")
      checkError("\"00000Z00-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000006")
      checkError("\"000000Z0-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000007")
      checkError("\"0000000Z-0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000008")
      checkError("\"00000000=0000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected '-', offset: 0x00000009")
      checkError("\"00000000-Z000-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000000a")
      checkError("\"00000000-0Z00-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000000b")
      checkError("\"00000000-00Z0-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000000c")
      checkError("\"00000000-000Z-0000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000000d")
      checkError("\"00000000-0000=0000-0000-000000000000\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000e")
      checkError("\"00000000-0000-Z000-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000000f")
      checkError("\"00000000-0000-0Z00-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000010")
      checkError("\"00000000-0000-00Z0-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000011")
      checkError("\"00000000-0000-000Z-0000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000012")
      checkError("\"00000000-0000-0000=0000-000000000000\"".getBytes("UTF-8"), "expected '-', offset: 0x00000013")
      checkError("\"00000000-0000-0000-Z000-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000014")
      checkError("\"00000000-0000-0000-0Z00-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000015")
      checkError("\"00000000-0000-0000-00Z0-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000016")
      checkError("\"00000000-0000-0000-000Z-000000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000017")
      checkError("\"00000000-0000-0000-0000=000000000000\"".getBytes("UTF-8"), "expected '-', offset: 0x00000018")
      checkError("\"00000000-0000-0000-0000-Z00000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000019")
      checkError("\"00000000-0000-0000-0000-0Z0000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001a")
      checkError("\"00000000-0000-0000-0000-00Z000000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001b")
      checkError("\"00000000-0000-0000-0000-000Z00000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001c")
      checkError("\"00000000-0000-0000-0000-0000Z0000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001d")
      checkError("\"00000000-0000-0000-0000-00000Z000000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001e")
      checkError("\"00000000-0000-0000-0000-000000Z00000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x0000001f")
      checkError("\"00000000-0000-0000-0000-0000000Z0000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000020")
      checkError("\"00000000-0000-0000-0000-00000000Z000\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000021")
      checkError("\"00000000-0000-0000-0000-000000000Z00\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000022")
      checkError("\"00000000-0000-0000-0000-0000000000Z0\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000023")
      checkError("\"00000000-0000-0000-0000-00000000000Z\"".getBytes("UTF-8"), "expected hex digit, offset: 0x00000024")
      checkError("\"×0000000-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000001")
      checkError("\"0×000000-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000002")
      checkError("\"00×00000-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000003")
      checkError("\"000×0000-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000004")
      checkError("\"0000×000-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000005")
      checkError("\"00000×00-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000006")
      checkError("\"000000×0-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000007")
      checkError("\"0000000×-0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000008")
      checkError("\"00000000÷0000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected '-', offset: 0x00000009")
      checkError("\"00000000-×000-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000000a")
      checkError("\"00000000-0×00-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000000b")
      checkError("\"00000000-00×0-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000000c")
      checkError("\"00000000-000×-0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000000d")
      checkError("\"00000000-0000÷0000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected '-', offset: 0x0000000e")
      checkError("\"00000000-0000-×000-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000000f")
      checkError("\"00000000-0000-0×00-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000010")
      checkError("\"00000000-0000-00×0-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000011")
      checkError("\"00000000-0000-000×-0000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000012")
      checkError("\"00000000-0000-0000÷0000-000000000000\"".getBytes("ISO-8859-1"), "expected '-', offset: 0x00000013")
      checkError("\"00000000-0000-0000-×000-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000014")
      checkError("\"00000000-0000-0000-0×00-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000015")
      checkError("\"00000000-0000-0000-00×0-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000016")
      checkError("\"00000000-0000-0000-000×-000000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000017")
      checkError("\"00000000-0000-0000-0000÷000000000000\"".getBytes("ISO-8859-1"), "expected '-', offset: 0x00000018")
      checkError("\"00000000-0000-0000-0000-×00000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000019")
      checkError("\"00000000-0000-0000-0000-0×0000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001a")
      checkError("\"00000000-0000-0000-0000-00×000000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001b")
      checkError("\"00000000-0000-0000-0000-000×00000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001c")
      checkError("\"00000000-0000-0000-0000-0000×0000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001d")
      checkError("\"00000000-0000-0000-0000-00000×000000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001e")
      checkError("\"00000000-0000-0000-0000-000000×00000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x0000001f")
      checkError("\"00000000-0000-0000-0000-0000000×0000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000020")
      checkError("\"00000000-0000-0000-0000-00000000×000\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000021")
      checkError("\"00000000-0000-0000-0000-000000000×00\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000022")
      checkError("\"00000000-0000-0000-0000-0000000000×0\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000023")
      checkError("\"00000000-0000-0000-0000-00000000000×\"".getBytes("ISO-8859-1"), "expected hex digit, offset: 0x00000024")
      checkError("\"00000000-0000-0000-0000-000000000000x".getBytes("ISO-8859-1"), "expected '\"', offset: 0x00000025")
    }
  }
  "JsonReader.readKeyAsInstant" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"2008-01-20T07:24:33Z\"".getBytes("UTF-8")).readKeyAsInstant())
        .getMessage.contains("unexpected end of input, offset: 0x00000016"))
      assert(intercept[JsonParseException](reader("\"2008-01-20T07:24:33Z\"x".getBytes("UTF-8")).readKeyAsInstant())
        .getMessage.contains("expected ':', offset: 0x00000016"))
    }
  }
  "JsonReader.readDuration and JsonReader.readKeyAsDuration" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readDuration(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsDuration())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Duration.parse("P2DT3H4M")
      reader("null".getBytes("UTF-8")).readDuration(default) shouldBe default
    }
    "parse Duration from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(s: String, x: Duration): Unit = {
        val xx = x.negated()
        readDuration(s) shouldBe x
        readKeyAsDuration(s) shouldBe x
        readDuration('-' + s) shouldBe xx
        readKeyAsDuration('-' + s) shouldBe xx
      }

      check("P0D", Duration.ZERO)
      check("PT0S", Duration.ZERO)
      forAll(genDuration, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken Duration string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readDuration(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsDuration()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"".getBytes("UTF-8"), "expected 'P', offset: 0x00000002")
      checkError("\"PXD\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000002")
      checkError("\"PT0SX".getBytes("UTF-8"), "expected '\"', offset: 0x00000005")
      checkError("\"P-XD\"".getBytes("UTF-8"), "expected digit, offset: 0x00000003")
      checkError("\"P1XD\"".getBytes("UTF-8"), "expected 'D' or digit, offset: 0x00000003")
      checkError("\"P106751991167301D\"".getBytes("UTF-8"), "llegal duration, offset: 0x00000011")
      checkError("\"P1067519911673000D\"".getBytes("UTF-8"), "llegal duration, offset: 0x00000011")
      checkError("\"P-106751991167301D\"".getBytes("UTF-8"), "llegal duration, offset: 0x00000012")
      checkError("\"P1DX1H\"".getBytes("UTF-8"), "expected 'T' or '\"', offset: 0x00000004")
      checkError("\"P1DTXH\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"P1DT-XH\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"P1DT1XH\"".getBytes("UTF-8"), "expected 'H' or 'M' or 'S or '.' or digit, offset: 0x00000006")
      checkError("\"P0DT2562047788015216H\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000015")
      checkError("\"P0DT-2562047788015216H\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000016")
      checkError("\"P0DT153722867280912931M\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000017")
      checkError("\"P0DT-153722867280912931M\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000018")
      checkError("\"P0DT9223372036854775808S\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000018")
      checkError("\"P0DT92233720368547758000S\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000018")
      checkError("\"P0DT-9223372036854775809S\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000018")
      checkError("\"P1DT1HXM\"".getBytes("UTF-8"), "expected '\"' or '-' or digit, offset: 0x00000007")
      checkError("\"P1DT1H-XM\"".getBytes("UTF-8"), "expected digit, offset: 0x00000008")
      checkError("\"P1DT1H1XM\"".getBytes("UTF-8"), "expected 'M' or 'S or '.' or digit, offset: 0x00000008")
      checkError("\"P0DT0H153722867280912931M\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000019")
      checkError("\"P0DT0H-153722867280912931M\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H9223372036854775808S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H92233720368547758000S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H-9223372036854775809S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001a")
      checkError("\"P1DT1H1MXS\"".getBytes("UTF-8"), "expected '\"' or '-' or digit, offset: 0x00000009")
      checkError("\"P1DT1H1M-XS\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0XS\"".getBytes("UTF-8"), "expected 'S or '.' or digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0.XS\"".getBytes("UTF-8"), "expected 'S' or digit, offset: 0x0000000b")
      checkError("\"P1DT1H1M0.012345678XS\"".getBytes("UTF-8"), "expected 'S', offset: 0x00000014")
      checkError("\"P1DT1H1M0.0123456789S\"".getBytes("UTF-8"), "expected 'S', offset: 0x00000014")
      checkError("\"P0DT0H0M9223372036854775808S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001c")
      checkError("\"P0DT0H0M92233720368547758080S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001c")
      checkError("\"P0DT0H0M-9223372036854775809S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001c")
      checkError("\"P106751991167300DT24H\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000016")
      checkError("\"P0DT2562047788015215H60M\"".getBytes("UTF-8"), "illegal duration, offset: 0x00000019")
      checkError("\"P0DT0H153722867280912930M60S\"".getBytes("UTF-8"), "illegal duration, offset: 0x0000001d")
    }
  }
  "JsonReader.readInstant and JsonReader.readKeyAsInstant" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readInstant(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsInstant())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Instant.parse("2008-01-20T07:24:33Z")
      reader("null".getBytes("UTF-8")).readInstant(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readInstant(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsInstant()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000015")
      checkError("\"008-01-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+1000000000=01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000c")
      checkError("\"2008-X0-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"".getBytes("UTF-8"), "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"".getBytes("UTF-8"), "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"".getBytes("UTF-8"), "expected ':' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"".getBytes("UTF-8"), "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes("UTF-8"), "expected '.' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes("UTF-8"), "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"".getBytes("UTF-8"), "expected 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"".getBytes("UTF-8"), "expected 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X\"".getBytes("UTF-8"), "expected 'Z', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x00000016")
      checkError("\"+1000000001-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"+1000000010-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"+9999999999-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"-1000000001-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"-1000000010-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"-9999999999-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001c")
      checkError("\"2008-00-20T07:24:33Z\"".getBytes("UTF-8"), "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"".getBytes("UTF-8"), "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2007-02-29T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-01-20T24:24:33Z\"".getBytes("UTF-8"), "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"".getBytes("UTF-8"), "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"".getBytes("UTF-8"), "illegal second, offset: 0x00000015")
    }
  }
  "JsonReader.readLocalDate and JsonReader.readKeyAsLocalDate" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readLocalDate(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsLocalDate())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDate.parse("2008-01-20")
      reader("null".getBytes("UTF-8")).readLocalDate(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readLocalDate(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalDate()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20".getBytes("UTF-8"), "unexpected end of input, offset: 0x0000000b")
      checkError("\"008-01-20\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+999999999=01-20\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20\"".getBytes("UTF-8"), "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2X\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X\"".getBytes("UTF-8"), "expected '\"', offset: 0x0000000b")
      checkError("\"-0000-01-20\"".getBytes("UTF-8"), "illegal year, offset: 0x0000000c")
      checkError("\"2008-00-20\"".getBytes("UTF-8"), "illegal month, offset: 0x0000000b")
      checkError("\"2008-13-20\"".getBytes("UTF-8"), "illegal month, offset: 0x0000000b")
      checkError("\"2008-01-00\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-01-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2007-02-29\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-02-30\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-03-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-04-31\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-05-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-06-31\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-07-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-08-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-09-31\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-10-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-11-31\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
      checkError("\"2008-12-32\"".getBytes("UTF-8"), "illegal day, offset: 0x0000000b")
    }
  }
  "JsonReader.readLocalDateTime and JsonReader.readKeyAsLocalDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readLocalDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsLocalDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDateTime.parse("2008-01-20T07:24:33")
      reader("null".getBytes("UTF-8")).readLocalDateTime(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readLocalDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000014")
      checkError("\"008-01-20T07:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+999999999=01-20T07:24:33\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33\"".getBytes("UTF-8"), "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33\"".getBytes("UTF-8"), "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33\"".getBytes("UTF-8"), "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes("UTF-8"), "expected '.' or '\"', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33.X\"".getBytes("UTF-8"), "expected '\"' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.123456789X\"".getBytes("UTF-8"), "expected '\"', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33\"".getBytes("UTF-8"), "illegal year, offset: 0x00000015")
      checkError("\"2008-00-20T07:24:33\"".getBytes("UTF-8"), "illegal month, offset: 0x00000014")
      checkError("\"2008-13-20T07:24:33\"".getBytes("UTF-8"), "illegal month, offset: 0x00000014")
      checkError("\"2008-01-00T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-01-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2007-02-29T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-02-30T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-03-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-04-31T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-05-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-06-31T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-07-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-08-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-09-31T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-10-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-11-31T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-12-32T07:24:33\"".getBytes("UTF-8"), "illegal day, offset: 0x00000014")
      checkError("\"2008-01-20T24:24:33\"".getBytes("UTF-8"), "illegal hour, offset: 0x00000014")
      checkError("\"2008-01-20T07:60:33\"".getBytes("UTF-8"), "illegal minute, offset: 0x00000014")
      checkError("\"2008-01-20T07:24:60\"".getBytes("UTF-8"), "illegal second, offset: 0x00000014")
    }
  }
  "JsonReader.readLocalTime and JsonReader.readKeyAsLocalTime" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readLocalTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsLocalTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalTime.parse("07:24:33")
      reader("null".getBytes("UTF-8")).readLocalTime(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readLocalTime(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsLocalTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected digit, offset: 0x00000001")
      checkError("\"07:24:33".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000009")
      checkError("\"7:24:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000002")
      checkError("\"07=24:33\"".getBytes("UTF-8"), "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33\"".getBytes("UTF-8"), "expected digit, offset: 0x00000005")
      checkError("\"07:24=33\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000006")
      checkError("\"07:24:X3\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"07:24:3X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"".getBytes("UTF-8"), "expected '.' or '\"', offset: 0x00000009")
      checkError("\"07:24:33.X\"".getBytes("UTF-8"), "expected '\"' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"".getBytes("UTF-8"), "expected '\"', offset: 0x00000013")
      checkError("\"24:24:33\"".getBytes("UTF-8"), "illegal hour, offset: 0x00000009")
      checkError("\"07:60:33\"".getBytes("UTF-8"), "illegal minute, offset: 0x00000009")
      checkError("\"07:24:60\"".getBytes("UTF-8"), "illegal second, offset: 0x00000009")
    }
  }
  "JsonReader.readMonthDay and JsonReader.readKeyAsMonthDay" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readMonthDay(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsMonthDay())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = MonthDay.parse("--01-20")
      reader("null".getBytes("UTF-8")).readMonthDay(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readMonthDay(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsMonthDay()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-', offset: 0x00000001")
      checkError("\"--01-20".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000008")
      checkError("\"-01-20\"".getBytes("UTF-8"), "expected '-', offset: 0x00000002")
      checkError("\"---01-20\"".getBytes("UTF-8"), "expected digit, offset: 0x00000003")
      checkError("\"--0X-20\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"--01=20\"".getBytes("UTF-8"), "expected '-', offset: 0x00000005")
      checkError("\"--01-X0\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"--01-2X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"--01-20X\"".getBytes("UTF-8"), "expected '\"', offset: 0x00000008")
      checkError("\"--00-20\"".getBytes("UTF-8"), "illegal month, offset: 0x00000008")
      checkError("\"--13-20\"".getBytes("UTF-8"), "illegal month, offset: 0x00000008")
      checkError("\"--01-00\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--01-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--02-30\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--03-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--04-31\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--05-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--06-31\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--07-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--08-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--09-31\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--10-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--11-31\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
      checkError("\"--12-32\"".getBytes("UTF-8"), "illegal day, offset: 0x00000008")
    }
  }
  "JsonReader.readOffsetDateTime and JsonReader.readKeyAsOffsetDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readOffsetDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsOffsetDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetDateTime.parse("2008-01-20T07:24Z")
      reader("null".getBytes("UTF-8")).readOffsetDateTime(default) shouldBe default
    }
    "parse OffsetDateTime from a string representation according to ISO-8601 format" in {
      def check(s: String, x: OffsetDateTime): Unit = {
        readOffsetDateTime(s) shouldBe x
        readKeyAsOffsetDateTime(s) shouldBe x
      }

      check("+999999999-12-31T23:59:59.999999999-18:00", OffsetDateTime.MAX)
      check("-999999999-01-01T00:00:00+18:00", OffsetDateTime.MIN)
      check("2018-01-01T00:00Z", OffsetDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      check("2018-01-01T00:00:00.000Z", OffsetDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      check("2018-01-01T00:00:00.000000000Z", OffsetDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      check("2018-01-01T00:00:00.000000000+00", OffsetDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
      forAll(genOffsetDateTime, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken OffsetDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readOffsetDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsOffsetDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000015")
      checkError("\"008-01-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+999999999=01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"".getBytes("UTF-8"), "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"".getBytes("UTF-8"), "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"".getBytes("UTF-8"), "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"".getBytes("UTF-8"), "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"".getBytes("UTF-8"), "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"".getBytes("UTF-8"), "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes("UTF-8"), "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"-0000-01-20T07:24:33Z\"".getBytes("UTF-8"), "illegal year, offset: 0x00000016")
      checkError("\"2008-00-20T07:24:33Z\"".getBytes("UTF-8"), "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"".getBytes("UTF-8"), "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2007-02-29T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"".getBytes("UTF-8"), "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33.123456789X\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"2008-01-20T24:24:33Z\"".getBytes("UTF-8"), "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"".getBytes("UTF-8"), "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"".getBytes("UTF-8"), "illegal second, offset: 0x00000015")
      checkError("\"2008-01-20T07:24+\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24-\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:33+\"".getBytes("UTF-8"), "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-\"".getBytes("UTF-8"), "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+\"".getBytes("UTF-8"), "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1\"".getBytes("UTF-8"), "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:\"".getBytes("UTF-8"), "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10=10\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.-18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+20:10\"".getBytes("UTF-8"), "illegal zone offset hour, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:90\"".getBytes("UTF-8"), "illegal zone offset minute, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:60\"".getBytes("UTF-8"), "illegal zone offset second, offset: 0x0000001e")
    }
  }
  "JsonReader.readOffsetTime and JsonReader.readKeyAsOffsetTime" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readOffsetTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsOffsetTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetTime.parse("07:24:33+01:00")
      reader("null".getBytes("UTF-8")).readOffsetTime(default) shouldBe default
    }
    "parse OffsetTime from a string representation according to ISO-8601 format" in {
      def check(s: String, x: OffsetTime): Unit = {
        readOffsetTime(s) shouldBe x
        readKeyAsOffsetTime(s) shouldBe x
      }

      check("23:59:59.999999999-18:00", OffsetTime.MAX)
      check("00:00:00+18:00", OffsetTime.MIN)
      check("00:00Z", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
      check("00:00:00.000Z", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
      check("00:00:00.000000000Z", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
      check("00:00:00.000000000+00", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC))
      forAll(genOffsetTime, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken OffsetTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readOffsetTime(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsOffsetTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected digit, offset: 0x00000001")
      checkError("\"07:24:33Z".getBytes("UTF-8"), "unexpected end of input, offset: 0x0000000a")
      checkError("\"7:24:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000002")
      checkError("\"07=24:33Z\"".getBytes("UTF-8"), "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000005")
      checkError("\"07:24=33Z\"".getBytes("UTF-8"), "expected ':' or '+' or '-' or 'Z', offset: 0x00000006")
      checkError("\"07:24:X3Z\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"07:24:3XZ\"".getBytes("UTF-8"), "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"".getBytes("UTF-8"), "expected '.' or '+' or '-' or 'Z', offset: 0x00000009")
      checkError("\"07:24:33.\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z', offset: 0x00000013")
      checkError("\"24:24:33Z\"".getBytes("UTF-8"), "illegal hour, offset: 0x0000000a")
      checkError("\"07:60:33Z\"".getBytes("UTF-8"), "illegal minute, offset: 0x0000000a")
      checkError("\"07:24:60Z\"".getBytes("UTF-8"), "illegal second, offset: 0x0000000a")
      checkError("\"07:24+\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"07:24-\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"07:24:33+\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33-\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33.+\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000b")
      checkError("\"07:24:33.+1\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000c")
      checkError("\"07:24:33.+10=\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x0000000d")
      checkError("\"07:24:33.+10:\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000e")
      checkError("\"07:24:33.+10:1\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000f")
      checkError("\"07:24:33.+10:10=10\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000010")
      checkError("\"07:24:33.+10:10:X0\"".getBytes("UTF-8"), "expected digit, offset: 0x00000011")
      checkError("\"07:24:33.+10:10:1X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"07:24:33.+10:10:10X\"".getBytes("UTF-8"), "expected '\"', offset: 0x00000013")
      checkError("\"07:24:33.+18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000010")
      checkError("\"07:24:33.-18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000010")
      checkError("\"07:24:33.+20:10\"".getBytes("UTF-8"), "illegal zone offset hour, offset: 0x00000010")
      checkError("\"07:24:33.+10:90\"".getBytes("UTF-8"), "illegal zone offset minute, offset: 0x00000010")
      checkError("\"07:24:33.+10:10:60\"".getBytes("UTF-8"), "illegal zone offset second, offset: 0x00000013")
    }
  }
  "JsonReader.readPeriod and JsonReader.readKeyAsPeriod" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readPeriod(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsPeriod())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Period.parse("P1Y2M3D")
      reader("null".getBytes("UTF-8")).readPeriod(default) shouldBe default
    }
    "parse Period from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(s: String, x: Period): Unit = {
        val xx = x.negated()
        readPeriod(s) shouldBe x
        readKeyAsPeriod(s) shouldBe x
        readPeriod('-' + s) shouldBe xx
        readKeyAsPeriod('-' + s) shouldBe xx
      }

      check("P0D", Period.ZERO)
      forAll(genPeriod, minSuccessful(100000))(x => check(x.toString, x))
      forAll(Gen.choose(Int.MinValue, Int.MaxValue), Gen.choose(Int.MinValue, Int.MaxValue), minSuccessful(100000)) {
        (x: Int, y: Int) =>
          check(s"P${x}Y", Period.of(x, 0, 0))
          check(s"P${x}M", Period.of(0, x, 0))
          check(s"P${x}D", Period.of(0, 0, x))
          check(s"P${x}Y${y}M", Period.of(x, y, 0))
          check(s"P${x}M${y}D", Period.of(0, x, y))
          check(s"P${x}Y${y}D", Period.of(x, 0, y))
      }
      forAll(Gen.choose(-1000000, 1000000), minSuccessful(100000)) {
        (weeks: Int) =>
          check(s"P${weeks}W", Period.of(0, 0, weeks * 7))
          check(s"P1Y${weeks}W", Period.of(1, 0, weeks * 7))
          check(s"P1Y1M${weeks}W", Period.of(1, 1, weeks * 7))
      }
      forAll(Gen.choose(-1000000, 1000000), Gen.choose(-1000000, 1000000), minSuccessful(100000)) {
        (weeks: Int, days: Int) =>
          check(s"P${weeks}W${days}D", Period.of(0, 0, weeks * 7 + days))
          check(s"P1Y${weeks}W${days}D", Period.of(1, 0, weeks * 7 + days))
          check(s"P1Y1M${weeks}W${days}D", Period.of(1, 1, weeks * 7 + days))
      }
    }
    "throw parsing exception for empty input and illegal or broken Period string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readPeriod(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsPeriod()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"".getBytes("UTF-8"), "expected 'P', offset: 0x00000002")
      checkError("\"PXY\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000002")
      checkError("\"P-XY\"".getBytes("UTF-8"), "expected digit, offset: 0x00000003")
      checkError("\"P1XY\"".getBytes("UTF-8"), "expected 'Y' or 'M' or 'W' or 'D' or digit, offset: 0x00000003")
      checkError("\"P2147483648Y\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470Y\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649Y\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000c")
      checkError("\"P1YXM\"".getBytes("UTF-8"), "expected '\"' or '-' or digit, offset: 0x00000004")
      checkError("\"P1Y-XM\"".getBytes("UTF-8"), "expected digit, offset: 0x00000005")
      checkError("\"P1Y1XM\"".getBytes("UTF-8"), "expected 'M' or 'W' or 'D' or digit, offset: 0x00000005")
      checkError("\"P1Y2147483648M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649M\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y2147483648W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y2147483648D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649D\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000e")
      checkError("\"P1Y1MXW\"".getBytes("UTF-8"), "expected '\"' or '-' or digit, offset: 0x00000006")
      checkError("\"P1Y1M-XW\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"P1Y1M1XW\"".getBytes("UTF-8"), "expected 'W' or 'D' or digit, offset: 0x00000007")
      checkError("\"P1Y1M306783379W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000f")
      checkError("\"P1Y1M3067833790W\"".getBytes("UTF-8"), "illegal period, offset: 0x0000000f")
      checkError("\"P1Y1M-306783379W\"".getBytes("UTF-8"), "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M2147483648D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M21474836470D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M-2147483649D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M1WXD\"".getBytes("UTF-8"), "expected '\"' or '-' or digit, offset: 0x00000008")
      checkError("\"P1Y1M1W-XD\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"P1Y1M1W1XD\"".getBytes("UTF-8"), "expected 'D' or digit, offset: 0x00000009")
      checkError("\"P1Y1M306783378W8D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000011")
      checkError("\"P1Y1M-306783378W-8D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000013")
      checkError("\"P1Y1M1W2147483647D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M-1W-2147483648D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000014")
      checkError("\"P1Y1M0W2147483648D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M0W21474836470D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M0W-2147483649D\"".getBytes("UTF-8"), "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M1W1DX".getBytes("UTF-8"), "expected '\"', offset: 0x0000000a")
    }
  }
  "JsonReader.readYear and JsonReader.readKeyAsYear" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readYear(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsYear())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Year.parse("2008")
      reader("null".getBytes("UTF-8")).readYear(default) shouldBe default
    }
    "parse Year from a string representation according to ISO-8601 format" in {
      def check(s: String, x: Year): Unit = {
        readYear(s) shouldBe x
        readKeyAsYear(s) shouldBe x
      }

      check("-999999999", Year.of(Year.MIN_VALUE))
      check("+999999999", Year.of(Year.MAX_VALUE))
      forAll(genYear, minSuccessful(100000)) { (x: Year) =>
        // '+' is required for years that extends 4 digits, see ISO 8601:2004 sections 3.4.2, 4.1.2.4
        val s = // FIXME: It looks like a bug in JDK that Year.toString serialize years as integer numbers
          if (x.getValue > 0) (if (x.getValue > 9999) "+" else "") + f"${x.getValue}%04d"
          else f"-${-x.getValue}%04d"
        check(s, x)
      }
    }
    "throw parsing exception for empty input and illegal or broken Year string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readYear(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsYear()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000005")
      checkError("\"200X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"-1000X\"".getBytes("UTF-8"), "expected '\"' or digit, offset: 0x00000006")
      checkError("\"+1000000000\"".getBytes("UTF-8"), "expected '\"', offset: 0x0000000b")
      checkError("\"-1000000000\"".getBytes("UTF-8"), "expected '\"', offset: 0x0000000b")
      checkError("\"-0000\"".getBytes("UTF-8"), "illegal year, offset: 0x00000006")
    }
  }
  "JsonReader.readYearMonth and JsonReader.readKeyAsYearMonth" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readYearMonth(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsYearMonth())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = YearMonth.parse("2008-01")
      reader("null".getBytes("UTF-8")).readYearMonth(default) shouldBe default
    }
    "parse YearMonth from a string representation according to ISO-8601 format" in {
      def check(s: String, x: YearMonth): Unit = {
        readYearMonth(s) shouldBe x
        readKeyAsYearMonth(s) shouldBe x
      }

      check("+999999999-12", YearMonth.of(Year.MAX_VALUE, 12))
      check("-999999999-01", YearMonth.of(Year.MIN_VALUE, 1))
      forAll(genYearMonth, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken YearMonth string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readYearMonth(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsYearMonth()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000008")
      checkError("\"008-01\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+999999999=01\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01X\"".getBytes("UTF-8"), "expected '\"', offset: 0x00000008")
      checkError("\"-0000-01\"".getBytes("UTF-8"), "illegal year, offset: 0x00000009")
      checkError("\"2008-00\"".getBytes("UTF-8"), "illegal month, offset: 0x00000008")
      checkError("\"2008-13\"".getBytes("UTF-8"), "illegal month, offset: 0x00000008")
    }
  }
  "JsonReader.readZonedDateTime and JsonReader.readKeyAsZonedDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readZonedDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsZonedDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZonedDateTime.parse("2008-01-20T07:24Z[UTC]")
      reader("null".getBytes("UTF-8")).readZonedDateTime(default) shouldBe default
    }
    "parse ZonedDateTime from a string representation according to ISO-8601 format with optional IANA time zone identifier in JDK 8+ format" in {
      def check(s: String, x: ZonedDateTime): Unit = {
        readZonedDateTime(s) shouldBe x
        readKeyAsZonedDateTime(s) shouldBe x
      }

      check("2018-01-01T00:00Z", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.UTC))
      check("2018-01-01T00:00:00Z", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.UTC))
      check("2018-01-01T00:00:00.000Z", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.UTC))
      check("2018-01-01T00:00+18", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.MAX))
      check("2018-01-01T00:00:00+18", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.MAX))
      check("2018-01-01T00:00:00+18[UTC+18]", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneId.of("UTC+18")))
      check("2018-01-01T00:00-18", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.MIN))
      check("2018-01-01T00:00:00-18", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneOffset.MIN))
      check("2018-01-01T00:00:00-18[UTC-18]", ZonedDateTime.of(LocalDateTime.of(2018, 1, 1, 0, 0, 0), ZoneId.of("UTC-18")))
      check("+999999999-12-31T23:59:59.999999999+18:00", ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MAX))
      check("-999999999-01-01T00:00:00-18:00", ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MIN))
      forAll(genZonedDateTime, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken ZonedDateTime string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readZonedDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZonedDateTime()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z[UTC]".getBytes("UTF-8"), "unexpected end of input, offset: 0x0000001a")
      checkError("\"008-01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected '-' or digit, offset: 0x00000005")
      checkError("\"+999999999=01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z[UTC]\"".getBytes("UTF-8"), "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z[UTC]\"".getBytes("UTF-8"), "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z[UTC]\"".getBytes("UTF-8"), "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X[UTC]\"".getBytes("UTF-8"), "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ".getBytes("UTF-8"), "expected '[' or '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.[UTC]\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000[UTC]\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X[UTC]\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal year, offset: 0x0000001b")
      checkError("\"2008-00-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal month, offset: 0x0000001a")
      checkError("\"2008-13-20T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal month, offset: 0x0000001a")
      checkError("\"2008-01-00T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2007-02-29T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-02-30T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-03-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-04-31T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-05-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-06-31T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-07-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-08-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-09-31T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-10-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-11-31T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-12-32T07:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-20T24:24:33Z[UTC]\"".getBytes("UTF-8"), "illegal hour, offset: 0x0000001a")
      checkError("\"2008-01-20T07:60:33Z[UTC]\"".getBytes("UTF-8"), "illegal minute, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:60Z[UTC]\"".getBytes("UTF-8"), "illegal second, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33+[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=[UTC]\"".getBytes("UTF-8"), "expected ':' or '[' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10[]\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10=10[UTC]\"".getBytes("UTF-8"), "expected ':' or '[' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X[UTC]\"".getBytes("UTF-8"), "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10:10[UTC]X\"".getBytes("UTF-8"), "expected '\"', offset: 0x00000023")
      checkError("\"2008-01-20T07:24:33.+18:10[UTC]\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.-18:10[UTC]\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+20:10[UTC]\"".getBytes("UTF-8"), "illegal zone offset hour, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:90[UTC]\"".getBytes("UTF-8"), "illegal zone offset minute, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:10:60[UTC]\"".getBytes("UTF-8"), "illegal zone offset second, offset: 0x00000023")
    }
  }
  "JsonReader.readZoneId and JsonReader.readKeyAsZoneId" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readZoneId(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsZoneId())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneId.of("Europe/Warsaw")
      reader("null".getBytes("UTF-8")).readZoneId(default) shouldBe default
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
        assert(intercept[JsonParseException](reader(bytes).readZoneId(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZoneId()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000001")
      checkError("\"+\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000002")
      //checkError("\"+1\"".getBytes("UTF-8"), "expected digit, offset: 0x00000003") FIXME: looks like a bug in ZoneId.of() parser
      checkError("\"+10=\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000005")
      checkError("\"+10:\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000005")
      checkError("\"+10:1\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000006")
      checkError("\"+18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"-18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+20:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+10:90\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"+10:10:60\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UT+\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000004")
      checkError("\"UT+10=\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"UT+10:\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000007")
      checkError("\"UT+10:1\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UT+18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT-18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+20:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+10:90\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UT+10:10:60\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000c")
      checkError("\"UTC+\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000005")
      checkError("\"UTC+10=\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UTC+10:\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000008")
      checkError("\"UTC+10:1\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"UTC+18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC-18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+20:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+10:90\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"UTC+10:10:60\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000d")
      checkError("\"GMT+\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000005")
      checkError("\"GMT+10=\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000008")
      checkError("\"GMT+10:\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000008")
      checkError("\"GMT+10:1\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x00000009")
      checkError("\"GMT+18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT-18:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+20:10\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+10:90\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000a")
      checkError("\"GMT+10:10:60\"".getBytes("UTF-8"), "illegal date/time/zone, offset: 0x0000000d")
    }
  }
  "JsonReader.readZoneOffset and JsonReader.readKeyAsZoneOffset" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readZoneOffset(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsZoneOffset())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneOffset.of("+01:00")
      reader("null".getBytes("UTF-8")).readZoneOffset(default) shouldBe default
    }
    "parse ZoneOffset from a string representation according to ISO-8601 format" in {
      def check(s: String, x: ZoneOffset): Unit = {
        readZoneOffset(s) shouldBe x
        readKeyAsZoneOffset(s) shouldBe x
      }

      check("Z", ZoneOffset.UTC)
      check("+18", ZoneOffset.MAX)
      check("+18:00", ZoneOffset.MAX)
      check("-18", ZoneOffset.MIN)
      check("-18:00", ZoneOffset.MIN)
      forAll(genZoneOffset, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken ZoneOffset string" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonParseException](reader(bytes).readZoneOffset(null)).getMessage.contains(error))
        assert(intercept[JsonParseException](reader(bytes).readKeyAsZoneOffset()).getMessage.contains(error))
      }

      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\"".getBytes("UTF-8"), "expected '+' or '-' or 'Z', offset: 0x00000001")
      checkError("\"+\"".getBytes("UTF-8"), "expected digit, offset: 0x00000002")
      checkError("\"+1\"".getBytes("UTF-8"), "expected digit, offset: 0x00000003")
      checkError("\"+10=\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000004")
      checkError("\"+10:\"".getBytes("UTF-8"), "expected digit, offset: 0x00000005")
      checkError("\"+10:1\"".getBytes("UTF-8"), "expected digit, offset: 0x00000006")
      checkError("\"+10:10=10\"".getBytes("UTF-8"), "expected ':' or '\"', offset: 0x00000007")
      checkError("\"+10:10:X0\"".getBytes("UTF-8"), "expected digit, offset: 0x00000008")
      checkError("\"+10:10:1X\"".getBytes("UTF-8"), "expected digit, offset: 0x00000009")
      checkError("\"+10:10:10X\"".getBytes("UTF-8"), "expected '\"', offset: 0x0000000a")
      checkError("\"+18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000007")
      checkError("\"-18:10\"".getBytes("UTF-8"), "illegal zone offset, offset: 0x00000007")
      checkError("\"+20:10\"".getBytes("UTF-8"), "illegal zone offset hour, offset: 0x00000007")
      checkError("\"+10:90\"".getBytes("UTF-8"), "illegal zone offset minute, offset: 0x00000007")
      checkError("\"+10:10:60\"".getBytes("UTF-8"), "illegal zone offset second, offset: 0x0000000a")
    }
  }
  "JsonReader.readKeyAsString" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonParseException](reader("\"\"".getBytes("UTF-8")).readKeyAsString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonParseException](reader("\"\"x".getBytes("UTF-8")).readKeyAsString())
        .getMessage.contains("expected ':', offset: 0x00000002"))
    }
  }
  "JsonReader.readString and JsonReader.readKeyAsString" should {
    def check(s: String): Unit = {
      readString(s) shouldBe s
      readKeyAsString(s) shouldBe s
    }

    def checkError(bytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonParseException](reader(bytes).readString(null)).getMessage.contains(error))
      assert(intercept[JsonParseException](reader(bytes).readKeyAsString()).getMessage.contains(error))
    }

    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readString(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readKeyAsString())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null".getBytes("UTF-8")).readString("VVV") shouldBe "VVV"
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
      forAll(genControlChar, minSuccessful(1000)) { (ch: Char) =>
        checkError(Array('"', ch.toByte, '"'), "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\\".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000002")
      checkError(Array[Byte](0x22.toByte, 0xF0.toByte, 0x80.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000004")
    }
    "throw parsing exception for boolean values & numbers" in {
      checkError("true".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
      checkError("false".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
      checkError("12345".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readString(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsString(s)).getMessage.contains(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\uZ000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0Z00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00Z0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u×000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0×00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00×0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000×", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
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
      checkError("\\ud834\\d834", "illegal escape sequence, offset: 0x00000008",
        "illegal escape sequence, offset: 0x00000008")
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
      assert(intercept[JsonParseException](reader("\"x\"".getBytes("UTF-8")).readKeyAsChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonParseException](reader("\"x\"x".getBytes("UTF-8")).readKeyAsChar())
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
          checkError(("\"" + ch + ch + "\"").getBytes("UTF-8"), "expected '\"'") // offset can differs for non-ASCII characters
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
      checkError("".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000000")
      checkError("\"".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000001")
      checkError("\"\\".getBytes("UTF-8"), "unexpected end of input, offset: 0x00000002")
      checkError("\"\"".getBytes("UTF-8"), "illegal value for char, offset: 0x00000001")
      checkError(Array[Byte](0x22.toByte, 0xC0.toByte), "unexpected end of input, offset: 0x00000002")
      checkError(Array[Byte](0x22.toByte, 0xE0.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000003")
    }
    "throw parsing exception for null, boolean values & numbers" in {
      checkError("null".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
      checkError("true".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
      checkError("false".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
      checkError("12345".getBytes("UTF-8"), "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonParseException](readChar(s)).getMessage.contains(error1))
        assert(intercept[JsonParseException](readKeyAsChar(s)).getMessage.contains(error2))
      }

      checkError("\\x0008", "illegal escape sequence, offset: 0x00000002",
        "illegal escape sequence, offset: 0x00000002")
      checkError("\\uZ000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0Z00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00Z0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000Z", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
      checkError("\\u×000", "expected hex digit, offset: 0x00000003", "expected hex digit, offset: 0x00000003")
      checkError("\\u0×00", "expected hex digit, offset: 0x00000004", "expected hex digit, offset: 0x00000004")
      checkError("\\u00×0", "expected hex digit, offset: 0x00000005", "expected hex digit, offset: 0x00000005")
      checkError("\\u000×", "expected hex digit, offset: 0x00000006", "expected hex digit, offset: 0x00000006")
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

    def check2(s: String): Unit = {
      readFloat(s).toString shouldBe s
      readKeyAsFloat(s).toString shouldBe s
      readStringAsFloat(s).toString shouldBe s
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
      forAll(minSuccessful(100000)) { (n: Double) =>
        checkFloat(n.toString)
      }
      //(1 to Int.MaxValue).par.foreach { n =>
      forAll(minSuccessful(100000)) { (n: Int) =>
        val x = java.lang.Float.floatToRawIntBits(n)
        if (java.lang.Float.isFinite(x)) checkFloat(x.toString)
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
    "parse positive and negative zero" in {
      check2("0.0")
      check2("-0.0")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
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

    def check2(s: String): Unit = {
      readDouble(s).toString shouldBe s
      readKeyAsDouble(s).toString shouldBe s
      readStringAsDouble(s).toString shouldBe s
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
      forAll(minSuccessful(100000)) { (n: Float) =>
        checkDouble(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        val x = java.lang.Double.doubleToRawLongBits(n)
        if (java.lang.Double.isFinite(x)) checkDouble(x.toString)
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
    "parse positive and negative zero" in {
      check2("0.0")
      check2("-0.0")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
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
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readBigInt(null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readStringAsBigInt(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      readBigInt("null", BigInt("12345")) shouldBe BigInt("12345")
      reader("null".getBytes("UTF-8")).readStringAsBigInt(BigInt("12345")) shouldBe BigInt("12345")
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
    "parse big number values without overflow up to limits" in {
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
      readBigDecimal(" \n\t\r1234567890123456789.0123456789", null, defaultMaxScale, defaultMathContext) shouldBe
        BigDecimal("1234567890123456789.0123456789")
      readBigDecimal(" \n\t\r-1234567890123456789.0123456789", null, defaultMaxScale, defaultMathContext) shouldBe
        BigDecimal("-1234567890123456789.0123456789")
    }
    "parse valid number values and stops on not numeric chars" in {
      readBigDecimal("0$", null, defaultMaxScale, defaultMathContext) shouldBe BigDecimal("0")
      readBigDecimal("1234567890123456789$", null, defaultMaxScale, defaultMathContext) shouldBe BigDecimal("1234567890123456789")
      readBigDecimal("1234567890123456789.0123456789$", null, defaultMaxScale, defaultMathContext) shouldBe BigDecimal("1234567890123456789.0123456789")
      readBigDecimal("1234567890123456789.0123456789e10$", null, defaultMaxScale, defaultMathContext) shouldBe BigDecimal("12345678901234567890123456789")
    }
  }
  "JsonReader.readBigDecimal and JsonReader.readStringAsBigDecimal" should {
    "don't parse null value" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readBigDecimal(null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8")).readStringAsBigDecimal(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      readBigDecimal("null", BigDecimal("12345"), defaultMaxScale, defaultMathContext) shouldBe BigDecimal("12345")
      reader("null".getBytes("UTF-8")).readStringAsBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
  }
  "JsonReader.readBigDecimal, JsonReader.readKeyAsBigDecimal and JsonReader.readStringAsBigDecimal" should {
    def check(n: BigDecimal, maxScale: Int = defaultMaxScale,
              mc: MathContext = defaultMathContext): Unit = {
      val s = n.toString
      readBigDecimal(s, null, maxScale, mc) shouldBe n
      readKeyAsBigDecimal(s, maxScale, mc) shouldBe n
      readStringAsBigDecimal(s, null, maxScale, mc) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonParseException](readBigDecimal(s, null, defaultMaxScale, defaultMathContext))
        .getMessage.contains(error1))
      assert(intercept[JsonParseException](readKeyAsBigDecimal(s, defaultMaxScale, defaultMathContext))
        .getMessage.contains(error2))
      assert(intercept[JsonParseException](readStringAsBigDecimal(s, null, defaultMaxScale, defaultMathContext))
        .getMessage.contains(error2))
    }

    "parse valid number values with scale less than specified maximum" in {
      forAll(minSuccessful(100000)) { (n: BigDecimal) =>
        check(n, Int.MaxValue, MathContext.UNLIMITED)
      }
    }
    "parse big number values without overflow up to limits" in {
      check(BigDecimal("12345e67"))
      check(BigDecimal("-12345e67"))
      check(BigDecimal("1234567890123456789012345678901234567890e-123456789"), Int.MaxValue, MathContext.UNLIMITED)
      check(BigDecimal("-1234567890123456789012345678901234567890e-123456789"), Int.MaxValue, MathContext.UNLIMITED)
    }
    "parse small number values without underflow up to limits" in {
      check(BigDecimal("12345e-67"))
      check(BigDecimal("-12345e-67"))
      check(BigDecimal("1234567890123456789012345678901234567890e-123456789"), Int.MaxValue, MathContext.UNLIMITED)
      check(BigDecimal("-1234567890123456789012345678901234567890e-123456789"), Int.MaxValue, MathContext.UNLIMITED)
    }
    "throw number format exception for too big exponents" in {
      checkError("12345678901234567890e1234",
        "illegal number, offset: 0x00000018", "illegal number, offset: 0x00000019")
      checkError("-12345678901234567890e1234",
        "illegal number, offset: 0x00000019", "illegal number, offset: 0x0000001a")
      checkError("12345678901234567890e-1234",
        "illegal number, offset: 0x00000019", "illegal number, offset: 0x0000001a")
      checkError("-12345678901234567890e-1234",
        "illegal number, offset: 0x0000001a", "illegal number, offset: 0x0000001b")
      checkError("12345678901234567890e1234",
        "illegal number, offset: 0x00000018", "illegal number, offset: 0x00000019")
    }
    "throw parsing exception on illegal or empty input" in {
      checkError("", "unexpected end of input, offset: 0x00000000", "illegal number, offset: 0x00000001")
      checkError(" ", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000001")
      checkError("-", "unexpected end of input, offset: 0x00000001", "illegal number, offset: 0x00000002")
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
        assert(intercept[JsonParseException](readBigDecimal(s, null, defaultMaxScale, defaultMathContext))
          .getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.setMark and JsonReader.rollbackToMark" should {
    "store current position of parsing and return back to it" in {
      val jsonReader = reader("{}".getBytes("UTF-8"))
      jsonReader.setMark()
      jsonReader.skip()
      jsonReader.rollbackToMark()
      jsonReader.nextToken() shouldBe '{'
    }
    "throw exception in case of rollbackToMark was called before setMark" in {
      val jsonReader = reader("{}".getBytes("UTF-8"))
      jsonReader.skip()
      assert(intercept[ArrayIndexOutOfBoundsException](jsonReader.rollbackToMark())
        .getMessage.contains("expected preceding call of 'setMark()'"))
    }
  }
  "JsonReader.skipToKey" should {
    "return true in case of key is found and set current position of parsing to its value" in {
      val jsonReader = reader("""{"key1":1,"key2":2}""".getBytes("UTF-8"))
      jsonReader.isNextToken('{') // enter to JSON object
      jsonReader.skipToKey("key2") shouldBe true
      jsonReader.readInt() shouldBe 2
    }
    "return false in case of key cannot be found and set current positing to the closing of object" in {
      val jsonReader = reader("""{"key1":1}""".getBytes("UTF-8"))
      jsonReader.isNextToken('{') // enter to JSON object
      jsonReader.skipToKey("key2")
      jsonReader.isCurrentToken('}') shouldBe true
    }
  }
  "JsonReader.requiredFieldError" should {
    "throw parsing exception with missing required field" in {
      val jsonReader = reader("{}".getBytes("UTF-8"))
      jsonReader.nextToken()
      assert(intercept[JsonParseException](jsonReader.requiredFieldError("name"))
        .getMessage.contains("missing required field \"name\", offset: 0x00000000"))
    }
  }
  "JsonReader.duplicatedKeyError" should {
    "throw parsing exception with name of duplicated key" in {
      val jsonReader = reader("\"xxx\"".getBytes("UTF-8"))
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonParseException](jsonReader.duplicatedKeyError(len))
        .getMessage.contains("duplicated field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.unexpectedKeyError" should {
    "throw parsing exception with name of unexpected key" in {
      val jsonReader = reader("\"xxx\"".getBytes("UTF-8"))
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonParseException](jsonReader.unexpectedKeyError(len))
        .getMessage.contains("unexpected field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.discriminatorValueError" should {
    val jsonReader = reader("\"xxx\"".getBytes("UTF-8"))
    val value = jsonReader.readString(null)
    "throw parsing exception with unexpected discriminator value" in {
      assert(intercept[JsonParseException](jsonReader.discriminatorValueError(value))
       .getMessage.contains("illegal value of discriminator field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.enumValueError" should {
    val jsonReader = reader("\"xxx\"".getBytes("UTF-8"))
    val value = jsonReader.readString(null)
    "throw parsing exception with unexpected enum value" in {
      assert(intercept[JsonParseException](jsonReader.enumValueError(value))
        .getMessage.contains("illegal enum value \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.commaError" should {
    val jsonReader = reader("{}".getBytes("UTF-8"))
    jsonReader.isNextToken(',')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.commaError())
        .getMessage.contains("expected ',', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayStartOrNullError" should {
    val jsonReader = reader("{}".getBytes("UTF-8"))
    jsonReader.isNextToken('[')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayStartOrNullError())
        .getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndError" should {
    val jsonReader = reader("}".getBytes("UTF-8"))
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayEndError())
        .getMessage.contains("expected ']', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndOrCommaError" should {
    val jsonReader = reader("}".getBytes("UTF-8"))
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.arrayEndOrCommaError())
        .getMessage.contains("expected ']' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader.objectStartOrNullError" should {
    val jsonReader = reader("[]".getBytes("UTF-8"))
    jsonReader.isNextToken('{')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectStartOrNullError())
        .getMessage.contains("expected '{' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.objectEndOrCommaError" should {
    val jsonReader = reader("]".getBytes("UTF-8"))
    jsonReader.isNextToken('}')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonParseException](jsonReader.objectEndOrCommaError())
        .getMessage.contains("expected '}' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader" should {
    "support hex dumps with offsets that greater than 4Gb" in {
      assert(intercept[JsonParseException](reader("null".getBytes("UTF-8"), 1L << 41).readInt())
        .getMessage.contains(
          """illegal number, offset: 0x20000000000, buf:
            |           +-------------------------------------------------+
            |           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 6e 75 6c 6c                                     | null             |
            |+----------+-------------------------------------------------+------------------+""".stripMargin))
    }
  }

  def validateSkip(s: String): Unit = {
    def checkWithSuffix(s: String, suffix: Char): Unit = {
      val r = reader((s + suffix).getBytes("UTF-8"))
      r.skip()
      r.nextToken().toChar shouldBe suffix
    }

    def check(s: String): Unit = {
      val r = reader(s.getBytes("UTF-8"))
      r.skip()
      assert(intercept[JsonParseException](r.nextToken()).getMessage.contains("unexpected end of input"))
    }

    checkWithSuffix(s, ',')
    checkWithSuffix(s, '}')
    checkWithSuffix(s, ']')
    check(s)
  }

  def readInstant(s: String): Instant = readInstant(s.getBytes("UTF-8"))

  def readInstant(buf: Array[Byte]): Instant = reader(stringify(buf)).readInstant(null)

  def readDuration(s: String): Duration = readDuration(s.getBytes("UTF-8"))

  def readDuration(buf: Array[Byte]): Duration = reader(stringify(buf)).readDuration(null)

  def readLocalDate(s: String): LocalDate = readLocalDate(s.getBytes("UTF-8"))

  def readLocalDate(buf: Array[Byte]): LocalDate = reader(stringify(buf)).readLocalDate(null)

  def readLocalDateTime(s: String): LocalDateTime = readLocalDateTime(s.getBytes("UTF-8"))

  def readLocalDateTime(buf: Array[Byte]): LocalDateTime = reader(stringify(buf)).readLocalDateTime(null)

  def readLocalTime(s: String): LocalTime = readLocalTime(s.getBytes("UTF-8"))

  def readLocalTime(buf: Array[Byte]): LocalTime = reader(stringify(buf)).readLocalTime(null)

  def readMonthDay(s: String): MonthDay = readMonthDay(s.getBytes("UTF-8"))

  def readMonthDay(buf: Array[Byte]): MonthDay = reader(stringify(buf)).readMonthDay(null)

  def readOffsetDateTime(s: String): OffsetDateTime = readOffsetDateTime(s.getBytes("UTF-8"))

  def readOffsetDateTime(buf: Array[Byte]): OffsetDateTime = reader(stringify(buf)).readOffsetDateTime(null)

  def readOffsetTime(s: String): OffsetTime = readOffsetTime(s.getBytes("UTF-8"))

  def readOffsetTime(buf: Array[Byte]): OffsetTime = reader(stringify(buf)).readOffsetTime(null)

  def readPeriod(s: String): Period = readPeriod(s.getBytes("UTF-8"))

  def readPeriod(buf: Array[Byte]): Period = reader(stringify(buf)).readPeriod(null)

  def readYear(s: String): Year = readYear(s.getBytes("UTF-8"))

  def readYear(buf: Array[Byte]): Year = reader(stringify(buf)).readYear(null)

  def readYearMonth(s: String): YearMonth = readYearMonth(s.getBytes("UTF-8"))

  def readYearMonth(buf: Array[Byte]): YearMonth = reader(stringify(buf)).readYearMonth(null)

  def readZonedDateTime(s: String): ZonedDateTime = readZonedDateTime(s.getBytes("UTF-8"))

  def readZonedDateTime(buf: Array[Byte]): ZonedDateTime = reader(stringify(buf)).readZonedDateTime(null)

  def readZoneOffset(s: String): ZoneOffset = readZoneOffset(s.getBytes("UTF-8"))

  def readZoneOffset(buf: Array[Byte]): ZoneOffset = reader(stringify(buf)).readZoneOffset(null)

  def readZoneId(s: String): ZoneId = readZoneId(s.getBytes("UTF-8"))

  def readZoneId(buf: Array[Byte]): ZoneId = reader(stringify(buf)).readZoneId(null)

  def readUUID(s: String): UUID = readUUID(s.getBytes("UTF-8"))

  def readUUID(buf: Array[Byte]): UUID = reader(stringify(buf)).readUUID(null)

  def readString(s: String): String = readString(s.getBytes("UTF-8"))

  def readString(buf: Array[Byte]): String = reader(stringify(buf)).readString(null)

  def readChar(s: String): Char = readChar(s.getBytes("UTF-8"))

  def readChar(buf: Array[Byte]): Char = reader(stringify(buf)).readChar()

  def readByte(s: String): Byte = readByte(s.getBytes("UTF-8"))

  def readByte(buf: Array[Byte]): Byte = reader(buf).readByte()

  def readShort(s: String): Short = readShort(s.getBytes("UTF-8"))

  def readShort(buf: Array[Byte]): Short = reader(buf).readShort()

  def readInt(s: String): Int = readInt(s.getBytes("UTF-8"))

  def readInt(buf: Array[Byte]): Int = reader(buf).readInt()

  def readLong(s: String): Long = readLong(s.getBytes("UTF-8"))

  def readLong(buf: Array[Byte]): Long = reader(buf).readLong()

  def readFloat(s: String): Float = readFloat(s.getBytes("UTF-8"))

  def readFloat(buf: Array[Byte]): Float = reader(buf).readFloat()

  def readDouble(s: String): Double = readDouble(s.getBytes("UTF-8"))

  def readDouble(buf: Array[Byte]): Double = reader(buf).readDouble()

  def readBigInt(s: String, default: BigInt): BigInt = readBigInt(s.getBytes("UTF-8"), default)

  def readBigInt(buf: Array[Byte], default: BigInt): BigInt = reader(buf).readBigInt(default)

  def readBigDecimal(s: String, default: BigDecimal, maxScale: Int, mc: MathContext): BigDecimal =
    readBigDecimal(s.getBytes("UTF-8"), default, maxScale, mc)

  def readBigDecimal(buf: Array[Byte], default: BigDecimal, maxScale: Int, mc: MathContext): BigDecimal =
    reader(buf).readBigDecimal(default, maxScale, mc)

  def readKeyAsInstant(s: String): Instant = readKeyAsInstant(s.getBytes("UTF-8"))

  def readKeyAsInstant(buf: Array[Byte]): Instant = reader(stringify(buf) :+ ':'.toByte).readKeyAsInstant()

  def readKeyAsDuration(s: String): Duration = readKeyAsDuration(s.getBytes("UTF-8"))

  def readKeyAsDuration(buf: Array[Byte]): Duration =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsDuration()

  def readKeyAsLocalDate(s: String): LocalDate = readKeyAsLocalDate(s.getBytes("UTF-8"))

  def readKeyAsLocalDate(buf: Array[Byte]): LocalDate =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalDate()

  def readKeyAsLocalDateTime(s: String): LocalDateTime = readKeyAsLocalDateTime(s.getBytes("UTF-8"))

  def readKeyAsLocalDateTime(buf: Array[Byte]): LocalDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalDateTime()

  def readKeyAsLocalTime(s: String): LocalTime = readKeyAsLocalTime(s.getBytes("UTF-8"))

  def readKeyAsLocalTime(buf: Array[Byte]): LocalTime = reader(stringify(buf) :+ ':'.toByte).readKeyAsLocalTime()

  def readKeyAsMonthDay(s: String): MonthDay = readKeyAsMonthDay(s.getBytes("UTF-8"))

  def readKeyAsMonthDay(buf: Array[Byte]): MonthDay = reader(stringify(buf) :+ ':'.toByte).readKeyAsMonthDay()

  def readKeyAsOffsetDateTime(s: String): OffsetDateTime = readKeyAsOffsetDateTime(s.getBytes("UTF-8"))

  def readKeyAsOffsetDateTime(buf: Array[Byte]): OffsetDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsOffsetDateTime()

  def readKeyAsOffsetTime(s: String): OffsetTime = readKeyAsOffsetTime(s.getBytes("UTF-8"))

  def readKeyAsOffsetTime(buf: Array[Byte]): OffsetTime = reader(stringify(buf) :+ ':'.toByte).readKeyAsOffsetTime()

  def readKeyAsPeriod(s: String): Period = readKeyAsPeriod(s.getBytes("UTF-8"))

  def readKeyAsPeriod(buf: Array[Byte]): Period = reader(stringify(buf) :+ ':'.toByte).readKeyAsPeriod()

  def readKeyAsYear(s: String): Year = readKeyAsYear(s.getBytes("UTF-8"))

  def readKeyAsYear(buf: Array[Byte]): Year = reader(stringify(buf) :+ ':'.toByte).readKeyAsYear()

  def readKeyAsYearMonth(s: String): YearMonth = readKeyAsYearMonth(s.getBytes("UTF-8"))

  def readKeyAsYearMonth(buf: Array[Byte]): YearMonth = reader(stringify(buf) :+ ':'.toByte).readKeyAsYearMonth()

  def readKeyAsZonedDateTime(s: String): ZonedDateTime = readKeyAsZonedDateTime(s.getBytes("UTF-8"))

  def readKeyAsZonedDateTime(buf: Array[Byte]): ZonedDateTime =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsZonedDateTime()

  def readKeyAsZoneOffset(s: String): ZoneOffset = readKeyAsZoneOffset(s.getBytes("UTF-8"))

  def readKeyAsZoneOffset(buf: Array[Byte]): ZoneOffset = reader(stringify(buf) :+ ':'.toByte).readKeyAsZoneOffset()

  def readKeyAsZoneId(s: String): ZoneId = readKeyAsZoneId(s.getBytes("UTF-8"))

  def readKeyAsZoneId(buf: Array[Byte]): ZoneId = reader(stringify(buf) :+ ':'.toByte).readKeyAsZoneId()

  def readKeyAsUUID(s: String): UUID = readKeyAsUUID(s.getBytes("UTF-8"))

  def readKeyAsUUID(buf: Array[Byte]): UUID = reader(stringify(buf) :+ ':'.toByte).readKeyAsUUID()

  def readKeyAsString(s: String): String = readKeyAsString(s.getBytes("UTF-8"))

  def readKeyAsString(buf: Array[Byte]): String = reader(stringify(buf) :+ ':'.toByte).readKeyAsString()

  def readKeyAsChar(s: String): Char = readKeyAsChar(s.getBytes("UTF-8"))

  def readKeyAsChar(buf: Array[Byte]): Char = reader(stringify(buf) :+ ':'.toByte).readKeyAsChar()

  def readKeyAsByte(s: String): Byte = readKeyAsByte(s.getBytes("UTF-8"))

  def readKeyAsByte(buf: Array[Byte]): Byte = reader(stringify(buf) :+ ':'.toByte).readKeyAsByte()

  def readKeyAsShort(s: String): Short = readKeyAsShort(s.getBytes("UTF-8"))

  def readKeyAsShort(buf: Array[Byte]): Short = reader(stringify(buf) :+ ':'.toByte).readKeyAsShort()

  def readKeyAsInt(s: String): Int = readKeyAsInt(s.getBytes("UTF-8"))

  def readKeyAsInt(buf: Array[Byte]): Int = reader(stringify(buf) :+ ':'.toByte).readKeyAsInt()

  def readKeyAsLong(s: String): Long = readKeyAsLong(s.getBytes("UTF-8"))

  def readKeyAsLong(buf: Array[Byte]): Long = reader(stringify(buf) :+ ':'.toByte).readKeyAsLong()

  def readKeyAsFloat(s: String): Float = readKeyAsFloat(s.getBytes("UTF-8"))

  def readKeyAsFloat(buf: Array[Byte]): Float = reader(stringify(buf) :+ ':'.toByte).readKeyAsFloat()

  def readKeyAsDouble(s: String): Double = readKeyAsDouble(s.getBytes("UTF-8"))

  def readKeyAsDouble(buf: Array[Byte]): Double = reader(stringify(buf) :+ ':'.toByte).readKeyAsDouble()

  def readKeyAsBigInt(s: String): BigInt = readKeyAsBigInt(s.getBytes("UTF-8"))

  def readKeyAsBigInt(buf: Array[Byte]): BigInt = reader(stringify(buf) :+ ':'.toByte).readKeyAsBigInt()

  def readKeyAsBigDecimal(s: String, maxScale: Int, mc: MathContext): BigDecimal =
    readKeyAsBigDecimal(s.getBytes("UTF-8"), maxScale, mc)

  def readKeyAsBigDecimal(buf: Array[Byte], maxScale: Int, mc: MathContext): BigDecimal =
    reader(stringify(buf) :+ ':'.toByte).readKeyAsBigDecimal(maxScale, mc)

  def readStringAsByte(s: String): Byte = readStringAsByte(s.getBytes("UTF-8"))

  def readStringAsByte(buf: Array[Byte]): Byte = reader(stringify(buf)).readStringAsByte()

  def readStringAsShort(s: String): Short = readStringAsShort(s.getBytes("UTF-8"))

  def readStringAsShort(buf: Array[Byte]): Short = reader(stringify(buf)).readStringAsShort()

  def readStringAsInt(s: String): Int = readStringAsInt(s.getBytes("UTF-8"))

  def readStringAsInt(buf: Array[Byte]): Int = reader(stringify(buf)).readStringAsInt()

  def readStringAsLong(s: String): Long = readStringAsLong(s.getBytes("UTF-8"))

  def readStringAsLong(buf: Array[Byte]): Long = reader(stringify(buf)).readStringAsLong()

  def readStringAsFloat(s: String): Float = readStringAsFloat(s.getBytes("UTF-8"))

  def readStringAsFloat(buf: Array[Byte]): Float = reader(stringify(buf)).readStringAsFloat()

  def readStringAsDouble(s: String): Double = readStringAsDouble(s.getBytes("UTF-8"))

  def readStringAsDouble(buf: Array[Byte]): Double = reader(stringify(buf)).readStringAsDouble()

  def readStringAsBigInt(s: String, default: BigInt): BigInt = readStringAsBigInt(s.getBytes("UTF-8"), default)

  def readStringAsBigInt(buf: Array[Byte], default: BigInt): BigInt = reader(stringify(buf)).readStringAsBigInt(default)

  def readStringAsBigDecimal(s: String, default: BigDecimal, maxScale: Int, mc: MathContext): BigDecimal =
    readStringAsBigDecimal(s.getBytes("UTF-8"), default, maxScale, mc)

  def readStringAsBigDecimal(buf: Array[Byte], default: BigDecimal, maxScale: Int, mc: MathContext): BigDecimal =
    reader(stringify(buf)).readStringAsBigDecimal(default, maxScale, mc)

  def reader(buf: Array[Byte], totalRead: Long = 0): JsonReader =
    new JsonReader(new Array[Byte](12), // a minimal allowed length of `buf`
      0, 0, 2147483647, new Array[Char](0), null, new ByteArrayInputStream(buf), totalRead, ReaderConfig())

  def stringify(buf: Array[Byte]): Array[Byte] = '"'.toByte +: buf :+ '"'.toByte
}