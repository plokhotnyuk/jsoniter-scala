package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, ReaderConfig, WriterConfig}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig

import scala.collection.immutable.{BitSet, IntMap, Map, Set, Seq}
import scala.collection.mutable

object JsoniterScalaCodecs {
  val exceptionWithoutDumpConfig = ReaderConfig(appendHexDumpToParseException = false)
  val exceptionWithStacktraceConfig = ReaderConfig(throwReaderExceptionWithStackTrace = true)
  val escapingConfig = WriterConfig(escapeUnicode = true)
  val prettyConfig = WriterConfig(indentionStep = 2, preferredBufSize = 32768)
  val bigDecimalCodec: JsonValueCodec[BigDecimal] =
    make(CodecMakerConfig(bigDecimalDigitsLimit = Int.MaxValue, bigDecimalScaleLimit = Int.MaxValue, bigDecimalPrecision = 0)) /*WARNING: don't do this for open-systems*/
  val bigIntCodec: JsonValueCodec[BigInt] =
    make(CodecMakerConfig(bigIntDigitsLimit = Int.MaxValue)) /*WARNING: don't do this for open-systems*/
  val intCodec: JsonValueCodec[Int] = make(CodecMakerConfig()) // don't define implicit for supported types
  val stringCodec: JsonValueCodec[String] = make(CodecMakerConfig()) // don't define implicit for supported types
  implicit val adtCodec: JsonValueCodec[ADTBase] =
    make(CodecMakerConfig(allowRecursiveTypes = true /*WARNING: don't do this for open-systems*/))
  implicit val anyRefsCodec: JsonValueCodec[AnyRefs] = make(CodecMakerConfig())
  implicit val anyValsCodec: JsonValueCodec[AnyVals] = make(CodecMakerConfig())
  implicit val bigDecimalArrayCodec: JsonValueCodec[Array[BigDecimal]] =
    make(CodecMakerConfig(bigDecimalDigitsLimit = Int.MaxValue, bigDecimalScaleLimit = Int.MaxValue, bigDecimalPrecision = 0)) /*WARNING: don't do this for open-systems*/
  implicit val bigIntArrayCodec: JsonValueCodec[Array[BigInt]] = make(CodecMakerConfig(bigIntDigitsLimit = Int.MaxValue)) /*WARNING: don't do this for open-systems*/
  implicit val booleanArrayBufferCodec: JsonValueCodec[mutable.ArrayBuffer[Boolean]] = make(CodecMakerConfig())
  implicit val booleanArrayCodec: JsonValueCodec[Array[Boolean]] = make(CodecMakerConfig())
  implicit val booleanListCodec: JsonValueCodec[List[Boolean]] = make(CodecMakerConfig())
  implicit val booleanVectorCodec: JsonValueCodec[Vector[Boolean]] = make(CodecMakerConfig())
  implicit val byteArrayCodec: JsonValueCodec[Array[Byte]] = make(CodecMakerConfig())
  implicit val charArrayCodec: JsonValueCodec[Array[Char]] = make(CodecMakerConfig())
  implicit val doubleArrayCodec: JsonValueCodec[Array[Double]] = make(CodecMakerConfig())
  implicit val durationArrayCodec: JsonValueCodec[Array[Duration]] = make(CodecMakerConfig())
  implicit val enumArrayCodec: JsonValueCodec[Array[SuitEnum]] = make(CodecMakerConfig())
  implicit val enumADTArrayCodec: JsonValueCodec[Array[SuitADT]] = make(CodecMakerConfig(discriminatorFieldName = None))
  implicit val floatArrayCodec: JsonValueCodec[Array[Float]] = make(CodecMakerConfig())
  implicit val geoJSONCodec: JsonValueCodec[GeoJSON] =
    make(CodecMakerConfig(allowRecursiveTypes = true /*WARNING: don't do this for open-systems*/))
  implicit val instantArrayCodec: JsonValueCodec[Array[Instant]] = make(CodecMakerConfig())
  implicit val intArrayCodec: JsonValueCodec[Array[Int]] = make(CodecMakerConfig())
  implicit val javaEnumArrayCodec: JsonValueCodec[Array[Suit]] = make(CodecMakerConfig())
  implicit val longArrayCodec: JsonValueCodec[Array[Long]] = make(CodecMakerConfig())
  implicit val localDateArrayCodec: JsonValueCodec[Array[LocalDate]] = make(CodecMakerConfig())
  implicit val localDateTimeArrayCodec: JsonValueCodec[Array[LocalDateTime]] = make(CodecMakerConfig())
  implicit val localTimeArrayCodec: JsonValueCodec[Array[LocalTime]] = make(CodecMakerConfig())
  implicit val monthDayArrayCodec: JsonValueCodec[Array[MonthDay]] = make(CodecMakerConfig())
  implicit val nestedStructsCodec: JsonValueCodec[NestedStructs] =
    make(CodecMakerConfig(allowRecursiveTypes = true /*WARNING: don't do this for open-systems*/))
  implicit val offsetDateTimeArrayCodec: JsonValueCodec[Array[OffsetDateTime]] = make(CodecMakerConfig())
  implicit val offsetTimeArrayCodec: JsonValueCodec[Array[OffsetTime]] = make(CodecMakerConfig())
  implicit val periodArrayCodec: JsonValueCodec[Array[Period]] = make(CodecMakerConfig())
  implicit val shortArrayCodec: JsonValueCodec[Array[Short]] = make(CodecMakerConfig())
  implicit val uuidArrayCodec: JsonValueCodec[Array[UUID]] = make(CodecMakerConfig())
  implicit val yearArrayCodec: JsonValueCodec[Array[Year]] = make(CodecMakerConfig())
  implicit val yearMonthArrayCodec: JsonValueCodec[Array[YearMonth]] = make(CodecMakerConfig())
  implicit val zonedDateTimeArrayCodec: JsonValueCodec[Array[ZonedDateTime]] = make(CodecMakerConfig())
  implicit val zoneIdArrayCodec: JsonValueCodec[Array[ZoneId]] = make(CodecMakerConfig())
  implicit val zoneOffsetArrayCodec: JsonValueCodec[Array[ZoneOffset]] = make(CodecMakerConfig())
  implicit val bitSetCodec: JsonValueCodec[BitSet] =
    make(CodecMakerConfig(bitSetValueLimit = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val extractFieldsCodec: JsonValueCodec[ExtractFields] = make(CodecMakerConfig())
  implicit val intMapOfBooleansCodec: JsonValueCodec[IntMap[Boolean]] =
    make(CodecMakerConfig(mapMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val googleMapsAPICodec: JsonValueCodec[DistanceMatrix] = make(CodecMakerConfig())
  implicit val mapOfIntsToBooleansCodec: JsonValueCodec[Map[Int, Boolean]] =
    make(CodecMakerConfig(mapMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val missingReqFieldCodec: JsonValueCodec[MissingRequiredFields] = make(CodecMakerConfig())
  implicit val mutableBitSetCodec: JsonValueCodec[mutable.BitSet] =
    make(CodecMakerConfig(bitSetValueLimit = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val mutableLongMapOfBooleansCodec: JsonValueCodec[mutable.LongMap[Boolean]] =
    make(CodecMakerConfig(mapMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val mutableMapOfIntsToBooleansCodec: JsonValueCodec[mutable.Map[Int, Boolean]] =
    make(CodecMakerConfig(mapMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val mutableSetOfIntsCodec: JsonValueCodec[mutable.Set[Int]] =
    make(CodecMakerConfig(setMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val openHashMapOfIntsToBooleansCodec: JsonValueCodec[mutable.OpenHashMap[Int, Boolean]] =
    make(CodecMakerConfig(mapMaxInsertNumber = Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val primitivesCodec: JsonValueCodec[Primitives] = make(CodecMakerConfig())
  implicit val setOfIntsCodec: JsonValueCodec[Set[Int]] = make(CodecMakerConfig())
  implicit val twitterAPICodec: JsonValueCodec[Seq[Tweet]] = make(CodecMakerConfig())
}
