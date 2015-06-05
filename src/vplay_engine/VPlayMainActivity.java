package vplay_engine;

import com.vplay.R;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

//Класс activity главного окна приложения VPlay
public abstract class VPlayMainActivity extends ActionBarActivity
{
	//При старте главного окна
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		//Стандартная инициализация
		super.onCreate(savedInstanceState);
		
		//Инициализация приложения
		VPlayApplication app = (VPlayApplication) this.getApplication();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
		app.init(app);
	}
		
	//При уничтожении главного окна приложения VPlay (при выходе из приложения)
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		//Деинициализация приложения
		VPlayApplication app = (VPlayApplication) this.getApplication();
		if(app == null)
		{
			//DebugClass.message(R.string.error_application_not_found);
			return;
		}
		app.deinit();
	}
}
