/*
 * Matcher.cpp
 *
 *  Created on: Jan 20, 2013
 *      Author: diego
 */
#include <jni.h>
#include <iostream>
#include <ctime>
#include <cstdlib>
#include <cstdio>
#include <fstream>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/legacy/legacy.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include <sys/timeb.h>
#include "TargetImage.h"
#include "newproto.pb.h"
using namespace std;
using namespace cv;

#define MAX_ITEM 200
#define MAX_KEY 500
#define INF 10000
#define EPSILON 0.0001
#define _DEBUG true

template<class T, class U>
bool compare(const pair<T, U> &a, const pair<T, U> &b) {
	return a.first > b.first;
}
#ifdef __cplusplus
extern "C" {
#endif

static vector<TargetImage> targetImages;

std::clock_t start;
double duration;

int getMilliCount() {
	timeb tb;
	ftime(&tb);
	int nCount = tb.millitm + (tb.time & 0xfffff) * 1000;
	return nCount;
}

int getMilliSpan(int nTimeStart) {
	int nSpan = getMilliCount() - nTimeStart;
	if (nSpan < 0)
		nSpan += 0x100000 * 1000;
	return nSpan;
}

/*void startTimer() {
 start = std::clock();

 }

 double stopTimer(char* text) {

 duration = (std::clock() - start) / (double) (CLOCKS_PER_SEC / 1000);
 cerr << "Time for: " << text << " " << duration << endl;
 return duration;

 }*/

/**
 * Convert data stored in targetImage into keypoints and descriptor
 */
void readKeyAndDesc(vector<KeyPoint> &trainKeys, Mat &trainDes, utils::TargetImage target) {
	// doc du lieu
	int keyNum, octave, classId;
	float x, y, angle, size, response;
	keyNum = target.keynum();
	int count = 0;
	for (int i = 0; i < keyNum; ++i) {
		angle = target.keys(count++);
		classId = target.keys(count++);
		octave = target.keys(count++);
		x = target.keys(count++);
		y = target.keys(count++);
		response = target.keys(count++);
		size = target.keys(count++);
		KeyPoint p(x, y, size, angle, response, octave, classId);
		trainKeys.push_back(p);
	}

	int rows, cols, type;
	uchar *data;
	rows = target.rows();
	cols = target.cols();
	type = target.type();
	int matSize = rows * cols;

	data = new uchar[matSize];
	for (int i = 0; i < matSize; ++i) {
		data[i] = target.des(i);
	}

	trainDes = Mat(rows, cols, CV_8U, data);
}

/**KeyPoint
 * Read database from an vector of targetimages
 */
void readDB(utils::VectorTargetImages mdata, int &count) {
	//int querySize;
	//querySize = mdata.datasize();
	if (targetImages.size() > 0) {
		targetImages.clear();
	}

	for (int i = 0; i < mdata.targets_size(); ++i) {
		utils::TargetImage target = mdata.targets(i);
		vector<KeyPoint> qK;
		Mat qD;
		Size qS;
		int ID;
		TargetImage targetimage;
		ID = target.id();
		qS.width = target.width();
		qS.height = target.height();

		readKeyAndDesc(qK, qD, target);

//		cerr << "READ DB ID: " << ID<< endl;

		targetimage.setId(ID);
		targetimage.setSize(qS);
		targetimage.setDescriptor(qD);
		targetimage.setKeypoints(qK);
		targetImages.push_back(targetimage);

	}
}

/**
 *  Checks the homography of the Homography
 */
bool niceHomography(const Mat& H) {
	const double det = (H.at<double>(0, 0) * H.at<double>(1, 1)) - (H.at<double>(1, 0) * H.at<double>(0, 1));
	if (det < 0)
		return false;
//
	const double N1 = sqrt((H.at<double>(0, 0) * H.at<double>(0, 0)) + (H.at<double>(1, 0) * H.at<double>(1, 0)));
	if (N1 > 4 || N1 < 0.1)
		return false;
//
	const double N2 = sqrt((H.at<double>(0, 1) * H.at<double>(0, 1)) + (H.at<double>(1, 1) * H.at<double>(1, 1)));
	if (N2 > 4 || N2 < 0.1)
		return false;
//
	const double N3 = sqrt((H.at<double>(2, 0) * H.at<double>(2, 0)) + (H.at<double>(2, 1) * H.at<double>(2, 1)));
	if (N3 > 0.002)
		return false;

	return true;
}

inline bool refineMatchesWithHomography(float &confidence, const std::vector<cv::KeyPoint>& queryKeypoints, const std::vector<cv::KeyPoint>& trainKeypoints, float reprojectionThreshold,
		std::vector<cv::DMatch>& matches, cv::Mat& homography) {
	const unsigned int minNumberMatchesAllowed = 15;

	if (matches.size() < minNumberMatchesAllowed)
		return false;

	// Prepare data for cv::findHomography
	std::vector<cv::Point2f> srcPoints(matches.size());
	std::vector<cv::Point2f> dstPoints(matches.size());

	for (size_t i = 0; i < matches.size(); i++) {
		srcPoints[i] = trainKeypoints[matches[i].trainIdx].pt;
		dstPoints[i] = queryKeypoints[matches[i].queryIdx].pt;
	}

	// Find homography matrix and get inliers mask
	std::vector<unsigned char> inliersMask(srcPoints.size());
	homography = cv::findHomography(srcPoints, dstPoints, CV_RANSAC, reprojectionThreshold, inliersMask);
	std::vector<cv::DMatch> inliers;
	for (size_t i = 0; i < inliersMask.size(); i++) {
		if (inliersMask[i])
			inliers.push_back(matches[i]);
	}
	confidence = (inliers.size() / (8 + 0.3 * matches.size())) * 100;

	matches.swap(inliers);

	return (matches.size() > minNumberMatchesAllowed) && niceHomography(homography) && (confidence > 55);
}

inline void showimage(string title, Mat& img) {
	Mat im_out;
	resize(img, im_out, Size((img.cols / img.rows) * 640, 640), 0, 0, INTER_LINEAR);
	imshow(title, im_out);
	waitKey(5);
}
inline void extractFeatures(const Mat& img, Mat& des, vector<KeyPoint>& keys) {
	// detect image keypoints

	ORB sfd1(1000);
	FREAK sde(true, true, 22, 8);
	sfd1.detect(img, keys);
	//cerr << "Train keys size start " << keys.size() << endl;
	int s = 500;
	// select only the appropriate number of keypoints


	while (keys.size() > 1000) {
		//cerr << "Train keys size " << keys.size() << endl;
		keys.clear();
		ORB sfd(s + 500);
		s += 500;
		sfd1.detect(img, keys);
	}

	// compute image descriptor
	sde.compute(img, keys, des);
}

inline void drawHomography(Mat& img, const std::vector<KeyPoint>& keypoints_object, const std::vector<KeyPoint>& keypoints_scene, const Size& dim, const vector<DMatch>& good_matches) {

	Mat img_scene = img.clone();

	//-- Localize the object
	std::vector<Point2f> obj;
	std::vector<Point2f> scene;

	for (size_t i = 0; i < good_matches.size(); i++) {
		//-- Get the keypoints from the good matches
		obj.push_back(keypoints_object[good_matches[i].queryIdx].pt);
		scene.push_back(keypoints_scene[good_matches[i].trainIdx].pt);
	}

	Mat H = findHomography(obj, scene, CV_RANSAC, 5);
//
//    //-- Get the corners from the image_1 ( the object to be "detected" )
	std::vector<Point2f> obj_corners(4);
	obj_corners[0] = cvPoint(0, 0);
	obj_corners[1] = cvPoint(dim.width, 0);
	obj_corners[2] = cvPoint(dim.width, dim.height);
	obj_corners[3] = cvPoint(0, dim.height);
	std::vector<Point2f> scene_corners(4);
//
//
//
	perspectiveTransform(obj_corners, scene_corners, H);
//
//    //-- Draw lines between the corners (the mapped object in the scene - image_2 )
//
	line(img_scene, scene_corners[0], scene_corners[1], Scalar(0, 255, 0), 10);
	line(img_scene, scene_corners[1], scene_corners[2], Scalar(0, 255, 0), 10);
	line(img_scene, scene_corners[2], scene_corners[3], Scalar(0, 255, 0), 10);
	line(img_scene, scene_corners[3], scene_corners[0], Scalar(0, 255, 0), 10);

	//-- Show detected matches

	showimage("Good Matches & Object detection", img_scene);

}

/**
 * Match the query image to images in database. The best matches are returned
 */
inline void match(Mat& m_grayImg, const vector<KeyPoint> &trainKeys, const Mat &trainDes, vector<pair<float, int> > &result, int begin, int end) {
	float confidence = 0;
	//cv::FlannBasedMatcher bf(new flann::LshIndexParams(10,10,2));
	BFMatcher bf(NORM_HAMMING, true);

	// train the query image
	//int size = targetImages.size();
	for (int i = begin; i < end; ++i) {
		// compute match score for each image in the database
		vector<DMatch> matches;
		vector<DMatch> refinedmatches;
		bf.match(targetImages[i].getDescriptor(), trainDes, matches);



		//Find homography transformation and detect good matches
		cv::Mat m_roughHomography;
		cv::Mat m_refinedHomography;
		bool homographyFound = refineMatchesWithHomography(confidence, targetImages[i].getKeypoints(), trainKeys, 4, matches, m_roughHomography);
		if (homographyFound) {

			//Testing the homography
			Mat m_warpedImg;
			cv::warpPerspective(m_grayImg, m_warpedImg, m_roughHomography, targetImages[i].getSize(), cv::INTER_LINEAR);

			//Shoe Warped Image
//		showimage("Title",m_warpedImg);

			//Extract Warped Image Keys
			Mat warpDes;
			vector<KeyPoint> warpKeys;
			extractFeatures(m_grayImg, warpDes, warpKeys);

			//Match
			bf.match(targetImages[i].getDescriptor(), warpDes, refinedmatches);
			homographyFound = refineMatchesWithHomography(confidence, targetImages[i].getKeypoints(), warpKeys,

			4, refinedmatches, m_refinedHomography);
			if (homographyFound) {
//				drawHomography(m_grayImg,targetImages[i].getKeypoints(),trainKeys,targetImages[i].getSize(),matches);
				pair<float, int> p(confidence, targetImages[i].getId());
				result.push_back(p);
			}
		}
	}

	// sort in descending
	std::sort(result.begin(), result.end(), compare<float, int>);
}

/**
 * Get min value of two number
 */
inline int min(int a, int b) {
	return a > b ? b : a;
}
JNIEXPORT void JNICALL Java_com_server_Matcher_load(JNIEnv *env, jclass obj, jstring path) {
	//cerr << "Loading... " << endl;

	const char *file = (*env).GetStringUTFChars(path, 0);

	//char* file = "/home/diego/Desktop/Mirage/data";

	utils::VectorTargetImages vectorTargets;

	fstream input(file, ios::in | ios::binary);
	if (!input) {
		cout << file << ": File not found.  Creating a new file." << endl;
	} else if (!vectorTargets.ParseFromIstream(&input)) {
		cerr << "Failed to parse address book." << endl;
	}

	//cerr << "Start " << endl;
	//cerr << "File: " << file << endl;
	vector<vector<KeyPoint> > queryKeys;
	vector<Mat> queryDes;
	vector<Size2i> querySizes;
	// read image from file
	vector<KeyPoint> trainKeys;
	Mat trainDes, img = imread("testProto.txt", 0);
	vector<pair<float, int> > result;

	// detect image keypoints
	extractFeatures(img, trainDes, trainKeys);

	int count = 1;

	readDB(vectorTargets, count);
	//cerr << "Loading Done" << endl;
	(*env).ReleaseStringUTFChars(path, file);

	input.close();

}

JNIEXPORT jintArray JNICALL Java_com_server_Matcher_recognition(JNIEnv *env, jclass obj, jstring path, jint jbegin, jint jend) {

//	jsize arrayCount = (*env).GetArrayLength(arrayIds);
//
//	jint bufIds[arrayCount];
//
//	(*env).GetIntArrayRegion(arrayIds, 0, arrayCount, bufIds);

	const char *nativeString = (*env).GetStringUTFChars(path, 0);

	int begin = (int) jbegin;
	int end = (int) jend;

	//cerr << "Start " << endl;
	//cerr << "File: " << nativeString << endl;

	// read image from file
	vector<KeyPoint> trainKeys;
	Mat trainDes, img = imread(nativeString, 0);
	vector<pair<float, int> > result;

	// detect image keypoints
	int start = getMilliCount();
	extractFeatures(img, trainDes, trainKeys);

	int extractFeaturesTime = getMilliSpan(start);

	//cerr << "SIZE KEYS " << trainKeys.size() << endl;

	//Load trainIG
	TargetImage trainTI;

	int startmatchTime = getMilliCount();
	//Change to add the homography and the debug

	match(img, trainKeys, trainDes, result, begin, end);


	int matchTime = getMilliSpan(startmatchTime);

	int size = min(result.size(), MAX_ITEM);
	// print out the best result
	//printf("Size: %d\n", result.size());

	jintArray resultArray;
	resultArray = (*env).NewIntArray(size + 2);
	if (resultArray == NULL) {
		return NULL; /* out of memory error thrown */
	}

	jint fill[size + 2];

	fill[0] = extractFeaturesTime;
	fill[1] = matchTime;

	for (int i = 2; i < size + 2; i++) {
		fill[i] = result[i - 2].second;
		//cout << result[i].first << " " << result[i].second << endl;
	}

	trainDes.release();
	trainKeys.clear();
	(*env).SetIntArrayRegion(resultArray, 0, size + 2, fill);
	(*env).ReleaseStringUTFChars(path, nativeString);
	return resultArray;

}

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
	for (int i = 0; i < size; ++i) {
		writeKey(fout, keys[i]);
	}
}

/**
 * Write descriptor to output stream
 */
void writeDes(ostream &fout, const Mat &des) {
	fout << des.rows << endl;
	fout << des.cols << endl;
	if (des.rows == 0 || des.cols == 0) {
		return;
	}

//int size = des.rows * des.cols;
	MatConstIterator_<uchar> it = des.begin<uchar>();

	while (it != des.end<uchar>()) {
		fout << (int) *it << endl;
		++it;
	}
}

JNIEXPORT jintArray JNICALL Java_com_server_Matcher_analyze(JNIEnv *env, jclass obj, jstring path) {

	const char *nativeString = (*env).GetStringUTFChars(path, 0);
	string s;
	FREAK sde;
	int count = 0;
	Mat img = imread(nativeString, 0);

	vector<KeyPoint> keys;
	Mat des;

	int level = 1000;
	ORB sfd(level);
	sfd.detect(img, keys);
// extract only the appropriate number of keypoints
	while ((keys.size() > 5000) && (keys.size() == 0)) {
		keys.clear();
		level += 500; // increase threshold to reduce number of detected keypoints
		ORB sfd1(level);
		sfd1.detect(img, keys);
		level++;
		//cout << "Keys: " << keys.size() << endl;
	}

// compute descriptor from keypoint and image
	sde.compute(img, keys, des);

	stringstream ss;

	ss << nativeString << ".txt";
	ofstream fout(ss.str().c_str());
	/*
	 fout << count << endl; // bookId
	 fout << "Title " << s << endl; // title
	 fout << "Author " << count << endl; // author
	 fout << "Info " << count << endl; // info
	 fout << "Tags " << count << endl; // tags
	 fout << 0 << endl; // rating
	 fout << 0 << endl; // rateCount
	 fout << s << endl; // path to image
	 fout << 15000 << endl; // price
	 */
	fout << nativeString << endl;
	fout << img.cols << endl;
	fout << img.rows << endl;
	writeKeys(fout, keys);
	//cout << "Keys num " << keys.size() << endl;
	writeDes(fout, des);
	fout.close();
	count++;
}

#ifdef __cplusplus
}
#endif
