// gauss.cpp
//

#include "stdafx.h"

#include <iostream>
#include <chrono>

#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc/imgproc.hpp>

int main() {
	std::cout << "reading file 'example.png'" << std::endl;
	cv::Mat cvMat = cv::imread("example.png");
	cv::Mat cvMatResult;
	int kernel = 91;

	cv::GaussianBlur(cvMat, cvMatResult, cv::Size(kernel, kernel), 0); // warming

	std::cout << "Gauss blur with kernel " << kernel << " with cv::Mat" << std::endl;

	auto clock_t1 = std::chrono::steady_clock::now();
	cv::GaussianBlur(cvMat, cvMatResult, cv::Size(kernel, kernel), 0);
	auto clock_t2 = std::chrono::steady_clock::now(); 
	double clock_mcs = static_cast<double>(std::chrono::duration_cast<std::chrono::milliseconds>(clock_t2 - clock_t1).count()); 
	std::cout << "Mat: " << cvMat.cols << "x" << cvMat.rows << " Duration: " << clock_mcs << "(ms)" << std::endl; 

	cv::imwrite("cvMatResult.jpg", cvMatResult);

	cv::UMat cvUMat = cvMat.getUMat(cv::ACCESS_WRITE);
	cv::UMat cvUMatResult;
	cv::GaussianBlur(cvUMat, cvUMatResult, cv::Size(kernel, kernel), 0); // warming
	std::cout << "Gauss blur with kernel " << kernel << " with cv::UMat" << std::endl;

	clock_t1 = std::chrono::steady_clock::now();
	cv::GaussianBlur(cvUMat, cvUMatResult, cv::Size(kernel, kernel), 0);
	clock_t2 = std::chrono::steady_clock::now();
	clock_mcs = static_cast<double>(std::chrono::duration_cast<std::chrono::milliseconds>(clock_t2 - clock_t1).count());
	std::cout << "UMat: " << cvUMat.cols << "x" << cvUMat.rows << " Duration: " << clock_mcs << "(ms)" << std::endl;

	cv::imwrite("cvUMatResult.jpg", cvUMatResult);

	system("pause");

    return 0;
}

