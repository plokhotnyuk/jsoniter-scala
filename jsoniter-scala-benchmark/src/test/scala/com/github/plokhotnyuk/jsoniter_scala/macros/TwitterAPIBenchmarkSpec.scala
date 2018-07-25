package com.github.plokhotnyuk.jsoniter_scala.macros

class TwitterAPIBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new TwitterAPIBenchmark
  
  "TwitterAPIBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: cannot alter uPickle to store Long as JSON number
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes empty collections
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe TwitterAPI.compactJsonString
      //FIXME: circe serializes empty collections
      //toString(benchmark.writeCirce()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJacksonScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.writeJsoniterScala()) shouldBe TwitterAPI.compactJsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe TwitterAPI.compactJsonString
      //FIXME: Play-JSON serializes empty collections
      //toString(benchmark.writePlayJson()) shouldBe TwitterAPI.compactJsonString
      //FIXME: cannot alter uPickle to store Long as JSON number
      //toString(benchmark.writeUPickle()) shouldBe TwitterAPI.compactJsonString
    }
  }
}