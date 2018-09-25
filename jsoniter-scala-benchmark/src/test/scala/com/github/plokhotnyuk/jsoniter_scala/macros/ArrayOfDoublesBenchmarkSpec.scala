package com.github.plokhotnyuk.jsoniter_scala.macros

class ArrayOfDoublesBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark = new ArrayOfDoublesBenchmark {
    setup()
  }
  
  "ArrayOfDoublesBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      benchmark.readCirce() shouldBe benchmark.obj
      benchmark.readDslJsonJava() shouldBe benchmark.obj
      benchmark.readJacksonScala() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      sameOrBetter(toString(benchmark.writeAVSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeCirce()), benchmark.jsonString)
      //FIXME: dsl-json serializes doubles in a plain representation
      //sameOrBetter(toString(benchmark.writeDslJsonJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJacksonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()), benchmark.jsonString)
      //FIXME: Play serializes doubles in different format than toString: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
      //sameOrBetter(toString(benchmark.writePlayJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeUPickle()), benchmark.jsonString)
    }
  }


  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toDouble == e.toDouble && a.length <= e.length,
        s"expected the same or better: $e, but got: $a")
    }
}