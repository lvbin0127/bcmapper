package com.nutiteq.utfgrid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.InflaterInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;

import com.nutiteq.components.MapPos;
import com.nutiteq.components.WgsBoundingBox;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.components.ZoomRange;
import com.nutiteq.db.TileDatabaseHelper;
import com.nutiteq.log.Log;
import com.nutiteq.maps.DbStoredMap;
import com.nutiteq.ui.StringCopyright;

/**
 * Enables to define tile databases in MBTiles.org format (for Android)
 * @author jaak
 *
 */
public class AndroidMbTilesDatabase implements TileDatabaseHelper {

    // according to mbtiles.org format
    private static final String TILE_TABLE = "tiles";
    private static final String GRID_TABLE = "grids";
    private static final String DATA_TABLE = "grid_data";
    
    private static final String KEY_ZOOM = "zoom_level";
    private static final String KEY_X = "tile_column";
    private static final String KEY_Y = "tile_row";
    private static final String KEY_TILE_DATA = "tile_data";
    private static final String KEY_GRID = "grid";
    private static final String KEY_GRID_NAME = "key_name";
    private static final String KEY_GRID_JSON = "key_json";

    private static final String TABLE_WHERE = KEY_ZOOM + " = ? and "+KEY_X+" = ? and "+KEY_Y+" = ?";
	
	private Vector <AndroidTileDatabaseHelper> databases = new Vector<AndroidTileDatabaseHelper>();
    private Vector<String> tables;

	/**
	 * @param ctx Application context
	 * @param dbFile DB file path, e.g. "/sdcard/db1.db"
	 */
	public AndroidMbTilesDatabase(final Context ctx, final String dbFile){
	   this(ctx,new String[]{dbFile});
	}
	
	/**
	 * @param ctx Application context
	 * @param dbFiles Array of DB file paths, e.g. String[]{"/sdcard/db1.db","/sdcard/db2.db"}
	 */
	public AndroidMbTilesDatabase(final Context ctx, final String dbFiles[]){
		for(String dbFile : dbFiles){
			this.databases.add(new AndroidTileDatabaseHelper(ctx, dbFile,
			        TILE_TABLE,GRID_TABLE,DATA_TABLE,KEY_ZOOM,KEY_X,KEY_Y,KEY_TILE_DATA,KEY_GRID,KEY_GRID_NAME,KEY_GRID_JSON,TABLE_WHERE));
		}
	}
	
	public byte[] getTileImage(int zoom, int x, int y) {
		// Return first found tile
	    int yFlip = (1<<(zoom))-1-y; //MBTiles has flipped Y
	 //   Log.debug("unflip y="+y+" flipped="+yFlip);
	    for (AndroidTileDatabaseHelper db: databases){
			//if (db.containsKey(zoom, x, yFlip,TILE_TABLE)){
				return db.getTileImg(zoom, x, yFlip);
			//}
		}
		return null;
	}

	public boolean contains(int zoom, int x, int y) {
		for (AndroidTileDatabaseHelper db: databases){
			if (db.containsKey(zoom, x, y, TILE_TABLE)){
				return true;
			}
		}
		return false;
	}

	public byte[] getTileImage(String resourcePath) {
		int zoom = Integer.parseInt(resourcePath.split("/")[2]);
		int x = Integer.parseInt(resourcePath.split("/")[3]);
		int y = Integer.parseInt(resourcePath.split("/")[4]);
		return getTileImage(zoom,x,y);
	}
	
    public MBTileUTFGrid getUTFGrid(int zoom, int x, int y) {
        int yFlip = (1 << (zoom)) - 1 - y; // MBTiles has flipped Y
        for (AndroidTileDatabaseHelper db : databases) {
                byte[] gridBytes = db.getGrid(zoom, x, yFlip);
                if(gridBytes == null){
                    Log.debug("no grid for "+zoom+"/"+x+"/"+y);
                    return null;
                }
                InflaterInputStream in = new InflaterInputStream(
                        new ByteArrayInputStream(gridBytes));
                ByteArrayOutputStream inflatedOut = new ByteArrayOutputStream();
                int readLength;
                byte[] block = new byte[1024];
                try {
                    while ((readLength = in.read(block)) != -1)
                        inflatedOut.write(block, 0, readLength);
                    inflatedOut.flush();

                    String gridJSON =  new String(inflatedOut.toByteArray());
                    MBTileUTFGrid grid = new MBTileUTFGrid();
                    JSONObject root = new JSONObject(gridJSON);
                    JSONArray gridA = root.getJSONArray("grid");
                    JSONArray keysA = root.getJSONArray("keys");
                    grid.grid = new String[gridA.length()];
                    
                    for(int i=0;i<gridA.length();i++){
                        grid.grid[i]=gridA.getString(i);
                    }
                    grid.keys = new String[keysA.length()];
                    for(int i=0;i<keysA.length();i++){
                        grid.keys[i]=keysA.getString(i);
                    }

                    return grid;
                
                } catch (IOException e) {
                    Log.error("cannot inflate utfgrid data "+e.getMessage());
                    e.printStackTrace();
                    return null;
                } catch (JSONException e) {
                    Log.error("JSON parser exception "+ e.getMessage());
                    e.printStackTrace();
                }
                
        }
        return null;
    }

	public void initialize() {
		Log.debug("init all tile dbs");
		for (AndroidTileDatabaseHelper db: databases){
			db.open();
		}
		tables = getTables();
	}
	
	public WgsBoundingBox getBoundingBox(){
	    double latMin = 90;
        double lonMin = 180;
        double latMax = -90;
        double lonMax = -180;

        DbStoredMap map = new DbStoredMap(new StringCopyright("OpenStreetMap (MBTiles)"));
        
	    for (AndroidTileDatabaseHelper db: databases){
            int[] zooms = db.getZoomRange();
            for(int z=zooms[0];z<zooms[1];z++){
                int[] tileBounds = db.tileBounds(z);
                int tileSize = map.getTileSize();
                double latMi = map.mapPosToWgs(new MapPos(tileBounds[0]*tileSize,flipY(tileBounds[2],z)*tileSize, z)).toWgsPoint().getLat();
                double latMa = map.mapPosToWgs(new MapPos((tileBounds[1]+1)*tileSize,flipY(tileBounds[3]+1,z)*tileSize, z)).toWgsPoint().getLat();
                double lonMi = map.mapPosToWgs(new MapPos(tileBounds[0]*tileSize,flipY(tileBounds[2],z)*tileSize, z)).toWgsPoint().getLon();
                double lonMa = map.mapPosToWgs(new MapPos((tileBounds[1]+1)*tileSize,flipY(tileBounds[3]+1,z)*tileSize, z)).toWgsPoint().getLon();
                
                latMin=Math.min(latMi,latMin);
                latMax=Math.max(latMa,latMax);
                lonMin=Math.min(lonMi,lonMin);
                lonMax=Math.max(lonMa,lonMax);
            }

	    }

	    return new WgsBoundingBox(new WgsPoint(lonMin,latMin),new WgsPoint(lonMax,latMax));
	}

	
	public ZoomRange getZoomRange(){
	    int minZ=Integer.MAX_VALUE;
	    int maxZ=Integer.MIN_VALUE;
        
        for (AndroidTileDatabaseHelper db: databases){
  	      int[] zooms = db.getZoomRange();
  	      if(zooms[0]<minZ){
  	          minZ=zooms[0];
  	      }
  	      if(zooms[1]>maxZ){
  	          maxZ=zooms[1];
          }
	    }
        return new ZoomRange(minZ,maxZ);
        
	}
	
	   public WgsBoundingBox getBoundingBox(int z){
	        double latMin = 90;
	        double lonMin = 180;
	        double latMax = -90;
	        double lonMax = -180;

	        DbStoredMap map = new DbStoredMap(new StringCopyright("OpenStreetMap (MBTiles)"));
	        
	        for (AndroidTileDatabaseHelper db: databases){
	                int[] tileBounds = db.tileBounds(z);
	                int tileSize = map.getTileSize();
	                double latMi = map.mapPosToWgs(new MapPos(tileBounds[0]*tileSize,flipY(tileBounds[2],z)*tileSize, z)).toWgsPoint().getLat();
	                double latMa = map.mapPosToWgs(new MapPos((tileBounds[1]+1)*tileSize,flipY(tileBounds[3]+1,z)*tileSize, z)).toWgsPoint().getLat();
	                double lonMi = map.mapPosToWgs(new MapPos(tileBounds[0]*tileSize,flipY(tileBounds[2],z)*tileSize,z)).toWgsPoint().getLon();
	                double lonMa = map.mapPosToWgs(new MapPos((tileBounds[1]+1)*tileSize,flipY(tileBounds[3]+1,z)*tileSize, z)).toWgsPoint().getLon();

	                latMin=Math.min(latMi,latMin);
	                latMax=Math.max(latMa,latMax);
	                lonMin=Math.min(lonMi,lonMin);
	                lonMax=Math.max(lonMa,lonMax);
		        }

	        return new WgsBoundingBox(new WgsPoint(lonMin,latMin),new WgsPoint(lonMax,latMax));
	    }
	
    private int flipY(int y,int zoom) {
        return (1<<zoom)-1-y;
    }


    /**
     * Close all opened databases
     */
    public void close() {
        for (AndroidTileDatabaseHelper db: databases){
            db.close();
        }
    }

    public String getUTFGridValue(int x, int y, int zoom, String string, int radius) {
        for (AndroidTileDatabaseHelper db: databases){
            Cursor c = db.getGridValue(x, (1 << (zoom)) - 1 - y, zoom, string, radius);
            while (c.moveToNext()){
                return c.getString(c.getColumnIndex(KEY_GRID_JSON));
            }
        }
        return null; // if not found from any of the db-s
    }
    
    public Boolean hasMbTiles(){
        return tables.contains(TILE_TABLE);
    }

    public Boolean hasGrids(){
        return tables.contains(GRID_TABLE);
    }
    
    public HashMap<String, String> getMetadata(int i){
        return databases.get(i).getMetadata();
    }
    
    private Vector<String> getTables() {
        Vector<String> tabs = new Vector<String>();
        
        for (AndroidTileDatabaseHelper db: databases){
            Cursor c = db.getTables();
            
            while (c.moveToNext()){
                tabs.add(c.getString(c.getColumnIndex("name")));
            }
        }
        return tabs;
    }
}
