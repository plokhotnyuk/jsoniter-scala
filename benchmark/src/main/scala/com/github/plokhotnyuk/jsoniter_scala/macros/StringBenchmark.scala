package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class StringBenchmark extends CommonParams {
  val asciiObj: String =
    "In computer science, an inverted index (also referred to as postings file or inverted file) is an index data structure storing a mapping from content, such as words or numbers, to its locations in a database file, or in a document or a set of documents (named in contrast to a Forward Index, which maps from documents to content). The purpose of an inverted index is to allow fast full text searches, at a cost of increased processing when a document is added to the database. The inverted file may be the database file itself, rather than its index. It is the most popular data structure used in document retrieval systems,[1] used on a large scale for example in search engines. Additionally, several significant general-purpose mainframe-based database management systems have used inverted list architectures, including ADABAS, DATACOM/DB, and Model 204. There are two main variants of inverted indexes: A record-level inverted index (or inverted file index or just inverted file) contains a list of references to documents for each word. A word-level inverted index (or full inverted index or inverted list) additionally contains the positions of each word within a document. The latter form offers more functionality (like phrase searches), but needs more processing power and space to be created."
  val asciiJsonString: String =
    """"In computer science, an inverted index (also referred to as postings file or inverted file) is an index data structure storing a mapping from content, such as words or numbers, to its locations in a database file, or in a document or a set of documents (named in contrast to a Forward Index, which maps from documents to content). The purpose of an inverted index is to allow fast full text searches, at a cost of increased processing when a document is added to the database. The inverted file may be the database file itself, rather than its index. It is the most popular data structure used in document retrieval systems,[1] used on a large scale for example in search engines. Additionally, several significant general-purpose mainframe-based database management systems have used inverted list architectures, including ADABAS, DATACOM/DB, and Model 204. There are two main variants of inverted indexes: A record-level inverted index (or inverted file index or just inverted file) contains a list of references to documents for each word. A word-level inverted index (or full inverted index or inverted list) additionally contains the positions of each word within a document. The latter form offers more functionality (like phrase searches), but needs more processing power and space to be created.""""
  val asciiJsonBytes: Array[Byte] = asciiJsonString.getBytes("UTF-8")
  val nonAsciiObj: String =
    "倒排索引（英语：Inverted index），也常被称为反向索引、置入档案或反向档案，是一种索引方法，被用来存储在全文搜索下某个单词在一个文档或者一组文档中的存储位置的映射。它是文档检索系统中最常用的数据结构。"
  val nonAsciiJsonString: String =
    """"倒排索引（英语：Inverted index），也常被称为反向索引、置入档案或反向档案，是一种索引方法，被用来存储在全文搜索下某个单词在一个文档或者一组文档中的存储位置的映射。它是文档检索系统中最常用的数据结构。""""
  val nonAsciiJsonBytes: Array[Byte] = nonAsciiJsonString.getBytes("UTF-8")

  @Benchmark
  def readAsciiCirce(): String = decode[String](new String(asciiJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readAsciiJacksonScala(): String = jacksonMapper.readValue[String](asciiJsonBytes)

  @Benchmark
  def readAsciiJsoniterScala(): String = JsonReader.read(stringCodec, asciiJsonBytes)

/* FIXME: find proper way to parse string value in Play JSON
  @Benchmark
  def readAsciiPlayJson(): String = Json.parse(asciiJsonBytes).toString()
*/
  @Benchmark
  def readNonAsciiCirce(): String = decode[String](new String(nonAsciiJsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readNonAsciiJacksonScala(): String = jacksonMapper.readValue[String](nonAsciiJsonBytes)

  @Benchmark
  def readNonAsciiJsoniterScala(): String = JsonReader.read(stringCodec, nonAsciiJsonBytes)

/* FIXME: find proper way to parse string value in Play JSON
  @Benchmark
  def readNonAsciiPlayJson(): String = Json.parse(nonAsciiJsonBytes).toString()
*/

  @Benchmark
  def writeAsciiCirce(): Array[Byte] = printer.pretty(asciiObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeAsciiJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(asciiObj)

  @Benchmark
  def writeAsciiJsoniterScala(): Array[Byte] = JsonWriter.write(stringCodec, asciiObj)

  @Benchmark
  def writeAsciiJsoniterScalaPrealloc(): Int = JsonWriter.write(stringCodec, asciiObj, preallocatedBuf, 0)

  @Benchmark
  def writeAsciiPlayJson(): Array[Byte] = Json.toBytes(Json.toJson(asciiObj))

  @Benchmark
  def writeNonAsciiCirce(): Array[Byte] = printer.pretty(nonAsciiObj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeNonAsciiJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(nonAsciiObj)

  @Benchmark
  def writeNonAsciiJsoniterScala(): Array[Byte] = JsonWriter.write(stringCodec, nonAsciiObj)

  @Benchmark
  def writeNonAsciiJsoniterScalaPrealloc(): Int = JsonWriter.write(stringCodec, nonAsciiObj, preallocatedBuf, 0)

  @Benchmark
  def writeNonAsciiPlayJson(): Array[Byte] = Json.toBytes(Json.toJson(nonAsciiObj))
}