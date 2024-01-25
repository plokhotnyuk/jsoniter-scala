package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import scala.annotation.tailrec

class JsonCodecMakerJVMSpec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize JVM-only collection types" in {
      verifySerDeser(make[_root_.scala.collection.concurrent.TrieMap[String, Int]],
        _root_.scala.collection.concurrent.TrieMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[_root_.scala.collection.mutable.WeakHashMap[String, Int]],
        _root_.scala.collection.mutable.WeakHashMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
    }
    "serialize and deserialize recursive structures with an implicit codec using a minimal thread stack" in {
      case class Nested(n: Option[Nested] = _root_.scala.None)

      @tailrec
      def construct(d: Int = 1000000, n: Nested = Nested()): Nested =
        if (d <= 0) n
        else construct(d - 1, Nested(_root_.scala.Some(n)))

      implicit val codecOfNestedStructs: JsonValueCodec[Nested] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
      val json = "{" + "\"n\":{" * 1000000 + "}" * 1000000 + "}"
      val readStackTrace = TestUtils.assertStackOverflow(readFromString[Nested](json))
      assert(readStackTrace.contains("d0"))
      assert(!readStackTrace.contains("d1"))
      assert(!readStackTrace.contains("decodeValue"))
      val writeStackTrace = TestUtils.assertStackOverflow(writeToString[Nested](construct()))
      assert(writeStackTrace.contains("e0"))
      assert(!writeStackTrace.contains("e1"))
      assert(!writeStackTrace.contains("encodeValue"))
    }
  }
}
