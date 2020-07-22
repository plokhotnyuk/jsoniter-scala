package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.avsystem.commons.serialization.transientDefault
import com.rallyhealth.weepickle.v1.implicits.dropDefault

@dropDefault
case class NestedStructs(@transientDefault n: Option[NestedStructs] = None)