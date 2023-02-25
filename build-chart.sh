echo 'var providedBenchmarks = ["Zulu 11", "Zulu 17", "OpenJDK 21", "GraalVM CE 17", "GraalVM CE 20", "GraalVM EE 11", "GraalVM EE 17", "GraalVM EE 19"];
var providedBenchmarkStore = {
"Zulu 11":'
jq -c . < zulu-11.json
echo ', "Zulu 17":'
jq -c . < zulu-17.json
echo ', "OpenJDK 21":'
jq -c . < openjdk-21.json
echo ', "GraalVM CE 17":'
jq -c . < graalvm-ce-java17.json
echo ', "GraalVM CE 20":'
jq -c . < graalvm-ce-java20.json
echo ', "GraalVM EE 11":'
jq -c . < graalvm-ee-java11.json
echo ', "GraalVM EE 17":'
jq -c . < graalvm-ee-java17.json
echo ', "GraalVM EE 19":'
jq -c . < graalvm-ee-java19.json
echo '}'
