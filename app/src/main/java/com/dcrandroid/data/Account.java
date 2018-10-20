package com.dcrandroid.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class Account implements Serializable{
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
            account.setAccountNumber(accJSONObject.getInt(Constants.NUMBER));
            account.setAccountName(accJSONObject.getString(Constants.NAME));
            account.setExternalKeyCount(accJSONObject.getInt(Constants.EXTERNAL_KEY_COUNT));
            account.setInternalKeyCount(accJSONObject.getInt(Constants.INTERNAL_KEY_COUNT));
            account.setImportedKeyCount(accJSONObject.getInt(Constants.IMPORTED_KEY_COUNT));
            JSONObject balanceObj = accJSONObject.getJSONObject(Constants.BALANCE );
            Balance balance = new Balance();
            balance.setTotal(balanceObj.getLong(Constants.TOTAL));
            balance.setSpendable(balanceObj.getLong(Constants.SPENDABLE));
            balance.setImmatureReward(balanceObj.getLong(Constants.IMMATURE_REWARDS));
            balance.setImmatureStakeGeneration(balanceObj.getLong(Constants.IMMATURE_STAKE_GEN));
            balance.setLockedByTickets(balanceObj.getLong(Constants.LOCKED_BY_TICKETS));
            balance.setVotingAuthority(balanceObj.getLong(Constants.VOTING_AUTHORITY));
            balance.setUnConfirmed(balanceObj.getLong(Constants.UNCONFIRMED));
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

    public String getHDPath(boolean isTestNet) {
        return (isTestNet ? Constants.TESTNET_HD_PATH : Constants.MAINNET_HD_PATH) + accountNumber + "'";
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
