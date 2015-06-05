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




//VPlay ������� ��� ������� � ������
public class VPlayAdapter extends BaseAdapter
{
	/***** �������� ��������	*****/
	
	//����������� ������ ��� ������
	private Context context;					//�������� ��� �������
	private int item_layout;					//ID layout ������ ������� � ��������
	private LayoutInflater layout_inflater;		//...
	private View header_view;					//������� view ������� �������
	
	//���������� � ��������
	private List<ColumnInfo> column_info;						//���������� � ������� ������� (��� � �� view ��������)
	private List<AdditionalColumnInfo> additional_column_info;	//�������������� ���������� � ������� �������
	
	//�������� �������
	private List<List<CellInfo>> objects;
	
	//������� ��� ������������ �� ������� ���������
	List<OnCheckedChangeListener> checkbox_listeners;
	
	/***** �������� ������������ �������	*****/
	
	//�����������
	public VPlayAdapter(Context context, int item_layout_id, List<ColumnInfo> column_info, List<AdditionalColumnInfo> additional_column_info, View header_view, List<OnCheckedChangeListener> checkbox_listeners)
	{
		//������������� ��������
		this.header_view = header_view;
		this.context = context;
		this.item_layout = item_layout_id;
		this.column_info = column_info;
		this.additional_column_info = additional_column_info;
		this.checkbox_listeners = checkbox_listeners;
		layout_inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		objects = new ArrayList<List<CellInfo>>();
	}
	
	//���-�� ���������
	@Override
	public int getCount()
	{
		return objects.size();
	}
	
	//������� �� �������
	@Override
	public Object getItem(int position)
	{
		return objects.get(position);
	}
	
	//id �������
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	//����� ������
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		//�������� view-������ ����� ������ �������
		View view = convertView;
		if(view == null)
			view = layout_inflater.inflate(item_layout, parent, false);
		
		//�������� ���������� �� �������, ������� ����� ���������� �� ���� ������
		List<CellInfo> current_item = get_item_params_list(position);
		
		//��������� ��� ��������� ������
		int column_number = 0;
		Iterator<ColumnInfo> ci = column_info.iterator();
		Iterator<AdditionalColumnInfo> cw = additional_column_info.iterator();
		Iterator<CellInfo> it = current_item.iterator();
		Iterator<OnCheckedChangeListener> listeners_it = checkbox_listeners.iterator();
		
		//��������� view �������
		while(it.hasNext())
		{
			//�������� ������ � ����������
			ColumnInfo cur_column_info = ci.next();
			AdditionalColumnInfo cur_add_column_info = cw.next();
			CellInfo cur_cell_info = it.next();
		
			//�������� ������ header ��������
			int cur_column_width = cur_add_column_info.width;
			
			//�� ���� ������� ���������� ���������� ��������
			if(cur_column_info.col_type.equals(EColumnType.TEXT_COLUMN))	//���� �����
			{
				//�������� ��������� view �������
				TextView tv = (TextView) view.findViewById(cur_column_info.view_id);
				
				//���������� ��������� view ��������
				tv.setText(cur_cell_info.text_info);
				
				//���������� ��������� �������� ���� (������ � ������)
				LayoutParams lp = tv.getLayoutParams();
				lp.width = cur_column_width;
				tv.setLayoutParams(lp);
			}
			else if(cur_column_info.col_type.equals(EColumnType.IMAGE_COLUMN))	//���� �����������
			{
				//�������� view ������� �����������
				ImageView iv = (ImageView) view.findViewById(cur_column_info.view_id);
				
				//���������� ��������� view ��������
				iv.setImageResource(cur_cell_info.image_info);
				
				//���������� ��������� �������� ���� (������ � ������)
				LayoutParams lp = iv.getLayoutParams();
				lp.width = cur_column_width;
				iv.setLayoutParams(lp);
			}
			else if(cur_column_info.col_type.equals(EColumnType.CHECK_COLUMN))	//���� �������
			{
				//�������� �������
				CheckBox check_box = (CheckBox) view.findViewById(cur_column_info.view_id);
				
				//��������� ����������
				check_box.setOnCheckedChangeListener(listeners_it.next());
				
				//������� ������ � �������
				List<Integer> check_box_tag = new ArrayList<Integer>();
				check_box_tag.add(position);
				check_box_tag.add(column_number);
				check_box.setTag(check_box_tag);
				
				//���������� ��������� view ��������
				check_box.setChecked(cur_cell_info.check_info);
				
				//���������� ��������� �������� ���� (������ � ������)
				LayoutParams lp = check_box.getLayoutParams();
				lp.width = cur_column_width;
				check_box.setLayoutParams(lp);
			}
			
			//��������� ������� ��������
			column_number++;
		}
		
		return view;
	}
	
	
	
	/***** �����������	*****/
	
	//���������� ����������
	OnCheckedChangeListener check_state_change = new OnCheckedChangeListener()
	{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			//�������� TAG ����������
			List<Integer> check_box_tag = (List<Integer>) buttonView.getTag();
			int row = (Integer) check_box_tag.get(0);
			int col = (Integer) check_box_tag.get(1);
			
			//�������� ������
			List<CellInfo> object_info = get_item_params_list(row);
			object_info.set(col, new CellInfo(isChecked));
			set_item_params_list(row, object_info);
		}
	};
	
	
	
	
	/***** ��������������/��������������� ������	*****/
	
	//�������� ������� ������� ��� ������ ���������� �������
	public List<CellInfo> get_item_params_list(int position)
	{
		return (List<CellInfo>) getItem(position);
	}
	
	//���������� ����� ������ ���������� ��� ������� � �������� �������
	public void set_item_params_list(int position, List<CellInfo> object_info)
	{
		objects.set(position, object_info);
		
		//�������� ������
		notifyDataSetChanged();
	}
	
	//�������� ������
	public void add_object(List<CellInfo> object_info)
	{
		//���� ���-�� ���������� �� ��������� � ������ - ������ �� ������
		if(object_info.size() != column_info.size())
			return;
		
		//�������� ������
		objects.add(object_info);
		
		//�������� ������
		notifyDataSetChanged();
	}
	
	//������� ������
	public void remove_object(int position)
	{
		//��������� �������� �������
		if(position < 0 || position >= objects.size())
			return;
		
		//������� ������
		objects.remove(position);
		
		//�������� ������
		notifyDataSetChanged();
	}
	
	
	
	/***** �������� �������������� �������� � ������������	*****/
	
	//���� ��������
	public static enum EColumnType
	{
		TEXT_COLUMN,		//������� � �������
		IMAGE_COLUMN,		//������� � ������������
		CHECK_COLUMN		//������� � ���������
	};
	
	//�������������� ���������� � �������
	public static class AdditionalColumnInfo
	{
		//��������
		public int header_id_for_column;
		public int width;
		
		//�����������
		public AdditionalColumnInfo(int header_id_for_column, int width)
		{
			this.header_id_for_column = header_id_for_column;
			this.width = width;
		}
	};
	
	//���������� � �������
	public static class ColumnInfo
	{
		//��������
		public EColumnType col_type;		//��� �������
		public int view_id;					//�� view ��������, ����������� �� �������
		
		//�����������
		public ColumnInfo(EColumnType col_type, int view_id)
		{
			this.col_type = col_type;
			this.view_id = view_id;
		}
	};
	
	//���������� � ������ (�������� ��������� �������� �������)
	public static class CellInfo
	{
		//��������
		public EColumnType col_type;		//��� �������
		public String text_info;			//��������� ����������
		public int image_info;				//�����������
		public boolean check_info;			//���������� ��������
		
		//����������� ��� ��������� ����������
		public CellInfo(String text_info)
		{
			this.col_type = EColumnType.TEXT_COLUMN;
			this.text_info = text_info;
		}
		
		//����������� ��� �����������
		public CellInfo(int image_info)
		{
			this.col_type = EColumnType.IMAGE_COLUMN;
			this.image_info = image_info;
		}
		
		//����������� ��� ��������
		public CellInfo(boolean check_info)
		{
			this.col_type = EColumnType.CHECK_COLUMN;
			this.check_info = check_info;
		}
	}
}
