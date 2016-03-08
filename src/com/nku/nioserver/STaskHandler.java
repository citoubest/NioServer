package com.nku.nioserver;

import java.io.IOException;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.nku.dao.AuthenticationDao;
import com.nku.main.SessionStateMgr;

public class STaskHandler implements Runnable {

	private static List<SelectionKey> taskPool = new LinkedList<SelectionKey>();

	public STaskHandler() {

	}

	@Override
	public void run() {
		while (true) {
			try {
				SelectionKey key;
				synchronized (taskPool) {
					while (taskPool.isEmpty()) {
						taskPool.wait();
					}
					key = (SelectionKey) taskPool.remove(0);
				}

				handleTask(key);
			} catch (Exception e) {
				SessionStateMgr.logger.error(e.getMessage()+"task size info:"+taskPool.size(),e);
				e.printStackTrace();
				continue;
			}
		}

	}

	public void handleTask(SelectionKey key) throws Exception
	{
		Request r = (Request) key.attachment();

		byte req[] = r.getDataInput();
		try
		{
			if(req!=null)
			{
				int len = req.length;
				if(len>0)
				{
					String request=new String(req);

					System.out.println(request);

					if(request.startsWith("DQT101"))
					{
						String header="DQT102";
						if(request.length()>14)
						{
							String sessionid = request.substring(6, 14);

							String id = request.substring(14, len);

							byte en_id[]=new byte[(len-14)/3];
							int j=0;
							if((len-14)%3==0)
							{
								for(int i=0;i<len-14;i+=3)
								{
									en_id[j]=(byte) (Integer.parseInt(id.substring(i, i+3))-256);
									j++;
								}

								byte[] en_key;
								synchronized (SessionStateMgr.sessionmap) {
									en_key=SessionStateMgr.sessionmap.get(sessionid).getKey();
								}
								
								if(en_key!=null)
								{
									//解析b中的数据，查询是否符合要求
									int	retVal= AuthenticationDao.isSessionIdValid(sessionid ,en_id,en_key);
									header+=retVal;
								}
								else
								{
									header+="404";
								}
							}
							else
							{
								header+="404";
							}
							r.setDataOutput(header.getBytes());
						}
						else
						{
							r.setDataOutput("DQT102411".getBytes());
						}
						SWriter.processRequest(key);
					}
				}
			}
		}
		catch(Exception e)
		{
			SocketChannel sc = (SocketChannel)key.channel();
			try {
				if(sc.socket()!=null)
				{
					sc.socket().close();
				}
				if(sc!=null)
				{
					sc.close();
				}
			} catch (IOException e1)
			{
			}
		}

	}
	public static void processTask(SelectionKey key) {
		synchronized (taskPool) {
			taskPool.add(taskPool.size(), key);
			taskPool.notifyAll();
		}
	}
}
