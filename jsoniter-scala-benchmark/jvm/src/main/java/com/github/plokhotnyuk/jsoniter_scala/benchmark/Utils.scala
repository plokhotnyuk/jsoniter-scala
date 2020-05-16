package com.github.plokhotnyuk.jsoniter_scala.benchmark

object Utils {
  def getResourceAsStream(resource: String): java.io.InputStream = getClass.getResourceAsStream(resource)
}
