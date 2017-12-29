package com.decrediton.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.decrediton.Activities.EncryptWallet;


import dcrwallet.Dcrwallet;

public class EncryptBackgroundWorker extends AsyncTask<String,String, String> {
    private ProgressDialog pd;
    private EncryptWallet context;

    public EncryptBackgroundWorker(ProgressDialog pd, EncryptWallet context){
        this.pd = pd;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return Dcrwallet.createWallet(params[0], params[1]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if(pd.isShowing()){
            pd.dismiss();
        }
        context.encryptWalletCallback(s);
    }
}
