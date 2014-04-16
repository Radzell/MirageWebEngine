package com.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.entity.DataIO;
import com.utils.Config;
import com.utils.Util;

/**
 * Server listens for connection from user and assigns jobs to different threads
 * 
 * @author hoangtung
 * 
 */
public class Server {
	ServerThread st[], reporter;

	public static final int MAX_THREAD = 10;
	ServerSocket ss;

	ExecutorService es;

	public static RecognitionProcess recognitionProcess;

	/**
	 * Create an instance of server which operates on a specific port
	 * 
	 * @param port
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
		System.out.println("Port: " + port);
		ss = new ServerSocket(port);
		ss.setReuseAddress(true);
		// create a number of thread for handling jobs
		st = new ServerThread[MAX_THREAD];
		for (int i = 0; i < MAX_THREAD; ++i) {
			st[i] = new ServerThread();
			st[i].start();
		}
		// a special thread for reporting error to user
		reporter = new ServerThread();
		reporter.start();

		recognitionProcess = new RecognitionProcess();

	}

	public Server() {

	}

	/**
	 * Assigns job to a specific thread or return an error message to user if
	 * server is too busy
	 * 
	 * @param skt
	 * @throws IOException
	 */
	private void assignJob(Socket skt) throws IOException {
		for (int i = 0; i < MAX_THREAD; ++i) {
			// assign job to an idle thread
			if (!st[i].hasJob) {
				st[i].setJob(skt);
				return;
			}
		}

	}

	/**
	 * Listen for user connections
	 */
	public void listen() {
		try {
			while (true) {
				Socket skt = ss.accept();
				assignJob(skt);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Shutdon standard input and output stream to daemonize the application
	 * 
	 * @throws IOException
	 */
	public static void daemonize() throws IOException {
		System.out.close();
		System.in.close();
	}

	public static void main(String args[]) {
		
		try {
			if (args != null && args.length == 2) {
				String filename = args[0];
				Matcher.analyze(filename);
				int id = Integer.parseInt(args[1]);
				DataIO.editTarget(filename + ".txt", id,1);
				System.out.println("Updated successfully");
			} else {
//				daemonize();
				new Server(Config.getPortNum()).listen();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startProcess(Job job) {

		int NUM_OF_TASKS = 1;

		int nrOfProcessors = Runtime.getRuntime().availableProcessors();
		es = Executors.newFixedThreadPool(nrOfProcessors);


		int targetsPerCore = (int) Util.numTargets / nrOfProcessors;
		int extLastCore = 0;

		if ((targetsPerCore * nrOfProcessors) != Util.numTargets) {
			extLastCore = Util.numTargets - (targetsPerCore * nrOfProcessors);
		}

		NUM_OF_TASKS = nrOfProcessors;

		int beginThisCore = 0;
		int endThisCore = beginThisCore + targetsPerCore - 1;

		for (int i = 0; i < NUM_OF_TASKS; i++) {
			if (i == (NUM_OF_TASKS - 1)) {
				extLastCore = endThisCore - Util.numTargets;
				endThisCore -= extLastCore + 1;
			}
			CallBackTest task = new CallBackTest(i, beginThisCore, endThisCore, job);
			task.setCaller(this);
			es.submit(task);

			beginThisCore = endThisCore + 1;
			endThisCore = beginThisCore + targetsPerCore;
		}
	}

	public void callback(Vector<Integer> result) {
		// System.out.println("RESULT SIZE " + result.size());
		if (result.size() > 2) {
			System.out.println(result.get(2));
			try {
				Class.forName(Config.getDriverString()).newInstance();
				Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());
				Vector<Integer> tempo = new Vector<Integer>();
				tempo.add(result.get(2));
				getTargetImages(tempo, con);
				es.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private String getTargetImages(Vector<Integer> ids, Connection con) throws SQLException {

		// create a sql statement for selecting books with the listed ids
		String sql = "select _name from targetimage where id in (";

		int idSize = ids.size();
		Iterator<Integer> it = ids.iterator();
		for (int i = 0; i < idSize; ++i) {
			sql += it.next();
			if (i < idSize - 1) {
				sql += ", ";
			}
		}
		sql += ")";

		String name = "";
		if (ids.size() > 0) {
			ResultSet rs = con.createStatement().executeQuery(sql);
			// get the result and reorder it
			while (rs.next()) {
				System.out.println(rs.getNString(1));
				name = rs.getNString(1);
			}
		}

		return name;
	}
}
