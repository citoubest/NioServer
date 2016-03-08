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

/*
 * 该线程监听与wowza通信的端口
 * */
public class SServer implements Runnable{
	
	private static Selector selector;  
	private	ServerSocketChannel sschannel = null;
	private InetSocketAddress address;
	private int port;
	private InetAddress ip = InetAddress.getLocalHost();

	public SServer(int port)throws Exception
	{
		this.port = port;
		
		for(int i=0;i<1;i++)
		{
			Thread r1 = new Thread(new SReader());
			r1.start();
			
			Thread w1 = new Thread(new SWriter());
			w1.start();
			
			Thread h1 = new Thread(new STaskHandler());
			h1.start();
		}

		//创建无阻塞网络套接字
		
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
		System.out.println("server to wowza started......");
		
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
							SReader.processRequest(key);
							key.cancel();
						}
						else if((key.readyOps()&SelectionKey.OP_WRITE)==SelectionKey.OP_WRITE)
						{
							SWriter.processRequest(key);
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
