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
	 * 创建数据库后，对数据库的操作
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub

	}

	/* 
	 * 更改数据库版本的操作 
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	/*
	 * 每次成功打开数据库后首先被执行
	 */
	@Override
	public void onOpen(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		super.onOpen(db);
	}
	
	

}
