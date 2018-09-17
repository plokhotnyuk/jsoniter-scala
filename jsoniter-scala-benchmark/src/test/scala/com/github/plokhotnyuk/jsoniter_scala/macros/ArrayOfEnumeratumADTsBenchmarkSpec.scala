package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfEnumeratumADTsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfEnumeratumADTsBenchmark {
    setup()
  }
  
  "ArrayOfEnumeratumADTsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec hasn't integration with enumeratum
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: Jackson-module-scala hasn't integration with enumeratum
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      //FIXME: the latest version of UPickle hasn't integration with enumeratum
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec hasn't integration with enumeratum
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: Jackson-module-scala hasn't integration with enumeratum
      //toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      //FIXME: the latest version of UPickle hasn't integration with enumeratum
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}