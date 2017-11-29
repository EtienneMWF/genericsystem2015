package org.genericsystem.cv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.genericsystem.cv.LinesDetector8.Line;
import org.genericsystem.cv.LinesDetector8.Lines;
import org.genericsystem.cv.utils.NativeLibraryLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class LinesDetector9 {

	static {
		NativeLibraryLoader.load();
	}

	public static void main(String[] args) {
		Img img = new Img(Imgcodecs.imread("resources/liberty_loft.jpg"));

		List<Edgelet> edgelets1 = computeEdgelets(img, 3);

		double[] vp1 = ransacVanishingPoint(edgelets1, 2000, 5);
		System.out.println("vp1 : " + vp1[0] / vp1[2] + ", " + vp1[1] / vp1[2] + ", 1");
		vp1 = reestimate_model(vp1, edgelets1, 5);
		System.out.println("vp1 reestimation : " + Arrays.toString(vp1));

		List<Edgelet> edgelets2 = removeInliers(vp1, edgelets1, 10);

		double[] vp2 = ransacVanishingPoint(edgelets2, 2000, 5);
		System.out.println("vp2 : " + vp2[0] / vp2[2] + ", " + vp2[1] / vp2[2] + " ,1");
		vp2 = reestimate_model(vp2, edgelets2, 5);
		System.out.println("vp2 reestimation : " + Arrays.toString(vp2));

		// double[][] vps = ransac_3_line(edgelets1, 1, 3000, 5);
		// vp1 = vps[0];
		// System.out.println("vp1 : " + vps[0][0] / vps[0][2] + ", " + vps[0][1] / vps[0][2] + ", 1");
		// vp2 = vps[1];
		// System.out.println("vp2 : " + vps[1][0] / vps[1][2] + ", " + vps[1][1] / vps[1][2] + ", 1");
		// double[] vp1 = new double[] { -1.28100000e+03, 9.33000000e+02, 1.00000000e+00 };
		// double[] vp2 = new double[] { 2.42000000e+02, -5.83520000e+04, 1.00000000e+00 };

		Imgcodecs.imwrite("resources/liberty_loft2.jpg", computeHomographyAndWarp(img, vp1, vp2, true, 3).getSrc());
	}

	private static class Edgelet {
		final double[] location;
		final double[] direction;
		final double strength;

		public Edgelet(Line line) {
			location = new double[] { (line.x1 + line.x2) / 2, (line.y1 + line.y2) / 2 };
			double x2minusx1 = line.x2 - line.x1;
			double y2minusy1 = line.y2 - line.y1;
			strength = Math.sqrt(Math.pow(x2minusx1, 2) + Math.pow(y2minusy1, 2));
			direction = new double[] { x2minusx1 / strength, y2minusy1 / strength };
		}
	}

	public static List<Edgelet> computeEdgelets(Img image, int sigma/* 3 */) {
		Img gray_img = image.bgr2Gray();
		Img edges = gray_img.canny(20, 80);
		Lines lines = new Lines(edges.houghLinesP(1, Math.PI / 180, 10, 3, 2));
		return lines.lines.stream().map(Edgelet::new).collect(Collectors.toList());
	}

	public static List<double[]> edgelet_lines(List<Edgelet> edgelets) {
		return edgelets.stream().map(edgelet -> new double[] { edgelet.direction[1], -edgelet.direction[0], edgelet.direction[0] * edgelet.location[1] - edgelet.direction[1] * edgelet.location[0] }).collect(Collectors.toList());
		// normals=np.zeros_like(directions);
		// normals[:,0]=directions[:,1];
		// normals[:,1]=-directions[:,0];
		// p=-np.sum(locations*normals,axis=1);
		// lines=np.concatenate((normals,p[:,np.newaxis]),axis=1);
	}

	public static List<Double> compute_votes(List<Edgelet> edgelets, double[] model, int threshold_inlier/* =5 */) {

		double[] vp = new double[] { model[0] / model[2], model[1] / model[2] };

		// List<double[]> est_directions = new ArrayList<>();
		List<Double> result = new ArrayList<>();
		for (Edgelet edgelet : edgelets) {
			double est_directions0 = edgelet.location[0] - vp[0];
			double est_directions1 = edgelet.location[1] - vp[1];
			// est_directions.add(new double[] { est_directions0, est_directions1 });
			double dotProd = est_directions0 * edgelet.direction[0] + est_directions1 * edgelet.direction[1];
			double absProd = Math.sqrt(Math.pow(edgelet.direction[0], 2) + Math.pow(edgelet.direction[1], 2)) * Math.sqrt(Math.pow(est_directions0, 2) + Math.pow(est_directions1, 2));
			if (absProd == 0)
				absProd = 1e-5;
			double theta = Math.acos(Math.abs(dotProd / absProd));
			double theta_thresh = threshold_inlier * Math.PI / 180;
			result.add(theta < theta_thresh ? edgelet.strength : 0);
		}
		return result;
		// est_directions = locations - vp;
		// dot_prod = np.sum(est_directions * directions, axis = 1);
		// abs_prod = np.linalg.norm(directions, axis = 1) * np.linalg.norm(est_directions, axis = 1);
		// abs_prod[abs_prod == 0] = 1e-5;
		//
		// cosine_theta = dot_prod / abs_prod;
		// theta = np.arccos(np.abs(cosine_theta));
		//
		// theta_thresh = threshold_inlier * np.pi / 180;
		// return (theta < theta_thresh) * strengths;
	}

	private static List<Integer> reverseArgSort(final List<Double> a) {
		List<Integer> indexes = new ArrayList<>(a.size());
		for (int i = 0; i < a.size(); i++)
			indexes.add(i);
		Collections.sort(indexes, (i1, i2) -> -Double.compare(a.get(i1), a.get(i2)));
		return indexes;
	}

	public static double[] ransacVanishingPoint(List<Edgelet> edgelets, int num_ransac_iter/* 2000 */, int threshold_inlier/* 5 */) {

		List<Double> strengths = edgelets.stream().map(edgelet -> edgelet.strength).collect(Collectors.toList());
		List<double[]> lines = edgelet_lines(edgelets);

		List<Integer> sorted = reverseArgSort(strengths);
		List<Integer> first_index_space = new ArrayList<>(sorted.subList(0, strengths.size() / 5));
		List<Integer> second_index_space = new ArrayList<>(sorted.subList(0, strengths.size() / 2));
		double[] best_model = null;
		double best_votes = 0;
		for (int ransacIter = 0; ransacIter < num_ransac_iter; ransacIter++) {
			double[] l1 = lines.get(first_index_space.get((int) (Math.random() * first_index_space.size())));
			double[] l2 = lines.get(second_index_space.get((int) (Math.random() * second_index_space.size())));
			double[] current_model = new double[] { l1[1] * l2[2] - l1[2] * l2[1], l1[2] * l2[0] - l1[0] * l2[2], l1[0] * l2[1] - l1[1] * l2[0] };
			// System.out.println("coucou " + current_model[2] + Arrays.toString(l1) + Arrays.toString(l2));
			if (current_model[0] * current_model[0] + current_model[1] * current_model[1] + current_model[2] * current_model[2] < 1 || current_model[2] == 0)
				// reject degenerate candidates
				continue;
			double current_votes = compute_votes(edgelets, current_model, threshold_inlier).stream().mapToDouble(d -> d).sum();
			// System.out.println("coucou : " + current_votes);
			if (current_votes > best_votes) {
				best_model = current_model;
				best_votes = current_votes;
				System.out.println("Current best model has " + best_votes + " votes at iteration " + ransacIter);
			}
		}
		return best_model;
	}

	public static double[][] ransac_3_line(List<Edgelet> edgelets, double focal_length, int num_ransac_iter/* 2000 */, int threshold_inlier/* =5 */) {
		List<Double> strengths = edgelets.stream().map(edgelet -> edgelet.strength).collect(Collectors.toList());
		List<double[]> lines = edgelet_lines(edgelets);
		List<Integer> sorted = reverseArgSort(strengths);
		List<Integer> first_index_space = new ArrayList<>(sorted.subList(0, strengths.size() / 5));
		List<Integer> second_index_space = new ArrayList<>(sorted.subList(0, strengths.size() / 5));
		List<Integer> third_index_space = new ArrayList<>(sorted.subList(0, strengths.size() / 2));

		double[][] best_model = null;
		double best_votes = 0;

		for (int ransacIter = 0; ransacIter < num_ransac_iter; ransacIter++) {
			double[] l1 = lines.get(first_index_space.get((int) (Math.random() * first_index_space.size())));
			double[] l2 = lines.get(second_index_space.get((int) (Math.random() * second_index_space.size())));
			double[] l3 = lines.get(third_index_space.get((int) (Math.random() * third_index_space.size())));

			double[] vp1 = new double[] { l1[1] * l2[2] - l1[2] * l2[1], l1[2] * l2[0] - l1[0] * l2[2], l1[0] * l2[1] - l1[1] * l2[0] };
			double[] h = new double[] { vp1[0] / (focal_length * focal_length), vp1[1] / (focal_length * focal_length), vp1[2] };
			// h = np.dot(vp1, [1 / focal_length**2, 1 / focal_length**2, 1]);
			double[] vp2 = new double[] { h[1] * l3[2] - h[2] * l3[1], h[2] * l3[0] - h[0] * l3[2], h[0] * l3[1] - h[1] * l3[0] };

			if ((vp1[0] * vp1[0] + vp1[1] * vp1[1] + vp1[2] * vp1[2] < 1) || vp1[2] == 0)
				continue;
			if ((vp2[0] * vp2[0] + vp2[1] * vp2[1] + vp2[2] * vp2[2] < 1) || vp2[2] == 0)
				continue;
			double current_votes = compute_votes(edgelets, vp1, threshold_inlier).stream().filter(d -> d > 0).mapToDouble(d -> d).sum() + compute_votes(edgelets, vp2, threshold_inlier).stream().filter(d -> d > 0).mapToDouble(d -> d).sum();
			if (current_votes > best_votes) {
				best_model = new double[][] { vp1, vp2 };
				best_votes = current_votes;
				System.out.println("Current best model has : " + current_votes + " votes at iteration : " + ransacIter);
			}
		}
		return best_model;
	}

	public static double[] reestimate_model(double[] model, List<Edgelet> edgelets, int threshold_reestimate/* =5 */) {
		List<Double> votes = compute_votes(edgelets, model, threshold_reestimate);
		List<Edgelet> inliersEdgelets = new ArrayList<>();
		for (int i = 0; i < edgelets.size(); i++)
			if (votes.get(i) > 0)
				inliersEdgelets.add(edgelets.get(i));
		List<double[]> lines = edgelet_lines(inliersEdgelets);
		double[][] a = lines.stream().map(line -> new double[] { line[0], line[1] }).toArray(double[][]::new);
		double[] b = lines.stream().mapToDouble(line -> -line[2]).toArray();
		DecompositionSolver ds = new SingularValueDecomposition(MatrixUtils.createRealMatrix(a)).getSolver();
		double[] est_model = ds.solve(MatrixUtils.createRealVector(b)).toArray();
		// double[] est_model = np.linalg.lstsq(a, b)[0];
		return new double[] { est_model[0], est_model[1], 1.0 };
	}

	public static List<Edgelet> removeInliers(double[] model, List<Edgelet> edgelets, int threshold_inlier/* 10 */) {
		List<Double> votes = compute_votes(edgelets, model, threshold_inlier);
		List<Edgelet> inliersEdgelets = new ArrayList<>();
		for (int i = 0; i < edgelets.size(); i++)
			if (votes.get(i) <= 0)
				inliersEdgelets.add(edgelets.get(i));
		return inliersEdgelets;
	}

	public static Img computeHomographyAndWarp(Img image, double[] vp1, double[] vp2, boolean clip/* true */, int clip_factor/* 3 */) {
		double[] vanishing_line = new double[] { vp1[1] * vp2[2] - vp1[2] * vp2[1], vp1[2] * vp2[0] - vp1[0] * vp2[2], vp1[0] * vp2[1] - vp1[1] * vp2[0] };
		// System.out.println(Arrays.toString(vanishing_line));

		double[][] H = new double[][] { new double[] { 1, 0, 0 }, new double[] { 0, 1, 0 }, new double[] { 0, 0, 1 } };
		H[2] = new double[] { vanishing_line[0] / vanishing_line[2], vanishing_line[1] / vanishing_line[2], 1 };
		// H=H/H[2,2];
		// System.out.println(Arrays.deepToString(H));
		double[] v_post1 = new double[] { H[0][0] * vp1[0] + H[0][1] * vp1[1] + H[0][2] * vp1[2], H[1][0] * vp1[0] + H[1][1] * vp1[1] + H[1][2] * vp1[2], H[2][0] * vp1[0] + H[2][1] * vp1[1] + H[2][2] * vp1[2] };
		double[] v_post2 = new double[] { H[0][0] * vp2[0] + H[0][1] * vp2[1] + H[0][2] * vp2[2], H[1][0] * vp2[0] + H[1][1] * vp2[1] + H[1][2] * vp2[2], H[2][0] * vp2[0] + H[2][1] * vp2[1] + H[2][2] * vp2[2] };

		double norm1 = Math.sqrt(v_post1[0] * v_post1[0] + v_post1[1] * v_post1[1]);
		v_post1 = new double[] { v_post1[0] / norm1, v_post1[1] / norm1, v_post1[2] / norm1 };

		double norm2 = Math.sqrt(v_post2[0] * v_post2[0] + v_post2[1] * v_post2[1]);

		v_post2 = new double[] { v_post2[0] / norm2, v_post2[1] / norm2, v_post2[2] / norm2 };

		double[][] directions = new double[][] { new double[] { v_post1[0], -v_post1[0], v_post2[0], -v_post2[0] }, new double[] { v_post1[1], -v_post1[1], v_post2[1], -v_post2[1] } };
		// System.out.println(Arrays.deepToString(directions));

		double thetas[] = new double[] { Math.atan2(directions[0][0], directions[1][0]), Math.atan2(directions[0][1], directions[1][1]), Math.atan2(directions[0][2], directions[1][2]), Math.atan2(directions[0][3], directions[1][3]) };
		// System.out.println(Arrays.toString(thetas));
		int h_ind = 0;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < thetas.length; i++) {
			double absTheta = Math.abs(thetas[i]);
			if (absTheta < min) {
				h_ind = i;
				min = absTheta;
			}
		}
		// System.out.println(h_ind);
		int v_ind = thetas[2] >= thetas[3] ? 0 : 1;
		if (h_ind / 2 == 0)
			v_ind += 2;
		// System.out.println(v_ind);
		double[][] A1 = new double[][] { new double[] { directions[0][v_ind], directions[0][h_ind], 0 }, new double[] { directions[1][v_ind], directions[1][h_ind], 0 }, new double[] { 0, 0, 1 } };

		LUDecomposition realA1 = new LUDecomposition(MatrixUtils.createRealMatrix(A1));
		if (realA1.getDeterminant() < 0)
			for (int i = 0; i < A1.length; i++)
				A1[i][0] = -A1[i][0];

		RealMatrix A = new LUDecomposition(MatrixUtils.createRealMatrix(A1)).getSolver().getInverse();
		// System.out.println(Arrays.deepToString(A.getData()));
		RealMatrix inter_matrix = A.multiply(MatrixUtils.createRealMatrix(H));
		// System.out.println(image.size());
		double[][] cords = inter_matrix.multiply(MatrixUtils.createRealMatrix(new double[][] { new double[] { 0, 0, image.cols(), image.cols() }, new double[] { 0, image.rows(), 0, image.rows() }, new double[] { 1, 1, 1, 1 } })).getData();
		cords = new double[][] { new double[] { cords[0][0] / cords[2][0], cords[0][1] / cords[2][1], cords[0][2] / cords[2][2], cords[0][3] / cords[2][3] },
				new double[] { cords[1][0] / cords[2][0], cords[1][1] / cords[2][1], cords[1][2] / cords[2][2], cords[1][3] / cords[2][3] } };
		// System.out.println(Arrays.deepToString(cords));
		double tx = Math.min(0, Math.min(Math.min(Math.min(cords[0][0], cords[0][1]), cords[0][2]), cords[0][3]));
		double ty = Math.min(0, Math.min(Math.min(Math.min(cords[1][0], cords[1][1]), cords[1][2]), cords[1][3]));
		double max_x = Math.max(Math.max(Math.max(cords[0][0], cords[0][1]), cords[0][2]), cords[0][3]) - tx;
		double max_y = Math.max(Math.max(Math.max(cords[1][0], cords[1][1]), cords[1][2]), cords[1][3]) - ty;
		if (clip) {
			int max_offset = Math.max(image.width(), image.height()) * clip_factor / 2;
			tx = Math.max(tx, -max_offset);
			ty = Math.max(ty, -max_offset);

			max_x = Math.min(max_x, -tx + max_offset);
			max_y = Math.min(max_y, -ty + max_offset);
		}
		// System.out.println(max_x + " " + max_y);
		double[][] T = new double[][] { new double[] { 1, 0, -tx }, new double[] { 0, 1, -ty }, new double[] { 0, 0, 1 } };

		double[][] final_homography = MatrixUtils.createRealMatrix(T).multiply(inter_matrix).getData();
		Mat warped_img = new Mat();
		// System.out.println(Arrays.deepToString(final_homography));
		Mat homography = new Mat(3, 3, CvType.CV_64FC1);
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 3; col++)
				homography.put(row, col, final_homography[row][col]);
		Imgproc.warpPerspective(image.getSrc(), warped_img, homography, new Size(max_x, max_y), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0));
		return new Img(warped_img, false);
	}
}