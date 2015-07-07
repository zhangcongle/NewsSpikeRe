package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import GetInDomainText.ReVerbExtractorWrap;
//import GetInDomainText.ReVerbResult;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;

//import javatools.administrative.D;
//import javatools.filehandlers.DR;
//import javatools.filehandlers.DW;
//import javatools.stanford.CorefResult;
//import javatools.stanford.CorenlpParsedArticle;
//import javatools.stanford.ParsedSentence;
//import crawlnews.grss.BingBoilpipeCluster;
//import crawlnews.grss.CorenlpBingCluster;
//import crawlnews.grss.Util;
import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;

public class AllSentenceCorenlpProcess {
	private static String[] s_stopWords = { "m", "a", "about", "above", "above", "across", "after", "afterwards",
			"again", "against", "all", "almost", "alone", "along", "already", "also", "although", "always", "am",
			"among", "amongst", "amoungst", "amount", "an", "and", "another", "any", "anyhow", "anyone", "anything",
			"anyway", "anywhere", "are", "around", "as", "at", "back", "be", "became", "because", "become", "becomes",
			"becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between",
			"beyond", "bill", "both", "bottom", "but", "by", "call", "can", "cannot", "cant", "co", "con", "could",
			"couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight",
			"either", "eleven", "else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone",
			"everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five",
			"for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give",
			"go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein",
			"hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in",
			"inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly",
			"least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more",
			"moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never",
			"nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere",
			"of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our",
			"ours", "ourselves", "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re",
			"same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side",
			"since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime",
			"sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them",
			"themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon",
			"these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout",
			"thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un",
			"under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever",
			"when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon",
			"wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why",
			"will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves",
			"the" };
	static Set<String> stopwords = new HashSet<String>();
	static {
		for (String a : s_stopWords)
			stopwords.add(a);
	}

	public static void oneBroilFile(String root, String readfrom, String output) {
		DW dw = new DW(output);
		List<String> inputs = new ArrayList<String>();
		Util.leafFiles(root + File.separator + readfrom, inputs);
		for (String file_input_cluster : inputs) {
			String json = "";
			String today = "";
			long clusterId = 0;
			{
				String[] temp = file_input_cluster.split("\\" + File.separator);
				today = temp[temp.length - 2];
				clusterId = Long.parseLong(temp[temp.length - 1]);
			}
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(new FileInputStream(file_input_cluster), "utf-8"));
				json = br.readLine();
				br.close();
				dw.write(today, clusterId, json);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dw.close();
	}

//	public static void parseBroilFile(String input_broil, String output_articles, String output_meta) {
//		DR dr = new DR(input_broil);
//		DW dw = new DW(output_articles);
//		DW dwmeta = new DW(output_meta);
//		Gson gson = new Gson();
//		String[] l;
//		int sectionId = 1;
//		while ((l = dr.read()) != null) {
//			String day = l[0];
//			String clusterId = l[1];
//			BingBoilpipeCluster bc = gson.fromJson(l[2], BingBoilpipeCluster.class);
//			for (int i = 0; i < bc.texts.size(); i++) {
//				String pagetitle = bc.pagetitles.get(i);
//				String article_text = bc.texts.get(i);
//				List<String> sections = getSectionsFromArticle(pagetitle, article_text, 100);
//				for (String s : sections) {
//					dw.write(sectionId, s);
//					dwmeta.write(sectionId, day, clusterId, i);
//					sectionId++;
//				}
//			}
//		}
//		dw.close();
//		dwmeta.close();
//	}

//	public static void getAbstractFromBroil(String input_broil, String output_sections, String output_meta) {
//		DR dr = new DR(input_broil);
//		DW dw = new DW(output_sections);
//		DW dwmeta = new DW(output_meta);
//		Gson gson = new Gson();
//		String[] l;
//		int sectionId = 1;
//
//		while ((l = dr.read()) != null) {
//			if (sectionId > 1000) {
//				//				break;
//			}
//			String day = l[0];
//			long clusterId = Long.parseLong(l[1]);
//			BingBoilpipeCluster bc = gson.fromJson(l[2], BingBoilpipeCluster.class);
//			for (int i = 0; i < bc.texts.size(); i++) {
//				int articleOffset = i;
//				String pagetitle = bc.pagetitles.get(i);
//				String article_text = bc.texts.get(i);
//				List<String> abs = getAbstractFromArticle(pagetitle, article_text, 100);
//				for (String s : abs) {
//					if (sectionId == 96) {
//						D.p(sectionId);
//						getAbstractFromArticle(pagetitle, article_text, 100);
//					}
//					dw.write(sectionId, s);
//					dwmeta.write(sectionId, day, clusterId, articleOffset);
//					sectionId++;
//				}
//			}
//		}
//		dw.close();
//		dwmeta.close();
//	}

//	public static void getAbstractFromBroilOneArticleOneSection(String input_broil, String output_sections,
//			String output_meta) {
//		DR dr = new DR(input_broil);
//		DW dw = new DW(output_sections);
//		DW dwmeta = new DW(output_meta);
//		Gson gson = new Gson();
//		String[] l;
//		int sectionId = 1;
//
//		int clusterNum = 0;
//		while ((l = dr.read()) != null) {
//			if (clusterNum++ > 110) {
//				//				break;
//			}
//			//			if (sectionId > 100) {
//			//				break;
//			//			}
//			String day = l[0];
//			long clusterId = Long.parseLong(l[1]);
//			BingBoilpipeCluster bc = gson.fromJson(l[2], BingBoilpipeCluster.class);
//			for (int i = 0; i < bc.texts.size(); i++) {
//				int articleOffset = i;
//				String pagetitle = bc.pagetitles.get(i);
//				String article_text = bc.texts.get(i);
//				List<String> abs = getAbstractFromArticle(pagetitle, article_text, 100);
//				StringBuilder sb = new StringBuilder();
//				for (String s : abs) {
//					sb.append(s).append(" ");
//				}
//				dw.write(sectionId, sb.toString());
//				dwmeta.write(sectionId, day, clusterId, articleOffset);
//				sectionId++;
//				//				for (String s : abs) {
//				//					dw.write(sectionId, s);
//				//					dwmeta.write(sectionId, day, clusterId, articleOffset);
//				//					sectionId++;
//				//				}
//			}
//		}
//		dw.close();
//		dwmeta.close();
//	}

	public static List<String> getAbstractFromArticle(String pagetitle, String article, int NUMWORDS) {
		List<String> ret = new ArrayList<String>();
		ret.add(pagetitle + ".");
		String[] sections = article.split("\n");
		int totalWords = 0;
		for (int i = 0; i < sections.length; i++) {
			String s = sections[i];
			boolean isSection = isSection(s);
			if (ret.size() < 10) {
				int p = -1;

				p = stripofdateline(s, "--", p);
				p = stripofdateline(s, ": ", p);
				p = stripofdateline(s, "- ", p);
				if (p > 0) {
					//					D.p(s.substring(0, p));
					s = s.substring(p + 2);
				}
			}
			int num_words_section = s.trim().split(" ").length;
			if (isSection && isEnglish(s) && totalWords < NUMWORDS && num_words_section < NUMWORDS) {
				//				sb.append(s).append("\n");
				ret.add(s);
				totalWords += num_words_section;
			}
		}
		//		return sb.toString();
		return ret;
	}

	static int stripofdateline(String s, String symbol, int oldp) {
		boolean containstopwords = false;
		int p = s.indexOf(symbol);
		if (p > 0) {
			String dateline = s.substring(0, p);
			String[] words = dateline.split(" ");
			for (String w : words) {
				if (stopwords.contains(w)) {
					containstopwords = true;
				}
			}
			if (!containstopwords && p > oldp) {
				return p;
			}
		}
		return oldp;
	}

	public static List<String> getSectionsFromArticle(String pagetitle, String article, int sectionWordLimit) {
		List<String> ret = new ArrayList<String>();
		//		StringBuilder sb = new StringBuilder();
		ret.add(pagetitle + " . ");
		//		sb.append(pagetitle).append(" . \n");
		String[] sections = article.split("\n");
		int totalWords = 0;
		for (String s : sections) {
			//if s is not a section
			if (s.contains("behind only nbc")) {
				//				D.p(s);
			}

			boolean isSection = isSection(s);
			if (!isSection) {
				//				System.err.println("NO SECTION\t" + s);
			}
			int num_words_section = s.trim().split(" ").length;
			if (isSection && isEnglish(s) && num_words_section < sectionWordLimit) {
				ret.add(s);
				//				sb.append(s).append("\n");
				totalWords += num_words_section;
			}
		}
		return ret;
	}

	public static boolean isSection(String s) {
		if (s.contains(".") || s.contains("?") || s.contains("!")) {
			return true;
		}

		return false;
	}

	public static boolean isEnglish(String s) {
		boolean ret = true;
		String[] word = s.split(" ");
		int avgwordlen = s.length() / word.length;
		if (avgwordlen > 10) {
			ret = false;
			//			D.p(s);
		}
		boolean containsStopWord = false;
		for (String w : word) {
			w = w.toLowerCase();
			if (stopwords.contains(w)) {
				containsStopWord = true;
			}
		}
		if (!containsStopWord) {
			ret = false;
		}
		//		int l1 = s.length();
		//		String rest = s.replaceAll("\\w", "");
		//		int l2 = rest.length();
		//		if (l2 > l1 * 0.2) {
		//			ret = false;
		//		}
		return ret;
	}

	public static void concatSectionCorenlp(String input_stanford, String input_meta, String output_article_corenlp) {
		DW dw = new DW(output_article_corenlp);
		HashMap<Integer, String> sectionId2articleId = new HashMap<Integer, String>();
		{
			DR drm = new DR(input_meta);
			String[] l;
			while ((l = drm.read()) != null) {
				sectionId2articleId.put(Integer.parseInt(l[0]), l[2] + "_" + l[3]);
			}
		}
		{
			Gson gson = new Gson();
			String[] l;
			String last = "";

			List<CorenlpParsedArticle> list = new ArrayList<CorenlpParsedArticle>();
			DR drs = new DR(input_stanford);
			while ((l = drs.read()) != null) {
				int sectionId = Integer.parseInt(l[0]);
				String articleId = sectionId2articleId.get(sectionId);
				//				String[] clusterId_articleOffset = articleId.split("_");
				if (!articleId.equals(last)) {
					//concat
					if (list.size() > 0) {
						String[] clusterId_articleOffset = last.split("_");
						int articleOffsetId = Integer.parseInt(clusterId_articleOffset[1]);
						CorenlpParsedArticle art = concatSectionCorenlp_help(list, articleOffsetId);
						dw.write(clusterId_articleOffset[0], clusterId_articleOffset[1], gson.toJson(art));
					}
					list = new ArrayList<CorenlpParsedArticle>();
					last = articleId;
				}
				try {
					String json = l[1];
					CorenlpParsedArticle cnp0 = gson.fromJson(json, CorenlpParsedArticle.class);
					list.add(cnp0);
				} catch (Exception e) {
					D.p(l);
				}
			}
			if (list.size() > 0) {
				String[] clusterId_articleOffset = last.split("_");
				int articleOffsetId = Integer.parseInt(clusterId_articleOffset[1]);
				CorenlpParsedArticle art = concatSectionCorenlp_help(list, articleOffsetId);
				dw.write(clusterId_articleOffset[0], clusterId_articleOffset[1], gson.toJson(art));
			}
		}
		dw.close();
	}

	public static CorenlpParsedArticle concatSectionCorenlp_help(List<CorenlpParsedArticle> list, int articleId) {
		CorenlpParsedArticle art = new CorenlpParsedArticle();
		art.sectionId = articleId;
		int sentenceoffset = 0;
		for (CorenlpParsedArticle section : list) {
			art.numSentence += section.numSentence;
			for (ParsedSentence ps : section.parsedsentence) {
				ps.sectId = art.sectionId;
				ps.sentId += sentenceoffset;
				art.parsedsentence.add(ps);
			}
			for (CorefResult cr : section.corefchains) {
				for (int[] c : cr.chain) {
					c[0] += sentenceoffset;
				}
				art.corefchains.add(cr);
			}
			sentenceoffset += section.numSentence;
		}
		return art;
	}

//	public static void reverbparse(String input_broil, String input_article_stanford, String output) {
//		DR dr = new DR(input_broil);
//		DR drs = new DR(input_article_stanford);
//		Multimap<Long, Integer> clusterId2sectionIds = HashMultimap.create();
//		//		{
//		//			DR drm = new DR(input_meta);
//		//			String[] l;
//		//			while ((l = drm.read()) != null) {
//		//				clusterId2sectionIds.put(Long.parseLong(l[2]), Integer.parseInt(l[0]));
//		//			}
//		//		}
//		ReVerbExtractorWrap rew = new ReVerbExtractorWrap();
//		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
//		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
//
//		DW dw = new DW(output);
//		Gson gson = new Gson();
//		String[] l;
//		String[] s = drs.read();
//		int written = 0;
//		while ((l = dr.read()) != null) {
//			//			if (written > 10)
//			//				break;
//			String day = l[0];
//			long clusterId = Long.parseLong(l[1]);
//			D.p(day, clusterId);
//			Set<Integer> set_sectionId = new HashSet<Integer>();
//			int maxsectionid = -1;
//			int minsectionid = Integer.MAX_VALUE;
//			for (int id : clusterId2sectionIds.get(clusterId)) {
//				maxsectionid = Math.max(maxsectionid, id);
//				minsectionid = Math.min(minsectionid, id);
//			}
//			BingBoilpipeCluster bc = gson.fromJson(l[2], BingBoilpipeCluster.class);
//			CorenlpBingCluster cc = new CorenlpBingCluster(bc);
//			while (s != null && Long.parseLong(s[0]) == clusterId) {
//				String json = s[2];
//				CorenlpParsedArticle cpa = gson.fromJson(json, CorenlpParsedArticle.class);
//				try {
//					for (int k = 0; k < cpa.parsedsentence.size(); k++) {
//						ParsedSentence ps = cpa.parsedsentence.get(k);
//						ReVerbResult rvr = rew.parse(ps.tkn, ps.pos);
//						ChunkedSentence sent = rvr.chunk_sent;
//						int articleId = cpa.sectionId;
//						int sentenceId = ps.sentId;
//						// #####NP chunks
//						for (Range r : sent.getNpChunkRanges()) {
//							ChunkedSentence sub = sent.getSubSequence(r);
//							String[] w = DW.tow(articleId, sentenceId,
//									r.getStart(),
//									r.getEnd(),
//									sub.getTokensAsString(),
//									sub.getTokenNormAsString());
//							cc.reverbNPs.add(w);
//						}
//						// #####VP chunks
//						for (Range r : sent.getVpChunkRanges()) {
//							ChunkedSentence sub = sent.getSubSequence(r);
//							String[] w = DW.tow(articleId, sentenceId,
//									r.getStart(),
//									r.getEnd(),
//									sub.getTokensAsString(),
//									sub.getTokenNormAsString());
//							if (w[4].equals("expected")) {
//								//								D.p(w);
//							}
//							cc.reverbVPs.add(w);
//						}
//						for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
//							NormalizedBinaryExtraction ne = normalizer.normalize(extr);
//							String[] w = DW.tow(articleId, sentenceId,
//									extr.getArgument1().getRange().getStart(),
//									extr.getArgument1().getRange().getEnd(),
//									extr.getRelation().getRange().getStart(),
//									extr.getRelation().getRange().getEnd(),
//									extr.getArgument2().getRange().getStart(),
//									extr.getArgument2().getRange().getEnd(),
//									extr.getArgument1(),
//									extr.getRelation(),
//									extr.getArgument2(),
//									headnoun_extractor.normalizeField(ne.getArgument1()),
//									ne.getRelationNorm(),
//									headnoun_extractor.normalizeField(ne.getArgument2()));
//							cc.reverbExts.add(w);
//							//							D.p(w);
//						}
//					}
//				} catch (Exception e) {
//					System.err.println("Error in reverb parsing ");
//				}
//				cc.parsedarticles.add(cpa);
//				s = drs.read();
//			}
//			dw.write(l[0], l[1], gson.toJson(cc));
//			written++;
//		}
//		dw.close();
//	}

	public static void testParsedCorenlp(String input) {
		DR dr = new DR(input);
		String[] l;
		while ((l = dr.read()) != null) {

		}
		dr.close();
	}

//	public static void main(String[] args) {
//		if (args[0].equals("-oneBroilFile")) {
//			oneBroilFile("/projects/pardosa/data02/clzhang/gnews/exp1", "bingbroilpipe",
//					"/projects/pardosa/s5/clzhang/ssd/gnews/corenlp/broil");
//		}
//		if (args[0].equals("-parseBroilFile")) {
//			parseBroilFile(args[1], args[2], args[3]);
//		}
//		if (args[0].equals("-getAbstractFromBroil")) {
//			getAbstractFromBroil(args[1], args[2], args[3]);
//		}
//		if (args[0].equals("-getAbstractFromBroilOneArticleOneSection")) {
//			getAbstractFromBroilOneArticleOneSection(args[1], args[2], args[3]);
//		}
//		if (args[0].equals("-corenlpparse")) {
//			//see detail in sh file!!!
//		}
//		if (args[0].equals("-concatSectionCorenlp")) {
//			concatSectionCorenlp(args[1], args[2], args[3]);
//		}
//		if (args[0].equals("-reverbparse")) {
//			reverbparse(args[1], args[2], args[3]);
//		}
//		if (args[0].equals("-testParsedCorenlp")) {
//
//		}
//
//	}
}
