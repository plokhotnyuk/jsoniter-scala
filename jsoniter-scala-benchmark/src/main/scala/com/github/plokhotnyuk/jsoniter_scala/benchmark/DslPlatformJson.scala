package com.github.plokhotnyuk.jsoniter_scala.benchmark

//import java.lang.reflect.{ParameterizedType, Type}
import java.time._
import java.util.UUID

import com.dslplatform.json._
import com.dslplatform.json.runtime.{ScalaMapEncoder, Settings}

import scala.collection.immutable.{BitSet, IntMap}
import scala.collection.mutable
import scala.reflect.runtime.universe.TypeTag

object DslPlatformJson {
  private[this] val dslJson = new DslJson[Any](Settings.withRuntime().`with`(new ConfigureScala)
    .doublePrecision(JsonReader.DoublePrecision.EXACT))
/*
  dslJson.registerWriterFactory(primitiveMapWriter)

  private[this] def primitiveMapWriter(manifest: Type, dslJson: DslJson[_]): ScalaMapEncoder[Int, Any] = {
    manifest match {
      case pt: ParameterizedType if pt.getRawType == classOf[IntMap[_]] =>
        val valueWriter = dslJson.tryFindWriter(pt.getActualTypeArguments.head)
        val encoder = new ScalaMapEncoder[Int, Any](dslJson,true,
          Some(NumberConverter.INT_WRITER.asInstanceOf[JsonWriter.WriteObject[Int]]),
          Some(valueWriter.asInstanceOf[JsonWriter.WriteObject[Any]]))
        dslJson.registerWriter(manifest, encoder)
        encoder
      case _ => null
    }
  }
*/
  private[this] val threadLocalJsonWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter
  }
  private[this] val threadLocalJsonReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader
  }

  implicit val (anyRefEncoder, anyRefDecoder) = setupCodecs[AnyRefs]
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.AnyVals
  implicit val (anyValsEncoder, anyValsDecoder) = setupCodecs[AnyVals]
*/
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
  implicit val (bitSetEncoder, bitSetDecoder) = setupCodecs[BitSet]
  implicit val (extractFieldsEncoder, extractFieldsDecoder) = setupCodecs[ExtractFields]
  implicit val (googleMapsAPIEncoder, googleMapsAPIDecoder) = setupCodecs[DistanceMatrix]
  implicit val (intEncoder, intDecoder) = setupCodecs[Int]
/* FIXME: DSL-JSON throws java.lang.ClassCastException: scala.Tuple2 cannot be cast to java.lang.Boolean
  implicit val intMapOfBooleansEncoder: JsonWriter.WriteObject[IntMap[Boolean]] = dslJson.encoder[IntMap[Boolean]]
*/
  implicit val (listOfBooleansEncoder, listOfBooleansDecoder) = setupCodecs[List[Boolean]]
  implicit val (mapOfIntsToBooleansEncoder, mapOfIntsToBooleansDecoder) = setupCodecs[Map[Int, Boolean]]
  implicit val (mutableBitSetEncoder, mutablebitSetDecoder) = setupCodecs[mutable.BitSet]
/* FIXME: DSL-JSON doesn't support mutable.LongMap
  implicit val (mutableLongMapOfBooleansEncoder, mutableLongMapOfBooleansDecoder) = setupCodecs[mutable.LongMap[Boolean]]
*/
  implicit val (mutableMapOfIntsToBooleansEncoder, mutableMapOfIntsToBooleansDecoder) =
    setupCodecs[mutable.Map[Int, Boolean]]
  implicit val (mutableSetOfIntsEncoder, mutableSetOfIntsDecoder) = setupCodecs[mutable.Set[Int]]
  implicit val (missingReqFieldsEncoder, missingReqFieldsDecoder) = setupCodecs[MissingReqFields]
  implicit val (nestedStructsEncoder, nestedStructsDecoder) = setupCodecs[NestedStructs]
/* FIXME: DSL-JSON throws NPE at com.dslplatform.json.runtime.Generics.getTypeNameCompat(Generics.java:200)
  implicit val (openHashMapOfIntsToBooleansEncoder, openHashMapOfIntsToBooleansDecoder) =
    setupCodecs[mutable.OpenHashMap[Int, Boolean]]
*/
/* FIXME: DSL_JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.Primitives
  implicit val (primitivesEncoder, primitivesDecoder) = setupCodecs[Primitives]
*/
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
