package edu.washington.nsre.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BingWordDefinition {
	public static List<String[]> oneword(String w, String dir) throws IOException {
		List<String[]> ret = new ArrayList<String[]>();
		// https://www.bing.com/search?q=define+beat&go=Submit&qs=n&form=QBLH&pq=define+beat&sc=8-11&sp=-1&sk=&cvid=c64594f562554edd93aa2fe3a075dc36
		String f = w.replace(" ", "_");
		if (!new File(dir + "/" + f).exists()) {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir
					+ "/" + f), "utf-8"));
			String url = "https://www.bing.com/search?q=define+" + w;
			String text = ReadURL.readUrl(url);
			bw.write(text);
			bw.close();
		}
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dir
					+ "/" + f)));
			StringBuilder sb = new StringBuilder();
			String l;
			while ((l = br.readLine()) != null)
				sb.append(l);
			ret = parseOneWord(w, sb.toString());
			br.close();
		}
		return ret;
	}

	public static List<String[]> parseOneWord2(String w, String crawl) {
		// synonyns
		// str.indexOf(ch)
		int x = 0;
		List<String[]> ret = new ArrayList<String[]>();
		while (x < crawl.length()) {
			int start = crawl.indexOf(
					"<span class=\"b_demoteText\" data-appLinkHookId=\"demoteText\">synonyms", x);
			if (start < 0)
				break;
			int end = crawl.indexOf("</span>", start);
			if (end < 0)
				break;
			String piece = crawl.substring(start, end);
			Pattern p = Pattern.compile("<a href=[^>]*>([^<]*)</a>");
			Matcher m = p.matcher(piece);
			while (m.find()) {
				String[] a = DW.tow("1", w, m.group(1));
				ret.add(a);
				// D.p(a);

			}
			// D.p(piece);
			x = end;
		}
		x = 0;
		while (x < crawl.length()) {
			int start = crawl
					.indexOf("antonyms: ",
							x);
			if (start < 0)
				break;
			int end = crawl.indexOf("</span>", start);
			if (end < 0)
				break;
			String piece = crawl.substring(start, end);
			Pattern p = Pattern.compile("<a href=[^>]*>([^<]*)</a>");
			Matcher m = p.matcher(piece);
			while (m.find()) {
				String[] a = DW.tow("-1", w, m.group(1));
				ret.add(a);
				// D.p(a);
			}
			// D.p(piece);
			x = end;
		}
		return ret;
	}

	public static List<String[]> parseOneWord(String w, String crawl) {
		// synonyns
		// str.indexOf(ch)
		int x = 0;
		List<String[]> ret = new ArrayList<String[]>();
		Pattern p = Pattern.compile("<a href=[^>]*>([^<]*)</a>");
		while (x < crawl.length()) {
			int start = crawl.indexOf(
					"synonyms: ", x);
			if (start < 0)
				break;
			int end = crawl.indexOf("</span>", start);
			if (end < 0)
				break;
			String piece = crawl.substring(start + "synonyms: ".length(), end);
			String[] splits = piece.split("&#183;");
			for (String one : splits) {
				if (one.contains("<")) {
					Matcher m = p.matcher(one);
					if (m.find()) {
						String[] a = DW.tow("1", w, m.group(1));
						ret.add(a);
						D.p(a);
					}
				} else {
					one = one.trim();
					if (one.length() > 0) {
						String[] a = DW.tow("1", w, one);
						ret.add(a);
						D.p(a);
					}
				}
			}
			x = end;
		}
		x = 0;
		while (x < crawl.length()) {
			int start = crawl.indexOf("antonyms: ", x);
			if (start < 0)
				break;
			int end = crawl.indexOf("</span>", start);
			if (end < 0)
				break;
			String piece = crawl.substring(start, end);
			String[] splits = piece.split("&#183;");
			for (String one : splits) {
				if (one.contains("<a href")) {
					Matcher m = p.matcher(one);
					if (m.find()) {
						String[] a = DW.tow("-1", w, m.group(1));
						ret.add(a);
						D.p(a);
					}
				} else {
					one = one.trim();
					one = one.replaceAll("</div>", "");
					if (one.length() > 0) {
						String[] a = DW.tow("-1", w, one);
						ret.add(a);
						D.p(a);
					}
				}
			}
			// D.p(piece);
			x = end;
		}
		return ret;
	}

	public static void main(String[] args) throws IOException {
		oneword("kill", "data/bing");
//		oneword("buy", ".");
//		oneword("acquisition", ".");
//		oneword("acquisitions", ".");
//		oneword("leave", ".");
//		oneword("return", ".");
//		oneword("accuse", ".");
	}
}

