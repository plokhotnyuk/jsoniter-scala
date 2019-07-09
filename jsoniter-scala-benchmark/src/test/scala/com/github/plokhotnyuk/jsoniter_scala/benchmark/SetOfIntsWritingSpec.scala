package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs.setOfIntsCodec
import com.github.plokhotnyuk.jsoniter_scala.core.readFromArray

import scala.collection.immutable.Set

class SetOfIntsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new SetOfIntsWriting {
    setup()
  }
  
  "SetOfIntsWriting" should {
    "serialize properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.scalikeJackson()) shouldBe benchmark.jsonString
      readFromArray[Set[Int]](benchmark.sprayJson()) shouldBe benchmark.obj
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}