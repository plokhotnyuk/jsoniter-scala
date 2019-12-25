package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.time._
import java.util.UUID

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.GenCodec._
import SuitEnum.SuitEnum
import com.avsystem.commons.serialization.json.JsonBinaryFormat.{Base64, HexString}
import com.avsystem.commons.serialization.json.JsonOptions
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask

import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.mutable

object AVSystemCodecs {
  val jsonOptions: JsonOptions = JsonOptions.Default.copy(mathContext = MathContext.UNLIMITED /*WARNING: don't do this for open-systems*/)
  val jsonBase16Options: JsonOptions = JsonOptions.Default.copy(binaryFormat = HexString)
  val jsonBase64Options: JsonOptions = JsonOptions.Default.copy(binaryFormat = Base64())
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
    transformed(_.toArray, (arr: Array[Int]) => BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: don't do this for open-systems */)))
  implicit val extractFieldsGenCodec: GenCodec[ExtractFields] = materializeRecursively
  implicit val geoJSONGenCodec: GenCodec[GeoJSON.GeoJSON] = materializeRecursively
  implicit val googleMapsAPIGenCodec: GenCodec[GoogleMapsAPI.DistanceMatrix] = materializeRecursively
  implicit val intMapOfBooleansGenCodec: GenCodec[IntMap[Boolean]] =
    transformed(m => (m: Map[Int, Boolean]),
      (m: Map[Int, Boolean]) => m.foldLeft(IntMap.empty[Boolean])((im, p) => im.updated(p._1, p._2)))
  implicit val missingReqFieldGenCodec: GenCodec[MissingRequiredFields] = materializeRecursively
  implicit val mutableBitSetGenCodec: GenCodec[mutable.BitSet] =
    transformed(_.toArray, (a: Array[Int]) => mutable.BitSet.fromBitMaskNoCopy(toBitMask(a, Int.MaxValue /* WARNING: don't do this for open-systems */)))
  implicit val mutableLongMapOfBooleansGenCodec: GenCodec[mutable.LongMap[Boolean]] =
    transformed(m => (m: mutable.Map[Long, Boolean]),
      (m: mutable.Map[Long, Boolean]) => m.foldLeft(new mutable.LongMap[Boolean])((lm, p) => lm += (p._1, p._2)))
  implicit val nestedStructsGenCodec: GenCodec[NestedStructs] = materializeRecursively
  implicit val openRTBGenCodec: GenCodec[OpenRTB.BidRequest] = materializeRecursively
  implicit val primitivesGenCodec: GenCodec[Primitives] = materializeRecursively
  implicit val twitterAPIGenCodec: GenCodec[TwitterAPI.Tweet] = materializeRecursively
}
