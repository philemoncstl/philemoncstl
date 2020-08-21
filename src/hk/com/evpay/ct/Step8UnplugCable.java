package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.LangUtil;

public class Step8UnplugCable extends CommonPanel{
	private static final Logger logger = Logger.getLogger(Step8UnplugCable.class);
	
	private I18nLabel lblUnplugCableInst;

	public Step8UnplugCable(CtrlPanel pnlCtrl) {
		super(pnlCtrl);

		setLayout(null);
		
		lblUnplugCableInst = createButton("unplugChargingCable", "img/msg_box.png", 270, 380, 744, 184);
		lblUnplugCableInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblUnplugCableInst, Font.PLAIN, 32);
		add(lblUnplugCableInst);
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		/*new Thread() {
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				
				if(pnlCtrl.isCurrentDisplayingPanel(Step8UnplugCable.this)) {
					SwingUtilities.invokeLater(new Runnable() {			
						@Override
						public void run() {
							pnlCtrl.goToHome();
						}
					});
				}
			}
		}.start();*/
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "chargingRecord";
	}
}
