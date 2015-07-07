package edu.washington.cs.nsre.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import edu.washington.nsre.util.D;
import net.billylieurance.azuresearch.AzureSearchNewsQuery;
import net.billylieurance.azuresearch.AzureSearchNewsResult;
import net.billylieurance.azuresearch.AzureSearchResultSet;
import net.billylieurance.azuresearch.AzureSearchWebQuery;
import net.billylieurance.azuresearch.AzureSearchWebResult;

public class BingAzureApi {
	
	public static List<String[]> fromNewstitle2Newslist(String newstitle) {
		List<String[]> ret = new ArrayList<String[]>();
		AzureSearchNewsQuery aq = new AzureSearchNewsQuery();
		aq.setAppid("POQ8S/XONvSFxnUFrKANXTadRVzDAMN2E2cnCXmPOSM=");// 50,000
																	// transactions
		// aq.setAppid("D4Makeg91kNY0ZuZ4SJBfNKQ69m/ay/VVwa+9NLTAIw=");//free
		// 5000 transactions
		aq.setQuery(newstitle);
		aq.doQuery();
		AzureSearchResultSet<AzureSearchNewsResult> ars = aq.getQueryResult();
		for (AzureSearchNewsResult nsr : ars) {
			ret.add(new String[] { nsr.getUrl(), nsr.getTitle(),
					nsr.getDescription() });
		}
		return ret;
	}

	public static List<String[]> fromNewstitle2Newslist2(String newstitle) {
		List<String[]> ret = new ArrayList<String[]>();
		AzureSearchNewsQuery aq = new AzureSearchNewsQuery();
		aq.setAppid("POQ8S/XONvSFxnUFrKANXTadRVzDAMN2E2cnCXmPOSM=");// 50,000
																	// transactions
		// aq.setAppid("D4Makeg91kNY0ZuZ4SJBfNKQ69m/ay/VVwa+9NLTAIw=");//free
		// 5000 transactions
		aq.setQuery(newstitle);
		aq.doQuery();
		AzureSearchResultSet<AzureSearchNewsResult> ars = aq.getQueryResult();
		for (AzureSearchNewsResult nsr : ars) {
			ret.add(new String[] { nsr.getUrl(), nsr.getTitle(),
					nsr.getDescription(), nsr.getDate() });
		}
		return ret;
	}
	
	public static List<String[]> fromQuery2Weblist(String query) {
		List<String[]> ret = new ArrayList<String[]>();
		AzureSearchWebQuery aq = new AzureSearchWebQuery();
		aq.setAppid("POQ8S/XONvSFxnUFrKANXTadRVzDAMN2E2cnCXmPOSM=");// 50,000
																	// transactions
		// aq.setAppid("D4Makeg91kNY0ZuZ4SJBfNKQ69m/ay/VVwa+9NLTAIw=");//free
		// 5000 transactions
		aq.setQuery(query);
		aq.doQuery();
		AzureSearchResultSet<AzureSearchWebResult> ars = aq.getQueryResult();

		for (AzureSearchWebResult nsr : ars) {
			ret.add(new String[] {
					nsr.getUrl(), nsr.getTitle(),
					nsr.getDescription() });
		}
		return ret;
	}

//	public static List<String[]> fromNewstitle2Newslist2(String newstitle) {
//		List<String[]> ret = new ArrayList<String[]>();
//		AzureSearchNewsQuery aq = new AzureSearchNewsQuery();
//		aq.setAppid("POQ8S/XONvSFxnUFrKANXTadRVzDAMN2E2cnCXmPOSM=");// 50,000
//																	// transactions
//		// aq.setAppid("D4Makeg91kNY0ZuZ4SJBfNKQ69m/ay/VVwa+9NLTAIw=");//free
//		// 5000 transactions
//		aq.setQuery(newstitle);
//		aq.doQuery();
//		AzureSearchResultSet<AzureSearchNewsResult> ars = aq.getQueryResult();
//		for (AzureSearchNewsResult nsr : ars) {
//			ret.add(new String[] { nsr.getUrl(), nsr.getTitle(),
//					nsr.getDescription(), nsr.getDate() });
//		}
//		return ret;
//	}
	public static void main(String[] args) {
		List<String[]> result = fromNewstitle2Newslist2("Obama");
		for (String[] a : result) {
			D.p(a);
		}
		List<String[]>results2 = fromQuery2Weblist("clash meaning");
		D.p(results2.size());
	}
}
