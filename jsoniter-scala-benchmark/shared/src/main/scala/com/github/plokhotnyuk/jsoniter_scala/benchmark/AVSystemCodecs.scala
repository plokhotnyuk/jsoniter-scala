package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.time._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.GenCodec._
import SuitEnum.SuitEnum
import com.avsystem.commons.serialization.json.JsonBinaryFormat.{Base64, HexString}
import com.avsystem.commons.serialization.json.JsonOptions
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask

import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.mutable

object AVSystemCodecs {
  val jsonOptions: JsonOptions =
    JsonOptions.Default.copy(mathContext = MathContext.UNLIMITED /* WARNING: It is an unsafe option for open systems */)
  val jsonBase16Options: JsonOptions = JsonOptions.Default.copy(binaryFormat = HexString)
  val jsonBase64Options: JsonOptions = JsonOptions.Default.copy(binaryFormat = Base64())
  implicit val adtGenCodec: GenCodec[ADTBase] = materializeRecursively
  implicit val anyValsGenCodec: GenCodec[AnyVals] = materializeRecursively
  implicit val durationGenCodec: GenCodec[Duration] = transformed(_.toString, Duration.parse)
  implicit val suitEnumGenCodec: GenCodec[SuitEnum] = transformed(_.toString, {
    val ec = new ConcurrentHashMap[String, SuitEnum]
    (s: String) => {
      var x = ec.get(s)
      if (x eq null) {
        x = SuitEnum.withName(s)
        ec.put(s, x)
      }
      x
    }
  })
  implicit val instantGenCodec: GenCodec[Instant] = transformed(_.toString, Instant.parse)
  implicit val localDateGenCodec: GenCodec[LocalDate] = transformed(_.toString, LocalDate.parse)
  implicit val localDateTimeGenCodec: GenCodec[LocalDateTime] = transformed(_.toString, LocalDateTime.parse)
  implicit val localTimeGenCodec: GenCodec[LocalTime] = transformed(_.toString, LocalTime.parse)
  implicit val monthDayGenCodec: GenCodec[MonthDay] = transformed(_.toString, MonthDay.parse)
  implicit val offsetDateTimeGenCodec: GenCodec[OffsetDateTime] = transformed(_.toString, OffsetDateTime.parse)
  implicit val offsetTimeGenCodec: GenCodec[OffsetTime] = transformed(_.toString, OffsetTime.parse)
  implicit val periodGenCodec: GenCodec[Period] = transformed(_.toString, Period.parse)
  implicit val suitADTGenCodec: GenCodec[SuitADT] = transformed[SuitADT, String](_.toString, {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => suite.getOrElse(s, throw new IllegalArgumentException("SuitADT"))
  })
  implicit val uuidGenCodec: GenCodec[UUID] = transformed(_.toString, UUID.fromString)
  implicit val yearGenCodec: GenCodec[Year] = transformed(_.toString, Year.parse)
  implicit val yearMonthGenCodec: GenCodec[YearMonth] = transformed(_.toString, YearMonth.parse)
  implicit val zonedDateTimeGenCodec: GenCodec[ZonedDateTime] = transformed(_.toString, ZonedDateTime.parse)
  implicit val zoneIdGenCodec: GenCodec[ZoneId] = transformed(_.toString, ZoneId.of)
  implicit val zoneOffsetGenCodec: GenCodec[ZoneOffset] = transformed(_.toString, ZoneOffset.of)
  implicit val bitSetGenCodec: GenCodec[BitSet] =
    transformed(_.toArray, (arr: Array[Int]) => BitSet.fromBitMaskNoCopy(toBitMask(arr, Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))
  implicit val extractFieldsGenCodec: GenCodec[ExtractFields] = materializeRecursively
  implicit val geoJSONGenCodec: GenCodec[GeoJSON.GeoJSON] = materializeRecursively
  implicit val gitHubActionAPIGenCodec: GenCodec[GitHubActionsAPI.Response] = {
    object ArtifactBuilder {
      def apply(id: Long, node_id: String, name: String, size_in_bytes: Long, url: String, archive_download_url: String,
                expired: String, created_at: Instant, expires_at: Instant): GitHubActionsAPI.Artifact =
        GitHubActionsAPI.Artifact(id, node_id, name, size_in_bytes, url, archive_download_url,
          expired.toBoolean, created_at, expires_at)

      def unapply(a: GitHubActionsAPI.Artifact): Option[(Long, String, String, Long, String, String, String, Instant, Instant)] =
        Some((a.id, a.node_id, a.name, a.size_in_bytes, a.url, a.archive_download_url,
          a.expired.toString, a.created_at, a.expires_at))
    }

    implicit val c1: GenCodec[GitHubActionsAPI.Artifact] = fromApplyUnapplyProvider(ArtifactBuilder)
    materialize
  }
  implicit val googleMapsAPIGenCodec: GenCodec[GoogleMapsAPI.DistanceMatrix] = materializeRecursively
  implicit val intMapOfBooleansGenCodec: GenCodec[IntMap[Boolean]] =
    transformed(m => (m: Map[Int, Boolean]),
      (m: Map[Int, Boolean]) => m.foldLeft(IntMap.empty[Boolean])((im, p) => im.updated(p._1, p._2)))
  implicit val missingReqFieldGenCodec: GenCodec[MissingRequiredFields] = materializeRecursively
  implicit val mutableBitSetGenCodec: GenCodec[mutable.BitSet] =
    transformed(_.toArray, (a: Array[Int]) => mutable.BitSet.fromBitMaskNoCopy(toBitMask(a, Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))
  implicit val mutableLongMapOfBooleansGenCodec: GenCodec[mutable.LongMap[Boolean]] =
    transformed(m => (m: mutable.Map[Long, Boolean]),
      (m: mutable.Map[Long, Boolean]) => m.foldLeft(new mutable.LongMap[Boolean])((lm, p) => lm += (p._1, p._2)))
  implicit val nestedStructsGenCodec: GenCodec[NestedStructs] = materializeRecursively
  implicit val openRTBGenCodec: GenCodec[OpenRTB.BidRequest] = materializeRecursively
  implicit val primitivesGenCodec: GenCodec[Primitives] = materializeRecursively
  implicit val twitterAPIGenCodec: GenCodec[TwitterAPI.Tweet] = materializeRecursively
}
