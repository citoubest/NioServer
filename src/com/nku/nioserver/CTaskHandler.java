package com.nku.nioserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.nku.dao.AuthenticationDao;
import com.nku.main.SessionStateMgr;
import com.nku.model.Session;
import com.nku.util.ConfigRetVal;
import com.nku.util.Des;
import com.nku.util.PacketUtil;

public class CTaskHandler implements Runnable {

	private static List<SelectionKey> taskPool = new LinkedList<SelectionKey>();

	private static boolean isFirst=true;
	Des des = new Des();

	public CTaskHandler() {

	}

	@Override
	public void run() {
		while (true) {
			try {
				SelectionKey key;
				synchronized (taskPool) {
					while (taskPool.isEmpty()) {
						taskPool.wait();
					}
					key = (SelectionKey) taskPool.remove(0);
				}

				handleTask(key);
			} catch (Exception e) {
				SessionStateMgr.logger.error(e.getMessage()+"task size info:"+taskPool.size(),e);
				e.printStackTrace();
				continue;
			}
		}

	}

	public void handleTask(SelectionKey key)
	{
		Request request = (Request) key.attachment();

		byte req[] = request.getDataInput();
		byte []msg = null;

		try{
			if (req != null)
			{
				//提取头部数据
				byte header = req[0];

				switch(header)
				{
				case 65://请求密钥
					msg=generateKey();
					request.setDataOutput(msg);
					break;
				case 66://验证机顶盒id 报文格式应该是66+12345678
					msg= identifyID(req);
					request.setDataOutput(msg);
					break;
					
				case 67://激活ID
					msg= activeID(req);
					request.setDataOutput(msg);
					break;
					
				case 68://充值卡激活ID
					msg =activeIdbyCard(req);
					request.setDataOutput(msg);
					break;
				
				case 69://账户续费
					msg = getAccountState(req);
					request.setDataOutput(msg);
					break;
				case 70://请求节目列表
					msg = askStreamList(req);
					request.setDataOutput(msg);
					break;
					
				case 71://更新状态
					updateState(req);
					break;
				default:
					request.setDataOutput(new byte[]{ (byte)127});
				}
			}
			CWriter.processRequest(key);

		}catch(Exception e)
		{
			e.printStackTrace();
			if(req!=null)
			{
				SessionStateMgr.logger.error(e.getMessage()+"  at handle task req:"+ PacketUtil.bytes2HexString(req),e);
			}
			else
			{
				SessionStateMgr.logger.error(e.getMessage(),e);
			}

			SocketChannel sc = (SocketChannel)key.channel();
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

		}

	}
	public byte[] generateKey()
	{
		String sessionid;
		byte mDyn_key[] = des.des_dynmic_key_get();
		byte mSes_key[];
		int mKey_len = 14;

		mSes_key = des.des_login_key_get(mDyn_key, mKey_len);

		byte sendDyn_key[] = new byte[32];
		for (int i = 0; i < 32; i++)
		{
			sendDyn_key[i] = (byte) (Math.random() * 255);
		}

		sendDyn_key[0] = mDyn_key[0];
		sendDyn_key[4] = mDyn_key[1];
		sendDyn_key[5] = mDyn_key[2];
		sendDyn_key[6] = mDyn_key[3];
		sendDyn_key[9] = mDyn_key[4];
		sendDyn_key[11] = mDyn_key[5];
		sendDyn_key[16] = mDyn_key[6];
		sendDyn_key[17] = mDyn_key[7];
		sendDyn_key[19] = mDyn_key[8];
		sendDyn_key[21] = mDyn_key[9];
		sendDyn_key[22] = mDyn_key[10];
		sendDyn_key[27] = mDyn_key[11];
		sendDyn_key[28] = mDyn_key[12];
		sendDyn_key[31] = mDyn_key[13];


		sessionid = des.randSessionID();

		Session session = new Session(sessionid, mSes_key);

		// 添加的session列表中
		synchronized (SessionStateMgr.sessionmap) {
			SessionStateMgr.sessionmap.put(sessionid, session);
		}

		byte header[] = new byte[]{(byte) 129};
		byte packet[] =PacketUtil.byteMerger(header,sessionid.getBytes());
		byte[] msg = PacketUtil.byteMerger(packet, sendDyn_key);

		return msg;
	}

	public byte[] identifyID(byte[] req)
	{
		String sessionid;
		int len =req.length;
		byte header[] = new byte[]{(byte) 130};
		byte[] response;
		
		if(len>=9)
		{
			sessionid=new String(req).substring(1, 9);
			byte content[] = new byte[len-9];

			for(int i=0;i<len-9;i++)
			{
				content[i]=req[i+9];
			}


			byte[] enKey=null;
			synchronized (SessionStateMgr.sessionmap){
				Session ss =SessionStateMgr.sessionmap.get(sessionid);
				if(ss!=null)
				{
					enKey = ss.getKey();
				}
			}

			if (null != enKey)
			{
				String flag = AuthenticationDao.isIDValid(sessionid,content, enKey);

				response = PacketUtil.byteMerger(header, flag.getBytes());
				byte enMsg[] = des.des_encrypt(response,response.length, enKey);
				return enMsg;
			}
			else//session失效
			{
				response = new byte[]{33};
				response = PacketUtil.byteMerger(header, response);
				return response;
			}
		}
		else//长度不够
		{
			response = new byte[]{34};
			response = PacketUtil.byteMerger(header, response);
			return response;
		}
	}

	//报文 ： 报头+sessionid+encrypt[byte(id_len)+id]
	public byte[]activeID(byte[]req)
	{
		byte[] response;
		byte header[] = new byte[]{(byte) 131};
		
		int len =req.length;
		if(len>=9)
		{	
			String sessionid = new String(req).substring(1, 9);

			byte content[] = new byte[len-9];

			for(int i=0;i<len-9;i++)
			{
				content[i]=req[i+9];
			}
			
			byte[] enKey=null;
			synchronized (SessionStateMgr.sessionmap){
				Session ss =SessionStateMgr.sessionmap.get(sessionid);
				if(ss!=null)
				{
					enKey = ss.getKey();
				}
			}

			
			if (null != enKey)
			{
				String flag = AuthenticationDao.activeID(sessionid,content, enKey);
				
				response = PacketUtil.byteMerger(header, flag.getBytes());

				byte enMsg[] = des.des_encrypt(response,response.length, enKey);

				return enMsg;
			}
			else
			{
				response = new byte[]{33};
				response = PacketUtil.byteMerger(header, response);
				return response;
			}
		}else 
		{
			response = new byte[]{34};
			response = PacketUtil.byteMerger(header, response);
			return response;
		}
	}

	//充值卡激活id  报文 ：报头+sessionid+encrypt[cardid+pwd+byte(id_len)+id]
	public byte[]activeIdbyCard(byte[]req)
	{
		int len =req.length;
		
		byte[] response;
		byte header[] = new byte[]{(byte) 132};
		if(len>=9)
		{	
			String sessionid = new String(req).substring(1, 9);

			byte content[] = new byte[len-9];

			for(int i=0;i<len-9;i++)
			{
				content[i]=req[i+9];
			}
			byte[] enKey=null;
			synchronized (SessionStateMgr.sessionmap){
				Session ss =SessionStateMgr.sessionmap.get(sessionid);
				if(ss!=null)
				{
					enKey = ss.getKey();
				}
			}
			if (null != enKey) {
				String flag = AuthenticationDao.activeIDByCard(sessionid,content, enKey);
				
				response = PacketUtil.byteMerger(header, flag.getBytes());

				byte enMsg[] = des.des_encrypt(response,response.length, enKey);

				return enMsg;
			}
			else
			{
				response = new byte[]{33};
				response = PacketUtil.byteMerger(header, response);
				return response;
			}
		}else
		{
			response = new byte[]{34};
			response = PacketUtil.byteMerger(header, response);
			return response;

		}

	}

	//账户续费 报文:报头+sessionid+encrypt[cardid+pwd+id]
//	public byte[]chargeAccount(byte[]req)
//	{
//		int len =req.length;
//		
//		byte[] response;
//		byte header[] = new byte[]{(byte) 133};
//		
//		if(len>=9)
//		{	
//			String sessionid = new String(req).substring(1, 9);
//
//			byte content[] = new byte[len-9];
//
//			for(int i=0;i<len-9;i++)
//			{
//				content[i]=req[i+9];
//			}
//			
//			byte[] enKey=null;
//			synchronized (SessionStateMgr.sessionmap){
//				Session ss =SessionStateMgr.sessionmap.get(sessionid);
//				if(ss!=null)
//				{
//					enKey = ss.getKey();
//				}
//			}
//			
//			if (null != enKey)
//			{
//				String flag = AuthenticationDao.rechargeIDByCard(sessionid,
//						content, enKey);
//				response = PacketUtil.byteMerger(header, flag.getBytes());
//
//				byte enMsg[] = des.des_encrypt(response,response.length, enKey);
//
//				return enMsg;
//			} 
//			else
//			{
//				response = new byte[]{33};
//				response = PacketUtil.byteMerger(header, response);
//				return response;
//			}
//		}
//		else
//		{
//			response = new byte[]{34};
//			response = PacketUtil.byteMerger(header, response);
//			return response;
//		}	
//	}
	
	public byte[] getAccountState(byte[]req)
	{
		byte[] response;
		byte header[] = new byte[]{(byte)133 };
		
		int len =req.length;
		if(len>=9)
		{	
			String sessionid = new String(req).substring(1, 9);

			byte content[] = new byte[len-9];

			for(int i=0;i<len-9;i++)
			{
				content[i]=req[i+9];
			}
			
			byte[] enKey=null;
			synchronized (SessionStateMgr.sessionmap){
				Session ss =SessionStateMgr.sessionmap.get(sessionid);
				if(ss!=null)
				{
					enKey = ss.getKey();
				}
			}

			
			if (null != enKey)
			{
				String info= AuthenticationDao.getAccountInfo(sessionid,content, enKey);
				
				response = PacketUtil.byteMerger(header,info.getBytes());

				byte enMsg[] = des.des_encrypt(response,response.length, enKey);

				return enMsg;
			}
			else
			{
				response = new byte[]{33};
				response = PacketUtil.byteMerger(header, response);
				return response;
			}
		}else 
		{
			response = new byte[]{34};
			response = PacketUtil.byteMerger(header, response);
			return response;
		}
	}
	public byte[]askStreamList(byte[]req)
	{
		byte[] response;
		byte header[] = new byte[]{(byte) 134};
		int len =req.length;
		if(len>=9)
		{	
			String sessionid = new String(req).substring(1, 9);

			byte content[] = new byte[len-9];

			for(int i=0;i<len-9;i++)
			{
				content[i]=req[i+9];
			}
			

			byte[] enKey=null;
			synchronized (SessionStateMgr.sessionmap){
				Session ss =SessionStateMgr.sessionmap.get(sessionid);
				if(ss!=null)
				{
					enKey = ss.getKey();
				}
			}

			String flag=null;
			if (null != enKey)
			{
				String str[] = AuthenticationDao.getVideoList(sessionid);
				if (str == null)
				{
					flag = ConfigRetVal.sessionid_invalid;
					response = PacketUtil.byteMerger(header, flag.getBytes());
				} else
				{
					flag = ConfigRetVal.id_valid;
					response = PacketUtil.byteMerger(header, flag.getBytes());
					
					byte[]con = PacketUtil.combPakcet(str);
					response = PacketUtil.byteMerger(response, con);
				}

				byte enMsg[] = des.des_encrypt(response,response.length, enKey);

				return enMsg;
			}
			else
			{
				response = new byte[]{33};
				response = PacketUtil.byteMerger(header, response);
				return response;
			}
		}
		else
		{
			response = new byte[]{34};
			response = PacketUtil.byteMerger(header, response);
			return response;
		}
	}
	
	public void updateState(byte[]req)
	{
		int len = req.length;
		if(len>=9)
		{	
			String sessionid = new String(req).substring(1, 9);
			AuthenticationDao.updateSIgnTime(sessionid);
		}
	}
	
	
	public static void processTask(SelectionKey key) {

		if(isFirst)
		{
			isFirst =false;
		}
		synchronized (taskPool)
		{
			taskPool.add(taskPool.size(), key);
			taskPool.notifyAll();
		}
	}
}
