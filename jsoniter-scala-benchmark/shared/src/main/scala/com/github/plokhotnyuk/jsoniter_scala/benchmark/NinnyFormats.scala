package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.github.kag0.ninny._
import io.github.kag0.ninny.ast._
import GoogleMapsAPI._
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

object NinnyFormats {
  lazy implicit val adtBaseToJson: ToSomeJson[ADTBase] = {
    implicit val v1: ToSomeJsonObject[X] = ToJson.auto[X]
    implicit val v2: ToSomeJsonObject[Y] = ToJson.auto[Y]
    implicit val v3: ToSomeJsonObject[Z] = ToJson.auto[Z]

    {
      case x: X => v1.toSome(x) + ("type" -> "X")
      case y: Y => v2.toSome(y) + ("type" -> "Y")
      case z: Z => v3.toSome(z) + ("type" -> "Z")
    }
  }

  lazy implicit val adtBaseFromJson: FromJson[ADTBase] = {
    implicit val v1: FromJson[X] = FromJson.auto[X]
    implicit val v2: FromJson[Y] = FromJson.auto[Y]
    implicit val v3: FromJson[Z] = FromJson.auto[Z]
    FromJson.fromSome(json =>
      (json / "type").to[String].flatMap {
        case "X" => v1.from(json)
        case "Y" => v2.from(json)
        case "Z" => v3.from(json)
      }
    )
  }

  implicit val extractFieldsFromJson: FromJson[ExtractFields] = FromJson.auto[ExtractFields]

  implicit val googleMapsAPIToFromJson: ToAndFromJson[DistanceMatrix] = {
    implicit val v1: ToAndFromJson[Value] = ToAndFromJson.auto[Value]
    implicit val v2: ToAndFromJson[Elements] = ToAndFromJson.auto[Elements]
    implicit val v3: ToAndFromJson[Rows] = ToAndFromJson.auto[Rows]
    ToAndFromJson.auto[DistanceMatrix]
  }

  implicit val githubActionsAPIToFromJson: ToAndFromJson[GitHubActionsAPI.Response] = {
    implicit val v1: ToSomeJson[Boolean] = b => JsonString(b.toString)
    implicit val v2: ToJsonValue[Instant, JsonValue] =
      ToJsonInstances.offsetDateTimeToJson.contramap[Instant](OffsetDateTime.ofInstant(_, ZoneOffset.UTC))
    implicit val v3: FromJson[Instant] = FromJsonInstances.offsetDateTimeFromJson.map(_.toInstant)
    implicit val v4: ToAndFromJson[GitHubActionsAPI.Artifact] = ToAndFromJson.auto[GitHubActionsAPI.Artifact]
    ToAndFromJson.auto[GitHubActionsAPI.Response]
  }
}
