package edu.washington.nsre.extraction;

import java.util.ArrayList;
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
import edu.washington.nsre.stanfordtools.*;
import edu.washington.nsre.util.*;

public class BuildExtractorLR {
	static Gson gson = new Gson();

	public static void buildExtractorLR(String input_generated,
			String input_tuple_test,
			String output_extraction
			) {
		StanfordRegression sr = new StanfordRegression();
		List<String> training_instances = new ArrayList<String>();
		List<HashMap<String, Double>> training_features = new ArrayList<HashMap<String, Double>>();
		List<String> training_labels = new ArrayList<String>();
		DR dr = new DR(input_generated);
		String[] l;
		// DW dw = new DW(output);
		HashMultimap<String, String> possibleEvents = HashMultimap.create();
		while ((l = dr.read()) != null) {
			if (l[0].equals("ENDCANDIDATE"))
				break;
		}
		while ((l = dr.read()) != null) {
			EventType eventtype = new EventType(l[0]);
			Tuple t = gson.fromJson(l[3], Tuple.class);
			String tupleName = l[2];
			possibleEvents.put(eventtype.arg1type + "|" + l[1] + "|" + eventtype.arg2type,
					eventtype.str);
			// for (String f : fts.keySet()) {
			//
			// }
			HashMap<String, Double> fts = new HashMap<String, Double>();
			// List<String> fts = new ArrayList<String>();
			for (String pattern : t.getPatternList()) {
				String f = eventtype.arg1type + "|" + pattern + "|" + eventtype.arg2type;
				fts.put(f, 1.0);
			}
			training_instances.add(tupleName);
			training_features.add(fts);
			training_labels.add(eventtype.str);

		}
		dr.close();
		sr.trainRVF(training_features, training_labels);
		sr.saveModel(output_extraction + ".model");
		// List<String> testing_instances = new ArrayList<String>();
		// List<HashMap<String, Double>> testing_features = new
		// ArrayList<HashMap<String, Double>>();
		// List<String> testing_labels = new ArrayList<String>();
		dr = new DR(input_tuple_test);
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

	public static void main(String[] args) {
		buildExtractorLR(args[0], args[1], args[2]);
	}
}
