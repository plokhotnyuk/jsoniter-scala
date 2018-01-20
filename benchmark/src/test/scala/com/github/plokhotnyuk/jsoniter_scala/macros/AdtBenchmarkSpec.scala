package com.github.plokhotnyuk.jsoniter_scala.macros

class AdtBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new AdtBenchmark
  
  "AdtBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: circe appends discriminator as a last field
      //toString(benchmark.writeAdtCirce()) shouldBe benchmark.adtJsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}