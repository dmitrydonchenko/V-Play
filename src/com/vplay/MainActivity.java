package com.vplay;

import vplay_engine.DebugClass;
import vplay_engine.VPlayMainActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends VPlayMainActivity
{
	
	
	
	/*****	Стандартные методы	*****/
	
	//При запуске главного окна приложения
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	//Стандартная инициализация окна
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    //При уничтожении главного окна приложения
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    }
    
    
    
    /***** События нажатия кнопок	*****/
    
    //Нажатие на кнопку перехода к игре Клад
    public void on_click_treasure_game_button(View v)
    {
    	//Переход в activity - Игра Клад
    	Intent intent = new Intent(this, TreasureStartActivity.class);
		startActivity(intent);
    }
    
    //Нажатие на кнопку перехода к игре Словодел
    public void on_click_wordmaker_game_button(View v)
    {
    	//Переход в activity - Игра Словодел
    	Intent intent = new Intent(this, WordmakerStartActivity.class);
		startActivity(intent);
    }
    
  //Нажатие на кнопку перехода к игре Покорми меня
    public void on_click_feedme_game_button(View v)
    {
    	//Переход в activity - Игра Покорми меня
    	Intent intent = new Intent(this, FeedMeActivity.class);
		startActivity(intent);
    }
    
    // Нажатие на кнопку перехода к игре Объединение слов
    public void on_click_wordgame_button(View v)
    {
    	//Переход в activity - Игра Покорми меня
    	Intent intent = new Intent(this, WordGameStartActivity.class);
		startActivity(intent);
    }
    
    //Нажатие на кнопку Настройки
    public void on_click_setting_button(View v)
    {
    	//Переход в activity - Настройки
    	Intent intent = new Intent(this, SettingActivity.class);
		startActivity(intent);
    }
}
