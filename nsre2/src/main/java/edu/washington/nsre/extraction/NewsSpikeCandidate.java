package edu.washington.nsre.extraction;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.IntCounter;

public class NewsSpikeCandidate {
	static Gson gson = new Gson();
	static int CAP1 = 20;
	static int CAP2 = 5;
	static int CAP3 = 50;

	static class KV {
		String kw;
		String phrase;
		String eecname;
		String s1;
		String s2;
		int count;

		public KV(String kw, String phrase, String eecname, String s1,
				String s2, int count) {
			this.kw = kw;
			this.phrase = phrase;
			this.eecname = eecname;
			this.s1 = s1;
			this.s2 = s2;
			this.count = count;

		}
	}

	static class KVList {
		List<KV> kvlist = new ArrayList<KV>();
	}

	public static void candidates(
			String inputParallel,
			String inputEvents,
			String outputKeywords,
			String outputCandidates) {

		Set<String> neg = new HashSet<String>();
		neg.add("no");
		neg.add("not");
		neg.add("never");
		neg.add("refuse");
		neg.add("cancel");
		HashMultimap<String, HashMap<String, String[]>> map = HashMultimap
				.create();
		HashMultimap<String, String> verb2events = HashMultimap.create();
		{
			DR dr = new DR(inputEvents);
			String[] l;
			while ((l = dr.read()) != null) {
				String verb = l[2];
				String event = l[0];
				verb2events.put(verb, event); // verb -> event
				map.put(event, new HashMap<String, String[]>());
			}
			dr.close();
		}

		{
			DR dr = new DR(inputParallel);
			List<String[]> b = null;
			List<String[]> tow = new ArrayList<String[]>();
			while ((b = dr.readBlock(0)) != null) {
				String eecname = b.get(0)[0];
				HashMap<String, Tuple> events2sentence = new HashMap<String, Tuple>();
				List<Tuple> tuples = new ArrayList<Tuple>();
				for (String[] l : b) {
					tuples.add(gson.fromJson(l[2], Tuple.class));
				}
				for (int i = 0; i < b.size(); i++) {
					String[] l = b.get(i);
					Tuple t = tuples.get(i);
					String eec = l[0];
					Counter<String> dist1 = t.getArg1FineGrainedNer();
					Counter<String> dist2 = t.getArg2FineGrainedNer();
					if (t.getRel() != null
							&& verb2events.containsKey(t.getRel())) {
						for (String event : verb2events.get(t.getRel())) {
							String[] verb_arg1type_arg2type = event.split("@");
							String arg1type = verb_arg1type_arg2type[1];
							String arg2type = verb_arg1type_arg2type[2];
							if (dist1.getCount(arg1type) > 0
									&& dist2.getCount(arg2type) > 0) {
								events2sentence.put(event, t);
							}
						}
					}
				}
				for (String event : events2sentence.keySet()) {
					for (int i = 0; i < b.size(); i++) {
						String[] l = b.get(i);
						String eventPhrases = l[1];// e.g. cut|[X] cut [Y]
						Tuple t = gson.fromJson(l[2], Tuple.class);
						Tuple tk = events2sentence.get(event);
						Set<String> keys = t.getPatternKeywords();
						String[] w = DW.tow(event, "",
								eecname, tk.getSentence(), t.getSentence(),
								tk.getPatternHead(), eventPhrases,
								t.getArg1Head()
										+ "\t" + t.getArg2Head()
								);
						for (String kw : keys) {
							w[1] = kw;
							tow.add(w);
						}
					}
				}
			}
			dr.close();
			Collections.sort(tow, new Comparator<String[]>() {
				public int compare(String[] o1, String[] o2) {
					return StringUtil
							.compareStrings(o1, o2, new int[] { 0, 1 });
				}
			});
			List<String[]> tow2 = new ArrayList<String[]>();
			List<List<String[]>> blocks = StringTable.toblock(tow, new int[] {
					0, 1 });
			DW dw = new DW(outputKeywords);
			DW dwcan = new DW(outputCandidates);
			for (List<String[]> c : blocks) {
				Set<String> uniqHeadPairs = new HashSet<String>();
				for (String[] cc : c) {
					uniqHeadPairs.add(cc[7]);
				}
				if (uniqHeadPairs.size() < 2)
					continue;
				HashMap<String, KV> phrase2kv = new HashMap<String, KV>();
				for (String[] l : c) {
					String[] rels = l[6].split("\\|");
					for (String r : rels) {
						if (!phrase2kv.containsKey(r)) {
							KV kv = new KV(l[1], r, l[2], l[3], l[4], 1);
							phrase2kv.put(r, kv);
						} else {
							phrase2kv.get(r).count++;
						}
					}
				}
				List<KV> kvs = new ArrayList<KV>();
				for (KV kv : phrase2kv.values()) {
					kvs.add(kv);
				}
				Collections.sort(kvs, new Comparator<KV>() {
					public int compare(KV o1, KV o2) {
						return o2.count - o1.count;
					}
				});
				String[] c0 = c.get(0);
				KVList kvlist = new KVList();
				kvlist.kvlist = kvs;
				String temp = gson.toJson(kvlist);
				if (c0[0].equals("win@/person@/award")
						&& c0[1].equals("dominate")) {
					// D.p("temp");
				}
				tow2.add(DW.tow("??", c0[0],
						c0[1],
						temp,
						uniqHeadPairs.size()));
			}

			// group by event+keyword
			// for (List<String[]> c : blocks) {
			// String[] l = c.get(0);
			// StringBuilder allstrs = new StringBuilder();
			// HashSet<String> allstrSet = new HashSet<String>();
			// for (String[] c0 : c) {
			// String[] rels = c0[6].split("\\|");
			// for (String r : rels)
			// allstrSet.add(r);
			// }
			//
			// Set<String> uniqHeadPairs = new HashSet<String>();
			// for (String[] cc : c) {
			// uniqHeadPairs.add(cc[7]);
			// }
			// if (uniqHeadPairs.size() >= 2) {
			// tow2.add(DW.tow("??", l[0],
			// l[1],
			// gson.toJson(allstrSet),
			// uniqHeadPairs.size(),
			// l[2], l[3], l[4],
			// l[5]));
			// }
			// }
			Collections.sort(tow2, new Comparator<String[]>() {
				public int compare(String[] o1, String[] o2) {
					return StringUtil
							.compareStrings(o1, o2, new int[] { 1, 4 },
									new boolean[] { false, true });
				}
			});
			Counter<String> c = new IntCounter<String>();
			List<String[]> ret = new ArrayList<String[]>();
			Set<String> appearedKeywords = new HashSet<String>();
			for (String[] w : tow2) {
				KVList kvlist = gson.fromJson(w[3], KVList.class);
				String eventtype = w[1];
				String kw = w[2];
				int uniqHeadSize = Integer.parseInt(w[4]);

				{
					for (KV kv : kvlist.kvlist) {
						String[] candidate = new String[] { eventtype, kw,
								kv.phrase };
						boolean containsNeg = false;
						for (String w0 : kv.phrase.split(" ")) {
							if (neg.contains(w0)) {
								containsNeg = true;
							}
						}
						if (!containsNeg) {
							ret.add(candidate);
						}
					}
					// c.incrementCount(eventtype);
				}
				if (c.getCount(eventtype) < CAP1 || (uniqHeadSize >= CAP2
						&& c.getCount(eventtype) < CAP3))
				{
					// if (labeled.containsKey(w[1] + "\t" + w[2])) {
					// w[0] = labeled.get(w[1] + "\t" + w[2]) + "";
					// }
					StringBuilder sb = new StringBuilder();
					int cap100 = 0;
					for (KV kv : kvlist.kvlist) {
						// for (String s : phrase2kv.keySet()) {
						sb.append(kv.phrase + ";");
						if (cap100++ > 100)
							break;
					}
					for (KV kv0 : kvlist.kvlist) {
						String[] words = kv0.phrase.split(" ");
						boolean containKeyword = false;
						for (String w0 : words) {
							if (appearedKeywords.contains(w[1] + "\t" + w0)) {
								containKeyword = true;
							}
						}
						if (containKeyword)
							continue;
						dw.write("??", w[1], kv0.kw, kv0.phrase, kv0.eecname,
								kv0.s1,
								kv0.s2,
								uniqHeadSize);

						appearedKeywords.add(w[1] + "\t" + kv0.kw);
						boolean containsNeg = false;
						for (String w0 : words) {
							if (neg.contains(w0)) {
								containsNeg = true;
							}
						}
						if (!containsNeg) {
							c.incrementCount(eventtype);
							break;
						}
					}
				}
			}
			for (String[] w : ret) {
				dwcan.write(w);
			}
			dwcan.close();
			dw.close();
			// return ret;
		}
	}


	public static void main(String[] args) {
		try {
			Config.parseConfig();
			candidates(Config.parallelFile, Config.eventsFile, Config.keywordsFile, Config.candidatesFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
