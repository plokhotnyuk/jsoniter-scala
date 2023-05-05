package com.github.plokhotnyuk.jsoniter_scala.benchmark

class NestedStructsWritingSpec extends BenchmarkSpecBase {
  def benchmark: NestedStructsWriting = new NestedStructsWriting {
    setup()
  }

  "NestedStructsWriting" should {
    "write properly" in {
      val b = benchmark
      //FIXME: Borer throws io.bullet.borer.Borer$Error$Unsupported: The JSON renderer doesn't support more than 64 JSON Array/Object nesting levels
      //toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.smithy4sJson()) shouldBe b.jsonString
      toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
    }
  }
}