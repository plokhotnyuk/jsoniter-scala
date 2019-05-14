package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringInput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB.BidRequest
import com.github.plokhotnyuk.jsoniter_scala.benchmark.OpenRTB._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import spray.json.JsonParser

//import io.circe.generic.auto._
//import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark

class OpenRTBReading extends OpenRTBBenchmark {
  @Benchmark
  def avSystemGenCodec(): BidRequest = JsonStringInput.read[BidRequest](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): BidRequest = io.bullet.borer.Json.decode(jsonBytes).to[BidRequest].value
/* FIXME: circe throws DecodingFailure(Attempt to decode value on failed cursor, List(DownField(mimes), DownField(banner), DownArray, DownField(imp)))
  @Benchmark
  def circe(): BidRequest = decode[BidRequest](new String(jsonBytes, UTF_8)).fold(throw _, identity)
*/
/*
  @Benchmark
  def jacksonScala(): BidRequest = jacksonMapper.readValue[BidRequest](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): BidRequest = readFromArray[BidRequest](jsonBytes)
/* FIXME: Spray-JSON throws spray.json.DeserializationException: Object is missing required member 'mimes'
  @Benchmark
  def sprayJson(): BidRequest = JsonParser(jsonBytes).convertTo[BidRequest]
*/
  @Benchmark
  def uPickle(): BidRequest = read[BidRequest](jsonBytes)
}