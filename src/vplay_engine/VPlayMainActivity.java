package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

//����� activity �������� ���� ���������� VPlay
public abstract class VPlayMainActivity extends ActionBarActivity
{
	//��� ������ �������� ����
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//����������� �������������
		super.onCreate(savedInstanceState);
		
		//������������� ����������
		VPlayApplication app = (VPlayApplication) this.getApplication();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
		app.init(app);
	}
		
	//��� ����������� �������� ���� ���������� VPlay (��� ������ �� ����������)
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		//��������������� ����������
		VPlayApplication app = (VPlayApplication) this.getApplication();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
		app.deinit();
	}
}
