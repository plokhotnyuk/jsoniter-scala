package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

class JsonCodecMakerKnownIssueWorkaroundSpec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize types that have back-quoted constructor params with identical prefixes" in {
      case class ScalaBug8831(`o-o`: String, o: String)

      verifySerDeser(make[ScalaBug8831], ScalaBug8831("VVV", "WWW"), """{"o-o":"VVV","o":"WWW"}""")
    }
  }
}
