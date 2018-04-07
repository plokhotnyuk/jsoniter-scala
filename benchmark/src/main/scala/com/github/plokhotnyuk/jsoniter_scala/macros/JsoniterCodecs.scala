package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig =
    ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val intCodec: JsonValueCodec[Int] = make[Int](CodecMakerConfig()) // don't define implicit for supported types
  val stringCodec: JsonValueCodec[String] = make[String](CodecMakerConfig()) // don't define implicit for supported types
  implicit val anyRefsCodec: JsonValueCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  implicit val arraysCodec: JsonValueCodec[Arrays] = make[Arrays](CodecMakerConfig())
  implicit val bigDecimalArrayCodec: JsonValueCodec[Array[BigDecimal]] = make[Array[BigDecimal]](CodecMakerConfig())
  implicit val bigIntArrayCodec: JsonValueCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  implicit val bitSetsCodec: JsonValueCodec[BitSets] = make[BitSets](CodecMakerConfig())
  implicit val booleanArrayCodec: JsonValueCodec[Array[Boolean]] = make[Array[Boolean]](CodecMakerConfig())
  implicit val byteArrayCodec: JsonValueCodec[Array[Byte]] = make[Array[Byte]](CodecMakerConfig())
  implicit val charArrayCodec: JsonValueCodec[Array[Char]] = make[Array[Char]](CodecMakerConfig())
  implicit val doubleArrayCodec: JsonValueCodec[Array[Double]] = make[Array[Double]](CodecMakerConfig())
  implicit val durationArrayCodec: JsonValueCodec[Array[Duration]] = make[Array[Duration]](CodecMakerConfig())
  implicit val floatArrayCodec: JsonValueCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  implicit val instantArrayCodec: JsonValueCodec[Array[Instant]] = make[Array[Instant]](CodecMakerConfig())
  implicit val localDateArrayCodec: JsonValueCodec[Array[LocalDate]] = make[Array[LocalDate]](CodecMakerConfig())
  implicit val localDateTimeArrayCodec: JsonValueCodec[Array[LocalDateTime]] = make[Array[LocalDateTime]](CodecMakerConfig())
  implicit val offsetDateTimeArrayCodec: JsonValueCodec[Array[OffsetDateTime]] = make[Array[OffsetDateTime]](CodecMakerConfig())
  implicit val offsetTimeArrayCodec: JsonValueCodec[Array[OffsetTime]] = make[Array[OffsetTime]](CodecMakerConfig())
  implicit val localTimeArrayCodec: JsonValueCodec[Array[LocalTime]] = make[Array[LocalTime]](CodecMakerConfig())
  implicit val periodArrayCodec: JsonValueCodec[Array[Period]] = make[Array[Period]](CodecMakerConfig())
  implicit val zonedDateTimeArrayCodec: JsonValueCodec[Array[ZonedDateTime]] = make[Array[ZonedDateTime]](CodecMakerConfig())
  implicit val zoneOffsetArrayCodec: JsonValueCodec[Array[ZoneOffset]] = make[Array[ZoneOffset]](CodecMakerConfig())
  implicit val zoneIdArrayCodec: JsonValueCodec[Array[ZoneId]] = make[Array[ZoneId]](CodecMakerConfig())
  implicit val javaEnumArrayCodec: JsonValueCodec[Array[Suit]] = make[Array[Suit]](CodecMakerConfig())
  implicit val enumArrayCodec: JsonValueCodec[Array[SuitEnum]] = make[Array[SuitEnum]](CodecMakerConfig())
  implicit val intArrayCodec: JsonValueCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
  implicit val shortArrayCodec: JsonValueCodec[Array[Short]] = make[Array[Short]](CodecMakerConfig())
  implicit val longArrayCodec: JsonValueCodec[Array[Long]] = make[Array[Long]](CodecMakerConfig())
  implicit val uuidArrayCodec: JsonValueCodec[Array[UUID]] = make[Array[UUID]](CodecMakerConfig())
  implicit val iterablesCodec: JsonValueCodec[Iterables] = make[Iterables](CodecMakerConfig())
  implicit val mapsCodec: JsonValueCodec[Maps] = make[Maps](CodecMakerConfig())
  implicit val missingReqFieldCodec: JsonValueCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  implicit val mutableIterablesCodec: JsonValueCodec[MutableIterables] = make[MutableIterables](CodecMakerConfig())
  implicit val mutableMapsCodec: JsonValueCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())
  implicit val intAndLongMapsCodec: JsonValueCodec[IntAndLongMaps] = make[IntAndLongMaps](CodecMakerConfig())
  implicit val primitivesCodec: JsonValueCodec[Primitives] = make[Primitives](CodecMakerConfig())
  implicit val extractFieldsCodec: JsonValueCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  implicit val adtCodec: JsonValueCodec[AdtBase] = make[AdtBase](CodecMakerConfig())
  implicit val googleMapsAPICodec: JsonValueCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  implicit val twitterAPICodec: JsonValueCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
}
