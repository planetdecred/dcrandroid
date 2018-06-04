package com.dcrandroid.util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;

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
            JSONArray transactions = object.getJSONArray("Transactions");
            for(int i = 0; i < transactions.length(); i++){
                JSONObject tx = transactions.getJSONObject(i);
                ArrayList<TransactionCredit> credit = new ArrayList<>();
                JSONArray cdt = tx.getJSONArray("Credits");
                for(int j = 0; j < cdt.length(); j++){
                    TransactionCredit item = new TransactionCredit();
                    item.account = cdt.getJSONObject(j).getInt("Account");
                    item.internal = cdt.getJSONObject(j).getBoolean("Internal");
                    item.address = cdt.getJSONObject(j).getString("Address");
                    item.index = cdt.getJSONObject(j).getInt("Index");
                    item.amount = cdt.getJSONObject(j).getLong("Amount");
                    credit.add(item);
                }
                ArrayList<TransactionDebit> debit = new ArrayList<>();
                JSONArray dbt = tx.getJSONArray("Debits");
                for(int j = 0; j < dbt.length(); j++){
                    TransactionDebit item = new TransactionDebit();
                    item.index = dbt.getJSONObject(j).getInt("Index");
                    item.previous_account = dbt.getJSONObject(j).getLong("PreviousAccount");
                    item.previous_amount = dbt.getJSONObject(j).getLong("PreviousAmount");
                    item.accountName = dbt.getJSONObject(j).getString("AccountName");
                    debit.add(item);
                }
                TransactionItem transaction = new TransactionItem();
                transaction.fee = tx.getLong("Fee");
                transaction.hash = tx.getString("Hash");
                transaction.timestamp = tx.getLong("Timestamp");
                transaction.tx = null;
                transaction.type = tx.getString("Type");
                transaction.height = tx.getInt("Height");
                transaction.direction = tx.getInt("Direction");
                transaction.credits = credit;
                transaction.debits = debit;
                transaction.amount = tx.getLong("Amount");
                response.transactions.add(transaction);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    public static class TransactionItem{
        public byte[] tx;
        public String hash, type;
        public int height, direction;
        public long fee,amount;
        public long timestamp;
        public ArrayList<TransactionCredit> credits;
        public ArrayList<TransactionDebit> debits;
    }

    public static class TransactionDebit{
        public long previous_account;
        public int index;
        public long previous_amount;
        public String accountName;
    }

    public static class TransactionCredit{
        public long index, account;
        public long amount;
        public boolean internal;
        public String address;
    }
}
