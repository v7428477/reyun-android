/**
 * 
 */
package com.reyun.common;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author changxianli
 *
 */
public class ReYunDbHelper extends SQLiteOpenHelper {
	
	
	public ReYunDbHelper(Context context){
		
		super(context, ReYunSqlConst.DATABASENAME, null, ReYunSqlConst.DATABASE_VERSION);
	}

	/* 
	 * �������ݿ�󣬶����ݿ�Ĳ���
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub

	}

	/* 
	 * �������ݿ�汾�Ĳ��� 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	/*
	 * ÿ�γɹ������ݿ�����ȱ�ִ��
	 */
	@Override
	public void onOpen(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		super.onOpen(db);
	}
	
	

}
