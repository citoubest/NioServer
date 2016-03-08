package com.nku.util.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

import com.nku.main.SessionStateMgr;

//DBCP数据库连接池类
public final class DBCPBean {

	private static DataSource DS;
	
	
	public Connection getConn()
	{
		try
		{
			return DS.getConnection();
		}
		catch(SQLException e)
		{
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
			return null;
		}
	}
	
	public DBCPBean(){};
	
	public DBCPBean(String connectURI,String username,String password)
	{
		initDS(connectURI,username,password);
	}
	public DBCPBean(String connectURI,String username,String pswd,String driverClass,
			int initialSize,int maxActive,int maxIdle,int maxWait)
	{
		initDS(connectURI,username,pswd,driverClass,initialSize,maxActive,maxIdle,maxWait);
	}
	
	public static void initDS(String connectURI,String username,String password) {  
        initDS(connectURI, username,password, "com.mysql.jdbc.Driver", 5, 100, 30, 10000);  
    } 
	
	public static void initDS(String connectURI, String username, String pswd, String driverClass, int initialSize,  
            int maxActive, int maxIdle, int maxWait)
	{  
        BasicDataSource ds = new BasicDataSource();  
        ds.setDriverClassName(driverClass);  
        ds.setUsername(username);  
        ds.setPassword(pswd);  
        ds.setUrl(connectURI);  
        ds.setInitialSize(initialSize); // 初始的连接数；  
        ds.setMaxActive(maxActive);  
        ds.setMaxIdle(maxIdle);  
        ds.setMaxWait(maxWait);  
        DS = ds;  
    } 
	
	public static void shutdownDataSource()
	{
		BasicDataSource bds = (BasicDataSource)DS;
		try {
			bds.close();
		} catch (SQLException e) {
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
	}
}
