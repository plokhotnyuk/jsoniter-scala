package com.github.plokhotnyuk.jsoniter_scala.benchmark

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("fs", JSImport.Namespace)
object FS extends js.Object {
  def readFileSync(path: String, encoding: String): String = js.native
}
