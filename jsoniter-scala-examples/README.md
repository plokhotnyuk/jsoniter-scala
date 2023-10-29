# Examples of jsoniter-scala usage

## Build uber jar, print its size, and measure its start up time on JVM

```sh
sudo apt install linux-tools-common linux-tools-generic
sudo sysctl kernel.perf_event_paranoid=1
sbt jsoniter-scala-examplesJVM/assembly
ls -l .jvm/target/scala-3.3.1/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
perf stat -r 100 java -jar .jvm/target/scala-3.3.1/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar > /dev/null
```

## Build GraalVM native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
sbt jsoniter-scala-examplesJVM/nativeImage 
ls -l .jvm/target/native-image/jsoniter-scala-examples
perf stat -r 100 .jvm/target/native-image/jsoniter-scala-examples > /dev/null
```

## Build Scala Native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
sbt jsoniter-scala-examplesNative/nativeLink 
ls -l .native/target/scala-3.3.1/jsoniter-scala-examples-out
perf stat -r 100 .native/target/scala-3.3.1/jsoniter-scala-examples-out > /dev/null
```