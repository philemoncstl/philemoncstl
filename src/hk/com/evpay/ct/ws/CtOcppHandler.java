package hk.com.evpay.ct.ws;


import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;
import com.ckzone.util.GsonUtil;

import hk.com.cstl.evcs.lms.LmsCons;
import hk.com.cstl.evcs.lms.LmsServEvent;
import hk.com.cstl.evcs.lms.LmsServEventType;
import hk.com.cstl.evcs.lms.TxDefaultProfilePara;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.ocpp.CpWebSocket;
import hk.com.cstl.evcs.ocpp.CpWebSocketServEndpoint;
import hk.com.cstl.evcs.ocpp.DefaultOcppHandler;
import hk.com.cstl.evcs.ocpp.eno.AuthorizationStatus;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.ChargingProfileStatus;
import hk.com.cstl.evcs.ocpp.eno.RegistrationStatus;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.cstl.evcs.ocpp.msg.BootNotification;
import hk.com.cstl.evcs.ocpp.msg.BootNotificationResp;
import hk.com.cstl.evcs.ocpp.msg.Heartbeat;
import hk.com.cstl.evcs.ocpp.msg.HeartbeatResp;
import hk.com.cstl.evcs.ocpp.msg.IdTagInfo;
import hk.com.cstl.evcs.ocpp.msg.MeterValue;
import hk.com.cstl.evcs.ocpp.msg.MeterValues;
import hk.com.cstl.evcs.ocpp.msg.MeterValuesResp;
import hk.com.cstl.evcs.ocpp.msg.RequestObj;
import hk.com.cstl.evcs.ocpp.msg.SampledValue;
import hk.com.cstl.evcs.ocpp.msg.StartTransaction;
import hk.com.cstl.evcs.ocpp.msg.StartTransactionResp;
import hk.com.cstl.evcs.ocpp.msg.StatusNotification;
import hk.com.cstl.evcs.ocpp.msg.StatusNotificationResp;
import hk.com.cstl.evcs.ocpp.msg.StopTransaction;
import hk.com.cstl.evcs.ocpp.msg.StopTransactionResp;
import hk.com.cstl.evcs.ocpp.msg.cs.ChargingProfile;
import hk.com.cstl.evcs.ocpp.msg.cs.CsRequestObj;
import hk.com.cstl.evcs.ocpp.msg.cs.CsResponseObj;
import hk.com.cstl.evcs.ocpp.msg.cs.SetChargingProfile;
import hk.com.cstl.evcs.ocpp.msg.cs.SetChargingProfileResp;
import hk.com.evpay.ct.CpPanel;
import hk.com.evpay.ct.CtClient;
import hk.com.evpay.ct.CtrlPanel;
import hk.com.evpay.ct.util.CtUtil;

public class CtOcppHandler extends DefaultOcppHandler{
	private static final Logger logger = Logger.getLogger(CtOcppHandler.class);
	
	public static void main(String[] args) {
		ChargePointStatus s = ChargePointStatus.fromString("Charging");
		System.out.println(s);
	}
	
	private void logEvent(RequestObj ro, CpModel cp) {
		try {
			CtWebSocketClient.uploadCpEvent(ro, cp);
		} catch(Exception e) {
			logger.error("Failed to load CP event:" + cp.getCpNo() + ", req:" + GsonUtil.toJson(ro), e);
		}
	}
	
	@Override
	public BootNotificationResp bootNotification(RequestObj ro, BootNotification oper) {
		logger.info("Received boot notification:" + ro.getCpNo());
		CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);
		cp.setBootDttm(new Date());
		
		BootNotificationResp resp = new BootNotificationResp();
		resp.setStatus(RegistrationStatus.Rejected);
		resp.setCurrentTime(DateUtil.formatTimestamp());
		
		//CK @ 20191216, set default profile if LMS enabled
		if(CtUtil.getConfig().isLms()) {
			logger.info("Sending default profile:" + ro.getCpNo());
			CpWebSocketServEndpoint servEp = CtrlPanel.getCpWebSocket(ro.getCpNo());
			if(servEp != null) {
				ChargingProfile dp = TxDefaultProfilePara.getDefaultProfile(CtUtil.getConfig().getCtId(), ro.getCpNo());
				try {
					SetChargingProfile msg = CpWebSocket.setChargingProfile(servEp, 1, dp);
					long timeout = System.currentTimeMillis() + LmsCons.SEND_DEFAULT_PROFILE_TIMEOUT_MS;
					while(timeout > System.currentTimeMillis()) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
						
						//already response
						if(msg.getResponseDttm() != null) {
							logger.info("Set default profile returned:" + msg.getResponse().getStatus());
							break;
						}
					}
					
					if(msg.getResponseDttm() != null) {
						SetChargingProfileResp dpResp = msg.getResponse();
						if(dpResp.getStatus() == ChargingProfileStatus.Accepted) {
							resp.setStatus(RegistrationStatus.Accepted);
						}
						else {
							logger.info("Set boot notification to rejected!");
							resp.setStatus(RegistrationStatus.Rejected);
						}
					}
					else {
						logger.info("Set default profile timeout.");
					}
				} catch(Exception e) {
					logger.error("Failed to set default profile.", e);
				}
			}
			else {
				logger.info("Set default profile skipped as session not found:" + ro.getCpNo());
			}
		}
		else {
			resp.setStatus(RegistrationStatus.Accepted);
		}
		
		logger.info("RegStatus:" + resp.getStatus());
		//TODO, bypass default profile response
		//resp.setStatus(RegistrationStatus.Accepted);
		
		if(resp.getStatus() == RegistrationStatus.Accepted) {
			resp.setInterval(CtUtil.getHeartbeatIntervalSec());
			resp.setAdminCardList(CtUtil.getServConfig() == null ? "" : CtUtil.getServConfig().getAdminCardList());	//add@20180311

			//CK @ 2018-03-23, send changeAvailability and request status notification
			CpPanel pnlCp = CtrlPanel.getCpPanelByNo(cp.getCpNo());
			if(pnlCp != null) {
				final boolean enabled = CtUtil.getCt().isEnabled() && cp.isEnabled();
				final CpWebSocketServEndpoint cpEp = pnlCp.getCpEp();
				CpWebSocket.sendRequestAfterBootNotificationOrConnected(enabled, cpEp);
			}
			else {
				logger.warn("CpPanel is null:" + cp.getCpNo());
			}
			
			//CK @ 20191204, notify LMS
			if(CtUtil.getConfig().isLms()) {
				LmsServEvent e = newLmsEvent(LmsServEventType.BootNotification, cp.getCpNo());
				triggerLmsEvent(e);
			}
		}		
		
		return resp;
	}
	
	@Override
	public StatusNotificationResp statusNotification(RequestObj ro, StatusNotification oper) {
		CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);
		ChargePointStatus oldStatus = cp.getStatus();
		ChargePointStatus newStatus = ChargePointStatus.fromString(oper.getStatus());
		
		//CK @ 20170319 Handle case below
		/*6. BootNotication check last transaction record. 
		based on status, 
			-cpStatusCharging			treat as normal
			-cpStatusSuspendedEVSE		treat as normal
			-cpStatusSuspendedEV		treat as normal
		otherwise, treat last transaction record as completed*/
		boolean updateTranToBackend = false;
		if(cp.getBootDttm() != null) {
			Date lupdStatusDttm = cp.getStatusDttm();
			//not yet received status notification since last boot
			if(lupdStatusDttm == null || lupdStatusDttm.getTime() < cp.getBootDttm().getTime()) {
				if(cp.getTran() != null && String.valueOf(TranStatus.Charging).equals(cp.getTran().getTranStatusCode())) {
					if(newStatus != ChargePointStatus.Charging && newStatus != ChargePointStatus.SuspendedEV && 
							newStatus != ChargePointStatus.SuspendedEVSE) {
						cp.getTran().setTranStatusCode(String.valueOf(TranStatus.Stopped));
						Date stopDttm = cp.getLastReceivedDttmBeforeBootNotification();	//@2018-03-23
						if(stopDttm == null) {
							stopDttm = cp.getBootDttm();
							logger.info("getLastReceivedDttmBeforeBootNotification() is null, use boot time instead:" + stopDttm);							
						}
						if(stopDttm != null) {
							cp.getTran().setActualEndDttm(stopDttm);
						}
						logger.info("Update tran status to stopped, CP:" + cp.getCpNo() + ", actualStopDttm:" + cp.getTran().getActualEndDttm() + ", tranId:" + cp.getTran().getIdTag());
						
						
						updateTranToBackend = true;
					}
				}
			}
		}
		
		cp.setStatus(newStatus);
		cp.setStatusDttm(new Date());
		
		if(oldStatus != cp.getStatus()) {
			logger.info("CP status changed, old:" + oldStatus + ", new:" + cp.getStatus());
			try {
				CtWebSocketClient.updateCp(cp);
			} catch(Exception e) {
				logger.error("Failed to updateCp to backend", e);
			}
		}
		
		if(updateTranToBackend) {
			logger.info("Update tran as Stopped:" + cp.getTran().getIdTag());
			try {
				CtWebSocketClient.uploadTran(cp.getTran());
			} catch(Exception e) {
				logger.error("Failed to update tran as Stopped:" + cp.getTran().getIdTag(), e);
			}
		}
		
		//CK @ 20191204, notify LMS
		LmsServEvent e = newLmsEvent(LmsServEventType.StatusNotification, cp.getCpNo());
		e.addParm(LmsCons.PARM_CP_STATUS, newStatus);
		triggerLmsEvent(e);
		
		return new StatusNotificationResp();
	}
	
	@Override
	public HeartbeatResp heartbeat(RequestObj ro, Heartbeat oper) {
		//No to log heartbeat event
		/*CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);*/
		return super.heartbeat(ro, oper);
	}

	@Override
	public void handleCsResponse(CsRequestObj csReq) {
		CpModel cp = CtUtil.getCp(csReq.getCpNo());
		if(cp == null) {
			logger.warn("CP model not found:" + csReq.getCpNo());
			return;
		}
		
		logger.info("Handling response for req:" + csReq.getCpNo() + ", uid:" + csReq.getUniqueId());
		
		if(csReq.isResponseSuccess() && csReq.getResponse() != null) {
			boolean res = ((CsResponseObj)csReq.getResponse()).responseReceived(cp);

			//TODO
		}
	}
	
	@Override
	public MeterValuesResp meterValues(RequestObj ro, MeterValues oper) {
		CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);
		
		if(cp.getTran() != null && oper.getTransactionId() != null) {
			if(cp.getTran().getTranId() == oper.getTransactionId()) {
				if(oper.getMeterValue() != null && oper.getMeterValue().length > 0) {
					//only get the first one
					MeterValue mv = oper.getMeterValue()[0];
					if(mv.getSampledValue() != null && mv.getSampledValue().length > 0) {
						//only get the first one
						SampledValue sv = mv.getSampledValue()[0];
						if(sv.getValue() != null) {
							cp.getTran().setMeterStop(new BigDecimal(sv.getValue()));
							logger.info("Meter value:" + sv.getValue() + ", timestamp:" + mv.getTimestamp());
						}
						else {
							logger.warn("Invalid value:" + sv.getValue());
						}
					}
					else {
						logger.warn("Sampled value is null or empty!");
					}	
				}
				else{
					logger.warn("Meter value is null or empty!");
				}
				logger.info("Tran:" + cp.getTran().getTranId() + ", set meterStop=" + oper.getMeterValue());
			}
			else {
				logger.warn("Tran ID not match, local:" + cp.getTran().getTranId() + ", CP:" + oper.getTransactionId());
			}
		}
		
		//CK @ 20191204, notify LMS
		LmsServEvent e = newLmsEvent(LmsServEventType.MeterValues, cp.getCpNo());
		e.addParm(LmsCons.PARM_METER_VALUES, oper);
		triggerLmsEvent(e);
		
		return new MeterValuesResp();
	}
	
	@Override
	public StartTransactionResp startTransaction(RequestObj ro, StartTransaction oper) {
		CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);
		
		StartTransactionResp resp = new StartTransactionResp();
		IdTagInfo idTagInfo = new IdTagInfo();
		idTagInfo.setStatus(AuthorizationStatus.Accepted);
		resp.setIdTagInfo(idTagInfo);

		if(cp != null) {
			if(cp.getTran() != null) {
				resp.setTransactionId(cp.getTran().getTranId());
				//Set meter start value
				if(oper.getMeterStart() != null) {
					cp.getTran().setMeterStart(new BigDecimal(oper.getMeterStart()));
				}
			}
			else {
				logger.warn("Tran is null:" + ro.getCpNo());
				resp.setTransactionId(-2);
			}
		}
		else {
			resp.setTransactionId(-1);
			logger.warn("CP is null:" + ro.getCpNo());
		}
		
		//CK @ 20191204, notify LMS
		LmsServEvent e = newLmsEvent(LmsServEventType.StartTransaction, cp.getCpNo());
		triggerLmsEvent(e);
		
		return resp;
	}
	
	@Override
	public StopTransactionResp stopTransaction(RequestObj ro, StopTransaction oper) {
		CpModel cp = CtUtil.getCp(ro.getCpNo());
		logEvent(ro, cp);
		
		StopTransactionResp resp = new StopTransactionResp();
		IdTagInfo tag = new IdTagInfo();
		tag.setStatus(AuthorizationStatus.Invalid);
		resp.setIdTagInfo(tag);
		
		if(cp != null) {
			if(cp.getTran() != null) {
				if(cp.getTran().getTranId() == oper.getTransactionId()) {
					tag.setStatus(AuthorizationStatus.Accepted);
					cp.getTran().setActualEndDttm(new Date());
					cp.getTran().setTranStatusCode(TranStatus.Stopped.toString());
					//set meter stop value
					if(oper.getMeterStop() != null) {
						cp.getTran().setMeterStop(new BigDecimal(oper.getMeterStop()));
					}
					BigDecimal start = cp.getTran().getMeterStart();
					BigDecimal stop = cp.getTran().getMeterStop();
					logger.info("Received stopTransaction:" + oper.getTransactionId() + ", meterStart:" + start + 
							", meterStop:" + stop + ", diff:" + (start != null && stop != null ? (stop.subtract(start)) : "N/A"));
					try {
						//CK @ 20180324
						CtWebSocketClient.uploadTran(cp.getTran());
					}
					catch(Exception e) {
						logger.error("Failed to upload tran:" + cp.getTran().getTranId());
					}
					
					//CK @ 20191204, notify LMS
					LmsServEvent e = newLmsEvent(LmsServEventType.StopTransaction, cp.getCpNo());
					triggerLmsEvent(e);
				}
				else {
					logger.warn("Tran ID not match:" + ro.getCpNo() + 
							", received:" + oper.getTransactionId() + ", current:" + cp.getTran().getTranId());
				}
			}
			else {
				logger.warn("Tran is null:" + ro.getCpNo());
			}
		}
		else {
			logger.warn("CP is null:" + ro.getCpNo());
		}
		
		return resp;
	}
	
	
	public LmsServEvent newLmsEvent(LmsServEventType type, String cpNo) {
		return CtClient.CUR_INST.newLmsEvent(type, cpNo);
	}
	
	public void triggerLmsEvent(LmsServEvent e) {
		CtClient.CUR_INST.triggerLmsEvent(e);
	}
}
