package com.github.plokhotnyuk.jsoniter_scala

class JsonException(message: String) extends RuntimeException(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}
