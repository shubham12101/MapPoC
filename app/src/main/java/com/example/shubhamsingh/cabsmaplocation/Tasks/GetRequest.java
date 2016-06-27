package com.example.shubhamsingh.cabsmaplocation.Tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.shubhamsingh.cabsmaplocation.Commons.CommonUtils;
import com.example.shubhamsingh.cabsmaplocation.Listeners.GetRequestListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by shubhamsingh on 17/06/16.
 */
public class GetRequest <T> extends AsyncTask<Void, Void, String> {
    private static String TAG = GetRequest.class.getSimpleName();

    private static Context context;
    private static URL url;
    private static HttpURLConnection httpURLConnection;
    private static GetRequestListener getRequestListener;
    private Class<T> classType;

    public GetRequest(Context context, GetRequestListener getRequestListener, String url, Class<T> t) throws IOException {
        this.context = context;
        this.getRequestListener = getRequestListener;
        this.url = new URL(url);
        this.classType = t;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setReadTimeout(10000 /* milliseconds */);
            httpURLConnection.setConnectTimeout(15000 /* milliseconds */);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();
            int status = httpURLConnection.getResponseCode();
            Log.d(TAG, "The response status is: " + status);

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Object obj = CommonUtils.getGsonFromString(s, classType);
        getRequestListener.onGetTaskCompleted(obj);
    }
}
