#!/bin/bash
sbt -java-home /usr/lib/jvm/jdk-17 ++3.6.4 clean jsoniter-scala-benchmarkJVM/assembly
/usr/lib/jvm/graalvm-jdk-25/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -t 16 -p size=512 -prof gc -rf json -rff graalvm-jdk-25-t16.json .* 2>&1 | tee graalvm-jdk-25-t16.txt
/usr/lib/jvm/jdk-25/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -t 16 -p size=512 -prof gc -rf json -rff jdk-25-t16.json .* 2>&1 | tee jdk-25-t16.txt
/usr/lib/jvm/graalvm-community-jdk-25/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -t 16 -p size=512 -prof gc -rf json -rff graalvm-community-jdk-25-t16.json .* 2>&1 | tee graalvm-community-jdk-25-t16.txt
/usr/lib/jvm/graalvm-jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff graalvm-jdk-21-t16.json .* 2>&1 | tee graalvm-jdk-21-t16.txt
/usr/lib/jvm/jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff jdk-21-t16.json .* 2>&1 | tee jdk-21-t16.txt
/usr/lib/jvm/graalvm-community-jdk-21/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff graalvm-community-jdk-21-t16.json .* 2>&1 | tee graalvm-community-jdk-21-t16.txt
/usr/lib/jvm/graalvm-jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff graalvm-jdk-17-t16.json .* 2>&1 | tee graalvm-jdk-17-t16.txt
/usr/lib/jvm/jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff jdk-17-t16.json .* 2>&1 | tee jdk-17-t16.txt
/usr/lib/jvm/graalvm-community-jdk-17/bin/java -jar jsoniter-scala-benchmark/jvm/target/scala-3.6.4/benchmarks.jar -t 16 -p size=512 -prof gc -rf json -rff graalvm-community-jdk-17-t16.json .* 2>&1 | tee graalvm-community-jdk-17-t16.txt
