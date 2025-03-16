package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfYearsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfYearsWriting = new ArrayOfYearsWriting {
    setup()
  }

  "ArrayOfYearsWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.jsonString
      toString(b.circe()) shouldBe b.jsonString
      toString(b.circeJsoniter()) shouldBe b.jsonString
      //FIXME: jackson 3.0.0-rc1 serializes `java.time.Year` values as numbers
      //toString(b.jacksonScala()) shouldBe b.jsonString
      toString(b.json4sJackson()) shouldBe b.jsonString
      toString(b.json4sNative()) shouldBe b.jsonString
      toString(b.jsoniterScala()) shouldBe b.jsonString
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString
      toString(b.playJson()) shouldBe b.jsonString
      toString(b.playJsonJsoniter()) shouldBe b.jsonString
      toString(b.sprayJson()) shouldBe b.jsonString
      toString(b.uPickle()) shouldBe b.jsonString
      toString(b.weePickle()) shouldBe b.jsonString
      toString(b.zioJson()) shouldBe b.jsonString
      toString(b.zioSchemaJson()) shouldBe b.jsonString
    }
  }
}