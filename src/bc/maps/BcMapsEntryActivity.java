
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import com.nutiteq.components.WgsPoint;
import com.nutiteq.fs.AndroidFileSystem;
import com.nutiteq.kml.KmlUrlReader;
import com.nutiteq.log.AndroidLogger;
import com.nutiteq.log.Log;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.maps.QKMap;
import com.nutiteq.maps.SimpleWMSMap;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.wrappers.AppContext;

public class BcMapsEntryActivity extends Activity {
    /** Called when the activity is first created. */
	private BasicMapComponent mapComponent;
	private boolean onRetainCalled;
	final int KML_DIALOG = 1293713;
	final int BASE_DIALOG = 1299810;
	final int BASE_BING = 0;
	final int BASE_AERIAL = 1;
	final int BASE_OSM = 2;
	final int BASE_WMS = 3;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.enableAll();
        Log.setLogger(new AndroidLogger("Map_Canvas"));
        onRetainCalled = false;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        WgsPoint origin = new WgsPoint(prefs.getFloat("originX", -76.17813F), prefs.getFloat("originY", 43.09899F));
        mapComponent = new BasicMapComponent("tutorial", new AppContext(this), 1, 1, origin, prefs.getInt("zoom", 6));
        mapComponent.setFileSystem(new AndroidFileSystem());
        
        setBaseMap(prefs.getInt("baseMap", 0));
        setKMLOverlay(prefs.getString("kml", null));
        
        mapComponent.setPanningStrategy(new ThreadDrivenPanning());
        mapComponent.startMapping();
        
        // get the mapview that was defined in main.xml
        MapView mapView = (MapView)findViewById(R.id.mapview);
        
        // mapview requires a mapcomponent
        mapView.setMapComponent(mapComponent);
        
		ZoomControls zoomControls = (ZoomControls)findViewById(R.id.zoomcontrols);
		// set zoomcontrols listeners to enable zooming
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapComponent.zoomIn();
				}
			});
			zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapComponent.zoomOut();
			}
		});
		
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	      onRetainCalled = true;
	      return mapComponent;
	}
	
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
        if (!onRetainCalled) {
        	WgsPoint origin = mapComponent.getCenterPoint();
        	
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        	SharedPreferences.Editor editor = prefs.edit();
        	editor.putFloat("originX", ((float) origin.getLon()));
        	editor.putFloat("originY", ((float)origin.getLat()));
        	editor.putInt("zoom", mapComponent.getZoom());
        	editor.commit();
        	
        	mapComponent.stopMapping();
        	mapComponent = null;
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
        switch(id) {
	        case BASE_DIALOG:
	        	final CharSequence[] items = {"Bing Street", "Bing Aerials", "OpenStreetMaps", "WMS Server", "CloudMade", "Local Tile Cache"};
	
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
            	LayoutInflater factory = LayoutInflater.from(this);
            	final View textEntryView = factory.inflate(R.layout.kml_input, null);
            	
            	dialog.setView(textEntryView);
            	dialog.setTitle("Enter a path or URL to the KML data");
            	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					public void onCancel(DialogInterface arg0) {
						arg0.cancel();
					}
		        });	
            	dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		            	EditText text = (EditText)((AlertDialog)dialog).findViewById(R.id.kmlPathEditText);
		            	String kmlURI = text.getText().toString();
		            	if (kmlURI.equals("") || kmlURI.equals(null))
		            		dialog.cancel();
		            	else
		            		setKMLOverlay(kmlURI);
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
	    	//KmlUrlReader reader = new KmlUrlReader("http://dl.dropbox.com/u/394378/doc2.kml?LANG=en_US.utf8&", 35, true )
	    	try {
		    	KmlUrlReader reader = new KmlUrlReader(path + "?LANG=en_US.utf8&", 100, true);
		        mapComponent.addKmlService(reader);
		        
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
    			mapComponent.setMap(new QKMap("Bing","http://ecn.t1.tiles.virtualearth.net/tiles/r",256,0,19,".png?g=461&mkt=en-us&shading=hill&n=z"));
    			break;
    		case BASE_AERIAL:
    			mapComponent.setMap(new QKMap("Microsoft",
    					  "http://ecn.t2.tiles.virtualearth.net/tiles/h", 256,
    					  1,17, ".jpeg?g=321&mkt=en-us"));
    			break;
    		case BASE_OSM:
    			mapComponent.setMap(OpenStreetMap.MAPNIK);
    			break;
    		/*case BASE_WMS:
    			SimpleWMSMap wms = new SimpleWMSMap(
				   "http://iceds.ge.ucl.ac.uk/cgi-bin/icedswms?VERSION=1.1.1&SRS=EPSG:4326",
				   256, 0, 18,"bluemarble,cities,countries", "image/jpeg",
				   "default", "GetMap", "© UCL");
				wms.setWidthHeightRatio(2.0);
				mapComponent.setMap(wms);
				break*/
    	}
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt("baseMap", mapid);
    	editor.commit();
    }
}