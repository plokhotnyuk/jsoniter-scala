# jsoniter-scala

[![Actions Build](https://github.com/plokhotnyuk/jsoniter-scala/workflows/build/badge.svg)](https://github.com/plokhotnyuk/jsoniter-scala/actions)
[![Scala Steward](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Gitter Chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/badge/maven--central-2.42.2-blue.svg)](https://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/)

Scala macros for compile-time generation of safe and ultra-fast JSON codecs.

[**Latest results of benchmarks on JVMs**](https://plokhotnyuk.github.io/jsoniter-scala/) that compare parsing and
serialization performance of jsoniter-scala with: [borer](https://github.com/sirthias/borer), 
[circe](https://github.com/circe/circe), 
[circe with jsoniter-scala booster](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe),
[fory-json-scala](https://fory.apache.org/docs/next/json/scala),
[jackson-module-scala](https://github.com/FasterXML/jackson-module-scala),
[json4s-jackson](https://github.com/json4s/json4s/tree/master/jackson),
[json4s-native](https://github.com/json4s/json4s/tree/master/native),
[play-json](https://github.com/playframework/play-json),
[play-json with jsoniter-scala booster](https://github.com/evolution-gaming/play-json-tools/tree/master/play-json-jsoniter),
[smithy4s-json](https://github.com/disneystreaming/smithy4s/tree/main/modules/json),
[spray-json](https://github.com/spray/spray-json), [uPickle](https://github.com/lihaoyi/upickle),
[weePickle](https://github.com/rallyhealth/weePickle), [zio-blocks](https://github.com/zio/zio-blocks), 
[zio-json](https://github.com/zio/zio-json), [zio-schema-json](https://github.com/zio/zio-schema/tree/main/zio-schema-json)
libraries compiled by Scala 3.9.0 with `-opt -opt-inline:**,!java.**` options for optimization and inlining
using different JDK and GraalVM versions on Intel® Core™ Ultra 9 285K CPU @ 3.7GHz (max 5.7GHz, 200S Boost), 
RAM 64Gb DDR5-6400, Ubuntu Server 25.04 (Linux 7.0), and latest versions of JDK 17/21/25/28ea,
GraalVM Community JDK 17/21/25, and GraalVM JDK 17/21/25[*](https://docs.google.com/spreadsheets/d/1IxIvLoLlLb0bxUaRgSsaaRuXV0RUQ3I04vFqhDc2Bt8/edit?usp=sharing).

[**Latest results of benchmarks on browsers**](https://plokhotnyuk.github.io/jsoniter-scala/index-scalajs-arm.html) that
compare performance of jsoniter-scala with: [circe](https://github.com/circe/circe), 
[circe with jsoniter-scala booster](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe),
[play-json](https://github.com/playframework/play-json),
[play-json with jsoniter-scala booster](https://github.com/evolution-gaming/play-json-tools/tree/master/play-json-jsoniter),
[smithy4s-json](https://github.com/disneystreaming/smithy4s/tree/main/modules/json),
[uPickle](https://github.com/lihaoyi/upickle), [zio-blocks](https://github.com/zio/zio-blocks), 
[zio-json](https://github.com/zio/zio-json), and [zio-schema-json](https://github.com/zio/zio-schema/tree/main/zio-schema-json)
libraries compiled by Scala 3.9.0 with `-opt -opt-inline:**,!java.**` options for optimization and inlining
using Scala.js 1.21.0 to ES 2021 with GCC v20220202 optimizations applied on 
Apple M5 Pro CPU @ 3.0GHz (max 4.6GHz, 18-cores), RAM 64Gb LPDDR5X-9600, macOS (Golden Gate 27) 
and latest versions of web browsers.

## Contents

- [Acknowledgments](#acknowledgments)
- [Goals](#goals)
- [Features and limitations](#features-and-limitations)
- [**How to use**](#how-to-use)
- [Known issues](#known-issues)
- [How to develop](#how-to-develop)

## Acknowledgments

This library started from macros that reused the reader and writer of
[jsoniter (json-iterator) for Java](https://github.com/json-iterator/java), and then evolved its own core for parsing
and serialization.

The idea to generate codecs by Scala macros and its main details came from
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) (originally developed by [Alexander Nemish](https://github.com/nau)), adapted for JSON.

Other Scala macros features came from [AVSystem Commons](https://github.com/AVSystem/scala-commons/tree/master/commons-macros/src/main/scala/com/avsystem/commons/macros)
and [magnolia](https://github.com/softwaremill/magnolia).

Ideas for the most efficient parsing and serialization of `java.time.*` values came from the `java.time.OffsetDateTime`
implementation of [DSL-JSON](https://github.com/ngs-doo/dsl-json).

Projects and a blog post behind the safety and performance of parsing and serialization of numbers:
- [XJB](https://github.com/xjb714/xjb) - the most efficient and concise serialization of doubles and floats, based on
  the Schubfach algorithm and other state-of-the-art algorithms
- [rust-lexical](https://github.com/Alexhuszagh/rust-lexical) - the most efficient precise parsing of floats and doubles
- [big-math](https://github.com/eobermuhlner/big-math) - parsing of `BigInt` and `BigDecimal` values with `O(n^1.5)`
  complexity instead of `O(n^2)` of Java's implementations, where `n` is the number of digits
- [James Anhalt's algorithm](https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/) - the ingenious printing of
  integers as decimal strings

Projects and a blog post behind SWAR tricks for the JVM:
- [borer](https://github.com/sirthias/borer/blob/fde9d1ce674d151b0fee1dd0c2565020c3f6633a/core/src/main/scala/io/bullet/borer/json/JsonParser.scala#L456) - fast parsing of JSON strings by 8-byte words
- [simdjson](https://github.com/simdjson/simdjson/blob/7e1893db428936e13457ba0e9a5aac0cdfb7bc15/include/simdjson/generic/numberparsing.h#L344) - fast checking of strings for digits by 8-byte words
- [FastDoubleParser](https://github.com/wrandelshofer/FastDoubleParser/blob/0903817a765b25e654f02a5a9d4f1476c98a80c9/src/main/java/ch.randelshofer.fastdoubleparser/ch/randelshofer/fastdoubleparser/FastDoubleSimd.java#L114-L130) - fast parsing of numbers by 8-byte words
- [Johnny Lee's article](https://johnnylee-sde.github.io/Fast-time-string-to-seconds/) - fast conversion of time strings
  to seconds

Big kudos to all contributors:

[![GitHub contributors](https://contrib.rocks/image?repo=plokhotnyuk/jsoniter-scala)](https://github.com/plokhotnyuk/jsoniter-scala/graphs/contributors)

A huge thank you to my [GitHub Sponsors](https://github.com/sponsors/plokhotnyuk) for their support, 
and to JetBrains for providing [excellent tooling](https://jb.gg/OpenSource) that powers this open-source work!

## Goals

1. **Safety**: validate parsed values fail-fast with clear error reports, limit suboptimal data structures with safe
   defaults against DoS attacks, and instantiate only a _fixed_ set of classes during parsing against RCE attacks
2. **Correctness**: support the latest JSON format (RFC-8259), never replace illegally encoded characters of strings
   with placeholders, round too long JSON numbers half-even to their binary representation, and serialize floats and
   doubles to the _shortest_ text without losing precision
3. **Speed**: parse and serialize JSON directly between UTF-8 bytes and your data structures, crazily fast: without
   runtime reflection, runtime code generation, intermediate ASTs, or hash maps, and with minimal allocations and copying
4. **Productivity**: derive codecs for complex types recursively with a one-line macro at _compile-time_ to minimize
   runtime issues, print generated sources to inspect their safety and correctness or to start custom codecs from them,
   and prohibit serialization of `null` values and parsing to them in generated codecs
5. **Ergonomics**: provide the safest defaults for common usage, easily changed by compile-time and runtime
   configurations, annotations, and implicits, pretty print JSON on demand, and show hex dumps in error messages to
   speed up viewing of the error context

## Features and limitations

Features:
- Parses JSON from `String`, `Array[Byte]`, `java.nio.ByteBuffer`, and `java.io.InputStream`/`java.io.FileInputStream`
- Serializes JSON to `String`, `Array[Byte]`, `java.nio.ByteBuffer`, and `java.io.OutputStream`/`java.io.FileOutputStream`
- Parses from and serializes to a part of `Array[Byte]` or `java.nio.ByteBuffer`, set by a position and a limit
- Parses streams of JSON values and JSON arrays from `java.io.InputStream`/`java.io.FileInputStream` without holding
  all input and parsed values in memory
- Parses escaped characters in JSON keys and string values
- Derives codecs for primitives, boxed primitives, enums, tuples, `String`, `BigInt`, `BigDecimal`, `Option`,
  `Either`, `java.util.UUID`, `java.time.*` (ISO-8601 representation only), Scala collections, arrays (including
  immutable arrays in Scala 3), module classes, literal types, value classes, and case classes with fields of these types
- Derives codecs for non-case Scala classes that have getters for all arguments of the primary constructor
- Supports primary constructors with multiple parameter lists and default values, including default values in
  non-first parameter lists that depend on preceding parameters (evaluated for missing fields after parsing all fields
  of a JSON object)
- Supports map keys of primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, `java.util.UUID`,
  `java.time.*`, literal types, and value classes of them
- Supports first-order and higher-kinded types, and type aliases of all types above
- Supports 2 representations of ADTs with a sealed trait or a Scala class as a base type and non-abstract classes or
  objects as leaf classes: a discriminator field with a string value, or string values for objects and wrapper JSON
  objects for classes, with the discriminator value as the key
- Uses implicit value codecs and key codecs from the scope, so custom codecs can add types or change their JSON
  representation
- Uses implicit `Ordering[K]` instances from the scope of the `make` call for sorted maps and sets
- Reads and writes byte arrays as Base16 and Base64 strings (RFC 4648) in custom codecs
- Reads and writes arbitrary bytes or raw values in custom codecs
- Treats fields with default values of the constructor as optional and other fields as required, with no annotations
- Skips serialization of fields with default values, or with empty options, collections, or arrays, for sparse output
- Skips parsing and serialization of fields that are annotated with `@transient` or not defined in the constructor
- Overrides field names with the `@named` annotation of constructor fields
- Reads and writes numbers as strings for fields annotated with `@stringified`
- Preserves the order of instance fields when serializing with generated codecs
- Reports the hex offset of the error in parse exceptions for `Array[Byte]`, `java.nio.ByteBuffer`, and
  `java.io.InputStream`/`java.io.FileInputStream` input, with an optional hex dump of the input around the error
- Specializes key and value codecs for primitives, so they don't box or unbox values
- Parses from `java.io.InputStream` and serializes to `java.io.OutputStream` without extra buffering
- Uses only black box macros, so codec derivation never changes your types
- Orders generated code to preserve checksums and maximize hit rates of remote build caches
- Prints generated codecs with an implicit `CodecMakerConfig.PrintCodec` in the scope of codec derivation
- Suppresses all WartRemover warnings of generated codecs for Scala 2.12 and 2.13
- Implements `java.io.Serializable` in codecs and runtime configurations for distributed computing
- Supports simple opaque types like `opaque type Foo = Int` and `opaque type Bar <: String = String`
- Supports tuples with arities greater than 22 in Scala 3, backed by `scala.runtime.TupleXXL`
- Supports named tuples from Scala 3.7 while staying on Scala 3.3 (LTS)
- Supports shading to another package to lock a particular release
- Integrates with circe for faster parsing and serialization of circe AST, and for faster circe codecs
- Supports Scala 2.12, 2.13, and 3.3, JVMs of Java 11+, GraalVM native images, and Scala.js 1.0+ and Scala Native 0.5+
  for all supported Scala versions
- Has no runtime dependencies except `scala-library` (all platforms) and `scala-java-time` (a replacement of
  `java.time._` for Scala.js and Scala Native)
- Keeps patch versions backward and forward compatible, and minor versions backward compatible

Limitations:
- Requires UTF-8 for byte input and output; `String` input and output works for other encodings but is much slower
- Doesn't parse escaped characters of strings mapped to byte arrays, numbers, and `java.time.*` types
- Doesn't support cyclic graphs of class instances in generated codecs
- Rejects duplicated keys of class instances (except maps) with a parse exception
- Rejects `null` values with `NullPointerException` when serializing
- Rejects `null` values when parsing, except for optional or collection types (parsed as `None` or empty collections)
  and for fields with non-null default values
- Requires correct `equals` implementations for values used directly or in default values of constructor parameters
  (mostly non-case classes and types with custom codecs)
- Requires a time zone database for time zones other than `UTC` on Scala.js and Scala Native, see the
  [scala-java-time documentation](https://cquiroz.github.io/scala-java-time/#time-zones)

Compile-time options of `CodecMakerConfig`:
- Reads and writes numbers as strings (`isStringified`)
- Reads and writes maps as JSON arrays (`mapAsArray`)
- Skips unexpected fields, or rejects them with a parse exception (`skipUnexpectedFields`)
- Turns off skipping serialization of fields with default values (`transientDefault`), empty collections
  (`transientEmpty`), or empty options (`transientNone`)
- Skips serialization of fields with `null` values for custom codecs of Scala 3 union types with `Null` (`transientNull`)
- Requires collection fields (`requireCollectionFields`) or fields with default values (`requireDefaultFields`) in JSON
  input
- Maps names of fields (`fieldNameMapper`), ADT leaf classes (`adtLeafClassNameMapper`), and Java enum values
  (`javaEnumValueNameMapper`) to JSON, with predefined mappers to snake_case, kebab-case, camelCase, or PascalCase
- Sets an optional name of the discriminator field for ADTs (`discriminatorFieldName`)
- Serializes the discriminator field in codecs of ADT leaf classes too (`alwaysEmitDiscriminator`)
- Allows the discriminator field after other fields, which is a parse exception by default (`requireDiscriminatorFirst`)
- Encodes Scala objects of ADTs like circe does (`circeLikeObjectEncoding`)
- Parses and serializes Scala enumerations by ids (`useScalaEnumValueId`)
- Distinguishes `null` and missing fields as `Some(None)` and `None` values of `Option[Option[_]]`
  (`skipNestedOptionValues`)
- Limits precision, scale, and digits of parsed `BigDecimal` values (`bigDecimalPrecision`, `bigDecimalScaleLimit`,
  `bigDecimalDigitsLimit`)
- Limits digits of parsed `BigInt` values (`bigIntDigitsLimit`)
- Limits values of parsed bit sets (`bitSetValueLimit`)
- Limits inserts when parsing maps or sets (`mapMaxInsertNumber`, `setMaxInsertNumber`)
- Allows recursive data structures, which are a compilation error by default (`allowRecursiveTypes`)
- Turns off checking of field duplication for smaller and faster codecs (`checkFieldDuplication`)
- Turns off generation of decoding or encoding for faster derivation and smaller class/JS/binary size (`encodingOnly`,
  `decodingOnly`)
- Inlines codecs of non-value classes that have a one-argument primary constructor (`inlineOneValueClasses`)
- Supports the `scala.transient` annotation (`scalaTransientSupport`)

Runtime options of `ReaderConfig` and `WriterConfig`:
- Escapes non-ASCII chars for ASCII-only output (`escapeUnicode`)
- Indents output with the given step (`indentionStep`)
- Turns on stack traces of exceptions for debugging, which are off by default for performance
  (`throwReaderExceptionWithStackTrace`, `throwWriterExceptionWithStackTrace`)
- Turns off hex dumps in parse exceptions for performance (`appendHexDumpToParseException`), or sets their size in
  16-byte lines (`hexDumpSize`)
- Turns off the check for non-whitespace bytes after the parsed value (`checkForEndOfInput`)
- Sets max and preferred sizes of input buffers for parsing from `java.io.InputStream` or `java.nio.DirectByteBuffer`
  (`maxBufSize`, `preferredBufSize`)
- Sets max and preferred sizes of char buffers for parsing strings (`maxCharBufSize`, `preferredCharBufSize`)
- Sets the preferred size of output buffers for serializing to `java.io.OutputStream` or `java.nio.DirectByteBuffer`
  (`preferredBufSize`)

Last releases for older platforms:
- [**v2.13.5.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.13.5.2): JDK 8+, and native images
  of earlier GraalVM versions
- [**v2.13.3.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.13.3.2): Scala 2.11
- [**v2.30.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.30.2): Scala Native 0.4+

For upcoming features and fixes see [Commits](https://github.com/plokhotnyuk/jsoniter-scala/commits/master)
and [Issues page](https://github.com/plokhotnyuk/jsoniter-scala/issues).

## How to use

Let's assume that you have the following data structures:
```scala
case class Device(id: Int, model: String)

case class User(name: String, devices: Seq[Device])
```

Add the core library with a "compile" scope and the macros library with "compile-internal" or "provided" scopes to your 
list of sbt dependencies:
```sbt
libraryDependencies ++= Seq(
  // Use the %%% operator instead of %% for Scala.js and Scala Native 
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.42.2",
  // Use the "provided" scope instead when the "compile-internal" scope is not supported  
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.42.2" % "compile-internal"
)
```
At the start of a Scala CLI script, use the "dep" scope for the core library and the "compileOnly.dep" scope for the
macros library:
```scala
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.42.2"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.42.2"
```

Derive a codec for the top-level type to parse or serialize:
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

given userCodec: JsonValueCodec[User] = JsonCodecMaker.make
```

That's it! You have derived a `com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec` for the whole nested data
structure. Nested classes like `Device` need their own codecs only to parse or serialize them separately from `User`,
or with a different derivation configuration.

Now use it for parsing and serialization from/to `String`:
```scala
val user = readFromString[User]("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""")
val json = writeToString(User("John", Seq(Device(2, "iPhone X"))))
```

For the network or disks, parse and serialize bytes, which is much more efficient:
- byte arrays using `readFromArray`/`writeToArray`
- byte sub-arrays using `readFromSubArray`/`writeToSubArray`
- `java.nio.ByteBuffer` instances using `readFromByteBuffer`/`writeToByteBuffer`
- `java.io.InputStream`/`java.io.OutputStream` instances using `readFromStream`/`writeToStream`

Parsing from bytes also validates UTF-8 encoding and throws a parse exception for malformed bytes.

To print the generated code of codecs, add the following line to the scope of codec derivation before the `make` call:

```scala
given CodecMakerConfig.PrintCodec with {}
```

For more use cases, see the tests:
- [JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)
- [PackageSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/PackageSpec.scala)
- [JsonReaderSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReaderSpec.scala)
- [JsonWriterSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriterSpec.scala)

Specs of Scala 3 only features are in [this directory](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros).

```text
NOTE: Until official docs will be published, please, use all these tests as tutorials and how-tos to help in your 
journey to become happy users. Also, they are recommended to skim through for checking of your expectation before
selection of this library among others.
```

Online services that generate initial versions of your data structures from JSON samples:
- [json2caseclass](https://json2caseclass.cleverapps.io/)
- [json-to-scala-case-class](https://transform.now.sh/json-to-scala-case-class/)
- [json2classes](https://chadselph.github.io/json2classes/)
- [quicktype](https://app.quicktype.io/)

Samples of integration with web frameworks and HTTP servers:
- [akka-http](https://github.com/hseeberger/akka-http-json/blob/master/akka-http-jsoniter-scala/src/test/scala/de/heikoseeberger/akkahttpjsoniterscala/ExampleApp.scala)
- [blaze](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/blaze/src/main/scala/Main.scala)
- [colossus](https://github.com/TechEmpower/FrameworkBenchmarks/blob/b3a39dcd95b207cd2509d7bbf873a0dfb91097f5/frameworks/Scala/colossus/src/main/scala/example/Main.scala)
- [http4s](https://github.com/TechEmpower/FrameworkBenchmarks/blob/d1f960b2d4d6ea7b5c30a3ef2a8b47670f346f1c/frameworks/Scala/http4s/src/main/scala/WebServer.scala)
- [pekko-http](https://github.com/pjfanning/pekko-http-json/blob/d8ca725bb5621e46c6df0c739ef83eb9f96bb3f1/pekko-http-jsoniter-scala/src/test/scala/com/github/pjfanning/pekkohttpjsoniterscala/ExampleApp.scala)
- [Play (with Netty native transport)](https://github.com/plokhotnyuk/play/tree/master/src/main/scala/microservice)
- [youi](https://github.com/TechEmpower/FrameworkBenchmarks/blob/master/frameworks/Scala/youi/src/main/scala/example/Main.scala)
- [zio-http](https://github.com/TechEmpower/FrameworkBenchmarks/blob/master/frameworks/Scala/zio-http/src/main/scala/Main.scala)

Usages of jsoniter-scala in OSS libraries:
- [akka-http-json](https://github.com/hseeberger/akka-http-json) - integrates some of the best JSON libs in Scala with Akka HTTP 
- [bootzooka](https://github.com/softwaremill/bootzooka) - a project to quickly start developing a Scala-based microservice or web app,
  without the need to write login, user registration etc.
- [caliban](https://github.com/ghostdogpr/caliban) - a purely functional library for building GraphQL servers and 
  clients in Scala
- [cats-eo](https://github.com/constructive-programming/eo) - existential optics library for Scala3 based on cats
- [dijon](https://github.com/jvican/dijon) - support of schema-less JSON using safe and efficient AST representation
- [geo-scala](https://github.com/gnieh/geo-scala) - a core AST and utilities for GeoJSON (RFC 7946) and more
- [iron](https://github.com/Iltotore/iron) - a lightweight library for refined types in Scala 3
- [kafka-serde-scala](https://github.com/azhur/kafka-serde-scala) - implicitly converts typeclass encoders to kafka Serializer, Deserializer, Serde
- [kindlings](https://github.com/kubuszok/kindlings) - hearth fire starter - incubator/dogfooding for Hearth-based macro libraries
- [linkml-scala](https://github.com/NeverBlink-OSS/linkml-scala) - a fast, portable (JS/JVM/native), and robust implementation of LinkML for cross-language data modeling
- [logging4s](https://github.com/logging4s/logging4s) - structured logging for Scala 3
- [neotype](https://github.com/kitlangton/neotype) - a friendly newtype library for Scala 3
- [openapi-generator](https://github.com/OpenAPITools/openapi-generator) - a generator for Scala3 + sttp4 + jsoniter-scala
- [oath](https://github.com/scala-jwt/oath) - yet another scala-jwt library which has the aim to enhance user experience
- [pekko-http-json](https://github.com/pjfanning/pekko-http-json) - integrates some of the best JSON libs in Scala with Pekko HTTP 
- [play-json-jsoniter](https://github.com/evolution-gaming/play-json-tools) - provides the fastest way to convert an 
  instance of `play.api.libs.json.JsValue` to byte array (or byte buffer, or output stream) and read it back
- [scalapb-jsoniter](https://github.com/hoangmaihuy/scalapb-jsoniter) - JSON/Protobuf convertors for ScalaPB use jsoniter-scala  
- [scalatest-json](https://github.com/stephennancekivell/scalatest-json) - Scalatest matchers with appropriate equality 
  and descriptive error messages
- [smithy4s-json](https://github.com/disneystreaming/smithy4s) - JSON protocol of [Smithy](https://awslabs.github.io/smithy/)
  tooling for Scala
- [sttp](https://github.com/softwaremill/sttp) - the Scala HTTP client you always wanted! 
- [sttp-oauth2](https://github.com/polyvariant/sttp-oauth2) - OAuth2 client library implemented in Scala using sttp 
- [tapir](https://tapir.softwaremill.com/en/latest/endpoint/json.html#jsoniter-scala) - typed API descRiptions
- [zio-gcp](https://github.com/AnyMindGroup/zio-gcp) - collection of generated Google Cloud clients for ZIO with authentication
- [zio-schema-jsoniter](https://github.com/jirihausner/zio-schema-jsoniter) - jsoniter-scala codecs for zio-schema

For usages in other OSS projects, see the `Dependents` section of the [project's Scala Index page](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala)

To keep up with the latest releases, use the [sbt-updates plugin](https://github.com/rtimush/sbt-updates) or the
[Scala Steward service](https://github.com/scala-steward).

## Known issues

1. Parsing doesn't limit the total length of JSON input.

Workaround: check the length of untrusted input before `readFromStream` or other `read...` calls. Parse JSON arrays or
whitespace-separated JSON values with `scanJsonArrayFromStream` or `scanJsonValuesFromStream` instead of
`readFromStream`.

2. Compilation of a `make` call fails when its configuration depends on code that uses the result of the macro call,
because the configuration is evaluated at compile time:
```
[error] Cannot evaluate a parameter of the 'make' macro call for type 'full.name.of.YourType'. It should not depend on
        code from the same compilation module where the 'make' macro is called. Use a separated submodule of the project
        to compile all such dependencies before their usage for generation of codecs.
```
Workaround: compile such dependencies in a separate submodule. The Scala 2 compiler can fail with the same error even
when the configuration has no clear dependencies on other code. Simpler workarounds for those cases:
- use `make` or `make...` macro calls without parameters 
- isolate the `make` macro call in a separate object, like in [this PR](https://github.com/plokhotnyuk/play/pull/5/files)
- make jsoniter-scala imports local, like [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorld.scala#L6-L7)
and [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorldController.scala#L12)
- use `sbt clean compile stage` or `sbt clean test stage` instead of just `sbt clean stage`, like in
[this repo](https://github.com/hochgi/HTTP-stream-exercise/tree/jsoniter-2nd-round)
- use `mill clean` if mill's native BSP support is used in IntelliJ IDEA
- keep only one version of jsoniter-scala on the classpath: different versions (e.g. from transitive dependencies) cause
  this error. Find them with `dependencyTree`/[`dependencyGraph`](https://www.scala-sbt.org/sbt-dependency-graph) and
  remove them with [exclusionRules](https://www.scala-sbt.org/1.x/docs/Library-Management.html#Exclude+Transitive+Dependencies)

3. Compilation fails with [unexpected errors](https://github.com/plokhotnyuk/jsoniter-scala/issues/551) for ADT
definitions or their derived codecs nested in classes or functions, like
[here](https://github.com/plokhotnyuk/jsoniter-scala/commit/db52782e6c426b73efac6c5ecaa4c28c9d128f48).

Workaround: define ADTs and their codecs in an outer _object_, not in _classes_, _traits_, or _functions_.

4. Compilation of `make` calls in Scala 3 fails for some expressions of name mappers in the configuration.

Workaround: use the supported expressions from examples of `CodecMakerConfig` usage in [unit tests](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala).   

5. Nested parsing or serialization routines fail with
   [unexpected errors](https://github.com/plokhotnyuk/jsoniter-scala/issues/923), because they reuse the same
   `JsonReader` or `JsonWriter` instance:
```scala
scanJsonValuesFromStream[String](in) { s =>
  readFromString[String](s)
}
```

Workaround: use `...Reentrant` routines for all but the innermost call. They create a new `JsonReader` or `JsonWriter`
instance on each call:
```scala
scanJsonValuesFromStreamReentrant[String](in) { s =>
  readFromString[String](s)
}
```

6. Scala.js doesn't support Java enums compiled from Java sources, so linking fails with errors like:
```
[error] Referring to non-existent class com.github.plokhotnyuk.jsoniter_scala.macros.Level
[error]   called from private com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.$anonfun$new$24()void
[error]   called from private com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.$anonfun$new$1()void
[error]   called from constructor com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.<init>()void
[error]   called from static constructor com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerSpec.<clinit>()void
[error]   called from core module analyzer
```

Workaround for Scala 2: split sources for JVM and other platforms, and emulate Java enums for Scala.js and Scala Native.

Code for JVM:
```java
public enum Level {
    HIGH, LOW;
}
```

Code for Scala.js and Scala Native:
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

Workaround for Scala 3, the same for all platforms:
```scala
enum Level extends Enum[Level] {
  case HIGH
  case LOW
}
```

7. The Scala 3 compiler fails to derive anonymous codecs for generic types with concrete type parameters:
```scala
case class DeResult[T](isSucceed: Boolean, data: T, message: String)

case class RootPathFiles(files: List[String])

given JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
```
Scala 3.2.x versions fail with a duplicate definition error like this:
```
[error] 19 |      given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
[error]    |      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
[error]    |given_JsonValueCodec_DeResult is already defined as given instance given_JsonValueCodec_DeResult
```
Workaround: name the codec instances:
```scala
given codecOfDeResult1: JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
given codecOfDeResult2: JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
```
or gather private type aliases with `given` definitions in a trait:
```scala
trait DeResultCodecs:

  private type DeResult1 = DeResult[Option[String]]
  private type DeResult2 = DeResult[RootPathFiles]

  given JsonValueCodec[DeResult1] = JsonCodecMaker.make
  given JsonValueCodec[DeResult2] = JsonCodecMaker.make

end DeResultCodecs

object DeResultCodecs extends DeResultCodecs

import DeResultCodecs.given
```

8. `JsonCodecMaker.make` calls fail to derive codecs for Scala 3 union types or complex opaque types.

Workaround: define custom codecs for these types with `implicit val` before the `JsonCodecMaker.make` call, like [here](https://github.com/plokhotnyuk/jsoniter-scala/blob/7da4af1c45e11f3877708ab6d394dad9f92a3766/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodeMakerNewTypeSpec.scala#L16-L45)
and [here](https://github.com/plokhotnyuk/jsoniter-scala/blob/7da4af1c45e11f3877708ab6d394dad9f92a3766/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodeMakerNewTypeSpec.scala#L47-L137).

9. The default name mapper strips simple names of ADT leaf classes or objects up to the last dot, if they contain dots.

Workaround: use the `@named` annotation, like here:
```scala
sealed abstract class Version(val value: String)

object Version {
  @named("8.10") case object `8.10` extends Version("8.10")

  @named("8.09") case object `8.09` extends Version("8.9")
}
```

10. Parsing of JSON strings to numbers or `java.time.*` values fails for escaped ASCII characters.

Workaround: use custom codecs that parse those values as strings and then convert them, like here:
```scala
implicit val customCodecOfOffsetDateTime: JsonValueCodec[OffsetDateTime] = new JsonValueCodec[OffsetDateTime] {
  private[this] val defaultCodec: JsonValueCodec[OffsetDateTime] = JsonCodecMaker.make[OffsetDateTime]

  def nullValue: OffsetDateTime = null

  def decodeValue(in: JsonReader, default: OffsetDateTime): OffsetDateTime = {
    in.setMark()
    try {
      val x = in.readOffsetDateTime(default) // fast path for values without escaped characters
      in.resetMark()
      x
    } catch {
      case _: JsonReaderException =>
        in.rollbackToMark()
        val s = in.readString(null)
        try readFromStringReentrant("\"" + s + "\"")(defaultCodec) catch {
          case NonFatal(_) => in.decodeError("illegal offset date time")
        }
    }
  }

  def encodeValue(x: OffsetDateTime, out: JsonWriter): Unit = out.writeVal(x)
}
```

11. `implicit def` and `inline given` methods that generate custom codecs duplicate codec instances. Scala 3.5.0+ warns
with `New anonymous class definition will be duplicated at each inline site` for some `inline given` cases, and
silently duplicates codec instances in other cases.

Workaround: convert methods of codec generation to `def` and derive custom codecs explicitly, like here:
```scala
object Tags {
  opaque type Tagged[+V, +T] = Any

  type @@[+V, +T] = V & Tagged[V, T]

  def tag[T]: [V] => V => V @@ Tag = [V] => (v: V) => v
}

object Graph {
  import Tags.{@@, tag}

  def tagJsonValueCodec[V, T](codec: JsonValueCodec[V]): JsonValueCodec[V @@ T] = new JsonValueCodec[V @@ T]:
    //println("+1")
    override def decodeValue(in: JsonReader, default: V @@ T): V @@ T = tag[T](codec.decodeValue(in, default: V))
    override def encodeValue(x: V @@ T, out: JsonWriter): Unit = codec.encodeValue(x, out)
    override def nullValue: V @@ T = tag[T](codec.nullValue)

  trait NodeIdTag

  type NodeId = Int @@ NodeIdTag

  case class Node(id: NodeId, name: String)
  case class Edge(node1: NodeId, node2: NodeId)

  given JsonValueCodec[Graph.NodeId] = Graph.tagJsonValueCodec(JsonCodecMaker.make)
  given JsonValueCodec[Graph.Node] = JsonCodecMaker.make
  given JsonValueCodec[Graph.Edge] = JsonCodecMaker.make
}
```

## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, 
or contribute by creating pull requests (improvements to docs, code, and tests are highly appreciated).

The `gh-pages` branch has about 10Gb of historical benchmark results, so clone with the `--single-branch` option to
fetch sources only.

If developing on a fork, make sure to download the git tags (required by the sbt build):
```sh
git remote add upstream git@github.com:plokhotnyuk/jsoniter-scala.git
git fetch --tags upstream
```

Building of Scala.js and Scala Native modules requires Clang 18.x and Node.js 16.x. These commands work for me:
```sh
sudo apt install clang libstdc++-12-dev libgc-dev 
curl https://raw.githubusercontent.com/creationix/nvm/master/install.sh | bash 
source ~/.bashrc
nvm install 16
node -v
```

### Get report of available dependency updates

```sh
sbt ";dependencyUpdates; reload plugins; dependencyUpdates; reload return"
```

### Run tests, check coverage and binary compatibility

```sh
sbt -java-home /usr/lib/jvm/jdk-17 ++2.13.18 clean coverage jsoniter-scala-coreJVM/test jsoniter-scala-circeJVM/test jsoniter-scala-macrosJVM/test jsoniter-scala-benchmarkJVM/test coverageReport
sbt -java-home /usr/lib/jvm/jdk-11 clean +test +mimaReportBinaryIssues
```

BEWARE: jsoniter-scala is included in [Scala Community Build](https://github.com/scala/community-builds)
 for Scala 2 and [Scala Open Community Build](https://scala3.westeurope.cloudapp.azure.com/job/runBuild/) for Scala 3.

### Run JVM benchmarks

Before running benchmarks, check that your CPU is in the `performance` mode, not `powersave`. On Linux, print the
current mode and set the `performance` mode with:
```sh
cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
for i in $(ls /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor); do echo performance | sudo tee $i; done
```
Then view your CPU frequency with:
```sh
cat /proc/cpuinfo | grep -i mhz
```

Stop unneeded applications and services. Print running services with:
```sh
sudo service --status-all | grep '\[ + \]'
sudo systemctl list-units --state running
```

Then clear the cache memory. On Linux, without a reboot:
```sh
sudo su
free -m -h && sync && echo 3 > /proc/sys/vm/drop_caches && free -m -h
```

Benchmarks use the sbt plugin for JMH. For their features and options, see
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](https://openjdk.java.net/projects/code-tools/jmh/)

Learn how to write benchmarks in [JMH samples](https://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](https://psy-lob-saw.blogspot.com/p/jmh-related-posts.html)
 blogs.

Print available options with:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -h'
```

Benchmark results can be stored in different formats: *.csv, *.json, etc. List supported formats with:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -lrf'
```

JMH runs benchmarks with different profilers. List supported profilers with (it can ask for the user password):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -lprof'
```

Print help for profiler options with (replace `<profiler_name>` with a profiler from the command above):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof <profiler_name>:help'
```

Set constant values of benchmark parameters with the `-p` option:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -p size=1,10,100,1000 ArrayOf.*'
```

To see throughput with the allocation rate of generated codecs, run benchmarks with the GC profiler:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof gc .*Reading.*'
```

Plot results stored in JSON with [JMH Visualizer](https://jmh.morethan.io/): drag and drop your file to the drop zone,
or pass an HTTP link to your file in the `source` parameter of the URL, like
[here](https://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/oraclejdk11.json).

On Linux, see CPU event statistics normalized per operation with the perf profiler:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof perfnorm TwitterAPIReading.jsoniterScala'
```

Run it with a list of events, e.g. to check CPU stalls with 16 threads: 
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -t 16 -prof "perfnorm:event=cycles,instructions,uops_executed.core,uops_executed.stall_cycles,cache-references,cache-misses,cycle_activity.stalls_total,cycle_activity.stalls_mem_any,cycle_activity.stalls_l3_miss,cycle_activity.stalls_l2_miss,cycle_activity.stalls_l1d_miss" .*'
```

List available events of the perf profiler with:
```sh
perf list
```

To record benchmarks with the JFR profiler, use a command like this:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof "jfr:dir=target/jfr-reports" -wi 10 -i 60 TwitterAPIReading.jsoniterScala'
```
Recordings are in the `jsoniter-scala-benchmark/jvm/target/jfr-reports` directory.

To record benchmarks with [Async profiler](https://github.com/jvm-profiling-tools/async-profiler), extract its binaries
to the `/opt/async-profiler` directory and set these runtime variables to capture kernel frames:
```sh
sudo sysctl kernel.perf_event_paranoid=1
sudo sysctl kernel.kptr_restrict=0
```

Then use a command like this:
```sh
sbt -java-home /usr/lib/jvm/jdk-21 jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof "async:dir=target/async-reports;interval=1000000;output=flamegraph;libPath=/opt/async-profiler/lib/libasyncProfiler.so" -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" --p size=128 -wi 5 -i 10 jsoniterScala'
```
Direct and reverse flame graphs are in the `jsoniter-scala-benchmark/jvm/target/async-reports` directory.

BEWARE: `-XX:+DebugNonSafepoints` can corrupt reports due to [a bug that is fixed only for JDK 21](https://bugs.openjdk.org/browse/JDK-8201516).  

To list available events, start your app or benchmark, and run `jps` to print PIDs and names of running Java
processes. While your process is running, launch Async Profiler with the `list` option and its PID:
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

Profile and print assembly code of the hottest methods with the following command. It requires
[the `hsdis` library for the PrintAssembly feature](https://builds.shipilev.net/hsdis/):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/Jmh/run -prof perfasm -wi 10 -i 10 -p size=128 BigIntReading.jsoniterScala'
```

For more extras, options, and flame graphs, see [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh).

Other benchmarks that compare jsoniter-scala with:
- [kostya/benchmarks](https://github.com/kostya/benchmarks/tree/master#json) - JSON parsers for different programming
  languages and compilers
- [simdjson-java#31](https://github.com/simdjson/simdjson-java/pull/31) - the most performant Java parsers: simdjson-java,
  fastjson, jackson, etc.
- [borer#30](https://github.com/sirthias/borer/pull/30) - JSON parsers for Scala on samples from real APIs, mapped to
  simple types like strings and primitives only, with results for GraalVM EE Java 8 only
- [scala-serialization#8](https://github.com/dkomanov/scala-serialization/pull/8) - the best binary parsers and
  serializers for Scala
- [serialization-benchmark](https://github.com/saint1991/serialization-benchmark) - binary and text serializers for
  Scala
- [json-bench](https://github.com/tkrs/json-bench) - JSON serializers for Scala on synthetic samples
- [scala-json-parsers-performance](https://github.com/yanns/scala-json-parsers-performance) - JSON parsers for Scala,
  parsing from and serializing to strings
- [mison#1](https://github.com/guillaumebort/mison/pull/1) - a state-of-the-art filter that by "building
  structural indices converts control flow into data flow, thereby largely eliminating inherently unpredictable branches
  in the program and exploiting the parallelism available in modern processors"

### Run Scala.js benchmarks

Build the `jsoniter-scala-benchmarkJS` module with JS only packaging, using JDK 17+:
```sh
sbt -DassemblyJSBenchmarks -java-home /usr/lib/jvm/jdk-17 ++3.9.0 jsoniter-scala-benchmarkJS/fullOptJS
```

Then deploy it to GitHub pages, open it in a browser, and select the batch mode that stores results in a `.zip` file.

Merge unpacked results from browsers with: `jq -s '[.[][]]' firefox/*.json >firefox.json`

Use similar steps for WASM + JS (dependencies) packaging:
```sh
sbt -DassemblyWASMBenchmarks -java-home /usr/lib/jvm/jdk-17 ++3.9.0 jsoniter-scala-benchmarkJS/fullLinkJS
```

Then use [WASM optimizer](https://github.com/WebAssembly/binaryen):
```
brew upgrade binaryen
wasm-opt --all-features --disable-custom-descriptors --disable-compact-imports -O3 main.wasm -o main-o3.wasm
```

Then deploy it to GitHub pages, open it in a browser, and select the batch mode that stores results in a `.zip` file.

Merge unpacked results from browsers with: `jq -s '[.[][]]' firefox/*.json >firefox-wasm.json`

Released versions of Scala.js benchmarks: 
- [JS only](https://plokhotnyuk.github.io/jsoniter-scala/scala-3-fullopt.html)
- [WASM + JS (dependencies)](https://plokhotnyuk.github.io/jsoniter-scala/scala-3-fulllink.html)  

### Run compilation time benchmarks

Use the [circe-argonaut-compile-times](https://github.com/stephennancekivell/circe-argonaut-compile-times) project to 
compare compilation time of jsoniter-scala for deeply nested product types with other JSON parsers like argonaut, 
play-json, and circe in 3 modes: auto, semi-auto, and derivation.

For Scala 3 use the [scala3-compile-tests](https://github.com/pme123/scala3-compile-tests) project to compare
compilation time of jsoniter-scala for Scala 3 enumerations (sum types) with circe in semi-auto mode.

Also see [an amazing talk from Mateusz Kubuszok](https://www.youtube.com/watch?v=scWvlO_fb78), co-author of
[Chimney](https://github.com/scalalandio/chimney), about approaches to type class derivation, and how a trick from
jsoniter-scala greatly speeds up sane auto-derivation, which you can reuse for your type classes too. 

### Publish locally

Use publishing of SNAPSHOT versions to your local artifact repositories for testing with other libraries or applications. 

Publish to the local Ivy repo:
```sh
sbt clean +publishLocal
```

Publish to the local Maven repo:
```sh
sbt clean +publishM2
```

### Release

For version numbering use [Recommended Versioning Scheme](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme)
that is used in the Scala ecosystem.

Double-check binary and source compatibility, including behavior, and release using the following command on the
environment with 16+GB of RAM:
```sh
sbt -java-home /usr/lib/jvm/jdk-11 -J-Xmx16g clean release
```

Do not push changes to GitHub until promoted artifacts for the new version are not available for downloading on
[Maven Central Repository](https://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala)
to avoid binary compatibility check failures in triggered Travis CI builds.

The last step is updating of the tag info in a [release list](https://github.com/plokhotnyuk/jsoniter-scala/releases).
