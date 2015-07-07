package edu.washington.nsre.extraction;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

public class Factor {
	Counter<String> features = new ClassicCounter<String>();

	public void add(String f) {
		f = f.replaceAll(" ", "_");
		features.incrementCount(f);
	}

	public double score(Counter<String> weight, double bias) {
		double score = bias;
		for (String f : features.keySet()) {
			score += features.getCount(f) * weight.getCount(f);
		}
		return score;
	}
}
