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
      verifySerDeser(make[_root_.scala.collection.immutable.LazyList[Int]],
        _root_.scala.collection.immutable.LazyList[Int](1, 2), """[1,2]""")
      verifySerDeser(make[_root_.scala.collection.mutable.Stack[Int]],
        _root_.scala.collection.mutable.Stack[Int](1, 2), """[1,2]""")
      verifySerDeser(make[_root_.scala.collection.mutable.ArrayDeque[Int]],
        _root_.scala.collection.mutable.ArrayDeque[Int](1, 2), """[1,2]""")
      verifySer(make[_root_.scala.collection.mutable.PriorityQueue[Int]],
        _root_.scala.collection.mutable.PriorityQueue[Int](2, 1), """[2,1]""")
      verifyDeserByCheck(make[_root_.scala.collection.mutable.PriorityQueue[Int]],
        """[2,1]""", (x: _root_.scala.collection.mutable.PriorityQueue[Int]) => x.toList shouldBe List(2, 1))
    }
  }
}
