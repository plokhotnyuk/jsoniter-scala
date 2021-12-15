package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.github.kag0.ninny._
import io.github.kag0.ninny.ast._
import scala.util.Failure
import OpenRTB._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import GoogleMapsAPI._
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import ujson.Str
import ujson.Obj
import ujson.Arr
import ujson.Num
import ujson.False
import ujson.True

object NinnyFormats {
  def translateUJson(ujson: _root_.ujson.Value): JsonValue = ujson match {
    case Str(value) => JsonString(value)
    case Obj(value) => JsonObject(value.view.mapValues(translateUJson).toMap)
    case Arr(value) => JsonArray(value.map(translateUJson).toSeq)
    case Num(value) => JsonDouble(value)
    case False => JsonFalse
    case True => JsonTrue
    case _root_.ujson.Null => JsonNull
  }

implicit val jsoniterWritingCodec = new JsonValueCodec[JsonValue] {
    def decodeValue(in: JsonReader, default: JsonValue) = ???
    def encodeValue(x: JsonValue, out: JsonWriter) = x match {
        case JsonObject(values) => 
            out.writeObjectStart()
            values.foreach{case (k, v) => 
                out.writeKey(k)
                encodeValue(v, out)
            }
            out.writeObjectEnd()
        case JsonDecimal(preciseValue) => 
          if(preciseValue.precision == 0)
            out.writeVal(preciseValue.toBigInt)
          else
            out.writeVal(preciseValue)
        case JsonDouble(value) => 
          val longValue = value.toLong
          if(longValue == value) out.writeVal(longValue)
          else out.writeVal(value)
        case JsonBlob(value) => out.writeBase64UrlVal(value.unsafeArray.asInstanceOf[Array[Byte]], false)
        case JsonString(value) => out.writeVal(value)
        case JsonFalse => out.writeVal(false)
        case JsonTrue => out.writeVal(true)
        case JsonArray(values) => 
          out.writeArrayStart()
          values.foreach(encodeValue(_, out))
          out.writeArrayEnd()
        case JsonNull => out.writeNull()
    }

    def nullValue = JsonNull
  }


  lazy implicit val adtBaseToJson: ToSomeJson[ADTBase] = {
      case x: X => adtXToJson.toSome(x) + ("type" -> "X")
      case y: Y => adtYToJson.toSome(y) + ("type" -> "Y")
      case z: Z => adtZToJson.toSome(z) + ("type" -> "Z")
  }

  implicit val adtXToJson = ToJson.auto[X]
  implicit val adtYToJson = ToJson.auto[Y]
  implicit val adtZToJson = ToJson.auto[Z]
  
  lazy implicit val adtBaseFromJson = FromJson.fromSome(json => 
    (json / "type").to[String].flatMap {
      case "X" => adtXFromJson.from(json)
      case "Y" => adtYFromJson.from(json)
      case "Z" => adtZFromJson.from(json)
    }
  )

  implicit val adtXFromJson: FromJson[X] = FromJson.auto[X]
  implicit val adtYFromJson: FromJson[Y] = FromJson.auto[Y]
  lazy implicit val adtZFromJson: FromJson[Z] = FromJson.auto[Z]

  // openrtb
  implicit val publisherToFromJson = ToAndFromJson.auto[Publisher]
  implicit val sourceToFromJson = ToAndFromJson.auto[Source]
  implicit val dealToJson = ToJson.auto[Deal]
  implicit val dealFromJson = FromJson.auto[Deal]
  implicit val formatToFromJson = ToAndFromJson.auto[Format]
  implicit val pmpToFromJson = ToAndFromJson.auto[Pmp]
  implicit val nativeToFromJson = ToAndFromJson.auto[Native]
  implicit val bannerToFromJson = ToAndFromJson.auto[Banner]
  implicit val audioToFromJson = ToAndFromJson.auto[Audio]
  implicit val videoToFromJson = ToAndFromJson.auto[Video]
  implicit val metricToFromJson = ToAndFromJson.auto[Metric]
  implicit val impToFromJson = ToAndFromJson.auto[Imp]
  implicit val reqsToFromJson = ToAndFromJson.auto[Reqs]
  implicit val producerToFromJson = ToAndFromJson.auto[Producer]
  implicit val segmentToFromJson = ToAndFromJson.auto[Segment]
  implicit val dataToFromJson = ToAndFromJson.auto[Data]
  implicit val contentToFromJson = ToAndFromJson.auto[Content]
  implicit val appToFromJson = ToAndFromJson.auto[App]
  implicit val siteToFromJson = ToAndFromJson.auto[Site]
  implicit val geoToFromJson = ToAndFromJson.auto[Geo]
  implicit val deviceToFromJson = ToAndFromJson.auto[Device]
  implicit val userToFromJson = ToAndFromJson.auto[User]
  implicit val bidRequestToFromJson = ToAndFromJson.auto[BidRequest]


  // google maps api
  implicit val valueToFromJson = ToAndFromJson.auto[Value]
  implicit val ElementsToFromJson = ToAndFromJson.auto[Elements]
  implicit val rowsToFromJson = ToAndFromJson.auto[Rows]
  implicit val distanceMatrixToFromJson = ToAndFromJson.auto[DistanceMatrix]

  // github actions api
  implicit val artifactToFromJson = {
    implicit val booleanStringer: ToSomeJson[Boolean] = b => JsonString(b.toString)
    implicit val isoInstantsToJson = ToJsonInstances.offsetDateTimeToJson.contramap[Instant](OffsetDateTime.ofInstant(_, ZoneOffset.UTC))
    implicit val isoInstantsFromJson = FromJsonInstances.offsetDateTimeFromJson.map(_.toInstant)
    ToAndFromJson.auto[GitHubActionsAPI.Artifact]
  }
  implicit val ghaResponseToFromJson = ToAndFromJson.auto[GitHubActionsAPI.Response]
}
