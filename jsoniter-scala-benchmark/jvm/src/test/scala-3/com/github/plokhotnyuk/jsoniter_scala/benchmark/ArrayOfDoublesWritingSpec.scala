package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfDoublesWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfDoublesWriting = new ArrayOfDoublesWriting {
    setup()
  }

  "ArrayOfDoublesWriting" should {
    "write properly" in {
      val b = benchmark
      check(toString(b.borer()), b.jsonString)
      check(toString(b.circe()), b.jsonString)
      check(toString(b.circeJsoniter()), b.jsonString)
      check(toString(b.jacksonScala()), b.jsonString)
      check(toString(b.jsoniterScala()), b.jsonString)
      check(toString(b.json4sJackson()), b.jsonString)
      check(toString(b.json4sNative()), b.jsonString)
      check(toString(b.preallocatedBuf, 64, b.jsoniterScalaPrealloc()), b.jsonString)
      check(toString(b.smithy4sJson()), b.jsonString)
      check(toString(b.weePickle()), b.jsonString)
    }
  }

  private[this] def check(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toDouble == e.toDouble, s"expected: $e, but got: $a when parsed back to double")
    }
}