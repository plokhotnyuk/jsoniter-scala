package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfFloatsReadingSpec extends BenchmarkSpecBase {
  private val benchmark: ArrayOfFloatsReading = new ArrayOfFloatsReading {
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
  
  "ArrayOfFloatsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
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
  }
}