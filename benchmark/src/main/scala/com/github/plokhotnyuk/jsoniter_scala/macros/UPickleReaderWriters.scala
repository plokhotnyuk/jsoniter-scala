package com.github.plokhotnyuk.jsoniter_scala.macros

import java.io.{Serializable, Writer}
import java.nio.charset.StandardCharsets
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import upickle.default._

object UPickleReaderWriters {
  private[this] final val writerPool: ThreadLocal[UTF8Writer] = new ThreadLocal[UTF8Writer] {
    override def initialValue(): UTF8Writer = new UTF8Writer(16 * 1024)
  }

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

  def writeToBytes[A: upickle.default.Writer](x: A, indent: Int = -1, prefBufSize: Int = 16384): Array[Byte] = {
    val w = writerPool.get
    w.reset(prefBufSize)
    writeTo(x, w, indent)
    w.toBytes
  }
}

final class UTF8Writer(capacity: Int) extends Writer with Serializable {
  private[this] var buf = new java.lang.StringBuilder(capacity)

  def reset(prefBufSize: Int): Unit =
    if (buf.capacity() <= prefBufSize) buf.setLength(0)
    else buf = new java.lang.StringBuilder(prefBufSize)

  override def append(value: Char): Writer = {
    buf.append(value)
    this
  }

  override def append(value: CharSequence): Writer = {
    buf.append(value)
    this
  }

  override def append(value: CharSequence, start: Int, end: Int): Writer = {
    buf.append(value, start, end)
    this
  }

  override def close(): Unit = ()

  override def flush(): Unit = ()

  override def write(value: String): Unit = buf.append(value)

  override def write(value: Array[Char], offset: Int, length: Int): Unit = buf.append(value, offset, length)

  def toBytes: Array[Byte] = buf.toString.getBytes(StandardCharsets.UTF_8)
}
