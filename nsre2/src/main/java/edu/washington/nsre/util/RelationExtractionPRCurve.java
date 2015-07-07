package edu.washington.nsre.util;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;



//import edu.stanford.nlp.kbp.slotfilling.MultiR;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.IntCounter;
//import edu.uw.multir.DW;
import edu.stanford.nlp.util.Triple;

public class RelationExtractionPRCurve {
	public List<double[]> prall;
	public HashMap<String, List<double[]>> prcurveByrel = new HashMap<String, List<double[]>>();
	public Set<String> relations;
	public double[] bestF1All;
	public HashMap<String, double[]> bestF1ByRel = new HashMap<String, double[]>();
	public Counter<String> truePositiveByRel = new IntCounter<String>();

	static class Pred {
		String gold;
		String pred;
		double score;
	}

	static Gson gson = new Gson();

	public void prcurve_old(String input_gold, String input_pred) {
		DR drg = new DR(input_gold);
		DR drp = new DR(input_pred);
		String[] g;
		String[] p;
		List<Pred> predictions = new ArrayList<Pred>();
		Set<String> relations = new HashSet<String>();
		while ((g = drg.read()) != null) {
			p = drp.read();
			HashMap<String, Double> pred_scorer = gson.fromJson(p[0], HashMap.class);
			HashMap<String, Double> gold_scorer = gson.fromJson(g[0], HashMap.class);
			if (gold_scorer.size() == 0) {
				gold_scorer.put("NA", 1.0);
			}
			if (pred_scorer.size() == 0) {
				pred_scorer.put("NA", 0.0);
			}
			for (String grel : gold_scorer.keySet()) {
				for (String prel : pred_scorer.keySet()) {
					double score = pred_scorer.get(prel);
					Pred onep = new Pred();
					onep.gold = grel;
					onep.pred = prel;
					onep.score = score;
					if (!onep.gold.equals("NA") || !onep.pred.equals("NA")) {
						predictions.add(onep);
					}
					if (!grel.equals("NA")) {
						relations.add(grel);
					}
					if (!prel.equals("NA")) {
						relations.add(prel);
					}
				}
			}
		}
		Collections.sort(predictions, new Comparator<Pred>() {
			public int compare(Pred o1, Pred o2) {
				return Double.compare(o2.score, o1.score);
			}
		});
		prall = prTable(predictions, "ALL");
		bestF1All = getBestF1(prall);
		for (String r : relations) {
			List<double[]> pr = prTable(predictions, r);
			prcurveByrel.put(r, pr);
			double[] bestf1 = getBestF1(pr);
			bestF1ByRel.put(r, bestf1);
		}
		drg.close();
		drp.close();
	}
	private static List<Triple<Integer, String, Double>> convertToSorted(List<Counter<String>> predictedLabels) {
		List<Triple<Integer, String, Double>> sorted = new ArrayList<Triple<Integer, String, Double>>();
		for (int i = 0; i < predictedLabels.size(); i++) {
			for (String l : predictedLabels.get(i).keySet()) {
				double s = predictedLabels.get(i).getCount(l);
				sorted.add(new Triple<Integer, String, Double>(i, l, s));
			}
		}
		Collections.sort(sorted, new Comparator<Triple<Integer, String, Double>>() {
			public int compare(Triple<Integer, String, Double> t1, Triple<Integer, String, Double> t2) {
				if (t1.third() > t2.third())
					return -1;
				else if (t1.third() < t2.third())
					return 1;
				return 0;
			}
		});
		return sorted;
	}
	private static Triple<Double, Double, Double> score(List<Triple<Integer, String, Double>> preds,
			List<Set<String>> golds) {
		int total = 0, predicted = 0, correct = 0;
		for (int i = 0; i < golds.size(); i++) {
			Set<String> gold = golds.get(i);
			total += gold.size();
		}
		for (Triple<Integer, String, Double> pred : preds) {
			predicted++;
			if (golds.get(pred.first()).contains(pred.second()))
				correct++;
		}

		double p = (double) correct / (double) predicted;
		double r = (double) correct / (double) total;
		double f1 = (p != 0 && r != 0 ? 2 * p * r / (p + r) : 0);
		return new Triple<Double, Double, Double>(p, r, f1);
	}
	private static void generatePRCurveNonProbScores(PrintStream os,
			List<Set<String>> goldLabels,
			List<Counter<String>> predictedLabels) {
		// each triple stores: position of tuple in gold, one label for this
		// tuple, its score
		List<Triple<Integer, String, Double>> preds = convertToSorted(predictedLabels);
		double prevP = -1, prevR = -1;
		int START_OFFSET = 10; // score at least this many predictions (makes no
								// sense to score 1...)
		for (int i = START_OFFSET; i < preds.size(); i++) {
			List<Triple<Integer, String, Double>> filteredLabels = preds.subList(0, i);
			Triple<Double, Double, Double> score = score(filteredLabels, goldLabels);
			if (score.first() != prevP || score.second() != prevR) {
				double ratio = (double) i / (double) preds.size();
				os.println(ratio + " P " + score.first() + " R " + score.second() + " F1 " + score.third());
				prevP = score.first();
				prevR = score.second();
			}
		}
	}
	public void prcurve(String input_gold, String input_pred) {
		DR drg = new DR(input_gold);
		DR drp = new DR(input_pred);
		String[] g;
		String[] p;
		List<Pred> predictions = new ArrayList<Pred>();
		relations = new HashSet<String>();
		List<Set<String>> goldLabels = new ArrayList<Set<String>>();
		List<Counter<String>> predictedLabels = new ArrayList<Counter<String>>();

		while ((g = drg.read()) != null) {
			p = drp.read();
			HashMap<String, Double> pred_scorer = gson.fromJson(p[0], HashMap.class);
			HashMap<String, Double> gold_scorer = gson.fromJson(g[0], HashMap.class);
			Set<String> gls = new HashSet<String>();
			for (String gl : gold_scorer.keySet()) {
				gls.add(gl);
				relations.add(gl);
			}
			goldLabels.add(gls);
			Counter<String> preds = new ClassicCounter<String>();
			for (String rel : pred_scorer.keySet()) {
				preds.incrementCount(rel, pred_scorer.get(rel));
			}
			predictedLabels.add(preds);
		}
		this.prall = generatePRCurveNonProbScores(goldLabels, predictedLabels);
		// D.p("all");
		this.bestF1All = getBestF1(prall);

		for (String r : relations) {
			List<Set<String>> goldLabels_r = new ArrayList<Set<String>>();
			List<Counter<String>> predictedLabels_r = new ArrayList<Counter<String>>();
			for (int i = 0; i < goldLabels.size(); i++) {
				Set<String> goldLabel = goldLabels.get(i);
				Counter<String> c = predictedLabels.get(i);
				if (goldLabels.contains(r) || c.getCount(r) > 0) {
					goldLabels_r.add(goldLabel);
					predictedLabels_r.add(c);
				}
			}
			List<double[]> pr = generatePRCurveNonProbScores(goldLabels_r, predictedLabels_r);
			// D.p(r);
			prcurveByrel.put(r, pr);
			double[] bestf1 = getBestF1(pr);
			bestF1ByRel.put(r, bestf1);
		}
		drg.close();
		drp.close();
	}

	public int numTruePositive(List<Set<String>> goldLabels) {
		int ret = 0;
		for (Set<String> label : goldLabels) {
			if (label.size() > 0)
				ret++;
			for (String r : label) {
				this.truePositiveByRel.incrementCount(r);
			}
		}
		return ret;
	}

	public void prcurve(List<Set<String>> goldLabels, List<Counter<String>> predictedLabels) {
		relations = new HashSet<String>();
		for (Set<String> gls : goldLabels) {
			for (String r : gls) {
				relations.add(r);
			}
		}
		this.prall = generatePRCurveNonProbScores(goldLabels, predictedLabels);
		this.bestF1All = getBestF1(prall);
		for (String r : relations) {
			List<Set<String>> goldLabels_r = new ArrayList<Set<String>>();
			List<Counter<String>> predictedLabels_r = new ArrayList<Counter<String>>();
			for (int i = 0; i < goldLabels.size(); i++) {
				Set<String> goldLabel = goldLabels.get(i);
				Counter<String> c = predictedLabels.get(i);
				Set<String> goldLabel_onlyr = new HashSet<String>();
				Counter<String> predLabel_onlyr = new ClassicCounter<String>();
				if (goldLabel.contains(r)) {
					goldLabel_onlyr.add(r);
				}
				if (c.getCount(r) > 0) {
					predLabel_onlyr.incrementCount(r, c.getCount(r));
				}
				if (goldLabel.contains(r) || c.getCount(r) > 0) {
					goldLabels_r.add(goldLabel_onlyr);
					predictedLabels_r.add(predLabel_onlyr);
				}
			}
			List<double[]> pr = generatePRCurveNonProbScores(goldLabels_r, predictedLabels_r);
			prcurveByrel.put(r, pr);
			double[] bestf1 = getBestF1(pr);
			bestF1ByRel.put(r, bestf1);
		}
	}

	public static List<double[]> generatePRCurveNonProbScores(
			List<Set<String>> goldLabels,
			List<Counter<String>> predictedLabels) {
		
		List<double[]> ret = new ArrayList<double[]>();
		HashSet<String> golds = new HashSet<String>();
		for (int i = 0; i < goldLabels.size(); i++) {
			Set<String> one = goldLabels.get(i);
			for (String r : one) {
				golds.add(i + "\t" + r);
			}
		}
		Counter<String> preds = new ClassicCounter<String>();
		for (int i = 0; i < predictedLabels.size(); i++) {
			Counter<String> one = predictedLabels.get(i);
			for (String r : one.keySet()) {
				preds.setCount(i + "\t" + r, one.getCount(r));
			}
		}
		List<String> predSorted = Counters.toSortedList(preds);
		double truepos = 0, falsepos = 0, falseneg = 0;
		for (int i = 0; i < predSorted.size(); i++) {
			String a = predSorted.get(i);
			if (golds.contains(a)) {
				truepos++;
			} else {
				falsepos++;
			}
			double precision = truepos * 1.0 / (truepos + falsepos);
			double recall = truepos * 1.0 / golds.size();
			ret.add(new double[] { precision, recall });
			// D.p( precision, recall, a);
		}
		return ret;
	}

	public static List<double[]> prTable(List<Pred> sorted_preds,
			String targetRel) {
		int totalGold = 0;
		for (Pred pred : sorted_preds) {
			if (pred.gold.equals(targetRel)
					|| targetRel.equals("ALL")) {
				totalGold++;
			}
		}
		List<double[]> prtable = new ArrayList<double[]>();
		int truepos = 0, falsepos = 0;
		for (Pred pred : sorted_preds) {
			if (pred.pred.equals(targetRel) || pred.gold.equals(targetRel) || targetRel.equals("ALL")) {
				if (pred.pred.equals(pred.gold) && !pred.pred.equals("NA")) {
					truepos++;
					prtable.add(new double[] { truepos * 1.0 / (truepos + falsepos),
							truepos * 1.0 / totalGold });
				} else if (!pred.pred.equals(pred.gold) && !pred.pred.equals("NA")) {
					falsepos++;
					prtable.add(new double[] { truepos * 1.0 / (truepos + falsepos),
							truepos * 1.0 / totalGold });
				}
			} else {
				// do not consider this point
			}
		}
		return prtable;
	}

	public static double[] getFinalF1(List<double[]> prtable) {
		double[] ret = new double[3];
		if (prtable.size() > 0) {
			double[] pr = prtable.get(prtable.size() - 1);
			double f1 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
			ret[0] = pr[0];
			ret[1] = pr[1];
			ret[2] = f1;
		}
		return ret;
	}

	public static double[] getBestF1(List<double[]> prtable) {
		double[] best = new double[3];
		for (double[] pr : prtable) {
			double f1 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
			if (f1 > best[2]) {
				best[0] = pr[0];
				best[1] = pr[1];
				best[2] = f1;
			}
		}
		return best;

	}

	public static double getAveragePrecision(List<double[]> prtable) {
		double sum = 0;
		for (int i = 0; i < prtable.size(); i++) {
			double[] pr = prtable.get(i);
			if (i >= 1) {
				double[] pr2 = prtable.get(i - 1);
				sum += ((pr[0] + pr2[0]) / 2) * (pr[1] - pr2[1]);
			} else {
				sum += pr[0] * pr[1];
			}
		}
		return sum;
	}

	public static void main(String[] args) {
		RelationExtractionPRCurve repr = new RelationExtractionPRCurve();

		repr.prcurve(args[0], args[1]);

		for (String r : repr.bestF1ByRel.keySet()) {
			// D.p(r, gson.toJson(repr.bestF1ByRel.get(r)));
		}
		String outputdir = args[2];

		if (!new File(outputdir).exists()) {
			(new File(outputdir)).mkdir();
		}
		DW dwreport = new DW(outputdir + "/summary");
		{
			D.p("ALL", gson.toJson(repr.bestF1All));
			DW dw = new DW(outputdir + "/all.pr");
			for (double[] pr : repr.prall) {
				double f1 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
				dw.write(pr[0], pr[1], f1);
			}
			dw.close();
			double[] finalf1 = repr.getFinalF1(repr.prall);
			double[] bestf1 = repr.getBestF1(repr.prall);
			double averageprec = repr.getAveragePrecision(repr.prall);
			dwreport.write("ALL", finalf1[2], bestf1[2], averageprec);
		}
		{
			for (String rel : repr.prcurveByrel.keySet()) {
				List<double[]> prs = repr.prcurveByrel.get(rel);
				String filename = rel.replaceAll("\\W+", "_");
				DW dw = new DW(outputdir + "/" + filename + ".pr");
				for (double[] pr : prs) {
					double f1 = 2 * pr[0] * pr[1] / (pr[0] + pr[1]);
					dw.write(pr[0], pr[1], f1);
				}
				dw.close();
				double[] finalf1 = repr.getFinalF1(prs);
				double[] bestf1 = repr.getBestF1(prs);
				double averageprec = repr.getAveragePrecision(prs);
				dwreport.write(rel, finalf1[2], bestf1[2], averageprec);
			}
		}
		dwreport.close();

	}
}
