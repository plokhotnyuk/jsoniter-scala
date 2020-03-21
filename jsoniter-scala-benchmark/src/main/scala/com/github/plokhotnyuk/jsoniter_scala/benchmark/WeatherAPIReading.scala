package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeatherAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark

class WeatherAPIReading extends WeatherAPIBenchmark {
  @Benchmark
  def circe(): WeatherAPI.Forecast = decode[WeatherAPI.Forecast](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jsoniterScala(): WeatherAPI.Forecast = readFromArray[WeatherAPI.Forecast](jsonBytes)
}