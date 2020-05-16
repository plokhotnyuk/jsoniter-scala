package com.github.plokhotnyuk.jsoniter_scala.benchmark

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("path", JSImport.Namespace)
object Path extends js.Object {
  def join(paths: String*): String = js.native
  def resolve(paths: String*): String = js.native
}
