package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import ujson.{ObjVisitor, _}
import upickle.default
import upickle.default._

object UPickleReaderWriters {
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

  def writeToBytes[A: default.Writer](x: A, config: WriterConfig = WriterConfig()): Array[Byte] =
    writeToArray(x, config)(new JsonValueCodec[A] {
      override def decodeValue(in: JsonReader, default: A): A = ???

      override def encodeValue(x: A, out: JsonWriter): Unit = {
        lazy val visitor: ujson.Visitor[JsonWriter, JsonWriter] = new ujson.Visitor[JsonWriter, JsonWriter] {
          override def visitArray(index: Int): ArrVisitor[JsonWriter, JsonWriter] = new ArrVisitor[JsonWriter, JsonWriter] {
            out.writeArrayStart()
            out.writeComma()

            override def subVisitor: Visitor[Nothing, Any] = visitor

            override def visitValue(v: JsonWriter, index: Int): Unit = out.writeComma()

            override def visitEnd(index: Int): JsonWriter = {
              out.writeArrayEnd2()
              out
            }
          }

          override def visitObject(index: Int): ObjVisitor[JsonWriter, JsonWriter] = new ObjVisitor[JsonWriter, JsonWriter] {
            out.writeObjectStart()

            override def visitKey(s: CharSequence, index: Int): Unit = out.writeKey(s.toString)

            override def subVisitor: Visitor[Nothing, Any] = visitor

            override def visitValue(v: JsonWriter, index: Int): Unit = ()

            override def visitEnd(index: Int): JsonWriter = {
              out.writeObjectEnd()
              out
            }
          }

          override def visitNull(index: Int): JsonWriter = {
            out.writeNull()
            out
          }

          override def visitFalse(index: Int): JsonWriter = {
            out.writeVal(false)
            out
          }

          override def visitTrue(index: Int): JsonWriter = {
            out.writeVal(true)
            out
          }

          override def visitNum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): JsonWriter = {
            out.writeNonEscapedAsciiStringWithoutParentheses(s.toString)
            out
          }

          override def visitString(s: CharSequence, index: Int): JsonWriter = {
            out.writeVal(s.toString)
            out
          }
        }
        upickle.default.transform(x).to(visitor)
      }

      override def nullValue: A = null.asInstanceOf[A]
    })
}
