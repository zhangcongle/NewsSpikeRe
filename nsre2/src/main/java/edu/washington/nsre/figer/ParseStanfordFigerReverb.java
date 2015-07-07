package edu.washington.nsre.figer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;
import edu.washington.cs.figer.analysis.MapType;
import edu.washington.cs.figer.analysis.Preprocessing;
import edu.washington.cs.figer.analysis.feature.NERFeature;
import edu.washington.cs.figer.data.EntityProtos.Mention;
import edu.washington.cs.figer.data.EntityProtos.Mention.Dependency;
import edu.washington.cs.figer.data.Feature;
import edu.washington.cs.figer.data.FeatureFactory;
import edu.washington.cs.figer.data.Instance;
import edu.washington.cs.figer.ml.Model;
import edu.washington.cs.figer.ml.MultiLabelLogisticRegression;
import edu.washington.cs.figer.ml.MultiLabelPerceptronNERClassifier;
import edu.washington.cs.figer.ml.NERClassifier;
import edu.washington.cs.figer.util.FileUtil;
import edu.washington.cs.figer.util.StanfordDependencyResolver;
import edu.washington.cs.figer.util.Timer;
import edu.washington.cs.figer.util.X;
import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;
import edu.washington.nsre.stanfordtools.CorenlpParsedArticle;
import edu.washington.nsre.stanfordtools.ParsedSentence;
import edu.washington.nsre.stanfordtools.SentDep;
import edu.washington.nsre.util.D;
import edu.washington.nsre.util.DR;
import edu.washington.nsre.util.DW;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class ParseStanfordFigerReverb {
	private static Logger logger = LoggerFactory.getLogger(ParseStanfordFigerReverb.class);

	private static ParseStanfordFigerReverb instance = null;
	public static String configFile = "data/config/figer.conf";

	// useful variables
	public Model model = new MultiLabelLogisticRegression();
	public NERClassifier classifier = null;
	public NERFeature nerFeature = null;
	// debug variables
	public static HashSet<String> mentions = new HashSet<String>();

	public synchronized static ParseStanfordFigerReverb instance() {
		if (instance == null) {
			instance = new ParseStanfordFigerReverb();
		}
		return instance;
	}

	private ParseStanfordFigerReverb() {
		X.parseArgs(configFile);

		Timer timer = new Timer();
		model.debug = X.getBoolean("debugModel");

		// read all tags
		MapType.typeFile = X.tagset;
		MapType.init();
		for (String newType : MapType.mapping.values()) {
			model.labelFactory.getLabel(newType);
		}
		logger.info("labels:\t" + model.labelFactory.allLabels);

		// read the model
		if (X.methodS == X.PERCEPTRON) {
			timer.task = "reading the model " + X.modelFile;
			timer.start();
			model.readModel(X.modelFile);
			model.featureFactory.isTrain = false;
			model.labelFactory.isTrain = false;
			timer.endPrint();

			// init NER Features
			nerFeature = new NERFeature(model);
			nerFeature.init();

			MultiLabelLogisticRegression.prob_threshold = X.getDouble("prob_threshold");
			classifier = new MultiLabelPerceptronNERClassifier(model.infer);
		}
	}

	public String predict(Annotation annotation, int sentId, int startToken, int endToken) {
		Mention m = buildMention(annotation, sentId, startToken, endToken);
		// features
		ArrayList<String> features = new ArrayList<String>();
		nerFeature.extract(m, features);
		return predict(features);
	}

	public Mention buildMention(Annotation annotation, int sentId, int startToken, int endToken) {
		CoreMap sentAnn = annotation.get(SentencesAnnotation.class).get(sentId);
		List<CoreLabel> tokens = sentAnn.get(TokensAnnotation.class);
		// create a Mention object
		Mention.Builder m = Mention.newBuilder();
		m.setStart(startToken);
		m.setEnd(endToken);
		for (int i = 0; i < tokens.size(); i++) {
			m.addTokens(tokens.get(i).get(OriginalTextAnnotation.class));
			m.addPosTags(tokens.get(i).get(PartOfSpeechAnnotation.class));
		}
		m.setEntityName("");
		m.setFileid("on-the-fly");
		m.setSentid(sentId);

		// dependency
		String depStr = StanfordDependencyResolver.getString(sentAnn);
		if (depStr != null) {
			for (String d : depStr.split("\t")) {
				Matcher match = Preprocessing.depPattern.matcher(d);
				if (match.find()) {
					m.addDeps(
							Dependency.newBuilder().setType(match.group(1)).setGov(Integer.parseInt(match.group(3)) - 1)
									.setDep(Integer.parseInt(match.group(5)) - 1).build());
				} else {

				}
			}
		}
		return m.build();
	}

	public String predict(List<String> features) {
		try {
			Instance inst = (Instance) X.instanceClass.newInstance();
			// logger.info("{}", features);
			for (String fea : features) {
				Feature f = model.featureFactory.getFeature(fea);
				FeatureFactory.setValue(inst, f, fea);
			}
			Hashtable<TIntList, String> pool = new Hashtable<TIntList, String>();
			TIntList entity = new TIntArrayList();
			classifier.predict(inst, pool, entity, model);

			String plabels = pool.get(entity);
			return plabels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// only use the following tag set to find named entities
	private static final Set<String> validTags = new HashSet<String>(
			Arrays.asList(new String[] { "PERSON", "ORGANIZATION", "LOCATION", "MISC", "O" }));

	public static List<Pair<Integer, Integer>> getAllNamedEntityMentions(CoreMap sentence) {
		List<Pair<Integer, Integer>> offsets = new ArrayList<Pair<Integer, Integer>>();
		String prevTag = "O";
		int tid = 0;
		int start = -1;
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			String tag = token.get(NamedEntityTagAnnotation.class);
			// if (!validTags.contains(tag)) {
			// tag = "O";
			// }
			if (tag.equals(prevTag)) {

			} else {
				if (tag.equals("O")) {
					offsets.add(Pair.makePair(start, tid));
					start = -1;
				} else {
					if (prevTag.equals("O")) {
						start = tid;
					} else {
						offsets.add(Pair.makePair(start, tid));
						start = tid;
					}
				}
			}
			prevTag = tag;
			tid++;
		}
		if (!prevTag.equals("O")) {
			offsets.add(Pair.makePair(start, tid));
		}
		return offsets;
	}

	public static List<Pair<Integer, Integer>> getNamedEntityMentions(CoreMap sentence) {
		List<Pair<Integer, Integer>> offsets = new ArrayList<Pair<Integer, Integer>>();
		String prevTag = "O";
		int tid = 0;
		int start = -1;
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			String tag = token.get(NamedEntityTagAnnotation.class);
			if (!validTags.contains(tag)) {
				tag = "O";
			}
			if (tag.equals(prevTag)) {

			} else {
				if (tag.equals("O")) {
					offsets.add(Pair.makePair(start, tid));
					start = -1;
				} else {
					if (prevTag.equals("O")) {
						start = tid;
					} else {
						offsets.add(Pair.makePair(start, tid));
						start = tid;
					}
				}
			}
			prevTag = tag;
			tid++;
		}
		if (!prevTag.equals("O")) {
			offsets.add(Pair.makePair(start, tid));
		}
		return offsets;
	}

	private static void usage() {
		System.out.println("sbt \"runMain edu.washington.cs.figer.FigerSystem [config_file] text_file\"");
		System.out.println("    [config_file] is optional with a default value \"config/figer.conf\"");
	}

	// public void parse(){
	//
	// }
	public static void parse(ParseStanfordFigerReverb sys, int lineId, String text) {
		Annotation annotation = new Annotation(text);
		Preprocessing.pipeline.annotate(annotation);
		// for each sentence
		int sentId = 0;
		for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
			// System.out.println("[l" + i + "][s"
			// + sentId + "]tokenized sentence="
			// + StringUtils.joinWithOriginalWhiteSpace(sentence
			// .get(TokensAnnotation.class)));
			List<Pair<Integer, Integer>> entityMentionOffsets = getNamedEntityMentions(sentence);
			for (Pair<Integer, Integer> offset : entityMentionOffsets) {
				String label = sys.predict(annotation, sentId, offset.first, offset.second);
				String mention = StringUtils.joinWithOriginalWhiteSpace(
						sentence.get(TokensAnnotation.class).subList(offset.first, offset.second));
				System.out.println("[l" + lineId + "][s" + sentId + "]mention" + mention + "(" + offset.first + ","
						+ offset.second + ") = " + mention + ", pred = " + label);
			}
			sentId++;
		}
	}

	public static int getLastHeadOfPhrase(FigerParsedSentence ps, int start, int end) {
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

	public static Set<Integer> getHeadOfPhrase(FigerParsedSentence ps, int start, int end) {
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

	public static void callreverb(ReVerbExtractorWrap rew, BinaryExtractionNormalizer normalizer,
			HeadNounExtractor headnoun_extractor, FigerParsedSentence ps) {
		try {
			ReVerbResult rvr = rew.parse(ps.tkn, ps.pos);
			ChunkedSentence sent = rvr.chunk_sent;
			for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
				NormalizedBinaryExtraction ne = normalizer.normalize(extr);
				OneReverb or = new OneReverb();
				or.a1start = extr.getArgument1().getRange().getStart();
				or.a1end = extr.getArgument1().getRange().getEnd();
				or.vstart = extr.getRelation().getRange().getStart();
				or.vend = extr.getRelation().getRange().getEnd();
				or.a2start = extr.getArgument2().getRange().getStart();
				or.a2end = extr.getArgument2().getRange().getEnd();
				or.a1head = getLastHeadOfPhrase(ps, or.a1start, or.a1end);
				or.vhead = getLastHeadOfPhrase(ps, or.vstart, or.vend);
				or.a2head = getLastHeadOfPhrase(ps, or.a2start, or.a2end);
				ps.reverbs.add(or);
				// extr.getArgument1(),
				// extr.getRelation(),
				// extr.getArgument2(),
				// headnoun_extractor.normalizeField(ne.getArgument1()),
				// ne.getRelationNorm(),
				// headnoun_extractor.normalizeField(ne.getArgument2())
				//
				// String[] w =
				// DW.tow(extr.getArgument1().getRange().getStart(),
				// extr.getArgument1().getRange().getEnd(),
				// extr.getRelation().getRange().getStart(),
				// extr.getRelation().getRange().getEnd(),
				// extr.getArgument2().getRange().getStart(),
				// extr.getArgument2().getRange().getEnd(),
				// extr.getArgument1(),
				// extr.getRelation(),
				// extr.getArgument2(),
				// headnoun_extractor.normalizeField(ne.getArgument1()),
				// ne.getRelationNorm(),
				// headnoun_extractor.normalizeField(ne.getArgument2()));
			}
		} catch (Exception e) {
			System.err.println("Error in reverb parsing ");
		}
	}

	// public static boolean callreverb(CorenlpParsedArticle cpa,
	// CorenlpNews cn,
	// ReVerbExtractorWrap rew,
	// BinaryExtractionNormalizer normalizer,
	// HeadNounExtractor headnoun_extractor) {
	// try {
	// for (int k = 0; k < cpa.parsedsentence.size(); k++) {
	// ParsedSentence ps = cpa.parsedsentence.get(k);
	// ReVerbResult rvr = rew.parse(ps.tkn, ps.pos);
	// ChunkedSentence sent = rvr.chunk_sent;
	// int articleId = cpa.sectionId;
	// int sentenceId = ps.sentId;
	// // #####NP chunks
	// for (Range r : sent.getNpChunkRanges()) {
	// ChunkedSentence sub = sent.getSubSequence(r);
	// String[] w = DW.tow(sentenceId,
	// r.getStart(),
	// r.getEnd(),
	// sub.getTokensAsString(),
	// sub.getTokenNormAsString());
	// cn.reverbNps.add(w);
	// }
	// // #####VP chunks
	// for (Range r : sent.getVpChunkRanges()) {
	// ChunkedSentence sub = sent.getSubSequence(r);
	// String[] w = DW.tow(articleId, sentenceId,
	// r.getStart(),
	// r.getEnd(),
	// sub.getTokensAsString(),
	// sub.getTokenNormAsString());
	// cn.reverbVps.add(w);
	// }
	// for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
	// NormalizedBinaryExtraction ne = normalizer.normalize(extr);
	// String[] w = DW.tow(sentenceId,
	// extr.getArgument1().getRange().getStart(),
	// extr.getArgument1().getRange().getEnd(),
	// extr.getRelation().getRange().getStart(),
	// extr.getRelation().getRange().getEnd(),
	// extr.getArgument2().getRange().getStart(),
	// extr.getArgument2().getRange().getEnd(),
	// extr.getArgument1(),
	// extr.getRelation(),
	// extr.getArgument2(),
	// headnoun_extractor.normalizeField(ne.getArgument1()),
	// ne.getRelationNorm(),
	// headnoun_extractor.normalizeField(ne.getArgument2()));
	// cn.reverbExts.add(w);
	// }
	// }
	// } catch (Exception e) {
	// System.err.println("Error in reverb parsing ");
	// return false;
	// }
	// return true;
	// }

	static Gson gson = new Gson();

	public static void main(String[] args) {
		String input = args[0];
		String output = args[1];
		ParseStanfordFigerReverb sys = instance();
		Preprocessing.initPipeline();
		ReVerbExtractorWrap rew = new ReVerbExtractorWrap();
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input), "utf-8"));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "utf-8"));
			String l;
			int lineId = 0;
			while ((l = br.readLine()) != null) {
				if (l.startsWith("Kentucky authorities say the")) {
					D.p("sentence:", l);
				}
				Annotation annotation = new Annotation(l);
				Preprocessing.pipeline.annotate(annotation);
				int sentId = 0;
				// CorenlpParsedArticle pa = new CorenlpParsedArticle();
				// pa.sectionId = lineId;
				List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
				// pa.numSentence = sentences.size();
				for (CoreMap sentence : sentences) {
					FigerParsedSentence ps = new FigerParsedSentence(lineId, sentId, sentence, Preprocessing.gsf);
					// pa.parsedsentence.add(ps);
					List<Pair<Integer, Integer>> entityAllMentionOffsets = getAllNamedEntityMentions(sentence);
					for (Pair<Integer, Integer> offset : entityAllMentionOffsets) {
						OneNer ner = new OneNer();
						ner.slabel = ps.ner[offset.first];
						ner.flabel = "";
						if (validTags.contains(ner.slabel)) {
							ner.flabel = sys.predict(annotation, sentId, offset.first, offset.second);
						}
						ps.blockNers.add(ner);
					}
					callreverb(rew, normalizer, headnoun_extractor, ps);
					// List<Pair<Integer, Integer>> entityMentionOffsets =
					// getNamedEntityMentions(sentence);
					// for (Pair<Integer, Integer> offset :
					// entityMentionOffsets) {
					// String label = sys.predict(annotation, sentId,
					// offset.first, offset.second);
					// String mention =
					// StringUtils.joinWithOriginalWhiteSpace(sentence.get(
					// TokensAnnotation.class).subList(offset.first,
					// offset.second));
					// System.out.println("[l" + 0 + "][s" + sentId + "]mention"
					// + mention + "(" + offset.first + ","
					// + offset.second + ") = " + mention + ", pred = "
					// + label);
					// }
					bw.write(gson.toJson(ps));
					bw.write("\n");
					sentId++;
				}
				// CorenlpNews cn = new CorenlpNews();
				// cn.parsedarticle = pa;
				// boolean success = callreverb(pa, cn, rew, normalizer,
				// headnoun_extractor);
				// System.out.println(gson.toJson(pa));
				// System.out.println(gson.toJson(cn));
				lineId++;
			}
			br.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// public static void main3(String[] args) {
	// String text = "UPDATE January 29 , 2015 8:39 EST : Ladd was executed
	// Thursday night by lethal injection in Huntsville , Texas .";
	// System.out.println(text);
	// String textFile = null;
	// ParseStanfordFigerReverb sys = instance();
	// Preprocessing.initPipeline();
	//
	// Annotation annotation = new Annotation(text);
	// Preprocessing.pipeline.annotate(annotation);
	// ReVerbExtractorWrap rew = new ReVerbExtractorWrap();
	// BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
	// HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
	// // for each sentence
	// int sentId = 0;
	// CorenlpParsedArticle pa = new CorenlpParsedArticle();
	// int sectionId = 0;
	// pa.sectionId = sectionId;
	// List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
	// pa.numSentence = sentences.size();
	// int i = 0;
	// for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
	// FigerParsedSentence ps = new ParsedSentence(pa.sectionId, i++, sentence,
	// Preprocessing.gsf);
	// pa.parsedsentence.add(ps);
	// List<Pair<Integer, Integer>> allEntityMentionOffsets =
	// getAllNamedEntityMentions(sentence);
	// for (Pair<Integer, Integer> offset : allEntityMentionOffsets) {
	// System.out.println(ps.ner[offset.first] + "\t" + offset.first + "\t"
	// + offset.second);
	// }
	// List<Pair<Integer, Integer>> entityMentionOffsets =
	// getNamedEntityMentions(sentence);
	//
	// for (Pair<Integer, Integer> offset : entityMentionOffsets) {
	// String label = sys.predict(annotation, sentId,
	// offset.first, offset.second);
	// String mention = StringUtils.joinWithOriginalWhiteSpace(sentence.get(
	// TokensAnnotation.class).subList(offset.first, offset.second));
	// System.out.println("[l" + 0 + "][s" + sentId + "]mention"
	// + mention + "(" + offset.first + ","
	// + offset.second + ") = " + mention + ", pred = "
	// + label);
	// }
	// sentId++;
	// }
	// CorenlpNews cn = new CorenlpNews();
	// cn.parsedarticle = pa;
	// boolean success = callreverb(pa, cn, rew, normalizer,
	// headnoun_extractor);
	// // System.out.println(gson.toJson(pa));
	// System.out.println(gson.toJson(cn));
	// }

	public static void main2(String[] args) {
		String textFile = null;
		if (args.length == 1) {
			textFile = args[0];
		} else if (args.length == 2) {
			configFile = args[0];
			textFile = args[1];
		} else {
			usage();
			System.exit(0);
		}

		// initialize the system
		ParseStanfordFigerReverb sys = instance();
		Preprocessing.initPipeline();

		// preprocess the text
		List<String> list = FileUtil.getLinesFromFile(textFile);
		for (int i = 0; i < list.size(); i++) {
			Annotation annotation = new Annotation(list.get(i));
			Preprocessing.pipeline.annotate(annotation);

			// for each sentence
			int sentId = 0;
			for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
				System.out.println("[l" + i + "][s" + sentId + "]tokenized sentence="
						+ StringUtils.joinWithOriginalWhiteSpace(sentence.get(TokensAnnotation.class)));
				List<Pair<Integer, Integer>> entityMentionOffsets = getNamedEntityMentions(sentence);
				for (Pair<Integer, Integer> offset : entityMentionOffsets) {
					String label = sys.predict(annotation, sentId, offset.first, offset.second);
					String mention = StringUtils.joinWithOriginalWhiteSpace(
							sentence.get(TokensAnnotation.class).subList(offset.first, offset.second));
					System.out.println("[l" + i + "][s" + sentId + "]mention" + mention + "(" + offset.first + ","
							+ offset.second + ") = " + mention + ", pred = " + label);
				}
				sentId++;
			}
		}
	}

}