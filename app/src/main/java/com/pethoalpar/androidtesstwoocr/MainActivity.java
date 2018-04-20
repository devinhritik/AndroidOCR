package com.pethoalpar.androidtesstwoocr;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;

public class MainActivity extends AppCompatActivity {

    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private TextView textView,textViewTranslated;
    private  TessBaseAPI tessBaseAPI;
    private Uri outputFileDir;
    private String mCurrentPhotoPath;

    private Button btnOpencv;
    private OcrUtils mOcrUtils=OcrUtils.getInstance();

    private ImageView ivGrey;
    private ProgressDialog dialog;

    static{
        if(OpenCVLoader.initDebug())
            Log.d(TAG, "static initializer: opencv success");
        else
            Log.d(TAG, "static initializer: opencv error happened");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.textView);
        textViewTranslated = (TextView) this.findViewById(R.id.textViewTranslated);
        final Activity activity = this;
        checkPermission();
        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
                dispatchTakePictureIntent();
            }
        });

        btnOpencv=(Button)findViewById(R.id.btn_opencv);
        ivGrey=(ImageView)findViewById(R.id.iv_grey);
        dialog=new ProgressDialog(this);
    }
    public void openOpenCvActivity(View view){
        startActivity(new Intent(this,OpencvActivity.class));
    }
    public void openPostData(View view){
        Log.d(TAG, "openPostData: called");
        startActivity(new Intent(this,PostData.class));
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1024);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );


        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1024) {
            if (resultCode == Activity.RESULT_OK) {
                prepareTessData();
                startOCR(outputFileDir);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private void startOCR(Uri imageUri){
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 6;
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
            String result = this.getText(bitmap);
            //textView.setText(result);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private  String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";

        Mat mat=new Mat();
        Mat matGrey=new Mat();
        Utils.bitmapToMat(bitmap,mat);
        Imgproc.cvtColor(mat,matGrey,COLOR_RGB2GRAY);
        Bitmap greyBitmap=Bitmap.createBitmap(matGrey.width(),matGrey.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matGrey,greyBitmap);
        ivGrey.setImageBitmap(greyBitmap);

        OcrUtils.setParams(tessBaseAPI,dataPath);

        ExtractionUtils utils=new ExtractionUtils();
        utils.execute(bitmap);



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
        return result.toString();
    }


    public class TranslationUtils extends AsyncTask<String, Void, String> {

       // private String translatedStr = "Still no result";

        protected void onPreExecute() {
            dialog.setMessage("translating...");
            dialog.show();
        }

        protected String doInBackground(String... arg0) {
            String name = arg0[0];

            try {

                //URL url = new URL("https://script.google.com/macros/s/AKfycbwht8Fh8CMY4qmCgThl3hXZb-bLoCsX3jEWOae4LKMe/dev");
                URL url = new URL("https://script.google.com/macros/s/AKfycbzLF-oc6IaPnwTzJsWqDgrl0KOT7gFF0e_KIffv1nprvhHKIko/exec");
                // https://script.google.com/macros/s/AKfycbyuAu6jWNYMiWt9X5yp63-hypxQPlg5JS8NimN6GEGmdKZcIFh0/exec
                JSONObject postDataParams = new JSONObject();

                //int i;
                //for(i=1;i<=70;i++)


                //    String usn = Integer.toString(i);


                postDataParams.put("name", name);


                Log.e("params", postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";

                    while ((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return new String("false : " + responseCode);
                }
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result,
            //Toast.LENGTH_LONG).show();
            Log.d("onPostExecute: ", result);
            //translatedStr = result;

            textViewTranslated.setText(result);
            dialog.dismiss();

        }

    }

    public class ExtractionUtils extends AsyncTask<Bitmap, Void, String[]> {

        // private String translatedStr = "Still no result";

        protected void onPreExecute() {
            dialog.setMessage("Extracting...");
            dialog.show();
        }

        protected String[] doInBackground(Bitmap... arg0) {
            String name = OcrUtils.getText(arg0[0]);
            Log.d(TAG, "doInBackground: untranslated "+name);
            //textView.setText(name);

            String strRet[]=new String[2];
            strRet[0]=name;

            try {

                //URL url = new URL("https://script.google.com/macros/s/AKfycbwht8Fh8CMY4qmCgThl3hXZb-bLoCsX3jEWOae4LKMe/dev");
                URL url = new URL("https://script.google.com/macros/s/AKfycbzLF-oc6IaPnwTzJsWqDgrl0KOT7gFF0e_KIffv1nprvhHKIko/exec");
                // https://script.google.com/macros/s/AKfycbyuAu6jWNYMiWt9X5yp63-hypxQPlg5JS8NimN6GEGmdKZcIFh0/exec
                JSONObject postDataParams = new JSONObject();

                //int i;
                //for(i=1;i<=70;i++)


                //    String usn = Integer.toString(i);


                postDataParams.put("name", name);


                Log.e("params", postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";

                    while ((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    strRet[1]=sb.toString();
                    return strRet ;

                } else {
                    strRet[1]= new String("false : " + responseCode);
                    return strRet;
                }
            } catch (Exception e) {
                strRet[1]= new String("Exception: " + e.getMessage());
                return strRet;
            }
        }

        @Override
        protected void onPostExecute(String result[]) {
            //Toast.makeText(getApplicationContext(), result,
            //Toast.LENGTH_LONG).show();
            Log.d("onPostExecute: ", result[0]);
            Log.d("onPostExecute: ", result[1]);
            //translatedStr = result;

            textView.setText(result[0]);
            textViewTranslated.setText(result[1]);
            dialog.dismiss();

        }

    }
}
