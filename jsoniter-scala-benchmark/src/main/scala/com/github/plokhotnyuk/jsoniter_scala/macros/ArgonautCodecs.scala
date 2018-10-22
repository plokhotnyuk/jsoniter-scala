package com.github.plokhotnyuk.jsoniter_scala.macros

import argonaut._
import argonaut.Argonaut._

object ArgonautCodecs {
  implicit val extractFieldsCodec: CodecJson[ExtractFields] =
    casecodec2(ExtractFields.apply, ExtractFields.unapply)("s", "i")
}
