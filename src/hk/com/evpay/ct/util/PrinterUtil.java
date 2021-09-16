package hk.com.evpay.ct.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;
import com.ckzone.util.StringUtil;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

import hk.com.evpay.ct.ws.CtWebSocketClient;

public class PrinterUtil {
	private static final Logger logger = Logger.getLogger(PrinterUtil.class);

	// Status
	public static final String NOT_AVAILABLE = "NotAvailable";
	public static final String ONLINE = "Available";
	public static final String OFFLINE = "Offline";
	public static final String EMPTY = "Empty";
	public static final String NEAR_EMPTY = "NearEmpty";
	public static final String UNKNOWN = "Unknown";	//CK @ 20180314
	

	// Receipt Return Code
	public static final int RES_OK = 0;
	public static final int RES_OFFLINE = 1;
	public static final int RES_ERROR = 2;
	public static final int RES_INCOMPLETE = 3;
	
	private static final String SPACE = "                      "; 

	// Receipt Format
	private static final Byte[] ALIGNMENT_LEFT = new Byte[] { 0x1b, 0x1d, 0x61, 0x00 };
	private static final Byte[] ALIGNMENT_CENTER = new Byte[] { 0x1b, 0x1d, 0x61, 0x01 };
	private static final Byte[] ALIGNMENT_RIGHT = new Byte[] { 0x1b, 0x1d, 0x61, 0x02 };
	private static final Byte[] PAGE_CUT = new Byte[] { 0x1b, 0x64, 0x02 };
	private static final Byte[] PARTIAL_CUT = new Byte[] { 0x1b, 0x64, 0x01 };
	private static final Byte[] DRAWER_DRIVE = new Byte[] { 0x1b, 0x2a, 0x72, 0x44, 0x01, 0x00}; 
	
	public static final String PAGE_NEW_LINE = "\r\n";

	private static String PORT_NAME = "usbprn:";
	
	private static String PREV_STATUS = null;
	
	public static void main(String[] args) {
		testReceipt(false, "Octopus");
		//testReceipt(false, "Contactless");
	}
	
	public static void testReceipt(boolean reprint, String paymentType) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		
		if(reprint) {
			map.put("reprintMessage", "Y");
			map.put("reprintDttm", DateUtil.getCurrentDateTimeStr());
		}
		
		map.put("receiptNo", "22125");
		map.put("tranDttm", "2018-03-22 11:51:08");
		String loc = CtUtil.getConfig().getLocationReceipt();
		if(StringUtil.isEmpty(loc)) {
			loc = CtUtil.getServConfig().getName();
		}
		map.put("location", loc);
		map.put("cpNo", "CP02");
		map.put("startDttm", "2018-03-22 11:51:08");
		map.put("endDttm", "2018-03-22 12:51:26");
		BigDecimal duration = new BigDecimal("1.00");
		map.put("duration", String.valueOf(duration));
		map.put("timeChargeRateOnPeak", "0.40");
		map.put("amount", "0.40");
		map.put("paymentType", paymentType);
		
		if("Octopus".equals(paymentType)) {
			map.put("deviceNo", "56FFFE");
			map.put("octopusNo", "16252666");
			map.put("amountDeducted", "0.40");
			map.put("remainingValue", "108.70");
			
			
			String infoChi = "";
			String infoEng = "";
			//if(ExtraInfo.BY_CASH.equals(tm.getOctopusLastAddValueType())){
				infoEng = "Last add value by Cash on 2018-03-16";
				infoChi = "上一次於 2018-03-16  現金增值";
			//}
			/*else if(ExtraInfo.BY_AAVS.equals(tm.getOctopusLastAddValueType())) {
				infoEng = "Last add value by AAVS on " + tm.getOctopusLastAddValueDate();
				infoChi = "上一次於 " + tm.getOctopusLastAddValueDate() + " 自動增值";
			}
			else {
				infoEng = "Last add value by Online on " + tm.getOctopusLastAddValueDate();
				infoChi = "上一次於 " + tm.getOctopusLastAddValueDate() + " 網上增值";
			}*/

			map.put("lastAddValueChi", infoChi);
			map.put("lastAddValueEng", infoEng);
			
			/*map.put("octopusMsgChi", "客戶如對上述的八達通交易有任何查詢，請保存此存根及致電八達通顧客服務熱線 2266 2222");
			map.put("octopusMsgEng", "If you have any queries on the above transaction, please keep the receipt and call Octopus Customer Service"
					+ PrinterUtil.PAGE_NEW_LINE + "Hotline at 2266 2222");*/
		}
		else {			
			map.put("cardType", "VISA");		
			
			map.put("cardNo", "************8224");
			map.put("mid", "000883234288883");
			map.put("rrn", "20576282089");
			map.put("ecrRef", "0001131217192325");
			map.put("expDate", "**/**");
			map.put("tid", "17093001");
			map.put("batchNo", "000009");
			map.put("appCode", "149587");
		}
		logger.info("ReceiptParameters:" + map);
		
		PrinterUtil.printReceipt(map);
	}
	
	
	private static void addTimeRate(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String timeRate = parm.get(ReceiptCons.TIME_CHARGE_RATE_ON_PEAK);
		String timeCharge = parm.get(ReceiptCons.TIME_CHARGE);
		list.addAll(CopyArray(("時間費用(小時)").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Time Charges (Hour)\u0009" + SPACE.substring(0, 20 - timeRate.length() - 2) + "$ " + timeRate + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - timeCharge.length() - 2) + "$ " + timeCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, timeCharge);
	}
	private static void addTimeRateOnPeak(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String duration = parm.get(ReceiptCons.DURATION_ON_PEAK);
		String timeRate = parm.get(ReceiptCons.TIME_CHARGE_RATE_ON_PEAK);
		String timeCharge = parm.get(ReceiptCons.TIME_CHARGE_ON_PEAK);
	
		list.addAll(CopyArray(("-充電時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charging Duration\u0009" + SPACE.substring(0, 10 - duration.length()) + duration).getBytes()));
		list.addAll(CopyArray(("小時/Hour" + PAGE_NEW_LINE).getBytes("Big5")));
		
		list.addAll(CopyArray(("-收費/小時").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charge/Hour\u0009" + SPACE.substring(0, 20 - timeRate.length() - 2) + "$ " + timeRate + PAGE_NEW_LINE).getBytes()));	
		
		list.addAll(CopyArray(("-總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - timeCharge.length() - 2) + "$ " + timeCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, timeCharge);
	}
	private static void addTimeRateOffPeak(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String duration = parm.get(ReceiptCons.DURATION_OFF_PEAK);
		String timeRate = parm.get(ReceiptCons.TIME_CHARGE_RATE_OFF_PEAK);
		String timeCharge = parm.get(ReceiptCons.TIME_CHARGE_OFF_PEAK);

		list.addAll(CopyArray(("-充電時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charging Duration\u0009" + SPACE.substring(0, 10 - duration.length()) + duration).getBytes()));
		list.addAll(CopyArray(("小時/hour" + PAGE_NEW_LINE).getBytes("Big5")));
		
		list.addAll(CopyArray(("-收費/小時").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charge/Hour\u0009" + SPACE.substring(0, 20 - timeRate.length() - 2) + "$ " + timeRate + PAGE_NEW_LINE).getBytes()));	
		
		list.addAll(CopyArray(("-總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - timeCharge.length() - 2) + "$ " + timeCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, timeCharge);
	}
	
	private static void addEnergyRate(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String energyRate = parm.get(ReceiptCons.ENERGY_CHARGE_RATE_ON_PEAK);
		String energyConsumed = parm.get(ReceiptCons.ENERGY_CONSUMED);
		String energyCharge = parm.get(ReceiptCons.ENERGY_CHARGE);
		
		list.addAll(CopyArray(("用電度數").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Energy Consumed\u0009" + SPACE.substring(0, 20 - energyConsumed.length()) + energyConsumed + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("收費/度").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charge/kWH\u0009" + SPACE.substring(0, 20 - energyRate.length() - 2) + "$ " + energyRate + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - energyCharge.length() - 2) + "$ " + energyCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, energyCharge);
	}	
	private static void addEnergyRateOnPeak(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String energyConsumed = parm.get(ReceiptCons.ENERGY_CONSUMED_ON_PEAK);
		String energyRate = parm.get(ReceiptCons.ENERGY_CHARGE_RATE_ON_PEAK);
		String energyCharge = parm.get(ReceiptCons.ENERGY_CHARGE_ON_PEAK);
	
		list.addAll(CopyArray(("-用電度數").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Energy Consumed\u0009" + SPACE.substring(0, 20 - energyConsumed.length()) + energyConsumed + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("-收費/度").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charge/kWH\u0009" + SPACE.substring(0, 20 - energyRate.length() - 2) + "$ " + energyRate + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("-總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - energyCharge.length() - 2) + "$ " + energyCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, energyCharge);
	}	
	private static void addEnergyRateOffPeak(List<Byte> list, Map<String, String> parm)  throws UnsupportedEncodingException {
		String energyConsumed = parm.get(ReceiptCons.ENERGY_CONSUMED_OFF_PEAK);
		String energyRate = parm.get(ReceiptCons.ENERGY_CHARGE_RATE_OFF_PEAK);
		String energyCharge = parm.get(ReceiptCons.ENERGY_CHARGE_OFF_PEAK);
		list.addAll(CopyArray(("-用電度數").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Energy Consumed\u0009" + SPACE.substring(0, 20 - energyConsumed.length()) + energyConsumed + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("-收費/度").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charge/kWH\u0009" + SPACE.substring(0, 20 - energyRate.length() - 2) + "$ " + energyRate + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("-總計(港幣)").getBytes("Big5")));
		//list.addAll(CopyArray(("\u0009Total (HKD)\u0009" + SPACE.substring(0, 20 - energyCharge.length() - 2) + "$ " + energyCharge + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("\u0009Total (HKD)\u0009").getBytes()));
		addAmount(list, 20, energyCharge);
	}
	
	private static void addOnPeak(List<Byte> list)  throws UnsupportedEncodingException {
		list.addAll(CopyArray(("高峰").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009On-peak\u0009" + PAGE_NEW_LINE).getBytes()));
	}
	private static void addOffPeak(List<Byte> list)  throws UnsupportedEncodingException {
		list.addAll(CopyArray(("非高峰").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Off-peak\u0009" + PAGE_NEW_LINE).getBytes()));
	}
	
	private static void addChargingInfo(List<Byte> list, Map<String, String> parm) throws UnsupportedEncodingException {
		list.addAll(Arrays.asList(ALIGNMENT_LEFT));
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x44, 0x12, 0x25, 0x01, 0x00})); //Set horizontal tab
				
		list.addAll(CopyArray(("收據編號").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Receipt No.\u0009" + SPACE.substring(0, 20 - parm.get("receiptNo").length()) + parm.get("receiptNo") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("日期/時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Date/Time\u0009" + SPACE.substring(0, 20 - parm.get("tranDttm").length()) + parm.get("tranDttm") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("地點").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Location\u0009" + SPACE.substring(0, 20 - parm.get("location").length()) + parm.get("location") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("充電站編號").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charger No.\u0009" + SPACE.substring(0, 20 - parm.get("cpNo").length()) + parm.get("cpNo") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("開始時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Start Time\u0009" + SPACE.substring(0, 20 - parm.get("startDttm").length()) + parm.get("startDttm") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("結束時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009End Time\u0009" + SPACE.substring(0, 20 - parm.get("endDttm").length()) + parm.get("endDttm") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("充電時間").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Charging Duration\u0009" + SPACE.substring(0, 10 - parm.get("duration").length()) + parm.get("duration") ).getBytes()));
		list.addAll(CopyArray(("小時/Hour" + PAGE_NEW_LINE).getBytes("Big5")));
		
		if(CtUtil.getServConfig().getFreeTimeUnit() != null && CtUtil.getServConfig().getFreeTimeUnit() > 0) {
			list.addAll(CopyArray(("免費充電時間").getBytes("Big5")));
			list.addAll(CopyArray(("\u0009Free Charging Duration\u0009" + SPACE.substring(0, 7 - parm.get("freeDuration").length()) + parm.get("freeDuration") ).getBytes()));
			list.addAll(CopyArray(("小時/Hour" + PAGE_NEW_LINE).getBytes("Big5")));
		}
		
		boolean timeEnabled = "Y".equalsIgnoreCase(parm.get(ReceiptCons.TIME_CHARGE_ENABLED));
		boolean energyEnabled = "Y".equalsIgnoreCase(parm.get(ReceiptCons.ENERGY_CHARGE_ENABLED));
		
		//time and energy
		if(timeEnabled && energyEnabled) {
			if("Y".equalsIgnoreCase(parm.get(ReceiptCons.ON_OFF_PEAK_SAME_RATE))) {
				addTimeRate(list, parm);
				addEnergyRate(list, parm);
			}
			else {
				//on peak
				addOnPeak(list);	
				addTimeRateOnPeak(list, parm);
				addEnergyRateOnPeak(list, parm);
				
				//off peak				
				addOffPeak(list);
				addTimeRateOffPeak(list, parm);
				addEnergyRateOffPeak(list, parm);
			}
		}
		//time only
		else if(timeEnabled) {
			//on off peak same time rate
			if("Y".equals(parm.get(ReceiptCons.ON_OFF_PEAK_SAME_TIME_RATE))) {			
				addTimeRate(list, parm);
			}
			else {
				//on peak
				addOnPeak(list);
				addTimeRateOnPeak(list, parm);
				
				//off peak
				addOffPeak(list);
				addTimeRateOffPeak(list, parm);				
			}
		}
		//energy only
		else if(energyEnabled) {
			//on off peak same energy rate
			if("Y".equals(parm.get(ReceiptCons.ON_OFF_PEAK_SAME_ENERGY_RATE))) {			
				addEnergyRate(list, parm);
			}
			else {
				//on peak
				addOnPeak(list);			
				addEnergyRateOnPeak(list, parm);
				
				//off peak				
				addOffPeak(list);				
				addEnergyRateOffPeak(list, parm);
			}
		}		
		
		list.addAll(CopyArray(("金額(港幣)").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Amount (HKD)\u0009").getBytes()));
		
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x69, 0x01, 0x01}));
		/*list.addAll(Arrays.asList(new Byte[]{0x1b, 0x45}));
		list.addAll(CopyArray((SPACE.substring(0, 10 - parm.get("amount").length() - 2) + "$ " + parm.get("amount") + PAGE_NEW_LINE).getBytes()));
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x46}));*/
		addAmount(list, 10, parm.get("amount"));
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x69, 0x00, 0x00}));  //Cancel Character Expansion
	}
	
	private static void addAmount(List<Byte> list, int charLength, String amount) {
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x45}));
		list.addAll(CopyArray((SPACE.substring(0, charLength - amount.length() - 2) + "$ " + amount + PAGE_NEW_LINE).getBytes()));
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x46}));
	}
	
	private static void addOctopusInfo(List<Byte> list, Map<String, String> parm) throws UnsupportedEncodingException {
		list.addAll(CopyArray(("機號").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Device no.\u0009" + SPACE.substring(0, 20 - parm.get("deviceNo").length()) + parm.get("deviceNo") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("八達通號碼").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Octopus no.\u0009" + SPACE.substring(0, 20 - parm.get("octopusNo").length()) + parm.get("octopusNo") + PAGE_NEW_LINE).getBytes()));
		
		list.addAll(CopyArray(("扣除金額").getBytes("Big5")));
		list.addAll(CopyArray(("\u0009Amount Deducted\u0009" + SPACE.substring(0, 20 - parm.get("amountDeducted").length()) + parm.get("amountDeducted") + PAGE_NEW_LINE).getBytes()));
		
		//only for normal Octopus
		if(!"Y".equals(parm.get(ReceiptCons.SMART_OCTOPUS))) {
			list.addAll(CopyArray(("餘額").getBytes("Big5")));
			list.addAll(CopyArray(("\u0009Remaining Value\u0009" + SPACE.substring(0, 20 - parm.get("remainingValue").length()) + parm.get("remainingValue") + PAGE_NEW_LINE).getBytes()));	
		}
	}
	
	private static void addContactlessInfo(List<Byte> list, Map<String, String> parm) throws UnsupportedEncodingException {
//		list.addAll(CopyArray((parm.get(ReceiptCons.PAYMENT_TYPE) + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray((parm.get(ReceiptCons.CARD_TYPE) + PAGE_NEW_LINE + PAGE_NEW_LINE).getBytes()));
		list.addAll(Arrays.asList(ALIGNMENT_LEFT));
		list.addAll(Arrays.asList(new Byte[]{0x1b, 0x44, 0x0A, 0x20, 0x2A, 0x00})); //Set horizontal tab
		list.addAll(CopyArray(("CARD NO.:\u0009" + parm.get(ReceiptCons.CARD_NO) + 
				"\u0009EXP DATE:\u0009" + SPACE.substring(0, 15 - parm.get(ReceiptCons.EXP_DATE).length()) + parm.get(ReceiptCons.EXP_DATE) + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("MID:\u0009" + parm.get(ReceiptCons.MID) + "\u0009TID:\u0009" + 
					SPACE.substring(0, 15 - parm.get(ReceiptCons.TID).length()) +  parm.get(ReceiptCons.TID) + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("RRN:\u0009" + parm.get(ReceiptCons.RRN) + "\u0009BATCH NO.:\u0009" + 
					SPACE.substring(0, 15 - parm.get(ReceiptCons.BATCH_NO).length()) + parm.get(ReceiptCons.BATCH_NO) + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("ECR REF:\u0009" + parm.get(ReceiptCons.ECR_REF) + "\u0009APP CODE:\u0009" + 
					SPACE.substring(0, 15 - parm.get(ReceiptCons.APP_CODE).length()) + parm.get(ReceiptCons.APP_CODE) + PAGE_NEW_LINE).getBytes()));
		list.addAll(CopyArray(("TRACE NO.:" + parm.get(ReceiptCons.TRACE) + PAGE_NEW_LINE).getBytes()));
	}
	
	public static int printReceipt(Map<String, String> parm) {
		logger.info("parm:" + parm);
		int res = 0;

		if (isOnline()) {
			StarIOPort port = null;
			try {
				port = StarIOPort.getPort(PORT_NAME, "", 10000); // Open Printer
																	// Port
				//CK @ 2018-05-15
				boolean printMerchantCopy = "Y".equals(CtUtil.getServConfig().getReceiptPrintMerchantCopy());
				logger.debug("Open Port Success, printMerchantCopy:" + printMerchantCopy);

				byte[] data;
				List<Byte> list = new ArrayList<Byte>();
				
				list.addAll(Arrays.asList(new Byte[] { 0x1b, 0x1e, 0x46, 0x01}));
				
				// list.addAll(Arrays.asList(new Byte[] { 0x1b, 0x1c, 0x70, 0x01, 0x00, '\r', '\n' })); // 打印圖像
				
				String merchantName = "Cornerstone EV Charging Service Limited" ;
				if(parm.containsKey("merchantName")) {
					merchantName = parm.get("merchantName");
				}
				list.addAll(Arrays.asList(ALIGNMENT_CENTER));
				list.addAll(Arrays.asList(new Byte[]{0x1b, 0x69, 0x01, 0x00}));
				list.addAll(Arrays.asList(new Byte[]{0x1b, 0x45}));
				list.addAll(CopyArray((merchantName + PAGE_NEW_LINE).getBytes("Big5")));
				list.addAll(Arrays.asList(new Byte[]{0x1b, 0x46}));
				list.addAll(Arrays.asList(new Byte[]{0x1b, 0x69, 0x00, 0x00}));  //Cancel Character Expansion

				if(parm.containsKey("reprintMessage") && "Y".equals(parm.get("reprintMessage"))&& parm.containsKey("reprintDttm")) {
					list.addAll(Arrays.asList(ALIGNMENT_RIGHT));
					list.addAll(CopyArray(("(重印 Reprint)" + PAGE_NEW_LINE).getBytes("Big5")));
					list.addAll(CopyArray((parm.get("reprintDttm") + PAGE_NEW_LINE).getBytes("Big5")));
				}
				if(parm.containsKey("voidMessage") && "Y".equals(parm.get("voidMessage"))&& parm.containsKey("voidDttm")) {
					list.addAll(Arrays.asList(ALIGNMENT_RIGHT));
					list.addAll(CopyArray(("(撤銷交易 Void)" + PAGE_NEW_LINE).getBytes("Big5")));
					list.addAll(CopyArray((parm.get("voidDttm") + PAGE_NEW_LINE).getBytes("Big5")));
				}
				list.addAll(CopyArray(("---------------------------------------------------------" + PAGE_NEW_LINE).getBytes("Big5")));
				
				if(printMerchantCopy) {
					list.addAll(CopyArray(("CUSTOMER COPY" + PAGE_NEW_LINE).getBytes("Big5")));		//@20180324
				}
				
				addChargingInfo(list, parm);
				
				
				
				list.addAll(Arrays.asList(ALIGNMENT_RIGHT));
				list.addAll(CopyArray(("===================" + PAGE_NEW_LINE).getBytes("Big5")));				
				
				
				list.addAll(CopyArray(("*********************************************************" + PAGE_NEW_LINE).getBytes("Big5")));
				
				list.addAll(Arrays.asList(ALIGNMENT_CENTER));
				
				String payType = parm.get("paymentType");
				if("Contactless".equals(payType) || "ContactlessGroup".equals(payType)) {
					addContactlessInfo(list, parm);
					
					list.addAll(Arrays.asList(ALIGNMENT_LEFT));
					list.addAll(CopyArray((PAGE_NEW_LINE + "NO SIGNATURE REQUIRED" + PAGE_NEW_LINE + PAGE_NEW_LINE).getBytes()));
				}
				else if("Octopus".equals(payType)){
					addOctopusInfo(list, parm);
					
					list.addAll(Arrays.asList(ALIGNMENT_LEFT));
					list.addAll(CopyArray((PAGE_NEW_LINE + parm.get("lastAddValueChi") + PAGE_NEW_LINE).getBytes("Big5")));
					list.addAll(CopyArray((parm.get("lastAddValueEng") + PAGE_NEW_LINE).getBytes()));
					
					//CK removed @ 2018-08-17
					/*list.addAll(CopyArray((PAGE_NEW_LINE + parm.get("octopusMsgChi") + PAGE_NEW_LINE).getBytes("Big5")));
					list.addAll(CopyArray((parm.get("octopusMsgEng") + PAGE_NEW_LINE).getBytes()));*/
				}
				else if("QR".equals(payType)){
					//TODO
				}
				
				list.addAll(CopyArray(("---------------------------------------------------------" + PAGE_NEW_LINE).getBytes("Big5")));
				
				list.addAll(Arrays.asList(ALIGNMENT_CENTER));
				list.addAll(CopyArray(("多謝! Thank you!" + PAGE_NEW_LINE + PAGE_NEW_LINE).getBytes("Big5")));
				
				if(printMerchantCopy) {
					//CK @ 2018-03-24, add merchant copy started
					list.addAll(CopyArray((PAGE_NEW_LINE + "---------------------------------------------------------" + PAGE_NEW_LINE + PAGE_NEW_LINE + PAGE_NEW_LINE).getBytes("Big5")));
					//list.addAll(Arrays.asList(PARTIAL_CUT));
					//list.addAll(Arrays.asList(DRAWER_DRIVE));
					list.addAll(Arrays.asList(ALIGNMENT_CENTER));
					list.addAll(CopyArray(("Merchant Copy" + PAGE_NEW_LINE).getBytes("Big5")));
					addChargingInfo(list, parm);
					
					//new line
					list.addAll(CopyArray(PAGE_NEW_LINE.getBytes("Big5")));
					
					if("Contactless".equals(parm.get("paymentType"))) {
						addContactlessInfo(list, parm);
					}
					else {
						addOctopusInfo(list, parm);
					}
					
					//redemption
					list.addAll(Arrays.asList(ALIGNMENT_LEFT));
					list.addAll(CopyArray((PAGE_NEW_LINE + PAGE_NEW_LINE + PAGE_NEW_LINE + "Redemption :____________________________________________"+ PAGE_NEW_LINE + PAGE_NEW_LINE).getBytes("Big5")));
					//CK @ 2018-03-24, add merchant copy ended
				}
				
				list.addAll(Arrays.asList(PAGE_CUT)); // Cut
			
				data = new byte[list.size()];

				for (int index = 0; index < data.length; index++) {
					data[index] = (Byte) list.get(index);
				}
				
				int totalSizeCommunicated = WritePortHelper(port, data);
				if (totalSizeCommunicated != data.length) {
					logger.warn("Could not write all data to the printer.");
					res = RES_INCOMPLETE;
				}

			} catch (Throwable e) {
				e.printStackTrace();
				logger.error("Failed to print receipt... ", e);
				res = RES_ERROR;
			} finally {
				if (port != null) {
					StarIOPort.releasePort(port); // Release Printer Port
				}
			}
		} else {
			res = RES_OFFLINE;
		}
		return res;
	}

	private static int WritePortHelper(StarIOPort port, byte[] writeBuffer) throws StarIOPortException {
		int zeroProgressOccurances = 0;
		int totalSizeCommunicated = 0;

		while ((totalSizeCommunicated < writeBuffer.length) && (zeroProgressOccurances < 2)) // adjust zeroProgressOccurances as needed
		{
			int sizeCommunicated = port.writePort(writeBuffer, totalSizeCommunicated,
					writeBuffer.length - totalSizeCommunicated);
			if (sizeCommunicated == 0) {
				zeroProgressOccurances++;
			} else {
				totalSizeCommunicated += sizeCommunicated;
				zeroProgressOccurances = 0;
			}
		}

		return totalSizeCommunicated;
	}

	private static List<Byte> CopyArray(byte[] array1) {
		Byte[] array2 = new Byte[array1.length];
		
		int index = 0;
		while (index < array2.length) {
			array2[index] = array1[index];
			index++;
		}
		
		return Arrays.asList(array2);
	}

	public static boolean isOnline() {
		String status = getPrinterStatusHelper();
		
		if(NOT_AVAILABLE.equals(status)) {
			//CK @ 2018-03-24, get again in 100ms if exception found
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			status = getPrinterStatusHelper();
		}
		
		//update the status datetime
		CtUtil.getCt().setPrinterStatusDttm(new Date());
		
		boolean res = (ONLINE.equals(status) || NEAR_EMPTY.equals(status));
		
		if(PREV_STATUS == null) {
			PREV_STATUS = CtUtil.getCt().getPrinterStatus();
		}
		
		if(!status.equals(PREV_STATUS)) {
			logger.info("Printer status changed, old:" + PREV_STATUS + ", new:" + status);
			
			CtUtil.getCt().setPrinterStatus(status);
			CtUtil.saveCurrentCt();
			CtWebSocketClient.updateCt();
		}
		PREV_STATUS = status;
		
		logger.info("Printer online: " + res);
		return res;
	}

	public static String getPrinterStatusHelper() {
		StarIOPort port = null;
		try {
			port = StarIOPort.getPort(PORT_NAME, "", 5000); // Open Printer Port
			logger.info("Open Port Success");

			StarPrinterStatus printerStatus = port.retreiveStatus(); // Get
																		// Printer
																		// Status
			if (printerStatus.offline == true) {
				if(printerStatus.receiptPaperEmpty == true) {
					logger.info("Paper Empty");
					return EMPTY;
				}
				else {
					logger.info("Offline");
					return OFFLINE;
				}
				
			} else {
				logger.info("Online");

				if (printerStatus.receiptPaperNearEmptyInner == true) {
					logger.info("Paper Near Empty");
					return NEAR_EMPTY;
				}

				return ONLINE;
			}
		} catch (StarIOPortException e) {
			// Fail to open port, throw the exception
			logger.info("Failed to get status:" + e.getMessage(), e);
			return NOT_AVAILABLE;
		} finally {
			if (port != null) {
				StarIOPort.releasePort(port); // Release Printer Port
			}
		}
	}

}
