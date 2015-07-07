package edu.washington.nsre.extraction;

import java.util.ArrayList;
import java.util.List;

public class ConnectedComponentTrain extends ConnectedComponent {
	int phraseTruth;
	int phrasePred;

	List<Integer> tupleTruths = new ArrayList<Integer>();
	List<Integer> tuplePreds = new ArrayList<Integer>();

	public ConnectedComponentTrain(ConnectedComponent cc) {
		this.ccid = cc.ccid;
		this.eventtype = cc.eventtype;
		this.phrase = cc.phrase;
		this.phraseFactor = cc.phraseFactor;
	}
}