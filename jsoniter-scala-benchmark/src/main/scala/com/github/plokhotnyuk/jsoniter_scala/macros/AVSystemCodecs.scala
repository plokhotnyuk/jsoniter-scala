package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.GenCodec._
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.{BitSet, IntMap}
import scala.collection.mutable

object AVSystemCodecs {
  implicit val adtGenCodec: GenCodec[ADTBase] = materializeRecursively
  implicit val anyValsGenCodec: GenCodec[AnyVals] = {
    // FIXME: AVSystem GenCodec wraps values of value classes by extra JSON objects
    // see https://github.com/AVSystem/scala-commons/issues/91
    implicit val byteValGenCodec: GenCodec[ByteVal] = materialize
    implicit val shortValGenCodec: GenCodec[ShortVal] = materialize
    implicit val intValGenCodec: GenCodec[IntVal] = materialize
    implicit val longValGenCodec: GenCodec[LongVal] = materialize
    implicit val booleanValGenCodec: GenCodec[BooleanVal] = materialize
    implicit val charValGenCodec: GenCodec[CharVal] = materialize
    implicit val doubleValGenCodec: GenCodec[DoubleVal] = materialize
    implicit val floatValGenCodec: GenCodec[FloatVal] = materialize
    materialize
  }
  implicit val anyRefsGenCodec: GenCodec[AnyRefs] = materialize
  implicit val durationGenCodec: GenCodec[Duration] = transformed(_.toString, Duration.parse)
  implicit val suitEnumGenCodec: GenCodec[SuitEnum] = transformed(_.toString, SuitEnum.withName)
  implicit val instantGenCodec: GenCodec[Instant] = transformed(_.toString, Instant.parse)
  implicit val localDateGenCodec: GenCodec[LocalDate] = transformed(_.toString, LocalDate.parse)
  implicit val localDateTimeGenCodec: GenCodec[LocalDateTime] = transformed(_.toString, LocalDateTime.parse)
  implicit val localTimeGenCodec: GenCodec[LocalTime] = transformed(_.toString, LocalTime.parse)
  implicit val monthDayGenCodec: GenCodec[MonthDay] = transformed(_.toString, MonthDay.parse)
  implicit val offsetDateTimeGenCodec: GenCodec[OffsetDateTime] = transformed(_.toString, OffsetDateTime.parse)
  implicit val offsetTimeGenCodec: GenCodec[OffsetTime] = transformed(_.toString, OffsetTime.parse)
  implicit val periodGenCodec: GenCodec[Period] = transformed(_.toString, Period.parse)
  implicit val suitADTGenCodec: GenCodec[SuitADT] = transformed[SuitADT, String](_.toString, {
    case "Hearts" => Hearts
    case "Spades" => Spades
    case "Diamonds" => Diamonds
    case "Clubs" => Clubs
    case _ => throw new IllegalArgumentException("SuitADT")
  })
  implicit val uuidGenCodec: GenCodec[UUID] = transformed(_.toString, UUID.fromString)
  implicit val yearGenCodec: GenCodec[Year] = transformed(_.toString, Year.parse)
  implicit val yearMonthGenCodec: GenCodec[YearMonth] = transformed(_.toString, YearMonth.parse)
  implicit val zonedDateTimeGenCodec: GenCodec[ZonedDateTime] = transformed(_.toString, ZonedDateTime.parse)
  implicit val zoneIdGenCodec: GenCodec[ZoneId] = transformed(_.toString, ZoneId.of)
  implicit val zoneOffsetGenCodec: GenCodec[ZoneOffset] = transformed(_.toString, ZoneOffset.of)
  implicit val bitSetGenCodec: GenCodec[BitSet] =
    transformed(_.toArray, (x: Array[Int]) => BitSet(x:_*)) // WARNING: don't do this for open-system
  implicit val extractFieldsGenCodec: GenCodec[ExtractFields] = materialize
  implicit val geoJSONGenCodec: GenCodec[GeoJSON] = materializeRecursively
  implicit val googleMapsAPIGenCodec: GenCodec[DistanceMatrix] = materializeRecursively
  implicit val intMapOfBooleansGenCodec: GenCodec[IntMap[Boolean]] =
    transformed(_.seq, (x: Map[Int, Boolean]) => IntMap(x.toArray:_*))
  implicit val missingReqFieldGenCodec: GenCodec[MissingReqFields] = materialize
  implicit val mutableBitSetGenCodec: GenCodec[mutable.BitSet] = // WARNING: don't do this for open-system
    transformed(_.toArray, (x: Array[Int]) => mutable.BitSet(x:_*))
  implicit val mutableLongMapOfBooleansGenCodec: GenCodec[mutable.LongMap[Boolean]] =
    transformed(_.seq, (x: mutable.Map[Long, Boolean]) => mutable.LongMap(x.toArray:_*))
  implicit val nestedStructsGenCodec: GenCodec[NestedStructs] = materialize
  implicit val primitivesGenCodec: GenCodec[Primitives] = materialize
  implicit val twitterAPIGenCodec: GenCodec[Tweet] = materializeRecursively
}
