package com.github.plokhotnyuk.jsoniter_scala.core

class JsonReaderException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause, true, withStackTrace) // FIXME: move to shared sources after fix of the following issue: https://github.com/scala-native/scala-native/issues/2561