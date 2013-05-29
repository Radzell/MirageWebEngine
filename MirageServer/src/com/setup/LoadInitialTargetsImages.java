package com.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.server.Matcher;
import com.utils.Util;

/**
 * Takes all the images in a folder and takes their descriptor and add it to the
 * database
 * 
 * @author radzell
 * 
 */
public class LoadInitialTargetsImages {

	public native void callFeatureRecogn();

//	public native void analyzeFeatureRecogn(String path);

	static {
		try {
			System.loadLibrary("MirageServer");
		} catch (Exception e) {
			//Util.writeLog(logger, e);
			e.printStackTrace();
		}

	}

	public void createFilesFromTargets(String args[]) {
		if (args.length < 0) {
			System.out.println("Not Enough Args");
			return;
		}
		String pathTemp = "posters/JPEG";
		System.out.println("Path: " + pathTemp);
		File folder = new File(pathTemp);
		File[] listOfFiles = folder.listFiles();

		System.out.println("Start");

		// p = pb.start(); // p.waitFor();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".jpg")) {
				System.out.println("ANALYZE " + listOfFiles[i].getAbsolutePath());
				Matcher.analyze(listOfFiles[i].getAbsolutePath());
				System.out.println("End");
			}
		}
	}

	public static void main(String args[]) {

		LoadInitialTargetsImages loadInit = new LoadInitialTargetsImages();

		
		Matcher.analyze("/home/diego/Desktop/Mirage/uploads/query.jpg");
		System.out.println("End");

	}

	public void testReadingFile() {
		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader("Movie Poster 1.jpg.txt"));
			String line = "";
			while((line=br.readLine())!=null){
				System.out.println(line);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}

	}
}
