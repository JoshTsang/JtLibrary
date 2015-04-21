package com.joshtttt.library.network;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.joshtttt.library.R;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;
import com.umeng.update.UpdateStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class ApplicationUpdateManager {

	private static final String TAG = "ApplicationUpdateManager";

//	private static final String KEY_VERSION_CODE = "versionCode";
//	private static final String KEY_VERSION_NAME = "version";
//	private static final String KEY_UPDATE_CONTENT = "updateContent";
//	private static final String KEY_FILE_NAME = "filename";
//	private static final String APK_DIR_NAME = "/apk/";

	private static final String KEY_IGNORE_VERSION_CODE = "ignoreVersionCode";

	private static final long MIN_CHECK_GAP = 1000L * 60 * 60 * 15;

	protected static final String UPDATE_MANAGER_PREFERENCES = "updateManagerPreferences";

	Context mContext;
	AlertDialog mUpgradDlg;
	static long mLastCheckTime = 0L;
	long mDownloadReference;
	private String mVersionInfoUrl;
	private String mDownloadPath;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent
					.getAction())) {
				long reference = intent.getLongExtra(
						DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				Log.d(TAG, "refrence:" + reference + "#my reference:"
						+ mDownloadReference + ", intent:" + intent.toString());
				if (mDownloadReference == reference) {
					DownloadManager downloadManager;
					mContext.unregisterReceiver(mReceiver);
					downloadManager = (DownloadManager) mContext
							.getSystemService(Context.DOWNLOAD_SERVICE);
					Uri apkUri = downloadManager
							.getUriForDownloadedFile(reference);

					if (apkUri != null) {
						Intent installIntent = new Intent(
								Intent.ACTION_INSTALL_PACKAGE);
						installIntent.setData(apkUri);
						mContext.startActivity(installIntent);
					}
				}
			}
		}
	};

	public class VersionInfo {
		String versionName;
		int versionCode;
		String updateContent;
		String url;
	}

	public ApplicationUpdateManager(Context context, String versionInfoUrl, String downloadPath) {
		mContext = context;
		mVersionInfoUrl = versionInfoUrl;
		mDownloadPath = downloadPath;
	}

	public void checkUpdate() {
		Log.d(TAG,
				"updage check gap:"
						+ Math.abs(mLastCheckTime - System.currentTimeMillis()));
		if (Math.abs(mLastCheckTime - System.currentTimeMillis()) > MIN_CHECK_GAP) {
			UmengUpdateAgent.setUpdateOnlyWifi(false);
			UmengUpdateAgent.setUpdateAutoPopup(false);
			UmengUpdateAgent.setUpdateFromPushMessage(false);
			UmengUpdateAgent.update(mContext);
			UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {
				@Override
				public void onUpdateReturned(int updateStatus,UpdateResponse updateInfo) {
					switch (updateStatus) {
					case UpdateStatus.Yes: // has update
						Log.d(TAG, "update info:" + updateInfo.toString());
						Log.d(TAG, "update path:" + updateInfo.path);
						UmengUpdateAgent.showUpdateDialog(mContext, updateInfo);
						break;
					default:
						new CheckUpdateTask().execute();
						break;
					}
				}
			});
		}
	}

	private void showUpdateDialog(final VersionInfo info) {
		AlertDialog.Builder dlg = new AlertDialog.Builder(mContext,
				AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

		final View layout = LayoutInflater.from(mContext).inflate(
				R.layout.umeng_update_dialog, null);
		final TextView versionUpdateContent = (TextView) layout
				.findViewById(R.id.umeng_update_content);
		final Button okBtn = (Button) layout
				.findViewById(R.id.umeng_update_id_ok);
		final Button cancelBtn = (Button) layout
				.findViewById(R.id.umeng_update_id_cancel);
		final Button closeBtn = (Button) layout
				.findViewById(R.id.umeng_update_id_close);
		final CheckBox ignoreThisVersion = (CheckBox) layout
				.findViewById(R.id.umeng_update_id_check);

		View.OnClickListener onCancelClicked = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (ignoreThisVersion.isChecked()) {
					SharedPreferences sharedPreferences = mContext
							.getSharedPreferences(UPDATE_MANAGER_PREFERENCES,
									Activity.MODE_PRIVATE);
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putInt(KEY_IGNORE_VERSION_CODE, info.versionCode);
					editor.commit();
				}
				mUpgradDlg.dismiss();
			}
		};

		String updateContent = String.format(Locale.CHINA, "%s:%s\n%s:\n%s",
				mContext.getString(R.string.app_versionTitle),
				info.versionName, mContext.getString(R.string.update_content),
				info.updateContent);
		versionUpdateContent.setText(updateContent);
		cancelBtn.setOnClickListener(onCancelClicked);
		closeBtn.setOnClickListener(onCancelClicked);
		okBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				upgrade(info);
				mUpgradDlg.dismiss();
			}
		});

		dlg.setView(layout);
		dlg.setCancelable(false);
		
		if (mUpgradDlg != null) {
			if (mUpgradDlg.isShowing()) {
				mUpgradDlg.dismiss();
			}
		}
		
		mUpgradDlg = dlg.create();
		mUpgradDlg.show();
	}

	private VersionInfo checkNewVersionAvailable() {
		VersionInfo versionInfo = null;
		versionInfo = getVersionInfoOnDailyBuildServer();
		if (versionInfo != null) {
			if (willDoUpdate(versionInfo)) {
				return versionInfo;
			}
		}

		versionInfo = getVersionInfoOnServer();
		if (versionInfo != null) {
			if (willDoUpdate(versionInfo)) {
				return versionInfo;
			}
		}

		return null;
	}

	private boolean willDoUpdate(VersionInfo versionInfo) {
		return versionInfo.versionCode > getCurrentVersionCode()
				&& versionInfo.versionCode > getIgnoredVersionCode();
	}

	private void upgrade(final VersionInfo info) {
		downloadApk(info);
	}

	private VersionInfo getVersionInfoOnServer() {
		// http://192.168.0.18/getFileList.php?project=tonghao&branch=master

//		String baseUrl = Utils.getWebFilePath(mContext, APK_DIR_NAME
//				+ "apk_info.json");

		HttpGet getMethod = new HttpGet(mVersionInfoUrl);

		HttpClient httpClient = new DefaultHttpClient();

		try {
			HttpResponse response = httpClient.execute(getMethod);

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				String body = EntityUtils.toString(response.getEntity(),
						"utf-8");
				Log.d(TAG, "result body:" + body);
				if (!TextUtils.isEmpty(body)) {
					return parseDailyBuildInfo(body);
				}
			} else {
				Log.d(TAG, "status code:" + statusCode);
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private VersionInfo getVersionInfoOnDailyBuildServer() {
		// http://192.168.0.18/getFileList.php?project=tonghao&branch=master
//		String ser = SERVER_ADDR.substring(0, 17);
//		if (!Utils.getServerAddress(mContext).contains(ser)) {
//			Log.d(TAG, "testing server addr:" + ser
//					+ ", current server:" + Utils.getServerAddress(mContext));
//			return null;
//		}
//
//		Log.i(TAG, "under testing environment, check daily build server");
//		List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
//		params.add(new BasicNameValuePair("project", PROJECT_NAME));
//		params.add(new BasicNameValuePair("branch", PROJECT_BRANCH));
//
//		String param = URLEncodedUtils.format(params, "UTF-8");
//
//		String baseUrl = SERVER_URL;
//
//		HttpGet getMethod = new HttpGet(baseUrl + "?" + param);
//
//		HttpClient httpClient = new DefaultHttpClient();
//
//		try {
//			HttpResponse response = httpClient.execute(getMethod);
//
//			int statusCode = response.getStatusLine().getStatusCode();
//
//			if (statusCode == 200) {
//				String body = EntityUtils.toString(response.getEntity(),
//						"utf-8");
//				Log.d(TAG, "result body:" + body);
//				return parseDailyBuildInfo(body);
//			} else {
//				Log.d(TAG, "status code:" + statusCode);
//			}
//		} catch (ClientProtocolException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		return null;
	}

	private void removeApkFiles() {
//		String dirPath = Utils.getExternalStorageDirectory();
		File dir = new File(mDownloadPath);
		if (dir.exists() && dir.isDirectory()) {
			String[] fileList = dir.list();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].endsWith(".apk")) {
					String apkFilePath = mDownloadPath + fileList[i];
					File apkFile = new File(apkFilePath);
					Log.d(TAG,
							"delete file:" + apkFilePath + ","
									+ apkFile.delete());
				}
			}
		} else {
			Log.d(TAG, "dir not exists:" + mDownloadPath);
		}
	}

	private int getCurrentVersionCode() {

		PackageManager packageManager = mContext.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					mContext.getPackageName(), 0);

			return packageInfo.versionCode;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Integer.MAX_VALUE;
	}

	private int getIgnoredVersionCode() {

		SharedPreferences sharedPreferences = mContext.getSharedPreferences(
				UPDATE_MANAGER_PREFERENCES, Activity.MODE_PRIVATE);

		return sharedPreferences.getInt(KEY_IGNORE_VERSION_CODE, 0);
	}

	private void downloadApk(VersionInfo info) {
		Log.d(TAG, "download apk:" + info.url);
		DownloadManager downloadManager;
		downloadManager = (DownloadManager) mContext
				.getSystemService(Context.DOWNLOAD_SERVICE);

		Uri uri = Uri.parse(info.url);
		DownloadManager.Request request = new Request(uri);
		File downloadDir = new File(mDownloadPath);
		if (!downloadDir.exists()) {
			downloadDir.mkdirs();
		}
		
		request.setTitle(mContext.getString(R.string.app_name));
		request.setDescription(info.versionName);
		request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
		request.setDestinationUri(Uri.fromFile(new File(mDownloadPath,
				info.versionCode + ".apk")));
		mDownloadReference = downloadManager.enqueue(request);
		IntentFilter filter = new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		mContext.registerReceiver(mReceiver, filter);
	}

	private VersionInfo parseDailyBuildInfo(String json) {
//		int maxVersionCode = 0;
		VersionInfo versionInfo = null;
//		try {
//			JSONArray infos = new JSONArray(json);
//			JSONObject buildInfo;
//			for (int i = 0; i < infos.length(); i++) {
//				buildInfo = (JSONObject) infos.get(i);
//				if (buildInfo.has(KEY_VERSION_CODE)) {
//					int versionCode = buildInfo.getInt(KEY_VERSION_CODE);
//					if (versionCode > maxVersionCode) {
//						if (versionInfo == null) {
//							versionInfo = new VersionInfo();
//						}
//						maxVersionCode = versionCode;
//						versionInfo.versionCode = versionCode;
//						versionInfo.versionName = buildInfo
//								.getString(KEY_VERSION_NAME);
//
//						if (buildInfo.has(KEY_FILE_NAME)) {
//							versionInfo.url = buildInfo
//									.getString(KEY_FILE_NAME);
//						}
//
//						if (buildInfo.has(KEY_UPDATE_CONTENT)) {
//							versionInfo.updateContent = buildInfo
//									.getString(KEY_UPDATE_CONTENT);
//							versionInfo.url = Utils.getWebFilePath(mContext,
//									APK_DIR_NAME + versionInfo.url);
//						} else {
//							versionInfo.updateContent = mContext
//									.getString(R.string.verion_on_ci_server);
//							versionInfo.url = String.format(DOWNLOAD_APK_URL
//									+ "%s", versionInfo.url);
//						}
//					}
//				}
//			}
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		return versionInfo;
	}

	class CheckUpdateTask extends
			AsyncTask<Void, Void, ApplicationUpdateManager.VersionInfo> {

		@Override
		public void onPostExecute(ApplicationUpdateManager.VersionInfo result) {
			super.onPostExecute(result);

			mLastCheckTime = System.currentTimeMillis();

			if (result != null) {
				showUpdateDialog(result);
			} else {
				removeApkFiles();
			}
		}

		@Override
		protected VersionInfo doInBackground(Void... params) {
			return checkNewVersionAvailable();
		}
	}
}
