package com.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Takes all the images in a folder and takes their descriptor and add it to the
 * database
 * 
 * @author radzell
 * 
 */
public class LoadInitialTargetsImages {
	public static void main(String args[]) {
		if (args.length < 0) {
			System.out.println("Not Enough Args");
			return;
		}
		System.out.println("Path: " + args[0]);
		File folder = new File(args[0]);
		File[] listOfFiles = folder.listFiles();
		Process p = null;
		ProcessBuilder pb = new ProcessBuilder("./FeatureRecogn");

		try {
			System.out.println("Start");

			p = pb.start();
			// p.waitFor();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()
						&& listOfFiles[i].getName().contains(".jpg")) {
					p = pb.start();
					OutputStream stdin = p.getOutputStream();

					BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter(stdin));
					// content is the string that I want to write to the
					// process.

					writer.write(listOfFiles[i].getPath());
					writer.flush();
					writer.close();

					BufferedReader br = new BufferedReader(
							new InputStreamReader(p.getInputStream()));

					String currLine = null;
					while ((currLine = br.readLine()) != null) {
						System.out.println(currLine);
					}
					p.destroy();
				}
			}
			System.out.println("End");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < listOfFiles.length; i++) {

		}
	}
}
