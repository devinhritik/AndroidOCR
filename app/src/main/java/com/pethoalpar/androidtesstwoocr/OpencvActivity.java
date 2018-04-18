package com.pethoalpar.androidtesstwoocr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public  class OpencvActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "OpencvActivity";

    private JavaCameraView mJavaCameraView;
    Mat mRgb,mGrey,mCanny;

    BaseLoaderCallback mBaseLoaderCallback=new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS:
                    mJavaCameraView.enableView();
                    break;
                default:
                        super.onManagerConnected(status);
                        break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opencv);

        mJavaCameraView=(JavaCameraView)findViewById(R.id.java_camera_view);
        mJavaCameraView.setVisibility(View.VISIBLE);
        mJavaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mJavaCameraView!=null)
            mJavaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mJavaCameraView!=null)
            mJavaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "static initializer: opencv success");
            mBaseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }

        else {
            Log.d(TAG, "static initializer: opencv error happened");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mBaseLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgb=new Mat(width,height, CvType.CV_8UC4);
        mGrey=new Mat(width,height, CvType.CV_8UC1);
        mCanny=new Mat(width,height, CvType.CV_8UC1);

    }

    @Override
    public void onCameraViewStopped() {

        mRgb.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgb=inputFrame.rgba();
        Imgproc.cvtColor(mRgb,mGrey,Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(mGrey,mCanny,50,100);
        return mCanny;
    }

}
