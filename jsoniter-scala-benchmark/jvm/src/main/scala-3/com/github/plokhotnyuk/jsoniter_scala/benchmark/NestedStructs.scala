package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.rallyhealth.weepickle.v1.implicits.dropDefault

@dropDefault
case class NestedStructs(n: Option[NestedStructs] = None)