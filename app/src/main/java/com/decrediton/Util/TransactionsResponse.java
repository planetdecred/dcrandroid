package com.decrediton.Util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
/**
 * Created by collins on 1/10/18.
 */

public class TransactionsResponse {
    private TransactionsResponse(){}
    public ArrayList<TransactionItem> transactions = new ArrayList<>();
    public static TransactionsResponse parse(String json){
        TransactionsResponse response = new TransactionsResponse();
        try {
            JSONObject object = new JSONObject(json);
            JSONArray mined = object.getJSONArray("Mined");
            for(int i = 0; i < mined.length(); i++){
                JSONObject tx = mined.getJSONObject(i);
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
                    debit.add(item);
                }
                TransactionItem transaction = new TransactionItem();
                transaction.fee = (float) tx.getDouble("Fee");
                transaction.hash = tx.getString("Hash");
                transaction.timestamp = tx.getLong("Timestamp");
                transaction.tx = null;
                transaction.type = tx.getString("Type");
                transaction.credits = credit;
                transaction.debits = debit;
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
        public float fee;
        public long timestamp;
        public ArrayList<TransactionCredit> credits;
        public ArrayList<TransactionDebit> debits;
    }

    public static class TransactionDebit{
        public long previous_account, previous_amount;
        public int index;
    }

    public static class TransactionCredit{
        public long index, account,amount;
        public boolean internal;
        public String address;
    }
}
