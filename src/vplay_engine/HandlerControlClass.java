package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.os.Handler;

//����� ��� ���������� handler-���
public class HandlerControlClass
{
	/*****	Handlers	*****/
	
	//����� ����������� ���������
	public static final Handler pop_up_message_handler = new Handler()
    {
		public void handleMessage(android.os.Message msg)
		{
			//�������� ������
			Bundle data = msg.getData();
			
			//���������� ����������� ���������
			DebugClass.message(data.getString("message_string"));
		};
    };
    
    //����� �������������� ����������� ������� � USB ������� � �������� � VPlay
    public static final Handler usb_package_handler = new Handler()
    {
    	public void handleMessage(android.os.Message msg)
    	{
    		//�������� ������
    		Bundle data = msg.getData();
    		
    		//���� ����� �� ������������������ - ������ �� ������
    		VPlayApplication app = VPlayApplication.get_instance();
    		if(app == null)
    		{
    			//DebugClass.message(R.string.error_application_not_found);
    			return;
    		}
    		
    		//��������� ������ ����������
    		app.update_observer(data);
    	}
    };
}
