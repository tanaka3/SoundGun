package net.masaya3.gunsound;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.masaya3.gunsound.data.BulletInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author tanaka
 *
 */
public class AutoReloadTask {
	private static final String DATE_PATTERN = "yyyyMMddHHmmss";

	public enum Mode {
		None,
		Processing,
		Finish
	}
	public interface OnCompletionListener {
		public void onCompletion(List<BulletInfo> result);
	}

	private Context mContext;
	private Mode mPollingMode = Mode.None;
	private List<BulletInfo> mBulletInfoList = new ArrayList<BulletInfo>();
	private final HashMap<String, BulletInfo> mBulletHistory;

	private OnCompletionListener mOnCompletionListener;
	public void setOnCompletionListener(OnCompletionListener listener){
		mOnCompletionListener = listener;
	}

	public boolean isProcessing() {
		return mPollingMode == Mode.Processing;
	}

	public AutoReloadTask(Context context, HashMap<String, BulletInfo> bulletHistory ) {
		mContext = context;
		mBulletHistory = bulletHistory;
	}

	public void start() {

		if(mPollingMode != Mode.None){
			return;
		}

		mPollingMode = Mode.Processing;
		getBulletConfig(mSuccessListener, mErrorListener);
	}


	private Response.Listener<JSONObject> mSuccessListener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			Log.d("CalenderFragment", response.toString());
			try {
				JSONArray values = response.getJSONArray("data");
				for (int i = 0; i < values.length(); i++) {
					BulletInfo info = new BulletInfo();
					JSONObject value = values.getJSONObject(i);
					info.url = value.getString("url");

					if(mBulletHistory != null &&
							!mBulletHistory.containsKey(info.url)){
						mBulletInfoList.add(info);
					}
				}

				AsyncTask task = new AsyncTask() {
					@Override
					protected Object doInBackground(Object[] objects) {

						for (BulletInfo info : mBulletInfoList) {
							downloadSound(info);
						}
						return null;
					}
					@Override
					protected void onPostExecute(Object result){
						if(mOnCompletionListener != null){
							mOnCompletionListener.onCompletion(mBulletInfoList);
						}

						mPollingMode = Mode.Finish;
					}
				};
				task.execute();

			} catch (JSONException e) {
				Log.d("CalenderFragment", e.toString());
			}
		}
	};

	private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mPollingMode = Mode.Finish;
		}
	};

	public void getBulletConfig(Response.Listener<JSONObject> successListener, Response.ErrorListener errorListener) {
		RequestQueue requestQueue = Volley.newRequestQueue(mContext);
		// Volley でリクエスト
		String url = mContext.getString(R.string.bullet_url);

		JsonObjectRequest indexJson = new JsonObjectRequest(url, successListener, errorListener) {
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> headers = super.getHeaders();
				Map<String, String> newHeaders = new HashMap<String, String>();
				newHeaders.putAll(headers);
				return newHeaders;
			}
		};

		requestQueue.add(indexJson);
	}

	private void downloadSound(BulletInfo info) {
		try {
			URL url = new URL(info.url);


			// HttpURLConnection インスタンス生成
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


			// タイムアウト設定

			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(20000);

			// リクエストメソッド
			urlConnection.setRequestMethod("GET");

			// リダイレクトを自動で許可しない設定
			urlConnection.setInstanceFollowRedirects(false);

			// ヘッダーの設定(複数設定可能)
			urlConnection.setRequestProperty("Accept-Language", "jp");

			// 接続
			urlConnection.connect();

			int resp = urlConnection.getResponseCode();

			switch (resp) {

				case HttpURLConnection.HTTP_OK:

					Calendar now = Calendar.getInstance();
					SimpleDateFormat dateformat = new SimpleDateFormat(DATE_PATTERN);
					Random rand = new Random();

					String sound_file = String.format("/%s_%04d.mp4", dateformat.format(now.getTime()), rand.nextInt(10000));
					info.soundFile = mContext.getCacheDir() + sound_file;

					InputStream input = urlConnection.getInputStream();
					DataInputStream dataInput = new DataInputStream(input);

					FileOutputStream fileOutput = new FileOutputStream(info.soundFile);
					DataOutputStream dataOut = new DataOutputStream(fileOutput);
					// 読み込みデータ単位
					final byte[] buffer = new byte[4096];
					// 読み込んだデータを一時的に格納しておく変数
					int readByte = 0;

					// ファイルを読み込む
					while((readByte = dataInput.read(buffer)) != -1) {
						dataOut.write(buffer, 0, readByte);
					}

					// 各ストリームを閉じる
					dataInput.close();
					fileOutput.close();
					dataInput.close();
					input.close();

					info.success = true;

					break;

				case HttpURLConnection.HTTP_UNAUTHORIZED:
					break;

				default:
					break;
			}

		} catch (Exception e) {

			e.printStackTrace();

			info.success = false;

		}
	}
}