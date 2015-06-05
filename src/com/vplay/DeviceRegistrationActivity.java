package com.vplay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import vplay_engine.DebugClass;
import vplay_engine.UsbService;
import vplay_engine.VPlayActivity;
import vplay_engine.VPlayAdapter.CellInfo;
import vplay_engine.VPlayAdapter.ColumnInfo;
import vplay_engine.VPlayAdapter.EColumnType;
import vplay_engine.VPlayApplication;
import vplay_engine.VPlayDatabase;
import vplay_engine.VPlayTable;
import vplay_engine.VPlayTable.HeaderCellInfo;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;



//���� ����������� �������
public class DeviceRegistrationActivity extends VPlayActivity
{
	
	/*****	�������� ����	*****/
	
	//�������
	VPlayTable table;
	
	
	
	
	/***** ����������� ������	*****/
	
	//��� �������� ����
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//����������� ������������� ����
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_registration);
		
		//��������
		Context context = this;
		
		//�� xml ����� �������� ���� ������ �������
		int item_layout_id = R.layout.activity_device_registration_tableitem;
		
		//���������� � ������� ������ �������
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.CHECK_COLUMN, R.id.device_registration_device_activity));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.device_registration_device_name));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.device_registration_device_params));
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.device_registration_device_image));
		
		//�� LinearLayout � ������� ����� ��������� �������
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.device_registration_main_layout);
		
		//���������� � ��������� �������
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("����������", 1, 250));
		header_info.add(new HeaderCellInfo("������������ ������", 2, 500));
		header_info.add(new HeaderCellInfo("�����������", 1, 300));
		
		//������� ��� ������������ �� ��������� ��������� ���������
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		listeners.add(check_state_change);
		
		//�������� �������
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//������������� ������� ���������� �� ��
		init_table();
		
		//��������� ����������� ���� ��� ������
		ListView lv = table.get_main_list();
		registerForContextMenu(lv);
	}
	
	
	
	
	/***** ������ ��� ������ � �����������	*****/
	
	//���������� ��� ��������� ������ ������ ��� ������� ������ �� ����������
	public void update_key(short build_number, byte device_number, UsbService.EDeviceKey key)
	{
		add_device(build_number, device_number);
	}
	
	//���������� ��� ��������� ������ ������ ��� ���������� RFID ����� �� ����������
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
	}
	
	
	
	
	/***** �������������� ������	*****/
	
	//��������� ����� ������ ��� �������
	private List<CellInfo> get_table_obj(boolean activity, int device_number, int build_number, int resource_id)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(activity));
		obj.add(new CellInfo("����� #" + device_number));
		obj.add(new CellInfo("��������: " + build_number));
		obj.add(new CellInfo(resource_id));
		
		return obj;
	}
	
	//������������� ������� ������� �� ��
	private void init_table()
	{
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� �������
		List<Map<String, Object>> table_data = db.get_all_device_info();
		
		//���� ���������� �������
		for(Iterator<Map<String, Object>> it = table_data.iterator(); it.hasNext(); )
		{
			//�������� ���������� � ������� �������
			Map<String, Object> cur_map = it.next();
			
			//�������� ������
			boolean activity = (Boolean) cur_map.get("activity");
			int device_number = (Integer) cur_map.get("number");
			int build_number = (Integer) cur_map.get("set");
			
			//�������� ������������ ����� � ������������ ��� ������
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//�������� �� ������� � ������������
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//������� ������ ��� �������
			List<CellInfo> obj = get_table_obj(activity, device_number, build_number, resource_id);
			
			//�������� ������
			table.add_object(obj);
		}
	}
	
	
	//�������� ����� � ���� ������
	private void add_device(short build_number, byte device_number)
	{
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� ����������
		boolean activity = true;		//���������� ����������
		boolean ret = db.add_device(build_number, device_number, activity);
		
		//���� ���������� ������� ���������
		if(ret)
		{
			DebugClass.message("���������� ���������");
			
			//�������� ������������ ����� � ������������ ��� ������
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//�������� �� ������� � ������������
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//������� ������ ��� �������
			List<CellInfo> obj = get_table_obj(activity, device_number, build_number, resource_id);
			
			//�������� � �������
			table.add_object(obj);
		}
		else	//���� ����� ���������� ��� ���� � ��
		{
			DebugClass.message("���������� ��� ���� � ������");
		}
	}
	
	
	
	
	/***** ����������� ��������� �������	*****/
	
	//���������� ����������
	OnCheckedChangeListener check_state_change = new OnCheckedChangeListener()
	{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			//�������� TAG ����������
			List<Integer> check_box_tag = (List<Integer>) buttonView.getTag();
			int row = (Integer) check_box_tag.get(0);
			int col = (Integer) check_box_tag.get(1);
			
			//�������� ��������� �� ����������
			VPlayApplication app = VPlayApplication.get_instance();
			
			//�������� ��������� �� ���� ������
			VPlayDatabase db = app.get_database();
			
			//�������� ������ � ��
			db.update_device_activity(row, isChecked);
			
			//�������� ������ � �������
			List<CellInfo> object_info = table.get_item_params_list(row);
			object_info.set(col, new CellInfo(isChecked));
			table.set_item_params_list(row, object_info);
		}
	};
	
	
	
	
	/*****	������ ��� ������������ ����	*****/
	
	private static final int CM_DELETE_ID = 1;		//������������� ������ �������� � ����������� ����
	
	
	
	/*****	������ ������ � ����������� ����	*****/
	
	//��� �������� ������������ ����
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CM_DELETE_ID, 0, "������� ������");
	}
	
	//��� ������ ������ � ����������� ����
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		//���� ������� ��������
		if(item.getItemId() == CM_DELETE_ID)
		{
			//�������� ���������� � ������ ������
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
			
			//�������� ������� ������
			int position = acmi.position;
			
			//�������� ��������� �� ����������
			VPlayApplication app = VPlayApplication.get_instance();
			
			//�������� ��������� �� ���� ������
			VPlayDatabase db = app.get_database();
			
			//������� ������ �� ��
			db.delete_device(position);
			
			//������� ������
			table.remove_object(position);
			
			return true;
		}
		return super.onContextItemSelected(item);
	}
}