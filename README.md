# Jsoniter Scala 

[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) 
[![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) 
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://img.shields.io/badge/macros-0.3.0-blue.svg?)](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala/macros)

Scala macros that generates codecs for case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

## Goals

Initially this library was developed for requirements of real-time bidding in ad-tech and goals was simple:
- do parsing & serialization of JSON directly from UTF-8 bytes to your case classes & Scala collections and back but 
  do it crazily fast w/o reflection, intermediate syntax tree, strings or events, w/ minimum allocations & copying
- do not replace illegally encoded characters of string values by placeholder characters and do not allow broken 
  surrogate pairs of characters to be parsed or serialized

It targets JDK 8 and above w/o any platform restrictions, but may works good enough on JDK 7 with Scala 2.11.

Support of Scala.js & Scala Native is not a goal for the moment. 

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Support reading part of `Array[Byte]` by specifying of position and limit of reading from/to
- Support writing to pre-allocated `Array[Byte]` by specifying of position of writing from
- Support of UTF-8 encoding
- Configurable serialization of strings with escaped Unicode characters to be ASCII compatible
- Configurable indenting of output
- Parsing of strings with escaped characters for JSON keys and string values 
- Codecs can be generated for primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, `Option`, tuples, 
  Scala collections, arrays, module classes, value classes and case classes with values/fields having any of types 
  listed here 
- Case classes should be defined as a top-level class or directly inside of another class or object and with public 
  constructor that has one list of arguments for all non-transient fields
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal` and value 
  classes for any of them 
- Support of ADTs with sealed trait or sealed abstract class base and case classes or case objects as leaf classes, 
  using discriminator field with string type of value
- Implicitly resolvable codecs for any types
- Support only acyclic graphs of class instances
- Fields with default values that defined in the constructor are optional, other fields are required (no special 
  annotation required)
- Fields with values that are equals to default values, or are empty options/collections/arrays are not serialized to 
  provide sparse output 
- Fields can be annotated as transient or just not defined in constructor to avoid parsing and serializing at all 
- Field names can be overridden for serialization/parsing by field annotation in case classes
- Parsing exception always reports a hexadecimal offset of `Array[Byte]` or `InputStream` where it occurs and hex dump 
  of affected by error part of an internal byte buffer
- Configurable turning off hex dumping of affected by error part of an internal byte buffer to reduce impact on 
  performance
- Configurable throwing of stack-less parsing exceptions to greatly reduce impact on performance  
- Configurable mapping function for names between case classes and JSON, including predefined functions which enforce 
  snake_case or camelCase names for all fields
- Configurable name of a discriminator field for ADTs
- Configurable mapping function for values of a discriminator field that is used for distinguish classes of ADTs
- Configurable by field annotation or by code generation ability to read/write numbers from/to string values
- Configurable skipping of unexpected fields or throwing of parse exceptions
- Configurable size of internal buffers when parsing from `InputStream` or serializing to `OutputStream`, no extra 
  buffering is required  

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master) 
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Add the library to your dependencies list

```sbt
libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.3.0"
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

Now you can use it with reader & writer APIs for parsing & serialization accordingly:

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

For more use cases & examples, please, check out
[JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)


## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by 
creating pull requests (fixes and improvements of docs, code and tests are highly appreciated)

### Run tests and check coverage

```sh
sbt clean +coverage +test +coverageReport
```

### Publish locally

```sh
sbt +publishLocal
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
sbt clean 'benchmark/jmh:run -prof jmh.extras.JFR -wi 10 -i 50 .*GoogleMapsAPI.*readJsoniter.*'
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
sbt -no-colors clean 'benchmark/jmh:run -prof perfasm -wi 10 -i 10 .*Adt.*readJsoniter.*' >read_adt_perfasm_results.txt
```

To see throughput & allocation rate of generated codecs run benchmarks with GC profiler using following command:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*Benchmark.*' >results.txt
```

More info about extras, including `jmh.extras.Async` and ability to generate flame graphs see in
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

Current results for the following environment(s):

[./results_jdk8.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results_jdk8.txt) 
Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 16.04, Linux 4.10.0-40-generic, 
Oracle JDK build 1.8.0_151-b12 64-bit

[./results_jdk9.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results_jdk9.txt) 
Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 16.04, Linux 4.10.0-40-generic, 
Oracle JDK build 9.0.1+11 64-bit


## Acknowledgements

This library started from macros that reused [Jsoniter Java](https://github.com/json-iterator/java) reader & writer and 
generated codecs for them but than evolved to have own core of mechanics for parsing & serialization. 

Idea to generate codecs by Scala macros & main details was borrowed from 
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) and adapted for needs of JSON domain. 
  
Other Scala macros features was peeped in [AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)