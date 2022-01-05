package com.github.plokhotnyuk.jsoniter_scala.macros

object NamespacePollutions {
  // Intentionally pollute the term namespace for testing of macro quasi-quotes
  lazy val java, scala, collection, mutable, immutable, util, switch, System, Array, None, Nil,
  JsonReader, JsonWriter, JsonValueCodec, JsonKeyCodec, JsonCodec =
    sys.error("Non fully-qualified term name is detected in quasi-quote(s)")

  // Intentionally pollute the type namespace for testing of macro quasi-quotes
  type Boolean = Nothing
  type Byte = Nothing
  type Short = Nothing
  type Unit = Nothing
}
