package edu.washington.nsre.util;

import java.util.List;

public class HtmlVisual {
	public List<String[]> table;

	public HtmlVisual(List<String[]> table) {
		this.table = table;
	}

	public static String table2htmltable(List<String[]> table) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table border=\"1\">\n");
		for (int i = 0; i < table.size(); i++) {
			String[] t = table.get(i);
			sb.append("<tr>\n");
			for (String t0 : t) {
				sb.append("<td>" + t0 + "</td>\n");
			}
			sb.append("</tr>\n");
		}
		return sb.toString();
	}

	public static void json2htmlStr(String attribute, String str,
			StringBuilder sb) {
		sb.append("<h3>" + attribute + "</h3>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<td>" + str + "</td>");
		sb.append("</tr>");
		sb.append("</table>");
	}

	public static void json2htmlStrArray(String attribute, List<String> list,
			StringBuilder sb) {
		sb.append("<h3>" + attribute + "</h3>");
		sb.append("<table border=\"1\">");
		for (int i = 0; i < list.size(); i++) {
			String t0 = list.get(i);
			sb.append("<tr>");
			sb.append("<td>" + i + "</td>");
			sb.append("<td>" + t0 + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
	}

	public static void json2htmlStrArray(String attribute, String[] list,
			StringBuilder sb) {
		sb.append("<h3>" + attribute + "</h3>");
		sb.append("<table border=\"1\">");
		for (int i = 0; i < list.length; i++) {
			String t0 = list[i];
			sb.append("<tr>");
			sb.append("<td>" + i + "</td>");
			sb.append("<td>" + t0 + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
	}

	public static void json2htmlStrTable(String attribute, List<String[]> list,
			StringBuilder sb) {
		sb.append("<h3>" + attribute + "</h3>");
		sb.append("<table border=\"1\">");
		for (int i = 0; i < list.size(); i++) {
			String[] t0 = list.get(i);
			String tt = StringUtil.join(t0, " | ");
			sb.append("<tr>");
			sb.append("<td>" + (i + 1) + "</td>");
			sb.append("<td>" + tt + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("\n");
	}

	public static void json2htmlStrTable(String attribute, List<String[]> list,
			String output) {
		StringBuilder sb = new StringBuilder();
		json2htmlStrTable(attribute, list, sb);
		DW dw = new DW(output);
		dw.write(sb.toString());
		dw.close();
	}

	public static void printPR(String attribute, int[] pr, StringBuilder sb) {
		int truepos = pr[0];
		int falsepos = pr[1];
		int falseneg = pr[2];
		double precision = truepos * 1.0 / (truepos + falsepos);
		double recall = truepos * 1.0 / (truepos + falseneg);
		double f1 = 2 * precision * recall / (precision + recall);

		String[] w = DW.tow(f1, precision, recall);
		HtmlVisual.json2htmlStrArray(attribute, w, sb);
	}
}
