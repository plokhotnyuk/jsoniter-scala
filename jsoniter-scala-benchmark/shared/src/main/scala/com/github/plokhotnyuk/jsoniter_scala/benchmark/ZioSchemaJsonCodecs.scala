package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.Chunk
import zio.json.JsonCodec
import zio.schema.StandardType
import zio.schema.annotation._
import zio.schema.{DeriveSchema, Schema}

object ZioSchemaJsonCodecs {
  implicit def indexedSeq[A](implicit codec: Schema[A]): Schema[IndexedSeq[A]] =
    Schema.chunk[A].transform(_.toVector, Chunk.fromIterable(_))

  implicit def seq[A](implicit codec: Schema[A]): Schema[Seq[A]] =
    Schema.chunk[A].transform(_.toSeq, Chunk.fromIterable(_))

  implicit val adtCodec: JsonCodec[ADTBase] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[ADTBase].annotate(discriminatorName("type")))
  implicit val anyValsC3cr: JsonCodec[AnyVals] = {
    implicit val s1: Schema[ByteVal] = Schema.Primitive(StandardType.ByteType).transform(ByteVal.apply, _.a)
    implicit val s2: Schema[ShortVal] = Schema.Primitive(StandardType.ShortType).transform(ShortVal.apply, _.a)
    implicit val s3: Schema[IntVal] = Schema.Primitive(StandardType.IntType).transform(IntVal.apply, _.a)
    implicit val s4: Schema[LongVal] = Schema.Primitive(StandardType.LongType).transform(LongVal.apply, _.a)
    implicit val s5: Schema[BooleanVal] = Schema.Primitive(StandardType.BoolType).transform(BooleanVal.apply, _.a)
    implicit val s6: Schema[CharVal] = Schema.Primitive(StandardType.CharType).transform(CharVal.apply, _.a)
    implicit val s7: Schema[DoubleVal] = Schema.Primitive(StandardType.DoubleType).transform(DoubleVal.apply, _.a)
    implicit val s8: Schema[FloatVal] = Schema.Primitive(StandardType.FloatType).transform(FloatVal.apply, _.a)
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[AnyVals])
  }
  implicit val extractFieldsCodec: JsonCodec[ExtractFields] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[ExtractFields])
  implicit val geoJsonCodec: JsonCodec[GeoJSON.GeoJSON] = {
    implicit val s1: Schema[GeoJSON.SimpleGeometry] = DeriveSchema.gen[GeoJSON.SimpleGeometry].annotate(discriminatorName("type"))
    implicit val s2: Schema[GeoJSON.Geometry] = DeriveSchema.gen[GeoJSON.Geometry].annotate(discriminatorName("type"))
    implicit val s3: Schema[GeoJSON.SimpleGeoJSON] = DeriveSchema.gen[GeoJSON.SimpleGeoJSON].annotate(discriminatorName("type"))
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GeoJSON.GeoJSON].annotate(discriminatorName("type")))
  }
  implicit val gitHubActionsAPICodec: JsonCodec[GitHubActionsAPI.Response] = {
    implicit val s1: Schema[Boolean] = Schema.primitive[String].transform(_.toBoolean, _.toString)
    implicit val s2: Schema[GitHubActionsAPI.Artifact] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GitHubActionsAPI.Response])
  }
  implicit val googleMapsAPICodec: JsonCodec[GoogleMapsAPI.DistanceMatrix] = {
    implicit val s1: Schema[GoogleMapsAPI.Value] = DeriveSchema.gen
    implicit val s2: Schema[GoogleMapsAPI.Elements] = DeriveSchema.gen
    implicit val s3: Schema[GoogleMapsAPI.Rows] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GoogleMapsAPI.DistanceMatrix])
  }
  implicit val nestedStructsCodec: JsonCodec[NestedStructs] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[NestedStructs])
  implicit val openRTBBidRequestC3c: JsonCodec[OpenRTB.BidRequest] = {
    implicit val s1: Schema[OpenRTB.Segment] = DeriveSchema.gen
    implicit val s2: Schema[OpenRTB.Format] = DeriveSchema.gen
    implicit val s3: Schema[OpenRTB.Deal] = DeriveSchema.gen
    implicit val s4: Schema[OpenRTB.Metric] = DeriveSchema.gen
    implicit val s5: Schema[OpenRTB.Banner] = DeriveSchema.gen
    implicit val s6: Schema[OpenRTB.Audio] = DeriveSchema.gen
    implicit val s7: Schema[OpenRTB.Video] = DeriveSchema.gen
    implicit val s8: Schema[OpenRTB.Native] = DeriveSchema.gen
    implicit val s9: Schema[OpenRTB.Pmp] = DeriveSchema.gen
    implicit val s10: Schema[OpenRTB.Producer] = DeriveSchema.gen
    implicit val s11: Schema[OpenRTB.Data] = DeriveSchema.gen
    implicit val s12: Schema[OpenRTB.Content] = DeriveSchema.gen
    implicit val s13: Schema[OpenRTB.Publisher] = DeriveSchema.gen
    implicit val s14: Schema[OpenRTB.Geo] = DeriveSchema.gen
    implicit val s15: Schema[OpenRTB.Imp] = DeriveSchema.gen
    implicit val s16: Schema[OpenRTB.Site] = DeriveSchema.gen
    implicit val s17: Schema[OpenRTB.App] = DeriveSchema.gen
    implicit val s18: Schema[OpenRTB.Device] = DeriveSchema.gen
    implicit val s19: Schema[OpenRTB.User] = DeriveSchema.gen
    implicit val s20: Schema[OpenRTB.Source] = DeriveSchema.gen
    implicit val s21: Schema[OpenRTB.Reqs] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[OpenRTB.BidRequest])
  }
  implicit val primitivesCodec: JsonCodec[Primitives] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[Primitives])
  implicit val twitterAPICodec: JsonCodec[TwitterAPI.Tweet] = {
    implicit val s1: Schema[TwitterAPI.UserMentions] = DeriveSchema.gen
    implicit val s2: Schema[TwitterAPI.Urls] = DeriveSchema.gen
    implicit val s3: Schema[TwitterAPI.Entities] = DeriveSchema.gen
    implicit val s4: Schema[TwitterAPI.Url] = DeriveSchema.gen
    implicit val s5: Schema[TwitterAPI.UserEntities] = DeriveSchema.gen
    implicit val s6: Schema[TwitterAPI.User] = DeriveSchema.gen
    implicit val s7: Schema[TwitterAPI.RetweetedStatus] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[TwitterAPI.Tweet])
  }
}