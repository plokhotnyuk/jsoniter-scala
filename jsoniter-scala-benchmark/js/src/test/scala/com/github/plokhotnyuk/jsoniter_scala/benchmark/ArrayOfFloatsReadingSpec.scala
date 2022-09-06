package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

class ArrayOfFloatsReadingSpec extends BenchmarkSpecBase {
  def benchmark: ArrayOfFloatsReading = new ArrayOfFloatsReading {
    private val values: Array[String] = Array(
      "7.038531e-26",
      "1.199999988079071",
      "3.4028235677973366e38",
      "7.006492321624086e-46"
    )

    setup()
    jsonString = (obj.map(_.toString) ++ values).mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    obj = obj ++ values.map(_.toFloat)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100 /*to avoid possible out of bounds error*/)
  }

  "ArrayOfFloatsReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      //FIXME circe parses 1.1999999284744263 as 1.2000000476837158
      //benchmark.circe() shouldBe benchmark.obj
      benchmark.circeJawn() shouldBe benchmark.obj
      //FIXME jsoniter-scala-circe parses 1.1999999284744263 as 1.2000000476837158
      //benchmark.circeJsoniter() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      //FIXME play-json parses 1.1999999284744263 as 1.2000000476837158
      //benchmark.playJson() shouldBe benchmark.obj
      //FIXME play-json-jsoniter parses 1.1999999284744263 as 1.2000000476837158
      //benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.smithy4sJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      //FIXME zio-json parses 1.1999999284744263 as 1.2000000476837158
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(0) = 'x'.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.circeJawn())
      intercept[Throwable](b.circeJsoniter())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.smithy4sJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.zioJson())
    }
  }
}