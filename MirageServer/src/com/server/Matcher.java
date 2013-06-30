package com.server;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
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
import com.utils.Data.VectorTargetImages;
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
			writeData(bs);
			load();
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

			int dataSize = 1;
			Iterator<TargetImage> it = b.iterator();
			int size = b.size();
			for (int i = 0; i < size; ++i) {
				TargetImage temp = it.next();
				dataSize += temp.dess.rows * temp.dess.cols + 3
						+ temp.keys.size() * 7 + 1;
			}

			VectorTargetImages.Builder vectorTargets = VectorTargetImages
					.newBuilder();

			vectorTargets.setDataSize(dataSize);
			vectorTargets.setSize(size);

			it = b.iterator();
			for (int i = 0; i < size; ++i) {
				TargetImage temp = it.next();

				com.utils.Data.TargetImage.Builder target = com.utils.Data.TargetImage
						.newBuilder();
				target.setId(temp.ID);
				target.setWidth(temp.width);
				target.setHeight(temp.height);

				int kSize = temp.keys.size();
				ArrayList<Float> data = new ArrayList<Float>();
				target.setKeyNum(kSize);
				for (int j = 0; j < kSize; ++j) {
					writeKey(data, temp.keys.get(j));
				}
				target.addAllKeys(data);

				ArrayList<Integer> dataDes = new ArrayList<Integer>();
				writeDes(target, dataDes, temp.dess);

				target.addAllDes(dataDes);

				vectorTargets.addTargets(target);

			}

			FileOutputStream output = new FileOutputStream(
					"/home/radzell/Desktop/data.mirage");
			vectorTargets.build().writeTo(output);
			output.close();
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
	private synchronized static void writeKey(ArrayList<Float> dat, KeyPoint k)
			throws IOException {
		dat.add(k.angle);
		dat.add((float) k.classId);
		dat.add((float) k.octave);
		dat.add(k.x);
		dat.add(k.y);
		dat.add(k.response);
		dat.add(k.size);
	}

	/**
	 * Write a Mat to file
	 * 
	 * @param bw
	 * @param k
	 * @throws IOException
	 */
	private synchronized static void writeDes(
			com.utils.Data.TargetImage.Builder target, ArrayList<Integer> dat,
			Mat k) throws IOException {
		target.setRows(k.rows);
		target.setCols(k.cols);
		target.setType(k.type);
		int size = k.rows * k.cols;
		for (int i = 0; i < size; ++i) {
			dat.add(k.data[i]);
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

	public native static void load();

	public native static void print();

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

		try {
			ImageIO.write(img, "JPG", new File(filename));
			System.out.println("Done write");
			long start = System.currentTimeMillis();
			System.out.println(filename);

			// Calls jni method to get matches
			int[] response = recognition(filename);
			for (int i = 0; i < response.length; i++) {
				ids.add(response[i]);
			}

			System.out.println("Done match "
					+ (System.currentTimeMillis() - start) + "ms");
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return ids;
	}

	static {
		try {
			System.loadLibrary("MirageServer");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

class MyShutdownHook extends Thread {
	@Override
	public void run() {

		Matcher.getProcess().destroy();
	}
}
