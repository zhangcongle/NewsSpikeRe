package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.xml.sax.InputSource;
import edu.washington.nsre.util.*;

import com.google.gson.Gson;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

class BroilNews2 {
	public Date date;
	public String title;
	public String querytitle;
	public String url;
	public String desc;
	public String text;
	public String source;

	public BroilNews2(HtmlNews hn) {
		this.date = hn.date;
		this.title = hn.title;
		this.querytitle = hn.querytitle;
		this.url = hn.url;
		this.desc = hn.desc;
		this.source = hn.source;
	}
}

public class Broil {
	public static String normTitle(String title) {
		String ret = title;
		{
			int temp = ret.lastIndexOf("-");
			if (temp > 0) {
				ret = ret.substring(0, temp);
			}
		}
		{
			int temp = ret.indexOf(":");
			if (temp > 0) {
				String head = ret.substring(0, temp);
				if (!head.contains(" ")) {
					ret = ret.substring(temp + 1);
				}
			}
		}
		{
			int temp = ret.indexOf("http");
			if (temp > 0) {
				ret = ret.substring(0, temp);

			}
		}
		return ret;
	}

	public static void main(String[] args) {
		String root = args[0];
		boolean restart = false;
		if (args.length > 1) {
			restart = Boolean.parseBoolean(args[1]);
		}
		Gson gson = new Gson();
		String readfrom = "html";
		String outputto = "broil";
		{
			File f = new File(root + File.separator + outputto);
			if (!f.exists()) {
				f.mkdir();
			}
		}
		HashSet<String> crawledurl = new HashSet<String>();
		{
			SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat(
					"yyyyMMdd");
			String today = dateformatYYYYMMDD.format((new Date()));
			List<String> already_list = new ArrayList<String>();
			Util.leafFiles(root + File.separator + outputto + File.separator
					+ today, already_list);
			for (String f : already_list) {
				try {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(new FileInputStream(f),
									"utf-8"));
					String l;
					while ((l = br.readLine()) != null) {
						try {
							BroilNews bn = gson.fromJson(l, BroilNews.class);
							crawledurl.add(bn.url);
						} catch (Exception e) {

						}
					}
					br.close();
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}
		}
		System.err.println("broil url\t" + crawledurl.size());
		while (true) {
			int target = 0;
			try {
				System.err.println("run again!");
				Date date = new Date();
				SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat(
						"yyyyMMdd");
				String today = dateformatYYYYMMDD.format(date);
				String outputdir = root + File.separator + outputto
						+ File.separator + today;
				if (!new File(outputdir).exists()) {
					new File(outputdir).mkdirs();
				}
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputdir + File.separator
								+ date.getTime()), "utf-8"));
				List<String> inputs = new ArrayList<String>();
				Util.leafFiles(root + File.separator + readfrom, inputs);
				List<BingNews> buffer = new ArrayList<BingNews>();
				int count = 0;
				for (String f : inputs) {
					try {
						BufferedReader br = new BufferedReader(
								new InputStreamReader(new FileInputStream(f),
										"utf-8"));
						String l;

						while ((l = br.readLine()) != null) {
							try {
								HtmlNews on = gson.fromJson(l, HtmlNews.class);
								if (!crawledurl.contains(on.url)) {
									BroilNews bn = new BroilNews(on);
									crawledurl.add(on.url);
									String html = on.html;
									String text = "";
									try {
										InputStream is = new ByteArrayInputStream(
												html.getBytes("utf-8"));
										text = ArticleExtractor.INSTANCE
												.getText(new InputSource(is));
									} catch (Exception e) {
										e.printStackTrace();
									}
									bn.text = text;
									bw.write(gson.toJson(bn) + "\n");
									System.err.println(bn.title + "\t"
											+ (new Date()).toString() + "\t"
											+ count++);
									bw.flush();
								}
							} catch (Exception e) {
								// e.printStackTrace();
							}
						}
						br.close();
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
				bw.close();
			} catch (Exception e) {
				// e.printStackTrace();
			}
			try {
				System.err.println("start sleeping!\t" + target);
				Thread.sleep(3600 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
