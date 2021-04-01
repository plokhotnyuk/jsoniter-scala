package com.github.plokhotnyuk.jsoniter_scala.core

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.time.format.DateTimeFormatter
import java.util.{Base64, UUID}

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class JsonWriterSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "WriterConfig.<init>" should {
    "have handy defaults" in {
      WriterConfig.throwWriterExceptionWithStackTrace shouldBe false
      WriterConfig.indentionStep shouldBe 0
      WriterConfig.escapeUnicode shouldBe false
      WriterConfig.preferredBufSize shouldBe 16384
    }
    "throw exception in case for unsupported values of params" in {
      WriterConfig.withIndentionStep(0)
      assert(intercept[IllegalArgumentException](WriterConfig.withIndentionStep(-1))
        .getMessage.contains("'indentionStep' should be not less than 0"))
      WriterConfig.withPreferredBufSize(1)
      assert(intercept[IllegalArgumentException](WriterConfig.withPreferredBufSize(0))
        .getMessage.contains("'preferredBufSize' should be not less than 1"))
    }
  }
  "JsonWriter.isNonEscapedAscii" should {
    "return false for all escaped ASCII or non-ASCII chars" in {
      forAll(minSuccessful(10000)) { ch: Char =>
        JsonWriter.isNonEscapedAscii(ch) shouldBe !isEscapedAscii(ch) && ch < 128
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and and JsonWriter.writeKey for boolean" should {
    "write valid true and false values" in {
      def check(value: Boolean): Unit = {
        val s = value.toString
        withWriter(_.writeVal(value)) shouldBe s
        withWriter(_.writeValAsString(value)) shouldBe s""""$s""""
        withWriter(_.writeKey(value)) shouldBe s""""$s":"""
        withWriter(WriterConfig.withIndentionStep(2))(_.writeKey(value)) shouldBe s""""$s": """
      }

      check(value = true)
      check(value = false)
    }
  }
  "JsonWriter.writeNonEscapedAsciiVal and JsonWriter.writeNonEscapedAsciiKey" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeNonEscapedAsciiVal(null.asInstanceOf[String])))
      intercept[NullPointerException](withWriter(_.writeNonEscapedAsciiKey(null.asInstanceOf[String])))
    }
    "write string of Ascii chars which should not be escaped" in {
      def check(s: String): Unit = {
        withWriter(_.writeNonEscapedAsciiVal(s)) shouldBe s""""$s""""
        withWriter(_.writeNonEscapedAsciiKey(s)) shouldBe s""""$s":"""
      }

      forAll(Gen.listOf(genAsciiChar).map(_.mkString.filter(JsonWriter.isNonEscapedAscii)), minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for UUID" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[UUID])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[UUID])))
    }
    "write UUID as a string representation according to format that defined in IETF RFC4122 (section 3)" in {
      def check(x: UUID): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      forAll(genUUID, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Duration" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Duration])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Duration])))
    }
    "write Duration as a string representation according to ISO-8601 format" in {
      def check(x: Duration, s: String): Unit = {
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(Duration.ZERO, "PT0S")
      check(Duration.ofSeconds(-60, -1), "PT-1M-0.000000001S")
      check(Duration.ofSeconds(-60, 1), "PT-59.999999999S")
      check(Duration.ofSeconds(-60, 20000000000L), "PT-40S")
      forAll(genDuration, minSuccessful(10000))(x => check(x, x.toString))
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Instant" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Instant])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Instant])))
    }
    "write Instant as a string representation according to ISO-8601 format" in {
      def check(x: Instant): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(Instant.MAX)
      check(Instant.MIN)
      forAll(genInstant, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for LocalDate" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[LocalDate])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[LocalDate])))
    }
    "write LocalDate as a string representation according to ISO-8601 format" in {
      def check(x: LocalDate): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(LocalDate.MAX)
      check(LocalDate.MIN)
      forAll(genLocalDate, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for LocalDateTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[LocalDateTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[LocalDateTime])))
    }
    "write LocalDateTime as a string representation according to ISO-8601 format" in {
      def check(x: LocalDateTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(LocalDateTime.MAX)
      check(LocalDateTime.MIN)
      forAll(genLocalDateTime, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for LocalTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[LocalTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[LocalTime])))
    }
    "write LocalTime as a string representation according to ISO-8601 format" in {
      def check(x: LocalTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(LocalTime.MAX)
      check(LocalTime.MIN)
      forAll(genLocalTime, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for MonthDay" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[MonthDay])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[MonthDay])))
    }
    "write MonthDay as a string representation according to ISO-8601 format" in {
      def check(x: MonthDay): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(MonthDay.of(12, 31))
      check(MonthDay.of(1, 1))
      forAll(genMonthDay, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for OffsetDateTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[OffsetDateTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[OffsetDateTime])))
    }
    "write OffsetDateTime as a string representation according to ISO-8601 format" in {
      def check(x: OffsetDateTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(OffsetDateTime.MAX)
      check(OffsetDateTime.MIN)
      forAll(genOffsetDateTime, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for OffsetTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[OffsetTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[OffsetTime])))
    }
    "write OffsetTime as a string representation according to ISO-8601 format" in {
      def check(x: OffsetTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(OffsetTime.MAX)
      check(OffsetTime.MIN)
      forAll(genOffsetTime, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Period" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Period])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Period])))
    }
    "write Period as a string representation according to ISO-8601 format" in {
      def check(x: Period): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(Period.ZERO)
      forAll(genPeriod, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Year" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Year])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Year])))
    }
    "write Year as a string representation according to ISO-8601 format" in {
      val yearFormatter = DateTimeFormatter.ofPattern("uuuu")

      def check(x: Year): Unit = {
        val s = x.format(yearFormatter)
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(Year.of(Year.MAX_VALUE))
      check(Year.of(Year.MIN_VALUE))
      forAll(genYear, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for YearMonth" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[YearMonth])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[YearMonth])))
    }
    "write YearMonth as a string representation according to ISO-8601 format" in {
      def check(x: YearMonth): Unit = {
        val s = (if (x.getYear >= 10000) "+" else "") + x.toString // '+' is required for years that extends 4 digits, see ISO 8601:2004 sections 3.4.2, 4.1.2.4
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(YearMonth.of(Year.MAX_VALUE, 12))
      check(YearMonth.of(Year.MIN_VALUE, 1))
      forAll(genYearMonth, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for ZonedDateTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[ZonedDateTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[ZonedDateTime])))
    }
    "write ZonedDateTime as a string representation according to ISO-8601 format with optional IANA timezone identifier in JDK 8+ format" in {
      def check(x: ZonedDateTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MAX))
      check(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MIN))
      forAll(genZonedDateTime, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for ZoneOffset" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[ZoneOffset])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[ZoneOffset])))
    }
    "write ZoneOffset as a string representation according to ISO-8601 format" in {
      def check(x: ZoneOffset): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneOffset, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for ZoneId" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[ZoneId])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[ZoneId])))
    }
    "write ZoneId as a string representation according to ISO-8601 format for timezone offset or JDK 8+ format for IANA timezone identifier" in {
      def check(x: ZoneId): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s""""$s""""
        withWriter(_.writeKey(x)) shouldBe s""""$s":"""
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneId, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for string" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[String])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[String])))
    }
    "write string of Unicode chars which are non-surrogate and should not be escaped" in {
      def check(s: String): Unit = {
        withWriter(_.writeVal(s)) shouldBe s""""$s""""
        withWriter(_.writeKey(s)) shouldBe s""""$s":"""
      }

      check("Oó!")
      forAll(minSuccessful(10000)) { s: String =>
        whenever(s.forall(ch => !Character.isSurrogate(ch) && !isEscapedAscii(ch))) {
          check(s)
        }
      }
    }
    "write strings with chars that should be escaped" in {
      def check(s: String, escapeUnicode: Boolean, f: String => String = _.flatMap(toEscaped(_))): Unit = {
        withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeVal(s)) shouldBe s""""${f(s)}""""
        withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeKey(s)) shouldBe s""""${f(s)}":"""
      }

      check("Oó!", escapeUnicode = true, _ => "O\\u00f3!")
      check("Є!", escapeUnicode = true, _ => "\\u0404!")
      forAll(Gen.listOf(genEscapedAsciiChar).map(_.mkString), Gen.oneOf(true, false), minSuccessful(10000)) {
        (s, escapeUnicode) => check(s, escapeUnicode)
      }
    }
    "write strings with escaped Unicode chars when it is specified by provided writer config" in {
      def check(s: String, f: String => String = _.flatMap(toEscaped(_))): Unit = {
        withWriter(WriterConfig.withEscapeUnicode(true))(_.writeVal(s)) shouldBe s""""${f(s)}""""
        withWriter(WriterConfig.withEscapeUnicode(true))(_.writeKey(s)) shouldBe s""""${f(s)}":"""
      }

      forAll(minSuccessful(10000)) { s: String =>
        whenever(s.forall(ch => isEscapedAscii(ch) || ch >= 128)) {
          check(s)
        }
      }
    }
    "write strings with valid character surrogate pair" in {
      def check(s: String): Unit = {
        withWriter(_.writeVal(s)) shouldBe s""""$s""""
        withWriter(_.writeKey(s)) shouldBe s""""$s":"""
        withWriter(WriterConfig.withEscapeUnicode(true))(_.writeVal(s)) shouldBe s""""${s.flatMap(toEscaped(_))}""""
        withWriter(WriterConfig.withEscapeUnicode(true))(_.writeKey(s)) shouldBe s""""${s.flatMap(toEscaped(_))}":"""
      }

      forAll(genHighSurrogateChar, genLowSurrogateChar, minSuccessful(10000)) { (ch1, ch2) =>
        check(ch1.toString + ch2.toString)
      }
    }
    "write string with mixed Latin-1 characters when escaping of Unicode chars is turned on" in {
      withWriter(WriterConfig.withEscapeUnicode(true))(_.writeVal("a\bc")) shouldBe "\"a\\bc\""
      withWriter(WriterConfig.withEscapeUnicode(true))(_.writeKey("a\bc")) shouldBe "\"a\\bc\":"
    }
    "write string with mixed UTF-8 characters when escaping of Unicode chars is turned off" in {
      withWriter(_.writeVal("ї\bc\u0000")) shouldBe "\"ї\\bc\\u0000\""
      withWriter(_.writeKey("ї\bc\u0000")) shouldBe "\"ї\\bc\\u0000\":"
    }
    "throw i/o exception in case of illegal character surrogate pair" in {
      def checkError(s: String, escapeUnicode: Boolean): Unit = {
        assert(intercept[JsonWriterException](withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeVal(s)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
        assert(intercept[JsonWriterException](withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeKey(s)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
      }

      forAll(genSurrogateChar, Gen.oneOf(true, false), minSuccessful(10000)) { (ch, escapeUnicode) =>
        checkError(ch.toString, escapeUnicode)
        checkError(ch.toString + ch.toString, escapeUnicode)
      }
      forAll(genLowSurrogateChar, genHighSurrogateChar, Gen.oneOf(true, false), minSuccessful(10000)) {
        (ch1, ch2, escapeUnicode) => checkError(ch1.toString + ch2.toString, escapeUnicode)
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for char" should {
    "write string with Unicode chars which are non-surrogate or should not be escaped" in {
      forAll(minSuccessful(10000)) { ch: Char =>
        whenever(!Character.isSurrogate(ch) && !isEscapedAscii(ch)) {
          withWriter(_.writeVal(ch)) shouldBe s""""$ch""""
          withWriter(_.writeKey(ch)) shouldBe s""""$ch":"""
        }
      }
    }
    "write string with chars that should be escaped" in {
      forAll(genEscapedAsciiChar, minSuccessful(10000)) { ch: Char =>
        withWriter(_.writeVal(ch)) shouldBe s""""${toEscaped(ch)}""""
        withWriter(_.writeKey(ch)) shouldBe s""""${toEscaped(ch)}":"""
      }
      forAll(genNonAsciiChar, minSuccessful(10000)) { ch: Char =>
        whenever(!Character.isSurrogate(ch)) {
          withWriter(WriterConfig.withEscapeUnicode(true))(_.writeVal(ch)) shouldBe s""""${toEscaped(ch)}""""
          withWriter(WriterConfig.withEscapeUnicode(true))(_.writeKey(ch)) shouldBe s""""${toEscaped(ch)}":"""
        }
      }
    }
    "write string with escaped Unicode chars when it is specified by provided writer config" in {
      forAll(minSuccessful(10000)) { ch: Char =>
        whenever(isEscapedAscii(ch) || ch >= 128) {
          withWriter(WriterConfig.withEscapeUnicode(true))(_.writeVal(ch)) shouldBe s""""${toEscaped(ch)}""""
          withWriter(WriterConfig.withEscapeUnicode(true))(_.writeKey(ch)) shouldBe s""""${toEscaped(ch)}":"""
        }
      }
    }
    "throw i/o exception in case of surrogate pair character" in {
      forAll(genSurrogateChar, Gen.oneOf(true, false)) { (ch: Char, escapeUnicode: Boolean) =>
        assert(intercept[JsonWriterException](withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeVal(ch)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
        assert(intercept[JsonWriterException](withWriter(WriterConfig.withEscapeUnicode(escapeUnicode))(_.writeKey(ch)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for byte" should {
    "write any short values" in {
      def check(n: Byte): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      forAll(arbitrary[Byte], minSuccessful(1000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for short" should {
    "write any short values" in {
      def check(n: Short): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      forAll(arbitrary[Short], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for int" should {
    "write any int values" in {
      def check(n: Int): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      forAll(arbitrary[Int], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for long" should {
    "write any long values" in {
      def check(n: Long): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      forAll(arbitrary[Int], minSuccessful(10000))(n => check(n.toLong))
      forAll(arbitrary[Long], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for float" should {
    "write finite float values" in {
      def check(n: Float): Unit = {
        val es = n.toString
        val s = withWriter(_.writeVal(n))
        val l = s.length
        val i = s.indexOf('.')
        s.toFloat shouldBe n // no data loss when parsing by JDK
        l should be <= es.length + {
          if (TestUtils.isJS) {
            if (es.indexOf('.') < 0) 3 // formatting differs from JS for floats represented as whole numbers
            else 0 // rounding and formatting isn't worse than in JS for floats represented in decimal or scientific notation
          } else 0 // rounding and formatting isn't worse than in JDK
        }
        i should be > 0 // has the '.' character inside
        i should be < l - 1
        Character.isDigit(s.charAt(i - 1)) shouldBe true // has a digit before the '.' character
        Character.isDigit(s.charAt(i + 1)) shouldBe true // has a digit after the '.' character
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(0.0f)
      check(-0.0f)
      check(1.0f)
      check(-1.0f)
      check(1.0E7f)
      check(java.lang.Float.intBitsToFloat(0x00800000)) // subnormal
      check(9999999.0f)
      check(0.001f)
      check(0.0009999999f)
      check(Float.MinValue)
      check(Float.MinPositiveValue)
      check(Float.MaxValue)
      check(3.3554448E7f)
      check(8.999999E9f)
      check(3.4366717E10f)
      check(4.7223665E21f)
      check(8388608.0f)
      check(1.6777216E7f)
      check(3.3554436E7f)
      check(6.7131496E7f)
      check(1.9310392E-38f)
      check(-2.47E-43f)
      check(1.993244E-38f)
      check(4103.9003f)
      check(5.3399997E9f)
      check(6.0898E-39f)
      check(0.0010310042f)
      check(2.8823261E17f)
      check(7.038531E-26f)
      check(9.2234038E17f)
      check(6.7108872E7f)
      check(1.0E-44f)
      check(2.816025E14f)
      check(9.223372E18f)
      check(1.5846085E29f)
      check(1.1811161E19f)
      check(5.368709E18f)
      check(4.6143165E18f)
      check(0.007812537f)
      check(1.4E-45f)
      check(1.18697724E20f)
      check(1.00014165E-36f)
      check(200f)
      check(3.3554432E7f)
      check(1.26217745E-29f)
      forAll(arbitrary[Int], minSuccessful(10000))(n => check(n.toFloat))
      forAll(arbitrary[Int], minSuccessful(10000)) { n =>
        val x = java.lang.Float.intBitsToFloat(n)
        whenever(java.lang.Float.isFinite(x)) {
          check(x)
        }
      }
//      (0 to Int.MaxValue).foreach { n =>
//        val x = java.lang.Float.intBitsToFloat(n)
//        if (java.lang.Float.isFinite(x)) {
//          try check(x) catch { case _: Throwable => println(x) }
//        }
//      }
      forAll(genFiniteFloat, minSuccessful(10000))(check)
    }
    "write float values exactly as expected" in {
      def check(n: Float, s: String): Unit = {
        n shouldBe s.toFloat
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(1.0E-43f, "9.9E-44") // 71 * 2 ^ -149 == 9.94... * 10 ^ -44
      check(1.0E-45f, "1.4E-45") // 1 * 2 ^ -149 == 1.40... * 10 ^ -45
      check(7.1E10f, "7.1E10") // Java serializes it to "7.0999998E10" (string of redundant 9s)
      check(1.1E15f, "1.1E15") // Java serializes it to "1.09999998E15" (string of redundant 9s)
      check(1.0E17f, "1.0E17") // Java serializes it to "9.9999998E16" (string of redundant 9s)
      check(6.3E9f, "6.3E9") // Java serializes it to "6.3000003E9" (string of redundant 0s)
      check(3.0E10f, "3.0E10") // Java serializes it to "3.0000001E10" (string of redundant 0s)
      check(1.1E10f, "1.1E10") // Java serializes it to "1.10000005E10" (string of redundant 0s)
    }
    "write round-even float values" in {
      def check(n: Float, s: String): Unit = {
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(0.33007812f, "0.33007812")
      check(0.036132812f, "0.036132812")
      check(0.0063476562f, "0.0063476562")
    }
    "throw i/o exception on non-finite numbers" in {
      forAll(genNonFiniteFloat) { n =>
        assert(intercept[JsonWriterException](withWriter(_.writeVal(n))).getMessage.contains("illegal number"))
        assert(intercept[JsonWriterException](withWriter(_.writeValAsString(n))).getMessage.contains("illegal number"))
        assert(intercept[JsonWriterException](withWriter(_.writeKey(n))).getMessage.contains("illegal number"))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for double" should {
    "write finite double values" in {
      def check(n: Double): Unit = {
        val es = n.toString
        val s = withWriter(_.writeVal(n))
        val l = s.length
        val i = s.indexOf('.')
        s.toDouble shouldBe n // no data loss when parsing by JDK or JS
        l should be <= es.length + {
          if (TestUtils.isJS) {
            if (es.indexOf('.') < 0) 4 // formatting differs from JS for doubles represented as whole numbers
            else if (es.indexOf('e') < 0 && Math.abs(n) > 1) 3 // formatting differs from JS for doubles with positive exponents that are represented in decimal notation
            else 0 // rounding and formatting isn't worse than in JS for doubles represented in scientific notation
          } else 0 // rounding and formatting isn't worse than in JDK
        }
        i should be > 0 // has the '.' character inside
        i should be < l - 1 // '.' is not the last character
        Character.isDigit(s.charAt(i - 1)) shouldBe true // has a digit before the '.' character
        Character.isDigit(s.charAt(i + 1)) shouldBe true // has a digit after the '.' character
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(0.0)
      check(-0.0)
      check(1.0)
      check(-1.0)
      check(java.lang.Double.longBitsToDouble(0x0010000000000000L)) // subnormal
      check(1.0E7)
      check(9999999.999999998)
      check(0.001)
      check(0.0009999999999999998)
      check(Double.MinValue)
      check(Double.MinPositiveValue)
      check(Double.MaxValue)
      check(-2.109808898695963E16)
      check(4.940656E-318)
      check(1.18575755E-316)
      check(2.989102097996E-312)
      check(9.0608011534336E15)
      check(4.708356024711512E18)
      check(9.409340012568248E18)
      check(1.8531501765868567E21)
      check(-3.347727380279489E33)
      check(1.9430376160308388E16)
      check(-6.9741824662760956E19)
      check(4.3816050601147837E18)
      check(7.1202363472230444E-307)
      check(3.67301024534615E16)
      check(5.9604644775390625E-8)
      forAll(arbitrary[Long], minSuccessful(10000))(n => check(n.toDouble))
      forAll(arbitrary[Long], minSuccessful(10000)) { n =>
        val x = java.lang.Double.longBitsToDouble(n)
        whenever(java.lang.Double.isFinite(x)) {
          check(x)
        }
      }
      forAll(genFiniteDouble, minSuccessful(10000))(check)
    }
    "write double values exactly as expected" in {
      def check(n: Double, s: String): Unit = {
        n shouldBe s.toDouble
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(1.0E-322, "9.9E-323") // 20 * 2 ^ -1074 == 9.88... * 10 ^ -323
      check(5.0E-324, "4.9E-324") // 1 * 2 ^ -1074 == 4.94... * 10 ^ -324
      check(1.0E23, "1.0E23") // Java serializes it to "9.999999999999999E22" (string of redundant 9s)
      check(8.41E21, "8.41E21") // Java serializes it to "8.409999999999999E21" (string of redundant 9s)
      check(8.962E21, "8.962E21") // Java serializes it to "8.961999999999999E21" (string of redundant 9s)
      check(7.3879E20, "7.3879E20") // Java serializes it to "7.387900000000001E20" (string of redundant 0s)
      check(3.1E22, "3.1E22") // Java serializes it to "3.1000000000000002E22" (string of redundant 0s)
      check(5.63E21, "5.63E21") // Java serializes it to "5.630000000000001E21" (string of redundant 0s)
      check(2.82879384806159E17, "2.82879384806159E17") // Java serializes it to "2.82879384806159008E17" (18 digits, even though 17 digits are *always* enough)
      check(1.387364135037754E18, "1.387364135037754E18") // Java serializes it to "1.38736413503775411E18" (18 digits, even though 17 digits are *always* enough)
      check(1.45800632428665E17, "1.45800632428665E17") // Java serializes it to "1.45800632428664992E17" (18 digits, even though 17 digits are *always* enough)
      check(1.790086667993E18, "1.790086667993E18") // Java serializes it to "1.79008666799299994E18" (5 digits too much)
      check(2.273317134858E18, "2.273317134858E18") // Java serializes it to "2.27331713485799987E18" (5 digits too much)
      check(7.68905065813E17, "7.68905065813E17") // Java serializes it to "7.6890506581299994E17" (5 digits too much)
      check(1.9400994884341945E25, "1.9400994884341945E25") // Java serializes it to "1.9400994884341944E25" (not the closest to the intermediate double)
      check(3.6131332396758635E25, "3.6131332396758635E25") // Java serializes it to "3.6131332396758634E25" (not the closest to the intermediate double)
      check(2.5138990223946153E25, "2.5138990223946153E25") // Java serializes it to "2.5138990223946152E25" (not the closest to the intermediate double)
    }
    "write round-even double values" in {
      def check(n: Double, s: String): Unit = {
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(1.8557466319180092E15, "1.8557466319180092E15")
      check(2.1454965803968662E14, "2.1454965803968662E14")
      check(5.724294694832342E14, "5.724294694832342E14")
    }
    "throw i/o exception on non-finite numbers" in {
      forAll(genNonFiniteDouble) { n =>
        assert(intercept[JsonWriterException](withWriter(_.writeVal(n))).getMessage.contains("illegal number"))
        assert(intercept[JsonWriterException](withWriter(_.writeValAsString(n))).getMessage.contains("illegal number"))
        assert(intercept[JsonWriterException](withWriter(_.writeKey(n))).getMessage.contains("illegal number"))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for BigInt" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[BigInt])))
      intercept[NullPointerException](withWriter(_.writeValAsString(null.asInstanceOf[BigInt])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[BigInt])))
    }
    "write number values" in {
      def check(n: BigInt): Unit = {
        val s = new java.math.BigDecimal(n.bigInteger).toPlainString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      forAll(arbitrary[Long], minSuccessful(10000))(n => check(BigInt(n)))
      forAll(genBigInt, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for BigDecimal" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[BigDecimal])))
      intercept[NullPointerException](withWriter(_.writeValAsString(null.asInstanceOf[BigDecimal])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[BigDecimal])))
    }
    "write number values" in {
      def check(n: BigDecimal): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe s""""$s""""
        withWriter(_.writeKey(n)) shouldBe s""""$s":"""
      }

      check(BigDecimal("1E-2147483647"))
      check(BigDecimal("1E+2147483647"))
      forAll(genBigInt, minSuccessful(100))(n => check(BigDecimal(n, -2147483648)))
      forAll(arbitrary[Double], minSuccessful(10000))(n => check(BigDecimal(n)))
      forAll(genBigDecimal, minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeBase16Val" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeBase16Val(null.asInstanceOf[Array[Byte]], lowerCase = true)))
      intercept[NullPointerException](withWriter(_.writeBase16Val(null.asInstanceOf[Array[Byte]], lowerCase = false)))
    }
    "write bytes as Base16 string according to format that defined in RFC4648" in {
      def check(s: String): Unit = {
        val bs = s.getBytes
        val base16LowerCase = bs.map(TestUtils.toHex).mkString("\"", "", "\"")
        val base16UpperCase = base16LowerCase.toUpperCase
        withWriter(_.writeBase16Val(bs, lowerCase = true)) shouldBe base16LowerCase
        withWriter(_.writeBase16Val(bs, lowerCase = false)) shouldBe base16UpperCase
      }

      forAll(arbitrary[String], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeBase64Val and JsonWriter.writeBase64UrlVal" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeBase64Val(null.asInstanceOf[Array[Byte]], doPadding = true)))
      intercept[NullPointerException](withWriter(_.writeBase64UrlVal(null.asInstanceOf[Array[Byte]], doPadding = true)))
      intercept[NullPointerException](withWriter(_.writeBase64Val(null.asInstanceOf[Array[Byte]], doPadding = false)))
      intercept[NullPointerException](withWriter(_.writeBase64UrlVal(null.asInstanceOf[Array[Byte]], doPadding = false)))
    }
    "write bytes as Base64 string according to format that defined in RFC4648" in {
      def check(s: String): Unit = {
        val bs = s.getBytes(UTF_8)
        withWriter(_.writeBase64Val(bs, doPadding = true)) shouldBe "\"" + Base64.getEncoder.encodeToString(bs) + "\""
        withWriter(_.writeBase64UrlVal(bs, doPadding = true)) shouldBe "\"" + Base64.getUrlEncoder.encodeToString(bs) + "\""
        withWriter(_.writeBase64Val(bs, doPadding = false)) shouldBe "\"" + Base64.getEncoder.withoutPadding.encodeToString(bs) + "\""
        withWriter(_.writeBase64UrlVal(bs, doPadding = false)) shouldBe "\"" + Base64.getUrlEncoder.withoutPadding.encodeToString(bs) + "\""
      }

      forAll(arbitrary[String], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeRawVal" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeRawVal(null.asInstanceOf[Array[Byte]])))
    }
    "write raw bytes as is" in {
      def check(s: String): Unit = withWriter(_.writeRawVal(s.getBytes(UTF_8))) shouldBe s

      forAll(arbitrary[String], minSuccessful(10000))(check)
    }
  }
  "JsonWriter.writeNull" should {
    "write null value" in {
      withWriter(_.writeNull()) shouldBe "null"
    }
  }
  "JsonWriter.writeArrayStart and JsonWriter.writeArrayEnd" should {
    "allow to write an empty JSON array" in {
      withWriter { w =>
        w.writeArrayStart()
        w.writeArrayEnd()
      } shouldBe "[]"
    }
    "allow to write a compact JSON array with values separated by comma" in {
      withWriter { w =>
        w.writeArrayStart()
        w.writeVal(1)
        w.writeVal("VVV")
        w.writeValAsString(2L)
        w.writeValAsString(true)
        w.writeRawVal(Array[Byte](51))
        w.writeArrayEnd()
      } shouldBe "[1,\"VVV\",\"2\",\"true\",3]"
    }
    "allow to write a prettified JSON array with values separated by comma" in {
      withWriter(WriterConfig.withIndentionStep(2)) { w =>
        w.writeArrayStart()
        w.writeVal(1)
        w.writeVal("VVV")
        w.writeNonEscapedAsciiVal("WWW")
        w.writeValAsString(2L)
        w.writeValAsString(true)
        w.writeRawVal(Array[Byte](51))
        w.writeArrayEnd()
      } shouldBe
        """[
           |  1,
           |  "VVV",
           |  "WWW",
           |  "2",
           |  "true",
           |  3
           |]""".stripMargin
    }
  }
  "JsonWriter.writeObjectStart and JsonWriter.writeObjectEnd" should {
    "allow to write an empty JSON object" in {
      withWriter { w =>
        w.writeObjectStart()
        w.writeObjectEnd()
      } shouldBe "{}"
    }
    "allow to write a compact JSON array with key/value pairs separated by comma" in {
      withWriter { w =>
        w.writeObjectStart()
        w.writeKey(1)
        w.writeVal("VVV")
        w.writeKey("true")
        w.writeNonEscapedAsciiVal("WWW")
        w.writeNonEscapedAsciiKey("2")
        w.writeRawVal(Array[Byte](51))
        w.writeObjectEnd()
      } shouldBe "{\"1\":\"VVV\",\"true\":\"WWW\",\"2\":3}"
    }
    "allow to write a prettified JSON array with key/value pairs separated by comma" in {
      withWriter(WriterConfig.withIndentionStep(2)) { w =>
        w.writeObjectStart()
        w.writeKey(1)
        w.writeVal("VVV")
        w.writeKey("true")
        w.writeNonEscapedAsciiVal("WWW")
        w.writeNonEscapedAsciiKey("2")
        w.writeRawVal(Array[Byte](51))
        w.writeObjectEnd()
      } shouldBe
        """{
          |  "1": "VVV",
          |  "true": "WWW",
          |  "2": 3
          |}""".stripMargin
    }
  }

  def withWriter(f: JsonWriter => Unit): String =
    withWriter(WriterConfig.withPreferredBufSize(1).withThrowWriterExceptionWithStackTrace(true))(f)

  def withWriter(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val writer = new JsonWriter(new Array[Byte](Random.nextInt(16)), 0, 0, 0, false, false, null, null, cfg)
    new String(writer.write(new JsonValueCodec[String] {
      override def decodeValue(in: JsonReader, default: String): String = ""

      override def encodeValue(x: String, out: JsonWriter): Unit = f(writer)

      override val nullValue: String = ""
    }, "", cfg), "UTF-8")
  }
}