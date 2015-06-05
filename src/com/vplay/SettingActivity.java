package com.vplay;

import vplay_engine.DebugClass;
import vplay_engine.VPlayApplication;
import vplay_engine.VPlayDatabase;
import vplay_engine.VPlayOnlineDatabase;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

//���� ��������
public class SettingActivity extends ActionBarActivity
{
	//�������� ���� ��������
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//����������� �������������
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
	}
	
	//��� ������� �� ������ ����������� ���������
	public void on_click_device_registration_button(View v)
	{
		//������� � activity - ����������� ���������
    	Intent intent = new Intent(this, DeviceRegistrationActivity.class);
		startActivity(intent);
	}
	
	//��� ������� �� ������ ���������� ���� ������ ����
	public void on_click_card_update_button(View v)
	{
		//������� ���� ������ � ��������� ������� ��
        String database_data = VPlayOnlineDatabase.get_database_string();
        if(database_data == "")
        {
        	DebugClass.message("�� ������� ������������ � ������ ���� ������");
        }
        else
        {
        	//�������� ��������� �� ����������
    		VPlayApplication app = VPlayApplication.get_instance();
    		
    		//�������� ��������� �� ���� ������
    		VPlayDatabase db = app.get_database();
    		
    		//���������
    		db.set_card_database_by_string(database_data);
    		
    		DebugClass.message("�������� ����������");
        }
	}
}