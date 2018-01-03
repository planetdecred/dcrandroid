package com.decrediton.Activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.decrediton.R;

/**
 * Created by Macsleven on 02/01/2018.
 */

public class TransactionDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Transaction details ");
        setContentView(R.layout.transaction_details_view);

        TextView amount = findViewById(R.id.tx_dts_amount);
        TextView account = findViewById(R.id.tx_dts_acc);
        TextView date = findViewById(R.id.tx_date);
        TextView status = findViewById(R.id.tx_dts__status);
        TextView txType = findViewById(R.id.label_tx);
        TextView address = findViewById(R.id.tx_addr);

        amount.setText(getIntent().getStringExtra("Amount"));
        account.setText(getIntent().getStringExtra("AccountName"));
        date.setText(getIntent().getStringExtra("TxDate"));
        status.setText(getIntent().getStringExtra("TxStatus"));
        txType.setText(getIntent().getStringExtra("TxType"));
        address.setText(getIntent().getStringExtra("Address"));
        if(status.getText().toString().equals("pending")){
            status.setBackgroundResource(R.drawable.tx_status_pending);
            status.setTextColor(Color.parseColor("#3d659c"));
        }
        else if(status.getText().toString().equals("confirmed")) {
            status.setBackgroundResource(R.drawable.tx_status_confirmed);
            status.setTextColor(Color.parseColor("#55bb97"));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
