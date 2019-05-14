package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.transientDefault
import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.collection.immutable.Seq
import scala.reflect.io.Streamable

object TwitterAPI {
  case class Urls(
    url: String,
    expanded_url: String,
    display_url: String,
    @transientDefault indices: Seq[Int] = Seq.empty)

  case class Url(@transientDefault urls: Seq[Urls] = Seq.empty)

  case class UserEntities(
    url: Url,
    description: Url)

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

  case class Entities(
    @transientDefault hashtags: Seq[String] = Seq.empty,
    @transientDefault symbols: Seq[String] = Seq.empty,
    @transientDefault user_mentions: Seq[UserMentions] = Seq.empty,
    @transientDefault urls: Seq[Urls] = Seq.empty)

  case class RetweetedStatus(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    @transientDefault in_reply_to_status_id: Option[String] = None,
    @transientDefault in_reply_to_status_id_str: Option[String] = None,
    @transientDefault in_reply_to_user_id: Option[String] = None,
    @transientDefault in_reply_to_user_id_str: Option[String] = None,
    @transientDefault in_reply_to_screen_name: Option[String] = None,
    user: User,
    @transientDefault geo: Option[String] = None,
    @transientDefault coordinates: Option[String] = None,
    @transientDefault place: Option[String] = None,
    @transientDefault contributors: Option[String] = None,
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)

  case class UserMentions(
    screen_name: String,
    name: String,
    id: Long,
    id_str: String,
    @transientDefault indices: Seq[Int] = Seq.empty)

  case class Tweet(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    @transientDefault in_reply_to_status_id: Option[String] = None,
    @transientDefault in_reply_to_status_id_str: Option[String] = None,
    @transientDefault in_reply_to_user_id: Option[String] = None,
    @transientDefault in_reply_to_user_id_str: Option[String] = None,
    @transientDefault in_reply_to_screen_name: Option[String] = None,
    user: User,
    @transientDefault geo: Option[String] = None,
    @transientDefault coordinates: Option[String] = None,
    @transientDefault place: Option[String] = None,
    @transientDefault contributors: Option[String] = None,
    retweeted_status: RetweetedStatus,
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)

  var jsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("twitter_api_response.json"))
  var jsonString: String = new String(jsonBytes, UTF_8)
  var compactJsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("twitter_api_compact_response.json"))
  var compactJsonString: String = new String(compactJsonBytes, UTF_8)
}

abstract class TwitterAPIBenchmark extends CommonParams {
  var obj: Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes.length + 100/*to avoid possible out of bounds error*/)
}