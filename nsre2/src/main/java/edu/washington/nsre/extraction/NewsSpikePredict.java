package edu.washington.nsre.extraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.washington.nsre.stanfordtools.StanfordRegression;
import edu.washington.nsre.util.D;
import edu.washington.nsre.util.DR;
import edu.washington.nsre.util.DW;
import edu.washington.nsre.util.Util;

public class NewsSpikePredict {

	public static void predict(
			String input_model,
			String input_test,
			String output_extraction) {
		Map<String, Counter<String>> ftrEventMap = new HashMap<String, Counter<String>>();
		DW dw = new DW(output_extraction);
		DR dr = new DR(input_model);
		String[] l;
		HashMap<String, Set<String>> phrasestr2candidate = new HashMap<String, Set<String>>();
		while ((l = dr.read()) != null) {
			if (l[0].equals("ENDCANDIDATE"))
				break;
			phrasestr2candidate.put(l[1], gson.fromJson(l[2], Set.class));
		}

		while ((l = dr.read()) != null) {
			String eventstr = l[0];
			String ftr = l[1];
			double val = Double.parseDouble(l[2]);
			if (!ftrEventMap.containsKey(ftr)) {
				ftrEventMap.put(ftr, new ClassicCounter<String>());
			}
			ftrEventMap.get(ftr).incrementCount(eventstr, val);
		}
		dr.close();
		dr = new DR(input_test);
		while ((l = dr.read()) != null) {
			Tuple t = gson.fromJson(l[2], Tuple.class);
			String s = t.getSentence();
			// if (s.contains("Carmelo Anthony sits as")) {
			// D.p(s);
			// }
			Set<String> candidates = new HashSet<String>();
			for (String phrase : t.getPatternList()) {
				if (phrasestr2candidate.containsKey(phrase)) {
					candidates.addAll(phrasestr2candidate.get(phrase));
				}
			}
			Counter<String> dist1 = t.getArg1FineGrainedNer();
			Counter<String> dist2 = t.getArg2FineGrainedNer();
			HashMap<String, Double> fts = new HashMap<String, Double>();
			for (String a1t : dist1.keySet()) {
				for (String a2t : dist2.keySet()) {
					for (String pattern : t.getPatternList()) {
						String f = a1t + "|" + pattern + "|" + a2t;
						double fw = dist1.getCount(a1t) * dist2.getCount(a2t);
						fts.put(f, fw);
					}
					String[] wordsInShortestPath = t.wordsInShortestPath();
					if (wordsInShortestPath != null && wordsInShortestPath.length >= 2) {
						String f = a1t + "|" + t.getShortestPathFromTuple() + "|" + a2t;
						fts.put(f, 1.0);
					}
				}
			}
			Counter<String> scorer_withneg = scoresOf(ftrEventMap, fts);
			Counter<String> scorer = new ClassicCounter<String>();
			for (String eventstr : candidates) {
				double v = scorer_withneg.getCount(eventstr);
				if (v > 0)
					scorer.incrementCount(eventstr, v);
			}
			// for (String eventstr : scorer_withneg.keySet()) {
			// double v = scorer_withneg.getCount(eventstr);
			// if (v > 1) {
			// scorer.incrementCount(eventstr, v);
			// }
			// }
			Counters.normalize(scorer);
			dw.write(Util.counter2jsonstr(scorer));
		}
		dr.close();
		dw.close();
	}

	public static Counter<String> scoresOf(Map<String, Counter<String>> ftrEventMap,
			HashMap<String, Double> fts) {
		Counter<String> ret = new ClassicCounter<String>();
		for (String f : fts.keySet()) {
			double v1 = fts.get(f);
			if (ftrEventMap.containsKey(f)) {
				Counter<String> weight = ftrEventMap.get(f);
				for (String eventstr : weight.keySet()) {
					double v2 = weight.getCount(eventstr);
					double s = v1 * v2;
					ret.incrementCount(eventstr, s);
				}
			}
		}
		return ret;
	}

	public static void predict(
			String input_generated,
			String input_model,
			String input_test,
			String output_extraction
			) {
		StanfordRegression sr = new StanfordRegression();
		sr.loadModel(input_model);
		DR dr = new DR(input_generated);
		String[] l;
		// DW dw = new DW(output);
		HashMultimap<String, String> possibleEvents = HashMultimap.create();
		while ((l = dr.read()) != null) {
			EventType eventtype = new EventType(l[0]);
			Tuple t = gson.fromJson(l[3], Tuple.class);
			String tupleName = l[2];
			possibleEvents.put(eventtype.arg1type + "|" + l[1] + "|" + eventtype.arg2type,
					eventtype.str);
			HashMap<String, Double> fts = new HashMap<String, Double>();
			for (String pattern : t.getPatternList()) {
				String f = eventtype.arg1type + "|" + pattern + "|" + eventtype.arg2type;
				fts.put(f, 1.0);
			}
		}
		dr.close();

		dr = new DR(input_test);
		DW dw = new DW(output_extraction);
		while ((l = dr.read()) != null) {
			Counter<String> scorer = new ClassicCounter<String>();
			Tuple t = gson.fromJson(l[2], Tuple.class);
			String tupleName = l[2];
			Counter<String> dist1 = t.getArg1FineGrainedNer();
			Counter<String> dist2 = t.getArg2FineGrainedNer();
			Set<String> candidates = new HashSet<String>();
			HashMap<String, Double> fts = new HashMap<String, Double>();
			for (String a1t : dist1.keySet()) {
				for (String a2t : dist2.keySet()) {
					for (String pattern : t.getPatternList()) {
						String f = a1t + "|" + pattern + "|" + a2t;
						if (possibleEvents.containsKey(f)) {
							candidates.addAll(possibleEvents.get(f));
						}
						double fw = dist1.getCount(a1t) * dist2.getCount(a2t);
						fts.put(f, fw);
					}

				}
			}
			if (fts.size() > 0) {
				Map<String, Double> allscores = sr.scoreOf(fts);
				for (String r : candidates) {
					scorer.incrementCount(r, allscores.get(r));
				}
				Counters.normalize(scorer);
			}
			dw.write(Util.counter2jsonstr(scorer));
		}
		dw.close();
	}

	public static void readDump(String input_model) {
		DR dr = new DR(input_model);
		String[] l0 = dr.read();
		String[] s = l0[0].split(" ");
		dr.close();
	}

	static Gson gson = new Gson();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// String input_generated = args[0];
		try {
			Config.parseConfig();
			String inputTestFile= args[0];
			String outputPredictFile = args[1];
			predict(Config.modelFile, inputTestFile, outputPredictFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		String input_model = args[0];
//		String input_test = args[1];
//		String output_extraction = args[2];
//		predict(input_model, input_test, output_extraction);
		// readDump(input_model);
		// predict(input_generated, input_model, input_test, output_extraction);

	}

}
