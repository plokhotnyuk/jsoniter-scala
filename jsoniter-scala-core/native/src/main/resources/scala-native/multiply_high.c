long jsoniter_scala_multiply_high(long x, long y) {
  unsigned __int128 z = x * (unsigned __int128)y;
  return z >> 64;
}

unsigned long jsoniter_scala_unsigned_multiply_high(unsigned long x, unsigned long y) {
  unsigned __int128 z = x * (unsigned __int128)y;
  return z >> 64;
}
