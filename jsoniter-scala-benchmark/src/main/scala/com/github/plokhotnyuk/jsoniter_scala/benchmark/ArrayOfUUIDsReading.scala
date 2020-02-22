package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ArrayOfUUIDsReading extends ArrayOfUUIDsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[UUID] = JsonStringInput.read[Array[UUID]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[UUID] = io.bullet.borer.Json.decode(jsonBytes).to[Array[UUID]].value

  @Benchmark
  def circe(): Array[UUID] = decode[Array[UUID]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[UUID] = dslJsonDecode[Array[UUID]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[UUID] = jacksonMapper.readValue[Array[UUID]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[UUID] = readFromArray[Array[UUID]](jsonBytes)

  @Benchmark
  def playJson(): Array[UUID] = Json.parse(jsonBytes).as[Array[UUID]]

  @Benchmark
  def sprayJson(): Array[UUID] = JsonParser(jsonBytes).convertTo[Array[UUID]]

  @Benchmark
  def uPickle(): Array[UUID] = read[Array[UUID]](jsonBytes)

  @Benchmark
  def weePickle(): Array[UUID] = FromJson(jsonBytes).transform(ToScala[Array[UUID]])

  @Benchmark
  def javaCopy(): Array[UUID] = {
    val len = res.length
    var i = 0
    while (i < len) {
      res(i) = obj(i)
      i += 1
    }
    res
  }

  @Benchmark
  def javaOrig(): Array[UUID] = {
    val len = res.length
    var i = 0
    while (i < len) {
      res(i) = UUID.fromString(strings(i))
      i += 1
    }
    res
  }

  @Benchmark
  def javaFast(): Array[UUID] = {
    val len = res.length
    var i = 0
    while (i < len) {
      res(i) = fromString(strings(i))
      i += 1
    }
    res
  }

  def fromString(name: String): UUID = {
    val ns = nibbles
    var msb, lsb = 0L
    if (name.length == 36 && {
      val ch1: Long = name.charAt(8)
      val ch2: Long = name.charAt(13)
      val ch3: Long = name.charAt(18)
      val ch4: Long = name.charAt(23)
      (ch1 << 48 | ch2 << 32 | ch3 << 16 | ch4) == 0x2D002D002D002DL
    } && {
      val msb1 = parse4Nibbles(name, ns, 0)
      val msb2 = parse4Nibbles(name, ns, 4)
      val msb3 = parse4Nibbles(name, ns, 9)
      val msb4 = parse4Nibbles(name, ns, 14)
      msb = msb1 << 48 | msb2 << 32 | msb3 << 16 | msb4
      (msb1 | msb2 | msb3 | msb4) >= 0
    } && {
      val lsb1 = parse4Nibbles(name, ns, 19)
      val lsb2 = parse4Nibbles(name, ns, 24)
      val lsb3 = parse4Nibbles(name, ns, 28)
      val lsb4 = parse4Nibbles(name, ns, 32)
      lsb = lsb1 << 48 | lsb2 << 32 | lsb3 << 16 | lsb4
      (lsb1 | lsb2 | lsb3 | lsb4) >= 0
    }) new UUID(msb, lsb)
    else UUID.fromString(name)
  }

  private[this] def parse4Nibbles(name: String, ns: Array[Byte], pos: Int): Long = {
    val ch1 = name.charAt(pos)
    val ch2 = name.charAt(pos + 1)
    val ch3 = name.charAt(pos + 2)
    val ch4 = name.charAt(pos + 3)
    if ((ch1 | ch2 | ch3 | ch4) > 0xFF) -1
    else ns(ch1) << 12 | ns(ch2) << 8 | ns(ch3) << 4 | ns(ch4)
  }

  private final val nibbles: Array[Byte] = {
    val ns = new Array[Byte](256)
    java.util.Arrays.fill(ns, -1: Byte)
    ns('0') = 0
    ns('1') = 1
    ns('2') = 2
    ns('3') = 3
    ns('4') = 4
    ns('5') = 5
    ns('6') = 6
    ns('7') = 7
    ns('8') = 8
    ns('9') = 9
    ns('A') = 10
    ns('B') = 11
    ns('C') = 12
    ns('D') = 13
    ns('E') = 14
    ns('F') = 15
    ns('a') = 10
    ns('b') = 11
    ns('c') = 12
    ns('d') = 13
    ns('e') = 14
    ns('f') = 15
    ns
  }
}