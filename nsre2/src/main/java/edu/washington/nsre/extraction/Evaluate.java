package edu.washington.nsre.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.washington.nsre.util.*;

import com.google.common.collect.HashBasedTable;
import com.google.gson.Gson;

public class Evaluate {
	static Gson gson = new Gson();

	public static void evaluate(
			String input_tuples_test,
			String input_preds,
			String input_labeled,
			String output_prefix) {
		HashBasedTable<String, String, Boolean> tuplegolds = HashBasedTable.create();
		HashBasedTable<String, String, Double> tuplepreds = HashBasedTable.create();
		HashMap<String, String[]> eecname2visual = new HashMap<String, String[]>();
		HashMap<String, String[]> tuple2visual = new HashMap<String, String[]>();
		HashMap<String, Tuple> tupleId2tuple = new HashMap<String, Tuple>();
		{
			DR dr = new DR(input_tuples_test);
			String[] l;
			int id = 0;
			while ((l = dr.read()) != null) {
				Tuple t = gson.fromJson(l[2], Tuple.class);
				tuple2visual.put(
						id + "",
						new String[] { t.getArg1(), t.getPatternListStr(), t.getArg2(),
								t.getSentence() });
				tupleId2tuple.put(id + "", t);
				id++;
			}
			dr.close();
		}
		if (!new File(output_prefix).exists()) {
			new File(output_prefix).mkdirs();
		}
		// load gold
		{
			DR dr = new DR(input_labeled);
			String[] l;
			while ((l = dr.read()) != null) {
				String rel = l[1];
				int id = Integer.parseInt(l[6]);
				boolean yes = l[0].equals("1") ? true : false;
				tuplegolds.put(id + "", rel, yes);
			}
			dr.close();
		}
		// load pred
		{
			DR dr = new DR(input_preds);
			DW dw = new DW(output_prefix + "/debug");
			String[] l;
			int id = 0;
			while ((l = dr.read()) != null) {
				HashMap<String, Double> score = gson.fromJson(l[0], HashMap.class);
				// int id = Integer.parseInt(l[5]);
				Tuple t = tupleId2tuple.get(id + "");
				if (tuplegolds.containsRow(id + "")) {
					dw.write(gson.toJson(tuplegolds.row(id + "")),
							gson.toJson(score),
							t.getArg1(),
							t.getArg2(),
							t.getPatternListStr(),
							t.getSentence());
				}
				for (String e : score.keySet()) {
					tuplepreds.put(id + "", e, score.get(e));
				}
				id++;
			}
			dw.close();
		}
		getPRVisual(tuplegolds, tuplepreds, tuple2visual, output_prefix);
	}

	public static void getPRVisual(
			HashBasedTable<String, String, Boolean> golds,
			HashBasedTable<String, String, Double> preds,
			HashMap<String, String[]> object2visual,
			String output_dir) {

		if (!new File(output_dir).exists()) {
			new File(output_dir).mkdirs();
		} else {
			new File(output_dir).deleteOnExit();
		}
		DW dwreport = new DW(output_dir + "/report");
		List<Set<String>> goldLabels = new ArrayList<Set<String>>();
		List<Counter<String>> predictLabels = new ArrayList<Counter<String>>();
		for (String instance : golds.rowKeySet()) {
			Map<String, Boolean> goldOfInstance = golds.row(instance);
			Set<String> goldLabel = new HashSet<String>();
			Counter<String> predLabel = new ClassicCounter<String>();
			for (Entry<String, Boolean> e : goldOfInstance.entrySet()) {
				String rel = e.getKey();
				boolean goldanswer = e.getValue();
				double predanswer = 0;
				if (preds.contains(instance, rel)) {
					predanswer = preds.get(instance, rel);
				}
				// get one test!
				if (goldanswer) {
					goldLabel.add(rel);
				}
				if (predanswer > 0)
					predLabel.incrementCount(rel, predanswer);
			}
			goldLabels.add(goldLabel);
			predictLabels.add(predLabel);
			{
				// just for debug
				// if (predLabel.size() > 0 && goldLabel.size() == 0) {
				// D.p("0",instance, predLabel.totalCount());
				// }else if(predLabel.size() > 0 && goldLabel.size() > 0){
				// D.p("1",instance, predLabel.totalCount());
				// }
			}
		}
		RelationExtractionPRCurve repr = new RelationExtractionPRCurve();
		repr.prcurve(goldLabels, predictLabels);

		dwreport.write("targetRelation",
				"numTruePositive",
				"precision",
				"recall",
				"f1",
				"bestf1",
				"averagePrecision");
		if (repr.prall.size() > 0) {
			double[] prf = repr.prall.get(repr.prall.size() - 1);
			dwreport.write("all",
					repr.numTruePositive(goldLabels),
					prf[0],
					prf[1],
					2 * prf[0] * prf[1] / (prf[0] + prf[1]),
					repr.bestF1All[2],
					repr.getAveragePrecision(repr.prall));
		}
		{
			DW dwpr = new DW(output_dir + "/all.pr");
			for (double[] pr : repr.prall) {
				dwpr.write(pr[0], pr[1]);
			}
			dwpr.close();
		}
		{
			List<String[]> reportForR = new ArrayList<String[]>();
			D.p("repr.relations.size()", repr.relations.size());
			// for (String r : repr.prcurveByrel.keySet()) {
			for (String r : repr.relations) {
				String rr = r.replaceAll("\\W+", "_");
				List<double[]> pr = repr.prcurveByrel.get(r);
				if (pr.size() > 0) {
					double[] prf = pr.get(pr.size() - 1);
					double f1 = 0;
					if (prf[0] + prf[1] > 0) {
						f1 = 2 * prf[0] * prf[1] / (prf[0] + prf[1]);
					}
					reportForR.add(DW.tow(r,
							repr.truePositiveByRel.getCount(r),
							prf[0],
							prf[1],
							f1,
							repr.bestF1ByRel.get(r)[2],
							repr.getAveragePrecision(repr.prcurveByrel.get(r))));

					// dwreport.write(rr,
					// repr.truePositiveByRel.getCount(r),
					// prf[0],
					// prf[1],
					// 2 * prf[0] * prf[1] / (prf[0] + prf[1]),
					// repr.bestF1ByRel.get(r)[2],
					// repr.getAveragePrecision(repr.prcurveByrel.get(r))
					// );
				} else {
					reportForR.add(DW.tow(r,
							repr.truePositiveByRel.getCount(r),
							0, 0, 0, 0, 0));
				}
				DW dwpr = new DW(output_dir + "/" + rr + ".pr");
				for (double[] pr0 : pr) {
					dwpr.write(pr0[0], pr0[1]);
				}
				dwpr.close();
			}
			double[] average = new double[reportForR.get(0).length];
			String[] averageStr = new String[reportForR.get(0).length];
			int numRel = 0;
			Collections.sort(reportForR, new Comparator<String[]>() {
				public int compare(String[] arg0, String[] arg1) {
					return arg0[0].compareTo(arg1[0]);
				}
			});
			for (String[] l : reportForR) {
				dwreport.write(l);
				for (int i = 1; i < l.length; i++) {
					average[i] += Double.parseDouble(l[i]);
				}
				numRel++;
			}
			averageStr[0] = "MacroAverage";
			for (int i = 1; i < average.length; i++) {
				averageStr[i] = average[i] / numRel + "";
			}
			dwreport.write(averageStr);
		}
		{
			List<String[]> tow = new ArrayList<String[]>();
			for (String instance : golds.rowKeySet()) {
				Map<String, Boolean> goldOfInstance = golds.row(instance);
				Set<String> goldLabel = new HashSet<String>();
				Counter<String> predLabel = new ClassicCounter<String>();
				for (Entry<String, Boolean> e : goldOfInstance.entrySet()) {
					String rel = e.getKey();
					boolean goldanswer = e.getValue();
					double predanswer = 0;
					if (preds.contains(instance, rel)) {
						predanswer = preds.get(instance, rel);
					}
					// get one test!
					if (goldanswer) {
						goldLabel.add(rel);
					}
					if (predanswer > 0)
						predLabel.incrementCount(rel, predanswer);
				}
				for (String x : goldLabel) {
					if (predLabel.getCount(x) == 0) {
						tow.add(DW.tow("miss", x, instance));
					}
				}
				for (String x : predLabel.keySet()) {
					if (!goldLabel.contains(x)) {
						tow.add(DW.tow("error", x, instance));
					}
				}
				for (String x : goldLabel) {
					if (predLabel.getCount(x) > 0) {
						tow.add(DW.tow("yes", x, instance));
					}
				}
			}
			Collections.sort(tow, new Comparator<String[]>() {
				public int compare(String[] arg0, String[] arg1) {
					return StringUtil.compareStrings(arg0, arg1, new int[] { 1, 0 });
				}
			});
			for (String[] w : tow) {
				String object = w[2];
				if (object2visual.containsKey(object)) {
					String[] v = object2visual.get(object);
					String[] ww = new String[w.length + v.length];
					System.arraycopy(w, 0, ww, 0, w.length);
					System.arraycopy(v, 0, ww, w.length, v.length);
					dwreport.write(ww);
				} else {
					dwreport.write(w);
				}
			}
		}
		dwreport.close();
	}

	public static void main(String[] args) {
		String input_tuples_test = args[0];
		String input_preds = args[1];
		String input_labeled = args[2];
		String output_prefix = args[3];
		evaluate(input_tuples_test, input_preds, input_labeled, output_prefix);
	}

}
