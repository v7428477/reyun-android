/**
 *
 * An open source analytics android sdk for mobile applications
 *
 *sdk 要做的工作，提供给用户一个启动sdk 的接口。上传单条数据的接口
 */
package com.reyun.sdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.http.network.httpnetwork;
import com.reyun.common.CommonUtil;
import com.reyun.common.ReYunConst;
import com.reyun.common.RequestParaExd;
import com.reyun.common.SqliteDbaseUtil;
import com.reyunloopj.JsonHttpResponseHandler;

/**
 * @author Administrator
 * 
 */
public class ReYun {

	private static String m_appid = null;
	private static String m_channelid = "unknown";
	private final static String TAG = "reyunsdk";
	private final static String TAG_NETWORK = "HTTP_NETWORK";

	private final static String KEY_VALUE_DIVIDE = "\001";
	private final static String CONTEXT_DIVIDE = "\002";

	private static Timer m_heartBeatTimer = new Timer(true);

	private static CatchHomeBtnThread m_catchHomeBtnThread = null;

	private static Context my_context = null;

	private static TimerTask my_timerTask = null;

	private static HomeBtnBroadcastReceiver my_homeBtnReceiver = null;

//	 private final static int HEART_BEAT_TIME = 1000;
	private final static int HEART_BEAT_TIME = 5 * 60 * 1000;

	private volatile static boolean isSdkExit = false;

	private static int my_level;

	private static long interval = 0;

	private static ScreenObserver mScreenObserver;

	// public static boolean m_isCatchingHomeState = true;

	// public static void setDebugMode(boolean pDebugMode){
	//
	// ReYunConst.DebugMode = pDebugMode;
	// }

	/**
	 * 枚举类型定义
	 */
	public enum Gender {
		M, F, O, UNKNOWN;
	}

	public enum AccountType {
		ANONYMOUS, REGISTERED, SINAWEIBO, WECHAT, QQ, FACEBOOK, TWITTER, UNKNOWN;
	}

	public enum PaymentType {
		UNIONPAY, APPLE, FREE;
	}

	public enum QuestStatus {
		a, c, f;
	}

	public static String getAppId() {
		return m_appid;
	}

	public static String getChannelId() {
		return m_channelid;
	}

	public String getDeviceId() {

		if (my_context != null) {

			return CommonUtil.getDeviceID(my_context);
		}

		return "unknown";
	}

	/**
	 * 调用时机：app 即将退出之前调用
	 */
	public static void exitSdk() {

		SqliteDbaseUtil.getInstance(my_context).closeDataBase();// 关闭数据库
		if (mScreenObserver != null)// 锁屏对象
			mScreenObserver.stopScreenStateUpdate();

		if (Integer.parseInt(Build.VERSION.SDK) < 14
				&& my_homeBtnReceiver != null) {

			my_context.unregisterReceiver(my_homeBtnReceiver);
		} else {
			if (m_catchHomeBtnThread != null) {

				try {
					m_catchHomeBtnThread.close();
					m_catchHomeBtnThread.interrupt();
					isSdkExit = true;
					Thread.sleep(0);
					m_catchHomeBtnThread = null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		if (m_heartBeatTimer != null) {
			m_heartBeatTimer.cancel();
			m_heartBeatTimer = null;
		}

		if (m_appid != null) {
			m_appid = null;
		}
		my_homeBtnReceiver = null;
		my_context = null;
	}

	/**
	 * 调用时机：在app 启动入口处调用
	 * 
	 * @param context
	 * @throws Exception
	 */
	public static void initWithKeyAndChannelId(Context appContext,
			String appId, String channelId) throws Exception {

		m_appid = appId;
		m_channelid = channelId;

		if (m_channelid == null || m_channelid.equals("")) {

			m_channelid = "unknown";
		}

		my_context = appContext;

		if (checkAppid(appContext) == false) {

			return;
		}

		SharedPreferences mysharedPreferences = my_context
				.getSharedPreferences("reyun_interval", my_context.MODE_PRIVATE);
		interval = mysharedPreferences.getLong("interval", 0);

		/**
		 * 获取服务器时间间隔
		 */
		JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, JSONObject responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);
			}

			@Override
			public void onSuccess(int respCode, JSONObject responseBody) {
				// TODO Auto-generated method stub
				super.onSuccess(respCode, responseBody);

				String serverTime = System.currentTimeMillis() + "";
				try {
					serverTime = responseBody.getString("ts");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				long myserverTime = Long.parseLong(serverTime);
				interval = System.currentTimeMillis() - myserverTime;

				SharedPreferences mysharedPreferences = my_context
						.getSharedPreferences("reyun_interval",
								my_context.MODE_PRIVATE);
				Editor myEditor = mysharedPreferences.edit();
				myEditor.clear();
				myEditor.putLong("interval", interval);
				myEditor.commit();
			}

		};
		httpnetwork.get(my_context, "receive/gettime", null, responseHandler);

		/**
		 * 安装事件上传
		 */
		SharedPreferences sharedPreferences = appContext.getSharedPreferences(
				"appIntall", appContext.MODE_PRIVATE);
		String value = sharedPreferences.getString("isAppIntall", "unIntalled");
		if (value.equals("unIntalled") == true) {

			CommonUtil.printLog(TAG, "============new intall event=========");
			final RequestParaExd mydata = getUserInstallData(appContext);
			JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

				@Override
				public void onFailure(Throwable exception, String responseBody) {
					// TODO Auto-generated method stub
					super.onFailure(exception, responseBody);

					CommonUtil.printErrLog(TAG,
							"==============SEND FAILED ========== install ");
					addRecordToDbase("install", mydata);
				}

				@Override
				public void onSuccess(int arg0, JSONObject arg1) {
					// TODO Auto-generated method stub
					super.onSuccess(arg0, arg1);

					CommonUtil.printLog(TAG,
							"==============SEND SUCCESS ========== install :"
									+ arg1.toString());
				}
			};

			httpnetwork.postJson(my_context, "install", mydata,
					myJsonRespHandler);
			sharedPreferences = appContext.getSharedPreferences("appIntall",
					appContext.MODE_PRIVATE);
			Editor appEditor = sharedPreferences.edit();
			appEditor.putString("isAppIntall", "intalled");
			appEditor.commit();
		}

		/**
		 * 每次启动事件上传
		 */

		final RequestParaExd mydata = getUserStartUpData(appContext);
		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== startup ");
				addRecordToDbase("startup", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(
						TAG,
						"==============SEND SUCCESS ========== startup "
								+ arg1.toString());
			}
		};
		httpnetwork.postJson(my_context, "startup", mydata, myJsonRespHandler);

	}

	/**
	 * 调用时机： 在用户成功注册后调用
	 * 
	 * @param accountid
	 *            账号 id app 开发者必须正确提供
	 * @param serverid
	 *            服务器 id，如果不填则为 unknown
	 * @param gender
	 *            性别，如果不填则为 unknown ，f 代表女，m 代表男，o 代表其它
	 * @return
	 */
	public static void setRegisterBusiness(String accountid,
			String accountType, Gender gender, int age, String serverId) {

		if (checkAppid(my_context) == false) {

			return;
		}

		if (accountid == null || accountid.equals("")
				|| accountid.trim().length() == 0) {

			CommonUtil.printErrLog(TAG,
					"accountid is incorrect,cancle send....");
			return;
		}

		String strAccountType = "";

		if (accountType == null || accountType.equals("")
				|| accountType.trim().length() == 0) {
			strAccountType = "unknown";
		} else {
			strAccountType = accountType;
		}

		String strServerid = "";
		if (serverId == null || serverId.equals("")) {
			strServerid = "unknown";
		} else {
			strServerid = serverId;
		}

		String strGender = "";

		if (gender == null) {
			strGender = "unknown";
		} else {
			strGender = gender.name();
		}

		SharedPreferences mysharedPreferences = my_context
				.getSharedPreferences("reyun_regInfo",
						ReYun.my_context.MODE_PRIVATE);
		Editor myEditor = mysharedPreferences.edit();
		myEditor.putString("accountid", accountid);
		myEditor.putString("accountType", strAccountType);
		myEditor.putString("gender", strGender);
		myEditor.putString("age", age + "");
		myEditor.putString("serverid", strServerid);
		myEditor.commit();

	}

	/**
	 * 调用时机： 在用户成功注册后调用
	 * 
	 * @param accountid
	 *            账号 id app 开发者必须正确提供
	 * @param serverid
	 *            服务器 id，如果不填则为 unknown
	 * @param gender
	 *            性别，如果不填则为 unknown ，f 代表女，m 代表男，o 代表其它
	 * @param context
	 *            全局 context
	 * @return
	 */
	public static void setRegisterWithAccountID(String accountid,
			String accountType, Gender gender, int age, String serverId) {

		if (checkAppid(my_context) == false) {

			return;
		}

		if (accountid == null || accountid.equals("")
				|| accountid.trim().length() == 0) {

			CommonUtil.printErrLog(TAG,
					"accountid is incorrect,cancle send....");
			return;
		}

		String strAccountType = "";

		if (accountType == null || accountType.equals("")
				|| accountType.trim().length() == 0) {
			strAccountType = "unknown";
		} else {
			strAccountType = accountType;
		}

		String strServerid = "";
		if (serverId == null || serverId.equals("")) {
			strServerid = "unknown";
		} else {
			strServerid = serverId;
		}

		String strGender = "";

		if (gender == null) {
			strGender = "unknown";
		} else {
			strGender = gender.name();
		}

		SharedPreferences mysharedPreferences = my_context
				.getSharedPreferences("reyun_regInfo",
						ReYun.my_context.MODE_PRIVATE);
		Editor myEditor = mysharedPreferences.edit();
		myEditor.putString("accountid", accountid);
		myEditor.putString("accountType", strAccountType);
		myEditor.putString("gender", strGender);
		myEditor.putString("age", age + "");
		myEditor.putString("serverid", strServerid);
		myEditor.commit();

		final RequestParaExd mydata = getUserRegisterData(accountid,
				strAccountType, strGender, age, strServerid, my_context);
		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== register ");
				addRecordToDbase("register", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(TAG,
						"==============SEND SUCCESS ========== register :"
								+ arg1.toString());
			}
		};
		httpnetwork.postJson(my_context, "register", mydata, myJsonRespHandler);
	}

	/**
	 * 调用时机： 在用户成功登录返回后调用,同 setLoginWithAccountID 两者不可兼容，只可调用一种。
	 * 
	 * @param accountid
	 *            账号id 必须正确提供
	 * @param serverid
	 *            服务器 id
	 * @param level
	 *            玩家等级
	 * 
	 */
	public static boolean setLoginSuccessBusiness(final String accountId,
			final int level, final String serverid) {

		if (checkAppid(my_context) == false) {
			return false;
		}
		String strAccountid = "";
		if (accountId == null || accountId.equals("")) {
			strAccountid = "visitors";
		} else {
			strAccountid = accountId;
		}

		String strLevel = "";

		if (level <= 0) {
			strLevel = "-1";
		} else {
			strLevel = level + "";
		}

		my_level = level;

		String strServerId = "unknown";
		if (serverid == null || serverid.equals("")
				|| serverid.trim().length() == 0) {
			// strServerId = "unknown";
		} else {
			strServerId = serverid;
		}

		SharedPreferences mysharedPreferences = my_context
				.getSharedPreferences("reyun_loginInfo",
						my_context.MODE_PRIVATE);
		Editor myEditor = mysharedPreferences.edit();
		myEditor.clear();
		myEditor.putString("accountid", strAccountid);
		myEditor.putString("serverid", strServerId);
		myEditor.putString("level", strLevel);
		myEditor.commit();

		/**
		 * 监听按下 home 按键事件
		 */
		if (Integer.parseInt(Build.VERSION.SDK) >= 14) {
			m_catchHomeBtnThread = new CatchHomeBtnThread();
			m_catchHomeBtnThread.setDaemon(true);
			m_catchHomeBtnThread.start();
		} else {
			sdkListenerHomeBtn();
		}

		mScreenObserver = new ScreenObserver(my_context);
		mScreenObserver.requestScreenStateUpdate(new ScreenStateListener() {

			@Override
			public void onScreenUnlock() {
				// TODO Auto-generated method stub
				CommonUtil.printLog(TAG, "=======onScreenUnlock======");
				if (isAppOnForeground()) {
					startHeartBeat(my_context);
				}
			}

			@Override
			public void onScreenOn() {
				// TODO Auto-generated method stub
				CommonUtil.printLog(TAG, "=======onScreenOn======");
			}

			@Override
			public void onScreenOff() {
				// TODO Auto-generated method stub
				CommonUtil.printLog(TAG, "=======onScreenOff======");
				if (isAppOnForeground()) {
					stopHeartBeat();
				}
			}
		});

		/**
		 * 调用时机：在用户成功登录后（开发者调用loggedin方法后）每30秒调用一次
		 */
		startHeartBeat(my_context);
		return true;
	}

	/**
	 * 调用时机： 在用户成功登录返回后调用
	 * 
	 * @param accountid
	 *            账号id 必须正确提供
	 * @param serverid
	 *            服务器 id
	 * @param level
	 *            玩家等级
	 * @param gender
	 *            性别 ， M ：男性 F 女性
	 * @param age
	 *            年龄
	 */
	public static void setLoginWithAccountID(final String accountId,
			final int level, final String serverid) {

		boolean is_success = setLoginSuccessBusiness(accountId, level, serverid);

		if (!is_success) {

			return;
		}

		String strAccountid = "";
		if (accountId == null || accountId.equals("")) {
			strAccountid = "visitors";
		} else {
			strAccountid = accountId;
		}

		String strLevel = "";

		if (level <= 0) {
			strLevel = "-1";
		} else {
			strLevel = level + "";
		}

		my_level = level;

		String strServerId = "unknown";
		if (serverid == null || serverid.equals("")
				|| serverid.trim().length() == 0) {
			// strServerId = "unknown";
		} else {
			strServerId = serverid;
		}

		final RequestParaExd mydata = getUserLoginData(strAccountid,
				strServerId, strLevel, my_context);
		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== login ");
				addRecordToDbase("login", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(
						TAG,
						"==============SEND SUCCESS ========== login :"
								+ arg1.toString());
			}
		};

		httpnetwork.postJson(my_context, "loggedin", mydata, myJsonRespHandler);

	}

	/**
	 * 启动心跳侦测
	 */
	public static void startHeartBeat(Context context) {
		my_context = context;

		stopHeartBeat();

		if (m_heartBeatTimer == null) {
			m_heartBeatTimer = new Timer(true);
		}

		if (my_timerTask == null) {

			my_timerTask = new TimerTask() {

				@Override
				public void run() {
					CommonUtil.printLog(TAG, "=============跳===========");
					// TODO Auto-generated method stub
					SharedPreferences mysharedPreferences = my_context
							.getSharedPreferences("reyun_loginInfo",
									my_context.MODE_PRIVATE);
					String accountid = mysharedPreferences.getString(
							"accountid", "unknown");
					String serverid = mysharedPreferences.getString("serverid",
							"unknown");

					final RequestParaExd mapdata = getUserHearBeatData(
							accountid, serverid, my_context);

					new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							BasicHttpParams httpParams = new BasicHttpParams();
							HttpConnectionParams.setConnectionTimeout(
									httpParams, ReYunConst.REQUEST_TIMEOUT);
							HttpConnectionParams.setSoTimeout(httpParams,
									ReYunConst.SO_TIMEOUT);
							HttpPost request = new HttpPost(ReYunConst.BASE_URL
									+ "receive/rest/heartbeat");
							// List<NameValuePair> list = new
							// ArrayList<NameValuePair>();

							if (CommonUtil.isWapConnected(my_context) == true) {

								/**
								 * ============ code for wap connect setting
								 * ===========
								 */
								String proxyHost = android.net.Proxy
										.getDefaultHost();
								int proxyPort = android.net.Proxy
										.getDefaultPort();
								HttpHost proxy = new HttpHost(proxyHost,
										proxyPort);
								httpParams.setParameter(
										ConnRouteParams.DEFAULT_PROXY, proxy);

								HttpConnectionParams.setConnectionTimeout(
										httpParams,
										ReYunConst.WAP_REQUEST_TIMEOUT);
								HttpConnectionParams.setSoTimeout(httpParams,
										ReYunConst.WAP_SO_TIMEOUT);
							}

							// for( Iterator it = mapdata.keySet().iterator();
							// it.hasNext();){
							//
							// String key = it.next().toString();
							// list.add(new BasicNameValuePair(key,
							// mapdata.get(key)));
							// }

							HttpEntity httpEntity = null;
							try {
								httpEntity = new StringEntity(mapdata
										.toString(), "UTF-8");
							} catch (UnsupportedEncodingException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							HttpClient httpClient = new DefaultHttpClient(
									httpParams);

							try {
								// httpEntity = new UrlEncodedFormEntity(list,
								// "UTF-8");
								request.addHeader("content-type",
										"application/json");
								request.setEntity(httpEntity);
								CommonUtil.printLog(
										TAG_NETWORK,
										"=======request params is ======"
												+ EntityUtils
														.toString(httpEntity));

								HttpResponse response = httpClient
										.execute(request);
								if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

									String str = EntityUtils.toString(response
											.getEntity());
									CommonUtil.printLog(TAG,
											"==============SEND HB SUCCESS =========="
													+ str);
								} else {
									CommonUtil.printErrLog(TAG,
											"==============SEND HB FAILED ========== Hb \n"
													+ response.getStatusLine()
															.getReasonPhrase());
								}
							} catch (ClientProtocolException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								httpClient.getConnectionManager().shutdown();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								httpClient.getConnectionManager().shutdown();
							} finally {
								httpClient.getConnectionManager().shutdown();
							}
						}
					}).start();

					/**
					 * 发送数据库失败的数据
					 */
					sendFailureRecord(10);
				}
			};
		}

		if (m_heartBeatTimer != null && my_timerTask != null) {

			m_heartBeatTimer.schedule(my_timerTask, 1000, HEART_BEAT_TIME);
		}
	}

	/**
	 * 停止心跳
	 */
	private static void stopHeartBeat() {
		CommonUtil.printLog(TAG, "=============停下来了===========");
		if (m_heartBeatTimer != null) {
			m_heartBeatTimer.cancel();
			m_heartBeatTimer = null;
		}

		if (my_timerTask != null) {
			my_timerTask.cancel();
			my_timerTask = null;
		}
	}

	/**
	 * 调用时机：用户操作事件发生之后调用
	 * 
	 * @param eventname
	 *            事件名称
	 * @param extra
	 *            用户指定的附加参数 为 map 键值对类型,可为空
	 * @return
	 */
	public static void setEvent(final String eventName, Map extra) {

		if (checkAppid(my_context) == false) {

			return;
		}

		if (eventName == null || eventName.trim().length() == 0) {
			return;
		}

		final RequestParaExd mydata = getUserEventData(eventName, extra,
				my_context);

		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== eventName :"
								+ eventName);
				addRecordToDbase("userEvent", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(TAG,
						"==============SEND SUCCESS ========== eventName :"
								+ eventName + ":" + arg1.toString());
			}
		};
		httpnetwork.postJson(my_context, "event", mydata, myJsonRespHandler);
	}

	/**
	 * 调用时机：用户充值成功之后调用
	 * 
	 * @param transactionId
	 *            交易的流水号
	 * @param paymentType
	 *            支付类型，例如支付宝，银联，苹果、谷歌官方等 系统赠送的，paymentType为：FREE
	 * @param currencyType
	 *            货币类型
	 * @param currencyAmount
	 *            真实货币的金额
	 * @param virtualCoinAmount
	 *            充值游戏内货币的数量
	 * @param iapName
	 *            游戏内购买道具的名称
	 * @param iapAmount
	 *            游戏内购买道具的数量
	 * @param level
	 *            玩家游戏等级
	 */
	public static void setPayment(String transactionId, String paymentType,
			String currencyType, float currencyAmount, float virtualCoinAmount,
			String iapName, long iapAmount, int level) {

		if (checkAppid(my_context) == false) {

			return;
		}
		my_level = level;

		if (transactionId == null || transactionId.trim().length() == 0
				|| paymentType == null || paymentType.trim().length() == 0
				|| currencyType == null || currencyType.trim().length() == 0
				|| iapName == null || iapName.trim().length() == 0) {

			return;
		}

		final RequestParaExd mydata = getUserPaymentData(transactionId,
				paymentType, currencyType, currencyAmount, virtualCoinAmount,
				iapName, iapAmount, level, my_context);

		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== payment");
				addRecordToDbase("payment", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(
						TAG,
						"==============SEND SUCCESS ========== payment"
								+ arg1.toString());
			}
		};

		httpnetwork.postJson(my_context, "payment", mydata, myJsonRespHandler);

	}

	/**
	 * 调用时机： 虚拟物品等交易发生之后调用
	 * 
	 * @param itemName
	 *            游戏内虚拟物品的名称/ID，最长64个字符
	 * @param itemAmount
	 *            交易的数量，最长16个字符
	 * @param itemTotalPrice
	 *            交易的总价，最长16个字符
	 * @param level
	 *            玩家游戏等级
	 */
	public static void setEconomy(String itemName, long itemAmount,
			float itemTotalPrice, int level) {

		if (checkAppid(my_context) == false) {

			return;
		}
		if (itemName == null || itemName.trim().length() == 0) {
			return;
		}

		final RequestParaExd mydata = getUserEconomyData(itemName, level,
				itemAmount, itemTotalPrice, my_context);

		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== economy");
				addRecordToDbase("economy", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(
						TAG,
						"==============SEND SUCCESS ========== economy "
								+ arg1.toString());
			}
		};

		httpnetwork.postJson(my_context, "economy", mydata, myJsonRespHandler);
	}

	/**
	 * 调用时机：用户接受任务/用户完成任务之后
	 * 
	 * @param questId
	 *            当前任务/关卡/副本的编号或名称，最长32个字符
	 * @param status
	 *            当前任务/关卡/副本的状态，有如下三种类型：
	 * @param questType
	 *            当前任务/关卡/副本的类型
	 * @param level
	 */
	public static void setQuest(String questId, QuestStatus status,
			String questType, int level) {

		if (checkAppid(my_context) == false) {

			return;
		}

		if (questId == null || questId.trim().length() == 0 || status == null
				|| questType == null || questType.trim().length() == 0) {

			return;
		}
		my_level = level;

		final RequestParaExd mydata = getUserTaskData(questId, questType,
				status, level, my_context);

		JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

			@Override
			public void onFailure(Throwable exception, String responseBody) {
				// TODO Auto-generated method stub
				super.onFailure(exception, responseBody);

				CommonUtil.printErrLog(TAG,
						"==============SEND FAILED ========== task");
				addRecordToDbase("task", mydata);
			}

			@Override
			public void onSuccess(int arg0, JSONObject arg1) {
				// TODO Auto-generated method stub
				super.onSuccess(arg0, arg1);

				CommonUtil.printLog(
						TAG,
						"==============SEND SUCCESS ========== task "
								+ arg1.toString());
			}
		};

		httpnetwork.postJson(my_context, "quest", mydata, myJsonRespHandler);
	}

	// ================================================具体实现内部调用===================================================================//

	private static void addRecordToDbase(String what, RequestParaExd record) {

		// byte[] byteDataArr = objToByteArray(record);
		byte[] byteDataArr = jsonObjToByteArray(record);
		SqliteDbaseUtil.getInstance(my_context).openDataBase();

		SqliteDbaseUtil.getInstance(my_context).insertOneRecordToTable(what,
				byteDataArr);

		/************************ test code *************************************/
		// int i = 20;
		// while(i >0 ){
		// SqliteDbaseUtil.getInstance(my_context).insertOneRecordToTable(what,
		// byteDataArr);
		// i--;
		// }
	}

	private static void sendFailureRecord(final int record_count) {

		SqliteDbaseUtil.getInstance(my_context).openDataBase();
		/**
		 * 旧的方式发送cache 数据
		 */
		// SqliteDbaseUtil.getInstance(my_context).queryAndSendAllRecordFromDb();

		/**
		 * 新的方式发送cache 数据
		 */
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				String mydata = SqliteDbaseUtil.getInstance(my_context)
						.queryRecordsByCount(record_count);
				mydbhandler.sendMessage(mydbhandler.obtainMessage(1,
						record_count, 0, mydata));
			}
		}).start();

	}

	/**
	 * 对象转数组
	 * 
	 * @param obj
	 * @return
	 */
	private static byte[] objToByteArray(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			bytes = bos.toByteArray();
			oos.close();
			bos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return bytes;
	}

	/**
	 * JSONobject 转byte 数组
	 * 
	 * @param obj
	 * @return
	 */
	private static byte[] jsonObjToByteArray(JSONObject obj) {
		byte[] bytes = null;
		if (obj != null) {

			try {
				bytes = obj.toString().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return bytes;
	}

	private static boolean checkAppid(Context context) {

		if (m_appid == null) {

			if (ReYunConst.DebugMode) {
				Log.e(TAG, "========appid is null=====");
			}

			return false;
		} else {
			if (m_appid.length() >= 32 && m_appid.length() <= 64) {

				return true;
			} else {

				if (ReYunConst.DebugMode) {
					Log.e(TAG, "========appid is uncorrected=====");
				}
				return false;
			}
		}

	}

	// 安装事件
	private static RequestParaExd getUserInstallData(Context context)
			throws JSONException {

		RequestParaExd params = new RequestParaExd();

		params.put("appid", m_appid);
		params.put("what", "install");
		params.put("when", CommonUtil.getTime(interval));

		JSONObject dev = new JSONObject();
		dev.put("deviceid", CommonUtil.getDeviceID(context));
		dev.put("serverid", "unknown");
		dev.put("channelid", m_channelid);
		params.put("context", dev);

		return params;
	}

	// 每次启动 app 事件
	private static RequestParaExd getUserStartUpData(final Context context)
			throws JSONException {

		RequestParaExd params = new RequestParaExd();
		params.put("appid", m_appid);
		params.put("what", "startup");
		params.put("when", CommonUtil.getTime(interval));

		JSONObject contextData = new JSONObject();
		contextData.put("deviceid", CommonUtil.getDeviceID(context));
		contextData.put("serverid", "unknown");
		contextData.put("channelid", m_channelid);
		contextData.put("tz", CommonUtil.getTimeZone());
		contextData.put("devicetype", Build.MANUFACTURER + "|" + Build.BRAND
				+ "|" + Build.MODEL);
		contextData.put("op", CommonUtil.getOperatorName(context));
		contextData.put("network", CommonUtil.getConnectType(context));

		String os = Build.VERSION.RELEASE;
		if (os == null)
			os = "unknown";
		else
			os = "Android " + Build.VERSION.RELEASE;

		contextData.put("os", os);

		contextData.put("resolution", CommonUtil.getPhoneResolution(context));

		params.put("context", contextData);

		return params;

	}

	// 注册事件 需要用户传递数据

	private static RequestParaExd getUserRegisterData(String accountid,
			String accountType, String gender, int age, String serverid,
			Context context) {

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", "" + accountid);
			params.put("what", "register");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));

			String my_serverId = serverid;
			if (serverid == null)
				serverid = "unknown";

			contextData.put("serverid", my_serverId);
			contextData.put("channelid", m_channelid);

			contextData.put("accounttype", accountType);
			contextData.put("gender", gender);
			contextData.put("age", age + "");

			params.put("context", contextData);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;

	}

	// 成功登录事件
	/**
	 * @param accountid
	 *            账号id 必须正确提供
	 * @param serverid
	 *            服务器id
	 * @param level
	 *            游戏等级
	 * @param gender
	 *            性别
	 * @param age
	 *            生日
	 * @param context
	 *            上下文
	 * @return
	 */
	private static RequestParaExd getUserLoginData(String accountid,
			String serverid, String level, final Context context) {

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", "" + accountid);
			params.put("what", "loggedin");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));

			String my_serverId = serverid;
			if (serverid == null)
				serverid = "unknown";

			contextData.put("serverid", my_serverId);
			contextData.put("channelid", m_channelid);
			contextData.put("level", level);

			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;

	}

	// 成功登陆 app 后 日常侦测事件（每30 秒调用一次）
	private static RequestParaExd getUserHearBeatData(String accountid,
			String serverid, final Context context) {

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", "" + accountid);
			params.put("what", "heartbeat");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));

			String my_serverId = serverid;
			if (serverid == null)
				serverid = "unknown";

			contextData.put("serverid", my_serverId);
			contextData.put("channelid", m_channelid);
			contextData.put("level", my_level + "");

			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;

	}

	// 调用时机：用户操作事件发生之后调用
	private static RequestParaExd getUserEventData(String eventname, Map extra,
			final Context context) {

		SharedPreferences mysharedPreferences = context.getSharedPreferences(
				"reyun_loginInfo", context.MODE_PRIVATE);
		String accountid = mysharedPreferences
				.getString("accountid", "unknown");
		String serverid = mysharedPreferences.getString("serverid", "unknown");

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", accountid);
			params.put("what", eventname);
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));

			String my_serverId = serverid;
			if (my_serverId == null)
				my_serverId = "unknown";

			contextData.put("serverid", my_serverId);
			contextData.put("channelid", m_channelid);

			String myExtras = "";
			if (extra != null) {

				Iterator it = extra.entrySet().iterator();
				while (it.hasNext()) {

					Map.Entry pEntry = (Map.Entry) it.next();
					contextData.put(pEntry.getKey().toString(), pEntry
							.getValue().toString());
				}

			}

			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;

	}

	// 用户支付操作事件跟踪,需要用户提供数据
	private static RequestParaExd getUserPaymentData(String transactionId,
			String paymentType, String currencyType, float currencyAmount,
			float virtualCoinAmount, String iapName, long iapAmount,
			int intlevel, final Context context) {

		SharedPreferences mysharedPreferences = context.getSharedPreferences(
				"reyun_loginInfo", context.MODE_PRIVATE);
		String accountid = mysharedPreferences
				.getString("accountid", "unknown");
		String serverid = mysharedPreferences.getString("serverid", "unknown");

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", accountid);
			params.put("what", "payment");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));

			String my_serverId = serverid;
			if (serverid == null)
				serverid = "unknown";

			contextData.put("serverid", my_serverId);
			contextData.put("channelid", m_channelid);
			contextData.put("level", intlevel + "");

			contextData.put("transactionid", transactionId);
			contextData.put("paymenttype", paymentType);
			contextData.put("currencytype", currencyType);
			contextData.put("currencyamount", currencyAmount + "");
			contextData.put("virtualcoinamount", virtualCoinAmount + "");
			contextData.put("iapname", iapName);
			contextData.put("iapamount", iapAmount + "");

			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;

	}

	// 调用时机： 虚拟物品等交易发生之后调用
	private static RequestParaExd getUserEconomyData(String name, int level,
			long num, float totalprice, final Context context) {

		SharedPreferences mysharedPreferences = context.getSharedPreferences(
				"reyun_loginInfo", context.MODE_PRIVATE);
		String accountid = mysharedPreferences
				.getString("accountid", "unknown");
		String serverid = mysharedPreferences.getString("serverid", "unknown");
		String strlevel = "";

		if (level <= 0) {
			strlevel = "-1";
		} else {
			strlevel = level + "";
		}

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", accountid);
			params.put("what", "economy");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));
			contextData.put("serverid", serverid);
			contextData.put("channelid", m_channelid);
			contextData.put("level", strlevel + "");
			contextData.put("itemname", name);
			contextData.put("itemamount", num + "");
			contextData.put("itemtotalprice", totalprice + "");
			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;
	}

	// 调用时机：
	private static RequestParaExd getUserTaskData(String taskId,
			String taskType, QuestStatus taskState, int level,
			final Context context) {

		SharedPreferences mysharedPreferences = context.getSharedPreferences(
				"reyun_loginInfo", context.MODE_PRIVATE);
		String accountid = mysharedPreferences
				.getString("accountid", "unknown");
		String serverid = mysharedPreferences.getString("serverid", "unknown");

		String strlevel = level + "";

		if (level <= 0) {
			strlevel = "-1";
		}

		if (taskId == null || taskId.equals("")) {
			taskId = "unknown";
		}

		String t_state = "";
		if (taskState != null) {
			t_state = taskState.name();
		}

		RequestParaExd params = new RequestParaExd();
		try {
			params.put("appid", m_appid);
			params.put("who", accountid);
			params.put("what", "quest");
			params.put("when", CommonUtil.getTime(interval));

			JSONObject contextData = new JSONObject();
			contextData.put("deviceid", CommonUtil.getDeviceID(context));
			contextData.put("serverid", serverid);
			contextData.put("channelid", m_channelid);
			contextData.put("level", strlevel);

			contextData.put("questId", taskId);
			contextData.put("queststatus", t_state);
			contextData.put("questtype", taskType);

			params.put("context", contextData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return params;
	}

	public static boolean isAppOnForeground() {

		if (my_context == null) {
			return false;
		}
		ActivityManager pActivityManager = (ActivityManager) my_context
				.getSystemService(Context.ACTIVITY_SERVICE);
		if (pActivityManager == null) {
			return false;
		}
		List<RunningAppProcessInfo> appProcesses = pActivityManager
				.getRunningAppProcesses();
		if (appProcesses == null)
			return false;

		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName == null) {

				if (ReYunConst.DebugMode == true) {

					Log.e(TAG, "appProcess.processName is null!");
				}
				return false;
			}
			if (my_context == null) {

				if (ReYunConst.DebugMode == true) {

					Log.e(TAG, "=====my_context is null!====");
				}
				return false;
			}
			// if (appProcess.processName.equals(my_context.getPackageName())
			// && appProcess.importance ==
			// RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
			// return true;
			// }
			if (appProcess.processName.equals(my_context.getPackageName())) {
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

					return true;
				} else {
					return false;
				}
			}
		}

		return false;
	}

	/**
	 * 监听 home 按键
	 */
	private static void sdkListenerHomeBtn() {

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		my_homeBtnReceiver = new HomeBtnBroadcastReceiver();
		my_context.registerReceiver(my_homeBtnReceiver, filter);
	}

	/**
	 * android 4.0 以下起作用 按下 home button 之后，保存当前游戏结束时间 session.等resume 时检测发送数据
	 */
	private static class HomeBtnBroadcastReceiver extends BroadcastReceiver {

		private String action = null;
		final String SYSTEM_HOME_KEY = "homekey";
		final String SYSTEM_RECENT_APPS = "recentapps";

		@Override
		public void onReceive(Context context, Intent intent) {
			action = intent.getAction();
			String dlg = Intent.ACTION_CLOSE_SYSTEM_DIALOGS;
			boolean isequal = dlg.equals(action);
			if (isequal && isAppOnForeground()) {

				String reason = intent.getStringExtra("reason");

				if (reason != null) {

					if (reason.equals(SYSTEM_HOME_KEY)) {

						CommonUtil.printLog(TAG,
								"=========== pressed home button ===========");

						stopHeartBeat();

					} else if (reason.equals(SYSTEM_RECENT_APPS)) {

						CommonUtil
								.printLog(TAG,
										"=========== long pressed home button ===========");

					}

				}

			}
		}
	}

	private static Handler myhandler = new Handler() {

		public void handleMessage(android.os.Message msg) {

			CommonUtil.printLog(TAG, "4.0 Home is Pressed+++++++++++++++++");

			stopHeartBeat();
		}
	};

	private static Handler mydbhandler = new Handler() {

		public void handleMessage(android.os.Message msg) {

			String mydata = (String) msg.obj;
			final int record_count = msg.arg1;
			if (mydata == null) {
				return;
			}

			RequestParaExd params = new RequestParaExd();
			try {
				params.put("appid", m_appid);
				params.put("data", new JSONArray(mydata));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

				@Override
				public void onFailure(Throwable exception, String responseBody) {
					// TODO Auto-generated method stub
					super.onFailure(exception, responseBody);
					CommonUtil
							.printLog(TAG,
									"############sendFailureRecord  failure ############ ");
				}

				@Override
				public void onSuccess(int arg0, JSONObject arg1) {
					// TODO Auto-generated method stub
					super.onSuccess(arg0, arg1);
					try {
						if (!arg1.isNull("status")
								&& arg1.getInt("status") == 0)
							SqliteDbaseUtil.getInstance(my_context)
									.delRecordsByCount(record_count);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					CommonUtil.printLog(TAG,
							"==============sendFailureRecord  SUCCESS =========="
									+ arg1.toString());
				}
			};

			// httpnetwork.post(my_context,"receive/receive", params,
			// myJsonRespHandler);
			httpnetwork.postBatchJson(my_context, "receive/batch", params,
					myJsonRespHandler);
		}
	};

	/**
	 * android 4.0 系统以上 监听 home 按键
	 */
	static class CatchHomeBtnThread extends Thread {

		private volatile boolean isThreadRun = true;

		@Override
		public void run() {

			synchronized (this) {

				// Process mLogcatProc = null;
				// BufferedReader reader = null;
				// String line;
				if (CommonUtil.checkPermissions(my_context,
						"android.permission.GET_TASKS") == false) {

					if (ReYunConst.DebugMode) {
						Log.e(TAG,
								"======== lost permission android.permission.GET_TASKS =========");
					}

				} else {
					while (isThreadRun) {

						try {
							sleep(500);

							if (!isAppOnForeground() && !isSdkExit) {
								// 发送监听home 键消息
								myhandler
										.sendMessage(myhandler.obtainMessage());

								try {
									wait();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									break;
								}
							}

						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							isThreadRun = false;
							break;
						}

					}
				}

			}
		}

		public void close() {

			this.isThreadRun = false;
		}

	};

	interface ScreenStateListener {
		public void onScreenOn();

		public void onScreenOff();

		public void onScreenUnlock();
	}

	static class ScreenObserver {

		private Context mContext;
		private ScreenBroadcastReceiver mScreenReceiver;
		private ScreenStateListener mScreenStateListener;

		public ScreenObserver(Context context) {
			mContext = context;
			mScreenReceiver = new ScreenBroadcastReceiver();
		}

		/*
		 * screen状态广播接收者
		 */

		private class ScreenBroadcastReceiver extends BroadcastReceiver {
			private String action = null;

			@Override
			public void onReceive(Context context, Intent intent) {
				action = intent.getAction();
				if (Intent.ACTION_SCREEN_ON.equals(action)) {
					mScreenStateListener.onScreenOn();
				} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
					mScreenStateListener.onScreenOff();
				} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
					mScreenStateListener.onScreenUnlock();
				}
			}
		}

		/**
		 * @param listener
		 */
		public void requestScreenStateUpdate(ScreenStateListener listener) {
			mScreenStateListener = listener;
			startScreenBroadcastReceiver();

		}

		/**
		 * 停止screen状态更新
		 */
		public void stopScreenStateUpdate() {// 取消注册
			mContext.unregisterReceiver(mScreenReceiver);
		}

		/**
		 * 启动screen状态广播接收器
		 */
		private void startScreenBroadcastReceiver() {// 启动屏幕注册
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			filter.addAction(Intent.ACTION_USER_PRESENT);
			mContext.registerReceiver(mScreenReceiver, filter);
		}

	};

}