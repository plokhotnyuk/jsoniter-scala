package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import ujson.BytesRenderer
import upickle.default._

object UPickleReaderWriters {
  // FIXME:  uPickle encodes Option[_] to JSON arrays
  def optionWriter[T: Writer]: Writer[Option[T]] = writer[T].comapNulls[Option[T]](_.getOrElse(null.asInstanceOf[T]))

  def optionReader[T: Reader]: Reader[Option[T]] = reader[T].map[Option[T]](Option.apply)

  implicit def optionRW[T: Reader: Writer]: ReadWriter[Option[T]] = ReadWriter.join(optionReader, optionWriter)

  implicit val adtReaderWriter: ReadWriter[AdtBase] = ReadWriter.merge(macroRW[A], macroRW[B], macroRW[C])
  implicit val anyRefsReaderWriter: ReadWriter[AnyRefs] = macroRW[AnyRefs]
  implicit val extractFieldsReaderWriter: ReadWriter[ExtractFields] = macroRW[ExtractFields]
  implicit val durationReaderWriter: ReadWriter[Duration] =
    readwriter[String].bimap[Duration](_.toString, Duration.parse)
  implicit val instantReaderWriter: ReadWriter[Instant] = readwriter[String].bimap[Instant](_.toString, Instant.parse)
  implicit val geoJsonReaderWriter: ReadWriter[GeoJSON] = {
    implicit lazy val v1: ReadWriter[Point] = macroRW[Point]
    implicit lazy val v2: ReadWriter[MultiPoint] = macroRW[MultiPoint]
    implicit lazy val v3: ReadWriter[LineString] = macroRW[LineString]
    implicit lazy val v4: ReadWriter[MultiLineString] = macroRW[MultiLineString]
    implicit lazy val v5: ReadWriter[Polygon] = macroRW[Polygon]
    implicit lazy val v6: ReadWriter[MultiPolygon] = macroRW[MultiPolygon]
    implicit lazy val v7: ReadWriter[GeometryCollection] = macroRW[GeometryCollection]
    implicit lazy val v8: ReadWriter[Geometry] = macroRW[Geometry]
    implicit lazy val v9: ReadWriter[FeatureCollection] = macroRW[FeatureCollection]
    implicit lazy val v10: ReadWriter[Feature] = macroRW[Feature]
    macroRW[GeoJSON]
  }
  implicit val googleMApsAPIReaderWriter: ReadWriter[DistanceMatrix] = {
    implicit val v1: ReadWriter[Value] = macroRW[Value]
    implicit val v2: ReadWriter[Elements] = macroRW[Elements]
    implicit val v3: ReadWriter[Rows] = macroRW[Rows]
    macroRW[DistanceMatrix]
  }
  implicit val localDateReaderWriter: ReadWriter[LocalDate] =
    readwriter[String].bimap[LocalDate](_.toString, LocalDate.parse)
  implicit val localDateTimeReaderWriter: ReadWriter[LocalDateTime] =
    readwriter[String].bimap[LocalDateTime](_.toString, LocalDateTime.parse)
  implicit val localTimeReaderWriter: ReadWriter[LocalTime] =
    readwriter[String].bimap[LocalTime](_.toString, LocalTime.parse)
  implicit val nestedStructsReaderWriter: ReadWriter[NestedStructs] = macroRW[NestedStructs]
  implicit val missingReqFieldsReaderWriter: ReadWriter[MissingReqFields] = macroRW[MissingReqFields]
  implicit val offsetDateTimeReaderWriter: ReadWriter[OffsetDateTime] =
    readwriter[String].bimap[OffsetDateTime](_.toString, OffsetDateTime.parse)
  implicit val offsetTimeReaderWriter: ReadWriter[OffsetTime] =
    readwriter[String].bimap[OffsetTime](_.toString, OffsetTime.parse)
  implicit val periodReaderWriter: ReadWriter[Period] = readwriter[String].bimap[Period](_.toString, Period.parse)
  implicit val primitivesReaderWriter: ReadWriter[Primitives] = macroRW[Primitives]
  implicit val suitEnumReaderWriter: ReadWriter[SuitEnum] =
    readwriter[String].bimap[SuitEnum](_.toString, SuitEnum.withName)
  implicit val suitReaderWriter: ReadWriter[Suit] = readwriter[String].bimap[Suit](_.name, Suit.valueOf)
  implicit val twitterAPIReaderWriter: ReadWriter[Tweet] = {
    implicit val v1: ReadWriter[Urls] = macroRW[Urls]
    implicit val v2: ReadWriter[Url] = macroRW[Url]
    implicit val v3: ReadWriter[UserMentions] = macroRW[UserMentions]
    implicit val v4: ReadWriter[Entities] = macroRW[Entities]
    implicit val v5: ReadWriter[UserEntities] = macroRW[UserEntities]
    implicit val v6: ReadWriter[User] = macroRW[User]
    implicit val v7: ReadWriter[RetweetedStatus] = macroRW[RetweetedStatus]
    macroRW[Tweet]
  }
  implicit val yearReaderWriter: ReadWriter[Year] = readwriter[String].bimap[Year](_.toString, Year.parse)
  implicit val yearMonthReaderWriter: ReadWriter[YearMonth] =
    readwriter[String].bimap[YearMonth](_.toString, YearMonth.parse)
  implicit val zonedDateTimeReaderWriter: ReadWriter[ZonedDateTime] =
    readwriter[String].bimap[ZonedDateTime](_.toString, ZonedDateTime.parse)
  implicit val zoneIdReaderWriter: ReadWriter[ZoneId] = readwriter[String].bimap[ZoneId](_.toString, ZoneId.of)
  implicit val zoneOffsetReaderWriter: ReadWriter[ZoneOffset] =
    readwriter[String].bimap[ZoneOffset](_.toString, ZoneOffset.of)

  // FIXME uPickle serializes to bytes through java.io.OutputStreamWriter
  def writeToBytes[T](t: T, indent: Int = -1)(implicit writer: Writer[T]): Array[Byte] =
    transform(t)(writer).to(BytesRenderer(indent)).toBytes
}