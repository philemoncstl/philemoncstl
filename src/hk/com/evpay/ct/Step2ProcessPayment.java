package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
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

import hk.com.cstl.evcs.model.RateDetailModel;
import hk.com.cstl.evcs.model.RateModel;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.i18n.FieldPanel;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.tool.TranHistCtrl;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;
import hk.com.evpay.ct.util.RateUtil;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class Step2ProcessPayment extends CommonPanelOctopus{
	private static final Logger logger = Logger.getLogger(Step2ProcessPayment.class);
	
	private I18nButtonLabel lblBgConnected;
	
	private I18nLabel lblCpStatus;
	
	private FieldPanel fpCp;
	private FieldPanel fpDuration;
	private FieldPanel fpOffPeakRate;
	private FieldPanel fpOnPeakRate;
	private FieldPanel fpAmount;
	
	private I18nLabel lblPayInst;
	
	private Thread payThread;


	public Step2ProcessPayment(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		lblCpStatus = createLabel("", "", 217, 265, 845, 60);
		LangUtil.setFont(lblCpStatus, Font.PLAIN, 40);
		add(lblCpStatus);
		
		JPanel pnl = new JPanel();
		pnl.setOpaque(false);
		add(pnl);
		pnl.setBounds(calcBoundsLabel(220, 335, 845, 198));

		//CP
		fpCp = new FieldPanel("cpNoLabel", "i18nLabel");		
		pnl.add(fpCp);
		
		//duration
		fpDuration = new FieldPanel("chargingTime", "chargingTimeVal");
		pnl.add(fpDuration);
		
		//off peak, or serviceFeeTimeUnit if no on/off peak
		fpOffPeakRate = new FieldPanel("offPeakPeriodTimeUnit", "hkdWithVal2");
		fpOffPeakRate.getLbl().setParms("10.00", "30");
		pnl.add(fpOffPeakRate);
		
		//on peak
		fpOnPeakRate = new FieldPanel("onPeakPeriodTimeUnit", "hkdWithVal2");
		pnl.add(fpOnPeakRate);
		
		fpAmount = new FieldPanel("totalAmount", "hkdWithVal2");
		fpAmount.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
		pnl.add(fpAmount);
		
		//pay inst (payInstContactless or payInstOct)
		lblPayInst = createButton("payInstContactless", "img/msg_box.png", 265, 576, 744, 184);
		lblPayInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblPayInst, Font.PLAIN, 32);
		add(lblPayInst);
		
		
		lblBgConnected = createButton("img/checkStatusPreparing.png", 170, 258, 920, 301);
		add(lblBgConnected);
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "chargingStatus";
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		lblCpStatus.setMsgCode("cpStatus" + cp.getCp().getStatus());
		fpCp.getVal().setParms(cp.getCp().getCpNo());
		
		/*if(pnlCtrl.getPayMethod() == PayMethod.Contactless) {
			lblPayInst.setMsgCode("payInstContactless");
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.Octopus){
			lblPayInst.setMsgCode("payInstOct");			
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.QR){
			lblPayInst.setMsgCode("payInstQR");			
		}*/
		
		TranModel tm = cp.getCp().getTran();
		int durationMin = tm.getDurationMin();
		int hour = durationMin / 60;
		int min = durationMin % 60;
		fpDuration.getVal().setParms(hour, min);
		
		RateModel rm = RateUtil.getRate();
		RateDetailModel rd = RateUtil.getRateDetail();
		if(rd.isOnOffPeakSameTimeRate()) {
			fpOffPeakRate.getLbl().setMsgCode("serviceFeeTimeUnit");
			fpOnPeakRate.setVisible(false);
			fpOffPeakRate.getVal().setParm(tm.getTimeCharge());
		}
		else {
			fpOffPeakRate.getLbl().setMsgCode("offPeakPeriodTimeUnit");
			fpOnPeakRate.setVisible(true);
			
			fpOnPeakRate.getLbl().setParms(rd.getOnPeakTimeRate().multiply(new BigDecimal(rm.getMins())).divide(new BigDecimal(60), 1, RoundingMode.DOWN).toString(), 
					String.valueOf(rm.getMins()));
			fpOnPeakRate.getVal().setParm(tm.getTimeChargeOnPeak());
			fpOffPeakRate.getVal().setParm(tm.getTimeChargeOffPeak());
		}
		
		fpOffPeakRate.getLbl().setParms(rd.getOffPeakTimeRate().multiply(new BigDecimal(rm.getMins())).divide(new BigDecimal(60), 1, RoundingMode.DOWN).toString(), 
				String.valueOf(rm.getMins()));
		fpAmount.getVal().setParm(tm.getTimeCharge());
		
		if(pnlCtrl.getPayMethod() == PayMethod.ContactlessGroup) {
			lblPayInst.setMsgCode("payInstContactless");
			handlePaymentContactless();
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.Octopus){
			lblPayInst.setMsgCode("payInstOct");
			handlePaymentOctopus();
		}
		else if(pnlCtrl.getPayMethod() == PayMethod.QR){
			lblPayInst.setMsgCode("payInstQR");
			handlePaymentQR();
		}
		
	}
	
	public static void setDummyCardInfoContactless(TranModel tran) {
		/*//map.put("cardType", "VISA");		
		map.put("cardNo", "************8224");
		map.put("mid", "000883234288883");
		map.put("rrn", "20576282089");
		map.put("ecrRef", "0001131217192325");*/
		//map.put("expDate", "**/**");
		//map.put("tid", "17093001");
		//map.put("batchNo", "000009");
		//map.put("appCode", "149587");
		
		tran.setPayMethodCode("Contactless");
		tran.setMerchantId("000883234288883");
		tran.setTerminalId("17093001");
		tran.setCardNo("************8224");
		tran.setRetrievalRefNo("20576282089");
		tran.setEcrRef("0001131217192325");
		tran.setCardExpiryDate("**/**");
		tran.setBatchNo("000009");
		tran.setApprovalCode("149587");
	}
	
	private void displayContactlessError(Status responseStatus) {
		pnlCtrl.showErrorMessage(responseStatus.toString());
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			logger.error("Contactless display Error sleep Fail", e);
		} finally {
			pnlCtrl.goToStep1SelectTime(pnlCtrl.getPnlSelectedCp());
		}
	}
	
	private void handlePaymentContactless() {
		new Thread() {
			@Override
			public void run() {
				Status responseStatus = null;
				JSONObject response = null;
				TranModel tran = pnlCtrl.getPnlSelectedCp().getCp().getTran();
				setReceiptNo(tran);
				
//				response = iUC285Util.doCardRead();
//				tran.setCardType(response.getString("CARD"));
//				tran.setCardNo(response.getString("PAN"));
				
				response 		= iUC285Util.doCardRead();
				responseStatus 	= iUC285Util.getStatus(response);
				if(responseStatus == Status.Approved) {
					response = null;
					logger.info("Contactless read card success");
					tran.setCardType(response.getString("CARD"));
					tran.setCardHash(response.getString("CARDHASH"));
					response = iUC285Util.doSale(tran, tran.getAmt().multiply(new BigDecimal("100")).intValue());
					responseStatus = iUC285Util.getStatus(response);
					
					if(responseStatus == Status.Approved) {
						logger.info("Contactless payment success");
						paymentSuccess();
					} else {
						logger.info("Contactless payment fail");
						pnlCtrl.showErrorMessage(responseStatus.toString());
						try {
							sleep(2000);
						} catch (InterruptedException e) {
							logger.error("Contactless display Error sleep Fail", e);
						} finally {
							pnlCtrl.goToStep1SelectTime(pnlCtrl.getPnlSelectedCp());
						}
					}
				} else {
					logger.info("Contactless read card fail");
					pnlCtrl.showErrorMessage(responseStatus.toString());
					try {
						sleep(2000);
					} catch (InterruptedException e) {
						logger.error("Contactless display Error sleep Fail", e);
					} finally {
						pnlCtrl.goToStep1SelectTime(pnlCtrl.getPnlSelectedCp());
					}
				}
			}
		}.start();		
	}
	
	public static String detectQr(long ttm, TranModel tran, CtrlPanel pnlCtrl) {
		logger.info(Thread.currentThread().getName() + " - Detect qr started");
		String qr = null;
		long startTm = System.currentTimeMillis();
		long timeout = startTm + CtUtil.getConfig().getQrScanTimeoutMs();
		
		String tmp = null;
		while(timeout > System.currentTimeMillis() && CtrlPanel.isSameQrThread(ttm)) {
			tmp = pnlCtrl.getQr();
			if(!StringUtil.isEmpty(tmp) && pnlCtrl.getQrDttm() > startTm) {
				qr = tmp;
				tran.setCardNo(qr);
				logger.info("Set qr:" + qr);
				return qr;
			}
			try {
				Thread.sleep(50);
			} catch(Exception e) {}
		}
		
		return null;
	}
	
	private void handlePaymentQR() {
		new Thread() {
			@Override
			public void run() {
				long ttm = System.currentTimeMillis();
				CtrlPanel.QR_THREAD_MS = ttm;
				TranModel tran = pnlCtrl.getPnlSelectedCp().getCp().getTran();
				String qr = detectQr(ttm, tran, pnlCtrl);
				logger.info(Thread.currentThread().getName() + " - detectQr ended, qr:" + qr);
				if(CtrlPanel.isSameQrThread(ttm)) {
					if(StringUtil.isEmpty(qr)) {
						logger.info("No QR scanned, go home now.");
						pnlCtrl.goToHome();
					}
					else {
						logger.info("Use qr:" + qr);
						setReceiptNo(tran);
						paymentSuccess();
					}
				}
				else {
					logger.info("Not same qr thread.");
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
					TranModel tran = pnlCtrl.getPnlSelectedCp().getCp().getTran();
					tran.setOctopusDeviceNo(OctUtil.DEV_ID);
					OctCheckerThread.setPooling(true);
					PollDeductReturn res = poll(tran.getAmt().multiply(new BigDecimal("10")).intValue(), null, true, tran);
					
					//for prepaid
					boolean deductSuccess = res.isDeductSuccess();
					
					if(deductSuccess) {
						PollEx pd = (PollEx)res.getPollReturn().getReturnData();
						pd.setRemainingValue(res.getDeductReturn().getReturnCode());
						tran.setRemainBal(new BigDecimal(pd.getRemainingValue()).divide(new BigDecimal(10)));
						tran.setSmartOctopus(pd.isSmartOctopus() ? "Y" : "N");
						
						if(res.isGetExtraInfoSuccess()) {
							ExtraInfo info = (ExtraInfo)res.getExtraInfoReturn().getReturnData();
							setOctopusExtraInfo(tran, info);
						}
						
						success = true;
					}
					else {
						//Go to home after timeout
						if(Step2ProcessPayment.this.isShowing()) {
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
					paymentSuccess();
				}
				logger.info("Octopus thread stopped.");
			}
		};
		payThread.start();
	}

	private void paymentSuccess() {
		final CpPanel pnlCp = pnlCtrl.getPnlSelectedCp();
		
		TranModel tm = pnlCp.getCp().getTran();
		tm.setTranStatusCode(String.valueOf(TranStatus.Charging));
		tm.setPayMethodCode(String.valueOf(pnlCtrl.getPayMethod()));
		if(StringUtil.isEmpty(tm.getReceiptNo())) {
			setReceiptNo(tm);
		}
		
		pnlCp.remoteStartTransaction();
		//save to hist
		TranHistCtrl.add(tm);
		
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
					
					if(pnlCtrl.isCurrentDisplayingPanel(Step2ProcessPayment.this) || pnlCtrl.isShowingErrorMessage()) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								if(PrinterUtil.isOnline()) {
									pnlCtrl.goToStep3PrintReceipt();
								}
								else {
									pnlCtrl.goToStep5ChargingRecord();
								}
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
		
		CtWebSocketClient.uploadTran(tm);
	}
}
