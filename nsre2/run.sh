#mvn exec:java -DXmx=10000m -Dexec.mainClass="edu.washington.cs.figer.FigerSystem" -Dexec.args="data/a"
CMD="java -Xmx10g -cp target/classes:target/lib/*"

# $CMD edu.washington.cs.figer.FigerSystem data/a
# $CMD edu.washington.nsre.figer.ParseStanfordFigerReverb data/preprocess/text data/preprocess/parsed
$CMD edu.washington.nsre.figer.Parsed2Tuple data/preprocess/parsed data/preprocess/tuples
# $CMD edu.washington.nsre.ilp.lo2