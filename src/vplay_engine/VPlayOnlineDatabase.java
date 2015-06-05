package vplay_engine;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.os.Environment;
import android.os.StrictMode;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

//Модуль для работы с онлайн базой данных
public class VPlayOnlineDatabase
{
	//Получить базу данных в виде строки - без получения доступа к аккаунту дропбокса
	public static String get_database_string()
	{
		//Инициализация работы онлайн
		init_online();
				
		//Загрузить файл
		if(!download_file_from_url_to_external(DATABASE_URL))
			return "";
		
		//Получить строку из файла
		String res = get_data_string_from_external_file();
		
		return res;
	}
	
	
	
	//Получить базу данных в виде строки
	public static String get_database_string_with_access()
	{
		//Инициализация работы онлайн
		init_online();
				
		//Залогиниться в дропбокс
		login_dropbox();
		
		//Проверить кол-во файлов
		int file_count = get_file_count();
		if(file_count == -1)	//Если не удалось получить информацию
			return "";
		
		//Если файлов нет
		if(file_count == 0)
			return "";
		
		//Загрузить файл
		if(!download_file_to_external(MAIN_DATABASE_FILENAME))
			return "";
		
		//Получить строку из файла
		String res = get_data_string_from_external_file();
		
		//Покинуть дропбокс
		leave_dropbox();
		
		return res;
	}
	
	//Обновить базу данных
	public static boolean update_database(String database_string)
	{
		//Инициализация работы онлайн
		init_online();
		
		//Залогиниться в дропбокс
		login_dropbox();
		
		//Проверить кол-во файлов
		int file_count = get_file_count();
		if(file_count == -1)	//Если не удалось получить информацию
			return false;
		
		//Если есть хотябы 1 файл
		if(file_count != 0)
		{
			//Загрузить файл
			if(!download_file_to_external(MAIN_DATABASE_FILENAME))
				return false;
			
			//Заменить на дропбоксе резервный файл (или создать новый - если нет)
			if(!upload_file_to_dropbox(RESERVE_DATABASE_FILENAME))
				return false;
		}
		
		//Создать файл с данными базы данных
		if(!create_external_file(database_string))
			return false;
		
		//Заменить на дропбоксе основной файл (или создать новый - если нет)
		if(!upload_file_to_dropbox(MAIN_DATABASE_FILENAME))
			return false;
		
		//Покинуть дропбокс
		leave_dropbox();
		
		return true;
	}
	
	
	//Данные для доступа к хранилищу
	final static private String APP_KEY = "28whwgkuszvp96j";
	final static private String APP_SECRET = "8c4v3f1t8q5jcuj";
	final static private String ACCESS_TOKEN = "zbZDYCeFSnUAAAAAAAAABK--RzVfm5H2ZWiCPf504ZOBgQIkOgM4hK3CMeDWmZX3";

	//Ссылка доступа к главному файлу базы данных на дропбоксе
	final static private String DATABASE_URL = "https://www.dropbox.com/s/opw3i0qvigw81kw/main.txt?dl=1";
	
	//Наименование файла в временном хранилище
	final static private String EXTERNAL_FILENAME = Environment.getExternalStorageDirectory() + File.separator + "VPLAY_EXTERNAL_FILE.txt";
	
	//Наименование файлов в дропбоксе
	final static private String MAIN_DATABASE_FILENAME = "/main.txt";
	final static private String RESERVE_DATABASE_FILENAME = "/reserve.txt";
	
	//Сессия дропбокса
	private static DropboxAPI<AndroidAuthSession> mDBApi = null;
	
	//Проинициализировать работу онлайн
	private static void init_online()
	{
		//...? (получить разрешение на межсетевое взаимодействие?)
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
	}
	
	//Подключиться к дропбоксу
	private static void login_dropbox()
	{
        //Создание сессии
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        
        //Авторизация
        mDBApi.getSession().setOAuth2AccessToken(ACCESS_TOKEN);
	}
	
	//Разлогиниться
	private static void leave_dropbox()
	{
		mDBApi.getSession().unlink();
		mDBApi = null;
		File file = new File(EXTERNAL_FILENAME);
		file.delete();
	}
	
	//Определить кол-во файлов в хранилище
	private static int get_file_count()
	{
		//Получить данные из корневой папки дропбокса
		Entry dirent = null;
		try
    	{
			dirent = mDBApi.metadata("/", 10, null, true, null);
		}
    	catch (DropboxException e)
    	{
			return -1;
		}
		
		//Получить список файлов
    	ArrayList<Entry> files = new ArrayList<Entry>();
    	for(Entry ent : dirent.contents)
    		files.add(ent);
    	
    	return files.size();
	}
	
	//Создать новый файл в хранилище с заданными данными
	private static boolean create_external_file(String database_data)
	{
		//Создать файл в хранилище
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//Если файл не создался - выйти
    	if(!file.exists())
			return false;
    	
    	//Создать поток данных
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//Получить данные в виде байтов
    	byte [] data = null;
    	try
    	{
			data = database_data.getBytes("UTF-8");
		}
    	catch (UnsupportedEncodingException e)
    	{
    		try
    		{
				output_stream.close();
			}
    		catch (IOException e1)
    		{
				return false;
			}
			return false;
		}
    	
    	//Заполнить файл данными
    	try
    	{
			output_stream.write(data);
		}
    	catch (IOException e)
    	{
    		try
    		{
				output_stream.close();
			}
    		catch (IOException e1)
    		{
				return false;
			}
			return false;
		}
    	try
    	{
			output_stream.close();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	return true;
	}
	
	//Создать новый файл в хранилище с заданными данными
	private static String get_data_string_from_external_file()
	{
		//Создать файл в хранилище
    	File file = new File(EXTERNAL_FILENAME);
    	
    	//Если файла нет - выйти
    	if(!file.exists())
			return "";
    	
    	//Создать поток данных
    	FileInputStream input_stream = null;
    	try
    	{
    		input_stream = new FileInputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return "";
		}
    	
    	//...
    	byte [] data = new byte [(int) file.length()];
    	
    	//Заполнить файл данными
    	try
    	{
			input_stream.read(data);
		}
    	catch (IOException e)
    	{
    		try
    		{
				input_stream.close();
			}
    		catch (IOException e1)
    		{
				return "";
			}
			return "";
		}
    	try
    	{
			input_stream.close();
		}
    	catch (IOException e)
    	{
			return "";
		}
    	
    	//Получить строку из байт
    	String res = "";
		try
		{
			res = new String(data, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return "";
		}
    	
    	return res;
	}
	
	//Загрузить файл в хранилище с url
	private static boolean download_file_from_url_to_external(String string_url)
	{
		//Создать файл в хранилище
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//Если файл не создался - выйти
    	if(!file.exists())
			return false;
    	
    	//Создать ссылку на файл
    	URL url = null;
    	try
    	{
			url = new URL(string_url);
		}
    	catch (MalformedURLException e)
		{
			return false;
		}
    	
    	//Поток для чтения с url
    	InputStream is;
		try
		{
			is = url.openStream();
		}
		catch (IOException e)
		{
			return false;
		}
    	
    	//Создать поток входной
    	DataInputStream dis = new DataInputStream(is);
    	
    	//Создать поток данных для записи в файл
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//Необходимые средства для записи в файл
    	byte[] buffer = new byte[1024];
        int length;
    
        //Скачивание файла
        try
        {
        	while((length = dis.read(buffer)) > 0)
        		output_stream.write(buffer, 0, length);
        }
        catch (IOException e1)
        {
        	return false;
        }
    	
    	//Закрыть поток
    	try
    	{
			output_stream.close();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	return true;
	}
	
	//Загрузить файл в хранилище
	private static boolean download_file_to_external(String filename)
	{
		//Создать файл в хранилище
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//Если файл не создался - выйти
    	if(!file.exists())
			return false;
    	
    	//Создать поток данных
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//Получить файл (загрузить)
    	try
    	{
    		mDBApi.getFile(filename, null, output_stream, null);
    	}
    	catch (DropboxException e)
    	{
			return false;
		}
    	
    	//Закрыть поток
    	try
    	{
			output_stream.close();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	return true;
	}
	
	//Загрузить файл на дропбокс (с заменой если такой файл уже существует)
	private static boolean upload_file_to_dropbox(String filename)
	{
		//Попытка октрыть файл
		File file = new File(EXTERNAL_FILENAME);
		
		//Если файла не существует - ничего не делать
		if(!file.exists())
			return false;
		
		//Создать поток для считывания из файла
    	FileInputStream input_stream;
		try
		{
			input_stream = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			return false;
		}
    	
    	//Отправить файл на дропбокс
    	try
    	{
			mDBApi.putFileOverwrite(filename, input_stream, file.length(), null);
		}
    	catch (DropboxException e)
    	{
			return false;
		}
    	
    	//Закрыть поток
    	try
    	{
			input_stream.close();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	return true;
	}
}