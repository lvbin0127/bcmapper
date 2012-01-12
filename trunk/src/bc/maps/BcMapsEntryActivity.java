package bc.maps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ZoomControls;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.kml.KmlUrlReader;
import com.nutiteq.log.AndroidLogger;
import com.nutiteq.log.Log;
import com.nutiteq.maps.QKMap;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.wrappers.AppContext;

public class BcMapsEntryActivity extends Activity {
    /** Called when the activity is first created. */
	private BasicMapComponent mapComponent;
	private boolean onRetainCalled;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.enableAll();
        Log.setLogger(new AndroidLogger("Map_Canvas"));
        onRetainCalled = false;
        
        mapComponent = new BasicMapComponent("tutorial", new AppContext(this), 1, 1, new WgsPoint(-81.446, 41.242), 14);
        mapComponent.setMap(new QKMap("Bing","http://ecn.t1.tiles.virtualearth.net/tiles/r",256,0,19,".png?g=461&mkt=en-us&shading=hill&n=z"));
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
		
		//mapComponent.setFileSystem(new AndroidFileSystem());
        //KmlUrlReader DD = new KmlUrlReader("file:///sdcard/kmls/mh.kml", false);
        //mapComponent.addKmlService(DD);
        mapComponent.addKmlService(new KmlUrlReader("http://dl.dropbox.com/u/394378/doc2.kml?LANG=en_US.utf8&", 
        											35,
        											true
        										));
        
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
        	mapComponent.stopMapping();
        	mapComponent = null;
        }
    }
}