/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.rallyhealth.weepickle.v1.implicits.dropDefault
import scala.collection.immutable.Seq

object TwitterAPI {
  @dropDefault
  case class Urls(
    url: String,
    expanded_url: String,
    display_url: String,
    indices: Seq[Int] = Nil)

  @dropDefault
  case class Url(urls: Seq[Urls] = Nil)

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

  @dropDefault
  case class Entities(
    hashtags: Seq[String] = Nil,
    symbols: Seq[String] = Nil,
    user_mentions: Seq[UserMentions] = Nil,
    urls: Seq[Urls] = Nil)

  @dropDefault
  case class RetweetedStatus(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    in_reply_to_status_id: Option[String] = None,
    in_reply_to_status_id_str: Option[String] = None,
    in_reply_to_user_id: Option[String] = None,
    in_reply_to_user_id_str: Option[String] = None,
    in_reply_to_screen_name: Option[String] = None,
    user: User,
    geo: Option[String] = None,
    coordinates: Option[String] = None,
    place: Option[String] = None,
    contributors: Option[String] = None,
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)

  @dropDefault
  case class UserMentions(
    screen_name: String,
    name: String,
    id: Long,
    id_str: String,
    indices: Seq[Int] = Nil)

  @dropDefault
  case class Tweet(
    created_at: String,
    id: Long,
    id_str: String,
    text: String,
    truncated: Boolean,
    entities: Entities,
    source: String,
    in_reply_to_status_id: Option[String] = None,
    in_reply_to_status_id_str: Option[String] = None,
    in_reply_to_user_id: Option[String] = None,
    in_reply_to_user_id_str: Option[String] = None,
    in_reply_to_screen_name: Option[String] = None,
    user: User,
    geo: Option[String] = None,
    coordinates: Option[String] = None,
    place: Option[String] = None,
    contributors: Option[String] = None,
    retweeted_status: RetweetedStatus,
    is_quote_status: Boolean,
    retweet_count: Int,
    favorite_count: Int,
    favorited: Boolean,
    retweeted: Boolean,
    possibly_sensitive: Boolean,
    lang: String)
}