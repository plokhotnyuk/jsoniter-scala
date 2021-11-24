package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.github.kag0.ninny._
import io.github.kag0.ninny.ast.JsonObject
import scala.util.Failure

object NinnyFormats {
  lazy implicit val adtBaseToJson: ToSomeJson[ADTBase] = {
      case x: X => adtXToJson.toSome(x)
      case y: Y => adtYToJson.toSome(y)
      case z: Z => adtZToJson.toSome(z)
  }

  implicit val adtXToJson = ToJson.auto[X]
  implicit val adtYToJson = ToJson.auto[Y]
  implicit val adtZToJson = ToJson.auto[Z]
  
  lazy implicit val adtBaseFromJson = FromJson.fromSome {
      case json @ JsonObject(value) => 
        if(value.contains("a")) adtXFromJson.from(json)
        else if(value.contains("b")) adtYFromJson.from(json)
        else adtZFromJson.from(json)
      case _ => Failure(new JsonException("expected object"))
  }

  implicit val adtXFromJson: FromJson[X] = FromJson.auto[X]
  implicit val adtYFromJson: FromJson[Y] = FromJson.auto[Y]
  lazy implicit val adtZFromJson: FromJson[Z] = FromJson.auto[Z]

}
