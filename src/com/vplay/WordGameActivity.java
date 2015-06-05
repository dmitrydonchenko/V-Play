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
	
/***** Свойства окна	*****/
	
	//Таблица
	private VPlayTable table;
	
	//Список участвующих пультов
	private List<Player> players = null;
	
	//Список слов из темы
	List<String> words_list = null;
	
	//Все доступные символы
	private List<String> letter_value_list = null;
	private List<List<String>> letter_rfid_list = null;	
	
	//Место для хранения списка спиннера и соответствующих файлов
	HashMap<String, String> spinner_map = new HashMap<String, String>();
	
	/***** Стандартные методы	*****/
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_word_game);
		
		//Контекст
		Context context = this;
		
		//ИД xml файла внешнего вида строки таблицы
		int item_layout_id = R.layout.activity_wordgame_tableitem;
		
		//Информация о ячейках строки таблицы
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.wordgame_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_correct_words_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_incorrect_words_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.wordgame_points_text));
		
		//ИД LinearLayout в который будет вставлена таблица
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.word_game_table_layout);
		
		//Информация о заголовке таблицы
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("Игрок", 1, 300));
		header_info.add(new HeaderCellInfo("Отгаданные слова", 1, 500));
		header_info.add(new HeaderCellInfo("Неправильные слова", 1, 600));
		header_info.add(new HeaderCellInfo("Очки", 1, 300));
		
		//Функции для реагирования на изменение состояния чекбоксов
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//Создание таблицы
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
				
		//Получить информацию о теме игры
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
			DebugClass.message("Не удалось получить информацию о теме игры");
			return;
		}
		
		//Получить список слов
		words_list = get_game_content_list(theme_name);

		//Если ошибка - ничего не делать
		if(words_list.size() == 0)
			return;
		
		//Проинициализировать игру
		init_game(game_theme_name);
	}	
	
	/*****	Нажатие на кнопки	*****/
	
	public void on_click_wordgame_end_game_button(View v)
	{
		this.finish();
	}	
		
	/****** Методы для работы с пультами	*****/	

	//Нажатие кнопки на пульте
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{
		//Если нажата кнопка отличная от 1 или 2 или DEL или SEND
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON) && !key.equals(EDeviceKey.DELETE_BUTTON) && !key.equals(EDeviceKey.SEND_BUTTON) && !key.equals(EDeviceKey.THREE_BUTTON))
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
		else if(key.equals(EDeviceKey.TWO_BUTTON))
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
			
			//Отправить информаци
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
		}
		else if(key.equals(EDeviceKey.DELETE_BUTTON))
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
			
			//Очистить текущее слово
			player.clear_current_word();
			
			//Получить указатель на usb сервис
			UsbService usb_service = UsbService.get_instance();
			
			//Отправить информаци
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
		}
		else if(key.equals(EDeviceKey.SEND_BUTTON))
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
			
			//Добавить текущее слово
			player.add_new_word();
			
			//Получить указатель на usb сервис
			UsbService usb_service = UsbService.get_instance();
			
			//Отправить информаци
			usb_service.send_display_string_command(build_number, device_number, player.get_current_word());
			
			update_table();
		}
		else if(key.equals(EDeviceKey.THREE_BUTTON))
		{
			//Посмотреть количество очков
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
			
			//Отправить информаци
			usb_service.send_display_string_command(build_number, device_number, "Кол-во очков: " + String.valueOf(player.get_score()));
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
		String word = get_letter(rfid);
		if(word == null)
		{
			DebugClass.message("Символа с такой меткой не существует");
			return;
		}	
		
		//Добавить текущему игроку эту букву
		player.set_current_word(word);
		
		//Выслать обновленную строку
		usb_service.send_display_string_command(build_number, device_number, player.current_word);
		
		//Обновить таблицу
		update_table();
	}	
	
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
			cur_item.set(1, new CellInfo(player.get_correct_words()));
			cur_item.set(2, new CellInfo(player.get_incorrect_words()));
			cur_item.set(3, new CellInfo(player.get_score() + ""));
			
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
			name = name.toUpperCase();
			
			//Добавить существо
			letter_value_list.add(name);
			letter_rfid_list.add(rfid);
		}
	}	
	
	//Заполнить таблицу исходными данными для игры и другие инициализации
	private void init_game(String theme_name)
	{
		//Проинициализировать все доступные символы
		init_letters(theme_name);
		
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
			
			//Сформировать нового игрока
			Player new_player = new Player(build_number, device_number);
			
			//Добавить
			players.add(new_player);
			
			//Создать запись для таблицы
			List<CellInfo> obj = get_table_obj(resource_id, new_player.get_correct_words(), new_player.get_incorrect_words(), new_player.get_score());
			
			//Добавить запись
			table.add_object(obj);
		}
	}	
	
	//Получить список слов
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
		
		//Открыть словарь
		try
		{
			is = asset_manager.open("word_game/" + filename);
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
				//Получено след слово
				String cur_word = line.toUpperCase();
				
				//Добавить в список
				ret.add(cur_word);
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
	private List<CellInfo> get_table_obj(int resource_id, String correct_words, String incorrect_words, int score)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(correct_words));
		obj.add(new CellInfo(incorrect_words));
		obj.add(new CellInfo("" + score));
		
		return obj;
	}	
	
	/*****	Дополнительные классы/структуры/перечисления	*****/
	
	//Класс игрока
	public class Player
	{
		//Свойства игрока
		
		int build_number;	//Номер набора
		int device_number;	//Номер пульта в наборе
		
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
			
			//Если такое слово указывается впервые, то добавить либо в коррект либо в инкорект
			
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
			//Проверка - есть ли такое слово в списке
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
