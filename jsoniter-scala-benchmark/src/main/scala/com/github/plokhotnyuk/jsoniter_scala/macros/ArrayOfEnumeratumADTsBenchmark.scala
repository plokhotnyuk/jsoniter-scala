package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._
import enumeratum._

import scala.collection.immutable

sealed trait EnumeratumSuit extends EnumEntry

object EnumeratumSuit extends Enum[EnumeratumSuit] with CirceEnum[EnumeratumSuit] with PlayJsonEnum[EnumeratumSuit] {
  val values: immutable.IndexedSeq[EnumeratumSuit] = findValues

  case object Hearts extends EnumeratumSuit

  case object Spades extends EnumeratumSuit

  case object Diamonds extends EnumeratumSuit

  case object Clubs extends EnumeratumSuit
}

class ArrayOfEnumeratumADTsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[EnumeratumSuit] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedOff: Int = 128
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => (i * 1498724053) & 3 match {
      case 0 => EnumeratumSuit.Hearts
      case 1 => EnumeratumSuit.Spades
      case 2 => EnumeratumSuit.Diamonds
      case 3 => EnumeratumSuit.Clubs
    }).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }
/* FIXME: AVSystem GenCodec hasn't integration with enumeratum
  @Benchmark
  def readAVSystemGenCodec(): Array[EnumeratumSuit] = JsonStringInput.read[Array[EnumeratumSuit]](new String(jsonBytes, UTF_8))
*/
  @Benchmark
  def readCirce(): Array[EnumeratumSuit] = decode[Array[EnumeratumSuit]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
/* FIXME: Jackson-module-scala hasn't integration with enumeratum
  @Benchmark
  def readJacksonScala(): Array[EnumeratumSuit] = jacksonMapper.readValue[Array[EnumeratumSuit]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): Array[EnumeratumSuit] = readFromArray[Array[EnumeratumSuit]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[EnumeratumSuit] = Json.parse(jsonBytes).as[Array[EnumeratumSuit]]

/* FIXME: the latest version of UPickle hasn't integration with enumeratum
  @Benchmark
  def readUPickle(): Array[EnumeratumSuit] = read[Array[EnumeratumSuit]](jsonBytes)
*/
/* FIXME: AVSystem GenCodec hasn't integration with enumeratum
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME: Jackson-module-scala hasn't integration with enumeratum
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
/* FIXME: the latest version of UPickle hasn't integration with enumeratum
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}