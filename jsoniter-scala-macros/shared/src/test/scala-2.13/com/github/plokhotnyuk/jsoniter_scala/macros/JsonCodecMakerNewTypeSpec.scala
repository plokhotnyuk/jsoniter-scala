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

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._

class JsonCodecMakerNewTypeSpec extends VerifyingSpec {
  import com.github.plokhotnyuk.jsoniter_scala.macros.NamespacePollutions._

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
