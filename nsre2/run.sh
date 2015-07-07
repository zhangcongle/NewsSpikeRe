#mvn exec:java -DXmx=10000m -Dexec.mainClass="edu.washington.cs.figer.FigerSystem" -Dexec.args="data/a"
CMD="java -Xmx10g -cp target/classes:target/lib/*"

######
#Test if java, mosek are configured correctly
# $CMD edu.washington.cs.figer.FigerSystem data/a
# $CMD edu.washington.nsre.ilp.lo2
####

######
#parse a text file
# $CMD edu.washington.nsre.figer.ParseStanfordFigerReverb data/preprocess/text data/preprocess/parsed
######

######
#get argument pairs from the sentences
#$CMD edu.washington.nsre.figer.Parsed2Tuple data/preprocess/parsed data/preprocess/tuples
######


######
#Learn a relation extraction model ( the model path is set in nsre.conf)
# $CMD edu.washington.nsre.extraction.NewsSpikeCandidate
# $CMD edu.washington.nsre.extraction.NewsSpikeExtractor
######

####
#Predict the relations in data/test, write the predictions in data/predict
#$CMD edu.washington.nsre.extraction.NewsSpikePredict data/test data/predict
####

####
#evaluation
#$CMD edu.washington.nsre.extraction.Evaluate data/test data/predict data/test.labeled data/eval
####