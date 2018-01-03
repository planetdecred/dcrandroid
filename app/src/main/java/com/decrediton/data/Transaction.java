package com.decrediton.data;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class Transaction {
    private String amount;
    private String address;
    private String txDate;
    private String txStatus;
    private String accountName;
    private String txType;

    public Transaction(){
    }
    public Transaction(String amount, String address, String txDate, String txStatus, String accountName, String txType){
        this.accountName = accountName;
        this.address = address;
        this.txDate = txDate;
        this. txStatus =txStatus;
        this.txType =txType;
        this. amount = amount;

    }

    public String getTxDate() {
        return txDate;
    }

    public void setTxDate(String txDate) {
        this.txDate = txDate;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTxStatus() {
        return txStatus;
    }

    public void setTxStatus(String txStatus) {
        this.txStatus = txStatus;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }
}
