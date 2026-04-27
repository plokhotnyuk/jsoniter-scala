#!/bin/bash
(cd ..; sbt -java-home /usr/lib/jvm/jdk-17 ++3.8.3 clean jsoniter-scala-benchmarkJVM/assembly)
/usr/lib/jvm/jdk-27/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -p size=512 -prof gc -rf json -rff jdk-27.json .* 2>&1 | tee jdk-27.txt
/usr/lib/jvm/graalvm-jdk-25/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -p size=512 -prof gc -rf json -rff graalvm-jdk-25.json .* 2>&1 | tee graalvm-jdk-25.txt
/usr/lib/jvm/jdk-25/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -p size=512 -prof gc -rf json -rff jdk-25.json .* 2>&1 | tee jdk-25.txt
/usr/lib/jvm/graalvm-community-jdk-25/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -jvmArgsAppend "-XX:+UseCompactObjectHeaders" -p size=512 -prof gc -rf json -rff graalvm-community-jdk-25.json .* 2>&1 | tee graalvm-community-jdk-25.txt
/usr/lib/jvm/graalvm-jdk-21/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff graalvm-jdk-21.json .* 2>&1 | tee graalvm-jdk-21.txt
/usr/lib/jvm/jdk-21/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff jdk-21.json .* 2>&1 | tee jdk-21.txt
/usr/lib/jvm/graalvm-community-jdk-21/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff graalvm-community-jdk-21.json .* 2>&1 | tee graalvm-community-jdk-21.txt
/usr/lib/jvm/graalvm-jdk-17/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff graalvm-jdk-17.json .* 2>&1 | tee graalvm-jdk-17.txt
/usr/lib/jvm/jdk-17/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff jdk-17.json .* 2>&1 | tee jdk-17.txt
/usr/lib/jvm/graalvm-community-jdk-17/bin/java -jar jvm/target/scala-3.8.3/benchmarks.jar -p size=512 -prof gc -rf json -rff graalvm-community-jdk-17.json .* 2>&1 | tee graalvm-community-jdk-17.txt
