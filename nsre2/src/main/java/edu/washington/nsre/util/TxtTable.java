package edu.washington.nsre.util;

import java.util.ArrayList;
import java.util.List;

public class TxtTable {

	int numColumns;
	int[] columnMaxWidth;

	int[] columnWidth;
	List<String[]> lines;

	public TxtTable(int numColumns, int[] columnMaxWidth, List<String[]> lines) {
		this.numColumns = numColumns;
		this.columnMaxWidth = columnMaxWidth;
		columnWidth = new int[numColumns];
		this.lines = lines;
		for (String[] l : lines) {
			for (int i = 0; i < numColumns; i++) {
				columnWidth[i] = Math.max(columnWidth[i], l[i].length() + 1);
			}
		}
		for (int i = 0; i < columnWidth.length; i++)
			columnWidth[i] = Math.min(columnWidth[i], columnMaxWidth[i]);
	}

	public String displayOneLine(String[] l) {
		StringBuilder sb = new StringBuilder();
		List<String[]> cells = new ArrayList<String[]>();
		int numLines = 1;
		for (int i = 0; i < numColumns; i++) {
			String[] cell_i = splitLongString(l[i], columnWidth[i]);
			cells.add(cell_i);
			numLines = Math.max(numLines, cell_i.length);
		}
		for (int i = 0; i < numLines; i++) {
			for (int j = 0; j < numColumns; j++) {
				sb.append("| ");
				String[] cell_j = cells.get(j);
				int occupied = 0;
				if (i < cell_j.length) {
					sb.append(cell_j[i]);
					occupied += cell_j[i].length();
				}
				for (int k = 0; k < columnWidth[j] - occupied; k++) {
					sb.append(' ');
				}
				sb.append(' ');
			}
			sb.append("|\r\n");
		}
		return sb.toString();
	}

	public String display() {
		StringBuilder sbSepLine = new StringBuilder();
		{
			for (int i = 0; i < numColumns; i++) {
				sbSepLine.append("+-");
				for (int k = 0; k < columnWidth[i]; k++) {
					sbSepLine.append('-');
				}
				sbSepLine.append('-');
			}
			sbSepLine.append("+\r\n");
		}
		StringBuilder sb = new StringBuilder();
		for (String[] l : lines) {
			sb.append(sbSepLine.toString());
			sb.append(displayOneLine(l));
		}
		sb.append(sbSepLine.toString());
		return sb.toString();
	}

	public static String[] splitLongString(String s, int maxWidth) {

		int slength = s.length();
		String[] ret = new String[slength / maxWidth + 1];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = s.substring(i * maxWidth,
					Math.min((i + 1) * maxWidth, slength));
		}
		return ret;
	}

	public static String getTextTableDisplay(int numColumns,
			int[] columnMaxWidth, List<String[]> table) {
		TxtTable tt = new TxtTable(numColumns, columnMaxWidth, table);
		return tt.display();
	}

	public static String getTextTableDisplay(int[] columnMaxWidth,
			List<String[]> table) {
		int numColumns = columnMaxWidth.length;
		TxtTable tt = new TxtTable(numColumns, columnMaxWidth, table);
		return tt.display();
	}

	public static String getTextTableDisplay(List<String[]> table) {
		int numColumns = table.get(0).length;
		int[] columnMaxWidth = new int[numColumns];
		for (int i = 0; i < columnMaxWidth.length; i++)
			columnMaxWidth[i] = 40;
		TxtTable tt = new TxtTable(numColumns, columnMaxWidth, table);
		return tt.display();
	}

	public static void main(String[] args) {
		List<String[]> table = new ArrayList<String[]>();
		table.add(new String[] {
				"XYZ",
				"This is a really nice day! I love University of Washington",
				"Jim Z Shi listened to Les Misérables: In Concert at the Royal Albert Hall by Les Misérables - 10th Anniversary Concert Cast on Spotify." });
		table.add(new String[] { "UUUVVV", "This is a really nice day! I love",
				"" });
		TxtTable tt = new TxtTable(3, new int[] { 10, 20, 20 }, table);
		System.out.println(tt.display());
	}
}
