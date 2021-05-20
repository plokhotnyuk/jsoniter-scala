package com.github.plokhotnyuk.jsoniter_scala.benchmark

class GeoJSONReadingSpec extends BenchmarkSpecBase {
  def benchmark = new GeoJSONReading
  
  "GeoJSONReading" should {
    "read properly" in {
      benchmark.avSystemGenCodec() shouldBe benchmark.obj
      benchmark.borer() shouldBe benchmark.obj
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jacksonScala() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
      benchmark.playJson() shouldBe benchmark.obj
      benchmark.playJsonJsoniter() shouldBe benchmark.obj
      benchmark.sprayJson() shouldBe benchmark.obj
      benchmark.uPickle() shouldBe benchmark.obj
      benchmark.weePickle() shouldBe benchmark.obj
      // FIXME: zio-json ignores @jsonDiscriminator("type") annotation
      //benchmark.zioJson() shouldBe benchmark.obj
    }
    "fail on invalid input" in {
      val b = benchmark
      b.jsonBytes(42) = '{'.toByte
      b.jsonBytes(43) = '}'.toByte
      b.jsonBytes(44) = ','.toByte
      b.jsonBytes(45) = '['.toByte
      intercept[Throwable](b.avSystemGenCodec())
      intercept[Throwable](b.borer())
      intercept[Throwable](b.circe())
      intercept[Throwable](b.jacksonScala())
      intercept[Throwable](b.jsoniterScala())
      intercept[Throwable](b.playJson())
      intercept[Throwable](b.playJsonJsoniter())
      intercept[Throwable](b.sprayJson())
      intercept[Throwable](b.uPickle())
      intercept[Throwable](b.weePickle())
      // FIXME: zio-json ignores @jsonDiscriminator("type") annotation
      //intercept[Throwable](b.zioJson())
    }
  }
}