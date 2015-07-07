package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import edu.washington.nsre.util.*;


public class GoogleRss {
	static HashMap<String, String> seeds = new HashMap<String, String>();
	static {
		seeds.put("top", "http://news.google.com/news?ned=us&topic=h&output=rss");
		seeds.put("world", "http://news.google.com/news?ned=us&topic=w&output=rss");
		seeds.put("us", "http://news.google.com/news?ned=us&topic=n&output=rss");
		seeds.put("business", "http://news.google.com/news?ned=us&topic=b&output=rss");
		seeds.put("tech", "http://news.google.com/news?pz=1&cf=all&ned=us&hl=en&topic=tc&output=rss");
		seeds.put("health", "https://news.google.com/news/feeds?ned=us&topic=m&output=rss");
		seeds.put("sports", "http://news.google.com/news?ned=us&topic=s&output=rss");
		seeds.put("entertain", "http://news.google.com/news?ned=us&topic=e&output=rss");
		seeds.put("science", "http://news.google.com/news?pz=1&cf=all&ned=us&hl=en&topic=snc&output=rss");
	}

	public static List<OneNews> parseGoogleNewsRss(String input_rss) {
		List<OneNews> ret = new ArrayList<OneNews>();
		try {
			Pattern p = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>");
			Pattern pcid = Pattern.compile("cluster=(\\d+)");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new File(input_rss));
			//			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
			NodeList nList = doc.getElementsByTagName("item");
			//			System.out.println("-----------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					NodeList titles = eElement.getElementsByTagName("title");
					NodeList links = eElement.getElementsByTagName("link");
					NodeList descriptions = eElement.getElementsByTagName("description");
					NodeList guid = eElement.getElementsByTagName("guid");
					NodeList pubDate = eElement.getElementsByTagName("pubDate");
					String title = null;
					Date date = new Date();
					long clusterId = -1;
					//					ParseRssCluster c = new ParseRssCluster();
					if (titles.getLength() > 0) {
						title = titles.item(0).getTextContent();
					}
					if (guid.getLength() > 0) {
						try {
							String guid_str = guid.item(0).getTextContent();
							Matcher m = pcid.matcher(guid_str);
							if (m.find()) {
								String clusterIdStr = m.group(1);
								clusterId = Long.parseLong(clusterIdStr);
							}
						} catch (Exception e) {

						}
					}
					if (pubDate.getLength() > 0) {
						try {
							String pubDateStr = pubDate.item(0).getTextContent();
							String[] ab = pubDateStr.split(" ");
							String s = ab[2] + " " + ab[1] + " " + ab[3];
							date = new SimpleDateFormat("MMMM d yyyy", Locale.ENGLISH).parse(s);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (clusterId > 0 && descriptions.getLength() > 0) {
						String desc_raw = descriptions.item(0).getTextContent();
						Matcher m = p.matcher(desc_raw);
						while (m.find()) {
							String relnews_title = StringEscapeUtils.unescapeHtml(m.group(2));
							String relnews_url_wg = m.group(1);
							String relnews_url = relnews_url_wg.split("url=")[1];
							relnews_title = relnews_title.replaceAll("<b>", "").replaceAll("</b>", "");
							//							String content = crawlNewsHtmlOutputText(relnews_url);
							if (!relnews_title.startsWith("<")) {
								OneNews on = new OneNews();
								on.date = date;
								on.url = relnews_url;
								on.title = relnews_title;
								on.source = "google_" + clusterId;
								ret.add(on);
							}
						}

					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error in parsing one google rss file");
		}
		return ret;
	}

	public static boolean crawlGoogleRss(String url_str, String output) {
		try {
			String rss = Util.readUrl(url_str);
			if (rss.length() > 0) {
				BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(output), "utf-8"));
				bw.write(rss);
				bw.close();
				Thread.sleep(10000);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	//	static String []seeds = new String[]{
	//		"http://news.google.com/news?ned=us&topic=h&output=rss",
	//		"http://news.google.com/news?ned=us&topic=w&output=rss",
	//		
	//	};

	public static void main(String[] args) {
		String root = args[0];
		D.p(root);
		String output1 = "googlerss";
		String output2 = "seeds";
		Gson gson = new Gson();
		while (true) {
			int numRss = 0;
			Date date = new Date();
			long timestamp = date.getTime();
			SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
			String today = dateformatYYYYMMDD.format(date);

			String outputdir = root + File.separator + output2 + File.separator + today;
			if (!new File(outputdir).exists()) {
				new File(outputdir).mkdirs();
			}
			HashSet<String> alreadytitle = new HashSet<String>();
			try {
				List<String> list_seed_files = new ArrayList<String>();
				Util.leafFiles(root + File.separator + output2, list_seed_files);
				for (String f : list_seed_files) {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
					String l;
					while ((l = br.readLine()) != null) {
						OneNews onenews = gson.fromJson(l, OneNews.class);
						alreadytitle.add(onenews.title);
					}
					br.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputdir
						+ File.separator + "google" + date.getTime()), "utf-8"));
				String leafdir_crawl = root + File.separator + output1 + File.separator + today;
				String output_rss = "";
				try {
					if (!new File(leafdir_crawl).exists()) {
						new File(leafdir_crawl).mkdirs();
					}
					for (Entry<String, String> s : seeds.entrySet()) {
						String category = s.getKey();
						String url = s.getValue();
						output_rss = leafdir_crawl + File.separator + category + "." + timestamp;
						boolean success = crawlGoogleRss(url, output_rss);
						if (success) {
							numRss++;
						}
						//parse rss
						List<OneNews> clusters = parseGoogleNewsRss(output_rss);
						System.err.println("Get " + clusters.size() + " clusters from: " + output_rss);
						for (OneNews c : clusters) {
							if (!alreadytitle.contains(c.title)) {
								bw.write(gson.toJson(c) + "\n");
								alreadytitle.add(c.title);
							}
						}
					}
				} catch (Exception e) {
					System.err.println("error in crawling google rss");
				}
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(3600 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("Number of New clusters: " + numRss);
		}
	}
}
