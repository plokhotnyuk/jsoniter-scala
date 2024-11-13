package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._

/**
 * An extension of `JsonValueCodec[A]` that could be used to derive codecs using the `... derives ...` syntax.
 *
 * A compile-time configuration can be provided by an `inline given CodecMakerConfig` value, which needs to be visible
 * in the scope, and defined/imported before the `... derives ...` syntax usage.
 *
 * BEWARE: Using of this class requires _runtime_ scope for the `jsoniter-scala-macros` dependency.
 *
 * Also, to avoid overhead in runtime use it only for top-level and big commonly used data structures.
 */
trait ConfiguredJsonValueCodec[A] extends JsonValueCodec[A]

object ConfiguredJsonValueCodec:
  inline def derived[A](using inline config: CodecMakerConfig = CodecMakerConfig): ConfiguredJsonValueCodec[A] =
    new ConfiguredJsonValueCodecWrapper(JsonCodecMaker.make[A](config))

private[macros] class ConfiguredJsonValueCodecWrapper[A](impl: JsonValueCodec[A]) extends ConfiguredJsonValueCodec[A] {
  @inline
  def decodeValue(in: JsonReader, default: A): A = impl.decodeValue(in, default)

  @inline
  def encodeValue(x: A, out: JsonWriter): Unit = impl.encodeValue(x, out)

  @inline
  def nullValue: A = impl.nullValue
}