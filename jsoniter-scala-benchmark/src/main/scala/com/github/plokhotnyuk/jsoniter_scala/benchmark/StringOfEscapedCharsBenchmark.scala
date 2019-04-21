package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.lang.Character._
import java.nio.charset.StandardCharsets.UTF_8

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import org.openjdk.jmh.annotations.{Param, Setup}

class StringOfEscapedCharsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: String = _
  var jsonString: String = _
  var jsonString2: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = {
    val jm = createJacksonMapper
    jm.getFactory.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
    jm
  }
/*
  val jsoniterJavaConfig: Config = new Config.Builder()
    .escapeUnicode(true)
    .encodingMode(EncodingMode.DYNAMIC_MODE)
    .decodingMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY)
    .build()
*/

  @Setup
  def setup(): Unit = {
    obj = {
      val cs = new Array[Char](size)
      var i = 0
      var j = 1
      while (i < cs.length) {
        cs(i) = {
          var ch: Char = 0
          do {
            ch = (j * 1498724053).toChar
            j += 1
          } while (ch < 128 || isSurrogate(ch))
          ch
        }
        i += 1
      }
      new String(cs)
    }
    jsonString = "\"" + obj.map(ch => f"\\u$ch%04x").mkString  + "\""
    jsonString2 = "\"" + obj.map(ch => f"\\u$ch%04X").mkString  + "\""
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}