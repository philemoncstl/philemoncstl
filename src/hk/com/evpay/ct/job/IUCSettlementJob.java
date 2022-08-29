package hk.com.evpay.ct.job;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import hk.com.evpay.ct.CtrlPanel;
import hk.com.evpay.ct.util.iUC285Util;

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
		int retryCount = 0;
		while(retryCount < 10) {
			if(startIUCSettlement()) {
				break;
			}
			retryCount ++;
			try {
				TimeUnit.MINUTES.sleep(30);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}

	}
	
	private boolean startIUCSettlement(){
		logger.info("Ct in home page start doSettlement...");
		boolean isApproved = true;
		try {
			CtrlPanel.goToErrorPage("cpStatusUnavailable");
			JSONObject response = null;
			response = iUC285Util.doSettlement("EDC");
			boolean hideContactlessButton = false;
			if (response != null && response.has("BATCH")) {
				JSONArray c = response.getJSONArray("BATCH");
				for(int i = 0 ; i < c.length(); i++) {
					 JSONObject obj = c.getJSONObject(i);
					 if(!"APPROVED".equals(obj.getString("STATUS"))) {
						 isApproved = false;
						break;
					 }
				}
				CtrlPanel.hideContactlessButton();
				if(hideContactlessButton) {
					logger.info("hideContactlessButton");
					hideContactlessButton = true;
				} else {
					logger.info("showContactlessButton");
					CtrlPanel.showContactlessButton();
				}
			} else {
				isApproved = false;
				logger.info("response == null || response do not has BATCH hideContactlessButton");
				CtrlPanel.hideContactlessButton();
			}
		}  catch (Exception e) {
			logger.error("Failed to startIUCSettlement:" + e.getMessage(), e);
			isApproved = false;
		}
		logger.info("Completed IUCSettlementJob");
		CtrlPanel.goToHomePage();
		return isApproved;
	}
}
