#!/bin/bash
sbt -java-home /usr/lib/jvm/graalvm-ee-1.0.0 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmee1.json .*Benchmark.*' 2>&1 | tee graalvmee1.txt
sbt -java-home /usr/lib/jvm/graalvm-ce-1.0.0 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmce1.json .*Benchmark.*' 2>&1 | tee graalvmce1.txt
sbt -java-home /usr/lib/jvm/jdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -jvm /usr/lib/jvm/jdk-12/bin/java -p size=128 -prof gc -rf json -rff oraclejdk12.json .*Benchmark.*' 2>&1 | tee oraclejdk12.txt
sbt -java-home /usr/lib/jvm/openjdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -jvm /usr/lib/jvm/openjdk-12/bin/java -p size=128 -prof gc -rf json -rff openjdk12.json .*Benchmark.*' 2>&1 | tee openjdk12.txt
sbt -java-home /usr/lib/jvm/jdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff oraclejdk11.json .*Benchmark.*' 2>&1 | tee oraclejdk11.txt
sbt -java-home /usr/lib/jvm/openjdk-11 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk11.json .*Benchmark.*' 2>&1 | tee openjdk11.txt
sbt -java-home /usr/lib/jvm/jdk1.8.0 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff oraclejdk8.json .*Benchmark.*' 2>&1 | tee oraclejdk8.txt
sbt -java-home /usr/lib/jvm/java-8-openjdk-amd64 -no-colors clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk8.json .*Benchmark.*' 2>&1 | tee openjdk8.txt
