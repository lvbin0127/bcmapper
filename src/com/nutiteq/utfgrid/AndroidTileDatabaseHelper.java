package com.nutiteq.utfgrid;

import java.util.HashMap;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Universal helper for SQLite tile database.
 * 
 * @author jaak
 */
public class AndroidTileDatabaseHelper {
    private static final int DATABASE_VERSION = 1;
    private final Context ctx;
    private DatabaseHelper databaseHelper;
    private final String databaseName;
    public SQLiteDatabase database;
    private String keyZoom;
    private String keyX;
    private String keyY;
    private String keyData;
    private String tableWhere;
    private String tileTable;
    private String gridTable;
    private String keyGrid;
    private String gridDataTable;
    private String keyGridName;
    private String keyGridJson;

    private static final String LOG_TAG = "TileDatabaseHelper";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(final Context context, final String databaseName) {
            super(context, databaseName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            // to nada, db is expected to exist
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {
            // to nada, db is expected to exist
        }
    }

    /**
     * Construct database helper with own tile table format.
     * 
     * @param ctx
     *            Android application context
     * @param databaseName
     *            Database file path
     * @param tileTable
     *            Tile table name, e.g. "tiles"
     * @param keyZoom
     *            column name for zoom
     * @param keyX
     *            table column name for x/column
     * @param keyY
     *            table column name for y/row
     * @param keyData
     *            column for binary data (blob)
     * @param tableWhere
     *            where SQL clause for tile row search, as prepared statement
     *            e.g. "zoom_level = ? and tile_column = ? and tile_row = ?"
     * @param tableWhere2 
     * @param keyGridJson 
     * @param keyGridName 
     * @param keyGrid 
     * @param keyTileData 
     */
    public AndroidTileDatabaseHelper(final Context ctx, 
            final String databaseName, String tileTable,  String gridTable,  String gridDataTable, String keyZoom,
            String keyX, String keyY, String keyData, String keyGrid, String keyGridName, String keyGridJson, String tableWhere) {
        
        this.ctx = ctx;
        this.databaseName = databaseName;
        this.keyZoom = keyZoom;
        this.keyX = keyX;
        this.keyY = keyY;
        this.keyData = keyData;
        this.tableWhere = tableWhere;
        this.tileTable = tileTable;
        this.gridTable = gridTable;
        this.keyGrid = keyGrid;
        this.gridDataTable = gridDataTable;
        this.keyGridName = keyGridName;
        this.keyGridJson = keyGridJson;
    }

    public void open() {
        Log.d(LOG_TAG,"Opening db "+databaseName);
        databaseHelper = new DatabaseHelper(ctx, databaseName);
        database = databaseHelper.getReadableDatabase();
    }

    public void close() {
        databaseHelper.close();
    }

    public boolean containsKey(final int z, final int x, final int y, final String table) {
        final long start = System.currentTimeMillis();
        final Cursor c = database.query(table, new String[] { keyX },
                tableWhere, new String[] { String.valueOf(z),
                        String.valueOf(x), String.valueOf(y) }, null, null,
                null);
        final boolean hasKey = c.moveToFirst();
        c.close();
        Log.d(LOG_TAG,table+
                " containsKey execution time "
                        + (System.currentTimeMillis() - start));
        return hasKey;
    }

    public byte[] getTileImg(final int z, final int x, final int y) {
        final long start = System.currentTimeMillis();
        final Cursor c = database.query(tileTable, new String[] { keyData },
                tableWhere, new String[] { String.valueOf(z),
                        String.valueOf(x), String.valueOf(y) }, null, null,
                null);
        if (!c.moveToFirst()) {
            Log.d(LOG_TAG,
                    "not found " + String.valueOf(z) + " " + String.valueOf(x)
                            + " " + String.valueOf(y));
            c.close();
            return null;
        }
        final byte[] data = c.getBlob(c.getColumnIndexOrThrow(keyData));
        c.close();
        Log.d(LOG_TAG, "get execution time "
                + (System.currentTimeMillis() - start));
        return data;
    }

    public byte[] getGrid(int zoom, int x, int y) {

        final Cursor c = database.query(gridTable, new String[] { keyGrid },
                tableWhere, new String[] { String.valueOf(zoom),
                        String.valueOf(x), String.valueOf(y) }, null, null,
                null);
        if (!c.moveToFirst()) {
            Log.d(LOG_TAG,
                    "getGrid not found " + String.valueOf(zoom) + " " + String.valueOf(x)
                            + " " + String.valueOf(y));
            c.close();
            return null;
        }
        final byte[] data = c.getBlob(c.getColumnIndexOrThrow(keyGrid));
        c.close();
        return data;
    }

    public int[] getZoomRange() {
        // select zoom_level from tiles order by zoom_level limit 1
        
        final Cursor c = database.rawQuery("select min(zoom_level),max(zoom_level) from tiles",new String[]{});
        if (!c.moveToFirst()) {
            Log.d(LOG_TAG, "zoomrange not found");
            c.close();
            return null;
        }
        int[] zooms = new int[]{c.getInt(0),c.getInt(1)};
        c.close();
        return zooms;
    }

    public int[] tileBounds(int zoom) {
        // select min(tile_column),max(tile_column),min(tile_row),max(tile_row)
        // from tiles where tile_zoom = <zoom>

        final Cursor c = database
                .rawQuery(
                        "select min(tile_column),max(tile_column),min(tile_row),max(tile_row) from tiles where zoom_level = ?",
                        new String[] { String.valueOf(zoom) });
        if (!c.moveToFirst()) {
            Log.d(LOG_TAG, "tileBounds not found");
            c.close();
            return null;
        }
        int[] ret = new int[4];
        ret[0] = c.getInt(0);
        ret[1] = c.getInt(1);
        ret[2] = c.getInt(2);
        ret[3] = c.getInt(3);

        c.close();
        return ret;
    }

    public Cursor getGridValue(int x, int y, int zoom, String key, int radius) {
        if(radius==0){
        return database.query(gridDataTable, new String[] { keyGridJson },
                keyZoom + " = ? and "+keyX+" = ? and "+keyY+" = ?"+" and "+keyGridName+" = ? AND "+keyGridJson+"<>'{\"NAME\":\"\"}'",
                new String[] { String.valueOf(zoom),
                String.valueOf(x), String.valueOf(y), key}, null, null,
                null);
        }else{
            return database.query(gridDataTable, new String[] { keyGridJson },
                    keyZoom + " = ? AND "+keyX+" >= ? AND "+keyX+" <= ? AND "+keyY+" >= ? AND "+keyY+" <= ? AND "+keyGridName+" = ?",
                    new String[] { String.valueOf(zoom),
                    String.valueOf(x-radius),String.valueOf(x+radius), String.valueOf(y-radius), String.valueOf(y+radius), key}, null, null,
                    null);
        }
    }

    public Cursor getTables(){
        return database
        .rawQuery(
                "select name from SQLITE_MASTER where type = 'table' OR type = 'view'",
                new String[] { });
        
    }
    
    public HashMap<String, String> getMetadata() {

        HashMap<String, String> metadata = new HashMap<String, String>();

        final Cursor c = database
                .rawQuery("SELECT name,value FROM metadata",null);
        while(c.moveToNext()){
            metadata.put(c.getString(0), c.getString(1));
        } while (c.moveToNext());
        
        c.close();
        return metadata;
    }




}
