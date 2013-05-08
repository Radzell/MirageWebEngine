package com.server;

import java.util.Vector;
import java.util.logging.Logger;

import com.entity.TargetImage;
import com.utils.Util;

public class ResponseHandler {

	private static Logger logger = Logger.getLogger(ResponseHandler.class.getName());
	String clientIP;
	String clientHostname;

	public ResponseHandler(String clientIP, String hostname) {
		this.clientIP = clientIP;
		this.clientHostname = hostname;
	}

	/**
	 * Send books' information to user
	 * 
	 * @param books
	 */
	public void responseTargetImages(Vector<TargetImage> b) {
		if (b == null || b.size() == 0) {
			responseError("Cannot find book");
		} else {
			try {
				String fileName = b.get(0).name;
				fileName = fileName.substring(fileName.lastIndexOf('/') + 1) + ".jpg";
				responseJson(0, fileName);

			} catch (Exception exc) {
				Util.writeLog(logger, exc);
				exc.printStackTrace();
			}
		}

	}

	/**
	 * Send an error message to user
	 * 
	 * @param message
	 */
	public void responseError(String message) {
		responseJson(1, "");
	}

	/**
	 * create a json with a simple structure to send
	 * @param code
	 * @param url
	 */
	public void responseJson(int code, String url) {
		String message = "";
		switch (code) {
		case 0:
			message = "OK";
			break;
		case 1:
			message = "Cannot find book";
			break;
		case 2:
			message = "PROBLEM";
			break;

		default:
			message = "OK";
			break;
		}

		String jsonResponse = "{\"response\":{\"code\":" + code + ",\"message\":\"" + message + "\"},\"URL\":\"" + url + "\"}";

		System.out.println(jsonResponse);

		Util.setResultJob(clientIP, clientHostname, jsonResponse);

	}

}
