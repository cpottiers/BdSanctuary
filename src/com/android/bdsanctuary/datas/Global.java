package com.android.bdsanctuary.datas;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.android.bdsanctuary.bd.MyDbAdaptor;
import com.cyrilpottiers.androlib.Log;
import com.cyrilpottiers.androlib.cache.CacheFileUtils;

public class Global {
    private final static String TAG                  = "MangaSanctuary";
    private final static String PREF_NAME            = "MS_Pref";

    private final static String PREF_USERID          = "ID";
    private final static String PREF_USERNAME        = "USERNAME";
    private final static String PREF_PASSWORD        = "PASSWORD";
    private final static String PREF_LASTREFRESHDATE = "REFRESHDATE";

    private static Global       instance             = null;

    private static Context      context              = null;
    private Resources           resources            = null;
    private MyDbAdaptor         adaptor              = null;
    private SharedPreferences   pref                 = null;

    private Global(Context context) {
        Log.initialize(context, true);
        Log.d(getLogTag(Global.class), "create new Global");
        Global.context = context;
        this.resources = context.getResources();
        adaptor = new MyDbAdaptor(context);
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
        CacheFileUtils.instantiateCacheFile(context);
    };

    public static String getLogTag(Class<?> cClass) {
        return new StringBuilder().append(TAG).append('-').append(cClass.getSimpleName()).toString();
    }

    public static void initialize(Context context) {
        if (instance == null) instance = new Global(context);
    }

    public static MyDbAdaptor getAdaptor() {
        if (instance == null) instance = new Global(context);
        if (instance.adaptor == null)
            instance.adaptor = new MyDbAdaptor(context);
        return instance.adaptor;
    }

    public static Context getContext() {
        return context;
    }

    public static Resources getResources() {
        if (instance == null) return null;
        return instance.resources;
    }

    public static String getUserID() {
        if (instance == null) return null;
        return instance.pref.getString(PREF_USERID, null);
    }

    public static String getUsername() {
        if (instance == null) return null;
        return instance.pref.getString(PREF_USERNAME, null);
    }

    public static String getPassword() {
        if (instance == null) return null;
        return instance.pref.getString(PREF_PASSWORD, null);
    }

    public static long getLastRefreshDate() {
        if (instance == null) return 0;
        return instance.pref.getLong(PREF_LASTREFRESHDATE, 0);
    }

    public static void setUsername(String username) {
        if (instance == null) return;
        Editor edit = instance.pref.edit();
        edit.putString(PREF_USERNAME, username);
        edit.commit();
    }

    public static void setPassword(String password) {
        if (instance == null) return;
        Editor edit = instance.pref.edit();
        edit.putString(PREF_PASSWORD, password);
        edit.commit();
    }

    public static void setUserId(String id) {
        if (instance == null) return;
        Editor edit = instance.pref.edit();
        edit.putString(PREF_USERID, id);
        edit.commit();
    }

    public static void setLastRefreshDate(long timeInMillis) {
        if (instance == null) return;
        Editor edit = instance.pref.edit();
        edit.putLong(PREF_LASTREFRESHDATE, timeInMillis);
        edit.commit();
    }
}
