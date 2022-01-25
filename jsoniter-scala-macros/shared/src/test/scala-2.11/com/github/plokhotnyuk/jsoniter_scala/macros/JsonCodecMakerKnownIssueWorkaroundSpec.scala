package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker.make

class JsonCodecMakerKnownIssueWorkaroundSpec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize types that have back-quoted constructor params with identical prefixes defined in some order" in {
      case class ScalaBug8831(o: String, `o-o`: String)

      verifySerDeser(make[ScalaBug8831], ScalaBug8831("WWW", "VVV"), """{"o":"WWW","o-o":"VVV"}""")
    }
  }
}
