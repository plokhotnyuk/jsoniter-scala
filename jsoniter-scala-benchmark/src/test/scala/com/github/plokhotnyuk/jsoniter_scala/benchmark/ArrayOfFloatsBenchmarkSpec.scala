package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfFloatsBenchmarkSpec extends BenchmarkSpecBase {
  private val benchmark: ArrayOfFloatsBenchmark = new ArrayOfFloatsBenchmark {
    private val values: Array[String] = Array(
      "7.038531e-26",
      "1.199999988079071",
      "3.4028235677973366e38",
      "7.006492321624086e-46"
    )

    setup()
    jsonBytes = (1 to size).map(i => values(i % values.length)).mkString("[", ",", "]").getBytes(UTF_8)
    obj = (1 to size).map(i => values(i % values.length).toFloat).toArray
    jsonString = obj.mkString("[", ",", "]")
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
  
  "ArrayOfFloatsBenchmark" should {
    "deserialize properly" in {
      benchmark.readAVSystemGenCodec() shouldBe benchmark.obj
      //FIXME: Circe parses 7.038531e-26 as 7.0385313e-26
      //benchmark.readCirce() shouldBe benchmark.obj
      //FIXME: DSL-JSON parses 1.199999988079071 as 1.2f instead of 1.1999999f
      //benchmark.readDslJsonScala() shouldBe benchmark.obj
      //FIXME: Jackson parses 1.199999988079071 as 1.2f instead of 1.1999999f
      //benchmark.readJacksonScala() shouldBe benchmark.obj
      //FIXME: Jsoniter Java parses 1.199999988079071 as 1.2f instead of 1.1999999f
      //benchmark.readJsoniterJava() shouldBe benchmark.obj
      benchmark.readJsoniterScala() shouldBe benchmark.obj
      benchmark.readPlayJson() shouldBe benchmark.obj
      benchmark.readSprayJson() shouldBe benchmark.obj
      benchmark.readUPickle() shouldBe benchmark.obj
    }
    "serialize properly" in {
      sameOrBetter(toString(benchmark.writeAVSystemGenCodec()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeCirce()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeDslJsonScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJacksonScala()), benchmark.jsonString)
      //FIXME: PreciseFloatSupport.enable() doesn't work sometime and Jsoniter Java serializes values rounded to 6 digits
      //sameOrBetter(toString(benchmark.writeJsoniterJava()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeJsoniterScala()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.preallocatedBuf, 0, benchmark.writeJsoniterScalaPrealloc()), benchmark.jsonString)
      //FIXME: Play-JSON serializes double values instead of float
      //sameOrBetter(toString(benchmark.writePlayJson()), benchmark.jsonString)
      //FIXME: Spray-JSON serializes double values instead of float
      //sameOrBetter(toString(benchmark.writeSprayJson()), benchmark.jsonString)
      sameOrBetter(toString(benchmark.writeUPickle()), benchmark.jsonString)
    }
  }

  private[this] def sameOrBetter(actual: String, expected: String): Unit =
    actual.substring(1, actual.length - 1).split(',')
      .zip(expected.substring(1, expected.length - 1).split(',')).foreach { case (a, e) =>
      require(a.toFloat == e.toFloat && a.length <= e.length,
        s"expected the same or better: $e, but got: $a")
    }
}