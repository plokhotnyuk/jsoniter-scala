# Jsoniter Scala Examples

## How to build and run

```sh
sbt clean +assembly

java -jar target/scala-2.11/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.13.0-M4/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```

## How to build a native image

```sh
/usr/lib/jvm/graalvm-ee-1.0.0-rc1/bin/native-image --no-server -H:UnsafeAutomaticSubstitutionsLogLevel=3 -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```