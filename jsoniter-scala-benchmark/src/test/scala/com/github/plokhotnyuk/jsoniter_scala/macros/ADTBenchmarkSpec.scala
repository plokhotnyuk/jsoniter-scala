package com.github.plokhotnyuk.jsoniter_scala.macros

class ADTBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ADTBenchmark
  
  "ADTBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: cannot alter uPickle discriminator name and value for ADT
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString1
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString2
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString1
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString1
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString3
      // FIXME: cannot alter uPickle discriminator name and value for ADT
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}