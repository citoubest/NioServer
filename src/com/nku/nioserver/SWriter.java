package com.nku.nioserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.nku.main.SessionStateMgr;


public class SWriter implements Runnable{

	private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
	public void run()
	{
		while(true)
		{
			try{
				SelectionKey key;
				synchronized (pool) {
					while(pool.isEmpty())
					{
						pool.wait();
					}
					key = (SelectionKey)pool.remove(0);
				}
				write(key);
			}catch(Exception e)
			{
				SessionStateMgr.logger.error(e.getMessage()+"  write pool size:"+pool.size(),e);
				e.printStackTrace();
				continue;
			}
		}
	}
	
	public void write(SelectionKey key)
	{
		SocketChannel sc =null;
		try
		{
			sc = (SocketChannel)key.channel();
			Response response = new Response(sc);

			//将数据出去
			Request request =(Request)key.attachment();
			byte[]dataoutput = request.getDataOutput();
			if(dataoutput!=null)
			{
				response.send(request.getDataOutput());
			}
			sc.socket().setReuseAddress(true);
			sc.finishConnect();
			sc.socket().close();
			sc.close();
		}
		catch(Exception e)
		{
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
		finally
		{
			try {
				if(sc.socket()!=null)
				{
					sc.socket().close();
				}
				if(sc!=null)
				{
					sc.close();
				}
			} catch (IOException e) {
			}
			
		}
	}
	
	
	
	public static void processRequest(SelectionKey key)
	{
		synchronized (pool)
		{
			pool.add(pool.size(),key);
			pool.notifyAll();
		}
	}
}
