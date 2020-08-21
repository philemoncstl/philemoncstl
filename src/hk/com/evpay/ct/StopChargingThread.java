package hk.com.evpay.ct;

import java.util.Date;

import org.apache.log4j.Logger;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class StopChargingThread extends Thread{
	private static final Logger logger = Logger.getLogger(StopChargingThread.class);
	private CpPanel pnlCp;

	public StopChargingThread(CpPanel pnlCp) {
		super();
		this.pnlCp = pnlCp;
	}
	
	@Override
	public void run() {
		TranModel tm = this.pnlCp.getCp().getTran();
		
		if(CtUtil.isTranStatusCharging(tm)) {
			long timeout = Math.max(2000, tm.getEndDttm().getTime() - System.currentTimeMillis());
			logger.info(getName() + " started, tran:" + tm.getTranId() + ", CP:" + tm.getCpNo() + ", stop charging will be triggered after " + (timeout / 1000) + " seconds.");
			try {
				Thread.sleep(timeout);
			} catch (Exception e) {
			}
			
			logger.info(getName() + " started, tran:" + tm.getTranId() + ", CP:" + tm.getCpNo() + "Charging expired.");
			//CK @ 2018-02-23, as suggested by Victor,  remoteStopTransaction will be called after user present the card.
			/*if(CtUtil.isTranStatusStop(tm)) {
				logger.info(getName() + ", tran " + tm.getTranId() + " already stopped.");
				return;
			}
			
			this.pnlCp.remoteStopTransaction();
			
			long expiredTm = System.currentTimeMillis() + CtUtil.getConfig().getRemoteStartStopTimeCheckMs();	//10 seconds
			while(expiredTm > System.currentTimeMillis()) {
				if(CtUtil.isCpStatusStopped(pnlCp.getCp().getStatus())) {
					logger.info(getName() + ", tran stopped:" + tm.getTranId());
					tm.setActualEndDttm(new Date());
					tm.setTranStatusCode(String.valueOf(TranStatus.Stopped));
					CtWebSocketClient.uploadTran(tm);
					
					CtrlPanel.updateUi(pnlCp);					
					
					//save the CT
					CtUtil.saveCurrentCt();
					break;
				}
				
				try {
					Thread.sleep(20);
				} catch (Exception e) {
				}
			}*/
			logger.info(getName() + " stopped, tran:" + tm.getTranId() + ", status:" + tm.getTranStatusCode() + ", CP:" + tm.getCpNo());
		}
		else {
			logger.warn("Tran already stopped:" + tm.getIdTag() + ", cp:" + tm.getCpNo());
		}
	}
}
