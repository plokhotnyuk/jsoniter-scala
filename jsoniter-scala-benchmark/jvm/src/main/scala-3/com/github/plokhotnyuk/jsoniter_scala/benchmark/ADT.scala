package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.rallyhealth.weepickle.v1.implicits.{discriminator, key}
import zio.json.jsonDiscriminator

@discriminator("type")
@jsonDiscriminator("type")
sealed trait ADTBase extends Product with Serializable

@key("X")
case class X(a: Int) extends ADTBase

@key("Y")
case class Y(b: String) extends ADTBase

@key("Z")
case class Z(l: ADTBase, r: ADTBase) extends ADTBase