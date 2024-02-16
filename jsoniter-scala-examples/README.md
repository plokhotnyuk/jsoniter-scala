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

            290.69 msec task-clock                       #    2.253 CPUs utilized               ( +-  0.29% )
             2,284      context-switches                 #    7.857 K/sec                       ( +-  0.82% )
                16      cpu-migrations                   #   55.041 /sec                        ( +-  1.88% )
            22,293      page-faults                      #   76.690 K/sec                       ( +-  0.13% )
     1,176,204,955      cycles                           #    4.046 GHz                         ( +-  0.26% )
     1,589,502,677      instructions                     #    1.35  insn per cycle              ( +-  0.14% )
       310,676,528      branches                         #    1.069 G/sec                       ( +-  0.15% )
         9,647,703      branch-misses                    #    3.11% of all branches             ( +-  0.20% )
                        TopdownL1                 #     13.4 %  tma_backend_bound      
                                                  #     35.1 %  tma_bad_speculation    
                                                  #     30.7 %  tma_frontend_bound     
                                                  #     20.8 %  tma_retiring             ( +-  0.12% )

          0.129034 +- 0.000284 seconds time elapsed  ( +-  0.22% )
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
 Performance counter stats for 'node example01.js' (100 runs):

             39.90 msec task-clock                       #    1.000 CPUs utilized               ( +-  0.14% )
                21      context-switches                 #  526.382 /sec                        ( +-  0.67% )
                 0      cpu-migrations                   #    0.000 /sec                      
             4,485      page-faults                      #  112.420 K/sec                       ( +-  0.01% )
       179,377,726      cycles                           #    4.496 GHz                         ( +-  0.09% )
       306,298,806      instructions                     #    1.71  insn per cycle              ( +-  0.01% )
        53,631,726      branches                         #    1.344 G/sec                       ( +-  0.01% )
         1,543,919      branch-misses                    #    2.88% of all branches             ( +-  0.02% )
                        TopdownL1                 #     15.0 %  tma_backend_bound      
                                                  #     19.7 %  tma_bad_speculation    
                                                  #     32.1 %  tma_frontend_bound     
                                                  #     33.3 %  tma_retiring             ( +-  0.09% )

         0.0399045 +- 0.0000686 seconds time elapsed  ( +-  0.17% )
```

### Build GraalVM native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --jvm graalvm --native-image example01.sc --force -o example01_graalvm.bin -- --no-fallback
ls -l ./example01_graalvm.bin
perf stat -r 100 ./example01_graalvm.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_graalvm.bin' (100 runs):

              2.08 msec task-clock                       #    0.952 CPUs utilized               ( +-  2.44% )
                11      context-switches                 #    5.299 K/sec                       ( +-  2.60% )
                 0      cpu-migrations                   #    0.000 /sec                      
               707      page-faults                      #  340.580 K/sec                       ( +-  0.02% )
         8,252,478      cycles                           #    3.975 GHz                         ( +-  1.12% )
        12,924,310      instructions                     #    1.57  insn per cycle              ( +-  0.07% )
         2,547,675      branches                         #    1.227 G/sec                       ( +-  0.05% )
            28,162      branch-misses                    #    1.11% of all branches             ( +- 13.33% )
                        TopdownL1                 #     28.2 %  tma_backend_bound      
                                                  #      8.9 %  tma_bad_speculation    
                                                  #     29.9 %  tma_frontend_bound     
                                                  #     33.0 %  tma_retiring             ( +-  1.11% )

         0.0021795 +- 0.0000496 seconds time elapsed  ( +-  2.28% )
```

### Build Scala Native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic clang libstdc++-12-dev libgc-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --native-version 0.4.16 --native example01.sc --force -o example01_native.bin
ls -l ./example01_native.bin
perf stat -r 100 ./example01_native.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_native.bin' (100 runs):

              0.93 msec task-clock                       #    0.839 CPUs utilized               ( +-  3.56% )
                 0      context-switches                 #    0.000 /sec                      
                 0      cpu-migrations                   #    0.000 /sec                      
               288      page-faults                      #  310.746 K/sec                       ( +-  0.04% )
         3,675,480      cycles                           #    3.966 GHz                         ( +-  0.49% )
         5,686,617      instructions                     #    1.55  insn per cycle              ( +-  0.05% )
         1,028,321      branches                         #    1.110 G/sec                       ( +-  0.05% )
            16,672      branch-misses                    #    1.62% of all branches             ( +-  0.42% )
                        TopdownL1                 #     24.7 %  tma_backend_bound      
                                                  #     11.2 %  tma_bad_speculation    
                                                  #     30.2 %  tma_frontend_bound     
                                                  #     33.9 %  tma_retiring             ( +-  0.25% )

         0.0011042 +- 0.0000401 seconds time elapsed  ( +-  3.63% )
```

## RFC-8259 validation (example02)

An example of a command line application that reads the system input, parses and validates JSON according to the latest
specification and in case of any error prints it to the system error output.

Scala.js build is missing because it doesn't support reading from the system input.

No GC options are used to speed up JSON validation for all supported builds.

### Build uber jar, print its size, and measure its start up time

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < test.json 2> /dev/null
```
Expected output:
```text
real	0m0.085s
user	0m0.110s
sys 	0m0.017s
```

### Build GraalVM native image, print its size, and measure its start up time

```sh
scala-cli --power package --jvm graalvm --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < test.json 2> /dev/null
```
Expected output:
```text
real	0m0.004s
user	0m0.000s
sys 	0m0.003s
```

### Build Scala Native image, print its size, and measure its start up time

```sh
scala-cli --power package --native-version 0.4.16 --native example02.sc --native-gc none --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < test.json 2> /dev/null
```
Expected output:
```text
real	0m0.003s
user	0m0.003s
sys 	0m0.000s
```
