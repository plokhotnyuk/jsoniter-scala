package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json
//import spray.json._

class ArrayOfDoublesWriting extends ArrayOfDoublesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def borerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def circe(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/*FIXME: dsl-json serializes doubles in a plain representation
  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonEncode(obj)
*/
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
/* FIXME: PreciseFloatSupport.enable() doesn't work sometime and Jsoniter Java serializes values rounded to 6 digits
  @Benchmark
  def jsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)
*/
  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play-JSON serializes doubles in BigDecimal format: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def scalikeJackson(): Array[Byte] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    obj.write.getBytes(UTF_8)
  }
/* FIXME: Spray-JSON serializes doubles in different format than toString: 6.653409109328879E-5 as 0.00006653409109328879
  @Benchmark
  def sprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)
*/
  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}