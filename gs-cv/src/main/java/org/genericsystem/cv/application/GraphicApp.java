package org.genericsystem.cv.application;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.genericsystem.cv.AbstractApp;
import org.genericsystem.cv.Calibrated.AngleCalibrated;
import org.genericsystem.cv.Lines;
import org.genericsystem.cv.utils.NativeLibraryLoader;
import org.genericsystem.layout.Layout;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class GraphicApp extends AbstractApp {

	private final double f = 6.053 / 0.009;
	private final GSCapture gsCapture = new GSVideoCapture(0, f, GSVideoCapture.HD, GSVideoCapture.VGA);
	// private final GSCapture gsCapture = new GSPhotoCapture("resources/image.pdf", f);
	private SuperFrameImg superFrame;
	private ReferenceManager referenceManager;
	private Config config = new Config();
	private Deperspectiver deperspectiver;
	private ScheduledExecutorService timer = new BoundedScheduledThreadPoolExecutor();
	ImageView[][] imageViews = new ImageView[][] { new ImageView[2], new ImageView[2], new ImageView[2] };

	public static void main(String[] args) {
		launch(args);
	}

	static {
		NativeLibraryLoader.load();
	}

	public GraphicApp() {
		superFrame = gsCapture.read();
		deperspectiver = new Deperspectiver(f, superFrame.getPp());
		referenceManager = new ReferenceManager(gsCapture.getResize());
	}

	@Override
	protected void fillGrid(GridPane mainGrid) {
		double displaySizeReduction = 1;
		for (int col = 0; col < imageViews.length; col++)
			for (int row = 0; row < imageViews[col].length; row++) {
				ImageView imageView = new ImageView();
				imageViews[col][row] = imageView;
				mainGrid.add(imageViews[col][row], col, row);
				imageView.setFitWidth(superFrame.width() / displaySizeReduction);
				imageView.setFitHeight(superFrame.height() / displaySizeReduction);
			}
		startTimer();
	}

	private void startTimer() {
		timer.scheduleAtFixedRate(() -> {
			try {
				Image[] images = doWork();
				if (images != null)
					Platform.runLater(() -> {
						Iterator<Image> it = Arrays.asList(images).iterator();
						for (int row = 0; row < imageViews.length; row++)
							for (int col = 0; col < imageViews[row].length; col++)
								imageViews[row][col].setImage(it.next());
					});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, 30, 30, TimeUnit.MILLISECONDS);
	}

	private Image[] doWork() {

		System.out.println("do work");
		if (!config.stabilizedMode)
			superFrame = gsCapture.read();
		Image[] images = new Image[6];
		Lines lines = superFrame.detectLines();
		AngleCalibrated[] calibratedVps = deperspectiver.computeCalibratedVps(superFrame, config.textsEnabledMode, lines);
		if (calibratedVps == null)
			return null;
		Mat deperspectiveHomography = deperspectiver.findHomography(superFrame, calibratedVps);
		if (deperspectiveHomography == null)
			return null;

		superFrame.draw(lines, new Scalar(0, 0, 255), 1);
		superFrame.drawVanishingPointLines(lines, calibratedVps[0], new Scalar(0, 255, 0), 1);
		superFrame.drawVanishingPointLines(lines, calibratedVps[1], new Scalar(255, 0, 0), 1);
		superFrame.drawVpsArrows(calibratedVps, new double[] { 20, 20 }, new Scalar(0, 255, 0), 2);
		images[0] = superFrame.getDisplay().toJfxImage();

		SuperTemplate superDeperspectived = superFrame.deperspective(deperspectiveHomography);
		images[1] = superDeperspectived.getDiffFrame().toJfxImage();

		List<Rect> detectedRects = superDeperspectived.detectRects();
		superDeperspectived.drawRects(detectedRects, new Scalar(0, 255, 0), -1);

		SuperTemplate surfaceTemplate = new SuperTemplate(new SuperFrameImg(superDeperspectived.getDisplay().bgr2Gray().getSrc(), superFrame.getPp(), f), CvType.CV_8UC1, SuperFrameImg::getDisplay);
		Layout surfaceLayout = surfaceTemplate.layout();
		double surface = surfaceLayout.computeTotalSurface(surfaceTemplate.getFrame());
		superDeperspectived.putText(String.valueOf(surface));

		images[2] = superDeperspectived.getDisplay().toJfxImage();		

		ImgDescriptor newImgDescriptor = new ImgDescriptor(superDeperspectived, surface);
		if (newImgDescriptor.getDescriptors().empty()) {
			System.out.println("Empty descriptors");
			return null;
		}
		referenceManager.submit(newImgDescriptor, detectedRects);
		List<Rect> referenceRects = referenceManager.getReferenceRects();
		SuperTemplate referenceTemplate = new SuperTemplate(referenceManager.getReference().getSuperFrame(), CvType.CV_8UC1, SuperFrameImg::getFrame);
		referenceTemplate.drawRects(referenceRects, new Scalar(255), -1);
		images[3] = referenceTemplate.getDisplay().toJfxImage();

		List<Point> detectedCenroids = superDeperspectived.detectCentroids();
		SuperTemplate superReferenceTemplate = new SuperTemplate(superDeperspectived, CvType.CV_8UC1, SuperFrameImg::getFrame);
		superReferenceTemplate.drawCentroids(detectedCenroids, new Scalar(255), -1, 2);
		new Lines(superReferenceTemplate.getDisplay().houghLinesP(1, Math.PI / 180, 10, 50, 47)).filter(line -> Math.atan2(Math.abs(line.y2 - line.y1), Math.abs(line.x2 - line.x1)) < 5 * Math.PI / 180).draw(superReferenceTemplate.getDisplay().getSrc(),
				new Scalar(255), 1);
		images[5] = superReferenceTemplate.getDisplay().toJfxImage();

		// SuperTemplate superReferenceTemplate = new SuperTemplate(superFrame, CvType.CV_8UC1, SuperFrameImg::getFrame);
		// superReferenceTemplate.drawRects(referenceManager.getResizedReferenceRects(), new Scalar(255), -1);
		// images[5] = superReferenceTemplate.getDisplay().toJfxImage();

		SuperTemplate layoutTemplate = new SuperTemplate(referenceTemplate, CvType.CV_8UC3, SuperFrameImg::getDisplay);
		Layout layout = layoutTemplate.layout();
		layoutTemplate.drawLayout(layout);
		images[4] = layoutTemplate.getDisplay().toJfxImage();

		return images;
	}

	@Override
	protected void onS() {
		config.stabilizedMode = !config.stabilizedMode;
	}

	@Override
	protected void onSpace() {
		if (config.isOn)
			timer.shutdown();
		else {
			timer = new BoundedScheduledThreadPoolExecutor();
			startTimer();
		}
		config.isOn = !config.isOn;
	}

	@Override
	protected void onR() {
		timer.schedule(() -> referenceManager.clear(), 0, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void onT() {
		config.textsEnabledMode = !config.textsEnabledMode;
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		timer.shutdown();
		timer.awaitTermination(5000, TimeUnit.MILLISECONDS);
		gsCapture.release();
	}
}
