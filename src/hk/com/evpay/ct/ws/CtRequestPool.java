package hk.com.evpay.ct.ws;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.ckzone.util.GsonUtil;

import hk.com.cstl.evcs.wsobj.CtRequest;
import hk.com.cstl.evcs.wsobj.WsAction;
import hk.com.evpay.ct.util.CtUtil;

public class CtRequestPool {
	private static Logger logger = Logger.getLogger(CtRequestPool.class);
	
	public static final String POOL_FILE_NAME = "ct_request.json";
	public static final String TRAN_FILE_NAME = "ct_request_tran.json";
	
	private static CtRequestPoolData POOL_DATA;
	private static CtRequestPoolData POOL_TRAN_DATA;
	
	public static LinkedHashMap<String, CtRequest> getRequestPool(){
		if(POOL_DATA == null) {
			loadRequest(false);
		}
		
		return POOL_DATA.getRequestMap();
	}
	
	public static LinkedHashMap<String, CtRequest> getTranPool(){
		if(POOL_TRAN_DATA == null) {
			loadRequest(true);
		}
		
		return POOL_TRAN_DATA.getRequestMap();
	}

	
	public static boolean addToPool(CtRequest req) {
		if(req.getAction() == WsAction.UploadCpEvent 
				|| req.getAction() == WsAction.UploadAlert ||req.getAction() ==  WsAction.OctEvent || req.getAction() ==WsAction.IUCEvent) {
			getRequestPool().put(req.getId(), req);
			logger.info("Added " + req + ", size:" + getRequestPool().size());
			saveRequest(false);
			return true;
		} else if(req.getAction() == WsAction.UploadTran) {
			getTranPool().put(req.getId(), req);
			logger.info("Added " + req + ", size:" + getRequestPool().size());
			saveRequest(true);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean removeFromPool(String reqId) {
		if(getRequestPool().containsKey(reqId)) {
			CtRequest req = getRequestPool().remove(reqId);
			logger.info("Removed " + req + ", size:" + getRequestPool().size());
			saveRequest(false);
			return true;
		} else if(getTranPool() != null && getTranPool().containsKey(reqId) ) {
			CtRequest req = getTranPool().remove(reqId);
			logger.info("Removed " + req + ", size:" + getTranPool().size());
			saveRequest(true);
			return true;
		}
		else {
			return false;
		}
	}
	
	public static void loadRequest(boolean isTran) {
		String fileName = POOL_FILE_NAME;
		if(isTran) {
			fileName = TRAN_FILE_NAME;
		}
		logger.info("Loading request pool from " + fileName);
		File f = CtUtil.getConfigFile(fileName);
		if(f.exists()) {
			try {
				if(isTran) {
					POOL_TRAN_DATA = GsonUtil.fromJson(f, CtRequestPoolData.class);
				} else {
					POOL_DATA = GsonUtil.fromJson(f, CtRequestPoolData.class);
				}
				
				//CK @ 2019-01-04, fix in case the file "ct_request.json" is corrupted
				if(isTran) {
					if(POOL_TRAN_DATA == null) {
						POOL_TRAN_DATA = new CtRequestPoolData();
					}
				} else {
					if(POOL_DATA == null) {
						POOL_DATA = new CtRequestPoolData();
					}
				}
			} catch (IOException e) {
				logger.error("Failed to load request pool.", e);
				if(isTran) {
					POOL_TRAN_DATA = new CtRequestPoolData();
				} else {
					POOL_DATA = new CtRequestPoolData();
				}
			}
		}
		else {
			if(isTran) {
				logger.warn("Request tran pool not exists, create a default one now");
				POOL_TRAN_DATA = new CtRequestPoolData();
			} else {
				logger.warn("Request pool not exists, create a default one now");
				POOL_DATA = new CtRequestPoolData();
			}

		}
	}
	
	public static void saveRequest(boolean isTran) {
		new Thread() {
			@Override
			public void run() {
				saveRequestHelper(isTran);
			}
		}.start();
	}
	
	public static synchronized boolean saveRequestHelper(boolean isTran) {
		boolean res = false;
		String sourse = POOL_FILE_NAME;
		CtRequestPoolData targer = POOL_DATA;
		if(isTran) {
			sourse = TRAN_FILE_NAME;
			targer = POOL_TRAN_DATA;
		}
		logger.info("Saving " + sourse);
		res = GsonUtil.saveJson(CtUtil.getConfigFile(sourse).getPath(), targer);
		logger.info("Res:" + res);
		return res;
	}
	
	static class CtRequestPoolData{
		private LinkedHashMap<String, CtRequest> requestMap = new LinkedHashMap<String, CtRequest>();
		
		public CtRequestPoolData() {
			
		}

		public LinkedHashMap<String, CtRequest> getRequestMap() {
			return requestMap;
		}

		public void setRequestMap(LinkedHashMap<String, CtRequest> requestMap) {
			this.requestMap = requestMap;
		}
	}
}
