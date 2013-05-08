package com.client;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;

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

public class Client {

	static Socket skt;
	static int port;

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

	static void getImage(String xml) throws ParserConfigurationException, SAXException, IOException {
		StringReader sr = new StringReader(xml);
		InputSource is = new InputSource(sr);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(is);
		NodeList nl = dom.getElementsByTagName("image");
		for (int i = 0; i < nl.getLength(); ++i) {
			BufferedImage img = ConvertValue.base64StringToBitmap(nl.item(i).getFirstChild().getNodeValue());
			ImageIO.write(img, "JPG", new File(i + ".jpg"));
		}
	}

	public static void main(String args[]) throws ParserConfigurationException, SAXException {

		try {
			skt = new Socket("localhost", 3302);
			// skt = new Socket("184.106.134.110", 3302);
			long start = System.currentTimeMillis();
			BufferedReader br = new BufferedReader(new InputStreamReader(skt.getInputStream()));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(skt.getOutputStream()));
			System.out.println("Connection established");
			bw.write("MATCH query.jpg\n");
			bw.flush();
			System.out.println("Request send, waiting response");
			String texto = br.readLine();
			System.out.println("Response from server: " + texto);

			br.close();
			bw.close();
			skt.close();

			System.out.println("Response time: " + (System.currentTimeMillis() - start) + "ms");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}