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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import edu.washington.nsre.util.*;

import com.google.gson.Gson;

class BingNews {
	Date date;
	String title;
	String querytitle;
	String url;
	String source;
	String desc;
}

public class BingSeed {
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
		String readfrom = "seeds";
		String outputto = "bing";
		{
			File f = new File(root + File.separator + outputto);
			if (!f.exists()) {
				f.mkdir();
			}
		}
		HashSet<String> searchedtitles = new HashSet<String>();
		{
			List<String> already_list = new ArrayList<String>();
			Util.leafFiles(root + File.separator + outputto, already_list);
			for (String f : already_list) {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
					String l;
					while ((l = br.readLine()) != null) {
						try {
							BingNews bn = gson.fromJson(l, BingNews.class);
							searchedtitles.add(bn.querytitle);
						} catch (Exception e) {
//							e.printStackTrace();
						}
					}
					br.close();
				} catch (Exception e) {
//					e.printStackTrace();
				}
			}
		}
		System.err.println("searched title\t" + searchedtitles.size());
		while (true) {
			try {
				Date date = new Date();
				SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
				String today = dateformatYYYYMMDD.format(date);
				String outputdir = root + File.separator + outputto + File.separator + today;
				if (!new File(outputdir).exists()) {
					new File(outputdir).mkdirs();
				}
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputdir
						+ File.separator + date.getTime()), "utf-8"));
				List<String> inputs = new ArrayList<String>();
				Util.leafFiles(root + File.separator + readfrom, inputs);
				for (String f : inputs) {
					try {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
						String l;
						while ((l = br.readLine()) != null) {
							OneNews on = gson.fromJson(l, OneNews.class);

							if (!searchedtitles.contains(on.title)) {
								{
									//first copy the original news
									BingNews bn = new BingNews();
									bn.date = on.date;
									bn.desc = "";
									bn.querytitle = on.title;
									bn.source = on.source;
									bn.title = on.title;
									bw.write(gson.toJson(bn) + "\n");
								}
								searchedtitles.add(on.title);
								System.err.println("BINGSEED " + (new Date()).toString() + "\t" + on.title);

								String query = normTitle(on.title);

								try {
									List<String[]> result = BingAzureApi.fromNewstitle2Newslist2(query);
									for (String[] r : result) {
										BingNews bn = new BingNews();
										bn.url = r[0];
										bn.title = r[1];
										bn.desc = r[2];
										String date0 = r[3].substring(0, 10);
										bn.date = new SimpleDateFormat("yyyy-MM-dd").parse(date0);
										bn.querytitle = on.title;
										bn.source = on.source;
										bw.write(gson.toJson(bn) + "\n");
										if (r[3].startsWith("2012")) {
											D.p("bingseed", r[3], bn.date);
										}
									}
								} catch (Exception e) {
//									e.printStackTrace();
								}
							}
						}
						br.close();
					} catch (Exception e) {
//						e.printStackTrace();
					}
				}
				bw.close();
			} catch (Exception e) {
//				e.printStackTrace();
			}
			try {
				Thread.sleep(3600 * 1000);
			} catch (Exception e) {
//				e.printStackTrace();
			}
		}
	}
}
