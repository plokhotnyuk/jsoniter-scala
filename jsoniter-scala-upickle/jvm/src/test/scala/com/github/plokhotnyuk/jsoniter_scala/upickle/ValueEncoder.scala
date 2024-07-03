package com.github.plokhotnyuk.jsoniter_scala.upickle

import ujson.Value

object ValueEncoder extends VisitorEncoder[Value](ujson.Value) {
  override def nullValue: Value = ujson.Null
}
