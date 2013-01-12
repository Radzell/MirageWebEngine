#include <stdio.h>
#include <iostream>
#include <fstream>
#include <opencv2/opencv.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
using namespace std;
using namespace cv;

/**
 * Write key to output stream
 */
void writeKey(ostream &fout, const KeyPoint &key) {
	fout << key.angle << endl;
	fout << key.class_id << endl;
	fout << key.octave << endl;
	fout << key.pt.x << endl;
	fout << key.pt.y << endl;
	fout << key.response << endl;
	fout << key.size << endl;
}

/**
 * Write multiple keys to output stream
 */
void writeKeys(ostream &fout, const vector<KeyPoint> &keys) {
	int size = keys.size();
	fout << size << endl;
	for(int i = 0; i < size; ++i) {
		writeKey(fout, keys[i]);
	}
}

/**
 * Write descriptor to output stream
 */
void writeDes(ostream &fout, const Mat &des) {
	fout << des.rows << endl;
	fout << des.cols << endl;
	if(des.rows == 0 || des.cols == 0) {
		return;
	}

	//int size = des.rows * des.cols;
	MatConstIterator_<uchar> it = des.begin<uchar>();
	
	while(it != des.end<uchar>()) {
		fout << (int)*it << endl;
		++it;
	}
}
/**
 * Read keypoints and descriptor from input stream
 *
 */
inline void readKeyAndDesc(ifstream& fin,Mat& trainDes) {
	// doc du lieu


	int keyNum, octave, classId;
	float x, y, angle, size, response;
	string line;

	getline(fin,line);
	getline(fin,line);
	keyNum =  atoi(line.c_str());

	for(int i = 0; i < keyNum; ++i) {
		getline(fin,line);
		angle=atof(line.c_str());
		getline(fin,line);
		classId=atoi(line.c_str());
		getline(fin,line);
		octave=atoi(line.c_str());
		getline(fin,line);
		x=atof(line.c_str());
		getline(fin,line);
		y=atof(line.c_str());
		getline(fin,line);
		response=atof(line.c_str());
		getline(fin,line);
		size=atof(line.c_str());


		KeyPoint p(x, y, size, angle, response, octave, classId);
		//cout<<"KeyNum "<<keyNum<<x<< y<< size<< angle<< response<< octave<< classId<<endl;
	}

	int rows, cols;
	uchar *data;

	getline(fin,line);
	rows = atoi(line.c_str());
	getline(fin,line);
	cols = atoi(line.c_str());
	//getline(fin,line);
	//type = atoi(line.c_str());


	cout<<"dest "<<rows<<" "<<" "<<cols<<" "<<0<<endl;
	int matSize = rows*cols;

	data = new uchar[matSize];
	for(int i = 0; i < matSize; ++i) {
		getline(fin,line);
		uchar c = atoi(line.c_str());
		data[i] = c;
	}

	trainDes = Mat(rows, cols, CV_8U, data);
}

/**
 * Get a copy of a Mat
 * @return: copy of the mat specified in parametter
 */
//Mat copyMat(const Mat &m) {
//	float *s = new float[m.rows*m.cols];
//	MatConstIterator_<float> it = m.begin<float>();
//	int i = 0;
//	while(it != m.end<float>()) {
//		s[i] = *it;
//		i++;
//		it++;
//	}
//
//	return Mat(m.rows, m.cols, CV_32F, s);
//}

/**
 * Main function: extract keypoints and descriptors from multiple images
 */
int main(int argc, char *argv[]) {
	string s;
	FREAK sde;
	int count = 0;
	while(getline(cin, s)) {
		cout << s << endl;
		Mat img = imread(s, 0);

		vector<KeyPoint> keys;
		Mat des;

		int level = 1000;
		ORB sfd(level);
		sfd.detect(img, keys);
		// extract only the appropriate number of keypoints
		while((keys.size() > 5000)&&(keys.size()==0)) {
			keys.clear();
			level += 500;	// increase threshold to reduce number of detected keypoints
			ORB sfd1(level);
			sfd1.detect(img, keys);
			level++;
			cout<<"Keys: "<<keys.size()<<endl;
		}
		
		// compute descriptor from keypoint and image
		sde.compute(img, keys, des);
		cout << "Ok " << endl;

		stringstream ss;
		ss << s << ".txt";
		ofstream fout(ss.str().c_str());
		/*
		fout << count << endl; // bookId
		fout << "Title " << s << endl; // title
		fout << "Author " << count << endl; // author
		fout << "Info " << count << endl; // info
		fout << "Tags " << count << endl; // tags
		fout << 0 << endl; // rating
		fout << 0 << endl; // rateCount
		fout << s << endl;	// path to image
		fout << 15000 << endl; // price
		*/
		fout << s << endl;
		fout<<img.cols<<endl;
		fout<<img.rows<<endl;
		writeKeys(fout, keys);
		cout << "Keys num " << keys.size() << endl;
		writeDes(fout, des);
		fout.close();
		count++;


		//Read text file and verify data.
		/*cout<<"des "<<des.rows<<" "<<" "<<des.cols<<" "<<des.type()<<endl;


		ifstream fin(ss.str().c_str());
		string line;
		Mat dest;
		if(fin.is_open()){
			readKeyAndDesc(fin,dest);
		}
		fin.close();
		cout<<"des"<<endl;
		cout<<des<<endl;
		cout<<"dest"<<endl;
		cout<<dest<<endl;*/
	}
	
	return 0;
}
