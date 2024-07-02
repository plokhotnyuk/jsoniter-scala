package com.github.plokhotnyuk.jsoniter_scala.upickle

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import upickle.core.Visitor
import upickle.jsoniter.{VisitorDecoder, VisitorNumberReader}

object JsoniterScalaCodec {

  /**
   * Creates a JSON value decoder that parses and composes a given type through a Visitor.
   *
   * @param maxDepth the maximum depth for decoding
   * @param numReader the number reader to use
   * @param visitor the visitor to compose the given type
   * @return JSON codec that supports decoding only
   */
  def visitorDecoder[J](maxDepth: Int = 32,
                        numReader: VisitorNumberReader = VisitorNumberReader.Default,
                        visitor: Visitor[?, J]): JsonValueCodec[J] =
    new VisitorDecoder(maxDepth, numReader, visitor)
}
