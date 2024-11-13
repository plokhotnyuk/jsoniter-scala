package com.github.plokhotnyuk.jsoniter_scala.circe

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import io.circe._

object JsoniterScalaCodec {
  /**
   * The implicit instance of jsoniter-scala's value codec for circe's Json.
   *
   * Uses default parameters for `JsoniterScalaCodec.jsonCodec`.
   */
  implicit val jsonC3c: JsonValueCodec[Json] = jsonCodec()

  /**
   * Creates a JSON value codec that parses and serialize to/from circe's JSON AST.
   *
   * @param maxDepth the maximum depth for decoding
   * @param initialSize the initial size hint for object and array collections
   * @param doSerialize a predicate that determines whether a value should be serialized
   * @param numberParser a function that parses JSON numbers
   * @return The JSON codec
   */
  def jsonCodec(
      maxDepth: Int,
      initialSize: Int,
      doSerialize: Json => Boolean,
      numberParser: JsonReader => Json): JsonValueCodec[Json] =
    jsonCodec(maxDepth, initialSize, doSerialize, numberParser, io.circe.JsoniterScalaCodec.defaultNumberSerializer)

  /**
   * Creates a JSON value codec that parses and serialize to/from circe's JSON AST.
   *
   * @param maxDepth the maximum depth for decoding
   * @param initialSize the initial size hint for object and array collections
   * @param doSerialize a predicate that determines whether a value should be serialized
   * @param numberParser a function that parses JSON numbers
   * @param numberSerializer a routine that serializes JSON numbers
   * @return The JSON codec
   */
  def jsonCodec(
      maxDepth: Int = 128,
      initialSize: Int = 8,
      doSerialize: Json => Boolean = _ => true,
      numberParser: JsonReader => Json = io.circe.JsoniterScalaCodec.defaultNumberParser,
      numberSerializer: (JsonWriter, JsonNumber) => Unit = io.circe.JsoniterScalaCodec.defaultNumberSerializer): JsonValueCodec[Json] =
    new io.circe.JsoniterScalaCodec(maxDepth, initialSize, doSerialize, numberParser, numberSerializer)
}
