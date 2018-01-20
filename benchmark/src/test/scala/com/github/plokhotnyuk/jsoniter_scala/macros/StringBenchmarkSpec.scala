package com.github.plokhotnyuk.jsoniter_scala.macros

class StringBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new StringBenchmark
  
  "StringBenchmark" should {
    "deserialize properly" in {
      benchmark.readAsciiCirce() shouldBe benchmark.asciiObj
      benchmark.readAsciiJacksonScala() shouldBe benchmark.asciiObj
      benchmark.readAsciiJsoniterScala() shouldBe benchmark.asciiObj
      //FIXME: find proper way to parse string value in Play JSON
      //benchmark.readAsciiStringPlayJson() shouldBe benchmark.asciiStringObj
      benchmark.readNonAsciiCirce() shouldBe benchmark.nonAsciiObj
      benchmark.readNonAsciiJacksonScala() shouldBe benchmark.nonAsciiObj
      benchmark.readNonAsciiJsoniterScala() shouldBe benchmark.nonAsciiObj
      //FIXME: find proper way to parse string value in Play JSON
      //benchmark.readNonAsciiStringPlayJson() shouldBe benchmark.nonAsciiStringObj
    }
    "serialize properly" in {
      toString(benchmark.writeAsciiCirce()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiJacksonScala()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiJsoniterScala()) shouldBe benchmark.asciiJsonString
      toString(benchmark.preallocatedBuf, benchmark.writeAsciiJsoniterScalaPrealloc()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeAsciiPlayJson()) shouldBe benchmark.asciiJsonString
      toString(benchmark.writeNonAsciiCirce()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiJacksonScala()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiJsoniterScala()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.preallocatedBuf, benchmark.writeNonAsciiJsoniterScalaPrealloc()) shouldBe benchmark.nonAsciiJsonString
      toString(benchmark.writeNonAsciiPlayJson()) shouldBe benchmark.nonAsciiJsonString
    }
  }
}