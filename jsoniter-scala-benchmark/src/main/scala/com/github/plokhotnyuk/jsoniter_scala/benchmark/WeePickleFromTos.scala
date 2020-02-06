package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI.DistanceMatrix
import com.rallyhealth.weejson.v1.jackson.CustomPrettyPrinter.FieldSepPrettyPrinter
import com.rallyhealth.weejson.v1.jackson.JsonGeneratorOps
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.Visitor

object WeePickleFromTos {
  object ToPrettyJson extends JsonGeneratorOps {
    private[this] val indenter = new DefaultIndenter("  ", "\n")

    override protected def wrapGenerator(g: JsonGenerator): JsonGenerator =
      g.setPrettyPrinter(new FieldSepPrettyPrinter(new DefaultPrettyPrinter()
        .withObjectIndenter(indenter)
        .withArrayIndenter(indenter)))
  }

  implicit val adtFromTos: FromTo[ADTBase] =
    FromTo.merge(macroFromTo[X], macroFromTo[Y], macroFromTo[Z])
  implicit val anyRefsFromTos: FromTo[AnyRefs] = macroFromTo
  implicit val anyValsFromTo: FromTo[AnyVals] = {
    implicit val v1: FromTo[ByteVal] = fromTo[Byte].bimap(_.a, ByteVal.apply)
    implicit val v2: FromTo[ShortVal] = fromTo[Short].bimap(_.a, ShortVal.apply)
    implicit val v3: FromTo[IntVal] = fromTo[Int].bimap(_.a, IntVal.apply)
    implicit val v4: FromTo[LongVal] = fromTo[Long].bimap(_.a, LongVal.apply)
    implicit val v5: FromTo[BooleanVal] = fromTo[Boolean].bimap(_.a, BooleanVal.apply)
    implicit val v6: FromTo[DoubleVal] = fromTo[Double].bimap(_.a, DoubleVal.apply)
    implicit val v7: FromTo[CharVal] = fromTo[Char].bimap(_.a, CharVal.apply)
    implicit val v8: FromTo[FloatVal] = fromTo[Float].bimap(_.a, FloatVal.apply)
    macroFromTo
  }
  implicit val simpleGeometryReadFromTos: FromTo[GeoJSON.SimpleGeometry] =
    FromTo.merge(macroFromTo[GeoJSON.Point], macroFromTo[GeoJSON.MultiPoint], macroFromTo[GeoJSON.LineString],
      macroFromTo[GeoJSON.MultiLineString], macroFromTo[GeoJSON.Polygon], macroFromTo[GeoJSON.MultiPolygon])
  implicit val geometryReadFromTos: FromTo[GeoJSON.Geometry] =
    FromTo.merge(macroFromTo[GeoJSON.Point], macroFromTo[GeoJSON.MultiPoint], macroFromTo[GeoJSON.LineString],
      macroFromTo[GeoJSON.MultiLineString], macroFromTo[GeoJSON.Polygon], macroFromTo[GeoJSON.MultiPolygon],
      macroFromTo[GeoJSON.GeometryCollection])
  implicit val simpleGeoJsonFromTos: FromTo[GeoJSON.SimpleGeoJSON] =
    FromTo.merge(macroFromTo[GeoJSON.Feature])
  implicit val geoJsonFromTos: FromTo[GeoJSON.GeoJSON] =
    FromTo.merge(macroFromTo[GeoJSON.Feature], macroFromTo[GeoJSON.FeatureCollection])
  implicit val googleMapsAPIDistanceMatrixFromTos: FromTo[DistanceMatrix] = {
    implicit val ft1: FromTo[GoogleMapsAPI.Value] = macroFromTo
    implicit val ft2: FromTo[GoogleMapsAPI.Elements] = macroFromTo
    implicit val ft3: FromTo[GoogleMapsAPI.Rows] = macroFromTo
    macroFromTo
  }
  implicit val missingRequiredFieldsFromTos: FromTo[MissingRequiredFields] = macroFromTo
  implicit val nestedStructsFromTos: FromTo[NestedStructs] = macroFromTo
  implicit val extractFieldsFromTos: FromTo[ExtractFields] = macroFromTo
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

  val fromNonBinaryByteArray: From[Array[Byte]] = {
    // Not an implicit val, since this isn't default behavior.
    // Force encoding as [0,1,255] rather than base64, e.g. Visitor.visitBinary().
    // Otherwise, WeePickle defaults to jackson.JsonGenerator.writeBinary()'s default.
    // Hack: Trick the `ArrayFrom` impl with a arg that does not eq `FromByte`.
    ArrayFrom[Byte](
      new From[Byte] {
        def transform0[R](v: Byte, out: Visitor[_, R]): R = out.visitInt32(v)
      }
    )
  }
}
