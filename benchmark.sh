#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-19 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmee19.json .*' 2>&1 | tee graalvmee19.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-19 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvmce19.json .*' 2>&1 | tee graalvmce19.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -jvm /usr/lib/jvm/openjdk-13/bin/java -jvmArgsAppend "-XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler" -p size=128 -prof gc -rf json -rff openjdk13graal.json .*' 2>&1 | tee openjdk13graal.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -jvm /usr/lib/jvm/openjdk-13/bin/java -p size=128 -prof gc -rf json -rff openjdk13.json .*' 2>&1 | tee openjdk13.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk11.json .*' 2>&1 | tee openjdk11.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-8 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk8.json .*' 2>&1 | tee openjdk8.txt
