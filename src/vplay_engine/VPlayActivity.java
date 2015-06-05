package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

//Класс activity окна приложения VPlay
public abstract class VPlayActivity extends ActionBarActivity
{
	//При создании окна
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Стандартная инициализация окна
        super.onCreate(savedInstanceState);
        
        //Зарегистрировать окно на получение сообщений от USB устройства
        VPlayApplication app = VPlayApplication.get_instance();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
        app.activity_registration(this);
	}
	
	//При уничтожении окна
	@Override
	protected void onDestroy()
	{
		//Стандартное уничтожение окна
		super.onDestroy();
				
		//Отписаться от получения сообщений
		VPlayApplication app = VPlayApplication.get_instance();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
        app.activity_unregistration(this);
	}
	
	//Обновление при получении пакета данных
	protected final void update(Bundle data)
	{
		//Получение стандартных данных о пакете
		UsbService.EDevicePackageType package_type = (UsbService.EDevicePackageType) data.getSerializable("message_type");	//Тип пакета
		short remote_receiver_id = data.getShort("remote_receiver_id");		//Номер комплекта
		byte remote_id = data.getByte("remote_id");							//Номер пульта в комплекте
		
		//Если пакет с информацией о нажатой кнопке
		if(package_type.equals(UsbService.EDevicePackageType.BUTTON_PACKAGE))
		{
			//Получить данные о кнопке
			UsbService.EDeviceKey key = (UsbService.EDeviceKey) data.getSerializable("key");
			
			//Обновить подписчика
			update_key(remote_receiver_id, remote_id, key);
		}
		else if(package_type.equals(UsbService.EDevicePackageType.RFID_PACKAGE))
		{
			//Получить данные о считанной метке
			String uid = data.getString("uid");

			//Обновить подписчика
			update_rfid(remote_receiver_id, remote_id, uid);
		}
		
		//Выслать команду о завершении всех процедур на пульте
		UsbService usb_service = UsbService.get_instance();
		usb_service.send_rf_ack_command(remote_receiver_id, remote_id);
	}
	
	//Обновление при получении пакета данных при нажатии кнопки на устройстве
	public abstract void update_key(short build_number, byte device_number, UsbService.EDeviceKey key);
	
	//Обновление при получении пакета данных при считывании RFID метки на устройстве
	public abstract void update_rfid(short build_number, byte device_number, String rfid);
}
