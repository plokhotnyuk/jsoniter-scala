package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID
import com.dslplatform.json._
import com.dslplatform.json.runtime.Settings
import scala.collection.immutable.{BitSet, Seq}
import scala.collection.mutable
import scala.reflect.runtime.universe.TypeTag

object DslPlatformJson {
  private[this] val dslJson = new DslJson[Any](Settings.withRuntime().`with`(new ConfigureScala)
    .limitDigitsBuffer(Int.MaxValue /* WARNING: It is an unsafe option for open systems */)
    .limitStringBuffer(Int.MaxValue /* WARNING: It is an unsafe option for open systems */)
    .doublePrecision(JsonReader.DoublePrecision.EXACT))

  dslJson.registerReader(classOf[Char], new JsonReader.ReadObject[Char] {
    override def read(reader: JsonReader[?]): Char = {
      val s = reader.readString()
      if (s.length != 1) reader.newParseError("expected string with a single char (not surrogate pair)")
      s.charAt(0)
    }
  })
  dslJson.registerWriter(classOf[Char], new JsonWriter.WriteObject[Char] {
    override def write(writer: JsonWriter, value: Char): Unit = writer.writeString(value.toString)
  })

  private[this] val threadLocalJsonWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter
  }
  private[this] val threadLocalJsonReader = new ThreadLocal[JsonReader[?]] {
    override def initialValue(): JsonReader[?] = dslJson.newReader
  }

  val (stringEncoder: JsonWriter.WriteObject[String],
  stringDecoder: JsonReader.ReadObject[String]) = codec[String]
  implicit val (arrayBufferOfBooleansEncoder: JsonWriter.WriteObject[mutable.ArrayBuffer[Boolean]],
  arrayBufferOfBooleansDecoder: JsonReader.ReadObject[mutable.ArrayBuffer[Boolean]]) = codec[mutable.ArrayBuffer[Boolean]]
  implicit val (arrayOfBigDecimalsEncoder: JsonWriter.WriteObject[Array[BigDecimal]],
  arrayOfBigDecimalsDecoder: JsonReader.ReadObject[Array[BigDecimal]]) = codec[Array[BigDecimal]]
  implicit val (arrayOfBigIntsEncoder: JsonWriter.WriteObject[Array[BigInt]],
  arrayOfBigIntsDecoder: JsonReader.ReadObject[Array[BigInt]]) = codec[Array[BigInt]]
  implicit val (arrayOfBooleansEncoder: JsonWriter.WriteObject[Array[Boolean]],
  arrayOfBooleansDecoder: JsonReader.ReadObject[Array[Boolean]]) = codec[Array[Boolean]]
  implicit val (arrayOfDoublesEncoder: JsonWriter.WriteObject[Array[Double]],
  arrayOfDoublesDecoder: JsonReader.ReadObject[Array[Double]]) = codec[Array[Double]]
  implicit val (arrayOfEnumADTsEncoder: JsonWriter.WriteObject[Array[SuitADT]],
  arrayOfEnumADTsDecoder: JsonReader.ReadObject[Array[SuitADT]]) = codec[Array[SuitADT]]
  implicit val (arrayOfFloatsEncoder: JsonWriter.WriteObject[Array[Float]],
  arrayOfFloatsDecoder: JsonReader.ReadObject[Array[Float]]) = codec[Array[Float]]
  implicit val (arrayOfIntsEncoder: JsonWriter.WriteObject[Array[Int]],
  arrayOfIntsDecoder: JsonReader.ReadObject[Array[Int]]) = codec[Array[Int]]
  implicit val (arrayOfJavaEnumsEncoder: JsonWriter.WriteObject[Array[Suit]],
  arrayOfJavaEnumsDecoder: JsonReader.ReadObject[Array[Suit]]) = codec[Array[Suit]]
  implicit val (arrayOfLocalDatesEncoder: JsonWriter.WriteObject[Array[LocalDate]],
  arrayOfLocalDatesDecoder: JsonReader.ReadObject[Array[LocalDate]]) = codec[Array[LocalDate]]
  implicit val (arrayOfLocalDateTimesEncoder: JsonWriter.WriteObject[Array[LocalDateTime]],
  arrayOfLocalDateTimesDecoder: JsonReader.ReadObject[Array[LocalDateTime]]) = codec[Array[LocalDateTime]]
  implicit val (arrayOfLocalTimesEncoder: JsonWriter.WriteObject[Array[LocalTime]],
  arrayOfLocalTimesDecoder: JsonReader.ReadObject[Array[LocalTime]]) = codec[Array[LocalTime]]
  implicit val (arrayOfLongsEncoder: JsonWriter.WriteObject[Array[Long]],
  arrayOfLongsDecoder: JsonReader.ReadObject[Array[Long]]) = codec[Array[Long]]
  implicit val (arrayOfOffsetDateTimesEncoder: JsonWriter.WriteObject[Array[OffsetDateTime]],
  arrayOfOffsetDateTimesDecoder: JsonReader.ReadObject[Array[OffsetDateTime]]) = codec[Array[OffsetDateTime]]
  implicit val (arrayOfOffsetTimesEncoder: JsonWriter.WriteObject[Array[OffsetTime]],
  arrayOfOffsetTimesDecoder: JsonReader.ReadObject[Array[OffsetTime]]) = codec[Array[OffsetTime]]
  implicit val (arrayOfShortsEncoder: JsonWriter.WriteObject[Array[Short]],
  arrayOfShortsDecoder: JsonReader.ReadObject[Array[Short]]) = codec[Array[Short]]
  implicit val (arrayOfUUIDsEncoder: JsonWriter.WriteObject[Array[UUID]],
  arrayOfUUIDsDecoder: JsonReader.ReadObject[Array[UUID]]) = codec[Array[UUID]]
  implicit val (arrayOfZonedDateTimesEncoder: JsonWriter.WriteObject[Array[ZonedDateTime]],
  arrayOfZonedDateTimesDecoder: JsonReader.ReadObject[Array[ZonedDateTime]]) = codec[Array[ZonedDateTime]]
  implicit val (base64Encoder: JsonWriter.WriteObject[Array[Byte]],
  base64Decoder: JsonReader.ReadObject[Array[Byte]]) = codec[Array[Byte]]
  implicit val (bigIntEncoder: JsonWriter.WriteObject[BigInt],
  bigIntDecoder: JsonReader.ReadObject[BigInt]) = codec[BigInt]
  implicit val (bigDecimalEncoder: JsonWriter.WriteObject[BigDecimal],
  bigDecimalDecoder: JsonReader.ReadObject[BigDecimal]) = codec[BigDecimal]
  implicit val (bitSetEncoder: JsonWriter.WriteObject[BitSet],
  bitSetDecoder: JsonReader.ReadObject[BitSet]) = codec[BitSet]
  implicit val (extractFieldsEncoder: JsonWriter.WriteObject[ExtractFields],
  extractFieldsDecoder: JsonReader.ReadObject[ExtractFields]) = codec[ExtractFields]
  implicit val (googleMapsAPIEncoder: JsonWriter.WriteObject[GoogleMapsAPI.DistanceMatrix],
  googleMapsAPIDecoder: JsonReader.ReadObject[GoogleMapsAPI.DistanceMatrix]) = codec[GoogleMapsAPI.DistanceMatrix]
  implicit val (intEncoder: JsonWriter.WriteObject[Int], intDecoder: JsonReader.ReadObject[Int]) = codec[Int]
  /* FIXME: DSL-JSON doesn't support immutable.IntMap
  implicit val (intMapOfBooleansEncoder: JsonWriter.WriteObject[IntMap[Boolean]],
    intMapOfBooleansDecoder: JsonReader.ReadObject[IntMap[Boolean]]) = codec[IntMap[Boolean]]
*/
  implicit val (listOfBooleansEncoder: JsonWriter.WriteObject[List[Boolean]],
  listOfBooleansDecoder: JsonReader.ReadObject[List[Boolean]]) = codec[List[Boolean]]
  implicit val (mapOfIntsToBooleansEncoder: JsonWriter.WriteObject[Map[Int, Boolean]],
  mapOfIntsToBooleansDecoder: JsonReader.ReadObject[Map[Int, Boolean]]) = codec[Map[Int, Boolean]]
  implicit val (mutableBitSetEncoder: JsonWriter.WriteObject[mutable.BitSet],
  mutableBitSetDecoder: JsonReader.ReadObject[mutable.BitSet]) = codec[mutable.BitSet]
  /* FIXME: DSL-JSON doesn't support mutable.LongMap
  implicit val (mutableLongMapOfBooleansEncoder: JsonWriter.WriteObject[mutable.LongMap[Boolean]], mutableLongMapOfBooleansDecoder: JsonReader.ReadObject[mutable.LongMap[Boolean]]) = codec[mutable.LongMap[Boolean]]
*/
  implicit val (mutableMapOfIntsToBooleansEncoder: JsonWriter.WriteObject[mutable.Map[Int, Boolean]],
  mutableMapOfIntsToBooleansDecoder: JsonReader.ReadObject[mutable.Map[Int, Boolean]]) = codec[mutable.Map[Int, Boolean]]
  implicit val (mutableSetOfIntsEncoder: JsonWriter.WriteObject[mutable.Set[Int]],
  mutableSetOfIntsDecoder: JsonReader.ReadObject[mutable.Set[Int]]) = codec[mutable.Set[Int]]
  implicit val (missingReqFieldsEncoder: JsonWriter.WriteObject[MissingRequiredFields],
  missingReqFieldsDecoder: JsonReader.ReadObject[MissingRequiredFields]) = codec[MissingRequiredFields]
  implicit val (nestedStructsEncoder: JsonWriter.WriteObject[NestedStructs],
  nestedStructsDecoder: JsonReader.ReadObject[NestedStructs]) = codec[NestedStructs]
  implicit val (seqOfTweetEncoder: JsonWriter.WriteObject[Seq[TwitterAPI.Tweet]],
  seqOfTweetDecoder: JsonReader.ReadObject[Seq[TwitterAPI.Tweet]]) = codec[Seq[TwitterAPI.Tweet]]
  implicit val (setOfIntsEncoder: JsonWriter.WriteObject[Set[Int]],
  setOfIntsDecoder: JsonReader.ReadObject[Set[Int]]) = codec[Set[Int]]
  implicit val (vectorOfBooleansEncoder: JsonWriter.WriteObject[Vector[Boolean]],
  vectorOfBooleansDecoder: JsonReader.ReadObject[Vector[Boolean]]) = codec[Vector[Boolean]]

  def dslJsonDecode[T](bytes: Array[Byte])(implicit decoder: JsonReader.ReadObject[T]): T = {
    val reader = threadLocalJsonReader.get().process(bytes, bytes.length)
    reader.read()
    decoder.read(reader)
  }

  def dslJsonEncode[T](obj: T)(implicit encoder: JsonWriter.WriteObject[T]): Array[Byte] = {
    val writer = threadLocalJsonWriter.get()
    writer.reset()
    encoder.write(writer, obj)
    writer.toByteArray
  }

  private[this] def codec[T](implicit tag: TypeTag[T]): (JsonWriter.WriteObject[T], JsonReader.ReadObject[T]) =
    dslJson.encoder[T] -> dslJson.decoder[T]
}
