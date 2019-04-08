# Jsoniter Scala Examples

## How to build uber jar and run it with JVM

```sh
sbt clean +assembly

java -jar target/scala-2.11/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.13.0-M5/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.13.0-RC1/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```

## How to build with a native image and run binaries

```sh
/usr/lib/jvm/graalvm-ee-1.0.0/bin/native-image --no-server -H:UnsafeAutomaticSubstitutionsLogLevel=3 -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar

./jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT
```