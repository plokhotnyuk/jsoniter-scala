# Jsoniter Scala Examples

## How to build uber jar and run it with JVM

```sh
sbt clean +assembly

java -jar target/scala-2.11/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
java -jar target/scala-2.13/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar
```

## How to build with a native image and run binaries

```sh
sudo /usr/lib/jvm/graalvm-ce-java8/bin/gu -L install ./native-image-installable-svm-java8-linux-amd64-19.3.0.2.jar # (optional) to install the AOT compiler

/usr/lib/jvm/graalvm-ce-java8/bin/native-image --no-server --no-fallback --allow-incomplete-classpath -H:UnsafeAutomaticSubstitutionsLogLevel=3 -H:+ReportExceptionStackTraces -jar target/scala-2.12/jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT.jar

time ./jsoniter-scala-examples-assembly-0.1.0-SNAPSHOT
```