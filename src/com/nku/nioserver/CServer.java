package com.nku.nioserver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.nku.main.SessionStateMgr;

public class CServer implements Runnable{

	private static Selector selector;
	private ServerSocketChannel sschannel;
	private InetSocketAddress address;
	private int port;
//	private String ip="127.0.0.1";
	private InetAddress ip=InetAddress.getLocalHost();
	
	public CServer(int port)throws Exception
	{
		this.port = port;
		
		for(int i=0;i<1;i++)
		{
			Thread r1 = new Thread(new Reader());
			r1.start();
			
			Thread w1 = new Thread(new CWriter());
			w1.start();
			
			Thread h1 = new Thread(new CTaskHandler());
			h1.start();
		}

		//���������������׽���
		
		selector = Selector.open();
		sschannel = ServerSocketChannel.open();
		address = new InetSocketAddress(ip,this.port);
		sschannel.configureBlocking(false);
		ServerSocket ss = sschannel.socket();
		ss.bind(address);
		sschannel.register(selector, SelectionKey.OP_ACCEPT);
		
	}
	@Override
	public void run()
	{
		System.out.println("server to client started......");
		
		while(true)
		{
			try
			{
				int num=0;
				num = selector.select();
				
				if(num>0)
				{
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> it = selectedKeys.iterator();
					while(it.hasNext())
					{
						SelectionKey key = (SelectionKey)it.next();
						it.remove();
						
						if((key.readyOps()&SelectionKey.OP_ACCEPT)==SelectionKey.OP_ACCEPT)
						{
							ServerSocketChannel scc = (ServerSocketChannel)key.channel();
							
							SocketChannel sc = scc.accept();
							
							Request request = new Request(sc);
							sc.configureBlocking(false);
							
							sc.register(selector, SelectionKey.OP_READ,request);
						}
						else if((key.readyOps()&SelectionKey.OP_READ)==SelectionKey.OP_READ)
						{
							Reader.processRequest(key);
							key.cancel();
						}
						else if((key.readyOps()&SelectionKey.OP_WRITE)==SelectionKey.OP_WRITE)
						{
							CWriter.processRequest(key);
							key.cancel();
						}
					}
				}
			}catch(Exception e)
			{
				SessionStateMgr.logger.error(e.getMessage(),e);
				e.printStackTrace();
				continue;
			}
		}
	}
	
}
