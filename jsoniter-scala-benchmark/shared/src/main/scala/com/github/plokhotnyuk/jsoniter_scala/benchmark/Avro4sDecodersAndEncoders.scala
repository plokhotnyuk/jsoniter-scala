package com.github.plokhotnyuk.jsoniter_scala.benchmark

import scala.jdk.CollectionConverters._
import org.apache.avro._
import com.sksamuel.avro4s._

object Avro4sDecodersAndEncoders {
  given[T](using schemaFor: SchemaFor[T]): SchemaFor[IndexedSeq[T]] =
    schemaFor.map(SchemaBuilder.array.items(_))

  given[T](using encoder: Encoder[T]): Encoder[IndexedSeq[T]] = new Encoder[IndexedSeq[T]] {
    override def encode(schema: Schema): IndexedSeq[T] => AnyRef = {
      require(schema.getType == Schema.Type.ARRAY)
      val elementEncoder = encoder.encode(schema.getElementType)
      { t => t.map(elementEncoder.apply).toList.asJava }
    }
  }

  given[T](using decoder: Decoder[T]): Decoder[IndexedSeq[T]] = new Decoder[IndexedSeq[T]] {
    def decode(schema: Schema): Any => IndexedSeq[T] = {
      require(schema.getType == Schema.Type.ARRAY, {
        s"Require schema type ARRAY (was $schema)"
      })
      val decodeT = decoder.decode(schema.getElementType)
      { value =>
        value match {
          case list: java.util.Collection[_] => list.asScala.map(decodeT).toIndexedSeq
          case list: Iterable[_] => list.map(decodeT).toIndexedSeq
          case array: Array[_] => array.toIndexedSeq.map(decodeT)
          case other => throw new Avro4sDecodingException("Unsupported collection type " + other, value)
        }
      }
    }
  }

  val arrayOfIntsSchemaFor: SchemaFor[Array[Int]] = SchemaFor[Array[Int]]
  val arrayOfIntsDecoder: Decoder[Array[Int]] = Decoder[Array[Int]]
  val arrayOfIntsEncoder: Encoder[Array[Int]] = Encoder[Array[Int]]
  val googleMapsAPISchemaFor: SchemaFor[GoogleMapsAPI.DistanceMatrix] = SchemaFor[GoogleMapsAPI.DistanceMatrix]
  val googleMapsAPIDecoder: Decoder[GoogleMapsAPI.DistanceMatrix] = Decoder[GoogleMapsAPI.DistanceMatrix]
  val googleMapsAPIEncoder: Encoder[GoogleMapsAPI.DistanceMatrix] = Encoder[GoogleMapsAPI.DistanceMatrix]

  def read[T](schemaFor: SchemaFor[T], decoder: Decoder[T], bytes: Array[Byte]): T =
    AvroInputStream.binary(using decoder).from(bytes).build(schemaFor.schema).iterator.next()

  def write[T](schemaFor: SchemaFor[T], encoder: Encoder[T], obj: T): Array[Byte] = {
    val baos = new java.io.ByteArrayOutputStream
    val out = AvroOutputStream.binary(using schemaFor, encoder).to(baos).build()
    try out.write(obj)
    finally out.close()
    baos.toByteArray
  }
}