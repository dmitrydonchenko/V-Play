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

//Класс для работы с базой данных
public class VPlayDatabase
{
	
	/***** Информация о БД	*****/
	
	//Основная информация
	private final static String DATABASE_NAME = "vplay_rfid_database";		//Наименование БД
	private final static int DATABASE_VERSION = 1;							//Версия БД
	
	//Наименования полей таблицы DEVICE_IMAGE_TABLE
	private final static String IMAGE_ID = "id";			//ИД изображения (сопоставляется с идентификатором пульта в комплекте)
	private final static String IMAGE_NAME = "name";		//Наименование файла с изображением для пульта
	
	//Наименования полей таблицы DEVICE_TABLE
	private final static String DEVICE_ID = "id";				//ИД устройства в таблице
	private final static String DEVICE_SET = "suit";			//Комплект пульта
	private final static String DEVICE_NUMBER = "number";		//Номер пульта в комплекте
	private final static String DEVICE_ACTIVITY = "activity";	//Активность пульта
		
	//Наименования таблиц в БД
	private final static String DEVICE_IMAGE_TABLE = "device_image_table";				//Таблица имен изображений, которые должны сопоставляться с пультами (если номер пульта в комплекте = 2, то ему будет сопоставлено изображение из этой таблицы с ИД = 2)
	private final static String DEVICE_TABLE = "device_table";							//Таблица зарегестрированных устройств
	private final static String CARD_GROUP_TABLE = "card_group_table";					//Таблица групп карточек
	private final static String CARD_CONTENT_TABLE = "card_content_table";				//Таблица содержимого групп карточек
	private final static String CARD_RFID_TABLE = "card_rfid_table";					//Таблица со значениями rfid и что они обозначают

	//Наименование полей таблицы CARD_GROUP_TABLE
	private final static String GROUP_ID = "id";		//ИД группы карточек в таблице
	private final static String GROUP_NAME = "name";	//Наименование группы
	
	//Наименование полей таблицы CARD_CONTENT_TABLE
	private final static String CONTENT_ID = "id";					//ИД контента
	private final static String CONTENT_NAME = "name";				//Наименование контента
	private final static String CONTENT_GROUP_ID = "group_id";		//Идентификатор категории к которой принадлежит контент
	
	//Наименование полей таблицы CARD_RFID_TABLE
	private final static String RFID_ID = "id";						//ИД метки
	private final static String RFID_RFID = "rfid";					//Метка RFID
	private final static String RFID_CONTENT_ID = "content_id";		//Идентификатор контента к которому метка принадлежит
	
	
	
	
	/*****	Свойства базы данных	*****/
	
	//Модель базы данных
	private SQLiteDatabase database;			//База данных SQL
	private database_open_helper dbhelper;		//Объект для создания и управления БД
	
	
	
	
	
	
	/***** Методы для работы с БД	*****/
	
	//Инициализация БД
	public void init()
	{
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить контекст
		Context context = app.getApplicationContext();
		
		//Открытие БД (и создание если требуется)
		dbhelper = new database_open_helper(context);
		database = dbhelper.getWritableDatabase();
	}
	
	
	//Деинициализация БД
	public void deinit()
	{
		database.close();
		dbhelper.close();
	}
	
	
	
	
	//Получить ИД устройства
	public int get_device_id(int build_number, int device_number)
	{
		//Идентификатор
		int ret = -1;
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int id_idx = c.getColumnIndex(DEVICE_ID);
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			
			do
			{
				//Получение значений
				int id = c.getInt(id_idx);
				int set_value = c.getInt(set_idx);
				int number_value = c.getInt(number_idx);
				
				//Если найдено - запомнить и завершить поиск
				if(set_value == build_number && device_number == number_value)
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить идентификатор устройства по позиции
	private int get_device_id(int position)
	{
		//Сделать запрос данных таблицы с устройствами
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//Поиск идентификатора
		int id = -1;
		
		//Если в таблице есть записи
		int cnt = 0;			//Счетчик записей (позиций)
		if(c.moveToFirst())
		{
			//Определить номера нужных столбцов
			int id_idx = c.getColumnIndex(DEVICE_ID);
			
			//Найти нужный идентфиикатор
			do
			{
				//Если дошло до нужной записи
				if(cnt == position)
				{
					//Получить значение
					id = c.getInt(id_idx);;
				}
				
				cnt++;
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return id;
	}
	
	//Получить активность устройства
	public boolean get_device_activity(int build_number, int device_number)
	{
		//Результат
		boolean ret = false;
		
		//Сделать запрос данных таблицы с устройствами
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//Если в таблице есть записи
		if(c.moveToFirst())
		{			
			//Определить номера нужных столбцов
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			int activity_idx = c.getColumnIndex(DEVICE_ACTIVITY);
		
			do
			{
				//Получить значения
				int set = c.getInt(set_idx);
				int number = c.getInt(number_idx);
				boolean activity = c.getInt(activity_idx)>0;
				
				//Если устройство найдено - запомнить данные
				if(set == build_number && number == device_number)
				{
					ret = activity;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}

	
	//Получить полную информацию о всех устройствах
	public List<Map<String, Object>> get_all_device_info()
	{
		//Создать контейнер для хранения таблицы
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		
		//Сделать запрос данных таблицы с устройствами
		Cursor c = database.query(DEVICE_TABLE, null, null, null, null, null, null);
		
		//Если в таблице есть записи
		if(c.moveToFirst())
		{			
			//Определить номера нужных столбцов
			int id_idx = c.getColumnIndex(DEVICE_ID);
			int set_idx = c.getColumnIndex(DEVICE_SET);
			int number_idx = c.getColumnIndex(DEVICE_NUMBER);
			int activity_idx = c.getColumnIndex(DEVICE_ACTIVITY);
		
			do
			{
				//Получить значения
				int id = c.getInt(id_idx);
				int set = c.getInt(set_idx);
				int number = c.getInt(number_idx);
				boolean activity = c.getInt(activity_idx)>0;
				
				//Создать контейнер для хранения данных
				Map<String, Object> cur_map = new HashMap<String, Object>();
				cur_map.put("id", id);
				cur_map.put("set", set);
				cur_map.put("number", number);
				cur_map.put("activity", activity);
				
				//Добавить к общему контейнеру
				ret.add(cur_map);
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить наименование файла с картинкой для пульта
	public String get_image_filename_for_device(int device_number)
	{
		//Найденное наименование
		String ret = "";
		
		//Сделать запрос данных таблицы с устройствами
		Cursor c = database.query(DEVICE_IMAGE_TABLE, null, null, null, null, null, null);
		
		//Если в таблице есть записи
		if(c.moveToFirst())
		{
			//Определить номера нужных столбцов
			int id_idx = c.getColumnIndex(IMAGE_ID);
			int name_idx = c.getColumnIndex(IMAGE_NAME);
			
			do
			{
				//Получить значения
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
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	
	//Получить список имен категорий
	public List<String> get_category_names()
	{
		//Создать контейнер
		List<String> ret = new ArrayList<String>();
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_GROUP_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int name_idx = c.getColumnIndex(GROUP_NAME);
			
			do
			{
				//Получение значений
				String name = c.getString(name_idx);
				
				//Добавление полученных данных в контейнер
				ret.add(name);
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить ИД категории по имени
	public int get_category_id(String category_name)
	{
		//Идентификатор
		int ret = -1;
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_GROUP_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int id_idx = c.getColumnIndex(GROUP_ID);
			int name_idx = c.getColumnIndex(GROUP_NAME);
			
			do
			{
				//Получение значений
				int id = c.getInt(id_idx);
				String name = c.getString(name_idx);
				
				
				
				//Если имя найдено - запомнить идентификатор и завершить поиск
				if(name.equals(category_name))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить список имен контента для определенной категории
	public List<String> get_content_names(int category_id)
	{
		//Создать контейнер
		List<String> ret = new ArrayList<String>();
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_CONTENT_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int name_idx = c.getColumnIndex(CONTENT_NAME);
			int group_id_idx = c.getColumnIndex(CONTENT_GROUP_ID); 
			
			do
			{
				//Получение значений
				String name = c.getString(name_idx);
				int group_id = c.getInt(group_id_idx);
				
				//Если контент принадлежит заданной группе - добавление в контейнер
				if(group_id == category_id)
					ret.add(name);
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить ИД контента
	public int get_content_id(String content_name)
	{
		//Идентификатор
		int ret = -1;
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_CONTENT_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int id_idx = c.getColumnIndex(CONTENT_ID);
			int name_idx = c.getColumnIndex(CONTENT_NAME);
			
			do
			{
				//Получение значений
				int id = c.getInt(id_idx);
				String name = c.getString(name_idx);
				
				//Если имя найдено - запомнить идентификатор и завершить поиск
				if(name.equals(content_name))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить список RFID заданного контента
	public List<String> get_rfid_list(int rfid_content_id)
	{
		//Создать контейнер
		List<String> ret = new ArrayList<String>();
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_RFID_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int rfid_idx = c.getColumnIndex(RFID_RFID);
			int content_id_idx = c.getColumnIndex(RFID_CONTENT_ID); 
			
			do
			{
				//Получение значений
				String rfid = c.getString(rfid_idx);
				int content_id = c.getInt(content_id_idx);
				
				//Если rfid принадлежит контенту - добавить
				if(content_id == rfid_content_id)
					ret.add(rfid);
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	//Получить ИД rfid кода
	public int get_rfid_id(String rfid_rfid)
	{
		//Идентификатор
		int ret = -1;
		
		//Создать запрос данных с таблицы
		Cursor c = database.query(CARD_RFID_TABLE, null, null, null, null, null, null);
		
		//Проход по записям
		if(c.moveToFirst())
		{
			//Определить индексы нужных значений
			int id_idx = c.getColumnIndex(RFID_ID);
			int rfid_idx = c.getColumnIndex(RFID_RFID);
			
			do
			{
				//Получение значений
				int id = c.getInt(id_idx);
				String rfid = c.getString(rfid_idx);
				
				//Если имя найдено - запомнить идентификатор и завершить поиск
				if(rfid.equals(rfid_rfid))
				{
					ret = id;
					break;
				}
			}
			while(c.moveToNext());
		}
		
		//Закрыть курсор
		c.close();
		
		return ret;
	}
	
	
	
	//Добавить пульт в БД
	public boolean add_device(int build_number, int device_number, boolean activity)
	{
		//Проверка на существование такого устройства
		if(get_device_id(build_number, device_number) != -1)
			return false;
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(DEVICE_SET, build_number);
		cv.put(DEVICE_NUMBER, device_number);
		cv.put(DEVICE_ACTIVITY, activity);
		
		//Добавить новую запись в бд
		database.insert(DEVICE_TABLE, null, cv);
		
		return true;
	}
	
	
	
	//Добавить новую категорию
	public boolean add_new_category(String category_name)
	{
		//Проверка на существование такой категории
		if(get_category_id(category_name) != -1)
			return false;
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(GROUP_NAME, category_name);
		
		//Добавить запись
		database.insert(CARD_GROUP_TABLE, null, cv);
		
		return true;
	}
	
	//Добавить новую категорию
	private void add_new_category(int category_id, String category_name)
	{
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(GROUP_ID, category_id);
		cv.put(GROUP_NAME, category_name);
		
		//Добавить запись
		database.insert(CARD_GROUP_TABLE, null, cv);
	}

	//Добавить новый контент
	public boolean add_new_content(int category_id, String content_name)
	{
		//Проверка на существование такого контента
		if(get_content_id(content_name) != -1)
			return false;
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(CONTENT_NAME, content_name);
		cv.put(CONTENT_GROUP_ID, category_id);
		
		//Добавить запись
		database.insert(CARD_CONTENT_TABLE, null, cv);
		
		return true;
	}
	
	//Добавить новый контент
	private void add_new_content(int content_id, int category_id, String content_name)
	{
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(CONTENT_ID, content_id);
		cv.put(CONTENT_NAME, content_name);
		cv.put(CONTENT_GROUP_ID, category_id);
		
		//Добавить запись
		database.insert(CARD_CONTENT_TABLE, null, cv);
	}

	//Добавить новый RFID
	public boolean add_new_rfid(int content_id, String rfid)
	{
		//Проверка на существование такого rfid
		if(get_rfid_id(rfid) != -1)
			return false;
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(RFID_RFID, rfid);
		cv.put(RFID_CONTENT_ID, content_id);
		
		//Добавить запись
		database.insert(CARD_RFID_TABLE, null, cv);
		
		return true;
	}
	
	//Добавить новый RFID
	private void add_new_rfid(int rfid_id, int content_id, String rfid)
	{
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(RFID_ID, rfid_id);
		cv.put(RFID_RFID, rfid);
		cv.put(RFID_CONTENT_ID, content_id);
		
		//Добавить запись
		database.insert(CARD_RFID_TABLE, null, cv);
	}
	
	
	//Удалить RFID
	public void remove_rfid(String rfid_rfid)
	{
		//Удалить запись
		database.delete(CARD_RFID_TABLE, RFID_RFID + " = \"" + rfid_rfid + "\"", null);
	}
	
	//Удалить RFID
	public void remove_rfid(int rfid_content_id, int pos)
	{
		//Контейнер
		List<String> ret = get_rfid_list(rfid_content_id);
		
		//Проверка на выход за границы
		if(pos >= ret.size() || pos < 0)
			return;
		
		//Получить нужный rfid
		String cur_rfid = ret.get(pos);
		
		//Удаление
		remove_rfid(cur_rfid);
	}
	
	//Удалить все rfid принадлежащие контенту
	public void remove_all_rfid_from_content(int rfid_content_id)
	{
		//Контейнер
		List<String> ret = get_rfid_list(rfid_content_id);
		
		//Удалить все rfid
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//Получить текущий rfid
			String cur_rfid = it.next();
			
			//Удалить
			remove_rfid(cur_rfid);
		}
	}
	
	//Удалить контент
	public void remove_content(String name)
	{
		//Получить ИД контента
		int content_id = get_content_id(name);
		
		//Удалить все rfid для контента
		remove_all_rfid_from_content(content_id);
		
		//Удалить запись
		database.delete(CARD_CONTENT_TABLE, CONTENT_ID + " = \"" + content_id + "\"", null);
	}
	
	//Удалить контент
	public void remove_content(int content_category_id, int pos)
	{
		//Контейнер
		List<String> ret = get_content_names(content_category_id);
		
		//Проверка на выход за границы
		if(pos >= ret.size() || pos < 0)
			return;
		
		//Получить нужный контент
		String content_name = ret.get(pos);
		
		//Удаление
		remove_content(content_name);
	}
	
	//Удалить весь контент принадлежащий категории
	public void remove_all_content_from_category(int content_category_id)
	{
		//Контейнер
		List<String> ret = get_content_names(content_category_id);
		
		//Удалить весь контент
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//Получить текущий контент
			String cur_content = it.next();
			
			//Удалить
			remove_content(cur_content);
		}
	}
	
	//Удалить категорию
	public void remove_category(String name)
	{
		//Получить ИД категории
		int category_id = get_category_id(name);
		
		//Удалить весь контент для категории
		remove_all_content_from_category(category_id);
		
		//Удалить запись
		database.delete(CARD_GROUP_TABLE, GROUP_ID + " = \"" + category_id + "\"", null);
	}
	
	//Удалить категорию
	public void remove_category(int pos)
	{
		//Контейнер
		List<String> ret = get_category_names();
		
		//Проверка на выход за границы
		if(pos >= ret.size() || pos < 0)
			return;
		
		//Получить нужную категорию
		String category_name = ret.get(pos);
		
		//Удаление
		remove_category(category_name);
	}
	
	
	//Удалить устройство из БД
	public void delete_device(int position)
	{
		//Поиск идентификатора
		int id = get_device_id(position);
		
		//Если ничего не найдено - ничего не делать
		if(id == -1)
			return;
		
		//Удалить запись
		database.delete(DEVICE_TABLE, "id = " + id, null);
	}
	
	
	//Очистка базы данных
	public void clear_card_database()
	{
		//Контейнер
		List<String> ret = get_category_names();
		
		//Удалить все категории
		for(Iterator<String> it = ret.iterator(); it.hasNext(); )
		{
			//Текущая категория
			String cur_category = it.next();
			
			//Удалить
			remove_category(cur_category);
		}
	}
	
	
	
	//Обновить активность устройства
	public void update_device_activity(int position, boolean activity)
	{
		//Поиск идентификатора
		int id = get_device_id(position);
		
		//Если ничего не найдено - ничего не делать
		if(id == -1)
			return;
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(DEVICE_ACTIVITY, activity);
		
		//Обновить по ИД
		database.update(DEVICE_TABLE, cv, "id = ?", new String[] { "" + id });
	}
	
	
	//Изменить имя категории
	public boolean update_category_name(String old_name, String new_name)
	{
		//Если имя не изменяется - ничего не делать
		if(old_name.equals(new_name))
			return true;
		
		//Если новое имя уже используется - ничего не делать и вернуть признак невыполненного обновления
		if(get_category_id(new_name) != -1)
			return false;
		
		//Получить идентификатор категории
		int id = get_category_id(old_name);
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(GROUP_NAME, new_name);
		
		//Обновить по ИД
		database.update(CARD_GROUP_TABLE, cv, GROUP_ID + " = ?", new String[] { "" + id});
		
		return true;
	}
	
	//Изменить имя категории
	public boolean update_category_name(int pos, String new_name)
	{
		//Контейнер
		List<String> ret = get_category_names();
		
		//Проверка на выход за границы
		if(pos >= ret.size() || pos < 0)
			return false;
		
		//Получить старое имя
		String old_name = ret.get(pos);
		
		//Обновить
		return update_category_name(old_name, new_name);
	}
	
	//Изменить имя контента
	public boolean update_content_name(String old_name, String new_name)
	{
		//Если имя не изменяется - ничего не делать
		if(old_name.equals(new_name))
			return true;
		
		//Если новое имя уже используется - ничего не делать и вернуть признак невыполненного обновления
		if(get_content_id(new_name) != -1)
			return false;
		
		//Получить идентификатор категории
		int id = get_content_id(old_name);
		
		//Объект для данных
		ContentValues cv = new ContentValues();
		
		//Заполнить данными
		cv.put(CONTENT_NAME, new_name);
		
		//Обновить по ИД
		database.update(CARD_CONTENT_TABLE, cv, CONTENT_ID + " = ?", new String[] { "" + id});
		
		return true;
	}
	
	//Изменить имя контента
	public boolean update_content_name(int category_id, int pos, String new_name)
	{
		//Контейнер
		List<String> ret = get_content_names(category_id);
		
		//Проверка на выход за границы
		if(pos >= ret.size() || pos < 0)
			return false;
		
		//Получить старое имя
		String old_name = ret.get(pos);
		
		//Обновить
		return update_content_name(old_name, new_name);
	}
	
	
	//Получить строку для базы данных
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
	
	//Получить нормальную строку из строки для базы данных
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

	
	//Перевод таблиц БД содержащих информацию о карточках в строку
	public String get_card_database_in_string()
	{
		//Создать строку
		String ret = "";
		
		//Получить список категорий
		List<String> category_list = get_category_names();
		
		//Запись кол-ва категорий
		ret += category_list.size();
		
		//Запись информации о категориях (ид, наименование)
		for(Iterator<String> category_it = category_list.iterator(); category_it.hasNext(); )
		{
			//Получить текущую категорию
			String cur_category = category_it.next();
			
			//Получить идентификатор
			int cur_category_id = get_category_id(cur_category);
			
			//Запись
			ret += "\t" + cur_category_id + "\t" + convert_from_normal_to_database(cur_category);
			
			//Получить список контента для категории
			List<String> content_list = get_content_names(cur_category_id);
			
			//Запись кол-ва контента для категории
			ret += "\t" + content_list.size();
			
			//Запись информации о контенте (ид, наименование, ид категории)
			for(Iterator<String> content_it = content_list.iterator(); content_it.hasNext(); )
			{
				//Получить текущий контент
				String cur_content = content_it.next();
				
				//Получить идентификатор
				int cur_content_id = get_content_id(cur_content);
				
				//Запись
				ret += "\t" + cur_content_id + "\t" + convert_from_normal_to_database(cur_content) + "\t" + cur_category_id;
				
				//Получить список rfid для контента
				List<String> rfid_list = get_rfid_list(cur_content_id);
				
				//Запись кол-ва rfid
				ret += "\t" + rfid_list.size();
				
				//Запись информации о rfid (ид, rfid, ид контента)
				for(Iterator<String> rfid_it = rfid_list.iterator(); rfid_it.hasNext(); )
				{
					//Получить текущий rfid
					String cur_rfid = rfid_it.next();
					
					//Получить идентификатор
					int cur_rfid_id = get_rfid_id(cur_rfid);
					
					//Запись
					ret += "\t" + cur_rfid_id + "\t" + convert_from_normal_to_database(cur_rfid) + "\t" + cur_content_id;
				}
			}
		}
		
		return ret;
	}
	
	
	//Получить строку содержащую информацию о таблицах БД содержащих информацию о карточках
	public void set_card_database_by_string(String database_string)
	{
		//Очистить БД
		clear_card_database();
		
		//Разбить строку на токены
		String [] tokens = database_string.split("\t");
		
		//Итератор по токенам
		int it = 0;
		
		//Считывание кол-ва категорий
		int category_count = Integer.parseInt(tokens[it++]);
			
		//Проход по кол-ву категорий
		for(int category_it = 0; category_it < category_count; category_it++)
		{
			//Получить ИД категории
			int cur_category_id = Integer.parseInt(tokens[it++]);
			
			//Получить имя категории
			String cur_category = tokens[it++];
			
			//Добавить категорию в бд
			add_new_category(cur_category_id, convert_from_database_to_normal(cur_category));
			
			//Считывание кол-ва контента в категории
			int content_count = Integer.parseInt(tokens[it++]);
			
			//Проход по кол-ву контейнта в категории
			for(int content_it = 0; content_it < content_count; content_it++)
			{
				//Получить ИД
				int cur_content_id = Integer.parseInt(tokens[it++]);
				
				//Получить имя контента
				String cur_content = tokens[it++];
				
				//Получить идентфикатор категории
				int cur_content_category_id = Integer.parseInt(tokens[it++]);
				
				//Добавить контент в бд
				add_new_content(cur_content_id, cur_content_category_id, convert_from_database_to_normal(cur_content));
				
				//Считывание кол-ва rfid в контенте
				int rfid_count = Integer.parseInt(tokens[it++]);
				
				//Проход по кол-ву rfid в контенте
				for(int rfid_it = 0; rfid_it < rfid_count; rfid_it++)
				{
					//Получить ИД
					int cur_rfid_id = Integer.parseInt(tokens[it++]);
					
					//Получить RFID
					String cur_rfid = tokens[it++];
					
					//Получить ИД контента
					int cur_rfid_content_id = Integer.parseInt(tokens[it++]);
					
					//Добавить rfid в БД
					add_new_rfid(cur_rfid_id, cur_rfid_content_id, convert_from_database_to_normal(cur_rfid));
				}
			}
		}
	}
	
	
	/***** Описание Database Helper	*****/
	
	class database_open_helper extends SQLiteOpenHelper
	{
		/***** Данные для создания БД	*****/
		
		//Запросы на создание таблиц
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
		
		/***** Основные методы для работы с database helper	*****/
		
		//Конструктор
		public database_open_helper(Context context)
		{
			//Конструктор суперкласса
			super(context, "vplay_database", null, 1);
		}
		
		//Если базы данных не существует - вызывается этот метод
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			//Создание таблиц
			db.execSQL(CREATE_DEVICE_IMAGE_TABLE);
			db.execSQL(CREATE_DEVICE_TABLE);
			db.execSQL(CREATE_CARD_GROUP_TABLE);
			db.execSQL(CREATE_CARD_CONTENT_TABLE);
			db.execSQL(CREATE_CARD_RFID_TABLE);
			
			//Получение указателя на приложение
			VPlayApplication app = VPlayApplication.get_instance();
			
			//Получение контекста
			Context context = app.getApplicationContext();
			
			//Получение ассета
			AssetManager asset_manager = context.getAssets();
			
			//Заполнение базы данных наименованиями изображений для пультов
			fill_device_image_table(db, asset_manager);
		}
		
		//Если база данных существует, но имеет более новую версию
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			//При обновлении БД пока ничего не делать
		}	
		
		
		/***** Вспомогательные методы	*****/
		
		//Заполнить таблицу DEVICE_IMAGE_TABLE
		private void fill_device_image_table(SQLiteDatabase db, AssetManager asset_manager)
		{
			//Поток для обработки файлов ассета
			InputStream is = null;
			
			//Объект чтения файла
			BufferedReader reader;
			
			//Строка для чтения файлов построчно
			String line;
			
			/***** Заполнение базы данных наименованиями изображений для пультов	*****/
			
			//Открыть файл с наименованиями изображений для пультов
			try
			{
				is = asset_manager.open("pult_images.txt");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			//Чтение записей из файла
			reader = new BufferedReader(new InputStreamReader(is));
			try
			{
				while((line = reader.readLine()) != null)
				{
					//Объект для данных
					ContentValues cv = new ContentValues();
					
					//Заполнить данными
					cv.put(IMAGE_NAME, line);
					
					//Добавить новую запись в бд
					db.insert(DEVICE_IMAGE_TABLE, null, cv);
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
	}
}
