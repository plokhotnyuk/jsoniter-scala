echo 'var providedBenchmarks = ["OpenJDK 8", "OpenJDK 11", "OpenJDK 13", "OpenJDK 13 Graal", "GraalVM CE 19", "GraalVM EE 19"];
var providedBenchmarkStore = {
"OpenJDK 8":'
cat openjdk8.json
echo ', "OpenJDK 11":'
cat openjdk11.json
echo ', "OpenJDK 13":'
cat openjdk13.json
echo ', "OpenJDK 13 Graal":'
cat openjdk13graal.json
echo ', "GraalVM CE 19":'
cat graalvmce19.json
echo ', "GraalVM EE 19":'
cat graalvmee19.json
echo '}'
