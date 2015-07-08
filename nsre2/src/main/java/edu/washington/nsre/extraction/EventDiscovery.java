package edu.washington.nsre.extraction;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.IntCounter;
import edu.washington.nsre.ilp.IntegerLinearProgramming;
import edu.washington.nsre.util.DR;
import edu.washington.nsre.util.DW;

public class EventDiscovery {

	static Gson gson = new Gson();
	public static Set<String> no = new HashSet<String>();

	static {
		String[] lightverblist = new String[] { "make", "give", "do", "be", "have", "say", "think", "ask", "announce",
				"warn", "urge", "defeat" };
		for (String v : lightverblist) {
			no.add(v);
		}
	}

	public static void candidateEventRelations(String inputParallel, int K, String output) {
		DR dr = new DR(inputParallel);
		List<String[]> b = null;
		List<String[]> tow = new ArrayList<String[]>();
		HashMultimap<String, String> rel2eecs = HashMultimap.create();
		List<String[]> rel_eec_bipartitegraph = new ArrayList<String[]>();
		Counter<String> va1a2count = new IntCounter<String>();
		while ((b = dr.readBlock(0)) != null) {
			String eecname = b.get(0)[0];
			HashMap<String, Tuple> events2sentence = new HashMap<String, Tuple>();
			List<Tuple> tuples = new ArrayList<Tuple>();
			String arg1 = null;
			String arg2 = null;
			Counter<String> dist1 = new ClassicCounter<String>();
			Counter<String> dist2 = new ClassicCounter<String>();
			Set<String> arg1types = new HashSet<String>();
			Set<String> arg2types = new HashSet<String>();

			for (String[] l : b) {
				Tuple t = gson.fromJson(l[2], Tuple.class);
				if (arg1 == null) {
					arg1 = t.getArg1();
					arg2 = t.getArg2();
				}
				Counter<String> temp1 = t.getArg1FineGrainedNer();
				Counter<String> temp2 = t.getArg2FineGrainedNer();

				// arg1types.add(Counters.argmax(temp1));
				// arg2types.add(Counters.argmax(temp2));
				arg1types.add(t.getArg1StanfordNer());
				arg2types.add(t.getArg2StanfordNer());
				Counters.addInPlace(dist1, temp1);
				Counters.addInPlace(dist2, temp2);
				tuples.add(t);
			}
			// if (dist1 == null || dist2 == null)
			// continue;
			double maxs1 = Counters.max(dist1);
			double maxs2 = Counters.max(dist2);

			for (String arg1type : dist1.keySet()) {
				double s = dist1.getCount(arg1type);
				if (s > maxs1 * 0.9) {
					arg1types.add(arg1type);
				}
			}
			for (String arg2type : dist2.keySet()) {
				double s = dist2.getCount(arg2type);
				if (s > maxs2 * 0.9) {
					arg2types.add(arg2type);
				}
			}
			Set<String> verbs = new HashSet<String>();
			for (Tuple t : tuples) {
				if (t.getRel() != null && t.getRelHead() != null) {
					String v = t.getRel();
					String h = t.getRelHead();
					if (v.startsWith(h)) {
						verbs.add(t.getRel());
					}
				}
			}

			for (String v : verbs) {
				String v0 = v.split(" ")[0];
				if (no.contains(v0)) {
					continue;
				}
				// for (String arg1type : dist1.keySet()) {
				// for (String arg2type : dist2.keySet()) {
				for (String arg1type : arg1types) {
					for (String arg2type : arg2types) {
						if (arg1type.equals("/misc") || arg2type.equals("/misc"))
							continue;
						// if (dist1.getCount(arg1type) >= maxs1 * 0.9 &&
						// dist2.getCount(arg2type) >= maxs2 * 0.9)
						{
							// va1a2count.incrementCount(v + "@" + arg1type
							// + "@" + arg2type);
							String rel = v + "@" + arg1type + "@" + arg2type;
							// rel2eecs.put(rel, arg1words[arg1words.length
							// - 1] + "\t"
							// + arg2words[arg2words.length - 1]);
							rel2eecs.put(rel, eecname);
							rel_eec_bipartitegraph.add(new String[] { "r#" + rel, "e#" + eecname });
						}
					}
				}
			}
		}
		// for debug purpose
		{
			DW dw0 = new DW(output + ".debug");
			Set<String> set1 = rel2eecs.get("beat@/organization@/organization");
			Set<String> set2 = rel2eecs.get("beat@/location@/location");
			for (String x : set1) {
				if (set2.contains(x)) {
					dw0.write(x);
				}
			}
			dw0.close();
		}
		DW dw = new DW(output);
		Set<String> valides = cover_fix_buckets(rel_eec_bipartitegraph, K);
		for (String a : valides) {
			if (a.startsWith("r#")) {
				String target = a.replace("r#", "");
				String[] abc = target.split("@");
				dw.write(target, abc[1], abc[0], abc[2]);
			}
		}
		dw.close();
	}

	// public static void candidateFluentRelations(String input_eecs, String
	// input_xiaotypes, int K, String output) {
	// HashMap<String, Counter<String>> arg2xiaotypescores = new HashMap<String,
	// Counter<String>>();
	// {
	// DR dr = new DR(input_xiaotypes);
	// String[] l;
	// while ((l = dr.read()) != null) {
	// String eec = l[0];
	// String[] abc = eec.split("@");
	// int k = Integer.parseInt(l[1]) - 1;
	// String arg = abc[k];
	// HashMap<String, Double> toprint = gson.fromJson(l[2], HashMap.class);
	// if (!arg2xiaotypescores.containsKey(arg)) {
	// arg2xiaotypescores.put(arg, Counters.fromMap(toprint));
	// }
	// }
	// dr.close();
	// }
	// {
	// DW dw1 = new DW(output);
	// // DW dwsim = new DW(output + ".sim");
	//
	// // DW dw2 = new DW(output + ".nocover");
	// HashMultimap<String, String> rel2eecs = HashMultimap.create();
	// List<String[]> rel_eec_bipartitegraph = new ArrayList<String[]>();
	// Counter<String> va1a2count = new IntCounter<String>();
	// DR dr = new DR(input_eecs);
	// String[] l;
	// while ((l = dr.read()) != null) {
	// String eecname = l[0];
	// Eec eec = gson.fromJson(l[1], Eec.class);
	// String arg1 = eec.getArg1();
	// String arg2 = eec.getArg2();
	// Counter<String> dist1 = arg2xiaotypescores.get(arg1);
	// Counter<String> dist2 = arg2xiaotypescores.get(arg2);
	// if (dist1 == null || dist2 == null)
	// continue;
	// double maxs1 = Counters.max(dist1);
	// double maxs2 = Counters.max(dist2);
	// Set<String> verbs = new HashSet<String>();
	// for (Tuple t : eec.tuples) {
	// String v = t.getRel();
	// String h = t.getRelHead();
	// if (v.startsWith(h)) {
	// verbs.add(t.getRel());
	// }
	// }
	// String[] arg1words = arg1.split(" ");
	// String[] arg2words = arg2.split(" ");
	// for (String v : verbs) {
	// String v0 = v.split(" ")[0];
	// if (no.contains(v0)) {
	// continue;
	// }
	// for (String arg1type : dist1.keySet()) {
	// for (String arg2type : dist2.keySet()) {
	// if (dist1.getCount(arg1type) >= maxs1 - 0.01 && dist2.getCount(arg2type)
	// >= maxs2 - 0.01) {
	// // va1a2count.incrementCount(v + "@" + arg1type
	// // + "@" + arg2type);
	// String rel = v + "@" + arg1type + "@" + arg2type;
	// // rel2eecs.put(rel, arg1words[arg1words.length
	// // - 1] + "\t"
	// // + arg2words[arg2words.length - 1]);
	// rel2eecs.put(rel, eecname);
	// rel_eec_bipartitegraph.add(new String[] { "r#" + rel, "e#" + eecname });
	// }
	// }
	// }
	// }
	// }
	// Set<String> valides = cover_fix_buckets(rel_eec_bipartitegraph, K);
	// for (String a : valides) {
	// if (a.startsWith("r#")) {
	// String target = a.replace("r#", "");
	// String[] abc = target.split("@");
	// dw1.write(target, abc[1], abc[0], abc[2]);
	// }
	// }
	// dw1.close();
	// }
	//
	// }

	/** Fix number of buckets */
	public static Set<String> cover_fix_buckets(List<String[]> rel_eec_bipartitegraph, int MAX_BUCKETS) {
		Set<String> picked = new HashSet<String>();
		// List<String[]> res = new ArrayList<String[]>();
		double infinity = 0;

		HashMap<String, Double> edge2score = new HashMap<String, Double>();
		// HashMultimap<String, String> bucket2edges = HashMultimap.create();
		// HashMultimap<String, String> element2edges = HashMultimap.create();
		HashMultimap<String, String> bucket2elements = HashMultimap.create();
		HashMultimap<String, String> element2bucket = HashMultimap.create();
		HashMultimap<String, String> verb2buckets = HashMultimap.create();

		for (String[] l : rel_eec_bipartitegraph) {
			String bucket = l[0];
			String element = l[1];
			bucket2elements.put(bucket, element);
			element2bucket.put(element, bucket);
			String firstverb = (bucket.split("@")[0]).split(" ")[0];
			verb2buckets.put(firstverb, bucket);
		}
		IntegerLinearProgramming ilp = new IntegerLinearProgramming();
		Counter<String> object = new ClassicCounter<String>();
		for (Entry<String, Double> e : edge2score.entrySet()) {
			String edgename = e.getKey();
			double edgescore = e.getValue();
			object.incrementCount(edgename, edgescore);
		}
		for (String element : element2bucket.keySet()) {
			String variable = element;
			object.incrementCount(variable, 1);
		}
		ilp.setObjective(object, true);
		// at most MAX_BUCKETS buckets are non-empty
		Counter<String> max_buckets = new ClassicCounter<String>();
		for (String bucket : bucket2elements.keySet()) {
			String variable = bucket;
			max_buckets.incrementCount(variable, 1.0);
		}
		ilp.addConstraint(max_buckets, false, 1.0, true, MAX_BUCKETS);
		// each ball can only appear in one bucket

		for (String element : element2bucket.keySet()) {
			Set<String> buckets = element2bucket.get(element);
			/*** the element is covered? */
			Counter<String> covered = new ClassicCounter<String>();
			covered.incrementCount(element, -1);
			for (String b : buckets) {
				covered.incrementCount(b);
			}
			ilp.addConstraint(covered, true, 0.0, false, buckets.size());
			/** the element should be covered by only one bucket */
			Counter<String> uniq = new ClassicCounter<String>();
			for (String b : buckets) {
				uniq.incrementCount(b);
			}

			ilp.addConstraint(uniq, false, 0.0, true, 1);
		}

		for (String v : verb2buckets.keySet()) {
			Set<String> buckets = verb2buckets.get(v);
			Counter<String> atmost2 = new ClassicCounter<String>();
			for (String b : buckets) {
				atmost2.incrementCount(b);
			}
			ilp.addConstraint(atmost2, false, 0.0, true, 2);

		}
		HashMap<String, Double> variable2score = ilp.run();
		for (Entry<String, Double> e : variable2score.entrySet()) {
			String varname = e.getKey();
			double val = e.getValue();
			// D.p(varname, val);
			if (val > 0.99) {
				picked.add(varname);
			}
		}
		return picked;
	}

	public static void main(String[] args) {
		String inputParallel = args[0];
		int K = Integer.parseInt(args[1]);
		String output = args[2];
		candidateEventRelations(inputParallel, K, output);
		// candidateFluentRelations(input_eecs, input_types, K, output);
	}
}
