package edu.washington.nsre.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

public class ReadURL {
	public static String readUrl(String url_str) {

		try {

			URL oracle = new URL(url_str);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					oracle.openStream(), "UTF-8"));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = in.readLine()) != null) {
				sb.append(line + "\n");
			}
			return sb.toString();
		} catch (Exception e) {
		}
		return "";
	}

	public static void main(String[] args) {
		String s = readUrl("https://www.bing.com/search?q=define+agree");
		System.out.println(s);
	}
}
