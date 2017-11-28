package com.github.plokhotnyuk.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.JsonCodecMaker.make
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.github.plokhotnyuk.jsoniter_scala.CustomPlayJsonFormats._

import scala.reflect.io.Streamable

object TwitterAPI {
  case class Entities(
    hashtags: Seq[String],
    symbols: Seq[String],
    user_mentions: Seq[UserMentions],
    urls: Seq[Urls])

  case class UserEntities(
    url: Url,
    description: Url)

  case class RetweetedStatus(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    in_reply_to_status_id: Option[String],
    in_reply_to_status_id_str: Option[String],
    in_reply_to_user_id: Option[String],
    in_reply_to_user_id_str: Option[String],
    in_reply_to_screen_name: Option[String],
    user: User,
    geo: Option[String],
    coordinates: Option[String],
    place: Option[String],
    contributors: Option[String],
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)

  case class Tweet(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    in_reply_to_status_id: Option[String],
    in_reply_to_status_id_str: Option[String],
    in_reply_to_user_id: Option[String],
    in_reply_to_user_id_str: Option[String],
    in_reply_to_screen_name: Option[String],
    user: User,
    geo: Option[String],
    coordinates: Option[String],
    place: Option[String],
    contributors: Option[String],
    retweeted_status: RetweetedStatus,
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)

  case class Url(urls: Seq[Urls])

  case class Urls(
    url: String,
    expanded_url: String,
    display_url: String,
    indices: Seq[Int])

  case class User(
    id: Long,
    id_str: String,
    name: String,
    screen_name: String,
    location: String,
    description: String,
    url: String,
    entities: UserEntities,
    `protected`: Boolean,
    followers_count: Int,
    friends_count: Int,
    listed_count: Int,
    created_at: String,
    favourites_count: Int,
    utc_offset: Int,
    time_zone: String,
    geo_enabled: Boolean,
    verified: Boolean,
    statuses_count: Int,
    lang: String,
    contributors_enabled: Boolean,
    is_translator: Boolean,
    is_translation_enabled: Boolean,
    profile_background_color: String,
    profile_background_image_url: String,
    profile_background_image_url_https: String,
    profile_background_tile: Boolean,
    profile_image_url: String,
    profile_image_url_https: String,
    profile_banner_url: String,
    profile_link_color: String,
    profile_sidebar_border_color: String,
    profile_sidebar_fill_color: String,
    profile_text_color: String,
    profile_use_background_image: Boolean,
    has_extended_profile: Boolean,
    default_profile: Boolean,
    default_profile_image: Boolean,
    following: Boolean,
    follow_request_sent: Boolean,
    notifications: Boolean,
    translator_type: String)

  case class UserMentions(
    screen_name: String,
    name: String,
    id: Long,
    id_str: String,
    indices: Seq[Int])

  val format: Format[Seq[Tweet]] = {
    implicit lazy val format7: OFormat[Urls] = Json.format[Urls]
    implicit lazy val format6: OFormat[Url] = Json.format[Url]
    implicit lazy val format5: OFormat[UserEntities] = Json.format[UserEntities]
    implicit lazy val format4: OFormat[UserMentions] = Json.format[UserMentions]
    implicit lazy val format3: OFormat[Entities] = Json.format[Entities]
    implicit lazy val format20: OFormat[(Long, String, String, String, String, String, String,
      UserEntities, Boolean, Int, Int, Int, String, Int, Int, String, Boolean, Boolean, Int, String, Boolean)] =
      ((__ \ "id").format[Long] and
        (__ \ "id_str").format[String] and
        (__ \ "name").format[String] and
        (__ \ "screen_name").format[String] and
        (__ \ "location").format[String] and
        (__ \ "description").format[String] and
        (__ \ "url").format[String] and
        (__ \ "entities").format[UserEntities] and
        (__ \ "protected").format[Boolean] and
        (__ \ "followers_count").format[Int] and
        (__ \ "friends_count").format[Int] and
        (__ \ "listed_count").format[Int] and
        (__ \ "created_at").format[String] and
        (__ \ "favourites_count").format[Int] and
        (__ \ "utc_offset").format[Int] and
        (__ \ "time_zone").format[String] and
        (__ \ "geo_enabled").format[Boolean] and
        (__ \ "verified").format[Boolean] and
        (__ \ "statuses_count").format[Int] and
        (__ \ "lang").format[String] and
        (__ \ "contributors_enabled").format[Boolean]).tupled
    implicit lazy val format21: OFormat[(Boolean, Boolean, String, String, String, Boolean, String,
      String, String, String, String, String, String, Boolean, Boolean, Boolean, Boolean, Boolean,
      Boolean, Boolean, String)] =
      ((__ \ "is_translator").format[Boolean] and
        (__ \ "is_translation_enabled").format[Boolean] and
        (__ \ "profile_background_color").format[String] and
        (__ \ "profile_background_image_url").format[String] and
        (__ \ "profile_background_image_url_https").format[String] and
        (__ \ "profile_background_tile").format[Boolean] and
        (__ \ "profile_image_url").format[String] and
        (__ \ "profile_image_url_https").format[String] and
        (__ \ "profile_banner_url").format[String] and
        (__ \ "profile_link_color").format[String] and
        (__ \ "profile_sidebar_border_color").format[String] and
        (__ \ "profile_sidebar_fill_color").format[String] and
        (__ \ "profile_text_color").format[String] and
        (__ \ "profile_use_background_image").format[Boolean] and
        (__ \ "has_extended_profile").format[Boolean] and
        (__ \ "default_profile").format[Boolean] and
        (__ \ "default_profile_image").format[Boolean] and
        (__ \ "following").format[Boolean] and
        (__ \ "follow_request_sent").format[Boolean] and
        (__ \ "notifications").format[Boolean] and
        (__ \ "translator_type").format[String]).tupled
    implicit lazy val format2: Format[User] = (format20 and format21).apply({
      case ((id, id_str, name, screen_name, location, description, url, entities, _protected, followers_count,
      friends_count, listed_count, created_at, favourites_count, utc_offset, time_zone, geo_enabled, verified,
      statuses_count, lang, contributors_enabled),
      (is_translator, is_translation_enabled, profile_background_color, profile_background_image_url,
      profile_background_image_url_https, profile_background_tile, profile_image_url, profile_image_url_https,
      profile_banner_url, profile_link_color, profile_sidebar_border_color, profile_sidebar_fill_color,
      profile_text_color, profile_use_background_image, has_extended_profile, default_profile, default_profile_image,
      following, follow_request_sent, notifications, translator_type)) =>
        User(id, id_str, name, screen_name, location, description, url, entities, _protected, followers_count,
          friends_count, listed_count, created_at, favourites_count, utc_offset, time_zone, geo_enabled, verified,
          statuses_count, lang, contributors_enabled, is_translator, is_translation_enabled, profile_background_color,
          profile_background_image_url, profile_background_image_url_https, profile_background_tile, profile_image_url,
          profile_image_url_https, profile_banner_url, profile_link_color, profile_sidebar_border_color,
          profile_sidebar_fill_color, profile_text_color, profile_use_background_image, has_extended_profile,
          default_profile, default_profile_image, following, follow_request_sent, notifications, translator_type)
    }, u => ((u.id, u.id_str, u.name, u.screen_name, u.location, u.description, u.url, u.entities, u.`protected`,
      u.followers_count, u.friends_count, u.listed_count, u.created_at, u.favourites_count, u.utc_offset, u.time_zone,
      u.geo_enabled, u.verified, u.statuses_count, u.lang, u.contributors_enabled),
      (u.is_translator, u.is_translation_enabled, u.profile_background_color, u.profile_background_image_url,
        u.profile_background_image_url_https, u.profile_background_tile, u.profile_image_url, u.profile_image_url_https,
        u.profile_banner_url, u.profile_link_color, u.profile_sidebar_border_color, u.profile_sidebar_fill_color,
        u.profile_text_color, u.profile_use_background_image, u.has_extended_profile, u.default_profile,
        u.default_profile_image, u.following, u.follow_request_sent, u.notifications, u.translator_type)))
    implicit lazy val format10: OFormat[(String, Long, String, String, Boolean, Entities, String, Option[String],
      Option[String], Option[String], Option[String], Option[String])] =
      ((__ \ "created_at").format[String] and
        (__ \ "id").format[Long] and
        (__ \ "id_str").format[String] and
        (__ \ "text").format[String] and
        (__ \ "truncated").format[Boolean] and
        (__ \ "entities").format[Entities] and
        (__ \ "source").format[String] and
        (__ \ "in_reply_to_status_id").format[Option[String]] and
        (__ \ "in_reply_to_status_id_str").format[Option[String]] and
        (__ \ "in_reply_to_user_id").format[Option[String]] and
        (__ \ "in_reply_to_user_id_str").format[Option[String]] and
        (__ \ "in_reply_to_screen_name").format[Option[String]]).tupled
    implicit lazy val format11: OFormat[(User, Option[String], Option[String], Option[String], Option[String],
      Boolean, Int, Int, Boolean, Boolean, Boolean, String)] =
      ((__ \ "user").format[User] and
        (__ \ "geo").format[Option[String]] and
        (__ \ "coordinates").format[Option[String]] and
        (__ \ "place").format[Option[String]] and
        (__ \ "contributors").format[Option[String]] and
        (__ \ "is_quote_status").format[Boolean] and
        (__ \ "retweet_count").format[Int] and
        (__ \ "favorite_count").format[Int] and
        (__ \ "favorited").format[Boolean] and
        (__ \ "retweeted").format[Boolean] and
        (__ \ "possibly_sensitive").format[Boolean] and
        (__ \ "lang").format[String]).tupled
    implicit lazy val format1: Format[RetweetedStatus] = (format10 and format11).apply({
      case ((created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
      in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name),
      (user, geo, coordinates, place, contributors, is_quote_status, retweet_count, favorite_count,
      favorited, retweeted, possibly_sensitive, lang)) =>
        RetweetedStatus(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
          in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name,
          user, geo, coordinates, place, contributors, is_quote_status, retweet_count, favorite_count,
          favorited, retweeted, possibly_sensitive, lang)
    }, s => ((s.created_at, s.id, s.id_str, s.text, s.truncated, s.entities, s.source, s.in_reply_to_status_id,
      s.in_reply_to_status_id_str, s.in_reply_to_user_id, s.in_reply_to_user_id_str, s.in_reply_to_screen_name),
      (s.user, s.geo, s.coordinates, s.place, s.contributors, s.is_quote_status, s.retweet_count,
        s.favorite_count, s.favorited, s.retweeted, s.possibly_sensitive, s.lang)))
    implicit lazy val format00: OFormat[(String, Long, String, String, Boolean, Entities, String, Option[String],
      Option[String], Option[String], Option[String], Option[String])] =
      ((__ \ "created_at").format[String] and
        (__ \ "id").format[Long] and
        (__ \ "id_str").format[String] and
        (__ \ "text").format[String] and
        (__ \ "truncated").format[Boolean] and
        (__ \ "entities").format[Entities] and
        (__ \ "source").format[String] and
        (__ \ "in_reply_to_status_id").format[Option[String]] and
        (__ \ "in_reply_to_status_id_str").format[Option[String]] and
        (__ \ "in_reply_to_user_id").format[Option[String]] and
        (__ \ "in_reply_to_user_id_str").format[Option[String]] and
        (__ \ "in_reply_to_screen_name").format[Option[String]]).tupled
    implicit lazy val format01: OFormat[(User, Option[String], Option[String], Option[String], Option[String],
      RetweetedStatus, Boolean, Int, Int, Boolean, Boolean, Boolean, String)] =
      ((__ \ "user").format[User] and
        (__ \ "geo").format[Option[String]] and
        (__ \ "coordinates").format[Option[String]] and
        (__ \ "place").format[Option[String]] and
        (__ \ "contributors").format[Option[String]] and
        (__ \ "retweeted_status").format[RetweetedStatus] and
        (__ \ "is_quote_status").format[Boolean] and
        (__ \ "retweet_count").format[Int] and
        (__ \ "favorite_count").format[Int] and
        (__ \ "favorited").format[Boolean] and
        (__ \ "retweeted").format[Boolean] and
        (__ \ "possibly_sensitive").format[Boolean] and
        (__ \ "lang").format[String]).tupled
    implicit lazy val format0: Format[Tweet] = (format00 and format01).apply({
      case ((created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
      in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name),
      (user, geo, coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count, favorite_count,
      favorited, retweeted, possibly_sensitive, lang)) =>
        Tweet(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
          in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name,
          user, geo, coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count, favorite_count,
          favorited, retweeted, possibly_sensitive, lang)
    }, t => ((t.created_at, t.id, t.id_str, t.text, t.truncated, t.entities, t.source, t.in_reply_to_status_id,
      t.in_reply_to_status_id_str, t.in_reply_to_user_id, t.in_reply_to_user_id_str, t.in_reply_to_screen_name),
      (t.user, t.geo, t.coordinates, t.place, t.contributors, t.retweeted_status, t.is_quote_status, t.retweet_count,
        t.favorite_count, t.favorited, t.retweeted, t.possibly_sensitive, t.lang)))
    Format[Seq[Tweet]](
      Reads[Seq[Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[Tweet](format0)))),
      Writes[Seq[Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)(format0)))))
  }
  val codec: JsonCodec[Seq[Tweet]] = make[Seq[Tweet]](CodecMakerConfig())
  val json: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("twitter_api_response.json"))
  val compactJson: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("twitter_api_compact_response.json"))
  val obj: Seq[Tweet] = JsonReader.read(codec, json)
}