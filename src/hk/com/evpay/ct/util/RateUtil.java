package hk.com.evpay.ct.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.EvCons;
import hk.com.cstl.evcs.model.RateDetailModel;
import hk.com.cstl.evcs.model.RateModel;
import hk.com.cstl.evcs.model.ServConfig;
import hk.com.cstl.evcs.model.TranModel;

public class RateUtil {
	private static final Logger logger = Logger.getLogger(RateUtil.class);
	
	public static final long CHARGING_BUFFER_MIN = 5;
	
	public static long EMERGY_NEGATIVE_VALUE_ALLOWANCE = 10000; //10kWh
		
	public static void main(String[] args) throws ParseException {
		/*System.out.println(getHourMinString(30));
		System.out.println(getHourMinString(900));
		System.out.println(getHourMinString(901));
		System.out.println(getHourMinString(910));
		System.out.println(getHourMinString(130));
		System.out.println(getHourMinString(2100));
		System.out.println(getHourMinString(2359));
		if(1==1) {
			return;
		}*/
		
		BigDecimal meterStop = new BigDecimal(17194);
		BigDecimal meterStart = new BigDecimal(17199);
		BigDecimal tot = meterStop.subtract(meterStart);
		//CK @ 20191205, handle negative value cases
		if(tot.intValue() < 0) {
			if(tot.intValue() * -1 > EMERGY_NEGATIVE_VALUE_ALLOWANCE) {
				tot = tot.add(new BigDecimal(10000000000L));
				logger.info("Meter reset, tot updated to:" + tot);
			}
			else {
				logger.warn("Energy NEGATIVATE value:" + tot);
				tot = BigDecimal.ZERO;
			}
		}
		tot = tot.divide(new BigDecimal(1000, MathContext.DECIMAL32));
		logger.info("tot:" + tot);
		
		//testPostpaid();
		//testPrepaid();
		

		System.exit(0);
	}
	
	
	private static void testPostpaid() {
		System.out.println("****************POSTPAID****************");
		RateDetailModel rate = getRateDetail();
		
		if(rate != null) {
			System.out.println("getRateDtlId:\t\t" + rate.getRateDtlId());
			System.out.println("getEffDate:\t\t" + rate.getEffDate());
			System.out.println("getOnPeakStart:\t\t" + rate.getOnPeakStartTime());
			System.out.println("getOffPeakEnergyRate:\t\t" + rate.getOffPeakEnergyRate());
			System.out.println("getOnPeakTimeRate:\t" + rate.getOnPeakTimeRate());
			System.out.println("getOffPeakTimeRate:\t" + rate.getOffPeakTimeRate());
			System.out.println("getOnPeakEnergyRate:\t" + rate.getOnPeakEnergyRate());
			System.out.println("getOffPeakEnergyRate:\t" + rate.getOffPeakEnergyRate());
			System.out.println("getTimeChargeEnableFlag:\t" + rate.getTimeChargeEnableFlag());
			System.out.println("getEnergyChargeEnableFlag:\t" + rate.getEnergyChargeEnableFlag());
			
			TranModel tran = new TranModel();
			tran.setMode(EvCons.MODE_POSTPAID);
			
			logger.info("***Case 0 (min pay)***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 03:00:01"));			
			tran.setMeterStart(new BigDecimal(100));
			tran.setMeterStop(new BigDecimal(1400));
			calcChargingFee(tran);			
			
			
			logger.info("***Case 1***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 05:00:00"));			
			tran.setMeterStart(new BigDecimal(100));
			tran.setMeterStop(new BigDecimal(1401));
			calcChargingFee(tran);
			
			logger.info("***Case 2***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 05:05:00"));			
			tran.setMeterStart(new BigDecimal(999999));
			tran.setMeterStop(new BigDecimal(1400));
			calcChargingFee(tran);
			
			logger.info("***Case 3***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 05:06:00"));			
			tran.setMeterStart(new BigDecimal(100));
			tran.setMeterStop(new BigDecimal(1100));
			calcChargingFee(tran);
			
			logger.info("***Case 4***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 05:29:00"));			
			tran.setMeterStart(new BigDecimal(100));
			tran.setMeterStop(new BigDecimal(1400));
			calcChargingFee(tran);
			
			logger.info("***Case 5***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 10:14:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 13:14:00"));			
			tran.setMeterStart(new BigDecimal(2800));
			tran.setMeterStop(new BigDecimal(16800));
			calcChargingFee(tran);
			
			
			logger.info("***Case 6***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 10:14:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 13:18:00"));			
			tran.setMeterStart(new BigDecimal(2000));
			tran.setMeterStop(new BigDecimal(22000));
			calcChargingFee(tran);
			
			logger.info("***Case 7***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 10:14:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 13:19:00"));			
			tran.setMeterStart(new BigDecimal(2000));
			tran.setMeterStop(new BigDecimal(102000));
			calcChargingFee(tran);
			
			
			logger.info("***Case 8***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 10:14:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 13:20:00"));			
			tran.setMeterStart(new BigDecimal(0));
			tran.setMeterStop(new BigDecimal(16800));
			calcChargingFee(tran);
			
			
			logger.info("***Case 8***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 10:14:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-04 13:20:00"));			
			tran.setMeterStart(new BigDecimal(2000));
			tran.setMeterStop(new BigDecimal(20200));
			calcChargingFee(tran);
			
			
			logger.info("***Case 9***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 20:16:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 22:21:00"));			
			tran.setMeterStart(new BigDecimal(2000));
			tran.setMeterStop(new BigDecimal(22000));
			calcChargingFee(tran);
			
			logger.info("***Case 10***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 20:16:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 22:22:00"));
			tran.setMeterStart(new BigDecimal(9999999990L));
			tran.setMeterStop(new BigDecimal(22000));
			calcChargingFee(tran);
			
			

			logger.info("***Case 11***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 20:16:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 20:17:00"));
			calcChargingFee(tran);
		}
		else {
			System.out.println("Cur rate is null");
		}
		
	}
	
	
	private static void testPrepaid() {
		System.out.println("****************PREPAID****************");
		
		RateDetailModel rate = getRateDetail();

		if(rate != null) {
			System.out.println("getRateDtlId:\t\t" + rate.getRateDtlId());
			System.out.println("getEffDate:\t\t" + rate.getEffDate());
			System.out.println("getOnPeakStart:\t\t" + rate.getOnPeakStartTime());
			System.out.println("getOffPeakEnergyRate:\t\t" + rate.getOffPeakEnergyRate());
			System.out.println("getOnPeakTimeRate:\t" + rate.getOnPeakTimeRate());
			System.out.println("getOffPeakTimeRate:\t" + rate.getOffPeakTimeRate());
			System.out.println("getOnPeakEnergyRate:\t" + rate.getOnPeakEnergyRate());
			System.out.println("getOffPeakEnergyRate:\t" + rate.getOffPeakEnergyRate());
			System.out.println("getTimeChargeEnableFlag:\t" + rate.getTimeChargeEnableFlag());
			System.out.println("getEnergyChargeEnableFlag:\t" + rate.getEnergyChargeEnableFlag());
			
			TranModel tran = new TranModel();
			tran.setMode(EvCons.MODE_PREPAID);
			logger.info("***Case 1***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 03:00:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 05:15:00"));
			calcChargingFee(tran);
			
			logger.info("***Case 2***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 07:45:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 12:15:00"));
			calcChargingFee(tran);
			
			logger.info("***Case 3***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 12:45:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 15:00:00"));
			calcChargingFee(tran);
			
			logger.info("***Case 4***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 19:20:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-03 22:20:00"));
			calcChargingFee(tran);
			
			logger.info("***Case 5***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 22:13:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-04 10:25:00"));
			calcChargingFee(tran);
			
			
			logger.info("***Case 6***");
			tran.setStartDttm(DateUtil.parseDateTime("2017-12-03 20:20:00"));
			tran.setEndDttm(DateUtil.parseDateTime("2017-12-05 10:26:00"));
			calcChargingFee(tran);
		}
		else {
			System.out.println("Cur rate is null");
		}
	}
	
	
	
	
	public static boolean isOnPeak(Date refTime, int onPeakStart, int onPeakEnd) {
		int refTimeHourMin = CtUtil.getHourMinInt(refTime);
		
		boolean res = refTimeHourMin >= onPeakStart && refTimeHourMin < onPeakEnd;
		return res;
	}
	
	public static boolean isOnPeak(Date refTime, String onPeakStart, String onPeakEnd) {
		return isOnPeak(refTime, DateUtil.getTimeInt(onPeakStart), DateUtil.getTimeInt(onPeakEnd));
	}
	
	public static boolean isOnPeak(Date refTime, RateDetailModel rate) {
		return isOnPeak(refTime, rate.getOnPeakStartTime(), rate.getOnPeakEndTime());
	}
		
	public static void ingoreSecond(Calendar cal) {
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
	}
	
	/**
	 * For time charge
	 * @param tran
	 * @return
	 */
	public static List<ChargingPeriod> getPeriodList(TranModel tran){
		RateModel rate = CtUtil.getServConfig().getRate();
		RateDetailModel rateDtl = rate.getCurrentRate(tran.getStartDttm().getTime());
		int onPeakStart = DateUtil.getTimeInt(rateDtl.getOnPeakStartTime());
		int onPeakEnd = DateUtil.getTimeInt(rateDtl.getOnPeakEndTime());
		
		List<ChargingPeriod> list = new ArrayList<ChargingPeriod>();		
		
		//start
		Calendar c1 = Calendar.getInstance();
		c1.setTime(tran.getStartDttm());
		ingoreSecond(c1);
		
		//end
		int actualMins = DateUtil.getMinBetween(tran.getStartDttm(), tran.getEndDttm());
		Integer minChargingUnit = CtUtil.getServConfig().getMinChargingUnit();
		int minChargingMins = minChargingUnit == null ? 0 : minChargingUnit * rate.getMins();
		
		Calendar c2 = Calendar.getInstance();
		if(actualMins >= minChargingMins) {
			c2.setTime(tran.getEndDttm());
		}
		else {
			logger.info("set to minChargingUnit:" + minChargingUnit + ", mins:" + minChargingMins);
			c2.setTimeInMillis(tran.getStartDttm().getTime() + minChargingMins * 60000);
		}
		ingoreSecond(c2);
		
		ChargingPeriod cp = null;
		do {
			cp = new ChargingPeriod();
			cp.setFrom(c1.getTime());
			
			c1.add(Calendar.MINUTE, tran.getChargingUnitMinutes());			
			if(c2.getTimeInMillis() >= c1.getTimeInMillis() /*|| cp.getFrom().getTime() + CHARGING_BUFFER_MIN * 60000 < c2.getTimeInMillis()*/) {
				cp.setTo(new Date(c1.getTimeInMillis() - 60000));
				cp.setOnPeak(isOnPeak(cp.getFrom(), onPeakStart, onPeakEnd));
				list.add(cp);
			}			
			else {
				//handle postpaid
				if(!CtUtil.isModePrepaid(tran)) {
					cp.setTo(c2.getTime());
					int min = (int)((cp.getTo().getTime() - cp.getFrom().getTime()) / 60000);
					logger.info("Last period duration (min):" + min + ", allowance:" + CtUtil.getServConfig().getPostpaidAllowanceMin());
					//check allowance
					if(min > CtUtil.getServConfig().getPostpaidAllowanceMin()) {
						cp.setOnPeak(isOnPeak(cp.getFrom(), onPeakStart, onPeakEnd));
						list.add(cp);
					}
					else {
						logger.info("Within allowance.");
					}
				}
			}
		} while(c2.getTimeInMillis() > c1.getTimeInMillis());
		
		logger.info("Charging period between " + DateUtil.formatDateTime(tran.getStartDttm(), true) + " and " + DateUtil.formatDateTime(tran.getEndDttm(), true) + ":");
		if(list.size() > 0) {			
			for(ChargingPeriod cp2 : list) {
				logger.info("\t" + cp2);
			}
		}
		else {
			logger.info("\tEmpty.");
		}
		
		return list;
	}
	
	public static BigDecimal roundDown(BigDecimal dec) {
		if(dec == null) {
			return BigDecimal.ZERO;
		}
		return dec.setScale(1, RoundingMode.DOWN);
	}
	
	public static BigDecimal getRatePerUnit(BigDecimal ratePerHour, int minPerUnit) {
		return ratePerHour.multiply(new BigDecimal(minPerUnit)).divide(new BigDecimal(60), 1, RoundingMode.DOWN);
	}
	
	public static BigDecimal getRatePerHour(BigDecimal ratePerUnit, int minPerUnit) {
		return ratePerUnit.multiply(new BigDecimal(60)).divide(new BigDecimal(minPerUnit), 1, RoundingMode.DOWN);
	}
	
	public static BigDecimal getTimeRateOffPeakPerUnit(RateModel rm, RateDetailModel rd) {
		if(rd == null) {
			rd = getRateDetail();
		}
		return getRatePerUnit(rd.getOffPeakTimeRate(), rm.getMins());
	}
	
	public static BigDecimal getTimeRateOnPeakPerUnit(RateModel rm, RateDetailModel rd) {
		if(rd == null) {
			rd = getRateDetail();
		}
		return getRatePerUnit(rd.getOnPeakTimeRate(), rm.getMins());
	}

	public static void calcEnergyCharge(TranModel tran) {		
		//set the rate info if not yet set
		if(tran.getEnergyRateOffPeak() == null && CtUtil.isEnergyEnabled(tran)) {
			RateDetailModel rd = getRateDetail();
			tran.setEnergyRateOffPeak(rd.getOffPeakEnergyRate());
			tran.setEnergyRateOnPeak(rd.getOnPeakEnergyRate());
		}
		
		tran.setEnergyChargeOnPeak(BigDecimal.ZERO);
		tran.setEnergyChargeOffPeak(BigDecimal.ZERO);
		tran.setEnergyCharge(BigDecimal.ZERO);
		
		tran.setEnergyConsumedOffPeak(BigDecimal.ZERO);
		tran.setEnergyConsumedOnPeak(BigDecimal.ZERO);
		tran.setEnergyConsumed(BigDecimal.ZERO);
		
		boolean meterReady = tran.getMeterStart() != null && tran.getMeterStop() != null;
		logger.info("energyEnabled:" + tran.getEnergyEnabled() + ", meter start:" + tran.getMeterStart() + ", stop:" + tran.getMeterStop());
		
		if(CtUtil.isEnergyEnabled(tran) && meterReady) {
			BigDecimal tot = tran.getMeterStop().subtract(tran.getMeterStart());
			//CK @ 20191205, handle negative value cases
			if(tot.intValue() < 0) {
				if(tot.intValue() * -1 > EMERGY_NEGATIVE_VALUE_ALLOWANCE) {
					tot = tot.add(new BigDecimal(10000000000L));
					logger.info("Meter reset, tot updated to:" + tot);
				}
				else {
					logger.warn("Energy NEGATIVATE value:" + tot);
					tot = BigDecimal.ZERO;
				}
			}
			tot = tot.divide(new BigDecimal(1000, MathContext.DECIMAL32));
			
			BigDecimal durationOn = new BigDecimal(tran.getDurationOnPeak());
			BigDecimal durationTot = new BigDecimal(tran.getDurationMin());
			 
			if(durationTot.compareTo(BigDecimal.ZERO) > 0) {
				tran.setEnergyConsumedOnPeak(roundDown(tot.multiply(durationOn.divide(durationTot, MathContext.DECIMAL32))));
				tran.setEnergyConsumedOffPeak(tot.subtract(tran.getEnergyConsumedOnPeak()));
				tran.setEnergyConsumed(tot);
				logger.info("kwh tot:" + tot + ", on:" + tran.getEnergyConsumedOnPeak() + ", off:" + tran.getEnergyConsumedOffPeak());
				
				tran.setEnergyChargeOnPeak(roundDown(tran.getEnergyConsumedOnPeak().multiply(tran.getEnergyRateOnPeak())));
				if(CtUtil.isOnOffPeakSameEnergyRate(tran)) {
					tran.setEnergyCharge(roundDown(tot.multiply(tran.getEnergyRateOnPeak())));
					tran.setEnergyChargeOffPeak(tran.getEnergyCharge().subtract(tran.getEnergyChargeOnPeak()));
				}
				else {
					tran.setEnergyChargeOffPeak(roundDown(tran.getEnergyConsumedOffPeak().multiply(tran.getEnergyRateOffPeak())));
					tran.setEnergyCharge(tran.getEnergyChargeOffPeak().add(tran.getEnergyChargeOnPeak()));
				}
				
				logger.info("$ tot:" + tran.getEnergyCharge() + ", on:" + tran.getEnergyChargeOnPeak() + ", off:" + tran.getEnergyChargeOffPeak());
			}
			else {
				logger.warn("durationTot is zero");
			}		
		}
	}
	
	public static void calcTimeCharge(TranModel tran) {		
		//set the rate info if not yet set
		if(tran.getTimeRateOffPeakPerUnit() == null && CtUtil.isTimeEnabled(tran)) {
			RateModel rm = getRate();
			RateDetailModel rd = getRateDetail();
			
			
			tran.setMinChargingUnit(rm.getMinChargingUnit());		
			tran.setTimeRateOffPeakPerUnit(getTimeRateOffPeakPerUnit(rm, rd));
			tran.setTimeRateOnPeakPerUnit(getTimeRateOnPeakPerUnit(rm, rd));
			tran.setChargingUnitMinutes(rm.getMins());
		}
		
		if(tran.getChargingUnitMinutes() == null) {	//for energy charge only
			tran.setChargingUnitMinutes(getRate().getMins());
		}
		
		
		int onPeak = 0;
		int offPeak = 0;
		
		Date tempStartTime = tran.getStartDttm();
		Integer freeTimeMins = tran.getDurationFreeMin() == null ? 0 : tran.getDurationFreeMin();
		if(freeTimeMins != 0 ) {
			Calendar newStartTime = Calendar.getInstance();
			newStartTime.setTime(tempStartTime);
			newStartTime.add(Calendar.MINUTE, freeTimeMins);
			tran.setStartDttm(newStartTime.getTime());	
			logger.debug("newStartTime: " + newStartTime.getTime());
		}
		if(tran.getStartDttm().before(tran.getEndDttm())) {
			List<ChargingPeriod> periodList = getPeriodList(tran);
			for(ChargingPeriod cp : periodList) {
				if(cp.isOnPeak()) {
					onPeak ++;
				}
				else {
					offPeak ++;
				}
			}
			tran.setTotChargingUnit(periodList.size()+freeTimeMins/tran.getChargingUnitMinutes());
		} else {
			if(EvCons.MODE_POSTPAID.equals(tran.getMode())) {
				tran.setTotChargingUnit((int) (Math.ceil(tran.getEndDttm().getTime() - tempStartTime.getTime())/(tran.getChargingUnitMinutes() * 60 * 1000) ));
			} else {
				tran.setTotChargingUnit(freeTimeMins/tran.getChargingUnitMinutes());
			}
		}
		if(freeTimeMins != 0 ) {
			tran.setStartDttm(tempStartTime);	
		}
		tran.setDurationOffPeak(offPeak * tran.getChargingUnitMinutes());
		tran.setDurationOnPeak(onPeak * tran.getChargingUnitMinutes());
		tran.setDurationMin(tran.getTotChargingUnit() * tran.getChargingUnitMinutes());

		//tran.setDurationMin(DateUtil.getMinBetween(tran.getStartDttm(), tran.getEndDttm()));
		logger.info("getTimeRateOffPeakPerUnit()\t" + tran.getTimeRateOffPeakPerUnit());
		logger.info("getTimeRateOnPeakPerUnit()\t" + tran.getTimeRateOnPeakPerUnit());
		logger.info("getDurationOffPeak()\t" + tran.getDurationOffPeak());
		logger.info("getDurationOnPeak()\t" + tran.getDurationOnPeak());
		logger.info("getDuration()\t" + tran.getDurationMin());
		
		
		if(CtUtil.isTimeEnabled(tran)) {
			tran.setTimeChargeOffPeak(roundDown(tran.getTimeRateOffPeakPerUnit().multiply(new BigDecimal(offPeak))));
			tran.setTimeChargeOnPeak(roundDown(tran.getTimeRateOnPeakPerUnit().multiply(new BigDecimal(onPeak))));
			tran.setTimeCharge(tran.getTimeChargeOnPeak().add(tran.getTimeChargeOffPeak()));			
		}
		else {
			tran.setTimeChargeOnPeak(BigDecimal.ZERO);
			tran.setTimeChargeOffPeak(BigDecimal.ZERO);
			tran.setTimeCharge(BigDecimal.ZERO);
		}
		
		logger.info("getTimeChargeOnPeak()\t" + tran.getTimeChargeOnPeak());
		logger.info("getTimeChargeOffPeak()\t" + tran.getTimeChargeOffPeak());
		logger.info("getTimeCharge()\t" + tran.getTimeCharge());
	}
	
	public static void setTimeEnergyMode(TranModel tran) {
		RateDetailModel rd = getRateDetail();
		tran.setTimeEnabled(rd.getTimeChargeEnableFlag());
		if(!CtUtil.isModePrepaid()) {
			tran.setEnergyEnabled(rd.getEnergyChargeEnableFlag());
		}
	}
		
	public static BigDecimal calcChargingFee(TranModel tran) {
		if(tran.getEndDttm() == null) {
			tran.setEndDttm(new Date());
		}		
		
		tran.setTimeCharge(BigDecimal.ZERO);
		tran.setEnergyCharge(BigDecimal.ZERO);
		
		if(CtUtil.isModePrepaid(tran)) {
			calcTimeCharge(tran);
		}
		else {
			calcTimeCharge(tran);
			calcEnergyCharge(tran);
		}
				
		tran.setAmt(tran.getTimeCharge().add(tran.getEnergyCharge()));
		
		//CK @ 20180817, check whether exceed max amount
		if(!CtUtil.isModePrepaid()) {
			if(isExceedMaxChargingFee(tran)) {
				logger.info("Exceed max amount:" + CtUtil.getConfig().getPostpaidMaxServiceFee() + ", org amount:" + tran.getAmt() + ", set to max amount!");
				tran.setAmt(new BigDecimal(CtUtil.getConfig().getPostpaidMaxServiceFee()));
			}
		}
		
		logger.info("getAmt()\t" + tran.getAmt() + ", time:" + tran.getTimeCharge() + ", energy:" + tran.getEnergyCharge());
		
		return tran.getAmt();
	}
	
	public static RateModel getRate() {
		ServConfig sc = CtUtil.getServConfig();
		if(sc == null) {
			return null;
		}
		
		return sc.getRate(); 
	}
	
	public static RateDetailModel getRateDetail() {
		ServConfig sc = CtUtil.getServConfig();
		if(sc == null) {
			return null;
		}
		
		return sc.getRate().getCurrentRate();
	}
	
	public static int getMaxChargingDurationMin() {
		RateModel rd = getRate();
		if(rd == null || CtUtil.getServConfig().getMaxChargingUnit() == null) {
			return 120;
		}
		else {
			return rd.getMins() * CtUtil.getServConfig().getMaxChargingUnit();
		}
	}
	
	public static String getHourMinString(int hourMin) {
		String res = "";
		
		int hour = hourMin / 100;
		if(hour == 0) {
			res = "00";
		}
		else if(hour < 10) {
			res = "0" + hour;
		}
		else {
			res = "" + hour;
		}
		
		int min = hourMin % 100;
		if(min == 0) {
			res += ":00";
		}
		else if(min < 10) {
			res += ":0" + min;
		}
		else {
			res += ":" + min;
		}
		
		
		return res;
	}
	
	/**
	 * For postpaid only
	 * @param tran
	 * @return
	 */
	public static boolean isExceedMaxChargingFee(TranModel tran) {
		if(tran != null && tran.getAmt() != null) {
			boolean res = tran.getAmt().intValue() > CtUtil.getConfig().getPostpaidMaxServiceFee();
			return res;
		}
		
		return false;
	}
	
	public static boolean isEnergyEnabled(RateDetailModel rd) {
		return "Y".equalsIgnoreCase(rd.getEnergyChargeEnableFlag());
	}
	
	public static boolean isOnOffPeakEnergySameRate(RateDetailModel rd) {
		return rd.getOnPeakEnergyRate().equals(rd.getOffPeakEnergyRate());
	}
	
	public static boolean isTimeEnabled(RateDetailModel rd) {
		return "Y".equalsIgnoreCase(rd.getTimeChargeEnableFlag());
	}
	
	public static boolean isOnOffPeakTimeSameRate(RateDetailModel rd) {
		return rd.getOnPeakTimeRate().equals(rd.getOffPeakTimeRate());
	}
}
