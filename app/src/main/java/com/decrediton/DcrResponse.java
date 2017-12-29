package com.decrediton;

import org.json.JSONException;
import org.json.JSONObject;


public class DcrResponse {
    private DcrResponse(){}
    public boolean errorOccurred;
    public int errorCode;
    public String content;
    public static DcrResponse parse(String jsonResponse) throws JSONException {
        DcrResponse response = new DcrResponse();
        JSONObject object = new JSONObject(jsonResponse);
        response.errorOccurred = object.getBoolean("ErrorOccurred");
        if(response.errorOccurred){
            JSONObject error = object.getJSONObject("Error");
            response.content = error.getString("Message");
            response.errorCode = error.getInt("Code");
        }else{
            JSONObject success = object.getJSONObject("Success");
            response.content = success.getString("content");
        }
        return response;
    }
}
