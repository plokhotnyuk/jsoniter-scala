package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers.createJacksonMapper
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfBytesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Byte] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = {
    val jm = createJacksonMapper
    jm.registerModule(new SimpleModule()
      .addSerializer(classOf[Array[Byte]], new ByteArraySerializer))
    jm
  }

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(_.toByte).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}