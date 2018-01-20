// provided.js can be overriden in order to have a predefined-report generated.


//var providedBenchmarks = []; // eslint-disable-line no-unused-vars
var providedBenchmarks = ['desktop_jdk8', 'desktop_jdk9', 'notebook_jdk8', 'notebook_jdk9']; // eslint-disable-line no-unused-vars

var providedBenchmarkStore = { // eslint-disable-line no-unused-vars
    desktop_jdk8: [
        {
            "benchmark": "io.morethan.javabenchmarks.datastructure.ListCreationBenchmark.arrayList",
            "mode": "thrpt",
            "threads": 1,
            "forks": 2,
            "warmupIterations": 3,
            "warmupTime": "1 s",
            "warmupBatchSize": 1,
            "measurementIterations": 5,
            "measurementTime": "1 s",
            "measurementBatchSize": 1,
            "primaryMetric": {
                "score": 3467631.2300505415,
                "scoreError": 98563.33258873208,
                "scoreConfidence": [
                    3369067.897461809,
                    3566194.5626392737
                ],
                "scorePercentiles": {
                    "0.0": 3333994.173460993,
                    "50.0": 3474993.5017186473,
                    "90.0": 3532339.4656069754,
                    "95.0": 3532453.3325443356,
                    "99.0": 3532453.3325443356,
                    "99.9": 3532453.3325443356,
                    "99.99": 3532453.3325443356,
                    "99.999": 3532453.3325443356,
                    "99.9999": 3532453.3325443356,
                    "100.0": 3532453.3325443356
                },
                "scoreUnit": "ops/s",
                "rawData": [
                    [
                        3524193.1540743182,
                        3531314.663170731,
                        3532453.3325443356,
                        3520395.64538141,
                        3474431.2865468427
                    ],
                    [
                        3440045.3067861893,
                        3333994.173460993,
                        3445535.4557112623,
                        3475555.716890452,
                        3398393.5659388755
                    ]
                ]
            },
            "secondaryMetrics": {
            }
        }],
    desktop_jdk9: [
        {
            "benchmark": "io.morethan.javabenchmarks.datastructure.ListCreationBenchmark.arrayList",
            "mode": "thrpt",
            "threads": 1,
            "forks": 2,
            "warmupIterations": 3,
            "warmupTime": "1 s",
            "warmupBatchSize": 1,
            "measurementIterations": 5,
            "measurementTime": "1 s",
            "measurementBatchSize": 1,
            "primaryMetric": {
                "score": 3467631.2300505415,
                "scoreError": 98563.33258873208,
                "scoreConfidence": [
                    3369067.897461809,
                    3566194.5626392737
                ],
                "scorePercentiles": {
                    "0.0": 3333994.173460993,
                    "50.0": 3474993.5017186473,
                    "90.0": 3532339.4656069754,
                    "95.0": 3532453.3325443356,
                    "99.0": 3532453.3325443356,
                    "99.9": 3532453.3325443356,
                    "99.99": 3532453.3325443356,
                    "99.999": 3532453.3325443356,
                    "99.9999": 3532453.3325443356,
                    "100.0": 3532453.3325443356
                },
                "scoreUnit": "ops/s",
                "rawData": [
                    [
                        3524193.1540743182,
                        3531314.663170731,
                        3532453.3325443356,
                        3520395.64538141,
                        3474431.2865468427
                    ],
                    [
                        3440045.3067861893,
                        3333994.173460993,
                        3445535.4557112623,
                        3475555.716890452,
                        3398393.5659388755
                    ]
                ]
            },
            "secondaryMetrics": {
            }
        }],
    notebook_jdk8: [
        {
            "benchmark": "io.morethan.javabenchmarks.datastructure.ListCreationBenchmark.arrayList",
            "mode": "thrpt",
            "threads": 1,
            "forks": 2,
            "warmupIterations": 3,
            "warmupTime": "1 s",
            "warmupBatchSize": 1,
            "measurementIterations": 5,
            "measurementTime": "1 s",
            "measurementBatchSize": 1,
            "primaryMetric": {
                "score": 3467631.2300505415,
                "scoreError": 98563.33258873208,
                "scoreConfidence": [
                    3369067.897461809,
                    3566194.5626392737
                ],
                "scorePercentiles": {
                    "0.0": 3333994.173460993,
                    "50.0": 3474993.5017186473,
                    "90.0": 3532339.4656069754,
                    "95.0": 3532453.3325443356,
                    "99.0": 3532453.3325443356,
                    "99.9": 3532453.3325443356,
                    "99.99": 3532453.3325443356,
                    "99.999": 3532453.3325443356,
                    "99.9999": 3532453.3325443356,
                    "100.0": 3532453.3325443356
                },
                "scoreUnit": "ops/s",
                "rawData": [
                    [
                        3524193.1540743182,
                        3531314.663170731,
                        3532453.3325443356,
                        3520395.64538141,
                        3474431.2865468427
                    ],
                    [
                        3440045.3067861893,
                        3333994.173460993,
                        3445535.4557112623,
                        3475555.716890452,
                        3398393.5659388755
                    ]
                ]
            },
            "secondaryMetrics": {
            }
        }],
    notebook_jdk9: [
        {
            "benchmark": "io.morethan.javabenchmarks.datastructure.ListCreationBenchmark.arrayList",
            "mode": "thrpt",
            "threads": 1,
            "forks": 2,
            "warmupIterations": 3,
            "warmupTime": "1 s",
            "warmupBatchSize": 1,
            "measurementIterations": 5,
            "measurementTime": "1 s",
            "measurementBatchSize": 1,
            "primaryMetric": {
                "score": 3467631.2300505415,
                "scoreError": 98563.33258873208,
                "scoreConfidence": [
                    3369067.897461809,
                    3566194.5626392737
                ],
                "scorePercentiles": {
                    "0.0": 3333994.173460993,
                    "50.0": 3474993.5017186473,
                    "90.0": 3532339.4656069754,
                    "95.0": 3532453.3325443356,
                    "99.0": 3532453.3325443356,
                    "99.9": 3532453.3325443356,
                    "99.99": 3532453.3325443356,
                    "99.999": 3532453.3325443356,
                    "99.9999": 3532453.3325443356,
                    "100.0": 3532453.3325443356
                },
                "scoreUnit": "ops/s",
                "rawData": [
                    [
                        3524193.1540743182,
                        3531314.663170731,
                        3532453.3325443356,
                        3520395.64538141,
                        3474431.2865468427
                    ],
                    [
                        3440045.3067861893,
                        3333994.173460993,
                        3445535.4557112623,
                        3475555.716890452,
                        3398393.5659388755
                    ]
                ]
            },
            "secondaryMetrics": {
            }
        }]
};