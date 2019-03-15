package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.bullet.borer.{Codec, Decoder, Encoder}
import io.bullet.borer.derivation.MapBasedCodecs._

object BorerJsonEncodersDecoders {
  implicit val Codec(googleMapsAPIEnc: Encoder[DistanceMatrix], googleMapsAPIDec: Decoder[DistanceMatrix]) = {
    implicit val c1: Codec[Value] = deriveCodec[Value]
    implicit val c2: Codec[Elements] = deriveCodec[Elements]
    implicit val c3: Codec[Rows] = deriveCodec[Rows]
    deriveCodec[DistanceMatrix]
  }
}