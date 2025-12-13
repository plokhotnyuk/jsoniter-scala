package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.blocks.schema.json._
import zio.blocks.schema._
import java.time._
import java.util.UUID
import scala.collection.immutable.ArraySeq

object ZioBlocksCodecs {
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2)
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val arrayOfBigDecimalsCodec: JsonBinaryCodec[Array[BigDecimal]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfBigIntsCodec: JsonBinaryCodec[Array[BigInt]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfBooleansCodec: JsonBinaryCodec[Array[Boolean]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfBytesCodec: JsonBinaryCodec[Array[Byte]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfCharsCodec: JsonBinaryCodec[Array[Char]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfDoublesCodec: JsonBinaryCodec[Array[Double]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfDurationsCodec: JsonBinaryCodec[Array[Duration]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfEnumADTsCodec: JsonBinaryCodec[Array[SuitADT]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfFloatsCodec: JsonBinaryCodec[Array[Float]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfInstantsCodec: JsonBinaryCodec[Array[Instant]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfIntsCodec: JsonBinaryCodec[Array[Int]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfLocalDatesCodec: JsonBinaryCodec[Array[LocalDate]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfLocalDateTimesCodec: JsonBinaryCodec[Array[LocalDateTime]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfLocalTimesCodec: JsonBinaryCodec[Array[LocalTime]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfLongsCodec: JsonBinaryCodec[Array[Long]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfMonthDaysCodec: JsonBinaryCodec[Array[MonthDay]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfOffsetDateTimesCodec: JsonBinaryCodec[Array[OffsetDateTime]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfOffsetTimesCodec: JsonBinaryCodec[Array[OffsetTime]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfPeriodsCodec: JsonBinaryCodec[Array[Period]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfShortsCodec: JsonBinaryCodec[Array[Short]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfUUIDsCodec: JsonBinaryCodec[Array[UUID]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfYearMonthsCodec: JsonBinaryCodec[Array[YearMonth]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfYearsCodec: JsonBinaryCodec[Array[Year]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfZonedDateTimesCodec: JsonBinaryCodec[Array[ZonedDateTime]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfZoneIdsCodec: JsonBinaryCodec[Array[ZoneId]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arrayOfZoneOffsetsCodec: JsonBinaryCodec[Array[ZoneOffset]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val arraySeqOfBooleansCodec: JsonBinaryCodec[ArraySeq[Boolean]] = Schema[ArraySeq[Boolean]].derive(JsonBinaryCodecDeriver)
  val extractFieldsCodec: JsonBinaryCodec[ExtractFields] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val geoJsonCodec: JsonBinaryCodec[GeoJSON.GeoJSON] =
    Schema.derived.derive(JsonBinaryCodecDeriver.withDiscriminatorKind(DiscriminatorKind.Field("type")))
  val gitHubActionsAPICodec: JsonBinaryCodec[GitHubActionsAPI.Response] =
    Schema.derived
      .deriving(JsonBinaryCodecDeriver)
      .instance(TypeName.boolean, new JsonBinaryCodec[Boolean](JsonBinaryCodec.booleanType) {
        override def decodeValue(in: JsonReader, default: Boolean): Boolean = in.readStringAsBoolean()

        override def encodeValue(x: Boolean, out: JsonWriter): Unit = out.writeValAsString(x)
      })
      .derive
  val googleMapsAPICodec: JsonBinaryCodec[GoogleMapsAPI.DistanceMatrix] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val listOfBooleansCodec: JsonBinaryCodec[List[Boolean]] = Schema[List[Boolean]].derive(JsonBinaryCodecDeriver)
  val mapOfIntsToBooleansCodec: JsonBinaryCodec[Map[Int, Boolean]] = Schema[Map[Int, Boolean]].derive(JsonBinaryCodecDeriver)
  val missingRequiredFieldsCodec: JsonBinaryCodec[MissingRequiredFields] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val nestedStructsCodec: JsonBinaryCodec[NestedStructs] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val openRTBBidRequestCodec: JsonBinaryCodec[OpenRTB.BidRequest] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val setOfIntsCodec: JsonBinaryCodec[Set[Int]] = Schema[Set[Int]].derive(JsonBinaryCodecDeriver)
  val stringCodec: JsonBinaryCodec[String] = Schema[String].derive(JsonBinaryCodecDeriver)
  val twitterAPICodec: JsonBinaryCodec[Seq[TwitterAPI.Tweet]] = Schema.derived.derive(JsonBinaryCodecDeriver)
  val vectorOfBooleansCodec: JsonBinaryCodec[Vector[Boolean]] = Schema[Vector[Boolean]].derive(JsonBinaryCodecDeriver)
}
