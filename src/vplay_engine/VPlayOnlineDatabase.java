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

//������ ��� ������ � ������ ����� ������
public class VPlayOnlineDatabase
{
	//�������� ���� ������ � ���� ������ - ��� ��������� ������� � �������� ���������
	public static String get_database_string()
	{
		//������������� ������ ������
		init_online();
				
		//��������� ����
		if(!download_file_from_url_to_external(DATABASE_URL))
			return "";
		
		//�������� ������ �� �����
		String res = get_data_string_from_external_file();
		
		return res;
	}
	
	
	
	//�������� ���� ������ � ���� ������
	public static String get_database_string_with_access()
	{
		//������������� ������ ������
		init_online();
				
		//������������ � ��������
		login_dropbox();
		
		//��������� ���-�� ������
		int file_count = get_file_count();
		if(file_count == -1)	//���� �� ������� �������� ����������
			return "";
		
		//���� ������ ���
		if(file_count == 0)
			return "";
		
		//��������� ����
		if(!download_file_to_external(MAIN_DATABASE_FILENAME))
			return "";
		
		//�������� ������ �� �����
		String res = get_data_string_from_external_file();
		
		//�������� ��������
		leave_dropbox();
		
		return res;
	}
	
	//�������� ���� ������
	public static boolean update_database(String database_string)
	{
		//������������� ������ ������
		init_online();
		
		//������������ � ��������
		login_dropbox();
		
		//��������� ���-�� ������
		int file_count = get_file_count();
		if(file_count == -1)	//���� �� ������� �������� ����������
			return false;
		
		//���� ���� ������ 1 ����
		if(file_count != 0)
		{
			//��������� ����
			if(!download_file_to_external(MAIN_DATABASE_FILENAME))
				return false;
			
			//�������� �� ��������� ��������� ���� (��� ������� ����� - ���� ���)
			if(!upload_file_to_dropbox(RESERVE_DATABASE_FILENAME))
				return false;
		}
		
		//������� ���� � ������� ���� ������
		if(!create_external_file(database_string))
			return false;
		
		//�������� �� ��������� �������� ���� (��� ������� ����� - ���� ���)
		if(!upload_file_to_dropbox(MAIN_DATABASE_FILENAME))
			return false;
		
		//�������� ��������
		leave_dropbox();
		
		return true;
	}
	
	
	//������ ��� ������� � ���������
	final static private String APP_KEY = "28whwgkuszvp96j";
	final static private String APP_SECRET = "8c4v3f1t8q5jcuj";
	final static private String ACCESS_TOKEN = "zbZDYCeFSnUAAAAAAAAABK--RzVfm5H2ZWiCPf504ZOBgQIkOgM4hK3CMeDWmZX3";

	//������ ������� � �������� ����� ���� ������ �� ���������
	final static private String DATABASE_URL = "https://www.dropbox.com/s/opw3i0qvigw81kw/main.txt?dl=1";
	
	//������������ ����� � ��������� ���������
	final static private String EXTERNAL_FILENAME = Environment.getExternalStorageDirectory() + File.separator + "VPLAY_EXTERNAL_FILE.txt";
	
	//������������ ������ � ���������
	final static private String MAIN_DATABASE_FILENAME = "/main.txt";
	final static private String RESERVE_DATABASE_FILENAME = "/reserve.txt";
	
	//������ ���������
	private static DropboxAPI<AndroidAuthSession> mDBApi = null;
	
	//������������������� ������ ������
	private static void init_online()
	{
		//...? (�������� ���������� �� ���������� ��������������?)
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
	}
	
	//������������ � ���������
	private static void login_dropbox()
	{
        //�������� ������
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        
        //�����������
        mDBApi.getSession().setOAuth2AccessToken(ACCESS_TOKEN);
	}
	
	//�������������
	private static void leave_dropbox()
	{
		mDBApi.getSession().unlink();
		mDBApi = null;
		File file = new File(EXTERNAL_FILENAME);
		file.delete();
	}
	
	//���������� ���-�� ������ � ���������
	private static int get_file_count()
	{
		//�������� ������ �� �������� ����� ���������
		Entry dirent = null;
		try
    	{
			dirent = mDBApi.metadata("/", 10, null, true, null);
		}
    	catch (DropboxException e)
    	{
			return -1;
		}
		
		//�������� ������ ������
    	ArrayList<Entry> files = new ArrayList<Entry>();
    	for(Entry ent : dirent.contents)
    		files.add(ent);
    	
    	return files.size();
	}
	
	//������� ����� ���� � ��������� � ��������� �������
	private static boolean create_external_file(String database_data)
	{
		//������� ���� � ���������
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//���� ���� �� �������� - �����
    	if(!file.exists())
			return false;
    	
    	//������� ����� ������
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//�������� ������ � ���� ������
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
    	
    	//��������� ���� �������
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
	
	//������� ����� ���� � ��������� � ��������� �������
	private static String get_data_string_from_external_file()
	{
		//������� ���� � ���������
    	File file = new File(EXTERNAL_FILENAME);
    	
    	//���� ����� ��� - �����
    	if(!file.exists())
			return "";
    	
    	//������� ����� ������
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
    	
    	//��������� ���� �������
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
    	
    	//�������� ������ �� ����
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
	
	//��������� ���� � ��������� � url
	private static boolean download_file_from_url_to_external(String string_url)
	{
		//������� ���� � ���������
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//���� ���� �� �������� - �����
    	if(!file.exists())
			return false;
    	
    	//������� ������ �� ����
    	URL url = null;
    	try
    	{
			url = new URL(string_url);
		}
    	catch (MalformedURLException e)
		{
			return false;
		}
    	
    	//����� ��� ������ � url
    	InputStream is;
		try
		{
			is = url.openStream();
		}
		catch (IOException e)
		{
			return false;
		}
    	
    	//������� ����� �������
    	DataInputStream dis = new DataInputStream(is);
    	
    	//������� ����� ������ ��� ������ � ����
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//����������� �������� ��� ������ � ����
    	byte[] buffer = new byte[1024];
        int length;
    
        //���������� �����
        try
        {
        	while((length = dis.read(buffer)) > 0)
        		output_stream.write(buffer, 0, length);
        }
        catch (IOException e1)
        {
        	return false;
        }
    	
    	//������� �����
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
	
	//��������� ���� � ���������
	private static boolean download_file_to_external(String filename)
	{
		//������� ���� � ���������
    	File file = new File(EXTERNAL_FILENAME);
    	try
    	{
			file.createNewFile();
		}
    	catch (IOException e)
    	{
			return false;
		}
    	
    	//���� ���� �� �������� - �����
    	if(!file.exists())
			return false;
    	
    	//������� ����� ������
    	FileOutputStream output_stream = null;
    	try
    	{
			output_stream = new FileOutputStream(file);
		}
    	catch (FileNotFoundException e)
    	{
			return false;
		}
    	
    	//�������� ���� (���������)
    	try
    	{
    		mDBApi.getFile(filename, null, output_stream, null);
    	}
    	catch (DropboxException e)
    	{
			return false;
		}
    	
    	//������� �����
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
	
	//��������� ���� �� �������� (� ������� ���� ����� ���� ��� ����������)
	private static boolean upload_file_to_dropbox(String filename)
	{
		//������� ������� ����
		File file = new File(EXTERNAL_FILENAME);
		
		//���� ����� �� ���������� - ������ �� ������
		if(!file.exists())
			return false;
		
		//������� ����� ��� ���������� �� �����
    	FileInputStream input_stream;
		try
		{
			input_stream = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			return false;
		}
    	
    	//��������� ���� �� ��������
    	try
    	{
			mDBApi.putFileOverwrite(filename, input_stream, file.length(), null);
		}
    	catch (DropboxException e)
    	{
			return false;
		}
    	
    	//������� �����
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