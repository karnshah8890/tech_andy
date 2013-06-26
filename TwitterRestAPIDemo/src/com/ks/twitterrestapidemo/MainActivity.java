package com.ks.twitterrestapidemo;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	private final String TAG = getClass().getSimpleName();

	private static String ACCESS_KEY = "250099150-XYN3XM0hKJGhJM1AOTvYesoYRi5WJTyHkMoSRIbS";
	private static String ACCESS_SECRET = "ssC9Cn4fbwKNcpgWmU8Mx4TzvOjQ4Tz25qdov00avk";
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.list);
		new FollowerList().execute(); // assuming that internet is available
	}

	/**
	 * 
	 * AsynTask used for getting list of followers.
	 * 
	 */
	private class FollowerList extends
			AsyncTask<String, Void, ArrayList<String>> {

		private ProgressDialog mProgressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (MainActivity.this != null) {
				mProgressDialog = ProgressDialog.show(MainActivity.this, "",
						"Loading...", true, true);
			}
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
		}

		@Override
		protected ArrayList<String> doInBackground(String... params) {

			try {
				return new WSTwitterTrends().getTwitterFollowers(ACCESS_KEY,
						ACCESS_SECRET);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			super.onPostExecute(result);
			if (result != null) {
				listView.setAdapter(new ArrayAdapter<String>(
						getApplicationContext(),
						android.R.layout.simple_list_item_1, result));
			}
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
		}

	}

}
