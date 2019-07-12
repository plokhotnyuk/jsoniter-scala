package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import java.util.UUID
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.util.control.NonFatal
import spray.json._

// Based on the code found: https://github.com/spray/spray-json/issues/200
case class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  override def read(json: JsValue): T#Value =
    e.values.iterator.find { ev =>
      json.isInstanceOf[JsString] && json.asInstanceOf[JsString].value == ev.toString
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

object SprayFormats extends DefaultJsonProtocol /*with KebsSpray.NoFlat*/ {
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /*WARNING: don't do this for open-systems*/
  val adtBaseJsonFormat: RootJsonFormat[ADTBase] = {
    implicit lazy val jf1: RootJsonFormat[X] = jsonFormat1(X)
    implicit lazy val jf2: RootJsonFormat[Y] = jsonFormat1(Y)
    implicit lazy val jf3: RootJsonFormat[Z] = jsonFormat2(Z)
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
  implicit val anyRefsJsonFormat: RootJsonFormat[AnyRefs] = jsonFormat3(AnyRefs)
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = {
    // Based on the following "horrible hack": https://github.com/spray/spray-json/issues/38#issuecomment-11708058
    case class AnyValJsonFormat[T <: AnyVal{def a : V}, V](construct: V => T)(implicit jf: JsonFormat[V]) extends JsonFormat[T] {
      import scala.language.reflectiveCalls

      override def read(json: JsValue): T = construct(jf.read(json))

      override def write(obj: T): JsValue = jf.write(obj.a)
    }

    implicit val jf1: JsonFormat[ByteVal] = AnyValJsonFormat(ByteVal)
    implicit val jf2: JsonFormat[ShortVal] = AnyValJsonFormat(ShortVal)
    implicit val jf3: JsonFormat[IntVal] = AnyValJsonFormat(IntVal)
    implicit val jf4: JsonFormat[LongVal] = AnyValJsonFormat(LongVal)
    implicit val jf5: JsonFormat[BooleanVal] = AnyValJsonFormat(BooleanVal)
    implicit val jf6: JsonFormat[CharVal] = AnyValJsonFormat(CharVal)
    implicit val jf7: JsonFormat[DoubleVal] = AnyValJsonFormat(DoubleVal)
    implicit val jf8: JsonFormat[FloatVal] = AnyValJsonFormat(FloatVal)
    jsonFormat8(AnyVals)
  }
  implicit val durationJsonFormat: RootJsonFormat[Duration] = stringJsonFormat(Duration.parse)
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormat2(ExtractFields)
  val geoJSONJsonFormat: RootJsonFormat[GeoJSON.GeoJSON] = {
    implicit lazy val jf1: RootJsonFormat[GeoJSON.Point] = jsonFormat1(GeoJSON.Point)
    implicit lazy val jf2: RootJsonFormat[GeoJSON.MultiPoint] = jsonFormat1(GeoJSON.MultiPoint)
    implicit lazy val jf3: RootJsonFormat[GeoJSON.LineString] = jsonFormat1(GeoJSON.LineString)
    implicit lazy val jf4: RootJsonFormat[GeoJSON.MultiLineString] = jsonFormat1(GeoJSON.MultiLineString)
    implicit lazy val jf5: RootJsonFormat[GeoJSON.Polygon] = jsonFormat1(GeoJSON.Polygon)
    implicit lazy val jf6: RootJsonFormat[GeoJSON.MultiPolygon] = jsonFormat1(GeoJSON.MultiPolygon)
    implicit lazy val jf7: RootJsonFormat[GeoJSON.GeometryCollection] = jsonFormat1(GeoJSON.GeometryCollection)
    implicit lazy val jf8: RootJsonFormat[GeoJSON.Geometry] = new RootJsonFormat[GeoJSON.Geometry] {
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
    implicit lazy val jf9: RootJsonFormat[GeoJSON.Feature] = jsonFormat3(GeoJSON.Feature)
    implicit lazy val jf10: RootJsonFormat[GeoJSON.FeatureCollection] = jsonFormat2(GeoJSON.FeatureCollection)
    implicit lazy val jf11: RootJsonFormat[GeoJSON.GeoJSON] = new RootJsonFormat[GeoJSON.GeoJSON] {
      override def read(json: JsValue): GeoJSON.GeoJSON = readADT(json) {
        case "Feature" => json.convertTo[GeoJSON.Feature]
        case "FeatureCollection" => json.convertTo[GeoJSON.FeatureCollection]
      }

      override def write(obj: GeoJSON.GeoJSON): JsValue = writeADT(obj) {
        case x: GeoJSON.Feature => x.toJson
        case y: GeoJSON.FeatureCollection => y.toJson
      }
    }
    jf11
  }
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[GoogleMapsAPI.DistanceMatrix] = {
    implicit val jf1: RootJsonFormat[GoogleMapsAPI.Value] = jsonFormat2(GoogleMapsAPI.Value)
    implicit val jf2: RootJsonFormat[GoogleMapsAPI.Elements] = jsonFormat3(GoogleMapsAPI.Elements)
    implicit val jf3: RootJsonFormat[GoogleMapsAPI.Rows] = jsonFormat1(GoogleMapsAPI.Rows)
    jsonFormat4(GoogleMapsAPI.DistanceMatrix)
  }
  implicit val instantJsonFormat: RootJsonFormat[Instant] = stringJsonFormat(Instant.parse)
  implicit val localDateJsonFormat: RootJsonFormat[LocalDate] = stringJsonFormat(LocalDate.parse)
  implicit val localDateTimeJsonFormat: RootJsonFormat[LocalDateTime] = stringJsonFormat(LocalDateTime.parse)
  implicit val localTimeJsonFormat: RootJsonFormat[LocalTime] = stringJsonFormat(LocalTime.parse)
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingRequiredFields] = jsonFormat2(MissingRequiredFields)
  implicit val monthDayJsonFormat: RootJsonFormat[MonthDay] = stringJsonFormat(MonthDay.parse)
  implicit val offsetDateTimeJsonFormat: RootJsonFormat[OffsetDateTime] = stringJsonFormat(OffsetDateTime.parse)
  implicit val offsetTimeJsonFormat: RootJsonFormat[OffsetTime] = stringJsonFormat(OffsetTime.parse)
  implicit val periodJsonFormat: RootJsonFormat[Period] = stringJsonFormat(Period.parse)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormat8(Primitives)
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
