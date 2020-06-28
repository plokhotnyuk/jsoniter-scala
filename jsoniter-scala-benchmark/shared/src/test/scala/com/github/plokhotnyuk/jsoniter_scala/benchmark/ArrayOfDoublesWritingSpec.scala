package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfDoublesWritingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfDoublesWriting = new ArrayOfDoublesWriting {
    setup()
  }
  
  "ArrayOfDoublesWriting" should {
    "write properly" in {
      val b = benchmark
      sameOrBetter(toString(b.avSystemGenCodec()), b.jsonString)
      sameOrBetter(toString(b.borer()), b.jsonString)
      sameOrBetter(toString(b.circe()), b.jsonString)
      //FIXME: DSL-JSON serializes doubles in a plain representation
      //sameOrBetter(toString(b.dslJsonScala()), b.jsonString)
      sameOrBetter(toString(b.jacksonScala()), b.jsonString)
      sameOrBetter(toString(b.jsoniterScala()), b.jsonString)
      sameOrBetter(toString(b.preallocatedBuf, 0, b.jsoniterScalaPrealloc()), b.jsonString)
      //FIXME: Play-JSON serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
      //sameOrBetter(toString(b.playJson()), b.jsonString)
      //FIXME: Spray-JSON serializes doubles in different format than toString: 6.653409109328879E-5 as 0.00006653409109328879
      //sameOrBetter(toString(b.sprayJson()), b.jsonString)
      sameOrBetter(toString(b.uPickle()), b.jsonString)
      sameOrBetter(toString(b.weePickle()), b.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
        require(a.toDouble == e.toDouble && a.length <= e.length, s"expected the same or better: $e, but got: $a")
      }
}