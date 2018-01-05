package com.github.plokhotnyuk.jsoniter_scala.macros

class StringBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new StringBenchmark
  
  "StringBenchmark" should {
    "deserialize properly" in {
      benchmark.readAsciiCirce() shouldBe benchmark.asciiObj
      benchmark.readAsciiJackson() shouldBe benchmark.asciiObj
      benchmark.readAsciiJsoniter() shouldBe benchmark.asciiObj
      //FIXME: find proper way to parse string value in Play JSON
      //benchmark.readAsciiStringPlay() shouldBe benchmark.asciiStringObj
      benchmark.readNonAsciiCirce() shouldBe benchmark.nonAsciiObj
      benchmark.readNonAsciiJackson() shouldBe benchmark.nonAsciiObj
      benchmark.readNonAsciiJsoniter() shouldBe benchmark.nonAsciiObj
      //FIXME: find proper way to parse string value in Play JSON
      //benchmark.readNonAsciiStringPlay() shouldBe benchmark.nonAsciiStringObj
    }
    "serialize properly" in {
      toString(benchmark.writeAsciiCirce()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiJackson()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiJsoniter()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiJsoniterPrealloc()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiPlay()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeNonAsciiCirce()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiJackson()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiJsoniter()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiJsoniterPrealloc()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiPlay()) shouldBe benchmark.nonAsciiJsonString
    }
  }
}