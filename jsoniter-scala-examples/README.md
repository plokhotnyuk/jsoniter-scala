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
perf stat -r 100 ./example01.jar > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01.jar' (100 runs):

            202.75 msec task-clock                       #    2.286 CPUs utilized               ( +-  0.27% )
             2,191      context-switches                 #   10.806 K/sec                       ( +-  1.53% )
                39      cpu-migrations                   #  192.356 /sec                        ( +-  2.49% )
            24,775      page-faults                      #  122.195 K/sec                       ( +-  0.27% )
       303,999,880      cpu_atom/cycles/                 #    1.499 GHz                         ( +-  3.51% )  (71.71%)
     1,005,678,639      cpu_core/cycles/                 #    4.960 GHz                         ( +-  0.44% )  (77.24%)
       431,508,692      cpu_atom/instructions/           #    1.42  insn per cycle              ( +-  3.69% )  (71.71%)
     1,684,651,998      cpu_core/instructions/           #    5.54  insn per cycle              ( +-  0.47% )  (77.24%)
        83,740,906      cpu_atom/branches/               #  413.027 M/sec                       ( +-  3.78% )  (71.71%)
       335,230,949      cpu_core/branches/               #    1.653 G/sec                       ( +-  0.49% )  (77.24%)
         2,017,405      cpu_atom/branch-misses/          #    2.41% of all branches             ( +-  4.20% )  (71.71%)
         9,450,892      cpu_core/branch-misses/          #   11.29% of all branches             ( +-  0.47% )  (77.24%)

          0.088700 +- 0.000135 seconds time elapsed  ( +-  0.15% )
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

             14.90 msec task-clock                       #    1.014 CPUs utilized               ( +-  0.30% )
                20      context-switches                 #    1.343 K/sec                       ( +-  1.60% )
                 2      cpu-migrations                   #  134.254 /sec                        ( +-  5.08% )
             2,748      page-faults                      #  184.466 K/sec                       ( +-  0.00% )
        45,371,808      cpu_atom/cycles/                 #    3.046 GHz                         ( +-  2.61% )  (20.12%)
        75,772,761      cpu_core/cycles/                 #    5.086 GHz                         ( +-  0.32% )  (85.22%)
        97,892,289      cpu_atom/instructions/           #    2.16  insn per cycle              ( +-  3.38% )  (20.12%)
       165,775,873      cpu_core/instructions/           #    3.65  insn per cycle              ( +-  0.29% )  (85.22%)
        16,573,542      cpu_atom/branches/               #    1.113 G/sec                       ( +-  3.35% )  (20.12%)
        28,245,004      cpu_core/branches/               #    1.896 G/sec                       ( +-  0.27% )  (85.22%)
            97,278      cpu_atom/branch-misses/          #    0.59% of all branches             ( +- 11.08% )  (20.12%)
           573,362      cpu_core/branch-misses/          #    3.46% of all branches             ( +-  0.72% )  (85.22%)

         0.0146864 +- 0.0000435 seconds time elapsed  ( +-  0.30% )
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

             23.36 msec task-clock                       #    1.036 CPUs utilized               ( +-  0.23% )
                61      context-switches                 #    2.611 K/sec                       ( +-  0.47% )
                 3      cpu-migrations                   #  128.413 /sec                        ( +-  4.53% )
             3,156      page-faults                      #  135.090 K/sec                       ( +-  0.01% )
        81,924,942      cpu_atom/cycles/                 #    3.507 GHz                         ( +-  0.93% )  (10.81%)
       115,425,581      cpu_core/cycles/                 #    4.941 GHz                         ( +-  0.30% )
       151,891,461      cpu_atom/instructions/           #    1.85  insn per cycle              ( +-  1.65% )  (10.81%)
       228,699,784      cpu_core/instructions/           #    2.79  insn per cycle              ( +-  0.23% )
        26,745,975      cpu_atom/branches/               #    1.145 G/sec                       ( +-  1.49% )  (10.81%)
        39,592,483      cpu_core/branches/               #    1.695 G/sec                       ( +-  0.24% )
           459,225      cpu_atom/branch-misses/          #    1.72% of all branches             ( +-  1.76% )  (10.81%)
         1,062,312      cpu_core/branch-misses/          #    3.97% of all branches             ( +-  0.30% )

         0.0225519 +- 0.0000530 seconds time elapsed  ( +-  0.23% )
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

              1.08 msec task-clock                       #    0.911 CPUs utilized               ( +-  0.73% )
                 1      context-switches                 #  927.547 /sec                        ( +-  4.79% )
                 0      cpu-migrations                   #    0.000 /sec                      
               477      page-faults                      #  442.440 K/sec                       ( +-  0.01% )
         4,315,458      cpu_atom/cycles/                 #    4.003 GHz                         ( +-  1.85% )  (19.75%)
         3,872,466      cpu_core/cycles/                 #    3.592 GHz                         ( +-  5.11% )
        10,829,542      cpu_atom/instructions/           #    2.51  insn per cycle              ( +-  2.48% )  (19.75%)
         5,351,563      cpu_core/instructions/           #    1.24  insn per cycle              ( +-  5.21% )
         1,856,118      cpu_atom/branches/               #    1.722 G/sec                       ( +-  2.33% )  (19.75%)
         1,000,400      cpu_core/branches/               #  927.918 M/sec                       ( +-  5.18% )
             7,651      cpu_atom/branch-misses/          #    0.41% of all branches             ( +-  6.90% )  (19.75%)
            11,792      cpu_core/branch-misses/          #    0.64% of all branches             ( +-  5.28% )

        0.00118361 +- 0.00000667 seconds time elapsed  ( +-  0.56% )
```

### Build Scala Native image, print its size, and measure its start up time (tested with Scala Native 0.5.7)

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --native-version 0.5.7 --native example01.sc --native-mode release-full --force -o example01_native.bin
ls -l ./example01_native.bin
perf stat -r 100 ./example01_native.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_native.bin' (100 runs):

              0.71 msec task-clock                       #    0.855 CPUs utilized               ( +-  0.64% )
                 0      context-switches                 #    0.000 /sec                      
                 0      cpu-migrations                   #    0.000 /sec                      
               731      page-faults                      #    1.024 M/sec                       ( +-  0.01% )
     <not counted>      cpu_atom/cycles/                                                        ( +-100.00% )  (0.00%)
         3,769,012      cpu_core/cycles/                 #    5.280 GHz                         ( +-  1.17% )
     <not counted>      cpu_atom/instructions/                                                  ( +-100.00% )  (0.00%)
         5,907,572      cpu_core/instructions/           #  139.11  insn per cycle              ( +-  1.02% )
     <not counted>      cpu_atom/branches/                                                      ( +-100.01% )  (0.00%)
         1,055,882      cpu_core/branches/               #    1.479 G/sec                       ( +-  1.02% )
     <not counted>      cpu_atom/branch-misses/                                                 ( +-100.62% )  (0.00%)
             9,104      cpu_core/branch-misses/          #   84.85% of all branches             ( +-  1.35% )

        0.00083490 +- 0.00000519 seconds time elapsed  ( +-  0.62% )
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
 Timing cached reads:   48588 MB in  2.00 seconds = 24334.91 MB/sec
 Timing buffered disk reads: 4456 MB in  3.00 seconds = 1485.08 MB/sec
```

### Build uber jar, print its size, and measure its running time (tested with Oracle GraalVM 25-dev)

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m37.615s
user	0m35.187s
sys	0m2.662s
```

### Build GraalVM native image, print its size, and measure its running time (tested with Oracle GraalVM 23)

```sh
scala-cli --power package --graalvm-jvm-id graalvm-oracle:23 --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon -O3 -H:+UnlockExperimentalVMOptions -R:MaxHeapSize=16m -H:-GenLoopSafepoints -H:-ParseRuntimeOptions -H:-IncludeMethodData --initialize-at-build-time
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m44.963s
user	0m42.471s
sys	0m2.486s
```

You can use profile guided optimization (PGO) to improve performance of Oracle GraalVM native image, for that you need:
- build an instrumented GraalVM native image with `--pgo-instrument` option added:
```sh
scala-cli --power package --graalvm-jvm-id graalvm-oracle:23 --native-image example02.sc --force -o example02_graalvm_instrumented.bin -- --no-fallback --gc=epsilon -O3 --pgo-instrument -H:+UnlockExperimentalVMOptions -R:MaxHeapSize=16m -H:-GenLoopSafepoints -H:-ParseRuntimeOptions -H:-IncludeMethodData --initialize-at-build-time
ls -l ./example02_graalvm_instrumented.bin
```
- run the instrumented image and collect the profile data:
```sh
time ./example02_graalvm_instrumented.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
- build a PGO-optimized GraalVM native image with `--pgo=default.iprof` option added:
```sh
scala-cli --power package --graalvm-jvm-id graalvm-oracle:23 --native-image example02.sc --force -o example02_graalvm_optimized.bin -- --no-fallback --gc=epsilon -O3 --pgo=default.iprof -H:+UnlockExperimentalVMOptions -R:MaxHeapSize=16m -H:-GenLoopSafepoints -H:-ParseRuntimeOptions -H:-IncludeMethodData --initialize-at-build-time
ls -l ./example02_graalvm_optimized.bin
```
- run the PGO-optimized image:
```sh
time ./example02_graalvm_optimized.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m43.674s
user	0m41.213s
sys	0m2.461s
```

### Build Scala Native image, print its size, and measure its running time

```sh
scala-cli --power package --native-version 0.5.7 --native example02.sc --native-mode release-full --native-gc none --native-lto thin --native-multithreading=false --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	0m49.232s
user	0m46.838s
sys	0m2.389s
```
