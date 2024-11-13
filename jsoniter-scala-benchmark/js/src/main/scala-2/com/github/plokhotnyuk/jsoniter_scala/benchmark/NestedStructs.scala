package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.avsystem.commons.serialization.transientDefault

case class NestedStructs(@transientDefault n: Option[NestedStructs] = None)
