package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import upickle.default._

object UPickleReaderWriters {
  implicit val adtReaderWriter: ReadWriter[ADTBase] = ReadWriter.merge(macroRW[X], macroRW[Y], macroRW[Z])
  implicit val suiteADTReaderWriter: ReadWriter[SuitADT] = readwriter[String].bimap(_.toString, {
    case "Hearts" => Hearts
    case "Spades" => Spades
    case "Diamonds" => Diamonds
    case "Clubs" => Clubs
    case _ => throw new IllegalArgumentException("SuitADT")
  })
  implicit val anyRefsReaderWriter: ReadWriter[AnyRefs] = macroRW
  implicit val extractFieldsReaderWriter: ReadWriter[ExtractFields] = macroRW
  implicit val durationReaderWriter: ReadWriter[Duration] = readwriter[String].bimap(_.toString, Duration.parse)
  implicit val instantReaderWriter: ReadWriter[Instant] = readwriter[String].bimap(_.toString, Instant.parse)
  implicit val geoJsonReaderWriter: ReadWriter[GeoJSON] = {
    implicit lazy val v1: ReadWriter[Point] = macroRW
    implicit lazy val v2: ReadWriter[MultiPoint] = macroRW
    implicit lazy val v3: ReadWriter[LineString] = macroRW
    implicit lazy val v4: ReadWriter[MultiLineString] = macroRW
    implicit lazy val v5: ReadWriter[Polygon] = macroRW
    implicit lazy val v6: ReadWriter[MultiPolygon] = macroRW
    implicit lazy val v7: ReadWriter[GeometryCollection] = macroRW
    implicit lazy val v8: ReadWriter[Geometry] = macroRW
    implicit lazy val v9: ReadWriter[FeatureCollection] = macroRW
    implicit lazy val v10: ReadWriter[Feature] = macroRW
    macroRW
  }
  implicit val googleMApsAPIReaderWriter: ReadWriter[DistanceMatrix] = {
    implicit val v1: ReadWriter[Value] = macroRW
    implicit val v2: ReadWriter[Elements] = macroRW
    implicit val v3: ReadWriter[Rows] = macroRW
    macroRW[DistanceMatrix]
  }
  implicit val localDateReaderWriter: ReadWriter[LocalDate] = readwriter[String].bimap(_.toString, LocalDate.parse)
  implicit val localDateTimeReaderWriter: ReadWriter[LocalDateTime] = readwriter[String].bimap(_.toString, LocalDateTime.parse)
  implicit val localTimeReaderWriter: ReadWriter[LocalTime] = readwriter[String].bimap(_.toString, LocalTime.parse)
  implicit val nestedStructsReaderWriter: ReadWriter[NestedStructs] = macroRW
  implicit val missingReqFieldsReaderWriter: ReadWriter[MissingReqFields] = macroRW
  implicit val monthDayReaderWriter: ReadWriter[MonthDay] = readwriter[String].bimap(_.toString, MonthDay.parse)
  implicit val offsetDateTimeReaderWriter: ReadWriter[OffsetDateTime] = readwriter[String].bimap(_.toString, OffsetDateTime.parse)
  implicit val offsetTimeReaderWriter: ReadWriter[OffsetTime] = readwriter[String].bimap(_.toString, OffsetTime.parse)
  implicit val periodReaderWriter: ReadWriter[Period] = readwriter[String].bimap(_.toString, Period.parse)
  implicit val primitivesReaderWriter: ReadWriter[Primitives] = macroRW
  implicit val suitEnumReaderWriter: ReadWriter[SuitEnum] = readwriter[String].bimap(_.toString, SuitEnum.withName)
  implicit val suitReaderWriter: ReadWriter[Suit] = readwriter[String].bimap(_.name, Suit.valueOf)
  implicit val twitterAPIReaderWriter: ReadWriter[Tweet] = {
    implicit val v1: ReadWriter[Urls] = macroRW
    implicit val v2: ReadWriter[Url] = macroRW
    implicit val v3: ReadWriter[UserMentions] = macroRW
    implicit val v4: ReadWriter[Entities] = macroRW
    implicit val v5: ReadWriter[UserEntities] = macroRW
    implicit val v6: ReadWriter[User] = macroRW
    implicit val v7: ReadWriter[RetweetedStatus] = macroRW
    macroRW[Tweet]
  }
  implicit val yearReaderWriter: ReadWriter[Year] = readwriter[String].bimap(_.toString, Year.parse)
  implicit val yearMonthReaderWriter: ReadWriter[YearMonth] = readwriter[String].bimap(_.toString, YearMonth.parse)
  implicit val zonedDateTimeReaderWriter: ReadWriter[ZonedDateTime] = readwriter[String].bimap(_.toString, ZonedDateTime.parse)
  implicit val zoneIdReaderWriter: ReadWriter[ZoneId] = readwriter[String].bimap(_.toString, ZoneId.of)
  implicit val zoneOffsetReaderWriter: ReadWriter[ZoneOffset] = readwriter[String].bimap(_.toString, ZoneOffset.of)
}