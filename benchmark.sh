#!/bin/bash
sbt -java-home /usr/lib/jvm/jdk-11 ++3.6.2 clean jsoniter-scala-benchmarkJVM/assembly
/usr/lib/jvm/jdk-25/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-25.json .* 2>&1 | tee jdk-25.txt
/usr/lib/jvm/jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-21.json .* 2>&1 | tee jdk-21.txt
/usr/lib/jvm/jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-17.json .* 2>&1 | tee jdk-17.txt
/usr/lib/jvm/graalvm-jdk-25/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-jdk-25.json .* 2>&1 | tee graalvm-jdk-25.txt
/usr/lib/jvm/graalvm-jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-jdk-21.json .* 2>&1 | tee graalvm-jdk-21.txt
/usr/lib/jvm/graalvm-jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-jdk-17.json .* 2>&1 | tee graalvm-jdk-17.txt
/usr/lib/jvm/graalvm-community-jdk-24/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-community-jdk-24.json .* 2>&1 | tee graalvm-community-jdk-24.txt
/usr/lib/jvm/graalvm-community-jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-community-jdk-21.json .* 2>&1 | tee graalvm-community-jdk-21.txt
/usr/lib/jvm/graalvm-community-jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.2/benchmarks.jar -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-community-jdk-17.json .* 2>&1 | tee graalvm-community-jdk-17.txt
