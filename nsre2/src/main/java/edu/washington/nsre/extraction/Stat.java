package edu.washington.nsre.extraction;

import java.util.HashMap;

import com.google.common.collect.HashMultimap;

public class Stat {
	HashMap<String, Eec> eecname2eec;
	HashMultimap<String, Tuple> phrase2tuples;
}
