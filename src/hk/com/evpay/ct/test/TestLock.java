package hk.com.evpay.ct.test;

import org.apache.log4j.Logger;

import com.ckzone.util.LogUtil;

public class TestLock {
	private static final Logger logger = Logger.getLogger(TestLock.class);
	
	public static void main(String[] args) {
		LogUtil.initLogger();
		TestLock t = new TestLock();
		t.testNow();
	}
	private String text = "abc";
	
	
	public void testNow() {
		Thread t = new Thread() {
			public void run() {
				logger.info(getName() + " running");
				synchronized(text) {
					logger.info(getName() + " sleeping");
					try {
						Thread.sleep(10000);
						text = "aaa";
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					logger.info(getName() + " wake up");
				}
				logger.info(getName() + " ended");
			};
		};
		t.start();
		
		Thread t2 = new Thread() {
			
			public void run() {
				logger.info(getName() + " running");
				try {
					text = new String("bbb");
					Thread.sleep(1000);
					logger.info(getName() + ", val:" + getVal());
				} catch (Exception e) {
				}
				logger.info(getName() + " ended");
			};
		};
		t2.start();
	}
	
	public String getVal() {
		return this.text;
	}
}
