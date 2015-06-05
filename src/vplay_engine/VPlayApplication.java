package vplay_engine;

import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.vplay.R;

//����� ���������� VPlay
public class VPlayApplication extends Application
{
	/*****	�������� ����������	*****/
	
	//��������� �� ����
	private static VPlayApplication instance = null;
	
	//��������� �� �������
	private static Resources resources = null;
	
	//������ ��� ������ � USB
	private Intent usb_service;
	
	//������� activity ����������� �� ����������
	private VPlayActivity current_vplay_activity = null;
	
	//���� ������ ����������
	private static VPlayDatabase database = null;
	

	/*****	����������� ������ ��� ������ � �����������	*****/
	
	//������������� ����������
	@Override
	public void onCreate()
	{
		//�������� ��������� �� ������� ����������
		resources = getResources();
	}
	
	
	
	/*****	������������� � ���������������	*****/
	
	//������������� ����������
	public void init(VPlayApplication app_instance)
	{	
		//�������� ��������� �� ����
		instance = app_instance;
		
		//������������� ���� ������
		database = new VPlayDatabase();
		database.init();
		
		//������������� ������� ��� ������ � USB
        usb_service = new Intent(this, UsbService.class);
        
        //������ ������� ��� ������ � USB
        startService(usb_service);
        
        //������������� ������ �������
		DebugClass.init_debug_class(this.getApplicationContext());
		
		DebugClass.message(R.string.toast_message_application_on);
	}
	
	//��������������� ����������
	public void deinit()
	{
		//���������� ������ ��� ������ � USB
		stopService(usb_service);
		
		//��������������� ���� ������
		database.deinit();
		database = null;
		
		DebugClass.message(R.string.toast_message_application_off);
		
		//�������� ��������� �� ����
		instance = null;
	}
	
	
	
	/*****	��������������� ������	*****/
	
	//�������� ��������� �� ����
	public static VPlayApplication get_instance()
	{
		return instance;
	}
	
	//�������� ��������� �� ������� ����������
	public static Resources get_resources()
	{
		return resources;
	}
	
	//�������� ��������� �� ���� ������
	public static VPlayDatabase get_database()
	{
		return database;
	}
	
	
	/*****	������ � ������������	*****/
	
	//�������� - ���� �� ���������
	public boolean is_have_observer()
	{
		if(current_vplay_activity != null)
			return true;
		return false;
	}
	
	//����������� �� �������� activity
	public void activity_registration(VPlayActivity observer)
	{
		current_vplay_activity = observer;
	}
	
	//���������� �� �������� activity
	public void activity_unregistration(VPlayActivity observer)
	{
		//���� ������������ ��������� ������������� - �� �������� ���
		if(current_vplay_activity == observer)
			current_vplay_activity = null;
	}
	
	//�������� ���������� ����������
	public void update_observer(Bundle data)
	{
		//���� ���������� ��� - ������ �� ������
		if(!is_have_observer())
			return;
		
		current_vplay_activity.update(data);
	}
}