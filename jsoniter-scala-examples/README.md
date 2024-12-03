# Examples of jsoniter-scala usage

## Getting started (example01)

Here you will learn how to build and run the simplest example and measure the startup time on Linux using 
Debian packages

### Build uber jar, print its size, and measure its start up time (tested with Oracle GraalVM 24-dev)

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

            275.05 msec task-clock                       #    2.067 CPUs utilized               ( +-  0.19% )
             1,970      context-switches                 #    7.162 K/sec                       ( +-  0.48% )
                15      cpu-migrations                   #   54.536 /sec                        ( +-  2.23% )
            22,159      page-faults                      #   80.565 K/sec                       ( +-  0.20% )
     1,101,372,203      cycles                           #    4.004 GHz                         ( +-  0.20% )
     1,504,873,660      instructions                     #    1.37  insn per cycle              ( +-  0.10% )
       297,952,673      branches                         #    1.083 G/sec                       ( +-  0.10% )
         9,110,277      branch-misses                    #    3.06% of all branches             ( +-  0.11% )
                        TopdownL1                 #     14.1 %  tma_backend_bound      
                                                  #     35.6 %  tma_bad_speculation    
                                                  #     30.0 %  tma_frontend_bound     
                                                  #     20.3 %  tma_retiring             ( +-  0.13% )

          0.133067 +- 0.000190 seconds time elapsed  ( +-  0.14% )
```

### Build Scala JS output, print its size, and measure its start up time with `node`

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

             21.58 msec task-clock                       #    0.996 CPUs utilized               ( +-  0.16% )
                19      context-switches                 #  880.402 /sec                        ( +-  1.65% )
                 0      cpu-migrations                   #    0.000 /sec                      
             2,705      page-faults                      #  125.341 K/sec                       ( +-  0.04% )
        94,278,347      cycles                           #    4.369 GHz                         ( +-  0.14% )
       168,171,694      instructions                     #    1.78  insn per cycle              ( +-  0.01% )
        28,793,274      branches                         #    1.334 G/sec                       ( +-  0.02% )
           580,994      branch-misses                    #    2.02% of all branches             ( +-  0.06% )
                        TopdownL1                 #     17.7 %  tma_backend_bound      
                                                  #     14.4 %  tma_bad_speculation    
                                                  #     31.8 %  tma_frontend_bound     
                                                  #     36.2 %  tma_retiring             ( +-  0.14% )

         0.0216665 +- 0.0000338 seconds time elapsed  ( +-  0.16% )
```

### Build Scala JS Wasm output, print its size, and measure its start up time with `node`

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

             33.51 msec task-clock                       #    0.997 CPUs utilized               ( +-  0.15% )
                66      context-switches                 #    1.970 K/sec                       ( +-  0.44% )
                 0      cpu-migrations                   #    0.000 /sec                      
             3,102      page-faults                      #   92.567 K/sec                       ( +-  0.04% )
       146,180,631      cycles                           #    4.362 GHz                         ( +-  0.13% )
       239,361,867      instructions                     #    1.64  insn per cycle              ( +-  0.01% )
        41,631,362      branches                         #    1.242 G/sec                       ( +-  0.01% )
         1,112,970      branch-misses                    #    2.67% of all branches             ( +-  0.04% )
                        TopdownL1                 #     14.6 %  tma_backend_bound      
                                                  #     23.2 %  tma_bad_speculation    
                                                  #     32.0 %  tma_frontend_bound     
                                                  #     30.3 %  tma_retiring             ( +-  0.12% )

         0.0336197 +- 0.0000488 seconds time elapsed  ( +-  0.15% )
```

### Build GraalVM native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --graalvm-jvm-id graalvm-oracle:23 --native-image example01.sc --force -o example01_graalvm.bin -- --no-fallback -O3
ls -l ./example01_graalvm.bin
perf stat -r 100 ./example01_graalvm.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_graalvm.bin' (100 runs):

              1.84 msec task-clock                       #    0.939 CPUs utilized               ( +-  0.40% )
                 1      context-switches                 #  542.721 /sec                        ( +-  3.21% )
                 0      cpu-migrations                   #    0.000 /sec                      
               633      page-faults                      #  343.543 K/sec                       ( +-  0.01% )
         7,594,064      cycles                           #    4.121 GHz                         ( +-  0.49% )
        11,150,492      instructions                     #    1.47  insn per cycle              ( +-  0.04% )
         2,133,178      branches                         #    1.158 G/sec                       ( +-  0.03% )
            21,759      branch-misses                    #    1.02% of all branches             ( +-  0.84% )
                        TopdownL1                 #     32.0 %  tma_backend_bound      
                                                  #      6.5 %  tma_bad_speculation    
                                                  #     30.0 %  tma_frontend_bound     
                                                  #     31.5 %  tma_retiring             ( +-  0.49% )

        0.00196304 +- 0.00000988 seconds time elapsed  ( +-  0.50% )
```

### Build Scala Native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --native-version 0.5.5 --native example01.sc --native-mode release-full --force -o example01_native.bin
ls -l ./example01_native.bin
perf stat -r 100 ./example01_native.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_native.bin' (100 runs):

              0.80 msec task-clock                       #    0.823 CPUs utilized               ( +-  0.41% )
                 0      context-switches                 #    0.000 /sec                      
                 0      cpu-migrations                   #    0.000 /sec                      
               246      page-faults                      #  306.115 K/sec                       ( +-  0.04% )
         3,345,318      cycles                           #    4.163 GHz                         ( +-  0.49% )
         5,077,992      instructions                     #    1.52  insn per cycle              ( +-  0.05% )
           924,515      branches                         #    1.150 G/sec                       ( +-  0.05% )
            16,240      branch-misses                    #    1.76% of all branches             ( +-  0.44% )
                        TopdownL1                 #     26.8 %  tma_backend_bound      
                                                  #     11.7 %  tma_bad_speculation    
                                                  #     29.4 %  tma_frontend_bound     
                                                  #     32.1 %  tma_retiring             ( +-  0.48% )

        0.00097599 +- 0.00000574 seconds time elapsed  ( +-  0.59% )
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
sudo hdparm -Tt /dev/sda
```
Here is an example of expected output:
```text
/dev/sda:
 Timing cached reads:   45532 MB in  2.00 seconds = 22806.82 MB/sec
 Timing buffered disk reads: 2180 MB in  3.04 seconds = 716.07 MB/sec
```

### Build uber jar, print its size, and measure its running time (tested with Oracle GraalVM 24-dev)

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m8.991s
user	1m5.239s
sys	0m4.102s
```

### Build GraalVM native image, print its size, and measure its running time

```sh
scala-cli --power package --graalvm-jvm-id graalvm-oracle:23 --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon -O3
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m34.353s
user	1m29.199s
sys	0m5.151s
```

### Build Scala Native image, print its size, and measure its running time

```sh
scala-cli --power package --native-version 0.5.5 --native example02.sc --native-mode release-full --native-gc none --native-lto thin --native-multithreading=false --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m27.275s
user	1m23.233s
sys	0m4.038s
```
