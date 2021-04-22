echo 'var providedBenchmarks = ["Corretto 8", "Corretto 11", "OpenJDK 17", "GraalVM CE 11", "GraalVM CE 16", "GraalVM EE 8", "GraalVM EE 11", "GraalVM EE 16"];
var providedBenchmarkStore = {
"Corretto 8":'
jq -c . < corretto-8.json
echo ', "Corretto 11":'
jq -c . < corretto-11.json
echo ', "OpenJDK 17":'
jq -c . < openjdk-17.json
echo ', "GraalVM CE 11":'
jq -c . < graalvm-ce-java11.json
echo ', "GraalVM CE 16":'
jq -c . < graalvm-ce-java16.json
echo ', "GraalVM EE 8":'
jq -c . < graalvm-ee-java8.json
echo ', "GraalVM EE 11":'
jq -c . < graalvm-ee-java11.json
echo ', "GraalVM EE 16":'
jq -c . < graalvm-ee-java16.json
echo '}'
