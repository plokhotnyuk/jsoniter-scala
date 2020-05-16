package com.github.plokhotnyuk.jsoniter_scala.benchmark

import scala.collection.immutable.IndexedSeq

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
}