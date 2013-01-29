package com.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import com.entity.KeyPoint;
import com.entity.Mat;
import com.entity.TargetImage;
import com.utils.Config;
import com.utils.Util;

import flexjson.JSONSerializer;

/**
 * ServerThread is responsible for serving user's request.
 * 
 * @author hoangtung
 * 
 */
public class ServerThread extends Thread {
	Socket skt;
	BufferedInputStream inFromClient;
	BufferedOutputStream outToClient;
	boolean hasJob;
	String jobType = NO_JOB;
	Connection con;

	public static final String NO_JOB = "NO_JOB";
	public static final String MATCH = "MATCH";
	public static final String RATE = "RATE";
	public static final String SIMILAR = "SIMILAR";
	public static final int MAX_SIMILAR_BOOK = 10;
	private static final String IMAGE = "IMAGE";

	/**
	 * Create new instance
	 */
	public ServerThread() {
		try {
			Class.forName(Config.getDriverString()).newInstance();
			con = DriverManager.getConnection(Config.getDBUrl(),
					Config.getUser(), Config.getPass());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Reconnect to sql server if connection was broken
	 * 
	 * @return
	 */
	private boolean reconnect() {
		try {
			Class.forName(Config.getDriverString()).newInstance();
			con = DriverManager.getConnection(Config.getDBUrl(),
					Config.getUser(), Config.getPass());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Assign job to this thread
	 * 
	 * @param s
	 * @return
	 */
	public boolean setJob(Socket s) {
		if (hasJob) {
			return false;
		}

		skt = s;
		try {
			inFromClient = new BufferedInputStream(skt.getInputStream());
			outToClient = new BufferedOutputStream(skt.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		hasJob = true;
		return true;
	}

	/**
	 * 
	 */
	@Override
	public void run() {
		while (true) {
			if (hasJob) {
				doJob();
			} else {
				try {
					sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Get user's request type
	 * 
	 * @return User's request type
	 */
	private String getJobType() {
		String s = null;
		int next = 0;
		try {
			switch (inFromClient.read()) {
			case 'M':
				s = MATCH;
				next = 5;
				break;
			case 'R':
				s = RATE;
				next = 4;
				break;
			case 'S':
				s = SIMILAR;
				next = 7;
				break;

			case 'I':
				s = IMAGE;
				next = 5;
				break;
			default:
				next = 5;
				s = NO_JOB;
			}

			for (int i = 0; i < next; ++i)
				System.out.print((char) inFromClient.read());
			System.out.print("OK\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return s;
	}

	/**
	 * Do the job assigned to this thread. This function determines the job type
	 * and calls the appropriate function to handle the job
	 */
	private void doJob() {
		jobType = getJobType();
		if (jobType.equals(MATCH)) {
			doMatchString();
		}

		jobType = NO_JOB;
		hasJob = false;
		try {
			inFromClient.close();
			outToClient.close();
			skt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Do the matching job: find the best match books and returns them to user
	 */
	private void doMatchString() {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				inFromClient));
		try {
			System.out.println("Reading");
			String image = br.readLine();
			String temp = null;
			while ((temp = br.readLine()) != null) {
				image += "\n" + temp;
			}
			System.out.println("Read ok");

			try {
				Vector<TargetImage> b = getTargetImages(Matcher.match(image));
				responseTargetImages(b);
			} catch (Exception exc) {
				responseError("Cannot find book");
				exc.printStackTrace();
				reconnect();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get list of books from database with the specified ids
	 * 
	 * @param ids
	 * @return
	 * @throws SQLException
	 */
	private Vector<TargetImage> getTargetImages(Vector<Integer> ids)
			throws SQLException {
		Vector<TargetImage> result = new Vector<TargetImage>();

		result.setSize(ids.size());
		int id, rc, p;
		String tit, au, in, ta, img;
		float ra;

		byte[] kypbt;
		byte[] dessbt;
		Vector<KeyPoint> kyp;

		Mat dess;

		String kypst;
		String dessst;

		// create a sql statement for selecting books with the listed ids
		String sql = "select id, _name, _author, _description, _rating, _rateCount, _image,_keypoint,_descriptor from targetimage where id in (";
		int idSize = ids.size();
		Iterator<Integer> it = ids.iterator();
		for (int i = 0; i < idSize; ++i) {
			sql += it.next();
			if (i < idSize - 1) {
				sql += ", ";
			}
		}
		sql += ")";
		System.out.println("SQL " + sql);
		ResultSet rs = con.createStatement().executeQuery(sql);
		// get the result and reorder it
		while (rs.next()) {
			// System.out.println("Add one book");
			id = rs.getInt(1);
			tit = rs.getNString(2);
			au = rs.getNString(3);
			in = rs.getNString(4);
			ra = rs.getFloat(5);
			rc = rs.getInt(6);
			img = rs.getString(7);
			kypbt = (byte[]) rs.getObject(8);
			kyp = (Vector<KeyPoint>) Util.objectFromByteArray(kypbt);
			dessbt = (byte[]) rs.getObject(9);
			dess = (Mat) Util.objectFromByteArray(dessbt);

			int idx = ids.indexOf(id);
			System.out.println("Idx " + idx);

			result.set(idx, new TargetImage(id, tit, au, in, ra, rc, img,
					kypbt, dessbt));
		}

		return result;
	}

	private void doImage() {
		Scanner input = new Scanner(inFromClient);
		String id = input.next();
		try {
			PreparedStatement ps = con
					.prepareStatement("select bigimage from targetimage where bookid = "
							+ id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String img = rs.getString(1);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						outToClient));
				bw.write(img);
				bw.close();
			}
		} catch (SQLException exc) {
			reconnect();
			responseError("Cannot load image");
			exc.printStackTrace();
		} catch (Exception exc) {
			responseError("Cannot load image");
			exc.printStackTrace();
		}
	}

	/**
	 * Standardize a string to avoid sql error
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unused")
	private String formalize(String name) {

		return name.replaceAll("'", "''");
	}

	/**
	 * Put book's information in a xml form
	 * 
	 * @param targetImage
	 * @return
	 */
	private String formatTargetImage(TargetImage b) {
		String s = "";

		s += XmlTag.targetimage_Start + XmlTag.id_Start + b.ID + XmlTag.id_End
				+ XmlTag.name_Start + b.name + XmlTag.name_End
				+ XmlTag.author_Start + b.author + XmlTag.author_End
				+ XmlTag.rating_Start + b.rating + XmlTag.rating_End
				+ XmlTag.description_Start + b.description
				+ XmlTag.description_End + XmlTag.image_Start + b.image
				+ XmlTag.image_End + XmlTag.targetimage_End;

		return s;
	}

	/**
	 * Send books' information to user
	 * 
	 * @param books
	 */
	private void responseTargetImages(Vector<TargetImage> b) {
		if (b == null || b.size() == 0) {
			responseError("Cannot find book");
			return;
		}
		JSONSerializer serializer = new JSONSerializer().include("keysbt",
				"dessbt");

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				outToClient));
		try {

			bw.write(serializer.serialize(b));

			bw.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Send an error message to user
	 * 
	 * @param message
	 */
	public void responseError(String message) {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				outToClient));
		try {
			bw.write("ERROR " + message);
			bw.close();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
