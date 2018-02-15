package com.dcrandroid.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.dcrandroid.activities.ConfirmSeedActivity;
import com.dcrandroid.R;
import com.dcrandroid.util.Utils;

import dcrwallet.Dcrwallet;

public class VerifySeedBackgroundWorker extends AsyncTask<String,String, String> {
    private ConfirmSeedActivity context;
    private ProgressDialog pd;
    public VerifySeedBackgroundWorker(ConfirmSeedActivity context){
        this.context = context;
        this.pd = Utils.getProgressDialog(context,false,false,context.getString(R.string.verifying_seed));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return Dcrwallet.verifySeed(params[0]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if(pd.isShowing()){
            pd.dismiss();
        }
        context.verifySeedCallback(s);
    }
}
