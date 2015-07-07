package edu.washington.nsre.crawl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.washington.nsre.stanfordtools.*;

public class CorenlpNews {

	public Date date;
	public String title;
	public String querytitle;
	public String url;
	public String desc;
	public String source;
	public String text;

	public long articleId;

	public CorenlpParsedArticle parsedarticle;
	public List<String[]> reverbNps;
	public List<String[]> reverbVps;
	public List<String[]> reverbExts;

	public CorenlpNews(BroilNews bn) {
		this.date = bn.date;
		this.title = bn.title;
		this.querytitle = bn.querytitle;
		this.url = bn.url;
		this.desc = bn.desc;
		this.text = bn.text;
		this.source = bn.source;

		reverbNps = new ArrayList<String[]>();
		reverbVps = new ArrayList<String[]>();
		reverbExts = new ArrayList<String[]>();
	}

	public CorenlpNews() {
		reverbNps = new ArrayList<String[]>();
		reverbVps = new ArrayList<String[]>();
		reverbExts = new ArrayList<String[]>();
	}
}