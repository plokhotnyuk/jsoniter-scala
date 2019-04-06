package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID

import com.dslplatform.json._
import com.dslplatform.json.runtime.Settings

import scala.collection.mutable
import scala.reflect.runtime.universe.TypeTag

object DslPlatformJson {
  private[this] val dslJson = new DslJson[Any](Settings.withRuntime().`with`(new ConfigureScala)
    .doublePrecision(JsonReader.DoublePrecision.EXACT))
  private[this] val threadLocalJsonWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter
  }
  private[this] val threadLocalJsonReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader
  }

  implicit val (anyRefEncoder, anyRefDecoder) = setupCodecs[AnyRefs]
  implicit val (arrayBufferOfBooleansEncoder, arrayBufferOfBooleansDecoder) = setupCodecs[mutable.ArrayBuffer[Boolean]]
  implicit val (arrayOfBigDecimalsEncoder, arrayOfBigDecimalsDecoder) = setupCodecs[Array[BigDecimal]]
  implicit val (arrayOfBigIntsEncoder, arrayOfBigIntsDecoder) = setupCodecs[Array[BigInt]]
  implicit val (arrayOfBooleansEncoder, arrayOfBooleansDecoder) = setupCodecs[Array[Boolean]]
  implicit val (arrayOfDoublesEncoder, arrayOfDoublesDecoder) = setupCodecs[Array[Double]]
  implicit val (arrayOfFloatsEncoder, arrayOfFloatsDecoder) = setupCodecs[Array[Float]]
  implicit val (arrayOfIntsEncoder, arrayOfIntsDecoder) = setupCodecs[Array[Int]]
  implicit val (arrayOfLocalDatesEncoder, arrayOfLocalDatesDecoder) = setupCodecs[Array[LocalDate]]
  implicit val (arrayOfLocalDateTimesEncoder, arrayOfLocalDateTimesDecoder) = setupCodecs[Array[LocalDateTime]]
  implicit val (arrayOfLongsEncoder, arrayOfLongsDecoder) = setupCodecs[Array[Long]]
  implicit val (arrayOfOffsetDateTimesEncoder, arrayOfOffsetDateTimesDecoder) = setupCodecs[Array[OffsetDateTime]]
  implicit val (arrayOfShortsEncoder, arrayOfShorsDecoder) = setupCodecs[Array[Short]]
  implicit val (arrayOfUUIDsEncoder, arrayOfUUIDsDecoder) = setupCodecs[Array[UUID]]
  implicit val (arrayOfZonedDateTimesEncoder, arrayOfZonedDateTimesDecoder) = setupCodecs[Array[ZonedDateTime]]
  implicit val (bigIntgEncoder, bigIntgDecoder) = setupCodecs[BigInt]
  implicit val (bigDecimalEncoder, bigDecimalDecoder) = setupCodecs[BigDecimal]
  implicit val (extractFieldsEncoder, extractFieldsDecoder) = setupCodecs[ExtractFields]
  implicit val (googleMapsAPIEncoder, googleMapsAPIDecoder) = setupCodecs[DistanceMatrix]
  implicit val (intEncoder, intDecoder) = setupCodecs[Int]
  implicit val (listOfBooleansEncoder, listOfBooleansDecoder) = setupCodecs[List[Boolean]]
  implicit val (mapOfIntsToBooleansEncoder, mapOfIntsToBooleansDecoder) = setupCodecs[Map[Int, Boolean]]
  implicit val (mutableMapOfIntsToBooleansEncoder, mutableMapOfIntsToBooleansDecoder) = setupCodecs[mutable.Map[Int, Boolean]]
  implicit val (mutableSetOfIntsEncoder, mutableSetOfIntsDecoder) = setupCodecs[mutable.Set[Int]]
  implicit val (missingReqFieldsEncoder, missingReqFieldsDecoder) = setupCodecs[MissingReqFields]
  implicit val (nestedStructsEncoder, nestedStructsDecoder) = setupCodecs[NestedStructs]
  implicit val (seqOfTweetEncoder, seqOfTweetDecoder) = setupCodecs[Seq[Tweet]]
  implicit val (setOfIntsEncoder, setOfIntsDecoder) = setupCodecs[Set[Int]]
  implicit val (stringEncoder, stringDecoder) = setupCodecs[String]
  implicit val (vectorOfBooleansEncoder, vectorOfBooleansDecoder) = setupCodecs[Vector[Boolean]]

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

  private[this] def setupCodecs[T](implicit tag: TypeTag[T]): (JsonWriter.WriteObject[T], JsonReader.ReadObject[T]) =
    dslJson.encoder[T] -> dslJson.decoder[T]
}
