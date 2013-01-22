package com.server;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.entity.KeyPoint;
import com.entity.Mat;
import com.entity.TargetImage;
import com.utils.Config;
import com.utils.ConvertValue;
import com.utils.Util;

/**
 * Bridge between the server written in Java and the matching engine written in
 * C++. This class assigns job to the matching engine and passes the result back
 * to the server
 * 
 * @author hoangtung
 * 
 */
@SuppressWarnings("unchecked")
public class Matcher {
	private static final float WRONG_THRESHOLD = 0.01f;
	private static Process p;
	private static Scanner in;
	private static Vector<Integer> IDs;

	/**
	 * Fetch data from database
	 */
	static synchronized void fetch() {
		IDs = new Vector<Integer>();
		Vector<TargetImage> bs = new Vector<TargetImage>();
		try {
			Class.forName(Config.getDriverString()).newInstance();
			System.out.println("Driver Info:" + Config.getDBUrl() + ", "
					+ Config.getUser() + ", " + Config.getPass());
			Connection con = DriverManager.getConnection(Config.getDBUrl(),
					Config.getUser(), Config.getPass());

			PreparedStatement ps = con
					.prepareStatement("select id, _keypoint, _descriptor, _width, _height from targetimage");
			ResultSet rs = ps.executeQuery();
			int count = 0;
			while (rs.next()) {
				if (count < 3) {

					count++;
				}
				bs.add(new TargetImage(rs.getInt(1), null, null, null, 0, 0,
						null, (Vector<KeyPoint>) Util.objectFromByteArray(rs
								.getBytes(2)), (Mat) Util
								.objectFromByteArray(rs.getBytes(3)), rs
								.getInt(4), rs.getInt(5)));

				IDs.add(rs.getInt(1));
			}
			// System.out.println("Size: " + bs.size());
			writeData(bs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write fetched keypoints and descriptors to file
	 * 
	 * @param b
	 */
	private synchronized static void writeData(Vector<TargetImage> b) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("Data.txt"));
			int dataSize = 1;
			Iterator<TargetImage> it = b.iterator();
			int size = b.size();
			for (int i = 0; i < size; ++i) {
				TargetImage temp = it.next();
				// System.out.println("Size:" + temp.dess);
				dataSize += temp.dess.rows * temp.dess.cols + 3
						+ temp.keys.size() * 7 + 1;
			}
			bw.write(dataSize + " ");
			bw.write(size + " ");
			it = b.iterator();
			for (int i = 0; i < size; ++i) {
				TargetImage temp = it.next();
				bw.write(temp.width + " ");
				bw.write(temp.height + " ");
				int kSize = temp.keys.size();
				bw.write(kSize + " ");
				for (int j = 0; j < kSize; ++j) {
					writeKey(bw, temp.keys.get(j));
				}
				writeDes(bw, temp.dess);
			}
			bw.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Write a KeyPoint to file
	 * 
	 * @param bw
	 * @param k
	 * @throws IOException
	 */
	private synchronized static void writeKey(BufferedWriter bw, KeyPoint k)
			throws IOException {
		bw.write(k.angle + " ");
		bw.write(k.classId + " ");
		bw.write(k.octave + " ");
		bw.write(k.x + " ");
		bw.write(k.y + " ");
		bw.write(k.response + " ");
		bw.write(k.size + " ");
	}

	/**
	 * Write a Mat to file
	 * 
	 * @param bw
	 * @param k
	 * @throws IOException
	 */
	private synchronized static void writeDes(BufferedWriter bw, Mat k)
			throws IOException {
		// System.out.println("R: " + k.cols + " : " + k.rows);
		bw.write(k.rows + " ");
		bw.write(k.cols + " ");
		bw.write(k.type + " ");
		int size = k.rows * k.cols;
		for (int i = 0; i < size; ++i) {
			bw.write(k.data[i] + " ");
		}
	}

	/**
	 * Get the matching subprocess
	 * 
	 * @return the matching process
	 */
	public synchronized static Process getProcess() {
		return p;
	}

	public native static int[] recognition(String path);

	/**
	 * Compare an image to database images to find out the most similar ones.
	 * 
	 * @param image
	 * @return ids of the most similar images
	 */
	public synchronized static Vector<Integer> match(String image) {
		BufferedImage img = ConvertValue.base64StringToBitmap(image);
		Vector<Integer> ids = new Vector<Integer>();
		String filename = System.currentTimeMillis() + ".jpg";

		try { // write the image to a file which will be the input for the
				// matching engine
			ImageIO.write(img, "JPG", new File(filename));
			System.out.println("Done write");
			long start = System.currentTimeMillis();
			System.out.println(filename);
			int[] test = recognition(filename);
			for (int i = 0; i < test.length; i++) {
				ids.add(test[i]);
			}

			System.out.println("Done match "
					+ (System.currentTimeMillis() - start) + "ms");
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return ids;
	}

	static {
		//System.loadLibrary("libopencv_core.so.2.4");
		System.load("/usr/include/lib/libopencv_core.so.2.4");
		System.load("/usr/include/lib/libopencv_highgui.so.2.4");
		System.load("/usr/include/lib/libopencv_imgproc.so.2.4");
		System.load("/usr/include/lib/libopencv_nonfree.so.2.4");
		System.load("/usr/include/lib/libopencv_features2d.so.2.4");
		System.load("/usr/include/lib/libopencv_legacy.so.2.4");
		System.load("/usr/include/lib/libopencv_nonfree.so.2.4");
		System.load("/usr/include/lib/libopencv_calib3d.so.2.4");
		System.load("/home/diego/workspaceNEW/MirageServerLib/Release/libMirageServerLib.so");
	}
}

class MyShutdownHook extends Thread {
	@Override
	public void run() {

		Matcher.getProcess().destroy();
	}
}
