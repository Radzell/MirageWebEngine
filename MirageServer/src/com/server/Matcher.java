package com.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;

import com.entity.DataIO;
import com.entity.KeyPoint;
import com.entity.Mat;
import com.entity.TargetImage;
import com.utils.Config;
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

	private static Logger logger = Logger.getLogger(Matcher.class.getName());

	/**
	 * Fetch data from database
	 */
	static synchronized int fetch(int idAuthor) {
		IDs = new Vector<Integer>();
		Vector<TargetImage> bs = new Vector<TargetImage>();
		try {
			String sql = "select id, _keypoint, _descriptor, _width, _height from patterns";

			if (idAuthor != 0) {
				sql += " where _author = " + idAuthor;
			}

			System.out.println(sql);
			DataIO.initConnection();
			PreparedStatement ps = DataIO.con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			int count = 0;
			while (rs.next()) {
				if (count < 3) {
					count++;
				}
				if (rs.getBytes(2) != null) {
					Vector<KeyPoint> test1 = (Vector<KeyPoint>) Util.objectFromByteArray(rs.getBytes(2));
					Mat test2 = (Mat) Util.objectFromByteArray(rs.getBytes(3));
					bs.add(new TargetImage(rs.getInt(1), null, null, null, 0, 0, null, test1, test2, rs.getInt(4), rs.getInt(5)));
					IDs.add(rs.getInt(1));
				}
			}
			System.out.println(IDs.size());
			writeData(bs);
			load(Config.getPathFiles() + "data");
		} catch (Exception e) {
			Util.writeLog(logger, e);
			e.printStackTrace();
		}

		return IDs.size();
	}

	static synchronized Vector<Integer> getIds(int idOwner) {
		Vector<Integer> arrayIds = new Vector<Integer>();
		try {
			Class.forName(Config.getDriverString()).newInstance();
			Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());

			PreparedStatement ps = con.prepareStatement("select id from patterns where _author =" + idOwner);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				arrayIds.add(rs.getInt(1));
			}
		} catch (Exception e) {
			Util.writeLog(logger, e);
			e.printStackTrace();
		}

		return arrayIds;
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
				dataSize += temp.dess.rows * temp.dess.cols + 3 + temp.keys.size() * 7 + 1;
			}

			VectorTargetImages.Builder vectorTargets = VectorTargetImages.newBuilder();

			vectorTargets.setDataSize(dataSize);
			vectorTargets.setSize(size);

			it = b.iterator();
			for (int i = 0; i < size; ++i) {
				TargetImage temp = it.next();

				com.utils.Data.TargetImage.Builder target = com.utils.Data.TargetImage.newBuilder();
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

			FileOutputStream output = new FileOutputStream(Config.getPathFiles() + "data");
			vectorTargets.build().writeTo(output);
			output.close();

		} catch (Exception exc) {
			Util.writeLog(logger, exc);
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
	private synchronized static void writeKey(ArrayList<Float> dat, KeyPoint k) throws IOException {
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
	private synchronized static void writeDes(com.utils.Data.TargetImage.Builder target, ArrayList<Integer> dat, Mat k) throws IOException {
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

	public native static int[] recognition(String path, int begin, int end);

	public native static void load(String path);

	public native static void print();

	public native static void analyze(String path);

	/**
	 * Compare an image to database images to find out the most similar ones.
	 * 
	 * @param image
	 * @return ids of the most similar images
	 */
	public static Vector<Integer> match(String image, int begin, int end) {
		Vector<Integer> ids = new Vector<Integer>();
		try {
			// Calls jni method to get matches
			int[] response = recognition(image, begin, end);

			for (int i = 0; i < response.length; i++) {
				ids.add(response[i]);
			}
		} catch (Exception exc) {
			Util.writeLog(logger, exc);
			exc.printStackTrace();
		}
		return ids;
	}

	static {
		try {
			System.load("/home/radzell/workspace/MirageWebApp/libMirageServer.so");
			System.out.println("Library Directory: "+System.getProperty("java.library.path"));
		} catch (Exception e) {
			Util.writeLog(logger, e);
			e.printStackTrace();
		}

	}

	public int sum(int num1, int num2) {
		int result = num1 + num2;
		return result;
	}

}

class MyShutdownHook extends Thread {
	@Override
	public void run() {
		Matcher.getProcess().destroy();
	}
}
