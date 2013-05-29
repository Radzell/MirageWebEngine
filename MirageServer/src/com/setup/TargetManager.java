package com.setup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.entity.DataIO;
import com.entity.TargetImage;
import com.server.Matcher;
import com.utils.Config;


/**
 * This class is just for testing the admin page
 * @author diego
 *
 */

public class TargetManager {

//	public static void delete(String id) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
//		Class.forName(Config.getDriverString()).newInstance();
//		// System.out.println("Driver Info:" + Config.getDBUrl() + ", " +
//		// Config.getUser() + ", " + Config.getPass());
//		Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());
//
//		PreparedStatement ps = con.prepareStatement("delete from targetimage where id = " + id);
//		int numRowsAffected = ps.executeUpdate();
//		System.out.println("Rows affected: " + numRowsAffected);
//
//	}
//
//	public static String moveFile(String filename) throws IOException {
//		String s = null;
//		String command = "mv " + Config.getPathUploads() + filename + " " + Config.getPathPosters() + filename;
//		Process p = Runtime.getRuntime().exec(command);
//		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
//		while ((s = stdInput.readLine()) != null) {
//			System.out.println(s);
//		}
//		return Config.getPathPosters() + filename;
//
//	}
//
//	public static String insert(String filename) throws IOException {
//		// filename = moveFile(filename);
//		
//		filename = Config.getPathPosters()+filename;
//		
//		Matcher.analyze(filename);
//		filename = filename + ".txt";
//		DataIO.createNewTarget(filename);
//		System.out.println("Insert succesfull on "+filename);
//		return filename;
//	}
//
//	public static ArrayList<TargetImage> getTargets() throws Exception {
//		// String responseJson = "[";
//		Class.forName(Config.getDriverString()).newInstance();
//		// System.out.println("Driver Info:" + Config.getDBUrl() + ", " +
//		// Config.getUser() + ", " + Config.getPass());
//		Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());
//		PreparedStatement ps = con.prepareStatement("Select * from targetimage order by _name");
//		ResultSet rs = ps.executeQuery();
//
//		ArrayList<TargetImage> arrayTargets = new ArrayList<TargetImage>();
//		while (rs.next()) {
//			TargetImage target = new TargetImage();
//			target.ID = rs.getInt("id");
//			target.name = rs.getString("_name");
//			target.author = rs.getString("_author");
//			target.rating = rs.getInt("_rating");
//			target.rateCount = rs.getInt("_ratecount");
//
//			// responseJson = responseJson + "{\"id\":\"" + rs.getString("id") +
//			// "\",\"name\":\"" + rs.getString("_name") + "\",\"author\":\""
//			// + rs.getString("_author") + "\",\"rating\":\"" +
//			// rs.getString("_rating") + "\",\"rateCount\":\"" +
//			// rs.getString("_ratecount")
//			// + "\"},";
//
//			arrayTargets.add(target);
//		}
//		return arrayTargets;
//		// return responseJson.substring(0, responseJson.length() - 1) + "]";
//	}
//
//	public static String getJson(TargetImage target) {
//		String responseJson = "{\"id\":\"" + target.ID + "\",\"name\":\"" + target.name + "\",\"author\":\"" + target.author + "\",\"rating\":\""
//				+ target.rating + "\",\"rateCount\":\"" + target.rateCount + "\"}";
//		return responseJson;
//	}
//
//	public static String getJobsHistory() {
//		String jsonHistory = "[";
//		try {
//
//			Class.forName(Config.getDriverString()).newInstance();
//			System.out.println("Driver Info:" + Config.getDBUrl() + ", " + Config.getUser() + ", " + Config.getPass());
//			Connection con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());
//
//			PreparedStatement ps = con.prepareStatement("select * from jobhistory order by requesttime");
//			ResultSet rs = ps.executeQuery();
//			while (rs.next()) {
//				// arrayJobs.add(new Job(rs.getInt(1), rs.getString(2),
//				// rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
//				// rs.getInt(7), rs
//				// .getString(8)));
//				jsonHistory += "{ \"id\":\"" + rs.getInt(1) + "\",\"requestTime\":\"" + rs.getString(2) + "\",\"image\":\"" + rs.getString(3)
//						+ "\",\"extractfeaturetime\":\"" + rs.getInt(4) + "\",\"matchtime\":\"" + rs.getInt(5) + "\",\"imageresult\":\""
//						+ rs.getString(6) + "\",\"duration\":\"" + rs.getInt(7) + "\",\"ip\":\"" + rs.getString(8) + "\"},";
//			}
//			return jsonHistory.substring(0, jsonHistory.length() - 1) + "]";
//
//			// System.out.println("ACA");
//			// for (int i = 0; i < arrayJobs.size(); i++) {
//			// writeJob(arrayJobs.get(i));
//			// }
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//		// return arrayJobs;
//	}
//
//	public static void showHelp() {
//		System.out.println("TargetManager.jar insert image.png");
//		System.out.println("TargetManager.jar delete 12");
//	}
//
//	public static void main(String[] args) {
//
//		try {
//			if (args.length >= 1) {
//				String method = args[0];
//				if (method.equals("-help") || method.equals("-h")) {
//					TargetManager.showHelp();
//				} else if (method.equals("gettargets")) {
//					System.out.println(TargetManager.getTargets());
//				} else {
//					if (args.length == 2) {
//						if (method.equals("insert")) {
//							TargetManager.insert(args[1]);
//						} else if (method.equals("delete")) {
//							TargetManager.delete(args[1]);
//						} else if (method.equals("edit")) {
//							System.out.println("Still in development");
//						} else {
//							System.out.println("Arguments invalid please use -help or -h to see the correct options");
//						}
//					} else {
//						System.out.println("Arguments invalid please use -help or -h to see the correct options");
//					}
//				}
//			} else {
//				System.out.println("Use -help or -h to see the options");
//			}
//
//		} catch (Exception e) {
//			System.out.println(e.getMessage());
//		}
//
//		// System.out.println(DataIO.getName("/opt/lampp/htdocs/jquery/server/php/files/iron-man-3-poster.jpg"));
//	}
}
