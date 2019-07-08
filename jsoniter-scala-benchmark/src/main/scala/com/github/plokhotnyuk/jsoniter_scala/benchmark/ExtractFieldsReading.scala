package com.github.plokhotnyuk.jsoniter_scala.benchmark

//import java.io.{File, FileInputStream, FileOutputStream}
//import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets.UTF_8
//import java.nio.file.StandardOpenOption._
//import java.nio.file.{Path, Paths}

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.HashCodeCollider._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._

case class ExtractFields(s: String, i: Int)

class ExtractFieldsReading extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 100
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
//  val tmpFilePath: Path = Paths.get(File.createTempFile("extract-fields-benchmark", ".json").getAbsolutePath)

  @Setup
  def setup(): Unit = {
    val value = """{"number":0.0,"boolean":false,"string":null}"""
    jsonString = zeroHashCodeStrings.take(size).mkString("""{"s":"s","""", s"""":$value,"""", s"""":$value,"i":1}""")
    //jsonString = """{"s":"s","x":""" + "9" * size + ""","i":1}"""
    //jsonString = """{"s":"s","x":"""" + "x" * size + """","i":1}"""
    //jsonString = """{"s":"s","x":""" + "[" * size + "]" * size + ""","i":1}"""
    //jsonString = """{"s":"s",""" + "\"x\":{" * size + "}" * size + ""","i":1}"""
    jsonBytes = jsonString.getBytes(UTF_8)
/*
    val fos = new FileOutputStream(tmpFilePath.toFile)
    try fos.write(jsonBytes)
    finally fos.close()
*/
  }

  @Benchmark
  def avSystemGenCodec(): ExtractFields = JsonStringInput.read[ExtractFields](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): ExtractFields = decode[ExtractFields](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): ExtractFields = dslJsonDecode[ExtractFields](jsonBytes)

  @Benchmark
  def jacksonScala(): ExtractFields = jacksonMapper.readValue[ExtractFields](jsonBytes)

  @Benchmark
  def jsoniterScala(): ExtractFields = readFromArray[ExtractFields](jsonBytes)
/*
  @Benchmark
  def jsoniterScalaIO(): ExtractFields = {
    val fis = new FileInputStream(tmpFilePath.toFile)
    try readFromStream[ExtractFields](fis)
    finally fis.close()
  }

  //FIXME: lot of warnings like: [warning][os,thread] Failed to start thread - pthread_create failed (EAGAIN) for attributes: stacksize: 1024k, guardsize: 4k, detached.
  //Read about mmap of files: http://www.mapdb.org/blog/mmap_files_alloc_and_jvm_crash/
  @Benchmark
  def jsoniterScalaNIO(): ExtractFields = {
    val fc = FileChannel.open(tmpFilePath, READ)
    try readFromByteBuffer[ExtractFields](fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size))
    finally fc.close()
  }
*/
  @Benchmark
  def playJson(): ExtractFields = Json.parse(jsonBytes).as[ExtractFields]

  @Benchmark
  def scalikeJackson(): ExtractFields = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[ExtractFields]
  }

  @Benchmark
  def sprayJson(): ExtractFields = JsonParser(jsonBytes).convertTo[ExtractFields]

  @Benchmark
  def uPickle(): ExtractFields = read[ExtractFields](jsonBytes)
}