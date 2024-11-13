package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.avsystem.commons.serialization.transientDefault
import com.rallyhealth.weepickle.v1.implicits.dropDefault

object OpenRTB {
  @dropDefault
  case class BidRequest (
    id: String,
    @transientDefault imp: List[Imp] = Nil,
    @transientDefault site: Option[Site] = None,
    @transientDefault app: Option[App] = None,
    @transientDefault device: Option[Device] = None,
    @transientDefault user: Option[User] = None,
    @transientDefault test: Int = 0,
    @transientDefault at: Int = 2,
    @transientDefault tmax: Option[Int] = None,
    @transientDefault wset: List[String] = Nil,
    @transientDefault bset: List[String] = Nil,
    @transientDefault allimps: Int = 0,
    @transientDefault cur: List[String] = Nil,
    @transientDefault wlang: List[String] = Nil,
    @transientDefault bcat: List[String] = Nil,
    @transientDefault badv: List[String] = Nil,
    @transientDefault bapp: List[String] = Nil,
    @transientDefault source: Option[Source] = None,
    @transientDefault reqs: Option[Reqs] = None,
    /*@transientDefault ext: Option[BidRequestExt] = None*/)

  @dropDefault
  case class Source(
    @transientDefault fd: Option[Int] = None,
    @transientDefault tid: Option[String] = None,
    @transientDefault pchain: Option[String] = None,
    /*@transientDefault ext: Option[SourceExt] = None*/)

  @dropDefault
  case class Reqs(
    coppa: Int,
    /*@transientDefault ext: Option[ReqsExt] = None*/)

  @dropDefault
  case class Imp(
    id: String,
    @transientDefault metric: List[Metric] = Nil,
    @transientDefault banner: Option[Banner] = None,
    @transientDefault video: Option[Video] = None,
    @transientDefault audio: Option[Audio] = None,
    @transientDefault native: Option[Native] = None,
    @transientDefault pmp: Option[Pmp] = None,
    @transientDefault displaymanager: Option[String] = None,
    @transientDefault displaymanagerver: Option[String] = None,
    @transientDefault instl: Int = 0,
    @transientDefault tagid: Option[String] = None,
    @transientDefault bidfloor: Double = 0.0,
    @transientDefault bidfloorcur: String = "USD",
    @transientDefault clickbrowser: Option[Int] = None,
    @transientDefault secure: Int = 0,
    @transientDefault iframebuster: List[String] = Nil,
    @transientDefault exp: Option[Int] = None,
    /*@transientDefault ext: Option[ImpExt] = None*/)

  @dropDefault
  case class Metric(
    `type`: String,
    value: Double,
    @transientDefault vendor: Option[String] = None,
    /*@transientDefault ext: Option[MetricExt] = None*/)

  @dropDefault
  case class Banner(
    @transientDefault format: Option[Format] = None,
    @transientDefault w: Option[Int] = None,
    @transientDefault h: Option[Int] = None,
    @transientDefault wmax: Option[Int] = None,
    @transientDefault hmax: Option[Int] = None,
    @transientDefault wmin: Option[Int] = None,
    @transientDefault hmin: Option[Int] = None,
    @transientDefault btype: List[Int] = Nil,
    @transientDefault battr: List[Int] = Nil,
    @transientDefault pos: Option[Int] = None,
    @transientDefault mimes: List[String] = Nil,
    @transientDefault topframe: Option[Int] = None,
    @transientDefault expdir: List[Int] = Nil,
    @transientDefault api: List[Int] = Nil,
    @transientDefault id: Option[String] = None,
    @transientDefault vcm: Option[Int] = None,
    /*@transientDefault ext: Option[BannerExt] = None*/)

  @dropDefault
  case class Video(
    @transientDefault mimes: List[String] = Nil,
    @transientDefault minduration: Option[Int] = None,
    @transientDefault maxduration: Option[Int] = None,
    @transientDefault protocols: List[Int] = Nil,
    @transientDefault protocol: Option[Int] = None,
    @transientDefault w: Option[Int] = None,
    @transientDefault h: Option[Int] = None,
    @transientDefault startdelay: Option[Int] = None,
    @transientDefault placement: Option[Int] = None,
    @transientDefault linearity: Option[Int] = None,
    @transientDefault skip: Option[Int] = None,
    @transientDefault skipmin: Int = 0,
    @transientDefault skipafter: Int = 0,
    @transientDefault sequence: Option[Int] = None,
    @transientDefault battr: List[Int] = Nil,
    @transientDefault maxextended: Option[Int] = None,
    @transientDefault minbitrate: Option[Int] = None,
    @transientDefault maxbitrate: Option[Int] = None,
    @transientDefault boxingallowed: Int = 1,
    @transientDefault playbackmethod: List[Int] = Nil,
    @transientDefault playbackend: Option[Int] = None,
    @transientDefault delivery: List[Int] = Nil,
    @transientDefault pos: Option[Int] = None,
    @transientDefault companionad: List[Banner] = Nil,
    @transientDefault api: List[Int] = Nil,
    @transientDefault companiontype: List[Int] = Nil,
    /*@transientDefault ext: Option[VideoExt] = None*/)

  @dropDefault
  case class Audio(
    @transientDefault mimes: List[String] = Nil,
    @transientDefault minduration: Option[Int] = None,
    @transientDefault maxduration: Option[Int] = None,
    @transientDefault protocols: List[Int] = Nil,
    @transientDefault startdelay: Option[Int] = None,
    @transientDefault sequence: Option[Int] = None,
    @transientDefault battr: List[Int] = Nil,
    @transientDefault maxextended: Option[Int] = None,
    @transientDefault minbitrate: Option[Int] = None,
    @transientDefault maxbitrate: Option[Int] = None,
    @transientDefault delivery: List[Int] = Nil,
    @transientDefault companionad: List[Banner] = Nil,
    @transientDefault api: List[Int] = Nil,
    @transientDefault companiontype: List[Int] = Nil,
    @transientDefault maxseq: Option[Int] = None,
    @transientDefault feed: Option[Int] = None,
    @transientDefault stitched: Option[Int] = None,
    @transientDefault nvol: Option[Int] = None,
    /*@transientDefault ext: Option[AudioExt] = None*/)

  @dropDefault
  case class Native(
    request: String,
    @transientDefault ver: Option[String] = None,
    @transientDefault api: List[Int] = Nil,
    @transientDefault battr: List[Int] = Nil,
    /*@transientDefault ext: Option[NativeExt] = None*/)

  @dropDefault
  case class Format(
    @transientDefault w: Option[Int] = None,
    @transientDefault h: Option[Int] = None,
    @transientDefault wratio: Option[Int] = None,
    @transientDefault hratio: Option[Int] = None,
    @transientDefault wmin: Option[Int] = None,
    /*@transientDefault ext: Option[FormatExt] = None*/)

  @dropDefault
  case class Pmp(
    @transientDefault private_auction: Int = 0,
    @transientDefault deals: List[Deal] = Nil,
    /*@transientDefault ext: Option[PmpExt] = None*/)

  @dropDefault
  case class Deal(
    id: String,
    @transientDefault bidfloor: Double = 0.0,
    @transientDefault bidfloorcur: String = "USD",
    @transientDefault at: Option[Int] = None,
    @transientDefault wseat: List[String] = Nil,
    @transientDefault wadomain: List[String] = Nil,
    /*@transientDefault ext: Option[DealExt] = None*/)

  @dropDefault
  case class Site(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault domain: Option[String] = None,
    @transientDefault cat: List[String] = Nil,
    @transientDefault sectioncat: List[String] = Nil,
    @transientDefault pagecat: List[String] = Nil,
    @transientDefault page: Option[String] = None,
    @transientDefault ref: Option[String] = None,
    @transientDefault search: Option[String] = None,
    @transientDefault mobile: Option[Int] = None,
    @transientDefault privacypolicy: Option[Int] = None,
    @transientDefault publisher: Option[Publisher] = None,
    @transientDefault content: Option[Content] = None,
    @transientDefault keywords: Option[String] = None,
    /*@transientDefault ext: Option[SiteExt] = None*/)

  @dropDefault
  case class App(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault bundle: Option[String] = None,
    @transientDefault domain: Option[String] = None,
    @transientDefault storeurl: Option[String] = None,
    @transientDefault cat: List[String] = Nil,
    @transientDefault sectioncat: List[String] = Nil,
    @transientDefault pagecat: List[String] = Nil,
    @transientDefault ver: Option[String] = None,
    @transientDefault privacypolicy: Option[Int] = None,
    @transientDefault paid: Option[Int] = None,
    @transientDefault publisher: Option[Publisher] = None,
    @transientDefault content: Option[Content] = None,
    @transientDefault keywords: Option[String] = None,
    /*@transientDefault ext: Option[AppExt] = None*/)

  @dropDefault
  case class Publisher(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault cat: List[String] = Nil,
    @transientDefault domain: Option[String] = None,
    /*@transientDefault ext: Option[PublisherExt] = None*/)

  @dropDefault
  case class Content(
    @transientDefault id: Option[String] = None,
    @transientDefault episode: Option[Int] = None,
    @transientDefault title: Option[String] = None,
    @transientDefault series: Option[String] = None,
    @transientDefault season: Option[String] = None,
    @transientDefault artist: Option[String] = None,
    @transientDefault genre: Option[String] = None,
    @transientDefault album: Option[String] = None,
    @transientDefault isrc: Option[String] = None,
    @transientDefault producer: Option[Producer] = None,
    @transientDefault url: Option[String] = None,
    @transientDefault cat: List[String] = Nil,
    @transientDefault prodq: Option[Int] = None,
    @transientDefault videoquality: Option[Int] = None,
    @transientDefault context: Option[Int] = None,
    @transientDefault contentrating: Option[String] = None,
    @transientDefault userrating: Option[String] = None,
    @transientDefault qagmediarating: Option[Int] = None,
    @transientDefault keywords: Option[String] = None,
    @transientDefault livestream: Option[Int] = None,
    @transientDefault sourcerelationship: Option[Int] = None,
    @transientDefault len: Option[Int] = None,
    @transientDefault language: Option[String] = None,
    @transientDefault embeddable: Option[Int] = None,
    @transientDefault data: Option[Data] = None,
    /*@transientDefault ext: Option[ContentExt] = None*/)

  @dropDefault
  case class Producer(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault cat: List[String] = Nil,
    @transientDefault domain: Option[String] = None,
    /*@transientDefault ext: Option[ProducerExt] = None*/)

  @dropDefault
  case class Device(
    @transientDefault ua: Option[String] = None,
    @transientDefault geo: Option[Geo] = None,
    @transientDefault dnt: Option[Int] = None,
    @transientDefault lmt: Option[Int] = None,
    @transientDefault ip: Option[String] = None,
    @transientDefault devicetype: Option[Int] = None,
    @transientDefault make: Option[String] = None,
    @transientDefault model: Option[String] = None,
    @transientDefault os: Option[String] = None,
    @transientDefault osv: Option[String] = None,
    @transientDefault hwv: Option[String] = None,
    @transientDefault h: Option[Int] = None,
    @transientDefault w: Option[Int] = None,
    @transientDefault ppi: Option[Int] = None,
    @transientDefault pxratio: Option[Double] = None,
    @transientDefault js: Option[Int] = None,
    @transientDefault geofetch: Option[Int] = None,
    @transientDefault flashver: Option[String] = None,
    @transientDefault language: Option[String] = None,
    @transientDefault carrier: Option[String] = None,
    @transientDefault mccmnc: Option[String] = None,
    @transientDefault connectiontype: Option[Int] = None,
    @transientDefault ifa: Option[String] = None,
    @transientDefault didsha1: Option[String] = None,
    @transientDefault didmd5: Option[String] = None,
    @transientDefault dpidsha1: Option[String] = None,
    @transientDefault dpidmd5: Option[String] = None,
    @transientDefault macsha1: Option[String] = None,
    @transientDefault macmd5: Option[String] = None,
    /*@transientDefault ext: Option[DeviceExt] = None*/)

  @dropDefault
  case class Geo(
    @transientDefault lat: Option[Double] = None,
    @transientDefault lon: Option[Double] = None,
    @transientDefault `type`: Option[Int] = None,
    @transientDefault accuracy: Option[Int] = None,
    @transientDefault lastfix: Option[Int] = None,
    @transientDefault ipservice: Option[Int] = None,
    @transientDefault country: Option[String] = None,
    @transientDefault region: Option[String] = None,
    @transientDefault regionfips104: Option[String] = None,
    @transientDefault metro: Option[String] = None,
    @transientDefault city: Option[String] = None,
    @transientDefault zip: Option[String] = None,
    @transientDefault utcoffset: Option[String] = None,
    /*@transientDefault ext: Option[GeoExt] = None*/)

  @dropDefault
  case class User(
    @transientDefault id: Option[String] = None,
    @transientDefault buyeruid: Option[String] = None,
    @transientDefault yob: Option[Int] = None,
    @transientDefault gender: Option[String] = None,
    @transientDefault keywords: Option[String] = None,
    @transientDefault customdata: Option[String] = None,
    @transientDefault geo: Option[Geo] = None,
    @transientDefault data: Option[Data] = None,
    /*@transientDefault ext: Option[UserExt] = None*/)

  @dropDefault
  case class Data(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault segment: List[Segment] = Nil,
    /*@transientDefault ext: Option[DataExt] = None*/)

  @dropDefault
  case class Segment(
    @transientDefault id: Option[String] = None,
    @transientDefault name: Option[String] = None,
    @transientDefault value: Option[String] = None,
    /*@transientDefault ext: Option[DataExt] = None*/)
}