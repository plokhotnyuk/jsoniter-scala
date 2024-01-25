#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/graalvm-community-jdk-22 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-community-jdk-22.json .*' 2>&1 | tee graalvm-community-jdk-22.txt
sbt -batch -java-home /usr/lib/jvm/jdk-23 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-23.json .*' 2>&1 | tee jdk-23.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-jdk-21 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-jdk-21.json .*' 2>&1 | tee graalvm-jdk-21.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-jdk-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-jdk-17.json .*' 2>&1 | tee graalvm-jdk-17.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-community-jdk-21 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff graalvm-community-jdk-21.json .*' 2>&1 | tee graalvm-community-jdk-21.txt
sbt -batch -java-home /usr/lib/jvm/jdk-21 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-21.json .*' 2>&1 | tee jdk-21.txt
sbt -batch -java-home /usr/lib/jvm/jdk-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvmArgsAppend "-Djmh.executor=FJP" -p size=128 -prof gc -rf json -rff jdk-17.json .*' 2>&1 | tee jdk-17.txt
