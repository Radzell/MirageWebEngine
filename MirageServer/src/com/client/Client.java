package com.client;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entity.DataIO;
import com.entity.KeyPoint;
import com.entity.Mat;
import com.entity.TargetImage;
import com.utils.ConvertValue;
import com.utils.Util;

import flexjson.JSONDeserializer;

public class Client {

	static Socket skt;

	static void writeKey(DataOutputStream dos, KeyPoint k) {
		try {
			dos.writeFloat(k.angle);
			dos.writeInt(k.classId);
			dos.writeInt(k.octave);
			dos.writeFloat(k.x);
			dos.writeFloat(k.y);
			dos.writeFloat(k.response);
			dos.writeFloat(k.size);
		} catch (Exception exc) {

		}
	}

	static void writeMat(DataOutputStream dos, Mat m) {
		try {
			dos.writeInt(m.rows);
			dos.writeInt(m.cols);
			int size = m.rows * m.cols;
			for (int i = 0; i < size; ++i) {
				dos.writeFloat(m.data[i]);
			}
		} catch (Exception exc) {

		}
	}

	static void sendMatch(DataOutputStream dos, String filename) {
		try {
			TargetImage b = DataIO.createTargetImage(filename);
			dos.writeBytes("MATCH ");
			int size = b.keys.size();
			// int dataSize =
			// size*book.keys.firstElement().getSize()+book.dess.getSize()+1;
			// dos.writeInt(dataSize);
			dos.writeInt(size);
			Iterator<KeyPoint> it = b.keys.iterator();
			for (int i = 0; i < size; ++i) {
				writeKey(dos, it.next());
			}
			writeMat(dos, b.dess);
			dos.writeBytes("\n");
			// dos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static void sendMatchString(OutputStream out, String imgName) {
		try {
			BufferedImage img = ImageIO.read(new File(imgName));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write("MATCH " + ConvertValue.bitmapToBase64String(img) + "\n");
			System.out.println("SE SUPONE QUE TERMINA");
			bw.flush();
			System.out.println("LO TERMINA");
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	static void getImage(String xml) throws ParserConfigurationException,
			SAXException, IOException {
		StringReader sr = new StringReader(xml);
		InputSource is = new InputSource(sr);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(is);
		NodeList nl = dom.getElementsByTagName("image");
		for (int i = 0; i < nl.getLength(); ++i) {
			BufferedImage img = ConvertValue.base64StringToBitmap(nl.item(i)
					.getFirstChild().getNodeValue());
			ImageIO.write(img, "JPG", new File(i + ".jpg"));
		}
	}

	public static void main(String args[]) throws ParserConfigurationException,
			SAXException {

		DataOutputStream dos;
		// for(int i = 0; i < 100; ++i)
		try {
			skt = new Socket("localhost", 3302);
			long start = System.currentTimeMillis();
			// skt = new Socket("184.106.134.110", 3302);
			dos = new DataOutputStream(skt.getOutputStream());
			Scanner input = new Scanner(skt.getInputStream());
			sendMatchString(dos, "posters/JPEG/query/query.jpg");
			System.out.println("Done send");
			String response = "";
			int count = 0;
			while (input.hasNext()) {
				response += input.nextLine();
				count++;

			}

			System.out.println("RESPUESTA "+response);
			
			System.out.println("TAMAÑO QUE LLEGA " + response.length());
			
			System.out.println("ELEMENTOS QUE LLEGAN "+count);

			System.out.println("Response time: "
					+ (System.currentTimeMillis() - start) + "ms");

			// getImage(response);

			//sendConfirmation(dos);

			
			dos.close();
			skt.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendConfirmation(OutputStream out) {

		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
			bw.write("ok");
			bw.flush();
			System.out.println("CONFIRMATION SEND");

		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	public static File getFile(String name) {
		File file = null;
		try {

			InputStream input = skt.getInputStream();

			file = new File("/home/diego/Desktop/" + name);
			FileOutputStream out = new FileOutputStream(file);

			byte[] buffer = new byte[1024 * 1024];

			int bytesReceived = 0;

			while ((bytesReceived = input.read(buffer)) > 0) {
				out.write(buffer, 0, bytesReceived);
				System.out.println(bytesReceived + "");
				break;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return file;
	}
}
