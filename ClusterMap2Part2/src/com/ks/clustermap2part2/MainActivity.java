package com.ks.clustermap2part2;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.ks.clustermap2part2.RandomPointsProvider.GenerateCallback;
import com.ks.clustermap2part2.RandomPointsProvider.GeographicDistribution;
import com.twotoasters.clusterkraf.Clusterkraf;
import com.twotoasters.clusterkraf.Clusterkraf.ProcessingListener;
import com.twotoasters.clusterkraf.InputPoint;
import com.twotoasters.clusterkraf.Options.ClusterClickBehavior;
import com.twotoasters.clusterkraf.Options.ClusterInfoWindowClickBehavior;
import com.twotoasters.clusterkraf.Options.SinglePointClickBehavior;

public class MainActivity extends FragmentActivity implements GenerateCallback,
		ProcessingListener {

	private static final String KEY_CAMERA_POSITION = "camera position";
	private GoogleMap map;
	private CameraPosition restoreCameraPosition;
	private Clusterkraf clusterkraf;
	private Options options;

	ArrayList<InputPoint> inputPoints;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.activity_main);

		if (this.options == null) {
			this.options = new Options();
		}

		RandomPointsProvider rpp = RandomPointsProvider.getInstance();

		if (savedInstanceState != null && rpp.hasPoints()) {
			this.restoreCameraPosition = savedInstanceState
					.getParcelable(KEY_CAMERA_POSITION);
			this.inputPoints = rpp.getPoints();
		} else {
			setProgressBarIndeterminateVisibility(true);

			rpp.generate(this, options.geographicDistribution,
					options.pointCount);
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (map != null) {
			CameraPosition cameraPosition = map.getCameraPosition();
			if (cameraPosition != null) {
				outState.putParcelable(KEY_CAMERA_POSITION, cameraPosition);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		initMap();
	}

	@Override
	protected void onPause() {
		super.onPause();
		/**
		 * When pausing, we clear all of the clusterkraf's markers in order to
		 * conserve memory. When (if) we resume, we can rebuild from where we
		 * left off.
		 */
		if (clusterkraf != null) {
			clusterkraf.clear();
			clusterkraf = null;
			if (map != null) {
				restoreCameraPosition = map.getCameraPosition();
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void initMap() {
		if (map == null) {
			SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			if (mapFragment != null) {
				map = mapFragment.getMap();
				if (map != null) {
					UiSettings uiSettings = map.getUiSettings();
					uiSettings.setAllGesturesEnabled(false);
					uiSettings.setScrollGesturesEnabled(true);
					uiSettings.setZoomGesturesEnabled(true);
					map.setOnCameraChangeListener(new OnCameraChangeListener() {
						@Override
						public void onCameraChange(CameraPosition arg0) {
							moveMapCameraToBoundsAndInitClusterkraf();
						}
					});
				}
			}
		} else {
			moveMapCameraToBoundsAndInitClusterkraf();
		}
	}

	private void moveMapCameraToBoundsAndInitClusterkraf() {
		if (map != null && options != null && inputPoints != null) {
			try {
				if (restoreCameraPosition != null) {
					/**
					 * if a restoreCameraPosition is available, move the camera
					 * there
					 */
					map.moveCamera(CameraUpdateFactory
							.newCameraPosition(restoreCameraPosition));
					restoreCameraPosition = null;
				}
				initClusterkraf();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	private void initClusterkraf() {
		if (map != null && inputPoints != null && inputPoints.size() > 0) {
			com.twotoasters.clusterkraf.Options options = new com.twotoasters.clusterkraf.Options();
			applyDemoOptionsToClusterkrafOptions(options);
			clusterkraf = new Clusterkraf(map, options, inputPoints);
		}
	}

	private void applyDemoOptionsToClusterkrafOptions(
			com.twotoasters.clusterkraf.Options options) {
		options.setTransitionDuration(this.options.transitionDuration);
		options.setTransitionInterpolator(new DecelerateInterpolator());

		/**
		 * Clusterkraf calculates whether InputPoint objects should join a
		 * cluster based on their pixel proximity. If you want to offer your app
		 * on devices with different screen densities, you should identify a
		 * Device Independent Pixel measurement and convert it to pixels based
		 * on the device's screen density at runtime.
		 */
		options.setPixelDistanceToJoinCluster(getPixelDistanceToJoinCluster());

		options.setZoomToBoundsAnimationDuration(this.options.zoomToBoundsAnimationDuration);
		options.setShowInfoWindowAnimationDuration(this.options.showInfoWindowAnimationDuration);
		options.setExpandBoundsFactor(this.options.expandBoundsFactor);
		options.setSinglePointClickBehavior(this.options.singlePointClickBehavior);
		options.setClusterClickBehavior(this.options.clusterClickBehavior);
		options.setClusterInfoWindowClickBehavior(this.options.clusterInfoWindowClickBehavior);

		/**
		 * Device Independent Pixel measurement should be converted to pixels
		 * here too. In this case, we cheat a little by using a Drawable's
		 * height. It's only cheating because we don't offer a variant for that
		 * Drawable for every density (xxhdpi, tvdpi, others?).
		 */
		options.setZoomToBoundsPadding(getResources().getDrawable(
				R.drawable.ic_map_pin_cluster).getIntrinsicHeight());

		options.setMarkerOptionsChooser(new ToastedMarkerOptionsChooser(this,
				inputPoints.get(0)));
		 options.setProcessingListener(this);
	}

	private int getPixelDistanceToJoinCluster() {
		return convertDeviceIndependentPixelsToPixels(this.options.dipDistanceToJoinCluster);
	}

	private int convertDeviceIndependentPixelsToPixels(int dip) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		return Math.round(displayMetrics.density * dip);
	}

	static class Options implements Serializable {

		private static final long serialVersionUID = 7492713360265465944L;

		// sample app-specific options
		int pointCount = 100;
		GeographicDistribution geographicDistribution = GeographicDistribution.Worldwide;

		// clusterkraf library options
		int transitionDuration = 500;
		String transitionInterpolator = LinearInterpolator.class
				.getCanonicalName();
		int dipDistanceToJoinCluster = 100;
		int zoomToBoundsAnimationDuration = 500;
		int showInfoWindowAnimationDuration = 500;
		double expandBoundsFactor = 0.5d;
		SinglePointClickBehavior singlePointClickBehavior = SinglePointClickBehavior.SHOW_INFO_WINDOW;
		ClusterClickBehavior clusterClickBehavior = ClusterClickBehavior.SHOW_INFO_WINDOW;
		ClusterInfoWindowClickBehavior clusterInfoWindowClickBehavior = ClusterInfoWindowClickBehavior.ZOOM_TO_BOUNDS;
	}

	@Override
	public void onRandomMarkersGenerated(ArrayList<InputPoint> inputPoints) {
		this.inputPoints = inputPoints;
		initMap();

	}

	@Override
	public void onClusteringStarted() {
	}

	@Override
	public void onClusteringFinished() {
		setProgressBarIndeterminateVisibility(false);
	}
}
