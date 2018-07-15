package com.github.plokhotnyuk.jsoniter_scala.macros

class NestedStructsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new NestedStructsBenchmark {
    setup()
  }
  
  "NestedObjectsBenchmark" should {
    "deserialize properly" in {
      //FIXME: cannot alter AVSystem GenCodec to parse missing optional fields as None
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: cannot alter uPickle to parse missing optional fields as None
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: cannot alter AVSystem GenCodec to don't serialize empty optional values
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: cannot alter uPickle to don't serialize empty optional values
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}