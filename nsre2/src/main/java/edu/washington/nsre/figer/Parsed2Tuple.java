package edu.washington.nsre.figer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gson.Gson;

import edu.washington.nsre.extraction.Tuple;
import edu.washington.nsre.util.DW;
import edu.washington.nsre.util.StringUtil;

public class Parsed2Tuple {

	public static void main(String[] args) {
		Gson gson = new Gson();
		String input = args[0];
		String output = args[1];
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(input), "utf-8"));
			DW dw = new DW(output);
			String l;
			while ((l = br.readLine()) != null) {
				FigerParsedSentence s = gson.fromJson(l, FigerParsedSentence.class);
				List<Tuple> tuples = getReverbTuples(s);
				for (Tuple t : tuples) {
					dw.write(t.getArg1()+"@"+t.getArg2()+"@20000101", t.getPatternListStr(), gson.toJson(t));
				}
				// bw.write(gson.toJson(s) + "\n");
			}
			br.close();
			dw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String mapSner2Fner(String ner) {
		if (ner.equals("PERSON")) {
			return "/person";
		} else if (ner.equals("ORGANIZATION")) {
			return "/organization";
		} else if (ner.equals("LOCATION")) {
			return "/location";
		} else if (ner.equals("TIME")) {
			return "/time";
		} else if (ner.equals("MONEY")) {
			return "/money";
		} else if (ner.equals("DATE")) {
			return "/time";
		} else if (ner.equals("NUMBER")) {
			return "/number";
		} else {
			return "/" + ner.toLowerCase();
		}
	}

	public static List<Tuple> getReverbTuples(FigerParsedSentence s) {
		List<Tuple> tuples = new ArrayList<Tuple>();
		List<int[]> argStartEnds = new ArrayList<int[]>();
		HashSet<String> appeared = new HashSet<String>();
		HashMap<String, OneReverb> rmap = new HashMap<String, OneReverb>();
		HashMap<Integer, OneNer> fnermap = new HashMap<Integer, OneNer>();
		for (OneNer on : s.blockNers) {
			if (on.flabel.length() > 0) {
				for (int i = on.start; i < on.end; i++) {
					fnermap.put(i, on);
				}
			}
		}
		for (OneReverb or : s.reverbs) {
			argStartEnds.add(new int[] { or.a1start, or.a1end, or.a2start, or.a2end });
			String key = or.a1start + "\t" + or.a1end + "\t" + or.a2start + "\t" + or.a2end;
			appeared.add(key);
			rmap.put(key, or);
		}
		for (int i = 0; i < s.blockNers.size(); i++) {
			OneNer n1 = s.blockNers.get(i);
			for (int j = 0; j < s.blockNers.size(); j++) {
				OneNer n2 = s.blockNers.get(j);
				if (n1.start >= n2.start)
					continue;
				String key = n1.start + "\t" + n1.end + "\t" + n2.start + "\t" + n2.end;
				if (!appeared.contains(key)) {
					argStartEnds.add(new int[] { n1.start, n1.end, n2.start, n2.end });
				}
			}
		}
		for (int[] a : argStartEnds) {
			Tuple t = new Tuple();
			t.articleId = s.sectId;
			t.tkn = s.tkn;
			t.pos = s.pos;
			t.ner = s.ner;
			t.lmma = s.lmma;
			t.deps = s.deps;
			String key = a[0] + "\t" + a[1] + "\t" + a[2] + "\t" + a[3];
			t.a1 = new int[] { a[0], a[1], 0 };
			t.a2 = new int[] { a[2], a[3], 0 };
			t.setArg1Head();
			t.setArg2Head();
			t.a1str = StringUtil.join(t.tkn, " ", t.a1[0], t.a1[1]);
			t.a2str = StringUtil.join(t.tkn, " ", t.a2[0], t.a2[1]);
			if (rmap.containsKey(key)) {
				OneReverb or = rmap.get(key);
				t.v = new int[] { or.vstart, or.vend, or.vhead };
				t.setRelHead();
				t.relstr = t.getLmmaRel();
			}
			t.getShortestPathFromTuple();
			t.getSubtree();
			t.getArg1Ner();
			t.getArg2Ner();
			t.getPatternHead();
			t.fner1.put(mapSner2Fner(t.getArg1Ner()), 1.0);
			t.fner2.put(mapSner2Fner(t.getArg2Ner()), 1.0);
			if (fnermap.containsKey(t.a1[2])) {
				OneNer on = fnermap.get(t.a1[2]);
				String flabel = on.flabel;
				t.setArg1FineGrainedNer(flabel);
			}
			if (fnermap.containsKey(t.a2[2])) {
				OneNer on = fnermap.get(t.a2[2]);
				String flabel = on.flabel;
				t.setArg2FineGrainedNer(flabel);

			}
			if (t.getPatternList().size() > 0) {
				tuples.add(t);
			}
		}
		return tuples;
	}
}
