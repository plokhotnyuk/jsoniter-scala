package com.github.plokhotnyuk.jsoniter_scala.macros

import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig =
    ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val anyRefsCodec: JsonCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  val arraysCodec: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())
  val bigIntArrayCodec: JsonCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  val bitSetsCodec: JsonCodec[BitSets] = make[BitSets](CodecMakerConfig())
  val booleanArrayCodec: JsonCodec[Array[Boolean]] = make[Array[Boolean]](CodecMakerConfig())
  val byteArrayCodec: JsonCodec[Array[Byte]] = make[Array[Byte]](CodecMakerConfig())
  val doubleArrayCodec: JsonCodec[Array[Double]] = make[Array[Double]](CodecMakerConfig())
  val floatArrayCodec: JsonCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  val instantArrayCodec: JsonCodec[Array[java.time.Instant]] = make[Array[java.time.Instant]](CodecMakerConfig())
  val localDateArrayCodec: JsonCodec[Array[java.time.LocalDate]] = make[Array[java.time.LocalDate]](CodecMakerConfig())
  val localTimeArrayCodec: JsonCodec[Array[java.time.LocalTime]] = make[Array[java.time.LocalTime]](CodecMakerConfig())
  val intArrayCodec: JsonCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
  val shortArrayCodec: JsonCodec[Array[Short]] = make[Array[Short]](CodecMakerConfig())
  val longArrayCodec: JsonCodec[Array[Long]] = make[Array[Long]](CodecMakerConfig())
  val uuidArrayCodec: JsonCodec[Array[UUID]] = make[Array[UUID]](CodecMakerConfig())
  val iterablesCodec: JsonCodec[Iterables] = make[Iterables](CodecMakerConfig())
  val mapsCodec: JsonCodec[Maps] = make[Maps](CodecMakerConfig())
  val missingReqFieldCodec: JsonCodec[MissingReqFields] = make[MissingReqFields](CodecMakerConfig())
  val mutableIterablesCodec: JsonCodec[MutableIterables] = make[MutableIterables](CodecMakerConfig())
  val mutableMapsCodec: JsonCodec[MutableMaps] = make[MutableMaps](CodecMakerConfig())
  val intAndLongMapsCodec: JsonCodec[IntAndLongMaps] = make[IntAndLongMaps](CodecMakerConfig())
  val primitivesCodec: JsonCodec[Primitives] = make[Primitives](CodecMakerConfig())
  val extractFieldsCodec: JsonCodec[ExtractFields] = make[ExtractFields](CodecMakerConfig())
  val adtCodec: JsonCodec[AdtBase] = make[AdtBase](CodecMakerConfig())
  val stringCodec: JsonCodec[String] = make[String](CodecMakerConfig())
  val googleMapsAPICodec: JsonCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  val twitterAPICodec: JsonCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
}
