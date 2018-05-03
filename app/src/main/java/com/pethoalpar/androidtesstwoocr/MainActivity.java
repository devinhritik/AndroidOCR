package com.pethoalpar.androidtesstwoocr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CAMERA = 1000;
    public static final int REQUEST_GALLERY = 1001;
    private ImageView ivRequired;

    private String scannedTxt="";
    private String translatedTxt="";
    final String[] translationString=new String[1];

    private TextView tvScanned,tvTranslated;
    private ProgressDialog dialog;

    private Bitmap mImageBitmap;
    private String mCurrentPhotoPath;

    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = "MainActivity";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";

    private  TessBaseAPI tessBaseAPI;

    private OcrUtils mOcrUtils=OcrUtils.getInstance();


    Mat imageMat=new Mat();
    Mat imageMat2=new Mat();


    static{
        if(OpenCVLoader.initDebug())
            Log.d(TAG, "static initializer: opencv success");
        else
            Log.d(TAG, "static initializer: opencv error happened");
    }


//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        textView = (TextView) this.findViewById(R.id.textView);
//        textViewTranslated = (TextView) this.findViewById(R.id.textViewTranslated);
//        final Activity activity = this;
//        checkPermission();
//        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                checkPermission();
//                dispatchTakePictureIntent();
//            }
//        });
//
//        btnOpencv=(Button)findViewById(R.id.btn_opencv);
//        ivGrey=(ImageView)findViewById(R.id.iv_grey);
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_activity_main);
        dialog=new ProgressDialog(this);


        checkPermission();
        ivRequired=(ImageView)findViewById(R.id.iv_required_img);
        tvScanned=(TextView)findViewById(R.id.tv_scanned);
        tvTranslated=(TextView)findViewById(R.id.tv_translated);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialog.dismiss();
    }

    public void chooseImg(View v){


        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, REQUEST_GALLERY);

    }

    public void captureImg(View v){
       Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
       startActivityForResult(intent, REQUEST_CAMERA);


    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 122);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        Bitmap bitmapImg=null;
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_CANCELED) {
            Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == REQUEST_GALLERY) {
            tvScanned.setText("");
            tvTranslated.setText("");
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    bitmapImg=bitmap;
                    //String path = saveImage(bitmap);
                    Toast.makeText(MainActivity.this, "Image Choosen!", Toast.LENGTH_SHORT).show();
                    ivRequired.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == REQUEST_CAMERA) {
            tvScanned.setText("");
            tvTranslated.setText("");
            Bitmap thumbnail=(Bitmap) data.getExtras().get("data");

            ivRequired.setImageBitmap(thumbnail);

                      bitmapImg=thumbnail;



        }

        if(bitmapImg!=null) {

            ExtractionUtils task=new ExtractionUtils();
            task.execute(bitmapImg);


        }

//        if((requestCode == REQUEST_GALLERY||requestCode==REQUEST_CAMERA )&& resultCode == Activity.RESULT_OK) {
//            //BitmapFactory.Options options = new BitmapFactory.Options();
//            //options.inSampleSize = 10;
//            //Bitmap bitmap = BitmapFactory.decodeFile(outputFileDir.getPath(), options);
//
//            Uri photoUri=(Uri)data.getExtras().get(MediaStore.EXTRA_OUTPUT);
//            if(photoUri==null){
//                Log.d(TAG, "onActivityResult: error");
//            }else{
//                Log.d(TAG, "onActivityResult: not null ");
//            }
//
//            prepareTessData();
//            Bitmap bitmap = startOCR();
//            ivRequired.setImageBitmap(bitmap);
//            if (bitmap != null) {
//                Log.d(TAG, "onActivityResult: bitmap !=null");
//
//                ExifInterface ei = null;
//                try {
//                    ei = new ExifInterface(mCurrentPhotoPath);
//                } catch (IOException e) {
//                    Log.d(TAG, "IO problem");
//                }
//                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
//                switch (orientation) {
//                    case ExifInterface.ORIENTATION_ROTATE_90:
//                        bitmap = rotateImage(bitmap, 90);
//                        break;
//                    case ExifInterface.ORIENTATION_ROTATE_180:
//                        bitmap = rotateImage(bitmap, 180);
//                        break;
//                    case ExifInterface.ORIENTATION_ROTATE_270:
//                        bitmap = rotateImage(bitmap, 270);
//                        break;
//                    case ExifInterface.ORIENTATION_NORMAL:
//                    default:
//                        break;
//                }
//
//                String txt=processImg(bitmap);
//                Log.d(TAG, "onActivityResult: txt "+txt);
//                Toast.makeText(this,"tr + "+txt,Toast.LENGTH_LONG).show();
//
//                /*Utils.bitmapToMat(bitmap, imageMat);
//                detectText(imageMat);
//                Bitmap newBitmap = bitmap.copy(bitmap.getConfig(), true);
//                Utils.matToBitmap(imageMat, newBitmap);*/
//                //ivGrey.setImageBitmap(bitmap);
//            } else {
//                Toast.makeText(getApplicationContext(), "Problem", Toast.LENGTH_SHORT).show();
//            }
//        }else{
//            Log.d(TAG, "onActivityResult: bitmap == null");
//        }
    }

    private Bitmap rotateImage (Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source,0 ,0, source.getWidth(), source.getHeight(), matrix, true);
    }
    private void detectText(Mat mat){
        Imgproc.cvtColor(imageMat, imageMat2, Imgproc.COLOR_RGB2GRAY);
        Mat mRgba = mat;
        Mat mGray = imageMat2;

        Scalar CONTOUR_COLOR = new Scalar(1, 255, 128, 0);
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();

        List<KeyPoint> listPoint = new ArrayList<>();
        KeyPoint kPoint = new KeyPoint();
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;

        Scalar zeros = new Scalar(0,0,0);
        List<MatOfPoint> contour2 = new ArrayList<>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morByte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3 = new Rect();
        int imgSize = mRgba.height() * mRgba.width();

        if(true){
            FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);

            detector.detect(mGray, keyPoint);
            listPoint = keyPoint.toList();
            for(int ind = 0; ind < listPoint.size(); ++ind){
                kPoint = listPoint.get(ind);

                rectanx1 = (int ) (kPoint.pt.x - 0.5 * kPoint.size);
                rectany1 = (int ) (kPoint.pt.y - 0.5 * kPoint.size);

                rectanx2 = (int) (kPoint.size);
                rectany2 = (int) (kPoint.size);
                if(rectanx1 <= 0){
                    rectanx1 = 1;
                }
                if(rectany1 <= 0){
                    rectany1 = 1;
                }
                if((rectanx1 + rectanx2) > mGray.width()){
                    rectanx2 = mGray.width() - rectanx1;
                }
                if((rectany1 + rectany2) > mGray.height()){
                    rectany2 = mGray.height() - rectany1;
                }
                Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            }
            Imgproc.morphologyEx(mask, morByte, Imgproc.MORPH_DILATE, kernel);
            Imgproc.findContours(morByte, contour2, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            for(int i = 0; i<contour2.size(); ++i){
                rectan3 = Imgproc.boundingRect(contour2.get(i));
                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
                Mat m1=new Mat(mGray,rectan3);

                Bitmap rectBitmap=Bitmap.createBitmap(m1.width(),m1.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(m1,rectBitmap);
                //String txt=getText(rectBitmap);
                String txt = OcrUtils.getText(rectBitmap);
                Log.d(TAG, "detectText: detected "+txt);




                /*if(rectan3.area() > 0.5 * imgSize || rectan3.area()<100 || rectan3.width / rectan3.height < 2){
                    Mat roi = new Mat(morByte, rectan3);
                    roi.setTo(zeros);
                }else{
                    Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
                }*/
            }
        }
    }




    private  String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";

//        Mat mat=new Mat();
//        Mat matResized=new Mat();
//        Mat matCopy=new Mat();
//        Mat matGrey=new Mat();
//        Mat matThresh=new Mat();
//        Mat matMedian=new Mat();
//        Mat matGaussian=new Mat();
//        Mat matCanny=new Mat();
//        Mat matHieracy=new Mat();
//        Mat matWrapped=new Mat();



//        Utils.bitmapToMat(bitmap,mat);
//        Utils.bitmapToMat(bitmap,matCopy);
//
//        List<MatOfPoint> cnts=new ArrayList<>();
//        List<MatOfPoint> cntsSorted=new ArrayList<>();
//        //Size sz=new Size(mat.width(),500)
//        double ratio=mat.width()/500.0;
//        //Imgproc.resize(mat,matResized,new Size(mat.width(),500));
//
//        Imgproc.cvtColor(mat,matGrey,COLOR_RGB2GRAY);
//        Imgproc.GaussianBlur(matGrey,matGaussian,new Size(5,5),0);
//        //Imgproc.Canny(matGaussian,matCanny,75,200);
//
//
//
//
//        /*Imgproc.findContours(matCanny,cnts,matHieracy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
//        Collections.sort(cnts,Collections.<MatOfPoint>reverseOrder());
//        cntsSorted=cnts.subList(0,6);
//        for(MatOfPoint c :cnts){
//            c.convertTo(c, CvType.CV_32FC2);
//            Imgproc.arcLength(c,true);
//
//        }*/
//
//        Imgproc.threshold(matGaussian,matThresh,0,255,Imgproc.THRESH_OTSU);
//        //Imgproc.medianBlur(matThresh,matMedian,3);
//
//        Bitmap greyBitmap=Bitmap.createBitmap(matThresh.width(),matMedian.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matThresh,greyBitmap);
//        ivGrey.setImageBitmap(greyBitmap);

        OcrUtils.setParams(tessBaseAPI,dataPath);

        //ProcessingUtils utils=new ProcessingUtils();
       // utils.execute(bitmap);



        //String unTranslated= OcrUtils.getText(bitmap);
        //textView.setText(unTranslated);

        //TranslationUtils utils=new TranslationUtils();
        //utils.execute(unTranslated);

        //textViewTranslated.setText(utils.getTranslatedStr());
        return "Hello";

    }


    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        Log.d(TAG, "onPostExecute :getPost  "+result.toString());
        return result.toString();
    }




    public class ExtractionUtils extends AsyncTask<Bitmap, Void, String[]> {

        // private String translatedStr = "Still no result";

        protected void onPreExecute() {
            Log.d(TAG, "onPreExecute: starts");
            dialog.setMessage("Extracting...");
            dialog.show();
        }

        protected String[] doInBackground(Bitmap... arg0) {
            Bitmap bitmapImg=arg0[0];
            String scanned=processImg2(bitmapImg);
            //tvScanned.setText(scannedTxt);
            Log.d(TAG, "onPostExecute : bitmap "+scannedTxt);
            getTranslation(scanned);//also sets
            Log.d(TAG, "doInBackground: tr "+translationString[0]);
            return new String[]{scanned};
        }

        @Override
        protected void onPostExecute(String result[]) {
            tvScanned.setText(result[0]);
            //tvTranslated.setText(translationString[0]);
            webServiceTranslation(result[0]);
            dialog.dismiss();

        }

    }



    /**
     * IMPORTANT METHOD
     *
     *
     * */
    private void prepareTessData(){
        try{
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private  void initTess() {
        try {
            tessBaseAPI = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";



        prepareTessData();
        OcrUtils.setParams(tessBaseAPI, dataPath);

    }

    public String processImg(Bitmap bitmap){

         int sourceWidth = 1366; // To scale to
         int thresholdMin = 85; // Threshold 80 to 105 is Ok
        int thresholdMax = 255; // Always 255

        Mat origin = new Mat();
        Utils.bitmapToMat(bitmap, origin);
        Size imgSize = origin.size();
        resize(origin, origin, new Size(sourceWidth, imgSize.height * sourceWidth / imgSize.width), 1.0, 1.0,
                INTER_CUBIC);

        // Convert the image to GRAY
        Mat grayMat = new Mat();
        cvtColor(origin, grayMat, COLOR_BGR2GRAY);

        // Process noisy, blur, and threshold to get black-white image
        //grayMat = processNoisy(originGray);

        Mat element1 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));
        Mat element2 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));
        dilate(grayMat, grayMat, element1);
        erode(grayMat, grayMat, element2);

        GaussianBlur(grayMat, grayMat, new Size(3, 3), 0);
        // The thresold value will be used here
        threshold(grayMat, grayMat, thresholdMin, thresholdMax, THRESH_BINARY);


        int newWidth = grayMat.width()/2;
        resize(grayMat, grayMat, new Size(newWidth, (grayMat.height() * newWidth) / grayMat.width()));


        Bitmap rectBitmap=Bitmap.createBitmap(grayMat.width(),grayMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayMat,rectBitmap);
        //ivGrey.setImageBitmap(rectBitmap);
        initTess();
        return OcrUtils.getText(rectBitmap);
        //Tess
    }


    protected String processImg2(Bitmap bitmap) {


        Mat mat=new Mat();

        Mat matCopy=new Mat();
        Mat matGrey=new Mat();
        Mat matThresh=new Mat();
        Mat matMedian=new Mat();
        Mat matGaussian=new Mat();


        Utils.bitmapToMat(bitmap,mat);
        //Utils.bitmapToMat(bitmap,matCopy);


        Imgproc.cvtColor(mat,matGrey,COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(matGrey,matGaussian,new Size(5,5),0);
        //Imgproc.Canny(matGaussian,matCanny,75,200);





        Imgproc.threshold(matGaussian,matThresh,0,255,Imgproc.THRESH_OTSU);
        //Imgproc.medianBlur(matThresh,matMedian,3);



        Bitmap greyBitmap=Bitmap.createBitmap(matThresh.width(),matThresh.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matThresh,greyBitmap);
        //ivGrey.setImageBitmap(greyBitmap);
        initTess();
        return OcrUtils.getText(greyBitmap);


    }


    public  void  getTranslation(final String scannedTxt){

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Endpoints.TRANSLATION_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onPostExecute tr "+response);
                        translationString[0]=response;
                        Log.d(TAG, "onResponse: tr "+translationString[0]);



                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: starts "+error.toString());

                        // Toast.makeText(mContext, "unknown error  error is  "+error.toString(), Toast.LENGTH_LONG).show();

                    }
                }
        ){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Log.d(TAG, "getParams: starts with "+scannedTxt);


                Map<String, String> params = new HashMap<>();
                params.put("scannedText", scannedTxt);


                return params;
            }

        };


        //Log.d(TAG, "getTranslation: before return "+translation[0]);
        RequestHandler.getInstance(this).addToRequestQueue(stringRequest);
       // Log.d(TAG, "getTranslation: return "+translation[0]);


    }


    public void webServiceTranslation(final String text) {



        TranslationUtils.getInstance(this).getTranslationResponse(text,
                new VolleyCallback() {
                    @Override
                    public void onSuccessResponse(String result) {

                            tvTranslated.setText(result);

                    }
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cam_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.action_settings){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
