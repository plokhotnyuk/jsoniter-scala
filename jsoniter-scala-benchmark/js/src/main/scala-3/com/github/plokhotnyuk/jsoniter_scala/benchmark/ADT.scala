package com.github.plokhotnyuk.jsoniter_scala.benchmark

import zio.json.jsonDiscriminator

@jsonDiscriminator("type")
sealed trait ADTBase extends Product with Serializable

case class X(a: Int) extends ADTBase

case class Y(b: String) extends ADTBase

case class Z(l: ADTBase, r: ADTBase) extends ADTBase