package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBytesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBytesBenchmark {
    setup()
  }
  
  "ArrayOfBytesBenchmark" should {
    "deserialize properly" in {
      //FIXME: AVSystem GenCodec expects a string of hexadecimal representation of bytes
      //benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      //FIXME: dsl-json expects a base64 string for the byte array
      //benchmark.readDslJsonJava().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
      benchmark.readUPickle().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes a byte array to a string of hexadecimal representation of bytes
      //toString(benchmark.writeAVSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json serializes a byte array to the base64 string
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.preallocatedOff, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
      toString(benchmark.writeUPickle()) shouldBe benchmark.jsonString
    }
  }
}