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
//  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormatRec
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
  implicit val tweetJsonFormat: RootJsonFormat[TwitterAPI.Tweet] = {
    implicit val jf1: RootJsonFormat[TwitterAPI.Urls] = jsonFormat4(TwitterAPI.Urls)
    implicit val jf2: RootJsonFormat[TwitterAPI.Url] = jsonFormat1(TwitterAPI.Url)
    implicit val jf3: RootJsonFormat[TwitterAPI.UserMentions] = jsonFormat5(TwitterAPI.UserMentions)
    implicit val jf4: RootJsonFormat[TwitterAPI.Entities] = jsonFormat4(TwitterAPI.Entities)
    implicit val jf5: RootJsonFormat[TwitterAPI.UserEntities] = jsonFormat2(TwitterAPI.UserEntities)
    implicit val jf6: RootJsonFormat[TwitterAPI.User] = new RootJsonFormat[TwitterAPI.User] {
      override def read(json: JsValue): TwitterAPI.User = {
        val x = json.asJsObject
        TwitterAPI.User(
          x.fields("id").convertTo[Long],
          x.fields("id_str").convertTo[String],
          x.fields("name").convertTo[String],
          x.fields("screen_name").convertTo[String],
          x.fields("location").convertTo[String],
          x.fields("description").convertTo[String],
          x.fields("url").convertTo[String],
          x.fields("entities").convertTo[TwitterAPI.UserEntities],
          x.fields("protected").convertTo[Boolean],
          x.fields("followers_count").convertTo[Int],
          x.fields("friends_count").convertTo[Int],
          x.fields("listed_count").convertTo[Int],
          x.fields("created_at").convertTo[String],
          x.fields("favourites_count").convertTo[Int],
          x.fields("utc_offset").convertTo[Int],
          x.fields("time_zone").convertTo[String],
          x.fields("geo_enabled").convertTo[Boolean],
          x.fields("verified").convertTo[Boolean],
          x.fields("statuses_count").convertTo[Int],
          x.fields("lang").convertTo[String],
          x.fields("contributors_enabled").convertTo[Boolean],
          x.fields("is_translator").convertTo[Boolean],
          x.fields("is_translation_enabled").convertTo[Boolean],
          x.fields("profile_background_color").convertTo[String],
          x.fields("profile_background_image_url").convertTo[String],
          x.fields("profile_background_image_url_https").convertTo[String],
          x.fields("profile_background_tile").convertTo[Boolean],
          x.fields("profile_image_url").convertTo[String],
          x.fields("profile_image_url_https").convertTo[String],
          x.fields("profile_banner_url").convertTo[String],
          x.fields("profile_link_color").convertTo[String],
          x.fields("profile_sidebar_border_color").convertTo[String],
          x.fields("profile_sidebar_fill_color").convertTo[String],
          x.fields("profile_text_color").convertTo[String],
          x.fields("profile_use_background_image").convertTo[Boolean],
          x.fields("has_extended_profile").convertTo[Boolean],
          x.fields("default_profile").convertTo[Boolean],
          x.fields("default_profile_image").convertTo[Boolean],
          x.fields("following").convertTo[Boolean],
          x.fields("follow_request_sent").convertTo[Boolean],
          x.fields("notifications").convertTo[Boolean],
          x.fields("translator_type").convertTo[String])
      }

      override def write(obj: TwitterAPI.User): JsValue =
        JsObject(
          "id" -> obj.id.toJson,
          "id_str" -> obj.id_str.toJson,
          "name" -> obj.name.toJson,
          "screen_name" -> obj.screen_name.toJson,
          "location" -> obj.location.toJson,
          "description" -> obj.description.toJson,
          "url" -> obj.url.toJson,
          "entities" -> obj.entities.toJson,
          "protected" -> obj.`protected`.toJson,
          "followers_count" -> obj.followers_count.toJson,
          "friends_count" -> obj.friends_count.toJson,
          "listed_count" -> obj.listed_count.toJson,
          "created_at" -> obj.created_at.toJson,
          "favourites_count" -> obj.favourites_count.toJson,
          "utc_offset" -> obj.utc_offset.toJson,
          "time_zone" -> obj.time_zone.toJson,
          "geo_enabled" -> obj.geo_enabled.toJson,
          "verified" -> obj.verified.toJson,
          "statuses_count" -> obj.statuses_count.toJson,
          "lang" -> obj.lang.toJson,
          "contributors_enabled" -> obj.contributors_enabled.toJson,
          "is_translator" -> obj.is_translator.toJson,
          "is_translation_enabled" -> obj.is_translation_enabled.toJson,
          "profile_background_color" -> obj.profile_background_color.toJson,
          "profile_background_image_url" -> obj.profile_background_image_url.toJson,
          "profile_background_image_url_https" -> obj.profile_background_image_url_https.toJson,
          "profile_background_tile" -> obj.profile_background_tile.toJson,
          "profile_image_url" -> obj.profile_image_url.toJson,
          "profile_image_url_https" -> obj.profile_image_url_https.toJson,
          "profile_banner_url" -> obj.profile_banner_url.toJson,
          "profile_link_color" -> obj.profile_link_color.toJson,
          "profile_sidebar_border_color" -> obj.profile_sidebar_border_color.toJson,
          "profile_sidebar_fill_color" -> obj.profile_sidebar_fill_color.toJson,
          "profile_text_color" -> obj.profile_text_color.toJson,
          "profile_use_background_image" -> obj.profile_use_background_image.toJson,
          "has_extended_profile" -> obj.has_extended_profile.toJson,
          "default_profile" -> obj.default_profile.toJson,
          "default_profile_image" -> obj.default_profile_image.toJson,
          "following" -> obj.following.toJson,
          "follow_request_sent" -> obj.follow_request_sent.toJson,
          "notifications" -> obj.notifications.toJson,
          "translator_type" -> obj.translator_type.toJson)
    }
    implicit val jf7: RootJsonFormat[TwitterAPI.RetweetedStatus] = new RootJsonFormat[TwitterAPI.RetweetedStatus] {
      override def read(json: JsValue): TwitterAPI.RetweetedStatus = {
        val x = json.asJsObject
        TwitterAPI.RetweetedStatus(
          x.fields("created_at").convertTo[String],
          x.fields("id").convertTo[Long],
          x.fields("id_str").convertTo[String],
          x.fields("text").convertTo[String],
          x.fields("truncated").convertTo[Boolean],
          x.fields("entities").convertTo[TwitterAPI.Entities],
          x.fields("source").convertTo[String],
          x.fields("in_reply_to_status_id").convertTo[Option[String]],
          x.fields("in_reply_to_status_id_str").convertTo[Option[String]],
          x.fields("in_reply_to_user_id").convertTo[Option[String]],
          x.fields("in_reply_to_user_id_str").convertTo[Option[String]],
          x.fields("in_reply_to_screen_name").convertTo[Option[String]],
          x.fields("user").convertTo[TwitterAPI.User],
          x.fields("geo").convertTo[Option[String]],
          x.fields("coordinates").convertTo[Option[String]],
          x.fields("place").convertTo[Option[String]],
          x.fields("contributors").convertTo[Option[String]],
          x.fields("is_quote_status").convertTo[Boolean],
          x.fields("retweet_count").convertTo[Int],
          x.fields("favorite_count").convertTo[Int],
          x.fields("favorited").convertTo[Boolean],
          x.fields("retweeted").convertTo[Boolean],
          x.fields("possibly_sensitive").convertTo[Boolean],
          x.fields("lang").convertTo[String])
      }

      override def write(obj: TwitterAPI.RetweetedStatus): JsValue =
        JsObject(
          "created_at" -> obj.created_at.toJson,
          "id" -> obj.id.toJson,
          "id_str" -> obj.id_str.toJson,
          "text" -> obj.text.toJson,
          "truncated" -> obj.truncated.toJson,
          "entities" -> obj.entities.toJson,
          "source" -> obj.source.toJson,
          "in_reply_to_status_id" -> obj.in_reply_to_status_id.toJson,
          "in_reply_to_status_id_str" -> obj.in_reply_to_status_id_str.toJson,
          "in_reply_to_user_id" -> obj.in_reply_to_user_id.toJson,
          "in_reply_to_user_id_str" -> obj.in_reply_to_user_id_str.toJson,
          "in_reply_to_screen_name" -> obj.in_reply_to_screen_name.toJson,
          "user" -> obj.user.toJson,
          "geo" -> obj.geo.toJson,
          "coordinates" -> obj.coordinates.toJson,
          "place" -> obj.place.toJson,
          "contributors" -> obj.contributors.toJson,
          "is_quote_status" -> obj.is_quote_status.toJson,
          "retweet_count" -> obj.retweet_count.toJson,
          "favorite_count" -> obj.favorite_count.toJson,
          "favorited" -> obj.favorited.toJson,
          "retweeted" -> obj.retweeted.toJson,
          "possibly_sensitive" -> obj.possibly_sensitive.toJson,
          "lang" -> obj.lang.toJson)
    }
    implicit val jf8: RootJsonFormat[TwitterAPI.Tweet] = new RootJsonFormat[TwitterAPI.Tweet] {
      override def read(json: JsValue): TwitterAPI.Tweet = {
        val x = json.asJsObject
        TwitterAPI.Tweet(
          x.fields("created_at").convertTo[String],
          x.fields("id").convertTo[Long],
          x.fields("id_str").convertTo[String],
          x.fields("text").convertTo[String],
          x.fields("truncated").convertTo[Boolean],
          x.fields("entities").convertTo[TwitterAPI.Entities],
          x.fields("source").convertTo[String],
          x.fields("in_reply_to_status_id").convertTo[Option[String]],
          x.fields("in_reply_to_status_id_str").convertTo[Option[String]],
          x.fields("in_reply_to_user_id").convertTo[Option[String]],
          x.fields("in_reply_to_user_id_str").convertTo[Option[String]],
          x.fields("in_reply_to_screen_name").convertTo[Option[String]],
          x.fields("user").convertTo[TwitterAPI.User],
          x.fields("geo").convertTo[Option[String]],
          x.fields("coordinates").convertTo[Option[String]],
          x.fields("place").convertTo[Option[String]],
          x.fields("contributors").convertTo[Option[String]],
          x.fields("retweeted_status").convertTo[TwitterAPI.RetweetedStatus],
          x.fields("is_quote_status").convertTo[Boolean],
          x.fields("retweet_count").convertTo[Int],
          x.fields("favorite_count").convertTo[Int],
          x.fields("favorited").convertTo[Boolean],
          x.fields("retweeted").convertTo[Boolean],
          x.fields("possibly_sensitive").convertTo[Boolean],
          x.fields("lang").convertTo[String])
      }

      override def write(obj: TwitterAPI.Tweet): JsValue =
        JsObject(
          "created_at" -> obj.created_at.toJson,
          "id" -> obj.id.toJson,
          "id_str" -> obj.id_str.toJson,
          "text" -> obj.text.toJson,
          "truncated" -> obj.truncated.toJson,
          "entities" -> obj.entities.toJson,
          "source" -> obj.source.toJson,
          "in_reply_to_status_id" -> obj.in_reply_to_status_id.toJson,
          "in_reply_to_status_id_str" -> obj.in_reply_to_status_id_str.toJson,
          "in_reply_to_user_id" -> obj.in_reply_to_user_id.toJson,
          "in_reply_to_user_id_str" -> obj.in_reply_to_user_id_str.toJson,
          "in_reply_to_screen_name" -> obj.in_reply_to_screen_name.toJson,
          "user" -> obj.user.toJson,
          "geo" -> obj.geo.toJson,
          "coordinates" -> obj.coordinates.toJson,
          "place" -> obj.place.toJson,
          "contributors" -> obj.contributors.toJson,
          "is_quote_status" -> obj.is_quote_status.toJson,
          "retweet_count" -> obj.retweet_count.toJson,
          "favorite_count" -> obj.favorite_count.toJson,
          "favorited" -> obj.favorited.toJson,
          "retweeted" -> obj.retweeted.toJson,
          "possibly_sensitive" -> obj.possibly_sensitive.toJson,
          "lang" -> obj.lang.toJson)
    }
    jf8
  }
//  implicit val bidRequestJsonFormat: RootJsonFormat[OpenRTB.BidRequest] = jsonFormatN
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
