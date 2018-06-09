#!/bin/bash
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.6 clean 'benchmark/jmh:run -p size=128 -jvm /usr/lib/jvm/graalvm-ee-1.0.0-rc2/bin/java -prof gc -rf json -rff graalvmee1.json .*Benchmark.*' >graalvmee1.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.6 clean 'benchmark/jmh:run -p size=128 -jvm /usr/lib/jvm/graalvm-ce-1.0.0-rc2/bin/java -prof gc -rf json -rff graalvmce1.json .*Benchmark.*' >graalvmce1.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.6 clean 'benchmark/jmh:run -p size=128 -jvm /usr/lib/jvm/java-8-oracle/bin/java -prof gc -rf json -rff jdk8.json .*Benchmark.*' >jdk8.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.6 clean 'benchmark/jmh:run -p size=128 -jvm /usr/lib/jvm/jdk-10/bin/java -prof gc -rf json -rff jdk10.json .*Benchmark.*' >jdk10.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.6 clean 'benchmark/jmh:run -p size=128 -jvm /usr/lib/jvm/jdk-10/bin/java -jvmArgsAppend "-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler" -prof gc -rf json -rff jdk10graal.json .*Benchmark.*' >jdk10graal.txt
