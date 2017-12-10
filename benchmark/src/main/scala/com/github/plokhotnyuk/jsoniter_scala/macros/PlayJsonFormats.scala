package com.github.plokhotnyuk.jsoniter_scala.macros

import julienrf.json.derived.flat
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.immutable.{BitSet, HashMap, IntMap, LongMap}
import scala.collection.{breakOut, mutable}

object PlayJsonFormats {
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format[MissingReqFields]
  val anyRefsFormat: OFormat[AnyRefs] = Json.format[AnyRefs]
  val arraysFormat: OFormat[Arrays] = {
    implicit val v1: Format[Array[BigInt]] = Format(
      Reads[Array[BigInt]](js => JsSuccess(js.as[Array[JsNumber]].map(_.value.toBigInt()))),
      Writes[Array[BigInt]](a => JsArray(a.map(v => JsNumber(BigDecimal(v))))))
    Json.format[Arrays]
  }
  val bitSetsFormat: OFormat[BitSets] = {
    implicit val v1: Reads[BitSet] = Reads[BitSet](js => JsSuccess(BitSet(js.as[Array[Int]]: _*)))
    implicit val v2: Reads[mutable.BitSet] =
      Reads[mutable.BitSet](js => JsSuccess(mutable.BitSet(js.as[Array[Int]]: _*)))
    Json.format[BitSets]
  }
  val iterablesFormat: OFormat[Iterables] = Json.format[Iterables]
  val mutableIterablesFormat: OFormat[MutableIterables] = Json.format[MutableIterables]
  val mapsFormat: OFormat[Maps] = {
    implicit val v1: OFormat[HashMap[String, Double]] = OFormat(
      Reads[HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
      OWrites[HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m)))
    implicit val v2: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat(
      Reads[Map[Int, HashMap[Long, Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
        (java.lang.Integer.parseInt(kv._1), kv._2.map { kv1 =>
          (java.lang.Long.parseLong(kv1._1), kv1._2)
        }(breakOut): HashMap[Long, Double])
      }(breakOut))),
      OWrites[Map[Int, HashMap[Long, Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
        (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2)))
      })))
    Json.format[Maps]
  }
  val mutableMapsFormat: OFormat[MutableMaps] = {
    implicit val v1: OFormat[mutable.HashMap[String, Double]] = OFormat(
      Reads[mutable.HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
      OWrites[mutable.HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m.toMap)))
    implicit val v2: OFormat[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] = OFormat(
      Reads[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] { js =>
        JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
          val v = kv._2
          val newV = new mutable.OpenHashMap[Long, Double](v.size)
          v.foreach(kv1 => newV.update(java.lang.Long.parseLong(kv1._1), kv1._2))
          (java.lang.Integer.parseInt(kv._1), newV)
        }(breakOut): mutable.Map[Int, mutable.OpenHashMap[Long, Double]])
      },
      OWrites[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] { m =>
        Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
          (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
        }(breakOut): Map[String, Map[String, Double]])
      })
    Json.format[MutableMaps]
  }
  val intAndLongMapsFormat: OFormat[IntAndLongMaps] = {
    implicit val v1: OFormat[IntMap[Double]] = OFormat(
      Reads[IntMap[Double]](js => JsSuccess(js.as[Map[String, Double]].map { kv =>
        (java.lang.Integer.parseInt(kv._1), kv._2)
      }(breakOut))),
      OWrites[IntMap[Double]](m => Json.toJsObject[Map[String, Double]](m.map { kv => (kv._1.toString, kv._2) })))
    implicit val v2: OFormat[mutable.LongMap[LongMap[Double]]] = OFormat(
      Reads[mutable.LongMap[LongMap[Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
        (java.lang.Long.parseLong(kv._1), kv._2.map { kv1 =>
          (java.lang.Long.parseLong(kv1._1), kv1._2)
        }(breakOut): LongMap[Double])
      }(breakOut))),
      OWrites[mutable.LongMap[LongMap[Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
        (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
      }(breakOut): Map[String, Map[String, Double]])))
    Json.format[IntAndLongMaps]
  }
  val primitivesFormat: OFormat[Primitives] = {
    implicit val v1: Format[Char] = Format(
      Reads[Char](js => JsSuccess(js.as[String].charAt(0))),
      Writes[Char](c => JsString(c.toString)))
    Json.format[Primitives]
  }
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format[ExtractFields]
  val adtFormat: OFormat[AdtBase] = {
    implicit lazy val v1: OFormat[A] = Json.format[A]
    implicit lazy val v2: OFormat[B] = Json.format[B]
    implicit lazy val v3: OFormat[C] = Json.format[C]
    implicit lazy val v4: OFormat[AdtBase] = flat.oformat((__ \ "type").format[String])
    v4
  }
  val googleMapsAPIFormat: OFormat[DistanceMatrix] = {
    implicit val v1: OFormat[Value] = Json.format[Value]
    implicit val v2: OFormat[Elements] = Json.format[Elements]
    implicit val v3: OFormat[Rows] = Json.format[Rows]
    Json.format[DistanceMatrix]
  }
  val twitterAPIFormat: Format[Seq[Tweet]] = {
    implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
      override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

      override def writes(o: Option[T]): JsValue = o match {
        case Some(t) => implicitly[Writes[T]].writes(t)
        case None => JsNull
      }
    }
    implicit val v1: OFormat[Urls] = Json.format[Urls]
    implicit val v2: OFormat[Url] = Json.format[Url]
    implicit val v3: OFormat[UserEntities] = Json.format[UserEntities]
    implicit val v4: OFormat[UserMentions] = Json.format[UserMentions]
    implicit val v5: OFormat[Entities] = Json.format[Entities]
    implicit val v6: Format[User] =
      (((__ \ "id").format[Long] and
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
        (__ \ "contributors_enabled").format[Boolean]).tupled and
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
          (__ \ "translator_type").format[String]).tupled).apply({
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
    implicit val v7: Format[RetweetedStatus] =
      (((__ \ "created_at").format[String] and
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
        (__ \ "in_reply_to_screen_name").format[Option[String]]).tupled and
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
          (__ \ "lang").format[String]).tupled).apply({
        case ((created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
        in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name),
        (user, geo, coordinates, place, contributors, is_quote_status, retweet_count, favorite_count, favorited,
        retweeted, possibly_sensitive, lang)) =>
          RetweetedStatus(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
            in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name, user,
            geo, coordinates, place, contributors, is_quote_status, retweet_count, favorite_count, favorited, retweeted,
            possibly_sensitive, lang)
      }, s => ((s.created_at, s.id, s.id_str, s.text, s.truncated, s.entities, s.source, s.in_reply_to_status_id,
        s.in_reply_to_status_id_str, s.in_reply_to_user_id, s.in_reply_to_user_id_str, s.in_reply_to_screen_name),
        (s.user, s.geo, s.coordinates, s.place, s.contributors, s.is_quote_status, s.retweet_count, s.favorite_count,
          s.favorited, s.retweeted, s.possibly_sensitive, s.lang)))
    implicit val v8: Format[Tweet] =
      (((__ \ "created_at").format[String] and
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
        (__ \ "in_reply_to_screen_name").format[Option[String]]).tupled and
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
          (__ \ "lang").format[String]).tupled).apply({
        case ((created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
        in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name),
        (user, geo, coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count, favorite_count,
        favorited, retweeted, possibly_sensitive, lang)) =>
          Tweet(created_at, id, id_str, text, truncated, entities, source, in_reply_to_status_id,
            in_reply_to_status_id_str, in_reply_to_user_id, in_reply_to_user_id_str, in_reply_to_screen_name,
            user, geo, coordinates, place, contributors, retweeted_status, is_quote_status, retweet_count,
            favorite_count, favorited, retweeted, possibly_sensitive, lang)
      }, t => ((t.created_at, t.id, t.id_str, t.text, t.truncated, t.entities, t.source, t.in_reply_to_status_id,
        t.in_reply_to_status_id_str, t.in_reply_to_user_id, t.in_reply_to_user_id_str, t.in_reply_to_screen_name),
        (t.user, t.geo, t.coordinates, t.place, t.contributors, t.retweeted_status, t.is_quote_status, t.retweet_count,
          t.favorite_count, t.favorited, t.retweeted, t.possibly_sensitive, t.lang)))
    Format[Seq[Tweet]](
      Reads[Seq[Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[Tweet](v8)))),
      Writes[Seq[Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)(v8)))))
  }
}