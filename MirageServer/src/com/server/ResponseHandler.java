package com.server;

import java.util.Vector;
import java.util.logging.Logger;

import org.json.JSONObject;

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
				responseJson(0, fileName,b.get(0).ID);

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
		responseJson(1, "",0);
	}

	/**
	 * create a json with a simple structure to send
	 * 
	 * @param code
	 * @param url
	 */
	public void responseJson(int code, String url,int target_id) {
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

		JSONObject jsonResponse = new JSONObject();
		JSONObject responseInfo = new JSONObject();
		responseInfo.put("code", code);
		responseInfo.put("message", message);
		jsonResponse.put("response", responseInfo);
		jsonResponse.put("URL", url);
		jsonResponse.put("id", target_id);

		System.out.println(jsonResponse.toString());

		Util.setResultJob(clientIP, clientHostname, jsonResponse.toString());

	}

}
