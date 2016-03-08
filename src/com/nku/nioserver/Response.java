package com.nku.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Response {

	private SocketChannel sc;
	
	public Response(SocketChannel sc)
	{
		this.sc =sc;
	}
	
	public void send(byte[]data)throws IOException
	{
		ByteBuffer buffer =ByteBuffer.allocate(data.length);
		buffer.put(data);
		buffer.flip();
		sc.write(buffer);
	}
}
