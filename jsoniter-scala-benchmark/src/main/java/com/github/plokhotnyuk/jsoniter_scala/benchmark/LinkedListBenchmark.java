package com.github.plokhotnyuk.jsoniter_scala.benchmark;

import org.openjdk.jmh.annotations.*;
import scala.collection.immutable.List;
import scala.collection.mutable.ListBuffer;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {
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
})
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class LinkedListBenchmark {
    @Param({"1", "10", "100"})
    int size;

    @Benchmark
    public LinkedList<Boolean> javaListOfBooleans() {
        LinkedList<Boolean> list = new LinkedList<>();
        int l = size;
        int i = 0;
        while (i < l) {
            list.add((i & 1) == 0);
            i++;
        }
        return list;
    }

    @Benchmark
    public List<Boolean> scalaListOfBooleans() {
        ListBuffer<Boolean> listBuffer = new ListBuffer<>();
        int l = size;
        int i = 0;
        while (i < l) {
            listBuffer.$plus$eq((i & 1) == 0);
            i++;
        }
        return listBuffer.toList();
    }
}
