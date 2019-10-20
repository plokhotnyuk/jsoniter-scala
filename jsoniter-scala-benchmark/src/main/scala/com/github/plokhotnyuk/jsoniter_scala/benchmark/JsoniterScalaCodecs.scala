package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.{Base64, UUID}

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter, ReaderConfig, WriterConfig}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig

import scala.collection.immutable.{BitSet, IntMap, Map, Seq, Set}
import scala.collection.mutable

object JsoniterScalaCodecs {
  val exceptionWithoutDumpConfig: ReaderConfig = ReaderConfig.withAppendHexDumpToParseException(false)
  val exceptionWithStacktraceConfig: ReaderConfig = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredBufSize(1024 * 1024)
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2).withPreferredBufSize(32768)
  val base64Codec: JsonValueCodec[Array[Byte]] = // don't define implicit for supported types
    new JsonValueCodec[Array[Byte]] {
      override def decodeValue(in: JsonReader, default: Array[Byte]): Array[Byte] = {
        val arr = in.readRawValAsBytes()
        if (arr(0) != '"' || arr(arr.length - 1) != '"') in.decodeError("Expected string value")
        Base64.getDecoder.decode(java.nio.ByteBuffer.wrap(arr, 1, arr.length - 2)).array()
      }

      override def encodeValue(x: Array[Byte], out: JsonWriter): Unit = {
        val arr = Base64.getEncoder.encode(x)
        val rawVal = new Array[Byte](arr.length + 2)
        System.arraycopy(arr, 0, rawVal, 1, arr.length)
        rawVal(0) = '"'
        rawVal(arr.length + 1 ) = '"'
        out.writeRawVal(rawVal)
      }

      override val nullValue: Array[Byte] = new Array[Byte](0)
    }
  val bigDecimalCodec: JsonValueCodec[BigDecimal] =
    make(CodecMakerConfig.withBigDecimalDigitsLimit(Int.MaxValue).withBigDecimalScaleLimit(Int.MaxValue).withBigDecimalPrecision(0)) /*WARNING: don't do this for open-systems*/
  val bigIntCodec: JsonValueCodec[BigInt] =
    make(CodecMakerConfig.withBigIntDigitsLimit(Int.MaxValue)) // WARNING: don't do this for open-systems
  val intCodec: JsonValueCodec[Int] = make(CodecMakerConfig) // don't define implicit for supported types
  val stringCodec: JsonValueCodec[String] = make(CodecMakerConfig) // don't define implicit for supported types
  implicit val adtCodec: JsonValueCodec[ADTBase] =
    make(CodecMakerConfig.withAllowRecursiveTypes(true)) // WARNING: don't do this for open-systems
  implicit val anyRefsCodec: JsonValueCodec[AnyRefs] = make(CodecMakerConfig)
  implicit val anyValsCodec: JsonValueCodec[AnyVals] = make(CodecMakerConfig)
  implicit val bigDecimalArrayCodec: JsonValueCodec[Array[BigDecimal]] = make(CodecMakerConfig)
  implicit val bigIntArrayCodec: JsonValueCodec[Array[BigInt]] = make(CodecMakerConfig)
  implicit val booleanArrayBufferCodec: JsonValueCodec[mutable.ArrayBuffer[Boolean]] = make(CodecMakerConfig)
  implicit val booleanArrayCodec: JsonValueCodec[Array[Boolean]] = make(CodecMakerConfig)
  implicit val booleanListCodec: JsonValueCodec[List[Boolean]] = make(CodecMakerConfig)
  implicit val booleanVectorCodec: JsonValueCodec[Vector[Boolean]] = make(CodecMakerConfig)
  implicit val byteArrayCodec: JsonValueCodec[Array[Byte]] = make(CodecMakerConfig)
  implicit val charArrayCodec: JsonValueCodec[Array[Char]] = make(CodecMakerConfig)
  implicit val doubleArrayCodec: JsonValueCodec[Array[Double]] = make(CodecMakerConfig)
  implicit val durationArrayCodec: JsonValueCodec[Array[Duration]] = make(CodecMakerConfig)
  implicit val enumArrayCodec: JsonValueCodec[Array[SuitEnum]] = make(CodecMakerConfig)
  implicit val enumADTArrayCodec: JsonValueCodec[Array[SuitADT]] = make(CodecMakerConfig.withDiscriminatorFieldName(None))
  implicit val floatArrayCodec: JsonValueCodec[Array[Float]] = make(CodecMakerConfig)
  implicit val geoJSONCodec: JsonValueCodec[GeoJSON.GeoJSON] = make(CodecMakerConfig)
  implicit val instantArrayCodec: JsonValueCodec[Array[Instant]] = make(CodecMakerConfig)
  implicit val intArrayCodec: JsonValueCodec[Array[Int]] = make(CodecMakerConfig)
  implicit val javaEnumArrayCodec: JsonValueCodec[Array[Suit]] = make(CodecMakerConfig)
  implicit val longArrayCodec: JsonValueCodec[Array[Long]] = make(CodecMakerConfig)
  implicit val localDateArrayCodec: JsonValueCodec[Array[LocalDate]] = make(CodecMakerConfig)
  implicit val localDateTimeArrayCodec: JsonValueCodec[Array[LocalDateTime]] = make(CodecMakerConfig)
  implicit val localTimeArrayCodec: JsonValueCodec[Array[LocalTime]] = make(CodecMakerConfig)
  implicit val monthDayArrayCodec: JsonValueCodec[Array[MonthDay]] = make(CodecMakerConfig)
  implicit val nestedStructsCodec: JsonValueCodec[NestedStructs] =
    make(CodecMakerConfig.withAllowRecursiveTypes(true)) // WARNING: don't do this for open-systems
  implicit val offsetDateTimeArrayCodec: JsonValueCodec[Array[OffsetDateTime]] = make(CodecMakerConfig)
  implicit val offsetTimeArrayCodec: JsonValueCodec[Array[OffsetTime]] = make(CodecMakerConfig)
  implicit val openRTB25Codec: JsonValueCodec[OpenRTB.BidRequest] = make(CodecMakerConfig)
  implicit val periodArrayCodec: JsonValueCodec[Array[Period]] = make(CodecMakerConfig)
  implicit val shortArrayCodec: JsonValueCodec[Array[Short]] = make(CodecMakerConfig)
  implicit val uuidArrayCodec: JsonValueCodec[Array[UUID]] = make(CodecMakerConfig)
  implicit val yearArrayCodec: JsonValueCodec[Array[Year]] = make(CodecMakerConfig)
  implicit val yearMonthArrayCodec: JsonValueCodec[Array[YearMonth]] = make(CodecMakerConfig)
  implicit val zonedDateTimeArrayCodec: JsonValueCodec[Array[ZonedDateTime]] = make(CodecMakerConfig)
  implicit val zoneIdArrayCodec: JsonValueCodec[Array[ZoneId]] = make(CodecMakerConfig)
  implicit val zoneOffsetArrayCodec: JsonValueCodec[Array[ZoneOffset]] = make(CodecMakerConfig)
  implicit val bitSetCodec: JsonValueCodec[BitSet] =
    make(CodecMakerConfig.withBitSetValueLimit(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val extractFieldsCodec: JsonValueCodec[ExtractFields] = make(CodecMakerConfig)
  implicit val intMapOfBooleansCodec: JsonValueCodec[IntMap[Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val googleMapsAPICodec: JsonValueCodec[GoogleMapsAPI.DistanceMatrix] = make(CodecMakerConfig)
  implicit val mapOfIntsToBooleansCodec: JsonValueCodec[Map[Int, Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val missingReqFieldCodec: JsonValueCodec[MissingRequiredFields] = make(CodecMakerConfig)
  implicit val mutableBitSetCodec: JsonValueCodec[mutable.BitSet] =
    make(CodecMakerConfig.withBitSetValueLimit(Int.MaxValue /*WARNING: don't do this for open-systems*/))
  implicit val mutableLongMapOfBooleansCodec: JsonValueCodec[mutable.LongMap[Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val mutableMapOfIntsToBooleansCodec: JsonValueCodec[mutable.Map[Int, Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val mutableSetOfIntsCodec: JsonValueCodec[mutable.Set[Int]] =
    make(CodecMakerConfig.withSetMaxInsertNumber(Int.MaxValue)) // WARNING: don't do this for open-systems
  implicit val primitivesCodec: JsonValueCodec[Primitives] = make(CodecMakerConfig)
  implicit val setOfIntsCodec: JsonValueCodec[Set[Int]] = make(CodecMakerConfig)
  implicit val twitterAPICodec: JsonValueCodec[Seq[TwitterAPI.Tweet]] = make(CodecMakerConfig)
}
