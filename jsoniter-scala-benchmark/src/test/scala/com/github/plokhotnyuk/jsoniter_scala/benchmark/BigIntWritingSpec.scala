package com.github.plokhotnyuk.jsoniter_scala.benchmark

class BigIntWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new BigIntWriting {
    setup()
  }
  
  "BigIntWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      toString(benchmark.borerJson()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      //FIXME: Play-JSON uses BigDecimal with engineering decimal representation to serialize numbers
      //toString(benchmark.playJson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
      //FIXME: weePickle serializes BigInt values as JSON strings
      //toString(benchmark.weePickle()) shouldBe benchmark.jsonString
    }
  }
}