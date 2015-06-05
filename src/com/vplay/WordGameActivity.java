package com.vplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import vplay_engine.DebugClass;
import vplay_engine.UsbService;
import vplay_engine.VPlayActivity;
import vplay_engine.VPlayApplication;
import vplay_engine.VPlayDatabase;
import vplay_engine.VPlayTable;
import vplay_engine.UsbService.EDeviceKey;
import vplay_engine.VPlayAdapter.CellInfo;
import vplay_engine.VPlayAdapter.ColumnInfo;
import vplay_engine.VPlayAdapter.EColumnType;
import vplay_engine.VPlayTable.HeaderCellInfo;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class WordGameActivity extends VPlayActivity {
	
/***** �������� ����	*****/
	
	//�������
	private VPlayTable table;
	
	//������ ����������� �������
	private List<Player> players = null;
	
	//������ ���� �� ����
	List<String> words_list = null;
	
	//��� ��������� �������
	private List<String> letter_value_list = null;
	private List<List<String>> letter_rfid_list = null;	
	
	//����� ��� �������� ������ �������� � ��������������� ������
	HashMap<String, String> spinner_map = new HashMap<String, String>();
	
	/***** ����������� ������	*****/
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_word_game);
		
		//��������
		Context context = this;
		
		//�� xml ����� �������� ���� ������ �������
		int item_layout_id = R.layout.activity_wordgame_tableitem;
		
		//���������� � ������� ������ �������
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.wordgame_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_correct_words_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_incorrect_words_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_points_text));
		
		//�� LinearLayout � ������� ����� ��������� �������
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.word_game_table_layout);
		
		//���������� � ��������� �������
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("�����", 1, 300));
		header_info.add(new HeaderCellInfo("���������� �����", 1, 500));
		header_info.add(new HeaderCellInfo("������������ �����", 1, 600));
		header_info.add(new HeaderCellInfo("����", 1, 300));
		
		//������� ��� ������������ �� ��������� ��������� ���������
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//�������� �������
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
				
		//�������� ���������� � ���� ����
		String theme_name = "";
		String game_theme_name = "";
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			theme_name = extras.getString("game_theme");
			game_theme_name = extras.getString("game_theme_name");
		}
		else
		{
			DebugClass.message("�� ������� �������� ���������� � ���� ����");
			return;
		}
		
		//�������� ������ ����
		words_list = get_game_content_list(theme_name);

		//���� ������ - ������ �� ������
		if(words_list.size() == 0)
			return;
		
		//������������������� ����
		init_game(game_theme_name);
	}	
	
	/*****	������� �� ������	*****/
	
	public void on_click_wordgame_end_game_button(View v)
	{
		this.finish();
	}	
		
	/****** ������ ��� ������ � ��������	*****/	

	//������� ������ �� ������
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{
		//���� ������ ������ �������� �� 1 ��� 2 ��� DEL ��� SEND
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON) && !key.equals(EDeviceKey.DELETE_BUTTON) && !key.equals(EDeviceKey.SEND_BUTTON) && !key.equals(EDeviceKey.THREE_BUTTON))
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
		else if(key.equals(EDeviceKey.TWO_BUTTON))
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
			
			//��������� ���������
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
		}
		else if(key.equals(EDeviceKey.DELETE_BUTTON))
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
			
			//�������� ������� �����
			player.clear_current_word();
			
			//�������� ��������� �� usb ������
			UsbService usb_service = UsbService.get_instance();
			
			//��������� ���������
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
		}
		else if(key.equals(EDeviceKey.SEND_BUTTON))
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
			
			//�������� ������� �����
			player.add_new_word();
			
			//�������� ��������� �� usb ������
			UsbService usb_service = UsbService.get_instance();
			
			//��������� ���������
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
			
			update_table();
		}
		else if(key.equals(EDeviceKey.THREE_BUTTON))
		{
			//���������� ���������� �����
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
			
			//��������� ���������
			usb_service.send_display_string_command(build_number, device_number, "���-�� �����: " + String.valueOf(player.get_score()));
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
		String word = get_letter(rfid);
		if(word == null)
		{
			DebugClass.message("������� � ����� ������ �� ����������");
			return;
		}	
		
		//�������� �������� ������ ��� �����
		player.set_current_word(word);
		
		//������� ����������� ������
		usb_service.send_display_string_command(build_number, device_number, player.current_word);
		
		//�������� �������
		update_table();
	}	
	
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
			cur_item.set(1, new CellInfo(player.get_correct_words()));
			cur_item.set(2, new CellInfo(player.get_incorrect_words()));
			cur_item.set(3, new CellInfo(player.get_score() + ""));
			
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
			name = name.toUpperCase();
			
			//�������� ��������
			letter_value_list.add(name);
			letter_rfid_list.add(rfid);
		}
	}	
	
	//��������� ������� ��������� ������� ��� ���� � ������ �������������
	private void init_game(String theme_name)
	{
		//������������������� ��� ��������� �������
		init_letters(theme_name);
		
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
			
			//������������ ������ ������
			Player new_player = new Player(build_number, device_number);
			
			//��������
			players.add(new_player);
			
			//������� ������ ��� �������
			List<CellInfo> obj = get_table_obj(resource_id, new_player.get_correct_words(), new_player.get_incorrect_words(), new_player.get_score());
			
			//�������� ������
			table.add_object(obj);
		}
	}	
	
	//�������� ������ ����
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
		
		//������� �������
		try
		{
			is = asset_manager.open("word_game/" + filename);
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
				//�������� ���� �����
				String cur_word = line.toUpperCase();
				
				//�������� � ������
				ret.add(cur_word);
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
	private List<CellInfo> get_table_obj(int resource_id, String correct_words, String incorrect_words, int score)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(correct_words));
		obj.add(new CellInfo(incorrect_words));
		obj.add(new CellInfo("" + score));
		
		return obj;
	}	
	
	/*****	�������������� ������/���������/������������	*****/
	
	//����� ������
	public class Player
	{
		//�������� ������
		
		int build_number;	//����� ������
		int device_number;	//����� ������ � ������
		
		private List<String> correct_words = new ArrayList<String>();
		private List<String> incorrect_words = new ArrayList<String>();
		private int score;
		private String current_word;
		
		public Player(int build_number, int device_number)
		{
			this.build_number = build_number;
			this.device_number = device_number;
			current_word = "";
		}
		
		public void clear_current_word()
		{
			current_word = "";
		}
		
		public String get_current_word()
		{
			return current_word;
		}
		
		public void set_current_word(String word)
		{
			current_word = word;
		}
		
		public void add_new_word()
		{
			if(current_word.equals(""))
				return;
			
			//���� ����� ����� ����������� �������, �� �������� ���� � ������� ���� � ��������
			
			current_word = current_word.toUpperCase();
			
			for(int i = 0; i < words_list.size(); i++)
			{
				String word = words_list.get(i);
				if(word.equals(current_word))
				{
					add_score(1);
					correct_words.add(current_word);
					clear_current_word();
					return;
				}
			}
			add_score(-1);
			incorrect_words.add(current_word);
			clear_current_word();
			return;
			//�������� - ���� �� ����� ����� � ������
			/*if(words_list.contains(current_word))
			{
				correct_words.add(current_word);
				add_score(1);
			}				
			else
			{
				incorrect_words.add(current_word);
				add_score(-1);
			}				
			
			clear_current_word();*/
		}
		
		public String get_correct_words()
		{
			String correct_words_string = "";
			for(Iterator<String> it = correct_words.iterator(); it.hasNext(); )
			{
				String cur_word = it.next();
				
				if(!correct_words_string.equals(""))
					correct_words_string += "\n";
				correct_words_string += cur_word;
			}
			return correct_words_string;
		}
		
		public String get_incorrect_words()
		{
			String incorrect_words_string = "";
			for(Iterator<String> it = incorrect_words.iterator(); it.hasNext(); )
			{
				String cur_word = it.next();
				
				if(!incorrect_words_string.equals(""))
					incorrect_words_string += "\n";
				incorrect_words_string += cur_word;
			}
			return incorrect_words_string;
		}
		
		public int get_score()
		{
			return score;
		}
		
		public void add_score(int val)
		{
			score += val;
		}
	};
}
