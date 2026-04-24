/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.macros

import org.scalatest.exceptions.TestFailedException

class JsonCodecMakerCompileTimeEvalSpec extends VerifyingSpec {
  "JsonCodecMaker.make" should {
    "don't generate codecs when a parameter of the 'make' call depends on not yet compiled code" in {
      assert(intercept[TestFailedException](assertCompiles {
        """object A {
          |  def f(fullClassName: String): String = fullClassName.split('.').head.charAt(0).toString
          |  case class B(i: Int)
          |  val c = JsonCodecMaker.make[B](CodecMakerConfig.withAdtLeafClassNameMapper(f))
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
          |  val c = JsonCodecMaker.make[B]
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