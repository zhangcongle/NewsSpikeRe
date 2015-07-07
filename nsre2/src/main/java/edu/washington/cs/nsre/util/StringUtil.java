package edu.washington.nsre.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class StringUtil {
	public static String string2stringkey(String str) {
		str = Stemmer.stem(str);
		return str.trim().replaceAll("\\s", "_").toLowerCase();
	}

	public static String string2stringkeystrict(String str) {
		return str.trim().replaceAll("\\s", "_").toLowerCase();
	}

	public static String removeParentheses(String a) {
		char[] ach = a.toCharArray();
		StringBuilder sb = new StringBuilder();
		int stackdepth = 0;
		for (int i = 0; i < ach.length; i++) {
			if (ach[i] == '(') {
				stackdepth++;
			} else if (ach[i] == ')' && stackdepth > 0) {
				stackdepth--;
			} else if (stackdepth == 0) {
				sb.append(ach[i]);
			}
		}
		return sb.toString();
	}

	public static String removeNonAscii(String raw, String torep) {
		return raw.replaceAll("[^\\x00-\\x7F]", "");
	}

	public static String removeBlanket(String a, char blanket1, char blanket2) {
		char[] ach = a.toCharArray();
		StringBuilder sb = new StringBuilder();
		int stackdepth = 0;
		for (int i = 0; i < ach.length; i++) {
			if (ach[i] == blanket1) {
				stackdepth++;
			} else if (ach[i] == blanket2 && stackdepth > 0) {
				stackdepth--;
				if (stackdepth == 0) {
					sb.append(" ");
				}
			} else if (stackdepth == 0) {
				if (ach[i] == ' ') {
					sb.append("_");
				} else {
					sb.append(ach[i]);
				}
			}
		}
		return sb.toString();
	}

	public static List<String> sortAndRemoveDuplicate(List<String> list) {
		HashSet<String> temp = new HashSet<String>();
		for (String a : list) {
			temp.add(a.toLowerCase());
		}
		ArrayList<String> result = new ArrayList<String>();
		result.addAll(temp);
		Collections.sort(result);
		// String previous = "";
		// Iterator<String> it = list.iterator();
		// while(it.hasNext()){
		// String cur = it.next();
		// if(cur.equals(previous)){
		// it.remove();
		// }else{
		// previous = cur;
		// }
		// }
		return result;
	}

	public static List<String> tokenize(String str, char[] stopChar) {
		List<String> result = new ArrayList<String>();
		HashSet<Character> stopCharSet = new HashSet<Character>();
		for (char a : stopChar)
			stopCharSet.add(a);
		char[] cs = str.toCharArray();
		int bufferStart = 0;
		for (int i = 0; i < cs.length; i++) {
			if (stopCharSet.contains(cs[i])) {
				addWord(result, bufferStart, i, str);
				bufferStart = i + 1;// give up this char because it is stop char
			} else if (Character.isUpperCase(cs[i])) {
				if (i > 0 && Character.isLowerCase(cs[i - 1])) {
					addWord(result, bufferStart, i, str);
					bufferStart = i;
				}
			} else if (Character.isLowerCase(cs[i])) {
				// if two previous characters are both Upper case, then shoot
				// the word
				if (i > 1 && Character.isUpperCase(cs[i - 1])
						&& Character.isUpperCase(cs[i - 2])) {
					addWord(result, bufferStart, i, str);
					bufferStart = i;
				}
			} else {
				// joke haha
			}
		}
		addWord(result, bufferStart, cs.length, str);
		return result;
	}

	public static List<String> tokenize(String str) {
		return tokenize(str, new char[] { ' ', '\t' });
	}

	private static void addWord(List<String> list, int start, int end,
			String str) {
		if (end > start)
			list.add(str.substring(start, end));
	}

	public static int numOfShareWords(List<String> sortedList1,
			List<String> sortedList2) {
		int i = 0, j = 0, num = 0;
		while (i < sortedList1.size() && j < sortedList2.size()) {
			String a = sortedList1.get(i);
			String b = sortedList2.get(j);
			int c = a.compareTo(b);
			if (c == 0) {
				num++;
				i++;
				j++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		return num;
	}

	/**
	 * Config: 0: toLowerCase; 1: removeStop; 2: stem
	 * */

	public static int numOfShareWords(String str1, String str2, boolean[] config) {
		List<String> l1 = tokenize(str1, new char[] { ' ', '_' });
		List<String> l2 = tokenize(str2, new char[] { ' ', '_' });
		return numOfShareWords(l1, l2, config);
	}

	public static List<String> getSharedWords(String str1, String str2,
			boolean[] config) {
		List<String> l1 = tokenize(str1, new char[] { ' ', '_' });
		List<String> l2 = tokenize(str2, new char[] { ' ', '_' });
		List<String> temp1 = new ArrayList<String>();
		List<String> temp2 = new ArrayList<String>();

		listconvert(l1, temp1, config);
		listconvert(l2, temp2, config);
		List<String> shared = new ArrayList<String>();
		int i = 0, j = 0, num = 0;
		while (i < temp1.size() && j < temp2.size()) {
			String a = temp1.get(i);
			String b = temp2.get(j);
			/** 0: to lower case */
			if (config[0]) {
				a = a.toLowerCase();
				b = b.toLowerCase();
			}
			int c = a.compareTo(b);
			if (c == 0) {
				num++;
				i++;
				j++;
				shared.add(a);
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		return shared;
	}

	/**
	 * Config: 0: toLowerCase; 1: removeStop; 2: stem
	 * */
	public static int numOfShareWords(List<String> wordlist1,
			List<String> wordlist2, boolean[] config) {
		List<String> temp1 = new ArrayList<String>();
		List<String> temp2 = new ArrayList<String>();

		listconvert(wordlist1, temp1, config);
		listconvert(wordlist2, temp2, config);

		// Collections.sort(temp1);
		// Collections.sort(temp2);

		int i = 0, j = 0, num = 0;
		while (i < temp1.size() && j < temp2.size()) {
			String a = temp1.get(i);
			String b = temp2.get(j);
			/** 0: to lower case */
			if (config[0]) {
				a = a.toLowerCase();
				b = b.toLowerCase();
			}
			int c = a.compareTo(b);
			if (c == 0) {
				num++;
				i++;
				j++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		return num;
	}

	private static void listconvert(List<String> original,
			List<String> converted, boolean[] config) {
		HashSet<String> temp = new HashSet();
		for (String w : original) {
			// remove stop words
			if (config[1]) {
				if (RemoveStopwords.isStop(w)) {
					continue;
				}
			}
			// convert to lower case
			if (config[0]) {
				w = w.toLowerCase();
			}
			// stem
			if (config[2]) {
				w = Stemmer.stem(w);
			}
			temp.add(w);
		}
		converted.addAll(temp);
		Collections.sort(converted);
	}

	public static int numOfShareWords(String str1, String str2, int[] par_return) {
		str1 = str1.toLowerCase();
		str2 = str2.toLowerCase();
		List<String> l1 = tokenize(str1, new char[] { ' ', '_' });
		List<String> l2 = tokenize(str2, new char[] { ' ', '_' });
		List<String> sorted_l1 = new ArrayList<String>();
		List<String> sorted_l2 = new ArrayList<String>();
		for (String a : l1)
			sorted_l1.add(a.toLowerCase());
		for (String a : l2)
			sorted_l2.add(a.toLowerCase());
		Collections.sort(sorted_l1);
		Collections.sort(sorted_l2);
		par_return[0] = l1.size();
		par_return[1] = l2.size();
		return numOfShareWords(sorted_l1, sorted_l2);
	}

	public static int numOfShareWords(String str1, String str2) {
		List<String> l1 = tokenize(str1, new char[] { ' ', '_' });
		List<String> l2 = tokenize(str2, new char[] { ' ', '_' });
		l1 = sortUniq(l1);
		l2 = sortUniq(l2);
		return numOfShareWords(l1, l2);
	}

	public static int numOfShareWords(String str1, String str2, char[] split) {
		List<String> l1 = tokenize(str1, split);
		List<String> l2 = tokenize(str2, split);
		l1 = sortUniq(l1);
		l2 = sortUniq(l2);
		return numOfShareWords(l1, l2);
	}

	public static double cosineSimilarity(String[] tkn0, String[] tkn1) {

		HashMap<String, int[]> map = new HashMap<String, int[]>();
		for (int i = 0; i < tkn0.length; i++) {
			String t = tkn0[i].toLowerCase();
			if (!map.containsKey(t)) {
				map.put(t, new int[2]);
			}
			map.get(t)[0]++;
			// if (!RemoveStopwords.isStop(t)) {
			//
			// }
		}
		for (int i = 0; i < tkn1.length; i++) {
			String t = tkn1[i].toLowerCase();
			if (!map.containsKey(t)) {
				map.put(t, new int[2]);
			}
			map.get(t)[1]++;
			// if (!RemoveStopwords.isStop(t)) {
			//
			// }
		}
		double dot = 0;
		double norma = 0;
		double normb = 0;
		for (Entry<String, int[]> e : map.entrySet()) {
			int[] v = e.getValue();
			dot += v[0] * v[1];
			norma += v[0] * v[0];
			normb += v[1] * v[1];
		}
		norma = Math.sqrt(norma);
		normb = Math.sqrt(normb);
		if (dot == 0) {
			return 0;
		} else {
			return dot / (norma * normb);
		}
	}

	public static List<String> sortUniq(List<String> list) {
		HashSet<String> tmp = new HashSet<String>();
		for (String a : list) {
			tmp.add(a);
		}
		List<String> result = new ArrayList<String>();
		result.addAll(tmp);
		Collections.sort(result);
		return result;
	}

	public static int numOfShareInteger(List<Integer> sortedList1,
			List<Integer> sortedList2) {
		int i = 0, j = 0, num = 0;
		while (i < sortedList1.size() && j < sortedList2.size()) {
			int a = sortedList1.get(i);
			int b = sortedList2.get(j);
			int c = a - b;
			if (c == 0) {
				num++;
				i++;
				j++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		return num;
	}

	/** The string contains none English letters */
	public static boolean doesContainLetter(String a) {
		char[] c = a.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] >= 'a' && c[i] <= 'z' || c[i] >= 'A' && c[i] <= 'Z') {
				return true;
			}
		}
		return false;
	}

	public static boolean isPunct(String a) {
		String b = a.replaceAll("\\p{Punct}", "");
		if (b.length() == 0 && a.length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/** The string contains none English letters */
	public static boolean containOnlyLetter(String a) {
		char[] c = a.toCharArray();
		int letternumber = 0;
		for (int i = 0; i < c.length; i++) {
			if (c[i] >= 'a' && c[i] <= 'z' || c[i] >= 'A' && c[i] <= 'Z'
					|| c[i] == ' ') {
				letternumber++;
			}
		}
		if (letternumber == c.length) {
			return true;
		} else {
			return false;
		}
	}

	/** The string contains none English letters */
	public static String replaceNonLetter(String a, char replaceChar) {
		char[] c = a.toCharArray();
		int letternumber = 0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < c.length; i++) {
			if (c[i] >= 'a' && c[i] <= 'z' || c[i] >= 'A' && c[i] <= 'Z'
					|| c[i] == ' ') {
				sb.append(c[i]);
			} else {
				sb.append(replaceChar);
			}
		}
		return sb.toString();
	}

	public static List<Integer> locateArgInTokens(String[] tokens,
			String[] argsplit) {
		List<Integer> position = new ArrayList<Integer>();
		// List<String> argsplit = StringUtil.tokenize(arg);
		for (int i = 0; i < tokens.length; i++) {
			boolean isStart = true;
			for (int j = 0; j < argsplit.length; j++) {
				if (i + j >= tokens.length
						|| !tokens[i + j].equals(argsplit[j])) {
					isStart = false;
					break;
				}
			}
			if (isStart) {
				position.add(i);
			}
		}
		return position;
	}

	public static String join(String[] tokens, String delimiter) {
		if (tokens == null) {
			return "null";
		}
		if (tokens.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tokens[0]);
		for (int i = 1; i < tokens.length; i++)
			sb.append(delimiter).append(tokens[i]);
		return sb.toString();
	}

	public static String join(String[] tokens, String delimiter, int start,
			int end) {
		if (tokens == null) {
			return "null";
		}
		if (tokens.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tokens[start]);
		for (int i = start + 1; i < end; i++)
			sb.append(delimiter).append(tokens[i]);
		return sb.toString();
	}

	public static String join(String delimiter, Object... tkns) {
		StringBuilder sb = new StringBuilder();
		for (Object t : tkns) {
			sb.append(t.toString()).append(delimiter);
		}
		return sb.toString();
	}

	public static String join(String[] tokens, String delimiter, int[] position) {
		if (tokens == null) {
			return "null";
		}
		if (tokens.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tokens[0]);
		position[0] = 0;
		for (int i = 1; i < tokens.length; i++) {
			position[i] = sb.length() + delimiter.length();
			sb.append(delimiter).append(tokens[i]);
		}
		return sb.toString();
	}

	public static String join(List<String> tokens, String delimiter) {
		if (tokens == null) {
			return "null";
		}
		if (tokens.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tokens.get(0));
		for (int i = 1; i < tokens.size(); i++)
			sb.append(delimiter).append(tokens.get(i));
		return sb.toString();
	}

	public static List<String> concat(String[]... arrays) {
		List<String> result = new ArrayList<String>();
		for (String[] p : arrays) {
			for (String x : p) {
				result.add(x);
			}
		}
		return result;
	}

	public static String uppercaseFirstLetter(String token) {
		char[] chs = token.toCharArray();
		chs[0] = Character.toUpperCase(chs[0]);
		String s = new String(chs);
		return s;
	}

	public static boolean isCapStartString(String a) {
		if (a.length() > 0) {
			char a0 = a.charAt(0);
			if (a0 <= 'Z' && a0 >= 'A') {
				return true;
			}
		}
		return false;
	}

	public static int compareStrings(String[] a, String[] b, int[] columns) {
		for (int i = 0; i < columns.length; i++) {
			int k = columns[i];
			int r = a[k].compareTo(b[k]);
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	public static int compareStrings(String[] a, String[] b, int[] columns,
			boolean[] isNumber) {
		for (int i = 0; i < columns.length; i++) {
			int k = columns[i];
			int r = a[k].compareTo(b[k]);
			if (isNumber[i]) {
				r = Double.compare(Double.parseDouble(b[k]),
						Double.parseDouble(a[k]));
			}
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	public static void main(String[] args) {
		D.p(cosineSimilarity(new String[] { "I", "am" }, new String[] { "I",
				"am" }));

	}
}
