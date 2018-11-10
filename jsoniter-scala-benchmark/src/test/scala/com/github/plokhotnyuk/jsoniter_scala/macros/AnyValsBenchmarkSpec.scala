package com.github.plokhotnyuk.jsoniter_scala.macros

class AnyValsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new AnyValsBenchmark
  
  "AnyValsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec wraps values of value classes by extra JSON objects, see https://github.com/AVSystem/scala-commons/issues/91
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //benchmark.readCirce() shouldBe benchmark.obj
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      //benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: uPickle parses Long from JSON string only
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec wraps values of value classes by extra JSON objects, see https://github.com/AVSystem/scala-commons/issues/91
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes Long as JSON string
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}