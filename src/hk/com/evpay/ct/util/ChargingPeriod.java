package hk.com.evpay.ct.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChargingPeriod {
	private static final DateFormat DF = new SimpleDateFormat("HH:mm");
	private Date from;
	private Date to;
	private boolean onPeak;
	
	public ChargingPeriod() {
		
	}
	
	public ChargingPeriod(Date from, Date to, boolean onPeak) {
		super();
		this.from = from;
		this.to = to;
		this.onPeak = onPeak;
	}


	@Override
	public String toString() {
		return "Period[" + DF.format(from) + " ~ " + DF.format(to) + ", " + (onPeak ? "On-peak" : "Off-peak") + "]";
	}
	
	public Date getFrom() {
		return from;
	}
	public void setFrom(Date from) {
		this.from = from;
	}
	public Date getTo() {
		return to;
	}
	public void setTo(Date to) {
		this.to = to;
	}
	public boolean isOnPeak() {
		return onPeak;
	}
	public void setOnPeak(boolean onPeak) {
		this.onPeak = onPeak;
	}
}
