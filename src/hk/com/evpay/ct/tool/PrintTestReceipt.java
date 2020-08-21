package hk.com.evpay.ct.tool;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.ckzone.octopus.ExtraInfo;
import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.EvCons;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.CtClient;
import hk.com.evpay.ct.Step3PrintReceipt;
import hk.com.evpay.ct.util.RateUtil;

public class PrintTestReceipt extends AbstractAction{
	private static final Logger logger = Logger.getLogger(PrintTestReceipt.class);
	
	public static void main(String[] args) {
		PrintTestReceipt r = new PrintTestReceipt();
		r.printReceipt(0, false);
		
		System.exit(0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("called.");
		String input = JOptionPane.showInputDialog(CtClient.CUR_INST, "Receipt to print[1-10]:?");
		logger.info("input:" + input);
		
		try {
			printReceipt(Integer.parseInt(input), true);
		} catch (Exception e2) {
			logger.error("Invalid input:" + input, e2);
		}
	}
	
	public void printReceipt(int idx, boolean printReceipt) {
		this.printReceipt(idx, printReceipt, false);
	}
	
	/**
	 * On peak: 0900-2100
	 * 
	 * Pre-paid
	 * 1 - Time charge with on/off peak same rate
	 * 2 - Time charge with on/off peak different rate
	 * 
	 * Post-paid
	 * 3 - Time charge with on/off peak same rate
	 * 4 - Time charge with on/off peak different rate
	 * 5 - Energy charge with on/off peak same rate
	 * 6 - Energy charge with on/off peak different rate
	 * 7 - Time and energy charge with on/off peak same rate
	 * 8 - Time and energy charge with on/off peak different rate
	 * 
	 * Others
	 * 9 - Reprint
	 * 10 - SmartOctopus
	 * @param idx
	 * @param printReceipt
	 * @param reprint
	 */
	public void printReceipt(int idx, boolean printReceipt, boolean reprint) {
		logger.info("idx:" + idx + ", printReceipt:" + printReceipt + ", reprint:" + reprint);
		
		TranModel tran = new TranModel();
		tran.setCpNo("CP01");
		tran.setPayMethodCode("Octopus");
		tran.setOctopusDeviceNo("56FFFE");
		tran.setOctopusNo("21629438");
		tran.setSmartOctopus("N");
		tran.setOctopusLastAddValueType(ExtraInfo.BY_CASH);
		tran.setOctopusLastAddValueDate("2018-04-28");
		
		tran.setMinChargingUnit(1);
		tran.setChargingUnitMinutes(15);
		tran.setMeterStart(new BigDecimal(1000));
		tran.setMeterStop(new BigDecimal(29800));		
		tran.setStartDttm(DateUtil.parseDateTime("2018-05-03 08:14:01"));
		tran.setEndDttm(DateUtil.parseDateTime("2018-05-03 10:14:01"));
		tran.setTranDttm(tran.getStartDttm());
		
		
		
		int receiptNo = 20501;
		//Time charge with on/off peak same rate (prepaid)
		if(idx == 0 || idx == 1) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_PREPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(4));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Time charge with on/off peak different rate (prepaid)
		if(idx == 0 || idx == 2) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_PREPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(5));
			tran.setReceiptNo(String.valueOf(receiptNo));
			tran.setOctopusLastAddValueType(ExtraInfo.BY_ONLINE);
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Time charge with on/off peak different rate (postpaid)
		if(idx == 0 || idx == 3) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(4));
			tran.setReceiptNo(String.valueOf(receiptNo));
			tran.setOctopusLastAddValueType(ExtraInfo.BY_AAVS);			
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Time charge with on/off peak different rate (postpaid)
		if(idx == 0 || idx == 4) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(5));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//tran.setEndDttm(DateUtil.parseDateTime("2018-05-03 11:14:01"));
		//Energy charge with on/off peak same rate
		if(idx == 0 || idx == 5) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("N");
			tran.setEnergyEnabled("Y");	
			tran.setEnergyRateOffPeak(new BigDecimal("1.2"));
			tran.setEnergyRateOnPeak(new BigDecimal("1.2"));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Energy charge with on/off peak different rate
		if(idx == 0 || idx == 6) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("N");
			tran.setEnergyEnabled("Y");	
			tran.setEnergyRateOffPeak(new BigDecimal("1.2"));
			tran.setEnergyRateOnPeak(new BigDecimal("1.5"));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Time and energy charge with on/off peak same rate
		if(idx == 0 || idx == 7) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("Y");	
			tran.setEnergyRateOffPeak(new BigDecimal("1.2"));
			tran.setEnergyRateOnPeak(new BigDecimal("1.2"));
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(4));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Time and energy charge with on/off peak different rate
		if(idx == 0 || idx == 8) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_POSTPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("Y");	
			tran.setEnergyRateOffPeak(new BigDecimal("1.2"));
			tran.setEnergyRateOnPeak(new BigDecimal("1.5"));
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(5));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
		
		//Reprint
		if(idx == 0 || idx == 9) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_PREPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(4));
			tran.setReceiptNo(String.valueOf(receiptNo));
			
			calc(tran, printReceipt, true);
		}
		receiptNo ++;
		
		//Smart Octopus
		if(idx == 0 || idx == 10) {
			logger.info("****** idx:" + idx + ", receiptNo:" + receiptNo);
			tran.setMode(EvCons.MODE_PREPAID);
			tran.setTimeEnabled("Y");
			tran.setEnergyEnabled("N");	
			tran.setTimeRateOffPeakPerUnit(new BigDecimal(4));
			tran.setTimeRateOnPeakPerUnit(new BigDecimal(4));
			tran.setReceiptNo(String.valueOf(receiptNo));
			tran.setSmartOctopus("Y");
			
			calc(tran, printReceipt, reprint);
		}
		receiptNo ++;
	}
	
	private void calc(TranModel tran, boolean printReceipt, boolean reprint) {
		RateUtil.calcChargingFee(tran);
		
		if(printReceipt) {
			tran.setRemainBal(new BigDecimal(200.5).subtract(tran.getAmt()));
			Step3PrintReceipt.printReceipt(tran, reprint);
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}		
	}

}
