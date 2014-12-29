add reyunchannel  & reyunsdk  Source Code
create version ：reyunchannel ->1.0  reyunsdk->1.0

接入方法说明：

1.	配置应用权限到 AndroidManifest.xml 文件
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission
android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission
android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_LOGS"/>
<uses-permission android:name="android.permission.GET_TASKS" />
2.	应用中所有 Activity 继承基类 BaseActivity。代码如下：

public class BaseActivity extends Activity {

	boolean isActive = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ReYunChannel.setContext(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		ReYunChannel.setContext(getApplicationContext());
		if (!isActive) {

			// ReYun.postSessionData();
			ReYunChannel.startHeartBeat(getApplicationContext());
			isActive = true;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		ReYunChannel.stopHeartBeat();
		if (ReYunChannel.isAppOnForeground() == false) {
			isActive = false;
		}
	}

}
注：如果在程序不继承了 BaseActivity，继承其他Activity也可以，仿照baseactivity 写就
行，但是得保证都继承一个activity ，代码里面所有 activity 都继承自一个基类 activity
，比如他们的 UnityActivity	

3.	使用sdk中的MyaPP类

  <application
        android:name="com.reyun.Application.MyApp"
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:theme="@style/AppTheme" >

4.	Manifest 中配置reyunKey

<meta-data
            android:name="com.reyun.KEY"
            android:value="8283e21a7484c03ed3f3d61cc12e93ed" />
5.	 初始化热云sdk
方法用途：
 用于在游戏启动后，初始化热云 SDK。报送游戏安装或启动事件
使用方法：
 调用 Reyun.initWithKeyAndChannelId(String appId,String
channelId) 方法进行初始化。
 参数 channelId 为渠道 ID ,如果传 null 或 “”则默认向热云报送为
unknown，如果设置了渠道 ID，则可以在游戏运营支撑平台中查询到不同
渠道的数据，对于不同的渠道包，需要分别设置不同的渠道 ID。需要重
新编译打包 apk
接口说明：
方法接口： 
		ReYunChannel.initWithKeyAndChanelId(Context appConetxt, String channel );
参数说明：

参数名	是否必填	参数类型	说明
appConetxt	是	Conetxt	上下文
Channel	是	String	

示例代码：
		ReYunChannel.initWithKeyAndChanelId(getApplicationContext(), "App43_1");
6.	统计玩家服务器注册数据
 用于在玩家首次进入某个服务器，用来统计这个服务器一个新的玩家账号
（账号需使用游戏的平台 id，这样方便统计用户滚服的信息）。如果是
单服游戏，那么在游戏内只需调用一次即可。多个服务器的游戏需要在首
次登服务器时调用一次且仅一次。
 玩家账号需要保证唯一性，否则会造成统计数据出错，作为统计使用的玩
家账号一般为游戏内给玩家生成的唯一 ID。
 当某个玩家用相同的账号在不同的设备上登录时，新增账号不会增加，但
设备激活会增加，但在相同设备上有多个不同账号登录时，新增账号会增
加，但设备激活不增加。
使用方法：

 在玩家首次进入某个服务器时调用 setRegisterWithAccountID 方法，具体
注册账号的时机和位置由开发者自己来决定，但要确保参数 accountId 不
能为空。
 如果开发者没有自己的用户系统，希望使用用户的设备 ID 来作为
accountId，直接调用 Reyun.getDeviceId()方法即可。
接口说明：
	方法接口： setRegisterWithAccountID(String accountId);
参数说明:
参数名	是否必填	参数类型	参数长度	说明
AccountId	是	字符串	最长64	游戏帐号


示例代码：

		ReYunChannel.setRegisterWithAccountID("liwei11121"); 
7.	统计玩家的账号登陆服务器数据
方法用途：
 统计玩家在游戏中的登陆、账号切换操作，游戏的每日 /周/月活跃人数、
登陆次数/频次、留存数据等重要数据均通过这个方法统计。
 每次登陆后 accountId、 serverId 信息缓存至本地，其他后续的方法调用使
用到这几个参数时，均使用本地缓存的值来报送。
 当玩家有登陆、切换账号操作时调用 setLoginWithAccountID 方法。
接口说明：
	方法接口： setLoginWithAccountID(String accountId);
参数说明：
参数名	是否必填	参数类型	参数长度	说明
AccountId	是	字符串	最长64	游戏帐号
示例代码：
		ReYunChannel.setLoginSuccessBusiness("liwei11121");
	
8.	 统计玩家的充值数据
方法用途：
 在玩家在游戏中充值成功后，进行统计充值数据，所有付费相关分析的数据报表均依赖此方法。
 当系统通过不同形式赠送虚拟货币给玩家时，调用此方法来记录系统赠送的虚拟币数据，当 paymentType 为 FREE 时，系统认为是系统赠送的，不会计算到当天的收入中。
使用方法：
 在玩家充值成功后调用 setPayment 方法。
接口说明：
		方法接口：setPayment(String transactionid,
			PaymentType paymenttype, CurrencyType currencytype,
			float currencyamount)
参数名	是否必填	参数类型	参数长度	说明
Transactionid	是	字符串	最长64	流水号
PaymentType	是	枚举类型	-	支付类型
currencytype	是	枚举类型	-	货币类型
currencyamount	是	浮点型	最长16	支付的真实货
币的金额
示例代码：

ReYunChannel.setPayment("liushuihao", PaymentType.APPLE,
				CurrencyType.CNY, 1200);


9.	 统计玩家的自定义事件
方法用途：
 开发者可以自由的统计玩家在游戏内的任意用户行为，例如打开某个面板，点击某个 Button，参与某个活动等。
 eventName 只能是合法的字符，包括中文、英文字母、数字和下划线。
 开发者调用此方法后，即可在热云的游戏运营支撑平台使用多维分析的强大功能。
使用方法：
 在您希望进行用户行为统计的地方，调用 setEvent 方法。
 除了开发者可以自定义 eventName 以外，还可以通过 extra 参数以字典的格式来记录更多信息，例如一款德州 Poker 游戏，定义了 eventName 为一盘结束，那么您可以同时记录牌型、下注次数、同桌牌友、输赢筹码等数据。
 自定义事件上限为十个种类。
接口说明：
	方法接口：setEvent(final String eventName);
	参数说明：
参数名	是否必填	参数类型	说明
EventName	是	字符串	自定义事件的名称

示例代码：

		ReYunChannel.setEvent("cesh1i");
	
10.	  退出 sdk
方法用途：应用退出时释放 sdk 占用资源。
使用方法：
 在程序退出时候调用 exitSdk 方法。
接口说明：
	方法接口： exitSdk();
	参数说明：无参数
示例代码：
	ReYunChannel.exitSdk();

