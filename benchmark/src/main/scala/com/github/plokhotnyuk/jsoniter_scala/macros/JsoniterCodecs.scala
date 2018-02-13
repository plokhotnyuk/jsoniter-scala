package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig =
    ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  implicit val anyRefsCodec: JsonCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  implicit val arraysCodec: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())
  implicit val bigDecimalArrayCodec: JsonCodec[Array[BigDecimal]] = make[Array[BigDecimal]](CodecMakerConfig())
  implicit val bigIntArrayCodec: JsonCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  implicit val bitSetsCodec: JsonCodec[BitSets] = make[BitSets](CodecMakerConfig())
  implicit val booleanArrayCodec: JsonCodec[Array[Boolean]] = make[Array[Boolean]](CodecMakerConfig())
  implicit val byteArrayCodec: JsonCodec[Array[Byte]] = make[Array[Byte]](CodecMakerConfig())
  implicit val doubleArrayCodec: JsonCodec[Array[Double]] = make[Array[Double]](CodecMakerConfig())
  implicit val durationArrayCodec: JsonCodec[Array[Duration]] = make[Array[Duration]](CodecMakerConfig())
  implicit val floatArrayCodec: JsonCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  implicit val instantArrayCodec: JsonCodec[Array[Instant]] = make[Array[Instant]](CodecMakerConfig())
  implicit val localDateArrayCodec: JsonCodec[Array[LocalDate]] = make[Array[LocalDate]](CodecMakerConfig())
  implicit val localDateTimeArrayCodec: JsonCodec[Array[LocalDateTime]] = make[Array[LocalDateTime]](CodecMakerConfig())
  implicit val offsetDateTimeArrayCodec: JsonCodec[Array[OffsetDateTime]] = make[Array[OffsetDateTime]](CodecMakerConfig())
  implicit val localTimeArrayCodec: JsonCodec[Array[LocalTime]] = make[Array[LocalTime]](CodecMakerConfig())
  implicit val periodArrayCodec: JsonCodec[Array[Period]] = make[Array[Period]](CodecMakerConfig())
  implicit val zonedDateTimeArrayCodec: JsonCodec[Array[ZonedDateTime]] = make[Array[ZonedDateTime]](CodecMakerConfig())
  implicit val zoneOffsetArrayCodec: JsonCodec[Array[ZoneOffset]] = make[Array[ZoneOffset]](CodecMakerConfig())
  implicit val zoneIdArrayCodec: JsonCodec[Array[ZoneId]] = make[Array[ZoneId]](CodecMakerConfig())
  implicit val javaEnumArrayCodec: JsonCodec[Array[Suit]] = make[Array[Suit]](CodecMakerConfig())
  implicit val enumArrayCodec: JsonCodec[Array[SuitEnum]] = make[Array[SuitEnum]](CodecMakerConfig())
  implicit val intArrayCodec: JsonCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
  implicit val shortArrayCodec: JsonCodec[Array[Short]] = make[Array[Short]](CodecMakerConfig())
  implicit val longArrayCodec: JsonCodec[Array[Long]] = make[Array[Long]](CodecMakerConfig())
  implicit val uuidArrayCodec: JsonCodec[Array[UUID]] = make[Array[UUID]](CodecMakerConfig())
  implicit val iterablesCodec: JsonCodec[Iterables] = make[Iterables](CodecMakerConfig())
  implicit val mapsCodec: JsonCodec[Maps] = make[Maps](CodecMakerConfig())
  implicit val missingReqFieldCodec: JsonCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  implicit val mutableIterablesCodec: JsonCodec[MutableIterables] = make[MutableIterables](CodecMakerConfig())
  implicit val mutableMapsCodec: JsonCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())
  implicit val intAndLongMapsCodec: JsonCodec[IntAndLongMaps] = make[IntAndLongMaps](CodecMakerConfig())
  implicit val primitivesCodec: JsonCodec[Primitives] = make[Primitives](CodecMakerConfig())
  implicit val extractFieldsCodec: JsonCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  implicit val adtCodec: JsonCodec[AdtBase] = make[AdtBase](CodecMakerConfig())
  implicit val stringCodec: JsonCodec[String] = make[String](CodecMakerConfig())
  implicit val googleMapsAPICodec: JsonCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  implicit val twitterAPICodec: JsonCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
}
