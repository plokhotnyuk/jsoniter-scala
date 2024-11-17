package com.github.plokhotnyuk.jsoniter_scala.core

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueType.JsonValueType
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

class JsonStructuredReaderSpec extends AnyFunSpec with Matchers {
  describe("JsonStructuredReaderSpec") {
    it("should parse booleans") {
      implicit val testCodec = makeDecoder(in => ExampleResult(in.readBoolean))

      "true" should decodeAs(true)
      "false" should decodeAs(false)

      "0" should failDecoding
      "" should failDecoding
      "tru" should failDecoding
    }

    it("should parse null") {
      implicit val testCodec = makeDecoder(in => ExampleResult(in.readNull))

      "null" should decodeAs(null)

      "nul" should failDecoding
      "true" should failDecoding
      "4.5" should failDecoding
      "\"foo\"" should failDecoding
      "" should failDecoding
    }

    it("should parse numbers") {
      implicit val testCodec = makeDecoder(in => ExampleResult(in.readNumber))

      "123" should decodeAs(123.0)
      "4.5" should decodeAs(4.5)

      "\"foo\"" should failDecoding
      "" should failDecoding
      "null" should failDecoding
      "tru" should failDecoding
    }

    it("should parse strings") {
      implicit val testCodec = makeDecoder(in => ExampleResult(in.readString))

      "\"foo\"" should decodeAs("foo")
      "\"\"" should decodeAs("")

      "0" should failDecoding
      "" should failDecoding
      "tru" should failDecoding
    }

    it("should parse arrays") {
      implicit val testCodec = makeDecoder(in =>
        ExampleResult(in.readArray {
          in.readNumber
        })
      )

      "[1, 2, 3]" should decodeAs(ArraySeq[Any](1.0, 2.0, 3.0))
      "[1]" should decodeAs(ArraySeq[Any](1.0))
      "[]" should decodeAs(ArraySeq[Any]())

      "[1, 2, 3" should failDecoding
      "[, 1, 2, 3]" should failDecoding
      "[1, 2, 3, ]" should failDecoding

      "1" should failDecoding
      "" should failDecoding
      "tru" should failDecoding
    }

    it("should parse objects") {
      implicit val testCodec = makeDecoder { in =>
        var x: Double = 0
        var y: Double = 0

        ExampleResult(
          in.readObject(
            {
              case "x" => x = in.readNumber
              case "y" => y = in.readNumber
              case _ => in.skipValue // Ignore other fields
            },
            (x, y)
          )
        )
      }

      "{\"x\": 1.0, \"y\": 2.0}" should decodeAs((1.0, 2.0))
      "{\"y\": 1.0, \"x\": 2.0}" should decodeAs((2.0, 1.0))
      "{\"x\": 1.0, \"y\": 2.0, \"z\": 3.0}" should decodeAs((1.0, 2.0))
      "{\"x\": 1.0}" should decodeAs((1.0, 0.0))
      "{\"y\": 1.0}" should decodeAs((0.0, 1.0))
      "{}" should decodeAs((0.0, 0.0))

      "{, \"x\": 1.0, \"y\": 2.0}" should failDecoding
      "{\"x\": 1.0, \"y\": 2.0, }" should failDecoding
      "{\"x: 1.0, \"y\": 2.0}" should failDecoding
      "{x: 1.0, \"y\": 2.0}" should failDecoding
      "{\"x\": null, \"y\": 2.0}" should failDecoding

      "[1, 2, 3]" should failDecoding
      "1" should failDecoding
      "" should failDecoding
      "tru" should failDecoding
    }

    it("should chain to other decoders") {
      case class TypeRef(name: String)

      implicit object OtherCodec extends JsonValueCodec[TypeRef] {
        override def decodeValue(in: JsonReader, default: TypeRef): TypeRef = {
          TypeRef(in.readString(null))
        }

        override def encodeValue(x: TypeRef, out: JsonWriter): Unit = ()

        override def nullValue: TypeRef = null
      }

      implicit val testCodec = makeDecoder(in => ExampleResult(in.read[TypeRef]))

      readFromString[TypeRef]("\"foo\"") shouldBe TypeRef("foo")

      "\"foo\"" should decodeAs(TypeRef("foo"))
      "foo" should failDecoding
      "true" should failDecoding
    }

    it("should peek for the next token") {
      "[1,2,3]" should havePeek(JsonValueType.Array)
      "true" should havePeek(JsonValueType.Boolean)
      "null" should havePeek(JsonValueType.Null)
      "123" should havePeek(JsonValueType.Number)
      "-123" should havePeek(JsonValueType.Number)
      "{}" should havePeek(JsonValueType.Object)
      "\"foo\"" should havePeek(JsonValueType.String)
    }

    it("should support the MetricsData example") {
      case class MetricData(data: ArraySeq[ArraySeq[Any]])

      implicit object codecForMetricData extends JsonStructuredCodec[MetricData] {
        override def decodeValue(in: JsonStructuredReader, default: MetricData): MetricData = {
          val data: ArraySeq[ArraySeq[Any]] =
            in.readArray {
              in.readArray {
                in.peek match {
                  case JsonValueType.Boolean => in.readBoolean
                  case JsonValueType.Null => in.readNull
                  case JsonValueType.Number => in.readNumber
                  case JsonValueType.String => in.readString
                  case _ => in.decodeError("Expected a primitive value")
                }
              }
            }

          MetricData(data)
        }
        override def encodeValue(x: MetricData, out: JsonWriter): Unit = ()
        override def nullValue: MetricData = null
      }

      val jsonInput =
        """[ [ 1, 2, 3 ],
          |  [ 4, 5, 6]
          |]
          |""".stripMargin

      readFromString[MetricData](jsonInput) shouldBe MetricData(
        ArraySeq(
          ArraySeq[Any](1.0, 2.0, 3.0),
          ArraySeq[Any](4.0, 5.0, 6.0)
        )
      )
    }
  }

  /** A type to make custom decoders for */
  case class ExampleResult(result: Any)

  /** Make a full JsonValueCodec for testing, given just an implementation of `decode`. */
  private def makeDecoder(decode: JsonStructuredReader => ExampleResult) = {
    new JsonValueCodec[ExampleResult] {

      override def decodeValue(in: JsonReader, default: ExampleResult): ExampleResult = {
        val structuredReader = new JsonStructuredReader(in)
        decode(structuredReader)
      }

      override def encodeValue(x: ExampleResult, out: JsonWriter): Unit = ()

      override def nullValue: ExampleResult = null
    }
  }

  /** Parse the input string and compute the value that comes from it. */
  private def computeParseResult(inputJson: String)(implicit exampleCodec: JsonValueCodec[ExampleResult]): Try[Any] = {
    try {
      Success(readFromString[ExampleResult](inputJson).result)
    } catch {
      case err: JsonReaderException => Failure(err)
    }
  }

  /** Test that the given JSON string will decode to an expected value */
  def decodeAs(expected: Any)(implicit exampleCodec: JsonValueCodec[ExampleResult]) = new Matcher[String] {
    override def apply(left: String): MatchResult = {
      val parseResult: Try[Any] = computeParseResult(left)

      MatchResult(
        parseResult.isSuccess && parseResult.get === expected,
        s"Unexpected parse of '$left'. Expected $expected but got $parseResult.'",
        s"The decode result was the expected value."
      )
    }
  }

  /** Test that the given JSON string will fail to decode */
  def failDecoding(implicit exampleCodec: JsonValueCodec[ExampleResult]) = new Matcher[String] {
    override def apply(left: String): MatchResult = {
      val parseResult: Try[Any] = computeParseResult(left)

      MatchResult(
        parseResult.isFailure,
        s"Unexpected parse of '$left'. Expected a failure, but got $parseResult.'",
        s"The decode result was the expected value."
      )
    }
  }

  /** Check that peeking at the text on the left will have the type on the right
    */
  def havePeek(right: JsonValueType) = new Matcher[String] {
    override def apply(left: String): MatchResult = {
      val codec = new JsonValueCodec[JsonValueType] {
        override def decodeValue(in: JsonReader, default: JsonValueType): JsonValueType = {
          val structuredReader = new JsonStructuredReader(in)
          val result = structuredReader.peek
          structuredReader.skipValue
          result
        }

        override def encodeValue(x: JsonValueType, out: JsonWriter): Unit = ()

        override def nullValue: JsonValueType = null
      }

      val actual: JsonValueType = readFromString(left)(codec)

      MatchResult(
        actual == right,
        s"Expected the peek type of $left to be $right but was $actual",
        "The peek type was correct."
      )
    }
  }
}
