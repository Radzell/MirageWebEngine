package com.entity;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.utils.Config;
import com.utils.ConvertValue;
import com.utils.Util;

/**
 * Utility class which sends formatted data to sql server
 * 
 * @author hoangtung
 * 
 */
public class DataIO {
	private static Connection con;
	public static boolean WINDOWS = true;
	public static final String TARGETIMAGE = "targetimage";
	public static final String DESCRIPTION = "_description";
	public static final String AUTHOR = "_author";
	public static final String IMAGE = "_image";
	public static final String NAME = "_name";
	public static final int S_IMG_H = 60;
	public static final int S_IMG_W = 40;
	public static final int B_IMG_H = 200;
	public static final int B_IMG_W = 150;

	static String splitChar = ".jpg";
	private static ArrayList<String> tags;

	static {
		try {
			Class.forName(Config.getDriverString()).newInstance();
			con = DriverManager.getConnection(Config.getDBUrl(),
					Config.getUser(), Config.getPass());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new book from file
	 * 
	 * @param filename
	 * @return
	 */
	public static TargetImage createTargetImage(String filename) {
		TargetImage targetimage = null;
		System.out.println(filename);
		try {
			Scanner sc = new Scanner(new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF8")));
			String name, author, description, tags, image;
			float rating;
			int rateCount, ID;
			Vector<KeyPoint> keys;
			Mat dess;

			ID = Integer.valueOf(sc.nextLine());
			name = sc.nextLine();
			author = sc.nextLine();
			description = sc.nextLine();
			// System.out.println(tags);
			rating = Float.valueOf(sc.nextLine());
			rateCount = Integer.valueOf(sc.nextLine());
			image = sc.nextLine();
			keys = KeyPoint.keysFromScanner(sc);
			dess = Mat.matFromScanner(sc);

			targetimage = new TargetImage(ID, name, author, description,
					rating, rateCount, image, keys, dess);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return targetimage;
	}

	/**
	 * Insert a book to database
	 * 
	 * @param b
	 */
	public static void insertTargetImage(TargetImage b) {
		String sql = "insert into targetimage (_name, _author, _description, _rating, _rateCount, _image, _bigImage,_width,_height, _keypoint, _descriptor) values (?, ?, ?, ?, ?, ?, ?,?,?, ?, ?)";

		try {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setNString(1, b.name);
			ps.setNString(2, b.author);
			ps.setNString(3, b.description);
			ps.setString(4, String.valueOf(b.rating));
			ps.setString(5, String.valueOf(b.rateCount));
			ps.setString(6, b.image);
			ps.setString(7, b.bigImg);
			ps.setString(8, String.valueOf(b.width));
			ps.setString(9, String.valueOf(b.height));
			ps.setBytes(10, Util.objectToByteArray(b.keys));
			ps.setBytes(11, Util.objectToByteArray(b.dess));

			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a rough title of a book through its filename
	 * 
	 * @param filename
	 * @return
	 */
	public static String getName(String filename) {
		String s = filename.substring(0, filename.lastIndexOf(splitChar));
		return s.substring(s.lastIndexOf(splitChar) + 1);
	}

	/**
	 * Add information to a book
	 * 
	 * @param b
	 * @param infoFile
	 */
	public static void addDescription(TargetImage b, String infoFile) {
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			Document dom = db.parse(infoFile);

			Element elem = dom.getDocumentElement();
			NodeList nl = elem.getElementsByTagName(NAME);
			b.name = nl.item(0).getFirstChild().getNodeValue();
			nl = elem.getElementsByTagName(DESCRIPTION);
			b.description = nl.item(0).getFirstChild().getNodeValue();
			nl = elem.getElementsByTagName(AUTHOR);
			b.author = nl.item(0).getFirstChild().getNodeValue();

		} catch (ParserConfigurationException pce) {
			System.out.println(infoFile);
			// pce.printStackTrace();
		} catch (SAXException se) {
			System.out.println(infoFile);
			// se.printStackTrace();
		} catch (IOException ioe) {
			// System.out.println(infoFile);
			// ioe.printStackTrace();
		}
	}

	/**
	 * Resizing a image to a specific size
	 * 
	 * @param originalImage
	 * @param type
	 * @return
	 */
	private static BufferedImage resizeImageWithHint(
			BufferedImage originalImage, int w, int h, int type) {

		BufferedImage resizedImage = new BufferedImage(w, h, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, w, h, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		return resizedImage;
	}

	/**
	 * Add image to a book
	 * 
	 * @param b
	 * @param imageFile
	 */
	public static void addImage(TargetImage b, String imageFile) {
		try {
			BufferedImage img = ImageIO.read(new File(imageFile));
			int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img
					.getType();
			b.image = ConvertValue.bitmapToBase64String(resizeImageWithHint(
					img, S_IMG_W, S_IMG_H, type));
			b.bigImg = ConvertValue.bitmapToBase64String(resizeImageWithHint(
					img, B_IMG_W, B_IMG_H, type));
			// System.out.println("Image size " + b.image.length());
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Read information of a book from file
	 * 
	 * @param filename
	 * @return
	 */
	public static TargetImage readTargetImage(String filename) {
		TargetImage b = new TargetImage();

		try {
			Scanner input = new Scanner(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			b.name = getName(input.nextLine());
			b.description = b.name;
			b.width = Integer.parseInt(input.nextLine());
			b.height = Integer.parseInt(input.nextLine());
			b.keys = KeyPoint.keysFromScanner(input);
			b.dess = Mat.matFromScanner(input);
			File f = new File(filename);
			String infoFile = f.getParent() + splitChar + "info.xml";
			String imageFile = filename.substring(0, filename.lastIndexOf('.'));
			// System.out.println(imageFile);
			addDescription(b, infoFile);
			addImage(b, imageFile);
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		return b;
	}

	public static void main(String args[]) {
		System.out.println("Path: " + args[0]);
		File folder = new File(args[0]);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			File file = listOfFiles[i];
			if (file.isFile() && file.getPath().contains(".txt")) {
				System.out.println(file.getPath());
				insertTargetImage(readTargetImage(file.getPath()));
			}
		}
		// Scanner input = new Scanner(System.in);
		// while (input.hasNext()) {
		// String s = input.nextLine();
		// insertTargetImage(readTargetImage(s));
		// // System.out.println("Done " + s);
		// }
	}

}
