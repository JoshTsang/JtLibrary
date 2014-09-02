package com.joshtttt.jtlibrary.view;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.Inflater;

import com.joshtttt.jtlibrary.R;
import com.joshtttt.jtlibrary.network.Samba;
import com.joshtttt.jtlibrary.util.DateUtils;
import com.joshtttt.jtlibrary.util.Log;
import com.joshtttt.jtlibrary.util.ZipUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DeveloperToolsView extends LinearLayout {
	private final static String TAG = "DeveloperToolsActivity";
	
	public static final String DEVELOPER_SETTING = "dev_setting";
	public static final String IS_DEVELOPER = "is_developer";
	public static final String DEVELOPER_SERVER_SETTING = "dev_server";
	public final static String DEVELOPER_TOOLS_UNLOCKED = "dev_tool_unlocked";
	public final static String FILE_LOG = "file_log";
	
	protected Context mContext;
	
	private enum UPDATE_TYPE {
		DATABASE, LOG
	}
	
	private UPDATE_TYPE mUploadType;
	
	@SuppressLint("NewApi")
	public DeveloperToolsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}


	public DeveloperToolsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.view_dev_tool, this);
		mContext = context;
	}


	public DeveloperToolsView(Context context) {
		super(context);
		mContext = context;
		
	}


	protected String getDeviceInfo(Context context) {
		try {
			org.json.JSONObject json = new org.json.JSONObject();
			android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);

			String device_id = tm.getDeviceId();

			android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);

			String mac = wifi.getConnectionInfo().getMacAddress();
			json.put("mac", mac);

			if (TextUtils.isEmpty(device_id)) {
				device_id = mac;
			}

			if (TextUtils.isEmpty(device_id)) {
				device_id = android.provider.Settings.Secure.getString(
						context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);
			}

			json.put("device_id", device_id);
			json.put("product_model", android.os.Build.MODEL);
			json.put("sdk_ver", android.os.Build.VERSION.RELEASE);
			DisplayMetrics dm = new DisplayMetrics();  
			//TODO get resulution
//	        getWindowManager().getDefaultDisplay().getMetrics(dm);  
//	        float width=dm.widthPixels*dm.density;   
//	        float height=dm.heightPixels*dm.density; 
//	        json.put("resolution", String.format("%fx%f", width, height));

			return json.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	protected int uploadDatabase() {
		//TODO updated entire db dir
		String uploadPath = getUmengConfig("databaseUploadPath");
		
		if (TextUtils.isEmpty(uploadPath)) {
			return R.string.upload_path_not_configed;
		}
		
		uploadPath += "/" + DateUtils.currentTimeStamp() + android.os.Build.MODEL;
		uploadPath = uploadPath.replace(" ", "_");
		String filePath = mContext.getDatabasePath(" ").getAbsolutePath();
		Log.w(TAG, filePath);
		try {
			File dbDir = new File(filePath);
			final String dbList[] = dbDir.list();
			final String remote = uploadPath;
			final String local = filePath;
			
			if (dbList == null) {
				return R.string.no_database;
			}
			
			if (dbList.length > 1) {
				AlertDialog.Builder selectDb = new AlertDialog.Builder(mContext);
				selectDb.setItems(dbList,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								Samba.upload(remote, local + "/"
										+ dbList[which]);
							}
						});
			} else if (dbList.length == 1) {
				Samba.upload(remote, local + "/"
						+ dbList[0]);
			} else {
				return R.string.no_database;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return R.string.upload_failed;
		}
		
		return R.string.upload_success;
	}
	
	protected int uploadLog() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String zipFilePath = DateUtils.currentTimeStamp()
					+ android.os.Build.MODEL;
			zipFilePath = Environment.getExternalStorageDirectory() + "/log.zip";
			
			String uploadPath = null;
			uploadPath = getUmengConfig("logUploadPath");
			
			
			if (TextUtils.isEmpty(uploadPath)) {
				Toast.makeText(mContext, "Log上传路径未指定", Toast.LENGTH_SHORT).show();
				return 0;
			} 
			
			uploadPath += "/" + DateUtils.currentTimeStamp() + android.os.Build.MODEL;
			uploadPath = uploadPath.replace(" ", "_");
			
			try {
				Log.d(TAG, "upload:" + zipFilePath + " to " + uploadPath);
				ZipUtil.zipFolder(Environment.getExternalStorageDirectory()
						+ Log.LOG_DIR, zipFilePath);
				Samba.upload(uploadPath, zipFilePath);
				File zipFile = new File(zipFilePath);
				zipFile.deleteOnExit();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return R.string.zip_failed;
			}
			
			return R.string.upload_success;
		} else {
			return R.string.no_sdcard;
		}
	}


	private String getUmengConfig(String param) {
		String config = null;
		try {
			Class c;
			Method m;
			c = Class.forName("com.umeng.analytics.MobclickAgent");
			m = c.getMethod("getConfigParams", new Class[] { Context.class,
					String.class });
			config = (String) m.invoke(c, new Object[] { mContext, param});
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
			Toast.makeText(mContext, "找不到友盟包", Toast.LENGTH_SHORT).show();
			return null;
		} catch (ClassNotFoundException e) {
			Toast.makeText(mContext, "找不到友盟包", Toast.LENGTH_SHORT).show();
			return null;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return config;
	}

	protected void zipLogAndDatabase() {
		String zipFilePath = null;
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String logDir = Environment.getExternalStorageDirectory()
					+ "/ibaby_ziped_log/";
			File dir = new File(logDir);
			if (!dir.exists()) {
				dir.mkdir();
			}
			
			zipFilePath = logDir + DateUtils.currentTimeStamp()
					+ android.os.Build.MODEL + ".zip";
			String dbZipPath = Environment.getExternalStorageDirectory()
					+ Log.LOG_DIR + DateUtils.currentTimeStamp()
					+ android.os.Build.MODEL + "_db.zip";
			
			String filePath = mContext.getDatabasePath("").getAbsolutePath();
			Log.e(TAG, filePath);
			try {
				ZipUtil.zipFolder(filePath, dbZipPath);
				ZipUtil.zipFolder(Environment.getExternalStorageDirectory()
						+ Log.LOG_DIR, zipFilePath);
				AlertDialog.Builder dlg = new AlertDialog.Builder(mContext);
				dlg.setMessage("压缩文件已保存到：" + zipFilePath);
				dlg.show();
			} catch (Exception e) {
				Toast.makeText(mContext, R.string.zip_failed, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		} else {
			Toast.makeText(mContext, R.string.no_sdcard, Toast.LENGTH_SHORT).show();
		}
	}

//	public void selectServer() {
//		final String serverList[] = MobclickAgent.getConfigParams(
//				mContext, "serverList").split(",");
//		Log.d(TAG, "serverList:" + MobclickAgent.getConfigParams(
//				mContext, "serverList"));
//		if (serverList != null) {
//			AlertDialog.Builder builder = Util.getAlertDialogBuilder(
//					DeveloperToolsActivity.this);
//			builder.setItems(serverList,
//					new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int which) {
//							String url = "";
//							if (which != 0) {
//								url = serverList[which];
//								RestClient.setOnlineBaseUrl(url);
//								RestClient.updateUrl();
//							}
//							SharedPreferences settings = getSharedPreferences(DEVELOPER_SETTING, 0);
//							SharedPreferences.Editor editor = settings.edit();
//							editor.putString(DEVELOPER_SERVER_SETTING, url);
//							editor.commit();
//						}
//					});
//			builder.create().show();
//		}
//	}
	
	public class UploadTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			// TODO Auto-generated method stub
			switch (mUploadType) {
			case DATABASE:
				return uploadDatabase();
			case LOG:
				return uploadLog();
			default:
				break;
			}
			
			return R.string.cancel;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (result > 0) {
				Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
			}
		}
    }
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		SharedPreferences settings = mContext.getSharedPreferences(DEVELOPER_SETTING, 0);
        
		Button report = (Button) findViewById(R.id.report);
		report.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder dlg = new AlertDialog.Builder(mContext);
				dlg.setTitle("设备信息已上报");
				dlg.setMessage(getDeviceInfo(mContext));
				dlg.show();
			}
		});
		
		Button enableDevOption = (Button) findViewById(R.id.enable_dev_setting);
		enableDevOption.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SharedPreferences settings = mContext.getSharedPreferences(DEVELOPER_SETTING, 0);
				boolean isDev = settings.getBoolean(IS_DEVELOPER, false);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(IS_DEVELOPER, !isDev);
				editor.commit();
				((Button)v).setText((isDev?"enable":"disable") + " dev mode");
			}
		});
		enableDevOption.setText((settings.getBoolean(IS_DEVELOPER, false)?"disable":"enable") + " dev mode");
		
		Button uploadDatabase = (Button) findViewById(R.id.upload_database);
		uploadDatabase.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UploadTask task = new UploadTask();
				v.setEnabled(false);
				mUploadType = UPDATE_TYPE.DATABASE;
				task.execute();
			}
		});
		
		Button uploadLog = (Button) findViewById(R.id.upload_log);
		uploadLog.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UploadTask task = new UploadTask();
				v.setEnabled(false);
				mUploadType = UPDATE_TYPE.LOG;
				task.execute();
			}
		});
		
		Button zipLogAndDatabase = (Button) findViewById(R.id.zip_log_and_db);
		zipLogAndDatabase.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				zipLogAndDatabase();
			}
		});
		
		Button fileLog = (Button) findViewById(R.id.enable_file_log);
		if (settings.getBoolean(FILE_LOG, false)) {
			fileLog.setText("disable file log");
		} else {
			fileLog.setText("enable file log");
		}
		fileLog.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SharedPreferences sp = mContext.getSharedPreferences(DEVELOPER_SETTING, 0);
				boolean logFile = sp.getBoolean(FILE_LOG, false);
				SharedPreferences.Editor editor = sp.edit();
				editor.putBoolean(FILE_LOG, !logFile);
				editor.commit();
				if (logFile) {
					((Button)v).setText("enable file log");
				} else {
					((Button)v).setText("disable file log");
				}
			}
		});
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(DEVELOPER_TOOLS_UNLOCKED, true);
		editor.commit();
	}

}
