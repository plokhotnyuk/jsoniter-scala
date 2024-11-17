package com.github.plokhotnyuk.jsoniter_scala.core

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.reflect.ClassTag

/** A wrapper of the Jsoniter [[JsonReader]] that makes it easier to write correct custom decoders. With this API, you
  * can consume an entire JSON value at once, without having to worry about things like where the commas go. There is a
  * method in this utility for each kind of underlying JSON value. Each such method will either successfully read the
  * requested kind of value, or else throw a decode exception that includes position information.
  */
class JsonStructuredReader(jsonReader: JsonReader) {

  /** Signal a decoder error. This forwards to [[JsonReader.decodeError()]] and will throw an exception indicating the
    * problem and the position in the input string.
    */
  def decodeError(errorMessage: String) = jsonReader.decodeError(errorMessage)

  /** Read an object using a different codec. */
  def read[T](implicit codec: JsonValueCodec[T]): T =
    codec.decodeValue(jsonReader, codec.nullValue)

  /** Read a boolean (true or false). */
  def readBoolean: Boolean = {
    jsonReader.readBoolean()
  }

  /** Read a null. */
  def readNull: Null = {
    if (jsonReader.nextToken() != 'n') {
      decodeError("Expected null")
    }
    jsonReader.readNullOrError("ignored", "Expected null")
    null
  }

  /** Read a number. */
  def readNumber: Double = {
    jsonReader.readDouble()
  }

  /** Read a string. */
  def readString: String = {
    jsonReader.readString(null)
  }

  /** Read an array. The caller supplies a `readElement` function, and it will be invoked at the beginning of each
    * element of the JSON array to consume one element. The return value will be a sequence of all the elements that
    * were decoded.
    */
  def readArray[T: ClassTag](readElement: => T): ArraySeq[T] = {
    val result = ArraySeq.newBuilder[T]

    if (jsonReader.nextToken() != '[') {
      jsonReader.decodeError("Expected [")
    }

    var first = true
    while (true) {
      val tok = jsonReader.nextToken()
      if (tok == ']') {
        return result.result()
      }

      if (first) {
        first = false
        jsonReader.rollbackToken()
      } else {
        if (tok != ',') {
          decodeError(s"Expected , or ]")
        }
      }

      result += readElement
    }

    throw new RuntimeException("unreachable")
  }

  /** Read an object. For each field seen in the object, the `readField` parameter will be invoked. It will be passed
    * the name of the field, and it is expected to parse one field and record the value in its own storage. When the end
    * of the object is reached in the input, the `computeObject` parameter is invoked, and it is expected to assemble
    * all the recorded field values into a constructed value to return.
    */
  def readObject[T](readField: (String) => Unit, computeObject: => T): T = {
    if (jsonReader.nextToken() != '{') {
      decodeError("Expected {")
    }
    val fieldNames = mutable.HashSet.empty[String]

    var first = true
    while (true) {
      val tok = jsonReader.nextToken()
      if (tok == '}') {
        return computeObject
      }

      if (first) {
        first = false
        jsonReader.rollbackToken()
      } else {
        if (tok != ',') {
          decodeError("Expected , or }")
        }
      }

      val fieldName = readString
      if (fieldNames.contains(fieldName)) {
        decodeError(s"Duplicate field $fieldName")
      }
      fieldNames += fieldName

      if (jsonReader.nextToken() != ':') {
        decodeError("Expected :")
      }

      readField(fieldName)
    }

    throw new RuntimeException("Unreachable")
  }

  /** Skip one JSON value of any type. This method is useful for skipping unrecognized fields in an object. For example,
    * when decoding an HTTP response from a server, it is often best to quietly ignore any new fields the server
    * maintainer has added.
    */
  def skipValue: Unit = {
    peek match {
      case JsonValueType.Array => readArray(skipValue)
      case JsonValueType.Boolean => readBoolean
      case JsonValueType.Null => readNull
      case JsonValueType.Number => readNumber
      case JsonValueType.Object => readObject(_ => skipValue, () => ())
      case JsonValueType.String => readString
    }
  }

  /** Peek at the next value that is coming up and return what type it is. */
  def peek: JsonValueType.JsonValueType = {
    val token = jsonReader.nextToken()
    jsonReader.rollbackToken()
    token match {
      case '[' => JsonValueType.Array
      case 't' | 'f' => JsonValueType.Boolean
      case 'n' => JsonValueType.Null
      case '-' => JsonValueType.Number
      case c if c >= '0' && c <= '9' => JsonValueType.Number
      case '{' => JsonValueType.Object
      case '"' => JsonValueType.String
    }
  }
}
