package com.decrediton.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.decrediton.Activities.EncryptWallet;
import com.decrediton.Util.PreferenceUtil;
import com.decrediton.fragments.OverviewFragment;


import dcrwallet.BlockScanResponse;
import dcrwallet.ConstructTxResponse;
import dcrwallet.Dcrwallet;

public class EncryptBackgroundWorker extends AsyncTask<String,Integer, String> implements BlockScanResponse{
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
        }else if(values[0] == 2){
            pd.setMessage("Fetching Headers");
        }else if(values[0] == 3){
            pd.setMessage("Connecting to dcrd");
        }else if(values[0] == 4){
            PreferenceUtil util = new PreferenceUtil(context);
            int percentage = (int) ((values[1]/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
            pd.setMessage("Scanning Blocks "+percentage+"%");
        }else if(values[0] == 5){
            pd.setMessage("Subscribing to block notifications");
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            publishProgress(0);
            String createResponse =  Dcrwallet.createWallet(params[0], params[1]);
            String dcrdAddress = "127.0.0.1:9109";
            if(Dcrwallet.isTestNet()){
                dcrdAddress = "127.0.0.1:19109";
            }
            publishProgress(3);
            for(;;) {
                if(Dcrwallet.connectToDcrd(dcrdAddress)){
                    break;
                }
            }
            publishProgress(5);
            Dcrwallet.subscibeToBlockNotifications();
            publishProgress(1);
            Dcrwallet.discoverAddresses(params[0]);
            PreferenceUtil util = new PreferenceUtil(context);
            util.set("key", params[0]);
            util.set("discover_address","true");
            publishProgress(2);
            int blockHeight = Dcrwallet.fetchHeaders();
            if(blockHeight != -1){
                util.set(PreferenceUtil.BLOCK_HEIGHT,String.valueOf(blockHeight));
            }
            Dcrwallet.reScanBlocks(EncryptBackgroundWorker.this);
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

    @Override
    public void onEnd(long height) {
        publishProgress(4, (int)height);
    }

    @Override
    public void onScan(long rescanned_through) {
        publishProgress(4, (int)rescanned_through);
    }
}
