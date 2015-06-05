package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

//����� activity ���� ���������� VPlay
public abstract class VPlayActivity extends ActionBarActivity
{
	//��� �������� ����
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//����������� ������������� ����
        super.onCreate(savedInstanceState);
        
        //���������������� ���� �� ��������� ��������� �� USB ����������
        VPlayApplication app = VPlayApplication.get_instance();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
        app.activity_registration(this);
	}
	
	//��� ����������� ����
	@Override
	protected void onDestroy()
	{
		//����������� ����������� ����
		super.onDestroy();
				
		//���������� �� ��������� ���������
		VPlayApplication app = VPlayApplication.get_instance();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
        app.activity_unregistration(this);
	}
	
	//���������� ��� ��������� ������ ������
	protected final void update(Bundle data)
	{
		//��������� ����������� ������ � ������
		UsbService.EDevicePackageType package_type = (UsbService.EDevicePackageType) data.getSerializable("message_type");	//��� ������
		short remote_receiver_id = data.getShort("remote_receiver_id");		//����� ���������
		byte remote_id = data.getByte("remote_id");							//����� ������ � ���������
		
		//���� ����� � ����������� � ������� ������
		if(package_type.equals(UsbService.EDevicePackageType.BUTTON_PACKAGE))
		{
			//�������� ������ � ������
			UsbService.EDeviceKey key = (UsbService.EDeviceKey) data.getSerializable("key");
			
			//�������� ����������
			update_key(remote_receiver_id, remote_id, key);
		}
		else if(package_type.equals(UsbService.EDevicePackageType.RFID_PACKAGE))
		{
			//�������� ������ � ��������� �����
			String uid = data.getString("uid");

			//�������� ����������
			update_rfid(remote_receiver_id, remote_id, uid);
		}
		
		//������� ������� � ���������� ���� �������� �� ������
		UsbService usb_service = UsbService.get_instance();
		usb_service.send_rf_ack_command(remote_receiver_id, remote_id);
	}
	
	//���������� ��� ��������� ������ ������ ��� ������� ������ �� ����������
	public abstract void update_key(short build_number, byte device_number, UsbService.EDeviceKey key);
	
	//���������� ��� ��������� ������ ������ ��� ���������� RFID ����� �� ����������
	public abstract void update_rfid(short build_number, byte device_number, String rfid);
}
