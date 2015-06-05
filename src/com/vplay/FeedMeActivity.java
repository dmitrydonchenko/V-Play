package com.vplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vplay.TreasureActivity.Player;

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
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;


//���� - ������� ����
@SuppressLint("DefaultLocale") public class FeedMeActivity extends VPlayActivity
{
	
	/***** �������� ����	*****/
	
	//�������
	private VPlayTable table;
	
	//������ ������������ �������
	private List<Player> players = null;

	//��������� ����
	private EFeedmeGameState game_state = EFeedmeGameState.PREGAME;
	
	//��� ��������� ����� ��������� � ���������� � ���
	private List<Creature> creatures;
	
	//������� �������
	private Map<String, List<String>> food_chain;
	
	
	/****** ����������� ������	******/
	
	
	//����� ����� � ���� (������������� ����)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//����������� ������������� ����
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed_me);
		
		//��������� ����
		game_state = EFeedmeGameState.PREGAME;
		
		//��������� ������
		Button button = (Button) findViewById(R.id.feedme_startgame_button);
		button.setText("������ ����");
		
		//��������
		Context context = this;
		
		//�� xml ����� �������� ���� ������ �������
		int item_layout_id = R.layout.activity_feed_me_tableitem;
		
		//���������� � ������� ������ �������
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.feedme_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_successful_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_not_successful_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_points_text));
		
		//�� LinearLayout � ������� ����� ��������� �������
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.feedme_main_layout);
		
		//���������� � ��������� �������
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("�����", 1, 300));
		header_info.add(new HeaderCellInfo("�������� ���������", 1, 600));
		header_info.add(new HeaderCellInfo("��������� ���������", 1, 600));
		header_info.add(new HeaderCellInfo("����", 1, 300));
		
		//������� ��� ������������ �� ��������� ��������� ���������
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//�������� �������
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//������������������� �������
		try
		{
			init_game();
		}
		catch(UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//��� ������� �� ������
	public void on_click_feedme_startgame_button(View v)
	{
		//�������� ������
		Button button = (Button) findViewById(R.id.feedme_startgame_button);
		
		//� ����������� �� ��������� ����
		if(game_state.equals(EFeedmeGameState.PREGAME))
		{
			game_state = EFeedmeGameState.GAME;
			button.setText("��������� ����");
		}
		else if(game_state.equals(EFeedmeGameState.GAME))
		{
			game_state = EFeedmeGameState.POSTGAME;
			button.setText("��������");
		}
		else if(game_state.equals(EFeedmeGameState.POSTGAME))
		{
			game_state = EFeedmeGameState.PREGAME;
			button.setText("������ ����");
			
			//�������� ������� � ������������
			clear_table();
			
			//������������������� ������� �� �����
			try
			{
				init_game();
			}
			catch(UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	
	
	/****** ������ ��� ������ � �������� � ������� ���������������	
	 * @throws UnsupportedEncodingException *****/
	
	
	
	//��������� ������� ��������� ������� ��� ���� � ������ �������������
	private void init_game() throws UnsupportedEncodingException
	{
		//������������� �������
		init_creatures();
		
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
			
			//������� ������ ��� �������
			List<CellInfo> obj = get_table_obj(resource_id, "", "", "0");
			
			//�������� ������
			table.add_object(obj);
			
			//������������ ������ ������
			Player new_player = new Player(build_number, device_number);

			//��������
			players.add(new_player);
		}
	}
	
	//��������� ����� ������ ��� �������
	private List<CellInfo> get_table_obj(int resource_id, String successful_feed, String not_successful_feed, String points)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(successful_feed));
		obj.add(new CellInfo(not_successful_feed));
		obj.add(new CellInfo(points));
		
		return obj;
	}
	
	//�������� �������
	private void clear_table()
	{
		table.clear_table();
		players = null;
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
			cur_item.set(1, new CellInfo(player.get_all_successful_fed()));
			cur_item.set(2, new CellInfo(player.get_all_not_successful_fed()));
			cur_item.set(3, new CellInfo(player.scores + ""));
			
			//�������� ������ � �������
			table.set_item_params_list(i, cur_item);
		}
	}
	
	//�������� ���������� � ������� �������
	private void init_food_chain() throws UnsupportedEncodingException
	{
		//��������� ��������� ��� ������� ����
		food_chain = new HashMap<String, List<String>>();
		
		//��������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//��������� ���������
		Context context = app.getApplicationContext();
		
		//��������� ������
		AssetManager asset_manager = context.getAssets();
		
		//����� ��� ��������� ������ ������
		InputStream is = null;
		
		//������ ������ �����
		BufferedReader reader;
		
		//������ ��� ������ ������ ���������
		String line;
		
		//������� ���� � ��������� ��������
		try
		{
			is = asset_manager.open("feed_me_game/food_chain.txt");
		}
		catch(IOException e)
		{
			DebugClass.message("����������� ���� � �������� ���������");
			return;
		}
		
		//������ ������� �� �����
		reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		try
		{
			while((line = reader.readLine()) != null)
			{
				//�������� ������� ��� ������ ��������
				
				//������� ������ �� �����
				String [] words = line.split("\\s+");
				
				//������ ����� - ��� ���
				String who_eat = words[0].toLowerCase();
				
				//��������� - ��� �� ���
				List<String> food = new ArrayList<String>();
				for(int i = 1; i < words.length; i++)
				{
					food.add(words[i].toLowerCase());
				}
				
				//�������� � ������� ����
				food_chain.put(who_eat, food);
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
	
	
	//�������� ��� ��������
	private List<String> get_food_for_creature(String name)
	{
		//���� ����� �������� �� ����� ����-�� ������
		if(!food_chain.containsKey(name))
			return new ArrayList<String>();
		
		//������� ������� ���
		return food_chain.get(name);
	}

	//�������� ������� �� ������
	private void add_creatures_from_card_group(String group_name, int lives)
	{
		//�������� ��������� �� ����������
		VPlayApplication app = VPlayApplication.get_instance();
		
		//�������� ��������� �� ���� ������
		VPlayDatabase db = app.get_database();
		
		//�������� �� ���������
		int category_id = db.get_category_id(group_name);
		if(category_id == -1)
		{
			DebugClass.message("������: ����������� ���������: " + group_name);
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
			
			//�������� ��� ��������
			List<String> food = get_food_for_creature(name);
			
			//�������� ��������
			creatures.add(new Creature(name, rfid, food, lives));
		}
	}
	
	//������������� �������
	private void init_creatures() throws UnsupportedEncodingException
	{
		//������������������� ������� �������
		init_food_chain();
		
		//������� ������ �������
		creatures = new ArrayList<Creature>();
		
		//���������� ������� �� ������ ���������
		add_creatures_from_card_group("�������", 1);
		add_creatures_from_card_group("����������", 1);
		add_creatures_from_card_group("��� ����������", -1);
	}
	
	//�������� ��������
	private Creature get_creature(String rfid)
	{
		for(Iterator<Creature> it = creatures.iterator(); it.hasNext(); )
		{
			//�������� ��������
			Creature creature = it.next();
			
			//���� ��� ����� � �������� - ����������
			if(creature.rfid == null)
				continue;
			
			//������ �����
			for(Iterator<String> it_rfid = creature.rfid.iterator(); it_rfid.hasNext(); )
			{
				//������� rfid
				String cur_rfid = it_rfid.next();
				
				//���� ������
				if(cur_rfid.equals(rfid))
					return creature;
			}
		}
		
		return null;
	}
	
	//����� �� ������ ���� �������� ������
	private boolean is_may_eat(String who_eat, String what_eat)
	{
		//���� ����� �������� �� ����� ����-�� ������
		if(!food_chain.containsKey(who_eat))
			return false;
		
		//�������� ������ ���
		List<String> food = food_chain.get(who_eat);
		
		//���� ����� ������
		if(food.contains(what_eat))
			return true;
		return false;
	}
	
	
	//���� �� ����� �������� � ������� ����
	private boolean is_in_food_chain(String name)
	{
		//���� �� ����� ��������
		if(food_chain.containsKey(name))
			return true;
		
		//���� �� ����� ���
		Collection<List<String> > food_chain_values = food_chain.values();
		for(Iterator<List<String>> it = food_chain_values.iterator(); it.hasNext(); )
		{
			//�������� ������ ��� ��� ���� ���������
			List<String> cur_list = it.next();
			
			//�������� ��� � ������� ������� ��������
			for(Iterator<String> it_food = cur_list.iterator(); it_food.hasNext(); )
			{
				String cur_name = it_food.next();
				
				//���� �������
				if(cur_name.equals(name))
				{
					//������� ��� �������
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	/****** ������ ��� ������ � ��������	*****/
	

	//������� ������ �� ������
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{
		//���� ��������� �� � ���� - ������� �������
		if(!game_state.equals(EFeedmeGameState.GAME))
			return;
		
		//���� ������ ������ �������� �� 1 ��� 2
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON) && !key.equals(EDeviceKey.THREE_BUTTON))
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
			usb_service.send_display_string_command(build_number, device_number, "���-�� �����: " + String.valueOf(player.scores));
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
			
			//��������� ���������� � ������� ���������
			usb_service.send_display_string_command(build_number, device_number, player.player_state_string);
		}
	}


	//��������� ��������� rfid �����
	@Override
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
		//���� ��������� �� � ���� - ������� �������
		if(!game_state.equals(EFeedmeGameState.GAME))
			return;
		
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
		
		//�������� ��������, ��������������� rfid �����
		Creature creature = get_creature(rfid);
		if(creature == null)
		{
			DebugClass.message("�������� � ����� ������ �� ����������");
			return;
		}
		
		//���� ������ �������� ��� � ������� - ������� �� ����
		if(!is_in_food_chain(creature.name))
		{
			DebugClass.message("������ �������� ��� � ������� ����");
			return;
		}
			
		//� ����������� �� ��������� ������ ������� ��������
		if(player.player_state.equals(Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT))
		{
			//����� ����, ��� ����� ����
			
			//��������� - ����� ��� ���
			if(creature.is_dead())
			{
				//�������� ��������� �������� ������
				player.add_action(new Player.PlayerAction(creature.name, "", Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_DEAD));
				player.player_state_string = "(�����) ���?";
				
				//��������� ������ �� ������� �������� ����������
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				//������ ����
				player.add_scores(-1);
				
				//�������� �������
				update_table();
				
				return;
			}
			
			//��������� - ��� ��� ���
			if(creature.food_cnt >= 3)
			{
				//�������� ��������� �������� ������
				player.add_action(new Player.PlayerAction(creature.name, "", Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_FULL));
				player.player_state_string = "(���) ���?";
				
				//��������� ������ �� ������� �������� ����������
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				//������ ����
				player.add_scores(-1);
				
				//�������� �������
				update_table();
				
				return;
			}
			
			//��������� � ������� �� ������ ���������
			player.who_eat = creature;
			player.player_state = Player.EPlayerActionState.READ_RFID_WHAT_SHOULD_BE_EAT;
			player.player_state_string = "����?";
			
			//��������� ������ �� ������� �������� ����������
			usb_service.send_led_flash_command(build_number, device_number, 0);
		}
		else if(player.player_state.equals(Player.EPlayerActionState.READ_RFID_WHAT_SHOULD_BE_EAT))
		{
			//����� ����, ��� ����� �������
			
			//��������� - ����� ��� ���
			if(creature.is_dead())
			{
				//�������� ��������� �������� ������
				player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.WHAT_SHOULD_BE_EAT_DEAD));
				player.player_state_string = "(�����) ���?";
				
				//��������� ������ �� ������� �������� ����������
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
				
				//������ ����
				player.add_scores(-1);
				
				//�������� �������
				update_table();
				
				return;
			}
			
			//��������� - ����� �� ��� ���� ��� ���
			if(!is_may_eat(player.who_eat.name, creature.name))
			{
				//�������� ��������� �������� ������
				player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_NOT_EAT));
				player.player_state_string = "(�� ���) ���?";
				
				//��������� ������ �� ������� �������� ����������
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
				
				//������ ����
				player.add_scores(-1);
				
				//�������� �������
				update_table();
				
				return;
			}
			
			//�������� ����� �������� ������ (��������)
			player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.SUCCESS));
			player.player_state_string = "(�����) ���?";
			
			//������� ����������� ���������
			player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
			
			//�������� ����
			player.add_scores(3);
			
			//����� �������� ������� ���� �������
			creature.damage();
			
			//�������� ������� ��������, ������� ���
			player.who_eat.food_cnt++;
			
			//��������
			player.who_eat = null;
			
			//��������� ������ �� ������� �������� ����������
			usb_service.send_led_flash_command(build_number, device_number, 0);
		}
		
		//�������� �������
		update_table();
	}
	
	
	
	
	
	
	
	/****** �������������� ���������/������/������������	*****/
	
	//����� ������
	private static class Player
	{
		//��������� �������� ������
		public static enum EPlayerActionState
		{
			READ_RFID_WHO_SHOULD_EAT,		//���������� ����� ����, ��� ������ ����
			READ_RFID_WHAT_SHOULD_BE_EAT	//���������� ����� ����, ��� ������ ���� ������
		};
		
		//�������� ������
		public static class PlayerAction
		{
			//�������� ������ ��������
			public static enum EPlayerActionInfo
			{
				SUCCESS,					//�������� ���������
				WHO_SHOULD_EAT_DEAD,		//���� ����� ������� �����
				WHO_SHOULD_EAT_FULL,		//���� ����� ������� ���
				WHO_SHOULD_EAT_NOT_EAT,		//���� ����� ������� �� ����� ��� ����
				WHAT_SHOULD_BE_EAT_DEAD		//��� ����� ��������� ��� �������
			};
			
			//���������� � �������� ������
			public String who_should_eat;				//���� ����� ���������
			public String what_should_be_eat;			//��� ����� ���������
			public EPlayerActionInfo action_state;		//����� ��������
			
			//�����������
			PlayerAction(String who_should_eat, String what_should_be_eat, EPlayerActionInfo action_state)
			{
				this.who_should_eat = who_should_eat;
				this.what_should_be_eat = what_should_be_eat;
				this.action_state = action_state;
			}
		};
		
		//�������� ������
		public EPlayerActionState player_state;		//��������� ������
		public int scores;							//���-�� ����� ������
		public List<PlayerAction> player_actions;	//�������� ������
		
		public String player_state_string = "���?";	//��������� ������
		
		//���������� ��� �������� ����� �����������
		public Creature who_eat;			//�������� ������� ����� ����
		
		//���������� �� ����������
		public int build_number;		//����� ��������� ������ ������
		public int device_number;		//����� ������ � ���������
		
		//�����������
		public Player(int build_number, int device_number)
		{
			//������������� �������� ������
			player_actions = new ArrayList<PlayerAction>();
			
			//������������� �����
			scores = 0;
			
			//��������� ������ �� ���������
			player_state = EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
			
			//���������� �� ����������
			this.build_number = build_number;
			this.device_number = device_number;
		}
		
		//�������� ��������
		public void add_action(String who_should_eat, String what_should_be_eat, PlayerAction.EPlayerActionInfo action_state)
		{
			player_actions.add(new PlayerAction(who_should_eat, what_should_be_eat, action_state));
		}
		public void add_action(PlayerAction player_action)
		{
			player_actions.add(player_action);
		}
		
		//�������� �����
		public void add_scores(int val)
		{
			scores += val;
		}
		
		//�������� ������ �������� �������� � ���� ������
		public String get_all_successful_fed()
		{
			String ret = "";
			
			//������ �� ���� ��������� ������
			for(Iterator<PlayerAction> it = player_actions.iterator(); it.hasNext(); )
			{
				//�������� ������� ��������
				PlayerAction cur_player_action = it.next();
				
				//�������� ��������� ��������
				PlayerAction.EPlayerActionInfo action_info = cur_player_action.action_state;
				
				//���� ��������� ��������
				if(action_info.equals(PlayerAction.EPlayerActionInfo.SUCCESS))
				{
					//���� �� ������ �������� ���������
					if(!ret.equals(""))
					{
						//�������� �����������
						ret += '\n';
					}
					
					//�������� ���������� � ��������
					ret += cur_player_action.who_should_eat + "->" + cur_player_action.what_should_be_eat;
				}
			}
			
			return ret;
		}
		
		//�������� ������ ���� �� �������� �������� � ���� ������
		public String get_all_not_successful_fed()
		{
			String ret = "";
			
			//������ �� ���� ��������� ������
			for(Iterator<PlayerAction> it = player_actions.iterator(); it.hasNext(); )
			{
				//�������� ������� ��������
				PlayerAction cur_player_action = it.next();
				
				//�������� ��������� ��������
				PlayerAction.EPlayerActionInfo action_info = cur_player_action.action_state;
				
				//���� ��������� �� ��������
				if(!action_info.equals(PlayerAction.EPlayerActionInfo.SUCCESS))
				{
					//���� �� ������ �� �������� ���������
					if(!ret.equals(""))
					{
						//�������� �����������
						ret += '\n';
					}
					
					//� ����������� �� ���� ������������ - ������� ��������
					if(action_info.equals(PlayerAction.EPlayerActionInfo.WHAT_SHOULD_BE_EAT_DEAD))
					{
						//��, ��� ������ ���� ������� ������
						
						ret += cur_player_action.who_should_eat + "->" + cur_player_action.what_should_be_eat + "(�����)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_DEAD))
					{
						//���, ��� ������ ���� ����
						
						ret += cur_player_action.who_should_eat + "(�����)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_FULL))
					{
						//���, ��� ������ ���� ���
						
						ret += cur_player_action.who_should_eat + "(���)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_NOT_EAT))
					{
						//���, ��� ������ ���� �� ����� ��� ����
						
						ret += cur_player_action.who_should_eat + "(���������)->" + cur_player_action.what_should_be_eat;
					}
				}
			}
			
			return ret;
		}
	};
	
	
	//�������� �������� ��� ��������
	private class Creature
	{
		//�������� ��������
		List<String> rfid;				//����� ��������
		String name;					//������������ ��������
		private boolean is_dead;		//������� ������
		private int lives;				//�����
		int food_cnt;					//���-�� ��������� ���
		List<String> food;				//������ ������������ ���
		
		//�����������
		Creature(String name, List<String> rfid, List<String> food, int lives)
		{
			//�� ��������� ������ �� ����
			food_cnt = 0;
			
			//�� ��������� ���
			is_dead = false;
			
			//��������� ���������
			this.name = name;
			this.rfid = rfid;
			this.food = food;
			this.lives = lives;
		}
		
		void damage()
		{
			if(lives == -1)
				return;
			
			lives--;
			if(lives <= 0)
				is_dead = true;
		}
		
		boolean is_dead()
		{
			return is_dead;
		}
	};
	
	//��������� ����
	private static enum EFeedmeGameState
	{
		PREGAME,	//���� ��� �� ��������
		GAME,		//���� ����
		POSTGAME	//���� �����������
	};
}
