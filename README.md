# Jsoniter Scala 

[![AppVeyor build](https://ci.appveyor.com/api/projects/status/17frw06h8rjwuc6g?svg=true)](https://ci.appveyor.com/project/plokhotnyuk/jsoniter-scala)
[![TravisCI build](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) 
[![Code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala)
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scaladex](https://img.shields.io/badge/jsoniter--scala--macros-0.39.0-blue.svg)](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala/jsoniter-scala-macros)

Scala macros that generate codecs for case classes, standard types and collections
to get maximum performance of JSON parsing and serialization.

[Latest results of benchmarks](http://plokhotnyuk.github.io/jsoniter-scala/) which compare parsing and serialization 
performance of Jsoniter Scala with [Circe](https://github.com/circe/circe), 
[Play-JSON](https://github.com/playframework/play-json), [Jackson](https://github.com/FasterXML/jackson-module-scala), 
[uPickle](https://github.com/lihaoyi/upickle), [AVSystem's scala-commons](https://github.com/AVSystem/scala-commons),
[DSL-JSON](https://github.com/ngs-doo/dsl-json) and [Jsoniter Java](https://github.com/json-iterator/java) libraries 
using different JDK and GraalVM versions on the following environment: Intel® Core™ i7-7700 CPU @ 3.6GHz (max 4.2GHz),
RAM 16Gb DDR4-2400, Ubuntu 18.04, latest versions of Open JDK 8, Oracle JDK 8, Open JDK 11, Oracle JDK 11,
Open JDK 11 + Graal JIT, Oracle JDK 11 + Graal JIT, and GraalVM CE/EE

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
  do it crazily fast without runtime-reflection, intermediate ASTs, strings or hash maps, with minimum allocations and
  copying
- do validation of UTF-8 encoding, JSON format and mapped values efficiently (fail fast approach) with clear reporting, 
  do not replace illegally encoded characters of string values by placeholder characters
- define classes, that will be instantiated during parsing, in _compile-time_ to minimize a probability of runtime issues, 
  generated sources can be inspected to prove that there are no security vulnerabilities during parsing

It targets JDK 8+ without any platform restrictions.

Support of Scala.js and Scala Native is not a goal for the moment. 

## Features and limitations
- JSON parsing from `Array[Byte]`, `java.io.InputStream` or `java.nio.ByteBuffer`
- JSON serialization to `Array[Byte]`, `java.io.OutputStream` or `java.nio.ByteBuffer`
- Support of parsing from or writing to part of `Array[Byte]` or `java.nio.ByteBuffer` by specifying of position and 
  limit
- Parsing of streaming JSON values and JSON arrays from `java.io.InputStream` without the need of holding all parsed 
  values in the memory
- Support of UTF-8 encoding only 
- Parsing of strings with escaped characters for JSON keys and string values
- Codecs can be generated for primitives, boxed primitives, enums, tuples, `String`, `BigInt`, `BigDecimal`, `Option`, 
  `Either`, `java.util.UUID`, `java.time.*` (to/from ISO-8601 representation only), Scala collections, arrays, module 
  classes, value classes and case classes with values/fields having any of types listed here
- Classes should be defined with a primary constructor that has one list of arguments for all non-transient fields
- Non-case Scala classes also supported but they should have getter accessors for all arguments of a primary 
  constructor
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`,
  `java.util.UUID`, `java.time.*`, and value classes for any of them
- Codecs for sorted maps and sets can be customized by implicit `Ordering[K]` instances for keys that are available at 
  the scope of the `make` macro call  
- Parsing of escaped characters is not supported for strings which are mapped to numeric and `java.time.*` types 
- Support of first-order and higher-kinded types
- Support of 2 representations of ADTs with a sealed trait or a Scala class as base type and non-abstract Scala classes 
  or objects as leaf classes: 1st representation uses discriminator field with string type of value, 2nd one uses string 
  values for objects and a wrapper JSON object with a discriminator key for case class instances
- Implicitly resolvable value codecs for JSON values and key codecs for JSON object keys that are mapped to maps allows
  to inject your custom codecs for adding support of other types or for altering representation in JSON for already 
  supported classes
- Type aliases are also supported for all types  
- Order of instance fields is preserved during serialization 
- Only acyclic graphs of class instances are supported
- Throws a parsing exception if duplicated keys were detected for a class instance (except maps)
- Fields with default values that defined in the constructor are optional, other fields are required (no special 
  annotation required)
- Fields with values that are equals to default values, or are empty options/collections/arrays are not serialized to
  provide a sparse output
- Any values that used directly or as part of default values of the constructor parameters should have right 
  implementations of the `equals` method (it mostly concerns non-case classes or other types that have custom codecs)
- Fields can be annotated as transient or just not defined in the constructor to avoid parsing and serializing at all 
- Field names can be overridden for serialization/parsing by field annotation in the primary constructor of classes
- Parsing exception always reports a hexadecimal offset of `Array[Byte]`, `java.io.InputStream` or `java.nio.ByteBuffer`
  where it occurs and an optional hex dump affected by error part of an internal byte buffer
- Configurable by field annotation ability to read/write numeric fields from/to string values
- Both key and value codecs are specialized to be work with primitives efficiently without boxing/unboxing
- No extra buffering is required when parsing from `java.io.InputStream` or serializing to `java.io.OutputStream`
- No dependencies on extra libraries in _runtime_ excluding Scala's `scala-library`
- Releases for different Scala versions: 2.11, 2.12, 2.13.0-M4, 2.13.0-M5  
- Support of compilation to a native image by GraalVM
  
There are configurable options that can be set in compile-time:
- Ability to read/write numbers from/to string values
- Skipping of unexpected fields or throwing of parse exceptions
- Skipping of serialization of field values that matched with defaults which is defined in the primary constructor can
  be turned off to force serialization of that values
- Mapping function for names between classes and JSON, including predefined functions which enforce snake_case, 
  kebab-case or camelCase names for all fields
- An optional name of the discriminator field for ADTs
- Mapping function for values of a discriminator field that is used for distinguishing classes of ADTs
- Ability to alter default of precision, scale limit, and max number of significant digits when parsing `BigDecimal` values
- Ability to alter default of max number of digits when parsing `BigDecimal` values
- Ability to alter default of max value when parsing bit sets
- Ability to alter default of limit for inserts when parsing sets or maps

List of options that change parsing and serialization in runtime:
- Serialization of strings with escaped Unicode characters to be ASCII compatible
- Indenting of output and its step
- Throwing of stack-less parsing exceptions by default to greatly reduce impact on performance, while stack traces can 
  be turned on in development for debugging
- Turning off hex dumping affected by error part of an internal byte buffer to reduce the impact on performance
- A preferred size of internal in buffers when parsing from `java.io.InputStream` or `java.nio.DirectByteBuffer`
- A preferred size of internal out buffers when serializing to `java.io.OutputStream` or `java.nio.DirectByteBuffer`
- A preferred size of char buffers when parsing string values

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master) 
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Add the core library with a "compile" scope and the macros library with a "provided" scope to your dependencies list:

```sbt
libraryDependencies ++= Seq(
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "0.39.0" % Compile, 
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "0.39.0" % Provided // required only in compile-time
)
```

Generate codecs for your Scala classes and collections:
    
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

If you don't know how make your data structures from scratch but have a JSON sample then use on-line services
[1](http://json2caseclass.cleverapps.io/) [2](https://transform.now.sh/json-to-scala-case-class/) to generate an initial
version of them.

To see generated code for codecs add the following line to your sbt build file

```sbt
scalacOptions ++= Seq("-Xmacro-settings:print-codecs")
```

Full code see in the [examples](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-examples) directory 

For more use cases, please, check out tests: 
- [JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)
- [PackageSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/PackageSpec.scala)
- [JsonReaderSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReaderSpec.scala)
- [JsonWriterSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriterSpec.scala)

Samples for integration with different web frameworks:
- [colossus](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/colossus/src/main/scala/example/Main.scala)
- [blaze](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/blaze/src/main/scala/Main.scala)
- [Play (with Netty native transport)](https://github.com/plokhotnyuk/play/tree/master/src/main/scala/microservice)
- [akka-http](https://github.com/hseeberger/akka-http-json/blob/master/akka-http-jsoniter-scala/src/test/scala/de/heikoseeberger/akkahttpjsoniterscala/ExampleApp.scala)
- [http4s](https://github.com/TechEmpower/FrameworkBenchmarks/blob/d1f960b2d4d6ea7b5c30a3ef2a8b47670f346f1c/frameworks/Scala/http4s/src/main/scala/WebServer.scala)

## Known issues

1. Scalac has a bug that affects case classes which have 2 fields where name of one is a prefix for the another name that
contains a character which should be encoded immediately after the prefix (like `o` and `o-o`). You will got compilation 
or runtime error, depending on the version of the compiler, see details here: https://github.com/scala/bug/issues/11212

The workaround is to move a definition of the field with encoded chars (`o-o` in our case) to be after the field that is
affected by the exception (after the `o` field)

2. A configuration parameter for the `make` macro is evaluated in compile time only that require no dependency on other 
code that uses result of the macro's call. In that case the following compilation error will be reported: 

```
[error] Cannot evaluate a parameter of the 'make' macro call for type 'full.name.of.YourType'. It should not depend on 
        code from the same compilation module where the 'make' macro is called. Use a separated submodule of the project
        to compile all such dependencies before their usage for generation of codecs.
```

But sometime scalac (or zinc) can fail to compile the `make` macro call with the same error message for configuration 
that has not clear dependencies on other code. For that cases workarounds can be simpler than recommended usage of 
separated submodule: 
- isolate the `make` macro call(s) in the separated object, like in [this PR](https://github.com/plokhotnyuk/play/pull/5/files)
- move jsoniter-scala imports to be local, like [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorld.scala#L6-L7)
and [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorldController.scala#L12)
- use `sbt clean compile stage` or `sbt clean test stage` instead of just `sbt clean stage`, like in 
[this repo](https://github.com/hochgi/HTTP-stream-exercise/tree/jsoniter-2nd-round)

3. Scalac can throw the following stack overflow exception on `make` call for ADTs with objects if the call and the ADT 
definition are enclosed in the definition of some outer class (for more details see: https://github.com/scala/bug/issues/11157):

```
java.lang.StackOverflowError
    ...
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	...
``` 

Workarounds are:
- don't enclose ADTs with object into outer classes
- use outer object (not a class) instead 

## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by 
creating pull requests (fixes and improvements to docs, code, and tests are highly appreciated)

### Run tests, check coverage and binary compatibility

```sh
sbt clean coverage test coverageReport
sbt -J-XX:MaxMetaspaceSize=512m clean +test +mimaReportBinaryIssues
```

BEWARE that jsoniter-scala is included into [Scala Community Build](https://github.com/scala/community-builds)
 for 2.11.x, 2.12.x, and 2.13.x versions of Scala. 

### Run benchmarks

Before benchmark running check if your CPU works in `performance` mode (not a `powersave` one). On Linux use following 
commands to print current and set the `performance` mode: 

```sh
cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo performance | sudo tee /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
```

Sbt plugin for JMH tool is used for benchmarking, to see all their features and options please check 
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](http://openjdk.java.net/projects/code-tools/jmh/).

Learn how to write benchmarks in [JMH samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](http://psy-lob-saw.blogspot.com/p/jmh-related-posts.html) 
 blogs.

List of available option can be printed by:

```sh
sbt 'jsoniter-scala-benchmark/jmh:run -h'
```

Results of benchmark can be stored in different formats: *.csv, *.json, etc. All supported formats can be listed by:
```sh
sbt 'jsoniter-scala-benchmark/jmh:run -lrf'
``` 

JMH allows to run benchmarks with different profilers, to get a list of supported use (can require entering of user 
password):

```sh
sbt 'jsoniter-scala-benchmark/jmh:run -lprof'
```

Help for profiler options can be printed by following command (`<profiler_name>` should be replaced by name of the 
supported profiler from the command above):

```sh
sbt 'jsoniter-scala-benchmark/jmh:run -prof <profiler_name>:help'
```

For parametrized benchmarks the constant value(s) for parameter(s) can be set by `-p` option:

```sh
sbt clean 'jsoniter-scala-benchmark/jmh:run -p size=1,10,100,1000 .*ArrayOf.*'
```

To see throughput with allocation rate of generated codecs run benchmarks with GC profiler using the following command:

```sh
sbt clean 'jsoniter-scala-benchmark/jmh:run -prof gc -rf json -rff jdk8.json .*Benchmark.*'
```

Results that are stored in JSON can be easy plotted in [JMH Visualizer](http://jmh.morethan.io/) by drugging & dropping
of your file to the drop zone or using the `source` parameter with an HTTP link to your file in the URL like 
[here](http://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/oraclejdk11.json).

On Linux the perf profiler can be used to see CPU event statistics normalized per ops:

```sh
sbt clean 'jsoniter-scala-benchmark/jmh:run -prof perfnorm .*TwitterAPI.*JsoniterScala.*'
```

To get a result for some benchmarks with an in-flight recording file from JFR profiler use command like this:

```sh
sbt clean 'jsoniter-scala-benchmark/jmh:run -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" -prof jmh.extras.JFR:dir=/tmp/profile-jfr;flameGraphDir=/home/andriy/Projects/com/github/brendangregg/FlameGraph;jfrFlameGraphDir=/home/andriy/Projects/com/github/chrishantha/jfr-flame-graph;verbose=true -wi 10 -i 60 .*GoogleMapsAPI.*readJsoniter.*'
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

To run benchmarks with recordings by [Async profiler](https://github.com/jvm-profiling-tools/async-profiler), clone its 
repository and use command like this:

```sh
sbt -no-colors 'jsoniter-scala-benchmark/jmh:run -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" -prof jmh.extras.Async:event=cpu;dir=/tmp/profile-async;asyncProfilerDir=/home/andriy/Projects/com/github/jvm-profiling-tools/async-profiler;flameGraphDir=/home/andriy/Projects/com/github/brendangregg/FlameGraph;flameGraphOpts=--color,java;verbose=true -wi 10 -i 60 .*TwitterAPIBenchmark.readJsoniterScala.*'
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
sbt clean 'jsoniter-scala-benchmark/jmh:run -prof perfasm -wi 10 -i 10 .*Adt.*readJsoniter.*'
```

More info about extras, options and ability to generate flame graphs see in [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

Other benchmarks with results for jsoniter-scala:
- [comparison](https://github.com/dkomanov/scala-serialization/pull/8) with best binary parsers and serializers for Scala
- [comparison](https://github.com/guillaumebort/mison/pull/1) with a state of the art filter that by "building
  structural indices converts control flow into data flow, thereby largely eliminating inherently unpredictable branches
  in the program and exploiting the parallelism available in modern processors"

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
[Maven Central Repository](http://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala)
to avoid binary compatibility check failures in triggered Travis CI builds. 

Create PRs with updated version of jsoniter-scala for OSS projects which depends on it in case of binary incompatible or 
security release:
1. [hseeberger/akka-http-json](https://github.com/hseeberger/akka-http-json/blob/master/build.sbt)
2. [ovotech/kafka-serialization](https://github.com/ovotech/kafka-serialization/blob/master/build.sbt)
3. [azhur/kafka-serde-scala](https://github.com/azhur/kafka-serde-scala/blob/master/build.sbt)
4. [permutive/fs2-google-pubsub](https://github.com/permutive/fs2-google-pubsub/blob/master/build.sc)
5. [loco/loco](https://github.com/yarhrn/loco/blob/master/project/Dependencies.scala)
6. [pityka/flatjoin](https://github.com/pityka/flatjoin/blob/master/build.sbt)
7. [pityka/tasks](https://github.com/pityka/tasks/blob/master/build.sbt)
8. [TechEmpower/FrameworkBenchmarks](https://github.com/TechEmpower/FrameworkBenchmarks/search?q=jsoniter-scala&unscoped_q=jsoniter-scala)
