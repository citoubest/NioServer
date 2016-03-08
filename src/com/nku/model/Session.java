package com.nku.model;

import java.util.Date;


public class Session {

	private String sessionid;
	private byte[] key;
	private String uid;
	private long lastsigntime;
	private int state;//0,wowza尚未用该session进行验证，1session已经验证过
	
	
	public Session(String sessionid, byte[] key) {
		super();
		this.sessionid = sessionid;
		this.key = key;
		this.state=0;
		this.lastsigntime=new Date().getTime();
	}
	
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	
	public String getSessionid() {
		return sessionid;
	}
	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}
	public byte[] getKey() {
		return key;
	}
	public void setKey(byte[] key) {
		this.key = key;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public long getLastsigntime() {
		return lastsigntime;
	}
	public void setLastsigntime(long lastsigntime) {
		this.lastsigntime = lastsigntime;
	}
	
	
		
}
