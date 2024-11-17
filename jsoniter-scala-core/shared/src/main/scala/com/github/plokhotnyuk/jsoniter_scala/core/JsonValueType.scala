package com.github.plokhotnyuk.jsoniter_scala.core

/** A type of JSON value */
object JsonValueType extends Enumeration {
  type JsonValueType = Value
  val Array, Boolean, Null, Number, Object, String = Value
}
