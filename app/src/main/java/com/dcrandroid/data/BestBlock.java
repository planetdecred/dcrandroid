package com.dcrandroid.data;

/**
 * Created by collins on 2/2/18.
 */

public class BestBlock {
    private String hash = "";
    private int height = 1;

    public BestBlock(){}

    public BestBlock(String hash, int height){
        this.hash = hash;
        this.height = height;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }
}
