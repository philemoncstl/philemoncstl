package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.i18n.FieldPanel;
import hk.com.evpay.ct.i18n.FieldPanel2Label;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.RateUtil;
import hk.com.evpay.ct.util.UiUtil;

public class PostStep4StopCharging extends CommonPanel{
	private static final Logger logger = Logger.getLogger(PostStep4StopCharging.class);
	
	private I18nButtonLabel lblBgInUse;
	
	private I18nLabel lblCpStatus;

	private FieldPanel fpCp;
	private FieldPanel fpStartTime;
	//private FieldPanel fpEndTime;
	private FieldPanel fpDuration;
	
	private FieldPanel fpKwh;	//20190730, display it for energy instead of duration	
	
	private FieldPanel2Label fpTimeChargeOffPeak;
	private FieldPanel2Label fpTimeChargeOnPeak;
	
	private FieldPanel2Label fpEnergyChargeOffPeak;
	private FieldPanel2Label fpEnergyChargeOnPeak;
	
	private FieldPanel2Label fpTimeCharge;
	private FieldPanel2Label fpEnergyCharge;
	
	private FieldPanel fpAmount;
	
	private I18nButtonLabel btnBack;
	private I18nButtonLabel btnStopCharging;

	public PostStep4StopCharging(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		lblCpStatus = createLabel("", "", 227, 265, 845, 60);
		LangUtil.setFont(lblCpStatus, Font.PLAIN, 40);
		add(lblCpStatus);
		
		JPanel pnl = new JPanel();
		pnl.setOpaque(false);
		JScrollPane jsp = new JScrollPane(pnl, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setOpaque(false);
		UiUtil.debugUi(pnl);
		UiUtil.debugUi(jsp);
		
		add(jsp);
		Rectangle r = calcBoundsLabel(230, 335, 845, 295);
		jsp.setBounds(r);
		pnl.setPreferredSize(new Dimension(r.width - 30, r.height - 10));
		
		fpCp = new FieldPanel("cpNoLabel", "i18nLabel");
		pnl.add(fpCp);
		
		fpStartTime = new FieldPanel("startTime", "i18nLabel");
		pnl.add(fpStartTime);
		
		/*fpEndTime = new FieldPanel("stopTime", "i18nLabel");
		pnl.add(fpEndTime);*/
		
		fpDuration = new FieldPanel("chargingTime", "chargingTimeVal");
		pnl.add(fpDuration);
		
		fpKwh = new FieldPanel("energyConsumed", "energyConsumedVal");
		pnl.add(fpKwh);
		
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
		
		//lblBgInUse = createButton("img/checkStatusInUseReceipt.png", 190, 250, 920, 528);
		lblBgInUse = createButton("img/checkStatusInUse.png", 190, 250, 924, 401);
		add(lblBgInUse);
		
		
		btnBack = createButton("back", "img/btn_back.png", 180, 682);
		add(btnBack);
		
		btnStopCharging = createButton("stopCharging", "img/btn_stop_charging.png", 990, 682);
		add(btnStopCharging);
		
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Object src = e.getSource();
				if(src == btnBack) {
					pnlCtrl.goToHome();
				}
				else if(src == btnStopCharging) {
					stopCharging();
				}
			}
		};
		btnBack.addMouseListener(ma);
		btnStopCharging.addMouseListener(ma);
	}
	
	
	private void stopCharging() {
		//check connection status
		boolean checkConn = pnlCtrl.requestCpConnectionCheck();
		if(!checkConn) {
			pnlCtrl.showErrorMessage("ERR9200", pnlCtrl.getPnlSelectedCp().getCp().getCpNo());
			pnlCtrl.getPnlSelectedCp().setCpConnected(false);	//2018-03-23
			pnlCtrl.getPnlSelectedCp().repaint();
			return;
		}
		
		pnlCtrl.goToPostStep5StopChargingTapCard();
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
			tm.setEndDttm(new Date());
			RateUtil.calcChargingFee(tm);
			fpStartTime.getVal().setParms(DateUtil.formatDateTime(tm.getStartDttm(), false));
			//fpEndTime.getVal().setParms(DateUtil.formatDateTime(tm.getEndDttm(), false));
			
			int duration = tm.getDurationMin();
			int h = duration / 60;
			int m = duration % 60;
			fpDuration.getVal().setParms(h, m);
			
			fpKwh.getVal().setParm(
					tm.getEnergyConsumed() == null ? BigDecimal.ZERO : 
						tm.getEnergyConsumed(), 1, RoundingMode.DOWN);
			
			//20190730, display Kwh when energy charge is enabled instead duration
			fpDuration.setVisible(false);
			fpKwh.setVisible(false);

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
			
				fpKwh.setVisible(true);
			}
			else {
				fpDuration.setVisible(true);
			}
			
			
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
				}
				else {
					fpTimeChargeOffPeak.setVisible(true);
					fpTimeChargeOnPeak.setVisible(true);
				}
			}
		
			String amt = CtUtil.bigDecimalToString(tm.getAmt());
			fpAmount.getVal().setParms(amt);
			
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
