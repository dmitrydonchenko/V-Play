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

//Окно игры
public class TreasureActivity extends VPlayActivity
{

	/***** Свойства окна	*****/
	
	//Таблица
	private VPlayTable table;
	
	//Список участвующих пультов
	private List<Player> players = null;
		
	//Список кладов
	List<String> treasure_list = null;
	
	//Все доступные символы
	private List<String> letter_value_list = null;
	private List<List<String>> letter_rfid_list = null;
	
	/***** Стандартные методы	*****/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_treasure);
	
		//Контекст
		Context context = this;
		
		//ИД xml файла внешнего вида строки таблицы
		int item_layout_id = R.layout.activity_treasure_tableitem;
		
		//Информация о ячейках строки таблицы
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.treasure_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.treasure_treasure_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.treasure_player_state_text));
		
		//ИД LinearLayout в который будет вставлена таблица
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.treasure_table_layout);
		
		//Информация о заголовке таблицы
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("Игрок", 1, 300));
		header_info.add(new HeaderCellInfo("Клад", 1, 500));
		header_info.add(new HeaderCellInfo("Состояние игрока", 1, 600));
		
		//Функции для реагирования на изменение состояния чекбоксов
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//Создание таблицы
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//Получить информацию о теме игры
		String game_theme_string = "";
		Bundle extras = getIntent().getExtras();
		if(extras != null)
			game_theme_string = extras.getString("game_theme");
		else
		{
			DebugClass.message("Не удалось получить информацию о теме игры");
			return;
		}
		
		//Получить список кладов
		treasure_list = get_game_content_list(game_theme_string);

		//Если ошибка - ничего не делать
		if(treasure_list.size() == 0)
			return;
		
		//Проинициализировать игру
		init_game();
	}
	
	
	
	
	
	/*****	Нажатие на кнопки	*****/
	
	public void on_click_treasure_end_game_button(View v)
	{
		this.finish();
	}
	
	
	
	
	
	/****** Методы для работы с пультами	*****/
	

	//Нажатие кнопки на пульте
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{	
		//Если нажата кнопка отличная от 1 или 2
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON))
			return;
		
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Если устройство не учавствует в игре - ничего не делать
		if(!db.get_device_activity(build_number, device_number))
			return;
		
		if(key.equals(EDeviceKey.ONE_BUTTON))
		{
			//Получить указатель на usb сервис
			UsbService usb_service = UsbService.get_instance();
			
			//Отправить сигнал чтения метки
			usb_service.send_read_rf_id_command(build_number, device_number);
		}
		else
		{
			//Найти игрока, который нажал
			Player player = null;
			for(Iterator<Player> it = players.iterator(); it.hasNext(); )
			{
				//Получить текущего игрока
				Player cur_player = it.next();
				
				//Если найден - сохранить и выйти из цикла
				if(cur_player.build_number == build_number && cur_player.device_number == device_number)
				{
					player = cur_player;
					break;
				}
			}
			if(player == null)
				return;
			
			//Получить указатель на usb сервис
			UsbService usb_service = UsbService.get_instance();
			
			//Отправить информацию о кладе
			usb_service.send_display_string_command(build_number, device_number, player.get_closed_word());
		}
	}


	//Получение считанной rfid метки
	@Override
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Получить указатель на usb сервис
		UsbService usb_service = UsbService.get_instance();
		
		//Если устройство не учавствует в игре - ничего не делать
		if(!db.get_device_activity(build_number, device_number))
			return;
		
		//Найти игрока, который нажал
		Player player = null;
		for(Iterator<Player> it = players.iterator(); it.hasNext(); )
		{
			//Получить текущего игрока
			Player cur_player = it.next();
			
			//Если найден - сохранить и выйти из цикла
			if(cur_player.build_number == build_number && cur_player.device_number == device_number)
			{
				player = cur_player;
				break;
			}
		}
		if(player == null)
			return;
		
		//Получить символ, соответствующее rfid метке
		String letter = get_letter(rfid);
		if(letter == null)
		{
			DebugClass.message("Символа с такой меткой не существует");
			return;
		}	
		
		//Добавить текущему игроку эту букву и если были изменения - отправить новую строку на пульт - иначе красный сигнал
		if(player.new_letter(letter))
		{
			//Выслать обновленную строку
			usb_service.send_display_string_command(build_number, device_number, player.get_closed_word());
		}
		else
		{
			//Красный сигнал
			usb_service.send_led_flash_command(build_number, device_number, 1);
		}
		
		//Обновить таблицу
		update_table();
	}
	
	
	
	
	
	/*****	Вспомогательные методы	*****/

	
	//Обновить таблицу
	private void update_table()
	{
		Iterator<Player> it = players.iterator();
		for(int i = 0; i < table.getCount(); i++)
		{
			//Получить текущую строку в таблице
			List<CellInfo> cur_item = table.get_item_params_list(i);
			
			//Получить текущего игрока
			Player player = it.next();
			
			//Обновить данные
			cur_item.set(1, new CellInfo(player.get_treasure()));
			cur_item.set(2, new CellInfo(player.get_closed_word()));
			
			//Обновить данные в таблице
			table.set_item_params_list(i, cur_item);
		}
	}
	
	
	//Получить букву из кода
	private String get_letter(String rfid)
	{
		Iterator<String> it_values = letter_value_list.iterator();
		Iterator<List<String>> it_rfids = letter_rfid_list.iterator();
		
		for(; it_values.hasNext() && it_rfids.hasNext(); )
		{
			//Получить символ
			String cur_letter = it_values.next();
			
			//Получить список rfid для буквы
			List<String> rfid_list = it_rfids.next();
		
			//Искать метку
			for(Iterator<String> it_rfid = rfid_list.iterator(); it_rfid.hasNext(); )
			{
				//Текущий rfid
				String cur_rfid = it_rfid.next();
				
				//Если найден
				if(cur_rfid.equals(rfid))
					return cur_letter;
			}
		}
		
		return null;
	}
	

	
	//Инициализация букв
	private void init_letters(String category_name)
	{
		//Создать список символов
		letter_value_list = new ArrayList<String>();
		letter_rfid_list = new ArrayList<List<String>>();
		
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Получить ИД категории
		int category_id = db.get_category_id(category_name);
		if(category_id == -1)
		{
			DebugClass.message("Ошибка: Отсутствует категория: " + category_name);
			return;
		}
		
		//Получить таблицу с информацией об элементах набора карт
		List<String> content_names = db.get_content_names(category_id);
		
		//Добавлять нужную информацию в список
		for(Iterator<String> it = content_names.iterator(); it.hasNext(); )
		{
			//Получить текущий объект
			String cur_content = it.next();
			
			//Получить нужную информацию
			String name = cur_content;
			
			//Получить ИД текущего контента
			int cur_content_id = db.get_content_id(cur_content);
			
			//Получить список RFID для контента
			List<String> rfid = db.get_rfid_list(cur_content_id);
			
			//К нижнему регистру
			name = name.toLowerCase();
			
			//Добавить существо
			letter_value_list.add(name);
			letter_rfid_list.add(rfid);
		}
	}
	
	
	//Заполнить таблицу исходными данными для игры и другие инициализации
	private void init_game()
	{
		//Проинициализировать все доступные символы
		init_letters("Буквы и цифры");
		
		//Создать список учавствующих пультов
		players = new ArrayList<Player>();
		
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Получить таблицу устройств
		List<Map<String, Object>> table_data = db.get_all_device_info();
		
		//Цикл прохода по таблице
		for(Iterator<Map<String, Object>> it = table_data.iterator(); it.hasNext(); )
		{
			//Получить информацию о текущем объекте
			Map<String, Object> cur_map = it.next();
			
			//Получить данные
			boolean activity = (Boolean) cur_map.get("activity");
			int device_number = (Integer) cur_map.get("number");
			int build_number = (Integer) cur_map.get("set");
			
			//Если пульт не активен - пропустить
			if(!activity)
				continue;
			
			//Получить наименование файла с изображением для пульта
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//Получить ИД ресурса с изображением
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//Получить кол-во кладов в игре
			int treasure_cnt = treasure_list.size();
			
			//Присвоить случайный клад текущему игроку
			Random random = new Random();
			int treasure_idx = random.nextInt(treasure_cnt - 0 + 1) + 0;
			String cur_treasure = treasure_list.get(treasure_idx);
			
			//Сформировать нового игрока
			Player new_player = new Player(build_number, device_number, cur_treasure);
			
			//Добавить
			players.add(new_player);
			
			//Создать запись для таблицы
			List<CellInfo> obj = get_table_obj(resource_id, cur_treasure, new_player.get_closed_word());
			
			//Добавить запись
			table.add_object(obj);
		}
	}
	
	
	//Получить список кладов
	private List<String> get_game_content_list(String filename)
	{
		//Список кладов
		List<String> ret = new ArrayList<String>();
		
		//Получение указателя на приложение
		VPlayApplication app = VPlayApplication.get_instance();
				
		//Получение контекста
		Context context = app.getApplicationContext();
		
		//Получение ассета
		AssetManager asset_manager = context.getAssets();
		
		//Поток для обработки файлов ассета
		InputStream is = null;
		
		//Объект чтения файла
		BufferedReader reader = null;
		
		//Строка для чтения файлов построчно
		String line;
		
		//Открыть файл с цепочками пищевыми
		try
		{
			is = asset_manager.open("treasure_game/" + filename);
		}
		catch(IOException e)
		{
			DebugClass.message("Не найден файл с темой игры");
			return ret;
		}
		
		//Чтение записей из файла
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
				//Получен след клад
				String treasure_name = line;
				
				//Добавить в список
				ret.add(treasure_name);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		//Закрыть файл с наименованиями изображения для пультов
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
	
	
	//Составить новый объект для таблицы
	private List<CellInfo> get_table_obj(int resource_id, String treasure, String closed_treasure)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(treasure));
		obj.add(new CellInfo(closed_treasure));
		
		return obj;
	}
	
	
	
	
	/*****	Дополнительные классы/структуры/перечисления	*****/
	
	//Класс игрока
	public class Player
	{
		//Свойства игрока
		
		int build_number;	//Номер набора
		int device_number;	//Номер пульта в наборе
		
		private String treasure;	//Присвоеный клад
		private String closed;		//Закрытое слово
		
		public Player(int build_number, int device_number, String treasure_string)
		{
			this.build_number = build_number;
			this.device_number = device_number;
			set_treasure(treasure_string);
		}
		
		//Задать слово
		public void set_treasure(String s)
		{
			treasure = s;
			closed = "";
			for(int i = 0; i < s.length(); i++)
				closed += "*";
		}
		
		//Получить закрытое слово
		public String get_closed_word()
		{
			return closed;
		}
		
		//Получить клад
		public String get_treasure()
		{
			return treasure;
		}
		
		//Считана новая буква
		public boolean new_letter(String ch_string)
		{
			//К нижнему регистру
			ch_string = ch_string.toLowerCase();
			char ch = ch_string.toCharArray()[0];
			
			//Получить слово в виде массива символов
			char [] treasure_char = treasure.toLowerCase().toCharArray();
			char [] closed_char = closed.toLowerCase().toCharArray();
			
			//Искать
			boolean is_find = false;
			closed = "";
			for(int i = 0; i < treasure_char.length; i++)
			{
				//Если найдено
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
