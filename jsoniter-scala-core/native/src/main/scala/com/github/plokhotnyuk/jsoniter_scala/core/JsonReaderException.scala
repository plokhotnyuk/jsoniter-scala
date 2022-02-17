package com.github.plokhotnyuk.jsoniter_scala.core

class JsonReaderException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause) // FIXME: remove after fix of the following issue: https://github.com/scala-native/scala-native/issues/2561