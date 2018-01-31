package com.decrediton.workers;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.decrediton.Activities.EncryptWallet;
import com.decrediton.R;
import com.decrediton.Util.PreferenceUtil;
import com.decrediton.fragments.OverviewFragment;


import dcrwallet.BlockScanResponse;
import dcrwallet.ConstructTxResponse;
import dcrwallet.Dcrwallet;

public class EncryptBackgroundWorker extends AsyncTask<String,Integer, String> implements BlockScanResponse{
    private ProgressDialog pd;
    private EncryptWallet context;
    String cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIICaDCCAcmgAwIBAgIRAN4bL47kMs4/Z6jaHXJV0AgwCgYIKoZIzj0EAwQwNjEg\n" +
            "MB4GA1UEChMXZGNyZCBhdXRvZ2VuZXJhdGVkIGNlcnQxEjAQBgNVBAMTCWxvY2Fs\n" +
            "aG9zdDAeFw0xODAxMDMwODExMjNaFw0yODAxMDIwODExMjNaMDYxIDAeBgNVBAoT\n" +
            "F2RjcmQgYXV0b2dlbmVyYXRlZCBjZXJ0MRIwEAYDVQQDEwlsb2NhbGhvc3QwgZsw\n" +
            "EAYHKoZIzj0CAQYFK4EEACMDgYYABABDkKzGKGPaTc3JG/TSkYPZsYiTl0qgK323\n" +
            "YWqs/UqimHPEN96u7ZG8HF7Mrx3YUNtOIS+4ewNwwQvha9/EaoWYcQEpzs6okd0O\n" +
            "A6kdbaVPyeLBzjcCvIY9wuLOAxBnYi9DoSl6cyJwXPeu2pbYzAYL3d0GFjUOSGlG\n" +
            "yPXBzskA0HwCC6N1MHMwDgYDVR0PAQH/BAQDAgKkMA8GA1UdEwEB/wQFMAMBAf8w\n" +
            "UAYDVR0RBEkwR4IJbG9jYWxob3N0hwR/AAABhxAAAAAAAAAAAAAAAAAAAAABhxD+\n" +
            "gAAAAAAAAKjZe//+gAW3hxD+gAAAAAAAAFjFg//+fas3MAoGCCqGSM49BAMEA4GM\n" +
            "ADCBiAJCAROEPRrzAVumn9zRoX+lHQrv1CCrbeJaCHVzxr7g2TqgHdn2UwmC0Jxz\n" +
            "j+WtcOAQwabqFb5kwZc6+uOfxsINfdC+AkIBcfvF8y8fYDkFCXHTxxnMaxkvJki8\n" +
            "Y2OFjX9Uxgzn9isY4TeLWo0lfThwU93VtSPC0QLGEjXCG6JU2xpwgxvqGUU=\n" +
            "-----END CERTIFICATE-----\n";
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
        PreferenceUtil util = new PreferenceUtil(context);
        if(values[0] == 0){
            pd.setMessage(context.getString(R.string.creating_wallet));
        }else if(values[0] == 1){
            pd.setMessage(context.getString(R.string.discovering_address));
        }else if(values[0] == 2){
            pd.setMessage(context.getString(R.string.fetching_headers));
        }else if(values[0] == 3){
            pd.setMessage(context.getString(R.string.conecting_to_dcrd));
        }else if(values[0] == 4){
            int percentage = (int) ((values[1]/Float.parseFloat(util.get(PreferenceUtil.BLOCK_HEIGHT))) * 100);
            pd.setMessage(context.getString(R.string.scanning_blocks)+percentage+"%");
        }else if(values[0] == 5){
            pd.setMessage(context.getString(R.string.subscribing_to_block_notification));
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            publishProgress(0);
            String createResponse =  Dcrwallet.createWallet(params[0], params[1]);
            String dcrdAddress = Dcrwallet.isTestNet() ? context.getString(R.string.dcrd_address_testnet) : context.getString(R.string.dcrd_address);
            publishProgress(3);
            for(;;) {
                if(Dcrwallet.connectToDcrd(dcrdAddress, cert.getBytes())){
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
