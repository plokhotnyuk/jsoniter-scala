package com.github.plokhotnyuk.jsoniter_scala.macros

class PrimitivesBenchmarkSpec extends BenchmarkSpecBase {
  val benchmark = new PrimitivesBenchmark
  
  "PrimitivesBenchmark" should {
    "deserialize properly" in {
      benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: dsl-json cannot find decoder for class com.github.plokhotnyuk.jsoniter_scala.macros.Primitives
      //benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
    }
    "serialize properly" in {
      toString(benchmark.writeCirce()) shouldBe benchmark.jsonString
      //FIXME: dsl-json throws com.dslplatform.json.SerializationException: Unable to find writer for char
      //toString(benchmark.writeDslJsonJava()) shouldBe benchmark.jsonString
      //val writer = benchmark.writeDslJsonJavaPrealloc()
      //toString(writer.getByteBuffer, writer.size()) shouldBe benchmark.jsonString
      toString(benchmark.writeJacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.writeJsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, benchmark.writeJsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.writePlayJson()) shouldBe benchmark.jsonString
    }
  }
}