package com.github.plokhotnyuk.jsoniter_scala.benchmark

class WeatherAPIReadingSpec extends BenchmarkSpecBase {
  private val benchmark = new WeatherAPIReading
  
  "WeatherAPIReading" should {
    "read properly" in {
      benchmark.circe() shouldBe benchmark.obj
      benchmark.jsoniterScala() shouldBe benchmark.obj
    }
  }
}