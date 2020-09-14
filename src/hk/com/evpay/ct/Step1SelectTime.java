package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.ckzone.util.StringUtil;

import hk.com.cstl.evcs.model.EvCons;
import hk.com.cstl.evcs.model.RateModel;
import hk.com.cstl.evcs.model.ServConfig;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.i18n.PayButton;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;
import hk.com.evpay.ct.util.RateUtil;

public class Step1SelectTime extends CommonPanel{
	private static Logger logger = Logger.getLogger(Step1SelectTime.class);
	
	private I18nLabel lblTime;
	private I18nLabel lblTimeBg;
	private I18nButtonLabel btnAddPeriod;
	private I18nButtonLabel btnMinusPeriod;
	private I18nLabel lblAmount;
	
	private I18nButtonLabel btnBack;
	
	private PayButton[] paymentBtns;
	
	
	private TranModel tran;
	private int chargingUnit;
	

	public Step1SelectTime(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		btnAddPeriod = createButton("img/period_add_sm.png", 590, 282, 43, 43);
		add(btnAddPeriod);
		btnMinusPeriod = createButton("img/period_minus_sm.png", 590, 330, 43, 43);
		add(btnMinusPeriod);
		
		lblTime = createButton("chargingTimeVal", "", 320, 270, 403, 116);
		LangUtil.setFont(lblTime, Font.PLAIN, 40);
		lblTime.setParms(new String[] {"1", "0"});
		add(lblTime);
		
		lblTimeBg = createButton("", "img/charging_time.png", 270, 270, 403, 116);
		add(lblTimeBg);
		
		lblAmount = createButton("hkdWithVal2", "img/charging_amount_box.png", 710, 261, 341, 135);
		lblAmount.setParms("96.00");
		lblAmount.setForeground(Color.WHITE);
		LangUtil.setFont(lblAmount, Font.PLAIN, 54);
		add(lblAmount);
		
		
		btnBack = createButton("back", "img/btn_back.png", 180, 682);
		add(btnBack);
		
		//20190623, set to disable style
		
		
		boolean contactlessDisabled = isContactlessDisabled();
		 
		paymentBtns = getPayButtons();
		for(PayButton pb : paymentBtns) {
			add(pb);
		}		
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object src = e.getSource();
				if(src == btnAddPeriod) {
					addPeriod();
				}
				else if(src == btnMinusPeriod) {
					minusPeriod();
				}
				else if(src == btnBack) {
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
					
					if(isContactless(src)) {
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
		btnAddPeriod.addMouseListener(ma);
		btnMinusPeriod.addMouseListener(ma);
		btnBack.addMouseListener(ma);
		
		for(int i = 0; i < paymentBtns.length; i ++) {
			paymentBtns[i].addMouseListener(ma);
		}
	}
	
	public static boolean isContactless(Object src) {
		if(src instanceof PayButton) {
			PayMethod pm = ((PayButton)src).getPmModel().getPayMethod();
			switch(pm) {
				case Contactless:
				case VisaPayWave:
				case MasterPaypass:
				case ApplePay:
				case GooglePay:
				case SamsungPay:
					return true;
				
			}
		}
		
		return false;
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		tran = new TranModel();
		tran.setMode(EvCons.MODE_PREPAID);
		RateUtil.setTimeEnergyMode(tran);
		tran.setCpNo(cp.getCp().getCpNo());
		cp.getCp().setTran(tran);
		tran.setIdTag(UUID.randomUUID().toString());
		logger.info("Prepared Tran:" + tran.getIdTag());
		
		ServConfig sc = CtUtil.getServConfig();
		chargingUnit = sc.getDefaultChargingUnit();
		if(chargingUnit < sc.getMinChargingUnit()) {
			chargingUnit = sc.getMinChargingUnit();
		}
		
		refreshChargingDuration();
	}
	
	private void refreshChargingDuration() {	
		RateModel rate = RateUtil.getRate();
		int durationMin = chargingUnit * rate.getMins();
		tran.setDurationMin(durationMin);
		
		Calendar cal = Calendar.getInstance();
		tran.setStartDttm(cal.getTime());
		tran.setTranDttm(tran.getStartDttm());
		cal.add(Calendar.MINUTE, durationMin);
		tran.setEndDttm(cal.getTime());
		RateUtil.calcChargingFee(tran);
		
		int hour = durationMin / 60;
		int min = durationMin % 60;
		lblTime.setParms(hour, min);
		
		lblAmount.setParms(CtUtil.bigDecimalToString(tran.getAmt()));
	}
	
	private void addPeriod() {
		logger.info("add period");
		
		chargingUnit ++;
		if(chargingUnit > CtUtil.getServConfig().getMaxChargingUnit()) {
			chargingUnit --;
		}
		else {
			refreshChargingDuration();
		}
	}
	
	private void minusPeriod() {
		logger.info("minus period");
		
		chargingUnit --;
		if(chargingUnit < CtUtil.getServConfig().getMinChargingUnit()) {
			chargingUnit ++;
		}
		else {
			refreshChargingDuration();
		}
	}
	
	private void startCharging(PayMethod payMethod) {
		logger.info("start charging, payMethod:" + payMethod);
		pnlCtrl.setPayMethod(payMethod);
		if(PrinterUtil.isOnline()) {
			pnlCtrl.goToStep2ProcessPayment();
		}
		else {
			pnlCtrl.goToStep2aReceiptNotAvailable();
		}
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "selectChargingTime";
	}
	
	public static boolean isContactlessDisabled() {
		return StringUtil.isEmpty(CtUtil.getConfig().getContactlessDevice()) || "none".equals(CtUtil.getConfig().getContactlessDevice());
	}
}
