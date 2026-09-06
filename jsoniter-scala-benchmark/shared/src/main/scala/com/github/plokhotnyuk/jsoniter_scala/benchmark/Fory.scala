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

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI.Tweet
import org.apache.fory.json.ForyJson
import org.apache.fory.json.scala.{ForyJsonScala, ScalaTypeRef}
import org.apache.fory.reflect.TypeRef
import scala.collection.immutable.{ArraySeq, IntMap}
import scala.collection.mutable

object Fory {
  val foryJson: ForyJson =
    ForyJsonScala.builder()
      .maxDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
      .writeNullFields(false)
      .build()
  val arraySeqOfBooleansType: TypeRef[ArraySeq[Boolean]] = ScalaTypeRef[ArraySeq[Boolean]]
  val intMapOfBooleansType: TypeRef[IntMap[Boolean]] = ScalaTypeRef[IntMap[Boolean]]
  val listOfBooleansType: TypeRef[List[Boolean]] = ScalaTypeRef[List[Boolean]]
  val mapOfIntsToBooleansType: TypeRef[Map[Int, Boolean]] = ScalaTypeRef[Map[Int, Boolean]]
  val mutableLongMapOfBooleansType: TypeRef[mutable.LongMap[Boolean]] = ScalaTypeRef[mutable.LongMap[Boolean]]
  val mutableMapOfIntsToBooleansType: TypeRef[mutable.Map[Int, Boolean]] = ScalaTypeRef[mutable.Map[Int, Boolean]]
  val mutableSetOfIntsType: TypeRef[mutable.Set[Int]] = ScalaTypeRef[mutable.Set[Int]]
  val setOfIntsType: TypeRef[Set[Int]] = ScalaTypeRef[Set[Int]]
  val seqOfTweetsType: TypeRef[Seq[Tweet]] = ScalaTypeRef[Seq[Tweet]]
  val vectorOfBooleansType: TypeRef[Vector[Boolean]] = ScalaTypeRef[Vector[Boolean]]
}
