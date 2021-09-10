package hk.com.evpay.ct.job;

import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.ckzone.util.GsonUtil;

import hk.com.cstl.evcs.ct.IucEventLog;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.iUC285Util;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class IUCSettlementJob  implements Job{
	private static final Logger logger = Logger.getLogger(IUCSettlementJob.class);
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		logger.info("Starting IUCSettlementJob......");
  		JSONObject response = iUC285Util.doSettlement("EDC");
  		IucEventLog log = new IucEventLog();
  		log.setEventDttm(new Date());
  		log.setCtId(CtUtil.getCt());
  		log.setEventType("Settlement");
  		log.setRemark(GsonUtil.toJson(response));
		boolean res = CtWebSocketClient.uploadIUCEvent(log);
		logger.info("Completed IUCSettlementJob: " + res);
	}
}
