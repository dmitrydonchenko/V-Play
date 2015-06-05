package vplay_engine;

import com.vplay.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;

//Сервис для работы с USB
public class UsbService extends Service
{
	
	
	
	/*****	Свойства сервиса	*****/
	
	//Указатель на себя
	private static UsbService instance = null;
	
	//Состояние USB устройства
	private EDeviceState state = EDeviceState.NO_DEVICE;	//По умолчанию - нет подключенного устройства
		
	//Потоки сервиса
	private Thread usb_thread;								//Поток для работы с USB устройством
	
	//Дополнительные данные
	private UsbManager usb_manager;							//Системный сервис для работы с USB
	private PendingIntent permission_intent;				//...
	private UsbDevice device;								//Подключенное устройство
	private UsbDeviceConnection usb_device_connection;		//Информация о связи с устройством USB для взаимодействия
	private UsbInterface usb_interface;						//Интерфейс USB устройства
	private UsbEndpoint usb_endpoint;						//Источник/приемник данных USB
	private boolean status = false;							//Состояние сервиса (вкл или выкл)
	
	private int send_count = 5;								//Кол-во попыток отослать пакет данных
	
	//Данные для работы с кодировками
	private static final String WINDOWS_1251_RUS = "ЂЃ‚ѓ„…†‡€‰Љ‹ЊЌЋЏђ‘’“”•–— ™љ›њќћџ ЎўЈ¤Ґ¦§Ё©Є«¬­®Ї°±Ііґµ¶·ё№є»јЅѕїАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя";
	
	//Данные для работы с USB
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";		//...
	
	
	//Контейнер для хранения флагов ожидания метки rfid
	HashMap<Short, HashMap<Byte, Boolean>> rfid_query_flags = new HashMap<Short, HashMap<Byte, Boolean>>();
	
	//Контейнер для хранения флагов ожидания ответа на запрос
	HashMap<Short, HashMap<Byte, Boolean>> answer_query_flags = new HashMap<Short, HashMap<Byte, Boolean>>();

	
	/*****	Стандартные методы сервиса	*****/
	
	//При создании сервиса
	@Override
	public void onCreate()
	{
		//Получить указатель на себя
		instance = this;
		
		//Инициализация статуса сервиса
		status = true;
		
		//Проинициализировать USB
		usb_manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		permission_intent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		
		//Проинициализировать потоки и запустить
		init_threads();
		
		super.onCreate();
	}
	
	//При старте сервиса
	public int onStartCommand(Intent intent, int flags, int startId)
	{	
		send_message_to_handler(R.string.toast_message_usb_service_started);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	//Действия при уничтожении сервиса
	public void onDestroy()
	{
		//Обнулить указатель на себя
		instance = null;
		
		//Установка статуса в позицию выкл
		status = false;
		
		send_message_to_handler(R.string.toast_message_usb_service_stoped);
		
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	/***** Вспомогательные методы сервиса	*****/
	
	//Получить указатель на себя
	public static UsbService get_instance()
	{
		return instance;
	}
	
	//Инициализация и запуск потоков
	private void init_threads()
	{
		//Инициализация потоков
		usb_thread = new Thread(null, usb_thread_runnable);
		
		//Запуск потоков
		usb_thread.start();
	}
	
	//Передача сообщения handler-у
	private void send_message_to_handler(String debug_message)
	{
		//Составление данных, для сообщения Handler-у
		Message msg = new Message();
		msg.setTarget(HandlerControlClass.pop_up_message_handler);
		Bundle msg_data = new Bundle();
		
		//Передача сообщения
		DebugClass.print(debug_message);
		msg_data.putString("message_string", debug_message);
		msg.setData(msg_data);
		HandlerControlClass.pop_up_message_handler.sendMessage(msg);
	}
	
	//Передача сообщения handler-у через ID ресурса строки
	private void send_message_to_handler(int debug_message_id)
	{
		//Получить доступ к ресурсам
		Resources resources = VPlayApplication.get_resources();
		
		//Получить строку из ресурсов по ID
		String debug_message = resources.getString(debug_message_id);
		
		//Составление данных, для сообщения Handler-у
		Message msg = new Message();
		msg.setTarget(HandlerControlClass.pop_up_message_handler);
		Bundle msg_data = new Bundle();
		
		//Передача сообщения
		DebugClass.print(debug_message);
		msg_data.putString("message_string", debug_message);
		msg.setData(msg_data);
		HandlerControlClass.pop_up_message_handler.sendMessage(msg);
	}
	
	//Перевод строки в кодировке UTF-8 в массив байт в кодировке Windows-1251
	//text - текст для перевода
	//return - массив байт (длина которого на единицу больше, чем текст)
	private static byte [] get_windows_encoding_bytes(String text)
	{
		//Дополнительные данные для расшифровки кодировки
		byte [] windows_bytes = new byte[WINDOWS_1251_RUS.length()];
		byte start_byte = (byte) 0x80;
		windows_bytes[0] = start_byte;
		for(int i = 1; i < WINDOWS_1251_RUS.length(); i++)
		{
			if(start_byte + 1 != 0x98)
			{
				windows_bytes[i] = start_byte++;
			}
			else
			{
				start_byte++;
				start_byte++;
				windows_bytes[i] = start_byte;
			}
		}
		
		
		//Массив байт для строки
		byte [] bytes = new byte[text.length() + 1];
		
		
		//Перевод строки
		for(int i = 0; i < text.length(); i++)
		{
			int index = WINDOWS_1251_RUS.indexOf(text.charAt(i));
			
			if(text.charAt(i) == ' ')
			{
				bytes[i] = 0x20;
			}
			else if(text.charAt(i) == '*')
			{
				bytes[i] = 0x2A;
			}
			else if(index == -1)
			{
				bytes[i] = text.getBytes(Charset.forName("ASCII"))[i];
			}
			else
			{
				bytes[i] = (byte)(windows_bytes[index] + 1);
			}
		}
		
		bytes[text.length()] = 0x00;
		return bytes;
	}
	
	//Перевод массива байт в строку hex
	private static String bytes_to_hex(byte[] bytes)
	{
		int x;
		char []hex_array = "0123456789ABCDEF".toCharArray();
		String result = "";
		for(int i = 0; i < bytes.length; i++)
		{
			x = bytes[i] & 0xFF;
			result += hex_array[x >>> 4];
			result += hex_array[x & 0x0F];
			if(i != bytes.length - 1)
				result += '-';
		}
		return result;
	}
	
	//Получить кнопку устройства по коду
	private EDeviceKey get_device_key(int key_code)
	{
		if(key_code == 127)
			return EDeviceKey.DELETE_BUTTON;
		if(key_code == 13)
			return EDeviceKey.SEND_BUTTON;
		if(key_code == 44)
			return EDeviceKey.ADDITIONAL1_BUTTON;
		if(key_code == 49)
			return EDeviceKey.ONE_BUTTON;
		if(key_code == 50)
			return EDeviceKey.TWO_BUTTON;
		if(key_code == 51)
			return EDeviceKey.THREE_BUTTON;
		if(key_code == 52)
			return EDeviceKey.FOUR_BUTTON;
		if(key_code == 53)
			return EDeviceKey.FIVE_BUTTON;
		if(key_code == 54)
			return EDeviceKey.SIX_BUTTON;
		if(key_code == 55)
			return EDeviceKey.SEVEN_BUTTON;
		if(key_code == 56)
			return EDeviceKey.EIGHT_BUTTON;
		if(key_code == 57)
			return EDeviceKey.NINE_BUTTON;
		if(key_code == 46)
			return EDeviceKey.ADDITIONAL2_BUTTON;
		if(key_code == 48)
			return EDeviceKey.ZERO_BUTTON;
		if(key_code == 2)
			return EDeviceKey.T2_BUTTON;
		return EDeviceKey.INCORRECT_BUTTON;
	}
	
	
	//Изменить состояние ожидания метки для пульта
	private void change_rfid_query_flags_state(short remote_receiver_id, byte byte_remote_id, Boolean state)
	{
		//Если информации об ожидании пакета для конкретного пульта не найдена - создать
		if(!rfid_query_flags.containsKey(remote_receiver_id))
		{
			HashMap<Byte, Boolean> cur_map = new HashMap<Byte, Boolean>();
			cur_map.put(byte_remote_id, state);
			rfid_query_flags.put(remote_receiver_id, cur_map);
		}
		else
		{
			rfid_query_flags.get(remote_receiver_id).put(byte_remote_id, state);
		}
	}
	
	//Вернуть состояние ожидания метки для пульта
	private Boolean get_rfid_query_flags_state(short remote_receiver_id, byte byte_remote_id)
	{
		//Если информации об ожидании пакета для конкретного пульта не найдена - создать
		if(!rfid_query_flags.containsKey(remote_receiver_id))
		{
			HashMap<Byte, Boolean> cur_map = new HashMap<Byte, Boolean>();
			cur_map.put(byte_remote_id, Boolean.FALSE);
			rfid_query_flags.put(remote_receiver_id, cur_map);
		}
		else if(!rfid_query_flags.get(remote_receiver_id).containsKey(byte_remote_id))
		{
			rfid_query_flags.get(remote_receiver_id).put(byte_remote_id, Boolean.FALSE);
		}
		
		return rfid_query_flags.get(remote_receiver_id).get(byte_remote_id);
	}
	
	//Изменить состояние ожидания ответа на запрос пульта
	private void change_answer_query_flags_state(short remote_receiver_id, byte byte_remote_id, Boolean state)
	{
		//Если информации об ожидании пакета для конкретного пульта не найдена - создать
		if(!answer_query_flags.containsKey(remote_receiver_id))
		{
			HashMap<Byte, Boolean> cur_map = new HashMap<Byte, Boolean>();
			cur_map.put(byte_remote_id, state);
			answer_query_flags.put(remote_receiver_id, cur_map);
		}
		else
		{
			answer_query_flags.get(remote_receiver_id).put(byte_remote_id, state);
		}
	}
	
	//Вернуть состояние ожидания ответа на запрос пульта
	private Boolean get_answer_query_flags_state(short remote_receiver_id, byte byte_remote_id)
	{
		//Если информации об ожидании пакета для конкретного пульта не найдена - создать
		if(!answer_query_flags.containsKey(remote_receiver_id))
		{
			HashMap<Byte, Boolean> cur_map = new HashMap<Byte, Boolean>();
			cur_map.put(byte_remote_id, Boolean.FALSE);
			answer_query_flags.put(remote_receiver_id, cur_map);
		}
		else if(!rfid_query_flags.get(remote_receiver_id).containsKey(byte_remote_id))
		{
			answer_query_flags.get(remote_receiver_id).put(byte_remote_id, Boolean.FALSE);
		}
		
		return answer_query_flags.get(remote_receiver_id).get(byte_remote_id);
	}
	
	
	/*****	Дополнительные структуры данных, перечисления, классы и прочее	*****/
	
	//Состояние поиска USB устройств
	private enum EDeviceState
	{
		DEVICE_ERROR,							//Состояние ошибки
		NO_DEVICE,								//Устройство не найдено
		HAVE_DEVICE_WITHOUT_PERMISSION,			//Устройство найдено, но нет доступа к нему
		HAVE_DEVICE_WITH_REQUEST_PERMISSION,	//Устройство найдено, выслан запрос на доступ к устройству
		HAVE_DEVICE_WITH_PERMISSION,			//Есть устройство с доступом
		HAVE_DEVICE								//Есть устройство и оно готово для взаимодействия
	};
	
	//Типы пакетов с устройства USB
	public enum EDevicePackageType
	{
		BUTTON_PACKAGE,		//Нажатие на кнопку USB устройства
		RFID_PACKAGE		//Считывание метки RFID
	};
	
	//Кнопки устройства
	public enum EDeviceKey
	{
		INCORRECT_BUTTON,	//Ошибочная кнопка
		DELETE_BUTTON,		//Delete
		SEND_BUTTON,		//Send
		ADDITIONAL1_BUTTON,	//,?()
		ONE_BUTTON,			//1
		TWO_BUTTON,			//2
		THREE_BUTTON,		//3
		FOUR_BUTTON,		//4
		FIVE_BUTTON,		//5
		SIX_BUTTON,			//6
		SEVEN_BUTTON,		//7
		EIGHT_BUTTON,		//8
		NINE_BUTTON,		//9
		ADDITIONAL2_BUTTON,	//:,/-+=
		ZERO_BUTTON,		//0-
		T2_BUTTON			//T2
	};
	
	
	
	/*****	Методы для взаимодействия с устройством	*****/
	
	//Послать команду завершения всех процедур на пульте
	//remote_receiver_id - номер комплекта
	//byte_remote_id - номер пульта
	public void send_rf_ack_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("Начало - rfack");
		
		//Если устройство не подключено полностью - ничего не делать
		//if(!state.equals(EDeviceState.HAVE_DEVICE))
		//	return;
		
		//Если устройство не ожидает ответа - ничего не делать
		//if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
		//	return;
		
		//Изменить состояние ожидания
		change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
				
		//Длина буфера
		int buffer_length = 6;
		
		//Создание буфера
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//Перевод формата представления битов в little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//По умолчанию данные
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//Длина пакета
		char package_length = 4;
		output_buffer.put((byte) package_length);
		
		//Номер комплекта
		output_buffer.putShort(remote_receiver_id);
		
		//Номер пульта
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//Перевод в массив байтов
		byte [] output_buffer_array = output_buffer.array();
		
		//Отправка данных
		boolean success = false;
		for(int i = 0; i < send_count; i++)
		{
			int result = usb_device_connection.controlTransfer(0x21, 0x9, 0x200, 0, output_buffer_array, buffer_length, 0);
			if(result != -1)
			{
				success = true;
				break;
			}
		}
		if(!success)
		{
			send_message_to_handler("Ошибка - rfack");
			send_message_to_handler(R.string.error_usb_transfer_data);
		}
		//send_message_to_handler("Конец - rfack");
	}
	
	//Послать команду ожидания
	//remote_receiver_id - номер комплекта
	//byte_remote_id - номер пульта
	private void send_wait_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("Начало - послать команду ожидания");
		
		//Если устройство не подключено полностью - ничего не делать
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
				
		//Длина буфера
		int buffer_length = 7;
		
		//Создание буфера
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//Перевод формата представления битов в little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//По умолчанию данные
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//Длина пакета
		char package_length = 5;
		output_buffer.put((byte) package_length);
		
		//Номер комплекта
		output_buffer.putShort(remote_receiver_id);
		
		//Номер пульта
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//Команда чтения метки
		char CMD_WAIT = 18;
		output_buffer.put((byte) CMD_WAIT);
		
		//Перевод в массив байтов
		byte [] output_buffer_array = output_buffer.array();
		
		//Отправка данных
		boolean success = false;
		for(int i = 0; i < send_count; i++)
		{
			int result = usb_device_connection.controlTransfer(0x21, 0x9, 0x200, 0, output_buffer_array, buffer_length, 0);
			if(result != -1)
			{
				success = true;
				break;
			}
		}
		if(!success)
		{
			send_message_to_handler("Ошибка - послать команду ожидания");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);	
		}
		//send_message_to_handler("Конец - послать команду ожидания");
	}
	
	//Послать команду чтения RF_ID метки
	//remote_receiver_id - номер комплекта
	//byte_remote_id - номер пульта
	public void send_read_rf_id_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("Начало - чтение Rfid");
		
		//Если устройство не подключено полностью - ничего не делать
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//Если устройство не ожидает ответа - ничего не делать
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;
		
		//Длина буфера
		int buffer_length = 7;
		
		//Создание буфера
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//Перевод формата представления битов в little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//По умолчанию данные
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//Длина пакета
		char package_length = 5;
		output_buffer.put((byte) package_length);
		
		//Номер комплекта
		output_buffer.putShort(remote_receiver_id);
		
		//Номер пульта
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//Команда чтения метки
		char CMD_TAG_READ = 15;
		output_buffer.put((byte) CMD_TAG_READ);
		
		//Перевод в массив байтов
		byte [] output_buffer_array = output_buffer.array();
		
		//Отправка данных
		boolean success = false;
		for(int i = 0; i < send_count; i++)
		{
			int result = usb_device_connection.controlTransfer(0x21, 0x9, 0x200, 0, output_buffer_array, buffer_length, 0);
			if(result != -1)
			{
				success = true;
				break;
			}
		}
		if(!success)
		{
			send_message_to_handler("Ошибка - чтение Rfid");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);	
		}
		else
		{
			//Изменить состояние ожидания
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
			
			//Указать - что пульт ожидает метку
			change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.TRUE);
			
			//send_message_to_handler("Конец - чтение Rfid");
		}
	}
	
	//Послать команду моргания светодиода
	//remote_receiver_id - номер комплекта
	//byte_remote_id - номер пульта
	//diode_color - цвет диода (0 - зеленый, другое значение - красный)
	public void send_led_flash_command(short remote_receiver_id, byte byte_remote_id, int diode_color)
	{
		//send_message_to_handler("Начало - послать команду led");
		
		//Если устройство не подключено полностью - ничего не делать
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//Если устройство не ожидает ответа - ничего не делать
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;

		//Длина буфера
		int buffer_length = 8;
		
		//Создание буфера
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//Перевод формата представления битов в little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//По умолчанию данные
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//Длина пакета
		char package_length = 6;
		output_buffer.put((byte) package_length);
		
		//Номер комплекта
		output_buffer.putShort(remote_receiver_id);
		
		//Номер пульта
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//Команда моргания светодиодов
		char CMD_LED_SCN = 12;
		output_buffer.put((byte) CMD_LED_SCN);
		
		//Цвет моргания
		byte flash_color = 0x0f;	//Красный
		if(diode_color == 0)
			flash_color = 0x1f;		//Зеленый
		output_buffer.put(flash_color);
		
		//Перевод в массив байтов
		byte [] output_buffer_array = output_buffer.array();
		
		//Отправка данных
		boolean success = false;
		for(int i = 0; i < send_count; i++)
		{
			int result = usb_device_connection.controlTransfer(0x21, 0x9, 0x200, 0, output_buffer_array, buffer_length, 0);
			if(result != -1)
			{
				success = true;
				
				break;
			}
		}
		if(!success)
		{
			send_message_to_handler("Ошибка - послать команду led");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);
		}
		else
		{
			//Изменить состояние ожидания
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
			//send_message_to_handler("Конец - послать команду led");
		}
	}
	
	//Послать команду отображения текстовой строки на экране
	//remote_receiver_id - номер комплекта
	//byte_remote_id - номер пульта
	//text - текст для отображения
	public void send_display_string_command(short remote_receiver_id, byte byte_remote_id, String text)
	{
		//send_message_to_handler("Начало - послать команду текста");
		
		//Если устройство не подключено полностью - ничего не делать
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//Если устройство не ожидает ответа - ничего не делать
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;
				
		//Длина буфера
		int buffer_length = 9 + text.length();
		
		//Создание буфера
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//Перевод формата представления битов в little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//По умолчанию данные
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//Длина пакета
		char package_length = (char) (buffer_length - 2);
		output_buffer.put((byte) package_length);
		
		//Номер комплекта
		output_buffer.putShort(remote_receiver_id);
		
		//Номер пульта
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//Команда отображения строки на экране
		char CMD_DISPLAY_STRING_CLEAR = 11;
		output_buffer.put((byte) CMD_DISPLAY_STRING_CLEAR);
		
		//Байт координат
		byte string_coord = 0x08;
		output_buffer.put(string_coord);
	
		//Текст
		byte [] text_bytes = get_windows_encoding_bytes(text);
		for(int i = 0; i < text.length() + 1; i++)
			output_buffer.put(text_bytes[i]);
		
		//Перевод в массив байтов
		byte [] output_buffer_array = output_buffer.array();
		
		//Отправка данных
		boolean success = false;
		for(int i = 0; i < send_count; i++)
		{
			int result = usb_device_connection.controlTransfer(0x21, 0x9, 0x200, 0, output_buffer_array, buffer_length, 0);
			if(result != -1)
			{
				success = true;
				
				break;
			}
		}
		if(!success)
		{
			send_message_to_handler("Ошибка - послать команду текста");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);
		}
		else
		{
			//send_message_to_handler("Конец - послать команду текста");
			//Изменить состояние ожидания
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
		}
	}
	
	/*****	Потоки, задачи для потоков и т.п.	*****/
	
	//Задача для потока работы с USB
	private Runnable usb_thread_runnable = new Runnable()
	{
		//Переменные для отслеживания информации по которой определяется новый пакет или старый
		byte old_insert_index = -1;
		byte old_extract_index = -1;
		
		//Функция потока
		public void run()
		{
			//Бесконечный цикл для работы с USB устройствами
			while(status)
			{
				/*
				Поиск подключенного устройства
				- Если устройство было подключено и на этом этапе оно не будет найдено - будет оповещение об отключении устройства
				*/
				
				//Получение списка устройств
				HashMap<String, UsbDevice> device_list = usb_manager.getDeviceList();
				if(device_list.size() == 0)		//Если нет подключенных устройств
				{
					//Если до этого небыло подключенного устройства - оповестить пользователя об отключении
					if(!state.equals(EDeviceState.NO_DEVICE))
					{
						//Если устройство было подключено полностью
						if(state.equals(EDeviceState.HAVE_DEVICE))
						{
							//Освобождение интерфейса
							usb_device_connection.releaseInterface(usb_interface);
							
							//Закрытие подключения с USB
							usb_device_connection.close();
						}
						send_message_to_handler(R.string.toast_message_usb_device_off);
					}
					state = EDeviceState.NO_DEVICE;
					continue;
				}
				
				//Получение устройства
				Iterator<UsbDevice> device_iterator = device_list.values().iterator();
				if(device_iterator.hasNext())	//Если найдено устройство в списке устройств
				{
					device = device_iterator.next();
					
					//Если не удалось получить устройство
					if(device == null)
					{
						//Если до этого небыло подключенного устройства - оповестить пользователя об отключении
						if(!state.equals(EDeviceState.NO_DEVICE))
						{
							//Если устройство было подключено полностью
							if(state.equals(EDeviceState.HAVE_DEVICE))
							{
								//Освобождение интерфейса
								usb_device_connection.releaseInterface(usb_interface);
								
								//Закрытие подключения с USB
								usb_device_connection.close();
							}
							send_message_to_handler(R.string.toast_message_usb_device_off);
						}
						state = EDeviceState.NO_DEVICE;
						continue;
					}
				}
				else	//Если не удалось найти устройство в списке устройств
				{
					//Если до этого небыло подключенного устройства - оповестить пользователя об отключении
					if(!state.equals(EDeviceState.NO_DEVICE))
					{
						//Если устройство было подключено полностью
						if(state.equals(EDeviceState.HAVE_DEVICE))
						{
							//Освобождение интерфейса
							usb_device_connection.releaseInterface(usb_interface);
							
							//Закрытие подключения с USB
							usb_device_connection.close();
						}
						send_message_to_handler(R.string.toast_message_usb_device_off);
					}
					state = EDeviceState.NO_DEVICE;
					continue;
				}
				
				/*
				На данном этапе устройство найдено
				- Дальше идет конечный автомат, который определяет действия в зависимости от текущего состояния
				*/
				
				//Начало конечного автомата
				if(state.equals(EDeviceState.DEVICE_ERROR))		//Ошибочное состояние
				{
					//Из ошибочного состояния можно выйти только путем переподключения USB устройства
				}
				else if(state.equals(EDeviceState.NO_DEVICE))	//Если устройство небыло подключено
				{
					//Перейти в состояние - устройство подключено, но не имеет доступа
					state = EDeviceState.HAVE_DEVICE_WITHOUT_PERMISSION;
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITHOUT_PERMISSION))	//Если устройство не имеет доступа
				{
					//Проверка на доступ (действительно ли так)
					boolean permission_flag = usb_manager.hasPermission(device);
					
					//Если доступ все же есть
					if(permission_flag)
					{
						//Перейти в состояние - устройство подключено и имеется доступ
						state = EDeviceState.HAVE_DEVICE_WITH_PERMISSION;
					}
					else	//Если доступа действительно нет
					{
						//Запрос доступа и переход в состояние ожидания получения доступа
						usb_manager.requestPermission(device,  permission_intent);
						state = EDeviceState.HAVE_DEVICE_WITH_REQUEST_PERMISSION;
					}
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITH_REQUEST_PERMISSION))	//Если устройство ожидает получения доступа
				{
					//Проверка на доступ
					boolean permission_flag = usb_manager.hasPermission(device);
					
					//Если доступ получен
					if(permission_flag)
					{
						//Перейти в состояние - устройство подключено и имеется доступ
						state = EDeviceState.HAVE_DEVICE_WITH_PERMISSION;
					}
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITH_PERMISSION))		//Если есть устройство с полученным доступом
				{
					//Если устройство не имеет интерфейсов
					if(device.getInterfaceCount() < 1)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_interfaces_not_found);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//Получить интерфейс
					usb_interface = device.getInterface(0);
					
					//Если устройство имеет некоректные интерфейсы
					if(device.getInterfaceCount() < 1)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_interfaces_incorrect);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//Если устройтво не имеет источников/приемников данных
					if(usb_interface.getEndpointCount() < 1)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_endpoint_not_found);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//Получить источник/приемник данных
					usb_endpoint = usb_interface.getEndpoint(0);
					
					//Если устройтво имеет некоректные источники/приемники данных
					if(usb_endpoint == null)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_endpoint_incorrect);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//Открыть устройство для взаимодействия с ним
					usb_device_connection = usb_manager.openDevice(device);
					
					//Если устройство не удалось открыть
					if(usb_device_connection == null)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_open_device);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//Закрепить интерфейс
					boolean result = usb_device_connection.claimInterface(usb_interface, true);
					
					//Если не удалось закрепить интерфейс
					if(!result)
					{
						//Оповестить пользователя об этом и перейти в ошибочное состояние
						send_message_to_handler(R.string.error_usb_claim_interface);
						state = EDeviceState.DEVICE_ERROR;
						
						//Закрыть подключение к устройству
						usb_device_connection.close();
						
						continue;
					}
					
					//Успешное подключение к устройству и готовность к обмену данными
					send_message_to_handler(R.string.toast_message_usb_device_on);
					state = EDeviceState.HAVE_DEVICE;
				}
				else if(state == EDeviceState.HAVE_DEVICE)	//Если устройство подключено
				{
					
					/*****	Получение пакета	*****/
					
					//Место для записи пакета
					ByteBuffer buffer = ByteBuffer.allocate(64);
					
					//Проинициализировать запрос пакета
					UsbRequest usb_request = new UsbRequest();
					usb_request.initialize(usb_device_connection, usb_endpoint);
					
					//Попытка получить пакет
					boolean result = usb_request.queue(buffer, 64);
					
					//Если пакет присутствует
					if(result)
					{	
						//Ожидание окончания выполнения запроса
						usb_request = usb_device_connection.requestWait();
						
						//send_rf_ack_command((short)0, (byte)0);
						//int val = 1;
						//if(val == 1)
						//	continue;
						
						//Перейти к little endian формату битов
						buffer.order(ByteOrder.LITTLE_ENDIAN);
						
						//Обнулить указатель считывания битов
						buffer.rewind();
						
						//send_message_to_handler("Начало получения данных об устройстве");
						
						//Считывать данные об устройстве и сообщении/пакете
						byte message_id = buffer.get();						//ИД сообщения
						if(message_id != 1)
						{
							continue;
						}
						short reciever_id = buffer.getShort();				//ИД ресивера
						byte buffer_insert_index = buffer.get();			//Данные, необходимые для различия пакетов
						byte buffer_extract_index = buffer.get();			//Данные, необходимые для различия пакетов
						byte RSSI = buffer.get();	
						short received_packet_count = buffer.getShort();	//...
						byte packet_length = buffer.get();					//...
						short remote_receiver_id = buffer.getShort();		//Номер комплекта
						//send_message_to_handler("remote_receiver_id = " + remote_receiver_id);
						byte byte_remote_id = buffer.get();					//Номер пульта в комплекте
						//send_message_to_handler("byte_remote_id = " + byte_remote_id);
						byte msg_index = buffer.get();						//Индекс сообщения
						byte data_id = buffer.get();						//...
						byte battery_level = buffer.get();					//...
						byte transmit_retry = buffer.get();					//...
						byte lang_id = buffer.get();						//...
						
						//Считывание данных, которые передает устройство
						int usefull_data_length = 12;	//Кол-во байт с информацией
						int uid_length = 8;				//Кол-во байт с информацией об идентификаторе считанной метки
						byte [] usefull_data_bytes = new byte[usefull_data_length];
						byte [] uid_bytes = new byte [uid_length];
						for(int i = 0; i < usefull_data_length; i++)
						{
							usefull_data_bytes[i] = buffer.get();
							if(i > 3)
								uid_bytes[i - 4] = usefull_data_bytes[i];
						}
							
						
						//Если старый пакет - ничего не делать
						if(buffer_insert_index == old_insert_index || buffer_extract_index == old_extract_index)
							continue;						
						
						//send_message_to_handler("Является новым пакетом");
						
						//Запомнить новые данные пакета
						old_insert_index = buffer_insert_index;
						old_extract_index = buffer_extract_index;
						
						//Если пакет не несет полезной информации - ничего не делать (6)
						if(message_id == 0)
							continue;
						
						//send_message_to_handler("Несет полезную информацию");
						
						//Указать, что пульт ожидает ответ
						change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.TRUE);
						
						//Предварительная обработка данных
						String hex_data = bytes_to_hex(usefull_data_bytes);
						int key_code = Integer.parseInt(hex_data.substring(0, 2), 16);
						EDeviceKey key = get_device_key(key_code);
						String uid_data = bytes_to_hex(uid_bytes);
						
						//Если была нажата кнопка на устройстве
						if(!key.equals(EDeviceKey.INCORRECT_BUTTON))
						{
							//send_message_to_handler("Была нажата кнопка " + key.toString());
							//Изменить информацию об ожидании метки от пульта
							change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
							
							//Составление данных, для сообщения Handler-у
							Message msg = new Message();
							msg.setTarget(HandlerControlClass.usb_package_handler);
							Bundle msg_data = new Bundle();
							
							//Передача данных
							msg_data.putSerializable("message_type", EDevicePackageType.BUTTON_PACKAGE);	//Тип сообщения
							msg_data.putShort("remote_receiver_id", remote_receiver_id);					//Номер комплекта
							msg_data.putByte("remote_id", byte_remote_id);									//Номер пульта в комплекте
							msg_data.putSerializable("key", key);											//Код кнопки
							msg.setData(msg_data);
							
							//Передача сообщения
							HandlerControlClass.usb_package_handler.sendMessage(msg);
						}
						else		//Если пришла метка
						{	
							//send_message_to_handler("Пришла метка rfid " + uid_data.toString());
							//Если пульт не ожидает метку
							if(get_rfid_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
							{
								//Выслать команду на завершение процедур на пульте
								send_rf_ack_command(remote_receiver_id, byte_remote_id);
								continue;
							}
							
							//Изменить информацию об ожидании метки от пульта
							change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
							
							//Если пришла ошибочная метка
							if(uid_data.equals("45-52-52-4F-52-00-00-00") || uid_data.equals("00-00-00-00-00-00-00-00"))
							{
								//Выслать команду на завершение процедур на пульте
								send_rf_ack_command(remote_receiver_id, byte_remote_id);
								continue;
							}
							
							//Составление данных, для сообщения Handler-у
							Message msg = new Message();
							msg.setTarget(HandlerControlClass.usb_package_handler);
							Bundle msg_data = new Bundle();
							
							//Передача данных
							msg_data.putSerializable("message_type", EDevicePackageType.RFID_PACKAGE);		//Тип сообщения
							msg_data.putShort("remote_receiver_id", remote_receiver_id);					//Номер комплекта
							msg_data.putByte("remote_id", byte_remote_id);									//Номер пульта в комплекте
							msg_data.putString("uid", uid_data);											//ИД считанной метки
							msg.setData(msg_data);
							
							//Передача сообщения
							HandlerControlClass.usb_package_handler.sendMessage(msg);
						}
	
						//Получить указатель на текущее приложение
						VPlayApplication app = VPlayApplication.get_instance();
						if(app == null)
						{
							send_message_to_handler(R.string.error_application_not_found);
							continue;
						}
						
						//Ответ на пакет
						if(!app.is_have_observer())	//Переслать сигнал завершения процедур если ответить на пакет никто не может
							send_rf_ack_command(remote_receiver_id, byte_remote_id);
						else	//Переслать сигнал ожидания если есть кому ответить
							send_wait_command(remote_receiver_id, byte_remote_id);
					}
				}
			}
			
			//Если устройство подключено
			if(state.equals(EDeviceState.HAVE_DEVICE))
			{
				//Освобождение интерфейса
				usb_device_connection.releaseInterface(usb_interface);
				
				//Закрытие подключения с USB
				usb_device_connection.close();
			}
		}
	};
}
