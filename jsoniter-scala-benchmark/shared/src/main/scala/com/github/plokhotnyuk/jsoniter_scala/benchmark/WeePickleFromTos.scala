package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.{JsonFactory, JsonFactoryBuilder, JsonGenerator, StreamReadFeature, StreamWriteFeature}
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI.DistanceMatrix
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.rallyhealth.weejson.v1.jackson.CustomPrettyPrinter.FieldSepPrettyPrinter
import com.rallyhealth.weejson.v1.jackson.JsonGeneratorOps
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.Visitor

import java.time._

object WeePickleFromTos extends WeePickleFromTos2 {
  def defaultJsonFactoryBuilder: JsonFactoryBuilder = JsonFactory.builder().asInstanceOf[JsonFactoryBuilder]
    .configure(StreamReadFeature.USE_FAST_DOUBLE_PARSER, true)
    .configure(StreamWriteFeature.USE_FAST_DOUBLE_WRITER, true)

  object ToJson extends JsonGeneratorOps(defaultJsonFactoryBuilder.build())

  object ToPrettyJson extends JsonGeneratorOps(defaultJsonFactoryBuilder.build()) {
    override protected def wrapGenerator(g: JsonGenerator): JsonGenerator =
      g.setPrettyPrinter(new FieldSepPrettyPrinter({
        val indenter = new DefaultIndenter("  ", "\n")
        new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
      }))
  }

  object ToEscapedNonAsciiJson extends JsonGeneratorOps(
    defaultJsonFactoryBuilder
      .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
      .build()
  )

  implicit val adtFromTos: FromTo[ADTBase] =
    FromTo.merge(macroFromTo[X], macroFromTo[Y], macroFromTo[Z])
  implicit val anyValsFromTo: FromTo[AnyVals] = {
    implicit val ft1: FromTo[ByteVal] = fromTo[Byte].bimap(_.a, ByteVal.apply)
    implicit val ft2: FromTo[ShortVal] = fromTo[Short].bimap(_.a, ShortVal.apply)
    implicit val ft3: FromTo[IntVal] = fromTo[Int].bimap(_.a, IntVal.apply)
    implicit val ft4: FromTo[LongVal] = fromTo[Long].bimap(_.a, LongVal.apply)
    implicit val ft5: FromTo[BooleanVal] = fromTo[Boolean].bimap(_.a, BooleanVal.apply)
    implicit val ft6: FromTo[DoubleVal] = fromTo[Double].bimap(_.a, DoubleVal.apply)
    implicit val ft7: FromTo[CharVal] = fromTo[Char].bimap(_.a, CharVal.apply)
    implicit val ft8: FromTo[FloatVal] = fromTo[Float].bimap(_.a, FloatVal.apply)
    macroFromTo
  }
  implicit val durationFromTo: FromTo[Duration] = fromTo[String].bimap(_.toString, Duration.parse)
  implicit val enumADTFromTo: FromTo[SuitADT] = fromTo[String].bimap(_.toString, {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => suite.getOrElse(s, throw new IllegalArgumentException("SuitADT"))
  })
  implicit val enumFromTo: FromTo[SuitEnum] = fromToEnumerationName(SuitEnum)
  implicit val extractFieldsFromTos: FromTo[ExtractFields] = macroFromTo
  implicit val javaEnumFromTo: FromTo[Suit] = fromTo[String].bimap(_.toString, Suit.valueOf)
  implicit val geoJsonFromTos: FromTo[GeoJSON.GeoJSON] = {
    implicit val ft1: FromTo[GeoJSON.SimpleGeometry] = FromTo.merge(
      macroFromTo[GeoJSON.Point],
      macroFromTo[GeoJSON.MultiPoint],
      macroFromTo[GeoJSON.LineString],
      macroFromTo[GeoJSON.MultiLineString],
      macroFromTo[GeoJSON.Polygon],
      macroFromTo[GeoJSON.MultiPolygon]
    )
    implicit val ft2: FromTo[GeoJSON.Geometry] = FromTo.merge(
      macroFromTo[GeoJSON.Point],
      macroFromTo[GeoJSON.MultiPoint],
      macroFromTo[GeoJSON.LineString],
      macroFromTo[GeoJSON.MultiLineString],
      macroFromTo[GeoJSON.Polygon],
      macroFromTo[GeoJSON.MultiPolygon],
      macroFromTo[GeoJSON.GeometryCollection]
    )
    implicit val ft3: FromTo[GeoJSON.SimpleGeoJSON] = FromTo.merge(macroFromTo[GeoJSON.Feature])
    FromTo.merge(macroFromTo[GeoJSON.Feature], macroFromTo[GeoJSON.FeatureCollection])
  }
  implicit val gitHubActionsAPIFromTos: FromTo[GitHubActionsAPI.Response] = {
    implicit val ft1: FromTo[Boolean] = fromTo[String].bimap(_.toString, _.toBoolean)
    implicit val ft2: FromTo[GitHubActionsAPI.Artifact] = macroFromTo
    macroFromTo
  }
  implicit val googleMapsAPIDistanceMatrixFromTos: FromTo[DistanceMatrix] = {
    implicit val ft1: FromTo[GoogleMapsAPI.Value] = macroFromTo
    implicit val ft2: FromTo[GoogleMapsAPI.Elements] = macroFromTo
    implicit val ft3: FromTo[GoogleMapsAPI.Rows] = macroFromTo
    macroFromTo
  }
  implicit val missingRequiredFieldsFromTos: FromTo[MissingRequiredFields] = macroFromTo
  implicit val monthDayFromTo: FromTo[MonthDay] = fromTo[String].bimap(_.toString, MonthDay.parse)
  implicit val nestedStructsFromTos: FromTo[NestedStructs] = macroFromTo
  implicit val offsetTimeFromTo: FromTo[OffsetTime] = fromTo[String].bimap(_.toString, OffsetTime.parse)
}

trait WeePickleFromTos2 {
  implicit val openRTBBidRequestFromTos: FromTo[OpenRTB.BidRequest] = {
    implicit val ft1: FromTo[OpenRTB.Segment] = macroFromTo
    implicit val ft2: FromTo[OpenRTB.Format] = macroFromTo
    implicit val ft3: FromTo[OpenRTB.Deal] = macroFromTo
    implicit val ft4: FromTo[OpenRTB.Metric] = macroFromTo
    implicit val ft5: FromTo[OpenRTB.Banner] = macroFromTo
    implicit val ft6: FromTo[OpenRTB.Audio] = macroFromTo
    implicit val ft7: FromTo[OpenRTB.Video] = macroFromTo
    implicit val ft8: FromTo[OpenRTB.Native] = macroFromTo
    implicit val ft9: FromTo[OpenRTB.Pmp] = macroFromTo
    implicit val ft10: FromTo[OpenRTB.Producer] = macroFromTo
    implicit val ft11: FromTo[OpenRTB.Data] = macroFromTo
    implicit val ft12: FromTo[OpenRTB.Content] = macroFromTo
    implicit val ft13: FromTo[OpenRTB.Publisher] = macroFromTo
    implicit val ft14: FromTo[OpenRTB.Geo] = macroFromTo
    implicit val ft15: FromTo[OpenRTB.Imp] = macroFromTo
    implicit val ft16: FromTo[OpenRTB.Site] = macroFromTo
    implicit val ft17: FromTo[OpenRTB.App] = macroFromTo
    implicit val ft18: FromTo[OpenRTB.Device] = macroFromTo
    implicit val ft19: FromTo[OpenRTB.User] = macroFromTo
    implicit val ft20: FromTo[OpenRTB.Source] = macroFromTo
    implicit val ft21: FromTo[OpenRTB.Reqs] = macroFromTo
    macroFromTo
  }
  implicit val periodFromTo: FromTo[Period] = fromTo[String].bimap(_.toString, Period.parse)
  implicit val primitivesFromTos: FromTo[Primitives] = macroFromTo
  implicit val twitterAPIFromTos: FromTo[TwitterAPI.Tweet] = {
    implicit val ft1: FromTo[TwitterAPI.Urls] = macroFromTo
    implicit val ft2: FromTo[TwitterAPI.Url] = macroFromTo
    implicit val ft3: FromTo[TwitterAPI.UserMentions] = macroFromTo
    implicit val ft4: FromTo[TwitterAPI.Entities] = macroFromTo
    implicit val ft5: FromTo[TwitterAPI.UserEntities] = macroFromTo
    implicit val ft6: FromTo[TwitterAPI.User] = macroFromTo
    implicit val ft7: FromTo[TwitterAPI.RetweetedStatus] = macroFromTo
    macroFromTo
  }
  implicit val yearMonthFromTo: FromTo[YearMonth] = fromTo[String].bimap(_.toString, YearMonth.parse)
  implicit val yearFromTo: FromTo[Year] = fromTo[String].bimap(_.toString, Year.parse)
  implicit val zoneIdFromTo: FromTo[ZoneId] = fromTo[String].bimap(_.toString, ZoneId.of)
  implicit val zoneOffsetFromTo: FromTo[ZoneOffset] = fromTo[String].bimap(_.toString, ZoneOffset.of)

  val fromNonBinaryByteArray: From[Array[Byte]] = ArrayFrom[Byte](new From[Byte] {
    // Force encoding as [0,1,255] rather than base64, e.g. Visitor.visitBinary().
    // Otherwise, WeePickle defaults to jackson.JsonGenerator.writeBinary()'s default.
    // Hack: Trick the `ArrayFrom` impl with a arg that does not eq `FromByte`.
    def transform0[R](v: Byte, out: Visitor[_, R]): R = out.visitInt32(v)
  })
}
