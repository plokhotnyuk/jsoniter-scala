package com.github.plokhotnyuk.jsoniter_scala.benchmark

class ArrayOfDoublesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfDoublesBenchmark {
    setup()
  }
  
  "ArrayOfDoublesBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonScala() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      //FIXME: Jsoniter Java cannot parse some numbers like 5.9823526 precisely
      //benchmark.readJsoniterJava() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      sameOrBetter(toString(benchmark.writeAVSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeBorerJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeCirce()), benchmark.jsonString)
      //FIXME: dsl-json serializes doubles in a plain representation
      //sameOrBetter(toString(benchmark.writeDslJsonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJacksonScala()), benchmark.jsonString)
      //FIXME: PreciseFloatSupport.enable() doesn't work sometime and Jsoniter Java serializes values rounded to 6 digits
      //sameOrBetter(toString(benchmark.writeJsoniterJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()), benchmark.jsonString)
      //FIXME: Play-JSON serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
      //sameOrBetter(toString(benchmark.writePlayJson()), benchmark.jsonString)
      //FIXME: Spray-JSON serializes doubles in different format than toString: 6.653409109328879E-5 as 0.00006653409109328879
      //sameOrBetter(toString(benchmark.writeSprayJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeUPickle()), benchmark.jsonString)
    }
  }


  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toDouble == e.toDouble && a.length <= e.length, s"expected the same or better: $e, but got: $a")
    }
}