
/*
bcMapper is a mapping application for displaying raster and vector data on the Android platform.


Copyright (C) Brown and Caldwell, 2012

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

For more information, contact:

    Chris Somerlot
    Brown and Caldwell
    290 Elwood Davis Rd, Suite 290
    Liverpool, NY 13088
    
*/

package bc.maps;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.cache.AndroidFileSystemCache;
import com.nutiteq.cache.Cache;
import com.nutiteq.cache.CachingChain;
import com.nutiteq.cache.MemoryCache;
import com.nutiteq.components.Place;
import com.nutiteq.components.Route;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.controls.AndroidKeysHandler;
import com.nutiteq.fs.AndroidFileSystem;
import com.nutiteq.kml.KmlUrlReader;
import com.nutiteq.location.NutiteqLocationMarker;
import com.nutiteq.log.AndroidLogger;
import com.nutiteq.log.Log;
import com.nutiteq.maps.DbStoredMap;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.maps.QKMap;
import com.nutiteq.maps.SimpleWMSMap;
import com.nutiteq.net.DefaultDownloadStreamOpener;
import com.nutiteq.net.NutiteqDownloadCounter;
import com.nutiteq.ui.DefaultZoomIndicator;
import com.nutiteq.ui.NutiteqDownloadDisplay;
import com.nutiteq.ui.SimpleScaleBar;
import com.nutiteq.ui.StringCopyright;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.utfgrid.AndroidMbTilesDatabase;
import com.nutiteq.wrappers.AppContext;

public class BcMapsEntryActivity extends Activity {
    /** Called when the activity is first created. */
	private BasicMapComponent _mapComponent;
	private NutiteqLocationMarker _gpsMarker;
	private boolean _onRetainCalled;
	
	private Route _route;
	private Place[] _instructionPlaces;
	private NutiteqLocationMarker _z;
    public AndroidMbTilesDatabase _tileDb;
    public Place _gridLabelPlace;
    
	final int KML_DIALOG = 1293713;
	final int BASE_DIALOG = 1299810;
	final int WMS_PATH_DIALOG = 19398374;
	final int CLOUDMADE_KEY_DIALOG = 1899473;
	final int LOCALTMCACHE_PATH_DIALOG = 9162383;
	final int LOCALAGSCACHE_PATH_DIALOG = 1348901;
	
	final int BASE_BING = 0;
	final int BASE_AERIAL = 1;
	final int BASE_OSM = 2;
	final int BASE_WMS = 3;
	final int BASE_CLOUDMADE = 4;
	final int BASE_MBTLOCALCACHE = 5;
	final int BASE_MGMLOCALCACHE = 6;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.enableAll();
        Log.setLogger(new AndroidLogger("Map_Canvas"));
        _onRetainCalled = false;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        Float x =  prefs.getFloat("originX", -76.17813F);
        Float y = prefs.getFloat("originY", 43.09899F);
        WgsPoint origin = new WgsPoint(x, y);
        
        String key = "a5bfc9e07964f8dddeb95fc584cd965d4f183a8b7736d4.37579663";
        Integer zoom = prefs.getInt("zoom", 6);
        _mapComponent =  new BasicMapComponent(key, new AppContext(this), 1, 1, origin, zoom);
        _mapComponent.setFileSystem(new AndroidFileSystem());
        
        
        final MemoryCache memoryCache = new MemoryCache(10 * 1024 * 1024);
        final File cacheDir = getDir("bcMapCache", Context.MODE_PRIVATE);
        if (!cacheDir.exists()) cacheDir.mkdir();
        
        final AndroidFileSystemCache fileSystemCache = new AndroidFileSystemCache(
                  this, "network_cache", cacheDir, 1024 * 1024 * 128);
        _mapComponent.setNetworkCache(new CachingChain(new Cache[] {
                  memoryCache, fileSystemCache }));
        //*/
        
        // Akron
        /*_mapComponent =  new BasicMapComponent(key, new AppContext(this), 1, 1, new WgsPoint(-81.52540F, 41.07783F), 18);
        _mapComponent.addKmlService(new KmlUrlReader("http://dl.dropbox.com/u/394378/Android/AkronCollectionSystem.kml?LANG=en_US.utf8&", 200, true ));
        //*/
        
        // set download timeout to 1 second only (default is 20 sec)
        _mapComponent.setDownloadStreamOpener(new DefaultDownloadStreamOpener(100));
        
        // key controls
        _mapComponent.setControlKeysHandler(new AndroidKeysHandler());
        _mapComponent.setPanningStrategy(new ThreadDrivenPanning());
        _mapComponent.setControlKeysHandler(new AndroidKeysHandler());
        
        // download data counter display, with bundled implementations
        _mapComponent.setDownloadCounter(new NutiteqDownloadCounter());
        _mapComponent.setDownloadDisplay(new NutiteqDownloadDisplay());
        
        // needed if stored maps are used
        _mapComponent.setFileSystem(new AndroidFileSystem()); 
        _mapComponent.setZoomLevelIndicator(new DefaultZoomIndicator(0,21));

        // touch-specific settings
        _mapComponent.setTouchClickTolerance(80); // in pixels
        _mapComponent.setSmoothZoom(true);
        _mapComponent.setDoubleClickZoomIn(true);
        _mapComponent.setDualClickZoomOut(true);
        _mapComponent.setShowOverlaysWhileZooming(false);

        // scalebar with customized location
        SimpleScaleBar scaleBar = new SimpleScaleBar();
        scaleBar.setAlignment(SimpleScaleBar.BOTTOM_LEFT);
        scaleBar.setOffset(20, 40); // default is (20,20)
        _mapComponent.setScaleBar(scaleBar);
        
        // get the mapview that was defined in main.xml
        MapView mapView = (MapView)findViewById(R.id.mapview);
        
        // mapview requires a mapcomponent
        mapView.setMapComponent(_mapComponent);
        
		ZoomControls zoomControls = (ZoomControls)findViewById(R.id.zoomcontrols);
		// set zoomcontrols listeners to enable zooming
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				_mapComponent.zoomIn();
				}
			});
			zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				_mapComponent.zoomOut();
			}
		});
		
		Integer baseMap = prefs.getInt("baseMap", 0);
        setBaseMap(0);
		
		// start mapping processes - mandatory
        _mapComponent.startMapping();
        
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	      _onRetainCalled = true;
	      return _mapComponent;
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
        if (!_onRetainCalled) {
        	WgsPoint origin = _mapComponent.getCenterPoint();
        	
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        	SharedPreferences.Editor editor = prefs.edit();
        	editor.putFloat("originX", ((float) origin.getLon()));
        	editor.putFloat("originY", ((float)origin.getLat()));
        	editor.putInt("zoom", _mapComponent.getZoom());
        	editor.commit();
        	
        	if(_mapComponent.getMap() instanceof DbStoredMap && _tileDb != null){
                _tileDb.close();
            }
        	_mapComponent.stopMapping();
        	_mapComponent = null;
        }
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.appglobalmenu, menu);
	    return true;
	}
    
    protected Dialog onCreateDialog(int id) {
    	AlertDialog dialog;
    	LayoutInflater factory;
        switch(id) {
	        case BASE_DIALOG:
	        	final CharSequence[] items = {"Bing Street", 
	        								  "Bing Aerials", 
	        								  "OpenStreetMaps", 
	        								  "WMS Server", 
	        								  "CloudMade", 
	        								  "Local TileMill Cache",
	        								  "Local ArcGIS Cache"};
	
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	builder.setTitle("Pick a Base Map Source");
	        	builder.setItems(items, new DialogInterface.OnClickListener() {
	        	    public void onClick(DialogInterface dialog, int item) {
	        	        setBaseMap(item);
	        	    }
	        	});
	        	dialog = builder.create();
	            break;
	        
	        case KML_DIALOG:
	        	dialog = new AlertDialog.Builder(this).create();
            	factory = LayoutInflater.from(this);

            	dialog.setTitle("Select the local KML file");
            	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					public void onCancel(DialogInterface arg0) {
						arg0.cancel();
					}
		        });	
            	dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	Intent next = new Intent("org.openintents.action.PICK_FILE");
			        	startActivityForResult(next, KML_DIALOG);
		            }
		        });
            	break;
	        case WMS_PATH_DIALOG:
	        	dialog = new AlertDialog.Builder(this).create();
            	factory = LayoutInflater.from(this);
            	final View wmsTextEntryView = factory.inflate(R.layout.kml_input, null);
            	
            	dialog.setView(wmsTextEntryView);
            	dialog.setTitle("Enter the WMS URL");
            	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					public void onCancel(DialogInterface arg0) {
						arg0.cancel();
					}
		        });	
            	dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	EditText pathText = (EditText)((AlertDialog)dialog).findViewById(R.id.wmsPathEditText);
		            	String wmsURI = pathText.getText().toString();
		            	
		            	EditText layersText = (EditText)((AlertDialog)dialog).findViewById(R.id.wmsPathEditText);
		            	String wmsLayers = layersText.getText().toString();
		            	if (wmsURI.equals("") || wmsLayers.equals(""))
		            		dialog.cancel();
		            	else {
		            		SimpleWMSMap wms = new SimpleWMSMap(
		     	 				   wmsURI,
		     	 				   256, 0, 18,
		     	 				   wmsLayers, 
		     	 				   "image/jpeg",
		     	 				   "default", "GetMap", "© UCL");
	     	 				wms.setWidthHeightRatio(2.0);
	     	 				_mapComponent.setMap(wms);
		            	}
		            }
		        });
	        	
	 			break;
	        case LOCALTMCACHE_PATH_DIALOG:
	        	dialog = new AlertDialog.Builder(this).create();
            	factory = LayoutInflater.from(this);

            	dialog.setTitle("Select the local mbtile cache");
            	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					public void onCancel(DialogInterface arg0) {
						arg0.cancel();
					}
		        });	
            	dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	Intent next = new Intent("org.openintents.action.PICK_FILE");
			        	startActivityForResult(next, LOCALTMCACHE_PATH_DIALOG);
		            }
		        });
	        	
	        	break;
	        default:
	        	goAway();
	            dialog = null;
        }
        return dialog;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
        if (resultCode != Activity.RESULT_CANCELED) {
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        	SharedPreferences.Editor editor = prefs.edit();
        	String path = null;
        	if (requestCode == LOCALTMCACHE_PATH_DIALOG){
        		path = data.getData().toString().replace("file://", "");
        		setMbTileDb(path);
        		editor.putString("baseMapPath", path);
        	}
        	else if (requestCode == KML_DIALOG){
        		path = data.getData().toString();
        		setKMLOverlay(path);
        		editor.putString("kmlPath", path.replace("%20", " "));
        	}
        	else
        		goAway();
        	
        	editor.commit();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.kmlOverlayOption:
        	showDialog(KML_DIALOG);
            return true;
        case R.id.baseMapOption:
        	showDialog(BASE_DIALOG);
            return true;
        default:
        	goAway();
        	return true;
        }
    }
    
    public void goAway() {
    	Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
    }
    
    public void setKMLOverlay(String path) {
    	if (path != null) {
	    	
	    	// Examples
	        //KmlUrlReader reader = new KmlUrlReader("file:///sdcard/kmls/mh.kml", false);
	    	//KmlUrlReader reader = new KmlUrlReader("http://dl.dropbox.com/u/394378/Akron.kml?LANG=en_US.utf8&", 50, true );
	    	try {
		    	//KmlUrlReader reader = new KmlUrlReader(path + "?LANG=en_US.utf8&", 100, true);
	    		KmlUrlReader reader = new KmlUrlReader(path, 1000, false);
		        _mapComponent.addKmlService(reader);
		        
		        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		    	SharedPreferences.Editor editor = prefs.edit();
		    	editor.putString("kml", path);
		    	editor.commit();
	    	}
	    	catch (Exception e) {
	    		Toast.makeText(getApplicationContext(), "Invalid KML", Toast.LENGTH_SHORT).show();
	    	}
    	}
    }
    
    public void setBaseMap(int mapid) {
    	switch (mapid) {
    		default:
    			goAway();
    			break;
    		case BASE_BING:
    			_mapComponent.setMap(new QKMap("Bing","http://ecn.t1.tiles.virtualearth.net/tiles/r",256,0,19,".png?g=461&mkt=en-us&shading=hill&n=z"));
    			break;
    		case BASE_AERIAL:
    			_mapComponent.setMap(new QKMap("Microsoft",
    					  "http://ecn.t2.tiles.virtualearth.net/tiles/h", 256,
    					  1,17, ".jpeg?g=321&mkt=en-us"));
    			break;
    		case BASE_OSM:
    			_mapComponent.setMap(OpenStreetMap.MAPNIK);
    			break;
    		case BASE_WMS:
    			showDialog(WMS_PATH_DIALOG);
				break;
    		case BASE_MBTLOCALCACHE:
    			showDialog(LOCALTMCACHE_PATH_DIALOG);
				break;
    		/*case BASE_MGMLOCALCACHE:
    			showDialog(LOCALAGSCACHE_PATH_DIALOG);
				break;
			*/
    	}
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt("baseMap", mapid);
    	editor.commit();
    }
    
    public BasicMapComponent getMapComponent() {
		return _mapComponent;
	}

	public void setMapComponent(BasicMapComponent theMap) {
		this._mapComponent = theMap;
	}
	
	public Route getRoute() {
		return _route;
	}
	
	public void setRoute(Route _route) {
		this._route = _route;
	}
	
	public void setInstrutionPlaces(Place[] _instructionPlaces) {
		this._instructionPlaces = _instructionPlaces;
	}
	
	public Place getInstructionPlace(int position) {
		return this._instructionPlaces[position];
	}
	
	public NutiteqLocationMarker getGpsMarker() {
		return _gpsMarker;
	}
	
	public void setGpsMarker(NutiteqLocationMarker _gpsMarker) {
		this._gpsMarker = _gpsMarker;
	}
	
	private void setMbTileDb(String path) {
		// MGM tile cache
		//_mapComponent.setMap(new StoredMap("YourMap", path, true));
		
		// MBT tile cache
		_tileDb = new AndroidMbTilesDatabase(this,new String[]{path});
        _tileDb.initialize();
        
        _mapComponent.setTileDatabaseSystem(_tileDb);
        DbStoredMap dbstore = new DbStoredMap(new StringCopyright(""));
        dbstore.setMaxZoom(20);
        _mapComponent.setMap(dbstore);
	}
}