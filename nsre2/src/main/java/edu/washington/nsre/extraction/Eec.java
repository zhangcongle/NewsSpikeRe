package edu.washington.nsre.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.HashMultimap;

public class Eec  {
	String eecname;
	List<Tuple> tuples = new ArrayList<Tuple>();

	public Eec(String eecname) {
		this.eecname = eecname;
	}
}
