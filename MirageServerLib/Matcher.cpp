/*
 * Matcher.cpp
 *
 *  Created on: Jan 20, 2013
 *      Author: diego
 */

#include "com_server_Matcher.h"
#include <iostream>
#include <ctime>
#include <cstdlib>
#include <cstdio>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/legacy/legacy.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace std;
using namespace cv;

#define MAX_ITEM 200
#define MAX_KEY 500
#define INF 10000
#define EPSILON 0.0001
#define _DEBUG true

template<class T, class U>
bool compare(const pair<T, U> &a, const pair<T, U> &b) {
	return a.first < b.first;
}

/**
 * Read keypoints and descriptor from input stream
 *
 */
inline void readKeyAndDesc(vector<KeyPoint> &trainKeys, Mat &trainDes) {
	// doc du lieu
	int keyNum, octave, classId;
	float x, y, angle, size, response;
	scanf("%d", &keyNum);

	for (int i = 0; i < keyNum; ++i) {
		scanf("%f%d%d%f%f%f%f", &angle, &classId, &octave, &x, &y, &response,
				&size);
		KeyPoint p(x, y, size, angle, response, octave, classId);
		trainKeys.push_back(p);
	}

	int rows, cols, type;
	uchar *data;
	scanf("%d%d%d", &rows, &cols, &type);
	int matSize = rows * cols;

	data = new uchar[matSize];
	for (int i = 0; i < matSize; ++i) {
		scanf("%d", &data[i]);
	}

	trainDes = Mat(rows, cols, CV_32F, data);
}

/**
 * Read database to memory.
 */
inline void readDatabase(vector<vector<KeyPoint> > &queryKeys,
		vector<Mat> &queryDes, vector<Size2i>& querySizes) {
	int querySize;
	scanf("%d", &querySize);
	for (int i = 0; i < querySize; ++i) {
		vector < KeyPoint > qK;
		Mat qD;
		Size2i qS;
		scanf("%d%d", qS.width, qS.height);
		// read a pair of keys and descriptors
		readKeyAndDesc(qK, qD);
		queryKeys.push_back(qK);
		queryDes.push_back(qD);
		querySizes.push_back(qS);
	}
}

/**
 * Convert data stored in an array into keypoints and descriptor
 */
void readKeyAndDesc(vector<KeyPoint> &trainKeys, Mat &trainDes, float *mdata,
		int &count) {
	// doc du lieu
	int keyNum, octave, classId;
	float x, y, angle, size, response;
	keyNum = mdata[count++];

	for (int i = 0; i < keyNum; ++i) {
		//scanf("%f%d%d%f%f%f%f", &angle, &classId, &octave, &x, &y, &response, &size);
		//ss >> angle >> classId >> octave >> x >> y >> response >> size;
		angle = mdata[count++];
		classId = mdata[count++];
		octave = mdata[count++];
		x = mdata[count++];
		y = mdata[count++];
		response = mdata[count++];
		size = mdata[count++];
		KeyPoint p(x, y, size, angle, response, octave, classId);
		trainKeys.push_back(p);
	}

	int rows, cols, type;
	uchar *data;
	rows = mdata[count++];
	cols = mdata[count++];
	type = mdata[count++];
	int matSize = rows * cols;

	data = new uchar[matSize];
	for (int i = 0; i < matSize; ++i) {
		data[i] = mdata[count++];
	}

	trainDes = Mat(rows, cols, CV_8U, data);
}

/**
 * Read database from an array
 */
void readDatabase(vector<vector<KeyPoint> > &queryKeys, vector<Mat> &queryDes,
		vector<Size> queryDims, float *mdata, int &count) {
	int querySize;
	//scanf("%d", &querySize);
	//ss >> querySize;
	querySize = mdata[count++];
	for (int i = 0; i < querySize; ++i) {
		vector < KeyPoint > qK;
		Mat qD;
		Size qS;

		qS.width = mdata[count++];
		qS.height = mdata[count++];
		cerr << qS.width << " " << qS.height << endl;
		readKeyAndDesc(qK, qD, mdata, count);
		queryKeys.push_back(qK);
		queryDes.push_back(qD);
		queryDims.push_back(qS);
	}
}

inline bool refineMatchesWithHomography(
		const vector<cv::KeyPoint>& queryKeypoints,
		const vector<cv::KeyPoint>& trainKeypoints, float reprojectionThreshold,
		vector<cv::DMatch>& matches, cv::Mat& homography) {
	const unsigned int minNumberMatchesAllowed = 8;

	if (matches.size() < minNumberMatchesAllowed)
		return false;

	// Prepare data for cv::findHomography
	vector < cv::Point2f > srcPoints(matches.size());
	vector < cv::Point2f > dstPoints(matches.size());

	for (size_t i = 0; i < matches.size(); i++) {
		srcPoints[i] = trainKeypoints[matches[i].trainIdx].pt;
		dstPoints[i] = queryKeypoints[matches[i].queryIdx].pt;
	}

	// Find homography matrix and get inliers mask
	vector<unsigned char> inliersMask(srcPoints.size());
	homography = cv::findHomography(srcPoints, dstPoints, CV_RANSAC,
			reprojectionThreshold, inliersMask);

	vector < cv::DMatch > inliers;
	for (size_t i = 0; i < inliersMask.size(); i++) {
		if (inliersMask[i])
			inliers.push_back(matches[i]);
	}

	matches.swap(inliers);
	return matches.size() > minNumberMatchesAllowed;
}

inline void showimage(string title, Mat& img) {
	Mat im_out;
	resize(img, im_out, Size((img.cols / img.rows) * 640, 640), 0, 0,
			INTER_LINEAR);
	imshow(title, im_out);
	waitKey(5);
}
inline void extractFeatures(const Mat& img, Mat& des, vector<KeyPoint>& keys) {
	// detect image keypoints

	ORB sfd1(1000);
	FREAK sde;
	sfd1.detect(img, keys);
	//cerr << "Train keys size start " << keys.size() << endl;
	int s = 500;
	// select only the appropriate number of keypoints
	while (keys.size() > 1000) {
		//cerr << "Train keys size " << keys.size() << endl;
		keys.clear();
		FREAK sfd(s + 500);
		s += 500;
		sfd1.detect(img, keys);
	}

	// compute image descriptor
	sde.compute(img, keys, des);
}
/**
 * Match the query image to images in database. The best matches are returned
 */
inline void match(const Mat& m_grayImg, const vector<KeyPoint> &trainKeys,
		const Mat &trainDes, const vector<vector<KeyPoint> > &queryKeys,
		const vector<Mat> &queryDes, vector<pair<float, int> > &result) {
	int runs = 0;
	// use Flann based matcher to match images
	BFMatcher bf(NORM_HAMMING, true);
	// train the query image
	int size = queryDes.size();
	for (int i = 0; i < size; ++i) {
		// compute match score for each image in the database
		vector < DMatch > matches;
		vector < DMatch > refinedmatches;
		bf.match(queryDes[i], trainDes, matches);

#if _DEBUG

#endif
		//Find homography transformation and detect good matches
		cv::Mat m_roughHomography;
		cv::Mat m_refinedHomography;

		bool homographyFound = refineMatchesWithHomography(queryKeys[i],
				trainKeys,

				1, matches, m_roughHomography);
		if (homographyFound) {

			//float m = filter(trainSize, matches);
			Mat m_warpedImg;
			Size2i size = cv::Size(773, 512);
			//cerr<<"Size"<<m_grayImg.cols<<" : "<<m_grayImg.rows<<endl;
			cv::warpPerspective(m_grayImg, m_warpedImg, m_roughHomography, size,
					cv::INTER_LINEAR);

			//Shoe Warped Image
			//showimage("Title", m_warpedImg);

			//Extract Warped Image Keys
			Mat warpDes;
			vector < KeyPoint > warpKeys;
			extractFeatures(m_grayImg, warpDes, warpKeys);

			//Match
			bf.match(queryDes[i], warpDes, refinedmatches);

			homographyFound = refineMatchesWithHomography(queryKeys[i],
					warpKeys,

					1, refinedmatches, m_refinedHomography);
			if (homographyFound) {
				pair<float, int> p(matches.size(), i);
				result.push_back(p);
				runs++;
			}
		}
	}

	// sort in descending
	sort(result.begin(), result.end(), compare<float, int>);
}

/**
 * Get min value of two number
 */
inline int min(int a, int b) {
	return a > b ? b : a;
}
JNIEXPORT jintArray JNICALL Java_com_server_Matcher_recognition(JNIEnv *env,
		jclass obj, jstring path) {

	const char *nativeString = (*env).GetStringUTFChars(path, 0);

	cerr << "Start " << endl;
	cerr << "File: " << nativeString << endl;

	vector < vector<KeyPoint> > queryKeys;
	vector < Mat > queryDes;
	vector < Size2i > querySizes;
// read image from file
	vector < KeyPoint > trainKeys;
	Mat trainDes, img = imread(nativeString, 0);
	vector<pair<float, int> > result;

// detect image keypoints
	extractFeatures(img, trainDes, trainKeys);

	FILE * pFile;
	long lSize;
	char * buffer;
	size_t sresult;

	pFile = fopen("Data.txt", "rb");
	if (pFile == NULL) {
		fputs("File error", stderr);
		exit(1);
	}

// obtain file size:
	fseek(pFile, 0, SEEK_END);
	lSize = ftell(pFile);
	rewind(pFile);

// allocate memory to contain the whole file:
	buffer = (char*) malloc(sizeof(char) * lSize);
	if (buffer == NULL) {
		fputs("Memory error", stderr);
		exit(2);
	}

// copy the file into the buffer:
	sresult = fread(buffer, 1, lSize, pFile);
	if (sresult != lSize) {
		fputs("Reading error", stderr);
		exit(3);
	}

	/* the whole file is now loaded in the memory buffer. */

// terminateknn
//fclose (pFile);
	int dataSize, count = 0;
	char *endPtr;
	dataSize = strtol(buffer, &endPtr, 10);
	float *mdata = new float[dataSize];
// read data as an array of float number
	for (int i = 0; i < dataSize; ++i) {
		mdata[i] = strtod(endPtr, &endPtr);
	}

	readDatabase(queryKeys, queryDes, querySizes, mdata, count);
	fclose(pFile);

//Change to add the homography and the debug
	cerr << "Matching begin" << endl;
	match(img, trainKeys, trainDes, queryKeys, queryDes, result);
	int size = min(result.size(), MAX_ITEM);
// print out the best result
	printf("Size: %d\n", result.size());

	jintArray resultArray;
	resultArray = (*env).NewIntArray(size);
	if (resultArray == NULL) {
		return NULL; /* out of memory error thrown */
	}

	jint fill[size];

	for (int i = 0; i < size; ++i) {
		fill[i] = result[i].second;
		cout << result[i].first << " " << result[i].second << endl;
	}
	trainDes.release();
	trainKeys.clear();
	delete[] mdata;

	(*env).SetIntArrayRegion(resultArray, 0, size, fill);
	(*env).ReleaseStringUTFChars(path, nativeString);
	return resultArray;

}

