package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.{BitSet, IntMap, Map, Set}
import scala.collection.mutable

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig =
    ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val intCodec: JsonValueCodec[Int] = make[Int](CodecMakerConfig()) // don't define implicit for supported types
  val stringCodec: JsonValueCodec[String] = make[String](CodecMakerConfig()) // don't define implicit for supported types
  val adtCodec: JsonValueCodec[AdtBase] = make[AdtBase](CodecMakerConfig()) // don't define implicit for recursive structures
  val geoJSONCodec: JsonValueCodec[GeoJSON] = make[GeoJSON](CodecMakerConfig()) // don't define implicit for recursive structures
  implicit val anyRefsCodec: JsonValueCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  implicit val bigDecimalArrayCodec: JsonValueCodec[Array[BigDecimal]] = make[Array[BigDecimal]](CodecMakerConfig())
  implicit val bigIntArrayCodec: JsonValueCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  implicit val booleanArrayBufferCodec: JsonValueCodec[mutable.ArrayBuffer[Boolean]] = make[mutable.ArrayBuffer[Boolean]](CodecMakerConfig())
  implicit val booleanArrayCodec: JsonValueCodec[Array[Boolean]] = make[Array[Boolean]](CodecMakerConfig())
  implicit val booleanListCodec: JsonValueCodec[List[Boolean]] = make[List[Boolean]](CodecMakerConfig())
  implicit val booleanVectorCodec: JsonValueCodec[Vector[Boolean]] = make[Vector[Boolean]](CodecMakerConfig())
  implicit val byteArrayCodec: JsonValueCodec[Array[Byte]] = make[Array[Byte]](CodecMakerConfig())
  implicit val charArrayCodec: JsonValueCodec[Array[Char]] = make[Array[Char]](CodecMakerConfig())
  implicit val doubleArrayCodec: JsonValueCodec[Array[Double]] = make[Array[Double]](CodecMakerConfig())
  implicit val durationArrayCodec: JsonValueCodec[Array[Duration]] = make[Array[Duration]](CodecMakerConfig())
  implicit val enumArrayCodec: JsonValueCodec[Array[SuitEnum]] = make[Array[SuitEnum]](CodecMakerConfig())
  implicit val floatArrayCodec: JsonValueCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  implicit val instantArrayCodec: JsonValueCodec[Array[Instant]] = make[Array[Instant]](CodecMakerConfig())
  implicit val intArrayCodec: JsonValueCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
  implicit val javaEnumArrayCodec: JsonValueCodec[Array[Suit]] = make[Array[Suit]](CodecMakerConfig())
  implicit val longArrayCodec: JsonValueCodec[Array[Long]] = make[Array[Long]](CodecMakerConfig())
  implicit val localDateArrayCodec: JsonValueCodec[Array[LocalDate]] = make[Array[LocalDate]](CodecMakerConfig())
  implicit val localDateTimeArrayCodec: JsonValueCodec[Array[LocalDateTime]] = make[Array[LocalDateTime]](CodecMakerConfig())
  implicit val localTimeArrayCodec: JsonValueCodec[Array[LocalTime]] = make[Array[LocalTime]](CodecMakerConfig())
  implicit val offsetDateTimeArrayCodec: JsonValueCodec[Array[OffsetDateTime]] = make[Array[OffsetDateTime]](CodecMakerConfig())
  implicit val offsetTimeArrayCodec: JsonValueCodec[Array[OffsetTime]] = make[Array[OffsetTime]](CodecMakerConfig())
  implicit val periodArrayCodec: JsonValueCodec[Array[Period]] = make[Array[Period]](CodecMakerConfig())
  implicit val shortArrayCodec: JsonValueCodec[Array[Short]] = make[Array[Short]](CodecMakerConfig())
  implicit val uuidArrayCodec: JsonValueCodec[Array[UUID]] = make[Array[UUID]](CodecMakerConfig())
  implicit val yearArrayCodec: JsonValueCodec[Array[Year]] = make[Array[Year]](CodecMakerConfig())
  implicit val yearMonthArrayCodec: JsonValueCodec[Array[YearMonth]] = make[Array[YearMonth]](CodecMakerConfig())
  implicit val zonedDateTimeArrayCodec: JsonValueCodec[Array[ZonedDateTime]] = make[Array[ZonedDateTime]](CodecMakerConfig())
  implicit val zoneIdArrayCodec: JsonValueCodec[Array[ZoneId]] = make[Array[ZoneId]](CodecMakerConfig())
  implicit val zoneOffsetArrayCodec: JsonValueCodec[Array[ZoneOffset]] = make[Array[ZoneOffset]](CodecMakerConfig())
  implicit val bitSetCodec: JsonValueCodec[BitSet] = make[BitSet](CodecMakerConfig(bitSetValueLimit = Int.MaxValue /*WARNING: don't do this for open-system*/))
  implicit val extractFieldsCodec: JsonValueCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  implicit val intMapOfBooleansCodec: JsonValueCodec[IntMap[Boolean]] = make[IntMap[Boolean]](CodecMakerConfig())
  implicit val googleMapsAPICodec: JsonValueCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  implicit val mapOfIntsToBooleansCodec: JsonValueCodec[Map[Int, Boolean]] = make[Map[Int, Boolean]](CodecMakerConfig())
  implicit val missingReqFieldCodec: JsonValueCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  implicit val mutableBitSetCodec: JsonValueCodec[mutable.BitSet] = make[mutable.BitSet](CodecMakerConfig(bitSetValueLimit = Int.MaxValue /*WARNING: don't do this for open-system*/))
  implicit val mutableLongMapOfBooleansCodec: JsonValueCodec[mutable.LongMap[Boolean]] = make[mutable.LongMap[Boolean]](CodecMakerConfig())
  implicit val mutableMapOfIntsToBooleansCodec: JsonValueCodec[mutable.Map[Int, Boolean]] = make[mutable.Map[Int, Boolean]](CodecMakerConfig())
  implicit val mutableSetOfIntsCodec: JsonValueCodec[mutable.Set[Int]] = make[mutable.Set[Int]](CodecMakerConfig())
  implicit val primitivesCodec: JsonValueCodec[Primitives] = make[Primitives](CodecMakerConfig())
  implicit val setOfIntsCodec: JsonValueCodec[Set[Int]] = make[Set[Int]](CodecMakerConfig())
  implicit val twitterAPICodec: JsonValueCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
}
