package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.circe._
import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, ReaderConfig, WriterConfig}

object CirceJsoniterCodecs {
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2).withPreferredBufSize(32768)
  val tooLongStringConfig: ReaderConfig = ReaderConfig.withPreferredCharBufSize(1024 * 1024)
  implicit val jsonCodec: JsonValueCodec[Json] = JsoniterScalaCodec.jsonCodec(doSerialize = _ ne Json.Null)
}