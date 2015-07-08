# NewsSpikeRe

To start with, you should install
- mosek (https://www.mosek.com/)
- java 1.8
- maven

To compile the system: 

- git clone ...
- cd nsre2
- wget http://www.cs.washington.edu/ai/clzhang/nsre2/data.tgz
- wget http://www.cs.washington.edu/ai/clzhang/nsre2/lib.tgz
- tar xvf data.tgz
- tar xvf lib.tgz
- cp $DYLD_LIBRARY_PATH/mosek.jar lib/mosek.jar (if you are using Linux, it should be "$LD_LIBRARY_PATH/mosek.jar"
- mvn compile


