package org.genericsystem.cv;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.genericsystem.cv.utils.Line;
import org.genericsystem.cv.utils.NativeLibraryLoader;
import org.genericsystem.cv.utils.Ransac;
import org.genericsystem.cv.utils.Ransac.Model;
import org.genericsystem.cv.utils.Tools;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class LinesDetector5 extends AbstractApp {

	static {
		NativeLibraryLoader.load();
	}

	public static void main(String[] args) {
		launch(args);
	}

	private final VideoCapture capture = new VideoCapture(0);
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	// private Damper vpxDamper = new Damper(1);
	// private Damper vpyDamper = new Damper(1);

	@Override
	protected void fillGrid(GridPane mainGrid) {
		// vpxDamper.pushNewValue(0);
		// vpyDamper.pushNewValue(0);
		Mat frame = new Mat();
		capture.read(frame);

		ImageView frameView = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(frameView, 0, 0);
		ImageView deskewedView = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(deskewedView, 0, 1);
		Mat dePerspectived = frame.clone();
		timer.scheduleAtFixedRate(() -> {
			try {
				capture.read(frame);
				Img grad = new Img(frame, false).morphologyEx(Imgproc.MORPH_GRADIENT, Imgproc.MORPH_RECT, new Size(2, 2)).otsu();
				// Img grad = new Img(frame, false).canny(60, 180);
				// Img grad = new Img(frame, false).bilateralFilter(20, 80, 80).bgr2Gray().adaptativeThresHold(255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 3).morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_RECT, new Size(11,
				// 3));
				Lines lines = new Lines(grad.houghLinesP(1, Math.PI / 180, 10, 100, 10));
				System.out.println("Average angle: " + lines.getMean() / Math.PI * 180);
				if (lines.size() > 10) {
					lines.draw(frame, new Scalar(0, 0, 255));

					frameView.setImage(Tools.mat2jfxImage(frame));
					// Mat dePerspectived = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
					Ransac<Line> ransac = lines.vanishingPointRansac(frame.width(), frame.height());
//					Mat vp_mat = (Mat) ransac.getBestModel().getParams()[0];
					Matrix vp_matrix =  (Matrix) ransac.getBestModel().getParams()[0];
					Mat vp_mat = vp_matrix.convert();
					Point vp = new Point(vp_mat.get(0, 0)[0], vp_mat.get(1, 0)[0]);
					// vpxDamper.pushNewValue(vp.x);
					// vpyDamper.pushNewValue(vp.y);
					Point bary = new Point(frame.width() / 2, frame.height() / 2);
					Mat homography = findHomography(new Point(vp.x, vp.y), bary, frame.width(), frame.height());
					lines = Lines.of(ransac.getBestDataSet().values());
					lines = Lines.of(lines.perspectivTransform(homography));

					Mat mask = new Mat(frame.size(), CvType.CV_8UC1, new Scalar(255));
					Mat maskWarpped = new Mat();
					Imgproc.warpPerspective(mask, maskWarpped, homography, frame.size());
					Mat tmp = new Mat();
					Imgproc.warpPerspective(frame, tmp, homography, frame.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, Scalar.all(255));
					tmp.copyTo(dePerspectived, maskWarpped);
					lines.draw(dePerspectived, new Scalar(0, 255, 0));
					deskewedView.setImage(Tools.mat2jfxImage(dePerspectived));

				} else
					System.out.println("Not enough lines : " + lines.size());

			} catch (Throwable e) {
				e.printStackTrace();
			}

		}, 33, 250, TimeUnit.MILLISECONDS);

	}

	public void print(Mat m) {
		for (int row = 0; row < m.rows(); row++) {
			System.out.print("(");
			for (int col = 0; col < m.cols() - 1; col++) {
				System.out.print(m.get(row, col)[0] + ",");
			}
			System.out.println(m.get(row, m.cols() - 1)[0] + ")");
		}
		System.out.println("---------------");

	}

	public Point[] rotate(Point bary, double alpha, Point... p) {
		Mat matrix = Imgproc.getRotationMatrix2D(bary, alpha / Math.PI * 180, 1);
		MatOfPoint2f results = new MatOfPoint2f();
		Core.transform(new MatOfPoint2f(p), results, matrix);
		return results.toArray();
	}

	public Point center(Point a, Point b) {
		return new Point((a.x + b.x) / 2, (a.y + b.y) / 2);
	}

	private Mat findHomography(Point vp, Point bary, double width, double height) {

		double alpha_ = Math.atan2((vp.y - bary.y), (vp.x - bary.x));
		if (alpha_ < -Math.PI / 2 && alpha_ > -Math.PI)
			alpha_ = alpha_ + Math.PI;
		if (alpha_ < Math.PI && alpha_ > Math.PI / 2)
			alpha_ = alpha_ - Math.PI;
		double alpha = alpha_;

		Point rotatedVp = rotate(bary, alpha, vp)[0];

		Point A = new Point(0, 0);
		Point B = new Point(width, 0);
		Point C = new Point(width, height);
		Point D = new Point(0, height);

		Point AB2 = new Point(width / 2, 0);
		Point CD2 = new Point(width / 2, height);

		Point A_, B_, C_, D_;
		if (rotatedVp.x >= width / 2) {
			A_ = new Line(AB2, rotatedVp).intersection(0);
			D_ = new Line(CD2, rotatedVp).intersection(0);
			C_ = new Line(A_, bary).intersection(new Line(CD2, rotatedVp));
			B_ = new Line(D_, bary).intersection(new Line(AB2, rotatedVp));
		} else {
			B_ = new Line(AB2, rotatedVp).intersection(width);
			C_ = new Line(CD2, rotatedVp).intersection(width);
			A_ = new Line(C_, bary).intersection(new Line(AB2, rotatedVp));
			D_ = new Line(B_, bary).intersection(new Line(CD2, rotatedVp));
		}

		System.out.println("vp : " + vp);
		System.out.println("rotated vp : " + rotatedVp);
		System.out.println("Alpha : " + alpha * 180 / Math.PI);
		// System.out.println("A : " + A + " " + A_);
		// System.out.println("B : " + B + " " + B_);
		// System.out.println("C : " + C + " " + C_);
		// System.out.println("D : " + D + " " + D_);

		return Imgproc.getPerspectiveTransform(new MatOfPoint2f(rotate(bary, -alpha, A_, B_, C_, D_)), new MatOfPoint2f(A, B, C, D));
	}

	public static class Lines extends org.genericsystem.cv.utils.Lines {

		private static Matrix K;

		public Lines(Mat src) {
			super(src);
		}

		public Lines(Collection<Line> lines) {
			super(lines);
		}

		public static Lines of(Collection<Line> lines) {
			return new Lines(lines);
		}

		private Matrix getLineMat(Line line) {

			Matrix a = new Matrix(3,1);
			Matrix b = new Matrix(3,1);
			a.set(0, 0, line.getX1());		
			a.set(1, 0, line.getY1());
			a.set(2, 0, 1d);
			b.set(0, 0, line.getX2());
			b.set(1, 0, line.getY2());
			b.set(2, 0, 1d);
			Matrix an = K.inv().times(a, 1);
			Matrix bn = K.inv().times(b, 1);
			Matrix li = Matrix.crossProduct(an ,bn);			
			return li.normalize();
			
		}


		// @SuppressWarnings({ "rawtypes", "unchecked" })
		public Ransac<Line> vanishingPointRansac(int width, int height) {
			int minimal_sample_set_dimension = 2;
			double maxError = (float) 0.01623 * 2;
			if (K == null) {
				K = new Matrix(3, 3);
				K.set(0, 0,  width );
				K.set(0, 2, width / 2);
				K.set(1, 1,  height);
				K.set(1, 2, height / 2 );
				K.set(2, 2, 1 );
			}
			return new Ransac<>(getLines(), getModelProvider(minimal_sample_set_dimension, maxError), minimal_sample_set_dimension, 100, maxError, Double.valueOf(Math.floor(this.size() * 0.7)).intValue());
		}

		private Function<Collection<Line>, Model<Line>> getModelProvider(int minimal_sample_set_dimension, double maxError) {
			return datas -> {
				Matrix[] vp = {new Matrix()};

				if (datas.size() == minimal_sample_set_dimension) {
					Iterator<Line> it = datas.iterator();
					vp[0] = Matrix.crossProduct(getLineMat(it.next()),getLineMat(it.next())).normalize();
				
				} else {
					// Extract the line segments corresponding to the indexes contained in the set
					Matrix li_set = new Matrix(3, datas.size());
					Matrix tau = new Matrix(datas.size(), datas.size());

					int i = 0;
					for (Line line : datas) {
						Matrix li = getLineMat(line);
						li_set.set(0, i, li.get(0, 0));
						li_set.set(1, i, li.get(1, 0));
						li_set.set(2, i, li.get(2, 0));
						tau.set(i, i, line.size());
						i++;
					}

				//	Mat dst = product(li_set, tau.t(), 1);
					Matrix dst = li_set.times(tau.t(), 1);
					dst = dst.times(tau, 1);
					Matrix ATA = dst.times(li_set.t(), 1);

					// Obtain eigendecomposition
					Mat v = new Mat();
					Mat ata = ATA.convert();
					Core.SVDecomp(ata, new Mat(), v, new Mat());
					Matrix result = Matrix.convert(v);
					// Check eigenvecs after SVDecomp
					if (v.rows() < 3)
						throw new IllegalStateException();

					// Assign the result (the last column of v, corresponding to the eigenvector with lowest eigenvalue)
					vp[0] = new Matrix(3, 1);
					vp[0].set(0, 0, result.get(0, 2));
					vp[0].set(1, 0, result.get(1, 2));
					vp[0].set(2, 0, result.get(2, 2));

					vp[0].normalize();
					
					//Core.normalize(vp, vp);
					vp[0] = K.times(vp[0], 1);
					//Core.gemm(K, vp, 1, new Mat(), 0, vp);

					if (vp[0].get(2, 0) != 0) {
						vp[0].set(0, 0, vp[0].get(0, 0)/ vp[0].get(2, 0));
						vp[0].set(1, 0, vp[0].get(1, 0) / vp[0].get(2, 0));
						vp[0].set(2, 0, 1d);
					} else {
						// Since this is infinite, it is better to leave it calibrated
						//Core.gemm(K.inv(), vp, 1, new Mat(), 0, vp);
						vp[0] = K.times(vp[0], 1);
					}

				}

				return new Model<Line>() {
					@Override
					public double computeError(Line line) {
						Matrix lineMat = getLineMat(line);
						double di = vp[0].dot(lineMat);
//						di /= (Core.norm(vp) * Core.norm(lineMat));
						di /= (vp[0].norm() * lineMat.norm());
						return di * di;
					}

					@Override
					public double computeGlobalError(List<Line> datas, Collection<Line> consensusDatas) {
						double globalError = 0;
						for (Line line : datas) {
							double error = computeError(line);
							if (error > maxError)
								error = maxError;
							globalError += error;
						}
						globalError = globalError / datas.size();
						return globalError;
					}

					@Override
					public Object[] getParams() {
						return new Object[] { vp[0] };
					}

				};
			};
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		timer.shutdown();
		timer.awaitTermination(5000, TimeUnit.MILLISECONDS);
		capture.release();
	}

}
