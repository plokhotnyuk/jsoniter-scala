[![build status](https://travis-ci.org/plokhotnyuk/jsoniter-scala.svg?branch=master)](https://travis-ci.org/plokhotnyuk/jsoniter-scala)
[![code coverage](https://codecov.io/gh/plokhotnyuk/jsoniter-scala/branch/master/graph/badge.svg)](https://codecov.io/gh/plokhotnyuk/jsoniter-scala)

# Jsoniter Scala

Macros that generates `com.jsoniter.spi.Decoder` and `com.jsoniter.spi.Encoder` interfaces in compile time,
based on compile time reflection.

Main goal is to provide a static code generation for Scala case classes, standard types and collections
to get maximum performance of JSON parsing & serialization.

Features and limitations:
- JSON parsing from `Array[Byte]` or `java.io.InputStream` using `com.jsoniter.JsonIterator`
- JSON serialization to `Array[Byte]` or `java.io.OutputStream` using `com.jsoniter.JsonStream`
- Full support of UTF-8 encoding (others are only supported if all characters are less than 128)
- Case classes should be defined as a top-level class or directly inside of another class or object
- Enums, BigInt, BigDecimal, Options & Scala collections as class fields are supported
- Up to 64 required fields are supported for case classes
- Doesn't serialize null values, default values, empty options and empty collections
- Doesn't parse and serialize values of fields annotated as transient or if them are not defined in constructor
- Need to materialize manually all case classes used in nested structures
- Key names can be overridden by field annotation
- TODO: parsing of primitive types & enums from strings
- TODO: serialization of chars & enums to strings
- TODO: efficient specialized immutable list & set for Int & Long primitive types


# How to use

Build and publish locally (release to Sonatype repo is coming)

```
sbt clean +publishLocal
```

Add the library to your dependencies list

```scala
libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.1-SNAPSHOT"
```

Generate some serializers for your case classes
    
```scala
import com.github.plokhotnyuk.jsoniter_scala.Codec

case class User(name: String)

val userCodec = Codec.materialize[User]
```

That's it! You have generated an instance of `com.github.plokhotnyuk.jsoniter_scala.Codec` which implements both
`com.jsoniter.spi.Decoder` and `com.jsoniter.spi.Encoder` interfaces for your User.

Now you can use it:

```scala
userCodec.read("""{"name":"John"}""".getBytes("UTF-8"))
userCodec.write(User(name = "John"))
```

To see generated code add the following line to your sbt build file

```scala
scalaOptions += "-Xmacro-settings:print-codecs"
```

For more features & examples, please, check out
[CodecSpec](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/macros/src/test/scala/com/github/plokhotnyuk/jsoniter_scala/CodecSpec.scala)


# How to develop

Feel free to ask questions by opening issues (Gitter chat is coming), or contribute by creating pull requests (code or/and tests are highly appreciated)

Run tests and check coverage

```
sbt clean +coverage +test +coverageReport
```

Run benchmarks

```
sbt -no-colors clean 'benchmark/jmh:run -prof gc .*CodecBenchmark.*' >results.txt
```

Currently [./results.txt](https://github.com/plokhotnyuk/jsoniter-scala/blob/master/results.txt) contains results for the following environment:
Intel(R) Core(TM) i7-2760QM CPU @ 2.40GHz (max 3.50GHz), RAM 16Gb DDR3-1600, Ubuntu 15.04, Linux 4.4.0-38-generic, Oracle JDK build 1.8.0_112-b15 64-bit


# Acknowledges

[Jsoniter Java](https://github.com/json-iterator/java)

[Kryo Macros](https://github.com/evolution-gaming/kryo-macros)

[AVSystem Commons Library for Scala](https://github.com/AVSystem/scala-commons)