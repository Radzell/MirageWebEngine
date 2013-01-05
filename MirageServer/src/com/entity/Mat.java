package com.entity;

import java.io.DataInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * Simple representation of OpenCV Mat in Java. More specifically, this is the
 * representation of a descriptor which is a 2D Mat of float
 * 
 * @author hoangtung
 * 
 */
public class Mat implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4221912172105877130L;

	public int rows; // r

	public int cols;
	public int type;
	public int[] data;

	public static final int CV_8U = 0;

	/**
	 * Construct a new Mat
	 * 
	 * @param rows
	 * @param cols
	 * @param type
	 * @param data
	 */
	public Mat(int rows, int cols, int type, int[] data) {
		this.rows = rows;
		this.cols = cols;
		this.type = type;
		this.data = data;
	}

	/**
	 * Create a new Mat from DataInputStream
	 * 
	 * @param dis
	 * @return
	 */
	public static Mat matFromStream(DataInputStream dis) {
		Mat m = null;

		try {
			int rows, cols;
			int type;
			int data[];

			rows = dis.readInt();
			cols = dis.readInt();
			// type = dis.readInt();
			type = CV_8U;
			int size = rows * cols;
			data = new int[size];
			for (int i = 0; i < size; ++i) {
				data[i] = dis.readInt();
			}

			m = new Mat(rows, cols, type, data);
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		return m;
	}

	/**
	 * Create a new Mat from ByteBuffer
	 * 
	 * @param bf
	 * @param m
	 * @param startIdx
	 * @return
	 */
	public static int matFromByteArray(ByteBuffer bf, Mat m, int startIdx) {
		int rows, cols;
		int type;
		int data[];

		rows = bf.getInt(startIdx);
		startIdx += 4;
		cols = bf.getInt(startIdx);
		startIdx += 4;
		type = CV_8U;
		int size = rows * cols;
		data = new int[size];
		for (int i = 0; i < size; ++i) {
			data[i] = bf.get(startIdx);
			startIdx += 4;
		}

		m = new Mat(rows, cols, type, data);

		return startIdx;
	}

	/**
	 * Create a new Mat from Scanner
	 * 
	 * @param sc
	 * @return
	 */
	public static Mat matFromScanner(Scanner sc) {
		Mat m = null;

		int rows, cols;
		int type;
		int data[];

		rows = sc.nextInt();
		cols = sc.nextInt();
		// type = sc.nextInt();
		type = CV_8U;
		int size = rows * cols;
		data = new int[size];
		for (int i = 0; i < size; ++i) {
			data[i] = sc.nextInt();
		}

		m = new Mat(rows, cols, type, data);

		return m;
	}

	/**
	 * Get String representation of this Mat
	 * 
	 * @return
	 */
	public StringBuffer getString() {
		StringBuffer s = new StringBuffer();
		s.append(rows);
		s.append(" ");
		s.append(cols);
		s.append(" ");
		s.append(type);
		s.append(" ");
		int size = rows * cols;
		for (int i = 0; i < size; ++i) {
			s.append(data[i]);
			s.append(" ");
		}

		return s;
	}

	/**
	 * Append the String representation of this Mat to the StringBuilder
	 * 
	 * @param s
	 */
	public void getString(StringBuilder s) {
		s.append(rows);
		s.append(" ");
		s.append(cols);
		s.append(" ");
		s.append(type);
		s.append(" ");
		int size = rows * cols;
		for (int i = 0; i < size; ++i) {
			s.append(data[i]);
			s.append(" ");
		}
	}

	/**
	 * Get the size of this Mat in byte
	 * 
	 * @return
	 */
	public int getSize() {
		return 8 + rows * cols * 4;
	}
}
