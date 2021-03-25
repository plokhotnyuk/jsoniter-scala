package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.json.JsonDecoder.JsonError
import zio.json._
import zio.json.internal._
import scala.reflect.ClassTag

object ZioJSONEncoderDecoders {
  implicit def array[A](implicit codec: JsonCodec[A], classTag: ClassTag[A]): JsonCodec[Array[A]] =
    JsonCodec((as: Array[A], indent: Option[Int], out: Write) => {
      out.write('[')
      val len = as.length
      var i: Int = 0
      while (i < len) {
        if (i != 0) {
          if (indent.isEmpty) out.write(',')
          else out.write(", ")
        }
        codec.unsafeEncode(as(i), indent, out)
        i += 1
      }
      out.write(']')
    }, (trace: List[JsonError], in: RetractReader) => {
      val builder = Array.newBuilder[A]
      Lexer.char(trace, in, '[')
      var i: Int = 0
      if (Lexer.firstArrayElement(in)) do {
        builder += codec.unsafeDecode(JsonError.ArrayAccess(i) :: trace, in)
        i += 1
      } while (Lexer.nextArrayElement(trace, in))
      builder.result()
    })

  implicit def indexedSeq[A](implicit codec: JsonCodec[A]): JsonCodec[IndexedSeq[A]] =
    JsonCodec((as: IndexedSeq[A], indent: Option[Int], out: Write) => {
      out.write('[')
      var first = true
      as.foreach { a =>
        if (first) first = false
        else if (indent.isEmpty) out.write(',')
        else out.write(", ")
        codec.unsafeEncode(a, indent, out)
      }
      out.write(']')
    }, (trace: List[JsonError], in: RetractReader) => {
      val builder = IndexedSeq.newBuilder[A]
      Lexer.char(trace, in, '[')
      var i: Int = 0
      if (Lexer.firstArrayElement(in)) do {
        builder += codec.unsafeDecode(JsonError.ArrayAccess(i) :: trace, in)
        i += 1
      } while (Lexer.nextArrayElement(trace, in))
      builder.result()
    })

  implicit val (adtE5r: JsonEncoder[ADTBase], adtD5r: JsonDecoder[ADTBase]) = {
    implicit val c1: JsonCodec[ADTBase] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (anyValsE5r: JsonEncoder[AnyVals], anyValsD5r: JsonDecoder[AnyVals]) = {
    implicit val c1: JsonCodec[ByteVal] = JsonCodec.byte.xmap(ByteVal.apply, _.a)
    implicit val c2: JsonCodec[ShortVal] = JsonCodec.short.xmap(ShortVal.apply, _.a)
    implicit val c3: JsonCodec[IntVal] = JsonCodec.int.xmap(IntVal.apply, _.a)
    implicit val c4: JsonCodec[LongVal] = JsonCodec.long.xmap(LongVal.apply, _.a)
    implicit val c5: JsonCodec[BooleanVal] = JsonCodec.boolean.xmap(BooleanVal.apply, _.a)
    implicit val c6: JsonCodec[CharVal] = JsonCodec.char.xmap(CharVal.apply, _.a)
    implicit val c7: JsonCodec[DoubleVal] = JsonCodec.double.xmap(DoubleVal.apply, _.a)
    implicit val c8: JsonCodec[FloatVal] = JsonCodec.float.xmap(FloatVal.apply, _.a)
    implicit val c9: JsonCodec[AnyVals] = DeriveJsonCodec.gen
    (c9.encoder, c9.decoder)
  }
  implicit val (geoJsonE5r: JsonEncoder[GeoJSON.GeoJSON], geoJsonD5r: JsonDecoder[GeoJSON.GeoJSON]) = {
    implicit val c1: JsonCodec[GeoJSON.SimpleGeometry] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GeoJSON.Geometry] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GeoJSON.SimpleGeoJSON] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[GeoJSON.GeoJSON] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (googleMapsAPIE5r: JsonEncoder[GoogleMapsAPI.DistanceMatrix], googleMapsAPID5r: JsonDecoder[GoogleMapsAPI.DistanceMatrix]) = {
    implicit val c1: JsonCodec[GoogleMapsAPI.Value] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GoogleMapsAPI.Elements] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GoogleMapsAPI.Rows] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[GoogleMapsAPI.DistanceMatrix] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (extractFieldsE5r: JsonEncoder[ExtractFields], extractFieldsD5r: JsonDecoder[ExtractFields]) = {
    implicit val c1: JsonCodec[ExtractFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (missingRequiredFieldsE5r: JsonEncoder[MissingRequiredFields], missingRequiredFieldsD5r: JsonDecoder[MissingRequiredFields]) = {
    implicit val c1: JsonCodec[MissingRequiredFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (nestedStructsE5r: JsonEncoder[NestedStructs], nestedStructsD5r: JsonDecoder[NestedStructs]) = {
    implicit lazy val c1: JsonCodec[NestedStructs] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (openRTBBidRequestE5r: JsonEncoder[OpenRTB.BidRequest], openRTBBidRequestD5r: JsonDecoder[OpenRTB.BidRequest]) = {
    implicit val c1: JsonCodec[OpenRTB.Segment] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[OpenRTB.Format] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[OpenRTB.Deal] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[OpenRTB.Metric] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[OpenRTB.Banner] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[OpenRTB.Audio] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[OpenRTB.Video] = DeriveJsonCodec.gen
    implicit val c8: JsonCodec[OpenRTB.Native] = DeriveJsonCodec.gen
    implicit val c9: JsonCodec[OpenRTB.Pmp] = DeriveJsonCodec.gen
    implicit val c10: JsonCodec[OpenRTB.Producer] = DeriveJsonCodec.gen
    implicit val c11: JsonCodec[OpenRTB.Data] = DeriveJsonCodec.gen
    implicit val c12: JsonCodec[OpenRTB.Content] = DeriveJsonCodec.gen
    implicit val c13: JsonCodec[OpenRTB.Publisher] = DeriveJsonCodec.gen
    implicit val c14: JsonCodec[OpenRTB.Geo] = DeriveJsonCodec.gen
    implicit val c15: JsonCodec[OpenRTB.Imp] = DeriveJsonCodec.gen
    implicit val c16: JsonCodec[OpenRTB.Site] = DeriveJsonCodec.gen
    implicit val c17: JsonCodec[OpenRTB.App] = DeriveJsonCodec.gen
    implicit val c18: JsonCodec[OpenRTB.Device] = DeriveJsonCodec.gen
    implicit val c19: JsonCodec[OpenRTB.User] = DeriveJsonCodec.gen
    implicit val c20: JsonCodec[OpenRTB.Source] = DeriveJsonCodec.gen
    implicit val c21: JsonCodec[OpenRTB.Reqs] = DeriveJsonCodec.gen
    implicit val c22: JsonCodec[OpenRTB.BidRequest] = DeriveJsonCodec.gen
    (c22.encoder, c22.decoder)
  }
  implicit val (primitivesE5r: JsonEncoder[Primitives], primitivesD5r: JsonDecoder[Primitives]) = {
    implicit val c1: JsonCodec[Primitives] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (twitterAPIE5r: JsonEncoder[TwitterAPI.Tweet], twitterAPID5r: JsonDecoder[TwitterAPI.Tweet]) = {
    implicit val c1: JsonCodec[TwitterAPI.UserMentions] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[TwitterAPI.Urls] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[TwitterAPI.Entities] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[TwitterAPI.Url] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[TwitterAPI.UserEntities] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[TwitterAPI.User] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[TwitterAPI.RetweetedStatus] = DeriveJsonCodec.gen
    implicit val c8: JsonCodec[TwitterAPI.Tweet] = DeriveJsonCodec.gen
    (c8.encoder, c8.decoder)
  }
}
