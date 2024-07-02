package upickle.jsoniter

import ujson.Value

object ValueEncoder extends VisitorEncoder[Value](ujson.Value) {
  override def nullValue: Value = ujson.Null
}
