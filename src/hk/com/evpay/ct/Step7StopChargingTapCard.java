package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.util.Date;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ckzone.octopus.PollDeductReturn;
import com.ckzone.octopus.util.OctCheckerThread;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.util.StringUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.iUC285Util;
import hk.com.evpay.ct.util.iUC285Util.Status;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class Step7StopChargingTapCard extends CommonPanelOctopus{
	private static final Logger logger = Logger.getLogger(Step7StopChargingTapCard.class);
	
	private I18nLabel lblStopInst;
	private CpPanel pnlCp;
	
	private Thread stopThread;

	public Step7StopChargingTapCard(CtrlPanel pnlCtrl) {
		super(pnlCtrl);

		setLayout(null);
		
		lblStopInst = createButton("stopChargingInstContactless", "img/msg_box.png", 270, 380, 744, 184);
		lblStopInst.setForeground(Color.WHITE);
		LangUtil.setFont(lblStopInst, Font.PLAIN, 32);
		add(lblStopInst);
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		pnlCp = cp;
		
		TranModel tran = cp.getCp().getTran();
		if(CtUtil.isPayByContactless(tran)) {
			lblStopInst.setMsgCode("stopChargingPreInstContactless");
			stopChargingContactless();
		}
		else if(CtUtil.isPayByOctopus(tran)){
			lblStopInst.setMsgCode("stopChargingInstOct");
			stopChargingOctopus();
		}
		else if(CtUtil.isPayByQr(tran)){
			lblStopInst.setMsgCode("stopChargingInstQR");
			stopChargingQr();
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
					//prepaid
					PollDeductReturn res = poll(-1, tran.getOctopusNo(), false, tran);
					
					boolean success = false;
					if(res.isPollSuccess() && res.isSameCard()) {
						success = true;
						OctUtil.playNormalToneOctopus();
					}
					
					
					
					if(success) {
						stopChargingNow();
					}
					else {				
						if (!res.isSameCard()) {
							logger.info("Not the same card, go to home now.");
							pnlCtrl.goToHome();
						}
						//Go to home after timeout
						else if(Step7StopChargingTapCard.this.isShowing()) {
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
	
	private void pollCardContactless(TranModel tran) {
		/*try {
			CardDetailResp resp = WisepayUtil.cardDetail();
			if(resp != null && WpRespUtil.isRespSuccess(resp.getStatus(), resp.getResultCode())) {
				if(resp.getCardData() != null) {
					String cardNo = resp.getCardData().getMaskedPan();
					logger.info("MaskedPan:" + cardNo);
					if(WpRespUtil.isSameCard(tran.getCardNo(), cardNo)) {
						stopChargingNow();
					}
					else {
						logger.warn("Not the same card, start card no:" + tran.getCardNo());
						showPresentSameCard("presentSameCardContactless", tran.getCardNo());
						pnlCtrl.goHomeCountDownExtend(10000);
						pollCardContactless(tran);	//try again	
					}
				}
			}
			else {
				logger.info("No card present or timeout, go to home now.");
				pnlCtrl.goToHome();
			}
		}catch(Exception e) {
			logger.error("Failed to poll card bbpos!", e);
		}*/
	}
	
	private void stopChargingContactless() {
		final TranModel tran = pnlCp.getCp().getTran();
		
		stopThread = new Thread() {
			public void run() {
				JSONObject response 	= iUC285Util.doCardRead();
				Status responseStatus 	= iUC285Util.getStatus(response);
				/*
				 * if(CtUtil.isContactlessBbpos()) { if(WpCheckerThread.isAvailable()) {
				 * pollCardContactless(tran); } else { pnlCtrl.showErrorMessageGeneral("9001",
				 * null); } }
				 */
				
				if(responseStatus == Status.Approved) {
					if(response.getString("CARDHASH").equals(tran.getCardHash())) {
						stopChargingNow();
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
		};
		stopThread.start();
	}
	
	public static boolean detectQrStop(long ttm, TranModel tran, CtrlPanel pnlCtrl) {
		logger.info(Thread.currentThread().getName() + " - detectQr started");
		String qr = null;
		long startTm = System.currentTimeMillis();
		long timeout = startTm + CtUtil.getConfig().getQrScanTimeoutMs();
		
		String tmp = null;
		while(timeout > System.currentTimeMillis() && CtrlPanel.isSameQrThread(ttm)) {
			tmp = pnlCtrl.getQr();
			if(!StringUtil.isEmpty(tmp) && pnlCtrl.getQrDttm() > startTm) {
				
				//CK@201911 also add admin qr for stopping charging
				if(tmp.equalsIgnoreCase(tran.getCardNo()) || CtUtil.ADMIN_QR.equalsIgnoreCase(tmp)) {
					return true;
				}
				else {
					if(qr == null || !qr.equals(tmp)) {
						logger.info("Diff qr, end:" + tmp + ", start:" + tran.getCardNo());
						//show diff qr
						pnlCtrl.showErrorMessage("presentSameQR", "");
					}
					qr = tmp;
				}
				
			}
			try {
				Thread.sleep(50);
			} catch(Exception e) {}
		}
		
		return false;
	}
	
	private void stopChargingQr() {
		final TranModel tran = pnlCp.getCp().getTran();
		
		stopThread = new Thread() {
			public void run() {
				long ttm = System.currentTimeMillis();
				CtrlPanel.QR_THREAD_MS = ttm;
				boolean done = detectQrStop(ttm, tran, pnlCtrl);
				logger.info(Thread.currentThread().getName() + " - detectQr ended, done:" + done);
				if(CtrlPanel.isSameQrThread(ttm)) {
					if(done) {
						stopChargingNow();
					}
					else {
						logger.info("No QR scanned or not matched, go home now.");
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
	
	private void stopChargingNow() {
		pnlCp.remoteStopTransaction();
		
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
					if(pnlCtrl.isCurrentDisplayingPanel(Step7StopChargingTapCard.this) || pnlCtrl.isShowingErrorMessage()) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								pnlCtrl.goToStep8UnplugCable();
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
