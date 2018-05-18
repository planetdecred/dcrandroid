package com.dcrandroid.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class Transaction implements Serializable{
    private float amount, TransactionFee;
    public float totalInput, totalOutput;
    private String type, hash, txStatus, txDate;
    private int height;
    public boolean animate = false;
    private ArrayList<String> usedInput,walletOutput;

    public void setTotalInput(float totalInput) {
        this.totalInput = totalInput;
    }

    public float getTotalInput() {
        return totalInput;
    }

    public void setTotalOutput(float totalOutput) {
        this.totalOutput = totalOutput;
    }

    public float getTotalOutput() {
        return totalOutput;
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

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getTransactionFee() {
        return TransactionFee;
    }

    public void setTransactionFee(float transactionFee) {
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

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
