package org.genericsystem.cv;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class LinesDetector extends AbstractApp {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) {
		launch(args);
	}

	private final VideoCapture capture = new VideoCapture(0);
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

	@Override
	protected void fillGrid(GridPane mainGrid) {
		Mat frame = new Mat();
		capture.read(frame);

		ImageView canny = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(canny, 0, 0);
		ImageView src = new ImageView(Tools.mat2jfxImage(frame));
		mainGrid.add(src, 0, 1);
		double sigma = 0.33;
		timer.scheduleAtFixedRate(() -> {
			capture.read(frame);

			Img grad = new Img(frame).morphologyEx(Imgproc.MORPH_GRADIENT, Imgproc.MORPH_RECT, new Size(2, 2)).otsu();

			Img gray = new Img(frame).gray();
			// Img blured = gray.gaussianBlur(new Size(3, 3));

			MatOfDouble mu = new MatOfDouble();
			Core.meanStdDev(gray.getSrc(), mu, new MatOfDouble());
			double median = mu.get(0, 0)[0];
			double lower = Math.max(0, (1.0 - sigma) * median);
			double upper = Math.min(255, (1.0 + sigma) * median);
			Img edges = gray.canny(lower, upper);
			Imgproc.dilate(edges.getSrc(), edges.getSrc(), new Mat());

			Img lines = edges.houghLinesP(1, Math.PI / 180, 50, 100, 10);
			double angle = 0.;
			for (int i = 0; i < lines.rows(); i++) {
				double[] val = lines.get(i, 0);
				Imgproc.line(frame, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 1);
				angle += Math.atan2(val[3] - val[1], val[2] - val[0]);
			}
			angle /= lines.rows(); // mean angle, in radians.

			canny.setImage(Tools.mat2jfxImage(edges.getSrc()));
			src.setImage(Tools.mat2jfxImage(grad.getSrc()));
			System.out.println("Average angle: " + Math.round(angle / Math.PI * 180));
		}, 0, 33, TimeUnit.MILLISECONDS);

	}

	@Override
	public void stop() throws Exception {
		timer.shutdown();
		capture.release();
		super.stop();
	}

}
