package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.os.Handler;

//Класс для управления handler-ами
public class HandlerControlClass
{
	/*****	Handlers	*****/
	
	//Вывод всплывающих сообщений
	public static final Handler pop_up_message_handler = new Handler()
    {
		public void handleMessage(android.os.Message msg)
		{
			//Вытянуть данные
			Bundle data = msg.getData();
			
			//Отобразить всплывающее сообщение
			DebugClass.message(data.getString("message_string"));
		};
    };
    
    //Прием предварительно обработаных пакетов с USB сервиса и передача в VPlay
    public static final Handler usb_package_handler = new Handler()
    {
    	public void handleMessage(android.os.Message msg)
    	{
    		//Вытянуть данные
    		Bundle data = msg.getData();
    		
    		//Если класс не проинициализирован - ничего не делать
    		VPlayApplication app = VPlayApplication.get_instance();
    		if(app == null)
    		{
    			//DebugClass.message(R.string.error_application_not_found);
    			return;
    		}
    		
    		//Отправить данные подписчику
    		app.update_observer(data);
    	}
    };
}
