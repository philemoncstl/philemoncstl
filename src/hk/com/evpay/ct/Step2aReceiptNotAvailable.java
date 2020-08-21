package hk.com.evpay.ct;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;

public class Step2aReceiptNotAvailable extends CommonPanel{
	private static final Logger logger = Logger.getLogger(Step2aReceiptNotAvailable.class);
	
	private I18nLabel lblMsg;
	
	private I18nButtonLabel btnNo;
	private I18nButtonLabel btnYes;

	public Step2aReceiptNotAvailable(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		lblMsg = createLabel("noReceiptAvailable", "", 400, 115, 485, 486);
		LangUtil.setFont(lblMsg, Font.PLAIN, 40);
		add(lblMsg);
		
		btnNo = createButton("no", "img/btn_no.png", 180, 682);
		add(btnNo);
		
		btnYes = createButton("yes", "img/btn_yes.png", 990, 682);
		add(btnYes);
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object src = e.getSource();
				if(src == btnNo) {
					pnlCtrl.goToHome();
				}
				else if(src == btnYes) {
					if(CtUtil.isModePrepaid()) {
						pnlCtrl.goToStep2ProcessPayment();
					}
					else {
						pnlCtrl.goToPostStep2ProcessPayment();
					}
				}
			}
		};
		btnNo.addMouseListener(ma);
		btnYes.addMouseListener(ma);
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_RECEIPT;
	}
}
