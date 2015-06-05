package com.vplay;

import vplay_engine.DebugClass;
import vplay_engine.VPlayApplication;
import vplay_engine.VPlayDatabase;
import vplay_engine.VPlayOnlineDatabase;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

//Окно настроек
public class SettingActivity extends ActionBarActivity
{
	//Создание окна настроек
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Стандартная инициализация
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
	}
	
	//При нажатии на кнопку регистрации устройств
	public void on_click_device_registration_button(View v)
	{
		//Переход в activity - Регистрация устройств
    	Intent intent = new Intent(this, DeviceRegistrationActivity.class);
		startActivity(intent);
	}
	
	//При нажатии на кнопку обновления базы данных карт
	public void on_click_card_update_button(View v)
	{
		//Скачать базу данных и заполнить данными бд
        String database_data = VPlayOnlineDatabase.get_database_string();
        if(database_data == "")
        {
        	DebugClass.message("Не удалось подключиться к онлайн базе данных");
        }
        else
        {
        	//Получить указатель на приложение
    		VPlayApplication app = VPlayApplication.get_instance();
    		
    		//Получить указатель на базу данных
    		VPlayDatabase db = app.get_database();
    		
    		//Заполнить
    		db.set_card_database_by_string(database_data);
    		
    		DebugClass.message("Успешное обновление");
        }
	}
}