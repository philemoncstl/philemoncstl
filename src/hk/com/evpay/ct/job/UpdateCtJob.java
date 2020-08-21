package hk.com.evpay.ct.job;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import hk.com.evpay.ct.ws.CtWebSocketClient;

public class UpdateCtJob implements Job{
	private static final Logger logger = Logger.getLogger(UpdateCtJob.class);
	
	@Override
	public void execute(JobExecutionContext ec) throws JobExecutionException {
		logger.info("Started");
		
		boolean res = CtWebSocketClient.updateCt();
		
		logger.info("Completed:" + res);
	}

}
