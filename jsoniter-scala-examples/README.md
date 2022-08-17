# Examples of jsoniter-scala usage

## Build uber jar, print its size, and measure its start up time on JVM

```sh
sbt jsoniter-scala-examplesJVM/assembly
ls -l .jvm/target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
perf stat -r 100 java -jar .jvm/target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar > /dev/null
```

## Build GraalVM native image, print its size, and measure its start up time

```sh
sbt jsoniter-scala-examplesJVM/nativeImage 
ls -l .jvm/target/native-image/jsoniter-scala-examples
perf stat -r 100 .jvm/target/native-image/jsoniter-scala-examples > /dev/null
```

## Build Scala Native image, print its size, and measure its start up time

```sh
sbt jsoniter-scala-examplesNative/nativeLink 
ls -l .native/target/scala-2.13/jsoniter-scala-examples-out
perf stat -r 100 .native/target/scala-2.13/jsoniter-scala-examples-out > /dev/null
```