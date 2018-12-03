package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfFloatsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfFloatsBenchmark {
    setup()
  }
  
  "ArrayOfFloatsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterJava() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      //FIXME: AVSystem GenCodec serializes double values instead of float
      //sameOrBetter(toString(benchmark.writeAVSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeCirce()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeDslJsonJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJacksonScala()), benchmark.jsonString)
      //FIXME: PreciseFloatSupport.enable() doesn't work sometime and Jsoniter Java serializes values rounded to 6 digits
      //sameOrBetter(toString(benchmark.writeJsoniterJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()),
        benchmark.jsonString)
      //FIXME: Play-JSON serializes double values instead of float
      //sameOrBetter(toString(benchmark.writePlayJson()), benchmark.jsonString)
      //FIXME: uPickle serializes double values instead of float
      //sameOrBetter(toString(benchmark.writeUPickle()), benchmark.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat && a.length <= e.length,
        s"expected the same or better: $e, but got: $a")
    }
}