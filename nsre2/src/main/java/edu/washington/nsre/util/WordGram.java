package edu.washington.nsre.util;

import java.util.HashMap;

public class WordGram {

	String input_wordcount;
	HashMap<String, Integer> wordcount = new HashMap<String, Integer>();

	public WordGram(String input_wordcount) {
		this.input_wordcount = input_wordcount;
		DR dr = new DR(input_wordcount);
		String[] l;
		while ((l = dr.read()) != null) {
			wordcount.put(l[0], Integer.parseInt(l[1]));
		}
		dr.close();
	}

	public String keyWordWithLowestFrequency(String phrase) {
		String[] xyz = phrase.split(" ");
		int keyCount = 10000000;
		String key = null;
		for (String a : xyz) {
			int c = 0;
			if (wordcount.containsKey(a)) {
				c = wordcount.get(a);
			}
			if (key == null || keyCount > c) {
				key = a;
				keyCount = c;
			}
		}
		return key;
	}

	public int getCount(String w) {
		int ret = 0;
		if (wordcount.containsKey(w)) {
			ret = wordcount.get(w);
		}
		return ret;
	}
}
