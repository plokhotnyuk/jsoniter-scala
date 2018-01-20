package com.github.plokhotnyuk.jsoniter_scala.macros

class ExtractFieldsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ExtractFieldsBenchmark
  
  "ExtractFieldsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
  }
}