package com.github.plokhotnyuk.jsoniter_scala.macros

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
