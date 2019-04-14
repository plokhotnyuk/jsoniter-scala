package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.bullet.borer.{Codec, Decoder, Encoder, Reader, Writer}
import io.bullet.borer.Borer.Error._
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.util.control.NonFatal

object BorerJsonEncodersDecoders {
  implicit val Codec(charEnc: Encoder[Char], charDec: Decoder[Char]) = stringCodec(_.charAt(0))
  implicit val Codec(googleMapsAPIEnc: Encoder[DistanceMatrix], googleMapsAPIDec: Decoder[DistanceMatrix]) = {
    implicit val c1: Codec[Value] = deriveCodec[Value]
    implicit val c2: Codec[Elements] = deriveCodec[Elements]
    implicit val c3: Codec[Rows] = deriveCodec[Rows]
    deriveCodec[DistanceMatrix]
  }
  implicit val Codec(primitivesEnc: Encoder[Primitives], primitivesDec: Decoder[Primitives]) = deriveCodec[Primitives]
  implicit val Codec(suitADTEnc: Encoder[SuitADT], suitADTDec: Decoder[SuitADT]) = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    stringCodec(suite.apply)
  }
  implicit val Codec(suitEnc: Encoder[Suit], suitDec: Decoder[Suit]) = stringCodec(Suit.valueOf)
  implicit val Codec(suitEnumEnc: Encoder[SuitEnum], suitEnumDec: Decoder[SuitEnum]) = enumCodec(SuitEnum)
  implicit val Codec(uuidEnc: Encoder[UUID], uuidDec: Decoder[UUID]) = stringCodec(UUID.fromString)

  def enumCodec[T <: scala.Enumeration](e: T): Codec[T#Value] = Codec(
    new Encoder[T#Value] {
      override def write(w: Writer, value: T#Value): Writer = w.writeString(value.toString)
    }, new Decoder[T#Value] {
      override def read(r: Reader): T#Value = {
        val v = r.readString()
        e.values.iterator.find(_.toString == v)
          .getOrElse(throw new InvalidJsonData(r.position, s"Expected [String] from enum $e, but got $v"))
      }
    })

  def stringCodec[T](f: String => T): Codec[T] = Codec(
    new Encoder[T] {
      override def write(w: Writer, value: T): Writer = w.writeString(value.toString)
    }, new Decoder[T] {
      override def read(r: Reader): T =
        try f(r.readString()) catch { case NonFatal(e) => throw new InvalidJsonData(r.position, e.getMessage) }
    })
}