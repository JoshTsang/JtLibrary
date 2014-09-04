package com.joshtttt.library.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Network {

	  public static boolean isNetworkAvailable(Context cnx) {
	        ConnectivityManager connectivity = (ConnectivityManager) cnx
	                .getSystemService(Context.CONNECTIVITY_SERVICE);
	        try {
	            NetworkInfo info = connectivity.getActiveNetworkInfo();
	            if (info != null && info.isAvailable()) {
	                return true;
	            }
	            
	        } catch (Exception e) {
	        }
	        
	        return false;
	    }
	    
}
