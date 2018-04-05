package com.github.plokhotnyuk.jsoniter_scala.macros

import com.dslplatform.json._

import scala.reflect.ClassTag

object DslPlatformJson {
  private val dslJson = new DslJson[Any]
  implicit val (encoderAnyRef, decoderAnyRef) = setupCodecs[AnyRefs]
  implicit val (encoderDM, decoderDM) = setupCodecs[DistanceMatrix]

  private val tlWriter = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = dslJson.newWriter()
  }
  private val tlReader = new ThreadLocal[JsonReader[_]] {
    override def initialValue(): JsonReader[_] = dslJson.newReader()
  }

  private def setupCodecs[T](implicit ct: ClassTag[T]): (JsonWriter.WriteObject[T], JsonReader.ReadObject[T]) = {
    val encoder = dslJson.tryFindWriter(ct.runtimeClass).asInstanceOf[JsonWriter.WriteObject[T]]
    val decoder = dslJson.tryFindReader(ct.runtimeClass).asInstanceOf[JsonReader.ReadObject[T]]
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
