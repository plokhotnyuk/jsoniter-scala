package com.github.plokhotnyuk.jsoniter_scala.macros

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms2g",
  "-Xmx2g",
  "-XX:NewSize=1g",
  "-XX:MaxNewSize=1g",
  "-XX:InitialCodeCacheSize=512m",
  "-XX:ReservedCodeCacheSize=512m",
  "-XX:+UseParallelGC",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
abstract class CommonParams {
  var preallocatedOff: Int = 128
  var preallocatedBuf: Array[Byte] = new Array(32768)
}