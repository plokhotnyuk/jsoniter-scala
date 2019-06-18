package com.github.plokhotnyuk.jsoniter_scala.benchmark

import reug.scalikejackson.ScalaJacksonFormatter
import reug.scalikejackson.play.Json

import scala.collection.mutable

object ScalikeJacksonFormatters {
  implicit val anyRefsFormatter: ScalaJacksonFormatter[AnyRefs] = Json.format()
  implicit val anyValsFormatter: ScalaJacksonFormatter[AnyVals] = Json.format()
  implicit val arrayBufferOfBooleansFormatter: ScalaJacksonFormatter[mutable.ArrayBuffer[Boolean]] = Json.format()
  implicit val arrayOfBigDecimalsFormatter: ScalaJacksonFormatter[Array[BigDecimal]] = Json.format()
  implicit val arrayOfBigIntsFormatter: ScalaJacksonFormatter[Array[BigInt]] = Json.format()
  implicit val arrayOfBooleansFormatter: ScalaJacksonFormatter[Array[Boolean]] = Json.format()
  implicit val arrayOfBytesFormatter: ScalaJacksonFormatter[Array[Byte]] = Json.format()
  implicit val arrayOfCharsFormatter: ScalaJacksonFormatter[Array[Char]] = Json.format()
  implicit val arrayOfDoublesFormatter: ScalaJacksonFormatter[Array[Double]] = Json.format()
  implicit val arrayOfFloatsFormatter: ScalaJacksonFormatter[Array[Float]] = Json.format()
  implicit val arrayOfIntsFormatter: ScalaJacksonFormatter[Array[Int]] = Json.format()
  implicit val arrayOfLongsFormatter: ScalaJacksonFormatter[Array[Long]] = Json.format()
  implicit val arrayOfShortsFormatter: ScalaJacksonFormatter[Array[Short]] = Json.format()
  implicit val primitivesFormatter: ScalaJacksonFormatter[Primitives] = Json.format()
}