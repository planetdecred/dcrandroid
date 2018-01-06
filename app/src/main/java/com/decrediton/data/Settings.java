package com.decrediton.data;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class Settings {
    private String setting;
    public Settings(){
    }
    public Settings(String setting){
        this.setting = setting;
    }

    public String getSetting(){
        return  setting;
    }

    public void setSetting(String seed){
        this.setting = setting;
    }
}
