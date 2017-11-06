# Jsoniter Scala 

[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala) [![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala) [![license](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt) [![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/plokhotnyuk/jsoniter-scala?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Scala macros that generates codecs for case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

## Features and limitations
- JSON parsing from `Array[Byte]` or `java.io.InputStream`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream`
- Support of UTF-8 encoding
- Parsing of strings with escaped characters for JSON field names, keys & values 
- Configurable serialization of strings with escaped Unicode characters to be ASCII compatible
- Configurable indenting of output
- Primitives, boxed primitives, enums, `String`, `BigInt`, `BigDecimal`, `Option`, Scala collections, 
  arrays and value classes for all them or case classes are supported 
- Types that supported as map keys are all which can be represented by single value, basically it is all above excluding case classes, arrays & collections  
- Implicitly resolvable codecs for any types: primitive, collections, enums, ADTs, etc.
- Need to materialize for all case classes used in nested structures using implicitly resolvable codecs
- Case classes should be defined as a top-level class or directly inside of another class or object
- Fields with default values that defined in a constructor are optional, other fields are required (no special annotation required)
- Fields with default values, empty options & empty collections/arrays are not serialized to provide sparse output 
- Fields can be annotated as transient or just not defined in constructor to avoid parsing and serializing at all 
- Key names can be overridden by field annotation
- Field names are matched by hash and can have collisions with other keys on the same JSON object value
- TODO: generate efficient and no-allocation collision resolving for field name matching
- TODO: generate codecs recursively from top-level type
- TODO: code generation option to serialize/parse all fields to/from snake_case or camelCase keys 
- TODO: extend codecs to allow using them for serialization/parsing of types to/from map keys
- TODO: more efficient implementation for serialization and parsing of numbers 
- TODO: efficient specialized immutable list & set for primitive types

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
import com.github.plokhotnyuk.jsoniter_scala.JsonCodec

case class Device(id: Int, model: String)
implicit val deviceCodec = JsonCodec.materialize[Device]

case class User(name: String, devices: Seq[Device])
val userCodec = JsonCodec.materialize[User]
```

That's it! You have generated couple instances of `com.github.plokhotnyuk.jsoniter_scala.Codec`.

Now you can use them:

```scala
import com.github.plokhotnyuk.jsoniter_scala.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.JsonWriter

val user = JsonReader.read(userCodec, """{"name":"John","devices":[{"id":1,model:"HTC One X"}]}""".getBytes("UTF-8"))
val jsonBytes = JsonWriter.write(userCodec, User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))
```

To see generated code add the following line to your sbt build file

```sbt
scalaOptions += "-Xmacro-settings:print-codecs"
```

For more features & examples, please, check out
[JsonCodecSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/JsonCodecSpec.scala)


## How to develop

Feel free to ask questions in [chat](https://gitter.im/plokhotnyuk/jsoniter-scala), open issues, or contribute by creating pull requests (code or/and tests are highly appreciated)

Run tests and check coverage

```sh
sbt clean +coverage +test +coverageReport
```

Run benchmarks

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*JsonCodecBenchmark.*' >results.txt
```

Current results for the following environment:

[./results.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results.txt) Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 15.04, Linux 4.4.0-38-generic, Oracle JDK build 1.8.0_152-b16 64-bit

[./results-5257U.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results-5257U.txt) Intel(R) Core(TM) i5-5257U CPU @ 2.70GHz (max 3.10GHz, 2 cores, 4 TH), RAM 16Gb DDR3-1867, macos 10.11.6, Oracle JDK build 1.8.0_152-b16 64-bit


## Acknowledges

[Jsoniter Java](https://github.com/json-iterator/java)

[Kryo Macros](https://github.com/evolution-gaming/kryo-macros)

[AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)