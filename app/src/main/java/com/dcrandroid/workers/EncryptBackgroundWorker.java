package com.dcrandroid.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.dcrandroid.activities.EncryptWallet;
import com.dcrandroid.R;
import com.dcrandroid.activities.SplashScreen;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;


import dcrwallet.BlockScanResponse;
import dcrwallet.Dcrwallet;

public class EncryptBackgroundWorker extends AsyncTask<String,Integer, String> implements BlockScanResponse{
    private ProgressDialog pd;
    private EncryptWallet context;
    PreferenceUtil util;
    public EncryptBackgroundWorker(ProgressDialog pd, EncryptWallet context){
        this.pd = pd;
        this.context = context;
        this.util = new PreferenceUtil(context);
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
            pd.setMessage(context.getString(R.string.creating_wallet));
        }else if(values[0] == 1){
            pd.setMessage(context.getString(R.string.discovering_address));
        }else if(values[0] == 2){
            pd.setMessage(context.getString(R.string.fetching_headers));
        }else if(values[0] == 3){
            pd.setMessage(context.getString(R.string.conecting_to_dcrd));
        }else if(values[0] == 4){
            try {
                //int percentage = (int) ((values[1] / Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
                pd.setMessage(context.getString(R.string.scanning_blocks) + " "+ values[1]);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else if(values[0] == 5){
            pd.setMessage(context.getString(R.string.subscribing_to_block_notification));
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            PreferenceUtil util = new PreferenceUtil(context);
            publishProgress(0);
            String createResponse =  Dcrwallet.createWallet(params[0], params[1]);
            //Utils.setDcrwalletConfig("noinitialload","false");
//            String dcrdAddress = Utils.getDcrdNetworkAddress(context);
//            if(util.getInt("network_mode") != 0) {
//                publishProgress(3);
//                for (; ; ) {
//                    if (Dcrwallet.connectToDcrd(dcrdAddress, Utils.getConnectionCertificate(context).getBytes())) {
//                        break;
//                    }
//                }
//            }
            if(util.getInt("network_mode") == 0){
                System.out.println("Connecting to peer");
                Dcrwallet.connectToPeer(util.get("peer_address"));
            }
            publishProgress(5);
            Dcrwallet.subscibeToBlockNotifications();
            publishProgress(1);
            Dcrwallet.discoverAddresses(params[0]);
            util.set("key", params[0]);
            util.set("discover_address","true");
            publishProgress(2);
            int blockHeight = Dcrwallet.fetchHeaders();
            if(blockHeight != -1){
                util.set(PreferenceUtil.BLOCK_HEIGHT,String.valueOf(blockHeight));
            }
            Dcrwallet.reScanBlocks(EncryptBackgroundWorker.this,0);
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
