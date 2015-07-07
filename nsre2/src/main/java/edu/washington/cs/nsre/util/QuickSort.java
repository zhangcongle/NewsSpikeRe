package edu.washington.nsre.util;

/**Written by Xiao*/
public class QuickSort {
	/**doesn't change the order of main*/
	public static int[] quicksortReturnIndexOnly(int[] main) {
		double[] temp = new double[main.length];
		for (int i = 0; i < main.length; i++)
			temp[i] = main[i];
		return quicksort(temp);
	}

	public static void sortByIndex(int[] main, int[] index) {
		int[] temp = new int[main.length];
		System.arraycopy(main, 0, temp, 0, main.length);
		for (int i = 0; i < index.length; i++) {
			main[i] = temp[index[i]];
		}
	}

	public static void sortByIndex(double[] main, int[] index) {
		double[] temp = new double[main.length];
		System.arraycopy(main, 0, temp, 0, main.length);
		for (int i = 0; i < index.length; i++) {
			main[i] = temp[index[i]];
		}
	}

	public static int[] quicksort(double[] main) {
		int[] index = new int[main.length];
		for (int i = 0; i < main.length; i++) {
			index[i] = i;
		}
		quicksort(main, index, 0, index.length - 1);
		return index;
	}

	public static int[] quicksort(double[] main, boolean inverse) {
		int[] index = new int[main.length];
		for (int i = 0; i < main.length; i++) {
			index[i] = i;
		}
		if (!inverse) {
			quicksort(main, index, 0, index.length - 1);
		} else {
			double[] temp = new double[main.length];
			for (int i = 0; i < main.length; i++)
				temp[i] = -1.0 * main[i];
			quicksort(temp, index, 0, index.length - 1);
		}
		return index;
	}

	// quicksort a[left] to a[right]
	public static void quicksort(double[] a, int[] index, int left, int right) {
		if (right <= left)
			return;
		int i = partition(a, index, left, right);
		quicksort(a, index, left, i - 1);
		quicksort(a, index, i + 1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, int left, int right) {
		int i = left - 1;
		int j = right;
		while (true) {
			while (less(a[++i], a[right]))
				// find item on left to swap
				; // a[right] acts as sentinel
			while (less(a[right], a[--j]))
				// find item on right to swap
				if (j == left)
					break; // don't go out-of-bounds
			if (i >= j)
				break; // check if pointers cross
			exch(a, index, i, j); // swap two elements into place
		}
		exch(a, index, i, right); // swap with partition element
		return i;
	}

	// is x < y ?
	private static boolean less(double x, double y) {
		return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
		double swap = a[i];
		a[i] = a[j];
		a[j] = swap;
		int b = index[i];
		index[i] = index[j];
		index[j] = b;
	}

	public static int largestIndex(double[] w) {
		double maxw = Double.MIN_VALUE;
		int index = 0;
		for (int i = 0; i < w.length; i++) {
			if (w[i] > maxw) {
				maxw = w[i];
				index = i;
			}
		}
		return index;
	}
}
