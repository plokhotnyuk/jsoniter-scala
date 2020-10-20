# Jsoniter Scala Examples

## How to build uber jar and run it with JVM

```sh
sbt clean assembly

time java -jar target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```

## How to build with a native image and run binaries

```sh
sbt clean nativeImage 

echo performance | sudo tee /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor

time ./target/native-image/jsoniter-scala-examples
```