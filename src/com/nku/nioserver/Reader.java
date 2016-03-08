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
public class Reader implements Runnable{
	
	private static List<SelectionKey> pool = new LinkedList<SelectionKey>();
	public Reader()
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
			CTaskHandler.processTask(key);

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
		synchronized (pool)
		{
			pool.add(pool.size(), key);
			pool.notifyAll();
		}
	}
}
