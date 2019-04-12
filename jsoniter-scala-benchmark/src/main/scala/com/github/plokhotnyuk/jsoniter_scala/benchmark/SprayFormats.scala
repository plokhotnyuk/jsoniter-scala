package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import pl.iterators.kebs.json.KebsSpray
import spray.json._

import scala.collection.immutable.Map
import scala.collection.mutable
import scala.util.control.NonFatal

// Based on the code found: https://github.com/spray/spray-json/issues/200
case class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  override def read(json: JsValue): T#Value =
    e.values.iterator.find { ev =>
      json.isInstanceOf[JsString] && json.asInstanceOf[JsString].value == ev.toString
    }.getOrElse(deserializationError(s"Expected JSON string of value from enum $e, but got $json"))

  override def write(ev: T#Value): JsValue = new JsString(ev.toString)
}

object CustomPrettyPrinter extends PrettyPrinter {
  override protected def printArray(vs: Seq[JsValue], sb: java.lang.StringBuilder, indent: Int): Unit = {
    sb.append("[\n")
    printSeq(vs, sb.append(",\n")){ v =>
      printIndent(sb, indent + Indent)
      print(v, sb, indent + Indent)
    }
    sb.append('\n')
    printIndent(sb, indent)
    sb.append(']')
  }
}

object FlatSprayFormats extends DefaultJsonProtocol with KebsSpray {
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = jsonFormatN
}

object SprayFormats extends DefaultJsonProtocol with KebsSpray.NoFlat {
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /*WARNING: don't do this for open-systems*/
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
  implicit val anyRefsJsonFormat: RootJsonFormat[AnyRefs] = jsonFormatN
  implicit val durationJsonFormat: RootJsonFormat[Duration] = stringJsonFormat(Duration.parse)
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormatN
  val geoJSONJsonFormat: RootJsonFormat[GeoJSON] = {
    implicit lazy val jf1: RootJsonFormat[Point] = jsonFormatN
    implicit lazy val jf2: RootJsonFormat[MultiPoint] = jsonFormatN
    implicit lazy val jf3: RootJsonFormat[LineString] = jsonFormatN
    implicit lazy val jf4: RootJsonFormat[MultiLineString] = jsonFormatN
    implicit lazy val jf5: RootJsonFormat[Polygon] = jsonFormatN
    implicit lazy val jf6: RootJsonFormat[MultiPolygon] = jsonFormatN
    implicit lazy val jf7: RootJsonFormat[GeometryCollection] = jsonFormatN
    implicit lazy val jf8: RootJsonFormat[Geometry] = new RootJsonFormat[Geometry] {
      override def read(json: JsValue): Geometry = readADT(json) {
        case "Point" => json.convertTo[Point]
        case "MultiPoint" => json.convertTo[MultiPoint]
        case "LineString" => json.convertTo[LineString]
        case "MultiLineString" => json.convertTo[MultiLineString]
        case "Polygon" => json.convertTo[Polygon]
        case "MultiPolygon" => json.convertTo[MultiPolygon]
        case "GeometryCollection" => json.convertTo[GeometryCollection]
      }

      override def write(obj: Geometry): JsValue = writeADT(obj) {
        case x: Point => x.toJson
        case x: MultiPoint => x.toJson
        case x: LineString => x.toJson
        case x: MultiLineString => x.toJson
        case x: Polygon => x.toJson
        case x: MultiPolygon => x.toJson
        case x: GeometryCollection => x.toJson
      }
    }
    implicit lazy val jf9: RootJsonFormat[Feature] = jsonFormatN
    implicit lazy val jf10: RootJsonFormat[FeatureCollection] = jsonFormatN
    implicit lazy val jf11: RootJsonFormat[GeoJSON] = new RootJsonFormat[GeoJSON] {
      override def read(json: JsValue): GeoJSON = readADT(json) {
        case "Feature" => json.convertTo[Feature]
        case "FeatureCollection" => json.convertTo[FeatureCollection]
      }

      override def write(obj: GeoJSON): JsValue = writeADT(obj) {
        case x: Feature => x.toJson
        case y: FeatureCollection => y.toJson
      }
    }
    jf11
  }
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[DistanceMatrix] = jsonFormatN
  implicit val instantJsonFormat: RootJsonFormat[Instant] = stringJsonFormat(Instant.parse)
  implicit val localDateJsonFormat: RootJsonFormat[LocalDate] = stringJsonFormat(LocalDate.parse)
  implicit val localDateTimeJsonFormat: RootJsonFormat[LocalDateTime] = stringJsonFormat(LocalDateTime.parse)
  implicit val localTimeJsonFormat: RootJsonFormat[LocalTime] = stringJsonFormat(LocalTime.parse)
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingReqFields] = jsonFormatN
  implicit val monthDayJsonFormat: RootJsonFormat[MonthDay] = stringJsonFormat(MonthDay.parse)
  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormatRec
  implicit val offsetDateTimeJsonFormat: RootJsonFormat[OffsetDateTime] = stringJsonFormat(OffsetDateTime.parse)
  implicit val offsetTimeJsonFormat: RootJsonFormat[OffsetTime] = stringJsonFormat(OffsetTime.parse)
  implicit val periodJsonFormat: RootJsonFormat[Period] = stringJsonFormat(Period.parse)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormatN
  implicit val suitEnumADTJsonFormat: RootJsonFormat[SuitADT] = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    stringJsonFormat(suite.apply)
  }
  implicit val suitEnumJsonFormat: RootJsonFormat[SuitEnum] = EnumJsonFormat(SuitEnum)
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = stringJsonFormat(Suit.valueOf)
  implicit val tweetJsonFormat: RootJsonFormat[Tweet] = jsonFormatN
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
