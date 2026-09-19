Examples with runtime configuration options for:
- Pretty printing and stack traces for debugging
- Max buffer sizes for safety when parsing from `java.io.InputStream` (or `java.nio.DirectByteBuffer`)
- Buffer sizes and disabling of exception hex dump for performance

## Challenge
Serialize the following string to JSON representation using `writeToString` and then convert resulting string to
byte array using some non UTF-8 charset and then parse it back using `readFromByteArray`. Will jsoniter-scala
parser throw an error with a helpful message? 

## Recap