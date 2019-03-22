package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.input.JsoniterJavaParser
//import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

class ArrayOfDoublesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Double] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size)
      .map(i => ((i * 6971258582664805397L) / Math.pow(10, i % 17)).toLong * Math.pow(10, (i % 38) - 19)).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[Double] = JsonStringInput.read[Array[Double]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[Double] = decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): Array[Double] = dslJsonDecode[Array[Double]](jsonBytes)

  @Benchmark
  def readJacksonScala(): Array[Double] = jacksonMapper.readValue[Array[Double]](jsonBytes)
/* FIXME: Jsoniter Java cannot parse some numbers like 5.9823526 precisely
  @Benchmark
  def readJsoniterJava(): Array[Double] = JsoniterJavaParser.parse[Array[Double]](jsonBytes, classOf[Array[Double]])
*/
  @Benchmark
  def readJsoniterScala(): Array[Double] = readFromArray[Array[Double]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Double] = Json.parse(jsonBytes).as[Array[Double]]

  @Benchmark
  def readUPickle(): Array[Double] = read[Array[Double]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/*FIXME: dsl-json serializes doubles in a plain representation
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode[Array[Double]](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
/* FIXME: PreciseFloatSupport.enable() doesn't work sometime and Jsoniter Java serializes values rounded to 6 digits
  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play serializes doubles in BigDecimal format: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}