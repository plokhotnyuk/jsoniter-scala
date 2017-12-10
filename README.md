# Jsoniter Scala 

[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) [![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Scala macros that generates codecs for case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Support reading part of `Array[Byte]` by specifying of position and limit of reading from/to
- Support writing to pre-allocated `Array[Byte]` by specifying of position of writing from
- Support of UTF-8 encoding
- Configurable serialization of strings with escaped Unicode characters to be ASCII compatible
- Configurable indenting of output
- Parsing of strings with escaped characters for JSON field names, keys & values 
- Codecs can be generated for primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, `Option`, Scala 
  collections, arrays, module classes, value classes and case classes with values/fields having any of types listed here 
- Case classes should be defined as a top-level class or directly inside of another class or object and with public 
  constructor that has one list of arguments for all non-transient fields
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal` and value 
  classes for any of them 
- Support of ADTs with sealed trait or sealed abstract class base and case classes or case objects as leaf classes, 
  using discriminator field with string type of value
- Implicitly resolvable codecs for any types
- Fields with default values that defined in the constructor are optional, other fields are required (no special 
  annotation required)
- Fields with values that are equals to default values, or are empty options/collections/arrays are not serialized to 
  provide sparse output 
- Fields can be annotated as transient or just not defined in constructor to avoid parsing and serializing at all 
- Field names can be overridden for serialization/parsing by field annotation in case classes
- Configurable mapping function for names between case classes and JSON, including predefined functions which enforce 
  snake_case or camelCase names for all fields
- Configurable name of a discriminator field for ADTs
- Configurable mapping function for values of a discriminator field that is used for distinguish classes of ADTs
- Configurable skipping of unexpected fields or throwing of parse exceptions
- Configurable throwing of stack-less parsing exceptions to greatly reduce impact on performance  
- Configurable turning off hex dumping of affected by error part of byte buffer to reduce impact on performance

For upcoming features and fixes see [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Build and publish locally for both Scala versions (release to Sonatype repo is coming)

```sh
sbt clean +publishLocal
```

Add the library to your dependencies list

```sbt
libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.1-SNAPSHOT"
```

Generate codecs for your case classes, collections, etc.
    
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

val codec = JsonCodecMaker.make[User](CodecMakerConfig())
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec`.

Now you can use it:

```scala
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter

val user = JsonReader.read(codec, """{"name":"John","devices":[{"id":1,model:"HTC One X"}]}""".getBytes("UTF-8"))
val jsonBytes = JsonWriter.write(codec, User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
```

To see generated code add the following line to your sbt build file

```sbt
scalaOptions += "-Xmacro-settings:print-codecs"
```

For more features & examples, please, check out
[JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)


## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by 
creating pull requests (fixes and improvements of docs, code/tests are highly appreciated)

### Run tests and check coverage

```sh
sbt clean +coverage +test +coverageReport
```

### Run benchmarks

Sbt plugin for JMH tool is used for benchmarking, to see all their features & options please check 
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](http://openjdk.java.net/projects/code-tools/jmh/). 

Learn how to write benchmarks in [JMH samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](http://psy-lob-saw.blogspot.com/p/jmh-related-posts.html) 
 blogs. 

List of available option can be printed by:

```sh
sbt 'benchmark/jmh:run -h'
```

JMH allows to run benchmarks with different profilers, to get list of supported use:

```sh
sbt 'benchmark/jmh:run -lprof'
```

Help for profiler options can be printed by following command:

```sh
sbt 'benchmark/jmh:run -prof <profiler_name>:help'
```

To get result for some benchmarks in flight recording file (which you can then open and analyse offline using JMC) use 
command like this:

```sh
sbt clean 'benchmark/jmh:run -prof jmh.extras.JFR -wi 10 -i 50 .*readGoogleMapsAPIJsoniter.*'
```

On Linux the perf profiler can be used to see CPU & system events normalized per ops (do 
`sudo apt-get install linux-tools` to install perf on Ubuntu):

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof perfnorm .*TwitterAPI.*' >twitter_api_perfnorm_results.txt
```

Following command can be used to profile & print assembly code of hottest methods, but it requires [setup of an 
additional library to make PrintAssembly feature enabled](http://psy-lob-saw.blogspot.com/2013/01/java-print-assembly.html) 
(just do `sudo apt-get install libhsdis0-fcml` for Ubuntu):

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof perfasm -wi 10 -i 10 .*readAdtJsoniter.*' >read_adt_perfasm_results.txt
```

To see throughput & allocation rate of generated codecs run benchmarks with GC profiler using following command:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*JsonCodecMakerBenchmark.*' >results.txt
```

Current results for the following environment(s):

[./results_jdk8.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results_jdk8.txt) 
Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 16.04, Linux 4.10.0-40-generic, 
Oracle JDK build 1.8.0_151-b12 64-bit

[./results_jdk9.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results_jdk9.txt) 
Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 16.04, Linux 4.10.0-40-generic, 
Oracle JDK build 9.0.1+11 64-bit


## Acknowledges

[Jsoniter Java](https://github.com/json-iterator/java)

[RapidJson](https://github.com/Tencent/rapidjson)

[sajson](https://github.com/chadaustin/sajson) 

[pjson](https://github.com/chadaustin/Web-Benchmarks/blob/master/json/third-party/pjson/pjson.h) 

[Kryo Macros](https://github.com/evolution-gaming/kryo-macros)

[AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)