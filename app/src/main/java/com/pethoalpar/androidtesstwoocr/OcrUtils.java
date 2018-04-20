package com.pethoalpar.androidtesstwoocr;

import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by Speed on 20/04/2018.
 */

public class OcrUtils {
    private static final String TAG = "OcrUtils";
    private static OcrUtils instance;
    static TessBaseAPI mTessBaseAPI;
    static String mDataPath;

    private OcrUtils(){

    }
    public static OcrUtils getInstance(){
        Log.d(TAG, "getInstance: called");
        if(instance==null)
            instance=new OcrUtils();
        return instance;
    }
    public static void setParams(TessBaseAPI tessBaseAPI,String dataPath){
        mTessBaseAPI=tessBaseAPI;
        mDataPath=dataPath;
        mTessBaseAPI.init(dataPath, "eng");

    }

    public static String getText(Bitmap bitmap){
        String retStr = "No result";
        if(mTessBaseAPI!=null){
            try{
                mTessBaseAPI.setImage(bitmap);
            }catch (Exception e){
                Log.d(TAG, "getText: ex happened");
                Log.d(TAG, "getText: error + "+e.getMessage());
                e.printStackTrace();
            }


            try{
                retStr = mTessBaseAPI.getUTF8Text();
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
            mTessBaseAPI.end();
        }

        return retStr;
    }
}
