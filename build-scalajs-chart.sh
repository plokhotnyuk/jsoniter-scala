echo 'var providedBenchmarks = ["Chrome", "Edge", "Firefox", "Opera", "Safari"];
var providedBenchmarkStore = {
"Chrome":'
jq -c . < chrome.json
echo ', "Edge":'
jq -c . < edge.json
echo ', "Firefox":'
jq -c . < firefox.json
echo ', "Opera":'
jq -c . < opera.json
echo ', "Safari":'
jq -c . < safari.json
echo '}'
