package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

import scala.annotation.tailrec

class JsonCodecMakerJVMSpec extends VerifyingSpec {
  import NamespacePollutions._

  "JsonCodecMaker.make generate codes which" should {
    "serialize and deserialize recursive structures with an implicit codec using a minimal thread stack" in {
      case class Nested(n: Option[Nested] = _root_.scala.None)

      @tailrec
      def construct(d: Int = 1000000, n: Nested = Nested()): Nested =
        if (d <= 0) n
        else construct(d - 1, Nested(Some(n)))

      implicit val codecOfNestedStructs: JsonValueCodec[Nested] = make(CodecMakerConfig.withAllowRecursiveTypes(true))
      val bytes = ("{" + "\"n\":{" * 1000000 + "}" * 1000000 + "}").getBytes
      val readStackTrace = AssertionUtils.assertStackOverflow(readFromArray[Nested](bytes))
      assert(readStackTrace.contains(".d0("))
      assert(!readStackTrace.contains(".d1("))
      assert(!readStackTrace.contains(".decodeValue("))
      val writeStackTrace = AssertionUtils.assertStackOverflow(writeToArray[Nested](construct()))
      assert(writeStackTrace.contains(".e0("))
      assert(!writeStackTrace.contains(".e1("))
      assert(!writeStackTrace.contains(".encodeValue("))
    }
  }
}
