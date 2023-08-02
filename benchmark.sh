#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/oracle-graalvm-jdk-20 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff oracle-graalvm-jdk-20.json .*' 2>&1 | tee oracle-graalvm-jdk-20.txt
sbt -batch -java-home /usr/lib/jvm/oracle-graalvm-jdk-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff oracle-graalvm-jdk-17.json .*' 2>&1 | tee oracle-graalvm-jdk-17.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-community-jdk-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvm /usr/lib/jvm/graalvm-community-jdk-21/bin/java -p size=128 -prof gc -rf json -rff graalvm-community-jdk-21.json .*' 2>&1 | tee graalvm-community-jdk-21.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-community-jdk-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff graalvm-community-jdk-17.json .*' 2>&1 | tee graalvm-community-jdk-17.txt
sbt -batch -java-home /usr/lib/jvm/zulu-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -jvm /usr/lib/jvm/jdk-22/bin/java -p size=128 -prof gc -rf json -rff jdk-22.json .*' 2>&1 | tee jdk-22.txt
sbt -batch -java-home /usr/lib/jvm/zulu-17 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-17.json .*' 2>&1 | tee zulu-17.txt
sbt -batch -java-home /usr/lib/jvm/zulu-11 clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=128 -prof gc -rf json -rff zulu-11.json .*' 2>&1 | tee zulu-11.txt
