package com.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.entity.DataIO;
import com.entity.TargetImage;
import com.utils.Util;

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
	String filename;
	int idUser;

	String clientIP;
	int clientPort;
	String clientHostname;

	double timeUsed = 0;

	public static final String NO_JOB = "NO_JOB";
	public static final String MATCH = "MATCH";
	public static final String RATE = "RATE";
	public static final String SIMILAR = "SIMILAR";
	public static final int MAX_SIMILAR_BOOK = 10;
	private static final String IMAGE = "IMAGE";
	private static final String HISTORY = "HISTORY";

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
		timeUsed = System.currentTimeMillis();
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
			case 'H':
				s = HISTORY;
				next = 0;
				break;
			default:
				next = 5;
				s = NO_JOB;
			}
			for (int i = 0; i < next; ++i) {
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
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(inFromClient));
			String jsonJob = br.readLine();
			System.out.println(jsonJob);
			JSONObject json = new JSONObject(jsonJob);
			jobType = json.getString("type");
			filename = json.getString("filename");
			idUser = json.getInt("user");
			
			
			if (jobType.equals(MATCH)) {
				doMatchString();
			} else if (jobType.equals(IMAGE)) {
				int targetID = json.getInt("targetid");
				Matcher.analyze(filename);
				DataIO.editTarget(filename + ".txt", targetID,idUser);
				System.out.println("Updated successfully");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		jobType = NO_JOB;
		hasJob = false;
		try {
			br.close();
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
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outToClient));
			String result;

			// TODO the client must send a code to avoid the lose of connection
			// when different clients have the same IP
			
			System.out.println("filename");
			System.out.println(filename);
			
			if (Util.checkFileExist(filename)) {

				Job job = new Job();
				job.setFilename(filename);
				job.setIp(clientIP);
				job.setHostname(clientHostname);
				job.setTimeInit(timeUsed);
				job.setIdUser(idUser);
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

			System.out.println("result");
			System.out.println(result);
			
			bw.write(result);
			bw.flush();

			bw.close();
			skt.close();
			System.out.println("Time request " + (System.currentTimeMillis() - timeUsed) / 1000 + "s");

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

	// private void doImage() {
	// Scanner input = new Scanner(inFromClient);
	// String id = input.next();
	// try {
	// PreparedStatement ps =
	// con.prepareStatement("select bigimage from targetimage where bookid = " +
	// id);
	// ResultSet rs = ps.executeQuery();
	// while (rs.next()) {
	// String img = rs.getString(1);
	// BufferedWriter bw = new BufferedWriter(new
	// OutputStreamWriter(outToClient));
	// bw.write(img);
	// bw.close();
	// }
	// } catch (SQLException exc) {
	// Util.writeLog(logger, exc);
	// reconnect();
	// responseError("Cannot load image");
	// exc.printStackTrace();
	// } catch (Exception exc) {
	// Util.writeLog(logger, exc);
	// responseError("Cannot load image");
	// exc.printStackTrace();
	// }
	// }

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

	// public void getJobHistory(){
	// Class.forName(Config.getDriverString()).newInstance();
	// System.out.println("Driver Info:" + Config.getDBUrl() + ", " +
	// Config.getUser() + ", " + Config.getPass());
	// Connection con = DriverManager.getConnection(Config.getDBUrl(),
	// Config.getUser(), Config.getPass());
	//
	// PreparedStatement ps =
	// con.prepareStatement("select * from jobhistory order by requesttime");
	// ResultSet rs = ps.executeQuery();
	// while (rs.next()) {
	// arrayJobs.add(new Job(rs.getInt(1), rs.getString(2), rs.getString(3),
	// rs.getInt(4), rs.getInt(5), rs.getString(6), rs.getInt(7), rs
	// .getString(8)));
	// }
	// System.out.println("ACA");
	// for (int i = 0; i < arrayJobs.size(); i++) {
	// writeJob(arrayJobs.get(i));
	// }
	// }

}
