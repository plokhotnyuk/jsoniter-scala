package com.github.plokhotnyuk.jsoniter_scala.macros

class AnyRefsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new AnyRefsBenchmark
  
  "AnyRefsBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec store option field as JSON array
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      // FIXME: cannot alter uPickle to store BigDecimal as JSON number
      //benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec store option field as JSON array
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      // FIXME: cannot alter uPickle to store BigDecimal as JSON number
      //toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}