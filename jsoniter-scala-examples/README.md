# Jsoniter Scala Examples

## How to build uber jar and run it with JVM

```sh
sbt clean assembly

perf stat -r 100 java -jar target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar > /dev/null
```

## How to build with a native image plugin and run binaries

```sh
sbt clean nativeImage 

perf stat -r 100 ./target/native-image/jsoniter-scala-examples > /dev/null
```