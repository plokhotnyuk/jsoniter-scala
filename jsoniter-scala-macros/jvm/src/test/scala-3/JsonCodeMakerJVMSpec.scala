package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

import scala.annotation.tailrec


//TODO:  java-only enums


class JsonCodecMakerJVMSpec extends VerifyingSpec {

  "JsonCodecMaker.make generate codes which" should {
    "serialize and deserialize recursive structures with an implicit codec using a minimal thread stack" in {
      case class Nested(n: Option[Nested] = _root_.scala.None)

      @tailrec
      def construct(d: Int = 1000000, n: Nested = Nested()): Nested =
        if (d <= 0) n
        else construct(d - 1, Nested(Some(n)))

      implicit val codecOfNestedStructs: JsonValueCodec[Nested] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
      val bytes = ("{" + "\"n\":{" * 1000000 + "}" * 1000000 + "}").getBytes
      val readStackTrace = AssertionUtils.assertStackOverflow(readFromArray[Nested](bytes)).split('\n')
      assert(readStackTrace.exists(_.matches(".*\\.d0(\\$[0-9]+)*\\(.*")))
      assert(!readStackTrace.exists(_.matches(".*\\.d1(\\$[0-9]+)*\\(.*")))
      assert(!readStackTrace.exists(_.matches(".*\\.decodeValue(\\$[0-9]+)*\\(")))
      val writeStackTrace = AssertionUtils.assertStackOverflow(writeToArray[Nested](construct())).split('\n')
      assert(writeStackTrace.exists(_.matches(".*\\.e0(\\$[0-9]+)*\\(.*")))
      assert(!writeStackTrace.exists(_.matches(".*\\.e1(\\$[0-9]+)*\\(.*")))
      assert(!writeStackTrace.exists(_.matches(".*\\.encodeValue(\\$[0-9]+)*\\(.*")))
    }
  }
}
