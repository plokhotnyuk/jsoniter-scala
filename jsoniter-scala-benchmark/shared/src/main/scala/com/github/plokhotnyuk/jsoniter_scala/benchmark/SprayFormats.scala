package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import pl.iterators.kebs.json.KebsSpray
import spray.json._

import scala.collection.immutable.Map
import scala.collection.mutable
import scala.util.Try
import scala.util.control.NonFatal

// Based on the code found: https://github.com/spray/spray-json/issues/200
case class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  private[this] val ec = new ConcurrentHashMap[String, T#Value]

  override def read(json: JsValue): T#Value = Try {
    val s = json.asInstanceOf[JsString].value
    var x = ec.get(s)
    if (x eq null) {
      x = e.values.iterator.find(_.toString == s).get
      ec.put(s, x)
    }
    x
  }.getOrElse(deserializationError(s"Expected JSON string of value from enum $e, but got $json"))

  override def write(ev: T#Value): JsValue = new JsString(ev.toString)
}

object CustomPrettyPrinter extends PrettyPrinter {
  override protected def printObject(kvs: Map[String, JsValue], sb: java.lang.StringBuilder, indent: Int): Unit = {
    sb.append('{').append('\n')
    var first = true
    kvs.foreach { kv =>
      if (first) first = false
      else sb.append(',').append('\n')
      printIndent(sb, indent + Indent)
      printString(kv._1, sb)
      print(kv._2, sb.append(':').append(' '), indent + Indent)
    }
    printIndent(sb.append('\n'), indent)
    sb.append('}')
  }

  override protected def printArray(vs: Seq[JsValue], sb: java.lang.StringBuilder, indent: Int): Unit = {
    sb.append('[').append('\n')
    var first = true
    vs.foreach { v =>
      if (first) first = false
      else sb.append(',').append('\n')
      printIndent(sb, indent + Indent)
      print(v, sb, indent + Indent)
    }
    printIndent(sb.append('\n'), indent)
    sb.append(']')
  }
}

object FlatSprayFormats extends DefaultJsonProtocol with KebsSpray {
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = jsonFormatN
}

object SprayFormats extends DefaultJsonProtocol with KebsSpray.NoFlat {
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /* WARNING: It is an unsafe option for open systems */
  val adtBaseJsonFormat: RootJsonFormat[ADTBase] = {
    implicit lazy val jf1: RootJsonFormat[X] = jsonFormatN
    implicit lazy val jf2: RootJsonFormat[Y] = jsonFormatN
    implicit lazy val jf3: RootJsonFormat[Z] = jsonFormatN
    implicit lazy val jf4: RootJsonFormat[ADTBase] = new RootJsonFormat[ADTBase] {
      override def read(json: JsValue): ADTBase = readADT(json) {
        case "X" => json.convertTo[X]
        case "Y" => json.convertTo[Y]
        case "Z" => json.convertTo[Z]
      }

      override def write(obj: ADTBase): JsValue = writeADT(obj) {
        case x: X => x.toJson
        case y: Y => y.toJson
        case z: Z => z.toJson
      }
    }
    jf4
  }
  implicit val durationJsonFormat: RootJsonFormat[Duration] = stringJsonFormat(Duration.parse)
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormatN
  val geoJSONJsonFormat: RootJsonFormat[GeoJSON.GeoJSON] = {
    implicit lazy val jf1: RootJsonFormat[GeoJSON.Point] = jsonFormatN
    implicit lazy val jf2: RootJsonFormat[GeoJSON.MultiPoint] = jsonFormatN
    implicit lazy val jf3: RootJsonFormat[GeoJSON.LineString] = jsonFormatN
    implicit lazy val jf4: RootJsonFormat[GeoJSON.MultiLineString] = jsonFormatN
    implicit lazy val jf5: RootJsonFormat[GeoJSON.Polygon] = jsonFormatN
    implicit lazy val jf6: RootJsonFormat[GeoJSON.MultiPolygon] = jsonFormatN
    implicit lazy val jf7: RootJsonFormat[GeoJSON.SimpleGeometry] = new RootJsonFormat[GeoJSON.SimpleGeometry] {
      override def read(json: JsValue): GeoJSON.SimpleGeometry = readADT(json) {
        case "Point" => json.convertTo[GeoJSON.Point]
        case "MultiPoint" => json.convertTo[GeoJSON.MultiPoint]
        case "LineString" => json.convertTo[GeoJSON.LineString]
        case "MultiLineString" => json.convertTo[GeoJSON.MultiLineString]
        case "Polygon" => json.convertTo[GeoJSON.Polygon]
        case "MultiPolygon" => json.convertTo[GeoJSON.MultiPolygon]
      }

      override def write(obj: GeoJSON.SimpleGeometry): JsValue = writeADT(obj) {
        case x: GeoJSON.Point => x.toJson
        case x: GeoJSON.MultiPoint => x.toJson
        case x: GeoJSON.LineString => x.toJson
        case x: GeoJSON.MultiLineString => x.toJson
        case x: GeoJSON.Polygon => x.toJson
        case x: GeoJSON.MultiPolygon => x.toJson
      }
    }
    implicit lazy val jf8: RootJsonFormat[GeoJSON.GeometryCollection] = jsonFormatN
    implicit lazy val jf9: RootJsonFormat[GeoJSON.Geometry] = new RootJsonFormat[GeoJSON.Geometry] {
      override def read(json: JsValue): GeoJSON.Geometry = readADT(json) {
        case "Point" => json.convertTo[GeoJSON.Point]
        case "MultiPoint" => json.convertTo[GeoJSON.MultiPoint]
        case "LineString" => json.convertTo[GeoJSON.LineString]
        case "MultiLineString" => json.convertTo[GeoJSON.MultiLineString]
        case "Polygon" => json.convertTo[GeoJSON.Polygon]
        case "MultiPolygon" => json.convertTo[GeoJSON.MultiPolygon]
        case "GeometryCollection" => json.convertTo[GeoJSON.GeometryCollection]
      }

      override def write(obj: GeoJSON.Geometry): JsValue = writeADT(obj) {
        case x: GeoJSON.Point => x.toJson
        case x: GeoJSON.MultiPoint => x.toJson
        case x: GeoJSON.LineString => x.toJson
        case x: GeoJSON.MultiLineString => x.toJson
        case x: GeoJSON.Polygon => x.toJson
        case x: GeoJSON.MultiPolygon => x.toJson
        case x: GeoJSON.GeometryCollection => x.toJson
      }
    }
    implicit lazy val jf10: RootJsonFormat[GeoJSON.Feature] = jsonFormatN
    implicit lazy val jf12: RootJsonFormat[GeoJSON.SimpleGeoJSON] = new RootJsonFormat[GeoJSON.SimpleGeoJSON] {
      override def read(json: JsValue): GeoJSON.SimpleGeoJSON = readADT(json) {
        case "Feature" => json.convertTo[GeoJSON.Feature]
      }

      override def write(obj: GeoJSON.SimpleGeoJSON): JsValue = writeADT(obj) {
        case x: GeoJSON.Feature => x.toJson
      }
    }
    implicit lazy val jf13: RootJsonFormat[GeoJSON.FeatureCollection] = jsonFormatN
    implicit lazy val jf14: RootJsonFormat[GeoJSON.GeoJSON] = new RootJsonFormat[GeoJSON.GeoJSON] {
      override def read(json: JsValue): GeoJSON.GeoJSON = readADT(json) {
        case "Feature" => json.convertTo[GeoJSON.Feature]
        case "FeatureCollection" => json.convertTo[GeoJSON.FeatureCollection]
      }

      override def write(obj: GeoJSON.GeoJSON): JsValue = writeADT(obj) {
        case x: GeoJSON.Feature => x.toJson
        case y: GeoJSON.FeatureCollection => y.toJson
      }
    }
    jf14
  }
  implicit val gitHubActionsAPIJsonFormat: RootJsonFormat[GitHubActionsAPI.Response] = {
    implicit val jf1: RootJsonFormat[GitHubActionsAPI.Artifact] = new RootJsonFormat[GitHubActionsAPI.Artifact] {
      override def read(json: JsValue): GitHubActionsAPI.Artifact = {
        val x = json.asJsObject
        new GitHubActionsAPI.Artifact(
          x.fields("id").convertTo[Int],
          x.fields("node_id").convertTo[String],
          x.fields("name").convertTo[String],
          x.fields("size_in_bytes").convertTo[Int],
          x.fields("url").convertTo[String],
          x.fields("archive_download_url").convertTo[String],
          x.fields("expired").convertTo[String].toBoolean,
          Instant.parse(x.fields("created_at").convertTo[String]),
          Instant.parse(x.fields("expires_at").convertTo[String])
        )
      }

      override def write(obj: GitHubActionsAPI.Artifact): JsValue = JsObject(
        "id" -> JsNumber(obj.id),
        "node_id" -> JsString(obj.node_id),
        "name" -> JsString(obj.name),
        "size_in_bytes" -> JsNumber(obj.size_in_bytes),
        "url" -> JsString(obj.url),
        "archive_download_url" -> JsString(obj.archive_download_url),
        "expired" -> JsString(obj.expired.toString),
        "created_at" -> JsString(obj.created_at.toString),
        "expires_at" -> JsString(obj.expires_at.toString))
    }
    jsonFormatN
  }
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[GoogleMapsAPI.DistanceMatrix] = jsonFormatN
  implicit val instantJsonFormat: RootJsonFormat[Instant] = stringJsonFormat(Instant.parse)
  implicit val localDateJsonFormat: RootJsonFormat[LocalDate] = stringJsonFormat(LocalDate.parse)
  implicit val localDateTimeJsonFormat: RootJsonFormat[LocalDateTime] = stringJsonFormat(LocalDateTime.parse)
  implicit val localTimeJsonFormat: RootJsonFormat[LocalTime] = stringJsonFormat(LocalTime.parse)
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingRequiredFields] = jsonFormatN
  implicit val monthDayJsonFormat: RootJsonFormat[MonthDay] = stringJsonFormat(MonthDay.parse)
  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormatRec
  implicit val offsetDateTimeJsonFormat: RootJsonFormat[OffsetDateTime] = stringJsonFormat(OffsetDateTime.parse)
  implicit val offsetTimeJsonFormat: RootJsonFormat[OffsetTime] = stringJsonFormat(OffsetTime.parse)
  implicit val periodJsonFormat: RootJsonFormat[Period] = stringJsonFormat(Period.parse)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormatN
  implicit val suitEnumADTJsonFormat: RootJsonFormat[SuitADT] = stringJsonFormat {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => suite.getOrElse(s, throw new IllegalArgumentException("SuitADT"))
  }
  implicit val suitEnumJsonFormat: RootJsonFormat[SuitEnum] = EnumJsonFormat(SuitEnum)
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = stringJsonFormat(Suit.valueOf)
  implicit val tweetJsonFormat: RootJsonFormat[TwitterAPI.Tweet] = jsonFormatN
  implicit val bidRequestJsonFormat: RootJsonFormat[OpenRTB.BidRequest] = jsonFormatN
  implicit val uuidJsonFormat: RootJsonFormat[UUID] = stringJsonFormat(UUID.fromString)
  implicit val yearMonthJsonFormat: RootJsonFormat[YearMonth] = stringJsonFormat(YearMonth.parse)
  implicit val yearJsonFormat: RootJsonFormat[Year] = stringJsonFormat(Year.parse)
  implicit val zonedDateTimeJsonFormat: RootJsonFormat[ZonedDateTime] = stringJsonFormat(ZonedDateTime.parse)
  implicit val zoneIdJsonFormat: RootJsonFormat[ZoneId] = stringJsonFormat(ZoneId.of)
  implicit val zoneOffsetJsonFormat: RootJsonFormat[ZoneOffset] = stringJsonFormat(ZoneOffset.of)

  // Based on the Cat/Dog sample: https://gist.github.com/jrudolph/f2d0825aac74ed81c92a
  def readADT[T](json: JsValue)(pf: PartialFunction[String, T]): T = {
    val t = json.asJsObject.fields("type")
    if (!t.isInstanceOf[JsString]) deserializationError(s"Expected JSON string, but got $json")
    else {
      val v = t.asInstanceOf[JsString].value
      pf.applyOrElse(v, (x: String) => deserializationError(s"Expected a name of ADT base subclass, but got $x"))
    }
  }

  def writeADT[T <: Product](obj: T)(pf: PartialFunction[T, JsValue]): JsObject =
    new JsObject(pf.applyOrElse(obj, (x: T) => deserializationError(s"Cannot serialize $x"))
      .asJsObject.fields.updated("type", new JsString(obj.productPrefix)))

  def stringJsonFormat[T](construct: String => T): RootJsonFormat[T] = new RootJsonFormat[T] {
    def read(json: JsValue): T =
      if (!json.isInstanceOf[JsString]) deserializationError(s"Expected JSON string, but got $json")
      else {
        val s = json.asInstanceOf[JsString].value
        try construct(s) catch { case NonFatal(e) => deserializationError(s"Illegal value: $json", e) }
      }

    def write(obj: T): JsValue = new JsString(obj.toString)
  }

  implicit def arrayBufferJsonFormat[T : JsonFormat]: RootJsonFormat[mutable.ArrayBuffer[T]] =
    new RootJsonFormat[mutable.ArrayBuffer[T]] {
      def read(json: JsValue): mutable.ArrayBuffer[T] =
        if (!json.isInstanceOf[JsArray]) deserializationError(s"Expected JSON array, but got $json")
        else {
          val es = json.asInstanceOf[JsArray].elements
          val buf = new mutable.ArrayBuffer[T](es.size)
          es.foreach(e => buf += e.convertTo[T])
          buf
        }

      def write(buf: mutable.ArrayBuffer[T]): JsValue = {
        val vs = Vector.newBuilder[JsValue]
        vs.sizeHint(buf.size)
        buf.foreach(x => vs += x.toJson)
        JsArray(vs.result)
      }
    }
}
