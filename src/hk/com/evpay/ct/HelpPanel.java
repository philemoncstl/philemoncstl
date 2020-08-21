package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;

import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import hk.com.cstl.evcs.model.RateDetailModel;
import hk.com.cstl.evcs.model.RateModel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.RateUtil;

public class HelpPanel extends CommonPanel{
	private static Logger logger = Logger.getLogger(HelpPanel.class);
	
	public static String[] LABEL = new String[]{
			"cpStatusAvailable",
			"cpStatusPreparing",
			"cpStatusSuspendedEVSE",
			"cpStatusCharging",
			"cpNoLabel",
			"noCp",
			"cpStatusUnavailableHelp"
	};
	
	public static String[] DESC = new String[]{
			"cpStatusDescAvailable",
			"cpStatusDescPreparing",
			"cpStatusDescSuspenedEVSE",
			"cpStatusDescCharging",
			"noNetworkDesc",
			"noCpDesc",
			"cpStatusDescUnavailable"
	};
	
	private I18nLabel lblServiceFee;	

	public HelpPanel(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		lblServiceFee = new I18nLabel("chargingServiceFeeHelp", "");
		lblServiceFee.setHorizontalAlignment(SwingConstants.CENTER);
		lblServiceFee.setVerticalAlignment(SwingConstants.CENTER);
		lblServiceFee.setBounds(calcBoundsLabel(206, 240, 852, 168));
		lblServiceFee.setForeground(Color.WHITE);
		LangUtil.setFont(lblServiceFee, Font.PLAIN, 36);
		add(lblServiceFee);
		
		int x = 158;
		int y = 616;
		int w = 100;
		int h = 64;
		
		int hgap = 43;
		int vgap = 90;
		
		for(int i = 0; i < LABEL.length; i ++) {
			I18nLabel lbl = new I18nLabel(LABEL[i], "");			
			lbl.setBounds(calcBoundsLabel(x + i * (w + hgap), y, w, h));
			lbl.setForeground(Color.WHITE);
			LangUtil.setFont(lbl, Font.PLAIN, 18);
			lbl.setHorizontalAlignment(SwingConstants.CENTER);
			lbl.setVerticalAlignment(SwingConstants.CENTER);
			add(lbl);
			//logger.info("lbl:" + lbl.getBounds() + ", " + lbl.getMsgCode() + ", " + lbl.getText());
			
			lbl = new I18nLabel(DESC[i], "");
			add(lbl);
			lbl.setBounds(calcBoundsLabel(x + i * (w + hgap) - 15, y + vgap, w + 40, h - 15));
			LangUtil.setFont(lbl, Font.PLAIN, 16);
			lbl.setHorizontalAlignment(SwingConstants.CENTER);
			lbl.setVerticalAlignment(SwingConstants.CENTER);
			add(lbl);
			//logger.info("desc:" + lbl.getBounds() + ", " + lbl.getMsgCode() + ", " + lbl.getText());
		}
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		setRateInfo(lblServiceFee);
	}
	
	public static void setRateInfo(I18nLabel lblServiceFee) {
		RateModel rm = RateUtil.getRate();
		RateDetailModel rd = RateUtil.getRateDetail();
		
		boolean time = rd.isTimeEnabled();
		boolean energy = rd.isEnergyEnabled();
		
		boolean prepaid = CtUtil.isModePrepaid();
		
		//time & energy
		if(time && energy && !prepaid) {
			if(rd.isOnOffPeakSameRate()) {
				lblServiceFee.setMsgCode("chargingServiceFeeHelpBoth");
				lblServiceFee.setParms(new String[] {
						CtUtil.bigDecimalToString(RateUtil.getTimeRateOffPeakPerUnit(rm, rd)), 
						String.valueOf(rm.getMins()),
						CtUtil.bigDecimalToString(rd.getOffPeakEnergyRate())
				});
			}
			else {
				lblServiceFee.setMsgCode("chargingServiceFeeHelpBothOnOffPeak");
				lblServiceFee.setParms(new String[] {
						CtUtil.bigDecimalToString(RateUtil.getTimeRateOffPeakPerUnit(rm, rd)), 
						String.valueOf(rm.getMins()),
						CtUtil.bigDecimalToString(rd.getOffPeakEnergyRate()),
						" (" + rd.getOnPeakEndTime() + " - " + rd.getOnPeakStartTime() + ")",
						CtUtil.bigDecimalToString(RateUtil.getTimeRateOnPeakPerUnit(rm, rd)),
						String.valueOf(rm.getMins()),
						CtUtil.bigDecimalToString(rd.getOnPeakEnergyRate()),
						" (" + rd.getOnPeakStartTime() + " - " + rd.getOnPeakEndTime() + ")"
				});
			}
		}
		//energy only
		else if(energy && !prepaid) {
			if(rd.isOnOffPeakSameEnergyRate()) {
				lblServiceFee.setMsgCode("chargingServiceFeeHelpEnergy");
				lblServiceFee.setParms(CtUtil.bigDecimalToString(rd.getOffPeakEnergyRate()));
			}
			else {
				lblServiceFee.setMsgCode("chargingServiceFeeHelpEnergyOnOffPeak");
				lblServiceFee.setParms(
					new String[] {
							CtUtil.bigDecimalToString(rd.getOffPeakEnergyRate()),
							" (" + rd.getOnPeakEndTime() + " - " + rd.getOnPeakStartTime() + ")",
							CtUtil.bigDecimalToString(rd.getOnPeakEnergyRate()),
							" (" + rd.getOnPeakStartTime() + " - " + rd.getOnPeakEndTime() + ")"
					});
				
			}
		}
		//time only
		else {
			if(rd.isOnOffPeakSameTimeRate()) {
				lblServiceFee.setMsgCode("chargingServiceFeeHelp");
				lblServiceFee.setParms(CtUtil.bigDecimalToString(RateUtil.getTimeRateOffPeakPerUnit(rm, rd)), String.valueOf(rm.getMins()));
			}
			else {
				lblServiceFee.setMsgCode("chargingServiceFeeHelpOnOffPeak");
				lblServiceFee.setParms(
					new String[] {
							CtUtil.bigDecimalToString(RateUtil.getTimeRateOffPeakPerUnit(rm, rd)),
							String.valueOf(rm.getMins()),
							" (" + rd.getOnPeakEndTime() + " - " + rd.getOnPeakStartTime() + ")",
							CtUtil.bigDecimalToString(RateUtil.getTimeRateOnPeakPerUnit(rm, rd)),
							String.valueOf(rm.getMins()),
							" (" + rd.getOnPeakStartTime() + " - " + rd.getOnPeakEndTime() + ")"
					});
				
			}
		}
	}
	
	
	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_HELP;
	}
}
