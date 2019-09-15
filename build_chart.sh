echo 'var providedBenchmarks = ["OpenJ9 JDK 11", "OpenJDK 8", "OpenJDK 11", "OpenJDK 13", "OpenJDK 13 Graal", "GraalVM CE 19", "GraalVM EE 19", "Zing JDK 11"];
var providedBenchmarkStore = {
"OpenJ9 JDK 11":'
cat openj9jdk11.json
echo ', "OpenJDK 8":'
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
echo ', "Zing JDK 11":'
cat zingjdk11.json
echo '}'
