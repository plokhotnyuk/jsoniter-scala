package com.github.plokhotnyuk.jsoniter_scala.circe

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec}
import io.circe._

object JsoniterScalaCodec {
  implicit val jsonC3c: JsonValueCodec[Json] = jsonCodec()

  def jsonCodec(
      maxDepth: Int = 128,
      initialSize: Int = 8,
      doSerialize: Json => Boolean = _ => true,
      numberParser: JsonReader => Json = io.circe.JsoniterScalaCodec.defaultNumberParser): JsonValueCodec[Json] =
    io.circe.JsoniterScalaCodec.jsonCodec(maxDepth, initialSize, doSerialize, numberParser)
}
