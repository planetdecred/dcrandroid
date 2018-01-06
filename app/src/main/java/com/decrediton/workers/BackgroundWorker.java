package com.decrediton.workers;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dcrwallet.Dcrwallet;

public class BackgroundWorker extends AsyncTask<Method,String, String> {
    private Method callback;
    private ProgressDialog pd;
    private Context context;
    private boolean showDialog;
    public BackgroundWorker(Method callback, ProgressDialog pd, Context context, boolean showDialog){
        this.callback = callback;
        this.pd = pd;
        this.context = context;
        this.showDialog = showDialog;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(showDialog) {
            pd.show();
        }
    }

    @Override
    protected String doInBackground(Method... methods) {
        for(Method method : methods){
            try {
                String value = (String) method.invoke(this);
                return value;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if(pd.isShowing()){
            pd.dismiss();
        }
        try {
            callback.invoke(context,s);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
