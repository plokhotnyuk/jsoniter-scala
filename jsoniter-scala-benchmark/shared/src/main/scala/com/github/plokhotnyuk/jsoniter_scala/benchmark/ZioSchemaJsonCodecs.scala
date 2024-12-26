package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.Chunk
import zio.json.JsonCodec
import zio.schema.StandardType
import zio.schema.annotation._
import zio.schema.{DeriveSchema, Schema}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.reflect.ClassTag

object ZioSchemaJsonCodecs {
  implicit def array[A: ClassTag](implicit schemaA: Schema[A]): Schema[Array[A]] =
   Schema.Sequence[Array[A], A, String](schemaA, _.toArray, Chunk.fromArray, Chunk.empty, "Array")

  implicit def arrayBuffer[A](implicit schemaA: Schema[A]): Schema[mutable.ArrayBuffer[A]] =
   Schema.Sequence(schemaA, mutable.ArrayBuffer.from, Chunk.fromIterable, Chunk.empty, "ArrayBuffer")

  implicit def arraySeq[A: ClassTag](implicit schemaA: Schema[A]): Schema[ArraySeq[A]] =
   Schema.Sequence[ArraySeq[A], A, String](schemaA, x => ArraySeq.unsafeWrapArray(x.toArray),
     x => Chunk.fromArray[A](x.unsafeArray.asInstanceOf[Array[A]]), Chunk.empty, "ArraySeq")

  implicit def indexedSeq[A](implicit schemaA: Schema[A]): Schema[IndexedSeq[A]] =
   Schema.Sequence[IndexedSeq[A], A, String](schemaA, _.toIndexedSeq, Chunk.fromIterable, Chunk.empty, "IndexedSeq")

  implicit def seq[A](implicit schemaA: Schema[A]): Schema[Seq[A]] =
    Schema.Sequence[Seq[A], A, String](schemaA, _.toSeq, Chunk.fromIterable, Chunk.empty, "Seq")

  val adtCodec: JsonCodec[ADTBase] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[ADTBase].annotate(discriminatorName("type")))
  val anyValsCodec: JsonCodec[AnyVals] = {
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
  val arrayBufferOfBooleansCodec: JsonCodec[mutable.ArrayBuffer[Boolean]] =
    zio.schema.codec.JsonCodec.jsonCodec(arrayBuffer[Boolean])
  val arrayOfBooleansCodec: JsonCodec[Array[Boolean]] =
    zio.schema.codec.JsonCodec.jsonCodec(array[Boolean])
  val arrayOfEnumADTsCodec: JsonCodec[Array[SuitADT]] = {
    implicit val s1: Schema[SuitADT] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(array[SuitADT])
  }
  val arraySeqOfBooleansCodec: JsonCodec[ArraySeq[Boolean]] =
    zio.schema.codec.JsonCodec.jsonCodec(arraySeq[Boolean])
  val extractFieldsCodec: JsonCodec[ExtractFields] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[ExtractFields])
  val geoJsonCodec: JsonCodec[GeoJSON.GeoJSON] = {
    implicit val s1: Schema[GeoJSON.SimpleGeometry] = DeriveSchema.gen[GeoJSON.SimpleGeometry].annotate(discriminatorName("type"))
    implicit val s2: Schema[GeoJSON.Geometry] = DeriveSchema.gen[GeoJSON.Geometry].annotate(discriminatorName("type"))
    implicit val s3: Schema[GeoJSON.SimpleGeoJSON] = DeriveSchema.gen[GeoJSON.SimpleGeoJSON].annotate(discriminatorName("type"))
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GeoJSON.GeoJSON].annotate(discriminatorName("type")))
  }
  val gitHubActionsAPICodec: JsonCodec[GitHubActionsAPI.Response] = {
    implicit val s1: Schema[Boolean] = Schema.primitive[String].transform(_.toBoolean, _.toString)
    implicit val s2: Schema[GitHubActionsAPI.Artifact] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GitHubActionsAPI.Response])
  }
  val googleMapsAPICodec: JsonCodec[GoogleMapsAPI.DistanceMatrix] = {
    implicit val s1: Schema[GoogleMapsAPI.Value] = DeriveSchema.gen
    implicit val s2: Schema[GoogleMapsAPI.Elements] = DeriveSchema.gen
    implicit val s3: Schema[GoogleMapsAPI.Rows] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[GoogleMapsAPI.DistanceMatrix])
  }
  val listOfBooleansCodec: JsonCodec[List[Boolean]] =
    zio.schema.codec.JsonCodec.jsonCodec(Schema.list[Boolean])
  val missingRequiredFieldsCodec: JsonCodec[MissingRequiredFields] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[MissingRequiredFields])
  val nestedStructsCodec: JsonCodec[NestedStructs] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[NestedStructs])
  val openRTBBidRequestCodec: JsonCodec[OpenRTB.BidRequest] = {
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
  val primitivesCodec: JsonCodec[Primitives] =
    zio.schema.codec.JsonCodec.jsonCodec(DeriveSchema.gen[Primitives])
  val setOfIntsCodec: JsonCodec[Set[Int]] =
    zio.schema.codec.JsonCodec.jsonCodec(Schema.set[Int])
  val twitterAPICodec: JsonCodec[Seq[TwitterAPI.Tweet]] = {
    implicit val s1: Schema[TwitterAPI.UserMentions] = DeriveSchema.gen
    implicit val s2: Schema[TwitterAPI.Urls] = DeriveSchema.gen
    implicit val s3: Schema[TwitterAPI.Entities] = DeriveSchema.gen
    implicit val s4: Schema[TwitterAPI.Url] = DeriveSchema.gen
    implicit val s5: Schema[TwitterAPI.UserEntities] = DeriveSchema.gen
    implicit val s6: Schema[TwitterAPI.User] = DeriveSchema.gen
    implicit val s7: Schema[TwitterAPI.RetweetedStatus] = DeriveSchema.gen
    implicit val s8: Schema[TwitterAPI.Tweet] = DeriveSchema.gen
    zio.schema.codec.JsonCodec.jsonCodec(seq[TwitterAPI.Tweet])
  }
  val vectorOfBooleansCodec: JsonCodec[Vector[Boolean]] =
    zio.schema.codec.JsonCodec.jsonCodec(Schema.vector[Boolean])
}