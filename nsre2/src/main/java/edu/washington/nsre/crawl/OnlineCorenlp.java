package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.xml.sax.InputSource;

import edu.washington.nsre.util.*;
import edu.washington.nsre.stanfordtools.*;

//import javatools.administrative.D;
//import javatools.filehandlers.DW;
//import javatools.stanford.CoreNlpPipeline;
//import javatools.stanford.CorenlpParsedArticle;
//import javatools.stanford.ParsedSentence;
//import javatools.webapi.BingAzureApi;

//import GetInDomainText.ReVerbExtractorWrap;
//import GetInDomainText.ReVerbResult;

import com.google.gson.Gson;

//import crawlnews.cluster.AllSentenceCorenlpProcess;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;

public class OnlineCorenlp {
	public static void main(String[] args) {
		CoreNlpPipeline cnp = new CoreNlpPipeline("tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		ReVerbExtractorWrap rew = new ReVerbExtractorWrap();
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
		String root = args[0];
		boolean restart = false;
		if (args.length > 1) {
			restart = Boolean.parseBoolean(args[1]);
		}
		Gson gson = new Gson();
		String readfrom = "broil";
		String outputto = "corenlp";
		{
			File f = new File(root + File.separator + outputto);
			if (!f.exists()) {
				f.mkdir();
			}
		}
		HashSet<String> parsedtitle = new HashSet<String>();
		{
			List<String> already_list = new ArrayList<String>();
			Util.leafFiles(root + File.separator + outputto, already_list);
			for (String f : already_list) {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
					String l;
					while ((l = br.readLine()) != null) {
						try {
							CorenlpNews cn = gson.fromJson(l, CorenlpNews.class);
							parsedtitle.add(cn.title);
						} catch (Exception e) {

						}
					}
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.err.println("crawledurl\t" + parsedtitle.size());
		while (true) {
			try {
				Date date = new Date();
				SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
				String today = dateformatYYYYMMDD.format(date);
				String outputdir = root + File.separator + outputto + File.separator + today;
				if (!new File(outputdir).exists()) {
					new File(outputdir).mkdirs();
				}
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputdir
						+ File.separator + date.getTime()), "utf-8"));
				List<String> inputs = new ArrayList<String>();
				Util.leafFiles(root + File.separator + readfrom, inputs);
				for (String f : inputs) {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
						String l;
						while ((l = br.readLine()) != null) {

							BroilNews bn = gson.fromJson(l, BroilNews.class);
							if (!parsedtitle.contains(bn.title)) {
								try {
									CorenlpNews cn = new CorenlpNews(bn);
									String pagetitle = cn.title;
									String article_text = cn.text;
									if (article_text == null) {
										article_text = cn.desc;
									}
									List<String> sections = AllSentenceCorenlpProcess.getAbstractFromArticle(pagetitle,
											article_text, 100);
									StringBuilder sb = new StringBuilder();
									for (String s : sections) {
										sb.append(s + " ");
									}
									CorenlpParsedArticle cpa = cnp.parseDocumentWithCoref(-1, sb.toString(), gson);
									if (cpa == null)
										continue;
									cn.parsedarticle = cpa;
									boolean success = callreverb(cpa, cn, rew, normalizer, headnoun_extractor);
									if (success) {
										bw.write(gson.toJson(cn));
										bw.write('\n');
										bw.flush();
										D.p("parse", cn.title, cn.parsedarticle.numSentence, (new Date()).toString());
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								parsedtitle.add(bn.title);

							}
						}
						br.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				bw.close();
			} catch (Exception e) {

			}
			try {
				Thread.sleep(3600 * 100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean callreverb(CorenlpParsedArticle cpa, 
			CorenlpNews cn, 
			ReVerbExtractorWrap rew,
			BinaryExtractionNormalizer normalizer,
			HeadNounExtractor headnoun_extractor) {
		try {
			for (int k = 0; k < cpa.parsedsentence.size(); k++) {
				ParsedSentence ps = cpa.parsedsentence.get(k);
				ReVerbResult rvr = rew.parse(ps.tkn, ps.pos);
				ChunkedSentence sent = rvr.chunk_sent;
				int articleId = cpa.sectionId;
				int sentenceId = ps.sentId;
				// #####NP chunks
				for (Range r : sent.getNpChunkRanges()) {
					ChunkedSentence sub = sent.getSubSequence(r);
					String[] w = DW.tow(sentenceId,
							r.getStart(),
							r.getEnd(),
							sub.getTokensAsString(),
							sub.getTokenNormAsString());
					cn.reverbNps.add(w);
				}
				// #####VP chunks
				for (Range r : sent.getVpChunkRanges()) {
					ChunkedSentence sub = sent.getSubSequence(r);
					String[] w = DW.tow(articleId, sentenceId,
							r.getStart(),
							r.getEnd(),
							sub.getTokensAsString(),
							sub.getTokenNormAsString());
					cn.reverbVps.add(w);
				}
				for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
					NormalizedBinaryExtraction ne = normalizer.normalize(extr);
					String[] w = DW.tow(sentenceId,
							extr.getArgument1().getRange().getStart(),
							extr.getArgument1().getRange().getEnd(),
							extr.getRelation().getRange().getStart(),
							extr.getRelation().getRange().getEnd(),
							extr.getArgument2().getRange().getStart(),
							extr.getArgument2().getRange().getEnd(),
							extr.getArgument1(),
							extr.getRelation(),
							extr.getArgument2(),
							headnoun_extractor.normalizeField(ne.getArgument1()),
							ne.getRelationNorm(),
							headnoun_extractor.normalizeField(ne.getArgument2()));
					cn.reverbExts.add(w);
				}
			}
		} catch (Exception e) {
			System.err.println("Error in reverb parsing ");
			return false;
		}
		return true;
	}
}
