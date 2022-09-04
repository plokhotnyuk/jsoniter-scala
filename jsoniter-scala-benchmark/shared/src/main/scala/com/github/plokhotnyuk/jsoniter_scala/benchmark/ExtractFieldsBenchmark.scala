package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.{Param, Setup}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.HashCodeCollider._
import com.github.plokhotnyuk.jsoniter_scala.core._
import java.nio.charset.StandardCharsets.UTF_8

abstract class ExtractFieldsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    val value = """{"number":0.0,"boolean":false,"string":null}"""
    jsonString = zeroHashCodeStrings.map(s => writeToString(s)(JsoniterScalaCodecs.stringCodec)).take(size)
      .mkString("""{"s":"s",""", s""":$value,""", s""":$value,"i":1}""")
    jsonBytes = jsonString.getBytes(UTF_8)
  }
}