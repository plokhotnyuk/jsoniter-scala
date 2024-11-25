1. CodecMakerConfiguration (renaming, stringification, enabling of recursion, disabling generation of decoding or encoding implementations, etc.)
2. Static annotations at field and class definitions and their priority over CodecMakerConfiguration
3. Codec injection using implicits or givens

## Challenge
Experiment with different combinations of compile-time options and find those that are not supported. 
Will compiler errors explain why codecs cannot be generated for such options?

## Recap
