package hk.com.evpay.ct.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ckzone.util.GsonUtil;

import hk.com.cstl.evcs.ct.PayConfig;
import hk.com.cstl.evcs.ct.PayMethodModel;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.model.CtModel;
import hk.com.cstl.evcs.model.EvCons;
import hk.com.cstl.evcs.model.ServConfig;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.CpWebSocket;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.cstl.evcs.ocpp.eno.TranStatus;
import hk.com.evpay.ct.CtConfig;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class CtUtil {
	private static Logger logger = Logger.getLogger(CtUtil.class);
	
	public static final String PAY_CONFIG_FILE_NAME = "pay_method.json";
	
	public static final String SERV_CONFIG_FILE_NAME = "config.json";
	public static final String LOCAL_CONFIG_FILE_NAME = "config_local.xml";
	public static final String CT_DATA_FILE_NAME = "ct.xml";
	public static final String CT_DATA_FILE_NAME_BAK = "ct.xml.bak";
	
	public static final String ADMIN_QR = "468e861f-5a49-440a-9e75-ed4cb491d219";
	
	
	private static CtModel CT = null;
	
	private static CtConfig CONFIG = null;
	
	/**
	 * Remote config, such as name, rate, etc.
	 */
	private static ServConfig SERV_CONFIG = null;
	
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
	
	private static PayConfig PAY_CONFIG = null;
	
	public static CtConfig getConfig() {
		if(CONFIG == null) {
			loadConfig();
		}
		return CONFIG;
	}
	
	public static synchronized CtConfig loadConfig() {
		File f = getConfigFile(LOCAL_CONFIG_FILE_NAME);
		if (f.exists()) {
			try {
				JAXBContext jc = JAXBContext.newInstance(CtConfig.class);
				Unmarshaller u = jc.createUnmarshaller();
				CONFIG = (CtConfig) u.unmarshal(f);				
				
			} catch (Exception e) {
				logger.error("Failed to load " + LOCAL_CONFIG_FILE_NAME, e);
			}				
		}
		else {
			logger.error("Missing file:" + f.getPath() + ", use default config.");
			CONFIG = new CtConfig();
			saveConfig();			
		}
		
		return CONFIG;
	}
	
	public static synchronized void saveConfig() {
		new Thread() {
			public void run() {
				saveConfig(CONFIG);
			}
		}.start();
	}
	
	public static synchronized boolean saveConfig(CtConfig cfg) {
		logger.info("Saving config...");
		boolean res = false;
		
		try {
			JAXBContext context = JAXBContext.newInstance(CtConfig.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getConfigFile(LOCAL_CONFIG_FILE_NAME)), "UTF-8"));
			m.marshal(cfg, out);
			res = true;
		} catch (Exception e) {
			logger.error("Failed to save config!", e);
		}
		logger.info("Save config:" + res);
		return res;
	}
	
	
	public static CtModel getCt() {
		if(CT == null) {
			CT = loadCt();
		}
		
		return CT;
	}
	
	public static CpModel getCp(String cpNo) {
		CtModel ct = getCt();
		if(ct != null) {
			for(CpModel cp : ct.getCpList()) {
				if(cp.getCpNo().equals(cpNo)) {
					return cp;
				}
			}
		}
		
		return null;
	}
	
	public static void setCt(CtModel ct) {
		CT = ct;
	}
	
	public static void saveCurrentCt() {
		new Thread() {
			@Override
			public void run() {
				try {
					File src = getConfigFile(CT_DATA_FILE_NAME);
					if(src.exists() && src.length() > 0) {
						FileUtils.copyFile(src, getConfigFile(CT_DATA_FILE_NAME_BAK));
					}
					else {
						logger.warn("Config not copied, exist:" + src.exists() + ", length:" + src.length());
					}
				} catch (IOException e) {
					logger.error("Failed to copy backup file", e);
				}
				saveCt(getCt());
			}
		}.start();
	}
	
	public static synchronized boolean saveCt(CtModel ct) {
		boolean res = false;
		
		try {
			JAXBContext context = JAXBContext.newInstance(CtModel.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getConfigFile(CT_DATA_FILE_NAME)), "UTF-8"));
			m.marshal(ct, out);
			res = true;
		} catch (Exception e) {
			logger.error("Failed to save ct!", e);
		}
		logger.info("Save CT:" + res);
		return res;
	}
	
	public static synchronized CtModel loadCt() {
		CtModel ct = null;
		File f = getConfigFile(CT_DATA_FILE_NAME);
		if (f.exists()) {
			logger.info("Loading CT Model...");
			try {
				JAXBContext jc = JAXBContext.newInstance(CtModel.class);
				Unmarshaller u = jc.createUnmarshaller();
				try {
					ct = (CtModel) u.unmarshal(f);
					logger.info("Loaded config:" + f.getPath());
				} catch(Exception e) {
					logger.error("Failed to load:" + CT_DATA_FILE_NAME, e);
					logger.info("Try to load from backup file:" + CT_DATA_FILE_NAME_BAK);
					f = getConfigFile(CT_DATA_FILE_NAME_BAK);
					ct = (CtModel) u.unmarshal(f);
					logger.info("Loaded config:" + f.getPath());
				}				
				
				//set to disconnected by default and Unavailable
				for(CpModel cp : ct.getCpList()) {
					cp.setConnected(false);
					cp.setStatus(ChargePointStatus.Unavailable);
				}
			} catch (Exception e) {
				logger.error("Failed to load " + f.getPath(), e);
			}				
		}
		else {
			logger.error("Missing file:" + f.getPath());
			System.exit(1);
		}
		
		return ct;
	}
	
	private static synchronized void loadServConfig() {		
		File f = getConfigFile(SERV_CONFIG_FILE_NAME);
		logger.info("Loading serv config from:" + f.getPath());
		if(f.exists()) {
			try {
				SERV_CONFIG = GsonUtil.fromJson(f, ServConfig.class);
				CpWebSocket.REQUEST_HEARTBEAT_INTERVAL_MS = SERV_CONFIG.getHeartbeatIntervalSec() * 1000;
				logger.info("***getHeartbeatIntervalSec:" + SERV_CONFIG.getHeartbeatIntervalSec());
			} catch (IOException e) {
				logger.error("Failed to load serv config.", e);
			}
		}
		else {
			logger.info("Try to load from server.");
			boolean res = CtWebSocketClient.getConfig();
			if(res) {
				try {
					logger.info("Wait for 5 seconds");
					Thread.sleep(5000);
				} catch (Exception e) {
					
				}
			}
		}
	}
	
	public static void setServConfig(ServConfig sc) {
		SERV_CONFIG = sc;		
	}
	
	public static synchronized void saveServConfig() {
		boolean res = GsonUtil.saveJson(getConfigFile(SERV_CONFIG_FILE_NAME).getPath(), SERV_CONFIG);
		logger.info("Save serv config:" + res);
	}
	
	public static ServConfig getServConfig() {
		if(SERV_CONFIG == null) {
			loadServConfig();
		}
		
		return SERV_CONFIG;
	}
	
	public static int getHeartbeatIntervalSec() {
		if(SERV_CONFIG != null && SERV_CONFIG.getHeartbeatIntervalSec() != null) {
			return SERV_CONFIG.getHeartbeatIntervalSec();
		}
		return 30;
	}
	
	public static long getHeartbeatIntervalMs() {
		return getHeartbeatIntervalSec() * 1000;
	}
	
	public static String getRemainingTime(Date endTime) {
		return getDuration(new Date(), endTime);
	}
	
	public static String getDuration(Date startTime) {
		return getDuration(startTime, new Date());
	}
	
	public static String getDuration(Date startTime, Date endTime) {
		if(startTime == null || endTime == null) {
			return ""; 
		}
		
		int[] res = null;
		if(startTime.getTime() >= endTime.getTime()) {	//already expired
			res = new int[] {0, 0};
		}
		else {
			res = getDurationInt(startTime, endTime);
		}
		String s = res[0] + " " + LangUtil.getMsg("hr") + " " + res[1] + " " + LangUtil.getMsg("min");
		/* + " " + res[2] + " " + LangUtil.getMsg("sec")*/
		return s;
	}
	
	public static int[] getDurationInt(Date startTime, Date endTime) {
		long st = startTime.getTime();
		long et = endTime == null ? System.currentTimeMillis() : endTime.getTime();
		long diffSec = (et - st) / 1000;
		int[] res = new int[3];
		res[0] = (int)(diffSec / 3600);
		res[1] = (int)(diffSec % 3600 / 60);
		res[2] = (int)(diffSec % 60);
		return res;
	}
	
	public static int getHourMinInt(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(date.getTime());
		
		return cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE);
	}
	
	public static int getMinutesBetweenDates(Date from, Date to) {
		return (int)((to.getTime() - from.getTime()) / 60000);
	}
	
	public static boolean isCpStatusCharging(ChargePointStatus status) {
		return status == ChargePointStatus.Charging || 
				status == ChargePointStatus.SuspendedEV ||
				status == ChargePointStatus.SuspendedEVSE;
	}
	
	public static boolean isCpStatusStopped(ChargePointStatus status) {
		return status == ChargePointStatus.Finishing;
	}
	
	public static int getReceiptNo() {
		int no = getCt().nextReceiptNo();
		
		CtUtil.saveCurrentCt();
		return getCt().getCtId() * 10000 + no;
	}
	
	public static boolean isContactlessBbpos() {
		return EvCons.CONTACTLESS_DEVICE_BBPOS.equals(getConfig().getContactlessDevice());
	}
	
	public static boolean isPayByContactless(TranModel tm) {
//		return String.valueOf(PayMethod.Contactless).equals(tm.getPayMethodCode());
		return String.valueOf(PayMethod.ContactlessGroup).equals(tm.getPayMethodCode());
	}
	
	public static boolean isPayByOctopus(TranModel tm) {
		return String.valueOf(PayMethod.Octopus).equals(tm.getPayMethodCode());
	}
	
	public static boolean isPayByQr(TranModel tm) {
		return String.valueOf(PayMethod.QR).equals(tm.getPayMethodCode());
	}
	
	public static boolean isTranStatusCharging(TranModel tm) {
		return String.valueOf(TranStatus.Charging).equals(tm.getTranStatusCode());
	}
	
	public static boolean isTranStatusStop(TranModel tm) {
		return String.valueOf(TranStatus.Stopped).equals(tm.getTranStatusCode());
	}
	
	public static String bigDecimalToString(BigDecimal bd) {
		if(bd == null) {
			return "";
		}
		
		return String.valueOf(bd.setScale(2, RoundingMode.DOWN));
	}
	
	public static boolean isTimeEnabled(TranModel tm) {
		return "Y".equalsIgnoreCase(tm.getTimeEnabled());
	}	
	public static boolean isOnOffPeakSameTimeRate(TranModel tm) {
		return tm.getTimeRateOffPeakPerUnit().compareTo(tm.getTimeRateOnPeakPerUnit()) == 0;
	}
	
	public static boolean isEnergyEnabled(TranModel tm) {
		return "Y".equalsIgnoreCase(tm.getEnergyEnabled());
	}	
	public static boolean isOnOffPeakSameEnergyRate(TranModel tm) {
		return tm.getEnergyRateOnPeak().compareTo(tm.getEnergyRateOffPeak()) == 0;
	}
	
	public static boolean isSameRate(TranModel tm) {
		return isOnOffPeakSameTimeRate(tm) && isOnOffPeakSameEnergyRate(tm);
	}
	
	public static File getConfigFile(String name) {
		return new File(System.getProperty("user.dir") + "/config/" + name);
	}
	
	public static String getAmountStr(int amt) {
		return "" + DECIMAL_FORMAT.format(amt*1.0/10);
	}
	
	
	public static boolean isModePrepaid() {
		return EvCons.MODE_PREPAID.equals(getServConfig().getMode());
	}
	
	public static boolean isModePrepaid(TranModel tran) {
		if(tran == null) {
			return false;
		}
		
		return EvCons.MODE_PREPAID.equals(tran.getMode());
	}
	
	public static void setMode(TranModel tran) {
		if(tran != null) {
			tran.setMode(isModePrepaid() ? EvCons.MODE_PREPAID : EvCons.MODE_POSTPAID);
		}
		else {
			logger.warn("tran is null!");
		}
	}
	
	
	public static synchronized void loadPayConfig() {		
		File f = getConfigFile(PAY_CONFIG_FILE_NAME);
		logger.info("Loading pay method config from:" + f.getPath());
		if(f.exists()) {
			try {
				PAY_CONFIG = GsonUtil.fromJson(f, PayConfig.class);
				if(PAY_CONFIG.getMethods() != null) {
					Collections.sort(PAY_CONFIG.getMethods());
					logger.info("Pay Methods:");
					for(PayMethodModel pm : PAY_CONFIG.getMethods()) {
						logger.info(" " + pm.getSeq() + "\t" + pm.getPayMethod() + "\t" + pm.isEnabled() + "\t" + pm.getIconName());
					}
				}
			} catch (Exception e) {
				logger.error("Failed to load pay method config.", e);
			}
		}
		else {
			logger.info("Config file not found:" + f.getPath());
			PAY_CONFIG = new PayConfig();
			PayMethodModel pm = new PayMethodModel();
			pm.setPayMethod(PayMethod.Octopus);
			PAY_CONFIG.getMethods().add(pm);
		}
	}
	
	public static synchronized boolean savePayConfig() {
		logger.info("Saving pay config...");
		boolean res = false;
		
		try {
			res = GsonUtil.saveJson(getConfigFile(PAY_CONFIG_FILE_NAME).getPath(), PAY_CONFIG);
			logger.info("Save pay config:" + res);
			} catch (Exception e) {
			logger.error("Failed to save pay config!", e);
		}
		logger.info("Save pay config:" + res);
		return res;
	}
	
	public static PayConfig getPayConfig() {
		if(PAY_CONFIG == null) {
			loadPayConfig();
		}
		
		return PAY_CONFIG;
	}
	
	public static void main(String[] args) {
		PayConfig pc = getPayConfig();
		/*
		 * pc.getMethods().clear(); int i = 1; for(PayMethod p : PayMethod.values()) {
		 * PayMethodModel pm = new PayMethodModel(); pm.setPayMethod(p);
		 * pm.setEnabled(true); pm.setSeq(i); pm.setIconName("pm_" + p.toString() +
		 * ".png"); pc.getMethods().add(pm); i++;}
		 * savePayConfig();
		 */
		System.out.println(pc.getMethods().size());
		
	}
}
