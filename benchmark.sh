#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java19 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java19.json .*' 2>&1 | tee graalvm-ee-java19.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java17.json .*' 2>&1 | tee graalvm-ee-java17.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java11 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java11.json .*' 2>&1 | tee graalvm-ee-java11.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java19 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java19.json .*' 2>&1 | tee graalvm-ce-java19.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java17.json .*' 2>&1 | tee graalvm-ce-java17.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-20 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff openjdk-20.json .*' 2>&1 | tee openjdk-20.txt
sbt -batch -java-home /usr/lib/jvm/zulu-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-17.json .*' 2>&1 | tee zulu-17.txt
sbt -batch -java-home /usr/lib/jvm/zulu-11 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-11.json .*' 2>&1 | tee zulu-11.txt
