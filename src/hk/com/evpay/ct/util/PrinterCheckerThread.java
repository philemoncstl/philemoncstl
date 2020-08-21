package hk.com.evpay.ct.util;

import org.apache.log4j.Logger;

public class PrinterCheckerThread extends Thread{
private static final Logger logger = Logger.getLogger(PrinterCheckerThread.class);
	
	private boolean cont = true;
	private long checkInterval;
	
	public PrinterCheckerThread() {
		this(5000);
	}
	
	public PrinterCheckerThread(long checkInterval) {
		this.checkInterval = checkInterval;
	}
	
	@Override
	public void run() {
		boolean online = false;
		do {
			try {
				online = PrinterUtil.isOnline();
				if(online) {
					try {
						logger.debug("Check after 3 mins");
						Thread.sleep(180000);
					} catch (Exception e1) {
					}
				}
			} catch (Throwable e) {
				logger.error("Failed to check printer status", e);				
			} finally {
				if(!online) {
					logger.debug("Check after " + this.checkInterval + " ms");
					try {
						Thread.sleep(this.checkInterval);
					} catch (Exception e1) {
					}
				}
			}
		} while(cont);
	}

	public long getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

	public boolean isCont() {
		return cont;
	}

	public void setCont(boolean cont) {
		this.cont = cont;
	}
}
