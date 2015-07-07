package edu.washington.nsre.stanfordtools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.washington.nsre.util.*;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class ParsedSentence {
	public int sectId;
	public int sentId; // relative id
	public int len;
	public int[] beginPos;
	public int[] endPos;
	public String[] tkn;
	public String[] pos;
	public String[] ner;
	public String[] lmma;
	public List<SentDep> deps;

	public ParsedSentence(int sectId, int sentId, CoreMap sentence, GrammaticalStructureFactory gsf) {
		this.sectId = sectId;
		this.sentId = sentId;

		List<String> tokenlist = new ArrayList<String>();
		List<String> poslist = new ArrayList<String>();
		List<String> morphlist = new ArrayList<String>();
		List<String> depslist = new ArrayList<String>();
		List<String> nelist = new ArrayList<String>();
		List<String[]> parsed_sentence = new ArrayList<String[]>();

		List<CoreLabel> labeledtokens = sentence.get(TokensAnnotation.class);
		this.len = labeledtokens.size();
		tkn = new String[len];
		pos = new String[len];
		ner = new String[len];
		lmma = new String[len];
		beginPos = new int[len];
		endPos = new int[len];
		deps = new ArrayList<SentDep>();

		for (int i = 0; i < labeledtokens.size(); i++) {
			CoreLabel token = labeledtokens.get(i);
			beginPos[i] = token.beginPosition();
			endPos[i] = token.endPosition();
			tkn[i] = token.get(TextAnnotation.class);
			pos[i] = token.get(PartOfSpeechAnnotation.class);
			lmma[i] = token.get(LemmaAnnotation.class);
			ner[i] = token.get(NamedEntityTagAnnotation.class);
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
				if (gov < 1 || dep < 1) {
					continue;
				}
				SentDep sd = new SentDep(gov - 1, dep - 1, name);
				this.deps.add(sd);
			}
		}
	}

	public ParsedSentence(int sectId, int sentId, String[] tkn, String[] pos, String[] ner,
			String[] lmma,
			List<SentDep> deps) {
		this.sectId = sectId;
		this.sentId = sentId;
		this.tkn = tkn;
		this.pos = pos;
		this.ner = ner;
		this.lmma = lmma;
		this.deps = deps;
	}
}
