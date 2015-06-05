package vplay_engine;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

//Класс, реализующий методы для отладки и ведения логов работы приложения
public class DebugClass
{
	private static final String debug_log_filter = "debug_message";
	private static Context application_context = null;
	
	//Инициализация класса отладки
	public static void init_debug_class(Context context)
	{
		application_context = context;
	}
	
	//Вывод debug сообщения в лог
	public static void print(String debug_message)
	{
		Log.d(debug_log_filter, debug_message);
	}
	
	//Вывод всплывающего сообщения
	public static void message(String message)
	{
		if(application_context == null)
			return;
		Toast t = Toast.makeText(application_context, message, Toast.LENGTH_SHORT);
		if(message.equals(""))
			return;
		t.show();
	}
	
	//Вывод всплывающего сообщения по ID строки из ресурсов
	public static void message(int string_id)
	{
		//Получить ресурсы
		Resources resources = VPlayApplication.get_resources();
		
		//Получить строку
		String s = resources.getString(string_id);
		
		//Вывод всплывающего сообщения
		message(s);
	}
	
	//Вывод debug сообщения в лог и такого же всплывающего сообщения
	public static void print_log_and_message(String message)
	{
		if(application_context == null)
			return;
		print(message);
		message(message);
	}
}
