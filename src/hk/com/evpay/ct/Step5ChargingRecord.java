package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

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
import hk.com.evpay.ct.util.UiUtil;

public class Step5ChargingRecord extends CommonPanel{
	private static final Logger logger = Logger.getLogger(Step5ChargingRecord.class);
	
	private I18nButtonLabel lblBgInUse;
	
	private I18nLabel lblCpStatus;
	
	private FieldPanel fpTranDttm;
	private FieldPanel fpCp;
	private FieldPanel fpStartTime;
	private FieldPanel fpEndTime;
	private FieldPanel fpDuration;
	private FieldPanel2Label fpTimeChargeOffPeak;
	private FieldPanel2Label fpTimeChargeOnPeak;
	private FieldPanel2Label fpTimeCharge;
	private FieldPanel fpAmount;
	private FieldPanel fpReceiptNo;
	private FieldPanel fpOctDeviceNo;
	private FieldPanel fpOctNo;
	private FieldPanel fpOctDeductAmount;
	private FieldPanel fpOctRemainingBal;
	
	private JScrollPane jsp = null;
	private JPanel pnl = null;
	private Dimension dimSameRate = null;
	private Dimension dimDiffRate = null;

	public Step5ChargingRecord(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);
		
		lblCpStatus = createLabel("", "", 217, 260, 845, 60);
		LangUtil.setFont(lblCpStatus, Font.PLAIN, 40);
		add(lblCpStatus);
		
		pnl = new JPanel();
		pnl.setOpaque(false);
		jsp = new JScrollPane(pnl, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jsp.getVerticalScrollBar().setUnitIncrement(200);
		//jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setOpaque(false);
		UiUtil.debugUi(pnl);
		UiUtil.debugUi(jsp);
		
		add(jsp);
		Rectangle r = calcBoundsLabel(220, 335, 845, 420);
		jsp.setBounds(r);
		dimSameRate = new Dimension(r.width - 30, r.height - 10);
		dimDiffRate = new Dimension(r.width - 30, r.height + 60);
		pnl.setPreferredSize(dimSameRate);
		
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
		
		//or serviceFeeTimeUnit
		fpTimeChargeOffPeak = new FieldPanel2Label("offPeakChargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeChargeOffPeak);
		
		fpTimeChargeOnPeak = new FieldPanel2Label("onPeakChargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeChargeOnPeak);
		
		fpTimeCharge = new FieldPanel2Label("chargingFee", "timeUnit", "hkdWithVal2");
		pnl.add(fpTimeCharge);
		
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
			
			
			fpTimeChargeOffPeak.setVisible(false);
			fpTimeChargeOffPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeChargeOffPeak()));
			fpTimeChargeOffPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOffPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));
			fpTimeChargeOnPeak.setVisible(false);
			fpTimeChargeOnPeak.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeChargeOnPeak()));
			fpTimeChargeOnPeak.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOnPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));

			fpTimeCharge.setVisible(false);
			fpTimeCharge.getVal().setParms(CtUtil.bigDecimalToString(tm.getTimeCharge()));
			fpTimeCharge.getLbl2().setParms(CtUtil.bigDecimalToString(tm.getTimeRateOffPeakPerUnit()), String.valueOf(tm.getChargingUnitMinutes()));
			
			if(CtUtil.isOnOffPeakSameTimeRate(tm)) {
				fpTimeCharge.setVisible(true);
				pnl.setPreferredSize(dimSameRate);
			}
			else {
				fpTimeChargeOffPeak.setVisible(true);
				fpTimeChargeOnPeak.setVisible(true);
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
