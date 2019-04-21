package com.github.plokhotnyuk.jsoniter_scala.benchmark

class StringOfNonAsciiCharsReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new StringOfNonAsciiCharsReading {
    setup()
  }
  
  "StringOfNonAsciiCharsReading" should {
    "read properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readBorerJson() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterJava() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
  }
}