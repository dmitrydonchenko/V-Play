package vplay_engine;

import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.vplay.R;

//Класс приложения VPlay
public class VPlayApplication extends Application
{
	/*****	Свойства приложения	*****/
	
	//Указатель на себя
	private static VPlayApplication instance = null;
	
	//Указатель на ресурсы
	private static Resources resources = null;
	
	//Сервис для работы с USB
	private Intent usb_service;
	
	//Текущий activity подписанный на обновления
	private VPlayActivity current_vplay_activity = null;
	
	//База данных приложения
	private static VPlayDatabase database = null;
	

	/*****	Стандартные методы для работы с приложением	*****/
	
	//Инициализация приложения
	@Override
	public void onCreate()
	{
		//Получить указатель на ресурсы приложения
		resources = getResources();
	}
	
	
	
	/*****	Инициализация и деинициализация	*****/
	
	//Инициализация приложения
	public void init(VPlayApplication app_instance)
	{	
		//Получить указатель на себя
		instance = app_instance;
		
		//Инициализация базы данных
		database = new VPlayDatabase();
		database.init();
		
		//Инициализация сервиса для работы с USB
        usb_service = new Intent(this, UsbService.class);
        
        //Запуск сервиса для работы с USB
        startService(usb_service);
        
        //Инициализация класса отладки
		DebugClass.init_debug_class(this.getApplicationContext());
		
		DebugClass.message(R.string.toast_message_application_on);
	}
	
	//Деинициализация приложения
	public void deinit()
	{
		//Остановить сервис для работы с USB
		stopService(usb_service);
		
		//Деинициализация базы данных
		database.deinit();
		database = null;
		
		DebugClass.message(R.string.toast_message_application_off);
		
		//Обнулить указатель на себя
		instance = null;
	}
	
	
	
	/*****	Вспомогательные методы	*****/
	
	//Получить указатель на себя
	public static VPlayApplication get_instance()
	{
		return instance;
	}
	
	//Получить указатель на ресурсы приложения
	public static Resources get_resources()
	{
		return resources;
	}
	
	//Получить указатель на базу данных
	public static VPlayDatabase get_database()
	{
		return database;
	}
	
	
	/*****	Работа с подписчиками	*****/
	
	//Проверка - есть ли подписчик
	public boolean is_have_observer()
	{
		if(current_vplay_activity != null)
			return true;
		return false;
	}
	
	//Регистрация на подписку activity
	public void activity_registration(VPlayActivity observer)
	{
		current_vplay_activity = observer;
	}
	
	//Отписаться от подписки activity
	public void activity_unregistration(VPlayActivity observer)
	{
		//Если отписывается последний подписавшийся - то отписать его
		if(current_vplay_activity == observer)
			current_vplay_activity = null;
	}
	
	//Обновить информацию подписчика
	public void update_observer(Bundle data)
	{
		//Если подписчика нет - ничего не делать
		if(!is_have_observer())
			return;
		
		current_vplay_activity.update(data);
	}
}