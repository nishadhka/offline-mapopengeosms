/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package com.mapapp.main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.KeyEvent;

import com.mapapp.helpers.PointD;
import com.mapapp.mapapp.R;
import com.mapapp.tileManagement.TilesProvider;
import com.mapapp.views.MapView;
import com.mapapp.views.MapViewLocationListener;

public class MapAppActivity extends Activity
{

	// Constant strings used in onSaveInstanceState, onRestoreInstanceState
	private final class Save
	{
		public final static String GPS_LON = "gpsLon";
		public final static String GPS_LAT = "gpsLAT";
		public final static String GPS_ALT = "gpsALT";
		public final static String GPS_ACC = "gpsAcc";
	}

	// Constant strings to save settings in SharedPreferences
	// Also used for restoring settings
	private final class Pref
	{
		public final static String SEEK_LON = "seek_lon";
		public final static String SEEK_LAT = "seek_lat";
		public final static String ZOOM = "zoom";
	}

	// Our only view, created in code
	MapView mapView;

	// Provides us with Tiles objects, passed to MapView
	TilesProvider tilesProvider;

	// Updates marker location in MapView
	MapViewLocationListener locationListener;

	Location savedGpsLocation;

	@Override
	protected void onResume()
	{
		// Create MapView
		initViews();

		// Restore zoom and location data for the MapView
		restoreMapViewSettings();

		// Creating and registering the location listener
		locationListener = new MapViewLocationListener(mapView);
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		// Set our MapView as the main view for the activity
		setContentView(mapView);

		// Never ever forget this :)
		super.onResume();
	}

	void initViews()
	{
		// Creating the bitmap of the marker from the resources
		Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.marker);

		// Creating our database tilesProvider to pass it to our MapView
		String path = Environment.getExternalStorageDirectory() + "/mapapp/world.sqlitedb";
		tilesProvider = new TilesProvider(path);

		// Creating the mapView and make sure it fills the screen
		Display display = getWindowManager().getDefaultDisplay();
		mapView = new MapView(this, display.getWidth(), display.getHeight(), tilesProvider, marker);

		// If a location was saved while pausing the app then use it.
		if (savedGpsLocation != null) mapView.setGpsLocation(savedGpsLocation);

		// Update and draw the map view
		mapView.refresh();
	}

	@Override
	protected void onPause()
	{
		// Save settings before leaving
		saveMapViewSettings();

		// Mainly releases the MapView pointer inside the listener
		locationListener.stop();

		// Unregistering our listener
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);

		// Closes the source of the tiles (Database in our case)
		tilesProvider.close();
		// Clears the tiles held in the tilesProvider
		tilesProvider.clear();

		// Release mapView pointer
		mapView = null;

		super.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// Zooming
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_Z)
		{
			mapView.zoomIn();
			return true;
		}
		// Zooming
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_X)
		{
			mapView.zoomOut();
			return true;
		}
		// Enable auto follow
		if (keyCode == KeyEvent.KEYCODE_H || keyCode == KeyEvent.KEYCODE_FOCUS)
		{
			mapView.followMarker();
			return true;
		}
		// Simulate being at some location, for testing only
		else if (keyCode == KeyEvent.KEYCODE_M || keyCode == KeyEvent.KEYCODE_MENU)
		{
			mapView.setGpsLocation(76.786977056886, 11.092323819616, 0, 182);
			mapView.invalidate();

			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	// Called manually to restore settings from SharedPreferences
	void restoreMapViewSettings()
	{
		SharedPreferences pref = getSharedPreferences("View_Settings", MODE_PRIVATE);

		double lon, lat;
		int zoom;

		lon = Double.parseDouble(pref.getString(Pref.SEEK_LON, "0"));
		lat = Double.parseDouble(pref.getString(Pref.SEEK_LAT, "0"));
		zoom = pref.getInt(Pref.ZOOM, 0);

		mapView.setSeekLocation(lon, lat);
		mapView.setZoom(zoom);
		mapView.refresh();
	}

	// Called manually to save settings in SharedPreferences
	void saveMapViewSettings()
	{
		SharedPreferences.Editor editor = getSharedPreferences("View_Settings", MODE_PRIVATE).edit();

		PointD seekLocation = mapView.getSeekLocation();
		editor.putString(Pref.SEEK_LON, Double.toString(seekLocation.x));
		editor.putString(Pref.SEEK_LAT, Double.toString(seekLocation.y));
		editor.putInt(Pref.ZOOM, mapView.getZoom());

		editor.commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (mapView.getGpsLocation() != null)
		{
			outState.putDouble(Save.GPS_LON, mapView.getGpsLocation().getLongitude());
			outState.putDouble(Save.GPS_LAT, mapView.getGpsLocation().getLatitude());
			outState.putDouble(Save.GPS_ALT, mapView.getGpsLocation().getAltitude());
			outState.putFloat(Save.GPS_ACC, mapView.getGpsLocation().getAccuracy());
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		double gpsLon, gpsLat, gpsAlt;
		float gpsAcc;

		gpsLon = savedInstanceState.getDouble(Save.GPS_LON, 999);
		gpsLat = savedInstanceState.getDouble(Save.GPS_LAT, 999);
		gpsAlt = savedInstanceState.getDouble(Save.GPS_ALT, 999);
		gpsAcc = savedInstanceState.getFloat(Save.GPS_ACC, 999);

		if (gpsLon != 999 && gpsLat != 999 && gpsAlt != 999 && gpsAcc != 999)
		{
			savedGpsLocation = new Location(LocationManager.GPS_PROVIDER);
			savedGpsLocation.setLongitude(gpsLon);
			savedGpsLocation.setLatitude(gpsLat);
			savedGpsLocation.setAltitude(gpsAlt);
			savedGpsLocation.setAccuracy(gpsAcc);
		}

		super.onRestoreInstanceState(savedInstanceState);
	}
}