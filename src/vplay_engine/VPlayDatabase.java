package vplay_engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//����� ��� ������ � ����� ������
public class VPlayDatabase
{
	
	/***** ���������� � ��	*****/
	
	//�������� ����������
	private final static String DATABASE_NAME = "vplay_rfid_database";		//������������ ��
	private final static int DATABASE_VERSION = 1;							//������ ��
	
	//������������ ����� ������� DEVICE_IMAGE_TABLE
	private final static String IMAGE_ID = "id";			//�� ����������� (�������������� � ��������������� ������ � ���������)
	private final static String IMAGE_NAME = "name";		//������������ ����� � ������������ ��� ������
	
	//������������ ����� ������� DEVICE_TABLE
	private final static String DEVICE_ID = "id";				//�� ���������� � �������
	private final static String DEVICE_SET = "suit";			//�������� ������
	private final static String DEVICE_NUMBER = "number";		//����� ������ � ���������
	private final static String DEVICE_ACTIVITY = "activity";	//���������� ������
		
	//������������ ������ � ��
	private final static String DEVICE_IMAGE_TABLE = "device_image_table";				//������� ���� �����������, ������� ������ �������������� � �������� (���� ����� ������ � ��������� = 2, �� ��� ����� ������������ ����������� �� ���� ������� � �� = 2)
	private final static String DEVICE_TABLE = "device_table";							//������� ������������������ ���������
	private final static String CARD_GROUP_TABLE = "card_group_table";					//������� ����� ��������
	private final static String CARD_CONTENT_TABLE = "card_content_table";				//������� ����������� ����� ��������
	private final static String CARD_RFID_TABLE = "card_rfid_table";					//������� �� ���������� rfid � ��� ��� ����������

	//������������ ����� ������� CARD_GROUP_TABLE
	private final static String GROUP_ID = "id";		//�� ������ �������� � �������
	private final static String GROUP_NAME = "name";	//������������ ������
	
	//������������ ����� ������� CARD_CONTENT_TABLE
	private final static String CONTENT_ID = "id";					//�� ��������
	private final static String CONTENT_NAME = "name";				//������������ ��������
	private final static String CONTENT_GROUP_ID = "group_id";		//������������� ��������� � ������� ����������� �������
	
	//������������ ����� ������� CARD_RFID_TABLE
	private final static String RFID_ID = "id";						//�� �����
	private final static String RFID_RFID = "rfid";					//����� RFID
	private final static String RFID_CONTENT_ID = "content_id";		//������������� �������� � �������� ����� �����������
	
	
	
	
	/*****	�������� ���� ������	*****/
	
	//������ ���� ������
	private SQLiteDatabase database;			//���� ������ SQL
	private database_open_helper dbhelper;		//������ ��� �������� � ���������� ��
	
	
	
	
	
	
	/***** ������ ��� ������ � ��	*****/
	
	//������������� ��
	public void init()
	{
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������
		Context context = app.getApplicationContext();
		
		//�������� �� (� �������� ���� ���������)
		dbhelper = new database_open_helper(context);
		database = dbhelper.getWritableDatabase();
	}
	
	
	//��������������� ��
	public void deinit()
	{
		database.close();
		dbhelper.close();
	}
	
	
	
	
	//�������� �� ����������
	public int get_device_id(int build_number, int device_number)
	{
		//�������������
		int ret = -1;
		
		//������� ������ ������ � �������
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int id_idx = c.getColumnIndex(DEVICE_ID);
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			
			do
			{
				//��������� ��������
				int id = c.getInt(id_idx);
				int set_value = c.getInt(set_idx);
				int number_value = c.getInt(number_idx);
				
				//���� ������� - ��������� � ��������� �����
				if(set_value == build_number && device_number == number_value)
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� ������������� ���������� �� �������
	private int get_device_id(int position)
	{
		//������� ������ ������ ������� � ������������
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//����� ��������������
		int id = -1;
		
		//���� � ������� ���� ������
		int cnt = 0;			//������� ������� (�������)
		if(c.moveToFirst())
		{
			//���������� ������ ������ ��������
			int id_idx = c.getColumnIndex(DEVICE_ID);
			
			//����� ������ �������������
			do
			{
				//���� ����� �� ������ ������
				if(cnt == position)
				{
					//�������� ��������
					id = c.getInt(id_idx);;
				}
				
				cnt++;
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return id;
	}
	
	//�������� ���������� ����������
	public boolean get_device_activity(int build_number, int device_number)
	{
		//���������
		boolean ret = false;
		
		//������� ������ ������ ������� � ������������
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//���� � ������� ���� ������
		if(c.moveToFirst())
		{			
			//���������� ������ ������ ��������
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			int activity_idx = c.getColumnIndex(DEVICE_ACTIVITY);
		
			do
			{
				//�������� ��������
				int set = c.getInt(set_idx);
				int number = c.getInt(number_idx);
				boolean activity = c.getInt(activity_idx)>0;
				
				//���� ���������� ������� - ��������� ������
				if(set == build_number && number == device_number)
				{
					ret = activity;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}

	
	//�������� ������ ���������� � ���� �����������
	public List<Map<String, Object>> get_all_device_info()
	{
		//������� ��������� ��� �������� �������
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		
		//������� ������ ������ ������� � ������������
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//���� � ������� ���� ������
		if(c.moveToFirst())
		{			
			//���������� ������ ������ ��������
			int id_idx = c.getColumnIndex(DEVICE_ID);
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			int activity_idx = c.getColumnIndex(DEVICE_ACTIVITY);
		
			do
			{
				//�������� ��������
				int id = c.getInt(id_idx);
				int set = c.getInt(set_idx);
				int number = c.getInt(number_idx);
				boolean activity = c.getInt(activity_idx)>0;
				
				//������� ��������� ��� �������� ������
				Map<String, Object> cur_map = new HashMap<String, Object>();
				cur_map.put("id", id);
				cur_map.put("set", set);
				cur_map.put("number", number);
				cur_map.put("activity", activity);
				
				//�������� � ������ ����������
				ret.add(cur_map);
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� ������������ ����� � ��������� ��� ������
	public String get_image_filename_for_device(int device_number)
	{
		//��������� ������������
		String ret = "";
		
		//������� ������ ������ ������� � ������������
		Cursor c = database.query(DEVICE_IMAGE_TABLE, null, null, null, null, null, null);
		
		//���� � ������� ���� ������
		if(c.moveToFirst())
		{
			//���������� ������ ������ ��������
			int id_idx = c.getColumnIndex(IMAGE_ID);
			int name_idx = c.getColumnIndex(IMAGE_NAME);
			
			do
			{
				//�������� ��������
				int id = c.getInt(id_idx);
				String name = c.getString(name_idx);
				
				if(id == device_number)
				{
					ret = name;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	
	//�������� ������ ���� ���������
	public List<String> get_category_names()
	{
		//������� ���������
		List<String> ret = new ArrayList<String>();
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_GROUP_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int name_idx = c.getColumnIndex(GROUP_NAME);
			
			do
			{
				//��������� ��������
				String name = c.getString(name_idx);
				
				//���������� ���������� ������ � ���������
				ret.add(name);
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� �� ��������� �� �����
	public int get_category_id(String category_name)
	{
		//�������������
		int ret = -1;
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_GROUP_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int id_idx = c.getColumnIndex(GROUP_ID);
			int name_idx = c.getColumnIndex(GROUP_NAME);
			
			do
			{
				//��������� ��������
				int id = c.getInt(id_idx);
				String name = c.getString(name_idx);
				
				
				
				//���� ��� ������� - ��������� ������������� � ��������� �����
				if(name.equals(category_name))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� ������ ���� �������� ��� ������������ ���������
	public List<String> get_content_names(int category_id)
	{
		//������� ���������
		List<String> ret = new ArrayList<String>();
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_CONTENT_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int name_idx = c.getColumnIndex(CONTENT_NAME);
			int group_id_idx = c.getColumnIndex(CONTENT_GROUP_ID); 
			
			do
			{
				//��������� ��������
				String name = c.getString(name_idx);
				int group_id = c.getInt(group_id_idx);
				
				//���� ������� ����������� �������� ������ - ���������� � ���������
				if(group_id == category_id)
					ret.add(name);
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� �� ��������
	public int get_content_id(String content_name)
	{
		//�������������
		int ret = -1;
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_CONTENT_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int id_idx = c.getColumnIndex(CONTENT_ID);
			int name_idx = c.getColumnIndex(CONTENT_NAME);
			
			do
			{
				//��������� ��������
				int id = c.getInt(id_idx);
				String name = c.getString(name_idx);
				
				//���� ��� ������� - ��������� ������������� � ��������� �����
				if(name.equals(content_name))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� ������ RFID ��������� ��������
	public List<String> get_rfid_list(int rfid_content_id)
	{
		//������� ���������
		List<String> ret = new ArrayList<String>();
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_RFID_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int rfid_idx = c.getColumnIndex(RFID_RFID);
			int content_id_idx = c.getColumnIndex(RFID_CONTENT_ID); 
			
			do
			{
				//��������� ��������
				String rfid = c.getString(rfid_idx);
				int content_id = c.getInt(content_id_idx);
				
				//���� rfid ����������� �������� - ��������
				if(content_id == rfid_content_id)
					ret.add(rfid);
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	//�������� �� rfid ����
	public int get_rfid_id(String rfid_rfid)
	{
		//�������������
		int ret = -1;
		
		//������� ������ ������ � �������
		Cursor c = database.query(CARD_RFID_TABLE, null, null, null, null, null, null);
		
		//������ �� �������
		if(c.moveToFirst())
		{
			//���������� ������� ������ ��������
			int id_idx = c.getColumnIndex(RFID_ID);
			int rfid_idx = c.getColumnIndex(RFID_RFID);
			
			do
			{
				//��������� ��������
				int id = c.getInt(id_idx);
				String rfid = c.getString(rfid_idx);
				
				//���� ��� ������� - ��������� ������������� � ��������� �����
				if(rfid.equals(rfid_rfid))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//������� ������
		c.close();
		
		return ret;
	}
	
	
	
	//�������� ����� � ��
	public boolean add_device(int build_number, int device_number, boolean activity)
	{
		//�������� �� ������������� ������ ����������
		if(get_device_id(build_number, device_number) != -1)
			return false;
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(DEVICE_SET, build_number);
		cv.put(DEVICE_NUMBER, device_number);
		cv.put(DEVICE_ACTIVITY, activity);
		
		//�������� ����� ������ � ��
		database.insert(DEVICE_TABLE, null, cv);
		
		return true;
	}
	
	
	
	//�������� ����� ���������
	public boolean add_new_category(String category_name)
	{
		//�������� �� ������������� ����� ���������
		if(get_category_id(category_name) != -1)
			return false;
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(GROUP_NAME, category_name);
		
		//�������� ������
		database.insert(CARD_GROUP_TABLE, null, cv);
		
		return true;
	}
	
	//�������� ����� ���������
	private void add_new_category(int category_id, String category_name)
	{
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(GROUP_ID, category_id);
		cv.put(GROUP_NAME, category_name);
		
		//�������� ������
		database.insert(CARD_GROUP_TABLE, null, cv);
	}

	//�������� ����� �������
	public boolean add_new_content(int category_id, String content_name)
	{
		//�������� �� ������������� ������ ��������
		if(get_content_id(content_name) != -1)
			return false;
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(CONTENT_NAME, content_name);
		cv.put(CONTENT_GROUP_ID, category_id);
		
		//�������� ������
		database.insert(CARD_CONTENT_TABLE, null, cv);
		
		return true;
	}
	
	//�������� ����� �������
	private void add_new_content(int content_id, int category_id, String content_name)
	{
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(CONTENT_ID, content_id);
		cv.put(CONTENT_NAME, content_name);
		cv.put(CONTENT_GROUP_ID, category_id);
		
		//�������� ������
		database.insert(CARD_CONTENT_TABLE, null, cv);
	}

	//�������� ����� RFID
	public boolean add_new_rfid(int content_id, String rfid)
	{
		//�������� �� ������������� ������ rfid
		if(get_rfid_id(rfid) != -1)
			return false;
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(RFID_RFID, rfid);
		cv.put(RFID_CONTENT_ID, content_id);
		
		//�������� ������
		database.insert(CARD_RFID_TABLE, null, cv);
		
		return true;
	}
	
	//�������� ����� RFID
	private void add_new_rfid(int rfid_id, int content_id, String rfid)
	{
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(RFID_ID, rfid_id);
		cv.put(RFID_RFID, rfid);
		cv.put(RFID_CONTENT_ID, content_id);
		
		//�������� ������
		database.insert(CARD_RFID_TABLE, null, cv);
	}
	
	
	//������� RFID
	public void remove_rfid(String rfid_rfid)
	{
		//������� ������
		database.delete(CARD_RFID_TABLE, RFID_RFID + " = \"" + rfid_rfid + "\"", null);
	}
	
	//������� RFID
	public void remove_rfid(int rfid_content_id, int pos)
	{
		//���������
		List<String> ret = get_rfid_list(rfid_content_id);
		
		//�������� �� ����� �� �������
		if(pos >= ret.size() || pos < 0)
			return;
		
		//�������� ������ rfid
		String cur_rfid = ret.get(pos);
		
		//��������
		remove_rfid(cur_rfid);
	}
	
	//������� ��� rfid ������������� ��������
	public void remove_all_rfid_from_content(int rfid_content_id)
	{
		//���������
		List<String> ret = get_rfid_list(rfid_content_id);
		
		//������� ��� rfid
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//�������� ������� rfid
			String cur_rfid = it.next();
			
			//�������
			remove_rfid(cur_rfid);
		}
	}
	
	//������� �������
	public void remove_content(String name)
	{
		//�������� �� ��������
		int content_id = get_content_id(name);
		
		//������� ��� rfid ��� ��������
		remove_all_rfid_from_content(content_id);
		
		//������� ������
		database.delete(CARD_CONTENT_TABLE, CONTENT_ID + " = \"" + content_id + "\"", null);
	}
	
	//������� �������
	public void remove_content(int content_category_id, int pos)
	{
		//���������
		List<String> ret = get_content_names(content_category_id);
		
		//�������� �� ����� �� �������
		if(pos >= ret.size() || pos < 0)
			return;
		
		//�������� ������ �������
		String content_name = ret.get(pos);
		
		//��������
		remove_content(content_name);
	}
	
	//������� ���� ������� ������������� ���������
	public void remove_all_content_from_category(int content_category_id)
	{
		//���������
		List<String> ret = get_content_names(content_category_id);
		
		//������� ���� �������
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//�������� ������� �������
			String cur_content = it.next();
			
			//�������
			remove_content(cur_content);
		}
	}
	
	//������� ���������
	public void remove_category(String name)
	{
		//�������� �� ���������
		int category_id = get_category_id(name);
		
		//������� ���� ������� ��� ���������
		remove_all_content_from_category(category_id);
		
		//������� ������
		database.delete(CARD_GROUP_TABLE, GROUP_ID + " = \"" + category_id + "\"", null);
	}
	
	//������� ���������
	public void remove_category(int pos)
	{
		//���������
		List<String> ret = get_category_names();
		
		//�������� �� ����� �� �������
		if(pos >= ret.size() || pos < 0)
			return;
		
		//�������� ������ ���������
		String category_name = ret.get(pos);
		
		//��������
		remove_category(category_name);
	}
	
	
	//������� ���������� �� ��
	public void delete_device(int position)
	{
		//����� ��������������
		int id = get_device_id(position);
		
		//���� ������ �� ������� - ������ �� ������
		if(id == -1)
			return;
		
		//������� ������
		database.delete(DEVICE_TABLE, "id = " + id, null);
	}
	
	
	//������� ���� ������
	public void clear_card_database()
	{
		//���������
		List<String> ret = get_category_names();
		
		//������� ��� ���������
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//������� ���������
			String cur_category = it.next();
			
			//�������
			remove_category(cur_category);
		}
	}
	
	
	
	//�������� ���������� ����������
	public void update_device_activity(int position, boolean activity)
	{
		//����� ��������������
		int id = get_device_id(position);
		
		//���� ������ �� ������� - ������ �� ������
		if(id == -1)
			return;
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(DEVICE_ACTIVITY, activity);
		
		//�������� �� ��
		database.update(DEVICE_TABLE, cv, "id = ?", new String[] { "" + id });
	}
	
	
	//�������� ��� ���������
	public boolean update_category_name(String old_name, String new_name)
	{
		//���� ��� �� ���������� - ������ �� ������
		if(old_name.equals(new_name))
			return true;
		
		//���� ����� ��� ��� ������������ - ������ �� ������ � ������� ������� �������������� ����������
		if(get_category_id(new_name) != -1)
			return false;
		
		//�������� ������������� ���������
		int id = get_category_id(old_name);
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(GROUP_NAME, new_name);
		
		//�������� �� ��
		database.update(CARD_GROUP_TABLE, cv, GROUP_ID + " = ?", new String[] { "" + id});
		
		return true;
	}
	
	//�������� ��� ���������
	public boolean update_category_name(int pos, String new_name)
	{
		//���������
		List<String> ret = get_category_names();
		
		//�������� �� ����� �� �������
		if(pos >= ret.size() || pos < 0)
			return false;
		
		//�������� ������ ���
		String old_name = ret.get(pos);
		
		//��������
		return update_category_name(old_name, new_name);
	}
	
	//�������� ��� ��������
	public boolean update_content_name(String old_name, String new_name)
	{
		//���� ��� �� ���������� - ������ �� ������
		if(old_name.equals(new_name))
			return true;
		
		//���� ����� ��� ��� ������������ - ������ �� ������ � ������� ������� �������������� ����������
		if(get_content_id(new_name) != -1)
			return false;
		
		//�������� ������������� ���������
		int id = get_content_id(old_name);
		
		//������ ��� ������
		ContentValues cv = new ContentValues();
		
		//��������� �������
		cv.put(CONTENT_NAME, new_name);
		
		//�������� �� ��
		database.update(CARD_CONTENT_TABLE, cv, CONTENT_ID + " = ?", new String[] { "" + id});
		
		return true;
	}
	
	//�������� ��� ��������
	public boolean update_content_name(int category_id, int pos, String new_name)
	{
		//���������
		List<String> ret = get_content_names(category_id);
		
		//�������� �� ����� �� �������
		if(pos >= ret.size() || pos < 0)
			return false;
		
		//�������� ������ ���
		String old_name = ret.get(pos);
		
		//��������
		return update_content_name(old_name, new_name);
	}
	
	
	//�������� ������ ��� ���� ������
	private String convert_from_normal_to_database(String s)
	{
		char [] ret = s.toCharArray();
		String ret_string = "";
		for(int i = 0; i < ret.length; i++)
		{
			if(ret[i] == ' ')
				ret_string += '_';
			else
				ret_string += ret[i];
		}
		return ret_string;
	}
	
	//�������� ���������� ������ �� ������ ��� ���� ������
	private String convert_from_database_to_normal(String s)
	{
		char [] ret = s.toCharArray();
		String ret_string = "";
		for(int i = 0; i < ret.length; i++)
		{
			if(ret[i] == '_')
				ret_string += ' ';
			else
				ret_string += ret[i];
		}
		return ret_string;
	}

	
	//������� ������ �� ���������� ���������� � ��������� � ������
	public String get_card_database_in_string()
	{
		//������� ������
		String ret = "";
		
		//�������� ������ ���������
		List<String> category_list = get_category_names();
		
		//������ ���-�� ���������
		ret += category_list.size();
		
		//������ ���������� � ���������� (��, ������������)
		for(Iterator<String> category_it = category_list.iterator(); category_it.hasNext(); )
		{
			//�������� ������� ���������
			String cur_category = category_it.next();
			
			//�������� �������������
			int cur_category_id = get_category_id(cur_category);
			
			//������
			ret += "\t" + cur_category_id + "\t" + convert_from_normal_to_database(cur_category);
			
			//�������� ������ �������� ��� ���������
			List<String> content_list = get_content_names(cur_category_id);
			
			//������ ���-�� �������� ��� ���������
			ret += "\t" + content_list.size();
			
			//������ ���������� � �������� (��, ������������, �� ���������)
			for(Iterator<String> content_it = content_list.iterator(); content_it.hasNext(); )
			{
				//�������� ������� �������
				String cur_content = content_it.next();
				
				//�������� �������������
				int cur_content_id = get_content_id(cur_content);
				
				//������
				ret += "\t" + cur_content_id + "\t" + convert_from_normal_to_database(cur_content) + "\t" + cur_category_id;
				
				//�������� ������ rfid ��� ��������
				List<String> rfid_list = get_rfid_list(cur_content_id);
				
				//������ ���-�� rfid
				ret += "\t" + rfid_list.size();
				
				//������ ���������� � rfid (��, rfid, �� ��������)
				for(Iterator<String> rfid_it = rfid_list.iterator(); rfid_it.hasNext(); )
				{
					//�������� ������� rfid
					String cur_rfid = rfid_it.next();
					
					//�������� �������������
					int cur_rfid_id = get_rfid_id(cur_rfid);
					
					//������
					ret += "\t" + cur_rfid_id + "\t" + convert_from_normal_to_database(cur_rfid) + "\t" + cur_content_id;
				}
			}
		}
		
		return ret;
	}
	
	
	//�������� ������ ���������� ���������� � �������� �� ���������� ���������� � ���������
	public void set_card_database_by_string(String database_string)
	{
		//�������� ��
		clear_card_database();
		
		//������� ������ �� ������
		String [] tokens = database_string.split("\t");
		
		//�������� �� �������
		int it = 0;
		
		//���������� ���-�� ���������
		int category_count = Integer.parseInt(tokens[it++]);
			
		//������ �� ���-�� ���������
		for(int category_it = 0; category_it < category_count; category_it++)
		{
			//�������� �� ���������
			int cur_category_id = Integer.parseInt(tokens[it++]);
			
			//�������� ��� ���������
			String cur_category = tokens[it++];
			
			//�������� ��������� � ��
			add_new_category(cur_category_id, convert_from_database_to_normal(cur_category));
			
			//���������� ���-�� �������� � ���������
			int content_count = Integer.parseInt(tokens[it++]);
			
			//������ �� ���-�� ��������� � ���������
			for(int content_it = 0; content_it < content_count; content_it++)
			{
				//�������� ��
				int cur_content_id = Integer.parseInt(tokens[it++]);
				
				//�������� ��� ��������
				String cur_content = tokens[it++];
				
				//�������� ������������ ���������
				int cur_content_category_id = Integer.parseInt(tokens[it++]);
				
				//�������� ������� � ��
				add_new_content(cur_content_id, cur_content_category_id, convert_from_database_to_normal(cur_content));
				
				//���������� ���-�� rfid � ��������
				int rfid_count = Integer.parseInt(tokens[it++]);
				
				//������ �� ���-�� rfid � ��������
				for(int rfid_it = 0; rfid_it < rfid_count; rfid_it++)
				{
					//�������� ��
					int cur_rfid_id = Integer.parseInt(tokens[it++]);
					
					//�������� RFID
					String cur_rfid = tokens[it++];
					
					//�������� �� ��������
					int cur_rfid_content_id = Integer.parseInt(tokens[it++]);
					
					//�������� rfid � ��
					add_new_rfid(cur_rfid_id, cur_rfid_content_id, convert_from_database_to_normal(cur_rfid));
				}
			}
		}
	}
	
	
	/***** �������� Database Helper	*****/
	
	class database_open_helper extends SQLiteOpenHelper
	{
		/***** ������ ��� �������� ��	*****/
		
		//������� �� �������� ������
		private final static String CREATE_DEVICE_IMAGE_TABLE =
				"create table " + DEVICE_IMAGE_TABLE + "(" +
				IMAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				IMAGE_NAME + " VARCHAR NOT NULL);";
		private final static String CREATE_DEVICE_TABLE =
				"create table " + DEVICE_TABLE + "(" +
				DEVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				DEVICE_SET + " INTEGER NOT NULL, " +
				DEVICE_NUMBER + " INTEGER NOT NULL, " +
				DEVICE_ACTIVITY + " BOOLEAN NOT NULL);";
		private final static String CREATE_CARD_GROUP_TABLE =
				"create table " + CARD_GROUP_TABLE + "(" +
				GROUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				GROUP_NAME + " VARCHAR NOT NULL);";
		private final static String CREATE_CARD_CONTENT_TABLE = 
				"create table " + CARD_CONTENT_TABLE + "(" +
				CONTENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				CONTENT_NAME + " VARCHAR NOT NULL, " +
				CONTENT_GROUP_ID + " INTEGER NOT NULL);";
		private final static String CREATE_CARD_RFID_TABLE = 
				"create table " + CARD_RFID_TABLE + "(" +
				RFID_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
				RFID_RFID + " VARCHAR NOT NULL, " +
				RFID_CONTENT_ID + " INTEGER NOT NULL);";
		
		/***** �������� ������ ��� ������ � database helper	*****/
		
		//�����������
		public database_open_helper(Context context)
		{
			//����������� �����������
			super(context, "vplay_database", null, 1);
		}
		
		//���� ���� ������ �� ���������� - ���������� ���� �����
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			//�������� ������
			db.execSQL(CREATE_DEVICE_IMAGE_TABLE);
			db.execSQL(CREATE_DEVICE_TABLE);
			db.execSQL(CREATE_CARD_GROUP_TABLE);
			db.execSQL(CREATE_CARD_CONTENT_TABLE);
			db.execSQL(CREATE_CARD_RFID_TABLE);
			
			//��������� ��������� �� ����������
			VPlayApplication app = VPlayApplication.get_instance();
			
			//��������� ���������
			Context context = app.getApplicationContext();
			
			//��������� ������
			AssetManager asset_manager = context.getAssets();
			
			//���������� ���� ������ �������������� ����������� ��� �������
			fill_device_image_table(db, asset_manager);
		}
		
		//���� ���� ������ ����������, �� ����� ����� ����� ������
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			//��� ���������� �� ���� ������ �� ������
		}	
		
		
		/***** ��������������� ������	*****/
		
		//��������� ������� DEVICE_IMAGE_TABLE
		private void fill_device_image_table(SQLiteDatabase db, AssetManager asset_manager)
		{
			//����� ��� ��������� ������ ������
			InputStream is = null;
			
			//������ ������ �����
			BufferedReader reader;
			
			//������ ��� ������ ������ ���������
			String line;
			
			/***** ���������� ���� ������ �������������� ����������� ��� �������	*****/
			
			//������� ���� � �������������� ����������� ��� �������
			try
			{
				is = asset_manager.open("pult_images.txt");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			//������ ������� �� �����
			reader = new BufferedReader(new InputStreamReader(is));
			try
			{
				while((line = reader.readLine()) != null)
				{
					//������ ��� ������
					ContentValues cv = new ContentValues();
					
					//��������� �������
					cv.put(IMAGE_NAME, line);
					
					//�������� ����� ������ � ��
					db.insert(DEVICE_IMAGE_TABLE, null, cv);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			//������� ���� � �������������� ����������� ��� �������
			try
			{
				is.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
