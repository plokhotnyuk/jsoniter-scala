package com.github.plokhotnyuk.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.JsonCodecMaker.make
import play.api.libs.json.{Json, OFormat}

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
    destination_addresses: Seq[String],
    origin_addresses: Seq[String],
    rows: Seq[Rows],
    status: String)

  case class Rows(elements: Seq[Elements])

  val format: OFormat[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: OFormat[Value] = Json.format[Value]
    implicit val v2: OFormat[Elements] = Json.format[Elements]
    implicit val v3: OFormat[Rows] = Json.format[Rows]
    Json.format[DistanceMatrix]
  }
  val codec: JsonCodec[DistanceMatrix] = make[DistanceMatrix](CodecMakerConfig())
  //Distance Matrix API call for top-10 by population cities in US:
  //https://maps.googleapis.com/maps/api/distancematrix/json?origins=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose&destinations=New+York|Los+Angeles|Chicago|Houston|Phoenix+AZ|Philadelphia|San+Antonio|San+Diego|Dallas|San+Jose
  val json: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_response.json"))
  val compactJson: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("google_maps_api_compact_response.json"))
  val obj: DistanceMatrix = JsonReader.read(codec, json)
}