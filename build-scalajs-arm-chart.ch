echo 'var providedBenchmarks = ["Chrome", "Edge", "Firefox", "Safari"];
var providedBenchmarkStore = {
"Chrome":'
jq -c . < chrome-arm.json
echo ', "Edge":'
jq -c . < edge-arm.json
echo ', "Firefox":'
jq -c . < firefox-arm.json
echo ', "Safari":'
jq -c . < safari-arm.json
echo '}'
