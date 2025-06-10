#if defined(_WIN64)

#include <intrin.h>

long long jsoniter_scala_multiply_high(long long x, long long y) {
  return __mulh(x, y);
}

unsigned long long jsoniter_scala_unsigned_multiply_high(unsigned long long x, unsigned long long y) {
  return __umulh(x, y);
}

#elif defined(__SIZEOF_INT128__)

long long jsoniter_scala_multiply_high(long long x, long long y) {
  return x * (__int128) y >> 64;
}

unsigned long long jsoniter_scala_unsigned_multiply_high(unsigned long long x, unsigned long long y) {
  return x * (unsigned __int128) y >> 64;
}

#else

// Based on `int mulhs(int u, int v)` function from Hacker’s Delight 2nd Ed. by Henry S. Warren, Jr.
long long jsoniter_scala_multiply_high(long long x, long long y) {
  long long x0 = x & 0xFFFFFFFFL;
  long long x1 = x >> 32;
  long long y0 = y & 0xFFFFFFFFL;
  long long y1 = y >> 32;
  long long t = x1 * y0 + ((unsigned long long) (x0 * y0) >> 32);
  return x1 * y1 + (t >> 32) + (((t & 0xFFFFFFFFL) + x0 * y1) >> 32);
}

// Based on `unsigned mulhu(unsigned u, unsigned v)` function from Hacker’s Delight 2nd Ed. by Henry S. Warren, Jr.
unsigned long long jsoniter_scala_unsigned_multiply_high(unsigned long long x, unsigned long long y) {
  unsigned long long x0 = x & 0xFFFFFFFFL;
  unsigned long long x1 = x >> 32;
  unsigned long long y0 = y & 0xFFFFFFFFL;
  unsigned long long y1 = y >> 32;
  unsigned long long t = x1 * y0 + ((x0 * y0) >> 32);
  return x1 * y1 + (t >> 32) + (((t & 0xFFFFFFFFL) + x0 * y1) >> 32);
}

#endif
