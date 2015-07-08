package edu.washington.nsre.extraction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;

//import javatools.administrative.D;
//import javatools.filehandlers.DW;
//import javatools.stanford.SentDep;
//import javatools.string.RemoveStopwords;
//import javatools.string.StringUtil;
//import library.ollie.OllieSentence;
//import library.ollie.OneOllieExtr;
//import multir.RelationECML;
//import reverbstruct.ReverbExtraction;
//import reverbstruct.ReverbSentence;
//import util.UtilMath;
//import util.WordGram;

public class Tuple {
	String eecname = "";
	public Date date;
	public long articleId;
	public int artOffset;

	public String[] tkn;
	public String[] pos;
	public String[] ner;
	public String[] lmma;
	public List<SentDep> deps;

	HashMap<Integer, HashMap<Integer, String>> indexdep;
	public int a1[], a2[], v[];// a1start, a1end, a1head

	public String a1str;
	public String a2str;
	public String relstr;

	String tense;

	String sner1;// stanford Ner for argument 1
	String sner2; // stanford Ner for argument 2

	public HashMap<String, Double> fner1 = new HashMap<String, Double>();
	public HashMap<String, Double> fner2 = new HashMap<String, Double>();
	public String subtree;
	public String shortestPath;
	String pHead;
	int pHeadIdx = -2;
	String relollie;
	int tupleId;

	// public HashBasedTable<Integer, Integer, String> indexdep;
	static class DepTree {
		int id;
		List<DepTree> sons = new ArrayList<DepTree>();

		public DepTree(int id) {
			this.id = id;
		}

		/** find a subtree rooted in node */
		public static void traverse(DepTree node, Set<Integer> traversed) {
			if (traversed.contains(node.id)) {
				// loop!
				return;
			}
			traversed.add(node.id);
			for (DepTree s : node.sons) {
				traverse(s, traversed);
			}
		}
	}

	public Tuple(Date date, ReverbExtraction re, ReverbSentence rs, long articleId) {
		this.date = date;
		this.articleId = articleId;
		artOffset = rs.artOffset;
		tkn = rs.tkn;
		pos = rs.pos;
		ner = rs.ner;
		lmma = rs.lmma;
		deps = rs.deps;
		// for (SentDep sd : rs.deps) {
		// if (!indexdep.containsKey(sd.g)) {
		// indexdep.put(sd.g, new HashMap<Integer, String>());
		// }
		// indexdep.get(sd.g).put(sd.d, sd.t);
		// }
		a1 = new int[] { re.arg1.start, re.arg1.end, re.arg1.head };
		a2 = new int[] { re.arg2.start, re.arg2.end, re.arg2.head };
		v = new int[] { re.verb.start, re.verb.end, re.verb.head };
		setRelHead();
		a1str = StringUtil.join(tkn, " ", a1[0], a1[1]);
		a2str = StringUtil.join(tkn, " ", a2[0], a2[1]);
		relstr = getLmmaRel();
	}

	public Tuple() {

	}

	public void setRelHeadAndArgrelstr() {
		setRelHead();
		a1str = StringUtil.join(tkn, " ", a1[0], a1[1]);
		a2str = StringUtil.join(tkn, " ", a2[0], a2[1]);
		relstr = getLmmaRel();
	}

	public void setRelHead() {
		int start = v[0];
		int end = v[1];
		int head = -1;
		for (SentDep sd : deps) {
			if (pos[sd.g].startsWith("V") && (sd.g < end && sd.g >= start) && (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				// String v = lmma[h].toLowerCase();
				if (h > head) {
					head = h;
				}
			}
			if (pos[sd.d].startsWith("V") && (sd.d < end && sd.d >= start) && (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				// String v = lmma[h].toLowerCase();
				// if (!RemoveStopwords.isStopVerb(v))
				if (h > head)
					head = h;
				// heads.add(v);
			}
		}
		if (head > 0) {
			v[2] = head;
		} else {
			v[2] = start;
		}
	}

	void setRelHead_newbutdontwork() {
		// if (this.relstr.equals("will be in")) {
		// D.p(this.relstr);
		// }
		int start = v[0];
		int end = v[1];
		int[] count = new int[tkn.length];
		for (SentDep sd : deps) {
			if (pos[sd.g].startsWith("V") && (sd.g < end && sd.g >= start) && (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				count[h]++;

			}
			if (pos[sd.d].startsWith("V") && (sd.d < end && sd.d >= start) && (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				count[h]++;
			}
		}
		int head = end - 1;
		int max = 0;
		for (int i = v[1] - 1; i >= v[0]; i--) {
			if (!RemoveStopwords.isStopVerb(lmma[i]) && !RemoveStopwords.isStop(lmma[i]) && count[i] > max) {
				head = i;
				max = count[i];
			}
		}
		int lastverb = end - 1;
		for (int i = v[1] - 1; i >= v[0]; i--) {
			if (pos[i].startsWith("V")) {
				lastverb = i;
				break;
			}
		}
		if (max > 0) {
			v[2] = head;
		} else {
			// get the last verb
			v[2] = lastverb;
		}
	}

	public void setArg1Head() {
		setArgHead(a1);

	}

	public void setArg2Head() {
		setArgHead(a2);
	}

	public void setArgHead(int[] a) {
		int start = a[0];
		int end = a[1];
		int head = -1;
		for (SentDep sd : deps) {
			if ((sd.g < end && sd.g >= start) && (sd.d >= end || sd.d < start)) {
				int h = sd.g;
				if (h > head) {
					head = h;
				}
			}
			if ((sd.d < end && sd.d >= start) && (sd.g >= end || sd.g < start)) {
				int h = sd.d;
				// if (!RemoveStopwords.isStopVerb(v))
				if (h > head)
					head = h;
				// heads.add(v);
			}
		}
		if (head > 0) {
			a[2] = head;
		} else {
			a[2] = end - 1;
		}
	}

	public void setup() {

		// setIndexDeps();
		setRelHead();
		this.tense = UtilMath.tenseOfVerbPhrase(this.tkn, this.pos, this.v[0], this.v[1]);
	}

	public String getTense() {
		if (tense == null) {
			this.tense = UtilMath.tenseOfVerbPhrase(this.tkn, this.pos, this.v[0], this.v[1]);
		}
		return tense;
	}

	static Gson gson = new Gson();

	public static Tuple loadFromJson(String jsonstr) {
		Tuple t = gson.fromJson(jsonstr, Tuple.class);
		t.setup();
		// t.setIndexDeps();
		// t.setRelHead();
		// t.tense = UtilMath.tenseOfVerbPhrase(t.tkn, t.pos, t.v[0], t.v[1]);

		return t;
	}

	public static Tuple loadFromJson(String[] l) {
		Tuple t = gson.fromJson(l[3], Tuple.class);
		t.a1str = l[0];
		t.a2str = l[1];
		t.relstr = l[2];
		t.setup();
		return t;
	}

	public String getArg1() {
		// if (a1str == null) {
		// StringBuilder sb = new StringBuilder();
		// for (int i = a1[0]; i < a1[1]; i++) {
		// sb.append(lmma[i] + " ");
		// }
		// a1str = sb.toString().toLowerCase().trim();
		// }
		return a1str;
	}

	public String getArg1Ner() {
		// return ner[a1[2]];
		if (sner1 == null) {
			sner1 = ner[a1[1] - 1];
		}
		return sner1;
	}

	public String getArg1StanfordNer() {
		String sner1 = getArg1Ner();
		return mapSner2Fner(sner1);
	}

	public String getArg2StanfordNer() {
		String sner2 = getArg2Ner();
		return mapSner2Fner(sner2);
	}

	public String getArg2Ner() {
		// return ner[a2[2]];
		if (sner2 == null) {
			sner2 = ner[a2[1] - 1];
		}
		return sner2;
	}

	public static String mapSner2Fner(String ner) {
		if (ner.equals("PERSON")) {
			return "/person";
		} else if (ner.equals("ORGANIZATION")) {
			return "/organization";
		} else if (ner.equals("LOCATION")) {
			return "/location";
		} else if (ner.equals("TIME")) {
			return "/time";
		} else if (ner.equals("MONEY")) {
			return "/money";
		} else if (ner.equals("DATE")) {
			return "/time";
		} else if (ner.equals("NUMBER")) {
			return "/number";
		} else {
			return "/" + ner.toLowerCase();
		}
	}

	public Counter<String> getArg1FineGrainedNer() {
		return Counters.fromMap(this.fner1);
	}

	public Counter<String> getArg2FineGrainedNer() {
		return Counters.fromMap(this.fner2);
	}

	public void setArg1FineGrainedNer(String flabel) {
		String[] s = flabel.split(",");
		for (String a : s) {
			String[] xy = a.split("@");
			String ner = xy[0];
			double value = Double.parseDouble(xy[1]);
			this.fner1.put(ner, value);
		}
	}

	public void setArg2FineGrainedNer(String flabel) {
		String[] s = flabel.split(",");
		for (String a : s) {
			String[] xy = a.split("@");
			String ner = xy[0];
			double value = Double.parseDouble(xy[1]);
			this.fner2.put(ner, value);
		}
	}

	public String getNerStrOfArg1() {
		return nerStrOfArg(a1[2], a1[0], a1[1]);
	}

	public String getNerStrOfArg2() {
		return nerStrOfArg(a2[2], a2[0], a2[1]);
	}

	public HashSet<String> getAllNersOfArg1() {
		HashSet<String> result = new HashSet<String>();
		List<String[]> res = getAllNers(a1[0], a1[1]);
		for (String[] w : res) {
			result.add(w[0]);
		}
		return result;
	}

	public HashSet<String> getAllNersOfArg2() {
		HashSet<String> result = new HashSet<String>();
		List<String[]> res = getAllNers(a2[0], a2[1]);
		for (String[] w : res) {
			result.add(w[0]);
		}
		return result;
	}

	public String getLastNersOfArg2() {
		List<String[]> res = getAllNers(a2[0], a2[1]);
		if (res.size() > 0) {
			return res.get(res.size() - 1)[0];
		} else {
			return "";
		}
	}

	public String getLastNersOfArg1() {
		List<String[]> res = getAllNers(a1[0], a1[1]);
		if (res.size() > 0) {
			return res.get(res.size() - 1)[0];
		} else {
			return "";
		}
	}

	public List<String[]> getAllNers(int START, int END) {
		List<String[]> ret = new ArrayList<String[]>();
		String currentNer = "O";
		int start = -1;
		for (int i = START; i < END; i++) {
			if (!ner[i].equals(currentNer)) {
				if (!currentNer.equals("O")) {
					// shoot an NER
					int end = i;
					ret.add(DW.tow(StringUtil.join(tkn, " ", start, end).trim(), currentNer, start, end));
				}
				start = i;
				currentNer = ner[i];
			}
		}
		if (start > 0 && !currentNer.equals("O")) {
			ret.add(DW.tow(StringUtil.join(tkn, " ", start, END).trim(), currentNer, start, END));
		}
		return ret;
	}

	public String nerStrOfArg(int pos, int argstart, int argend) {

		int start = pos;
		int end = pos + 1;
		for (int k = pos - 1; k >= argstart; k--) {
			if (ner[k].equals(ner[pos])) {
				start = k;
			} else {
				break;
			}
		}
		for (int k = pos + 1; k < argend; k++) {
			if (ner[k].equals(ner[pos])) {
				end = k + 1;
			} else {
				break;
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int k = start; k < end; k++) {
			sb.append(tkn[k] + " ");
		}
		return sb.toString().trim();
	}

	public String getArg2() {
		// if (a2str == null) {
		// StringBuilder sb = new StringBuilder();
		// for (int i = a2[0]; i < a2[1]; i++) {
		// sb.append(lmma[i] + " ");
		// }
		// a2str = sb.toString().toLowerCase().trim();
		// }
		return a2str;
	}

	public String getRel() {
		// if (relstr == null) {
		// StringBuilder sb = new StringBuilder();
		// for (int i = v[0]; i < v[1]; i++) {
		// sb.append(lmma[i] + " ");
		// }
		// relstr = sb.toString().toLowerCase().trim();
		// }
		return relstr;
	}

	public String getStrBetweenArgs() {
		// if (relstr == null) {
		// StringBuilder sb = new StringBuilder();
		// for (int i = v[0]; i < v[1]; i++) {
		// sb.append(lmma[i] + " ");
		// }
		// relstr = sb.toString().toLowerCase().trim();
		// }
		StringBuilder sb = new StringBuilder();
		for (int i = a1[1]; i < a2[0]; i++) {
			sb.append(this.tkn[i] + " ");
		}
		return sb.toString().trim();
	}

	public String getLmmaBetweenArgs() {
		// if (relstr == null) {
		// StringBuilder sb = new StringBuilder();
		// for (int i = v[0]; i < v[1]; i++) {
		// sb.append(lmma[i] + " ");
		// }
		// relstr = sb.toString().toLowerCase().trim();
		// }
		StringBuilder sb = new StringBuilder();
		for (int i = a1[1]; i < a2[0]; i++) {
			sb.append(this.lmma[i] + " ");
		}
		return sb.toString().trim();
	}

	public List<String> getNgramBetweenArgs(int K) {
		List<String> ret = new ArrayList<String>();
		int start = a1[1];
		int end = a2[0] - 1;
		if (end - start + 1 < K || end - start > K + 5) {
			return ret;
		} else {
			for (int s = start; s <= end - K + 1; s++) {
				StringBuilder sb = new StringBuilder();
				for (int i = s; i < s + K; i++) {
					sb.append(this.lmma[i].toLowerCase() + "_");
				}
				ret.add(sb.toString());
			}
			return ret;
		}
	}

	public String getPatternListStr() {
		StringBuilder sb = new StringBuilder();
		if (this.getRel() != null) {
			sb.append("|").append(this.getRel());
		}
		if (this.getSubtree() != null) {
			sb.append("|").append(this.getSubtree());
		}
		if (sb.length() > 0)
			return sb.toString().substring(1);
		else
			return "";
	}

	public List<String> getPatternList() {
		List<String> ret = new ArrayList<String>();
		if (this.getRel() != null) {
			ret.add(this.getRel());
		}
		if (this.getSubtree() != null) {
			ret.add(this.getSubtree());
		}
		return ret;
	}

	public String getSubtree() {
		/** start */
		if (this.subtree == null) {
			StringBuilder sb = new StringBuilder();
			int[] parent = new int[this.tkn.length];
			for (int i = 0; i < parent.length; i++) {
				parent[i] = -1;
			}
			for (int i = 0; i < this.deps.size(); i++) {
				SentDep sd = this.deps.get(i);
				int a = sd.d;
				int b = sd.g;
				if (a != b) {// it sucks that sometimes 35 conj-and 35 happens
					parent[sd.d] = sd.g;
				}
			}
			int a1p = a1[2];
			int a2p = a2[2];
			Set<Integer> parentOfA1 = new HashSet<Integer>();
			int commonancester = -1;
			{
				int now = a1p;
				while (now >= 0 && now < parent.length) {
					if (parentOfA1.contains(now)) {
						break;
					}
					parentOfA1.add(now);
					now = parent[now];
				}
				now = a2p;
				Set<Integer> appeared = new HashSet<Integer>();
				while (now >= 0 && now < parent.length && !parentOfA1.contains(now)) {
					if (appeared.contains(now)) {
						break;
					}
					appeared.add(now);
					now = parent[now];
				}
				if (parentOfA1.contains(now)) {
					commonancester = now;
				}
			}
			HashMap<Integer, DepTree> tree = new HashMap<Integer, DepTree>();
			for (int i = 0; i < parent.length; i++) {
				tree.put(i, new DepTree(i));
			}
			for (int i = 0; i < parent.length; i++) {
				if (parent[i] >= 0 && parent[i] < parent.length) {
					tree.get(parent[i]).sons.add(tree.get(i));
				}
			}
			if (commonancester >= 0) {
				Set<Integer> traversed = new HashSet<Integer>();
				DepTree.traverse(tree.get(commonancester), traversed);
				boolean addedX = false, addedY = false;
				int numOfUseful = 0;
				for (int i = 0; i < parent.length; i++) {
					if (traversed.contains(i)) {
						if (i >= a1[0] && i < a1[1]) {
							if (!addedX) {
								sb.append("[X] ");
								addedX = true;
							}
						} else if (i >= a2[0] && i < a2[1]) {
							if (!addedY) {
								sb.append("[Y] ");
								addedY = true;
							}
						} else {
							numOfUseful++;
							if (this.ner[i].equals("NUMBER") || this.ner[i].equals("TIME") || this.ner[i].equals("DATE")
									|| this.ner[i].equals("MONEY")) {
								if (!sb.toString().endsWith("NN "))
									sb.append("NN ");
							} else {
								sb.append(this.lmma[i].toLowerCase() + " ");
							}
						}
					}
				}
				if (!(numOfUseful > 0 && numOfUseful <= 5)) {
					// D.p(sb.toString(), this.getSentence());
					sb = new StringBuilder();
				}
			}
			if (sb.toString().trim().length() > 0)
				subtree = sb.toString().trim();
			else
				subtree = null;
		}
		return this.subtree;
	}

	public String getPattern() {
		if (relstr != null) {
			return relstr;
		} else {
			return getShortestPathFromTuple();
		}
	}

	public String getPatternAll() {
		StringBuilder sb = new StringBuilder();
		if (relstr != null) {
			sb.append(relstr + ";");
		}
		sb.append(getShortestPathFromTuple());
		return sb.toString();
	}

	static RelationECML ecml = new RelationECML();

	public String getShortestPathFromTuple() {
		Tuple t = this;
		if (t.shortestPath == null) {
			StringBuilder sb = new StringBuilder();
			String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
			LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(t.tkn.length, t.deps);
			List<Integer> path = new ArrayList<Integer>();
			List<Integer> pathdir = new ArrayList<Integer>();
			boolean success = ecml.findShortestPath(idxDeps, t.a1[2], t.a2[2], path, pathdir);
			if (success) {
				for (int i = path.size() - 1; i >= 1; i--) {
					int start = path.get(i);
					int end = path.get(i - 1);
					int direct = pathdir.get(i - 1);
					if (direct == -1) {
						String depname = deps[end][start];
						sb.append("-[").append(depname).append("]->");
					} else {
						String depname = deps[start][end];
						sb.append("<-[").append(depname).append("]-");
					}
					if (i >= 2) {
						sb.append(t.lmma[end].toLowerCase());
					}
				}
				shortestPath = sb.toString();
			} else {
				shortestPath = null;
			}
		}
		return shortestPath;

	}

	String[] wordsInShortestPath;

	public String[] wordsInShortestPath() {
		Tuple t = this;
		if (wordsInShortestPath == null) {
			String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
			LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(t.tkn.length, t.deps);
			List<Integer> path = new ArrayList<Integer>();
			List<Integer> pathdir = new ArrayList<Integer>();
			boolean success = ecml.findShortestPath(idxDeps, t.a1[2], t.a2[2], path, pathdir);
			if (success) {
				wordsInShortestPath = new String[path.size()];
				for (int i = path.size() - 1; i >= 0; i--) {
					wordsInShortestPath[i] = t.lmma[path.get(i)].toLowerCase();
				}
			}
		}
		return wordsInShortestPath;
	}

	public String getSentence() {
		return StringUtil.join(this.tkn, " ");
	}

	public String getRelForReverb() {
		StringBuilder sb = new StringBuilder();
		for (int i = v[0]; i < v[1]; i++) {
			sb.append(lmma[i] + " ");
		}
		return sb.toString().toLowerCase().trim();
	}

	public String getRelOllie() {
		if (this.relollie == null) {
			// append prep of (prep_for) into the show name
			StringBuilder sb = new StringBuilder();
			sb.append(relstr).append(" ");
			int vhead = v[2];
			if (indexdep.containsKey(vhead)) {
				for (Entry<Integer, String> e : indexdep.get(vhead).entrySet()) {
					String type = e.getValue();
					int dep = e.getKey();
					if (dep >= a2[0] && dep < a2[1] || dep >= a1[0] && dep < a1[1] || dep >= v[0] && dep < v[1])
						continue;
					if (type.startsWith("prep-")) {
						type = type.replace("prep-", "");
						sb.append("(").append(type).append(" ");
						sb.append(lmma[dep]).append(") ");
					}
				}
				relollie = sb.toString().toLowerCase();
			}
		}
		return relollie;
	}

	public List<Integer> getRelOllieIndex() {
		// append prep of (prep_for) into the show name
		List<Integer> idx = new ArrayList<Integer>();
		int vhead = v[2];
		if (indexdep.containsKey(vhead)) {
			for (Entry<Integer, String> e : indexdep.get(vhead).entrySet()) {
				String type = e.getValue();
				int dep = e.getKey();
				if (dep >= a2[0] && dep < a2[1] || dep >= a1[0] && dep < a1[1] || dep >= v[0] && dep < v[1])
					continue;
				if (type.startsWith("prep-")) {
					idx.add(dep);
				}
			}
		}
		return idx;
	}

	public static String getRidOfOlliePartOfRelation(String s) {
		String r = s;
		if (r.indexOf("(") > 0) {
			r = r.substring(0, r.indexOf("("));
		}
		r = r.trim();
		return r;
	}

	public String getLmmaRel() {
		StringBuilder sb = new StringBuilder();
		for (int i = v[0]; i < v[1]; i++) {
			sb.append(lmma[i] + " ");
		}
		String lmmarel = sb.toString().toLowerCase().trim();
		return lmmarel;
	}

	public void setRelstr() {
		StringBuilder sb = new StringBuilder();
		for (int i = v[0]; i < v[1]; i++) {
			sb.append(lmma[i] + " ");
		}
		String lmmarel = sb.toString().toLowerCase().trim();
		relstr = lmmarel;
	}

	// Some Ollie relation doesn't have any verb inside! (Michael added the verb
	// to it)
	public boolean hasVerbInRel() {
		boolean ret = false;
		try {
			for (int i = v[0]; i < v[1]; i++) {
				if (this.pos[i].startsWith("V")) {
					ret = true;
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error\t" + gson.toJson(this.pos));
		}
		return ret;
	}

	public String getRelHead() {
		if (!lightverbs.contains(this.v[2])) {
			pHeadIdx = this.v[2];
			return lmma[this.v[2]];
		} else if (lmma[this.v[0]].equals("be") && this.v[0] + 1 < this.v[1]) {
			return lmma[this.v[0] + 1];
		} else {
			for (int k = this.v[1] - 1; k >= this.v[0]; k--) {
				if (!lightverbs.contains(lmma[k]) && !RemoveStopwords.isStop(lmma[k])) {
					pHeadIdx = k;
					return lmma[k];
				}
			}
		}
		pHeadIdx = this.v[2];
		return lmma[this.v[2]];
	}

	public String getRelHead2() {
		if (!lightverbs.contains(this.v[2])) {
			return lmma[this.v[2]];
		} else {
			for (int k = this.v[1] - 1; k >= this.v[0]; k--) {
				if (!lightverbs.contains(lmma[k]) && !RemoveStopwords.isStop(lmma[k])) {
					return lmma[k];
				}
			}
		}
		return lmma[this.v[2]];
	}

	static Set<String> lightverbs = new HashSet<String>();

	static {
		String[] lightverblist = new String[] { "make", "give", "do", "be", "have", "say", "think", "ask", "announce",
				"warn", "urge", "take", "would", "will", "may" };
		for (String v : lightverblist) {
			lightverbs.add(v);
		}
	}

	public String getPatternHeadWithPrep() {
		String pattern = this.getPattern();
		String head = this.getPatternHead();
		String key = head + "-[prep-";
		int idx0 = pattern.indexOf(key);
		if (idx0 >= 0) {
			int idx1 = pattern.indexOf(']', idx0);
			String sub = pattern.substring(idx0 + key.length(), idx1);
			return head + " " + sub;
		} else {
			return null;
		}
	}

	// public String getPatternHeadWithPrep() {
	// Tuple t = this;
	// String rel = t.getRel();
	// if (rel == null)
	// rel = t.getSubtree();
	// int headIdx = this.pHeadIdx;
	// if (headIdx + 1 < this.pos.length && pos[headIdx + 1].equals("IN")) {
	// String s = (lmma[headIdx] + " " + lmma[headIdx + 1]).toLowerCase();
	// if (rel.contains(s)) {
	// return s;
	// }
	// }
	// return null;
	// }

	/** not useful */
	public String getPatternHeadWithNextWord() {
		if (this.getRel() != null) {
			int headidx = this.v[2];
			if (headidx + 1 < this.v[1]) {
				return (lmma[headidx] + " " + lmma[headidx + 1]).toLowerCase();
			}
		} else {
			// Tuple t = this;
			// String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
			// LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(
			// t.tkn.length, t.deps);
			// List<Integer> path = new ArrayList<Integer>();
			// List<Integer> pathdir = new ArrayList<Integer>();
			// boolean success = ecml.findShortestPath(idxDeps, t.a1[2],
			// t.a2[2], path, pathdir);
			// if (success) {
			// // find last verb
			// int headpos = -1;
			// for (int i = 1; i < path.size() - 1; i++) {
			// if (t.pos[path.get(i)].startsWith("V")
			// && !lightverbs.contains(t.lmma[path.get(i)])) {
			// headpos = path.get(i);
			// break;
			// }
			// }
			// // if there is no verb on the path, just find the last word
			// if (headpos >= 0) {
			// for (int i = 0; i < deps.length; i++) {
			// if (deps[headpos][i] != null) {
			// return (lmma[headpos] + " " + lmma[i])
			// .toLowerCase();
			// }
			// }
			// }
			// }
		}
		return null;

	}

	public String getPatternHead() {
		if (this.getRel() != null) {
			return this.getRelHead();
		} else {
			// String []wordsInPath = this.wordsInShortestPath();
			Tuple t = this;
			if (pHead == null) {
				String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
				LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(t.tkn.length, t.deps);
				List<Integer> path = new ArrayList<Integer>();
				List<Integer> pathdir = new ArrayList<Integer>();
				boolean success = ecml.findShortestPath(idxDeps, t.a1[2], t.a2[2], path, pathdir);
				if (success) {
					// find last verb
					for (int i = 1; i < path.size() - 1; i++) {
						if (t.pos[path.get(i)].startsWith("V") && !lightverbs.contains(t.lmma[path.get(i)])) {
							pHeadIdx = path.get(i);
							pHead = t.lmma[path.get(i)].toLowerCase();
							break;
						}
					}
					// if there is no verb on the path, just find the last word
					if (pHead == null) {
						if (path.size() >= 3) {
							pHeadIdx = path.get(1);
							pHead = t.lmma[path.get(1)].toLowerCase();
						}
					}
				}
			}
			// if (patternHead == null) {
			// patternHead = "NULL";
			// }
			return pHead;
		}
	}

	public Set<String> getPatternWords() {
		Set<String> words = new HashSet<String>();
		if (this.getRel() != null) {
			for (int k = this.v[1] - 1; k >= this.v[0]; k--) {
				// if (!lightverbs.contains(lmma[k]) &&
				// !RemoveStopwords.isStop(lmma[k])) {
				words.add(lmma[k].toLowerCase());
				// }
			}
		} else {
			// String []wordsInPath = this.wordsInShortestPath();
			Tuple t = this;
			if (pHead == null) {
				String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
				LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(t.tkn.length, t.deps);
				List<Integer> path = new ArrayList<Integer>();
				List<Integer> pathdir = new ArrayList<Integer>();
				boolean success = ecml.findShortestPath(idxDeps, t.a1[2], t.a2[2], path, pathdir);
				if (success) {
					// find last verb
					for (int i = 1; i < path.size() - 1; i++) {
						words.add(t.lmma[path.get(i)].toLowerCase());
					}
				}
			}
		}
		Set<String> ret = new HashSet<String>();
		for (String w : words) {
			// if (!lightverbs.contains(w) && !RemoveStopwords.isStop(w)) {
			ret.add(w);
			// }
		}
		return ret;
	}

	public Set<String> getPatternVerbNoun() {
		Set<String> words = new HashSet<String>();
		if (this.getRel() != null) {
			for (int k = this.v[1] - 1; k >= this.v[0]; k--) {
				// if (!lightverbs.contains(lmma[k]) &&
				// !RemoveStopwords.isStop(lmma[k])) {
				if (pos[k].startsWith("V") || pos[k].startsWith("N"))
					words.add(lmma[k].toLowerCase());
				// }
			}
		} else {
			// String []wordsInPath = this.wordsInShortestPath();
			Tuple t = this;
			// if (pHead == null) {
			String[][] deps = RelationECML.getDeps(t.tkn.length, t.deps);
			LinkedList<int[]>[] idxDeps = RelationECML.indexDeps(t.tkn.length, t.deps);
			List<Integer> path = new ArrayList<Integer>();
			List<Integer> pathdir = new ArrayList<Integer>();
			boolean success = ecml.findShortestPath(idxDeps, t.a1[2], t.a2[2], path, pathdir);
			if (success) {
				// find last verb
				for (int i = 1; i < path.size() - 1; i++) {
					if (pos[i].startsWith("V") || pos[i].startsWith("N"))
						words.add(t.lmma[path.get(i)].toLowerCase());
				}
			}
			// }
		}
		Set<String> ret = new HashSet<String>();
		for (String w : words) {
			ret.add(w);
		}
		return ret;
	}

	public String getRelHead(WordGram wg) {
		String keyword = null;
		int count = 0;
		if (v[1] - v[0] <= 4) {
			for (int i = v[0]; i < v[1]; i++) {
				String w = lmma[i].toLowerCase();
				// if (pos[i].startsWith("V") || pos[i].startsWith("N")) {
				if (!w.endsWith("ly")) {
					int c = wg.getCount(lmma[i]);
					if (keyword == null || c < count) {
						keyword = w;
						count = c;
					}
				}
			}
		}
		if (keyword == null) {
			keyword = getRelHead();
		}
		// D.p(this.getRel(), keyword);
		return keyword;
	}

	// public String getRelHead() {
	// int headpos = this.v[2];
	// for (int i = v[0]; i < v[1]; i++) {
	// if (pos[i].startsWith("V") && !lmma[i].equals("be") &&
	// !lmma[i].equals("do")) {
	// headpos = i;
	// break;
	// }
	// }
	// return lmma[headpos];
	// }

	public String getArg1Head() {
		return lmma[this.a1[2]];
	}

	public String getArg2Head() {
		return lmma[this.a2[2]];
	}

	public String getArg1Lastword() {
		return lmma[this.a1[1] - 1];
	}

	public String getArg2Lastword() {
		return lmma[this.a2[1] - 1];
	}

	static HashSet<String> rbForLightVerbs = new HashSet<String>();

	static {
		String[] rbs = new String[] { "out", "off" };
		for (String r : rbs)
			rbForLightVerbs.add(r);
	}

	public String[] getArg1Arg2ArgPairToQueryTimeSeries() {
		String[] ret = new String[3];
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (int i = a1[1] - 2; i < a1[1]; i++) {
			if (i >= a1[0]) {
				sb1.append(lmma[i] + " ");
			}
		}
		for (int i = a2[1] - 2; i < a2[1]; i++) {
			if (i >= a2[0]) {
				sb2.append(lmma[i] + " ");
			}
		}
		ret[0] = sb1.toString().toLowerCase();
		ret[1] = sb2.toString().toLowerCase();
		ret[2] = ret[0] + "::" + ret[1];
		return ret;

	}

	public String getRelAsVariable() {
		String relOllie = getRelOllie();
		// StringBuilder sb = new StringBuilder();
		// for (int i = v[0]; i < v[1]; i++) {
		// sb.append(tkn[i] + " ");
		// }
		// relOllie = sb.toString().toLowerCase().trim();

		// int vhead = v[2];
		// Map<Integer, String> map = indexdep.row(vhead);
		// for (Entry<Integer, String> e : map.entrySet()) {
		// String type = e.getValue();
		// int d = e.getKey();
		// if (d >= v[0] && d < v[1])
		// continue;
		// if (type.startsWith("prep_")) {
		// String x = type.replace("prep_", "");
		// relOllie += " " + x;
		// } else if ((type.equals("prt") || type.equals("advmod"))
		// && rbForLightVerbs.contains(lmma[d])) {
		// relOllie += " " + lmma[d];
		// }
		// }
		return relOllie.replaceAll(" ", "_");
	}

	boolean isMD() {
		boolean isMD = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("MD")) {
				isMD = true;
				break;
			}
		}
		return isMD;
	}

	boolean isHAVE() {
		boolean isHAVE = false;
		int start = v[0];
		int end = v[1];
		if (lmma[v[0]].toLowerCase().equals("have")) {
			isHAVE = true;
		}
		return isHAVE;
	}

	boolean isNeg() {
		boolean isHAVE = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			String s = lmma[i].toLowerCase();
			if (s.equals("no") || s.equals("not")) {
				isHAVE = true;
				break;
			}
		}
		return isHAVE;
	}

	boolean hasNegBetweenArgs() {
		boolean ret = false;
		int start = Math.min(a1[1], a2[1]);
		int end = Math.max(a1[0], a2[0]);
		for (int i = start; i < end; i++) {
			String s = lmma[i].toLowerCase();
			if (s.equals("not") || s.equals("no") || s.equals("n't")) {
				ret = true;
			}
		}
		return ret;
	}

	boolean isPast() {
		boolean isPast = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("VBD")) {
				isPast = true;
				break;
			}
		}
		return isPast;
	}

	boolean isPresent() {
		boolean isPresent = false;
		int start = v[0];
		int end = v[1];
		for (int i = start; i < end; i++) {
			if (pos[i].equals("VBZ") || pos[i].equals("VBP")) {
				isPresent = true;
				break;
			}
		}
		return isPresent;
	}

	public String[] lmmaExceptTuple() {
		List<String> temp = new ArrayList<String>();
		for (int i = 0; i < lmma.length; i++) {
			if (i >= a1[0] && i < a1[1] || i >= a2[0] && i < a2[1] || i >= v[0] && i < v[1]) {
				continue;
			}
			temp.add(lmma[i]);
		}
		String[] ret = new String[temp.size()];
		for (int i = 0; i < temp.size(); i++) {
			ret[i] = temp.get(i);
		}
		return ret;
	}

	public boolean doesReverbHasNot() {
		for (int i = v[0]; i < v[1]; i++) {
			if (this.lmma[i].equals("not") || this.lmma[i].equals("never")) {
				return true;
			}
		}
		return false;
	}

	static HashSet<String> uncertainWords = new HashSet<String>();

	static {
		String[] words = new String[] { "will", "would", "want", "if", "?", "may", "might" };
		for (String w : words) {
			uncertainWords.add(w);
		}
	}

	public boolean doesTupleHasUncertainWord() {
		for (String w : this.tkn) {
			if (uncertainWords.contains(w)) {
				return true;
			}
		}
		return false;
	}

	public boolean isReverbUncertain() {
		String[] uncertainWords = new String[] { "may", "can", "will", "have", "want" };
		HashSet<String> uncertains = new HashSet<String>();
		for (String s : uncertainWords) {
			uncertains.add(s);
		}
		for (int i = v[0]; i < v[1]; i++) {
			if (uncertains.contains(this.lmma[i])) {
				return true;
			}
		}
		return false;
	}

	public void setTupleId(int tupleId) {
		this.tupleId = tupleId;
	}

	public int getTupleId() {
		return this.tupleId;
	}

	public Set<String> getPatternKeywords() {
		Tuple t = this;
		Set<String> keys = new HashSet<String>();
		keys.add(t.getPatternHead());
		String headprep = t.getPatternHeadWithPrep();
		if (headprep != null) {
			keys.add(headprep);
		}
		for (String kw : t.getPatternVerbNoun()) {
			keys.add(kw);
		}
		Set<String> ret = new HashSet<String>();
		for (String kw : keys) {
			if (!RemoveStopwords.isStop(kw) && !RemoveStopwords.isStopVerb(kw)) {
				ret.add(kw);
			}
		}
		return ret;
	}

	public void setEecname(String eecname) {
		this.eecname = eecname;
	}

	public String getEecname() {
		if (eecname == null) {
			eecname = this.getArg1() + "@" + this.getArg2() + "@" + this.date;

		}
		return eecname;
	}
}
