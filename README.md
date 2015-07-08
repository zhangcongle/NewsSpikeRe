# NewsSpikeRe

To start with, you should install
- mosek (https://www.mosek.com/)
- java 1.8
- maven

To compile the system: 

- git clone https://github.com/zhangcongle/NewsSpikeRe.git
- cd nsre2
- wget http://www.cs.washington.edu/ai/clzhang/nsre2/data.tgz
- wget http://www.cs.washington.edu/ai/clzhang/nsre2/lib.tgz
- tar xvf data.tgz
- tar xvf lib.tgz
- cp $DYLD_LIBRARY_PATH/mosek.jar lib/mosek.jar (if you are using Linux, it should be "$LD_LIBRARY_PATH/mosek.jar"
- mvn compile

To predict relations in the parsed sentences from data/test:
- java -Xmx10g -cp target/classes:target/lib/* edu.washington.nsre.extraction.NewsSpikePredict data/test data/predict

Here are the commands to parse sentences to the same format of data/test (the output file is data/preprocess/tuples)
- java -Xmx10g -cp target/classes:target/lib/* edu.washington.nsre.figer.ParseStanfordFigerReverb data/preprocess/text data/preprocess/parsed
- java -Xmx10g -cp target/classes:target/lib/* edu.washington.nsre.figer.Parsed2Tuple data/preprocess/parsed data/preprocess/tuples

Here are the commands to 

