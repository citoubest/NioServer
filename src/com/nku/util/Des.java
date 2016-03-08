package com.nku.util;

import java.text.DecimalFormat;
import java.util.Random;

public class Des {

	native byte[] encrypt(byte msg[],int len,byte[]key);
	native byte[] decrypt(byte msg[],int len,byte[]key);
	native byte[] generate_session_key(byte dyn_key[],byte static_key[],int len);
	
	private static byte mStatic_key[];
	
	static 
	{
		System.loadLibrary("Des");
	
		mStatic_key=new byte[14];
		
		for(int i=0;i<mStatic_key.length;i++)
		{
			mStatic_key[i]='a';
		}
	}

	public byte[] des_encrypt(byte msg[],int len,byte[]key)
	{
		if(msg==null ||key==null)
		{
			return null;
		}
		return encrypt(msg,len,key);
	}
	
	public byte[] des_decrypt(byte msg[],int len,byte[]key)
	{
		if(msg==null || key==null)
		{
			return null;
		}
		return decrypt(msg, len, key);
	}
	
	public byte[]  des_login_key_get(byte []dyn_key,int m_len)
	{
		if(dyn_key==null)
		{
			return null;
		}
		return generate_session_key(dyn_key, mStatic_key, m_len);
	}
	
	public byte[] des_dynmic_key_get()
	{
		byte sendDyn_key[] = new byte[32];
		byte mDyn_key[] = new byte[14];
		for (int i = 0;i < 32; i++) 
		{
			sendDyn_key[i] = (byte) (Math.random()*255);
		}
		mDyn_key[0] = sendDyn_key[0];
		mDyn_key[1] = sendDyn_key[4];
		mDyn_key[2] = sendDyn_key[5];
		mDyn_key[3] = sendDyn_key[6];
		mDyn_key[4] = sendDyn_key[9];
		mDyn_key[5] = sendDyn_key[11];
		mDyn_key[6] = sendDyn_key[16];
		mDyn_key[7] = sendDyn_key[17];
		mDyn_key[8] = sendDyn_key[19];
		mDyn_key[9] = sendDyn_key[21];
		mDyn_key[10] = sendDyn_key[22];
		mDyn_key[11] = sendDyn_key[27];
		mDyn_key[12] = sendDyn_key[28];
		mDyn_key[13] = sendDyn_key[31];
		
		return mDyn_key;
	}
	
	public String randSessionID()
	{
		int rand=Math.abs(new Random().nextInt())%100000000;
		String sid=new DecimalFormat("00000000").format(rand);
		return sid;
	}
}
