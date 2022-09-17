package com.github.plokhotnyuk.jsoniter_scala.benchmark

import fabric.rw._

object FabricRW {
  implicit val xRW: RW[X] = ccRW
  implicit val yRW: RW[Y] = ccRW
  implicit val zRW: RW[Z] = ccRW
  implicit val adtRW: RW[ADTBase] = RW.poly[ADTBase](getType = getType) {
    case "X" => xRW
    case "Y" => yRW
    case "Z" => zRW
  }

  private def getType[P](p: P): String = p.getClass.getSimpleName.replace("$", "")
}