package com.github.plokhotnyuk.jsoniter_scala.macros

class NestedStructsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new NestedStructsBenchmark {
    setup()
  }
  
  "NestedObjectsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec cannot parse option values when field is missing
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: cannot alter uPickle to parse missing optional fields as None
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes empty option value as null
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: uPickle serializes empty optional values
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}