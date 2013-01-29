/*
 * TargetImage.h
 *
 *  Created on: Jan 28, 2013
 *      Author: radzell
 */
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/legacy/legacy.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace cv;

#ifndef TARGETIMAGE_H_
#define TARGETIMAGE_H_

class TargetImage
{
public:
  TargetImage();
  virtual
  ~TargetImage();

  Mat
  getDescriptor() const
  {
    return descriptor;
  }

  void
  setDescriptor(Mat descriptor)
  {
    this->descriptor = descriptor;
  }

  int
  getId() const
  {
    return ID;
  }

  void
  setId(int id)
  {
    ID = id;
  }

  vector<cv::KeyPoint>
  getKeypoints() const
  {
    return keypoints;
  }

  void
  setKeypoints(vector<cv::KeyPoint> keypoints)
  {
    this->keypoints = keypoints;
  }

  Size
  getSize() const
  {
    return size;
  }

  void
  setSize(Size size)
  {
    this->size = size;
  }
private:
  int ID;
  vector<cv::KeyPoint> keypoints;
  Mat descriptor;
  Size size;
};

#endif /* TARGETIMAGE_H_ */
