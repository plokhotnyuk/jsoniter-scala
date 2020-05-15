package com.github.plokhotnyuk.jsoniter_scala.benchmark

case class MissingRequiredFields(
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) s: String,
    @com.fasterxml.jackson.annotation.JsonProperty(required = true) i: Int)