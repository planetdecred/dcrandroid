package com.decrediton.data;

import java.util.ArrayList;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class Transaction {
    private String amount;
    private String TransactionFee;
    private String txDate;
    private String txStatus;
    private String accountName;
    private String txType;
    private ArrayList<String> usedIput;
    private ArrayList<String>  walletOutput;

    public Transaction(){
    }
    public Transaction(String amount, String TransactionFee, String txDate, String txStatus, String accountName, String txType, ArrayList<String> usedIput, ArrayList<String> walletOutput){
        this.accountName = accountName;
        this.TransactionFee = TransactionFee;
        this.txDate = txDate;
        this. txStatus =txStatus;
        this.txType =txType;
        this. amount = amount;
        this.usedIput = usedIput;
        this.walletOutput = walletOutput;
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

    public String getTransactionFee() {
        return TransactionFee;
    }

    public void setTransactionFee(String transactionFee) {
        this.TransactionFee = transactionFee;
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

    public ArrayList<String> getUsedIput() {
        return usedIput;
    }

    public void setUsedIput(ArrayList<String> usedIput) {
        this.usedIput = usedIput;
    }

    public ArrayList<String> getWalletOutput() {
        return walletOutput;
    }

    public void setWalletOutput(ArrayList<String> walletOutput) {
        this.walletOutput = walletOutput;
    }
}
