package com.dcrandroid.util;

import com.dcrandroid.data.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by collins on 1/10/18.
 */

public class TransactionsResponse {
    private TransactionsResponse(){}
    public ArrayList<TransactionItem> transactions = new ArrayList<>();
    public boolean errorOccurred = true;
    public String errorMessage = "";
    public static TransactionsResponse parse(String json){
        TransactionsResponse response = new TransactionsResponse();
        try {
            JSONObject object = new JSONObject(json);
            response.errorOccurred = object.getBoolean("ErrorOccurred");
            if(response.errorOccurred){
                response.errorMessage = object.getString("ErrorMessage");
            }
            JSONArray transactions = object.getJSONArray(Constants.TRANSACTIONS);
            for(int i = 0; i < transactions.length(); i++){
                TransactionItem transaction = new TransactionItem();
                JSONObject tx = transactions.getJSONObject(i);
                ArrayList<TransactionOutput> outputs = new ArrayList<>();
                JSONArray cdt = tx.getJSONArray(Constants.CREDITS);
                for(int j = 0; j < cdt.length(); j++){
                    TransactionOutput output = new TransactionOutput();
                    output.account = cdt.getJSONObject(j).getInt(Constants.ACCOUNT);
                    output.internal = cdt.getJSONObject(j).getBoolean(Constants.INTERNAL);
                    output.address = cdt.getJSONObject(j).getString(Constants.ADDRESS);
                    output.index = cdt.getJSONObject(j).getInt(Constants.INDEX);
                    output.amount = cdt.getJSONObject(j).getLong(Constants.AMOUNT);
                    transaction.totalOutputs += output.amount;
                    outputs.add(output);
                }
                ArrayList<TransactionInput> inputs = new ArrayList<>();
                JSONArray dbt = tx.getJSONArray(Constants.DEBITS);
                for(int j = 0; j < dbt.length(); j++){
                    TransactionInput input = new TransactionInput();
                    input.index = dbt.getJSONObject(j).getInt(Constants.INDEX);
                    input.previous_account = dbt.getJSONObject(j).getLong(Constants.PREVIOUS_ACCOUNT);
                    input.previous_amount = dbt.getJSONObject(j).getLong(Constants.PREVIOUS_AMOUNT);
                    input.accountName = dbt.getJSONObject(j).getString(Constants.ACCOUNT_NAME);
                    transaction.totalInput += input.previous_amount;
                    inputs.add(input);
                }
                transaction.fee = tx.getLong(Constants.FEE);
                transaction.hash = tx.getString(Constants.HASH);
                transaction.timestamp = tx.getLong(Constants.TIMESTAMP);
                transaction.tx = null;
                transaction.type = tx.getString(Constants.TYPE);
                transaction.height = tx.getInt(Constants.HEIGHT);
                transaction.direction = tx.getInt(Constants.DIRECTION);
                transaction.outputs = outputs;
                transaction.inputs = inputs;
                transaction.amount = tx.getLong(Constants.AMOUNT);
                response.transactions.add(transaction);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    public static class TransactionItem implements Serializable{
        public byte[] tx;
        public String hash, type;
        public int height, direction;
        public long fee, amount, totalInput = 0, totalOutputs = 0;
        public long timestamp;
        public boolean animate = false;
        public ArrayList<TransactionOutput> outputs;
        public ArrayList<TransactionInput> inputs;

        public int getHeight() {
            return height;
        }

        public int getDirection() {
            return direction;
        }

        public long getAmount() {
            return amount;
        }

        public long getFee() {
            return fee;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class TransactionInput implements Serializable{
        public long previous_account;
        public int index;
        public long previous_amount;
        public String accountName;
    }

    public static class TransactionOutput implements Serializable{
        public int index, account;
        public long amount;
        public boolean internal;
        public String address;
    }
}
