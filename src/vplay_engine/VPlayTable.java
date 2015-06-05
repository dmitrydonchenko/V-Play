package vplay_engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vplay_engine.VPlayAdapter.AdditionalColumnInfo;
import vplay_engine.VPlayAdapter.CellInfo;
import vplay_engine.VPlayAdapter.ColumnInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.vplay.R;

public class VPlayTable
{	
	
	/***** Свойства таблицы	*****/
	
	//Необходимые данные
	private View view;					//Представление таблицы
	private VPlayAdapter adapter;		//Адаптер для таблицы
	private List<Integer> headers_id;	//Идентификаторы для заголовков (view id)
	
	
	
	
	/*****	Основные методы для работы	*****/
	
	//Конструктор
	public VPlayTable(Context context, LinearLayout main_linear_layout, int item_layout_id, List<HeaderCellInfo> header_info, List<ColumnInfo> column_info, List<OnCheckedChangeListener> checkbox_listeners)
	{
		//Получить таблицу
		LayoutInflater layout_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = layout_inflater.inflate(R.layout.vplay_table_view, main_linear_layout, false);
		
		//Создать массив для хранения ИД заголовков
		headers_id = new ArrayList<Integer>();
		
		//Получить LinearLayout для header
		LinearLayout header = (LinearLayout) view.findViewById(R.id.vplay_table_header);
		
		//Сформировать заголовок
		for(Iterator<HeaderCellInfo> it = header_info.iterator(); it.hasNext(); )
		{
			//Получить информацию о новой ячейке заголовка
			HeaderCellInfo cell_info = it.next();
			
			//Создать текстовый View
			TextView tv = new TextView(context);
			
			//Присвоить ему новый идентификатор и запомнить его
			int new_id = View.generateViewId();
			headers_id.add(new_id);
			tv.setId(new_id);
			
			//Установить параметры отображения
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.width = cell_info.width;
			lp.height = LayoutParams.WRAP_CONTENT;
			tv.setLayoutParams(lp);
			
			//Установить данные для текстового view
			tv.setText(cell_info.text);
			
			//Добавить текстовый View в заголовок
			header.addView(tv);
		}
		
		//Добавить таблицу на главный экран
		main_linear_layout.addView(view);
		
		//Создать массив для запоминания ширины
		List<AdditionalColumnInfo> additional_column_info = new ArrayList<AdditionalColumnInfo>();
		
		//Получить итераторы
		Iterator<HeaderCellInfo> it = header_info.iterator();
		Iterator<Integer> headers_id_it = headers_id.iterator();
		
		//Получить ширину каждого элемента с данными
		while(it.hasNext())
		{
			//Получить информацию о новой ячейке заголовка
			HeaderCellInfo cell_info = it.next();
			
			//Получить идентификатор
			int header_id = headers_id_it.next();
			
			//Проход по всем view элементам, которые должен охватить заголовок и запомнить для них ширину
			for(int i = 0; i < cell_info.view_cnt; i++)
				additional_column_info.add(new AdditionalColumnInfo(header_id, cell_info.width));
		}
		
		//Создание адаптера
		adapter = new VPlayAdapter(context, item_layout_id, column_info, additional_column_info, header, checkbox_listeners);
		
		//Настройка списка
		ListView lvMain = get_main_list();
		lvMain.setAdapter(adapter);
	}
	
	//Получить таблицу (без заголовка)
	public ListView get_main_list()
	{
		ListView lv = (ListView) view.findViewById(R.id.vplay_table_main_list);
		return lv;
	}
	
	//Получить элемент таблицы как список параметров объекта
	public List<CellInfo> get_item_params_list(int position)
	{
		return adapter.get_item_params_list(position);
	}
	
	//Установить новый список параметров для объекта в заданной позиции
	public void set_item_params_list(int position, List<CellInfo> object_info)
	{
		adapter.set_item_params_list(position, object_info);
	}
	
	//Добавить запись
	public void add_object(List<CellInfo> object_info)
	{
		adapter.add_object(object_info);
	}
	
	//Удалить запись
	public void remove_object(int position)
	{
		adapter.remove_object(position);
	}
	
	//Очистить таблицу
	public void clear_table()
	{
		while(adapter.getCount() > 0)
		{
			remove_object(0);
		}
	}
	
	//Кол-во элементов
	public int getCount()
	{
		return adapter.getCount();
	}
	
	
	
	
	/*****	Дополнительные классы/структуры/перечисления	*****/
	
	//Информация об одной ячейке заголовка
	public static class HeaderCellInfo
	{
		//Свойства
		public String text;			//Надпись
		public int view_cnt;		//Кол-во элементов с данными (подряд), которые охватывает заголовок
		public int width;
		
		//Конструктор
		public HeaderCellInfo(String text, int view_cnt, int width)
		{
			this.text = text;
			this.view_cnt = view_cnt;
			this.width = width;
		}
	};
}
