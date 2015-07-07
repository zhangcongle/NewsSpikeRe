package edu.washington.nsre.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DelimitedReader {

	private int BUFFER_SIZE = 8 * 1024 * 1024;
	private BufferedReader br;
	public boolean EOF = false;
	int count = 0;
	long sec_last100000 = (new Date()).getTime();

	public DelimitedReader(String filename) {
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"), BUFFER_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DelimitedReader(String filename, int bufferSize) throws IOException {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"), bufferSize);
	}

	public DelimitedReader(String filename, String charset) throws IOException {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), charset), BUFFER_SIZE);
	}

	public DelimitedReader(String filename, String charset, boolean gzip) throws IOException {
		if (!gzip)
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), charset), BUFFER_SIZE);
		else
			br = new BufferedReader(new InputStreamReader(new java.util.zip.GZIPInputStream((new FileInputStream(
					filename))), charset), BUFFER_SIZE);
	}

	public String[] read() {
		try {
			count++;
			if (count % 100000 == 0) {
				Date d = new Date();
				long t = d.getTime();
				System.out.print(count + "\t" + (t - sec_last100000) + "\t" + d + "\r");
				sec_last100000 = t;
				System.out.flush();
			}
			String line = br.readLine();
			if (line == null) {
				this.EOF = true;
				return null;
			}
			/*
			 * String[] cols = line.split("\t"); for (int i=0; i < cols.length; i++)
			 * cols[i] = Util.unescape(cols[i]); return cols;
			 */
			return toCols(line);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String[] toCols(String line) {
		String[] buffer = new String[32];
		int numBuffer = 0;
		int start = 0;
		int end;

		while ((end = line.indexOf('\t', start)) != -1) {
			if (numBuffer >= buffer.length)
				buffer = extendBuffer(buffer);
			buffer[numBuffer++] = DelimitedEscape.unescape(line.substring(start, end));
			start = end + 1;
		}
		if (numBuffer >= buffer.length)
			buffer = extendBuffer(buffer);
		buffer[numBuffer++] = DelimitedEscape.unescape(line.substring(start));

		String[] returnValue = new String[numBuffer];
		System.arraycopy(buffer, 0, returnValue, 0, numBuffer);
		return returnValue;
	}

	private static String[] extendBuffer(String[] buffer) {
		String[] newBuffer = new String[2 * buffer.length];
		System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
		return newBuffer;
	}

	public List<String[]> readAll() throws IOException {
		List<String[]> all = new ArrayList<String[]>();
		String[] line;
		while ((line = read()) != null) {
			all.add(line);
		}
		return all;
	}

	public List<String[]> readAll(int MAX) throws IOException {
		List<String[]> all = new ArrayList<String[]>(MAX);
		String[] line;
		while ((line = read()) != null) {
			all.add(line);
		}
		return all;
	}

	public HashMap<String, String> readAll2Hash(int keyId, int valueId) throws IOException {
		HashMap<String, String> all = new HashMap<String, String>();
		String[] l;
		while ((l = read()) != null) {
			all.put(l[keyId], l[valueId]);
		}
		return all;
	}

	private String[] blockbuffer;

	public List<String[]> readBlock(int key) {
		if (blockbuffer == null) {
			blockbuffer = this.read();
		}
		if (this.EOF || blockbuffer == null)
			return null;
		List<String[]> block = new ArrayList<String[]>();
		block.add(blockbuffer);
		String[] l;
		while ((l = this.read()) != null && l[key].equals(block.get(0)[key])) {
			block.add(l);
		}
		blockbuffer = l;
		return block;
	}

	public List<String[]> readUntil(int key, int until) {
		if (blockbuffer == null) {
			blockbuffer = this.read();
		}
		if (this.EOF || blockbuffer == null)
			return null;
		List<String[]> block = new ArrayList<String[]>();
		block.add(blockbuffer);
		String[] l;
		while ((l = this.read()) != null && Integer.parseInt(l[key]) <= until) {
			block.add(l);
		}
		blockbuffer = l;
		return block;
	}

	private String[] blockbufferlimited;

	public List<String[]> readBlocklimited(int key, int limitedsize) throws IOException {
		if (blockbufferlimited == null) {
			blockbufferlimited = this.read();
		}
		if (this.EOF || blockbufferlimited == null)
			return null;
		List<String[]> block = new ArrayList<String[]>();
		block.add(blockbufferlimited);
		String[] l;
		while ((l = this.read()) != null && l[key].equals(block.get(0)[key])) {
			if (block.size() < limitedsize) {
				block.add(l);
			}
		}
		blockbufferlimited = l;
		return block;
	}

	public List<String[]> readBlock(int[] keys) throws IOException {
		if (blockbuffer == null) {
			blockbuffer = this.read();
		}
		if (this.EOF || blockbuffer == null)
			return null;
		List<String[]> block = new ArrayList<String[]>();
		block.add(blockbuffer);
		String[] l;
		while ((l = this.read()) != null) {
			boolean match = true;
			for (int k : keys) {
				if (!l[k].equals(block.get(0)[k])) {
					match = false;
					break;
				}
			}
			if (match) {
				block.add(l);
			} else {
				break;
			}
		}
		blockbuffer = l;
		return block;
	}

	public List<String[]> readBlocklimited(int[] keys, int limitedsize) throws IOException {
		if (blockbuffer == null) {
			blockbuffer = this.read();
		}
		if (this.EOF || blockbuffer == null)
			return null;
		List<String[]> block = new ArrayList<String[]>();
		block.add(blockbuffer);
		String[] l;
		while ((l = this.read()) != null) {
			boolean match = true;
			for (int k : keys) {
				if (!l[k].equals(block.get(0)[k])) {
					match = false;
					break;
				}
			}
			if (match) {
				if (block.size() < limitedsize) {
					block.add(l);
				}
			} else {
				break;
			}
		}
		blockbuffer = l;
		return block;
	}

	public void close() {
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Set<String> readColumn(String file, int columnId) {
		Set<String> all = new HashSet<String>();
		String[] l;
		DelimitedReader dr = new DelimitedReader(file);
		while ((l = dr.read()) != null) {
			all.add(l[columnId]);
		}
		dr.close();
		return all;
	}

	public static Set<Integer> readColumnInt(String file, int columnId) {
		Set<Integer> all = new HashSet<Integer>();
		String[] l;
		DelimitedReader dr = new DelimitedReader(file);
		while ((l = dr.read()) != null) {
			all.add(Integer.parseInt(l[columnId]));
		}
		dr.close();
		return all;
	}

}
