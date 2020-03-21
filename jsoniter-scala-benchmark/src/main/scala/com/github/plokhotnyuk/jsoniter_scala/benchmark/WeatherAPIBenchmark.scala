package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeatherAPI._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.immutable.IndexedSeq
import scala.reflect.io.Streamable

object WeatherAPI {
  case class Context(
    wx: String,
    geo: String,
    unit: String,
    `@vocab`: String)

  case class ForecastProperties(
    updated: String,
    units: String,
    forecastGenerator: String,
    generatedAt: String,
    updateTime: String,
    validTimes: String,
    elevation: Elevation,
    periods: IndexedSeq[Periods])

  case class Elevation(
    value: Double,
    unitCode: String)

  case class Periods(
    number: Double,
    name: String,
    startTime: String,
    endTime: String,
    isDaytime: Boolean,
    temperature: Double,
    temperatureUnit: String,
    temperatureTrend: Option[String] = None,
    windSpeed: String,
    windDirection: String,
    icon: String,
    shortForecast: String,
    detailedForecast: String)

  sealed trait Geometry extends Product with Serializable

  sealed trait SimpleGeometry extends Geometry

  case class Point(coordinates: (Double, Double)) extends SimpleGeometry

  case class MultiPoint(coordinates: IndexedSeq[(Double, Double)]) extends SimpleGeometry

  case class LineString(coordinates: IndexedSeq[(Double, Double)]) extends SimpleGeometry

  case class MultiLineString(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends SimpleGeometry

  case class Polygon(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends SimpleGeometry

  case class MultiPolygon(coordinates: IndexedSeq[IndexedSeq[IndexedSeq[(Double, Double)]]]) extends SimpleGeometry

  case class GeometryCollection(geometries: IndexedSeq[SimpleGeometry]) extends Geometry

  case class Forecast(
    `@context`: (String, Context),
    `type`: String = "Feature",
    geometry: Geometry,
    properties: ForecastProperties)

  //Forecast response for the following request at 2020-03-21T06:09:01+00:00: https://api.weather.gov/gridpoints/FFC/52,67/forecast
  var jsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("weather_api_response.json"))
  var jsonString: String = new String(jsonBytes, UTF_8)
  var jsonString1: String = new String(Streamable.bytes(getClass.getResourceAsStream("weather_api_compact_response-1.json")), UTF_8)
  var jsonString2: String = new String(Streamable.bytes(getClass.getResourceAsStream("weather_api_compact_response-2.json")), UTF_8)
}

abstract class WeatherAPIBenchmark extends CommonParams {
  var obj: Forecast = readFromArray[Forecast](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
}