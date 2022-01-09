package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

class JsonCodecMaker212Spec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker212.make generate codes which" should {
    "serialize and deserialize types that have back-quoted constructor params with identical prefixes" in {
      case class ScalaBug8831(`o-o`: String, o: String)

      verifySerDeser(make[ScalaBug8831], ScalaBug8831("VVV", "WWW"), """{"o-o":"VVV","o":"WWW"}""")
    }
  }
}
