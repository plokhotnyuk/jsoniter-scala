package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ExtractFieldsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ExtractFieldsBenchmark {
    setup()
  }
  
  "ExtractFieldsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
/*
      benchmark.readJsoniterScalaIO() shouldBe benchmark.obj
      benchmark.readJsoniterScalaNIO() shouldBe benchmark.obj
*/
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
  }
}