package com.utils;

import java.io.*;

/**
 * Get basic configuration for this server
 * 
 * @author hoangtung
 * 
 */
public class Config {
	
	
	
	//TODO fix the path
	private static String config = "../../config.txt";
	private static String driver = null;
	private static String dbUrl = null;
	private static String pass = null;
	private static String user = null;
	private static String pathFiles = null;
	private static String pathUploads = null;
	private static String pathPosters = null;
	private static int portNum = -1;

	static {
		try {
			System.out.println("Working Directory = " +System.getProperty("user.dir"));
			///config = (Util.checkFileExist("config")) ? "../../config.txt" : "config.txt";
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(config));
			driver = br.readLine();
			dbUrl = br.readLine();
			user = br.readLine();
			pass = br.readLine();
			portNum = Integer.valueOf(br.readLine());
			pathFiles = br.readLine();
			pathUploads = br.readLine();
			pathPosters = br.readLine();
			
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	public static String getDriverString() {
		return driver;
	}

	public static String getDBUrl() {
		return dbUrl;
	}

	public static String getPass() {
		return pass;
	}

	public static String getUser() {
		return user;
	}

	public static int getPortNum() {
		return portNum;
	}

	public static String getPathFiles() {
		return pathFiles;
	}

	public static String getPathUploads() {
		return pathUploads;
	}

	public static String getPathPosters() {
		return pathPosters;
	}
	
	public static void main(String[] args) {
		new Config();
	}


}
