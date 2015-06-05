package vplay_engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;




//VPlay адаптер для списков и таблиц
public class VPlayAdapter extends BaseAdapter
{
	/***** Свойства адаптера	*****/
	
	//Необходимые данные для работы
	private Context context;					//Контекст для таблицы
	private int item_layout;					//ID layout строки таблицы в ресурсах
	private LayoutInflater layout_inflater;		//...
	private View header_view;					//Главный view элемент таблицы
	
	//Информация о столбцах
	private List<ColumnInfo> column_info;						//Информация о колонке таблицы (тип и ид view элемента)
	private List<AdditionalColumnInfo> additional_column_info;	//Дополнительная информация о колонке таблицы
	
	//Элементы таблицы
	private List<List<CellInfo>> objects;
	
	//Функции для реагирования на нажатия чекбоксов
	List<OnCheckedChangeListener> checkbox_listeners;
	
	/***** Описание обязательных методов	*****/
	
	//Конструктор
	public VPlayAdapter(Context context, int item_layout_id, List<ColumnInfo> column_info, List<AdditionalColumnInfo> additional_column_info, View header_view, List<OnCheckedChangeListener> checkbox_listeners)
	{
		//Инициализация адаптера
		this.header_view = header_view;
		this.context = context;
		this.item_layout = item_layout_id;
		this.column_info = column_info;
		this.additional_column_info = additional_column_info;
		this.checkbox_listeners = checkbox_listeners;
		layout_inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		objects = new ArrayList<List<CellInfo>>();
	}
	
	//Кол-во элементов
	@Override
	public int getCount()
	{
		return objects.size();
	}
	
	//Элемент по позиции
	@Override
	public Object getItem(int position)
	{
		return objects.get(position);
	}
	
	//id позиции
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	//Пункт списка
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		//Получить view-объект одной строки таблицы
		View view = convertView;
		if(view == null)
			view = layout_inflater.inflate(item_layout, parent, false);
		
		//Получить информацию об объекте, который будет расположен на этой строке
		List<CellInfo> current_item = get_item_params_list(position);
		
		//Итераторы для получения данных
		int column_number = 0;
		Iterator<ColumnInfo> ci = column_info.iterator();
		Iterator<AdditionalColumnInfo> cw = additional_column_info.iterator();
		Iterator<CellInfo> it = current_item.iterator();
		Iterator<OnCheckedChangeListener> listeners_it = checkbox_listeners.iterator();
		
		//Заполнить view данными
		while(it.hasNext())
		{
			//Получить данные с итераторов
			ColumnInfo cur_column_info = ci.next();
			AdditionalColumnInfo cur_add_column_info = cw.next();
			CellInfo cur_cell_info = it.next();
		
			//Получить ширину header элемента
			int cur_column_width = cur_add_column_info.width;
			
			//По типу столбца определить дальнейшие действия
			if(cur_column_info.col_type.equals(EColumnType.TEXT_COLUMN))	//Если текст
			{
				//Получить текстовый view элемент
				TextView tv = (TextView) view.findViewById(cur_column_info.view_id);
				
				//Установить параметры view элемента
				tv.setText(cur_cell_info.text_info);
				
				//Установить параметры внешнего вида (размер и прочее)
				LayoutParams lp = tv.getLayoutParams();
				lp.width = cur_column_width;
				tv.setLayoutParams(lp);
			}
			else if(cur_column_info.col_type.equals(EColumnType.IMAGE_COLUMN))	//Если изображение
			{
				//Получить view элемент изображения
				ImageView iv = (ImageView) view.findViewById(cur_column_info.view_id);
				
				//Установить параметры view элемента
				iv.setImageResource(cur_cell_info.image_info);
				
				//Установить параметры внешнего вида (размер и прочее)
				LayoutParams lp = iv.getLayoutParams();
				lp.width = cur_column_width;
				iv.setLayoutParams(lp);
			}
			else if(cur_column_info.col_type.equals(EColumnType.CHECK_COLUMN))	//Если чекбокс
			{
				//Получить элемент
				CheckBox check_box = (CheckBox) view.findViewById(cur_column_info.view_id);
				
				//Присвоить обработчик
				check_box.setOnCheckedChangeListener(listeners_it.next());
				
				//Вписать данные о позиции
				List<Integer> check_box_tag = new ArrayList<Integer>();
				check_box_tag.add(position);
				check_box_tag.add(column_number);
				check_box.setTag(check_box_tag);
				
				//Установить параметры view элемента
				check_box.setChecked(cur_cell_info.check_info);
				
				//Установить параметры внешнего вида (размер и прочее)
				LayoutParams lp = check_box.getLayoutParams();
				lp.width = cur_column_width;
				check_box.setLayoutParams(lp);
			}
			
			//Увеличить счетчик столбцов
			column_number++;
		}
		
		return view;
	}
	
	
	
	/***** Обработчики	*****/
	
	//Обработчик чекбокосов
	OnCheckedChangeListener check_state_change = new OnCheckedChangeListener()
	{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			//Получить TAG информацию
			List<Integer> check_box_tag = (List<Integer>) buttonView.getTag();
			int row = (Integer) check_box_tag.get(0);
			int col = (Integer) check_box_tag.get(1);
			
			//Поменять данные
			List<CellInfo> object_info = get_item_params_list(row);
			object_info.set(col, new CellInfo(isChecked));
			set_item_params_list(row, object_info);
		}
	};
	
	
	
	
	/***** Дополнительные/вспомогательные методы	*****/
	
	//Получить элемент таблицы как список параметров объекта
	public List<CellInfo> get_item_params_list(int position)
	{
		return (List<CellInfo>) getItem(position);
	}
	
	//Установить новый список параметров для объекта в заданной позиции
	public void set_item_params_list(int position, List<CellInfo> object_info)
	{
		objects.set(position, object_info);
		
		//Обновить данные
		notifyDataSetChanged();
	}
	
	//Добавить запись
	public void add_object(List<CellInfo> object_info)
	{
		//Если кол-во параметров не совпадает с нужным - ничего не делать
		if(object_info.size() != column_info.size())
			return;
		
		//Добавить объект
		objects.add(object_info);
		
		//Обновить данные
		notifyDataSetChanged();
	}
	
	//Удалить запись
	public void remove_object(int position)
	{
		//Отсечение неверных позиций
		if(position < 0 || position >= objects.size())
			return;
		
		//Удалить объект
		objects.remove(position);
		
		//Обновить данные
		notifyDataSetChanged();
	}
	
	
	
	/***** Описание дополнительных структур и перечислений	*****/
	
	//Типы столбцов
	public static enum EColumnType
	{
		TEXT_COLUMN,		//Столбец с текстом
		IMAGE_COLUMN,		//Столбец с изображением
		CHECK_COLUMN		//Столбец с чекбоксом
	};
	
	//Дополнительная информация о столбце
	public static class AdditionalColumnInfo
	{
		//Свойства
		public int header_id_for_column;
		public int width;
		
		//Конструктор
		public AdditionalColumnInfo(int header_id_for_column, int width)
		{
			this.header_id_for_column = header_id_for_column;
			this.width = width;
		}
	};
	
	//Информация о столбце
	public static class ColumnInfo
	{
		//Свойства
		public EColumnType col_type;		//Тип столбца
		public int view_id;					//ИД view элемента, отвечающего за столбец
		
		//Конструктор
		public ColumnInfo(EColumnType col_type, int view_id)
		{
			this.col_type = col_type;
			this.view_id = view_id;
		}
	};
	
	//Информация о ячейке (описание параметра элемента таблицы)
	public static class CellInfo
	{
		//Свойства
		public EColumnType col_type;		//Тип столбца
		public String text_info;			//Текстовая информация
		public int image_info;				//Изображение
		public boolean check_info;			//Информация чекбокса
		
		//Конструктор для текстовой информации
		public CellInfo(String text_info)
		{
			this.col_type = EColumnType.TEXT_COLUMN;
			this.text_info = text_info;
		}
		
		//Конструктор для изображения
		public CellInfo(int image_info)
		{
			this.col_type = EColumnType.IMAGE_COLUMN;
			this.image_info = image_info;
		}
		
		//Конструктор для чекбокса
		public CellInfo(boolean check_info)
		{
			this.col_type = EColumnType.CHECK_COLUMN;
			this.check_info = check_info;
		}
	}
}
