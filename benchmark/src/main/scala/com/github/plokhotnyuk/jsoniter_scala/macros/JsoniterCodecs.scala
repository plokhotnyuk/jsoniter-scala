package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig =
    ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val anyRefsCodec: JsonCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  val arraysCodec: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())
  val bigDecimalArrayCodec: JsonCodec[Array[BigDecimal]] = make[Array[BigDecimal]](CodecMakerConfig())
  val bigIntArrayCodec: JsonCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  val bitSetsCodec: JsonCodec[BitSets] = make[BitSets](CodecMakerConfig())
  val booleanArrayCodec: JsonCodec[Array[Boolean]] = make[Array[Boolean]](CodecMakerConfig())
  val byteArrayCodec: JsonCodec[Array[Byte]] = make[Array[Byte]](CodecMakerConfig())
  val doubleArrayCodec: JsonCodec[Array[Double]] = make[Array[Double]](CodecMakerConfig())
  val durationArrayCodec: JsonCodec[Array[Duration]] = make[Array[Duration]](CodecMakerConfig())
  val floatArrayCodec: JsonCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  val instantArrayCodec: JsonCodec[Array[Instant]] = make[Array[Instant]](CodecMakerConfig())
  val localDateArrayCodec: JsonCodec[Array[LocalDate]] = make[Array[LocalDate]](CodecMakerConfig())
  val localDateTimeArrayCodec: JsonCodec[Array[LocalDateTime]] = make[Array[LocalDateTime]](CodecMakerConfig())
  val offsetDateTimeArrayCodec: JsonCodec[Array[OffsetDateTime]] = make[Array[OffsetDateTime]](CodecMakerConfig())
  val localTimeArrayCodec: JsonCodec[Array[LocalTime]] = make[Array[LocalTime]](CodecMakerConfig())
  val periodArrayCodec: JsonCodec[Array[Period]] = make[Array[Period]](CodecMakerConfig())
  val zoneOffsetArrayCodec: JsonCodec[Array[ZoneOffset]] = make[Array[ZoneOffset]](CodecMakerConfig())
  val zoneIdArrayCodec: JsonCodec[Array[ZoneId]] = make[Array[ZoneId]](CodecMakerConfig())
  val intArrayCodec: JsonCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
  val shortArrayCodec: JsonCodec[Array[Short]] = make[Array[Short]](CodecMakerConfig())
  val longArrayCodec: JsonCodec[Array[Long]] = make[Array[Long]](CodecMakerConfig())
  val uuidArrayCodec: JsonCodec[Array[UUID]] = make[Array[UUID]](CodecMakerConfig())
  val iterablesCodec: JsonCodec[Iterables] = make[Iterables](CodecMakerConfig())
  val mapsCodec: JsonCodec[Maps] = make[Maps](CodecMakerConfig())
  val missingReqFieldCodec: JsonCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  val mutableIterablesCodec: JsonCodec[MutableIterables] = make[MutableIterables](CodecMakerConfig())
  val mutableMapsCodec: JsonCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())
  val intAndLongMapsCodec: JsonCodec[IntAndLongMaps] = make[IntAndLongMaps](CodecMakerConfig())
  val primitivesCodec: JsonCodec[Primitives] = make[Primitives](CodecMakerConfig())
  val extractFieldsCodec: JsonCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  val adtCodec: JsonCodec[AdtBase] = make[AdtBase](CodecMakerConfig())
  val stringCodec: JsonCodec[String] = make[String](CodecMakerConfig())
  val googleMapsAPICodec: JsonCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  val twitterAPICodec: JsonCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
}
