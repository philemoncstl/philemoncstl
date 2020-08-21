package hk.com.evpay.ct;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ckzone.octopus.ExtraInfo;
import com.ckzone.util.DateUtil;
import com.ckzone.util.StringUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.PrinterUtil;
import hk.com.evpay.ct.util.RateUtil;
import hk.com.evpay.ct.util.ReceiptCons;

public class Step3PrintReceipt extends CommonPanel{
	private static final Logger logger = Logger.getLogger(Step3PrintReceipt.class);
	
	private I18nLabel lblPrintReceipt;
	
	private I18nButtonLabel btnNo;
	private I18nButtonLabel btnYes;

	public Step3PrintReceipt(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		lblPrintReceipt = createLabel("printReceipt", "", 400, 115, 485, 486);
		LangUtil.setFont(lblPrintReceipt, Font.PLAIN, 40);
		add(lblPrintReceipt);
		
		btnNo = createButton("no", "img/btn_no.png", 180, 682);
		add(btnNo);
		
		btnYes = createButton("yes", "img/btn_yes.png", 990, 682);
		add(btnYes);
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object src = e.getSource();
				if(src == btnNo) {
					printReceipt(false);
				}
				else if(src == btnYes) {
					printReceipt(true);
				}
			}
		};
		btnNo.addMouseListener(ma);
		btnYes.addMouseListener(ma);
	}
	
	public void printReceipt(boolean print) {
		logger.info("printReceipt:" + print);
		
		TranModel tm = pnlCtrl.getPnlSelectedCp().getCp().getTran();		
		
		if(print) {
			printReceipt(tm, false);
			pnlCtrl.goToStep4GetReceipt();
		}
		else {
			if(CtUtil.isModePrepaid(pnlCtrl.getPnlSelectedCp().getCp().getTran())) {
				pnlCtrl.goToStep5ChargingRecord();
			}
			else {
				pnlCtrl.goToPostStep6ShowReceipt();
			}
		}
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_RECEIPT;
	}
	
	public static void printReceipt(TranModel tm, boolean reprint) {
		logger.info("Print receipt:" + tm.getReceiptNo());
		Map<String, String> map = new LinkedHashMap<String, String>();
		
		//20190623, add receipt heading
		String receiptHeading = CtUtil.getConfig().getReceiptHeading();
		if(!StringUtil.isEmpty(receiptHeading)) {
			map.put("merchantName", receiptHeading);
		}
		
		
		if(reprint) {
			map.put("reprintMessage", "Y");
			map.put("reprintDttm", DateUtil.getCurrentDateTimeStr());
		}
		
		map.put("receiptNo", tm.getReceiptNo());
		map.put("tranDttm", DateUtil.formatDateTime(tm.getTranDttm()));
		String loc = CtUtil.getConfig().getLocationReceipt();
		if(StringUtil.isEmpty(loc)) {
			loc = CtUtil.getServConfig().getName();
		}
		map.put("location", loc);
		map.put("cpNo", tm.getCpNo());
		map.put("startDttm", DateUtil.formatDateTime(tm.getStartDttm()));
		map.put("endDttm", DateUtil.formatDateTime(tm.getEndDttm()));
		BigDecimal duration = new BigDecimal(tm.getDurationMin()).divide(new BigDecimal(60), 2, RoundingMode.DOWN);
		map.put("duration", String.valueOf(duration));
		/*BigDecimal rate = tm.getTimeRateOffPeakPerUnit().multiply(new BigDecimal(60))
				.divide(new BigDecimal(tm.getChargingUnitMinutes()));*/
		
		if(CtUtil.isTimeEnabled(tm) && CtUtil.isEnergyEnabled(tm)) {
			map.put(ReceiptCons.ON_OFF_PEAK_SAME_RATE, CtUtil.isSameRate(tm) ? "Y" : "N");
		}
		
		//Time charge
		map.put(ReceiptCons.TIME_CHARGE_ENABLED, tm.getTimeEnabled());
		if(CtUtil.isTimeEnabled(tm)) {
			map.put(ReceiptCons.ON_OFF_PEAK_SAME_TIME_RATE, CtUtil.isOnOffPeakSameTimeRate(tm) ? "Y" : "N");
			
			//off-peak
			BigDecimal rate = RateUtil.getRatePerHour(tm.getTimeRateOffPeakPerUnit(), tm.getChargingUnitMinutes());
			map.put(ReceiptCons.TIME_CHARGE_RATE_OFF_PEAK, CtUtil.bigDecimalToString(rate));
			duration = new BigDecimal(tm.getDurationOffPeak()).divide(new BigDecimal(60), 2, RoundingMode.DOWN);
			map.put(ReceiptCons.DURATION_OFF_PEAK, String.valueOf(duration));
			map.put(ReceiptCons.TIME_CHARGE_OFF_PEAK, CtUtil.bigDecimalToString(tm.getTimeChargeOffPeak()));		
			//on-peak
			rate = RateUtil.getRatePerHour(tm.getTimeRateOnPeakPerUnit(), tm.getChargingUnitMinutes());
			map.put(ReceiptCons.TIME_CHARGE_RATE_ON_PEAK, CtUtil.bigDecimalToString(rate));
			duration = new BigDecimal(tm.getDurationOnPeak()).divide(new BigDecimal(60), 2, RoundingMode.DOWN);
			map.put(ReceiptCons.DURATION_ON_PEAK, String.valueOf(duration));
			map.put(ReceiptCons.TIME_CHARGE_ON_PEAK, CtUtil.bigDecimalToString(tm.getTimeChargeOnPeak()));
			//Total
			map.put(ReceiptCons.TIME_CHARGE, CtUtil.bigDecimalToString(tm.getTimeCharge()));
		}
		
		//Energy charge
		map.put(ReceiptCons.ENERGY_CHARGE_ENABLED, tm.getEnergyEnabled());
		if(CtUtil.isEnergyEnabled(tm)) {
			map.put(ReceiptCons.ON_OFF_PEAK_SAME_ENERGY_RATE, CtUtil.isOnOffPeakSameEnergyRate(tm) ? "Y" : "N");		
			//off-peak
			map.put(ReceiptCons.ENERGY_CHARGE_RATE_OFF_PEAK, CtUtil.bigDecimalToString(tm.getEnergyRateOffPeak()));
			map.put(ReceiptCons.ENERGY_CHARGE_OFF_PEAK, CtUtil.bigDecimalToString(tm.getEnergyChargeOffPeak()));
			map.put(ReceiptCons.ENERGY_CONSUMED_OFF_PEAK, CtUtil.bigDecimalToString(tm.getEnergyConsumedOffPeak()));
			//on-peak
			map.put(ReceiptCons.ENERGY_CHARGE_RATE_ON_PEAK, CtUtil.bigDecimalToString(tm.getEnergyRateOnPeak()));
			map.put(ReceiptCons.ENERGY_CHARGE_ON_PEAK, CtUtil.bigDecimalToString(tm.getEnergyChargeOnPeak()));
			map.put(ReceiptCons.ENERGY_CONSUMED_ON_PEAK, CtUtil.bigDecimalToString(tm.getEnergyConsumedOnPeak()));
			
			//total
			map.put(ReceiptCons.ENERGY_CONSUMED, CtUtil.bigDecimalToString(tm.getEnergyConsumed()));
			map.put(ReceiptCons.ENERGY_CHARGE, CtUtil.bigDecimalToString(tm.getEnergyCharge()));
		}
		
		map.put("amount", CtUtil.bigDecimalToString(tm.getAmt()));
		map.put("paymentType", tm.getPayMethodCode());
		
		if(CtUtil.isPayByOctopus(tm)) {
			map.put("deviceNo", tm.getOctopusDeviceNo());
			map.put("octopusNo", tm.getOctopusNo());
			map.put("amountDeducted", CtUtil.bigDecimalToString(tm.getAmt()));
			map.put("remainingValue", CtUtil.bigDecimalToString(tm.getRemainBal()));
			map.put(ReceiptCons.SMART_OCTOPUS, "Y".equals(tm.getSmartOctopus()) ? "Y" : "N");
			
			String infoChi = "";
			String infoEng = "";
			if(!StringUtil.isEmpty(tm.getOctopusLastAddValueDate()) && !StringUtil.isEmpty(tm.getOctopusLastAddValueType())) {				
				if(ExtraInfo.BY_CASH.equals(tm.getOctopusLastAddValueType())){
					infoEng = "Last add value by Cash on " + tm.getOctopusLastAddValueDate();
					infoChi = "上一次於 " + tm.getOctopusLastAddValueDate() + " 現金增值";
				}
				else if(ExtraInfo.BY_AAVS.equals(tm.getOctopusLastAddValueType())) {
					infoEng = "Last add value by AAVS on " + tm.getOctopusLastAddValueDate();
					infoChi = "上一次於 " + tm.getOctopusLastAddValueDate() + " 自動增值";
				}
				else if(ExtraInfo.BY_ONLINE.equals(tm.getOctopusLastAddValueType())){
					infoEng = "Last add value by Online on " + tm.getOctopusLastAddValueDate();
					infoChi = "上一次於 " + tm.getOctopusLastAddValueDate() + " 網上增值";
				}
				else {
					logger.info("No need to display last add value info, type:" + tm.getOctopusLastAddValueType());
				}
			}
			map.put("lastAddValueChi", infoChi);
			map.put("lastAddValueEng", infoEng);
			
			/*map.put("octopusMsgChi", "客戶如對上述的八達通交易有任何查詢，請保存此存根及致電八達通顧客服務熱線 2266 2222");
			map.put("octopusMsgEng", "If you have any queries on the above transaction, please keep the receipt and call Octopus Customer Service"
					+ PrinterUtil.PAGE_NEW_LINE + "Hotline at 2266 2222");*/
		}
		else if(CtUtil.isPayByContactless(tm)) {			
			map.put(ReceiptCons.CARD_TYPE, tm.getCardType());			
			map.put(ReceiptCons.CARD_NO, tm.getCardNo());
			map.put(ReceiptCons.MID, tm.getMerchantId());
			map.put(ReceiptCons.RRN, tm.getRetrievalRefNo());
			map.put(ReceiptCons.ECR_REF, tm.getEcrRef());
			map.put(ReceiptCons.EXP_DATE, tm.getCardExpiryDate());
			map.put(ReceiptCons.TID, tm.getTerminalId());
			map.put(ReceiptCons.BATCH_NO, tm.getBatchNo());
			map.put(ReceiptCons.APP_CODE, tm.getApprovalCode());
		}
		else if(CtUtil.isPayByQr(tm)) {
			map.put(ReceiptCons.CARD_NO, tm.getCardNo());
		}
		logger.info("ReceiptParameters:" + map);
		
		int res = PrinterUtil.printReceipt(map);
		if(res != PrinterUtil.RES_OK) {
			logger.warn("1st try to print receipt after 100 ms");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			
			res = PrinterUtil.printReceipt(map);
			if(res != PrinterUtil.RES_OK) {
				logger.warn("2nd try to print receipt after 200 ms");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
				
				res = PrinterUtil.printReceipt(map);
				
				if(res != PrinterUtil.RES_OK) {
					logger.warn("3rd try to print receipt after 400 ms");
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
					}
					
					res = PrinterUtil.printReceipt(map);
				}
			}
		}
		logger.info("Print receipt result:" + res);
		
		/* Sample
		map.put("receiptNo", "0000000001");
		map.put("tranDttm", "2017-12-16 11:00:00");
		map.put("location", "Testing Location");
		map.put("cpNo", "00001");
		map.put("startDttm", "2017-12-16 11:00:00");
		map.put("endDttm", "2017-12-16 12:00:00");
		map.put("duration", "1");
		map.put("chargeRate", "1000.00");
		map.put("amount", "1000.00");
		map.put("paymentType", "Contactless");
		map.put("cardType", "VISA");
		
		map.put("cardNo", "************8224");
		map.put("mid", "000883234288883");
		map.put("rrn", "20576282089");
		map.put("ecrRef", "0001131217192325");*/
		//map.put("expDate", "**/**");
		/*map.put("tid", "17093001");
		map.put("batchNo", "000009");
		map.put("appCode", "149587");*/
		
		
	}
}
