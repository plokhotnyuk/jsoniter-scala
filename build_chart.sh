echo 'var providedBenchmarks = ["Zulu 11", "Zulu 17", "OpenJDK 20", "GraalVM CE 11", "GraalVM CE 17", "GraalVM EE 11", "GraalVM EE 17"];
var providedBenchmarkStore = {
"Zulu 11":'
jq -c . < zulu-11.json
echo ', "Zulu 17":'
jq -c . < zulu-17.json
echo ', "OpenJDK 20":'
jq -c . < openjdk-20.json
echo ', "GraalVM CE 11":'
jq -c . < graalvm-ce-java11.json
echo ', "GraalVM CE 17":'
jq -c . < graalvm-ce-java17.json
echo ', "GraalVM EE 11":'
jq -c . < graalvm-ee-java11.json
echo ', "GraalVM EE 17":'
jq -c . < graalvm-ee-java17.json
echo '}'
