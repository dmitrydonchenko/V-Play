package vplay_engine;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

//�����, ����������� ������ ��� ������� � ������� ����� ������ ����������
public class DebugClass
{
	private static final String debug_log_filter = "debug_message";
	private static Context application_context = null;
	
	//������������� ������ �������
	public static void init_debug_class(Context context)
	{
		application_context = context;
	}
	
	//����� debug ��������� � ���
	public static void print(String debug_message)
	{
		Log.d(debug_log_filter, debug_message);
	}
	
	//����� ������������ ���������
	public static void message(String message)
	{
		if(application_context == null)
			return;
		Toast t = Toast.makeText(application_context, message, Toast.LENGTH_SHORT);
		if(message.equals(""))
			return;
		t.show();
	}
	
	//����� ������������ ��������� �� ID ������ �� ��������
	public static void message(int string_id)
	{
		//�������� �������
		Resources resources = VPlayApplication.get_resources();
		
		//�������� ������
		String s = resources.getString(string_id);
		
		//����� ������������ ���������
		message(s);
	}
	
	//����� debug ��������� � ��� � ������ �� ������������ ���������
	public static void print_log_and_message(String message)
	{
		if(application_context == null)
			return;
		print(message);
		message(message);
	}
}
