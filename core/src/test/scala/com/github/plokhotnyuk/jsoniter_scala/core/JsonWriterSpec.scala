package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayOutputStream, IOException}
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class JsonWriterSpec extends WordSpec with Matchers with PropertyChecks {
  "WriterConfig.<init>" should {
    "have handy defaults" in {
      WriterConfig().indentionStep shouldBe 0
      WriterConfig().escapeUnicode shouldBe false
      WriterConfig().preferredBufSize shouldBe 16384
    }
    "throw exception in case for unsupported values of params" in {
      assert(intercept[IllegalArgumentException](WriterConfig(indentionStep = -1))
        .getMessage.contains("'indentionStep' should be not less than 0"))
      assert(intercept[IllegalArgumentException](WriterConfig(preferredBufSize = -1))
        .getMessage.contains("'preferredBufSize' should be not less than 0"))
    }
  }
  "JsonWriter.isNonEscapedAscii" should {
    "return false for all escaped ASCII or non-ASCII chars" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        JsonWriter.isNonEscapedAscii(ch) shouldBe !isEscapedAscii(ch) && ch < 128
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and and JsonWriter.writeKey for boolean" should {
    "write valid true and false values" in {
      def check(value: Boolean, excpectedOut: String): Unit = {
        val s = value.toString
        withWriter(_.writeVal(value)) shouldBe s
        withWriter(_.writeValAsString(value)) shouldBe '\"' + s + '\"'
        withWriter(_.writeKey(value)) shouldBe '\"' + s + "\":"
        withWriter(WriterConfig(indentionStep = 2))(_.writeKey(value)) shouldBe '\"' + s + "\": "
      }

      check(value = true, "true")
      check(value = false, "false")
    }
  }
  "JsonWriter.writeNonEscapedAsciiVal and JsonWriter.writeNonEscapedAsciiKey" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeNonEscapedAsciiVal(null.asInstanceOf[String])))
      intercept[NullPointerException](withWriter(_.writeNonEscapedAsciiKey(null.asInstanceOf[String])))
    }
    "write string of Ascii chars which should not be escaped" in {
      def check(s: String): Unit = {
        withWriter(_.writeNonEscapedAsciiVal(s)) shouldBe '"' + s + '"'
        withWriter(_.writeNonEscapedAsciiKey(s)) shouldBe '"' + s + "\":"
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      forAll(Gen.uuid, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Duration" should {
    "write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Duration])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Duration])))
    }
    "write Duration as a string representation according to ISO-8601 format" in {
      def check(x: Duration, s: String): Unit = {
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(Duration.ZERO, "PT0S")
      check(Duration.ofSeconds(-60, -1), "PT-1M-0.000000001S")
      check(Duration.ofSeconds(-60, 1), "PT-59.999999999S")
      check(Duration.ofSeconds(-60, 20000000000L), "PT-40S")
      forAll(genDuration, minSuccessful(100000))(x => check(x, x.toString))
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(Instant.MAX)
      check(Instant.MIN)
      forAll(genInstant, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(LocalDate.MAX)
      check(LocalDate.MIN)
      forAll(genLocalDate, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(LocalDateTime.MAX)
      check(LocalDateTime.MIN)
      forAll(genLocalDateTime, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(LocalTime.MAX)
      check(LocalTime.MIN)
      forAll(genLocalTime, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(MonthDay.of(12, 31))
      check(MonthDay.of(1, 1))
      forAll(genMonthDay, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(OffsetDateTime.MAX)
      check(OffsetDateTime.MIN)
      forAll(genOffsetDateTime, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(OffsetTime.MAX)
      check(OffsetTime.MIN)
      forAll(genOffsetTime, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(Period.ZERO)
      forAll(genPeriod, minSuccessful(100000))(check)
      forAll(Gen.choose(Int.MinValue, Int.MaxValue), Gen.choose(Int.MinValue, Int.MaxValue), minSuccessful(100000)) {
        (x: Int, y: Int) => check(Period.of(x, y, 0))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for Year" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[Year])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[Year])))
    }
    "write Year as a string representation according to ISO-8601 format" in {
      def check(x: Year): Unit = {
        // '+' is required for years that extends 4 digits, see ISO 8601:2004 sections 3.4.2, 4.1.2.4
        val s = // FIXME: It looks like a bug in JDK that Year.toString doesn't serialize years > 9999 with the '+' prefix
          if (x.getValue > 0) (if (x.getValue > 9999) "+" else "") + f"${x.getValue}%04d"
          else f"-${-x.getValue}%04d"
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(Year.of(Year.MAX_VALUE))
      check(Year.of(Year.MIN_VALUE))
      forAll(genYear, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for YearMonth" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[YearMonth])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[YearMonth])))
    }
    "write YearMonth as a string representation according to ISO-8601 format" in {
      def check(x: YearMonth): Unit = {
        // '+' is required for years that extends 4 digits, see ISO 8601:2004 sections 3.4.2, 4.1.2.4
        val s = (if (x.getYear >= 10000) "+" else "") + x.toString
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(YearMonth.of(Year.MAX_VALUE, 12))
      check(YearMonth.of(Year.MIN_VALUE, 1))
      forAll(genYearMonth, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for ZonedDateTime" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[ZonedDateTime])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[ZonedDateTime])))
    }
    "write ZonedDateTime as a string representation according to ISO-8601 format with optional IANA time zone identifier in JDK 8+ format" in {
      def check(x: ZonedDateTime): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MAX))
      check(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MIN))
      forAll(genZonedDateTime, minSuccessful(100000))(check)
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
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneOffset, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for ZoneId" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[ZoneId])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[ZoneId])))
    }
    "write ZoneId as a string representation according to ISO-8601 format for zone offset or JDK 8+ format for IANA time zone identifier" in {
      def check(x: ZoneId): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      check(ZoneOffset.MAX)
      check(ZoneOffset.MIN)
      forAll(genZoneId, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for string" should {
    "don't write null value" in {
      intercept[NullPointerException](withWriter(_.writeVal(null.asInstanceOf[String])))
      intercept[NullPointerException](withWriter(_.writeKey(null.asInstanceOf[String])))
    }
    "write string of Unicode chars which are non-surrogate and should not be escaped" in {
      def check(s: String): Unit = {
        withWriter(_.writeVal(s)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(s)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (s: String) =>
        whenever(s.forall(ch => !Character.isSurrogate(ch) && !isEscapedAscii(ch))) {
          check(s)
        }
      }
    }
    "write strings with chars that should be escaped" in {
      def check(s: String, escapeUnicode: Boolean): Unit = {
        withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeVal(s)) shouldBe
          "\"" + s.flatMap(toEscaped) + "\""
        withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeKey(s)) shouldBe
          "\"" + s.flatMap(toEscaped) + "\":"
      }

      forAll(Gen.listOf(genEscapedAsciiChar).map(_.mkString), Gen.oneOf(true, false), minSuccessful(10000)) {
        (s: String, escapeUnicode: Boolean) =>
          check(s, escapeUnicode)
      }
    }
    "write strings with escaped Unicode chars if it is specified by provided writer config" in {
      def check(s: String): Unit = {
        withWriter(WriterConfig(escapeUnicode = true))(_.writeVal(s)) shouldBe "\"" + s.flatMap(toEscaped) + "\""
        withWriter(WriterConfig(escapeUnicode = true))(_.writeKey(s)) shouldBe "\"" + s.flatMap(toEscaped) + "\":"
      }

      forAll(minSuccessful(100000)) { (s: String) =>
        whenever(s.forall(ch => isEscapedAscii(ch) || ch >= 128)) {
          check(s)
        }
      }
    }
    "write strings with valid character surrogate pair" in {
      def check(s: String): Unit = {
        withWriter(WriterConfig(escapeUnicode = false))(_.writeVal(s)) shouldBe "\"" + s + "\""
        withWriter(WriterConfig(escapeUnicode = false))(_.writeKey(s)) shouldBe "\"" + s + "\":"
        withWriter(WriterConfig(escapeUnicode = true))(_.writeVal(s)) shouldBe "\"" + s.flatMap(toEscaped) + "\""
        withWriter(WriterConfig(escapeUnicode = true))(_.writeKey(s)) shouldBe "\"" + s.flatMap(toEscaped) + "\":"
      }

      forAll(genHighSurrogateChar, genLowSurrogateChar, minSuccessful(10000)) { (ch1: Char, ch2: Char) =>
        check(ch1.toString + ch2.toString)
      }
    }
    "throw i/o exception in case of illegal character surrogate pair" in {
      def check(s: String, escapeUnicode: Boolean): Unit = {
        assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeVal(s)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
        assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeKey(s)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
      }

      forAll(genSurrogateChar, Gen.oneOf(true, false), minSuccessful(10000)) { (ch: Char, escapeUnicode: Boolean) =>
        check(ch.toString, escapeUnicode)
        check(ch.toString + ch.toString, escapeUnicode)
      }
      forAll(genLowSurrogateChar, genHighSurrogateChar, Gen.oneOf(true, false), minSuccessful(10000)) {
        (ch1: Char, ch2: Char, escapeUnicode: Boolean) =>
          check(ch1.toString + ch2.toString, escapeUnicode)
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for char" should {
    "write string with Unicode chars which are non-surrogate or should not be escaped" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(!Character.isSurrogate(ch) && !isEscapedAscii(ch)) {
          withWriter(_.writeVal(ch)) shouldBe "\"" + ch + "\""
          withWriter(_.writeKey(ch)) shouldBe "\"" + ch + "\":"
        }
      }
    }
    "write string with chars that should be escaped" in {
      forAll(genEscapedAsciiChar, minSuccessful(10000)) { (ch: Char) =>
        withWriter(_.writeVal(ch)) shouldBe "\"" + toEscaped(ch) + "\""
        withWriter(_.writeKey(ch)) shouldBe "\"" + toEscaped(ch) + "\":"
      }
    }
    "write string with escaped Unicode chars if it is specified by provided writer config" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        whenever(isEscapedAscii(ch) || ch >= 128) {
          withWriter(WriterConfig(escapeUnicode = true))(_.writeVal(ch)) shouldBe "\"" + toEscaped(ch) + "\""
          withWriter(WriterConfig(escapeUnicode = true))(_.writeKey(ch)) shouldBe "\"" + toEscaped(ch) + "\":"
        }
      }
    }
    "throw i/o exception in case of surrogate pair character" in {
      forAll(genSurrogateChar, Gen.oneOf(true, false)) { (ch: Char, escapeUnicode: Boolean) =>
        assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeVal(ch)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
        assert(intercept[IOException](withWriter(WriterConfig(escapeUnicode = escapeUnicode))(_.writeKey(ch)))
          .getMessage.contains("illegal char sequence of surrogate pair"))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for byte" should {
    "write any short values" in {
      forAll(minSuccessful(1000)) { (n: Byte) =>
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for short" should {
    "write any short values" in {
      forAll(minSuccessful(10000)) { (n: Short) =>
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for int" should {
    "write any int values" in {
      def check(n: Int): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: Int) =>
        check(n)
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for long" should {
    "write any long values" in {
      def check(n: Long): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: Int) =>
        check(n.toLong)
      }
      forAll(minSuccessful(100000)) { (n: Long) =>
        check(n)
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for float" should {
    "write finite float values" in {
      def check(n: Float): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: Float) =>
        whenever(java.lang.Float.isFinite(n)) {
          check(n)
        }
      }
    }
    "throw i/o exception on non-finite numbers" in {
      forAll(Gen.oneOf(Float.NaN, Float.PositiveInfinity, Float.NegativeInfinity)) { (n: Float) =>
        assert(intercept[IOException](withWriter(_.writeVal(n))).getMessage.contains("illegal number"))
        assert(intercept[IOException](withWriter(_.writeValAsString(n))).getMessage.contains("illegal number"))
        assert(intercept[IOException](withWriter(_.writeKey(n))).getMessage.contains("illegal number"))
      }
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for double" should {
    "write finite double values" in {
      def check(n: Double): Unit = {
        val s = n.toString
        withWriter(_.writeVal(n)) shouldBe s
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: Double) =>
        whenever(java.lang.Double.isFinite(n)) {
          check(n)
        }
      }
    }
    "throw i/o exception on non-finite numbers" in {
      forAll(Gen.oneOf(Double.NaN, Double.PositiveInfinity, Double.NegativeInfinity)) { (n: Double) =>
        assert(intercept[IOException](withWriter(_.writeVal(n))).getMessage.contains("illegal number"))
        assert(intercept[IOException](withWriter(_.writeValAsString(n))).getMessage.contains("illegal number"))
        assert(intercept[IOException](withWriter(_.writeKey(n))).getMessage.contains("illegal number"))
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
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: Long) =>
        check(BigInt(n))
      }
      forAll(minSuccessful(100000)) { (n: BigInt) =>
        check(n)
      }
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
        withWriter(_.writeValAsString(n)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(n)) shouldBe '"' + s + "\":"
      }

      forAll(minSuccessful(100000)) { (n: BigDecimal) =>
        check(n)
      }
    }
  }

  def withWriter(f: JsonWriter => Unit): String = withWriter(WriterConfig(preferredBufSize = 0))(f)

  def withWriter(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val out = new ByteArrayOutputStream(256)
    val writer = new JsonWriter(new Array[Byte](0), 0, 0, false, true, out, cfg)
    try f(writer)
    finally writer.flushBuf()
    out.toString("UTF-8")
  }

  def isEscapedAscii(ch: Char): Boolean = ch < ' ' || ch == '\\' || ch == '"' || ch == '\u007f'

  def toEscaped(ch: Char): String = ch match {
    case '"' => """\""""
    case '\\' => """\\"""
    case '\b' => """\b"""
    case '\f' => """\f"""
    case '\n' => """\n"""
    case '\r' => """\r"""
    case '\t' => """\t"""
    case _ => toHexEscaped(ch)
  }

  def toHexEscaped(ch: Char): String = f"\\u$ch%04x"
}