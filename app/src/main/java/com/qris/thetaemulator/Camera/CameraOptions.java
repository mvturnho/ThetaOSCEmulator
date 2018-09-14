package com.qris.thetaemulator.Camera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mvturnho on 26/03/2018.
 */

class CameraOptions {
    private static final String TAG = "CameraOptions";
    private JSONObject jsonOptions;

    public CameraOptions(JSONObject jsonObject) {
        jsonOptions = jsonObject;
    }

    public Object getOption(String key) {
        Object value = null;
        try {
            value = (Object) jsonOptions.get(key);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        Log.d(TAG, value.toString());
        return value;
    }

    public void setOption(String key, Object value) {
        try {
            Log.d(TAG, key + "=" + value);
            jsonOptions.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
