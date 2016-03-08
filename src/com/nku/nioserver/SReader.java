package com.nku.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.nku.main.SessionStateMgr;
import com.nku.util.PacketUtil;
public class SReader implements Runnable{
	
	private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
	private static int count=0;
	private static long firsrequest;
	private static long current;
	private static  boolean isFirst=true;
	public SReader()
	{
		
	}
	
	@Override
	public void run() {

		while(true)
		{
			try
			{
				SelectionKey key;
				synchronized (pool) {
					while(pool.isEmpty())
					{
						pool.wait();
					}
					key =(SelectionKey)pool.remove(0);
				}
				
				read(key);
			}catch(Exception e)
			{
				SessionStateMgr.logger.error(e.getMessage()+" reader pool size:"+pool.size(),e);
				e.printStackTrace();
				continue;
			}
		}
		
	}
	
	public static byte[]readRequest(SocketChannel sc) throws IOException, NotYetConnectedException
	{
		ByteBuffer bytebuffer = ByteBuffer.allocate(128);
		
		while(true)
		{
			int readBytes = sc.read(bytebuffer);  
			
			if (readBytes > 0)
			{  
				bytebuffer.flip();
				
				byte[] retVal = new byte[readBytes];
				for(int i=0;i<readBytes;i++)
				{
					retVal[i]=bytebuffer.get(i);
				}
				
				return retVal;
			} 
		}
	}
	
	public void read(SelectionKey key)
	{
		byte[]clientData=null;
		SocketChannel sc = null;
		try
		{
			sc = (SocketChannel)key.channel();
			clientData = readRequest(sc);
		
			Request request = (Request)key.attachment();
			request.setDataInput(clientData);
			
			//放入任务队列中
			STaskHandler.processTask(key);
			count++;

			if (count == 1000) {
				current = System.currentTimeMillis();
				System.out.println("reader 1000 task:" + (current - firsrequest));
			}if (count == 5000) {
				current = System.currentTimeMillis();
				System.out.println("reader 5000 task:" + (current - firsrequest));
			}
			if (count == 10000) {
				current = System.currentTimeMillis();
				System.out.println("reader 10000 task:" + (current - firsrequest));
			}
			if (count == 100000) {
				current = System.currentTimeMillis();
				System.out.println("reader 100000 task:" + (current - firsrequest));
			}
			if (count == 200000) {
				current = System.currentTimeMillis();
				System.out.println("reader 200000 task:" + (current - firsrequest));
			}
			if (count == 300000) {
				current = System.currentTimeMillis();
				System.out.println("reader 300000 task:" + (current - firsrequest));
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
			if(clientData!=null)
			{
				SessionStateMgr.logger.error(e.getMessage()+PacketUtil.bytes2HexString(clientData),e);
			}
			try {
				if(sc.socket()!=null)
				{
					sc.socket().close();
				}
				if(sc!=null)
				{
					sc.close();
				}
			} catch (IOException e1) {
			}
			e.printStackTrace();
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
			pool.add(pool.size(), key);
			pool.notifyAll();
		}
	}
}
