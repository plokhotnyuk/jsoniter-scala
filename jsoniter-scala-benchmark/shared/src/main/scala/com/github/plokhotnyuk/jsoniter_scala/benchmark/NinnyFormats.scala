package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.github.kag0.ninny._
import io.github.kag0.ninny.ast.JsonObject
import scala.util.Failure

object NinnyFormats {
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

}
