echo 'var providedBenchmarks = ["Zulu 11", "Zulu 17", "OpenJDK 22", "GraalVM Community 17", "GraalVM Community 21", "Oracle GraalVM 17", "Oracle GraalVM 20"];
var providedBenchmarkStore = {
"Zulu 11":'
jq -c . < zulu-11-t16.json
echo ', "Zulu 17":'
jq -c . < zulu-17-t16.json
echo ', "OpenJDK 22":'
jq -c . < jdk-22-t16.json
echo ', "GraalVM Community 17":'
jq -c . < graalvm-community-jdk-17-t16.json
echo ', "GraalVM Community 21":'
jq -c . < graalvm-community-jdk-21-t16.json
echo ', "Oracle GraalVM 17":'
jq -c . < oracle-graalvm-jdk-17-t16.json
echo ', "Oracle GraalVM 20":'
jq -c . < oracle-graalvm-jdk-20-t16.json
echo '}'
