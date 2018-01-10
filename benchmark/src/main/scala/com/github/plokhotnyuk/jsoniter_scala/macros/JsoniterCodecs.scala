package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonCodec, ReaderConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

object JsoniterCodecs {
  val stacklessExceptionConfig = ReaderConfig(throwParseExceptionWithStackTrace = false)
  val stacklessExceptionWithoutDumpConfig = ReaderConfig(throwParseExceptionWithStackTrace = false, appendHexDumpToParseException = false)
  val anyRefsCodec: JsonCodec[AnyRefs] = make[AnyRefs](CodecMakerConfig())
  val arraysCodec: JsonCodec[Arrays] = make[Arrays](CodecMakerConfig())
  val bigIntArrayCodec: JsonCodec[Array[BigInt]] = make[Array[BigInt]](CodecMakerConfig())
  val bitSetsCodec: JsonCodec[BitSets] = make[BitSets](CodecMakerConfig())
  val floatArrayCodec: JsonCodec[Array[Float]] = make[Array[Float]](CodecMakerConfig())
  val intArrayCodec: JsonCodec[Array[Int]] = make[Array[Int]](CodecMakerConfig())
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
