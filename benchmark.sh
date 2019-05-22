#!/bin/bash
sbt -java-home /usr/lib/jvm/graalvm-ee-19.0.0 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmee19.json .*' 2>&1 | tee graalvmee19.txt
sbt -java-home /usr/lib/jvm/graalvm-ce-19.0.0 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmce19.json .*' 2>&1 | tee graalvmce19.txt
sbt -java-home /usr/lib/jvm/openjdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -jvm /usr/lib/jvm/openjdk-12/bin/java -p size=128 -prof gc -rf json -rff openjdk12.json .*' 2>&1 | tee openjdk12.txt
sbt -java-home /usr/lib/jvm/openjdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk11.json .*' 2>&1 | tee openjdk11.txt
sbt -java-home /usr/lib/jvm/java-8-openjdk-amd64 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk8.json .*' 2>&1 | tee openjdk8.txt
