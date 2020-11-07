# Jsoniter Scala Examples

## How to turn on performance (not power save) mode 

```
echo performance | sudo tee /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
```

## How to build uber jar and run it with JVM

```sh
sbt clean assembly

time java -jar target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```

## How to build with a native image plugin and run binaries

```sh
sbt clean nativeImage 

time ./target/native-image/jsoniter-scala-examples
```