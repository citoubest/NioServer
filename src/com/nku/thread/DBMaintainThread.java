package com.nku.thread;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.TimerTask;
import com.nku.main.SessionStateMgr;
import com.nku.util.DateUtil;

/**
 * 该线程负责定时检查数据库中的充值卡是否过期,账户是否欠费等数据库维护信息
 * 
 * */
public class DBMaintainThread extends TimerTask{

	@Override
	public void run() {

		//当前日期
		String currDay=DateUtil.getStringDate("yyyyMMdd");

		//将正在使用的账户，置为欠费状态
		String sql1="update accountinfo set state=1004 where enddate < ? and state=1002";

		//将未使用的、过期的充值卡置为作废状态
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
