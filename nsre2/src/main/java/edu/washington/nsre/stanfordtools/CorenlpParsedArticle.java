package edu.washington.nsre.stanfordtools;

import java.util.ArrayList;
import java.util.List;

/**
 * When i don't want coref, just use ParsedText!
 **/
public class CorenlpParsedArticle {
	public int sectionId;
	public int numSentence;
	public List<ParsedSentence> parsedsentence = new ArrayList<ParsedSentence>();
	public List<CorefResult> corefchains = new ArrayList<CorefResult>();

}