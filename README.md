# Jsoniter Scala
 
[![Actions Build](https://github.com/plokhotnyuk/jsoniter-scala/workflows/build/badge.svg)](https://github.com/plokhotnyuk/jsoniter-scala/actions)
[![TravisCI Build](https://travis-ci.com/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.com/plokhotnyuk/jsoniter-scala) 
[![Coverage Status](https://coveralls.io/repos/github/plokhotnyuk/jsoniter-scala/badge.svg?branch=master)](https://coveralls.io/github/plokhotnyuk/jsoniter-scala?branch=master)
[![Scala Steward](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Gitter Chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](https://www.scala-js.org)
[![Maven Central](https://img.shields.io/badge/maven--central-2.7.1-blue.svg)](https://search.maven.org/search?q=jsoniter-scala-macros)

Scala macros that generate codecs for case classes, standard types, and collections to get maximum performance of JSON 
parsing and serialization.

[**Latest results of benchmarks on JVMs**](https://plokhotnyuk.github.io/jsoniter-scala/) that compare parsing and
serialization performance of jsoniter-scala with [AVSystem's scala-commons](https://github.com/AVSystem/scala-commons), 
[Borer](https://github.com/sirthias/borer), [Circe](https://github.com/circe/circe), [DSL-JSON](https://github.com/ngs-doo/dsl-json), 
[Jackson](https://github.com/FasterXML/jackson-module-scala), [Play-JSON](https://github.com/playframework/play-json),
[play-json-jsoniter](https://github.com/evolution-gaming/play-json-tools/tree/master/play-json-jsoniter/src),
[Spray-JSON](https://github.com/spray/spray-json), [uPickle](https://github.com/lihaoyi/upickle),
[weePickle](https://github.com/rallyhealth/weePickle), and [zio-json](https://github.com/zio/zio-json) 
libraries using different JDK and GraalVM versions on the following environment: Intel® Core™ i9-9880H CPU @ 2.3GHz
(max 4.8GHz), RAM 16Gb DDR4-2400, macOS Mojave 10.14.6, and latest versions of Amazon Corretto 8/11, OpenJDK 17
(early-access build) [*](https://docs.google.com/spreadsheets/d/1IxIvLoLlLb0bxUaRgSsaaRuXV0RUQ3I04vFqhDc2Bt8/edit?usp=sharing),
GraalVM CE 21.1 (dev build) for Java 11/16, GraalVM EE 21 (release) for Java 8/11.

[**Latest results of benchmarks on browsers**](https://plokhotnyuk.github.io/jsoniter-scala/index-scalajs.html) that 
compares the same libraries on the same environment by the same code which is compiled by Scala.js to ES 5.1 with GCC
optimizations applied.

## Contents
- [Acknowledgments](#acknowledgments)
- [Goals](#goals)
- [Features and limitations](#features-and-limitations)
- [**How to use**](#how-to-use)
- [Known issues](#known-issues)
- [How to develop](#how-to-develop)

## Acknowledgments

This library had started from macros that reused [jsoniter (json-iterator) for Java](https://github.com/json-iterator/java) 
reader and writer but then the library evolved to have its own core of mechanics for parsing and serialization.

The idea to generate codecs by Scala macros and main details were borrowed from
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) and adapted for the needs of the JSON domain.
  
Other Scala macros features were peeped in [AVSystem Commons](https://github.com/AVSystem/scala-commons/tree/master/commons-macros/src/main/scala/com/avsystem/commons/macros)
and [magnolia](https://github.com/propensive/magnolia) libraries.

Ideas for the most efficient parsing and serialization of `java.time.*` values were inspired by
[DSL-JSON](https://github.com/ngs-doo/dsl-json)'s implementation for `java.time.OffsetDateTime`.

Other projects that have helped deliver unparalleled safety and performance characteristics for parsing and 
serialization of floating-point and big numbers:
- [Schubfach](https://github.com/c4f7fcce9cb06515/Schubfach/) - the most efficient and concise way to serialize doubles 
  and floats to the textual representation
- [rust-lexical](https://github.com/Alexhuszagh/rust-lexical) - the most efficient way to parse floats and doubles from
  the textual representation precisely
- [big-math](https://github.com/eobermuhlner/big-math) - parsing of `BigInt` and `BigDecimal` values with the `O(n^1.5)`
  complexity instead of `O(n^2)` using Java's implementations where `n` is a number of digits 

## Goals

1. **Safety**: validate parsed values safely with the fail-fast approach and clear reporting, provide configurable 
limits for suboptimal data structures with safe defaults to be resilient for DoS attacks, generate codecs that create
instances of a _fixed_ set of classes during parsing to avoid RCE attacks
2. **Correctness**: support the latest JSON format (RFC-8259), do not replace illegally encoded characters of string 
values by placeholder characters, parse numbers with limited binary representation doing half even rounding for too 
long JSON numbers, serialize floats and doubles to the _shortest_ textual representation without loosing of precision 
3. **Speed**: do parsing and serialization of JSON directly from UTF-8 bytes to your data structures and back, do it 
crazily fast without using of run-time reflection, intermediate ASTs, strings or hash maps, with minimum allocations and
copying
4. **Productivity**: derive codecs recursively for complex types using one line macro, do it in _compile-time_ to 
minimize the probability of run-time issues, optionally print generated sources as compiler output to be inspected for 
proving safety and correctness or to be reused as a starting point for the implementation of custom codecs, prohibit 
serializing of `null` Scala values and parsing immediately to them in generated codecs
5. **Ergonomics**: have preconfigured defaults for the safest and common usage that can be easily altered by compile- 
and run-time configuration instances, combined with compile-time annotations and implicits, embrace the textual 
representation of JSON providing a pretty printing option, provide a hex dump in the error message to speed up the
view of an error context

The library targets JDK 8+ and GraalVM 19+ (including compilation to native images) without any platform restrictions.

## Features and limitations
- JSON parsing from `Array[Byte]`, `java.io.InputStream` or `java.nio.ByteBuffer`
- JSON serialization to `Array[Byte]`, `java.io.OutputStream` or `java.nio.ByteBuffer`
- Support of parsing from or writing to part of `Array[Byte]` or `java.nio.ByteBuffer` by specifying of position and 
  limit
- Parsing of streaming JSON values and JSON arrays from `java.io.InputStream` without the need of holding all parsed
  values in the memory
- Only UTF-8 encoding is supported when working with buffered bytes directly but there is a fallback to parse and
  serialize JSON from/to `String` (while this is much less efficient)
- Parsing of strings with escaped characters for JSON keys and string values
- Codecs can be generated for primitives, boxed primitives, enums, tuples, `String`, `BigInt`, `BigDecimal`, `Option`,
  `Either`, `java.util.UUID`, `java.time.*` (to/from ISO-8601 representation only), Scala collections, arrays, module
  classes, literal types, value classes, and case classes with values/fields having any of types listed here
- Classes should be defined with a primary constructor that has one list of arguments for all non-transient fields
- Non-case Scala classes also supported, but they should have getter accessors for all arguments of a primary
  constructor
- Types that supported as map keys are primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`,
  `java.util.UUID`, `java.time.*`, literal types, and value classes for any of them
- Codecs for sorted maps and sets can be customized by implicit `Ordering[K]` instances for keys that are available at
  the scope of the `make` macro call
- Core module support reading and writing byte arrays from/to Base16 and Base64 representations (RFC 4648) for using in 
  custom codecs    
- Parsing of escaped characters is not supported for strings which are mapped to byte arrays, numeric and `java.time.*` 
  types
- Support of first-order and higher-kind types
- Support of 2 representations of ADTs with a sealed trait or a Scala class as a base type and non-abstract Scala 
  classes or objects as leaf classes: 1st representation uses discriminator field with string type of value, 2nd one 
  uses string values for objects and a wrapper JSON object with a discriminator key for case class instances
- Implicitly resolvable value codecs for JSON values and key codecs for JSON object keys that are mapped to maps allows
  to inject your custom codecs for adding support of other types or for altering representation in JSON for already
  supported classes
- Type aliases are supported for all types mentioned above
- Only acyclic graphs of class instances are supported by generated codecs
- Order of instance fields is preserved during serialization for generated codecs
- Throws a parsing exception if duplicated keys were detected for a class instance (except maps)
- Serialization of `null` values is prohibited by throwing of `NullPointerException` errors
- Parsing of `null` values allowed only for optional of collection types (that means the `None` value or an empty 
  collection accordingly) and for fields which have defined non-null default values
- Fields with default values that defined in the constructor are optional, other fields are required (no special
  annotation required)
- Fields with values that are equals to default values, or are empty options/collections/arrays are not serialized to
  provide a sparse output
- Any values that used directly or as part of default values of the constructor parameters should have right
  implementations of the `equals` method (it mostly concerns non-case classes or other types that have custom codecs)
- Fields can be annotated as transient or just not defined in the constructor to avoid parsing and serializing at all
- Field names can be overridden for serialization/parsing by field annotation in the primary constructor of classes
- Reading and writing of any arbitrary bytes or raw values are possible by using custom codecs
- Parsing exception always reports a hexadecimal offset of `Array[Byte]`, `java.io.InputStream` or `java.nio.ByteBuffer`
  where it occurs, and an optional hex dump affected by error part of an internal byte buffer
- Configurable by field annotation ability to read/write numeric fields from/to string values
- Both key and value codecs are specialized to work with primitives efficiently without boxing/unboxing
- No extra buffering is required when parsing from `java.io.InputStream` or serializing to `java.io.OutputStream`
- Using black box macros only for codec generation ensures that your types will never be changed
- Ability to print all generated code for codecs using a custom scala compiler option: `-Xmacro-settings:print-codecs`
- No dependencies on extra libraries in _runtime_ excluding Scala's `scala-library`
- Releases for different Scala versions: 2.11, 2.12, 2.13
- Support of shading to another package for locking on a particular released version
- Patch versions are backward and forward compatible 
- Support of compilation to a native image by GraalVM
- Support of Scala.js 1.0+
  
There are configurable options that can be set in compile-time:
- Ability to read/write numbers from/to string values
- Skipping of unexpected fields or throwing of parse exceptions
- Skipping of serialization of fields that have empty collection values can be turned off to force serialization of them
- Skipping of serialization of fields that have empty optional values can be turned off to force serialization of them
- Skipping of serialization of fields which values are matched with defaults that are defined in the primary constructor
  can be turned off to force serialization of that values
- Mapping functions from names of classes and their fields to JSON keys or from names of Java enumeration values to 
  JSON strings and back, including predefined functions which enforce snake_case, kebab-case, camelCase or 
  PascalCase names for all fields in the generated codec
- An optional name of the discriminator field for ADTs
- Mapping function for values of a discriminator field that is used for distinguishing classes of ADTs
- Ability to set a precision, a scale limit, and the max number of significant digits when parsing `BigDecimal` values
- Ability to set the max number of significant digits when parsing `BigInt` values
- Ability to set the max allowed value when parsing bit sets
- Ability to set the limit for the number of inserts when parsing sets or maps
- Throwing of a compilation error for recursive data structures can be turned off
- Throwing of a runtime error when the discriminator is not the first field can be turned off
- Ability to parse/serialize Scala enumeration from/to id numbers

List of options that change parsing and serialization in runtime:
- Serialization of strings with escaped Unicode characters to be ASCII compatible
- Indenting of output and its step
- Throwing of stack-less parsing exceptions by default to greatly reduce the impact on performance, while stack traces
  can be turned on in development for debugging
- Turning off hex dumping affected by error part of an internal byte buffer to reduce the impact on performance
- Preferred size of internal input buffers when parsing from `java.io.InputStream` or `java.nio.DirectByteBuffer`
- Preferred size of internal output buffers when serializing to `java.io.OutputStream` or `java.nio.DirectByteBuffer`
- Preferred size of char buffers when parsing string values

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master)
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Let's assume that you have the following data structures:
```scala
case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])
```

Add the core library with a "compile" scope and the macros library with "compile-internal" or "provided" scopes to your 
list of dependencies:
```sbt
libraryDependencies ++= Seq(
  // Use the %%% operator instead of %% for Scala.js  
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.7.1",
  // Use the "provided" scope instead when the "compile-internal" scope is not supported  
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.7.1" % "compile-internal"
)
```

Derive a codec for the top-level type that need to be parsed or serialized:
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec`.

Now use it for parsing and serialization:
```scala
val user = readFromArray("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""".getBytes("UTF-8"))
val json = writeToArray(User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
```

To see generated code for codecs add the following line to your sbt build file:
```sbt
scalacOptions ++= Seq("-Xmacro-settings:print-codecs")
```

Full code see in the [examples](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-examples/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/examples/Example01.scala)
directory.

Use [macrolizer](https://github.com/sirthias/macrolizer) to print the code for a selected macro call only.

Also, you can use the following on-line services to generate an initial version of your data structures from JSON 
samples: [json2caseclass](https://json2caseclass.cleverapps.io/), [json-to-scala-case-class](https://transform.now.sh/json-to-scala-case-class/), 
and [json2classes](https://chadselph.github.io/json2classes/). 

For more use cases, please, check out tests:
- [JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)
- [PackageSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/PackageSpec.scala)
- [JsonReaderSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReaderSpec.scala)
- [JsonWriterSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriterSpec.scala)

Samples for integration with different web frameworks:
- [akka-http](https://github.com/hseeberger/akka-http-json/blob/master/akka-http-jsoniter-scala/src/test/scala/de/heikoseeberger/akkahttpjsoniterscala/ExampleApp.scala)
- [blaze](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/blaze/src/main/scala/Main.scala)
- [colossus](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/colossus/src/main/scala/example/Main.scala)
- [http4s](https://github.com/TechEmpower/FrameworkBenchmarks/blob/d1f960b2d4d6ea7b5c30a3ef2a8b47670f346f1c/frameworks/Scala/http4s/src/main/scala/WebServer.scala)
- [Play (with Netty native transport)](https://github.com/plokhotnyuk/play/tree/master/src/main/scala/microservice)

Other usages of jsoniter-scala:
- [Dijon](https://github.com/jvican/dijon) - support of schema-less JSON using safe and efficient AST representation
- [play-json-jsoniter](https://github.com/evolution-gaming/play-json-tools) - provides the fastest way to convert an 
instance of `play.api.libs.json.JsValue` to byte array (or byte buffer, or output stream) and read it back
- [scalatest-json](https://github.com/stephennancekivell/scalatest-json) - Scalatest matchers with appropriate equality 
and descriptive error messages
- [tapir](https://tapir.softwaremill.com/en/latest/endpoint/json.html#jsoniter-scala) - Typed API descRiptions

For all dependent projects it is recommended to use [sbt-updates plugin](https://github.com/rtimush/sbt-updates) or
[Scala steward service](https://github.com/scala-steward) to keep up with using of the latest releases.

## Known issues

1. There is no validation for the length of JSON representation during parsing.

So if your system is sensitive for that and can accept untrusted input then avoid parsing with `java.io.InputStream` and
check the input length for other ways of parsing.

2. Scalac has a bug that affects case classes which have 2 fields where the name of one is a prefix for another name
that contains a character that should be encoded immediately after the prefix (like `o` and `o-o`). You will get 
compilation or runtime error, depending on the version of the compiler, see details [here](https://github.com/scala/bug/issues/11212).

The workaround is to move a definition of the field with encoded chars (`o-o` in our case) to be after the field that is
affected by the exception (after the `o` field).

3. A configuration parameter for the `make` macro is evaluated in compile-time only and requires no dependency on other
code that uses a result of the macro's call, otherwise the following compilation error will be reported:
```
[error] Cannot evaluate a parameter of the 'make' macro call for type 'full.name.of.YourType'. It should not depend on
        code from the same compilation module where the 'make' macro is called. Use a separated submodule of the project
        to compile all such dependencies before their usage for generation of codecs.
```
Sometime scalac (or zinc) can fail to compile the `make` macro call with the same error message for the configuration 
that has not clear dependencies on other code. For those cases workarounds can be simpler than recommended usage of
separated submodule:
- use the `make` macro call without parameters when they match with defaults
- use the `makeWithoutDiscriminator` macro call without parameters when the following configuration is used: `CodecMakerConfig.withDiscriminatorFieldName(None)`
- use the `makeWithRequiredCollectionFields` macro call without parameters when the following configuration is used: `CodecMakerConfig.withRequireCollectionFields(true).withTransientEmpty(false)`
- isolate the `make` macro call in the separated object, like in [this PR](https://github.com/plokhotnyuk/play/pull/5/files)
- move jsoniter-scala imports to be local, like [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorld.scala#L6-L7)
and [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorldController.scala#L12)
- use `sbt clean compile stage` or `sbt clean test stage` instead of just `sbt clean stage`, like in
[this repo](https://github.com/hochgi/HTTP-stream-exercise/tree/jsoniter-2nd-round)

4. [Scalac can throw the following stack overflow exception](https://github.com/scala/bug/issues/11157) on `make` call 
for ADTs with objects if the derivation call and the ADT definition are enclosed in the definition of some outer class:
```
java.lang.StackOverflowError
    ...
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
	at scala.tools.nsc.transform.ExplicitOuter$OuterPathTransformer.outerPath(ExplicitOuter.scala:267)
```
Also, [internal compiler error](https://github.com/plokhotnyuk/jsoniter-scala/issues/551) can happen during compilation 
of derived codecs for ADT definitions that are nested in some classes or functions like [here](https://github.com/plokhotnyuk/jsoniter-scala/commit/db52782e6c426b73efac6c5ecaa4c28c9d128f48)

Workaround is the same for both cases: don't enclose ADT definitions into outer _classes_ or _functions_, use the outer
_object_ (not a class) instead.

5. Scala.js doesn't support Java enums compiled from Java sources, so linking fails with errors like:
```
[error] Referring to non-existent class com.github.plokhotnyuk.jsoniter_scala.macros.Level
[error]   called from private com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.$anonfun$new$24()void
[error]   called from private com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.$anonfun$new$1()void
[error]   called from constructor com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.<init>()void
[error]   called from static constructor com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.<clinit>()void
[error]   called from core module analyzer
```

The workaround is to split sources for JVM and JS and use Java enum emulation for JS.

Code for JVM:
```java
public enum Level {
    HIGH, LOW;
}
```

Code for JS:
```scala
object Level {
  val HIGH: Level = new Level("HIGH", 0)
  val LOW: Level = new Level("LOW", 1)
  
  val values: Array[Level] = Array(HIGH, LOW)

  def valueOf(name: String): Level =
    if (HIGH.name() == name) HIGH
    else if (LOW.name() == name) LOW
    else throw new IllegalArgumentException(s"Unrecognized Level name: $name")
}

final class Level private (name: String, ordinal: Int) extends Enum[Level](name, ordinal)
```

6. Scala.js can introduce 1ULP rounding error when parsing of float values with a long mantissa, see details 
[here](https://github.com/scala-js/scala-js/issues/4035).

The workaround is using `double` or `BigDecimal` types for cases when an exact precision matters.

7. Some kinds or versions of browsers can show low performance in runtime when the compiler emits ES 2015 that is 
a default option for Scala.js 1.0+.

A workaround is using the following configuration for the compiler to produce ES 5.1 output:
```
scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(false)) }
```

8. Nested option types like `Option[Option[Option[String]]]` are not supported for all values. Only `None` and 
`Some(Some(Some(x: String))))` values can be serialized and then parsed without lost of the info. `Some(None)` and 
`Some(Some(None))` values will be normalized to `None`.

A workaround could be using of a custom codec, but it cannot be injected precisely for some specified class field yet.  

## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by 
creating pull requests (fixes and improvements to docs, code, and tests are highly appreciated).

### Run tests, check coverage and binary compatibility

```sh
sbt clean coverage test coverageReport
sbt clean +test +mimaReportBinaryIssues
```

BEWARE: jsoniter-scala is included into [Scala Community Build](https://github.com/scala/community-builds)
 for 2.11.x, 2.12.x, and 2.13.x versions of Scala.
 
### Printing of code generated by macros

To see and check code generated by the `make` macro add the `-Dmacro.settings=print-codecs` option like here:
```sh
sbt -Dmacro.settings=print-codecs clean test
```

Also, to print code generated by the `eval` macro use the `-Dmacro.settings=print-expr-results` option.

Both options can be combined: `-Dmacro.settings=print-codecs,print-expr-results`

### Run JVM benchmarks

Before benchmark running check if your CPU works in `performance` mode (not a `powersave` one). On Linux use following
commands to print current and set the `performance` mode:
```sh
cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo performance | sudo tee /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
```

Sbt plugin for JMH tool is used for benchmarking, to see all their features and options please check
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](https://openjdk.java.net/projects/code-tools/jmh/

Learn how to write benchmarks in [JMH samples](https://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](https://psy-lob-saw.blogspot.com/p/jmh-related-posts.html)
 blogs.

List of available options can be printed by:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -h'
```

Results of benchmark can be stored in different formats: *.csv, *.json, etc. All supported formats can be listed by:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -lrf'
```

JMH allows running benchmarks with different profilers, to get a list of supported use (can require entering of user
password):
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -lprof'
```

Help for profiler options can be printed by following command (`<profiler_name>` should be replaced by the name of the
supported profiler from the command above):
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof <profiler_name>:help'
```

For parametrized benchmarks the constant value(s) for parameter(s) can be set by `-p` option:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -p size=1,10,100,1000 ArrayOf.*'
```

To see throughput with the allocation rate of generated codecs run benchmarks with GC profiler using the following
command:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof gc -rf json -rff jdk8.json .*Reading.*'
```

Results that are stored in JSON can be easy plotted in [JMH Visualizer](https://jmh.morethan.io/) by drugging & dropping
of your file to the drop zone or using the `source` parameter with an HTTP link to your file in the URL like
[here](https://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/oraclejdk11.json).

On Linux the perf profiler can be used to see CPU event statistics normalized per ops:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof perfnorm TwitterAPIReading.jsoniterScala'
```

Also, it can be run with a specified list of events: 
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof "perfnorm:event=cycles,instructions,ld_blocks_partial.address_alias" TwitterAPIReading.jsoniterScala'
```

List of available events for the perf profiler can be retrieved by the following command:
```sh
perf list
```

To get a result for some benchmarks with an in-flight recording file from JFR profiler use command like this:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof "jfr:dir=target/jfr-reports" -wi 10 -i 60 TwitterAPIReading.jsoniterScala'
```
You will get the profile in the `jsoniter-scala-benchmark/jvm/target/jfr-reports` directory.

To run benchmarks with recordings by [Async profiler](https://github.com/jvm-profiling-tools/async-profiler), extract
binaries to `/opt/async-profiler` directory and use command like this:
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof "async:dir=target/async-reports;output=flamegraph;libPath=/opt/async-profiler/build/libasyncProfiler.so" -wi 10 -i 60 TwitterAPIReading.jsoniterScala'
```
Now you can open direct and reverse flame graphs in the `jsoniter-scala-benchmark/jvmtarget/async-reports` directory.

To see list of available events need to start your app or benchmark, and run `jps` command. I will show list of PIDs and
names for currently running Java processes. While your Java process still running launch the Async Profiler with the
`list` option and ID of your process like here:
```sh
$ ~/Projects/com/github/jvm-profiling-tools/async-profiler/profiler.sh list 6924
Basic events:
  cpu
  alloc
  lock
  wall
  itimer
Perf events:
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
  mem:breakpoint
  trace:tracepoint
```

Following command can be used to profile and print assembly code of the hottest methods, but it requires [a setup of an 
additional library to make PrintAssembly feature enabled](https://psy-lob-saw.blogspot.com/2013/01/java-print-assembly.html):
```sh
sbt 'jsoniter-scala-benchmarkJVM/jmh:run -prof perfasm -wi 10 -i 10 -p size=128 BigIntReading.jsoniterScala'
```

More info about extras, options, and ability to generate flame graphs see in [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

Other benchmarks with results for jsoniter-scala:
- [comparison](https://github.com/sirthias/borer/pull/30) with other JSON parsers for Scala mostly on samples from real
  APIs, but with mapping to simple types only like strings and primitives and results for GraalVM EE Java8 only
- [comparison](https://github.com/dkomanov/scala-serialization/pull/8) with the best binary parsers and serializers for
  Scala
- [comparison](https://github.com/saint1991/serialization-benchmark) with different binary and text serializers for 
  Scala
- [comparison](https://github.com/tkrs/json-bench) with JSON serializers for Scala on synthetic samples
- [comparison](https://github.com/yanns/scala-json-parsers-performance) with JSON parsers for Scala when parsing from/to
  a string representation
- [comparison](https://github.com/guillaumebort/mison/pull/1) with a state-of-the-art filter that by "building
  structural indices converts control flow into data flow, thereby largely eliminating inherently unpredictable branches
  in the program and exploiting the parallelism available in modern processors"

### Run Scala.js benchmarks

Use JDK 11+ for building of `jsoniter-scala-benchmarkJS` module:
```sh
sbt -java-home /usr/lib/jvm/openjdk-17 jsoniter-scala-benchmarkJS/fullLinkJS
```

Then open the list of benchmarks in a browser:
```sh
cd jsoniter-scala-benchmark/js
open scala-2.13-fullopt.html
```

The released version of Scala.js benchmarks is available [here](https://plokhotnyuk.github.io/jsoniter-scala/scala-2.13-fullopt.html).

### Run compilation time benchmarks

Use the [circe-argonaut-compile-times](https://github.com/stephennancekivell/circe-argonaut-compile-times) project to 
compare compilation time of jsoniter-scala for deeply nested data structures with other JSON parsers like argonaut, 
play-json, and circe in 3 modes: auto, semi-auto, and derivation.

### Publish locally

Publish to local Ivy repo:
```sh
sbt clean +publishLocal
```

Publish to local Maven repo:
```sh
sbt clean +publishM2
```

### Release

For version numbering use [Recommended Versioning Scheme](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme)
that is used in the Scala ecosystem.

Double-check binary and source compatibility, including behavior, and release using the following command:
```sh
sbt release
```

Do not push changes to GitHub until promoted artifacts for the new version are not available for downloading on
[Maven Central Repository](https://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala)
to avoid binary compatibility check failures in triggered Travis CI builds.

The last step is updating of the tag info in a [release list](https://github.com/plokhotnyuk/jsoniter-scala/releases).
