echo 'var providedBenchmarks = ["OpenJDK 8", "OpenJDK 11", "OpenJDK 12", "GraalVM CE 19", "GraalVM EE 19"];
var providedBenchmarkStore = {
"OpenJDK 8":'
cat openjdk8.json
echo ', "OpenJDK 11":'
cat openjdk11.json
echo ', "OpenJDK 12":'
cat openjdk12.json
echo ', "GraalVM CE 19":'
cat graalvmce19.json
echo ', "GraalVM EE 19":'
cat graalvmee19.json
echo '}'
