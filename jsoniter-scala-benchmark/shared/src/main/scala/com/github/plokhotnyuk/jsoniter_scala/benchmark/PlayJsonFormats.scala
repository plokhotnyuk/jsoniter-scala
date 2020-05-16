package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._

import ai.x.play.json.Encoders._
import ai.x.play.json.Jsonx
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask
import julienrf.json.derived.flat
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule

import scala.collection.immutable.{BitSet, IntMap, Map, Seq}
import scala.collection.mutable
import scala.util.Try

object PlayJsonFormats {
  private[this] val prettyPrintMapper = new ObjectMapper {
    registerModule(new PlayJsonModule(JsonParserSettings.settings))
    configure(SerializationFeature.INDENT_OUTPUT, true)
    setDefaultPrettyPrinter {
      val indenter = new DefaultIndenter("  ", "\n")
      new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
    }
  }

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = prettyPrintMapper.writeValueAsBytes(jsValue)

  def stringFormat[A](name: String)(f: String => A): Format[A] =
    new Format[A] {
      override def reads(js: JsValue): JsResult[A] =
        Try(JsSuccess(f(js.asInstanceOf[JsString].value))).getOrElse(JsError(s"expected.${name}string"))

      override def writes(v: A): JsValue = JsString(v.toString)
    }

  implicit val intKeyReads: KeyReads[Int] = (s: String) =>
    Try(JsSuccess(s.toInt)).getOrElse(JsError(s"expected.intstring"))
  implicit val intKeyWrites: KeyWrites[Int] = _.toString
  implicit val longKeyReads: KeyReads[Long] = (s: String) =>
    Try(JsSuccess(s.toLong)).getOrElse(JsError(s"expected.longstring"))

  implicit def mutableMapReads[A, B](implicit mapReads: Reads[Map[A, B]]): Reads[mutable.Map[A, B]] =
    Reads[mutable.Map[A, B]](js => JsSuccess(js.as[Map[A, B]].foldLeft(mutable.Map.empty[A, B]) {
      (m, p) => m += ((p._1, p._2))
    }))

  implicit def mutableLongMapFormat[A](implicit mapReads: Reads[Map[Long, A]], aWrites: Writes[A]): Format[mutable.LongMap[A]] =
    new Format[mutable.LongMap[A]] {
      override def reads(js: JsValue): JsResult[mutable.LongMap[A]] =
        JsSuccess(js.as[Map[Long, A]].foldLeft(mutable.LongMap.empty[A])((m, p) => m += (p._1, p._2)))

      override def writes(v: mutable.LongMap[A]): JsValue =
        Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
          (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
        })
    }

  implicit def intMapFormat[A](implicit mapReads: Reads[Map[Int, A]], aWrites: Writes[A]): Format[IntMap[A]] =
    new Format[IntMap[A]] {
      override def reads(js: JsValue): JsResult[IntMap[A]] =
        JsSuccess(js.as[Map[Int, A]].foldLeft(IntMap.empty[A])((m, p) => m.updated(p._1, p._2)))

      override def writes(v: IntMap[A]): JsValue = Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
        (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
      })
    }

  // Allow case classes with Tuple2 types to be represented as a Json Array with 2 elements e.g. (Double, Double)
  // Borrowed from https://gist.github.com/alexanderjarvis/4595298
  implicit def tuple2Format[A, B](implicit aFormat: Format[A], bFormat: Format[B]): Format[Tuple2[A, B]] =
    new Format[Tuple2[A, B]] {
      override def reads(js: JsValue): JsResult[(A, B)] = Try {
        val arr = js.asInstanceOf[JsArray]
        aFormat.reads(arr(0)).flatMap(a => bFormat.reads(arr(1)).map(b => (a, b)))
      }.getOrElse(JsError("expected.jsarray"))

      override def writes(tuple: (A, B)): JsValue = JsArray(Seq(aFormat.writes(tuple._1), bFormat.writes(tuple._2)))
    }

  implicit val charFormat: Format[Char] = stringFormat("char") { case s if s.length == 1 => s.charAt(0) }
  implicit val missingReqFieldsFormat: Format[MissingRequiredFields] = Json.format
  implicit val nestedStructsFormat: Format[NestedStructs] = Json.format
  implicit val anyValsFormat: Format[AnyVals] = {
    implicit val v1: Format[ByteVal] = Jsonx.formatInline
    implicit val v2: Format[ShortVal] = Jsonx.formatInline
    implicit val v3: Format[IntVal] = Jsonx.formatInline
    implicit val v4: Format[LongVal] = Jsonx.formatInline
    implicit val v5: Format[BooleanVal] = Jsonx.formatInline
    implicit val v6: Format[CharVal] = Jsonx.formatInline
    implicit val v7: Format[DoubleVal] = Jsonx.formatInline
    implicit val v8: Format[FloatVal] = Jsonx.formatInline
    Json.format
  }
  implicit val bitSetFormat: Format[BitSet] = Format(
    Reads(js => JsSuccess(BitSet.fromBitMaskNoCopy(toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))),
    Writes((es: BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val mutableBitSetFormat: Format[mutable.BitSet] = Format(
    Reads(js => JsSuccess(mutable.BitSet.fromBitMaskNoCopy(toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: It is an unsafe option for open systems */)))),
    Writes((es: mutable.BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val primitivesFormat: Format[Primitives] = Json.format
  implicit val extractFieldsFormat: Format[ExtractFields] = Json.format
  val adtFormat: Format[ADTBase] = {
    implicit lazy val v1: Format[X] = Json.format
    implicit lazy val v2: Format[Y] = Json.format
    implicit lazy val v3: Format[Z] = Json.format
    implicit lazy val v4: Format[ADTBase] = flat.oformat((__ \ "type").format[String])
    v4
  }
  val geoJSONFormat: Format[GeoJSON.GeoJSON] = {
    implicit lazy val v1: Format[GeoJSON.Point] =
      (__ \ "coordinates").format[(Double, Double)].inmap(GeoJSON.Point.apply, _.coordinates)
    implicit lazy val v2: Format[GeoJSON.MultiPoint] = Json.format
    implicit lazy val v3: Format[GeoJSON.LineString] = Json.format
    implicit lazy val v4: Format[GeoJSON.MultiLineString] = Json.format
    implicit lazy val v5: Format[GeoJSON.Polygon] = Json.format
    implicit lazy val v6: Format[GeoJSON.MultiPolygon] = Json.format
    implicit lazy val v7: Format[GeoJSON.SimpleGeometry] = flat.oformat((__ \ "type").format[String])
    implicit lazy val v8: Format[GeoJSON.GeometryCollection] = Json.format
    implicit lazy val v9: Format[GeoJSON.Geometry] = flat.oformat((__ \ "type").format[String])
    implicit lazy val v10: Format[GeoJSON.Feature] = Json.format
    implicit lazy val v11: Format[GeoJSON.SimpleGeoJSON] = flat.oformat((__ \ "type").format[String])
    implicit lazy val v12: Format[GeoJSON.FeatureCollection] = Json.format
    implicit lazy val v13: Format[GeoJSON.GeoJSON] = flat.oformat((__ \ "type").format[String])
    v13
  }
  implicit val googleMapsAPIFormat: Format[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: Format[GoogleMapsAPI.Value] = Json.format
    implicit val v2: Format[GoogleMapsAPI.Elements] = Json.format
    implicit val v3: Format[GoogleMapsAPI.Rows] = Json.format
    Json.format
  }
  implicit val openRTBBidRequestFormat: Format[OpenRTB.BidRequest] = {
    implicit val v1: Format[OpenRTB.Segment] = Jsonx.formatCaseClassUseDefaults
    implicit val v2: Format[OpenRTB.Format] = Jsonx.formatCaseClassUseDefaults
    implicit val v3: Format[OpenRTB.Deal] = Jsonx.formatCaseClassUseDefaults
    implicit val v4: Format[OpenRTB.Metric] = Jsonx.formatCaseClassUseDefaults
    implicit val v5: Format[OpenRTB.Banner] = Jsonx.formatCaseClassUseDefaults
    implicit val v6: Format[OpenRTB.Audio] = Jsonx.formatCaseClassUseDefaults
    implicit val v7: Format[OpenRTB.Video] = Jsonx.formatCaseClassUseDefaults
    implicit val v8: Format[OpenRTB.Native] = Jsonx.formatCaseClassUseDefaults
    implicit val v9: Format[OpenRTB.Pmp] = Jsonx.formatCaseClassUseDefaults
    implicit val v10: Format[OpenRTB.Producer] = Jsonx.formatCaseClassUseDefaults
    implicit val v11: Format[OpenRTB.Data] = Jsonx.formatCaseClassUseDefaults
    implicit val v12: Format[OpenRTB.Content] = Jsonx.formatCaseClassUseDefaults
    implicit val v13: Format[OpenRTB.Publisher] = Jsonx.formatCaseClassUseDefaults
    implicit val v14: Format[OpenRTB.Geo] = Jsonx.formatCaseClassUseDefaults
    implicit val v15: Format[OpenRTB.Imp] = Jsonx.formatCaseClassUseDefaults
    implicit val v16: Format[OpenRTB.Site] = Jsonx.formatCaseClassUseDefaults
    implicit val v17: Format[OpenRTB.App] = Jsonx.formatCaseClassUseDefaults
    implicit val v18: Format[OpenRTB.Device] = Jsonx.formatCaseClassUseDefaults
    implicit val v19: Format[OpenRTB.User] = Jsonx.formatCaseClassUseDefaults
    implicit val v20: Format[OpenRTB.Source] = Jsonx.formatCaseClassUseDefaults
    implicit val v21: Format[OpenRTB.Reqs] = Jsonx.formatCaseClassUseDefaults
    Json.format
  }
  implicit val twitterFormat: Format[TwitterAPI.Tweet] = {
    implicit val v1: Format[TwitterAPI.Urls] = Json.format
    implicit val v2: Format[TwitterAPI.Url] = Json.format
    implicit val v3: Format[TwitterAPI.UserEntities] = Json.format
    implicit val v4: Format[TwitterAPI.UserMentions] = Json.format
    implicit val v5: Format[TwitterAPI.Entities] = Json.format
    implicit val v6: Format[TwitterAPI.User] = Jsonx.formatCaseClass
    implicit val v7: Format[TwitterAPI.RetweetedStatus] = Jsonx.formatCaseClass
    Jsonx.formatCaseClass
  }
  implicit val enumFormat: Format[SuitEnum] = Format(Reads.enumNameReads(SuitEnum), Writes.enumNameWrites)
  implicit val enumADTFormat: Format[SuitADT] = stringFormat("suitadt") {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s: String => suite(s)
  }
  implicit val javaEnumFormat: Format[Suit] = stringFormat("suitenum")(Suit.valueOf)
  implicit val monthDayFormat: Format[MonthDay] = stringFormat("monthday")(MonthDay.parse)
  implicit val offsetTimeFormat: Format[OffsetTime] = stringFormat("offsettime")(OffsetTime.parse)
  implicit val yearFormat: Format[Year] = stringFormat("year")(Year.parse)
  implicit val yearMonthFormat: Format[YearMonth] = stringFormat("yearmonth")(YearMonth.parse)
  implicit val zoneOffsetFormat: Format[ZoneOffset] = stringFormat("zoneoffset")(ZoneOffset.of)
}