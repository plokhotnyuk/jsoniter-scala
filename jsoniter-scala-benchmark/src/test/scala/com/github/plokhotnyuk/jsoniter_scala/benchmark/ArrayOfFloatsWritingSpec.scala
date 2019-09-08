package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfFloatsWritingSpec extends BenchmarkSpecBase {
  private val benchmark: ArrayOfFloatsWriting = new ArrayOfFloatsWriting {
    setup()
  }
  
  "ArrayOfFloatsWriting" should {
    "write properly" in {
      sameOrBetter(toString(benchmark.avSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.borerJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.circe()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.dslJsonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.jacksonScala()), benchmark.jsonString)
      //FIXME: Jsoniter Java serializes values rounded to 6 digits because PreciseFloatSupport.enable() doesn't work sometime
      //sameOrBetter(toString(benchmark.jsoniterJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.jsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()), benchmark.jsonString)
      //FIXME: Play-JSON serializes double values instead of float
      //sameOrBetter(toString(benchmark.playJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.scalikeJackson()), benchmark.jsonString)
      //FIXME: Spray-JSON serializes double values instead of float
      //sameOrBetter(toString(benchmark.sprayJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.uPickle()), benchmark.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat && a.length <= e.length,
        s"expected the same or better: $e, but got: $a")
    }
}