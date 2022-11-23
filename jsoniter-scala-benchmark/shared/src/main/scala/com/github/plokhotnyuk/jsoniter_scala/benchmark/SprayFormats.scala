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
          fields("indices").convertTo[Seq[Int]]
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
          fields("hashtags").convertTo[Seq[String]],
          fields("symbols").convertTo[Seq[String]],
          fields("user_mentions").convertTo[Seq[TwitterAPI.UserMentions]],
          fields("urls").convertTo[Seq[TwitterAPI.Urls]]
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
          fields("in_reply_to_status_id").convertTo[Option[String]],
          fields("in_reply_to_status_id_str").convertTo[Option[String]],
          fields("in_reply_to_user_id").convertTo[Option[String]],
          fields("in_reply_to_user_id_str").convertTo[Option[String]],
          fields("in_reply_to_screen_name").convertTo[Option[String]],
          fields("user").convertTo[TwitterAPI.User],
          fields("geo").convertTo[Option[String]],
          fields("coordinates").convertTo[Option[String]],
          fields("place").convertTo[Option[String]],
          fields("contributors").convertTo[Option[String]],
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
          fields("in_reply_to_status_id").convertTo[Option[String]],
          fields("in_reply_to_status_id_str").convertTo[Option[String]],
          fields("in_reply_to_user_id").convertTo[Option[String]],
          fields("in_reply_to_user_id_str").convertTo[Option[String]],
          fields("in_reply_to_screen_name").convertTo[Option[String]],
          fields("user").convertTo[TwitterAPI.User],
          fields("geo").convertTo[Option[String]],
          fields("coordinates").convertTo[Option[String]],
          fields("place").convertTo[Option[String]],
          fields("contributors").convertTo[Option[String]],
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
  implicit lazy val bidRequestJsonFormat: RootJsonFormat[OpenRTB.BidRequest] = ??? //FIXME: Add OpenRTB benchmarks for spray-json
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

  private[this] def toJsObject(fields: (String, JsValue)*): JsObject = JsObject(fields.filterNot { case (_, v) =>
    (v eq JsNull) || (v.isInstanceOf[JsArray] && v.asInstanceOf[JsArray].elements.isEmpty)
  }:_*)
}
