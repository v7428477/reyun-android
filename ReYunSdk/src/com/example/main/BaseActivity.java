package com.example.main;

import com.reyun.sdk.ReYun;

import android.app.Activity;
import android.os.Bundle;

public class BaseActivity extends Activity {

	boolean isActive = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		
		if (!isActive) { // app �Ӻ�̨���ѵ�ǰ̨

			// ReYun.postSessionData();
			ReYun.startHeartBeat(getApplicationContext());
			isActive = true;
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (ReYun.isAppOnForeground() == false) {

			// app �����̨
			isActive = false;
		}
	}

}
