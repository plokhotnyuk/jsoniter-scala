package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfFloatsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfFloatsWriting = new ArrayOfFloatsWriting {
    setup()
  }
  
  "ArrayOfFloatsWriting" should {
    "write properly" in {
      val b = benchmark
      check(toString(b.avSystemGenCodec()), b.jsonString)
      check(toString(b.borer()), b.jsonString)
      check(toString(b.circe()), b.jsonString)
      check(toString(b.dslJsonScala()), b.jsonString)
      check(toString(b.jacksonScala()), b.jsonString)
      check(toString(b.jsoniterScala()), b.jsonString)
      check(toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()), b.jsonString)
      check(toString(b.playJson()), b.jsonString)
      check(toString(b.playJsonJsoniter()), b.jsonString)
      check(toString(b.sprayJson()), b.jsonString)
      check(toString(b.uPickle()), b.jsonString)
      check(toString(b.weePickle()), b.jsonString)
    }
  }

  private[this] def check(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat, s"expected: $e, but got: $a when parsed back to float")
    }
}