echo 'var providedBenchmarks = ["Chrome", "Edge", "Firefox", "Opera"];
var providedBenchmarkStore = {
"Chrome":'
jq -c . < chrome.json
echo ', "Edge":'
jq -c . < edge.json
echo ', "Firefox":'
jq -c . < firefox.json
echo ', "Opera":'
jq -c . < opera.json
echo '}'
