package com.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.server.Job;
import com.server.Server;

/**
 * Contains utility functions
 * 
 * @author hoangtung
 * 
 */
public class Util {

	public static int jobsDone = 0;
	public static int extractFeaturesTime = 0;
	public static int matchTime = 0;

	public static synchronized void addExtractFeaturesTime(int time) {
		extractFeaturesTime += time;
	}

	public static synchronized void addMatchTime(int time) {
		matchTime += time;
		jobsDone++;
	}

	public static synchronized void restartTime() {
		extractFeaturesTime = 0;
		matchTime = 0;
		jobsDone = 0;
	}

	public static int getExtractFeaturesTime() {
		int result = extractFeaturesTime / jobsDone;
		return result;
	}

	public static int getMatchTime() {
		int result = matchTime / jobsDone;
		return result;
	}

	public static final int mask[] = { 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000 };

	public static String getNameLog() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return timeStamp;
	}

	public static FileHandler createLog(Logger logger) {
		FileHandler fileHandler = null;
		try {
			fileHandler = new FileHandler(getNameLog() + ".log", true);
			logger.addHandler(fileHandler);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileHandler;
	}

	public static void writeLog(Logger logger, Exception exception) {
		StackTraceElement[] stack = exception.getStackTrace();
		String theTrace = "";
		for (StackTraceElement line : stack) {
			theTrace += line.toString() + "\n";
		}
		if (logger.getHandlers().length == 0) {
			createLog(logger);
		}
		if (logger != null) {
			logger.info(theTrace);
		}
	}

	public static void writeLog(Logger logger, String message) {
		if (logger.getHandlers().length == 0) {
			createLog(logger);
		}
		if (logger != null) {
			logger.info(message);

		}
	}

	/**
	 * Compute average of an array
	 * 
	 * @param a
	 * @return
	 */
	public static float compAver(final float a[]) {
		float aver = 0;
		for (int i = 0; i < a.length; ++i) {
			aver += a[i];
		}

		return aver / a.length;
	}

	/**
	 * Compute standard deviation of an array
	 * 
	 * @param a
	 * @param aver
	 * @return
	 */
	public static float compDevi(final float a[], float aver) {
		float devi = 0;
		for (int i = 0; i < a.length; ++i) {
			devi += (a[i] - aver) * (a[i] - aver);
		}

		return (float) Math.sqrt(devi / a.length);
	}

	/**
	 * Convert float array to byte array
	 * 
	 * @param array
	 * @return
	 */
	public static byte[] floatArrayToByteArray(final float array[]) {
		ByteBuffer bb = ByteBuffer.allocate(array.length * 4);
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(array);

		return bb.array();
	}

	/**
	 * Convert a float number to byte array
	 * 
	 * @param f
	 * @return
	 */
	public static byte[] floatToByteArray(float f) {
		byte result[] = new byte[4];

		int value = Float.floatToIntBits(f);
		for (int j = 0; j < 4; ++j) {
			result[3 - j] = (byte) ((value & mask[j]) >> (j * 8));
		}

		return result;
	}

	/**
	 * Convert an int to byte array
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] intToByteArray(int value) {
		byte result[] = new byte[4];
		for (int j = 0; j < 4; ++j) {
			result[3 - j] = (byte) ((value & mask[j]) >> (j * 8));
		}

		return result;
	}

	/**
	 * Convert int array to byte array
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] intArrayToByteArray(int[] value) {
		ByteBuffer bb = ByteBuffer.allocate(value.length * 4);
		IntBuffer intb = bb.asIntBuffer();
		intb.put(value);

		return bb.array();
	}

	/**
	 * Convert byte array to float
	 * 
	 * @param b
	 * @return
	 */
	public static float byteArrayToFloat(byte b[]) {
		int temp = 0;
		for (int i = 0; i < 4; ++i) {
			temp = temp | (b[3 - i] << (i * 8));
		}

		System.out.println(temp);

		return Float.intBitsToFloat(temp);
	}

	/**
	 * Convert byte array to float array
	 * 
	 * @param barr
	 * @return
	 */
	public static float[] byteArrayToFloatArray(byte barr[]) {
		FloatBuffer buffer = ByteBuffer.wrap(barr).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();
		float[] ints = new float[barr.length / 4];
		buffer.get(ints);
		return ints;
	}

	/**
	 * Convert byte array to int array
	 * 
	 * @param barr
	 * @return
	 */
	public static int[] byteArrayToIntArray(byte[] barr) {
		IntBuffer buffer = ByteBuffer.wrap(barr).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
		int[] ints = new int[barr.length / 4];
		buffer.get(ints);
		return ints;
	}

	/**
	 * Convert a serializable object to byte array
	 * 
	 * @param obj
	 * @return
	 */
	public static byte[] objectToByteArray(Object obj) {
		ObjectOutputStream oos;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return baos.toByteArray();
	}

	/**
	 * Convert byte array to object
	 * 
	 * @param b
	 * @return
	 */
	public static Object objectFromByteArray(byte b[]) {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bais);
			Object obj = ois.readObject();
			ois.close();
			return obj;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static int numTargets = 0;

	private static Vector<Job> jobsToDo;

	public synchronized static void addJob(Job job) {
		if (jobsToDo == null) {
			jobsToDo = new Vector<Job>();
		}
		jobsToDo.add(job);
		Server.recognitionProcess.start();
	}

	public synchronized static void removeJobFrom(String ip, String hostname) {
		for (int i = 0; i < jobsToDo.size(); i++) {
			Job job = jobsToDo.get(i);
			if (job.getIp().equals(ip) && job.getHostname().equals(hostname)) {
				jobsToDo.remove(job);
			}
		}
	}

	public static Job getJob() {
		if (jobsToDo != null) {
			for (int i = 0; i < jobsToDo.size(); i++) {
				Job job = jobsToDo.get(i);
				if (!job.isAssigned()) {
					job.setAssigned(true);
					return job;
				}
			}
		}
		return null;
	}

	public static void setResultJob(String ip, String hostname, String result) {
		if (jobsToDo != null) {
			for (int i = 0; i < jobsToDo.size(); i++) {
				Job job = jobsToDo.get(i);
				if (job.getIp().equals(ip) && job.getHostname().equals(hostname)) {
					job.setResult(result);
					job.setProcessed(true);
					jobsToDo.set(i, job);
				}
			}
		}
	}

	public static String getJobResult(String ip, String hostname) {
		for (int i = 0; i < jobsToDo.size(); i++) {
			Job job = jobsToDo.get(i);
			if (job.getIp().equals(ip) && job.getHostname().equals(hostname)) {
				if (job.getResult() != null) {
					return job.getResult();
				} else {
					return null;
				}
			}
		}
		return null;
	}

	public static void insertNewRecord(String imageUploaded, int extractfeaturetime, int matchtime, String imageResult, int duration,
			String ip) {
		
		try {
			Class.forName(Config.getDriverString()).newInstance();
			Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());

			String sql = "insert into jobhistory (image,extractfeaturetime,matchtime,imageresult,duration,ip) values (?,?,?,?,?,?)";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setNString(1, imageUploaded);
			ps.setInt(2, extractfeaturetime);
			ps.setInt(3, matchtime);
			ps.setString(4, imageResult);
			ps.setInt(5, duration);
			ps.setNString(6, ip);
			int numRowsAffected = ps.executeUpdate();
			System.out.println("Rows affected: " + numRowsAffected);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkFileExist(String filename) {
		File f = new File(filename);
		return f.exists();
	}

}
