package edu.washington.nsre.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UtilMath {
	public static double variance(double[] array) {
		// double sum = 0;
		// for (int i = 0; i < array.length; i++)
		// sum += array[i];
		double[] temp = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			temp[i] = array[i];
		}
		double avg = 1.0 / array.length;
		double var = 0;
		for (int i = 0; i < temp.length; i++) {
			var += (temp[i] - avg) * (temp[i] - avg);
		}
		var = var / temp.length;
		return var;
	}

	public static double norm_variance(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		double[] temp = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			temp[i] = array[i] / sum;
		}
		double avg = 1.0 / array.length;
		double var = 0;
		for (int i = 0; i < temp.length; i++) {
			var += (temp[i] - avg) * (temp[i] - avg);
		}
		return var;
	}

	public static double log_variance(double[] array) {
		double sum = 0;
		// for (int i = 0; i < array.length; i++)
		// sum += array[i];
		double[] temp = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			temp[i] = Math.log(array[i] + 1);
		}
		double avg = 1.0 / array.length;
		double var = 0;
		for (int i = 0; i < temp.length; i++) {
			var += (temp[i] - avg) * (temp[i] - avg);
		}
		var = var / temp.length;
		return var;
	}

	public static double sumOfArray(double[] array) {
		double sum = 0;
		for (double x : array) {
			sum += x;
		}
		return sum;
	}

	public static double avgOfArray(double[] array) {
		double sum = 0;
		for (double x : array) {
			sum += x;
		}
		if (sum == 0)
			return 0;
		else
			return sum / array.length;
	}

	public static int modpos(int x, int y) {
		int result = x % y;
		if (result < 0) {
			result += y;
		}
		return result;
	}

	// public static int modpos2(int x, int y) {
	// int result = x % y;
	// if (result < 0) {
	// result += y;
	// }
	// return result;
	// }
	static Date dateZero = new Date();
	static {
		try {
			dateZero = (new SimpleDateFormat("yyyyMMdd")).parse("20121101");
		} catch (Exception e) {

		}
	}

	public static int getNormDate(Date date) {
		long interval = date.getTime() - dateZero.getTime();
		int interval_days = (int) (interval / 3600 / 24 / 1000);
		return interval_days;
	}

	static Set<String> setstopverbs = new HashSet<String>();
	static {
		String[] stopverbs = new String[] { "be", "say", "have", "go", "call",
				"get", "tell", "do", "want", "announce", "rise", "fall" };
		for (String b : stopverbs)
			setstopverbs.add(b);
	}

	public static boolean isStopVerb(String v) {
		v = v.toLowerCase();
		if (setstopverbs.contains(v))
			return true;
		else
			return false;
	}

	static Set<String> set_negativewords = new HashSet<String>();
	static {
		String[] negs = new String[] { "abysmal", "adverse", "alarming",
				"angry", "annoy", "anxious", "apathy", "appalling",
				"atrocious", "awful", "B", "bad", "banal", "barbed",
				"belligerent", "bemoan", "beneath", "boring", "broken", "C",
				"callous", "can't", "clumsy", "coarse", "cold", "cold-hearted",
				"collapse", "confused", "contradictory", "contrary",
				"corrosive", "corrupt", "crazy", "creepy", "criminal", "cruel",
				"cry", "cutting", "D", "dead", "decaying", "damage",
				"damaging", "dastardly", "deplorable", "depressed", "deprived",
				"deformed", "D Cont.", "deny", "despicable", "detrimental",
				"dirty", "disease", "disgusting", "disheveled", "dishonest",
				"dishonorable", "dismal", "distress", "don't", "dreadful",
				"dreary", "E", "enraged", "eroding", "evil", "F", "fail",
				"faulty", "fear", "feeble", "fight", "filthy", "foul",
				"frighten", "frightful", "G", "gawky", "ghastly", "grave",
				"greed", "grim", "grimace", "gross", "grotesque", "gruesome",
				"guilty", "H", "haggard", "hard", "hard-hearted", "harmful",
				"hate", "hideous", "homely", "horrendous", "horrible",
				"hostile", "hurt", "hurtful", "I", "icky", "ignore",
				"ignorant", "ill", "immature", "imperfect", "impossible",
				"inane", "inelegant", "infernal", "injure", "injurious",
				"insane", "insidious", "insipid", "J", "jealous", "junky", "L",
				"lose", "lousy", "lumpy", "M", "malicious", "mean", "menacing",
				"messy", "misshapen", "missing", "misunderstood", "moan",
				"moldy", "monstrous", "N", "naive", "nasty", "naughty",
				"negate", "negative", "never", "no", "nobody", "nondescript",
				"nonsense", "not", "noxious", "O", "objectionable", "odious",
				"offensive", "old", "oppressive", "P", "pain", "perturb",
				"pessimistic", "petty", "plain", "poisonous", "poor",
				"prejudice", "Q", "questionable", "quirky", "quit", "R",
				"reject", "renege", "repellant", "reptilian", "repulsive",
				"repugnant", "revenge", "revolting", "rocky", "rotten", "rude",
				"ruthless", "S", "sad", "savage", "scare", "scary", "scream",
				"severe", "shoddy", "shocking", "sick", "sickening",
				"sinister", "slimy", "smelly", "sobbing", "sorry", "spiteful",
				"sticky", "stinky", "stormy", "stressful", "stuck", "stupid",
				"substandard", "suspect", "suspicious", "T", "tense",
				"terrible", "terrifying", "threatening", "U", "ugly",
				"undermine", "unfair", "unfavorable", "unhappy", "unhealthy",
				"unjust", "unlucky", "unpleasant", "upset", "unsatisfactory",
				"unsightly", "untoward", "unwanted", "unwelcome",
				"unwholesome", "unwieldy", "unwise", "upset", "V", "vice",
				"vicious", "vile", "villainous", "vindictive", "W", "wary",
				"weary", "wicked", "woeful", "worthless", "wound", "Y", "yell",
				"yucky", "Z", "zero" };
		for (String b : negs) {
			if (b.length() > 1) {
				set_negativewords.add(b);
			}
		}
	}

	public static boolean isNegativeWord(String v) {
		RemoveStopwords.isStop(v);
		v = v.toLowerCase();
		if (set_negativewords.contains(v)) {
			return true;
		} else {
			return false;
		}
	}

	public static void getSubsetForLabel(List<String[]> all, String outputhtml,
			int K) {
		List<String[]> temp = new ArrayList<String[]>();
		for (int i = 0; i < all.size() && i < K; i++) {
			temp.add(all.get(i));
		}
		DW dw = new DW(outputhtml + ".txt");
		for (int i = 0; i < temp.size(); i++) {
			String[] l = temp.get(i);
			String[] w = new String[l.length + 2];
			w[0] = "N";
			w[1] = "N";
			System.arraycopy(l, 0, w, 2, l.length);
			dw.write(w);
		}
		dw.close();
		HtmlVisual.json2htmlStrTable(K + "", temp, outputhtml);
	}

	public static String getDateStr(Date date) {
		SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
		String today = dateformatYYYYMMDD.format(date);
		return today;
	}

	public static int getDay(Date date, HashMap<String, Integer> date2day) {
		SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
		String today = dateformatYYYYMMDD.format(date);
		int day = date2day.get(today);
		return day;
	}

	public static String tenseOfVerbPhrase(String[] lmma, String[] pos,
			int start, int end) {
		/**
		 * VBD - Verb, past tense VBG - Verb, gerund or present participle VBN -
		 * Verb, past participle VBP - Verb, non-3rd person singular present VBZ
		 * - Verb, 3rd person singular present
		 */
		boolean isPast = false, isPresent = false, isMD = false, isHave = true;

		for (int i = start; i < end; i++) {
			// if (tkn[i].equals("have") || tkn[i].equals("has") ||
			// tkn[i].equals("had")) {
			// isHave = true;
			// } else
			if (pos[i].equals("VBZ") || pos[i].equals("VBP")) {
				isPresent = true;
			} else if (pos[i].equals("VBD")) {
				isPast = true;
			} else if (pos[i].equals("MD")) {
				isMD = true;
			}
		}
		// String s = StringUtil.join(tkn, " ", start, end);
		// if (s.equals("remains in")) {
		// D.p(StringUtil.join(tkn, " ", start, end), isPast, isPresent, isMD);
		// }
		// if (isHave) {
		// return "HAVE";
		// } else
		if (isMD) {
			return "MD";
		} else if (isPast) {
			return "PAST";
		} else if (isPresent) {
			return "PRESENT";
		} else {
			return "NA";
		}
	}

	public static String pairAB(String a, String b) {
		if (a.compareTo(b) > 0) {
			return a + "::" + b;
		} else {
			return b + "::" + a;
		}
	}

	public static void main(String[] args) {
		// D.p(-5 % 20);
		// double[] array = new double[] { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,
		// 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		// 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		// System.out.println(norm_variance(array));
		getNormDate(new Date());
	}

}
