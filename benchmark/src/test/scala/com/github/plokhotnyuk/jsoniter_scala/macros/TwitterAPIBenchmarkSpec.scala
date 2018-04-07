package com.github.plokhotnyuk.jsoniter_scala.macros

class TwitterAPIBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new TwitterAPIBenchmark
  
  "TwitterAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: dsl-json cannot find decoder for interface scala.collection.Seq
      //benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: circe serializes empty collections
      //toString(benchmark.writeCirce()) shouldBe TwitterAPI.compactJsonString
      //FIXME: dsl-json serializes empty collections
      //toString(benchmark.writeDslJsonJava()) shouldBe TwitterAPI.compactJsonString
      //val writer = benchmark.writeDslJsonJavaPrealloc()
      //toString(writer.getByteBuffer, writer.size()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJacksonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJsoniterScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writePlayJson()) shouldBe TwitterAPI.compactJsonString
    }
  }
}