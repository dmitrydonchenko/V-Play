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

//������ ��� ������ � USB
public class UsbService extends Service
{
	
	
	
	/*****	�������� �������	*****/
	
	//��������� �� ����
	private static UsbService instance = null;
	
	//��������� USB ����������
	private EDeviceState state = EDeviceState.NO_DEVICE;	//�� ��������� - ��� ������������� ����������
		
	//������ �������
	private Thread usb_thread;								//����� ��� ������ � USB �����������
	
	//�������������� ������
	private UsbManager usb_manager;							//��������� ������ ��� ������ � USB
	private PendingIntent permission_intent;				//...
	private UsbDevice device;								//������������ ����������
	private UsbDeviceConnection usb_device_connection;		//���������� � ����� � ����������� USB ��� ��������������
	private UsbInterface usb_interface;						//��������� USB ����������
	private UsbEndpoint usb_endpoint;						//��������/�������� ������ USB
	private boolean status = false;							//��������� ������� (��� ��� ����)
	
	private int send_count = 5;								//���-�� ������� �������� ����� ������
	
	//������ ��� ������ � �����������
	private static final String WINDOWS_1251_RUS = "������������������������ ������� �����������������������������������������������������������������������������������������������";
	
	//������ ��� ������ � USB
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";		//...
	
	
	//��������� ��� �������� ������ �������� ����� rfid
	HashMap<Short, HashMap<Byte, Boolean>> rfid_query_flags = new HashMap<Short, HashMap<Byte, Boolean>>();
	
	//��������� ��� �������� ������ �������� ������ �� ������
	HashMap<Short, HashMap<Byte, Boolean>> answer_query_flags = new HashMap<Short, HashMap<Byte, Boolean>>();

	
	/*****	����������� ������ �������	*****/
	
	//��� �������� �������
	@Override
	public void onCreate()
	{
		//�������� ��������� �� ����
		instance = this;
		
		//������������� ������� �������
		status = true;
		
		//������������������� USB
		usb_manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		permission_intent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		
		//������������������� ������ � ���������
		init_threads();
		
		super.onCreate();
	}
	
	//��� ������ �������
	public int onStartCommand(Intent intent, int flags, int startId)
	{	
		send_message_to_handler(R.string.toast_message_usb_service_started);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	//�������� ��� ����������� �������
	public void onDestroy()
	{
		//�������� ��������� �� ����
		instance = null;
		
		//��������� ������� � ������� ����
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
	
	
	
	/***** ��������������� ������ �������	*****/
	
	//�������� ��������� �� ����
	public static UsbService get_instance()
	{
		return instance;
	}
	
	//������������� � ������ �������
	private void init_threads()
	{
		//������������� �������
		usb_thread = new Thread(null, usb_thread_runnable);
		
		//������ �������
		usb_thread.start();
	}
	
	//�������� ��������� handler-�
	private void send_message_to_handler(String debug_message)
	{
		//����������� ������, ��� ��������� Handler-�
		Message msg = new Message();
		msg.setTarget(HandlerControlClass.pop_up_message_handler);
		Bundle msg_data = new Bundle();
		
		//�������� ���������
		DebugClass.print(debug_message);
		msg_data.putString("message_string", debug_message);
		msg.setData(msg_data);
		HandlerControlClass.pop_up_message_handler.sendMessage(msg);
	}
	
	//�������� ��������� handler-� ����� ID ������� ������
	private void send_message_to_handler(int debug_message_id)
	{
		//�������� ������ � ��������
		Resources resources = VPlayApplication.get_resources();
		
		//�������� ������ �� �������� �� ID
		String debug_message = resources.getString(debug_message_id);
		
		//����������� ������, ��� ��������� Handler-�
		Message msg = new Message();
		msg.setTarget(HandlerControlClass.pop_up_message_handler);
		Bundle msg_data = new Bundle();
		
		//�������� ���������
		DebugClass.print(debug_message);
		msg_data.putString("message_string", debug_message);
		msg.setData(msg_data);
		HandlerControlClass.pop_up_message_handler.sendMessage(msg);
	}
	
	//������� ������ � ��������� UTF-8 � ������ ���� � ��������� Windows-1251
	//text - ����� ��� ��������
	//return - ������ ���� (����� �������� �� ������� ������, ��� �����)
	private static byte [] get_windows_encoding_bytes(String text)
	{
		//�������������� ������ ��� ����������� ���������
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
		
		
		//������ ���� ��� ������
		byte [] bytes = new byte[text.length() + 1];
		
		
		//������� ������
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
	
	//������� ������� ���� � ������ hex
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
	
	//�������� ������ ���������� �� ����
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
	
	
	//�������� ��������� �������� ����� ��� ������
	private void change_rfid_query_flags_state(short remote_receiver_id, byte byte_remote_id, Boolean state)
	{
		//���� ���������� �� �������� ������ ��� ����������� ������ �� ������� - �������
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
	
	//������� ��������� �������� ����� ��� ������
	private Boolean get_rfid_query_flags_state(short remote_receiver_id, byte byte_remote_id)
	{
		//���� ���������� �� �������� ������ ��� ����������� ������ �� ������� - �������
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
	
	//�������� ��������� �������� ������ �� ������ ������
	private void change_answer_query_flags_state(short remote_receiver_id, byte byte_remote_id, Boolean state)
	{
		//���� ���������� �� �������� ������ ��� ����������� ������ �� ������� - �������
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
	
	//������� ��������� �������� ������ �� ������ ������
	private Boolean get_answer_query_flags_state(short remote_receiver_id, byte byte_remote_id)
	{
		//���� ���������� �� �������� ������ ��� ����������� ������ �� ������� - �������
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
	
	
	/*****	�������������� ��������� ������, ������������, ������ � ������	*****/
	
	//��������� ������ USB ���������
	private enum EDeviceState
	{
		DEVICE_ERROR,							//��������� ������
		NO_DEVICE,								//���������� �� �������
		HAVE_DEVICE_WITHOUT_PERMISSION,			//���������� �������, �� ��� ������� � ����
		HAVE_DEVICE_WITH_REQUEST_PERMISSION,	//���������� �������, ������ ������ �� ������ � ����������
		HAVE_DEVICE_WITH_PERMISSION,			//���� ���������� � ��������
		HAVE_DEVICE								//���� ���������� � ��� ������ ��� ��������������
	};
	
	//���� ������� � ���������� USB
	public enum EDevicePackageType
	{
		BUTTON_PACKAGE,		//������� �� ������ USB ����������
		RFID_PACKAGE		//���������� ����� RFID
	};
	
	//������ ����������
	public enum EDeviceKey
	{
		INCORRECT_BUTTON,	//��������� ������
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
	
	
	
	/*****	������ ��� �������������� � �����������	*****/
	
	//������� ������� ���������� ���� �������� �� ������
	//remote_receiver_id - ����� ���������
	//byte_remote_id - ����� ������
	public void send_rf_ack_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("������ - rfack");
		
		//���� ���������� �� ���������� ��������� - ������ �� ������
		//if(!state.equals(EDeviceState.HAVE_DEVICE))
		//	return;
		
		//���� ���������� �� ������� ������ - ������ �� ������
		//if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
		//	return;
		
		//�������� ��������� ��������
		change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
				
		//����� ������
		int buffer_length = 6;
		
		//�������� ������
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//������� ������� ������������� ����� � little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//�� ��������� ������
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//����� ������
		char package_length = 4;
		output_buffer.put((byte) package_length);
		
		//����� ���������
		output_buffer.putShort(remote_receiver_id);
		
		//����� ������
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//������� � ������ ������
		byte [] output_buffer_array = output_buffer.array();
		
		//�������� ������
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
			send_message_to_handler("������ - rfack");
			send_message_to_handler(R.string.error_usb_transfer_data);
		}
		//send_message_to_handler("����� - rfack");
	}
	
	//������� ������� ��������
	//remote_receiver_id - ����� ���������
	//byte_remote_id - ����� ������
	private void send_wait_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("������ - ������� ������� ��������");
		
		//���� ���������� �� ���������� ��������� - ������ �� ������
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
				
		//����� ������
		int buffer_length = 7;
		
		//�������� ������
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//������� ������� ������������� ����� � little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//�� ��������� ������
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//����� ������
		char package_length = 5;
		output_buffer.put((byte) package_length);
		
		//����� ���������
		output_buffer.putShort(remote_receiver_id);
		
		//����� ������
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//������� ������ �����
		char CMD_WAIT = 18;
		output_buffer.put((byte) CMD_WAIT);
		
		//������� � ������ ������
		byte [] output_buffer_array = output_buffer.array();
		
		//�������� ������
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
			send_message_to_handler("������ - ������� ������� ��������");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);	
		}
		//send_message_to_handler("����� - ������� ������� ��������");
	}
	
	//������� ������� ������ RF_ID �����
	//remote_receiver_id - ����� ���������
	//byte_remote_id - ����� ������
	public void send_read_rf_id_command(short remote_receiver_id, byte byte_remote_id)
	{
		//send_message_to_handler("������ - ������ Rfid");
		
		//���� ���������� �� ���������� ��������� - ������ �� ������
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//���� ���������� �� ������� ������ - ������ �� ������
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;
		
		//����� ������
		int buffer_length = 7;
		
		//�������� ������
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//������� ������� ������������� ����� � little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//�� ��������� ������
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//����� ������
		char package_length = 5;
		output_buffer.put((byte) package_length);
		
		//����� ���������
		output_buffer.putShort(remote_receiver_id);
		
		//����� ������
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//������� ������ �����
		char CMD_TAG_READ = 15;
		output_buffer.put((byte) CMD_TAG_READ);
		
		//������� � ������ ������
		byte [] output_buffer_array = output_buffer.array();
		
		//�������� ������
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
			send_message_to_handler("������ - ������ Rfid");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);	
		}
		else
		{
			//�������� ��������� ��������
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
			
			//������� - ��� ����� ������� �����
			change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.TRUE);
			
			//send_message_to_handler("����� - ������ Rfid");
		}
	}
	
	//������� ������� �������� ����������
	//remote_receiver_id - ����� ���������
	//byte_remote_id - ����� ������
	//diode_color - ���� ����� (0 - �������, ������ �������� - �������)
	public void send_led_flash_command(short remote_receiver_id, byte byte_remote_id, int diode_color)
	{
		//send_message_to_handler("������ - ������� ������� led");
		
		//���� ���������� �� ���������� ��������� - ������ �� ������
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//���� ���������� �� ������� ������ - ������ �� ������
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;

		//����� ������
		int buffer_length = 8;
		
		//�������� ������
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//������� ������� ������������� ����� � little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//�� ��������� ������
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//����� ������
		char package_length = 6;
		output_buffer.put((byte) package_length);
		
		//����� ���������
		output_buffer.putShort(remote_receiver_id);
		
		//����� ������
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//������� �������� �����������
		char CMD_LED_SCN = 12;
		output_buffer.put((byte) CMD_LED_SCN);
		
		//���� ��������
		byte flash_color = 0x0f;	//�������
		if(diode_color == 0)
			flash_color = 0x1f;		//�������
		output_buffer.put(flash_color);
		
		//������� � ������ ������
		byte [] output_buffer_array = output_buffer.array();
		
		//�������� ������
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
			send_message_to_handler("������ - ������� ������� led");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);
		}
		else
		{
			//�������� ��������� ��������
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
			//send_message_to_handler("����� - ������� ������� led");
		}
	}
	
	//������� ������� ����������� ��������� ������ �� ������
	//remote_receiver_id - ����� ���������
	//byte_remote_id - ����� ������
	//text - ����� ��� �����������
	public void send_display_string_command(short remote_receiver_id, byte byte_remote_id, String text)
	{
		//send_message_to_handler("������ - ������� ������� ������");
		
		//���� ���������� �� ���������� ��������� - ������ �� ������
		if(!state.equals(EDeviceState.HAVE_DEVICE))
			return;
		
		//���� ���������� �� ������� ������ - ������ �� ������
		if(get_answer_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
			return;
				
		//����� ������
		int buffer_length = 9 + text.length();
		
		//�������� ������
		ByteBuffer output_buffer = ByteBuffer.allocate(buffer_length);
		
		//������� ������� ������������� ����� � little endian
		output_buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		//�� ��������� ������
		char HID_REMOTE_SET_DATA = 2;
		output_buffer.put((byte) HID_REMOTE_SET_DATA);
		
		//����� ������
		char package_length = (char) (buffer_length - 2);
		output_buffer.put((byte) package_length);
		
		//����� ���������
		output_buffer.putShort(remote_receiver_id);
		
		//����� ������
		output_buffer.put(byte_remote_id);
		
		//RF ACK ...?
		char RF_ACK = 1;
		output_buffer.put((byte) RF_ACK);
		
		//������� ����������� ������ �� ������
		char CMD_DISPLAY_STRING_CLEAR = 11;
		output_buffer.put((byte) CMD_DISPLAY_STRING_CLEAR);
		
		//���� ���������
		byte string_coord = 0x08;
		output_buffer.put(string_coord);
	
		//�����
		byte [] text_bytes = get_windows_encoding_bytes(text);
		for(int i = 0; i < text.length() + 1; i++)
			output_buffer.put(text_bytes[i]);
		
		//������� � ������ ������
		byte [] output_buffer_array = output_buffer.array();
		
		//�������� ������
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
			send_message_to_handler("������ - ������� ������� ������");
			send_message_to_handler(R.string.error_usb_transfer_data);
			send_rf_ack_command(remote_receiver_id, byte_remote_id);
		}
		else
		{
			//send_message_to_handler("����� - ������� ������� ������");
			//�������� ��������� ��������
			change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
		}
	}
	
	/*****	������, ������ ��� ������� � �.�.	*****/
	
	//������ ��� ������ ������ � USB
	private Runnable usb_thread_runnable = new Runnable()
	{
		//���������� ��� ������������ ���������� �� ������� ������������ ����� ����� ��� ������
		byte old_insert_index = -1;
		byte old_extract_index = -1;
		
		//������� ������
		public void run()
		{
			//����������� ���� ��� ������ � USB ������������
			while(status)
			{
				/*
				����� ������������� ����������
				- ���� ���������� ���� ���������� � �� ���� ����� ��� �� ����� ������� - ����� ���������� �� ���������� ����������
				*/
				
				//��������� ������ ���������
				HashMap<String, UsbDevice> device_list = usb_manager.getDeviceList();
				if(device_list.size() == 0)		//���� ��� ������������ ���������
				{
					//���� �� ����� ������ ������������� ���������� - ���������� ������������ �� ����������
					if(!state.equals(EDeviceState.NO_DEVICE))
					{
						//���� ���������� ���� ���������� ���������
						if(state.equals(EDeviceState.HAVE_DEVICE))
						{
							//������������ ����������
							usb_device_connection.releaseInterface(usb_interface);
							
							//�������� ����������� � USB
							usb_device_connection.close();
						}
						send_message_to_handler(R.string.toast_message_usb_device_off);
					}
					state = EDeviceState.NO_DEVICE;
					continue;
				}
				
				//��������� ����������
				Iterator<UsbDevice> device_iterator = device_list.values().iterator();
				if(device_iterator.hasNext())	//���� ������� ���������� � ������ ���������
				{
					device = device_iterator.next();
					
					//���� �� ������� �������� ����������
					if(device == null)
					{
						//���� �� ����� ������ ������������� ���������� - ���������� ������������ �� ����������
						if(!state.equals(EDeviceState.NO_DEVICE))
						{
							//���� ���������� ���� ���������� ���������
							if(state.equals(EDeviceState.HAVE_DEVICE))
							{
								//������������ ����������
								usb_device_connection.releaseInterface(usb_interface);
								
								//�������� ����������� � USB
								usb_device_connection.close();
							}
							send_message_to_handler(R.string.toast_message_usb_device_off);
						}
						state = EDeviceState.NO_DEVICE;
						continue;
					}
				}
				else	//���� �� ������� ����� ���������� � ������ ���������
				{
					//���� �� ����� ������ ������������� ���������� - ���������� ������������ �� ����������
					if(!state.equals(EDeviceState.NO_DEVICE))
					{
						//���� ���������� ���� ���������� ���������
						if(state.equals(EDeviceState.HAVE_DEVICE))
						{
							//������������ ����������
							usb_device_connection.releaseInterface(usb_interface);
							
							//�������� ����������� � USB
							usb_device_connection.close();
						}
						send_message_to_handler(R.string.toast_message_usb_device_off);
					}
					state = EDeviceState.NO_DEVICE;
					continue;
				}
				
				/*
				�� ������ ����� ���������� �������
				- ������ ���� �������� �������, ������� ���������� �������� � ����������� �� �������� ���������
				*/
				
				//������ ��������� ��������
				if(state.equals(EDeviceState.DEVICE_ERROR))		//��������� ���������
				{
					//�� ���������� ��������� ����� ����� ������ ����� ��������������� USB ����������
				}
				else if(state.equals(EDeviceState.NO_DEVICE))	//���� ���������� ������ ����������
				{
					//������� � ��������� - ���������� ����������, �� �� ����� �������
					state = EDeviceState.HAVE_DEVICE_WITHOUT_PERMISSION;
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITHOUT_PERMISSION))	//���� ���������� �� ����� �������
				{
					//�������� �� ������ (������������� �� ���)
					boolean permission_flag = usb_manager.hasPermission(device);
					
					//���� ������ ��� �� ����
					if(permission_flag)
					{
						//������� � ��������� - ���������� ���������� � ������� ������
						state = EDeviceState.HAVE_DEVICE_WITH_PERMISSION;
					}
					else	//���� ������� ������������� ���
					{
						//������ ������� � ������� � ��������� �������� ��������� �������
						usb_manager.requestPermission(device,  permission_intent);
						state = EDeviceState.HAVE_DEVICE_WITH_REQUEST_PERMISSION;
					}
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITH_REQUEST_PERMISSION))	//���� ���������� ������� ��������� �������
				{
					//�������� �� ������
					boolean permission_flag = usb_manager.hasPermission(device);
					
					//���� ������ �������
					if(permission_flag)
					{
						//������� � ��������� - ���������� ���������� � ������� ������
						state = EDeviceState.HAVE_DEVICE_WITH_PERMISSION;
					}
				}
				else if(state.equals(EDeviceState.HAVE_DEVICE_WITH_PERMISSION))		//���� ���� ���������� � ���������� ��������
				{
					//���� ���������� �� ����� �����������
					if(device.getInterfaceCount() < 1)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_interfaces_not_found);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//�������� ���������
					usb_interface = device.getInterface(0);
					
					//���� ���������� ����� ����������� ����������
					if(device.getInterfaceCount() < 1)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_interfaces_incorrect);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//���� ��������� �� ����� ����������/���������� ������
					if(usb_interface.getEndpointCount() < 1)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_endpoint_not_found);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//�������� ��������/�������� ������
					usb_endpoint = usb_interface.getEndpoint(0);
					
					//���� ��������� ����� ����������� ���������/��������� ������
					if(usb_endpoint == null)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_endpoint_incorrect);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//������� ���������� ��� �������������� � ���
					usb_device_connection = usb_manager.openDevice(device);
					
					//���� ���������� �� ������� �������
					if(usb_device_connection == null)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_open_device);
						state = EDeviceState.DEVICE_ERROR;
						continue;
					}
					
					//��������� ���������
					boolean result = usb_device_connection.claimInterface(usb_interface, true);
					
					//���� �� ������� ��������� ���������
					if(!result)
					{
						//���������� ������������ �� ���� � ������� � ��������� ���������
						send_message_to_handler(R.string.error_usb_claim_interface);
						state = EDeviceState.DEVICE_ERROR;
						
						//������� ����������� � ����������
						usb_device_connection.close();
						
						continue;
					}
					
					//�������� ����������� � ���������� � ���������� � ������ �������
					send_message_to_handler(R.string.toast_message_usb_device_on);
					state = EDeviceState.HAVE_DEVICE;
				}
				else if(state == EDeviceState.HAVE_DEVICE)	//���� ���������� ����������
				{
					
					/*****	��������� ������	*****/
					
					//����� ��� ������ ������
					ByteBuffer buffer = ByteBuffer.allocate(64);
					
					//������������������� ������ ������
					UsbRequest usb_request = new UsbRequest();
					usb_request.initialize(usb_device_connection, usb_endpoint);
					
					//������� �������� �����
					boolean result = usb_request.queue(buffer, 64);
					
					//���� ����� ������������
					if(result)
					{	
						//�������� ��������� ���������� �������
						usb_request = usb_device_connection.requestWait();
						
						//send_rf_ack_command((short)0, (byte)0);
						//int val = 1;
						//if(val == 1)
						//	continue;
						
						//������� � little endian ������� �����
						buffer.order(ByteOrder.LITTLE_ENDIAN);
						
						//�������� ��������� ���������� �����
						buffer.rewind();
						
						//send_message_to_handler("������ ��������� ������ �� ����������");
						
						//��������� ������ �� ���������� � ���������/������
						byte message_id = buffer.get();						//�� ���������
						if(message_id != 1)
						{
							continue;
						}
						short reciever_id = buffer.getShort();				//�� ��������
						byte buffer_insert_index = buffer.get();			//������, ����������� ��� �������� �������
						byte buffer_extract_index = buffer.get();			//������, ����������� ��� �������� �������
						byte RSSI = buffer.get();	
						short received_packet_count = buffer.getShort();	//...
						byte packet_length = buffer.get();					//...
						short remote_receiver_id = buffer.getShort();		//����� ���������
						//send_message_to_handler("remote_receiver_id = " + remote_receiver_id);
						byte byte_remote_id = buffer.get();					//����� ������ � ���������
						//send_message_to_handler("byte_remote_id = " + byte_remote_id);
						byte msg_index = buffer.get();						//������ ���������
						byte data_id = buffer.get();						//...
						byte battery_level = buffer.get();					//...
						byte transmit_retry = buffer.get();					//...
						byte lang_id = buffer.get();						//...
						
						//���������� ������, ������� �������� ����������
						int usefull_data_length = 12;	//���-�� ���� � �����������
						int uid_length = 8;				//���-�� ���� � ����������� �� �������������� ��������� �����
						byte [] usefull_data_bytes = new byte[usefull_data_length];
						byte [] uid_bytes = new byte [uid_length];
						for(int i = 0; i < usefull_data_length; i++)
						{
							usefull_data_bytes[i] = buffer.get();
							if(i > 3)
								uid_bytes[i - 4] = usefull_data_bytes[i];
						}
							
						
						//���� ������ ����� - ������ �� ������
						if(buffer_insert_index == old_insert_index || buffer_extract_index == old_extract_index)
							continue;						
						
						//send_message_to_handler("�������� ����� �������");
						
						//��������� ����� ������ ������
						old_insert_index = buffer_insert_index;
						old_extract_index = buffer_extract_index;
						
						//���� ����� �� ����� �������� ���������� - ������ �� ������ (6)
						if(message_id == 0)
							continue;
						
						//send_message_to_handler("����� �������� ����������");
						
						//�������, ��� ����� ������� �����
						change_answer_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.TRUE);
						
						//��������������� ��������� ������
						String hex_data = bytes_to_hex(usefull_data_bytes);
						int key_code = Integer.parseInt(hex_data.substring(0, 2), 16);
						EDeviceKey key = get_device_key(key_code);
						String uid_data = bytes_to_hex(uid_bytes);
						
						//���� ���� ������ ������ �� ����������
						if(!key.equals(EDeviceKey.INCORRECT_BUTTON))
						{
							//send_message_to_handler("���� ������ ������ " + key.toString());
							//�������� ���������� �� �������� ����� �� ������
							change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
							
							//����������� ������, ��� ��������� Handler-�
							Message msg = new Message();
							msg.setTarget(HandlerControlClass.usb_package_handler);
							Bundle msg_data = new Bundle();
							
							//�������� ������
							msg_data.putSerializable("message_type", EDevicePackageType.BUTTON_PACKAGE);	//��� ���������
							msg_data.putShort("remote_receiver_id", remote_receiver_id);					//����� ���������
							msg_data.putByte("remote_id", byte_remote_id);									//����� ������ � ���������
							msg_data.putSerializable("key", key);											//��� ������
							msg.setData(msg_data);
							
							//�������� ���������
							HandlerControlClass.usb_package_handler.sendMessage(msg);
						}
						else		//���� ������ �����
						{	
							//send_message_to_handler("������ ����� rfid " + uid_data.toString());
							//���� ����� �� ������� �����
							if(get_rfid_query_flags_state(remote_receiver_id, byte_remote_id) == Boolean.FALSE)
							{
								//������� ������� �� ���������� �������� �� ������
								send_rf_ack_command(remote_receiver_id, byte_remote_id);
								continue;
							}
							
							//�������� ���������� �� �������� ����� �� ������
							change_rfid_query_flags_state(remote_receiver_id, byte_remote_id, Boolean.FALSE);
							
							//���� ������ ��������� �����
							if(uid_data.equals("45-52-52-4F-52-00-00-00") || uid_data.equals("00-00-00-00-00-00-00-00"))
							{
								//������� ������� �� ���������� �������� �� ������
								send_rf_ack_command(remote_receiver_id, byte_remote_id);
								continue;
							}
							
							//����������� ������, ��� ��������� Handler-�
							Message msg = new Message();
							msg.setTarget(HandlerControlClass.usb_package_handler);
							Bundle msg_data = new Bundle();
							
							//�������� ������
							msg_data.putSerializable("message_type", EDevicePackageType.RFID_PACKAGE);		//��� ���������
							msg_data.putShort("remote_receiver_id", remote_receiver_id);					//����� ���������
							msg_data.putByte("remote_id", byte_remote_id);									//����� ������ � ���������
							msg_data.putString("uid", uid_data);											//�� ��������� �����
							msg.setData(msg_data);
							
							//�������� ���������
							HandlerControlClass.usb_package_handler.sendMessage(msg);
						}
	
						//�������� ��������� �� ������� ����������
						VPlayApplication app = VPlayApplication.get_instance();
						if(app == null)
						{
							send_message_to_handler(R.string.error_application_not_found);
							continue;
						}
						
						//����� �� �����
						if(!app.is_have_observer())	//��������� ������ ���������� �������� ���� �������� �� ����� ����� �� �����
							send_rf_ack_command(remote_receiver_id, byte_remote_id);
						else	//��������� ������ �������� ���� ���� ���� ��������
							send_wait_command(remote_receiver_id, byte_remote_id);
					}
				}
			}
			
			//���� ���������� ����������
			if(state.equals(EDeviceState.HAVE_DEVICE))
			{
				//������������ ����������
				usb_device_connection.releaseInterface(usb_interface);
				
				//�������� ����������� � USB
				usb_device_connection.close();
			}
		}
	};
}
