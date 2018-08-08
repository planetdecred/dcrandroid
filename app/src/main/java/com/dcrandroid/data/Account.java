package com.dcrandroid.data;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class Account {
    private String accountName;
    private Balance balance;
    private int accountNumber, externalKeyCount, internalKeyCount, importedKeyCount;

    public static ArrayList<Account> parse(String json) throws JSONException{
        JSONObject obj = new JSONObject(json);
        JSONArray acc = obj.getJSONArray("Acc");
        ArrayList<Account> items = new ArrayList<>();
        for (int i = 0; i < acc.length(); i++) {
            final JSONObject accJSONObject = acc.getJSONObject(i);
            Account account = new Account();
            account.setAccountNumber(accJSONObject.getInt("Number"));
            account.setAccountName(accJSONObject.getString("Name"));
            account.setExternalKeyCount(accJSONObject.getInt("ExternalKeyCount"));
            account.setInternalKeyCount(accJSONObject.getInt("InternalKeyCount"));
            account.setImportedKeyCount(accJSONObject.getInt("ImportedKeyCount"));
            JSONObject balanceObj = accJSONObject.getJSONObject("Balance");
            Balance balance = new Balance();
            balance.setTotal(balanceObj.getLong("Total"));
            balance.setSpendable(balanceObj.getLong("Spendable"));
            balance.setImmatureReward(balanceObj.getLong("ImmatureReward"));
            balance.setImmatureStakeGeneration(balanceObj.getLong("ImmatureStakeGeneration"));
            balance.setLockedByTickets(balanceObj.getLong("LockedByTickets"));
            balance.setVotingAuthority(balanceObj.getLong("VotingAuthority"));
            balance.setUnConfirmed(balanceObj.getLong("UnConfirmed"));
            account.setBalance(balance);
            items.add(account);
        }
        return items;
    }

    public Balance getBalance() {
        return balance;
    }

    public void setBalance(Balance balance) {
        this.balance = balance;
    }

    public String getAccountName(){
        return  accountName;
    }

    public void setAccountName(String accountName){
        this.accountName = accountName;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHDPath() {
        return "m / 44' / 11' / "+accountNumber;
    }

    public void setExternalKeyCount(int externalKeyCount) {
        this.externalKeyCount = externalKeyCount;
    }

    public int getExternalKeyCount() {
        return externalKeyCount;
    }

    public void setImportedKeyCount(int importedKeyCount) {
        this.importedKeyCount = importedKeyCount;
    }

    public int getImportedKeyCount() {
        return importedKeyCount;
    }

    public void setInternalKeyCount(int internalKeyCount) {
        this.internalKeyCount = internalKeyCount;
    }

    public int getInternalKeyCount() {
        return internalKeyCount;
    }
}
