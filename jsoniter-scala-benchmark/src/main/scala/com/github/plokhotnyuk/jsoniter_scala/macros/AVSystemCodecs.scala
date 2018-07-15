package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.GenCodec._
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.{BitSet, IntMap}
import scala.collection.mutable

object AVSystemCodecs {
  implicit val adtGenCodec: GenCodec[AdtBase] = materializeRecursively[AdtBase]
  implicit val anyRefsGenCodec: GenCodec[AnyRefs] = materialize[AnyRefs]
  implicit val durationGenCodec: GenCodec[Duration] = transformed[Duration, String](_.toString, Duration.parse)
  implicit val suitEnumGenCodec: GenCodec[SuitEnum] = transformed[SuitEnum, String](_.toString, SuitEnum.withName)
  implicit val instantGenCodec: GenCodec[Instant] = transformed[Instant, String](_.toString, Instant.parse)
  implicit val localDateGenCodec: GenCodec[LocalDate] = transformed[LocalDate, String](_.toString, LocalDate.parse)
  implicit val localDateTimeGenCodec: GenCodec[LocalDateTime] =
    transformed[LocalDateTime, String](_.toString, LocalDateTime.parse)
  implicit val localTimeGenCodec: GenCodec[LocalTime] = transformed[LocalTime, String](_.toString, LocalTime.parse)
  implicit val offsetDateTimeGenCodec: GenCodec[OffsetDateTime] =
    transformed[OffsetDateTime, String](_.toString, OffsetDateTime.parse)
  implicit val offsetTimeGenCodec: GenCodec[OffsetTime] = transformed[OffsetTime, String](_.toString, OffsetTime.parse)
  implicit val periodGenCodec: GenCodec[Period] = transformed[Period, String](_.toString, Period.parse)
  implicit val uuidGenCodec: GenCodec[UUID] = transformed[UUID, String](_.toString, UUID.fromString)
  implicit val yearGenCodec: GenCodec[Year] = transformed[Year, String](_.toString, Year.parse)
  implicit val yearMonthGenCodec: GenCodec[YearMonth] = transformed[YearMonth, String](_.toString, YearMonth.parse)
  implicit val zonedDateTimeGenCodec: GenCodec[ZonedDateTime] =
    transformed[ZonedDateTime, String](_.toString, ZonedDateTime.parse)
  implicit val zoneIdGenCodec: GenCodec[ZoneId] = transformed[ZoneId, String](_.toString, ZoneId.of)
  implicit val zoneOffsetGenCodec: GenCodec[ZoneOffset] = transformed[ZoneOffset, String](_.toString, ZoneOffset.of)
  implicit val bitSetGenCodec: GenCodec[BitSet] = transformed[BitSet, Array[Int]](_.toArray, x => BitSet(x:_*)) // WARNING: don't do this for open-system
  implicit val extractFieldsGenCodec: GenCodec[ExtractFields] = materialize[ExtractFields]
  implicit val geoJSONGenCodec: GenCodec[GeoJSON] = materializeRecursively[GeoJSON]
  implicit val googleMapsAPIGenCodec: GenCodec[DistanceMatrix] = materializeRecursively[DistanceMatrix]
  implicit val intMapOfBooleansGenCodec: GenCodec[IntMap[Boolean]] =
    transformed[IntMap[Boolean], Map[Int, Boolean]](_.seq, x => IntMap(x.toArray:_*))
  implicit val missingReqFieldGenCodec: GenCodec[MissingReqFields] = materialize[MissingReqFields]
  implicit val mutableBitSetGenCodec: GenCodec[mutable.BitSet] = // WARNING: don't do this for open-system
    transformed[mutable.BitSet, Array[Int]](_.toArray, x => mutable.BitSet(x:_*))
  implicit val mutableLongMapOfBooleansGenCodec: GenCodec[mutable.LongMap[Boolean]] =
    transformed[mutable.LongMap[Boolean], mutable.Map[Long, Boolean]](_.seq, x => mutable.LongMap(x.toArray:_*))
  implicit val nestedStructsGenCodec: GenCodec[NestedStructs] = materializeRecursively[NestedStructs]
  implicit val primitivesGenCodec: GenCodec[Primitives] = materialize[Primitives]
  implicit val twitterAPIGenCodec: GenCodec[Tweet] = materializeRecursively[Tweet]
}
