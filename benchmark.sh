#!/bin/bash
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java11.json .*' 2>&1 | tee graalvm-ee-java11.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ee-java8 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ee-java8.json .*' 2>&1 | tee graalvm-ee-java8.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java11.json .*' 2>&1 | tee graalvm-ce-java11.txt
sbt -batch -java-home /usr/lib/jvm/graalvm-ce-java8 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff graalvm-ce-java8.json .*' 2>&1 | tee graalvm-ce-java8.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-15 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -jvmArgsAppend "-XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler" -p size=128 -prof gc -rf json -rff openjdk-15-graal.json .*' 2>&1 | tee openjdk-15-graal.txt
sbt -batch -java-home /usr/lib/jvm/openjdk-15 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff openjdk-15.json .*' 2>&1 | tee openjdk-15.txt
sbt -batch -java-home /usr/lib/jvm/corretto-11 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff corretto-11.json .*' 2>&1 | tee corretto-11.txt
sbt -batch -java-home /usr/lib/jvm/corretto-8 -Dmacro.settings=print-codecs clean 'jsoniter-scala-benchmark/jmh:run -p size=128 -prof gc -rf json -rff corretto-8.json .*' 2>&1 | tee corretto-8.txt
