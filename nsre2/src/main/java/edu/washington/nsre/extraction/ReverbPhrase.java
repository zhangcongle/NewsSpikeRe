package edu.washington.nsre.extraction;

import edu.washington.nsre.stanfordtools.ParsedSentence;
import edu.washington.nsre.util.StringUtil;

public class ReverbPhrase {
	public String uniqid;
	public int start;
	public int end;
	public int head;
	public String pos; // NP or VP?
	public ReverbSentence rs;
	public String str;

	/**
	 * @param args
	 */

	public ReverbPhrase(ReverbSentence rs, int start, int end, int head, String pos) {
		this.rs = rs;
		this.start = start;
		this.end = end;
		this.head = head;
		this.pos = pos;
		this.str = StringUtil.join(rs.tkn, " ", start, end);
		uniqid = getPhraseId();
	}

	public String getPhraseId() {
		return rs.sentenceId + "_" + start + "_" + end;
	}

	public String toString() {
		return this.str;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
