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


//Игра - Покорми меня
@SuppressLint("DefaultLocale") public class FeedMeActivity extends VPlayActivity
{
	
	/***** Свойства игры	*****/
	
	//Таблица
	private VPlayTable table;
	
	//Список учавствующих пультов
	private List<Player> players = null;

	//Состояние игры
	private EFeedmeGameState game_state = EFeedmeGameState.PREGAME;
	
	//Все доступные живые организмы и информация о них
	private List<Creature> creatures;
	
	//Пищевые цепочки
	private Map<String, List<String>> food_chain;
	
	
	/****** Стандартные методы	******/
	
	
	//Точка входа в игру (инициализация игры)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Стандартная инициализация окна
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed_me);
		
		//Состояние игры
		game_state = EFeedmeGameState.PREGAME;
		
		//Параметры кнопки
		Button button = (Button) findViewById(R.id.feedme_startgame_button);
		button.setText("Начать игру");
		
		//Контекст
		Context context = this;
		
		//ИД xml файла внешнего вида строки таблицы
		int item_layout_id = R.layout.activity_feed_me_tableitem;
		
		//Информация о ячейках строки таблицы
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.feedme_device_image));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_successful_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_not_successful_text));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.feedme_points_text));
		
		//ИД LinearLayout в который будет вставлена таблица
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.feedme_main_layout);
		
		//Информация о заголовке таблицы
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("Игрок", 1, 300));
		header_info.add(new HeaderCellInfo("Успешные кормления", 1, 600));
		header_info.add(new HeaderCellInfo("Неудачные кормления", 1, 600));
		header_info.add(new HeaderCellInfo("Очки", 1, 300));
		
		//Функции для реагирования на изменение состояния чекбоксов
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		
		//Создание таблицы
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//Проинициализировать таблицу
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
	
	
	//При нажатии на кнопку
	public void on_click_feedme_startgame_button(View v)
	{
		//Получить кнопку
		Button button = (Button) findViewById(R.id.feedme_startgame_button);
		
		//В зависимости от состояния игры
		if(game_state.equals(EFeedmeGameState.PREGAME))
		{
			game_state = EFeedmeGameState.GAME;
			button.setText("Завершить игру");
		}
		else if(game_state.equals(EFeedmeGameState.GAME))
		{
			game_state = EFeedmeGameState.POSTGAME;
			button.setText("Очистить");
		}
		else if(game_state.equals(EFeedmeGameState.POSTGAME))
		{
			game_state = EFeedmeGameState.PREGAME;
			button.setText("Начать игру");
			
			//Очистить таблицу с результатами
			clear_table();
			
			//Проинициализировать таблицу по новой
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
	
	
	
	
	
	
	/****** Методы для работы с таблицей и прочими инициализациями	
	 * @throws UnsupportedEncodingException *****/
	
	
	
	//Заполнить таблицу исходными данными для игры и другие инициализации
	private void init_game() throws UnsupportedEncodingException
	{
		//Инициализация существ
		init_creatures();
		
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
			
			//Создать запись для таблицы
			List<CellInfo> obj = get_table_obj(resource_id, "", "", "0");
			
			//Добавить запись
			table.add_object(obj);
			
			//Сформировать нового игрока
			Player new_player = new Player(build_number, device_number);

			//Добавить
			players.add(new_player);
		}
	}
	
	//Составить новый объект для таблицы
	private List<CellInfo> get_table_obj(int resource_id, String successful_feed, String not_successful_feed, String points)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(resource_id));
		obj.add(new CellInfo(successful_feed));
		obj.add(new CellInfo(not_successful_feed));
		obj.add(new CellInfo(points));
		
		return obj;
	}
	
	//Очистить таблицу
	private void clear_table()
	{
		table.clear_table();
		players = null;
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
			cur_item.set(1, new CellInfo(player.get_all_successful_fed()));
			cur_item.set(2, new CellInfo(player.get_all_not_successful_fed()));
			cur_item.set(3, new CellInfo(player.scores + ""));
			
			//Обновить данные в таблице
			table.set_item_params_list(i, cur_item);
		}
	}
	
	//Получить информацию о пищевой цепочке
	private void init_food_chain() throws UnsupportedEncodingException
	{
		//Составить контейнер для пищевой цепи
		food_chain = new HashMap<String, List<String>>();
		
		//Получение указателя на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получение контекста
		Context context = app.getApplicationContext();
		
		//Получение ассета
		AssetManager asset_manager = context.getAssets();
		
		//Поток для обработки файлов ассета
		InputStream is = null;
		
		//Объект чтения файла
		BufferedReader reader;
		
		//Строка для чтения файлов построчно
		String line;
		
		//Открыть файл с цепочками пищевыми
		try
		{
			is = asset_manager.open("feed_me_game/food_chain.txt");
		}
		catch(IOException e)
		{
			DebugClass.message("Отсутствует файл с пищевыми цепочками");
			return;
		}
		
		//Чтение записей из файла
		reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		try
		{
			while((line = reader.readLine()) != null)
			{
				//Получена цепочка для одного существа
				
				//Разбить строку на слова
				String [] words = line.split("\\s+");
				
				//Первое слово - кто ест
				String who_eat = words[0].toLowerCase();
				
				//Остальные - что он ест
				List<String> food = new ArrayList<String>();
				for(int i = 1; i < words.length; i++)
				{
					food.add(words[i].toLowerCase());
				}
				
				//Добавить в пищевую цепь
				food_chain.put(who_eat, food);
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
	}
	
	
	//Получить еду существа
	private List<String> get_food_for_creature(String name)
	{
		//Если такое существо не может кого-то съесть
		if(!food_chain.containsKey(name))
			return new ArrayList<String>();
		
		//Вернуть цепочку еды
		return food_chain.get(name);
	}

	//Добавить существ из списка
	private void add_creatures_from_card_group(String group_name, int lives)
	{
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Получить ИД категории
		int category_id = db.get_category_id(group_name);
		if(category_id == -1)
		{
			DebugClass.message("Ошибка: Отсутствует категория: " + group_name);
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
			
			//Получить еду существа
			List<String> food = get_food_for_creature(name);
			
			//Добавить существо
			creatures.add(new Creature(name, rfid, food, lives));
		}
	}
	
	//Инициализация существ
	private void init_creatures() throws UnsupportedEncodingException
	{
		//Проинициализировать пищевые цепочки
		init_food_chain();
		
		//Создать список существ
		creatures = new ArrayList<Creature>();
		
		//Добавление существ из разных категорий
		add_creatures_from_card_group("Хищники", 1);
		add_creatures_from_card_group("Травоядные", 1);
		add_creatures_from_card_group("Еда травоядных", -1);
	}
	
	//Получить существо
	private Creature get_creature(String rfid)
	{
		for(Iterator<Creature> it = creatures.iterator(); it.hasNext(); )
		{
			//Получить существо
			Creature creature = it.next();
			
			//Если нет метки у существа - пропустить
			if(creature.rfid == null)
				continue;
			
			//Искать метку
			for(Iterator<String> it_rfid = creature.rfid.iterator(); it_rfid.hasNext(); )
			{
				//Текущий rfid
				String cur_rfid = it_rfid.next();
				
				//Если найден
				if(cur_rfid.equals(rfid))
					return creature;
			}
		}
		
		return null;
	}
	
	//Может ли съесть одно существо другое
	private boolean is_may_eat(String who_eat, String what_eat)
	{
		//Если такое существо не может кого-то съесть
		if(!food_chain.containsKey(who_eat))
			return false;
		
		//Получить список еды
		List<String> food = food_chain.get(who_eat);
		
		//Если может съесть
		if(food.contains(what_eat))
			return true;
		return false;
	}
	
	
	//Есть ли такое существо в пищевой цепи
	private boolean is_in_food_chain(String name)
	{
		//Есть ли среди хищников
		if(food_chain.containsKey(name))
			return true;
		
		//Есть ли среди еды
		Collection<List<String> > food_chain_values = food_chain.values();
		for(Iterator<List<String>> it = food_chain_values.iterator(); it.hasNext(); )
		{
			//Получить список еды для след животного
			List<String> cur_list = it.next();
			
			//Просмотр еды в поисках нужного существа
			for(Iterator<String> it_food = cur_list.iterator(); it_food.hasNext(); )
			{
				String cur_name = it_food.next();
				
				//Если найдено
				if(cur_name.equals(name))
				{
					//Вернуть что найдено
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	/****** Методы для работы с пультами	*****/
	

	//Нажатие кнопки на пульте
	@Override
	public void update_key(short build_number, byte device_number, EDeviceKey key)
	{
		//Если находимся не в игре - никакой реакции
		if(!game_state.equals(EFeedmeGameState.GAME))
			return;
		
		//Если нажата кнопка отличная от 1 или 2
		if(!key.equals(EDeviceKey.ONE_BUTTON) && !key.equals(EDeviceKey.TWO_BUTTON) && !key.equals(EDeviceKey.THREE_BUTTON))
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
			usb_service.send_display_string_command(build_number, device_number, "Кол-во очков: " + String.valueOf(player.scores));
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
			
			//Отправить информацию о текущем состоянии
			usb_service.send_display_string_command(build_number, device_number, player.player_state_string);
		}
	}


	//Получение считанной rfid метки
	@Override
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
		//Если находимся не в игре - никакой реакции
		if(!game_state.equals(EFeedmeGameState.GAME))
			return;
		
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
		
		//Получить существо, соответствующее rfid метке
		Creature creature = get_creature(rfid);
		if(creature == null)
		{
			DebugClass.message("Существа с такой меткой не существует");
			return;
		}
		
		//Если такого существа нет в цепочке - сказать об этом
		if(!is_in_food_chain(creature.name))
		{
			DebugClass.message("Такого существа нет в пищевой цепи");
			return;
		}
			
		//В зависимости от состояния игрока выбрать действие
		if(player.player_state.equals(Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT))
		{
			//Выбор того, кто будет есть
			
			//Проверить - мертв или нет
			if(creature.is_dead())
			{
				//Добавить ошибочное действие игроку
				player.add_action(new Player.PlayerAction(creature.name, "", Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_DEAD));
				player.player_state_string = "(Мертв) Кто?";
				
				//Отправить сигнал на мигание красного светодиода
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				//Отнять очки
				player.add_scores(-1);
				
				//Обновить таблицу
				update_table();
				
				return;
			}
			
			//Проверить - сыт или нет
			if(creature.food_cnt >= 3)
			{
				//Добавить ошибочное действие игроку
				player.add_action(new Player.PlayerAction(creature.name, "", Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_FULL));
				player.player_state_string = "(Сыт) Кто?";
				
				//Отправить сигнал на мигание красного светодиода
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				//Отнять очки
				player.add_scores(-1);
				
				//Обновить таблицу
				update_table();
				
				return;
			}
			
			//Запомнить и перейти на другое состояние
			player.who_eat = creature;
			player.player_state = Player.EPlayerActionState.READ_RFID_WHAT_SHOULD_BE_EAT;
			player.player_state_string = "Кого?";
			
			//Отправить сигнал на мигание зеленого светодиода
			usb_service.send_led_flash_command(build_number, device_number, 0);
		}
		else if(player.player_state.equals(Player.EPlayerActionState.READ_RFID_WHAT_SHOULD_BE_EAT))
		{
			//Выбор того, что будет съедено
			
			//Проверить - мертв или нет
			if(creature.is_dead())
			{
				//Добавить ошибочное действие игроку
				player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.WHAT_SHOULD_BE_EAT_DEAD));
				player.player_state_string = "(Мертв) Кто?";
				
				//Отправить сигнал на мигание красного светодиода
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
				
				//Отнять очки
				player.add_scores(-1);
				
				//Обновить таблицу
				update_table();
				
				return;
			}
			
			//Проверить - будет ли его есть или нет
			if(!is_may_eat(player.who_eat.name, creature.name))
			{
				//Добавить ошибочное действие игроку
				player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_NOT_EAT));
				player.player_state_string = "(Не ест) Кто?";
				
				//Отправить сигнал на мигание красного светодиода
				usb_service.send_led_flash_command(build_number, device_number, 1);
				
				player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
				
				//Отнять очки
				player.add_scores(-1);
				
				//Обновить таблицу
				update_table();
				
				return;
			}
			
			//Добавить новое действие игроку (успешное)
			player.add_action(new Player.PlayerAction(player.who_eat.name, creature.name, Player.PlayerAction.EPlayerActionInfo.SUCCESS));
			player.player_state_string = "(Успех) Кто?";
			
			//Вернуть изначальное состояние
			player.player_state = Player.EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
			
			//Добавить очки
			player.add_scores(3);
			
			//Убить существо которое было съедено
			creature.damage();
			
			//Повысить сытость существа, которое ело
			player.who_eat.food_cnt++;
			
			//Обнулить
			player.who_eat = null;
			
			//Отправить сигнал на мигание зеленого светодиода
			usb_service.send_led_flash_command(build_number, device_number, 0);
		}
		
		//Обновить таблицу
		update_table();
	}
	
	
	
	
	
	
	
	/****** Дополнительные структуры/классы/перечисления	*****/
	
	//Класс игрока
	private static class Player
	{
		//Состояние действий игрока
		public static enum EPlayerActionState
		{
			READ_RFID_WHO_SHOULD_EAT,		//Считывание метки того, кто должен есть
			READ_RFID_WHAT_SHOULD_BE_EAT	//Считывание метки того, кто должен быть съеден
		};
		
		//Действие игрока
		public static class PlayerAction
		{
			//Описание исхода действия
			public static enum EPlayerActionInfo
			{
				SUCCESS,					//Успешное кормление
				WHO_SHOULD_EAT_DEAD,		//Кого нужно кормить мертв
				WHO_SHOULD_EAT_FULL,		//Кого нужно кормить сыт
				WHO_SHOULD_EAT_NOT_EAT,		//Кого нужно кормить не будет это есть
				WHAT_SHOULD_BE_EAT_DEAD		//Чем нужно покормить уже съедено
			};
			
			//Информация о действии игрока
			public String who_should_eat;				//Кого нужно покормить
			public String what_should_be_eat;			//Чем нужно покормить
			public EPlayerActionInfo action_state;		//Исход действия
			
			//Конструктор
			PlayerAction(String who_should_eat, String what_should_be_eat, EPlayerActionInfo action_state)
			{
				this.who_should_eat = who_should_eat;
				this.what_should_be_eat = what_should_be_eat;
				this.action_state = action_state;
			}
		};
		
		//Свойства игрока
		public EPlayerActionState player_state;		//Состояние игрока
		public int scores;							//Кол-во очков игрока
		public List<PlayerAction> player_actions;	//Действия игрока
		
		public String player_state_string = "Кто?";	//Состояние игрока
		
		//Информация для передачи между состояниями
		public Creature who_eat;			//Существо которое будет есть
		
		//Информация об устройстве
		public int build_number;		//Номер комплекта пульта игрока
		public int device_number;		//Номер пульта в комплекте
		
		//Конструктор
		public Player(int build_number, int device_number)
		{
			//Инициализация действий игрока
			player_actions = new ArrayList<PlayerAction>();
			
			//Инициализация очков
			scores = 0;
			
			//Состояние игрока по умолчанию
			player_state = EPlayerActionState.READ_RFID_WHO_SHOULD_EAT;
			
			//Информация об устройстве
			this.build_number = build_number;
			this.device_number = device_number;
		}
		
		//Добавить действие
		public void add_action(String who_should_eat, String what_should_be_eat, PlayerAction.EPlayerActionInfo action_state)
		{
			player_actions.add(new PlayerAction(who_should_eat, what_should_be_eat, action_state));
		}
		public void add_action(PlayerAction player_action)
		{
			player_actions.add(player_action);
		}
		
		//Добавить очков
		public void add_scores(int val)
		{
			scores += val;
		}
		
		//Получить список успешных кормежек в виде строки
		public String get_all_successful_fed()
		{
			String ret = "";
			
			//Проход по всем действиям игрока
			for(Iterator<PlayerAction> it = player_actions.iterator(); it.hasNext(); )
			{
				//Получить текущее действие
				PlayerAction cur_player_action = it.next();
				
				//Получить состояние действия
				PlayerAction.EPlayerActionInfo action_info = cur_player_action.action_state;
				
				//Если состояние успешное
				if(action_info.equals(PlayerAction.EPlayerActionInfo.SUCCESS))
				{
					//Если не первое успешное состояние
					if(!ret.equals(""))
					{
						//Добавить разделитель
						ret += '\n';
					}
					
					//Добавить информацию о действии
					ret += cur_player_action.who_should_eat + "->" + cur_player_action.what_should_be_eat;
				}
			}
			
			return ret;
		}
		
		//Получить список всех не успешных кормежек в виде строки
		public String get_all_not_successful_fed()
		{
			String ret = "";
			
			//Проход по всем действиям игрока
			for(Iterator<PlayerAction> it = player_actions.iterator(); it.hasNext(); )
			{
				//Получить текущее действие
				PlayerAction cur_player_action = it.next();
				
				//Получить состояние действия
				PlayerAction.EPlayerActionInfo action_info = cur_player_action.action_state;
				
				//Если состояние не успешное
				if(!action_info.equals(PlayerAction.EPlayerActionInfo.SUCCESS))
				{
					//Если не первое не успешное состояние
					if(!ret.equals(""))
					{
						//Добавить разделитель
						ret += '\n';
					}
					
					//В зависимости от типа неуспешности - выбрать действие
					if(action_info.equals(PlayerAction.EPlayerActionInfo.WHAT_SHOULD_BE_EAT_DEAD))
					{
						//То, что должно быть съедено умерло
						
						ret += cur_player_action.who_should_eat + "->" + cur_player_action.what_should_be_eat + "(Мертв)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_DEAD))
					{
						//Тот, кто должен есть умер
						
						ret += cur_player_action.who_should_eat + "(Мертв)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_FULL))
					{
						//Тот, кто должен есть сыт
						
						ret += cur_player_action.who_should_eat + "(Сыт)";
					}
					else if(action_info.equals(PlayerAction.EPlayerActionInfo.WHO_SHOULD_EAT_NOT_EAT))
					{
						//Тот, кто должен есть не будет это есть
						
						ret += cur_player_action.who_should_eat + "(Отказался)->" + cur_player_action.what_should_be_eat;
					}
				}
			}
			
			return ret;
		}
	};
	
	
	//Описание существа или растения
	private class Creature
	{
		//Свойства существа
		List<String> rfid;				//Метки существа
		String name;					//Наименование существа
		private boolean is_dead;		//Признак смерти
		private int lives;				//Жизни
		int food_cnt;					//Кол-во съеденной еды
		List<String> food;				//Список наименований еды
		
		//Конструктор
		Creature(String name, List<String> rfid, List<String> food, int lives)
		{
			//По умолчанию ничего не съел
			food_cnt = 0;
			
			//По умолчанию жив
			is_dead = false;
			
			//Остальные параметры
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
	
	//Состояние игры
	private static enum EFeedmeGameState
	{
		PREGAME,	//Игра еще не началась
		GAME,		//Игра идет
		POSTGAME	//Игра закончилась
	};
}
