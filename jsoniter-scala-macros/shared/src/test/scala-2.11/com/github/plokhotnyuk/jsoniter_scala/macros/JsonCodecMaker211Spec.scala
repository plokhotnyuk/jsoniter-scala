package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

class JsonCodecMaker211Spec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker211.make generate codes which" should {
    "serialize and deserialize types that have back-quoted constructor params with identical prefixes defined in some order" in {
      case class ScalaBug8831(o: String, `o-o`: String)

      verifySerDeser(make[ScalaBug8831], ScalaBug8831("WWW", "VVV"), """{"o":"WWW","o-o":"VVV"}""")
    }
  }
}
