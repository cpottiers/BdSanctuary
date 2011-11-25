package com.android.bdsanctuary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bdsanctuary.bd.MySQLiteOpenHelper;
import com.android.bdsanctuary.datas.Global;
import com.android.bdsanctuary.datas.Serie;
import com.android.bdsanctuary.datas.Tome;
import com.android.bdsanctuary.http.HttpListener;
import com.android.bdsanctuary.http.ServerConnector;
import com.android.bdsanctuary.http.ServerConnector.ErrorCode;
import com.cyrilpottiers.androlib.Log;
import com.cyrilpottiers.androlib.cache.CacheFileUtils;
import com.cyrilpottiers.androlib.strings.StringUtils;
import com.cyrilpottiers.androlib.strings.StringUtils.SpanStyle;

public class MainActivity extends ListActivity implements HttpListener /*
                                                                        * ,
                                                                        * OnClickListener
                                                                        */{

    private final static int   INTENT_CODE       = 200;
    private final static int   SELECT_USER       = 0;

    private final static int   MENU_FIRST        = 0;
    private final static int   MENU_REFRESH      = MENU_FIRST + 100;
    private final static int   MENU_SHOPPING     = MENU_FIRST + 150;
    private final static int   MENU_LEGEND       = MENU_FIRST + 175;
    private final static int   MENU_QUIT         = MENU_FIRST + 200;

    CursorAdapter              cursorAdapter     = null;
    Cursor                     serieCursor       = null;
    ProgressDialog             waitingDialog     = null;
    boolean                    isMissingList     = false;

    Hashtable<Integer, Cursor> listCursorMap     = new Hashtable<Integer, Cursor>();
    Hashtable<Integer, Cursor> shoppingCursorMap = new Hashtable<Integer, Cursor>();

    private static class SeriesViewHolder {
        public TextView        separator;
        public TextView        title;
        public Gallery         tomes;
        public Serie.Status    status;
        public CharArrayBuffer titleBuffer = new CharArrayBuffer(128);
    }

    private static class TomesViewHolder {
        public ImageView icon;
        public TextView  title;
        public String    url;
    }

    private class SeriesAdapter extends CursorAdapter implements SectionIndexer {

        /**
         * State of ListView item that has never been determined.
         */
        private static final int      STATE_UNKNOWN        = 0;

        /**
         * State of a ListView item that is sectioned. A sectioned item must
         * display the separator.
         */
        private static final int      STATE_SECTIONED_CELL = 1;

        /**
         * State of a ListView item that is not sectioned and therefore does not
         * display the separator.
         */
        private static final int      STATE_REGULAR_CELL   = 2;

        private final CharArrayBuffer mBuffer              = new CharArrayBuffer(128);
        private int[]                 mCellStates;
        private int                   mColumnNameIndex, mColumnIdIndex,
                mColumnCountIndex, mColumnStatusIndex;

        HashMap<String, Integer>      alphaIndexer         = new HashMap<String, Integer>();
        String[]                      sections;
        boolean                       isMissing            = false;

        //        AlphabetIndexer               alphaIndexer;

        public SeriesAdapter(Context context, Cursor cursor, boolean isMissing) {
            super(context, cursor);
            this.isMissing = isMissing;
            // here is the tricky stuff
            // in this hashmap we will store here the positions for
            // the sections
            initAdapter(cursor);
        }

        public void initAdapter(Cursor cursor) {
            Log.w(Global.getLogTag(MainActivity.class), "SeriesAdapter initAdapter");
            mCellStates = cursor == null ? null : new int[cursor.getCount()];
            if (!isMissing) {
                mColumnCountIndex = cursor.getColumnIndex("_count");
            }
            mColumnNameIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_NAME);
            mColumnIdIndex = cursor.getColumnIndex("_id");
            mColumnStatusIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_STATUS);

            alphaIndexer.clear();
            String section;
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    cursor.copyStringToBuffer(mColumnNameIndex, mBuffer);
                    section = getSectionHeader(mBuffer.data[0]);
                    if (!alphaIndexer.containsKey(section))
                        alphaIndexer.put(section, cursor.getPosition());
                    cursor.moveToNext();
                }
            }
            cursor.moveToFirst();
            ArrayList<String> array = new ArrayList<String>(alphaIndexer.keySet());
            Collections.sort(array);
            sections = new String[array.size()];
            for (int i = sections.length; --i >= 0;)
                sections[i] = array.get(i);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            Log.w(Global.getLogTag(MainActivity.class), "SeriesAdapter changeCursor");
            super.changeCursor(cursor);
            initAdapter(cursor);
        }

        @Override
        public void notifyDataSetChanged() {
            Log.w(Global.getLogTag(MainActivity.class), "SeriesAdapter notifyDataSetChanged");
            super.notifyDataSetChanged();
            initAdapter(getCursor());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SeriesViewHolder holder = (SeriesViewHolder) view.getTag();

            /*
             * Separator
             */
            boolean needSeparator = false;

            final int position = cursor.getPosition();

            if (mCellStates == null || mCellStates.length <= position) return;

            cursor.copyStringToBuffer(mColumnNameIndex, holder.titleBuffer);

            switch (mCellStates[position]) {
                case STATE_SECTIONED_CELL:
                    needSeparator = true;
                    break;

                case STATE_REGULAR_CELL:
                    needSeparator = false;
                    break;

                case STATE_UNKNOWN:
                default:
                    // A separator is needed if it's the first itemview of the
                    // ListView or if the group of the current cell is different
                    // from the previous itemview.
                    if (position == 0) {
                        needSeparator = true;
                    }
                    else {
                        cursor.moveToPosition(position - 1);

                        cursor.copyStringToBuffer(mColumnNameIndex, mBuffer);
                        if (mBuffer.sizeCopied > 0
                            && holder.titleBuffer.sizeCopied > 0
                            && !isSameSection(mBuffer.data[0], holder.titleBuffer.data[0])) {
                            needSeparator = true;
                        }

                        cursor.moveToPosition(position);
                    }

                    // Cache the result
                    mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
                    break;
            }

            if (needSeparator) {
                //                char header = holder.titleBuffer.data[0];
                String sectionHeader = getSectionHeader(holder.titleBuffer.data[0]);
                holder.separator.setText(sectionHeader);
                holder.separator.setVisibility(View.VISIBLE);
            }
            else {
                holder.separator.setVisibility(View.GONE);
            }

            holder.status = Serie.Status.getValue(cursor.getInt(mColumnStatusIndex));

            StringBuilder sb = new StringBuilder();
            sb.append(holder.titleBuffer.data, 0, holder.titleBuffer.sizeCopied);
            //            holder.title.setOnClickListener(MainActivity.this);

            int count = 0;

            if (!isMissing) {
                count = cursor.getInt(mColumnCountIndex);
                if (count > 0) sb.insert(0, "* ");

                switch (holder.status) {
                    case SUIVIE:
                        sb.insert(0, "[[image=" + R.drawable.flag_blue
                            + "]].[[//]] ");
                        break;
                    case COMPLETE:
                        sb.insert(0, "[[image=" + R.drawable.flag_green
                            + "]].[[//]] ");
                        break;
                    case NON_SUIVIE:
                        sb.insert(0, "[[image=" + R.drawable.flag_red
                            + "]].[[//]] ");
                        break;
                    case INTERROMPUE:
                        sb.insert(0, "[[image=" + R.drawable.flag_orange
                            + "]].[[//]] ");
                        break;
                }

                ArrayList<SpanStyle> spans = new ArrayList<SpanStyle>();
                holder.title.setText(StringUtils.formatSpan(context, sb.toString(), spans), TextView.BufferType.SPANNABLE);
                Spannable sText = (Spannable) holder.title.getText();
                for (SpanStyle style : spans) {
                    sText.setSpan(style.whats, style.start, style.end, style.flags);
                }
            }
            else
                holder.title.setText(sb.toString());

            int serie_id = cursor.getInt(mColumnIdIndex);
            Cursor c = null;

            if (isMissing)
                c = Global.getAdaptor().getAllMissingTomesFromSerieId(serie_id);
            else
                c = Global.getAdaptor().getAllTomesFromSerieId(serie_id);
            startManagingCursor(c);
            CursorAdapter adaptor;
            if (isMissing) {
                adaptor = new MissingTomesAdapter(context, c);
                holder.tomes.setAdapter(adaptor);
                shoppingCursorMap.put(serie_id, c);
            }
            else {
                adaptor = new TomesAdapter(context, c);
                holder.tomes.setAdapter(adaptor);
                listCursorMap.put(serie_id, c);
            }

            if (!isMissing)
                holder.tomes.setSelection(holder.tomes.getCount() - 1);

            holder.tomes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterview, View view,
                        int position, long id) {
                    final TomesViewHolder holder = (TomesViewHolder) view.getTag();
                    Log.d(Global.getLogTag(getClass()), "click url="
                        + holder.url);
                    if (holder != null && holder.url != null) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.Open_Url_Alert);
                        builder.setPositiveButton(R.string.Error_Alert_OK_Btn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(holder.url));
                                startActivity(i);
                            }
                        });
                        builder.setNegativeButton(R.string.Error_Alert_Cancel_Btn, null);
                        builder.show();
                    }
                }
            });

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            View v = LayoutInflater.from(context).inflate(R.layout.serie_item_layout, parent, false);

            // The following code allows us to keep a reference on the child
            // views of the item. It prevents us from calling findViewById at
            // each getView/bindView and boosts the rendering code.
            SeriesViewHolder holder = new SeriesViewHolder();
            holder.separator = (TextView) v.findViewById(R.id.separator);
            holder.title = (TextView) v.findViewById(R.id.serie_name);
            holder.tomes = (Gallery) v.findViewById(R.id.tome_gallery);

            v.setTag(holder);
            return v;
        }

        private boolean isSameSection(char prev, char current) {
            String prevHeader = getSectionHeader(prev);
            String currentHeader = getSectionHeader(current);
            return prevHeader.equals(currentHeader);
        }

        private String getSectionHeader(char letter) {
            String header = String.valueOf(StringUtils.getUnaccentedChar(letter)).toUpperCase();
            if (letter >= 0x30 && letter <= 0x39) header = "0-9";
            return header;
        }

        @Override
        public int getPositionForSection(int section) {
            if (section < sections.length) {
                Log.d(Global.getLogTag(SeriesAdapter.class), "getPositionForSection("
                    + section + ")=" + alphaIndexer.get(sections[section]));
                return alphaIndexer.get(sections[section]);
            }
            Log.d(Global.getLogTag(SeriesAdapter.class), "getPositionForSection("
                + section + ")=0");
            return 0;
            //            return alphaIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
            int section = 0;
            while (section < sections.length) {
                if (alphaIndexer.get(sections[section]) > position) {
                    Log.d(Global.getLogTag(SeriesAdapter.class), "getSectionForPosition("
                        + position
                        + ")="
                        + ((section - 1 < 0) ? 0 : section - 1));
                    return ((section - 1 < 0) ? 0 : section - 1);
                }
                section++;
            }
            Log.d(Global.getLogTag(SeriesAdapter.class), "getSectionForPosition("
                + position + ")=0");
            return 0;
            //            return alphaIndexer.getSectionForPosition(position);
        }

        @Override
        public Object[] getSections() {
            for (int i = 0; i < sections.length; i++)
                Log.w(Global.getLogTag(SeriesAdapter.class), "\tsections[" + i
                    + "]=" + sections[i]);

            return sections;
            //            return alphaIndexer.getSections();
        }
    }

    private class TomesAdapter extends SimpleCursorAdapter {

        private int mColumnNameIndex, mColumnIconIndex, mColumnUrlIndex;

        public TomesAdapter(Context context, Cursor cursor) {
            super(context, R.layout.tome_item_layout, cursor, new String[] { MySQLiteOpenHelper.COL_TOME_ICON, MySQLiteOpenHelper.COL_TOME_NUMBER }, new int[] { R.id.tome_icon, R.id.tome_name });
            initAdapter(cursor);
        }

        public void initAdapter(Cursor cursor) {
            Log.w(Global.getLogTag(MainActivity.class), "TomesAdapter initAdapter");
            mColumnNameIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_NUMBER);
            mColumnIconIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICON);
            mColumnUrlIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_PAGEURL);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            Log.w(Global.getLogTag(MainActivity.class), "TomesAdapter changeCursor");
            super.changeCursor(cursor);
            initAdapter(cursor);
        }

        @Override
        public void notifyDataSetChanged() {
            Log.w(Global.getLogTag(MainActivity.class), "TomesAdapter notifyDataSetChanged");
            super.notifyDataSetChanged();
            initAdapter(getCursor());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TomesViewHolder holder = (TomesViewHolder) view.getTag();
            holder.title.setText(context.getResources().getString(R.string.Tome_Title_Pattern, cursor.getInt(mColumnNameIndex)));
            byte[] datas = cursor.getBlob(mColumnIconIndex);
            if (datas == null)
                holder.icon.setImageResource(R.drawable.no_icon);
            else {
                Bitmap icon = BitmapFactory.decodeByteArray(datas, 0, datas.length);
                if (icon == null) {
                    Tome tome = new Tome();
                    tome.isMissingTome = false;
                    tome.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                    Global.getAdaptor().deleteTomeIcon(tome);
                    holder.icon.setImageResource(R.drawable.no_icon);
                }
                else
                    holder.icon.setImageBitmap(icon);
            }
            String url = cursor.getString(mColumnUrlIndex);
            Log.d(Global.getLogTag(getClass()), "build item title="
                + holder.title.getText() + " url=" + url + " icon="
                + holder.icon);
            if (url != null && !url.startsWith("http")) {
                url = new StringBuilder().append(getResources().getString(R.string.MS_ROOT)).append('/').append(url).toString();
            }
            holder.url = url;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = LayoutInflater.from(context).inflate(R.layout.tome_item_layout, parent, false);

            // The following code allows us to keep a reference on the child
            // views of the item. It prevents us from calling findViewById at
            // each getView/bindView and boosts the rendering code.
            TomesViewHolder holder = new TomesViewHolder();
            holder.icon = (ImageView) v.findViewById(R.id.tome_icon);
            holder.title = (TextView) v.findViewById(R.id.tome_name);

            v.setTag(holder);
            return v;
        }
    }

    private class MissingTomesAdapter extends SimpleCursorAdapter {

        private int mColumnNameIndex, mColumnIconIndex, mColumnUrlIndex;

        public MissingTomesAdapter(Context context, Cursor cursor) {
            super(context, R.layout.tome_item_layout, cursor, new String[] { MySQLiteOpenHelper.COL_MISSING_ICON, MySQLiteOpenHelper.COL_MISSING_NUMBER }, new int[] { R.id.tome_icon, R.id.tome_name });
            initAdapter(cursor);
        }

        public void initAdapter(Cursor cursor) {
            mColumnNameIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_NUMBER);
            mColumnIconIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ICON);
            mColumnUrlIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_PAGEURL);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            initAdapter(cursor);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            initAdapter(getCursor());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TomesViewHolder holder = (TomesViewHolder) view.getTag();
            holder.title.setText(context.getResources().getString(R.string.Tome_Title_Pattern, cursor.getInt(mColumnNameIndex)));
            byte[] datas = cursor.getBlob(mColumnIconIndex);
            if (datas == null)
                holder.icon.setImageResource(R.drawable.no_icon);
            else {
                Bitmap icon = BitmapFactory.decodeByteArray(datas, 0, datas.length);
                if (icon == null) {
                    Tome tome = new Tome();
                    tome.isMissingTome = true;
                    tome.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                    Global.getAdaptor().deleteTomeIcon(tome);
                    holder.icon.setImageResource(R.drawable.no_icon);
                }
                else
                    holder.icon.setImageBitmap(icon);
            }
            String url = cursor.getString(mColumnUrlIndex);
            Log.d(Global.getLogTag(getClass()), "build item title="
                + holder.title.getText() + " url=" + url + " icon="
                + holder.icon);
            if (url != null && !url.startsWith("http")) {
                url = new StringBuilder().append(getResources().getString(R.string.MS_ROOT_MOBILE)).append('/').append(url).toString();
            }
            holder.url = url;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = LayoutInflater.from(context).inflate(R.layout.tome_item_layout, parent, false);

            // The following code allows us to keep a reference on the child
            // views of the item. It prevents us from calling findViewById at
            // each getView/bindView and boosts the rendering code.
            TomesViewHolder holder = new TomesViewHolder();
            holder.icon = (ImageView) v.findViewById(R.id.tome_icon);
            holder.title = (TextView) v.findViewById(R.id.tome_name);

            v.setTag(holder);
            return v;
        }

    }

    public Handler handler = new Handler() {
                               @Override
                               public void handleMessage(Message msg) {
                                   switch (msg.what) {
                                       case ServerConnector.WSEnum.GET_ID_VALUE: {
                                           Bundle b = msg.getData();
                                           ErrorCode error = ErrorCode.getValue(b.getInt(ServerConnector.ERROR));

                                           if (error != ErrorCode.NONE) {
                                               switch (error) {
                                                   case NETWORK_ERROR:
                                                       Toast.makeText(MainActivity.this, R.string.Error_Alert_Server, Toast.LENGTH_LONG).show();
                                                       break;
                                                   case GETID_ERROR:
                                                       Toast.makeText(MainActivity.this, R.string.Error_Alert_IdUnknown, Toast.LENGTH_LONG).show();
                                                       break;
                                               }
                                               // Cancel progressDialog
                                               if (waitingDialog != null
                                                   && waitingDialog.isShowing())
                                                   waitingDialog.dismiss();
                                               Log.v(Global.getLogTag(MainActivity.class), "select user");
                                               Intent intent = new Intent(MainActivity.this, SelectUserActivity.class);
                                               intent.putExtra(SelectUserActivity.IS_NOT_CANCELLABLE_EXTRA, true);
                                               startActivityForResult(intent, INTENT_CODE
                                                   + SELECT_USER);
                                           }
                                           else {
                                               waitingDialog.setMessage(getResources().getString(R.string.Sync_Alert_Series));
                                               // lancer la synchro en arrière plan
                                               Log.v(Global.getLogTag(MainActivity.class), "sync series");
                                               ServerConnector.syncSeries(handler);
                                           }
                                       }
                                           break;
                                       case ServerConnector.WSEnum.SYNC_MISSING_VALUE:
                                       case ServerConnector.WSEnum.SYNC_SERIES_VALUE: {
                                           Bundle b = msg.getData();
                                           ErrorCode error = ErrorCode.getValue(b.getInt(ServerConnector.ERROR));

                                           if (error != ErrorCode.NONE) {
                                               switch (error) {
                                                   case NETWORK_ERROR:
                                                       Toast.makeText(MainActivity.this, R.string.Error_Alert_Server, Toast.LENGTH_LONG).show();
                                                       break;
                                                   case SYNCSERIES_ERROR:
                                                       Toast.makeText(MainActivity.this, R.string.Error_Alert_Synchro, Toast.LENGTH_LONG).show();
                                                       CacheFileUtils.appendErrorFile((Throwable)b.getSerializable(ServerConnector.ERROR_TRACE));
                                                       break;
                                               }
                                           }
                                           else {
                                               if (msg.what == ServerConnector.WSEnum.SYNC_SERIES_VALUE) {
                                                   // on rafraichit la liste des éléments manquants
                                                   ServerConnector.syncMissing(this);
                                               }
                                               else {
                                                   // on récupère les icon des éléments manquants
                                                   Global.getAdaptor().checkMissingTomesIcons(handler);
                                               }
                                               // refresh listView
                                               try {
                                                   if (serieCursor != null)
                                                       serieCursor.requery();
                                                   cursorAdapter.notifyDataSetChanged();
                                               }
                                               catch (Exception e) {
                                                   Log.printStackTrace(Global.getLogTag(MainActivity.class), e);
                                               }
                                           }
                                           // Cancel progressDialog
                                           if (waitingDialog != null
                                               && waitingDialog.isShowing())
                                               waitingDialog.dismiss();
                                       }
                                           break;
                                       case ServerConnector.WSEnum.GET_TOMEICON_VALUE:
                                       case ServerConnector.WSEnum.SYNC_EDITION_VALUE: {
                                           Bundle b = msg.getData();
                                           ErrorCode error = ErrorCode.getValue(b.getInt(ServerConnector.ERROR));
                                           if (error != ErrorCode.NONE) {
                                               switch (error) {
                                                   case NETWORK_ERROR:
                                                       Toast.makeText(MainActivity.this, R.string.Error_Alert_Server, Toast.LENGTH_LONG).show();
                                                       break;
                                                   case SYNCEDITION_ERROR: {
                                                       Serie serie = Global.getAdaptor().lookupSerieFromId(b.getInt(ServerConnector.SERIE_ID, -1));
                                                       if (serie != null)
                                                           Toast.makeText(MainActivity.this, Global.getResources().getString(R.string.Error_Alert_SynchroSerie, serie.getName()), Toast.LENGTH_LONG).show();
                                                       CacheFileUtils.appendErrorFile((Throwable)b.getSerializable(ServerConnector.ERROR_TRACE));
                                                       break;
                                                   }
                                               }
                                           }
                                           else {
                                               // refresh EDITION gallery
                                               int eid = b.getInt(ServerConnector.EDITION_ID, -1);
                                               if (eid >= 0) {
                                                   Cursor cursor = listCursorMap.get(eid);
                                                   try {
                                                       if (cursor != null)
                                                           cursor.requery();
                                                       cursorAdapter.notifyDataSetChanged();
                                                   }
                                                   catch (Exception e) {
                                                       Log.printStackTrace(Global.getLogTag(MainActivity.class), e);
                                                   }
                                                   cursor = shoppingCursorMap.get(eid);
                                                   try {
                                                       if (cursor != null)
                                                           cursor.requery();
                                                       cursorAdapter.notifyDataSetChanged();
                                                   }
                                                   catch (Exception e) {
                                                       Log.printStackTrace(Global.getLogTag(MainActivity.class), e);
                                                   }
                                               }
                                           }
                                       }
                                           break;
                                   }
                               }
                           };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.series_list_layout);
        ListView myListView = (ListView) findViewById(android.R.id.list);
        myListView.setFastScrollEnabled(true);

        ServerConnector.registerHttpOverListener(this);

        Global.getAdaptor().openToWrite();
        serieCursor = Global.getAdaptor().getAllSeries();
        startManagingCursor(serieCursor);
        cursorAdapter = new SeriesAdapter(this, serieCursor, false);
        setListAdapter(cursorAdapter);

        // Check user known
        String id = Global.getUserID();
        Log.w(Global.getLogTag(MainActivity.class), "Check user details id="
            + id);
        if (id == null) {
            Log.v(Global.getLogTag(MainActivity.class), "select user");
            Intent intent = new Intent(this, SelectUserActivity.class);
            intent.putExtra(SelectUserActivity.IS_NOT_CANCELLABLE_EXTRA, true);
            startActivityForResult(intent, INTENT_CODE + SELECT_USER);
        }
        else {
            // lancer la synchro en arrière plan
            long diff = Calendar.getInstance().getTimeInMillis()
                - Global.getLastRefreshDate();
            diff = TimeUnit.MILLISECONDS.toDays(diff);
            Log.v(Global.getLogTag(MainActivity.class), "sync series diff="
                + diff);
            if (diff > 1) {
                ServerConnector.syncSeries(handler);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopManagingCursor(serieCursor);
        serieCursor = null;
        Enumeration<Integer> e = listCursorMap.keys();
        while (e.hasMoreElements())
            stopManagingCursor(listCursorMap.get(e.nextElement()));
        e = shoppingCursorMap.keys();
        while (e.hasMoreElements())
            stopManagingCursor(shoppingCursorMap.get(e.nextElement()));
        Global.getAdaptor().close();
        ServerConnector.unregisterHttpOverListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_CODE + SELECT_USER) {
            if (resultCode == RESULT_CANCELED) return;
            if (resultCode == SelectUserActivity.RESULT_QUIT) {
                finish();
                return;
            }
            if (Global.getUsername() == null || Global.getPassword() == null) {
                Log.v(Global.getLogTag(MainActivity.class), "Select_user callback invalid");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Error_Alert_Title);
                builder.setMessage(R.string.Error_Alert_Login_Needed);
                builder.setPositiveButton(R.string.Error_Alert_OK_Btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, SelectUserActivity.class);
                        startActivityForResult(intent, INTENT_CODE
                            + SELECT_USER);
                    }
                });
                builder.setNegativeButton(R.string.Error_Alert_Quit_Btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                });
                builder.show();
            }
            else {
                // Show waiting progressBar et launch first full sync
                waitingDialog = ProgressDialog.show(this, getResources().getString(R.string.Sync_Alert_Title), getResources().getString(R.string.Sync_Alert_Connect), true, false);
                Log.v(Global.getLogTag(MainActivity.class), "Select_user ok get_ID");
                ServerConnector.getId(handler);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isHttpProgress = false;

    @Override
    public void onHttpBegin() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        isHttpProgress = true;
    }

    @Override
    public void onHttpOver() {
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(View.INVISIBLE);
        isHttpProgress = false;

        Global.setLastRefreshDate(Calendar.getInstance().getTimeInMillis());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, MENU_REFRESH, MENU_REFRESH, getResources().getString(R.string.Menu_Refresh));
        item.setIcon(R.drawable.ic_menu_refresh);
        item = menu.add(Menu.NONE, MENU_SHOPPING, MENU_SHOPPING, getResources().getString(R.string.Menu_ShoppingList));
        item.setIcon(R.drawable.ic_menu_shopping);
        item = menu.add(Menu.NONE, MENU_LEGEND, MENU_LEGEND, getResources().getString(R.string.Menu_Legend));
        item.setIcon(R.drawable.ic_menu_info_details);
        item = menu.add(Menu.NONE, MENU_QUIT, MENU_QUIT, getResources().getString(R.string.Menu_Quit));
        item.setIcon(R.drawable.ic_menu_close_clear_cancel);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(MENU_SHOPPING);
        if (!isMissingList) {
            item.setIcon(R.drawable.ic_menu_shopping);
            item.setTitle(R.string.Menu_ShoppingList);
        }
        else {
            item.setIcon(R.drawable.ic_menu_list);
            item.setTitle(R.string.Menu_List);
        }
        item = menu.findItem(MENU_LEGEND);
        item.setVisible(!isMissingList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                // on check qu'on ne soit pas deja en train de rafraichir
                if (isHttpProgress) break;
                // on lance un refresh
                ServerConnector.syncSeries(handler);
                break;
            case MENU_SHOPPING:
                switchStatus();
                break;
            case MENU_QUIT:
                finish();
                break;
            case MENU_LEGEND:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.Legend_Alert_Title);
                builder.setView(getLayoutInflater().inflate(R.layout.legend, null));
                builder.setPositiveButton(getResources().getString(R.string.Error_Alert_OK_Btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //    @Override
    //    public void onClick(View v) {
    //        Log.i(Global.getLogTag(MainActivity.class), "Click on v=" + v);
    //    }

    public void switchStatus() {
        //                Intent intent = new Intent(this, ShoppingActivity.class);
        //                startActivity(intent);
        isMissingList = !isMissingList;

        stopManagingCursor(serieCursor);
        if (isMissingList)
            serieCursor = Global.getAdaptor().getAllMissingSeries();
        else
            serieCursor = Global.getAdaptor().getAllSeries();
        startManagingCursor(serieCursor);

        cursorAdapter = new SeriesAdapter(this, serieCursor, isMissingList);
        setListAdapter(cursorAdapter);

        TextView tv = (TextView) findViewById(R.id.caption);
        if (isMissingList)
            tv.setText(R.string.Missing_Empty);
        else
            tv.setText(R.string.Collection_Empty);

        tv = (TextView) findViewById(R.id.title);
        if (isMissingList)
            tv.setText(R.string.Shopping_Title);
        else
            tv.setText(R.string.Collection_Title);
    }

    @Override
    public void onBackPressed() {
        if (isMissingList) {
            switchStatus();
            return;
        }

        super.onBackPressed();
    }
}
