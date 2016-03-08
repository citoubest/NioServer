package com.nku.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.TimerTask;
import com.nku.main.SessionStateMgr;
import com.nku.util.DateUtil;

/**
 * ���̸߳���ʱ������ݿ��еĳ�ֵ���Ƿ����,�˻��Ƿ�Ƿ�ѵ����ݿ�ά����Ϣ
 * 
 * */
public class DBMaintainThread extends TimerTask{

	@Override
	public void run() {

		//��ǰ����
		String currDay=DateUtil.getStringDate("yyyyMMdd");

		//������ʹ�õ��˻�����ΪǷ��״̬
		String sql1="update accountinfo set state=1004 where enddate < ? and state=1002";

		//��δʹ�õġ����ڵĳ�ֵ����Ϊ����״̬
		String sql2="update recardinfo set state=1003 where enddate < ? and state=1001";


		Connection conn = SessionStateMgr.bean.getConn();
		PreparedStatement stmt=null;

		try {
			stmt = conn.prepareStatement(sql1);
			stmt.setString(1, currDay);
			stmt.executeUpdate();


			stmt = conn.prepareStatement(sql2);
			stmt.setString(1, currDay);
			stmt.executeUpdate();
		}
		catch (Exception e) {
			SessionStateMgr.logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
		finally
		{
			try { 
				if (stmt != null)  
					stmt.close();  
				if (conn != null)  
					conn.close();  
			} catch (Exception e) { 
				e.printStackTrace();  
			}  
		}
	}

}
