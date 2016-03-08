package com.nku.nioserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.nku.main.SessionStateMgr;


public class CWriter implements Runnable{

	private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
	private static int count = 0;
	
	private static long firsrequest;
	private static long current;
	private static  boolean isFirst=true;
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
			
			count++;
			
			if (count == 1000) {
				current = System.currentTimeMillis();
				System.out.println("writer 1000 task:" + (current - firsrequest));
			}
			if (count == 5000) {
				current = System.currentTimeMillis();
				System.out.println("writer 5000 task:" + (current - firsrequest));
			}
			if (count == 10000) {
				current = System.currentTimeMillis();
				System.out.println("writer 10000 task:" + (current - firsrequest));
			}
			if (count == 100000) {
				current = System.currentTimeMillis();
				System.out.println("writer 100000 task:" + (current - firsrequest));
			}
			if (count == 200000) {
				current = System.currentTimeMillis();
				System.out.println("writer 200000 task:" + (current - firsrequest));
			}
			if (count == 300000) {
				current = System.currentTimeMillis();
				System.out.println("writer 300000 task:" + (current - firsrequest));
			}
			if (count == 500000) {
				current = System.currentTimeMillis();
				System.out.println("handle 500000 task:" + (current - firsrequest));
			}
			if (count == 1000000) {
				current = System.currentTimeMillis();
				System.out.println("handle 1000000 task:" + (current - firsrequest));
			}
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
		if(isFirst)
		{
			firsrequest = System.currentTimeMillis();
			isFirst=false;
		}
		
		synchronized (pool)
		{
			pool.add(pool.size(),key);
			pool.notifyAll();
		}
	}
}
