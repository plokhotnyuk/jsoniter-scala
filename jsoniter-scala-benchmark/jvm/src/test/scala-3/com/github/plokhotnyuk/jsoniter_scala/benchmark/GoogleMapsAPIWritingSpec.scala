package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIWritingSpec extends BenchmarkSpecBase {
  def benchmark: GoogleMapsAPIWriting = new GoogleMapsAPIWriting {
    setup()
  }

  "GoogleMapsAPIWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.borer()) shouldBe b.compactJsonString1
      toString(b.circe()) shouldBe b.compactJsonString1
      toString(b.circeJsoniter()) shouldBe b.compactJsonString1
      toString(b.jacksonScala()) shouldBe b.compactJsonString1
      toString(b.json4sJackson()) shouldBe b.compactJsonString1
      toString(b.json4sNative()) shouldBe b.compactJsonString1
      toString(b.jsoniterScala()) shouldBe b.compactJsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.compactJsonString1
      toString(b.playJson()) shouldBe b.compactJsonString1
      toString(b.playJsonJsoniter()) shouldBe b.compactJsonString1
      toString(b.smithy4sJson()) shouldBe b.compactJsonString1
      toString(b.sprayJson()) shouldBe b.compactJsonString1
      toString(b.uPickle()) shouldBe b.compactJsonString1
      toString(b.weePickle()) shouldBe b.compactJsonString1
      toString(b.zioJson()) shouldBe b.compactJsonString1
      toString(b.zioSchemaJson()) shouldBe b.compactJsonString1
      toString(b.zioSchemaAvro()) shouldBe toString(b.avroBytes)
      toString(b.avro4s()) shouldBe toString(b.avroBytes)
    }
  }
}