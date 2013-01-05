package com.entity;

import java.io.DataInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Vector;

/**
 * Java representation of OpenCV KeyPoint
 * 
 * @author hoangtung
 * 
 */
public class KeyPoint implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7584516784881016940L;
	public float x, y, size;
	public float angle;
	public float response;
	public int octave, classId;

	/**
	 * Create a new KeyPoint
	 * 
	 * @param x
	 * @param y
	 * @param size
	 * @param angle
	 * @param response
	 * @param octave
	 * @param classId
	 */
	public KeyPoint(float x, float y, float size, float angle, float response,
			int octave, int classId) {
		this.x = x;
		this.y = y;
		this.size = size;
		this.angle = angle;
		this.response = response;
		this.octave = octave;
		this.classId = classId;
	}

	/**
	 * Create a vector of KeyPoint from DataInputStream
	 * 
	 * @param dis
	 * @return
	 */
	public static Vector<KeyPoint> keysFromStream(DataInputStream dis) {
		Vector<KeyPoint> keys = new Vector<KeyPoint>();

		try {
			float x, y, size, angle, response;
			int octave, classId;

			int keyNum = dis.readInt();
			keys.ensureCapacity(keyNum);
			for (int i = 0; i < keyNum; ++i) {
				angle = dis.readFloat();
				classId = dis.readInt();
				octave = dis.readInt();
				x = dis.readFloat();
				y = dis.readFloat();
				response = dis.readFloat();
				size = dis.readFloat();
				keys.add(i, new KeyPoint(x, y, size, angle, response, octave,
						classId));
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		return keys;
	}

	/**
	 * Create a Vector of KeyPoint from byte array
	 * 
	 * @param bf
	 * @param keys
	 * @param startIdx
	 * @return
	 */
	public static int keysFromByte(ByteBuffer bf, Vector<KeyPoint> keys,
			int startIdx) {
		float x, y, size, angle, response;
		int octave, classId;
		keys = new Vector<KeyPoint>();
		int keyNum = bf.getInt(startIdx);
		startIdx += 4;
		keys.ensureCapacity(keyNum);

		for (int i = 0; i < keyNum; ++i) {
			angle = bf.getFloat(startIdx);
			startIdx += 4;
			classId = bf.getInt(startIdx);
			startIdx += 4;
			octave = bf.getInt(startIdx);
			startIdx += 4;
			x = bf.getFloat(startIdx);
			startIdx += 4;
			y = bf.getFloat(startIdx);
			startIdx += 4;
			response = bf.getFloat(startIdx);
			startIdx += 4;
			size = bf.getFloat(startIdx);
			startIdx += 4;
			keys.add(i, new KeyPoint(x, y, size, angle, response, octave,
					classId));
		}

		return startIdx;
	}

	/**
	 * Create a Vector of KeyPoint from Scanner
	 * 
	 * @param sc
	 * @return
	 */
	public static Vector<KeyPoint> keysFromScanner(Scanner sc) {
		Vector<KeyPoint> keys = new Vector<KeyPoint>();

		int keyNum = sc.nextInt();
		float x, y, size, angle, response;
		int octave, classId;

		for (int i = 0; i < keyNum; ++i) {
			angle = sc.nextFloat();
			classId = sc.nextInt();
			octave = sc.nextInt();
			x = sc.nextFloat();
			y = sc.nextFloat();
			response = sc.nextFloat();
			size = sc.nextFloat();

			keys.add(new KeyPoint(x, y, size, angle, response, octave, classId));
		}

		return keys;
	}

	/**
	 * Get the String representation of this KeyPoint
	 * 
	 * @return a string representing this KeyPoint
	 */
	public StringBuilder getString() {
		StringBuilder bf = new StringBuilder();
		bf.append(angle);
		bf.append(" ");
		bf.append(classId);
		bf.append(" ");
		bf.append(octave);
		bf.append(" ");
		bf.append(x);
		bf.append(" ");
		bf.append(y);
		bf.append(" ");
		bf.append(response);
		bf.append(" ");
		bf.append(size);
		bf.append(" ");
		return bf;
	}

	/**
	 * Get the size of this KeyPoint in byte
	 * 
	 * @return size of this keypoint
	 */
	public int getSize() {
		return 28;
	}
}
