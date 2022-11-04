package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GoogleMapsAPIPrettyPrintingSpec extends BenchmarkSpecBase {
  def benchmark: GoogleMapsAPIPrettyPrinting = new GoogleMapsAPIPrettyPrinting {
    setup()
  }

  "GoogleMapsAPIPrettyPrinting" should {
    "pretty print properly" in {
      val b = benchmark
      toString(b.jacksonScala()) shouldBe b.jsonString1
      toString(b.json4sJackson()) shouldBe b.jsonString1
      toString(b.json4sNative()) shouldBe b.jsonString3
      toString(b.jsoniterScala()) shouldBe b.jsonString2
      toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()) shouldBe b.jsonString2
      toString(b.smithy4sJson()) shouldBe b.jsonString2
      toString(b.weePickle()) shouldBe b.jsonString2
    }
  }
}