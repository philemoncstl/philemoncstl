package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;
import com.jcraft.jsch.ConfigRepository.Config;

import hk.com.cstl.evcs.lms.CpChargingProfile;
import hk.com.cstl.evcs.lms.TxDefaultProfilePara;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.CpWebSocketServEndpoint;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.msg.cs.ChargingProfile;
import hk.com.cstl.evcs.ocpp.msg.cs.ChargingSchedule;
import hk.com.cstl.evcs.ocpp.msg.cs.ChargingSchedulePeriod;
import hk.com.cstl.evcs.ocpp.msg.cs.RemoteStartTransaction;
import hk.com.cstl.evcs.ocpp.msg.cs.RemoteStartTransactionRequestData;
import hk.com.cstl.evcs.ocpp.msg.cs.RemoteStopTransaction;
import hk.com.cstl.evcs.ocpp.msg.cs.RemoteStopTransactionRequestData;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.ws.CtOcppHandler;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class CpPanel extends CommonPanel{
	private static final Logger logger = Logger.getLogger(CpPanel.class);
	
	private static Dimension DIM = null;
	
	private static Map<ChargePointStatus, BufferedImage> STATUS_MAP = null;
	private static BufferedImage UNKNOWN_IMAGE = null;
	
	private CpModel cp;
	
	private CpWebSocketServEndpoint cpEp;
	
	private I18nLabel lblTitle;
	private I18nLabel lblStatus;
	
	private boolean cpConnected = false;
	
	public CpPanel(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
				
		setLayout(null);
		lblTitle = new I18nLabel();
		LangUtil.setFont(lblTitle, Font.PLAIN, 22);
		lblTitle.setForeground(Color.WHITE);
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setBounds(0, 8, getCpDim().width, 32);
		add(lblTitle);
		
		lblStatus = new I18nLabel();
		LangUtil.setFont(lblStatus, Font.PLAIN, 18);
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		lblStatus.setBounds(0, getCpDim().height - 38, getCpDim().width, 30);
		add(lblStatus);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		logger.info("****setEnabled:" + enabled + ", CP:" + cp.getCpNo());
	}
	
	public void remoteStartTransaction() {
		if(cp == null) {
			return;
		}
		
		final TranModel tran = cp.getTran();
		CtUtil.saveCurrentCt();	//update the tran id
		final RemoteStartTransaction req = new RemoteStartTransaction();
		RemoteStartTransactionRequestData reqData = new RemoteStartTransactionRequestData();
		req.setRequestData(reqData);
		
		reqData.setIdTag(tran.getIdTag());
		ChargingProfile profile = new ChargingProfile();
		reqData.setChargingProfile(profile);
		
		profile.setChargingProfileId(tran.getTranId());
		profile.setTransactionId(tran.getTranId());
		profile.setValidFrom(DateUtil.formatTimestamp(tran.getStartDttm()));
		profile.setValidTo(DateUtil.formatTimestamp(tran.getEndDttm()));
		ChargingSchedule schedule = new ChargingSchedule();
		profile.setChargingSchedule(schedule);
		
		//CK @ 20190818, duration no need to set for postpaid
		if(CtUtil.isModePrepaid(tran)) {
			schedule.setDuration(tran.getDurationMin() * 60);
		}
		
		//CK @ 20200113, set default charging profile for LMS enabled, keep org logic if not enabled
		CtConfig cfg = CtUtil.getConfig();
		if(cfg.isLms()) {
			TxDefaultProfilePara defaultPara = new TxDefaultProfilePara(cfg.getCtId());
			
			//Copy from LMS
			ChargingSchedulePeriod[] schedulePeriods = new ChargingSchedulePeriod[defaultPara.SCHEDULE_PERIOD_SIZE];
			
			schedulePeriods[0] = new ChargingSchedulePeriod();
			schedulePeriods[0].setLimit(defaultPara.CURRENT_LIMIT1);
			schedulePeriods[0].setStartPeriod(defaultPara.START_PERIOD1);
			schedulePeriods[0].setNumberPhases(defaultPara.getNumPhaseS1());
			
			if(defaultPara.SCHEDULE_PERIOD_SIZE>1) {
				schedulePeriods[1] = new ChargingSchedulePeriod();
				schedulePeriods[1].setLimit(defaultPara.CURRENT_LIMIT2);
				schedulePeriods[1].setStartPeriod(defaultPara.START_PERIOD2);
				schedulePeriods[1].setNumberPhases(defaultPara.getNumPhaseS2());						
			}
			schedule.setChargingSchedulePeriod(schedulePeriods);
		}
		else {
			ChargingSchedulePeriod schedulePeriod = new ChargingSchedulePeriod();
			schedulePeriod.setStartPeriod(tran.getDurationMin() * 60);
			schedule.setChargingSchedulePeriod(new ChargingSchedulePeriod[] {schedulePeriod});
		}
		
		logger.info("Sending RemoteStartTransaction to " + cp.getCpNo() + ", tran:" + tran);
		if(cpEp != null) {
			cpEp.sendRequest(req);
		}
		else {
			logger.warn("CP not connected:" + cp.getCpNo());
		}
		
		new Thread() {
			public void run() {
				try {
					long expTime = System.currentTimeMillis() + config.getCheckOperationResultTimeoutMs();
					CpPanel.this.setEnabled(false);
					while(System.currentTimeMillis() < expTime) {
						if(req.getHandledDttm() != null) {
							CtOcppHandler.removeRequest(req.getUniqueId());
							
							if(req.isResponseSuccess()) {
								cp.setTran(tran);								
								CtUtil.saveCurrentCt();							
							}
							break;
						}
						
						sleep(20);
					}
					
					if(req.getHandledDttm() == null) {
						logger.warn("RemoteStartTransaction timeout:" + tran);
					}
				} catch(Exception e) {
					logger.error("Failed to check RemoteStartTransaction result:" + e.getMessage(), e);
				} finally {
					CpPanel.this.setEnabled(true);
				}
			}
		}.start();		
	}
	
	
	public void remoteStopTransaction() {
		if(cp == null) {
			return;
		}
		
		final RemoteStopTransaction req = new RemoteStopTransaction();
		RemoteStopTransactionRequestData reqData = new RemoteStopTransactionRequestData();
		reqData.setTransactionId(cp.getTran() == null ? 0 : cp.getTran().getTranId());
		req.setRequestData(reqData);
		
		logger.info("Sending RemoteStopTransaction to " + cp.getCpNo() + ", tran:" + cp.getTran());
		if(cpEp != null) {
			cpEp.sendRequest(req);
		}
		
		new Thread() {
			public void run() {
				try {
					long expTime = System.currentTimeMillis() + config.getCheckOperationResultTimeoutMs();
					CpPanel.this.setEnabled(false);
					while(System.currentTimeMillis() < expTime) {
						if(req.getHandledDttm() != null) {
							CtOcppHandler.removeRequest(req.getUniqueId());
							
							if(req.isResponseSuccess()) {
								//removed @ 2018-05-09 for postpaid
								//cp.setTran(null);
							}
							break;
						}
						
						sleep(20);
					}
					
					if(req.getHandledDttm() == null) {
						logger.warn("RemoteStopTransaction timeout:" + cp.getTran());
					}
				} catch(Exception e) {
					logger.error("Failed to check RemoteStopTransaction result:" + e.getMessage(), e);
				} finally {
					CpPanel.this.setEnabled(true);
				}
			}
		}.start();		
	}
	
	public boolean isCpEnabled() {
		//logger.info("cp != null:" + (cp != null) + ", ct.isEnabled():" + ct.isEnabled());
		return cp != null && ct.isEnabled() && cp.isEnabled();
	}
	
	public void setCpConnected(boolean cpConnected) {
		this.cpConnected = cpConnected;
	}
	
	public void checkCpConnection() {
		//assume last received message within heartbeat interval + 10 seconds (allowance) = "Connected" 
		boolean conn = cp != null && cp.getLastReceivedDttm() != null && 
				(System.currentTimeMillis() - cp.getLastReceivedDttm().getTime() <= CtUtil.getHeartbeatIntervalMs() + CtWebSocketClient.RECONN_ALLOWANCE_MS);
		boolean changed = conn != this.cpConnected;
		if(changed) {
			logger.info((cp == null ? "" : cp.getCpNo()) +  " ws conn changed, old:" + this.cpConnected + ", new:" + conn);
			logger.info("conn:" + conn + ",cpEp != null:" + (cpEp != null)  + ", ws open:" 
					+ (cpEp != null && cpEp.getSession().isOpen()) 
					+ ", lastReceivedDttm != null:" + (cp != null && cp.getLastReceivedDttm() != null));
		}
		setCpConnected(conn);
		if(changed) {
			CtrlPanel.updateUi(this);
			
			if(!conn) {
				//set to offline and send back to server
				setCpStatus(ChargePointStatus.Unavailable);
			}
		}
	}
	
	public void setCpStatus(ChargePointStatus status) {
		cp.setStatus(status);
		cp.setStatusDttm(new Date());
		CtWebSocketClient.updateCp(cp);
		//CtUtil.saveCurrentCt();		
	}
	
	public boolean isCpConnected() {
		return this.cpConnected;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		getStatusImageMap();
		
		if(cp != null) {
			//lblTitle.setMsgCode("cpTitle");
			//CK @ 20200116, not showing the text "CP."
			lblTitle.setMsgCode("i18nLabel");	//empty
			lblTitle.setParms(cp.getCpNo());
			
			ChargePointStatus status = ChargePointStatus.Unavailable;
			if(isCpEnabled()) {
				if(isCpConnected()) {
					status = cp.getStatus();
				}
				else {
					lblStatus.setMsgCode("");
					if(UNKNOWN_IMAGE != null) {
						g.drawImage(UNKNOWN_IMAGE, 0, 0, null);				
					}
					return;
				}
			}
						
			lblStatus.setMsgCode("cpStatus" + status);
			if(getStatusImageMap().containsKey(status)) {
				g.drawImage(getStatusImageMap().get(status), 0, 0, null);
			}
		}
		else {
			lblTitle.setMsgCode("");
			lblStatus.setMsgCode("");			
			if(UNKNOWN_IMAGE != null) {
				g.drawImage(UNKNOWN_IMAGE, 0, 0, null);				
			}
		}
	}
	
	@Override
	public Dimension getPreferredSize() {		
		return getCpDim();
	}
	
	private Dimension getCpDim() {
		if(DIM == null) {
			/*Insets si = config.getScreenInset();
			int w = (config.getCtWidth() - si.left - si.right - config.getCpHgap() * 6) / 5;
			int h = (config.getCtHeight() - si.top - si.bottom - config.getNorthHeight() - config.getSouthHeight()
					- config.getCpVgap() * 3) / 2;
			DIM = new Dimension(w, h);*/
			DIM = new Dimension(config.getCpWidth(), config.getCpHeight());
			logger.info("CP Dim:" + DIM);
		}
		
		return DIM;
	}

	public CpModel getCp() {
		return cp;
	}

	public void setCp(CpModel cp) {
		this.cp = cp;
	}

	public CpWebSocketServEndpoint getCpEp() {
		return cpEp;
	}

	public void setCpEp(CpWebSocketServEndpoint cpEp) {
		this.cpEp = cpEp;
	}
	
	public static synchronized Map<ChargePointStatus, BufferedImage> getStatusImageMap() {
		if(STATUS_MAP == null) {
			logger.info("Loading CP status image map...");
			STATUS_MAP = new HashMap<ChargePointStatus, BufferedImage>();
			
			File path = null;
			for(ChargePointStatus cpStatus : ChargePointStatus.values()) {
				path = new File("img/cpStatus" + String.valueOf(cpStatus) + ".png");
				if(path.exists()) {
					try {
						STATUS_MAP.put(cpStatus, ImageIO.read(path));
						//logger.info("CP status image " + path.getName() + " loaded");
					} catch (Exception e) {
						logger.error("CP status image " + path.getName() + " failed to load", e);
					}
				}
				else {
					logger.warn("CP status image " + String.valueOf(cpStatus) + " not exists.");
				}				
			}
			
			path = new File("img/cpStatusUnknown.png");
			if(path.exists()) {
				try {
					UNKNOWN_IMAGE = ImageIO.read(path);
					logger.info("CP status image " + path.getName() + " loaded");
				} catch (Exception e) {
					logger.error("CP status image " + path.getName() + " failed to load", e);
				}
			}
			else {
				logger.warn("CP status image unknown not exists.");
			}
		}
		
		return STATUS_MAP;
	}
}
