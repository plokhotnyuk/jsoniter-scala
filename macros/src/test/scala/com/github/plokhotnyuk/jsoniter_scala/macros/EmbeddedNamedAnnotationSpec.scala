package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
import org.scalacheck.Gen
import org.scalatest.FunSuite
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
  * This is a failing case I ran across. I found that removing the annotation in Event
  * or adding an explicit codec for Event fixed it, but I didn't expect to need to do
  * either of those things.
  *
  * The problem seems to be that any value in the inner json object is deserialized to
  * None. I suspect the annotation isn't used when creating the Inner part of the
  * Outer codec.
  */
class EmbeddedNamedAnnotationSpec extends FunSuite with GeneratorDrivenPropertyChecks {

  test("outer") {
    forAll(Outer.genOuter) { expected =>
      val j = new String(writeToArray(expected))
      val actual = readFromArray[Outer](j.getBytes("UTF-8"))

      assertResult(expected)(actual)
    }
  }

}

case class Outer(
  inner: Inner
)

object Outer {
  implicit val jsonCodec: JsonValueCodec[Outer] =
    JsonCodecMaker.make[Outer](CodecMakerConfig())

  val genOuter: Gen[Outer] = {
    for {
      events <- Inner.genInner
    } yield Outer(events)
  }
}

case class Inner(
  @named("id") maybeId: Option[String]
)

object Inner {
//  implicit val jsonCodec: JsonValueCodec[Event] =
//    JsonCodecMaker.make[Event](CodecMakerConfig(skipUnexpectedFields = true))

  val genInner: Gen[Inner] =
    for {
      maybeId <- Gen.option(Gen.alphaStr)
    } yield Inner(maybeId)
}
