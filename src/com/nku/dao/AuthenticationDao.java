package com.nku.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import com.nku.main.SessionStateMgr;
import com.nku.model.Session;
import com.nku.properties.Config;
import com.nku.util.ConfigRetVal;
import com.nku.util.DateUtil;
import com.nku.util.Des;
import com.nku.util.PacketUtil;


public class AuthenticationDao
{

	private static Des des = new Des();
	/**
	 * @输入：sessionid, id
	 * 
	 * @输出：200：有效且未超过连接次数，
	 *        201：有效但超过连接次数，
	 *        404：无效
	 * 
	 * */
	public static int isSessionIdValid(String sessionid ,byte[]en_id,byte[]key)
	{
		byte[] id = des.des_decrypt(en_id,en_id.length, key);

		if(id==null)
		{
			return 404;
		}

		int len = (int)id[0];
		char cid[] = new char[len];

		for(int t = 0;t < cid.length; t++)
		{
			cid[t]=(char) id[t+1];
		}
		String sid = String.valueOf(cid);

		int count=0;
		int retVal;
		boolean flag = false;//判断这个session是否验证通过
		Session curSession=null;
		synchronized (SessionStateMgr.sessionmap)
		{
			Collection<Session> c=SessionStateMgr.sessionmap.values();

			for (Session session : c)
			{
				if(session.getSessionid().equals(sessionid) && session.getUid().equals(sid) && session.getState()==0)
				{
					flag=true;
					curSession=session;
				}
				//有认证通过的
				if(sid.equals(session.getUid()))
				{
					//处理过的
					if(session.getState()==1)
					{
						count++;
					}

				}
			}
		}
		//验证通过，且还没有以该id播放过，或者以该id播放过的数量在范围内
		if(flag&&count==0|| (0<count && count<Config.MAXCONNETCOUNT))
		{
			retVal=200;
			synchronized (SessionStateMgr.sessionmap)
			{
				curSession.setState(1);
			}
		}
		else if((!flag)&&count==0)
		{
			retVal = 404;
		}
		else
		{
			retVal = 201;
		}
		return retVal;
	}

	/**
	 * @输入：sessionid ，encrypt(id)，解密密钥
	 * @输出：200 id有效
	 * 		  401 账户未激活
	 *        402 账户欠费
	 *        403 服务器端未能解析出可理解的内容
	 *        404 未找到对应id的账户
	 *        406 账户已经作废
	 *        501 服务器内部错误
	 * 
	 * */
	public static String isIDValid(String sessionid,byte en_id[],byte[] key)
	{
		byte[] id = des.des_decrypt(en_id,en_id.length, key);
		//解密失败
		if(id==null)
		{
			return ConfigRetVal.parse_err;
		}

		int len = (int)id[0];
		char cid[] = new char[len];

		if(id.length<len+1)
		{
			return ConfigRetVal.format_notcorrect;
		}

		for(int t = 0;t < cid.length; t++)
		{
			cid[t]=(char) id[t+1];
		}
		String sid = String.valueOf(cid);

		Connection conn=SessionStateMgr.bean.getConn();		
		String str ="select state as State from accountinfo where id=?";
		PreparedStatement stmt =null;
		ResultSet rs =null;

		String retVal;

		try {
			stmt = conn.prepareStatement(str);
			stmt.setString(1, sid);
			rs= stmt.executeQuery();

			if (rs.next() == true)
			{
				int state = rs.getInt("State");
				switch(state)
				{
				case 1002://有效(激活状态)
					retVal=ConfigRetVal.id_valid;

					synchronized (SessionStateMgr.sessionmap)
					{
						SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
					}
					break;
				case 1001://未激活
					retVal=ConfigRetVal.id_unactive;
					break;
				case 1003:
					retVal = ConfigRetVal.account_disable;
					break;
				case 1004://欠费
					retVal=ConfigRetVal.account_arrears;
					break;
				default:
					retVal=ConfigRetVal.not_find;
				}
			}
			else
			{
				retVal=ConfigRetVal.not_find;//没有找到该id的信息
			}
		} catch (SQLException e) {
			SessionStateMgr.logger.error(e.getMessage()+"isIDValid  sessionid:"+sessionid+"   id"+sid,e);
			e.printStackTrace();
			retVal=ConfigRetVal.server_err;//服务器内部错误
			return retVal;
		}
		finally
		{
			try { 
				if(rs!=null)
					rs.close();
				if (stmt != null)  
					stmt.close();  
				if (conn != null)  
					conn.close();  
			} catch (Exception e) {
			}  
		}

		return retVal;
	}

	/**
	 * @输入：sessidid，encrypt(id)， 密钥
	 * @输出：
	 * */
	public static String activeID(String sessionid,byte[]content,byte[]key)
	{
		byte[] id = des.des_decrypt(content,content.length, key);

		if(id==null)
		{
			return ConfigRetVal.parse_err;
		}


		int len = (int)id[0];

		if(id.length<len+1)
		{
			return ConfigRetVal.format_notcorrect;
		}
		char cid[] = new char[len];
		for(int t = 0;t < cid.length; t++)
		{
			cid[t]=(char) id[t+1];
		}
		String sid = String.valueOf(cid);

		Connection conn=SessionStateMgr.bean.getConn();	

		String str ="select state as State,bindday as Value from accountinfo where id=?";
		PreparedStatement stmt =null;
		ResultSet rs =null;
		String retVal="";
		try
		{
			stmt = conn.prepareStatement(str);
			stmt.setString(1, sid);
			rs= stmt.executeQuery();

			//1.判断id是否有效，有效则取出账户绑定时长
			if(rs.next()==true)//如果不为空，说明该id有效
			{
				int state = rs.getInt("State");
				int value = rs.getInt("Value");

				switch(state)
				{
				case 1001://未激活	
					String enddate = DateUtil.dataOperate(new Date(), value);
					str ="update accountinfo set state='1002',enddate=? where id=? and state=1001";
					stmt = conn.prepareStatement(str);
					stmt.setString(1, enddate);
					stmt.setString(2, sid);

					int flag = stmt.executeUpdate();
					if(flag>0)//成功激活
					{
						retVal="200";
						synchronized (SessionStateMgr.sessionmap)
						{
							SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
						}
					}
					else if(flag==0)//未找到对应ID
					{
						retVal=ConfigRetVal.not_find;
					}
					else
					{
						retVal=ConfigRetVal.server_err;
					}
					break;
				case 1002://已经处于激活状态
					retVal=ConfigRetVal.already_actived;
					break;
				case 1003:
					retVal = ConfigRetVal.account_disable;
					break;
				case 1004://欠费
					retVal=ConfigRetVal.account_arrears;
					break;
				}

			}
			else
			{
				retVal=ConfigRetVal.not_find;
			}
		}catch (SQLException e) {
			SessionStateMgr.logger.error(e.getMessage()+"activeID  sessionid:"+sessionid+"   id"+sid,e);
			e.printStackTrace();
			retVal=ConfigRetVal.server_err;
			return retVal;
		}
		finally
		{
			try { 
				if (stmt != null)  
					stmt.close();  
				if (conn != null)  
					conn.close();  
			} catch (Exception e) {  
			}  
		}
		return retVal;
	}

//	encrypt[pwd+byte(id_len)+id]
	public static String activeIDByCard(String sessionid,byte[]content,byte[] key)
	{
		byte[] cont = des.des_decrypt(content,content.length, key);

		if(cont==null)
		{
			return ConfigRetVal.parse_err;
		}
		char[]crd_pwd =new char[16];

		if(cont.length<17)
		{
			return ConfigRetVal.format_notcorrect;
		}

		//提取出充值卡和密码
		for(int i=0;i<16;i++)
		{
			crd_pwd[i]=(char) cont[i];
		}

		int len = (int)cont[16];
		char cid[] = new char[len];

		if(cont.length<len+17)
		{
			return ConfigRetVal.format_notcorrect;
		}

		for(int t = 0;t < cid.length; t++)
		{
			cid[t]=(char) cont[t+17];
		}

		String sid = String.valueOf(cid);
		String spwd =String.valueOf(crd_pwd);
		Connection conn=SessionStateMgr.bean.getConn();	
		//查询充值卡信息
		String str ="select state as State, value as Value from recardinfo where pw=?";
		PreparedStatement stmt =null;
		ResultSet rs =null;
		String retVal = ConfigRetVal.not_find;
		try {
			stmt = conn.prepareStatement(str);
			stmt.setString(1, spwd);
			rs= stmt.executeQuery();

			//1.判断充值卡号是否有效
			if(rs.next()==true)//如果不为空，说明该充值卡有效
			{
				int state = rs.getInt("State");
				int value = rs.getInt("Value");

				switch(state)
				{
				case 1001://充值卡有效

					//检查输入的id是否有效
					str = "select state,enddate from accountinfo where id=?";
					stmt = conn.prepareStatement(str);
					stmt.setString(1, sid);
					rs = stmt.executeQuery();
					if(rs.next()==true)
					{
						int accState = rs.getInt(1);
						String ed=rs.getString(2);
						String fromToday = DateUtil.dataOperate(new Date(), value);
						String enddate="00000000";

						boolean canCharge = false;;
						switch(accState)
						{
						case 1004:
							enddate=fromToday;
							canCharge = true;
							break;
						case 1002:
							enddate=DateUtil.dataOperate(ed, value);
							canCharge = true;
							break;
						case 1001:
							canCharge = false;;
							retVal = ConfigRetVal.id_unactive;
							break;
						default:
							canCharge = false;
							retVal = ConfigRetVal.account_disable;
							break;
						}
						if(canCharge)
						{
							try{
								conn.setAutoCommit(false);
								//修改充值卡为使用状态
								String usedate=DateUtil.getStringDate("yyyyMMdd");
								str = "update recardinfo set state='1002',useaccid=? ,usedate=? where pw=?";
								stmt =conn.prepareStatement(str);
								stmt.setString(1, sid);
								stmt.setString(2, usedate);
								stmt.setString(3, spwd);
								int flag1=stmt.executeUpdate();


								str ="update accountinfo set enddate=?, state =1002 where id=?";
								stmt = conn.prepareStatement(str);
								stmt.setString(1, enddate);
								stmt.setString(2, sid);		
								int flag2= stmt.executeUpdate();

								conn.commit();
								if(flag1>0 && flag2>0)//修改充值卡信息成功
								{
									retVal=ConfigRetVal.id_valid;
								}
								else
								{
									retVal=ConfigRetVal.server_err;//更新数据库时出错
								}
							}
							catch(SQLException e)
							{
								conn.rollback();
								retVal = ConfigRetVal.server_err;
							}
							finally
							{
								conn.setAutoCommit(true);
							}

						}//end of canchange
					}//end of id 存在
					else// id 不存在
					{
						try
						{	
							conn.setAutoCommit(false);

							String usedate=DateUtil.getStringDate("yyyyMMdd");
							str = "update recardinfo set state='1002',useaccid=? ,usedate=? where pw=?";
							stmt =conn.prepareStatement(str);
							stmt.setString(1, sid);
							stmt.setString(2, usedate);
							stmt.setString(3, spwd);
							int flag1=stmt.executeUpdate();

							String endate = DateUtil.dataOperate(new Date(), value);
							str ="insert into accountinfo(id,state,bindday,enddate) values(?,?,?,?)";
							stmt = conn.prepareStatement(str);
							stmt.setString(1, sid);
							stmt.setInt(2, 1002);
							stmt.setInt(3, value);
							stmt.setString(4, endate);
							int flag2= stmt.executeUpdate();
							conn.commit();

							if(flag1>0 && flag2>0)//修改充值卡信息成功
							{
								retVal=ConfigRetVal.id_valid;

								synchronized (SessionStateMgr.sessionmap)
								{
									SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
								}
							}
							else
							{
								retVal=ConfigRetVal.server_err;//更新数据库时出错
							}
						}catch(SQLException e)
						{
							retVal = ConfigRetVal.server_err;
							conn.rollback();
							e.printStackTrace();
							return retVal;
						}
						finally
						{
							conn.setAutoCommit(true);
						}
					}// end of id 不存在
					break;
				case 1002:
					retVal = ConfigRetVal.card_invalid;
					break;
				case 1003:
					retVal = ConfigRetVal.card_disable;
					break;
				default:
					retVal=ConfigRetVal.not_find;
				}

			}else//查询充值卡返回空
			{
				retVal=ConfigRetVal.card_invalid;
			}
			return retVal;
		}
		catch (Exception e)
		{
			SessionStateMgr.logger.error(e.getMessage()+"activeIDByCard  sessionid:"+sessionid+"   sid:"+sid+
					"   pwd:"+spwd,e);
			e.printStackTrace();
			return ConfigRetVal.server_err;
		}
		finally
		{
			try { 
				if(rs != null)  
					rs.close();  
				if (stmt != null)  
					stmt.close();  
				if (conn != null)  
					conn.close();  
			} catch (Exception e) {
			}  
		}
	}


	/**
	 * 更新连接状态
	 * @param sessionid 
	 * 
	 */
	public static void updateSIgnTime(String sessionId)
	{
		//更新sessionid对应内存数据中的lastsigntime

		synchronized (SessionStateMgr.sessionmap)
		{
			Session session = SessionStateMgr.sessionmap.get(sessionId);
			if(session!=null)
			{
				session.setLastsigntime(new Date().getTime());
			}
		}
	}

	/**
	 *函数功能：获取账户状态
	 * @param sessionid 
	 * @param en_id 加密状态的客户端id
	 * @param key    解密密钥
	 * @return 200+state_len+state+bindday_len+bindday+enddate_lem+enddate
	 */
	
	public static String getAccountInfo(String sessionid,byte en_id[],byte[] key)
	{
		byte[] id = des.des_decrypt(en_id,en_id.length, key);
		//解密失败
		if(id==null)
		{
			return ConfigRetVal.parse_err;
		}

		int len = (int)id[0];
		char cid[] = new char[len];

		if(id.length<len+1)
		{
			return ConfigRetVal.format_notcorrect;
		}

		for(int t = 0;t < cid.length; t++)
		{
			cid[t]=(char) id[t+1];
		}
		String sid = String.valueOf(cid);

		String retVal=null;
		
		Connection conn=SessionStateMgr.bean.getConn();	
		String str ="select state,bindday,enddate from accountinfo where id=?";
		PreparedStatement stmt =null;
		ResultSet rs =null;
		StringBuilder builder = new StringBuilder();
		String info[];
		try {
			stmt = conn.prepareStatement(str);
			stmt.setString(1, sid);
			rs= stmt.executeQuery();

			if(rs.next()==true)
			{
				int State = rs.getInt(1);
				int bidday = rs.getInt(2);
				String enddate = rs.getString(3);
				builder.append(State).append("#").append(bidday).append("#").append(enddate);
				
				info = builder.toString().split("#");
				retVal = ConfigRetVal.id_valid;
				String s=new String(PacketUtil.combPakcet(info));
				retVal+=s;
				return retVal;
			}
			else
			{
				retVal = ConfigRetVal.not_find;
				return retVal;
			}
		}catch (Exception e)
		{
			retVal = ConfigRetVal.not_find;
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
			
			return retVal;
		}
		finally
		{
			try{
				if(conn!=null)
				{
					conn.close();
				}
				if(stmt!=null)
				{
					stmt.close();
				}
				if(rs!=null)
				{
					rs.close();
				}
			}catch(Exception e)
			{
			}
		}
	}
	
	public static String[] getVideoList(String sessionid)
	{
		Session session =null;
		synchronized (SessionStateMgr.sessionmap)
		{
			if((session=(SessionStateMgr.sessionmap.get(sessionid)))==null)
			{
				return null;
			}
			else
			{
				if(session.getUid()==null)
				{
					return null;
				}
			}
		}
		Connection conn=SessionStateMgr.bean.getConn();		
		String str ="select streamname from streaminfo";
		Statement stmt =null;
		ResultSet rs =null;

		StringBuilder builder = new StringBuilder();
		builder.append(Config.SERVER_URL).append("#");
		String retVal[];
		try {
			stmt = conn.createStatement();
			rs= stmt.executeQuery(str);
			while(rs.next())
			{
				builder.append(rs.getString(1)).append("#");
			}
			retVal = builder.toString().split("#");
			return retVal;
		}catch (Exception e)
		{
			retVal= null;
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
			return retVal;
		}
		finally
		{
			try{
				if(conn!=null)
				{
					conn.close();
				}
				if(stmt!=null)
				{
					stmt.close();
				}
				if(rs!=null)
				{
					rs.close();
				}
			}catch(Exception e)
			{
			}
		}
	}
}
