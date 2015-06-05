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



//Окно регистрации пультов
public class DeviceRegistrationActivity extends VPlayActivity
{
	
	/*****	Свойства окна	*****/
	
	//Таблица
	VPlayTable table;
	
	
	
	
	/***** Стандартные методы	*****/
	
	//При создании окна
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Стандартная инициализация окна
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_registration);
		
		//Контекст
		Context context = this;
		
		//ИД xml файла внешнего вида строки таблицы
		int item_layout_id = R.layout.activity_device_registration_tableitem;
		
		//Информация о ячейках строки таблицы
		List<ColumnInfo> column_info = new ArrayList<ColumnInfo>();
		column_info.add(new ColumnInfo(EColumnType.CHECK_COLUMN, R.id.device_registration_device_activity));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.device_registration_device_name));
		column_info.add(new ColumnInfo(EColumnType.TEXT_COLUMN, R.id.device_registration_device_params));
		column_info.add(new ColumnInfo(EColumnType.IMAGE_COLUMN, R.id.device_registration_device_image));
		
		//ИД LinearLayout в который будет вставлена таблица
		LinearLayout main_linear_layout = (LinearLayout) findViewById(R.id.device_registration_main_layout);
		
		//Информация о заголовке таблицы
		List<HeaderCellInfo> header_info = new ArrayList<HeaderCellInfo>();
		header_info.add(new HeaderCellInfo("Активность", 1, 250));
		header_info.add(new HeaderCellInfo("Наименование пульта", 2, 500));
		header_info.add(new HeaderCellInfo("Изображение", 1, 300));
		
		//Функции для реагирования на изменение состояния чекбоксов
		List<OnCheckedChangeListener> listeners = new ArrayList<OnCheckedChangeListener>();
		listeners.add(check_state_change);
		
		//Создание таблицы
		table = new VPlayTable(context, main_linear_layout, item_layout_id, header_info, column_info, listeners);
		
		//Инициализация таблицы значениями из БД
		init_table();
		
		//Настроить контекстное меню для списка
		ListView lv = table.get_main_list();
		registerForContextMenu(lv);
	}
	
	
	
	
	/***** Методы для работы с устройством	*****/
	
	//Обновление при получении пакета данных при нажатии кнопки на устройстве
	public void update_key(short build_number, byte device_number, UsbService.EDeviceKey key)
	{
		add_device(build_number, device_number);
	}
	
	//Обновление при получении пакета данных при считывании RFID метки на устройстве
	public void update_rfid(short build_number, byte device_number, String rfid)
	{
	}
	
	
	
	
	/***** Дополнительные методы	*****/
	
	//Составить новый объект для таблицы
	private List<CellInfo> get_table_obj(boolean activity, int device_number, int build_number, int resource_id)
	{
		List<CellInfo> obj = new ArrayList<CellInfo>();
		obj.add(new CellInfo(activity));
		obj.add(new CellInfo("Пульт #" + device_number));
		obj.add(new CellInfo("Комплект: " + build_number));
		obj.add(new CellInfo(resource_id));
		
		return obj;
	}
	
	//Инициализация таблицы данными из БД
	private void init_table()
	{
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Получить таблицу
		List<Map<String, Object>> table_data = db.get_all_device_info();
		
		//Цикл заполнения таблицы
		for(Iterator<Map<String, Object>> it = table_data.iterator(); it.hasNext(); )
		{
			//Получить информацию о текущем объекте
			Map<String, Object> cur_map = it.next();
			
			//Получить данные
			boolean activity = (Boolean) cur_map.get("activity");
			int device_number = (Integer) cur_map.get("number");
			int build_number = (Integer) cur_map.get("set");
			
			//Получить наименование файла с изображением для пульта
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//Получить ИД ресурса с изображением
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//Создать запись для таблицы
			List<CellInfo> obj = get_table_obj(activity, device_number, build_number, resource_id);
			
			//Добавить запись
			table.add_object(obj);
		}
	}
	
	
	//Добавить пульт в базу данных
	private void add_device(short build_number, byte device_number)
	{
		//Получить указатель на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получить указатель на базу данных
		VPlayDatabase db = app.get_database();
		
		//Добавить устройство
		boolean activity = true;		//Активность устройства
		boolean ret = db.add_device(build_number, device_number, activity);
		
		//Если устройство успешно добавлено
		if(ret)
		{
			DebugClass.message("Устройство добавлено");
			
			//Получить наименование файла с изображением для пульта
			String image_filename = db.get_image_filename_for_device(device_number);
			
			//Получить ИД ресурса с изображением
			Resources resources = app.get_resources();
			Context context = app.getApplicationContext();
			int resource_id = resources.getIdentifier(image_filename, "drawable", context.getPackageName());
			
			//Создать запись для таблицы
			List<CellInfo> obj = get_table_obj(activity, device_number, build_number, resource_id);
			
			//Добавить в таблицу
			table.add_object(obj);
		}
		else	//Если такое устройство уже было в БД
		{
			DebugClass.message("Устройство уже есть в списке");
		}
	}
	
	
	
	
	/***** Обработчики чекбоксов таблицы	*****/
	
	//Обработчик чекбокосов
	OnCheckedChangeListener check_state_change = new OnCheckedChangeListener()
	{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			//Получить TAG информацию
			List<Integer> check_box_tag = (List<Integer>) buttonView.getTag();
			int row = (Integer) check_box_tag.get(0);
			int col = (Integer) check_box_tag.get(1);
			
			//Получить указатель на приложение
			VPlayApplication app = VPlayApplication.get_instance();
			
			//Получить указатель на базу данных
			VPlayDatabase db = app.get_database();
			
			//Поменять данные в БД
			db.update_device_activity(row, isChecked);
			
			//Поменять данные в таблице
			List<CellInfo> object_info = table.get_item_params_list(row);
			object_info.set(col, new CellInfo(isChecked));
			table.set_item_params_list(row, object_info);
		}
	};
	
	
	
	
	/*****	Данные для контекстного меню	*****/
	
	private static final int CM_DELETE_ID = 1;		//Идентификатор кнопки удаления в контекстном меню
	
	
	
	/*****	Методы работы с контекстным меню	*****/
	
	//При открытии контекстного меню
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CM_DELETE_ID, 0, "Удалить запись");
	}
	
	//При выборе пункта в контекстном меню
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		//Если выбрано удаление
		if(item.getItemId() == CM_DELETE_ID)
		{
			//Получить информацию о пункте списка
			AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
			
			//Получить позицию записи
			int position = acmi.position;
			
			//Получить указатель на приложение
			VPlayApplication app = VPlayApplication.get_instance();
			
			//Получить указатель на базу данных
			VPlayDatabase db = app.get_database();
			
			//Удалить запись из БД
			db.delete_device(position);
			
			//Удалить запись
			table.remove_object(position);
			
			return true;
		}
		return super.onContextItemSelected(item);
	}
}