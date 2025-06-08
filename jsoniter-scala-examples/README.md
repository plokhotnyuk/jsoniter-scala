# Examples of jsoniter-scala usage

## Getting started (example01)

Here you will learn how to build and run the simplest example and measure the startup time on Linux using 
Debian packages

### Build uber jar, print its size, and measure its start up time (tested with Oracle GraalVM 25-dev)

```sh
sudo apt install linux-tools-common linux-tools-generic
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --assembly example01.sc --force -o example01.jar
ls -l ./example01.jar
perf stat -r 100 java --sun-misc-unsafe-memory-access=allow -jar ./example01.jar > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01.jar' (100 runs):

            205.66 msec task-clock                       #    2.215 CPUs utilized               ( +-  0.30% )
             1,624      context-switches                 #    7.897 K/sec                       ( +-  0.83% )
                17      cpu-migrations                   #   82.662 /sec                        ( +-  1.67% )
            23,158      page-faults                      #  112.605 K/sec                       ( +-  0.09% )
     1,195,984,140      cpu_atom/instructions/           #    1.57  insn per cycle              ( +-  1.24% )
       906,422,673      cpu_core/instructions/           #    1.77  insn per cycle              ( +-  2.94% )  (39.78%)
       762,329,450      cpu_atom/cycles/                 #    3.707 GHz                         ( +-  1.34% )
       513,382,841      cpu_core/cycles/                 #    2.496 GHz                         ( +-  3.75% )  (39.78%)
       237,678,684      cpu_atom/branches/               #    1.156 G/sec                       ( +-  1.24% )
       174,316,667      cpu_core/branches/               #  847.610 M/sec                       ( +-  3.18% )  (39.78%)
         6,725,338      cpu_atom/branch-misses/          #    2.83% of all branches             ( +-  1.78% )
         5,919,295      cpu_core/branch-misses/          #    3.40% of all branches             ( +-  4.63% )  (39.78%)

          0.092830 +- 0.000511 seconds time elapsed  ( +-  0.55% )
```

### Build Scala JS output, print its size, and measure its start up time with `node` (tested with node.js 22)

```sh
curl https://raw.githubusercontent.com/creationix/nvm/master/install.sh | bash 
source ~/.bashrc
nvm install 22
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --js --js-mode release example01.sc --force -o example01.js
ls -l ./example01.js
perf stat -r 100 node ./example01.js > /dev/null
```
Expected output:
```text
 Performance counter stats for 'node ./example01.js' (100 runs):

             17.64 msec task-clock                       #    0.994 CPUs utilized               ( +-  0.14% )
                18      context-switches                 #    1.021 K/sec                       ( +-  1.43% )
                 2      cpu-migrations                   #  113.401 /sec                        ( +-  2.30% )
             2,760      page-faults                      #  156.494 K/sec                       ( +-  0.01% )
       170,140,863      cpu_atom/instructions/           #    2.12  insn per cycle              ( +-  0.01% )
     <not counted>      cpu_core/instructions/                                                  ( +- 26.12% )  (0.00%)
        80,128,642      cpu_atom/cycles/                 #    4.543 GHz                         ( +-  0.14% )
     <not counted>      cpu_core/cycles/                                                        ( +- 26.44% )  (0.00%)
        28,956,452      cpu_atom/branches/               #    1.642 G/sec                       ( +-  0.01% )
     <not counted>      cpu_core/branches/                                                      ( +- 25.84% )  (0.00%)
           560,195      cpu_atom/branch-misses/          #    1.93% of all branches             ( +-  0.04% )
     <not counted>      cpu_core/branch-misses/                                                 ( +- 24.67% )  (0.00%)

         0.0177346 +- 0.0000278 seconds time elapsed  ( +-  0.16% )
```

### Build Scala JS Wasm output, print its size, and measure its start up time with `node` (tested with node.js 22)

```sh
curl https://raw.githubusercontent.com/creationix/nvm/master/install.sh | bash 
source ~/.bashrc
nvm install 22
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --js --js-mode release --js-emit-wasm --js-module-kind es --js-module-split-style fewestmodules example01.sc --force
ls -l ./example01.js/
perf stat -r 100 node --experimental-wasm-exnref ./example01.js/main.js > /dev/null
```

Expected output:
```text
 Performance counter stats for 'node --experimental-wasm-exnref ./example01.js/main.js' (100 runs):

             27.40 msec task-clock                       #    1.023 CPUs utilized               ( +-  0.11% )
                57      context-switches                 #    2.080 K/sec                       ( +-  0.54% )
                 3      cpu-migrations                   #  109.497 /sec                        ( +-  3.18% )
             3,187      page-faults                      #  116.323 K/sec                       ( +-  0.01% )
       242,622,105      cpu_atom/instructions/           #    1.95  insn per cycle              ( +-  0.01% )
     <not counted>      cpu_core/instructions/                                                  ( +- 30.95% )  (0.00%)
       124,113,120      cpu_atom/cycles/                 #    4.530 GHz                         ( +-  0.10% )
     <not counted>      cpu_core/cycles/                                                        ( +- 30.29% )  (0.00%)
        42,063,607      cpu_atom/branches/               #    1.535 G/sec                       ( +-  0.01% )
     <not counted>      cpu_core/branches/                                                      ( +- 31.04% )  (0.00%)
         1,072,114      cpu_atom/branch-misses/          #    2.55% of all branches             ( +-  0.04% )
     <not counted>      cpu_core/branch-misses/                                                 ( +- 29.68% )  (0.00%)

         0.0267775 +- 0.0000388 seconds time elapsed  ( +-  0.14% )
```

### Build GraalVM native image, print its size, and measure its start up time (tested with Oracle GraalVM 24)

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --graalvm-jvm-id graalvm-oracle:24 --native-image example01.sc --force -o example01_graalvm.bin -- --no-fallback -O3 -H:+UnlockExperimentalVMOptions -R:MaxHeapSize=16m -H:-GenLoopSafepoints -H:-ParseRuntimeOptions -H:-IncludeMethodData --initialize-at-build-time
ls -l ./example01_graalvm.bin
perf stat -r 100 ./example01_graalvm.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_graalvm.bin' (100 runs):

              1.01 msec task-clock                       #    0.893 CPUs utilized               ( +-  0.46% )
                 1      context-switches                 #  988.945 /sec                        ( +-  4.40% )
                 0      cpu-migrations                   #    0.000 /sec                      
               470      page-faults                      #  464.804 K/sec                       ( +-  0.01% )
         7,312,659      cpu_atom/instructions/           #    1.59  insn per cycle              ( +-  0.10% )
     <not counted>      cpu_core/instructions/                                                  (0.00%)
         4,591,123      cpu_atom/cycles/                 #    4.540 GHz                         ( +-  0.37% )
     <not counted>      cpu_core/cycles/                                                        (0.00%)
         1,319,275      cpu_atom/branches/               #    1.305 G/sec                       ( +-  0.09% )
     <not counted>      cpu_core/branches/                                                      (0.00%)
            10,884      cpu_atom/branch-misses/          #    0.82% of all branches             ( +-  1.27% )
     <not counted>      cpu_core/branch-misses/                                                 (0.00%)

        0.00113238 +- 0.00000577 seconds time elapsed  ( +-  0.51% )
```

### Build Scala Native image, print its size, and measure its start up time (tested with Scala Native 0.5.7)

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --native-version 0.5.8 --native example01.sc --native-mode release-full --force -o example01_native.bin
ls -l ./example01_native.bin
perf stat -r 100 ./example01_native.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_native.bin' (100 runs):

              0.81 msec task-clock                       #    0.801 CPUs utilized               ( +-  0.22% )
                 0      context-switches                 #    0.000 /sec                      
                 0      cpu-migrations                   #    0.000 /sec                      
               732      page-faults                      #  900.150 K/sec                       ( +-  0.02% )
         5,950,991      cpu_atom/instructions/           #    1.60  insn per cycle              ( +-  0.08% )
     <not counted>      cpu_core/instructions/                                                  (0.00%)
         3,728,384      cpu_atom/cycles/                 #    4.585 GHz                         ( +-  0.22% )
     <not counted>      cpu_core/cycles/                                                        (0.00%)
         1,062,936      cpu_atom/branches/               #    1.307 G/sec                       ( +-  0.08% )
     <not counted>      cpu_core/branches/                                                      (0.00%)
             7,488      cpu_atom/branch-misses/          #    0.70% of all branches             ( +-  1.09% )
     <not counted>      cpu_core/branch-misses/                                                 (0.00%)

        0.00101568 +- 0.00000732 seconds time elapsed  ( +-  0.72% )
```

## RFC-8259 validation (example02)

An example of a command line application that reads the system input, parses and validates JSON according to the latest
specification and in case of any error prints it to the system error output.

Scala.js build is missing because it doesn't support reading from the system input.

No GC options are used to speed up JSON validation for all supported builds.

Also, this script was tested with large (megabytes) and huge (gigabytes) inputs that could be downloaded and unpacked using 
following commands:
```sh
wget https://raw.githubusercontent.com/json-iterator/test-data/master/large-file.json
wget https://webtpa-public-access.s3.us-west-2.amazonaws.com/subfolder/2023_06_430_65B0_in_network_rates.json.gz
gunzip 2023_06_430_65B0_in_network_rates.json.gz
```

Speed of validation highly depends on performance of file reading on the particular disk system. Use the following command
to measure reading speed on Linux:
```sh
sudo hdparm -Tt /dev/nvme0n1p4
```
Here is an example of expected output:
```text
/dev/nvme0n1p4:
 Timing cached reads:   50804 MB in  2.00 seconds = 25445.50 MB/sec
 Timing buffered disk reads: 8678 MB in  3.00 seconds = 2892.53 MB/sec
```

### Build uber jar, print its size, and measure its running time (tested with Oracle GraalVM 25-dev)

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m36.840s
user	0m34.748s
sys	0m2.362s
```

### Build GraalVM native image, print its size, and measure its running time (tested with Oracle GraalVM 24/23)

```sh
scala-cli --power package --graalvm-jvm-id graalvm-oracle:24 --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon -O3 -H:+UnlockExperimentalVMOptions -R:MaxHeapSize=16m -H:-GenLoopSafepoints -H:-ParseRuntimeOptions -H:-IncludeMethodData --initialize-at-build-time
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m45.922s
user	0m43.495s
sys	0m2.424s
```

### Build Scala Native image, print its size, and measure its running time

```sh
scala-cli --power package --native-version 0.5.8 --native example02.sc --native-mode release-full --native-gc none --native-lto thin --native-multithreading=false --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m49.766s
user	0m47.408s
sys	0m2.355s
```
