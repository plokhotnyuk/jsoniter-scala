package com.github.plokhotnyuk.jsoniter_scala

import java.util
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.Codec.materialize
import org.openjdk.jmh.annotations._

import scala.collection.immutable.{HashMap, Map}

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class CodecBenchmark {
  private val jacksonMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  private val anyRefsCodec = materialize[AnyRefs]
  private val iterablesCodec = materialize[Iterables]
  private val mapsCodec = materialize[Maps]
  private val primitivesCodec = materialize[Primitives]
  private val anyRefsJson = """{"s":"s","bd":1,"os":"os"}""".getBytes
  private val iterablesJson = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes
  private val mapsJson = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes
  private val primitivesJson = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes
  private val anyRefsObj = AnyRefs("s", 1, Some("os"))
  private val iterablesObj = Iterables(List("1", "2", "3"), Set(4, 5, 6), List(Set(1, 2), Set()))
  private val mapsObj = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  private val primitivesObj = Primitives(1, 2, 3, 4, bl = true, 'V', 1.1, 2.2f)
  require(readAnyRefsJackson() == anyRefsObj)
  require(readAnyRefsJsoniter() == anyRefsObj)
  require(readIterablesJackson() == iterablesObj)
  require(readIterablesJsoniter() == iterablesObj)
  //FIXME: Jackson-module-scala instantiates Map instead of HashMap
  // require(readMapsJackson() == mapsObj)
  require(readMapsJsoniter() == mapsObj)
  require(readPrimitivesJackson() == primitivesObj)
  require(readPrimitivesJsoniter() == primitivesObj)
  require(util.Arrays.equals(writeAnyRefsJackson(), anyRefsJson))
  require(util.Arrays.equals(writeAnyRefsJsoniter(), anyRefsJson))
  require(util.Arrays.equals(writeIterablesJackson(), iterablesJson))
  require(util.Arrays.equals(writeIterablesJsoniter(), iterablesJson))
  require(util.Arrays.equals(writeMapsJackson(), mapsJson))
  require(util.Arrays.equals(writeMapsJsoniter(), mapsJson))
  //FIXME: by default Jackson stores Char as String, while Jsoniter stores it as Int
  // require(util.Arrays.equals(writePrimitivesJackson(), primitivesJson))
  require(util.Arrays.equals(writePrimitivesJsoniter(), primitivesJson))

  @Benchmark
  def readAnyRefsJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](anyRefsJson)

  @Benchmark
  def readAnyRefsJsoniter(): AnyRefs = anyRefsCodec.read(anyRefsJson)

  @Benchmark
  def readIterablesJackson(): Iterables = jacksonMapper.readValue[Iterables](iterablesJson)

  @Benchmark
  def readIterablesJsoniter(): Iterables = iterablesCodec.read(iterablesJson)

  @Benchmark
  def readMapsJackson(): Maps = jacksonMapper.readValue[Maps](mapsJson)

  @Benchmark
  def readMapsJsoniter(): Maps = mapsCodec.read(mapsJson)

  @Benchmark
  def readPrimitivesJackson(): Primitives = jacksonMapper.readValue[Primitives](primitivesJson)

  @Benchmark
  def readPrimitivesJsoniter(): Primitives = primitivesCodec.read(primitivesJson)

  @Benchmark
  def writeAnyRefsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniter(): Array[Byte] = anyRefsCodec.write(anyRefsObj)

  @Benchmark
  def writeIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(iterablesObj)

  @Benchmark
  def writeIterablesJsoniter(): Array[Byte] = iterablesCodec.write(iterablesObj)

  @Benchmark
  def writeMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mapsObj)

  @Benchmark
  def writeMapsJsoniter(): Array[Byte] = mapsCodec.write(mapsObj)

  @Benchmark
  def writePrimitivesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(primitivesObj)

  @Benchmark
  def writePrimitivesJsoniter(): Array[Byte] = primitivesCodec.write(primitivesObj)
}

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

case class Iterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

case class Maps(m: HashMap[String, Double], mm: Map[Int, HashMap[Long, Double]])

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)
