sed -i '/"gc./ s//"·gc./g' *.json
echo 'var providedBenchmarks = ["JDK 17", "JDK 21", "JDK 24", "GraalVM CE JDK 17", "GraalVM CE JDK 21", "GraalVM CE JDK 24", "GraalVM JDK 17", "GraalVM JDK 21", "GraalVM JDK 22"];
var providedBenchmarkStore = {
"JDK 17":'
jq -c . < jdk-17.json
echo ', "JDK 21":'
jq -c . < jdk-21.json
echo ', "JDK 24":'
jq -c . < jdk-24.json
echo ', "GraalVM CE JDK 17:'
jq -c . < graalvm-community-jdk-17.json
echo ', "GraalVM CE JDK 21":'
jq -c . < graalvm-community-jdk-21.json
echo ', "GraalVM CE JDK 24":'
jq -c . < graalvm-community-jdk-24.json
echo ', "GraalVM JDK 17":'
jq -c . < graalvm-jdk-17.json
echo ', "GraalVM JDK 21":'
jq -c . < graalvm-jdk-21.json
echo ', "GraalVM JDK 22":'
jq -c . < graalvm-jdk-22.json
echo '}'
