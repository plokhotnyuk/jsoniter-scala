Tools to check:
- Load test with realistic or synthetic input using async-profiler in different modes (-e cpu, alloc, wall)
- Write JMH benchmarks with realistic or synthetic input using different profilers (gc, perfnorm, perfasm)
- Decompile byte code with CFR decompiler to Java

Options to consider:
- Tune sizes of internal buffers (do not rely on defaults)
- Do pretty printing or use exception stack traces only for debugging
- Use reading and writing from/to pre-allocated sub-arrays
- Use short up to 8 field names per case class (not more than 64 characters in total)
- Use ASCII field and class names
- Turn off checking of field duplication (when parsing on client side)
- Avoid using string types for data that could be immediately parsed to UUID, data-time, and numbers
- Prefer arrays (or immutable arrays in Scala 3) for JSON arrays, especially when they have primitive type values
- Prefer discriminator keys and avoid discriminator fields
- Use JSON arrays for some stable data structures with all required values (2D/3D coordinates, price/quantity, etc.)
- In product types define required fields first, then most frequently used optional fields
- In enumeration and sum-types define most frequently used classes/objects first
- Turn off hex dumps for exceptions
- Generate decoders/encoders only (reduce code size for Scala.js)

Anti-patterns:
- Generation of codecs for intermediate (non-top level) data structures (as an example, overuse of `derives` keyword)
- Generation and wide usage of codecs for primitives, strings, etc.
