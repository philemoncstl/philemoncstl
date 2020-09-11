package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.apache.log4j.Logger;

import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.i18n.PayButton;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;

public class PostStep1SelectPayment extends CommonPanelOctopus{
	private static final Logger logger = Logger.getLogger(PostStep1SelectPayment.class);
	
	private I18nLabel lblInst;
	private CpPanel pnlCp;
		
	private I18nButtonLabel btnBack;
	
	private PayButton[] paymentBtns;

	public PostStep1SelectPayment(CtrlPanel pnlCtrl) {
		super(pnlCtrl);

		setLayout(null);
		
//		lblInst = createButton("selectPaymentMethodInst", "img/msg_box.png", 270, 380, 744, 184);
		lblInst = createButton("selectPaymentMethodInst", "img/msg_box_L.png", 215, 260, 861, 135);
		lblInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblInst, Font.PLAIN, 30);
		add(lblInst);
		
		boolean contactlessDisabled = Step1SelectTime.isContactlessDisabled();
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object src = e.getSource();
				if(src == btnBack) {
					pnlCtrl.goToHome();
				}
				else if(src instanceof PayButton) {
					PayButton pb = (PayButton) src;
					PayMethod pm = pb.getPmModel().getPayMethod();
					logger.info("PayMethod pressed:" + pm);
					
					//check connection status
					boolean checkConn = pnlCtrl.requestCpConnectionCheck();
					if(!checkConn) {
						pnlCtrl.showErrorMessage("ERR9100", pnlCtrl.getPnlSelectedCp().getCp().getCpNo());
						pnlCtrl.getPnlSelectedCp().setCpConnected(false);	//2018-03-23
						pnlCtrl.getPnlSelectedCp().repaint();
						return;
					}
					
					if(Step1SelectTime.isContactless(src)) {
						if(contactlessDisabled) {
							logger.warn("Contactless disabled");
						}
						else {
							startCharging(PayMethod.Contactless);
						}
					}
					else if(pm == PayMethod.Octopus){
						startCharging(PayMethod.Octopus);
					}
					else if(pm == PayMethod.QR) {
						startCharging(pm);
					}
					else {
						logger.warn("Pay method:" + pm + " not supported");
					}
				}
				
			}
		};
		
		btnBack = createButton("back", "img/btn_back.png", 180, 682);
		add(btnBack);
		
		//20190623, set to disable style
		/*
		 * btnStartChargingContactless = createButton("contactless",
		 * "img/payment_contactless.png", 740, 682, contactlessDisabled ? Color.GRAY :
		 * Color.WHITE, 1); add(btnStartChargingContactless);
		 * 
		 * btnStartChargingOctopus = createButton("octopus", "img/payment_octopus.png",
		 * 990, 682, Color.WHITE, 1); //20190623, remove icon
		 * add(btnStartChargingOctopus);
		 */
		
		paymentBtns = getPayButtons();
		for(PayButton pb : paymentBtns) {
			add(pb);
			pb.addMouseListener(ma);
		}
	
		btnBack.addMouseListener(ma);
	}
	
	private void startCharging(PayMethod payMethod) {
		logger.info("start charging, payMethod:" + payMethod);
		pnlCtrl.setPayMethod(payMethod);
		if(PrinterUtil.isOnline()) {
			pnlCtrl.goToPostStep2ProcessPayment();
		}
		else {
			pnlCtrl.goToStep2aReceiptNotAvailable();
		}
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		pnlCp = cp;
		
		HelpPanel.setRateInfo(lblInst);
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "selectPaymentMethod";
	}
}
