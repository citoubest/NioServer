package com.nku.util;

public class PacketUtil {

	public static byte[] combPakcet(String[]content)
	{
		StringBuilder builder = new StringBuilder();
		for (String str : content) {
			builder.append((byte)str.length()).append(str);
		}
		
		return builder.toString().getBytes();
	}
	
	
	public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
		byte[] byte_3 = new byte[byte_1.length+byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}
	
	
	public static String bytes2HexString(byte[] b) {
		  String ret = "";
		  for (int i = 0; i < b.length; i++) {
		   String hex = Integer.toHexString(b[ i ] & 0xFF);
		   if (hex.length() == 1) {
		    hex = '0' + hex;
		   }
		   ret += hex.toUpperCase();
		  }
		  return ret;
		}
}
