package edu.washington.nsre.stanfordtools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;

import edu.washington.nsre.util.*;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
//import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
//import edu.stanford.nlp.trees.semgraph.SemanticGraph;
//import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class CoreNlpPipeline {
	Properties props;
	StanfordCoreNLP pipeline;
	GrammaticalStructureFactory gsf;

	public static void main(String[] args) {
		CoreNlpPipeline cnp = new CoreNlpPipeline("tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		String sentence = "Angus T. Jones is urging ` Two and a Half Men ' fans not to watch anymore , calling his own sitcom `` filth . ''";
		Gson gson = new Gson();
		CorenlpParsedArticle cpa = cnp.parseDocumentWithCoref(0, sentence, gson);
		System.out.println(gson.toJson(cpa));
		// cnp.parse2lines("Bieber , 18 , was leaving the theater in suburban
		// Calabasas with girlfriend Selena Gomez on May 27 when he had the
		// encounter in a parking lot .");
		// cnp.parse2lines("Bieber , 18 , was leaving the theater in suburban
		// Calabasas with girlfriend Selena Gomez on May 27 when he had the
		// encounter in a parking lot .");
		// cnp.parseDocumentJson(input, output);
	}

	public static void main_(String[] args) throws FileNotFoundException, IOException {
		CoreNlpPipeline cnp = new CoreNlpPipeline();
		String input = args[0];
		String output = args[1];
		// cnp.parseDocument(input, output);
		cnp.parseDocumentJson(input, output);
	}

	public CoreNlpPipeline() {
		props = new Properties();
		// props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse,
		// dcoref");
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		pipeline = new StanfordCoreNLP(props);
		gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
	}

	public CoreNlpPipeline(String props_str) {
		props = new Properties();
		// props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse,
		// dcoref");
		props.put("annotators", props_str);
		pipeline = new StanfordCoreNLP(props);
		gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
	}

	public String parseDocumentJson(int sectionId, String text, Gson gson) {

		// l[]: article_name (String), section_id (int), text
		ParsedText pt = new ParsedText();
		pt.sectionId = sectionId;
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		pt.numSentence = sentences.size();
		for (int i = 0; i < sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			ParsedSentence ps = new ParsedSentence(pt.sectionId, i, sentence, gsf);
			pt.parsedsentence.add(ps);
		}
		String jsonstr = gson.toJson(pt);
		return jsonstr;
	}

	public ParsedText parseDocument(int sectionId, String text, Gson gson) {

		// l[]: article_name (String), section_id (int), text
		ParsedText pt = new ParsedText();
		pt.sectionId = sectionId;
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		pt.numSentence = sentences.size();
		for (int i = 0; i < sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			ParsedSentence ps = new ParsedSentence(pt.sectionId, i, sentence, gsf);
			pt.parsedsentence.add(ps);
		}
		return pt;
	}

	public CorenlpParsedArticle parseDocumentWithCoref(int sectionId, String text, Gson gson) {

		// l[]: article_name (String), section_id (int), text
		CorenlpParsedArticle pa = new CorenlpParsedArticle();
		pa.sectionId = sectionId;
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		pa.numSentence = sentences.size();
		for (int i = 0; i < sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			ParsedSentence ps = new ParsedSentence(pa.sectionId, i, sentence, gsf);
			pa.parsedsentence.add(ps);
		}
		{
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			// D.p(graph.size());
			for (Entry<Integer, CorefChain> e : graph.entrySet()) {
				int chainid = e.getKey();
				CorefChain cc = e.getValue();
				CorefResult cr = new CorefResult();
				pa.corefchains.add(cr);
				// for (CorefMention m : cc.getCorefMentions()) {
				// // for (CorefMention m : cc.getMentionsInTextualOrder()) {
				// cr.names.add(m.mentionSpan);
				// cr.chain.add(new int[] { m.sentNum - 1, m.startIndex - 1,
				// m.endIndex - 1 });
				// // D.p(m.toString(), cc.getChainID());
				// }
			}
			graph = null;
		}
		document = null;
		sentences = null;
		return pa;
	}

	public String parseDocumentJsonWithCoref(int sectionId, String text, Gson gson) {

		// l[]: article_name (String), section_id (int), text
		CorenlpParsedArticle pa = new CorenlpParsedArticle();
		pa.sectionId = sectionId;
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		pa.numSentence = sentences.size();
		for (int i = 0; i < sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			ParsedSentence ps = new ParsedSentence(pa.sectionId, i, sentence, gsf);
			pa.parsedsentence.add(ps);
		}
		String result = "";
		{
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			// D.p(graph.size());
			for (Entry<Integer, CorefChain> e : graph.entrySet()) {
				int chainid = e.getKey();
				CorefChain cc = e.getValue();
				CorefResult cr = new CorefResult();
				pa.corefchains.add(cr);
				// for (CorefMention m : cc.getCorefMentions()) {
				// // for (CorefMention m : cc.getMentionsInTextualOrder()) {
				// cr.names.add(m.mentionSpan);
				// cr.chain.add(new int[] { m.sentNum - 1, m.startIndex - 1,
				// m.endIndex - 1 });
				// // D.p(m.toString(), cc.getChainID());
				// }
			}
			graph = null;
			result = gson.toJson(pa);

		}
		document = null;
		sentences = null;
		return result;
	}

	public ParsedSentence parseSentence(String sentence_str) {
		Annotation document = new Annotation(sentence_str);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		// all sentences are actually inside one sentence, so there is an offset
		// for dep

		int totallen = 0;
		List<ParsedSentence> subs = new ArrayList<ParsedSentence>();

		for (int i = 0; i < sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			ParsedSentence ps = new ParsedSentence(0, 0, sentence, gsf);
			subs.add(ps);
			totallen += ps.tkn.length;
		}
		String[] tkn = new String[totallen];
		String[] pos = new String[totallen];
		String[] lmma = new String[totallen];
		String[] ner = new String[totallen];
		List<SentDep> sentdeps = new ArrayList<SentDep>();
		int offset = 0;
		for (ParsedSentence ps : subs) {
			for (int k = 0; k < ps.tkn.length; k++) {
				tkn[offset + k] = ps.tkn[k];
				pos[offset + k] = ps.pos[k];
				lmma[offset + k] = ps.lmma[k];
				ner[offset + k] = ps.ner[k];
			}
			for (SentDep sd : ps.deps) {
				SentDep nsd = new SentDep(sd.g + offset, sd.d + offset, sd.t);
				sentdeps.add(nsd);
			}
			offset += ps.tkn.length;
		}
		ParsedSentence ps0 = new ParsedSentence(-1, -1, tkn, pos, ner, lmma, sentdeps);
		return ps0;
	}

	public void parseDocumentJson(String input, String output) throws FileNotFoundException, IOException {
		DelimitedReader dr = new DelimitedReader(input);
		// GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new
		// File(output + ".gz")));
		BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(output + ".gz"))), "UTF-8"));
		// DelimitedWriter dw = new DelimitedWriter(output);
		Gson gson = new Gson();
		String[] l;
		// l[]: article_name (String), section_id (int), text
		int numSections = 0;
		long startTime = (new Date()).getTime();
		while ((l = dr.read()) != null) {
			ParsedText pt = new ParsedText();
			pt.sectionId = Integer.parseInt(l[0]);

			String text = l[1];
			Annotation document = new Annotation(text);
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			pt.numSentence = sentences.size();
			for (int i = 0; i < sentences.size(); i++) {
				CoreMap sentence = sentences.get(i);
				ParsedSentence ps = new ParsedSentence(pt.sectionId, i, sentence, gsf);
				pt.parsedsentence.add(ps);
			}
			String jsonstr = gson.toJson(pt);
			bw.write(pt.sectionId + "\t" + jsonstr + "\n");
			{// speed
				numSections++;
				long spend = (new Date()).getTime() - startTime;
				if (numSections % 100 == 0) {
					D.p(input, "Avg speed", spend * 1.0 / numSections);
					bw.flush();
				}
			}
		}
		bw.close();
		dr.close();
	}

	public void parseDocumentJson2(String input, String output) {

		Gson gson = new Gson();

		// l[]: article_name (String), section_id (int), text
		int numSections = 0;
		long startTime = (new Date()).getTime();

		List<String[]> lines = new ArrayList<String[]>();
		List<String[]> tow = new ArrayList<String[]>();
		{
			DelimitedReader dr = new DelimitedReader(input);
			String[] l;

			while ((l = dr.read()) != null) {
				lines.add(l);
			}
			dr.close();
		}
		for (String[] l : lines) {
			ParsedText pt = new ParsedText();
			pt.sectionId = Integer.parseInt(l[0]);

			String text = l[1];
			Annotation document = new Annotation(text);
			pipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			pt.numSentence = sentences.size();
			for (int i = 0; i < sentences.size(); i++) {
				CoreMap sentence = sentences.get(i);
				ParsedSentence ps = new ParsedSentence(pt.sectionId, i, sentence, gsf);
				pt.parsedsentence.add(ps);
			}
			String jsonstr = gson.toJson(pt);
			// dw.write(pt.sectionId, jsonstr);
			tow.add(new String[] { pt.sectionId + "", jsonstr });
			{// speed
				numSections++;
				long spend = (new Date()).getTime() - startTime;
				if (numSections % 10 == 0) {
					D.p(input, "Avg speed", spend * 1.0 / numSections);
					// dw.flush();
				}
			}
		}
		{
			DelimitedWriter dw = new DelimitedWriter(output);
			for (String[] w : tow) {
				dw.write(w);
			}
			dw.close();
		}
	}

	public void parseDocument(String input, String output) {
		int sentenceId = 0;
		DelimitedReader dr = new DelimitedReader(input);
		DelimitedWriter dw_meta = new DelimitedWriter(output + ".sectionId");
		DelimitedWriter dw_token = new DelimitedWriter(output + ".token");
		DelimitedWriter dw_pos = new DelimitedWriter(output + ".pos");
		DelimitedWriter dw_morpha = new DelimitedWriter(output + ".morpha");
		DelimitedWriter dw_ner = new DelimitedWriter(output + ".ner");
		DelimitedWriter dw_deps = new DelimitedWriter(output + ".deps");
		DelimitedWriter dw_coref = new DelimitedWriter(output + ".coref");

		String[] l;
		// l[]: article_name (String), section_id (int), text
		while ((l = dr.read()) != null) {
			int section_id = Integer.parseInt(l[0]);
			String text = l[1];
			parse(section_id, text, dw_token, dw_pos, dw_morpha, dw_deps, dw_ner, dw_coref);
			// for (int i = sentenceId; i < updatedsentenceId; i++) {
			// dw_meta.write(section_id, i);
			// }
			// sentenceId = updatedsentenceId;
		}
		dr.close();
		dw_meta.close();
		dw_token.close();
		dw_pos.close();
		dw_morpha.close();
		dw_ner.close();
		dw_deps.close();
		dw_coref.close();

	}

	public static List<String[]> processNer(List<String> nelist) {
		List<String[]> nes = new ArrayList<String[]>();
		List<String[]> bbb = new ArrayList<String[]>();
		for (String a : nelist) {
			bbb.add(new String[] { a });
		}
		List<List<String[]>> blocks = StringTable.toblock(bbb, 0);
		int k = 0;
		for (List<String[]> b : blocks) {
			String nertype = b.get(0)[0];
			if (nertype.equals("O"))
				continue;
			nes.add(new String[] { k + "", (k + b.size()) + "", nertype });
			k += b.size();
		}
		return nes;
	}

	public void parse(int sectionId, String text, DelimitedWriter dw_token, DelimitedWriter dw_pos,
			DelimitedWriter dw_morph, DelimitedWriter dw_deps, DelimitedWriter dw_ner, DelimitedWriter dw_coref) {
		// int sentenceId = 0;
		List<List<String[]>> result = new ArrayList<List<String[]>>();
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (int sentenceId = 0; sentenceId < sentences.size(); sentenceId++) {
			CoreMap sentence = sentences.get(sentenceId);
			// for (CoreMap sentence : sentences) {
			List<String> tokenlist = new ArrayList<String>();
			List<String> poslist = new ArrayList<String>();
			List<String> inputlist = new ArrayList<String>();
			List<String> morphlist = new ArrayList<String>();
			List<String> depslist = new ArrayList<String>();
			List<String> nelist = new ArrayList<String>();
			List<String[]> parsed_sentence = new ArrayList<String[]>();
			int lastNe = -1;
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				String morph = token.get(LemmaAnnotation.class);
				String ne = token.get(NamedEntityTagAnnotation.class);
				nelist.add(ne);
				poslist.add(pos);
				morphlist.add(morph);
				inputlist.add(word + "_" + pos);
				tokenlist.add(word);
				D.p(ne);
				// D.p(word, pos, ne, morph);
				// parsed_sentence.add(r);
				// System.out.println(word + "\t" + pos + "\t" + ne);

			}

			dw_token.write(sectionId, sentenceId, StringUtil.join(tokenlist, " "));
			dw_pos.write(sectionId, sentenceId, StringUtil.join(poslist, " "));
			dw_morph.write(sectionId, sentenceId, StringUtil.join(morphlist, " "));

			List<String[]> netowrite = processNer(nelist);
			for (String[] w : netowrite) {
				dw_ner.write(sectionId, sentenceId, w[0], w[1], w[2]);
			}
			Tree tree = sentence.get(TreeAnnotation.class);
			GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
			Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed(true);
			{
				StringBuilder sb = new StringBuilder();
				for (TypedDependency td : tdl) {
					// TypedDependency td = tdl.(i);
					String name = td.reln().getShortName();
					if (td.reln().getSpecific() != null)
						name += "-" + td.reln().getSpecific();

					int gov = td.gov().index();
					int dep = td.dep().index();
					if (gov == dep) {
						// System.err.println("same???");
					}
					if (gov == 0 || dep == 1) {
						continue;
					}
					depslist.add(name + "(" + tokenlist.get(gov - 1) + "-" + gov + ", " + tokenlist.get(dep - 1) + "-"
							+ dep + ")");
					sb.append((td.gov().index()) + " ");
					sb.append(name + " ");
					sb.append((td.dep().index()));
					sb.append("|");
					// if (i < tdl.size() - 1)
					// sb.append("|");
				}
				dw_deps.write(sectionId, sentenceId, sb.toString());
			}
		}
		{
			Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
			D.p(graph.size());
			List<String[]> tow = new ArrayList<String[]>();
			for (Entry<Integer, CorefChain> e : graph.entrySet()) {
				// int chainid = e.getKey();
				CorefChain cc = e.getValue();

				// for (CorefMention m : cc.getCorefMentions()) {
				// // for (CorefMention m : cc.getMentionsInTextualOrder()) {
				// int sid = m.sentNum;
				// tow.add(new String[] { sid + "",
				// m.corefClusterID + "",
				// m.mentionID + "",
				// m.startIndex + "",
				// m.endIndex + "",
				// m.gender + "",
				// m.mentionSpan + "",
				// m.mentionType + "",
				// m.number + ""
				// });
				// }
			}
			Collections.sort(tow, new Comparator<String[]>() {
				public int compare(String[] x, String[] y) {
					// TODO Auto-generated method stub
					return Integer.parseInt(x[1]) - Integer.parseInt(y[1]);
				}
			});
			for (String[] w : tow) {
				dw_coref.write(w);
			}
		}
	}

	public List<List<String[]>> parse(String rawText) {
		// create an empty Annotation just with the given text
		List<List<String[]>> result = new ArrayList<List<String[]>>();
		Annotation document = new Annotation(rawText);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		List<String> tokenlist = new ArrayList<String>();
		List<String> inputlist = new ArrayList<String>();
		List<String> morphlist = new ArrayList<String>();
		List<String> depslist = new ArrayList<String>();
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			List<String[]> parsed_sentence = new ArrayList<String[]>();
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the NER label of the token
				// String ne = token.get(NamedEntityTagAnnotation.class);
				String morph = token.get(LemmaAnnotation.class);

				morphlist.add(morph);
				inputlist.add(word + "_" + pos);
				tokenlist.add(word);
				// D.p(word, pos, ne, morph);
				// parsed_sentence.add(r);
				// System.out.println(word + "\t" + pos + "\t" + ne);

			}
			// result.add(parsed_sentence);
			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
			Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed(true);
			{
				StringBuilder sb = new StringBuilder();
				for (TypedDependency td : tdl) {
					// TypedDependency td = tdl.(i);
					String name = td.reln().getShortName();
					if (td.reln().getSpecific() != null)
						name += "-" + td.reln().getSpecific();

					int gov = td.gov().index();
					int dep = td.dep().index();
					if (gov == dep) {
						// System.err.println("same???");
					}
					if (gov < 1 || dep < 1) {
						continue;
					}
					depslist.add(name + "(" + tokenlist.get(gov - 1) + "-" + gov + ", " + tokenlist.get(dep - 1) + "-"
							+ dep + ")");
					sb.append((td.gov().index()) + " ");
					sb.append(name + " ");
					sb.append((td.dep().index()));
					sb.append("|");
					// if (i < tdl.size() - 1)
					// sb.append("|");
				}
				// System.out.println(sb.toString());
				// this is the Stanford dependency graph of the current sentence
				// SemanticGraph dependencies =
				// sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				// dependencies.
				// D.p(dependencies);

				// This is the coreference link graph
				// Each chain stores a set of mentions that link to each other,
				// along with a method for getting the most representative
				// mention
				// Both sentence and token offsets start at 1!
			}
			{
				Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
				D.p(graph.size());
				for (Entry<Integer, CorefChain> e : graph.entrySet()) {
					int chainid = e.getKey();
					CorefChain cc = e.getValue();
					// for (CorefMention m : cc.getMentionsInTextualOrder()) {
					// for (CorefMention m : cc.getCorefMentions()) {
					// D.p(m.toString(), cc.getChainID());
					// }
				}
			}
			for (String a : inputlist) {
				D.p(a);
			}
			D.p("\n");
			for (String a : morphlist) {
				D.p(a);
			}
			D.p("\n");
			for (String a : depslist) {
				D.p(a);
			}
			D.p("\n");
		}
		return result;

	}

	public List<String[]> parse2lines(String rawtext) {
		List<String[]> result = new ArrayList<String[]>();
		List<List<String[]>> parsed = parse(rawtext);
		for (List<String[]> s : parsed) {
			String towrite[] = new String[5];

			StringBuilder sb1 = new StringBuilder(), sb2 = new StringBuilder(), sb3 = new StringBuilder();
			for (String[] w : s) {
				// if(w[0].contains(" ") || w[1].contains(" ")||
				// w[2].contains(" ")){
				// System.err.println("blank happens!!!");
				// }
				w[0] = w[0].replaceAll(" ", "_");
				w[1] = w[1].replaceAll(" ", "_");
				w[2] = w[2].replaceAll(" ", "_");
				sb1.append(w[0] + " ");
				sb2.append(w[1] + " ");
				sb3.append(w[2] + " ");
			}
			int id = 0;
			towrite[0] = sb1.toString();
			towrite[1] = sb2.toString();
			towrite[2] = sb3.toString();
			result.add(towrite);
		}
		return result;
	}

}
