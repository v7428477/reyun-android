package com.reyun.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.http.network.httpnetwork;
import com.reyunloopj.JsonHttpResponseHandler;
import com.reyunloopj.RequestParams;

public class SqliteDbaseUtil {

	private Context sqliteContext;
	private ReYunDbHelper dbHelper;
	private static SqliteDbaseUtil mSqliteUtilInstance = null;
	private SQLiteDatabase mSqliteDB = null;
	private final String TAG = "sqlite";
	private ArrayList my_list;

	public synchronized static SqliteDbaseUtil getInstance(Context context) {

		if (mSqliteUtilInstance == null) {

			mSqliteUtilInstance = new SqliteDbaseUtil(context);
		}

		return mSqliteUtilInstance;
	}

	private SqliteDbaseUtil() {

	}

	private SqliteDbaseUtil(Context context) {

		this.sqliteContext = context;
	}

	public boolean openDataBase() {

		if (mSqliteDB == null) {
			dbHelper = new ReYunDbHelper(sqliteContext);
			mSqliteDB = dbHelper.getWritableDatabase();
			mSqliteDB
					.execSQL("CREATE TABLE IF NOT EXISTS "
							+ ReYunSqlConst.TABLE_NAME
							+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT, what char, value BLOB);");
		}
		return (mSqliteDB == null) ? false : true;
	}

	public void closeDataBase() {
		if (mSqliteDB != null) {

			mSqliteDB.close();
			mSqliteDB = null;
		}
	}

	/**
	 * ��ݿ� ���һ����¼
	 */
	public void insertOneRecordToTable(String what, byte[] requestParams) {

		ContentValues cValues = new ContentValues();
		cValues.put("what", what);
		cValues.put("value", requestParams);

		mSqliteDB.insert(ReYunSqlConst.TABLE_NAME, null, cValues);
		if (ReYunConst.DebugMode) {
			Log.e(TAG, "====data base insert OneRecord  ==:" + what);
		}

	}

	/**
	 * ��ݿ�ɾ��һ����¼
	 */
	private void delOneRecord(int id) {
		String sql = "delete from " + ReYunSqlConst.TABLE_NAME
				+ " where _id  = " + id;
		mSqliteDB.execSQL(sql);
		if (ReYunConst.DebugMode) {
			Log.e(TAG, "=== delete OneRecord  == id :" + id);
		}
	}

	/**
	 * ��ѯ��ݿ�ǰ�����м�¼,�����ز�ѯ���ļ�¼
	 */
	public String queryRecordsByCount(int count) {

		// Cursor mycursor = mSqliteDB.rawQuery("SELECT * FROM "
		// + ReYunSqlConst.TABLE_NAME + " order by _id asc limit ?",
		// new String[]{String.valueOf(count)});
		/** ������ ******/
		Cursor mycursor = null;
		mSqliteDB.beginTransaction();
		try {
			mycursor = mSqliteDB.rawQuery("SELECT * FROM "
					+ ReYunSqlConst.TABLE_NAME + " limit ?",
					new String[] { String.valueOf(count) });
			mSqliteDB.setTransactionSuccessful();
		} catch (Exception e) {
			// TODO: handle exception
			mSqliteDB.endTransaction();
		} finally {
			mSqliteDB.endTransaction();
		}

		if (ReYunConst.DebugMode) {
			Log.e(TAG,
					"====database queryRecords count is ======"
							+ mycursor.getCount());
		}
		String mydata = null;
		if (mycursor != null) {

			if (mycursor.getCount() > 0) {

				my_list = new ArrayList<String>();

				JSONArray array = new JSONArray();
				while (mycursor.moveToNext()) {
					final int id = mycursor.getInt(mycursor
							.getColumnIndex("_id"));
					String name = mycursor.getString(mycursor
							.getColumnIndex("what"));
					byte[] value = mycursor.getBlob(mycursor
							.getColumnIndex("value"));

					my_list.add(id + "");
					JSONObject object = byteArrayToJsonObj(value);
					// RequestParaExd object = (RequestParaExd)
					// byteArrayToObject(value);

					CommonUtil.printErrLog(TAG,
							"====query failed record row id is ======" + id);

					array.put(object);
				}
				mydata = array.toString();
			}

			mycursor.close();
		}

		return mydata;
	}

	/**
	 * ɾ����ݿ�ǰ�����м�¼
	 */
	public void delRecordsByCount(int count) {

		mSqliteDB.beginTransaction();

		try {

			// ����д����ݲ���
			if (my_list != null) {
				for (int index = 0; index < my_list.size(); index++) {

					String sql = "delete from " + ReYunSqlConst.TABLE_NAME
							+ " where _id = " + my_list.get(index);
					mSqliteDB.execSQL(sql);
				}
			}

			mSqliteDB.setTransactionSuccessful();

			// ����������ɹ��������û��Զ��ع����ύ

		} catch (Exception e) {

		} finally {

			mSqliteDB.endTransaction(); // �ύ

		}

		if (ReYunConst.DebugMode) {
			Log.e(TAG, "=== delete records count  == count :" + count);
		}

	}

	/**
	 * ��ѯ������ȫ����ݣ�ĳһ����¼���ͳɹ���ɾ��ü�¼
	 * 
	 * @return
	 */
	public Cursor queryAndSendAllRecordFromDb() {

		Cursor mycursor = mSqliteDB.rawQuery("SELECT * FROM "
				+ ReYunSqlConst.TABLE_NAME, null);

		if (ReYunConst.DebugMode) {
			Log.e(TAG,
					"====data base records count is ======"
							+ mycursor.getCount());
		}

		if (mycursor.getCount() > 0) {

			while (mycursor.moveToNext()) {
				final int id = mycursor.getInt(mycursor.getColumnIndex("_id"));
				String name = mycursor.getString(mycursor
						.getColumnIndex("what"));
				byte[] value = mycursor.getBlob(mycursor
						.getColumnIndex("value"));
				RequestParaExd object = (RequestParaExd) byteArrayToObject(value);

				if (ReYunConst.DebugMode) {
					Log.e(TAG, "====sending row id is ======" + id);
					// Log.e(TAG, "====sending row name is ======" +name);
					Log.e(TAG, "=======================================");

				}

				JsonHttpResponseHandler myJsonRespHandler = new JsonHttpResponseHandler() {

					@Override
					public void onFailure(Throwable exception,
							String responseBody) {
						// TODO Auto-generated method stub
						super.onFailure(exception, responseBody);
					}

					@Override
					public void onSuccess(int arg0, JSONObject arg1) {
						// TODO Auto-generated method stub
						super.onSuccess(arg0, arg1);

						if (arg1.isNull("what") == false) {
							String what = null;
							try {
								what = arg1.getString("what");
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							CommonUtil.printLog(TAG,
									"==============SEND SUCCESS ========== what :"
											+ what);

							delOneRecord(id);

						}
					}
				};

				httpnetwork.post(sqliteContext, "receive/receive",
						new RequestParams(object), myJsonRespHandler);
			}

		}
		mycursor.close();
		return mycursor;
	}

	private Object byteArrayToObject(byte[] bytes) {
		Object obj = null;

		if (bytes == null) {
			return null;
		}
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bis);
			obj = ois.readObject();
			ois.close();
			bis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return obj;
	}

	/**
	 * byte[] ����ת JsonObject ����
	 * 
	 * @param bytes
	 * @return
	 */
	private JSONObject byteArrayToJsonObj(byte[] bytes) {
		JSONObject jsonobj = null;

		if (bytes != null) {
			InputStream inputstream = new ByteArrayInputStream(bytes);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					inputstream), 8 * 1024);
			StringBuffer json_buffer = new StringBuffer();
			String line = "";
			try {
				while ((line = in.readLine()) != null) {
					json_buffer.append(line);
				}

				jsonobj = new JSONObject(json_buffer.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return jsonobj;
	}

}
