package com.github.plokhotnyuk.jsoniter_scala.macros

import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

object CirceEncodersDecoders {
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val aEncoder: Encoder[A] = deriveEncoder[A]
  implicit val aDecoder: Decoder[A] = deriveDecoder[A]
  implicit val bEncoder: Encoder[B] = deriveEncoder[B]
  implicit val bDecoder: Decoder[B] = deriveDecoder[B]
  implicit val cEncoder: Encoder[C] = deriveEncoder[C]
  implicit val cDecoder: Decoder[C] = deriveDecoder[C]
  implicit val adtEncoder: Encoder[AdtBase] = deriveEncoder[AdtBase]
  implicit val adtDecoder: Decoder[AdtBase] = deriveDecoder[AdtBase]
}