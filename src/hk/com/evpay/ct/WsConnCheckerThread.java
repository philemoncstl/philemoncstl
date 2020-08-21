package hk.com.evpay.ct;

import org.apache.log4j.Logger;

import hk.com.cstl.evcs.ocpp.CpWebSocket;

public class WsConnCheckerThread extends Thread{
	private static final Logger logger = Logger.getLogger(WsConnCheckerThread.class);
	private boolean cont = true;
	private long checkInterval = 1000;
	
	private CtrlPanel ctrl;
	
	public WsConnCheckerThread(CtrlPanel ctrl) {
		this.ctrl = ctrl;
		
	}
	
	public WsConnCheckerThread(CtrlPanel ctrl, long checkInterval) {
		this.ctrl = ctrl;
		this.checkInterval = checkInterval;
	}

	@Override
	public void run() {
		int count = 1;
		while(cont) {
			try {
				//check CP <-> CT
				for(CpPanel cp : ctrl.getPnlCpList().getCpList()) {
					if(cp.getCp() == null) {
						continue;
					}
					cp.checkCpConnection();
					//logger.info("Checking CP:" + cp.getCp().getCpNo() + ", conn:" + cp.isCpConnected());
					
					//CK @ 20180329, trigger status notification every 60 seconds
					if(count % 60 == 0 && cp.isCpConnected()) {
						logger.info(cp.getCp().getCpNo() + " request status notification");
						CpWebSocket.requestStatusNotification(cp.getCpEp());
					}
				}
			} catch(Exception e) {
				logger.error("Failed to check ws conn status:" + e.getMessage(), e);
			} finally {
				count ++;
				try {
					Thread.sleep(checkInterval);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public boolean isCont() {
		return cont;
	}

	public void setCont(boolean cont) {
		this.cont = cont;
	}

	public long getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}
	
	
}
