package hk.com.evpay.ct.tool;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.ckzone.util.GsonUtil;
import com.ckzone.util.ReflectionUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.util.CtUtil;

public class TranHistCtrl {
	private static Logger logger = Logger.getLogger(TranHistCtrl.class);
	
	public static final int MAX_KEEP = 300;
	
	public static final String FILE_NAME = "tran_hist.json";
	
	private static TranHist HIST;
	
	public static TranHist getTranHist(){
		if(HIST == null) {
			loadTranHist();
		}
		
		return HIST;
	}
	
	public static void add(TranModel tran) {
		new Thread() {
			@Override
			public void run() {
				try {
					addHelp(tran);
				} catch (Exception e) {
					logger.error("Failed to add tran, cp:" + tran.getCpNo() + ", receipt:" + tran.getReceiptNo() + ", amt:" + tran.getAmt(), e);
				}
			}
		}.start();
	}
	
	public static void addHelp(TranModel tran) throws Exception {
		logger.info("Add tran, receipt:" + tran.getReceiptNo() + ", mode:" + tran.getMode());
		TranModel copy = new TranModel();
		ReflectionUtil.copyProperties(copy, tran);
		logger.info("Add tran, cp:" + copy.getCpNo() + ", receipt:" + copy.getReceiptNo() + ", amt:" + copy.getAmt());
		getTranHist().getTrans().add(0, copy);
		if(HIST.getTrans().size() > MAX_KEEP) {
			TranModel tm = HIST.getTrans().remove(MAX_KEEP);
			logger.info("Remove tran, cp:" + tm.getCpNo() + ", receipt:" + tm.getReceiptNo() + ", amt:" + tm.getAmt() + ", size:" + HIST.getTrans().size());
		}
		saveHelper();
	}
	
	public static synchronized void loadTranHist() {
		logger.info("Loading tran hist from " + FILE_NAME);
		File f = CtUtil.getConfigFile(FILE_NAME);
		HIST = new TranHist();
		if(f.exists()) {
			try {
				TranHist TempH = GsonUtil.fromJson(f, TranHist.class);
				Vector<TranModel> tnl = new Vector<>();
				for(TranModel t : TempH.getTrans()) {
					if(!"Void".equals(t.getTranStatusCode())) {
						tnl.add(t);
					}
				}
				HIST.setTrans(tnl);
				logger.info("Tran hist loaded, count:" + HIST.getTrans().size());
			} catch (IOException e) {
				logger.error("Failed to load tran hist.", e);
				HIST = new TranHist();
			}
		}
		else {
			logger.warn("Tran hist not exists, create a default one now");
			HIST = new TranHist();
		}
	}
	
	public static void save() {
		new Thread() {
			@Override
			public void run() {
				saveHelper();
			}
		}.start();
	}
	
	public static synchronized boolean saveHelper() {
		logger.info("Saving " + FILE_NAME);
		boolean res = GsonUtil.saveJson(CtUtil.getConfigFile(FILE_NAME).getPath(), HIST);
		logger.info("Res:" + res);
		return res;
	}
	
	
}
