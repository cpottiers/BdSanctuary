package com.android.bdsanctuary.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.bdsanctuary.R;
import com.android.bdsanctuary.datas.Global;
import com.android.bdsanctuary.datas.Serie;
import com.android.bdsanctuary.datas.Tome;
import com.android.bdsanctuary.parser.XMLParser;
import com.cyrilpottiers.androlib.Log;
import com.cyrilpottiers.androlib.arrays.ArraysUtils;
import com.cyrilpottiers.androlib.cache.CacheFileUtils;

public class ServerConnector {
    public static final String ERROR       = "error";
    public static final String ERROR_TRACE = "stack";
    public static final String EDITION_ID  = "eid";
    public static final String SERIE_ID    = "sid";
    public static final int    MAX_RETRY   = 5;

    /* Enum */
    public enum WSEnum {
        UNKNOWN,
        GET_ID,
        SYNC_SERIES,
        SYNC_EDITION,
        GET_TOMEICON,
        GET_MISSINGTOMEICON,
        SYNC_MISSING;

        public static final int UNKNOWN_VALUE             = -1;
        public static final int GET_ID_VALUE              = 0;
        public static final int SYNC_SERIES_VALUE         = 1;
        public static final int SYNC_EDITION_VALUE        = 2;
        public static final int SYNC_MISSING_VALUE        = 4;
        public static final int GET_TOMEICON_VALUE        = 3;
        public static final int GET_MISSINGTOMEICON_VALUE = 5;

        private WSEnum() {
        }

        public int getValue() {
            switch (this) {
                case GET_ID:
                    return GET_ID_VALUE;
                case SYNC_SERIES:
                    return SYNC_SERIES_VALUE;
                case SYNC_EDITION:
                    return SYNC_EDITION_VALUE;
                case GET_TOMEICON:
                    return GET_TOMEICON_VALUE;
                case SYNC_MISSING:
                    return SYNC_MISSING_VALUE;
                case GET_MISSINGTOMEICON:
                    return GET_MISSINGTOMEICON_VALUE;
            }
            return UNKNOWN_VALUE;
        }

        public static WSEnum getValue(int value) {
            switch (value) {
                case GET_ID_VALUE:
                    return GET_ID;
                case SYNC_SERIES_VALUE:
                    return SYNC_SERIES;
                case SYNC_EDITION_VALUE:
                    return SYNC_EDITION;
                case GET_TOMEICON_VALUE:
                    return GET_TOMEICON;
                case SYNC_MISSING_VALUE:
                    return SYNC_MISSING;
                case GET_MISSINGTOMEICON_VALUE:
                    return GET_MISSINGTOMEICON;
            }
            return UNKNOWN;
        }
    };

    public enum ErrorCode {
        NONE,
        UNKNOWN,
        NETWORK_ERROR,
        SYNCSERIES_ERROR,
        SYNCEDITION_ERROR,
        SYNCMISSING_ERROR,
        GETID_ERROR,
        GETICON_ERROR,
        GETMISSINGTOMEICON_ERROR, ;

        public static final int NONE_VALUE                     = 0;
        public static final int UNKNOWN_VALUE                  = 1;
        public static final int NETWORK_ERROR_VALUE            = 2;
        public static final int GETID_ERROR_VALUE              = 3;
        public static final int SYNCSERIES_ERROR_VALUE         = 4;
        public static final int SYNCEDITION_ERROR_VALUE        = 5;
        public static final int GETICON_ERROR_VALUE            = 6;
        public static final int SYNCMISSING_ERROR_VALUE        = 7;
        public static final int GETMISSINGTOMEICON_ERROR_VALUE = 8;

        private ErrorCode() {
        }

        public int getValue() {
            switch (this) {
                case UNKNOWN:
                    return UNKNOWN_VALUE;
                case NETWORK_ERROR:
                    return NETWORK_ERROR_VALUE;
                case SYNCSERIES_ERROR:
                    return SYNCSERIES_ERROR_VALUE;
                case SYNCEDITION_ERROR:
                    return SYNCEDITION_ERROR_VALUE;
                case GETID_ERROR:
                    return GETID_ERROR_VALUE;
                case GETICON_ERROR:
                    return GETICON_ERROR_VALUE;
                case SYNCMISSING_ERROR:
                    return SYNCMISSING_ERROR_VALUE;
                case GETMISSINGTOMEICON_ERROR:
                    return GETMISSINGTOMEICON_ERROR_VALUE;
            }
            return NONE_VALUE;
        }

        public static ErrorCode getValue(int value) {
            switch (value) {
                case UNKNOWN_VALUE:
                    return UNKNOWN;
                case NETWORK_ERROR_VALUE:
                    return NETWORK_ERROR;
                case SYNCSERIES_ERROR_VALUE:
                    return SYNCSERIES_ERROR;
                case SYNCEDITION_ERROR_VALUE:
                    return SYNCEDITION_ERROR;
                case GETID_ERROR_VALUE:
                    return GETID_ERROR;
                case GETICON_ERROR_VALUE:
                    return GETICON_ERROR;
                case SYNCMISSING_ERROR_VALUE:
                    return SYNCMISSING_ERROR;
                case GETMISSINGTOMEICON_ERROR_VALUE:
                    return GETMISSINGTOMEICON_ERROR;
            }
            return NONE;
        }
    };

    private class Command {
        public WSEnum    type;
        public Handler   handler;
        public Object[]  params;
        public ErrorCode error;
        public Throwable errorTrace;
        public int       id;

        private Command(WSEnum type, Handler callback, Object... params) {
            this.type = type;
            this.handler = callback;
            this.params = params;
            this.error = ErrorCode.NONE;
        }
    }

    private class HTTPStackCheckLoop extends Thread {
        private Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                        Log.printStackTrace(Global.getLogTag(HTTPStackCheckLoop.class), e);
                    }
                    getCommand();
                    // No more command
                    if (out.size() == 0)
                        for (HttpListener listener : listeners)
                            listener.onHttpOver();
                }
            });
        }
    }

    private class HTTPTask extends AsyncTask<Void, Void, Void> {
        private Command currentCmd;

        public void setCurrentCommand(Command cmd) {
            this.currentCmd = cmd;
        }

        @Override
        protected void onPreExecute() {
            // set current command
            out.add(currentCmd);
            Log.d(Global.getLogTag(ServerConnector.class), "[" + currentCmd.id
                + "] PROCESS OutStack Size=" + out.size());
        }

        @Override
        protected Void doInBackground(Void... args) {
            switch (currentCmd.type) {
                case GET_ID: {
                    getId(currentCmd, (String) currentCmd.params[0], (String) currentCmd.params[1]);
                    break;
                }
                case SYNC_SERIES: {
                    syncSeries(currentCmd, (String) currentCmd.params[0]);
                    break;
                }
                case SYNC_MISSING: {
                    //                    syncMissing(currentCmd, (String) currentCmd.params[0]);
                    syncMissingMobile(currentCmd, (String) currentCmd.params[0], (String) currentCmd.params[1]);
                    break;
                }
                case SYNC_EDITION: {
                    syncEdition(currentCmd, (String) currentCmd.params[0], (String) currentCmd.params[1], (Integer) currentCmd.params[2]);
                    break;
                }
                case GET_TOMEICON: {
                    getTomeIcon(currentCmd, (Tome) currentCmd.params[0]);
                    break;
                }
                case GET_MISSINGTOMEICON: {
                    getMissingTomeIcon(currentCmd, (Tome) currentCmd.params[0]);
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void _) {
            // call handler
            if (currentCmd.handler != null) {
                Message msg = currentCmd.handler.obtainMessage(currentCmd.type.getValue());
                Bundle bdl = new Bundle();
                bdl.putInt(ERROR, currentCmd.error.getValue());

                switch (currentCmd.type) {
                    case GET_ID: {
                        break;
                    }
                    case SYNC_SERIES: {
                        break;
                    }
                    case SYNC_MISSING: {
                        break;
                    }
                    case SYNC_EDITION: {
                        bdl.putInt(EDITION_ID, Integer.parseInt((String) currentCmd.params[1]));
                        bdl.putInt(SERIE_ID, (Integer) currentCmd.params[2]);
                        break;
                    }
                    case GET_TOMEICON: {
                        bdl.putInt(EDITION_ID, ((Tome) currentCmd.params[0]).getEditionId());
                        break;
                    }
                    case GET_MISSINGTOMEICON: {
                        bdl.putInt(EDITION_ID, ((Tome) currentCmd.params[0]).getEditionId());
                        break;
                    }
                }

                bdl.putSerializable(ERROR_TRACE, currentCmd.errorTrace);
                //                bdl.putString(RESULT, currentCmd.result);
                msg.setData(bdl);

                currentCmd.handler.sendMessage(msg);
            }

            // On initialise la stack HTTP
            out.remove(currentCmd);
            Log.d(Global.getLogTag(ServerConnector.class), "[" + currentCmd.id
                + "] END OutStack Size=" + out.size());
            currentCmd = null;
            new HTTPStackCheckLoop().start();
        }
    }

    private static ServerConnector instance  = new ServerConnector();
    private static int             httpCount = 0;

    private ServerConnector() {
    }

    public static ServerConnector getInstance() {
        return instance;
    }

    private ArrayList<HttpListener> listeners = new ArrayList<HttpListener>();

    public static void registerHttpOverListener(HttpListener listener) {
        instance.listeners.add(listener);
    }

    public static void unregisterHttpOverListener(HttpListener listener) {
        instance.listeners.remove(listener);
    }

    private final static int OUT_STACK_SIZE = 5;
    private Vector<Command>  in             = new Vector<Command>();
    private Vector<Command>  out            = new Vector<Command>(OUT_STACK_SIZE);

    private synchronized void pushCommand(WSEnum type, Handler callback,
            Object... params) {
        Command cmd = new Command(type, callback, params);
        cmd.id = ++httpCount;
        if (cmd.id == 1) CacheFileUtils.clearCacheFile("http_list.html");
        Log.v(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] PUSH COMMAND type=" + cmd.type);
        in.add(cmd);

        if (out.size() == 0 && in.size() == 1)
            for (HttpListener listener : listeners)
                listener.onHttpBegin();

        new HTTPStackCheckLoop().start();
    }

    private synchronized void getCommand() {
        // on vérifie qu'une commande ne soit pas deja encours de traitement
        if (out.size() >= OUT_STACK_SIZE) {
            Log.v(Global.getLogTag(ServerConnector.class), "GET COMMAND FULL WAITING FREE PLACE");
            return;
        }

        Command first = null;
        if (!in.isEmpty()) first = in.remove(0);
        if (first != null) {
            Log.v(Global.getLogTag(ServerConnector.class), "[" + first.id
                + "] GET COMMAND type=" + first.type);
            HTTPTask task = new HTTPTask();
            task.setCurrentCommand(first);
            task.execute();
        }
    }

    private static String getResponse(Command cmd, HttpRequestBase http,
            Charset charset) {
        StringBuffer stringBuffer = null;
        BufferedReader bufferedReader = null;
        DefaultHttpClient httpClient = null;

        Log.i(Global.getLogTag(ServerConnector.class), "Exec HTTP  uri:"
            + http.getURI());
        int retry = 0;
        while (retry < MAX_RETRY) {
            stringBuffer = new StringBuffer("");
            // Création d'un DefaultHttpClient et un HttpGet permettant d'effectuer
            // une requête HTTP de type GET
            httpClient = new DefaultHttpClient();

            // Ajout de headers

            try {
                // Execution du client HTTP avec le HttpGet
                HttpResponse httpResponse = httpClient.execute(http);

                // On récupère la réponse dans un InputStream
                InputStream inputStream = httpResponse.getEntity().getContent();
                // On crée un bufferedReader pour pouvoir stocker le résultat dans un string
                if (charset == null)
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                else
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset));
                // On lit ligne à ligne le bufferedReader pour le stocker dans le stringBuffer
                String line = null;
                while (null != (line = bufferedReader.readLine())) {
                    stringBuffer.append(line).append('\n');
                }
                if (stringBuffer.length() > 0)
                    stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                break;
            }
            catch (Exception e) {
                Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
                Log.i(Global.getLogTag(ServerConnector.class), "HTTP error["
                    + retry + "]:" + e.getMessage());
                retry++;
            }
        }
        if (retry == MAX_RETRY) {
            Log.e(Global.getLogTag(ServerConnector.class), "NETWORK_ERROR");
            cmd.error = ErrorCode.NETWORK_ERROR;
            return null;
        }

        String result = stringBuffer.toString();

        if (Log.isDebugging())
            CacheFileUtils.writeDebugFile("http_" + cmd.id + ".html", result);

        if (result == null || result.length() == 0) {
            cmd.error = ErrorCode.UNKNOWN;
            return null;
        }

        return result;
    }

    public static void getId(Handler handler) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH GET ID username="
            + Global.getUsername() + " password=" + Global.getPassword());
        getInstance().pushCommand(WSEnum.GET_ID, handler, Global.getUsername(), Global.getPassword());
    }

    private static void getId(Command cmd, String username, String password) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC GET ID username=" + username + " password=" + password);
        Resources res = Global.getResources();
        StringBuilder sb = new StringBuilder();
        sb.append(res.getString(R.string.MS_ROOT));
        sb.append(res.getString(R.string.MS_LOGIN));
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_AUTOLOGIN), res.getString(R.string.MS_LOGIN_AUTOLOGIN_VALUE)));
        nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_LOGIN), res.getString(R.string.MS_LOGIN_LOGIN_VALUE)));
        nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_PASSWORD), password));
        nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_USERNAME), username));
        nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_REDIRECT), res.getString(R.string.MS_LOGIN_REDIRECT_VALUE)));
        HttpPost httpPost = null;
        try {
            //            URI uri = new URI(sb.toString());
            String uri = sb.toString();
            httpPost = new HttpPost(uri);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Ajout de headers
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
            httpPost.setHeader("Accept-Charset", "ISO-8859-1,utf-8");
            httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
            httpPost.setHeader("Referer", Global.getResources().getString(R.string.MS_ROOT));

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC GET ID username=" + username + " password="
                + password + " uri=" + uri);
        }
        catch (Exception e) {
        }

        String response = getResponse(cmd, httpPost, null);
        if (cmd.error != ErrorCode.NONE) return;

        // on parse la réponse
        String pattern = res.getString(R.string.MS_LOGIN_COLLECTION_PATTERN, "\\s*(\\d+)");
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(response);
        String myId = null;
        if (m.find()) {
            myId = m.group(1);
        }
        Log.d(Global.getLogTag(ServerConnector.class), "[" + cmd.id + "] id="
            + myId + " pattern=" + pattern);

        if (myId == null || myId.equalsIgnoreCase("1")) {
            Log.d(Global.getLogTag(ServerConnector.class), "ERROR response="
                + response);
            cmd.error = ErrorCode.GETID_ERROR;
        }
        else {
            Global.setUserId(myId);
        }
    }

    public static void syncSeries(Handler handler) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH SYNC SERIES uid="
            + Global.getUserID());
        getInstance().pushCommand(WSEnum.SYNC_SERIES, handler, Global.getUserID());
    }

    private static void syncSeries(Command cmd, String uid) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC SYNC SERIES uid=" + uid);
        Resources res = Global.getResources();
        StringBuilder sb = new StringBuilder();
        sb.append(res.getString(R.string.MS_ROOT));
        sb.append(res.getString(R.string.MS_COLLECTION, uid));
        HttpGet httpGet = null;
        try {
            //            URI uri = new URI(sb.toString());
            String uri = sb.toString();
            httpGet = new HttpGet(uri);

            // Ajout de headers
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
            httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8");

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC SYNC SERIES uid=" + uid + " uri=" + uri);
        }
        catch (Exception e) {
        }

        String response = getResponse(cmd, httpGet, Charset.forName("UTF-8"));
        if (cmd.error != ErrorCode.NONE) return;

        try {
            int cIndex = response.indexOf("<table class=\"collection\"");
            // Empty collection
            if (cIndex < 0) return;
            response = response.substring(cIndex);
            response = response.substring(0, response.indexOf("</table>") + 8);
            ArrayList<Serie> series = XMLParser.parseSeriesXML(response);

            for (Serie serie : series) {
                if (!Global.getAdaptor().insertSerie(cmd.handler, serie))
                    throw new Exception(res.getString(R.string.Error_Alert_InsertionSerie, serie.toString()));
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
            cmd.error = ErrorCode.SYNCSERIES_ERROR;
            cmd.errorTrace = e;
            return;
        }
    }

    public static void syncMissing(Handler handler) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH SYNC MISSING uid="
            + Global.getUserID());

        getInstance().pushCommand(WSEnum.SYNC_MISSING, handler, Global.getUsername(), Global.getPassword());
    }

    private static void syncMissingMobile(Command cmd, String username,
            String password) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC SYNC MISSING MOBILE");
        Resources res = Global.getResources();
        String response;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(res.getString(R.string.MS_ROOT));
            sb.append(res.getString(R.string.MS_MISSING_CONNEXION));
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_AUTOLOGIN), res.getString(R.string.MS_LOGIN_AUTOLOGIN_VALUE)));
            nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_LOGIN), res.getString(R.string.MS_LOGIN_LOGIN_VALUE)));
            nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_PASSWORD), password));
            nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_USERNAME), username));
            nameValuePairs.add(new BasicNameValuePair(res.getString(R.string.MS_LOGIN_REDIRECT), res.getString(R.string.MS_LOGIN_REDIRECT_VALUE)));
            //            URI uri = new URI(sb.toString());
            String uri = sb.toString();
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC SYNC MISSING 1" + " uri=" + uri);

            // Ajout de headers
            httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
            httpPost.setHeader("Accept-Charset", "ISO-8859-1,utf-8");

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse == null || httpResponse.getStatusLine() == null
                || httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new HttpException();
            }

            // On récupère la réponse dans un InputStream
            InputStream inputStream = httpResponse.getEntity().getContent();
            // On crée un bufferedReader pour pouvoir stocker le résultat dans un string
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            // On lit ligne à ligne le bufferedReader pour le stocker dans le stringBuffer
            String line = null;
            sb = new StringBuilder();
            while (null != (line = bufferedReader.readLine())) {
                sb.append(line).append('\n');
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            response = sb.toString();

            if (Log.isDebugging())
                CacheFileUtils.writeDebugFile("http_" + cmd.id + "_1.html", response);

            //            CookieStore store = httpclient.getCookieStore();
            //
            //            httpclient = new DefaultHttpClient();
            //            httpclient.setCookieStore(store);

            sb = new StringBuilder();
            sb.append(res.getString(R.string.MS_ROOT));
            sb.append(res.getString(R.string.MS_MISSING_COLLECTION, Global.getUserID()));
            //            uri = new URI(sb.toString());
            uri = sb.toString();
            HttpGet httpGet = new HttpGet(uri);

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC SYNC MISSING 2" + " uri=" + uri);

            httpResponse = httpClient.execute(httpGet);
            if (httpResponse == null || httpResponse.getStatusLine() == null
                || httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new HttpException(uri.toString());
            }

            // On récupère la réponse dans un InputStream
            inputStream = httpResponse.getEntity().getContent();
            // On crée un bufferedReader pour pouvoir stocker le résultat dans un string
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            // On lit ligne à ligne le bufferedReader pour le stocker dans le stringBuffer
            line = null;
            sb = new StringBuilder();
            while (null != (line = bufferedReader.readLine())) {
                sb.append(line).append('\n');
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            response = sb.toString();

            if (Log.isDebugging())
                CacheFileUtils.writeDebugFile("http_" + cmd.id + "_2.html", response);
        }
        catch (Exception e) {
            Log.d(Global.getLogTag(ServerConnector.class), "[" + cmd.id
                + "] EXEC SYNC MISSING ERROR e=" + e);
            Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
            cmd.error = ErrorCode.NETWORK_ERROR;
            return;
        }

        response = response.substring(response.indexOf("<table class=\"collection\""));
        response = response.substring(0, response.indexOf("</table>") + 8);

        ArrayList<Tome> tomes = XMLParser.parseMissingTomesXML(response);
        if (tomes == null) {
            cmd.error = ErrorCode.SYNCMISSING_ERROR;
            return;
        }
        Global.getAdaptor().checkMissingTomes(cmd.handler, tomes);
    }

    /*
     * private static void syncMissing(Command cmd, String uid) {
     * Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
     * + "] EXEC SYNC MISSING uid=" + uid);
     * Resources res = Global.getResources();
     * StringBuilder sb = new StringBuilder();
     * sb.append(res.getString(R.string.MS_ROOT));
     * sb.append(res.getString(R.string.MS_MISSING, uid));
     * HttpGet httpGet = null;
     * try {
     * URI uri = new URI(sb.toString());
     * httpGet = new HttpGet(uri);
     * 
     * // Ajout de headers
     * httpGet.setHeader("Accept",
     * "text/html,application/xhtml+xml,application/xml");
     * httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8");
     * }
     * catch (Exception e) {
     * }
     * 
     * String response = getResponse(cmd, httpGet, Charset.forName("UTF-8"));
     * if (cmd.error != ErrorCode.NONE) return;
     * 
     * try {
     * int cIndex = response.indexOf("<table class=\"collection\"");
     * // Empty collection
     * if (cIndex < 0) return;
     * response = response.substring(cIndex);
     * response = response.substring(0, response.indexOf("</table>") + 8);
     * 
     * ArrayList<Tome> tomes = XMLParser.parseMissingTomesXML(response);
     * if (tomes == null) {
     * cmd.error = ErrorCode.SYNCMISSING_ERROR;
     * return;
     * }
     * Global.getAdaptor().checkMissingTomes(cmd.handler, tomes);
     * }
     * catch (Exception e) {
     * e.printStackTrace();
     * cmd.error = ErrorCode.SYNCMISSING_ERROR;
     * return;
     * }
     * }
     */

    public static void syncEdition(Handler handler, String eid, int sid) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH SYNC EDITION uid="
            + Global.getUserID() + " eid=" + eid + " sid=" + sid);
        getInstance().pushCommand(WSEnum.SYNC_EDITION, handler, Global.getUserID(), eid, sid);
    }

    private static void syncEdition(Command cmd, String uid, String eid,
            Integer sid) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC SYNC EDITION uid=" + uid + " eid=" + eid + " sid=" + sid);
        Resources res = Global.getResources();
        StringBuilder sb = new StringBuilder();
        sb.append(res.getString(R.string.MS_ROOT));
        sb.append(res.getString(R.string.MS_EDITION, uid, eid));
        HttpGet httpGet = null;
        try {
            //            URI uri = new URI(sb.toString());
            String uri = sb.toString();
            httpGet = new HttpGet(uri);

            // Ajout de headers
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
            httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8");

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC SYNC EDITION uid=" + uid + " eid=" + eid + " sid="
                + sid + " uri=" + uri);
        }
        catch (Exception e) {
        }

        String response = getResponse(cmd, httpGet, Charset.forName("UTF-8"));
        if (cmd.error != ErrorCode.NONE) return;

        try {
            response = response.substring(response.indexOf("<div id=\"liste_volumes_collection\""));
            response = response.substring(0, response.indexOf("</div>", response.indexOf("</ul>")) + 6);
            ArrayList<Tome> tomes = XMLParser.parseEditionXML(response);

            for (Tome tome : tomes) {
                tome.setEditionId(Integer.parseInt(eid));
                tome.setSerieId(sid);
                Global.getAdaptor().insertTome(cmd.handler, tome);
            }
        }
        catch (Exception e) {
            Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
            cmd.error = ErrorCode.SYNCEDITION_ERROR;
            cmd.errorTrace = e;
            return;
        }
    }

    public static void getTomeIcon(Handler handler, Tome tome) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH GET ICON tid="
            + tome.getId() + " turl=" + tome.getIconUrl());
        getInstance().pushCommand(WSEnum.GET_TOMEICON, handler, tome);
    }

    private static void getTomeIcon(Command cmd, Tome tome) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC GET ICON tid=" + tome.getId() + " turl="
            + tome.getIconUrl());
        Resources res = Global.getResources();
        StringBuilder sb = new StringBuilder();
        if (!tome.getIconUrl().startsWith("http://")) {
            //            if(tome.isMissingTome)
            //                sb.append(res.getString(R.string.MS_ROOT_MOBILE)).append('/');
            //            else 
            sb.append(res.getString(R.string.MS_ROOT));
        }
        sb.append(tome.getIconUrl());
        HttpGet httpGet = null;
        try {
            //            URI uri = new URI(sb.toString());
            String uri = sb.toString();
            httpGet = new HttpGet(uri);

            // Ajout de headers
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
            httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8");

            CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                + "] EXEC GET ICON tid=" + tome.getId() + " turl="
                + tome.getIconUrl() + " uri=" + uri);

            Log.i(Global.getLogTag(ServerConnector.class), "Exec HTTP  uri:"
                + httpGet.getURI());
        }
        catch (Exception e) {
            Log.e(Global.getLogTag(ServerConnector.class), "Exec error e=" + e);
        }

        DefaultHttpClient httpClient = null;

        Log.i(Global.getLogTag(ServerConnector.class), "Exec HTTP  uri:"
            + httpGet.getURI());
        int retry = 0;
        while (retry < MAX_RETRY) {
            // Création d'un DefaultHttpClient et un HttpGet permettant d'effectuer
            // une requête HTTP de type GET
            httpClient = new DefaultHttpClient();
            try {
                // Execution du client HTTP avec le HttpGet
                HttpResponse httpResponse = httpClient.execute(httpGet);

                int L = 10240;
                int count = 0;
                int r = 0;
                byte[] buffer = new byte[L];
                byte[] b = new byte[L];
                InputStream is = httpResponse.getEntity().getContent();
                while (r >= 0) {
                    r = is.read(b, 0, L);
                    if (count + r >= buffer.length)
                        buffer = ArraysUtils.arraybyteexpend(buffer, L);
                    for (int j = 0; j < r; count++, j++)
                        buffer[count] = b[j];
                }
                tome.setIcon(buffer);
                Log.d(Global.getLogTag(ServerConnector.class), "[" + cmd.id
                    + "] GET ICON image length=" + count);
                break;
            }
            catch (Exception e) {
                Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
                Log.i(Global.getLogTag(ServerConnector.class), "HTTP error["
                    + retry + "]:" + e.getMessage());
                retry++;
            }
        }
        if (retry == MAX_RETRY) {
            Log.e(Global.getLogTag(ServerConnector.class), "NETWORK_ERROR");
            cmd.error = ErrorCode.NETWORK_ERROR;
            return;
        }

        if (Log.isDebugging())
            CacheFileUtils.writeBinDebugFile("http_" + cmd.id + ".bin", tome.getIcon());

        if (!Global.getAdaptor().updateTomeIcon(tome)) {
            cmd.error = ErrorCode.GETICON_ERROR;
        }
        return;
    }

    public static void getMissingTomeIcon(Handler handler, Tome tome) {
        Log.d(Global.getLogTag(ServerConnector.class), "PUSH GET MISSING ICON");
        getInstance().pushCommand(WSEnum.GET_MISSINGTOMEICON, handler, tome);
    }

    private static void getMissingTomeIcon(Command cmd, Tome tome) {
        Log.e(Global.getLogTag(ServerConnector.class), "[" + cmd.id
            + "] EXEC GET ICON tid=" + tome.getId() + " turl="
            + tome.getIconUrl());
        if (tome.getIcon() != null) return;
        if (tome.getIconUrl() != null) {
            ServerConnector.getTomeIcon(cmd.handler, tome);
        }
        else {
            // get tome page and get icon url
            Resources res = Global.getResources();
            StringBuilder sb = new StringBuilder();
            if (!tome.getTomePageUrl().startsWith("http://"))
                sb.append(res.getString(R.string.MS_ROOT_MOBILE)).append('/');
            sb.append(tome.getTomePageUrl());
            HttpGet httpGet = null;
            try {
                //                URI uri = new URI(sb.toString());
                String uri = sb.toString();
                httpGet = new HttpGet(uri);

                // Ajout de headers
                httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
                httpGet.setHeader("Accept-Charset", "ISO-8859-1,utf-8");
                CacheFileUtils.appendDebugFile("http_list.html", "[" + cmd.id
                    + "] EXEC GET ICON tid=" + tome.getId() + " turl="
                    + tome.getIconUrl() + " uri=" + uri);
            }
            catch (Exception e) {
            }

            String response = getResponse(cmd, httpGet, Charset.forName("UTF-8"));
            if (cmd.error != ErrorCode.NONE) return;

            try {
                response = response.substring(response.indexOf("<div id=\"image_serie\""));
                response = response.substring(0, response.indexOf("</div>") + 6);
                Document doc = Jsoup.parse(response);
                Element node = doc.getElementsByTag("img").first();
                String sUrl = node.attr("src");
                if (sUrl != null) {

                    Log.w("test", "sUrl avant=" + sUrl);

                    if (sUrl.indexOf('/') != 0 && !sUrl.startsWith("http"))
                        sUrl = new StringBuilder().append('/').append(sUrl).toString();
                    int i = sUrl.lastIndexOf('/') + 1;
                    int j = sUrl.lastIndexOf('.');
                    if (j > 0)
                        sUrl = new StringBuilder(sUrl.substring(0, i)).append(URLEncoder.encode(sUrl.substring(i, j))).append(sUrl.substring(j)).toString();
                    else
                        sUrl = new StringBuilder(sUrl.substring(0, i)).append(URLEncoder.encode(sUrl.substring(i))).toString();
                    Log.w("test", "i=" + i + " j=" + j + " sUrl apres=" + sUrl);
                    tome.setIconUrl(sUrl);
                }
                Global.getAdaptor().insertTome(cmd.handler, tome);
            }
            catch (Exception e) {
                Log.printStackTrace(Global.getLogTag(ServerConnector.class), e);
                cmd.error = ErrorCode.GETMISSINGTOMEICON_ERROR;
                cmd.errorTrace = e;
                return;
            }

        }
    }
}
