package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.immutable.IndexedSeq
import scala.reflect.io.Streamable

object GoogleMapsAPI {
  case class Value(
    text: String,
    value: Int)

  case class Elements(
    distance: Value,
    duration: Value,
    status: String)

  case class DistanceMatrix(
    destination_addresses: IndexedSeq[String],
    origin_addresses: IndexedSeq[String],
    rows: IndexedSeq[Rows],
    status: String)

  case class Rows(elements: IndexedSeq[Elements])

  //Distance Matrix API call for top-10 by population cities in US:
  //https://maps.googleapis.com/maps/api/distancematrix/json?origins=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose&destinations=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose
  var jsonBytes1: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_response-1.json"))
  var jsonString1: String = new String(jsonBytes1, UTF_8)
  var jsonBytes2: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_response-2.json"))
  var jsonString2: String = new String(jsonBytes2, UTF_8)
  var compactJsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_compact_response.json"))
  var compactJsonString: String = new String(compactJsonBytes, UTF_8)
}

abstract class GoogleMapsAPIBenchmark extends CommonParams {
  var obj: DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes1)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes1.length + 100/*to avoid possible out of bounds error*/)
}