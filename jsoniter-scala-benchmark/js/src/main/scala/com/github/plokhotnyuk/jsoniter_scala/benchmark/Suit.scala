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