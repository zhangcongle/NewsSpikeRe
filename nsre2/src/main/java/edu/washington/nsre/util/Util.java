package edu.washington.nsre.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.washington.nsre.extraction.Tuple;

public class Util {
	public static String counter2str(Counter<String> c) {
		if (c != null) {
			return Counters.toSortedString(c, Integer.MAX_VALUE, "%s=%f", ", ", "[%s]");
		} else {
			return "{}";
		}
	}

	public static String counter2str(Counter<String> c, int k) {
		if (c != null && c.size() > 0)
			return Counters.toSortedString(c, k, "%s=%f", ", ", "[%s]");
		else
			return "";
	}

	public static HashMap<String, Double> counter2map(Counter<String> c) {
		HashMap<String, Double> m = new HashMap<String, Double>();
		for (String k : c.keySet()) {
			m.put(k, c.getCount(k));
		}
		return m;
	}

	public static double average(List<Double> values) {
		double sum = 0;
		for (Double a : values) {
			sum += a;
		}
		return sum * 1.0 / values.size();
	}

	public static double avgFirstK(List<Double> values, int K) {
		List<Double> nvals = new ArrayList<Double>(values);
		Collections.sort(values);
		double sum = 0;
		int k = 0;
		for (int i = 0; i < K && i < values.size(); i++) {
			sum += values.get(i);
			k++;
		}
		return sum * 1.0 / k;
	}

	static Gson gson = new Gson();

	public static String counter2jsonstr(Counter<String> c) {
		Map<String, Double> m = new HashMap<String, Double>();
		for (String k : c.keySet()) {
			m.put(k, c.getCount(k));
		}
		return gson.toJson(m);
	}

	public static Counter<String> jsonstr2counter(String s) {
		Map<String, Double> m = gson.fromJson(s, Map.class);
		return Counters.fromMap(m);
	}

	public static void leafFiles(String dir, List<String> files) {
		File root = new File(dir);
		if (root.exists()) {
			File[] list = root.listFiles();

			for (File f : list) {
				if (f.isDirectory()) {
					leafFiles(f.getAbsolutePath(), files);
					// System.out.println("Dir:" + f.getAbsoluteFile());
				} else {
					files.add(f.getAbsolutePath());
					// System.out.println("File:" + f.getAbsoluteFile());
				}
			}
		}
	}

	public static String pairAB(String a, String b) {
		if (a.compareTo(b) > 0) {
			return a + "::" + b;
		} else {
			return b + "::" + a;
		}
	}

	public static String pairAB(String a, String b, char sep) {
		if (a.compareTo(b) < 0) {
			return a + sep + b;
		} else {
			return b + sep + a;
		}
	}

	public static String[] unpairAB(String ab, char sep) {
		return ab.split(sep + "");
	}

	// /**
	// * For some pivotVerb, human annotators never assign any class, they are
	// * stop verbs, get rid of them!
	// */
	// public static boolean isStopPivotVerb(VerbGroupCluster vgcgold) {
	// boolean res = true;
	// for (String a : vgcgold.eecclustering.values()) {
	// if (!a.equals("NA"))
	// res = false;
	// }
	// // D.p(res, vgcgold.eecclustering.values());
	// return res;
	// }

	public static String getDayInStringOfDate(Date date) {
		SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
		String today = dateformatYYYYMMDD.format(date);
		return today;
	}

	public static void getRelationPairsFromClustering(String input, Set<String> pairs, Set<String> diffheadPairs) {
		DR dr = new DR(input);
		String[] l;
		List<String[]> lines = new ArrayList<String[]>();
		while ((l = dr.read()) != null) {
			if (l[0].equals("###")) {
				List<String> rels = new ArrayList<String>();
				List<String> heads = new ArrayList<String>();
				for (String[] ll : lines) {
					if (Double.parseDouble(ll[0]) > 0.9) {
						String r = Tuple.getRidOfOlliePartOfRelation(ll[1]);
						rels.add(r);
						heads.add(ll[2]);
					}
				}
				for (int i = 0; i < rels.size(); i++) {
					for (int j = i + 1; j < rels.size(); j++) {
						String a = rels.get(i);
						String b = rels.get(j);
						if (!a.equals(b)) {
							String p = Util.pairAB(a, b);
							pairs.add(p);
							// if (a.compareTo(b) > 0) {
							// a = rels.get(j);
							// b = rels.get(i);
							// }
							// pairs.add(a + "::" + b);
							if (!heads.get(i).equals(heads.get(j))) {
								diffheadPairs.add(p);
							}
						}
					}
				}
				lines = new ArrayList<String[]>();
			} else {
				lines.add(l);
			}
		}
		dr.close();
	}

	public static HashSet<String> getPositiveRelationsFromClustering(String input) {
		HashSet<String> correct = new HashSet<String>();
		{
			DR dr = new DR(input);
			String[] l;
			List<String[]> lines = new ArrayList<String[]>();
			int NewsSpikeId = -1;
			while ((l = dr.read()) != null) {
				if (l[0].equals("###")) {
					List<String> rels = new ArrayList<String>();
					for (String[] ll : lines) {
						if (Double.parseDouble(ll[0]) > 0.9) {
							correct.add(NewsSpikeId + "\t" + ll[1]);
						}
					}
					lines = new ArrayList<String[]>();
					if (l.length > 1) {
						NewsSpikeId = Integer.parseInt(l[2]);
					}
				} else {
					lines.add(l);
				}
			}
			dr.close();
		}
		return correct;
	}

	public static HashMap<Integer, List<String[]>> loadClusterOutput(String file) {
		HashMap<Integer, List<String[]>> ret = new HashMap<Integer, List<String[]>>();
		DR dr = new DR(file);
		String[] l;
		List<String[]> lines = new ArrayList<String[]>();
		int newsspikeid = 0;
		while ((l = dr.read()) != null) {
			if (l[0].equals("###")) {
				ret.put(newsspikeid, lines);
				lines = new ArrayList<String[]>();
				if (l.length > 1)
					newsspikeid = Integer.parseInt(l[2]);
			} else {
				lines.add(l);
			}
		}
		return ret;
	}

	public static HashMultimap<Integer, String> loadClusterOutputInStrHardonly(String file) {
		HashMultimap<Integer, String> ret = HashMultimap.create();
		DR dr = new DR(file);
		String[] l;
		List<String[]> lines = new ArrayList<String[]>();
		int newsspikeid = -1;
		while ((l = dr.read()) != null) {
			if (l[0].equals("###")) {
				if (newsspikeid >= 0 && lines.size() > 0) {
					HashSet<String> temp = new HashSet<String>();
					for (String[] a : lines) {
						String r = a[2];
						String key = newsspikeid + "\t" + r;
						if (Double.parseDouble(a[0]) > 0.99 && !ret.containsEntry(newsspikeid, key)) {
							temp.add(key);
						}
					}
					if (temp.size() > 1) {
						for (String k : temp)
							ret.put(newsspikeid, k);
					}
				}
				lines = new ArrayList<String[]>();
				if (l.length > 1)
					newsspikeid = Integer.parseInt(l[2]);
			} else {
				lines.add(l);
			}
		}
		return ret;
	}

	public static HashMultimap<Integer, String> loadClusterOutputInStrHardonly0702(String file) {
		HashMultimap<Integer, String> ret = HashMultimap.create();
		DR dr = new DR(file);
		String[] l;
		List<String[]> lines = new ArrayList<String[]>();
		int newsspikeid = -1;
		while ((l = dr.read()) != null) {
			if (l[0].equals("###")) {
				// ret.put(newsspikeid, lines);
				if (newsspikeid >= 0 && lines.size() > 0) {
					// HashCount<String> hc = countHead(lines);
					String majorityHead = majorityHead(lines);
					for (String[] a : lines) {
						String r = Tuple.getRidOfOlliePartOfRelation(a[1]);
						// int c = headcount.see(a[2]);
						if (Double.parseDouble(a[0]) > 0.99 && !a[2].equals(majorityHead)
						// && hc.see(a[2]) == 1
								&& !ret.containsEntry(newsspikeid, newsspikeid + "\t" + r)) {
							ret.put(newsspikeid, newsspikeid + "\t" + r);
						}
					}
				}
				lines = new ArrayList<String[]>();
				if (l.length > 1)
					newsspikeid = Integer.parseInt(l[2]);
			} else {
				lines.add(l);
			}
		}
		return ret;
	}

	public static HashCount<String> countHead(List<String[]> lines) {
		// HashSet<String> appeared = new HashSet<String>();
		HashCount<String> headcount = new HashCount<String>();
		for (String[] a : lines) {
			String r = Tuple.getRidOfOlliePartOfRelation(a[1]);
			// if (!appeared.contains(r)) {
			headcount.add(a[2]);
			// appeared.add(r);
			// }
		}
		return headcount;
	}

	public static String majorityHead(List<String[]> lines) {
		HashSet<String> appeared = new HashSet<String>();
		HashCount<String> headcount = new HashCount<String>();
		for (String[] a : lines) {
			String r = Tuple.getRidOfOlliePartOfRelation(a[1]);
			if (!appeared.contains(r)) {
				headcount.add(a[2]);
				appeared.add(r);
			}
		}
		int max = 0;
		String ret = null;
		for (Entry<String, Integer> e : headcount.entries()) {
			int c = e.getValue();
			if (c >= 2 && c > max) {
				ret = e.getKey();
			}
		}
		if (ret == null) {
			ret = "NO_MAJORITY";
		}
		return ret;
	}

	public static List<String[]> evalsingle(HashMap<Integer, List<String[]>> gold,
			HashMap<Integer, List<String[]>> answer, int[] ret) {
		List<String[]> debug = new ArrayList<String[]>();
		for (Entry<Integer, List<String[]>> e : gold.entrySet()) {
			int modelId = e.getKey();
			int[] a = new int[6];
			List<String[]> glines = gold.get(modelId);
			if (answer.containsKey(modelId)) {
				List<String[]> alines = answer.get(modelId);
				evalsingleHelp(glines, alines, a, debug, modelId);
			} else {
				evalsingleHelpNoRetAnswer(glines, a, debug, modelId);
			}
			for (int i = 0; i < a.length; i++) {
				ret[i] += a[i];
			}
		}
		// for (Entry<Integer, List<String[]>> e : answer.entrySet()) {
		// int modelId = e.getKey();
		// if (gold.containsKey(modelId)) {
		// List<String[]> glines = gold.get(modelId);
		// List<String[]> alines = answer.get(modelId);
		// int[] a = new int[6];
		// evalsingleHelp(glines, alines, a, debug);
		// for (int i = 0; i < a.length; i++) {
		// ret[i] += a[i];
		// }
		// }
		// }
		Collections.sort(debug, new Comparator<String[]>() {
			public int compare(String[] arg0, String[] arg1) {
				return arg0[0].compareTo(arg1[0]);
			}
		});
		return debug;
	}

	private static void evalsingleHelp_0702(List<String[]> glines, List<String[]> alines, int[] ret,
			List<String[]> debug, int modelId) {
		// set ret[0]
		String majorityHead = majorityHead(glines);
		{
			Set<String> golds = new HashSet<String>();
			Set<String> answers = new HashSet<String>();
			for (String[] l : glines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					golds.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			for (String[] l : alines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					answers.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			// if (answers.size() > 1) {

			for (String s : answers) {
				if (golds.contains(s)) {
					ret[0]++;
				} else {
					debug.add(new String[] { "P", s, modelId + "" });
				}
				ret[1]++;
			}

			for (String s : golds) {
				if (!answers.contains(s)) {
					debug.add(new String[] { "R", s, modelId + "" });
				}
			}
			// ret[1] = answers.size();
			ret[2] = golds.size();
		}
		{
			Set<String> golds = new HashSet<String>();
			Set<String> answers = new HashSet<String>();
			HashCount<String> headcount = new HashCount<String>();
			for (String[] l : glines) {
				headcount.add(l[2]);
			}
			for (String[] l : glines) {
				int c = headcount.see(l[2]);
				if (Double.parseDouble(l[0]) > 0.9 && !majorityHead.equals(l[2])
				// && c <= 1
				) {
					golds.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			for (String[] l : alines) {
				if (Double.parseDouble(l[0]) > 0.9 && !majorityHead.equals(l[2])
				// headcount.see(l[2]) <= 1
				) {
					answers.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			for (String s : answers) {
				if (golds.contains(s)) {
					ret[3]++;
				}
			}
			ret[4] = answers.size();
			ret[5] = golds.size();
		}
	}

	private static void evalsingleHelp(List<String[]> glines, List<String[]> alines, int[] ret, List<String[]> debug,
			int modelId) {
		// set ret[0]
		// String majorityHead = majorityHead(glines);
		{
			Set<String> golds = new HashSet<String>();
			Set<String> answers = new HashSet<String>();
			for (String[] l : glines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					golds.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			for (String[] l : alines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					answers.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			// if (answers.size() > 1) {

			for (String s : answers) {
				if (golds.contains(s)) {
					ret[0]++;
				} else {
					debug.add(new String[] { "P", s, modelId + "" });
				}
				ret[1]++;
			}

			for (String s : golds) {
				if (!answers.contains(s)) {
					debug.add(new String[] { "R", s, modelId + "" });
				}
			}
			// ret[1] = answers.size();
			ret[2] = golds.size();
		}
		{
			Set<String> golds = new HashSet<String>();
			Set<String> answers = new HashSet<String>();
			HashCount<String> headcount = new HashCount<String>();
			for (String[] l : glines) {
				headcount.add(l[2]);
			}
			for (String[] l : glines) {
				int c = headcount.see(l[2]);
				if (Double.parseDouble(l[0]) > 0.9) {
					golds.add(l[2]);
				}
			}
			for (String[] l : alines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					answers.add(l[2]);
				}
			}
			if (answers.size() > 1 && golds.size() > 1) {
				for (String s : answers) {
					if (golds.contains(s)) {
						ret[3]++;
					}
				}
			}
			if (answers.size() > 1) {
				ret[4] = answers.size();
			}
			if (golds.size() > 1) {
				ret[5] = golds.size();
			}
		}
	}

	private static void evalsingleHelpNoRetAnswer(List<String[]> glines, int[] ret, List<String[]> debug, int modelId) {
		// set ret[0]
		{
			Set<String> golds = new HashSet<String>();
			for (String[] l : glines) {
				if (Double.parseDouble(l[0]) > 0.9) {
					golds.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}

			for (String s : golds) {
				debug.add(new String[] { "R", s, modelId + "" });
			}
			ret[2] = golds.size();
		}
		{
			Set<String> golds = new HashSet<String>();
			HashCount<String> headcount = new HashCount<String>();
			for (String[] l : glines) {
				headcount.add(l[2]);
			}
			for (String[] l : glines) {
				int c = headcount.see(l[2]);
				if (Double.parseDouble(l[0]) > 0.9 && c <= 1) {
					golds.add(Tuple.getRidOfOlliePartOfRelation(l[1]));
				}
			}
			ret[5] = golds.size();
		}
	}

	public static List<String[]> getNersFromTokensNers(String[] tkns, String[] ners) {
		List<String[]> res = new ArrayList<String[]>();
		int i = 0, j = 0;
		while (i < ners.length) {
			if (!ners[i].equals("O")) {
				j = i + 1;
				while (j < ners.length && ners[j].equals(ners[i])) {
					j++;
				}
				StringBuilder sb = new StringBuilder();
				for (int k = i; k < j; k++) {
					sb.append(tkns[k] + " ");
				}
				res.add(DW.tow(sb.toString(), ners[i]));
				i = j;
			} else {
				i++;
			}

		}
		return res;
	}

	public static List<String[]> getNersFromTokensNers(String[] tkns, String[] ners, int start, int end) {
		List<String[]> res = new ArrayList<String[]>();
		int i = start, j = 0;
		while (i < end) {
			if (!ners[i].equals("O")) {
				j = i + 1;
				while (j < end && ners[j].equals(ners[i])) {
					j++;
				}
				StringBuilder sb = new StringBuilder();
				for (int k = i; k < j; k++) {
					sb.append(tkns[k] + " ");
				}
				res.add(DW.tow(sb.toString().trim(), ners[i]));
				i = j;
			} else {
				i++;
			}

		}
		return res;
	}

	public static HashMap<String, Counter<String>> createBipartite(List<String[]> all) {
		HashMap<String, Counter<String>> bipartite = new HashMap<String, Counter<String>>();
		for (String[] l : all) {
			String left = l[0];
			String right = l[1];
			if (!bipartite.containsKey(left)) {
				bipartite.put(left, new ClassicCounter<String>());
			}
			bipartite.get(left).incrementCount(right);
		}
		return bipartite;
	}

	public static HashMap<String, String> greedyCoveringSolver(HashMap<String, Counter<String>> bipartite,
			double threshold) {
		// HashMultimap<String, String> ftr2obj = HashMultimap.create();
		HashMap<String, String> node2tag = new HashMap<String, String>();
		HashSet<String> appearedtags = new HashSet<String>();
		while (true) {
			Counter<String> scorer = new ClassicCounter<String>();
			for (Entry<String, Counter<String>> e : bipartite.entrySet()) {
				String node = e.getKey();
				if (node2tag.containsKey(node))
					continue;
				Counter<String> c = e.getValue();
				for (Entry<String, Double> ee : c.entrySet()) {
					String tag = ee.getKey();
					if (appearedtags.contains(tag))
						continue;
					double v = ee.getValue();
					if (v > threshold)
						scorer.incrementCount(ee.getKey(), ee.getValue());
				}
			}
			if (scorer.size() > 0) {
				String choosedtag = Counters.toSortedList(scorer).get(0);
				for (Entry<String, Counter<String>> e : bipartite.entrySet()) {
					String node = e.getKey();
					Counter<String> c = e.getValue();
					if (c.getCount(choosedtag) > threshold) {
						node2tag.put(node, choosedtag);
					}
				}
				appearedtags.add(choosedtag);
			} else {
				break;
			}
		}
		return node2tag;
	}

	public static String mapSNer2Nelner(String sner) {
		if (sner.equals("LOCATION")) {
			return "/location";
		} else if (sner.equals("PERSON")) {
			return "/person";
		} else if (sner.equals("ORGANIZATION")) {
			return "/organization";
		} else if (sner.equals("TIME")) {
			return "/time";
		} else if (sner.equals("DATE")) {
			return "/time";
		} else if (sner.equals("DURATION")) {
			return "/time";
		} else {
			return sner;
		}
	}

	public static String mapSNer2NelnerOnlyUseful(String sner) {
		if (sner.equals("LOCATION")) {
			return "/location";
		} else if (sner.equals("PERSON")) {
			return "/person";
		} else if (sner.equals("ORGANIZATION")) {
			return "/organization";
		} else if (sner.equals("TIME")) {
			return "/time";
		} else if (sner.equals("DATE")) {
			return "/time";
		} else if (sner.equals("DURATION")) {
			return "/time";
		} else if (sner.equals("MONEY") || sner.equals("PERCENT") || sner.equals("NUMBER")) {
			return "/" + sner.toLowerCase();
		} else {
			return null;
		}
	}

	public static String[] xiaonerHierarchy(String xiaoner) {
		String[] ab = xiaoner.replaceAll("/", " ").trim().split(" ");
		String[] res = new String[ab.length];
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ab.length; i++) {
			sb.append("/").append(ab[i]);
			res[i] = sb.toString();
		}
		return res;
	}
}
