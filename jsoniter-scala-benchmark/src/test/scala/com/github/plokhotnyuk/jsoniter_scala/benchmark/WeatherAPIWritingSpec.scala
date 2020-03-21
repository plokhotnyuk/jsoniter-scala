package com.github.plokhotnyuk.jsoniter_scala.benchmark

class WeatherAPIWritingSpec extends BenchmarkSpecBase {
  private val benchmark = new WeatherAPIWriting
  
  "WeatherAPIWriting" should {
    "write properly" in {
      toString(benchmark.circe()) shouldBe WeatherAPI.jsonString2
      toString(benchmark.jsoniterScala()) shouldBe WeatherAPI.jsonString1
      toString(benchmark.preallocatedBuf, 0, benchmark.jsoniterScalaPrealloc()) shouldBe WeatherAPI.jsonString1
    }
  }
}