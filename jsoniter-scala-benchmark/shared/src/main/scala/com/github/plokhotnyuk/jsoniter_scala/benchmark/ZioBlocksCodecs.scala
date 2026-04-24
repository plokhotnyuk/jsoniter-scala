/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.blocks.schema.json._
import zio.blocks.schema._
import zio.blocks.typeid._
import java.math.MathContext
import java.time._
import java.util.UUID
import scala.collection.immutable.ArraySeq
import scala.util.control.NonFatal

object ZioBlocksCodecs {
  val prettyConfig: WriterConfig = WriterConfig.withIndentionStep(2)
  val escapingConfig: WriterConfig = WriterConfig.withEscapeUnicode(true)
  val arrayOfBigDecimalsCodec: JsonCodec[Array[BigDecimal]] = Schema.derived.jsonCodec
  val arrayOfBigIntsCodec: JsonCodec[Array[BigInt]] = Schema.derived.jsonCodec
  val arrayOfBooleansCodec: JsonCodec[Array[Boolean]] = Schema.derived.jsonCodec
  val arrayOfBytesCodec: JsonCodec[Array[Byte]] = Schema.derived.jsonCodec
  val arrayOfCharsCodec: JsonCodec[Array[Char]] = Schema.derived.jsonCodec
  val arrayOfDoublesCodec: JsonCodec[Array[Double]] = Schema.derived.jsonCodec
  val arrayOfDurationsCodec: JsonCodec[Array[Duration]] = Schema.derived.jsonCodec
  val arrayOfEnumADTsCodec: JsonCodec[Array[SuitADT]] = Schema.derived.jsonCodec
  val arrayOfFloatsCodec: JsonCodec[Array[Float]] = Schema.derived.jsonCodec
  val arrayOfInstantsCodec: JsonCodec[Array[Instant]] = Schema.derived.jsonCodec
  val arrayOfIntsCodec: JsonCodec[Array[Int]] = Schema.derived.jsonCodec
  val arrayOfLocalDatesCodec: JsonCodec[Array[LocalDate]] = Schema.derived.jsonCodec
  val arrayOfLocalDateTimesCodec: JsonCodec[Array[LocalDateTime]] = Schema.derived.jsonCodec
  val arrayOfLocalTimesCodec: JsonCodec[Array[LocalTime]] = Schema.derived.jsonCodec
  val arrayOfLongsCodec: JsonCodec[Array[Long]] = Schema.derived.jsonCodec
  val arrayOfMonthDaysCodec: JsonCodec[Array[MonthDay]] = Schema.derived.jsonCodec
  val arrayOfOffsetDateTimesCodec: JsonCodec[Array[OffsetDateTime]] = Schema.derived.jsonCodec
  val arrayOfOffsetTimesCodec: JsonCodec[Array[OffsetTime]] = Schema.derived.jsonCodec
  val arrayOfPeriodsCodec: JsonCodec[Array[Period]] = Schema.derived.jsonCodec
  val arrayOfShortsCodec: JsonCodec[Array[Short]] = Schema.derived.jsonCodec
  val arrayOfUUIDsCodec: JsonCodec[Array[UUID]] = Schema.derived.jsonCodec
  val arrayOfYearMonthsCodec: JsonCodec[Array[YearMonth]] = Schema.derived.jsonCodec
  val arrayOfYearsCodec: JsonCodec[Array[Year]] = Schema.derived.jsonCodec
  val arrayOfZonedDateTimesCodec: JsonCodec[Array[ZonedDateTime]] = Schema.derived.jsonCodec
  val arrayOfZoneIdsCodec: JsonCodec[Array[ZoneId]] = Schema.derived.jsonCodec
  val arrayOfZoneOffsetsCodec: JsonCodec[Array[ZoneOffset]] = Schema.derived.jsonCodec
  val arraySeqOfBooleansCodec: JsonCodec[ArraySeq[Boolean]] = Schema.derived.jsonCodec
  val bigDecimalCodec: JsonCodec[BigDecimal] = new JsonCodec[BigDecimal] {
    override def decodeValue(in: JsonReader): BigDecimal =
      in.readBigDecimal(MathContext.UNLIMITED, Int.MaxValue, Int.MaxValue) // WARNING: It is an unsafe option for open systems

    override def encodeValue(x: BigDecimal, out: JsonWriter): Unit = out.writeVal(x)
  }
  val bigIntCodec: JsonCodec[BigInt] = new JsonCodec[BigInt] {
    override def decodeValue(in: JsonReader): BigInt =
      in.readBigInt(Int.MaxValue) // WARNING: It is an unsafe option for open systems

    override def encodeValue(x: BigInt, out: JsonWriter): Unit = out.writeVal(x)
  }
  val extractFieldsCodec: JsonCodec[ExtractFields] = Schema.derived.jsonCodec
  val geoJsonCodec: JsonCodec[GeoJSON.GeoJSON] =
    Schema.derived
      .deriving(JsonCodecDeriver.withDiscriminatorKind(DiscriminatorKind.Field("type")))
      .instance( // use a custom codec for (Double, Double) to improve parsing and serialization performance
        TypeId.of[(Double, Double)],
        new JsonCodec[(Double, Double)] {
          override def decodeValue(in: JsonReader): (Double, Double) =
            if (in.isNextToken('[')) {
              val x =
                try in.readDouble()
                catch {
                  case err if NonFatal(err) => error(new DynamicOptic.Node.Field("_1"), err)
                }
              if (in.isNextToken(',')) {
                val y =
                  try in.readDouble()
                  catch {
                    case err if NonFatal(err) => error(new DynamicOptic.Node.Field("_2"), err)
                  }
                if (in.isNextToken(']')) new Tuple2[Double, Double](x, y)
                else error("expected ']'")
              } else error("expected ','")
            } else error("expected '['")

          override def encodeValue(x: (Double, Double), out: JsonWriter): Unit = {
            out.writeArrayStart()
            out.writeVal(x._1)
            out.writeVal(x._2)
            out.writeArrayEnd()
          }
        }
      )
      .derive
  val gitHubActionsAPICodec: JsonCodec[GitHubActionsAPI.Response] =
    Schema.derived
      .deriving(JsonCodecDeriver)
      .instance( // use a custom codec to stringify booleans
        TypeId.boolean,
        new JsonCodec[Boolean] {
          override def decodeValue(in: JsonReader): Boolean = in.readStringAsBoolean()

          override def encodeValue(x: Boolean, out: JsonWriter): Unit = out.writeValAsString(x)
        }
      )
      .derive
  val googleMapsAPICodec: JsonCodec[GoogleMapsAPI.DistanceMatrix] = Schema.derived.jsonCodec
  val listOfBooleansCodec: JsonCodec[List[Boolean]] = Schema[List[Boolean]].jsonCodec
  val mapOfIntsToBooleansCodec: JsonCodec[Map[Int, Boolean]] = Schema[Map[Int, Boolean]].jsonCodec
  val missingRequiredFieldsCodec: JsonCodec[MissingRequiredFields] = Schema.derived.jsonCodec
  val nestedStructsCodec: JsonCodec[NestedStructs] = Schema.derived.jsonCodec
  val openRTBBidRequestCodec: JsonCodec[OpenRTB.BidRequest] = Schema.derived.jsonCodec
  val setOfIntsCodec: JsonCodec[Set[Int]] = Schema[Set[Int]].jsonCodec
  val stringCodec: JsonCodec[String] = Schema[String].jsonCodec
  val twitterAPICodec: JsonCodec[Seq[TwitterAPI.Tweet]] = Schema.derived.jsonCodec
  val vectorOfBooleansCodec: JsonCodec[Vector[Boolean]] = Schema[Vector[Boolean]].jsonCodec
}
