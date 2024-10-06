# jsoniter-scala
 
[![Actions Build](https://github.com/plokhotnyuk/jsoniter-scala/workflows/build/badge.svg)](https://github.com/plokhotnyuk/jsoniter-scala/actions)
[![Scala Steward](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Gitter Chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/badge/maven--central-2.30.15-blue.svg)](https://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/)

Scala macros for compile-time generation of safe and ultra-fast JSON codecs.

![image](https://user-images.githubusercontent.com/890289/213166187-b63bbd17-baf0-435a-9c02-4b9737079323.png)

[**Latest results of benchmarks on JVMs**](https://plokhotnyuk.github.io/jsoniter-scala/) that compare parsing and
serialization performance of jsoniter-scala with: [borer](https://github.com/sirthias/borer), 
[circe](https://github.com/circe/circe), 
[circe with jsoniter-scala booster](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe),
[jackson-module-scala](https://github.com/FasterXML/jackson-module-scala),
[json4s-jackson](https://github.com/json4s/json4s/tree/master/jackson),
[json4s-native](https://github.com/json4s/json4s/tree/master/native),
[play-json](https://github.com/playframework/play-json),
[play-json with jsoniter-scala booster](https://github.com/evolution-gaming/play-json-tools/tree/master/play-json-jsoniter),
[smithy4s-json](https://github.com/disneystreaming/smithy4s/tree/main/modules/json),
[spray-json](https://github.com/spray/spray-json), [uPickle](https://github.com/lihaoyi/upickle),
[weePickle](https://github.com/rallyhealth/weePickle), [zio-json](https://github.com/zio/zio-json)
libraries using different JDK and GraalVM versions on the following environment: Intel® Core™ i9-13900K CPU @ 3.0GHz
(max 5.8GHz, performance-cores only), RAM 64Gb DDR5-4800, Ubuntu 24.04 (Linux 6.8), and latest versions of JDK 17/21/24-ea,
GraalVM Community JDK 17/21/24-ea, and GraalVM JDK 17/21/24-ea[*](https://docs.google.com/spreadsheets/d/1IxIvLoLlLb0bxUaRgSsaaRuXV0RUQ3I04vFqhDc2Bt8/edit?usp=sharing).

[**Latest results of benchmarks on browsers**](https://plokhotnyuk.github.io/jsoniter-scala/index-scalajs.html) that
compare performance of jsoniter-scala with: [circe](https://github.com/circe/circe), 
[circe with jsoniter-scala booster](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe),
[play-json](https://github.com/playframework/play-json),
[play-json with jsoniter-scala booster](https://github.com/evolution-gaming/play-json-tools/tree/master/play-json-jsoniter),
[smithy4s-json](https://github.com/disneystreaming/smithy4s/tree/main/modules/json),
[uPickle](https://github.com/lihaoyi/upickle), [zio-json](https://github.com/zio/zio-json)
compiled by Scala.js 1.17.0 to ES 2015 with GCC v20220202 optimizations applied on
Intel® Core™ i7-11800H CPU @ 2.3GHz (max 4.6GHz), RAM 64Gb DDR4-3200, Ubuntu 23.10 (Linux 6.6).

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
[Kryo Macros](https://github.com/evolution-gaming/kryo-macros) (originally developed by [Alexander Nemish](https://github.com/nau)) and adapted for the needs of the JSON domain.
  
Other Scala macros features were peeped in [AVSystem Commons](https://github.com/AVSystem/scala-commons/tree/master/commons-macros/src/main/scala/com/avsystem/commons/macros)
and [magnolia](https://github.com/softwaremill/magnolia) libraries.

Ideas for the most efficient parsing and serialization of `java.time.*` values were inspired by
[DSL-JSON](https://github.com/ngs-doo/dsl-json)'s implementation for `java.time.OffsetDateTime`.

Other projects and a blog post that have helped deliver unparalleled safety and performance characteristics for parsing
and serialization of numbers:
- [Schubfach](https://github.com/c4f7fcce9cb06515/Schubfach/) - the most efficient and concise way to serialize doubles 
  and floats to the textual representation
- [rust-lexical](https://github.com/Alexhuszagh/rust-lexical) - the most efficient way to parse floats and doubles from
  the textual representation precisely
- [big-math](https://github.com/eobermuhlner/big-math) - parsing of `BigInt` and `BigDecimal` values with the `O(n^1.5)`
  complexity instead of `O(n^2)` using Java's implementations where `n` is a number of digits
- [James Anhalt's algorithm](https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/) - the ingenious algorithm for 
  printing integers into decimal strings
  
A bunch of SWAR technique tricks for JVM platform are based on following projects and a blog post:
- [borer](https://github.com/sirthias/borer/blob/fde9d1ce674d151b0fee1dd0c2565020c3f6633a/core/src/main/scala/io/bullet/borer/json/JsonParser.scala#L456) - the fast parsing of JSON strings by 8-byte words
- [simdjson](https://github.com/simdjson/simdjson/blob/7e1893db428936e13457ba0e9a5aac0cdfb7bc15/include/simdjson/generic/numberparsing.h#L344) - the fast checking of string for digits by 8-byte words
- [FastDoubleParser](https://github.com/wrandelshofer/FastDoubleParser/blob/0903817a765b25e654f02a5a9d4f1476c98a80c9/src/main/java/ch.randelshofer.fastdoubleparser/ch/randelshofer/fastdoubleparser/FastDoubleSimd.java#L114-L130) - the fast parsing of numbers by 8-byte words
- [Johnny Lee's article](https://johnnylee-sde.github.io/Fast-time-string-to-seconds/) - the fast time string to seconds
  conversion

Big kudos to all contributors:

[![GitHub contributors](https://contrib.rocks/image?repo=plokhotnyuk/jsoniter-scala)](https://github.com/plokhotnyuk/jsoniter-scala/graphs/contributors)


## Goals

1. **Safety**: validate parsed values safely with the fail-fast approach and clear reporting, provide configurable 
limits for suboptimal data structures with safe defaults to be resilient for DoS attacks, generate codecs that create
instances of a _fixed_ set of classes during parsing to avoid RCE attacks
2. **Correctness**: support the latest JSON format (RFC-8259), do not replace illegally encoded characters of string 
values by placeholder characters, parse numbers with limited binary representation doing half even rounding for too 
long JSON numbers, serialize floats and doubles to the _shortest_ textual representation without loosing of precision 
3. **Speed**: do parsing and serialization of JSON directly from UTF-8 bytes to your data structures and back, do it 
crazily fast without using of runtime reflection or runtime code generation, intermediate ASTs, hash maps, but with 
minimum allocations and copying
4. **Productivity**: derive codecs recursively for complex types using one line macro, do it in _compile-time_ to 
minimize the probability of run-time issues, optionally print generated sources as compiler output to be inspected for 
proving safety and correctness or to be reused as a starting point for the implementation of custom codecs, prohibit 
serializing of `null` Scala values and parsing immediately to them in generated codecs
5. **Ergonomics**: have preconfigured defaults for the safest and common usage that can be easily altered by compile- 
and run-time configuration instances, combined with compile-time annotations and implicits, embrace the textual 
representation of JSON providing a pretty printing option, provide a hex dump in the error message to speed up the
view of an error context

## Features and limitations

- JSON parsing from `String`, `Array[Byte]`, `java.nio.ByteBuffer`, `java.io.InputStream`/`java.io.FileInputStream`
- JSON serialization to `String`, `Array[Byte]`, `java.nio.ByteBuffer`, `java.io.OutputStream`/`java.io.FileOutputStream`
- Support of parsing from or writing to part of `Array[Byte]` or `java.nio.ByteBuffer` by specifying of position and 
  limit
- Parsing of streaming JSON values and JSON arrays from `java.io.InputStream`/`java.io.FileInputStream` without the need
  of holding all input and parsed values in the memory
- Only UTF-8 encoding is supported when working with buffered bytes directly but there is a fallback to parse and
  serialize JSON from/to `String` (while this is much less efficient)
- Parsing of strings with escaped characters for JSON keys and string values
- Codecs can be generated for primitives, boxed primitives, enums, tuples, `String`, `BigInt`, `BigDecimal`, `Option`,
  `Either`, `java.util.UUID`, `java.time.*` (to/from ISO-8601 representation only), Scala collections, arrays
  (including immutable arrays in Scala 3), module classes, literal types, value classes, and case classes with
  values/fields having any of types listed here
- Classes should be defined with a primary constructor that hasn't defined default values in non-first parameter lists
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
- Parsing of `null` values allowed only for optional or collection types (that means the `None` value or an empty 
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
- Parsing exception always reports a hexadecimal offset of `Array[Byte]`, `java.nio.ByteBuffer`, `java.io.InputStream`/
  `java.io.FileInputStream` where it occurs, and an optional hex dump affected by error part of an internal byte buffer
- Configurable by field annotation ability to read/write numeric fields from/to string values
- Both key and value codecs are specialized to work with primitives efficiently without boxing/unboxing
- No extra buffering is required when parsing from `java.io.InputStream`/`java.io.FileInputStream` or serializing to
  `java.io.OutputStream`/`java.io.FileOuputStream`
- Using black box macros only for codec generation ensures that your types will never be changed
- Ability to print generated code for codecs using an implicit val of `CodecMakerConfig.PrintCodec` type in a scope of 
  codec derivation
- No dependencies on extra libraries in _runtime_ excluding Scala's `scala-library` (all platforms) and
  `scala-java-time` (replacement of JDKs `java.time._` types for Scala.js and Scala Native)
- On Scala.js and Scala Native platforms, if you need support for timezones besides `UTC` then you should follow the 
  [scala-java-time documentation](https://cquiroz.github.io/scala-java-time/#time-zones) for adding a time zone database
  to your application
- Codecs and runtime configurations implement `java.io.Serializable` for easier usage in distributive computing
- Support of shading to another package for locking on a particular released version
- Patch versions are backward and forward compatible, minor versions are backward compatible
- Integration with circe for faster parsing/serialization and decoding/encoding to/from circe AST 
- Releases for different Scala versions: 2.12, 2.13, and 3.3
- Support of JVMs for Java 11+ versions
- Support of compilation to a native image by GraalVM
- Support of Scala.js 1.0+ for all supported Scala versions
- Support of Scala Native 0.5+ for all supported Scala versions
- Suppressing of all WartRemover warnings of generated codecs for Scala 2.12 and 2.13 

There are configurable options that can be set in compile-time:
- Ability to read/write numbers from/to string values
- Ability to read/write maps as JSON arrays
- Skipping of unexpected fields or throwing of parse exceptions
- Skipping of serialization of fields that have empty collection values can be turned off to force serialization of them
- Skipping of serialization of fields that have empty optional values can be turned off to force serialization of them
- Skipping of serialization of fields which values are matched with defaults that are defined in the primary constructor
  can be turned off to force serialization of that values
- Ability to require collection fields in the JSON input
- Ability to require fields with default values in the JSON input
- Ability to override names of classes of ADTs and fields using a compile-time annotation  
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
- Ability to derive codecs that can distinguish `null` field values and missing fields as `Some(None)` and `None` values
  of `Option[Option[_]]`
- Ability to turn on circe-like encoding of Scala objects in ADTs
- Ability to disable generation of implementation for decoding or encoding
- Ability to require fields that have defined default values
- Ability to generate smaller and more efficient codecs for classes when checking of field duplication is not needed
- Ability to inline non value classes which have the primary constructor with just one argument 

List of options that change parsing and serialization in runtime:
- Serialization of strings with escaped Unicode characters to be ASCII compatible
- Indenting of output and its step
- Throwing of stack-less parsing exceptions by default to greatly reduce the impact on performance, while stack traces
  can be turned on in development for debugging
- Turning off hex dumping affected by error part of an internal byte buffer to reduce the impact on performance
- Size of the hex dump can be adjusted for bigger or smaller number of 16-byte lines
- Max size of internal input buffers when parsing from `java.io.InputStream` or `java.nio.DirectByteBuffer`
- Preferred size of internal input buffers when parsing from `java.io.InputStream` or `java.nio.DirectByteBuffer`
- Preferred size of internal output buffers when serializing to `java.io.OutputStream` or `java.nio.DirectByteBuffer`
- Max size of char buffers when parsing string values
- Preferred size of char buffers when parsing string values

The [**v2.13.5.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.13.5.2) release is the last version that
supports JDK 8+ and native image compilation with earlier versions of GraalVM.

The [**v2.13.3.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.13.3.2) release is the last version that
supports Scala 2.11.

The [**v2.30.2**](https://github.com/plokhotnyuk/jsoniter-scala/releases/tag/v2.30.2) release is the last version that
supports Scala Native 0.4+.


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
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.30.15",
  // Use the "provided" scope instead when the "compile-internal" scope is not supported  
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.30.15" % "compile-internal"
)
```
In the beginning of Scala CLI script use "dep" scope for the core library or "compileOnly.dep" scope for the macros
libary:
```scala
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.30.15"
//> using compileOnly.dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.30.15"
```

Derive a codec for the top-level type that need to be parsed or serialized:
```scala
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

given userCodec: JsonValueCodec[User] = JsonCodecMaker.make
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec` for the
whole nested data structure. No need to derive intermediate codecs for inner nested classes like `Device` if you are not
going to parse/serialize them from/to JSON in isolation (not as a part of `User`) and use the default or the same 
derivation configuration for their codecs.

Now use it for parsing and serialization from/to `String`:
```scala
val user = readFromString[User]("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""")
val json = writeToString(User("John", Seq(Device(2, "iPhone X"))))
```

When your input comes from the network or disks much more efficient ways are to parse and serialize from/to:
- byte arrays using `readFromArray`/`writeToArray`
- byte sub-arrays using `readFromSubArray`/`writeToSubArray`
- `java.nio.ByteBuffer` instances using `readFromByteBuffer`/`writeToByteBuffer`
- `java.io.InputStream`/`java.io.OutputStream` instances using `readFromStream`/`writeToStream`

Also, parsing from bytes will check `UTF-8` encoding and throw an error in case of malformed bytes.

To print generated code for codecs add the following line to the scope of the codec derivation before `make` call.

```scala
given CodecMakerConfig.PrintCodec with {}
```

For more use cases of jsoniter-scala, please, check out tests:
- [JsonCodecMakerSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala)
- [PackageSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/PackageSpec.scala)
- [JsonReaderSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonReaderSpec.scala)
- [JsonWriterSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-core/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/core/JsonWriterSpec.scala)

All Scala 3 only features are tested by specs in [this directory](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros).

```text
NOTE: Until official docs will be published, please, use all these tests as tutorials and how-tos to help in your 
journey to become happy users. Also, they are recommended to skim through for checking of your expectation before
selection of this library among others.
```

You can use the following on-line services to generate an initial version of your data structures from JSON
samples:
- [json2caseclass](https://json2caseclass.cleverapps.io/)
- [json-to-scala-case-class](https://transform.now.sh/json-to-scala-case-class/)
- [json2classes](https://chadselph.github.io/json2classes/)
- [quicktype](https://app.quicktype.io/)

Also, if you have JSON Schema the following on-line service can generate corresponding data structures for you:
- [json-schema-to-case-class](https://cchandurkar.github.io/json-schema-to-case-class/)
- [quicktype](https://app.quicktype.io/)

And the following library can generate JSON Schema for your existing data structures:
- [scala-jsonschema](https://github.com/andyglow/scala-jsonschema)

Samples for its integration with different web frameworks and HTTP servers:
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
- [caliban](https://github.com/ghostdogpr/caliban) - a purely functional library for building GraphQL servers and 
  clients in Scala
- [dijon](https://github.com/jvican/dijon) - support of schema-less JSON using safe and efficient AST representation
- [geo-scala](https://github.com/gnieh/geo-scala) - a core AST and utilities for GeoJSON (RFC 7946) and more
- [iron](https://github.com/Iltotore/iron) - a lightweight library for refined types in Scala 3
- [jsoniter-scala-circe](https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe) - the circe 
  booster for faster parsing/serialization to/form circe AST and decoding/encoding of `java.time._` and `BigInt` types
- [kafka-serde-scala](https://github.com/azhur/kafka-serde-scala) - implicitly converts typeclass encoders to kafka Serializer, Deserializer, Serde
- [logging4s](https://github.com/logging4s/logging4s) - structured logging for Scala 3
- [neotype](https://github.com/kitlangton/neotype) - a friendly newtype library for Scala 3
- [oath](https://github.com/scala-jwt/oath) - yet another scala-jwt library which has the aim to enhance user experience
- [pekko-http-json](https://github.com/pjfanning/pekko-http-json) - integrates some of the best JSON libs in Scala with Pekko HTTP 
- [play-json-jsoniter](https://github.com/evolution-gaming/play-json-tools) - provides the fastest way to convert an 
  instance of `play.api.libs.json.JsValue` to byte array (or byte buffer, or output stream) and read it back
- [scalatest-json](https://github.com/stephennancekivell/scalatest-json) - Scalatest matchers with appropriate equality 
  and descriptive error messages
- [smithy4s-json](https://github.com/disneystreaming/smithy4s) - JSON protocol of [Smithy](https://awslabs.github.io/smithy/)
  tooling for Scala
- [sttp](https://github.com/softwaremill/sttp) - the Scala HTTP client you always wanted! 
- [sttp-oauth2](https://github.com/polyvariant/sttp-oauth2) - OAuth2 client library implemented in Scala using sttp 
- [tapir](https://tapir.softwaremill.com/en/latest/endpoint/json.html#jsoniter-scala) - Typed API descRiptions

Also, for usages in other OSS projects see the `Dependents` section of [peoject's Scala Index page](https://index.scala-lang.org/plokhotnyuk/jsoniter-scala)

For all dependent projects it is recommended to use [sbt-updates plugin](https://github.com/rtimush/sbt-updates) or
[Scala steward service](https://github.com/scala-steward) to keep up with using of the latest releases.

## Known issues

1. There is no validation for the length of JSON representation during parsing.

If your system can accept too long untrusted input then check the input length before parsing with `readFromStream`
or other `read...` calls.

Also, if you have an input that is an array of values or white-space separate values then consider parsing it by
`scanJsonArrayFromInputStream` or `scanJsonValuesFromInputStream` instead of `readFromStream`.

2. The configuration parameter for the `make` macro is evaluated in compile-time. It requires no dependency
on other code that uses a result of the macro's call, otherwise the following compilation error will be reported:
```
[error] Cannot evaluate a parameter of the 'make' macro call for type 'full.name.of.YourType'. It should not depend on
        code from the same compilation module where the 'make' macro is called. Use a separated submodule of the project
        to compile all such dependencies before their usage for generation of codecs.
```
Sometime Scala 2 compiler can fail to compile the `make` macro call with the same error message for the configuration 
that has not clear dependencies on other code. For those cases workarounds can be simpler than recommended usage of
separated submodule:
- use `make` or `make...` macro calls without parameters 
- isolate the `make` macro call in the separated object, like in [this PR](https://github.com/plokhotnyuk/play/pull/5/files)
- move jsoniter-scala imports to be local, like [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorld.scala#L6-L7)
and [here](https://github.com/plokhotnyuk/play/blob/master/src/main/scala/microservice/HelloWorldController.scala#L12)
- use `sbt clean compile stage` or `sbt clean test stage` instead of just `sbt clean stage`, like in
[this repo](https://github.com/hochgi/HTTP-stream-exercise/tree/jsoniter-2nd-round)
- use `mill clean` if mill's native BSP support is used in IntelliJ IDEA

3. [Unexpected compiler errors](https://github.com/plokhotnyuk/jsoniter-scala/issues/551)
can happen during compilation of ADT definitions or their derived codecs if they are nested in some classes or functions
like [here](https://github.com/plokhotnyuk/jsoniter-scala/commit/db52782e6c426b73efac6c5ecaa4c28c9d128f48).

The workaround is the same for both cases: don't enclose ADT definitions into outer _classes_, _traits_ or _functions_,
use the outer _object_ (not a class) instead.

4. Compile-time configuration for `make` calls in Scala 3 has limited support of possible expressions for name mapping.

Please use examples of `CodecMakerConfig` usage from [unit tests](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/jsoniter-scala-macros/shared/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMakerSpec.scala).   

5. [Unexpected parsing or serialization errors](https://github.com/plokhotnyuk/jsoniter-scala/issues/923)
   can happen for nested parsing or serialization routines when the same instance of `JsonReader` or `JsonWriter` is
   reused:
```scala
scanJsonValuesFromStream[String](in) { s =>
  readFromString[String](s)
}
```

The workaround is using reentrant parsing or serialization routines for all except the most nested call. That will
create a new instance of `JsonReader` or `JsonWriter` on each reentrant call:
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

The workaround for Scala 2 is to split sources for JVM and other platforms and use Java enum emulation for Scala.js and
Scala Native.

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

For Scala 3 the workaround can be the same for all platforms:
```scala
enum Level extends Enum[Level] {
  case HIGH
  case LOW
}
```

7. Scala 3 compiler cannot derive anonymous codecs for generic types with concrete type parameters:
```scala
case class DeResult[T](isSucceed: Boolean, data: T, message: String)

case class RootPathFiles(files: List[String])

given JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
```
Current 3.2.x versions of scalac fail with the duplicating definition error like this:
```
[error] 19 |      given JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
[error]    |      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
[error]    |given_JsonValueCodec_DeResult is already defined as given instance given_JsonValueCodec_DeResult
```
The workaround is using named instances of codecs:
```scala
given codecOfDeResult1: JsonValueCodec[DeResult[Option[String]]] = JsonCodecMaker.make
given codecOfDeResult2: JsonValueCodec[DeResult[RootPathFiles]] = JsonCodecMaker.make
```
or private type aliases with `given` definitions gathered in some trait:
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

8. Currently, the `JsonCodecMaker.make` call cannot derive codecs for Scala 3 opaque and union types.
The workaround is using a custom codec for these types defined with `implicit val` before the `JsonCodecMaker.make`
call, like [here](https://github.com/plokhotnyuk/jsoniter-scala/blob/7da4af1c45e11f3877708ab6d394dad9f92a3766/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodeMakerNewTypeSpec.scala#L16-L45)
and [here](https://github.com/plokhotnyuk/jsoniter-scala/blob/7da4af1c45e11f3877708ab6d394dad9f92a3766/jsoniter-scala-macros/shared/src/test/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodeMakerNewTypeSpec.scala#L47-L137).

9. If ADT leaf classes/object contains dots in their simple names the default name mapper will strip names up to the
last dot character.
The workaround is to use `@named` annotation like here:
```scala
sealed abstract class Version(val value: String)

object Version {
  @named("8.10") case object `8.10` extends Version("8.10")

  @named("8.09") case object `8.09` extends Version("8.9")
}
```

10. When parsing JSON strings to numeric or `java.time.*` values escaped encoding of ASCII characters is not supported.
The workaround is to use custom codecs which parse those values as strings and then convert them to corresponding types,
like here:
```scala
implicit val customCodecOfOffsetDateTime: JsonValueCodec[OffsetDateTime] = new JsonValueCodec[OffsetDateTime] {
  private[this] val defaultCodec: JsonValueCodec[OffsetDateTime] = JsonCodecMaker.make[OffsetDateTime]
  private[this] val maxLen = 44 // should be enough for the longest offset date time value
  private[this] val pool = new ThreadLocal[Array[Byte]] {
    override def initialValue(): Array[Byte] = new Array[Byte](maxLen + 2)
  }

  def nullValue: OffsetDateTime = null

  def decodeValue(in: JsonReader, default: OffsetDateTime): OffsetDateTime = {
    val buf = pool.get
    val s = in.readString(null)
    val len = s.length
    if (len <= maxLen && {
      buf(0) = '"'
      var bits, i = 0
      while (i < len) {
        val ch = s.charAt(i)
        buf(i + 1) = ch.toByte
        bits |= ch
        i += 1
      }
      buf(i + 1) = '"'
      bits < 0x80
    }) {
      try {
        return readFromSubArrayReentrant(buf, 0, len + 2, ReaderConfig)(defaultCodec)
      } catch {
        case NonFatal(_) => ()
      }
    }
    in.decodeError("illegal offset date time")
  }

  def encodeValue(x: OffsetDateTime, out: JsonWriter): Unit = out.writeVal(x)
}
```

11. Do not use `implicit def` and `inline given` methods for generation of custom codes.
Scala 3.5.0+ shows compilation time warning `New anonymous class definition will be duplicated at each inline site`
for some `inline given` cases, but for other use cases the compiler will silently generate duplicated codec instances.
To mitigate that convert methods of codec generation to `def` and explicitly derive custom codecs, like here:
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

Currently, the `gh-pages` branch contains a lot of historycal data of benchmark results, so to
avoid cloing 10Gb of them use `--single-branch` branch option to fetch sources only.

If developing on a fork, make sure to download the git tags (required by the sbt build):
```sh
git remote add upstream git@github.com:plokhotnyuk/jsoniter-scala.git
git fetch --tags upstream
```

Prerequisites for building of Scala.js and Scala Native modules are Clang 18.x and Node.js 16.x.
The following sequence of commands works for me:
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
sbt -java-home /usr/lib/jvm/jdk-11 ++2.13.15 clean coverage jsoniter-scala-coreJVM/test jsoniter-scala-circeJVM/test jsoniter-scala-macrosJVM/test jsoniter-scala-benchmarkJVM/test coverageReport
sbt -java-home /usr/lib/jvm/jdk-11 clean +test +mimaReportBinaryIssues
```

BEWARE: jsoniter-scala is included into [Scala Community Build](https://github.com/scala/community-builds)
 for Scala 2 and [Scala Open Community Build](https://scala3.westeurope.cloudapp.azure.com/job/runBuild/) for Scala 3.

### Run JVM benchmarks

Before benchmark running check if your CPU works in `performance` mode (not a `powersave` one). On Linux use following
commands to print current and set the `performance` mode:
```sh
cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
for i in $(ls /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor); do echo performance | sudo tee $i; done
```
Then view your CPU frequency with:
```sh
cat /proc/cpuinfo | grep -i mhz
```

Stop un-needed applications and services. List of running services can be printed by:
```sh
sudo service --status-all | grep '\[ + \]'
sudo systemctl list-units --state running
```

Then clear cache memory to improve system performance. One way to clear cache memory
on Linux without having to reboot the system:
```sh
sudo su
free -m -h && sync && echo 3 > /proc/sys/vm/drop_caches && free -m -h
```

Sbt plugin for JMH tool is used for benchmarking, to see all their features and options please check
[Sbt-JMH docs](https://github.com/ktoso/sbt-jmh) and [JMH tool docs](https://openjdk.java.net/projects/code-tools/jmh/)

Learn how to write benchmarks in [JMH samples](https://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
 and JMH articles posted in [Aleksey Shipilёv’s](https://shipilev.net/) and [Nitsan Wakart’s](https://psy-lob-saw.blogspot.com/p/jmh-related-posts.html)
 blogs.

List of available options can be printed by:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -h'
```

Results of benchmark can be stored in different formats: *.csv, *.json, etc. All supported formats can be listed by:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -lrf'
```

JMH allows running benchmarks with different profilers, to get a list of supported use (can require entering of user
password):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -lprof'
```

Help for profiler options can be printed by following command (`<profiler_name>` should be replaced by the name of the
supported profiler from the command above):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof <profiler_name>:help'
```

For parametrized benchmarks the constant value(s) for parameter(s) can be set by `-p` option:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -p size=1,10,100,1000 ArrayOf.*'
```

To see throughput with the allocation rate of generated codecs run benchmarks with GC profiler using the following
command:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof gc .*Reading.*'
```

Results that are stored in JSON can be easy plotted in [JMH Visualizer](https://jmh.morethan.io/) by drugging & dropping
of your file to the drop zone or using the `source` parameter with an HTTP link to your file in the URL like
[here](https://jmh.morethan.io/?source=https://plokhotnyuk.github.io/jsoniter-scala/oraclejdk11.json).

On Linux the perf profiler can be used to see CPU event statistics normalized per ops:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof perfnorm TwitterAPIReading.jsoniterScala'
```

Also, it can be run with a specified list of events. Here is an example of benchmarking using 16 threads to check of CPU
stalls: 
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -t 16 -prof "perfnorm:event=cycles,instructions,uops_executed.core,uops_executed.stall_cycles,cache-references,cache-misses,cycle_activity.stalls_total,cycle_activity.stalls_mem_any,cycle_activity.stalls_l3_miss,cycle_activity.stalls_l2_miss,cycle_activity.stalls_l1d_miss" .*'
```

List of available events for the perf profiler can be retrieved by the following command:
```sh
perf list
```

To get a result for some benchmarks with an in-flight recording file from JFR profiler use command like this:
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof "jfr:dir=target/jfr-reports" -wi 10 -i 60 TwitterAPIReading.jsoniterScala'
```
You will get the profile in the `jsoniter-scala-benchmark/jvm/target/jfr-reports` directory.

To run benchmarks with recordings by [Async profiler](https://github.com/jvm-profiling-tools/async-profiler), extract
binaries to `/opt/async-profiler` directory and set the following runtime variables to capture kernel frames:
```sh
sudo sysctl kernel.perf_event_paranoid=1
sudo sysctl kernel.kptr_restrict=0
```

Then use command like this:
```sh
sbt -java-home /usr/lib/jvm/jdk-21 jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof "async:dir=target/async-reports;interval=1000000;output=flamegraph;libPath=/opt/async-profiler/lib/libasyncProfiler.so" -jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" --p size=128 -wi 5 -i 10 jsoniterScala'
```
Now you can open direct and reverse flame graphs in the `jsoniter-scala-benchmark/jvmtarget/async-reports` directory.

Beware that `-XX:+DebugNonSafepoints` can lead to incorrect report due to [a bug which was fixed only for JDK 21 currently](https://bugs.openjdk.org/browse/JDK-8201516).  

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

Following command can be used to profile and print assembly code of the hottest methods, but it requires [a setup of 
`hsdis` library to make PrintAssembly feature enabled](https://builds.shipilev.net/hsdis/):
```sh
sbt jsoniter-scala-benchmarkJVM/clean 'jsoniter-scala-benchmarkJVM/jmh:run -prof perfasm -wi 10 -i 10 -p size=128 BigIntReading.jsoniterScala'
```

More info about extras, options, and ability to generate flame graphs see in [Sbt-JMH docs](https://github.com/ktoso/sbt-jmh)

Other benchmarks with results for jsoniter-scala:
- [comparison](https://github.com/kostya/benchmarks/tree/master#json) with other JSON parsers for different programming
  languages and compilers
- [comparison](https://github.com/simdjson/simdjson-java/pull/31) with most performant java parsers: simdjson-java, fastjson, jackson, etc.
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

Use JDK 11+ for building of `jsoniter-scala-benchmarkJS` module for Scala 2.13 and Scala 3:
```sh
sbt -DassemblyJSBenchmarks -java-home /usr/lib/jvm/jdk-11 +jsoniter-scala-benchmarkJS/fullOptJS
```

Then open the list of benchmarks in a browser:
```sh
cd jsoniter-scala-benchmark/js
open scala-3-fullopt.html
open scala-2.13-fullopt.html
```

Then select the batch mode with storing results in a `.zip` file.

Use the following command for merging unpacked results from browsers: `jq -s '[.[][]]' firefox/*.json >firefox.json` 

The released version of Scala.js benchmarks is available [here](https://plokhotnyuk.github.io/jsoniter-scala/scala-3-fullopt.html).

### Run compilation time benchmarks

Use the [circe-argonaut-compile-times](https://github.com/stephennancekivell/circe-argonaut-compile-times) project to 
compare compilation time of jsoniter-scala for deeply nested product types with other JSON parsers like argonaut, 
play-json, and circe in 3 modes: auto, semi-auto, and derivation.

For Scala 3 use the [scala3-compile-tests](https://github.com/pme123/scala3-compile-tests) project to compare
compilation time of jsoniter-scala for Scala 3 enumerations (sum types) with circe in semi-auto mode.

### Publish locally

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
sbt -java-home /usr/lib/jvm/jdk-11 -J-Xmx12g clean release
```

Do not push changes to GitHub until promoted artifacts for the new version are not available for downloading on
[Maven Central Repository](https://repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala)
to avoid binary compatibility check failures in triggered Travis CI builds.

The last step is updating of the tag info in a [release list](https://github.com/plokhotnyuk/jsoniter-scala/releases).
