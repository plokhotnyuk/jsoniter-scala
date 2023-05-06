#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java17.json .*' 2>&1 | tee graalvm-ee-java17.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java11 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java11.json .*' 2>&1 | tee graalvm-ee-java11.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java20 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java20.json .*' 2>&1 | tee graalvm-ce-java20.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java17.json .*' 2>&1 | tee graalvm-ce-java17.txt
sbt -batch -java-home /usr/lib/jvm/zulu-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvm /usr/lib/jvm/openjdk-21/bin/java -p size=128 -prof gc -rf json -rff openjdk-21.json .*' 2>&1 | tee openjdk-21.txt
sbt -batch -java-home /usr/lib/jvm/zulu-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-17.json .*' 2>&1 | tee zulu-17.txt
sbt -batch -java-home /usr/lib/jvm/zulu-11 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-11.json .*' 2>&1 | tee zulu-11.txt
