package com.android.bdsanctuary.bd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.bdsanctuary.datas.Global;
import com.android.bdsanctuary.datas.Serie;
import com.cyrilpottiers.androlib.Log;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String   DATABASE_NAME                                = "MS.sqlite";
    private static final int      RELEASE1_0                                   = 3;
    private static final int      RELEASE1_2                                   = 4;
    private static final int      RELEASE1_3                                   = 5;

    public static final String    TABLE_SERIES                                 = "series";
    public static final String    COL_SERIES_ID                                = "id";
    public static final String    COL_SERIES_NAME                              = "name";
    public static final String    COL_SERIES_STATUS                            = "status";

    private static final String   CREATE_SERIES_TABLE                          = "CREATE TABLE "
                                                                                   + TABLE_SERIES
                                                                                   + " ("
                                                                                   + COL_SERIES_ID
                                                                                   + " INTEGER PRIMARY KEY NOT NULL, "
                                                                                   + COL_SERIES_NAME
                                                                                   + " TEXT NOT NULL, "
                                                                                   + COL_SERIES_STATUS
                                                                                   + " INTEGER NOT NULL "
                                                                                   + ");";

    public static final String    TABLE_TOMES                                  = "tomes";
    public static final String    COL_TOME_ID                                  = "id";
    public static final String    COL_TOME_NUMBER                              = "number";
    public static final String    COL_TOME_SERIE_ID                            = "serie_id";
    public static final String    COL_TOME_EDITION_ID                          = "edition_id";
    public static final String    COL_TOME_PAGEURL                             = "page_url";
    public static final String    COL_TOME_ICON                                = "icone";
    public static final String    COL_TOME_ICONURL                             = "icone_url";

    private static final String   CREATE_TOME_TABLE                            = "CREATE TABLE "
                                                                                   + TABLE_TOMES
                                                                                   + " ("
                                                                                   + COL_TOME_ID
                                                                                   + " INTEGER PRIMARY KEY NOT NULL, "
                                                                                   + COL_TOME_NUMBER
                                                                                   + " INTEGER NOT NULL, "
                                                                                   + COL_TOME_SERIE_ID
                                                                                   + " INTEGER NOT NULL, "
                                                                                   + COL_TOME_EDITION_ID
                                                                                   + " INTEGER NOT NULL, "
                                                                                   + COL_TOME_ICON
                                                                                   + " BLOB, "
                                                                                   + COL_TOME_ICONURL
                                                                                   + " TEXT, "
                                                                                   + COL_TOME_PAGEURL
                                                                                   + " TEXT, "
                                                                                   + "FOREIGN KEY("
                                                                                   + COL_TOME_SERIE_ID
                                                                                   + ") REFERENCES "
                                                                                   + TABLE_SERIES
                                                                                   + "("
                                                                                   + COL_SERIES_ID
                                                                                   + ")"
                                                                                   + ");";

    public static final String    TABLE_MISSING                                = "missing";
    public static final String    COL_MISSING_ID                               = "_id";
    public static final String    COL_MISSING_NUMBER                           = "number";
    public static final String    COL_MISSING_SERIE_ID                         = "serie_id";
    public static final String    COL_MISSING_PAGEURL                          = "page_url";
    public static final String    COL_MISSING_ICONURL                          = "icon_url";
    public static final String    COL_MISSING_ICON                             = "icon";

    private static final String   CREATE_MISSING_TABLE                         = "CREATE TABLE "
                                                                                   + TABLE_MISSING
                                                                                   + " ("
                                                                                   + COL_MISSING_ID
                                                                                   + " INTEGER PRIMARY KEY, "
                                                                                   + COL_MISSING_SERIE_ID
                                                                                   + " INTEGER NOT NULL, "
                                                                                   + COL_MISSING_NUMBER
                                                                                   + " INTEGER NOT NULL, "
                                                                                   + COL_MISSING_PAGEURL
                                                                                   + " TEXT NOT NULL, "
                                                                                   + COL_MISSING_ICONURL
                                                                                   + " TEXT , "
                                                                                   + COL_MISSING_ICON
                                                                                   + " BLOB, "
                                                                                   + "FOREIGN KEY("
                                                                                   + COL_MISSING_SERIE_ID
                                                                                   + ") REFERENCES "
                                                                                   + TABLE_SERIES
                                                                                   + "("
                                                                                   + COL_SERIES_ID
                                                                                   + ")"
                                                                                   + ");";

    public static final String    SELECT_ALL_SERIES_RQST                       = "SELECT "
                                                                                   + COL_SERIES_ID
                                                                                   + " AS _id, "
                                                                                   + COL_SERIES_NAME
                                                                                   + ", "
                                                                                   + COL_SERIES_STATUS
                                                                                   + ", "
                                                                                   + " (SELECT COUNT(*)"
                                                                                   + " FROM "
                                                                                   + TABLE_MISSING
                                                                                   + " WHERE "
                                                                                   + TABLE_MISSING
                                                                                   + "."
                                                                                   + COL_MISSING_SERIE_ID
                                                                                   + "="
                                                                                   + COL_SERIES_ID
                                                                                   + ") AS _count"
                                                                                   + " FROM "
                                                                                   + TABLE_SERIES
                                                                                   + " ORDER BY "
                                                                                   + COL_SERIES_NAME
                                                                                   + " COLLATE LOCALIZED";

    public static final String    SELECT_ALL_MISSING_SERIES_RQST               = "SELECT "
                                                                                   + COL_SERIES_ID
                                                                                   + " AS _id, "
                                                                                   + COL_SERIES_NAME
                                                                                   + ", "
                                                                                   + COL_SERIES_STATUS
                                                                                   + ", "
                                                                                   + " (SELECT COUNT(*)"
                                                                                   + " FROM "
                                                                                   + TABLE_MISSING
                                                                                   + " WHERE "
                                                                                   + TABLE_MISSING
                                                                                   + "."
                                                                                   + COL_MISSING_SERIE_ID
                                                                                   + "="
                                                                                   + COL_SERIES_ID
                                                                                   + ") AS _count"
                                                                                   + " FROM "
                                                                                   + TABLE_SERIES
                                                                                   + " WHERE _count>0"
                                                                                   + " ORDER BY "
                                                                                   + COL_SERIES_NAME
                                                                                   + " COLLATE LOCALIZED";

    public static final String    SELECT_SERIE_FROM_NAME_RQST                  = "SELECT "
                                                                                   + COL_SERIES_ID
                                                                                   + ", "
                                                                                   + COL_SERIES_NAME
                                                                                   + ", "
                                                                                   + COL_SERIES_STATUS
                                                                                   + " FROM "
                                                                                   + TABLE_SERIES
                                                                                   + " WHERE "
                                                                                   + COL_SERIES_NAME
                                                                                   + " LIKE ?";

    public static final String    SELECT_SERIE_FROM_ID_RQST                    = "SELECT "
                                                                                   + COL_SERIES_ID
                                                                                   + ", "
                                                                                   + COL_SERIES_NAME
                                                                                   + ", "
                                                                                   + COL_SERIES_STATUS
                                                                                   + " FROM "
                                                                                   + TABLE_SERIES
                                                                                   + " WHERE "
                                                                                   + COL_SERIES_ID
                                                                                   + "=?";

    public static final String    SELECT_COUNT_TOME_FROM_SERIE_ID_RQST         = "SELECT COUNT(*) FROM "
                                                                                   + TABLE_TOMES
                                                                                   + " WHERE "
                                                                                   + COL_TOME_SERIE_ID
                                                                                   + "=?";

    public static final String    SELECT_COUNT_TOME_FROM_EDITION_ID_RQST       = "SELECT COUNT(*) FROM "
                                                                                   + TABLE_TOMES
                                                                                   + " WHERE "
                                                                                   + COL_TOME_EDITION_ID
                                                                                   + "=?";

    public static final String    SELECT_TOME_FROM_SERIE_ID_RQST               = "SELECT "
                                                                                   + COL_TOME_ID
                                                                                   + " AS _id, "
                                                                                   + COL_TOME_NUMBER
                                                                                   + ", "
                                                                                   + COL_TOME_PAGEURL
                                                                                   + ", "
                                                                                   + COL_TOME_ICON
                                                                                   + " FROM "
                                                                                   + TABLE_TOMES
                                                                                   + " WHERE "
                                                                                   + COL_TOME_SERIE_ID
                                                                                   + "=?"
                                                                                   + " ORDER BY "
                                                                                   + COL_TOME_NUMBER;

    public static final String    SELECT_MISSINGTOME_FROM_SERIE_ID_RQST        = "SELECT "
                                                                                   + COL_MISSING_ID
                                                                                   + " AS _id, "
                                                                                   + COL_MISSING_NUMBER
                                                                                   + ", "
                                                                                   + COL_MISSING_PAGEURL
                                                                                   + ", "
                                                                                   + COL_MISSING_ICON
                                                                                   + " FROM "
                                                                                   + TABLE_MISSING
                                                                                   + " WHERE "
                                                                                   + COL_MISSING_SERIE_ID
                                                                                   + "=?"
                                                                                   + " ORDER BY "
                                                                                   + COL_MISSING_NUMBER;

    public static final String    SELECT_UNICON_TOME_FROM_SERIE_ID_RQST        = "SELECT *"
                                                                                   + " FROM "
                                                                                   + TABLE_TOMES
                                                                                   + " WHERE "
                                                                                   + COL_TOME_SERIE_ID
                                                                                   + "=?"
                                                                                   + " AND "
                                                                                   + COL_TOME_ICONURL
                                                                                   + " IS NOT NULL"
                                                                                   + " AND "
                                                                                   + COL_TOME_ICON
                                                                                   + " IS NULL";

    public static final String    SELECT_ALL_MISSING_RQST                      = "SELECT * FROM "
                                                                                   + TABLE_MISSING;

    public static final String    SELECT_ALL_MISSING_WITHOUT_ICON_RQST         = "SELECT * FROM "
                                                                                   + TABLE_MISSING
                                                                                   + " WHERE "
                                                                                   + COL_MISSING_ICONURL
                                                                                   + " IS NULL"
                                                                                   + " OR "
                                                                                   + COL_MISSING_ICON
                                                                                   + " IS NULL";

    private static final String   ALTER_TOME_FROM_RELEASE_1_0_TO_RELEASE_1_2   = "ALTER TABLE "
                                                                                   + TABLE_TOMES
                                                                                   + " ADD COLUMN "
                                                                                   + COL_TOME_PAGEURL
                                                                                   + " TEXT";

    private static final String   ALTER_SERIES_FROM_RELEASE_1_2_TO_RELEASE_1_3 = "ALTER TABLE "
                                                                                   + TABLE_SERIES
                                                                                   + " ADD COLUMN "
                                                                                   + COL_SERIES_STATUS
                                                                                   + " INTEGER NOT NULL DEFAULT "
                                                                                   + Serie.Status.SUIVIE_VALUE;

    private static final String[] ALTER_FROM_RELEASE_1_0_TO_RELEASE_1_2        = new String[] { ALTER_TOME_FROM_RELEASE_1_0_TO_RELEASE_1_2, CREATE_MISSING_TABLE };
    private static final String[] ALTER_FROM_RELEASE_1_2_TO_RELEASE_1_3        = new String[] { ALTER_SERIES_FROM_RELEASE_1_2_TO_RELEASE_1_3 };
    private static final String[] ALTER_FROM_RELEASE_1_0_TO_RELEASE_1_3        = new String[] { ALTER_TOME_FROM_RELEASE_1_0_TO_RELEASE_1_2, ALTER_SERIES_FROM_RELEASE_1_2_TO_RELEASE_1_3, CREATE_MISSING_TABLE };

    public MySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, RELEASE1_3);
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "create new Helper");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onCreateDb");
        //on créé la table à partir de la requête écrite dans la variable CREATE_TOME_TABLE
        db.execSQL(CREATE_SERIES_TABLE);
        db.execSQL(CREATE_TOME_TABLE);
        db.execSQL(CREATE_MISSING_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onUpgradeDb");

        switch (oldVersion) {
            case RELEASE1_0: {
                switch (newVersion) {
                    case RELEASE1_3:
                        execBatch(db, ALTER_FROM_RELEASE_1_0_TO_RELEASE_1_3);
                        break;
                    case RELEASE1_2:
                        execBatch(db, ALTER_FROM_RELEASE_1_0_TO_RELEASE_1_2);
                        break;
                    default:
                        clean(db);
                        onCreate(db);
                        break;
                }
            }
                break;
            case RELEASE1_2: {
                switch (newVersion) {
                    case RELEASE1_3:
                        execBatch(db, ALTER_FROM_RELEASE_1_2_TO_RELEASE_1_3);
                        break;
                    default:
                        clean(db);
                        onCreate(db);
                        break;
                }
            }
                break;
            default:
                clean(db);
                onCreate(db);
                break;
        }
    }

    public void clean(SQLiteDatabase db) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onCleanDb");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOMES + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MISSING + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERIES + ";");
    }

    public void execBatch(SQLiteDatabase db, String[] queries) {
        if (queries == null) return;
        for (int i = 0; i < queries.length; i++)
            db.execSQL(queries[i]);
    }
}
