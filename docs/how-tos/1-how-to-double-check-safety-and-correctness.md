- Check if some security sensitive options of compile-time and runtime configuration are safe for your use cases
- Print and analyze sources of generated codecs (check number of anonymous classes, size of generated methods, etc.)
- Patch jsoniter-scala sources with some instrumentation, build and publish locally (as example to check number of instantiated codecs)

Challenge: Find a probable correctness or security flaw and submit a bug issue to jsoniter-scala project