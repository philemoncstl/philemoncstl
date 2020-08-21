package hk.com.evpay.ct.test;

import javax.swing.JFrame;

import hk.com.evpay.ct.PostStep4StopCharging;
import hk.com.evpay.ct.Step1SelectTime;

public class TestLayout {
	public static void main(String[] args) {
		hk.com.evpay.ct.util.LangUtil.setEnglish();
		JFrame f = new JFrame("Testing");
		//PostStep6ShowReceipt pnl = new PostStep6ShowReceipt(null);
		//PostStep4StopCharging pnl = new PostStep4StopCharging(null);
		//Step5ChargingRecord pnl = new Step5ChargingRecord(null);
		//CommonPanelOctopus pnl = new Step2ProcessPayment(null);
		Step1SelectTime pnl = new Step1SelectTime(null);
		f.getContentPane().add(pnl);
		f.setSize(1280, 800);
		f.setVisible(true);
	}
}
