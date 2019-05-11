package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.bullet.borer.{Codec, Decoder, Encoder, Reader, Writer}
import io.bullet.borer.Borer.Error._
import io.bullet.borer.Json.DecodingConfig
import io.bullet.borer.derivation.MapBasedCodecs._

import scala.util.control.NonFatal

object BorerJsonEncodersDecoders {
  val decodingConfig = DecodingConfig(
    maxNumberMantissaDigits = 1000000, /*WARNING: don't do this for open-systems*/
    maxNumberAbsExponent = 1000000, /*WARNING: don't do this for open-systems*/
    readDecimalNumbersOnlyAsNumberStrings = true)
  implicit val bigIntDec: Decoder[BigInt] =
    Decoder.forJBigInteger(maxJsonNumberStringLength = 1000000).map(x => new BigInt(x)) /*WARNING: don't do this for open-systems*/
  implicit val bigDecimalDec: Decoder[BigDecimal] =
    Decoder.forJBigDecimal(maxJsonNumberStringLength = 1000000).map(x => new BigDecimal(x, MathContext.UNLIMITED)) /*WARNING: don't do this for open-systems*/
  implicit val Codec(anyRefsEnc: Encoder[AnyRefs], anyRefsDec: Decoder[AnyRefs]) = deriveCodec[AnyRefs]
  implicit val Codec(charEnc: Encoder[Char], charDec: Decoder[Char]) = stringCodec(_.charAt(0))
  implicit val Codec(extractFieldsEnc: Encoder[ExtractFields], extractFieldsDec: Decoder[ExtractFields]) =
    deriveCodec[ExtractFields]
  implicit val Codec(googleMapsAPIEnc: Encoder[DistanceMatrix], googleMapsAPIDec: Decoder[DistanceMatrix]) = {
    implicit val c1: Codec[Value] = deriveCodec[Value]
    implicit val c2: Codec[Elements] = deriveCodec[Elements]
    implicit val c3: Codec[Rows] = deriveCodec[Rows]
    deriveCodec[DistanceMatrix]
  }
  implicit val Codec(missingRequiredFieldsEnc: Encoder[MissingRequiredFields],
                     missingRequiredFieldsDec: Decoder[MissingRequiredFields]) = deriveCodec[MissingRequiredFields]
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
    (w: Writer, value: T#Value) => w.writeString(value.toString),
    (r: Reader) => {
      val v = r.readString()
      e.values.iterator.find(_.toString == v)
        .getOrElse(throw new InvalidInputData(r.position, s"Expected [String] from enum $e, but got $v"))
    })

  def stringCodec[T](f: String => T): Codec[T] = Codec(
    (w: Writer, value: T) => w.writeString(value.toString),
    (r: Reader) =>
      try f(r.readString()) catch { case NonFatal(e) => throw new InvalidInputData(r.position, e.getMessage) })
}