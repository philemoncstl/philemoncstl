package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ckzone.octopus.ExtraInfo;
import com.ckzone.octopus.PollDeductReturn;
import com.ckzone.octopus.PollEx;
import com.ckzone.octopus.util.OctCheckerThread;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.util.StringUtil;

import hk.com.evpay.ct.util.iUC285Util;
import hk.com.evpay.ct.util.iUC285Util.Status;

import hk.com.cstl.evcs.model.EvCons;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;
import hk.com.evpay.ct.util.RateUtil;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class PostStep2ProcessPayment extends CommonPanelOctopus{
	private static final Logger logger = Logger.getLogger(PostStep2ProcessPayment.class);
	
	private I18nLabel lblInst;
	private CpPanel pnlCp;
	
	private TranModel tran;
	
	private Thread payThread;


	public PostStep2ProcessPayment(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		lblInst = createButton("startChargingInstPostpaid", "img/msg_box.png", 270, 380, 744, 184);
		lblInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblInst, Font.PLAIN, 32);
		add(lblInst);
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "";
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		this.pnlCp = cp;
		
		tran = new TranModel();
		tran.setMode(EvCons.MODE_POSTPAID);
		RateUtil.setTimeEnergyMode(tran);
		tran.setCpNo(cp.getCp().getCpNo());
		tran.setIdTag(UUID.randomUUID().toString());
		logger.info("Prepared Tran:" + tran.getIdTag());
		
		//for test only
		/*if(CtUtil.getConfig().isDebugUi()) {
			tran.setMeterStart(new BigDecimal(1000));
			tran.setMeterStop(new BigDecimal(21000));
		}*/
		tran.setDurationFreeMin(0);
		if(CtUtil.getServConfig().getFreeTimeUnit() != null && CtUtil.getServConfig().getFreeTimeUnit() >0) {
			tran.setDurationFreeMin(CtUtil.getServConfig().getFreeTimeUnit() * RateUtil.getRate().getMins());
		}
		
		if(pnlCtrl.getPayMethod() == PayMethod.ContactlessGroup) {
			lblInst.setMsgCode("payInstContactless");
			handlePaymentContactless();
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.Octopus){
			lblInst.setMsgCode("startChargingInstPostpaid");
			handlePaymentOctopus();
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.QR){
			lblInst.setMsgCode("startChargingInstPostpaidQR");
			handlePaymentQR();
		}
		
	}
	
	private void handlePaymentQR() {
		new Thread() {
			@Override
			public void run() {
				long ttm = System.currentTimeMillis();
				CtrlPanel.QR_THREAD_MS = ttm;

				String qr = Step2ProcessPayment.detectQr(ttm, tran, pnlCtrl);
				logger.info(Thread.currentThread().getName() + " - detectQr ended, qr:" + qr);
				if(CtrlPanel.isSameQrThread(ttm)) {
					if(StringUtil.isEmpty(qr)) {
						logger.info("No QR scanned, go home now.");
						pnlCtrl.goToHome();
					}
					else {
						logger.info("Use qr:" + qr);
						setReceiptNo(tran);
						cardDetected();
						logger.info("Receipt:" + tran.getReceiptNo() + ", qr:" + tran.getCardNo());
					}
				}
				else {
					logger.info("Not same qr thread.");
				}
			}
		}.start();		
	}
	
	private void handlePaymentContactless() {
		new Thread() {
			@Override
			public void run() {
				Status responseStatus 	= null;
				JSONObject response 	= null;
				response 		= iUC285Util.doCardRead();
				responseStatus 	= iUC285Util.getStatus(response);
				if(responseStatus == Status.Approved) {

					logger.info("Contactless read card success");
					tran.setCardType(response.getString("CARD"));
					tran.setCardHash(response.getString("CARDHASH"));
					tran.setCardNo(response.getString("PAN"));
					setReceiptNo(tran);
//					Step2ProcessPayment.setDummyCardInfoContactless(tran);
					cardDetected();
				} else {
					logger.info("Contactless read card fail");
					pnlCtrl.showErrorMessage(responseStatus.toString());
					try {
						sleep(2000);
					} catch (InterruptedException e) {
						logger.error("Contactless display Error sleep Fail", e);
					} finally {
						pnlCtrl.goToPostStep1SelectPayment(pnlCtrl.getPnlSelectedCp());
					}
				}
			}
		}.start();		
	}
	
	private void handlePaymentOctopus() {
		polling = true;
		error100022CardNo = null;
		
		payThread = new Thread() {
			public void run() {
				logger.info("Octopus thread started.");
				boolean success = false;

				try {
					tran.setOctopusDeviceNo(OctUtil.DEV_ID);
					OctCheckerThread.setPooling(true);
					PollDeductReturn res = poll(-1, null, false, tran);
					
					boolean pollSuccess = res.isPollSuccess();
					
					if(pollSuccess) {
						PollEx pd = (PollEx)res.getPollReturn().getReturnData();
						pd.setRemainingValue(res.getPollReturn().getReturnCode());
						tran.setOctopusNo(pd.getNewCardId());
						tran.setRemainBal(new BigDecimal(pd.getRemainingValue()).divide(new BigDecimal(10)));
						tran.setSmartOctopus(pd.isSmartOctopus() ? "Y" : "N");
						setReceiptNo(tran);
						OctUtil.playNormalToneOctopus();
						success = true;
					}
					else {
						//Go to home after timeout
						if(PostStep2ProcessPayment.this.isShowing()) {
							logger.info("No card present, go to home now.");
							pnlCtrl.goToHome();
						}
					}
				}
				catch (Throwable e) {
					logger.error("Failed to poll card:" + e.getMessage(), e);
					pnlCtrl.showErrorMessageGeneral("9100", e);	//unknown error
				} finally {
					OctCheckerThread.setPooling(false);
				}
				
				
				if(success) {
					cardDetected();
				}
				logger.info("Octopus thread stopped.");
			}
		};
		payThread.start();
	}
	
	
	
	private void cardDetected() {
		pnlCp.getCp().setTran(tran);
		tran.setTranStatusCode(String.valueOf(TranStatus.Charging));
		tran.setPayMethodCode(String.valueOf(pnlCtrl.getPayMethod()));

		//set to max charing unit by default
		Calendar cal = Calendar.getInstance();
		tran.setStartDttm(cal.getTime());
		tran.setTranDttm(tran.getStartDttm());
		int duration = RateUtil.getMaxChargingDurationMin();
		cal.add(Calendar.MINUTE, duration);
		tran.setEndDttm(cal.getTime());
		tran.setDurationMin(duration);
		
		pnlCp.remoteStartTransaction();
		
		new Thread() {
			public void run() {
				logger.info("Check CP status thread started:" + pnlCp.getCp().getCpNo());
				long expiredTm = System.currentTimeMillis() + config.getRemoteStartStopTimeCheckMs();	//10 seconds
				while(expiredTm > System.currentTimeMillis()) {
					if(CtUtil.isCpStatusCharging(pnlCp.getCp().getStatus())) {
						break;
					}
					
					try {
						Thread.sleep(20);
					} catch (Exception e) {
					}
				}
				logger.info("Check CP status thread ended:" + pnlCp.getCp().getCpNo() + ", status:" + pnlCp.getCp().getStatus());
				
				if(CtUtil.isCpStatusCharging(pnlCp.getCp().getStatus())) {
					//Stop charging thread
					new StopChargingThread(pnlCp).start();
					
					if(pnlCtrl.isCurrentDisplayingPanel(PostStep2ProcessPayment.this) || pnlCtrl.isShowingErrorMessage()) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								pnlCtrl.goToPostStep3ChargingRecord();
							}
						});
					}
				}
				else {
					logger.warn("Failed to start charging, set to Unavailable, cp:" + pnlCp.getCp().getCpNo());
					pnlCp.getCp().setStatus(ChargePointStatus.Unavailable);
					
					showErrorMessage("ERR9100", pnlCp.getCp().getCpNo());	
					
					CtWebSocketClient.updateCp(pnlCp.getCp());
				}
				
			}
		}.start();
		
		CtWebSocketClient.uploadTran(tran);
	}
}
