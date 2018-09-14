package com.qris.thetaemulator.Camera;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.qris.thetaemulator.Camera.CameraOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by mvturnho on 25/03/2018.
 */

public class OSCCamera {
    private static final String TAG = "OSCCamera";
    private final AssetManager assets;
    private String sessionId = "SID_0000";
    private Double batteryLevel = 0.33;             //0.0, 0.33, 0.67, or 1.0
    private String _batteryState = "disconnect";    //"charging", "charged", "disconnect"
    private String _captureStatus = "idle";         //"shooting", "idle", "self-timer countdown"
    private Integer _recordedTime = 0;              //Shooting time of movie (sec)
    private Integer _recordableTime = 0;            //Remaining time of movie (sec)
    private Integer _compositeShootingElapsedTime = 0;
    private String storageUri = "";                 //storageUri
    private String _latestFileUri = "";             //URL of the last saved file

    private int pictureId = 1;
    private String state = "done";
    private String _cameraError = "";               //Error information of the camera

    public HashMap<String, String> errorMap = null;

    private CameraOptions cameraOptions;


    public OSCCamera(AssetManager assets) {
        this.assets = assets;
        try {
            cameraOptions = new CameraOptions(new JSONObject(loadJSONFromAsset("options.json")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        errorMap = new HashMap<String, String>();
        errorMap.put("NO_MEMORY", "Insufficient memory");
        errorMap.put("WRITING_DATA", "Writing data");
        errorMap.put("FILE_NUMBER_OVER", "Maximum file number exceeded");
        errorMap.put("NO_DATE_SETTING", "Camera clock not set");
        errorMap.put("COMPASS_CALIBRATION", "Electronic compass error");
        errorMap.put("CARD_DETECT_FAIL", "SD memory card not inserted");
        errorMap.put("CAPTURE_HW_FAILED", "Shooting hardware failure");
        errorMap.put("CANT_USE_THIS_CARD", "Medium failure");
        errorMap.put("FORMAT_INTERNAL_MEM", "Internal memory format error");
        errorMap.put("FORMAT_CARD", "SD memory card format error");
        errorMap.put("INTERNAL_MEM_ACCESS_FAIL", "Internal memory access error");
        errorMap.put("CARD_ACCESS_FAIL", "SD memory card access error");
        errorMap.put("UNEXPECTED_ERROR", "Undefined error");
        errorMap.put("BATTERY_CHARGE_FAIL", "Charging error");
        errorMap.put("HIGH_TEMPERATURE", "Abnormal temperature");
    }

    public String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = assets.open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static JSONObject updateJSONObject(JSONObject obj, String keyMain, Object newValue) throws Exception {
        // We need to know keys of Jsonobject
        JSONObject json = new JSONObject();
        Iterator iterator = obj.keys();
        String key = null;
        while (iterator.hasNext()) {
            key = (String) iterator.next();
            // if object is just string we change value in key
            if ((obj.optJSONArray(key) == null) && (obj.optJSONObject(key) == null)) {
                if (key.equals(keyMain)) {
                    // put new value
                    obj.put(key, newValue);
                    return obj;
                }
            }

            // if it's jsonobject
            if (obj.optJSONObject(key) != null) {
                updateJSONObject(obj.getJSONObject(key), keyMain, newValue);
            }

            // if it's jsonarray
            if (obj.optJSONArray(key) != null) {
                JSONArray jArray = obj.getJSONArray(key);
                for (int i = 0; i < jArray.length(); i++) {
                    updateJSONObject(jArray.getJSONObject(i), keyMain, newValue);
                }
            }
        }
        return obj;
    }

    public String getState() {
        String json = loadJSONFromAsset("getState.json");
        try {
            JSONObject response = new JSONObject(json);
            updateJSONObject(response, "sessionId", sessionId);
            updateJSONObject(response, "batteryLevel", batteryLevel);
            updateJSONObject(response, "_batteryState", _batteryState);
            if (_cameraError.length() > 0)
                ((JSONObject) response.get("state")).put("_cameraError", _cameraError);
            json = response.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public String getInfo() {
        Log.d("GET_INFO", "");
        return this.loadJSONFromAsset("getInfo.json");
    }

    public String startSession() {
        sessionId = "SID_002";
        String json = loadJSONFromAsset("startSession.json");
        try {
            JSONObject response = new JSONObject(json);
            updateJSONObject(response, "sessionId", sessionId);
            json = response.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public String takePicture(JSONObject jsonObj) {
        JSONObject response = null;
        _latestFileUri = "100RICOH/R0011232.JPG";
        try {
            response = new JSONObject(loadJSONFromAsset("takePicture.json"));
            updateJSONObject(response, "sessionId", sessionId);
            updateJSONObject(response, "id", pictureId++);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public String commandError(String code, String message) {
        JSONObject response = null;
        try {
            response = new JSONObject(loadJSONFromAsset("error.json"));
            updateJSONObject(response, "state", "error");
            updateJSONObject(response, "code", code);
            updateJSONObject(response, "message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public String getStatus(JSONObject jsonObj) {
        JSONObject response = null;
        try {
            response = new JSONObject(loadJSONFromAsset("getStatus.json"));
            updateJSONObject(response, "state", state);
            updateJSONObject(response, "id", pictureId);
            updateJSONObject(response, "fileUri", _latestFileUri);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public String getOptions(JSONObject jsonObj) {
        //Log.d("GET_OPTIONS", jsonObj.toString());
        JSONObject response = null;
        try {
            response = new JSONObject(loadJSONFromAsset("getOptions.json"));
            //JSONObject defaultoptions = new JSONObject(loadJSONFromAsset("options.json"));
            JSONArray reqoptions = jsonObj.getJSONObject("parameters").getJSONArray("optionNames");
            JSONObject responseOptions = new JSONObject();
            for (int i = 0; i < reqoptions.length(); i++) {
                //System.out.println(reqoptions.get(i));
                responseOptions.put(reqoptions.get(i).toString(), cameraOptions.getOption(reqoptions.get(i).toString()));
            }
            updateJSONObject(response, "sessionId", sessionId);
            //updateJSONObject(response, "options", responseOptions);
            ((JSONObject) response.get("results")).put("options", responseOptions);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public String setOptions(JSONObject jsonObj) {
        //Log.d("GET_OPTIONS", jsonObj.toString());
        JSONObject response = null;
        try {
            response = new JSONObject();
            JSONObject reqoptions = jsonObj.getJSONObject("parameters").getJSONObject("options");
            Iterator<String> iter = reqoptions.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                cameraOptions.setOption(key, reqoptions.get(key));
            }
            response.put("name", jsonObj.get("name"));
            response.put("state", "done");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public String listImages(JSONObject jsonObj) {
        JSONObject response = null;
        try {
            response = new JSONObject(loadJSONFromAsset("listImages.json"));
            JSONObject image_entry = null;
            JSONObject parameters = (JSONObject) jsonObj.get("parameters");
            int numentries = parameters.getInt("entryCount");

            if (parameters.getBoolean("includeThumb")) {
                int maxsize = parameters.getInt("maxSize");
                image_entry = new JSONObject(loadJSONFromAsset("listEntryThumb.json"));
                Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/pic.jpg"), 100, 100);
                String encoded = getBase64String(thumbnail);
                image_entry.put("thumbnail", encoded);
                image_entry.put("_thumbSize", encoded.length());
            } else
                image_entry = new JSONObject(loadJSONFromAsset("listEntry.json"));

            JSONArray entries = response.getJSONObject("results").getJSONArray("entries");
            for (int i = 0; i < numentries; i++)
                entries.put(image_entry);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public Bundle getStateMessage() {
        Bundle bundle = new Bundle();
        bundle.putInt("pictureId", pictureId);
        bundle.putString("_cameraError", _cameraError);
        bundle.putString("_latestFileUri", _latestFileUri);
        bundle.putString("_batteryState", _batteryState);
        bundle.putDouble("batteryLevel", batteryLevel);
        return bundle;
    }

    public void setBatteryState(String batteryState) {
        this._batteryState = batteryState;
    }

    public void setBatteryLevel(String batteryLevel) {
        Double batlev = Double.valueOf(batteryLevel);
        batlev = batlev / 100.0;
        this.batteryLevel = batlev;
    }

    private String getBase64String(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        return base64String;
    }


    public String getSessionId() {
        return sessionId;
    }

    public Double getBatteryLevel() {
        return batteryLevel;
    }

    public String get_batteryState() {
        return _batteryState;
    }

    public String get_captureStatus() {
        return _captureStatus;
    }

    public Integer get_recordedTime() {
        return _recordedTime;
    }

    public Integer get_recordableTime() {
        return _recordableTime;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String get_latestFileUri() {
        return _latestFileUri;
    }

    public int getPictureId() {
        return pictureId;
    }

    public String get_cameraError() {
        return _cameraError;
    }


    public void setBusy(boolean busy) {
        if (busy)
            this.state = "inProgress";
        else
            this.state = "done";
    }

    public boolean isBusy() {
        return !state.equals("done");
    }
}
