package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfDoublesWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfDoublesWriting {
    setup()
  }
  
  "ArrayOfDoublesWriting" should {
    "write properly" in {
      sameOrBetter(toString(benchmark.avSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.borerJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.circe()), benchmark.jsonString)
      //FIXME: DSL-JSON serializes doubles in a plain representation
      //sameOrBetter(toString(benchmark.dslJsonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.jacksonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.jsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()), benchmark.jsonString)
      //FIXME: Play-JSON serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
      //sameOrBetter(toString(benchmark.playJson()), benchmark.jsonString)
      //FIXME: Spray-JSON serializes doubles in different format than toString: 6.653409109328879E-5 as 0.00006653409109328879
      //sameOrBetter(toString(benchmark.sprayJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.uPickle()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.weePickle()), benchmark.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
        require(a.toDouble == e.toDouble && a.length <= e.length, s"expected the same or better: $e, but got: $a")
      }
}