package com.android.bdsanctuary.bd;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;

import com.android.bdsanctuary.datas.Global;
import com.android.bdsanctuary.datas.Serie;
import com.android.bdsanctuary.datas.Serie.Status;
import com.android.bdsanctuary.datas.Tome;
import com.android.bdsanctuary.http.ServerConnector;
import com.cyrilpottiers.androlib.Log;

public class MyDbAdaptor {

    private MySQLiteOpenHelper baseHelper;
    private SQLiteDatabase     database;

    public MyDbAdaptor(Context context) {
        Log.d(Global.getLogTag(MyDbAdaptor.class), "create new Adaptor");
        baseHelper = new MySQLiteOpenHelper(context);
    }

    public void openToRead() throws android.database.SQLException {
        database = baseHelper.getReadableDatabase();
    }

    public void openToWrite() throws android.database.SQLException {
        database = baseHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null) database.close();
        database = null;
    }

    public boolean insertSerie(Handler handler, Serie serie) {
        if (serie == null) return false;
        Hashtable<String, String> editions = serie.getEditions();
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insertSerie "
            + serie.getName() + "(" + serie.getId() + ") - "
            + serie.getTomeCount());
        Enumeration<String> e = editions.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String value = editions.get(key);
            Log.d(Global.getLogTag(MyDbAdaptor.class), "id_edition = " + key
                + " count=" + value);
        }

        boolean result = false;
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COL_SERIES_ID, serie.getId());
        values.put(MySQLiteOpenHelper.COL_SERIES_NAME, serie.getName());
        values.put(MySQLiteOpenHelper.COL_SERIES_STATUS, serie.getStatus().getValue());

        // Find row
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_SERIES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_SERIES_ID).append("=?").toString(), new String[] { Integer.toString(serie.getId()) }, null, null, null);

        Enumeration<String> key = serie.getEditions().keys();
        int tome_count;
        String id_edition;
        if (cursor.getCount() == 0) {
            result = (database.insert(MySQLiteOpenHelper.TABLE_SERIES, null, values) >= 0);
            while (key.hasMoreElements()) {
                id_edition = key.nextElement();
                tome_count = Integer.parseInt(serie.getEditions().get(id_edition));
                if (tome_count != getTomesCountFromEditionId(id_edition)) {
                    // sync editions tomes
                    ServerConnector.syncEdition(handler, id_edition, serie.getId());
                }
            }
        }
        else {
            cursor.moveToFirst();
            int tomeCount = 0;
            if (serie.getTomeCount() != (tomeCount = getTomesCountFromSerieId(serie.getId()))) {
                while (key.hasMoreElements()) {
                    id_edition = key.nextElement();
                    tome_count = Integer.parseInt(serie.getEditions().get(id_edition));
                    if (tome_count != getTomesCountFromEditionId(id_edition)) {
                        // sync editions tomes
                        ServerConnector.syncEdition(handler, id_edition, serie.getId());
                    }
                }
            }
            else if (tomeCount > 0) checkTomeIcons(handler, serie.getId());

            Serie saved = loadSerieFromCursor(cursor);
            if (!(result = saved.equals(serie)))
                result = (database.update(MySQLiteOpenHelper.TABLE_SERIES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_SERIES_ID).append("=?").toString(), new String[] { Integer.toString(serie.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }

    private void checkTomeIcons(Handler handler, int sid) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_UNICON_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(sid) });
        if (cursor.moveToFirst()) {
            Tome tome;
            while (!cursor.isAfterLast()) {
                tome = new Tome();
                tome.setId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ID)));
                tome.setNumber(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_NUMBER)));
                tome.setEditionId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_EDITION_ID)));
                tome.setIconUrl(cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICONURL)));
                tome.setSerieId(sid);
                ServerConnector.getTomeIcon(handler, tome);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    public Serie loadSerieFromCursor(Cursor cursor) {
        Serie serie = new Serie();
        int columnIndex;
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_ID)) >= 0)
            serie.setId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_NAME)) >= 0)
            serie.setName(cursor.getString(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_STATUS)) >= 0)
            serie.setStatus(Status.getValue(cursor.getInt(columnIndex)));
        return serie;
    }

    public Cursor getAllSeries() {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_ALL_SERIES_RQST, null);
        Log.d(Global.getLogTag(MyDbAdaptor.class), MySQLiteOpenHelper.SELECT_ALL_SERIES_RQST
            + " -> " + cursor.getCount());
        return cursor;
    }

    public Cursor getAllMissingSeries() {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_ALL_MISSING_SERIES_RQST, null);
        Log.d(Global.getLogTag(MyDbAdaptor.class), MySQLiteOpenHelper.SELECT_ALL_MISSING_SERIES_RQST
            + " -> " + cursor.getCount());
        return cursor;
    }

    public int getTomesCountFromSerieId(int serie_id) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_COUNT_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(serie_id) });
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("COUNT(*)"));
        cursor.close();
        return count;
    }

    public boolean insertTome(Handler handler, Tome tome) {
        if (tome == null) return false;
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insertTome "
            + tome.getNumber() + "(" + tome.getId() + ") - "
            + tome.getSerieId() + " url=" + tome.getIconUrl() + " icon="
            + tome.getIcon());

        boolean result = false;
        ContentValues values = new ContentValues();
        if (tome.isMissingTome) {
            values.put(MySQLiteOpenHelper.COL_MISSING_SERIE_ID, tome.getSerieId());
            values.put(MySQLiteOpenHelper.COL_MISSING_NUMBER, tome.getNumber());
            values.put(MySQLiteOpenHelper.COL_MISSING_ICONURL, tome.getIconUrl());
            if (tome.getIconUrl() == null)
                values.putNull(MySQLiteOpenHelper.COL_MISSING_ICON);
        }
        else {
            values.put(MySQLiteOpenHelper.COL_TOME_ID, tome.getId());
            values.put(MySQLiteOpenHelper.COL_TOME_SERIE_ID, tome.getSerieId());
            values.put(MySQLiteOpenHelper.COL_TOME_EDITION_ID, tome.getEditionId());
            values.put(MySQLiteOpenHelper.COL_TOME_NUMBER, tome.getNumber());
            values.put(MySQLiteOpenHelper.COL_TOME_PAGEURL, tome.getTomePageUrl());
            values.put(MySQLiteOpenHelper.COL_TOME_ICONURL, tome.getIconUrl());
            if (tome.getIconUrl() == null)
                values.putNull(MySQLiteOpenHelper.COL_TOME_ICON);
        }
        // Find row
        Cursor cursor = null;
        if (tome.isMissingTome)
            cursor = database.query(MySQLiteOpenHelper.TABLE_MISSING, null, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);
        else
            cursor = database.query(MySQLiteOpenHelper.TABLE_TOMES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);

        if (cursor.getCount() == 0) {
            if (tome.isMissingTome)
                result = (database.insert(MySQLiteOpenHelper.TABLE_MISSING, null, values) >= 0);
            else
                result = (database.insert(MySQLiteOpenHelper.TABLE_TOMES, null, values) >= 0);
            if (tome.getIconUrl() != null)
                ServerConnector.getTomeIcon(handler, tome);
        }
        else {
            cursor.moveToFirst();
            Tome saved = loadTomeFromCursor(cursor);
            Log.d(Global.getLogTag(MyDbAdaptor.class), "saved url="
                + saved.getIconUrl() + " icon=" + saved.getIcon());
            if (tome.getIconUrl() != null) {
                if (saved.getIconUrl() != null
                    && tome.getIconUrl().equals(saved.getIconUrl())
                    && saved.getIcon() != null) {
                    if (tome.isMissingTome)
                        values.remove(MySQLiteOpenHelper.COL_MISSING_ICON);
                    else
                        values.remove(MySQLiteOpenHelper.COL_TOME_ICON);
                }
                else {
                    ServerConnector.getTomeIcon(handler, tome);
                }
            }

            if (!tome.equals(saved)) {
                if (tome.isMissingTome)
                    result = (database.update(MySQLiteOpenHelper.TABLE_MISSING, values, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
                else
                    result = (database.update(MySQLiteOpenHelper.TABLE_TOMES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
            }
        }
        cursor.close();
        return result;
    }

    public Tome loadTomeFromCursor(Cursor cursor) {
        Tome tome = new Tome();
        int columnIndex;
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ID)) >= 0)
            tome.setId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_EDITION_ID)) >= 0)
            tome.setEditionId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_SERIE_ID)) >= 0)
            tome.setSerieId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_NUMBER)) >= 0)
            tome.setNumber(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICONURL)) >= 0)
            tome.setIconUrl(cursor.getString(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICON)) >= 0)
            tome.setIcon(cursor.getBlob(columnIndex));
        return tome;
    }

    public int getTomesCountFromEditionId(String id_edition) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_COUNT_TOME_FROM_EDITION_ID_RQST, new String[] { id_edition });
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("COUNT(*)"));
        cursor.close();
        return count;
    }

    public Cursor getAllTomesFromSerieId(int serie_id) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(serie_id) });
        Log.w(Global.getLogTag(MyDbAdaptor.class), "getAllTomesFromSerieId("
            + serie_id + ") -> " + cursor.getCount());
        return cursor;
    }

    public Cursor getAllMissingTomesFromSerieId(int serie_id) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_MISSINGTOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(serie_id) });
        Log.w(Global.getLogTag(MyDbAdaptor.class), "getAllTomesFromSerieId("
            + serie_id + ") -> " + cursor.getCount());
        return cursor;
    }

    public boolean updateTomeIcon(Tome tome) {
        if (tome == null || database == null) return false;
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insert tome icon "
            + tome.getNumber() + "(" + tome.getId() + ") - "
            + tome.getSerieId());

        boolean result = false;
        ContentValues values = new ContentValues();
        if (tome.isMissingTome)
            values.put(MySQLiteOpenHelper.COL_MISSING_ICON, tome.getIcon());
        else
            values.put(MySQLiteOpenHelper.COL_TOME_ICON, tome.getIcon());

        // Find row
        Cursor cursor = null;
        if (tome.isMissingTome)
            cursor = database.query(MySQLiteOpenHelper.TABLE_MISSING, null, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);
        else
            cursor = database.query(MySQLiteOpenHelper.TABLE_TOMES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        else {
            cursor.moveToFirst();
            if (tome.isMissingTome)
                result = (database.update(MySQLiteOpenHelper.TABLE_MISSING, values, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
            else
                result = (database.update(MySQLiteOpenHelper.TABLE_TOMES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }

    public boolean deleteTomeIcon(Tome tome) {
        if (tome == null || database == null) return false;
        Log.d(Global.getLogTag(MyDbAdaptor.class), "delete tome icon "
            + tome.getId());

        boolean result = false;
        ContentValues values = new ContentValues();
        if (tome.isMissingTome)
            values.putNull(MySQLiteOpenHelper.COL_MISSING_ICON);
        else
            values.putNull(MySQLiteOpenHelper.COL_TOME_ICON);

        // Find row
        Cursor cursor = null;
        if (tome.isMissingTome)
            cursor = database.query(MySQLiteOpenHelper.TABLE_MISSING, null, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);
        else
            cursor = database.query(MySQLiteOpenHelper.TABLE_TOMES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        else {
            cursor.moveToFirst();
            if (tome.isMissingTome)
                result = (database.update(MySQLiteOpenHelper.TABLE_MISSING, values, new StringBuilder().append(MySQLiteOpenHelper.COL_MISSING_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
            else
                result = (database.update(MySQLiteOpenHelper.TABLE_TOMES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }

    public boolean checkMissingTomes(Handler handler, ArrayList<Tome> tomes) {
        Tome tome;

        Log.d(Global.getLogTag(MyDbAdaptor.class), "checkMissingTomes stack1:");
        for (Tome t : tomes)
            Log.d(Global.getLogTag(MyDbAdaptor.class), " sid=" + t.getSerieId()
                + " num=" + t.getNumber() + " url=" + t.getTomePageUrl());

        Log.d(Global.getLogTag(MyDbAdaptor.class), "clean missing tomes table");
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_ALL_MISSING_RQST, null);
        if (cursor.moveToFirst()) {
            boolean found;
            String id;
            while (!cursor.isAfterLast()) {
                found = false;
                tome = new Tome();
                tome.isMissingTome = true;
                tome.setNumber(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_NUMBER)));
                tome.setSerieId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_SERIE_ID)));
                tome.setTomePageUrl(cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_PAGEURL)));
                id = cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ID));
                for (Tome t : tomes)
                    if (t.equals(tome)) {
                        tomes.remove(t);
                        found = true;
                        break;
                    }
                if (!found) {
                    // delete tome;
                    Log.d(Global.getLogTag(MyDbAdaptor.class), "delete tome id="
                        + id
                        + " result="
                        + database.delete(MySQLiteOpenHelper.TABLE_MISSING, MySQLiteOpenHelper.COL_MISSING_ID
                            + "=?", new String[] { id }));
                }
                cursor.moveToNext();
            }
        }
        cursor.close();

        Log.d(Global.getLogTag(MyDbAdaptor.class), "checkMissingTomes stack2:");
        ContentValues values;
        for (Tome t : tomes) {
            Log.d(Global.getLogTag(MyDbAdaptor.class), " sid=" + t.getSerieId()
                + " num=" + t.getNumber() + " url=" + t.getTomePageUrl());
            values = new ContentValues();
            values.put(MySQLiteOpenHelper.COL_MISSING_SERIE_ID, t.getSerieId());
            values.put(MySQLiteOpenHelper.COL_MISSING_NUMBER, t.getNumber());
            values.put(MySQLiteOpenHelper.COL_MISSING_PAGEURL, t.getTomePageUrl());

            database.insert(MySQLiteOpenHelper.TABLE_MISSING, null, values);
        }
        return true;
    }

    public boolean checkMissingTomesIcons(Handler handler) {
        Tome tome;

        Log.d(Global.getLogTag(MyDbAdaptor.class), "checkMissingTomesIcons");
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_ALL_MISSING_WITHOUT_ICON_RQST, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                tome = new Tome();
                tome.isMissingTome = true;
                tome.setId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ID)));
                tome.setNumber(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_NUMBER)));
                tome.setSerieId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_SERIE_ID)));
                tome.setTomePageUrl(cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_PAGEURL)));
                tome.setIconUrl(cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ICONURL)));
                tome.setIcon(cursor.getBlob(cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ICON)));
                ServerConnector.getMissingTomeIcon(handler, tome);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return true;
    }

    //    public boolean insertMissingTome(Handler handler, Tome tome) {
    //        if (tome == null) return false;
    //        Log.d(Global.getLogTag(MyDbAdaptor.class), "insert missing Tome "
    //            + tome.getNumber() + " - " + tome.getSerieId());
    //
    //        boolean result = false;
    //        ContentValues values = new ContentValues();
    //        values.put(MySQLiteOpenHelper.COL_MISSING_SERIE_ID, tome.getSerieId());
    //        values.put(MySQLiteOpenHelper.COL_MISSING_NUMBER, tome.getNumber());
    //
    //        result = (database.insert(MySQLiteOpenHelper.TABLE_MISSING, null, values) >= 0);
    //
    //        return result;
    //    }

    public Serie lookupSerieFromName(String serie_name) {
        Serie serie = null;
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_SERIE_FROM_NAME_RQST, new String[] { serie_name });
        Log.w(Global.getLogTag(MyDbAdaptor.class), "lookupSerieFromName("
            + serie_name + ") -> " + cursor.getCount());

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        else {
            cursor.moveToFirst();
            //          serie = new Serie();
            //          serie.setId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_ID)));
            serie = loadSerieFromCursor(cursor);
        }
        cursor.close();
        return serie;
    }

    public Serie lookupSerieFromId(Integer sid) {
        Serie serie = null;
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_SERIE_FROM_ID_RQST, new String[] { String.valueOf(sid) });
        Log.w(Global.getLogTag(MyDbAdaptor.class), "lookupSerieFromId(" + sid
            + ") -> " + cursor.getCount());

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        else {
            cursor.moveToFirst();
            serie = loadSerieFromCursor(cursor);
        }
        cursor.close();
        return serie;
    }
}
