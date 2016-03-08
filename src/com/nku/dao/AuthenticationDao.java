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
	 * @���룺sessionid, id
	 * 
	 * @�����200����Ч��δ�������Ӵ�����
	 *        201����Ч���������Ӵ�����
	 *        404����Ч
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
		boolean flag = false;//�ж����session�Ƿ���֤ͨ��
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
				//����֤ͨ����
				if(sid.equals(session.getUid()))
				{
					//�������
					if(session.getState()==1)
					{
						count++;
					}

				}
			}
		}
		//��֤ͨ�����һ�û���Ը�id���Ź��������Ը�id���Ź��������ڷ�Χ��
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
	 * @���룺sessionid ��encrypt(id)��������Կ
	 * @�����200 id��Ч
	 * 		  401 �˻�δ����
	 *        402 �˻�Ƿ��
	 *        403 ��������δ�ܽ���������������
	 *        404 δ�ҵ���Ӧid���˻�
	 *        406 �˻��Ѿ�����
	 *        501 �������ڲ�����
	 * 
	 * */
	public static String isIDValid(String sessionid,byte en_id[],byte[] key)
	{
		byte[] id = des.des_decrypt(en_id,en_id.length, key);
		//����ʧ��
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
				case 1002://��Ч(����״̬)
					retVal=ConfigRetVal.id_valid;

					synchronized (SessionStateMgr.sessionmap)
					{
						SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
					}
					break;
				case 1001://δ����
					retVal=ConfigRetVal.id_unactive;
					break;
				case 1003:
					retVal = ConfigRetVal.account_disable;
					break;
				case 1004://Ƿ��
					retVal=ConfigRetVal.account_arrears;
					break;
				default:
					retVal=ConfigRetVal.not_find;
				}
			}
			else
			{
				retVal=ConfigRetVal.not_find;//û���ҵ���id����Ϣ
			}
		} catch (SQLException e) {
			SessionStateMgr.logger.error(e.getMessage()+"isIDValid  sessionid:"+sessionid+"   id"+sid,e);
			e.printStackTrace();
			retVal=ConfigRetVal.server_err;//�������ڲ�����
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
	 * @���룺sessidid��encrypt(id)�� ��Կ
	 * @�����
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

			//1.�ж�id�Ƿ���Ч����Ч��ȡ���˻���ʱ��
			if(rs.next()==true)//�����Ϊ�գ�˵����id��Ч
			{
				int state = rs.getInt("State");
				int value = rs.getInt("Value");

				switch(state)
				{
				case 1001://δ����	
					String enddate = DateUtil.dataOperate(new Date(), value);
					str ="update accountinfo set state='1002',enddate=? where id=? and state=1001";
					stmt = conn.prepareStatement(str);
					stmt.setString(1, enddate);
					stmt.setString(2, sid);

					int flag = stmt.executeUpdate();
					if(flag>0)//�ɹ�����
					{
						retVal="200";
						synchronized (SessionStateMgr.sessionmap)
						{
							SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
						}
					}
					else if(flag==0)//δ�ҵ���ӦID
					{
						retVal=ConfigRetVal.not_find;
					}
					else
					{
						retVal=ConfigRetVal.server_err;
					}
					break;
				case 1002://�Ѿ����ڼ���״̬
					retVal=ConfigRetVal.already_actived;
					break;
				case 1003:
					retVal = ConfigRetVal.account_disable;
					break;
				case 1004://Ƿ��
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

		//��ȡ����ֵ��������
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
		//��ѯ��ֵ����Ϣ
		String str ="select state as State, value as Value from recardinfo where pw=?";
		PreparedStatement stmt =null;
		ResultSet rs =null;
		String retVal = ConfigRetVal.not_find;
		try {
			stmt = conn.prepareStatement(str);
			stmt.setString(1, spwd);
			rs= stmt.executeQuery();

			//1.�жϳ�ֵ�����Ƿ���Ч
			if(rs.next()==true)//�����Ϊ�գ�˵���ó�ֵ����Ч
			{
				int state = rs.getInt("State");
				int value = rs.getInt("Value");

				switch(state)
				{
				case 1001://��ֵ����Ч

					//��������id�Ƿ���Ч
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
								//�޸ĳ�ֵ��Ϊʹ��״̬
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
								if(flag1>0 && flag2>0)//�޸ĳ�ֵ����Ϣ�ɹ�
								{
									retVal=ConfigRetVal.id_valid;
								}
								else
								{
									retVal=ConfigRetVal.server_err;//�������ݿ�ʱ����
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
					}//end of id ����
					else// id ������
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

							if(flag1>0 && flag2>0)//�޸ĳ�ֵ����Ϣ�ɹ�
							{
								retVal=ConfigRetVal.id_valid;

								synchronized (SessionStateMgr.sessionmap)
								{
									SessionStateMgr.sessionmap.get(sessionid).setUid(sid);
								}
							}
							else
							{
								retVal=ConfigRetVal.server_err;//�������ݿ�ʱ����
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
					}// end of id ������
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

			}else//��ѯ��ֵ�����ؿ�
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
	 * ��������״̬
	 * @param sessionid 
	 * 
	 */
	public static void updateSIgnTime(String sessionId)
	{
		//����sessionid��Ӧ�ڴ������е�lastsigntime

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
	 *�������ܣ���ȡ�˻�״̬
	 * @param sessionid 
	 * @param en_id ����״̬�Ŀͻ���id
	 * @param key    ������Կ
	 * @return 200+state_len+state+bindday_len+bindday+enddate_lem+enddate
	 */
	
	public static String getAccountInfo(String sessionid,byte en_id[],byte[] key)
	{
		byte[] id = des.des_decrypt(en_id,en_id.length, key);
		//����ʧ��
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
