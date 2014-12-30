package com.example.main;

import android.os.Bundle;
import android.view.Menu;

import com.reyun.common.ReYunConst;
import com.reyun.sdk.ReYun;
import com.sdk.reyunsdk.R;

public class MainActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ReYunConst.DebugMode = true;
		setContentView(R.layout.activity_main);
//		InstallEvent  install = new InstallEvent();
//		install.setAppid("&vswew#");
//		RequestParams  params = new RequestParams()
//		httpnetwork.post(null, params, responseHandler);
		
		try {
			ReYun.initWithKeyAndChannelId(getApplicationContext(), "8283e21a7484c03ed3f3d61cc12e93ed", "moboge");
//			ReYun.setRegisterWithAccountID("123456", AccountType.ANONYMOUS.name(), Gender.F, 18, "");
//			ReYun.setEconomy("盘古石", 10, 200, 16);
//			ReYun.setQuest("", QuestStatus.a, "", 0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ReYun.setLoginWithAccountID("465458", 30, "xiaoaojianghu");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		System.exit(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	

}
