package com.github.plokhotnyuk.jsoniter_scala.core

import java.math.MathContext
import java.math.MathContext._
import java.math.RoundingMode._
import java.time._

import org.scalacheck.{Arbitrary, Gen}

import scala.collection.JavaConverters._
import scala.util.Try

object GenUtils {
  val isJDK8: Boolean = System.getProperty("java.version").startsWith("1.8")
  val genHighSurrogateChar: Gen[Char] = Gen.choose('\ud800', '\udbff')
  val genLowSurrogateChar: Gen[Char] = Gen.choose('\udc00', '\udfff')
  val genSurrogateChar: Gen[Char] = Gen.oneOf(genHighSurrogateChar, genLowSurrogateChar)
  val genAsciiChar: Gen[Char] = Gen.choose('\u0000', '\u007f')
  val genControlChar: Gen[Char] = Gen.choose('\u0000', '\u001f')
  val genMustBeEscapedAsciiChar: Gen[Char] = Gen.oneOf(genControlChar, Gen.oneOf('\\', '"'))
  val genEscapedAsciiChar: Gen[Char] = Gen.oneOf(genMustBeEscapedAsciiChar, Gen.const('\u007f'))
  val genNonAsciiChar: Gen[Char] = Gen.choose('\u0100', '\uffff')
  val genMathContext: Gen[MathContext] = for {
    precision <- Gen.choose(0, 10000)
    rounding <- Gen.oneOf(CEILING, DOWN, FLOOR, HALF_DOWN, HALF_EVEN, HALF_UP, UNNECESSARY, UP)
  } yield new MathContext(precision, rounding)
  val genBigInt: Gen[BigInt] = Gen.frequency(
    (100, Arbitrary.arbitrary[BigInt]),
    (1, for {
      size <- Gen.choose(1, 10000)
      digits <- Gen.containerOfN[Array, Byte](size, Arbitrary.arbByte.arbitrary)
    } yield BigInt(digits)))
  val genBigDecimal: Gen[BigDecimal] = Gen.frequency(
    (100, Arbitrary.arbitrary[BigDecimal]),
    (1, for {
      size <- Gen.choose(1, 10000)
      digits <- Gen.containerOfN[Array, Byte](size, Arbitrary.arbByte.arbitrary)
      scale <- Gen.choose(-10000, 10000)
      mc <- genMathContext
    } yield Try(BigDecimal(BigInt(digits), scale, mc)).getOrElse(BigDecimal(BigInt(digits), scale, UNLIMITED))))
  val genZoneOffset: Gen[ZoneOffset] = Gen.oneOf(
    Gen.choose(-18, 18).map(ZoneOffset.ofHours),
    Gen.choose(-18 * 60, 18 * 60).map(x => ZoneOffset.ofHoursMinutes(x / 60, x % 60)),
    Gen.choose(-18 * 60 * 60, 18 * 60 * 60).map(ZoneOffset.ofTotalSeconds))
  val genDuration: Gen[Duration] = Gen.oneOf(
    Gen.choose(Long.MinValue / 86400, Long.MaxValue / 86400).map(Duration.ofDays),
    Gen.choose(Long.MinValue / 3600, Long.MaxValue / 3600).map(Duration.ofHours),
    Gen.choose(Long.MinValue / 60, Long.MaxValue / 60).map(Duration.ofMinutes),
    // FIXME: JDK 8/9 have bug in parsing of Duration with zero seconds and negative nanos
    Gen.choose(Long.MinValue, Long.MaxValue).map(Duration.ofSeconds),
    // FIXME: JDK 8 has bug in serialization of Duration with negative nanos
    Gen.choose(if (isJDK8) 0L else Int.MinValue, Int.MaxValue.toLong).map(Duration.ofMillis),
    Gen.choose(if (isJDK8) 0L else Int.MinValue, Int.MaxValue.toLong).map(Duration.ofNanos))
  val genInstant: Gen[Instant] = for {
    epochSecond <- Gen.choose(Instant.MIN.getEpochSecond, Instant.MAX.getEpochSecond)
    nanoAdjustment <- Gen.choose(Long.MinValue, Long.MaxValue)
    fallbackInstant <- Gen.oneOf(Instant.MIN, Instant.EPOCH, Instant.MAX)
  } yield Try(Instant.ofEpochSecond(epochSecond, nanoAdjustment)).getOrElse(fallbackInstant)
  val genLocalDate: Gen[LocalDate] = for {
    year <- Gen.choose(-999999999, 999999999)
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, Month.of(month).length(Year.of(year).isLeap))
  } yield LocalDate.of(year, month, day)
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
    year <- Arbitrary.arbitrary[Int]
    month <- Arbitrary.arbitrary[Int]
    day <- Arbitrary.arbitrary[Int]
  } yield Period.of(year, month, day)
  val genYear: Gen[Year] = Gen.choose(-999999999, 999999999).map(Year.of)
  val genYearMonth: Gen[YearMonth] = for {
    year <- Gen.choose(-999999999, 999999999)
    month <- Gen.choose(1, 12)
  } yield YearMonth.of(year, month)
  val genZoneId: Gen[ZoneId] = Gen.oneOf(
    genZoneOffset,
    genZoneOffset.map(zo => ZoneId.ofOffset("UT", zo)),
    genZoneOffset.map(zo => ZoneId.ofOffset("UTC", zo)),
    genZoneOffset.map(zo => ZoneId.ofOffset("GMT", zo)),
    Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toList).map(ZoneId.of),
    Gen.oneOf(ZoneId.SHORT_IDS.values().asScala.toList).map(ZoneId.of))
  val genZonedDateTime: Gen[ZonedDateTime] = for {
    localDateTime <- genLocalDateTime
    zoneId <- genZoneId
  } yield ZonedDateTime.of(localDateTime, zoneId)
  val genNonFiniteDouble: Gen[Double] = Gen.oneOf(
    Gen.oneOf(java.lang.Double.NaN, java.lang.Double.NEGATIVE_INFINITY, java.lang.Double.POSITIVE_INFINITY),
    Gen.choose(0, 0x0007FFFFFFFFFFFFL).map(x => java.lang.Double.longBitsToDouble(x | 0x7FF8000000000000L))) // Double.NaN with error code
  val genNonFiniteFloat: Gen[Float] = Gen.oneOf(
    Gen.oneOf(java.lang.Float.NaN, java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.POSITIVE_INFINITY),
    Gen.choose(0, 0x003FFFFF).map(x => java.lang.Float.intBitsToFloat(x | 0x7FC00000))) // Float.NaN with error code
}