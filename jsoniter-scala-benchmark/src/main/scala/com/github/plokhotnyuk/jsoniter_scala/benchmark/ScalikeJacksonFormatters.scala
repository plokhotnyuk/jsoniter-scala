package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI.DistanceMatrix
import reug.scalikejackson.ScalaJacksonFormatter
import reug.scalikejackson.play.Json

import scala.collection.immutable.Map
import scala.collection.immutable.Seq
import scala.collection.immutable.Set
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
  implicit val arrayOfUUIDsFormatter: ScalaJacksonFormatter[Array[UUID]] = Json.format()
  implicit val bigDecimalFormatter: ScalaJacksonFormatter[BigDecimal] = Json.format()
  implicit val bigIntFormatter: ScalaJacksonFormatter[BigInt] = Json.format()
  implicit val distanceMatrixFormatter: ScalaJacksonFormatter[DistanceMatrix] = Json.format()
  implicit val extractFieldsFormatter: ScalaJacksonFormatter[ExtractFields] = Json.format()
  implicit val listOfBooleansFormatter: ScalaJacksonFormatter[List[Boolean]] = Json.format()
  implicit val mapOfIntsToBooleansFormatter: ScalaJacksonFormatter[Map[Int, Boolean]] = Json.format()
  implicit val mutableMapOfIntsToBooleansFormatter: ScalaJacksonFormatter[mutable.Map[Int, Boolean]] = Json.format()
  implicit val mutableSetOfIntsFormatter: ScalaJacksonFormatter[mutable.Set[Int]] = Json.format()
  implicit val missingRequiredFieldsFormatter: ScalaJacksonFormatter[MissingRequiredFields] = Json.format()
  implicit val nestedStructsFormatter: ScalaJacksonFormatter[NestedStructs] = Json.format()
  implicit val primitivesFormatter: ScalaJacksonFormatter[Primitives] = Json.format()
  implicit val seqOfTweetsFormatter: ScalaJacksonFormatter[Seq[TwitterAPI.Tweet]] = Json.format()
  implicit val setOfIntsFormatter: ScalaJacksonFormatter[Set[Int]] = Json.format()
  implicit val vectorOfBooleansFormatter: ScalaJacksonFormatter[Vector[Boolean]] = Json.format()
}