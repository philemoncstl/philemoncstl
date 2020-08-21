package hk.com.evpay.ct;

import java.awt.Font;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;

public class Step4TakeReceipt extends CommonPanel{
	private static final Logger logger = Logger.getLogger(Step4TakeReceipt.class);
	
	private I18nLabel lblGetReceipt;

	public Step4TakeReceipt(CtrlPanel pnlCtrl) {
		super(pnlCtrl);

		setLayout(null);
		lblGetReceipt = createLabel("takeReceipt", "", 400, 115, 485, 486);
		LangUtil.setFont(lblGetReceipt, Font.PLAIN, 48);
		add(lblGetReceipt);
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		new Thread() {
			public void run() {
				logger.info("Go to step 5 after " + CtUtil.getConfig().getTakeReceiptDelayMs() + " ms");
				try {
					Thread.sleep(CtUtil.getConfig().getTakeReceiptDelayMs());
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				
				if(pnlCtrl.isCurrentDisplayingPanel(Step4TakeReceipt.this)) {
					SwingUtilities.invokeLater(new Runnable() {			
						@Override
						public void run() {
							if(CtUtil.isModePrepaid(cp.getCp().getTran())) {
								pnlCtrl.goToStep5ChargingRecord();
							}
							else {
								//pnlCtrl.goToStep8UnplugCable();
								pnlCtrl.goToPostStep6ShowReceipt();
							}
						}
					});
				}
			}
		}.start();
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_RECEIPT;
	}
}
