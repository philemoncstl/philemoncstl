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
	
	private static CtRequestPoolData POOL_DATA;
	
	public static LinkedHashMap<String, CtRequest> getRequestPool(){
		if(POOL_DATA == null) {
			loadRequest();
		}
		
		return POOL_DATA.getRequestMap();
	}
	
	public static boolean addToPool(CtRequest req) {
		if(req.getAction() == WsAction.UploadTran || req.getAction() == WsAction.UploadCpEvent 
				|| req.getAction() == WsAction.UploadAlert) {
			getRequestPool().put(req.getId(), req);
			logger.info("Added " + req + ", size:" + getRequestPool().size());
			saveRequest();
			return true;
		}
		else {
			return false;
		}
	}
	
	public static boolean removeFromPool(String reqId) {
		if(getRequestPool().containsKey(reqId)) {
			CtRequest req = getRequestPool().remove(reqId);
			logger.info("Removed " + req + ", size:" + getRequestPool().size());
			saveRequest();
			return true;
		}
		else {
			return false;
		}
	}
	
	public static synchronized void loadRequest() {
		logger.info("Loading request pool from " + POOL_FILE_NAME);
		File f = CtUtil.getConfigFile(POOL_FILE_NAME);
		if(f.exists()) {
			try {
				POOL_DATA = GsonUtil.fromJson(f, CtRequestPoolData.class);
				
				//CK @ 2019-01-04, fix in case the file "ct_request.json" is corrupted
				if(POOL_DATA == null) {
					POOL_DATA = new CtRequestPoolData();
				}
			} catch (IOException e) {
				logger.error("Failed to load request pool.", e);
				POOL_DATA = new CtRequestPoolData();
			}
		}
		else {
			logger.warn("Request pool not exists, create a default one now");
			POOL_DATA = new CtRequestPoolData();
		}
	}
	
	public static void saveRequest() {
		new Thread() {
			@Override
			public void run() {
				saveRequestHelper();
			}
		}.start();
	}
	
	public static synchronized boolean saveRequestHelper() {
		logger.info("Saving " + POOL_FILE_NAME);
		boolean res = GsonUtil.saveJson(CtUtil.getConfigFile(POOL_FILE_NAME).getPath(), POOL_DATA);
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
