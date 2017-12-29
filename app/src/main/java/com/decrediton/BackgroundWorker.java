package com.decrediton;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BackgroundWorker extends AsyncTask<Method,String, String> {
    Method callback;
    ProgressDialog pd;
    Context context;
    public BackgroundWorker(Method callback, ProgressDialog pd, Context context){
        this.callback = callback;
        this.pd = pd;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd.show();
    }

    @Override
    protected String doInBackground(Method... methods) {
        for(Method method : methods){
            try {
                String seed = (String) method.invoke(this);
                return seed;
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
