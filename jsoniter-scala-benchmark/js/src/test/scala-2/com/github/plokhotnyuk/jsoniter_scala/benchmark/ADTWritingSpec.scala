package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ADTWritingSpec extends BenchmarkSpecBase {
  def benchmark: ADTWriting = new ADTWriting {
    setup()
  }

  "ADTWriting" should {
    "write properly" in {
      val b = benchmark
      toString(b.avSystemGenCodec()) shouldBe b.jsonString1
      toString(b.borer()) shouldBe b.jsonString1
      toString(b.circe()) shouldBe b.jsonString1
      toString(b.circeJsoniter()) shouldBe b.jsonString1
      toString(b.jsoniterScala()) shouldBe b.jsonString1
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString1
      toString(b.playJson()) shouldBe b.jsonString1
      toString(b.playJsonJsoniter()) shouldBe b.jsonString1
      toString(b.smithy4sJson()) shouldBe b.jsonString1
      toString(b.uPickle()) shouldBe b.jsonString1
      //FIXME: zio-schema-json doesn't serialize the discriminator field
      //toString(b.zioJson()) shouldBe b.jsonString1
    }
  }
}