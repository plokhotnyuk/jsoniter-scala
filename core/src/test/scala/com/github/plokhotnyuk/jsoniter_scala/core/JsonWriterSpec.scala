package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.GenUtils._
import com.github.plokhotnyuk.jsoniter_scala.core.UserAPI._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class JsonWriterSpec extends WordSpec with Matchers with PropertyChecks {
  val buf = new Array[Byte](150)
  "JsonWriter.isNonEscapedAscii" should {
    "return false for all escaped ASCII or non-ASCII chars" in {
      forAll(minSuccessful(10000)) { (ch: Char) =>
        JsonWriter.isNonEscapedAscii(ch) shouldBe !isEscapedAscii(ch) && ch < 128
      }
    }
  }
  "JsonWriter.write" should {
    "serialize an object to the provided output stream" in {
      val out1 = new ByteArrayOutputStream()
      JsonWriter.write(codec, user, out1)
      out1.toString("UTF-8") shouldBe toString(compactJson)
      val out2 = new ByteArrayOutputStream()
      JsonWriter.write(codec, user, out2, WriterConfig(indentionStep = 2))
      out2.toString("UTF-8") shouldBe toString(prettyJson)
    }
    "serialize an object to a new instance of byte array" in {
      toString(JsonWriter.write(codec, user)) shouldBe toString(compactJson)
      toString(JsonWriter.write(codec, user, WriterConfig(indentionStep = 2))) shouldBe toString(prettyJson)
    }
    "serialize an object to the provided byte array from specified position" in {
      val from1 = 10
      val to1 = JsonWriter.write(codec, user, buf, from1)
      new String(buf, from1, to1 - from1, UTF_8) shouldBe toString(compactJson)
      val from2 = 0
      val to2 = JsonWriter.write(codec, user, buf, from2, WriterConfig(indentionStep = 2))
      new String(buf, from2, to2 - from2, UTF_8) shouldBe toString(prettyJson)
    }
    "throw array index out of bounds exception in case of the provided byte array is overflown during serialization" in {
      assert(intercept[ArrayIndexOutOfBoundsException](JsonWriter.write(codec, user, buf, 100))
        .getMessage.contains("`buf` length exceeded"))
    }
    "throw i/o exception in case of the provided params are invalid" in {
      intercept[NullPointerException](JsonWriter.write(null, user))
      intercept[NullPointerException](JsonWriter.write(null, user, new ByteArrayOutputStream()))
      intercept[NullPointerException](JsonWriter.write(null, user, buf, 0))
      intercept[NullPointerException](JsonWriter.write(codec, user, null.asInstanceOf[OutputStream]))
      intercept[NullPointerException](JsonWriter.write(codec, user, null, 50))
      intercept[NullPointerException](JsonWriter.write(codec, user, null.asInstanceOf[WriterConfig]))
      intercept[NullPointerException](JsonWriter.write(codec, user, new ByteArrayOutputStream(), null))
      intercept[NullPointerException](JsonWriter.write(codec, user, buf, 0, null))
      assert(intercept[ArrayIndexOutOfBoundsException](JsonWriter.write(codec, user, new Array[Byte](10), 50))
        .getMessage.contains("`from` should be positive and not greater than `buf` length"))
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
    "write null value" in {
      withWriter(_.writeNonEscapedAsciiVal(null.asInstanceOf[String])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeNonEscapedAsciiKey(null.asInstanceOf[String])))
        .getMessage.contains("key cannot be null"))
    }
    "write string of Ascii chars which should not be escaped" in {
      def check(s: String): Unit = {
        withWriter(_.writeNonEscapedAsciiVal(s)) shouldBe '"' + s + '"'
        withWriter(_.writeNonEscapedAsciiKey(s)) shouldBe '"' + s + "\":"
      }

      forAll(Gen.listOf(genAsciiChar).map(_.mkString.filter(JsonWriter.isNonEscapedAscii)))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for UUID" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[UUID])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[UUID])))
        .getMessage.contains("key cannot be null"))
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
  "JsonWriter.writeVal and JsonWriter.writeKey for Instant" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[Instant])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[Instant])))
        .getMessage.contains("key cannot be null"))
    }
    "write Instant as a string representation according to ISO-8601 format" in {
      def check(x: Instant): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      //FIXME add serialization of min/max values
      //check(Instant.MAX)
      //check(Instant.MIN)
      forAll(genInstant, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for LocalDate" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[LocalDate])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[LocalDate])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[LocalDateTime])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[LocalDateTime])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[LocalTime])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[LocalTime])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[MonthDay])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[MonthDay])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[OffsetDateTime])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[OffsetDateTime])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[OffsetTime])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[OffsetTime])))
        .getMessage.contains("key cannot be null"))
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
  "JsonWriter.writeVal and JsonWriter.writeValAsString and JsonWriter.writeKey for Year" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[Year])) shouldBe "null"
      withWriter(_.writeValAsString(null.asInstanceOf[Year])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[Year])))
        .getMessage.contains("key cannot be null"))
    }
    "write number values" in {
      def check(x: Year): Unit = {
        val s = x.toString
        withWriter(_.writeVal(x)) shouldBe s
        withWriter(_.writeValAsString(x)) shouldBe '"' + s + '"'
        withWriter(_.writeKey(x)) shouldBe '"' + s + "\":"
      }

      forAll(genYear, minSuccessful(100000))(check)
    }
  }
  "JsonWriter.writeVal and JsonWriter.writeKey for YearMonth" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[YearMonth])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[YearMonth])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[ZonedDateTime])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[ZonedDateTime])))
        .getMessage.contains("key cannot be null"))
    }
    "write ZonedDateTime as a string representation according to ISO-8601 format" in {
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
  "JsonWriter.writeVal and JsonWriter.writeKey for string" should {
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[String])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[String])))
        .getMessage.contains("key cannot be null"))
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

      forAll(Gen.listOf(genEscapedAsciiChar).map(_.mkString), Gen.oneOf(true, false)) {
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

      forAll(genHighSurrogateChar, genLowSurrogateChar) { (ch1: Char, ch2: Char) =>
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

      forAll(genSurrogateChar, Gen.oneOf(true, false)) { (ch: Char, escapeUnicode: Boolean) =>
        check(ch.toString, escapeUnicode)
        check(ch.toString + ch.toString, escapeUnicode)
      }
      forAll(genLowSurrogateChar, genHighSurrogateChar, Gen.oneOf(true, false)) {
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
      forAll(genEscapedAsciiChar) { (ch: Char) =>
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[BigInt])) shouldBe "null"
      withWriter(_.writeValAsString(null.asInstanceOf[BigInt])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[BigInt])))
        .getMessage.contains("key cannot be null"))
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
    "write null value" in {
      withWriter(_.writeVal(null.asInstanceOf[BigDecimal])) shouldBe "null"
      withWriter(_.writeValAsString(null.asInstanceOf[BigDecimal])) shouldBe "null"
      assert(intercept[IOException](withWriter(_.writeKey(null.asInstanceOf[BigDecimal])))
        .getMessage.contains("key cannot be null"))
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

  def withWriter(f: JsonWriter => Unit): String = withWriter(WriterConfig())(f)

  def withWriter(cfg: WriterConfig)(f: JsonWriter => Unit): String = {
    val out = new ByteArrayOutputStream(256)
    val writer = new JsonWriter(new Array[Byte](0), 0, 0, false, true, out, cfg)
    try f(writer)
    finally writer.flushBuffer()
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

  def toString(json: Array[Byte]): String = new String(json, 0, json.length, UTF_8)
}