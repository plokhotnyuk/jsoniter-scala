long jsoniter_scala_multiply_high(long x, long y) {
  unsigned __int128 z = x * (unsigned __int128)y;
  return z >> 64;
}
