package com.vplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vplay.WordGameActivity.Player;

import vplay_engine.DebugClass;
import vplay_engine.UsbService;
import vplay_engine.UsbService.EDeviceKey;
import vplay_engine.VPlayActivity;
import vplay_engine.VPlayAdapter.CellInfo;
import vplay_engine.VPlayAdapter.ColumnInfo;
import vplay_engine.VPlayAdapter.EColumnType;
import vplay_engine.VPlayApplication;
import vplay_engine.VPlayDatabase;
import vplay_engine.VPlayTable;
import vplay_engine.VPlayTable.HeaderCellInfo;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

//���� ����
public class TreasureActivity extends VPlayActivity
{

	/***** �������� ����	*****/
	
	//�������
	private VPlayTable table;
	
	//������ ����������� �������
	private List<Player> players = null;
		
	//������ ������
	List<String> treasure_list = null;
	
	//��� ��������� �������
	private List<String> letter_value_list = null;
	private List<List<String>> letter_rfid_list = null;
	
	/***** ����������� ������	*****/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_treasure);
	
		//��������
		Context context = this;
		
		//�� xml ����� �������� ���� ������ �������
		int item_layout_id = R.layout.activity_treasure_tableitem;
		
		//���������� � ������� ������ �������
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.treasure_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.treasure_treasure_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.treasure_player_state_text));
		
		//�� LinearLayout � ������� ����� ��������� �������
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.treasure_table_layout);
		
		//���������� � ��������� �������
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("�����", 1, 300));
		header_info.add(new HeaderCellInfo("����", 1, 500));
		header_info.add(new HeaderCellInfo("��������� ������", 1, 600));
		
		//������� ��� ������������ �� ��������� ��������� ���������
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//�������� �������
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//�������� ���������� � ���� ����
		String game_theme_string = "";
		Bundle extras = getIntent().getExtras();
		if(extras != null)
			game_theme_string = extras.getString("game_theme");
		else
		{
			DebugClass.message("�� ������� �������� ���������� � ���� ����");
			return;
		}
		
		//�������� ������ ������
		treasure_list = get_game_content_list(game_theme_string);

		//���� ������ - ������ �� ������
		if(treasure_list.size() == 0)
			return;
		
		//������������������� ����
		init_game();
	}
	
	
	
	
	
	/*****	������� �� ������	*****/
	
	public void on_click_treasure_end_game_button(View v)
	{
		this.finish();
	}
	
	
	
	
	
	/****** ������ ��� ������ � ��������	*****/
	

	//������� ������ �� ������
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{	
		//���� ������ ������ �������� �� 1 ��� 2
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON))
			return;
		
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//���� ���������� �� ���������� � ���� - ������ �� ������
		if(!db.get_device_activity(build_number, device_number))
			return;
		
		if(key.equals(EDeviceKey.ONE_BUTTON))
		{
			//�������� ��������� �� usb ������
			UsbService usb_service = UsbService.get_instance();
			
			//��������� ������ ������ �����
			usb_service.send_read_rf_id_command(build_number, device_number);
		}
		else
		{
			//����� ������, ������� �����
			Player player = null;
			for(Iterator<Player> it = players.iterator(); it.hasNext(); )
			{
				//�������� �������� ������
				Player cur_player = it.next();
				
				//���� ������ - ��������� � ����� �� �����
				if(cur_player.build_number == build_number && cur_player.device_number == device_number)
				{
					player = cur_player;
					break;
				}
			}
			if(player == null)
				return;
			
			//�������� ��������� �� usb ������
			UsbService usb_service = UsbService.get_instance();
			
			//��������� ���������� � �����
			usb_service.send_display_string_command(build_number, device_number, player.get_closed_word());
		}
	}


	//��������� ��������� rfid �����
	@Override
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� ��������� �� usb ������
		UsbService usb_service = UsbService.get_instance();
		
		//���� ���������� �� ���������� � ���� - ������ �� ������
		if(!db.get_device_activity(build_number, device_number))
			return;
		
		//����� ������, ������� �����
		Player player = null;
		for(Iterator<Player> it = players.iterator(); it.hasNext(); )
		{
			//�������� �������� ������
			Player cur_player = it.next();
			
			//���� ������ - ��������� � ����� �� �����
			if(cur_player.build_number == build_number && cur_player.device_number == device_number)
			{
				player = cur_player;
				break;
			}
		}
		if(player == null)
			return;
		
		//�������� ������, ��������������� rfid �����
		String letter = get_letter(rfid);
		if(letter == null)
		{
			DebugClass.message("������� � ����� ������ �� ����������");
			return;
		}	
		
		//�������� �������� ������ ��� ����� � ���� ���� ��������� - ��������� ����� ������ �� ����� - ����� ������� ������
		if(player.new_letter(letter))
		{
			//������� ����������� ������
			usb_service.send_display_string_command(build_number, device_number, player.get_closed_word());
		}
		else
		{
			//������� ������
			usb_service.send_led_flash_command(build_number, device_number, 1);
		}
		
		//�������� �������
		update_table();
	}
	
	
	
	
	
	/*****	��������������� ������	*****/

	
	//�������� �������
	private void update_table()
	{
		Iterator<Player> it = players.iterator();
		for(int i = 0; i < table.getCount(); i++)
		{
			//�������� ������� ������ � �������
			List<CellInfo> cur_item = table.get_item_params_list(i);
			
			//�������� �������� ������
			Player player = it.next();
			
			//�������� ������
			cur_item.set(1, new CellInfo(player.get_treasure()));
			cur_item.set(2, new CellInfo(player.get_closed_word()));
			
			//�������� ������ � �������
			table.set_item_params_list(i, cur_item);
		}
	}
	
	
	//�������� ����� �� ����
	private String get_letter(String rfid)
	{
		Iterator<String> it_values = letter_value_list.iterator();
		Iterator<List<String>> it_rfids = letter_rfid_list.iterator();
		
		for(; it_values.hasNext() && it_rfids.hasNext(); )
		{
			//�������� ������
			String cur_letter = it_values.next();
			
			//�������� ������ rfid ��� �����
			List<String> rfid_list = it_rfids.next();
		
			//������ �����
			for(Iterator<String> it_rfid = rfid_list.iterator(); it_rfid.hasNext(); )
			{
				//������� rfid
				String cur_rfid = it_rfid.next();
				
				//���� ������
				if(cur_rfid.equals(rfid))
					return cur_letter;
			}
		}
		
		return null;
	}
	

	
	//������������� ����
	private void init_letters(String category_name)
	{
		//������� ������ ��������
		letter_value_list = new ArrayList<String>();
		letter_rfid_list = new ArrayList<List<String>>();
		
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� �� ���������
		int category_id = db.get_category_id(category_name);
		if(category_id == -1)
		{
			DebugClass.message("������: ����������� ���������: " + category_name);
			return;
		}
		
		//�������� ������� � ����������� �� ��������� ������ ����
		List<String> content_names = db.get_content_names(category_id);
		
		//��������� ������ ���������� � ������
		for(Iterator<String> it = content_names.iterator(); it.hasNext(); )
		{
			//�������� ������� ������
			String cur_content = it.next();
			
			//�������� ������ ����������
			String name = cur_content;
			
			//�������� �� �������� ��������
			int cur_content_id = db.get_content_id(cur_content);
			
			//�������� ������ RFID ��� ��������
			List<String> rfid = db.get_rfid_list(cur_content_id);
			
			//� ������� ��������
			name = name.toLowerCase();
			
			//�������� ��������
			letter_value_list.add(name);
			letter_rfid_list.add(rfid);
		}
	}
	
	
	//��������� ������� ��������� ������� ��� ���� � ������ �������������
	private void init_game()
	{
		//������������������� ��� ��������� �������
		init_letters("����� � �����");
		
		//������� ������ ������������ �������
		players = new ArrayList<Player>();
		
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� ������� ���������
		List<Map<String, Object>> table_data = db.get_all_device_info();
		
		//���� ������� �� �������
		for(Iterator<Map<String, Object>> it = table_data.iterator(); it.hasNext(); )
		{
			//�������� ���������� � ������� �������
			Map<String, Object> cur_map = it.next();
			
			//�������� ������
			boolean activity = (Boolean) cur_map.get("activity");
			int device_number = (Integer) cur_map.get("number");
			int build_number = (Integer) cur_map.get("set");
			
			//���� ����� �� ������� - ����������
			if(!activity)
				continue;
			
			//�������� ������������ ����� � ������������ ��� ������
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//�������� �� ������� � ������������
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//�������� ���-�� ������ � ����
			int treasure_cnt = treasure_list.size();
			
			//��������� ��������� ���� �������� ������
			Random random = new Random();
			int treasure_idx = random.nextInt(treasure_cnt - 0 + 1) + 0;
			String cur_treasure = treasure_list.get(treasure_idx);
			
			//������������ ������ ������
			Player new_player = new Player(build_number, device_number, cur_treasure);
			
			//��������
			players.add(new_player);
			
			//������� ������ ��� �������
			List<CellInfo> obj = get_table_obj(resource_id, cur_treasure, new_player.get_closed_word());
			
			//�������� ������
			table.add_object(obj);
		}
	}
	
	
	//�������� ������ ������
	private List<String> get_game_content_list(String filename)
	{
		//������ ������
		List<String> ret = new ArrayList<String>();
		
		//��������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
				
		//��������� ���������
		Context context = app.getApplicationContext();
		
		//��������� ������
		AssetManager asset_manager = context.getAssets();
		
		//����� ��� ��������� ������ ������
		InputStream is = null;
		
		//������ ������ �����
		BufferedReader reader = null;
		
		//������ ��� ������ ������ ���������
		String line;
		
		//������� ���� � ��������� ��������
		try
		{
			is = asset_manager.open("treasure_game/" + filename);
		}
		catch(IOException e)
		{
			DebugClass.message("�� ������ ���� � ����� ����");
			return ret;
		}
		
		//������ ������� �� �����
		try
		{
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		}
		catch(UnsupportedEncodingException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try
		{
			while((line = reader.readLine()) != null)
			{
				//������� ���� ����
				String treasure_name = line;
				
				//�������� � ������
				ret.add(treasure_name);
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
		
		return ret;
	}
	
	
	//��������� ����� ������ ��� �������
	private List<CellInfo> get_table_obj(int resource_id, String treasure, String closed_treasure)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(treasure));
		obj.add(new CellInfo(closed_treasure));
		
		return obj;
	}
	
	
	
	
	/*****	�������������� ������/���������/������������	*****/
	
	//����� ������
	public class Player
	{
		//�������� ������
		
		int build_number;	//����� ������
		int device_number;	//����� ������ � ������
		
		private String treasure;	//���������� ����
		private String closed;		//�������� �����
		
		public Player(int build_number, int device_number, String treasure_string)
		{
			this.build_number = build_number;
			this.device_number = device_number;
			set_treasure(treasure_string);
		}
		
		//������ �����
		public void set_treasure(String s)
		{
			treasure = s;
			closed = "";
			for(int i = 0; i < s.length(); i++)
				closed += "*";
		}
		
		//�������� �������� �����
		public String get_closed_word()
		{
			return closed;
		}
		
		//�������� ����
		public String get_treasure()
		{
			return treasure;
		}
		
		//������� ����� �����
		public boolean new_letter(String ch_string)
		{
			//� ������� ��������
			ch_string = ch_string.toLowerCase();
			char ch = ch_string.toCharArray()[0];
			
			//�������� ����� � ���� ������� ��������
			char [] treasure_char = treasure.toLowerCase().toCharArray();
			char [] closed_char = closed.toLowerCase().toCharArray();
			
			//������
			boolean is_find = false;
			closed = "";
			for(int i = 0; i < treasure_char.length; i++)
			{
				//���� �������
				if(treasure_char[i] == ch)
				{
					is_find = true;
					closed += treasure_char[i];
				}
				else
				{
					closed += closed_char[i];
				}
			}
			
			return is_find;
		}
	};
}
