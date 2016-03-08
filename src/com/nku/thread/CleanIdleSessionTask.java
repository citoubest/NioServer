package com.nku.thread;

import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

import com.nku.main.SessionStateMgr;
import com.nku.model.Session;

public class CleanIdleSessionTask extends TimerTask{

	@Override
	public void run() {
		int count =0;
		long currenttime=new Date().getTime();
		synchronized (SessionStateMgr.sessionmap)
		{
			Set<Entry<String, Session>>  set= SessionStateMgr.sessionmap.entrySet();
			Iterator<Entry<String, Session>> iter =set.iterator();
			while(iter.hasNext())
			{
				Entry<String, Session> entry =  iter.next();	
				long lastsigntime=entry.getValue().getLastsigntime();
				if(currenttime-lastsigntime>SessionStateMgr.INTERVAL)
				{
					iter.remove();
					count++;
				}				
			}
		}
		
		SessionStateMgr.logger.info("clean idle session:"+count);
		SessionStateMgr.logger.info("session size:"+SessionStateMgr.sessionmap.size());
	}
}
