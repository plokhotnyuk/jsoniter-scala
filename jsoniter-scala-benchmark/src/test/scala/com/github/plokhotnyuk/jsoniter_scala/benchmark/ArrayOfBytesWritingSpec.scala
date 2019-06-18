package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfBytesWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfBytesWriting {
    setup()
  }
  
  "ArrayOfBytesWriting" should {
    "write properly" in {
      toString(benchmark.avSystemGenCodec()) shouldBe benchmark.jsonString
      //FIXME: Borer throws io.bullet.borer.Borer$Error$Unsupported: The JSON renderer doesn't support byte strings (Output.ToByteArray index 0)
      //toString(benchmark.borerJson()) shouldBe benchmark.jsonString
      toString(benchmark.circe()) shouldBe benchmark.jsonString
      //FIXME: dsl-json serializes a byte array to the base64 string
      //toString(benchmark.dslJsonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jacksonScala()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterJava()) shouldBe benchmark.jsonString
      toString(benchmark.jsoniterScala()) shouldBe benchmark.jsonString
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe benchmark.jsonString
      toString(benchmark.playJson()) shouldBe benchmark.jsonString
      //FIXME: ScalikeJackson serializes a byte array to the base64 string
      //toString(benchmark.scalikeJackson()) shouldBe benchmark.jsonString
      toString(benchmark.sprayJson()) shouldBe benchmark.jsonString
      toString(benchmark.uPickle()) shouldBe benchmark.jsonString
    }
  }
}