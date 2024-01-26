package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerNewTypeSpec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize new collection types" in {
      verifySerDeser(make[_root_.scala.collection.mutable.CollisionProofHashMap[String, Int]],
        _root_.scala.collection.mutable.CollisionProofHashMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[_root_.scala.collection.immutable.TreeSeqMap[String, Int]],
        _root_.scala.collection.immutable.TreeSeqMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[_root_.scala.collection.immutable.VectorMap[String, Int]],
        _root_.scala.collection.immutable.VectorMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
    }
  }
}
