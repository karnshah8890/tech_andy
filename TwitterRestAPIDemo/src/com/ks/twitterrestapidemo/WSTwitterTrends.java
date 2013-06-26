package com.ks.twitterrestapidemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.R.array;
import android.util.Base64;
import android.util.Log;

public class WSTwitterTrends {

	private final String TAG = getClass().getSimpleName();

	public ArrayList<String> getTwitterFollowers(String accessToken, String accessSecret)
			throws Exception {

		String method = "GET";
		// String url = "https://api.twitter.com/1.1/trends/available.json";
		String url = "https://api.twitter.com/1.1/followers/list.json";
		List<NameValuePair> urlParams = new ArrayList<NameValuePair>();
		// urlParams.add( new BasicNameValuePair("screen_name","twitterapi") );
		// urlParams.add( new BasicNameValuePair("count", "10") );

		String oAuthConsumerKey = "vhyFNlE1G0EArt2h9RdaJA";
		String oAuthConsumerSecret = "zyzmCh9J4IgSf5bHXjHriKbfvOVk5HrdnDwb1f4"; // <---
																				// DO
																				// NOT
																				// SHARE
																				// THIS
																				// VALUE

		String oAuthAccessToken = accessToken;
		String oAuthAccessTokenSecret = accessSecret; // <--DO NOT SHARE THIS
														// VALUE

		String oAuthNonce = String.valueOf(System.currentTimeMillis());
		String oAuthSignatureMethod = "HMAC-SHA1";
		String oAuthTimestamp = time();
		String oAuthVersion = "1.0";

		String signatureBaseString1 = method;
		String signatureBaseString2 = url;
		List<NameValuePair> allParams = new ArrayList<NameValuePair>();
		allParams.add(new BasicNameValuePair("oauth_consumer_key",
				oAuthConsumerKey));
		allParams.add(new BasicNameValuePair("oauth_nonce", oAuthNonce));
		allParams.add(new BasicNameValuePair("oauth_signature_method",
				oAuthSignatureMethod));
		allParams
				.add(new BasicNameValuePair("oauth_timestamp", oAuthTimestamp));
		allParams.add(new BasicNameValuePair("oauth_token", oAuthAccessToken));
		allParams.add(new BasicNameValuePair("oauth_version", oAuthVersion));
		allParams.addAll(urlParams);

		Collections.sort(allParams, new NvpComparator());

		StringBuffer signatureBaseString3 = new StringBuffer();
		for (int i = 0; i < allParams.size(); i++) {
			NameValuePair nvp = allParams.get(i);
			if (i > 0) {
				signatureBaseString3.append("&");
			}
			signatureBaseString3.append(nvp.getName() + "=" + nvp.getValue());
		}

		String signatureBaseStringTemplate = "%s&%s&%s";
		String signatureBaseString = String.format(signatureBaseStringTemplate,
				URLEncoder.encode(signatureBaseString1, "UTF-8"),
				URLEncoder.encode(signatureBaseString2, "UTF-8"),
				URLEncoder.encode(signatureBaseString3.toString(), "UTF-8"));

		System.out.println("signatureBaseString: " + signatureBaseString);

		String compositeKey = URLEncoder.encode(oAuthConsumerSecret, "UTF-8")
				+ "&" + URLEncoder.encode(oAuthAccessTokenSecret, "UTF-8");

		String oAuthSignature = computeSignature(signatureBaseString,
				compositeKey);
		System.out.println("oAuthSignature       : " + oAuthSignature);

		String oAuthSignatureEncoded = URLEncoder.encode(oAuthSignature,
				"UTF-8");
		System.out.println("oAuthSignatureEncoded: " + oAuthSignatureEncoded);

		String authorizationHeaderValueTempl = "OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"%s\", oauth_timestamp=\"%s\", oauth_token=\"%s\", oauth_version=\"%s\"";

		String authorizationHeaderValue = String.format(
				authorizationHeaderValueTempl, oAuthConsumerKey, oAuthNonce,
				oAuthSignatureEncoded, oAuthSignatureMethod, oAuthTimestamp,
				oAuthAccessToken, oAuthVersion);
		System.out.println("authorizationHeaderValue: "
				+ authorizationHeaderValue);

		StringBuffer urlWithParams = new StringBuffer(url);
		for (int i = 0; i < urlParams.size(); i++) {
			if (i == 0) {
				urlWithParams.append("?");
			} else {
				urlWithParams.append("&");
			}
			NameValuePair urlParam = urlParams.get(i);
			urlWithParams
					.append(urlParam.getName() + "=" + urlParam.getValue());
		}

		System.out.println("urlWithParams: " + urlWithParams.toString());
		System.out.println("authorizationHeaderValue:"
				+ authorizationHeaderValue);

		HttpGet getMethod = new HttpGet(urlWithParams.toString());
		getMethod.addHeader("Authorization", authorizationHeaderValue);

		HttpClient cli = new DefaultHttpClient();
		HttpResponse response = cli.execute(getMethod);
		// System.out.println("Status:"+response);
		// System.out.println("response : "+convertInputStreamToString(response));
		// System.out.println("length : "+response.getEntity().getContentLength());

		return parseResponse(convertInputStreamToString(response));
	}

	private ArrayList<String> parseResponse(String response) {
		Log.e(TAG, "res : " + response);

		try {
			JSONObject jsonResponse = new JSONObject(response);
			JSONArray jsonUsers = jsonResponse.getJSONArray("users");
			ArrayList<String> arrayList = new ArrayList<String>();
			for (int i = 0; i < jsonUsers.length(); i++) {
				arrayList.add(jsonUsers.getJSONObject(i).getString(
						"name"));
			}
			return arrayList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		/*
		 * try { JSONArray array = new JSONArray(response);
		 * 
		 * for (int i = 0; i < array.length(); i++) { Log.v(TAG, "WOEID : " +
		 * array.getJSONObject(i).get("woeid")); // Log.v(TAG, "url : " +
		 * array.getJSONObject(i).get("url")); // Log.v(TAG, "country : " +
		 * array.getJSONObject(i).get("country")); // Log.v(TAG, //
		 * "parentid : " + array.getJSONObject(i).get("parentid")); //
		 * Log.v(TAG, "name : " + array.getJSONObject(i).get("name"));
		 * dbHelper.insertTrends(array.getJSONObject(i).getLong("woeid"),
		 * array.getJSONObject(i).getString("name"), array
		 * .getJSONObject(i).getLong("parentid"), array
		 * .getJSONObject(i).getString("country"), array
		 * .getJSONObject(i).getString("url")); }
		 * 
		 * } catch (JSONException e) { e.printStackTrace(); }
		 */

	}

	private static String computeSignature(String baseString, String keyString)
			throws GeneralSecurityException, UnsupportedEncodingException,
			Exception {
		SecretKey secretKey = null;

		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

		Mac mac = Mac.getInstance("HmacSHA1");

		mac.init(secretKey);

		byte[] text = baseString.getBytes();

		return new String(Base64.encode(mac.doFinal(text), Base64.DEFAULT))
				.trim();
	}

	private static String convertInputStreamToString(HttpResponse response) {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(response
					.getEntity().getContent()));
			final StringBuffer stringBuffer = new StringBuffer("");
			String line = "";
			final String LineSeparator = System.getProperty("line.separator");

			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line + LineSeparator);
			}
			return stringBuffer.toString();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return "";
	}

	private String time() {
		long millis = System.currentTimeMillis();
		long secs = millis / 1000;
		return String.valueOf(secs);
	}

	public class NvpComparator implements Comparator<NameValuePair> {

		public int compare(NameValuePair arg0, NameValuePair arg1) {
			String name0 = arg0.getName();
			String name1 = arg1.getName();
			return name0.compareTo(name1);
		}
	}
}
