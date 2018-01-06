package com.decrediton.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.decrediton.Activities.EncryptWallet;


import dcrwallet.Dcrwallet;

public class EncryptBackgroundWorker extends AsyncTask<String,Integer, String> {
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
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if(values[0] == 0){
            pd.setMessage("Creating Wallet");
        }else if(values[0] == 1){
            pd.setMessage("Discovering Addresses");
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            publishProgress(0);
            String createResponse =  Dcrwallet.createWallet(params[0], params[1]);
            publishProgress(1);
            Dcrwallet.discoverAddresses(params[0]);
            return createResponse;
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
