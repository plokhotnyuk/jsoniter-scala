Approaches to consider:
- Switch to Scala 3 (safer and faster generation of smaller codecs)
- Use data-structures that follow JSON representation as close as possible (don't forget compile-time configuration during derivation or using annotations)
- Use 2 data models for huge projects with different API versions or 3-rd party data structures (use chimney or ducktape for transformation between them)
- Use GraphQL (caliban) for reach and highly customized requests
- Use Smithy (smithy4s-json) for model first approach with cross-language APIs

Challenge: Use Dependents side section of the https://index.scala-lang.org/plokhotnyuk/jsoniter-scala page to discover and try some other integrations with jsoniter-scala