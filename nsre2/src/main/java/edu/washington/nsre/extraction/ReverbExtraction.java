package edu.washington.nsre.extraction;

import edu.washington.nsre.util.*;

public class ReverbExtraction {
	public ReverbPhrase arg1;
	public ReverbPhrase arg2;
	public ReverbPhrase verb;

	public ReverbSentence rs;

	public ReverbExtraction(ReverbPhrase arg1, ReverbPhrase verb, ReverbPhrase arg2, ReverbSentence rs) {
		this.rs = rs;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.verb = verb;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(arg1).append("|").append(verb).append("|").append(arg2);
		return sb.toString();
	}

	public String getVerbLmma() {
		StringBuilder sb = new StringBuilder();
		for (int i = this.verb.start; i < this.verb.end; i++) {
			sb.append(rs.lmma[i] + " ");
		}
		return sb.toString().trim();
	}

	public String getTripleLmma() {
		StringBuilder sb = new StringBuilder();
		for (int i = this.arg1.start; i < this.arg1.end; i++) {
			sb.append(rs.lmma[i] + " ");
		}
		for (int i = this.verb.start; i < this.verb.end; i++) {
			sb.append(rs.lmma[i] + " ");
		}
		for (int i = this.arg2.start; i < this.arg2.end; i++) {
			sb.append(rs.lmma[i] + " ");
		}
		return sb.toString().trim();
	}

	public String[] getArgsLmma() {
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (int i = this.arg1.start; i < this.arg1.end; i++) {
			sb1.append(rs.lmma[i] + " ");
		}

		for (int i = this.arg2.start; i < this.arg2.end; i++) {
			sb2.append(rs.lmma[i] + " ");
		}
		String[] res = new String[2];
		res[0] = sb1.toString().trim();
		res[1] = sb2.toString().trim();
		return res;
	}

	public String[] getArgsLmmaNoHe() {
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (int i = this.arg1.start; i < this.arg1.end; i++) {
			if (!RemoveStopwords.isStop(rs.lmma[i])) {
				sb1.append(rs.lmma[i] + " ");
			}
		}

		for (int i = this.arg2.start; i < this.arg2.end; i++) {
			if (!RemoveStopwords.isStop(rs.lmma[i])) {
				sb2.append(rs.lmma[i] + " ");
			}
		}
		String[] res = new String[2];
		res[0] = sb1.toString().trim();
		res[1] = sb2.toString().trim();
		return res;
	}

	public String[] getTripple() {
		String[] res = new String[3];
		res[0] = arg1.str;
		res[1] = verb.str;
		res[2] = arg2.str;
		return res;
	}
}
