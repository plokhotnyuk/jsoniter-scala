package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfFloatsWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfFloatsWriting = new ArrayOfFloatsWriting {
    setup()
  }
  
  "ArrayOfFloatsWriting" should {
    "write properly" in {
      val b = benchmark
      sameOrBetter(toString(b.avSystemGenCodec()), b.jsonString)
      sameOrBetter(toString(b.borer()), b.jsonString)
      sameOrBetter(toString(b.circe()), b.jsonString)
      sameOrBetter(toString(b.dslJsonScala()), b.jsonString)
      sameOrBetter(toString(b.jacksonScala()), b.jsonString)
      sameOrBetter(toString(b.jsoniterScala()), b.jsonString)
      sameOrBetter(toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()), b.jsonString)
      //FIXME: Play-JSON serializes double values instead of float
      //sameOrBetter(toString(b.playJson()), b.jsonString)
      //FIXME: Spray-JSON serializes double values instead of float
      //sameOrBetter(toString(b.sprayJson()), b.jsonString)
      sameOrBetter(toString(b.uPickle()), b.jsonString)
      sameOrBetter(toString(b.weePickle()), b.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat && a.length <= e.length,
        s"expected the same or better: $e, but got: $a")
    }
}