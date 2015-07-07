package edu.washington.nsre.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class DW {

	private int BUFFER_SIZE = 8 * 1024 * 1024;
	private BufferedWriter bw;

	public DW(String filename) {
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"), BUFFER_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DW(String filename, int bufferSize) throws IOException {
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"), bufferSize);
	}

	public DW(String filename, boolean append) throws IOException {
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), "utf-8"), BUFFER_SIZE);
	}

	public DW(String filename, String charset) throws IOException {
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), charset), BUFFER_SIZE);
	}

	public DW(String filename, String charset, boolean gzipped) throws IOException {
		if (gzipped)
			bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename)),
					charset), BUFFER_SIZE);
		else
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), charset), BUFFER_SIZE);
	}

	public void write(String... cols) {
		try {
			if (cols.length == 0)
				return;

			bw.write(DelimitedEscape.escape(cols[0]));
			for (int i = 1; i < cols.length; i++)
				bw.write("\t" + DelimitedEscape.escape(cols[i]));
			bw.write("\n");
		} catch (Exception e) {

		}
	}

	public static void write(String output, List<String[]> tow) {
		DW dw = new DW(output);
		for (String[] w : tow)
			dw.write(w);
		dw.close();
	}

	public void write(Object... cols) {
		try {
			if (cols.length == 0)
				return;
			bw.write(DelimitedEscape.escape(cols[0].toString()));
			for (int i = 1; i < cols.length; i++)
				bw.write("\t" + DelimitedEscape.escape(cols[i].toString()));
			bw.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] tow(Object... cols) {
		if (cols.length == 0)
			return null;
		String[] w = new String[cols.length];
		for (int i = 0; i < cols.length; i++) {
			w[i] = DelimitedEscape.escape(cols[i].toString());
		}
		return w;
	}

	public void close() {
		try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void flush() throws IOException {
		bw.flush();
	}
}
