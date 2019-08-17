package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.bullet.borer.{AdtEncodingStrategy, Codec, Decoder, Encoder, Reader, Writer}
import io.bullet.borer.Borer.Error._
import io.bullet.borer.Json.DecodingConfig
import io.bullet.borer.derivation.ArrayBasedCodecs
import io.bullet.borer.derivation.MapBasedCodecs._

object BorerJsonEncodersDecoders {
  val decodingConfig = DecodingConfig(
    maxNumberMantissaDigits = 200,
    maxNumberAbsExponent = 999,
    readDecimalNumbersOnlyAsNumberStrings = true)
  implicit val bigIntDec: Decoder[BigInt] =
    Decoder.forJBigInteger(maxJsonNumberStringLength = 1000000).map(x => new BigInt(x)) /*WARNING: don't do this for open-systems*/
  implicit val bigDecimalDec: Decoder[BigDecimal] =
    Decoder.forJBigDecimal(maxJsonNumberStringLength = 1000000).map(x => new BigDecimal(x, MathContext.UNLIMITED)) /*WARNING: don't do this for open-systems*/
  implicit val flatAdtEncoding: AdtEncodingStrategy = AdtEncodingStrategy.flat(typeMemberName = "type")
  implicit val Codec(charEnc: Encoder[Char], charDec: Decoder[Char]) = stringCodec(_.charAt(0))
  implicit val Codec(adtEnc: Encoder[ADTBase], adtDec: Decoder[ADTBase]) = {
    implicit val c1: Codec[X] = deriveCodec
    implicit val c2: Codec[Y] = deriveCodec
    implicit val c3: Codec[Z] = deriveCodec
    deriveCodec[ADTBase]
  }
  implicit val Codec(anyRefsEnc: Encoder[AnyRefs], anyRefsDec: Decoder[AnyRefs]) = deriveCodec[AnyRefs]
  implicit val Codec(anyValsEnc: Encoder[AnyVals], anyValsDec: Decoder[AnyVals]) = {
    implicit val c1: Codec[ByteVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c2: Codec[ShortVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c3: Codec[IntVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c4: Codec[LongVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c5: Codec[BooleanVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c6: Codec[CharVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c7: Codec[DoubleVal] = ArrayBasedCodecs.deriveUnaryCodec
    implicit val c8: Codec[FloatVal] = ArrayBasedCodecs.deriveUnaryCodec
    deriveCodec[AnyVals]
  }
  implicit val Codec(extractFieldsEnc: Encoder[ExtractFields], extractFieldsDec: Decoder[ExtractFields]) =
    deriveCodec[ExtractFields]
  implicit val Codec(geoJsonEnc: Encoder[GeoJSON.GeoJSON], geoJsonDec: Decoder[GeoJSON.GeoJSON]) = {
    implicit lazy val c1: Codec[GeoJSON.Geometry] = {
      implicit val c11: Codec[GeoJSON.Point] = deriveCodec
      implicit val c12: Codec[GeoJSON.MultiPoint] = deriveCodec
      implicit val c13: Codec[GeoJSON.LineString] = deriveCodec
      implicit val c14: Codec[GeoJSON.MultiLineString] = deriveCodec
      implicit val c15: Codec[GeoJSON.Polygon] = deriveCodec
      implicit val c16: Codec[GeoJSON.MultiPolygon] = deriveCodec
      implicit val c17: Codec[GeoJSON.GeometryCollection] = deriveCodec
      deriveCodec[GeoJSON.Geometry]
    }
    implicit val c2: Codec[GeoJSON.Feature] = deriveCodec
    implicit val c3: Codec[GeoJSON.FeatureCollection] = deriveCodec
    deriveCodec[GeoJSON.GeoJSON]
  }
  implicit val Codec(googleMapsAPIEnc: Encoder[GoogleMapsAPI.DistanceMatrix],
  googleMapsAPIDec: Decoder[GoogleMapsAPI.DistanceMatrix]) = {
    implicit val c1: Codec[GoogleMapsAPI.Value] = deriveCodec
    implicit val c2: Codec[GoogleMapsAPI.Elements] = deriveCodec
    implicit val c3: Codec[GoogleMapsAPI.Rows] = deriveCodec
    deriveCodec[GoogleMapsAPI.DistanceMatrix]
  }
  implicit val Codec(missingRequiredFieldsEnc: Encoder[MissingRequiredFields],
  missingRequiredFieldsDec: Decoder[MissingRequiredFields]) = deriveCodec[MissingRequiredFields]
  implicit val Codec(nestedStructsEnc: Encoder[NestedStructs],
  nestedStructsDec: Decoder[NestedStructs]) = deriveCodec[NestedStructs]
  implicit val Codec(openRTBBidRequestEnc: Encoder[OpenRTB.BidRequest],
  openRTBBidRequestDec: Decoder[OpenRTB.BidRequest]) = {
    implicit val c1: Codec[OpenRTB.Segment] = deriveCodec
    implicit val c2: Codec[OpenRTB.Format] = deriveCodec
    implicit val c3: Codec[OpenRTB.Deal] = deriveCodec
    implicit val c4: Codec[OpenRTB.Metric] = deriveCodec
    implicit val c5: Codec[OpenRTB.Banner] = deriveCodec
    implicit val c6: Codec[OpenRTB.Audio] = deriveCodec
    implicit val c7: Codec[OpenRTB.Video] = deriveCodec
    implicit val c8: Codec[OpenRTB.Native] = deriveCodec
    implicit val c9: Codec[OpenRTB.Pmp] = deriveCodec
    implicit val c10: Codec[OpenRTB.Producer] = deriveCodec
    implicit val c11: Codec[OpenRTB.Data] = deriveCodec
    implicit val c12: Codec[OpenRTB.Content] = deriveCodec
    implicit val c13: Codec[OpenRTB.Publisher] = deriveCodec
    implicit val c14: Codec[OpenRTB.Geo] = deriveCodec
    implicit val c15: Codec[OpenRTB.Imp] = deriveCodec
    implicit val c16: Codec[OpenRTB.Site] = deriveCodec
    implicit val c17: Codec[OpenRTB.App] = deriveCodec
    implicit val c18: Codec[OpenRTB.Device] = deriveCodec
    implicit val c19: Codec[OpenRTB.User] = deriveCodec
    implicit val c20: Codec[OpenRTB.Source] = deriveCodec
    implicit val c21: Codec[OpenRTB.Reqs] = deriveCodec
    deriveCodec[OpenRTB.BidRequest]
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
  implicit val Codec(twitterAPIEnc: Encoder[TwitterAPI.Tweet], twitterAPIDec: Decoder[TwitterAPI.Tweet]) = {
    implicit val c1: Codec[TwitterAPI.UserMentions] = deriveCodec
    implicit val c2: Codec[TwitterAPI.Urls] = deriveCodec
    implicit val c3: Codec[TwitterAPI.Entities] = deriveCodec
    implicit val c4: Codec[TwitterAPI.Url] = deriveCodec
    implicit val c5: Codec[TwitterAPI.UserEntities] = deriveCodec
    implicit val c6: Codec[TwitterAPI.User] = deriveCodec
    implicit val c7: Codec[TwitterAPI.RetweetedStatus] = deriveCodec
    deriveCodec[TwitterAPI.Tweet]
  }
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
    (r: Reader) => f(r.readString()))
}