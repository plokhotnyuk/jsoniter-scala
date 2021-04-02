package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import zio.json.JsonDecoder.JsonError
import zio.json._
import zio.json.internal._

import java.time._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.ClassTag

object ZioJSONEncoderDecoders extends ZioJSONNonGenEncoderDecoders {
  implicit val (adtE5r: JsonEncoder[ADTBase], adtD5r: JsonDecoder[ADTBase]) = {
    implicit val c1: JsonCodec[ADTBase] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (anyValsE5r: JsonEncoder[AnyVals], anyValsD5r: JsonDecoder[AnyVals]) = {
    implicit val c1: JsonCodec[ByteVal] = JsonCodec.byte.xmap(ByteVal.apply, _.a)
    implicit val c2: JsonCodec[ShortVal] = JsonCodec.short.xmap(ShortVal.apply, _.a)
    implicit val c3: JsonCodec[IntVal] = JsonCodec.int.xmap(IntVal.apply, _.a)
    implicit val c4: JsonCodec[LongVal] = JsonCodec.long.xmap(LongVal.apply, _.a)
    implicit val c5: JsonCodec[BooleanVal] = JsonCodec.boolean.xmap(BooleanVal.apply, _.a)
    implicit val c6: JsonCodec[CharVal] = JsonCodec.char.xmap(CharVal.apply, _.a)
    implicit val c7: JsonCodec[DoubleVal] = JsonCodec.double.xmap(DoubleVal.apply, _.a)
    implicit val c8: JsonCodec[FloatVal] = JsonCodec.float.xmap(FloatVal.apply, _.a)
    implicit val c9: JsonCodec[AnyVals] = DeriveJsonCodec.gen
    (c9.encoder, c9.decoder)
  }
  implicit val (geoJsonE5r: JsonEncoder[GeoJSON.GeoJSON], geoJsonD5r: JsonDecoder[GeoJSON.GeoJSON]) = {
    implicit val c1: JsonCodec[GeoJSON.SimpleGeometry] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GeoJSON.Geometry] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GeoJSON.SimpleGeoJSON] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[GeoJSON.GeoJSON] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (gitHubActionsAPIE5r: JsonEncoder[GitHubActionsAPI.Response], gitHubActionsAPID5r: JsonDecoder[GitHubActionsAPI.Response]) = {
    implicit val c1: JsonCodec[Boolean] = new JsonCodec[Boolean] {
      override def encoder: JsonEncoder[Boolean] = this

      override def decoder: JsonDecoder[Boolean] = this

      override def unsafeEncode(a: Boolean, indent: Option[Int], out: Write): Unit =
        out.write(if (a) "\"true\"" else "\"false\"")

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): Boolean = {
        Lexer.char(trace, in, '"')
        val x = Lexer.boolean(trace, in)
        Lexer.char(trace, in, '"')
        x
      }
    }
    implicit val c2: JsonCodec[GitHubActionsAPI.Artifact] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GitHubActionsAPI.Response] = DeriveJsonCodec.gen
    (c3.encoder, c3.decoder)
  }
  implicit val (googleMapsAPIE5r: JsonEncoder[GoogleMapsAPI.DistanceMatrix], googleMapsAPID5r: JsonDecoder[GoogleMapsAPI.DistanceMatrix]) = {
    implicit val c1: JsonCodec[GoogleMapsAPI.Value] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[GoogleMapsAPI.Elements] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[GoogleMapsAPI.Rows] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[GoogleMapsAPI.DistanceMatrix] = DeriveJsonCodec.gen
    (c4.encoder, c4.decoder)
  }
  implicit val (extractFieldsE5r: JsonEncoder[ExtractFields], extractFieldsD5r: JsonDecoder[ExtractFields]) = {
    implicit val c1: JsonCodec[ExtractFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (missingRequiredFieldsE5r: JsonEncoder[MissingRequiredFields], missingRequiredFieldsD5r: JsonDecoder[MissingRequiredFields]) = {
    implicit val c1: JsonCodec[MissingRequiredFields] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (nestedStructsE5r: JsonEncoder[NestedStructs], nestedStructsD5r: JsonDecoder[NestedStructs]) = {
    implicit lazy val c1: JsonCodec[NestedStructs] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (openRTBBidRequestE5r: JsonEncoder[OpenRTB.BidRequest], openRTBBidRequestD5r: JsonDecoder[OpenRTB.BidRequest]) = {
    implicit val c1: JsonCodec[OpenRTB.Segment] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[OpenRTB.Format] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[OpenRTB.Deal] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[OpenRTB.Metric] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[OpenRTB.Banner] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[OpenRTB.Audio] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[OpenRTB.Video] = DeriveJsonCodec.gen
    implicit val c8: JsonCodec[OpenRTB.Native] = DeriveJsonCodec.gen
    implicit val c9: JsonCodec[OpenRTB.Pmp] = DeriveJsonCodec.gen
    implicit val c10: JsonCodec[OpenRTB.Producer] = DeriveJsonCodec.gen
    implicit val c11: JsonCodec[OpenRTB.Data] = DeriveJsonCodec.gen
    implicit val c12: JsonCodec[OpenRTB.Content] = DeriveJsonCodec.gen
    implicit val c13: JsonCodec[OpenRTB.Publisher] = DeriveJsonCodec.gen
    implicit val c14: JsonCodec[OpenRTB.Geo] = DeriveJsonCodec.gen
    implicit val c15: JsonCodec[OpenRTB.Imp] = DeriveJsonCodec.gen
    implicit val c16: JsonCodec[OpenRTB.Site] = DeriveJsonCodec.gen
    implicit val c17: JsonCodec[OpenRTB.App] = DeriveJsonCodec.gen
    implicit val c18: JsonCodec[OpenRTB.Device] = DeriveJsonCodec.gen
    implicit val c19: JsonCodec[OpenRTB.User] = DeriveJsonCodec.gen
    implicit val c20: JsonCodec[OpenRTB.Source] = DeriveJsonCodec.gen
    implicit val c21: JsonCodec[OpenRTB.Reqs] = DeriveJsonCodec.gen
    implicit val c22: JsonCodec[OpenRTB.BidRequest] = DeriveJsonCodec.gen
    (c22.encoder, c22.decoder)
  }
  implicit val (primitivesE5r: JsonEncoder[Primitives], primitivesD5r: JsonDecoder[Primitives]) = {
    implicit val c1: JsonCodec[Primitives] = DeriveJsonCodec.gen
    (c1.encoder, c1.decoder)
  }
  implicit val (twitterAPIE5r: JsonEncoder[TwitterAPI.Tweet], twitterAPID5r: JsonDecoder[TwitterAPI.Tweet]) = {
    implicit val c1: JsonCodec[TwitterAPI.UserMentions] = DeriveJsonCodec.gen
    implicit val c2: JsonCodec[TwitterAPI.Urls] = DeriveJsonCodec.gen
    implicit val c3: JsonCodec[TwitterAPI.Entities] = DeriveJsonCodec.gen
    implicit val c4: JsonCodec[TwitterAPI.Url] = DeriveJsonCodec.gen
    implicit val c5: JsonCodec[TwitterAPI.UserEntities] = DeriveJsonCodec.gen
    implicit val c6: JsonCodec[TwitterAPI.User] = DeriveJsonCodec.gen
    implicit val c7: JsonCodec[TwitterAPI.RetweetedStatus] = DeriveJsonCodec.gen
    implicit val c8: JsonCodec[TwitterAPI.Tweet] = DeriveJsonCodec.gen
    (c8.encoder, c8.decoder)
  }
}

trait ZioJSONNonGenEncoderDecoders {
  implicit val (intFE5r: JsonFieldEncoder[Int], intFD5r: JsonFieldDecoder[Int]) =
    (new JsonFieldEncoder[Int] {
      override def unsafeEncodeField(in: Int): String = in.toString
    }, new JsonFieldDecoder[Int] {
      override def unsafeDecodeField(trace: List[JsonError], in: String): Int = Integer.parseInt(in)
    })
  implicit val (arrayOfBigDecimalsE5r: JsonEncoder[Array[BigDecimal]], arrayOfBigDecimalsD5r: JsonDecoder[Array[BigDecimal]]) =
    (arrayEncoder[BigDecimal], arrayDecoder[BigDecimal])
  implicit val (arrayOfBooleansE5r: JsonEncoder[Array[Boolean]], arrayOfBooleansD5r: JsonDecoder[Array[Boolean]]) =
    (arrayEncoder[Boolean], arrayDecoder[Boolean])
  implicit val (arrayOfBytesE5r: JsonEncoder[Array[Byte]], arrayOfBytesD5r: JsonDecoder[Array[Byte]]) =
    (arrayEncoder[Byte], arrayDecoder[Byte])
  implicit val (arrayOfCharsE5r: JsonEncoder[Array[Char]], arrayOfCharsD5r: JsonDecoder[Array[Char]]) =
    (arrayEncoder[Char], arrayDecoder[Char])
  implicit val (arrayOfDoublesE5r: JsonEncoder[Array[Double]], arrayOfDoublesD5r: JsonDecoder[Array[Double]]) =
    (arrayEncoder[Double], arrayDecoder[Double])
  implicit val (arrayOfDurationsE5r: JsonEncoder[Array[Duration]], arrayOfDurationsD5r: JsonDecoder[Array[Duration]]) =
    (arrayEncoder[Duration], arrayDecoder[Duration])
  implicit val (arrayOfEnumADTsE5r: JsonEncoder[Array[SuitADT]], arrayOfEnumADTsD5r: JsonDecoder[Array[SuitADT]]) =
    (arrayEncoder[SuitADT]{ (a: SuitADT, indent: Option[Int], out: Write) =>
      out.write('"')
      out.write(a.toString)
      out.write('"')
    }, arrayDecoder[SuitADT](new JsonDecoder[SuitADT] {
      private[this] val suite = Map(
        "Hearts" -> Hearts,
        "Spades" -> Spades,
        "Diamonds" -> Diamonds,
        "Clubs" -> Clubs)

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitADT =
        suite.getOrElse(Lexer.string(trace, in).toString, throw new IllegalArgumentException("SuitADT"))
    }, ClassTag(classOf[SuitADT])))
  implicit val (arrayOfEnumsE5r: JsonEncoder[Array[SuitEnum]], arrayOfEnumsD5r: JsonDecoder[Array[SuitEnum]]) =
    (arrayEncoder[SuitEnum]{ (a: SuitEnum, indent: Option[Int], out: Write) =>
      out.write('"')
      out.write(a.toString)
      out.write('"')
    }, arrayDecoder[SuitEnum](new JsonDecoder[SuitEnum] {
      private[this] val ec = new ConcurrentHashMap[String, SuitEnum]

      override def unsafeDecode(trace: List[JsonError], in: RetractReader): SuitEnum = {
        val s = Lexer.string(trace, in).toString
        var v = ec.get(s)
        if (v eq null) {
          v = SuitEnum.values.iterator.find(_.toString == s).getOrElse(throw new IllegalArgumentException("SuitEnum"))
          ec.put(s, v)
        }
        v
      }
    }, ClassTag(classOf[SuitEnum])))
  implicit val (arrayOfFloatsE5r: JsonEncoder[Array[Float]], arrayOfFloatsD5r: JsonDecoder[Array[Float]]) =
    (arrayEncoder[Float], arrayDecoder[Float])
  implicit val (arrayOfInstantsE5r: JsonEncoder[Array[Instant]], arrayOfInstantsD5r: JsonDecoder[Array[Instant]]) =
    (arrayEncoder[Instant], arrayDecoder[Instant])
  implicit val (arrayOfIntsE5r: JsonEncoder[Array[Int]], arrayOfIntsD5r: JsonDecoder[Array[Int]]) =
    (arrayEncoder[Int], arrayDecoder[Int])
  implicit val (arrayOfLocalDatesE5r: JsonEncoder[Array[LocalDate]], arrayOfLocalDatesD5r: JsonDecoder[Array[LocalDate]]) =
    (arrayEncoder[LocalDate], arrayDecoder[LocalDate])
  implicit val (arrayOfLocalDateTimesE5r: JsonEncoder[Array[LocalDateTime]], arrayOfLocalDateTimesD5r: JsonDecoder[Array[LocalDateTime]]) =
    (arrayEncoder[LocalDateTime], arrayDecoder[LocalDateTime])
  implicit val (arrayOfLocalTimesE5r: JsonEncoder[Array[LocalTime]], arrayOfLocalTimesD5r: JsonDecoder[Array[LocalTime]]) =
    (arrayEncoder[LocalTime], arrayDecoder[LocalTime])
  implicit val (arrayOfLongsE5r: JsonEncoder[Array[Long]], arrayOfLongsD5r: JsonDecoder[Array[Long]]) =
    (arrayEncoder[Long], arrayDecoder[Long])
  implicit val (arrayOfMonthDaysE5r: JsonEncoder[Array[MonthDay]], arrayOfMonthDaysD5r: JsonDecoder[Array[MonthDay]]) =
    (arrayEncoder[MonthDay], arrayDecoder[MonthDay])
  implicit val (arrayOfOffsetDateTimesE5r: JsonEncoder[Array[OffsetDateTime]], arrayOfOffsetDateTimesD5r: JsonDecoder[Array[OffsetDateTime]]) =
    (arrayEncoder[OffsetDateTime], arrayDecoder[OffsetDateTime])
  implicit val (arrayOfOffsetTimesE5r: JsonEncoder[Array[OffsetTime]], arrayOfOffsetTimesD5r: JsonDecoder[Array[OffsetTime]]) =
    (arrayEncoder[OffsetTime], arrayDecoder[OffsetTime])
  implicit val (arrayOfPeriodsE5r: JsonEncoder[Array[Period]], arrayOfPeriodsD5r: JsonDecoder[Array[Period]]) =
    (arrayEncoder[Period], arrayDecoder[Period])
  implicit val (arrayOfShortsE5r: JsonEncoder[Array[Short]], arrayOfShortsD5r: JsonDecoder[Array[Short]]) =
    (arrayEncoder[Short], arrayDecoder[Short])
  implicit val (arrayOfUUIDsE5r: JsonEncoder[Array[UUID]], arrayOfUUIDsD5r: JsonDecoder[Array[UUID]]) =
    (arrayEncoder[UUID], arrayDecoder[UUID])
  implicit val (arrayOfYearMonthsE5r: JsonEncoder[Array[YearMonth]], arrayOfYearMonthsD5r: JsonDecoder[Array[YearMonth]]) =
    (arrayEncoder[YearMonth], arrayDecoder[YearMonth])
  implicit val (arrayOfYearsE5r: JsonEncoder[Array[Year]], arrayOfYearsD5r: JsonDecoder[Array[Year]]) =
    (arrayEncoder[Year], arrayDecoder[Year])
  implicit val (arrayOfZonedDateTimesE5r: JsonEncoder[Array[ZonedDateTime]], arrayOfZonedDateTimesD5r: JsonDecoder[Array[ZonedDateTime]]) =
    (arrayEncoder[ZonedDateTime], arrayDecoder[ZonedDateTime])
  implicit val (arrayOfZoneIdsE5r: JsonEncoder[Array[ZoneId]], arrayOfZoneIdsD5r: JsonDecoder[Array[ZoneId]]) =
    (arrayEncoder[ZoneId], arrayDecoder[ZoneId])
  implicit val (arrayOfZoneOffsetsE5r: JsonEncoder[Array[ZoneOffset]], arrayOfZoneOffsetsD5r: JsonDecoder[Array[ZoneOffset]]) =
    (arrayEncoder[ZoneOffset], arrayDecoder[ZoneOffset])
  implicit val (listOfBooleansE5r: JsonEncoder[List[Boolean]], listOfBooleansD5r: JsonDecoder[List[Boolean]]) =
    (JsonEncoder.list[Boolean], JsonDecoder.list[Boolean])
  implicit val (mapOfIntsToBooleansE5r: JsonEncoder[Map[Int, Boolean]], mapOfIntsToBooleansD5r: JsonDecoder[Map[Int, Boolean]]) =
    (JsonEncoder.map[Int, Boolean], JsonDecoder.map[Int, Boolean])
  implicit val (setOfIntsE5r: JsonEncoder[Set[Int]], setOfIntsD5r: JsonDecoder[Set[Int]]) =
    (JsonEncoder.set[Int], JsonDecoder.set[Int])
  implicit val (vectorOfBooleansE5r: JsonEncoder[Vector[Boolean]], vectorOfBooleansD5r: JsonDecoder[Vector[Boolean]]) =
    (JsonEncoder.vector[Boolean], JsonDecoder.vector[Boolean])

  implicit def indexedSeqCodec[A](implicit codec: JsonCodec[A]): JsonCodec[IndexedSeq[A]] =
    JsonCodec.apply(indexedSeqEncoder(codec.encoder), indexedSeqDecoder(codec.decoder))

  implicit def indexedSeqEncoder[A](implicit encoder: JsonEncoder[A]): JsonEncoder[IndexedSeq[A]] =
    (as: IndexedSeq[A], indent: Option[Int], out: Write) => {
      out.write('[')
      if (indent.isEmpty) {
        as.foreach {
          var first = true
          a =>
            if (first) first = false
            else out.write(',')
            encoder.unsafeEncode(a, indent, out)
        }
      } else {
        as.foreach {
          var first = true
          a =>
            if (first) first = false
            else out.write(", ")
            encoder.unsafeEncode(a, indent, out)
        }
      }
      out.write(']')
    }

  implicit def indexedSeqDecoder[A](implicit decoder: JsonDecoder[A]): JsonDecoder[IndexedSeq[A]] =
    (trace: List[JsonError], in: RetractReader) => {
      val builder = IndexedSeq.newBuilder[A]
      Lexer.char(trace, in, '[')
      var i: Int = 0
      if (Lexer.firstArrayElement(in)) do {
        builder += decoder.unsafeDecode(JsonError.ArrayAccess(i) :: trace, in)
        i += 1
      } while (Lexer.nextArrayElement(trace, in))
      builder.result()
    }

  private[this] def arrayEncoder[A](implicit encoder: JsonEncoder[A]): JsonEncoder[Array[A]] =
    (as: Array[A], indent: Option[Int], out: Write) => {
      out.write('[')
      val len = as.length
      var i: Int = 0
      if (indent.isEmpty) {
        while (i < len) {
          if (i != 0) out.write(',')
          encoder.unsafeEncode(as(i), indent, out)
          i += 1
        }
      } else {
        while (i < len) {
          if (i != 0) out.write(", ")
          encoder.unsafeEncode(as(i), indent, out)
          i += 1
        }
      }
      out.write(']')
    }

  private[this] def arrayDecoder[A](implicit decoder: JsonDecoder[A], classTag: ClassTag[A]): JsonDecoder[Array[A]] =
    (trace: List[JsonError], in: RetractReader) => {
      val builder = Array.newBuilder[A]
      Lexer.char(trace, in, '[')
      var i: Int = 0
      if (Lexer.firstArrayElement(in)) do {
        builder += decoder.unsafeDecode(JsonError.ArrayAccess(i) :: trace, in)
        i += 1
      } while (Lexer.nextArrayElement(trace, in))
      builder.result()
    }
}

object ZioJSONNonGenEncoderDecoders extends ZioJSONNonGenEncoderDecoders