/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match.controllers;

import java.util.*;

import org.drugepi.match.MatchDistanceCalculator;
import org.drugepi.match.storage.*;

public class MatchPatientKDTree {
	private MatchPatientKDTree left, right;
	private int depth;
	private double split;
	private boolean leaf;
	private MatchPatient p;
	private boolean isleft = false;
	private boolean isdeleted = false;
	private double minx, miny;
	private double maxx, maxy;

	public static boolean debug = false;
	public static String pad = "                                                             ";

	public MatchPatientKDTree(MatchPatient p, int depth) {
		left = right = null;
		leaf = true;
		split = 0;
		isleft = true;
		this.p = p;
		minx = maxx = p.getPs(0);
		miny = maxy = p.getPs(1);
		leaf = true;
		this.depth = depth;
		isdeleted = false;
	}

	public MatchPatientKDTree(MatchPatient p, double s, int depth) {
		left = right = null;
		leaf = false;
		split = s;
		this.depth = depth;
		isleft = true;
		this.p = p;
		isdeleted = false;
	}

	public static Comparator<MatchPatient> xComp = new Comparator<MatchPatient>() {
		public int compare(MatchPatient o1, MatchPatient o2) {
			if (o1.getPs(0) == o2.getPs(0)) {
				return 0;
			} else if (o1.getPs(0) < o2.getPs(0)) {
				return 1;
			} else
				return -1;
		}
	};

	public static Comparator<MatchPatient> yComp = new Comparator<MatchPatient>() {
		public int compare(MatchPatient o1, MatchPatient o2) {
			if (o1.getPs(1) == o2.getPs(1)) {
				return 0;
			} else if (o1.getPs(1) < o2.getPs(1)) {
				return 1;
			} else
				return -1;
		}
	};

	public static MatchPatientKDTree makeTree(List<MatchPatient> mg) {
		MatchPatientKDTree root = makeTree(mg, 0, null);
		setMinMax(root);
		return root;
	}

	public static MatchPatientKDTree makeTree(List<MatchPatient> mg, int dep, MatchPatientKDTree par) {
		int n = mg.size();
		if (n <= 1) {
			MatchPatientKDTree k = new MatchPatientKDTree(mg.get(0), dep);
			return k;
		}

		// aas: this is the slower method for tree construction
		// replace with the median method
		if ((dep % 2) == 0) {
			Collections.sort(mg, xComp);
		} else {
			Collections.sort(mg, yComp);
		}

		List<MatchPatient> bigger = mg.subList(0, n / 2);
		List<MatchPatient> smaller = mg.subList(n / 2, n);

		MatchPatient m = smaller.get(0);
		double split = m.getPs((dep) % 2);
		MatchPatientKDTree k = new MatchPatientKDTree(m, split, dep);

		if (dep % 2 == 0) {
			k.maxx = mg.get(0).getPs(0);
			k.minx = mg.get(n - 1).getPs(0);
		} else {
			k.maxy = mg.get(0).getPs(1);
			k.miny = mg.get(n - 1).getPs(1);
		}

		k.left = makeTree(smaller, dep + 1, k);
		k.right = makeTree(bigger, dep + 1, k);
		k.leaf = false;
		k.left.isleft = true;
		k.right.isleft = false;

		return k;
	}

	public static void setMinMax(MatchPatientKDTree root) {
		if (root == null) {
			return;
		}
		if (root.depth == 0) {
			root.miny = java.lang.Math.min(root.left.miny, root.right.miny);
			root.maxy = java.lang.Math.max(root.left.maxy, root.right.maxy);
		}

		if (root.left != null && root.right != null) {
			if (root.depth % 2 == 0) {
				root.left.minx = root.minx;
				root.left.maxx = root.split;
				root.right.minx = root.split;
				root.right.maxx = root.maxx;
			} else {
				root.left.miny = root.miny;
				root.left.maxy = root.split;
				root.right.miny = root.split;
				root.right.maxy = root.maxy;
			}
			setMinMax(root.left);
			setMinMax(root.right);
		}
	}

	public static void printTree(MatchPatientKDTree root) {
		printTree(root, 0, root.miny, root.maxy);
	}

	// called with (root, 0, 0, 1.0)
	public static void printTree(MatchPatientKDTree node, int depth, double last,
			double direction) {
		if (node == null) {
			return;
		}
		int frac = 100 - depth * 5;
		double size = 0.005;
		System.out.println("\\draw[thin, blue!" + frac + "] (" + node.p.getPs(0)
				+ "," + node.p.getPs(1) + ") circle (" + size + "pt);");
		if (!node.leaf) {
			/*
			 * int td = depth; kdn pp = node; while (td >= 3) { pp =
			 * pp.parent.parent; if (node.isleft != pp.isleft) { direction =
			 * pp.parent.split; break; } td -= 2; } // aas: the code above
			 * produces touching lines instead of the min/max ones
			 */
			double leftdir, rightdir;

			String note = (node.isleft ? "L" : "R") + depth;

			if (depth % 2 == 0) {
				System.out.println("\\draw[thin,gray!" + frac + "] ("
						+ node.split + "," + last + ") -- node {" + note
						+ "} (" + node.split + "," + direction + ");     %% "
						+ depth);
				leftdir = node.minx;
				rightdir = node.maxx;
			} else {
				System.out.println("\\draw[thin,orange!" + frac + "] (" + last
						+ "," + node.split + ") -- node {" + note + "} ("
						+ direction + "," + node.split + ");    %% " + depth);
				leftdir = node.miny;
				rightdir = node.maxy;
			}

			printTree(node.left, depth + 1, node.split, leftdir);
			printTree(node.right, depth + 1, node.split, rightdir);
		}
	}

	// this attempts to delete a NODE that is in the TREE by setting its delete
	// flag and propagating delete flags up
	// if q is not a node in the tree, then the method does nothing.
	public boolean deletepoint(MatchPatient q) {
		if (leaf) {
			if (this.p.equals(q)) {
				isdeleted = true;
				return true;
			} else {
				System.out.println("    delete looking at " + this.p.id);
			}
			return false;
		}
		if (isdeleted) {
			return false;
		}

		double gap = q.getPs(this.depth % 2) - split;
		boolean done = false;

		if (gap <= 0) {
			done = left.deletepoint(q);
		} else {
			done = right.deletepoint(q);
		}

		if ((left == null || left.isdeleted)
				&& (right == null || right.isdeleted)) {
			isdeleted = true;
		}

		return done;

	}

	public MatchPatient nearest(MatchPatient q) {
		if (isdeleted) {
			return null;
		}
		
		if (debug) {
			System.out.println(pad.substring(0, depth)
					+ "\\draw[red!10,opacity=10] (" + minx + "," + miny
					+ ") rectangle (" + maxx + "," + maxy + ");");
		}
		
		if (leaf) {
			return this.p;
		}

		MatchPatient t = null;
		MatchPatientKDTree other = null;

		double gap = q.getPs(this.depth % 2) - split;

		if (gap <= 0) {
			t = left.nearest(q);
			other = right;
			gap = 0 - gap;
		} else {
			t = right.nearest(q);
			other = left;
		}

		double dist2 = Double.MAX_VALUE;
		if (t != null) {
			dist2 = MatchDistanceCalculator.abhiDistance2(q, t);
		}

		// possible for there to be a point on the other side that is closer to
		// the query point. recall that dist is distance-squared, so square gap
		MatchPatient t2 = t;
		if ((gap * gap) < dist2) {
			t2 = other.nearest(q);
			if (t2 != null && MatchDistanceCalculator.abhiDistance2(t2, q) < dist2) {
				t = t2;
			}
		}
		return t;
	}

	public List<MatchPatient> nearest(MatchPatient q, double dist) {
		
		PriorityQueue<ClosePatient> r = new PriorityQueue<ClosePatient>(10,
				new Comparator<Object>() {
					public int compare(Object o1, Object o2) {
						ClosePatient p1 = (ClosePatient) o1;
						ClosePatient p2 = (ClosePatient) o2;
						double d1 = p1.d;
						double d2 = p2.d;
						if (d1 < d2) {
							return 1;
						} else if (d1 > d2) {
							return -1;
						} else
							return 0;
					}
				});

		nearest(q, dist * dist, r);

		List<MatchPatient> results = new ArrayList<MatchPatient>(r.size());
		while (r.peek() != null) {
			results.add(r.poll().p);
		}

		return results;
	}

	private int nearest(MatchPatient q, double d2, PriorityQueue<ClosePatient> result) {
		if (isdeleted) {
			return 0;
		}
		if (leaf) {
			double dp = MatchDistanceCalculator.abhiDistance2(this.p, q); 
			if (dp < d2) {
				// System.out.format(" [nearest] query:%3d ADD %03d d2:%.06f %.06f\n",
				// q.i,p.i,d2,dp);
				result.add(new ClosePatient(this.p, d2));
				return 1;
			}
		}

		MatchPatientKDTree other = null;
		int num = 0;

		double gap = q.getPs(this.depth % 2) - this.split;

		if (gap <= 0) {
			other = right;
			gap = 0 - gap;
			if (left != null) {
				// System.out.format("      %3d [%2d] d2:%.07f left on %.06f gap:%.07f\n",q.i,depth,d2,split,gap);
				num = left.nearest(q, d2, result);
			}
		} else {
			other = left;
			if (right != null) {
				// System.out.format("      %3d [%2d] d2:%.07f right on %.06f gap:%.07f\n",q.i,depth,d2,split,gap);
				num = right.nearest(q, d2, result);
			}
		}

		// possible for there to be points on the other side that are within
		// delta
		// of the query point. recall that dist is distance-squared, so square
		// gap
		if ((gap * gap) < d2) {
			if (other != null) { // System.out.format("       %3d [%2d] d2:%.07f other on %.06f gap:%.07f\n",q.i,depth,d2,split,gap);
				num += other.nearest(q, d2, result);
			}
		} /*
		 * else { System.out.format(
		 * "      [%2d] SKIP other split:%.07f d2:%.07f gap:%.07f\n",
		 * depth,this.split,d2,gap); }
		 */

		return num;

	}

	private class ClosePatient {
		MatchPatient p;
		double d;

		public ClosePatient(MatchPatient p, double d) {
			this.p = p;
			this.d = d;
		}
	}

}