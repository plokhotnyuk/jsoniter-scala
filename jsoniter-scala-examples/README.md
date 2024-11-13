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

            286,59 msec task-clock                       #    1,985 CPUs utilized               ( +-  0,82% )
             1 914      context-switches                 #    6,679 K/sec                       ( +-  1,44% )
               140      cpu-migrations                   #  488,508 /sec                        ( +-  7,96% )
            19 161      page-faults                      #   66,859 K/sec                       ( +-  0,18% )
     1 171 824 946      cycles                           #    4,089 GHz                         ( +-  0,49% )
     1 419 578 393      instructions                     #    1,21  insn per cycle              ( +-  0,16% )
       281 308 247      branches                         #  981,581 M/sec                       ( +-  0,17% )
         9 408 222      branch-misses                    #    3,34% of all branches             ( +-  0,24% )
                        TopdownL1                 #     11,4 %  tma_backend_bound      
                                                  #     36,5 %  tma_bad_speculation    
                                                  #     33,2 %  tma_frontend_bound     
                                                  #     19,0 %  tma_retiring             ( +-  0,19% )

          0,144400 +- 0,000766 seconds time elapsed  ( +-  0,53% )
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

             27,18 msec task-clock                       #    0,990 CPUs utilized               ( +-  0,18% )
                22      context-switches                 #  809,497 /sec                        ( +-  5,66% )
                 1      cpu-migrations                   #   36,795 /sec                        ( +- 10,70% )
             2 803      page-faults                      #  103,137 K/sec                       ( +-  0,03% )
       125 384 201      cycles                           #    4,614 GHz                         ( +-  0,16% )
       196 598 438      instructions                     #    1,57  insn per cycle              ( +-  0,09% )
        34 031 877      branches                         #    1,252 G/sec                       ( +-  0,09% )
         1 247 743      branch-misses                    #    3,67% of all branches             ( +-  0,05% )
                        TopdownL1                 #     12,8 %  tma_backend_bound      
                                                  #     19,9 %  tma_bad_speculation    
                                                  #     36,1 %  tma_frontend_bound     
                                                  #     31,2 %  tma_retiring             ( +-  0,14% )

          0,027464 +- 0,000292 seconds time elapsed  ( +-  1,06% )
```

### Build GraalVM native image, print its size, and measure its start up time

```sh
sudo apt install linux-tools-common linux-tools-generic gcc zlib1g-dev
sudo sysctl kernel.perf_event_paranoid=1
scala-cli --power package --graalvm-jvm-id graalvm-java23:23.0.0 --native-image example01.sc --force -o example01_graalvm.bin -- --no-fallback -Os
ls -l ./example01_graalvm.bin
perf stat -r 100 ./example01_graalvm.bin > /dev/null
```
Expected output:
```text
 Performance counter stats for './example01_graalvm.bin' (100 runs):

              1,81 msec task-clock                       #    0,950 CPUs utilized               ( +-  0,29% )
                 1      context-switches                 #  551,909 /sec                        ( +-  5,14% )
                 0      cpu-migrations                   #    0,000 /sec                      
               705      page-faults                      #  389,096 K/sec                       ( +-  0,01% )
         7 795 856      cycles                           #    4,303 GHz                         ( +-  0,32% )
        11 758 377      instructions                     #    1,51  insn per cycle              ( +-  0,05% )
         2 299 367      branches                         #    1,269 G/sec                       ( +-  0,03% )
            22 831      branch-misses                    #    0,99% of all branches             ( +-  0,71% )
                        TopdownL1                 #     27,8 %  tma_backend_bound      
                                                  #      6,8 %  tma_bad_speculation    
                                                  #     32,9 %  tma_frontend_bound     
                                                  #     32,5 %  tma_retiring             ( +-  0,32% )

        0,00190782 +- 0,00000647 seconds time elapsed  ( +-  0,34% )
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

              0,74 msec task-clock                       #    0,834 CPUs utilized               ( +-  0,39% )
                 0      context-switches                 #    0,000 /sec                      
                 0      cpu-migrations                   #    0,000 /sec                      
               245      page-faults                      #  330,182 K/sec                       ( +-  0,04% )
         3 232 185      cycles                           #    4,356 GHz                         ( +-  0,41% )
         5 088 414      instructions                     #    1,57  insn per cycle              ( +-  0,05% )
           927 130      branches                         #    1,249 G/sec                       ( +-  0,06% )
            16 482      branch-misses                    #    1,78% of all branches             ( +-  0,44% )
                        TopdownL1                 #     23,2 %  tma_backend_bound      
                                                  #     12,1 %  tma_bad_speculation    
                                                  #     31,5 %  tma_frontend_bound     
                                                  #     33,2 %  tma_retiring             ( +-  0,41% )

        0,00089004 +- 0,00000503 seconds time elapsed  ( +-  0,57% )
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

### Build uber jar, print its size, and measure its running time (tested with Oracle Graal VM 24-dev)

```sh
scala-cli --power package --assembly example02.sc --force -o example02.jar
ls -l ./example02.jar
time ./example02.jar -J-XX:+UnlockExperimentalVMOptions -J-XX:+UseEpsilonGC -J-Xms8m -J-Xmx8m -J-XX:+AlwaysPreTouch < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m2,667s
user	0m59,750s
sys	0m3,171s
```

### Build GraalVM native image, print its size, and measure its running time

```sh
scala-cli --power package --graalvm-jvm-id graalvm-java23:23.0.0 --native-image example02.sc --force -o example02_graalvm.bin -- --no-fallback --gc=epsilon -Os
ls -l ./example02_graalvm.bin
time ./example02_graalvm.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m45,502s
user	1m42,411s
sys	0m3,088s
```

### Build Scala Native image, print its size, and measure its running time

```sh
scala-cli --power package --native-version 0.5.5 --native example02.sc --native-mode release-full --native-gc none --native-lto thin --native-multithreading=false --force -o example02_native.bin
ls -l ./example02_native.bin
time ./example02_native.bin < 2023_06_430_65B0_in_network_rates.json 2> /dev/null
```
Expected output:
```text
real	1m25,617s
user	1m22,498s
sys	0m3,116s
```
