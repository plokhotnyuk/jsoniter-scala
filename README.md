# Jsoniter Scala 

[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) 
[![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) 
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://img.shields.io/badge/macros-0.5.2-blue.svg?)](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala/macros)

Scala macros that generates codecs for case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

[Report with latest results of benchmarks](http://plokhotnyuk.github.io/jsoniter-scala/) for JDK 8 & JDK 9 on the 
following environment: Intel® Core™ i7-7700HQ CPU @ 2.8GHz (max 3.8GHz), RAM 16Gb DDR4-2400, Ubuntu 16.04, 
Linux notebook 4.13.0-26-generic, Oracle JDK builds 1.8.0_151-b12 & 9.0.1+11 64-bit

## Goals

Initially this library was developed for requirements of real-time bidding in ad-tech and goals was simple:
- do parsing & serialization of JSON directly from UTF-8 bytes to your case classes & Scala collections and back but 
  do it crazily fast w/o reflection, intermediate trees, strings or events, w/ minimum allocations & copying
- do validation of UTF-8 encoding, JSON format & mapped values efficiently with clear reporting, do not replace 
  illegally encoded characters of string values by placeholder characters

It targets JDK 8 and above w/o any platform restrictions, but may works good enough on JDK 7 with Scala 2.11.

Support of Scala.js & Scala Native is not a goal for the moment. 

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Support reading part of `Array[Byte]` by specifying of position and limit of reading from/to
- Support writing to pre-allocated `Array[Byte]` by specifying of position of writing from
- Support of UTF-8 encoding
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
- Configurable by field annotation ability to read/write numeric fields from/to string values
- No extra buffering is required when parsing from `InputStream` or serializing to `OutputStream` 
  
There are number of configurable options that can be set in compile-time:
- Ability to read/write number of containers from/to string values
- Skipping of unexpected fields or throwing of parse exceptions
- Mapping function for names between case classes and JSON, including predefined functions which enforce 
  snake_case or camelCase names for all fields
- Name of a discriminator field for ADTs
- Mapping function for values of a discriminator field that is used for distinguish classes of ADTs

List of options that change parsing & serialization in runtime:
- Serialization of strings with escaped Unicode characters to be ASCII compatible
- Indenting of output and its step
- Throwing of stack-less parsing exceptions to greatly reduce impact on performance  
- Turning off hex dumping of affected by error part of an internal byte buffer to reduce impact on performance
- Preferred size of internal buffers when parsing from `InputStream` or serializing to `OutputStream`

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master) 
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Add the library to your dependencies list

```sbt
libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.5.2"
```

Generate codecs for your case classes, collections, etc.
    
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros._

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

val codec = JsonCodecMaker.make[User](CodecMakerConfig())
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.core.JsonCodec`.

Now you can use it with reader & writer APIs for parsing & serialization accordingly:

```scala
import com.github.plokhotnyuk.jsoniter_scala.core._

val user = JsonReader.read(codec, """{"name":"John","devices":[{"id":1,model:"HTC One X"}]}""".getBytes)
val json = JsonWriter.write(codec, User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
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

### Run tests, check coverage and binary compatibility

```sh
sbt clean +coverage +test +coverageReport +mimaReportBinaryIssues
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

On Linux the perf profiler can be used to see CPU event statistics normalized per ops:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof perfnorm .*TwitterAPI.*' >twitter_api_perfnorm_results.txt
```

Following command can be used to profile & print assembly code of hottest methods, but it requires [setup of an 
additional library to make PrintAssembly feature enabled](http://psy-lob-saw.blogspot.com/2013/01/java-print-assembly.html):

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof perfasm -wi 10 -i 10 .*Adt.*readJsoniter.*' >read_adt_perfasm_results.txt
```

To see throughput with allocation rate of generated codecs run benchmarks with GC profiler using following command:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*Benchmark.*' >results.txt
```

Results of benchmark can be stored in different formats: *.csv, *.json, etc. All supported formats can be listed by:
```sh
sbt 'benchmark/jmh:run -lrf
``` 

Results that are stored in JSON can be easy plotted in [JMH Visualizer](http://jmh.morethan.io/) by drugging & dropping
of your file to the drop zone or using the `source` parameter with an HTTP link to your file in the URL like 
[here](http://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/jdk8.json).

More info about extras, including `jmh.extras.Async` and ability to generate flame graphs see in
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

### Publish to local repos

Publish to Ivy repo:

```sh
sbt publishLocal
```

Publish to Maven repo:

```sh
sbt publishM2
```

### Release

For version numbering use [Recommended Versioning Scheme](http://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme)
that is used in the Scala ecosystem.

Double check binary & source compatibility (including behaviour) and release using following command (credentials required):

```sh
sbt "release cross"
```

Do not push changes to github until promoted artifacts for new version are not available for download on 
[Maven Central Repository](http://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/macros_2.12/)
to avoid binary compatibility check failures in triggered Travis CI builds. 

## Acknowledgements

This library started from macros that reused [Jsoniter Java](https://github.com/json-iterator/java) reader & writer and 
generated codecs for them but than evolved to have own core of mechanics for parsing & serialization. 

Idea to generate codecs by Scala macros & main details was borrowed from 
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) and adapted for needs of JSON domain. 
  
Other Scala macros features was peeped in [AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)