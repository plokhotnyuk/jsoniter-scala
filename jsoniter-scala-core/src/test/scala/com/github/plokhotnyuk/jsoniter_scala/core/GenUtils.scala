package com.github.plokhotnyuk.jsoniter_scala.core

import java.time._

import org.scalacheck.{Arbitrary, Gen}

import scala.collection.JavaConverters._

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
  val genInstant: Gen[Instant] =
    for {
      year <- Gen.choose(-1000000000, 1000000000)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, maxDaysInMonth(year, month))
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
      offset <- genZoneOffset
    } yield LocalDateTime.of(year, month, day, hour, minute, second, nano).toInstant(offset)
  val genLocalDate: Gen[LocalDate] =
    for {
      year <- Gen.choose(-999999999, 999999999)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, maxDaysInMonth(year, month))
    } yield LocalDate.of(year, month, day)
  val genLocalDateTime: Gen[LocalDateTime] =
    for {
      year <- Gen.choose(-999999999, 999999999)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, maxDaysInMonth(year, month))
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
    } yield LocalDateTime.of(year, month, day, hour, minute, second, nano)
  val genLocalTime: Gen[LocalTime] =
    for {
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
    } yield LocalTime.of(hour, minute, second, nano)
  val genMonthDay: Gen[MonthDay] =
    for {
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, 29)
    } yield MonthDay.of(month, day)
  val genOffsetDateTime: Gen[OffsetDateTime] =
    for {
      year <- Gen.choose(-999999999, 999999999)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, maxDaysInMonth(year, month))
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
      offset <- genZoneOffset
    } yield OffsetDateTime.of(year, month, day, hour, minute, second, nano, offset)
  val genOffsetTime: Gen[OffsetTime] =
    for {
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
      zoneOffset <- genZoneOffset
    } yield OffsetTime.of(hour, minute, second, nano, zoneOffset)
  val genPeriod: Gen[Period] =
    for {
      year <- Arbitrary.arbitrary[Int]
      month <- Arbitrary.arbitrary[Int]
      day <- Arbitrary.arbitrary[Int]
    } yield Period.of(year, month, day)
  val genYear: Gen[Year] = Gen.choose(-999999999, 999999999).map(Year.of)
  val genYearMonth: Gen[YearMonth] =
    for {
      year <- Gen.choose(-999999999, 999999999)
      month <- Gen.choose(1, 12)
    } yield YearMonth.of(year, month)
  val genZoneId: Gen[ZoneId] =
    Gen.oneOf(genZoneOffset,
      genZoneOffset.map(zo => ZoneId.ofOffset("UT", zo)),
      genZoneOffset.map(zo => ZoneId.ofOffset("UTC", zo)),
      genZoneOffset.map(zo => ZoneId.ofOffset("GMT", zo)),
      Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toList).map(ZoneId.of),
      Gen.oneOf(ZoneId.SHORT_IDS.values().asScala.toList).map(ZoneId.of))
  val genZonedDateTime: Gen[ZonedDateTime] =
    for {
      year <- Gen.choose(-999999999, 999999999)
      month <- Gen.choose(1, 12)
      day <- Gen.choose(1, maxDaysInMonth(year, month))
      hour <- Gen.choose(0, 23)
      minute <- Gen.choose(0, 59)
      second <- Gen.choose(0, 59)
      nano <- Gen.choose(0, 999999999)
      zoneId <- genZoneId
    } yield ZonedDateTime.of(year, month, day, hour, minute, second, nano, zoneId)
  val genNonFiniteDouble: Gen[Double] = Gen.oneOf(
    Gen.oneOf(java.lang.Double.NaN, java.lang.Double.NEGATIVE_INFINITY, java.lang.Double.POSITIVE_INFINITY),
    Gen.choose(0, 0x0007FFFFFFFFFFFFL).map(x => java.lang.Double.longBitsToDouble(x | 0x7FF8000000000000L))) // Double.NaN with error code
  val genNonFiniteFloat: Gen[Float] = Gen.oneOf(
    Gen.oneOf(java.lang.Float.NaN, java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.POSITIVE_INFINITY),
    Gen.choose(0, 0x003FFFFF).map(x => java.lang.Float.intBitsToFloat(x | 0x7FC00000))) // Float.NaN with error code

  private def maxDaysInMonth(year: Int, month: Int): Int = Month.of(month).length(Year.of(year).isLeap)
}
