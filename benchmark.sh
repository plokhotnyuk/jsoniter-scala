#!/bin/bash
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.5 clean 'benchmark/jmh:run -jvm /usr/lib/jvm/java-8-oracle/bin/java -prof gc -rf json -rff jdk8.json .*Benchmark.*' >jdk8.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.5 clean 'benchmark/jmh:run -jvm /usr/lib/jvm/java-9-oracle/bin/java -prof gc -rf json -rff jdk9.json .*Benchmark.*' >jdk9.txt
sbt -java-home /usr/lib/jvm/java-8-oracle -no-colors ++2.12.5 clean 'benchmark/jmh:run -jvm /usr/lib/jvm/jdk-10/bin/java -prof gc -rf json -rff jdk10.json .*Benchmark.*' >jdk10.txt
