package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import scala.collection.immutable.IndexedSeq
import scala.reflect.io.Streamable

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

object GoogleMapsAPI {
  //Distance Matrix API call for top-10 by population cities in US:
  //https://maps.googleapis.com/maps/api/distancematrix/json?origins=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose&destinations=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose
  var jsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_response.json"))
  var jsonString: String = new String(jsonBytes, UTF_8)
  var compactJsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_compact_response.json"))
  var compactJsonString: String = new String(compactJsonBytes, UTF_8)
}