package edu.washington.nsre.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import edu.washington.nsre.util.*;


class OneNews {
	Date date;
	String title;
	String source;
	String url;
}

class UserNewsTweets {
	long id;
	String screen_name;
}

class OneNewsTweets {
	String created_at;
	String text;
	OneNewsTweets retweeted_status;
	UserNewsTweets user;
	long id;
}

public class ParseTweets {
	public static void main2(String[] args) throws IOException {
		Gson gson = new Gson();
		String input = "o:/unix/projects/pardosa/data03/clzhang/tweetnews/nytimes";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.length() > 0) {
				OneNewsTweets ont = gson.fromJson(line, OneNewsTweets.class);
				if (ont.retweeted_status != null) {
					D.p(ont.retweeted_status.user.screen_name, ont.retweeted_status.text);
				}
			}
		}
		br.close();
	}

	static Set<String> newsagencies = new HashSet<String>();
	static {
		String[] temp = new String[] { "WSJ", "washingtonpost", "nytimes", "latimes", "USATODAY" };
		for (String t : temp)
			newsagencies.add(t);
	}

	static Date createat2date(String create_at) {
		Date date = new Date();
		try {
			String[] ab = create_at.split(" ");
			String s = ab[1] + " " + ab[2] + " " + ab[5];
			date = new SimpleDateFormat("MMMM d yyyy", Locale.ENGLISH).parse(s);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date;

	}

	public static void main(String[] args) throws IOException {
		String root = args[0];
		boolean restart = false;
		if (args.length > 1) {
			restart = Boolean.parseBoolean(args[1]);
		}
		Gson gson = new Gson();
		String readfrom = "crawltweets";
		String outputto = "seeds";
		{
			File f = new File(root + File.separator + outputto);
			if (!f.exists()) {
				f.mkdir();
			}
		}
		HashSet<String> alreadytitle = new HashSet<String>();
		{
			List<String> list_seed_files = new ArrayList<String>();
			Util.leafFiles(root + File.separator + outputto, list_seed_files);
			for (String f : list_seed_files) {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String l;
				while ((l = br.readLine()) != null) {
					OneNews onenews = gson.fromJson(l, OneNews.class);
					alreadytitle.add(onenews.title);
				}
				br.close();
			}
		}
		while (true) {
			Date date = new Date();
			SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyyMMdd");
			String today = dateformatYYYYMMDD.format(date);
			List<String> files = new ArrayList<String>();
			Util.leafFiles(root + File.separator + readfrom + File.separator + today, files);
			String outputdir = root + File.separator + outputto + File.separator + today;
			if (!new File(outputdir).exists()) {
				new File(outputdir).mkdirs();
			}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputdir
					+ File.separator + date.getTime())));
			for (String f : files) {
				try {
					OneNews onenews = new OneNews();
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
					String line = br.readLine();
					if (line != null && line.length() > 0) {
						OneNewsTweets ont = gson.fromJson(line, OneNewsTweets.class);
						if (newsagencies.contains(ont.user.screen_name)) {
							onenews.title = ont.text;
							onenews.source = ont.user.screen_name;
							onenews.date = createat2date(ont.created_at);
						} else if (ont.retweeted_status != null) {
							if (newsagencies.contains(ont.retweeted_status.user.screen_name)) {
								onenews.title = ont.retweeted_status.text;
								onenews.source = ont.retweeted_status.user.screen_name;
								onenews.date = createat2date(ont.retweeted_status.created_at);
							}
						}
					}
					if (onenews.title != null && !onenews.title.toLowerCase().startsWith("rt ")) {
						String url = "";
						String[] ab = onenews.title.split(" ");
						for (String x : ab) {
							if (x.startsWith("http")) {
								url = x;
							}
						}
						onenews.url = url;
						if (!alreadytitle.contains(onenews.title)) {
							bw.write(gson.toJson(onenews) + "\n");
							alreadytitle.add(onenews.title);
						}
					}
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			bw.close();
			D.p("parsetweets", alreadytitle.size());
			try {
				Thread.sleep(3600 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
