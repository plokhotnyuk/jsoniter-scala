package com.github.plokhotnyuk.jsoniter_scala

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.Codec.materialize
import org.openjdk.jmh.annotations._

import scala.collection.immutable.{BitSet, HashMap, IntMap, LongMap, Map}
import scala.collection.mutable

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class CodecBenchmark {
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  val anyRefsCodec: Codec[AnyRefs] = materialize[AnyRefs]
  val bitSetsCodec: Codec[BitSets] = materialize[BitSets]
  val iterablesCodec: Codec[Iterables] = materialize[Iterables]
  val mapsCodec: Codec[Maps] = materialize[Maps]
  val mutableMapsCodec: Codec[MutableMaps] = materialize[MutableMaps]
  val intAndLongMapsCodec: Codec[IntAndLongMaps] = materialize[IntAndLongMaps]
  val primitivesCodec: Codec[Primitives] = materialize[Primitives]
  val anyRefsJson: Array[Byte] = """{"s":"s","bd":1,"os":"os"}""".getBytes
  val bitSetsJson: Array[Byte] = """{"bs":[1,2,3],"mbs":[4,5,6]}""".getBytes
  val iterablesJson: Array[Byte] = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}""".getBytes
  val mapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"1":{"3":3.3},"2":{}}}""".getBytes
  val mutableMapsJson: Array[Byte] = """{"m":{"2":2.2,"1":1.1},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val intAndLongMapsJson: Array[Byte] = """{"m":{"1":1.1,"2":2.2},"mm":{"2":{},"1":{"3":3.3}}}""".getBytes
  val primitivesJson: Array[Byte] = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":86,"dbl":1.1,"f":2.2}""".getBytes
  val anyRefsObj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val bitSetsObj: BitSets = BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6))
  val iterablesObj: Iterables = Iterables(List("1", "2", "3"), Set(4, 5, 6), List(Set(1, 2), Set()))
  val mapsObj: Maps = Maps(HashMap("1" -> 1.1, "2" -> 2.2), Map(1 -> HashMap(3L -> 3.3), 2 -> HashMap.empty[Long, Double]))
  val mutableMapsObj: MutableMaps = MutableMaps(mutable.HashMap("1" -> 1.1, "2" -> 2.2),
    mutable.Map(1 -> mutable.OpenHashMap(3L -> 3.3), 2 -> mutable.OpenHashMap.empty[Long, Double]))
  val intAndLongMapsObj: IntAndLongMaps = IntAndLongMaps(IntMap(1 -> 1.1, 2 -> 2.2),
    mutable.LongMap(1L -> LongMap(3L -> 3.3), 2L -> LongMap.empty[Double]))
  val primitivesObj: Primitives = Primitives(1, 2, 3, 4, bl = true, 'V', 1.1, 2.2f)

  @Benchmark
  def readAnyRefsJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](anyRefsJson)

  @Benchmark
  def readAnyRefsJsoniter(): AnyRefs = anyRefsCodec.read(anyRefsJson)

//FIXME: Jackson-module-scala doesn`t support parsing of bitsets
/*
  @Benchmark
  def readBitSetsJackson(): BitSets = jacksonMapper.readValue[BitSets](bitSetsJson)
*/

  @Benchmark
  def readBitSetsJsoniter(): BitSets = bitSetsCodec.read(bitSetsJson)

  @Benchmark
  def readIterablesJackson(): Iterables = jacksonMapper.readValue[Iterables](iterablesJson)

  @Benchmark
  def readIterablesJsoniter(): Iterables = iterablesCodec.read(iterablesJson)

  @Benchmark
  def readMapsJackson(): Maps = jacksonMapper.readValue[Maps](mapsJson)

  @Benchmark
  def readMapsJsoniter(): Maps = mapsCodec.read(mapsJson)

  @Benchmark
  def readMutableMapsJackson(): MutableMaps = jacksonMapper.readValue[MutableMaps](mutableMapsJson)

  @Benchmark
  def readMutableMapsJsoniter(): MutableMaps = mutableMapsCodec.read(mutableMapsJson)

//FIXME: Jackson-module-scala doesn`t support parsing of Int & Long maps
/*
  @Benchmark
  def readIntAndLongMapsJackson(): IntAndLongMaps = jacksonMapper.readValue[IntAndLongMaps](intAndLongMapsJson)
*/

  @Benchmark
  def readIntAndLongMapsJsoniter(): IntAndLongMaps = intAndLongMapsCodec.read(intAndLongMapsJson)

  @Benchmark
  def readPrimitivesJackson(): Primitives = jacksonMapper.readValue[Primitives](primitivesJson)

  @Benchmark
  def readPrimitivesJsoniter(): Primitives = primitivesCodec.read(primitivesJson)

  @Benchmark
  def writeAnyRefsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(anyRefsObj)

  @Benchmark
  def writeAnyRefsJsoniter(): Array[Byte] = anyRefsCodec.write(anyRefsObj)

  @Benchmark
  def writeBitSetsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(bitSetsObj)

  @Benchmark
  def writeBitSetsJsoniter(): Array[Byte] = bitSetsCodec.write(bitSetsObj)

  @Benchmark
  def writeIterablesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(iterablesObj)

  @Benchmark
  def writeIterablesJsoniter(): Array[Byte] = iterablesCodec.write(iterablesObj)

  @Benchmark
  def writeMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mapsObj)

  @Benchmark
  def writeMapsJsoniter(): Array[Byte] = mapsCodec.write(mapsObj)

  @Benchmark
  def writeMutableMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(mutableMapsObj)

  @Benchmark
  def writeMutableMapsJsoniter(): Array[Byte] = mutableMapsCodec.write(mutableMapsObj)

  @Benchmark
  def writeIntAndLongMapsJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(intAndLongMapsObj)

  @Benchmark
  def writeIntAndLongMapsJsoniter(): Array[Byte] = intAndLongMapsCodec.write(intAndLongMapsObj)

  @Benchmark
  def writePrimitivesJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(primitivesObj)

  @Benchmark
  def writePrimitivesJsoniter(): Array[Byte] = primitivesCodec.write(primitivesObj)
}

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

case class Iterables(l: List[String], s: Set[Int], ls: List[Set[Int]])

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

case class Maps(m: HashMap[String, Double], mm: Map[Int, HashMap[Long, Double]])

case class MutableMaps(m: mutable.HashMap[String, Double], mm: mutable.Map[Int, mutable.OpenHashMap[Long, Double]])

case class IntAndLongMaps(m: IntMap[Double], mm: mutable.LongMap[LongMap[Double]])

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)
