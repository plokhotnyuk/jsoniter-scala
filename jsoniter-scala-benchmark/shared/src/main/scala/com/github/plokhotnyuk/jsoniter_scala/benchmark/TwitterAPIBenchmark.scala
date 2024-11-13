package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.Setup

abstract class TwitterAPIBenchmark extends CommonParams {
  var obj: Seq[TwitterAPI.Tweet] = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String =
    """[
      |  {
      |    "created_at": "Thu Apr 06 15:28:43 +0000 2017",
      |    "id": 850007368138018817,
      |    "id_str": "850007368138018817",
      |    "text": "RT @TwitterDev: 1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP",
      |    "truncated": false,
      |    "entities": {
      |      "hashtags": [],
      |      "symbols": [],
      |      "user_mentions": [
      |        {
      |          "screen_name": "TwitterDev",
      |          "name": "TwitterDev",
      |          "id": 2244994945,
      |          "id_str": "2244994945",
      |          "indices": [
      |            3,
      |            14
      |          ]
      |        }
      |      ],
      |      "urls": [
      |        {
      |          "url": "https://t.co/XweGngmxlP",
      |          "expanded_url": "https://cards.twitter.com/cards/18ce53wgo4h/3xo1c",
      |          "display_url": "cards.twitter.com/cards/18ce53wg…",
      |          "indices": [
      |            94,
      |            117
      |          ]
      |        }
      |      ]
      |    },
      |    "source": "<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>",
      |    "in_reply_to_status_id": null,
      |    "in_reply_to_status_id_str": null,
      |    "in_reply_to_user_id": null,
      |    "in_reply_to_user_id_str": null,
      |    "in_reply_to_screen_name": null,
      |    "user": {
      |      "id": 6253282,
      |      "id_str": "6253282",
      |      "name": "Twitter API",
      |      "screen_name": "twitterapi",
      |      "location": "San Francisco, CA",
      |      "description": "The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.",
      |      "url": "http://t.co/78pYTvWfJd",
      |      "entities": {
      |        "url": {
      |          "urls": [
      |            {
      |              "url": "http://t.co/78pYTvWfJd",
      |              "expanded_url": "https://dev.twitter.com",
      |              "display_url": "dev.twitter.com",
      |              "indices": [
      |                0,
      |                22
      |              ]
      |            }
      |          ]
      |        },
      |        "description": {
      |          "urls": []
      |        }
      |      },
      |      "protected": false,
      |      "followers_count": 6172353,
      |      "friends_count": 46,
      |      "listed_count": 13091,
      |      "created_at": "Wed May 23 06:01:13 +0000 2007",
      |      "favourites_count": 26,
      |      "utc_offset": -25200,
      |      "time_zone": "Pacific Time (US & Canada)",
      |      "geo_enabled": true,
      |      "verified": true,
      |      "statuses_count": 3583,
      |      "lang": "en",
      |      "contributors_enabled": false,
      |      "is_translator": false,
      |      "is_translation_enabled": false,
      |      "profile_background_color": "C0DEED",
      |      "profile_background_image_url": "http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png",
      |      "profile_background_image_url_https": "https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png",
      |      "profile_background_tile": true,
      |      "profile_image_url": "http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png",
      |      "profile_image_url_https": "https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png",
      |      "profile_banner_url": "https://pbs.twimg.com/profile_banners/6253282/1431474710",
      |      "profile_link_color": "0084B4",
      |      "profile_sidebar_border_color": "C0DEED",
      |      "profile_sidebar_fill_color": "DDEEF6",
      |      "profile_text_color": "333333",
      |      "profile_use_background_image": true,
      |      "has_extended_profile": false,
      |      "default_profile": false,
      |      "default_profile_image": false,
      |      "following": true,
      |      "follow_request_sent": false,
      |      "notifications": false,
      |      "translator_type": "regular"
      |    },
      |    "geo": null,
      |    "coordinates": null,
      |    "place": null,
      |    "contributors": null,
      |    "retweeted_status": {
      |      "created_at": "Thu Apr 06 15:24:15 +0000 2017",
      |      "id": 850006245121695744,
      |      "id_str": "850006245121695744",
      |      "text": "1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP",
      |      "truncated": false,
      |      "entities": {
      |        "hashtags": [],
      |        "symbols": [],
      |        "user_mentions": [],
      |        "urls": [
      |          {
      |            "url": "https://t.co/XweGngmxlP",
      |            "expanded_url": "https://cards.twitter.com/cards/18ce53wgo4h/3xo1c",
      |            "display_url": "cards.twitter.com/cards/18ce53wg…",
      |            "indices": [
      |              78,
      |              101
      |            ]
      |          }
      |        ]
      |      },
      |      "source": "<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>",
      |      "in_reply_to_status_id": null,
      |      "in_reply_to_status_id_str": null,
      |      "in_reply_to_user_id": null,
      |      "in_reply_to_user_id_str": null,
      |      "in_reply_to_screen_name": null,
      |      "user": {
      |        "id": 2244994945,
      |        "id_str": "2244994945",
      |        "name": "TwitterDev",
      |        "screen_name": "TwitterDev",
      |        "location": "Internet",
      |        "description": "Your official source for Twitter Platform news, updates & events. Need technical help? Visit https://t.co/mGHnxZCxkt ⌨️  #TapIntoTwitter",
      |        "url": "https://t.co/66w26cua1O",
      |        "entities": {
      |          "url": {
      |            "urls": [
      |              {
      |                "url": "https://t.co/66w26cua1O",
      |                "expanded_url": "https://dev.twitter.com/",
      |                "display_url": "dev.twitter.com",
      |                "indices": [
      |                  0,
      |                  23
      |                ]
      |              }
      |            ]
      |          },
      |          "description": {
      |            "urls": [
      |              {
      |                "url": "https://t.co/mGHnxZCxkt",
      |                "expanded_url": "https://twittercommunity.com/",
      |                "display_url": "twittercommunity.com",
      |                "indices": [
      |                  93,
      |                  116
      |                ]
      |              }
      |            ]
      |          }
      |        },
      |        "protected": false,
      |        "followers_count": 465425,
      |        "friends_count": 1523,
      |        "listed_count": 1168,
      |        "created_at": "Sat Dec 14 04:35:55 +0000 2013",
      |        "favourites_count": 2098,
      |        "utc_offset": -25200,
      |        "time_zone": "Pacific Time (US & Canada)",
      |        "geo_enabled": true,
      |        "verified": true,
      |        "statuses_count": 3031,
      |        "lang": "en",
      |        "contributors_enabled": false,
      |        "is_translator": false,
      |        "is_translation_enabled": false,
      |        "profile_background_color": "FFFFFF",
      |        "profile_background_image_url": "http://abs.twimg.com/images/themes/theme1/bg.png",
      |        "profile_background_image_url_https": "https://abs.twimg.com/images/themes/theme1/bg.png",
      |        "profile_background_tile": false,
      |        "profile_image_url": "http://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png",
      |        "profile_image_url_https": "https://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png",
      |        "profile_banner_url": "https://pbs.twimg.com/profile_banners/2244994945/1396995246",
      |        "profile_link_color": "0084B4",
      |        "profile_sidebar_border_color": "FFFFFF",
      |        "profile_sidebar_fill_color": "DDEEF6",
      |        "profile_text_color": "333333",
      |        "profile_use_background_image": false,
      |        "has_extended_profile": false,
      |        "default_profile": false,
      |        "default_profile_image": false,
      |        "following": true,
      |        "follow_request_sent": false,
      |        "notifications": false,
      |        "translator_type": "regular"
      |      },
      |      "geo": null,
      |      "coordinates": null,
      |      "place": null,
      |      "contributors": null,
      |      "is_quote_status": false,
      |      "retweet_count": 284,
      |      "favorite_count": 399,
      |      "favorited": false,
      |      "retweeted": false,
      |      "possibly_sensitive": false,
      |      "lang": "en"
      |    },
      |    "is_quote_status": false,
      |    "retweet_count": 284,
      |    "favorite_count": 0,
      |    "favorited": false,
      |    "retweeted": false,
      |    "possibly_sensitive": false,
      |    "lang": "en"
      |  },
      |  {
      |    "created_at": "Mon Apr 03 16:09:50 +0000 2017",
      |    "id": 848930551989915648,
      |    "id_str": "848930551989915648",
      |    "text": "RT @TwitterMktg: Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYn…",
      |    "truncated": false,
      |    "entities": {
      |      "hashtags": [],
      |      "symbols": [],
      |      "user_mentions": [
      |        {
      |          "screen_name": "TwitterMktg",
      |          "name": "Twitter Marketing",
      |          "id": 357750891,
      |          "id_str": "357750891",
      |          "indices": [
      |            3,
      |            15
      |          ]
      |        }
      |      ],
      |      "urls": []
      |    },
      |    "source": "<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>",
      |    "in_reply_to_status_id": null,
      |    "in_reply_to_status_id_str": null,
      |    "in_reply_to_user_id": null,
      |    "in_reply_to_user_id_str": null,
      |    "in_reply_to_screen_name": null,
      |    "user": {
      |      "id": 6253282,
      |      "id_str": "6253282",
      |      "name": "Twitter API",
      |      "screen_name": "twitterapi",
      |      "location": "San Francisco, CA",
      |      "description": "The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.",
      |      "url": "http://t.co/78pYTvWfJd",
      |      "entities": {
      |        "url": {
      |          "urls": [
      |            {
      |              "url": "http://t.co/78pYTvWfJd",
      |              "expanded_url": "https://dev.twitter.com",
      |              "display_url": "dev.twitter.com",
      |              "indices": [
      |                0,
      |                22
      |              ]
      |            }
      |          ]
      |        },
      |        "description": {
      |          "urls": []
      |        }
      |      },
      |      "protected": false,
      |      "followers_count": 6172353,
      |      "friends_count": 46,
      |      "listed_count": 13091,
      |      "created_at": "Wed May 23 06:01:13 +0000 2007",
      |      "favourites_count": 26,
      |      "utc_offset": -25200,
      |      "time_zone": "Pacific Time (US & Canada)",
      |      "geo_enabled": true,
      |      "verified": true,
      |      "statuses_count": 3583,
      |      "lang": "en",
      |      "contributors_enabled": false,
      |      "is_translator": false,
      |      "is_translation_enabled": false,
      |      "profile_background_color": "C0DEED",
      |      "profile_background_image_url": "http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png",
      |      "profile_background_image_url_https": "https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png",
      |      "profile_background_tile": true,
      |      "profile_image_url": "http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png",
      |      "profile_image_url_https": "https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png",
      |      "profile_banner_url": "https://pbs.twimg.com/profile_banners/6253282/1431474710",
      |      "profile_link_color": "0084B4",
      |      "profile_sidebar_border_color": "C0DEED",
      |      "profile_sidebar_fill_color": "DDEEF6",
      |      "profile_text_color": "333333",
      |      "profile_use_background_image": true,
      |      "has_extended_profile": false,
      |      "default_profile": false,
      |      "default_profile_image": false,
      |      "following": true,
      |      "follow_request_sent": false,
      |      "notifications": false,
      |      "translator_type": "regular"
      |    },
      |    "geo": null,
      |    "coordinates": null,
      |    "place": null,
      |    "contributors": null,
      |    "retweeted_status": {
      |      "created_at": "Mon Apr 03 16:05:05 +0000 2017",
      |      "id": 848929357519241216,
      |      "id_str": "848929357519241216",
      |      "text": "Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYndqWfQw",
      |      "truncated": false,
      |      "entities": {
      |        "hashtags": [],
      |        "symbols": [],
      |        "user_mentions": [],
      |        "urls": [
      |          {
      |            "url": "https://t.co/rpYndqWfQw",
      |            "expanded_url": "https://cards.twitter.com/cards/5wzucr/3x700",
      |            "display_url": "cards.twitter.com/cards/5wzucr/3…",
      |            "indices": [
      |              105,
      |              128
      |            ]
      |          }
      |        ]
      |      },
      |      "source": "<a href=\"https://ads.twitter.com\" rel=\"nofollow\">Twitter Ads</a>",
      |      "in_reply_to_status_id": null,
      |      "in_reply_to_status_id_str": null,
      |      "in_reply_to_user_id": null,
      |      "in_reply_to_user_id_str": null,
      |      "in_reply_to_screen_name": null,
      |      "user": {
      |        "id": 357750891,
      |        "id_str": "357750891",
      |        "name": "Twitter Marketing",
      |        "screen_name": "TwitterMktg",
      |        "location": "Twitter HQ ",
      |        "description": "Twitter’s place for marketers, agencies, and creative thinkers ⭐ Bringing you insights, news, updates, and inspiration. Visit @TwitterAdsHelp for Ads support.",
      |        "url": "https://t.co/Tfo4moo92y",
      |        "entities": {
      |          "url": {
      |            "urls": [
      |              {
      |                "url": "https://t.co/Tfo4moo92y",
      |                "expanded_url": "https://marketing.twitter.com",
      |                "display_url": "marketing.twitter.com",
      |                "indices": [
      |                  0,
      |                  23
      |                ]
      |              }
      |            ]
      |          },
      |          "description": {
      |            "urls": []
      |          }
      |        },
      |        "protected": false,
      |        "followers_count": 924546,
      |        "friends_count": 661,
      |        "listed_count": 3893,
      |        "created_at": "Thu Aug 18 21:08:15 +0000 2011",
      |        "favourites_count": 1934,
      |        "utc_offset": -25200,
      |        "time_zone": "Pacific Time (US & Canada)",
      |        "geo_enabled": true,
      |        "verified": true,
      |        "statuses_count": 6329,
      |        "lang": "en",
      |        "contributors_enabled": false,
      |        "is_translator": false,
      |        "is_translation_enabled": false,
      |        "profile_background_color": "C0DEED",
      |        "profile_background_image_url": "http://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png",
      |        "profile_background_image_url_https": "https://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png",
      |        "profile_background_tile": true,
      |        "profile_image_url": "http://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg",
      |        "profile_image_url_https": "https://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg",
      |        "profile_banner_url": "https://pbs.twimg.com/profile_banners/357750891/1487188210",
      |        "profile_link_color": "19CF86",
      |        "profile_sidebar_border_color": "FFFFFF",
      |        "profile_sidebar_fill_color": "DDEEF6",
      |        "profile_text_color": "333333",
      |        "profile_use_background_image": true,
      |        "has_extended_profile": false,
      |        "default_profile": false,
      |        "default_profile_image": false,
      |        "following": false,
      |        "follow_request_sent": false,
      |        "notifications": false,
      |        "translator_type": "none"
      |      },
      |      "geo": null,
      |      "coordinates": null,
      |      "place": null,
      |      "contributors": null,
      |      "is_quote_status": false,
      |      "retweet_count": 111,
      |      "favorite_count": 162,
      |      "favorited": false,
      |      "retweeted": false,
      |      "possibly_sensitive": false,
      |      "lang": "en"
      |    },
      |    "is_quote_status": false,
      |    "retweet_count": 111,
      |    "favorite_count": 0,
      |    "favorited": false,
      |    "retweeted": false,
      |    "possibly_sensitive": false,
      |    "lang": "en"
      |  }
      |]""".stripMargin
  var compactJsonString1: String = """[{"created_at":"Thu Apr 06 15:28:43 +0000 2017","id":850007368138018817,"id_str":"850007368138018817","text":"RT @TwitterDev: 1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP","truncated":false,"entities":{"user_mentions":[{"screen_name":"TwitterDev","name":"TwitterDev","id":2244994945,"id_str":"2244994945","indices":[3,14]}],"urls":[{"url":"https://t.co/XweGngmxlP","expanded_url":"https://cards.twitter.com/cards/18ce53wgo4h/3xo1c","display_url":"cards.twitter.com/cards/18ce53wg…","indices":[94,117]}]},"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","user":{"id":6253282,"id_str":"6253282","name":"Twitter API","screen_name":"twitterapi","location":"San Francisco, CA","description":"The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.","url":"http://t.co/78pYTvWfJd","entities":{"url":{"urls":[{"url":"http://t.co/78pYTvWfJd","expanded_url":"https://dev.twitter.com","display_url":"dev.twitter.com","indices":[0,22]}]},"description":{}},"protected":false,"followers_count":6172353,"friends_count":46,"listed_count":13091,"created_at":"Wed May 23 06:01:13 +0000 2007","favourites_count":26,"utc_offset":-25200,"time_zone":"Pacific Time (US & Canada)","geo_enabled":true,"verified":true,"statuses_count":3583,"lang":"en","contributors_enabled":false,"is_translator":false,"is_translation_enabled":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_tile":true,"profile_image_url":"http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_banner_url":"https://pbs.twimg.com/profile_banners/6253282/1431474710","profile_link_color":"0084B4","profile_sidebar_border_color":"C0DEED","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"has_extended_profile":false,"default_profile":false,"default_profile_image":false,"following":true,"follow_request_sent":false,"notifications":false,"translator_type":"regular"},"retweeted_status":{"created_at":"Thu Apr 06 15:24:15 +0000 2017","id":850006245121695744,"id_str":"850006245121695744","text":"1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP","truncated":false,"entities":{"urls":[{"url":"https://t.co/XweGngmxlP","expanded_url":"https://cards.twitter.com/cards/18ce53wgo4h/3xo1c","display_url":"cards.twitter.com/cards/18ce53wg…","indices":[78,101]}]},"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","user":{"id":2244994945,"id_str":"2244994945","name":"TwitterDev","screen_name":"TwitterDev","location":"Internet","description":"Your official source for Twitter Platform news, updates & events. Need technical help? Visit https://t.co/mGHnxZCxkt ⌨️  #TapIntoTwitter","url":"https://t.co/66w26cua1O","entities":{"url":{"urls":[{"url":"https://t.co/66w26cua1O","expanded_url":"https://dev.twitter.com/","display_url":"dev.twitter.com","indices":[0,23]}]},"description":{"urls":[{"url":"https://t.co/mGHnxZCxkt","expanded_url":"https://twittercommunity.com/","display_url":"twittercommunity.com","indices":[93,116]}]}},"protected":false,"followers_count":465425,"friends_count":1523,"listed_count":1168,"created_at":"Sat Dec 14 04:35:55 +0000 2013","favourites_count":2098,"utc_offset":-25200,"time_zone":"Pacific Time (US & Canada)","geo_enabled":true,"verified":true,"statuses_count":3031,"lang":"en","contributors_enabled":false,"is_translator":false,"is_translation_enabled":false,"profile_background_color":"FFFFFF","profile_background_image_url":"http://abs.twimg.com/images/themes/theme1/bg.png","profile_background_image_url_https":"https://abs.twimg.com/images/themes/theme1/bg.png","profile_background_tile":false,"profile_image_url":"http://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png","profile_banner_url":"https://pbs.twimg.com/profile_banners/2244994945/1396995246","profile_link_color":"0084B4","profile_sidebar_border_color":"FFFFFF","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":false,"has_extended_profile":false,"default_profile":false,"default_profile_image":false,"following":true,"follow_request_sent":false,"notifications":false,"translator_type":"regular"},"is_quote_status":false,"retweet_count":284,"favorite_count":399,"favorited":false,"retweeted":false,"possibly_sensitive":false,"lang":"en"},"is_quote_status":false,"retweet_count":284,"favorite_count":0,"favorited":false,"retweeted":false,"possibly_sensitive":false,"lang":"en"},{"created_at":"Mon Apr 03 16:09:50 +0000 2017","id":848930551989915648,"id_str":"848930551989915648","text":"RT @TwitterMktg: Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYn…","truncated":false,"entities":{"user_mentions":[{"screen_name":"TwitterMktg","name":"Twitter Marketing","id":357750891,"id_str":"357750891","indices":[3,15]}]},"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","user":{"id":6253282,"id_str":"6253282","name":"Twitter API","screen_name":"twitterapi","location":"San Francisco, CA","description":"The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.","url":"http://t.co/78pYTvWfJd","entities":{"url":{"urls":[{"url":"http://t.co/78pYTvWfJd","expanded_url":"https://dev.twitter.com","display_url":"dev.twitter.com","indices":[0,22]}]},"description":{}},"protected":false,"followers_count":6172353,"friends_count":46,"listed_count":13091,"created_at":"Wed May 23 06:01:13 +0000 2007","favourites_count":26,"utc_offset":-25200,"time_zone":"Pacific Time (US & Canada)","geo_enabled":true,"verified":true,"statuses_count":3583,"lang":"en","contributors_enabled":false,"is_translator":false,"is_translation_enabled":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_tile":true,"profile_image_url":"http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_banner_url":"https://pbs.twimg.com/profile_banners/6253282/1431474710","profile_link_color":"0084B4","profile_sidebar_border_color":"C0DEED","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"has_extended_profile":false,"default_profile":false,"default_profile_image":false,"following":true,"follow_request_sent":false,"notifications":false,"translator_type":"regular"},"retweeted_status":{"created_at":"Mon Apr 03 16:05:05 +0000 2017","id":848929357519241216,"id_str":"848929357519241216","text":"Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYndqWfQw","truncated":false,"entities":{"urls":[{"url":"https://t.co/rpYndqWfQw","expanded_url":"https://cards.twitter.com/cards/5wzucr/3x700","display_url":"cards.twitter.com/cards/5wzucr/3…","indices":[105,128]}]},"source":"<a href=\"https://ads.twitter.com\" rel=\"nofollow\">Twitter Ads</a>","user":{"id":357750891,"id_str":"357750891","name":"Twitter Marketing","screen_name":"TwitterMktg","location":"Twitter HQ ","description":"Twitter’s place for marketers, agencies, and creative thinkers ⭐ Bringing you insights, news, updates, and inspiration. Visit @TwitterAdsHelp for Ads support.","url":"https://t.co/Tfo4moo92y","entities":{"url":{"urls":[{"url":"https://t.co/Tfo4moo92y","expanded_url":"https://marketing.twitter.com","display_url":"marketing.twitter.com","indices":[0,23]}]},"description":{}},"protected":false,"followers_count":924546,"friends_count":661,"listed_count":3893,"created_at":"Thu Aug 18 21:08:15 +0000 2011","favourites_count":1934,"utc_offset":-25200,"time_zone":"Pacific Time (US & Canada)","geo_enabled":true,"verified":true,"statuses_count":6329,"lang":"en","contributors_enabled":false,"is_translator":false,"is_translation_enabled":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png","profile_background_tile":true,"profile_image_url":"http://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg","profile_image_url_https":"https://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg","profile_banner_url":"https://pbs.twimg.com/profile_banners/357750891/1487188210","profile_link_color":"19CF86","profile_sidebar_border_color":"FFFFFF","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"has_extended_profile":false,"default_profile":false,"default_profile_image":false,"following":false,"follow_request_sent":false,"notifications":false,"translator_type":"none"},"is_quote_status":false,"retweet_count":111,"favorite_count":162,"favorited":false,"retweeted":false,"possibly_sensitive":false,"lang":"en"},"is_quote_status":false,"retweet_count":111,"favorite_count":0,"favorited":false,"retweeted":false,"possibly_sensitive":false,"lang":"en"}]"""
  var compactJsonString2: String = """[{"created_at":"Thu Apr 06 15:28:43 +0000 2017","entities":{"urls":[{"display_url":"cards.twitter.com/cards/18ce53wg…","expanded_url":"https://cards.twitter.com/cards/18ce53wgo4h/3xo1c","indices":[94,117],"url":"https://t.co/XweGngmxlP"}],"user_mentions":[{"id":2244994945,"id_str":"2244994945","indices":[3,14],"name":"TwitterDev","screen_name":"TwitterDev"}]},"favorite_count":0,"favorited":false,"id":850007368138018817,"id_str":"850007368138018817","is_quote_status":false,"lang":"en","possibly_sensitive":false,"retweet_count":284,"retweeted":false,"retweeted_status":{"created_at":"Thu Apr 06 15:24:15 +0000 2017","entities":{"urls":[{"display_url":"cards.twitter.com/cards/18ce53wg…","expanded_url":"https://cards.twitter.com/cards/18ce53wgo4h/3xo1c","indices":[78,101],"url":"https://t.co/XweGngmxlP"}]},"favorite_count":399,"favorited":false,"id":850006245121695744,"id_str":"850006245121695744","is_quote_status":false,"lang":"en","possibly_sensitive":false,"retweet_count":284,"retweeted":false,"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","text":"1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP","truncated":false,"user":{"contributors_enabled":false,"created_at":"Sat Dec 14 04:35:55 +0000 2013","default_profile":false,"default_profile_image":false,"description":"Your official source for Twitter Platform news, updates & events. Need technical help? Visit https://t.co/mGHnxZCxkt ⌨️  #TapIntoTwitter","entities":{"description":{"urls":[{"display_url":"twittercommunity.com","expanded_url":"https://twittercommunity.com/","indices":[93,116],"url":"https://t.co/mGHnxZCxkt"}]},"url":{"urls":[{"display_url":"dev.twitter.com","expanded_url":"https://dev.twitter.com/","indices":[0,23],"url":"https://t.co/66w26cua1O"}]}},"favourites_count":2098,"follow_request_sent":false,"followers_count":465425,"following":true,"friends_count":1523,"geo_enabled":true,"has_extended_profile":false,"id":2244994945,"id_str":"2244994945","is_translation_enabled":false,"is_translator":false,"lang":"en","listed_count":1168,"location":"Internet","name":"TwitterDev","notifications":false,"profile_background_color":"FFFFFF","profile_background_image_url":"http://abs.twimg.com/images/themes/theme1/bg.png","profile_background_image_url_https":"https://abs.twimg.com/images/themes/theme1/bg.png","profile_background_tile":false,"profile_banner_url":"https://pbs.twimg.com/profile_banners/2244994945/1396995246","profile_image_url":"http://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/530814764687949824/npQQVkq8_normal.png","profile_link_color":"0084B4","profile_sidebar_border_color":"FFFFFF","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":false,"protected":false,"screen_name":"TwitterDev","statuses_count":3031,"time_zone":"Pacific Time (US & Canada)","translator_type":"regular","url":"https://t.co/66w26cua1O","utc_offset":-25200,"verified":true}},"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","text":"RT @TwitterDev: 1/ Today we’re sharing our vision for the future of the Twitter API platform!\nhttps://t.co/XweGngmxlP","truncated":false,"user":{"contributors_enabled":false,"created_at":"Wed May 23 06:01:13 +0000 2007","default_profile":false,"default_profile_image":false,"description":"The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.","entities":{"description":{},"url":{"urls":[{"display_url":"dev.twitter.com","expanded_url":"https://dev.twitter.com","indices":[0,22],"url":"http://t.co/78pYTvWfJd"}]}},"favourites_count":26,"follow_request_sent":false,"followers_count":6172353,"following":true,"friends_count":46,"geo_enabled":true,"has_extended_profile":false,"id":6253282,"id_str":"6253282","is_translation_enabled":false,"is_translator":false,"lang":"en","listed_count":13091,"location":"San Francisco, CA","name":"Twitter API","notifications":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_tile":true,"profile_banner_url":"https://pbs.twimg.com/profile_banners/6253282/1431474710","profile_image_url":"http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_link_color":"0084B4","profile_sidebar_border_color":"C0DEED","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"protected":false,"screen_name":"twitterapi","statuses_count":3583,"time_zone":"Pacific Time (US & Canada)","translator_type":"regular","url":"http://t.co/78pYTvWfJd","utc_offset":-25200,"verified":true}},{"created_at":"Mon Apr 03 16:09:50 +0000 2017","entities":{"user_mentions":[{"id":357750891,"id_str":"357750891","indices":[3,15],"name":"Twitter Marketing","screen_name":"TwitterMktg"}]},"favorite_count":0,"favorited":false,"id":848930551989915648,"id_str":"848930551989915648","is_quote_status":false,"lang":"en","possibly_sensitive":false,"retweet_count":111,"retweeted":false,"retweeted_status":{"created_at":"Mon Apr 03 16:05:05 +0000 2017","entities":{"urls":[{"display_url":"cards.twitter.com/cards/5wzucr/3…","expanded_url":"https://cards.twitter.com/cards/5wzucr/3x700","indices":[105,128],"url":"https://t.co/rpYndqWfQw"}]},"favorite_count":162,"favorited":false,"id":848929357519241216,"id_str":"848929357519241216","is_quote_status":false,"lang":"en","possibly_sensitive":false,"retweet_count":111,"retweeted":false,"source":"<a href=\"https://ads.twitter.com\" rel=\"nofollow\">Twitter Ads</a>","text":"Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYndqWfQw","truncated":false,"user":{"contributors_enabled":false,"created_at":"Thu Aug 18 21:08:15 +0000 2011","default_profile":false,"default_profile_image":false,"description":"Twitter’s place for marketers, agencies, and creative thinkers ⭐ Bringing you insights, news, updates, and inspiration. Visit @TwitterAdsHelp for Ads support.","entities":{"description":{},"url":{"urls":[{"display_url":"marketing.twitter.com","expanded_url":"https://marketing.twitter.com","indices":[0,23],"url":"https://t.co/Tfo4moo92y"}]}},"favourites_count":1934,"follow_request_sent":false,"followers_count":924546,"following":false,"friends_count":661,"geo_enabled":true,"has_extended_profile":false,"id":357750891,"id_str":"357750891","is_translation_enabled":false,"is_translator":false,"lang":"en","listed_count":3893,"location":"Twitter HQ ","name":"Twitter Marketing","notifications":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/662767273/jvmxdpdrplhxcw8yvkv2.png","profile_background_tile":true,"profile_banner_url":"https://pbs.twimg.com/profile_banners/357750891/1487188210","profile_image_url":"http://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg","profile_image_url_https":"https://pbs.twimg.com/profile_images/800953549697888256/UlXXL5h5_normal.jpg","profile_link_color":"19CF86","profile_sidebar_border_color":"FFFFFF","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"protected":false,"screen_name":"TwitterMktg","statuses_count":6329,"time_zone":"Pacific Time (US & Canada)","translator_type":"none","url":"https://t.co/Tfo4moo92y","utc_offset":-25200,"verified":true}},"source":"<a href=\"http://twitter.com\" rel=\"nofollow\">Twitter Web Client</a>","text":"RT @TwitterMktg: Starting today, businesses can request and share locations when engaging with people in Direct Messages. https://t.co/rpYn…","truncated":false,"user":{"contributors_enabled":false,"created_at":"Wed May 23 06:01:13 +0000 2007","default_profile":false,"default_profile_image":false,"description":"The Real Twitter API. I tweet about API changes, service issues and happily answer questions about Twitter and our API. Don't get an answer? It's on my website.","entities":{"description":{},"url":{"urls":[{"display_url":"dev.twitter.com","expanded_url":"https://dev.twitter.com","indices":[0,22],"url":"http://t.co/78pYTvWfJd"}]}},"favourites_count":26,"follow_request_sent":false,"followers_count":6172353,"following":true,"friends_count":46,"geo_enabled":true,"has_extended_profile":false,"id":6253282,"id_str":"6253282","is_translation_enabled":false,"is_translator":false,"lang":"en","listed_count":13091,"location":"San Francisco, CA","name":"Twitter API","notifications":false,"profile_background_color":"C0DEED","profile_background_image_url":"http://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/656927849/miyt9dpjz77sc0w3d4vj.png","profile_background_tile":true,"profile_banner_url":"https://pbs.twimg.com/profile_banners/6253282/1431474710","profile_image_url":"http://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_image_url_https":"https://pbs.twimg.com/profile_images/2284174872/7df3h38zabcvjylnyfe3_normal.png","profile_link_color":"0084B4","profile_sidebar_border_color":"C0DEED","profile_sidebar_fill_color":"DDEEF6","profile_text_color":"333333","profile_use_background_image":true,"protected":false,"screen_name":"twitterapi","statuses_count":3583,"time_zone":"Pacific Time (US & Canada)","translator_type":"regular","url":"http://t.co/78pYTvWfJd","utc_offset":-25200,"verified":true}}]"""

  @Setup
  def setup(): Unit = {
    jsonBytes = jsonString.getBytes(UTF_8)
    obj = readFromArray[Seq[TwitterAPI.Tweet]](jsonBytes)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}
