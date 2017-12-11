package com.github.plokhotnyuk.jsoniter_scala.macros

import scala.reflect.io.Streamable

case class Value(
  text: String,
  value: Int)

case class Elements(
  distance: Value,
  duration: Value,
  status: String)

case class DistanceMatrix(
  destination_addresses: Seq[String],
  origin_addresses: Seq[String],
  rows: Seq[Rows],
  status: String)

case class Rows(elements: Seq[Elements])

object GoogleMapsAPI {
  val json: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_response.json"))
  val compactJson: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_compact_response.json"))
}