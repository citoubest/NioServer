package com.nku.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.nku.model.Session;
import com.nku.nioserver.CServer;
import com.nku.nioserver.SServer;
import com.nku.properties.Config;
import com.nku.thread.CleanIdleSessionTask;
import com.nku.thread.DBMaintainThread;
import com.nku.util.db.DBCPBean;


public class SessionStateMgr {

	//	public //�洢Session�ı���
	public static Map<String,Session> sessionmap;
	public static DBCPBean bean;
	public static final long INTERVAL=Config.LASTSIGNTIMEINTERVAL*60*1000;
	public static final long CHECKIDLEFREQUENCY =Config.CHECKIDLEFREQUENCY*1000;
	public static final long DBCHECKFREQUENCT  = Config.DBCHECK*24*60*60*1000;
	public static final int THREADPOOLSIZE=20;
	public static Logger logger =  Logger.getLogger("logTest"); 


	static
	{
		PropertyConfigurator.configure("config/log4j.properties"); 
	}
	public static void main(String[] args)
	{
		SessionStateMgr ssm = new SessionStateMgr();
		ssm.init();
	}
	private void init()
	{
		try
		{
			//1.��ʼ��session��
			sessionmap = new HashMap<String, Session>();

			//2.���ݿ����ӳ�
			bean= new DBCPBean(Config.CONNECTURL,Config.USERNAME,Config.PASSWORD);
			//3. ����������
			startServices();
		}
		catch(Exception e)
		{
			logger.error("init����ʧ��:"+e.getMessage(),e);
			e.printStackTrace();
			destroy();
		}

	}

	private void startServices()
	{	
		try {
			Thread ss = new Thread(new SServer(Config.SERVER_PORT));
			ss.start();
			
			CServer server = new CServer(Config.CLIENT_PORT);
			Thread tServer = new Thread(server);
			tServer.start();
		} catch (Exception e) {
			logger.error("startServices:"+e.getMessage(),e);
			
		}


		//�嵥��ʱ�������session�̣߳�CHECKIDLEFREQUENCY����һ��ִ�У�ÿ��CHECKIDLEFREQUENCY�룬ִ��һ��
		Timer timer1 = new Timer();
		timer1.scheduleAtFixedRate(new CleanIdleSessionTask(), CHECKIDLEFREQUENCY, CHECKIDLEFREQUENCY);

		//������ڵĳ�ֵ��������Ƿ�ѵ��û�
		Timer timer2 = new Timer();
		timer2.scheduleAtFixedRate(new DBMaintainThread(),0 ,DBCHECKFREQUENCT);
		
	}

	public void destroy()
	{
		//����session����
		sessionmap=null;
		DBCPBean.shutdownDataSource();
		bean=null;
	}
}
