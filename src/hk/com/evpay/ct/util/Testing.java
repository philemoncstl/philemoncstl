package hk.com.evpay.ct.util;

import java.util.HashMap;

public class Testing {
	
	static final int TIMEOUT = 10000;
	 
	public static void main(String[] args) {
//		PrinterUtil.loadPortName();
		
		HashMap<String, String> testData = new HashMap<String, String>();
		testData.put("reprintMessage", "Y");
		testData.put("reprintDttm", "2017-12-16 12:00:00");
		testData.put("receiptNo", "0000000001");
		testData.put("tranDttm", "2017-12-16 11:00:00");
		testData.put("location", "Testing Location");
		testData.put("cpNo", "00001");
		testData.put("startDttm", "2017-12-16 11:00:00");
		testData.put("endDttm", "2017-12-16 12:00:00");
		testData.put("duration", "1");
		testData.put("chargeRate", "1000.00");
		testData.put("amount", "1000.00");
		testData.put("paymentType", "Contactless");
		testData.put("cardType", "VISA");
		
		testData.put("cardNo", "************8224");
		testData.put("mid", "000883234288883");
		testData.put("rrn", "20576282089");
		testData.put("ecrRef", "0001131217192325");
		testData.put("expDate", "**/**");
		testData.put("tid", "17093001");
		testData.put("batchNo", "000009");
		testData.put("appCode", "149587");
		
		
		System.out.println("Print Test: " + PrinterUtil.printReceipt(testData));
	}
}
