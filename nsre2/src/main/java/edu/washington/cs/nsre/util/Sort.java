package edu.washington.nsre.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



public class Sort {

	//public static long MEMORY = 100 * 1024 *1024;
//	public static long MEMORY = 1024l * 1024 *1024;
	public static long MEMORY = 512 * 1024l *1024l;
//	public static long MEMORY = 4l * 1024l * 1024l *1024l;
	//public static long MEMORY = 2048l * 1024 *1024;
	
	public static void sort(String input, String output, String tmpDir, 
			Comparator<String[]> comp)
	throws IOException
	{
		String prefix = tmpDir + File.separator + "run";
		int stage = 0;
		int runs = 0;
		
		// sort initial runs
		List<String[]> list = new ArrayList<String[]>();
		long memoryUse = 0;
		String[] t;
		DelimitedReader r = new DelimitedReader(input);		
		while ((t = r.read()) != null)
		{
			//if (runs <= 19)
			list.add(t);
			
			// memory use estimate
			memoryUse += 4 + 4*t.length;
			for (int i=0; i < t.length; i++) memoryUse += 2 * t[i].length(); 
			
			if (memoryUse >= MEMORY) {
				//if (runs <= 19)
				//	computeInitialRun(runs, list, comp, prefix + stage + "-");
				//runs++;
				computeInitialRun(runs++, list, comp, prefix + stage + "-");
				list.clear();
				memoryUse = 0;
			}
		}
		if (list.size() > 0) {			
			computeInitialRun(runs++, list, comp, prefix + stage + "-");
			list.clear();
		}
		
		while (runs > 1)
		{
			stage++;
			
			// begin merging
			for (int i=0; i < runs/2; i++) {
				String r1 = prefix + (stage-1) + "-" + (2*i);
				String r2 = prefix + (stage-1) + "-" + (2*i + 1);
				
				computeMergedRun(r1, r2,
						prefix + stage + "-" + i, comp);
				new File(r1).delete();
				new File(r2).delete();
			}
			if (runs % 2 == 1) {   // 2*(runs/2) equals runs-1
				new File(prefix + (stage-1) + "-" + 2*(runs/2)).renameTo(
						new File(prefix + stage + "-" + (runs/2)));
			}
			runs = (runs % 2 == 0) ? runs/2 : runs/2 + 1;
		}
		
		move(prefix + stage + "-0", output);
		//new File(prefix + stage + "-0").renameTo(new File(output));
	}

	public static void debug(String input, String output, String tmpDir, 
			Comparator<String[]> comp)
	throws IOException
	{
		String prefix = tmpDir + File.separator + "run";
		// complete stage 4
		computeMergedRun(prefix+"3-50", prefix+"3-51", prefix+"4-25", comp);
		computeMergedRun(prefix+"3-52", prefix+"3-53", prefix+"4-26", comp);
		computeMergedRun(prefix+"3-54", prefix+"3-55", prefix+"4-27", comp);
		computeMergedRun(prefix+"3-56", prefix+"3-57", prefix+"4-28", comp);
		computeMergedRun(prefix+"3-58", prefix+"3-59", prefix+"4-29", comp);
		computeMergedRun(prefix+"3-60", prefix+"3-61", prefix+"4-30", comp);
		computeMergedRun(prefix+"3-62", prefix+"3-63", prefix+"4-31", comp);
		computeMergedRun(prefix+"3-64", prefix+"3-65", prefix+"4-32", comp);
		computeMergedRun(prefix+"3-66", prefix+"3-67", prefix+"4-33", comp);
		computeMergedRun(prefix+"3-68", prefix+"3-69", prefix+"4-34", comp);
		computeMergedRun(prefix+"3-70", prefix+"3-71", prefix+"4-35", comp);
		computeMergedRun(prefix+"3-72", prefix+"3-73", prefix+"4-36", comp);
		computeMergedRun(prefix+"3-74", prefix+"3-75", prefix+"4-37", comp);
		computeMergedRun(prefix+"3-76", prefix+"3-77", prefix+"4-38", comp);
		computeMergedRun(prefix+"3-78", prefix+"3-79", prefix+"4-39", comp);
		computeMergedRun(prefix+"3-80", prefix+"3-81", prefix+"4-40", comp);
		new File(prefix+"3-82").renameTo(
				new File(prefix + "4-41"));

		int stage = 4;
		int runs = 42;
		while (runs > 1)
		{
			stage++;
			
			// begin merging
			for (int i=0; i < runs/2; i++) {
				String r1 = prefix + (stage-1) + "-" + (2*i);
				String r2 = prefix + (stage-1) + "-" + (2*i + 1);
				
				computeMergedRun(r1, r2,
						prefix + stage + "-" + i, comp);
				new File(r1).delete();
				new File(r2).delete();
			}
			if (runs % 2 == 1) {   // 2*(runs/2) equals runs-1
				new File(prefix + (stage-1) + "-" + 2*(runs/2)).renameTo(
						new File(prefix + stage + "-" + (runs/2)));
			}
			runs = (runs % 2 == 0) ? runs/2 : runs/2 + 1;
		}
		
		move(prefix + stage + "-0", output);		
	}
	
	private static void computeInitialRun(int run, List<String[]>list, 
			Comparator<String[]> comp, String prefix) 
		throws IOException {
		Collections.sort(list, comp);
		// write sorted collection to disk
		DelimitedWriter w = new DelimitedWriter(prefix + run);
		for (int i=0; i < list.size(); i++)
			w.write(list.get(i));
		w.close();
	}
	
	public static void computeMergedRun(String run1, String run2, String mergedRun,
		Comparator<String[]> comp) 
		throws IOException {
		
		DelimitedReader r1 = new DelimitedReader(run1);
		DelimitedReader r2 = new DelimitedReader(run2);
		DelimitedWriter w = new DelimitedWriter(mergedRun);
		
		String[] t1 = r1.read();
		String[] t2 = r2.read();
		
		while (t1 != null && t2 != null) {
			int c = comp.compare(t1, t2);
			if (c < 0) {
				w.write(t1);
				t1 = r1.read();
			} else if (c > 0) {
				w.write(t2);
				t2 = r2.read();
			} else {
				w.write(t1);
				w.write(t2);
				t1 = r1.read();
				t2 = r2.read();
			}
		}
		while (t1 != null) {
			w.write(t1);
			t1 = r1.read();
		}
		while (t2 != null) {
			w.write(t2);
			t2 = r2.read();
		}
		
		w.close();
		r1.close();
		r2.close();
	}
	
	private static void move(String from, String to) throws IOException {
		File fromFile = new File(from);
		File toFile = new File(to);
		
		// if target exists delete
		if (toFile.exists()) toFile.delete();
		
		// try if renaming works (e.g. if on same drive, platform-dependent)
		fromFile.renameTo(toFile);		
		if (toFile.exists()) return;
		
		// must copy file
        FileChannel inChannel = new
        	FileInputStream(from).getChannel();
        FileChannel outChannel = new
        	FileOutputStream(to).getChannel();
	    try {
           // magic number for Windows, 64Mb - 32Kb)
           int maxCount = (64 * 1024 * 1024) - (32 * 1024);
           long size = inChannel.size();
           long position = 0;
           while (position < size) {
              position += 
                inChannel.transferTo(position, maxCount, outChannel);
           }
	    } 
	    catch (IOException e) {
	        throw e;
	    }
	    finally {
	        if (inChannel != null) inChannel.close();
	        if (outChannel != null) outChannel.close();
	    }
	    fromFile.delete();
	}
	
	public static void main(String... args) throws Exception {
		String input = null, output = null;
		String tmpDir = "/tmp";
		boolean numeric = false, ignoreCase = false;
		int field = 0;
		
		for (int i=0; i < args.length; i++) {
			if (args[i].equals("-input"))
				input = args[++i];
			else if (args[i].equals("-output"))
				output = args[++i];
			else if (args[i].equals("-tmpDir"))
				tmpDir = args[++i];
			else if (args[i].equals("-numeric"))
				numeric = true;
			else if (args[i].equals("-ignoreCase"))
				ignoreCase = true;
			else if (args[i].equals("-field"))
				field = Integer.parseInt(args[++i]);
		}
		
		final int f = field;
		final boolean n = numeric;
		final boolean i = ignoreCase;
		
		Comparator<String[]> comparator = new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2) {
				if (n) {
					int i1 = Integer.parseInt(o1[f]);
					int i2 = Integer.parseInt(o2[f]);
					return i1 - i2;
				} else if (i) {
					return o1[f].compareToIgnoreCase(o2[f]);
				} else {
					return o1[f].compareTo(o2[f]);
				}
			}
		};
		
		sort(input, output, tmpDir, comparator);		
	}
	
	public static void sort(String... args) throws Exception {
		main(args);
	}
}
