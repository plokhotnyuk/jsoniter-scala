package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.input.JsoniterJavaParser
//import com.jsoniter.output.JsoniterJavaSerializer
//import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

class ArrayOfFloatsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Float] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size)
      .map(i => (((i * 1498724053) / Math.pow(10, i % 9)).toInt * Math.pow(10, (i % 20) - 10)).toFloat).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
/* FIXME: AVSystem GenCodec parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readAVSystemGenCodec(): Array[Float] = JsonStringInput.read[Array[Float]](new String(jsonBytes, UTF_8))
*/
/* FIXME: circe parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readCirce(): Array[Float] = decode[Array[Float]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
*/
/* FIXME: DSL-JSON parses 7.006492321624086e-46 as Float.Infinity
  @Benchmark
  def readDslJsonScala(): Array[Float] = decodeDslJson[Array[Float]](jsonBytes)
*/
/* FIXME: Jackson Scala parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readJacksonScala(): Array[Float] = jacksonMapper.readValue[Array[Float]](jsonBytes)
*/
/* FIXME: Jsoniter Java parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readJsoniterJava(): Array[Float] = JsoniterJavaParser.parse[Array[Float]](jsonBytes, classOf[Array[Float]])
*/
  @Benchmark
  def readJsoniterScala(): Array[Float] = readFromArray[Array[Float]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Float] = Json.parse(jsonBytes).as[Array[Float]]

  @Benchmark
  def readUPickle(): Array[Float] = read[Array[Float]](jsonBytes)
/* FIXME: AVSystem GenCodec serializes double values instead of float
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = encodeDslJson[Array[Float]](obj)

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
/* FIXME: Play-JSON serializes double values instead of float
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}