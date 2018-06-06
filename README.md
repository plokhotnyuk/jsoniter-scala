# Jsoniter Scala 

[![AppVeyor build](https://ci.appveyor.com/api/projects/status/17frw06h8rjwuc6g?svg=true)](https://ci.appveyor.com/project/plokhotnyuk/jsoniter-scala)
[![TravisCI build](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) 
[![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) 
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://img.shields.io/badge/macros-0.27.4-blue.svg?)](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala/macros)

Scala macros that generate codecs for case classes, standard types and collections
to get maximum performance of JSON parsing and serialization.

[Latest results of benchmarks](http://plokhotnyuk.github.io/jsoniter-scala/) which compare parsing and serialization 
performance of Jsoniter Scala vs. [dsl-json](https://github.com/ngs-doo/dsl-json) (using its Java API only),
[Jackson](https://github.com/FasterXML/jackson-module-scala), [Circe](https://github.com/circe/circe),  
[Play-JSON](https://github.com/playframework/play-json) and [uPickle](https://github.com/lihaoyi/upickle) libraries 
using different JDK and GraalVM versions on the following environment: Intel® Core™ i7-7700HQ CPU @ 2.8GHz (max 3.8GHz), 
RAM 16Gb DDR4-2400, Ubuntu 18.04, latest versions of Oracle JDK 8/10, Oracle JDK 10 + Graal compiler, and GraalVM CE/EE

## Acknowledgments

This library started from macros that reused [Jsoniter Java](https://github.com/json-iterator/java) reader & writer and 
generated codecs for them but then evolved to have own core of mechanics for parsing and serialization. 

Idea to generate codecs by Scala macros and main details was borrowed from 
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) and adapted for needs of the JSON domain. 
  
Other Scala macros features were peeped in 
[AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons/tree/master/commons-macros/src/main/scala/com/avsystem/commons/macros)

## Goals

Initially, this library was developed for requirements of real-time bidding in ad-tech and goals are simple:
- do parsing and serialization of JSON directly from UTF-8 bytes to your case classes and Scala collections and back but 
  do it crazily fast without runtime-reflection, intermediate AST-trees, strings or hash maps, with minimum allocations 
  and copying
- do validation of UTF-8 encoding, JSON format and mapped values efficiently (fail fast approach) with clear reporting, 
  do not replace illegally encoded characters of string values by placeholder characters
- define classes, that will be instantiated during parsing, in _compile-time_ to minimize a probability of runtime issues, 
  generated sources can be inspected to prove that there are no security vulnerabilities during parsing

It targets JDK 8+ without any platform restrictions.

Support of Scala.js and Scala Native is not a goal for the moment. 

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Parsing of streaming JSON values and JSON arrays from `java.io.InputStream` without the need of holding all parsed 
  values in the memory
- Support reading part of `Array[Byte]` by specifying of position and limit of reading from/to
- Support writing to pre-allocated `Array[Byte]` by specifying of a position of writing from
- Support of UTF-8 encoding only
- Parsing of strings with escaped characters for JSON keys and string values
- Codecs can be generated for primitives, boxed primitives, enums, tuples, `String`, `BigInt`, `BigDecimal`, `Option`, 
  `Either`, `java.util.UUID`, `java.time.*`, Scala collections, arrays, module classes, value classes and case classes 
  with values/fields having any of types listed here 
- Case classes should be defined with a primary constructor that has one list of arguments for all non-transient fields
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, 
  `java.util.UUID`, `java.time.*`, and value classes for any of them 
- Parsing of escaped characters are not supported for strings which are mapped to numeric and data/time types 
- Support of first-order and higher-kinded types for case classes and value classes
- Support of ADTs with sealed trait or sealed abstract class base and case classes or case objects as leaf classes, 
  using discriminator field with string type of value
- Implicitly resolvable values codecs for JSON values and key codecs for JSON object keys that are mapped to maps
- Support only acyclic graphs of class instances
- Fields with default values that defined in the constructor are optional, other fields are required (no special 
  annotation required)
- Fields with values that are equals to default values, or are empty options/collections/arrays are not serialized to
  provide a sparse output
- Fields can be annotated as transient or just not defined in the constructor to avoid parsing and serializing at all 
- Field names can be overridden for serialization/parsing by field annotation in case classes
- Parsing exception always reports a hexadecimal offset of `Array[Byte]` or `InputStream` where it occurs and 
  an optional hex dump affected by error part of an internal byte buffer
- Configurable by field annotation ability to read/write numeric fields from/to string values
- Both key and value codecs are specialized to be work with primitives efficiently without boxing/unboxing
- No extra buffering is required when parsing from `InputStream` or serializing to `OutputStream` 
- No dependencies on extra libraries in _runtime_ excluding Scala's `scala-library`
- Support of compilation to a native image by GraalVM 
  
There are configurable options that can be set in compile-time:
- Ability to read/write numbers of containers from/to string values
- Skipping of unexpected fields or throwing of parse exceptions
- Mapping function for names between case classes and JSON, including predefined functions which enforce 
  snake_case, kebab-case or camelCase names for all fields
- Name of a discriminator field for ADTs
- Mapping function for values of a discriminator field that is used for distinguishing classes of ADTs

List of options that change parsing and serialization in runtime:
- Serialization of strings with escaped Unicode characters to be ASCII compatible
- Indenting of output and its step
- Throwing of stack-less parsing exceptions to greatly reduce impact on performance  
- Turning off hex dumping affected by error part of an internal byte buffer to reduce the impact on performance
- A preferred size of internal buffers when parsing from `InputStream` or serializing to `OutputStream`

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master) 
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Add the core library with a "compile" scope and the macros library with a "provided" scope to your dependencies list:

```sbt
libraryDependencies ++= Seq(
  "com.github.plokhotnyuk.jsoniter-scala" %% "core" % "0.27.4" % Compile, 
  "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.27.4" % Provided // required only in compile-time
)
```

Generate codecs for your case classes, collections, etc.
    
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])

implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make[User](CodecMakerConfig())
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec`.

Now you can use it for parsing and serialization:

```scala
val user = readFromArray("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""".getBytes("UTF-8"))
val json = writeToArray(User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
```

To see generated code add the following line to your sbt build file

```sbt
scalacOptions ++= Seq("-Xmacro-settings:print-codecs")
```

Full code see in the [examples](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/examples) directory 

For more use cases, please, check out tests: 
- [JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)
- [PackageSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/PackageSpec.scala)
- [JsonReaderSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReaderSpec.scala)
- [JsonWriterSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriterSpec.scala)

## Known issues

1. Scalac has a bug that affects case classes which have 2 fields where name of one is a prefix for the another name that 
contains a character that should be encoded immediately after the prefix (like `o` and `o-o`). You will got compilation 
or runtime error, depending of version of the compiler, see details here: https://github.com/scala/bug/issues/10825
W/a are: move a definition of the field with encoded chars (`o-o` in our case) to be after the field that is affected by 
the exception (after the `o` field) or rename the `o-o` field to `oO` or some other name and use the `@named("o-o")` 
annotation for it. 

## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by 
creating pull requests (fixes and improvements to docs, code, and tests are highly appreciated)

### Run tests, check coverage and binary compatibility

```sh
sbt -J-XX:MaxMetaspaceSize=512m ++2.11.12 clean coverage test coverageReport
sbt -J-XX:MaxMetaspaceSize=512m ++2.12.6 clean coverage test coverageReport
sbt -J-XX:MaxMetaspaceSize=512m clean +mimaReportBinaryIssues
```

### Run benchmarks

Sbt plugin for JMH tool is used for benchmarking, to see all their features and options please check 
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](http://openjdk.java.net/projects/code-tools/jmh/).

Learn how to write benchmarks in [JMH samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](http://psy-lob-saw.blogspot.com/p/jmh-related-posts.html) 
 blogs. 

List of available option can be printed by:

```sh
sbt 'benchmark/jmh:run -h'
```

Results of benchmark can be stored in different formats: *.csv, *.json, etc. All supported formats can be listed by:
```sh
sbt 'benchmark/jmh:run -lrf
``` 

JMH allows to run benchmarks with different profilers, to get a list of supported use:

```sh
sbt 'benchmark/jmh:run -lprof'
```

Help for profiler options can be printed by following command:

```sh
sbt 'benchmark/jmh:run -prof <profiler_name>:help'
```

For parametrized benchmarks the constant value(s) for parameter(s) can be set by `-p` option:

```sh
sbt clean 'benchmark/jmh:run -p size=1,10,100,100 .*ArrayOf.*'
```

To see throughput with allocation rate of generated codecs run benchmarks with GC profiler using the following command:

```sh
sbt clean 'benchmark/jmh:run -prof gc -rf json -rff jdk8.json .*Benchmark.*'
```

Results that are stored in JSON can be easy plotted in [JMH Visualizer](http://jmh.morethan.io/) by drugging & dropping
of your file to the drop zone or using the `source` parameter with an HTTP link to your file in the URL like 
[here](http://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/jdk8.json).

On Linux the perf profiler can be used to see CPU event statistics normalized per ops:

```sh
sbt clean 'benchmark/jmh:run -prof perfnorm .*TwitterAPI.*'
```

To get a result for some benchmarks with an in-flight recording file from JFR profiler use command like this:

```sh
sbt clean 'benchmark/jmh:run -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" -prof jmh.extras.JFR:dir=/tmp/profile-jfr;flameGraphDir=/home/andriy/Projects/com/github/brendangregg/FlameGraph;jfrFlameGraphDir=/home/andriy/Projects/com/github/chrishantha/jfr-flame-graph;verbose=true -wi 10 -i 60 .*GoogleMapsAPI.*readJsoniter.*'
```

Now you can open files from the `/tmp/profile-jfr` directory:
```sh
profile.jfr                             # JFR profile, open and analyze it using JMC
jfr-collapsed-cpu.txt                   # Data from JFR profile that are extracted for Flame Graph tool
flame-graph-cpu.svg                     # Flame graph of CPU usage 
flame-graph-cpu-reverse.svg             # Reversed flame graph of CPU usage
flame-graph-allocation-tlab.svg         # Flame graph of heap allocations in TLAB
flame-graph-allocation-tlab-reverse.svg # Reversed flame graph of heap allocations in TLAB
``` 

To run benchmarks with recordings by Async profiler, clone its repository and use command like this:

```sh
sbt -no-colors 'benchmark/jmh:run -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" -prof jmh.extras.Async:event=cpu;dir=/tmp/profile-async;asyncProfilerDir=/home/andriy/Projects/com/github/jvm-profiling-tools/async-profiler;flameGraphDir=/home/andriy/Projects/com/github/brendangregg/FlameGraph;flameGraphOpts=--color,java;verbose=true -wi 10 -i 60 .*TwitterAPIBenchmark.readJsoniterScala.*'
```

To see list of available events need to start your app or benchmark, and run `jps` command. I will show list of PIDs and
names for currently running Java processes. While your Java process still running launch the Async Profiler with the 
`list` option and ID of your process like here:  
```sh
$ ~/Projects/com/github/jvm-profiling-tools/async-profiler/profiler.sh list 6924
Perf events:
  cpu
  page-faults
  context-switches
  cycles
  instructions
  cache-references
  cache-misses
  branches
  branch-misses
  bus-cycles
  L1-dcache-load-misses
  LLC-load-misses
  dTLB-load-misses
Java events:
  alloc
  lock
```

Following command can be used to profile and print assembly code of hottest methods, but it requires [a setup of an 
additional library to make PrintAssembly feature enabled](http://psy-lob-saw.blogspot.com/2013/01/java-print-assembly.html):

```sh
sbt clean 'benchmark/jmh:run -prof perfasm -wi 10 -i 10 .*Adt.*readJsoniter.*'
```

More info about extras, options and ability to generate flame graphs see in [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

### Publish locally

Publish to local Ivy repo:

```sh
sbt publishLocal
```

Publish to local Maven repo:

```sh
sbt publishM2
```

### Release

For version numbering use [Recommended Versioning Scheme](http://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme)
that is used in the Scala ecosystem.

Double check binary and source compatibility, including behavior, and release using the following command (credentials 
are required):

```sh
sbt -J-XX:MaxMetaspaceSize=512m release
```

Do not push changes to github until promoted artifacts for the new version are not available for download on 
[Maven Central Repository](http://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/macros_2.12/)
to avoid binary compatibility check failures in triggered Travis CI builds. 
