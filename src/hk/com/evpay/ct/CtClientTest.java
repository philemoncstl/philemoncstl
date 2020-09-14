package hk.com.evpay.ct;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ckzone.util.DateUtil;
import com.ckzone.util.GsonUtil;

import hk.com.cstl.evcs.wsobj.WsAction;

public class CtClientTest{
	public static void main(String[] args) {
		 System.out.println("****" + new Timestamp(System.currentTimeMillis()));
		 System.out.println("****" + new Date());
		 System.out.println("****" + new Timestamp(System.currentTimeMillis()));
		 System.out.println("****" + GsonUtil.toJson(new Timestamp(System.currentTimeMillis())));
		 
		 //WsAction.DisableCp
		 String s = "DisableCp";
		 WsAction as = WsAction.valueOf(s);
		 System.out.println(as);
		 s = "DisableCp";
		 as = WsAction.valueOf(s);
		 System.out.println(as);
		 SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSSZ");
		 //format.setTimeZone(TimeZone.getTimeZone("GMT"));
		 System.out.println(format.format(new Date()));
		 
		 
		 Date d = new Date();
		 System.out.println("d:" + d);
		 s = DateUtil.formatTimestamp(d);
		 System.out.println("s:" + s);
		 Date d2 = DateUtil.parseTimestamp(s);
		 System.out.println("d2:" + d2);
		 System.out.println(d.equals(d2));
	}
}
