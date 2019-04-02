package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.time._
import java.util.UUID

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.GenCodec._
import SuitEnum.SuitEnum
import com.avsystem.commons.serialization.json.JsonOptions

import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.mutable

object AVSystemCodecs {
  val jsonOptions: JsonOptions = JsonOptions.Default.copy(mathContext = MathContext.UNLIMITED /*WARNING: don't do this for open-systems*/)
  implicit val adtGenCodec: GenCodec[ADTBase] = materializeRecursively
  implicit val anyValsGenCodec: GenCodec[AnyVals] = materializeRecursively
  implicit val anyRefsGenCodec: GenCodec[AnyRefs] = materializeRecursively
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
  implicit val suitADTGenCodec: GenCodec[SuitADT] = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    transformed[SuitADT, String](_.toString, s => suite.getOrElse(s, throw new IllegalArgumentException("SuitADT")))
  }
  implicit val uuidGenCodec: GenCodec[UUID] = transformed(_.toString, UUID.fromString)
  implicit val yearGenCodec: GenCodec[Year] = transformed(_.toString, Year.parse)
  implicit val yearMonthGenCodec: GenCodec[YearMonth] = transformed(_.toString, YearMonth.parse)
  implicit val zonedDateTimeGenCodec: GenCodec[ZonedDateTime] = transformed(_.toString, ZonedDateTime.parse)
  implicit val zoneIdGenCodec: GenCodec[ZoneId] = transformed(_.toString, ZoneId.of)
  implicit val zoneOffsetGenCodec: GenCodec[ZoneOffset] = transformed(_.toString, ZoneOffset.of)
  implicit val bitSetGenCodec: GenCodec[BitSet] =
    transformed(_.toArray, (x: Array[Int]) => BitSet(x:_*)) // WARNING: don't do this for open-systems
  implicit val extractFieldsGenCodec: GenCodec[ExtractFields] = materializeRecursively
  implicit val geoJSONGenCodec: GenCodec[GeoJSON] = materializeRecursively
  implicit val googleMapsAPIGenCodec: GenCodec[DistanceMatrix] = materializeRecursively
  implicit val intMapOfBooleansGenCodec: GenCodec[IntMap[Boolean]] =
    transformed(_.seq, (x: Map[Int, Boolean]) => IntMap(x.toArray:_*))
  implicit val missingReqFieldGenCodec: GenCodec[MissingReqFields] = materializeRecursively
  implicit val mutableBitSetGenCodec: GenCodec[mutable.BitSet] =
    transformed(_.toArray, (x: Array[Int]) => mutable.BitSet(x:_*)) // WARNING: don't do this for open-systems
  implicit val mutableLongMapOfBooleansGenCodec: GenCodec[mutable.LongMap[Boolean]] =
    transformed(_.seq, (x: mutable.Map[Long, Boolean]) => mutable.LongMap(x.toArray:_*))
  implicit val nestedStructsGenCodec: GenCodec[NestedStructs] = materializeRecursively
  implicit val openHashMapOfIntsToBooleansGenCodec: GenCodec[mutable.OpenHashMap[Int, Boolean]] =
    transformed(_.seq, (x: mutable.Map[Int, Boolean]) => mutable.OpenHashMap(x.toArray:_*))
  implicit val primitivesGenCodec: GenCodec[Primitives] = materializeRecursively
  implicit val twitterAPIGenCodec: GenCodec[Tweet] = materializeRecursively
}
