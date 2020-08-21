package hk.com.evpay.ct;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.util.CtUtil;


public class GoHomeCountDownThread extends Thread{
	private static final Logger logger = Logger.getLogger(GoHomeCountDownThread.class);
	
	private CtrlPanel pnlCtrl;
	private JComponent requestComponent;
	private long timeoutMs;
	
	public GoHomeCountDownThread(CtrlPanel pnlCtrl, JComponent requestComponent) {
		this.pnlCtrl = pnlCtrl;
		this.requestComponent = requestComponent;
		this.timeoutMs = System.currentTimeMillis() + (CtUtil.getConfig().getGoHomeDelaySec() * 1000);
	}
	
	@Override
	public void run() {
		logger.info(getName() + " started");
		
		while(timeoutMs >= System.currentTimeMillis()) {
			try {
				Thread.sleep(50);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			
			if(requestComponent == null || !requestComponent.isShowing()) {
				logger.info(getName() + " ended, active screen changed");
				return;
			}
			
			if(pnlCtrl.getCountDownThead() != this) {
				logger.info(getName() + " ended, count down thread changed");
				return;
			}
		}
		
		if(requestComponent != null && requestComponent.isShowing()) {
			logger.info(getName() + ", matched");
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					pnlCtrl.goToHome();
				}
			});
			
		}
		else {
			logger.debug(getName() + "ended, go home ignored");
		}
	}
	
	public void checkAndExtendTimeout(long timeMs) {
		long tm = timeoutMs - System.currentTimeMillis();
		if(tm > timeMs) {
			logger.info("No need to extend. Timeout after " + tm + " ms.");
		}
		else {
			timeoutMs = System.currentTimeMillis() + timeMs;
			logger.info("Timeout extend: " + tm + " ms.");
		}
	}
}
