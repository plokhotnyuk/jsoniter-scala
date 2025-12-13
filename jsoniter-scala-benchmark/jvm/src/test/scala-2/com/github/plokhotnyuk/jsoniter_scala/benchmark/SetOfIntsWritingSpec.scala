package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs.setOfIntsCodec
import com.github.plokhotnyuk.jsoniter_scala.core.readFromArray

class SetOfIntsWritingSpec extends BenchmarkSpecBase {
  def benchmark: SetOfIntsWriting = new SetOfIntsWriting {
    setup()
  }

  "SetOfIntsWriting" should {
    "serialize properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.dslJsonScala()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      readFromArray[Set[Int]](b.sprayJson()) shouldBe b.obj
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioBlocks()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
      toString(b.zioSchemaJson()) shouldBe b.jsonString
    }
  }
}