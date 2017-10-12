package org.genericsystem.cv.utils;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.io.FilenameUtils;
import org.genericsystem.cv.Img;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains static methods that can be used to deskew an {@link Img}.
 * 
 * @author Nicolas Feybesse
 * @author Pierrik Lassalas
 */
public class Deskewer {

	static {
		NativeLibraryLoader.load();
	}

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final double closedImgSizeFactor = 2E-6;
	private static final double minAreaFactor = 3E-5;

	// Only for testing purposes
	public static void main(String[] args) {
		final String filename = System.getenv("HOME") + "/genericsystem/gs-ir-files/converted-png/image-4.png";
		Path imgPath = Paths.get(filename);
		Path temp = deskewAndSave(imgPath);
		System.out.println(temp);
	}

	/**
	 * Deskew an image, and save it in the same folder as the original image.
	 * 
	 * @param imgPath - the Path to the image
	 * @return the path of the newly saved image
	 */
	public static Path deskewAndSave(final Path imgPath) {
		final String ext = FilenameUtils.getExtension(imgPath.getFileName().toString());
		final String filename = imgPath.toString().replace("." + ext, "") + "_deskewed." + ext;
		// TODO: save to a child folder containing only deskewed images?
		Path savedPath = imgPath.resolveSibling(filename);

		Img img = deskew(imgPath);
		try {
			synchronized (Deskewer.class) {
				if (savedPath.toFile().exists()) {
					String[] fileNameParts = savedPath.getFileName().toString().split("\\.(?=[^\\.]+$)");
					savedPath = File.createTempFile(fileNameParts[0] + "-", "." + fileNameParts[1], imgPath.getParent().toFile()).toPath();
				}
			}
			Imgcodecs.imwrite(savedPath.toString(), img.getSrc());
			return savedPath;
		} catch (IOException e) {
			logger.error("An error has occured while saving file " + savedPath.toString(), e);
			return null;
		} finally {
			if (null != img)
				img.close();
		}
	}

	/**
	 * Deskew an image.
	 * 
	 * @param imgPath - the path to the image
	 * @return a new {@link Img}
	 */
	public static Img deskew(final Path imgPath) {
		if (!imgPath.toFile().exists())
			throw new IllegalStateException("No files were found at Path " + imgPath);
		Img img = new Img(imgPath.toString());
		Img deskewed = _deskew(img);
		img.close();
		return deskewed;
	}

	/**
	 * Draw the Rotated rectangles used to calculate the deskew angle.
	 * 
	 * @param img - the source image
	 * @param scalar - the color used to draw the rectangles
	 * @param thickness - the thickness
	 * @return - an annotated Img
	 */
	public static Img getRotatedRectanglesDrawn(final Img img, Scalar scalar, int thickness) {
		Img imgCopy = new Img(img.getSrc(), true);
		Img closed = getClosedImg(imgCopy);
		List<RotatedRect> rectangles = getRotatedRects(closed.getSrc());
		rectangles.stream().forEach(rect -> drawSingleRotatedRectangle(imgCopy.getSrc(), rect, scalar, thickness));
		List<RotatedRect> filteredRectangles = getInliers(rectangles, 1.0);
		filteredRectangles.stream().forEach(rect -> drawSingleRotatedRectangle(imgCopy.getSrc(), rect, new Scalar(0, 255, 0), thickness));
		closed.close();
		return imgCopy;
	}

	/**
	 * Get the binary image used to compute the deskew angle.
	 * 
	 * @param img - the source image
	 * @return a binary image
	 */
	public static Img getBinary(final Img img) {
		return getClosedImg(img);
	}

	// This function modifies the Mat mat
	private static void drawSingleRotatedRectangle(Mat mat, final RotatedRect rect, final Scalar scalar, final int thickness) {
		Point points[] = new Point[4];
		rect.points(points);
		for (int i = 0; i < 4; ++i) {
			Imgproc.line(mat, points[i], points[(i + 1) % 4], scalar, thickness);
		}
	}

	private static Img _deskew(final Img img) {
		final Img closed = getClosedImg(img);
		final double angle = contoursDetection(closed.getSrc());
		System.out.println("angle: " + angle);

		final Point center = new Point(img.width() / 2, img.height() / 2);
		// Rotation matrix
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1);

		// Get the bounding rectangle
		Rect bbox = new RotatedRect(center, img.size(), angle).boundingRect();
		// Adjust the transformation matrix to prevent image cropping
		double[] array = rotationMatrix.get(0, 2);
		array[0] += bbox.width / 2 - center.x;
		rotationMatrix.put(0, 2, array);
		array = rotationMatrix.get(1, 2);
		array[0] += bbox.height / 2 - center.y;
		rotationMatrix.put(1, 2, array);

		// Rotated Mat and empty Mat to apply the mask
		Mat rotated = new Mat(bbox.size(), CvType.CV_8UC3, Scalar.all(255));
		Mat rotatedMasked = new Mat();
		// New mask
		Mat mask = new Mat(img.size(), CvType.CV_8UC1, new Scalar(255));
		Mat warpedMask = new Mat();
		// Compute the rotation for the mask and the image
		Imgproc.warpAffine(mask, warpedMask, rotationMatrix, bbox.size());
		Imgproc.warpAffine(img.getSrc(), rotatedMasked, rotationMatrix, bbox.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, Scalar.all(255));
		// Apply the mask to the rotated Mat
		rotatedMasked.copyTo(rotated, warpedMask);
		// Release the matrices before return
		rotatedMasked.release();
		mask.release();
		warpedMask.release();
		rotationMatrix.release();
		closed.close();
		return new Img(rotated, false);
	}

	private static Img getClosedImg(final Img img) {
		double size = (closedImgSizeFactor * img.size().area());
		// Round the size factor to the nearest odd int
		size = 2 * (Math.floor(size / 2)) + 1;
		// return img.bilateralFilter(20, 80, 80).adaptativeGaussianInvThreshold(17, 9).morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_ELLIPSE, new Size(closedImgSizeFactor, closedImgSizeFactor));
		return img.bilateralFilter(20, 80, 80).bgr2Gray().grad(2.0d, 2.0d).thresHold(0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU).bitwise_not().morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_ELLIPSE, new Size(size, size));
	}

	private static double contoursDetection(final Mat dilated) {
		List<RotatedRect> rotatedRects = getRotatedRects(dilated);
		for (RotatedRect rotatedRect : rotatedRects) {
			if (rotatedRect.angle <= -45.0) {
				rotatedRect.angle += 90.0;
				double tmp = rotatedRect.size.width;
				rotatedRect.size.width = rotatedRect.size.height;
				rotatedRect.size.height = tmp;
			}
		}
		return getInliers(rotatedRects, 1.0).stream().mapToDouble(i -> i.angle).average().getAsDouble();
	}

	private static List<RotatedRect> getInliers(final List<RotatedRect> data, final double confidence) {
		if (null == data)
			return null;

		double average = data.stream().mapToDouble(rect -> rect.angle).average().getAsDouble();
		double sd = Math.sqrt(data.stream().mapToDouble(rect -> Math.pow(rect.angle - average, 2)).average().getAsDouble());
		Collections.sort(data, (r1, r2) -> Double.compare(r1.angle, r2.angle));
		int middle = data.size() / 2;
		double median;
		if (middle % 2 == 1)
			median = data.get(middle).angle;
		else
			median = DoubleStream.of(data.get(middle).angle, data.get(middle - 1).angle).average().getAsDouble();

		List<RotatedRect> result = data.stream().filter(rect -> Math.abs(rect.angle - median) < confidence * sd).collect(Collectors.toList());
		// List<RotatedRect> result = data.stream().filter(rect -> Math.abs(rect.angle - average) < confidence * sd).collect(Collectors.toList());
		// List<RotatedRect> result = data.stream().filter(rect -> Math.abs(rect.angle - average) < 5).collect(Collectors.toList());
		return result;
	}

	private static List<RotatedRect> getRotatedRects(final Mat dilated) {
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(dilated, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		double minArea = minAreaFactor * dilated.size().area();
		List<RotatedRect> rotatedRects = contours.stream().filter(contour -> Imgproc.contourArea(contour) > minArea).map(contour -> Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()))).collect(Collectors.toList());
		return rotatedRects;
	}
}