/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.util;

/**
 * Quick selection algorithm. Places the kth smallest item in a[k-1].
 * 
 * @param a
 *            an array of Comparable items.
 * @param k
 *            the desired rank (1 is minimum) in the entire array.
 */
@SuppressWarnings("rawtypes")
public class Quickselect {
	public static Comparable getMedian(Comparable[] a) {
		int k = Math.round(a.length / 2);
		quickSelect(a, 0, a.length - 1, k);
		
		return a[k - 1];
	}
	
	public static Comparable getQuartile(Comparable[] a, int quartile) {
		int k = (int) Math.round(a.length * (0.25 * quartile));
		quickSelect(a, 0, a.length - 1, k);
		
		return a[k - 1];
	}

	public static Comparable getQuintile(Comparable[] a, int quintile) {
		int k = (int) Math.round(a.length * (0.2 * quintile));
		quickSelect(a, 0, a.length - 1, k);
		
		return a[k - 1];
	}
	
	public static Comparable getDecile(Comparable[] a, int decile) {
		int k = (int) Math.round(a.length * (0.1 * decile));
		quickSelect(a, 0, a.length - 1, k);
		
		return a[k - 1];
	}
	
	public static void quickSelect(Comparable[] a, int k) {
		quickSelect(a, 0, a.length - 1, k);
	}

	/**
	 * Internal selection method that makes recursive calls. Uses
	 * median-of-three partitioning and a cutoff of 10. Places the kth smallest
	 * item in a[k-1].
	 * 
	 * @param a
	 *            an array of Comparable items.
	 * @param low
	 *            the left-most index of the subarray.
	 * @param high
	 *            the right-most index of the subarray.
	 * @param k
	 *            the desired rank (1 is minimum) in the entire array.
	 */
	@SuppressWarnings("unchecked")
	private static void quickSelect(Comparable[] a, int low, int high, int k) {
		if (low + CUTOFF > high)
			insertionSort(a, low, high);
		else {
			// Sort low, middle, high
			int middle = (low + high) / 2;
			if (a[middle].compareTo(a[low]) < 0)
				swapReferences(a, low, middle);
			if (a[high].compareTo(a[low]) < 0)
				swapReferences(a, low, high);
			if (a[high].compareTo(a[middle]) < 0)
				swapReferences(a, middle, high);

			// Place pivot at position high - 1
			swapReferences(a, middle, high - 1);
			Comparable pivot = a[high - 1];

			// Begin partitioning
			int i, j;
			for (i = low, j = high - 1;;) {
				while (a[++i].compareTo(pivot) < 0)
					;
				while (pivot.compareTo(a[--j]) < 0)
					;
				if (i >= j)
					break;
				swapReferences(a, i, j);
			}

			// Restore pivot
			swapReferences(a, i, high - 1);

			// Recurse; only this part changes
			if (k <= i)
				quickSelect(a, low, i - 1, k);
			else if (k > i + 1)
				quickSelect(a, i + 1, high, k);
		}
	}

	/**
	 * Internal insertion sort routine for subarrays that is used by quicksort.
	 * 
	 * @param a
	 *            an array of Comparable items.
	 * @param low
	 *            the left-most index of the subarray.
	 * @param n
	 *            the number of items to sort.
	 */
	@SuppressWarnings("unchecked")
	private static void insertionSort(Comparable[] a, int low, int high) {
		for (int p = low + 1; p <= high; p++) {
			Comparable tmp = a[p];
			int j;

			for (j = p; j > low && tmp.compareTo(a[j - 1]) < 0; j--)
				a[j] = a[j - 1];
			a[j] = tmp;
		}
	}

	private static final int CUTOFF = 10;

	/**
	 * Method to swap to elements in an array.
	 * 
	 * @param a
	 *            an array of objects.
	 * @param index1
	 *            the index of the first object.
	 * @param index2
	 *            the index of the second object.
	 */
	public static final void swapReferences(Object[] a, int index1, int index2) {
		Object tmp = a[index1];
		a[index1] = a[index2];
		a[index2] = tmp;
	}
}