package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import scala.collection.immutable._
import scala.collection.mutable

object JsoniterScalaCodecs {
  //implicit val printCodec: CodecMakerConfig.PrintCodec = new CodecMakerConfig.PrintCodec {}
  val exceptionWithoutDumpConfig: ReaderConfig = ReaderConfig.withAppendHexDumpToParseException(false)
  val exceptionWithStacktraceConfig: ReaderConfig = ReaderConfig.withThrowReaderExceptionWithStackTrace(true)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredCharBufSize(1024 * 1024)
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2)
  val base16Codec: JsonValueCodec[Array[Byte]] = new JsonValueCodec[Array[Byte]] {
    override def decodeValue(in: JsonReader, default: Array[Byte]): Array[Byte] = in.readBase16AsBytes(default)

    override def encodeValue(x: Array[Byte], out: JsonWriter): Unit = out.writeBase16Val(x, lowerCase = true)

    override val nullValue: Array[Byte] = new Array[Byte](0)
  }
  val base64Codec: JsonValueCodec[Array[Byte]] = new JsonValueCodec[Array[Byte]] {
    override def decodeValue(in: JsonReader, default: Array[Byte]): Array[Byte] = in.readBase64AsBytes(default)

    override def encodeValue(x: Array[Byte], out: JsonWriter): Unit = out.writeBase64Val(x, doPadding = true)

    override val nullValue: Array[Byte] = new Array[Byte](0)
  }
  val bigDecimalCodec: JsonValueCodec[BigDecimal] =
    make(CodecMakerConfig.withBigDecimalDigitsLimit(Int.MaxValue).withBigDecimalScaleLimit(Int.MaxValue).withBigDecimalPrecision(0)) /* WARNING: It is an unsafe option for open systems */
  val bigIntCodec: JsonValueCodec[BigInt] =
    make(CodecMakerConfig.withBigIntDigitsLimit(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  val stringCodec: JsonValueCodec[String] = make
  implicit val bigDecimalArrayCodec: JsonValueCodec[Array[BigDecimal]] = make
  implicit val bigIntArrayCodec: JsonValueCodec[Array[BigInt]] = make
  implicit val booleanArrayCodec: JsonValueCodec[Array[Boolean]] = make
  implicit val booleanArraySeqCodec: JsonValueCodec[ArraySeq[Boolean]] = make
  implicit val booleanListCodec: JsonValueCodec[List[Boolean]] = make
  implicit val booleanVectorCodec: JsonValueCodec[Vector[Boolean]] = make
  implicit val byteArrayCodec: JsonValueCodec[Array[Byte]] = make
  implicit val charArrayCodec: JsonValueCodec[Array[Char]] = make
  implicit val doubleArrayCodec: JsonValueCodec[Array[Double]] = make
  implicit val durationArrayCodec: JsonValueCodec[Array[Duration]] = make
  implicit val enumArrayCodec: JsonValueCodec[Array[SuitEnum]] = make
  implicit val enumADTArrayCodec: JsonValueCodec[Array[SuitADT]] =
    make(CodecMakerConfig.withDiscriminatorFieldName(None))
  implicit val floatArrayCodec: JsonValueCodec[Array[Float]] = make
  implicit val geoJSONCodec: JsonValueCodec[GeoJSON.GeoJSON] = make(CodecMakerConfig.withTransientDefault(false))
  implicit val gitHubActionsAPICodec: JsonValueCodec[GitHubActionsAPI.Response] =
    make(CodecMakerConfig.withCheckFieldDuplication(false))
  implicit val instantArrayCodec: JsonValueCodec[Array[Instant]] = make
  implicit val intArrayCodec: JsonValueCodec[Array[Int]] = make
  implicit val javaEnumArrayCodec: JsonValueCodec[Array[Suit]] = make
  implicit val longArrayCodec: JsonValueCodec[Array[Long]] = make
  implicit val localDateArrayCodec: JsonValueCodec[Array[LocalDate]] = make
  implicit val localDateTimeArrayCodec: JsonValueCodec[Array[LocalDateTime]] = make
  implicit val localTimeArrayCodec: JsonValueCodec[Array[LocalTime]] = make
  implicit val monthDayArrayCodec: JsonValueCodec[Array[MonthDay]] = make
  implicit val nestedStructsCodec: JsonValueCodec[NestedStructs] =
    make(CodecMakerConfig.withCheckFieldDuplication(false).withTransientDefault(false).withAllowRecursiveTypes(true)) // WARNING: It is an unsafe option for open systems
  implicit val offsetDateTimeArrayCodec: JsonValueCodec[Array[OffsetDateTime]] = make
  implicit val offsetTimeArrayCodec: JsonValueCodec[Array[OffsetTime]] = make
  implicit val openRTB25Codec: JsonValueCodec[OpenRTB.BidRequest] = make
  implicit val periodArrayCodec: JsonValueCodec[Array[Period]] = make
  implicit val shortArrayCodec: JsonValueCodec[Array[Short]] = make
  implicit val uuidArrayCodec: JsonValueCodec[Array[UUID]] = make
  implicit val yearArrayCodec: JsonValueCodec[Array[Year]] = make
  implicit val yearMonthArrayCodec: JsonValueCodec[Array[YearMonth]] = make
  implicit val zonedDateTimeArrayCodec: JsonValueCodec[Array[ZonedDateTime]] = make
  implicit val zoneIdArrayCodec: JsonValueCodec[Array[ZoneId]] = make
  implicit val zoneOffsetArrayCodec: JsonValueCodec[Array[ZoneOffset]] = make
  implicit val bitSetCodec: JsonValueCodec[BitSet] =
    make(CodecMakerConfig.withBitSetValueLimit(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val extractFieldsCodec: JsonValueCodec[ExtractFields] =
    make(CodecMakerConfig.withCheckFieldDuplication(false))
  implicit val intMapOfBooleansCodec: JsonValueCodec[IntMap[Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val googleMapsAPICodec: JsonValueCodec[GoogleMapsAPI.DistanceMatrix] =
    make(CodecMakerConfig.withCheckFieldDuplication(false))
  implicit val mapOfIntsToBooleansCodec: JsonValueCodec[Map[Int, Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val missingReqFieldCodec: JsonValueCodec[MissingRequiredFields] =
    make(CodecMakerConfig.withCheckFieldDuplication(false))
  implicit val mutableBitSetCodec: JsonValueCodec[mutable.BitSet] =
    make(CodecMakerConfig.withBitSetValueLimit(Int.MaxValue /* WARNING: It is an unsafe option for open systems */))
  implicit val mutableLongMapOfBooleansCodec: JsonValueCodec[mutable.LongMap[Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val mutableMapOfIntsToBooleansCodec: JsonValueCodec[mutable.Map[Int, Boolean]] =
    make(CodecMakerConfig.withMapMaxInsertNumber(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val mutableSetOfIntsCodec: JsonValueCodec[mutable.Set[Int]] =
    make(CodecMakerConfig.withSetMaxInsertNumber(Int.MaxValue)) // WARNING: It is an unsafe option for open systems
  implicit val setOfIntsCodec: JsonValueCodec[Set[Int]] = make
  implicit val twitterAPICodec: JsonValueCodec[Seq[TwitterAPI.Tweet]] =
    make(CodecMakerConfig.withTransientDefault(false).withCheckFieldDuplication(false))
}
