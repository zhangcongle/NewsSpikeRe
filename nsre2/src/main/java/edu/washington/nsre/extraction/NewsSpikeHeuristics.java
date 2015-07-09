package edu.washington.nsre.extraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.IntCounter;
import edu.washington.nsre.util.*;

public class NewsSpikeHeuristics {
	static Gson gson = new Gson();

	public static void loadPhrase2eecnames(String input_tuples, HashMultimap<String, String> phrase2eecnames,
			HashMultimap<String, String> head2eecnames) {
		{
			DR dr = new DR(input_tuples);
			String[] l;
			while ((l = dr.read()) != null) {
				Tuple t = gson.fromJson(l[2], Tuple.class);
				Counter<String> dist1 = t.getArg1FineGrainedNer();
				Counter<String> dist2 = t.getArg2FineGrainedNer();
				for (String t1 : dist1.keySet()) {
					for (String t2 : dist2.keySet()) {
						for (String p : t.getPatternList()) {
							phrase2eecnames.put(t1 + "|" + p + "|" + t2, t.getArg1Head() + "\t" + t.getArg2Head());
							head2eecnames.put(t1 + "|" + t.getPatternHead() + "|" + t2,
									t.getArg1() + "\t" + t.getArg2());

						}
					}
				}
			}
			dr.close();
		}

	}

	public static List<String[]> keywordsHeuristicsSameHead(String input_keywords_unlabeled, String dir) {
		DR dr = new DR(input_keywords_unlabeled);
		DW dw = new DW(dir + "/sameHead");
		List<String[]> ret = new ArrayList<String[]>();
		String[] l;
		while ((l = dr.read()) != null) {
			if (l[1].startsWith(l[2]) || l[1].contains(l[2])) {
				l[0] = "1";
				dw.write(l);
				// D.p("h1", l[0], l[1], l[2], l[3]);
			}
			ret.add(l);

		}
		dw.close();
		dr.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsSameHead(List<String[]> keywords_unlabeled, String dir) {
		DW dw = new DW(dir + "/sameHead");
		List<String[]> ret = new ArrayList<String[]>();
		for (String[] l : keywords_unlabeled) {
			if (l[1].startsWith(l[2]) || l[1].contains(l[2])) {
				l[0] = "1";
				dw.write(l);
				// D.p("h1", l[0], l[1], l[2], l[3]);
			}
			ret.add(l);

		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsPrefix(List<String[]> keywords_unlabeled, String dir) {
		DW dw = new DW(dir + "/prefix");
		List<String[]> ret = new ArrayList<String[]>();
		for (String[] l : keywords_unlabeled) {
			if (l[0].equals("1") || l[0].equals("0")) {
				ret.add(l);
			} else {
				EventType et = new EventType(l[1]);
				String head1 = et.eventphrase.head;
				String w = l[2];
				int share = 0;
				for (int i = 0; i < head1.length() && i < w.length(); i++) {
					if (head1.charAt(i) == w.charAt(i))
						share++;
					else
						break;
				}
				if (share >= 4 || share == head1.length()) {
					l[0] = "1";
					dw.write(l);
				}
				if (l[2].startsWith("re") && l[1].startsWith(l[2].substring(2))) {
					l[0] = "1";
					dw.write(l);
				}
				ret.add(l);
			}
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsBingSynonym(List<String[]> list, String dir,
			String input_bing_synonyms_antonyms) {
		List<String[]> ret = new ArrayList<String[]>();
		HashMultimap<String, String> synonyms = HashMultimap.create();
		HashMultimap<String, String> antonyms = HashMultimap.create();
		{
			DR dr = new DR(input_bing_synonyms_antonyms);
			String[] l;
			while ((l = dr.read()) != null) {
				if (l[0].equals("1")) {
					synonyms.put(l[1], l[2]);
				} else {
					antonyms.put(l[1], l[2]);
				}
			}
			dr.close();
		}
		DW dw = new DW(dir + "/bing");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				String p = l[3];
				boolean isSyn = false;
				boolean isAnt = false;
				if (synonyms.containsEntry(ethead, w)) {
					// l[0] = "1";
					isSyn = true;
				}
				// if (p.contains(" ")) {
				// for (String s : synonyms.get(ethead)) {
				// if (p.startsWith(s) || s.startsWith(p)) {
				// isSyn = true;
				// }
				// }
				// }
				// for (String x : l[3].split(" ")) {
				// if (synonyms.containsEntry(ethead, x)
				// || synonyms.containsEntry(x, ethead)) {
				// isSyn = true;
				// }
				// }
				if (antonyms.containsEntry(ethead, w) || antonyms.containsEntry(w, ethead)) {
					// l[0] = "0";
					isAnt = true;
				}
				// for (String x : l[3].split(" ")) {
				// if (antonyms.containsEntry(ethead, x) ||
				// antonyms.containsEntry
				// (x, ethead)) {
				// isAnt = true;
				// }
				// }
				if (isAnt) {
					l[0] = "0";
					dw.write(l);
				} else if (isSyn) {
					l[0] = "1";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsBingAntonymTrans(List<String[]> list, String dir,
			String input_bing_synonyms_antonyms) {
		List<String[]> ret = new ArrayList<String[]>();
		HashMultimap<String, String> synonyms = HashMultimap.create();
		HashMultimap<String, String> antonyms = HashMultimap.create();
		{
			DR dr = new DR(input_bing_synonyms_antonyms);
			String[] l;
			while ((l = dr.read()) != null) {
				if (l[0].equals("1")) {
					synonyms.put(l[1], l[2]);
				} else {
					antonyms.put(l[1], l[2]);
					antonyms.put(l[2], l[1]);
				}
			}
			dr.close();
		}
		DW dw = new DW(dir + "/anttrans");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				boolean isAnt = false;
				for (String y : antonyms.get(ethead)) {
					if (synonyms.containsEntry(w, y) || synonyms.containsEntry(y, w)) {
						isAnt = true;
						D.p(ethead, y, w);
					}
				}
				if (isAnt) {
					l[0] = "0";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsStrongNegative(List<String[]> list, String dir,
			String input_negatives) {
		List<String[]> ret = new ArrayList<String[]>();
		HashMultimap<String, String> negativesMap = HashMultimap.create();
		Set<String> uncertain = new HashSet<String>();
		uncertain.add("would");
		uncertain.add("could");
		uncertain.add("should");
		uncertain.add("will");
		uncertain.add("only");
		uncertain.add("just");
		uncertain.add("about");
		uncertain.add("have");

		Counter<String> negatives = new ClassicCounter<String>();
		HashMap<String, Counter<String>> keyhead2negatives = new HashMap<String, Counter<String>>();
		{
			DR dr = new DR(input_negatives);
			String[] l;
			while ((l = dr.read()) != null) {
				String[] ab = l[4].split("@");
				String key = l[2] + " " + l[3];
				String[] words = key.split(" |\\|");
				boolean isUncertain = false;
				for (String w : words) {
					if (uncertain.contains(w)) {
						isUncertain = true;
					}
				}
				if (!isUncertain) {
					negativesMap.put(l[0] + "\t" + l[1], ab[0] + "\t" + ab[1]);
					negativesMap.put(l[1] + "\t" + l[0], ab[0] + "\t" + ab[1]);
				}
				// negatives.incrementCount(l[0] + "\t" + l[1]);
				// negatives.incrementCount(l[1] + "\t" + l[0]);
			}
			dr.close();
		}
		for (String key : negativesMap.keySet()) {
			negatives.incrementCount(key, negativesMap.get(key).size());
			String[] ab = key.split("\t");
			if (!keyhead2negatives.containsKey(ab[0])) {
				keyhead2negatives.put(ab[0], new IntCounter<String>());
			}
			keyhead2negatives.get(ab[0]).incrementCount(ab[1]);
		}
		DW dw = new DW(dir + "/strongnegatives");
		List<String[]> temp = new ArrayList<String[]>();
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				double c = negatives.getCount(ethead + "\t" + w);
				if (c > 0)
					temp.add(DW.tow(et.str, ethead, w, c));
			}
		}
		Collections.sort(temp, new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2) {
				if (o1[0].equals(o2[0]))
					return Double.compare(Double.parseDouble(o2[3]), Double.parseDouble(o1[3]));
				else
					return o1[0].compareTo(o2[0]);
			}
		});
		// for (String[] t : temp) {
		// D.p(t);
		// }
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				if (negatives.getCount(ethead + "\t" + w) >= 2) {
					l[0] = "0";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsNegative(List<String[]> list, String dir, String input_negatives) {
		List<String[]> ret = new ArrayList<String[]>();
		HashMultimap<String, String> negativesMap = HashMultimap.create();
		Counter<String> negatives = new ClassicCounter<String>();
		{
			DR dr = new DR(input_negatives);
			String[] l;
			while ((l = dr.read()) != null) {
				String[] ab = l[4].split("@");
				if(l.length<5 || ab.length<2)
					D.p(l);
				negativesMap.put(l[0] + "\t" + l[1], ab[0] + "\t" + ab[1]);
				negativesMap.put(l[1] + "\t" + l[0], ab[0] + "\t" + ab[1]);
				// negatives.incrementCount(l[0] + "\t" + l[1]);
				// negatives.incrementCount(l[1] + "\t" + l[0]);
			}
			dr.close();
		}
		for (String k : negativesMap.keySet()) {
			negatives.incrementCount(k, negativesMap.get(k).size());
		}
		DW dw = new DW(dir + "/negatives");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				if (negatives.getCount(ethead + "\t" + w) > 0) {
					l[0] = "0";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsStrongPositive(List<String[]> list, String dir,
			String input_strongpositive, HashMultimap<String, String> phrase2eecnames) {
		List<String[]> ret = new ArrayList<String[]>();
		Set<String> uncertain = new HashSet<String>();
		uncertain.add("by");
		Counter<String> positives = new ClassicCounter<String>();
		{
			DR dr = new DR(input_strongpositive);
			String[] l;
			while ((l = dr.read()) != null) {

				String words[] = (l[2] + " " + l[3]).split(" |\\|");
				boolean isUncertain = false;
				for (String w : words) {
					if (uncertain.contains(w)) {
						isUncertain = true;
					}
				}
				if (!isUncertain) {
					positives.incrementCount(l[0] + "\t" + l[1]);
					positives.incrementCount(l[1] + "\t" + l[0]);
				}

			}
			dr.close();
		}
		D.p(positives.size());
		DW dw = new DW(dir + "/strongpositives");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				String p = l[3];
				Set<String> eecsP = phrase2eecnames.get(et.getArg1TypeRoot() + "|" + p + "|" + et.getArg2TypeRoot());
				Set<String> eecsW = phrase2eecnames.get(et.getArg1TypeRoot() + "|" + w + "|" + et.getArg2TypeRoot());
				int c = Math.max(eecsP.size(), eecsW.size());
				if (positives.getCount(ethead + "\t" + w) > 0 /* && c <= 150 */) {
//					if (c > 150) {
//						D.p(l[0], l[1], l[2], l[3], c);
//					}
					l[0] = "1";
					dw.write(l[0], l[1], l[2], l[3], c);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsWeakPositive(List<String[]> list, String dir,
			String input_weakpositive) {
		List<String[]> ret = new ArrayList<String[]>();
		Counter<String> positives = new ClassicCounter<String>();
		{
			DR dr = new DR(input_weakpositive);
			String[] l;
			while ((l = dr.read()) != null) {
				positives.incrementCount(l[0] + "\t" + l[1]);
				positives.incrementCount(l[1] + "\t" + l[0]);
			}
			dr.close();
		}
		D.p(positives.size());
		DW dw = new DW(dir + "/weakpositives");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				EventType et = new EventType(l[1]);
				String ethead = et.eventphrase.head;
				String w = l[2];
				if (positives.getCount(ethead + "\t" + w) > 1) {
					l[0] = "1";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsTooGeneral(List<String[]> list, String dir,
			HashMultimap<String, String> phrase2eecnames) {
		List<String[]> ret = new ArrayList<String[]>();
		DW dw = new DW(dir + "/toogeneral");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				boolean fired = false;
				EventType et = new EventType(l[1]);
				String w = l[2];
				String p = l[3];
				// Set<String> eecsEt = head2eecnames.get(et.eventphrase.head);
				// Set<String> eecsP = head2eecnames.get(w);
				Set<String> eecsEt = phrase2eecnames
						.get(et.getArg1TypeRoot() + "|" + et.eventphrase.str + "|" + et.getArg2TypeRoot());
				Set<String> eecsP = phrase2eecnames.get(et.getArg1TypeRoot() + "|" + p + "|" + et.getArg2TypeRoot());
				int pCount = eecsP.size();
				int etCount = eecsEt.size();
				if (pCount > etCount) {
					l[0] = "0";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsCommonPhrase(List<String[]> list, String dir,
			HashMultimap<String, String> phrase2eecnames) {
		List<String[]> ret = new ArrayList<String[]>();
		DW dw = new DW(dir + "/common");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				boolean fired = false;
				EventType et = new EventType(l[1]);
				String p = l[3];
				Set<String> eecsEt = phrase2eecnames.get(et.arg1type + "|" + et.eventphrase.str + "|" + et.arg2type);
				Set<String> eecsP = phrase2eecnames.get(et.arg1type + "|" + p + "|" + et.arg2type);
				int pCount = eecsP.size();
				int pOverlap = 0;
				for (String eec : eecsP) {
					if (eecsEt.contains(eec)) {
						pOverlap++;
					}
				}
				if (pOverlap > 5 && pOverlap * 1.0 / pCount > 0.7) {
					fired = true;
				}
				if (fired) {
					l[0] = "1";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsRarePhrase(List<String[]> list, String dir,
			HashMultimap<String, String> head2eecnames) {
		List<String[]> ret = new ArrayList<String[]>();
		DW dw = new DW(dir + "/rare");
		for (String[] l : list) {
			if (l[0].equals("??")) {
				boolean fired = false;
				EventType et = new EventType(l[1]);
				String w = l[2];
				String p = l[3];
				Set<String> eecsEt = head2eecnames.get(et.arg1type + "|" + et.eventphrase.head + "|" + et.arg2type);
				Set<String> eecsP = head2eecnames.get(et.arg1type + "|" + w + "|" + et.arg2type);
				int pCount = eecsP.size();
				int pOverlap = 0;
				for (String eec : eecsP) {
					if (eecsEt.contains(eec)) {
						pOverlap++;
					}
				}
				if (pOverlap <= 5 && pOverlap * 1.0 / pCount > 0.6) {
					fired = true;
				}
				if (fired) {
					l[0] = "1";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static List<String[]> keywordsHeuristicsRestPhrase(List<String[]> list, String dir,
			HashMultimap<String, String> head2eecnames) {
		List<String[]> ret = new ArrayList<String[]>();
		DW dw = new DW(dir + "/rest");
		HashMultimap<String, String> posPhrases = HashMultimap.create();
		for (String[] l : list) {
			EventType et = new EventType(l[1]);
			String w = l[2];
			String p = l[3];
			if (l[0].equals("1")) {
				posPhrases.put(et.eventphrase.str, w);
			}
		}
		for (String[] l : list) {
			if (l[0].equals("??")) {
				boolean fired = false;
				EventType et = new EventType(l[1]);
				String w = l[2];
				String p = l[3];
				Set<String> eecsEt = new HashSet<String>();
				for (String pos : posPhrases.get(et.eventphrase.str)) {
					eecsEt.addAll(head2eecnames.get(et.getArg1TypeRoot() + "|" + pos + "|" + et.getArg2TypeRoot()));
				}
				// Set<String> eecsEt = head2eecnames.get(et.getArg1TypeRoot() +
				// "|"
				// + et.eventphrase.head
				// + "|" + et.getArg2TypeRoot());

				Set<String> eecsP = head2eecnames.get(et.getArg1TypeRoot() + "|" + w + "|" + et.getArg2TypeRoot());
				int pCount = eecsP.size();
				int pOverlap = 0;
				for (String eec : eecsP) {
					if (eecsEt.contains(eec)) {
						pOverlap++;
					}
				}
				if (pOverlap * 1.0 / pCount > 0.9) {
					fired = true;
				}
				if (fired) {
					l[0] = "1";
					dw.write(l);
				}
			}
			ret.add(l);
		}
		dw.close();
		return ret;
	}

	public static void heuristics(String input_keywords_unlabeled, String dir, String output, String input_tuples,
			String input_dictionary, String input_heuristic_negatives, String input_heuristic_positives) {
		if (!new File(dir).exists())
			(new File(dir)).mkdir();
		HashMultimap<String, String> phrase2eecnames = HashMultimap.create();
		HashMultimap<String, String> head2eecnames = HashMultimap.create();
		loadPhrase2eecnames(input_tuples, phrase2eecnames, head2eecnames);
		List<String[]> list = keywordsHeuristicsSameHead(input_keywords_unlabeled, dir);
		list = keywordsHeuristicsPrefix(list, dir);
		list = keywordsHeuristicsStrongPositive(list, dir, input_heuristic_positives, head2eecnames);
		list = keywordsHeuristicsNegative(list, dir, input_heuristic_negatives);

		list = keywordsHeuristicsBingSynonym(list, dir, input_dictionary);
		list = keywordsHeuristicsBingAntonymTrans(list, dir, input_dictionary);
		// list = keywordsHeuristicsTooGeneral(list, dir, phrase2eecnames);
		// list = keywordsHeuristicsStrongNegative(list, dir,
		// input_heuristic_negatives);
		list = keywordsHeuristicsRestPhrase(list, dir, head2eecnames);
		// list = keywordsHeuristicsCommonPhrase(list, dir,
		// phrase2eecnames);
		// list = keywordsHeuristicsRarePhrase(list, dir,
		// phrase2eecnames);
		// evalHeuristics(dir, input_labeled);
		{
			DW dw = new DW(output);
			for (String[] l : list) {
				if (l[0].equals("1") || l[0].equals("0")) {
					dw.write(l);
				}
			}
			dw.close();
		}
	}

	public static void crawlBingDefinition(String input_candidates,
			String bingWordDir,
			String tempHeuristicsDir,
			String output) throws IOException {
		// HashMap<String, String> words = new HashMap<String, String>();
		HashSet<String> words = new HashSet<String>();
		{
			DR dr = new DR(input_candidates);
			String[] l;
			while ((l = dr.read()) != null) {
				String w = l[1].split(" ")[0];
				words.add(w);
			}
			dr.close();
		}
		if (!new File(tempHeuristicsDir).exists()) {
			new File(tempHeuristicsDir).mkdir();
		}
		String file1 = tempHeuristicsDir + File.separator + "bing_syn_ant_1";
		{

			DW dw = new DW(file1);
			for (String w : words) {
				List<String[]> ret = BingWordDefinition.oneword(w, bingWordDir);
				for (String[] r : ret)
					dw.write(r);
			}
			dw.close();
		}
		{
			DR dr = new DR(file1);
			DW dw = new DW(output);
			List<String[]> b;
			while ((b = dr.readBlock(1)) != null) {
				Counter<String> c = new ClassicCounter<String>();
				String key = b.get(0)[1];
				for (String[] l : b) {
					dw.write(l);
					if (l[0].equals("1") && l[2].contains(" ")) {
						String[] abc = l[2].split(" ");
						for (String w : abc) {
							if (!RemoveStopwords.isStop(w) && !RemoveStopwords.isStopVerb(w)) {
								c.incrementCount(w);
							}
						}
					}
				}
				for (String w : c.keySet()) {
					if (c.getCount(w) > 1) {
						D.p("1", key, w);
						dw.write("1", key, w);
					}
				}
			}
			dw.close();
		}
	}

	public static void bing_synonyms_antonyms_add_count(String input_tuples, String input_bing_synonyms_antonyms,
			String output_bing_synonyms_antonyms_count) {
		Counter<String> countKeywords = new IntCounter<String>();
		{
			DR dr = new DR(input_tuples);
			String[] l;
			while ((l = dr.read()) != null) {
				Tuple t = gson.fromJson(l[2], Tuple.class);
				Set<String> keywords = t.getPatternKeywords();
				for (String p : t.getPatternList()) {
					keywords.add(p);
				}
				for (String w : keywords) {
					countKeywords.incrementCount(w);
				}
			}
			dr.close();
		}
		{
			DR dr = new DR(input_bing_synonyms_antonyms);
			DW dw = new DW(output_bing_synonyms_antonyms_count);
			String[] l;
			while ((l = dr.read()) != null) {
				int c1 = (int) countKeywords.getCount(l[1]);
				int c2 = (int) countKeywords.getCount(l[2]);
				dw.write(l[0], l[1], l[2], c1, c2);
			}
			dr.close();
			dw.close();
		}
	}

	public static void main(String[] args) {
		// String input_keywords_unlabeled = args[0];
		// String dir = args[1];
		// String output = args[2];
		// String input_tuples = args[3];
		// String input_dictionary = args[4];
		// String input_heuristic_negatives = args[5];
		// String input_heuristic_positives = args[6];
		try {
			if (args.length > 0) {
				Config.configFile = args[0];
			}
			Config.parseConfig();
			NewsSpikeCandidate.CAP1 = 100;
			NewsSpikeCandidate.CAP3 = 100;
			NewsSpikeCandidate.candidates(Config.parallelFile, Config.eventsFile, Config.keywordsFile,
					Config.candidatesFile);
			crawlBingDefinition(Config.candidatesFile,
					Config.bingWordsDir,
					Config.tempDirHeuristics,
					Config.dictionaryFile);
			heuristics(Config.keywordsFile, Config.tempDirHeuristics, Config.keywordsAnnotationFile,
					Config.parallelFile, Config.dictionaryFile, Config.heuristicNegativeFile,
					Config.heuristicPositiveFile);
			List<ConnectedComponent> ccs = NewsSpikeExtractor.featurize(Config.parallelFile, Config.candidatesFile,
					Config.tempDirGenerate);
			NewsSpikeExtractor.learning(ccs, Config.keywordsAnnotationFile, Config.generatedTrainingFile);
			NewsSpikeExtractor.buildExtractor(Config.generatedTrainingFile, Config.modelFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
