package com.pethoalpar.androidtesstwoocr;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class TranslationUtils{
    private static final String TAG = "TranslationUtils";
    private static Context mContext;
    private static TranslationUtils mInstance;
    private TranslationUtils(Context context){
        mContext=context;
    }
    public static TranslationUtils getInstance(Context context){
        if(mInstance==null)
            mInstance=new TranslationUtils(context);
        return mInstance;
    }

    public  String[]  getTranslation(final String scannedTxt){
         final String[] translation=new String[1];
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Endpoints.TRANSLATION_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onPostExecute tr "+response);
                        translation[0]=response;
                        Log.d(TAG, "getTranslation: after response "+translation[0]);



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


        Log.d(TAG, "getTranslation: before return "+translation[0]);
        RequestHandler.getInstance(mContext).addToRequestQueue(stringRequest);
        Log.d(TAG, "getTranslation: return "+translation[0]);
        return translation;

    }


    public void getTranslationResponse(final String scannedTxt, final VolleyCallback callback) {



        StringRequest strreq = new StringRequest(Request.Method.POST,
                Endpoints.TRANSLATION_URL,
                new Response.Listener < String > () {

            @Override
            public void onResponse(String Response) {
                callback.onSuccessResponse(Response);
            }
        }, new Response.ErrorListener() {
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
        RequestHandler.getInstance(mContext).addToRequestQueue(strreq);
    }

}