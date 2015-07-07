package edu.washington.nsre.extraction;
import java.util.ArrayList;
import java.util.List;
import edu.washington.nsre.util.*;

import com.google.common.collect.HashBasedTable;

public class ConnectedComponent {
	int ccid;
	EventType eventtype;
	EventPhrase phrase;
	List<Tuple> tuples = new ArrayList<Tuple>();
	Factor phraseFactor = new Factor();
	List<Factor> tupleFactors = new ArrayList<Factor>();

	HashBasedTable<Integer, Integer, Factor> crossFactors = HashBasedTable
			.create();

	public String phraseFactorToStr() {
		return Util.counter2str(phraseFactor.features);
	}

	public void addCrossFeature(int i, int j, String f) {
		if (!crossFactors.contains(i, j)) {
			crossFactors.put(i, j, new Factor());
		}
		crossFactors.get(i, j).add(f);
	}
}