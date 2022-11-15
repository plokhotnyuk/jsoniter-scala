package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._
import ai.x.play.json.Encoders._
import ai.x.play.json.Jsonx
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask
import play.api.libs.functional.syntax._
import play.api.libs.json._
import java.util.Base64
import scala.collection.immutable.{BitSet, IntMap, Map, Seq}
import scala.collection.mutable
import scala.util.Try

object PlayJsonFormats {
  implicit val config = JsonConfiguration(typeNaming = JsonNaming { fullName =>
    fullName.substring(Math.max(fullName.lastIndexOf('.') + 1, 0))
  }, discriminator = "type")

  def stringFormat[A](name: String)(f: String => A): Format[A] = new Format[A] {
    override def reads(js: JsValue): JsResult[A] =
      Try(JsSuccess(f(js.asInstanceOf[JsString].value))).getOrElse(JsError(s"expected.${name}string"))

    override def writes(v: A): JsValue = JsString(v.toString)
  }

  implicit def mutableMapReads[A, B](implicit mapReads: Reads[Map[A, B]]): Reads[mutable.Map[A, B]] =
    Reads[mutable.Map[A, B]](js => JsSuccess(js.as[Map[A, B]].foldLeft(mutable.Map.empty[A, B]) {
      (m, p) => m += ((p._1, p._2))
    }))

  implicit def mutableLongMapFormat[A](implicit mapReads: Reads[Map[Long, A]],
                                       aWrites: Writes[A]): Format[mutable.LongMap[A]] =
    new Format[mutable.LongMap[A]] {
      override def reads(js: JsValue): JsResult[mutable.LongMap[A]] =
        JsSuccess(js.as[Map[Long, A]].foldLeft(mutable.LongMap.empty[A]) { (m, p) =>
          m.update(p._1, p._2)
          m
        })

      override def writes(v: mutable.LongMap[A]): JsValue =
        Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
          (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
        })
    }

  implicit def intMapFormat[A](implicit mapReads: Reads[Map[Int, A]], aWrites: Writes[A]): Format[IntMap[A]] =
    new Format[IntMap[A]] {
      override def reads(js: JsValue): JsResult[IntMap[A]] =
        JsSuccess(js.as[Map[Int, A]].foldLeft(IntMap.empty[A])((m, p) => m.updated(p._1, p._2)))

      override def writes(v: IntMap[A]): JsValue =
        Json.toJsObject(v.foldLeft(mutable.LinkedHashMap.empty[String, JsValue]) {
          (m, p) => m += ((p._1.toString, aWrites.writes(p._2)))
        })
    }

  // Allow case classes with Tuple2 types to be represented as a Json Array with 2 elements e.g. (Double, Double)
  // Borrowed from https://gist.github.com/alexanderjarvis/4595298
  implicit def tuple2Format[A, B](implicit aFormat: Format[A], bFormat: Format[B]): Format[(A, B)] =
    new Format[(A, B)] {
      override def reads(js: JsValue): JsResult[(A, B)] = Try(readsUnsafe(js)).getOrElse(JsError("expected.jsarray"))

      override def writes(tuple: (A, B)): JsValue = JsArray(Seq(aFormat.writes(tuple._1), bFormat.writes(tuple._2)))

      private[this] def readsUnsafe(js: JsValue): JsResult[(A, B)] = {
        val arr = js.asInstanceOf[JsArray]
        aFormat.reads(arr(0)).flatMap(a => bFormat.reads(arr(1)).map(b => (a, b)))
      }
    }

  val base64Format: Format[Array[Byte]] = new Format[Array[Byte]] {
    override def reads(js: JsValue): JsResult[Array[Byte]] =
      Try(JsSuccess(Base64.getDecoder.decode(js.asInstanceOf[JsString].value)))
        .getOrElse(JsError(s"expected.base64string"))

    override def writes(v: Array[Byte]): JsValue = JsString(Base64.getEncoder.encodeToString(v))
  }

  implicit val charFormat: Format[Char] = stringFormat("char") { case s if s.length == 1 => s.charAt(0) }
  implicit val missingReqFieldsFormat: Format[MissingRequiredFields] = Json.format
  implicit val nestedStructsFormat: Format[NestedStructs] = Json.format
  implicit val anyValsFormat: Format[AnyVals] = {
    implicit val v1: Format[ByteVal] = implicitly[Format[Byte]].inmap(ByteVal.apply, _.a)
    implicit val v2: Format[ShortVal] = implicitly[Format[Short]].inmap(ShortVal.apply, _.a)
    implicit val v3: Format[IntVal] = implicitly[Format[Int]].inmap(IntVal.apply, _.a)
    implicit val v4: Format[LongVal] = implicitly[Format[Long]].inmap(LongVal.apply, _.a)
    implicit val v5: Format[BooleanVal] = implicitly[Format[Boolean]].inmap(BooleanVal.apply, _.a)
    implicit val v6: Format[CharVal] = charFormat.inmap(CharVal.apply, _.a)
    implicit val v7: Format[DoubleVal] = implicitly[Format[Double]].inmap(DoubleVal.apply, _.a)
    implicit val v8: Format[FloatVal] = implicitly[Format[Float]].inmap(FloatVal.apply, _.a)
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
    implicit lazy val v4: Format[ADTBase] = Json.format
    v4
  }
  val geoJSONFormat: Format[GeoJSON.GeoJSON] = {
    implicit val v1: Format[GeoJSON.Point] =
      (__ \ "coordinates").format[(Double, Double)].inmap(GeoJSON.Point.apply, _.coordinates)
    implicit lazy val v2: Format[GeoJSON.MultiPoint] = Json.format
    implicit lazy val v3: Format[GeoJSON.LineString] = Json.format
    implicit lazy val v4: Format[GeoJSON.MultiLineString] = Json.format
    implicit lazy val v5: Format[GeoJSON.Polygon] = Json.format
    implicit lazy val v6: Format[GeoJSON.MultiPolygon] = Json.format
    implicit lazy val v7: Format[GeoJSON.SimpleGeometry] = Json.format
    implicit lazy val v8: Format[GeoJSON.GeometryCollection] = Json.format
    implicit lazy val v9: Format[GeoJSON.Geometry] = Json.format
    implicit lazy val v10: Format[GeoJSON.Feature] = Json.format
    implicit lazy val v11: Format[GeoJSON.SimpleGeoJSON] = Json.format
    implicit lazy val v12: Format[GeoJSON.FeatureCollection] = Json.format
    implicit lazy val v13: Format[GeoJSON.GeoJSON] = Json.format
    v13
  }
  implicit val gitHubActionsAPIFormat: Format[GitHubActionsAPI.Response] = {
    implicit val v1: Format[Boolean] = stringFormat[Boolean]("boolean") { s =>
      "true" == s || "false" != s && sys.error("")
    }
    implicit val v2: Format[GitHubActionsAPI.Artifact] = Json.format
    Json.format
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
    implicit val v1: Format[TwitterAPI.Urls] = Format({
      for {
        url <- (__ \ "url").read[String]
        expanded_url <- (__ \ "expanded_url").read[String]
        display_url <- (__ \ "display_url").read[String]
        indices <- (__ \ "indices").readWithDefault[Seq[Int]](Nil)
      } yield TwitterAPI.Urls(url, expanded_url, display_url, indices)
    }, (x: TwitterAPI.Urls) => {
      toJsObject(
        "url" -> Json.toJson(x.url),
        "expanded_url" -> Json.toJson(x.expanded_url),
        "display_url" -> Json.toJson(x.display_url),
        "indices" -> Json.toJson(x.indices),
      )
    })
    implicit val v2: Format[TwitterAPI.Url] = Format({
      (__ \ "urls").readWithDefault[Seq[TwitterAPI.Urls]](Nil).map(TwitterAPI.Url.apply)
    }, (x: TwitterAPI.Url) => toJsObject("urls" -> Json.toJson(x.urls)))
    implicit val v3: Format[TwitterAPI.UserEntities] = Format({
      for {
        url <- (__ \ "url").read[TwitterAPI.Url]
        description <- (__ \ "description").read[TwitterAPI.Url]
      } yield TwitterAPI.UserEntities(url, description)
    }, (x: TwitterAPI.UserEntities) => {
      toJsObject(
        "url" -> Json.toJson(x.url),
        "description" -> Json.toJson(x.description),
      )
    })
    implicit val v4: Format[TwitterAPI.UserMentions] = Format({
      for {
        screen_name <- (__ \ "screen_name").read[String]
        name <- (__ \ "name").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        indices <- (__ \ "indices").readWithDefault[Seq[Int]](Nil)
      } yield TwitterAPI.UserMentions(screen_name, name, id, id_str, indices)
    }, (x: TwitterAPI.UserMentions) => {
      toJsObject(
        "screen_name" -> Json.toJson(x.screen_name),
        "name" -> Json.toJson(x.name),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "indices" -> Json.toJson(x.indices),
      )
    })
    implicit val v5: Format[TwitterAPI.Entities] = Format({
      for {
        hashtags <- (__ \ "hashtags").readWithDefault[Seq[String]](Nil)
        symbols <- (__ \ "symbols").readWithDefault[Seq[String]](Nil)
        user_mentions <- (__ \ "user_mentions").readWithDefault[Seq[TwitterAPI.UserMentions]](Nil)
        urls <- (__ \ "urls").readWithDefault[Seq[TwitterAPI.Urls]](Nil)
      } yield TwitterAPI.Entities(hashtags, symbols, user_mentions, urls)
    }, (x: TwitterAPI.Entities) => {
      toJsObject(
        "hashtags" -> Json.toJson(x.hashtags),
        "symbols" -> Json.toJson(x.symbols),
        "user_mentions" -> Json.toJson(x.user_mentions),
        "urls" -> Json.toJson(x.urls),
      )
    })
    implicit val v6: Format[TwitterAPI.User] = Format({
      for {
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        name <- (__ \ "name").read[String]
        screen_name <- (__ \ "screen_name").read[String]
        location <- (__ \ "location").read[String]
        description <- (__ \ "description").read[String]
        url <- (__ \ "url").read[String]
        entities <- (__ \ "entities").read[TwitterAPI.UserEntities]
        protected_ <- (__ \ "protected").read[Boolean]
        followers_count <- (__ \ "followers_count").read[Int]
        friends_count <- (__ \ "friends_count").read[Int]
        listed_count <- (__ \ "listed_count").read[Int]
        created_at <- (__ \ "created_at").read[String]
        favourites_count <- (__ \ "favourites_count").read[Int]
        utc_offset <- (__ \ "utc_offset").read[Int]
        time_zone <- (__ \ "time_zone").read[String]
        geo_enabled <- (__ \ "geo_enabled").read[Boolean]
        verified <- (__ \ "verified").read[Boolean]
        statuses_count <- (__ \ "statuses_count").read[Int]
        lang <- (__ \ "lang").read[String]
        contributors_enabled <- (__ \ "contributors_enabled").read[Boolean]
        is_translator <- (__ \ "is_translator").read[Boolean]
        is_translation_enabled <- (__ \ "is_translation_enabled").read[Boolean]
        profile_background_color <- (__ \ "profile_background_color").read[String]
        profile_background_image_url <- (__ \ "profile_background_image_url").read[String]
        profile_background_image_url_https <- (__ \ "profile_background_image_url_https").read[String]
        profile_background_tile <- (__ \ "profile_background_tile").read[Boolean]
        profile_image_url <- (__ \ "profile_image_url").read[String]
        profile_image_url_https <- (__ \ "profile_image_url_https").read[String]
        profile_banner_url <- (__ \ "profile_banner_url").read[String]
        profile_link_color <- (__ \ "profile_link_color").read[String]
        profile_sidebar_border_color <- (__ \ "profile_sidebar_border_color").read[String]
        profile_sidebar_fill_color <- (__ \ "profile_sidebar_fill_color").read[String]
        profile_text_color <- (__ \ "profile_text_color").read[String]
        profile_use_background_image <- (__ \ "profile_use_background_image").read[Boolean]
        has_extended_profile <- (__ \ "has_extended_profile").read[Boolean]
        default_profile <- (__ \ "default_profile").read[Boolean]
        default_profile_image <- (__ \ "default_profile_image").read[Boolean]
        following <- (__ \ "following").read[Boolean]
        follow_request_sent <- (__ \ "follow_request_sent").read[Boolean]
        notifications <- (__ \ "notifications").read[Boolean]
        translator_type <- (__ \ "translator_type").read[String]
      } yield TwitterAPI.User(id, id_str, name, screen_name, location, description, url, entities, protected_,
        followers_count, friends_count, listed_count, created_at, favourites_count, utc_offset, time_zone, geo_enabled,
        verified, statuses_count, lang, contributors_enabled, is_translator, is_translation_enabled,
        profile_background_color, profile_background_image_url, profile_background_image_url_https,
        profile_background_tile, profile_image_url, profile_image_url_https, profile_banner_url, profile_link_color,
        profile_sidebar_border_color, profile_sidebar_fill_color, profile_text_color, profile_use_background_image,
        has_extended_profile, default_profile, default_profile_image, following, follow_request_sent, notifications,
        translator_type)
    }, (x: TwitterAPI.User) => {
      toJsObject(
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "name" -> Json.toJson(x.name),
        "screen_name" -> Json.toJson(x.screen_name),
        "location" -> Json.toJson(x.location),
        "description" -> Json.toJson(x.description),
        "url" -> Json.toJson(x.url),
        "entities" -> Json.toJson(x.entities),
        "protected" -> Json.toJson(x.`protected`),
        "followers_count" -> Json.toJson(x.followers_count),
        "friends_count" -> Json.toJson(x.friends_count),
        "listed_count" -> Json.toJson(x.listed_count),
        "created_at" -> Json.toJson(x.created_at),
        "favourites_count" -> Json.toJson(x.favourites_count),
        "utc_offset" -> Json.toJson(x.utc_offset),
        "time_zone" -> Json.toJson(x.time_zone),
        "geo_enabled" -> Json.toJson(x.geo_enabled),
        "verified" -> Json.toJson(x.verified),
        "statuses_count" -> Json.toJson(x.statuses_count),
        "lang" -> Json.toJson(x.lang),
        "contributors_enabled" -> Json.toJson(x.contributors_enabled),
        "is_translator" -> Json.toJson(x.is_translator),
        "is_translation_enabled" -> Json.toJson(x.is_translation_enabled),
        "profile_background_color" -> Json.toJson(x.profile_background_color),
        "profile_background_image_url" -> Json.toJson(x.profile_background_image_url),
        "profile_background_image_url_https" -> Json.toJson(x.profile_background_image_url_https),
        "profile_background_tile" -> Json.toJson(x.profile_background_tile),
        "profile_image_url" -> Json.toJson(x.profile_image_url),
        "profile_image_url_https" -> Json.toJson(x.profile_image_url_https),
        "profile_banner_url" -> Json.toJson(x.profile_banner_url),
        "profile_link_color" -> Json.toJson(x.profile_link_color),
        "profile_sidebar_border_color" -> Json.toJson(x.profile_sidebar_border_color),
        "profile_sidebar_fill_color" -> Json.toJson(x.profile_sidebar_fill_color),
        "profile_text_color" -> Json.toJson(x.profile_text_color),
        "profile_use_background_image" -> Json.toJson(x.profile_use_background_image),
        "has_extended_profile" -> Json.toJson(x.has_extended_profile),
        "default_profile" -> Json.toJson(x.default_profile),
        "default_profile_image" -> Json.toJson(x.default_profile_image),
        "following" -> Json.toJson(x.following),
        "follow_request_sent" -> Json.toJson(x.follow_request_sent),
        "notifications" -> Json.toJson(x.notifications),
        "translator_type" -> Json.toJson(x.translator_type)
      )
    })
    implicit val v7: Format[TwitterAPI.RetweetedStatus] = Format({
      for {
        created_at <- (__ \ "created_at").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        text <- (__ \ "text").read[String]
        truncated <- (__ \ "truncated").read[Boolean]
        entities <- (__ \ "entities").read[TwitterAPI.Entities]
        source <- (__ \ "source").read[String]
        in_reply_to_status_id <- (__ \ "in_reply_to_status_id").readNullableWithDefault[String](None)
        in_reply_to_status_id_str <- (__ \ "in_reply_to_status_id_str").readNullableWithDefault[String](None)
        in_reply_to_user_id <- (__ \ "in_reply_to_user_id").readNullableWithDefault[String](None)
        in_reply_to_user_id_str <- (__ \ "in_reply_to_user_id_str").readNullableWithDefault[String](None)
        in_reply_to_screen_name <- (__ \ "in_reply_to_screen_name").readNullableWithDefault[String](None)
        user <- (__ \ "user").read[TwitterAPI.User]
        geo <- (__ \ "geo").readNullableWithDefault[String](None)
        coordinates <- (__ \ "coordinates").readNullableWithDefault[String](None)
        place <- (__ \ "place").readNullableWithDefault[String](None)
        contributors <- (__ \ "contributors").readNullableWithDefault[String](None)
        is_quote_status <- (__ \ "is_quote_status").read[Boolean]
        retweet_count <- (__ \ "retweet_count").read[Int]
        favorite_count <- (__ \ "favorite_count").read[Int]
        favorited <- (__ \ "favorited").read[Boolean]
        retweeted <- (__ \ "retweeted").read[Boolean]
        possibly_sensitive <- (__ \ "possibly_sensitive").read[Boolean]
        lang <- (__ \ "lang").read[String]
      } yield TwitterAPI.RetweetedStatus(created_at, id, id_str, text, truncated, entities, source,
        in_reply_to_status_id, in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str,
        in_reply_to_screen_name, user, geo, coordinates, place, contributors, is_quote_status, retweet_count,
        favorite_count, favorited, retweeted, possibly_sensitive, lang)
    }, (x: TwitterAPI.RetweetedStatus) => {
      toJsObject(
        "created_at" -> Json.toJson(x.created_at),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "text" -> Json.toJson(x.text),
        "truncated" -> Json.toJson(x.truncated),
        "entities" -> Json.toJson(x.entities),
        "source" -> Json.toJson(x.source),
        "in_reply_to_status_id" -> Json.toJson(x.in_reply_to_status_id),
        "in_reply_to_status_id_str" -> Json.toJson(x.in_reply_to_status_id_str),
        "in_reply_to_user_id" -> Json.toJson(x.in_reply_to_user_id),
        "in_reply_to_user_id_str" -> Json.toJson(x.in_reply_to_user_id_str),
        "in_reply_to_screen_name" -> Json.toJson(x.in_reply_to_screen_name),
        "user" -> Json.toJson(x.user),
        "coordinates" -> Json.toJson(x.coordinates),
        "place" -> Json.toJson(x.place),
        "contributors" -> Json.toJson(x.contributors),
        "is_quote_status" -> Json.toJson(x.is_quote_status),
        "retweet_count" -> Json.toJson(x.retweet_count),
        "favorite_count" -> Json.toJson(x.favorite_count),
        "favorited" -> Json.toJson(x.favorited),
        "retweeted" -> Json.toJson(x.retweeted),
        "possibly_sensitive" -> Json.toJson(x.possibly_sensitive),
        "lang" -> Json.toJson(x.lang)
      )
    })
    Format({
      for {
        created_at <- (__ \ "created_at").read[String]
        id <- (__ \ "id").read[Long]
        id_str <- (__ \ "id_str").read[String]
        text <- (__ \ "text").read[String]
        truncated <- (__ \ "truncated").read[Boolean]
        entities <- (__ \ "entities").read[TwitterAPI.Entities]
        source <- (__ \ "source").read[String]
        in_reply_to_status_id <- (__ \ "in_reply_to_status_id").readNullableWithDefault[String](None)
        in_reply_to_status_id_str <- (__ \ "in_reply_to_status_id_str").readNullableWithDefault[String](None)
        in_reply_to_user_id <- (__ \ "in_reply_to_user_id").readNullableWithDefault[String](None)
        in_reply_to_user_id_str <- (__ \ "in_reply_to_user_id_str").readNullableWithDefault[String](None)
        in_reply_to_screen_name <- (__ \ "in_reply_to_screen_name").readNullableWithDefault[String](None)
        user <- (__ \ "user").read[TwitterAPI.User]
        geo <- (__ \ "geo").readNullableWithDefault[String](None)
        coordinates <- (__ \ "coordinates").readNullableWithDefault[String](None)
        place <- (__ \ "place").readNullableWithDefault[String](None)
        contributors <- (__ \ "contributors").readNullableWithDefault[String](None)
        retweeted_status <- (__ \ "retweeted_status").read[TwitterAPI.RetweetedStatus]
        is_quote_status <- (__ \ "is_quote_status").read[Boolean]
        retweet_count <- (__ \ "retweet_count").read[Int]
        favorite_count <- (__ \ "favorite_count").read[Int]
        favorited <- (__ \ "favorited").read[Boolean]
        retweeted <- (__ \ "retweeted").read[Boolean]
        possibly_sensitive <- (__ \ "possibly_sensitive").read[Boolean]
        lang <- (__ \ "lang").read[String]
      } yield TwitterAPI.Tweet(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
        in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name, user, geo,
        coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count, favorite_count, favorited,
        retweeted, possibly_sensitive, lang)
    }, (x: TwitterAPI.Tweet) => {
      toJsObject(
        "created_at" -> Json.toJson(x.created_at),
        "id" -> Json.toJson(x.id),
        "id_str" -> Json.toJson(x.id_str),
        "text" -> Json.toJson(x.text),
        "truncated" -> Json.toJson(x.truncated),
        "entities" -> Json.toJson(x.entities),
        "source" -> Json.toJson(x.source),
        "in_reply_to_status_id" -> Json.toJson(x.in_reply_to_status_id),
        "in_reply_to_status_id_str" -> Json.toJson(x.in_reply_to_status_id_str),
        "in_reply_to_user_id" -> Json.toJson(x.in_reply_to_user_id),
        "in_reply_to_user_id_str" -> Json.toJson(x.in_reply_to_user_id_str),
        "in_reply_to_screen_name" -> Json.toJson(x.in_reply_to_screen_name),
        "user" -> Json.toJson(x.user),
        "coordinates" -> Json.toJson(x.coordinates),
        "place" -> Json.toJson(x.place),
        "contributors" -> Json.toJson(x.contributors),
        "retweeted_status" -> Json.toJson(x.retweeted_status),
        "is_quote_status" -> Json.toJson(x.is_quote_status),
        "retweet_count" -> Json.toJson(x.retweet_count),
        "favorite_count" -> Json.toJson(x.favorite_count),
        "favorited" -> Json.toJson(x.favorited),
        "retweeted" -> Json.toJson(x.retweeted),
        "possibly_sensitive" -> Json.toJson(x.possibly_sensitive),
        "lang" -> Json.toJson(x.lang)
      )
    })
  }
  implicit val enumFormat: Format[SuitEnum] = Format(Reads.enumNameReads(SuitEnum), Writes.enumNameWrites)
  implicit val enumADTFormat: Format[SuitADT] = stringFormat("suitadt") {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    (s: String) => suite(s)
  }
  implicit val javaEnumFormat: Format[Suit] = stringFormat("suitenum")(Suit.valueOf)
  implicit val durationFormat: Format[Duration] = stringFormat("instant")(Duration.parse)
  implicit lazy val instantFormat: Format[Instant] = stringFormat("instant")(Instant.parse)
  implicit val localDateTimeFormat: Format[LocalDateTime] = stringFormat("localdatetime")(LocalDateTime.parse)
  implicit val localDateFormat: Format[LocalDate] = stringFormat("localdate")(LocalDate.parse)
  implicit val localTimeFormat: Format[LocalTime] = stringFormat("localtime")(LocalTime.parse)
  implicit val monthDayFormat: Format[MonthDay] = stringFormat("monthday")(MonthDay.parse)
  implicit val offsetDateTimeFormat: Format[OffsetDateTime] = stringFormat("offsetdatetime")(OffsetDateTime.parse)
  implicit val offsetTimeFormat: Format[OffsetTime] = stringFormat("offsettime")(OffsetTime.parse)
  implicit val periodFormat: Format[Period] = stringFormat("period")(Period.parse)
  implicit val yearFormat: Format[Year] = stringFormat("year")(Year.parse)
  implicit val yearMonthFormat: Format[YearMonth] = stringFormat("yearmonth")(YearMonth.parse)
  implicit val zoneOffsetFormat: Format[ZoneOffset] = stringFormat("zoneoffset")(ZoneOffset.of)
  implicit val zoneIdFormat: Format[ZoneId] = stringFormat("zoneid")(ZoneId.of)
  implicit val zonedDateTimeFormat: Format[ZonedDateTime] = stringFormat("zoneddatetime")(ZonedDateTime.parse)

  private[this] def toJsObject(fields: (String, JsValue)*): JsObject = JsObject(fields.filterNot { case (_, v) =>
    (v eq JsNull) || (v.isInstanceOf[JsArray] && v.asInstanceOf[JsArray].value.isEmpty)
  })
}