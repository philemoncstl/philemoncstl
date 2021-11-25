package hk.com.evpay.ct.job;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import hk.com.cstl.evcs.ct.IucEventLogDto;
import hk.com.evpay.ct.CtrlPanel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.iUC285Util;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class IUCSettlementJob  implements Job{
	private static final Logger logger = Logger.getLogger(IUCSettlementJob.class);
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		logger.info("Starting IUCSettlementJob......");
		while(!CtrlPanel.ishomePage()) {
			logger.info("Ct is not in home page wait 3 mins to retry....");
			try {
				TimeUnit.MINUTES.sleep(3);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
		logger.info("Ct in home page start doSettlement...");
		CtrlPanel.goToErrorPage("cpStatusUnavailable");
		JSONObject response = null;
		response = iUC285Util.doSettlement("EDC");
		boolean hideContactlessButton = false;
		if (response != null && response.has("BATCH")) {
			JSONArray c = response.getJSONArray("BATCH");
			for(int i = 0 ; i < c.length(); i++) {
				 JSONObject obj = c.getJSONObject(i);
				 if("CommError".equals(obj.getString("STATUS")) || "doTranBad".equals(obj.getString("STATUS"))) {
					CtrlPanel.hideContactlessButton();
					break;
				 }
			}
			if(hideContactlessButton) {
				logger.info("hideContactlessButton");
				hideContactlessButton = true;
			} else {
				logger.info("showContactlessButton");
				CtrlPanel.showContactlessButton();
			}
		} else {
			logger.info("hideContactlessButton");
			CtrlPanel.hideContactlessButton();
		}
		logger.info("Completed IUCSettlementJob");
		CtrlPanel.goToHomePage();
	}
}
