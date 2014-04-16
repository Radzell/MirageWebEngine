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
import java.sql.ResultSet;
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

import com.server.Matcher;
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
	public static Connection con;
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

	static String splitChar = "/";
	private static ArrayList<String> tags;

	public static void initConnection() {
		try {
			if (con == null) {
				con = DriverManager.getConnection(Config.getDBUrl(), Config.getUser(), Config.getPass());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new targetimage from file
	 * 
	 * @param filename
	 * @return
	 */
	public static TargetImage createTargetImage(String filename) {
		TargetImage targetimage = null;
		System.out.println(filename);
		try {
			Scanner sc = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8")));
			String name, author, description, tags, image;
			float rating;
			int rateCount, ID;
			Vector<KeyPoint> keys;
			Mat dess;

			ID = Integer.valueOf(sc.nextLine());
			name = sc.nextLine();
			author = sc.nextLine();
			description = sc.nextLine();
			rating = Float.valueOf(sc.nextLine());
			rateCount = Integer.valueOf(sc.nextLine());
			image = sc.nextLine();
			keys = KeyPoint.keysFromScanner(sc);
			dess = Mat.matFromScanner(sc);

			targetimage = new TargetImage(ID, name, author, description, rating, rateCount, image, keys, dess);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return targetimage;
	}

	/**
	 * Insert a targetimage to database
	 * 
	 * @param b
	 */
	public static void insertTargetImage(TargetImage b) {

		String sql = "insert into patterns (_name, _author, _description, _rating, _rateCount, _image, _bigImage,_width,_height, _keypoint, _descriptor) values (?, ?, ?, ?, ?, ?, ?,?,?, ?, ?)";

		System.out.println("SQL INSERT " + sql);
		try {
			initConnection();
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
	 * Extract info from the database for that pattern, delete the pattern and insert all the info again with the same ID
	 * I have to do that because for some reason the update doesnt always work, I keep try to find the problem but for now we can this solution.
	 * @param targetImage
	 * @param id
	 * @param idUser
	 */
	public static void editTargetImage(TargetImage targetImage, int id, int idUser) {

		int db_id = 0;
		int user_id = 0;
		String r_image = "";

		try {
			initConnection();

			System.out.println("Selection");
			String sqlSelection = "select _name,_author,_description,_rating,_rateCount,r_image,db_id,user_id from patterns where id =" + id;

			PreparedStatement psSelect = con.prepareStatement(sqlSelection);

			ResultSet rs = psSelect.executeQuery();
			while (rs.next()) {
				targetImage.name = rs.getString(1);
				targetImage.author = rs.getString(2);
				targetImage.description = rs.getString(3);
				targetImage.rating = rs.getInt(4);
				targetImage.rateCount = rs.getInt(5);
				r_image = rs.getString(6);
				db_id = rs.getInt(7);
				user_id = rs.getInt(8);
			}

			System.out.println("Deleting");

			String deleteSQL = "delete from patterns where id = ?";
			PreparedStatement psDelete = con.prepareStatement(deleteSQL);
			psDelete.setInt(1, id);
			psDelete.executeUpdate();

			System.out.println("Inserting");

			String sql = "insert into patterns (_name, _author, _description, _rating, _rateCount, _image, _bigImage,_width,_height, _keypoint, _descriptor,db_id,user_id,id,r_image) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement psInsert = con.prepareStatement(sql);
			psInsert.setNString(1, targetImage.name);
			psInsert.setNString(2, targetImage.author);
			psInsert.setNString(3, targetImage.description);
			psInsert.setString(4, String.valueOf(targetImage.rating));
			psInsert.setString(5, String.valueOf(targetImage.rateCount));
			psInsert.setString(6, targetImage.image);
			psInsert.setString(7, targetImage.bigImg);
			psInsert.setString(8, String.valueOf(targetImage.width));
			psInsert.setString(9, String.valueOf(targetImage.height));
			psInsert.setBytes(10, Util.objectToByteArray(targetImage.keys));
			psInsert.setBytes(11, Util.objectToByteArray(targetImage.dess));
			psInsert.setString(12, String.valueOf(db_id));
			psInsert.setString(13, String.valueOf(user_id));
			psInsert.setString(14, String.valueOf(id));
			psInsert.setString(15, r_image);

			psInsert.execute();

			System.out.println("Create new pattern end");

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Get a rough title of a targetimage through its filename
	 * 
	 * @param filename
	 * @return
	 */
	public static String getName(String filename) {
		String path = filename.substring(0, filename.lastIndexOf(splitChar));
		path = path.substring(path.lastIndexOf('/') + 1);
		return path;
	}

	/**
	 * Add information to a targetimage
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
		} catch (SAXException se) {
			System.out.println(infoFile);
		} catch (IOException ioe) {
		}
	}

	/**
	 * Resizing a image to a specific size
	 * 
	 * @param originalImage
	 * @param type
	 * @return
	 */
	private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int w, int h, int type) {

		BufferedImage resizedImage = new BufferedImage(w, h, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, w, h, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		return resizedImage;
	}

	/**
	 * Add image to a targetimage
	 * 
	 * @param b
	 * @param imageFile
	 */
	public static void addImage(TargetImage b, String imageFile) {
		try {
			BufferedImage img = ImageIO.read(new File(imageFile));
			int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
			b.image = ConvertValue.bitmapToBase64String(resizeImageWithHint(img, S_IMG_W, S_IMG_H, type));
			b.bigImg = ConvertValue.bitmapToBase64String(resizeImageWithHint(img, B_IMG_W, B_IMG_H, type));
			// System.out.println("Image size " + b.image.length());
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Read information of a targetimage from file
	 * 
	 * @param filename
	 * @return
	 */
	public static TargetImage readTargetImage(String filename) {
		TargetImage b = new TargetImage();

		try {
			Scanner input = new Scanner(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
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

	public static void createNewTarget(String filename) {
		insertTargetImage(readTargetImage(filename));
	}

	public static void editTarget(String filename, int id, int idUser) {
		editTargetImage(readTargetImage(filename), id, idUser);
	}
	
	
	
	public static void updatePatterns(String directoryName,String id) {
	    File directory = new File(directoryName);
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	        	System.out.println(file.getAbsolutePath());
	        	if(file.getAbsolutePath().endsWith(".jpg")){
	        		Util.deleteFile(file.getAbsolutePath() + ".txt");
					Matcher.analyze(file.getAbsolutePath());
					if (Util.checkFileExist(file.getAbsolutePath() + ".txt")) {
						editTarget(file.getAbsolutePath()+".txt", Integer.parseInt(id), 0);
						System.out.println("Updated successfully");
					} else {
						System.out.println("Fail the file doesnt exist");
					}
	        		
	        		System.out.println("IMAGE with id "+id);
	        	}
	        } else if (file.isDirectory()) {
	        	updatePatterns(file.getAbsolutePath(),file.getName());
	        }
	    }
	}
	

	public static void main(String args[]) {
		System.out.println("Connected"+args[0]+" "+args[1]);

		try {
			if (args.length == 2) {
				String filename = args[0];
				Matcher.analyze(filename);
				int id = Integer.parseInt(args[1]);
				DataIO.editTarget(filename + ".txt", id, 1);
				System.out.println("Updated successfully");

			} else if (args.length > 2) {
				System.out.println("Error too much arguments");
			} else {
				System.out.println("Error need more arguments");
			}

		} catch (Exception e) {
			System.out.println("Something went wrong");
		}

	}

}
