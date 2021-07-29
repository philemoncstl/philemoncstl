package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.i18n.FieldPanel;
import hk.com.evpay.ct.i18n.FieldPanel2Label;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class PostStep6ShowReceipt extends CommonPanel{
	private static final Logger logger = Logger.getLogger(PostStep6ShowReceipt.class);
	
	private I18nButtonLabel lblBgInUse;
	
	private I18nLabel lblCpStatus;
	
	private FieldPanel fpTranDttm;
	private FieldPanel fpCp;
	private FieldPanel fpStartTime;
	private FieldPanel fpEndTime;
	private FieldPanel fpDuration;
	private FieldPanel fpFreeDuration;
	
	private FieldPanel2Label fpTimeChargeOffPeak;
	private FieldPanel2Label fpTimeChargeOnPeak;
	
	private FieldPanel2Label fpEnergyChargeOffPeak;
	private FieldPanel2Label fpEnergyChargeOnPeak;
	
	private FieldPanel2Label fpTimeCharge;
	private FieldPanel2Label fpEnergyCharge;
	
	private FieldPanel fpAmount;
	private FieldPanel fpReceiptNo;
	private FieldPanel fpOctDeviceNo;
	private FieldPanel fpOctNo;
	private FieldPanel fpOctDeductAmount;
	private FieldPanel fpOctRemainingBal;
	
	
	private I18nLabel lblPrintReceipt;	
	private I18nButtonLabel btnNo;
	private I18nButtonLabel btnYes;
	
	private JScrollPane jsp = null;
	private JPanel pnl = null;
	private Dimension dimSameRate = null;
	private Dimension dimDiffRate = null;

	public PostStep6ShowReceipt(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		lblCpStatus = createLabel("", "", 217, 260, 845, 60);
		LangUtil.setFont(lblCpStatus, Font.PLAIN, 40);
		add(lblCpStatus);
		
		pnl = new JPanel();
		pnl.setOpaque(false);
		jsp = new JScrollPane(pnl, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jsp.getVerticalScrollBar().setUnitIncrement(200);
		jsp.setOpaque(false);
		UiUtil.debugUi(pnl);
		UiUtil.debugUi(jsp);
		
		add(jsp);
		Rectangle r = calcBoundsLabel(220, 335, 845, 422);
		jsp.setBounds(r);
		dimSameRate = new Dimension(r.width - 30, r.height - 10);
		dimDiffRate = new Dimension(r.width - 30, r.height + 180);
		pnl.setPreferredSize(dimDiffRate);
		
		fpTranDttm = new FieldPanel("tranDatetime", "i18nLabel");
		pnl.add(fpTranDttm);
		
		fpCp = new FieldPanel("cpNoLabel", "i18nLabel");
		pnl.add(fpCp);
		
		fpStartTime = new FieldPanel("startTime", "i18nLabel");
		pnl.add(fpStartTime);
		
		fpEndTime = new FieldPanel("stopTime", "i18nLabel");
		pnl.add(fpEndTime);
		
		fpDuration = new FieldPanel("chargingTime", "chargingTimeVal");
		pnl.add(fpDuration);
		
        if(CtUtil.getServConfig().getFreeTimeUnit() != null && CtUtil.getServConfig().getFreeTimeUnit() >0) {
			fpFreeDuration = new FieldPanel("freeChargingDuration", "chargingTimeVal"); 
			pnl.add(fpFreeDuration);
        }
		//off-peak charging fee
		fpTimeChargeOffPeak = new FieldPanel2Label("offPeakChargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeChargeOffPeak);
		fpEnergyChargeOffPeak = new FieldPanel2Label("offPeakChargingFee", "energyUnit", "hkdWithVal2");
		fpEnergyChargeOffPeak.getLbl().setVisible(false);
		pnl.add(fpEnergyChargeOffPeak);
		
		//on-peak charging fee
		fpTimeChargeOnPeak = new FieldPanel2Label("onPeakChargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeChargeOnPeak);
		fpEnergyChargeOnPeak = new FieldPanel2Label("offPeakChargingFee", "energyUnit", "hkdWithVal2");
		fpEnergyChargeOnPeak.getLbl().setVisible(false);
		pnl.add(fpEnergyChargeOnPeak);
		
		
		//charging fee (on/off peak same rate)
		fpTimeCharge = new FieldPanel2Label("chargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeCharge);
		fpEnergyCharge = new FieldPanel2Label("chargingFee", "energyUnit", "hkdWithVal2");
		pnl.add(fpEnergyCharge);
		
		fpAmount = new FieldPanel("totalAmount", "hkdWithVal2");
		fpAmount.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
		pnl.add(fpAmount);
		
		fpReceiptNo = new FieldPanel("receiptNo", "i18nLabel");
		pnl.add(fpReceiptNo);
		
		fpOctDeviceNo = new FieldPanel("deviceNo", "i18nLabel");
		pnl.add(fpOctDeviceNo);
		
		fpOctNo = new FieldPanel("octopusNo", "i18nLabel");
		pnl.add(fpOctNo);
		
		fpOctDeductAmount = new FieldPanel("deductAmount", "hkdWithVal2");
		pnl.add(fpOctDeductAmount);
		
		fpOctRemainingBal = new FieldPanel("remainingValue", "hkdWithVal2");
		pnl.add(fpOctRemainingBal);
		
		lblBgInUse = createButton("img/checkStatusInUseReceipt.png", 190, 250, 920, 528);
		//lblBgInUse = createButton("img/checkStatusInUse.png", 190, 250, 920, 401);
		add(lblBgInUse);	
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		lblCpStatus.setMsgCode("cpStatus" + cp.getCp().getStatus());
		fpCp.getVal().setMsgCode(cp.getCp().getCpNo());
		
		TranModel tm = cp.getCp().getTran();
		if(tm == null) {
			fpStartTime.getVal().setText("");
		}
		else {
			fpTranDttm.getVal().setParms(DateUtil.formatDateTime(tm.getTranDttm(), true));
			fpStartTime.getVal().setParms(DateUtil.formatDateTime(tm.getStartDttm(), false));
			fpEndTime.getVal().setParms(DateUtil.formatDateTime(tm.getEndDttm(), false));
			
			int duration = tm.getDurationMin();
			int h = duration / 60;
			int m = duration % 60;
			fpDuration.getVal().setParms(h, m);
			if(fpFreeDuration != null) {
				int freeDuration = tm.getDurationFreeMin();
				h = freeDuration / 60;
				m = freeDuration % 60;
				fpFreeDuration.getVal().setParms(h, m);
			}
			
			fpTimeChargeOffPeak.setVisible(false);			
			fpTimeChargeOnPeak.setVisible(false);
			fpTimeCharge.setVisible(false);
			
			fpEnergyChargeOffPeak.setVisible(false);			
			fpEnergyChargeOnPeak.setVisible(false);			
			fpEnergyCharge.setVisible(false);			
			
			
			boolean time = CtUtil.isTimeEnabled(tm);
			boolean energy = CtUtil.isEnergyEnabled(tm);
			logger.info("time:" + time + ", energy:" + energy);
			
			if(time) {
				fpTimeChargeOffPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeChargeOffPeak()));
				fpTimeChargeOffPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOffPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));
				fpTimeChargeOnPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeChargeOnPeak()));
				fpTimeChargeOnPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOnPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));
				fpTimeCharge.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeCharge()));
				fpTimeCharge.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOffPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));
			}
			
			if(energy) {
				fpEnergyChargeOffPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getEnergyChargeOffPeak()));
				fpEnergyChargeOffPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getEnergyRateOffPeak()));
				fpEnergyChargeOnPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getEnergyChargeOnPeak()));
				fpEnergyChargeOnPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getEnergyRateOnPeak()));
				fpEnergyCharge.getVal().setParms(CtUtil.bigDecimalToString(tm.getEnergyCharge()));
				fpEnergyCharge.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getEnergyRateOffPeak()));
			}
			
			
			boolean useSameRateSize = false;
			
			//time & energy
			if(time && energy) {
				if(CtUtil.isSameRate(tm)) {
					fpTimeCharge.setVisible(true);
					fpEnergyCharge.setVisible(true);
					fpEnergyCharge.getLbl().setVisible(false);
				}
				else {
					fpTimeChargeOffPeak.setVisible(true);
					fpEnergyChargeOffPeak.setVisible(true);
					fpEnergyChargeOffPeak.getLbl().setVisible(false);
					
					fpTimeChargeOnPeak.setVisible(true);
					fpEnergyChargeOnPeak.setVisible(true);
					fpEnergyChargeOnPeak.getLbl().setVisible(false);
				}
			}
			//energy only
			else if(energy) {
				if(CtUtil.isOnOffPeakSameEnergyRate(tm)) {
					fpEnergyCharge.setVisible(true);
					fpEnergyCharge.getLbl().setVisible(true);
					useSameRateSize  = true;
				}
				else {
					fpEnergyChargeOffPeak.setVisible(true);
					fpEnergyChargeOffPeak.getLbl().setVisible(true);
					fpEnergyChargeOnPeak.setVisible(true);
					fpEnergyChargeOnPeak.getLbl().setVisible(true);
				}
			}
			//time only
			else {
				if(CtUtil.isOnOffPeakSameTimeRate(tm)) {
					fpTimeCharge.setVisible(true);
					useSameRateSize  = true;
				}
				else {
					fpTimeChargeOffPeak.setVisible(true);
					fpTimeChargeOnPeak.setVisible(true);
				}
			}
			
			if(useSameRateSize) {
				pnl.setPreferredSize(dimSameRate);
			}
			else {
				pnl.setPreferredSize(dimDiffRate);
			}
			
			String amt = CtUtil.bigDecimalToString(tm.getAmt());
			fpAmount.getVal().setParms(amt);
			fpReceiptNo.getVal().setParms(tm.getReceiptNo());
			
			//Handle for Octopus payment
			boolean oct = CtUtil.isPayByOctopus(tm);
			fpOctDeviceNo.setVisible(oct);
			fpOctNo.setVisible(oct);
			fpOctDeductAmount.setVisible(oct);
			//fpOctRemainingBal.setVisible(oct);
			//2018-05-04, handle smart octopus
			if(oct && !"Y".equals(tm.getSmartOctopus())) {
				fpOctRemainingBal.setVisible(true);
			}
			else {
				fpOctRemainingBal.setVisible(false);
			}
			
			if(oct) {
				fpOctDeviceNo.getVal().setParms(tm.getOctopusDeviceNo());
				fpOctNo.getVal().setParms(tm.getOctopusNo());
				fpOctDeductAmount.getVal().setParms(amt);
				fpOctRemainingBal.getVal().setParms(CtUtil.bigDecimalToString(tm.getRemainBal()));
			}
			
			//reset to top
			jsp.getViewport().setViewPosition(new Point(0, 0));

			
			new Thread() {
				public void run() {
					logger.info("Go to unplug cable after " + CtUtil.getConfig().getPostpaidShowReceiptDelayMs()+ " ms");
					try {
						Thread.sleep(CtUtil.getConfig().getPostpaidShowReceiptDelayMs());
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
					
					if(pnlCtrl.isCurrentDisplayingPanel(PostStep6ShowReceipt.this)) {
						SwingUtilities.invokeLater(new Runnable() {			
							@Override
							public void run() {
								pnlCtrl.goToStep8UnplugCable();
							}
						});
					}
				}
			}.start();
		}
	}

	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}

	@Override
	public String getTitleMsgKey() {
		return "chargingStatus";
	}
}
