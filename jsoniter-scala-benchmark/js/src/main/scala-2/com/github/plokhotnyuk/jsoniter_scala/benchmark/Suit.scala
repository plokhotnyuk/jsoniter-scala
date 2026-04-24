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

package com.github.plokhotnyuk.jsoniter_scala.benchmark;

object Suit {
  val Hearts: Suit = new Suit("Hearts", 0)
  val Spades: Suit = new Suit("Spades", 1)
  val Diamonds: Suit = new Suit("Diamonds", 2)
  val Clubs: Suit = new Suit("Clubs", 3)
  val values: Array[Suit] = Array(Hearts, Spades, Diamonds, Clubs)

  def valueOf(name: String): Suit =
    if (Hearts.name() == name) Hearts
    else if (Spades.name() == name) Spades
    else if (Diamonds.name() == name) Diamonds
    else if (Clubs.name() == name) Clubs
    else throw new IllegalArgumentException(s"Unrecognized Suit name: $name")
}

final class Suit private(name: String, ordinal: Int) extends Enum[Suit](name, ordinal)