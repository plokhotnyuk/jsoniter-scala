package com.github.plokhotnyuk.jsoniter_scala.macros

import org.scalatest.exceptions.TestFailedException

class JsonCodecMakerCompileTimeEvalSpec extends VerifyingSpec {
  "JsonCodecMaker.make" should {
    "don't generate codecs when a parameter of the 'make' call depends on not yet compiled code" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object A {
          |  def f(fullClassName: String): String = fullClassName.split('.').head.charAt(0).toString
          |  case class B(i: Int)
          |  implicit val c = JsonCodecMaker.make[B](CodecMakerConfig.withAdtLeafClassNameMapper(f))
          |}""".stripMargin
      }).getMessage.contains {
        """Cannot evaluate a parameter of the 'make' macro call for type
          |'com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerCompileTimeEvalSpec.A.B'.
          |It should not depend on code from the same compilation module where the 'make' macro is called.
          |Use a separated submodule of the project to compile all such dependencies before their usage for
          |generation of codecs. Cause:""".stripMargin.replace('\n', ' ')
      })
    }
    "don't generate codecs when a parameter of the '@named' annotation depends on not yet compiled code" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object A {
          |  def f(x: String): String = x
          |  case class B(@named(f("XXX")) i: Int)
          |  implicit val c = JsonCodecMaker.make[B]
          |}""".stripMargin
      }).getMessage.contains {
        """Cannot evaluate a parameter of the '@named' annotation in type
          |'com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMakerCompileTimeEvalSpec.A.B'.
          |It should not depend on code from the same compilation module where the 'make' macro is called.
          |Use a separated submodule of the project to compile all such dependencies before their usage for
          |generation of codecs. Cause:""".stripMargin.replace('\n', ' ')
      })
    }
  }
}