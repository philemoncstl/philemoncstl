package hk.com.evpay.ct;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.JPanel;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.evpay.ct.i18n.FieldPanel;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.LangUtil;

public class Step6StopCharging extends CommonPanel{
	private I18nButtonLabel lblBgInUse;
	private I18nButtonLabel lblBgAvailable;
	
	private I18nLabel lblCpStatus;
	
	private FieldPanel fpCp;
	private FieldPanel fpStartTime;
	private FieldPanel fpRemainTime;
	
	private I18nButtonLabel btnBack;
	private I18nButtonLabel btnStopCharging;
	

	public Step6StopCharging(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		setLayout(null);		
		
		lblCpStatus = createLabel("", "", 217, 272, 845, 60);
		LangUtil.setFont(lblCpStatus, Font.PLAIN, 40);
		add(lblCpStatus);
		
		JPanel pnl = new JPanel();
		pnl.setOpaque(false);
		add(pnl);
		pnl.setBounds(calcBoundsLabel(220, 335, 845, 220));
		
		//CP
		fpCp = new FieldPanel("cpNoLabel", "i18nLabel");		
		pnl.add(fpCp);
		
		fpStartTime = new FieldPanel("startTime", "i18nLabel");
		pnl.add(fpStartTime);
		
		fpRemainTime = new FieldPanel("remainingChargingTime", "chargingTimeVal");
		pnl.add(fpRemainTime);
		
		
		btnBack = createButton("back", "img/btn_back.png", 180, 682);
		add(btnBack);
		
		btnStopCharging = createButton("stopCharging", "img/btn_stop_charging.png", 990, 682);
		add(btnStopCharging);
		
		
		lblBgInUse = createButton("img/checkStatusInUse.png", 170, 258, 924, 401);
		add(lblBgInUse);
		
		lblBgAvailable = createButton("img/checkStatusAvailable.png", 170, 258, 924, 401);
		add(lblBgAvailable);
		
		
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
		
		pnlCtrl.goToStep7StopChargingTapCard();
	}
	
	@Override
	public void onDisplay(CpPanel cp) {
		super.onDisplay(cp);
		
		lblCpStatus.setMsgCode("cpStatus" + cp.getCp().getStatus());
		fpCp.getVal().setParms(cp.getCp().getCpNo());
		
		lblBgAvailable.setVisible(false);
		lblBgInUse.setVisible(false);
		
		//CK @ 20180223, also make SuspendedEVSE displayed as "Expired" as discussed with Victor
		if(cp.getCp().getStatus() == ChargePointStatus.Finishing || cp.getCp().getStatus() == ChargePointStatus.SuspendedEVSE) {
			lblBgAvailable.setVisible(true);
		}
		else {
			lblBgInUse.setVisible(true);
		}
		
		TranModel tm = cp.getCp().getTran();
		if(tm == null) {
			fpStartTime.getVal().setParms("");
			fpRemainTime.getVal().setParms(0, 0);
		}
		else {
			fpStartTime.getVal().setParms(DateUtil.formatDateTime(tm.getStartDttm(), false));
			long time = System.currentTimeMillis();
			if(time >= tm.getEndDttm().getTime()) {
				fpRemainTime.getVal().setParms(0, 0);
			}
			else {
				int m = DateUtil.getMinBetween(new Date(time), tm.getEndDttm());
				fpRemainTime.getVal().setParms(m / 60, m % 60);
			}
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
