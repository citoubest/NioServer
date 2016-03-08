package com.nku.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
	
	public static String getStringDate(String format)
	{
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		return sdf.format(date);
	}
	
	
	public static String dataOperate(String olddate,int days)
	{
		int year =Integer.parseInt( olddate.substring(0, 4));
		int month=Integer.parseInt( olddate.substring(4, 6));
		int day =Integer.parseInt( olddate.substring(6, 8));

		return	dataAdd(year,month,day,days);
		
	}
	@SuppressWarnings("deprecation")
	public static String dataOperate(Date date,int days)
	{
		int year = date.getYear()+1900;
		int month=date.getMonth()+1;
		int day =date.getDate();
		
		return	dataAdd(year,month,day,days);
	}
	
	private static  String dataAdd(int year,int month,int day,int days)
	{
		if(days==183)
		{
			if(month<7)
			{
				month+=6;
			}
			else
			{
				month=month+6-12;
				year+=1;
			}
		}
		else if(days==365)
		{
			year+=1;
		}
		StringBuilder builder = new StringBuilder();
		
		String y=new DecimalFormat("0000").format(year);
		builder.append(y);
		
		String m=new DecimalFormat("00").format(month);
		builder.append(m);
		
		String d=new DecimalFormat("00").format(day);
		builder.append(d);

		return builder.toString();
	}
}
