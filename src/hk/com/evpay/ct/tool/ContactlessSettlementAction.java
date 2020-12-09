package hk.com.evpay.ct.tool;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.CommonPanel;
import hk.com.evpay.ct.CpSelectionPanel;
import hk.com.evpay.ct.CtClient;
import hk.com.evpay.ct.HelpPanel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.UiUtil;

public class ContactlessSettlementAction extends AbstractAction{
	private static final Logger logger = Logger.getLogger(ContactlessSettlementAction.class);
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("Contactless Settlement Action received");
		
		CtClient c = CtClient.CUR_INST;
		CommonPanel pnlCur = c.getPnlCt().getCurrentPanel();
		if(pnlCur != null && (pnlCur instanceof CpSelectionPanel || pnlCur instanceof HelpPanel)) {		
			ContactlessSettlementDialog  dialog = new ContactlessSettlementDialog(c, false);
	
			dialog.setSize(980, 700);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setLocationRelativeTo(c);
			if(CtUtil.getConfig().isFullScreen()) {
				UiUtil.setEmptyCursor(dialog);
			}
			dialog.setVisible(true);
			dialog.setAlwaysOnTop(true);
		}
		else {
			logger.info(pnlCur == null ? "Cur panel is null" : "Cur panel:" + pnlCur.getClass());
		}
	}

}
