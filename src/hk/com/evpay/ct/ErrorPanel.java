package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;

import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.util.LangUtil;

public class ErrorPanel extends CommonPanel{
	private static Logger logger = Logger.getLogger(HelpPanel.class);
	
	private I18nButtonLabel lblMsg;
	
	public ErrorPanel(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		lblMsg = createButton("EMPTY", "img/msg_box.png", 300, 300, 744, 184);
		lblMsg.setHorizontalAlignment(SwingConstants.CENTER);
		lblMsg.setVerticalAlignment(SwingConstants.CENTER);
		lblMsg.setForeground(Color.WHITE);
		LangUtil.setFont(lblMsg, Font.PLAIN, 30);
		add(lblMsg);
	}

	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
	}
	
	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITHOUT_TITLE;
	}
	
	public I18nButtonLabel getLblMsg() {
		return lblMsg;
	}
}
