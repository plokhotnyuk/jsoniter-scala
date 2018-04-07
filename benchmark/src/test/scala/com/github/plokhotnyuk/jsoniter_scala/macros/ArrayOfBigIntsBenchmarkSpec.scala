package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfBigIntsBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new ArrayOfBigIntsBenchmark
  
  "ArrayOfBigIntsBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce().deep shouldBe benchmark.obj.deep
      //FIXME: dsl-json throws Error parsing number at position: 33. Integer overflow detected
      //benchmark.readDslJsonJava().deep shouldBe benchmark.obj.deep
      benchmark.readJacksonScala().deep shouldBe benchmark.obj.deep
      benchmark.readJsoniterScala().deep shouldBe benchmark.obj.deep
      benchmark.readPlayJson().deep shouldBe benchmark.obj.deep
    }
    "serialize properly" in {
      //FIXME: Circe uses an engineering decimal notation to serialize BigInt
      //toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json cannot find serializer for
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      //val writer = benchmark.writeDslJsonJavaPrealloc()
      //toString(writer.getByteBuffer, writer.size()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
      //toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}