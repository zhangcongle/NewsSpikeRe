package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import edu.washington.nsre.util.*;


public class Util {

	public static void leafFiles(String dir, List<String> files) {
		File root = new File(dir);
		if (root.exists()) {
			File[] list = root.listFiles();

			for (File f : list) {
				if (f.isDirectory()) {
					leafFiles(f.getAbsolutePath(), files);
					// System.out.println("Dir:" + f.getAbsoluteFile());
				} else {
					files.add(f.getAbsolutePath());
					// System.out.println("File:" + f.getAbsoluteFile());
				}
			}
		}
	}

	public static String readUrl(String url_str) {
		String text = "";
		try {
			URL url = new URL(url_str);
			URLConnection hc = url.openConnection();
			hc.setConnectTimeout(10000);
			hc.setConnectTimeout(10000);
			hc.setReadTimeout(10000);
			hc.setAllowUserInteraction(false);
			hc.setDoOutput(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					hc.getInputStream(), "utf-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line + "\n");
			}
			text = sb.toString();
			System.err.print("Get page " + url_str + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}

	public static String cleanHtml(String html) {
		html = StringEscapeUtils.unescapeHtml(html);
		return html;
	}

	public static void main(String[] args) {
		String content = Util
				.readUrl("http://api.bing.com/rss.aspx?source=web&query=sushi+los%20angeles");
		D.p(content);
	}

}
