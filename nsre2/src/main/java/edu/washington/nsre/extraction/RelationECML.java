package edu.washington.nsre.extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;

public class RelationECML {

	static String serializedClassifier = "../model/ner-eng-ie.crf-3-all2008.ser.gz";
	static final int LongPathThresh = 5;

	// private AbstractSequenceClassifier classifier;

	public RelationECML() {
		// classifier = CRFClassifier
		// .getClassifierNoExceptions(serializedClassifier);
	}

	/**
	 * [ALAN, E., SHAMEER, Senior, Industry, Economist, Rinfret, Associates,
	 * New, York, ,, Dec., 9, ,, 1986] [NNP, NNP, NNP, NNP, NNP, NNP, NNP, NNPS,
	 * NNP, NNP, ,, NNP, CD, ,, CD] [4, 4, 4, 4, 7, 7, 7, 9, 9, -1, 9, 14, 11,
	 * 14, 9] [NAME, NAME, NAME, NAME, NAME, NAME, NAME, NAME, NAME, ROOT, P,
	 * NMOD, NMOD, P, APPO] [0, 5] ORGANIZATION [8, 10] LOCATION 4 9 path1 [4,
	 * 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1] path2 [9, -1, -1,
	 * -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]
	 * 
	 * dirs [->, ->] strs [, Associates] rels [NAME, NAME] middleDirs ->->
	 * middleRels [NAME]->[NAME]-> middleStrs [NAME]->Associates[NAME]->
	 * basicDir ORGANIZATION|->->|LOCATION basicDep
	 * ORGANIZATION|[NAME]->[NAME]->|LOCATION basicStr
	 * ORGANIZATION|[NAME]->Associates[NAME]->|LOCATION arg1dirs [->, ->, ->,
	 * ->] arg1deps [[NAME]->, [NAME]->, [NAME]->, [NAME]->] arg1strs
	 * [ALAN[NAME]->, E.[NAME]->, SHAMEER[NAME]->, Senior[NAME]->] arg2dirs [->,
	 * ->, ->] //get ride of "Associates" because it is in the path arg2deps
	 * [[NAME]->, [P]->, [APPO]->] arg2strs [[NAME]->New, [P]->,, [APPO]->1986]
	 * */
	public List<String> getFeatures(int sentenceId, String[] tokens, String[] postags, int[] depParents,
			String[] depTypes, int[] arg1Pos, int[] arg2Pos, String arg1ner, String arg2ner) {

		List<String> features = new ArrayList<String>();

		/** ner feature, such as LOCATION->PERSON */
		features.add(arg1ner + "->" + arg2ner);

		// it's easier to deal with first, second
		int[] first = arg1Pos, second = arg2Pos;
		String firstNer = arg1ner, secondNer = arg2ner;
		if (arg1Pos[0] > arg2Pos[0]) {
			second = arg1Pos;
			first = arg2Pos;
			firstNer = arg2ner;
			secondNer = arg1ner;
		}

		// define the inverse prefix
		String inv = (arg1Pos[0] > arg2Pos[0]) ? "inverse_true" : "inverse_false";

		// define the middle parts
		StringBuilder middleTokens = new StringBuilder();
		StringBuilder middleTags = new StringBuilder();
		for (int i = first[1]; i < second[0]; i++) {
			if (i > first[1]) {
				middleTokens.append(" ");
				middleTags.append(" ");
			}
			middleTokens.append(tokens[i]);
			middleTags.append(postags[i]);
		}

		if (second[0] - first[1] > 10) {
			middleTokens.setLength(0);
			middleTokens.append("*LONG*");

			// newly added
			middleTags.setLength(0);
			middleTags.append("*LONG*");
		}

		// define the prefixes and suffixes
		String[] prefixTokens = new String[2];
		String[] suffixTokens = new String[2];

		for (int i = 0; i < 2; i++) {
			int tokIndex = first[0] - i - 1;
			if (tokIndex < 0)
				prefixTokens[i] = "B_" + tokIndex;
			else
				prefixTokens[i] = tokens[tokIndex];
		}

		for (int i = 0; i < 2; i++) {
			int tokIndex = second[1] + i;
			if (tokIndex >= tokens.length)
				suffixTokens[i] = "B_" + (tokIndex - tokens.length + 1);
			else
				suffixTokens[i] = tokens[tokIndex];
		}

		String[] prefixes = new String[3];
		String[] suffixes = new String[3];

		prefixes[0] = suffixes[0] = "";
		prefixes[1] = prefixTokens[0];
		prefixes[2] = prefixTokens[1] + " " + prefixTokens[0];
		suffixes[1] = suffixTokens[0];
		suffixes[2] = suffixTokens[0] + " " + suffixTokens[1];

		// generate the features in the same order as in ecml data
		String mto = middleTokens.toString();
		String mta = middleTags.toString();

		features.add(inv + "|" + firstNer + "|" + mto + "|" + secondNer);
		features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[1]);
		features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[2]);

		features.add(inv + "|" + firstNer + "|" + mta + "|" + secondNer);
		features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mta + "|" + secondNer + "|" + suffixes[1]);
		features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mta + "|" + secondNer + "|" + suffixes[2]);

		// dependency features
		if (depParents == null || depParents.length < tokens.length)
			return features;

		// identify head words of arg1 and arg2
		// (start at end, while inside entity, jump)
		int head1 = arg1Pos[1] - 1;
		while (depParents[head1] >= arg1Pos[0] && depParents[head1] < arg1Pos[1])
			head1 = depParents[head1];
		int head2 = arg2Pos[1] - 1;
		// System.out.println(head1 + " " + head2);
		while (depParents[head2] >= arg2Pos[0] && depParents[head2] < arg2Pos[1])
			head2 = depParents[head2];

		// find path of dependencies from first to second
		int[] path1 = new int[tokens.length];
		for (int i = 0; i < path1.length; i++)
			path1[i] = -1;
		path1[0] = head1; // last token of first argument
		for (int i = 1; i < path1.length; i++) {
			path1[i] = depParents[path1[i - 1]];
			if (path1[i] == -1)
				break;
		}
		int[] path2 = new int[tokens.length];
		for (int i = 0; i < path2.length; i++)
			path2[i] = -1;
		path2[0] = head2; // last token of first argument
		for (int i = 1; i < path2.length; i++) {
			path2[i] = depParents[path2[i - 1]];
			if (path2[i] == -1)
				break;
		}
		int lca = -1;
		int lcaUp = 0, lcaDown = 0;
		outer: for (int i = 0; i < path1.length; i++)
			for (int j = 0; j < path2.length; j++) {
				if (path1[i] == -1 || path2[j] == -1) {
					break; // no path
				}
				if (path1[i] == path2[j]) {
					lca = path1[i];
					lcaUp = i;
					lcaDown = j;
					break outer;
				}
			}

		if (lca < 0)
			return features; // no dependency path (shouldn't happen)

		String[] dirs = new String[lcaUp + lcaDown];
		String[] strs = new String[lcaUp + lcaDown];
		String[] rels = new String[lcaUp + lcaDown];

		StringBuilder middleDirs = new StringBuilder();
		StringBuilder middleRels = new StringBuilder();
		StringBuilder middleStrs = new StringBuilder();

		if (lcaUp + lcaDown < 12) {

			for (int i = 0; i < lcaUp; i++) {
				dirs[i] = "->";
				strs[i] = i > 0 ? tokens[path1[i]] : "";
				rels[i] = depTypes[path1[i]];
				// System.out.println("[" + depTypes[path1[i]] + "]->");
			}
			for (int j = 0; j < lcaDown; j++) {
				// for (int j=lcaDown-1; j >= 0; j--) {
				dirs[lcaUp + j] = "<-";
				strs[lcaUp + j] = (lcaUp + j > 0) ? tokens[path2[lcaDown - j]] : ""; // word
																						// taken
																						// from
																						// above
				rels[lcaUp + j] = depTypes[path2[lcaDown - j]];
				// System.out.println("[" + depTypes[path2[j]] + "]<-");
			}

			for (int i = 0; i < dirs.length; i++) {
				middleDirs.append(dirs[i]);
				middleRels.append("[" + rels[i] + "]" + dirs[i]);
				middleStrs.append(strs[i] + "[" + rels[i] + "]" + dirs[i]);
			}
		} else {
			middleDirs.append("*LONG-PATH*");
			middleRels.append("*LONG-PATH*");
			middleStrs.append("*LONG-PATH*");
		}

		String basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
		String basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
		String basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;

		// new left and right windows: all elements pointing to first arg, but
		// not on path
		// List<Integer> lws = new ArrayList<Integer>();
		// List<Integer> rws = new ArrayList<Integer>();

		List<String> arg1dirs = new ArrayList<String>();
		List<String> arg1deps = new ArrayList<String>();
		List<String> arg1strs = new ArrayList<String>();
		List<String> arg2dirs = new ArrayList<String>();
		List<String> arg2deps = new ArrayList<String>();
		List<String> arg2strs = new ArrayList<String>();

		// pointing out of argument
		for (int i = 0; i < tokens.length; i++) {
			// make sure itself is not either argument
			// if (i >= first[0] && i < first[1]) continue;
			// if (i >= second[0] && i < second[1]) continue;
			if (i == head1)
				continue;
			if (i == head2)
				continue;

			// make sure i is not on path
			boolean onPath = false;
			for (int j = 0; j < lcaUp; j++)
				if (path1[j] == i)
					onPath = true;
			for (int j = 0; j < lcaDown; j++)
				if (path2[j] == i)
					onPath = true;
			if (onPath)
				continue;
			// make sure i points to first or second arg
			// if (depParents[i] >= first[0] && depParents[i] < first[1])
			// lws.add(i);
			// if (depParents[i] >= second[0] && depParents[i] < second[1])
			// rws.add(i);
			if (depParents[i] == head1) {
				// lws.add(i);
				arg1dirs.add("->");
				arg1deps.add("[" + depTypes[i] + "]->");
				arg1strs.add(tokens[i] + "[" + depTypes[i] + "]->");
			}
			if (depParents[i] == head2) {
				// rws.add(i);
				arg2dirs.add("->");
				arg2deps.add("[" + depTypes[i] + "]->");
				arg2strs.add("[" + depTypes[i] + "]->" + tokens[i]);
			}
		}

		// case 1: pointing into the argument pair structure (always attach to
		// lhs):
		// pointing from arguments
		if (lcaUp == 0 && depParents[head1] != -1 || depParents[head1] == head2) {
			arg1dirs.add("<-");
			arg1deps.add("[" + depTypes[head1] + "]<-");
			arg1strs.add(tokens[head1] + "[" + depTypes[head1] + "]<-");

			if (depParents[depParents[head1]] != -1) {
				arg1dirs.add("<-");
				arg1deps.add("[" + depTypes[depParents[head1]] + "]<-");
				arg1strs.add(tokens[depParents[head1]] + "[" + depTypes[depParents[head1]] + "]<-");
			}
		}
		// if parent is not on path or if parent is
		if (lcaDown == 0 && depParents[head2] != -1 || depParents[head2] == head1) { // should
																						// this
																						// actually
																						// attach
																						// to
																						// rhs???
			arg1dirs.add("<-");
			arg1deps.add("[" + depTypes[head2] + "]<-");
			arg1strs.add(tokens[head2] + "[" + depTypes[head2] + "]<-");

			if (depParents[depParents[head2]] != -1) {
				arg1dirs.add("<-");
				arg1deps.add("[" + depTypes[depParents[head2]] + "]<-");
				arg1strs.add(tokens[depParents[head2]] + "[" + depTypes[depParents[head2]] + "]<-");
			}
		}

		// case 2: pointing out of argument

		// features.add("dir:" + basicDir);
		// features.add("dep:" + basicDep);

		// left and right, including word
		for (String w1 : arg1strs)
			for (String w2 : arg2strs)
				features.add("str:" + w1 + "|" + basicStr + "|" + w2);

		/*
		 * for (int lw : lws) { for (int rw : rws) { features.add("str:" +
		 * tokens[lw] + "[" + depTypes[lw] + "]<-" + "|" + basicStr + "|" + "["
		 * + depTypes[rw] + "]->" + tokens[rw]); } }
		 */

		// only left
		for (int i = 0; i < arg1dirs.size(); i++) {
			features.add("str:" + arg1strs.get(i) + "|" + basicStr);
			features.add("dep:" + arg1deps.get(i) + "|" + basicDep);
			features.add("dir:" + arg1dirs.get(i) + "|" + basicDir);
		}

		// only right
		for (int i = 0; i < arg2dirs.size(); i++) {
			features.add("str:" + basicStr + "|" + arg2strs.get(i));
			features.add("dep:" + basicDep + "|" + arg2deps.get(i));
			features.add("dir:" + basicDir + "|" + arg2dirs.get(i));
		}

		features.add("str:" + basicStr);

		return features;
	}

	/** deps[17][22]="prep-at" 17:professor; 22:institute */
	public List<String> getFeaturesStanford(int sentenceId, String[] tokens, String[] postags, String[][] deps,
			LinkedList<int[]>[] lllist, int[] arg1Pos, int[] arg2Pos, String arg1ner, String arg2ner) {
		List<String> features = new ArrayList<String>();
		/** ner feature, such as LOCATION->PERSON */
		features.add(arg1ner + "->" + arg2ner);
		// it's easier to deal with first, second
		int[] first = arg1Pos, second = arg2Pos;
		String firstNer = arg1ner, secondNer = arg2ner;
		if (arg1Pos[0] > arg2Pos[0]) {
			second = arg1Pos;
			first = arg2Pos;
			firstNer = arg2ner;
			secondNer = arg1ner;
		}

		// define the inverse prefix
		String inv = (arg1Pos[0] > arg2Pos[0]) ? "inverse_true" : "inverse_false";

		// define the middle parts
		StringBuilder middleTokens = new StringBuilder();
		StringBuilder middleTags = new StringBuilder();
		for (int i = first[1]; i < second[0]; i++) {
			if (i > first[1]) {
				middleTokens.append(" ");
				middleTags.append(" ");
			}
			middleTokens.append(tokens[i]);
			middleTags.append(postags[i]);
		}

		if (second[0] - first[1] > 10) {
			middleTokens.setLength(0);
			middleTokens.append("*LONG*");

			// newly added
			middleTags.setLength(0);
			middleTags.append("*LONG*");
		}

		// define the prefixes and suffixes
		String[] prefixTokens = new String[2];
		String[] suffixTokens = new String[2];

		for (int i = 0; i < 2; i++) {
			int tokIndex = first[0] - i - 1;
			if (tokIndex < 0)
				prefixTokens[i] = "B_" + tokIndex;
			else
				prefixTokens[i] = tokens[tokIndex];
		}

		for (int i = 0; i < 2; i++) {
			int tokIndex = second[1] + i;
			if (tokIndex >= tokens.length)
				suffixTokens[i] = "B_" + (tokIndex - tokens.length + 1);
			else
				suffixTokens[i] = tokens[tokIndex];
		}

		String[] prefixes = new String[3];
		String[] suffixes = new String[3];

		prefixes[0] = suffixes[0] = "";
		prefixes[1] = prefixTokens[0];
		prefixes[2] = prefixTokens[1] + " " + prefixTokens[0];
		suffixes[1] = suffixTokens[0];
		suffixes[2] = suffixTokens[0] + " " + suffixTokens[1];

		// generate the features in the same order as in ecml data
		String mto = middleTokens.toString();
		String mta = middleTags.toString();

		features.add(inv + "|" + firstNer + "|" + mto + "|" + secondNer);
		features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[1]);
		features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mto + "|" + secondNer + "|" + suffixes[2]);

		features.add(inv + "|" + firstNer + "|" + mta + "|" + secondNer);
		features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mta + "|" + secondNer + "|" + suffixes[1]);
		features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mta + "|" + secondNer + "|" + suffixes[2]);

		if (deps == null)
			return features;

		boolean[] stupid = getStupidTokens(deps);
		// identify head words of arg1 and arg2
		// (start at end, while inside entity, jump)
		int head1 = findHeadOfNounPhrase(arg1Pos, deps, stupid);
		int head2 = findHeadOfNounPhrase(arg2Pos, deps, stupid);

		List<Integer> path = new ArrayList<Integer>();
		List<Integer> pathdir = new ArrayList<Integer>();
		boolean success = findShortestPath(lllist, head1, head2, path, pathdir);
		if (!success)
			return features;// no dependency feature

		List<String> dirs = new ArrayList<String>();
		List<String> strs = new ArrayList<String>();
		List<String> rels = new ArrayList<String>();

		StringBuilder middleDirs = new StringBuilder();
		StringBuilder middleRels = new StringBuilder();
		StringBuilder middleStrs = new StringBuilder();

		String basicDir = "";
		String basicDep = "";
		String basicStr = "";

		List<String> arg1dirs = new ArrayList<String>();
		List<String> arg1deps = new ArrayList<String>();
		List<String> arg1strs = new ArrayList<String>();
		List<String> arg2dirs = new ArrayList<String>();
		List<String> arg2deps = new ArrayList<String>();
		List<String> arg2strs = new ArrayList<String>();
		if (path.size() < 12) {
			int prev = path.get(path.size() - 1);
			for (int i = path.size() - 2; i >= 0; i--) {
				int cur = path.get(i);
				int direct = pathdir.get(i);
				if (direct == 1) {
					dirs.add("->");
					rels.add(deps[prev][cur]);
				} else if (direct == -1) {
					dirs.add("<-");
					rels.add(deps[cur][prev]);
				}
				strs.add(tokens[cur]);
				prev = cur;
			}

			for (int i = 0; i < strs.size(); i++) {
				middleDirs.append(dirs.get(i));
				middleRels.append(dirs.get(i) + "[" + rels.get(i) + "]");
				if (i == strs.size() - 1) {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]");
				} else {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]" + strs.get(i));
				}
			}
		} else {
			middleDirs.append("*LONG-PATH*");
			middleRels.append("*LONG-PATH*");
			middleStrs.append("*LONG-PATH*");
		}

		basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
		basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
		basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;

		// arg1dirs,arg1deps,arg1strs
		Set<Integer> path2set = new HashSet<Integer>();
		path2set.addAll(path);
		LinkedList<int[]> l1 = lllist[head1];
		for (int[] x : l1) {
			if (x[0] == head1 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg1dirs.add("<-");
				arg1deps.add("<-[" + deps[head1][x[1]] + "]");
				arg1strs.add("<-" + "[" + deps[head1][x[1]] + "]" + tokens[x[1]]);
			}
		}
		LinkedList<int[]> l2 = lllist[head2];
		for (int[] x : l2) {
			if (x[0] == head2 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg2dirs.add("->");
				arg2deps.add("->[" + deps[head2][x[1]] + "]");
				arg2strs.add("->" + "[" + deps[head2][x[1]] + "]" + tokens[x[1]]);
			}
		}

		// case 2: pointing out of argument

		// features.add("dir:" + basicDir);
		// features.add("dep:" + basicDep);

		// left and right, including word
		for (String w1 : arg1strs)
			for (String w2 : arg2strs)
				features.add("str:" + w1 + "|" + basicStr + "|" + w2);

		// only left
		for (int i = 0; i < arg1dirs.size(); i++) {
			features.add("str:" + arg1strs.get(i) + "|" + basicStr);
			features.add("dep:" + arg1deps.get(i) + "|" + basicDep);
			features.add("dir:" + arg1dirs.get(i) + "|" + basicDir);
		}

		// only right
		for (int i = 0; i < arg2dirs.size(); i++) {
			features.add("str:" + basicStr + "|" + arg2strs.get(i));
			features.add("dep:" + basicDep + "|" + arg2deps.get(i));
			features.add("dir:" + basicDir + "|" + arg2dirs.get(i));
		}

		features.add("str:" + basicStr);
		return features;
	}

	public static String[][] getDeps(int tokensize, List<SentDep> depends) {
		if (depends == null || depends.size() == 0)
			return null;

		String[][] deps = new String[tokensize][tokensize];
		for (int i = 0; i < depends.size(); i++) {
			SentDep sd = depends.get(i);
			int a = sd.d;
			int b = sd.g;
			if (a != b)// it sucks that sometimes 35 conj-and 35 happens
				deps[a][b] = sd.t;

		}
		return deps;
	}

	public static LinkedList<int[]>[] indexDeps(int tokensize, List<SentDep> depends) {
		LinkedList<int[]>[] ll = new LinkedList[tokensize];
		for (int i = 0; i < ll.length; i++) {
			ll[i] = new LinkedList<int[]>();
		}
		for (int i = 0; i < depends.size(); i++) {
			SentDep sd = depends.get(i);
			// String[] t = tuples[i].split(" ");
			int a = sd.d;
			int b = sd.g;
			if (a != b) {// it sucks that sometimes 35 conj-and 35 happens
				ll[a].add(new int[] { a, b });
				ll[b].add(new int[] { a, b });
			}
		}
		return ll;

	}

	public List<String> getFeaturesStanfordSimple(int sentenceId, String[] tokens, String[] postags, String[][] deps,
			LinkedList<int[]>[] lllist, int[] arg1Pos, int[] arg2Pos, String arg1ner, String arg2ner) {
		List<String> features = new ArrayList<String>();
		/** ner feature, such as LOCATION->PERSON */
		features.add(arg1ner + "->" + arg2ner);
		// it's easier to deal with first, second
		int[] first = arg1Pos, second = arg2Pos;
		String firstNer = arg1ner, secondNer = arg2ner;
		if (arg1Pos[0] > arg2Pos[0]) {
			second = arg1Pos;
			first = arg2Pos;
			firstNer = arg2ner;
			secondNer = arg1ner;
		}

		// define the inverse prefix
		String inv = (arg1Pos[0] > arg2Pos[0]) ? "inverse_true" : "inverse_false";

		// define the middle parts
		StringBuilder middleTokens = new StringBuilder();
		StringBuilder middleTags = new StringBuilder();
		for (int i = first[1]; i < second[0]; i++) {
			if (i > first[1]) {
				middleTokens.append(" ");
				middleTags.append(" ");
			}
			middleTokens.append(tokens[i]);
			middleTags.append(postags[i]);
		}

		if (second[0] - first[1] > 10) {
			middleTokens.setLength(0);
			middleTokens.append("*LONG*");

			// newly added
			middleTags.setLength(0);
			middleTags.append("*LONG*");
		}

		// define the prefixes and suffixes
		String[] prefixTokens = new String[2];
		String[] suffixTokens = new String[2];

		for (int i = 0; i < 2; i++) {
			int tokIndex = first[0] - i - 1;
			if (tokIndex < 0)
				prefixTokens[i] = "B_" + tokIndex;
			else
				prefixTokens[i] = tokens[tokIndex];
		}

		for (int i = 0; i < 2; i++) {
			int tokIndex = second[1] + i;
			if (tokIndex >= tokens.length)
				suffixTokens[i] = "B_" + (tokIndex - tokens.length + 1);
			else
				suffixTokens[i] = tokens[tokIndex];
		}

		String[] prefixes = new String[3];
		String[] suffixes = new String[3];

		prefixes[0] = suffixes[0] = "";
		prefixes[1] = prefixTokens[0];
		prefixes[2] = prefixTokens[1] + " " + prefixTokens[0];
		suffixes[1] = suffixTokens[0];
		suffixes[2] = suffixTokens[0] + " " + suffixTokens[1];

		// generate the features in the same order as in ecml data
		String mto = middleTokens.toString();
		String mta = middleTags.toString();

		// features.add(inv + "|" + firstNer + "|" + mto + "|" + secondNer);
		// features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mto +
		// "|" + secondNer + "|" + suffixes[1]);
		// features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mto +
		// "|" + secondNer + "|" + suffixes[2]);
		//
		// features.add(inv + "|" + firstNer + "|" + mta + "|" + secondNer);
		// features.add(inv + "|" + prefixes[1] + "|" + firstNer + "|" + mta +
		// "|" + secondNer + "|" + suffixes[1]);
		// features.add(inv + "|" + prefixes[2] + "|" + firstNer + "|" + mta +
		// "|" + secondNer + "|" + suffixes[2]);

		if (deps == null)
			return features;

		boolean[] stupid = getStupidTokens(deps);
		// identify head words of arg1 and arg2
		// (start at end, while inside entity, jump)
		int head1 = findHeadOfNounPhrase(arg1Pos, deps, stupid);
		int head2 = findHeadOfNounPhrase(arg2Pos, deps, stupid);

		List<Integer> path = new ArrayList<Integer>();
		List<Integer> pathdir = new ArrayList<Integer>();
		boolean success = findShortestPath(lllist, head1, head2, path, pathdir);
		if (!success)
			return features;// no dependency feature

		List<String> dirs = new ArrayList<String>();
		List<String> strs = new ArrayList<String>();
		List<String> rels = new ArrayList<String>();

		StringBuilder middleDirs = new StringBuilder();
		StringBuilder middleRels = new StringBuilder();
		StringBuilder middleStrs = new StringBuilder();

		String basicDir = "";
		String basicDep = "";
		String basicStr = "";

		List<String> arg1dirs = new ArrayList<String>();
		List<String> arg1deps = new ArrayList<String>();
		List<String> arg1strs = new ArrayList<String>();
		List<String> arg2dirs = new ArrayList<String>();
		List<String> arg2deps = new ArrayList<String>();
		List<String> arg2strs = new ArrayList<String>();
		if (path.size() <= 3) {
			int prev = path.get(path.size() - 1);
			for (int i = path.size() - 2; i >= 0; i--) {
				int cur = path.get(i);
				int direct = pathdir.get(i);
				if (direct == 1) {
					dirs.add("->");
					rels.add(deps[prev][cur]);
				} else if (direct == -1) {
					dirs.add("<-");
					rels.add(deps[cur][prev]);
				}
				strs.add(tokens[cur]);
				prev = cur;
			}

			for (int i = 0; i < strs.size(); i++) {
				middleDirs.append(dirs.get(i));
				middleRels.append(dirs.get(i) + "[" + rels.get(i) + "]");
				if (i == strs.size() - 1) {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]");
				} else {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]" + strs.get(i));
				}
			}
		} else {
			return features;// no dependency feature
			// middleDirs.append("*LONG-PATH*");
			// middleRels.append("*LONG-PATH*");
			// middleStrs.append("*LONG-PATH*");
		}

		basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
		basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
		basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;

		// arg1dirs,arg1deps,arg1strs
		Set<Integer> path2set = new HashSet<Integer>();
		path2set.addAll(path);
		LinkedList<int[]> l1 = lllist[head1];
		for (int[] x : l1) {
			if (x[0] == head1 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg1dirs.add("<-");
				arg1deps.add("<-[" + deps[head1][x[1]] + "]");
				arg1strs.add("<-" + "[" + deps[head1][x[1]] + "]" + tokens[x[1]]);
			}
		}
		LinkedList<int[]> l2 = lllist[head2];
		for (int[] x : l2) {
			if (x[0] == head2 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg2dirs.add("->");
				arg2deps.add("->[" + deps[head2][x[1]] + "]");
				arg2strs.add("->" + "[" + deps[head2][x[1]] + "]" + tokens[x[1]]);
			}
		}

		// case 2: pointing out of argument

		// features.add("dir:" + basicDir);
		// features.add("dep:" + basicDep);

		// left and right, including word
		for (String w1 : arg1strs)
			for (String w2 : arg2strs)
				features.add("str:" + w1 + "|" + basicStr + "|" + w2);

		// only left
		for (int i = 0; i < arg1dirs.size(); i++) {
			// features.add("str:" + arg1strs.get(i) + "|" + basicStr);
			// features.add("dep:" + arg1deps.get(i) + "|" + basicDep);
			// features.add("dir:" + arg1dirs.get(i) + "|" + basicDir);
		}

		// only right
		for (int i = 0; i < arg2dirs.size(); i++) {
			// features.add("str:" + basicStr + "|" + arg2strs.get(i));
			// features.add("dep:" + basicDep + "|" + arg2deps.get(i));
			// features.add("dir:" + basicDir + "|" + arg2dirs.get(i));
		}

		features.add("str:" + basicStr);
		return features;
	}

	public int findHeadOfNounPhrase(int[] argpos, String[][] deps, boolean[] isStupidToken) {
		int tokensize = deps.length;
		boolean[] r = new boolean[tokensize];
		for (int i = 0; i < r.length; i++) {
			r[i] = true;
		}
		for (int i = argpos[0]; i < argpos[1]; i++) {
			// if token i has parent between argpos[0]-argpos[1]
			for (int j = argpos[0]; j < argpos[1]; j++) {
				if (deps[j][i] != null) {// j is the parent of i, j is more
											// important
					r[i] = false;
				}
			}
		}
		for (int k = argpos[1] - 1; k >= argpos[0]; k--) {
			if (r[k] && !isStupidToken[k])
				return k;
		}
		/**
		 * Actually it is possible, if the token itself is stupid, like a number
		 */
		// System.err.println("all ner tokens has parent inside the ner, not possible");
		return argpos[1] - 1;// by default
	}

	public void testFindHeadOfNounPhrase(DW dw, int[] argpos, String[][] deps, String[] tokens,
			boolean[] isStupidToken, String[] tuples) {
		int head = findHeadOfNounPhrase(argpos, deps, isStupidToken);
		head = findHeadOfNounPhrase(argpos, deps, isStupidToken);
		if (argpos[1] != argpos[0] + 1) {
			// not simple case
			StringBuilder original = new StringBuilder();
			for (int k = argpos[0]; k < argpos[1]; k++)
				original.append(tokens[k] + " ");
			dw.write(original.toString(), tokens[head]);
		}
	}

	static boolean[] getStupidTokens(String[][] deps) {
		boolean[] r = new boolean[deps.length];
		for (int i = 0; i < r.length; i++)
			r[i] = true;
		for (int i = 0; i < r.length; i++) {
			for (int j = 0; j < r.length; j++) {
				if (deps[i][j] != null || deps[j][i] != null) {
					r[i] = false;
					r[j] = false;
				}
			}
		}
		return r;
	}

	public boolean findShortestPath(LinkedList<int[]>[] lllist, int head1, int head2, List<Integer> path,
			List<Integer> pathdir) {
		if (head1 == head2)
			return false;
		// result should include basicDir, basicDep, basicStr (no NER)
		class TokenIdxDir {
			int token;
			int prev;
			int dir;

			public TokenIdxDir(int token, int prev, int dir) {
				this.token = token;
				this.prev = prev;
				this.dir = dir;
			}

			public String toString() {
				return token + "\t" + prev + "\t" + dir;
			}
		}
		List<TokenIdxDir> list = new ArrayList<TokenIdxDir>();
		HashSet<Integer> inelements = new HashSet<Integer>();
		list.add(new TokenIdxDir(head1, -1, 0));
		int k = 0;

		outer: while (k < list.size()) {
			TokenIdxDir cur = list.get(k);
			LinkedList<int[]> ll = lllist[cur.token];
			for (int j = 0; j < ll.size(); j++) {
				int[] ab = ll.get(j);
				int other = -1;
				if (ab[0] == cur.token) {
					other = ab[1];
				} else {
					other = ab[0];
				}
				if (inelements.contains(other)) {
					continue;
				}
				inelements.add(other);
				if (ab[0] == cur.token) {
					list.add(new TokenIdxDir(other, k, 1));
				} else {
					list.add(new TokenIdxDir(other, k, -1));
				}
				if (other == head2) {
					break outer;
				}
			}
			// for (int j = 0; j < deps.length; j++) {
			// if (deps[cur.token][j] != null || deps[cur.token][j] != null)
			// {//cur token has children j
			// if (inelements.contains(j))//make sure the same token will not be
			// added into the list twice
			// continue;
			// inelements.add(j);
			// if (deps[cur.token][j] != null) {
			// list.add(new TokenIdxDir(j, k, 1));
			// } else {
			// list.add(new TokenIdxDir(j, k, -1));
			// }
			// if (j == head2) {
			// break outer;
			// }
			// }
			// }
			k++;
		}
		if (list.get(list.size() - 1).token == head2) {
			// find the path!!! track it
			int cur = list.size() - 1;
			while (cur >= 0) {
				path.add(list.get(cur).token);
				pathdir.add(list.get(cur).dir);
				cur = list.get(cur).prev;
			}
			return true;
		} else {
			return false;
		}
	}

	public void testFindPath(DW dw, int[] arg1pos, int[] arg2pos, String[][] deps,
			LinkedList<int[]>[] llist, String[] tokens, boolean[] isStupidToken, String[] tuples) {
		int head1 = findHeadOfNounPhrase(arg1pos, deps, isStupidToken);
		int head2 = findHeadOfNounPhrase(arg2pos, deps, isStupidToken);
		List<Integer> path = new ArrayList<Integer>();
		List<Integer> pathdir = new ArrayList<Integer>();
		boolean success = findShortestPath(llist, head1, head2, path, pathdir);
		StringBuilder sb = new StringBuilder();
		if (success) {
			int prev = path.get(path.size() - 1);
			sb.append(tokens[path.get(path.size() - 1)]);
			for (int i = path.size() - 2; i >= 0; i--) {
				int cur = path.get(i);
				int direct = pathdir.get(i);
				if (direct == 1) {
					sb.append("-" + deps[prev][cur] + "->");
					if (deps[prev][cur] == null) {
						System.err.println("should not be null!!!");
					}
				} else if (direct == -1) {
					sb.append("<-" + deps[cur][prev] + "-");
					if (deps[cur][prev] == null) {
						System.err.println("should not be null!!!");
					}
				}
				sb.append(tokens[cur]);
				prev = cur;
			}
		}
		StringBuilder senSB = new StringBuilder();
		for (String t : tokens)
			senSB.append(t + " ");
		dw.write(senSB.toString(), tokens[head1], tokens[head2], sb.toString());
	}

	public void testBasicDirDepStr(DW dw, int[] arg1pos, int[] arg2pos, String[][] deps,
			LinkedList<int[]>[] lllist, String[] tokens, boolean[] isStupidToken, String[] tuples, String arg1ner,
			String arg2ner) {
		StringBuilder senSB = new StringBuilder();
		for (String t : tokens)
			senSB.append(t + " ");

		int head1 = findHeadOfNounPhrase(arg1pos, deps, isStupidToken);
		int head2 = findHeadOfNounPhrase(arg2pos, deps, isStupidToken);
		List<Integer> path = new ArrayList<Integer>();
		List<Integer> pathdir = new ArrayList<Integer>();
		boolean success = findShortestPath(lllist, head1, head2, path, pathdir);
		List<String> dirs = new ArrayList<String>();
		List<String> strs = new ArrayList<String>();
		List<String> rels = new ArrayList<String>();

		StringBuilder middleDirs = new StringBuilder();
		StringBuilder middleRels = new StringBuilder();
		StringBuilder middleStrs = new StringBuilder();

		String basicDir = "";
		String basicDep = "";
		String basicStr = "";

		List<String> arg1dirs = new ArrayList<String>();
		List<String> arg1deps = new ArrayList<String>();
		List<String> arg1strs = new ArrayList<String>();
		List<String> arg2dirs = new ArrayList<String>();
		List<String> arg2deps = new ArrayList<String>();
		List<String> arg2strs = new ArrayList<String>();
		if (!success) {
			dw.write(senSB.toString(), "*NOPATH*");
			return;// no dependency feature
		}
		if (path.size() < 12) {
			int prev = path.get(path.size() - 1);
			for (int i = path.size() - 2; i >= 0; i--) {
				int cur = path.get(i);
				int direct = pathdir.get(i);
				if (direct == 1) {
					dirs.add("->");
					rels.add(deps[prev][cur]);
				} else if (direct == -1) {
					dirs.add("<-");
					rels.add(deps[cur][prev]);
				}
				strs.add(tokens[cur]);
				prev = cur;
			}

			for (int i = 0; i < strs.size(); i++) {
				middleDirs.append(dirs.get(i));
				middleRels.append(dirs.get(i) + "[" + rels.get(i) + "]");
				if (i == strs.size() - 1) {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]");
				} else {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]" + strs.get(i));
				}
			}
		} else {
			middleDirs.append("*LONG-PATH*");
			middleRels.append("*LONG-PATH*");
			middleStrs.append("*LONG-PATH*");
		}

		basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
		basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
		basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;
		dw.write(senSB.toString(), tokens[head1], tokens[head2], basicDir, basicDep, basicStr);

	}

	public void testExtendDirDepStr(DW dw, int[] arg1pos, int[] arg2pos, String[][] deps,
			LinkedList<int[]>[] lllist, String[] tokens, boolean[] isStupidToken, String[] tuples, String arg1ner,
			String arg2ner) {
		StringBuilder senSB = new StringBuilder();
		for (String t : tokens)
			senSB.append(t + " ");

		int head1 = findHeadOfNounPhrase(arg1pos, deps, isStupidToken);
		int head2 = findHeadOfNounPhrase(arg2pos, deps, isStupidToken);
		List<Integer> path = new ArrayList<Integer>();
		List<Integer> pathdir = new ArrayList<Integer>();
		boolean success = findShortestPath(lllist, head1, head2, path, pathdir);
		List<String> dirs = new ArrayList<String>();
		List<String> strs = new ArrayList<String>();
		List<String> rels = new ArrayList<String>();

		StringBuilder middleDirs = new StringBuilder();
		StringBuilder middleRels = new StringBuilder();
		StringBuilder middleStrs = new StringBuilder();

		String basicDir = "";
		String basicDep = "";
		String basicStr = "";

		List<String> arg1dirs = new ArrayList<String>();
		List<String> arg1deps = new ArrayList<String>();
		List<String> arg1strs = new ArrayList<String>();
		List<String> arg2dirs = new ArrayList<String>();
		List<String> arg2deps = new ArrayList<String>();
		List<String> arg2strs = new ArrayList<String>();
		if (!success) {
			dw.write(senSB.toString(), "*NOPATH*");
			return;// no dependency feature
		}
		if (path.size() < 12) {
			int prev = path.get(path.size() - 1);
			for (int i = path.size() - 2; i >= 0; i--) {
				int cur = path.get(i);
				int direct = pathdir.get(i);
				if (direct == 1) {
					dirs.add("->");
					rels.add(deps[prev][cur]);
				} else if (direct == -1) {
					dirs.add("<-");
					rels.add(deps[cur][prev]);
				}
				strs.add(tokens[cur]);
				prev = cur;
			}

			for (int i = 0; i < strs.size(); i++) {
				middleDirs.append(dirs.get(i));
				middleRels.append(dirs.get(i) + "[" + rels.get(i) + "]");
				if (i == strs.size() - 1) {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]");
				} else {
					middleStrs.append(dirs.get(i) + "[" + rels.get(i) + "]" + strs.get(i));
				}
			}
		} else {
			middleDirs.append("*LONG-PATH*");
			middleRels.append("*LONG-PATH*");
			middleStrs.append("*LONG-PATH*");
		}

		basicDir = arg1ner + "|" + middleDirs.toString() + "|" + arg2ner;
		basicDep = arg1ner + "|" + middleRels.toString() + "|" + arg2ner;
		basicStr = arg1ner + "|" + middleStrs.toString() + "|" + arg2ner;

		// arg1dirs,arg1deps,arg1strs
		Set<Integer> path2set = new HashSet<Integer>();
		path2set.addAll(path);
		LinkedList<int[]> l1 = lllist[head1];
		for (int[] x : l1) {
			if (x[0] == head1 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg1dirs.add("<-");
				arg1deps.add("<-[" + deps[head1][x[1]] + "]");
				arg1strs.add("<-" + "[" + deps[head1][x[1]] + "]" + tokens[x[1]]);
			}
		}
		LinkedList<int[]> l2 = lllist[head2];
		for (int[] x : l2) {
			if (x[0] == head2 && !path2set.contains(x[1])) {// head1 is more
															// important than
															// x[1]
				arg2dirs.add("->");
				arg2deps.add("->[" + deps[head2][x[1]] + "]");
				arg2strs.add("->" + "[" + deps[head2][x[1]] + "]" + tokens[x[1]]);
			}
		}

		dw.write(senSB.toString(), tokens[head1], tokens[head2], basicStr, arg1dirs + ";;", arg1deps + ";;", arg1strs
				+ ";;", arg2dirs + ";;", arg2deps + ";;", arg2strs + ";;");
	}

}
