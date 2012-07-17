/* 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package org.drugepi.match;

import java.util.List;

import org.drugepi.match.storage.*;

import Jama.Matrix;

public class MatchDistanceCalculator {
	public static int c;
	
	public static double getDistance(MatchPatient p1, MatchPatient p2) {
		return Math.abs(p2.getPs() - p1.getPs());
	}

	public static double getDistance(double p1, double p2) {
		return Math.abs(p2 - p1);
	}
	
	public static double[] getMidpoint(MatchPatient p1, MatchPatient p2) {
		double[][] ps = new double[2][2];
		
		ps[0][0] = p1.getPs(0);
		ps[0][1] = p1.getPs(1);

		ps[1][0] = p2.getPs(0);
		ps[1][1] = p2.getPs(1);

		double[] midpoint = new double[2];
		midpoint[0] = (ps[1][0] + ps[0][0]) / 2;
		midpoint[1] = (ps[1][1] + ps[0][1]) / 2;
		
//		System.out.printf("Midpoint between (%f, %f) and (%f, %f) is (%f, %f)\n",
//				ps[0][0], ps[0][1],
//				ps[1][0], ps[1][1],
//				midpoint[0], midpoint[1]);
		
		return midpoint;
	}

	private static Matrix[] getPsMatrices(MatchPatient p[])
	{
		Matrix points[] = new Matrix[p.length];
		for (int i = 0; i < p.length; i++) 
			points[i] = p[i].getPsAsMatrix();
		
		return points;
	}
	
	private static Matrix getCentroid(Matrix p[]) {
		Matrix centroid = new Matrix(1, p[0].getColumnDimension());
		
		// sum the columns
		for (int i = 0; i < p.length; i++) 
			centroid.plusEquals(p[i]);
		
		// take the average
		centroid.timesEquals(1d / (double) p.length);

		return centroid;
	}

	public static double getEuclideanDistanceSquared(Matrix from, Matrix to)
	{
		Matrix difference = to.minus(from);
		
		double sum = 0;
		for (int i = 0; i < from.getColumnDimension(); i++) {
			sum += Math.pow(difference.get(0, i), 2);
		}
		
		return sum;
	}
	
	public static double getEuclideanDistanceFromCentroid(MatchPatient p[])
	{
		Matrix patientPs[] = getPsMatrices(p);
		Matrix centroid = getCentroid(patientPs);
		
		double distance = 0;
		
		for (int i = 0; i < p.length; i++) {
			distance += Math.sqrt(getEuclideanDistanceSquared(patientPs[i], centroid));
		}
		
		return distance;
	}
	
	public static double getPenalizedDistanceFromCentroid(MatchPatient p[])
	{
		Matrix patientPs[] = getPsMatrices(p);
		Matrix centroid = getCentroid(patientPs);
		
		double distance = 0;
		
		for (int i = 0; i < p.length; i++) {
			distance += getEuclideanDistanceSquared(patientPs[i], centroid);
		}
		
		return Math.sqrt(distance);
	}

	public static double getPerimeterOfTriangle(List<MatchPatient> patients) 
	{
		double[][] ps = new double[3][2];
		
		ps[0][0] = patients.get(0).getPs(0);
		ps[0][1] = patients.get(0).getPs(1);

		ps[1][0] = patients.get(1).getPs(0);
		ps[1][1] = patients.get(1).getPs(1);

		ps[2][0] = patients.get(2).getPs(0);
		ps[2][1] = patients.get(2).getPs(1);

		double perim = 0;
		double t = 0;
		
		t = Math.pow(ps[1][0] - ps[0][0], 2) + Math.pow(ps[1][1] - ps[0][1], 2);
		perim += Math.sqrt(t);
		
		t = Math.pow(ps[2][0] - ps[1][0], 2) + Math.pow(ps[2][1] - ps[1][1], 2);
		perim += Math.sqrt(t);
		
		t = Math.pow(ps[0][0] - ps[2][0], 2) + Math.pow(ps[0][1] - ps[2][1], 2);
		perim += Math.sqrt(t);
		
		return perim;
	}	
	
	public static double getVectorLength(Matrix v)
	{
		return (v.norm2());
	}
	
	public static double getSumOverMatrix(Matrix m) {
		double sum = 0;
		
		for (int i = 0; i < m.getRowDimension(); i++) 
			for (int j = 0; j < m.getColumnDimension(); j++)
				sum += m.get(i, j);
		
		return sum;
	}
	
	public static double getDotProduct(Matrix v1, Matrix v2)
	{
		Matrix m = v1.arrayTimes(v2);
		double dotProduct = getSumOverMatrix(m);
		
		return dotProduct;
	}
	
	public static double abhiDistance(MatchPatient p1, MatchPatient p2) {
//		double dx = p.d[0] - d[0];
//		double dy = p.d[1] - d[1];
		return java.lang.Math.sqrt(abhiDistance2(p1, p2));
	}
	
	public static double abhiDistance2(MatchPatient p1, MatchPatient p2) {
		double dx = p2.getPs(0) - p1.getPs(0);
		double dy = p2.getPs(1) - p1.getPs(1);
		return (dx * dx + dy * dy);
	}
	
	public static ProjectionResult projectAndMove(MatchPatient r, MatchPatient b, 
			MatchPatient g) 
	{
		// Gets the scalar projection of BG onto RB
		// Then, moves from point B the length of the projection 
		// along the line RB
		
		ProjectionResult pr = new ProjectionResult();
		
		// Let point B be the origin
		Matrix pointB = b.getPsAsMatrix();

		// Recenter to the origin
		Matrix vectorRB = r.getPsAsMatrix().minus(pointB);
		Matrix vectorBG = g.getPsAsMatrix().minus(pointB);
		
//		System.out.printf("\nPROJECTION DATA\n" +
//				"R = (%f, %f), B = (%f, %f), G = (%f, %f)\n",
//				r.getPs(0), r.getPs(1),
//				b.getPs(0), b.getPs(1),
//				g.getPs(0), g.getPs(1)
//		);
//		
//		System.out.printf("RB = (%f, %f), BG = (%f, %f)\n",
//				vectorRB.get(0, 0), vectorRB.get(0, 1),
//				vectorBG.get(0, 0), vectorBG.get(0, 1)
//		);

		double dotProduct = getDotProduct(vectorBG, vectorRB);
		double lenRB = getVectorLength(vectorRB);
		double length = dotProduct / lenRB;
		pr.projectedLength = length;

//		System.out.printf("|RB| = %f\n", lenRB);
//		System.out.printf("|Projection| = %f\n", length);

		// use Abs to ignore sign
		double percentage = Math.abs(length / lenRB);
		pr.isPastOriginalPoint = (percentage > 1d);

//		System.out.printf("Percentage = %f\n", percentage);
	
		Matrix newMidpoint = pointB.plus(vectorRB.times(percentage));
		pr.projectedPoint = newMidpoint.getArray()[0];

//		System.out.printf("Final projected midpoint = (%f, %f)\n",
//				newMidpoint.get(0, 0), newMidpoint.get(0, 1)
//		);
		
//		System.out.printf("Theta is %f\n", Math.toDegrees(Math.acos(cosTheta)));
//		System.out.printf("Length A  is %f\n", lenRB);
//		System.out.printf("Length B  is %f\n", lenBG);
//		System.out.printf("Length P1 is %f\n", cosTheta * lenRB);

		// get projection
//		double num   = dotProduct;
//		double denom = getDotProduct(vectorBG, vectorBG);
//		
//		Matrix projection = vectorBG.times(num / denom); 
//
//		System.out.printf("Projected vector = (%f, %f), |v| = %f\n",
//				projection.get(0, 0), projection.get(0, 1), length
//		);
//
//		// Recenter back to Point B
//		projection.plusEquals(pointB);
		
		
//		System.out.printf("plot(x=1, y=1, xlim=c(0.086,0.088), ylim=c(0.7323,0.7327));\n");
//
//		System.out.printf("points(x=%f, y=%f, col=\"red\");\n" ,
//				r.getPsAsMatrix().get(0, 0), r.getPsAsMatrix().get(0, 1));
//
//		System.out.printf("points(x=%f, y=%f, col=\"green\");\n" ,
//				g.getPsAsMatrix().get(0, 0), g.getPsAsMatrix().get(0, 1));
//
//		System.out.printf("points(x=%f, y=%f, col=\"blue\");\n" ,
//				b.getPsAsMatrix().get(0, 0), b.getPsAsMatrix().get(0, 1));
//
//		System.out.printf("lines(x=c(0, %f), y=c(0, %f), type=\"l\", col=\"green\");\n" ,
//				vectorBG.get(0, 0), vectorBG.get(0, 1)
//		);
//
//		System.out.printf("lines(x=c(0, %f), y=c(0, %f), type=\"l\", col=\"blue\");\n" ,
//				vectorRB.get(0, 0), vectorRB.get(0, 1)
//		);
//		
//		System.out.printf("lines(x=c(%f, %f), y=c(%f, %f), type=\"l\", col=\"black\");\n" ,
//				b.getPsAsMatrix().get(0, 0), 
//				newMidpoint.get(0, 0), 
//				b.getPsAsMatrix().get(0, 1),
//				newMidpoint.get(0, 1)
//		);
		
		return pr;
	}
	
	public static void main(String[] args)
	{
		MatchPatient p1 = new MatchPatient(3);
		p1.addPs(-1d);
		p1.addPs(-2d);
//		p1.addPs(3d);
		
		MatchPatient p2 = new MatchPatient(3);
		p2.addPs(2d);
		p2.addPs(4d);
//		p2.addPs(5d);
		
		MatchPatient p3 = new MatchPatient(3);
		p3.addPs(0);
		p3.addPs(0);
//		p3.addPs(0);

		ProjectionResult pr = MatchDistanceCalculator.projectAndMove(p1, p3, p2);
		System.out.printf("Projection is %f, %f\n",
				pr.projectedPoint[0], pr.projectedPoint[1]);
		
//		double[] d1 = {1d, 1d, 1d };
//		double[] d2 = {2d, 2d, 2d };
//		Matrix m1 = new Matrix(d1, 3);
//		Matrix m2 = new Matrix(d2, 3);
//		double q = getVectorLength(m2);
//		double q2 = m2.norm2();
//		System.out.printf("Dot product is %f\n", q2);
	}
}


