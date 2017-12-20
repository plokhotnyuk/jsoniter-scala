# Change Log

## Version 0.X
_2017-12-2X_ [GitHub Diff](https://github.com/plokhotnyuk/jsoniter-scala/compare/v0.1...master)
 * Minor refactoring of reader API: changes in signatures of some methods that are used for generation of codecs. 
   If you used `macros` module only w/a own custom codecs then it doesn't require a migration. 
 * Add ability to read/write numbers & booleans from/to string values in case class fields, value classes, options,
   arrays & collections [#12](https://github.com/plokhotnyuk/jsoniter-scala/issues/12#event-1362958656)
 * Added more checks of internal state of reader
 * Added wrapping of number format exceptions by parser exceptions for cases when too big exponent for `BigDecimal` 
   is parsed
 * Turn on printer options of Circe benchmarks for more efficient serialization 
 * Clean up of docs, code, tests and benchmarks

## Version 0.1
_2017-12-12_
 * Initial release
