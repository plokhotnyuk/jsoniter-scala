# Examples of jsoniter-scala usage

## Getting started (example01)

Here you will learn how to build and run the simplest example and measure the startup time on Linux using 
Debian packages

### Build uber jar, print its size, and measure its start up time

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

            164,40 msec task-clock                       #    1,532 CPUs utilized               ( +-  0,18% )
               823      context-switches                 #    5,006 K/sec                       ( +-  0,29% )
                14      cpu-migrations                   #   85,160 /sec                        ( +-  2,47% )
            10 073      page-faults                      #   61,273 K/sec                       ( +-  0,06% )
       739 505 378      cycles                           #    4,498 GHz                         ( +-  0,20% )
       911 793 014      instructions                     #    1,23  insn per cycle              ( +-  0,06% )
       179 997 789      branches                         #    1,095 G/sec                       ( +-  0,06% )
         7 171 265      branch-misses                    #    3,98% of all branches             ( +-  0,11% )
                        TopdownL1                 #     13,5 %  tma_backend_bound      
                                                  #     34,9 %  tma_bad_speculation    
                                                  #     32,5 %  tma_frontend_bound     
                                                  #     19,0 %  tma_retiring             ( +-  0,10% )

          0,107341 +- 0,000322 seconds time elapsed  ( +-  0,30% )
```

### Build Scala JS output, print its size, and measure its start up time with `node`

```sh
curl https://raw.githubusercontent.com/creationix/nvm/master/install.sh | bash 
source ~/.bashrc
nvm install 16
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --js --js-mode release example01.sc --force -o example01.js
ls -l ./example01.js
perf stat -r 100 node ./example01.js > /dev/null
```
Expected output:
```text
 Performance counter stats for 'node ./example01.js' (100 runs):

             31,00 msec task-clock                       #    0,990 CPUs utilized               ( +-  0,25% )
                25      context-switches                 #  806,523 /sec                        ( +- 10,21% )
                 1      cpu-migrations                   #   32,261 /sec                        ( +- 13,03% )
             3 361      page-faults                      #  108,429 K/sec                       ( +-  0,03% )
       145 717 779      cycles                           #    4,701 GHz                         ( +-  0,22% )
       231 347 563      instructions                     #    1,59  insn per cycle              ( +-  0,15% )
        40 439 616      branches                         #    1,305 G/sec                       ( +-  0,15% )
         1 344 209      branch-misses                    #    3,32% of all branches             ( +-  0,05% )
                        TopdownL1                 #     14,2 %  tma_backend_bound      
                                                  #     20,0 %  tma_bad_speculation    
                                                  #     34,7 %  tma_frontend_bound     
                                                  #     31,1 %  tma_retiring             ( +-  0,20% )

          0,031317 +- 0,000465 seconds time elapsed  ( +-  1,49% )
```

### Build GraalVM native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --graalvm-jvm-id graalvm-java21:21.0.1 --native-image example01.sc --force -o example01_graalvm.bin -- --no-fallback
ls -l ./example01_graalvm.bin
perf stat -r 100 ./example01_graalvm.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_graalvm.bin' (100 runs):

              1,93 msec task-clock                       #    0,898 CPUs utilized               ( +-  1,11% )
                10      context-switches                 #    5,170 K/sec                       ( +-  7,95% )
                 0      cpu-migrations                   #    0,000 /sec                      
               703      page-faults                      #  363,484 K/sec                       ( +-  0,04% )
         8 279 606      cycles                           #    4,281 GHz                         ( +-  1,23% )
        12 248 386      instructions                     #    1,48  insn per cycle              ( +-  1,00% )
         2 378 672      branches                         #    1,230 G/sec                       ( +-  0,88% )
            24 150      branch-misses                    #    1,02% of all branches             ( +-  0,71% )
                        TopdownL1                 #     30,1 %  tma_backend_bound      
                                                  #      8,9 %  tma_bad_speculation    
                                                  #     29,9 %  tma_frontend_bound     
                                                  #     31,1 %  tma_retiring             ( +-  1,23% )

          0,002154 +- 0,000177 seconds time elapsed  ( +-  8,20% )
```

### Build Scala Native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --native-version 0.5.4 --native example01.sc --native-mode release-full --force -o example01_native.bin
ls -l ./example01_native.bin
perf stat -r 100 ./example01_native.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_native.bin' (100 runs):

              0,77 msec task-clock                       #    0,766 CPUs utilized               ( +-  0,88% )
                 0      context-switches                 #    0,000 /sec                      
                 0      cpu-migrations                   #    0,000 /sec                      
               258      page-faults                      #  333,144 K/sec                       ( +-  0,08% )
         3 381 379      cycles                           #    4,366 GHz                         ( +-  0,96% )
         5 367 398      instructions                     #    1,59  insn per cycle              ( +-  0,59% )
           972 469      branches                         #    1,256 G/sec                       ( +-  0,56% )
            16 431      branch-misses                    #    1,69% of all branches             ( +-  0,49% )
                        TopdownL1                 #     25,0 %  tma_backend_bound      
                                                  #     11,8 %  tma_bad_speculation    
                                                  #     29,5 %  tma_frontend_bound     
                                                  #     33,7 %  tma_retiring             ( +-  0,95% )

          0,001011 +- 0,000114 seconds time elapsed  ( +- 11,23% )
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

### Build uber jar, print its size, and measure its running time

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m6,675s
user	1m3,442s
sys	0m3,448s
```

### Build GraalVM native image, print its size, and measure its running time

```sh
scala-cli --power package --graalvm-jvm-id graalvm-java21:21.0.1 --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m50,180s
user	1m46,471s
sys	0m3,436s
```

### Build Scala Native image, print its size, and measure its running time

```sh
scala-cli --power package --native-version 0.5.4 --native example02.sc --native-mode release-full --native-gc none --native-lto thin --native-multithreading=false --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m27,921s
user	1m22,861s
sys	0m4,003s
```
