package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.io.InputStream
import java.util
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
  "-XX:-UseAdaptiveSizePolicy",
  "-XX:MaxInlineLevel=18",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch",
  "-XX:+UseNUMA",
  "-XX:-UseAdaptiveNUMAChunkSizing"
))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
abstract class CommonParams {
  def bytes(in: InputStream): Array[Byte] = try {
    val step = 8192
    var buf = new Array[Byte](step)
    var pos, n = 0
    while ({
      if (pos + step > buf.length) buf = util.Arrays.copyOf(buf, buf.length << 1)
      n = in.read(buf, pos, step)
      n != -1
    }) pos += n
    if (pos != buf.length) buf = util.Arrays.copyOf(buf, pos)
    buf
  } finally in.close()
}