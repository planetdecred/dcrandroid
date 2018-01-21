package com.decrediton.data;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class Settings {
    private String settingName;
    private String rightValue;
    public Settings(){
    }
    public Settings(String setting){
        this.settingName = setting;
    }
    public Settings(String setting,String rightValue){
        this.settingName = setting;
        this.rightValue = rightValue;
    }

    public String getSettingName(){
        return settingName;
    }

    public void setSettingName(String settingName){
        this.settingName = settingName;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }
}
