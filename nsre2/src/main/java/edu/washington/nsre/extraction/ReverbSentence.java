package edu.washington.nsre.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import edu.washington.nsre.util.D;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import edu.washington.nsre.crawl.CorenlpNews;
import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;


public class ReverbSentence {

	public long articleId;
	public long sentenceId; //global id
	public int artOffset;

	public String[] tkn;
	public String[] pos;
	public String[] ner;
	public String[] lmma;
	public List<SentDep> deps;
	HashMap<String, ReverbPhrase> indexReverbPhrase;
	//	public ReverbPhrase[] indexReverbPhrase;

	//	public List<ReverbPhrase> reverbphrase = new ArrayList<ReverbPhrase>();
	//	public List<ReverbPhrase[]> reverbextraction = new ArrayList<ReverbPhrase[]>();
	public List<ReverbExtraction> reverbextractions = new ArrayList<ReverbExtraction>();

	public ReverbSentence(ParsedSentence ps, long articleId, long sentenceId, int artOffset) {
		this.articleId = articleId;
		this.sentenceId = sentenceId;
		this.artOffset = artOffset;
		this.tkn = ps.tkn;
		this.pos = ps.pos;
		this.ner = ps.ner;
		this.lmma = ps.lmma;
		this.deps = ps.deps;
		indexReverbPhrase = new HashMap<String, ReverbPhrase>();
	}

	public String getRpIndex(long sentenceId, int start, int end) {
		return sentenceId + "_" + start + "_" + end;
	}

	public void addReverbPhrase(ReverbPhrase rp) {
		String key = getRpIndex(this.sentenceId, rp.start, rp.end);
		indexReverbPhrase.put(key, rp);
	}

	public ReverbPhrase getReverbPhrase(int start, int end) {
		String key = getRpIndex(this.sentenceId, start, end);
		return indexReverbPhrase.get(key);
	}

	public void addReverbPhrase(int start, int end, int head, String pos) {
		ReverbPhrase rp = new ReverbPhrase(this, start, end, head, pos);
		this.addReverbPhrase(rp);
	}

	public void addExtraction(int arg1start, int arg1end, int verbstart, int verbend, int arg2start, int arg2end) {
		ReverbPhrase a1 = getReverbPhrase(arg1start, arg1end);
		ReverbPhrase v = getReverbPhrase(verbstart, verbend);
		ReverbPhrase a2 = getReverbPhrase(arg2start, arg2end);
		ReverbExtraction ext = new ReverbExtraction(a1, v, a2, this);
		reverbextractions.add(ext);
	}

//	public static List<ReverbSentence> getReverbSentencesFromCorenlpAddCoref(CorenlpBingCluster c,
//			HashMultimap<String, String> coref) {
//		//		int ARTICLEID = 0;
//		//the key of coref is the phraseId, sentenceId_start_end
//		List<ReverbSentence> reverbsentences = new ArrayList<ReverbSentence>();
//		int SENTENCEID = 0;
//		HashMap<String, List<String[]>> artId2reverbNp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbVp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbExt = new HashMap<String, List<String[]>>();
//		{
//			for (String[] p : c.reverbVPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbVp.containsKey(key))
//					artId2reverbVp.put(key, new ArrayList<String[]>());
//				artId2reverbVp.get(key).add(p);
//				//						D.p(p);
//			}
//
//			for (String[] p : c.reverbNPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbNp.containsKey(key))
//					artId2reverbNp.put(key, new ArrayList<String[]>());
//				artId2reverbNp.get(key).add(p);
//			}
//			for (String[] p : c.reverbExts) {
//				String key = p[0] + "_" + p[1];
//				//				int artId = Integer.parseInt(p[0]);
//				if (!artId2reverbExt.containsKey(key))
//					artId2reverbExt.put(key, new ArrayList<String[]>());
//				artId2reverbExt.get(key).add(p);
//			}
//		}
//		for (int i = 0; i < c.parsedarticles.size(); i++) {
//			CorenlpParsedArticle ca = c.parsedarticles.get(i);
//			HashMap<String, String> tempcoref = new HashMap<String, String>();
//			int corefId = 0;
//			for (CorefResult cr : ca.corefchains) {
//				corefId++;
//				if (cr.chain.size() > 1)
//					for (int[] x : cr.chain) {
//						int sentenceOffset = x[0];
//						int start = x[1];
//						int end = x[2];
//						ParsedSentence ps = ca.parsedsentence.get(sentenceOffset);
//						int head = getLastHeadOfPhrase(ps, start, end);
//						tempcoref.put(sentenceOffset + "_" + head, "C" + i + "_" + corefId);
//					}
//			}
//			for (int j = 0; j < ca.parsedsentence.size(); j++) {
//				ParsedSentence ps = ca.parsedsentence.get(j);
//				ReverbSentence rs = new ReverbSentence(ps, i, SENTENCEID++, j);
//				reverbsentences.add(rs);
//				String key = i + "_" + j;
//				List<String[]> reverbNps = artId2reverbNp.get(key);
//				List<String[]> reverbVps = artId2reverbVp.get(key);
//				List<String[]> reverbExts = artId2reverbExt.get(key);
//				//				if (reverbNps != null) {
//				//					for (String[] np : reverbNps) {
//				//						int start = Integer.parseInt(np[2]);
//				//						int end = Integer.parseInt(np[3]);
//				//						int head = getLastHeadOfPhrase(ps, start, end);
//				//						rs.addReverbPhrase(start, end, head, "NP");
//				//					}
//				//				}
//				//				if (reverbVps != null)
//				//					for (String[] vp : reverbVps) {
//				//						int start = Integer.parseInt(vp[2]);
//				//						int end = Integer.parseInt(vp[3]);
//				//						int head = getLastHeadOfPhrase(ps, start, end);
//				//						rs.addReverbPhrase(start, end, head, "VP");
//				//					}
//				if (reverbExts != null) {
//					for (String[] ext : reverbExts) {
//						int arg1start = Integer.parseInt(ext[2]);
//						int arg1end = Integer.parseInt(ext[3]);
//						int arg1head = getLastHeadOfPhrase(ps, arg1start, arg1end);
//						rs.addReverbPhrase(arg1start, arg1end, arg1head, "NP");
//						int verbstart = Integer.parseInt(ext[4]);
//						int verbend = Integer.parseInt(ext[5]);
//						int verbhead = getLastHeadOfPhrase(ps, verbstart, verbend);
//						rs.addReverbPhrase(verbstart, verbend, verbhead, "VP");
//						int arg2start = Integer.parseInt(ext[6]);
//						int arg2end = Integer.parseInt(ext[7]);
//						int arg2head = getLastHeadOfPhrase(ps, arg2start, arg2end);
//						rs.addReverbPhrase(arg2start, arg2end, arg2head, "NP");
//						rs.addExtraction(arg1start, arg1end, verbstart, verbend, arg2start, arg2end);
//					}
//					for (Entry<String, ReverbPhrase> e : rs.indexReverbPhrase.entrySet()) {
//						String rpkey = e.getKey();
//						ReverbPhrase rp = e.getValue();
//						int offset = j;
//						int head = rp.head;
//						if (rp.pos.equals("NP") && tempcoref.containsKey(offset + "_" + head)) {
//							String cid = tempcoref.get(j + "_" + head);
//							coref.put(cid, rpkey);
//						}
//					}
//				}
//			}
//		}
//		return reverbsentences;
//	}
//
//	public static List<ReverbSentence> getReverbSentencesFromCorenlpAddCorefIncludeNoExtraction(CorenlpBingCluster c,
//			HashMultimap<String, String> coref) {
//		//		int ARTICLEID = 0;
//		//the key of coref is the phraseId, sentenceId_start_end
//		List<ReverbSentence> reverbsentences = new ArrayList<ReverbSentence>();
//		int SENTENCEID = 0;
//		HashMap<String, List<String[]>> artId2reverbNp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbVp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbExt = new HashMap<String, List<String[]>>();
//		{
//			for (String[] p : c.reverbVPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbVp.containsKey(key))
//					artId2reverbVp.put(key, new ArrayList<String[]>());
//				artId2reverbVp.get(key).add(p);
//				//						D.p(p);
//			}
//
//			for (String[] p : c.reverbNPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbNp.containsKey(key))
//					artId2reverbNp.put(key, new ArrayList<String[]>());
//				artId2reverbNp.get(key).add(p);
//			}
//			for (String[] p : c.reverbExts) {
//				String key = p[0] + "_" + p[1];
//				//				int artId = Integer.parseInt(p[0]);
//				if (!artId2reverbExt.containsKey(key))
//					artId2reverbExt.put(key, new ArrayList<String[]>());
//				artId2reverbExt.get(key).add(p);
//			}
//		}
//		for (int i = 0; i < c.parsedarticles.size(); i++) {
//			CorenlpParsedArticle ca = c.parsedarticles.get(i);
//			HashMap<String, String> tempcoref = new HashMap<String, String>();
//			int corefId = 0;
//			for (CorefResult cr : ca.corefchains) {
//				corefId++;
//				if (cr.chain.size() > 1)
//					for (int[] x : cr.chain) {
//						int sentenceOffset = x[0];
//						int start = x[1];
//						int end = x[2];
//						ParsedSentence ps = ca.parsedsentence.get(sentenceOffset);
//						int head = getLastHeadOfPhrase(ps, start, end);
//						tempcoref.put(sentenceOffset + "_" + head, "C" + i + "_" + corefId);
//					}
//			}
//			for (int j = 0; j < ca.parsedsentence.size(); j++) {
//				ParsedSentence ps = ca.parsedsentence.get(j);
//				ReverbSentence rs = new ReverbSentence(ps, i, SENTENCEID++, j);
//				reverbsentences.add(rs);
//				String key = i + "_" + j;
//				List<String[]> reverbNps = artId2reverbNp.get(key);
//				List<String[]> reverbVps = artId2reverbVp.get(key);
//				List<String[]> reverbExts = artId2reverbExt.get(key);
//				if (reverbNps != null) {
//					for (String[] np : reverbNps) {
//						int start = Integer.parseInt(np[2]);
//						int end = Integer.parseInt(np[3]);
//						int head = getLastHeadOfPhrase(ps, start, end);
//						rs.addReverbPhrase(start, end, head, "NP");
//					}
//				}
//				if (reverbVps != null)
//					for (String[] vp : reverbVps) {
//						int start = Integer.parseInt(vp[2]);
//						int end = Integer.parseInt(vp[3]);
//						int head = getLastHeadOfPhrase(ps, start, end);
//						rs.addReverbPhrase(start, end, head, "VP");
//					}
//				if (reverbExts != null) {
//					for (String[] ext : reverbExts) {
//						int arg1start = Integer.parseInt(ext[2]);
//						int arg1end = Integer.parseInt(ext[3]);
//						int arg1head = getLastHeadOfPhrase(ps, arg1start, arg1end);
//						rs.addReverbPhrase(arg1start, arg1end, arg1head, "NP");
//						int verbstart = Integer.parseInt(ext[4]);
//						int verbend = Integer.parseInt(ext[5]);
//						int verbhead = getLastHeadOfPhrase(ps, verbstart, verbend);
//						rs.addReverbPhrase(verbstart, verbend, verbhead, "VP");
//						int arg2start = Integer.parseInt(ext[6]);
//						int arg2end = Integer.parseInt(ext[7]);
//						int arg2head = getLastHeadOfPhrase(ps, arg2start, arg2end);
//						rs.addReverbPhrase(arg2start, arg2end, arg2head, "NP");
//						rs.addExtraction(arg1start, arg1end, verbstart, verbend, arg2start, arg2end);
//					}
//					for (Entry<String, ReverbPhrase> e : rs.indexReverbPhrase.entrySet()) {
//						String rpkey = e.getKey();
//						ReverbPhrase rp = e.getValue();
//						int offset = j;
//						int head = rp.head;
//						if (rp.pos.equals("NP") && tempcoref.containsKey(offset + "_" + head)) {
//							String cid = tempcoref.get(j + "_" + head);
//							coref.put(cid, rpkey);
//						}
//					}
//				}
//			}
//		}
//		return reverbsentences;
//	}
//
//	public static List<ReverbSentence> getReverbSentencesFromCorenlp(CorenlpBingCluster c) {
//		//		int ARTICLEID = 0;
//		List<ReverbSentence> reverbsentences = new ArrayList<ReverbSentence>();
//		int SENTENCEID = 0;
//		HashMap<String, List<String[]>> artId2reverbNp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbVp = new HashMap<String, List<String[]>>();
//		HashMap<String, List<String[]>> artId2reverbExt = new HashMap<String, List<String[]>>();
//		{
//			for (String[] p : c.reverbVPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbVp.containsKey(key))
//					artId2reverbVp.put(key, new ArrayList<String[]>());
//				artId2reverbVp.get(key).add(p);
//				//						D.p(p);
//			}
//
//			for (String[] p : c.reverbNPs) {
//				//				int artId = Integer.parseInt(p[0]);
//				String key = p[0] + "_" + p[1];
//				if (!artId2reverbNp.containsKey(key))
//					artId2reverbNp.put(key, new ArrayList<String[]>());
//				artId2reverbNp.get(key).add(p);
//			}
//			for (String[] p : c.reverbExts) {
//				String key = p[0] + "_" + p[1];
//				//				int artId = Integer.parseInt(p[0]);
//				if (!artId2reverbExt.containsKey(key))
//					artId2reverbExt.put(key, new ArrayList<String[]>());
//				artId2reverbExt.get(key).add(p);
//			}
//		}
//		for (int i = 0; i < c.parsedarticles.size(); i++) {
//			CorenlpParsedArticle ca = c.parsedarticles.get(i);
//
//			for (int j = 0; j < ca.parsedsentence.size(); j++) {
//				ParsedSentence ps = ca.parsedsentence.get(j);
//				ReverbSentence rs = new ReverbSentence(ps, i, SENTENCEID++, j);
//				reverbsentences.add(rs);
//				String key = i + "_" + j;
//				List<String[]> reverbNps = artId2reverbNp.get(key);
//				List<String[]> reverbVps = artId2reverbVp.get(key);
//				List<String[]> reverbExts = artId2reverbExt.get(key);
//				//				if (reverbNps != null) {
//				//					for (String[] np : reverbNps) {
//				//						int start = Integer.parseInt(np[2]);
//				//						int end = Integer.parseInt(np[3]);
//				//						int head = getLastHeadOfPhrase(ps, start, end);
//				//						rs.addReverbPhrase(start, end, head, "NP");
//				//					}
//				//				}
//				//				if (reverbVps != null)
//				//					for (String[] vp : reverbVps) {
//				//						int start = Integer.parseInt(vp[2]);
//				//						int end = Integer.parseInt(vp[3]);
//				//						int head = getLastHeadOfPhrase(ps, start, end);
//				//						rs.addReverbPhrase(start, end, head, "VP");
//				//					}
//				if (reverbExts != null)
//					for (String[] ext : reverbExts) {
//						int arg1start = Integer.parseInt(ext[2]);
//						int arg1end = Integer.parseInt(ext[3]);
//						int arg1head = getLastHeadOfPhrase(ps, arg1start, arg1end);
//						rs.addReverbPhrase(arg1start, arg1end, arg1head, "NP");
//						int verbstart = Integer.parseInt(ext[4]);
//						int verbend = Integer.parseInt(ext[5]);
//						int verbhead = getLastHeadOfPhrase(ps, verbstart, verbend);
//						rs.addReverbPhrase(verbstart, verbend, verbhead, "VP");
//						int arg2start = Integer.parseInt(ext[6]);
//						int arg2end = Integer.parseInt(ext[7]);
//						int arg2head = getLastHeadOfPhrase(ps, arg2start, arg2end);
//						rs.addReverbPhrase(arg2start, arg2end, arg2head, "NP");
//						rs.addExtraction(arg1start, arg1end, verbstart, verbend, arg2start, arg2end);
//					}
//
//			}
//
//		}
//		return reverbsentences;
//	}

	public static List<ReverbSentence> getReverbSentencesFromCorenlpNews(CorenlpNews cn,
			HashMultimap<String, String> coref) {
		//		int ARTICLEID = 0;
		//the key of coref is the phraseId, sentenceId_start_end
		List<ReverbSentence> reverbsentences = new ArrayList<ReverbSentence>();
		int SENTENCEID = 0;
		HashMap<Integer, List<String[]>> sentenceId2reverbNp = new HashMap<Integer, List<String[]>>();
		HashMap<Integer, List<String[]>> sentenceId2reverbVp = new HashMap<Integer, List<String[]>>();
		HashMap<Integer, List<String[]>> sentenceId2reverbExt = new HashMap<Integer, List<String[]>>();
		{
			for (String[] p : cn.reverbVps) {
				//				int artId = Integer.parseInt(p[0]);
				int key = Integer.parseInt(p[0]);
				if (!sentenceId2reverbVp.containsKey(key))
					sentenceId2reverbVp.put(key, new ArrayList<String[]>());
				sentenceId2reverbVp.get(key).add(p);
				//						D.p(p);
			}

			for (String[] p : cn.reverbNps) {
				//				int artId = Integer.parseInt(p[0]);
				int key = Integer.parseInt(p[0]);
				if (!sentenceId2reverbNp.containsKey(key))
					sentenceId2reverbNp.put(key, new ArrayList<String[]>());
				sentenceId2reverbNp.get(key).add(p);
			}
			for (String[] p : cn.reverbExts) {
				int key = Integer.parseInt(p[0]);
				//				int artId = Integer.parseInt(p[0]);
				if (!sentenceId2reverbExt.containsKey(key))
					sentenceId2reverbExt.put(key, new ArrayList<String[]>());
				sentenceId2reverbExt.get(key).add(p);
			}
		}
		CorenlpParsedArticle ca = cn.parsedarticle;
		HashMap<String, String> tempcoref = new HashMap<String, String>();
		int corefId = 0;
		for (CorefResult cr : ca.corefchains) {
			corefId++;
			if (cr.chain.size() > 1)
				for (int[] x : cr.chain) {
					int sentenceOffset = x[0];
					int start = x[1];
					int end = x[2];
					ParsedSentence ps = ca.parsedsentence.get(sentenceOffset);
					int head = getLastHeadOfPhrase(ps, start, end);
					tempcoref.put(sentenceOffset + "_" + head, "C" + corefId);
				}
		}
		for (int j = 0; j < ca.parsedsentence.size(); j++) {
			ParsedSentence ps = ca.parsedsentence.get(j);
			ReverbSentence rs = new ReverbSentence(ps, cn.articleId, cn.articleId * 100 + j, j);
			reverbsentences.add(rs);
			int key = j;
			List<String[]> reverbNps = sentenceId2reverbNp.get(key);
			List<String[]> reverbVps = sentenceId2reverbVp.get(key);
			List<String[]> reverbExts = sentenceId2reverbExt.get(key);
			//				if (reverbNps != null) {
			//					for (String[] np : reverbNps) {
			//						int start = Integer.parseInt(np[2]);
			//						int end = Integer.parseInt(np[3]);
			//						int head = getLastHeadOfPhrase(ps, start, end);
			//						rs.addReverbPhrase(start, end, head, "NP");
			//					}
			//				}
			//				if (reverbVps != null)
			//					for (String[] vp : reverbVps) {
			//						int start = Integer.parseInt(vp[2]);
			//						int end = Integer.parseInt(vp[3]);
			//						int head = getLastHeadOfPhrase(ps, start, end);
			//						rs.addReverbPhrase(start, end, head, "VP");
			//					}
			if (reverbExts != null) {
				for (String[] ext : reverbExts) {
					int arg1start = Integer.parseInt(ext[1]);
					int arg1end = Integer.parseInt(ext[2]);
					int arg1head = getLastHeadOfPhrase(ps, arg1start, arg1end);
					rs.addReverbPhrase(arg1start, arg1end, arg1head, "NP");
					int verbstart = Integer.parseInt(ext[3]);
					int verbend = Integer.parseInt(ext[4]);
					int verbhead = getLastHeadOfPhrase(ps, verbstart, verbend);
					rs.addReverbPhrase(verbstart, verbend, verbhead, "VP");
					int arg2start = Integer.parseInt(ext[5]);
					int arg2end = Integer.parseInt(ext[6]);
					int arg2head = getLastHeadOfPhrase(ps, arg2start, arg2end);
					rs.addReverbPhrase(arg2start, arg2end, arg2head, "NP");
					rs.addExtraction(arg1start, arg1end, verbstart, verbend, arg2start, arg2end);
				}
				for (Entry<String, ReverbPhrase> e : rs.indexReverbPhrase.entrySet()) {
					String rpkey = e.getKey();
					ReverbPhrase rp = e.getValue();
					int offset = j;
					int head = rp.head;
					if (rp.pos.equals("NP") && tempcoref.containsKey(offset + "_" + head)) {
						String cid = tempcoref.get(j + "_" + head);
						coref.put(cid, rpkey);
					}
				}
			}
		}
		return reverbsentences;
	}

	public static int getLastHeadOfPhrase(ParsedSentence ps, int start, int end) {
		Set<Integer> heads = getHeadOfPhrase(ps, start, end);
		int lasthead = -1;
		for (int h : heads) {
			if (h > lasthead) {
				lasthead = h;
			}
		}
		if (lasthead < 0) {
			lasthead = end - 1;
		}
		return lasthead;
	}

	public static Set<Integer> getHeadOfPhrase(ParsedSentence ps, int start, int end) {
		Set<Integer> heads = new HashSet<Integer>();
		if (end == start + 1) {
			heads.add(start);
		} else {
			for (SentDep sd : ps.deps) {
				int d = sd.d;
				int g = sd.g;
				if (d >= start && d < end && (g >= end || g < start)) {
					heads.add(d);
				} else if (g >= start && g < end && (d >= end || d < start)) {
					heads.add(g);
				}
			}
		}
		return heads;
	}
}
