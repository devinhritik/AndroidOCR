//package com.pethoalpar.androidtesstwoocr;
//
//import android.os.AsyncTask;
//import android.util.Log;
//
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.util.Iterator;
//
//import javax.net.ssl.HttpsURLConnection;
//
///**
// * Created by Speed on 20/04/2018.
// */
//
//public class TranslationUtils extends AsyncTask<String, Void, String> {
//
//    private String translatedStr = "Still no result";
//
//    protected void onPreExecute() {
//    }
//
//    protected String doInBackground(String... arg0) {
//        String name = arg0[0];
//
//        try {
//
//            //URL url = new URL("https://script.google.com/macros/s/AKfycbwht8Fh8CMY4qmCgThl3hXZb-bLoCsX3jEWOae4LKMe/dev");
//            URL url = new URL("https://script.google.com/macros/s/AKfycbzLF-oc6IaPnwTzJsWqDgrl0KOT7gFF0e_KIffv1nprvhHKIko/exec");
//            // https://script.google.com/macros/s/AKfycbyuAu6jWNYMiWt9X5yp63-hypxQPlg5JS8NimN6GEGmdKZcIFh0/exec
//            JSONObject postDataParams = new JSONObject();
//
//            //int i;
//            //for(i=1;i<=70;i++)
//
//
//            //    String usn = Integer.toString(i);
//
//
//            postDataParams.put("name", name);
//
//
//            Log.e("params", postDataParams.toString());
//
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setReadTimeout(15000 /* milliseconds */);
//            conn.setConnectTimeout(15000 /* milliseconds */);
//            conn.setRequestMethod("POST");
//            conn.setDoInput(true);
//            conn.setDoOutput(true);
//
//            OutputStream os = conn.getOutputStream();
//            BufferedWriter writer = new BufferedWriter(
//                    new OutputStreamWriter(os, "UTF-8"));
//            writer.write(getPostDataString(postDataParams));
//
//            writer.flush();
//            writer.close();
//            os.close();
//
//            int responseCode = conn.getResponseCode();
//
//            if (responseCode == HttpsURLConnection.HTTP_OK) {
//
//                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                StringBuffer sb = new StringBuffer("");
//                String line = "";
//
//                while ((line = in.readLine()) != null) {
//
//                    sb.append(line);
//                    break;
//                }
//
//                in.close();
//                return sb.toString();
//
//            } else {
//                return new String("false : " + responseCode);
//            }
//        } catch (Exception e) {
//            return new String("Exception: " + e.getMessage());
//        }
//    }
//
//    @Override
//    protected void onPostExecute(String result) {
//        //Toast.makeText(getApplicationContext(), result,
//        //Toast.LENGTH_LONG).show();
//        Log.d("onPostExecute: ", result);
//        translatedStr = result;
//
//    }
//
//}
//
//    public String getTranslatedStr(){
//        return translatedStr;
//    }
//
//
//}
