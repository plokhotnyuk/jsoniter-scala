package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.ByteArrayInputStream
import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader._
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class JsonReaderSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {
  "ReaderConfig.<init>" should {
    "have safe and handy defaults" in {
      ReaderConfig().throwReaderExceptionWithStackTrace shouldBe false
      ReaderConfig().appendHexDumpToParseException shouldBe true
      ReaderConfig().preferredBufSize shouldBe 16384
      ReaderConfig().preferredCharBufSize shouldBe 1024
    }
    "throw exception in case for unsupported values of params" in {
      assert(intercept[IllegalArgumentException](ReaderConfig(preferredBufSize = 11))
        .getMessage.contains("'preferredBufSize' should be not less than 12"))
      assert(intercept[IllegalArgumentException](ReaderConfig(preferredCharBufSize = -1))
        .getMessage.contains("'preferredCharBufSize' should be not less than 0"))
    }
  }
  "JsonReader.toHashCode" should {
    "produce the same hash value for strings as JDK by default" in {
      forAll(minSuccessful(100000)) { (x: String) =>
        assert(JsonReader.toHashCode(x.toCharArray, x.length) == x.hashCode)
      }
    }
    "produce 0 hash value when the provided 'len' isn't greater than 0" in {
      forAll(minSuccessful(100000)) { (x: Int) =>
        whenever(x <= 0) {
          assert(JsonReader.toHashCode("VVV".toCharArray, x) == 0)
        }
      }
    }
    "throw exception in case null value for the char array is provided" in {
      intercept[NullPointerException](JsonReader.toHashCode(null, 1))
    }
    "throw exception in case the char array length is less than the provided 'len'" in {
      intercept[ArrayIndexOutOfBoundsException](JsonReader.toHashCode("XXX".toCharArray, 4))
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
      assert(intercept[JsonReaderException](validateSkip("\""))
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](validateSkip("\"abc"))
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
      assert(intercept[JsonReaderException](validateSkip("t"))
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](validateSkip("f"))
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
      assert(intercept[JsonReaderException](validateSkip("{{}"))
        .getMessage.contains("unexpected end of input, offset: 0x00000004"))
    }
    "skip array values" in {
      validateSkip("[]")
      validateSkip(" \n\t\r[[[[[]]]][[[]]]]")
      validateSkip("[\"[\"]")
    }
    "throw parsing exception when skipping not closed array" in {
      assert(intercept[JsonReaderException](validateSkip("[[]"))
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
      def checkError(invalidInput: String): Unit =
        assert(intercept[JsonReaderException](validateSkip(invalidInput))
          .getMessage.contains("expected value, offset: 0x00000000"))

      checkError("]")
      checkError("}")
      checkError(",")
      checkError(":")
    }
  }
  "JsonReader.nextByte" should {
    "return next byte of input" in {
      val r = reader("{\n}")
      assert(r.nextByte() == '{')
      assert(r.nextByte() == '\n')
      assert(r.nextByte() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}")
      r.skip()
      assert(intercept[JsonReaderException](r.nextByte() == '{')
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.nextToken" should {
    "find next non-whitespace byte of input" in {
      val r = reader("{\n}")
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '}')
    }
    "throw parse exception in case of end of input" in {
      val r = reader("{}")
      r.skip()
      assert(intercept[JsonReaderException](r.nextToken() == '{')
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
    }
  }
  "JsonReader.readNullOrError" should {
    "parse null value" in {
      val r = reader("null")
      r.isNextToken('n') shouldBe true
      r.readNullOrError("default", "error") shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      assert(intercept[JsonReaderException] {
        val r = reader("nul")
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null")
      }.getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonReaderException] {
        val r = reader("xull")
        r.isNextToken('x') shouldBe true
        r.readNullOrError("default", "expected null")
      }.getMessage.contains("expected null, offset: 0x00000000"))
      assert(intercept[JsonReaderException] {
        val r = reader("nxll")
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null")
      }.getMessage.contains("expected null, offset: 0x00000001"))
      assert(intercept[JsonReaderException] {
        val r = reader("nuxl")
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null")
      }.getMessage.contains("expected null, offset: 0x00000002"))
      assert(intercept[JsonReaderException] {
        val r = reader("nulx")
        r.isNextToken('n') shouldBe true
        r.readNullOrError("default", "expected null")
      }.getMessage.contains("expected null, offset: 0x00000003"))
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("null").readNullOrError("default", "error"))
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readNullOrTokenError" should {
    "parse null value" in {
      val r = reader("null")
      r.isNextToken('n') shouldBe true
      r.readNullOrTokenError("default", 'x') shouldBe "default"
    }
    "throw parse exception in case of invalid null value" in {
      assert(intercept[JsonReaderException] {
        val r = reader("nul")
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x')
      }.getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonReaderException] {
        val r = reader("yull")
        r.isNextToken('y') shouldBe true
        r.readNullOrTokenError("default", 'x')
      }.getMessage.contains("expected 'x' or null, offset: 0x00000000"))
      assert(intercept[JsonReaderException] {
        val r = reader("nxll")
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x')
      }.getMessage.contains("expected 'x' or null, offset: 0x00000001"))
      assert(intercept[JsonReaderException] {
        val r = reader("nuxl")
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x')
      }.getMessage.contains("expected 'x' or null, offset: 0x00000002"))
      assert(intercept[JsonReaderException] {
        val r = reader("nulx")
        r.isNextToken('n') shouldBe true
        r.readNullOrTokenError("default", 'x')
      }.getMessage.contains("expected 'x' or null, offset: 0x00000003"))
    }
    "throw array index out of bounds exception in case of call without preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("null").readNullOrError("default", "error"))
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.rollbackToken" should {
    "rollback of reading last byte of input" in {
      val r = reader("""{"x":1}""")
      assert(r.nextToken() == '{')
      r.rollbackToken()
      assert(r.nextToken() == '{')
      assert(r.nextToken() == '"')
      r.rollbackToken()
      assert(r.nextToken() == '"')
    }
    "throw array index out of bounds exception in case of missing preceding call of 'nextToken()' or 'isNextToken()'" in {
      assert(intercept[ArrayIndexOutOfBoundsException](reader("{}").rollbackToken())
        .getMessage.contains("expected preceding call of 'nextToken()' or 'isNextToken()'"))
    }
  }
  "JsonReader.readBoolean, JsonReader.readStringAsBoolean and JsonReader.readKeyAsBoolean" should {
    def check(s: String, value: Boolean): Unit = {
      reader(s).readBoolean() shouldBe value
      reader(s""""$s"""").readStringAsBoolean() shouldBe value
      reader(s""""$s":""").readKeyAsBoolean() shouldBe value
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBoolean()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBoolean()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBoolean()).getMessage.contains(error2))
    }

    "parse valid true and false values" in {
      check("true", value = true)
      check("false", value = false)
    }
    "throw parsing exception for empty input and illegal or broken value" in {
      checkError("xrue", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("txue", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      checkError("trxe", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      checkError("trux", "illegal boolean, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("xalse", "illegal boolean, offset: 0x00000000", "illegal boolean, offset: 0x00000001")
      checkError("fxlse", "illegal boolean, offset: 0x00000001", "illegal boolean, offset: 0x00000002")
      checkError("faxse", "illegal boolean, offset: 0x00000002", "illegal boolean, offset: 0x00000003")
      checkError("falxe", "illegal boolean, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("falsx", "illegal boolean, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
      checkError("tru", "unexpected end of input, offset: 0x00000003", "illegal boolean, offset: 0x00000004")
      checkError("fals", "unexpected end of input, offset: 0x00000004", "illegal boolean, offset: 0x00000005")
    }
  }
  "JsonReader.readKeyAsUUID" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader("\"00000000-0000-0000-0000-000000000000\"").readKeyAsUUID())
        .getMessage.contains("unexpected end of input, offset: 0x00000026"))
      assert(intercept[JsonReaderException](reader("\"00000000-0000-0000-0000-000000000000\"x").readKeyAsUUID())
        .getMessage.contains("expected ':', offset: 0x00000026"))
    }
  }
  "JsonReader.readUUID and JsonReader.readKeyAsUUID" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readUUID(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsUUID())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = new UUID(0, 0)
      reader("null").readUUID(default) shouldBe default
    }
    "parse UUID from a string representation according to format that defined in IETF RFC4122 (section 3)" in {
      def check(x: UUID): Unit = {
        val s = x.toString
        reader(s""""${s.toLowerCase}"""").readUUID(null) shouldBe x
        reader(s""""${s.toUpperCase}"""").readUUID(null) shouldBe x
        reader(s""""${s.toLowerCase}":""").readKeyAsUUID() shouldBe x
        reader(s""""${s.toUpperCase}":""").readKeyAsUUID() shouldBe x
      }

      forAll(Gen.uuid, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken UUID string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readUUID(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsUUID()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\"", "unexpected end of input, offset: 0x00000002")
      checkError("\"00000000-0000-0000-0000-000000000000", "unexpected end of input, offset: 0x00000025")
      checkError("\"Z0000000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000001")
      checkError("\"0Z000000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000002")
      checkError("\"00Z00000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000003")
      checkError("\"000Z0000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000004")
      checkError("\"0000Z000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000005")
      checkError("\"00000Z00-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000006")
      checkError("\"000000Z0-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000007")
      checkError("\"0000000Z-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000008")
      checkError("\"00000000=0000-0000-0000-000000000000\"", "expected '-', offset: 0x00000009")
      checkError("\"00000000-Z000-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000a")
      checkError("\"00000000-0Z00-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000b")
      checkError("\"00000000-00Z0-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000c")
      checkError("\"00000000-000Z-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000d")
      checkError("\"00000000-0000=0000-0000-000000000000\"", "expected '-', offset: 0x0000000e")
      checkError("\"00000000-0000-Z000-0000-000000000000\"", "expected hex digit, offset: 0x0000000f")
      checkError("\"00000000-0000-0Z00-0000-000000000000\"", "expected hex digit, offset: 0x00000010")
      checkError("\"00000000-0000-00Z0-0000-000000000000\"", "expected hex digit, offset: 0x00000011")
      checkError("\"00000000-0000-000Z-0000-000000000000\"", "expected hex digit, offset: 0x00000012")
      checkError("\"00000000-0000-0000=0000-000000000000\"", "expected '-', offset: 0x00000013")
      checkError("\"00000000-0000-0000-Z000-000000000000\"", "expected hex digit, offset: 0x00000014")
      checkError("\"00000000-0000-0000-0Z00-000000000000\"", "expected hex digit, offset: 0x00000015")
      checkError("\"00000000-0000-0000-00Z0-000000000000\"", "expected hex digit, offset: 0x00000016")
      checkError("\"00000000-0000-0000-000Z-000000000000\"", "expected hex digit, offset: 0x00000017")
      checkError("\"00000000-0000-0000-0000=000000000000\"", "expected '-', offset: 0x00000018")
      checkError("\"00000000-0000-0000-0000-Z00000000000\"", "expected hex digit, offset: 0x00000019")
      checkError("\"00000000-0000-0000-0000-0Z0000000000\"", "expected hex digit, offset: 0x0000001a")
      checkError("\"00000000-0000-0000-0000-00Z000000000\"", "expected hex digit, offset: 0x0000001b")
      checkError("\"00000000-0000-0000-0000-000Z00000000\"", "expected hex digit, offset: 0x0000001c")
      checkError("\"00000000-0000-0000-0000-0000Z0000000\"", "expected hex digit, offset: 0x0000001d")
      checkError("\"00000000-0000-0000-0000-00000Z000000\"", "expected hex digit, offset: 0x0000001e")
      checkError("\"00000000-0000-0000-0000-000000Z00000\"", "expected hex digit, offset: 0x0000001f")
      checkError("\"00000000-0000-0000-0000-0000000Z0000\"", "expected hex digit, offset: 0x00000020")
      checkError("\"00000000-0000-0000-0000-00000000Z000\"", "expected hex digit, offset: 0x00000021")
      checkError("\"00000000-0000-0000-0000-000000000Z00\"", "expected hex digit, offset: 0x00000022")
      checkError("\"00000000-0000-0000-0000-0000000000Z0\"", "expected hex digit, offset: 0x00000023")
      checkError("\"00000000-0000-0000-0000-00000000000Z\"", "expected hex digit, offset: 0x00000024")
      checkError("\"×0000000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000001")
      checkError("\"0×000000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000002")
      checkError("\"00×00000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000003")
      checkError("\"000×0000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000004")
      checkError("\"0000×000-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000005")
      checkError("\"00000×00-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000006")
      checkError("\"000000×0-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000007")
      checkError("\"0000000×-0000-0000-0000-000000000000\"", "expected hex digit, offset: 0x00000008")
      checkError("\"00000000÷0000-0000-0000-000000000000\"", "expected '-', offset: 0x00000009")
      checkError("\"00000000-×000-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000a")
      checkError("\"00000000-0×00-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000b")
      checkError("\"00000000-00×0-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000c")
      checkError("\"00000000-000×-0000-0000-000000000000\"", "expected hex digit, offset: 0x0000000d")
      checkError("\"00000000-0000÷0000-0000-000000000000\"", "expected '-', offset: 0x0000000e")
      checkError("\"00000000-0000-×000-0000-000000000000\"", "expected hex digit, offset: 0x0000000f")
      checkError("\"00000000-0000-0×00-0000-000000000000\"", "expected hex digit, offset: 0x00000010")
      checkError("\"00000000-0000-00×0-0000-000000000000\"", "expected hex digit, offset: 0x00000011")
      checkError("\"00000000-0000-000×-0000-000000000000\"", "expected hex digit, offset: 0x00000012")
      checkError("\"00000000-0000-0000÷0000-000000000000\"", "expected '-', offset: 0x00000013")
      checkError("\"00000000-0000-0000-×000-000000000000\"", "expected hex digit, offset: 0x00000014")
      checkError("\"00000000-0000-0000-0×00-000000000000\"", "expected hex digit, offset: 0x00000015")
      checkError("\"00000000-0000-0000-00×0-000000000000\"", "expected hex digit, offset: 0x00000016")
      checkError("\"00000000-0000-0000-000×-000000000000\"", "expected hex digit, offset: 0x00000017")
      checkError("\"00000000-0000-0000-0000÷000000000000\"", "expected '-', offset: 0x00000018")
      checkError("\"00000000-0000-0000-0000-×00000000000\"", "expected hex digit, offset: 0x00000019")
      checkError("\"00000000-0000-0000-0000-0×0000000000\"", "expected hex digit, offset: 0x0000001a")
      checkError("\"00000000-0000-0000-0000-00×000000000\"", "expected hex digit, offset: 0x0000001b")
      checkError("\"00000000-0000-0000-0000-000×00000000\"", "expected hex digit, offset: 0x0000001c")
      checkError("\"00000000-0000-0000-0000-0000×0000000\"", "expected hex digit, offset: 0x0000001d")
      checkError("\"00000000-0000-0000-0000-00000×000000\"", "expected hex digit, offset: 0x0000001e")
      checkError("\"00000000-0000-0000-0000-000000×00000\"", "expected hex digit, offset: 0x0000001f")
      checkError("\"00000000-0000-0000-0000-0000000×0000\"", "expected hex digit, offset: 0x00000020")
      checkError("\"00000000-0000-0000-0000-00000000×000\"", "expected hex digit, offset: 0x00000021")
      checkError("\"00000000-0000-0000-0000-000000000×00\"", "expected hex digit, offset: 0x00000022")
      checkError("\"00000000-0000-0000-0000-0000000000×0\"", "expected hex digit, offset: 0x00000023")
      checkError("\"00000000-0000-0000-0000-00000000000×\"", "expected hex digit, offset: 0x00000024")
      checkError("\"00000000-0000-0000-0000-000000000000x", "expected '\"', offset: 0x00000025")
    }
  }
  "JsonReader.readKeyAsInstant" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader("\"2008-01-20T07:24:33Z\"").readKeyAsInstant())
        .getMessage.contains("unexpected end of input, offset: 0x00000016"))
      assert(intercept[JsonReaderException](reader("\"2008-01-20T07:24:33Z\"x").readKeyAsInstant())
        .getMessage.contains("expected ':', offset: 0x00000016"))
    }
  }
  "JsonReader.readDuration and JsonReader.readKeyAsDuration" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readDuration(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsDuration())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Duration.parse("P2DT3H4M")
      reader("null").readDuration(default) shouldBe default
    }
    "parse Duration from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(s: String, x: Duration): Unit = {
        reader(s""""$s"""").readDuration(null) shouldBe x
        reader(s""""$s":""").readKeyAsDuration() shouldBe x
        reader(s""""-$s"""").readDuration(null) shouldBe x.negated()
        reader(s""""-$s":""").readKeyAsDuration() shouldBe x.negated()
      }

      check("P0D", Duration.ZERO)
      check("PT0S", Duration.ZERO)
      forAll(genDuration, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken Duration string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readDuration(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsDuration()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\"", "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"", "expected 'P', offset: 0x00000002")
      checkError("\"PXD\"", "expected '-' or digit, offset: 0x00000002")
      checkError("\"PT0SX", "expected '\"', offset: 0x00000005")
      checkError("\"P-XD\"", "expected digit, offset: 0x00000003")
      checkError("\"P1XD\"", "expected 'D' or digit, offset: 0x00000003")
      checkError("\"P106751991167301D\"", "llegal duration, offset: 0x00000011")
      checkError("\"P1067519911673000D\"", "llegal duration, offset: 0x00000012")
      checkError("\"P-106751991167301D\"", "llegal duration, offset: 0x00000012")
      checkError("\"P1DX1H\"", "expected 'T' or '\"', offset: 0x00000004")
      checkError("\"P1DTXH\"", "expected '-' or digit, offset: 0x00000005")
      checkError("\"P1DT-XH\"", "expected digit, offset: 0x00000006")
      checkError("\"P1DT1XH\"", "expected 'H' or 'M' or 'S or '.' or digit, offset: 0x00000006")
      checkError("\"P0DT2562047788015216H\"", "illegal duration, offset: 0x00000015")
      checkError("\"P0DT-2562047788015216H\"", "illegal duration, offset: 0x00000016")
      checkError("\"P0DT153722867280912931M\"", "illegal duration, offset: 0x00000017")
      checkError("\"P0DT-153722867280912931M\"", "illegal duration, offset: 0x00000018")
      checkError("\"P0DT9223372036854775808S\"", "illegal duration, offset: 0x00000018")
      checkError("\"P0DT92233720368547758000S\"", "illegal duration, offset: 0x00000018")
      checkError("\"P0DT-9223372036854775809S\"", "illegal duration, offset: 0x00000018")
      checkError("\"P1DT1HXM\"", "expected '\"' or '-' or digit, offset: 0x00000007")
      checkError("\"P1DT1H-XM\"", "expected digit, offset: 0x00000008")
      checkError("\"P1DT1H1XM\"", "expected 'M' or 'S or '.' or digit, offset: 0x00000008")
      checkError("\"P0DT0H153722867280912931M\"", "illegal duration, offset: 0x00000019")
      checkError("\"P0DT0H-153722867280912931M\"", "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H9223372036854775808S\"", "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H92233720368547758000S\"", "illegal duration, offset: 0x0000001a")
      checkError("\"P0DT0H-9223372036854775809S\"", "illegal duration, offset: 0x0000001a")
      checkError("\"P1DT1H1MXS\"", "expected '\"' or '-' or digit, offset: 0x00000009")
      checkError("\"P1DT1H1M-XS\"", "expected digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0XS\"", "expected 'S or '.' or digit, offset: 0x0000000a")
      checkError("\"P1DT1H1M0.XS\"", "expected 'S' or digit, offset: 0x0000000b")
      checkError("\"P1DT1H1M0.012345678XS\"", "expected 'S', offset: 0x00000014")
      checkError("\"P1DT1H1M0.0123456789S\"", "expected 'S', offset: 0x00000014")
      checkError("\"P0DT0H0M9223372036854775808S\"", "illegal duration, offset: 0x0000001c")
      checkError("\"P0DT0H0M92233720368547758080S\"", "illegal duration, offset: 0x0000001c")
      checkError("\"P0DT0H0M-9223372036854775809S\"", "illegal duration, offset: 0x0000001c")
      checkError("\"P106751991167300DT24H\"", "illegal duration, offset: 0x00000015")
      checkError("\"P0DT2562047788015215H60M\"", "illegal duration, offset: 0x00000018")
      checkError("\"P0DT0H153722867280912930M60S\"", "illegal duration, offset: 0x0000001c")
    }
  }
  "JsonReader.readInstant and JsonReader.readKeyAsInstant" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readInstant(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsInstant())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Instant.parse("2008-01-20T07:24:33Z")
      reader("null").readInstant(default) shouldBe default
    }
    "parse Instant from a string representation according to ISO-8601 format" in {
      def check(x: Instant): Unit = {
        val s = x.toString
        reader(s""""$s"""").readInstant(null) shouldBe x
        reader(s""""$s":""").readKeyAsInstant() shouldBe x
      }

      check(Instant.MAX)
      check(Instant.MIN)
      forAll(genInstant, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken Instant string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readInstant(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsInstant()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z", "unexpected end of input, offset: 0x00000015")
      checkError("\"+1000000000=01-20T07:24:33Z\"", "expected '-', offset: 0x0000000c")
      checkError("\"X008-01-20T07:24:33Z\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01-20T07:24:33Z\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01-20T07:24:33Z\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01-20T07:24:33Z\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"", "expected '-', offset: 0x00000005")
      checkError("\"2008-X0-20T07:24:33Z\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"", "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"", "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"", "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"", "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"", "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"", "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"", "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"", "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"", "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"", "expected ':' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"", "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"", "expected '.' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ", "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"", "expected 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"", "expected 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X\"", "expected 'Z', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33Z\"", "illegal year, offset: 0x00000016")
      checkError("\"+X0000-01-20T07:24:33Z\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01-20T07:24:33Z\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01-20T07:24:33Z\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01-20T07:24:33Z\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01-20T07:24:33Z\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+100000000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x0000000b")
      checkError("\"+1000000001-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"+4000000000-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"+9999999999-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"-1000000001-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"-4000000000-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"-9999999999-01-20T07:24:33Z\"", "illegal year, offset: 0x0000001c")
      checkError("\"2008-00-20T07:24:33Z\"", "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"", "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2007-02-29T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-01-20T24:24:33Z\"", "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"", "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"", "illegal second, offset: 0x00000015")
    }
  }
  "JsonReader.readLocalDate and JsonReader.readKeyAsLocalDate" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalDate(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalDate())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDate.parse("2008-01-20")
      reader("null").readLocalDate(default) shouldBe default
    }
    "parse LocalDate from a string representation according to ISO-8601 format" in {
      def check(x: LocalDate): Unit = {
        val s = x.toString
        reader(s""""$s"""").readLocalDate(null) shouldBe x
        reader(s""""$s":""").readKeyAsLocalDate() shouldBe x
      }

      check(LocalDate.MAX)
      check(LocalDate.MIN)
      forAll(genLocalDate, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDate string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalDate(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalDate()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01-20", "unexpected end of input, offset: 0x0000000b")
      checkError("\"X008-01-20\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01-20\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01-20\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01-20\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20\"", "expected '-', offset: 0x00000005")
      checkError("\"+X0000-01-20\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01-20\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01-20\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01-20\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01-20\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01-20\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01-20\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01-20\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01-20\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01-20\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+999999999=01-20\"", "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20\"", "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20\"", "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20\"", "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0\"", "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2X\"", "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X\"", "expected '\"', offset: 0x0000000b")
      checkError("\"-0000-01-20\"", "illegal year, offset: 0x0000000c")
      checkError("\"2008-00-20\"", "illegal month, offset: 0x0000000b")
      checkError("\"2008-13-20\"", "illegal month, offset: 0x0000000b")
      checkError("\"2008-01-00\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-01-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2007-02-29\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-02-30\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-03-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-04-31\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-05-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-06-31\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-07-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-08-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-09-31\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-10-32\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-11-31\"", "illegal day, offset: 0x0000000b")
      checkError("\"2008-12-32\"", "illegal day, offset: 0x0000000b")
    }
  }
  "JsonReader.readLocalDateTime and JsonReader.readKeyAsLocalDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalDateTime.parse("2008-01-20T07:24:33")
      reader("null").readLocalDateTime(default) shouldBe default
    }
    "parse LocalDateTime from a string representation according to ISO-8601 format" in {
      def check(x: LocalDateTime): Unit = {
        val s = x.toString
        reader(s""""$s"""").readLocalDateTime(null) shouldBe x
        reader(s""""$s":""").readKeyAsLocalDateTime() shouldBe x
      }

      check(LocalDateTime.MAX)
      check(LocalDateTime.MIN)
      forAll(genLocalDateTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalDateTime()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33", "unexpected end of input, offset: 0x00000014")
      checkError("\"X008-01-20T07:24:33\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01-20T07:24:33\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01-20T07:24:33\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01-20T07:24:33\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33\"", "expected '-', offset: 0x00000005")
      checkError("\"+X0000-01-20T07:24:33\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01-20T07:24:33\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01-20T07:24:33\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01-20T07:24:33\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01-20T07:24:33\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01-20T07:24:33\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01-20T07:24:33\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01-20T07:24:33\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01-20T07:24:33\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01-20T07:24:33\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+999999999=01-20T07:24:33\"", "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33\"", "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33\"", "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33\"", "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33\"", "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33\"", "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33\"", "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33\"", "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33\"", "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33\"", "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33\"", "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33\"", "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33\"", "expected ':' or '\"', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3\"", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3X\"", "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"", "expected '.' or '\"', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33.X\"", "expected '\"' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.123456789X\"", "expected '\"', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33\"", "illegal year, offset: 0x00000015")
      checkError("\"2008-00-20T07:24:33\"", "illegal month, offset: 0x00000014")
      checkError("\"2008-13-20T07:24:33\"", "illegal month, offset: 0x00000014")
      checkError("\"2008-01-00T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-01-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2007-02-29T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-02-30T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-03-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-04-31T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-05-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-06-31T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-07-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-08-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-09-31T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-10-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-11-31T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-12-32T07:24:33\"", "illegal day, offset: 0x00000014")
      checkError("\"2008-01-20T24:24:33\"", "illegal hour, offset: 0x00000014")
      checkError("\"2008-01-20T07:60:33\"", "illegal minute, offset: 0x00000014")
      checkError("\"2008-01-20T07:24:60\"", "illegal second, offset: 0x00000014")
    }
  }
  "JsonReader.readLocalTime and JsonReader.readKeyAsLocalTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readLocalTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsLocalTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = LocalTime.parse("07:24:33")
      reader("null").readLocalTime(default) shouldBe default
    }
    "parse LocalTime from a string representation according to ISO-8601 format" in {
      def check(x: LocalTime): Unit = {
        val s = x.toString
        reader(s""""$s"""").readLocalTime(null) shouldBe x
        reader(s""""$s":""").readKeyAsLocalTime() shouldBe x
      }

      check(LocalTime.MAX)
      check(LocalTime.MIN)
      forAll(genLocalTime, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readLocalTime(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsLocalTime()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"07:24:33", "unexpected end of input, offset: 0x00000009")
      checkError("\"X7:24:33\"", "expected digit, offset: 0x00000001")
      checkError("\"0X:24:33\"", "expected digit, offset: 0x00000002")
      checkError("\"07=24:33\"", "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33\"", "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33\"", "expected digit, offset: 0x00000005")
      checkError("\"07:24=33\"", "expected ':' or '\"', offset: 0x00000006")
      checkError("\"07:24:X3\"", "expected digit, offset: 0x00000007")
      checkError("\"07:24:3X\"", "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"", "expected '.' or '\"', offset: 0x00000009")
      checkError("\"07:24:33.X\"", "expected '\"' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"", "expected '\"', offset: 0x00000013")
      checkError("\"24:24:33\"", "illegal hour, offset: 0x00000009")
      checkError("\"07:60:33\"", "illegal minute, offset: 0x00000009")
      checkError("\"07:24:60\"", "illegal second, offset: 0x00000009")
    }
  }
  "JsonReader.readMonthDay and JsonReader.readKeyAsMonthDay" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readMonthDay(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsMonthDay())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = MonthDay.parse("--01-20")
      reader("null").readMonthDay(default) shouldBe default
    }
    "parse MonthDay from a string representation according to ISO-8601 format" in {
      def check(x: MonthDay): Unit = {
        val s = x.toString
        reader(s""""$s"""").readMonthDay(null) shouldBe x
        reader(s""""$s":""").readKeyAsMonthDay() shouldBe x
      }

      check(MonthDay.of(12, 31))
      check(MonthDay.of(1, 1))
      forAll(genMonthDay, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken LocalDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readMonthDay(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsMonthDay()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"=-01-20\"", "expected '-', offset: 0x00000001")
      checkError("\"-=01-20\"", "expected '-', offset: 0x00000002")
      checkError("\"--X1-20\"", "expected digit, offset: 0x00000003")
      checkError("\"--0X-20\"", "expected digit, offset: 0x00000004")
      checkError("\"--01=20\"", "expected '-', offset: 0x00000005")
      checkError("\"--01-X0\"", "expected digit, offset: 0x00000006")
      checkError("\"--01-2X\"", "expected digit, offset: 0x00000007")
      checkError("\"--01-20X\"", "expected '\"', offset: 0x00000008")
      checkError("\"--00-20\"", "illegal month, offset: 0x00000008")
      checkError("\"--13-20\"", "illegal month, offset: 0x00000008")
      checkError("\"--01-00\"", "illegal day, offset: 0x00000008")
      checkError("\"--01-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--02-30\"", "illegal day, offset: 0x00000008")
      checkError("\"--03-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--04-31\"", "illegal day, offset: 0x00000008")
      checkError("\"--05-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--06-31\"", "illegal day, offset: 0x00000008")
      checkError("\"--07-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--08-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--09-31\"", "illegal day, offset: 0x00000008")
      checkError("\"--10-32\"", "illegal day, offset: 0x00000008")
      checkError("\"--11-31\"", "illegal day, offset: 0x00000008")
      checkError("\"--12-32\"", "illegal day, offset: 0x00000008")
    }
  }
  "JsonReader.readOffsetDateTime and JsonReader.readKeyAsOffsetDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readOffsetDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsOffsetDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetDateTime.parse("2008-01-20T07:24Z")
      reader("null").readOffsetDateTime(default) shouldBe default
    }
    "parse OffsetDateTime from a string representation according to ISO-8601 format" in {
      def check(s: String, x: OffsetDateTime): Unit = {
        reader(s""""$s"""").readOffsetDateTime(null) shouldBe x
        reader(s""""$s":""").readKeyAsOffsetDateTime() shouldBe x
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
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readOffsetDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsOffsetDateTime()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z", "unexpected end of input, offset: 0x00000015")
      checkError("\"X008-01-20T07:24:33Z\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01-20T07:24:33Z\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01-20T07:24:33Z\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01-20T07:24:33Z\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z\"", "expected '-', offset: 0x00000005")
      checkError("\"+X0000-01-20T07:24:33Z\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01-20T07:24:33Z\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01-20T07:24:33Z\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01-20T07:24:33Z\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01-20T07:24:33Z\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01-20T07:24:33Z\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+999999999=01-20T07:24:33Z\"", "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33Z\"", "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z\"", "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33Z\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z\"", "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z\"", "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z\"", "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z\"", "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z\"", "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z\"", "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z\"", "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z\"", "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z\"", "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z\"", "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z\"", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ\"", "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X\"", "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ", "expected '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.\"", "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000\"", "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"-0000-01-20T07:24:33Z\"", "illegal year, offset: 0x00000016")
      checkError("\"2008-00-20T07:24:33Z\"", "illegal month, offset: 0x00000015")
      checkError("\"2008-13-20T07:24:33Z\"", "illegal month, offset: 0x00000015")
      checkError("\"2008-01-00T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-01-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2007-02-29T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-02-30T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-03-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-04-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-05-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-06-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-07-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-08-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-09-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-10-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-11-31T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33Z\"", "illegal day, offset: 0x00000015")
      checkError("\"2008-12-32T07:24:33.123456789X\"", "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"2008-01-20T24:24:33Z\"", "illegal hour, offset: 0x00000015")
      checkError("\"2008-01-20T07:60:33Z\"", "illegal minute, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:60Z\"", "illegal second, offset: 0x00000015")
      checkError("\"2008-01-20T07:24+\"  ", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24-\"  ", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:33+\"  ", "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-\"  ", "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+\" ", "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1\"", "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=\"", "expected ':' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:\" ", "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1\"", "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10=10\"", "expected ':' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0\"", "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X\"", "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+18:10\"", "illegal timezone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.-18:10\"", "illegal timezone offset, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+20:10\"", "illegal timezone offset hour, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:90\"", "illegal timezone offset minute, offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:60\"", "illegal timezone offset second, offset: 0x0000001e")
    }
  }
  "JsonReader.readOffsetTime and JsonReader.readKeyAsOffsetTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readOffsetTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsOffsetTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = OffsetTime.parse("07:24:33+01:00")
      reader("null").readOffsetTime(default) shouldBe default
    }
    "parse OffsetTime from a string representation according to ISO-8601 format" in {
      def check(s: String, x: OffsetTime): Unit = {
        reader(s""""$s"""").readOffsetTime(null) shouldBe x
        reader(s""""$s":""").readKeyAsOffsetTime() shouldBe x
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
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readOffsetTime(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsOffsetTime()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"07:24:33Z", "unexpected end of input, offset: 0x0000000a")
      checkError("\"X7:24:33Z\"", "expected digit, offset: 0x00000001")
      checkError("\"0X:24:33Z\"", "expected digit, offset: 0x00000002")
      checkError("\"07=24:33Z\"", "expected ':', offset: 0x00000003")
      checkError("\"07:X4:33Z\"", "expected digit, offset: 0x00000004")
      checkError("\"07:2X:33Z\"", "expected digit, offset: 0x00000005")
      checkError("\"07:24=33Z\"", "expected ':' or '+' or '-' or 'Z', offset: 0x00000006")
      checkError("\"07:24:X3Z\"", "expected digit, offset: 0x00000007")
      checkError("\"07:24:3XZ\"", "expected digit, offset: 0x00000008")
      checkError("\"07:24:33X\"", "expected '.' or '+' or '-' or 'Z', offset: 0x00000009")
      checkError("\"07:24:33.\"", "expected '+' or '-' or 'Z' or digit, offset: 0x0000000a")
      checkError("\"07:24:33.123456789X\"", "expected '+' or '-' or 'Z', offset: 0x00000013")
      checkError("\"24:24:33Z\"", "illegal hour, offset: 0x0000000a")
      checkError("\"07:60:33Z\"", "illegal minute, offset: 0x0000000a")
      checkError("\"07:24:60Z\"", "illegal second, offset: 0x0000000a")
      checkError("\"07:24+\"  ", "expected digit, offset: 0x00000007")
      checkError("\"07:24-\"  ", "expected digit, offset: 0x00000007")
      checkError("\"07:24:33+\"  ", "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33-\"  ", "expected digit, offset: 0x0000000a")
      checkError("\"07:24:33.+\"  ", "expected digit, offset: 0x0000000b")
      checkError("\"07:24:33.+1\" ", "expected digit, offset: 0x0000000c")
      checkError("\"07:24:33.+10=\"", "expected ':' or '\"', offset: 0x0000000d")
      checkError("\"07:24:33.+10:\" ", "expected digit, offset: 0x0000000e")
      checkError("\"07:24:33.+10:1\"", "expected digit, offset: 0x0000000f")
      checkError("\"07:24:33.+10:10=10\"", "expected ':' or '\"', offset: 0x00000010")
      checkError("\"07:24:33.+10:10:X0\"", "expected digit, offset: 0x00000011")
      checkError("\"07:24:33.+10:10:1X\"", "expected digit, offset: 0x00000012")
      checkError("\"07:24:33.+10:10:10X\"", "expected '\"', offset: 0x00000013")
      checkError("\"07:24:33.+18:10\"", "illegal timezone offset, offset: 0x00000010")
      checkError("\"07:24:33.-18:10\"", "illegal timezone offset, offset: 0x00000010")
      checkError("\"07:24:33.+20:10\"", "illegal timezone offset hour, offset: 0x00000010")
      checkError("\"07:24:33.+10:90\"", "illegal timezone offset minute, offset: 0x00000010")
      checkError("\"07:24:33.+10:10:60\"", "illegal timezone offset second, offset: 0x00000013")
    }
  }
  "JsonReader.readPeriod and JsonReader.readKeyAsPeriod" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readPeriod(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsPeriod())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Period.parse("P1Y2M3D")
      reader("null").readPeriod(default) shouldBe default
    }
    "parse Period from a string representation according to JDK 8+ format that is based on ISO-8601 format" in {
      def check(s: String, x: Period): Unit = {
        reader(s""""$s"""").readPeriod(null) shouldBe x
        reader(s""""$s":""").readKeyAsPeriod() shouldBe x
        if (x.getYears != Int.MinValue && x.getMonths != Int.MinValue && x.getDays != Int.MinValue) {
          reader(s""""-$s"""").readPeriod(null) shouldBe x.negated()
          reader(s""""-$s":""").readKeyAsPeriod() shouldBe x.negated()
        }
      }

      check("P0D", Period.ZERO)
      forAll(genPeriod, minSuccessful(100000))(x => check(x.toString, x))
      forAll(minSuccessful(100000)) { (x: Int, y: Int) =>
        check(s"P${x}Y", Period.of(x, 0, 0))
        check(s"P${x}M", Period.of(0, x, 0))
        check(s"P${x}D", Period.of(0, 0, x))
        check(s"P${x}Y${y}M", Period.of(x, y, 0))
        check(s"P${x}M${y}D", Period.of(0, x, y))
        check(s"P${x}Y${y}D", Period.of(x, 0, y))
      }
      forAll(Gen.choose(-1000000, 1000000), minSuccessful(100000)) { (w: Int) =>
        check(s"P${w}W", Period.of(0, 0, w * 7))
        check(s"P1Y${w}W", Period.of(1, 0, w * 7))
        check(s"P1Y1M${w}W", Period.of(1, 1, w * 7))
      }
      forAll(Gen.choose(-1000000, 1000000), Gen.choose(-1000000, 1000000), minSuccessful(100000)) { (w: Int, d: Int) =>
        check(s"P${w}W${d}D", Period.of(0, 0, w * 7 + d))
        check(s"P1Y${w}W${d}D", Period.of(1, 0, w * 7 + d))
        check(s"P1Y1M${w}W${d}D", Period.of(1, 1, w * 7 + d))
      }
    }
    "throw parsing exception for empty input and illegal or broken Period string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readPeriod(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsPeriod()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\"", "expected 'P' or '-', offset: 0x00000001")
      checkError("\"-\"", "expected 'P', offset: 0x00000002")
      checkError("\"PXY\"", "expected '-' or digit, offset: 0x00000002")
      checkError("\"P-XY\"", "expected digit, offset: 0x00000003")
      checkError("\"P1XY\"", "expected 'Y' or 'M' or 'W' or 'D' or digit, offset: 0x00000003")
      checkError("\"P2147483648Y\"", "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470Y\"", "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649Y\"", "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648M\"", "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470M\"", "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649M\"", "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648W\"", "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470W\"", "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649W\"", "illegal period, offset: 0x0000000c")
      checkError("\"P2147483648D\"", "illegal period, offset: 0x0000000c")
      checkError("\"P21474836470D\"", "illegal period, offset: 0x0000000c")
      checkError("\"P-2147483649D\"", "illegal period, offset: 0x0000000c")
      checkError("\"P1YXM\"", "expected '\"' or '-' or digit, offset: 0x00000004")
      checkError("\"P1Y-XM\"", "expected digit, offset: 0x00000005")
      checkError("\"P1Y1XM\"", "expected 'M' or 'W' or 'D' or digit, offset: 0x00000005")
      checkError("\"P1Y2147483648M\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470M\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649M\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y2147483648W\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470W\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649W\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y2147483648D\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y21474836470D\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y-2147483649D\"", "illegal period, offset: 0x0000000e")
      checkError("\"P1Y1MXW\"", "expected '\"' or '-' or digit, offset: 0x00000006")
      checkError("\"P1Y1M-XW\"", "expected digit, offset: 0x00000007")
      checkError("\"P1Y1M1XW\"", "expected 'W' or 'D' or digit, offset: 0x00000007")
      checkError("\"P1Y1M306783379W\"", "illegal period, offset: 0x0000000f")
      checkError("\"P1Y1M3067833790W\"", "illegal period, offset: 0x0000000f")
      checkError("\"P1Y1M-306783379W\"", "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M2147483648D\"", "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M21474836470D\"", "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M-2147483649D\"", "illegal period, offset: 0x00000010")
      checkError("\"P1Y1M1WXD\"", "expected '\"' or '-' or digit, offset: 0x00000008")
      checkError("\"P1Y1M1W-XD\"", "expected digit, offset: 0x00000009")
      checkError("\"P1Y1M1W1XD\"", "expected 'D' or digit, offset: 0x00000009")
      checkError("\"P1Y1M306783378W8D\"", "illegal period, offset: 0x00000011")
      checkError("\"P1Y1M-306783378W-8D\"", "illegal period, offset: 0x00000013")
      checkError("\"P1Y1M1W2147483647D\"", "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M-1W-2147483648D\"", "illegal period, offset: 0x00000014")
      checkError("\"P1Y1M0W2147483648D\"", "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M0W21474836470D\"", "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M0W-2147483649D\"", "illegal period, offset: 0x00000012")
      checkError("\"P1Y1M1W1DX", "expected '\"', offset: 0x0000000a")
    }
  }
  "JsonReader.readYear and JsonReader.readKeyAsYear" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readYear(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsYear())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = Year.parse("2008")
      reader("null").readYear(default) shouldBe default
    }
    "parse Year from a string representation according to ISO-8601 format" in {
      val yearFormatter = DateTimeFormatter.ofPattern("uuuu")

      def check(s: String, x: Year): Unit = {
        reader(s""""$s"""").readYear(null) shouldBe x
        reader(s""""$s":""").readKeyAsYear() shouldBe x
      }

      check("-999999999", Year.of(Year.MIN_VALUE))
      check("+999999999", Year.of(Year.MAX_VALUE))
      forAll(genYear, minSuccessful(100000)) { (x: Year) =>
        check(x.format(yearFormatter), x)
      }
    }
    "throw parsing exception for empty input and illegal or broken Year string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readYear(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsYear()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008", "unexpected end of input, offset: 0x00000005")
      checkError("\"X008\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8\"", "expected digit, offset: 0x00000003")
      checkError("\"200X\"", "expected digit, offset: 0x00000004")
      checkError("\"+X0000\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X\"", "expected '\"' or digit, offset: 0x00000006")
      checkError("\"+10000X\"", "expected '\"' or digit, offset: 0x00000007")
      checkError("\"+100000X\"", "expected '\"' or digit, offset: 0x00000008")
      checkError("\"+1000000X\"", "expected '\"' or digit, offset: 0x00000009")
      checkError("\"+10000000X\"", "expected '\"' or digit, offset: 0x0000000a")
      checkError("\"+1000000000\"", "expected '\"', offset: 0x0000000b")
      checkError("\"-1000000000\"", "expected '\"', offset: 0x0000000b")
      checkError("\"-0000\"", "illegal year, offset: 0x00000006")
    }
  }
  "JsonReader.readYearMonth and JsonReader.readKeyAsYearMonth" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readYearMonth(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsYearMonth())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = YearMonth.parse("2008-01")
      reader("null").readYearMonth(default) shouldBe default
    }
    "parse YearMonth from a string representation according to ISO-8601 format" in {
      def check(s: String, x: YearMonth): Unit = {
        reader(s""""$s"""").readYearMonth(null) shouldBe x
        reader(s""""$s":""").readKeyAsYearMonth() shouldBe x
      }

      check("+999999999-12", YearMonth.of(Year.MAX_VALUE, 12))
      check("-999999999-01", YearMonth.of(Year.MIN_VALUE, 1))
      forAll(genYearMonth, minSuccessful(100000)) { (x: YearMonth) =>
        val s = x.toString
        val fixed =
          if (x.getYear < 0 && !s.startsWith("-")) s"-$s"
          else if (x.getYear > 9999 && !s.startsWith("+")) s"+$s"
          else s
        check(fixed, x)
      }
    }
    "throw parsing exception for empty input and illegal or broken YearMonth string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readYearMonth(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsYearMonth()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01", "unexpected end of input, offset: 0x00000008")
      checkError("\"X008-01\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01\"", "expected '-', offset: 0x00000005")
      checkError("\"+X0000-01\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+999999999=01\"", "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01\"", "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01\"", "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01X\"", "expected '\"', offset: 0x00000008")
      checkError("\"-0000-01\"", "illegal year, offset: 0x00000009")
      checkError("\"2008-00\"", "illegal month, offset: 0x00000008")
      checkError("\"2008-13\"", "illegal month, offset: 0x00000008")
    }
  }
  "JsonReader.readZonedDateTime and JsonReader.readKeyAsZonedDateTime" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZonedDateTime(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZonedDateTime())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZonedDateTime.parse("2008-01-20T07:24Z[UTC]")
      reader("null").readZonedDateTime(default) shouldBe default
    }
    "parse ZonedDateTime from a string representation according to ISO-8601 format with optional IANA timezone identifier in JDK 8+ format" in {
      def check(s: String, x: ZonedDateTime): Unit = {
        reader(s""""$s"""").readZonedDateTime(null) shouldBe x
        reader(s""""$s":""").readKeyAsZonedDateTime() shouldBe x
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
      check("2018-03-25T02:30+01:00[Europe/Warsaw]", ZonedDateTime.parse("2018-03-25T02:30+01:00[Europe/Warsaw]"))
      if (!isJDK8) {
        //FIXME: Bug in JDK 8 at ZonedDateTime.parse, see https://bugs.openjdk.java.net/browse/JDK-8066982
        check("2018-03-25T02:30+00:00[Europe/Warsaw]", ZonedDateTime.parse("2018-03-25T02:30+00:00[Europe/Warsaw]"))
        check("2018-03-25T02:30+02:00[Europe/Warsaw]", ZonedDateTime.parse("2018-03-25T02:30+02:00[Europe/Warsaw]"))
        check("2018-03-25T02:30+03:00[Europe/Warsaw]", ZonedDateTime.parse("2018-03-25T02:30+03:00[Europe/Warsaw]"))
        check("2018-10-28T02:30+00:00[Europe/Warsaw]", ZonedDateTime.parse("2018-10-28T02:30+00:00[Europe/Warsaw]"))
        check("2018-10-28T02:30+01:00[Europe/Warsaw]", ZonedDateTime.parse("2018-10-28T02:30+01:00[Europe/Warsaw]"))
        check("2018-10-28T02:30+02:00[Europe/Warsaw]", ZonedDateTime.parse("2018-10-28T02:30+02:00[Europe/Warsaw]"))
        check("2018-10-28T02:30+03:00[Europe/Warsaw]", ZonedDateTime.parse("2018-10-28T02:30+03:00[Europe/Warsaw]"))
      }
      forAll(genZonedDateTime, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken ZonedDateTime string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZonedDateTime(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZonedDateTime()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"2008-01-20T07:24:33Z[UTC]", "unexpected end of input, offset: 0x0000001a")
      checkError("\"X008-01-20T07:24:33Z[UTC]\"", "expected '-' or '+' or digit, offset: 0x00000001")
      checkError("\"2X08-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000002")
      checkError("\"20X8-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000003")
      checkError("\"200X-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000004")
      checkError("\"2008=01-20T07:24:33Z[UTC]\"", "expected '-', offset: 0x00000005")
      checkError("\"+X0000-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000002")
      checkError("\"+1X000-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000003")
      checkError("\"+10X00-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000004")
      checkError("\"+100X0-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000005")
      checkError("\"+1000X-01-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000006")
      checkError("\"-1000X-01-20T07:24:33Z[UTC]\"", "expected '-' or digit, offset: 0x00000006")
      checkError("\"+10000X-01-20T07:24:33Z[UTC]\"", "expected '-' or digit, offset: 0x00000007")
      checkError("\"+100000X-01-20T07:24:33Z[UTC]\"", "expected '-' or digit, offset: 0x00000008")
      checkError("\"+1000000X-01-20T07:24:33Z[UTC]\"", "expected '-' or digit, offset: 0x00000009")
      checkError("\"+10000000X-01-20T07:24:33Z[UTC]\"", "expected '-' or digit, offset: 0x0000000a")
      checkError("\"+999999999=01-20T07:24:33Z[UTC]\"", "expected '-', offset: 0x0000000b")
      checkError("\"+1000000000-01-20T07:24:33Z[UTC]\"", "expected '-', offset: 0x0000000b")
      checkError("\"-1000000000-01-20T07:24:33Z[UTC]\"", "expected '-', offset: 0x0000000b")
      checkError("\"2008-X1-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000006")
      checkError("\"2008-0X-20T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000007")
      checkError("\"2008-01=20T07:24:33Z[UTC]\"", "expected '-', offset: 0x00000008")
      checkError("\"2008-01-X0T07:24:33Z[UTC]\"", "expected digit, offset: 0x00000009")
      checkError("\"2008-01-2XT07:24:33Z[UTC]\"", "expected digit, offset: 0x0000000a")
      checkError("\"2008-01-20X07:24:33Z[UTC]\"", "expected 'T', offset: 0x0000000b")
      checkError("\"2008-01-20TX7:24:33Z[UTC]\"", "expected digit, offset: 0x0000000c")
      checkError("\"2008-01-20T0X:24:33Z[UTC]\"", "expected digit, offset: 0x0000000d")
      checkError("\"2008-01-20T07=24:33Z[UTC]\"", "expected ':', offset: 0x0000000e")
      checkError("\"2008-01-20T07:X4:33Z[UTC]\"", "expected digit, offset: 0x0000000f")
      checkError("\"2008-01-20T07:2X:33Z[UTC]\"", "expected digit, offset: 0x00000010")
      checkError("\"2008-01-20T07:24=33Z[UTC]\"", "expected ':' or '+' or '-' or 'Z', offset: 0x00000011")
      checkError("\"2008-01-20T07:24:X3Z[UTC]\"", "expected digit, offset: 0x00000012")
      checkError("\"2008-01-20T07:24:3XZ[UTC]\"", "expected digit, offset: 0x00000013")
      checkError("\"2008-01-20T07:24:33X[UTC]\"", "expected '.' or '+' or '-' or 'Z', offset: 0x00000014")
      checkError("\"2008-01-20T07:24:33ZZ", "expected '[' or '\"', offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.[UTC]\"", "expected '+' or '-' or 'Z' or digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.000[UTC]\"", "expected '+' or '-' or 'Z' or digit, offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.123456789X[UTC]\"", "expected '+' or '-' or 'Z', offset: 0x0000001e")
      checkError("\"-0000-01-20T07:24:33Z[UTC]\"", "illegal year, offset: 0x0000001b")
      checkError("\"2008-00-20T07:24:33Z[UTC]\"", "illegal month, offset: 0x0000001a")
      checkError("\"2008-13-20T07:24:33Z[UTC]\"", "illegal month, offset: 0x0000001a")
      checkError("\"2008-01-00T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2007-02-29T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-02-30T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-03-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-04-31T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-05-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-06-31T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-07-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-08-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-09-31T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-10-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-11-31T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-12-32T07:24:33Z[UTC]\"", "illegal day, offset: 0x0000001a")
      checkError("\"2008-01-20T24:24:33Z[UTC]\"", "illegal hour, offset: 0x0000001a")
      checkError("\"2008-01-20T07:60:33Z[UTC]\"", "illegal minute, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:60Z[UTC]\"", "illegal second, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33+[UTC]\"", "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33-[UTC]\"", "expected digit, offset: 0x00000015")
      checkError("\"2008-01-20T07:24:33.+[UTC]\"", "expected digit, offset: 0x00000016")
      checkError("\"2008-01-20T07:24:33.+1[UTC]\"", "expected digit, offset: 0x00000017")
      checkError("\"2008-01-20T07:24:33.+10=[UTC]\"", "expected ':' or '[' or '\"', offset: 0x00000018")
      checkError("\"2008-01-20T07:24:33.+10:[UTC]\"", "expected digit, offset: 0x00000019")
      checkError("\"2008-01-20T07:24:33.+10:1[UTC]\"", "expected digit, offset: 0x0000001a")
      checkError("\"2008-01-20T07:24:33.+10:10[]\"", "illegal timezone, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10=10[UTC]\"", "expected ':' or '[' or '\"', offset: 0x0000001b")
      checkError("\"2008-01-20T07:24:33.+10:10:X0[UTC]\"", "expected digit, offset: 0x0000001c")
      checkError("\"2008-01-20T07:24:33.+10:10:1X[UTC]\"", "expected digit, offset: 0x0000001d")
      checkError("\"2008-01-20T07:24:33.+10:10:10[UTC]X\"", "expected '\"', offset: 0x00000023")
      checkError("\"2008-01-20T07:24:33.+18:10[UTC]\"", "illegal timezone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.-18:10[UTC]\"", "illegal timezone offset, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+20:10[UTC]\"", "illegal timezone offset hour, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:90[UTC]\"", "illegal timezone offset minute, offset: 0x00000020")
      checkError("\"2008-01-20T07:24:33.+10:10:60[UTC]\"", "illegal timezone offset second, offset: 0x00000023")
    }
  }
  "JsonReader.readZoneId and JsonReader.readKeyAsZoneId" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZoneId(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZoneId())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneId.of("Europe/Warsaw")
      reader("null").readZoneId(default) shouldBe default
    }
    "parse ZoneId from a string representation according to ISO-8601 format for timezone offset or JDK 8+ format for IANA timezone identifier" in {
      def check(x: ZoneId): Unit = {
        val s = x.toString
        reader(s""""$s"""").readZoneId(null) shouldBe x
        reader(s""""$s":""").readKeyAsZoneId() shouldBe x
      }

      forAll(genZoneId, minSuccessful(100000))(check)
    }
    "throw parsing exception for empty input and illegal or broken ZoneId string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZoneId(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZoneId()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\"", "illegal timezone, offset: 0x00000001")
      checkError("\"+\"", "illegal timezone, offset: 0x00000002")
      //checkError("\"+1\"", "expected digit, offset: 0x00000003") FIXME: looks like a bug in ZoneId.of() parser
      checkError("\"XXX\"", "illegal timezone, offset: 0x00000004")
      checkError("\"+10=\"", "illegal timezone, offset: 0x00000005")
      checkError("\"+10:\"", "illegal timezone, offset: 0x00000005")
      checkError("\"+10:1\"", "illegal timezone, offset: 0x00000006")
      checkError("\"+18:10\"", "illegal timezone, offset: 0x00000007")
      checkError("\"-18:10\"", "illegal timezone, offset: 0x00000007")
      checkError("\"+20:10\"", "illegal timezone, offset: 0x00000007")
      checkError("\"+10:90\"", "illegal timezone, offset: 0x00000007")
      checkError("\"+10:10:60\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"UT+\"", "illegal timezone, offset: 0x00000004")
      checkError("\"UT+10=\"", "illegal timezone, offset: 0x00000007")
      checkError("\"UT+10:\"", "illegal timezone, offset: 0x00000007")
      checkError("\"UT+10:1\"", "illegal timezone, offset: 0x00000008")
      checkError("\"UT+18:10\"", "illegal timezone, offset: 0x00000009")
      checkError("\"UT-18:10\"", "illegal timezone, offset: 0x00000009")
      checkError("\"UT+20:10\"", "illegal timezone, offset: 0x00000009")
      checkError("\"UT+10:90\"", "illegal timezone, offset: 0x00000009")
      checkError("\"UT+10:10:60\"", "illegal timezone, offset: 0x0000000c")
      checkError("\"UTC+\"", "illegal timezone, offset: 0x00000005")
      checkError("\"UTC+10=\"", "illegal timezone, offset: 0x00000008")
      checkError("\"UTC+10:\"", "illegal timezone, offset: 0x00000008")
      checkError("\"UTC+10:1\"", "illegal timezone, offset: 0x00000009")
      checkError("\"UTC+18:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"UTC-18:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"UTC+20:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"UTC+10:90\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"UTC+10:10:60\"", "illegal timezone, offset: 0x0000000d")
      checkError("\"GMT+\"", "illegal timezone, offset: 0x00000005")
      checkError("\"GMT+10=\"", "illegal timezone, offset: 0x00000008")
      checkError("\"GMT+10:\"", "illegal timezone, offset: 0x00000008")
      checkError("\"GMT+10:1\"", "illegal timezone, offset: 0x00000009")
      checkError("\"GMT+18:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"GMT-18:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"GMT+20:10\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"GMT+10:90\"", "illegal timezone, offset: 0x0000000a")
      checkError("\"GMT+10:10:60\"", "illegal timezone, offset: 0x0000000d")
    }
  }
  "JsonReader.readZoneOffset and JsonReader.readKeyAsZoneOffset" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readZoneOffset(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsZoneOffset())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      val default = ZoneOffset.of("+01:00")
      reader("null").readZoneOffset(default) shouldBe default
    }
    "parse ZoneOffset from a string representation according to ISO-8601 format" in {
      def check(s: String, x: ZoneOffset): Unit = {
        reader(s""""$s"""").readZoneOffset(null) shouldBe x
        reader(s""""$s":""").readKeyAsZoneOffset() shouldBe x
      }

      check("Z", ZoneOffset.UTC)
      check("+00", ZoneOffset.UTC)
      check("+00:00", ZoneOffset.UTC)
      check("-00", ZoneOffset.UTC)
      check("-00:00", ZoneOffset.UTC)
      check("+18", ZoneOffset.MAX)
      check("+18:00", ZoneOffset.MAX)
      check("-18", ZoneOffset.MIN)
      check("-18:00", ZoneOffset.MIN)
      forAll(genZoneOffset, minSuccessful(100000))(x => check(x.toString, x))
    }
    "throw parsing exception for empty input and illegal or broken ZoneOffset string" in {
      def checkError(json: String, error: String): Unit = {
        assert(intercept[JsonReaderException](reader(json).readZoneOffset(null)).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader(json).readKeyAsZoneOffset()).getMessage.contains(error))
      }

      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\"", "expected '+' or '-' or 'Z', offset: 0x00000001")
      checkError("\"+\" ", "expected digit, offset: 0x00000002")
      checkError("\"+1\"", "expected digit, offset: 0x00000003")
      checkError("\"+10=\"", "expected ':' or '\"', offset: 0x00000004")
      checkError("\"+10:\" ", "expected digit, offset: 0x00000005")
      checkError("\"+10:1\"", "expected digit, offset: 0x00000006")
      checkError("\"+10:10=10\"", "expected ':' or '\"', offset: 0x00000007")
      checkError("\"+10:10:X0\"", "expected digit, offset: 0x00000008")
      checkError("\"+10:10:1X\"", "expected digit, offset: 0x00000009")
      checkError("\"+10:10:10X\"", "expected '\"', offset: 0x0000000a")
      checkError("\"+18:10\"", "illegal timezone offset, offset: 0x00000007")
      checkError("\"-18:10\"", "illegal timezone offset, offset: 0x00000007")
      checkError("\"+20:10\"", "illegal timezone offset hour, offset: 0x00000007")
      checkError("\"+10:90\"", "illegal timezone offset minute, offset: 0x00000007")
      checkError("\"+10:10:60\"", "illegal timezone offset second, offset: 0x0000000a")
    }
  }
  "JsonReader.isCharBufEqualsTo" should {
    "return true if content of internal char buffer for the specified length is equal to the provided string" in {
      def check(s1: String, s2: String): Unit = {
        val r = reader(s""""$s1"""")
        r.isCharBufEqualsTo(r.readStringAsCharBuf(), s2) shouldBe s1 == s2
      }

      check("", "")
      check("x", "")
      check("", "x")
      check("x", "x")
      forAll(minSuccessful(100000)) { (s1: String, s2: String) =>
        whenever(s1.forall(ch => ch >= 32 && ch != '"' && ch != '\\' && !Character.isSurrogate(ch))) {
          check(s1, s2)
        }
      }
    }
    "throw exception for null value of string to compare" in {
      val r = reader("\"\"")
      intercept[NullPointerException](r.isCharBufEqualsTo(r.readStringAsCharBuf(), null))
    }
  }
  "JsonReader.readKeyAsString" should {
    "throw parsing exception for missing ':' in the end" in {
      assert(intercept[JsonReaderException](reader("\"\"").readKeyAsString())
        .getMessage.contains("unexpected end of input, offset: 0x00000002"))
      assert(intercept[JsonReaderException](reader("\"\"x").readKeyAsString())
        .getMessage.contains("expected ':', offset: 0x00000002"))
    }
  }
  "JsonReader.readString, JsonReader.readStringAsCharBuf and JsonReader.readKeyAsString" should {
    def check(s: String): Unit = {
      reader(s""""$s"""").readString(null) shouldBe s
      val r = reader(s""""$s"""")
      r.isCharBufEqualsTo(r.readStringAsCharBuf(), s) shouldBe true
      reader(s""""$s":""").readKeyAsString() shouldBe s
    }

    def checkError(json: String, error: String): Unit = {
      assert(intercept[JsonReaderException](reader(json).readString(null)).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader(json).readStringAsCharBuf()).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader(json).readKeyAsString()).getMessage.contains(error))
    }

    def checkError2(jsonBytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonReaderException](reader2(jsonBytes).readString(null)).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readStringAsCharBuf()).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readKeyAsString()).getMessage.contains(error))
    }

    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readString(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readStringAsCharBuf())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readKeyAsString())
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null").readString("VVV") shouldBe "VVV"
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
        reader(s""""$s1"""").readString(null) shouldBe s2
        val r = reader(s""""$s1"""")
        r.isCharBufEqualsTo(r.readStringAsCharBuf(), s2) shouldBe true
        reader(s""""$s1":""").readKeyAsString() shouldBe s2
      }

      checkEncoded("""\b\f\n\r\t\/\\""", "\b\f\n\r\t/\\")
      checkEncoded("\\u0008\\u000C\\u000a\\u000D\\u0009\\u002F\\u0041\\u0438\\u10d1\\ud834\\udd1e", "\b\f\n\r\t/Aиბ𝄞")
    }
    "throw parsing exception for control chars that must be escaped" in {
      forAll(genControlChar, minSuccessful(1000)) { (ch: Char) =>
        checkError(s""""${ch.toString}"""", "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\\", "unexpected end of input, offset: 0x00000002")
      checkError2(Array[Byte](0x22.toByte, 0xF0.toByte, 0x80.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000004")
    }
    "throw parsing exception for boolean values & numbers" in {
      checkError("true", "expected '\"', offset: 0x00000000")
      checkError("false", "expected '\"', offset: 0x00000000")
      checkError("12345", "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonReaderException](reader(s""""$s"""").readString(null)).getMessage.contains(error1))
        assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsCharBuf()).getMessage.contains(error1))
        assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsString()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readString(null))
          .getMessage.contains(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readStringAsCharBuf())
          .getMessage.contains(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte :+ ':'.toByte).readString(null))
          .getMessage.contains(error))
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
      assert(intercept[JsonReaderException](reader("\"x\"").readKeyAsChar())
        .getMessage.contains("unexpected end of input, offset: 0x00000003"))
      assert(intercept[JsonReaderException](reader("\"x\"x").readKeyAsChar())
        .getMessage.contains("expected ':', offset: 0x00000003"))
    }
  }
  "JsonReader.readChar and JsonReader.readKeyAsChar" should {
    def check(ch: Char): Unit = {
      reader(s""""${ch.toString}"""").readChar() shouldBe ch
      reader(s""""${ch.toString}":""").readKeyAsChar() shouldBe ch
    }

    def checkEscaped(escaped: String, nonEscaped: Char): Unit = {
      reader(s""""$escaped"""").readChar() shouldBe nonEscaped
      reader(s""""$escaped":""").readKeyAsChar() shouldBe nonEscaped
    }

    def checkError(json: String, error: String): Unit = {
      assert(intercept[JsonReaderException](reader(json).readChar()).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader(json).readKeyAsChar()).getMessage.contains(error))
    }

    def checkError2(jsonBytes: Array[Byte], error: String): Unit = {
      assert(intercept[JsonReaderException](reader2(jsonBytes).readChar()).getMessage.contains(error))
      assert(intercept[JsonReaderException](reader2(jsonBytes).readKeyAsChar()).getMessage.contains(error))
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
          checkError((s""""$ch$ch""""), "expected '\"'") // offset can differs for non-ASCII characters
        }
      }
    }
    "throw parsing exception for control chars that must be escaped" in {
      def checkError(bytes: Array[Byte], error: String): Unit = {
        assert(intercept[JsonReaderException](reader2(bytes).readChar()).getMessage.contains(error))
        assert(intercept[JsonReaderException](reader2(bytes).readKeyAsChar()).getMessage.contains(error))
      }

      forAll(genControlChar, minSuccessful(1000)) { (ch: Char) =>
        checkError(Array('"', ch.toByte, '"'), "unescaped control character, offset: 0x00000001")
      }
    }
    "throw parsing exception for empty input and illegal or broken string" in {
      checkError("", "unexpected end of input, offset: 0x00000000")
      checkError("\"", "unexpected end of input, offset: 0x00000001")
      checkError("\"\\", "unexpected end of input, offset: 0x00000002")
      checkError("\"\"", "illegal value for char, offset: 0x00000001")
      checkError2(Array[Byte](0x22.toByte, 0xC0.toByte), "unexpected end of input, offset: 0x00000002")
      checkError2(Array[Byte](0x22.toByte, 0xE0.toByte, 0x80.toByte), "unexpected end of input, offset: 0x00000003")
    }
    "throw parsing exception for null, boolean values & numbers" in {
      checkError("null", "expected '\"', offset: 0x00000000")
      checkError("true", "expected '\"', offset: 0x00000000")
      checkError("false", "expected '\"', offset: 0x00000000")
      checkError("12345", "expected '\"', offset: 0x00000000")
    }
    "throw parsing exception in case of illegal escape sequence" in {
      def checkError(s: String, error1: String, error2: String): Unit = {
        assert(intercept[JsonReaderException](reader(s""""$s"""").readChar()).getMessage.contains(error1))
        assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsChar()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte).readChar())
          .getMessage.contains(error))
        assert(intercept[JsonReaderException](reader2('"'.toByte +: bytes :+ '"'.toByte :+ ':'.toByte).readKeyAsChar())
          .getMessage.contains(error))
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
      reader(" \n\t\r123").readByte() shouldBe 123.toByte
      reader(" \n\t\r-123").readByte() shouldBe (-123).toByte
    }
    "parse valid byte values and stops on not numeric chars (except '.', 'e', 'E')" in {
      reader("0$").readByte() shouldBe 0
      reader("123$").readByte() shouldBe 123
    }
  }
  "JsonReader.readByte, JsonReader.readKeyAsByte and JsonReader.readStringAsByte" should {
    def check(n: Byte): Unit = {
      val s = n.toString
      reader(s).readByte() shouldBe n
      reader(s""""$s":""").readKeyAsByte() shouldBe n
      reader(s""""$s"""").readStringAsByte() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readByte()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsByte()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsByte()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader(s).readByte()).getMessage.contains(error))

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
      reader(" \n\t\r12345").readShort() shouldBe 12345.toShort
      reader(" \n\t\r-12345").readShort() shouldBe -12345.toShort
    }
    "parse valid short values and stops on not numeric chars (except '.', 'e', 'E')" in {
      reader("0$").readShort() shouldBe 0
      reader("12345$").readShort() shouldBe 12345
    }
  }
  "JsonReader.readShort, JsonReader.readKeyAsShort and JsonReader.readStringAsShort" should {
    def check(n: Short): Unit = {
      val s = n.toString
      reader(s).readShort() shouldBe n
      reader(s""""$s":""").readKeyAsShort() shouldBe n
      reader(s""""$s"""").readStringAsShort() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readShort()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsShort()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsShort()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader(s).readShort()).getMessage.contains(error))

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
      reader(" \n\t\r123456789").readInt() shouldBe 123456789
      reader(" \n\t\r-123456789").readInt() shouldBe -123456789
    }
    "parse valid int values and stops on not numeric chars (except '.', 'e', 'E')" in {
      reader("0$").readInt() shouldBe 0
      reader("123456789$").readInt() shouldBe 123456789
    }
  }
  "JsonReader.readInt, JsonReader.readKeyAsInt and JsonReader.readStringAsInt" should {
    def check(n: Int): Unit = {
      val s = n.toString
      reader(s).readInt() shouldBe n
      reader(s""""$s":""").readKeyAsInt() shouldBe n
      reader(s""""$s"""").readStringAsInt() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readInt()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsInt()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsInt()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader(s).readInt()).getMessage.contains(error))

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
      reader(" \n\t\r1234567890123456789").readLong() shouldBe 1234567890123456789L
      reader(" \n\t\r-1234567890123456789").readLong() shouldBe -1234567890123456789L
    }
    "parse valid long values and stops on not numeric chars (except '.', 'e', 'E')" in {
      reader("0$").readLong() shouldBe 0L
      reader("1234567890123456789$").readLong() shouldBe 1234567890123456789L
    }
  }
  "JsonReader.readLong, JsonReader.readKeyAsLong and JsonReader.readStringAsLong" should {
    def check(n: Long): Unit = {
      val s = n.toString
      reader(s).readLong() shouldBe n
      reader(s""""$s":""").readKeyAsLong() shouldBe n
      reader(s""""$s"""").readStringAsLong() shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readLong()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsLong()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsLong()).getMessage.contains(error2))
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
        assert(intercept[JsonReaderException](reader(s).readLong()).getMessage.contains(error))

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
      reader(" \n\t\r12345.6789").readFloat() shouldBe 12345.6789f
      reader(" \n\t\r-12345.6789").readFloat() shouldBe -12345.6789f
    }
    "parse valid float values and stops on not numeric chars" in {
      reader("0$").readFloat() shouldBe 0.0f
      reader("12345$").readFloat() shouldBe 12345f
      reader("12345.6789$").readFloat() shouldBe 12345.6789f
      reader("12345.6789e10$").readFloat() shouldBe 1.23456788e14f
      reader("12345678901234567890e12345678901234567890$").readFloat() shouldBe Float.PositiveInfinity
    }
  }
  "JsonReader.readFloat, JsonReader.readKeyAsFloat and JsonReader.readStringAsFloat" should {
    def check(s: String, n: Float): Unit = {
      reader(s).readFloat() shouldBe n
      reader(s""""$s":""").readKeyAsFloat() shouldBe n
      reader(s""""$s"""").readStringAsFloat() shouldBe n
    }

    def check2(s: String): Unit = {
      reader(s).readFloat().toString shouldBe s
      reader(s""""$s":""").readKeyAsFloat().toString shouldBe s
      reader(s""""$s"""").readStringAsFloat().toString shouldBe s
    }

    def checkFloat(s: String): Unit = check(s, java.lang.Float.parseFloat(s))

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readFloat()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsFloat()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsFloat()).getMessage.contains(error2))
    }

    "parse valid float values" in {
      checkFloat("1.199999988079071")
      checkFloat("3.4028235677973366e38")
      checkFloat("7.006492321624086e-46")
      forAll(minSuccessful(100000)) { (n: Float) =>
        checkFloat(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Double) =>
        checkFloat(n.toString)
      }
      //(1 to Int.MaxValue).par.foreach { n =>
      forAll(minSuccessful(100000)) { (n: Int) =>
        val x = java.lang.Float.intBitsToFloat(n)
        if (java.lang.Float.isFinite(x)) checkFloat(x.toString)
      }
      forAll(Gen.choose(0, (1 << 24) - 1), Gen.choose(-10, 10), minSuccessful(100000)) { (m: Int, e: Int) =>
        checkFloat(s"${m}e$e")
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        val x = java.lang.Double.longBitsToDouble(n & ~0xFFFFFFFL)
        if (java.lang.Double.isFinite(x)) checkFloat(n.toString)
      }
      forAll(genBigInt, minSuccessful(100000)) { (n: BigInt) =>
        checkFloat(n.toString)
      }
      forAll(genBigDecimal, minSuccessful(100000)) { (n: BigDecimal) =>
        checkFloat(n.toString)
      }
    }
    "parse infinity on float overflow" in {
      check("12345e6789", Float.PositiveInfinity)
      check("-12345e6789", Float.NegativeInfinity)
      check("12345678901234567890e12345678901234567890", Float.PositiveInfinity)
      check("-12345678901234567890e12345678901234567890", Float.NegativeInfinity)
      reader("12345678901234567890e12345678901234567890$").readFloat() shouldBe Float.PositiveInfinity
      reader("-12345678901234567890e12345678901234567890$").readFloat() shouldBe Float.NegativeInfinity
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
        assert(intercept[JsonReaderException](reader(s).readFloat()).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readDouble" should {
    "parse valid double values with skipping of JSON space characters" in {
      reader(" \n\t\r123456789.12345678").readDouble() shouldBe 1.2345678912345678e8
      reader(" \n\t\r-123456789.12345678").readDouble() shouldBe -1.2345678912345678e8
    }
    "parse valid double values and stops on not numeric chars" in {
      reader("0$").readDouble() shouldBe 0.0
      reader("123456789$").readDouble() shouldBe 123456789.0
      reader("123456789.12345678$").readDouble() shouldBe 1.2345678912345678e8
      reader("123456789.123456789e10$").readDouble() shouldBe 1.23456789123456794e18
      reader("12345678901234567890e12345678901234567890$").readDouble() shouldBe Double.PositiveInfinity
    }
  }
  "JsonReader.readDouble, JsonReader.readKeyAsDouble and JsonReader.readStringAsDouble" should {
    def check(s: String, n: Double): Unit = {
      reader(s).readDouble() shouldBe n
      reader(s""""$s":""").readKeyAsDouble() shouldBe n
      reader(s""""$s"""").readStringAsDouble() shouldBe n
    }

    def check2(s: String): Unit = {
      reader(s).readDouble().toString shouldBe s
      reader(s""""$s":""").readKeyAsDouble().toString shouldBe s
      reader(s""""$s"""").readStringAsDouble().toString shouldBe s
    }

    def checkDouble(s: String): Unit = check(s, java.lang.Double.parseDouble(s))

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readDouble()).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsDouble()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsDouble()).getMessage.contains(error2))
    }

    "parse valid double values" in {
      forAll(minSuccessful(100000)) { (n: Double) =>
        checkDouble(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Float) =>
        checkDouble(n.toString)
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        val x = java.lang.Double.longBitsToDouble(n)
        if (java.lang.Double.isFinite(x)) checkDouble(x.toString)
      }
      forAll(Gen.choose(0L, (1L << 53) - 1), Gen.choose(-22, 22 + 15), minSuccessful(100000)) { (m: Long, e: Int) =>
        checkDouble(s"${m}e$e")
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        checkDouble(n.toString)
      }
      forAll(genBigInt, minSuccessful(100000)) { (n: BigInt) =>
        checkDouble(n.toString)
      }
      forAll(genBigDecimal, minSuccessful(100000)) { (n: BigDecimal) =>
        checkDouble(n.toString)
      }
    }
    "parse infinity on double overflow" in {
      check("12345e6789", Double.PositiveInfinity)
      check("-12345e6789", Double.NegativeInfinity)
      check("12345678901234567890e12345678901234567890", Double.PositiveInfinity)
      check("-12345678901234567890e12345678901234567890", Double.NegativeInfinity)
      reader("12345678901234567890e12345678901234567890$").readDouble() shouldBe Double.PositiveInfinity
      reader("-12345678901234567890e12345678901234567890$").readDouble() shouldBe Double.NegativeInfinity
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
        assert(intercept[JsonReaderException](reader(s).readDouble()).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigInt" should {
    "parse valid number values with skipping of JSON space characters" in {
      reader(" \n\t\r12345678901234567890").readBigInt(null) shouldBe BigInt("12345678901234567890")
      reader(" \n\t\r-12345678901234567890").readBigInt(null) shouldBe BigInt("-12345678901234567890")
    }
    "parse valid number values and stops on not numeric chars (except '.', 'e', 'E')" in {
      reader("0$").readBigInt(null) shouldBe BigInt("0")
      reader("1234567890123456789$").readBigInt(null) shouldBe BigInt("1234567890123456789")
    }
  }
  "JsonReader.readBigInt and JsonReader.readStringAsBigInt" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBigInt(null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readStringAsBigInt(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null").readBigInt(BigInt("12345")) shouldBe BigInt("12345")
      reader("null").readStringAsBigInt(BigInt("12345")) shouldBe BigInt("12345")
    }
  }
  "JsonReader.readBigInt, JsonReader.readStringAsBigInt and JsonReader.readKeyAsBigInt" should {
    def check(n: BigInt): Unit = {
      val s = n.toString
      reader(s).readBigInt(null, Int.MaxValue) shouldBe n
      reader(s""""$s":""").readKeyAsBigInt(Int.MaxValue) shouldBe n
      reader(s""""$s"""").readStringAsBigInt(null, Int.MaxValue) shouldBe n
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBigInt(null)).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBigInt()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBigInt(null)).getMessage.contains(error2))
    }

    "parse valid number values" in {
      forAll(genBigInt, minSuccessful(100000)) { (n: BigInt) =>
        check(n)
      }
    }
    "parse big number values without overflow up to limits" in {
      val bigNumber = "9" * 1000
      check(BigInt(bigNumber))
      check(BigInt(s"-$bigNumber"))
    }
    "throw parsing exception for values with more than max allowed digits" in {
      checkError("9" * 308,
        "value exceeds limit for number of digits, offset: 0x00000133",
        "value exceeds limit for number of digits, offset: 0x00000134")
      checkError(s"-${"9" * 308}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
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
        assert(intercept[JsonReaderException](reader(s).readBigInt(null)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.readBigDecimal" should {
    "parse valid number values with skipping of JSON space characters" in {
      reader(" \n\t\r1234567890123456789.0123456789").readBigDecimal(null) shouldBe
        BigDecimal("1234567890123456789.0123456789")
      reader(" \n\t\r-1234567890123456789.0123456789").readBigDecimal(null) shouldBe
        BigDecimal("-1234567890123456789.0123456789")
    }
    "parse valid number values and stops on not numeric chars" in {
      reader("0$").readBigDecimal(null) shouldBe BigDecimal("0")
      reader("1234567890123456789$").readBigDecimal(null) shouldBe BigDecimal("1234567890123456789")
      reader("1234567890123456789.0123456789$").readBigDecimal(null) shouldBe
        BigDecimal("1234567890123456789.0123456789")
      reader("1234567890123456789.0123456789e10$").readBigDecimal(null) shouldBe
        BigDecimal("12345678901234567890123456789")
    }
  }
  "JsonReader.readBigDecimal and JsonReader.readStringAsBigDecimal" should {
    "don't parse null value" in {
      assert(intercept[JsonReaderException](reader("null").readBigDecimal(null))
        .getMessage.contains("illegal number, offset: 0x00000000"))
      assert(intercept[JsonReaderException](reader("null").readStringAsBigDecimal(null))
        .getMessage.contains("expected '\"', offset: 0x00000000"))
    }
    "return supplied default value instead of null value" in {
      reader("null").readBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
      reader("null").readStringAsBigDecimal(BigDecimal("12345")) shouldBe BigDecimal("12345")
    }
  }
  "JsonReader.readBigDecimal, JsonReader.readKeyAsBigDecimal and JsonReader.readStringAsBigDecimal" should {
    def check(s: String, mc: MathContext = bigDecimalMathContext, scaleLimit: Int = bigDecimalScaleLimit,
              digitsLimit: Int = bigDecimalDigitsLimit): Unit = {
      def compare(a: BigDecimal, b: BigDecimal): Unit = {
        a.bigDecimal shouldBe b.bigDecimal
        a.mc shouldBe b.mc
      }

      val n = BigDecimal(s, mc)
      compare(reader(s).readBigDecimal(null, mc, scaleLimit, digitsLimit), n)
      compare(reader(s""""$s":""").readKeyAsBigDecimal(mc, scaleLimit, digitsLimit), n)
      compare(reader(s""""$s"""").readStringAsBigDecimal(null, mc, scaleLimit, digitsLimit), n)
    }

    def checkError(s: String, error1: String, error2: String): Unit = {
      assert(intercept[JsonReaderException](reader(s).readBigDecimal(null)).getMessage.contains(error1))
      assert(intercept[JsonReaderException](reader(s""""$s":""").readKeyAsBigDecimal()).getMessage.contains(error2))
      assert(intercept[JsonReaderException](reader(s""""$s"""").readStringAsBigDecimal(null))
        .getMessage.contains(error2))
    }

    "parse valid number values with scale less than specified maximum" in {
      forAll(genBigDecimal, minSuccessful(100000)) { (n: BigDecimal) =>
        check(n.toString, MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue)
      }
    }
    "parse big number values without overflow up to limits" in {
      check("1234567890" * 30)
      check("12345e67")
      check("-12345e67")
      check("12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue)
      check("-12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue)
    }
    "parse small number values without underflow up to limits" in {
      check(s"0.${"0" * 100}1234567890123456789012345678901234")
      check("12345e-67")
      check("-12345e-67")
      check("12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue)
      check("-12345678901234567890123456789012345678901234567890123456789012345678901234567890e-123456789", MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue)
    }
    "throw number format exception for too big mantissa" in {
      checkError("9" * 308,
        "value exceeds limit for number of digits, offset: 0x00000133",
        "value exceeds limit for number of digits, offset: 0x00000134")
      checkError(s"-${"9" * 308}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
      checkError(s"${"9" * 154}.${"9" * 154}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
      checkError(s"0.${"0" * 307}",
        "value exceeds limit for number of digits, offset: 0x00000134",
        "value exceeds limit for number of digits, offset: 0x00000135")
    }
    "throw number format exception for too big scale" in {
      check(s"1e${bigDecimalScaleLimit - 1}")
      check(s"1e-${bigDecimalScaleLimit - 1}")
      check(s"${"1" * 50}e${bigDecimalScaleLimit - 17}", MathContext.DECIMAL128)
      check(s"${"1" * 50}e-${bigDecimalScaleLimit + 15}", MathContext.DECIMAL128)
      checkError(s"1e$bigDecimalScaleLimit",
        "value exceeds limit for scale, offset: 0x00000005",
        "value exceeds limit for scale, offset: 0x00000006")
      checkError(s"1e-$bigDecimalScaleLimit",
        "value exceeds limit for scale, offset: 0x00000006",
        "value exceeds limit for scale, offset: 0x00000007")
      checkError(s"1.1e${bigDecimalScaleLimit + 1}",
        "value exceeds limit for scale, offset: 0x00000007",
        "value exceeds limit for scale, offset: 0x00000008")
      checkError(s"1.1e-${bigDecimalScaleLimit + 1}",
        "value exceeds limit for scale, offset: 0x00000008",
        "value exceeds limit for scale, offset: 0x00000009")
      checkError("1e2147483648",
        "illegal number, offset: 0x0000000b",
        "illegal number, offset: 0x0000000c")
      checkError("1e-2147483649",
        "illegal number, offset: 0x0000000c",
        "illegal number, offset: 0x0000000d")
      checkError("1e9999999999",
        "illegal number, offset: 0x0000000b",
        "illegal number, offset: 0x0000000c")
      checkError("1e-9999999999",
        "illegal number, offset: 0x0000000c",
        "illegal number, offset: 0x0000000d")
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
        assert(intercept[JsonReaderException](reader(s).readBigDecimal(null)).getMessage.contains(error))

      checkError("00", "illegal number with leading zero, offset: 0x00000000")
      checkError("-00", "illegal number with leading zero, offset: 0x00000001")
      checkError("012345.6789", "illegal number with leading zero, offset: 0x00000000")
      checkError("-012345.6789", "illegal number with leading zero, offset: 0x00000001")
    }
  }
  "JsonReader.setMark and JsonReader.rollbackToMark" should {
    "store current position of parsing and return back to it" in {
      val jsonReader = reader("{}")
      jsonReader.setMark()
      jsonReader.skip()
      jsonReader.rollbackToMark()
      jsonReader.nextToken() shouldBe '{'
    }
    "throw exception in case of rollbackToMark was called before setMark" in {
      val jsonReader = reader("{}")
      jsonReader.skip()
      assert(intercept[ArrayIndexOutOfBoundsException](jsonReader.rollbackToMark())
        .getMessage.contains("expected preceding call of 'setMark()'"))
    }
  }
  "JsonReader.skipToKey" should {
    "return true in case of key is found and set current position of parsing to its value" in {
      val jsonReader = reader("""{"key1":1,"key2":2}""")
      jsonReader.isNextToken('{')
      jsonReader.skipToKey("key2") shouldBe true
      jsonReader.readInt() shouldBe 2
    }
    "return false in case of key cannot be found and set current positing to the closing of object" in {
      val jsonReader = reader("""{"key1":1}""")
      jsonReader.isNextToken('{')
      jsonReader.skipToKey("key2")
      jsonReader.isCurrentToken('}') shouldBe true
    }
  }
  "JsonReader.requiredFieldError" should {
    "throw parsing exception with missing required field" in {
      val jsonReader = reader("{}")
      jsonReader.nextToken()
      assert(intercept[JsonReaderException](jsonReader.requiredFieldError("name"))
        .getMessage.contains("missing required field \"name\", offset: 0x00000000"))
    }
  }
  "JsonReader.duplicatedKeyError" should {
    "throw parsing exception with name of duplicated key" in {
      val jsonReader = reader("\"xxx\"")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.duplicatedKeyError(len))
        .getMessage.contains("duplicated field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.unexpectedKeyError" should {
    "throw parsing exception with name of unexpected key" in {
      val jsonReader = reader("\"xxx\"")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.unexpectedKeyError(len))
        .getMessage.contains("unexpected field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.discriminatorError" should {
    "throw parsing exception with unexpected discriminator" in {
      val jsonReader = reader("\"xxx\"")
      jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.discriminatorError())
        .getMessage.contains("xxx"))
    }
  }
  "JsonReader.discriminatorValueError" should {
    "throw parsing exception with unexpected discriminator value" in {
      val jsonReader = reader("\"xxx\"")
      val value = jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.discriminatorValueError(value))
        .getMessage.contains("illegal value of discriminator field \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.enumValueError" should {
    "throw parsing exception with unexpected enum value as string" in {
      val jsonReader = reader("\"xxx\"")
      val value = jsonReader.readString(null)
      assert(intercept[JsonReaderException](jsonReader.enumValueError(value))
        .getMessage.contains("illegal enum value \"xxx\", offset: 0x00000004"))
    }
    "throw parsing exception with unexpected enum value as length of character buffer" in {
      val jsonReader = reader("\"xxx\"")
      val len = jsonReader.readStringAsCharBuf()
      assert(intercept[JsonReaderException](jsonReader.enumValueError(len))
        .getMessage.contains("illegal enum value \"xxx\", offset: 0x00000004"))
    }
  }
  "JsonReader.commaError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("{}")
      jsonReader.isNextToken(',')
      assert(intercept[JsonReaderException](jsonReader.commaError())
        .getMessage.contains("expected ',', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayStartOrNullError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("{}")
      jsonReader.isNextToken('[')
      assert(intercept[JsonReaderException](jsonReader.arrayStartOrNullError())
        .getMessage.contains("expected '[' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("}")
      jsonReader.isNextToken(']')
      assert(intercept[JsonReaderException](jsonReader.arrayEndError())
        .getMessage.contains("expected ']', offset: 0x00000000"))
    }
  }
  "JsonReader.arrayEndOrCommaError" should {
    val jsonReader = reader("}")
    jsonReader.isNextToken(']')
    "throw parsing exception with expected token(s)" in {
      assert(intercept[JsonReaderException](jsonReader.arrayEndOrCommaError())
        .getMessage.contains("expected ']' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader.objectStartOrNullError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("[]")
      jsonReader.isNextToken('{')
      assert(intercept[JsonReaderException](jsonReader.objectStartOrNullError())
        .getMessage.contains("expected '{' or null, offset: 0x00000000"))
    }
  }
  "JsonReader.objectEndOrCommaError" should {
    "throw parsing exception with expected token(s)" in {
      val jsonReader = reader("]")
      jsonReader.isNextToken('}')
      assert(intercept[JsonReaderException](jsonReader.objectEndOrCommaError())
        .getMessage.contains("expected '}' or ',', offset: 0x00000000"))
    }
  }
  "JsonReader" should {
    "support hex dumps with offsets that greater than 4Gb" in {
      assert(intercept[JsonReaderException](reader("null", 1L << 41).readInt())
        .getMessage.contains(
        """illegal number, offset: 0x20000000000, buf:
          |+----------+-------------------------------------------------+------------------+
          ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
          |+----------+-------------------------------------------------+------------------+
          || 00000000 | 6e 75 6c 6c                                     | null             |
          |+----------+-------------------------------------------------+------------------+""".stripMargin))
    }
  }

  def validateSkip(s: String): Unit = {
    def checkWithSuffix(s: String, suffix: Char): Unit = {
      val r = reader(s + suffix)
      r.skip()
      r.nextToken().toChar shouldBe suffix
    }

    def check(s: String): Unit = {
      val r = reader(s)
      r.skip()
      assert(intercept[JsonReaderException](r.nextToken()).getMessage.contains("unexpected end of input"))
    }

    checkWithSuffix(s, ',')
    checkWithSuffix(s, '}')
    checkWithSuffix(s, ']')
    check(s)
  }

  def reader(json: String, totalRead: Long = 0): JsonReader = reader2(json.getBytes(UTF_8), totalRead)

  def reader2(jsonBytes: Array[Byte], totalRead: Long = 0): JsonReader =
    new JsonReader(new Array[Byte](12), // a minimal allowed length
      0, 0, 2147483647, new Array[Char](0), null, new ByteArrayInputStream(jsonBytes), totalRead,
      ReaderConfig(throwReaderExceptionWithStackTrace = true))
}