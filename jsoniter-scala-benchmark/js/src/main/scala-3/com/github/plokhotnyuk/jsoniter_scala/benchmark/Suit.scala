package com.github.plokhotnyuk.jsoniter_scala.benchmark;

enum Suit(name: String, ordinal: Int) extends Enum[Suit]:
  case Hearts extends Suit("Hearts", 0)
  case Spades extends Suit("Spades", 1)
  case Diamonds extends Suit("Diamonds", 2)
  case Clubs extends Suit("Clubs", 3)