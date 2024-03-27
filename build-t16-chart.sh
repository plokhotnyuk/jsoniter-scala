sed -i '/"gc./ s//"·gc./g' *.json
echo 'var providedBenchmarks = ["JDK 17", "JDK 21", "JDK 23", "GraalVM CE JDK 21", "GraalVM CE JDK 23", "GraalVM JDK 17", "GraalVM JDK 21", "GraalVM JDK 22"];
var providedBenchmarkStore = {
"JDK 17":'
jq -c . < jdk-17-t16.json
echo ', "JDK 21":'
jq -c . < jdk-21-t16.json
echo ', "JDK 23":'
jq -c . < jdk-23-t16.json
echo ', "GraalVM CE JDK 21":'
jq -c . < graalvm-community-jdk-21-t16.json
echo ', "GraalVM CE JDK 23":'
jq -c . < graalvm-community-jdk-23-t16.json
echo ', "GraalVM JDK 17":'
jq -c . < graalvm-jdk-17-t16.json
echo ', "GraalVM JDK 21":'
jq -c . < graalvm-jdk-21-t16.json
echo ', "GraalVM JDK 22":'
jq -c . < graalvm-jdk-22-t16.json
echo '}'
