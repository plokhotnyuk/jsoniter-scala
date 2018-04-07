package com.github.plokhotnyuk.jsoniter_scala.macros

import com.dslplatform.json._

import scala.reflect.ClassTag

object DslPlatformJson {
  private val dslJson = new DslJson[Any]
  private val tlWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter()
  }
  private val tlReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader()
  }

  implicit val (anyRefEncoder, anyRefDecoder) = setupCodecs[AnyRefs]
  implicit val (arrayOfBigIntsEncoder, arrayOfBigIntsDecoder) = setupCodecs[Array[BigInt]]
  implicit val (arrayOfBooleansEncoder, arrayOfBooleansDecoder) = setupCodecs[Array[Boolean]]
  implicit val (arrayOfBytesEncoder, arrayOfBytesDecoder) = setupCodecs[Array[Byte]]
  implicit val (arrayOfCharsEncoder, arrayOfCharsDecoder) = setupCodecs[Array[Char]]
  implicit val (arrayOfDoublesEncoder, arrayOfDoublesDecoder) = setupCodecs[Array[Double]]
  implicit val (arrayOfFloatsEncoder, arrayOfFloatsDecoder) = setupCodecs[Array[Float]]
  implicit val (arrayOfIntsEncoder, arrayOfIntsDecoder) = setupCodecs[Array[Int]]
  implicit val (arrayOfLongsEncoder, arrayOfLongsDecoder) = setupCodecs[Array[Long]]
  implicit val (arrayOfShortsEncoder, arrayOfShorsDecoder) = setupCodecs[Array[Short]]
  implicit val (extractFieldsEncoder, extractFieldsDecoder) = setupCodecs[ExtractFields]
  implicit val (googleMapsAPIEncoder, googleMapsAPIDecoder) = setupCodecs[DistanceMatrix]
  implicit val (intEncoder, intDecoder) = setupCodecs[Int]
  implicit val (missingReqFieldsEncoder, missingReqFieldsDecoder) = setupCodecs[MissingReqFields]
  implicit val (stringEncoder, stringDecoder) = setupCodecs[String]
  implicit val (primitivesEncoder, primitivesDecoder) = setupCodecs[Primitives]
  implicit val (twitterAPIEncoder, twitterAPIDecoder) = setupCodecs[Seq[Tweet]]

  private def setupCodecs[T](implicit ct: ClassTag[T]): (JsonWriter.WriteObject[T], JsonReader.ReadObject[T]) = {
    val encoder = dslJson.tryFindWriter(ct.runtimeClass).asInstanceOf[JsonWriter.WriteObject[T]]
    val decoder = dslJson.tryFindReader(ct.runtimeClass).asInstanceOf[JsonReader.ReadObject[T]]
    //require(encoder != null, s"cannot find encoder for ${ct.runtimeClass}")
    //require(decoder != null, s"cannot find decoder for ${ct.runtimeClass}")
    encoder -> decoder
  }

  def decodeDslJson[T](bytes: Array[Byte])(implicit decoder: JsonReader.ReadObject[T]): T = {
    val reader = tlReader.get().process(bytes, bytes.length)
    reader.read()
    decoder.read(reader)
  }

  def encodeDslJson[T](obj: T)(implicit encoder: JsonWriter.WriteObject[T]): JsonWriter = {
    val writer = tlWriter.get()
    writer.reset()
    encoder.write(writer, obj)
    writer
  }
}
