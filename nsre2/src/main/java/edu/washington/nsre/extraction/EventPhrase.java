package edu.washington.nsre.extraction;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;

public class EventPhrase {

	EventType eventtype;
	String str;
	String head;
	Set<String> words = new HashSet<String>();
	Set<String> extendedwords = new HashSet<String>();

	public EventPhrase(EventType eventtype, String str, String head) {
		this.eventtype = eventtype;
		this.str = str;
		this.head = head;
		String[] splits = str.split(" ");
		// D.p("event phrase!!!");
		for (int i = 0; i < splits.length; i++) {
			if (keywords.containsEntry(eventtype.str, splits[i])) {
				words.add(splits[i]);
			}
		}
		for (int i = 0; i < splits.length - 1; i++) {
			String w = splits[i] + " " + splits[i + 1];
			if (keywords.containsEntry(eventtype.str, w)) {
				words.add(w);
			}
		}
		for (int i = 0; i < splits.length; i++) {
			String w = splits[i];
			for (int e = Math.max(4, w.length() - 3); e < w.length(); e++) {
				String r = w.substring(0, e);
				if (keywordsroot2event2keywords.contains(r, eventtype.str)) {
					String extendedw = keywordsroot2event2keywords.get(r,
							eventtype.str);
					if (!words.contains(extendedw)) {
						extendedwords.add(extendedw);
					}
				}
			}
		}
	}

	public String toString() {
		return this.str;
	}

	static HashMultimap<String, String> keywords = HashMultimap.create();
	// static HashMultimap<String, String> keywordsroot2keywords = HashMultimap
	// .create();
	static HashBasedTable<String, String, String> keywordsroot2event2keywords = HashBasedTable
			.create();
}