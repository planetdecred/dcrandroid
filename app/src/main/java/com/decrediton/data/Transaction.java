package com.decrediton.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class Transaction implements Serializable{
    private String amount;
    private String TransactionFee;
    private String txDate;
    private String txStatus;
    private String type;
    private String confirmations;
    private String hash;
    private ArrayList<String> usedInput;
    private ArrayList<String> walletOutput;

    public Transaction(){
    }
    public Transaction(String amount, String TransactionFee, String txDate, String txStatus, String confirmations, String txType, ArrayList<String> usedInput, ArrayList<String> walletOutput){
        this.TransactionFee = TransactionFee;
        this.txDate = txDate;
        this.txStatus =txStatus;
        this.type =txType;
        this.amount = amount;
        this.usedInput = usedInput;
        this.walletOutput = walletOutput;
        this.confirmations = confirmations;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getUsedInput() {
        return usedInput;
    }

    public void setUsedInput(ArrayList<String> input) {
        this.usedInput = input;
    }

    public ArrayList<String> getWalletOutput() {
        return walletOutput;
    }

    public void setWalletOutput(ArrayList<String> walletOutput) {
        this.walletOutput = walletOutput;
    }

    public String getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(String confirmations) {
        this.confirmations = confirmations;
    }
}
