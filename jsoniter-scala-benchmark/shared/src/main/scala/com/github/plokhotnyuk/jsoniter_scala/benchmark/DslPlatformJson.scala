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
    override def read(reader: JsonReader[_]): Char = {
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
  private[this] val threadLocalJsonReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader
  }

  val (stringEncoder, stringDecoder) = codec[String]
  implicit val (anyValsEncoder, anyValsDecoder) = codec[AnyVals]
  implicit val (arrayBufferOfBooleansEncoder, arrayBufferOfBooleansDecoder) = codec[mutable.ArrayBuffer[Boolean]]
  implicit val (arrayOfBigDecimalsEncoder, arrayOfBigDecimalsDecoder) = codec[Array[BigDecimal]]
  implicit val (arrayOfBigIntsEncoder, arrayOfBigIntsDecoder) = codec[Array[BigInt]]
  implicit val (arrayOfBooleansEncoder, arrayOfBooleansDecoder) = codec[Array[Boolean]]
  implicit val (arrayOfDoublesEncoder, arrayOfDoublesDecoder) = codec[Array[Double]]
  implicit val (arrayOfEnumADTsEncoder, arrayOfEnumADTsDecoder) = codec[Array[SuitADT]]
  implicit val (arrayOfFloatsEncoder, arrayOfFloatsDecoder) = codec[Array[Float]]
  implicit val (arrayOfIntsEncoder, arrayOfIntsDecoder) = codec[Array[Int]]
  implicit val (arrayOfJavaEnumsEncoder, arrayOfJavaEnumsDecoder) = codec[Array[Suit]]
  implicit val (arrayOfLocalDatesEncoder, arrayOfLocalDatesDecoder) = codec[Array[LocalDate]]
  implicit val (arrayOfLocalDateTimesEncoder, arrayOfLocalDateTimesDecoder) = codec[Array[LocalDateTime]]
  implicit val (arrayOfLocalTimesEncoder, arrayOfLocalTimesDecoder) = codec[Array[LocalTime]]
  implicit val (arrayOfLongsEncoder, arrayOfLongsDecoder) = codec[Array[Long]]
  implicit val (arrayOfOffsetDateTimesEncoder, arrayOfOffsetDateTimesDecoder) = codec[Array[OffsetDateTime]]
  implicit val (arrayOfOffsetTimesEncoder, arrayOfOffsetTimesDecoder) = codec[Array[OffsetTime]]
  implicit val (arrayOfShortsEncoder, arrayOfShortsDecoder) = codec[Array[Short]]
  implicit val (arrayOfUUIDsEncoder, arrayOfUUIDsDecoder) = codec[Array[UUID]]
  implicit val (arrayOfZonedDateTimesEncoder, arrayOfZonedDateTimesDecoder) = codec[Array[ZonedDateTime]]
  implicit val (base64Encoder, base64Decoder) = codec[Array[Byte]]
  implicit val (bigIntgEncoder, bigIntgDecoder) = codec[BigInt]
  implicit val (bigDecimalEncoder, bigDecimalDecoder) = codec[BigDecimal]
  implicit val (bitSetEncoder, bitSetDecoder) = codec[BitSet]
  implicit val (extractFieldsEncoder, extractFieldsDecoder) = codec[ExtractFields]
  implicit val (googleMapsAPIEncoder, googleMapsAPIDecoder) = codec[GoogleMapsAPI.DistanceMatrix]
  implicit val (intEncoder, intDecoder) = codec[Int]
/* FIXME: DSL-JSON doesn't support immutable.IntMap
  implicit val (intMapOfBooleansEncoder, intMapOfBooleansDecoder) = codec[IntMap[Boolean]]
*/
  implicit val (listOfBooleansEncoder, listOfBooleansDecoder) = codec[List[Boolean]]
  implicit val (mapOfIntsToBooleansEncoder, mapOfIntsToBooleansDecoder) = codec[Map[Int, Boolean]]
  implicit val (mutableBitSetEncoder, mutablebitSetDecoder) = codec[mutable.BitSet]
/* FIXME: DSL-JSON doesn't support mutable.LongMap
  implicit val (mutableLongMapOfBooleansEncoder, mutableLongMapOfBooleansDecoder) = codec[mutable.LongMap[Boolean]]
*/
  implicit val (mutableMapOfIntsToBooleansEncoder, mutableMapOfIntsToBooleansDecoder) = codec[mutable.Map[Int, Boolean]]
  implicit val (mutableSetOfIntsEncoder, mutableSetOfIntsDecoder) = codec[mutable.Set[Int]]
  implicit val (missingReqFieldsEncoder, missingReqFieldsDecoder) = codec[MissingRequiredFields]
  implicit val (nestedStructsEncoder, nestedStructsDecoder) = codec[NestedStructs]
  implicit val (primitivesEncoder, primitivesDecoder) = codec[Primitives]
  implicit val (seqOfTweetEncoder, seqOfTweetDecoder) = codec[Seq[TwitterAPI.Tweet]]
  implicit val (setOfIntsEncoder, setOfIntsDecoder) = codec[Set[Int]]
  implicit val (vectorOfBooleansEncoder, vectorOfBooleansDecoder) = codec[Vector[Boolean]]

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
