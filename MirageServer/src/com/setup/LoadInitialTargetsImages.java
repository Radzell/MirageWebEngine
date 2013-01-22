package com.setup;

import java.io.File;

/**
 * Takes all the images in a folder and takes their descriptor and add it to the
 * database
 * 
 * @author radzell
 * 
 */
public class LoadInitialTargetsImages {


	public native void callFeatureRecogn();

	public native void analyzeFeatureRecogn(String path);

	static {
		System.load("/usr/include/lib/libopencv_core.so.2.4");
		System.load("/usr/include/lib/libopencv_highgui.so.2.4");
		System.load("/usr/include/lib/libopencv_imgproc.so.2.4");
		System.load("/usr/include/lib/libopencv_nonfree.so.2.4");
		System.load("/usr/include/lib/libopencv_features2d.so.2.4");
		System.load("/usr/include/lib/libopencv_legacy.so.2.4");
		System.load("/usr/include/lib/libopencv_nonfree.so.2.4");
		System.load("/usr/include/lib/libopencv_calib3d.so.2.4");
		System.load("/home/diego/workspaceNEW/MirageServerLib/Release/libMirageServerLib.so");
	}

	public static void main(String args[]) {

		LoadInitialTargetsImages loadInit = new LoadInitialTargetsImages();

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
			if (listOfFiles[i].isFile()
					&& listOfFiles[i].getName().contains(".jpg")) {
				System.out.println("ANALYZE " + listOfFiles[i].getAbsolutePath());
				loadInit.analyzeFeatureRecogn(listOfFiles[i].getAbsolutePath());
				System.out.println("End");
			}
		}

	}
}
