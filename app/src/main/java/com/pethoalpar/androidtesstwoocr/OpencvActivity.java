package com.pethoalpar.androidtesstwoocr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public  class OpencvActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2
        {

            private Mat hsvFrame, rgbaFrame, rgbFrame, inRangeMask, filteredFrame, hChannel;

            private int camHeight, camWidth, frameDim;

            public static Bitmap bitmap, bitmapMask;
            Integer timeToElapse;
            private boolean methodAuto, countDown;
            String osdSecond;
            private Scalar thresMin = new Scalar(0, 0, 0);
            private Scalar thresMax = new Scalar(180, 255, 255);

            private static final String TAG = "OpencvActivity";

    private JavaCameraView mJavaCameraView;
    Mat mRgb,mGrey,mCanny;

    ImageView img,imgFiltered;
    TextView tvTranslated;

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

        img=(ImageView)findViewById(R.id.img_original_);
        imgFiltered=(ImageView)findViewById(R.id.img_filtered_);
        tvTranslated=(TextView)findViewById(R.id.tv_translated);
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

        camHeight = height;
        camWidth = width;
        frameDim = height * width;
        rgbaFrame = new Mat();
        rgbFrame = new Mat();
        hsvFrame = new Mat();
        filteredFrame = new Mat();
        inRangeMask = new Mat();
        hChannel = new Mat();

    }

    @Override
    public void onCameraViewStopped() {

        mRgb.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        {
            // The frame currently captured by the camera, converted in the color RGBA
            rgbaFrame = inputFrame.rgba();

            // Convert the frame in the HSV color space, to be able to identify the color with the thresholds
            Imgproc.cvtColor(rgbaFrame, rgbFrame, Imgproc.COLOR_RGBA2RGB); // Cant't convert directly rgba->hsv
            Imgproc.cvtColor(rgbFrame, hsvFrame, Imgproc.COLOR_RGB2HSV);

            // Create a mask with ONLY zones of the chosen color on the frame currently captured
            Core.inRange(hsvFrame, thresMin, thresMax, inRangeMask);
            filteredFrame.setTo(new Scalar(0, 0, 0));
            rgbFrame.copyTo(filteredFrame, inRangeMask);

            // if the method of shooting image is set to manual, exit and return the filtered image...
            if(!methodAuto){ return filteredFrame; }

            //...else it was setted the automatic method, so continue with the method
            // Check the H channel of the image to see if the searched color is present on the frame
            Core.extractChannel(filteredFrame, hChannel, 0);

		/* There are two method to verify the color presence; below a little explanation */

		/* checkRange: if almost one pixel of the searched color is found, continue with the countdown
		 * Pro -> fast.
		 * Versus -> less accurate, possible presence of false positive depending the quality of the camera
		 * if(!Core.checkRange(hChannel, true, 0, 1)){ */

		/* Percentage: count the pixel of the searched color, and if there are almost the
		 * 0.1% of total pixel of the frame with the searched color, continue with the countdown
		 * Pro: more accurate, lower risk of false positive
		 * Versus: slower than checkRange
		 * N.B.: the threshold percentage is imposted with a low value, otherwise small object will not be seen */

            int perc = Core.countNonZero(hChannel); // Percentage
            if(perc > (frameDim * 0.001))
            {
                // if the shooting method is setted to 'immediate', the photo is returned now;
                // otherwise continue with the countdown
                if(!countDown){ takePicture1(); return rgbaFrame; }

                // 'point' is where the countdown will be visualized; in that case at
                //  a quarter of height and width than left up angle
                Point point = new Point(rgbaFrame.cols() >> 2 ,rgbaFrame.rows() >> 2);

                // Update the osd countdown every 75*8 ms (if color searched is present)
                // Use the division in 75 ms cause a higher value would give the user the feeling of screen/app 'blocked'.
                if(timeToElapse % 8 == 0)
                {
                    if(osdSecond.compareTo("") == 0) osdSecond = ((Integer)(timeToElapse >> 3)).toString();
                    else osdSecond = osdSecond.concat(".." + (((Integer)(timeToElapse >> 3)).toString()));
                    //Core.putText(rgbaFrame, osdSecond, point, 1, 3, Scalar.all(255));
                }
                timeToElapse -= 1;

                // the user has framed an object for more than 3 seconds; shoot the photo
                if(timeToElapse <= 0)
                {
                    timeToElapse = 24;
                    takePicture1();
                }
                // the user has framed an object for less than 3 seconds; wait
                else
                {
                    try { synchronized (this){ wait(75); } }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
            // the user has NOT framed a color searched object; reset osd
            else
            {
                timeToElapse = 24;
                osdSecond = "";
            }
            return rgbaFrame;
        }

        /*mRgb=inputFrame.rgba();
        Imgproc.cvtColor(mRgb,mGrey,Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(mGrey,mCanny,50,100);
        return mCanny;*/
    }
     public void takePicture1(View view)
            {
                // Make bitmaps to display images and (if the user want) save them on storage memory
                bitmap = Bitmap.createBitmap(camWidth, camHeight, Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(rgbFrame, bitmap);
                if(bitmap==null){
                    Log.d(TAG, "takePicture1: =null");
                }else{
                    Log.d(TAG, "takePicture1: !=null");
                }

                bitmapMask = Bitmap.createBitmap(camWidth, camHeight, Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(filteredFrame, bitmapMask);
                if(bitmapMask==null){
                    Log.d(TAG, "takePicture1: =null");
                }else{
                    Log.d(TAG, "takePicture1: !=null");
                }


                Log.d(TAG, "takePicture1:with views starts");

                img.setImageBitmap(bitmap);
                imgFiltered.setImageBitmap(bitmapMask);
                String translated=OcrUtils.getText(bitmap);

                tvTranslated.setText(translated);

                // Showing the image at the user, and ask if save them or not; the response will be processed on method onActivityResult
                //Intent intent = new Intent(this, CapturedFrameActivity.class);
                //startActivityForResult(intent, 1);
            }


            public void takePicture1()
            {
                // Make bitmaps to display images and (if the user want) save them on storage memory
                bitmap = Bitmap.createBitmap(camWidth, camHeight, Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(rgbFrame, bitmap);

                bitmapMask = Bitmap.createBitmap(camWidth, camHeight, Bitmap.Config.ARGB_8888) ;
                Utils.matToBitmap(filteredFrame, bitmapMask);



                Log.d(TAG, "takePicture1:no views starts");

                // Showing the image at the user, and ask if save them or not; the response will be processed on method onActivityResult
                img.setImageBitmap(bitmap);
                imgFiltered.setImageBitmap(bitmapMask);
            }





}
