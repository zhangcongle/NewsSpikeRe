package edu.washington.nsre.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.washington.nsre.util.*;
import edu.washington.nsre.ilp.IntegerLinearProgramming;
import edu.washington.nsre.stanfordtools.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Table.Cell;
import com.google.gson.Gson;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.IntCounter;

public class NewsSpikeExtractor {
	static Gson gson = new Gson();

	static Set<String> neg = new HashSet<String>();
	static {
		neg.add("no");
		neg.add("not");
		neg.add("never");
		neg.add("refuse");
		neg.add("cancel");
	}

	public static List<ConnectedComponent> featurize(
			String inputParallel,
			String inputCandidates,
			String dirGenerate) {
		if (!(new File(dirGenerate)).exists()) {
			(new File(dirGenerate)).mkdir();
		}
		List<ConnectedComponent> ccs = new ArrayList<ConnectedComponent>();
		// HashMap<String, Eec> eecname2eec = new HashMap<String, Eec>();
		Stat stat = new Stat();
		generateConnectedComponents(ccs, stat, inputParallel,
				inputCandidates, dirGenerate + "/generateConnectedComponent");
		for (ConnectedComponent cc : ccs) {
			featurizePhraseWordpair(cc);
			featurizePhraseContainsNot(cc);
			featurizePhraseExtendedwordpair(cc);
			featurizeEecStanfordNer(cc);
			featurizeEecFineNer(cc);
			featurizeEecFineMaxNer(cc);
			featurizePairwise(cc, stat);
		}

		// featurizeInverseOverlap(ccs, stat); don't do this now!
		// debug
		DW dw = new DW(dirGenerate + "/feature.phrase");
		DW dw2 = new DW(dirGenerate + "/feature.eec");
		DW dw3 = new DW(dirGenerate + "/feature.cross");
		for (ConnectedComponent cc : ccs) {
			// StringBuilder sb = new StringBuilder();
			dw.write(cc.ccid, Util.counter2jsonstr(cc.phraseFactor.features),
					cc.eventtype.str, cc.phrase.str,
					cc.tuples.size());
			for (int i = 0; i < cc.tuples.size(); i++) {
				Tuple t = cc.tuples.get(i);
				Factor f = cc.tupleFactors.get(i);
				dw2.write(cc.ccid, i, Util.counter2jsonstr(f.features),
						cc.eventtype.str, cc.phrase.str,
						cc.phrase.head, t.getArg1(), t.getArg2(),
						t.getArg1Ner(),
						t.getArg2Ner(),
						Util.counter2str(t.getArg1FineGrainedNer()),
						Util.counter2str(t.getArg2FineGrainedNer())
						);
			}
			for (Cell<Integer, Integer, Factor> c : cc.crossFactors.cellSet()) {
				Factor f = c.getValue();
				int i = c.getColumnKey();
				int j = c.getRowKey();
				dw3.write(cc.ccid, i, j, Util.counter2jsonstr(f.features),
						cc.tuples.get(i).getEecname(), cc.tuples.get(j)
								.getEecname());
			}
		}
		dw.close();
		dw2.close();
		dw3.close();
		return ccs;
	}

	public static void featurizePhraseWordpair(ConnectedComponent cc) {
		// word
		String head1 = cc.eventtype.eventphrase.head;
		for (String w : cc.phrase.words) {
			cc.phraseFactor.add("wp@" + head1 + "::" + w);
		}
	}

	public static void featurizePhraseContainsNot(ConnectedComponent cc) {
		// word
		for (String w : cc.phrase.str.split(" ")) {
			if (neg.contains(w)) {
				cc.phraseFactor.add("CONTAINS_NO");
			}
		}
		// for (String w : cc.phrase.words) {
		// cc.phraseFactor.add("wp@" + head1 + "::" + w); }
	}

	public static void featurizePhraseExtendedwordpair(ConnectedComponent cc) {
		String head1 = cc.eventtype.eventphrase.head;
		for (String w : cc.phrase.extendedwords) {
			cc.phraseFactor.add("wpe@" + head1 + "::" + w);
			// D.p(cc.phrase.str, head1, w);
		}
	}

	public static void featurizeEecStanfordNer(ConnectedComponent cc) {
		for (int i = 0; i < cc.tuples.size(); i++) {
			Tuple t = cc.tuples.get(i);
			Factor tf = cc.tupleFactors.get(i);
			String sner1 = Util.mapSNer2Nelner(t.getArg1Ner());
			String sner2 = Util.mapSNer2Nelner(t.getArg2Ner());
			if (sner1.equals(cc.eventtype.arg1type) &&
					sner2.equals(cc.eventtype.arg2type)) {
				tf.add("sner@" + sner1 + "_" + sner2);
			}
		}
	}

	public static void featurizeEecFineNer(ConnectedComponent cc) {
		for (int i = 0; i < cc.tuples.size(); i++) {
			Tuple t = cc.tuples.get(i);
			Factor tf = cc.tupleFactors.get(i);
			Counter<String> t1s = t.getArg1FineGrainedNer();
			Counter<String> t2s = t.getArg2FineGrainedNer();
			if (t1s.getCount(cc.eventtype.arg1type) > 0 &&
					t2s.getCount(cc.eventtype.arg2type) > 0) {
				// tf.add("fner@" + cc.eventtype.arg1type + "_"
				// + cc.eventtype.arg2type);
				tf.add("fner");
			}
		}
	}

	public static void featurizeEecFineMaxNer(ConnectedComponent cc) {
		for (int i = 0; i < cc.tuples.size(); i++) {
			Tuple t = cc.tuples.get(i);
			Factor tf = cc.tupleFactors.get(i);
			Counter<String> t1s = t.getArg1FineGrainedNer();
			Counter<String> t2s = t.getArg2FineGrainedNer();
			String fner1max = Counters.argmax(t1s);
			String fner2max = Counters.argmax(t2s);
			if ((fner1max.equals(cc.eventtype.arg1type) ||
					cc.eventtype.arg1typelen > 1
							&& t1s.getCount(cc.eventtype.arg1type) > 0)
					&&
					(fner2max.equals(cc.eventtype.arg2type) ||
					cc.eventtype.arg2typelen > 1
							&& t2s.getCount(cc.eventtype.arg2type) > 0)) {
				tf.add("fnermax@" + cc.eventtype.arg1type + "_"
						+ cc.eventtype.arg2type);
				// tf.add("fnermax");
			}
		}
	}

	public static void featurizePairwise(ConnectedComponent cc, Stat stat) {
		List<String[]> seeds = new ArrayList<String[]>();
		List<String[]> others = new ArrayList<String[]>();
		List<String[]> otherstype = new ArrayList<String[]>();
		HashMap<String, String> eec2text = new HashMap<String, String>();
		HashMap<String, Set<String>> eec2phrases = new HashMap<String, Set<String>>();
		for (int i = 0; i < cc.tuples.size(); i++) {
			Tuple ti = cc.tuples.get(i);
			Eec ei = stat.eecname2eec.get(ti.getEecname());
			{
				// type of this Tuple
				String[] w = DW.tow(ei.eecname,
						i + "",
						ti.getArg1(),
						ti.getArg2(),
						ti.getArg1Head(),
						ti.getArg2Head(),
						"", "");
				Factor tf = cc.tupleFactors.get(i);
				int type = -1; // do not consider
				if (tf.features.size() > 0)
					type = 1;
				for (String f : tf.features.keySet()) {
					if (f.startsWith("fnermax")) {
						type = 2;
					}
				}
				if (type == 2) {
					seeds.add(w);
				}
				{
					others.add(w);
					if (type == 1 || type == 2) {
						otherstype.add(w);
					}
				}
				Counter<String> t1s = ti.getArg1FineGrainedNer();
				Counter<String> t2s = ti.getArg2FineGrainedNer();
				if (t1s.getCount(cc.eventtype.arg1type) > 0) {
					w[6] = "yes";
				}
				if (t2s.getCount(cc.eventtype.arg2type) > 0) {
					w[7] = "yes";
				}
			}
			{
				// text information
				StringBuilder sbi = new StringBuilder();
				for (Tuple t : ei.tuples) {
					for (String w : t.lmma) {
						if (!RemoveStopwords.isStop(w)) {
							sbi.append(w.toLowerCase() + ' ');
						}
					}
				}
				eec2text.put(ei.eecname, sbi.toString());
			}
			{
				// phrases information
				Set<String> phrases = new HashSet<String>();
				for (Tuple t : ei.tuples) {
					if (t.getRel() != null)
						phrases.add(t.getRel());
					else if (t.getSubtree() != null)
						phrases.add(t.getSubtree());
				}
				eec2phrases.put(ei.eecname, phrases);
			}
		}
		// SAMEARG
		for (String[] o : others) {
			for (String[] s : seeds) {
				int oi = Integer.parseInt(o[1]);
				int si = Integer.parseInt(s[1]);
				if (oi == si)
					continue;
				if (o[2].equals(s[2]) && o[3].equals(s[3])) {
					cc.addCrossFeature(oi, si, "SAMEARG");
					break;
				}
			}
		}
		// SAME HEAD
		for (String[] o : others) {
			for (String[] s : seeds) {
				int oi = Integer.parseInt(o[1]);
				int si = Integer.parseInt(s[1]);
				if (oi == si)
					continue;

				if (o[4].equals(s[4]) && o[5].equals(s[5])) {
					cc.addCrossFeature(oi, si, "SAMEHEAD");
					break;
				}
			}
		}
		for (String[] o : otherstype) {
			for (String[] s : seeds) {
				String text1 = eec2text.get(s[0]);
				String text2 = eec2text.get(o[0]);
				if (text1 == null || text2 == null)
					continue;
				int oi = Integer.parseInt(o[1]);
				int si = Integer.parseInt(s[1]);
				if (oi == si)
					continue;

				int x = text1.split(" ").length + 1;
				int y = text2.split(" ").length + 1;
				int num = StringUtil.numOfShareWords(text1, text2);
				double cosine = num * 1.0 / Math.sqrt(x * y);
				if (cosine > 0.1) {
					cc.addCrossFeature(oi, si, "TEXT");
					// D.p(cc.ccid, o[1], s[1], cosine,
					// o[0], s[0]);
					break;
				}
			}
		}
		for (String[] o : otherstype) {
			for (String[] s : seeds) {
				int oi = Integer.parseInt(o[1]);
				int si = Integer.parseInt(s[1]);
				if (oi == si)
					continue;
				if (!eec2phrases.containsKey(s[0])
						|| !eec2phrases.containsKey(o[0]))
					continue;
				List<String> eiphrases = new ArrayList<String>(
						eec2phrases.get(s[0]));
				List<String> ejphrases = new ArrayList<String>(
						eec2phrases.get(o[0]));

				int shared = StringUtil.numOfShareWords(eiphrases, ejphrases);
				if (shared >= 3) {
					cc.addCrossFeature(oi, si, "SHAREPHRASE");
					break;
				}
			}
		}
		for (String[] o : otherstype) {
			for (String[] s : seeds) {
				int oi = Integer.parseInt(o[1]);
				int si = Integer.parseInt(s[1]);
				if (oi == si)
					continue;
				String oarg1 = o[2];
				String oarg2 = o[3];
				String sarg1 = s[2];
				String sarg2 = s[3];
				if (o[6].equals("yes") && shareCapitalWord(oarg2, sarg2)) {
					cc.addCrossFeature(oi, si, "SHARECAPITAL2");
					break;
				}
				if (o[7].equals("yes") && shareCapitalWord(oarg1, sarg1)) {
					cc.addCrossFeature(oi, si, "SHARECAPITAL1");
					break;
				}
			}
		}
	}

	public static boolean shareCapitalWord(String str1, String str2) {
		String[] w1split = str1.split(" ");
		String[] w2split = str2.split(" ");
		Set<String> w2set = new HashSet<String>();
		for (String w : w2split)
			w2set.add(w);
		for (String w : w1split) {
			if (w2set.contains(w) && StringUtil.isCapStartString(w)
					&& !RemoveStopwords.isStop(w.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	public static void generateConnectedComponents(
			List<ConnectedComponent> ccs,
			Stat stat,
			// HashMap<String, Eec> eecname2eec,
			String input_tuples,
			String input_candidates,
			String output) {
		List<Tuple> tuples = new ArrayList<Tuple>();
		HashMultimap<String, Tuple> phrase2tuples = HashMultimap.create();
		stat.phrase2tuples = phrase2tuples;
		stat.eecname2eec = new HashMap<String, Eec>();
		{
			DR dr = new DR(input_candidates);
			String[] l;
			while ((l = dr.read()) != null) {
				String eventtype = l[0];
				String head = l[1];
				if (head.equals("score")) {
					// D.p();
				}
				EventPhrase.keywords.put(eventtype, head);
				for (int i = 4; i < head.length() + 1; i++) {
					String root = head.substring(0, i);
					EventPhrase.keywordsroot2event2keywords.put(root, eventtype
							, head);
				}
			}
			dr.close();
		}
		// for (Cell<String, String, String> c :
		// EventPhrase.keywordsroot2event2keywords
		// .cellSet()) {
		// D.p(c.getRowKey(), c.getColumnKey(), c.getValue());
		// }
		{
			DR dr = new DR(input_tuples);
			List<String[]> b;
			while ((b = dr.readBlock(0)) != null) {
				String eecname = b.get(0)[0];
				Eec eec = new Eec(eecname);
				stat.eecname2eec.put(eecname, eec);
				for (String[] l : b) {
					Tuple t = gson.fromJson(l[2], Tuple.class);
					t.setEecname(eecname);
					eec.tuples.add(t);
					tuples.add(t);
					String[] phrases = l[1].split("\\|");
					for (String p : phrases) {
						phrase2tuples.put(p, t);
					}
				}
			}
			dr.close();
		}

		int ccid = 0;
		// List<ConnectedComponent> ccs = new ArrayList<ConnectedComponent>();
		{
			DR dr = new DR(input_candidates);
			String[] l;
			while ((l = dr.read()) != null) {
				ConnectedComponent cc = new ConnectedComponent();
				cc.eventtype = new EventType(l[0]);
				String head = l[1];
				String phrase = l[2];
				cc.phrase = new EventPhrase(cc.eventtype, phrase, head);
				cc.tuples = new ArrayList<Tuple>();
				for (Tuple t : phrase2tuples.get(phrase)) {
					Set<String> patternKeywords = t.getPatternKeywords();
					Counter<String> arg1typedist = t.getArg1FineGrainedNer();
					Counter<String> arg2typedist = t.getArg2FineGrainedNer();
					// eec contains event head
					boolean eecContainsHead = false;
					{
						String eecname = t.getEecname();
						for (Tuple tInEec : stat.eecname2eec.get(eecname).tuples) {
							if (tInEec.getPatternHead().contains(
									cc.eventtype.eventphrase.head)) {
								eecContainsHead = true;
								break;
							}
						}
					}
					if (eecContainsHead &&
							patternKeywords.contains(head)
					// &&
					// arg1typedist.getCount(cc.eventtype.arg1type) > 0 &&
					// arg2typedist.getCount(cc.eventtype.arg2type) > 0
					) {
						cc.tuples.add(t);
					}
				}
				if (cc.tuples.size() > 0) {
					cc.ccid = ccid++;
					for (int i = 0; i < cc.tuples.size(); i++) {
						cc.tupleFactors.add(new Factor());
					}
					ccs.add(cc);
				}
			}
			dr.close();
		}
		D.p("Number of connected components", ccs.size());
	}

	public static void learning_copy(List<ConnectedComponent> ccs,
			String input_labeledphrase,
			String output) {
		DW dwdebug = new DW(output + ".debug");
		HashMap<String, Integer> phraselabels = new HashMap<String, Integer>();
		{
			DR dr = new DR(input_labeledphrase);
			String[] l;
			while ((l = dr.read()) != null) {
				phraselabels.put(l[1] + "\t" + l[3], Integer.parseInt(l[0]));
			}
			dr.close();
		}
		HashMap<Integer, Integer> ccid2labels = new HashMap<Integer, Integer>();

		HashMultimap<String, EventType> eecnamePhrase2Label = HashMultimap
				.create();
		for (ConnectedComponent cc : ccs) {
			if (phraselabels.containsKey(cc.eventtype.str + "\t"
					+ cc.phrase.str)) {
				int z = phraselabels.get(cc.eventtype.str + "\t"
						+ cc.phrase.str);
				ccid2labels.put(cc.ccid, z);
				if (z == 1) {
					for (int i = 0; i < cc.tuples.size(); i++) {
						Tuple t = cc.tuples.get(i);
						Factor f = cc.tupleFactors.get(i);
						Counter<String> t1s = t.getArg1FineGrainedNer();
						Counter<String> t2s = t.getArg2FineGrainedNer();
						String fner1max = Counters.argmax(t1s);
						String fner2max = Counters.argmax(t2s);
						if (fner1max.equals(cc.eventtype.arg1type) &&
								fner2max.equals(cc.eventtype.arg2type)) {
							eecnamePhrase2Label.put(t.getArg1() + "\t" + t.getArg2() + "\t"
									+ cc.phrase, cc.eventtype);
						}
					}
				}
			}
		}
		List<ConnectedComponentTrain> ccts = new ArrayList<ConnectedComponentTrain>();
		HashMap<Integer, ConnectedComponentTrain> ccid2ccts = new HashMap<Integer, ConnectedComponentTrain>();

		for (ConnectedComponent cc : ccs) {
			if (ccid2labels.containsKey(cc.ccid)) {
				ConnectedComponentTrain cct = new ConnectedComponentTrain(cc);
				ccid2ccts.put(cct.ccid, cct);
				ccts.add(cct);
				if (ccid2labels.get(cc.ccid) == 1) {
					cct.phraseTruth = 1;
				} else {
					cct.phraseTruth = -1;
				}
				// set up tuple Truth
				if (cct.phraseTruth == -1) {
					// do not need anything
				} else {
					HashMap<Integer, Integer> oldid2newid = new HashMap<Integer, Integer>();
					for (int i = 0; i < cc.tuples.size(); i++) {
						Tuple t = cc.tuples.get(i);
						Factor f = cc.tupleFactors.get(i);
						String key = t.getArg1() + "\t" + t.getArg2() + "\t" + cct.phrase;
						if (eecnamePhrase2Label.containsKey(key)) {
							int newid = cct.tuples.size();
							cct.tuples.add(t);
							cct.tupleFactors.add(f);
							oldid2newid.put(i, newid);
							Set<EventType> eventtypes = eecnamePhrase2Label.get(key);
							if (!eventtypes.contains(cc.eventtype)) {
								cct.tupleTruths.add(-1);
							} else {
								cct.tupleTruths.add(1);
							}

						}
					}
					// set up cross factors
					for (Cell<Integer, Integer, Factor> cell : cc.crossFactors.cellSet()) {
						int oldidX = cell.getColumnKey();
						int oldidY = cell.getRowKey();
						Factor f = cell.getValue();
						if (oldid2newid.containsKey(oldidX) && oldid2newid.containsKey(oldidY)) {
							int newidX = oldid2newid.get(oldidX);
							int newidY = oldid2newid.get(oldidY);
							cct.crossFactors.put(newidX, newidY, f);
						}
					}
				}
			}
		}
		for (ConnectedComponentTrain cct : ccts) {
			dwdebug.write("labelY", cct.phraseTruth, cct.eventtype, cct.phrase,
					Util.counter2str(cct.phraseFactor.features));
			for (int i = 0; i < cct.tuples.size(); i++) {
				Tuple t = cct.tuples.get(i);
				dwdebug.write("labelZ", cct.tupleTruths.get(i), t.getArg1() + "\t" + t.getArg2()
						+ "\t"
						+ cct.phrase, cct.eventtype.str,
						Util.counter2str(cct.tupleFactors.get(i).features));
			}
		}

		// set initial weight
		Counter<String> weight = new ClassicCounter<String>();
		for (ConnectedComponentTrain cct : ccts) {
			cct.phrasePred = -1;
			for (String f : cct.phraseFactor.features.keySet()) {
				weight.setCount(f, 0);
			}
			for (int i = 0; i < cct.tuples.size(); i++) {
				cct.tuplePreds.add(-1);
				for (String f : cct.tupleFactors.get(i).features.keySet()) {
					weight.setCount(f, 0);
				}
			}
			for (Cell<Integer, Integer, Factor> cell : cct.crossFactors.cellSet()) {
				for (String f : cell.getValue().features.keySet()) {
					weight.setCount(f, 0);
				}
			}
		}
		for (int t = 0; t < 5; t++) {
			D.p("iteration", t);
			for (ConnectedComponentTrain cct : ccts) {
				HashMap<String, Double> infer = inference(weight, cct);
				cct.phrasePred = infer.containsKey("Y") && infer.get("Y") > 0.9 ? 1 : -1;
				for (int i = 0; i < cct.tuples.size(); i++) {
					int p = infer.containsKey("Z" + i) && infer.get("Z" + i) > 0.9 ? 1 : -1;
					cct.tuplePreds.set(i, p);
				}
				// print errors
				if (cct.phrasePred != cct.phraseTruth) {
					dwdebug.write("ErrorY" + t, cct.phraseTruth, cct.phrasePred, cct.eventtype,
							cct.phrase,
							Util.counter2str(cct.phraseFactor.features));
				}
				for (int i = 0; i < cct.tuples.size(); i++) {
					Tuple t0 = cct.tuples.get(i);
					if (cct.tuplePreds.get(i) != cct.tupleTruths.get(i)) {
						dwdebug.write("ErrorZ" + t, cct.tupleTruths.get(i), cct.tuplePreds.get(i),
								t0.getArg1() + "\t" + t0.getArg2() + "\t"
										+ cct.phrase, cct.eventtype.str,
								Util.counter2str(cct.tupleFactors.get(i).features));
					}
				}
			}
			for (ConnectedComponentTrain cct : ccts) {
				{
					int dir = getUpdateDir(cct.phrasePred, cct.phraseTruth);
					for (String f : cct.phraseFactor.features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
				for (int i = 0; i < cct.tuples.size(); i++) {
					int dir = getUpdateDir(cct.tuplePreds.get(i), cct.tupleTruths.get(i));
					for (String f : cct.tupleFactors.get(i).features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
				for (Cell<Integer, Integer, Factor> cell : cct.crossFactors.cellSet()) {
					int x = cell.getRowKey();
					int y = cell.getColumnKey();
					Factor factor = cell.getValue();
					int dir = 0;
					if (cct.tupleTruths.get(x) + cct.tupleTruths.get(y) == 2
							&& (cct.tuplePreds.get(x) != 1 || cct.tuplePreds.get(y) != 1)) {
						dir = 1;
					} else if (cct.tupleTruths.get(x) + cct.tupleTruths.get(y) == 0
							&& (cct.tuplePreds.get(x) + cct.tuplePreds.get(y) == 2)) {
						dir = -1;
					}
					for (String f : factor.features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
			}
		}
		DW dw = new DW(output);
		for (ConnectedComponent cc : ccs) {
			Set<String> appeared = new HashSet<String>();
			if (ccid2ccts.containsKey(cc.ccid)) {
				ConnectedComponentTrain cct = ccid2ccts.get(cc.ccid);
				for (int i = 0; i < cct.tuples.size(); i++) {
					Tuple t = cc.tuples.get(i);
					if (cct.tupleTruths.get(i) == 1) {
						dw.write(cc.eventtype.str, cc.phrase.str, t.getEecname(), gson.toJson(t));
						appeared.add(t.getSentence());
					}
				}
			}
			HashMap<String, Double> infer = inference(weight, cc);
			for (int i = 0; i < cc.tuples.size(); i++) {
				Tuple t = cc.tuples.get(i);
				if (appeared.contains(t.getSentence()))
					continue;
				if (infer.containsKey("Z" + i) && infer.get("Z" + i) > 0.9) {
					dw.write(cc.eventtype.str, cc.phrase.str, t.getEecname(), gson.toJson(t));
				}
			}
		}
		dw.close();
		dwdebug.close();
	}

	public static void learning(List<ConnectedComponent> ccs,
			String input_labeledphrase,
			String output) {
		DW dwdebug = new DW(output + ".debug");
		HashMap<String, Integer> phraselabels = new HashMap<String, Integer>();
		{
			D.p(input_labeledphrase);
			DR dr = new DR(input_labeledphrase);
			String[] l;
			while ((l = dr.read()) != null) {
				phraselabels.put(l[1] + "\t" + l[3], Integer.parseInt(l[0]));
			}
			dr.close();
		}
		HashMap<Integer, Integer> ccid2labels = new HashMap<Integer, Integer>();

		HashMultimap<String, EventType> eecnamePhrase2Label = HashMultimap
				.create();
		for (ConnectedComponent cc : ccs) {
			if (phraselabels.containsKey(cc.eventtype.str + "\t"
					+ cc.phrase.str)) {
				int z = phraselabels.get(cc.eventtype.str + "\t"
						+ cc.phrase.str);
				ccid2labels.put(cc.ccid, z);
				if (z == 1) {
					for (int i = 0; i < cc.tuples.size(); i++) {
						Tuple t = cc.tuples.get(i);
						Factor f = cc.tupleFactors.get(i);
						Counter<String> t1s = t.getArg1FineGrainedNer();
						Counter<String> t2s = t.getArg2FineGrainedNer();
						String fner1max = Counters.argmax(t1s);
						String fner2max = Counters.argmax(t2s);
						if (fner1max.equals(cc.eventtype.arg1type) &&
								fner2max.equals(cc.eventtype.arg2type)) {
							eecnamePhrase2Label.put(t.getArg1() + "\t" + t.getArg2() + "\t"
									+ cc.phrase, cc.eventtype);
						}
					}
				}
			}
		}
		List<ConnectedComponentTrain> ccts = new ArrayList<ConnectedComponentTrain>();
		HashMap<Integer, ConnectedComponentTrain> ccid2ccts = new HashMap<Integer, ConnectedComponentTrain>();

		for (ConnectedComponent cc : ccs) {
			if (ccid2labels.containsKey(cc.ccid)) {
				ConnectedComponentTrain cct = new ConnectedComponentTrain(cc);
				ccid2ccts.put(cct.ccid, cct);
				ccts.add(cct);
				if (ccid2labels.get(cc.ccid) == 1) {
					cct.phraseTruth = 1;
				} else {
					cct.phraseTruth = -1;
				}
				// set up tuple Truth
				if (cct.phraseTruth == -1) {
					// do not need anything
				} else {
					HashMap<Integer, Integer> oldid2newid = new HashMap<Integer, Integer>();
					for (int i = 0; i < cc.tuples.size(); i++) {
						Tuple t = cc.tuples.get(i);
						Factor f = cc.tupleFactors.get(i);
						String key = t.getArg1() + "\t" + t.getArg2() + "\t" + cct.phrase;
						if (eecnamePhrase2Label.containsKey(key)) {
							int newid = cct.tuples.size();
							cct.tuples.add(t);
							cct.tupleFactors.add(f);
							oldid2newid.put(i, newid);
							Set<EventType> eventtypes = eecnamePhrase2Label.get(key);
							if (!eventtypes.contains(cc.eventtype)) {
								cct.tupleTruths.add(-1);
							} else {
								cct.tupleTruths.add(1);
							}

						}
					}
					// set up cross factors
					for (Cell<Integer, Integer, Factor> cell : cc.crossFactors.cellSet()) {
						int oldidX = cell.getColumnKey();
						int oldidY = cell.getRowKey();
						Factor f = cell.getValue();
						if (oldid2newid.containsKey(oldidX) && oldid2newid.containsKey(oldidY)) {
							int newidX = oldid2newid.get(oldidX);
							int newidY = oldid2newid.get(oldidY);
							cct.crossFactors.put(newidX, newidY, f);
						}
					}
				}
			}
		}
		for (ConnectedComponentTrain cct : ccts) {
			dwdebug.write("labelY", cct.phraseTruth, cct.eventtype, cct.phrase,
					Util.counter2str(cct.phraseFactor.features));
			for (int i = 0; i < cct.tuples.size(); i++) {
				Tuple t = cct.tuples.get(i);
				dwdebug.write("labelZ", cct.tupleTruths.get(i), t.getArg1() + "\t" + t.getArg2()
						+ "\t"
						+ cct.phrase, cct.eventtype.str,
						Util.counter2str(cct.tupleFactors.get(i).features));
			}
		}

		// set initial weight
		Counter<String> weight = new ClassicCounter<String>();
		for (ConnectedComponentTrain cct : ccts) {
			cct.phrasePred = -1;
			for (String f : cct.phraseFactor.features.keySet()) {
				weight.setCount(f, 0);
			}
			for (int i = 0; i < cct.tuples.size(); i++) {
				cct.tuplePreds.add(-1);
				for (String f : cct.tupleFactors.get(i).features.keySet()) {
					weight.setCount(f, 0);
				}
			}
			for (Cell<Integer, Integer, Factor> cell : cct.crossFactors.cellSet()) {
				for (String f : cell.getValue().features.keySet()) {
					weight.setCount(f, 0);
				}
			}
		}
		for (int t = 0; t < 10; t++) {
//			D.p("iteration", t);
			for (ConnectedComponentTrain cct : ccts) {
				HashMap<String, Double> infer = inference(weight, cct);
				cct.phrasePred = infer.containsKey("Y") && infer.get("Y") > 0.9 ? 1 : -1;
				for (int i = 0; i < cct.tuples.size(); i++) {
					int p = infer.containsKey("Z" + i) && infer.get("Z" + i) > 0.9 ? 1 : -1;
					cct.tuplePreds.set(i, p);
				}
				// print errors
				if (cct.phrasePred != cct.phraseTruth) {
					dwdebug.write("ErrorY" + t, cct.phraseTruth, cct.phrasePred, cct.eventtype,
							cct.phrase,
							Util.counter2str(cct.phraseFactor.features));
				}
				for (int i = 0; i < cct.tuples.size(); i++) {
					Tuple t0 = cct.tuples.get(i);
					if (cct.tuplePreds.get(i) != cct.tupleTruths.get(i)) {
						dwdebug.write("ErrorZ" + t, cct.tupleTruths.get(i), cct.tuplePreds.get(i),
								t0.getArg1() + "\t" + t0.getArg2() + "\t"
										+ cct.phrase, cct.eventtype.str,
								Util.counter2str(cct.tupleFactors.get(i).features));
					}
				}
			}
			for (ConnectedComponentTrain cct : ccts) {
				{
					int dir = getUpdateDir(cct.phrasePred, cct.phraseTruth);
					for (String f : cct.phraseFactor.features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
				for (int i = 0; i < cct.tuples.size(); i++) {
					int dir = getUpdateDir(cct.tuplePreds.get(i), cct.tupleTruths.get(i));
					for (String f : cct.tupleFactors.get(i).features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
				for (Cell<Integer, Integer, Factor> cell : cct.crossFactors.cellSet()) {
					int x = cell.getRowKey();
					int y = cell.getColumnKey();
					Factor factor = cell.getValue();
					int dir = 0;
					if (cct.tupleTruths.get(x) + cct.tupleTruths.get(y) == 2
							&& (cct.tuplePreds.get(x) != 1 || cct.tuplePreds.get(y) != 1)) {
						dir = 1;
					} else if (cct.tupleTruths.get(x) + cct.tupleTruths.get(y) == 0
							&& (cct.tuplePreds.get(x) + cct.tuplePreds.get(y) == 2)) {
						dir = -1;
					}
					for (String f : factor.features.keySet()) {
						weight.incrementCount(f, dir);
					}
				}
			}
		}
		DW dw = new DW(output);
		HashMap<String, Set<String>> phrase2eventCandidates = new HashMap<String, Set<String>>();
		// HashMap<String, Set<String>> event2candidates = new HashMap<String,
		// Set<String>>();
		for (ConnectedComponent cc : ccs) {
			String eventstr = cc.eventtype.str;
			if (!phrase2eventCandidates.containsKey(cc.phrase.str)) {
				phrase2eventCandidates.put(cc.phrase.str, new HashSet<String>());
			}
			phrase2eventCandidates.get(cc.phrase.str).add(eventstr);
		}
		for (String phrasestr : phrase2eventCandidates.keySet()) {
			dw.write("CANDIDATE", phrasestr, gson.toJson(phrase2eventCandidates.get(phrasestr)));
		}
		dw.write("ENDCANDIDATE");
		List<String[]> tow = new ArrayList<String[]>();
		for (ConnectedComponent cc : ccs) {
			Set<String> appeared = new HashSet<String>();
			boolean positive = false;
			if (ccid2ccts.containsKey(cc.ccid)) {
				ConnectedComponentTrain cct = ccid2ccts.get(cc.ccid);
				for (int i = 0; i < cct.tuples.size(); i++) {
					Tuple t = cc.tuples.get(i);
					if (cct.tupleTruths.get(i) == 1) {
						tow.add(DW.tow(cc.eventtype.str, cc.phrase.str, t.getEecname(),
								gson.toJson(t)));
						// dw.write(cc.eventtype.str, cc.phrase.str,
						// t.getEecname(), gson.toJson(t));
						positive = true;
						appeared.add(t.getSentence());
					}
				}
			}
			HashMap<String, Double> infer = inference(weight, cc);
			for (int i = 0; i < cc.tuples.size(); i++) {
				Tuple t = cc.tuples.get(i);
				if (appeared.contains(t.getSentence()))
					continue;
				if (infer.containsKey("Z" + i) && infer.get("Z" + i) > 0.9) {
					tow.add(DW.tow(cc.eventtype.str, cc.phrase.str, t.getEecname(), gson.toJson(t)));
					// dw.write(cc.eventtype.str, cc.phrase.str, t.getEecname(),
					// gson.toJson(t));
					positive = true;
				}
			}
			if (!positive) {
				dw.write("NEGATIVE", cc.eventtype.str, cc.phrase.str);
			}
		}
		dw.write("ENDNEGATIVE");
		for (String[] w : tow) {
			dw.write(w);
		}
		dw.close();
		dwdebug.close();
	}

	public static int getUpdateDir(int pred, int truth) {
		int dir = 0;
		if (pred == -1 && truth == 1) {
			dir = 1;
		} else if (pred == 1 && truth == -1) {
			dir = -1;
		}
		return dir;
	}

	public static HashMap<String, Double> inferenceILP(Counter<String> weight,
			ConnectedComponent cc) {
		IntegerLinearProgramming ilp = new IntegerLinearProgramming();
		Counter<String> objective = new ClassicCounter<String>();
		ilp.setObjective(objective, true);
		double scoreY = cc.phraseFactor.score(weight, -0.1);
		objective.incrementCount("Y", scoreY);
		if (scoreY < 0) {
			Counter<String> y_pos_constraint = new IntCounter<String>();
			y_pos_constraint.incrementCount("Y", 1.0);
			ilp.addConstraint(y_pos_constraint, false, 0, true, 0);
		}
		for (int i = 0; i < cc.tuples.size(); i++) {
			Factor tf = cc.tupleFactors.get(i);
			double scoreZ = tf.score(weight, -0.1);
			objective.incrementCount("Z" + i, scoreZ);
			Counter<String> yz_constraint = new IntCounter<String>();
			yz_constraint.incrementCount("Z" + i, 1.0);
			yz_constraint.incrementCount("Y", -1.0);
			ilp.addConstraint(yz_constraint, false, 0, true, 0);
		}
		for (Cell<Integer, Integer, Factor> c : cc.crossFactors.cellSet()) {
			String Z1 = "Z" + c.getRowKey();
			String Z2 = "Z" + c.getColumnKey();
			double score = c.getValue().score(weight, 0);
			String u1 = "U1:" + Z1 + ":" + Z2;
			String u2 = "U2:" + Z1 + ":" + Z2;
			objective.incrementCount(u1, score);
			objective.incrementCount(u2, score);
			Counter<String> u1c = new IntCounter<String>();
			u1c.incrementCount(u1, 1.0);
			u1c.incrementCount(Z1, -0.5);
			u1c.incrementCount(Z2, -0.5);
			ilp.addConstraint(u1c, false, 0, true, 0);

			Counter<String> u2c = new IntCounter<String>();
			u2c.incrementCount(u2, 1.0);
			u2c.incrementCount(Z1, 0.5);
			u2c.incrementCount(Z2, 0.5);
			ilp.addConstraint(u2c, false, 0, true, 1);
		}
		HashMap<String, Double> result = ilp.run();
		return result;
	}


	public static HashMap<String, Double> inference(Counter<String> weight,
			ConnectedComponent cc) {
		HashMap<String, Double> result = new HashMap<String, Double>();
		double scoreY = cc.phraseFactor.score(weight, -0.1);
		if (scoreY > 0) {
			result.put("Y", 1.0);
			for (int i = 0; i < cc.tuples.size(); i++) {
				Factor tf = cc.tupleFactors.get(i);
				double scoreZ = tf.score(weight, -0.1);
				if (scoreZ > 0) {
					result.put("Z" + i, 1.0);
				}
			}
			for (Cell<Integer, Integer, Factor> c : cc.crossFactors.cellSet()) {
				double score = c.getValue().score(weight, 0);
				if (score < 0)
					continue;
				String Z1 = "Z" + c.getRowKey();
				String Z2 = "Z" + c.getColumnKey();
				if (result.containsKey(Z1) && !result.containsKey(Z2)) {
					result.put(Z2, 1.0);
				}
				if (!result.containsKey(Z1) && result.containsKey(Z2)) {
					result.put(Z1, 1.0);
				}
			}
		}
		return result;
	}

	public static void buildExtractor(String input_generated,
			String input_test,
			String output_extraction) {
		StanfordRegression sr = new StanfordRegression();
		List<String> training_instances = new ArrayList<String>();
		List<HashMap<String, Double>> training_features = new ArrayList<HashMap<String, Double>>();
		List<String> training_labels = new ArrayList<String>();
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

	public static void buildExtractor(
			String generatedTrainingFile,
			String modelFile) throws IOException {
		StanfordRegression sr = new StanfordRegression();
		List<String> training_instances = new ArrayList<String>();
		List<HashMap<String, Double>> training_features = new ArrayList<HashMap<String, Double>>();
		List<String> training_labels = new ArrayList<String>();
		Map<String, Counter<String>> weight = new HashMap<String, Counter<String>>();
		DR dr = new DR(generatedTrainingFile);
		DW dw = new DW(modelFile);
		String[] l;
		// DW dw = new DW(output);
		while ((l = dr.read()) != null) {
			dw.write(l);
			if (l[0].equals("ENDCANDIDATE")) {
				break;
			}
		}
		while ((l = dr.read()) != null) {
			if (l[0].equals("ENDNEGATIVE"))
				break;
			EventType eventtype = new EventType(l[1]);
			String phrase = l[2];
			// D.p(eventtype.str, eventtype.arg1type + "|" + phrase + "|"
			// + eventtype.arg2type);
			if (!weight.containsKey(eventtype.str)) {
				weight.put(eventtype.str, new ClassicCounter<String>());
			}
			weight.get(eventtype.str).setCount(eventtype.arg1type + "|" + phrase + "|"
					+ eventtype.arg2type, -1000);
		}
		int k = 0;
		
		while ((l = dr.read()) != null) {
			EventType eventtype = new EventType(l[0]);
			Tuple t = gson.fromJson(l[3], Tuple.class);
			String tupleName = l[2];
			HashMap<String, Double> fts = new HashMap<String, Double>();
			// List<String> fts = new ArrayList<String>();
			for (String pattern : t.getPatternList()) {
				String f = eventtype.arg1type + "|" + pattern + "|" + eventtype.arg2type;
				fts.put(f, 1.0);
				// if (f.equals("/organization|fall to|/organization")
				// && eventtype.str.equals("beat@/organization@/organization"))
				// {
				// D.p(l);
				// D.p(t.getPatternHead(),t.getPatternListStr());
				// }
			}
			String[] wordsInShortestPath = t.wordsInShortestPath();
			if (wordsInShortestPath!=null && wordsInShortestPath.length >= 2) {
				String f = eventtype.arg1type + "|" + t.getShortestPathFromTuple() + "|"
						+ eventtype.arg2type;
				fts.put(f, 1.0);
			}
			training_instances.add(tupleName);
			training_features.add(fts);
			training_labels.add(eventtype.str);
			if (!weight.containsKey(eventtype.str)) {
				weight.put(eventtype.str, new ClassicCounter<String>());
			}
			weight.get(eventtype.str).setCount(eventtype.arg1type + "|" + l[1] + "|"
					+ eventtype.arg2type, 100);
		}
		dr.close();
		sr.trainRVF(training_features, training_labels);
		Map<String, Counter<String>> weightsr = sr.weightsAsMapOfCounters();
		for (String label : weightsr.keySet()) {
			Counter<String> ftrweight = weightsr.get(label);
			for (String ftr : ftrweight.keySet()) {
				weight.get(label).incrementCount(ftr, ftrweight.getCount(ftr));
			}
		}
		for (String label : weight.keySet()) {
			Counter<String> ftrweight = weight.get(label);
			for (String ftr : ftrweight.keySet()) {
				// bw.write(label + "\t" + ftr + "\t" + ftrweight.getCount(ftr)
				// + "\n");
				dw.write(label, ftr, ftrweight.getCount(ftr));
			}
		}
		dw.close();
		// bw.close();
		// sr.dump(output_model);
		// sr.saveModel(output_model);

	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		try {
			Config.parseConfig();
			List<ConnectedComponent> ccs = featurize(Config.parallelFile, Config.candidatesFile,
					Config.tempDirGenerate);
			learning(ccs, Config.keywordsAnnotationFile, Config.generatedTrainingFile);
			buildExtractor(Config.generatedTrainingFile, Config.modelFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
