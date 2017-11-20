# Jsoniter Scala 

[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) [![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Scala macros that generates codecs for case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Support of UTF-8 encoding
- Configurable serialization of strings with escaped Unicode characters to be ASCII compatible
- Configurable indenting of output
- Parsing of strings with escaped characters for JSON field names, keys & values 
- Codecs can be generated for primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, `Option`, Scala collections, 
  arrays, module classes, value classes and case classes with values/fields having any of types listed here 
- Case classes should be defined as a top-level class or directly inside of another class or object
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal` and value classes for any of them 
- Implicitly resolvable codecs for all above types
- Fields with default values that defined in a first list of arguments of the constructor are optional, other fields are required (no special annotation required)
- Fields with default values, empty options & empty collections/arrays are not serialized to provide sparse output 
- Fields can be annotated as transient or just not defined in constructor to avoid parsing and serializing at all 
- Field names can be overridden for serialization/parsing by field annotation in case classes
- Configurable mapping function for names between case classes and JSON, including predefined functions which enforce snake_case or camelCase names for all fields
- Configurable name of discriminator field for ADTs
- Configurable skipping of unexpected fields or throwing of parse exceptions
- Configurable throwing of stack-less parsing exceptions to greatly reduce impact on performance  
- Configurable turning off hex dumping of affected by error part of byte buffer to reduce impact on performance
- TODO: Generate codecs for ADTs with a specified resolving matcher
- TODO: Add ability to read/write numbers from/to string values and vice versa   
- TODO: More efficient implementation for serialization and parsing of numbers 
- TODO: Extend codecs to allow using them for customization of serialization/parsing of types to/from map keys
- TODO: Add support for configurable decimal number formatting for writing   
- TODO: Add support for some popular Scala and Java classes like `Duration`, `DateTime`, `UUID`, `URL`, etc.
- TODO: Add extension modules with code generation for 3-rd party libraries with collections that are specialized for primitive types
- TODO: Add support of UTF-16 & UTF-32 binaries and JSON encoded strings as input/output

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
import com.github.plokhotnyuk.jsoniter_scala.JsonCodecMaker
import com.github.plokhotnyuk.jsoniter_scala.CodecMakerConfig

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

val codec = JsonCodecMaker.make[User](CodecMakerConfig())
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.JsonCodec`.

Now you can use it:

```scala
import com.github.plokhotnyuk.jsoniter_scala.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.JsonWriter

val user = JsonReader.read(codec, """{"name":"John","devices":[{"id":1,model:"HTC One X"}]}""".getBytes("UTF-8"))
val jsonBytes = JsonWriter.write(codec, User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
```

To see generated code add the following line to your sbt build file

```sbt
scalaOptions += "-Xmacro-settings:print-codecs"
```

For more features & examples, please, check out
[JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/JsonCodecMakerSpec.scala)


## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by creating pull requests (code or/and tests are highly appreciated)

### Run tests and check coverage

```sh
sbt clean +coverage +test +coverageReport
```

### Run benchmarks

Sbt plugin for JMH tool is used for benchmarking, to see all their features & options please check [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](http://openjdk.java.net/projects/code-tools/jmh/). 

Learn how to write benchmarks in [JMH samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/) and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](http://psy-lob-saw.blogspot.com/p/jmh-related-posts.html) blogs. 

List of available option can be printed by:

```sh
sbt 'benchmark/jmh:run -h'
```

JMH allows to run benchmarks with different profilers, to get list of supported use:

```sh
sbt 'benchmark/jmh:run -lprof'
```

To get result for some benchmarks in flight recording file (which you can then open and analyse offline using JMC) use command like this:

```sh
sbt clean 'benchmark/jmh:run -prof jmh.extras.JFR -wi 10 -i 50 .*readGoogleMapsAPIJsoniter.*'
```

On Linux the perf profiler can be used to see CPU & system statistics normalized per ops:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof perfnorm .*TwitterAPI.*' >twitter_api_results.txt
```

To see throughput & allocation rate of generated codecs run benchmarks with GC profiler using following command:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*JsonCodecMakerBenchmark.*' >results.txt
```

Current results for the following environment:

[./results.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results.txt) Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 15.04, Linux 4.4.0-38-generic, Oracle JDK build 1.8.0_152-b16 64-bit


## Acknowledges

[Jsoniter Java](https://github.com/json-iterator/java)

[Kryo Macros](https://github.com/evolution-gaming/kryo-macros)

[AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)