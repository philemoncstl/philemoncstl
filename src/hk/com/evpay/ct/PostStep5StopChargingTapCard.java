package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.math.BigDecimal;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
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


import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.tool.TranHistCtrl;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class PostStep5StopChargingTapCard extends CommonPanelOctopus{
	private static final Logger logger = Logger.getLogger(PostStep5StopChargingTapCard.class);
	
	private I18nLabel lblStopInst;
	private CpPanel pnlCp;
	
	private Thread stopThread;
	
	private Image  defaultImg, contactlessFirstImg, contactlessSecondImg, contactlessFinishImg;

	public PostStep5StopChargingTapCard(CtrlPanel pnlCtrl) {
		super(pnlCtrl);

		setLayout(null);
		ImageIcon imageIcon = new ImageIcon("img/msg_box.png");
		Image image = imageIcon.getImage(); // transform it 
		defaultImg = image.getScaledInstance(744, 184,  Image.SCALE_SMOOTH); // scale it the smooth way  
		lblStopInst = createButton("stopChargingInstContactless", "img/msg_box.png", 270, 380, 744, 184);
		lblStopInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblStopInst, Font.PLAIN, 32);
		add(lblStopInst);
		
		
		
		//for Contactless new UI
		imageIcon = new ImageIcon("img/payment_contactless_first.png");
		image = imageIcon.getImage(); // transform it 
		contactlessFirstImg = image.getScaledInstance(744, 300,  Image.SCALE_SMOOTH); // scale it the smooth way  
		
		imageIcon = new ImageIcon("img/payment_contactless_second.png");
		image = imageIcon.getImage(); // transform it 
		contactlessSecondImg = image.getScaledInstance(744, 300,  Image.SCALE_SMOOTH); // scale it the smooth way  
		
		imageIcon = new ImageIcon("img/payment_contactless_finish.png");
		image = imageIcon.getImage(); // transform it 
		contactlessFinishImg = image.getScaledInstance(744, 300,  Image.SCALE_SMOOTH); // scale it the smooth way 
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		pnlCp = cp;
		
		TranModel tran = cp.getCp().getTran();
//		TODO cannot get Payment method
		
		logger.info("Check Payment");
		if(CtUtil.isPayByContactless(tran)) {
			logger.info("isPayByContactless");
			lblStopInst.setSize(744, 300);
			lblStopInst.setMsgCode("stopChargingInstContactlessVerify");
			lblStopInst.setIcon(new ImageIcon(contactlessFirstImg));
			stopChargingContactless();
		}
		else if(CtUtil.isPayByOctopus(tran)){
			logger.info("isPayByOctopus");
			lblStopInst.setMsgCode("stopChargingInstPostpaid");
			lblStopInst.setIcon(new ImageIcon(defaultImg));
			stopChargingOctopus();
		}
		else if(CtUtil.isPayByQr(tran)){
			logger.info("isPayByQr");
			lblStopInst.setMsgCode("stopChargingInstPostpaidQR");
			lblStopInst.setIcon(new ImageIcon(defaultImg));
			stopChargingQr();
		} else {
			logger.info("trans getPayMethodCode" + tran.getPayMethodCode());
		}
	}
	
	private void stopChargingOctopus() {
		polling = true;
		final TranModel tran = pnlCp.getCp().getTran();
		error100022CardNo = null;
		
		stopThread = new Thread() {
			public void run() {
				logger.info("Octopus thread started.");
				try {
					OctCheckerThread.setPooling(true);
					//postpaid
					PollDeductReturn res = poll(tran.getAmt().multiply(new BigDecimal("10")).intValue(), tran.getOctopusNo(), true, tran);
					
					boolean success = false;
					if(res.isDeductSuccess() && res.isSameCard()) {						
						success = true;
						
						PollEx pd = (PollEx)res.getPollReturn().getReturnData();
						pd.setRemainingValue(res.getDeductReturn().getReturnCode());
						tran.setRemainBal(new BigDecimal(pd.getRemainingValue()).divide(new BigDecimal(10)));
						
						if(res.isGetExtraInfoSuccess()) {
							ExtraInfo info = (ExtraInfo)res.getExtraInfoReturn().getReturnData();
							setOctopusExtraInfo(tran, info);
						}						
					}
					
					if(success) {
						if(StringUtil.isEmpty(tran.getOctopusDeviceNo())) {
							tran.setOctopusDeviceNo(OctUtil.DEV_ID);
						}
						stopChargingNow();
					}
					else {				
						if (!res.isSameCard()) {
							logger.info("Not the same card, go to home now.");
							pnlCtrl.goToHome();
						}
						//Go to home after timeout
						else if(PostStep5StopChargingTapCard.this.isShowing()) {
							logger.info("No card present, go to home now.");
							pnlCtrl.goToHome();
						} 
					}
				} catch (Exception e) {
					logger.error("Failed to poll card:" + e.getMessage(), e);
					pnlCtrl.showErrorMessageGeneral("9200", e);
				} finally {
					OctCheckerThread.setPooling(false);
				}
				
				logger.info("Octopus thread stopped.");
			};
		};
		stopThread.start();
	}
	
	private void stopChargingQr() {
		stopThread = new Thread() {
			public void run() {
				long ttm = System.currentTimeMillis();
				CtrlPanel.QR_THREAD_MS = ttm;
				final TranModel tran = pnlCp.getCp().getTran();
				logger.info("Stop charging, receipt:" + tran.getReceiptNo() + ", qr:" + tran.getCardNo());
				boolean done = Step7StopChargingTapCard.detectQrStop(ttm, tran, pnlCtrl);
				logger.info(Thread.currentThread().getName() + " - detectQr ended, done:" + done);
				if(CtrlPanel.isSameQrThread(ttm)) {
					if(done) {
						stopChargingNow();
					}
					else {
						logger.info("No QR present or diff QR, go to home now.");
						pnlCtrl.goToHome();
					}
				}
				else {
					logger.info("Not same QR thread");
				}
			}
		};
		stopThread.start();
	}
	
	private void stopChargingContactless() {
		final TranModel tran = pnlCp.getCp().getTran();
		logger.info("stopChargingContactless");
		stopThread = new Thread() {
			public void run() {
				JSONObject response 	= iUC285Util.doCardRead();
				Status responseStatus 	= iUC285Util.getStatus(response);
				if(responseStatus == Status.Approved) {
					if(response.getString("CARDHASH").equals(tran.getCardHash())) {
						lblStopInst.setMsgCode("stopChargingInstContactless");
						lblStopInst.setIcon(new ImageIcon(contactlessSecondImg));
						response = iUC285Util.doSale(tran, tran.getAmt().multiply(new BigDecimal("100")).intValue());
						responseStatus = iUC285Util.getStatus(response);
						tran.setTransactionTrace(response.getString("TRACE"));
						if(responseStatus == Status.Approved) {
							Step2ProcessPayment.setCardInfoContactless(tran, response);
							logger.info("Contactless payment success");
							try {
								lblStopInst.setMsgCode("finishChargingInstContactless");
								lblStopInst.setIcon(new ImageIcon(contactlessFinishImg));
								sleep(1000);
							} catch (InterruptedException e) {
								logger.error("Contactless display Error sleep Fail", e);
							} finally {
								stopChargingNow();
							}
						}
						else if(responseStatus == Status.Timeout) {
							logger.warn("Contactless Status Timeout get retrieval again...");
							JSONObject res = iUC285Util.doRetrieval(tran);
							if(iUC285Util.getStatus(res) == Status.Approved) {
								Step2ProcessPayment.setCardInfoContactless(tran, response);
								logger.info("Contactless payment success");
								try {
									lblStopInst.setMsgCode("finishChargingInstContactless");
									lblStopInst.setIcon(new ImageIcon(contactlessFinishImg));
									sleep(1000);
								} catch (InterruptedException e) {
									logger.error("Contactless display Error sleep Fail", e);
								} finally {
									stopChargingNow();
								}
							} else {
								logger.info("Contactless payment fail");
								pnlCtrl.showErrorMessage(iUC285Util.getStatus(res).toString());
								try {
									sleep(2000);
								} catch (InterruptedException e) {
									logger.error("Contactless display Error sleep Fail", e);
								} finally {
									pnlCtrl.goToHome();
								}
							}
						}
						else {
							logger.info("Contactless payment fail");
							pnlCtrl.showErrorMessage(responseStatus.toString());
							try {
								sleep(2000);
							} catch (InterruptedException e) {
								logger.error("Contactless display Error sleep Fail", e);
							} finally {
								pnlCtrl.goToHome();
							}
						}
					} else {
						showPresentSameCardContactless(tran.getCardNo());
						try {
							sleep(10000);
						} catch (InterruptedException e) {
							logger.error("Contactless display Error sleep Fail", e);
						} finally {
							pnlCtrl.goToHome();
						}
					}
				}
				else if(responseStatus == Status.doTranBad) {
					logger.error("Contactless Status doTranBad.");
					pnlCtrl.goToHome();
				} else {

					pnlCtrl.showErrorMessage(responseStatus.toString());

 					try {
 						sleep(2000);
 					} catch (InterruptedException e) {
 						logger.error("Contactless display Error sleep Fail", e);
 					} finally {
 						pnlCtrl.goToHome();
 					}

				}		
			}
		};
		stopThread.start();
	}
	
	private void stopChargingNow() {
		pnlCp.remoteStopTransaction();
		
		//save to hist
		TranHistCtrl.add(pnlCp.getCp().getTran());
		
		new Thread() {
			public void run() {
				logger.info("Check CP status thread started:" + pnlCp.getCp().getCpNo());
				long expiredTm = System.currentTimeMillis() + config.getRemoteStartStopTimeCheckMs();	//10 seconds
				while(expiredTm > System.currentTimeMillis()) {
					if(CtUtil.isCpStatusStopped(pnlCp.getCp().getStatus())) {
						break;
					}
					
					try {
						Thread.sleep(20);
					} catch (Exception e) {
					}
				}
				logger.info("Check CP status thread ended:" + pnlCp.getCp().getCpNo() + ", status:" + pnlCp.getCp().getStatus());
				if(CtUtil.isCpStatusStopped(pnlCp.getCp().getStatus())) {
					if(pnlCtrl.isCurrentDisplayingPanel(PostStep5StopChargingTapCard.this) || pnlCtrl.isShowingErrorMessage()) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								if(PrinterUtil.isOnline() && "Y".equals(CtUtil.getServConfig().getEnablePrinter())) {
									pnlCtrl.goToStep3PrintReceipt();
								}
								else {
									pnlCtrl.goToPostStep6ShowReceipt();
								}
							}
						});
					}
				}
				else {
					logger.warn("Failed to stop charging, set to Unavailable, cp:" + pnlCp.getCp().getCpNo());
					pnlCp.getCp().setStatus(ChargePointStatus.Unavailable);
					
					showErrorMessage("ERR9200", pnlCp.getCp().getCpNo());
					
					CtWebSocketClient.updateCp(pnlCp.getCp());
				}
				
				CtUtil.saveCurrentCt();
			}
		}.start();
		
		TranModel tm = pnlCp.getCp().getTran();
		tm.setActualEndDttm(new Date());
		tm.setTranStatusCode(String.valueOf(TranStatus.Stopped));
		CtWebSocketClient.uploadTran(tm);
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
