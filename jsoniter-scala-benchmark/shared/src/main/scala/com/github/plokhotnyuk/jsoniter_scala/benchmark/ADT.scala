package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.avsystem.commons.serialization.flatten
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.rallyhealth.weepickle.v1.implicits.{discriminator, key}
import zio.json.jsonDiscriminator

@jsonDiscriminator("type")
@discriminator("type")
@flatten("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[X], name = "X"),
  new Type(value = classOf[Y], name = "Y"),
  new Type(value = classOf[Z], name = "Z")))
sealed trait ADTBase extends Product with Serializable

@key("X")
case class X(a: Int) extends ADTBase

@key("Y")
case class Y(b: String) extends ADTBase

@key("Z")
case class Z(l: ADTBase, r: ADTBase) extends ADTBase