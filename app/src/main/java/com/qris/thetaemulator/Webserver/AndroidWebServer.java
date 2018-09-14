package com.qris.thetaemulator.Webserver;

/**
 * Created by mvturnho on 25/03/2018.
 */

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.qris.thetaemulator.Camera.OSCCamera;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Mikhael LOPEZ on 14/12/2015.
 */
public class AndroidWebServer extends NanoHTTPD {

    private static final String TAG = "AndroidWebServer";

    private static final String MIME_JSON = "application/json";
    private static final String MIME_HTML = "text/html";
    private static final String MIME_JPG = "image/jpeg";
    private static final String MIME_TXT = "text/plain";
    private Handler mHandler;
    private Context context;

    private OSCCamera camera;

    public AndroidWebServer(Context applicationContext, Handler handler, int port) {
        super(port);
        camera = new OSCCamera(applicationContext.getAssets());
        context = applicationContext;
        mHandler = handler;
    }

    public AndroidWebServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, List<String>> parms = session.getParameters();

        msg += "<br/><p>url, " + session.getUri() + "</p>";
        msg += "<br/><p>header: " + session.getHeaders() + "</p>";
        msg += "<br/><p>method: " + session.getMethod() + "</p>";

        if (Method.POST.equals(session.getMethod()))
            return handlePost(session);
        else if (Method.GET.equals(session.getMethod()))
            return handleGET(session);
        else {
            msg += "ERROR</body></html>\n";
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, msg);
        }
    }

    private Response handleGET(IHTTPSession session) {
        if (session.getUri().equals("/osc/info"))
            return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.getInfo());
        if (session.getUri().startsWith("/100RICOH/")) {
            File picture = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            try {
                InputStream targetStream = new FileInputStream(picture);
                return newFixedLengthResponse(Response.Status.OK, MIME_JPG, targetStream,picture.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_JSON, camera.commandError("unexpected", "Other errors"));
        } else
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_JSON, camera.commandError("unexpected", "Other errors"));
    }

    private Response handlePost(IHTTPSession session) {
        //String msg = "";
        Map<String, String> files = new HashMap<String, String>();

        Integer contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
        byte[] buffer = new byte[contentLength];
        try {
            JSONObject jsonObj = null;
            if (contentLength > 0) {
                session.getInputStream().read(buffer, 0, contentLength);
                jsonObj = new JSONObject(new String(buffer));
            }
            if (session.getUri().equals("/osc/state")) {
                return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.getState());
            } else if (session.getUri().equals("/osc/commands/execute")) {
                String name = jsonObj.getString("name");
                if (name.equals("camera.startSession"))
                    return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.startSession());
                else if (name.equals("camera.getOptions"))
                    return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.getOptions(jsonObj));
                else if (name.equals("camera.setOptions"))
                    return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.setOptions(jsonObj));
                else if (name.equals("camera.takePicture")) {
                    String picturemsg = camera.takePicture(jsonObj);
                    Message msg = mHandler.obtainMessage();
                    msg.setData(camera.getStateMessage());
                    msg.sendToTarget();
                    return newFixedLengthResponse(Response.Status.OK, MIME_JSON, picturemsg);
                } else if (name.equals("camera.listImages"))
                    return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.listImages(jsonObj));
                else
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_JSON, camera.commandError("unknownCommand", "Command executed is unknown."));
            } else if (session.getUri().equals("/osc/commands/status")) {
                return newFixedLengthResponse(Response.Status.OK, MIME_JSON, camera.getStatus(jsonObj));
            } else
                return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_JSON, camera.commandError("unexpected", "Other errors"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_JSON, camera.commandError("unknownCommand", "Command executed is unknown."));
    }

    public OSCCamera getOscCamera() {
        return camera;
    }
}