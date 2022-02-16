package com.github.plokhotnyuk.jsoniter_scala.core

class JsonReaderException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause)