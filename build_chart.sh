echo 'var providedBenchmarks = ["Corretto 8", "Corretto 11", "OpenJDK 16", "GraalVM CE 8", "GraalVM CE 11", "GraalVM EE 8", "GraalVM EE 11"];
var providedBenchmarkStore = {
"Corretto 8":'
cat corretto-8.json
echo ', "Corretto 11":'
cat corretto-11.json
echo ', "OpenJDK 16":'
cat openjdk-16.json
echo ', "GraalVM CE 8":'
cat graalvm-ce-java8.json
echo ', "GraalVM CE 11":'
cat graalvm-ce-java11.json
echo ', "GraalVM EE 8":'
cat graalvm-ee-java8.json
echo ', "GraalVM EE 11":'
cat graalvm-ee-java11.json
echo '}'
