package hk.com.evpay.ct.ws;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ckzone.octopus.OctEventData;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.util.ClientEncryptUtil;
import com.ckzone.util.GsonUtil;
import com.ckzone.util.StringUtil;

import hk.com.cstl.evcs.ct.IucEventLogDto;
import hk.com.cstl.evcs.lms.LmsCons;
import hk.com.cstl.evcs.lms.LmsServEvent;
import hk.com.cstl.evcs.lms.LmsServEventType;
import hk.com.cstl.evcs.model.AlertModel;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.model.CtModel;
import hk.com.cstl.evcs.model.ServConfig;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.CpWebSocket;
import hk.com.cstl.evcs.ocpp.eno.AvailabilityType;
import hk.com.cstl.evcs.ocpp.msg.RequestObj;
import hk.com.cstl.evcs.wsobj.CtRequest;
import hk.com.cstl.evcs.wsobj.WsAction;
import hk.com.cstl.evcs.wsobj.WsResponse;
import hk.com.evpay.ct.CpPanel;
import hk.com.evpay.ct.CtClient;
import hk.com.evpay.ct.CtrlPanel;
import hk.com.evpay.ct.util.CtUtil;

@ClientEndpoint
public class CtWebSocketClient {
	private static final Logger logger = Logger.getLogger(CtWebSocketClient.class);
	
	public static final long RECONN_ALLOWANCE_MS = 10000;	//10 seconds
	
	public static boolean cont = true;
	
	private static Session session = null;
	
	private static Thread checkerThread;
	
	private static ExecutorService ES = Executors.newFixedThreadPool(1);
	
	private static long lastMsgDttm = -1;
	
	private static final String OCPP_TEXT = "CSTEV2020";
	
	private static String encryptedOCPPText = "";
	
	// init cipher here	
	static {
		try {
			String ct = CtUtil.getConfig().getCtId();
			String plainText = ct + "_" + OCPP_TEXT + System.currentTimeMillis();
			ClientEncryptUtil.setKEY_PUBLIC("./rsa_512.pub");
			encryptedOCPPText = ClientEncryptUtil.encryptWithBase64URLSafeString(plainText);
			encryptedOCPPText = URLEncoder.encode(encryptedOCPPText, "UTF-8");
			logger.info("encryptedOCPPText:"+ encryptedOCPPText);
		} catch (Exception e2) {
			logger.error("Error in Initialize encryptedOCPPText" + e2);			
		}
	}
	
	public static void startWebSocket() {
		checkerThread = new Thread() {
			@Override
			public void run() {
				logger.info("Checker thread started");
				while(cont) {
					logger.info(getSessionId() + "Checking ws connection status...");
					if(!isConnected(false)) {
						connect();
					}
					else {
						heartbeat();
					}
					
					try {
						Thread.sleep(CtUtil.getServConfig().getHeartbeatIntervalSec() * 1000);
					} catch(Exception e) {
						logger.error(e);
					}
				}
				logger.info(getSessionId() + "Checker thread ended");
				closeSession();
			}
		};
		
		checkerThread.start();
	}
	
	private void changeAvailablility(CpPanel pnlCp, AvailabilityType type) {
		if(pnlCp == null) {
			logger.warn("changeAvailablility() pnlCp is null");
			return;			
		}
		
		if(pnlCp.isCpConnected()) {
			new Thread() {
				public void run() {
					CpWebSocket.changeAvailability(pnlCp.getCpEp(), 0, type);
				}
			}.start();			
		}
		else {
			logger.warn("CP:" + pnlCp.getCp().getCpNo() + " not connected, change availability to " + type + " will not sent out");
		}
	}
	
	private static void changeConfiguration(CpPanel pnlCp, String key, String value) {
		if(pnlCp == null) {
			logger.warn("changeConfiguration() pnlCp is null");
			return;			
		}
		
		if(pnlCp.isCpConnected()) {
			new Thread() {
				public void run() {
					CpWebSocket.changeConfiguration(pnlCp.getCpEp(), key, value);
				}
			}.start();			
		}
		else {
			logger.warn("CP:" + pnlCp.getCp().getCpNo() + " not connected, change configuration, key:" + key 
					+ ", value:" + value + " will not sent out");
		}
	}

	@OnMessage
	public void onMessage(String message) {
		lastMsgDttm = System.currentTimeMillis();
		logger.info(getSessionId() + "Received msg: " + message);
		
		try {
			WsResponse resp = GsonUtil.fromJsonDatetime(message, WsResponse.class);
			if(WsAction.isCsAction(resp.getAction())) {	//request from CS
				
				CtRequest req = GsonUtil.fromJsonDatetime(message, CtRequest.class);
				CtModel ct = CtUtil.getCt();
				CpModel cp = null;
				CpPanel pnlCp = null;
				if(!StringUtil.isEmpty(req.getCpNo())) {
					pnlCp = CtrlPanel.getCpPanelByNo(req.getCpNo());
					if(pnlCp != null) {
						cp = pnlCp.getCp();
					}
				}
				logger.info(getSessionId() + "Action:" + req.getAction());
				
				resp = WsResponse.createResponse(req);
				int resultCode = 0;
				String resultDesc = "";
				
				switch(req.getAction()) {
				case UploadConfig:
					updateConfig(req.getData());
					break;
				case EnableCp:
					if(pnlCp != null) {
						cp.setEnabled(true);
						CtrlPanel.updateUi(pnlCp);
						changeAvailablility(pnlCp, AvailabilityType.Operative);
					}
					else {
						resultDesc = "CP not found:" + req.getCpNo();
					}
					break;
				case DisableCp:
					if(pnlCp != null) {
						cp.setEnabled(false);
						CtrlPanel.updateUi(pnlCp);
						changeAvailablility(pnlCp, AvailabilityType.Inoperative);
					}
					else {
						resultDesc = "CP not found:" + req.getCpNo();
					}
					break;
				case StartCharging:
					if(pnlCp != null) {
						pnlCp.remoteStartTransaction();
					}
					else {
						resultDesc = "CP not found:" + req.getCpNo();
					}
					break;
				case StopCharging:
					if(pnlCp != null) {
						pnlCp.remoteStopTransaction();
					}
					else {
						resultDesc = "CP not found:" + req.getCpNo();
					}
					break;
				case EnableCt:
					ct.setEnabled(true);
					CtrlPanel.updateCtUi();
					
					for(CpModel cpm : ct.getCpList()) {
						changeAvailablility(CtrlPanel.getCpPanelByNo(cpm.getCpNo()), AvailabilityType.Operative);
					}
					break;
				case DisableCt:
					ct.setEnabled(false);
					CtrlPanel.updateCtUi();
					for(CpModel cpm : ct.getCpList()) {
						changeAvailablility(CtrlPanel.getCpPanelByNo(cpm.getCpNo()), AvailabilityType.Inoperative);
					}
					break;
				case RebootCt:
					CtrlPanel.rebootCt();
					break;
				}
				
				resp.setResultCode(resultCode);
				if(!StringUtil.isEmpty(resultDesc)) {
					resp.setResultDesc(resultDesc);
					logger.warn("Action " + req.getAction() + " failed!, " + resultDesc);
				}
				
				String js = GsonUtil.toJson(resp);
				sendMessage(js);
				
				if(resp.isSuccess() && req.getAction() != WsAction.UploadConfig && req.getAction() != WsAction.RebootCt) {
					CtUtil.saveConfig();
				}
			}
			else {	//response from CS
				if(!StringUtil.isEmpty(resp.getId()) && resp.isSuccess()) {
					CtRequestPool.removeFromPool(resp.getId());
				}
				
				switch(resp.getAction()) {
				case GetConfig:
					updateConfig(resp.getData());					
					break;
				}
			}			
		} catch(Exception e) {
			logger.error("Failed to process msg", e);
		}
	}
	
	public static String getSessionId() {
		return session == null ? "[-] " : "[" + session.getId() + "] ";
	}
	
	private static void updateConfig(String data) throws IOException {
		if(StringUtil.isEmpty(data)) {
			logger.warn("Missing config data");
		}
		else {
			String prevAdminCard = null;
			BigDecimal prevMaxPowerOutput = null;
			if(CtUtil.getServConfig() != null) {
				prevAdminCard = CtUtil.getServConfig().getAdminCardList();
				prevMaxPowerOutput = CtUtil.getServConfig().getMaxPowerOutput();
			}
			
			ServConfig cfg = GsonUtil.fromJsonDatetime(data, ServConfig.class);
			CtUtil.setServConfig(cfg);
			CtUtil.saveServConfig();
			boolean enabled = "Y".equalsIgnoreCase(cfg.getEnableFlag());
			if(enabled != CtUtil.getCt().isEnabled()) {
				CtUtil.getCt().setEnabled(enabled);
				CtUtil.saveCurrentCt();
				CtrlPanel.updateCtUi();
			}
			
			boolean adminCardChanged = (prevAdminCard == null && cfg.getAdminCardList() != null) || 
					(prevAdminCard != null && !prevAdminCard.equals(cfg.getAdminCardList()));
			logger.info(getSessionId() + "CT config updated, adminCardChanged:" + adminCardChanged);
			//@20180326
			if(adminCardChanged) {
				for(CpModel cpm : CtUtil.getCt().getCpList()) {
					changeConfiguration(CtrlPanel.getCpPanelByNo(cpm.getCpNo()), "adminCard", cfg.getAdminCardList());
				}
			}
			
			//CK @ 20191227, notify LMS if max power output changed
			boolean maxPowerOutputChanged = (prevMaxPowerOutput == null && cfg.getMaxPowerOutput() != null) ||
					(prevMaxPowerOutput != null && cfg.getMaxPowerOutput() != null && prevMaxPowerOutput.compareTo(cfg.getMaxPowerOutput()) != 0);
			logger.info(getSessionId() + "CT config updated, maxPowerOutput changed:" + maxPowerOutputChanged);
			if(maxPowerOutputChanged) {
				LmsServEvent e = CtClient.CUR_INST.newLmsEvent(LmsServEventType.UpdateZoneCurCapConfig, null);
				e.addParm(LmsCons.PARM_ZONE_CAP, cfg.getMaxPowerOutput().floatValue());
				CtClient.CUR_INST.triggerLmsEvent(e);
			}
		}
	}
	
	public static boolean isConnected() {
		return isConnected(true);
	}
	
	public static boolean isConnected(boolean log) {
		boolean res = session != null && session.isOpen() && lastMsgDttm != -1 
				&& (System.currentTimeMillis() - lastMsgDttm <= (CtUtil.getHeartbeatIntervalMs() + RECONN_ALLOWANCE_MS ));
		if(log) {
			logger.info(getSessionId() + "Connected:" + res + ", session:" + (session==null ? "null" : (session.isOpen() ? "Open" : "Not open")) 
					+ ", lastMsgDttm:" + lastMsgDttm + ", diffMs:" + (System.currentTimeMillis() - lastMsgDttm));
		}
		return res;
	}
	
	private static synchronized void connect() {
		if(isConnected(false)) {
			return ;
		}
		
		logger.info("Connecting to server...");
		WebSocketContainer container = null;//
		String url = CtUtil.getConfig().getWebSocketUrl() + CtUtil.getConfig().getCtId() + "?cipher=" + encryptedOCPPText;
		//url = "ws://test.evpay.com.hk:8082/ev/ctws/2?cipher=WB0raack68vPtxskGyOAKoeUSZDKiVdxd8ywnbRNWGYxej241RQbBuAmViywlDKpP2GLZixvxIJoucX6oKiGlA==";
		logger.info("URL:" + url);
		try {
			// Tyrus is plugged via ServiceLoader API. See notes above
			container = ContainerProvider.getWebSocketContainer();

			session = container.connectToServer(CtWebSocketClient.class, URI.create(url));
			lastMsgDttm = System.currentTimeMillis();
			logger.info(getSessionId() + "Connected, bufferSize:" + session.getMaxTextMessageBufferSize());
			//send the CT status
			updateCt();
			
			//also get the latest config
			getConfig();
			
			resendRequest();
		} catch (Exception e) {
			logger.error("Failed to open web socket to backend:" + url + ", error:" + e.getMessage());
		}
	}
	
	public static void resendRequest() {
		if(CtRequestPool.getTranPool().size() > 0) {
			ES.shutdown();
			ES = Executors.newFixedThreadPool(1);
			final LinkedHashMap<String, CtRequest> map = new LinkedHashMap<String, CtRequest>(CtRequestPool.getTranPool());
			logger.info(getSessionId() + "Resending Tran, size:" + map.size());
			new Thread() {
				public void run() {
					for(CtRequest r : map.values()) {
						try {
							logger.info("Resending " + r);
							sendRequest(r, false);
							
							//CK @ 20180615, add some delay for sending messages
							logger.info("Send next request after 1 sec");
							Thread.sleep(1000);
						}catch(Exception e) {
							logger.error("Failed to resend " + r, e);
						}
					}
				}
			}.start();
		}
		else {
			logger.info("resendRequest(), No Tran in pool");
		}
		if(CtRequestPool.getRequestPool().size() > 0) {
			final LinkedHashMap<String, CtRequest> map = new LinkedHashMap<String, CtRequest>(CtRequestPool.getRequestPool());
			logger.info(getSessionId() + "Resending request, size:" + map.size());
			new Thread() {
				public void run() {
					for(CtRequest r : map.values()) {
						try {
							logger.info("Resending " + r);
							sendRequest(r, false);
							
							//CK @ 20180615, add some delay for sending messages
							logger.info("Send next request after 1 sec");
							Thread.sleep(1000);
						}catch(Exception e) {
							logger.error("Failed to resend " + r, e);
						}
					}
				}
			}.start();
		}
		else {
			logger.info("resendRequest(), No request in pool");
		}
	}
	
	public static void closeSession() {
		 logger.info(getSessionId() + "Closing session");
		if (session != null) {
			try {
				session.close();
				logger.info(getSessionId() + "session closed");
			} catch (Exception e) {
				logger.error("Failed to close socket", e);
			}
		}
		session = null;
	}
	
	public static boolean heartbeat() {
		return sendRequest(WsAction.Heartbeat, null);
	}
	
	public static boolean updateCt() {
		return sendRequest(WsAction.UpdateCt, CtUtil.getCt());
	}
	
	public static boolean uploadIUCEvent(IucEventLogDto log) {
		return sendRequest(WsAction.IUCEvent, log);
	}
	
	public static boolean updateCp(CpModel cp) {
		return sendRequest(WsAction.UpdateCp, cp, cp.getCpNo());
	}
	
	public static boolean uploadCpEvent(RequestObj ro, CpModel cp) {
		return sendRequest(WsAction.UploadCpEvent, ro, cp.getCpNo());
	}
	
	public static boolean uploadOctEvent(OctEventData data) {
		return sendRequest(WsAction.OctEvent, data, null);
	}
	
	public static boolean uploadAlert(String uuid, String refNo, String cpNo, Throwable e) {
		String errMsg = null;
		if(e != null) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			
			errMsg = sw.toString();
			if(errMsg.length() > 5000) {
				errMsg = errMsg.substring(0,  5000);
			}
			
			OctUtil.logger.error(refNo + ", cp:" + cpNo, e);
		}
		else {
			OctUtil.logger.error(refNo + ", cp:" + cpNo);
		}

		AlertModel am = new AlertModel(refNo, uuid, errMsg);
		return sendRequest(WsAction.UploadAlert, am, cpNo);
	}
	
	public static boolean getConfig() {
		return sendRequest(WsAction.GetConfig, null);
	}
	
	public static boolean uploadTran(TranModel tm) {
		return sendRequest(WsAction.UploadTran, tm);
	}
	
	public static boolean sendRequest(WsAction action, Object data) {
		return sendRequest(action, data, null);
	}
	
	public static boolean sendRequest(WsAction action, Object data, String cpNo) {
		CtRequest req = new CtRequest(action);
		if(!StringUtil.isEmpty(cpNo)) {
			req.setCpNo(cpNo);
		}
		
		if(data != null) {
			req.setData(GsonUtil.toJson(data));
		}
		try {
			CtRequestPool.addToPool(req);
			sendRequest(req);
			return true;
		} catch (IOException e) {
			logger.error(getSessionId() + "Failed to send request:" + action, e);
		}
		return false;
	}
	
	public static void sendRequest(CtRequest req) throws IOException {
		sendRequest(req, true);
	}
	
	public static void sendRequest(CtRequest req, boolean checkConnect) throws IOException {
		if(checkConnect) {
			if(!isConnected()) {
				//CK @ 20190625 Move to thread to avoid network instable issues caused CP disconnected. 
				new Thread() {
					@Override
					public void run() {
						connect();
					}
				}.start();
				//connect();
			}
		}
		
		String js = GsonUtil.toJson(req, true);
		sendMessage(js);
	}
	
	public static void sendMessage(final String msg) {
		ES.execute(new Runnable() {			
			@Override
			public void run() {
				try {
					sendMessageHelper(msg);
				} catch (IOException e) {
					logger.error(getSessionId() + "Failed to send msg:" + msg, e);
				}
			}
		});
	}
	
	private static void sendMessageHelper(String msg) throws IOException {
		if(isConnected()) {
			//CK @ 20180309, fix multiple thread write problem for tomcat
			//session.getBasicRemote().sendText(msg);			
			synchronized (session) {
				logger.info(getSessionId() + "Sending msg:" + msg);
				session.getBasicRemote().sendText(msg);
			}
		}
		else {
			logger.warn(getSessionId() + "Web socket not connected");
		}
	}
	

	public static void main(String[] args) {
		System.out.println();
		
		try {
			connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}