package com.github.plokhotnyuk.jsoniter_scala.benchmark

class NestedStructsWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new NestedStructsWriting {
    setup()
  }
  
  "NestedStructsWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: Borer throws io.bullet.borer.Borer$Error$Unsupported: The JSON renderer doesn't support more than 64 JSON Array/Object nesting levels
      //toString(benchmark.borerJson()) shouldBe benchmark.jsonString
      //FIXME: DSL-JSON serializes null value for Option.None
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}