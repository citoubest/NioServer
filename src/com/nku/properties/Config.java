package com.nku.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.nku.main.SessionStateMgr;

public class Config {

	private static Properties prop = new Properties();
	private static String path="./config/config.properties";
	static
	{
		try
		{
			prop.load(new FileInputStream(path));
		}
		catch(IOException e)
		{
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
	}
	
	
	public static final int CLIENT_PORT=Integer.parseInt(prop.getProperty("clientPort"));
	public static final int SERVER_PORT=Integer.parseInt(prop.getProperty("serverPort"));
	
	public static final String SERVER_URL  =prop.getProperty("ServerURL");
	
	//…Ë÷√≥£¡ø
	public static final String CLASS_NAME = prop.getProperty("CLASS_NAME");
	public static final String CONNECTURL = prop.getProperty("CONNECTURL");
	public static final String USERNAME = prop.getProperty("USERNAME");
	public static final String PASSWORD = prop.getProperty("PASSWORD");
	public static final int MAXCONNETCOUNT=Integer.valueOf(prop.getProperty("MAXCONNETCOUNT"));
	public static final int LASTSIGNTIMEINTERVAL=Integer.valueOf(prop.getProperty("LASTSIGNTIMEINTERVAL"));
	public static final int CHECKIDLEFREQUENCY = Integer.valueOf(prop.getProperty("CHECKIDLEFREQUENCY"));
	public static final int DBCHECK = Integer.valueOf(prop.getProperty("DBCHECK"));
}
