package com.nku.nioserver;

import java.nio.channels.SocketChannel;

public class Request {

	private SocketChannel sc;
	private byte[]dataInput = null;

	private byte[]dataOutput = null;

	public Request(SocketChannel sc)
	{
		this.sc = sc;
	}
	public SocketChannel getSC()
	{
		return this.sc;
	}

	public byte[] getDataInput() {
		return dataInput;
	}
	public void setDataInput(byte[] dataInput) {
		this.dataInput = dataInput;
	}
	
	public byte[] getDataOutput() {
		return dataOutput;
	}
	public void setDataOutput(byte[] dataOutput) {
		this.dataOutput = dataOutput;
	}
}
