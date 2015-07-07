package edu.washington.nsre.stanfordtools;

import java.util.ArrayList;
import java.util.List;

public class CorefResult {
	public List<String> names = new ArrayList<String>();
	public List<int[]> chain = new ArrayList<int[]>(); //new int[3]: [0]: sentenceId, [1]: start; [2]:end
}
