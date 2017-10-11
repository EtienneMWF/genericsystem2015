package org.genericsystem.cv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.genericsystem.cv.LinesDetector.Damper;
import org.genericsystem.cv.utils.NativeLibraryLoader;
import org.genericsystem.cv.utils.Tools;
import org.genericsystem.layout.Ransac;
import org.genericsystem.layout.Ransac.Model;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;

import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class LinesDetector2 extends AbstractApp {

	static {
		NativeLibraryLoader.load();
	}

	public static void main(String[] args) {
		launch(args);
	}

	private final VideoCapture capture = new VideoCapture(0);
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
	private Damper damper = new Damper(10);

	@Override
	protected void fillGrid(GridPane mainGrid) {
		damper.pushNewValue(0);
		Mat frame = new Mat();
		capture.read(frame);

		ImageView frameView = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(frameView, 0, 0);
		ImageView deskiewedView = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(deskiewedView, 0, 1);
		timer.scheduleAtFixedRate(() -> {
			try {
				capture.read(frame);
				Img grad = new Img(frame, false).morphologyEx(Imgproc.MORPH_GRADIENT, Imgproc.MORPH_ELLIPSE, new Size(2, 2)).otsu().morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_ELLIPSE, new Size(7, 7));
				Lines lines = new Lines(grad.houghLinesP(1, Math.PI / 180, 100, 200, 20));
				System.out.println("Average angle: " + lines.getMean() / Math.PI * 180);
				if (lines.size() > 10) {
					lines.draw(frame, new Scalar(0, 0, 255));
					Ransac<Line> ransac = lines.vanishingPointRansac(frame.width(), frame.height());
					lines = new Lines(ransac.getBestDataSet().values());
					lines.draw(frame, new Scalar(0, 255, 0));
					frameView.setImage(Tools.mat2jfxImage(frame));
					Mat dePerspectived = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
					Mat homography = (Mat) ransac.getBestModel().getParams()[0];
					System.out.println("Ransac angle : " + ((double) ransac.getBestModel().getParams()[1]) / Math.PI * 180);
					Mat mask = new Mat(frame.size(), CvType.CV_8UC1, new Scalar(255));
					Mat maskWarpped = new Mat();
					Imgproc.warpPerspective(mask, maskWarpped, homography, frame.size());
					Mat tmp = new Mat();
					Imgproc.warpPerspective(frame, tmp, homography, frame.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, Scalar.all(255));
					tmp.copyTo(dePerspectived, maskWarpped);
					deskiewedView.setImage(Tools.mat2jfxImage(dePerspectived));

				} else
					System.out.println("Not enough lines : " + lines.size());

			} catch (Exception e) {
				e.printStackTrace();
			}

		}, 0, 33, TimeUnit.MILLISECONDS);

	}

	public static class Lines {

		private final List<Line> lines = new ArrayList<>();
		private final double mean;

		public Lines(Mat src) {
			double mean = 0;
			for (int i = 0; i < src.rows(); i++) {
				double[] val = src.get(i, 0);
				Line line = new Line(val[0], val[1], val[2], val[3]);
				lines.add(line);
				mean += line.getAngle();
			}
			this.mean = mean / src.rows();
		}

		public Ransac<Line> vanishingPointRansac(int width, int height) {
			Function<Collection<Line>, Model<Line>> modelProvider = datas -> {
				Iterator<Line> it = datas.iterator();
				Line line = it.next();
				if (datas.size() > 2)
					throw new IllegalStateException("" + datas.size());
				double a = (line.y2 - line.y1) / (line.x2 - line.x1);
				double b = (line.y1 + line.y2 - a * (line.x1 + line.x2)) / 2;

				Line line2 = it.next();

				double a2 = (line2.y2 - line2.y1) / (line2.x2 - line2.x1);
				double b2 = (line2.y1 + line2.y2 - a * (line2.x1 + line2.x2)) / 2;

				double vpx = (b2 - b) / (a - a2);
				double vpy = a * vpx + b;
				double alpha = ((vpy - height / 2) / (vpx - width / 2));
				// System.out.println("alpha : " + alpha / Math.PI * 180);
				Mat matrix = Imgproc.getRotationMatrix2D(new Point(width / 2, height / 2), alpha, 1);
				Mat points = Converters.vector_Point2f_to_Mat(Arrays.asList(new Point(line.x1, line.y1), new Point(line.x2, line.y2), new Point(line2.x2, line2.y2), new Point(line2.x1, line2.y1)));
				MatOfPoint2f results = new MatOfPoint2f();
				Core.transform(points, results, matrix);
				Point[] targets = results.toArray();
				a = (targets[1].y - targets[0].y) / (targets[1].x - targets[0].x);
				b = (targets[0].y + targets[1].y - a * (targets[0].x + targets[1].x)) / 2;
				double newy1 = a * width / 2 + b;

				targets[0] = new Point(targets[0].x, newy1);
				targets[1] = new Point(targets[1].x, newy1);
				a = (targets[2].y - targets[3].y) / (targets[2].x - targets[3].x);
				b = (targets[3].y + targets[2].y - a * (targets[3].x + targets[2].x)) / 2;
				double newy2 = a * width / 2 + b;

				targets[2] = new Point(targets[2].x, newy2);
				targets[3] = new Point(targets[3].x, newy2);

				Mat homography = Imgproc.getPerspectiveTransform(points, new MatOfPoint2f(targets));

				return new Model<Line>() {

					@Override
					public double computeError(Line line) {
						return Math.abs(line.perspectivTransform(homography).getAngle());
					}

					@Override
					public double computeGlobalError(Collection<Line> datas) {
						double error = 0;
						for (Line data : datas)
							error += Math.pow(computeError(data), 2);
						return error;
					}

					@Override
					public Object[] getParams() {
						return new Object[] { homography, alpha };
					}

				};
			};

			Ransac<Line> ransac = new Ransac<>(lines, modelProvider, 2, 200, 0.5 * Math.PI / 180, Double.valueOf(Math.floor(lines.size() * 0.5)).intValue());
			ransac.compute(false);
			return ransac;
		}

		public Lines rotate(Mat matrix) {
			return new Lines(lines.stream().map(line -> line.transform(matrix)).collect(Collectors.toList()));
		}

		public Lines perspectivTransform(Mat matrix) {
			return new Lines(lines.stream().map(line -> line.perspectivTransform(matrix)).collect(Collectors.toList()));
		}

		public void draw(Mat frame, Scalar color) {
			lines.forEach(line -> line.draw(frame, color));
		}

		public Lines(Collection<Line> lines) {
			double mean = 0;
			for (Line line : lines) {
				this.lines.add(line);
				mean += line.getAngle();
			}
			this.mean = mean / lines.size();

		}

		public int size() {
			return lines.size();
		}

		public double getMean() {
			return mean;
		}

	}

	public static class Line {
		private final double x1, y1, x2, y2, angle;

		public Line(double x1, double y1, double x2, double y2) {
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
			this.angle = Math.atan2(y2 - y1, x2 - x1);
		}

		public Line transform(Mat rotationMatrix) {
			MatOfPoint2f results = new MatOfPoint2f();
			Core.transform(Converters.vector_Point2f_to_Mat(Arrays.asList(new Point(x1, y1), new Point(x2, y2))), results, rotationMatrix);
			Point[] targets = results.toArray();
			return new Line(targets[0].x, targets[0].y, targets[1].x, targets[1].y);
		}

		public Line perspectivTransform(Mat homography) {
			MatOfPoint2f results = new MatOfPoint2f();
			Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(Arrays.asList(new Point(x1, y1), new Point(x2, y2))), results, homography);
			Point[] targets = results.toArray();
			return new Line(targets[0].x, targets[0].y, targets[1].x, targets[1].y);
		}

		public void draw(Mat frame, Scalar color) {
			Imgproc.line(frame, new Point(x1, y1), new Point(x2, y2), color, 1);
		}

		@Override
		public String toString() {
			return "Line : " + angle;
		}

		public double getAngle() {
			return angle;
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
