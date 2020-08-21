package hk.com.evpay.ct.tool;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;

public class CapScreenAction extends AbstractAction{
	private static final Logger logger = Logger.getLogger(CapScreenAction.class);
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("Cap screen started");
		try {
			Runtime.getRuntime().exec("/home/ct/evpay/script/cap.sh");
		} catch (Exception e2) {
			logger.error("Failed to cap screen:" + e2.getMessage(), e2);
		}
		logger.info("Cap screen ended");
	}

}
