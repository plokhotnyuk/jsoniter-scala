package com.github.plokhotnyuk.jsoniter_scala.core

import java.math.MathContext
import java.math.MathContext._
import java.math.RoundingMode._
import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.UUID

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen

import scala.jdk.CollectionConverters._
import scala.util.{Random, Try}

object GenUtils {
  val whitespaces: Seq[String] = (0 to 99).map {
    val ws = Array(' '.toByte, '\n'.toByte, '\t'.toByte, '\r'.toByte)
    size =>
      val bs = new Array[Byte](size)
      java.util.Arrays.fill(bs, ws(Random.nextInt(ws.length)))
      new String(bs, 0, bs.length, UTF_8)
  }
  val genHighSurrogateChar: Gen[Char] = Gen.choose('\ud800', '\udbff')
  val genLowSurrogateChar: Gen[Char] = Gen.choose('\udc00', '\udfff')
  val genSurrogateChar: Gen[Char] = Gen.oneOf(genHighSurrogateChar, genLowSurrogateChar)
  val genAsciiChar: Gen[Char] = Gen.choose('\u0000', '\u007f')
  val genControlChar: Gen[Char] = Gen.choose('\u0000', '\u001f')
  val genMustBeEscapedAsciiChar: Gen[Char] = Gen.oneOf(genControlChar, Gen.oneOf('\\', '"'))
  val genEscapedAsciiChar: Gen[Char] = Gen.oneOf(genMustBeEscapedAsciiChar, Gen.const('\u007f'))
  val genNonAsciiChar: Gen[Char] = Gen.choose('\u0100', '\uffff')
  val genWhitespaces: Gen[String] = Gen.choose(0, 99).map(whitespaces)
  val genSize: Gen[Int] = Gen.frequency((9, Gen.choose(1, 10)), (3, Gen.choose(1, 100)), (1, Gen.choose(1, 1000)))
  val genMathContext: Gen[MathContext] = for {
    precision <- genSize
    rounding <- Gen.oneOf(CEILING, DOWN, FLOOR, HALF_DOWN, HALF_EVEN, HALF_UP, UNNECESSARY, UP)
  } yield new MathContext(precision, rounding)
  val genBigInt: Gen[BigInt] = for {
    size <- genSize
    digits <- Gen.containerOfN[Array, Byte](size, arbitrary[Byte])
  } yield BigInt(digits)
  val genBigDecimal: Gen[BigDecimal] = for {
    unscaled <- genBigInt
    posScale <- genSize
    isPositive <- arbitrary[Boolean]
    scale = if (isPositive) posScale else -posScale
    mc <- genMathContext
  } yield Try(BigDecimal(unscaled, scale, mc)).getOrElse(BigDecimal(unscaled, scale, UNLIMITED))
  val genZoneOffset: Gen[ZoneOffset] = Gen.oneOf(
    Gen.choose(-18, 18).map(ZoneOffset.ofHours),
    Gen.choose(-18 * 60, 18 * 60).map(x => ZoneOffset.ofHoursMinutes(x / 60, x % 60)),
    Gen.choose(-18 * 60 * 60, 18 * 60 * 60).map(ZoneOffset.ofTotalSeconds))
  val genDuration: Gen[Duration] = Gen.oneOf(
    Gen.choose(Long.MinValue / 86400, Long.MaxValue / 86400).map(Duration.ofDays),
    Gen.choose(Long.MinValue / 3600, Long.MaxValue / 3600).map(Duration.ofHours),
    Gen.choose(Long.MinValue / 60, Long.MaxValue / 60).map(Duration.ofMinutes),
    // FIXME: JDK 8 has bug in parsing and serialization of Duration with zero seconds and negative nanos,
    // see https://bugs.openjdk.java.net/browse/JDK-8054978
    Gen.choose(Long.MinValue, Long.MaxValue).map(Duration.ofSeconds),
    Gen.choose(if (TestUtils.isJDK8) 0L else Int.MinValue, Int.MaxValue.toLong).map(Duration.ofMillis),
    Gen.choose(if (TestUtils.isJDK8) 0L else Int.MinValue, Int.MaxValue.toLong).map(Duration.ofNanos))
  val genInstant: Gen[Instant] = for {
    epochSecond <- Gen.choose(Instant.MIN.getEpochSecond, Instant.MAX.getEpochSecond)
    nanoAdjustment <- Gen.choose(Long.MinValue, Long.MaxValue)
    fallbackInstant <- Gen.oneOf(Instant.MIN, Instant.EPOCH, Instant.MAX)
  } yield Try(Instant.ofEpochSecond(epochSecond, nanoAdjustment)).getOrElse(fallbackInstant)
  val genYear: Gen[Year] =
    Gen.frequency((3, Gen.choose(-9999, 9999)), (1, Gen.choose(-999999999, 999999999))).map(Year.of)
  val genLocalDate: Gen[LocalDate] = for {
    year <- genYear
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, Month.of(month).length(year.isLeap))
  } yield LocalDate.of(year.getValue, month, day)
  val genLocalTime: Gen[LocalTime] = for {
    hour <- Gen.choose(0, 23)
    minute <- Gen.choose(0, 59)
    second <- Gen.choose(0, 59)
    nano <- Gen.choose(0, 999999999)
  } yield LocalTime.of(hour, minute, second, nano)
  val genLocalDateTime: Gen[LocalDateTime] = for {
    localDate <- genLocalDate
    localTime <- genLocalTime
  } yield LocalDateTime.of(localDate, localTime)
  val genMonthDay: Gen[MonthDay] = for {
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, 29)
  } yield MonthDay.of(month, day)
  val genOffsetDateTime: Gen[OffsetDateTime] = for {
    localDateTime <- genLocalDateTime
    zoneOffset <- genZoneOffset
  } yield OffsetDateTime.of(localDateTime, zoneOffset)
  val genOffsetTime: Gen[OffsetTime] = for {
    localTime <- genLocalTime
    zoneOffset <- genZoneOffset
  } yield OffsetTime.of(localTime, zoneOffset)
  val genPeriod: Gen[Period] = for {
    year <- arbitrary[Int]
    month <- arbitrary[Int]
    day <- arbitrary[Int]
  } yield Period.of(year, month, day)
  val genYearMonth: Gen[YearMonth] = for {
    year <- genYear
    month <- Gen.choose(1, 12)
  } yield YearMonth.of(year.getValue, month)
  val genZoneId: Gen[ZoneId] = Gen.oneOf(
    genZoneOffset,
    genZoneOffset.map(zo => ZoneId.ofOffset("UT", zo)),
    genZoneOffset.map(zo => ZoneId.ofOffset("UTC", zo)),
    genZoneOffset.map(zo => ZoneId.ofOffset("GMT", zo)),
    Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toSeq).map(ZoneId.of),
    Gen.oneOf(ZoneId.SHORT_IDS.values().asScala.toSeq).map(ZoneId.of))
  val genZonedDateTime: Gen[ZonedDateTime] = for {
    localDateTime <- genLocalDateTime
    zoneId <- genZoneId
  } yield ZonedDateTime.of(localDateTime, zoneId)
  val genUUID: Gen[UUID] = for {
    msb <- arbitrary[Long]
    lsb <- arbitrary[Long]
  } yield new UUID(msb, lsb)
  val genFiniteDouble: Gen[Double] = arbitrary[Double].filter(java.lang.Double.isFinite)
  val genFiniteFloat: Gen[Float] = arbitrary[Float].filter(java.lang.Float.isFinite)
  val genNonFiniteDouble: Gen[Double] = Gen.oneOf(
    Gen.oneOf(java.lang.Double.NaN, java.lang.Double.NEGATIVE_INFINITY, java.lang.Double.POSITIVE_INFINITY),
    Gen.choose(0, 0x0007FFFFFFFFFFFFL).map(x => java.lang.Double.longBitsToDouble(x | 0x7FF8000000000000L))) // Double.NaN with error code
  val genNonFiniteFloat: Gen[Float] = Gen.oneOf(
    Gen.oneOf(java.lang.Float.NaN, java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.POSITIVE_INFINITY),
    Gen.choose(0, 0x003FFFFF).map(x => java.lang.Float.intBitsToFloat(x | 0x7FC00000))) // Float.NaN with error code

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