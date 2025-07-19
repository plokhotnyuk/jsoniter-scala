package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.Chunk
import zio.schema.DeriveSchema.gen
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.AvroCodec

import scala.reflect.ClassTag

object ZioSchemaAvroCodecs {
  implicit def array[A: ClassTag](implicit schemaA: Schema[A]): Schema[Array[A]] =
   Schema.Sequence[Array[A], A, String](schemaA, _.toArray, Chunk.fromArray, Chunk.empty, "Array")

  implicit def indexedSeq[A](implicit schemaA: Schema[A]): Schema[IndexedSeq[A]] =
    Schema.Sequence[IndexedSeq[A], A, String](schemaA, _.toIndexedSeq, Chunk.fromIterable, Chunk.empty, "IndexedSeq")

  val arrayOfIntsCodec: AvroCodec.ExtendedBinaryCodec[Array[Int]] = AvroCodec.schemaBasedBinaryCodec[Array[Int]]
  val googleMapsAPICodec: AvroCodec.ExtendedBinaryCodec[GoogleMapsAPI.DistanceMatrix] = {
    implicit val s1: Schema[GoogleMapsAPI.Value] = DeriveSchema.gen
    implicit val s2: Schema[GoogleMapsAPI.Elements] = DeriveSchema.gen
    implicit val s3: Schema[GoogleMapsAPI.Rows] = DeriveSchema.gen
    AvroCodec.schemaBasedBinaryCodec(DeriveSchema.gen[GoogleMapsAPI.DistanceMatrix])
  }
}