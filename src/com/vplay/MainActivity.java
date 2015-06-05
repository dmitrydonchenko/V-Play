package com.vplay;

import vplay_engine.DebugClass;
import vplay_engine.VPlayMainActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends VPlayMainActivity
{
	
	
	
	/*****	����������� ������	*****/
	
	//��� ������� �������� ���� ����������
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	//����������� ������������� ����
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    //��� ����������� �������� ���� ����������
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    }
    
    
    
    /***** ������� ������� ������	*****/
    
    //������� �� ������ �������� � ���� ����
    public void on_click_treasure_game_button(View v)
    {
    	//������� � activity - ���� ����
    	Intent intent = new Intent(this, TreasureStartActivity.class);
		startActivity(intent);
    }
    
    //������� �� ������ �������� � ���� ��������
    public void on_click_wordmaker_game_button(View v)
    {
    	//������� � activity - ���� ��������
    	Intent intent = new Intent(this, WordmakerStartActivity.class);
		startActivity(intent);
    }
    
  //������� �� ������ �������� � ���� ������� ����
    public void on_click_feedme_game_button(View v)
    {
    	//������� � activity - ���� ������� ����
    	Intent intent = new Intent(this, FeedMeActivity.class);
		startActivity(intent);
    }
    
    // ������� �� ������ �������� � ���� ����������� ����
    public void on_click_wordgame_button(View v)
    {
    	//������� � activity - ���� ������� ����
    	Intent intent = new Intent(this, WordGameStartActivity.class);
		startActivity(intent);
    }
    
    //������� �� ������ ���������
    public void on_click_setting_button(View v)
    {
    	//������� � activity - ���������
    	Intent intent = new Intent(this, SettingActivity.class);
		startActivity(intent);
    }
}
