package hk.com.evpay.ct;

import org.apache.log4j.Logger;

public class GoHomeUnlockThread extends Thread{
	private static final Logger logger = Logger.getLogger(GoHomeUnlockThread.class);
	
	private CtrlPanel pnlCtrl;
	private long timeoutMs;

	public GoHomeUnlockThread(CtrlPanel pnlCtrl, long unlockTimeDelayMs) {
		this.pnlCtrl = pnlCtrl;
		this.timeoutMs = System.currentTimeMillis() + unlockTimeDelayMs;
	}
	
	@Override
	public void run() {
		logger.info(getName() + " started");
		
		while(timeoutMs > System.currentTimeMillis()) {			
			try {
				Thread.sleep(50);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
			if(pnlCtrl.getUnlockThread() != this) {
				logger.info(getName() + " ended, unlock cancelled");
				return;
			}
		}
		
		if(pnlCtrl.getUnlockThread() == this) {
			logger.info(getName() + " ended, unlock now");
			pnlCtrl.goHomeUnlock();
		}
	}
}
