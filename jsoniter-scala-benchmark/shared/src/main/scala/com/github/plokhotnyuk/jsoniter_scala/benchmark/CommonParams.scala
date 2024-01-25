package com.github.plokhotnyuk.jsoniter_scala.benchmark

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
  "-XX:NonNMethodCodeHeapSize=32m",
  "-XX:NonProfiledCodeHeapSize=240m",
  "-XX:ProfiledCodeHeapSize=240m",
  "-XX:TLABSize=16m",
  "-XX:-ResizeTLAB",
  "-XX:+UseParallelGC",
  "-XX:-UseAdaptiveSizePolicy",
  "-XX:MaxInlineLevel=20",
  "-XX:InlineSmallCode=1500",
  "-XX:+AlwaysPreTouch",
  "-XX:+UseNUMA",
  "-XX:-UseAdaptiveNUMAChunkSizing",
  "-XX:+PerfDisableSharedMem", // See https://github.com/Simonis/mmap-pause#readme
  "-XX:+UnlockExperimentalVMOptions",
  "-XX:+TrustFinalNonStaticFields"
))
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
abstract class CommonParams
