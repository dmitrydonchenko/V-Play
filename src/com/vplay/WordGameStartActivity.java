package com.vplay;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import vplay_engine.DebugClass;
import vplay_engine.VPlayApplication;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class WordGameStartActivity extends Activity {
	
/*****	Свойства окна	*****/
	
	//Место для хранения списка спиннера и соответствующих файлов
	HashMap<String, String> spinner_map = new HashMap<String, String>();	
	
	/*****	Стандартные методы	*****/

	//Точка входа в игру
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_word_game_start);
		
		//Инициализация спиннеров
		init_spinners();
	}	
	
	//Инициализация спиннера
	private void init_spinners()
	{
		//Список для спиннера
		List<String> spinner_list = new ArrayList<String>();
		
		//Получение указателя на приложение
		VPlayApplication app = VPlayApplication.get_instance();
		
		//Получение контекста
		Context context = app.getApplicationContext();
		
		//Получение ассета
		AssetManager asset_manager = context.getAssets();
		
		//Поток для обработки файлов ассета
		InputStream is = null;
		
		//Объект чтения файла
		BufferedReader reader = null;
		
		//Строка для чтения файлов построчно
		String line;
		
		//Открыть файл с игровыми темами
		try
		{
			is = asset_manager.open("word_game/game_themes.txt");
		}
		catch(IOException e)
		{
			DebugClass.message("Отсутствует файл с темами для игры");
			return;
		}
		
		try {
			Scanner in = new Scanner(asset_manager.open("word_game/game_themes.txt"));	
			int index = 1;
			String theme_name= "", theme_filename = "";
			while (in.hasNext())
			{
				if(index % 2 == 1)
				{
					theme_name = in.nextLine();
					if(index == 1)
					{
						theme_name = theme_name.substring(1, theme_name.length());
					}
				}
				else
				{
					theme_filename = in.nextLine();
					//Добавить в мап
					spinner_map.put(theme_name, theme_filename);
					
					//Добавить в список
					spinner_list.add(theme_name);
				}
				index++;
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Чтение записей из файла
		/*try
		{
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		}
		catch(UnsupportedEncodingException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try
		{
			while((line = reader.readLine()) != null)
			{
				//Получена след. тема
				String theme_name = line.trim();
				String theme_filename = reader.readLine().trim();
				
				//Добавить в мап
				spinner_map.put(theme_name, theme_filename);
				
				//Добавить в список
				spinner_list.add(theme_name);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		//Закрыть файл 
		try
		{
			is.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}*/
		
		//Адаптер для спинера
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinner_list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		//Получить спинер с GUI интерфейса
		Spinner spinner = (Spinner) findViewById(R.id.wordgame_start_game_theme_spinner);
		
		//Установить на него адаптер
		spinner.setAdapter(adapter);
		
		//Выбрать первый элемент в списке
		spinner.setSelection(0);
	}	
	
	
	/*****	Методы нажатия кнопок	*****/
	
	//Нажатие на кнопку - начать игру
	public void on_click_wordgame_start_start_game_button(View v)
	{
		//Получить спинер с GUI интерфейса
		Spinner spinner = (Spinner) findViewById(R.id.wordgame_start_game_theme_spinner);		
				
		//Получить текущее название темы
		String theme_name = (String)spinner.getSelectedItem();
		
		Intent i = new Intent(getApplicationContext(), WordGameActivity.class);
		i.putExtra("game_theme_name", theme_name);
		i.putExtra("game_theme", spinner_map.get(theme_name));
		startActivity(i);
	}
}
