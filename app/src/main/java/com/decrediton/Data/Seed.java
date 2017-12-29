package com.decrediton.Data;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class Seed {
    private String seed;
    public Seed(){
    }
    public Seed(String seed){
        this.seed = seed;
    }

    public String getSeed(){
        return  seed;
    }

    public void setSeed(String seed){
        this.seed = seed;
    }
}
