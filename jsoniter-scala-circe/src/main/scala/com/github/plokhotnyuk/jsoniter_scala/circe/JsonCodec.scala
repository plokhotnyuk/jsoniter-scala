package com.github.plokhotnyuk.jsoniter_scala.circe

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec}
import io.circe._

object JsonCodec {
  implicit val codec: JsonValueCodec[Json] = jsonCodec()

  def jsonCodec(
      maxDepth: Int = 128,
      initialSize: Int = 8,
      doSerialize: Json => Boolean = _ => true,
      numberParser: JsonReader => Json = io.circe.JsonCodec.defaultNumberParser): JsonValueCodec[Json] =
    io.circe.JsonCodec.jsonCodec(maxDepth, initialSize, doSerialize, numberParser)
}
