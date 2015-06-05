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
	
	/***** �������� �������	*****/
	
	//����������� ������
	private View view;					//������������� �������
	private VPlayAdapter adapter;		//������� ��� �������
	private List<Integer> headers_id;	//�������������� ��� ���������� (view id)
	
	
	
	
	/*****	�������� ������ ��� ������	*****/
	
	//�����������
	public VPlayTable(Context context, LinearLayout main_linear_layout, int item_layout_id, List<HeaderCellInfo> header_info, List<ColumnInfo> column_info, List<OnCheckedChangeListener> checkbox_listeners)
	{
		//�������� �������
		LayoutInflater layout_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = layout_inflater.inflate(R.layout.vplay_table_view, main_linear_layout, false);
		
		//������� ������ ��� �������� �� ����������
		headers_id = new ArrayList<Integer>();
		
		//�������� LinearLayout ��� header
		LinearLayout header = (LinearLayout) view.findViewById(R.id.vplay_table_header);
		
		//������������ ���������
		for(Iterator<HeaderCellInfo> it = header_info.iterator(); it.hasNext(); )
		{
			//�������� ���������� � ����� ������ ���������
			HeaderCellInfo cell_info = it.next();
			
			//������� ��������� View
			TextView tv = new TextView(context);
			
			//��������� ��� ����� ������������� � ��������� ���
			int new_id = View.generateViewId();
			headers_id.add(new_id);
			tv.setId(new_id);
			
			//���������� ��������� �����������
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.width = cell_info.width;
			lp.height = LayoutParams.WRAP_CONTENT;
			tv.setLayoutParams(lp);
			
			//���������� ������ ��� ���������� view
			tv.setText(cell_info.text);
			
			//�������� ��������� View � ���������
			header.addView(tv);
		}
		
		//�������� ������� �� ������� �����
		main_linear_layout.addView(view);
		
		//������� ������ ��� ����������� ������
		List<AdditionalColumnInfo> additional_column_info = new ArrayList<AdditionalColumnInfo>();
		
		//�������� ���������
		Iterator<HeaderCellInfo> it = header_info.iterator();
		Iterator<Integer> headers_id_it = headers_id.iterator();
		
		//�������� ������ ������� �������� � �������
		while(it.hasNext())
		{
			//�������� ���������� � ����� ������ ���������
			HeaderCellInfo cell_info = it.next();
			
			//�������� �������������
			int header_id = headers_id_it.next();
			
			//������ �� ���� view ���������, ������� ������ �������� ��������� � ��������� ��� ��� ������
			for(int i = 0; i < cell_info.view_cnt; i++)
				additional_column_info.add(new AdditionalColumnInfo(header_id, cell_info.width));
		}
		
		//�������� ��������
		adapter = new VPlayAdapter(context, item_layout_id, column_info, additional_column_info, header, checkbox_listeners);
		
		//��������� ������
		ListView lvMain = get_main_list();
		lvMain.setAdapter(adapter);
	}
	
	//�������� ������� (��� ���������)
	public ListView get_main_list()
	{
		ListView lv = (ListView) view.findViewById(R.id.vplay_table_main_list);
		return lv;
	}
	
	//�������� ������� ������� ��� ������ ���������� �������
	public List<CellInfo> get_item_params_list(int position)
	{
		return adapter.get_item_params_list(position);
	}
	
	//���������� ����� ������ ���������� ��� ������� � �������� �������
	public void set_item_params_list(int position, List<CellInfo> object_info)
	{
		adapter.set_item_params_list(position, object_info);
	}
	
	//�������� ������
	public void add_object(List<CellInfo> object_info)
	{
		adapter.add_object(object_info);
	}
	
	//������� ������
	public void remove_object(int position)
	{
		adapter.remove_object(position);
	}
	
	//�������� �������
	public void clear_table()
	{
		while(adapter.getCount() > 0)
		{
			remove_object(0);
		}
	}
	
	//���-�� ���������
	public int getCount()
	{
		return adapter.getCount();
	}
	
	
	
	
	/*****	�������������� ������/���������/������������	*****/
	
	//���������� �� ����� ������ ���������
	public static class HeaderCellInfo
	{
		//��������
		public String text;			//�������
		public int view_cnt;		//���-�� ��������� � ������� (������), ������� ���������� ���������
		public int width;
		
		//�����������
		public HeaderCellInfo(String text, int view_cnt, int width)
		{
			this.text = text;
			this.view_cnt = view_cnt;
			this.width = width;
		}
	};
}
