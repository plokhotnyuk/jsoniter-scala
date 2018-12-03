package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.dslplatform.json._

import scala.reflect.ClassTag

object DslPlatformJson {
  private[this] val dslJson = new DslJson[Any]
  private[this] val tlWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter()
  }
  private[this] val tlReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader()
  }

  implicit val (anyRefEncoder, anyRefDecoder) = setupCodecs[AnyRefs]
  implicit val (arrayOfBooleansEncoder, arrayOfBooleansDecoder) = setupCodecs[Array[Boolean]]
  implicit val (arrayOfBytesEncoder, arrayOfBytesDecoder) = setupCodecs[Array[Byte]]
  implicit val (arrayOfDoublesEncoder, arrayOfDoublesDecoder) = setupCodecs[Array[Double]]
  implicit val (arrayOfFloatsEncoder, arrayOfFloatsDecoder) = setupCodecs[Array[Float]]
  implicit val (arrayOfIntsEncoder, arrayOfIntsDecoder) = setupCodecs[Array[Int]]
  implicit val (arrayOfLongsEncoder, arrayOfLongsDecoder) = setupCodecs[Array[Long]]
  implicit val (arrayOfShortsEncoder, arrayOfShorsDecoder) = setupCodecs[Array[Short]]
  implicit val (extractFieldsEncoder, extractFieldsDecoder) = setupCodecs[ExtractFields]
  implicit val (googleMapsAPIEncoder, googleMapsAPIDecoder) = setupCodecs[DistanceMatrix]
  implicit val (intEncoder, intDecoder) = setupCodecs[Int]
  implicit val (missingReqFieldsEncoder, missingReqFieldsDecoder) = setupCodecs[MissingReqFields]
  implicit val (nestedStructsEncoder, nestedStructsDecoder) = setupCodecs[NestedStructs]
  implicit val (stringEncoder, stringDecoder) = setupCodecs[String]

  def decodeDslJson[T](bytes: Array[Byte])(implicit decoder: JsonReader.ReadObject[T]): T = {
    val reader = tlReader.get().process(bytes, bytes.length)
    reader.read()
    decoder.read(reader)
  }

  def encodeDslJson[T](obj: T)(implicit encoder: JsonWriter.WriteObject[T]): Array[Byte] = {
    val writer = tlWriter.get()
    writer.reset()
    encoder.write(writer, obj)
    writer.toByteArray
  }

  private[this] def setupCodecs[T](implicit ct: ClassTag[T]): (JsonWriter.WriteObject[T], JsonReader.ReadObject[T]) = {
    val encoder = dslJson.tryFindWriter(ct.runtimeClass).asInstanceOf[JsonWriter.WriteObject[T]]
    val decoder = dslJson.tryFindReader(ct.runtimeClass).asInstanceOf[JsonReader.ReadObject[T]]
    encoder -> decoder
  }
}
