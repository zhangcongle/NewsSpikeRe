package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

class HtmlNews2 {
	Date date;
	String title;
	String querytitle;
	String url;
	String desc;
	String html;
	String source;

	public HtmlNews2(BingNews bn) {
		this.date = bn.date;
		this.title = bn.title;
		this.querytitle = bn.querytitle;
		this.url = bn.url;
		this.desc = bn.desc;
		this.source = bn.source;
	}
}

public class CrawlHtml {
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
		String readfrom = "bing";
		String outputto = "html";
		String index = "index_html";
		{
			File f = new File(root + File.separator + outputto);
			if (!f.exists()) {
				f.mkdir();
			}
		}
		HashSet<String> crawledurl = new HashSet<String>();
		{
			List<String> already_list = new ArrayList<String>();
			Util.leafFiles(root + File.separator + index, already_list);
			for (String f : already_list) {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
					String l;
					while ((l = br.readLine()) != null) {
						try {
							crawledurl.add(l);
						} catch (Exception e) {

						}
					}
					br.close();
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
		}
		System.err.println("crawledurl\t" + crawledurl.size());
		while (true) {
			int target = 0;
			try {
				System.err.println("run again!");
				Date date = new Date();
				SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
				String today = dateformatYYYYMMDD.format(date);
				String outputdir = root + File.separator + outputto + File.separator + today;
				String output_index_dir = root + File.separator + index + File.separator + today;
				if (!new File(outputdir).exists()) {
					new File(outputdir).mkdirs();
				}
				if (!new File(output_index_dir).exists()) {
					new File(output_index_dir).mkdirs();
				}

				List<String> inputs = new ArrayList<String>();
				Util.leafFiles(root + File.separator + readfrom, inputs);
				List<BingNews> buffer = new ArrayList<BingNews>();
				for (String f : inputs) {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
						String l;
						while ((l = br.readLine()) != null) {
							try {
								BingNews on = gson.fromJson(l, BingNews.class);
								if (!crawledurl.contains(on.url)) {
									buffer.add(on);
									crawledurl.add(on.url);
									target++;
								}
							} catch (Exception e) {
								//								e.printStackTrace();
							}
						}
						br.close();
					} catch (Exception e) {
						//						e.printStackTrace();
					}
				}
				date = new Date();
				System.err.println("buffer size:" + buffer.size());
				BufferedWriter bw = null;
				BufferedWriter bw_index = null;
				for (int i = 0; i < buffer.size(); i++) {
					if (i % 10000 == 0) {
						if (bw != null) {
							bw.close();
							bw_index.close();
						}
						bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputdir
									+ File.separator + date.getTime()), "utf-8"));
						bw_index = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
									output_index_dir + File.separator + date.getTime()), "utf-8"));
					}
					try {
						BingNews on = buffer.get(i);
						System.err.println(on.url + "\t" + on.title + "\t" + (new Date()).toString() + "\t" + i
									+ "\t"
									+ target);
						HtmlNews hn = new HtmlNews(on);
						//						BroilNews broilnews = new BroilNews(on);
						try {
							hn.html = Util.readUrl(hn.url);
						} catch (Exception e) {
							hn.html = "";
							//							e.printStackTrace();
						}
						bw.write(gson.toJson(hn) + "\n");
						bw_index.write(hn.url + "\n");
					} catch (Exception e) {
						//						e.printStackTrace();
					}
				}
				if (bw != null) {
					bw.close();
					bw_index.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
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
