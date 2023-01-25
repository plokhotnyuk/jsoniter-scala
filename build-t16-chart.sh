echo 'var providedBenchmarks = ["Zulu 11", "Zulu 17", "OpenJDK 21", "GraalVM CE 19", "GraalVM EE 11", "GraalVM EE 17", "GraalVM EE 19"];
var providedBenchmarkStore = {
"Zulu 11":'
jq -c . < zulu-11-t16.json
echo ', "Zulu 17":'
jq -c . < zulu-17-t16.json
echo ', "OpenJDK 21":'
jq -c . < openjdk-21-t16.json
echo ', "GraalVM CE 19":'
jq -c . < graalvm-ce-java19-t16.json
echo ', "GraalVM EE 11":'
jq -c . < graalvm-ee-java11-t16.json
echo ', "GraalVM EE 17":'
jq -c . < graalvm-ee-java17-t16.json
echo ', "GraalVM EE 19":'
jq -c . < graalvm-ee-java19-t16.json
echo '}'
