/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import scala.annotation.tailrec

class JsonCodecMakerJVMSpec extends VerifyingSpec {
  import com.github.plokhotnyuk.jsoniter_scala.macros.NamespacePollutions._

  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize JVM-only collection types" in {
      verifySerDeser(make[_root_.scala.collection.concurrent.TrieMap[String, Int]],
        _root_.scala.collection.concurrent.TrieMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
      verifySerDeser(make[_root_.scala.collection.mutable.WeakHashMap[String, Int]],
        _root_.scala.collection.mutable.WeakHashMap[String, Int]("WWW" -> 1, "VVV" -> 2),
        """{"WWW":1,"VVV":2}""")
    }
    "serialize and deserialize Java enumerations with more than 8 values" in {
      val months = _root_.java.time.Month.values.toList
      verifySerDeser(make[List[_root_.java.time.Month]], months, months.map(_.name).mkString("[\"", "\",\"", "\"]"))
      verifySerDeser(make[List[_root_.java.time.Month]](CodecMakerConfig.withJavaEnumValueNameMapper(JsonCodecMaker.enforce_snake_case)),
        months, months.map(_.name.toLowerCase).mkString("[\"", "\",\"", "\"]"))
      verifySerDeser(make[Map[_root_.java.time.Month, Int]], Map(_root_.java.time.Month.DECEMBER -> 12), """{"DECEMBER":12}""")
      verifySerDeser(make[Map[_root_.java.time.Month, Int]](CodecMakerConfig.withJavaEnumValueNameMapper(JsonCodecMaker.enforce_snake_case)),
        Map(_root_.java.time.Month.DECEMBER -> 12), """{"december":12}""")
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
      assert(!readStackTrace.contains("d2"))
      assert(!readStackTrace.contains("decodeValue"))
      val writeStackTrace = TestUtils.assertStackOverflow(writeToString[Nested](construct()))
      assert(writeStackTrace.contains("e1"))
      assert(!writeStackTrace.contains("e3"))
      assert(!writeStackTrace.contains("encodeValue"))
    }
  }
}
