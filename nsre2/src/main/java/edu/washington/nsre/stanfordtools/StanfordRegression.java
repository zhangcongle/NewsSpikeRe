package edu.washington.nsre.stanfordtools;

import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.RVFDatum;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.washington.nsre.util.DW;

public class StanfordRegression {

	public StanfordRegression() {

	}

	LinearClassifier<String, String> classifier;

	public LinearClassifier trainBasic(
			List<List<String>> list_features, List<String> list_labels) {
		List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();
		for (int i = 0; i < list_features.size(); i++) {
			List<String> features = list_features.get(i);
			String label = list_labels.get(i);
			Datum<String, String> d = new BasicDatum<String, String>(features, label);
			trainingData.add(d);
		}
		// Build a classifier factory
		LinearClassifierFactory<String, String> factory = new LinearClassifierFactory<String, String>();
		// factory.setTol(tol);
		// factory.setSigma(1);
		// factory.setEpsilon(0.01);
		// factory.useQuasiNewton();
		factory.setVerbose(true);
		LinearClassifier<String, String> classifier = factory.trainClassifier(trainingData);
		// {
		// ArrayList<String> temp = new ArrayList<String>();
		// temp.add("NS=" + GREEN);
		// System.out.println(classifier.scoreOf(new BasicDatum<String,
		// String>(temp, BROKEN), BROKEN));
		// }

		this.classifier = classifier;
		return classifier;
	}

	public LinearClassifier trainRVF(List<HashMap<String, Double>> list_feature2values,
			List<String> list_labels) {
		List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();
		for (int i = 0; i < list_feature2values.size(); i++) {
			HashMap<String, Double> feature2values = list_feature2values.get(i);
			String label = list_labels.get(i);
			Datum<String, String> d = new RVFDatum(Counters.fromMap(feature2values), label);
			trainingData.add(d);
		}
		// Build a classifier factory
		LinearClassifierFactory<String, String> factory = new LinearClassifierFactory<String, String>();
		factory.setSigma(3);
		factory.setEpsilon(15);
		factory.useQuasiNewton();
		factory.setVerbose(true);
		LinearClassifier<String, String> classifier = factory.trainClassifier(trainingData);
		// {
		// ArrayList<String> temp = new ArrayList<String>();
		// temp.add("NS=" + GREEN);
		// System.out.println(classifier.scoreOf(new BasicDatum<String,
		// String>(temp, BROKEN), BROKEN));
		// }

		this.classifier = classifier;
		return classifier;
	}

	public Map<String, Double> scoreOf(List<String> features) {
		Datum<String, String> d = new BasicDatum<String, String>(features, "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		for (String label : c.keySet()) {
			label2score.put(label, c.getCount(label));
		}
		return label2score;
	}

	public Map<String, Double> scoreOf(HashMap<String, Double> ftrValues) {
		Datum<String, String> d = new RVFDatum(Counters.fromMap(ftrValues), "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		for (String label : c.keySet()) {
			label2score.put(label, c.getCount(label));
		}
		return label2score;
	}

	public double scoreOf(HashMap<String, Double> ftrValues, String label) {
		Datum<String, String> d = new RVFDatum(Counters.fromMap(ftrValues), "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		return c.getCount(label);
	}

	public double scoreOf(List<String> features, String label) {
		Datum<String, String> d = new BasicDatum<String, String>(features, "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		return c.getCount(label);
	}

	public Map<String, Double> scoreOf(String feature) {
		List<String> features = new ArrayList<String>();
		features.add(feature);
		Datum<String, String> d = new BasicDatum<String, String>(features, "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		for (String label : c.keySet()) {
			label2score.put(label, c.getCount(label));
		}
		return label2score;
	}

	public double scoreOf(String feature, String label) {
		List<String> features = new ArrayList<String>();
		features.add(feature);
		Datum<String, String> d = new BasicDatum<String, String>(features, "");
		HashMap<String, Double> label2score = new HashMap<String, Double>();
		Counter<String> c = classifier.scoresOf(d);
		return c.getCount(label);
	}

	public Map<String, Counter<String>> weightsAsMapOfCounters() {
		return classifier.weightsAsMapOfCounters();
	}

	public void dump(String output) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					output), "utf-8"));
			Map<String, Counter<String>> weight = classifier.weightsAsMapOfCounters();
			for (String label : weight.keySet()) {
				Counter<String> ftrweight = weight.get(label);
				for (String ftr : ftrweight.keySet()) {
					bw.write(label + "\t" + ftr + "\t" + ftrweight.getCount(ftr) + "\n");
				}
			}
			bw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveModel(String model) {
		LinearClassifier.writeClassifier(classifier, model);
	}

	public LinearClassifier loadModel(String model) {
		classifier = LinearClassifier.readClassifier(model);
		return classifier;
	}

	public static void main(String[] args) {

	}
}
