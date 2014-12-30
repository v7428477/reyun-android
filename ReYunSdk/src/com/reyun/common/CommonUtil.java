/**
 */
package com.reyun.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class CommonUtil {
	/**
	 * checkPermissions
	 * @param context
	 * @param permission  
	 * @return true or  false
	 */
	public static boolean checkPermissions(Context context, String permission) {
		PackageManager localPackageManager = context.getPackageManager();
		return localPackageManager.checkPermission(permission, context
				.getPackageName()) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Get the current time     format  yyyy-MM-dd HH:mm:ss
	 * 
	 * @return
	 */
	public static String getTime(long  interval) {
		Date date = new Date(System.currentTimeMillis() - interval);
		SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		return localSimpleDateFormat.format(date);
	}
	public static String getTimeZone() {
		
		int time_zone = TimeZone.getDefault().getRawOffset()/1000/3600 ;
		if(time_zone >0)
			return "+" +time_zone;
		else
			return time_zone+"";
	}

	/**
	 * get currnet activity's name
	 * @param context
	 * @return
	 */
	public static String getActivityName(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		if(checkPermissions(context, "android.permission.GET_TASKS")){
			ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
			return cn.getShortClassName();
		}else{
			if(ReYunConst.DebugMode){
				Log.e("lost permission", "android.permission.GET_TASKS");
			}
			
			return null;
		}
		
		
	}

	/**
	 * get  PackageName
	 * @param context
	 * @return
	 */
	public static String getPackageName(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		
		if(checkPermissions(context, "android.permission.GET_TASKS")){
			ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
			return cn.getPackageName();
		}else{
			if(ReYunConst.DebugMode){
				Log.e("lost permission", "android.permission.GET_TASKS");
			}
			
			return null;
		}
		
	}


	/**
	 * get deviceid
	 * @param context
	 *  add  <uses-permission android:name="READ_PHONE_STATE" /> 
	 * @return
	 */
	public static String getDeviceID(Context context) {
		if(checkPermissions(context, "android.permission.READ_PHONE_STATE")){
			String deviceId = "";
			if (checkPhoneState(context)) {
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
				deviceId = tm.getDeviceId();
				
				if(deviceId == null){
					deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
				}
			}
			if (deviceId != null) {
				
				if(ReYunConst.DebugMode){
					printLog("commonUtil", "deviceId:" + deviceId);
				}
				
				return deviceId;
			} else {
				if(ReYunConst.DebugMode){
					Log.e("commonUtil", "deviceId is null");
				}
				
				return "unknown";
			}
		}else{
			if(ReYunConst.DebugMode){
				Log.e("lost permissioin", "lost----->android.permission.READ_PHONE_STATE");
//				Toast.makeText(context, "getDeviceID lost----->android.permission.READ_PHONE_STATE", Toast.LENGTH_LONG);
			}
			
			
			return "unknown";
		}
	}
	
	// ��ȡ��Ӫ�̼�� ,Ĭ��ֵ 
	public static String getOperatorName(Context context) {
		if(checkPermissions(context, "android.permission.READ_PHONE_STATE")){
			
			String op = "unknown";
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			op = tm.getSimOperatorName();
			if (op != null) {
				if(op.equals(""))
					op = "unknown";
				
				if(ReYunConst.DebugMode){
					printLog("commonUtil", "op:" + op);
				}
				
				return op;
			} else {
				if(ReYunConst.DebugMode){
					Log.e("commonUtil", "deviceId is null");
				}
				
				return "unknown";
			}
		}else{
			if(ReYunConst.DebugMode){
				Log.e("lost permissioin", "lost----->android.permission.READ_PHONE_STATE");
				Toast.makeText(context, "getOperatorName lost----->android.permission.READ_PHONE_STATE", Toast.LENGTH_LONG);
			}
			
			return "unknown";
		}
	}
	
	public static String getPhoneResolution(Context context){
		
		 String resolution = "unknown";
		 WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		 DisplayMetrics displaysMetrics = new DisplayMetrics();
		 manager.getDefaultDisplay().getMetrics(displaysMetrics);
		 
		 resolution = displaysMetrics.widthPixels+"*"+displaysMetrics.heightPixels;
		
		return resolution;		
	}
	

	/**
	 * check phone _state is readied ;
	 * 
	 * @param context
	 * @return
	 */
	public static boolean checkPhoneState(Context context) {
		PackageManager packageManager = context.getPackageManager();
		if (packageManager.checkPermission("android.permission.READ_PHONE_STATE", context
				.getPackageName()) != 0) {
			return false;
		}
		return true;
	}

	/**
	 * get sdk number
	 * @param paramContext
	 * @return
	 */
	public static String getSdkVersion(Context paramContext) {
		String osVersion = "";
		if (!checkPhoneState(paramContext)) {
			osVersion = android.os.Build.VERSION.RELEASE;
			if(ReYunConst.DebugMode){
				Log.e("android_osVersion", "OsVerson" + osVersion);
			}
			
			return osVersion;
		} else {
			if(ReYunConst.DebugMode){
				Log.e("android_osVersion", "OsVerson get failed");
			}
			
			return null;
		}
	}

	/**
	 * Get the version number of the current program
	 * @param context
	 * @return
	 */

	public static String getCurVersion(Context context) {
		String curversion = "";
		try {
			// ---get the package info---
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm
					.getPackageInfo(context.getPackageName(), 0);
			curversion = pi.versionName;
			if (curversion == null || curversion.length() <= 0) {
				return "";
			}
		} catch (Exception e) {
			if(ReYunConst.DebugMode){
				Log.e("VersionInfo", "Exception", e);
			}
			
		}
		return curversion;
	}
    
    /**
     * To determine whether it contains a gyroscope
     * @return
     */
  public static boolean isHaveGravity(Context context){
	  SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	  if(manager==null){
		  return false;
	  }
	return true;
  }
  
  /**
   * Get the current networking
   * @param context
   * @return  WIFI or MOBILE
   */
  private static String getNetworkType(Context context){
      TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    int type=  manager.getNetworkType();
    String typeString="UNKOWN";
    if(type==TelephonyManager.NETWORK_TYPE_CDMA){
    	typeString ="CDMA";
    	typeString = "2G"; //����2G
    }else if(type==TelephonyManager.NETWORK_TYPE_EDGE){
    	typeString ="EDGE";
    	typeString = "2G";  // 2G ����
    }else if(type==TelephonyManager.NETWORK_TYPE_EVDO_0){
    	typeString ="EVDO_0";
    	typeString = "3G";  //����3G
    }else if(type==TelephonyManager.NETWORK_TYPE_EVDO_A){
    	typeString ="EVDO_A";
    	typeString = "3G";    //���� 3G
    }else if(type==TelephonyManager.NETWORK_TYPE_GPRS){
    	typeString ="GPRS";  
    	typeString = "2G";  //2G ����
    }else if(type==TelephonyManager.NETWORK_TYPE_HSDPA){
    	typeString ="HSDPA";
    	typeString = "3G";  // ��ͨ3G
    }else if(type==TelephonyManager.NETWORK_TYPE_HSPA){
    	typeString ="HSPA";
    }else if(type==TelephonyManager.NETWORK_TYPE_HSUPA){
    	typeString ="HSUPA";
    }else if(type==TelephonyManager.NETWORK_TYPE_UMTS){
    	typeString ="UMTS";
    	typeString = "3G"; //��ͨ 3G
    }else if(type==TelephonyManager.NETWORK_TYPE_UNKNOWN){
    	typeString ="UNKOWN";
    }
    
   
	return typeString;
  }
  /**
   * Determine the current network type  
   * @param context
   * @return
   */
public static boolean isNetworkTypeWifi(Context context) {
	// TODO Auto-generated method stub
	

	if(checkPermissions(context, "android.permission.INTERNET")){
		ConnectivityManager cManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); 
		NetworkInfo info = cManager.getActiveNetworkInfo(); 
		
			if (info != null && info.isAvailable()&&info.getTypeName().equals("WIFI")){ 
		        return true; 
		  }else{ 
			  if(ReYunConst.DebugMode){
				  Log.e("error", "Network not wifi");
			  }
		        return false; 
		  } 
	}else{
		if(ReYunConst.DebugMode){
			Log.e(" lost  permission", "lost----> android.permission.INTERNET");
		}
		return false;
	}
	
}

public static String getConnectType(Context context){
	
	if(checkPermissions(context, "android.permission.INTERNET")){
		ConnectivityManager cManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); 
		NetworkInfo mWifi = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);  
	    NetworkInfo mMobile = cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
		
				if (mWifi != null && mWifi.isAvailable()&&mWifi.isConnected()) {
					
					return  "WIFI";
				}else if(mMobile != null && mMobile.isAvailable()&&mMobile.isConnected()){
					String  connectType = getNetworkType(context);
					return connectType;
				}else{
					return "unknown";
				}
		}else {
				// Ȩ��ȱʧ
				if (ReYunConst.DebugMode) {
					Log.e(" lost  permission","lost----> android.permission.INTERNET");
				}
		}
	
	return "unknown";	
}

/**
 * check whether wap connect
 */
public static boolean isWapConnected(Context context){
	
	if(checkPermissions(context, "android.permission.INTERNET")){
		ConnectivityManager cManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mMobile = cManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if(mMobile != null && mMobile.isAvailable()&&mMobile.isConnected()){  
			
			String proxyHost = android.net.Proxy.getDefaultHost(); 
			if (proxyHost != null && !proxyHost.equals("")){ 
				
				// wap connection
				return true;
			}			
		}
	}else{
		
		if (ReYunConst.DebugMode) {
			Log.e(" lost  permission","lost----> android.permission.INTERNET");
		}
	}
	
	return false;	
}


/**
 * Get the current application version number
 * @param context
 * @return
 */
public static String getVersion(Context context) {
	String versionName = "";  
	try {  
		PackageManager pm = context.getPackageManager();  
		PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);  
		versionName = pi.versionName;  
		if (versionName == null || versionName.length() <= 0) {  
			return "";  
		}  
	} catch (Exception e) {  
		if(ReYunConst.DebugMode){
			Log.e("reyunsdk", "Exception", e);  
		}
		
	}  
	return versionName;
 }

/**
 * Set the output log
 * @param tag
 * @param log
 */
  public static void printLog(String tag,String log) {
	if(ReYunConst.DebugMode==true){
		Log.d(tag, log);
	}
}
  
  public static void printErrLog(String tag,String log) {
	  if(ReYunConst.DebugMode==true){
		  Log.e(tag, log);
	  }
  }
  
}
