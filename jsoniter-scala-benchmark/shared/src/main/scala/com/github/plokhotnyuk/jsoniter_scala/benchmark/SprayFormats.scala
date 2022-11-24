package com.github.plokhotnyuk.jsoniter_scala.benchmark

import spray.json._
import java.time._
import java.util.concurrent.ConcurrentHashMap
import java.util.{Base64, UUID}
import scala.collection.immutable.{ArraySeq, Map}
import scala.collection.mutable
import scala.reflect.ClassTag

object SprayFormats extends DefaultJsonProtocol {
  val prettyPrinter: PrettyPrinter = new PrettyPrinter {
    override protected def printObject(kvs: Map[String, JsValue], sb: java.lang.StringBuilder, indent: Int): Unit = {
      sb.append('{').append('\n')
      var first = true
      kvs.foreach { kv =>
        if (first) first = false
        else sb.append(',').append('\n')
        printIndent(sb, indent + 2)
        printString(kv._1, sb)
        print(kv._2, sb.append(':').append(' '), indent + 2)
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
        printIndent(sb, indent + 2)
        print(v, sb, indent + 2)
      }
      printIndent(sb.append('\n'), indent)
      sb.append(']')
    }
  }
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = new RootJsonFormat[AnyVals] {
    override def read(json: JsValue): AnyVals = {
      val fields = json.asJsObject.fields
      new AnyVals(
        new ByteVal(fields("b").convertTo[Byte]),
        new ShortVal(fields("s").convertTo[Short]),
        new IntVal(fields("i").convertTo[Int]),
        new LongVal(fields("l").convertTo[Long]),
        new BooleanVal(fields("bl").convertTo[Boolean]),
        new CharVal(fields("ch").convertTo[Char]),
        new DoubleVal(fields("dbl").convertTo[Double]),
        new FloatVal(fields("f").convertTo[Float])
      )
    }

    override def write(x: AnyVals): JsValue = JsObject(
      ("b", JsNumber(x.b.a)),
      ("s", JsNumber(x.s.a)),
      ("i", JsNumber(x.i.a)),
      ("l", JsNumber(x.l.a)),
      ("bl", JsBoolean(x.bl.a)),
      ("ch", JsString(x.ch.a.toString)),
      ("dbl", JsNumber(x.dbl.a)),
      ("f", JsNumber(x.f.a))
    )
  }
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /* WARNING: It is an unsafe option for open systems */
  val adtBaseJsonFormat: RootJsonFormat[ADTBase] = {
    implicit lazy val jf1: RootJsonFormat[X] = jsonFormat1(X.apply)
    implicit lazy val jf2: RootJsonFormat[Y] = jsonFormat1(Y.apply)
    implicit lazy val jf3: RootJsonFormat[Z] = jsonFormat2(Z.apply)
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
  val base64JsonFormat: RootJsonFormat[Array[Byte]] = new RootJsonFormat[Array[Byte]] {
    def read(json: JsValue): Array[Byte] = json match {
      case js: JsString => Base64.getDecoder.decode(js.value)
      case _ => deserializationError(s"Expected JSON string")
    }

    def write(obj: Array[Byte]): JsValue = new JsString(Base64.getEncoder.encodeToString(obj))
  }
  implicit val durationJsonFormat: RootJsonFormat[Duration] = stringJsonFormat(Duration.parse)
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormat2(ExtractFields.apply)
  val geoJSONJsonFormat: RootJsonFormat[GeoJSON.GeoJSON] = {
    implicit lazy val jf1: RootJsonFormat[GeoJSON.Point] = jsonFormat1(GeoJSON.Point.apply)
    implicit lazy val jf2: RootJsonFormat[GeoJSON.MultiPoint] = jsonFormat1(GeoJSON.MultiPoint.apply)
    implicit lazy val jf3: RootJsonFormat[GeoJSON.LineString] = jsonFormat1(GeoJSON.LineString.apply)
    implicit lazy val jf4: RootJsonFormat[GeoJSON.MultiLineString] = jsonFormat1(GeoJSON.MultiLineString.apply)
    implicit lazy val jf5: RootJsonFormat[GeoJSON.Polygon] = jsonFormat1(GeoJSON.Polygon.apply)
    implicit lazy val jf6: RootJsonFormat[GeoJSON.MultiPolygon] = jsonFormat1(GeoJSON.MultiPolygon.apply)
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
    implicit lazy val jf8: RootJsonFormat[GeoJSON.GeometryCollection] = jsonFormat1(GeoJSON.GeometryCollection.apply)
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
    implicit lazy val jf10: RootJsonFormat[GeoJSON.Feature] = jsonFormat3(GeoJSON.Feature.apply)
    implicit lazy val jf12: RootJsonFormat[GeoJSON.SimpleGeoJSON] = new RootJsonFormat[GeoJSON.SimpleGeoJSON] {
      override def read(json: JsValue): GeoJSON.SimpleGeoJSON = readADT(json) {
        case "Feature" => json.convertTo[GeoJSON.Feature]
      }

      override def write(obj: GeoJSON.SimpleGeoJSON): JsValue = writeADT(obj) {
        case x: GeoJSON.Feature => x.toJson
      }
    }
    implicit lazy val jf13: RootJsonFormat[GeoJSON.FeatureCollection] = jsonFormat2(GeoJSON.FeatureCollection.apply)
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
        val fields = json.asJsObject.fields
        new GitHubActionsAPI.Artifact(
          fields("id").convertTo[Int],
          fields("node_id").convertTo[String],
          fields("name").convertTo[String],
          fields("size_in_bytes").convertTo[Int],
          fields("url").convertTo[String],
          fields("archive_download_url").convertTo[String],
          fields("expired").convertTo[String].toBoolean,
          Instant.parse(fields("created_at").convertTo[String]),
          Instant.parse(fields("expires_at").convertTo[String])
        )
      }

      override def write(x: GitHubActionsAPI.Artifact): JsValue = JsObject(
        ("id", JsNumber(x.id)),
        ("node_id", JsString(x.node_id)),
        ("name", JsString(x.name)),
        ("size_in_bytes", JsNumber(x.size_in_bytes)),
        ("url", JsString(x.url)),
        ("archive_download_url", JsString(x.archive_download_url)),
        ("expired", JsString(x.expired.toString)),
        ("created_at", JsString(x.created_at.toString)),
        ("expires_at", JsString(x.expires_at.toString)))
    }
    jsonFormat2(GitHubActionsAPI.Response.apply)
  }
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[GoogleMapsAPI.DistanceMatrix] = {
    implicit val jf1: RootJsonFormat[GoogleMapsAPI.Value] = jsonFormat2(GoogleMapsAPI.Value.apply)
    implicit val jf2: RootJsonFormat[GoogleMapsAPI.Elements] = jsonFormat3(GoogleMapsAPI.Elements.apply)
    implicit val jf3: RootJsonFormat[GoogleMapsAPI.Rows] = jsonFormat1(GoogleMapsAPI.Rows.apply)
    jsonFormat4(GoogleMapsAPI.DistanceMatrix.apply)
  }
  implicit val instantJsonFormat: RootJsonFormat[Instant] = stringJsonFormat(Instant.parse)
  implicit val localDateJsonFormat: RootJsonFormat[LocalDate] = stringJsonFormat(LocalDate.parse)
  implicit val localDateTimeJsonFormat: RootJsonFormat[LocalDateTime] = stringJsonFormat(LocalDateTime.parse)
  implicit val localTimeJsonFormat: RootJsonFormat[LocalTime] = stringJsonFormat(LocalTime.parse)
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingRequiredFields] = jsonFormat2(MissingRequiredFields.apply)
  implicit val monthDayJsonFormat: RootJsonFormat[MonthDay] = stringJsonFormat(MonthDay.parse)
  implicit lazy val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = rootFormat(lazyFormat(jsonFormat1(NestedStructs.apply)))
  implicit val offsetDateTimeJsonFormat: RootJsonFormat[OffsetDateTime] = stringJsonFormat(OffsetDateTime.parse)
  implicit val offsetTimeJsonFormat: RootJsonFormat[OffsetTime] = stringJsonFormat(OffsetTime.parse)
  implicit val periodJsonFormat: RootJsonFormat[Period] = stringJsonFormat(Period.parse)
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormat8(Primitives.apply)
  implicit val suitEnumADTJsonFormat: RootJsonFormat[SuitADT] = stringJsonFormat {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    s => suite.getOrElse(s, throw new IllegalArgumentException("SuitADT"))
  }
  implicit val suitEnumJsonFormat: RootJsonFormat[SuitEnum.SuitEnum] = new RootJsonFormat[SuitEnum.SuitEnum] {
    private[this] val ec = new ConcurrentHashMap[String, SuitEnum.SuitEnum]

    override def read(json: JsValue): SuitEnum.SuitEnum = {
      var x: SuitEnum.SuitEnum = null
      json match {
        case js: JsString =>
          val s = js.value
          x = ec.get(s)
          if (x eq null) {
            x = SuitEnum.values.iterator.find(_.toString == s).orNull
            ec.put(s, x)
          }
        case _ =>
      }
      if (x eq null) deserializationError(s"Expected JSON string of value from SuitEnum")
      x
    }

    override def write(ev: SuitEnum.SuitEnum): JsValue = new JsString(ev.toString)
  }
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = stringJsonFormat(Suit.valueOf)
  implicit val tweetJsonFormat: RootJsonFormat[TwitterAPI.Tweet] = {
    implicit val jf1: RootJsonFormat[TwitterAPI.Urls] = new RootJsonFormat[TwitterAPI.Urls] {
      override def read(json: JsValue): TwitterAPI.Urls = {
        val fields = json.asJsObject.fields
        new TwitterAPI.Urls(
          fields("url").convertTo[String],
          fields("expanded_url").convertTo[String],
          fields("display_url").convertTo[String],
          fields("indices").convertTo[Seq[Int]]
        )
      }

      override def write(x: TwitterAPI.Urls): JsValue = toJsObject(
        ("url", JsString(x.url)),
        ("expanded_url", JsString(x.expanded_url)),
        ("display_url", JsString(x.display_url)),
        ("indices", x.indices.toJson)
      )
    }
    implicit val jf2: RootJsonFormat[TwitterAPI.Url] = new RootJsonFormat[TwitterAPI.Url] {
      override def read(json: JsValue): TwitterAPI.Url =
        new TwitterAPI.Url(json.asJsObject.fields("urls").convertTo[Seq[TwitterAPI.Urls]])

      override def write(x: TwitterAPI.Url): JsValue = toJsObject(("urls", x.urls.toJson))
    }
    implicit val jf3: RootJsonFormat[TwitterAPI.UserEntities] = new RootJsonFormat[TwitterAPI.UserEntities] {
      override def read(json: JsValue): TwitterAPI.UserEntities = {
        val fields = json.asJsObject.fields
        new TwitterAPI.UserEntities(
          fields("url").convertTo[TwitterAPI.Url],
          fields("description").convertTo[TwitterAPI.Url]
        )
      }
      override def write(x: TwitterAPI.UserEntities): JsValue = toJsObject(
        ("url", x.url.toJson),
        ("description", x.description.toJson)
      )
    }
    implicit val jf4: RootJsonFormat[TwitterAPI.User] = new RootJsonFormat[TwitterAPI.User] {
      override def read(json: JsValue): TwitterAPI.User = {
        val fields = json.asJsObject.fields
        new TwitterAPI.User(
          fields("id").convertTo[Long],
          fields("id_str").convertTo[String],
          fields("name").convertTo[String],
          fields("screen_name").convertTo[String],
          fields("location").convertTo[String],
          fields("description").convertTo[String],
          fields("url").convertTo[String],
          fields("entities").convertTo[TwitterAPI.UserEntities],
          fields("protected").convertTo[Boolean],
          fields("followers_count").convertTo[Int],
          fields("friends_count").convertTo[Int],
          fields("listed_count").convertTo[Int],
          fields("created_at").convertTo[String],
          fields("favourites_count").convertTo[Int],
          fields("utc_offset").convertTo[Int],
          fields("time_zone").convertTo[String],
          fields("geo_enabled").convertTo[Boolean],
          fields("verified").convertTo[Boolean],
          fields("statuses_count").convertTo[Int],
          fields("lang").convertTo[String],
          fields("contributors_enabled").convertTo[Boolean],
          fields("is_translator").convertTo[Boolean],
          fields("is_translation_enabled").convertTo[Boolean],
          fields("profile_background_color").convertTo[String],
          fields("profile_background_image_url").convertTo[String],
          fields("profile_background_image_url_https").convertTo[String],
          fields("profile_background_tile").convertTo[Boolean],
          fields("profile_image_url").convertTo[String],
          fields("profile_image_url_https").convertTo[String],
          fields("profile_banner_url").convertTo[String],
          fields("profile_link_color").convertTo[String],
          fields("profile_sidebar_border_color").convertTo[String],
          fields("profile_sidebar_fill_color").convertTo[String],
          fields("profile_text_color").convertTo[String],
          fields("profile_use_background_image").convertTo[Boolean],
          fields("has_extended_profile").convertTo[Boolean],
          fields("default_profile").convertTo[Boolean],
          fields("default_profile_image").convertTo[Boolean],
          fields("following").convertTo[Boolean],
          fields("follow_request_sent").convertTo[Boolean],
          fields("notifications").convertTo[Boolean],
          fields("translator_type").convertTo[String]
        )
      }

      override def write(x: TwitterAPI.User): JsValue = toJsObject(
        ("id", JsNumber(x.id)),
        ("id_str", JsString(x.id_str)),
        ("name", JsString(x.name)),
        ("screen_name", JsString(x.screen_name)),
        ("location", JsString(x.location)),
        ("description", JsString(x.description)),
        ("url", JsString(x.url)),
        ("entities", x.entities.toJson),
        ("protected", JsBoolean(x.`protected`)),
        ("followers_count", JsNumber(x.followers_count)),
        ("friends_count", JsNumber(x.friends_count)),
        ("listed_count", JsNumber(x.listed_count)),
        ("created_at", JsString(x.created_at)),
        ("favourites_count", JsNumber(x.favourites_count)),
        ("utc_offset", JsNumber(x.utc_offset)),
        ("time_zone", JsString(x.time_zone)),
        ("geo_enabled", JsBoolean(x.geo_enabled)),
        ("verified", JsBoolean(x.verified)),
        ("statuses_count", JsNumber(x.statuses_count)),
        ("lang", JsString(x.lang)),
        ("contributors_enabled", JsBoolean(x.contributors_enabled)),
        ("is_translator", JsBoolean(x.is_translator)),
        ("is_translation_enabled", JsBoolean(x.is_translation_enabled)),
        ("profile_background_color", JsString(x.profile_background_color)),
        ("profile_background_image_url", JsString(x.profile_background_image_url)),
        ("profile_background_image_url_https", JsString(x.profile_background_image_url_https)),
        ("profile_background_tile", JsBoolean(x.profile_background_tile)),
        ("profile_image_url", JsString(x.profile_image_url)),
        ("profile_image_url_https", JsString(x.profile_image_url_https)),
        ("profile_banner_url", JsString(x.profile_banner_url)),
        ("profile_link_color", JsString(x.profile_link_color)),
        ("profile_sidebar_border_color", JsString(x.profile_sidebar_border_color)),
        ("profile_sidebar_fill_color", JsString(x.profile_sidebar_fill_color)),
        ("profile_text_color", JsString(x.profile_text_color)),
        ("profile_use_background_image", JsBoolean(x.profile_use_background_image)),
        ("has_extended_profile", JsBoolean(x.has_extended_profile)),
        ("default_profile", JsBoolean(x.default_profile)),
        ("default_profile_image", JsBoolean(x.default_profile_image)),
        ("following", JsBoolean(x.following)),
        ("follow_request_sent", JsBoolean(x.follow_request_sent)),
        ("notifications", JsBoolean(x.notifications)),
        ("translator_type", JsString(x.translator_type))
      )
    }
    implicit val jf5: RootJsonFormat[TwitterAPI.UserMentions] = new RootJsonFormat[TwitterAPI.UserMentions] {
      override def read(json: JsValue): TwitterAPI.UserMentions = {
        val fields = json.asJsObject.fields
        new TwitterAPI.UserMentions(
          fields("screen_name").convertTo[String],
          fields("name").convertTo[String],
          fields("id").convertTo[Long],
          fields("id_str").convertTo[String],
          fields.get("indices").fold[Seq[Int]](Nil)(_.convertTo[Seq[Int]])
        )
      }

      override def write(x: TwitterAPI.UserMentions): JsValue = toJsObject(
        ("screen_name", JsString(x.screen_name)),
        ("name", JsString(x.name)),
        ("id", JsNumber(x.id)),
        ("id_str", JsString(x.id_str)),
        ("indices", x.indices.toJson)
      )
    }
    implicit val jf6: RootJsonFormat[TwitterAPI.Entities] = new RootJsonFormat[TwitterAPI.Entities] {
      override def read(json: JsValue): TwitterAPI.Entities = {
        val fields = json.asJsObject.fields
        new TwitterAPI.Entities(
          fields.get("hashtags").fold[Seq[String]](Nil)(_.convertTo[Seq[String]]),
          fields.get("symbols").fold[Seq[String]](Nil)(_.convertTo[Seq[String]]),
          fields.get("user_mentions").fold[Seq[TwitterAPI.UserMentions]](Nil)(_.convertTo[Seq[TwitterAPI.UserMentions]]),
          fields.get("urls").fold[Seq[TwitterAPI.Urls]](Nil)(_.convertTo[Seq[TwitterAPI.Urls]])
        )
      }

      override def write(x: TwitterAPI.Entities): JsValue = toJsObject(
        ("hashtags", x.hashtags.toJson),
        ("symbols", x.symbols.toJson),
        ("user_mentions", x.user_mentions.toJson),
        ("urls", x.urls.toJson)
      )
    }
    implicit val jf7: RootJsonFormat[TwitterAPI.RetweetedStatus] = new RootJsonFormat[TwitterAPI.RetweetedStatus] {
      override def read(json: JsValue): TwitterAPI.RetweetedStatus = {
        val fields = json.asJsObject.fields
        new TwitterAPI.RetweetedStatus(
          fields("created_at").convertTo[String],
          fields("id").convertTo[Long],
          fields("id_str").convertTo[String],
          fields("text").convertTo[String],
          fields("truncated").convertTo[Boolean],
          fields("entities").convertTo[TwitterAPI.Entities],
          fields("source").convertTo[String],
          fields.get("in_reply_to_status_id").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_status_id_str").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_user_id").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_user_id_str").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_screen_name").flatMap(_.convertTo[Option[String]]),
          fields("user").convertTo[TwitterAPI.User],
          fields.get("geo").flatMap(_.convertTo[Option[String]]),
          fields.get("coordinates").flatMap(_.convertTo[Option[String]]),
          fields.get("place").flatMap(_.convertTo[Option[String]]),
          fields.get("contributors").flatMap(_.convertTo[Option[String]]),
          fields("is_quote_status").convertTo[Boolean],
          fields("retweet_count").convertTo[Int],
          fields("favorite_count").convertTo[Int],
          fields("favorited").convertTo[Boolean],
          fields("retweeted").convertTo[Boolean],
          fields("possibly_sensitive").convertTo[Boolean],
          fields("lang").convertTo[String]
        )
      }

      override def write(x: TwitterAPI.RetweetedStatus): JsValue = toJsObject(
        ("created_at", JsString(x.created_at)),
        ("id", JsNumber(x.id)),
        ("id_str", JsString(x.id_str)),
        ("text", JsString(x.text)),
        ("truncated", JsBoolean(x.truncated)),
        ("entities", x.entities.toJson),
        ("source", JsString(x.source)),
        ("in_reply_to_status_id", x.in_reply_to_status_id.toJson),
        ("in_reply_to_status_id_str", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_user_id", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_user_id_str", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_screen_name", x.in_reply_to_status_id_str.toJson),
        ("user", x.user.toJson),
        ("geo", x.geo.toJson),
        ("coordinates", x.coordinates.toJson),
        ("place", x.place.toJson),
        ("contributors", x.contributors.toJson),
        ("is_quote_status", JsBoolean(x.is_quote_status)),
        ("retweet_count", JsNumber(x.retweet_count)),
        ("favorite_count", JsNumber(x.favorite_count)),
        ("favorited", JsBoolean(x.favorited)),
        ("retweeted", JsBoolean(x.retweeted)),
        ("possibly_sensitive", JsBoolean(x.possibly_sensitive)),
        ("lang", JsString(x.lang))
      )
    }
    new RootJsonFormat[TwitterAPI.Tweet] {
      override def read(json: JsValue): TwitterAPI.Tweet = {
        val fields = json.asJsObject.fields
        new TwitterAPI.Tweet(
          fields("created_at").convertTo[String],
          fields("id").convertTo[Long],
          fields("id_str").convertTo[String],
          fields("text").convertTo[String],
          fields("truncated").convertTo[Boolean],
          fields("entities").convertTo[TwitterAPI.Entities],
          fields("source").convertTo[String],
          fields.get("in_reply_to_status_id").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_status_id_str").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_user_id").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_user_id_str").flatMap(_.convertTo[Option[String]]),
          fields.get("in_reply_to_screen_name").flatMap(_.convertTo[Option[String]]),
          fields("user").convertTo[TwitterAPI.User],
          fields.get("geo").flatMap(_.convertTo[Option[String]]),
          fields.get("coordinates").flatMap(_.convertTo[Option[String]]),
          fields.get("place").flatMap(_.convertTo[Option[String]]),
          fields.get("contributors").flatMap(_.convertTo[Option[String]]),
          fields("retweeted_status").convertTo[TwitterAPI.RetweetedStatus],
          fields("is_quote_status").convertTo[Boolean],
          fields("retweet_count").convertTo[Int],
          fields("favorite_count").convertTo[Int],
          fields("favorited").convertTo[Boolean],
          fields("retweeted").convertTo[Boolean],
          fields("possibly_sensitive").convertTo[Boolean],
          fields("lang").convertTo[String]
        )
      }

      override def write(x: TwitterAPI.Tweet): JsValue = toJsObject(
        ("created_at", JsString(x.created_at)),
        ("id", JsNumber(x.id)),
        ("id_str", JsString(x.id_str)),
        ("text", JsString(x.text)),
        ("truncated", JsBoolean(x.truncated)),
        ("entities", x.entities.toJson),
        ("source", JsString(x.source)),
        ("in_reply_to_status_id", x.in_reply_to_status_id.toJson),
        ("in_reply_to_status_id_str", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_user_id", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_user_id_str", x.in_reply_to_status_id_str.toJson),
        ("in_reply_to_screen_name", x.in_reply_to_status_id_str.toJson),
        ("user", x.user.toJson),
        ("geo", x.geo.toJson),
        ("coordinates", x.coordinates.toJson),
        ("place", x.place.toJson),
        ("contributors", x.contributors.toJson),
        ("retweeted_status", x.retweeted_status.toJson),
        ("is_quote_status", JsBoolean(x.is_quote_status)),
        ("retweet_count", JsNumber(x.retweet_count)),
        ("favorite_count", JsNumber(x.favorite_count)),
        ("favorited", JsBoolean(x.favorited)),
        ("retweeted", JsBoolean(x.retweeted)),
        ("possibly_sensitive", JsBoolean(x.possibly_sensitive)),
        ("lang", JsString(x.lang))
      )
    }
  }
  implicit lazy val bidRequestJsonFormat: RootJsonFormat[OpenRTB.BidRequest] = {
    implicit val jf21: RootJsonFormat[OpenRTB.Segment] = new RootJsonFormat[OpenRTB.Segment] {
      override def read(json: JsValue): OpenRTB.Segment = {
        val fields = json.asJsObject.fields
        new OpenRTB.Segment(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("value").flatMap(_.convertTo[Option[String]]),
        )
      }

      override def write(x: OpenRTB.Segment): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("value", x.value.toJson),
      )
    }
    implicit val jf20: RootJsonFormat[OpenRTB.Data] = new RootJsonFormat[OpenRTB.Data] {
      override def read(json: JsValue): OpenRTB.Data = {
        val fields = json.asJsObject.fields
        new OpenRTB.Data(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("segment").fold[List[OpenRTB.Segment]](Nil)(_.convertTo[List[OpenRTB.Segment]]),
        )
      }

      override def write(x: OpenRTB.Data): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("segment", x.segment.toJson),
      )
    }
    implicit val jf19: RootJsonFormat[OpenRTB.Geo] = new RootJsonFormat[OpenRTB.Geo] {
      override def read(json: JsValue): OpenRTB.Geo = {
        val fields = json.asJsObject.fields
        new OpenRTB.Geo(
          fields.get("lat").flatMap(_.convertTo[Option[Double]]),
          fields.get("lon").flatMap(_.convertTo[Option[Double]]),
          fields.get("type").flatMap(_.convertTo[Option[Int]]),
          fields.get("accuracy").flatMap(_.convertTo[Option[Int]]),
          fields.get("lastfix").flatMap(_.convertTo[Option[Int]]),
          fields.get("ipservice").flatMap(_.convertTo[Option[Int]]),
          fields.get("country").flatMap(_.convertTo[Option[String]]),
          fields.get("region").flatMap(_.convertTo[Option[String]]),
          fields.get("regionfips104").flatMap(_.convertTo[Option[String]]),
          fields.get("metro").flatMap(_.convertTo[Option[String]]),
          fields.get("city").flatMap(_.convertTo[Option[String]]),
          fields.get("zip").flatMap(_.convertTo[Option[String]]),
          fields.get("utcoffset").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Geo): JsValue = toJsObject(
        ("lat", x.lat.toJson),
        ("lon", x.lon.toJson),
        ("type", x.`type`.toJson),
        ("accuracy", x.accuracy.toJson),
        ("lastfix", x.lastfix.toJson),
        ("ipservice", x.ipservice.toJson),
        ("country", x.country.toJson),
        ("region", x.region.toJson),
        ("regionfips104", x.regionfips104.toJson),
        ("metro", x.metro.toJson),
        ("city", x.city.toJson),
        ("zip", x.zip.toJson),
        ("utcoffset", x.utcoffset.toJson)
      )
    }
    implicit val jf18: RootJsonFormat[OpenRTB.User] = new RootJsonFormat[OpenRTB.User] {
      override def read(json: JsValue): OpenRTB.User = {
        val fields = json.asJsObject.fields
        new OpenRTB.User(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("buyeruid").flatMap(_.convertTo[Option[String]]),
          fields.get("yob").flatMap(_.convertTo[Option[Int]]),
          fields.get("gender").flatMap(_.convertTo[Option[String]]),
          fields.get("keywords").flatMap(_.convertTo[Option[String]]),
          fields.get("customdata").flatMap(_.convertTo[Option[String]]),
          fields.get("geo").flatMap(_.convertTo[Option[OpenRTB.Geo]]),
          fields.get("data").flatMap(_.convertTo[Option[OpenRTB.Data]])
        )
      }

      override def write(x: OpenRTB.User): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("buyeruid", x.buyeruid.toJson),
        ("yob", x.yob.toJson),
        ("gender", x.gender.toJson),
        ("keywords", x.keywords.toJson),
        ("customdata", x.customdata.toJson),
        ("geo", x.geo.toJson),
        ("data", x.data.toJson)
      )
    }
    implicit val jf17: RootJsonFormat[OpenRTB.Device] = new RootJsonFormat[OpenRTB.Device] {
      override def read(json: JsValue): OpenRTB.Device = {
        val fields = json.asJsObject.fields
        new OpenRTB.Device(
          fields.get("ua").flatMap(_.convertTo[Option[String]]),
          fields.get("geo").flatMap(_.convertTo[Option[OpenRTB.Geo]]),
          fields.get("dnt").flatMap(_.convertTo[Option[Int]]),
          fields.get("lmt").flatMap(_.convertTo[Option[Int]]),
          fields.get("ip").flatMap(_.convertTo[Option[String]]),
          fields.get("devicetype").flatMap(_.convertTo[Option[Int]]),
          fields.get("make").flatMap(_.convertTo[Option[String]]),
          fields.get("model").flatMap(_.convertTo[Option[String]]),
          fields.get("os").flatMap(_.convertTo[Option[String]]),
          fields.get("osv").flatMap(_.convertTo[Option[String]]),
          fields.get("hwv").flatMap(_.convertTo[Option[String]]),
          fields.get("h").flatMap(_.convertTo[Option[Int]]),
          fields.get("w").flatMap(_.convertTo[Option[Int]]),
          fields.get("ppi").flatMap(_.convertTo[Option[Int]]),
          fields.get("pxratio").flatMap(_.convertTo[Option[Double]]),
          fields.get("js").flatMap(_.convertTo[Option[Int]]),
          fields.get("geofetch").flatMap(_.convertTo[Option[Int]]),
          fields.get("flashver").flatMap(_.convertTo[Option[String]]),
          fields.get("language").flatMap(_.convertTo[Option[String]]),
          fields.get("carrier").flatMap(_.convertTo[Option[String]]),
          fields.get("mccmnc").flatMap(_.convertTo[Option[String]]),
          fields.get("connectiontype").flatMap(_.convertTo[Option[Int]]),
          fields.get("ifa").flatMap(_.convertTo[Option[String]]),
          fields.get("didsha1").flatMap(_.convertTo[Option[String]]),
          fields.get("didmd5").flatMap(_.convertTo[Option[String]]),
          fields.get("dpidsha1").flatMap(_.convertTo[Option[String]]),
          fields.get("dpidmd5").flatMap(_.convertTo[Option[String]]),
          fields.get("macsha1").flatMap(_.convertTo[Option[String]]),
          fields.get("macmd5").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Device): JsValue = toJsObject(
        ("ua", x.ua.toJson),
        ("geo", x.geo.toJson),
        ("dnt", x.dnt.toJson),
        ("lmt", x.lmt.toJson),
        ("ip", x.ip.toJson),
        ("devicetype", x.devicetype.toJson),
        ("make", x.make.toJson),
        ("model", x.model.toJson),
        ("os", x.os.toJson),
        ("osv", x.osv.toJson),
        ("hwv", x.hwv.toJson),
        ("h", x.h.toJson),
        ("w", x.w.toJson),
        ("ppi", x.ppi.toJson),
        ("pxratio", x.pxratio.toJson),
        ("js", x.js.toJson),
        ("geofetch", x.geofetch.toJson),
        ("flashver", x.flashver.toJson),
        ("language", x.language.toJson),
        ("carrier", x.carrier.toJson),
        ("mccmnc", x.mccmnc.toJson),
        ("connectiontype", x.connectiontype.toJson),
        ("ifa", x.ifa.toJson),
        ("didsha1", x.didsha1.toJson),
        ("didmd5", x.didmd5.toJson),
        ("dpidsha1", x.dpidsha1.toJson),
        ("dpidmd5", x.dpidmd5.toJson),
        ("macsha1", x.macsha1.toJson),
        ("macmd5", x.macmd5.toJson)
      )
    }
    implicit val jf16: RootJsonFormat[OpenRTB.Producer] = new RootJsonFormat[OpenRTB.Producer] {
      override def read(json: JsValue): OpenRTB.Producer = {
        val fields = json.asJsObject.fields
        new OpenRTB.Producer(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("cat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("domain").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Producer): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("cat", x.cat.toJson),
        ("domain", x.domain.toJson)
      )
    }
    implicit val jf15: RootJsonFormat[OpenRTB.Content] = new RootJsonFormat[OpenRTB.Content] {
      override def read(json: JsValue): OpenRTB.Content = {
        val fields = json.asJsObject.fields
        new OpenRTB.Content(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("episode").flatMap(_.convertTo[Option[Int]]),
          fields.get("title").flatMap(_.convertTo[Option[String]]),
          fields.get("series").flatMap(_.convertTo[Option[String]]),
          fields.get("season").flatMap(_.convertTo[Option[String]]),
          fields.get("artist").flatMap(_.convertTo[Option[String]]),
          fields.get("genre").flatMap(_.convertTo[Option[String]]),
          fields.get("album").flatMap(_.convertTo[Option[String]]),
          fields.get("isrc").flatMap(_.convertTo[Option[String]]),
          fields.get("producer").flatMap(_.convertTo[Option[OpenRTB.Producer]]),
          fields.get("url").flatMap(_.convertTo[Option[String]]),
          fields.get("cat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("prodq").flatMap(_.convertTo[Option[Int]]),
          fields.get("videoquality").flatMap(_.convertTo[Option[Int]]),
          fields.get("context").flatMap(_.convertTo[Option[Int]]),
          fields.get("contentrating").flatMap(_.convertTo[Option[String]]),
          fields.get("userrating").flatMap(_.convertTo[Option[String]]),
          fields.get("qagmediarating").flatMap(_.convertTo[Option[Int]]),
          fields.get("keywords").flatMap(_.convertTo[Option[String]]),
          fields.get("livestream").flatMap(_.convertTo[Option[Int]]),
          fields.get("sourcerelationship").flatMap(_.convertTo[Option[Int]]),
          fields.get("len").flatMap(_.convertTo[Option[Int]]),
          fields.get("language").flatMap(_.convertTo[Option[String]]),
          fields.get("embeddable").flatMap(_.convertTo[Option[Int]]),
          fields.get("data").flatMap(_.convertTo[Option[OpenRTB.Data]])
        )
      }

      override def write(x: OpenRTB.Content): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("episode", x.episode.toJson),
        ("title", x.title.toJson),
        ("series", x.series.toJson),
        ("season", x.season.toJson),
        ("artist", x.artist.toJson),
        ("genre", x.genre.toJson),
        ("album", x.album.toJson),
        ("isrc", x.isrc.toJson),
        ("producer", x.producer.toJson),
        ("url", x.url.toJson),
        ("cat", x.cat.toJson),
        ("prodq", x.prodq.toJson),
        ("videoquality", x.videoquality.toJson),
        ("context", x.context.toJson),
        ("contentrating", x.contentrating.toJson),
        ("userrating", x.userrating.toJson),
        ("qagmediarating", x.qagmediarating.toJson),
        ("keywords", x.keywords.toJson),
        ("livestream", x.livestream.toJson),
        ("sourcerelationship", x.sourcerelationship.toJson),
        ("len", x.len.toJson),
        ("language", x.language.toJson),
        ("embeddable", x.embeddable.toJson),
        ("data", x.data.toJson)
      )
    }
    implicit val jf14: RootJsonFormat[OpenRTB.Publisher] = new RootJsonFormat[OpenRTB.Publisher] {
      override def read(json: JsValue): OpenRTB.Publisher = {
        val fields = json.asJsObject.fields
        new OpenRTB.Publisher(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("cat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("domain").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Publisher): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("cat", x.cat.toJson),
        ("domain", x.domain.toJson)
      )
    }
    implicit val jf13: RootJsonFormat[OpenRTB.App] = new RootJsonFormat[OpenRTB.App] {
      override def read(json: JsValue): OpenRTB.App = {
        val fields = json.asJsObject.fields
        new OpenRTB.App(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("bundle").flatMap(_.convertTo[Option[String]]),
          fields.get("domain").flatMap(_.convertTo[Option[String]]),
          fields.get("storeurl").flatMap(_.convertTo[Option[String]]),
          fields.get("cat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("sectioncat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("pagecat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("ver").flatMap(_.convertTo[Option[String]]),
          fields.get("privacypolicy").flatMap(_.convertTo[Option[Int]]),
          fields.get("paid").flatMap(_.convertTo[Option[Int]]),
          fields.get("publisher").flatMap(_.convertTo[Option[OpenRTB.Publisher]]),
          fields.get("content").flatMap(_.convertTo[Option[OpenRTB.Content]]),
          fields.get("keywords").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.App): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("bundle", x.bundle.toJson),
        ("domain", x.domain.toJson),
        ("storeurl", x.storeurl.toJson),
        ("cat", x.cat.toJson),
        ("sectioncat", x.sectioncat.toJson),
        ("pagecat", x.pagecat.toJson),
        ("ver", x.ver.toJson),
        ("privacypolicy", x.privacypolicy.toJson),
        ("paid", x.paid.toJson),
        ("publisher", x.publisher.toJson),
        ("content", x.content.toJson),
        ("keywords", x.keywords.toJson)
      )
    }
    implicit val jf12: RootJsonFormat[OpenRTB.Site] = new RootJsonFormat[OpenRTB.Site] {
      override def read(json: JsValue): OpenRTB.Site = {
        val fields = json.asJsObject.fields
        new OpenRTB.Site(
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("name").flatMap(_.convertTo[Option[String]]),
          fields.get("domain").flatMap(_.convertTo[Option[String]]),
          fields.get("cat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("sectioncat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("pagecat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("page").flatMap(_.convertTo[Option[String]]),
          fields.get("ref").flatMap(_.convertTo[Option[String]]),
          fields.get("search").flatMap(_.convertTo[Option[String]]),
          fields.get("mobile").flatMap(_.convertTo[Option[Int]]),
          fields.get("privacypolicy").flatMap(_.convertTo[Option[Int]]),
          fields.get("publisher").flatMap(_.convertTo[Option[OpenRTB.Publisher]]),
          fields.get("content").flatMap(_.convertTo[Option[OpenRTB.Content]]),
          fields.get("keywords").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Site): JsValue = toJsObject(
        ("id", x.id.toJson),
        ("name", x.name.toJson),
        ("domain", x.domain.toJson),
        ("cat", x.cat.toJson),
        ("sectioncat", x.sectioncat.toJson),
        ("pagecat", x.pagecat.toJson),
        ("page", x.page.toJson),
        ("ref", x.ref.toJson),
        ("search", x.search.toJson),
        ("mobile", x.mobile.toJson),
        ("privacypolicy", x.privacypolicy.toJson),
        ("publisher", x.publisher.toJson),
        ("content", x.content.toJson),
        ("keywords", x.keywords.toJson)
      )
    }
    implicit val jf11: RootJsonFormat[OpenRTB.Deal] = new RootJsonFormat[OpenRTB.Deal] {
      override def read(json: JsValue): OpenRTB.Deal = {
        val fields = json.asJsObject.fields
        new OpenRTB.Deal(
          fields("id").convertTo[String],
          fields.get("bidfloor").fold(0.0)(_.convertTo[Double]),
          fields.get("bidfloorcur").fold("USD")(_.convertTo[String]),
          fields.get("at").flatMap(_.convertTo[Option[Int]]),
          fields.get("wseat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("wadomain").fold[List[String]](Nil)(_.convertTo[List[String]])
        )
      }

      override def write(x: OpenRTB.Deal): JsValue = toJsObject(
        ("id", JsString(x.id)),
        ("bidfloor", toJson(x.bidfloor, 0.0)),
        ("bidfloorcur", toJson(x.bidfloorcur, "USD")),
        ("at", x.at.toJson),
        ("wseat", x.wseat.toJson),
        ("wadomain", x.wadomain.toJson)
      )
    }
    implicit val jf10: RootJsonFormat[OpenRTB.Pmp] = new RootJsonFormat[OpenRTB.Pmp] {
      override def read(json: JsValue): OpenRTB.Pmp = {
        val fields = json.asJsObject.fields
        new OpenRTB.Pmp(
          fields.get("private_auction").fold(0)(_.convertTo[Int]),
          fields.get("deals").fold[List[OpenRTB.Deal]](Nil)(_.convertTo[List[OpenRTB.Deal]])
        )
      }

      override def write(x: OpenRTB.Pmp): JsValue = toJsObject(
        ("private_auction", toJson(x.private_auction, 0)),
        ("deals", x.deals.toJson)
      )
    }
    implicit val jf9: RootJsonFormat[OpenRTB.Format] = new RootJsonFormat[OpenRTB.Format] {
      override def read(json: JsValue): OpenRTB.Format = {
        val fields = json.asJsObject.fields
        new OpenRTB.Format(
          fields.get("w").flatMap(_.convertTo[Option[Int]]),
          fields.get("h").flatMap(_.convertTo[Option[Int]]),
          fields.get("wratio").flatMap(_.convertTo[Option[Int]]),
          fields.get("hratio").flatMap(_.convertTo[Option[Int]]),
          fields.get("wmin").flatMap(_.convertTo[Option[Int]])
        )
      }

      override def write(x: OpenRTB.Format): JsValue = toJsObject(
        ("w", x.w.toJson),
        ("h", x.h.toJson),
        ("wratio", x.wratio.toJson),
        ("hratio", x.hratio.toJson),
        ("wmin", x.wmin.toJson)
      )
    }
    implicit val jf8: RootJsonFormat[OpenRTB.Native] = new RootJsonFormat[OpenRTB.Native] {
      override def read(json: JsValue): OpenRTB.Native = {
        val fields = json.asJsObject.fields
        new OpenRTB.Native(
          fields("request").convertTo[String],
          fields.get("ver").flatMap(_.convertTo[Option[String]]),
          fields.get("api").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("battr").fold[List[Int]](Nil)(_.convertTo[List[Int]])
        )
      }

      override def write(x: OpenRTB.Native): JsValue = toJsObject(
        ("request", JsString(x.request)),
        ("ver", x.ver.toJson),
        ("api", x.api.toJson),
        ("battr", x.battr.toJson)
      )
    }
    implicit val jf7: RootJsonFormat[OpenRTB.Banner] = new RootJsonFormat[OpenRTB.Banner] {
      override def read(json: JsValue): OpenRTB.Banner = {
        val fields = json.asJsObject.fields
        new OpenRTB.Banner(
          fields.get("format").flatMap(_.convertTo[Option[OpenRTB.Format]]),
          fields.get("w").flatMap(_.convertTo[Option[Int]]),
          fields.get("h").flatMap(_.convertTo[Option[Int]]),
          fields.get("wmax").flatMap(_.convertTo[Option[Int]]),
          fields.get("hmax").flatMap(_.convertTo[Option[Int]]),
          fields.get("wmin").flatMap(_.convertTo[Option[Int]]),
          fields.get("hmin").flatMap(_.convertTo[Option[Int]]),
          fields.get("btype").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("battr").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("pos").flatMap(_.convertTo[Option[Int]]),
          fields.get("mimes").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("topframe").flatMap(_.convertTo[Option[Int]]),
          fields.get("expdir").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("api").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("id").flatMap(_.convertTo[Option[String]]),
          fields.get("vcm").flatMap(_.convertTo[Option[Int]])
        )
      }

      override def write(x: OpenRTB.Banner): JsValue = toJsObject(
        ("format", x.format.toJson),
        ("w", x.w.toJson),
        ("h", x.h.toJson),
        ("wmax", x.wmax.toJson),
        ("hmax", x.hmax.toJson),
        ("wmin", x.wmin.toJson),
        ("hmin", x.hmin.toJson),
        ("btype", x.btype.toJson),
        ("battr", x.battr.toJson),
        ("pos", x.pos.toJson),
        ("mimes", x.mimes.toJson),
        ("topframe", x.topframe.toJson),
        ("expdir", x.expdir.toJson),
        ("api", x.api.toJson),
        ("id", x.id.toJson),
        ("vcm", x.vcm.toJson)
      )
    }
    implicit val jf6: RootJsonFormat[OpenRTB.Audio] = new RootJsonFormat[OpenRTB.Audio] {
      override def read(json: JsValue): OpenRTB.Audio = {
        val fields = json.asJsObject.fields
        new OpenRTB.Audio(
          fields.get("mimes").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("minduration").flatMap(_.convertTo[Option[Int]]),
          fields.get("maxduration").flatMap(_.convertTo[Option[Int]]),
          fields.get("protocols").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("startdelay").flatMap(_.convertTo[Option[Int]]),
          fields.get("sequence").flatMap(_.convertTo[Option[Int]]),
          fields.get("battr").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("maxextended").flatMap(_.convertTo[Option[Int]]),
          fields.get("minbitrate").flatMap(_.convertTo[Option[Int]]),
          fields.get("maxbitrate").flatMap(_.convertTo[Option[Int]]),
          fields.get("delivery").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("companionad").fold[List[OpenRTB.Banner]](Nil)(_.convertTo[List[OpenRTB.Banner]]),
          fields.get("api").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("companiontype").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("maxseq").flatMap(_.convertTo[Option[Int]]),
          fields.get("feed").flatMap(_.convertTo[Option[Int]]),
          fields.get("stitched").flatMap(_.convertTo[Option[Int]]),
          fields.get("nvol").flatMap(_.convertTo[Option[Int]])
        )
      }

      override def write(x: OpenRTB.Audio): JsValue = toJsObject(
        ("mimes", x.mimes.toJson),
        ("minduration", x.minduration.toJson),
        ("maxduration", x.maxduration.toJson),
        ("protocols", x.protocols.toJson),
        ("startdelay", x.startdelay.toJson),
        ("sequence", x.sequence.toJson),
        ("battr", x.battr.toJson),
        ("maxextended", x.maxextended.toJson),
        ("minbitrate", x.minbitrate.toJson),
        ("maxbitrate", x.maxbitrate.toJson),
        ("delivery", x.delivery.toJson),
        ("companionad", x.companionad.toJson),
        ("api", x.api.toJson),
        ("companiontype", x.companiontype.toJson),
        ("maxseq", x.maxseq.toJson),
        ("feed", x.feed.toJson),
        ("stitched", x.stitched.toJson),
        ("nvol", x.nvol.toJson)
      )
    }
    implicit val jf5: RootJsonFormat[OpenRTB.Video] = new RootJsonFormat[OpenRTB.Video] {
      override def read(json: JsValue): OpenRTB.Video = {
        val fields = json.asJsObject.fields
        new OpenRTB.Video(
          fields.get("mimes").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("minduration").flatMap(_.convertTo[Option[Int]]),
          fields.get("maxduration").flatMap(_.convertTo[Option[Int]]),
          fields.get("protocols").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("protocol").flatMap(_.convertTo[Option[Int]]),
          fields.get("w").flatMap(_.convertTo[Option[Int]]),
          fields.get("h").flatMap(_.convertTo[Option[Int]]),
          fields.get("startdelay").flatMap(_.convertTo[Option[Int]]),
          fields.get("placement").flatMap(_.convertTo[Option[Int]]),
          fields.get("linearity").flatMap(_.convertTo[Option[Int]]),
          fields.get("skip").flatMap(_.convertTo[Option[Int]]),
          fields.get("skipmin").fold(0)(_.convertTo[Int]),
          fields.get("skipafter").fold(0)(_.convertTo[Int]),
          fields.get("sequence").flatMap(_.convertTo[Option[Int]]),
          fields.get("battr").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("maxextended").flatMap(_.convertTo[Option[Int]]),
          fields.get("minbitrate").flatMap(_.convertTo[Option[Int]]),
          fields.get("maxbitrate").flatMap(_.convertTo[Option[Int]]),
          fields.get("boxingallowed").fold(1)(_.convertTo[Int]),
          fields.get("playbackmethod").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("playbackend").flatMap(_.convertTo[Option[Int]]),
          fields.get("delivery").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("pos").flatMap(_.convertTo[Option[Int]]),
          fields.get("companionad").fold[List[OpenRTB.Banner]](Nil)(_.convertTo[List[OpenRTB.Banner]]),
          fields.get("api").fold[List[Int]](Nil)(_.convertTo[List[Int]]),
          fields.get("companiontype").fold[List[Int]](Nil)(_.convertTo[List[Int]])
        )
      }

      override def write(x: OpenRTB.Video): JsValue = toJsObject(
        ("mimes", x.mimes.toJson),
        ("minduration", x.minduration.toJson),
        ("maxduration", x.maxduration.toJson),
        ("protocols", x.protocols.toJson),
        ("protocol", x.protocol.toJson),
        ("w", x.w.toJson),
        ("h", x.h.toJson),
        ("startdelay", x.startdelay.toJson),
        ("placement", x.placement.toJson),
        ("linearity", x.linearity.toJson),
        ("skip", x.skip.toJson),
        ("skipmin", toJson(x.skipmin, 0)),
        ("skipafter", toJson(x.skipafter, 0)),
        ("sequence", x.sequence.toJson),
        ("battr", x.battr.toJson),
        ("maxextended", x.maxextended.toJson),
        ("minbitrate", x.minbitrate.toJson),
        ("maxbitrate", x.maxbitrate.toJson),
        ("boxingallowed", toJson(x.boxingallowed, 1)),
        ("playbackmethod", x.playbackmethod.toJson),
        ("playbackend", x.playbackend.toJson),
        ("delivery", x.delivery.toJson),
        ("pos", x.pos.toJson),
        ("companionad", x.companionad.toJson),
        ("api", x.api.toJson),
        ("companiontype", x.companiontype.toJson)
      )
    }
    implicit val jf4: RootJsonFormat[OpenRTB.Metric] = new RootJsonFormat[OpenRTB.Metric] {
      override def read(json: JsValue): OpenRTB.Metric = {
        val fields = json.asJsObject.fields
        new OpenRTB.Metric(
          fields("type").convertTo[String],
          fields("value").convertTo[Double],
          fields.get("vendor").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Metric): JsValue = toJsObject(
        ("type", JsString(x.`type`)),
        ("value", JsNumber(x.value)),
        ("vendor", x.vendor.toJson)
      )
    }
    implicit val jf3: RootJsonFormat[OpenRTB.Imp] = new RootJsonFormat[OpenRTB.Imp] {
      override def read(json: JsValue): OpenRTB.Imp = {
        val fields = json.asJsObject.fields
        new OpenRTB.Imp(
          fields("id").convertTo[String],
          fields.get("metric").fold[List[OpenRTB.Metric]](Nil)(_.convertTo[List[OpenRTB.Metric]]),
          fields.get("banner").flatMap(_.convertTo[Option[OpenRTB.Banner]]),
          fields.get("video").flatMap(_.convertTo[Option[OpenRTB.Video]]),
          fields.get("audio").flatMap(_.convertTo[Option[OpenRTB.Audio]]),
          fields.get("native").flatMap(_.convertTo[Option[OpenRTB.Native]]),
          fields.get("pmp").flatMap(_.convertTo[Option[OpenRTB.Pmp]]),
          fields.get("displaymanager").flatMap(_.convertTo[Option[String]]),
          fields.get("displaymanagerver").flatMap(_.convertTo[Option[String]]),
          fields.get("instl").fold(0)(_.convertTo[Int]),
          fields.get("tagid").flatMap(_.convertTo[Option[String]]),
          fields.get("bidfloor").fold(0.0)(_.convertTo[Double]),
          fields.get("bidfloorcur").fold("USD")(_.convertTo[String]),
          fields.get("clickbrowser").flatMap(_.convertTo[Option[Int]]),
          fields.get("secure").fold(0)(_.convertTo[Int]),
          fields.get("iframebuster").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("exp").flatMap(_.convertTo[Option[Int]])
        )
      }

      override def write(x: OpenRTB.Imp): JsValue = toJsObject(
        ("id", JsString(x.id)),
        ("metric", x.metric.toJson),
        ("banner", x.banner.toJson),
        ("video", x.video.toJson),
        ("audio", x.audio.toJson),
        ("native", x.native.toJson),
        ("pmp", x.pmp.toJson),
        ("displaymanager", x.displaymanager.toJson),
        ("displaymanagerver", x.displaymanagerver.toJson),
        ("instl", toJson(x.instl, 0)),
        ("tagid", x.tagid.toJson),
        ("bidfloor", toJson(x.bidfloor, 0.0)),
        ("bidfloorcur", toJson(x.bidfloorcur, "USD")),
        ("clickbrowser", x.clickbrowser.toJson),
        ("secure", toJson(x.secure, 0)),
        ("iframebuster", x.iframebuster.toJson),
        ("exp", x.exp.toJson)
      )
    }
    implicit val jf2: RootJsonFormat[OpenRTB.Reqs] = new RootJsonFormat[OpenRTB.Reqs] {
      override def read(json: JsValue): OpenRTB.Reqs =
        new OpenRTB.Reqs(json.asJsObject.fields("coppa").convertTo[Int])

      override def write(x: OpenRTB.Reqs): JsValue = JsObject(("coppa", JsNumber(x.coppa)))
    }
    implicit val jf1: RootJsonFormat[OpenRTB.Source] = new RootJsonFormat[OpenRTB.Source] {
      override def read(json: JsValue): OpenRTB.Source = {
        val fields = json.asJsObject.fields
        new OpenRTB.Source(
          fields.get("fd").flatMap(_.convertTo[Option[Int]]),
          fields.get("tid").flatMap(_.convertTo[Option[String]]),
          fields.get("pchain").flatMap(_.convertTo[Option[String]])
        )
      }

      override def write(x: OpenRTB.Source): JsValue = toJsObject(
        ("fd", x.fd.toJson),
        ("tid", x.tid.toJson),
        ("pchain", x.pchain.toJson)
      )
    }
    new RootJsonFormat[OpenRTB.BidRequest] {
      override def read(json: JsValue): OpenRTB.BidRequest = {
        val fields = json.asJsObject.fields
        new OpenRTB.BidRequest(
          fields("id").convertTo[String],
          fields.get("imp").fold[List[OpenRTB.Imp]](Nil)(_.convertTo[List[OpenRTB.Imp]]),
          fields.get("site").flatMap(_.convertTo[Option[OpenRTB.Site]]),
          fields.get("app").flatMap(_.convertTo[Option[OpenRTB.App]]),
          fields.get("device").flatMap(_.convertTo[Option[OpenRTB.Device]]),
          fields.get("user").flatMap(_.convertTo[Option[OpenRTB.User]]),
          fields.get("test").fold(0)(_.convertTo[Int]),
          fields.get("at").fold(2)(_.convertTo[Int]),
          fields.get("tmax").flatMap(_.convertTo[Option[Int]]),
          fields.get("wset").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("bset").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("allimps").fold(0)(_.convertTo[Int]),
          fields.get("cur").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("wlang").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("bcat").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("badv").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("bapp").fold[List[String]](Nil)(_.convertTo[List[String]]),
          fields.get("source").flatMap(_.convertTo[Option[OpenRTB.Source]]),
          fields.get("reqs").flatMap(_.convertTo[Option[OpenRTB.Reqs]])
        )
      }

      override def write(x: OpenRTB.BidRequest): JsValue = toJsObject(
        ("id", JsString(x.id)),
        ("imp", x.imp.toJson),
        ("site", x.site.toJson),
        ("app", x.app.toJson),
        ("device", x.device.toJson),
        ("user", x.user.toJson),
        ("test", toJson(x.test, 0)),
        ("at", toJson(x.at, 2)),
        ("tmax", x.tmax.toJson),
        ("wset", x.wset.toJson),
        ("bset", x.bset.toJson),
        ("allimps", toJson(x.allimps, 0)),
        ("cur", x.cur.toJson),
        ("wlang", x.wlang.toJson),
        ("bcat", x.bcat.toJson),
        ("badv", x.badv.toJson),
        ("bapp", x.bapp.toJson),
        ("source", x.source.toJson),
        ("reqs", x.reqs.toJson)
      )
    }
  }
  implicit val uuidJsonFormat: RootJsonFormat[UUID] = stringJsonFormat(UUID.fromString)
  implicit val yearMonthJsonFormat: RootJsonFormat[YearMonth] = stringJsonFormat(YearMonth.parse)
  implicit val yearJsonFormat: RootJsonFormat[Year] = stringJsonFormat(Year.parse)
  implicit val zonedDateTimeJsonFormat: RootJsonFormat[ZonedDateTime] = stringJsonFormat(ZonedDateTime.parse)
  implicit val zoneIdJsonFormat: RootJsonFormat[ZoneId] = stringJsonFormat(ZoneId.of)
  implicit val zoneOffsetJsonFormat: RootJsonFormat[ZoneOffset] = stringJsonFormat(ZoneOffset.of)

  // Based on the Cat/Dog sample: https://gist.github.com/jrudolph/f2d0825aac74ed81c92a
  def readADT[T](json: JsValue)(pf: PartialFunction[String, T]): T =
    json.asJsObject.fields("type") match {
      case js: JsString if pf.isDefinedAt(js.value) => pf(js.value)
      case _ => deserializationError(s"Expected JSON string of ADT subclass name")
    }

  def writeADT[T <: Product](obj: T)(pf: PartialFunction[T, JsValue]): JsObject =
    new JsObject(pf.applyOrElse(obj, (x: T) => deserializationError(s"Cannot serialize $x"))
      .asJsObject.fields.updated("type", new JsString(obj.productPrefix)))

  def stringJsonFormat[T](construct: String => T): RootJsonFormat[T] = new RootJsonFormat[T] {
    def read(json: JsValue): T = json match {
      case js: JsString => construct(js.value)
      case _ => deserializationError(s"Expected JSON string")
    }

    def write(obj: T): JsValue = new JsString(obj.toString)
  }

  implicit def arrayBufferJsonFormat[T : JsonFormat]: RootJsonFormat[mutable.ArrayBuffer[T]] =
    new RootJsonFormat[mutable.ArrayBuffer[T]] {
      def read(json: JsValue): mutable.ArrayBuffer[T] =
        if (!json.isInstanceOf[JsArray]) deserializationError(s"Expected JSON array")
        else {
          val es = json.asInstanceOf[JsArray].elements
          val buf = new mutable.ArrayBuffer[T](es.size)
          es.foreach(e => buf += e.convertTo[T])
          buf
        }

      def write(buf: mutable.ArrayBuffer[T]): JsValue = {
        val vs = Vector.newBuilder[JsValue]
        buf.foreach(x => vs += x.toJson)
        JsArray(vs.result())
      }
    }

  implicit def arraySeqJsonFormat[T : JsonFormat : ClassTag]: RootJsonFormat[ArraySeq[T]] =
    new RootJsonFormat[ArraySeq[T]] {
      def read(json: JsValue): ArraySeq[T] = json match {
        case ja: JsArray =>
          val b = ArraySeq.newBuilder[T]
          ja.elements.foreach(e => b += e.convertTo[T])
          b.result()
        case _ => deserializationError(s"Expected JSON array")
      }

      def write(as: ArraySeq[T]): JsValue = {
        val vs = Vector.newBuilder[JsValue]
        as.foreach(x => vs += x.toJson)
        JsArray(vs.result())
      }
    }

  private[this] def toJson[T: JsonWriter](x: T, d: T): JsValue =
    if (x == d) JsNull
    else x.toJson

  private[this] def toJsObject(fields: (String, JsValue)*): JsObject = JsObject(fields.filterNot { case (_, v) =>
    (v eq JsNull) || (v.isInstanceOf[JsArray] && v.asInstanceOf[JsArray].elements.isEmpty)
  }:_*)
}