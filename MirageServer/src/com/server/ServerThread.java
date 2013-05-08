package com.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

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

	// ObjectOutputStream out;
	// ObjectInputStream in;
	boolean hasJob;
	String jobType = NO_JOB;

	String clientIP;
	int clientPort;
	String clientHostname;

	public static final String NO_JOB = "NO_JOB";
	public static final String MATCH = "MATCH";
	public static final String RATE = "RATE";
	public static final String SIMILAR = "SIMILAR";
	public static final int MAX_SIMILAR_BOOK = 10;
	private static final String IMAGE = "IMAGE";

	private static Logger logger = Logger.getLogger(ServerThread.class.getName());

	/**
	 * Create new instance
	 */
	public ServerThread() {

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
		clientIP = skt.getInetAddress().getHostAddress();
		clientPort = skt.getPort();
		clientHostname = skt.getInetAddress().getHostName();
		String connectionInfo = "Connected from " + skt.getInetAddress() + " on port " + skt.getPort() + " to port " + skt.getLocalPort() + " of "
				+ skt.getLocalAddress();

		Util.writeLog(logger, connectionInfo);
		try {
			inFromClient = new BufferedInputStream(skt.getInputStream());
			outToClient = new BufferedOutputStream(skt.getOutputStream());
		} catch (IOException e) {
			Util.writeLog(logger, e);
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
			for (int i = 0; i < next; ++i){
				inFromClient.read();
			}
			// System.out.print("OK\n");
		} catch (IOException e) {
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
			skt.close();
		} catch (IOException e) {
			Util.writeLog(logger, e);
			e.printStackTrace();
		}
	}

	/**
	 * Do the matching job: find the best match books and returns them to user
	 */
	private void doMatchString() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(inFromClient));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outToClient));
			String result;
			String message = br.readLine();

			// TODO the client must send a code to avoid the lose of connection
			// when different clients have the same IP

			if (Util.checkFileExist(message)) {

				Job job = new Job();
				job.setFilename(message);
				job.setIp(clientIP);
				job.setHostname(clientHostname);
				Util.addJob(job);
				while (true) {
					Thread.sleep(300);
					result = Util.getJobResult(clientIP, clientHostname);
					if (result != null) {
						break;
					}
				}
				Util.removeJobFrom(clientIP, clientHostname);
			} else {
				result = "{\"response\":{\"code\":2,\"message\":\"the file doesn't exist\"}}";
			}

			bw.write(result);
			bw.flush();

			bw.close();
			br.close();
			skt.close();
			

		} catch (IOException e) {
			Util.writeLog(logger, e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
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

//	private void doImage() {
//		Scanner input = new Scanner(inFromClient);
//		String id = input.next();
//		try {
//			PreparedStatement ps = con.prepareStatement("select bigimage from targetimage where bookid = " + id);
//			ResultSet rs = ps.executeQuery();
//			while (rs.next()) {
//				String img = rs.getString(1);
//				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outToClient));
//				bw.write(img);
//				bw.close();
//			}
//		} catch (SQLException exc) {
//			Util.writeLog(logger, exc);
//			reconnect();
//			responseError("Cannot load image");
//			exc.printStackTrace();
//		} catch (Exception exc) {
//			Util.writeLog(logger, exc);
//			responseError("Cannot load image");
//			exc.printStackTrace();
//		}
//	}

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

		s += XmlTag.targetimage_Start + XmlTag.id_Start + b.ID + XmlTag.id_End + XmlTag.name_Start + b.name + XmlTag.name_End + XmlTag.author_Start
				+ b.author + XmlTag.author_End + XmlTag.rating_Start + b.rating + XmlTag.rating_End + XmlTag.description_Start + b.description
				+ XmlTag.description_End + XmlTag.image_Start + b.image + XmlTag.image_End + XmlTag.targetimage_End;

		return s;
	}

}
