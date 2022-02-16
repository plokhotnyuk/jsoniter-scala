package com.github.plokhotnyuk.jsoniter_scala.core

class JsonWriterException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause)